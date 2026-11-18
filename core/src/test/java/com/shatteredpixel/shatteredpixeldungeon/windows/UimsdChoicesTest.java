package com.shatteredpixel.shatteredpixeldungeon.windows;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class UimsdChoicesTest {
	@Test public void explanationIsAScrollingNoteAndChoicesKeepTheirOrderAndWording(){
		List<WndUimsdList.Entry> entries = WndUimsdChoices.contentEntries("An explanation", "Queue the request", "Cancel");
		assertEquals(3, entries.size());
		assertEquals(WndUimsdList.Entry.NOTE, entries.get(0).kind);
		assertFalse(entries.get(0).isItem());
		assertEquals("An explanation", entries.get(0).label);
		assertEquals("Queue the request", entries.get(1).label);
		assertEquals("Cancel", entries.get(2).label);
		assertTrue(entries.get(1).isItem());
		assertTrue(entries.get(2).isItem());
	}
	@Test public void noExplanationDoesNotInsertAnEmptyRow(){
		assertEquals(1, WndUimsdChoices.contentEntries(null, "Close").size());
		assertEquals(WndUimsdList.Entry.ITEM, WndUimsdChoices.contentEntries("", "Close").get(0).kind);
	}
	@Test public void uimsdConfirmationsUseWrappedActionsInsteadOfFixedHeightOptions() throws Exception {
		java.nio.file.Path root = java.nio.file.Path.of(System.getProperty("apd.game.src"));
		for (String file : new String[]{"windows/WndUimsdOptions.java", "windows/WndUimsdReport.java"}){
			String source = java.nio.file.Files.readString(root.resolve(file));
			assertFalse(file, source.contains("new WndOptions("));
			assertTrue(file, source.contains("new WndUimsdChoices("));
		}
		String list = java.nio.file.Files.readString(root.resolve("windows/WndUimsdList.java"));
		assertTrue(list.contains("e.action ? WindowGeometry.actionHeight(h)"));
		assertTrue(list.contains("PixelScene.renderTextBlock(itemText(e), UimsdStyle.SIZE_TEXT)"));
		assertTrue(list.indexOf("label.maxWidth(textWidth)") < list.indexOf("e.action ? WindowGeometry.actionHeight(h)"));
		String demo = java.nio.file.Files.readString(root.resolve("windows/WndDemoUnlock.java"));
		assertTrue(demo.contains("extends WndUimsdChoices"));
		assertTrue(demo.contains("super(UimsdGate.WARNING_TITLE, UimsdGate.WARNING_TEXT, UimsdGate.CHOICE_CONFUSION, UimsdGate.CHOICE_PLAY)"));
		assertFalse(demo.contains("WIDTH_P"));
	}
}
