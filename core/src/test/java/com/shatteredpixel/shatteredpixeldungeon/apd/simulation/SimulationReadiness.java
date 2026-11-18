package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

// A normal pickup or interrupted move may return control without spending a turn.
public final class SimulationReadiness {
    private SimulationReadiness() {}
    public static boolean actionReleased(float beforeTime, float currentTime, int beforeDepth,
                                         int currentDepth, boolean alive, boolean ready, boolean queuedAction) {
        return !alive || currentDepth != beforeDepth || currentTime > beforeTime
                || (ready && !queuedAction);
    }
}
