package com.shatteredpixel.shatteredpixeldungeon.apd.trophy;

import static org.junit.Assert.*;

import org.junit.Test;

public class TrophiesTest {
	@Test public void normalEnemiesUse100_500_2000(){
		assertEquals(Trophies.NONE, Trophies.tierFor(99, false));
		assertEquals(Trophies.BRONZE, Trophies.tierFor(100, false));
		assertEquals(Trophies.BRONZE, Trophies.tierFor(499, false));
		assertEquals(Trophies.SILVER, Trophies.tierFor(500, false));
		assertEquals(Trophies.GOLD, Trophies.tierFor(2000, false));
		assertEquals(Trophies.GOLD, Trophies.tierFor(Integer.MAX_VALUE, false));
	}

	@Test public void bossesAndRaresUse5_25_100(){
		assertEquals(Trophies.NONE, Trophies.tierFor(4, true));
		assertEquals(Trophies.BRONZE, Trophies.tierFor(5, true));
		assertEquals(Trophies.SILVER, Trophies.tierFor(25, true));
		assertEquals(Trophies.GOLD, Trophies.tierFor(100, true));
	}

	@Test public void colorsAndNamesAreDistinctPerTier(){
		assertEquals(0, Trophies.color(Trophies.NONE));
		assertNotEquals(Trophies.color(Trophies.BRONZE), Trophies.color(Trophies.SILVER));
		assertNotEquals(Trophies.color(Trophies.SILVER), Trophies.color(Trophies.GOLD));
		assertEquals("Gold", Trophies.name(Trophies.GOLD));
		assertEquals(Trophies.NONE, Trophies.tier(null));
	}

	@Test public void thresholdsAreDefensiveCopies(){
		int[] t = Trophies.thresholds(Object.class);
		t[0] = 1;
		assertEquals(100, Trophies.thresholds(Object.class)[0]);
	}

	@Test public void bossesUseTheLowerThresholds(){
		com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport.start();
		assertTrue(Trophies.special(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo.class));
		assertFalse(Trophies.special(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat.class));
		assertEquals(5, Trophies.thresholds(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo.class)[0]);
		assertEquals(100, Trophies.thresholds(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat.class)[0]);
	}

	@Test public void bestiaryLineNamesTheTierAndTheNextOne(){
		com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport.start();
		String line = Trophies.describe(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat.class);
		assertTrue(line, line.startsWith("Trophy: "));
		assertTrue(line, line.contains(" at _") || line.contains("_Gold_"));
	}
}
