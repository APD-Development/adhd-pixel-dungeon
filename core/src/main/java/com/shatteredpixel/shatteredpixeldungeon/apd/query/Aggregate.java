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

//A number computed for each group. Only known values take part; the result says how many did.
//
//Three optional refinements apply to any function:
//  where:    only rows where a column has a given value take part ("win rate when equipped" is a win rate over the rows where
//            "ever equipped" is yes). A row whose value for that column is unknown, or only a lower bound, cannot be judged: it is
//            left out and the result says so (AggValue.UNSURE).
//  distinct: rows are first reduced to one per distinct value of a column ("per run": a run that had three of something counts once).
//  over:     RATIO only, the second column: the sum of 'column' divided by the sum of 'over'.
public final class Aggregate {

	public enum Fn { COUNT, COUNT_KNOWN, SUM, AVG, MIN, MAX, RATE, COUNT_DISTINCT, MEDIAN, RATIO }

	public final Fn fn;
	//the column (COUNT has none)
	public final String column;
	//RATE: the value of the column that counts as a hit (a win rate is outcome = won)
	public final long target;
	//RATIO: the denominator column, "" otherwise
	public final String over;
	//the condition on rows, "" for none
	public final String whereColumn;
	public final long whereValue;
	//one row per distinct value of this column, "" for none
	public final String distinctBy;
	//what a table header calls it, "" for the generated name. Not part of what the aggregate is.
	public final String label;

	public Aggregate( Fn fn, String column, long target ){
		this(fn, column, target, "", "", 0, "", "");
	}

	public Aggregate( Fn fn, String column, long target, String over, String whereColumn, long whereValue, String distinctBy ){
		this(fn, column, target, over, whereColumn, whereValue, distinctBy, "");
	}

	public Aggregate( Fn fn, String column, long target, String over, String whereColumn, long whereValue, String distinctBy, String label ){
		this.fn = fn;
		this.column = column == null ? "" : column;
		this.target = target;
		this.over = over == null ? "" : over;
		this.whereColumn = whereColumn == null ? "" : whereColumn;
		this.whereValue = this.whereColumn.isEmpty() ? 0 : whereValue;
		this.distinctBy = distinctBy == null ? "" : distinctBy;
		this.label = label == null ? "" : label;
	}

	public static Aggregate count(){
		return new Aggregate(Fn.COUNT, null, 0);
	}

	public static Aggregate of( Fn fn, String column ){
		return new Aggregate(fn, column, 0);
	}

	public static Aggregate rate( String column, long target ){
		return new Aggregate(Fn.RATE, column, target);
	}

	//how many different values of a column (text or number): "runs containing it" is the distinct count of the run column
	public static Aggregate distinct( String column ){
		return new Aggregate(Fn.COUNT_DISTINCT, column, 0);
	}

	//the sum of one column over the sum of another
	public static Aggregate ratio( String numerator, String denominator ){
		return new Aggregate(Fn.RATIO, numerator, 0, denominator, "", 0, "", "");
	}

	public Aggregate where( String column, long value ){
		return new Aggregate(fn, this.column, target, over, column, value, distinctBy, label);
	}

	public Aggregate perDistinct( String column ){
		return new Aggregate(fn, this.column, target, over, whereColumn, whereValue, column, label);
	}

	//the plain row count of a group, which every grouped table shows as its own column ("Rows", "Instances", ...)
	public boolean isPlainCount(){
		return fn == Fn.COUNT && whereColumn.isEmpty() && distinctBy.isEmpty();
	}

	public Aggregate labeled( String text ){
		return new Aggregate(fn, column, target, over, whereColumn, whereValue, distinctBy, text);
	}

	//the id a Sort can refer to (and what tells two aggregates apart)
	public String id(){
		String base = fn == Fn.COUNT ? "count" : fn == Fn.RATE ? "rate:" + column + "=" + target
				: fn == Fn.RATIO ? "ratio:" + column + "/" + over : fn.name().toLowerCase(java.util.Locale.ROOT) + ":" + column;
		if (!whereColumn.isEmpty()) base += "|where:" + whereColumn + "=" + whereValue;
		if (!distinctBy.isEmpty()) base += "|per:" + distinctBy;
		return base;
	}

	@Override
	public boolean equals( Object o ){
		return o instanceof Aggregate && ((Aggregate) o).id().equals(id()) && ((Aggregate) o).fn == fn;
	}

	@Override
	public int hashCode(){
		return id().hashCode();
	}
}

