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

//One line of a dataset's statistics sheet: what a group of rows (all glaives, all rats) is summed up as in its detail window.
//A sheet is part of the dataset's definition, so every view of the dataset shares it. Purely schema data.
public final class StatLine {

	//a heading line when 'aggregate' is null
	public final String label;
	public final Aggregate aggregate;
	//what the number means, in a sentence, for the detail window
	public final String explanation;

	private StatLine( String label, Aggregate aggregate, String explanation ){
		this.label = label;
		this.aggregate = aggregate;
		this.explanation = explanation == null ? "" : explanation;
	}

	public static StatLine section( String title ){
		return new StatLine(title, null, "");
	}

	public static StatLine of( String label, Aggregate a, String explanation ){
		return new StatLine(label, a, explanation);
	}

	public boolean isSection(){
		return aggregate == null;
	}
}
