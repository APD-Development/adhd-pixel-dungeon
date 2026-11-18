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

// How often surprise popups may appear. The default is the most conservative one that is still on.
public enum Frequency {

	OFF( "Off", 0L, 0.0 ),
	RARE( "Rare", 3L * 24 * 3600 * 1000, 0.30 ),
	RARER( "Rarer", 10L * 24 * 3600 * 1000, 0.15 );

	public static final Frequency DEFAULT = RARER;

	public final String label;
	// at least this long since the last surprise popup
	public final long minGapMs;
	// chance that a session, once the gap has passed, gets a popup at all (rolled once per session)
	public final double chance;

	Frequency( String label, long minGapMs, double chance ){
		this.label = label;
		this.minGapMs = minGapMs;
		this.chance = chance;
	}

	public static Frequency of( String name ){
		for (Frequency f : values()) if (f.name().equals( name )) return f;
		return DEFAULT;
	}

	// the order the settings button cycles through
	public Frequency next(){
		switch (this){
			case RARER: return RARE;
			case RARE: return OFF;
			default: return RARER;
		}
	}
}
