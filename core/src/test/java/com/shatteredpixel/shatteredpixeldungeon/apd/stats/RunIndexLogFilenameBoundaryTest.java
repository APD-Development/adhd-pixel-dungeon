package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

// A run-index cache is derived history. Invalid on-disk names must not create
// fake run identities or override the summaries of real logs.
public class RunIndexLogFilenameBoundaryTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void invalidLogFilenamesAreIgnoredWithoutChangingTheirBytes() throws Exception {
        File root = tmp.newFolder("invalid-stems");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdirs());
        byte[] original = new byte[]{1, 2, 3, 4};
        File[] invalid = {
                new File(runs, ".apdlog"),
                new File(runs, "bad name.apdlog"),
                new File(runs, "bad.name.apdlog"),
                new File(runs, "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.apdlog")
        };
        for (File file : invalid) LogTestSupport.write(file, original);
        RunIndex index = new StatsStore(root).index();
        assertEquals(0, index.size());
        for (File file : invalid) assertArrayEquals(file.getName(), original, LogTestSupport.bytes(file));
    }

    @Test
    public void validHeaderUnderInvalidFilenameCannotImpersonateItsRun() throws Exception {
        File genuine = LogTestSupport.writeLog(tmp.newFolder("genuine-source"), "source", 2, true);
        byte[] original = LogTestSupport.bytes(genuine);
        File root = tmp.newFolder("invalid-header-alias");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdirs());
        File alias = new File(runs, "invalid name.apdlog");
        LogTestSupport.write(alias, original);

        RunIndex index = new StatsStore(root).index();
        assertEquals(0, index.size());
        assertNull(index.get("source"));
        assertArrayEquals(original, LogTestSupport.bytes(alias));
    }

    @Test
    public void checksumValidLegacyCacheWithInvalidLogFilenameIsRebuilt() throws Exception {
        StatsStore store = new StatsStore(tmp.newFolder("old-index"));
        RunLogWriter writer = store.startRun("real", "test", 100);
        assertEquals(RunLogWriter.OpenStatus.OK, writer.openStatus());
        assertTrue(writer.close());
        store.refreshIndex("real");
        byte[] original = LogTestSupport.bytes(store.logFile("real"));

        // Simulate an older CRC-valid derived cache: the entry claims a log
        // filename that cannot correspond to an APD run ID.
        store.index().upsert(new RunIndexEntry("forged", null, false,
                "bad name.apdlog", 1, RunIndexEntry.Status.OPEN, 0, 0, 0, 1));
        store.index().save();
        RunIndex loaded = RunIndex.load(new File(store.root(), "index.apdi"), store.runsDir());
        assertTrue(loaded.wasRebuilt());
        assertEquals(1, loaded.size());
        assertNotNull(loaded.get("real"));
        assertNull(loaded.get("forged"));
        assertFalse(loaded.get("real").status == RunIndexEntry.Status.UNREADABLE);
        assertArrayEquals(original, LogTestSupport.bytes(store.logFile("real")));
    }

    @Test
    public void validFilenameWithUnreadableLogRemainsVisible() throws Exception {
        File root = tmp.newFolder("damaged-but-valid-name");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdirs());
        File damaged = new File(runs, "damaged.apdlog");
        byte[] original = new byte[]{7, 8, 9};
        LogTestSupport.write(damaged, original);
        RunIndex index = new StatsStore(root).index();
        assertEquals(1, index.size());
        assertEquals(RunIndexEntry.Status.UNREADABLE, index.get("damaged").status);
        assertEquals("damaged.apdlog", index.get("damaged").logFile);
        assertArrayEquals(original, LogTestSupport.bytes(damaged));
    }
}
