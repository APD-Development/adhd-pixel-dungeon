package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.SubmissionQueue;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.UimsdUnlock;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

//UIMSD Demo Unlock: the demo runs are marked and kept apart, the unlock is permanent, the button is used up for good, deleting demo data is safe.
public class UimsdUnlockTest {

	private static final Ruleset RULES = Ruleset.of(30, 0);
	private static final TelemetryVersion V = TelemetryVersion.CURRENT;
	private static final long NOW = 1_800_000_000_000L;

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File parent, realRoot, demoRoot;

	//the two flags as the game keeps them: they survive "restarts" because tests copy them into a new object
	private static final class MemFlags implements UimsdUnlock.Flags {
		boolean unlocked, demoUsed, tour, pending, shown;
		public boolean unlocked(){ return unlocked; }
		public void setUnlocked(){ unlocked = true; }
		public boolean demoUsed(){ return demoUsed; }
		public void setDemoUsed(){ demoUsed = true; }
		public boolean tourUnlocked(){ return tour; }
		public void setTourUnlocked(){ tour = true; }
		public boolean promptPending(){ return pending; }
		public void setPromptPending(){ pending = true; }
		public boolean promptShown(){ return shown; }
		public void setPromptShown(){ shown = true; }
		MemFlags restart(){
			MemFlags f = new MemFlags();
			f.unlocked = unlocked;
			f.demoUsed = demoUsed;
			f.tour = tour;
			f.pending = pending;
			f.shown = shown;
			return f;
		}
	}

	@Before
	public void setUp() throws Exception {
		HeadlessTestSupport.start();
		parent = tmp.newFolder("data");
		realRoot = new File(parent, "apd");
		demoRoot = DemoData.demoRootFor(realRoot);
	}

	@After
	public void tearDown(){
		ApdRecorder.reset();
	}

