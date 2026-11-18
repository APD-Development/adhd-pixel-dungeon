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

//One condition on one column. Queries hold any number of them and a row must pass all of them. A filter never matches a
//row whose value for that column is unknown (or hidden), except "is unknown": the result says how many rows that cost.
public final class Filter {

	public final String column;
	public final Op op;
	//NUM and ENUM columns
	public final long[] nums;
	//TEXT columns
	public final String[] texts;

	public Filter( String column, Op op, long[] nums, String[] texts ){
		this.column = column;
		this.op = op;
		this.nums = nums == null ? new long[0] : nums;
		this.texts = texts == null ? new String[0] : texts;
	}

	public static Filter num( String column, Op op, long... values ){
		return new Filter(column, op, values, null);
	}

	public static Filter text( String column, Op op, String... values ){
		return new Filter(column, op, null, values);
	}

	public static Filter known( String column ){
		return new Filter(column, Op.KNOWN, null, null);
	}

	public static Filter unknown( String column ){
		return new Filter(column, Op.UNKNOWN, null, null);
	}

	@Override
	public boolean equals( Object o ){
		if (!(o instanceof Filter)) return false;
		Filter f = (Filter) o;
		return column.equals(f.column) && op == f.op && java.util.Arrays.equals(nums, f.nums) && java.util.Arrays.equals(texts, f.texts);
	}

	@Override
	public int hashCode(){
		return column.hashCode() * 31 + op.hashCode() + java.util.Arrays.hashCode(nums) * 7 + java.util.Arrays.hashCode(texts);
	}

	@Override
	public String toString(){
		StringBuilder b = new StringBuilder(column).append(' ').append(op.word);
		for (long n : nums) b.append(' ').append(n);
		for (String t : texts) b.append(" \"").append(t).append('"');
		return b.toString();
	}
}
