package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AllySummonRecordingTest {

	private static final class NamedAlly extends Char {
		private final String shownName;

		NamedAlly( String shownName ){
			this.shownName = shownName;
			alignment = Alignment.ALLY;
			HP = HT = 10;
		}

		@Override
		public String name(){
			return shownName;
		}
	}

	@Before
	@After
	public void cleanUp(){
		ApdRecorder.reset();
		while (ApdPlayRestoring.isRestoring()) ApdPlay.restoreEnd();
	}

	private static List<FactEvents.Observation> summons( ReadResult result ){
		List<FactEvents.Observation> out = new ArrayList<>();
		for (ApdEvent event : result.events){
			if (!FactEvents.isFactEvent(event.type)) continue;
			for (FactEvents.Observation observation : FactEvents.decode(event, result)){
				if (observation.key.equals("ally.summon")) out.add(observation);
			}
		}
		return out;
	}

	@Test
	public void directAndVisibleChainedPlayerSummonsRecordActualCreatorExactlyOnce() throws Exception {
		RecorderTestSupport.Session session = ItemTestSupport.startRun(199L);
		String runId = ApdRecorder.currentRunId();
		Dungeon.depth = 7;
		Hero hero = Dungeon.hero;
		new QuietSprite(hero);

		NamedAlly hawk = new NamedAlly("Visible hawk");
		hawk.apdInheritOffensiveOrigin(hero);
		hawk.pos = hero.pos;
		if (Dungeon.level.heroFOV == null) Dungeon.level.heroFOV = new boolean[Dungeon.level.length()];
		Dungeon.level.heroFOV[hawk.pos] = true;

		NamedAlly echo = new NamedAlly("Echo ally");
		echo.apdInheritOffensiveOrigin(hawk);
		echo.apdInheritOffensiveOrigin(hawk);

		ApdRecorder.flushForReading();
		List<FactEvents.Observation> got = summons(session.read(runId));
		assertEquals(2, got.size());
		assertEquals("Visible hawk", got.get(0).subject);
		assertEquals(CombatTracker.nameOf(hero), got.get(0).actor);
		assertArrayEquals(new long[]{ 7 }, got.get(0).values);
		assertEquals("Echo ally", got.get(1).subject);
		assertEquals("Visible hawk", got.get(1).actor);
		assertArrayEquals(new long[]{ 7 }, got.get(1).values);
	}

	@Test
	public void hiddenNonPlayerAndRestorePathsEmitNoPublicSummonFact() throws Exception {
		RecorderTestSupport.Session session = ItemTestSupport.startRun(200L);
		String runId = ApdRecorder.currentRunId();
		Hero hero = Dungeon.hero;

		NamedAlly hiddenCreator = new NamedAlly("Hidden creator");
		ApdPlay.restoreBegin();
		try {
			hiddenCreator.apdInheritOffensiveOrigin(hero);
			new NamedAlly("Restored ally").apdInheritOffensiveOrigin(hero);
		} finally {
			ApdPlay.restoreEnd();
		}
		hiddenCreator.pos = hero.pos;
		if (Dungeon.level.heroFOV == null) Dungeon.level.heroFOV = new boolean[Dungeon.level.length()];
		Dungeon.level.heroFOV[hiddenCreator.pos] = false;
		new NamedAlly("Hidden child").apdInheritOffensiveOrigin(hiddenCreator);
		new NamedAlly("Enemy child").apdInheritOffensiveOrigin(new Rat());
		new NamedAlly("Boss child").apdInheritOffensiveOrigin(new Goo());
		new NamedAlly("Unknown child").apdInheritOffensiveOrigin(null);

		ApdRecorder.flushForReading();
		assertEquals(0, summons(session.read(runId)).size());
	}

	//ApdPlay deliberately keeps its restoration depth private; this probe only prevents a failed test from leaking it.
	private static final class ApdPlayRestoring {
		static boolean isRestoring(){
			try {
				java.lang.reflect.Field field = ApdPlay.class.getDeclaredField("restoring");
				field.setAccessible(true);
				return field.getInt(null) > 0;
			} catch (Throwable ignored){
				return false;
			}
		}
	}
}
