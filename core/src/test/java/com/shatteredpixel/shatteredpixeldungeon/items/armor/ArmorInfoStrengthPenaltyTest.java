package com.shatteredpixel.shatteredpixeldungeon.items.armor;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Scheduled Nov 18: the penalty line in the real armor description, without leaking unknown upgrades. */
public class ArmorInfoStrengthPenaltyTest {
	private Hero oldHero;

	@Before public void setUp(){
		HeadlessTestSupport.start();
		oldHero = Dungeon.hero;
		Hero hero = new Hero();
		hero.STR = 10;
		Dungeon.hero = hero;
	}

	@After public void tearDown(){ Dungeon.hero = oldHero; }

	private static PlateArmor plate(int level, boolean known){
		PlateArmor armor = new PlateArmor();
		armor.level(level);
		armor.levelKnown = known;
		armor.cursedKnown = known;
		return armor;
	}

	@Test public void identifiedHeavyArmorShowsItsExactPenalty(){
		PlateArmor armor = plate(3, true);
		int missing = armor.STRReq() - Dungeon.hero.STR();
		assertTrue("plate +3 is still too heavy at 10 strength", missing > 0);
		String info = armor.info();
		assertTrue(info, info.contains(Armor.strengthPenalty(missing)));
		assertFalse(info, info.contains("If it's unupgraded"));
	}

	@Test public void unidentifiedArmorShowsThePlusZeroCostAndNotItsRealLevel(){
		PlateArmor armor = plate(3, false);
		int missingAtZero = armor.STRReq(0) - Dungeon.hero.STR();
		int missingReal = armor.STRReq() - Dungeon.hero.STR();
		assertNotEquals("the test needs the upgrade to change the requirement", missingAtZero, missingReal);
		String info = armor.info();
		assertTrue(info, info.contains(Armor.strengthPenaltyUnupgraded(missingAtZero)));
		assertFalse("must not leak the real level", info.contains(Armor.strengthPenalty(missingReal)));
		assertFalse(info.contains("_" + missingReal + " strength_ short"));
	}

	@Test public void lightArmorHasNoPenaltyLine(){
		Dungeon.hero.STR = 20;
		assertFalse(plate(0, true).info().contains("strength_ short"));
		assertFalse(plate(0, false).info().contains("might need too much strength"));
	}
}
