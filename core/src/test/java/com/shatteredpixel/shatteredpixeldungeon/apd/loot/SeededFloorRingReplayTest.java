package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * The seeded ring deck alone does not establish which ring actually appears
 * on a floor. Rebuild real dungeon floors on six independent new-run
 * initializations and compare the position and concrete type of every ring
 * inside a generated heap. This deliberately does not assert that an owner's
 * reported pickup has been reproduced without its exact seed and settings.
 */
public class SeededFloorRingReplayTest {

    @Test public void sixNewRunsKeepGroundRingTypesAndLocationsForIdenticalSeedAndContext()
            throws Exception {
        HeadlessTestSupport.start();
        int oldChallenges = SPDSettings.challenges();
        int oldDifficulty = SPDSettings.apdDifficulty();
        boolean oldIntro = SPDSettings.intro();
        BiConsumer<Integer, Level> oldObserver = LevelGenerationHarness.levelObserver;
        try {
            // Two established golden-fixture seeds, not an invented owner
            // reproduction. A replay is one full Dungeon.init/newLevel cycle.
            for (long seed : new long[]{12345L, 987654321L}) {
                List<String> baseline = null;
                for (int replay = 0; replay < 6; replay++) {
                    List<String> found = new ArrayList<>();
                    LevelGenerationHarness.levelObserver = (depth, level) -> {
                        for (Heap heap : level.heaps.valueList()) {
                            for (Item item : heap.items) {
                                if (item instanceof Ring) {
                                    found.add(depth + ":" + heap.pos + ":"
                                            + item.getClass().getName());
                                }
                            }
                        }
                    };
                    SPDSettings.apdDifficulty(3);
                    LevelGenerationHarness.generate(seed, 0, HeroClass.WARRIOR,
                            LevelGenerationHarness.Context.RETURNING, 8);
                    Collections.sort(found);
                    if (baseline == null) {
                        assertFalse("fixture seed " + seed
                                + " generated no observable ground rings", found.isEmpty());
                        baseline = found;
                    } else {
                        assertEquals("same-seed real-floor ring identity/location changed"
                                + " for seed " + seed + " on attempt " + (replay + 1),
                                baseline, found);
                    }
                }
            }
        } finally {
            LevelGenerationHarness.levelObserver = oldObserver;
            SPDSettings.challenges(oldChallenges);
            SPDSettings.apdDifficulty(oldDifficulty);
            SPDSettings.intro(oldIntro);
        }
    }
}
