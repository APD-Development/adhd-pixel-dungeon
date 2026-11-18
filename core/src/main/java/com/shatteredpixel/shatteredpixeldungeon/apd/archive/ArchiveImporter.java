/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadOptions;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadStatus;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

//Adds the history of an archive to this device's history. It only ever ADDS, and it is all or nothing:
//  - the manifest is checked first (its own checksum, format, totals); a damaged or newer one stops everything before anything is written;
//  - every file is streamed to a temporary file and must match the manifest's size and SHA-256 and be a readable APD log of the run the
//    manifest says; if ANY file fails, or the archive ends early, or a listed file is missing, NOTHING is imported (the history is exactly
//    as it was) and the problems are reported. Only when everything has been verified are the files put in place, and if that fails
//    half way the files already put in place are taken out again;
//  - a log that already exists is NEVER replaced: an identical one is skipped, a different one is kept as it is and reported;
//  - only "runs/<id>.apdlog" and "legacy/<id>.apdlog" with plain ids can be written, whatever the archive's entry names say;
//  - synthetic demo data is rejected, and a folder that carries the demo marker is refused;
//  - the index is rebuilt afterwards, and the derived caches rebuild themselves from the new logs.
//Reading is streamed, a file at a time, so a large archive does not need to fit in memory. Never throws: problems are in the Report.
public final class ArchiveImporter {

	private ArchiveImporter(){}

	//no single log can be larger than this (the biggest real run logs are a few MB)
	public static final long MAX_FILE_BYTES = 512L * 1024 * 1024;
	//room to keep free on the device while importing
	public static final long FREE_SPACE_MARGIN = 16L * 1024 * 1024;
	// Archive entry payloads are still fully checked/drained; only incidental
	// diagnostic names are bounded to avoid growing an untrusted ZIP's report.
	private static final int MAX_INVALID_PATH_DETAIL_CHARS = 160;
	private static final int MAX_IGNORED_ENTRY_DETAILS = 100;
	// A valid but hostile manifest can list thousands of absent files.
	// Keep their exact rejection count without retaining every path in the UI.
	private static final int MAX_MISSING_ENTRY_DETAILS = 100;
	// Manifest-listed ZIP members that fail validation can likewise be numerous.
	// Preserve rejection counts without retaining every individual failure name.
	private static final int MAX_INVALID_ENTRY_DETAILS = 100;
	// A valid archive may contain many logs whose local filenames already hold
	// different bytes. Keep every original log and exact counts, not every note.
	private static final int MAX_DIFFERENT_EXISTING_DETAILS = 100;

	public static final class Report {
		public boolean ok;
		public String problem = "";
		public int imported;
		public int skippedAlreadyHere;
		public int keptDifferentExisting;
		public int rejected;
		public int newerThanThisBuild;
		public int redactedImported;
		public int redactedDifferentExisting;
		public final List<String> details = new ArrayList<>();

		public String summary(){
			if (!ok && imported == 0 && skippedAlreadyHere == 0){
				StringBuilder n = new StringBuilder("Nothing was imported: " + problem);
				for (String d : details) n.append("\n- ").append(d);
				return n.toString();
			}
			StringBuilder b = new StringBuilder();
			b.append("Imported ").append(imported).append(imported == 1 ? " log" : " logs").append(".");
			if (skippedAlreadyHere > 0) b.append("\n").append(skippedAlreadyHere).append(" already here (identical), skipped.");
			if (keptDifferentExisting > 0) b.append("\n").append(keptDifferentExisting).append(" differ from a log already here: yours was kept, nothing was replaced.");
			if (redactedDifferentExisting > 0) b.append("\n").append(redactedDifferentExisting)
					.append(" of those archived copies had hidden information removed because their runs were unfinished when exported. Such differences can be expected; the original logs are safe.");
			if (redactedImported > 0) b.append("\n").append(redactedImported).append(" belong to runs that were not finished when exported (hidden information left out).");
			if (newerThanThisBuild > 0) b.append("\n").append(newerThanThisBuild).append(" were recorded by a newer telemetry version: kept, but this version may not show all of it.");
			if (rejected > 0) b.append("\n").append(rejected).append(" were not imported (damaged, not matching the manifest, or not allowed).");
			if (!ok) b.append("\nStopped early: ").append(problem);
			for (String d : details) b.append("\n- ").append(d);
			return b.toString();
		}
	}

