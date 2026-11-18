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

//A question in the player's words, before it is turned into a query: "the average [mana spent] of [spell casts] per [floor visit], by
//[spell], only where [run class is Cleric]". Immutable; each with-method gives a changed copy. The builder screen edits one of these, Questions
//turns it into a query and an answer, and the same question can be opened as an ordinary table for the full query tools.
public final class Question {

	public enum Measure {
		COUNT("how many", Aggregate.Fn.COUNT),
		TOTAL("the total", Aggregate.Fn.SUM),
		AVERAGE("the average", Aggregate.Fn.AVG),
		HIGHEST("the highest", Aggregate.Fn.MAX),
		LOWEST("the lowest", Aggregate.Fn.MIN),
		TYPICAL("the typical (median)", Aggregate.Fn.MEDIAN);

		public final String phrase;
		public final Aggregate.Fn fn;

		Measure( String phrase, Aggregate.Fn fn ){
			this.phrase = phrase;
			this.fn = fn;
		}

		public boolean needsColumn(){
			return this != COUNT;
		}
	}

	public final String dataset;
	public final Measure measure;
	//the number measured (not for a plain count)
	public final String column;
	//divide by runs, floor visits, turns or hours played; null = no
	public final PerUnit.Unit per;
	public final List<String> groupBy;
	public final List<Filter> filters;
	public final Scope scope;
	public final boolean showInternal;

	public Question( String dataset, Measure measure, String column, PerUnit.Unit per, List<String> groupBy, List<Filter> filters, Scope scope, boolean showInternal ){
		this.dataset = dataset;
		this.measure = measure == null ? Measure.COUNT : measure;
		this.column = column == null ? "" : column;
		this.per = per;
		this.groupBy = Collections.unmodifiableList(new ArrayList<>(groupBy));
		this.filters = Collections.unmodifiableList(new ArrayList<>(filters));
		this.scope = scope == null ? Scope.DEFAULT : scope;
		this.showInternal = showInternal;
	}

	public static Question about( String dataset ){
		return new Question(dataset, Measure.COUNT, "", null, new ArrayList<String>(), new ArrayList<Filter>(), Scope.DEFAULT, false);
	}

	public Question withDataset( String d ){
		return about(d).withScope(scope).withShowInternal(showInternal);
	}

	public Question withMeasure( Measure m, String col ){
		//an average of a number per run is not a thing: "per" only goes with a count or a total
		PerUnit.Unit u = (m == Measure.COUNT || m == Measure.TOTAL) ? per : null;
		return new Question(dataset, m, col, u, groupBy, filters, scope, showInternal);
	}

	public Question withPer( PerUnit.Unit u ){
		return new Question(dataset, measure, column, u, groupBy, filters, scope, showInternal);
	}

	public Question withGroupBy( List<String> g ){
		return new Question(dataset, measure, column, per, g, filters, scope, showInternal);
	}

	public Question withFilters( List<Filter> f ){
		return new Question(dataset, measure, column, per, groupBy, f, scope, showInternal);
	}

	public Question addFilter( Filter f ){
		List<Filter> l = new ArrayList<>(filters);
		l.add(f);
		return withFilters(l);
	}

	public Question removeFilter( int index ){
		List<Filter> l = new ArrayList<>(filters);
		l.remove(index);
		return withFilters(l);
	}

	public Question withScope( Scope s ){
		return new Question(dataset, measure, column, per, groupBy, filters, s, showInternal);
	}

	public Question withShowInternal( boolean v ){
		return new Question(dataset, measure, column, per, groupBy, filters, scope, v);
	}

	//How elaborate the question is: every extra thing asked for adds one. Used to notice an absurdly involved question, never to refuse one.
	public int complexity(){
		int c = 1;
		if (measure != Measure.COUNT) c++;
		if (per != null) c++;
		c += groupBy.size();
		c += filters.size();
		if (showInternal) c++;
		return c;
	}
}
