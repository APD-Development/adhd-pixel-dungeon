package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

//A run keeps the telemetry version that recorded it, and nothing silently relabels it.
public class TelemetryVersionHandlingTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File writeWith( String id, TelemetryVersion v, boolean seal ) throws Exception {
		File f = new File(tmp.getRoot(), id + ".apdlog");
		RunLogWriter w = RunLogWriter.create(f, v, id, "test", 7);
		w.event(LogTestSupport.T_PLAIN).clock(1).unsigned(1).emit();
		w.close(seal);
		return f;
	}

	@Test
	public void oldVersionStaysOldWhenReadByANewerBuild() throws Exception {
		TelemetryVersion old = new TelemetryVersion(0, 9);
		File f = writeWith("old", old, true);
		ReadResult r = RunLogReader.read(f);
		assertEquals(old, r.header.telemetryVersion);
		assertFalse(r.newerThanReader());
		assertEquals(1, r.events.size());
	}

	@Test
	public void newerMinorIsReadableAndFlagged() throws Exception {
		File f = writeWith("minor", new TelemetryVersion(1, 99), true);
		ReadResult r = RunLogReader.read(f);
		assertEquals(ReadStatus.SEALED, r.status);
		assertTrue(r.newerThanReader());
		assertEquals(new TelemetryVersion(1, 99), r.header.telemetryVersion);
		assertEquals(1, r.events.size());
	}

	@Test
	public void newerMajorIsReadableAtFrameLevelAndFlaggedIncompatible() throws Exception {
		File f = writeWith("major", new TelemetryVersion(2, 0), true);
		ReadResult r = RunLogReader.read(f);
		assertEquals(ReadStatus.SEALED, r.status);
		assertTrue(r.newerThanReader());
		assertTrue(r.header.telemetryVersion.isIncompatibleMajor(TelemetryVersion.CURRENT));
		//frames and events can be walked, but it is the caller's job not to interpret them as the current schema
		assertEquals(1, r.events.size());
	}

	@Test
	public void readingNeverModifiesTheFile() throws Exception {
		File f = writeWith("ro", new TelemetryVersion(3, 1), false);
		byte[] before = LogTestSupport.bytes(f);
		long modified = f.lastModified();
		RunLogReader.read(f);
		RunLogReader.read(f, new ReadOptions().resync(true).knownTypesOnly(true));
		assertArrayEquals(before, LogTestSupport.bytes(f));
		assertEquals(modified, f.lastModified());
	}

	@Test
	public void appendRequiresTheSameTelemetryVersion() throws Exception {
		TelemetryVersion v = new TelemetryVersion(1, 0);
		File f = writeWith("app", v, false);
		byte[] before = LogTestSupport.bytes(f);

		RunLogWriter other = RunLogWriter.openForAppend(f, new TelemetryVersion(1, 1));
		assertEquals(RunLogWriter.OpenStatus.FAILED_VERSION_MISMATCH, other.openStatus());
		assertTrue(other.isDisabled());
		assertFalse(other.event(LogTestSupport.T_PLAIN).emit());
		assertArrayEquals("a refused append must not touch the log", before, LogTestSupport.bytes(f));

		RunLogWriter same = RunLogWriter.openForAppend(f, v);
		assertEquals(RunLogWriter.OpenStatus.OK, same.openStatus());
		assertTrue(same.event(LogTestSupport.T_PLAIN).clock(2).unsigned(2).emit());
		assertTrue(same.close());
		ReadResult r = RunLogReader.read(f);
		assertEquals(v, r.header.telemetryVersion);
		assertEquals(2, r.events.size());
	}

	@Test
	public void futureContainerFormatIsRefusedNotMisread() throws Exception {
		File f = writeWith("cont", TelemetryVersion.CURRENT, true);
		byte[] data = LogTestSupport.bytes(f);
		data[4] = 9; //container format byte
		ReadResult r = RunLogReader.parse(data);
		assertEquals(ReadStatus.UNSUPPORTED_CONTAINER, r.status);
		assertTrue(r.events.isEmpty());
		LogTestSupport.write(f, data);
		RunLogWriter w = RunLogWriter.openForAppend(f, TelemetryVersion.CURRENT);
		assertEquals(RunLogWriter.OpenStatus.FAILED_UNSUPPORTED_CONTAINER, w.openStatus());
		assertArrayEquals(data, LogTestSupport.bytes(f));
	}

	@Test
	public void newRunsAreRecordedWithTheCurrentVersion() throws Exception {
		StatsStore store = new StatsStore(tmp.newFolder("store"));
		RunLogWriter w = store.startRun("r1", "build", 5);
		w.close();
		assertEquals(TelemetryVersion.CURRENT, RunLogReader.read(store.logFile("r1")).header.telemetryVersion);
		assertEquals(TelemetryVersion.CURRENT, store.index().get("r1").telemetryVersion);
	}
}