	private static final Object LOCK = new Object();

	public static Report importArchive( File root, InputStream in ){
		if (in == null){
			Report report = new Report();
			report.problem = "the archive source is not available.";
			return report;
		}
		return importZip(root, new ZipInputStream(new ProgressGuardInputStream(new NonClosingInputStream(in))));
	}

	//the same, from an already opened zip stream (tests give it streams that misbehave the way old Android's does)
	static Report importZip( File root, ZipInputStream zip ){
		Report report = new Report();
		if (zip == null){
			report.problem = "the archive ZIP stream is not available.";
			return report;
		}
		File tmpDir = null;
		synchronized (LOCK){
			try {
				if (root == null){
					report.problem = "history is not set up.";
					return report;
				}
				if (new File(root, DatasetStore.DEMO_MARKER_FILE).isFile()){
					report.problem = "this folder holds demo data, which never receives history.";
					return report;
				}
				ZipEntry first = zip.getNextEntry();
				if (first == null || !ArchiveManifest.ENTRY_NAME.equals(first.getName())){
					report.problem = "this is not an APD history archive (no manifest first).";
					return report;
				}
				byte[] manifestBytes = readLimited(zip, ArchiveManifest.MAX_BYTES);
				if (manifestBytes == null){
					report.problem = "the manifest is too large.";
					return report;
				}
				ArchiveManifest manifest;
				try {
					manifest = ArchiveManifest.parse(manifestBytes);
				} catch (IllegalArgumentException bad){
					report.problem = bad.getMessage();
					return report;
				}
				//every entry the manifest lists must be a path we would ever write, a plain size, and not demo data
				for (ArchiveManifest.Entry e : manifest.files){
					if (!ArchiveManifest.isValidPath(e.path)){
						// The manifest can contain a very long, syntactically valid but
					// forbidden path. Do not duplicate it into the refusal report.
					String excerpt = e.path.length() <= MAX_INVALID_PATH_DETAIL_CHARS ? e.path
							: e.path.substring(0, MAX_INVALID_PATH_DETAIL_CHARS) + "... (truncated)";
					report.problem = "the archive lists a file it is not allowed to write: " + excerpt;
						return report;
					}
					String expectedKind = e.path.startsWith("runs/") ? ArchiveManifest.KIND_RUN : ArchiveManifest.KIND_IMPORTED;
					if (!expectedKind.equals(e.kind)){
						report.problem = "the archive gives a history file the wrong kind: " + e.path;
						return report;
					}
					if (e.size > MAX_FILE_BYTES){
						report.problem = "the archive lists a file that is too large: " + e.path;
						return report;
					}
					if (e.logId().startsWith(DatasetStore.DEMO_LOG_PREFIX) || e.runId.startsWith(DatasetStore.DEMO_LOG_PREFIX)){
						report.problem = "the archive contains demo data, which is never imported.";
						return report;
					}
				}
				if (root.getUsableSpace() > 0 && root.getUsableSpace() < manifest.totalBytes() + FREE_SPACE_MARGIN){
					report.problem = "there is not enough free space (" + Size.text(manifest.totalBytes()) + " needed).";
					return report;
				}

				tmpDir = new File(root, "import-tmp");
				deleteTree(tmpDir);
				// Staging may not follow a leftover symbolic link after cleanup.
				// The history root itself is allowed to have a canonical parent.
				if (!tmpDir.getCanonicalFile().equals(new File(root.getCanonicalFile(), "import-tmp"))){
					report.problem = "the archive temporary folder is not inside history.";
					return report;
				}
				if (!tmpDir.mkdirs() && !tmpDir.isDirectory()){
					report.problem = "cannot create a working folder.";
					return report;
				}
				Map<String, ArchiveManifest.Entry> byPath = new HashMap<>();
				for (ArchiveManifest.Entry listed : manifest.files){
					// Match find() semantics for repeated manifest paths: first entry wins.
					if (!byPath.containsKey(listed.path)) byPath.put(listed.path, listed);
				}
				Set<String> seen = new HashSet<>();
				List<Staged> staged = new ArrayList<>();
				ZipEntry entry;
				int problems = 0;
				int ignoredDetails = 0;
				int invalidDetails = 0;
				// Reuse the same ZIP scratch bytes for both validated logs and ignored
				// entries. Even a large history archive needs only one read buffer.
				byte[] entryBuffer = new byte[16 * 1024];
				while ((entry = zip.getNextEntry()) != null){
					String name = entry.getName();
					ArchiveManifest.Entry e = byPath.get(name);
					if (e == null || !seen.add(name)){
						//Something the manifest does not list is never written. Report a
						//bounded sample: thousands of extra ZIP entries must not make the
						//diagnostics consume unbounded memory. Still drain every payload.
						if (ignoredDetails < MAX_IGNORED_ENTRY_DETAILS){
							report.details.add("not in the manifest or repeated, ignored: " + safe(name));
							ignoredDetails++;
						} else if (ignoredDetails == MAX_IGNORED_ENTRY_DETAILS){
							report.details.add("Additional unlisted or repeated archive entries were ignored (details omitted).");
							ignoredDetails++;
						}
						// getNextEntry otherwise drains this payload without our size limit.
						// The buffer is reused once this entry has finished draining.
						long ignoredSize = 0;
						int ignoredRead;
						while ((ignoredRead = readChunk(zip, entryBuffer)) > 0){
							ignoredSize += ignoredRead;
							if (ignoredSize > MAX_FILE_BYTES) throw new IOException("an ignored file in the archive is too large");
						}
						continue;
					}
					Staged st = stage(root, tmpDir, zip, e, staged.size(), report,
							invalidDetails < MAX_INVALID_ENTRY_DETAILS, entryBuffer);
					if (st == null){
						problems++;
						if (invalidDetails++ == MAX_INVALID_ENTRY_DETAILS){
							report.details.add("Additional invalid manifest-listed archive files were rejected (details omitted).");
						}
					} else staged.add(st);
				}
				int missingDetails = 0;
				for (ArchiveManifest.Entry e : manifest.files){
					if (!seen.contains(e.path)){
						problems++;
						if (missingDetails < MAX_MISSING_ENTRY_DETAILS){
							report.details.add("listed but missing from the archive: " + e.path);
							missingDetails++;
						} else if (missingDetails == MAX_MISSING_ENTRY_DETAILS){
							report.details.add("Additional manifest-listed files were missing (details omitted).");
							missingDetails++;
						}
					}
				}
				if (problems > 0){
					report.rejected = problems;
					report.problem = "the archive has " + problems + (problems == 1 ? " problem" : " problems") + ", so nothing was imported.";
					return report;
				}
				commit(root, staged, report);
				report.ok = true;
				rebuildIndex(root);
				return report;
			} catch (Throwable t){
				report.ok = false;
				report.problem = String.valueOf(t);
				return report;
			} finally {
				if (tmpDir != null) deleteTree(tmpDir);
				//Release the ZIP inflater on every success, refusal and failure path.
				try { zip.close(); } catch (IOException ignored){}
			}
		}
	}

