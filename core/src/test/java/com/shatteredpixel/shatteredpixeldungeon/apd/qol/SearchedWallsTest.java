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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import static org.junit.Assert.*;

public class SearchedWallsTest {
    private Level oldLevel;private int oldDepth,oldBranch;
    private Hero oldHero;
    private GrassDewPickupTest.TestLevel level;
    public static class NoopSprite extends HeroSprite {
        @Override public void operate(int pos){}
        @Override public void showStatus(int color,String text,Object...args){}
    }
    private static class SearchHero extends Hero {@Override public void next(){}}
    @Before public void setup(){HeadlessTestSupport.start();oldLevel=Dungeon.level;oldHero=Dungeon.hero;oldDepth=Dungeon.depth;oldBranch=Dungeon.branch;Dungeon.depth=2;Dungeon.branch=0;level=new GrassDewPickupTest.TestLevel();level.setSize(7,7);Dungeon.level=level;Arrays.fill(level.map,Terrain.WALL);Arrays.fill(level.heroFOV,true);}
    @After public void cleanup(){Dungeon.level=oldLevel;Dungeon.hero=oldHero;Dungeon.depth=oldDepth;Dungeon.branch=oldBranch;}
    @Test public void onlyVisibleWallTilesCanBeMarkedAndItDoesNotRevealSecrets(){for(int t:new int[]{Terrain.WALL,Terrain.SECRET_DOOR}){level.map[16]=t;assertTrue(level.searchedWalls.mark(level,16));assertEquals(t,level.map[16]);assertTrue(level.searchedWalls.shows(level,16));level.searchedWalls.clear();}level.heroFOV[16]=false;assertFalse(level.searchedWalls.mark(level,16));level.heroFOV[16]=true;level.map[16]=Terrain.EMPTY;assertFalse(level.searchedWalls.mark(level,16));assertFalse(level.searchedWalls.mark(level,-1));assertFalse(level.searchedWalls.mark(level,49));}
    @Test public void sparseMemoryRoundTripsWithoutFabricatingLegacySearches(){level.searchedWalls.mark(level,16);Bundle b=new Bundle();level.searchedWalls.store(b);SearchedWalls restored=new SearchedWalls();restored.restore(b,level.length());assertTrue(restored.shows(level,16));assertFalse(restored.shows(level,17));restored.restore(new Bundle(),level.length());assertFalse(restored.shows(level,16));}
    @Test public void unsearchedFloorAddsNoSaveFields(){Bundle b=new Bundle();String before=b.toString();level.searchedWalls.store(b);assertEquals(before,b.toString());}
    @Test public void marksFollowFogMemoryAndDisappearWhenWallIsOpened(){level.searchedWalls.mark(level,16);level.heroFOV[16]=false;assertFalse(level.searchedWalls.shows(level,16));level.visited[16]=true;assertTrue(level.searchedWalls.shows(level,16));level.map[16]=Terrain.DOOR;assertFalse(level.searchedWalls.shows(level,16));}
    @Test public void floorResetAndSeparateFloorsHaveIndependentMemory(){level.searchedWalls.mark(level,16);Level other=new GrassDewPickupTest.TestLevel();other.setSize(7,7);assertFalse(other.searchedWalls.shows(other,16));level.setSize(7,7);assertFalse(level.searchedWalls.shows(level,16));}
    @Test public void damagedOutOfRangeIndicesDoNotBreakLoading(){Bundle b=new Bundle();b.put("apd_searched_walls_v1",new int[]{-1,16,16,Integer.MAX_VALUE});level.searchedWalls.restore(b,49);assertTrue(level.searchedWalls.shows(level,16));assertFalse(level.searchedWalls.shows(level,17));}
    /** Simulates Bundle.getIntArray returning null after it encounters corrupt saved data. */
    private static class UnreadableArrayBundle extends Bundle {
        private final String unreadableKey;
        UnreadableArrayBundle(String unreadableKey) { this.unreadableKey = unreadableKey; }
        @Override public int[] getIntArray(String key) {
            return unreadableKey.equals(key) ? null : super.getIntArray(key);
        }
    }
    @Test public void unreadableDoorChecksDoNotDiscardValidSearchMarks() {
        Bundle damaged = new UnreadableArrayBundle("apd_door_checks_v1");
        damaged.put("apd_door_checks_v1", new int[]{16});
        damaged.put("apd_searched_walls_v1", new int[]{16, -1, 49});
        level.searchedWalls.restore(damaged, level.length());
        assertTrue(level.searchedWalls.attempted(16));
        Bundle saved = new Bundle();
        level.searchedWalls.store(saved);
        assertFalse("Malformed checkmarks must not be fabricated", saved.contains("apd_door_checks_v1"));
        assertArrayEquals(new int[]{16}, saved.getIntArray("apd_searched_walls_v1"));
    }
    @Test public void unreadableSearchMarksDoNotDiscardValidDoorChecks() {
        Bundle damaged = new UnreadableArrayBundle("apd_searched_walls_v1");
        damaged.put("apd_door_checks_v1", new int[]{16, -1, 49});
        damaged.put("apd_searched_walls_v1", new int[]{16});
        level.searchedWalls.restore(damaged, level.length());
        assertFalse(level.searchedWalls.attempted(16));
        Bundle saved = new Bundle();
        level.searchedWalls.store(saved);
        assertArrayEquals(new int[]{16}, saved.getIntArray("apd_door_checks_v1"));
        assertFalse("Malformed marks must not be fabricated", saved.contains("apd_searched_walls_v1"));
    }
    @Test public void unreadableArraysClearOldMemoryWithoutChangingFloor() {
        level.searchedWalls.mark(level, 16);
        int[] before = level.map.clone();
        Bundle damaged = new UnreadableArrayBundle("apd_searched_walls_v1");
        damaged.put("apd_searched_walls_v1", new int[]{16});
        level.searchedWalls.restore(damaged, level.length());
        assertFalse(level.searchedWalls.attempted(16));
        assertArrayEquals(before, level.map);
        Bundle saved = new Bundle();
        level.searchedWalls.store(saved);
        assertFalse(saved.contains("apd_searched_walls_v1"));
        assertFalse(saved.contains("apd_door_checks_v1"));
    }
    @Test public void recordingDoesNotConsumeRandomness(){Random.pushGenerator(71);long expected=Random.Long();Random.popGenerator();Random.pushGenerator(71);try{level.searchedWalls.mark(level,16);assertEquals(expected,Random.Long());}finally{Random.popGenerator();}}
    @Test public void realManualSearchMarksOnlyItsVisibleRadiusAndKeepsNormalTurnCost()throws Exception{SearchHero h=hero();float before=h.cooldown();assertFalse(h.search(true));assertTrue(level.searchedWalls.shows(level,16));assertFalse(level.searchedWalls.shows(level,10));assertEquals(before+2f,h.cooldown(),0);}
    @Test public void realAutomaticSearchDoesNotAddAnyMarkersOrExtraTurn()throws Exception{SearchHero h=hero();float before=h.cooldown();assertFalse(h.search(false));Bundle b=new Bundle();level.searchedWalls.store(b);assertEquals("{}",b.toString());assertEquals(before,h.cooldown(),0);}
    @Test public void bottomWallProjectsOntoItsVisibleOverhang(){level.map[24]=Terrain.EMPTY;level.heroFOV[38]=false;level.searchedWalls.mark(level,31);assertEquals(SearchedWallMarkers.OVERHANG,SearchedWallMarkers.visual(level,24));assertEquals(-1,SearchedWallMarkers.visual(level,31));}
    @Test public void upperWallKeepsItsFrontFaceMarker(){level.map[24]=Terrain.EMPTY;level.heroFOV[10]=false;level.searchedWalls.mark(level,17);assertEquals(SearchedWallMarkers.FACE,SearchedWallMarkers.visual(level,17));}
    @Test public void sideWallsUseTheExposedEdge(){level.heroFOV[22]=false;level.heroFOV[26]=false;level.map[17]=Terrain.EMPTY;level.map[24]=Terrain.EMPTY;level.map[31]=Terrain.EMPTY;level.searchedWalls.mark(level,23);level.searchedWalls.mark(level,25);assertEquals(SearchedWallMarkers.RIGHT,SearchedWallMarkers.visual(level,23));assertEquals(SearchedWallMarkers.LEFT,SearchedWallMarkers.visual(level,25));}
    @Test public void projectionNeverInventsUnsearchedOrHiddenMarks(){level.map[24]=Terrain.EMPTY;level.heroFOV[38]=false;assertEquals(-1,SearchedWallMarkers.visual(level,24));level.searchedWalls.mark(level,31);level.heroFOV[24]=false;assertEquals(-1,SearchedWallMarkers.visual(level,24));level.heroFOV[24]=true;level.heroFOV[31]=false;assertEquals(-1,SearchedWallMarkers.visual(level,24));}
    @Test public void openingSearchedWallRemovesItsProjectedMark(){level.map[24]=Terrain.EMPTY;level.searchedWalls.mark(level,31);level.map[31]=Terrain.DOOR;assertEquals(-1,SearchedWallMarkers.visual(level,24));}
    @Test public void projectionDoesNotWrapBetweenRowsOrPastMap(){level.map[0]=Terrain.EMPTY;level.searchedWalls.mark(level,6);assertEquals(-1,SearchedWallMarkers.visual(level,6));assertEquals(-1,SearchedWallMarkers.visual(level,-1));assertEquals(-1,SearchedWallMarkers.visual(level,49));}
    @Test public void enclosedDiagonalCornersHaveNoCheckmark(){level.map[24]=Terrain.EMPTY;for(int cell:new int[]{16,18,30,32}){level.searchedWalls.mark(level,cell);assertEquals("inside corner "+cell,-1,SearchedWallMarkers.visual(level,cell));assertEquals("no projected check on its wall neighbour",-1,SearchedWallMarkers.visual(level,cell-7));}}
    @Test public void sideWallEndsBesideCornersHaveNoCheckmark(){level.heroFOV[22]=false;level.map[17]=Terrain.EMPTY;level.map[24]=Terrain.EMPTY;level.map[31]=Terrain.EMPTY;for(int cell:new int[]{16,18,30,32}){level.searchedWalls.mark(level,cell);assertEquals("partial side-wall end "+cell,-1,SearchedWallMarkers.visual(level,cell));}level.searchedWalls.mark(level,23);assertEquals("straight middle wall keeps its marker",SearchedWallMarkers.RIGHT,SearchedWallMarkers.visual(level,23));}
    @Test public void isolatedPillarsAndVisibleCornersCannotBeDoors(){
        Arrays.fill(level.map,Terrain.EMPTY);level.map[24]=Terrain.WALL;level.searchedWalls.mark(level,24);
        assertFalse(SearchedWallMarkers.couldBeDoor(level,24));assertEquals(-1,SearchedWallMarkers.visual(level,24));
        level.map[23]=Terrain.WALL;level.map[17]=Terrain.WALL;
        assertFalse("L-shaped corner",SearchedWallMarkers.couldBeDoor(level,24));
    }
    @Test public void straightRoomBoundaryKeepsARealDoorCandidate(){
        level.map[17]=Terrain.EMPTY;level.map[31]=Terrain.EMPTY;level.searchedWalls.mark(level,24);
        assertTrue(SearchedWallMarkers.couldBeDoor(level,24));assertEquals(SearchedWallMarkers.FACE,SearchedWallMarkers.visual(level,24));
    }
    @Test public void knownThickStoneAndMapEdgesCannotBeDoors(){
        level.map[31]=Terrain.EMPTY;level.searchedWalls.mark(level,24);
        assertFalse(SearchedWallMarkers.couldBeDoor(level,24));assertEquals(-1,SearchedWallMarkers.visual(level,24));
        for(int p:new int[]{0,3,6,7,13,42,45,48})assertFalse("edge "+p,SearchedWallMarkers.couldBeDoor(level,p));
        assertTrue("filtering preserves search memory",level.searchedWalls.shows(level,24));
    }
    @Test public void undiscoveredTerrainCannotRevealWhetherARoomExists(){
        level.map[31]=Terrain.EMPTY;level.heroFOV[17]=false;
        assertTrue(SearchedWallMarkers.couldBeDoor(level,24));
        for(int tile:new int[]{Terrain.WALL,Terrain.EMPTY,Terrain.WATER,Terrain.SECRET_DOOR}){
            level.map[17]=tile;assertTrue("unseen tile must not affect eligibility",SearchedWallMarkers.couldBeDoor(level,24));
        }
        level.map[17]=Terrain.WALL;level.visited[17]=true;assertFalse("known stone now rules it out",SearchedWallMarkers.couldBeDoor(level,24));
    }
    @Test public void secretDoorAndMatchingStoneHaveIdenticalVisualEligibility(){
        level.map[17]=Terrain.EMPTY;level.map[31]=Terrain.EMPTY;
        for(int tile:new int[]{Terrain.WALL,Terrain.SECRET_DOOR}){
            level.map[24]=tile;assertTrue(SearchedWallMarkers.couldBeDoor(level,24));
        }
    }
    @Test public void validFaceAndLipSurviveLeavingSightAndReloadingSearchMemory(){
        level.map[17]=Terrain.EMPTY;level.map[31]=Terrain.EMPTY;
        assertTrue(level.searchedWalls.mark(level,24));
        assertEquals(SearchedWallMarkers.FACE,SearchedWallMarkers.visual(level,24));
        assertEquals(SearchedWallMarkers.OVERHANG,SearchedWallMarkers.visual(level,17));
        int[] before=new int[level.length()];for(int p=0;p<before.length;p++)before[p]=SearchedWallMarkers.visual(level,p);
        Bundle saved=new Bundle();level.searchedWalls.store(saved);
        System.arraycopy(level.heroFOV,0,level.visited,0,level.length());Arrays.fill(level.heroFOV,false);
        for(int p=0;p<before.length;p++)assertEquals("leaving sight cell "+p,before[p],SearchedWallMarkers.visual(level,p));
        level.searchedWalls.clear();level.searchedWalls.restore(saved,level.length());
        for(int p=0;p<before.length;p++)assertEquals("reload/revisit cell "+p,before[p],SearchedWallMarkers.visual(level,p));
        Arrays.fill(level.heroFOV,true);
        for(int p=0;p<before.length;p++)assertEquals("return to sight cell "+p,before[p],SearchedWallMarkers.visual(level,p));
        Bundle after=new Bundle();level.searchedWalls.store(after);assertEquals(saved.toString(),after.toString());
    }
    @Test public void existingOpenDoorCannotFrameAnotherDoorwayAtAWallEnd(){
        level.map[17]=Terrain.EMPTY;level.map[31]=Terrain.EMPTY;level.map[23]=Terrain.OPEN_DOOR;
        assertFalse(SearchedWallMarkers.couldBeDoor(level,24));
    }
    @Test public void appearanceEligibilityDoesNotConsumeSearchOrCombatRandomness(){
        Random.pushGenerator(8673);long expected=Random.Long();Random.popGenerator();Random.pushGenerator(8673);
        try{SearchedWallMarkers.couldBeDoor(level,24);SearchedWallMarkers.visual(level,24);assertEquals(expected,Random.Long());}finally{Random.popGenerator();}
    }

