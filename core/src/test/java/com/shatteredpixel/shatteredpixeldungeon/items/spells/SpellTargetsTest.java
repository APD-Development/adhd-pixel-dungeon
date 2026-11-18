package com.shatteredpixel.shatteredpixeldungeon.items.spells;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.CausticBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAugmentation;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;

import org.junit.Test;

import java.lang.reflect.Method;

/** Item-targeting spells only offer the items their text says they work on. */
public class SpellTargetsTest {
	private static boolean usable(InventorySpell s, Item i) throws Exception {
		Method m = s.getClass().getDeclaredMethod("usableOnItem", Item.class);
		m.setAccessible(true);
		return (Boolean) m.invoke(s, i);
	}

	@Test public void magicalInfusionTakesUpgradablesOnly() throws Exception {
		HeadlessTestSupport.start();
		MagicalInfusion s = new MagicalInfusion();
		assertTrue(usable(s, new Dagger()));
		assertTrue(usable(s, new LeatherArmor()));
		assertTrue(usable(s, new RingOfHaste()));
		assertTrue(usable(s, new WandOfMagicMissile()));
		assertFalse(usable(s, new Food()));
		assertFalse(usable(s, new PotionOfHealing()));
	}

	@Test public void curseInfusionTakesGearOnly() throws Exception {
		HeadlessTestSupport.start();
		CurseInfusion s = new CurseInfusion();
		assertTrue(usable(s, new Dagger()));
		assertTrue(usable(s, new LeatherArmor()));
		assertTrue(usable(s, new WandOfMagicMissile()));
		assertFalse(usable(s, new Food()));
		assertFalse(usable(s, new ScrollOfIdentify()));
	}

	@Test public void recycleTakesConsumablesButNotBrewsOrElixirs() throws Exception {
		HeadlessTestSupport.start();
		Recycle s = new Recycle();
		assertTrue(usable(s, new PotionOfHealing()));
		assertTrue(usable(s, new ScrollOfIdentify()));
		assertTrue(usable(s, new Sungrass.Seed()));
		assertTrue(usable(s, new StoneOfAugmentation()));
		assertFalse(usable(s, new CausticBrew()));
		assertFalse(usable(s, new ElixirOfMight()));
		assertFalse(usable(s, new Dagger()));
		assertFalse(usable(s, new Food()));
	}

	@Test public void curseInfusionCursesAWeapon() throws Exception {
		HeadlessTestSupport.start();
		com.watabou.utils.Random.pushGenerator(3L);
		try {
			Dagger d = new Dagger();
			com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment c = com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment.randomCurse();
			d.enchant(c);
			d.cursed = true;
			assertTrue(d.hasCurseEnchant());
			assertFalse(d.hasGoodEnchant());
		} finally { com.watabou.utils.Random.popGenerator(); }
	}
}
