package com.shatteredpixel.shatteredpixeldungeon.items.bags;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;

import org.junit.Test;

/** Bags hold 20 different things; stacks merge without using a slot; pouches only take what they are for. */
public class BagCapacityTest {
	@Test public void aFullBagRefusesNewItemsButStillStacks(){
		HeadlessTestSupport.start();
		Dungeon.hero = null;
		Bag bag = new Bag(){};
		bag.items.add(new Food());
		for (int i = 0; i < 19; i++) bag.items.add(new Sword());
		assertEquals(20, bag.items.size());
		assertFalse("no room for another different item", bag.canHold(new Sword()));
		Food more = new Food();
		assertTrue("a matching stack still merges", more.collect(bag));
		assertEquals(20, bag.items.size());
		assertEquals(2, bag.items.get(0).quantity());
	}

	@Test public void velvetPouchTakesSeedsNotSwords(){
		HeadlessTestSupport.start();
		VelvetPouch pouch = new VelvetPouch();
		assertTrue(pouch.canHold(new Sungrass.Seed()));
		assertFalse(pouch.canHold(new Sword()));
	}
}
