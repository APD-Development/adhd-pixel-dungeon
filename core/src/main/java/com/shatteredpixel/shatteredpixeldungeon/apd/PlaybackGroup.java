package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

/**
 * Visual-only playback control available to every player.
 * Actor turns, UI input timing, and the global game clock are untouched.
 */
public final class PlaybackGroup extends Group {
    private static final float[] FACTORS = {0.5f, 1f, 2f, 4f, 8f, 16f, 32f, 64f, 100f, 1000f};
    private static final String[] LABELS =
            {"0.5x", "1x", "2x", "4x", "8x", "16x", "32x", "64x", "100x", "1000x"};
    private static final int NORMAL = 1;

    private static float speed = 1f;
    private static int updating;

    /** Only five owner-exempt visual categories use the normal frame clock. */
    public static Group normalSpeedGroup() {
        return new Group();
    }

    public static int choiceCount() {
        return FACTORS.length;
    }

    public static boolean allowed(int selection) {
        return selection >= 0 && selection < choiceCount();
    }

    public static String label(int selection) {
        return LABELS[allowed(selection) ? selection : NORMAL];
    }

    public static void speed(int selection) {
        speed = FACTORS[allowed(selection) ? selection : NORMAL];
    }

    /** Unscaled wall-frame delta even when this group's visuals are accelerated.
     * Audio cadence must not be tied to selected visual playback speed. */
    public static float realElapsed() {
        return updating > 0 ? Game.elapsed / speed : Game.elapsed;
    }

    /**
     * Keep the hero's two-frame standing animation readable when experimental
     * visual playback is extremely fast. This runs only inside a PlaybackGroup;
     * movement/attacks/effects still receive the full selected speed.
     */
    public static float heroIdleElapsed() {
        return updating > 0 && speed > 2f ? Game.elapsed / speed : Game.elapsed;
    }

    @Override public synchronized void update() {
        float original = Game.elapsed;
        if (updating++ == 0) Game.elapsed = original * speed;
        try { super.update(); }
        finally { updating--; Game.elapsed = original; }
    }
}
