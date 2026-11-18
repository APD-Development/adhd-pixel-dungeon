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

package com.shatteredpixel.shatteredpixeldungeon.apd.backend;

// Retry delay: 30 s, doubling up to 6 h.
public final class Backoff {

	public static final long FIRST_MILLIS = 30_000L, MAX_MILLIS = 6L * 60 * 60 * 1000;

	private Backoff(){}

	public static long delayMillis( int attempts ){
		if (attempts <= 0) return 0;
		long d = FIRST_MILLIS;
		for (int i = 1; i < attempts && d < MAX_MILLIS; i++) d *= 2;
		return Math.min(d, MAX_MILLIS);
	}
}
