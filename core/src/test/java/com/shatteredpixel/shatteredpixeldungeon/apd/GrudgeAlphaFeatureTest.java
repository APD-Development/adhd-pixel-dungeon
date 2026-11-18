/* ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
 * SPDX-License-Identifier: GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.apd;

import org.junit.Test;
import static org.junit.Assert.*;

/** Only Grudge is shared with approved Alpha Testers, not Developer-only controls. */
public class GrudgeAlphaFeatureTest {
    @Test public void alphaFeaturesAllowHiddenGrudgeWithoutDeveloperUnlocks(){
        ApdFeatures.parse("extendedPlayback\ngrudge\n");
        assertTrue(ApdFeatures.grudge());
        assertTrue(ApdFeatures.extendedPlayback());
        assertFalse(ApdFeatures.testBuild());
        assertFalse(ApdFeatures.demoUnlock());
        assertFalse(ApdFeatures.clericPlayable());
        ApdFeatures.resetForTest();
    }
    @Test public void ordinaryUnflaggedBuildStillGatesGrudge(){
        ApdFeatures.parse("grudge\n");
        assertFalse(ApdFeatures.grudge());
        ApdFeatures.parse("testBuild\ngrudge\ndemoUnlock\nclericPlayable\n");
        assertTrue(ApdFeatures.grudge());
        assertTrue(ApdFeatures.testBuild());
        ApdFeatures.resetForTest();
    }
}
