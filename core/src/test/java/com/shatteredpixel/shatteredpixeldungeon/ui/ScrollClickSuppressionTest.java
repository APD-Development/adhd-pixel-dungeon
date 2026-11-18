package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.watabou.input.PointerEvent;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.Visual;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScrollClickSuppressionTest {
    private static class VisibleHit extends Visual {
        VisibleHit(){ super(0, 0, 30, 30); }
        @Override public boolean overlapsScreenPoint(int x, int y){ return true; }
    }
    private static class ClickTarget extends PointerArea {
        int clicks;
        int releases;
        ClickTarget(){ super(new VisibleHit()); }
        @Override protected void onPointerUp(PointerEvent event){ releases++; }
        @Override protected void onClick(PointerEvent event){ clicks++; }
    }
    private static PointerEvent touch(int id) {
        return new PointerEvent(10, 12, id, PointerEvent.Type.DOWN, PointerEvent.LEFT);
    }

    @Test public void aScrollingGestureReleasesItsTargetButCannotActivateIt(){
        ClickTarget control = new ClickTarget();
        try {
            PointerEvent scrolling = touch(101);
            assertFalse(scrolling.scrolled);
            control.onSignal(scrolling);
            scrolling.scrolled = true;
            control.onSignal(scrolling.up());
            assertEquals("finger release is preserved", 1, control.releases);
            assertEquals("scroll cannot press a button or a link", 0, control.clicks);

            PointerEvent tap = touch(102);
            assertFalse("new pointers must never inherit old suppression", tap.scrolled);
            control.onSignal(tap);
            control.onSignal(tap.up());
            assertEquals("ordinary tap still activates control", 1, control.clicks);
            assertEquals(2, control.releases);

            PointerEvent cancelled = touch(103);
            control.onSignal(cancelled);
            control.onSignal(cancelled.cancel());
            assertEquals("cancel never clicks", 1, control.clicks);
        } finally {
            control.destroy();
        }
    }
}