	private void genuineRun( int n, boolean finished ){
		SyntheticLog l = new SyntheticLog(realRoot, "real-" + n, V, 1000L * n).start("real-run-" + n, 0, "WARRIOR", RULES.id, 0, 30, false).enter(1)
				.acquired("axe" + n, "axe", ItemEvents.CAT_WEAPON, false, 0);
		if (finished) l.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 10);
		l.close(finished);
	}

	private static Map<String, String> snapshot( File dir ) throws Exception {
		Map<String, String> m = new TreeMap<>();
		walk(dir, dir, m);
		return m;
	}

	private static void walk( File base, File f, Map<String, String> m ) throws Exception {
		File[] kids = f.listFiles();
		if (kids == null) return;
		for (File k : kids){
			if (k.isDirectory()) walk(base, k, m);
			else m.put(base.toURI().relativize(k.toURI()).getPath(), k.length() + ":" + java.util.Arrays.hashCode(Files.readAllBytes(k.toPath())));
		}
	}

	// ---- the ten demo runs ----

	@Test
	public void theTenDemoRunsAreVariedMarkedAndTheSameEveryTime() throws Exception {
		assertEquals(10, DemoGenerator.generateUnlockSet(demoRoot, RULES, NOW));
		Catalog cat = new DatasetStore(demoRoot).load();
		assertTrue("every row is flagged as made-up data", cat.synthetic);
		assertTrue(DemoData.isSyntheticRoot(demoRoot));
		Dataset runs = cat.dataset(Datasets.RUNS);
		assertEquals(10, runs.rows.size());
		Set<String> classes = new HashSet<>();
		Set<Long> outcomes = new HashSet<>();
		int cClass = runs.def.indexOf(FactKeys.HERO_CLASS), cOutcome = runs.def.indexOf(FactKeys.OUTCOME);
		int finished = 0;
		for (Row r : runs.rows){
			if (r.finished){
				finished++;
				outcomes.add(r.num(cOutcome));
			}
			classes.add(r.text(cClass));
		}
		//nine finished and one still going, so the rules that hide a running game's secrets have something to show
		assertEquals(9, finished);
		assertTrue("several hero classes: " + classes, classes.size() >= 3);
		assertTrue("both wins and losses: " + outcomes, outcomes.size() >= 2);
		for (File f : new File(demoRoot, "runs").listFiles()){
			if (f.getName().endsWith(".apdlog")) assertTrue("every log is named as demo data", f.getName().startsWith(DemoData.RUN_PREFIX));
		}
		//the logs are the history (the folder also holds rebuildable caches that the catalog load wrote)
		Map<String, String> first = snapshot(new File(demoRoot, "runs"));
		assertEquals(10, DemoGenerator.generateUnlockSet(demoRoot, RULES, NOW));
		assertEquals("same input, same demo history", first, snapshot(new File(demoRoot, "runs")));
	}

	@Test
	public void demoRunsNeverCountAsGenuineAndNeverReachTheRealCatalogOrTheSubmissionQueue() throws Exception {
		genuineRun(1, true);
		genuineRun(2, false);
		DemoGenerator.generateUnlockSet(demoRoot, RULES, NOW);
		assertEquals("only the one finished genuine run counts", 1, UimsdUnlock.genuineRuns(realRoot, 100));
		Catalog real = new DatasetStore(realRoot).load();
		assertFalse(real.synthetic);
		assertEquals("the real catalog holds the player's own runs only", 2, real.dataset(Datasets.RUNS).rows.size());
		//a demo run cannot be queued for submission, from either folder
		//the folder also holds caches, and listing order differs between file systems: take a real log
		String demoId = null;
		for (File f : new File(demoRoot, "runs").listFiles()){
			if (f.getName().endsWith(".apdlog") && (demoId == null || f.getName().compareTo(demoId) < 0)) demoId = f.getName().replace(".apdlog", "");
		}
		assertNotNull(demoId);
		assertEquals(SubmissionQueue.Result.REFUSED, SubmissionQueue.enqueueRun(demoRoot, demoId, "b", 1).result);
		assertEquals(0, SubmissionQueue.pending(demoRoot));
		assertEquals(SubmissionQueue.Result.REFUSED, SubmissionQueue.enqueueRun(realRoot, demoId, "b", 1).result);
	}

	// ---- the unlock ----

	@Test
	public void theDemoRouteUnlocksOnceAndTheButtonNeverComesBack() throws Exception {
		MemFlags flags = new MemFlags();
		assertFalse("locked at first", UimsdUnlock.isUnlocked(flags, realRoot, true));
		assertTrue(UimsdUnlock.demoButtonAvailable(flags, realRoot, true));
		assertEquals(UimsdUnlock.Generated.DONE, UimsdUnlock.generateDemo(flags, realRoot, RULES, NOW, true));
		assertTrue(UimsdUnlock.isUnlocked(flags, realRoot, true));
		assertFalse("the button is gone", UimsdUnlock.demoButtonAvailable(flags, realRoot, true));
		assertEquals("and cannot be used a second time", UimsdUnlock.Generated.NOT_AVAILABLE, UimsdUnlock.generateDemo(flags, realRoot, RULES, NOW, true));

		//delete every demo run: UIMSD stays open and the button stays gone
		assertTrue(UimsdUnlock.deleteDemoData(realRoot));
		assertFalse(demoRoot.exists());
		assertTrue(UimsdUnlock.isUnlocked(flags, realRoot, true));
		assertFalse(UimsdUnlock.demoButtonAvailable(flags, realRoot, true));

		//a restart: the flags come back from storage, the demo data is not needed for either
		MemFlags after = flags.restart();
		assertTrue(UimsdUnlock.isUnlocked(after, realRoot, true));
		assertFalse(UimsdUnlock.demoButtonAvailable(after, realRoot, true));
		assertEquals(0, UimsdUnlock.genuineRuns(realRoot, 100));
	}

	@Test
	public void tenGenuineRunsUnlockForGoodAndDeletingThemDoesNotLockAgain() throws Exception {
		MemFlags flags = new MemFlags();
		for (int i = 1; i <= 9; i++) genuineRun(i, true);
		assertFalse("nine are not enough", UimsdUnlock.isUnlocked(flags, realRoot, true));
		assertTrue(UimsdUnlock.demoButtonAvailable(flags, realRoot, true));
		genuineRun(10, false);
		assertFalse("an unfinished run is not a completed one", UimsdUnlock.isUnlocked(flags, realRoot, true));
		genuineRun(11, true);
		assertTrue("ten finished runs open it", UimsdUnlock.isUnlocked(flags, realRoot, true));
		assertTrue("and that is remembered", flags.unlocked);
		assertFalse("the demo button is not offered once it is open", UimsdUnlock.demoButtonAvailable(flags, realRoot, true));
		File[] logs = new File(realRoot, "runs").listFiles();
		for (File f : logs) f.delete();
		assertTrue("deleting the runs does not lock it again", UimsdUnlock.isUnlocked(flags.restart(), realRoot, true));
	}

	@Test
	public void withTheGateOffEverythingStaysOpenAndNoButtonIsShown() throws Exception {
		MemFlags flags = new MemFlags();
		assertTrue(UimsdUnlock.isUnlocked(flags, realRoot, false));
		assertFalse(UimsdUnlock.demoButtonAvailable(flags, realRoot, false));
		assertEquals(UimsdUnlock.Generated.NOT_AVAILABLE, UimsdUnlock.generateDemo(flags, realRoot, RULES, NOW, false));
		assertFalse("nothing was recorded", flags.unlocked || flags.demoUsed);
	}

	@Test
	public void aFailedGenerationChangesNothingAndNeverTouchesAForeignFolder() throws Exception {
		//something that is not demo data is sitting where the demo folder would go
		assertTrue(demoRoot.mkdirs());
		File mine = new File(demoRoot, "important.txt");
		Files.write(mine.toPath(), "keep".getBytes());
		MemFlags flags = new MemFlags();
		assertEquals(UimsdUnlock.Generated.FAILED, UimsdUnlock.generateDemo(flags, realRoot, RULES, NOW, true));
		assertFalse("still locked, button still there", flags.unlocked || flags.demoUsed);
		assertTrue(UimsdUnlock.demoButtonAvailable(flags, realRoot, true));
		assertTrue("the foreign file is untouched", mine.isFile());
	}

	// ---- deleting is safe ----

	@Test
	public void deletingDemoDataLeavesGenuineHistoryAndSavesAlone() throws Exception {
		genuineRun(1, true);
		genuineRun(2, true);
		File saves = new File(parent, "game1");
		assertTrue(saves.mkdirs());
		Files.write(new File(saves, "game.dat").toPath(), "an active save".getBytes());
		DemoGenerator.generateUnlockSet(demoRoot, RULES, NOW);
		Map<String, String> realBefore = snapshot(realRoot), savesBefore = snapshot(saves);
		assertTrue(UimsdUnlock.deleteDemoData(realRoot));
		assertFalse(demoRoot.exists());
		assertEquals(realBefore, snapshot(realRoot));
		assertEquals(savesBefore, snapshot(saves));
		assertEquals("a second delete with nothing there is fine", true, UimsdUnlock.deleteDemoData(realRoot));
		assertNotNull(new DatasetStore(realRoot).load().dataset(Datasets.RUNS));
	}

	// ---- the guided tour ----

	@Test
	public void theTourStaysClosedAfterTheDemoRouteAndOpensOnlyWithTenGenuineRuns() throws Exception {
		MemFlags flags = new MemFlags();
		assertFalse("closed at first", UimsdUnlock.isTourUnlocked(flags, realRoot, true));
		assertEquals(UimsdUnlock.Generated.DONE, UimsdUnlock.generateDemo(flags, realRoot, RULES, NOW, true));
		assertTrue("UIMSD itself is open", UimsdUnlock.isUnlocked(flags, realRoot, true));
		assertFalse("but the tour is not", UimsdUnlock.isTourUnlocked(flags, realRoot, true));
		assertFalse(flags.tour);

		//ten demo runs are not ten genuine runs, nine genuine runs are not enough
		for (int i = 1; i <= 9; i++) genuineRun(i, true);
		assertFalse("demo runs plus nine genuine runs: still closed", UimsdUnlock.isTourUnlocked(flags, realRoot, true));

		//deleting the demo data and restarting change nothing
		assertTrue(UimsdUnlock.deleteDemoData(realRoot));
		MemFlags restarted = flags.restart();
		assertFalse(UimsdUnlock.isTourUnlocked(restarted, realRoot, true));
		assertTrue(UimsdUnlock.isUnlocked(restarted, realRoot, true));

		//the tenth genuine run opens the tour, even though UIMSD was unlocked by the demo route
		genuineRun(10, true);
		assertTrue(UimsdUnlock.isTourUnlocked(restarted, realRoot, true));
		assertTrue("and that is remembered", restarted.tour);

		//deleting the runs afterwards does not close it again, also after a restart
		for (File f : new File(realRoot, "runs").listFiles()) f.delete();
		assertTrue(UimsdUnlock.isTourUnlocked(restarted.restart(), realRoot, true));
	}

	@Test
	public void tenGenuineRunsOpenUimsdAndTheTourTogetherAndTheGateOffOpensBoth() throws Exception {
		MemFlags flags = new MemFlags();
		for (int i = 1; i <= 10; i++) genuineRun(i, true);
		assertTrue(UimsdUnlock.isUnlocked(flags, realRoot, true));
		assertTrue(flags.tour);
		assertTrue(UimsdUnlock.isTourUnlocked(new MemFlags(), realRoot, false));
		MemFlags fresh = new MemFlags();
		assertTrue("gate off: the tour is open for everyone", UimsdUnlock.isTourUnlocked(fresh, tmp.newFolder("empty"), false));
		assertFalse("and nothing was recorded", fresh.tour);
	}

	// ---- the tenth genuine run ----

	@Test
	public void theTenthGenuineRunQueuesTheOneTimePromptAndNothingElseDoes() throws Exception {
		MemFlags flags = new MemFlags();
		for (int i = 1; i <= 9; i++){
			genuineRun(i, true);
			assertFalse("run " + i + " is not the tenth", UimsdUnlock.onRunEnded(flags, realRoot, true));
		}
		assertFalse(flags.pending);
		genuineRun(10, false);
		assertFalse("a run that is not finished is not a completed run", UimsdUnlock.onRunEnded(flags, realRoot, true));
		genuineRun(11, true);
		assertTrue("the tenth finished run queues the prompt", UimsdUnlock.onRunEnded(flags, realRoot, true));
		assertTrue(flags.pending);
		assertTrue("and opens UIMSD and the tour", flags.unlocked && flags.tour);
		assertFalse("asking again does not queue it twice", UimsdUnlock.onRunEnded(flags, realRoot, true));

		//the arrow stays due, however many times the title screen is built, until UIMSD is opened
		assertTrue(UimsdUnlock.promptActive(flags));
		assertTrue(UimsdUnlock.promptActive(flags.restart()));
		assertTrue("opening UIMSD retires it", UimsdUnlock.retirePrompt(flags));
		assertFalse(UimsdUnlock.promptActive(flags));
		assertFalse("and only once", UimsdUnlock.retirePrompt(flags));
		assertTrue(flags.shown);

		//after a restart it is still retired, and later runs never bring it back
		MemFlags after = flags.restart();
		assertFalse(UimsdUnlock.promptActive(after));
		genuineRun(12, true);
		assertFalse("the eleventh and later runs never prompt", UimsdUnlock.onRunEnded(after, realRoot, true));
	}

	@Test
	public void aQueuedPromptSurvivesARestartUntilItIsShown() throws Exception {
		MemFlags flags = new MemFlags();
		for (int i = 1; i <= 10; i++) genuineRun(i, true);
		assertTrue(UimsdUnlock.onRunEnded(flags, realRoot, true));
		MemFlags restarted = flags.restart();
		assertTrue("the game was closed before UIMSD was opened: still due", UimsdUnlock.promptActive(restarted));
		assertTrue(UimsdUnlock.retirePrompt(restarted));
		assertFalse(UimsdUnlock.promptActive(restarted.restart()));
	}

	@Test
	public void demoRunsNeverTriggerThePromptAndTheDemoRouteDoesNotSpendIt() throws Exception {
		MemFlags flags = new MemFlags();
		assertEquals(UimsdUnlock.Generated.DONE, UimsdUnlock.generateDemo(flags, realRoot, RULES, NOW, true));
		assertFalse("ten demo runs are not ten genuine runs", UimsdUnlock.onRunEnded(flags, realRoot, true));
		assertFalse(flags.pending);
		for (int i = 1; i <= 9; i++) genuineRun(i, true);
		assertFalse(UimsdUnlock.onRunEnded(flags, realRoot, true));
		genuineRun(10, true);
		assertTrue("the tenth genuine run still gets its prompt, after the demo shortcut", UimsdUnlock.onRunEnded(flags, realRoot, true));
		assertTrue(flags.tour);
		assertTrue(UimsdUnlock.promptActive(flags));
		assertFalse("exploring UIMSD through the demo before did not retire it", flags.shown);
	}

	@Test
	public void withTheGateOffNothingIsQueued() throws Exception {
		MemFlags flags = new MemFlags();
		for (int i = 1; i <= 10; i++) genuineRun(i, true);
		assertFalse(UimsdUnlock.onRunEnded(flags, realRoot, false));
		assertFalse(flags.pending);
	}
}
