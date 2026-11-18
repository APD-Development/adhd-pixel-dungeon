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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// What a line may know about the game: facts that really were recorded, as plain text values. A line that needs a fact the context does
// not have is not eligible, so nothing is ever guessed or filled in.
public final class Context {

	// every key a line may use (in `requires` or as a {placeholder}); a pack using any other key fails its lint
	public static final Set<String> KEYS;
	static {
		Set<String> k = new HashSet<>();
		Collections.addAll( k,
				"hero",              // the class being played now, as its code name (for conditions)
				"heroName",          // the same, as the player reads it (for {placeholders})
				"lastHeroName",
				"runVerified",       // 1 only when the game itself confirmed a genuine, permanently ended run (every line about a finished run requires it)
				"lastAscended",      // 1 only when the run that just ended was a completed ascension (not merely a win by leaving), else 0
				"lastAmulet",        // 1 when the hero had taken the Amulet in the run that just ended
				"clericAvailable",   // 1 only while Cleric can actually be chosen for a run (0 while it is blocked)
				"lastHero",          // the class of the run that just ended
				"lastOutcome",       // DIED or WON
				"depth",             // depth the run ended on (or the current depth)
				"killer",            // the creature that killed the hero (only when the cause was a creature)
				"boss",              // a boss the hero has JUST defeated (only at the moment right after it fell)
				"subclass", "spell", "weapon", "armorAbility",
				"clericRuns", "clericWins", "clericDeaths", "clericStreak",
				"guidingLightCasts",
				//what the grudge itself has really shown this player (for the game to notice how often it complains)
				"complaintsHeard", "complaintsTotal", "appreciationsHeard", "appreciationsTotal" );
		KEYS = Collections.unmodifiableSet( k );
	}

	public static final Context EMPTY = new Context( Collections.<String, String>emptyMap() );

	private final Map<String, String> facts;

	private Context( Map<String, String> facts ){
		this.facts = facts;
	}

	public Context with( String key, Object value ){
		if (!KEYS.contains( key )) throw new IllegalArgumentException( "unknown context key " + key );
		Map<String, String> m = new HashMap<>( facts );
		m.put( key, String.valueOf( value ) );
		return new Context( m );
	}

	// this context with every fact of `other` added (other wins on a clash)
	public Context withAll( Context other ){
		Map<String, String> m = new HashMap<>( facts );
		m.putAll( other.facts );
		return new Context( m );
	}

	public String get( String key ){
		return facts.get( key );
	}
}
