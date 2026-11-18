package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ImpQuestPersistenceTest {

	@After
	public void cleanUp() { Imp.Quest.reset(); }

	private Bundle legacyCompletedQuest() {
		Bundle quests = new Bundle();
		Bundle imp = new Bundle();
		imp.put("spawned", true);
		imp.put("given", true);
		imp.put("completed", true);
		imp.put("alternative", true);
		quests.put("demon", imp);
		return quests;
	}

	@Test
	public void loadingSaveWithoutQuestCannotInheritCompletedQuest() {
		Imp.Quest.restoreFromBundle(legacyCompletedQuest());
		assertTrue(Imp.Quest.isCompleted());
		assertTrue(Imp.Quest.earnedShop());
		Imp.Quest.restoreFromBundle(new Bundle());
		assertFalse(Imp.Quest.isCompleted());
		assertFalse(Imp.Quest.earnedShop());
		assertFalse(Imp.Quest.given());
	}

	@Test
	public void unspawnedSaveClearsPreviousQuestAndRewards() {
		Imp.Quest.restoreFromBundle(legacyCompletedQuest());
		Imp.Quest.rewardOptions.add(new Sword());
		Bundle quests = new Bundle();
		Bundle imp = new Bundle();
		imp.put("spawned", false);
		quests.put("demon", imp);
		Imp.Quest.restoreFromBundle(quests);
		assertFalse(Imp.Quest.earnedShop());
		assertFalse(Imp.Quest.isOld());
		assertTrue(Imp.Quest.rewardOptions.isEmpty());
	}

	@Test
	public void resetClearsRewardsAndLegacyStateWithoutChangingLoadedLegacySave() {
		Imp.Quest.restoreFromBundle(legacyCompletedQuest());
		assertTrue(Imp.Quest.isOld());
		Imp.Quest.rewardOptions.add(new Sword());
		Imp.Quest.reset();
		assertFalse(Imp.Quest.isOld());
		assertTrue(Imp.Quest.rewardOptions.isEmpty());
		assertEquals(2, Imp.Quest.hazardFreebies);
		Imp.Quest.restoreFromBundle(legacyCompletedQuest());
		assertTrue(Imp.Quest.isOld());
		assertTrue(Imp.Quest.earnedShop());
	}
}
