package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Test;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class BlacksmithQuestPersistenceTest {
	@After
	public void cleanUp() {
		Blacksmith.Quest.freePickaxe = false;
		Blacksmith.Quest.smithRewards = null;
		Blacksmith.Quest.smithEnchant = null;
		Blacksmith.Quest.smithGlyph = null;
	}

	private Bundle saveWith(Bundle smith) {
		smith.put("spawned", true);
		Bundle quests = new Bundle();
		quests.put("blacksmith", smith);
		return quests;
	}

	private void previousSave() {
		Blacksmith.Quest.freePickaxe = true;
		Blacksmith.Quest.smithEnchant = new Blazing();
		Blacksmith.Quest.smithGlyph = new Flow();
	}

	@Test
	public void oldSaveWithoutOptionalFieldsCannotInheritFreePickaxeOrEnchantments() {
		previousSave();
		Blacksmith.Quest.restoreFromBundle(saveWith(new Bundle()));
		assertFalse(Blacksmith.Quest.freePickaxe);
		assertNull(Blacksmith.Quest.smithRewards);
		assertNull(Blacksmith.Quest.smithEnchant);
		assertNull(Blacksmith.Quest.smithGlyph);
	}

	@Test
	public void unenchantedRewardsCannotInheritAnotherSavesEnchantments() {
		previousSave();
		Bundle smith = new Bundle();
		smith.put("smith_rewards", new ArrayList<Item>());
		Blacksmith.Quest.restoreFromBundle(saveWith(smith));
		assertNotNull(Blacksmith.Quest.smithRewards);
		assertNull(Blacksmith.Quest.smithEnchant);
		assertNull(Blacksmith.Quest.smithGlyph);
	}

	@Test
	public void explicitFreePickaxeAndEnchantedRewardsStillRestore() {
		Bundle smith = new Bundle();
		smith.put("free_pickaxe", true);
		smith.put("smith_rewards", new ArrayList<Item>());
		smith.put("enchant", new Blazing());
		smith.put("glyph", new Flow());
		Blacksmith.Quest.restoreFromBundle(saveWith(smith));
		assertTrue(Blacksmith.Quest.freePickaxe);
		assertTrue(Blacksmith.Quest.smithEnchant instanceof Blazing);
		assertTrue(Blacksmith.Quest.smithGlyph instanceof Flow);
	}
}
