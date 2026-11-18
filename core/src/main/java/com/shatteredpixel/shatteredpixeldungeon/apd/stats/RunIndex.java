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


package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//A small file listing every run log with a summary line, so a list of runs does not need every log to be opened.
//
//The index is DERIVED data. The logs are the evidence. If the index file is missing, damaged, or of an unknown
//version it is simply rebuilt by scanning the log folder, and it can always be rebuilt again.
//
//Format: "APDI" | formatVersion u8 | entryCount varint | (entryLength varint | entry bytes)* | crc32 u32 (of all before)
public final class RunIndex {

	static final byte[] MAGIC = { 'A', 'P', 'D', 'I' };
	static final int FORMAT = 2;
	public static final String LOG_SUFFIX = ".apdlog";

	//RunIndex objects for the same on-disk index do not share their instance monitor.
	//Serialize the tiny atomic replacement section so they never open/truncate the same temp file concurrently.
	private static final Object SAVE_LOCK = new Object();

	interface SaveHook {
		void afterTempOpened( File temp ) throws InterruptedException;
	}

	private final File indexFile;
	private final File runsDir;
	private final Map<String, RunIndexEntry> entries = new LinkedHashMap<>();
	private boolean rebuilt;

	public RunIndex( File indexFile, File runsDir ){
		this.indexFile = indexFile;
		this.runsDir = runsDir;
	}

	//Loads the index, rebuilding it from the logs if it is missing or unusable. Never throws for bad index data.
	public static RunIndex load( File indexFile, File runsDir ){
		RunIndex idx = new RunIndex(indexFile, runsDir);
		boolean ok = false;
		if (indexFile.isFile() && isDirectIndexFile(indexFile)){
			try {
				idx.readFrom(RunLogReader.readFully(indexFile));
				ok = true;
			} catch (Throwable t){
				idx.entries.clear();
			}
		}
		if (!ok){
			idx.rebuild();
			idx.rebuilt = true;
			try {
				idx.save();
			} catch (IOException ignored){
				//the in-memory index is still correct
			}
		}
		return idx;
	}

	//true if load() had to rebuild the index
	public boolean wasRebuilt(){
		return rebuilt;
	}

	public synchronized RunIndexEntry get( String runId ){
		return entries.get(runId);
	}

	public synchronized int size(){
		return entries.size();
	}

	//oldest first
	public synchronized List<RunIndexEntry> all(){
		List<RunIndexEntry> list = new ArrayList<>(entries.values());
		Collections.sort(list, new Comparator<RunIndexEntry>(){
			@Override
			public int compare( RunIndexEntry a, RunIndexEntry b ){
				if (a.createdAtMillis != b.createdAtMillis) return a.createdAtMillis < b.createdAtMillis ? -1 : 1;
				return a.runId.compareTo(b.runId);
			}
		});
		return list;
	}

	public synchronized void upsert( RunIndexEntry entry ){
		entries.put(entry.runId, entry);
	}

	//Re-reads one log and updates its entry. Returns the new entry, or null if the log is not there.
	public synchronized RunIndexEntry refresh( String runId ) throws IOException {
		// Run IDs are filenames, not paths. Never let a caller read another folder through the derived index.
		if (!StatsStore.isValidRunId(runId)) throw new IOException("invalid run ID for index refresh");
		File log = new File(runsDir, runId + LOG_SUFFIX);
		if (!log.isFile()){
			entries.remove(runId);
			return null;
		}
		// A filename can be a symlink to another run or outside the configured history.
		// Match the canonical direct-child rule used by destructive log recovery.
		if (!isDirectRunLog(log)) throw new IOException("run index refresh refused linked log");
		RunIndexEntry e = describeDirectLog(log, runId);
		entries.put(runId, e);
		return e;
	}

	//Throws away the entries and scans the log folder. Logs that cannot be read still get an UNREADABLE entry,
	//so they stay visible instead of disappearing.
	public synchronized void rebuild(){
		entries.clear();
		File[] files = runsDir.listFiles();
		if (files == null) return;
		for (File f : files){
			String name = f.getName();
			// A direct child with an invalid filename is not an APD run ID. Never
			// let its header supply another ID and impersonate a genuine run.
			if (!f.isFile() || !isValidLogFileName(name) || !isDirectRunLog(f)) continue;
			String stem = name.substring(0, name.length() - LOG_SUFFIX.length());
			try {
				RunIndexEntry e = describeDirectLog(f, stem);
				entries.put(e.runId, e);
			} catch (Throwable t){
				entries.put(stem, new RunIndexEntry(stem, null, false, name, 0, RunIndexEntry.Status.UNREADABLE, 0, 0, 0,
						f.length()));
			}
		}
	}

	// The on-disk filename is the run's identity for the derived index. A copied
	// log whose readable header names another run remains visible as unreadable,
	// rather than impersonating that other run or overwriting its summary.
	private static RunIndexEntry describeDirectLog( File log, String fileRunId ) throws IOException {
		RunIndexEntry described = RunIndexEntry.describe(log, fileRunId);
		if (fileRunId.equals(described.runId)) return described;
		return new RunIndexEntry(fileRunId, null, false, log.getName(), 0,
				RunIndexEntry.Status.UNREADABLE, 0, 0, 0, log.length());
	}

