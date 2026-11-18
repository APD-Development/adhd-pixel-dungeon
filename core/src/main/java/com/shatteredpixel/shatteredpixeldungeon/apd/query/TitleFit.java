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

//Fits a screen header (the UIMSD screen's full name) into the room beside the exit button: the largest text size at which the name
//wraps into at most two lines (a third line only on a screen so narrow that nothing smaller helps). Pure arithmetic on an estimate
//of the game font, like TableGeometry, so it can be checked for every screen width.
public final class TitleFit {

	private TitleFit(){}

	public static final int MAX_LINES = 2;

	//text sizes to try, largest first
	private static final int[] SIZES = { 10, 9, 8, 7, 6 };

	//a cautious estimate of one character's width at a text size (the table uses 4.2 at size 6, which is 0.7 per size)
	public static float charWidth( int size ){
		return 0.75f * size;
	}

	//how many lines the text takes at that size in that width (words are never split, a word wider than the line gets its own line)
	public static int lines( float available, String text, int size ){
		int perLine = Math.max(1, (int) (available / charWidth(size)));
		int lines = 1, used = 0;
		for (String word : text.split(" ")){
			int len = word.length();
			if (used == 0){
				used = len;
			} else if (used + 1 + len <= perLine){
				used += 1 + len;
			} else {
				lines++;
				used = len;
			}
		}
		return lines;
	}

	public static int size( float available, String text ){
		for (int s : SIZES){
			if (lines(available, text, s) <= MAX_LINES) return s;
		}
		return SIZES[SIZES.length - 1];
	}

}
