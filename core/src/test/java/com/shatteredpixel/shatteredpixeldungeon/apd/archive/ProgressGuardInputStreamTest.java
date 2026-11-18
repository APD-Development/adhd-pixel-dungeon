package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProgressGuardInputStreamTest {
	@Test public void passesNormalReadsAndEndOfStream() throws IOException {
		ProgressGuardInputStream in = new ProgressGuardInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3}));
		byte[] buf = new byte[8];
		assertEquals("asking for nothing is fine", 0, in.read(buf, 0, 0));
		assertEquals(3, in.read(buf, 0, 8));
		assertEquals(-1, in.read(buf, 0, 8));
	}

	@Test public void aSourceThatAnswersZeroForeverFails(){
		InputStream stuck = new InputStream(){
			@Override public int read(){ return 0; }
			@Override public int read(byte[] b, int off, int len){ return 0; }
		};
		try {
			new ProgressGuardInputStream(stuck).read(new byte[4], 0, 4);
			fail("a zero-byte answer must not be accepted");
		} catch (IOException expected){
			assertTrue(expected.getMessage().contains("stopped answering"));
		}
	}
}
