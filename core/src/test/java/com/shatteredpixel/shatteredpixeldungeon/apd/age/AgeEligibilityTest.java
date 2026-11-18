package com.shatteredpixel.shatteredpixeldungeon.apd.age;

import org.junit.Test;
import static org.junit.Assert.*;

public class AgeEligibilityTest {
	@Test public void eligibleAdultsStillChooseWhetherToEnableHumor(){
		assertTrue(AgeEligibility.adultHumor(AgeEligibility.State.ELIGIBLE, true));
		assertFalse(AgeEligibility.adultHumor(AgeEligibility.State.ELIGIBLE, false));
	}
	@Test public void ineligiblePlayersNeverReceiveAdultHumor(){
		assertFalse(AgeEligibility.adultHumor(AgeEligibility.State.INELIGIBLE, true));
	}
	@Test public void unknownAgeAllowsAnExplicitAdultChoiceWithoutClaimingVerification(){
		assertTrue(AgeEligibility.adultHumor(AgeEligibility.State.UNKNOWN, true));
		assertTrue(AgeEligibility.adultHumor(null, true));
		assertFalse(AgeEligibility.adultHumor(AgeEligibility.State.UNKNOWN, false));
	}
	@Test public void unavailableVerificationAllowsThePlayersChoice(){
		assertTrue(AgeEligibility.adultHumor(AgeEligibility.State.UNAVAILABLE, true));
		assertFalse(AgeEligibility.adultHumor(AgeEligibility.State.UNAVAILABLE, false));
		assertEquals(AgeEligibility.State.UNAVAILABLE, AgeEligibility.current());
	}
	@Test public void everyoneDefaultsToCleanHumor(){
		for (AgeEligibility.State state : AgeEligibility.State.values()) {
			assertFalse(state.toString(), AgeEligibility.defaultAdultHumor(state));
			assertFalse(state.toString(), AgeEligibility.adultHumor(state, AgeEligibility.defaultAdultHumor(state)));
		}
		assertFalse(AgeEligibility.defaultAdultHumor(null));
	}
	@Test public void adultHumorIsStillAChoiceForAnyoneNotKnownUnderage(){
		assertTrue(AgeEligibility.adultHumor(AgeEligibility.State.ELIGIBLE, true));
		assertTrue(AgeEligibility.adultHumor(AgeEligibility.State.UNAVAILABLE, true));
		assertFalse(AgeEligibility.adultHumor(AgeEligibility.State.INELIGIBLE, true));
		assertFalse(AgeEligibility.adultHumor(AgeEligibility.State.ELIGIBLE, false));
	}
	@Test public void settingsAndTitleAreWiredThroughThePolicy() throws Exception {
		java.io.File root = new java.io.File(System.getProperty("apd.game.src"));
		String settings = java.nio.file.Files.readString(new java.io.File(root, "SPDSettings.java").toPath());
		assertTrue(settings.contains("AgeEligibility.adultHumor("));
		assertTrue(settings.contains("AgeEligibility.permitsAdult("));
		String title = java.nio.file.Files.readString(new java.io.File(root, "scenes/TitleScene.java").toPath());
		assertFalse(title.contains("HEY, GO HERE FOR DRUGS"));
		assertTrue(title.contains("annotation.visible = !annotationText.isEmpty()"));
	}
}
