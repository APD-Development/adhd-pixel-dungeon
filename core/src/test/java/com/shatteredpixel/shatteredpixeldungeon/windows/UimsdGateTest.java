package com.shatteredpixel.shatteredpixeldungeon.windows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.demo.UimsdUnlock;

import java.io.File;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.files.FileHandle;
import com.watabou.utils.GameSettings;

//The demo-unlock warning is the owner's own wording; it must not drift.
public class UimsdGateTest {
	@Rule public TemporaryFolder preferencesDirectory = new TemporaryFolder();
	private Preferences previous;
	@Before public void isolatePreferences() throws Exception {
		java.lang.reflect.Field prefs = GameSettings.class.getDeclaredField("prefs");
		prefs.setAccessible(true);
		previous = (Preferences) prefs.get(null);
		GameSettings.set(new HeadlessPreferences(new FileHandle(new File(preferencesDirectory.getRoot(), "settings.xml"))));
	}
	@After public void restorePreferences(){ GameSettings.set(previous); }

	@Test
	public void theWarningAndBothChoicesAreWordForWord(){
		assertEquals("ARE YOU SURE ABOUT THIS?", UimsdGate.WARNING_TITLE);
		assertEquals("UIMSD is an absurdly detailed statistics database. You can unlock it immediately with 10 demo runs, but there's a very good chance you'll have absolutely no idea what you're looking at.\n\nI strongly recommend playing 10 actual runs first.",
				UimsdGate.WARNING_TEXT);
		assertEquals("I LIKE CONFUSION", UimsdGate.CHOICE_CONFUSION);
		assertEquals("I'LL PLAY 10 RUNS", UimsdGate.CHOICE_PLAY);
		assertEquals("THIS BUTTON IS EFFECTIVELY USELESS TILL 10 RUNS HAVE BEEN COMPLETED.", UimsdGate.LOCKED_ARROW_TEXT);
		assertEquals(10, UimsdUnlock.DEMO_RUNS);
		assertEquals(10, UimsdUnlock.TEN_RUNS);
	}

	@Test
	public void theGateIsOnInEveryBuild(){
		com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.resetForTest();
		assertFalse(com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.demoUnlock());
		assertTrue("Alpha and Play builds have the gate too (Chris, Nov 18)", UimsdUnlock.enforced());
	}

	private static final class Mem implements UimsdUnlock.Flags {
		boolean unlocked, demoUsed, tour, pending, shown;
		public boolean unlocked(){ return unlocked; }
		public void setUnlocked(){ unlocked = true; }
		public boolean demoUsed(){ return demoUsed; }
		public void setDemoUsed(){ demoUsed = true; }
		public boolean tourUnlocked(){ return tour; }
		public void setTourUnlocked(){ tour = true; }
		public boolean promptPending(){ return pending; }
		public void setPromptPending(){ pending = true; }
		public boolean promptShown(){ return shown; }
		public void setPromptShown(){ shown = true; }
	}

	@Test
	public void theArrowSaysTheLockedMessageThenTheHumorMessageUntilUimsdIsOpenedThenItsUsualText() throws Exception {
		File empty = java.nio.file.Files.createTempDirectory("apd-arrow").toFile();
		String locked = "THIS BUTTON IS EFFECTIVELY USELESS TILL 10 RUNS HAVE BEEN COMPLETED.";
		Mem flags = new Mem();
		assertEquals("fewer than ten runs: the locked message, exactly", locked, UimsdGate.annotationFor(flags, empty, true, false, "USUAL"));
		assertEquals("in Adult mode too", locked, UimsdGate.annotationFor(flags, empty, true, true, "USUAL"));
		assertEquals("gate off: always the usual text", "USUAL", UimsdGate.annotationFor(flags, empty, false, false, "USUAL"));

		//the tenth genuine run queued the arrow: Clean and Adult each get their own message, and it stays for every build of the title
		flags.setUnlocked();
		flags.setTourUnlocked();
		flags.setPromptPending();
		for (int i = 0; i < 3; i++){
			assertEquals("HEY, GO HERE FOR UNLIMITED DOPAMINE.", UimsdGate.annotationFor(flags, empty, true, false, "USUAL"));
			assertEquals("HEY, GO HERE FOR DRUGS.", UimsdGate.annotationFor(flags, empty, true, true, "USUAL"));
		}
		//opening UIMSD retires it for good
		assertTrue(UimsdUnlock.retirePrompt(flags));
		assertEquals(UimsdGate.TENTH_RUN_ARROW_CLEAN, UimsdGate.annotationFor(flags, empty, true, false, "USUAL"));
		assertEquals(UimsdGate.TENTH_RUN_ARROW_ADULT, UimsdGate.annotationFor(flags, empty, true, true, "USUAL"));

		//Demo access before the tenth genuine run still shows the instructional notice.
		Mem demo = new Mem();
		demo.setUnlocked();
		demo.setDemoUsed();
		assertEquals(locked, UimsdGate.annotationFor(demo, empty, true, true, "USUAL"));
		//the tenth genuine run comes later: the arrow appears for that player too
		demo.setTourUnlocked();
		demo.setPromptPending();
		assertEquals("HEY, GO HERE FOR DRUGS.", UimsdGate.annotationFor(demo, empty, true, true, "USUAL"));
	}

	@Test
	public void theHumorSettingUsesTheAgeDefaultAndTheStoredChoice() throws Exception {
		String settings = new String(java.nio.file.Files.readAllBytes(new File(System.getProperty("apd.game.src"), "SPDSettings.java").toPath()), "UTF-8");
		assertTrue(settings.contains("AgeEligibility.adultHumor("));
		assertTrue(settings.contains("AgeEligibility.current()"));
		assertTrue(settings.contains("AgeEligibility.defaultAdultHumor(age)"));
	}

}
