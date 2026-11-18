package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class RowMergerAnyProvenanceTest {
    private final DatasetDef def = new DatasetDef("x", "Items", "Items");
    public RowMergerAnyProvenanceTest() {
        def.add(ColumnDef.enumeration("item.equipped", "Ever equipped").merge(ColumnDef.Merge.ANY));
    }
    private Row part(int value, int provenance) {
        Row r = new Row("run/item", "run/item", "run", 1);
        r.setNum(0, value, provenance);
        return r;
    }
    @Test public void falsePreservesTheWeakestKnownSegmentProvenance() {
        for (int p : new int[]{Fact.PROV_RECORDED, Fact.PROV_LEGACY,
                Fact.PROV_INFERRED, Fact.PROV_INCOMPLETE}) {
            Row first = part(0, Fact.PROV_RECORDED), other = part(0, p);
            for (java.util.List<Row> parts : Arrays.asList(Arrays.asList(first, other), Arrays.asList(other, first))) {
                Row merged = RowMerger.merge(def, parts);
                assertTrue(merged.isKnown(0));
                assertEquals(0, merged.num(0));
                assertEquals("Merging must not promote uncertain history to recorded truth", p, merged.prov(0));
                assertEquals(p, other.prov(0));
            }
        }
    }
    @Test public void incompleteNeverEquippedDoesNotBecomeAnExactRateObservation() {
        Row uncertain = RowMerger.merge(def, Arrays.asList(part(0, Fact.PROV_RECORDED), part(0, Fact.PROV_INCOMPLETE)));
        AggValue rate = QueryEngine.evaluate(def, Query.of("x"), Aggregate.rate("item.equipped", 1),
                Arrays.asList(uncertain, part(1, Fact.PROV_RECORDED)));
        assertTrue(rate.known);
        assertEquals(1, rate.contributing);
        assertEquals(2, rate.total);
        assertEquals(1.0, rate.value, 0);
        Row unknown = new Row("run/item", "run/item", "run", 1);
        assertFalse(RowMerger.merge(def, Arrays.asList(part(0, Fact.PROV_RECORDED), unknown)).isKnown(0));
        assertEquals(1, RowMerger.merge(def, Arrays.asList(part(1, Fact.PROV_RECORDED), unknown)).num(0));
    }
    @Test public void realPartialItemLogsKeepUncertaintyThroughColdAndCachedCatalogLoads() throws Exception {
        java.nio.file.Path root = java.nio.file.Files.createTempDirectory("apd-any-provenance-");
        try {
            java.nio.file.Files.createDirectory(root.resolve("runs"));
            java.util.List<byte[]> originals = new java.util.ArrayList<>();
            for (int segment = 0; segment < 2; segment++) {
                java.io.File file = root.resolve("runs/segment-" + segment + ".apdlog").toFile();
                com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter writer =
                        com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter.create(file,
                                com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion.CURRENT, "fixture", "test", segment + 1);
                try {
                    com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.RunStart start =
                            new com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.RunStart();
                    start.runId = "same-run"; start.segment = segment; start.rulesetId = "fixture";
                    assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.emitRunStart(writer, 0, segment == 1, start));
                    com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.Seen seen =
                            new com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.Seen();
                    seen.uid = "same-item";
                    seen.state.name = "leather armor";
                    seen.state.category = com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.CAT_ARMOR;
                    assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.emitSeen(writer, 1, 0, seen));
                } finally { writer.close(); }
                originals.add(java.nio.file.Files.readAllBytes(file.toPath()));
            }
            for (int load = 0; load < 2; load++) {
                Catalog catalog = new DatasetStore(root.toFile()).load();
                assertEquals(load == 0 ? 2 : 0, catalog.logsRebuilt);
                assertEquals(load == 0 ? 0 : 2, catalog.logsCached);
                Dataset items = catalog.dataset(Datasets.ITEMS);
                assertEquals(1, items.rows.size());
                int column = items.def.indexOf("item.equipped");
                assertEquals(0, items.rows.get(0).num(column));
                assertEquals(Fact.PROV_INCOMPLETE, items.rows.get(0).prov(column));
                assertFalse(QueryEngine.evaluate(items.def, Query.of(Datasets.ITEMS),
                        Aggregate.rate("item.equipped", 1), items.rows).known);
            }
            for (int segment = 0; segment < 2; segment++)
                assertArrayEquals(originals.get(segment), java.nio.file.Files.readAllBytes(root.resolve("runs/segment-" + segment + ".apdlog")));
        } finally {
            try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(root)) {
                for (java.nio.file.Path path : files.sorted(java.util.Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList()))
                    java.nio.file.Files.delete(path);
            }
        }
    }
}
