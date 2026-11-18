package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//How long UIMSD takes on a large history. The numbers are printed for the phase report.
public class UimsdPerformanceTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void queryingFiftyThousandItemRowsIsFast(){
		DatasetDef def = Datasets.items();
		List<Row> rows = new ArrayList<>();
		java.util.Random rnd = new java.util.Random(7);   //test data only
		int name = def.indexOf("item.name"), cat = def.indexOf("item.category"), fate = def.indexOf("item.fate"),
				turns = def.indexOf("item.turnsEquipped"), depth = def.indexOf("item.firstSeenDepth"), cls = def.indexOf("item.trueClass");
		for (int i = 0; i < 50000; i++){
			Row r = new Row("run-" + (i / 500) + "/" + i, null, "run-" + (i / 500), def.size());
			r.finished = true;
			r.ruleset = "r";
			r.setText(name, "item " + rnd.nextInt(300), 1);
			r.setNum(cat, rnd.nextInt(16), 1);
			r.setNum(fate, rnd.nextInt(4), 1);
			if (rnd.nextInt(5) > 0) r.setNum(turns, rnd.nextInt(5000), rnd.nextInt(10) == 0 ? 4 : 1);
			r.setNum(depth, 1 + rnd.nextInt(26), 1);
			r.setText(cls, "items.Class" + rnd.nextInt(80), 1);
			rows.add(r);
		}
		Dataset ds = new Dataset(def, rows, new ArrayList<Dataset.RunInfo>());
		Query base = Query.of("items").withShowInternal(true).withScope(Scope.DEFAULT);
		long t0 = System.nanoTime();
		QueryResult plain = QueryEngine.run(ds, base, "r");
		long t1 = System.nanoTime();
		QueryResult filtered = QueryEngine.run(ds, base.addFilter(Filter.num("item.category", Op.IN, 1, 2, 3))
				.addFilter(Filter.num("item.turnsEquipped", Op.GE, 100)).addFilter(Filter.text("item.name", Op.CONTAINS, "item 1"))
				.withSorts(Arrays.asList(new Sort("item.turnsEquipped", true), new Sort("item.name", false))), "r");
		long t2 = System.nanoTime();
		QueryResult grouped = QueryEngine.run(ds, base.withGroup(new GroupBy(Arrays.asList("item.category", "item.fate"),
				Arrays.asList(Aggregate.count(), Aggregate.of(Aggregate.Fn.AVG, "item.turnsEquipped"), Aggregate.rate("item.fate", 3)))), "r");
		long t3 = System.nanoTime();
		ResultTable.build(filtered, 3, 50);
		long t4 = System.nanoTime();
		System.out.println("APD-PERF query: 50000 rows: scan " + (t1 - t0) / 1000000 + " ms, 3 filters + 2-key sort " + (t2 - t1) / 1000000
				+ " ms, group by 2 columns " + (t3 - t2) / 1000000 + " ms, one page of the table " + (t4 - t3) / 1000 + " us");
		assertEquals(50000, plain.rows.size());
		assertTrue(filtered.rows.size() > 0 && filtered.rows.size() < 50000);
		assertTrue(grouped.groups.size() > 10);
		assertTrue("a plain scan of 50,000 rows must be quick", (t1 - t0) / 1000000 < 2000);
		assertTrue("filters and sorting must be quick", (t2 - t1) / 1000000 < 3000);
		assertTrue("grouping must be quick", (t3 - t2) / 1000000 < 3000);
	}

	@Test
	public void buildingAndReloadingAHistoryOfManyRuns() throws Exception {
		File root = tmp.getRoot();
		int runs = 40;
		for (int i = 0; i < runs; i++){
			SyntheticLog l = new SyntheticLog(root, "run" + i, TelemetryVersion.CURRENT, 1000 + i).start("run-" + i, 0, i % 2 == 0 ? "WARRIOR" : "MAGE",
					"r1-aaaaaaaaaaaa", 0, 0, false);
			for (int d = 1; d <= 10; d++){
				l.enter(d);
				for (int k = 0; k < 25; k++){
					String uid = "i" + d + "-" + k;
					l.registered(uid, "items.Thing", 0, false, ItemEvents.ORIGIN_GENERATED).generated(uid, 0, "R");
					if (k % 3 == 0) l.seen(uid, "thing", ItemEvents.CAT_POTION).acquired(uid, "thing", ItemEvents.CAT_POTION, false, 0);
				}
				l.heroAttacks("rat", "actors.mobs.Rat", true).damageFromHero("rat", "actors.mobs.Rat", 3).heroKills("rat", "actors.mobs.Rat", 1);
			}
			l.end(LifecycleEvents.OUTCOME_DIED, "Rat", 10, 8, 1000 + i).close(true);
		}
		DatasetStore store = new DatasetStore(root);
		long t0 = System.nanoTime();
		Catalog first = store.load();
		long t1 = System.nanoTime();
		Catalog second = store.load();
		long t2 = System.nanoTime();
		assertEquals(runs, first.logsRebuilt);
		assertEquals(runs, second.logsCached);
		assertEquals(runs * 250, first.dataset("items").rows.size());
		long cacheBytes = 0;
		for (File f : store.rowsDir().listFiles()) cacheBytes += f.length();
		System.out.println("APD-PERF datasets: " + runs + " runs (" + first.dataset("items").rows.size() + " item rows): first build " + (t1 - t0) / 1000000
				+ " ms, reload from the row cache " + (t2 - t1) / 1000000 + " ms, cache " + cacheBytes / 1024 + " KiB");
		assertTrue(second.dataset("items").rows.size() == first.dataset("items").rows.size());
	}
}
