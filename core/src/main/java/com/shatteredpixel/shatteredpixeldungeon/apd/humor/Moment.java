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

// The few places where a line may be shown. Each is a quiet point: no turn is running and nothing the player is deciding is waiting.
public enum Moment {

	// the first title screen after the game starts, built and idle (never during loading or recovery)
	STARTUP( true ),
	// the title screen on later visits, built and idle
	MENU( true ),
	// a new floor has fully loaded and the hero is idle with nothing hostile in view
	LEVEL_CALM( true ),
	// a boss was just defeated and the hero is idle again with nothing hostile in view
	BOSS_CLEARED( true ),
	// after the run has permanently ended and its results are on screen
	RUN_END_DEATH( true ),
	RUN_END_VICTORY( true ),
	// the player just picked Cleric to start a run (their own choice)
	FIRST_CLERIC_RUN( false ),
	// a statistics screen (UIMSD, records) the player opened on purpose
	STATS( true ),
	// the player opened the counter screen
	COUNTER_SCREEN( false );

	// a surprise: it counts against the popup budget
	public final boolean surprise;

	Moment( boolean surprise ){
		this.surprise = surprise;
	}

	// the player-facing place this moment belongs to (null for the one-off, player-initiated moments)
	public Location location(){
		switch (this){
			case STARTUP: return Location.STARTUP;
			case MENU: return Location.MENU;
			case LEVEL_CALM: case BOSS_CLEARED: return Location.RUN;
			case RUN_END_DEATH: case RUN_END_VICTORY: return Location.AFTER_RUN;
			case STATS: return Location.STATS;
			default: return null;
		}
	}
}
