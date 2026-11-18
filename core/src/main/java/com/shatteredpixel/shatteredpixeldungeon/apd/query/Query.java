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

//Everything that decides what a UIMSD screen shows, as plain immutable data: which dataset, which scope, which filters, how
//it is sorted, which columns, whether it is grouped, and whether hidden information of FINISHED runs may be shown. A saved
//query later is just this, written out (see QueryCodec).
public final class Query {

	public final String dataset;
	public final Scope scope;
	public final List<Filter> filters;
	public final List<Sort> sorts;
	//the visible columns in order, empty: the dataset's default columns
	public final List<String> columns;
	public final GroupBy group;
	//The player's wish to see hidden information. It only ever applies to rows of finished runs: for an unfinished run the
	//engine ignores it (see QueryEngine).
	public final boolean showInternal;
	//groups with fewer rows than this are listed as hidden for a small sample (0 or 1: all groups are shown)
	public final int minSample;

	public Query( String dataset, Scope scope, List<Filter> filters, List<Sort> sorts, List<String> columns, GroupBy group,
	              boolean showInternal, int minSample ){
		this.dataset = dataset;
		this.scope = scope == null ? Scope.DEFAULT : scope;
		this.filters = Collections.unmodifiableList(new ArrayList<>(filters));
		this.sorts = Collections.unmodifiableList(new ArrayList<>(sorts));
		this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
		this.group = group;
		this.showInternal = showInternal;
		this.minSample = Math.max(0, minSample);
	}

	public static Query of( String dataset ){
		return new Query(dataset, Scope.DEFAULT, new ArrayList<Filter>(), new ArrayList<Sort>(), new ArrayList<String>(), null, false, 0);
	}

	public Query withScope( Scope s ){
		return new Query(dataset, s, filters, sorts, columns, group, showInternal, minSample);
	}

	public Query withFilters( List<Filter> f ){
		return new Query(dataset, scope, f, sorts, columns, group, showInternal, minSample);
	}

	public Query addFilter( Filter f ){
		List<Filter> l = new ArrayList<>(filters);
		l.add(f);
		return withFilters(l);
	}

	public Query replaceFilter( int index, Filter f ){
		List<Filter> l = new ArrayList<>(filters);
		l.set(index, f);
		return withFilters(l);
	}

	public Query removeFilter( int index ){
		List<Filter> l = new ArrayList<>(filters);
		l.remove(index);
		return withFilters(l);
	}

	public Query withSorts( List<Sort> s ){
		return new Query(dataset, scope, filters, s, columns, group, showInternal, minSample);
	}

	public Query withColumns( List<String> c ){
		return new Query(dataset, scope, filters, sorts, c, group, showInternal, minSample);
	}

	public Query withGroup( GroupBy g ){
		return new Query(dataset, scope, filters, sorts, columns, g, showInternal, minSample);
	}

	public Query withShowInternal( boolean v ){
		return new Query(dataset, scope, filters, sorts, columns, group, v, minSample);
	}

	public Query withMinSample( int n ){
		return new Query(dataset, scope, filters, sorts, columns, group, showInternal, n);
	}

	public Query withDataset( String id ){
		return new Query(id, scope, filters, sorts, columns, group, showInternal, minSample);
	}

	//Clicking a column header: ascending, then descending, then off. The clicked column becomes the primary sort.
	public Query cycleSort( String column ){
		boolean found = false, desc = false;
		for (Sort s : sorts) if (s.column.equals(column)){ found = true; desc = s.descending; break; }
		List<Sort> l = new ArrayList<>();
		if (!found){
			l.add(new Sort(column, false));
		} else if (!desc){
			l.add(new Sort(column, true));
		}
		for (Sort s : sorts) if (!s.column.equals(column)) l.add(s);
		while (l.size() > 3) l.remove(l.size() - 1);
		return withSorts(l);
	}

	@Override
	public boolean equals( Object o ){
		if (!(o instanceof Query)) return false;
		Query q = (Query) o;
		return dataset.equals(q.dataset) && scope.equals(q.scope) && filters.equals(q.filters) && sorts.equals(q.sorts)
				&& columns.equals(q.columns) && (group == null ? q.group == null : group.equals(q.group))
				&& showInternal == q.showInternal && minSample == q.minSample;
	}

	@Override
	public int hashCode(){
		return dataset.hashCode() * 31 + filters.hashCode() + sorts.hashCode() + columns.hashCode() + (showInternal ? 1 : 0) + minSample;
	}
}
