package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class NormalWarriorTurnPolicyTest {

    @Test
    public void rootedStateSelectsTheSharedWaitCommand() {
        assertNull(NormalWarriorTurnPolicy.rootedWait(false, 17));
        WarriorTurnCommand command = NormalWarriorTurnPolicy.rootedWait(true, 17);
        assertNotNull(command);
        assertEquals(WarriorTurnCommand.Type.WAIT, command.type());
        assertEquals(WarriorTurnCommand.Reason.ROOTED, command.reason());
        assertEquals(17, command.target());
    }

    @Test
    public void recoveryRestPreservesTheExistingStrictLimits() {
        WarriorTurnCommand command = NormalWarriorTurnPolicy.recoveryWait(
                23, false, true, 49, 100, NormalWarriorTurnPolicy.MAX_SAFE_RESTS - 1);
        assertNotNull(command);
        assertEquals(WarriorTurnCommand.Reason.RECOVERY, command.reason());

        assertNull(NormalWarriorTurnPolicy.recoveryWait(23, false, true, 50, 100, 0));
        assertNull(NormalWarriorTurnPolicy.recoveryWait(23, true, true, 49, 100, 0));
        assertNull(NormalWarriorTurnPolicy.recoveryWait(23, false, false, 49, 100, 0));
        assertNull(NormalWarriorTurnPolicy.recoveryWait(23, false, true, 49, 100,
                NormalWarriorTurnPolicy.MAX_SAFE_RESTS));
    }

    @Test(expected = IllegalArgumentException.class)
    public void recoveryRejectsImpossibleObservedHealth() {
        NormalWarriorTurnPolicy.recoveryWait(23, false, true, 101, 100, 0);
    }
}
