package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

/** Scheduled Nov 18: the status tab's "Hungry in N turns" uses the same rate the hunger buff ticks at. */
public class HungerTurnsUntilHungryTest {
	private Hero oldHero;
	private boolean oldIntro;
	private com.shatteredpixel.shatteredpixeldungeon.levels.Level oldLevel;

	@Before public void setUp(){
		HeadlessTestSupport.start();
		oldHero = Dungeon.hero;
		oldLevel = Dungeon.level;
		oldIntro = com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro();
		com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro(false);
		Dungeon.level = null;
		Dungeon.hero = new Hero();
	}

	@After public void tearDown(){
		Dungeon.hero = oldHero;
		Dungeon.level = oldLevel;
		com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro(oldIntro);
	}

	private static Hunger at(float level) throws Exception {
		Hunger h = new Hunger();
		Field f = Hunger.class.getDeclaredField("level");
		f.setAccessible(true);
		f.setFloat(h, level);
		return h;
	}

	@Test public void countsDownToHungryAtTheCurrentRate() throws Exception {
		float rate = Hunger.perTurn();
		assertTrue("hunger rises by default", rate > 0);
		assertEquals((int)Math.ceil(300 / rate), at(0).turnsUntilHungry());
		assertEquals((int)Math.ceil(120 / rate), at(180).turnsUntilHungry());
		assertEquals((int)Math.ceil(0.5f / rate), at(299.5f).turnsUntilHungry());
	}

	@Test public void zeroOnceHungryOrStarving() throws Exception {
		assertEquals(0, at(Hunger.HUNGRY).turnsUntilHungry());
		assertEquals(0, at(400).turnsUntilHungry());
		Hunger starving = at(Hunger.STARVING);
		assertEquals(0, starving.turnsUntilHungry());
		assertTrue(starving.isStarving());
		assertFalse(at(Hunger.HUNGRY).isStarving());
	}

	@Test public void hungerValueIsReportedAsAWholeNumber() throws Exception {
		assertEquals("rounds up, like the hunger buff", 181, at(180.4f).hunger());
		assertEquals(0, at(0).hunger());
	}

	@Test public void pausedDuringTheTutorialAndBossArenas() throws Exception {
		com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro(true);
		assertEquals(-1, at(100).turnsUntilHungry());
		com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro(false);
		com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel arena = new com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel();
		arena.locked = true;
		Dungeon.level = arena;
		assertEquals(-1, at(100).turnsUntilHungry());
	}

	@Test public void wellFedTurnsAreAdded() throws Exception {
		Hero hero = new Hero();
		hero.HP = hero.HT = 20;
		Dungeon.hero = hero;
		Hunger h = Buff.affect(hero, Hunger.class);
		Field f = Hunger.class.getDeclaredField("level");
		f.setAccessible(true);
		f.setFloat(h, 200);
		int plain = h.turnsUntilHungry();
		WellFed fed = Buff.affect(hero, WellFed.class);
		fed.reset();
		assertEquals(plain + fed.remainingNutritionUpdates(), h.turnsUntilHungry());
	}
}
