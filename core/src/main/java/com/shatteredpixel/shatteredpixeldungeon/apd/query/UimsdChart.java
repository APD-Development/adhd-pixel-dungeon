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

//A small-chart data model made only from an already-authorized grouped QueryResult. It never re-reads rows, expands
//the query scope or exposes raw group keys: labels and knowledge states come through the same CellFormatter as the table.
//Drawing is deliberately separate so phones can choose a responsive presentation without changing statistical meaning.
public final class UimsdChart {

	public static final int MAX_POINTS = 24;
	public static final String COUNT = "count";

	public static final class Measure {
		public final String id;
		public final String label;

		private Measure( String id, String label ){
			this.id = id;
			this.label = label;
		}
	}

	public static final class Point {
		//The existing group behind this point, for the same drill-down a table row uses.
		public final GroupRow group;
		public final String label;
		public final int labelState;
		public final boolean known;
		//The aggregate engine's value. It is for relative geometry; shown is the authoritative formatted value.
		public final double value;
		public final String shown;
		public final String detail;
		public final int state;
		public final boolean small;

		private Point( GroupRow group, String label, int labelState, boolean known, double value,
		               String shown, String detail, int state, boolean small ){
			this.group = group;
			this.label = label;
			this.labelState = labelState;
			this.known = known;
			this.value = value;
			this.shown = shown;
			this.detail = detail;
			this.state = state;
			this.small = small;
		}
	}

	public static final class Series {
		public final Measure measure;
		public final List<Point> points;
		public final int sourceGroups;
		public final int omitted;
		//Known values only. Both are zero when every point is unknown.
		public final double min;
		public final double max;

		private Series( Measure measure, List<Point> points, int sourceGroups, double min, double max ){
			this.measure = measure;
			this.points = Collections.unmodifiableList(new ArrayList<>(points));
			this.sourceGroups = sourceGroups;
			this.omitted = Math.max(0, sourceGroups - points.size());
			this.min = min;
			this.max = max;
		}

		public boolean isTruncated(){
			return omitted > 0;
		}
	}

	private UimsdChart(){}

	//A chart needs named groups. Ungrouped tables keep their normal rows and do not invent categories.
	public static List<Measure> measures( QueryResult result ){
		if (result == null || !result.isGrouped()) return Collections.emptyList();
		List<Measure> out = new ArrayList<>();
		out.add(new Measure(COUNT, result.def.countLabel));
		for (Aggregate aggregate : result.aggregates){
			if (!aggregate.isPlainCount()) out.add(new Measure(aggregate.id(),
					CellFormatter.aggregateLabel(result.def, aggregate)));
		}
		return Collections.unmodifiableList(out);
	}

	public static Series build( QueryResult result, String measureId, int requestedPoints ){
		if (result == null || !result.isGrouped()) return null;
		Measure measure = null;
		int aggregateIndex = -1;
		for (Measure candidate : measures(result)){
			if (candidate.id.equals(measureId)){
				measure = candidate;
				break;
			}
		}
		if (measure == null) return null;
		if (!COUNT.equals(measure.id)){
			for (int i = 0; i < result.aggregates.size(); i++){
				Aggregate aggregate = result.aggregates.get(i);
				if (!aggregate.isPlainCount() && aggregate.id().equals(measure.id)){
					aggregateIndex = i;
					break;
				}
			}
			if (aggregateIndex < 0) return null;
		}

		int limit = requestedPoints <= 0 ? MAX_POINTS : Math.min(MAX_POINTS, requestedPoints);
		int count = Math.min(limit, result.groups.size());
		List<Point> points = new ArrayList<>(count);
		double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < count; i++){
			GroupRow group = result.groups.get(i);
			Label label = label(result, group);
			Point point;
			if (COUNT.equals(measure.id)){
				point = new Point(group, label.text, label.state, true, group.n,
						Integer.toString(group.n), group.n + (group.n == 1 ? " entry" : " entries"),
						CellText.EXACT, false);
			} else {
				Aggregate aggregate = result.aggregates.get(aggregateIndex);
				AggValue value = group.aggregates.get(aggregateIndex);
				CellFormatter.AggText shown = CellFormatter.aggregate(result.def, aggregate, value);
				point = new Point(group, label.text, label.state, value.known, value.known ? value.value : 0,
						shown.compact, shown.detail, shown.state, shown.small);
			}
			points.add(point);
			if (point.known){
				min = Math.min(min, point.value);
				max = Math.max(max, point.value);
			}
		}
		if (min == Double.POSITIVE_INFINITY){
			min = 0;
			max = 0;
		}
		return new Series(measure, points, result.groups.size(), min, max);
	}

	private static final class Label {
		final String text;
		final int state;

		Label( String text, int state ){
			this.text = text;
			this.state = state;
		}
	}

	private static Label label( QueryResult result, GroupRow group ){
		if (result.groupKeys.isEmpty()) return new Label("All matching " + result.def.unitPlural(), CellText.EXACT);
		StringBuilder text = new StringBuilder();
		int state = CellText.EXACT;
		for (int i = 0; i < result.groupKeys.size(); i++){
			ColumnDef key = result.groupKeys.get(i);
			CellText cell = CellFormatter.cell(result.query, group.sample, key);
			if (i > 0) text.append(" / ");
			if (result.groupKeys.size() > 1) text.append(key.label).append(": ");
			text.append(cell.text);
			state = lessCertain(state, cell.state);
		}
		return new Label(text.toString(), state);
	}

	private static int lessCertain( int current, int candidate ){
		return confidence(candidate) > confidence(current) ? candidate : current;
	}

	private static int confidence( int state ){
		switch (state){
			case CellText.HIDDEN: return 5;
			case CellText.UNKNOWN: return 4;
			case CellText.LOWER: return 3;
			case CellText.INFERRED: return 2;
			case CellText.LEGACY: return 1;
			default: return 0;
		}
	}
}
