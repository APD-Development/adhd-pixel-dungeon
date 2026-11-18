/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.apd;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import static org.junit.Assert.*;

public class GplLicenseTest {
    @Test public void offlineLicenseIsByteIdenticalAndEveryCharacterIsReachable() throws Exception {
        byte[] root = Files.readAllBytes(Paths.get("../../../../LICENSE.txt"));
        byte[] bundled = Files.readAllBytes(Paths.get(GplLicense.ASSET));
        assertArrayEquals(root, bundled);
        String text = new String(bundled, StandardCharsets.UTF_8);
        List<String> pages = GplLicense.pages(text, 2000);
        assertEquals(text, String.join("", pages));
        assertTrue(pages.size() > 1);
        for (String page : pages) assertTrue(page.length() > 0 && page.length() <= 2000);
        assertTrue(text.contains("GNU GENERAL PUBLIC LICENSE"));
        assertTrue(text.contains("17. Interpretation of Sections 15 and 16"));
    }
    @Test public void pageBoundariesPreserveWhitespaceAndLongLines() {
        String text = "  a\r\n" + "0123456789" + "\n\nlast\n";
        assertEquals(text, String.join("", GplLicense.pages(text, 4)));
        assertTrue(GplLicense.pages("", 2000).isEmpty());
    }
    @Test public void noticesDistinguishTheForkAndGiveItsSource() {
        assertTrue(GplLicense.NOTICE.contains("Chris Charlton"));
        assertTrue(GplLicense.NOTICE.contains("Evan Debenham"));
        assertTrue(GplLicense.NOTICE.contains("Oleg Dolya"));
        assertTrue(GplLicense.NOTICE.contains("not affiliated with or endorsed"));
        assertTrue(GplLicense.NOTICE.contains("without warranty"));
        assertTrue(GplLicense.NOTICE.contains(GplLicense.SOURCE_URL));
    }
}