	private static final class NonClosingInputStream extends FilterInputStream {
		NonClosingInputStream( InputStream in ){
			super(in);
		}

		@Override
		public void close(){
		}
	}

	//a file that has been read from the archive and fully verified, waiting for the rest
	private static final class Staged {
		final ArchiveManifest.Entry entry;
		final File tmp;
		final boolean newerThanReader;

		Staged( ArchiveManifest.Entry entry, File tmp, boolean newerThanReader ){
			this.entry = entry;
			this.tmp = tmp;
			this.newerThanReader = newerThanReader;
		}
	}

	//Streams one entry to a temporary file and verifies it. Returns null (and says why in the report) if it cannot be trusted.
	private static Staged stage( File root, File tmpDir, ZipInputStream zip, ArchiveManifest.Entry e, int number,
			Report report, boolean includeDetail, byte[] buf ) throws IOException {
		File tmp = new File(tmpDir, "entry-" + number + ".tmp");
		MessageDigest digest = newDigest();
		long size = 0;
		boolean tooBig = false;
		OutputStream out = new FileOutputStream(tmp);
		try {
			int n;
			while ((n = readChunk(zip, buf)) > 0){
				size += n;
				if (size > e.size + (64L << 20)) throw new IOException("a file in the archive is far larger than the manifest says");
				if (size > e.size || size > MAX_FILE_BYTES){
					tooBig = true;   //keep reading to move past it, but write no more
					continue;
				}
				digest.update(buf, 0, n);
				out.write(buf, 0, n);
			}
		} finally {
			out.close();
		}
		if (tooBig || size != e.size || !ArchiveManifest.hex(digest.digest()).equalsIgnoreCase(e.sha256)){
			tmp.delete();
			if (includeDetail) report.details.add(e.path + ": does not match the manifest (size or checksum).");
			return null;
		}
		//it must be a readable APD log, of the run the manifest says, and never demo data
		ReadResult r;
		try {
			//Every frame and seal is still validated. Only RUN_START events
			//must be retained to check the archived file's logical identity.
			//An import of a long run must not keep its full gameplay event list.
			r = RunLogReader.read(tmp, new ReadOptions().onlyRunStartEvents(true));
		} catch (IOException unreadable){
			r = null;
		}
		if (r == null || r.header == null || r.status == ReadStatus.BAD_HEADER || r.status == ReadStatus.UNSUPPORTED_CONTAINER){
			tmp.delete();
			if (includeDetail) report.details.add(e.path + ": not a readable APD log.");
			return null;
		}
		String startedRun = null;
		for (com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent ev : r.events){
			if (ev.type == com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes.RUN_START){
				try {
					String candidate = com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.decodeRunStart(ev, r).runId;
					if (StatsStore.isValidRunId(candidate)){
						startedRun = candidate;
						break;
					}
				} catch (RuntimeException ignored){
					//Keep looking: archive identity follows the first readable typed start.
				}
			}
		}
		boolean runMatches = !e.kind.equals(ArchiveManifest.KIND_RUN)
				|| (startedRun != null && startedRun.length() > 0 ? startedRun.equals(e.runId) : r.header.runId.equals(e.runId));
		// A run's typed RUN_START identifies its logical run (shared by resume
		// segments), but the log header identifies this individual file. Both
		// identities must survive import, or RunIndex later marks the copied
		// history file unreadable under its on-disk filename.
		boolean headerMatchesFilename = !e.kind.equals(ArchiveManifest.KIND_RUN)
				|| e.logId().equals(r.header.runId);
		if (!StatsStore.isValidRunId(r.header.runId) || r.header.runId.startsWith(DatasetStore.DEMO_LOG_PREFIX)
				|| (startedRun != null && startedRun.startsWith(DatasetStore.DEMO_LOG_PREFIX))
				|| r.header.writerBuild.contains("SYNTHETIC-DEMO") || !runMatches || !headerMatchesFilename){
			tmp.delete();
			if (includeDetail) report.details.add(e.path + ": its header does not match the manifest, or it is demo data.");
			return null;
		}
		return new Staged(e, tmp, r.newerThanReader());
	}

