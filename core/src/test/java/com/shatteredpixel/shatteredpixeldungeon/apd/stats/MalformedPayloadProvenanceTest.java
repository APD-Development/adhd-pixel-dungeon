package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import static org.junit.Assert.*;

/** Valid framing must not certify unreadable typed-event payloads as complete. */
public class MalformedPayloadProvenanceTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private int sequence;

    private byte[] log() throws Exception {
        String id = "payload-" + sequence++;
        File file = new File(tmp.getRoot(), id + ".apdlog");
        RunLogWriter w = RunLogWriter.create(file, TelemetryVersion.CURRENT, id, "test", 1);
        LifecycleEvents.RunStart start = new LifecycleEvents.RunStart();
        start.runId = id; start.heroClass = "WARRIOR";
        assertTrue(LifecycleEvents.emitRunStart(w, 0, false, start));
        ItemEvents.Registered registered = new ItemEvents.Registered(); registered.uid = "u1";
        assertTrue(ItemEvents.emitRegistered(w, 1, 0, registered));
        ItemEvents.Acquired acquired = new ItemEvents.Acquired(); acquired.uid = "u1";
        assertTrue(ItemEvents.emitAcquired(w, 1, 0, acquired));
        ItemEvents.Equip equip = new ItemEvents.Equip(); equip.uid = "u1";
        assertTrue(ItemEvents.emitEquip(w, 2, 0, true, equip));
        CombatEvents.Attack attack = new CombatEvents.Attack();
        attack.attacker.kind = CombatEvents.KIND_HERO;
        attack.defender.kind = CombatEvents.KIND_ENEMY; attack.defender.name = "Rat"; attack.hit = true;
        assertTrue(CombatEvents.emitAttack(w, 3, attack));
        assertTrue(w.close(false));
        return Files.readAllBytes(file.toPath());
    }

    private void check(byte[] bytes, int expected) {
        byte[] original = bytes.clone();
        ReadResult read = RunLogReader.parse(bytes);
        assertEquals(ReadStatus.UNSEALED, read.status);
        assertEquals(0, read.gapEvents);
        RunFacts facts = RunFactsBuilder.fromLog(read, "facts");
        assertEquals(1, facts.numOr(FactKeys.HERO_ATTACKS, -1));
        assertEquals(expected, facts.get(FactKeys.HERO_ATTACKS).prov);
        assertFalse("Unreadable payloads must not pretend the run started mid-run", facts.isKnown(FactKeys.PARTIAL));
        LogRows rows = RowBuilders.fromLog(read, "rows");
        DatasetDef items = RowBuilders.def(Datasets.ITEMS), enemies = RowBuilders.def(Datasets.ENEMIES);
        assertEquals(1, rows.of(Datasets.ITEMS).size());
        Row item = rows.of(Datasets.ITEMS).get(0);
        assertEquals(1, item.num(items.indexOf("item.equipCount")));
        assertEquals("Item totals must retain the run's missing-event warning", expected, item.prov(items.indexOf("item.equipCount")));
        assertEquals(1, rows.of(Datasets.ENEMIES).size());
        Row enemy = rows.of(Datasets.ENEMIES).get(0);
        assertEquals(1, enemy.num(enemies.indexOf("enemy.attacksByHero")));
        assertEquals("Enemy totals must retain the run's missing-event warning", expected, enemy.prov(enemies.indexOf("enemy.attacksByHero")));
        assertArrayEquals("Derivation must never rewrite raw history", original, bytes);
    }

    private byte[] damaged(int type) throws Exception {
        byte[] clean = log(), bad = LogFormat.eventFrame(type, 0, 1, new byte[0]);
        byte[] joined = Arrays.copyOf(clean, clean.length + bad.length);
        System.arraycopy(bad, 0, joined, clean.length, bad.length);
        return joined;
    }

    @Test public void healthyTypedEventsKeepExactCounts() throws Exception { check(log(), Fact.PROV_RECORDED); }
    @Test public void unreadableAttackMakesItemAndEnemyTotalsLowerBounds() throws Exception { check(damaged(EventTypes.ATTACK), Fact.PROV_INCOMPLETE); }
    @Test public void unreadableEquipmentEventMakesItemAndEnemyTotalsLowerBounds() throws Exception { check(damaged(EventTypes.ITEM_EQUIPPED), Fact.PROV_INCOMPLETE); }

    @Test public void previousBuilderCacheIsRebuiltWithoutChangingHistory() throws Exception {
        byte[] original = damaged(EventTypes.ATTACK);
        File root = tmp.newFolder(), runs = new File(root, "runs"); assertTrue(runs.mkdir());
        File file = new File(runs, "cache-history.apdlog"); Files.write(file.toPath(), original);
        DatasetStore store = new DatasetStore(root);
        assertEquals(1, store.load().logsRebuilt);
        File cache = new File(store.rowsDir(), "runs-cache-history.apdr");
        byte[] stored = Files.readAllBytes(cache.toPath());
        // Row caches are stored compressed; an older plain cache is still accepted.
        assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore.isGzip(stored));
        byte[] bytes = com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore.gunzip(stored);
        assertEquals(18, bytes[5]); bytes[5] = 17;
        java.util.zip.CRC32 crc = new java.util.zip.CRC32(); crc.update(bytes, 0, bytes.length - 4);
        java.nio.ByteBuffer.wrap(bytes, bytes.length - 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt((int) crc.getValue());
        Files.write(cache.toPath(), bytes);
        Catalog rebuilt = store.load(); assertEquals(0, rebuilt.logsCached); assertEquals(1, rebuilt.logsRebuilt);
        Dataset items = rebuilt.dataset(Datasets.ITEMS), enemies = rebuilt.dataset(Datasets.ENEMIES);
        assertEquals(Fact.PROV_INCOMPLETE, items.rows.get(0).prov(items.def.indexOf("item.equipCount")));
        assertEquals(Fact.PROV_INCOMPLETE, enemies.rows.get(0).prov(enemies.def.indexOf("enemy.attacksByHero")));
        assertEquals(1, store.load().logsCached);
        assertArrayEquals(original, Files.readAllBytes(file.toPath()));
    }
}

