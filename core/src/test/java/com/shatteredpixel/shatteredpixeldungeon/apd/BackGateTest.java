package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.watabou.input.BackGate;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

//One press of Back is one Back, however many routes deliver it; two real presses are two.
public class BackGateTest {

	private static final long MS = 1_000_000L;

	@Before @After public void clearState(){
		BackGate.setSystemBackOwner(null);
		BackGate.reset();
	}

	@Test
	public void theSamePressByTwoRoutesCountsOnce(){
		assertTrue(BackGate.accept(1000 * MS));
		assertFalse("the second route delivering the same press", BackGate.accept(1000 * MS + 5 * MS));
		assertFalse(BackGate.accept(1000 * MS + 60 * MS));
	}

	@Test
	public void twoRealPressesAreTwoBacks(){
		assertTrue(BackGate.accept(5000 * MS));
		assertTrue("a person cannot press twice this fast, so this is a second press", BackGate.accept(5000 * MS + 200 * MS));
		assertTrue(BackGate.accept(5000 * MS + 600 * MS));
	}

	@Test
	public void theFirstPressIsAlwaysAccepted(){
		assertTrue(BackGate.accept(0));
	}

	@Test public void delayedLegacyCopyCannotCloseTheReturnedTitle(){
		BackGate.setSystemBackOwner(new Object());
		assertTrue(BackGate.acceptSystem(1000 * MS));
		assertFalse("Observed API36 duplicate arrived 356ms later", BackGate.acceptLegacy(1356 * MS, 999 * MS));
		assertTrue("A new backend timestamp identifies a genuine independent fallback", BackGate.acceptLegacy(6000 * MS, 6000 * MS));
	}

	@Test public void registeredCallbackKeepsLegacyFallbackUntilSystemDelivery(){
		BackGate.setSystemBackOwner(new Object());
		assertTrue("Registration alone must not disable Back", BackGate.acceptLegacy(1000 * MS));
		assertFalse("Ordinary timing deduplication remains", BackGate.acceptLegacy(1005 * MS));
		assertTrue(BackGate.acceptLegacy(1200 * MS));
	}

	@Test public void eachSystemPressSuppressesOnlyItsOwnLegacyCopy(){
		BackGate.setSystemBackOwner(new Object());
		assertTrue(BackGate.acceptSystem(1000 * MS));
		assertFalse(BackGate.acceptLegacy(1356 * MS, 999 * MS));
		assertTrue(BackGate.acceptSystem(1600 * MS));
		assertFalse(BackGate.acceptLegacy(1956 * MS, 1599 * MS));
		assertTrue("A later fallback still works", BackGate.acceptLegacy(2200 * MS));
	}

	@Test public void legacyRouteWorksWithoutModernAndroidCallback(){
		assertTrue(BackGate.acceptLegacy(1000 * MS));
		assertFalse(BackGate.acceptLegacy(1005 * MS));
		assertTrue(BackGate.acceptLegacy(1200 * MS));
	}

	@Test public void oldActivityCannotClearNewActivityOwnership(){
		Object oldActivity=new Object(),newActivity=new Object();
		BackGate.setSystemBackOwner(oldActivity);
		BackGate.setSystemBackOwner(newActivity);
		BackGate.clearSystemBackOwner(oldActivity);
		assertTrue(BackGate.systemBackOwned());
		assertTrue("New owner registration still keeps fallback available", BackGate.acceptLegacy(1000 * MS));
		BackGate.clearSystemBackOwner(newActivity);
		assertFalse(BackGate.systemBackOwned());
		assertTrue(BackGate.acceptLegacy(1200 * MS));
	}

	@Test public void inputTimingResetDoesNotReleaseRegisteredSystemRoute(){
		BackGate.setSystemBackOwner(new Object());
		assertTrue(BackGate.acceptSystem(1000 * MS));
		BackGate.reset();
		assertTrue(BackGate.systemBackOwned());
		assertTrue("Reset clears stale delivery suppression but not callback ownership", BackGate.acceptLegacy(0));
	}

	@Test public void distinctSystemPressesKeepOriginalTimingWithoutExtraLatency(){
		BackGate.setSystemBackOwner(new Object());
		assertTrue(BackGate.acceptSystem(1000 * MS));
		assertTrue(BackGate.acceptSystem(1120 * MS));
		assertTrue(BackGate.acceptSystem(1240 * MS));
	}

 @Test public void missingDuplicateDoesNotConsumeANewIndependentFallback(){
  BackGate.setSystemBackOwner(new Object());
  assertTrue(BackGate.acceptSystem(1000*MS));
  assertTrue("No old-copy debt can swallow a genuine new press",BackGate.acceptLegacy(6000*MS,6000*MS));
 }
 @Test public void completedBackendPressRejectsOldCopiesButAllowsFreshPresses(){
  BackGate.setSystemBackOwner(new Object());
  assertTrue(BackGate.acceptLegacy(1000*MS,990*MS));
  assertFalse("An already accepted backend DOWN is not a new press",BackGate.acceptLegacy(1400*MS,990*MS));
  BackGate.recordLegacyRelease(1010*MS);
  assertFalse(BackGate.acceptLegacy(1400*MS,1010*MS));
  assertFalse(BackGate.acceptLegacy(6000*MS,1010*MS));
  assertTrue(BackGate.acceptLegacy(6200*MS,6190*MS));
 }
 @Test public void unknownBackendTimeNeverConsumesAFuturePressOnAnUnprovenDuplicate(){
  BackGate.setSystemBackOwner(new Object());
  assertTrue(BackGate.acceptSystem(1000*MS));
  assertTrue(BackGate.acceptLegacy(6000*MS,0));
 }
}
