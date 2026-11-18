package com.shatteredpixel.shatteredpixeldungeon.apd.diagnostics;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

public class BugNotesTest {
	File dir;

	@Before public void setUp() throws Exception {
		dir = Files.createTempDirectory("bugnotes").toFile();
		BugNotes.dirOverride = dir;
	}

	@After public void tearDown(){
		BugNotes.clear();
		BugNotes.dirOverride = null;
	}

	@Test public void savesNotesWithContextAndKeepsThem(){
		assertEquals("", BugNotes.read());
		assertTrue(BugNotes.add("dart went through a wall"));
		assertTrue(BugNotes.add("  second\nline  "));
		String t = BugNotes.read();
		assertTrue(t.contains("UTC"));
		assertTrue(t.contains("  dart went through a wall\n"));
		assertTrue(t.contains("  second line\n"));
		assertEquals(2, BugNotes.count());
	}

	@Test public void blankNotesAreIgnored(){
		assertFalse(BugNotes.add("   "));
		assertFalse(BugNotes.add(null));
		assertEquals(0, BugNotes.count());
	}

	@Test public void stopsAtSizeCap() throws Exception {
		Files.write(new File(dir, "bug-notes.txt").toPath(), new byte[(int) BugNotes.MAX_BYTES + 1]);
		assertFalse(BugNotes.add("too much"));
	}

	@Test public void standardReportIsUnchangedWithoutNotesAndGainsASectionWithThem(){
		assertEquals("REPORT", DiagnosticReport.withBugNotes("REPORT", ""));
		assertEquals("REPORT", DiagnosticReport.withBugNotes("REPORT", null));
		assertEquals("REPORT\n\n=== Bug notes ===\nnote\n", DiagnosticReport.withBugNotes("REPORT", "note\n"));
	}

	@Test public void buttonOnlyInTheDeveloperBuild(){
		try {
			com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.parse("");
			assertFalse("Alpha Testers never see Bug Note", BugNotes.buttonVisible());
			com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.parse("testBuild\n");
			assertTrue(BugNotes.buttonVisible());
		} finally {
			com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.resetForTest();
		}
	}
}
