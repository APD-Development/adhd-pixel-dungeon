package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import static org.junit.Assert.*;

import org.junit.Test;

/** Scheduled Nov 18: the top-right hunger number and the status-tab hunger line. */
public class HungerDisplayTest {
	@Test public void cornerShowsValueOutOf450OrNothing(){
		assertEquals("Hunger 180/450", HungerDisplay.corner(180));
		assertEquals("Hunger 0/450", HungerDisplay.corner(0));
		assertEquals("", HungerDisplay.corner(-1));
	}

	@Test public void colorTurnsYellowAt300AndRedAt450(){
		assertEquals(HungerDisplay.NORMAL_COLOR, HungerDisplay.color(299));
		assertEquals(HungerDisplay.HUNGRY_COLOR, HungerDisplay.color(300));
		assertEquals(HungerDisplay.HUNGRY_COLOR, HungerDisplay.color(449));
		assertEquals(HungerDisplay.STARVING_COLOR, HungerDisplay.color(450));
	}

	@Test public void statusLineCoversEveryState(){
		assertEquals("Hunger: _180/450_. Hungry in _120 turns_.", HungerDisplay.status(180, false, 120));
		assertEquals("Hunger: _299/450_. Hungry in _1 turn_.", HungerDisplay.status(299, false, 1));
		assertEquals("Hunger: _300/450_. You are _Hungry_.", HungerDisplay.status(300, false, 0));
		assertEquals("Hunger: _450/450_. You are _Starving_.", HungerDisplay.status(450, true, 0));
		assertEquals("Hunger: _10/450_. Your hunger isn't rising.", HungerDisplay.status(10, false, -1));
	}
}
