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

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKind;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKinds;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//The datasets generated from the fact-kind registry (see FactKinds): one dataset per kind, one row per observation. Nothing here knows
//any particular kind: the columns come from the kind's declaration, the rows from the generic FACT events of the run's own log. So a
//new character or mechanic that declares a kind gets a dataset with filters, sorts, groups and aggregates with no change to UIMSD.
//
//Every observation is a row (never a summary of rows): a count, total, average, minimum or maximum is asked of the rows by the query
//engine. The context of each row (floor, branch, turn, and the run's class, outcome, difficulty and challenges) is resolved here from the
//run's timeline instead of being stored on every observation.
public final class FactDatasets {

	private FactDatasets(){}

	public static final String PREFIX = "fact:";

	public static String idOf( FactKind k ){
		return PREFIX + k.key;
	}

	public static boolean isFactDataset( String id ){
		return id != null && id.startsWith(PREFIX);
	}

	private static final Map<String, DatasetDef> CACHE = new HashMap<>();
	private static String cacheFingerprint = "";

	//null when no kind of that key is registered
	public static synchronized DatasetDef def( String datasetId ){
		if (!isFactDataset(datasetId)) return null;
		FactKind k = FactKinds.get(datasetId.substring(PREFIX.length()));
		return k == null ? null : def(k);
	}

	public static synchronized DatasetDef def( FactKind k ){
		String fp = FactKinds.fingerprint();
		if (!fp.equals(cacheFingerprint)){
			CACHE.clear();
			cacheFingerprint = fp;
		}
		DatasetDef d = CACHE.get(k.key);
		if (d == null){
			d = build(k);
			CACHE.put(k.key, d);
		}
		return d;
	}

	public static List<DatasetDef> defs(){
		List<DatasetDef> out = new ArrayList<>();
		for (FactKind k : FactKinds.all()) out.add(def(k));
		return out;
	}

	//column ids of the generic part of every fact dataset
	public static final String RUN = "fact.run", TURN = "fact.turn", DEPTH = "fact.depth", BRANCH = "fact.branch",
			SUBJECT = "fact.subject", ACTOR = "fact.actor", RUN_DIFFICULTY = "fact.runDifficulty";

	public static String valueColumn( FactKind.Field f ){
		return "fact.v." + f.key;
	}

	public static String tagColumn( FactKind.Field f ){
		return "fact.t." + f.key;
	}

	private static DatasetDef build( FactKind k ){
		DatasetDef d = new DatasetDef(idOf(k), k.plural, k.description.isEmpty() ? "Every " + k.label.toLowerCase() + " recorded." : k.description);
		d.minTelemetry = k.since;
		d.countLabel = k.plural;
		String area = k.area;
		d.add(ColumnDef.text(RUN, "Run").group("When"));
		d.add(ColumnDef.num(TURN, "Turn").shown().group("When").describe("The game turn it happened on."));
		d.add(ColumnDef.num(DEPTH, "Floor").shown().group("When").describe("The floor the hero was on."));
		d.add(ColumnDef.num(BRANCH, "Branch").group("When"));
		if (!k.subjectLabel.isEmpty()) d.add(ColumnDef.text(SUBJECT, k.subjectLabel).shown().group(area));
		if (k.hasWho) d.add(ColumnDef.text(ACTOR, "Who").shown().group(area).describe("Empty means the hero."));
		for (FactKind.Field f : k.values()){
			ColumnDef c = ColumnDef.num(valueColumn(f), f.label).shown().group(area);
			if (f.scale == FactKind.HUNDREDTHS) c.format(ColumnDef.Format.HUNDREDTHS);
			else if (f.scale == FactKind.THOUSANDTHS) c.format(ColumnDef.Format.THOUSANDTHS);
			if (!f.description.isEmpty()) c.describe(f.description);
			d.add(c);
		}
		for (FactKind.Field f : k.tags()){
			ColumnDef c = ColumnDef.text(tagColumn(f), f.label).shown().group(area);
			if (!f.description.isEmpty()) c.describe(f.description);
			d.add(c);
		}
		Datasets.addRunFacts(d, "fact");
		d.add(ColumnDef.num(RUN_DIFFICULTY, "Run difficulty").group("Run").describe("The difficulty (1-5) the run this row belongs to was played on."));
		d.link(new LinkDef("The run", Datasets.RUNS, RUN, FactKeys.RUN_ID));
		return d;
	}

	//---- rows ----

	//Adds a dataset's rows for every kind that has observations in this log. 'events' is the canonical timeline.
	static void addRows( LogRows out, List<ApdEvent> events, ReadResult r, LogMeta meta ){
		int depth = -1, branch = -1;
		Map<String, Integer> counts = new HashMap<>();
		for (ApdEvent e : events){
			if (e.type == EventTypes.LEVEL_ENTER){
				try {
					LifecycleEvents.LevelEnter l = LifecycleEvents.decodeLevelEnter(e, r);
					depth = l.depth;
					branch = l.branch;
				} catch (RuntimeException bad){
					//the floor stays as it was
				}
				continue;
			}
			if (!FactEvents.isFactEvent(e.type)) continue;
			List<FactEvents.Observation> obs;
			try {
				obs = FactEvents.decode(e, r);
			} catch (RuntimeException bad){
				continue;   //an event this reader cannot decode contributes nothing
			}
			for (FactEvents.Observation o : obs){
				FactKind k = FactKinds.get(o.key);
				if (k == null) continue;   //a kind this build does not know
				DatasetDef d = def(k);
				List<Row> rows = out.of(idOf(k));
				int n = counts.containsKey(k.key) ? counts.get(k.key) : 0;
				counts.put(k.key, n + 1);
				Row row = new Row(meta.runId + "/f/" + k.key + "/" + n + "@" + meta.logId, null, meta.runId, d.size());
				row.finished = meta.hasRunEnd;
				row.ruleset = meta.ruleset;
				row.telemetry = meta.telemetry;
				row.publicRow = !k.isInternal();
				row.setText(d.indexOf(RUN), meta.runId, Fact.PROV_RECORDED);
				row.setNum(d.indexOf(TURN), o.clock, Fact.PROV_RECORDED);
				if (depth >= 0){
					row.setNum(d.indexOf(DEPTH), depth, Fact.PROV_RECORDED);
					row.setNum(d.indexOf(BRANCH), branch, Fact.PROV_RECORDED);
				}
				int si = d.indexOf(SUBJECT);
				if (si >= 0) row.setText(si, o.subject, Fact.PROV_RECORDED);
				int ai = d.indexOf(ACTOR);
				if (ai >= 0) row.setText(ai, o.actor, Fact.PROV_RECORDED);
				List<FactKind.Field> vs = k.values();
				for (int i = 0; i < vs.size() && i < o.values.length; i++) row.setNum(d.indexOf(valueColumn(vs.get(i))), o.values[i], Fact.PROV_RECORDED);
				List<FactKind.Field> ts = k.tags();
				for (int i = 0; i < ts.size() && i < o.tags.length; i++) row.setText(d.indexOf(tagColumn(ts.get(i))), o.tags[i], Fact.PROV_RECORDED);
				rows.add(row);
			}
		}
	}
}
