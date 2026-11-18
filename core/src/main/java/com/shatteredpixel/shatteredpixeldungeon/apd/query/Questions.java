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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//Turns a Question into a query and an answer, and offers the choices the question builder shows. Everything here is generated from the
//datasets and their columns, so a new dataset, column or kind of fact is askable the moment it exists, with no change to the builder.
public final class Questions {

	private Questions(){}

	//---- what can be chosen ----

	//datasets that have anything to ask about, the long-standing ones first
	public static List<Dataset> askable( Catalog cat ){
		List<Dataset> out = new ArrayList<>();
		if (cat == null) return out;
		for (Dataset d : cat.datasets()) if (!d.rows.isEmpty()) out.add(d);
		return out;
	}

	//numbers that can be totalled, averaged and so on (not the numbers that stand for words)
	public static List<ColumnDef> measurable( DatasetDef def, boolean showInternal ){
		List<ColumnDef> out = new ArrayList<>();
		for (ColumnDef c : def.columns()){
			if (c.type != ColumnDef.Type.NUM) continue;
			if (c.isInternal() && !showInternal) continue;
			out.add(c);
		}
		return out;
	}

	//what the answer can be split by: the columns that stand for words come first, then numbers such as the floor
	public static List<ColumnDef> groupable( DatasetDef def, boolean showInternal ){
		List<ColumnDef> words = new ArrayList<>(), numbers = new ArrayList<>();
		for (ColumnDef c : def.columns()){
			if (c.isInternal() && !showInternal) continue;
			if (c.id.endsWith(".run") || c.id.equals("run.id") || c.id.equals("run.log")) continue;
			if (c.type == ColumnDef.Type.NUM) numbers.add(c); else words.add(c);
		}
		List<ColumnDef> out = new ArrayList<>(words);
		out.addAll(numbers);
		return out;
	}

	//what "per" can mean for this question: only counts and totals of things that happened during play can be divided by what the player did
	public static List<PerUnit.Unit> unitsFor( Question q ){
		if (PerUnit.prefixOf(q.dataset) == null) return Collections.emptyList();
		if (q.measure != Question.Measure.COUNT && q.measure != Question.Measure.TOTAL) return Collections.emptyList();
		return Arrays.asList(PerUnit.Unit.RUN, PerUnit.Unit.FLOOR, PerUnit.Unit.TURN, PerUnit.Unit.THOUSAND_TURNS, PerUnit.Unit.HOUR);
	}

	//---- the query behind a question ----

	//Turning hidden details off is a visibility boundary, not just a display preference. Any
	//question parts which depend on internal columns must leave with it, or the sentence can
	//promise a condition which QueryEngine correctly refuses to apply.
	public static Question withInternalVisibility( Question q, DatasetDef def, boolean showInternal ){
		if (showInternal) return q.withShowInternal(true);
		Question.Measure measure = q.measure;
		String column = q.column;
		ColumnDef measured = def == null ? null : def.column(column);
		if (measure.needsColumn() && measured != null && measured.isInternal()){
			measure = Question.Measure.COUNT;
			column = "";
		}
		List<String> groups = new ArrayList<>();
		for (String id : q.groupBy){
			ColumnDef c = def == null ? null : def.column(id);
			if (c == null || !c.isInternal()) groups.add(id);
		}
		List<Filter> filters = new ArrayList<>();
		for (Filter f : q.filters){
			ColumnDef c = def == null ? null : def.column(f.column);
			if (c == null || !c.isInternal()) filters.add(f);
		}
		return new Question(q.dataset, measure, column, q.per, groups, filters, q.scope, false);
	}

	public static Query toQuery( Question q, DatasetDef def ){
		q = withInternalVisibility(q, def, q.showInternal);
		List<Aggregate> aggs = new ArrayList<>();
		if (q.measure == Question.Measure.COUNT || def.column(q.column) == null) aggs.add(Aggregate.count());
		else aggs.add(Aggregate.of(q.measure.fn, q.column));
		List<String> keys = new ArrayList<>();
		for (String g : q.groupBy) if (def.column(g) != null) keys.add(g);
		return Query.of(q.dataset).withScope(q.scope).withFilters(q.filters).withShowInternal(q.showInternal).withMinSample(1)
				.withGroup(new GroupBy(keys, aggs));
	}

	//---- the answer ----

	public static final class Line {
		public final String label;
		public final String value;
		public final String detail;
		//the number, NaN when it cannot be said
		public final double number;
		public final boolean lowerBound;

		Line( String label, String value, String detail, double number, boolean lowerBound ){
			this.label = label;
			this.value = value;
			this.detail = detail;
			this.number = number;
			this.lowerBound = lowerBound;
		}
	}

	public static final class Answer {
		public final String question;
		public final List<Line> lines;
		public final String note;
		//the same question as a table with the full query tools
		public final Query query;

		Answer( String question, List<Line> lines, String note, Query query ){
			this.question = question;
			this.lines = lines;
			this.note = note;
			this.query = query;
		}

		public boolean isEmpty(){
			return lines.isEmpty();
		}
	}

