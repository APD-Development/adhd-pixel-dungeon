package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

//Landmarks the journal notes (including every shown level feeling) and keys picked up or used are recorded when they happen;
//repeating one that is already there is not a change.
public class JournalRecordingTest {

	@Before
	@After
	public void cleanUp(){
		ApdRecorder.reset();
	}

	@Test
	public void landmarksAndKeysAreRecordedWhenTheJournalChanges() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(43L);
		String id = ApdRecorder.currentRunId();
		Notes.add(Notes.Landmark.SHOP, 7);
		Notes.add(Notes.Landmark.SHOP, 7);              //already noted: not a change
		Notes.remove(Notes.Landmark.SHOP, 7);
		Notes.remove(Notes.Landmark.SHOP, 7);           //already gone
		Notes.add(new IronKey(7));
		Notes.remove(new IronKey(7));
		ApdRecorder.flushForReading();
		ReadResult r = s.read(id);
		List<FactEvents.Observation> got = new ArrayList<>();
		for (ApdEvent e : r.events){
			if (!FactEvents.isFactEvent(e.type)) continue;
			for (FactEvents.Observation o : FactEvents.decode(e, r)) if (o.key.startsWith("journal.")) got.add(o);
		}
		assertEquals(4, got.size());
		assertEquals("journal.landmark", got.get(0).key);
		assertEquals("SHOP", got.get(0).subject);
		assertArrayEquals(new long[]{ 7 }, got.get(0).values);
		assertArrayEquals(new String[]{ "added" }, got.get(0).tags);
		assertArrayEquals(new String[]{ "removed" }, got.get(1).tags);
		assertEquals("journal.key", got.get(2).key);
		assertEquals("items.keys.IronKey", got.get(2).subject);
		assertArrayEquals(new long[]{ 7, 1 }, got.get(2).values);
		assertArrayEquals(new String[]{ "added" }, got.get(2).tags);
		assertArrayEquals(new String[]{ "used" }, got.get(3).tags);
	}

	@Test
	public void everyShownLevelFeelingIsRecordedOnceAtItsFloor() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(46L);
		String id = ApdRecorder.currentRunId();
		Notes.Landmark[] feelings = new Notes.Landmark[]{
				Notes.Landmark.CHASM_FLOOR, Notes.Landmark.WATER_FLOOR, Notes.Landmark.GRASS_FLOOR,
				Notes.Landmark.DARK_FLOOR, Notes.Landmark.LARGE_FLOOR, Notes.Landmark.TRAPS_FLOOR,
				Notes.Landmark.SECRETS_FLOOR
		};
		for (Notes.Landmark feeling : feelings){
			Notes.add(feeling, 8);
			Notes.add(feeling, 8);                     //already noted: not another shown feeling
		}
		ApdRecorder.flushForReading();
		ReadResult r = s.read(id);
		List<FactEvents.Observation> got = new ArrayList<>();
		for (ApdEvent e : r.events){
			if (!FactEvents.isFactEvent(e.type)) continue;
			for (FactEvents.Observation o : FactEvents.decode(e, r)){
				if (o.key.equals("journal.landmark") && o.subject.endsWith("_FLOOR")) got.add(o);
			}
		}
		assertEquals(feelings.length, got.size());
		for (int i = 0; i < feelings.length; i++){
			assertEquals(feelings[i].name(), got.get(i).subject);
			assertArrayEquals(new long[]{ 8 }, got.get(i).values);
			assertArrayEquals(new String[]{ "added" }, got.get(i).tags);
		}
	}

	@Test
	public void gameSceneShowsAndNotesEveryNonNeutralLevelFeeling() throws Exception {
		File root = new File(System.getProperty("apd.game.src"));
		String source = new String(Files.readAllBytes(new File(root, "scenes/GameScene.java").toPath()), "UTF-8");
		int start = source.indexOf("switch (Dungeon.level.feeling)");
		int end = source.indexOf("for (Mob mob : Dungeon.level.mobs)", start);
		assertTrue("level-feeling display switch must remain in GameScene", start >= 0 && end > start);
		String shown = source.substring(start, end);
		assertEquals("all seven non-neutral feelings must be shown", 7, count(shown, "GLog.w(Dungeon.level.feeling.desc());"));
		String[][] mappings = new String[][]{{"CHASM", "CHASM_FLOOR"}, {"WATER", "WATER_FLOOR"},
				{"GRASS", "GRASS_FLOOR"}, {"DARK", "DARK_FLOOR"}, {"LARGE", "LARGE_FLOOR"},
				{"TRAPS", "TRAPS_FLOOR"}, {"SECRETS", "SECRETS_FLOOR"}};
		for (String[] mapping : mappings){
			assertTrue(mapping[0] + " display branch is missing", shown.contains("case " + mapping[0] + ":"));
			assertEquals(mapping[0] + " must add its matching journal landmark exactly once", 1,
					count(shown, "Notes.add(Notes.Landmark." + mapping[1] + ");"));
		}
	}

	private static int count( String text, String needle ){
		int count = 0, at = 0;
		while ((at = text.indexOf(needle, at)) >= 0){ count++; at += needle.length(); }
		return count;
	}

	@Test
	public void revealedSecretsAreRecordedWithWhatTheyWere() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(44L);
		String id = ApdRecorder.currentRunId();
		int door = ItemTestSupport.freeCell(), trap = ItemTestSupport.freeCell(), plain = ItemTestSupport.freeCell();
		Dungeon.level.map[door] = Terrain.SECRET_DOOR;
		Dungeon.level.map[trap] = Terrain.SECRET_TRAP;
		Dungeon.level.discover(door);
		Dungeon.level.discover(trap);
		Dungeon.level.discover(plain);                  //nothing hidden there: nothing to record
		ApdRecorder.flushForReading();
		ReadResult r = s.read(id);
		List<FactEvents.Observation> got = new ArrayList<>();
		for (ApdEvent e : r.events){
			if (!FactEvents.isFactEvent(e.type)) continue;
			for (FactEvents.Observation o : FactEvents.decode(e, r)) if (o.key.equals("explore.discover")) got.add(o);
		}
		assertEquals(2, got.size());
		assertEquals("Secret door", got.get(0).subject);
		assertArrayEquals(new long[]{ door }, got.get(0).values);
		assertEquals("Secret trap", got.get(1).subject);
	}

	@Test
	public void journalPagesFoundAndReadForTheFirstTimeAreRecorded() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(45L);
		String id = ApdRecorder.currentRunId();
		Document doc = Document.ADVENTURERS_GUIDE;
		String page = doc.pageNames().iterator().next();
		doc.deletePage(page);                          //back to "not found" (debug builds start with every page read)
		assertEquals(true, doc.findPage(page));
		doc.findPage(page);                            //already found: not a change
		doc.readPage(page);
		doc.readPage(page);                            //already read: not a change
		ApdRecorder.flushForReading();
		ReadResult r = s.read(id);
		List<FactEvents.Observation> got = new ArrayList<>();
		for (ApdEvent e : r.events){
			if (!FactEvents.isFactEvent(e.type)) continue;
			for (FactEvents.Observation o : FactEvents.decode(e, r)) if (o.key.equals("journal.page")) got.add(o);
		}
		assertEquals(2, got.size());
		assertArrayEquals(new String[]{ "found", page }, got.get(0).tags);
		assertArrayEquals(new String[]{ "read", page }, got.get(1).tags);
		assertEquals("ADVENTURERS_GUIDE", got.get(0).subject);
	}
}
