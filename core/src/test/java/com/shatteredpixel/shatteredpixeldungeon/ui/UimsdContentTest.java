package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import org.junit.Test;
import static org.junit.Assert.*;

public class UimsdContentTest {
    private static class Tracked extends Gizmo {
        int destroyed;
        @Override public void destroy() { destroyed++; super.destroy(); }
    }
    @Test public void replacedChildrenAreDisposedOnce() {
        UimsdContent content = new UimsdContent();
        Tracked child = new Tracked(); content.add(child);
        content.clear(); content.clear(); content.destroy();
        assertEquals(1, child.destroyed); assertNull(child.parent);
    }
    @Test public void replacementContainerRemainsReusable() {
        UimsdContent content = new UimsdContent();
        Tracked first = new Tracked(); content.add(first); content.clear();
        Tracked next = new Tracked(); content.add(next);
        assertSame(content, next.parent); assertEquals(0, next.destroyed);
        content.clear(); assertEquals(1, next.destroyed);
    }
    @Test public void nestedResourcesAreDisposed() {
        UimsdContent content = new UimsdContent();
        Group group = new Group(); Tracked child = new Tracked();
        group.add(child); content.add(group); content.clear();
        assertEquals(1, child.destroyed); assertNull(child.parent);
    }
    @Test public void childDetachingDuringDestructionDoesNotSkipSibling() {
        UimsdContent content = new UimsdContent();
        Tracked first = new Tracked() {
            @Override public void destroy() {
                if (parent != null) parent.erase(this);
                super.destroy();
            }
        };
        Tracked sibling = new Tracked();
        content.add(first); content.add(sibling); content.clear();
        assertEquals(1, first.destroyed); assertEquals(1, sibling.destroyed);
    }
    @Test public void removedChildrenAreNotDestroyedByFormerOwner() {
        UimsdContent content = new UimsdContent();
        Tracked removed = new Tracked(); Tracked retained = new Tracked();
        content.add(removed); content.add(retained); content.remove(removed);
        content.clear(); assertEquals(0, removed.destroyed);
        assertEquals(1, retained.destroyed);
    }
}
