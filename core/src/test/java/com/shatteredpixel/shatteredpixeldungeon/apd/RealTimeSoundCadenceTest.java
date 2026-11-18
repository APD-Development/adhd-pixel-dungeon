package com.shatteredpixel.shatteredpixeldungeon.apd;
import static org.junit.Assert.*;
import org.junit.Test;

public class RealTimeSoundCadenceTest {
    private static int simulate(float speed) {
        RealTimeSoundCadence gate=new RealTimeSoundCadence(0.8f);
        int completions=0,sounds=0;
        // 8 real seconds in 0.01-second frames; hammer animation repeats
        // once per 0.8 visual seconds, while its audio must stay real-time.
        float visual=0;
        for(int f=0;f<800;f++) {
            gate.advance(0.01f);
            visual+=0.01f*speed;
            while(visual>=0.8f) {
                visual-=0.8f;
                completions++;
                if(gate.accept())sounds++;
            }
        }
        assertTrue(completions>=sounds);
        return sounds;
    }
    @Test public void twiceAsFastAnimationDoesNotDoubleHammerSfx(){int normal=simulate(1);int fast=simulate(2);assertTrue(Math.abs(fast-normal)<=1);}
    @Test public void extremelyFastAnimationStillKeepsNormalHammerRhythm(){int normal=simulate(1);for(float s:new float[]{4,8,16,32})assertTrue(Math.abs(simulate(s)-normal)<=1);}
    @Test public void cooldownDoesNotAdvanceTwiceOnOneFrame(){RealTimeSoundCadence c=new RealTimeSoundCadence(0.8f);assertTrue(c.accept());assertFalse(c.accept());c.advance(.4f);assertFalse(c.accept());c.advance(.41f);assertTrue(c.accept());}
    @Test public void invalidElapsedCannotCauseExtraStrike(){RealTimeSoundCadence c=new RealTimeSoundCadence(.8f);assertTrue(c.accept());c.advance(-1);c.advance(Float.NaN);assertFalse(c.accept());}
}
