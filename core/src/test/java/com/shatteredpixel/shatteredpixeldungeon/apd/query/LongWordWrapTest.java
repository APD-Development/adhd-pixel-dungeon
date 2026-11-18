package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import org.junit.Test;
import static org.junit.Assert.*;

public class LongWordWrapTest {
    @Test public void ordinaryTextAndWhitespaceStayUnchanged() {
        String text = "one  two\nthree\tend";
        assertEquals(text, LongWordWrap.wrap(text, 8, String::length));
    }
    @Test public void unbrokenPreviewIdsWrapWithoutLosingCharacters() {
        String text = "run_id: " + "W".repeat(100);
        String shown = LongWordWrap.wrap(text, 8, String::length);
        assertEquals(text, shown.replace("\n", ""));
        assertTrue(shown.contains("\n"));
        for (String word : shown.split("\\s+")) assertTrue(word.length() <= 8);
    }
    @Test public void measuredWidthsRatherThanCharacterCountsDetermineBreaks() {
        String text = "WWiiiiWWiiii";
        String shown = LongWordWrap.wrap(text, 6, s -> s.chars().map(c -> c == 'W' ? 4 : 1).sum());
        assertEquals(text, shown.replace("\n", ""));
        for (String line : shown.split("\n")) assertTrue(line.chars().map(c -> c == 'W' ? 4 : 1).sum() <= 6);
    }
    @Test public void unicodeCodePointsAreNeverSplitInHalf() {
        String text = "😀abc😀abc😀";
        String shown = LongWordWrap.wrap(text, 3, s -> s.codePointCount(0, s.length()));
        assertEquals(text, shown.replace("\n", ""));
        for (String line : shown.split("\n")) {
            assertFalse(Character.isLowSurrogate(line.charAt(0)));
            assertFalse(Character.isHighSurrogate(line.charAt(line.length()-1)));
            assertTrue(line.codePointCount(0, line.length()) <= 3);
        }
    }
    @Test public void rawUnderscoresAndStarsAreNeverRemoved() {
        String text = "_run_id_**literal**";
        assertEquals(text, LongWordWrap.wrap(text, 4, String::length).replace("\n", ""));
    }
    @Test public void oneGlyphWiderThanTheViewportDoesNotCauseAnInfiniteLoop() {
        assertEquals("W\nW", LongWordWrap.wrap("WW", 1, s -> s.length() * 4));
    }
    @Test(timeout=2000) public void largeUnbrokenValuesHaveBoundedWork() {
        String text = "x".repeat(100000);
        String shown = LongWordWrap.wrap(text, 8, String::length);
        assertEquals(text, shown.replace("\n", ""));
    }
}
