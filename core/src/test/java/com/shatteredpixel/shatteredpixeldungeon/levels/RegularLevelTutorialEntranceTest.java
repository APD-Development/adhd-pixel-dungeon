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

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.entrance.EntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.watabou.utils.SparseArray;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RegularLevelTutorialEntranceTest {

	@Test
	public void legacyTutorialRepairOnlyExposesEntranceBoundaryDoors(){
		TestLevel level = new TestLevel();
		int entranceDoor = level.cell(4, 2);
		int unrelatedSecretDoor = level.cell(7, 4);
		int entranceWall = level.cell(2, 4);

		Level.set(entranceDoor, Terrain.SECRET_DOOR, level);
		Level.set(unrelatedSecretDoor, Terrain.SECRET_DOOR, level);

		level.repairTutorialEntrance();

		assertEquals(Terrain.DOOR, level.map[entranceDoor]);
		assertTrue(level.passable[entranceDoor]);
		assertFalse(level.secret[entranceDoor]);
		assertEquals(Terrain.SECRET_DOOR, level.map[unrelatedSecretDoor]);
		assertEquals(Terrain.WALL, level.map[entranceWall]);
	}

	private static final class TestLevel extends RegularLevel {

		private TestLevel(){
			setSize(10, 10);
			traps = new SparseArray<Trap>();
			blobs = new HashMap<Class<? extends Blob>, Blob>();
			roomEntrance = new EntranceRoom();
			roomEntrance.set(2, 2, 6, 6);
		}

		private int cell(int x, int y){
			return x + y * width();
		}

		@Override
		protected boolean build(){
			return true;
		}

		@Override
		protected Painter painter(){
			return null;
		}

		@Override
		protected void createMobs(){
		}

		@Override
		protected void createItems(){
		}
	}
}
