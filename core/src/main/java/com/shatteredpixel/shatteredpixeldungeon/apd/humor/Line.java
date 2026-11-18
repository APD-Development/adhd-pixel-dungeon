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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// One authored line. The id is permanent: it is how a player's history remembers the line, so it is never renumbered or reused.
public final class Line {

	static final Pattern PLACEHOLDER = Pattern.compile( "\\{([A-Za-z]+)\\}" );

	public final String id;
	public final Category category;
	// version that introduced the line, as major*1000+minor (1.10 -> 1010); newer lines are served first among the unseen
	public final int since;
	// counts toward the complaint counter (everything but the grudge system's own meta lines)
	public final boolean counted;
	// an obvious exaggeration with invented numbers; never mixed with recorded data
	public final boolean joke;
	public final int weight;
	public final Pool pool;
	// a follow-up: shown only after this line (by id) has really been seen, and with nextSession only in a later session than that
	public final String after;
	public final boolean nextSession;
	// a long-delayed punchline: at least this many days after the first part was last seen (0: no delay)
	public final int delayDays;
	// the first part of a story: some other line must follow it (checked when the packs load)
	public final boolean setup;
	final List<Term> requires;
	public final String clean;
	// optional stronger wording, shown only when Adult humor is on
	public final String adult;

	Line( String id, Category category, int since, boolean counted, boolean joke, int weight, Pool pool, String after, boolean nextSession,
	      int delayDays, boolean setup, List<Term> requires, String clean, String adult ){
		this.id = id;
		this.category = category;
		this.since = since;
		this.counted = counted;
		this.joke = joke;
		this.weight = weight;
		this.pool = pool;
		this.after = after;
		this.nextSession = nextSession;
		this.delayDays = delayDays;
		this.setup = setup;
		this.requires = requires;
		this.clean = clean;
		this.adult = adult;
	}

	// sessionStart: when this run of the game began; a nextSession follow-up needs its prerequisite to have been seen before that
	public boolean eligible( Moment m, Context c, Ledger ledger, long sessionStart, long now ){
		if (!category.allows( m )) return false;
		if (after != null){
			Ledger.Seen first = ledger.get( after );
			if (first == null) return false;
			//both rules count from the first time the player was really shown the first part (never from install or eligibility)
			if (nextSession && first.firstAt >= sessionStart) return false;
			if (delayDays > 0 && now - first.firstAt < delayDays * 86_400_000L) return false;
		}
		for (Term t : requires) if (!t.holds( c )) return false;
		return render( c, false ) != null;
	}

	// The text to show, or null when it needs a fact the context lacks. Adult wording is used only when asked for AND the line has some.
	public String render( Context c, boolean adultHumor ){
		String text = (adultHumor && adult != null) ? adult : clean;
		Matcher m = PLACEHOLDER.matcher( text );
		StringBuffer out = new StringBuffer();
		while (m.find()){
			String v = c.get( m.group( 1 ) );
			if (v == null) return null;
			m.appendReplacement( out, Matcher.quoteReplacement( v ) );
		}
		m.appendTail( out );
		return out.toString();
	}
}
