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

package com.shatteredpixel.shatteredpixeldungeon.apd.humor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

// Chooses which line to show. Unseen lines always come first (the newest unseen ones favoured, and follow-ups whose first part was seen); only when every eligible line has been
// seen does it repeat, and then it leaves out whatever was shown recently and takes among the longest-unseen. It uses only the random
// source it is given, never the game's, so choosing a line cannot change anything in a run.
public final class Picker {

	// lines shown most recently (across all categories) that a repeat avoids when there is anything else
	static final int RECENT_WINDOW = 200;

	private Picker(){}

	public static Line pick( List<Line> pool, Moment moment, Context ctx, Ledger ledger, Random rng, Category avoid ){
		return pick( pool, moment, ctx, ledger, rng, avoid, Long.MAX_VALUE, Long.MAX_VALUE );
	}

	public static Line pick( List<Line> pool, Moment moment, Context ctx, Ledger ledger, Random rng, Category avoid, long sessionStart ){
		return pick( pool, moment, ctx, ledger, rng, avoid, sessionStart, Long.MAX_VALUE );
	}

	// sessionStart and now: see Line.eligible
	public static Line pick( List<Line> pool, Moment moment, Context ctx, Ledger ledger, Random rng, Category avoid, long sessionStart, long now ){
		List<Line> eligible = new ArrayList<>();
		for (Line l : pool) if (l.eligible( moment, ctx, ledger, sessionStart, now )) eligible.add( l );
		if (eligible.isEmpty()) return null;

		//not the same kind of joke twice running, when there is a choice
		if (avoid != null){
			List<Line> other = new ArrayList<>();
			for (Line l : eligible) if (l.category != avoid) other.add( l );
			if (!other.isEmpty()) eligible = other;
		}

		List<Line> unseen = new ArrayList<>();
		for (Line l : eligible) if (ledger.get( l.id ) == null) unseen.add( l );
		if (!unseen.isEmpty()) return weighted( unseen, rng );

		return longestUnseen( eligible, ledger, rng );
	}

	private static Line weighted( List<Line> lines, Random rng ){
		int newest = 0;
		for (Line l : lines) newest = Math.max( newest, l.since );
		long total = 0;
		for (Line l : lines) total += weightOf( l, newest );
		long roll = (long)(rng.nextDouble() * total);
		for (Line l : lines){
			roll -= weightOf( l, newest );
			if (roll < 0) return l;
		}
		return lines.get( lines.size() - 1 );
	}

	private static long weightOf( Line l, int newest ){
		long w = (l.since == newest && newest > 0) ? l.weight * 2L : l.weight;
		//the second half of a story is favoured once it is possible, so the payoff does arrive
		return l.after != null ? w * 4 : w;
	}

	private static Line longestUnseen( List<Line> eligible, final Ledger ledger, Random rng ){
		int window = Math.min( RECENT_WINDOW, eligible.size() / 2 );
		long cutoff = ledger.seq() - window;
		List<Line> rested = new ArrayList<>();
		for (Line l : eligible) if (ledger.get( l.id ).lastSeq <= cutoff) rested.add( l );
		if (rested.isEmpty()) rested = new ArrayList<>( eligible );
		Collections.sort( rested, new Comparator<Line>(){
			@Override
			public int compare( Line a, Line b ){
				return Long.compare( ledger.get( a.id ).lastSeq, ledger.get( b.id ).lastSeq );
			}
		} );
		int pool = Math.max( 1, rested.size() / 4 );
		return rested.get( rng.nextInt( pool ) );
	}
}
