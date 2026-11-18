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

// How many Cleric runs the player really has on record, for the Cleric grudge's history jokes. Counts only finished, non-duplicate runs
// whose class and outcome are recorded; nothing is estimated, and a missing dataset gives zero counts (the grudge then says nothing
// that needs them, because a line whose fact is absent is not eligible).
public final class ClericHistory {

	public final int runs;
	public final int wins;
	public final int deaths;

	private ClericHistory( int runs, int wins, int deaths ){
		this.runs = runs;
		this.wins = wins;
		this.deaths = deaths;
	}

	public static ClericHistory compute( Dataset runs ){
		if (runs == null) return new ClericHistory( 0, 0, 0 );
		int cClass = runs.def.indexOf( FactKeys.HERO_CLASS ), cOutcome = runs.def.indexOf( FactKeys.OUTCOME );
		if (cClass < 0 || cOutcome < 0) return new ClericHistory( 0, 0, 0 );
		int n = 0, won = 0, died = 0;
		for (Row r : runs.rows){
			if (r.duplicate || !r.finished || !r.isKnown( cClass ) || !r.isKnown( cOutcome )
					|| !r.text( cClass ).equalsIgnoreCase( "CLERIC" )) continue;
			n++;
			if (r.num( cOutcome ) == LifecycleEvents.OUTCOME_WON) won++;
			else if (r.num( cOutcome ) == LifecycleEvents.OUTCOME_DIED) died++;
		}
		return new ClericHistory( n, won, died );
	}

	/** Cheap version for the in-game grudge: one summary row per run (DerivedStore's run facts)
	 * instead of the whole UIMSD catalog, which with a large history could use hundreds of MB
	 * during play and crash the game out of memory. Same rules: finished runs with known class
	 * and outcome, each run once. */
	public static ClericHistory fromRunFacts( java.util.List<com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts> rows ){
		if (rows == null) return new ClericHistory( 0, 0, 0 );
		int n = 0, won = 0, died = 0;
		for (com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts r : rows){
			if (r == null || !r.isKnown( FactKeys.OUTCOME )
					|| !"CLERIC".equalsIgnoreCase( r.textOr( FactKeys.HERO_CLASS, "" ) )) continue;
			long outcome = r.numOr( FactKeys.OUTCOME, -1 );
			n++;
			if (outcome == LifecycleEvents.OUTCOME_WON) won++;
			else if (outcome == LifecycleEvents.OUTCOME_DIED) died++;
		}
		return new ClericHistory( n, won, died );
	}
}
