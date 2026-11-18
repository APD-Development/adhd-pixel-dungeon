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
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LogFormat;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunIndex;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//Writes the whole raw history to one ZIP archive: every run log and every imported-record file, and a manifest with a checksum of
//each, enough to rebuild everything derived (the index and the caches are not exported, they are rebuilt).
//
//  - A FINISHED run (its newest log ends with the run's end) travels byte for byte, hidden information included: the player may read
//    that after a run is over, so it is theirs to keep.
//  - A run that is NOT finished travels only as a redacted copy (see LogRedactor), so the hidden information of a run in progress
//    never leaves the app.
//  - Synthetic demo data is never exported: a folder that carries the demo marker is refused, and demo-named logs are skipped.
//  - Nothing is changed or deleted in the history. Never throws: problems are in the Report.
public final class ArchiveExporter {

	private ArchiveExporter(){}

	public static final class Report {
		public boolean ok;
		public String problem = "";
		public int runLogs;
		public int redactedRunLogs;
		public int importedFiles;
		public int skippedDamagedTails;
		public int skippedUnreadable;
		public int skippedDemo;
		public long bytes;
		public final List<String> notes = new ArrayList<>();

		public String summary(){
			if (!ok) return "Nothing was exported: " + problem;
			StringBuilder b = new StringBuilder();
			b.append("Exported ").append(runLogs).append(runLogs == 1 ? " run log" : " run logs").append(" and ").append(importedFiles)
					.append(importedFiles == 1 ? " imported-record file" : " imported-record files").append(" (").append(Size.text(bytes)).append(" in the archive).");
			if (redactedRunLogs > 0){
				b.append("\n").append(redactedRunLogs).append(redactedRunLogs == 1 ? " log belongs to a run" : " logs belong to runs").append(" that ")
						.append(redactedRunLogs == 1 ? "is" : "are").append(" not finished: they are exported without their hidden information, which stays protected until the run is over.");
			}
			if (skippedUnreadable > 0) b.append("\n").append(skippedUnreadable).append(" file(s) were not readable as logs and were left out.");
			if (skippedDamagedTails > 0) b.append("\n").append(skippedDamagedTails).append(" saved damaged tail(s) stay on this device only.");
			for (String n : notes) b.append("\n").append(n);
			return b.toString();
		}
	}

	//what one archive entry will be made from
	private static final class Plan {
		final ArchiveManifest.Entry entry = new ArchiveManifest.Entry();
		File file;
		boolean redact;
		//A run that is not finished may still be growing (the game is recording it), so its redacted copy is made once and that
		//snapshot is what is measured and written. Finished runs and imported files never change.
		byte[] snapshot;
	}

	private static final class LogInfo {
		File file;
		String logId = "";
		boolean imported;
		String runId = "";
		long created;
		int segment;
		boolean ended;
		boolean sealed;
		long events;
		String telemetry = "";
		String status = "";
		boolean readable;
	}

