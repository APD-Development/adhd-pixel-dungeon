package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Wand charges: one more per upgrade, capped at 10, and an upgrade gives one charge back. */
public class WandChargesTest {
	private Hero oldHero;

	@Before public void setUp(){ HeadlessTestSupport.start(); oldHero = Dungeon.hero; Dungeon.hero = new Hero(); }
	@After public void tearDown(){ Dungeon.hero = oldHero; }

	@Test public void maxChargesGrowOnePerLevelUpToTen(){
		WandOfMagicMissile w = new WandOfMagicMissile();
		int base = w.maxCharges;
		w.upgrade();
		assertEquals(base + 1, w.maxCharges);
		w.upgrade(20);
		assertEquals(10, w.maxCharges);
	}

	@Test public void upgradeRefillsOneChargeNeverAboveMax(){
		WandOfMagicMissile w = new WandOfMagicMissile();
		w.curCharges = 0;
		w.upgrade();
		assertEquals(1, w.curCharges);
		w.curCharges = w.maxCharges;
		w.upgrade();
		assertTrue(w.curCharges <= w.maxCharges);
	}

	@Test public void losingLevelsCapsCurrentCharges(){
		WandOfMagicMissile w = new WandOfMagicMissile();
		w.upgrade(4);
		w.curCharges = w.maxCharges;
		w.level(0);
		w.updateLevel();
		assertEquals(w.maxCharges, w.curCharges);
	}
}
