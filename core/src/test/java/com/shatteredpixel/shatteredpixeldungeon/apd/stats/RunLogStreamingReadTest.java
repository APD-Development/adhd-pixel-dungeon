package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotSame;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class RunLogStreamingReadTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void ordinaryFileReadMatchesTheByteArrayParser() throws Exception {
		File file = new File(tmp.getRoot(), "parity.apdlog");
		RunLogWriter writer = RunLogWriter.create(file, TelemetryVersion.CURRENT,
				"parity", "test", 1234);
		writer.event(LogTestSupport.T_PLAIN).clock(7).flags(3).string("alpha").signed(-9).emit();
		writer.event(LogTestSupport.T_PLAIN).clock(11).bytes(new byte[]{1, 2, 3}).emit();
		writer.close();

		ReadResult streamed = RunLogReader.read(file);
		ReadResult buffered = RunLogReader.parse(LogTestSupport.bytes(file));
		assertEquivalent(buffered, streamed);
	}

	@Test
	public void corruptSparseTailDoesNotRequireWholeFileMemory() throws Exception {
		File file = new File(tmp.getRoot(), "sparse.apdlog");
		byte[] header = LogHeader.encode(TelemetryVersion.CURRENT, 1000, "sparse", "test");
		FileOutputStream out = new FileOutputStream(file);
		out.write(header);
		out.write(0); //not a frame marker
		out.close();

		long sparseLength = 1L << 30;
		RandomAccessFile sparse = new RandomAccessFile(file, "rw");
		sparse.setLength(sparseLength);
		sparse.close();

		ReadResult result = RunLogReader.read(file);
		assertEquals(ReadStatus.CORRUPT, result.status);
		assertEquals(sparseLength, result.fileLength);
		assertEquals(header.length, result.validBytes);
		assertEquals(sparseLength - header.length, result.damagedBytes());
		assertEquals(0, result.events.size());
	}

	@Test
	public void identityOnlyReadValidatesLargeGameplayEventsWithoutCopyingTheirTails() throws Exception {
		File file = new File(tmp.getRoot(), "large-event-tails.apdlog");
		RunLogWriter writer = RunLogWriter.create(file, TelemetryVersion.CURRENT,
				"large-event-tails", "test", 1234);
		byte[] largePayload = new byte[32 * 1024];
		for (int i = 0; i < largePayload.length; i++) largePayload[i] = (byte) (i * 31);
		for (int i = 0; i < 32; i++){
			writer.event(LogTestSupport.T_PLAIN).clock(i + 1).bytes(largePayload).emit();
		}
		writer.close();

		ReadResult ordinary = RunLogReader.read(file);
		ReadResult identityStream = RunLogReader.read(file, new ReadOptions().onlyRunStartEvents(true));
		ReadResult identityBytes = RunLogReader.parse(LogTestSupport.bytes(file),
				new ReadOptions().onlyRunStartEvents(true));
		assertEquals(ReadStatus.SEALED, ordinary.status);
		assertEquals(32, ordinary.events.size());
		assertEquals(0, identityStream.events.size());
		assertEquals(0, identityBytes.events.size());
		assertEquals(32, identityStream.eventCount);
		assertEquals(ordinary.status, identityStream.status);
		assertEquals(ordinary.status, identityBytes.status);
		assertEquals(ordinary.eventCount, identityStream.eventCount);
		assertEquals(ordinary.eventCount, identityBytes.eventCount);
		assertEquals(ordinary.sealEventCount, identityStream.sealEventCount);
		assertEquals(ordinary.sealEventCount, identityBytes.sealEventCount);
		assertEquals(ordinary.lastClock, identityStream.lastClock);
		assertEquals(ordinary.lastClock, identityBytes.lastClock);
		assertEquals(ordinary.validBytes, identityStream.validBytes);
		assertEquals(ordinary.validBytes, identityBytes.validBytes);
		assertEquals(ordinary.strings, identityStream.strings);
		assertEquals(ordinary.strings, identityBytes.strings);
		assertArrayEquals(ordinary.events.get(0).payload,
				RunLogReader.parse(LogTestSupport.bytes(file)).events.get(0).payload);
	}


	@Test
	public void identityOnlyStreamingReusesEqualLengthFramesAndKeepsDifferentLengthsCorrect() throws Exception {
		File file = new File(tmp.getRoot(), "repeated-frame-lengths.apdlog");
		RunLogWriter writer = RunLogWriter.create(file, TelemetryVersion.CURRENT,
				"repeated-frame-lengths", "test", 1234);
		byte[] wide = new byte[4096];
		byte[] narrow = new byte[19];
		for (int i = 0; i < 768; i++){
			// Runs of equal-size frames permit reuse; changing sizes must not
			// expose stale bytes from an earlier, larger payload.
			byte[] payload = (i / 8) % 3 == 0 ? narrow : wide;
			Arrays.fill(payload, (byte) (i * 17));
			writer.event(LogTestSupport.T_PLAIN).clock(i + 1).bytes(payload).emit();
		}
		writer.close();

		ReadResult ordinary = RunLogReader.read(file);
		ReadResult buffered = RunLogReader.parse(LogTestSupport.bytes(file));
		assertEquivalent(buffered, ordinary);
		ReadResult imported = RunLogReader.read(file, new ReadOptions().onlyRunStartEvents(true));
		ReadResult parsed = RunLogReader.parse(LogTestSupport.bytes(file),
				new ReadOptions().onlyRunStartEvents(true));
		assertEquals(ReadStatus.SEALED, imported.status);
		assertEquals(0, imported.events.size());
		assertEquals(768, imported.eventCount);
		assertEquals(ordinary.status, imported.status);
		assertEquals(ordinary.eventCount, imported.eventCount);
		assertEquals(ordinary.sealEventCount, imported.sealEventCount);
		assertEquals(ordinary.lastClock, imported.lastClock);
		assertEquals(ordinary.validBytes, imported.validBytes);
		assertEquals(ordinary.strings, imported.strings);
		assertEquals(parsed.status, imported.status);
		assertEquals(parsed.eventCount, imported.eventCount);
		assertEquals(parsed.sealEventCount, imported.sealEventCount);
		assertEquals(parsed.validBytes, imported.validBytes);
	}


	@Test
	public void streamingFrameResultReuseKeepsEveryEventPayloadIndependent() throws Exception {
		File file = new File(tmp.getRoot(), "streamed-frame-result-reuse.apdlog");
		RunLogWriter writer = RunLogWriter.create(file, TelemetryVersion.CURRENT,
				"frame-record-reuse", "test", 1234);
		for (int i = 0; i < 384; i++){
			// Repeated sizes expose aliased frame storage; varying sizes exercise
			// the same result record after short and long frames.
			byte[] payload = new byte[(i / 12) % 2 == 0 ? 64 : 512];
			Arrays.fill(payload, (byte) (i * 19));
			writer.event(LogTestSupport.T_PLAIN).clock(i + 1).bytes(payload).emit();
		}
		writer.close();

		ReadResult buffered = RunLogReader.parse(LogTestSupport.bytes(file));
		ReadResult streamed = RunLogReader.read(file);
		assertEquals(ReadStatus.SEALED, streamed.status);
		assertEquals(384, streamed.events.size());
		assertEquivalent(buffered, streamed);
		for (int i = 1; i < streamed.events.size(); i++){
			assertNotSame(streamed.events.get(i - 1).payload, streamed.events.get(i).payload);
			assertArrayEquals(buffered.events.get(i - 1).payload, streamed.events.get(i - 1).payload);
		}
		ReadResult identity = RunLogReader.read(file, new ReadOptions().onlyRunStartEvents(true));
		assertEquals(ReadStatus.SEALED, identity.status);
		assertEquals(384, identity.eventCount);
		assertEquals(0, identity.events.size());
		assertEquals(streamed.validBytes, identity.validBytes);
	}


	@Test
	public void streamingChecksumReusePreservesManyValidFramesAndDetectsLaterCorruption() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(LogHeader.encode(TelemetryVersion.CURRENT, 1234, "crc-frames", "test"));
		int corruptChecksumByte = -1;
		for (int i = 0; i < 512; i++){
			byte[] payload = new byte[1 + (i % 37)];
			Arrays.fill(payload, (byte) (i * 13));
			out.write(LogFormat.frame(250, payload));
			if (i == 255) corruptChecksumByte = out.size() - 1;
		}
		out.write(LogFormat.sealFrame(0, 0));
		byte[] valid = out.toByteArray();

		File file = new File(tmp.getRoot(), "crc-valid.apdlog");
		FileOutputStream stream = new FileOutputStream(file);
		try {
			stream.write(valid);
		} finally {
			stream.close();
		}
		ReadResult ordinary = RunLogReader.read(file);
		assertEquals(ReadStatus.SEALED, ordinary.status);
		assertEquals(512, ordinary.skippedUnknownFrames);
		assertEquivalent(RunLogReader.parse(valid), ordinary);

		byte[] damaged = Arrays.copyOf(valid, valid.length);
		damaged[corruptChecksumByte] ^= 0x55;
		file = new File(tmp.getRoot(), "crc-corrupt.apdlog");
		stream = new FileOutputStream(file);
		try {
			stream.write(damaged);
		} finally {
			stream.close();
		}
		ReadResult corrupted = RunLogReader.read(file);
		assertEquals(ReadStatus.CORRUPT, corrupted.status);
		assertEquivalent(RunLogReader.parse(damaged), corrupted);
	}

	@Test
	public void streamingChecksumReusePreservesTruncatedChecksumRefusal() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(LogHeader.encode(TelemetryVersion.CURRENT, 1234, "crc-truncated", "test"));
		for (int i = 0; i < 128; i++){
			byte[] payload = new byte[2 + (i % 31)];
			Arrays.fill(payload, (byte) (i * 17));
			out.write(LogFormat.frame(250, payload));
		}
		byte[] complete = out.toByteArray();
		for (int missingChecksumBytes = 1; missingChecksumBytes <= 4; missingChecksumBytes++){
			byte[] truncated = Arrays.copyOf(complete, complete.length - missingChecksumBytes);
			File file = new File(tmp.getRoot(), "crc-truncated-" + missingChecksumBytes + ".apdlog");
			FileOutputStream stream = new FileOutputStream(file);
			try {
				stream.write(truncated);
			} finally {
				stream.close();
			}
			ReadResult streamed = RunLogReader.read(file);
			assertEquals(ReadStatus.TRUNCATED, streamed.status);
			assertEquivalent(RunLogReader.parse(truncated), streamed);
		}
	}

	@Test
	public void streamedFrameLengthVarintBoundariesMatchBufferedReader() throws Exception {
		File file = new File(tmp.getRoot(), "frame-length-boundaries.apdlog");
		int[] sizes = {0, 1, 126, 127, 128, 16383, 16384, 65535, 65536};
		FileOutputStream out = new FileOutputStream(file);
		try {
			out.write(LogHeader.encode(TelemetryVersion.CURRENT, 1234, "varint-boundaries", "test"));
			for (int i = 0; i < sizes.length; i++){
				byte[] payload = new byte[sizes[i]];
				Arrays.fill(payload, (byte) (i * 13));
				out.write(LogFormat.frame(250, payload)); // Unknown frame still requires CRC/length validation.
			}
			out.write(LogFormat.sealFrame(0, 0));
		} finally {
			out.close();
		}
		ReadResult buffered = RunLogReader.parse(LogTestSupport.bytes(file));
		ReadResult streamed = RunLogReader.read(file);
		assertEquals(ReadStatus.SEALED, buffered.status);
		assertEquals(sizes.length, buffered.skippedUnknownFrames);
		assertEquals(sizes.length, streamed.skippedUnknownFrames);
		assertEquivalent(buffered, streamed);
	}

	@Test
	public void malformedStreamedLengthVarintsRetainExistingTruncationResults() throws Exception {
		// A missing final length byte, a ten-byte continuation and an overflowing
		// tenth byte must all match the legacy byte-array parser's refusal.
		int[][] malformed = {
			{0x80},
			{0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80},
			{0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x02}
		};
		for (int i = 0; i < malformed.length; i++){
			File file = new File(tmp.getRoot(), "malformed-varint-" + i + ".apdlog");
			FileOutputStream out = new FileOutputStream(file);
			try {
				out.write(LogHeader.encode(TelemetryVersion.CURRENT, 1234, "bad-varint-" + i, "test"));
				out.write(LogFormat.SYNC);
				out.write(LogFormat.FRAME_EVENT);
				for (int b : malformed[i]) out.write(b);
			} finally {
				out.close();
			}
			ReadResult buffered = RunLogReader.parse(LogTestSupport.bytes(file));
			ReadResult streamed = RunLogReader.read(file);
			assertEquals(ReadStatus.TRUNCATED, buffered.status);
			assertEquivalent(buffered, streamed);
		}
	}


	@Test
	public void streamedHeaderExactLengthsMatchBufferedReaderUpToMaximum() throws Exception {
		int[] buildLengths = {0, 1, 127, 128, 2048, 4089};
		for (int buildLength : buildLengths){
			StringBuilder build = new StringBuilder();
			for (int i = 0; i < buildLength; i++) build.append('b');
			byte[] header = LogHeader.encode(TelemetryVersion.CURRENT, 1, "x", build.toString());
			if (buildLength == 4089){
				assertEquals(LogFormat.MAX_HEADER_BODY + LogFormat.MAGIC.length + 7, header.length);
			}
			byte[] seal = LogFormat.sealFrame(0, 0);
			File file = new File(tmp.getRoot(), "header-" + buildLength + ".apdlog");
			FileOutputStream out = new FileOutputStream(file);
			try {
				out.write(header);
				out.write(seal);
			} finally {
				out.close();
			}
			ReadResult buffered = RunLogReader.parse(LogTestSupport.bytes(file));
			ReadResult streamed = RunLogReader.read(file);
			assertEquals(ReadStatus.SEALED, streamed.status);
			assertEquals(header.length, streamed.header.length);
			assertEquals(build.toString(), streamed.header.writerBuild);
			assertEquivalent(buffered, streamed);
		}
	}

	@Test
	public void streamedHeaderTruncationAndFutureFormatMatchBufferedReader() throws Exception {
		StringBuilder build = new StringBuilder();
		for (int i = 0; i < 2048; i++) build.append('h');
		byte[] header = LogHeader.encode(TelemetryVersion.CURRENT, 1234, "header-trunc", build.toString());
		int[] endOffsets = {0, 1, 4, 5, 6, 7, 8, 24, header.length - 5,
				header.length - 4, header.length - 3, header.length - 1};
		for (int i = 0; i < endOffsets.length; i++){
			byte[] truncated = Arrays.copyOf(header, endOffsets[i]);
			File file = new File(tmp.getRoot(), "header-cut-" + i + ".apdlog");
			FileOutputStream out = new FileOutputStream(file);
			try {
				out.write(truncated);
			} finally {
				out.close();
			}
			ReadResult buffered = RunLogReader.parse(truncated);
			ReadResult streamed = RunLogReader.read(file);
			assertEquals(ReadStatus.BAD_HEADER, streamed.status);
			assertEquivalent(buffered, streamed);
		}

		byte[] future = Arrays.copyOf(header, header.length);
		future[LogFormat.MAGIC.length] = (byte) (LogFormat.CONTAINER_FORMAT + 1);
		File file = new File(tmp.getRoot(), "future-header.apdlog");
		FileOutputStream out = new FileOutputStream(file);
		try {
			out.write(future);
		} finally {
			out.close();
		}
		ReadResult streamed = RunLogReader.read(file);
		assertEquals(ReadStatus.UNSUPPORTED_CONTAINER, streamed.status);
		assertEquivalent(RunLogReader.parse(future), streamed);
	}


	@Test
	public void largeUnknownFramesPreserveKnownEventsAndStreamedOffsets() throws Exception {
		File file = new File(tmp.getRoot(), "large-unknown-frames.apdlog");
		byte[] ignored = new byte[4 * 1024 * 1024];
		for (int i = 0; i < ignored.length; i++) ignored[i] = (byte) (i * 37);
		FileOutputStream out = new FileOutputStream(file);
		try {
			out.write(LogHeader.encode(TelemetryVersion.CURRENT, 1234, "unknown-boundary", "test"));
			out.write(LogFormat.frame(250, ignored));
			out.write(LogFormat.stringFrame(0, "kept"));
			out.write(LogFormat.eventFrame(LogTestSupport.T_PLAIN, 3, 5, new byte[]{4, 5, 6}));
			// A second large unknown frame must reuse the same bounded scratch area.
			out.write(LogFormat.frame(251, ignored));
			out.write(LogFormat.sealFrame(1, 5));
		} finally {
			out.close();
		}

		byte[] bytes = LogTestSupport.bytes(file);
		ReadResult buffered = RunLogReader.parse(bytes);
		ReadResult streamed = RunLogReader.read(file);
		ReadResult identity = RunLogReader.read(file, new ReadOptions().onlyRunStartEvents(true));
		assertEquals(ReadStatus.SEALED, streamed.status);
		assertEquals(2, streamed.skippedUnknownFrames);
		assertEquals(1, streamed.eventCount);
		assertEquals(1, streamed.events.size());
		assertEquals(1, streamed.strings.size());
		assertEquals("kept", streamed.strings.get(0));
		assertEquals(bytes.length, streamed.validBytes);
		assertEquivalent(buffered, streamed);
		assertEquals(ReadStatus.SEALED, identity.status);
		assertEquals(2, identity.skippedUnknownFrames);
		assertEquals(1, identity.eventCount);
		assertEquals(0, identity.events.size());
		assertEquals(buffered.validBytes, identity.validBytes);
	}

	@Test
	public void unknownFramesStillRejectCorruptCrcAndTruncatedPayloadOrChecksum() throws Exception {
		byte[] header = LogHeader.encode(TelemetryVersion.CURRENT, 1234, "unknown-errors", "test");
		byte[] unknown = LogFormat.frame(250, new byte[256 * 1024]);
		byte[] damaged = Arrays.copyOf(unknown, unknown.length);
		damaged[damaged.length - 1] ^= 0x40;
		File file = new File(tmp.getRoot(), "unknown-bad-checksum.apdlog");
		FileOutputStream out = new FileOutputStream(file);
		try {
			out.write(header);
			out.write(damaged);
			out.write(LogFormat.sealFrame(0, 0));
		} finally {
			out.close();
		}
		ReadResult checksumBad = RunLogReader.read(file);
		assertEquals(ReadStatus.CORRUPT, checksumBad.status);
		assertEquals(0, checksumBad.skippedUnknownFrames);
		assertEquivalent(RunLogReader.parse(LogTestSupport.bytes(file)), checksumBad);

		for (int removed = 1; removed <= 5; removed++){
			File shortFile = new File(tmp.getRoot(), "unknown-truncated-" + removed + ".apdlog");
			out = new FileOutputStream(shortFile);
			try {
				out.write(header);
				out.write(unknown, 0, unknown.length - removed);
			} finally {
				out.close();
			}
			byte[] bytes = LogTestSupport.bytes(shortFile);
			ReadResult buffered = RunLogReader.parse(bytes);
			ReadResult streamed = RunLogReader.read(shortFile);
			assertEquals(ReadStatus.TRUNCATED, streamed.status);
			assertEquals(0, streamed.skippedUnknownFrames);
			assertEquivalent(buffered, streamed);
		}
	}


	@Test
	public void bufferedForwardOnlyReadsMatchParserAcrossRefillsAndDamage() throws Exception {
		// Force many internal 8 KiB buffer refills, with tiny unknown frames
		// interleaved with real events and a final seal. The stream must not
		// change the parser's byte positions or checksum/identity semantics.
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(LogHeader.encode(TelemetryVersion.CURRENT, 1234, "buffered-refills", "test"));
		int events = 0;
		for (int i = 0; i < 900; i++){
			byte[] noise = new byte[31 + (i % 43)];
			Arrays.fill(noise, (byte) (i * 13));
			out.write(LogFormat.frame(250, noise));
			if (i % 17 == 0){
				out.write(LogFormat.eventFrame(LogTestSupport.T_PLAIN, i & 1,
						2, new byte[]{(byte) i, (byte) (i >>> 8)}));
				events++;
			}
		}
		out.write(LogFormat.sealFrame(events, events * 2));
		byte[] bytes = out.toByteArray();
		File file = new File(tmp.getRoot(), "buffered-refills.apdlog");
		FileOutputStream stream = new FileOutputStream(file);
		try {
			stream.write(bytes);
		} finally {
			stream.close();
		}
		ReadResult ordinary = RunLogReader.read(file);
		assertEquals(ReadStatus.SEALED, ordinary.status);
		assertEquals(900, ordinary.skippedUnknownFrames);
		assertEquals(events, ordinary.eventCount);
		assertEquals(bytes.length, ordinary.validBytes);
		assertEquivalent(RunLogReader.parse(bytes), ordinary);

		ReadOptions identity = new ReadOptions().onlyRunStartEvents(true);
		ReadResult identityStream = RunLogReader.read(file, identity);
		assertEquals(ReadStatus.SEALED, identityStream.status);
		assertEquals(0, identityStream.events.size());
		assertEquals(events, identityStream.eventCount);
		assertEquivalent(RunLogReader.parse(bytes, new ReadOptions().onlyRunStartEvents(true)),
				identityStream);

		// A buffered read must never treat bytes prefetched beyond the logical
		// read position as a complete frame, especially near refill boundaries.
		int[] cuts = {8191, 8192, 8193, 16383, 16384, 16385, bytes.length - 1};
		for (int cut : cuts){
			byte[] truncated = Arrays.copyOf(bytes, cut);
			File shortFile = new File(tmp.getRoot(), "buffered-cut-" + cut + ".apdlog");
			stream = new FileOutputStream(shortFile);
			try {
				stream.write(truncated);
			} finally {
				stream.close();
			}
			assertEquivalent(RunLogReader.parse(truncated), RunLogReader.read(shortFile));
		}

		byte[] damaged = Arrays.copyOf(bytes, bytes.length);
		damaged[16384] ^= 0x40;
		File corrupt = new File(tmp.getRoot(), "buffered-corrupt.apdlog");
		stream = new FileOutputStream(corrupt);
		try {
			stream.write(damaged);
		} finally {
			stream.close();
		}
		assertEquivalent(RunLogReader.parse(damaged), RunLogReader.read(corrupt));
	}

	private static void assertEquivalent( ReadResult expected, ReadResult actual ){
		assertEquals(expected.status, actual.status);
		assertEquals(expected.problem, actual.problem);
		assertEquals(expected.fileLength, actual.fileLength);
		assertEquals(expected.validBytes, actual.validBytes);
		assertEquals(expected.sealed, actual.sealed);
		assertEquals(expected.eventCount, actual.eventCount);
		assertEquals(expected.lastClock, actual.lastClock);
		assertEquals(expected.strings, actual.strings);
		assertEquals(expected.events.size(), actual.events.size());
		for (int i = 0; i < expected.events.size(); i++){
			ApdEvent left = expected.events.get(i);
			ApdEvent right = actual.events.get(i);
			assertEquals(left.index, right.index);
			assertEquals(left.type, right.type);
			assertEquals(left.flags, right.flags);
			assertEquals(left.clock, right.clock);
			assertArrayEquals(left.payload, right.payload);
		}
		if (expected.header == null){
			assertNull(actual.header);
		} else {
			assertEquals(expected.header.containerFormat, actual.header.containerFormat);
			assertEquals(expected.header.telemetryVersion, actual.header.telemetryVersion);
			assertEquals(expected.header.createdAtMillis, actual.header.createdAtMillis);
			assertEquals(expected.header.runId, actual.header.runId);
			assertEquals(expected.header.writerBuild, actual.header.writerBuild);
			assertEquals(expected.header.length, actual.header.length);
		}
	}
}
