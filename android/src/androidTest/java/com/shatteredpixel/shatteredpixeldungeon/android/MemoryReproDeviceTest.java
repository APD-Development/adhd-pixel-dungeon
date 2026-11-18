package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.lifecycle.Lifecycle;
import androidx.test.platform.app.InstrumentationRegistry;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.watabou.noosa.Game;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Disposable emulator-only reproduction; never imports or exports player saves. */
@RunWith(AndroidJUnit4.class)
public class MemoryReproDeviceTest {
 interface Action {void run() throws Exception;}
 static void render(Action action)throws Exception{
  CountDownLatch latch=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();
  Game.runOnRenderThread(()->{try{action.run();}catch(Throwable t){error.set(t);}finally{latch.countDown();}});
  assertTrue(latch.await(60,TimeUnit.SECONDS));if(error.get()!=null)throw new AssertionError(error.get());
 }
 static void waitFor(Callable<Boolean> condition)throws Exception{
  long end=System.currentTimeMillis()+120000;while(System.currentTimeMillis()<end){if(condition.call())return;Thread.sleep(100);}fail("fixture ready");
 }
 static long memory(String stage)throws Exception{
  System.gc();Thread.sleep(200);Runtime r=Runtime.getRuntime();long used=r.totalMemory()-r.freeMemory();
  android.util.Log.i("APD_MEMORY_PROBE",stage+" used="+used+" limit="+r.maxMemory());return used;
 }
 @Test public void gameplaySearchAndResumeRetainedHeapProbe()throws Exception{
  try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
   waitFor(()->Game.instance!=null&&Game.scene()!=null);render(()->Game.switchScene(TitleScene.class));waitFor(()->Game.scene() instanceof TitleScene);
   memory("title");render(()->{
    SPDSettings.intro(false);GamesInProgress.curSlot=GamesInProgress.firstEmpty(HeroClass.WARRIOR);assertTrue(GamesInProgress.curSlot>0);
    GamesInProgress.selectedClass=HeroClass.WARRIOR;Dungeon.hero=null;Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.customSeedText="";Dungeon.initSeed();
    InterlevelScene.mode=InterlevelScene.Mode.DESCEND;Game.switchScene(InterlevelScene.class);
   });waitFor(()->Game.scene() instanceof GameScene&&Dungeon.hero!=null&&Dungeon.hero.ready);
   render(()->{Dungeon.hero.HT=100000;Dungeon.hero.HP=100000;});
   long initial=memory("game-initial");
   for(int cycle=0;cycle<12;cycle++){
    render(()->{for(int i=0;i<20;i++)Dungeon.hero.search(true);});Thread.sleep(700);
    scenario.moveToState(Lifecycle.State.CREATED);Thread.sleep(100);scenario.moveToState(Lifecycle.State.RESUMED);
    waitFor(()->Game.scene() instanceof GameScene&&!Game.switchingScene());memory("resume-"+cycle);
   }
   File heap=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"memory-probe.hprof");
   android.os.Debug.dumpHprofData(heap.getAbsolutePath());
   long end=memory("game-final");assertTrue("retained growth under 24MiB: "+(end-initial),end-initial<24L*1024*1024);
  }
 }
}
