package com.shatteredpixel.shatteredpixeldungeon.items.rings;

import static org.junit.Assert.*;

import org.junit.Test;

/** The per-level ring multipliers: neutral at 0, compound per level, and cursed levels go the other way. */
public class RingMultipliersTest {
	@Test public void zeroBonusIsNeutral(){
		assertEquals(1f, RingOfHaste.speedMultiplier(0), 1e-6f);
		assertEquals(1f, RingOfFuror.attackSpeedMultiplier(0), 1e-6f);
		assertEquals(1f, RingOfEvasion.evasionMultiplier(0), 1e-6f);
		assertEquals(1f, RingOfAccuracy.accuracyMultiplier(0), 1e-6f);
	}

	@Test public void eachLevelCompounds(){
		assertEquals(1.15f, RingOfHaste.speedMultiplier(1), 1e-5f);
		assertEquals(1.15f * 1.15f, RingOfHaste.speedMultiplier(2), 1e-4f);
		assertEquals(1.125f, RingOfEvasion.evasionMultiplier(1), 1e-5f);
		assertEquals(1.3f * 1.3f * 1.3f, RingOfAccuracy.accuracyMultiplier(3), 1e-3f);
	}

	@Test public void negativeLevelsWeaken(){
		assertTrue(RingOfHaste.speedMultiplier(-1) < 1f);
		assertEquals(1f, RingOfHaste.speedMultiplier(-2) * RingOfHaste.speedMultiplier(2), 1e-4f);
		assertTrue(RingOfFuror.attackSpeedMultiplier(-3) < 1f);
	}
}
