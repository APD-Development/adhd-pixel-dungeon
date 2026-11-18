package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;

import org.junit.Test;

/** Standard melee scaling for weapons that keep the default formula: tier + level to 5(tier+1) + level(tier+1). */
public class StandardMeleeScalingTest {
	@Test public void swordFollowsTheDefaultFormula(){
		HeadlessTestSupport.start();
		Sword s = new Sword();
		int t = s.tier;
		for (int lvl = 0; lvl <= 5; lvl++){
			assertEquals(t + lvl, s.min(lvl));
			assertEquals(5 * (t + 1) + lvl * (t + 1), s.max(lvl));
			assertTrue(s.min(lvl) <= s.max(lvl));
		}
	}

	@Test public void weaponStrengthDropsLikeArmor(){
		HeadlessTestSupport.start();
		Sword s = new Sword();
		assertEquals(8 + 2 * s.tier, s.STRReq(0));
		assertEquals(8 + 2 * s.tier - 1, s.STRReq(1));
		assertEquals(8 + 2 * s.tier - 2, s.STRReq(3));
		assertEquals(s.STRReq(0), s.STRReq(-2));
	}
}
