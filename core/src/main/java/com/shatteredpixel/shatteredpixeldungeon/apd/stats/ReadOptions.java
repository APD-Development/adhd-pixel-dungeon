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


package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

public final class ReadOptions {

	//After a damaged frame, scan forward for the next valid frame and keep reading, instead of stopping.
	//Recovered events after a gap are real (checksummed) but the history between is missing.
	public boolean resync = false;

	//Leave events whose type this build does not know out of ReadResult.events (they are still counted).
	public boolean knownTypesOnly = false;

	//Used by history-import validation, which only needs the typed RUN_START
	//identity. Every frame still receives normal validation and event counting.
	//Other readers keep their complete event list unless they opt in.
	public boolean onlyRunStartEvents = false;

	public ReadOptions resync( boolean v ){
		resync = v;
		return this;
	}

	public ReadOptions knownTypesOnly( boolean v ){
		knownTypesOnly = v;
		return this;
	}

	public ReadOptions onlyRunStartEvents( boolean v ){
		onlyRunStartEvents = v;
		return this;
	}
}
