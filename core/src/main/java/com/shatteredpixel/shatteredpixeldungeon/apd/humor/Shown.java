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

package com.shatteredpixel.shatteredpixeldungeon.apd.humor;

// A line chosen to be shown. Choosing does not use it up: it counts as heard (and a story moves on) only when the game confirms that the
// player was really shown it (ClericGrudge.confirm).
public final class Shown {

	public final String id;
	public final Category category;
	public final Moment moment;
	public final String text;

	Shown( String id, Category category, Moment moment, String text ){
		this.id = id;
		this.category = category;
		this.moment = moment;
		this.text = text;
	}
}
