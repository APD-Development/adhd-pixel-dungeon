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
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.*;
import com.watabou.noosa.Game;
import com.watabou.utils.Point;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Real taps through the settings entry and threshold chooser, not direct editor construction. */
@RunWith(AndroidJUnit4.class)
public class HealingSettingsDeviceTest {
    private static void waitFor(String message,Callable<Boolean> condition)throws Exception{
        long end=SystemClock.uptimeMillis()+60000;
        while(SystemClock.uptimeMillis()<end){if(condition.call())return;Thread.sleep(100);}fail(message);
    }
    private static void frames()throws Exception{
        long before=com.badlogic.gdx.Gdx.graphics.getFrameId();
        waitFor("renderer advances",()->com.badlogic.gdx.Gdx.graphics.getFrameId()>=before+4);
    }
    private static Object field(Object target,String name)throws Exception{
        for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())try{
            java.lang.reflect.Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(target);
        }catch(NoSuchFieldException missing){}throw new NoSuchFieldException(name);
    }
    private static <T> T window(Class<T> type)throws Exception{
        java.util.List<T> windows=TopScoresDeviceTest.find(Game.scene(),type);
        return windows.isEmpty()?null:windows.get(windows.size()-1);
    }
    private static void tap(Point p)throws Exception{
        android.app.Instrumentation in=InstrumentationRegistry.getInstrumentation();long down=SystemClock.uptimeMillis();
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
            MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,p.x,p.y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            in.sendPointerSync(event);event.recycle();Thread.sleep(100);
        }frames();
    }
    private static void button(Object parent,String text)throws Exception{
        frames();AtomicReference<Point> point=new AtomicReference<>();
        TopScoresDeviceTest.render(()->{try{
            for(RedButton b:TopScoresDeviceTest.find((com.watabou.noosa.Group)parent,RedButton.class))if(text.equals(b.text())){
                point.set(b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2));break;
            }
            assertNotNull("visible button "+text,point.get());
        }catch(Exception e){throw new RuntimeException(e);}});tap(point.get());
    }
    @Test public void actualSettingsChooserSavesThresholdAndGatesRealPotionUse()throws Exception{
        int previous=-1,slot=0,oldMode=0;int[] oldCategories=new int[6];String[] oldItems=new String[6];boolean intro=false,initialized=false;HeroClass selected=null;
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
            waitFor("activity ready",()->Game.instance!=null&&Game.scene()!=null);
            previous=SPDSettings.healingConfirmationThreshold();slot=GamesInProgress.curSlot;intro=SPDSettings.intro();selected=GamesInProgress.selectedClass;oldMode=SPDSettings.apdHotbarMode();for(int i=0;i<6;i++){oldCategories[i]=SPDSettings.apdHotbarCategory(i);oldItems[i]=SPDSettings.apdHotbarItem(i);}initialized=true;
            TopScoresDeviceTest.render(()->{
                SPDSettings.healingConfirmationThreshold(-1);SPDSettings.intro(false);
                int free=GamesInProgress.firstEmpty(HeroClass.WARRIOR);assertTrue(free>0);GamesInProgress.curSlot=free;GamesInProgress.selectedClass=HeroClass.WARRIOR;
                Dungeon.hero=null;Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.customSeedText="";Dungeon.initSeed();
                InterlevelScene.mode=InterlevelScene.Mode.DESCEND;Game.switchScene(InterlevelScene.class);
            });
            waitFor("game ready",()->Game.scene() instanceof GameScene&&Dungeon.hero!=null&&Dungeon.hero.ready);frames();
            AtomicReference<WndAPDSettings> parent=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{WndAPDSettings w=new WndAPDSettings(com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection.GAMEPLAY);GameScene.show(w);parent.set(w);});
            TopScoresDeviceTest.render(()->{try{
                APDSettingsPane pane=(APDSettingsPane)field(parent.get(),"pane");RedButton b=(RedButton)field(pane,"btnHealingConfirmation");
                ((ScrollPane)field(parent.get(),"scroll")).scrollTo(0,Math.max(0,b.top()-8));
            }catch(Exception e){throw new RuntimeException(e);}});
            button(parent.get(),"Healing confirmation");waitFor("healing options opens",()->window(WndHealingConfirmation.SettingsWindow.class)!=null);
            WndHealingConfirmation.SettingsWindow settings=window(WndHealingConfirmation.SettingsWindow.class);
            float turnBeforeToggle=Dungeon.hero.cooldown();
            button(settings,"Off");assertEquals("Off button enables confirmation without a slider",0,SPDSettings.healingConfirmationThreshold());
            assertSame("toggle stays open and updates in place",settings,window(WndHealingConfirmation.SettingsWindow.class));
            button(settings,"On");assertEquals(-1,SPDSettings.healingConfirmationThreshold());
            button(settings,"Off");assertEquals(0,SPDSettings.healingConfirmationThreshold());
            assertEquals("changing the toggle spends no turn",turnBeforeToggle,Dungeon.hero.cooldown(),0f);
            TopScoresDeviceTest.screenshot("healing-settings-toggle-on.png");
            button(settings,"Close");
            TopScoresDeviceTest.render(()->parent.get().hide());
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);frames();
            assertEquals("On persists after pause/resume",0,SPDSettings.healingConfirmationThreshold());
            PotionOfHealing enabledPotion=new PotionOfHealing();enabledPotion.quantity(2);enabledPotion.setKnown();
            final float[] enabledTurn={0};
            TopScoresDeviceTest.render(()->{
                Dungeon.hero.HT=100;Dungeon.hero.HP=10;
                assertTrue(enabledPotion.collect(Dungeon.hero.belongings.backpack));enabledTurn[0]=Dungeon.hero.cooldown();
                GameScene.show(new WndUseItem(null,enabledPotion));
            });
            button(window(WndUseItem.class),"DRINK");
            waitFor("On asks even at low HP after a real Drink tap",()->window(WndOptions.class)!=null);
            button(window(WndOptions.class),"CANCEL");
            TopScoresDeviceTest.render(()->{
                assertEquals(2,enabledPotion.quantity());assertEquals(10,Dungeon.hero.HP);
                assertEquals("cancel spends no turn",enabledTurn[0],Dungeon.hero.cooldown(),0f);
                Dungeon.hero.belongings.backpack.items.remove(enabledPotion);
                WndAPDSettings w=new WndAPDSettings(com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection.GAMEPLAY);GameScene.show(w);parent.set(w);
            });
            TopScoresDeviceTest.render(WndHealingConfirmation::settings);
            waitFor("saved On settings reopens",()->window(WndHealingConfirmation.SettingsWindow.class)!=null);
            settings=window(WndHealingConfirmation.SettingsWindow.class);
            TopScoresDeviceTest.screenshot("healing-settings-entry.png");button(settings,"Choose HP threshold");
            waitFor("Choose HP threshold opens editor",()->window(WndHealingConfirmation.ThresholdWindow.class)!=null);
            WndHealingConfirmation.ThresholdWindow editor=window(WndHealingConfirmation.ThresholdWindow.class);frames();
            AtomicReference<Point> sliderPoint=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{try{
                OptionSlider slider=(OptionSlider)field(editor,"percentage");sliderPoint.set(slider.camera().cameraToScreen(slider.left()+slider.width()/2,slider.bottom()-7));
            }catch(Exception e){throw new RuntimeException(e);}});tap(sliderPoint.get());
            TopScoresDeviceTest.render(()->{try{assertEquals(50,((OptionSlider)field(editor,"percentage")).getSelectedValue());assertEquals("unsaved slider leaves the enabled On value intact",0,SPDSettings.healingConfirmationThreshold());}catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.screenshot("healing-settings-editor.png");button(editor,"Save");waitFor("threshold saved",()->SPDSettings.healingConfirmationThreshold()==50);
            TopScoresDeviceTest.render(()->parent.get().hide());frames();
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);frames();assertEquals(50,SPDSettings.healingConfirmationThreshold());
            PotionOfHealing potion=new PotionOfHealing();potion.quantity(2);potion.setKnown();final float[] before={0};final int[] hp={0};
            TopScoresDeviceTest.render(()->{Dungeon.hero.HT=100;Dungeon.hero.HP=90;assertTrue(potion.collect(Dungeon.hero.belongings.backpack));before[0]=Dungeon.hero.cooldown();hp[0]=Dungeon.hero.HP;potion.execute(Dungeon.hero,PotionOfHealing.AC_DRINK);});
            waitFor("saved threshold opens drink warning",()->window(WndOptions.class)!=null);TopScoresDeviceTest.screenshot("healing-settings-confirm.png");button(window(WndOptions.class),"CANCEL");
            TopScoresDeviceTest.render(()->{assertEquals(2,potion.quantity());assertEquals(before[0],Dungeon.hero.cooldown(),0);assertEquals(hp[0],Dungeon.hero.HP);potion.execute(Dungeon.hero,PotionOfHealing.AC_DRINK);});
            waitFor("warning reopens",()->window(WndOptions.class)!=null);button(window(WndOptions.class),"USE HEALING ITEM");
            TopScoresDeviceTest.render(()->{assertEquals(1,potion.quantity());assertNotNull(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing.class));});
            waitFor("hero ready",()->Dungeon.hero.ready);
            TopScoresDeviceTest.render(WndHealingConfirmation::settings);
            waitFor("On toggle available",()->window(WndHealingConfirmation.SettingsWindow.class)!=null);
            button(window(WndHealingConfirmation.SettingsWindow.class),"On");
            assertEquals(-1,SPDSettings.healingConfirmationThreshold());
            TopScoresDeviceTest.screenshot("healing-settings-toggle-off.png");
            button(window(WndHealingConfirmation.SettingsWindow.class),"Close");
            assertEquals("Off button disables the saved threshold",-1,SPDSettings.healingConfirmationThreshold());
            TopScoresDeviceTest.render(()->{
                potion.quantity(2);Dungeon.hero.HP=90;
                assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.CategoryHotbar.chooseItem(0,potion));
                QuickSlotButton.cancel();Toolbar.updateLayout();QuickSlotButton.refresh();
            });frames();
            TopScoresDeviceTest.render(()->{try{assertTrue("settings must be closed before tapping the game hotbar",TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.ui.Window.class).isEmpty());}catch(Exception e){throw new RuntimeException(e);}});
            AtomicReference<Point> hotbarPoint=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{try{
                for(QuickSlotButton b:TopScoresDeviceTest.find(Game.scene(),QuickSlotButton.class))if((Integer)field(b,"slotNum")==0&&b.visible&&b.active){
                    hotbarPoint.set(b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2));break;
                }assertNotNull("actual healing quickslot",hotbarPoint.get());
            }catch(Exception e){throw new RuntimeException(e);}});tap(hotbarPoint.get());
            waitFor("Off tap drinks or exposes unexpected chooser",()->potion.quantity()==1||window(WndUimsdList.class)!=null);
            assertEquals("Off (one tap) must drink directly from an unambiguous healing quickslot",1,potion.quantity());
            assertNull("Off quickslot must not open an item chooser",window(WndUimsdList.class));
            waitFor("ready after Off drink",()->Dungeon.hero.ready);
            TopScoresDeviceTest.render(()->Game.switchScene(TitleScene.class));waitFor("title ready",()->Game.scene() instanceof TitleScene);frames();
        }finally{if(initialized){SPDSettings.healingConfirmationThreshold(previous);SPDSettings.intro(intro);for(int i=0;i<6;i++){SPDSettings.apdHotbarCategory(i,oldCategories[i]);SPDSettings.apdHotbarItem(i,oldItems[i]);}SPDSettings.apdHotbarMode(oldMode);GamesInProgress.curSlot=slot;GamesInProgress.selectedClass=selected;}}
    }
    @Test public void continuingOlderRunAdoptsZeroWithCrystalsAndPreservesItsItemsAndScore()throws Exception{
        int previous=0,slot=0;boolean initialized=false,intro=false;HeroClass selected=null;
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
            waitFor("activity ready",()->Game.instance!=null&&Game.scene()!=null);
            previous=SPDSettings.apdExoticSetting();slot=GamesInProgress.curSlot;selected=GamesInProgress.selectedClass;intro=SPDSettings.intro();initialized=true;
            TopScoresDeviceTest.render(()->{
                SPDSettings.intro(false);SPDSettings.apdExoticChance(0);int free=GamesInProgress.firstEmpty(HeroClass.WARRIOR);assertTrue(free>0);
                GamesInProgress.curSlot=free;GamesInProgress.selectedClass=HeroClass.WARRIOR;Dungeon.hero=null;Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.customSeedText="";Dungeon.initSeed();
                InterlevelScene.mode=InterlevelScene.Mode.DESCEND;Game.switchScene(InterlevelScene.class);
            });waitFor("game ready",()->Game.scene() instanceof GameScene&&Dungeon.hero!=null&&Dungeon.hero.ready);frames();
            AtomicReference<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero> oldHero=new AtomicReference<>();
            final String[] preserved=new String[2];final int[] depth={0};
            TopScoresDeviceTest.render(()->{try{
                com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals crystals=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals();crystals.level(3);assertTrue(crystals.collect());
                Dungeon.gold=211;Dungeon.apdExotic=80;oldHero.set(Dungeon.hero);depth[0]=Dungeon.depth;
                preserved[0]=Dungeon.customRules().identityText();preserved[1]=Dungeon.customScoring().multiplierExact;
                Dungeon.saveAll();Game.switchScene(TitleScene.class);
            }catch(Exception e){throw new RuntimeException(e);}});
            waitFor("left gameplay before preparing legacy save",()->Game.scene() instanceof TitleScene);frames();
            TopScoresDeviceTest.render(()->{try{
                String file=GamesInProgress.gameFile(GamesInProgress.curSlot);
                com.watabou.utils.Bundle save=com.watabou.utils.FileUtils.bundleFromFile(file);
                save.put(com.shatteredpixel.shatteredpixeldungeon.apd.healing.HealingConversionUpgrade.KEY,1);
                com.watabou.utils.FileUtils.bundleToFile(file,save);
                InterlevelScene.mode=InterlevelScene.Mode.CONTINUE;Game.switchScene(InterlevelScene.class);
            }catch(Exception e){throw new RuntimeException(e);}});
            waitFor("old run resumed",()->Game.scene() instanceof GameScene&&Dungeon.hero!=oldHero.get()&&Dungeon.hero.ready);frames();
            TopScoresDeviceTest.render(()->{try{
                assertEquals("current run adopts selected zero",0,Dungeon.apdExotic);assertEquals(0,SPDSettings.apdExoticChance());
                assertEquals(211,Dungeon.gold);assertEquals(depth[0],Dungeon.depth);assertEquals(preserved[0],Dungeon.customRules().identityText());assertEquals(preserved[1],Dungeon.customScoring().multiplierExact);
                assertEquals(3,Dungeon.hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals.class).level());
                int before=Dungeon.LimitedDrops.BAT_HP.count;
                try{for(int i=0;i<32;i++)assertEquals(PotionOfHealing.class,new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bat().createLoot().getClass());}
                finally{Dungeon.LimitedDrops.BAT_HP.count=before;}
                Dungeon.saveAll();com.watabou.utils.Bundle save=com.watabou.utils.FileUtils.bundleFromFile(GamesInProgress.gameFile(GamesInProgress.curSlot));
                assertEquals(2,save.getInt(com.shatteredpixel.shatteredpixeldungeon.apd.healing.HealingConversionUpgrade.KEY));
                assertEquals(0,save.getInt("apd_exotic"));
                SPDSettings.apdExoticChance(80);oldHero.set(Dungeon.hero);Game.switchScene(TitleScene.class);
            }catch(Exception e){throw new RuntimeException(e);}});
            waitFor("left upgraded gameplay",()->Game.scene() instanceof TitleScene);frames();
            TopScoresDeviceTest.render(()->{InterlevelScene.mode=InterlevelScene.Mode.CONTINUE;Game.switchScene(InterlevelScene.class);});
            waitFor("upgraded run resumed again",()->Game.scene() instanceof GameScene&&Dungeon.hero!=oldHero.get()&&Dungeon.hero.ready);frames();
            TopScoresDeviceTest.render(()->{
                assertEquals("later new-run preference does not rewrite this run",0,Dungeon.apdExotic);assertEquals(80,SPDSettings.apdExoticSetting());assertEquals(211,Dungeon.gold);
                assertEquals(preserved[1],Dungeon.customScoring().multiplierExact);Game.switchScene(TitleScene.class);
            });waitFor("title ready",()->Game.scene() instanceof TitleScene);frames();
        }finally{if(initialized){SPDSettings.apdExoticChance(previous);SPDSettings.intro(intro);GamesInProgress.curSlot=slot;GamesInProgress.selectedClass=selected;}}
    }

}
