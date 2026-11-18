package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import java.io.File;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class RunIndexCountValidationTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test public void unsignedCountOutsideSignedRangeRebuildsFromLogs() throws Exception {
        File root=tmp.newFolder();
        File runs=new File(root,"runs");assertTrue(runs.mkdir());
        byte[] original={3,1,4,1,5};
        File log=new File(runs,"preserved.apdlog");Files.write(log.toPath(),original);
        ByteSink body=new ByteSink().writeBytes(RunIndex.MAGIC).writeByte(RunIndex.FORMAT);
        // A syntactically valid unsigned 64-bit value that becomes negative as a Java long.
        for(int i=0;i<9;i++)body.writeByte(255);
        body.writeByte(1);
        body.writeInt32((int)LogFormat.crc(body.rawBuffer(),0,body.size()));
        File index=new File(root,"index.apdi");Files.write(index.toPath(),body.toByteArray());
        RunIndex loaded=RunIndex.load(index,runs);
        assertTrue("malformed count must not hide existing history behind a valid CRC",loaded.wasRebuilt());
        assertEquals(1,loaded.size());
        assertNotNull(loaded.get("preserved"));
        assertArrayEquals(original,Files.readAllBytes(log.toPath()));
    }
    @Test public void checksumValidUncountedTailRebuildsFromOriginalLogs() throws Exception {
        File root = tmp.newFolder("checksum-valid-tail");
        StatsStore store = new StatsStore(root);
        RunLogWriter writer = store.startRun("preserved", "test", 1);
        writer.event(LogTestSupport.T_PLAIN).clock(3).unsigned(1).emit();
        writer.close();
        assertTrue(store.refreshIndex("preserved"));
        byte[] original = Files.readAllBytes(store.logFile("preserved").toPath());

        // The index body claims zero entries while carrying extra data under a fresh, valid CRC.
        // Previously this was accepted and hid the preserved run until an explicit rebuild.
        ByteSink body = new ByteSink().writeBytes(RunIndex.MAGIC).writeByte(RunIndex.FORMAT);
        body.writeUnsigned(0);
        body.writeByte(0x5A);
        body.writeInt32((int) LogFormat.crc(body.rawBuffer(), 0, body.size()));
        File indexFile = new File(root, "index.apdi");
        Files.write(indexFile.toPath(), body.toByteArray());

        RunIndex loaded = RunIndex.load(indexFile, store.runsDir());
        assertTrue("extra checksum-valid payload must rebuild from logs", loaded.wasRebuilt());
        assertEquals(1, loaded.size());
        assertNotNull(loaded.get("preserved"));
        assertEquals(1, loaded.get("preserved").eventCount);
        assertArrayEquals("original log is immutable evidence", original, Files.readAllBytes(store.logFile("preserved").toPath()));

        RunIndex reloaded = RunIndex.load(indexFile, store.runsDir());
        assertFalse("rebuilt clean index must parse normally", reloaded.wasRebuilt());
        assertNotNull(reloaded.get("preserved"));
    }

    @Test public void checksumValidDuplicateRunEntriesRebuildFromOriginalLogs() throws Exception {
        File root = tmp.newFolder("duplicate-index-entry");
        StatsStore store = new StatsStore(root);
        RunLogWriter writer = store.startRun("preserved", "test", 1);
        writer.event(LogTestSupport.T_PLAIN).clock(3).unsigned(17).emit();
        assertTrue(writer.close());
        assertTrue(store.refreshIndex("preserved"));
        byte[] original = Files.readAllBytes(store.logFile("preserved").toPath());
        RunIndexEntry correct = store.index().get("preserved");
        assertEquals(1, correct.eventCount);

        // Both entries have a valid encoding and the entire index has a valid CRC.
        // Without duplicate rejection the later zero-count summary silently wins.
        RunIndexEntry falseSummary = new RunIndexEntry(correct.runId, correct.telemetryVersion,
                correct.newerThanReader, correct.logFile, correct.createdAtMillis, correct.status,
                0, correct.lastClock, correct.validBytes, correct.fileLength);
        byte[] first = correct.encode();
        byte[] second = falseSummary.encode();
        ByteSink body = new ByteSink().writeBytes(RunIndex.MAGIC).writeByte(RunIndex.FORMAT)
                .writeUnsigned(2);
        body.writeUnsigned(first.length).writeBytes(first);
        body.writeUnsigned(second.length).writeBytes(second);
        body.writeInt32((int) LogFormat.crc(body.rawBuffer(), 0, body.size()));
        File indexFile = new File(root, "index.apdi");
        Files.write(indexFile.toPath(), body.toByteArray());

        RunIndex loaded = RunIndex.load(indexFile, store.runsDir());
        assertTrue("duplicate run IDs must rebuild from original logs", loaded.wasRebuilt());
        assertEquals(1, loaded.size());
        assertEquals(1, loaded.get("preserved").eventCount);
        assertArrayEquals("the sealed original log must be unchanged", original,
                Files.readAllBytes(store.logFile("preserved").toPath()));
        RunIndex reloaded = RunIndex.load(indexFile, store.runsDir());
        assertFalse("the clean rebuilt index remains compatible", reloaded.wasRebuilt());
        assertEquals(1, reloaded.get("preserved").eventCount);
    }

    @Test public void legitimateEmptyIndexRemainsValidWithoutRebuilding() throws Exception {
        File root=tmp.newFolder();File runs=new File(root,"runs");assertTrue(runs.mkdir());
        File index=new File(root,"index.apdi");new RunIndex(index,runs).save();
        RunIndex loaded=RunIndex.load(index,runs);
        assertFalse(loaded.wasRebuilt());assertEquals(0,loaded.size());
    }
}