	public static Report export( File root, OutputStream out, String appVersion, long nowMillis ){
		Report report = new Report();
		try {
			if (root == null || !root.isDirectory()){
				report.problem = "there is no history yet.";
				return report;
			}
			if (new File(root, DatasetStore.DEMO_MARKER_FILE).isFile()){
				report.problem = "this folder holds demo data, which is never exported.";
				return report;
			}
			List<LogInfo> logs = new ArrayList<>();
			collect(new File(root, "runs"), false, logs, report);
			collect(new File(root, "legacy"), true, logs, report);
			report.skippedDamagedTails = countDamaged(new File(root, "runs"));

			//a run is finished when its newest log ended
			Map<String, LogInfo> newest = new HashMap<>();
			for (LogInfo l : logs){
				if (l.imported || !l.readable) continue;
				LogInfo n = newest.get(l.runId);
				// Match DatasetStore: the run-start segment breaks same-millisecond ties.
				// Two distinct logs may still claim the same timestamp and segment.
				// If their completion states disagree, treat that ambiguity as unfinished
				// instead of releasing hidden facts based on filename ordering.
				if (n == null || l.created > n.created || (l.created == n.created
						&& (l.segment > n.segment || (l.segment == n.segment && !l.ended && n.ended)))){
					newest.put(l.runId, l);
				}
			}
			List<Plan> plans = new ArrayList<>();
			for (LogInfo l : logs){
				if (!l.readable){
					report.skippedUnreadable++;
					continue;
				}
				Plan p = new Plan();
				p.file = l.file;
				p.redact = !l.imported && !newest.get(l.runId).ended;
				ArchiveManifest.Entry e = p.entry;
				e.path = (l.imported ? "legacy/" : "runs/") + l.logId + ArchiveManifest.SUFFIX;
				e.kind = l.imported ? ArchiveManifest.KIND_IMPORTED : ArchiveManifest.KIND_RUN;
				e.runId = l.runId;
				e.telemetry = l.telemetry;
				e.sealed = l.sealed;
				e.redacted = p.redact;
				e.events = l.events;
				e.status = l.status;
				plans.add(p);
			}
			Collections.sort(plans, new Comparator<Plan>(){
				@Override
				public int compare( Plan a, Plan b ){
					return a.entry.path.compareTo(b.entry.path);
				}
			});

			//first pass: what each entry will contain (size and checksum), for the manifest at the front
			ArchiveManifest manifest = new ArchiveManifest();
			manifest.createdMillis = nowMillis;
			manifest.app = appVersion == null ? "" : appVersion;
			manifest.telemetry = TelemetryVersion.CURRENT.toString();
			manifest.container = LogFormat.CONTAINER_FORMAT;
			List<Plan> included = new ArrayList<>();
			for (Plan p : plans){
				byte[] bytes = content(p);
				if (bytes == null){
					report.skippedUnreadable++;
					continue;
				}
				p.entry.size = bytes.length;
				p.entry.sha256 = ArchiveManifest.sha256Hex(bytes);
				if (p.redact){
					ReadResult rr = RunLogReader.parse(bytes);
					p.entry.events = rr.eventCount;
					p.entry.sealed = rr.sealed;
				}
				manifest.files.add(p.entry);
				included.add(p);
			}
			if (report.redactedRunLogs == 0){
				for (Plan p : included) if (p.redact) report.redactedRunLogs++;
			}
			manifest.notes.add("Raw evidence only. The index and the derived caches are rebuilt by the app that imports this.");
			if (report.skippedDamagedTails > 0) manifest.notes.add(report.skippedDamagedTails + " saved damaged tails were not exported.");

			//second pass: write
			CountingOutputStream counted = new CountingOutputStream(new NonClosingOutputStream(out));
			ZipOutputStream zip = new ZipOutputStream(counted);
			try {
				byte[] text = manifest.encode().getBytes(ArchiveManifest.utf8());
				put(zip, ArchiveManifest.ENTRY_NAME, text);
				for (Plan p : included){
					byte[] bytes = content(p);
					if (bytes == null || bytes.length != p.entry.size || !ArchiveManifest.sha256Hex(bytes).equals(p.entry.sha256)){
						//The manifest already lists this file. Skipping it creates an archive import must reject.
						throw new IOException(p.entry.path + " changed while exporting. The archive is incomplete; export again.");
					}
					put(zip, p.entry.path, bytes);
					// Report the number of ZIP bytes actually written, not this entry's uncompressed size.
					if (p.entry.kind.equals(ArchiveManifest.KIND_IMPORTED)) report.importedFiles++;
					else report.runLogs++;
				}
				zip.finish();
				zip.flush();
				report.bytes = counted.bytesWritten;
				report.ok = true;
				return report;
			} finally {
				//Release the ZIP compressor promptly, but the caller still owns the destination stream.
				try { zip.close(); } catch (IOException ignored){}
			}
		} catch (Throwable t){
			report.ok = false;
			report.problem = String.valueOf(t);
			return report;
		}
	}

