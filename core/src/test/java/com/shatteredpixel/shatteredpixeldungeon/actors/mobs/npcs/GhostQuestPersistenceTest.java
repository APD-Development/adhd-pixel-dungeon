package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class GhostQuestPersistenceTest {
	@After
	public void cleanUp() { Ghost.Quest.reset(); }

	@Test
	public void unenchantedSaveCannotInheritPreviousSavesEnchantments() {
		Ghost.Quest.enchant = new Blazing();
		Ghost.Quest.glyph = new Flow();
		Bundle quests = new Bundle();
		Bundle ghost = new Bundle();
		ghost.put("spawned", true);
		quests.put("sadGhost", ghost);
		Ghost.Quest.restoreFromBundle(quests);
		assertNull(Ghost.Quest.enchant);
		assertNull(Ghost.Quest.glyph);
	}

	@Test
	public void enchantedSaveStillRestoresItsOwnEnchantments() {
		Bundle quests = new Bundle();
		Bundle ghost = new Bundle();
		ghost.put("spawned", true);
		ghost.put("enchant", new Blazing());
		ghost.put("glyph", new Flow());
		quests.put("sadGhost", ghost);
		Ghost.Quest.restoreFromBundle(quests);
		assertTrue(Ghost.Quest.enchant instanceof Blazing);
		assertTrue(Ghost.Quest.glyph instanceof Flow);
	}
}
