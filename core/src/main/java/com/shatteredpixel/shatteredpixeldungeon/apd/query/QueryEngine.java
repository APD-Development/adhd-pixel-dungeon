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

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

//Runs a Query over a Dataset. Pure and read-only. The rules it enforces:
//
//HIDDEN INFORMATION. Rows and columns are PUBLIC or INTERNAL. An INTERNAL cell is looked at only when the player asked for
//hidden information AND the row's run is permanently finished. For an unfinished run it is masked whatever the query says:
//it cannot be shown, filtered, sorted, grouped or counted (so it cannot be inferred either), and rows that exist only as
//hidden information are dropped without a trace (they are not even counted). Rows of finished runs that stay out because
//hidden information is off are counted, which is safe.
//
//UNKNOWN IS NOT ZERO. A filter never matches an unknown value (except "is unknown"), sorting puts unknown last, aggregates
//use only known values and say how many that was. A value that is only a lower bound is never treated as exact.
//
//SCOPE. Only the chosen ruleset, only finished runs, no duplicate imported records, unless the query says otherwise. Every
//group left out is counted.
public final class QueryEngine {

	private QueryEngine(){}

	private static final int TRUE = 0, FALSE = 1, UNKNOWN = 2, UNCERTAIN = 3, HIDDEN = 4;

	//Is this cell of this row open to be looked at under this query?
	public static boolean open( Query q, Row row, ColumnDef col ){
		return !col.isInternal() || (q.showInternal && row.finished);
	}

