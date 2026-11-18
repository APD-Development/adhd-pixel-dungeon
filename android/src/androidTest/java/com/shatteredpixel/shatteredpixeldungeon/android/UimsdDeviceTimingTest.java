package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Dataset;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Query;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryEngine;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Scope;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ViewDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

//Phase 9 measures before it optimises: how long UIMSD takes on this device for a realistic history (the 72-run demo history, which now
//includes exact healing). The numbers go to logcat (tag APDPERF, streamed into the CI job log); the budgets are generous for an emulator
//but a real regression would break them.
@RunWith(AndroidJUnit4.class)
public class UimsdDeviceTimingTest {

	private static final String TAG = "APDPERF";

	private static void delete( File f ){
		File[] kids = f.listFiles();
		if (kids != null) for (File k : kids) delete(k);
		f.delete();
	}

	@Test
	public void uimsdIsFastEnoughOnThisDevice() throws Exception {
		Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
		File base = new File(ctx.getFilesDir(), "apd-timing-" + System.nanoTime());
		File demo = DemoData.demoRootFor(new File(base, "apd"));
		try {
			Ruleset rules = Ruleset.of(30, 0);
			long g0 = System.nanoTime();
			DemoGenerator.generate(demo, rules, System.currentTimeMillis());
			long g1 = System.nanoTime();

			DatasetStore store = new DatasetStore(demo);
			long t0 = System.nanoTime();
			Catalog cold = store.load();
			long t1 = System.nanoTime();
			Catalog warm = store.load();
			long t2 = System.nanoTime();

			long slowest = 0;
			String slowestView = "";
			long all = 0;
			int rows = 0, views = 0;
			for (ViewDef v : warm.views()){
				Dataset ds = warm.datasetOf(v);
				Query q = v.initialQuery().withMinSample(0).withScope(Scope.DEFAULT.withUnfinished(true).withRules(Scope.Rules.ALL, "")).withShowInternal(true);
				long s0 = System.nanoTime();
				QueryResult r = QueryEngine.run(ds, q, rules.id);
				long ms = (System.nanoTime() - s0) / 1000000;
				all += ms;
				views++;
				rows += r.rows.size();
				if (ms >= slowest){
					slowest = ms;
					slowestView = v.id;
				}
			}
			long genMs = (g1 - g0) / 1000000, coldMs = (t1 - t0) / 1000000, warmMs = (t2 - t1) / 1000000;
			Log.w(TAG, "demo history generated in " + genMs + " ms; first build of every dataset " + coldMs + " ms; reload from the row cache " + warmMs
					+ " ms; " + views + " views run in " + all + " ms in total, slowest " + slowestView + " " + slowest + " ms (" + rows + " rows shown)");
			assertTrue(cold.logsRebuilt > 0);
			assertTrue("the second load must come from the cache", warm.logsCached > 0);
			assertTrue("first build too slow: " + coldMs + " ms", coldMs < 30000);
			assertTrue("reload from the cache too slow: " + warmMs + " ms", warmMs < 5000);
			assertTrue("one view too slow: " + slowestView + " " + slowest + " ms", slowest < 3000);

			//the Home page, built from the same catalog
			long h0 = System.nanoTime();
			com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdHome home = com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdHome.build(warm, rules.id, true);
			long homeMs = (System.nanoTime() - h0) / 1000000;
			Log.w(TAG, "Home built in " + homeMs + " ms (" + home.cards.size() + " cards, " + home.runs + " runs)");
			assertTrue("Home has nothing on it", !home.isEmpty() && home.cards.size() >= 5);
			assertTrue("Home too slow: " + homeMs + " ms", homeMs < 3000);

			//The title-menu path prepares this same fully validated model in the
			//background. Measure the work separately from the tap-time handoff:
			//takeReady must only validate file metadata and must never wait for a load.
			java.lang.reflect.Method resetWarm = com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdWarmCache.class
					.getDeclaredMethod("resetForTest");
			java.lang.reflect.Method awaitWarm = com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdWarmCache.class
					.getDeclaredMethod("awaitPrewarmForTest", long.class);
			resetWarm.setAccessible(true);
			awaitWarm.setAccessible(true);
			resetWarm.invoke(null);
			try {
				long prewarmStart = System.nanoTime();
				com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdWarmCache.prewarm(
						demo, rules.id, "Device timing", true);
				awaitWarm.invoke(null, 60000L);
				long prewarmMs = (System.nanoTime() - prewarmStart) / 1000000;
				long handoffStart = System.nanoTime();
				com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel prepared =
						com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdWarmCache.takeReady(
								demo, rules.id, "Device timing", true);
				long handoffMs = (System.nanoTime() - handoffStart) / 1000000;
				Log.w(TAG, "title prewarm prepared the fully READY model in " + prewarmMs
						+ " ms; tap-time validated handoff took " + handoffMs + " ms");
				assertNotNull("title prewarm did not produce a model", prepared);
				assertEquals(com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel.State.READY,
						prepared.state());
				assertTrue("title prewarm too slow: " + prewarmMs + " ms", prewarmMs < 30000);
				assertTrue("tap-time handoff too slow: " + handoffMs + " ms", handoffMs < 1000);
				assertNull("a mutable warm model may only be consumed once",
						com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdWarmCache.takeReady(
								demo, rules.id, "Device timing", true));
			} finally {
				resetWarm.invoke(null);
			}

			//difficulty: the demo history has recorded and unknown difficulties, and the difficulty view runs
			com.shatteredpixel.shatteredpixeldungeon.apd.query.Dataset runsDs = warm.dataset("runs");
			int diffCol = runsDs.def.indexOf(com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.DIFFICULTY);
			java.util.Set<Long> levels = new java.util.HashSet<>();
			int unknownDiff = 0;
			for (com.shatteredpixel.shatteredpixeldungeon.apd.query.Row rr : runsDs.rows){
				if (rr.isKnown(diffCol)) levels.add(rr.num(diffCol)); else unknownDiff++;
			}
			Log.w(TAG, "difficulty levels in the demo history: " + levels + ", unknown on " + unknownDiff + " runs");
			assertTrue("demo history should record several difficulties: " + levels, levels.size() >= 3);
			assertTrue("older demo runs must show an unknown difficulty", unknownDiff > 0);
			assertTrue(warm.view("difficulty") != null);
			com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdHome homeWithLevels = com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdHome.build(warm, rules.id, true);
			assertTrue("Home should offer the difficulty card", homeWithLevels.card("difficulty") != null);
			boolean namesDifficulty = false;
			for (Catalog.RulesetInfo ri : warm.rulesets) if (ri.label.contains("difficulty")) namesDifficulty = true;
			assertTrue("the rules list should name the difficulty", namesDifficulty);
			assertTrue("difficulty 3 keeps the id the rules had before", "r1-37d60350c6b1".equals(Ruleset.of(30, 0).id)
					&& Ruleset.of(30, 0).id.equals(Ruleset.of(30, 0, 3).id) && !Ruleset.of(30, 0, 4).id.equals(Ruleset.of(30, 0).id));

			//reports from the same catalog, in all four formats
			long rp0 = System.nanoTime();
			com.shatteredpixel.shatteredpixeldungeon.apd.report.Report life = com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportBuilder
					.lifetime(warm, rules.id, "", true, System.currentTimeMillis(), "device");
			ViewDef runsView = warm.view("runs");
			QueryResult runsRes = QueryEngine.run(warm.datasetOf(runsView), runsView.initialQuery().withScope(Scope.DEFAULT.withUnfinished(true)
					.withRules(Scope.Rules.ALL, "")), rules.id);
			com.shatteredpixel.shatteredpixeldungeon.apd.report.Report viewReport = com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportBuilder
					.view(runsRes, "Runs", "", System.currentTimeMillis(), "device");
			com.shatteredpixel.shatteredpixeldungeon.apd.query.Row someRun = warm.dataset("runs").rows.get(0);
			com.shatteredpixel.shatteredpixeldungeon.apd.report.Report runReport = com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportBuilder
					.run(warm, someRun, false, rules.id, System.currentTimeMillis(), "device");
			int chars = 0;
			for (com.shatteredpixel.shatteredpixeldungeon.apd.report.Report rep : new com.shatteredpixel.shatteredpixeldungeon.apd.report.Report[]{ life, viewReport, runReport }){
				for (com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportExport.Format f : com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportExport.Format.values()){
					String out = com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportExport.render(rep, f);
					assertTrue(rep.title + " as " + f + " is empty", out.length() > 100);
					chars += out.length();
				}
			}
			long reportMs = (System.nanoTime() - rp0) / 1000000;
			Log.w(TAG, "3 reports in 4 formats built in " + reportMs + " ms (" + chars + " chars)");
			assertTrue("reports too slow: " + reportMs + " ms", reportMs < 5000);

			//the tour's sample history: made, readable, marked, and removable without touching the demo or real history
			File sample = DemoData.sampleRootFor(new File(base, "apd"));
			long m0 = System.nanoTime();
			DemoGenerator.generateSample(sample, rules, System.currentTimeMillis());
			Catalog sc = new DatasetStore(sample).load();
			long sampleMs = (System.nanoTime() - m0) / 1000000;
			Log.w(TAG, "tour sample made and read in " + sampleMs + " ms");
			assertTrue("sample history empty", com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdHome.build(sc, rules.id, true).runs > 0);
			assertTrue("sample too slow: " + sampleMs + " ms", sampleMs < 20000);
			DemoData.delete(sample);
			assertTrue("sample folder not removed", !sample.exists());
			assertTrue("demo must survive removing the sample", demo.exists());
		} finally {
			delete(base);
		}
	}
}
