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

/** Stable saved palette indices. Keep existing entries in order when adding colors. */
public final class SearchedWallAppearance {
    public enum Color {
        GREEN("Green",0x93DFAB),WHITE("White",0xFFFFFF),YELLOW("Yellow",0xFFE34D),
        CYAN("Cyan",0x50E5FF),MAGENTA("Magenta",0xFF66CC),ORANGE("Orange",0xFF9C3A),
        RED("Red",0xFF5A5A),BLUE("Blue",0x789DFF);
        public final String label;public final int rgb;
        Color(String label,int rgb){this.label=label;this.rgb=rgb;}
    }
    private static final Color[] COLORS=Color.values();
    private SearchedWallAppearance(){}
    public static int count(){return COLORS.length;}
    public static int normalize(int index){return index>=0&&index<COLORS.length?index:0;}
    public static Color color(int index){return COLORS[normalize(index)];}
}
