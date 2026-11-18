package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import org.junit.Test;
import static org.junit.Assert.*;

public class SimulationReadinessTest {
    @Test public void pendingCommandMustNotReuseThePreviousReadyFlag() {
        assertFalse(SimulationReadiness.actionReleased(23, 23, 2, 2, true, true, true));
        assertFalse(SimulationReadiness.actionReleased(23, 23, 2, 2, true, false, false));
    }
    @Test public void freePickupOrInterruptedMoveCanReleaseInputWithoutTurnAdvance() {
        assertTrue(SimulationReadiness.actionReleased(23, 23, 2, 2, true, true, false));
    }
    @Test public void paidActionDeathAndFloorTransitionReleaseTheInitialWait() {
        assertTrue(SimulationReadiness.actionReleased(23, 24, 2, 2, true, false, true));
        assertTrue(SimulationReadiness.actionReleased(23, 23, 2, 2, false, false, true));
        assertTrue(SimulationReadiness.actionReleased(23, 0, 2, 3, true, false, true));
    }
}
