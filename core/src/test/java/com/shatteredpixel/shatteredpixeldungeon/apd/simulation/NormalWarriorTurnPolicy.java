package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

/**
 * Pure selectors for synchronous passive actions in the normal Warrior profile.
 * The Android and JVM adapters execute the returned command through the same boundary.
 */
public final class NormalWarriorTurnPolicy {
    public static final int MAX_SAFE_RESTS = 30;

    private NormalWarriorTurnPolicy() {}

    public static WarriorTurnCommand rootedWait(boolean rooted, int position) {
        return rooted ? WarriorTurnCommand.waitAt(WarriorTurnCommand.Reason.ROOTED, position) : null;
    }

    public static WarriorTurnCommand recoveryWait(int position, boolean enemiesVisible,
            boolean canSafelyRest, int hp, int maximumHp, int safeRestActions) {
        if (position < 0 || maximumHp <= 0 || hp < 0 || hp > maximumHp || safeRestActions < 0)
            throw new IllegalArgumentException("invalid recovery observation");
        if (!enemiesVisible && canSafelyRest && hp * 2 < maximumHp
                && safeRestActions < MAX_SAFE_RESTS) {
            return WarriorTurnCommand.waitAt(WarriorTurnCommand.Reason.RECOVERY, position);
        }
        return null;
    }
}