	// Counts the actual compressed ZIP bytes, including the manifest, entry headers,
	// central directory and ZIP footer. The caller still owns its output stream.
	private static final class CountingOutputStream extends FilterOutputStream {
		long bytesWritten;
		CountingOutputStream( OutputStream out ){ super(out); }
		@Override public void write( int value ) throws IOException {
			out.write(value);
			bytesWritten++;
		}
		@Override public void write( byte[] bytes, int off, int length ) throws IOException {
			out.write(bytes, off, length);
			bytesWritten += length;
		}
	}

	private static final class NonClosingOutputStream extends FilterOutputStream {
		NonClosingOutputStream( OutputStream out ){
			super(out);
		}

		@Override
		public void close() throws IOException {
			flush();
		}
	}

	private static void put( ZipOutputStream zip, String name, byte[] bytes ) throws IOException {
		ZipEntry e = new ZipEntry(name);
		e.setTime(0);
		zip.putNextEntry(e);
		zip.write(bytes);
		zip.closeEntry();
	}

	//the bytes an entry holds: the file itself, or its redacted copy
	private static byte[] content( Plan p ){
		try {
			// A log may be replaced between manifest planning and ZIP writing.
			// Never follow a new filesystem alias even if its bytes happen to match.
			if (!isDirectHistoryFile(p.file)) return null;
			if (p.snapshot != null) return p.snapshot;
			byte[] raw = readAll(p.file);
			if (!p.redact) return raw;
			LogRedactor.Result r = LogRedactor.redact(raw);
			p.snapshot = r == null ? null : r.bytes;
			return p.snapshot;
		} catch (Throwable t){
			return null;
		}
	}

	// The caller may supply a history root through a symlink, but its runs/
	// and legacy/ children and log entries must be direct children of that root.
	// File.isFile() follows symlinks, so check canonical destinations first.
	private static boolean isDirectHistoryFolder( File dir ) throws IOException {
		File parent = dir.getParentFile();
		return parent != null && dir.getCanonicalFile().equals(new File(parent.getCanonicalFile(), dir.getName()));
	}

	private static boolean isDirectHistoryFile( File f ) throws IOException {
		File folder = f.getParentFile();
		return folder != null && isDirectHistoryFolder(folder)
				&& f.getCanonicalFile().equals(new File(folder.getCanonicalFile(), f.getName()));
	}

