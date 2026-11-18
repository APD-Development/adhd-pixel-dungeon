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

// The places a surprise Cleric message may appear. Each one is a switch the player controls on the Grudge page; any combination is allowed,
// including all or none.
public enum Location {

	STARTUP( "Game startup", true ),
	MENU( "Main menu", true ),
	RUN( "During dungeon runs", true ),
	AFTER_RUN( "After a run", true ),
	STATS( "Statistics screens", false );

	public final String label;
	public final boolean defaultOn;

	Location( String label, boolean defaultOn ){
		this.label = label;
		this.defaultOn = defaultOn;
	}
}
