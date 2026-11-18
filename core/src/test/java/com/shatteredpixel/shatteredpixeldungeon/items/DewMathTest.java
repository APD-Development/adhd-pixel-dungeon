package com.shatteredpixel.shatteredpixeldungeon.items;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;

import org.junit.Test;

/** Dew heals 5% of max HP per drop, rounded together; the Shielding Dew cap is 20% per talent rank. */
public class DewMathTest {
	@Test public void fivePercentPerDropRoundedTogether(){
		HeadlessTestSupport.start();
		Hero h = new Hero();
		h.HT = 30;
		assertEquals(2, Dewdrop.baseDewEffect(h, 1)); // 1.5 rounds to 2
		assertEquals(3, Dewdrop.baseDewEffect(h, 2));
		assertEquals(8, Dewdrop.baseDewEffect(h, 5)); // 7.5 rounds to 8, not 5 x 2 = 10
		assertEquals(0, Dewdrop.baseDewEffect(h, 0));
	}

	@Test public void noShieldingDewWithoutTheTalent(){
		HeadlessTestSupport.start();
		Hero h = new Hero();
		h.HT = 40;
		assertEquals(0, Dewdrop.shieldingDewCap(h));
	}
}
