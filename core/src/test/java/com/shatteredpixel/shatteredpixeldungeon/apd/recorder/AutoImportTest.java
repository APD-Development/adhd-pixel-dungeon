package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Rankings;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.DerivedStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactTable;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//The import that runs by itself when a run ends: it must keep records that SPD is about to drop, never write one twice,
//never change rankings.dat, and never get in the way of finishing the run.
public class AutoImportTest {

	@After
	public void cleanUp(){
		ApdRecorder.reset();
		Rankings.INSTANCE.records = null;
		Rankings.INSTANCE.lastRecord = 0;
		FileUtils.getFileHandle(Rankings.RANKINGS_FILE).delete();
	}

	private static Rankings.Record record( String id, int score ){
		Statistics.totalScore = score;
		Rankings.Record rec = new Rankings.Record();
		rec.cause = Rat.class;
		rec.heroClass = Dungeon.hero.heroClass;
		rec.herolevel = 5;
		rec.depth = 3;
		rec.score = score;
		rec.customSeed = "";
		rec.date = "2025-01-01";
		rec.version = "v3.1.1";
		rec.gameID = id;
		Rankings.INSTANCE.saveGameData(rec);
		return rec;
	}

	private static void writeRankings( Rankings.Record... recs ) throws Exception {
		List<Rankings.Record> list = new ArrayList<>();
		for (Rankings.Record r : recs) list.add(r);
		Bundle b = new Bundle();
		b.put("records", list);
		b.put("latest", 0);
		b.put("total", 30);
		b.put("won", 1);
		FileUtils.bundleToFile(Rankings.RANKINGS_FILE, b);
	}

	private static byte[] rankingsBytes(){
		return FileUtils.getFileHandle(Rankings.RANKINGS_FILE).readBytes();
	}

	private static RecorderTestSupport.Session startedRun( long seed ){
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		RecorderTestSupport.newRun(s, seed);
		RecorderTestSupport.enterLevel(1);
		return s;
	}

	private static FactTable legacyTable( File root ){
		return new DerivedStore(root).buildLegacyOnly();
	}

	@Test
	public void finishingARunImportsTheRankingsByItself() throws Exception {
		RecorderTestSupport.Session s = startedRun(1L);
		writeRankings(record("a", 100), record("b", 200));
		byte[] before = rankingsBytes();
		String id = ApdRecorder.currentRunId();
		ApdRecorder.onRunEnded(false, Rat.class);
		ApdRecorder.awaitAutoImportForTests();
		assertEquals(2, legacyTable(s.root).legacyRows.size());
		assertArrayEquals("rankings.dat is only read", before, rankingsBytes());
		ReadResult r = s.read(id);
		assertTrue(r.sealed);
		assertEquals(1, RecorderTestSupport.ofType(r, EventTypes.RUN_END).size());
	}

	@Test
	public void aRecordSpdIsAboutToDropIsCopiedBeforeTheSubmit() throws Exception {
		RecorderTestSupport.Session s = startedRun(2L);
		writeRankings(record("old-1", 100), record("old-2", 200), record("old-3", 300));
		//Dungeon.fail calls this just before Rankings.submit, which then trims the table
		ApdRecorder.onBeforeRankingSubmit();
		ApdRecorder.awaitAutoImportForTests();
		assertEquals(3, legacyTable(s.root).legacyRows.size());

		//SPD drops the lowest and adds a new one
		writeRankings(record("old-2", 200), record("old-3", 300), record("new-4", 400));
		ApdRecorder.onRunEnded(false, Rat.class);
		ApdRecorder.awaitAutoImportForTests();
		FactTable t = legacyTable(s.root);
		assertEquals("the dropped record is kept, the new one is added, nothing is duplicated", 4, t.legacyRows.size());
		StringBuilder ids = new StringBuilder();
		for (com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts f : t.legacyRows) ids.append(f.id).append(' ');
		assertTrue(ids.toString(), ids.toString().contains("legacy:old-1") && ids.toString().contains("legacy:new-4"));
	}