	// Allow ordinary symlinked storage roots, but never treat a linked individual log
	// (including an alias to another local log) as its own run-history source.
	private boolean isDirectRunLog( File log ){
		try {
			File canonical = log.getCanonicalFile();
			return canonical.getParentFile().equals(runsDir.getCanonicalFile())
					&& canonical.getName().equals(log.getName());
		} catch (IOException ignored){
			return false;
		}
	}

	// Index entries are derived from direct runs/<id>.apdlog files only. This
	// also checks old checksum-valid caches made before rebuild rejected bad names.
	private static boolean isValidLogFileName( String name ){
		return name != null && name.endsWith(LOG_SUFFIX)
				&& StatsStore.isValidRunId(name.substring(0, name.length() - LOG_SUFFIX.length()));
	}

	// A derived cache must not be loaded or written through an individual symbolic
	// link. Resolve the parent independently so symlinked history roots still work.
	private static boolean isDirectIndexFile( File file ){
		try {
			File absolute = file.getAbsoluteFile();
			File parent = absolute.getParentFile();
			return parent != null && absolute.getCanonicalFile().equals(
					new File(parent.getCanonicalFile(), absolute.getName()));
		} catch (IOException ignored){
			return false;
		}
	}

	//Writes the index atomically: a complete temp file is written first, then moved over the old one.
	public synchronized void save() throws IOException {
		save(null);
	}

	//Package-private hook makes the cross-instance temp-file ordering regression deterministic.
	synchronized void save( SaveHook hook ) throws IOException {
		ByteSink body = new ByteSink();
		body.writeBytes(MAGIC);
		body.writeByte(FORMAT);
		List<RunIndexEntry> list = all();
		body.writeUnsigned(list.size());
		for (RunIndexEntry e : list){
			byte[] enc = e.encode();
			body.writeUnsigned(enc.length);
			body.writeBytes(enc);
		}
		body.writeInt32((int) LogFormat.crc(body.rawBuffer(), 0, body.size()));

		synchronized (SAVE_LOCK){
			File parent = indexFile.getAbsoluteFile().getParentFile();
			if (parent != null && !parent.isDirectory() && !parent.mkdirs()){
				throw new IOException("cannot create " + parent);
			}
			File tmp = new File(indexFile.getPath() + ".tmp");
			// The predictable leftover temp name must never redirect writes to
			// another file or outside this index's storage directory.
			if (!isDirectIndexFile(tmp)) throw new IOException("refusing linked run index temp file");
			FileOutputStream out = new FileOutputStream(tmp);
			try {
				if (hook != null){
					try {
						hook.afterTempOpened(tmp);
					} catch (InterruptedException interrupted){
						Thread.currentThread().interrupt();
						throw new IOException("interrupted while saving the run index", interrupted);
					}
				}
				out.write(body.rawBuffer(), 0, body.size());
				out.flush();
				out.getFD().sync();
			} finally {
				out.close();
			}
			if (!tmp.renameTo(indexFile)){
				//some platforms refuse to rename over an existing file
				if (indexFile.exists() && !indexFile.delete()) throw new IOException("cannot replace " + indexFile);
				if (!tmp.renameTo(indexFile)) throw new IOException("cannot move " + tmp + " to " + indexFile);
			}
		}
	}

	private void readFrom( byte[] data ){
		if (data.length < MAGIC.length + 1 + 1 + 4) throw new FormatException("index too short");
		long stored = new ByteSource(data, data.length - 4, 4).readInt32() & 0xFFFFFFFFL;
		if (LogFormat.crc(data, 0, data.length - 4) != stored) throw new FormatException("index checksum mismatch");
		for (int i = 0; i < MAGIC.length; i++){
			if (data[i] != MAGIC[i]) throw new FormatException("not an APD index");
		}
		ByteSource src = new ByteSource(data, MAGIC.length, data.length - MAGIC.length - 4);
		int fmt = src.readByte();
		if (fmt != FORMAT) throw new FormatException("unknown index format " + fmt);
		long count = src.readUnsigned();
		if (count < 0 || count > src.remaining()) throw new FormatException("invalid index entry count");
		for (long i = 0; i < count; i++){
			int len = src.readUnsignedInt();
			RunIndexEntry e = RunIndexEntry.decode(src.readBytes(len));
			if (!isValidLogFileName(e.logFile)) throw new FormatException("invalid indexed run log filename");
			String fileRunId = e.logFile.substring(0, e.logFile.length() - LOG_SUFFIX.length());
			if (!fileRunId.equals(e.runId)) throw new FormatException("indexed run ID differs from log filename");
			// Duplicate identities in a valid-CRC derived index cannot be trusted: the
			// later summary would silently replace the first and hide original history.
			if (entries.containsKey(e.runId)) throw new FormatException("duplicate run index entry");
			entries.put(e.runId, e);
		}
		// A checksum-valid index with uncounted bytes is still malformed derived data.
		// Rebuild from the original logs rather than hiding valid history behind it.
		if (src.hasRemaining()) throw new FormatException("trailing data in run index");
	}
}
