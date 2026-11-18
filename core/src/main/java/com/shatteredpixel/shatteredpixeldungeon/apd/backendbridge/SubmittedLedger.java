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

package com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// What this device has actually sent: run id, the digest of the log it was built from, and when. An evidence request is only ever answered
// against this record, so a log that changed afterwards is never passed off as the one that was submitted.
public final class SubmittedLedger {

	public static final String FILE_NAME = "submitted_runs.txt";

	public static final class Entry {
		public final String runId, digestHex;
		public final long sentAtMillis;

		Entry( String runId, String digestHex, long sentAtMillis ){
			this.runId = runId;
			this.digestHex = digestHex;
			this.sentAtMillis = sentAtMillis;
		}
	}

	private SubmittedLedger(){}

	public static File fileIn( File historyRoot ){
		return new File(historyRoot, FILE_NAME);
	}

	public static List<Entry> list( File historyRoot ){
		List<Entry> out = new ArrayList<>();
		if (historyRoot == null) return out;
		for (String[] r : TabFile.read(fileIn(historyRoot), 3)){
			try {
				out.add(new Entry(r[0], r[1], Long.parseLong(r[2])));
			} catch (NumberFormatException e){
				//skip
			}
		}
		return out;
	}

	public static Entry get( File historyRoot, String runId ){
		for (Entry e : list(historyRoot)) if (e.runId.equals(runId)) return e;
		return null;
	}

	//the first record for a run stands: sending the same run again does not rewrite what was originally sent
	public static boolean record( File historyRoot, String runId, String digestHex, long sentAtMillis ){
		if (historyRoot == null || get(historyRoot, runId) != null) return false;
		List<String[]> rows = new ArrayList<>();
		for (Entry e : list(historyRoot)) rows.add(new String[]{ e.runId, e.digestHex, String.valueOf(e.sentAtMillis) });
		rows.add(new String[]{ runId, digestHex, String.valueOf(sentAtMillis) });
		try {
			TabFile.write(fileIn(historyRoot), rows);
			return true;
		} catch (IOException e){
			return false;
		}
	}
}
