package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class LogTestSupport {

	private LogTestSupport(){}

	//event types used by tests, all far from the registered ones (so they count as "unknown" to the registry)
	static final int T_PLAIN = 5000;
	static final int T_STRINGS = 5001;
	static final int T_FUTURE = 9999;

	static byte[] bytes( File f ) throws IOException {
		return RunLogReader.readFully(f);
	}

	static void write( File f, byte[] data ) throws IOException {
		FileOutputStream out = new FileOutputStream(f);
		try {
			out.write(data);
		} finally {
			out.close();
		}
	}

	static byte[] concat( byte[]... parts ){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (byte[] p : parts) out.write(p, 0, p.length);
		return out.toByteArray();
	}

	//a log built directly from frames, no writer thread involved
	static byte[] build( TelemetryVersion v, String runId, byte[]... frames ){
		return concat(LogHeader.encode(v, 1000, runId, "test"), concat(frames));
	}

	//n simple events, event i has clock 10*i and payload = varint i
	static List<byte[]> plainFrames( int n ){
		List<byte[]> frames = new ArrayList<>();
		frames.add(LogFormat.clockFrame(0));
		long last = 0;
		for (int i = 0; i < n; i++){
			long c = 10L * i;
			frames.add(LogFormat.eventFrame(T_PLAIN, 0, c - last, new ByteSink().writeUnsigned(i).toByteArray()));
			last = c;
		}
		return frames;
	}

	//writes n events (each with a string) through the real writer and leaves the log unsealed
	static File writeLog( File dir, String runId, int n, boolean seal ) throws IOException {
		File f = new File(dir, runId + ".apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, runId, "test", 1000);
		assertEquals(RunLogWriter.OpenStatus.OK, w.openStatus());
		for (int i = 0; i < n; i++){
			w.event(T_STRINGS).clock(10L * i).unsigned(i).string("s" + (i % 7)).signed(-i).emit();
		}
		w.close(seal);
		return f;
	}

	static void assertPrefix( List<ApdEvent> expected, List<ApdEvent> actual ){
		org.junit.Assert.assertTrue("got more events than were written", actual.size() <= expected.size());
		for (int i = 0; i < actual.size(); i++){
			ApdEvent e = expected.get(i), a = actual.get(i);
			assertEquals("type at " + i, e.type, a.type);
			assertEquals("clock at " + i, e.clock, a.clock);
			org.junit.Assert.assertArrayEquals("payload at " + i, e.payload, a.payload);
		}
	}
}
