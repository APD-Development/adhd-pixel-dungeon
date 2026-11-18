/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */


package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;

//Reads the current game state into recorder data. READ ONLY: nothing here changes the game, calls anything with side
//effects, or touches random numbers.
final class GameSnapshot {

	private GameSnapshot(){}

	private static final String GAME_PACKAGE = "com.shatteredpixel.shatteredpixeldungeon.";
	private static final int MAX_BAG_DEPTH = 4;
	private static final int MAX_ITEMS = 400;

	//the game clock in whole turns
	static long turn(){
		return Math.max(0, (long) Statistics.duration);
	}

	static String shortName( Class<?> cls ){
		String n = cls.getName();
		return n.startsWith(GAME_PACKAGE) ? n.substring(GAME_PACKAGE.length()) : n;
	}

	static LifecycleEvents.Checkpoint checkpoint( int reason ){
		LifecycleEvents.Checkpoint c = new LifecycleEvents.Checkpoint();
		c.reason = reason;
		c.depth = Dungeon.depth;
		c.branch = Dungeon.branch;
		c.turn = turn();
		c.gold = Dungeon.gold;
		c.energy = Dungeon.energy;

		Hero hero = Dungeon.hero;
		if (hero == null) return c;

		c.heroClass = hero.heroClass == null ? "" : hero.heroClass.name();
		c.subClass = hero.subClass == null ? "" : hero.subClass.name();
		c.heroLevel = hero.lvl;
		c.exp = hero.exp;
		c.hp = hero.HP;
		c.hpMax = hero.HT;
		try {
			c.strength = hero.STR();
		} catch (Throwable ignored){
			c.strength = 0;
		}
		Hunger hunger = hero.buff(Hunger.class);
		c.hunger = hunger == null ? -1 : hunger.hunger();
		try {
			c.shielding = Math.max(0, hero.shielding());
		} catch (Throwable ignored){
			c.shielding = 0;
		}

		Belongings b = hero.belongings;
		if (b != null){
			add(c, LifecycleEvents.SLOT_WEAPON, b.weapon);
			add(c, LifecycleEvents.SLOT_ARMOR, b.armor);
			add(c, LifecycleEvents.SLOT_ARTIFACT, b.artifact);
			add(c, LifecycleEvents.SLOT_MISC, b.misc);
			add(c, LifecycleEvents.SLOT_RING, b.ring);
			add(c, LifecycleEvents.SLOT_SECOND_WEAPON, b.secondWep);
			if (b.backpack != null) addBag(c, b.backpack, 0);
		}
		return c;
	}

	private static void addBag( LifecycleEvents.Checkpoint c, Bag bag, int depth ){
		for (Item item : bag.items.toArray(new Item[0])){
			if (c.items.size() >= MAX_ITEMS) return;
			add(c, LifecycleEvents.SLOT_BACKPACK, item);
			if (item instanceof Bag && depth < MAX_BAG_DEPTH) addBag(c, (Bag) item, depth + 1);
		}
	}

	private static void add( LifecycleEvents.Checkpoint c, int slot, Item item ){
		if (item == null) return;
		try {
			LifecycleEvents.ItemEntry e = new LifecycleEvents.ItemEntry();
			e.slot = slot;
			e.className = shortName(item.getClass());
			e.quantity = item.quantity();
			e.level = item.level();
			e.levelKnown = item.levelKnown;
			e.cursedKnown = item.cursedKnown;
			c.items.add(e);
		} catch (Throwable ignored){
			//one odd item must not stop the checkpoint
		}
	}
}
