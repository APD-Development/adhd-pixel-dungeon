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

//A way to move from a row of one dataset to the matching rows of another: "Items in this run" is a link from the Runs
//dataset to the Items dataset where the Items column "item.run" equals the Runs column "run.id". Purely schema data.
public final class LinkDef {

	public final String label;
	public final String targetView;
	public final String ourColumn;
	public final String targetColumn;

	public LinkDef( String label, String targetView, String ourColumn, String targetColumn ){
		this.label = label;
		this.targetView = targetView;
		this.ourColumn = ourColumn;
		this.targetColumn = targetColumn;
	}
}
