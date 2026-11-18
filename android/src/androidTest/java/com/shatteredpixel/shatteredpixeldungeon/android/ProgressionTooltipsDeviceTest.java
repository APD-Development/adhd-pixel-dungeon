package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.ProgressionPickups;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUseItem;
import com.watabou.noosa.Game;
import com.watabou.utils.Point;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Disposable emulator slot only: actual native popup bounds and new/legacy text. */
@RunWith(AndroidJUnit4.class)
public class ProgressionTooltipsDeviceTest {
    private interface Action { void run() throws Exception; }
    private static void render(Action action)throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();
        Game.runOnRenderThread(()->{try{action.run();}catch(Throwable t){error.set(t);}finally{done.countDown();}});
        assertTrue("render callback",done.await(60,TimeUnit.SECONDS));
        if(error.get()!=null)throw new AssertionError(error.get());
    }
    private static void waitFor(Callable<Boolean> ready)throws Exception {
        long end=android.os.SystemClock.uptimeMillis()+120000;
        while(android.os.SystemClock.uptimeMillis()<end){if(ready.call())return;Thread.sleep(100);}
        fail("Timed out waiting for disposable tooltip fixture");
    }
    private static void show(Item item,String image,boolean legacy)throws Exception {
        AtomicReference<WndUseItem> shown=new AtomicReference<>();
        render(()->{
            String info=item.info();
            assertFalse("Real line breaks",info.contains("\\n"));
            assertTrue(info,info.contains(item instanceof PotionOfStrength?
                    "Strength potions collected by region":"Scrolls of Upgrade collected by region"));
            if(item instanceof PotionOfStrength){
                assertTrue(info,info.contains(legacy?"Floors 1–2: Unknown (older save)":"Floors 1–2: Collected"));
                assertTrue(info,info.contains(legacy?"Floors 3–4: Unknown (older save)":"Floors 3–4: Not collected"));
            }
            WndUseItem window=new WndUseItem(null,item);GameScene.show(window);shown.set(window);
            java.util.List<RedButton> buttons=TopScoresDeviceTest.find(window,RedButton.class);
            assertTrue("Normal item actions remain available",buttons.size()>=3);
            for(RedButton button:buttons){
                assertTrue(button.active&&button.visible);
                Point a=button.camera().cameraToScreen(button.left(),button.top());
                Point b=button.camera().cameraToScreen(button.right(),button.bottom());
                assertTrue("Full action button remains on screen "+button.text(),
                        a.x>=0&&a.y>=0&&b.x<=Game.width&&b.y<=Game.height);
            }
        });
        Thread.sleep(300);TopScoresDeviceTest.screenshot(image);render(()->shown.get().hide());
    }
    @Test public void resourceTooltipsShowExactPairsAndKeepActionsVisibleForNewAndLegacyHistory()throws Exception {
        boolean intro=SPDSettings.intro();int previousSlot=GamesInProgress.curSlot;
        HeroClass previousClass=GamesInProgress.selectedClass;
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
            waitFor(()->Game.instance!=null&&Game.scene()!=null);
            render(()->Game.switchScene(TitleScene.class));waitFor(()->Game.scene() instanceof TitleScene);
            render(()->{
                SPDSettings.intro(false);int slot=GamesInProgress.firstEmpty(HeroClass.WARRIOR);
                assertTrue("A free disposable fixture slot is required",slot>0);
                GamesInProgress.curSlot=slot;GamesInProgress.selectedClass=HeroClass.WARRIOR;
                Dungeon.hero=null;Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.customSeedText="";Dungeon.initSeed();
                InterlevelScene.mode=InterlevelScene.Mode.DESCEND;Game.switchScene(InterlevelScene.class);
            });
            waitFor(()->Game.scene() instanceof GameScene&&Dungeon.hero!=null&&Dungeon.hero.ready);
            PotionOfStrength potion=new PotionOfStrength();ScrollOfUpgrade scroll=new ScrollOfUpgrade();
            render(()->{
                potion.setKnown();scroll.setKnown();
                assertTrue(potion.collect(Dungeon.hero.belongings.backpack));
                assertTrue(scroll.collect(Dungeon.hero.belongings.backpack));
                ProgressionPickups.reset(true);
                PotionOfStrength guaranteed=new PotionOfStrength();ProgressionPickups.mark(guaranteed,2,true);ProgressionPickups.claim(guaranteed);
                ScrollOfUpgrade found=new ScrollOfUpgrade();ProgressionPickups.mark(found,1,false);ProgressionPickups.claim(found);
            });
            show(potion,"resource-strength-current.png",false);show(scroll,"resource-upgrade-current.png",false);
            render(()->ProgressionPickups.reset(false));
            show(potion,"resource-strength-legacy.png",true);show(scroll,"resource-upgrade-legacy.png",true);
            render(()->Game.switchScene(TitleScene.class));waitFor(()->Game.scene() instanceof TitleScene);
        }finally{SPDSettings.intro(intro);GamesInProgress.curSlot=previousSlot;GamesInProgress.selectedClass=previousClass;}
    }
}
