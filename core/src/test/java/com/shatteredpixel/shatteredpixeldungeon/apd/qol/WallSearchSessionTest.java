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
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.watabou.utils.*;
import org.junit.*;
import java.util.Arrays;
import java.util.HashMap;
import static org.junit.Assert.*;

public class WallSearchSessionTest {
    private Hero oldHero;private Level oldLevel;private int oldDepth,oldBranch;private java.lang.reflect.Field selectorField;private Object oldSelector;
    private GrassDewPickupTest.TestLevel level;private QuietHero hero;
    public static class QuietSprite extends HeroSprite { @Override public void operate(int p){} @Override public void showStatus(int c,String s,Object...a){} }
    public static class QuietHero extends Hero { @Override public void next(){} }
    @Before public void setup()throws Exception{
        HeadlessTestSupport.start();oldHero=Dungeon.hero;oldLevel=Dungeon.level;oldDepth=Dungeon.depth;oldBranch=Dungeon.branch;
        Dungeon.depth=3;Dungeon.branch=0;level=new GrassDewPickupTest.TestLevel();level.setSize(9,9);level.traps=new SparseArray<>();level.blobs=new HashMap<>();level.heaps=new SparseArray<>();level.plants=new SparseArray<>();
        Arrays.fill(level.map,Terrain.EMPTY);Arrays.fill(level.passable,true);Arrays.fill(level.visited,true);Arrays.fill(level.heroFOV,true);level.locked=true;Dungeon.level=level;
        hero=new QuietHero();hero.pos=40;hero.heroClass=HeroClass.WARRIOR;hero.ready=true;hero.fieldOfView=level.heroFOV;Dungeon.hero=hero;
        java.lang.reflect.Field f=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");f.setAccessible(true);Object u=f.get(null);hero.sprite=(QuietSprite)u.getClass().getMethod("allocateInstance",Class.class).invoke(u,QuietSprite.class);
        selectorField=com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("cellSelector");selectorField.setAccessible(true);oldSelector=selectorField.get(null);selectorField.set(null,u.getClass().getMethod("allocateInstance",Class.class).invoke(u,com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector.class));
        level.map[41]=Terrain.WALL;level.map[32]=Terrain.WALL;level.map[50]=Terrain.WALL;
    }
    @After public void restore() throws Exception {selectorField.set(null,oldSelector);Dungeon.hero=oldHero;Dungeon.level=oldLevel;Dungeon.depth=oldDepth;Dungeon.branch=oldBranch;}
    @Test public void manualSearchRemembersPositionAndCountAtNormalCost(){float before=hero.cooldown();hero.search(true);assertEquals(40,hero.wallSearch.anchor());assertEquals(1,hero.wallSearch.searches());assertEquals(2,hero.cooldown()-before,0.001f);hero.search(true);assertEquals(2,hero.wallSearch.searches());}
    @Test public void passiveSearchNeverInventsManualProgress(){hero.search(false);assertFalse(hero.wallSearch.exists());assertFalse(level.searchedWalls.confirmed(level,41));}
    @Test public void combatInterruptAndMovementDoNotEraseInvestigation(){assertTrue(hero.wallSearch.select(hero,41));hero.search(true);hero.curAction=new HeroAction.Attack(new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat());hero.interrupt();hero.pos=49;assertEquals(40,hero.wallSearch.anchor());assertEquals(41,hero.wallSearch.target());assertEquals(1,hero.wallSearch.searches());}
    @Test public void selectingSameTargetPreservesProgress(){hero.wallSearch.select(hero,41);hero.search(true);hero.ready=true;assertTrue(hero.wallSearch.select(hero,41));assertEquals(1,hero.wallSearch.searches());}
    @Test public void deliberateSearchElsewhereStartsANewInvestigation(){hero.wallSearch.select(hero,41);hero.search(true);hero.pos=49;hero.search(true);assertEquals(49,hero.wallSearch.anchor());assertEquals(-1,hero.wallSearch.target());assertEquals(1,hero.wallSearch.searches());}
    @Test public void sessionAndProgressRoundTripWithoutAnActiveCommand(){hero.wallSearch.select(hero,41);hero.search(true);Bundle b=new Bundle();hero.wallSearch.store(b);WallSearchSession saved=new WallSearchSession();saved.restore(b);assertEquals(40,saved.anchor());assertEquals(41,saved.target());assertEquals(1,saved.searches());assertTrue(saved.matches(level));}
    @Test public void legacyAndMalformedSavesDoNotInventTargets(){WallSearchSession saved=new WallSearchSession();saved.restore(new Bundle());assertFalse(saved.exists());Bundle b=new Bundle();b.put("apd_wall_search_session_v1",new Bundle());saved.restore(b);assertFalse(saved.exists());}
    @Test public void wrongFloorOrBranchCannotResumeButMemoryIsRetained(){hero.search(true);Dungeon.depth=4;assertFalse(hero.wallSearch.matches(level));assertNotNull(hero.wallSearch.resume(hero));Dungeon.depth=3;Dungeon.branch=1;assertFalse(hero.wallSearch.matches(level));Dungeon.branch=0;assertTrue(hero.wallSearch.matches(level));}
    @Test public void hiddenOrDistantWallsCannotBeSelectedForRemoteFreeSearch(){level.heroFOV[41]=false;assertFalse(hero.wallSearch.select(hero,41));level.heroFOV[41]=true;level.map[43]=Terrain.WALL;assertFalse(hero.wallSearch.select(hero,43));assertFalse(hero.wallSearch.exists());}
    @Test public void warriorAndRogueFootprintsRemainDifferent(){assertFalse(WallSearchSession.covers(hero,level,40,42));hero.heroClass=HeroClass.ROGUE;assertTrue(WallSearchSession.covers(hero,level,40,42));}
    @Test public void legacyCheckmarkDoesNotCertifyNoHiddenDoor(){level.searchedWalls.mark(level,41);assertTrue(level.searchedWalls.attempted(41));assertFalse(level.searchedWalls.confirmed(level,41));Bundle b=new Bundle();level.searchedWalls.store(b);SearchedWalls loaded=new SearchedWalls();loaded.restore(b,81);assertFalse(loaded.confirmed(level,41));}
    @Test public void completedManualSearchSuppliesProofAndKeepsItThroughSave(){hero.search(true);assertTrue(level.searchedWalls.confirmed(level,41));Bundle b=new Bundle();level.searchedWalls.store(b);SearchedWalls loaded=new SearchedWalls();loaded.restore(b,81);assertTrue(loaded.confirmed(level,41));}
    @Test public void unresolvedOrUnsearchableCheckCannotBeNegative(){level.searchedWalls.recordCompletedDoorCheck(level,41,false);assertFalse(level.searchedWalls.confirmed(level,41));level.map[41]=Terrain.SECRET_DOOR;level.searchedWalls.recordCompletedDoorCheck(level,41,true);assertFalse(level.searchedWalls.confirmed(level,41));assertEquals(Terrain.SECRET_DOOR,level.map[41]);}
    @Test public void unseenOrUncoveredTilesDoNotGainProof(){level.heroFOV[41]=false;hero.search(true);assertFalse(level.searchedWalls.confirmed(level,41));level.heroFOV[41]=true;assertFalse(level.searchedWalls.confirmed(level,41));level.map[43]=Terrain.WALL;hero.search(true);assertFalse(level.searchedWalls.confirmed(level,43));}
    @Test public void terrainChangesInvalidateAnEarlierNegative(){hero.search(true);assertTrue(level.searchedWalls.confirmed(level,41));Level.set(41,Terrain.SECRET_DOOR,level);assertFalse(level.searchedWalls.confirmed(level,41));assertFalse(level.searchedWalls.attempted(41));}
    @Test public void searchingAgainAfterReturningPreservesOriginalActionTiming(){hero.search(true);hero.ready=true;level.locked=false;float before=hero.cooldown();assertNull(hero.wallSearch.resume(hero));assertEquals(2,hero.cooldown()-before,0.001f);assertEquals(2,hero.wallSearch.searches());}
    @Test public void doorlessFloorsKeepNormalSearchTurnsWithoutPhantomWallTargets(){for(int depth:new int[]{1,10,15,20,25,26}){Dungeon.depth=depth;hero.ready=true;hero.wallSearch.clear();float before=hero.cooldown();hero.search(true);assertEquals(2,hero.cooldown()-before,0.001f);assertFalse(hero.wallSearch.exists());assertFalse(hero.wallSearch.select(hero,41));}}
    @Test public void bookshelvesAndImpossibleGeometryCannotBecomeDoorSearchTargets(){level.map[41]=Terrain.BOOKSHELF;assertFalse(hero.wallSearch.select(hero,41));level.map[41]=Terrain.WALL;level.map[32]=level.map[50]=Terrain.EMPTY;assertFalse(hero.wallSearch.select(hero,41));assertFalse(hero.wallSearch.exists());}

