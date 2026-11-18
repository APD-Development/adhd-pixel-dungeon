/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.apd.query;

// Boss enemy classes, for the Bosses table. Mirrors the mobs with Char.Property.BOSS. Only the yes/no is shown; the class itself stays hidden.
// TODO: replace with a recorded flag if a boss is ever added that isn't in this list.
public final class BossClasses {

	private static final String[] SIMPLE_NAMES = { "Goo", "Tengu", "DM300", "DwarfKing", "YogDzewa", "YogFist",
			"BurningFist", "SoiledFist", "RottingFist", "RustedFist", "BrightFist", "DarkFist",
			"CrystalSpire", "GnollGeomancer", "FungalCore", "VaultBossElemental" };

	private BossClasses(){}

	//'className' may be a full class name or a simple one; a column that holds several classes is "(several)"
	public static boolean isBoss( String className ){
		if (className == null) return false;
		int dot = className.lastIndexOf('.');
		String simple = dot >= 0 ? className.substring(dot + 1) : className;
		int dollar = simple.lastIndexOf('$');
		if (dollar >= 0) simple = simple.substring(dollar + 1);
		for (String n : SIMPLE_NAMES) if (n.equals(simple)) return true;
		return false;
	}
}
