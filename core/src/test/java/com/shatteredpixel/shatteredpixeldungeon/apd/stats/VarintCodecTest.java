package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Random;

public class VarintCodecTest {

	@Test
	public void unsignedRoundTrip(){
		long[] values = {0, 1, 127, 128, 255, 16383, 16384, Integer.MAX_VALUE, 1L << 35, Long.MAX_VALUE};
		for (long v : values){
			ByteSink s = new ByteSink().writeUnsigned(v);
			ByteSource src = new ByteSource(s.toByteArray());
			assertEquals(v, src.readUnsigned());
			assertEquals(0, src.remaining());
		}
	}

	@Test
	public void signedRoundTripIncludingExtremes(){
		long[] values = {0, 1, -1, 63, -64, 64, -65, Long.MAX_VALUE, Long.MIN_VALUE, Integer.MIN_VALUE};
		for (long v : values){
			ByteSource src = new ByteSource(new ByteSink().writeSigned(v).toByteArray());
			assertEquals(v, src.readSigned());
		}
	}

	@Test
	public void smallMagnitudesStaySmall(){
		assertEquals(1, new ByteSink().writeSigned(-1).size());
		assertEquals(1, new ByteSink().writeSigned(63).size());
		assertEquals(1, new ByteSink().writeUnsigned(127).size());
		assertEquals(2, new ByteSink().writeUnsigned(128).size());
	}

	@Test
	public void randomValuesRoundTrip(){
		Random rnd = new Random(42);
		ByteSink s = new ByteSink();
		long[] u = new long[500], g = new long[500];
		for (int i = 0; i < 500; i++){
			u[i] = (rnd.nextLong() & Long.MAX_VALUE) >>> rnd.nextInt(63);
			g[i] = rnd.nextLong() >> rnd.nextInt(64);
			s.writeUnsigned(u[i]).writeSigned(g[i]);
		}
		ByteSource src = new ByteSource(s.toByteArray());
		for (int i = 0; i < 500; i++){
			assertEquals(u[i], src.readUnsigned());
			assertEquals(g[i], src.readSigned());
		}
	}

	@Test
	public void otherPrimitivesRoundTrip(){
		ByteSink s = new ByteSink().writeBoolean(true).writeBoolean(false).writeFloat(-1.5f).writeDouble(Math.PI)
				.writeInt32(-2).writeInt64(Long.MIN_VALUE).writeString("héllo 世界 😀");
		ByteSource src = new ByteSource(s.toByteArray());
		assertEquals(true, src.readBoolean());
		assertEquals(false, src.readBoolean());
		assertEquals(-1.5f, src.readFloat(), 0);
		assertEquals(Math.PI, src.readDouble(), 0);
		assertEquals(-2, src.readInt32());
		assertEquals(Long.MIN_VALUE, src.readInt64());
		assertEquals("héllo 世界 😀", src.readString());
	}

	@Test
	public void malformedInputThrowsFormatExceptionNeverAnythingElse(){
		byte[][] bad = {
				{},                                                 //nothing
				{(byte) 0x80},                                      //continuation with no next byte
				{(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
						(byte) 0x80, (byte) 0x80, (byte) 0x80, 0x01}, //too long
				{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
						(byte) 0xFF, (byte) 0xFF, 0x7F},             //overflows 64 bits
		};
		for (byte[] b : bad){
			try {
				new ByteSource(b).readUnsigned();
				fail("expected FormatException");
			} catch (FormatException expected){
				//good
			}
		}
		try { new ByteSource(new byte[2]).readInt32(); fail(); } catch (FormatException expected){}
		try { new ByteSource(new byte[]{5, 'a'}).readString(); fail(); } catch (FormatException expected){}
		try { new ByteSource(new byte[]{2}).readBoolean(); fail(); } catch (FormatException expected){}
		try { new ByteSink().writeUnsigned(-1); fail(); } catch (FormatException expected){}
	}
}
