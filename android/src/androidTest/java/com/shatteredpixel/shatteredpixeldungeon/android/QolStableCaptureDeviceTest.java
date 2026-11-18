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
import android.view.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.*;
import com.watabou.noosa.Game;
import com.watabou.utils.Point;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Native controls and actual renderer, using a newly created disposable emulator run. */
@RunWith(AndroidJUnit4.class)
public class QolStableCaptureDeviceTest {
    private static void waitFor(Callable<Boolean> condition)throws Exception{
        long end=SystemClock.uptimeMillis()+120000;
        while(SystemClock.uptimeMillis()<end){if(condition.call())return;Thread.sleep(100);}fail("QoL fixture timed out");
    }
    private static void touch(Point p)throws Exception{
        android.app.Instrumentation in=InstrumentationRegistry.getInstrumentation();long down=SystemClock.uptimeMillis();
        for(int a:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
            MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),a,p.x,p.y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);in.sendPointerSync(e);e.recycle();Thread.sleep(100);
        }
        Thread.sleep(250);
    }
    private static void settleFrames()throws Exception{
        long start=com.badlogic.gdx.Gdx.graphics.getFrameId();
        waitFor(()->com.badlogic.gdx.Gdx.graphics.getFrameId()>=start+4);
        Thread.sleep(150);
    }
    private static void showItem(Item item,String expected,String image)throws Exception{
        AtomicReference<WndUseItem> shown=new AtomicReference<>();
        TopScoresDeviceTest.render(()->{
            assertTrue(item.info(),item.info().contains(expected));WndUseItem w=new WndUseItem(null,item);GameScene.show(w);shown.set(w);
            try{for(RedButton b:TopScoresDeviceTest.find(w,RedButton.class)){
                Point a=b.camera().cameraToScreen(b.left(),b.top()),z=b.camera().cameraToScreen(b.right(),b.bottom());
                assertTrue("Item action remains visible",a.x>=0&&a.y>=0&&z.x<=Game.width&&z.y<=Game.height);
            }}catch(Exception e){throw new RuntimeException(e);}
        });
        settleFrames();TopScoresDeviceTest.screenshot(image);TopScoresDeviceTest.render(()->shown.get().hide());
    }
    @Test public void grassDewControlsRingBreakdownsSearchedWallAndOfflineLicenseRenderNatively()throws Exception{
        boolean intro=SPDSettings.intro(),auto=SPDSettings.autoCollectGrassDew(),enemies=SPDSettings.autoCollectDewWithEnemies();
        int slot=GamesInProgress.curSlot;HeroClass selected=GamesInProgress.selectedClass;
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
            waitFor(()->Game.instance!=null&&Game.scene()!=null);
            TopScoresDeviceTest.render(()->Game.switchScene(TitleScene.class));waitFor(()->Game.scene() instanceof TitleScene);
            com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel parked=new com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel("fixture","fixture");
            parked.load(new java.io.File(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"qol-memory-fixture"));parked.awaitLoad(30000);
            assertNotNull(parked.catalog());
            TopScoresDeviceTest.render(()->{try{
                for(String name:new String[]{"model","mainModel"}){java.lang.reflect.Field f=UimsdScene.class.getDeclaredField(name);f.setAccessible(true);f.set(null,parked);}
            }catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.render(()->{
                SPDSettings.intro(false);int free=GamesInProgress.firstEmpty(HeroClass.WARRIOR);assertTrue(free>0);
                GamesInProgress.curSlot=free;GamesInProgress.selectedClass=HeroClass.WARRIOR;Dungeon.hero=null;
                Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.customSeedText="";Dungeon.initSeed();
                InterlevelScene.mode=InterlevelScene.Mode.DESCEND;Game.switchScene(InterlevelScene.class);
            });
            waitFor(()->Game.scene() instanceof GameScene&&Dungeon.hero!=null&&Dungeon.hero.ready);
            assertNull("gameplay releases the parked full catalog",parked.catalog());
            assertNull("gameplay releases its query result",parked.result());
            QolItemFeedbackDeviceTest.combinedOriginsAndRatPreferenceRenderAndRespond();
            AtomicReference<WndGrassDewSettings> settings=new AtomicReference<>();Point[] p=new Point[1];
            TopScoresDeviceTest.render(()->{try{
                SPDSettings.autoCollectGrassDew(true);SPDSettings.autoCollectDewWithEnemies(false);
                WndGrassDewSettings w=new WndGrassDewSettings();GameScene.show(w);settings.set(w);
                java.util.List<CheckBox> controls=TopScoresDeviceTest.find(w,CheckBox.class);assertEquals(2,controls.size());
                CheckBox enemy=controls.get(1);assertFalse(enemy.checked());p[0]=enemy.camera().cameraToScreen(enemy.left()+enemy.width()/2,enemy.top()+enemy.height()/2);
            }catch(Exception e){throw new RuntimeException(e);}});
            settleFrames();
            TopScoresDeviceTest.render(()->{try{
                CheckBox enemy=TopScoresDeviceTest.find(settings.get(),CheckBox.class).get(1);
                p[0]=enemy.camera().cameraToScreen(enemy.left()+enemy.width()/2,enemy.top()+enemy.height()/2);
            }catch(Exception e){throw new RuntimeException(e);}});
            touch(p[0]);waitFor(()->SPDSettings.autoCollectDewWithEnemies());
            TopScoresDeviceTest.render(()->assertTrue(SPDSettings.autoCollectDewWithEnemies()));
            settleFrames();TopScoresDeviceTest.screenshot("qol-dew-settings.png");TopScoresDeviceTest.render(()->settings.get().hide());
            Sword sword=new Sword();sword.levelKnown=true;sword.cursedKnown=true;
            TopScoresDeviceTest.render(()->{RingOfForce r=new RingOfForce();r.anonymize();r.levelKnown=true;r.cursedKnown=true;Dungeon.hero.belongings.ring=r;r.activate(Dungeon.hero);});
            showItem(sword,"Force changes this weapon’s base armed damage","qol-force-weapon.png");
            ThrowingKnife knife=new ThrowingKnife();knife.levelKnown=true;knife.cursedKnown=true;
            TopScoresDeviceTest.render(()->{RingOfSharpshooting r=new RingOfSharpshooting();r.anonymize();r.levelKnown=true;r.cursedKnown=true;Dungeon.hero.belongings.misc=r;r.activate(Dungeon.hero);});
            showItem(knife,"2-6 -> 3-8","qol-sharpshooting-weapon.png");
            QolItemFeedbackDeviceTest.prepareSearchableFloor();
            final int[] center={0};
            TopScoresDeviceTest.render(()->{
                int w=Dungeon.level.width();center[0]=(Dungeon.level.height()/2)*w+w/2;
                Dungeon.hero.HT=100000;Dungeon.hero.HP=100000;
                for(int y=-2;y<=2;y++)for(int x=-2;x<=2;x++){
                    int c=center[0]+y*w+x;Level.set(c,Terrain.WALL);Dungeon.level.heroFOV[c]=true;Dungeon.level.visited[c]=true;Dungeon.level.discoverable[c]=true;
                }
                for(int delta:new int[]{-2*w,2*w,-2,2}){
                    int behind=center[0]+delta;Dungeon.level.heroFOV[behind]=false;Dungeon.level.visited[behind]=false;Dungeon.level.mapped[behind]=false;
                }
                Level.set(center[0],Terrain.EMPTY);Dungeon.hero.fieldOfView=Dungeon.level.heroFOV;Dungeon.hero.pos=center[0];Dungeon.hero.sprite.place(center[0]);
            });
            QolItemFeedbackDeviceTest.verifyOldBookshelfMarksAreHidden(center[0]-Dungeon.level.width());
            TopScoresDeviceTest.render(()->{
                int w=Dungeon.level.width();
                Dungeon.hero.search(true);GameScene.updateMap();
                assertTrue("north front face",com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,center[0]-w)>0);
                assertEquals("south lip drawn on visible floor above",com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.OVERHANG,com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,center[0]));
                assertEquals("west partial corner end is unmarked",-1,com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,center[0]-1));
                assertEquals("east partial corner end is unmarked",-1,com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,center[0]+1));
            });
            Thread.sleep(1800);settleFrames();TopScoresDeviceTest.screenshot("qol-searched-wall.png");
            SearchedWallColorDeviceChecks.chooseColorThroughSettingsAndVerifyLiveLayer();
            TopScoresDeviceTest.render(()->{
                int w=Dungeon.level.width();Level.set(center[0]-w,Terrain.EMPTY);Level.set(center[0]+w,Terrain.EMPTY);
                Dungeon.level.heroFOV[center[0]-w]=true;Dungeon.level.heroFOV[center[0]+w]=true;
                GameScene.updateMap();
                assertEquals("straight west section remains marked",com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.RIGHT,com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,center[0]-1));
                assertEquals("straight east section remains marked",com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.LEFT,com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,center[0]+1));
                Level.set(center[0]-w,Terrain.WALL);Level.set(center[0]+w,Terrain.WALL);GameScene.updateMap();
            });
            for(int i=0;i<12;i++){
                waitFor(()->Dungeon.hero.ready);TopScoresDeviceTest.render(()->Dungeon.hero.search(true));Thread.sleep(150);
            }
            settleFrames();
            Thread pauseProbe=new Thread(()->{
                try{Thread.sleep(2500);}catch(InterruptedException stopped){return;}
                for(java.util.Map.Entry<Thread,StackTraceElement[]> entry:Thread.getAllStackTraces().entrySet()){
                    StringBuilder trace=new StringBuilder(entry.getKey().getName()+" "+entry.getKey().getState());
                    for(StackTraceElement line:entry.getValue())trace.append("\n at ").append(line);
                    android.util.Log.w("APD_PAUSE_PROBE",trace.toString());
                }
            },"APD-pause-test-probe");
            pauseProbe.setDaemon(true);pauseProbe.start();
            try{
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
                Thread.sleep(100);
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
            }finally{pauseProbe.interrupt();}
            waitFor(()->Game.scene() instanceof GameScene&&!Game.switchingScene());
            TopScoresDeviceTest.render(()->assertTrue("searched south wall survives resume",Dungeon.level.searchedWalls.shows(Dungeon.level,center[0]+Dungeon.level.width())));
            System.gc();Thread.sleep(300);Runtime runtime=Runtime.getRuntime();
            android.util.Log.i("APD_MEMORY_PROBE","fixed-search-resume used="+(runtime.totalMemory()-runtime.freeMemory())+" limit="+runtime.maxMemory());
            settleFrames();TopScoresDeviceTest.render(WndGplLicense::show);settleFrames();TopScoresDeviceTest.screenshot("qol-offline-license.png");
            TopScoresDeviceTest.render(()->Game.switchScene(TitleScene.class));waitFor(()->Game.scene() instanceof TitleScene);
        }finally{SPDSettings.intro(intro);SPDSettings.autoCollectGrassDew(auto);SPDSettings.autoCollectDewWithEnemies(enemies);GamesInProgress.curSlot=slot;GamesInProgress.selectedClass=selected;}
    }
}
