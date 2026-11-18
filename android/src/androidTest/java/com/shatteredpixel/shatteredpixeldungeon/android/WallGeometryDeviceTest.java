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
package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ArmoryRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.EmptyRoom;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.tiles.SearchedWallTilemap;
import com.watabou.noosa.*;
import com.watabou.utils.Bundle;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Callable;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Synthetic geometry on a disposable actual game floor; never presented as a natural seed. */
@RunWith(AndroidJUnit4.class)
public class WallGeometryDeviceTest {
    private static void waitFor(Callable<Boolean> test)throws Exception{long until=SystemClock.uptimeMillis()+120000;while(SystemClock.uptimeMillis()<until){if(test.call())return;Thread.sleep(100);}fail("Wall geometry fixture timed out");}
    private static int visual(SearchedWallTilemap layer,int cell)throws Exception{Field f=Tilemap.class.getDeclaredField("data");f.setAccessible(true);return ((int[])f.get(layer))[cell];}
    private static void waitForBanners()throws Exception{
        waitFor(()->{
            java.util.concurrent.atomic.AtomicBoolean clear=new java.util.concurrent.atomic.AtomicBoolean();
            TopScoresDeviceTest.render(()->{try{
                clear.set(TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.effects.BadgeBanner.class)
                        .stream().noneMatch(b->b.exists&&b.visible));
            }catch(Exception error){throw new RuntimeException(error);}});
            return clear.get();
        });
    }
    private static void assertVisibleMarkerPixels(int cell)throws Exception{
        int count=0;long end=SystemClock.uptimeMillis()+6000;
        do{
            com.watabou.utils.Point[] corners=new com.watabou.utils.Point[2];
            TopScoresDeviceTest.render(()->{int w=Dungeon.level.width();corners[0]=Camera.main.cameraToScreen((cell%w)*16,(cell/w)*16);corners[1]=Camera.main.cameraToScreen((cell%w+1)*16,(cell/w+1)*16);});
            Thread.sleep(250);
            android.graphics.Bitmap bitmap=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
            try{count=0;for(int y=Math.max(0,corners[0].y);y<Math.min(bitmap.getHeight(),corners[1].y);y++)for(int x=Math.max(0,corners[0].x);x<Math.min(bitmap.getWidth(),corners[1].x);x++){int c=bitmap.getPixel(x,y);if(Math.abs(android.graphics.Color.red(c)-255)<=24&&Math.abs(android.graphics.Color.green(c)-102)<=24&&Math.abs(android.graphics.Color.blue(c)-204)<=24)count++;}}finally{bitmap.recycle();}
            if(count>=4)return;
        }while(SystemClock.uptimeMillis()<end);
        TopScoresDeviceTest.screenshot("wall-geometry-missing-pixels-after-lifecycle.png");
        TopScoresDeviceTest.render(()->{try{Level l=Dungeon.level;SearchedWallTilemap layer=TopScoresDeviceTest.find(Game.scene(),SearchedWallTilemap.class).get(0);System.out.println("WALL_PIXEL_FAILURE model="+SearchedWallMarkers.visual(l,cell)+" layer="+visual(layer,cell)+" attempted="+l.searchedWalls.attempted(cell)+" candidate="+DoorSearchEligibility.candidate(l,cell)+" fov="+l.heroFOV[cell]+" visited="+l.visited[cell]+" color="+SPDSettings.searchedWallColor()+" frame="+com.badlogic.gdx.Gdx.graphics.getFrameId());}catch(Exception error){throw new RuntimeException(error);}});
        fail("rendered magenta checkmark pixels absent after waiting for resumed rendering: "+count);
    }
    @Test public void observedGeometryAndRetainedMarksUseTheActualAndroidLayer()throws Exception{
        final boolean intro=SPDSettings.intro();final int[] oldMarkColor={-1};final int oldSlot=GamesInProgress.curSlot;final HeroClass oldClass=GamesInProgress.selectedClass;final int[] slot={-1},wall={-1};final Bundle saved=new Bundle();
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            waitFor(()->Game.scene()!=null&&PixelScene.uiCamera!=null);TopScoresDeviceTest.render(()->oldMarkColor[0]=SPDSettings.searchedWallColor());
            TopScoresDeviceTest.render(()->ShatteredPixelDungeon.switchNoFade(TitleScene.class));waitFor(()->Game.scene() instanceof TitleScene);
            TopScoresDeviceTest.render(()->{SPDSettings.intro(false);slot[0]=GamesInProgress.firstEmpty(HeroClass.WARRIOR);assertTrue(slot[0]>0);GamesInProgress.curSlot=slot[0];GamesInProgress.selectedClass=HeroClass.WARRIOR;Dungeon.hero=null;Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.customSeedText="";Dungeon.initSeed();InterlevelScene.mode=InterlevelScene.Mode.DESCEND;Game.switchScene(InterlevelScene.class);});
            waitFor(()->Game.scene() instanceof GameScene&&Dungeon.hero!=null&&Dungeon.hero.ready);
            QolItemFeedbackDeviceTest.prepareSearchableFloor();TopScoresDeviceTest.render(()->{SPDSettings.searchedWallColor(4);GameScene.updateSearchedWallAppearance();});Thread.sleep(300);waitForBanners();
            TopScoresDeviceTest.render(()->{try{
                Hero hero=Dungeon.hero;Level l=Dungeon.level;int w=l.width(),anchor=w*(l.height()/2)+w/2,p=anchor-w;wall[0]=p;
                // This is synthetic geometry, not a test of randomly generated rooms.
                // Isolate the fixture from seeded special-room perimeters: otherwise
                // a naturally gated room can suppress an unrelated synthetic marker.
                assertTrue(l instanceof RegularLevel);
                Field originalRooms=RegularLevel.class.getDeclaredField("rooms");
                originalRooms.setAccessible(true);
                originalRooms.set(l,new ArrayList<Room>());
                assertNull("synthetic wall has no generated room",((RegularLevel)l).room(p));
                for(Mob mob:new ArrayList<>(l.mobs)){Actor.remove(mob);if(mob.sprite!=null)mob.sprite.killAndErase();}l.mobs.clear();
                for(int y=-5;y<=5;y++)for(int x=-5;x<=5;x++){int c=anchor+y*w+x;Level.set(c,Terrain.EMPTY);l.heroFOV[c]=l.visited[c]=l.discoverable[c]=true;}
                for(int x=-3;x<=3;x++)Level.set(p+x,Terrain.WALL);
                hero.curAction=null;hero.lastAction=null;hero.pos=anchor;hero.sprite.place(anchor);hero.fieldOfView=l.heroFOV;Camera.main.snapTo(hero.sprite.center());
                Level.set(p-1,Terrain.EMPTY);l.heroFOV[p-1]=l.visited[p-1]=l.mapped[p-1]=false;
                l.heroFOV[p-w]=l.visited[p-w]=l.mapped[p-w]=false;
                Bundle legacy=new Bundle();legacy.put("apd_searched_walls_v1",new int[]{p});l.searchedWalls.restore(legacy,l.length());GameScene.updateMap();GameScene.updateFog();
                SearchedWallTilemap layer=TopScoresDeviceTest.find(Game.scene(),SearchedWallTilemap.class).get(0);
                assertTrue(l.searchedWalls.attempted(p));assertFalse(DoorSearchEligibility.candidate(l,p));assertEquals(-1,visual(layer,p));
                l.heroFOV[p-1]=l.visited[p-1]=true;GameScene.updateFog();layer.refreshKnownGeometry();assertFalse(DoorSearchEligibility.candidate(l,p));assertEquals(-1,visual(layer,p));
            }catch(Exception error){throw new RuntimeException(error);}});
            Thread.sleep(300);TopScoresDeviceTest.screenshot("wall-geometry-corner-no-false-check.png");
            TopScoresDeviceTest.render(()->{try{
                Level l=Dungeon.level;int p=wall[0],w=l.width();SearchedWallTilemap layer=TopScoresDeviceTest.find(Game.scene(),SearchedWallTilemap.class).get(0);
                Level.set(p-1,Terrain.WALL);GameScene.updateMap();GameScene.updateFog();layer.refreshKnownGeometry();assertTrue(DoorSearchEligibility.candidate(l,p));assertEquals(SearchedWallMarkers.FACE,visual(layer,p));
                for(int t:new int[]{Terrain.WALL,Terrain.EMPTY,Terrain.WATER}){l.map[p-w]=t;layer.refreshKnownGeometry();assertTrue(DoorSearchEligibility.candidate(l,p));assertEquals("unseen backing cannot affect actual render data",SearchedWallMarkers.FACE,visual(layer,p));}
                l.map[p-w]=Terrain.EMPTY;l.searchedWalls.store(saved);
                l.searchedWalls.clear();layer.refreshKnownGeometry();assertEquals(-1,visual(layer,p));l.searchedWalls.restore(saved,l.length());layer.refreshKnownGeometry();assertEquals(SearchedWallMarkers.FACE,visual(layer,p));
            }catch(Exception error){throw new RuntimeException(error);}});
            Thread.sleep(300);TopScoresDeviceTest.screenshot("wall-geometry-valid-searched-face.png");
            assertVisibleMarkerPixels(wall[0]);
            // A disposable real-renderer fixture for an observed iron-key entrance.
            final Object[] oldRooms={null};final int[] keyedDoor={-1};
            TopScoresDeviceTest.render(()->{try{
                Level l=Dungeon.level;assertTrue(l instanceof RegularLevel);
                int p=wall[0],w=l.width(),x=p%w,y=p/w;
                Field rooms=RegularLevel.class.getDeclaredField("rooms");rooms.setAccessible(true);
                oldRooms[0]=rooms.get(l);
                ArmoryRoom gated=new ArmoryRoom();gated.set(x-3,y-5,x+4,y);
                Room.Door entrance=new Room.Door(x+3,y);entrance.set(Room.Door.Type.LOCKED);
                gated.connected.put(new EmptyRoom(),entrance);
                rooms.set(l,new ArrayList<>(Collections.singletonList(gated)));
                keyedDoor[0]=entrance.y*w+entrance.x;
                Level.set(keyedDoor[0],Terrain.LOCKED_DOOR);
                l.heroFOV[keyedDoor[0]]=l.visited[keyedDoor[0]]=l.mapped[keyedDoor[0]]=false;
                SearchedWallTilemap layer=TopScoresDeviceTest.find(Game.scene(),SearchedWallTilemap.class).get(0);
                layer.refreshKnownGeometry();
                assertTrue("original doorway geometry stays eligible",DoorSearchEligibility.candidate(l,p));
                assertEquals("unknown entrance must not reveal gated room",SearchedWallMarkers.FACE,visual(layer,p));
                l.heroFOV[keyedDoor[0]]=true;
                GameScene.updateMap();GameScene.updateFog();layer.refreshKnownGeometry();
                assertEquals("observed locked room wall must not show check", -1,visual(layer,p));
                assertEquals("projected raised marker must also disappear", -1,visual(layer,p-w));
                assertTrue("old search history remains",l.searchedWalls.attempted(p));
            }catch(Exception error){throw new RuntimeException(error);}});
            Thread.sleep(300);TopScoresDeviceTest.screenshot("wall-geometry-locked-room-no-check.png");
            TopScoresDeviceTest.render(()->{try{
                Level l=Dungeon.level;Field rooms=RegularLevel.class.getDeclaredField("rooms");rooms.setAccessible(true);
                rooms.set(l,oldRooms[0]);
                Level.set(keyedDoor[0],Terrain.WALL);
                SearchedWallTilemap layer=TopScoresDeviceTest.find(Game.scene(),SearchedWallTilemap.class).get(0);
                layer.refreshKnownGeometry();
                assertEquals("normal marker returns with original room context",SearchedWallMarkers.FACE,visual(layer,wall[0]));
            }catch(Exception error){throw new RuntimeException(error);}});
            assertVisibleMarkerPixels(wall[0]);
            TopScoresDeviceTest.render(()->{Camera.main.zoom(Camera.main.zoom+1);Camera.main.snapTo(Dungeon.hero.sprite.center());});assertVisibleMarkerPixels(wall[0]);
            TopScoresDeviceTest.render(()->{com.watabou.utils.PointF center=Dungeon.hero.sprite.center();Camera.main.snapTo(new com.watabou.utils.PointF(center.x+160,center.y));});Thread.sleep(200);
            TopScoresDeviceTest.render(()->Camera.main.snapTo(Dungeon.hero.sprite.center()));assertVisibleMarkerPixels(wall[0]);
            app.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);app.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);waitFor(()->Game.scene() instanceof GameScene&&Dungeon.hero.ready&&!Game.switchingScene());assertVisibleMarkerPixels(wall[0]);
            TopScoresDeviceTest.render(()->ShatteredPixelDungeon.switchNoFade(GameScene.class));waitFor(()->Game.scene() instanceof GameScene&&Dungeon.hero.ready&&!Game.switchingScene());waitForBanners();assertVisibleMarkerPixels(wall[0]);
            TopScoresDeviceTest.screenshot("wall-geometry-retained-after-zoom-pan-pause-rebuild.png");
            TopScoresDeviceTest.render(()->{try{
                Level l=Dungeon.level;int p=wall[0];SearchedWallTilemap layer=TopScoresDeviceTest.find(Game.scene(),SearchedWallTilemap.class).get(0);
                l.heroFOV[p]=false;l.heroFOV[p-1]=false;l.heroFOV[p+1]=false;layer.refreshKnownGeometry();assertTrue(DoorSearchEligibility.candidate(l,p));assertEquals(SearchedWallMarkers.FACE,visual(layer,p));
                Bundle after=new Bundle();l.searchedWalls.store(after);assertEquals(saved.toString(),after.toString());
                l.heroFOV[p]=true;Level.set(p,Terrain.DOOR);GameScene.updateMap();GameScene.updateFog();layer.refreshKnownGeometry();assertEquals(-1,visual(layer,p));assertEquals(-1,SearchedWallMarkers.visual(l,p-l.width()));
            }catch(Exception error){throw new RuntimeException(error);}});
            Thread.sleep(300);TopScoresDeviceTest.screenshot("wall-geometry-door-no-stale-check.png");
        }finally{if(oldMarkColor[0]>=0)SPDSettings.searchedWallColor(oldMarkColor[0]);SPDSettings.intro(intro);if(slot[0]>0)Dungeon.deleteGame(slot[0],true);GamesInProgress.curSlot=oldSlot;GamesInProgress.selectedClass=oldClass;}
    }
}
