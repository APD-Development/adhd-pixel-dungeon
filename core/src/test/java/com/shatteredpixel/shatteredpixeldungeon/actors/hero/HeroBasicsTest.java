package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.Bundle;

import org.junit.Test;

/** A fresh hero's starting numbers, the experience curve, and that core stats survive a save. */
public class HeroBasicsTest {
	@Test public void freshHeroStartsAtLevelOneWithTenStrength(){
		HeadlessTestSupport.start();
		Hero h = new Hero();
		assertEquals(1, h.lvl);
		assertEquals(Hero.STARTING_STR, h.STR);
		assertEquals(10, Hero.STARTING_STR);
		assertNotNull(h.belongings);
	}

	@Test public void experienceNeededGrowsByFivePerLevel(){
		assertEquals(10, Hero.maxExp(1));
		assertEquals(15, Hero.maxExp(2));
		assertEquals(5 + 29 * 5, Hero.maxExp(29));
		for (int l = 1; l < Hero.MAX_LEVEL; l++) assertEquals(5, Hero.maxExp(l + 1) - Hero.maxExp(l));
	}

	@Test public void levelStrengthHpAndExpSurviveASave(){
		HeadlessTestSupport.start();
		Hero h = new Hero();
		h.heroClass = HeroClass.WARRIOR;
		Talent.initClassTalents(h);
		h.lvl = 7; h.STR = 13; h.HT = 50; h.HP = 31; h.exp = 4;
		Bundle b = new Bundle();
		h.storeInBundle(b);
		Hero back = new Hero();
		back.heroClass = HeroClass.WARRIOR;
		Talent.initClassTalents(back);
		back.restoreFromBundle(b);
		assertEquals(7, back.lvl);
		assertEquals(13, back.STR);
		assertEquals(31, back.HP);
		assertEquals(4, back.exp);
	}
}