    @Test public void missingOrTruncatedObservationCannotCreateWallMarks() {
        int[] originalMap = level.map;
        boolean[] originalFov = level.heroFOV;
        level.heroFOV = null;
        assertFalse(level.searchedWalls.mark(level, 16));
        level.heroFOV = new boolean[16];
        assertFalse(level.searchedWalls.mark(level, 16));
        level.heroFOV = originalFov;
        level.map = new int[16];
        assertFalse(level.searchedWalls.mark(level, 16));
        level.map = originalMap;
        assertTrue(level.searchedWalls.mark(level, 16));
    }

    @Test public void existingWallMarksRemainReadableFromAvailableFogMemory() {
        assertTrue(level.searchedWalls.mark(level, 16));
        boolean[] originalFov = level.heroFOV;
        boolean[] originalVisited = level.visited;
        boolean[] originalMapped = level.mapped;
        level.heroFOV = null;
        level.visited[16] = true;
        assertTrue(level.searchedWalls.shows(level, 16));
        level.visited = null;
        level.mapped[16] = true;
        assertTrue(level.searchedWalls.shows(level, 16));
        level.mapped = null;
        assertFalse(level.searchedWalls.shows(level, 16));
        level.heroFOV = originalFov;
        level.visited = originalVisited;
        level.mapped = originalMapped;
        assertTrue(level.searchedWalls.shows(level, 16));
    }

