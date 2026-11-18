package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures;
import com.shatteredpixel.shatteredpixeldungeon.apd.PlaybackGroup;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.MovieClip.Animation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MobIdlePlaybackTest {

    private float priorElapsed;

    @Before
    public void start() {
        priorElapsed = Game.elapsed;
        ApdFeatures.parse("testBuild\\n".replace("\\n", "\n"));
    }

    @After
    public void restore() {
        Game.elapsed = priorElapsed;
        PlaybackGroup.speed(1);
        ApdFeatures.resetForTest();
    }

    private static final class ProbeMob extends MobSprite {
        private final Animation action = new Animation(2, true);

        ProbeMob() {
            idle = new Animation(2, true);
            idle.frames = new com.watabou.utils.RectF[1];
            action.frames = new com.watabou.utils.RectF[1];
        }

        float advance(boolean standing) {
            curAnim = standing ? idle : action;
            frameTimer = 0f;
            updateAnimation();
            return frameTimer;
        }
    }

    private float insidePlaybackGroup(int mode, boolean standing) {
        PlaybackGroup.speed(mode);
        Game.elapsed = 0.02f;
        ProbeMob probe = new ProbeMob();
        final float[] timer = {-1f};
        PlaybackGroup group = new PlaybackGroup();
        group.add(new Gizmo() {
            @Override public void update() {
                timer[0] = probe.advance(standing);
            }
        });
        group.update();
        assertEquals("The global clock must be restored", 0.02f, Game.elapsed, 0.000001f);
        return timer[0];
    }

    @Test
    public void fourXDoesNotAccelerateMonsterStandingPose() {
        assertEquals(0.02f, insidePlaybackGroup(3, true), 0.000001f);
        assertEquals("Action animation must retain the selected 4x visual speed",
                0.08f, insidePlaybackGroup(3, false), 0.000001f);
    }

    @Test
    public void originalSpeedsPreserveMonsterIdleAndActionTiming() {
        float[] expected = {0.01f, 0.02f, 0.04f};
        for (int mode = 0; mode < 3; mode++) {
            assertEquals(expected[mode], insidePlaybackGroup(mode, true), 0.000001f);
            assertEquals(expected[mode], insidePlaybackGroup(mode, false), 0.000001f);
        }
    }
}
