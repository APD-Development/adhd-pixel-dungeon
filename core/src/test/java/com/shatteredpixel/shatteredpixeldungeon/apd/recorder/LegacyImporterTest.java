package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Rankings;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.DerivedStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactTable;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts;
import com.watabou.utils.Bundle;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//The importer reads the rankings bundle exactly as SPD wrote it. The bundles here come from the game's own
//Rankings.saveGameData and Statistics.storeInBundle, so a renamed key in the game breaks these tests.
public class LegacyImporterTest {

	@After
	public void cleanUp(){
		ApdRecorder.reset();
		Rankings.INSTANCE.records = null;
		Rankings.INSTANCE.lastRecord = 0;
		Rankings.INSTANCE.latestDaily = null;
	}

	private static File freshRoot(){
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		RecorderTestSupport.newRun(s, 5L);
		return s.root;
	}

	private static Rankings.Record record( String gameId, boolean win, int score ){
		Statistics.deepestFloor = 6;
		Statistics.highestAscent = 0;
		Statistics.enemiesSlain = 41;
		Statistics.goldCollected = 700;
		Statistics.foodEaten = 3;
		Statistics.itemsCrafted = 2;
		Statistics.upgradesUsed = 5;
		Statistics.duration = 1234.5f;
		Statistics.chalMultiplier = 1.25f;
		Statistics.winMultiplier = 1f;
		Statistics.totalScore = score;
		Dungeon.hero.lvl = 9;
		Dungeon.hero.STR = 13;
		Rankings.Record rec = new Rankings.Record();
		rec.cause = Rat.class;
		rec.win = win;
		rec.heroClass = Dungeon.hero.heroClass;
		rec.armorTier = 2;
		rec.herolevel = 9;
		rec.depth = 6;
		rec.score = score;
		rec.customSeed = "";
		rec.daily = false;
		rec.date = "2025-03-04";
		rec.version = "v3.1.1";
		rec.gameID = gameId;
		Rankings.INSTANCE.saveGameData(rec);
		return rec;
	}

	private static Bundle rankings( int total, int won, Rankings.Record... recs ){
		Bundle b = new Bundle();
		List<Rankings.Record> list = new ArrayList<>();
		for (Rankings.Record r : recs) list.add(r);
		b.put("records", list);
		b.put("latest", 0);
		b.put("total", total);
		b.put("won", won);
		return b;
	}

	private static RunFacts only( FactTable t ){
		assertEquals(1, t.legacyRows.size());
		return t.legacyRows.get(0);
	}

	@Test
	public void aRecordInTheGamesOwnFormatIsImportedFieldByField() throws Exception {
		File root = freshRoot();
		Bundle b = rankings(40, 3, record("game-1", false, 4321));
		LegacyImporter.Report r = LegacyImporter.importFrom(b, root, 1000L);
		assertEquals(r.summary(), 1, r.imported);
		assertTrue(r.fieldsKnown > 25);

		RunFacts f = only(new DerivedStore(root).build());
		assertEquals("legacy:game-1", f.id);
		assertEquals(4321, f.numOr(FactKeys.SCORE, -1));
		assertEquals(LifecycleEvents.OUTCOME_DIED, f.numOr(FactKeys.OUTCOME, -1));
		assertEquals("WARRIOR", f.textOr(FactKeys.HERO_CLASS, ""));
		assertEquals("Rat", f.textOr(FactKeys.CAUSE, ""));
		assertEquals(9, f.numOr(FactKeys.HERO_LEVEL, -1));
		assertEquals(6, f.numOr(FactKeys.DEEPEST, -1));
		assertEquals("2025-03-04", f.textOr(FactKeys.DATE, ""));
		assertEquals("v3.1.1", f.textOr(FactKeys.GAME_VERSION, ""));
		assertEquals(5L, f.numOr(FactKeys.SEED, -1));
		assertEquals(41, f.numOr("legacy.enemiesSlain", -1));
		assertEquals(700, f.numOr("legacy.goldCollected", -1));
		assertEquals(2, f.numOr("legacy.itemsCrafted", -1));
		assertEquals(1234500, f.numOr("legacy.durationMilli", -1));
		assertEquals(125, f.numOr("legacy.chalMultiplierPct", -1));
		assertEquals(13, f.numOr(FactKeys.FINAL_STRENGTH, -1));
		assertEquals(9, f.numOr(FactKeys.FINAL_HERO_LEVEL, -1));
		assertTrue("the warrior's weapon is part of the final loadout", f.isKnown("loadout.weapon"));
		assertTrue(f.textOr("loadout.weapon", "").length() > 0);
		assertTrue(f.isKnown("loadout.weapon.level"));
		assertEquals(Fact.PROV_LEGACY, f.get(FactKeys.SCORE).prov);
	}

