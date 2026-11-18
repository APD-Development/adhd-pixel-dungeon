package com.shatteredpixel.shatteredpixeldungeon.apd.humor;

import com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.util.Random;
import static org.junit.Assert.*;

public class ClericLoreActivationTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    @After public void reset() { ApdFeatures.resetForTest(); }

    @Test public void privateBuildGateReachesShippedFirstRunAndRemembersOnlyActualDisplay() {
        ApdFeatures.parse("testBuild\ngrudge\nclericPlayable\n");
        assertTrue(ClericGrudge.enabled());
        File history = new File(tmp.getRoot(), "ledger.txt");
        GrudgePrefs prefs = HumorTestSupport.prefs(Frequency.RARER);
        ClericGrudge g = new ClericGrudge(HumorTestSupport.shipped(), new Ledger(history), new Random(7), ClericGrudge.enabled(), 100L);
        Shown first = g.firstClericRun(prefs, false, 100L);
        assertNotNull("No surprise grace/chance applies to first-run message", first);
        assertEquals("first.0001", first.id);
        assertNotNull(g.firstClericRun(prefs, false, 101L));
        g.confirm(first, 102L);
        ClericGrudge reloaded = new ClericGrudge(HumorTestSupport.shipped(), new Ledger(history), new Random(8), ClericGrudge.enabled(), 200L);
        assertNull("An update/restart must not repeat a collected first-run entry", reloaded.firstClericRun(prefs, true, 200L));
        assertNotNull(new Ledger(history).get("first.0001"));
    }

    @Test public void privateActivationPreservesSavedOptOutWithoutConsumingEntry() {
        ApdFeatures.parse("testBuild\ngrudge\nclericPlayable\n");
        GrudgePrefs prefs = HumorTestSupport.prefs(Frequency.OFF);
        ClericGrudge g = new ClericGrudge(HumorTestSupport.shipped(), new Ledger(null), new Random(7), ClericGrudge.enabled(), 100L);
        assertNull(g.firstClericRun(prefs, false, 100L));
        prefs.setFrequency(Frequency.RARE);
        assertEquals("first.0001", g.firstClericRun(prefs, false, 101L).id);
    }
}