    @Test public void incompleteDoorCheckInputsClearUnprovenNegativeResult() {
        level.map[17] = Terrain.EMPTY;
        level.map[31] = Terrain.EMPTY;
        assertTrue(DoorSearchEligibility.candidate(level, 24));
        level.searchedWalls.recordCompletedDoorCheck(level, 24, true);
        assertTrue(level.searchedWalls.confirmed(level, 24));
        boolean[] originalSecret = level.secret;
        level.secret = null;
        level.searchedWalls.recordCompletedDoorCheck(level, 24, true);
        assertFalse(level.searchedWalls.confirmed(level, 24));
        level.secret = originalSecret;
        level.searchedWalls.recordCompletedDoorCheck(level, 24, true);
        assertTrue(level.searchedWalls.confirmed(level, 24));
    }

    @Test public void incompleteNeighbourObservationsCannotCertifyDoorGeometry() {
        level.map[17] = Terrain.EMPTY;
        level.map[31] = Terrain.EMPTY;
        level.searchedWalls.recordCompletedDoorCheck(level, 24, true);
        assertTrue(level.searchedWalls.confirmed(level, 24));
        boolean[] originalVisited = level.visited;
        level.visited = new boolean[24];
        assertFalse(level.searchedWalls.confirmed(level, 24));
        level.visited = null;
        assertFalse(level.searchedWalls.confirmed(level, 24));
        level.visited = originalVisited;
        int[] originalMap = level.map;
        level.map = new int[24];
        assertFalse(level.searchedWalls.confirmed(level, 24));
        level.map = originalMap;
        assertTrue(level.searchedWalls.confirmed(level, 24));
    }
    private SearchHero hero()throws Exception{Field u=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");u.setAccessible(true);Object unsafe=u.get(null);SearchHero h=new SearchHero();h.heroClass=HeroClass.WARRIOR;h.pos=24;h.fieldOfView=level.heroFOV;h.sprite=(NoopSprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,NoopSprite.class);Dungeon.hero=h;level.locked=true;return h;}
}