    @Test public void legacyUncertainTargetStillResumesOrdinaryPaidSearchWithoutFalseImpossibility(){
        assertTrue(hero.wallSearch.select(hero,41));hero.search(true);hero.ready=true;
        level.visited[32]=level.visited[50]=false;level.heroFOV[32]=level.heroFOV[50]=false;
        assertFalse(DoorSearchEligibility.candidate(level,41));assertTrue(DoorSearchEligibility.couldHaveDoor(level,41));
        Bundle before=new Bundle();hero.wallSearch.store(before);WallSearchSession saved=new WallSearchSession();saved.restore(before);
        assertEquals(41,saved.target());assertEquals(1,saved.searches());
        level.locked=false;float turn=hero.cooldown();assertNull(hero.wallSearch.resume(hero));
        assertEquals(2,hero.cooldown()-turn,0.001f);assertEquals(41,hero.wallSearch.target());assertEquals(2,hero.wallSearch.searches());
    }


    @Test public void missingVisibilityCannotCrashOrClearRememberedWallSearch(){
        assertTrue(hero.wallSearch.select(hero,41));
        level.heroFOV=null;
        assertFalse(WallSearchSession.covers(hero,level,40,41));
        assertFalse(hero.wallSearch.select(hero,41));
        assertEquals(40,hero.wallSearch.anchor());
        assertEquals(41,hero.wallSearch.target());
    }

    @Test public void truncatedVisibilityRejectsUnrepresentedCellWithoutCreatingMemory(){
        level.heroFOV=new boolean[41];
        assertFalse(WallSearchSession.covers(hero,level,40,41));
        assertFalse(hero.wallSearch.select(hero,41));
        assertFalse(hero.wallSearch.exists());
    }

    @Test public void restoredVisibilityKeepsOriginalWallCoverageAndForesightBypass(){
        assertTrue(WallSearchSession.covers(hero,level,40,41));
        assertTrue(hero.wallSearch.select(hero,41));
        level.heroFOV=null;
        // An already-observed Foresight state has always bypassed ordinary FOV in covers.
        assertTrue(WallSearchSession.covers(hero,level,40,41,true));
        assertFalse(WallSearchSession.covers(hero,level,40,41,false));
    }

}
