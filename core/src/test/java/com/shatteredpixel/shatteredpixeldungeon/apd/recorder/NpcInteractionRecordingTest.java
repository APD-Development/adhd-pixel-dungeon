package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

//NPC interaction recording lives at Hero's central interaction point so every accepted visible NPC interaction is captured once,
//independently of the separate quest-step events, while a rejected interaction is not invented.
public class NpcInteractionRecordingTest {

	@Before
	@After
	public void cleanUp(){
		ApdRecorder.reset();
	}

	private static NPC npc( final String shownName, final boolean accepted ){
		return new NPC(){
			@Override
			public String name(){
				return shownName;
			}

			@Override
			public boolean interact( Char c ){
				return accepted;
			}
		};
	}

	private static boolean interact( Hero hero, NPC npc ) throws Exception {
		Method method = Hero.class.getDeclaredMethod("interactAndRecord", Char.class);
		method.setAccessible(true);
		return (Boolean) method.invoke(hero, npc);
	}

	@Test
	public void centralHeroInteractionRecordsAcceptedNpcOnceAndRejectsNoEvent() throws Exception {
		RecorderTestSupport.Session session = ItemTestSupport.startRun(196L);
		String runId = ApdRecorder.currentRunId();
		Hero hero = Dungeon.hero;
		new QuietSprite(hero);

		assertTrue(interact(hero, npc("Visible guide", true)));
		assertFalse(interact(hero, npc("Silent prop", false)));

		ApdRecorder.flushForReading();
		ReadResult result = session.read(runId);
		List<FactEvents.Observation> interactions = new ArrayList<>();
		for (ApdEvent event : result.events){
			if (!FactEvents.isFactEvent(event.type)) continue;
			for (FactEvents.Observation observation : FactEvents.decode(event, result)){
				if (observation.key.equals("npc.interact")) interactions.add(observation);
			}
		}

		assertEquals(1, interactions.size());
		assertEquals("Visible guide", interactions.get(0).subject);
		assertArrayEquals(new long[]{ 1 }, interactions.get(0).values);
		assertEquals(0, interactions.get(0).tags.length);
	}
}
