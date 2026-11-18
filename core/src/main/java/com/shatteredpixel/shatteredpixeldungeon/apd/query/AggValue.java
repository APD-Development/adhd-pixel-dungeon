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


package com.shatteredpixel.shatteredpixeldungeon.apd.query;

//The result of one aggregate for one group, with how many values it is made of.
public final class AggValue {

	public static final int LOWER = 1;     //some contributing value is only a lower bound: the real result is at least this
	public static final int INFERRED = 2;  //some contributing value was inferred, not recorded exactly
	public static final int LEGACY = 4;    //some contributing value comes from the rankings file
	public static final int UNSURE = 8;    //some rows could not be judged exactly by the condition or distinct key

	public final boolean known;
	public final double value;
	//RATE: hits and the number of values judged
	public final long hits;
	//how many values took part, and out of how many rows
	public final int contributing;
	public final int total;
	public final int flags;

	//RATIO: what the hits are out of (the sum of the denominator column)
	public final long outOf;

	public AggValue( boolean known, double value, long hits, int contributing, int total, int flags ){
		this(known, value, hits, contributing, total, flags, 0);
	}

	public AggValue( boolean known, double value, long hits, int contributing, int total, int flags, long outOf ){
		this.outOf = outOf;
		this.known = known;
		this.value = value;
		this.hits = hits;
		this.contributing = contributing;
		this.total = total;
		this.flags = flags;
	}

	public boolean isLowerBound(){
		return (flags & LOWER) != 0;
	}

	public boolean isUnsure(){
		return (flags & UNSURE) != 0;
	}

	public boolean isInferred(){
		return (flags & INFERRED) != 0;
	}

	public boolean isLegacy(){
		return (flags & LEGACY) != 0;
	}
}
