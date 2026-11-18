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

//Sizes for UIMSD's dialogs, worked out from the screen they open on instead of from fixed numbers: the width follows the screen
//(never wider than it leaves room for), the list is as tall as its content up to most of the screen height and scrolls beyond that,
//and rows stack without gaps or overlaps whatever their text needs. Pure arithmetic, so it is tested without a screen.
public final class WindowGeometry {

	private WindowGeometry(){}

	public static final float SIDE_MARGIN = 14f;       //kept free on each side of the window, chrome included
	public static final float MIN_WIDTH = 90f;
	public static final float PORTRAIT_WIDTH = 120f;
	public static final float LANDSCAPE_WIDTH = 180f;
	public static final float MAX_HEIGHT_SHARE = 0.85f;
	public static final float MIN_PANE = 40f;
	public static final float ROW_PAD = 3f;
	public static final float MIN_ROW = 15f;
	public static final float TEXT_INSET = 4f;

	//the window width on a screen 'screenWidth' units wide
	public static int width( boolean landscape, float screenWidth ){
		float preferred = landscape ? LANDSCAPE_WIDTH : PORTRAIT_WIDTH;
		float room = screenWidth - 2 * SIDE_MARGIN;
		//On a narrow viewport the available space wins over our preferred minimum.
		//Wrapped labels keep their font size; forcing MIN_WIDTH here pushes the chrome off screen.
		return (int) Math.max(1f, Math.min(preferred, room));
	}

	//how tall the scrolling part may be: most of the screen, less what sits above and below it
	public static float maxPane( float screenHeight, float above, float below ){
		return Math.max(MIN_PANE, screenHeight * MAX_HEIGHT_SHARE - above - below);
	}

	public static float paneHeight( float contentHeight, float maxPane ){
		return Math.min(contentHeight, maxPane);
	}

	public static boolean scrolls( float contentHeight, float maxPane ){
		return contentHeight > maxPane;
	}

	//the width text may use inside a list row
	public static int rowTextWidth( int windowWidth ){
		return (int) (windowWidth - 2 * ROW_PAD - TEXT_INSET);
	}

	public static float rowHeight( float textHeight ){
		return Math.max(MIN_ROW, textHeight + 2 * ROW_PAD);
	}

	//Action labels retain their font size; the touch target grows after wrapping.
	public static float actionHeight(float textHeight){
		return Math.max(22f, textHeight + 2 * ROW_PAD);
	}

	//the top of each row when the rows are stacked, and the total height at the end
	public static float[] stack( float[] heights ){
		float[] tops = new float[heights.length + 1];
		float y = 0;
		for (int i = 0; i < heights.length; i++){
			tops[i] = y;
			y += heights[i];
		}
		tops[heights.length] = y;
		return tops;
	}

	//which stacked row contains y (measured from the top of the list), -1 if none
	public static int rowAt( float[] tops, float y ){
		for (int i = 0; i + 1 < tops.length; i++) if (y >= tops[i] && y < tops[i + 1]) return i;
		return -1;
	}
}
