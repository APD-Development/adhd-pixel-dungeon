/* ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.TalentExplanation;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regression coverage for the two still-partial normal/detail talent descriptions.
 * Existing SupplyRation/custom-healing and floor-generation runtime tests remain
 * in the complete suite; these checks do not replace them with copied formulas.
 */
public class RogueTalentDescriptionTest {
    private Hero previous;
    private Hero hero;

    @Before public void setup() {
        HeadlessTestSupport.start();
        previous = Dungeon.hero;
        hero = new Hero();
        hero.heroClass = HeroClass.ROGUE;
        Talent.initClassTalents(hero);
        hero.HP = 7;
        hero.HT = 20;
        hero.apdCustomHealingFraction = .75f;
        Dungeon.hero = hero;
    }

    @After public void restore() { Dungeon.hero = previous; }

    @Test public void cachedRationsNormalTextQualifiesHealingAndRunWideLimit() {
        String text = Talent.CACHED_RATIONS.desc();
        assertTrue(text, text.contains("_2 supply rations_"));
        assertTrue(text, text.contains("_3 rations_"));
        assertTrue(text, text.contains("including any already placed"));
        assertTrue(text, text.contains("normally restore up to 5 HP"));
        assertTrue(text, text.contains("no HP when healing is disabled"));
        assertTrue(text, text.contains("1 charge"));
        assertTrue(text, text.contains("up to capacity"));
        assertTrue(text, text.contains("floors already generated"));
    }

    @Test public void foresightNormalTextStatesEligibilityAndStableRoll() {
        String text = Talent.ROGUES_FORESIGHT.desc();
        assertTrue(text, text.contains("_75% chance_"));
        assertTrue(text, text.contains("_100%_"));
        assertTrue(text, text.contains("regular main-dungeon floors"));
        assertTrue(text, text.contains("at least one secret room"));
        assertTrue(text, text.contains("at least two on a secrets-feeling floor"));
        assertTrue(text, text.contains("revisiting does not reroll"));
        assertTrue(text, text.contains("does not reveal an entrance"));
        assertTrue(text, text.contains("no hint does not prove"));
    }

    @Test public void bothRankDetailsRemainConciseWithoutDroppingConditions() {
        for (int rank = 1; rank <= 2; rank++) {
            String ration = TalentExplanation.details(Talent.CACHED_RATIONS, rank);
            assertTrue(ration, ration.startsWith("_Cached Rations_"));
            assertTrue(ration, ration.contains("_" + (rank + 1) + " supply rations_"));
            assertTrue(ration, ration.contains("including rations already placed"));
            assertTrue(ration, ration.contains("suitable special-room tile"));
            assertTrue(ration, ration.contains("no HP when healing is disabled"));
            String foresight = TalentExplanation.details(Talent.ROGUES_FORESIGHT, rank);
            assertTrue(foresight, foresight.contains("_" + (rank == 1 ? 75 : 100) + "% chance_"));
            assertTrue(foresight, foresight.contains("at least two on a secrets-feeling floor"));
            assertTrue(foresight, foresight.contains("revisiting does not reroll"));
            assertTrue(foresight, foresight.contains("No hint does not prove"));
            assertTrue("Ration detail should remain readable", ration.length() < 650);
            assertTrue("Foresight detail should remain readable", foresight.length() < 500);
        }
    }

    @Test public void inspectionDoesNotAlterHeroOrRngAndNeedsNoLoadedRun() {
        float time = hero.cooldown();
        int items = hero.belongings.backpack.items.size();
        String talents = hero.talents.toString();
        Random.pushGenerator(433110);
        long expected;
        try { expected = Random.Long(); } finally { Random.popGenerator(); }
        Random.pushGenerator(433110);
        try {
            for (Talent talent : new Talent[]{Talent.CACHED_RATIONS, Talent.ROGUES_FORESIGHT}) {
                for (int rank = 1; rank <= 2; rank++) {
                    assertFalse(TalentExplanation.details(talent, rank).isEmpty());
                    assertFalse(talent.desc().isEmpty());
                }
                assertEquals("", TalentExplanation.details(talent, 0));
            }
            assertEquals(expected, Random.Long());
        } finally { Random.popGenerator(); }
        assertEquals(7, hero.HP);
        assertEquals(20, hero.HT);
        assertEquals(.75f, hero.apdCustomHealingFraction, 0f);
        assertEquals(time, hero.cooldown(), 0f);
        assertEquals(items, hero.belongings.backpack.items.size());
        assertEquals(talents, hero.talents.toString());
        Dungeon.hero = null;
        for (Talent talent : new Talent[]{Talent.CACHED_RATIONS, Talent.ROGUES_FORESIGHT}) {
            assertFalse(TalentExplanation.details(talent, 2).isEmpty());
            assertFalse(talent.desc().isEmpty());
        }
    }
}
