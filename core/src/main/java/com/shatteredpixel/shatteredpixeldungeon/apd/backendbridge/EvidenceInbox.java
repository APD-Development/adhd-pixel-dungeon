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

import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Evidence;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.EvidenceMessages;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Evidence requests a server has made that the player has not answered yet. Nothing here sends anything: a request only waits here until the
// player decides (see EvidenceFlow).
public final class EvidenceInbox {

	public static final String FILE_NAME = "evidence_requests.txt";
	public static final int MAX_PENDING = 20;

	public static final class Entry {
		public final EvidenceMessages.Asked asked;
		public final long receivedAtMillis;

		Entry( EvidenceMessages.Asked asked, long receivedAtMillis ){
			this.asked = asked;
			this.receivedAtMillis = receivedAtMillis;
		}
	}

	private EvidenceInbox(){}

	public static File fileIn( File historyRoot ){
		return new File(historyRoot, FILE_NAME);
	}

	public static List<Entry> list( File historyRoot ){
		List<Entry> out = new ArrayList<>();
		if (historyRoot == null) return out;
		for (String[] r : TabFile.read(fileIn(historyRoot), 5)){
			try {
				Evidence.Request req = new Evidence.Request(Id.of(Id.Kind.SUBMISSION, r[0]), Evidence.Kind.valueOf(r[1]), Long.parseLong(r[2]));
				out.add(new Entry(new EvidenceMessages.Asked(req, r[3]), Long.parseLong(r[4])));
			} catch (RuntimeException e){
				//skip a damaged record
			}
		}
		return out;
	}

	//drops requests whose deadline has passed (they can no longer be answered); returns how many were dropped
	public static int prune( File historyRoot, long nowMillis ){
		List<Entry> all = list(historyRoot);
		int dropped = 0;
		for (int i = all.size() - 1; i >= 0; i--){
			if (all.get(i).asked.request.deadlineMillis < nowMillis){
				all.remove(i);
				dropped++;
			}
		}
		if (dropped > 0 && !save(historyRoot, all)) return 0;
		return dropped;
	}

	public static int pending( File historyRoot ){
		return list(historyRoot).size();
	}

	//A request already waiting for the same submission and kind is not added twice; the inbox holds at most MAX_PENDING.
	public static boolean add( File historyRoot, EvidenceMessages.Asked asked, long nowMillis ){
		if (historyRoot == null) return false;
		List<Entry> all = list(historyRoot);
		if (all.size() >= MAX_PENDING) return false;
		for (Entry e : all){
			if (e.asked.request.submissionId.equals(asked.request.submissionId) && e.asked.request.kind == asked.request.kind) return false;
		}
		all.add(new Entry(asked, nowMillis));
		return save(historyRoot, all);
	}

	public static boolean remove( File historyRoot, Id submission, Evidence.Kind kind ){
		List<Entry> all = list(historyRoot);
		boolean removed = false;
		for (int i = all.size() - 1; i >= 0; i--){
			Evidence.Request r = all.get(i).asked.request;
			if (r.submissionId.equals(submission) && r.kind == kind){
				all.remove(i);
				removed = true;
			}
		}
		return removed && save(historyRoot, all);
	}

	private static boolean save( File historyRoot, List<Entry> all ){
		List<String[]> rows = new ArrayList<>();
		for (Entry e : all){
			Evidence.Request r = e.asked.request;
			rows.add(new String[]{ r.submissionId.value, r.kind.name(), String.valueOf(r.deadlineMillis), e.asked.runId, String.valueOf(e.receivedAtMillis) });
		}
		try {
			TabFile.write(fileIn(historyRoot), rows);
			return true;
		} catch (IOException e){
			return false;
		}
	}
}