	public static QueryResult run( Dataset ds, Query q, String currentRuleset ){
		DatasetDef def = ds.def;
		List<String> problems = new ArrayList<>();
		QueryResult.Stats stats = new QueryResult.Stats();

		//---- what in the query can be applied ----
		List<ColumnDef> columns = new ArrayList<>();
		List<String> wanted = q.columns.isEmpty() ? def.defaultColumnIds() : q.columns;
		for (String id : wanted){
			ColumnDef c = def.column(id);
			if (c == null){
				problems.add("Column " + id + " does not exist and was left out.");
			} else if (c.isInternal() && !q.showInternal){
				//hidden information is off: the column is simply not offered
			} else if (!columns.contains(c)){
				columns.add(c);
			}
		}
		List<Filter> filters = new ArrayList<>();
		for (Filter f : q.filters){
			String why = invalid(def, q, f);
			if (why == null) filters.add(f); else problems.add("Filter " + f + " was not applied: " + why);
		}
		List<ColumnDef> groupKeys = null;
		List<Aggregate> aggregates = null;
		if (q.group != null){
			groupKeys = new ArrayList<>();
			for (String k : q.group.keys){
				ColumnDef c = def.column(k);
				if (c == null || (c.isInternal() && !q.showInternal)){
					problems.add("Group by " + k + " was left out.");
				} else {
					groupKeys.add(c);
				}
			}
			aggregates = new ArrayList<>();
			for (Aggregate a : q.group.aggregates){
				if (aggregateOk(def, q, a)) aggregates.add(a); else problems.add("Aggregate " + a.id() + " was left out.");
			}
			if (groupKeys.isEmpty() && aggregates.isEmpty()){
				groupKeys = null;
				aggregates = null;
			}
		}
		boolean grouped = groupKeys != null;

		//---- scope and hidden information ----
		List<Row> candidates = new ArrayList<>();
		for (Row r : ds.rows){
			boolean legit = r.publicRow || (q.showInternal && r.finished);
			if (!legit){
				if (r.finished) stats.hiddenRows++;
				continue;   //hidden information of an unfinished run: not even counted
			}
			stats.total++;
			int why = scopeReason(q.scope, r.duplicate, r.finished, r.ruleset, currentRuleset);
			if (why != 0){
				count(stats, why);
				continue;
			}
			candidates.add(r);
		}
		stats.inScope = candidates.size();
		java.util.Set<String> rulesetKinds = new java.util.HashSet<>();
		for (Row r : candidates) rulesetKinds.add(r.ruleset == null ? "" : r.ruleset);
		stats.rulesetsInScope = rulesetKinds.size();
		for (Dataset.RunInfo ri : ds.runs){
			if (scopeReason(q.scope, ri.duplicate, ri.finished, ri.ruleset, currentRuleset) != 0) continue;
			stats.runsInScope++;
			if (ri.covered) stats.runsCovered++;
		}

		//---- filters, one after the other, each reporting what it cost ----
		List<Row> current = candidates;
		for (Filter f : filters){
			ColumnDef c = def.column(f.column);
			QueryResult.FilterReport rep = new QueryResult.FilterReport(f);
			List<Row> next = new ArrayList<>(current.size());
			for (Row r : current){
				rep.in++;
				int v = eval(r, c, f, open(q, r, c));
				switch (v){
					case TRUE: rep.matched++; next.add(r); break;
					case FALSE: rep.notMatched++; break;
					case UNKNOWN: rep.unknown++; break;
					case UNCERTAIN: rep.uncertain++; break;
					default: rep.hidden++;
				}
			}
			stats.filters.add(rep);
			current = next;
		}
		stats.matching = current.size();

		//---- coverage of the visible columns over what is left ----
		List<QueryResult.ColumnCoverage> coverage = new ArrayList<>();
		for (ColumnDef c : columns){
			int total = 0, known = 0, lower = 0, inferred = 0, hidden = 0;
			for (Row r : current){
				if (!open(q, r, c)){
					hidden++;
					continue;
				}
				total++;
				if (r.isKnown(c.index)){
					known++;
					int p = r.prov(c.index);
					if (p == Fact.PROV_INCOMPLETE) lower++;
					else if (p == Fact.PROV_INFERRED) inferred++;
				}
			}
			coverage.add(new QueryResult.ColumnCoverage(c, total, known, lower, inferred, hidden));
		}

		if (!grouped){
			sortRows(current, q, def, filterSorts(q, def, null));
			return new QueryResult(q, def, columns, current, null, null, null, coverage, stats, problems);
		}

		//---- grouping ----
		Map<String, List<Row>> buckets = new LinkedHashMap<>();
		for (Row r : current){
			StringBuilder key = new StringBuilder();
			for (ColumnDef c : groupKeys){
				if (!open(q, r, c)) key.append("H");
				else if (!r.isKnown(c.index)) key.append("U");
				else if (r.kind(c.index) == Row.K_NUM) key.append("N").append(r.num(c.index)).append(r.prov(c.index) == Fact.PROV_INCOMPLETE ? "+" : "");
				else {
					String text = r.text(c.index);
					key.append("T").append(text.length()).append(":").append(text);
				}
				key.append('\u0001');
			}
			List<Row> b = buckets.get(key.toString());
			if (b == null){
				b = new ArrayList<>();
				buckets.put(key.toString(), b);
			}
			b.add(r);
		}
		List<GroupRow> groups = new ArrayList<>();
		for (List<Row> b : buckets.values()){
			if (q.minSample > 1 && b.size() < q.minSample){
				stats.hiddenSmallGroups++;
				stats.hiddenSmallGroupRows += b.size();
				continue;
			}
			List<AggValue> vals = new ArrayList<>();
			for (Aggregate a : aggregates) vals.add(aggregate(def, q, a, b));
			groups.add(new GroupRow(b.get(0), b.size(), vals, b));
		}
		sortGroups(groups, q, def, groupKeys, aggregates);
		return new QueryResult(q, def, columns, Collections.<Row>emptyList(), groups, groupKeys, aggregates, coverage, stats, problems);
	}

	//---- scope ----

	private static final int R_OTHER = 1, R_UNKNOWN = 2, R_MIXED = 3, R_UNFINISHED = 4, R_DUPLICATE = 5;

