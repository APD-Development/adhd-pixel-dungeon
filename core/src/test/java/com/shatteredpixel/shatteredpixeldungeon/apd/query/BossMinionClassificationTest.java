package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class BossMinionClassificationTest {
    @Rule public TemporaryFolder history = new TemporaryFolder();
    @Test public void actualYogMinionsAreNotBosses() {
        for (Class<?> minion : new Class<?>[]{YogDzewa.Larva.class, YogDzewa.YogRipper.class,
                YogDzewa.YogEye.class, YogDzewa.YogScorpio.class})
            assertFalse(minion.getName(), BossClasses.isBoss(minion.getName()));
    }
    @Test public void everyActualFistRemainsABoss() {
        for (Class<?> fist : new Class<?>[]{YogFist.BurningFist.class, YogFist.SoiledFist.class,
                YogFist.RottingFist.class, YogFist.RustedFist.class, YogFist.BrightFist.class, YogFist.DarkFist.class})
            assertTrue(fist.getName(), BossClasses.isBoss(fist.getName()));
    }
    @Test public void completedHistoryRebuildsOldBossClassificationWithoutChangingLogs() throws Exception {
        SyntheticLog log = new SyntheticLog(history.getRoot(), "boss-minion", new TelemetryVersion(1,4), 100);
        log.start("run-minion", 0, "WARRIOR", "r1-aaaaaaaaaaaa", 0, 30, false)
            .heroAttacks("larva", YogDzewa.Larva.class.getName(), true)
            .end(LifecycleEvents.OUTCOME_DIED, "larva", 25, 1, 0).close(true);
        byte[] original = Files.readAllBytes(log.file.toPath());
        DatasetStore store = new DatasetStore(history.getRoot());
        assertNotBoss(store.load());
        File[] caches = store.rowsDir().listFiles((dir,name) -> name.endsWith(".apdr"));
        assertNotNull(caches); assertEquals(1, caches.length);
        byte[] old = com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore.plainCache(Files.readAllBytes(caches[0].toPath()));
        old[5] = 8; // Persisted TEST50 row-builder revision, before corrected classification.
        Files.write(caches[0].toPath(), old);
        Catalog rebuilt = store.load();
        assertEquals("old derived cache is rebuilt", 1, rebuilt.logsRebuilt);
        assertNotBoss(rebuilt);
        assertArrayEquals("raw history is never rewritten", original, Files.readAllBytes(log.file.toPath()));
    }
    private static void assertNotBoss(Catalog catalog) {
        Dataset enemies = catalog.dataset(Datasets.ENEMIES);
        assertEquals(1, enemies.rows.size());
        int boss = enemies.def.indexOf("enemy.boss");
        assertTrue(enemies.rows.get(0).isKnown(boss));
        assertEquals(0, enemies.rows.get(0).num(boss));
    }
}
