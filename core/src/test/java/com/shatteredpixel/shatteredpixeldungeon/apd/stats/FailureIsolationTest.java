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
import java.util.concurrent.atomic.AtomicInteger;

//A storage problem must never reach the caller: no exceptions, no hangs, just a disabled writer that says why.
public class FailureIsolationTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static void useDisabledWriterEveryWay( RunLogWriter w ){
		//none of these may throw or block
		assertFalse(w.event(LogTestSupport.T_PLAIN).clock(1).flags(2).unsigned(3).signed(-3).bool(true).f32(1).f64(2)
				.bytes(new byte[]{1}).bytes(null).string("x").string(null).emit());
		assertFalse(w.append(LogTestSupport.T_PLAIN, 0, 0, new byte[0]));
		assertFalse(w.append(LogTestSupport.T_PLAIN, 0, 0, null));
		assertFalse(w.flush());
		assertFalse(w.flush(1, true));
		assertFalse(w.close());
		assertFalse(w.close(false));
		assertTrue(w.isDisabled());
	}

	@Test
	public void unwritableLocationGivesADisabledWriterNotAnException() throws Exception {
		File notADir = tmp.newFile("file-in-the-way");
		final AtomicInteger calls = new AtomicInteger();
		RunLogWriter.Options o = new RunLogWriter.Options().listener((what, err) -> calls.incrementAndGet());
		RunLogWriter w = RunLogWriter.create(new File(notADir, "sub/x.apdlog"), TelemetryVersion.CURRENT, "x", "t", 0, o);
		assertEquals(RunLogWriter.OpenStatus.FAILED_IO, w.openStatus());
		assertNotNull(w.failure());
		assertTrue(calls.get() >= 1);
		useDisabledWriterEveryWay(w);
	}

	@Test
	public void existingLogIsNeverOverwritten() throws Exception {
		File f = LogTestSupport.writeLog(tmp.getRoot(), "keep", 10, true);
		byte[] before = LogTestSupport.bytes(f);
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "keep", "t", 0);
		assertEquals(RunLogWriter.OpenStatus.FAILED_EXISTS, w.openStatus());
		useDisabledWriterEveryWay(w);
		assertArrayEquals(before, LogTestSupport.bytes(f));
	}

	@Test
	public void badRunIdsAreRefused() throws Exception {
		String[] bad = {"", "../escape", "a/b", "a b", "dot.dot", "café", null,
				"0123456789012345678901234567890123456789012345678901234567890123456789"};
		for (String id : bad){
			RunLogWriter w = RunLogWriter.create(new File(tmp.getRoot(), "bad.apdlog"), TelemetryVersion.CURRENT, id, "t", 0);
			assertEquals("id " + id, RunLogWriter.OpenStatus.FAILED_BAD_RUN_ID, w.openStatus());
			useDisabledWriterEveryWay(w);
		}
		assertFalse(new File(tmp.getRoot(), "bad.apdlog").exists());
		assertTrue(StatsStore.isValidRunId("3f2b9c1e-7d4a-4c1b-9e5f-0a1b2c3d4e5f"));
		assertTrue(StatsStore.isValidRunId("run_1-A"));
	}

	@Test
	public void invalidEventsAreIgnoredNotFatal() throws Exception {
		RunLogWriter w = RunLogWriter.create(new File(tmp.getRoot(), "inv.apdlog"), TelemetryVersion.CURRENT, "inv", "t", 0);
		assertFalse(w.event(0).emit());
		assertFalse(w.event(-5).emit());
		assertFalse(w.event(LogTestSupport.T_PLAIN).unsigned(-1).emit()); //negative in an unsigned field
		assertFalse("those mistakes must not disable recording", w.isDisabled());
		assertTrue(w.event(LogTestSupport.T_PLAIN).unsigned(1).emit());
		assertTrue(w.close());
		assertEquals(1, RunLogReader.read(w.file()).events.size());
	}

	@Test
	public void aDiskFailureMidWriteDisablesTheWriterAndLeavesARecoverableLog() throws Exception {
		final AtomicInteger notified = new AtomicInteger();
		RunLogWriter.Options o = new RunLogWriter.Options().listener((what, err) -> notified.incrementAndGet());
		o.failAfterBytes = 400; //the "disk" fills up part way through the 400th byte
		File f = new File(tmp.getRoot(), "disk.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "disk", "t", 0, o);
		int accepted = 0;
		for (int i = 0; i < 200; i++){
			if (w.event(LogTestSupport.T_PLAIN).clock(i).unsigned(i).emit()) accepted++;
			if (i == 100) w.flush();
		}
		w.flush();
		assertTrue("writer must have failed", w.isDisabled());
		assertNotNull(w.failure());
		assertTrue(notified.get() >= 1);
		//after the failure nothing is accepted, and nothing throws or blocks
		assertFalse(w.event(LogTestSupport.T_PLAIN).unsigned(1).emit());
		assertFalse(w.close());

		//what reached the disk is a valid prefix with a partly written last frame
		ReadResult r = RunLogReader.read(f);
		assertTrue(r.status == ReadStatus.TRUNCATED || r.status == ReadStatus.UNSEALED);
		assertTrue(r.events.size() > 0 && r.events.size() < accepted);
		for (int i = 0; i < r.events.size(); i++) assertEquals(i, r.events.get(i).reader(r).unsigned());

		LogRecovery.RepairReport rep = LogRecovery.repair(f);
		assertNull(rep.error);
		assertTrue(RunLogReader.read(f).status.isClean());
	}

	@Test
	public void aListenerThatThrowsDoesNotMatter() throws Exception {
		RunLogWriter.Options o = new RunLogWriter.Options().listener((what, err) -> { throw new IllegalStateException("boom"); });
		o.failAfterBytes = 100;
		RunLogWriter w = RunLogWriter.create(new File(tmp.getRoot(), "l.apdlog"), TelemetryVersion.CURRENT, "l", "t", 0, o);
		for (int i = 0; i < 100; i++) w.event(LogTestSupport.T_PLAIN).unsigned(i).emit();
		w.flush();
		assertTrue(w.isDisabled());
		assertFalse(w.close());
	}

	@Test
	public void readerNeverThrowsOnArbitraryFiles() throws Exception {
		java.util.Random rnd = new java.util.Random(7);
		for (int i = 0; i < 300; i++){
			byte[] junk = new byte[rnd.nextInt(200)];
			rnd.nextBytes(junk);
			ReadResult r = RunLogReader.parse(junk, new ReadOptions().resync(true).knownTypesOnly(true));
			assertNotNull(r.status);
		}
		//a file starting with the real magic but nonsense after it
		byte[] magicOnly = LogTestSupport.concat(LogFormat.MAGIC, new byte[]{1, 0, 5, 1, 2, 3, 4, 5, 6, 7, 8, 9});
		assertEquals(ReadStatus.BAD_HEADER, RunLogReader.parse(magicOnly).status);
	}
}
