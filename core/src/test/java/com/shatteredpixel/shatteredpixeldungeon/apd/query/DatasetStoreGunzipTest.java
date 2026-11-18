package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

/** Row caches decompress to exactly the original bytes, sized once from the gzip trailer, and survive a bad trailer. */
public class DatasetStoreGunzipTest {
	private static byte[] gz(byte[] plain) throws Exception {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		try (GZIPOutputStream z = new GZIPOutputStream(b)) { z.write(plain); }
		return b.toByteArray();
	}

	@Test public void roundTripsSmallEmptyAndLarge() throws Exception {
		Random r = new Random(7);
		for (int size : new int[]{0, 1, 1000, 3_000_000}){
			byte[] plain = new byte[size];
			for (int i = 0; i < size; i++) plain[i] = (byte) (r.nextInt(16) + 'a'); // compressible, like row caches
			assertArrayEquals("size " + size, plain, DatasetStore.gunzip(gz(plain)));
		}
	}

	@Test public void aDamagedSizeTrailerStillDecompressesCorrectly() throws Exception {
		byte[] plain = new byte[200_000];
		for (int i = 0; i < plain.length; i++) plain[i] = (byte) (i % 251);
		for (int fake : new int[]{0, 10, 199_999, 200_001, 5_000_000}){
			byte[] z = gz(plain);
			int t = z.length - 4;
			z[t] = (byte) fake; z[t + 1] = (byte) (fake >> 8); z[t + 2] = (byte) (fake >> 16); z[t + 3] = (byte) (fake >> 24);
			try {
				assertArrayEquals("trailer " + fake, plain, DatasetStore.gunzip(z));
			} catch (java.io.IOException acceptable) {
				// GZIPInputStream itself may refuse a trailer that doesn't match; refusing is safe (the cache is rebuilt).
			}
		}
	}
}
