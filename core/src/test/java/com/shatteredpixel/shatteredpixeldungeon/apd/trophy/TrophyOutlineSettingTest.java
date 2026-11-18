package com.shatteredpixel.shatteredpixeldungeon.apd.trophy;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Dungeon trophy outlines are a player setting, off by default; the Bestiary's outlines don't depend on it. */
public class TrophyOutlineSettingTest {
	private static String src(String path) throws Exception {
		return new String(Files.readAllBytes(new File(System.getProperty("apd.game.src"), path).toPath()), StandardCharsets.UTF_8);
	}

	@Test public void offByDefault_shownInApdSettings_andOnlyTheDungeonOutlineChecksIt() throws Exception {
		assertTrue(src("SPDSettings.java").contains("getBoolean(\"apd_trophy_outlines\",false)"));
		assertTrue(src("apd/trophy/Silhouette.java").contains("SPDSettings.trophyOutlines()"));
		String pane = src("windows/APDSettingsPane.java");
		assertTrue(pane.contains("new SettingCheckBox(\"Trophy outlines\")"));
		assertTrue(pane.contains("SPDSettings.trophyOutlines(checked())"));
		assertFalse("the Bestiary keeps its outlines", src("windows/WndJournal.java").contains("trophyOutlines()"));
	}
}
