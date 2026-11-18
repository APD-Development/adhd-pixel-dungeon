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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ColumnOrderTest {

	private static final List<String> C = Arrays.asList("a", "b", "c", "d");

	@Test
	public void movesByOnePlaceAndStopsAtTheEnds(){
		assertEquals(Arrays.asList("a", "c", "b", "d"), ColumnOrder.move(C, "b", 1));
		assertEquals(Arrays.asList("b", "a", "c", "d"), ColumnOrder.move(C, "b", -1));
		assertEquals(C, ColumnOrder.move(C, "a", -1));
		assertEquals(C, ColumnOrder.move(C, "d", 1));
		assertEquals("an unknown column changes nothing", C, ColumnOrder.move(C, "zz", 1));
	}

	@Test
	public void frontAndBackKeepEveryColumn(){
		assertEquals(Arrays.asList("c", "a", "b", "d"), ColumnOrder.toFront(C, "c"));
		assertEquals(Arrays.asList("a", "b", "d", "c"), ColumnOrder.toBack(C, "c"));
		assertEquals("the input is never changed", Arrays.asList("a", "b", "c", "d"), C);
	}
}
