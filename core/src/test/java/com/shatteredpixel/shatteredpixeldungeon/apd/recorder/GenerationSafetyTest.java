package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Random;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//Recording generation must leave the game exactly as it was: the level, the item generator's decks, the guaranteed-drop
//counters, the ids the game hands out and the random numbers.
public class GenerationSafetyTest {

	@After
	public void cleanUp(){
		ApdRecorder.reset();
	}

	//builds three levels (ordinary level flow) and reports everything that could differ
	private static List<String> build( boolean record, boolean failing ) throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		com.shatteredpixel.shatteredpixeldungeon.actors.Actor.resetNextID();
		ApdRecorder.configure(s.root, record);
		RecorderTestSupport.newRun(s, 31337L);
		if (failing) ApdRecorder.failureInjection = new RuntimeException("injected");
		List<String> out = new ArrayList<>();
		try {
			for (int depth = 1; depth <= 3; depth++){
				RecorderTestSupport.enterLevel(depth);
				Level level = Dungeon.level;
				//(the level's full content is compared by the golden and recorder-on golden tests; which mob gets which id is
				// the game's own unseeded order, so only the set of ids handed out is compared here)
				List<String> mobs = new ArrayList<>();
				List<Integer> ids = new ArrayList<>();
				for (Mob m : level.mobs){ mobs.add(m.getClass().getSimpleName() + "@" + m.pos); ids.add(m.apdPeekId()); }
				Collections.sort(mobs);
				Collections.sort(ids);
				out.add("mobs " + mobs + " ids " + ids);
				out.add("items " + level.heaps.valueList().size() + " " + level.mobs.size() + " " + level.map.length);
				StringBuilder decks = new StringBuilder();
				for (Generator.Category c : Generator.Category.values()) decks.append(c.name()).append('=').append(c.dropped).append(' ');
				out.add("decks " + decks);
				StringBuilder lim = new StringBuilder();
				for (Dungeon.LimitedDrops d : Dungeon.LimitedDrops.values()) lim.append(d.name()).append('=').append(d.count).append(' ');
				out.add("limited " + lim);
			}
			//the game's own random numbers carry on from the same place
			Random.pushGenerator(555L);
			out.add("next " + Random.Int() + "," + Random.Float());
			Random.popGenerator();
			out.add("unseeded-stream-position-free");
		} finally {
			ApdRecorder.reset();
		}
		return out;
	}

	@Test
	public void recordingChangesNothingAboutTheLevelsTheDecksTheCountersTheIdsOrTheRandomNumbers() throws Exception {
		List<String> off = build(false, false);
		List<String> on = build(true, false);
		assertEquals(off, on);
		assertTrue(off.size() > 10);
	}

	@Test
	public void aFailingRecorderChangesNothingEither() throws Exception {
		List<String> off = build(false, false);
		List<String> failing = build(true, true);
		assertEquals(off, failing);
	}

	@Test
	public void repeatedFailuresSwitchGenerationRecordingOffAndTheGameCarriesOn() throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		RecorderTestSupport.newRun(s, 4L);
		ApdRecorder.failureInjection = new RuntimeException("injected");
		for (int depth = 1; depth <= 4; depth++){
			RecorderTestSupport.enterLevel(depth);
			assertTrue(Dungeon.level != null);
		}
		assertTrue(ApdRecorder.status().sessionDisabled);
		assertFalse(ApdGen.active);
	}

	@Test
	public void theHooksNeverThrowOnOddInput() throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		RecorderTestSupport.newRun(s, 5L);
		ApdGen.end(-1, ApdGen.KIND_RANDOM, null, null);
		ApdGen.end(0, ApdGen.KIND_RANDOM, null, null);
		ApdGen.onLevelGenerated(null);
		ApdGen.onLootCreated(null, null);
		ApdGen.onBonusDrop(null, -1);
		ApdGen.setStage(99);
		assertEquals(0, ApdRecorder.status().errorCount);
	}

	@Test
	public void generatingLevelsIsCheapWithRecordingOn() throws Exception {
		double off = millisPerLevel(false);
		double on = millisPerLevel(true);
		System.out.println("APD-PERF generation: " + String.format("%.1f", off) + " ms/level without recording, "
				+ String.format("%.1f", on) + " ms/level with recording");
		assertTrue("recording must not add more than 50 ms to a level, added " + (on - off), on - off < 50);
	}

	private static double millisPerLevel( boolean record ) throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		ApdRecorder.configure(s.root, record);
		RecorderTestSupport.newRun(s, 777L);
		int levels = 0;
		long t0 = System.nanoTime();
		for (int depth = 1; depth <= 10; depth++){
			Dungeon.depth = depth;
			Dungeon.branch = 0;
			Dungeon.level = Dungeon.newLevel();
			levels++;
		}
		long t1 = System.nanoTime();
		if (record){
			ApdRecorder.flushForTests();
			long bytes = s.log(ApdRecorder.currentRunId()).length();
			System.out.println("APD-PERF generation log: " + bytes + " bytes for " + levels + " levels (" + (bytes / levels) + " B/level)");
		}
		ApdRecorder.reset();
		return (t1 - t0) / 1e6 / levels;
	}
}