	private static void collect( File dir, boolean imported, List<LogInfo> out, Report report ) throws IOException {
		if (!isDirectHistoryFolder(dir)){
			throw new IOException("history folder resolves outside its designated path: " + dir.getName());
		}
		File[] files = dir.listFiles();
		if (files == null) return;
		Arrays.sort(files);
		for (File f : files){
			String n = f.getName();
			if (!f.isFile() || !n.endsWith(RunIndex.LOG_SUFFIX)) continue;
			String id = n.substring(0, n.length() - RunIndex.LOG_SUFFIX.length());
			if (!StatsStore.isValidRunId(id)) continue;
			if (!isDirectHistoryFile(f)){
				throw new IOException("history log resolves outside its designated path: " + n);
			}
			if (id.startsWith(DatasetStore.DEMO_LOG_PREFIX)){
				report.skippedDemo++;
				continue;
			}
			LogInfo l = new LogInfo();
			l.file = f;
			l.logId = id;
			l.imported = imported;
			try {
				byte[] bytes = readAll(f);
				ReadResult r = RunLogReader.parse(bytes);
				// A copied log renamed under another runs/<id>.apdlog name must not
				// enter an archive the importer will reject. The header's ID is
				// per-file; RUN_START may name a different shared logical run.
				// Imported legacy files retain their existing compatibility rules.
				if (r.header != null && StatsStore.isValidRunId(r.header.runId)
						&& (imported || id.equals(r.header.runId))){
					// Both header ID and typed RUN_START identity matter. For legacy logs
					// the typed ID may differ, but it cannot erase demo header provenance.
					// Match the importer's independent header-ID and writer checks.
					if (r.header.runId.startsWith(DatasetStore.DEMO_LOG_PREFIX)
							|| r.header.writerBuild.contains("SYNTHETIC-DEMO")){
						report.skippedDemo++;
						continue;
					}
					l.readable = true;
					l.runId = r.header.runId;
					l.created = r.header.createdAtMillis;
					l.telemetry = r.header.telemetryVersion.toString();
					l.sealed = r.sealed;
					l.events = r.eventCount;
					l.status = r.status.name();
					//the run a log belongs to is the one its run start names (the segments of one run share it, each log has its own file id)
					boolean started = false;
					for (ApdEvent e : r.events){
						if (e.type == EventTypes.RUN_START && !started){
							try {
								LifecycleEvents.RunStart runStart = LifecycleEvents.decodeRunStart(e, r);
								String started0 = runStart.runId;
								if (StatsStore.isValidRunId(started0)){
									started = true;
									l.segment = runStart.segment;
									l.runId = started0;
								}
							} catch (RuntimeException ignored){
								//Keep looking: an unreadable typed start must not hide the first readable one.
							}
						}
						}
					// The typed RUN_START carries the logical ID for all segments.
					// A copied demo log must never escape simply by using a plain filename.
					if (l.runId.startsWith(DatasetStore.DEMO_LOG_PREFIX)){
						report.skippedDemo++;
						continue;
					}
					//The normal recorder may write one RANKING_LINK after RUN_END,
					//before sealing. That verified trailer is not gameplay progress.
					//An unsealed, damaged, or otherwise extended log is ambiguous:
					//never export its hidden history in raw form.
					// A seal does not prove a complete physical log if bytes follow it.
					// The reader stops at SEAL; recovery treats those bytes as a damaged tail.
					// Never export hidden history raw until the actual file ends at that seal.
					// A terminal end alone does not prove a genuine finished run when
					// no valid typed RUN_START ever established its logical identity.
					if (started && r.sealed && r.status.isClean() && r.damagedBytes() == 0 && !r.events.isEmpty()){
						int end = r.events.size() - 1;
						ApdEvent last = r.events.get(end);
						if (last.type == EventTypes.RANKING_LINK){
							try {
								LegacyEvents.decodeRankingLink(last, r);
								end--;
							} catch (RuntimeException ignored){
								end = -1; //A malformed trailer cannot prove completion.
							}
						}
						if (end >= 0){
							ApdEvent possibleEnd = r.events.get(end);
							if (possibleEnd.type == EventTypes.RUN_END){
								try {
									// Decoding alone does not validate the outcome enum. Only a
									// recognized terminal result proves hidden history is safe to export.
									LifecycleEvents.RunEnd runEnd = LifecycleEvents.decodeRunEnd(possibleEnd, r);
									if (runEnd.outcome == LifecycleEvents.OUTCOME_DIED
											|| runEnd.outcome == LifecycleEvents.OUTCOME_WON
											|| runEnd.outcome == LifecycleEvents.OUTCOME_ABANDONED){
										l.ended = true;
									}
								} catch (RuntimeException ignored){
									//Malformed run end cannot prove completion.
								}
							}
						}
					}
				}
			} catch (Throwable t){
				l.readable = false;
			}
			out.add(l);
		}
	}

	private static int countDamaged( File dir ){
		File[] files = dir.listFiles();
		int n = 0;
		if (files != null) for (File f : files) if (f.getName().endsWith(".damaged")) n++;
		return n;
	}

	static byte[] readAll( File f ) throws IOException {
		InputStream in = new FileInputStream(f);
		try {
			long length = f.length();
			if (length > Integer.MAX_VALUE - 16) throw new IOException("file too large");
			byte[] data = new byte[(int) length];
			int off = 0;
			while (off < data.length){
				int n = in.read(data, off, data.length - off);
				if (n < 0) break;
				off += n;
			}
			return off == data.length ? data : Arrays.copyOf(data, off);
		} finally {
			in.close();
		}
	}
}
