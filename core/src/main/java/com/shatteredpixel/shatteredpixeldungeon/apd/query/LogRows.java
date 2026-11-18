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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Everything one log contributes: its meta and the rows of each dataset. This is what the row cache stores per log.
public final class LogRows {

	public final LogMeta meta = new LogMeta();
	public final Map<String, List<Row>> rows = new LinkedHashMap<>();

	public List<Row> of( String dataset ){
		List<Row> l = rows.get(dataset);
		if (l == null){
			l = new ArrayList<>();
			rows.put(dataset, l);
		}
		return l;
	}
}
