package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Reproducible adversarial workload for the *derived* UIMSD row cache.
 * This never touches a player's history or rewrites a saved run.
 */
public class RowCacheRandomizedIntegrityTest {

    private static final long SEED = 0x41504455494D5344L;
    private static final String[] TEXT = {
            "", "ordinary", "épée 日本語 🗡", "pipe|semicolon;tab\t",
            "line one\nline two", "zero\u0000inside", "🔎 repeated 🔎"
    };

    private static Map<String, DatasetDef> definitions(DatasetDef definition) {
        Map<String, DatasetDef> result = new LinkedHashMap<>();
        result.put(definition.id, definition);
        return result;
    }

    private static String ruleset(int choice) {
        switch (choice % 3) {
            case 0: return null;
            case 1: return Row.RULESET_MIXED;
            default: return "difficulty:3|challenge:4";
        }
    }

    private static LogRows generated(DatasetDef def, Random random, int batch, int count) {
        LogRows source = new LogRows();
        LogMeta meta = source.meta;
        meta.logId = "log-" + batch;
        meta.runId = "run-" + batch;
        meta.telemetry = "v1.4";
        meta.createdAtMillis = 1_700_000_000_000L + batch;
        meta.segment = batch % 9;
        meta.hasRunEnd = (batch & 1) == 0;
        meta.imported = (batch & 2) != 0;
        meta.unreadable = (batch & 4) != 0;
        meta.ruleset = ruleset(batch);
        meta.rankingId = "ranking-" + batch;

        List<Row> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Row row = new Row("row-" + batch + "-" + i,
                    i % 4 == 0 ? null : "merge-" + (i % 17),
                    "run-" + batch + "-" + (i % 11), def.size());
            row.finished = i % 3 != 0;
            row.publicRow = i % 5 != 0;
            row.imported = i % 7 == 0;
            row.duplicate = i % 13 == 0;
            row.ruleset = ruleset(i + batch);
            row.telemetry = "v" + (i % 5);
            for (int c = 0; c < def.size(); c++) {
                int kind = (i + c + batch) % 3;
                int provenance = random.nextInt(64);
                if (kind == Row.K_NUM) {
                    long number = (i % 17 == 0) ? Long.MIN_VALUE :
                            (i % 19 == 0 ? Long.MAX_VALUE : random.nextLong());
                    row.setNum(c, number, provenance);
                } else if (kind == Row.K_TEXT) {
                    row.setText(c, TEXT[random.nextInt(TEXT.length)], provenance);
                }
            }
            rows.add(row);
        }
        source.rows.put(def.id, rows);
        return source;
    }

    private static void assertMeta(LogMeta expected, LogMeta actual) {
        assertEquals(expected.logId, actual.logId);
        assertEquals(expected.runId, actual.runId);
        assertEquals(expected.telemetry, actual.telemetry);
        assertEquals(expected.createdAtMillis, actual.createdAtMillis);
        assertEquals(expected.segment, actual.segment);
        assertEquals(expected.hasRunEnd, actual.hasRunEnd);
        assertEquals(expected.imported, actual.imported);
        assertEquals(expected.unreadable, actual.unreadable);
        assertEquals(expected.ruleset, actual.ruleset);
        assertEquals(expected.rankingId, actual.rankingId);
    }

    private static void assertRow(Row expected, Row actual, int[] columns) {
        assertEquals(expected.id, actual.id);
        assertEquals(expected.mergeKey, actual.mergeKey);
        assertEquals(expected.runId, actual.runId);
        assertEquals(expected.finished, actual.finished);
        assertEquals(expected.publicRow, actual.publicRow);
        assertEquals(expected.imported, actual.imported);
        assertEquals(expected.duplicate, actual.duplicate);
        assertEquals(expected.ruleset, actual.ruleset);
        assertEquals(expected.telemetry, actual.telemetry);
        for (int i = 0; i < columns.length; i++) {
            int from = columns[i];
            assertEquals("cell kind " + expected.id + ":" + from, expected.kind(from), actual.kind(i));
            if (expected.kind(from) != Row.K_UNKNOWN) {
                assertEquals("provenance " + expected.id + ":" + from, expected.prov(from), actual.prov(i));
                if (expected.kind(from) == Row.K_NUM) {
                    assertEquals("number " + expected.id + ":" + from, expected.num(from), actual.num(i));
                } else {
                    assertEquals("text " + expected.id + ":" + from, expected.text(from), actual.text(i));
                }
            }
        }
    }

    @Test public void seededLargeAndSmallCorporaPreserveExactRowValuesAndMetadata() {
        DatasetDef def = QuerySupport.def();
        Map<String, DatasetDef> defs = definitions(def);
        Random random = new Random(SEED);
        int[] allColumns = {0, 1, 2, 3, 4, 5};
        // A 10,000-row log followed by 64 varied smaller logs. Fixed seed is replayable.
        for (int batch = 0; batch < 65; batch++) {
            int count = batch == 0 ? 10_000 : 1 + random.nextInt(120);
            LogRows source = generated(def, random, batch, count);
            long sourceLength = 8192L + batch * 137L;
            byte[] fingerprint = null;
            if ((batch & 1) == 0) {
                fingerprint = new byte[32];
                random.nextBytes(fingerprint);
            }
            byte[] encoded = RowCodec.encode(source, sourceLength, defs, fingerprint);
            LogRows decoded = RowCodec.decode(encoded, sourceLength, defs, fingerprint);
            assertNotNull("valid cache batch " + batch, decoded);
            assertMeta(source.meta, decoded.meta);
            List<Row> expected = source.rows.get(def.id), actual = decoded.rows.get(def.id);
            assertNotNull(actual);
            assertEquals("row count batch " + batch, expected.size(), actual.size());
            for (int i = 0; i < expected.size(); i++) assertRow(expected.get(i), actual.get(i), allColumns);
            assertArrayEquals("stable re-encoding batch " + batch,
                    encoded, RowCodec.encode(decoded, sourceLength, defs, fingerprint));
            assertNull("wrong raw-log length must miss batch " + batch,
                    RowCodec.decode(encoded, sourceLength + 1, defs, fingerprint));
            if (fingerprint != null) {
                byte[] wrong = fingerprint.clone();
                wrong[0] ^= 1;
                assertNull("wrong source fingerprint must miss batch " + batch,
                        RowCodec.decode(encoded, sourceLength, defs, wrong));
            }
        }
    }

    @Test public void deletedAndReorderedColumnsDoNotShiftKnownCells() {
        DatasetDef original = QuerySupport.def();
        LogRows source = generated(original, new Random(SEED), 71, 2048);
        Map<String, DatasetDef> oldSchema = definitions(original);
        byte[] fingerprint = new byte[32];
        new Random(SEED + 1).nextBytes(fingerprint);
        byte[] encoded = RowCodec.encode(source, 9001, oldSchema, fingerprint);

        // The reader now has only two columns, in the opposite order from the writer.
        DatasetDef projected = new DatasetDef("things", "Things", "schema projection");
        projected.add(ColumnDef.text("note", "Note"));
        projected.add(ColumnDef.num("score", "Score"));
        LogRows decoded = RowCodec.decode(encoded, 9001, definitions(projected), fingerprint);
        assertNotNull(decoded);
        assertMeta(source.meta, decoded.meta);
        List<Row> expected = source.rows.get(original.id), actual = decoded.rows.get(projected.id);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertRow(expected.get(i), actual.get(i), new int[]{QuerySupport.NOTE, QuerySupport.SCORE});
        }
        // A single altered byte must invalidate the checksum even in a removed column.
        byte[] damaged = encoded.clone();
        damaged[damaged.length / 2] ^= 1;
        assertNull(RowCodec.decode(damaged, 9001, definitions(projected), fingerprint));
    }
}
