package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class RunLogCloseCompletionTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    @Test public void timeoutAndRepeatedCloseDoNotClaimQueuedWritesCompleted() throws Exception {
        CountDownLatch gate=new CountDownLatch(1);
        RunLogWriter.Options options=new RunLogWriter.Options();options.workerGate=gate;
        File file=new File(tmp.getRoot(),"pending.apdlog");
        RunLogWriter writer=RunLogWriter.create(file,TelemetryVersion.CURRENT,"pending","test",1,options);
        try {
            assertTrue(writer.append(123,0,1,new byte[]{7}));
            assertFalse("queued close is not completed disk writing",writer.close(true,10));
            assertFalse("repeated close is still pending",writer.close(true,10));
        } finally {
            gate.countDown();writer.workerThread().join(2000);
        }
        assertFalse(writer.workerThread().isAlive());
        assertTrue(writer.close());
        ReadResult recorded=RunLogReader.read(file);
        assertEquals(ReadStatus.SEALED,recorded.status);
        assertEquals(1,recorded.eventCount);
    }
}
