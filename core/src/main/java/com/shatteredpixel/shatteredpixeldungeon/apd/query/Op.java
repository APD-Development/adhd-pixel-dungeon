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

//What a filter does with its column. Which operators a column offers depends on its type (see ops()).
public enum Op {
	EQ("is", "exactly this"),
	NE("is not", "anything except this"),
	LT("is less than", "a smaller number"),
	LE("is at most", "this or smaller"),
	GT("is more than", "a bigger number"),
	GE("is at least", "this or bigger"),
	BETWEEN("is between", "from one value up to another, both included"),
	CONTAINS("contains", "has this text somewhere in it"),
	IN("is one of", "any of several values"),
	KNOWN("is known", "only entries where this was recorded"),
	UNKNOWN("is unknown", "only entries where this was never recorded"),
    // Drilldown predicates match the grouping key, not the unknown underlying truth.
    // They remain outside ops(): ordinary player-selected filters keep their semantics.
    GROUP_VALUE("group value is", "this recorded value, excluding lower bounds"),
    GROUP_BOUND("group lower bound is", "this recorded lower bound"),
    GROUP_TEXT("group text is", "this exact case-sensitive text"),
    // Navigation follows identities, not case-insensitive player text searches.
    IDENTIFIER_IN("identifier is one of", "these exact case-sensitive identifiers");

	//how a filter reads in a sentence ("Score is at least 100"), and what it does in a few words (for the picker)
	public final String word;
	public final String hint;

	Op( String word, String hint ){
		this.word = word;
		this.hint = hint;
	}

	//the operators that make sense for a column type, in the order a picker should list them
	public static Op[] ops( ColumnDef.Type type ){
		switch (type){
			case NUM:
				return new Op[]{EQ, NE, LT, LE, GT, GE, BETWEEN, KNOWN, UNKNOWN};
			case TEXT:
				return new Op[]{EQ, NE, CONTAINS, IN, KNOWN, UNKNOWN};
			default:
				return new Op[]{EQ, NE, IN, KNOWN, UNKNOWN};
		}
	}

	public boolean takesValue(){
		return this != KNOWN && this != UNKNOWN;
	}
}

