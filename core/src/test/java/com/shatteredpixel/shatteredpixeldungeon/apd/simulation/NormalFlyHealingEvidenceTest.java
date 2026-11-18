package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import org.junit.Test;
import static org.junit.Assert.*;

public class NormalFlyHealingEvidenceTest {
    private GenerationEvents.ItemGenerated drop() {
        GenerationEvents.ItemGenerated d = new GenerationEvents.ItemGenerated();
        d.container = GenerationEvents.C_MOB_LOOT;
        d.owner = "actors.mobs.Swarm";
        d.trueClass = "items.potions.PotionOfHealing";
        return d;
    }
    @Test public void acceptsActualRecorderNamesAndRejectsSimpleNames() {
        GenerationEvents.ItemGenerated d = drop();
        assertTrue(NormalFlyHealingEvidence.matches(d));
        d.owner = "Swarm"; assertFalse(NormalFlyHealingEvidence.matches(d));
        d = drop(); d.trueClass = "PotionOfHealing"; assertFalse(NormalFlyHealingEvidence.matches(d));
    }
    @Test public void bonusLootAndOtherSourcesAreNotNormalFlyHealing() {
        GenerationEvents.ItemGenerated d = drop(); d.container = GenerationEvents.C_BONUS_DROP;
        assertFalse(NormalFlyHealingEvidence.matches(d));
        d = drop(); d.owner = "actors.mobs.Bat"; assertFalse(NormalFlyHealingEvidence.matches(d));
        assertFalse(NormalFlyHealingEvidence.matches(null));
    }
    @Test public void exoticShieldingDoesNotBecomeAHealingObservation() {
        GenerationEvents.ItemGenerated d = drop(); d.trueClass = "items.potions.exotic.PotionOfShielding";
        assertFalse(NormalFlyHealingEvidence.matches(d));
    }
}