	@Test
	public void importingAgainImportsNothingAndWritesNoFile() throws Exception {
		File root = freshRoot();
		Bundle b = rankings(40, 3, record("game-1", false, 4321), record("game-2", true, 99999));
		assertEquals(2, LegacyImporter.importFrom(b, root, 1000L).imported);
		File dir = new File(root, "legacy");
		assertEquals(1, dir.listFiles().length);

		LegacyImporter.Report again = LegacyImporter.importFrom(b, root, 2000L);
		assertEquals(0, again.imported);
		assertEquals(2, again.alreadyKnown);
		assertFalse(again.totalsImported);
		assertEquals("nothing new, so no new file", 1, dir.listFiles().length);
		assertTrue(again.summary(), again.summary().startsWith("Nothing new"));
		assertEquals(2, new DerivedStore(root).build().legacyRows.size());
	}

	@Test
	public void aNewRecordLaterIsTheOnlyThingAdded() throws Exception {
		File root = freshRoot();
		LegacyImporter.importFrom(rankings(40, 3, record("game-1", false, 4321)), root, 1000L);
		LegacyImporter.Report r = LegacyImporter.importFrom(rankings(41, 3, record("game-1", false, 4321), record("game-3", true, 7777)), root, 2000L);
		assertEquals(1, r.imported);
		assertTrue("the played counter changed", r.totalsImported);
		FactTable t = new DerivedStore(root).build();
		assertEquals(2, t.legacyRows.size());
		assertEquals(41, t.legacyTotals.numOr("legacy.totalNumber", -1));
	}

	@Test
	public void fieldsAnOldRecordLacksStayUnknownNotZero() throws Exception {
		File root = freshRoot();
		Rankings.Record rec = record("game-old", true, 5000);
		Bundle one = new Bundle();
		rec.storeInBundle(one);
		//an old record: no date or version (SPD only added them later), no id, none of the newer statistics
		one.remove("date");
		one.remove("version");
		one.remove("gameID");
		Bundle data = one.getBundle("gameData");
		data.remove("seed");
		data.remove("game_version");
		Bundle stats = data.getBundle("stats");
		stats.remove("potionsCooked");
		stats.remove("duration");
		stats.remove("hazard_assists");
		stats.remove("item_types_discovered");
		stats.remove("tot_quest");
		one.remove("gameData");
		data.remove("stats");
		data.put("stats", stats);
		one.put("gameData", data);
		Bundle b = new Bundle();
		b.put("total", 10);
		b.put("won", 0);
		//wrap the raw record the way the file does
		java.util.Collection<com.watabou.utils.Bundlable> none = new ArrayList<>();
		b.put("records", none);
		Bundle file = rawFile(10, 0, one);

		LegacyImporter.Report r = LegacyImporter.importFrom(file, root, 1000L);
		assertEquals(r.summary(), 1, r.imported);
		RunFacts f = only(new DerivedStore(root).build());
		assertTrue(f.id.startsWith("legacy:content-"));
		for (String key : new String[]{FactKeys.DATE, FactKeys.GAME_VERSION, FactKeys.SEED, FactKeys.INITIAL_VERSION,
				"legacy.itemsCrafted", "legacy.durationMilli", "legacy.hazardAssistedKills", "legacy.itemTypesDiscovered", "legacy.questScore"}){
			assertFalse(key + " was not in the old record, so it is unknown and not zero", f.isKnown(key));
		}
		assertTrue("what it did contain is still imported", f.isKnown("legacy.enemiesSlain"));
		assertEquals(5000, f.numOr(FactKeys.SCORE, -1));

		//and the stand-in id is stable: importing the same old record again finds it
		LegacyImporter.Report again = LegacyImporter.importFrom(rawFile(10, 0, one), root, 2000L);
		assertEquals(0, again.imported);
		assertEquals(1, again.alreadyKnown);
	}

