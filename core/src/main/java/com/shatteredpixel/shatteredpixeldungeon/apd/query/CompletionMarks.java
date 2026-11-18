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

package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;

import java.util.Map;
import java.util.TreeMap;

// Per hero class: wins and highest difficulty won, from the Runs table. A win without a recorded difficulty counts as a win but not toward the highest difficulty. No screen uses it yet (marker design is open).
public final class CompletionMarks {

	public static final class Mark {
		public final String heroClass;
		public int runs, wins;
		//0 = no win with a recorded difficulty
		public int bestDifficultyWon;
		//wins whose difficulty was not recorded (runs from before difficulty existed)
		public int winsWithoutDifficulty;

		Mark( String heroClass ){
			this.heroClass = heroClass;
		}
	}

	private CompletionMarks(){}

	public static Map<String, Mark> compute( Dataset runs ){
		Map<String, Mark> out = new TreeMap<>();
		if (runs == null) return out;
		int cClass = runs.def.indexOf(FactKeys.HERO_CLASS), cOutcome = runs.def.indexOf(FactKeys.OUTCOME), cDiff = runs.def.indexOf(FactKeys.DIFFICULTY);
		if (cClass < 0 || cOutcome < 0) return out;
		for (Row r : runs.rows){
			if (r.duplicate || !r.finished || !r.isKnown(cClass)) continue;
			String cls = r.text(cClass);
			Mark m = out.get(cls);
			if (m == null){
				m = new Mark(cls);
				out.put(cls, m);
			}
			m.runs++;
			if (r.isKnown(cOutcome) && r.num(cOutcome) == LifecycleEvents.OUTCOME_WON){
				m.wins++;
				if (cDiff >= 0 && r.isKnown(cDiff)) m.bestDifficultyWon = Math.max(m.bestDifficultyWon, (int) r.num(cDiff));
				else m.winsWithoutDifficulty++;
			}
		}
		return out;
	}
}
