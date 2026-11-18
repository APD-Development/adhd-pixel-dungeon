/* ADHD Pixel Dungeon (C) 2026 Chris Charlton, GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ArmoryRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.CrystalVaultRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.StorageRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.EmptyRoom;
import com.watabou.utils.Bundle;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Visual-only gating: saved searches and actual secret-door discovery never change. */
public class GatedRoomWallMarkerTest {
    private static final int W = 11;
    private static int at(int x, int y) { return y * W + x; }

    private static class Arena extends SewerLevel {
        void roomForTest(Room room) { rooms = new ArrayList<>(Arrays.asList(room)); }
    }

    private Level priorLevel;
    private int priorDepth, priorBranch;
    private Arena level;
    private final int wall = at(4, 3), entrance = at(6, 3);

    @Before public void setup() {
        HeadlessTestSupport.start();
        priorLevel = Dungeon.level;
        priorDepth = Dungeon.depth;
        priorBranch = Dungeon.branch;
        Dungeon.depth = 7;
        Dungeon.branch = 0;
        level = new Arena();
        level.setSize(W, W);
        Dungeon.level = level;
        Arrays.fill(level.map, Terrain.WALL);
        Arrays.fill(level.heroFOV, true);
        Arrays.fill(level.visited, false);
        Arrays.fill(level.mapped, false);
        level.map[at(4, 2)] = level.map[at(4, 4)] = Terrain.EMPTY;
        level.map[entrance] = Terrain.LOCKED_DOOR;
        assertTrue(level.searchedWalls.mark(level, wall));
        assertTrue(DoorSearchEligibility.candidate(level, wall));
    }

    @After public void restore() {
        Dungeon.level = priorLevel;
        Dungeon.depth = priorDepth;
        Dungeon.branch = priorBranch;
    }

    private void setRoom(Room room, Room.Door.Type doorType) {
        room.set(3, 3, 8, 8);
        Room.Door door = new Room.Door(6, 3);
        door.set(doorType);
        room.connected.put(new EmptyRoom(), door);
        level.roomForTest(room);
    }

    @Test public void observedIronKeyCrystalAndBarricadedRoomsHidePerimeterChecks() {
        Object[][] rooms = {
                {new ArmoryRoom(), Room.Door.Type.LOCKED},
                {new CrystalVaultRoom(), Room.Door.Type.LOCKED},
                {new StorageRoom(), Room.Door.Type.BARRICADE},
                {new StorageRoom(), Room.Door.Type.CRYSTAL}
        };
        for (Object[] fixture : rooms) {
            setRoom((SpecialRoom) fixture[0], (Room.Door.Type) fixture[1]);
            Bundle before = new Bundle();
            level.searchedWalls.store(before);
            assertTrue(level.searchedWalls.attempted(wall));
            assertEquals(-1, SearchedWallMarkers.visual(level, wall));
            assertEquals("raised checkmark must also be hidden", -1,
                    SearchedWallMarkers.visual(level, at(4, 2)));
            assertTrue("real search eligibility stays unchanged",
                    DoorSearchEligibility.candidate(level, wall));
            Bundle after = new Bundle();
            level.searchedWalls.store(after);
            assertEquals(before.toString(), after.toString());
        }
    }

    @Test public void unseenEntranceCannotRevealHiddenGatedRoomThroughCheckmark() {
        setRoom(new ArmoryRoom(), Room.Door.Type.LOCKED);
        level.heroFOV[entrance] = false;
        assertEquals(SearchedWallMarkers.FACE, SearchedWallMarkers.visual(level, wall));
        assertEquals(SearchedWallMarkers.OVERHANG, SearchedWallMarkers.visual(level, at(4, 2)));
        level.visited[entrance] = true;
        assertEquals(-1, SearchedWallMarkers.visual(level, wall));
        level.visited[entrance] = false;
        level.mapped[entrance] = true;
        assertEquals(-1, SearchedWallMarkers.visual(level, wall));
    }

    @Test public void ordinaryAndOpenSpecialRoomsKeepTheirExistingCheckmark() {
        setRoom(new EmptyRoom(), Room.Door.Type.LOCKED);
        assertEquals(SearchedWallMarkers.FACE, SearchedWallMarkers.visual(level, wall));
        setRoom(new ArmoryRoom(), Room.Door.Type.REGULAR);
        assertEquals(SearchedWallMarkers.FACE, SearchedWallMarkers.visual(level, wall));
        setRoom(new ArmoryRoom(), Room.Door.Type.UNLOCKED);
        assertEquals(SearchedWallMarkers.FACE, SearchedWallMarkers.visual(level, wall));
    }

    @Test public void lockedRoomCheckNeverRevealsTheActualUnseenBackingTile() {
        setRoom(new ArmoryRoom(), Room.Door.Type.LOCKED);
        level.heroFOV[at(4, 4)] = false;
        for (int tile = 0; tile < Terrain.flags.length; tile++) {
            level.map[at(4, 4)] = tile;
            assertEquals("hidden backing terrain " + tile, -1,
                    SearchedWallMarkers.visual(level, wall));
        }
        assertTrue(level.searchedWalls.attempted(wall));
    }
}
