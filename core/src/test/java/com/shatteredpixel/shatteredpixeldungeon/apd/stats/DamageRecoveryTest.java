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
import java.util.Random;

//Truncation, corruption and repair. The rule everywhere: read what is valid, report what is not, never throw,
//and never make the whole run unreadable because of a bad tail.
public class DamageRecoveryTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private byte[] fullLog( int n, boolean seal ) throws Exception {
		return LogTestSupport.bytes(LogTestSupport.writeLog(tmp.newFolder(), "dmg", n, seal));
	}

	//the string field of a T_STRINGS event (second field, after the unsigned counter)
	private static String stringOf( ReadResult r, int eventIndex ){
		PayloadReader p = r.events.get(eventIndex).reader(r);
		p.unsigned();
		return p.string();
	}

	//---- truncation ----------------------------------------------------------------------------------------

	@Test
	public void cuttingTheFileAtEveryByteGivesTheValidPrefixAndNeverThrows() throws Exception {
		byte[] full = fullLog(60, false);
		ReadResult whole = RunLogReader.parse(full);
		assertEquals(ReadStatus.UNSEALED, whole.status);
		int headerLength = whole.header.length;

		long lastValid = 0;
		int lastEvents = 0;
		for (int cut = 0; cut <= full.length; cut++){
			ReadResult r = RunLogReader.parse(Arrays.copyOf(full, cut));
			if (cut < headerLength){
				assertEquals("cut " + cut, ReadStatus.BAD_HEADER, r.status);
				assertTrue(r.events.isEmpty());
				continue;
			}
			assertTrue("cut " + cut + " status " + r.status, r.status == ReadStatus.UNSEALED || r.status == ReadStatus.TRUNCATED);
			LogTestSupport.assertPrefix(whole.events, r.events);
			assertTrue(r.validBytes <= cut);
			assertTrue("valid bytes never go backwards", r.validBytes >= lastValid);
			assertTrue("events never disappear when more bytes are available", r.events.size() >= lastEvents);
			if (r.status == ReadStatus.UNSEALED){
				assertEquals("a clean end is exactly at a frame boundary", cut, r.validBytes);
			} else {
				assertTrue(r.damagedBytes() > 0);
			}
			lastValid = r.validBytes;
			lastEvents = r.events.size();
		}
		assertEquals(whole.events.size(), lastEvents);
	}

	@Test
	public void aSealedLogCutBeforeTheSealIsJustUnsealed() throws Exception {
		byte[] full = fullLog(10, true);
		ReadResult sealed = RunLogReader.parse(full);
		assertEquals(ReadStatus.SEALED, sealed.status);
		//drop exactly the seal frame
		byte[] noSeal = Arrays.copyOf(full, (int) sealed.validBytes - LogFormat.sealFrame(10, 90).length);
		ReadResult r = RunLogReader.parse(noSeal);
		assertEquals(ReadStatus.UNSEALED, r.status);
		assertEquals(10, r.events.size());
	}

	//---- corruption ----------------------------------------------------------------------------------------

	@Test
	public void flippedByteInTheMiddleStopsThereButKeepsEverythingBefore() throws Exception {
		byte[] full = fullLog(80, true);
		ReadResult whole = RunLogReader.parse(full);
		byte[] bad = full.clone();
		int at = (int) (whole.validBytes / 2);
		bad[at] ^= 0x55;
		ReadResult r = RunLogReader.parse(bad);
		assertEquals(ReadStatus.CORRUPT, r.status);
		assertNotNull(r.problem);
		assertTrue(r.events.size() > 0 && r.events.size() < whole.events.size());
		LogTestSupport.assertPrefix(whole.events, r.events);
		assertTrue(r.validBytes <= at);
		assertEquals(bad.length - r.validBytes, r.damagedBytes());
	}

	@Test
	public void resyncRecoversLaterEventsAndMarksTheGap() throws Exception {
		//events without strings, so every frame stands alone
		java.util.List<byte[]> frames = LogTestSupport.plainFrames(100);
		byte[] data = LogTestSupport.build(TelemetryVersion.CURRENT, "rs", frames.toArray(new byte[0][]));
		ReadResult whole = RunLogReader.parse(data);
		//damage one frame in the middle
		int start = whole.header.length;
		for (int i = 0; i < 50; i++) start += frames.get(i).length;
		byte[] bad = data.clone();
		bad[start + 4] ^= 0x7F;

		ReadResult strict = RunLogReader.parse(bad);
		assertEquals(ReadStatus.CORRUPT, strict.status);
		assertTrue(strict.events.size() < 50);

		ReadResult resync = RunLogReader.parse(bad, new ReadOptions().resync(true));
		assertEquals(ReadStatus.CORRUPT, resync.status); //damage happened, that is still reported
		assertEquals(1, resync.resyncs);
		assertTrue(resync.skippedBytes > 0);
		assertEquals(strict.validBytes, resync.validBytes); //repair would still cut at the first damage
		assertEquals(99, resync.events.size()); //only the damaged frame is lost
		//the next clock frame is up to 128 events away, so these events are flagged as having an uncertain clock
		assertTrue(resync.uncertainClockEvents > 0);
		assertEquals(whole.events.get(99).type, resync.events.get(98).type);
	}

	@Test
	public void garbageAppendedToAnUnsealedLogIsCorruptTail() throws Exception {
		byte[] full = fullLog(20, false);
		byte[] junk = new byte[37];
		new Random(1).nextBytes(junk);
		junk[0] = 0x00; //make sure it is not accidentally a frame marker
		ReadResult r = RunLogReader.parse(LogTestSupport.concat(full, junk));
		assertEquals(ReadStatus.CORRUPT, r.status);
		assertEquals(20, r.events.size());
		assertEquals(full.length, r.validBytes);
		assertEquals(37, r.damagedBytes());
	}

	@Test
	public void garbageAfterTheSealLeavesTheRunSealed() throws Exception {
		byte[] full = fullLog(20, true);
		ReadResult r = RunLogReader.parse(LogTestSupport.concat(full, new byte[]{1, 2, 3}));
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(20, r.events.size());
		assertEquals(3, r.damagedBytes());
	}

	@Test
	public void damagedHeaderMakesTheFileUnreadableButNeverThrows() throws Exception {
		byte[] full = fullLog(5, true);
		for (int i = 0; i < RunLogReader.parse(full).header.length; i++){
			byte[] bad = full.clone();
			bad[i] ^= 0x01;
			ReadResult r = RunLogReader.parse(bad);
			assertTrue("byte " + i + " " + r.status, r.status == ReadStatus.BAD_HEADER || r.status == ReadStatus.UNSUPPORTED_CONTAINER);
			assertTrue(r.events.isEmpty());
		}
	}

	@Test
	public void sealWithTheWrongEventCountIsReported() throws Exception {
		byte[] data = LogTestSupport.build(TelemetryVersion.CURRENT, "sm",
				LogFormat.eventFrame(LogTestSupport.T_PLAIN, 0, 0, new byte[0]), LogFormat.sealFrame(5, 0));
		ReadResult r = RunLogReader.parse(data);
		assertEquals(ReadStatus.SEAL_MISMATCH, r.status);
		assertEquals(1, r.events.size());
	}

	@Test
	public void randomDamageNeverThrowsAndEventsAreAlwaysTheRealOnes() throws Exception {
		byte[] full = fullLog(150, true);
		ReadResult whole = RunLogReader.parse(full);
		Random rnd = new Random(2024);
		for (int round = 0; round < 1500; round++){
			byte[] bad = full.clone();
			int flips = 1 + rnd.nextInt(3);
			for (int i = 0; i < flips; i++) bad[rnd.nextInt(bad.length)] ^= (byte) (1 + rnd.nextInt(255));
			if (rnd.nextInt(4) == 0) bad = Arrays.copyOf(bad, rnd.nextInt(bad.length + 1));

			ReadResult strict = RunLogReader.parse(bad);
			LogTestSupport.assertPrefix(whole.events, strict.events);

			//with resync, events that survive must be genuine ones (same type and payload as some original event)
			ReadResult resync = RunLogReader.parse(bad, new ReadOptions().resync(true));
			for (ApdEvent e : resync.events){
				boolean found = false;
				for (ApdEvent o : whole.events){
					if (o.type == e.type && Arrays.equals(o.payload, e.payload)){ found = true; break; }
				}
				assertTrue("round " + round + " produced an event that was never written: " + e, found);
			}
			assertTrue(resync.events.size() >= strict.events.size());
		}
	}

	//---- repair --------------------------------------------------------------------------------------------

	@Test
	public void repairCutsTheTailSavesItAndLeavesACleanLog() throws Exception {
		File f = LogTestSupport.writeLog(tmp.getRoot(), "fix", 30, false);
		byte[] full = LogTestSupport.bytes(f);
		ReadResult whole = RunLogReader.parse(full);
		byte[] cut = Arrays.copyOf(full, full.length - 3); //interrupted mid-frame
		LogTestSupport.write(f, cut);

		LogRecovery.RepairReport rep = LogRecovery.repair(f);
		assertNull(rep.error);
		assertEquals(ReadStatus.TRUNCATED, rep.statusBefore);
		assertTrue(rep.truncated);
		assertTrue(rep.discardedBytes > 0);
		assertNotNull(rep.savedTail);
		//the saved tail is exactly the bytes that were removed
		byte[] tail = LogTestSupport.bytes(rep.savedTail);
		assertEquals(rep.discardedBytes, tail.length);
		assertArrayEquals(Arrays.copyOfRange(cut, (int) rep.validBytes, cut.length), tail);
		assertEquals(rep.validBytes, f.length());

		ReadResult after = RunLogReader.read(f);
		assertEquals(ReadStatus.UNSEALED, after.status);
		assertEquals(29, after.events.size());
		LogTestSupport.assertPrefix(whole.events, after.events);

		//repairing again changes nothing
		LogRecovery.RepairReport again = LogRecovery.repair(f);
		assertFalse(again.truncated);
		assertEquals(0, again.discardedBytes);
		assertNull(again.savedTail);
	}

	@Test
	public void repairLeavesHealthySealedAndUnreadableLogsAlone() throws Exception {
		File sealed = LogTestSupport.writeLog(tmp.getRoot(), "sealed", 10, true);
		byte[] before = LogTestSupport.bytes(sealed);
		assertFalse(LogRecovery.repair(sealed).truncated);
		assertArrayEquals(before, LogTestSupport.bytes(sealed));

		File junk = new File(tmp.getRoot(), "junk.apdlog");
		LogTestSupport.write(junk, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
		LogRecovery.RepairReport rep = LogRecovery.repair(junk);
		assertEquals(ReadStatus.BAD_HEADER, rep.statusBefore);
		assertFalse(rep.truncated);
		assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, LogTestSupport.bytes(junk));
	}

	@Test
	public void repairTrimsJunkAfterTheSealToo() throws Exception {
		File f = LogTestSupport.writeLog(tmp.getRoot(), "trail", 10, true);
		byte[] full = LogTestSupport.bytes(f);
		LogTestSupport.write(f, LogTestSupport.concat(full, new byte[]{9, 9, 9}));
		LogRecovery.RepairReport rep = LogRecovery.repair(f);
		assertTrue(rep.truncated);
		assertEquals(3, rep.discardedBytes);
		assertArrayEquals(full, LogTestSupport.bytes(f));
	}

	@Test
	public void reopeningADamagedLogRecoversRecordsAGapAndContinues() throws Exception {
		File f = LogTestSupport.writeLog(tmp.getRoot(), "reopen", 25, false);
		byte[] full = LogTestSupport.bytes(f);
		LogTestSupport.write(f, Arrays.copyOf(full, full.length - 5));
		ReadResult damaged = RunLogReader.read(f);
		assertEquals(ReadStatus.TRUNCATED, damaged.status);

		RunLogWriter w = RunLogWriter.openForAppend(f, TelemetryVersion.CURRENT);
		assertEquals(RunLogWriter.OpenStatus.RECOVERED_TAIL, w.openStatus());
		assertFalse(w.isDisabled());
		//reuses an existing string ("s0") and adds a new one, ids must not collide
		assertTrue(w.event(LogTestSupport.T_STRINGS).clock(1000).unsigned(1).string("s0").signed(1).emit());
		assertTrue(w.event(LogTestSupport.T_STRINGS).clock(1010).unsigned(2).string("brand new").signed(2).emit());
		assertTrue(w.close());

		ReadResult r = RunLogReader.read(f);
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(24 + 1 + 2, r.events.size()); //24 survived, a GAP, 2 new
		ApdEvent gap = r.events.get(24);
		assertEquals(EventTypes.GAP, gap.type);
		PayloadReader gp = gap.reader(r);
		assertEquals(EventTypes.GAP_RECOVERED_TAIL, gp.unsigned());
		assertEquals(damaged.damagedBytes(), gp.unsigned());
		assertEquals("s0", stringOf(r, 25));
		assertEquals(1000, r.events.get(25).clock);
		assertEquals("brand new", stringOf(r, 26));
		assertEquals(1, r.gapEvents);
		//the cut bytes were kept
		File[] saved = tmp.getRoot().listFiles((d, name) -> name.endsWith(".damaged"));
		assertEquals(1, saved.length);
	}

	@Test
	public void sealedMissingAndUnreadableLogsAreNotAppendedTo() throws Exception {
		File sealed = LogTestSupport.writeLog(tmp.getRoot(), "s", 3, true);
		assertEquals(RunLogWriter.OpenStatus.FAILED_SEALED, RunLogWriter.openForAppend(sealed, TelemetryVersion.CURRENT).openStatus());
		assertEquals(RunLogWriter.OpenStatus.FAILED_MISSING,
				RunLogWriter.openForAppend(new File(tmp.getRoot(), "nope.apdlog"), TelemetryVersion.CURRENT).openStatus());
		File junk = new File(tmp.getRoot(), "j.apdlog");
		LogTestSupport.write(junk, new byte[]{1, 2, 3});
		assertEquals(RunLogWriter.OpenStatus.FAILED_BAD_HEADER, RunLogWriter.openForAppend(junk, TelemetryVersion.CURRENT).openStatus());
	}
}
