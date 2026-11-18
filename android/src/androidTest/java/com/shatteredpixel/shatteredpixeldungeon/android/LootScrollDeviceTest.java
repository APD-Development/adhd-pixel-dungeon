package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.os.SystemClock;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.loot.*;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfForwarding;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUseItem;
import com.watabou.noosa.Game;
import com.watabou.utils.Point;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Disposable emulator fixture. No existing slot is overwritten or deleted. */
@RunWith(AndroidJUnit4.class)
public class LootScrollDeviceTest {
    private static void render(Runnable action)throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();
        Game.runOnRenderThread(()->{try{action.run();}catch(Throwable t){error.set(t);}finally{done.countDown();}});
        assertTrue("render callback",done.await(60,TimeUnit.SECONDS));
        if(error.get()!=null)throw new AssertionError(error.get());
    }
    private static void waitFor(Callable<Boolean> ready)throws Exception {
        long end=SystemClock.uptimeMillis()+120000;
        while(SystemClock.uptimeMillis()<end){if(ready.call())return;Thread.sleep(100);}
        fail("Timed out waiting for dungeon state");
    }
    private static void tap(Point p)throws Exception {
        long down=SystemClock.uptimeMillis();
        MotionEvent start=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,p.x,p.y,0);
        start.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try{assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(start,true));}finally{start.recycle();}
        Thread.sleep(120);
        MotionEvent end=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,p.x,p.y,0);
        end.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try{assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(end,true));}finally{end.recycle();}
    }
    private static void showAndRead(Scroll scroll,String image)throws Exception {
        AtomicReference<Point> point=new AtomicReference<>();
        render(()->{try{
            if (scroll instanceof ScrollOfGathering) {
                assertEquals(com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.SCROLL_GATHERING,scroll.image());
                assertEquals(-1,scroll.icon);
            }
            assertTrue(Dungeon.hero.ready);assertTrue(scroll.collect(Dungeon.hero.belongings.backpack));
            WndUseItem window=new WndUseItem(null,scroll);GameScene.show(window);
            String label=scroll.actionName(Scroll.AC_READ,Dungeon.hero);
            for(RedButton button:TopScoresDeviceTest.find(window,RedButton.class))if(label.equals(button.text())){
                assertTrue(button.active&&button.visible);
                Point p=button.camera().cameraToScreen(button.centerX(),button.centerY());
                assertTrue(p.x>=0&&p.x<Game.width&&p.y>=0&&p.y<Game.height);point.set(p);
            }
            assertNotNull("Native item window exposes Read",point.get());
        }catch(Exception e){throw new RuntimeException(e);}});
        Thread.sleep(400);TopScoresDeviceTest.screenshot(image);tap(point.get());
        waitFor(()->Dungeon.hero.ready&&!Dungeon.hero.belongings.backpack.contains(scroll));
    }
    private static void looseFood(String uid)throws Exception {
        render(()->{
            int target=LootPlacement.besideDownstairs(Dungeon.level),cell=-1;
            for(int i=0;i<Dungeon.level.length();i++)if(i!=target&&LootPlacement.safe(Dungeon.level,i)){cell=i;break;}
            assertTrue(cell>=0);Heap heap=Dungeon.level.heaps.get(cell);
            if(heap==null){heap=new Heap();heap.pos=cell;Dungeon.level.heaps.put(cell,heap);}
            Pasty food=new Pasty();food.quantity(7);food.apdUid=uid;heap.items.add(food);
            if(heap.sprite==null)GameScene.add(heap);
        });
    }
    @Test public void bothScrollsReadThroughNativeItemButtonsAndShipmentSurvivesContinue()throws Exception {
        boolean intro=SPDSettings.intro();int previousSlot=GamesInProgress.curSlot;
        HeroClass previousClass=GamesInProgress.selectedClass;
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
            waitFor(()->Game.instance!=null&&Game.scene()!=null);
            render(()->Game.switchScene(TitleScene.class));waitFor(()->Game.scene() instanceof TitleScene);
            render(()->{
                SPDSettings.intro(false);int slot=GamesInProgress.firstEmpty(HeroClass.WARRIOR);
                assertTrue("A free fixture slot is required",slot>0);
                GamesInProgress.curSlot=slot;GamesInProgress.selectedClass=HeroClass.WARRIOR;
                Dungeon.hero=null;Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.customSeedText="";Dungeon.initSeed();
                InterlevelScene.mode=InterlevelScene.Mode.DESCEND;Game.switchScene(InterlevelScene.class);
            });
            waitFor(()->Game.scene() instanceof GameScene&&Dungeon.hero!=null&&Dungeon.hero.ready);
            looseFood("native-gather-food");showAndRead(new ScrollOfGathering(),"loot-scroll-gather-description.png");
            render(()->{
                Heap heap=Dungeon.level.heaps.get(LootPlacement.besideDownstairs(Dungeon.level));assertNotNull(heap);
                assertTrue(heap.items.stream().anyMatch(i->"native-gather-food".equals(i.apdUid)&&i.quantity()==7));
            });
            TopScoresDeviceTest.screenshot("loot-scroll-gather-result.png");
            looseFood("native-forward-food");showAndRead(new ScrollOfForwarding(),"loot-scroll-forward-description.png");
            render(()->{
                assertEquals(1,LootTransfers.state.pending().size());
                assertEquals(6,LootTransfers.state.pending().get(0).destinationDepth());
                try{Dungeon.saveAll();}catch(Exception e){throw new RuntimeException(e);}
                Game.switchScene(TitleScene.class);
            });
            waitFor(()->Game.scene() instanceof TitleScene);
            render(()->{Dungeon.hero=null;InterlevelScene.mode=InterlevelScene.Mode.CONTINUE;Game.switchScene(InterlevelScene.class);});
            waitFor(()->Game.scene() instanceof GameScene&&Dungeon.hero!=null&&Dungeon.hero.ready);
            render(()->assertEquals(1,LootTransfers.state.pending().size()));
            TopScoresDeviceTest.screenshot("loot-scroll-pending-after-continue.png");
            // Leave the disposable test run saved; never delete/replace an existing player's slot.
            render(()->{try{Dungeon.saveAll();}catch(Exception e){throw new RuntimeException(e);}Game.switchScene(TitleScene.class);});
            waitFor(()->Game.scene() instanceof TitleScene);
        }finally{SPDSettings.intro(intro);GamesInProgress.curSlot=previousSlot;GamesInProgress.selectedClass=previousClass;}
    }
}
