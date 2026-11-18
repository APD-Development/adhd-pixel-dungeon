package com.shatteredpixel.shatteredpixeldungeon.levels;

import static org.junit.Assert.*;

import org.junit.Test;

/** The terrain rules other systems rely on: walkable floors, blocking walls, hidden doors that look like walls. */
public class TerrainFlagsTest {
	private static boolean has(int terrain, int flag){ return (Terrain.flags[terrain] & flag) != 0; }

	@Test public void floorsWaterStairsAndOpenDoorsAreWalkable(){
		for (int t : new int[]{Terrain.EMPTY, Terrain.WATER, Terrain.ENTRANCE, Terrain.EXIT, Terrain.OPEN_DOOR, Terrain.DOOR, Terrain.EMPTY_DECO, Terrain.INACTIVE_TRAP})
			assertTrue("terrain " + t, has(t, Terrain.PASSABLE));
	}

	@Test public void wallsBlockMovementAndSight(){
		for (int t : new int[]{Terrain.WALL, Terrain.WALL_DECO, Terrain.SECRET_DOOR}){
			assertTrue("terrain " + t, has(t, Terrain.SOLID));
			assertTrue("terrain " + t, has(t, Terrain.LOS_BLOCKING));
			assertFalse("terrain " + t, has(t, Terrain.PASSABLE));
		}
	}

	@Test public void secretsLookLikeWhatTheyHideAs(){
		assertEquals(Terrain.flags[Terrain.WALL] | Terrain.SECRET, Terrain.flags[Terrain.SECRET_DOOR]);
		assertEquals(Terrain.flags[Terrain.EMPTY] | Terrain.SECRET, Terrain.flags[Terrain.SECRET_TRAP]);
	}

	@Test public void chasmsAreAvoidedPits(){
		assertTrue(has(Terrain.CHASM, Terrain.AVOID));
		assertTrue(has(Terrain.CHASM, Terrain.PIT));
		assertFalse(has(Terrain.CHASM, Terrain.PASSABLE));
	}
}
