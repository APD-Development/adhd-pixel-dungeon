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

public class MovementCommandRecordingTest {

	@Before
	@After
	public void cleanUp(){
		ApdRecorder.reset();
	}

	private static List<FactEvents.Observation> commands( ReadResult result ){
		List<FactEvents.Observation> out = new ArrayList<>();
		for (ApdEvent event : result.events){
			if (!FactEvents.isFactEvent(event.type)) continue;
			for (FactEvents.Observation observation : FactEvents.decode(event, result)){
				if (observation.key.equals("move.command")) out.add(observation);
			}
		}
		return out;
	}

	@Test
	public void recordsPublicStepTravelAndStairCommands() throws Exception {
		RecorderTestSupport.Session session = ItemTestSupport.startRun(204L);
		String runId = ApdRecorder.currentRunId();
		int width = Dungeon.level.width();

		ApdPlay.onMoveCommand(7, "step");
		ApdPlay.onMoveCommand(19, "travel");
		ApdPlay.onMoveCommand(31, "stairs");

		ApdRecorder.flushForReading();
		List<FactEvents.Observation> got = commands(session.read(runId));
		assertEquals(3, got.size());
		assertArrayEquals(new long[]{ 7, width }, got.get(0).values);
		assertArrayEquals(new String[]{ "step" }, got.get(0).tags);
		assertArrayEquals(new long[]{ 19, width }, got.get(1).values);
		assertArrayEquals(new String[]{ "travel" }, got.get(1).tags);
		assertArrayEquals(new long[]{ 31, width }, got.get(2).values);
		assertArrayEquals(new String[]{ "stairs" }, got.get(2).tags);
	}
}
