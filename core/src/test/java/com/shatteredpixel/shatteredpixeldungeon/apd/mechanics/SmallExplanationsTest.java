package com.shatteredpixel.shatteredpixeldungeon.apd.mechanics;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.CrystalKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.GoldenKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;

import org.junit.Before;
import org.junit.Test;

/** Dew, key and staff explanations: their branches and the honest "unknown" answers. */
public class SmallExplanationsTest {
	@Before public void setUp(){ HeadlessTestSupport.start(); }

	@Test public void keysNameTheirLockAndFloor(){
		String gold = KeyExplanation.details(new GoldenKey(4));
		assertTrue(gold, gold.contains("a locked chest"));
		assertTrue(gold, gold.contains("Floor 4"));
		assertTrue(KeyExplanation.details(new IronKey(2)).contains("an ordinary locked door"));
		assertTrue(KeyExplanation.details(new CrystalKey(9)).contains("crystal chest or crystal door"));
		assertTrue(KeyExplanation.details(new IronKey(0)).contains("No floor is recorded"));
	}

	@Test public void dewWithoutACharacterAssumesNothing(){
		String text = DewExplanation.details(new Waterskin(), null);
		assertTrue(text, text.contains("require a loaded character"));
		assertTrue(text, text.contains("of " + Waterskin.capacity() + " drops"));
	}

	@Test public void dewWithAHeroShowsTheirNumbers(){
		Hero hero = new Hero();
		hero.HT = hero.HP = 40;
		String text = DewExplanation.details(new Waterskin(), hero);
		assertTrue(text, text.contains("Maximum HP: 40 HP"));
	}

	@Test public void staffNeedsAWandAndKnownLevels(){
		MagesStaff staff = new MagesStaff();
		WandOfMagicMissile wand = new WandOfMagicMissile();
		assertEquals("", StaffExplanation.details(staff, null, new Hero()));
		assertEquals("", StaffExplanation.details(staff, wand, null));
		staff.levelKnown = false;
		assertTrue(StaffExplanation.details(staff, wand, new Hero()).startsWith("Identify the staff"));
	}
}
