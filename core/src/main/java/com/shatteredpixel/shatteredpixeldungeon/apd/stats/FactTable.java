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

import java.util.ArrayList;
import java.util.List;

//Every derived row (APD runs and imported old records) with the links between them. Rebuildable from raw data.
public final class FactTable {

	//SPD's own played/won counters from the latest import, null if never imported
	public RunFacts legacyTotals;
	//daily scores from imports: {day, score}
	public final List<long[]> dailyScores = new ArrayList<>();
	public final List<RunFacts> apdRows = new ArrayList<>();
	public final List<RunFacts> legacyRows = new ArrayList<>();

	//Marks the imported records that are the same run as an APD run. EXACT: APD saw the ranking record's id.
	//PROBABLE: no id, but exactly one finished APD run has the same class, outcome, deepest floor, level and score.
	//Ambiguous matches are left unlinked. Linked records are never merged or deleted, only marked.
	public void link(){
		for (RunFacts leg : legacyRows){
			String gameId = leg.id.substring(LegacyFacts.ID_PREFIX.length());
			RunFacts exact = null;
			for (RunFacts apd : apdRows){
				if (gameId.equals(apd.textOr(FactKeys.RANKING_ID, null))){
					exact = apd;
					break;
				}
			}
			if (exact != null){
				mark(leg, exact, FactKeys.LINK_EXACT);
				continue;
			}
			RunFacts only = null;
			int candidates = 0;
			for (RunFacts apd : apdRows){
				if (apd.isKnown(FactKeys.RANKING_ID) || apd.isKnown(FactKeys.LINKED_LEGACY)) continue;
				if (!sameRun(leg, apd)) continue;
				candidates++;
				only = apd;
			}
			if (candidates == 1) mark(leg, only, FactKeys.LINK_PROBABLE);
		}
	}

	private static boolean sameRun( RunFacts leg, RunFacts apd ){
		String[] texts = { FactKeys.HERO_CLASS };
		String[] nums = { FactKeys.OUTCOME, FactKeys.DEEPEST, FactKeys.HERO_LEVEL, FactKeys.SCORE };
		for (String k : texts){
			if (!leg.isKnown(k) || !apd.isKnown(k) || !leg.textOr(k, "").equals(apd.textOr(k, "!"))) return false;
		}
		for (String k : nums){
			if (!leg.isKnown(k) || !apd.isKnown(k) || leg.numOr(k, -1) != apd.numOr(k, -2)) return false;
		}
		return true;
	}

	private static void mark( RunFacts leg, RunFacts apd, int kind ){
		leg.text(FactKeys.LINKED_RUN, apd.id, Fact.PROV_RECORDED);
		leg.num(FactKeys.LINK_KIND, kind, Fact.PROV_RECORDED);
		apd.text(FactKeys.LINKED_LEGACY, leg.id, Fact.PROV_RECORDED);
		apd.num(FactKeys.LINK_KIND, kind, Fact.PROV_RECORDED);
	}

	//Rows for statistics: every APD row, and the imported records that are not also an APD run. A run is never counted twice.
	public List<RunFacts> rows(){
		List<RunFacts> out = new ArrayList<>(apdRows);
		for (RunFacts leg : legacyRows) if (!leg.isKnown(FactKeys.LINKED_RUN)) out.add(leg);
		return out;
	}

	//every row, linked imported records included
	public List<RunFacts> allRows(){
		List<RunFacts> out = new ArrayList<>(apdRows);
		out.addAll(legacyRows);
		return out;
	}

	public static final class Coverage {
		public final String key;
		public final int known;
		public final int total;

		Coverage( String key, int known, int total ){
			this.key = key;
			this.known = known;
			this.total = total;
		}

		@Override
		public String toString(){
			return known + " of " + total + " runs contain " + key;
		}
	}

	//how many of the given rows contain the fact at all
	public static Coverage coverage( List<RunFacts> rows, String key ){
		int known = 0;
		for (RunFacts r : rows) if (r.isKnown(key)) known++;
		return new Coverage(key, known, rows.size());
	}

	//how many of the given rows have the fact marked incomplete (a lower bound)
	public static int incomplete( List<RunFacts> rows, String key ){
		int n = 0;
		for (RunFacts r : rows){
			Fact f = r.get(key);
			if (f != null && f.prov == Fact.PROV_INCOMPLETE) n++;
		}
		return n;
	}
}
