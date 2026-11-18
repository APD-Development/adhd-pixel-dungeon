package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.apd.demo.UimsdUnlock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class UimsdDemoNoticeTest {
    @Rule public TemporaryFolder history = new TemporaryFolder();
    private static class Flags implements UimsdUnlock.Flags {
        boolean tour;
        public boolean unlocked(){ return true; }
        public void setUnlocked(){}
        public boolean demoUsed(){ return true; }
        public void setDemoUsed(){}
        public boolean tourUnlocked(){ return tour; }
        public void setTourUnlocked(){ tour = true; }
        public boolean promptPending(){ return false; }
        public void setPromptPending(){}
        public boolean promptShown(){ return false; }
        public void setPromptShown(){}
    }
    @Test public void demoAccessDoesNotHideTheTenRealRunNotice() {
        Flags flags = new Flags();
        for (boolean adult : new boolean[]{false, true}) {
            assertEquals(UimsdGate.LOCKED_ARROW_TEXT,
                    UimsdGate.annotationFor(flags, history.getRoot(), true, adult, ""));
        }
        assertTrue(UimsdUnlock.isUnlocked(flags, history.getRoot(), true));
        assertFalse(flags.tour);
    }
    @Test public void genuineProgressionAndNormalBuildKeepTheNoticeRetired() {
        Flags flags = new Flags();
        flags.setTourUnlocked();
        assertEquals(UimsdGate.TENTH_RUN_ARROW_CLEAN, UimsdGate.annotationFor(flags, history.getRoot(), true, false, ""));
        assertEquals("", UimsdGate.annotationFor(new Flags(), history.getRoot(), false, false, ""));
    }
}
