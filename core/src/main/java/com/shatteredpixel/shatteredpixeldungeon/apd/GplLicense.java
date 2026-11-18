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

import java.util.ArrayList;
import java.util.List;

/** Full license, bounded into readable pages without discarding any text. */
public final class GplLicense {
    private GplLicense() {}
    public static final String ASSET = "legal/GPL-3.0.txt";
    public static final String SOURCE_URL = "https://github.com/APD-Development/ShieldingShouldHaveWorked";
    public static final String NOTICE = "ADHD Pixel Dungeon is modified by Chris Charlton (2026). "
            + "It is based on Shattered Pixel Dungeon by Evan Debenham and Pixel Dungeon by Oleg Dolya (Watabou).\n\n"
            + "Licensed under GNU GPL version 3 or later, without warranty. "
            + "The complete license is bundled for offline reading.\n\n"
            + "APD is not affiliated with or endorsed by Evan Debenham or Shattered Pixel.\n\n"
            + "Matching source must be provided beside your build download. Development source: " + SOURCE_URL
            + "\n\nThe development repository is private. If you cannot access the matching source, request it from Chris. "
            + "GPL rights are not restricted to the alpha-tester group.";

    public static List<String> pages(String text, int limit) {
        if (limit < 1) throw new IllegalArgumentException("Positive page limit required");
        List<String> pages = new ArrayList<>();
        for (int start = 0; start < text.length();) {
            int end = Math.min(text.length(), start + limit);
            if (end < text.length()) {
                int newline = text.lastIndexOf('\n', end - 1);
                if (newline >= start) end = newline + 1;
                else if (Character.isHighSurrogate(text.charAt(end - 1))
                        && Character.isLowSurrogate(text.charAt(end)) && end - start > 1) end--;
            }
            pages.add(text.substring(start, end));
            start = end;
        }
        return pages;
    }
}
