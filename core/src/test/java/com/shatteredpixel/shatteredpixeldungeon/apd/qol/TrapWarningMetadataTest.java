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

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap;
import com.watabou.utils.SparseArray;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/** An incomplete level must not crash voluntary trap warning checks. */
public class TrapWarningMetadataTest {
    private static final int STEP = 13;

    private static class EmptyLevel extends Level {
        @Override protected boolean build() { return true; }
        @Override protected void createMobs() { }
        @Override protected void createItems() { }
    }

    private EmptyLevel level;
    private Hero hero;
    private WornDartTrap trap;

    @Before public void setup() {
        HeadlessTestSupport.start();
        level = new EmptyLevel();
        level.setSize(5, 5);
        level.traps = new SparseArray<>();
        level.heroFOV = new boolean[level.length()];
        level.heroFOV[STEP] = true;
        level.map[STEP] = Terrain.TRAP;
        trap = new WornDartTrap();
        trap.pos = STEP;
        trap.visible = true;
        trap.active = true;
        level.traps.put(STEP, trap);
        hero = new Hero();
        hero.pos = 12;
    }

    private boolean warns() {
        return TrapStepPermit.needsWarning(true, level, hero, STEP);
    }

    @Test public void visibleIdentifiedActiveTrapStillWarns() {
        assertTrue(warns());
        trap.visible = false;
        assertFalse(warns());
        trap.visible = true;
        level.heroFOV[STEP] = false;
        assertFalse(warns());
        level.heroFOV[STEP] = true;
        level.map[STEP] = Terrain.EMPTY;
        assertFalse(warns());
        level.map[STEP] = Terrain.TRAP;
        assertTrue(warns());
    }

    @Test public void missingTrapCollectionDoesNotCrashOrInventWarning() {
        SparseArray<com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap> original = level.traps;
        level.traps = null;
        assertFalse(warns());
        level.traps = original;
        assertTrue(warns());
    }

    @Test public void missingOrTruncatedVisibilityDoesNotCrashOrInventWarning() {
        boolean[] original = level.heroFOV;
        level.heroFOV = null;
        assertFalse(warns());
        level.heroFOV = new boolean[STEP];
        assertFalse(warns());
        level.heroFOV = original;
        assertTrue(warns());
    }

    @Test public void missingOrTruncatedTerrainDoesNotCrashOrInventWarning() {
        int[] original = level.map;
        level.map = null;
        assertFalse(warns());
        level.map = new int[STEP];
        assertFalse(warns());
        level.map = original;
        assertTrue(warns());
    }
}
