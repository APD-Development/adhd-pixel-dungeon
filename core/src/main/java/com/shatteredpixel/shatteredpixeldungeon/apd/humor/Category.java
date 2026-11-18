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

// Where a line belongs. A category also says at which moments it may be shown, so a death joke can only ever appear after a death.
public enum Category {

	ANYTIME( Moment.STARTUP, Moment.MENU, Moment.STATS, Moment.LEVEL_CALM, Moment.BOSS_CLEARED, Moment.RUN_END_DEATH, Moment.RUN_END_VICTORY ),
	// used by the Appreciation pool for lines that fit anywhere
	APPRECIATION( Moment.STARTUP, Moment.MENU, Moment.STATS, Moment.LEVEL_CALM, Moment.BOSS_CLEARED, Moment.RUN_END_DEATH, Moment.RUN_END_VICTORY ),
	FOURTH_WALL( Moment.STARTUP, Moment.MENU, Moment.STATS, Moment.LEVEL_CALM, Moment.RUN_END_DEATH, Moment.RUN_END_VICTORY ),
	MENU( Moment.STARTUP, Moment.MENU ),
	DEATH( Moment.RUN_END_DEATH ),
	VICTORY( Moment.RUN_END_VICTORY ),
	// reached the amulet floor or the last stretch and still did not ascend
	ASCENSION( Moment.RUN_END_DEATH ),
	POSTRUN( Moment.RUN_END_DEATH, Moment.RUN_END_VICTORY ),
	// about a boss the hero has just defeated
	BOSS( Moment.BOSS_CLEARED ),
	// about the class being played right now (in play only)
	PLAYING( Moment.LEVEL_CALM, Moment.BOSS_CLEARED ),
	ABILITY( Moment.LEVEL_CALM, Moment.RUN_END_DEATH, Moment.RUN_END_VICTORY ),
	EQUIPMENT( Moment.LEVEL_CALM, Moment.RUN_END_DEATH, Moment.RUN_END_VICTORY ),
	HISTORY( Moment.STARTUP, Moment.MENU, Moment.STATS, Moment.RUN_END_DEATH, Moment.RUN_END_VICTORY ),
	// the one-time warning when the player first commits to a Cleric run
	FIRST_RUN( Moment.FIRST_CLERIC_RUN ),
	// lines about the grudge system itself (the counter); never drawn by the normal picker
	META( Moment.COUNTER_SCREEN );

	private final Moment[] moments;

	Category( Moment... moments ){
		this.moments = moments;
	}

	public boolean allows( Moment m ){
		for (Moment x : moments) if (x == m) return true;
		return false;
	}
}