	//Puts the verified files in place. A name that is taken is never replaced. If something fails half way, everything this import
	//put in place is taken out again.
	private static void commit( File root, List<Staged> staged, Report report ) throws IOException {
		// Resolve the supplied history root once. An existing runs/legacy directory
		// or log file can be a symbolic link even when the manifest path itself
		// has no traversal components.
		File canonicalRoot = root.getCanonicalFile();
		List<File> placed = new ArrayList<>();
		int imported = 0, already = 0, kept = 0, redacted = 0, redactedKept = 0, newer = 0;
		List<String> notes = new ArrayList<>();
		// A history archive can encounter many occupied filenames. Reuse these
		// scratch bytes for comparisons and the rare rename fallback.
		byte[] localBuffer = new byte[16 * 1024];
		try {
			for (Staged st : staged){
				File target = new File(root, st.entry.path);
				// Never follow a pre-existing symlink to another folder or file.
				// ArchiveManifest.isValidPath has already restricted this to a
				// direct runs/ or legacy/ child with a plain log ID.
				File expectedTarget = new File(canonicalRoot, st.entry.path).getAbsoluteFile();
				if (!expectedTarget.equals(target.getCanonicalFile())){
					throw new IOException("history archive target resolves outside its designated path: " + st.entry.path);
				}
				File folder = target.getParentFile();
				if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("cannot create " + folder);
				//claim the name atomically: an existing log is never replaced
				if (!target.createNewFile()){
					if (sameContent(target, st.entry.size, st.entry.sha256, localBuffer)) already++;
					else {
						kept++;
						if (st.entry.redacted) redactedKept++;
						if (kept <= MAX_DIFFERENT_EXISTING_DETAILS){
							notes.add(st.entry.path + ": a different log with this name is already here, yours was kept.");
						} else if (kept == MAX_DIFFERENT_EXISTING_DETAILS + 1){
							notes.add("Additional archived files differed from existing history; original files were kept (details omitted).");
						}
					}
					st.tmp.delete();
					continue;
				}
				placed.add(target);
				if (!st.tmp.renameTo(target)){
					//the claim is an empty file we created ourselves: replace it by copying
					copy(st.tmp, target, localBuffer);
					st.tmp.delete();
				}
				imported++;
				if (st.entry.redacted) redacted++;
				if (st.newerThanReader) newer++;
			}
		} catch (IOException | RuntimeException failure){
			for (File f : placed) f.delete();
			report.problem = "putting the files in place failed (" + failure + "); nothing was imported.";
			throw new IOException(report.problem);
		}
		report.imported = imported;
		report.skippedAlreadyHere = already;
		report.keptDifferentExisting = kept;
		report.redactedImported = redacted;
		report.redactedDifferentExisting = redactedKept;
		report.newerThanThisBuild = newer;
		report.details.addAll(notes);
	}

