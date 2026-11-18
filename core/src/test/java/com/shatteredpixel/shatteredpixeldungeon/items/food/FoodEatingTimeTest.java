package com.shatteredpixel.shatteredpixeldungeon.items.food;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Eating takes 3 turns, or 1 with a meal talent; the preview is unknown without a hero. */
public class FoodEatingTimeTest {
	private Hero oldHero;

	@Before public void setUp(){ HeadlessTestSupport.start(); oldHero = Dungeon.hero; }
	@After public void tearDown(){ Dungeon.hero = oldHero; }

	@Test public void noHeroMeansNoPreview(){
		Dungeon.hero = null;
		assertTrue(Float.isNaN(new Food().previewEatingTime()));
	}

	@Test public void threeTurnsNormallyOneWithAMealTalent(){
		Hero h = new Hero();
		h.heroClass = HeroClass.WARRIOR;
		Talent.initClassTalents(h);
		Dungeon.hero = h;
		assertEquals(Food.TIME_TO_EAT, new Food().previewEatingTime(), 0f);
		h.upgradeTalent(Talent.IRON_STOMACH);
		assertEquals(Food.TIME_TO_EAT - 2, new Food().previewEatingTime(), 0f);
	}
}
