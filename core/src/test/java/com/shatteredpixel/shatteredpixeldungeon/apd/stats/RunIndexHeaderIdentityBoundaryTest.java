package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

/** Derived history must identify the physical log, not a different ID in an alias's header/cache. */
public class RunIndexHeaderIdentityBoundaryTest {

    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private StatsStore storeWithOriginal() throws Exception {
        StatsStore store = new StatsStore(temp.newFolder());
        RunLogWriter writer = store.startRun("original", "test", 123);
        writer.event(LogTestSupport.T_PLAIN).clock(7).unsigned(1).emit();
        writer.close();
        return store;
    }

    @Test
    public void copiedLogWithDifferentHeaderIdCannotImpersonateOriginalDuringRebuild() throws Exception {
        StatsStore store = storeWithOriginal();
        File source = store.logFile("original");
        File alias = new File(store.runsDir(), "alias.apdlog");
        byte[] originalBytes = LogTestSupport.bytes(source);
        Files.copy(source.toPath(), alias.toPath());
        store.index().rebuild();

        RunIndexEntry original = store.index().get("original");
        RunIndexEntry aliasEntry = store.index().get("alias");
        assertNotNull(original);
        assertEquals(RunIndexEntry.Status.SEALED, original.status);
        assertNotNull(aliasEntry);
        assertEquals("alias", aliasEntry.runId);
        assertEquals("alias.apdlog", aliasEntry.logFile);
        assertEquals(RunIndexEntry.Status.UNREADABLE, aliasEntry.status);
        assertEquals(2, store.index().size());
        assertTrue(Arrays.equals(originalBytes, LogTestSupport.bytes(source)));
        assertTrue(Arrays.equals(originalBytes, LogTestSupport.bytes(alias)));
    }

    @Test
    public void refreshKeepsCopiedLogUnderItsOwnFilenameIdentity() throws Exception {
        StatsStore store = storeWithOriginal();
        File source = store.logFile("original");
        File alias = new File(store.runsDir(), "alias.apdlog");
        byte[] originalBytes = LogTestSupport.bytes(source);
        Files.copy(source.toPath(), alias.toPath());
        RunIndexEntry aliasEntry = store.index().refresh("alias");

        assertEquals("alias", aliasEntry.runId);
        assertEquals("alias.apdlog", aliasEntry.logFile);
        assertEquals(RunIndexEntry.Status.UNREADABLE, aliasEntry.status);
        assertNotNull(store.index().get("original"));
        assertEquals(2, store.index().size());
        store.index().save();
        RunIndex reload = RunIndex.load(new File(store.root(), "index.apdi"), store.runsDir());
        assertFalse(reload.wasRebuilt());
        assertEquals(RunIndexEntry.Status.UNREADABLE, reload.get("alias").status);
        assertTrue(Arrays.equals(originalBytes, LogTestSupport.bytes(source)));
        assertTrue(Arrays.equals(originalBytes, LogTestSupport.bytes(alias)));
    }

    @Test
    public void checksumValidCachedEntryWithMismatchedRunIdRebuildsFromOriginalLog() throws Exception {
        StatsStore store = storeWithOriginal();
        File source = store.logFile("original");
        byte[] originalBytes = LogTestSupport.bytes(source);
        RunIndexEntry original = store.index().get("original");
        assertNotNull(original);
        // save() computes a genuine CRC, so this is a checksum-valid but false cache.
        store.index().upsert(new RunIndexEntry("phantom", original.telemetryVersion,
                original.newerThanReader, "original.apdlog", original.createdAtMillis,
                original.status, original.eventCount, original.lastClock,
                original.validBytes, original.fileLength));
        store.index().save();

        RunIndex loaded = RunIndex.load(new File(store.root(), "index.apdi"), store.runsDir());
        assertTrue("a false derived identity must be rebuilt", loaded.wasRebuilt());
        assertEquals(1, loaded.size());
        assertNotNull(loaded.get("original"));
        assertNull(loaded.get("phantom"));
        assertTrue(Arrays.equals(originalBytes, LogTestSupport.bytes(source)));
    }
}
