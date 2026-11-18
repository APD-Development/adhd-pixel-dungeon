package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

/**
 * Boundary between adapter-neutral simulation decisions and a concrete game runtime.
 */
public interface WarriorTurnDriver {
    void execute(WarriorTurnCommand command);
}
