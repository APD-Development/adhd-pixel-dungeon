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

//A QueryResult laid out as a table of text, for any screen to draw: headers (what to call each column, whether tapping it sorts,
//how much of it is known) and lines of CellText. It knows nothing about any particular dataset.
public final class ResultTable {

	public static final class Header {
		//the column id (or an aggregate id in a grouped result): what a sort refers to
		public final String id;
		public final String label;
		public final boolean sortable;
		//Presentation alignment only; values and query behavior are unchanged.
		public final boolean numeric;
		//"17/28": cells known out of cells that may be looked at, "" when it does not apply
		public final String coverage;
		//the widest text in this column, in characters, header included (an estimate for sizing)
		public int chars;
		//+1 ascending, -1 descending, 0 not sorted, and the position among the sort keys (1-based), 0 if none
		public int sortDirection;
		public int sortRank;

		Header( String id, String label, boolean sortable, String coverage ){
			this(id, label, sortable, coverage, false);
		}

		Header( String id, String label, boolean sortable, String coverage, boolean numeric ){
			this.numeric = numeric;
			this.id = id;
			this.label = label;
			this.sortable = sortable;
			this.coverage = coverage;
			this.chars = Math.max(label.length(), coverage.length());
		}
	}

	public static final class Line {
		//a row of an ungrouped result, null for a group
		public final Row row;
		public final GroupRow group;
		public final CellText[] cells;
		//what to show when a group's aggregate is tapped: "9 of 12 known" per aggregate
		public final String[] details;

		Line( Row row, GroupRow group, CellText[] cells, String[] details ){
			this.row = row;
			this.group = group;
			this.cells = cells;
			this.details = details;
		}
	}

	public final List<Header> headers = new ArrayList<>();
	public final List<Line> lines = new ArrayList<>();
	//the page shown and how many lines there are in all
	public final int total;
	public final int pageStart;
	public final int pageSize;
	public final boolean grouped;

	private ResultTable( int total, int pageStart, int pageSize, boolean grouped ){
		this.total = total;
		this.pageStart = pageStart;
		this.pageSize = pageSize;
		this.grouped = grouped;
	}

	public int pageCount(){
		return pageSize <= 0 ? 1 : Math.max(1, (total + pageSize - 1) / pageSize);
	}

	public int page(){
		return pageSize <= 0 ? 0 : pageStart / pageSize;
	}

	public static ResultTable build( QueryResult r, int page, int pageSize ){
		int total = r.isGrouped() ? r.groups.size() : r.rows.size();
		int size = Math.max(1, pageSize);
		int maxPage = Math.max(0, (total + size - 1) / size - 1);
		int p = Math.max(0, Math.min(page, maxPage));
		int start = p * size, end = Math.min(total, start + size);
		ResultTable t = new ResultTable(total, start, size, r.isGrouped());
		Query q = r.query;

		if (!r.isGrouped()){
			for (ColumnDef c : r.columns){
				QueryResult.ColumnCoverage cov = r.coverageOf(c.id);
				String coverage = cov == null ? "" : cov.known + "/" + cov.total + (cov.hidden > 0 ? "+" + cov.hidden + " hidden" : "");
				t.headers.add(new Header(c.id, c.label, true, coverage, c.type == ColumnDef.Type.NUM));
			}
			for (int i = start; i < end; i++){
				Row row = r.rows.get(i);
				CellText[] cells = new CellText[r.columns.size()];
				for (int k = 0; k < cells.length; k++) cells[k] = CellFormatter.cell(q, row, r.columns.get(k));
				t.lines.add(new Line(row, null, cells, null));
			}
		} else {
			for (ColumnDef c : r.groupKeys) t.headers.add(new Header(c.id, c.label, true, "", c.type == ColumnDef.Type.NUM));
			t.headers.add(new Header("count", r.def.countLabel, true, "", true));
			for (Aggregate a : r.aggregates){
				if (a.isPlainCount()) continue;
				t.headers.add(new Header(a.id(), CellFormatter.aggregateLabel(r.def, a), true, "", true));
			}
			for (int i = start; i < end; i++){
				GroupRow g = r.groups.get(i);
				List<CellText> cells = new ArrayList<>();
				for (ColumnDef c : r.groupKeys) cells.add(CellFormatter.cell(q, g.sample, c));
				cells.add(new CellText(Integer.toString(g.n), CellText.EXACT));
				List<String> details = new ArrayList<>();
				for (int k = 0; k < cells.size(); k++) details.add("");
				for (int k = 0; k < r.aggregates.size(); k++){
					Aggregate a = r.aggregates.get(k);
					if (a.isPlainCount()) continue;
					CellFormatter.AggText at = CellFormatter.aggregate(r.def, a, g.aggregates.get(k));
					cells.add(new CellText(at.compact, at.state, at.small));
					details.add(at.detail);
				}
				t.lines.add(new Line(null, g, cells.toArray(new CellText[0]), details.toArray(new String[0])));
			}
		}
		for (int h = 0; h < t.headers.size(); h++){
			Header header = t.headers.get(h);
			for (int s = 0; s < q.sorts.size(); s++){
				if (q.sorts.get(s).column.equals(header.id)){
					header.sortDirection = q.sorts.get(s).descending ? -1 : 1;
					header.sortRank = s + 1;
				}
			}
			for (Line line : t.lines) header.chars = Math.max(header.chars, line.cells[h].text.length());
		}
		return t;
	}
}
