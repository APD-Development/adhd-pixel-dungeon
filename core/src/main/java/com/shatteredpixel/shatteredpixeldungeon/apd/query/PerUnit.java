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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//The denominators of "per run / per floor / per turn / per hour" answers. "Average Guiding Light casts per floor" is the number of casts
//divided by the number of floors, and the floors must be counted from the FLOORS table (every visit, quiet ones included), not from the
//rows that happened to contain a cast: the denominator comes from what the player did, not from the thing being counted.
//
//Given an answer (a grouped query on any dataset that carries the run context), this computes for each group how many runs, floor visits, turns or
//hours the group stands for: the Floors that match the query's context filters and the group's own context keys (class, outcome, difficulty,
//challenges, floor, branch). Keys that are not context (a spell, an enemy, a value) do not narrow the denominator: every spell is measured
//against the same floors.
public final class PerUnit {

	private PerUnit(){}

	public enum Unit {
		RUN("run", "runs", "per run"), FLOOR("floor visit", "floor visits", "per floor visit"), TURN("turn", "turns", "per turn"),
		THOUSAND_TURNS("1,000 turns", "1,000 turns", "per 1,000 turns"), HOUR("hour played", "hours played", "per hour played");

		public final String singular, plural, phrase;

		Unit( String singular, String plural, String phrase ){
			this.singular = singular;
			this.plural = plural;
			this.phrase = phrase;
		}
	}

	//the parts of a row's context that the Floors table also has (the suffix of the column id after the dataset's prefix)
	static final String[] CONTEXT = { "runClass", "runOutcome", "runDifficulty", "runDeepest", "runChallenges", "depth", "branch" };

	public static final class Result {
		public final Unit unit;
		//per group of the answer, in the answer's order; NaN = cannot be said (no floors, or time not recorded)
		public final double[] denominators;
		//the denominator is only a lower bound (a floor that is not over yet)
		public final boolean[] lower;
		public final String note;

		Result( Unit unit, double[] d, boolean[] lower, String note ){
			this.unit = unit;
			this.denominators = d;
			this.lower = lower;
			this.note = note;
		}

		//numerator per unit, NaN when either side is unknown or the denominator is zero
		public double per( double numerator, int group ){
			double d = denominators[group];
			if (Double.isNaN(d) || d <= 0 || Double.isNaN(numerator)) return Double.NaN;
			return numerator / d;
		}
	}

	//The prefix of the dataset's own columns ("fact", "attack", "floor", ...), or null when the dataset does not carry the run context.
	static String prefixOf( String dataset ){
		if (FactDatasets.isFactDataset(dataset)) return "fact";
		if (EventDatasets.isEventDataset(dataset)) return EventDatasets.prefixOf(dataset);
		return null;
	}

	public static Result compute( Catalog cat, Query q, QueryResult res, Unit unit, String currentRuleset ){
		int groups = res.groups == null ? 1 : res.groups.size();
		double[] den = new double[groups];
		boolean[] lower = new boolean[groups];
		Arrays.fill(den, Double.NaN);
		String p = prefixOf(q.dataset);
		Dataset floors = cat == null ? null : cat.dataset(EventDatasets.FLOORS);
		if (p == null || floors == null || floors.rows.isEmpty()) return new Result(unit, den, lower, "No floor history to measure against.");
		if (unit == Unit.HOUR && floors.def.indexOf("floor.activeMillis") < 0) return new Result(unit, den, lower, "Time played is not recorded.");

		//the context keys of the answer, as columns of the Floors table
		List<String> keys = new ArrayList<>();
		List<Integer> keyColumnsInAnswer = new ArrayList<>();
		if (res.groupKeys != null){
			for (ColumnDef c : res.groupKeys){
				String fc = floorColumn(p, c.id);
				if (fc != null && !keys.contains(fc)){
					keys.add(fc);
					keyColumnsInAnswer.add(c.index);
				}
			}
		}
		List<Filter> fl = new ArrayList<>();
		for (Filter f : q.filters){
			String fc = floorColumn(p, f.column);
			if (fc != null) fl.add(new Filter(fc, f.op, f.nums, f.texts));
		}
		List<Aggregate> aggs = new ArrayList<>();
		aggs.add(Aggregate.count());
		aggs.add(Aggregate.of(Aggregate.Fn.SUM, "floor.turns"));
		aggs.add(Aggregate.distinct("floor.run"));
		boolean hour = floors.def.indexOf("floor.activeMillis") >= 0;
		if (hour) aggs.add(Aggregate.of(Aggregate.Fn.SUM, "floor.activeMillis"));
		Query fq = Query.of(EventDatasets.FLOORS).withScope(q.scope).withFilters(fl).withShowInternal(q.showInternal).withMinSample(1)
				.withGroup(new GroupBy(keys, aggs));
		QueryResult fr = QueryEngine.run(floors, fq, currentRuleset);
		if (fr.groups == null) return new Result(unit, den, lower, "No floor history to measure against.");

		Map<String, GroupRow> byKey = new HashMap<>();
		for (GroupRow g : fr.groups) byKey.put(keyOf(g.sample, fr.groupKeys), g);
		for (int i = 0; i < groups; i++){
			GroupRow wanted = null;
			if (res.groups == null || keys.isEmpty()){
				wanted = byKey.get(keyOf(null, null));
			} else {
				StringBuilder k = new StringBuilder();
				Row sample = res.groups.get(i).sample;
				for (int idx : keyColumnsInAnswer) k.append(cell(sample, idx));
				wanted = byKey.get(k.toString());
			}
			if (wanted == null) continue;
			AggValue count = wanted.aggregates.get(0), turns = wanted.aggregates.get(1), runs = wanted.aggregates.get(2);
			AggValue chosen;
			double scale = 1;
			switch (unit){
				case FLOOR: chosen = count; break;
				case TURN: chosen = turns; break;
				case THOUSAND_TURNS: chosen = turns; scale = 1000; break;
				case HOUR: chosen = hour ? wanted.aggregates.get(3) : null; scale = 3_600_000d; break;
				default: chosen = runs;
			}
			if (chosen == null || !chosen.known) continue;
			den[i] = chosen.value / scale;
			lower[i] = (chosen.flags & AggValue.LOWER) != 0;
		}
		return new Result(unit, den, lower, "");
	}

	//the Floors column that stands for a context column of dataset prefix p, null when it is not context
	static String floorColumn( String p, String column ){
		if (!column.startsWith(p + ".")) return null;
		String suffix = column.substring(p.length() + 1);
		for (String c : CONTEXT) if (c.equals(suffix)) return "floor." + suffix;
		return null;
	}

	private static String cell( Row r, int idx ){
		if (r == null || !r.isKnown(idx)) return "U\u0001";
		return (r.kind(idx) == Row.K_NUM ? "N" + r.num(idx)
                + (r.prov(idx) == com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact.PROV_INCOMPLETE ? "+" : "")
                : "T" + r.text(idx)) + "\u0001";
	}

	private static String keyOf( Row sample, List<ColumnDef> cols ){
		if (cols == null || sample == null) return "";
		StringBuilder b = new StringBuilder();
		for (ColumnDef c : cols) b.append(cell(sample, c.index));
		return b.toString();
	}
}