	//0 when in scope
	private static int scopeReason( Scope s, boolean duplicate, boolean finished, String ruleset, String current ){
		if (duplicate && !s.includeDuplicates) return R_DUPLICATE;
		if (!finished && !s.includeUnfinished) return R_UNFINISHED;
		if (ruleset == null) return s.includeUnknownRuleset ? 0 : R_UNKNOWN;
		if (ruleset.equals(Row.RULESET_MIXED)) return s.includeMixedRuleset ? 0 : R_MIXED;
		switch (s.rules){
			case ALL:
				return 0;
			case SPECIFIC:
				return ruleset.equals(s.ruleset) ? 0 : R_OTHER;
			default:
				return current != null && ruleset.equals(current) ? 0 : R_OTHER;
		}
	}

	private static void count( QueryResult.Stats st, int why ){
		switch (why){
			case R_OTHER: st.otherRulesets++; break;
			case R_UNKNOWN: st.unknownRuleset++; break;
			case R_MIXED: st.mixedRuleset++; break;
			case R_UNFINISHED: st.unfinished++; break;
			default: st.duplicates++;
		}
	}

	//---- filters ----

	private static String invalid( DatasetDef def, Query q, Filter f ){
		ColumnDef c = def.column(f.column);
		if (c == null) return "no such column.";
		if (c.isInternal() && !q.showInternal) return "it uses hidden information, which is off.";
		boolean ok = c.type == ColumnDef.Type.TEXT ? f.op == Op.GROUP_TEXT || f.op == Op.IDENTIFIER_IN
                : f.op == Op.GROUP_VALUE || f.op == Op.GROUP_BOUND;
		for (Op o : Op.ops(c.type)) if (o == f.op) ok = true;
		if (!ok) return "\"" + f.op.word + "\" does not apply to " + c.label + ".";
		if (f.op.takesValue()){
			int have = c.type == ColumnDef.Type.TEXT ? f.texts.length : f.nums.length;
			int need = f.op == Op.BETWEEN ? 2 : 1;
			if (have < need) return "it needs a value.";
		}
		return null;
	}

	private static int eval( Row r, ColumnDef c, Filter f, boolean open ){
		if (!open) return HIDDEN;
		int i = c.index;
		boolean known = r.isKnown(i);
		if (f.op == Op.KNOWN) return known ? TRUE : FALSE;
		if (f.op == Op.UNKNOWN) return known ? FALSE : TRUE;
		if (!known) return UNKNOWN;
		if (c.type == ColumnDef.Type.TEXT){
			String v = r.text(i);
			switch (f.op){
				case GROUP_TEXT: return v.equals(f.texts[0]) ? TRUE : FALSE;
                case IDENTIFIER_IN:
                    for (String t : f.texts) if (v.equals(t)) return TRUE;
                    return FALSE;
                case EQ: return v.equalsIgnoreCase(f.texts[0]) ? TRUE : FALSE;
				case NE: return v.equalsIgnoreCase(f.texts[0]) ? FALSE : TRUE;
				case CONTAINS: return v.toLowerCase(Locale.ROOT).contains(f.texts[0].toLowerCase(Locale.ROOT)) ? TRUE : FALSE;
				case IN:
					for (String t : f.texts) if (v.equalsIgnoreCase(t)) return TRUE;
					return FALSE;
				default: return FALSE;
			}
		}
		long v = r.num(i);
		boolean lb = c.type == ColumnDef.Type.NUM && r.prov(i) == Fact.PROV_INCOMPLETE;
		//a lower bound means the real value is somewhere in [v, infinity): only answers that hold for all of it are given
		switch (f.op){
            case GROUP_VALUE:
                return r.prov(i) != Fact.PROV_INCOMPLETE && v == f.nums[0] ? TRUE : FALSE;
            case GROUP_BOUND:
                return r.prov(i) == Fact.PROV_INCOMPLETE && v == f.nums[0] ? TRUE : FALSE;
			case EQ: {
				long a = f.nums[0];
				if (!lb) return v == a ? TRUE : FALSE;
				return a < v ? FALSE : UNCERTAIN;
			}
			case NE: {
				long a = f.nums[0];
				if (!lb) return v != a ? TRUE : FALSE;
				return a < v ? TRUE : UNCERTAIN;
			}
			case LT: {
				long a = f.nums[0];
				if (!lb) return v < a ? TRUE : FALSE;
				return v >= a ? FALSE : UNCERTAIN;
			}
			case LE: {
				long a = f.nums[0];
				if (!lb) return v <= a ? TRUE : FALSE;
				return v > a ? FALSE : UNCERTAIN;
			}
			case GT: {
				long a = f.nums[0];
				if (!lb) return v > a ? TRUE : FALSE;
				return v > a ? TRUE : UNCERTAIN;
			}
			case GE: {
				long a = f.nums[0];
				if (!lb) return v >= a ? TRUE : FALSE;
				return v >= a ? TRUE : UNCERTAIN;
			}
			case BETWEEN: {
				long lo = Math.min(f.nums[0], f.nums[1]), hi = Math.max(f.nums[0], f.nums[1]);
				if (!lb) return v >= lo && v <= hi ? TRUE : FALSE;
				return v > hi ? FALSE : UNCERTAIN;
			}
			case IN:
				for (long n : f.nums) if (v == n) return TRUE;
				return FALSE;
			default:
				return FALSE;
		}
	}

