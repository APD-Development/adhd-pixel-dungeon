package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;

// Reopening a long, unsealed run only needs validated append metadata, not
// another in-memory copy of every prior gameplay event and payload.
public class RunLogAppendIdentityScanTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void longUnsealedHistoryReopensWithoutLosingPriorEventsStringsOrClocks() throws Exception {
		final int priorEvents = 1024; // exercise multiple writer clock-frame checkpoints
		File file = LogTestSupport.writeLog(tmp.getRoot(), "long-append", priorEvents, false);
		byte[] originalBytes = LogTestSupport.bytes(file);
		ReadResult full = RunLogReader.read(file);
		ReadResult appendMetadata = RunLogReader.read(file, new ReadOptions().onlyRunStartEvents(true));

		assertEquals(ReadStatus.UNSEALED, full.status);
		assertEquals(full.status, appendMetadata.status);
		assertEquals(priorEvents, appendMetadata.eventCount);
		assertTrue(appendMetadata.events.isEmpty());
		assertEquals(full.strings, appendMetadata.strings);
		assertEquals(full.lastClock, appendMetadata.lastClock);
		assertEquals(full.validBytes, appendMetadata.validBytes);
		assertEquals(7, full.strings.size());

		RunLogWriter resumed = RunLogWriter.openForAppend(file, TelemetryVersion.CURRENT);
		assertEquals(RunLogWriter.OpenStatus.OK, resumed.openStatus());
		assertFalse(resumed.isDisabled());
		// An existing definition must keep its original ID; a new definition
		// must get the next ID without changing any prior event's meaning.
		assertTrue(resumed.event(LogTestSupport.T_STRINGS).clock(10240)
				.unsigned(priorEvents).string("s3").signed(-1).emit());
		assertTrue(resumed.event(LogTestSupport.T_STRINGS).clock(10250)
				.unsigned(priorEvents + 1).string("new-after-reopen").signed(-2).emit());
		assertTrue(resumed.close());

		byte[] afterBytes = LogTestSupport.bytes(file);
		assertArrayEquals(originalBytes, Arrays.copyOf(afterBytes, originalBytes.length));
		ReadResult after = RunLogReader.read(file);
		assertEquals(ReadStatus.SEALED, after.status);
		assertEquals(priorEvents + 2, after.eventCount);
		assertEquals(priorEvents + 2, after.sealEventCount);
		assertEquals(priorEvents + 2, after.events.size());
		assertEquals(8, after.strings.size());
		assertEquals(10250, after.lastClock);
		assertEquals(file.length(), after.validBytes);
		LogTestSupport.assertPrefix(after.events, full.events);

		PayloadReader reused = after.events.get(priorEvents).reader(after);
		assertEquals(priorEvents, reused.unsigned());
		assertEquals("s3", reused.string());
		assertEquals(-1, reused.signed());
		assertFalse(reused.hasRemaining());

		PayloadReader added = after.events.get(priorEvents + 1).reader(after);
		assertEquals(priorEvents + 1, added.unsigned());
		assertEquals("new-after-reopen", added.string());
		assertEquals(-2, added.signed());
		assertFalse(added.hasRemaining());
	}
}