	private static boolean sameContent( File f, long expectedSize, String sha256, byte[] buf ){
		// A size mismatch proves that the local file is different, without
		// rereading a potentially very large original history log.
		if (f.length() != expectedSize) return false;
		try {
			MessageDigest d = newDigest();
			InputStream in = new FileInputStream(f);
			try {
				int n;
				while ((n = in.read(buf)) > 0) d.update(buf, 0, n);
			} finally {
				in.close();
			}
			return ArchiveManifest.hex(d.digest()).equalsIgnoreCase(sha256);
		} catch (IOException e){
			return false;
		}
	}

	private static void copy( File from, File to, byte[] buf ) throws IOException {
		InputStream in = new FileInputStream(from);
		try {
			OutputStream out = new FileOutputStream(to);
			try {
				int n;
				while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}
	}

	private static void rebuildIndex( File root ){
		try {
			if (root != null && root.isDirectory()){
				com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunIndex idx = new StatsStore(root).index();
				idx.rebuild();
				idx.save();
			}
		} catch (Throwable ignored){
			//the index is derived and rebuilds itself when it is next read
		}
	}

	//One read from the archive's zip stream: bytes, or -1 at the end of the entry. A stream that answers 0 to a read that asked for bytes
	//breaks the contract: old Android (5.0) does that for an entry that is cut short, and its getNextEntry() would then spin for ever
	//skipping the rest of the entry (and hold the importer's lock, so every later import would hang). It means the archive is damaged:
	//stop right here.
	static int readChunk( InputStream in, byte[] buf ) throws IOException {
		int n = in.read(buf);
		if (n == 0) throw new IOException("the archive is cut short or damaged (a file in it ends early)");
		return n;
	}

	private static byte[] readLimited( InputStream in, int max ) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int n;
		while ((n = readChunk(in, buf)) > 0){
			if (out.size() + n > max) return null;
			out.write(buf, 0, n);
		}
		return out.toByteArray();
	}

	private static MessageDigest newDigest(){
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (java.security.NoSuchAlgorithmException e){
			throw new IllegalStateException(e);
		}
	}

	private static String safe( String name ){
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < name.length() && i < 80; i++){
			char c = name.charAt(i);
			b.append(c >= 32 && c < 127 ? c : '?');
		}
		return b.toString();
	}

	private static void deleteTree( File f ){
		// File.listFiles() follows directory symlinks. Never recurse into a
		// leftover staging link whose target is outside its canonical parent.
		try {
			File parent = f.getAbsoluteFile().getParentFile();
			if (parent != null && !f.getCanonicalFile().equals(new File(parent.getCanonicalFile(), f.getName()))){
				f.delete(); //remove the link itself, not the files it points at
				return;
			}
		} catch (IOException ignored){
			// Unknown canonical destination: leave it untouched.
			return;
		}
		File[] kids = f.listFiles();
		if (kids != null) for (File k : kids) deleteTree(k);
		f.delete();
	}
}
