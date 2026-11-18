package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon;

/**
 * DM-300's current active pylon is the useful auto-target during the
 * supercharged phase. This only chooses an already visible/aimable candidate;
 * it never changes pylon alignment, damage, boss phases, or projectile rules.
 */
public final class BossPylonTargeting {
    private BossPylonTargeting() {}

    public static boolean isActivePylon(Mob mob) {
        return mob instanceof Pylon && mob.isAlive()
                && mob.alignment == Char.Alignment.ENEMY;
    }

    public static boolean prefer(Mob candidate, Mob current) {
        if (current == null) return true;
        if (isActivePylon(candidate) != isActivePylon(current))
            return isActivePylon(candidate);
        return false; // caller's existing distance order is preserved
    }

    /** Candidates must already be visible and attackable. No extra range rules. */
    public static Mob activeAttackablePylon(Iterable<? extends Mob> candidates) {
        if (candidates == null) return null;
        for (Mob mob : candidates) {
            if (isActivePylon(mob)) return mob;
        }
        return null;
    }

    public static boolean replaceInvulnerableBoss(Char previous, Mob choice) {
        return isActivePylon(choice) && previous instanceof DM300
                && ((DM300) previous).isSupercharged();
    }
}