	public static Answer answer( Catalog cat, Question q, String currentRuleset ){
		Dataset ds = cat == null ? null : cat.dataset(q.dataset);
		if (ds == null) return new Answer("", new ArrayList<Line>(), "There is nothing recorded to ask about yet.", Query.of(q.dataset));
		DatasetDef def = ds.def;
		Question effective = withInternalVisibility(q, def, q.showInternal);
		Query query = toQuery(effective, def);
		String sentence = describe(effective, def);
		QueryResult res = QueryEngine.run(ds, query, currentRuleset);
		List<Line> lines = new ArrayList<>();
		if (res.groups == null || res.groups.isEmpty()){
			String note = res.stats != null && res.stats.total > 0 && res.stats.inScope == 0
					? "Nothing here counts under the current rules. Widen it to include other rules, unfinished runs or older recordings."
					: "Nothing was recorded that matches.";
			return new Answer(sentence, lines, note, query);
		}
		Aggregate agg = res.aggregates.get(0);
		PerUnit.Result per = null;
		if (effective.per != null && !unitsFor(effective).isEmpty()) per = PerUnit.compute(cat, query, res, effective.per, currentRuleset);
		for (int i = 0; i < res.groups.size(); i++){
			GroupRow g = res.groups.get(i);
			StringBuilder label = new StringBuilder();
			for (ColumnDef k : res.groupKeys){
				if (label.length() > 0) label.append(" / ");
				CellText t = CellFormatter.cell(query, g.sample, k);
				label.append(t.text.isEmpty() ? CellFormatter.UNKNOWN_TEXT : t.text);
			}
			if (label.length() == 0) label.append("All");
			AggValue v = g.aggregates.get(0);
			CellFormatter.AggText at = CellFormatter.aggregate(def, agg, v);
			if (per == null){
				lines.add(new Line(label.toString(), at.text, at.detail, v.known ? CellFormatter.displayNumber(def.column(agg.column), v.value) : Double.NaN, v.isLowerBound()));
			} else {
				double n = v.known ? CellFormatter.displayNumber(def.column(agg.column), v.value) : Double.NaN;
				double r = per.per(n, i);
				boolean numeratorLower = v.isLowerBound(), denominatorLower = per.lower[i];
				boolean ambiguous = numeratorLower && denominatorLower && n > 0;
				boolean lower = false;
				String text;
				if (Double.isNaN(r) || ambiguous){
					text = CellFormatter.UNKNOWN_TEXT;
					r = Double.NaN;
				} else if (denominatorLower){
					lower = numeratorLower || n < 0;
					if (numeratorLower) text = ">=" + boundDecimals(r, true);
					else if (n < 0) text = boundDecimals(r, true) + " or more";
					else if (n > 0) text = boundDecimals(r, false) + " or less";
					else text = twoDecimals(r);
				} else {
					lower = numeratorLower;
					text = lower ? ">=" + boundDecimals(r, true) : twoDecimals(r);
				}
				String detail = at.text + " in " + (denominatorLower ? "at least " : "")
						+ twoDecimals(per.denominators[i]).replaceAll("\\.00$", "") + " " + effective.per.plural;
				if (ambiguous) detail += ". Both totals are lower bounds; the rate is unknown.";
				lines.add(new Line(label.toString(), text, Double.isNaN(per.denominators[i]) ? per.note : detail, r, lower));
			}
		}
		Collections.sort(lines, new Comparator<Line>(){
			@Override
			public int compare( Line a, Line b ){
				boolean an = Double.isNaN(a.number), bn = Double.isNaN(b.number);
				if (an != bn) return an ? 1 : -1;
				if (!an && a.number != b.number) return a.number > b.number ? -1 : 1;
				return a.label.compareTo(b.label);
			}
		});
		String note = "";
		if (res.stats != null && res.stats.inScope > 0 && res.stats.matching < res.stats.inScope) note = res.stats.matching + " of " + res.stats.inScope + " entries match your conditions.";
		return new Answer(sentence, lines, note, query);
	}

	// A displayed bound must remain conservative after rounding to two decimal places.
	private static String boundDecimals(double value, boolean lower){
		return java.math.BigDecimal.valueOf(value).setScale(2,
				lower ? java.math.RoundingMode.FLOOR : java.math.RoundingMode.CEILING).toPlainString();
	}

	private static String twoDecimals( double v ){
		if (!Double.isFinite(v)) return "?";
		// Preserve ordinary rounding; avoid Math.round saturation for valid large ratios.
		if (Math.abs(v) * 100 >= Long.MAX_VALUE)
			return java.math.BigDecimal.valueOf(v).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
		long hundredths = Math.round(v * 100);
		long whole = hundredths / 100, frac = Math.abs(hundredths % 100);
		return (hundredths < 0 && whole == 0 ? "-" : "") + whole + "." + (frac < 10 ? "0" : "") + frac;
	}

	//---- the question in words ----

	public static String describe( Question q, DatasetDef def ){
		StringBuilder b = new StringBuilder();
		String things = def.countLabel.toLowerCase();
		ColumnDef col = def.column(q.column);
		switch (q.measure){
			case COUNT: b.append("How many ").append(things); break;
			default: b.append(cap(q.measure.phrase)).append(' ').append(col == null ? "number" : col.label.toLowerCase()).append(" of ").append(things);
		}
		if (q.per != null && !unitsFor(q).isEmpty()) b.append(' ').append(q.per.phrase);
		if (!q.groupBy.isEmpty()){
			b.append(", by ");
			for (int i = 0; i < q.groupBy.size(); i++){
				ColumnDef g = def.column(q.groupBy.get(i));
				b.append(i > 0 ? " and " : "").append(g == null ? q.groupBy.get(i) : g.label.toLowerCase());
			}
		}
		for (int i = 0; i < q.filters.size(); i++){
			Filter f = q.filters.get(i);
			ColumnDef c = def.column(f.column);
			b.append(i == 0 ? ", only where " : " and ").append(c == null ? f.column : c.label.toLowerCase()).append(' ').append(f.op.word);
			for (long n : f.nums) b.append(' ').append(c == null ? Long.toString(n) : CellFormatter.value(c, n));
			for (String t : f.texts) b.append(' ').append(t);
		}
		return b.toString();
	}

	private static String cap( String s ){
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
