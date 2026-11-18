/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap;
import org.junit.Test;
import static org.junit.Assert.*;

/** The one-step trap confirmation must never authorize missing identities. */
public class TrapStepPermitIdentityTest {

    private static class EmptyLevel extends Level {
        @Override protected boolean build() { return true; }
        @Override protected void createMobs() { }
        @Override protected void createItems() { }
    }

    @Test public void missingLevelCannotAuthorizeEvenAnIdenticalNullLevel() {
        TrapStepPermit permit = new TrapStepPermit();
        WornDartTrap trap = new WornDartTrap();
        HeroAction action = new HeroAction.Move(13);
        permit.allow(null, trap, action, 12, 13);
        assertFalse(permit.consume(null, trap, action, 12, 13));
    }

    @Test public void missingTrapCannotAuthorizeEvenAnIdenticalNullTrap() {
        TrapStepPermit permit = new TrapStepPermit();
        Level level = new EmptyLevel();
        HeroAction action = new HeroAction.Move(13);
        permit.allow(level, null, action, 12, 13);
        assertFalse(permit.consume(level, null, action, 12, 13));
    }

    @Test public void invalidGrantClearsPreviouslyValidPermission() {
        TrapStepPermit permit = new TrapStepPermit();
        Level level = new EmptyLevel();
        WornDartTrap trap = new WornDartTrap();
        HeroAction action = new HeroAction.Move(13);
        permit.allow(level, trap, action, 12, 13);
        permit.allow(level, null, action, 12, 13);
        assertFalse(permit.consume(level, trap, action, 12, 13));
        permit.allow(level, trap, action, 12, 13);
        permit.allow(null, trap, action, 12, 13);
        assertFalse(permit.consume(level, trap, action, 12, 13));
        permit.allow(level, trap, action, 12, 13);
        permit.allow(level, trap, null, 12, 13);
        assertFalse(permit.consume(level, trap, action, 12, 13));
    }

    @Test public void validExactConfirmationRemainsSingleUseAndMismatchesConsumeIt() {
        TrapStepPermit permit = new TrapStepPermit();
        Level level = new EmptyLevel();
        WornDartTrap trap = new WornDartTrap();
        HeroAction action = new HeroAction.Move(13);
        permit.allow(level, trap, action, 12, 13);
        assertTrue(permit.consume(level, trap, action, 12, 13));
        assertFalse(permit.consume(level, trap, action, 12, 13));
        permit.allow(level, trap, action, 12, 13);
        assertFalse(permit.consume(level, trap, action, 12, 14));
        assertFalse(permit.consume(level, trap, action, 12, 13));
    }
}
