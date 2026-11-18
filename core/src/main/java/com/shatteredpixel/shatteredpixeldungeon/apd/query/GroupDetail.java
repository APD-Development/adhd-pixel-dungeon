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

//The statistics sheet of one group of a grouped table (all War Hammers, all rats): the dataset's sheet worked out over the
//group's rows, each statistic with how many values it rests on and what kind of knowledge it is. Pure data for a window to show.
//
//The sheet is computed under the same query as the table, so hidden information follows the same rules: a statistic that needs
//hidden information while it is off is not shown at all, and the hidden values of an unfinished run never take part.
public final class GroupDetail {

	public static final class Line {
		public final boolean section;
		public final String label;
		public final String value;
		//"9 of 12 known; ..." and the like
		public final String detail;
		public final String explanation;
		public final int state;
		public final boolean small;

		Line( boolean section, String label, String value, String detail, String explanation, int state, boolean small ){
			this.section = section;
			this.label = label;
			this.value = value;
			this.detail = detail;
			this.explanation = explanation;
			this.state = state;
			this.small = small;
		}
	}

	public final String title;
	public final int rows;
	public final List<Line> keys = new ArrayList<>();
	public final List<Line> lines = new ArrayList<>();

	private GroupDetail( String title, int rows ){
		this.title = title;
		this.rows = rows;
	}

	public static GroupDetail build( QueryResult r, GroupRow g ){
		StringBuilder title = new StringBuilder();
		List<Line> keys = new ArrayList<>();
		for (ColumnDef c : r.groupKeys){
			CellText t = CellFormatter.cell(r.query, g.sample, c);
			if (title.length() > 0) title.append(" / ");
			title.append(t.text);
			keys.add(new Line(false, c.label, t.text, t.state == CellText.EXACT ? "" : CellFormatter.explain(t.state), "", t.state, false));
		}
		GroupDetail d = new GroupDetail(title.length() == 0 ? "Group" : title.toString(), g.n);
		d.keys.addAll(keys);
		if (r.def.sheet.isEmpty()){
			//a dataset without a sheet: what the table computed
			for (int i = 0; i < r.aggregates.size(); i++){
				Aggregate a = r.aggregates.get(i);
				if (a.isPlainCount()) continue;
				d.lines.add(line(CellFormatter.aggregateLabel(r.def, a), "", CellFormatter.aggregate(r.def, a, g.aggregates.get(i))));
			}
			return d;
		}
		for (StatLine s : r.def.sheet){
			if (s.isSection()){
				d.lines.add(new Line(true, s.label, "", "", "", CellText.EXACT, false));
				continue;
			}
			AggValue v = QueryEngine.evaluate(r.def, r.query, s.aggregate, g.rows);
			if (v == null) continue;   //needs hidden information that is off
			d.lines.add(line(s.label, s.explanation, CellFormatter.aggregate(r.def, s.aggregate, v)));
		}
		//a heading with nothing under it is not shown
		for (int i = d.lines.size() - 1; i >= 0; i--){
			if (d.lines.get(i).section && (i == d.lines.size() - 1 || d.lines.get(i + 1).section)) d.lines.remove(i);
		}
		return d;
	}

	private static Line line( String label, String explanation, CellFormatter.AggText at ){
		return new Line(false, label, at.text, at.detail, explanation, at.state, at.small);
	}

	//the sheet as text for a window (headings between underscores, as the other detail windows do)
	public String text(){
		StringBuilder b = new StringBuilder();
		for (Line k : keys){
			b.append(k.label).append(": ").append(k.value);
			if (!k.detail.isEmpty()) b.append("  (").append(k.detail).append(')');
			b.append('\n');
		}
		b.append(rows).append(rows == 1 ? " row in this group\n" : " rows in this group\n");
		for (Line l : lines){
			if (l.section){
				b.append("\n_").append(l.label).append("_\n");
				continue;
			}
			b.append(l.label).append(": ").append(l.value);
			String more = l.detail;
			if (l.state == CellText.LOWER) more += "; at least this much, some values were lower bounds";
			if (l.state == CellText.INFERRED) more += "; includes inferred values";
			b.append("  (").append(more).append(")\n");
		}
		return b.toString();
	}
}
