package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;

public final class NormalFlyHealingEvidence {
    private NormalFlyHealingEvidence() {}
    public static boolean matches(GenerationEvents.ItemGenerated drop) {
        // Recorder names retain their package below the game package; simple names are not its format.
        return drop != null && drop.container == GenerationEvents.C_MOB_LOOT
                && "actors.mobs.Swarm".equals(drop.owner)
                && "items.potions.PotionOfHealing".equals(drop.trueClass);
    }
}
