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

//What a query produced, with everything needed to show it honestly: how many rows there were, what the scope and each
//filter cost, and how much of each column is actually known.
public final class QueryResult {

	public static final class ColumnCoverage {
		public final ColumnDef column;
		public final int total;       //rows in the result whose cell for this column may be looked at
		public final int known;
		public final int lowerBound;  //known, but only a lower bound
		public final int inferred;
		public final int hidden;      //rows whose cell is hidden information of an unfinished run

		public ColumnCoverage( ColumnDef column, int total, int known, int lowerBound, int inferred, int hidden ){
			this.column = column;
			this.total = total;
			this.known = known;
			this.lowerBound = lowerBound;
			this.inferred = inferred;
			this.hidden = hidden;
		}
	}

	public static final class FilterReport {
		public final Filter filter;
		public int in;
		public int matched;
		public int notMatched;
		public int unknown;      //excluded because the value is unknown
		public int uncertain;    //could not be decided because the value is only a lower bound
		public int hidden;       //excluded because the value is hidden information

		public FilterReport( Filter filter ){
			this.filter = filter;
		}
	}

	public static final class Stats {
		//rows the player may legitimately know about (before scope)
		public int total;
		public int inScope;
		public int matching;
		//left out by scope, each counted so the player can include them
		public int otherRulesets;
		public int unknownRuleset;
		public int mixedRuleset;
		public int unfinished;
		//how many different rulesets the rows in scope were played under (rows with an unknown ruleset count as one more)
		public int rulesetsInScope;
		public int duplicates;
		//rows of FINISHED runs that exist only as hidden information (shown when hidden information is on)
		public int hiddenRows;
		public int hiddenSmallGroups;
		public int hiddenSmallGroupRows;
		//runs in scope and how many of them can have rows in this dataset at all
		public int runsInScope;
		public int runsCovered;
		public final List<FilterReport> filters = new ArrayList<>();
	}

	public final Query query;
	public final DatasetDef def;
	public final List<ColumnDef> columns;
	public final List<Row> rows;
	public final List<GroupRow> groups;
	public final List<ColumnDef> groupKeys;
	public final List<Aggregate> aggregates;
	public final List<ColumnCoverage> coverage;
	public final Stats stats;
	//parts of the query that could not be applied, as sentences
	public final List<String> problems;

	QueryResult( Query query, DatasetDef def, List<ColumnDef> columns, List<Row> rows, List<GroupRow> groups,
	             List<ColumnDef> groupKeys, List<Aggregate> aggregates, List<ColumnCoverage> coverage, Stats stats,
	             List<String> problems ){
		this.query = query;
		this.def = def;
		this.columns = columns;
		this.rows = rows;
		this.groups = groups;
		this.groupKeys = groupKeys;
		this.aggregates = aggregates;
		this.coverage = coverage;
		this.stats = stats;
		this.problems = problems;
	}

	public boolean isGrouped(){
		return groups != null;
	}

	//May the cell of this row for this column be looked at? Hidden information of an unfinished run never may.
	public boolean open( Row row, ColumnDef col ){
		return QueryEngine.open(query, row, col);
	}

	public ColumnCoverage coverageOf( String columnId ){
		for (ColumnCoverage c : coverage) if (c.column.id.equals(columnId)) return c;
		return null;
	}
}
