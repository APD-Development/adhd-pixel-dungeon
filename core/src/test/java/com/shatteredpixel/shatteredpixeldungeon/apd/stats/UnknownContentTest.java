package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

//An older reader must be able to step over event types and frame types that were added after it was written.
public class UnknownContentTest {

	private static byte[] event( int type, long delta, long value ){
		return LogFormat.eventFrame(type, 0, delta, new ByteSink().writeUnsigned(value).toByteArray());
	}

	@Test
	public void unknownEventTypesAreDeliveredAndCounted(){
		byte[] data = LogTestSupport.build(TelemetryVersion.CURRENT, "u1",
				event(EventTypes.GAP, 0, 1), event(LogTestSupport.T_FUTURE, 5, 2), event(LogTestSupport.T_PLAIN, 5, 3),
				LogFormat.sealFrame(3, 10));
		ReadResult r = RunLogReader.parse(data);
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(3, r.events.size());
		assertEquals(LogTestSupport.T_FUTURE, r.events.get(1).type);
		assertFalse(EventTypes.isKnown(LogTestSupport.T_FUTURE));
	}

	@Test
	public void knownTypesOnlyLeavesUnknownEventsOutButStillCountsThem(){
		byte[] data = LogTestSupport.build(TelemetryVersion.CURRENT, "u2",
				event(EventTypes.GAP, 0, 1), event(LogTestSupport.T_FUTURE, 5, 2), event(LogTestSupport.T_FUTURE + 1, 5, 3),
				event(EventTypes.GAP, 5, 4), LogFormat.sealFrame(4, 15));
		ReadResult r = RunLogReader.parse(data, new ReadOptions().knownTypesOnly(true));
		assertEquals(ReadStatus.SEALED, r.status); //the seal count still matches, skipped events were counted
		assertEquals(2, r.events.size());
		assertEquals(4, r.eventCount);
		assertEquals(2, r.skippedUnknownEvents);
		//the clock still advanced through the skipped events
		assertEquals(15, r.events.get(1).clock);
	}

	@Test
	public void unknownFrameTypesAreSkipped(){
		byte[] futureFrame = LogFormat.frame(200, new byte[]{1, 2, 3, 4, 5});
		byte[] emptyFrame = LogFormat.frame(77, new byte[0]);
		byte[] data = LogTestSupport.build(TelemetryVersion.CURRENT, "u3",
				event(LogTestSupport.T_PLAIN, 0, 1), futureFrame, event(LogTestSupport.T_PLAIN, 1, 2), emptyFrame,
				event(LogTestSupport.T_PLAIN, 1, 3), LogFormat.sealFrame(3, 2));
		ReadResult r = RunLogReader.parse(data);
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(3, r.events.size());
		assertEquals(2, r.skippedUnknownFrames);
		assertEquals(0, r.damagedBytes());
	}

	@Test
	public void unknownTrailingHeaderBytesAreIgnored(){
		//build a header with extra bytes after the known fields, like a later container revision might add
		ByteSink body = new ByteSink();
		body.writeUnsigned(1).writeUnsigned(0).writeUnsigned(5).writeString("h1").writeString("b");
		body.writeBytes(new byte[]{9, 9, 9, 9});
		ByteSink out = new ByteSink();
		out.writeBytes(LogFormat.MAGIC).writeByte(LogFormat.CONTAINER_FORMAT).writeByte(body.size() >>> 8).writeByte(body.size());
		out.writeBytes(body.toByteArray());
		out.writeInt32((int) LogFormat.crc(out.rawBuffer(), 4, out.size() - 4));
		byte[] data = LogTestSupport.concat(out.toByteArray(), event(LogTestSupport.T_PLAIN, 0, 1));
		ReadResult r = RunLogReader.parse(data);
		assertTrue(r.status.isClean());
		assertEquals("h1", r.header.runId);
		assertEquals(1, r.events.size());
	}
}
