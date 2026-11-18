package com.shatteredpixel.shatteredpixeldungeon.apd.qol;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class DotForecastTest {
    @Before public void setup(){HeadlessTestSupport.start();Dungeon.depth=8;}

    @Test public void poisonUsesLiveDamageTickAndDurationWithoutRng(){
        Poison p=new Poison();
        p.set(6f);
        DotForecast f=DotForecast.of(p);
        assertNotNull(f);
        assertTrue(f.next.contains("before target defense"));
        assertTrue(f.duration.contains("6.0"));
        assertTrue(f.details().contains("Applied so far"));
    }
    @Test public void corrosionIncreasesNextTickAsMechanicsChange(){
        Corrosion c=new Corrosion();
        c.set(5f,4);
        DotForecast f=DotForecast.of(c);
        assertTrue(f.next.startsWith("4 raw"));
        assertTrue(f.remaining.contains("before target defense"));
    }
    @Test public void oozeReflectsDepthAndDuration(){
        Ooze o=new Ooze();
        o.set(4f);
        assertTrue(DotForecast.of(o).duration.contains("4.0"));
        assertTrue(DotForecast.of(o).next.contains("raw"));
    }
    @Test public void bleedingExplainsRandomDecayAndNeverClaimsExactTotal(){
        Bleeding b=new Bleeding();
        b.set(8f);
        DotForecast f=DotForecast.of(b);
        assertTrue(f.remaining.startsWith("~"));
        assertTrue(f.duration.contains("random decay"));
    }
    @Test public void damageHistoryKeepsRawShieldAndHpSeparately(){
        Poison p=new Poison();
        p.apdRecordDot(12,7,3);
        p.apdRecordDot(8,0,2);
        assertEquals(20,p.apdDotRaw());
        assertEquals(7,p.apdDotShield());
        assertEquals(5,p.apdDotHp());
        assertTrue(DotForecast.of(p).spent.contains("7 shielding"));
    }
    @Test public void buffWithoutDamageEffectDoesNotGenerateForecast(){
        assertNull(DotForecast.of(new Haste()));
        assertNull(DotForecast.of(null));
    }
}
