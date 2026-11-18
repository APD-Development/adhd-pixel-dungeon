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
import java.util.Arrays;
import java.util.List;

/** Recovery must not trim an APD log that belongs to a different on-disk run identity. */
public class StatsStoreRecoveryHeaderIdentityBoundaryTest {

    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private LogRecovery.RepairReport reportFor(List<LogRecovery.RepairReport> reports, File file){
        for (LogRecovery.RepairReport report : reports){
            if (report.file.equals(file)) return report;
        }
        throw new AssertionError("missing recovery report for " + file);
    }

    @Test
    public void damagedLogCopiedUnderAnotherValidRunIdIsNotTruncated() throws Exception {
        StatsStore store = new StatsStore(temp.newFolder("mismatched-damaged-header"));
        RunLogWriter writer = store.startRun("original", "test", 101);
        for (int i = 0; i < 9; i++){
            writer.event(LogTestSupport.T_PLAIN).clock(i).unsigned(i).emit();
        }
        writer.close(false);

        File original = store.logFile("original");
        byte[] complete = LogTestSupport.bytes(original);
        byte[] damaged = Arrays.copyOf(complete, complete.length - 2);
        assertEquals(ReadStatus.TRUNCATED, RunLogReader.parse(damaged).status);
        LogTestSupport.write(original, damaged);
        File copied = store.logFile("copied");
        LogTestSupport.write(copied, damaged);

        List<LogRecovery.RepairReport> reports = store.recoverAll();
        assertEquals(2, reports.size());
        LogRecovery.RepairReport legitimate = reportFor(reports, original);
        assertTrue("genuinely named damaged run must still repair", legitimate.truncated);
        assertNotNull(legitimate.savedTail);
        assertNull(legitimate.error);
        assertEquals(legitimate.validBytes, original.length());

        LogRecovery.RepairReport alias = reportFor(reports, copied);
        assertEquals(ReadStatus.TRUNCATED, alias.statusBefore);
        assertFalse("copied foreign header must not be repaired", alias.truncated);
        assertEquals(0, alias.discardedBytes);
        assertNull(alias.savedTail);
        assertNotNull("refused identity must be reported", alias.error);
        assertArrayEquals("copied run bytes must remain unchanged", damaged, LogTestSupport.bytes(copied));
        assertEquals(RunIndexEntry.Status.UNREADABLE, store.index().get("copied").status);
        assertNotNull(store.index().get("original"));
        for (File child : store.runsDir().listFiles()){
            assertFalse("copied log must not get a tail sidecar",
                    child.getName().startsWith("copied.apdlog.tail-"));
        }
    }

    @Test
    public void healthyCopiedLogStillRemainsUntouchedAndUnreadable() throws Exception {
        StatsStore store = new StatsStore(temp.newFolder("mismatched-healthy-header"));
        RunLogWriter writer = store.startRun("original", "test", 102);
        writer.event(LogTestSupport.T_PLAIN).clock(4).unsigned(3).emit();
        writer.close();

        File original = store.logFile("original");
        byte[] bytes = LogTestSupport.bytes(original);
        File copied = store.logFile("copied");
        LogTestSupport.write(copied, bytes);

        List<LogRecovery.RepairReport> reports = store.recoverAll();
        assertEquals(2, reports.size());
        assertFalse(reportFor(reports, original).truncated);
        LogRecovery.RepairReport alias = reportFor(reports, copied);
        assertFalse(alias.truncated);
        assertNotNull(alias.error);
        assertNull(alias.savedTail);
        assertArrayEquals(bytes, LogTestSupport.bytes(copied));
        assertArrayEquals(bytes, LogTestSupport.bytes(original));
        assertEquals(RunIndexEntry.Status.UNREADABLE, store.index().get("copied").status);
        assertEquals(RunIndexEntry.Status.SEALED, store.index().get("original").status);
    }
}