	//---- sorting ----

	private static List<Sort> filterSorts( Query q, DatasetDef def, List<String> aggIds ){
		List<Sort> out = new ArrayList<>();
		for (Sort s : q.sorts){
			ColumnDef c = def.column(s.column);
			if (c != null && c.isInternal() && !q.showInternal) continue;
			if (c == null && (aggIds == null || !aggIds.contains(s.column))) continue;
			out.add(s);
		}
		return out;
	}

	//compares two cells: unknown and hidden last, in either direction
	private static int compareCells( Query q, ColumnDef c, Row a, Row b, boolean desc ){
		boolean ua = !open(q, a, c) || !a.isKnown(c.index), ub = !open(q, b, c) || !b.isKnown(c.index);
		if (ua || ub) return ua && ub ? 0 : ua ? 1 : -1;
		int cmp;
		if (c.type == ColumnDef.Type.TEXT) cmp = a.text(c.index).compareToIgnoreCase(b.text(c.index));
		else cmp = a.num(c.index) < b.num(c.index) ? -1 : a.num(c.index) > b.num(c.index) ? 1 : 0;
		return desc ? -cmp : cmp;
	}

	private static void sortRows( List<Row> rows, final Query q, final DatasetDef def, final List<Sort> sorts ){
		if (sorts.isEmpty()) return;
		final Map<Row, Integer> order = new java.util.IdentityHashMap<>();
		for (int i = 0; i < rows.size(); i++) order.put(rows.get(i), i);
		Collections.sort(rows, new Comparator<Row>(){
			@Override
			public int compare( Row a, Row b ){
				for (Sort s : sorts){
					int c = compareCells(q, def.column(s.column), a, b, s.descending);
					if (c != 0) return c;
				}
				return order.get(a) - order.get(b);   //stable
			}
		});
	}

