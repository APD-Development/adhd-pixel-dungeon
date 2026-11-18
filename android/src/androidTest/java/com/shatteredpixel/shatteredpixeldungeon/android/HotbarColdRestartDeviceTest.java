package com.shatteredpixel.shatteredpixeldungeon.android;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.badlogic.gdx.Files;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.CategoryHotbar;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** Two independently invoked instrumentation phases separated by adb am force-stop.
 * Uses only APIs present in TEST285 so the same test APK can demonstrate the old defect. */
@RunWith(AndroidJUnit4.class)
public class HotbarColdRestartDeviceTest {
    private static void render(Runnable r)throws Exception{
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();
        Game.runOnRenderThread(()->{try{r.run();}catch(Throwable e){error.set(e);}finally{done.countDown();}});
        assertTrue("Render thread timeout",done.await(90,TimeUnit.SECONDS));if(error.get()!=null)throw new AssertionError(error.get());
    }
    private static File root(){return new File(ApplicationProvider.getApplicationContext().getCacheDir(),"hotbar-cold-restart");}
    private static void directory(){FileUtils.setDefaultFileProperties(Files.FileType.Absolute,root().getAbsolutePath()+File.separator);}
    private static void title()throws Exception{
        long until=System.currentTimeMillis()+90000;while((Game.scene()==null || PixelScene.uiCamera==null)&&System.currentTimeMillis()<until)Thread.sleep(100);
        assertNotNull(Game.scene());render(()->ShatteredPixelDungeon.switchNoFade(TitleScene.class));Thread.sleep(800);
    }
    private static String[] assignments(){String[] values=new String[6];for(int i=0;i<6;i++){Item item=Dungeon.quickslot.getItem(i);values[i]=item==null?"empty":item.getClass().getName()+"/"+item.quantity();}return values;}
    @Test public void prepareManuallyAssignedSavedRuns()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            title();render(()->{try{
                directory();Bundle fixture=new Bundle();fixture.put("version",1);fixture.put("previous_waterskin",SPDSettings.waterskinStartingSlot());fixture.put("previous_mode",SPDSettings.apdHotbarMode());fixture.put("previous_recording",SPDSettings.apdRecording());
                String[] types=new String[6];int[] categories=new int[6];for(int i=0;i<6;i++){types[i]=SPDSettings.apdHotbarItem(i);categories[i]=SPDSettings.apdHotbarCategory(i);}fixture.put("previous_types",types);fixture.put("previous_categories",categories);
                SPDSettings.apdRecording(false);SPDSettings.challenges(0);SPDSettings.intro(false);
                for(int scenario=0;scenario<5;scenario++){
                    ApdRecorder.reset();GamesInProgress.curSlot=scenario+1;GamesInProgress.selectedClass=HeroClass.DUELIST;Dungeon.seed=774201+scenario;Dungeon.customSeedText="";Dungeon.daily=false;Dungeon.dailyReplay=false;SPDSettings.waterskinStartingSlot(1);
                    Dungeon.init();Dungeon.level=Dungeon.newLevel();Dungeon.switchLevel(Dungeon.level,-1);
                    Waterskin skin=Dungeon.hero.belongings.getItem(Waterskin.class);ThrowingSpike spikes=Dungeon.hero.belongings.getItem(ThrowingSpike.class);assertNotNull(skin);assertNotNull(spikes);
                    Dungeon.quickslot.setSlot(1,skin);Dungeon.quickslot.setSlot(4,spikes);Dungeon.quickslot.setSlot(2,Dungeon.hero.belongings.getItem(Food.class));
                    Item scroll=new ScrollOfIdentify();Dungeon.hero.belongings.backpack.items.add(scroll);Dungeon.quickslot.setSlot(3,scroll);
                    Item potion=new PotionOfHealing();Dungeon.hero.belongings.backpack.items.add(potion);Dungeon.quickslot.setSlot(5,potion);
                    if(scenario==0){
                        // Persist an older arrangement, then perform only actual UI shortcut commands.
                        // The test ends on Title, so an Activity pause cannot save these commands for us.
                        SPDSettings.apdHotbarMode(0);Dungeon.quickslot.setSlot(1,Dungeon.hero.belongings.getItem(Food.class));Dungeon.quickslot.setSlot(2,spikes);Dungeon.quickslot.setSlot(4,skin);fixture.put("old_disk_slots_0",assignments());Dungeon.saveGame(1);
                        Dungeon.hero.ready=true;com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.set(1,skin);com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.set(4,spikes);com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.set(2,Dungeon.hero.belongings.getItem(Food.class));
                    }
                    if(scenario==2){Dungeon.quickslot.setSlot(5,skin);Dungeon.quickslot.clearSlot(2);}
                    if(scenario==3)Dungeon.quickslot.clearItem(skin);
                    if(scenario==4){CategoryHotbar.chooseItem(1,skin);CategoryHotbar.chooseItem(4,spikes);Dungeon.quickslot.convertToPlaceholder(spikes);assertTrue(Dungeon.hero.belongings.backpack.items.remove(spikes));}
                    SPDSettings.apdHotbarMode(scenario);fixture.put("slots_"+scenario,assignments());if(scenario!=0)Dungeon.saveGame(scenario+1);
                }
                // A preference change for future runs must not change any saved manual assignments.
                SPDSettings.waterskinStartingSlot(4);fixture.put("prepared_preference",4);FileUtils.bundleToFile("fixture.dat",fixture);
                System.out.println("HOTBAR_COLD_PREPARED five real saved runs; Waterskin2/spikes5, conflicting preference, Automatic, unassigned and placeholder cases.");
            }catch(Exception e){throw new RuntimeException(e);}});
        }
    }
    @Test public void coldProcessRestoresExistingSavesWithoutReapplyingStartingPreference()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            title();render(()->{try{
                directory();Bundle fixture=FileUtils.bundleFromFile("fixture.dat");assertEquals(1,fixture.getInt("version"));assertEquals("Preference persisted through a real process stop",fixture.getInt("prepared_preference"),SPDSettings.waterskinStartingSlot());
                java.util.List<String> mismatches=new java.util.ArrayList<>();
                for(int scenario=0;scenario<5;scenario++){
                    SPDSettings.waterskinStartingSlot(scenario==0?1:scenario==2?-1:4);SPDSettings.apdHotbarMode(scenario);
                    GamesInProgress.curSlot=scenario+1;Dungeon.hero=null;Dungeon.quickslot=new QuickSlot(6);Dungeon.loadGame(scenario+1);
                    System.out.println("HOTBAR_COLD_RESTORED scenario="+scenario+" mode="+SPDSettings.apdHotbarMode()+" preference="+SPDSettings.waterskinStartingSlot()+" assignments="+java.util.Arrays.toString(assignments()));
                    // An upgrade must preserve the actual old disk record. It cannot invent
                    // manual edits which TEST285 never wrote. Fresh corrected saves below
                    // must retain the complete manually edited layout instead.
                    boolean oldDisk=Boolean.parseBoolean(androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("apdOldDiskState","false"));
                    String[] expected=fixture.getStringArray(oldDisk && scenario==0?"old_disk_slots_0":"slots_"+scenario);
                    if(!java.util.Arrays.equals(expected,assignments()))mismatches.add("scenario="+scenario+" expected="+java.util.Arrays.toString(expected)+" actual="+java.util.Arrays.toString(assignments()));
                    assertEquals(scenario,SPDSettings.apdHotbarMode());
                }
                assertTrue("Manual Waterskin and throwing-spike assignments must survive cold restart: "+mismatches,mismatches.isEmpty());
                assertEquals(Waterskin.class.getName(),SPDSettings.apdHotbarItem(1));assertEquals(ThrowingSpike.class.getName(),SPDSettings.apdHotbarItem(4));
                // Display the resumed run using the normal game scene, with all six Classic slots visible when the viewport permits.
                SPDSettings.apdHotbarMode(0);GamesInProgress.curSlot=1;Dungeon.loadGame(1);Dungeon.level=Dungeon.loadLevel(1);Dungeon.switchLevel(Dungeon.level,Dungeon.hero.pos);InterlevelScene.mode=InterlevelScene.Mode.CONTINUE;ShatteredPixelDungeon.switchNoFade(GameScene.class);
            }catch(Exception e){throw new RuntimeException(e);}});
            long until=System.currentTimeMillis()+60000;while(!(Game.scene() instanceof GameScene)&&System.currentTimeMillis()<until)Thread.sleep(100);assertTrue(Game.scene() instanceof GameScene);Thread.sleep(900);
            PlayRunDeviceTest.visibilityScreenshot("hotbar-cold-restored.png");
            render(()->ShatteredPixelDungeon.switchNoFade(TitleScene.class));Thread.sleep(500);
            render(()->{try{Bundle fixture=FileUtils.bundleFromFile("fixture.dat");SPDSettings.waterskinStartingSlot(fixture.getInt("previous_waterskin"));SPDSettings.apdHotbarMode(fixture.getInt("previous_mode"));SPDSettings.apdRecording(fixture.getBoolean("previous_recording"));
                String[] types=fixture.getStringArray("previous_types");int[] categories=fixture.getIntArray("previous_categories");for(int i=0;i<6;i++){SPDSettings.apdHotbarItem(i,types[i]);SPDSettings.apdHotbarCategory(i,categories[i]);}
            }catch(Exception e){throw new RuntimeException(e);}});
        }
    }
}
