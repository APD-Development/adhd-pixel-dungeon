package com.shatteredpixel.shatteredpixeldungeon.items.weapon;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.watabou.utils.Random;

import org.junit.Test;

/** Random enchantments and glyphs are never curses, random curses always are, and excluded picks are never rolled. */
public class RandomEnchantAndGlyphTest {
	@Test public void goodRollsAreNeverCursesAndCurseRollsAlwaysAre(){
		HeadlessTestSupport.start();
		Random.pushGenerator(181118L);
		try {
			for (int i = 0; i < 300; i++){
				assertFalse(Weapon.Enchantment.random().curse());
				assertTrue(Weapon.Enchantment.randomCurse().curse());
				assertFalse(Armor.Glyph.random().curse());
				assertTrue(Armor.Glyph.randomCurse().curse());
			}
		} finally { Random.popGenerator(); }
	}

	@Test public void ignoredEnchantmentIsNeverRolled(){
		HeadlessTestSupport.start();
		Random.pushGenerator(42L);
		try {
			Weapon.Enchantment first = Weapon.Enchantment.randomCommon();
			for (int i = 0; i < 200; i++){
				assertNotSame(first.getClass(), Weapon.Enchantment.randomCommon(first.getClass()).getClass());
			}
		} finally { Random.popGenerator(); }
	}
}
