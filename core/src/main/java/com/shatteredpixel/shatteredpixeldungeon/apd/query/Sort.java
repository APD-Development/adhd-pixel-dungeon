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

//Order by a column (or, in a grouped result, by an aggregate's id). Unknown and hidden values always come last, in both
//directions: they are never ranked as zero.
public final class Sort {

	public final String column;
	public final boolean descending;

	public Sort( String column, boolean descending ){
		this.column = column;
		this.descending = descending;
	}

	@Override
	public boolean equals( Object o ){
		return o instanceof Sort && ((Sort) o).column.equals(column) && ((Sort) o).descending == descending;
	}

	@Override
	public int hashCode(){
		return column.hashCode() * 2 + (descending ? 1 : 0);
	}
}
