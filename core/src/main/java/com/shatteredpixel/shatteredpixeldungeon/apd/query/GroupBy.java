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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//Group the rows by the values of some columns and compute aggregates for each group.
public final class GroupBy {

	public final List<String> keys;
	public final List<Aggregate> aggregates;

	public GroupBy( List<String> keys, List<Aggregate> aggregates ){
		this.keys = Collections.unmodifiableList(new ArrayList<>(keys));
		this.aggregates = Collections.unmodifiableList(new ArrayList<>(aggregates));
	}

	@Override
	public boolean equals( Object o ){
		return o instanceof GroupBy && ((GroupBy) o).keys.equals(keys) && ((GroupBy) o).aggregates.equals(aggregates);
	}

	@Override
	public int hashCode(){
		return keys.hashCode() * 31 + aggregates.hashCode();
	}
}
