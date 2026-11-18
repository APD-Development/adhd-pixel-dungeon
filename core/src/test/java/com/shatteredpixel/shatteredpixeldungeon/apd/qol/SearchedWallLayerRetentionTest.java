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
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.tiles.SearchedWallTilemap;
import com.watabou.utils.Bundle;
import org.junit.*;
import java.util.Arrays;
import static org.junit.Assert.*;
public class SearchedWallLayerRetentionTest {
 private Hero oldHero;private Level oldLevel;private int oldDepth,oldBranch;private GrassDewPickupTest.TestLevel level;private Layer layer;
 private static class Layer {
  final SearchedWallTilemap actual=new SearchedWallTilemap();
  int[] visuals(){try{java.lang.reflect.Field field=com.watabou.noosa.Tilemap.class.getDeclaredField("data");field.setAccessible(true);return ((int[])field.get(actual)).clone();}catch(Exception e){throw new RuntimeException(e);}}
  int visual(int p){return visuals()[p];}void updateMap(){actual.updateMap();}void refreshKnownGeometry(){actual.refreshKnownGeometry();}
 }
 @Before public void setup(){HeadlessTestSupport.start();oldHero=Dungeon.hero;oldLevel=Dungeon.level;oldDepth=Dungeon.depth;oldBranch=Dungeon.branch;Dungeon.depth=2;Dungeon.branch=0;level=new GrassDewPickupTest.TestLevel();level.setSize(7,7);Dungeon.level=level;Arrays.fill(level.map,Terrain.WALL);Arrays.fill(level.heroFOV,true);level.map[17]=level.map[31]=Terrain.EMPTY;level.searchedWalls.mark(level,24);layer=new Layer();layer.updateMap();}
 @After public void restore(){Dungeon.hero=oldHero;Dungeon.level=oldLevel;Dungeon.depth=oldDepth;Dungeon.branch=oldBranch;}
 @Test public void actualLayerKeepsValidFaceAndLipThroughFovAndSaveMemoryReload(){
  assertEquals(SearchedWallMarkers.FACE,layer.visual(24));assertEquals(SearchedWallMarkers.OVERHANG,layer.visual(17));int[] before=layer.visuals();Bundle save=new Bundle();level.searchedWalls.store(save);
  Arrays.fill(level.visited,true);Arrays.fill(level.heroFOV,false);layer.refreshKnownGeometry();assertArrayEquals(before,layer.visuals());
  level.searchedWalls.clear();level.searchedWalls.restore(save,49);Layer rebuilt=new Layer();rebuilt.updateMap();assertArrayEquals(before,rebuilt.visuals());Arrays.fill(level.heroFOV,true);rebuilt.refreshKnownGeometry();assertArrayEquals(before,rebuilt.visuals());
 }
 @Test public void invalidatedMemoryMustClearBothPreviouslyProjectedTilesWithoutASceneReload(){
  assertEquals(SearchedWallMarkers.FACE,layer.visual(24));level.map[24]=Terrain.DOOR;level.searchedWalls.invalidate(24);
  layer.refreshKnownGeometry();assertEquals("no stale mark on revealed doorway",-1,layer.visual(24));assertEquals("no stale raised-wall lip",-1,layer.visual(17));
 }
 @Test public void refreshCannotMixAnotherFloorsMemoryIntoAnOldSceneLayer(){
  int[] before=layer.visuals();GrassDewPickupTest.TestLevel other=new GrassDewPickupTest.TestLevel();other.setSize(7,7);Dungeon.level=other;
  layer.refreshKnownGeometry();assertArrayEquals(before,layer.visuals());
 }
 @Test public void clearingAndRestoringMemoryRefreshesExistingLayerWithoutLosingValidSavedMarks(){
  Bundle saved=new Bundle();level.searchedWalls.store(saved);level.searchedWalls.clear();layer.refreshKnownGeometry();assertEquals(-1,layer.visual(24));assertEquals(-1,layer.visual(17));
  level.searchedWalls.restore(saved,49);layer.refreshKnownGeometry();assertEquals(SearchedWallMarkers.FACE,layer.visual(24));assertEquals(SearchedWallMarkers.OVERHANG,layer.visual(17));
 }
 @Test public void actualLayerWaitsForObservedSupportAndThenKeepsTheRememberedMark(){
  level.heroFOV[23]=false;layer=new Layer();layer.updateMap();
  assertEquals(-1,layer.visual(24));assertEquals(-1,layer.visual(17));
  Bundle before=new Bundle();level.searchedWalls.store(before);
  level.heroFOV[23]=true;level.visited[23]=true;layer.refreshKnownGeometry();
  assertEquals(SearchedWallMarkers.FACE,layer.visual(24));assertEquals(SearchedWallMarkers.OVERHANG,layer.visual(17));
  Arrays.fill(level.visited,true);Arrays.fill(level.heroFOV,false);layer.refreshKnownGeometry();
  assertEquals(SearchedWallMarkers.FACE,layer.visual(24));assertEquals(SearchedWallMarkers.OVERHANG,layer.visual(17));
  Bundle after=new Bundle();level.searchedWalls.store(after);assertEquals(before.toString(),after.toString());
 }

}
