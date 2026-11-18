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

package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKind;

//The game's entry point for recording ANY registered kind of fact (see FactKinds). A mechanic declares its kind once and then calls
//record() where the thing happens. Returns at once (one volatile read) unless a run is being recorded, never throws, and never
//changes the game or consumes its random numbers.
//
//What is recorded is exactly what the caller passes: the run, floor, turn, character and so on come from the run's own timeline when
//the facts are read. Only record what the player could know unless the kind is declared internal (hidden truth).
public final class ApdFacts {

	private ApdFacts(){}

	//true while a run is being recorded. Written by ApdRecorder only.
	static volatile boolean active;
	//what ApdPlay records through. Written by ApdRecorder only.
	static volatile PlayTracker play;

	public static boolean isActive(){
		return active;
	}

	//the hero did something to a subject: ApdFacts.record(kind, "GuidingLight", 1, 3)
	public static void record( FactKind kind, String subject, long... values ){
		if (active) ApdRecorder.factRecord(kind, subject, "", values, null);
	}

	//the full form: someone other than the hero (actor), numbers and text tags in the order the kind declares them
	public static void record( FactKind kind, String subject, String actor, long[] values, String[] tags ){
		if (active) ApdRecorder.factRecord(kind, subject, actor, values, tags);
	}
}
