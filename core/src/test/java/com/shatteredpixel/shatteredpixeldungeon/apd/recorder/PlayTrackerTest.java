package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GameFacts;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//The parts of the play recorder that need no running game: gold changes are seen as they happen with their cause, an unchanged state is
//not an observation, and the played-time total only moves while the game is actually being played.
public class PlayTrackerTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private List<FactEvents.Observation> run( Body body ) throws Exception {
		File f = new File(tmp.getRoot(), "p.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "p", "test", 1);
		FactTracker facts = new FactTracker(w);
		int gold = Dungeon.gold;
		try {
			body.run(facts);
		} finally {
			facts.flushAll();
			Dungeon.gold = gold;
		}
		w.close(false, 5000);
		ReadResult r = RunLogReader.read(f);
		List<FactEvents.Observation> out = new ArrayList<>();
		//hero.state is checked in HeroStateRecordingTest; a hero left over from an earlier test would add one to every turn here
		for (ApdEvent e : r.events){
			if (!FactEvents.isFactEvent(e.type)) continue;
			for (FactEvents.Observation o : FactEvents.decode(e, r)) if (!o.key.equals("hero.state")) out.add(o);
		}
		return out;
	}

	private interface Body {
		void run( FactTracker facts ) throws Exception;
	}

	@Test
	public void everyGoldChangeIsRecordedWithItsCauseAndNothingElseIs() throws Exception {
		List<FactEvents.Observation> got = run(new Body(){
			public void run( FactTracker facts ){
				Dungeon.gold = 10;
				PlayTracker t = new PlayTracker(facts);
				Dungeon.gold = 25;
				t.pollGold("pickup");
				t.pollGold("pickup");         //nothing changed: not an observation
				Dungeon.gold = 5;
				t.pollGold("shop.buy");
				Dungeon.gold = 6;
				t.onHeroTurn();               //changed without a hook: seen at the next turn, cause "other"
			}
		});
		assertEquals(3, got.size());
		assertArrayEquals(new long[]{ 15, 25 }, got.get(0).values);
		assertArrayEquals(new String[]{ "pickup" }, got.get(0).tags);
		assertArrayEquals(new long[]{ -20, 5 }, got.get(1).values);
		assertArrayEquals(new String[]{ "shop.buy" }, got.get(1).tags);
		assertArrayEquals(new long[]{ 1, 6 }, got.get(2).values);
		assertArrayEquals(new String[]{ "other" }, got.get(2).tags);
		assertEquals("economy.gold", got.get(0).key);
	}

	@Test
	public void playedTimeIsRecordedAsARunningTotalThatCountsOnlyShortGaps() throws Exception {
		List<FactEvents.Observation> got = run(new Body(){
			public void run( FactTracker facts ) throws Exception {
				PlayTracker t = new PlayTracker(facts);
				t.recordTime();
				Thread.sleep(120);
				t.onHeroTurn();
				t.recordTime();
			}
		});
		assertEquals(2, got.size());
		assertEquals(GameFacts.TIME_ACTIVE.key, got.get(0).key);
		assertTrue(got.get(0).values[0] <= 5);
		assertTrue("about the time that passed, was " + got.get(1).values[0], got.get(1).values[0] >= 100 && got.get(1).values[0] < 5000);
		assertFalse(got.get(1).values[0] < got.get(0).values[0]);
	}

	@Test
	public void aLongIdleGapAddsNoActivePlayTime() throws Exception {
		List<FactEvents.Observation> got = run(new Body(){
			public void run( FactTracker facts ) throws Exception {
				PlayTracker t = new PlayTracker(facts);
				java.lang.reflect.Field last = PlayTracker.class.getDeclaredField("lastNano");
				last.setAccessible(true);
				last.setLong(t, System.nanoTime() - 31_000_000_000L);
				t.onHeroTurn();
				t.recordTime();
			}
		});
		assertEquals(1, got.size());
		assertEquals(GameFacts.TIME_ACTIVE.key, got.get(0).key);
		assertEquals(0, got.get(0).values[0]);
	}
}