	@Test
	public void anEmptyRecordImportsAsAllUnknown() throws Exception {
		File root = freshRoot();
		Bundle file = rawFile(0, 0, new Bundle());
		LegacyImporter.importFrom(file, root, 1000L);
		RunFacts f = only(new DerivedStore(root).build());
		assertFalse(f.isKnown(FactKeys.OUTCOME));
		assertFalse(f.isKnown(FactKeys.SCORE));
		assertFalse(f.isKnown(FactKeys.HERO_CLASS));
		assertFalse(f.isKnown("loadout.weapon"));
	}

	//a rankings file with raw record bundles (the way a file written by an older game is read)
	private static Bundle rawFile( int total, int won, Bundle... records ){
		//build the JSON the file holds: records is an array of record objects
		StringBuilder json = new StringBuilder("{\"records\":[");
		for (int i = 0; i < records.length; i++){
			if (i > 0) json.append(',');
			json.append(records[i].toString());
		}
		json.append("],\"latest\":0,\"total\":").append(total).append(",\"won\":").append(won).append("}");
		try {
			java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(json.toString().getBytes("UTF-8"));
			return Bundle.read(in);
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	@Test
	public void totalsThatSpdCannotDistinguishFromNothingStayUnknown() throws Exception {
		File root = freshRoot();
		LegacyImporter.importFrom(rankings(0, 0, record("g", false, 10)), root, 1000L);
		FactTable t = new DerivedStore(root).build();
		assertNull("a total of 0 means 'not stored' to SPD itself, so no total is claimed", t.legacyTotals);
		assertEquals(1, t.legacyRows.size());

		File root2 = freshRoot();
		LegacyImporter.importFrom(rankings(7, 0, record("g", false, 10)), root2, 1000L);
		FactTable t2 = new DerivedStore(root2).build();
		assertEquals(7, t2.legacyTotals.numOr("legacy.totalNumber", -1));
		assertEquals("0 wins out of 7 games is real", 0, t2.legacyTotals.numOr("legacy.wonNumber", -1));
	}

	@Test
	public void dailyScoresAreImportedOnce() throws Exception {
		File root = freshRoot();
		Bundle b = rankings(5, 1, record("g", false, 10));
		b.put("daily_history_dates", new long[]{20250101L, 20250102L});
		b.put("daily_history_scores", new int[]{300, 450});
		assertEquals(2, LegacyImporter.importFrom(b, root, 1000L).dailyScoresImported);
		assertEquals(0, LegacyImporter.importFrom(b, root, 2000L).dailyScoresImported);
		assertEquals(2, new DerivedStore(root).build().dailyScores.size());
	}

	@Test
	public void importingNeverChangesTheRankingsDataOrTheGame() throws Exception {
		File root = freshRoot();
		Bundle b = rankings(40, 3, record("game-1", false, 4321));
		String before = b.toString();
		java.util.ArrayList<Rankings.Record> held = new java.util.ArrayList<>();
		Rankings.INSTANCE.records = held;
		int hp = Dungeon.hero.HP, depth = Dungeon.depth;
		LegacyImporter.importFrom(b, root, 1000L);
		assertEquals("the rankings bundle is only read", before, b.toString());
		assertTrue("the game's rankings table is untouched", Rankings.INSTANCE.records == held && held.isEmpty());
		assertEquals(hp, Dungeon.hero.HP);
		assertEquals(depth, Dungeon.depth);
	}

	@Test
	public void noRankingsFileMeansNothingHappens() throws Exception {
		File root = freshRoot();
		LegacyImporter.Report r = LegacyImporter.importFrom(null, root, 1000L);
		assertFalse(r.fileFound);
		assertEquals(0, r.imported);
		assertFalse(new File(root, "legacy").exists());
		assertTrue(r.summary(), r.summary().startsWith("No APD rankings"));
	}

	@Test
	public void theImporterNeverThrowsOnGarbage() throws Exception {
		File root = freshRoot();
		Bundle garbage = new Bundle();
		garbage.put("records", "not an array");
		LegacyImporter.Report r = LegacyImporter.importFrom(garbage, root, 1000L);
		assertNotNull(r.summary());
		Bundle weird = rawFile(3, 1, new Bundle());
		assertNotNull(LegacyImporter.importFrom(weird, new File("/proc/definitely/not/writable"), 1000L).summary());
	}

	@Test
	public void aFinishedRunRemembersWhichRankingRecordItProduced() throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		RecorderTestSupport.newRun(s, 21L);
		RecorderTestSupport.enterLevel(1);
		String id = ApdRecorder.currentRunId();

		//what Rankings.submit has just done: put this run's record in the table
		Rankings.Record rec = new Rankings.Record();
		rec.gameID = "ranking-77";
		rec.win = false;
		rec.score = 555;
		rec.customSeed = "";
		Statistics.totalScore = 555;
		Rankings.INSTANCE.records = new ArrayList<>();
		Rankings.INSTANCE.records.add(rec);
		Rankings.INSTANCE.lastRecord = 0;
		ApdRecorder.onRunEnded(false, Rat.class);

		ReadResult r = s.read(id);
		List<com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent> links = RecorderTestSupport.ofType(r, EventTypes.RANKING_LINK);
		assertEquals(1, links.size());
		assertEquals("ranking-77", LegacyEvents.decodeRankingLink(links.get(0), r).gameId);
		//the link is recorded after the end and before the seal, and the facts pick it up
		FactTable t = new DerivedStore(s.root).build();
		assertEquals("ranking-77", t.apdRows.get(0).textOr(FactKeys.RANKING_ID, ""));
	}

	@Test
	public void aRecordThatIsNotThisRunsIsNotLinked() throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		RecorderTestSupport.newRun(s, 22L);
		RecorderTestSupport.enterLevel(1);
		String id = ApdRecorder.currentRunId();
		Rankings.Record rec = new Rankings.Record();
		rec.gameID = "someone-else";
		rec.win = false;
		rec.score = 1;
		rec.customSeed = "";
		Statistics.totalScore = 999;     //different score: not the record this run just made
		Rankings.INSTANCE.records = new ArrayList<>();
		Rankings.INSTANCE.records.add(rec);
		Rankings.INSTANCE.lastRecord = 0;
		ApdRecorder.onRunEnded(false, Rat.class);
		ReadResult r = s.read(id);
		assertTrue(RecorderTestSupport.ofType(r, EventTypes.RANKING_LINK).isEmpty());
		assertEquals(1, RecorderTestSupport.ofType(r, EventTypes.RUN_END).size());
	}

	@Test
	public void aRunEndedWithNoRankingsTableStillEndsCleanly() throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		RecorderTestSupport.newRun(s, 23L);
		RecorderTestSupport.enterLevel(1);
		String id = ApdRecorder.currentRunId();
		Rankings.INSTANCE.records = null;
		ApdRecorder.onRunEnded(false, Rat.class);
		ReadResult r = s.read(id);
		assertTrue(r.sealed);
		assertTrue(RecorderTestSupport.ofType(r, EventTypes.RANKING_LINK).isEmpty());
		assertEquals(0, ApdRecorder.status().errorCount);
	}
}
