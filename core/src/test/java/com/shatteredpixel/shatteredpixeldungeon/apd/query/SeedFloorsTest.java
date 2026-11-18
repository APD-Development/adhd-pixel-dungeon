package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;
import com.shatteredpixel.shatteredpixeldungeon.apd.replay.FloorReplay;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ReplayFloor;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//Seed floors (Phase B) and the recorded-vs-canonical comparison (Phase C): provenance on every row, recorded floors authoritative.
public class SeedFloorsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Ruleset RULES = Ruleset.of(30, 0);

	@Before
	public void setUp(){
		HeadlessTestSupport.start();
		FloorReplay.setLiveGameCheck(null);
		com.shatteredpixel.shatteredpixeldungeon.apd.replay.SeedFloorsRunner.clearCache();
	}

	private static ReplayFloor floor( int depth, String... classes ){
		ReplayFloor f = new ReplayFloor();
		f.depth = depth;
		f.levelClass = "levels.SewerLevel";
		for (String c : classes){
			ReplayFloor.Item i = new ReplayFloor.Item();
			i.trueClass = c;
			i.container = "HEAP";
			i.quantity = 1;
			f.items.add(i);
		}
		ReplayFloor.Mob m = new ReplayFloor.Mob();
		m.mobClass = "actors.mobs.Rat";
		m.hp = 8;
		f.mobs.add(m);
		return f;
	}

	private static SeedFloors.Recorded rec( int floor, String cls ){
		SeedFloors.Recorded r = new SeedFloors.Recorded();
		r.floor = floor;
		r.trueClass = cls;
		r.container = "Heap";
		r.stateKnown = true;     //recorded at generation (level 0, not cursed, no enchantment)
		return r;
	}

	private static SeedFloors.Spec spec( int reached ){
		SeedFloors.Spec s = new SeedFloors.Spec();
		s.runId = "run-x";
		s.seed = 5;
		s.heroClass = "WARRIOR";
		s.reached = reached;
		s.ruleset = RULES.id;
		return s;
	}

	private static int basisOf( Row r, DatasetDef d, String col ){
		return (int) r.num(d.indexOf(col));
	}

	@Test
	public void recordedFloorsAreAuthoritativeAndOthersAreLabelledCanonical() {
		List<ReplayFloor> canonical = new ArrayList<>();
		for (int d = 1; d <= 6; d++) canonical.add(floor(d, "items.food.Food", "items.potions.PotionOfHealing"));
		List<SeedFloors.Recorded> recorded = new ArrayList<>();
		recorded.add(rec(1, "items.food.Food"));
		recorded.add(rec(1, "items.weapon.melee.Sword"));
		recorded.add(rec(2, "items.food.Food"));
		SeedFloors.Built b = SeedFloors.assemble(spec(4), recorded, canonical);

		assertEquals(2, b.recordedFloors);
		assertEquals(4, b.canonicalFloors);
		DatasetDef d = b.items.def;
		int recordedRows = 0;
		for (Row r : b.items.rows){
			int floor = (int) r.num(d.indexOf("sf.floor"));
			int basis = basisOf(r, d, "sf.basis");
			assertTrue(r.finished);
			if (floor <= 2){
				//a recorded floor holds only what the log says: no canonical item may stand in for it
				assertEquals(SeedFloors.BASIS_RECORDED, basis);
				assertEquals(Fact.PROV_RECORDED, r.prov(d.indexOf("sf.item")));
				recordedRows++;
			} else if (floor <= 4){
				assertEquals(SeedFloors.BASIS_UNRECORDED, basis);
				assertEquals(Fact.PROV_INFERRED, r.prov(d.indexOf("sf.item")));
			} else {
				assertEquals(SeedFloors.BASIS_UNREACHED, basis);
				assertEquals(Fact.PROV_INFERRED, r.prov(d.indexOf("sf.item")));
			}
		}
		assertEquals(3, recordedRows);
		//creatures: canonical floors only, every one labelled
		for (Row r : b.mobs.rows){
			assertTrue(r.num(b.mobs.def.indexOf("sm.floor")) >= 3);
			assertTrue(basisOf(r, b.mobs.def, "sm.basis") != SeedFloors.BASIS_RECORDED);
		}
		assertEquals(4, b.mobs.rows.size());
	}

	@Test
	public void unknownReachIsSaidSoNeverCalledUnreached() {
		List<ReplayFloor> canonical = new ArrayList<>();
		canonical.add(floor(1, "items.food.Food"));
		SeedFloors.Built b = SeedFloors.assemble(spec(-1), new ArrayList<SeedFloors.Recorded>(), canonical);
		assertEquals(SeedFloors.BASIS_REACH_UNKNOWN, basisOf(b.items.rows.get(0), b.items.def, "sf.basis"));
	}

	@Test
	public void comparisonCountsWhatIsTheSameAndWhatIsNot() {
		List<ReplayFloor> canonical = new ArrayList<>();
		canonical.add(floor(1, "items.food.Food", "items.potions.PotionOfHealing", "items.food.Food"));
		List<SeedFloors.Recorded> recorded = new ArrayList<>();
		recorded.add(rec(1, "items.food.Food"));
		recorded.add(rec(1, "items.food.Food"));
		recorded.add(rec(1, "items.weapon.melee.Sword"));
		SeedFloors.Built b = SeedFloors.assemble(spec(3), recorded, canonical);
		assertEquals(1, b.compare.rows.size());
		Row r = b.compare.rows.get(0);
		DatasetDef d = b.compare.def;
		assertEquals(3, r.num(d.indexOf("sc.recorded")));
		assertEquals(3, r.num(d.indexOf("sc.canonical")));
		assertEquals(2, r.num(d.indexOf("sc.same")));
		assertEquals(1, r.num(d.indexOf("sc.onlyRecorded")));
		assertEquals(1, r.num(d.indexOf("sc.onlyCanonical")));
		assertEquals(6666, r.num(d.indexOf("sc.agree")));
		assertEquals(SeedFloors.BASIS_COMPARISON, basisOf(r, d, "sc.basis"));
		//recorded counts are recorded facts, canonical counts are marked as worked out
		assertEquals(Fact.PROV_RECORDED, r.prov(d.indexOf("sc.recorded")));
		assertEquals(Fact.PROV_INFERRED, r.prov(d.indexOf("sc.canonical")));
	}

	@Test
	public void anItemWhoseGeneratedStateWasNotRecordedIsComparedByKindAndNeverCountedAsAMismatch() {
		ReplayFloor f = floor(1, "items.weapon.melee.Sword");
		ReplayFloor.Item ring = new ReplayFloor.Item();
		ring.trueClass = "items.rings.RingOfHaste";
		ring.level = 2;
		ring.container = "HEAP";
		ring.quantity = 1;
		f.items.add(ring);
		List<ReplayFloor> canonical = new ArrayList<>();
		canonical.add(f);
		List<SeedFloors.Recorded> recorded = new ArrayList<>();
		recorded.add(rec(1, "items.weapon.melee.Sword"));                 //state at generation recorded: level 0, same as canonical
		SeedFloors.Recorded ringSeen = rec(1, "items.rings.RingOfHaste");  //only a later state was recorded
		ringSeen.stateKnown = false;
		recorded.add(ringSeen);
		SeedFloors.Recorded wand = rec(1, "items.wands.WandOfFire");     //not on the canonical floor at all
		wand.stateKnown = false;
		recorded.add(wand);
		SeedFloors.Built b = SeedFloors.assemble(spec(3), recorded, canonical);
		Row r = b.compare.rows.get(0);
		DatasetDef d = b.compare.def;
		assertEquals(3, r.num(d.indexOf("sc.recorded")));
		assertEquals(2, r.num(d.indexOf("sc.canonical")));
		assertEquals(1, r.num(d.indexOf("sc.same")));
		assertEquals("same kind, state not recorded", 1, r.num(d.indexOf("sc.kindOnly")));
		assertEquals("only the wand, whose kind is genuinely not there", 1, r.num(d.indexOf("sc.onlyRecorded")));
		assertEquals(0, r.num(d.indexOf("sc.onlyCanonical")));
		assertEquals("agreement is over the items whose state could be compared", 5000, r.num(d.indexOf("sc.agree")));
		//the recorded item rows leave the unrecorded state unknown, not 0
		DatasetDef id = b.items.def;
		int unknownState = 0;
		for (Row row : b.items.rows){
			if (basisOf(row, id, "sf.basis") != SeedFloors.BASIS_RECORDED) continue;
			boolean known = row.isKnown(id.indexOf("sf.level"));
			assertEquals(row.text(id.indexOf("sf.item")).contains("Sword"), known);
			if (!known) unknownState++;
		}
		assertEquals(2, unknownState);
	}

	@Test
	public void recordedStateIsTheStateWhenGeneratedNeverTheFinalOneAndOnlyForItemsFirstRecordedAtGeneration() throws Exception {
		File root = tmp.newFolder("gen");
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion v = com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion.CURRENT;
		SyntheticLog log = new SyntheticLog(root, "g1", v, 100L);
		log.start("run-g", 0, "WARRIOR", RULES.id, 0, 30, false).enter(1)
				.registered("a", "items.weapon.melee.Sword", 0, false, com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.ORIGIN_GENERATED)
				.generated("a", com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents.C_CHEST, "Vault")
				.truthChanged("a", 3, true)       //play upgraded and cursed it afterwards
				.registered("b", "items.weapon.melee.Dagger", 2, false, com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.ORIGIN_OBSERVED)
				.generated("b", com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents.C_CHEST, "Vault")
				.end(com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Catalog cat = new DatasetStore(root).load();
		DatasetDef id = cat.dataset(Datasets.ITEMS).def;
		for (Row row : cat.dataset(Datasets.ITEMS).rows){
			if (row.text(id.indexOf("item.trueClass")).contains("Sword")){
				assertEquals("the final state is what the Items table shows", 3, row.num(id.indexOf("item.trueLevel")));
				assertEquals("the state at generation is kept apart", 0, row.num(id.indexOf("item.genLevel")));
			} else {
				assertFalse("first recorded some other way: how it was generated is not known", row.isKnown(id.indexOf("item.genLevel")));
			}
		}
		List<SeedFloors.Recorded> rec = SeedFloors.recorded(cat, "run-g");
		assertEquals(2, rec.size());
		for (SeedFloors.Recorded x : rec){
			if (x.trueClass.contains("Sword")){
				assertTrue(x.stateKnown);
				assertEquals(0, x.level);
				assertFalse(x.cursed);
			} else {
				assertFalse(x.stateKnown);
			}
		}
	}

	@Test
	public void everySeedFloorsViewRunsThroughTheQueryEngine() {
		List<ReplayFloor> canonical = new ArrayList<>();
		for (int d = 1; d <= 3; d++) canonical.add(floor(d, "items.food.Food"));
		SeedFloors.Built b = SeedFloors.assemble(spec(1), new ArrayList<SeedFloors.Recorded>(), canonical);
		Catalog cat = new Catalog();
		SeedFloors.install(cat, b);
		for (String id : new String[]{ SeedFloors.ITEMS, SeedFloors.MOBS, SeedFloors.COMPARE }){
			ViewDef v = cat.view(id);
			assertNotNull(id, v);
			QueryResult r = QueryEngine.run(cat.datasetOf(v), v.initialQuery().withShowInternal(true), RULES.id);
			if (!id.equals(SeedFloors.COMPARE)) assertFalse(id + " empty", r.rows.isEmpty());
		}
	}

	@Test
	public void spec_refusesRunsThatCannotBeReplayed() throws Exception {
		File sampleRoot = DemoData.sampleRootFor(new File(tmp.newFolder("data"), "apd"));
		DemoGenerator.generateSample(sampleRoot, RULES, 1_800_000_000_000L);
		Catalog cat = new DatasetStore(sampleRoot).load();
		Dataset runs = cat.dataset(Datasets.RUNS);
		Row finished = null;
		for (Row r : runs.rows) if (r.finished){ finished = r; break; }
		assertNotNull(finished);
		SeedFloors.Spec s = SeedFloors.spec(cat, finished);
		assertEquals(finished.runId, s.runId);
		assertFalse(s.heroClass.isEmpty());
		//an unfinished run is never replayed (its hidden information must stay hidden)
		finished.finished = false;
		try {
			SeedFloors.spec(cat, finished);
			fail("unfinished run accepted");
		} catch (SeedFloors.NotAvailable expected){
			assertTrue(expected.getMessage().contains("not finished"));
		} finally {
			finished.finished = true;
		}
	}

	@Test
	public void specRefusesARunWhoseRulesChangedAndOneWhoseRulesWereNeverRecorded() throws Exception {
		File sampleRoot = DemoData.sampleRootFor(new File(tmp.newFolder("data3"), "apd"));
		DemoGenerator.generateSample(sampleRoot, RULES, 1_800_000_000_000L);
		Catalog cat = new DatasetStore(sampleRoot).load();
		Row run = null;
		for (Row r : cat.dataset(Datasets.RUNS).rows) if (r.finished){ run = r; break; }
		String was = run.ruleset;
		run.ruleset = Row.RULESET_MIXED;
		try {
			SeedFloors.spec(cat, run);
			fail("a run whose rules changed has no single set of rules to replay");
		} catch (SeedFloors.NotAvailable expected){
			assertTrue(expected.getMessage().contains("rules changed"));
		} finally {
			run.ruleset = was;
		}
		run.imported = true;
		try {
			SeedFloors.spec(cat, run);
			fail("an imported record is not a recorded run");
		} catch (SeedFloors.NotAvailable expected){
			assertTrue(expected.getMessage().contains("imported"));
		} finally {
			run.imported = false;
		}
	}

	@Test
	public void aFinishedRunOfTheSampleHistoryBuildsAndOpensThroughTheModel() throws Exception {
		File sampleRoot = DemoData.sampleRootFor(new File(tmp.newFolder("data2"), "apd"));
		DemoGenerator.generateSample(sampleRoot, RULES, 1_800_000_000_000L);
		Catalog cat = new DatasetStore(sampleRoot).load();
		Row run = null;
		DatasetDef d = cat.dataset(Datasets.RUNS).def;
		for (Row r : cat.dataset(Datasets.RUNS).rows){
			if (r.finished && r.isKnown(d.indexOf("run.seed")) && r.isKnown(d.indexOf("run.heroClass"))){
				run = r;
				break;
			}
		}
		assertNotNull("the sample history has a replayable run", run);

		UimsdModel m = new UimsdModel(RULES.id, "sample");
		m.setCatalog(cat);
		m.setSeedFloorsProvider(com.shatteredpixel.shatteredpixeldungeon.apd.replay.SeedFloorsRunner.INSTANCE);
		m.selectView("runs");
		assertTrue(m.canSeedFloors(run));
		java.util.concurrent.atomic.AtomicReference<String> publishedNotice = new java.util.concurrent.atomic.AtomicReference<>();
		m.setListener(() -> { if (SeedFloors.ITEMS.equals(m.viewId())) publishedNotice.set(m.notice()); });
		m.startSeedFloors(run);
		long end = System.currentTimeMillis() + 60000;
		while (!SeedFloors.ITEMS.equals(m.viewId()) && System.currentTimeMillis() < end){
			Thread.sleep(50);
		}
		assertEquals("notice: " + m.notice(), SeedFloors.ITEMS, m.viewId());
		assertTrue(m.query().showInternal);
		assertFalse(m.result().rows.isEmpty());
		assertEquals("", m.notice());
		assertEquals("The first success notification must already have retired its busy notice", "", publishedNotice.get());
		//Back leads to the run table again
		assertTrue(m.back());
		assertEquals("runs", m.viewId());
	}

	private UimsdModel modelAtRuns( String folder, boolean hiddenOn, Row[] runOut ) throws Exception {
		File sampleRoot = DemoData.sampleRootFor(new File(tmp.newFolder(folder), "apd"));
		DemoGenerator.generateSample(sampleRoot, RULES, 1_800_000_000_000L);
		Catalog cat = new DatasetStore(sampleRoot).load();
		DatasetDef d = cat.dataset(Datasets.RUNS).def;
		for (Row r : cat.dataset(Datasets.RUNS).rows){
			if (r.finished && r.isKnown(d.indexOf("run.seed")) && r.isKnown(d.indexOf("run.heroClass")) && r.isKnown(d.indexOf("run.exoticChance"))){
				runOut[0] = r;
				break;
			}
		}
		UimsdModel m = new UimsdModel(RULES.id, "sample");
		m.setCatalog(cat);
		m.setSeedFloorsProvider(com.shatteredpixel.shatteredpixeldungeon.apd.replay.SeedFloorsRunner.INSTANCE);
		m.selectView("runs");
		m.setShowInternal(hiddenOn);
		return m;
	}

	private static void openSeedFloors( UimsdModel m, Row run ) throws Exception {
		m.startSeedFloors(run);
		long end = System.currentTimeMillis() + 60000;
		while (!SeedFloors.ITEMS.equals(m.viewId()) && System.currentTimeMillis() < end) Thread.sleep(50);
		assertEquals("notice: " + m.notice(), SeedFloors.ITEMS, m.viewId());
		assertTrue("the seed view shows what it needs", m.query().showInternal);
	}

	@Test
	public void leavingSeedFloorsRestoresThePlayersHiddenInformationChoice() throws Exception {
		Row[] run = new Row[1];
		//off before: Back and opening another view both bring it back off
		UimsdModel m = modelAtRuns("hid1", false, run);
		openSeedFloors(m, run[0]);
		assertTrue(m.back());
		assertEquals("runs", m.viewId());
		assertFalse("Back restores the choice", m.query().showInternal);

		openSeedFloors(m, run[0]);
		m.selectView("characters");
		assertFalse("another view starts from the player's own choice, not from the seed view's", m.query().showInternal);
		m.selectView("runs");
		assertFalse("and it stays off", m.query().showInternal);

		//on before: it stays on
		UimsdModel on = modelAtRuns("hid2", true, run);
		openSeedFloors(on, run[0]);
		assertTrue(on.back());
		assertTrue(on.query().showInternal);
	}

	@Test
	public void aLiveGameMakesItANoticeNotACrash() throws Exception {
		File sampleRoot = DemoData.sampleRootFor(new File(tmp.newFolder("data3"), "apd"));
		DemoGenerator.generateSample(sampleRoot, RULES, 1_800_000_000_000L);
		Catalog cat = new DatasetStore(sampleRoot).load();
		Row run = null;
		DatasetDef d = cat.dataset(Datasets.RUNS).def;
		for (Row r : cat.dataset(Datasets.RUNS).rows){
			if (r.finished && r.isKnown(d.indexOf("run.seed")) && r.isKnown(d.indexOf("run.heroClass"))){
				run = r;
				break;
			}
		}
		FloorReplay.setLiveGameCheck(new FloorReplay.LiveGameCheck(){
			@Override
			public String liveGame(){
				return "a game is loaded";
			}
		});
		try {
			UimsdModel m = new UimsdModel(RULES.id, "sample");
			m.setCatalog(cat);
		m.setSeedFloorsProvider(com.shatteredpixel.shatteredpixeldungeon.apd.replay.SeedFloorsRunner.INSTANCE);
			m.selectView("runs");
			m.startSeedFloors(run);
			long end = System.currentTimeMillis() + 30000;
			while (m.notice().startsWith("Replaying") && System.currentTimeMillis() < end) Thread.sleep(50);
			assertTrue(m.notice(), m.notice().contains("a game is loaded"));
			assertEquals("runs", m.viewId());
		} finally {
			FloorReplay.setLiveGameCheck(null);
		}
	}
}
