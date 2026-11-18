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

//Where the columns and rows of a ResultTable go, in screen units. Pure arithmetic with no drawing, so the layout for portrait and
//landscape and for any dataset can be tested. Column widths come from the widest text, within limits; the first column is kept
//apart (the screen freezes it while the rest scrolls sideways).
public final class TableGeometry {

	public static final float CHAR_WIDTH = 4.2f;   //an estimate for the game's small font
	public static final float PAD = 6f;
	public static final float MIN_COLUMN = 24f;
	public static final float MAX_COLUMN = 84f;
	public static final float MAX_FIRST = 64f;
	public static final float ROW_HEIGHT = 11f;
	public static final float HEADER_HEIGHT = 30f;     //two lines of label and a line of "how much is known"

	public final float[] widths;
	//left edge of each column within the whole table
	public final float[] lefts;
	public final float totalWidth;
	public final int rows;

	private TableGeometry( float[] widths, int rows ){
		this.widths = widths;
		this.rows = rows;
		this.lefts = new float[widths.length];
		float x = 0;
		for (int i = 0; i < widths.length; i++){
			lefts[i] = x;
			x += widths[i];
		}
		this.totalWidth = x;
	}

	public static TableGeometry of( ResultTable t ){
		float[] w = new float[t.headers.size()];
		for (int i = 0; i < w.length; i++){
			float cap = i == 0 ? MAX_FIRST : MAX_COLUMN;
			w[i] = Math.max(MIN_COLUMN, Math.min(cap, t.headers.get(i).chars * CHAR_WIDTH + PAD));
		}
		return new TableGeometry(w, t.lines.size());
	}

	public float firstWidth(){
		return widths.length == 0 ? 0 : widths[0];
	}

	//the width of everything except the first column
	public float scrollingWidth(){
		return totalWidth - firstWidth();
	}

	public float bodyHeight(){
		return rows * ROW_HEIGHT;
	}

	//the column whose span contains x (measured from the left edge of the whole table), -1 if none
	public int columnAt( float x ){
		for (int i = 0; i < widths.length; i++) if (x >= lefts[i] && x < lefts[i] + widths[i]) return i;
		return -1;
	}

	//the row at y (measured from the top of the body), -1 if none
	public int rowAt( float y ){
		if (y < 0) return -1;
		int r = (int) (y / ROW_HEIGHT);
		return r < rows ? r : -1;
	}

	//the longest text a cell of this column can show (the rest is cut)
	public int maxChars( int column ){
		return Math.max(3, (int) ((widths[column] - PAD) / CHAR_WIDTH));
	}

	//A header label on at most two lines of 'maxChars': it breaks at a space, so a long heading ("True enchantment when generated") is read in full
	//instead of being cut. Only a label too long even for two lines is cut (and then with "..").
	public static String[] twoLines( String label, int maxChars ){
		if (label.length() <= maxChars) return new String[]{ label, "" };
		int cut = label.lastIndexOf(' ', maxChars);
		if (cut <= 0) cut = maxChars;      //one long word: break it
		String first = label.substring(0, cut).trim();
		String rest = label.substring(cut).trim();
		return new String[]{ first, fit(rest, maxChars) };
	}

	public static String fit( String text, int maxChars ){
		if (text.length() <= maxChars) return text;
		if (maxChars <= 2) return text.substring(0, Math.max(0, maxChars));
		return text.substring(0, maxChars - 2) + "..";
	}
}
