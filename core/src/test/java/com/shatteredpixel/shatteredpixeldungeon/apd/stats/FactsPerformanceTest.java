package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

//How long deriving facts takes for a history of a realistic size. The numbers are printed for the phase report.
public class FactsPerformanceTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void derivingFactsFromAFullHistoryIsFastAndTheCacheIsFaster() throws Exception {
		int runs = 60, fightsPerRun = 1500;
		long bytes = 0;
		for (int i = 0; i < runs; i++){
			File f = new File(new File(tmp.getRoot(), "runs"), "run" + i + ".apdlog");
			RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "run" + i, "test", 1,
					new RunLogWriter.Options().queueCapacity(1 << 20));
			LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
			s.runId = "run" + i; s.heroClass = "WARRIOR";
			LifecycleEvents.emitRunStart(w, 0, false, s);
			for (int k = 0; k < fightsPerRun; k++){
				CombatEvents.Attack a = new CombatEvents.Attack();
				a.attacker.kind = CombatEvents.KIND_HERO; a.attacker.name = "you"; a.defender.kind = CombatEvents.KIND_ENEMY;
				a.defender.name = "rat"; a.hit = true; a.weaponName = "sword";
				CombatEvents.emitAttack(w, k, a);
				CombatEvents.Damage d = new CombatEvents.Damage();
				d.target.kind = CombatEvents.KIND_ENEMY; d.target.name = "rat"; d.shown = 4;
				CombatEvents.emitDamage(w, k, d);
				CombatEvents.XpGain x = new CombatEvents.XpGain();
				x.amount = 1;
				CombatEvents.emitXpGain(w, k, x);
			}
			LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
			e.outcome = LifecycleEvents.OUTCOME_DIED; e.score = 100 + i;
			LifecycleEvents.emitRunEnd(w, fightsPerRun, e);
			w.close(true);
			bytes += f.length();
		}
		DerivedStore store = new DerivedStore(tmp.getRoot());
		long t0 = System.nanoTime();
		FactTable built = store.rebuild();
		long t1 = System.nanoTime();
		FactTable cached = store.load();
		long t2 = System.nanoTime();
		assertEquals(DerivedStore.Source.CACHE, store.lastSource());
		assertEquals(runs, built.apdRows.size());
		assertEquals(built.apdRows, cached.apdRows);
		assertEquals(fightsPerRun, built.apdRows.get(0).numOr(FactKeys.HERO_ATTACKS, -1));
		long rebuildMs = (t1 - t0) / 1000000L, cachedMs = (t2 - t1) / 1000000L;
		System.out.println("APD-PERF facts: rebuild from " + runs + " runs (" + (bytes / 1024) + " KiB of logs, " + (runs * (fightsPerRun * 3 + 2))
				+ " events) took " + rebuildMs + " ms; reading the cache took " + cachedMs + " ms; cache file " + (store.cacheFile().length() / 1024) + " KiB");
		assertTrue("rebuilding must stay interactive on a desktop JVM, took " + rebuildMs + " ms", rebuildMs < 5000);
		assertTrue("the cache must be faster than rebuilding", cachedMs <= rebuildMs);
	}
}