	private static void sortGroups( List<GroupRow> groups, final Query q, final DatasetDef def, final List<ColumnDef> keys,
	                                final List<Aggregate> aggs ){
		final List<String> aggIds = new ArrayList<>();
		for (Aggregate a : aggs) aggIds.add(a.id());
		final List<Sort> sorts = new ArrayList<>();
		for (Sort s : filterSorts(q, def, aggIds)){
			ColumnDef c = def.column(s.column);
			if (c == null || keys.contains(c)) sorts.add(s);
		}
		if (sorts.isEmpty()) return;
		final Map<GroupRow, Integer> order = new java.util.IdentityHashMap<>();
		for (int i = 0; i < groups.size(); i++) order.put(groups.get(i), i);
		Collections.sort(groups, new Comparator<GroupRow>(){
			@Override
			public int compare( GroupRow a, GroupRow b ){
				for (Sort s : sorts){
					int c;
					ColumnDef col = def.column(s.column);
					if (col != null){
						c = compareCells(q, col, a.sample, b.sample, s.descending);
					} else {
						int idx = aggIds.indexOf(s.column);
						AggValue x = a.aggregates.get(idx), y = b.aggregates.get(idx);
						if (!x.known || !y.known) c = x.known == y.known ? 0 : x.known ? -1 : 1;
						else {
							c = Double.compare(x.value, y.value);
							if (s.descending) c = -c;
						}
					}
					if (c != 0) return c;
				}
				return order.get(a) - order.get(b);
			}
		});
	}

	//---- aggregates ----

	//One aggregate over some rows (the rows of a group), under the query's hidden-information setting: what a statistics sheet
	//shows. Null when the aggregate cannot be computed under this query (it uses a hidden column while hidden information is off).
	public static AggValue evaluate( DatasetDef def, Query q, Aggregate a, List<Row> rows ){
		return aggregateOk(def, q, a) ? aggregate(def, q, a, rows) : null;
	}

	private static boolean usable( DatasetDef def, Query q, String column ){
		ColumnDef c = def.column(column);
		return c != null && !(c.isInternal() && !q.showInternal);
	}

	private static boolean aggregateOk( DatasetDef def, Query q, Aggregate a ){
		if (!a.whereColumn.isEmpty() && (!usable(def, q, a.whereColumn) || def.column(a.whereColumn).type == ColumnDef.Type.TEXT)) return false;
		if (!a.distinctBy.isEmpty() && !usable(def, q, a.distinctBy)) return false;
		if (a.fn == Aggregate.Fn.COUNT) return true;
		if (!usable(def, q, a.column)) return false;
		ColumnDef c = def.column(a.column);
		if (a.fn == Aggregate.Fn.COUNT_KNOWN || a.fn == Aggregate.Fn.COUNT_DISTINCT) return true;
		if (c.type == ColumnDef.Type.TEXT) return false;
		if (a.fn == Aggregate.Fn.RATIO){
			ColumnDef o = def.column(a.over);
			return o != null && usable(def, q, a.over) && o.type != ColumnDef.Type.TEXT;
		}
		return true;
	}

	//the rows an aggregate looks at: those that pass its condition, then one per distinct value of its "per" column
	private static List<Row> selected( DatasetDef def, Query q, Aggregate a, List<Row> rows, int[] unsure ){
		List<Row> sel = rows;
		if (!a.whereColumn.isEmpty()){
			ColumnDef wc = def.column(a.whereColumn);
			sel = new ArrayList<>();
			for (Row r : rows){
				if (!open(q, r, wc) || !r.isKnown(wc.index) || r.prov(wc.index) == Fact.PROV_INCOMPLETE){
					unsure[0]++;   //cannot be judged, so it is not counted either way
					continue;
				}
				if (r.num(wc.index) == a.whereValue) sel.add(r);
			}
		}
		if (!a.distinctBy.isEmpty()){
			ColumnDef dc = def.column(a.distinctBy);
			Map<String, Row> first = new LinkedHashMap<>();
			int loose = 0;
			for (Row r : sel){
				String key;
				if (open(q, r, dc) && r.isKnown(dc.index) && r.prov(dc.index) != Fact.PROV_INCOMPLETE){
					key = "K" + r.asText(dc.index);
				} else {
					key = "R" + (loose++);   //no exact value to share: it stands for itself
					unsure[0]++;
				}
				if (!first.containsKey(key)) first.put(key, r);
			}
			sel = new ArrayList<>(first.values());
		}
		return sel;
	}

