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

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.SelfTest;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import java.io.File;
import java.util.Arrays;

//The device self-test for UIMSD: writes a small made-up history, builds the datasets from it twice (from the logs, then from the row
//cache), queries them (including a hidden-information check) and times it on this device. It uses a work folder of its own and
//removes it afterwards, and never touches the real history.
public final class UimsdSelfTest {

	private UimsdSelfTest(){}

	private static void delete( File f ){
		File[] kids = f.listFiles();
		if (kids != null) for (File k : kids) delete(k);
		f.delete();
	}

	public static void run( File workDir, SelfTest.Report r ){
		File root = new File(workDir, "uimsd-selftest-" + System.nanoTime());
		try {
			int runs = 30, itemsPerRun = 40;
			for (int i = 0; i < runs; i++){
				boolean open = i == runs - 1;
				File f = new File(new File(root, "runs"), "run" + i + ".apdlog");
				RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "run" + i, "selftest", 1000 + i);
				LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
				s.runId = "run-" + i; s.heroClass = i % 2 == 0 ? "WARRIOR" : "MAGE"; s.rulesetId = "r1-selftest";
				LifecycleEvents.emitRunStart(w, 0, false, s);
				LifecycleEvents.LevelEnter l = new LifecycleEvents.LevelEnter();
				l.depth = 1;
				LifecycleEvents.emitLevelEnter(w, 1, l);
				for (int k = 0; k < itemsPerRun; k++){
					ItemEvents.Registered g = new ItemEvents.Registered();
					g.uid = "u" + k; g.trueClass = "items.Secret" + (k % 7); g.trueLevel = k % 5;
					ItemEvents.emitRegistered(w, 1, 0, g);
					if (k % 2 == 0){
						ItemEvents.Seen seen = new ItemEvents.Seen();
						seen.uid = "u" + k; seen.state.name = "thing " + k; seen.state.category = ItemEvents.CAT_POTION;
						ItemEvents.emitSeen(w, 2 + k, 0, seen);
					}
				}
				if (!open){
					LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
					e.outcome = i % 3 == 0 ? LifecycleEvents.OUTCOME_WON : LifecycleEvents.OUTCOME_DIED;
					e.deepest = 1 + i % 9; e.heroLevel = 3 + i % 7; e.score = 100 * i; e.turns = 500 + i;
					LifecycleEvents.emitRunEnd(w, 500, e);
				}
				w.close(!open);
			}
			DatasetStore store = new DatasetStore(root);
			long t0 = System.nanoTime();
			Catalog first = store.load();
			long t1 = System.nanoTime();
			Catalog second = store.load();
			long t2 = System.nanoTime();
			Dataset items = second.dataset(Datasets.ITEMS);
			Query q = Query.of(Datasets.ITEMS).withShowInternal(true).withScope(Scope.DEFAULT.withUnfinished(true).withRules(Scope.Rules.ALL, ""));
			QueryResult res = QueryEngine.run(items, q, "r1-selftest");
			//the finished runs' hidden information is readable, the open run's is masked, and its hidden-only rows are not even there
			int hiddenCells = 0, openRows = 0;
			ColumnDef trueClass = items.def.column("item.trueClass");
			for (Row row : res.rows){
				if (!row.finished){
					openRows++;
					if (CellFormatter.cell(q, row, trueClass).state == CellText.HIDDEN) hiddenCells++;
				}
			}
			Query g = Query.of(Datasets.RUNS).withScope(Scope.DEFAULT).withGroup(new GroupBy(Arrays.asList("run.heroClass"),
					Arrays.asList(Aggregate.count(), Aggregate.rate("result.deepest", 1))));
			QueryResult grouped = QueryEngine.run(second.dataset(Datasets.RUNS), g, "r1-selftest");
			boolean ok = first.logsRebuilt == runs && second.logsCached == runs && second.dataset(Datasets.RUNS).rows.size() == runs
					&& items.rows.size() == runs * itemsPerRun && openRows == itemsPerRun / 2 && hiddenCells == openRows
					&& grouped.isGrouped() && grouped.groups.size() == 2;
			if (ok) r.pass("UIMSD datasets and queries (hidden information of an open run stays hidden)");
			else r.fail("UIMSD datasets or queries gave the wrong answer");
			r.info("UIMSD: build " + runs + " runs (" + items.rows.size() + " item rows) " + (t1 - t0) / 1000000 + " ms, reload from cache "
					+ (t2 - t1) / 1000000 + " ms");
		} catch (Throwable t){
			r.fail("UIMSD self-test threw " + t);
		} finally {
			delete(root);
		}
	}
}
