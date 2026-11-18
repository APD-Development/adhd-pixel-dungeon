package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

//Each quest giver's steps are recorded as they happen, by name and step only (nothing about what a quest will ask for before it is given).
public class QuestRecordingTest {

	@Before
	@After
	public void cleanUp(){
		ApdRecorder.reset();
	}

	@Test
	public void questStepsAreRecordedByGiverAndStep() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(46L);
		String id = ApdRecorder.currentRunId();
		ApdPlay.onQuest("Sad ghost", "given");
		Ghost.Quest.complete();
		Wandmaker.Quest.complete();
		new com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe().collect();   //the Blacksmith looks for the pickaxe he lent
		Blacksmith.Quest.complete();
		Imp.Quest.complete(5);
		ApdRecorder.flushForReading();
		ReadResult r = s.read(id);
		List<FactEvents.Observation> got = new ArrayList<>();
		for (ApdEvent e : r.events){
			if (!FactEvents.isFactEvent(e.type)) continue;
			for (FactEvents.Observation o : FactEvents.decode(e, r)) if (o.key.equals("npc.quest")) got.add(o);
		}
		assertEquals(5, got.size());
		String[] who = { "Sad ghost", "Sad ghost", "Wandmaker", "Blacksmith", "Ambitious imp" };
		String[] step = { "given", "done", "done", "done", "done" };
		for (int i = 0; i < 5; i++){
			assertEquals(who[i], got.get(i).subject);
			assertArrayEquals(new String[]{ step[i] }, got.get(i).tags);
		}
	}
}
