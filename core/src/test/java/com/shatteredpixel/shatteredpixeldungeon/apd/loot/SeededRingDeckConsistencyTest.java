package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.watabou.utils.Random;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A reproducible seed must not silently turn the same ordinal seeded ring
 * drop into a different ring merely because incidental RNG or non-deck
 * generation happened before the drop.
 *
 * This verifies the ring category's seeded deck, not an entire floor layout.
 * A ground-item mismatch still requires an exact level/edition reproduction.
 */
public class SeededRingDeckConsistencyTest {

    private static final long RUN_SEED = 123456789L;
    private static final int RING_DROPS = 16;

    private static List<Class<? extends Ring>> ringTypes(int replay) {
        // Same initial per-run seed context that Generator.fullReset sees
        // in Dungeon.init, without touching player saves or persisted history.
        Random.pushGenerator(RUN_SEED + 1);
        try {
            Generator.fullReset();
        } finally {
            Random.popGenerator();
        }

        List<Class<? extends Ring>> types = new ArrayList<>();
        for (int drop = 0; drop < RING_DROPS; drop++) {
            // The non-deck path and incidental random calls may occur in
            // different orders without advancing a seeded ring deck.
            for (int i = 0; i < replay * (drop + 1); i++) {
                Random.Long();
            }
            if (replay > 0 && drop % 3 == 0) {
                assertTrue(Generator.randomUsingDefaults(Generator.Category.RING) instanceof Ring);
            }
            Ring generated = (Ring) Generator.random(Generator.Category.RING);
            types.add(generated.getClass());
        }
        return types;
    }

    @Test public void sixReplaysKeepTheSameRingTypeSequenceDespiteIncidentalRng() {
        HeadlessTestSupport.start();
        Random.pushGenerator(0x51eedL);
        try {
            List<Class<? extends Ring>> first = ringTypes(0);
            assertEquals(RING_DROPS, first.size());
            for (int replay = 1; replay < 6; replay++) {
                assertEquals("Seeded ring deck changed on replay " + (replay + 1),
                        first, ringTypes(replay));
            }
        } finally {
            Random.popGenerator();
        }
    }
}
