package com.shatteredpixel.shatteredpixeldungeon.apd.settings;

import static org.junit.Assert.*;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.GameSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Chris's case (Nov 18): settings that used to be on by default must stay on for players who already had the game,
 * whether they never touched them or saved "on" themselves; a saved "off" stays off; fresh installs start off.
 */
public class OldDefaultsKeptTest {
	private Preferences old;
	private File file;

	@Before public void setUp() throws Exception {
		HeadlessTestSupport.start();
		java.lang.reflect.Field f = GameSettings.class.getDeclaredField("prefs");
		f.setAccessible(true);
		old = (Preferences) f.get(null);
		file = File.createTempFile("old-defaults-", ".xml", HeadlessTestSupport.tempDir());
		file.delete();
		GameSettings.set(new HeadlessPreferences(new FileHandle(file)));
	}

	@After public void tearDown(){ GameSettings.set(old); file.delete(); }

	private void reload(){ GameSettings.set(new HeadlessPreferences(new FileHandle(file))); }

	@Test public void existingPlayerKeepsEverythingThatWasOn(){
		SPDSettings.version(920);
		SPDSettings.dotOverlayEnabled(true);
		SPDSettings.autoCollectGrassDew(false);
		reload();
		SPDSettings.keepOldDefaultsForExistingPlayers();
		reload();
		assertTrue("never touched, was on by default", SPDSettings.stairNavigationShortcut());
		assertTrue(SPDSettings.adaptiveSearchChoice());
		assertTrue(SPDSettings.confirmWaterskin());
		assertTrue(SPDSettings.confirmKnownTraps());
		assertTrue("saved on stays on", SPDSettings.dotOverlayEnabled());
		assertFalse("saved off stays off", SPDSettings.autoCollectGrassDew());
		assertFalse("new settings still start off", SPDSettings.trophyOutlines());
	}

	@Test public void freshInstallStartsWithEverythingOff(){
		SPDSettings.keepOldDefaultsForExistingPlayers();
		reload();
		assertFalse(SPDSettings.stairNavigationShortcut());
		assertFalse(SPDSettings.adaptiveSearchChoice());
		assertFalse(SPDSettings.dotOverlayEnabled());
		assertFalse(SPDSettings.autoCollectGrassDew());
		assertFalse(SPDSettings.confirmWaterskin());
		assertFalse(SPDSettings.confirmKnownTraps());
	}

	@Test public void runsOnceSoALaterChoiceIsNeverOverwritten(){
		SPDSettings.version(921);
		assertEquals(6, SPDSettings.keepOldDefaultsForExistingPlayers());
		SPDSettings.stairNavigationShortcut(false);
		assertEquals(0, SPDSettings.keepOldDefaultsForExistingPlayers());
		reload();
		assertFalse(SPDSettings.stairNavigationShortcut());
	}

	@Test public void theReportShowsSavedAndUnsetValues(){
		SPDSettings.stairNavigationShortcut(true);
		String r = SPDSettings.apdSettingsReport();
		assertTrue(r, r.contains("apd_stair_navigation_shortcut=true"));
		assertTrue(r, r.contains("apd_trophy_outlines=unset"));
	}
}
