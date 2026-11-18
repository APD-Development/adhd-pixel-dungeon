package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class KnownMapExplorerTest {
    @Test public void ignoresUnseenAndBlockedCells() {
        boolean[] known = {true, true, false, false, true, false};
        boolean[] pass = {true, true, true, true, false, true};
        assertEquals(1, KnownMapExplorer.step(3, 0, known, pass, new int[6], -1));
    }
    @Test public void avoidsRowWrappingAndForbiddenStairs() {
        boolean[] known = {false, false, true, true, false, false};
        assertEquals(-1, KnownMapExplorer.step(3, 2, known, known, new int[6], -1));
        assertEquals(-1, KnownMapExplorer.step(3, 0, new boolean[]{true,true,true},
                new boolean[]{true,true,true}, new int[3], 1));
    }
    @Test public void prefersUnvisitedCellsAndReturnsOnlyOneStep() {
        boolean[] known = new boolean[9]; Arrays.fill(known, true);
        int[] visits = new int[9]; Arrays.fill(visits, 1); visits[8] = 0;
        assertEquals(4, KnownMapExplorer.step(3, 0, known, known, visits, -1));
        assertArrayEquals(new int[]{1,1,1,1,1,1,1,1,0}, visits);
    }
    @Test public void handlesNoReachableDestinationAndRejectsInvalidDimensions() {
        assertEquals(-1, KnownMapExplorer.step(1, 0, new boolean[]{true},
                new boolean[]{true}, new int[1], -1));
        try { KnownMapExplorer.step(4, 0, new boolean[3], new boolean[3], new int[3], -1); fail(); }
        catch (IllegalArgumentException expected) { }
    }
}
