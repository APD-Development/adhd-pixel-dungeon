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

import java.util.List;

//One group of a grouped result. 'sample' is any row of the group: all rows of a group have the same key values, so the key
//cells are read from it.
public final class GroupRow {

	public final Row sample;
	public final int n;
	public final List<AggValue> aggregates;
	//the rows of the group (a view for drilling in, not a copy)
	public final List<Row> rows;

	public GroupRow( Row sample, int n, List<AggValue> aggregates, List<Row> rows ){
		this.sample = sample;
		this.n = n;
		this.aggregates = aggregates;
		this.rows = rows;
	}
}
