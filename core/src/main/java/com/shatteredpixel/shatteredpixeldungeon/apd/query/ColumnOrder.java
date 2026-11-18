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
import java.util.List;

// Reordering of the visible columns: pure list moves.
public final class ColumnOrder {

	private ColumnOrder(){}

	//moves 'id' by 'delta' places (negative = left); stops at the ends
	public static List<String> move( List<String> cols, String id, int delta ){
		List<String> out = new ArrayList<>(cols);
		int i = out.indexOf(id);
		if (i < 0) return out;
		int j = Math.max(0, Math.min(out.size() - 1, i + delta));
		out.remove(i);
		out.add(j, id);
		return out;
	}

	public static List<String> toFront( List<String> cols, String id ){
		return move(cols, id, -cols.size());
	}

	public static List<String> toBack( List<String> cols, String id ){
		return move(cols, id, cols.size());
	}
}
