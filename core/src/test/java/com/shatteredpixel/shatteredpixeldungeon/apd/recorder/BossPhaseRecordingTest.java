package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BossPhaseRecordingTest {

	@Before
	@After
	public void cleanUp(){
		ApdRecorder.reset();
	}

	private static List<FactEvents.Observation> phases( ReadResult result ){
		List<FactEvents.Observation> out = new ArrayList<>();
		for (ApdEvent event : result.events){
			if (!FactEvents.isFactEvent(event.type)) continue;
			for (FactEvents.Observation observation : FactEvents.decode(event, result)){
				if (observation.key.equals("boss.phase")) out.add(observation);
			}
		}
		return out;
	}

	@Test
	public void recordsOnlyDisplayedBossAndVisiblePhaseDetails() throws Exception {
		RecorderTestSupport.Session session = ItemTestSupport.startRun(205L);
		String runId = ApdRecorder.currentRunId();
		Dungeon.depth = 20;

		ApdPlay.onBossPhase("Goo", 2, "enraged");
		ApdPlay.onBossPhase("Tengu", 2, "arena");
		ApdPlay.onBossPhase("DM-300", 4, "supercharged");
		ApdPlay.onBossPhase("Dwarf King", 3, "final");
		ApdPlay.onBossPhase("Yog-Dzewa", 5, "final");

		ApdRecorder.flushForReading();
		List<FactEvents.Observation> got = phases(session.read(runId));
		assertEquals(5, got.size());
		String[] bosses = { "Goo", "Tengu", "DM-300", "Dwarf King", "Yog-Dzewa" };
		int[] numbers = { 2, 2, 4, 3, 5 };
		String[] states = { "enraged", "arena", "supercharged", "final", "final" };
		for (int i = 0; i < got.size(); i++){
			assertEquals(bosses[i], got.get(i).subject);
			assertEquals("", got.get(i).actor);
			assertArrayEquals(new long[]{ numbers[i], 20 }, got.get(i).values);
			assertArrayEquals(new String[]{ states[i] }, got.get(i).tags);
		}
	}

	@Test
	public void invalidInitialAndRestoreTimeStatesRecordNothing() throws Exception {
		RecorderTestSupport.Session session = ItemTestSupport.startRun(206L);
		String runId = ApdRecorder.currentRunId();

		ApdPlay.onBossPhase(null, 2, "hidden");
		ApdPlay.onBossPhase("", 2, "hidden");
		ApdPlay.onBossPhase("Goo", 1, "initial");
		ApdPlay.restoreBegin();
		try {
			ApdPlay.onBossPhase("Dwarf King", 2, "shielded");
		} finally {
			ApdPlay.restoreEnd();
		}

		ApdRecorder.flushForReading();
		assertEquals(0, phases(session.read(runId)).size());
	}
}
