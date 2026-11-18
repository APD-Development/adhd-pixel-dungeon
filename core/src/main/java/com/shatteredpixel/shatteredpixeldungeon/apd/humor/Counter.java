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

// The four numbers on the counter screen. Each pool has `total` (authored lines in the installed version) and `heard` (distinct ones this
// player has been shown, each counted once however often it appeared). Only counts are ever shown, never which lines are still unseen.
public final class Counter {

	public final int total;
	public final int heard;
	public final int appreciationTotal;
	public final int appreciationHeard;

	Counter( int total, int heard, int appreciationTotal, int appreciationHeard ){
		this.total = total;
		this.heard = heard;
		this.appreciationTotal = appreciationTotal;
		this.appreciationHeard = appreciationHeard;
	}

	public String totalText(){
		return "I'VE COMPLAINED ABOUT CLERIC " + total + " TIMES.";
	}

	public String heardText(){
		return "YOU'VE HEARD ME COMPLAIN " + heard + " TIMES.";
	}

	public String appreciationTotalText(){
		return "I'VE APPRECIATED CLERIC " + appreciationTotal + " TIMES.";
	}

	public String appreciationHeardText(){
		return "YOU'VE WITNESSED " + appreciationHeard + " QUESTIONABLE COMPLIMENTS.";
	}
}
