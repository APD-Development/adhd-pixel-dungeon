package com.shatteredpixel.shatteredpixeldungeon.apd;

/** Limits repeated ambient SFX to real time, never modifies game or animation speed. */
public final class RealTimeSoundCadence {
    private final float minimumSeconds;
    private float remaining;
    public RealTimeSoundCadence(float minimumSeconds) {
        if (!(minimumSeconds > 0) || !Float.isFinite(minimumSeconds)) throw new IllegalArgumentException("cadence");
        this.minimumSeconds=minimumSeconds;
    }
    public void advance(float unscaledSeconds) {
        if (!Float.isFinite(unscaledSeconds) || unscaledSeconds < 0) return;
        remaining = Math.max(0,remaining-unscaledSeconds);
    }
    public boolean accept() {
        // A fractional frame of floating-point drift must not postpone a strike.
        if (remaining > 0.001f) return false;
        remaining = minimumSeconds;
        return true;
    }
}
