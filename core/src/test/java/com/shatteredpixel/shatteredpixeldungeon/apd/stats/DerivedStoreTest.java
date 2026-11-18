package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class DerivedStoreTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File root(){
		return tmp.getRoot();
	}

	private void apdRun( String id, String heroClass, int outcome, int deepest, int level, long score, String rankingId ) throws Exception {
		File f = new File(new File(root(), "runs"), id + ".apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, id, "test", 1);
		LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
		s.runId = id; s.heroClass = heroClass;
		LifecycleEvents.emitRunStart(w, 0, false, s);
		LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
		e.outcome = outcome; e.deepest = deepest; e.heroLevel = level; e.score = score; e.depth = deepest; e.turns = 100;
		LifecycleEvents.emitRunEnd(w, 5, e);
		if (rankingId != null){
			LegacyEvents.RankingLink l = new LegacyEvents.RankingLink();
			l.gameId = rankingId;
			LegacyEvents.emitRankingLink(w, 6, l);
		}
		w.close(true);
	}

	private LegacyEvents.Record legacy( String gameId, String heroClass, int outcome, int deepest, int level, long score ){
		LegacyEvents.Record r = new LegacyEvents.Record();
		r.gameId = gameId;
		r.source = "rankings.dat";
		r.putText(FactKeys.HERO_CLASS, heroClass);
		r.putNum(FactKeys.OUTCOME, outcome);
		r.putNum(FactKeys.DEEPEST, deepest);
		r.putNum(FactKeys.HERO_LEVEL, level);
		r.putNum(FactKeys.SCORE, score);
		r.putUnknown(FactKeys.DATE);
		return r;
	}

	private void importLog( String name, LegacyEvents.Record... records ) throws Exception {
		File f = new File(new File(root(), "legacy"), name + ".apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, name, "test", 1);
		long c = 0;
		for (LegacyEvents.Record r : records) LegacyEvents.emitRecord(w, c++, r);
		w.close(true);
	}

	@Test
	public void importedRecordsKeepUnknownFieldsUnknown() throws Exception {
		importLog("legacy-import-1", legacy("g1", "MAGE", LifecycleEvents.OUTCOME_DIED, 4, 7, 900));
		FactTable t = new DerivedStore(root()).build();
		assertEquals(1, t.legacyRows.size());
		RunFacts f = t.legacyRows.get(0);
		assertEquals("legacy:g1", f.id);
		assertEquals(900, f.numOr(FactKeys.SCORE, -1));
		assertEquals(Fact.PROV_LEGACY, f.get(FactKeys.SCORE).prov);
		assertFalse("a date the old record lacked is unknown", f.isKnown(FactKeys.DATE));
		assertFalse(f.isKnown(FactKeys.HERO_ATTACKS));
		assertEquals("legacy", f.textOr(FactKeys.STATUS, ""));
	}

	@Test
	public void aRankingIdLinksTheRecordToTheRunExactly() throws Exception {
		apdRun("r1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000, "g-exact");
		importLog("legacy-import-1", legacy("g-exact", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000),
				legacy("g-other", "ROGUE", LifecycleEvents.OUTCOME_WON, 26, 25, 90000));
		FactTable t = new DerivedStore(root()).build();
		RunFacts apd = t.apdRows.get(0);
		RunFacts leg = null;
		for (RunFacts r : t.legacyRows) if (r.id.equals("legacy:g-exact")) leg = r;
		assertEquals("r1", leg.textOr(FactKeys.LINKED_RUN, ""));
		assertEquals(FactKeys.LINK_EXACT, leg.numOr(FactKeys.LINK_KIND, 0));
		assertEquals("legacy:g-exact", apd.textOr(FactKeys.LINKED_LEGACY, ""));
		//a run is never counted twice, and nothing is deleted
		assertEquals(2, t.rows().size());
		assertEquals(3, t.allRows().size());
	}

	@Test
	public void aRunWithoutAnIdIsLinkedOnlyWhenExactlyOneRunMatches() throws Exception {
		apdRun("r1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000, null);
		importLog("legacy-import-1", legacy("g1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000));
		FactTable one = new DerivedStore(root()).build();
		assertEquals(FactKeys.LINK_PROBABLE, one.legacyRows.get(0).numOr(FactKeys.LINK_KIND, 0));
		assertEquals(1, one.rows().size());

		//two identical-looking runs: ambiguous, so neither is linked
		apdRun("r2", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000, null);
		FactTable two = new DerivedStore(root()).build();
		assertFalse(two.legacyRows.get(0).isKnown(FactKeys.LINKED_RUN));
		assertEquals(3, two.rows().size());
	}

	@Test
	public void aDifferentScoreIsNotTheSameRun() throws Exception {
		apdRun("r1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000, null);
		importLog("legacy-import-1", legacy("g1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2001));
		assertFalse(new DerivedStore(root()).build().legacyRows.get(0).isKnown(FactKeys.LINKED_RUN));
	}

	@Test
	public void theSameRecordInTwoImportsCountsOnce() throws Exception {
		LegacyEvents.Record r = legacy("g1", "MAGE", LifecycleEvents.OUTCOME_DIED, 4, 7, 900);
		importLog("legacy-import-1", r);
		importLog("legacy-import-2", r);
		assertEquals(1, new DerivedStore(root()).build().legacyRows.size());
	}

	@Test
	public void theCacheIsUsedWhileCurrentAndRebuiltWhenAnythingChanges() throws Exception {
		apdRun("r1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000, "g1");
		importLog("legacy-import-1", legacy("g1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000));
		DerivedStore store = new DerivedStore(root());
		FactTable built = store.load();
		assertEquals(DerivedStore.Source.REBUILT_NO_CACHE, store.lastSource());
		assertTrue(store.cacheFile().isFile());

		FactTable cached = store.load();
		assertEquals(DerivedStore.Source.CACHE, store.lastSource());
		assertSameTable(built, cached);

		//a new run makes the cache stale
		apdRun("r2", "ROGUE", LifecycleEvents.OUTCOME_WON, 26, 24, 80000, null);
		FactTable after = store.load();
		assertEquals(DerivedStore.Source.REBUILT_STALE, store.lastSource());
		assertEquals(2, after.apdRows.size());
		assertEquals(DerivedStore.Source.CACHE, reload(store));
	}

	@Test
	public void aSameLengthRawReplacementInvalidatesTheCache() throws Exception {
		apdRun("r1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000, null);
		DerivedStore store = new DerivedStore(root());
		assertEquals(2000, store.load().apdRows.get(0).numOr(FactKeys.SCORE, -1));
		assertEquals(DerivedStore.Source.CACHE, reload(store));

		File run = new File(new File(root(), "runs"), "r1.apdlog");
		long originalLength = run.length();
		assertTrue(run.delete());
		apdRun("r1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2001, null);
		assertEquals("the replacement exercises the old filename-and-length blind spot",
				originalLength, run.length());

		FactTable replaced = store.load();
		assertEquals(DerivedStore.Source.REBUILT_STALE, store.lastSource());
		assertEquals(2001, replaced.apdRows.get(0).numOr(FactKeys.SCORE, -1));
		assertEquals(DerivedStore.Source.CACHE, reload(store));
	}

	private DerivedStore.Source reload( DerivedStore s ){
		s.load();
		return s.lastSource();
	}

	@Test
	public void aDeletedOrDamagedCacheIsRebuiltIdentically() throws Exception {
		apdRun("r1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000, "g1");
		importLog("legacy-import-1", legacy("g1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000));
		DerivedStore store = new DerivedStore(root());
		FactTable original = store.load();

		store.deleteCache();
		FactTable fromRaw = store.load();
		assertEquals(DerivedStore.Source.REBUILT_NO_CACHE, store.lastSource());
		assertSameTable(original, fromRaw);

		//a cache cut short
		FileOutputStream out = new FileOutputStream(store.cacheFile());
		out.write(("APDD\t2\t" + DerivedStore.DERIVATION_VERSION + "\nR\t1\tx\nF\trun.id\t2\t1\tx").getBytes("UTF-8"));
		out.close();
		FactTable repaired = store.load();
		assertEquals(DerivedStore.Source.REBUILT_BAD_CACHE, store.lastSource());
		assertSameTable(original, repaired);

		//random bytes
		out = new FileOutputStream(store.cacheFile());
		out.write(new byte[]{1, 2, 3, 4, (byte) 200});
		out.close();
		assertSameTable(original, store.load());
		assertEquals(DerivedStore.Source.REBUILT_BAD_CACHE, store.lastSource());

		//the raw files were never touched by any of this
		assertTrue(new File(new File(root(), "runs"), "r1.apdlog").isFile());
		assertTrue(new File(new File(root(), "legacy"), "legacy-import-1.apdlog").isFile());
	}

	@Test
	public void rebuildAlwaysDerivesFromTheRawFiles() throws Exception {
		apdRun("r1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000, null);
		DerivedStore store = new DerivedStore(root());
		store.load();
		//poison the cache without changing any source file: load() would trust it, rebuild() must not
		FileOutputStream out = new FileOutputStream(store.cacheFile());
		out.write("APDD\t1\t1\nEND\n".getBytes("UTF-8"));
		out.close();
		FactTable t = store.rebuild();
		assertEquals(1, t.apdRows.size());
		assertEquals(1, store.load().apdRows.size());
	}

	@Test
	public void textWithTabsAndNewlinesSurvivesTheCache() throws Exception {
		LegacyEvents.Record r = legacy("g1", "MAGE", LifecycleEvents.OUTCOME_DIED, 4, 7, 900);
		r.putText(FactKeys.CUSTOM_SEED, "a\tb\nc\\d");
		importLog("legacy-import-1", r);
		DerivedStore store = new DerivedStore(root());
		FactTable first = store.load();
		FactTable second = store.load();
		assertEquals(DerivedStore.Source.CACHE, store.lastSource());
		assertEquals("a\tb\nc\\d", second.legacyRows.get(0).textOr(FactKeys.CUSTOM_SEED, ""));
		assertSameTable(first, second);
	}

	@Test
	public void coverageSaysHowManyRowsContainAFact() throws Exception {
		apdRun("r1", "WARRIOR", LifecycleEvents.OUTCOME_DIED, 5, 8, 2000, null);
		importLog("legacy-import-1", legacy("g1", "MAGE", LifecycleEvents.OUTCOME_DIED, 4, 7, 900));
		List<RunFacts> rows = new DerivedStore(root()).build().rows();
		assertEquals(2, rows.size());
		FactTable.Coverage score = FactTable.coverage(rows, FactKeys.SCORE);
		assertEquals(2, score.known);
		FactTable.Coverage attacks = FactTable.coverage(rows, FactKeys.HERO_ATTACKS);
		assertEquals(1, attacks.known);
		assertEquals(2, attacks.total);
		assertEquals("1 of 2 runs contain combat.heroAttacks", attacks.toString());
	}

	@Test
	public void totalsAndDailyScoresComeFromTheLatestImport() throws Exception {
		File f = new File(new File(root(), "legacy"), "legacy-import-5.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "legacy-import-5", "test", 1);
		LegacyEvents.Totals t1 = new LegacyEvents.Totals();
		t1.importedAtMillis = 10;
		t1.fields.put("legacy.totalNumber", LegacyEvents.Value.num(12));
		t1.fields.put("legacy.wonNumber", LegacyEvents.Value.UNKNOWN);
		LegacyEvents.emitTotals(w, 0, t1);
		LegacyEvents.Totals t2 = new LegacyEvents.Totals();
		t2.importedAtMillis = 20;
		t2.fields.put("legacy.totalNumber", LegacyEvents.Value.num(15));
		t2.fields.put("legacy.wonNumber", LegacyEvents.Value.num(2));
		LegacyEvents.emitTotals(w, 1, t2);
		LegacyEvents.DailyScore d = new LegacyEvents.DailyScore();
		d.day = 20250101; d.score = 4000;
		LegacyEvents.emitDailyScore(w, 2, d);
		w.close(true);
		FactTable t = new DerivedStore(root()).load();
		assertEquals(15, t.legacyTotals.numOr("legacy.totalNumber", -1));
		assertEquals(2, t.legacyTotals.numOr("legacy.wonNumber", -1));
		assertEquals(1, t.dailyScores.size());
		assertEquals(4000, t.dailyScores.get(0)[1]);
	}

	@Test
	public void anEmptyHistoryGivesAnEmptyTable(){
		FactTable t = new DerivedStore(root()).load();
		assertTrue(t.rows().isEmpty());
		assertNull(t.legacyTotals);
		assertNotNull(t.dailyScores);
	}

	private static void assertSameTable( FactTable a, FactTable b ){
		assertEquals(a.apdRows, b.apdRows);
		assertEquals(a.legacyRows, b.legacyRows);
		assertEquals(a.legacyTotals, b.legacyTotals);
		assertEquals(a.dailyScores.size(), b.dailyScores.size());
	}
}
