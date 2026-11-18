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

public class StatsStoreRecoveryFilenameBoundaryTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private byte[] damagedLog( StatsStore store, String id ) throws Exception {
		RunLogWriter writer = store.startRun(id, "test", 1);
		for (int i = 0; i < 9; i++) writer.event(LogTestSupport.T_PLAIN).clock(i).unsigned(i).emit();
		writer.close(false);
		byte[] original = LogTestSupport.bytes(store.logFile(id));
		return Arrays.copyOf(original, original.length - 2);
	}

	@Test
	public void invalidFilenameWithValidDamagedHeaderIsNeverRepaired() throws Exception {
		StatsStore store = new StatsStore(tmp.newFolder("invalid-recovery"));
		byte[] damaged = damagedLog(store, "legitimate");
		LogTestSupport.write(store.logFile("legitimate"), damaged);

		// This copied log has a real parseable APD header and damaged tail, but
		// its filename is not a run ID and must never be truncated or backed up.
		File foreign = new File(store.runsDir(), "not!a-run.apdlog");
		LogTestSupport.write(foreign, damaged);
		List<LogRecovery.RepairReport> reports = store.recoverAll();

		assertEquals(1, reports.size());
		assertEquals(store.logFile("legitimate"), reports.get(0).file);
		assertTrue("valid named damaged run must still repair", reports.get(0).truncated);
		assertNotNull(reports.get(0).savedTail);
		assertTrue(reports.get(0).savedTail.isFile());
		assertArrayEquals("foreign file bytes must remain unchanged", damaged, LogTestSupport.bytes(foreign));
		assertNull("invalid run names must not be indexed", store.index().get("not!a-run"));
		assertNotNull("legitimate run remains indexed", store.index().get("legitimate"));
		for (File child : store.runsDir().listFiles()){
			assertFalse("foreign damaged tail sidecar must not be created",
					child.getName().startsWith("not!a-run.apdlog.tail-"));
		}
	}

	@Test
	public void recoverySkipsInvalidNamesWithoutAnyValidRun() throws Exception {
		StatsStore store = new StatsStore(tmp.newFolder("foreign-only-recovery"));
		byte[] damaged = damagedLog(store, "source");
		File invalid = new File(store.runsDir(), "bad name.apdlog");
		LogTestSupport.write(invalid, damaged);
		File excessive = new File(store.runsDir(), repeat('x', LogFormat.MAX_RUN_ID_LENGTH + 1) + ".apdlog");
		LogTestSupport.write(excessive, damaged);
		assertTrue(store.logFile("source").delete());

		assertTrue("no foreign names are valid repair targets", store.recoverAll().isEmpty());
		assertArrayEquals(damaged, LogTestSupport.bytes(invalid));
		assertArrayEquals(damaged, LogTestSupport.bytes(excessive));
		assertEquals(0, store.index().size());
		for (File child : store.runsDir().listFiles()){
			assertFalse("foreign files must not create damaged-tail sidecars",
					child.getName().contains(".tail-"));
		}
	}

	private static String repeat(char c, int count){
		char[] chars = new char[count];
		Arrays.fill(chars, c);
		return new String(chars);
	}
}
