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

package com.shatteredpixel.shatteredpixeldungeon.apd.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Ranking as a pure function of server-held records, so any server ranks the same way. A board only counts its own ruleset, balance revision and difficulty, at or above its minimum trust level.
public final class Leaderboard {

	public static final class Board {
		public final String id, rulesetId;
		public final int balanceRevision, difficulty;   //difficulty 0 = every difficulty
		public final TrustLevel minTrust;

		public Board( String id, String rulesetId, int balanceRevision, int difficulty, TrustLevel minTrust ){
			this.id = id;
			this.rulesetId = rulesetId;
			this.balanceRevision = balanceRevision;
			this.difficulty = difficulty;
			this.minTrust = minTrust;
		}
	}

	public static final class Entry {
		public final Id player, submission;
		public final RunSubmission run;
		public final TrustLevel trust;
		public final long receivedAtMillis;

		public Entry( Id player, Id submission, RunSubmission run, TrustLevel trust, long receivedAtMillis ){
			this.player = player;
			this.submission = submission;
			this.run = run;
			this.trust = trust;
			this.receivedAtMillis = receivedAtMillis;
		}
	}

	private Leaderboard(){}

	//best run per player, ordered by score (high first); ties go to the earlier submission, then to the smaller id, so the order is total
	public static List<Entry> rank( Board b, List<Entry> all ){
		List<Entry> best = new ArrayList<>();
		for (Entry e : all){
			if (!e.run.rulesetId.equals(b.rulesetId) || e.run.balanceRevision != b.balanceRevision) continue;
			if (b.difficulty != 0 && e.run.difficulty != b.difficulty) continue;
			if (!e.trust.satisfies(b.minTrust)) continue;
			Entry cur = null;
			for (Entry x : best) if (x.player.equals(e.player)) cur = x;
			if (cur == null) best.add(e);
			else if (BETTER.compare(e, cur) < 0){
				best.remove(cur);
				best.add(e);
			}
		}
		Collections.sort(best, BETTER);
		return best;
	}

	private static final Comparator<Entry> BETTER = new Comparator<Entry>(){
		@Override
		public int compare( Entry a, Entry b ){
			if (a.run.scoreClaim != b.run.scoreClaim) return a.run.scoreClaim > b.run.scoreClaim ? -1 : 1;
			if (a.receivedAtMillis != b.receivedAtMillis) return a.receivedAtMillis < b.receivedAtMillis ? -1 : 1;
			return a.submission.compareTo(b.submission);
		}
	};
}
