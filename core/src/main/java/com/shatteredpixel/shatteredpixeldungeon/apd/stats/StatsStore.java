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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//The folder layout for APD run history, rooted at a directory the caller chooses:
//
//  <root>/runs/<runId>.apdlog     one append-only event log per run (the evidence)
//  <root>/index.apdi              derived run index, rebuildable
//
//Pure java.io, no game classes, so it is easy to test and portable. Later phases decide the actual root folder.
public final class StatsStore {

	private final File root;
	private final File runsDir;
	private final File indexFile;
	private RunIndex index;

	public StatsStore( File root ){
		this.root = root;
		this.runsDir = new File(root, "runs");
		this.indexFile = new File(root, "index.apdi");
	}

	public File root(){
		return root;
	}

	public File runsDir(){
		return runsDir;
	}

	//run ids become file names, so they are restricted to letters, digits, '-' and '_' (a UUID fits)
	public static boolean isValidRunId( String id ){
		if (id == null || id.length() == 0 || id.length() > LogFormat.MAX_RUN_ID_LENGTH) return false;
		for (int i = 0; i < id.length(); i++){
			char c = id.charAt(i);
			boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_';
			if (!ok) return false;
		}
		return true;
	}

	public File logFile( String runId ){
		return new File(runsDir, runId + RunIndex.LOG_SUFFIX);
	}

	public synchronized RunIndex index(){
		if (index == null) index = RunIndex.load(indexFile, runsDir);
		return index;
	}

	//Starts recording a new run with the current telemetry version. Never throws, see RunLogWriter.openStatus().
	public RunLogWriter startRun( String runId, String writerBuild, long createdAtMillis ){
		return startRun(runId, TelemetryVersion.CURRENT, writerBuild, createdAtMillis, new RunLogWriter.Options());
	}

	public RunLogWriter startRun( String runId, TelemetryVersion version, String writerBuild, long createdAtMillis,
	                              RunLogWriter.Options options ){
		RunLogWriter w = RunLogWriter.create(logFile(runId), version, runId, writerBuild, createdAtMillis, options);
		if (!w.isDisabled()){
			try {
				index().refresh(runId);
				index().save();
			} catch (Throwable ignored){
				//the index is derived, a later rebuild fixes it
			}
		}
		return w;
	}

	//Updates the index entry after a writer was closed (or periodically). Never throws.
	public boolean refreshIndex( String runId ){
		try {
			index().refresh(runId);
			index().save();
			return true;
		} catch (Throwable t){
			return false;
		}
	}

	//A directory entry can be a symlink even when isFile() is true. Never truncate a
	//log reached through a link outside the designated runs directory, or an alias
	//to a different run inside it. Canonicalizing the parent keeps ordinary symlinked
	//storage roots compatible, without granting individual run entries that freedom.
	private boolean isDirectRunLog( File log ){
		try {
			File canonical = log.getCanonicalFile();
			return canonical.getParentFile().equals(runsDir.getCanonicalFile())
					&& canonical.getName().equals(log.getName());
		} catch (IOException ignored){
			return false;
		}
	}

	//Repairs every log that has a damaged tail (saving the cut bytes) and rebuilds the index from the logs.
	//Logs are never deleted.
	public synchronized List<LogRecovery.RepairReport> recoverAll(){
		List<LogRecovery.RepairReport> reports = new ArrayList<>();
		File[] files = runsDir.listFiles();
		if (files != null){
			for (File f : files){
				String name = f.getName();
				// Recovery can truncate damaged logs. Only a recognized on-disk run ID
				// may be repaired, matching the direct-file rules of the derived index.
				// Foreign .apdlog files remain untouched, even with parseable headers.
				if (f.isFile() && name.endsWith(RunIndex.LOG_SUFFIX)
						&& isValidRunId(name.substring(0, name.length() - RunIndex.LOG_SUFFIX.length()))
						&& isDirectRunLog(f)){
					reports.add(LogRecovery.repair(f, name.substring(0, name.length() - RunIndex.LOG_SUFFIX.length())));
				}
			}
		}
		index = new RunIndex(indexFile, runsDir);
		index.rebuild();
		try {
			index.save();
		} catch (IOException ignored){
		}
		return reports;
	}
}
