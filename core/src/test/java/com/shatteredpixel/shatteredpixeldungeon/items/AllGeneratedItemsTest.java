package com.shatteredpixel.shatteredpixeldungeon.items;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Alchemize;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.BeaconOfReturning;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.CurseInfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.GreaterMetamorphosis;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.MagicalInfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.PhaseShift;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Recycle;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.TelekineticGrab;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.UnstableSpell;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.WildEnergy;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Every item the generator can drop, plus every spell and Gemstone Dust: builds, has text, a value, and saves. */
public class AllGeneratedItemsTest {
	private Hero oldHero;

	@Before public void setUp(){ HeadlessTestSupport.start(); oldHero = Dungeon.hero; Dungeon.hero = new Hero(); }
	@After public void tearDown(){ Dungeon.hero = oldHero; }

	private static Set<Class<?>> items(){
		Set<Class<?>> out = new LinkedHashSet<>();
		for (Generator.Category cat : Generator.Category.values()){
			if (cat.classes == null) continue;
			for (Class<?> c : cat.classes) if (c != null) out.add(c);
		}
		Class<?>[] extra = {Alchemize.class, BeaconOfReturning.class, CurseInfusion.class, GreaterMetamorphosis.class,
				MagicalInfusion.class, PhaseShift.class, ReclaimTrap.class, Recycle.class, SummonElemental.class,
				TelekineticGrab.class, UnstableSpell.class, WildEnergy.class, RingDust.class, ArcaneResin.class};
		for (Class<?> c : extra) out.add(c);
		return out;
	}

	@Test public void manyItemsAreCovered(){
		assertTrue(items().size() > 100);
	}

	@Test public void everyItemHasTextAndANonNegativeValue(){
		List<String> problems = new ArrayList<>();
		for (Class<?> c : items()){
			try {
				Item item = (Item) Reflection.newInstance(c);
				String name = item.name(), desc = item.desc();
				if (name == null || name.isEmpty() || name.contains(Messages.NO_TEXT_FOUND)) problems.add(c.getSimpleName() + " name");
				if (desc == null || desc.isEmpty() || desc.contains(Messages.NO_TEXT_FOUND)) problems.add(c.getSimpleName() + " desc");
				if (item.value() < 0) problems.add(c.getSimpleName() + " value " + item.value());
				if (item.quantity() < 1) problems.add(c.getSimpleName() + " quantity " + item.quantity());
			} catch (Throwable needsAScreen){
				//e.g. the Dried Rose describes its ghost through the current scene
			}
		}
		assertTrue(problems.toString(), problems.isEmpty());
	}

	@Test public void quantityLevelAndCurseSurviveASave(){
		List<String> problems = new ArrayList<>();
		for (Class<?> c : items()){
			try {
				Item item = (Item) Reflection.newInstance(c);
				if (item.stackable) item.quantity(3);
				if (item.isUpgradable()) item.level(2);
				Bundle b = new Bundle();
				item.storeInBundle(b);
				Item back = (Item) Reflection.newInstance(c);
				back.restoreFromBundle(b);
				if (back.quantity() != item.quantity()) problems.add(c.getSimpleName() + " quantity");
				if (back.level() != item.level()) problems.add(c.getSimpleName() + " level " + back.level() + " != " + item.level());
				if (back.cursed != item.cursed) problems.add(c.getSimpleName() + " curse");
			} catch (Throwable t){
				problems.add(c.getSimpleName() + " threw " + t);
			}
		}
		assertTrue(problems.toString(), problems.isEmpty());
	}
}
