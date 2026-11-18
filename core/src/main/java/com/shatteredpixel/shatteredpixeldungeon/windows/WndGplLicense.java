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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.GplLicense;
import java.util.ArrayList;
import java.util.List;

/** Uses native scrolling windows; no browser or network is needed for license text. */
public final class WndGplLicense {
    private WndGplLicense() {}
    public static void show() {
        final List<String> pages = GplLicense.pages(Gdx.files.internal(GplLicense.ASSET).readString("UTF-8"), 2000);
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) labels.add("GPLv3 — part " + (i + 1) + " / " + pages.size());
        ShatteredPixelDungeon.scene().addToFront(new WndUimsdList("GNU GPLv3", "Complete license, available offline.",
                labels, null, (index, checked) -> ShatteredPixelDungeon.scene().addToFront(
                        new WndUimsdText("GPLv3 — " + (index + 1) + " / " + pages.size(), pages.get(index), false))).keepOpen());
    }
}
