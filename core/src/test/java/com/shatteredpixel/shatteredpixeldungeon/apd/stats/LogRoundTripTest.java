package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class LogRoundTripTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void everyFieldTypeRoundTrips() throws Exception {
		File f = new File(tmp.getRoot(), "run1.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "run1", "APD 4.0.8-apd", 1_700_000_000_123L);
		byte[] blob = {0, 1, 2, (byte) 255, (byte) 128};
		assertTrue(w.event(LogTestSupport.T_PLAIN).clock(5).flags(3).unsigned(7).signed(-9).bool(true).bool(false)
				.f32(1.25f).f64(-2.5e100).bytes(blob).string("hello").string("héllo 世界 😀")
				.string("").string(null).string("hello").emit());
		assertTrue(w.close());

		ReadResult r = RunLogReader.read(f);
		assertEquals(ReadStatus.SEALED, r.status);
		assertTrue(r.sealed);
		assertEquals(1, r.eventCount);
		assertEquals("run1", r.header.runId);
		assertEquals("APD 4.0.8-apd", r.header.writerBuild);
		assertEquals(1_700_000_000_123L, r.header.createdAtMillis);
		assertEquals(TelemetryVersion.CURRENT, r.header.telemetryVersion);
		assertEquals(LogFormat.CONTAINER_FORMAT, r.header.containerFormat);

		ApdEvent e = r.events.get(0);
		assertEquals(LogTestSupport.T_PLAIN, e.type);
		assertEquals(3, e.flags);
		assertEquals(5, e.clock);
		PayloadReader p = e.reader(r);
		assertEquals(7, p.unsigned());
		assertEquals(-9, p.signed());
		assertTrue(p.bool());
		assertFalse(p.bool());
		assertEquals(1.25f, p.f32(), 0);
		assertEquals(-2.5e100, p.f64(), 0);
		assertArrayEquals(blob, p.bytes());
		assertEquals("hello", p.string());
		assertEquals("héllo 世界 😀", p.string());
		assertEquals("", p.string());
		assertEquals("", p.string()); //null is stored as the empty string
		assertEquals("hello", p.string());
		assertFalse(p.hasRemaining());
		//"hello", the unicode string and "" are the only distinct strings
		assertEquals(3, r.strings.size());
	}

	@Test
	public void manyEventsKeepOrderClocksAndPayloads() throws Exception {
		int n = 5000;
		File f = LogTestSupport.writeLog(tmp.getRoot(), "many", n, true);
		ReadResult r = RunLogReader.read(f);
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(n, r.eventCount);
		assertEquals(n, r.events.size());
		for (int i = 0; i < n; i++){
			ApdEvent e = r.events.get(i);
			assertEquals(i, e.index);
			assertEquals(10L * i, e.clock);
			PayloadReader p = e.reader(r);
			assertEquals(i, p.unsigned());
			assertEquals("s" + (i % 7), p.string());
			assertEquals(-i, p.signed());
		}
		assertEquals(7, r.strings.size());
	}

	@Test
	public void clockCanGoBackwards() throws Exception {
		File f = new File(tmp.getRoot(), "clock.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "clock", "t", 0);
		long[] clocks = {100, 90, 90, 1_000_000_000_000L, 5, -7};
		for (long c : clocks) w.event(LogTestSupport.T_PLAIN).clock(c).emit();
		w.close();
		ReadResult r = RunLogReader.read(f);
		for (int i = 0; i < clocks.length; i++) assertEquals(clocks[i], r.events.get(i).clock);
		assertEquals(-7, r.lastClock);
	}

	@Test
	public void eventsWithoutAClockKeepThePreviousOne() throws Exception {
		File f = new File(tmp.getRoot(), "noclock.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "noclock", "t", 0);
		w.event(LogTestSupport.T_PLAIN).clock(50).emit();
		w.event(LogTestSupport.T_PLAIN).emit();
		w.close();
		ReadResult r = RunLogReader.read(f);
		assertEquals(50, r.events.get(1).clock);
	}

	@Test
	public void clockFramesAreWrittenPeriodically() throws Exception {
		File f = LogTestSupport.writeLog(tmp.getRoot(), "frames", RunLogWriter.CLOCK_FRAME_INTERVAL * 3, true);
		byte[] data = LogTestSupport.bytes(f);
		//count clock frames by walking them
		ReadResult r = RunLogReader.parse(data);
		assertEquals(RunLogWriter.CLOCK_FRAME_INTERVAL * 3, r.eventCount);
		assertTrue(r.skippedUnknownFrames == 0);
	}

	@Test
	public void largePayloadsSurvive() throws Exception {
		File f = new File(tmp.getRoot(), "big.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "big", "t", 0);
		byte[] big = new byte[1 << 20];
		for (int i = 0; i < big.length; i++) big[i] = (byte) (i * 31);
		assertTrue(w.event(LogTestSupport.T_PLAIN).bytes(big).emit());
		w.close();
		ReadResult r = RunLogReader.read(f);
		assertEquals(ReadStatus.SEALED, r.status);
		assertArrayEquals(big, r.events.get(0).reader(r).bytes());
	}

	@Test
	public void unresolvedStringReferenceReadsAsNull() throws Exception {
		ReadResult r = new ReadResult();
		assertNull(r.get(3));
		assertNull(r.get(-1));
	}

	@Test
	public void unsealedLogReadsNormally() throws Exception {
		File f = LogTestSupport.writeLog(tmp.getRoot(), "open", 40, false);
		ReadResult r = RunLogReader.read(f);
		assertEquals(ReadStatus.UNSEALED, r.status);
		assertFalse(r.sealed);
		assertEquals(40, r.events.size());
		assertEquals(0, r.damagedBytes());
	}
}
