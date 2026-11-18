package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class TelemetryVersionTest {

	@Test
	public void currentVersionIsTelemetry1Point16(){
		assertEquals(new TelemetryVersion(1, 16), TelemetryVersion.CURRENT);
		assertEquals("1.16", TelemetryVersion.CURRENT.toString());
	}

	@Test
	public void parsesAndPrints(){
		assertEquals(new TelemetryVersion(1, 3), TelemetryVersion.parse("1.3"));
		assertEquals(new TelemetryVersion(2, 0), TelemetryVersion.parse("telemetry 2.0"));
		assertEquals(new TelemetryVersion(1, 12), TelemetryVersion.parse(" 1.12 "));
		assertEquals("1.14", TelemetryVersion.parse("1.14").toString());
	}

	@Test
	public void rejectsGarbage(){
		for (String s : new String[]{"", "1", "1.", ".1", "a.b", "1.2.3x", "-1.0", null, "65536.0"}){
			try {
				TelemetryVersion.parse(s);
				fail("accepted " + s);
			} catch (IllegalArgumentException expected){
				//good
			}
		}
	}

	@Test
	public void ordersMajorBeforeMinor(){
		TelemetryVersion v10 = new TelemetryVersion(1, 0), v13 = new TelemetryVersion(1, 3), v20 = new TelemetryVersion(2, 0);
		assertTrue(v10.compareTo(v13) < 0);
		assertTrue(v13.compareTo(v20) < 0);
		assertTrue(v20.isNewerThan(v13));
		assertFalse(v10.isNewerThan(v13));
		assertFalse(v13.isNewerThan(v13));
		assertTrue(v20.isIncompatibleMajor(v13));
		assertFalse(v13.isIncompatibleMajor(v10));
		assertNotEquals(v10, v13);
		assertEquals(v13, new TelemetryVersion(1, 3));
		assertEquals(v13.hashCode(), new TelemetryVersion(1, 3).hashCode());
	}
}
