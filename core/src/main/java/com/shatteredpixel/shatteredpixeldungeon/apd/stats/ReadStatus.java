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

//How reading a run log ended. In every case the events before the problem are returned intact.
public enum ReadStatus {

	//every frame valid, ended with a seal frame whose event count matches
	SEALED,
	//every frame valid and the file ends on a frame boundary, but there is no seal: the run is in progress, or
	//recording stopped without closing (crash, app killed)
	UNSEALED,
	//the file ends in the middle of a frame (interrupted write). Everything before it is valid.
	TRUNCATED,
	//a frame failed its checksum or is malformed. Everything before it is valid.
	CORRUPT,
	//the seal frame's event count disagrees with the frames actually found
	SEAL_MISMATCH,
	//the header is missing, too short or damaged: the file cannot be interpreted
	BAD_HEADER,
	//written by a newer container format than this build understands
	UNSUPPORTED_CONTAINER;

	//true if no bytes of the file are suspect (a trailing partial/corrupt frame, a bad header...)
	public boolean isClean(){
		return this == SEALED || this == UNSEALED;
	}
}
