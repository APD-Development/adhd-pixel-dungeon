package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GameSnapshotTest {
	private Hero oldHero;
	private int oldDepth;

	@Before public void setUp(){ HeadlessTestSupport.start(); oldHero = Dungeon.hero; oldDepth = Dungeon.depth; }
	@After public void tearDown(){ Dungeon.hero = oldHero; Dungeon.depth = oldDepth; }

	@Test public void shortNamesDropOnlyTheGamePackage(){
		assertEquals("items.weapon.melee.Sword", GameSnapshot.shortName(Sword.class));
		assertEquals("java.lang.String", GameSnapshot.shortName(String.class));
	}

	@Test public void checkpointWithoutAHeroKeepsTheDefaults(){
		Dungeon.hero = null;
		Dungeon.depth = 6;
		LifecycleEvents.Checkpoint c = GameSnapshot.checkpoint(3);
		assertEquals(3, c.reason);
		assertEquals(6, c.depth);
		assertEquals("", c.heroClass);
		assertEquals(-1, c.hunger);
		assertTrue(c.items.isEmpty());
		assertTrue(GameSnapshot.turn() >= 0);
	}

	@Test public void checkpointRecordsHeroHungerAndGear(){
		Hero hero = new Hero();
		hero.heroClass = HeroClass.WARRIOR;
		hero.HP = 12; hero.HT = 20;
		hero.belongings.weapon = new Sword();
		VelvetPouch pouch = new VelvetPouch();
		hero.belongings.backpack.items.add(pouch);
		hero.belongings.backpack.items.add(new Food());
		Buff.affect(hero, Hunger.class);
		Dungeon.hero = hero;
		LifecycleEvents.Checkpoint c = GameSnapshot.checkpoint(1);
		assertEquals("WARRIOR", c.heroClass);
		assertEquals(12, c.hp);
		assertEquals(20, c.hpMax);
		assertEquals(0, c.hunger);
		boolean weapon = false, food = false;
		for (LifecycleEvents.ItemEntry e : c.items){
			if (e.slot == LifecycleEvents.SLOT_WEAPON && e.className.endsWith("Sword")) weapon = true;
			if (e.slot == LifecycleEvents.SLOT_BACKPACK && e.className.endsWith("Food")) food = true;
		}
		assertTrue(weapon);
		assertTrue(food);
	}
}
