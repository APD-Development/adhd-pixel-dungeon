package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.AntiEntropy;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Annoying;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Remove Curse: what it can target, and that it strips both the curse flag and curse enchantments and glyphs. */
public class RemoveCurseTest {
	private Hero oldHero;

	@Before public void setUp(){ HeadlessTestSupport.start(); oldHero = Dungeon.hero; Dungeon.hero = new Hero(); }
	@After public void tearDown(){ Dungeon.hero = oldHero; }

	@Test public void targetsCursedOrUnknownGearButNotFood(){
		Sword cursed = new Sword(); cursed.cursed = true; cursed.cursedKnown = true;
		Sword clean = new Sword(); clean.cursed = false; clean.cursedKnown = true; clean.levelKnown = true;
		Sword unknown = new Sword(); unknown.cursedKnown = false;
		assertTrue(ScrollOfRemoveCurse.uncursable(cursed));
		assertTrue(ScrollOfRemoveCurse.uncursable(unknown));
		assertFalse(ScrollOfRemoveCurse.uncursable(clean));
		assertFalse(ScrollOfRemoveCurse.uncursable(new Food()));
	}

	@Test public void stripsCurseFlagEnchantmentAndGlyph(){
		Sword sword = new Sword(); sword.cursed = true; sword.enchant(new Annoying());
		LeatherArmor armor = new LeatherArmor(); armor.cursed = true; armor.inscribe(new AntiEntropy());
		assertTrue(ScrollOfRemoveCurse.uncurse(null, sword, armor));
		assertFalse(sword.cursed);
		assertTrue(sword.cursedKnown);
		assertFalse(sword.hasCurseEnchant());
		assertFalse(armor.cursed);
		assertFalse(armor.hasCurseGlyph());
	}

	@Test public void cleanItemsReportNothingDoneButBecomeKnown(){
		Sword sword = new Sword(); sword.cursed = false; sword.cursedKnown = false;
		assertFalse(ScrollOfRemoveCurse.uncurse(null, sword, null));
		assertTrue(sword.cursedKnown);
	}
}