	private static AggValue aggregate( DatasetDef def, Query q, Aggregate a, List<Row> all ){
		int[] unsure = new int[1];
		List<Row> rows = selected(def, q, a, all, unsure);
		int total = rows.size();
		int baseFlags = unsure[0] > 0 ? AggValue.UNSURE : 0;
		if (a.fn == Aggregate.Fn.COUNT) return new AggValue(true, total, 0, total, total, baseFlags);
		ColumnDef c = def.column(a.column);
		ColumnDef over = a.over.isEmpty() ? null : def.column(a.over);
		int contributing = 0, flags = baseFlags;
		double sum = 0, sumOver = 0, min = 0, max = 0;
		long hits = 0;
		java.util.Set<String> distinct = a.fn == Aggregate.Fn.COUNT_DISTINCT ? new java.util.HashSet<String>() : null;
		List<Double> values = a.fn == Aggregate.Fn.MEDIAN ? new ArrayList<Double>() : null;
		for (Row r : rows){
			if (!open(q, r, c) || !r.isKnown(c.index)) continue;
			int prov = r.prov(c.index);
			boolean lb = prov == Fact.PROV_INCOMPLETE;
			if ((a.fn == Aggregate.Fn.RATE || a.fn == Aggregate.Fn.RATIO) && lb) continue;   //a lower bound cannot be judged against a target
			if (a.fn == Aggregate.Fn.RATIO){
				if (!open(q, r, over) || !r.isKnown(over.index) || r.prov(over.index) == Fact.PROV_INCOMPLETE) continue;
				sumOver += r.num(over.index);
			}
			if (lb) flags |= AggValue.LOWER;
			if (prov == Fact.PROV_INFERRED) flags |= AggValue.INFERRED;
			if (prov == Fact.PROV_LEGACY) flags |= AggValue.LEGACY;
			contributing++;
			if (a.fn == Aggregate.Fn.COUNT_KNOWN) continue;
			if (a.fn == Aggregate.Fn.COUNT_DISTINCT){
				distinct.add(r.asText(c.index));
				continue;
			}
			double v = r.num(c.index);
			if (contributing == 1){
				min = max = v;
			} else {
				if (v < min) min = v;
				if (v > max) max = v;
			}
			sum += v;
			if (a.fn == Aggregate.Fn.MEDIAN) values.add(v);
			if (a.fn == Aggregate.Fn.RATE && r.num(c.index) == a.target) hits++;
		}
		//"how many are known" is a count: none known is a known zero. Every other aggregate over nothing is unknown, never zero.
		if (contributing == 0) return a.fn == Aggregate.Fn.COUNT_KNOWN ? new AggValue(true, 0, 0, 0, total, flags) : new AggValue(false, 0, 0, 0, total, flags);
		switch (a.fn){
			case COUNT_KNOWN: return new AggValue(true, contributing, 0, contributing, total, flags);
			case COUNT_DISTINCT: return new AggValue(true, distinct.size(), 0, contributing, total, flags);
			case SUM: return new AggValue(true, sum, 0, contributing, total, flags);
			case AVG: return new AggValue(true, sum / contributing, 0, contributing, total, flags);
			case MIN: return new AggValue(true, min, 0, contributing, total, flags);
			case MAX: return new AggValue(true, max, 0, contributing, total, flags);
			case MEDIAN: {
				Collections.sort(values);
				int n = values.size();
				double m = n % 2 == 1 ? values.get(n / 2) : (values.get(n / 2 - 1) + values.get(n / 2)) / 2.0;
				return new AggValue(true, m, 0, contributing, total, flags);
			}
			case RATIO:
				if (sumOver == 0) return new AggValue(false, 0, 0, 0, total, flags);   //nothing to divide by: not zero, not known
				return new AggValue(true, sum / sumOver, Math.round(sum), contributing, total, flags, Math.round(sumOver));
			default: return new AggValue(true, (double) hits / contributing, hits, contributing, total, flags);
		}
	}
}

