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
package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

/** Stable role positions; columns count inward from the chosen screen edge. */
public final class CombatTagLayout {
    private CombatTagLayout() { }
    public static int stairRow(boolean duelist) { return duelist ? 2 : 1; }
    public static int spellColumn(int slot) { return 1 + slot % 2; }
    public static int spellRow(int slot) { return slot / 2; }
    public static int[] fit(int column, int row, int availableRows) {
        if (availableRows >= 3) return new int[]{column,row};
        int rows = Math.max(1, availableRows), index = column*3+row;
        return new int[]{index/rows,index%rows};
    }
}
