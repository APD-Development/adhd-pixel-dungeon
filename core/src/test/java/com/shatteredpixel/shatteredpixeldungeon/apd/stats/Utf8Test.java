package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.*;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class Utf8Test {
	@Test public void asciiAndValidUtf8Decode(){
		assertEquals("rat", Utf8.decode("rat".getBytes(StandardCharsets.US_ASCII)));
		assertEquals("", Utf8.decode(new byte[0]));
		assertEquals("Grüße 🐀", Utf8.decode("Grüße 🐀".getBytes(StandardCharsets.UTF_8)));
	}

	@Test(expected = FormatException.class)
	public void damagedBytesAreRefusedNotReplaced(){
		Utf8.decode(new byte[]{'a', (byte) 0xC3});
	}

	@Test(expected = FormatException.class)
	public void overlongEncodingIsRefused(){
		Utf8.decode(new byte[]{(byte) 0xC0, (byte) 0xAF});
	}
}
