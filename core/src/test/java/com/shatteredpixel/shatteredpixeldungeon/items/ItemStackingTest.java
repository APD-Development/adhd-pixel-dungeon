package com.shatteredpixel.shatteredpixeldungeon.items;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;

import org.junit.Before;
import org.junit.Test;

/** Stacks merge only with the same kind, split without losing anything, and levels move one step at a time. */
public class ItemStackingTest {
	@Before public void setUp(){ HeadlessTestSupport.start(); }

	@Test public void mergeOnlyCombinesSimilarItems(){
		Food a = new Food(); a.quantity(2);
		Food b = new Food(); b.quantity(3);
		a.merge(b);
		assertEquals(5, a.quantity());
		assertEquals(0, b.quantity());
		Pasty p = new Pasty(); p.quantity(1);
		a.merge(p);
		assertEquals("different food does not merge", 5, a.quantity());
		assertEquals(1, p.quantity());
	}

	@Test public void splitKeepsTheTotalAndRefusesBadAmounts(){
		Food a = new Food(); a.quantity(5);
		Item piece = a.split(2);
		assertNotNull(piece);
		assertTrue(piece instanceof Food);
		assertEquals(2, piece.quantity());
		assertEquals(3, a.quantity());
		assertNull(a.split(0));
		assertNull(a.split(3));
		assertNull(a.split(-1));
		assertEquals(3, a.quantity());
	}

	@Test public void upgradeAndDegradeStepByOne(){
		Sword s = new Sword();
		s.upgrade(3);
		assertEquals(3, s.level());
		s.degrade();
		assertEquals(2, s.level());
		s.degrade(4);
		assertEquals(-2, s.level());
	}
}
