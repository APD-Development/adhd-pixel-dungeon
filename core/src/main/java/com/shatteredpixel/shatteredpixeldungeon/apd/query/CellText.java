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

//How one cell is shown: its text and what kind of knowledge it is. The state decides the colour and the marker, so exact,
//lower-bound, inferred, imported, unknown and hidden values can always be told apart.
public final class CellText {

	public static final int EXACT = 0, LOWER = 1, INFERRED = 2, LEGACY = 3, UNKNOWN = 4, HIDDEN = 5;

	public final String text;
	public final int state;
	//the value rests on very few known values (see CellFormatter.SMALL_SAMPLE): shown dimmed, never as confident as the rest
	public final boolean small;

	public CellText( String text, int state ){
		this(text, state, false);
	}

	public CellText( String text, int state, boolean small ){
		this.text = text;
		this.state = state;
		this.small = small;
	}

	@Override
	public String toString(){
		return text;
	}
}