	@Test
	public void theSameRankingsImportedAgainAndAgainAddsNothing() throws Exception {
		RecorderTestSupport.Session s = startedRun(3L);
		writeRankings(record("a", 100), record("b", 200));
		for (int i = 0; i < 3; i++){
			ApdRecorder.onBeforeRankingSubmit();
			ApdRecorder.awaitAutoImportForTests();
		}
		ApdRecorder.importApdRankings();
		assertEquals(2, legacyTable(s.root).legacyRows.size());
		assertEquals("one import file, the repeats wrote nothing", 1, new File(s.root, "legacy").listFiles().length);
	}

	@Test
	public void theButtonAndTheAutomaticImportRacingStillGiveEachRecordOnce() throws Exception {
		final RecorderTestSupport.Session s = startedRun(4L);
		writeRankings(record("a", 100), record("b", 200), record("c", 300));
		Thread[] threads = new Thread[4];
		for (int i = 0; i < threads.length; i++){
			threads[i] = new Thread(new Runnable(){
				@Override
				public void run(){
					ApdRecorder.importApdRankings();
				}
			});
		}
		ApdRecorder.onBeforeRankingSubmit();
		for (Thread t : threads) t.start();
		for (Thread t : threads) t.join();
		ApdRecorder.awaitAutoImportForTests();
		assertEquals(3, legacyTable(s.root).legacyRows.size());
	}

	@Test
	public void aBrokenRankingsFileNeverInterferesWithFinishingTheRun() throws Exception {
		RecorderTestSupport.Session s = startedRun(5L);
		FileUtils.getFileHandle(Rankings.RANKINGS_FILE).writeBytes(new byte[]{9, 9, 9, 9, 9, 9}, false);
		String id = ApdRecorder.currentRunId();
		ApdRecorder.onBeforeRankingSubmit();
		ApdRecorder.onRunEnded(false, Rat.class);
		ApdRecorder.awaitAutoImportForTests();
		assertEquals(0, ApdRecorder.status().errorCount);
		assertFalse(ApdRecorder.status().sessionDisabled);
		ReadResult r = s.read(id);
		assertTrue("the run still ended and its log was sealed", r.sealed);
		assertEquals(0, legacyTable(s.root).legacyRows.size());
	}

	@Test
	public void aHistoryFolderThatCannotBeWrittenNeverInterferesEither() throws Exception {
		RecorderTestSupport.Session s = startedRun(6L);
		writeRankings(record("a", 100));
		//the legacy folder's place is taken by a plain file, so no import file can be created
		assertTrue(new File(s.root, "legacy").createNewFile());
		String id = ApdRecorder.currentRunId();
		ApdRecorder.onRunEnded(false, Rat.class);
		ApdRecorder.awaitAutoImportForTests();
		assertEquals(0, ApdRecorder.status().errorCount);
		assertTrue(s.read(id).sealed);
	}

	@Test
	public void withRecordingOffNothingIsImported() throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		RecorderTestSupport.newRun(s, 7L);
		writeRankings(record("a", 100));
		ApdRecorder.configure(s.root, false);
		ApdRecorder.onBeforeRankingSubmit();
		ApdRecorder.onRunEnded(false, Rat.class);
		ApdRecorder.awaitAutoImportForTests();
		assertFalse(new File(s.root, "legacy").exists());
	}

	@Test
	public void noRankingsFileIsNotAnError() throws Exception {
		RecorderTestSupport.Session s = startedRun(8L);
		FileUtils.getFileHandle(Rankings.RANKINGS_FILE).delete();
		ApdRecorder.onBeforeRankingSubmit();
		ApdRecorder.onRunEnded(false, Rat.class);
		ApdRecorder.awaitAutoImportForTests();
		assertEquals(0, ApdRecorder.status().errorCount);
		assertFalse(new File(s.root, "legacy").exists());
	}

	@Test
	public void theHooksSitJustBeforeSubmitInBothEndings() throws Exception {
		String d = new String(java.nio.file.Files.readAllBytes(new File(System.getProperty("apd.game.src"), "Dungeon.java").toPath()), "UTF-8");
		for (String call : new String[]{"Rankings.INSTANCE.submit( false, cause );", "Rankings.INSTANCE.submit( true, cause );"}){
			int submit = d.indexOf(call);
			int hook = d.lastIndexOf("ApdRecorder.onBeforeRankingSubmit();", submit);
			assertTrue("the import must come right before " + call, submit > 0 && hook > 0 && submit - hook < 80);
		}
	}
}
