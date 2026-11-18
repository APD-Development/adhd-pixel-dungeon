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


package com.shatteredpixel.shatteredpixeldungeon.ui;

//APD: the look of UIMSD, in one place. Every UIMSD screen and dialog takes its colours and text sizes from here, so the same kind of thing always
//looks the same: a title is gold, a section heading is a quieter gold with a rule, ordinary text is white, help and metadata are grey, the
//current choice is green, a warning is amber, an error is red. (The colours of VALUES in tables - exact, at least, inferred, unknown, hidden - are
//not here: they carry meaning and live with the table, UimsdTable.color.)
public final class UimsdStyle {

	private UimsdStyle(){}

	public static final int TITLE = 0xFFFF44;       //the same gold as the game's own window titles
	public static final int HEADING = 0xE0B050;     //a section inside a screen or dialog
	public static final int TEXT = 0xFFFFFF;
	public static final int MUTED = 0xAAAAAA;       //help, metadata, descriptions
	public static final int FAINT = 0x888888;
	public static final int CURRENT = 0x99EE99;     //the choice that is on now
	public static final int WARN = 0xFFDD66;
	public static final int ERROR = 0xFF8888;

	//panels behind rows (ARGB)
	public static final int ROW = 0x66332B27;
	public static final int ROW_ON = 0x66335533;
	public static final int ROW_ACTION = 0x66553322;
	public static final int ROW_SELECTED = 0x55FFD966;
	public static final int RULE = 0x55E0B050;

	//the text sizes (the game's font is small: 5 is for metadata, 7 for the main text of a row)
	public static final int SIZE_TITLE = 9;
	public static final int SIZE_TEXT = 7;
	public static final int SIZE_HELP = 6;
	public static final int SIZE_META = 5;
}
