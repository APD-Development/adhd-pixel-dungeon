package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoLog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellFormatter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Dataset;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Datasets;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Filter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Op;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Query;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryEngine;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Scope;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ViewDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;
import com.shatteredpixel.shatteredpixeldungeon.apd.replay.FloorReplay;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

//The difficulty framework: levels and neutrality, ruleset identity, persistence (settings and run), telemetry 1.7, UIMSD visibility/filtering,
//legacy runs, and that difficulty 3 (and, until balance is approved, every level) leaves gameplay exactly as it was.
public class DifficultyTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private int savedSetting;

	@Before
	public void setUp(){
		HeadlessTestSupport.start();
		savedSetting = SPDSettings.apdDifficulty();
	}

	@After
	public void tearDown(){
		SPDSettings.apdDifficulty(savedSetting);
		Dungeon.difficulty = Difficulty.DEFAULT;
		Dungeon.difficultyKnown = true;
		Dungeon.difficultyBalance = 0;
	}

	//a run at this level with the newest balance, as a run started now would be
	private static void level( int l ){
		Dungeon.difficulty = l;
		Dungeon.difficultyBalance = Difficulty.balanceRevision(l);
	}

	//---- levels and neutrality ----

	@Test
	public void levelsAreOneToFiveAndThreeIsTheBaseline() {
		assertEquals(1, Difficulty.MIN);
		assertEquals(3, Difficulty.DEFAULT);
		assertEquals(5, Difficulty.MAX);
		assertEquals(3, Difficulty.normalize(0));
		assertEquals(3, Difficulty.normalize(9));
		assertEquals(4, Difficulty.normalize(4));
		assertTrue(Difficulty.isValid(1) && Difficulty.isValid(5) && !Difficulty.isValid(0) && !Difficulty.isValid(6));
		for (int l = 1; l <= 5; l++) assertFalse(Difficulty.label(l).isEmpty());
	}

	@Test
	public void levelThreeIsNeutralWhateverTheTableSaysAndHasNoBalanceRevision() throws Exception {
		Field f = Difficulty.class.getDeclaredField("TABLE");
		f.setAccessible(true);
		double[][] table = (double[][]) f.get(null);
		double was = table[2][0];
		try {
			table[2][0] = 2.5;   //even a corrupted table cannot move the baseline
			for (Difficulty.Modifier m : Difficulty.Modifier.values()) assertEquals(1.0, Difficulty.modifier(3, m), 0.0);
			assertTrue(Difficulty.isNeutral(3));
		} finally {
			table[2][0] = was;
		}
		assertEquals(0, Difficulty.balanceRevision(3));
	}

	@Test
	public void theFirstBalancePassIsExactlyTheApprovedValues() {
		double[][] want = {   // enemy HP, enemy damage, resources
				{ 0.70, 0.70, 1.40 }, { 0.85, 0.85, 1.20 }, { 1.0, 1.0, 1.0 }, { 1.20, 1.20, 0.85 }, { 1.50, 1.50, 0.70 } };
		int[] revision = { 1, 1, 0, 1, 1 };
		for (int l = 1; l <= 5; l++){
			assertEquals(want[l - 1][0], Difficulty.modifier(l, Difficulty.Modifier.ENEMY_HP), 0.0);
			assertEquals(want[l - 1][1], Difficulty.modifier(l, Difficulty.Modifier.ENEMY_DAMAGE), 0.0);
			assertEquals(want[l - 1][2], Difficulty.modifier(l, Difficulty.Modifier.RESOURCES), 0.0);
			assertEquals("balance revision of level " + l, revision[l - 1], Difficulty.balanceRevision(l));
			//everything else stays neutral in this pass: healing, food, spawns, bosses, champions, traps
			for (Difficulty.Modifier m : Difficulty.Modifier.values()){
				if (m == Difficulty.Modifier.ENEMY_HP || m == Difficulty.Modifier.ENEMY_DAMAGE || m == Difficulty.Modifier.RESOURCES) continue;
				assertEquals(m + " must stay neutral at level " + l, 1.0, Difficulty.modifier(l, m), 0.0);
			}
		}
		assertTrue(Difficulty.isNeutral(3));
		for (int l : new int[]{ 1, 2, 4, 5 }) assertFalse(Difficulty.isNeutral(l));
	}

	@Test
	public void roundingIsHalfUpWithExactHalvesAndEveryBoundaryIsPinned() {
		int[] pct = { 70, 85, 100, 120, 150, 140 };
		double[] k = { 0.70, 0.85, 1.0, 1.20, 1.50, 1.40 };
		for (int i = 0; i < pct.length; i++){
			for (int v = 0; v <= 600; v++){
				int expected = (v * pct[i] + 50) / 100;     //exact integer half-up
				assertEquals(v + " x " + pct[i] + "%", expected, Difficulty.scale(k[i], v));
			}
		}
		//the exact halves that binary floating point gets wrong
		assertEquals(4, Difficulty.scale(0.70, 5));    //3.5 -> 4
		assertEquals(9, Difficulty.scale(0.85, 10));   //8.5 -> 9
		assertEquals(1, Difficulty.scale(0.70, 1));    //0.7 -> 1
		assertEquals(7, Difficulty.scale(0.70, 10));
		assertEquals(12, Difficulty.scale(1.20, 10));
		assertEquals(15, Difficulty.scale(1.50, 10));
	}

	@Test
	public void ordinaryItemCountsAtEveryLevelForEveryPossibleBase() {
		//a regular level draws 3-5 ordinary items (+2 on a Large level): bases 3..7. Rows: level 1, 2, 3, 4, 5.
		int[][] want = { { 4, 6, 7, 8, 10 }, { 4, 5, 6, 7, 8 }, { 3, 4, 5, 6, 7 }, { 3, 3, 4, 5, 6 }, { 2, 3, 4, 4, 5 } };
		for (int level = 1; level <= 5; level++){
			level(level);
			for (int base = 3; base <= 7; base++){
				assertEquals("level " + level + " base " + base, want[level - 1][base - 3], DifficultyHooks.ordinaryItemCount(base));
			}
			assertEquals("never below one item", true, DifficultyHooks.ordinaryItemCount(1) >= 1);
		}
		level(3);
		assertEquals(0, DifficultyHooks.ordinaryItemCount(0));
	}

	@Test
	public void enemyHealthAndDamageAtEveryLevel() {
		Rat base = new Rat();
		int ht = base.HT;
		float[] dmgK = { 0.70f, 0.85f, 1.0f, 1.20f, 1.50f };
		for (int level = 1; level <= 5; level++){
			level(level);
			Rat m = new Rat();
			DifficultyHooks.onMobCreated(m);
			assertEquals("health at level " + level, Difficulty.scale(Difficulty.modifier(level, Difficulty.Modifier.ENEMY_HP), ht), m.HT);
			assertEquals(m.HT, m.HP);
			assertEquals(10f * dmgK[level - 1], DifficultyHooks.attackDamage(m, 10f), 1e-4f);
			assertEquals(1f * dmgK[level - 1], DifficultyHooks.attackDamage(m, 1f), 1e-5f);
		}
		level(3);
		Rat m = new Rat();
		DifficultyHooks.onMobCreated(m);
		assertEquals(ht, m.HT);
		//a boss is never scaled
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo boss = new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo();
		int bossHt = boss.HT;
		level(5);
		DifficultyHooks.onMobCreated(boss);
		assertEquals(bossHt, boss.HT);
		assertEquals(10f, DifficultyHooks.attackDamage(boss, 10f), 0f);
	}

	@Test
	public void scalingIsExactWhenNeutralAndRoundsOnlyWhenNot() throws Exception {
		level(3);
		for (float v : new float[]{ 0f, 1f, 7.3f, 123456.789f, Float.MIN_VALUE }) assertEquals(Float.floatToIntBits(v), Float.floatToIntBits(Difficulty.scale(Difficulty.Modifier.ENEMY_DAMAGE, v)));
		for (int v : new int[]{ 0, 1, 17, Integer.MAX_VALUE }) assertEquals(v, Difficulty.scale(Difficulty.Modifier.ENEMY_HP, v));
		Field f = Difficulty.class.getDeclaredField("TABLE");
		f.setAccessible(true);
		double[][] table = (double[][]) f.get(null);
		double was = table[3][0];
		try {
			table[3][0] = 1.5;
			level(4);
			assertEquals(15, Difficulty.scale(Difficulty.Modifier.ENEMY_HP, 10));
			assertEquals(15f, Difficulty.scale(Difficulty.Modifier.ENEMY_HP, 10f), 0f);
			level(3);
			assertEquals(10, Difficulty.scale(Difficulty.Modifier.ENEMY_HP, 10));
		} finally {
			table[3][0] = was;
		}
	}

	//---- ruleset identity ----

	@Test
	public void difficultyThreeKeepsTheRulesetIdsOfBeforeAndOtherLevelsGetTheirOwn() {
		//the ids these rules had before difficulty existed (pinned): old runs stay in the same ruleset as new difficulty-3 runs
		assertEquals("r1-37d60350c6b1", Ruleset.of(30, 0).id);
		assertEquals("r1-1e283854c38e", Ruleset.of(0, 0).id);
		assertEquals(Ruleset.of(30, 0).id, Ruleset.of(30, 0, 3).id);
		assertEquals(Ruleset.of(30, 0).text, Ruleset.of(30, 0, 3).text);
		java.util.Set<String> ids = new java.util.HashSet<>();
		for (int l = 1; l <= 5; l++) ids.add(Ruleset.of(30, 0, l).id);
		assertEquals("every level is its own ruleset", 5, ids.size());
		assertTrue(Ruleset.of(30, 0, 4).text.contains("difficulty=4") && Ruleset.of(30, 0, 4).text.contains("balance=1"));
		assertNotEquals(Ruleset.of(30, 0, 4).id, Ruleset.of(0, 0, 4).id);
		assertNotEquals(Ruleset.of(30, 0, 4).id, Ruleset.of(30, 1, 4).id);
		assertEquals("a level that does not exist is the baseline", Ruleset.of(30, 0).id, Ruleset.of(30, 0, 9).id);
	}

	@Test
	public void theRulesetOfTheRunAndOfTheNextRunFollowTheRightSources() {
		SPDSettings.apdDifficulty(5);
		level(2);
		Dungeon.difficultyKnown = true;
		assertEquals("the run in progress uses its own difficulty", 2, difficultyOf(Ruleset.current()));
		assertEquals("a new run uses the setting", 5, difficultyOf(Ruleset.forNewRun()));
	}

	private static int difficultyOf( Ruleset r ){
		for (int l = 1; l <= 5; l++) if ((Ruleset.of(r.exoticChance, challengesOf(r), l).id.equals(r.id) || Ruleset.withHealingOnlyConversion(Ruleset.of(r.exoticChance, challengesOf(r), l)).id.equals(r.id))) return l;
		return -1;
	}

	private static int challengesOf( Ruleset r ){
		int i = r.text.indexOf("challenges=") + "challenges=".length();
		int e = r.text.indexOf(';', i);
		return Integer.parseInt(e < 0 ? r.text.substring(i) : r.text.substring(i, e));
	}

	//---- persistence ----

	@Test
	public void theSettingIsStoredAndValidated() {
		SPDSettings.apdDifficulty(4);
		assertEquals(4, SPDSettings.apdDifficulty());
		SPDSettings.apdDifficulty(99);
		assertEquals("an impossible value falls back to the normal game", 3, SPDSettings.apdDifficulty());
		SPDSettings.apdDifficulty(1);
		assertEquals(1, SPDSettings.apdDifficulty());
	}

	@Test
	public void aRunKeepsItsDifficultyWhenTheSettingChangesAndAcrossSaveAndLoad() throws Exception {
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		GamesInProgress.curSlot = 1;
		SPDSettings.apdDifficulty(4);
		Dungeon.init();
		assertEquals(4, Dungeon.difficulty);
		assertTrue(Dungeon.difficultyKnown);
		SPDSettings.apdDifficulty(2);
		assertEquals("changing the setting never changes a run that exists", 4, Dungeon.difficulty);
		Dungeon.depth = 1;
		Dungeon.saveGame(1);
		level(3);
		Dungeon.difficultyKnown = false;
		Dungeon.loadGame(1);
		assertEquals(4, Dungeon.difficulty);
		assertTrue(Dungeon.difficultyKnown);
		assertEquals("the setting is still what the player chose", 2, SPDSettings.apdDifficulty());
	}

	@Test
	public void aRunSavedBeforeDifficultyExistedStaysNotRecordedAndPlaysAsThree() throws Exception {
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		GamesInProgress.curSlot = 1;
		SPDSettings.apdDifficulty(5);
		Dungeon.init();
		Dungeon.difficultyKnown = false;     //what an older save looks like when it is written back
		level(3);
		Dungeon.depth = 1;
		Dungeon.saveGame(1);
		level(5);
		Dungeon.difficultyKnown = true;
		Dungeon.loadGame(1);
		assertEquals(3, Dungeon.difficulty);
		assertFalse("never labelled with a difficulty it did not record", Dungeon.difficultyKnown);
	}

	@Test
	public void aRunKeepsTheBalanceRevisionItStartedWithAndAnEarlySaveIsRevisionZero() throws Exception {
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		GamesInProgress.curSlot = 1;
		SPDSettings.apdDifficulty(4);
		Dungeon.init();
		assertEquals(Difficulty.balanceRevision(4), Dungeon.difficultyBalance);
		Dungeon.difficultyBalance = 0;       //a run started by the early build, whose levels had no modifiers
		Dungeon.depth = 1;
		Dungeon.saveGame(1);
		Dungeon.difficultyBalance = 1;
		Dungeon.loadGame(1);
		assertEquals("the run's own revision comes back, not the newest", 0, Dungeon.difficultyBalance);
		assertEquals(1.0, Difficulty.modifier(Difficulty.Modifier.ENEMY_HP), 0);
		assertEquals(Ruleset.withHealingOnlyConversion(Ruleset.of(SPDSettings.apdExoticChance(), Dungeon.challenges, 4, 0)).id, Ruleset.current().id);
		assertNotEquals(Ruleset.withHealingOnlyConversion(Ruleset.of(SPDSettings.apdExoticChance(), Dungeon.challenges, 4, 1)).id, Ruleset.current().id);

		Bundle b = new Bundle();
		b.put("apd_difficulty", 4);       //a save that has a difficulty but no revision at all
		Dungeon.difficultyBalance = 1;
		Dungeon.apdRestoreDifficulty(b);
		assertEquals(0, Dungeon.difficultyBalance);
		assertTrue(Dungeon.difficultyKnown);
		b = new Bundle();
		Dungeon.apdRestoreDifficulty(b);      //a save from before difficulty existed
		assertEquals(3, Dungeon.difficulty);
		assertEquals(0, Dungeon.difficultyBalance);
		assertFalse(Dungeon.difficultyKnown);
	}

	@Test
	public void aRunKeepsItsExoticSettingAcrossSettingChangesAndSaveLoadAndALegacySaveIsNeverGivenOne() throws Exception {
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		GamesInProgress.curSlot = 1;
		int was = SPDSettings.apdExoticSetting();
		try {
			SPDSettings.apdExoticChance(30);
			Dungeon.init();
			assertTrue(Dungeon.apdExoticKnown);
			assertEquals(30, Dungeon.apdExotic);
			String id = Ruleset.current().id;
			assertEquals(Ruleset.withHealingOnlyConversion(Ruleset.of(30, Dungeon.challenges, Dungeon.difficulty, Dungeon.difficultyBalance)).id, id);
            assertNotEquals("Historical global-conversion identity remains distinct", Ruleset.of(30, Dungeon.challenges, Dungeon.difficulty, Dungeon.difficultyBalance).id, id);

			SPDSettings.apdExoticChance(80);     //the player moves the slider in the middle of the run
			assertEquals("gameplay keeps the run's own value", 30, SPDSettings.apdExoticChance());
			assertEquals("the setting is what the NEXT run gets", 80, SPDSettings.apdExoticSetting());
			assertEquals("identity does not follow the setting", id, Ruleset.current().id);
			assertEquals(80, Ruleset.forNewRun().exoticChance);

			Dungeon.depth = 1;
			Dungeon.saveGame(1);
			Dungeon.apdExotic = 0;
			Dungeon.apdExoticKnown = false;
			Dungeon.loadGame(1);
			assertTrue(Dungeon.apdExoticKnown);
			assertEquals("saved with the run", 30, Dungeon.apdExotic);
			assertEquals(id, Ruleset.current().id);

			//a save from before the exotic setting was part of the run
			Bundle old = new Bundle();
			old.put("apd_difficulty", 3);
			Dungeon.apdRestoreDifficulty(old);
			assertFalse(Dungeon.apdExoticKnown);
			assertTrue("its rules are unknown, not today's", Ruleset.current().isUnknown());
			assertEquals(Ruleset.EXOTIC_NOT_RECORDED, Ruleset.current().exoticChance);
			assertEquals("it still plays with the live setting, as it always did", 80, SPDSettings.apdExoticChance());
		} finally {
			SPDSettings.apdExoticChance(was);
			Dungeon.apdExoticKnown = false;
			Dungeon.apdExotic = 0;
		}
	}

	@Test
	public void revisionZeroIsNeutralAtEveryLevelAndRevisionOneIsTheApprovedPass() {
		for (int level = 1; level <= 5; level++){
			for (Difficulty.Modifier m : Difficulty.Modifier.values()){
				assertEquals(1.0, Difficulty.modifier(level, 0, m), 0);
				assertEquals(Difficulty.modifier(level, m), Difficulty.modifier(level, Difficulty.balanceRevision(level), m), 0);
			}
			assertTrue(Difficulty.supportsRevision(level, 0));
			assertFalse(Difficulty.supportsRevision(level, 99));
		}
		assertTrue(Difficulty.supportsRevision(4, 1));
		assertFalse("level 3 has only revision 0", Difficulty.supportsRevision(3, 1));
	}

	@Test
	public void aReplayOfARevisionZeroRunUsesNoModifiersAndAnUnknownRevisionIsRefused() throws Exception {
		long seed = 123456L;
		List<com.shatteredpixel.shatteredpixeldungeon.apd.query.ReplayFloor> d3 = FloorReplay.replay(seed, 0, 30, 3, 0, HeroClass.WARRIOR, 3, null);
		List<com.shatteredpixel.shatteredpixeldungeon.apd.query.ReplayFloor> d4r0 = FloorReplay.replay(seed, 0, 30, 4, 0, HeroClass.WARRIOR, 3, null);
		assertEquals(d3.size(), d4r0.size());
		for (int i = 0; i < d3.size(); i++){
			assertEquals("a level-4 run with no modifiers builds the floors of level 3", d3.get(i).items.size(), d4r0.get(i).items.size());
		}
		try {
			FloorReplay.replay(seed, 0, 30, 4, 99, HeroClass.WARRIOR, 3, null);
			org.junit.Assert.fail("an unknown revision must be refused");
		} catch (FloorReplay.Refused e){
			assertTrue(e.getMessage().contains("balance"));
		}
	}

	@Test
	public void seedFloorsAreNeverReplayedWithAGameOpenBehindTheScreen() throws Exception {
		Class<?> was = com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene.backTo;
		try {
			com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene.backTo = com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class;
			assertNotNull(com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene.liveGameCheck().liveGame());
			com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene.backTo = com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene.class;
			assertNull(com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene.liveGameCheck().liveGame());
		} finally {
			com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene.backTo = (Class) was;
		}
	}

	//---- telemetry 1.7, facts, UIMSD ----

	private File root(){
		return tmp.getRoot();
	}

	private void log( String id, TelemetryVersion v, int difficulty, Ruleset rules ) throws Exception {
		DemoLog l = new DemoLog(root(), id, v, 1_700_000_000_000L);
		l.difficulty = difficulty;
		l.difficultyBalance = difficulty == 0 ? 0 : Difficulty.balanceRevision(difficulty);
		l.start(id, 0, "WARRIOR", rules.id, 0, 30, false).enter(1).tick(100).heroDies("rat", 0).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 10).close(true);
	}

	private Row find( Dataset runs, String idPart ){
		for (Row r : runs.rows) if (r.runId.contains(idPart)) return r;
		return null;
	}

	@Test
	public void newRunsRecordDifficultyLegacyRunsStayUnknownAndUimsdCanFilterAndGroup() throws Exception {
		TelemetryVersion v17 = new TelemetryVersion(1, 7), v15 = new TelemetryVersion(1, 5);
		File demo = new File(root(), "x");
		//(the DemoLog writer marks itself demo data: use demo- ids so the store accepts the folder as a demo folder)
		new File(root(), com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore.DEMO_MARKER_FILE).createNewFile();
		log("demo-d3", v17, 3, Ruleset.of(30, 0, 3));
		log("demo-d4", v17, 4, Ruleset.of(30, 0, 4));
		log("demo-d5", v17, 5, Ruleset.of(30, 0, 5));
		log("demo-old", v15, 0, Ruleset.of(30, 0, 3));
		Catalog cat = new DatasetStore(root()).load();
		Dataset runs = cat.dataset(Datasets.RUNS);
		int dc = runs.def.indexOf(FactKeys.DIFFICULTY), bc = runs.def.indexOf(FactKeys.DIFFICULTY_BALANCE);
		assertTrue(dc >= 0 && bc >= 0);
		assertEquals(4, runs.rows.size());
		Row d3 = find(runs, "demo-d3"), d4 = find(runs, "demo-d4"), old = find(runs, "demo-old");
		assertEquals(3, d3.num(dc));
		assertEquals(4, d4.num(dc));
		assertEquals("difficulty 4 was played with balance revision 1", 1, d4.num(bc));
		assertEquals("difficulty 3 is always revision 0", 0, d3.num(bc));
		assertFalse("a run recorded before 1.7 has no difficulty: unknown, never 3", old.isKnown(dc));
		//the table says what each is
		Query q = Query.of(Datasets.RUNS);
		assertEquals("3 - Standard", CellFormatter.cell(q, d3, runs.def.column(FactKeys.DIFFICULTY)).text);
		assertEquals("?", CellFormatter.cell(q, old, runs.def.column(FactKeys.DIFFICULTY)).text);
		assertEquals(CellText.UNKNOWN, CellFormatter.cell(q, old, runs.def.column(FactKeys.DIFFICULTY)).state);
		//runs of different difficulty are different rulesets: the default scope never mixes them
		String r3 = Ruleset.of(30, 0, 3).id;
		assertEquals(r3, d3.ruleset);
		assertNotEquals(r3, d4.ruleset);

		Scope all = Scope.DEFAULT.withRules(Scope.Rules.ALL, "");
		QueryResult only4 = QueryEngine.run(runs, q.withScope(all).addFilter(Filter.num(FactKeys.DIFFICULTY, Op.EQ, 4)), r3);
		assertEquals(1, only4.rows.size());
		QueryResult only3 = QueryEngine.run(runs, q.withScope(all).addFilter(Filter.num(FactKeys.DIFFICULTY, Op.EQ, 3)), r3);
		assertEquals("legacy runs are not in 'difficulty = 3'", 1, only3.rows.size());
		QueryResult unknown = QueryEngine.run(runs, q.withScope(all).addFilter(Filter.unknown(FactKeys.DIFFICULTY)), r3);
		assertEquals(1, unknown.rows.size());
		assertEquals(old.runId, unknown.rows.get(0).runId);
		assertEquals("in the current rules only the difficulty-3 runs are in scope", 2, QueryEngine.run(runs, q, r3).rows.size());

		//the ruleset list names the difficulty: runs of the standard difficulty share their ruleset with older runs that never recorded one
		String label3 = null, label4 = null;
		for (Catalog.RulesetInfo ri : cat.rulesets){
			if (ri.id.equals(r3)) label3 = ri.label;
			if (ri.id.equals(d4.ruleset)) label4 = ri.label;
		}
		assertNotNull(label3);
		assertTrue(label3, label3.contains("difficulty 3") && label3.contains("older runs: not recorded"));
		assertTrue(String.valueOf(label4), label4 != null && label4.contains("difficulty 4") && !label4.contains("not recorded"));
		assertEquals("a hero class is shown as a word, not as a key", "Warrior", CellFormatter.cell(q, d3, runs.def.column(FactKeys.HERO_CLASS)).text);

		ViewDef view = cat.view("difficulty");
		assertNotNull(view);
		QueryResult grouped = QueryEngine.run(runs, view.initialQuery().withMinSample(0), r3);
		assertEquals("one group per difficulty plus the unknown group", 4, grouped.groups.size());
		//the smallest-group rule still applies by default (groups of fewer runs are counted as hidden, not dropped)
		assertTrue(QueryEngine.run(runs, view.initialQuery(), r3).stats.hiddenSmallGroups > 0);
	}

	@Test
	public void redactedCopiesAndOlderStartsStillReadTheirDifficultyCorrectly() throws Exception {
		//a start event written by an older build has no difficulty fields: decoding gives 0 (not recorded)
		LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
		s.difficulty = 4;
		s.difficultyBalance = 0;
		assertEquals(4, s.difficulty);
		assertEquals(0, new LifecycleEvents.RunStart().difficulty);
	}

	//---- difficulty 3 changes nothing ----

	//the progression items the game only ever places on purpose (never drawn at random); stones and the catalyst can also be random draws
	private static final java.util.Set<String> GUARANTEED = new java.util.HashSet<>(java.util.Arrays.asList(
			"items.potions.PotionOfStrength", "items.scrolls.ScrollOfUpgrade", "items.Stylus"));

	//what one generated floor looks like for the checks below
	private static final class FloorLook {
		final java.util.List<String> mobs = new ArrayList<>();      //class@pos, in a fixed order
		final java.util.List<Integer> mobHt = new ArrayList<>();
		final java.util.List<Boolean> scalable = new ArrayList<>();
		final java.util.Map<String, Integer> guaranteed = new java.util.TreeMap<>();
		int items;
		String hash;
	}

	private List<FloorLook> looks( long seed, int level, int depth ) throws Exception {
		SPDSettings.apdDifficulty(level);
		final List<FloorLook> out = new ArrayList<>();
		LevelGenerationHarness.levelObserver = new java.util.function.BiConsumer<Integer, Level>(){
			@Override
			public void accept( Integer d, Level l ){
				FloorLook f = new FloorLook();
				java.util.List<com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob> ms = new ArrayList<>(l.mobs);
				java.util.Collections.sort(ms, new java.util.Comparator<com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob>(){
					@Override
					public int compare( com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob a, com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob b ){
						return a.pos != b.pos ? Integer.compare(a.pos, b.pos) : a.getClass().getName().compareTo(b.getClass().getName());
					}
				});
				for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : ms){
					//mimics are made while the level's items are placed, so how many there are follows the item count (loot), not the spawns
					if (m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic) continue;
					f.mobs.add(m.getClass().getName() + "@" + m.pos);
					f.mobHt.add(m.HT);
				}
				for (com.shatteredpixel.shatteredpixeldungeon.items.Heap h : l.heaps.valueList()){
					for (com.shatteredpixel.shatteredpixeldungeon.items.Item i : h.items){
						f.items++;
						String n = i.getClass().getName().replace("com.shatteredpixel.shatteredpixeldungeon.", "");
						if (GUARANTEED.contains(n)) f.guaranteed.put(n, (f.guaranteed.containsKey(n) ? f.guaranteed.get(n) : 0) + 1);
					}
				}
				out.add(f);
			}
		};
		try {
			List<LevelGenerationHarness.LevelPrint> prints = LevelGenerationHarness.generate(seed, 0, HeroClass.WARRIOR, LevelGenerationHarness.Context.REFERENCE, depth);
			for (int i = 0; i < prints.size(); i++) out.get(i).hash = prints.get(i).hash;
		} finally {
			LevelGenerationHarness.levelObserver = null;
		}
		return out;
	}

	@Test
	public void levelThreeBuildsTheFloorsOfBeforeAndOtherLevelsChangeOnlyHealthAndOrdinaryLoot() throws Exception {
		int hpChanged = 0, comparable = 0;
		long[] seeds = { 2468L, 13579L, 777L, 31337L };
		long itemsLow = 0, itemsMid = 0, itemsHigh = 0;
		for (long seed : seeds){
			List<FloorLook> base = looks(seed, 3, 26);
			for (int level : new int[]{ 1, 2, 4, 5 }){
				List<FloorLook> got = looks(seed, level, 26);
				assertEquals(base.size(), got.size());
				for (int d = 0; d < base.size(); d++){
					String where = "seed " + seed + " level " + level + " floor " + (d + 1);
					//the guaranteed items (progression) are the same on every floor, whatever the loot setting
					assertEquals(where, base.get(d).guaranteed, got.get(d).guaranteed);
					//Floor 1 starts from nothing, so it has the same creatures in the same places: difficulty never adds, removes or moves a spawn.
					//Later floors can differ in layout, because the item decks drawn from on earlier floors hold different cards when fewer or more
					//items were drawn; where the creatures do match, their health is compared.
					if (d == 0) assertEquals(where, base.get(d).mobs, got.get(d).mobs);
					if (!base.get(d).mobs.equals(got.get(d).mobs)) continue;
					comparable++;
					for (int m = 0; m < base.get(d).mobHt.size(); m++){
						int b = base.get(d).mobHt.get(m), g = got.get(d).mobHt.get(m);
						int scaled = Difficulty.scale(Difficulty.modifier(level, Difficulty.Modifier.ENEMY_HP), b);
						//an ordinary creature is scaled; a boss, mimic, statue or quest giver (not made by createMob) keeps its health
						assertTrue(where + " mob " + m + ": " + b + " -> " + g, g == b || g == Math.max(1, scaled));
						if (g != b) hpChanged++;
					}
				}
				long total = 0;
				for (FloorLook f : got) total += f.items;
				if (level == 1) itemsLow += total;
				if (level == 5) itemsHigh += total;
			}
			for (FloorLook f : base) itemsMid += f.items;
		}
		assertTrue("floors with matching creatures: " + comparable, comparable > 40);
		assertTrue("health must actually change somewhere", hpChanged > 100);
		//more loot at the easy end, less at the hard end (ordinary items only, so the difference is a share of the total)
		assertTrue("level 1 " + itemsLow + " vs level 3 " + itemsMid, itemsLow > itemsMid * 1.02);
		assertTrue("level 5 " + itemsHigh + " vs level 3 " + itemsMid, itemsHigh < itemsMid * 0.98);
	}

	@Test
	public void itemTotalsFallAsDifficultyRises() throws Exception {
		long[] total = new long[6];
		for (long seed : new long[]{ 11L, 22L, 33L, 44L, 55L, 66L }){
			for (int level = 1; level <= 5; level++){
				for (FloorLook f : looks(seed, level, 20)) total[level] += f.items;
			}
		}
		for (int level = 1; level < 5; level++) assertTrue("level " + level + " (" + total[level] + ") must have more items than level " + (level + 1) + " (" + total[level + 1] + ")", total[level] > total[level + 1]);
	}

	@Test
	public void theGameSeamsAreIdentityAtThreeAndScaleEnemiesOnlyWhenAModifierIsSet() throws Exception {
		Rat mob = new Rat();
		int hp = mob.HP, ht = mob.HT;
		level(3);
		DifficultyHooks.onMobCreated(mob);
		assertEquals(hp, mob.HP);
		assertEquals(ht, mob.HT);
		assertEquals(Float.floatToIntBits(7.3f), Float.floatToIntBits(DifficultyHooks.attackDamage(mob, 7.3f)));

		Field f = Difficulty.class.getDeclaredField("TABLE");
		f.setAccessible(true);
		double[][] table = (double[][]) f.get(null);
		double hpWas = table[3][Difficulty.Modifier.ENEMY_HP.ordinal()], dmgWas = table[3][Difficulty.Modifier.ENEMY_DAMAGE.ordinal()];
		try {
			table[3][Difficulty.Modifier.ENEMY_HP.ordinal()] = 2.0;
			table[3][Difficulty.Modifier.ENEMY_DAMAGE.ordinal()] = 1.5;
			level(4);
			Rat scaled = new Rat();
			DifficultyHooks.onMobCreated(scaled);
			assertEquals(ht * 2, scaled.HT);
			assertEquals(hp * 2, scaled.HP);
			assertEquals(15f, DifficultyHooks.attackDamage(mob, 10f), 0f);
			//only hostile creatures are scaled
			assertEquals(10f, DifficultyHooks.attackDamage(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero == null ? new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero() : Dungeon.hero, 10f), 0f);
			Rat ally = new Rat();
			ally.alignment = Char.Alignment.ALLY;
			assertEquals(10f, DifficultyHooks.attackDamage(ally, 10f), 0f);
			//at 3 the same table values change nothing
			level(3);
			assertEquals(10f, DifficultyHooks.attackDamage(mob, 10f), 0f);
		} finally {
			table[3][Difficulty.Modifier.ENEMY_HP.ordinal()] = hpWas;
			table[3][Difficulty.Modifier.ENEMY_DAMAGE.ordinal()] = dmgWas;
		}
	}

	@Test
	public void theSeamsAreCalledFromTheGameAtOnePlaceEach() throws Exception {
		String base = "src/main/java/com/shatteredpixel/shatteredpixeldungeon/";
		File level = new File(base + "levels/Level.java"), ch = new File(base + "actors/Char.java");
		if (!level.isFile()) return;
		assertTrue(new String(Files.readAllBytes(level.toPath()), "UTF-8").contains("DifficultyHooks.onMobCreated(m)"));
		assertTrue(new String(Files.readAllBytes(ch.toPath()), "UTF-8").contains("DifficultyHooks.attackDamage(this, dmg)"));
		File regular = new File(base + "levels/RegularLevel.java");
		assertTrue(new String(Files.readAllBytes(regular.toPath()), "UTF-8").contains("DifficultyHooks.ordinaryItemCount(nItems)"));
		//and difficulty is read through Difficulty only: gameplay code never touches Dungeon.difficulty directly
		int direct = 0;
		for (String f : new String[]{ "actors/mobs/Mob.java", "levels/RegularLevel.java", "items/Generator.java", "actors/buffs/Hunger.java" }){
			File file = new File(base + f);
			if (file.isFile() && new String(Files.readAllBytes(file.toPath()), "UTF-8").contains("Dungeon.difficulty")) direct++;
		}
		assertEquals(0, direct);
	}

	@Test
	public void aFloorReplayUsesTheRunsDifficultyNotTheInstalls() throws Exception {
		SPDSettings.apdDifficulty(2);
		level(5);
		final int[] seen = { -1 };
		FloorReplay.replay(77L, 0, 0, 4, HeroClass.WARRIOR, 2, new FloorReplay.Observer(){
			@Override
			public void level( int depth, Level level ){
				seen[0] = Difficulty.level();
			}
		});
		assertEquals(4, seen[0]);
		assertEquals("outside the replay the run's own difficulty is back", 5, Difficulty.level());
	}

	@Test
	public void theSettingsScreenOffersTheSelectorForNewRuns() throws Exception {
		File pane = new File("src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/APDSettingsPane.java");
		if (!pane.isFile()) return;
		String text = new String(Files.readAllBytes(pane.toPath()), "UTF-8");
		assertTrue(text.contains("optDifficulty"));
		assertTrue(text.contains("SPDSettings.apdDifficulty("));
		assertTrue("the screen says it only affects new runs", text.contains("Applies to runs you start from now on"));
		//no money anywhere near it
		assertFalse(text.toLowerCase().contains("purchase") || text.toLowerCase().contains("buy"));
	}
}
