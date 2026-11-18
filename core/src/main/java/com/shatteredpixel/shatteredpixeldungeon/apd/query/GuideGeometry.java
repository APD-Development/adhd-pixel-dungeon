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

//Where the tour's instruction panel goes: over the lower part of the screen, above the bottom buttons, unless the thing it points at is
//there, in which case it moves up under the header. Pure numbers, so it can be checked for every screen shape.
public final class GuideGeometry {

	private GuideGeometry(){}

	public static final float MIN_PANEL = 40f;

	//The top of a panel of height 'panelH' that fits between 'top' and 'bottom' and keeps off the target (a rectangle, or null).
	//Falls back to the lower position when the target is too big for either to be clear (the panel is never taller than the room).
	public static float placeTop( float top, float bottom, float panelH, float tx, float ty, float tw, float th, boolean hasTarget ){
		float room = bottom - top;
		float h = Math.min(panelH, room);
		float low = bottom - h;
		float high = top;
		if (!hasTarget) return low;
		boolean lowHits = ty < low + h && ty + th > low;
		if (!lowHits) return low;
		boolean highHits = ty < high + h && ty + th > high;
		return highHits ? low : high;
	}

	//the height the panel needs for a number of text lines, buttons included, capped at a share of the screen
	public static float panelHeight( int textLines, float lineH, float buttonsH, float screenH ){
		float want = 8 + textLines * lineH + buttonsH + 6;
		return Math.max(MIN_PANEL, Math.min(want, screenH * 0.45f));
	}
}
