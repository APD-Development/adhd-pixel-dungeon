package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

/**
 * One adapter-neutral command selected by the normal Warrior test policy.
 * Commands are test-only and execute real game actions in each adapter.
 */
public final class WarriorTurnCommand {
    public enum Type { WAIT }
    public enum Reason { ROOTED, RECOVERY }

    private final Type type;
    private final Reason reason;
    private final int target;

    private WarriorTurnCommand(Type type, Reason reason, int target) {
        if (type == null || reason == null || target < 0)
            throw new IllegalArgumentException("invalid turn command");
        this.type = type;
        this.reason = reason;
        this.target = target;
    }

    public static WarriorTurnCommand waitAt(Reason reason, int target) {
        return new WarriorTurnCommand(Type.WAIT, reason, target);
    }

    public Type type() { return type; }
    public Reason reason() { return reason; }
    public int target() { return target; }
}
