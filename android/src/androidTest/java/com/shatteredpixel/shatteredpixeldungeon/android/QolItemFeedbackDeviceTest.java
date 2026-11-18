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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.*;
import com.watabou.noosa.Game;
import com.watabou.utils.Point;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Native controls and actual renderer, using a newly created disposable emulator run. */
@RunWith(AndroidJUnit4.class)
public class QolItemFeedbackDeviceTest {
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
    private static void showItem(Item item,String expected,String image)throws Exception{
        showItem(item,expected,image,false);
    }
    private static void showItem(Item item,String expected,String image,boolean captureBottom)throws Exception{
        showItem(item,expected,image,captureBottom,false);
    }
    private static void showItem(Item item,String expected,String image,boolean captureBottom,boolean retainedPanel)throws Exception{
        for(boolean useWindow:new boolean[]{false,true}){
            AtomicReference<WndInfoItem> shown=new AtomicReference<>();boolean[] temporarilyOwned={false};
            float[] time=new float[1];int[] hp=new int[1];String[] saved=new String[1];
            TopScoresDeviceTest.render(()->{
                time[0]=Dungeon.hero.cooldown();hp[0]=Dungeon.hero.HP;
                com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();item.storeInBundle(before);saved[0]=before.toString();
                assertTrue(item.info(),item.info().contains(expected));
                if(useWindow&&!Dungeon.hero.belongings.contains(item)){Dungeon.hero.belongings.backpack.items.add(item);temporarilyOwned[0]=true;}
                WndInfoItem window=useWindow?new WndUseItem(null,item):new WndInfoItem(item);
                GameScene.show(window);shown.set(window);
            });
            Thread.sleep(300);
            TopScoresDeviceTest.render(()->{try{
                WndInfoItem window=shown.get();assertDescriptionViewport(window);
                ScrollPane scroll=TopScoresDeviceTest.find(window,ScrollPane.class).get(0);
                boolean found=false;
                for(RenderedTextBlock text:TopScoresDeviceTest.find(scroll.content(),RenderedTextBlock.class)){
                    if(text.text().equals(item.info())){found=true;assertTrue(text.text().contains(expected));}
                }
                assertTrue("Approved ordinary description remains intact",found);
                for(RenderedTextBlock body:TopScoresDeviceTest.find(scroll.content(),RenderedTextBlock.class)){
                    assertTrue("Description reserves space below its final line",scroll.content().height()>=Math.ceil(body.height())+2);
                    float bottomScroll=Math.max(0,scroll.content().height()-scroll.content().camera.height);
                    assertTrue("Last line is reachable without clipping",body.bottom()-bottomScroll<=scroll.content().camera.height-2);
                }

                java.util.List<RedButton> buttons=TopScoresDeviceTest.find(window,RedButton.class);
                int retainedExpanders=0;
                for(RedButton button:buttons){
                    if(retainedPanel&&button.text().equals("Show calculations"))retainedExpanders++;
                    else assertNotEquals("No unnecessary expander on these ordinary summaries","Show calculations",button.text());
                    assertNotEquals("No unnecessary expander on these ordinary summaries","Hide calculations",button.text());
                    Point a=button.camera().cameraToScreen(button.left(),button.top()),z=button.camera().cameraToScreen(button.right(),button.bottom());
                    assertTrue("Item action remains visible",a.x>=0&&a.y>=0&&z.x<=Game.width&&z.y<=Game.height);
                }
                int expectedButtons=useWindow&&Dungeon.hero.isAlive()&&Dungeon.hero.belongings.contains(item)?item.actions(Dungeon.hero).size():0;
                assertEquals("Only the explicitly retained panel appears",retainedPanel?1:0,retainedExpanders);
                assertEquals("Only existing item actions and the retained control appear",expectedButtons+(retainedPanel?1:0),buttons.size());
                assertEquals(time[0],Dungeon.hero.cooldown(),0);assertEquals(hp[0],Dungeon.hero.HP);
                com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();item.storeInBundle(after);assertEquals(saved[0],after.toString());
            }catch(Exception e){throw new RuntimeException(e);}});
            if(useWindow){
                TopScoresDeviceTest.screenshot(image);
                if(captureBottom){
                    TopScoresDeviceTest.render(()->{try{
                        ScrollPane scroll=TopScoresDeviceTest.find(shown.get(),ScrollPane.class).get(0);
                        scroll.scrollTo(0,Math.max(0,scroll.content().height()-scroll.height()));
                    }catch(Exception e){throw new RuntimeException(e);}});
                    Thread.sleep(200);TopScoresDeviceTest.screenshot(image.replace(".png","-bottom.png"));
                }
            }
            TopScoresDeviceTest.render(()->{shown.get().hide();if(temporarilyOwned[0])Dungeon.hero.belongings.backpack.items.remove(item);});
        }
    }
    private static void calculationWindow(java.util.concurrent.Callable<Window> factory,String expected,String screenshot)throws Exception{
        AtomicReference<Window> shown=new AtomicReference<>();
        TopScoresDeviceTest.render(()->{try{Window window=factory.call();GameScene.show(window);shown.set(window);}catch(Exception e){throw new RuntimeException(e);}});
        checkCalculationWindow(shown.get(),expected,screenshot);
    }
    private static void assertDescriptionViewport(Window window){try{
        ScrollPane scroll=TopScoresDeviceTest.find(window,ScrollPane.class).get(0);
        Point origin=scroll.camera().cameraToScreen(scroll.left(),scroll.top());
        assertEquals("Description viewport follows the resized/offset window horizontally",origin.x,scroll.content().camera.x,0);
        assertEquals("Description viewport follows the resized/offset window vertically",origin.y,scroll.content().camera.y,0);
        assertEquals(scroll.camera().zoom,scroll.content().camera.zoom,0);
        assertEquals((int)scroll.width(),scroll.content().camera.width,0);
        assertEquals((int)scroll.height(),scroll.content().camera.height,0);
        for(RenderedTextBlock text:TopScoresDeviceTest.find(scroll.content(),RenderedTextBlock.class))assertSame("Body uses the clipped content camera",scroll.content().camera,text.camera());
    }catch(Exception e){throw new RuntimeException(e);}}
    private static void checkCalculationWindow(Window window,String expected,String screenshot)throws Exception{
        AtomicReference<RedButton> expander=new AtomicReference<>();Point[] at=new Point[1];float[] time=new float[1],plainHeight=new float[1];int[] hp=new int[1],size=new int[2];
        java.util.Map<Button,float[]> controls=new java.util.IdentityHashMap<>();
        TopScoresDeviceTest.render(()->{try{
            time[0]=Dungeon.hero.cooldown();hp[0]=Dungeon.hero.HP;
            java.lang.reflect.Field width=Window.class.getDeclaredField("width"),height=Window.class.getDeclaredField("height");width.setAccessible(true);height.setAccessible(true);size[0]=width.getInt(window);size[1]=height.getInt(window);
            Point first=window.camera().cameraToScreen(0,0),last=window.camera().cameraToScreen(size[0],size[1]);
            assertTrue("Calculation window content fits the screen",first.x>=0&&first.y>=0&&last.x<=Game.width&&last.y<=Game.height);
            for(Button button:TopScoresDeviceTest.find(window,Button.class)){
                if(button instanceof RedButton&&((RedButton)button).text().equals("Show calculations")){expander.set((RedButton)button);at[0]=button.camera().cameraToScreen(button.left()+button.width()/2,button.top()+button.height()/2);}
                else controls.put(button,new float[]{button.left(),button.top(),button.width(),button.height()});
            }
            for(RenderedTextBlock text:TopScoresDeviceTest.find(window,RenderedTextBlock.class)){
                plainHeight[0]=Math.max(plainHeight[0],text.height());
                assertFalse("Information windows contain no missing-message placeholder",text.text().contains("!!!NO TEXT FOUND!!!"));
                assertFalse("No singular duration with a plural unit",java.util.regex.Pattern.compile("(?<![0-9.])1 turns(?![a-zA-Z])").matcher(text.text()).find());
            }
            assertNotNull("The actual information window offers calculations",expander.get());
        }catch(Exception e){throw new RuntimeException(e);}});
        if(screenshot.startsWith("qol-existing-status-clear-")||screenshot.startsWith("qol-existing-power-link-")||screenshot.startsWith("qol-reviewed-talent-clear-")||screenshot.startsWith("qol-twin-upgrades-existing-panel-")||screenshot.startsWith("qol-weapon-recharging-existing-panel-")||screenshot.startsWith("qol-reviewed-spell-clear-")||screenshot.startsWith("qol-reviewed-spell-magic-")||screenshot.startsWith("qol-elemental-strike-reviewed-")||screenshot.startsWith("qol-reviewed-ability-charge-")||screenshot.startsWith("qol-reviewed-ability-all-")){
            Thread.sleep(120);TopScoresDeviceTest.render(()->assertDescriptionViewport(window));
            TopScoresDeviceTest.screenshot(screenshot.replace(".png","-normal.png"));
        }
        if(screenshot.equals("qol-reviewed-talent-clear-EXCESS_CHARGE.png")){
            TopScoresDeviceTest.render(()->{try{
                ScrollPane scroll=TopScoresDeviceTest.find(window,ScrollPane.class).get(0);
                boolean found=false;
                for(RenderedTextBlock body:TopScoresDeviceTest.find(scroll.content(),RenderedTextBlock.class)){
                    String text=body.text();
                    if(text.contains("Temporary wand-level bonuses count.")){
                        found=true;
                        assertTrue("Normal rank-one description uses the effective imbued wand level",text.contains("0.67 shielding per effective level of its imbued wand"));
                        assertTrue("Normal rank-two description uses the actual factor",text.contains("1.34 shielding per effective level of its imbued wand"));
                        assertTrue("Normal rank-three description uses the actual factor",text.contains("2.01 shielding per effective level of its imbued wand"));
                        assertTrue("Normal description explains whole-point rounding",text.contains("rounded to the nearest whole shield point"));
                        assertTrue("Normal final sentence preserves the existing stronger Barrier",text.endsWith("A stronger existing Barrier is kept; the amounts do not add together."));
                        assertFalse("These assertions inspect the collapsed ordinary description",text.contains("Calculations and explanation"));
                    }
                }
                assertTrue("Actual talent window contains the corrected ordinary Excess Charge description",found);
                float bottom=Math.max(0,scroll.content().height()-scroll.height());
                scroll.scrollTo(0,bottom);
                assertEquals("Ordinary Excess Charge text can scroll fully to its final line",bottom,scroll.content().camera.scroll.y,.001f);
                for(RenderedTextBlock body:TopScoresDeviceTest.find(scroll.content(),RenderedTextBlock.class)){
                    assertTrue("Ordinary final line has padding below it",scroll.content().height()>=Math.ceil(body.height())+2);
                    assertTrue("Ordinary final line is reachable without clipping",body.bottom()-bottom<=scroll.content().camera.height-2);
                }
                assertEquals(time[0],Dungeon.hero.cooldown(),0);assertEquals(hp[0],Dungeon.hero.HP);
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(120);TopScoresDeviceTest.render(()->assertDescriptionViewport(window));
            TopScoresDeviceTest.screenshot(screenshot.replace(".png","-normal-bottom.png"));
            TopScoresDeviceTest.render(()->{try{TopScoresDeviceTest.find(window,ScrollPane.class).get(0).scrollTo(0,0);}catch(Exception e){throw new RuntimeException(e);}});
        }
        touch(at[0]);TopScoresDeviceTest.render(()->{try{
            assertEquals("Pointer opens advanced explanation","Hide calculations",expander.get().text());
            for(RenderedTextBlock text:TopScoresDeviceTest.find(window,RenderedTextBlock.class)){
                assertFalse("Information windows contain no missing-message placeholder",text.text().contains("!!!NO TEXT FOUND!!!"));
                assertFalse("No singular duration with a plural unit",java.util.regex.Pattern.compile("(?<![0-9.])1 turns(?![a-zA-Z])").matcher(text.text()).find());
                if(screenshot.contains("Shadows.png")||screenshot.contains("power-link-1.png")||screenshot.contains("power-link-2.png"))assertFalse("Special countdown must not claim ordinary expiry",text.text().contains("Duration remaining"));
            }
            boolean found=false;for(RenderedTextBlock text:TopScoresDeviceTest.find(window,RenderedTextBlock.class))if(text.text().contains(expected))found=true;assertTrue("Rendered calculation contains live explanation: "+expected,found);
            if(screenshot.startsWith("qol-reviewed-spell-clear-ShieldOfLight")){boolean timings=false;for(RenderedTextBlock text:TopScoresDeviceTest.find(window,RenderedTextBlock.class))timings|=text.text().contains("Lasts 5 turns for you, or 4 turns for the linked ally");assertTrue("Shield of Light shows actual displayed hero/linked-ally timers",timings);}
            for(RenderedTextBlock text:TopScoresDeviceTest.find(window,RenderedTextBlock.class)){
                if(screenshot.startsWith("qol-existing-status-clear-")||screenshot.startsWith("qol-existing-power-link-")||screenshot.startsWith("qol-warrior-shield-"))assertFalse("Reviewed status windows avoid technical duration units",text.text().contains("game-time units"));
                if(screenshot.startsWith("qol-twin-upgrades-existing-panel-")||screenshot.startsWith("qol-weapon-recharging-existing-panel-"))assertFalse("Reviewed talent windows avoid generic rank boilerplate",text.text().contains("rank-specific amounts"));
            }
            for(java.util.Map.Entry<Button,float[]> entry:controls.entrySet()){Button b=entry.getKey();float[] before=entry.getValue();assertEquals(before[0],b.left(),0);assertEquals(before[1],b.top(),0);assertEquals(before[2],b.width(),0);assertEquals(before[3],b.height(),0);}
            java.lang.reflect.Field width=Window.class.getDeclaredField("width"),height=Window.class.getDeclaredField("height");width.setAccessible(true);height.setAccessible(true);assertEquals(size[0],width.getInt(window));assertEquals(size[1],height.getInt(window));
            assertEquals(time[0],Dungeon.hero.cooldown(),0);assertEquals(hp[0],Dungeon.hero.HP);
            ScrollPane scroll=TopScoresDeviceTest.find(window,ScrollPane.class).get(0);scroll.scrollTo(0,Math.min(plainHeight[0],Math.max(0,scroll.content().height()-scroll.height())));
        }catch(Exception e){throw new RuntimeException(e);}});
        Thread.sleep(250);TopScoresDeviceTest.render(()->assertDescriptionViewport(window));
        Point originalOffset=window.getOffset();TopScoresDeviceTest.render(()->window.offset(originalOffset.x+1,originalOffset.y+1));
        Thread.sleep(100);TopScoresDeviceTest.render(()->{assertDescriptionViewport(window);window.offset(originalOffset);});
        Thread.sleep(100);TopScoresDeviceTest.render(()->assertDescriptionViewport(window));
        TopScoresDeviceTest.screenshot(screenshot);
        TopScoresDeviceTest.render(()->{try{ScrollPane scroll=TopScoresDeviceTest.find(window,ScrollPane.class).get(0);float bottom=Math.max(0,scroll.content().height()-scroll.height());scroll.scrollTo(0,bottom);assertEquals("Last description line remains reachable",bottom,scroll.content().camera.scroll.y,.001f);assertDescriptionViewport(window);}catch(Exception e){throw new RuntimeException(e);}});
        Thread.sleep(100);TopScoresDeviceTest.screenshot(screenshot.replace(".png","-bottom.png"));touch(at[0]);
        TopScoresDeviceTest.render(()->{assertEquals("Pointer closes advanced explanation","Show calculations",expander.get().text());assertEquals(time[0],Dungeon.hero.cooldown(),0);assertEquals(hp[0],Dungeon.hero.HP);window.hide();});
    }
    private static void inventoryOrderShowsAutomatically()throws Exception{
        final java.util.ArrayList<Item> original=new java.util.ArrayList<>();
        final java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag> bags=new java.util.ArrayList<>();
        final java.util.ArrayList<java.util.List<Item>> expected=new java.util.ArrayList<>();
        TopScoresDeviceTest.render(()->{
            com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag backpack=Dungeon.hero.belongings.backpack;original.addAll(backpack.items);backpack.items.clear();
            Item low=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword().level(8).identify();
            Item middle=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword().identify();
            Item plus=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear().level(3).identify();Item high=new Sword().identify();Item unknown=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword().level(-2);
            backpack.items.addAll(java.util.Arrays.asList(high,unknown,plus,middle,low));bags.add(backpack);expected.add(java.util.Arrays.asList(low,middle,plus,high,unknown));
            com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster holster=new com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster();
            Item knife=new ThrowingKnife().level(8).identify(),club=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingClub().identify(),clubPlus=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingClub().level(3).identify();
            Item wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost().identify(),wandPlus=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile().level(4).identify(),unknownWand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost().level(-3);
            holster.owner=Dungeon.hero;backpack.items.add(holster);holster.items.addAll(java.util.Arrays.asList(unknownWand,wandPlus,clubPlus,wand,club,knife));bags.add(holster);expected.add(java.util.Arrays.asList(knife,club,clubPlus,wand,wandPlus,unknownWand));
            com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier bandolier=new com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier();
            Item few=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing().identify().quantity(1),many=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding().identify().quantity(8);
            bandolier.owner=Dungeon.hero;backpack.items.add(bandolier);bandolier.items.addAll(java.util.Arrays.asList(many,few));bags.add(bandolier);expected.add(java.util.Arrays.asList(few,many));
            com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch pouch=new com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch();
            Item seedLarge=new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed().quantity(6);
            Item seedSmall=new com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom.Seed().quantity(1);
            Item stoneSmall=new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink().quantity(1);
            Item stoneLarge=new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition().quantity(8);
            pouch.owner=Dungeon.hero;backpack.items.add(pouch);
            pouch.items.addAll(java.util.Arrays.asList(stoneSmall,seedLarge,stoneLarge,seedSmall));
            bags.add(pouch);expected.add(java.util.Arrays.asList(seedSmall,seedLarge,stoneSmall,stoneLarge));
        });
        try{
            for(int i=0;i<bags.size()+1;i++){
                final int index=i<bags.size()?i:0;
                if(i==bags.size())TopScoresDeviceTest.render(()->{Item moved=expected.get(0).get(4);moved.identify();java.util.ArrayList<Item> newOrder=new java.util.ArrayList<>(expected.get(0));newOrder.remove(moved);newOrder.add(0,moved);expected.set(0,newOrder);});AtomicReference<WndBag> window=new AtomicReference<>();
                TopScoresDeviceTest.render(()->{window.set(new WndBag(bags.get(index)));GameScene.show(window.get());});Thread.sleep(200);
                TopScoresDeviceTest.render(()->{try{
                    java.lang.reflect.Field item=ItemSlot.class.getDeclaredField("item");item.setAccessible(true);
                    java.util.List<InventorySlot> slots=TopScoresDeviceTest.find(window.get(),InventorySlot.class);slots.sort(java.util.Comparator.comparingDouble((InventorySlot slot)->slot.top()).thenComparingDouble(slot->slot.left()));
                    java.util.ArrayList<Item> actual=new java.util.ArrayList<>();for(InventorySlot slot:slots){Item value=(Item)item.get(slot);if(expected.get(index).contains(value))actual.add(value);}
                    assertEquals("Actual inventory slots sort automatically without a button",expected.get(index),actual);
                }catch(Exception e){throw new AssertionError(e);}});
                TopScoresDeviceTest.screenshot("qol-automatic-inventory-"+i+".png");TopScoresDeviceTest.render(()->window.get().hide());
            }
        }finally{TopScoresDeviceTest.render(()->{Dungeon.hero.belongings.backpack.items.clear();Dungeon.hero.belongings.backpack.items.addAll(original);});}
    }
    private static void elementalStrikeLiveTalentDescriptionsRender()throws Exception{
        final com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon[] previous={null};
        final java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer> original=new java.util.LinkedHashMap<>();
        try{
            TopScoresDeviceTest.render(()->{previous[0]=Dungeon.hero.belongings.weapon;original.putAll(Dungeon.hero.talents.get(3));});
            for(int rank:new int[]{0,2,4})for(String name:new String[]{"None","Lucky","Elastic","Crystal"}){
                final AtomicReference<Window> shown=new AtomicReference<>();
                TopScoresDeviceTest.render(()->{try{
                    Dungeon.hero.talents.get(3).put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.ELEMENTAL_REACH,rank);Dungeon.hero.talents.get(3).put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.STRIKING_FORCE,rank);
                    Sword sword=new Sword();sword.level(8);sword.levelKnown=false;sword.cursedKnown=true;if(!name.equals("None"))sword.enchant((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments."+name).getDeclaredConstructor().newInstance());Dungeon.hero.belongings.weapon=sword;
                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike();assertTrue(ability.desc(),ability.desc().contains("_"+(4+rank)+" tiles_"));assertTrue(ability.desc(),ability.desc().contains("_"+(65+10*rank)+" degrees_"));Window window=new WndInfoArmorAbility(HeroClass.DUELIST,ability);GameScene.show(window);shown.set(window);
                }catch(Exception e){throw new RuntimeException(e);}});
                try{Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot("qol-elemental-strike-live-"+rank+"-"+name+".png");}finally{TopScoresDeviceTest.render(()->shown.get().hide());}
            }
        }finally{TopScoresDeviceTest.render(()->{Dungeon.hero.belongings.weapon=previous[0];Dungeon.hero.talents.get(3).clear();Dungeon.hero.talents.get(3).putAll(original);});}
    }
    private static void elementalStrikeUnknownCurseDescriptionRenders()throws Exception{
        final com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon[] previous={null};
        final Sword sword=new Sword();
        final com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike();
        final AtomicReference<Window> shown=new AtomicReference<>();
        final String[] generic={null},saved={null};
        try{
            TopScoresDeviceTest.render(()->{
                previous[0]=Dungeon.hero.belongings.weapon;Dungeon.hero.belongings.weapon=sword;
                sword.level(8);sword.levelKnown=sword.cursedKnown=false;generic[0]=ability.desc();
                sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Annoying());
                assertEquals("Unknown curse cannot change normal ability text",generic[0],ability.desc());
                com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();sword.storeInBundle(before);saved[0]=before.toString();
                Window window=new WndInfoArmorAbility(HeroClass.DUELIST,ability);GameScene.show(window);shown.set(window);
            });
            Thread.sleep(300);
            TopScoresDeviceTest.render(()->{try{
                for(RenderedTextBlock text:TopScoresDeviceTest.find(shown.get(),RenderedTextBlock.class)){
                    assertFalse("Normal rendered ability hides unknown curse",text.text().toLowerCase(java.util.Locale.ROOT).contains("annoying"));
                }
                assertDescriptionViewport(shown.get());
            }catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.screenshot("qol-elemental-strike-unknown-curse-normal.png");
            checkCalculationWindow(shown.get(),"Identify the weapon's unknown properties","qol-elemental-strike-unknown-curse-expanded.png");
            TopScoresDeviceTest.render(()->{
                com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();sword.storeInBundle(after);assertEquals(saved[0],after.toString());
                sword.cursedKnown=true;assertTrue(ability.desc(),ability.desc().toLowerCase(java.util.Locale.ROOT).contains("annoying"));
            });
        }finally{TopScoresDeviceTest.render(()->{if(shown.get()!=null)shown.get().hide();Dungeon.hero.belongings.weapon=previous[0];});}
    }
    private static void reviewedArmorAbilityWindows()throws Exception{
        final HeroClass[] oldClass=new HeroClass[1];
        final com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass[] oldSubclass=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass[1];
        final com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[] oldAbility=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[1];
        final com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon[] oldWeapon=new com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon[1];
        final boolean[] oldRatEnergy=new boolean[1];
        final java.util.ArrayList<java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer>> oldTalents=new java.util.ArrayList<>();
        TopScoresDeviceTest.render(()->{oldClass[0]=Dungeon.hero.heroClass;oldSubclass[0]=Dungeon.hero.subClass;oldAbility[0]=Dungeon.hero.armorAbility;oldWeapon[0]=Dungeon.hero.belongings.weapon;oldRatEnergy[0]=com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify.useRatroicEnergy;for(java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer> tier:Dungeon.hero.talents)oldTalents.add(new java.util.LinkedHashMap<>(tier));});
        java.util.ArrayList<HeroClass> classes=new java.util.ArrayList<>();
        java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility> abilities=new java.util.ArrayList<>();
        for(HeroClass type:HeroClass.values())for(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility ability:type.armorAbilities()){classes.add(type);abilities.add(ability);}
        classes.add(HeroClass.WARRIOR);abilities.add(new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify());assertEquals("All existing selectable armor abilities",19,abilities.size());
        try{for(int i=0;i<abilities.size();i++){
            final HeroClass type=classes.get(i);final com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility ability=abilities.get(i);final String[] expected=new String[1];
            TopScoresDeviceTest.render(()->{
                Dungeon.hero.heroClass=type;Dungeon.hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.NONE;Dungeon.hero.armorAbility=ability;
                for(int tier=0;tier<oldTalents.size();tier++){Dungeon.hero.talents.get(tier).clear();Dungeon.hero.talents.get(tier).putAll(oldTalents.get(tier));}
                for(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent talent:ability.talents()){for(java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer> tier:Dungeon.hero.talents)tier.remove(talent);Dungeon.hero.talents.get(0).put(talent,2);}
                if(ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity)for(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent talent:new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent[]{com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.BODY_FORM,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.MIND_FORM,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.SPIRIT_FORM}){for(java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer> tier:Dungeon.hero.talents)tier.remove(talent);Dungeon.hero.talents.get(0).put(talent,2);}
                com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon weapon=ability instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast?new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile()):new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();weapon.level(3);weapon.levelKnown=weapon.cursedKnown=true;Dungeon.hero.belongings.weapon=weapon;
                expected[0]=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.AbilityExplanation.effects(ability,Dungeon.hero).trim();assertFalse("Existing ability has its own effect explanation",expected[0].isEmpty());
            });
            calculationWindow(()->new WndInfoArmorAbility(type,ability),expected[0],"qol-reviewed-ability-all-"+ability.getClass().getSimpleName()+".png");
        }}finally{TopScoresDeviceTest.render(()->{Dungeon.hero.heroClass=oldClass[0];Dungeon.hero.subClass=oldSubclass[0];Dungeon.hero.armorAbility=oldAbility[0];Dungeon.hero.belongings.weapon=oldWeapon[0];com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify.useRatroicEnergy=oldRatEnergy[0];for(int tier=0;tier<oldTalents.size();tier++){Dungeon.hero.talents.get(tier).clear();Dungeon.hero.talents.get(tier).putAll(oldTalents.get(tier));}});}
    }
    private static void ordinaryTalentWindowRenders(Talent talent,int rank,HeroClass heroClass,
                                                   HeroSubClass subclass,String... expected)throws Exception{
        AtomicReference<WndInfoTalent> shown=new AtomicReference<>();
        HeroClass[] previousClass=new HeroClass[1];HeroSubClass[] previousSubclass=new HeroSubClass[1];
        java.util.LinkedHashMap<Talent,Talent> previousMetamorphs=new java.util.LinkedHashMap<>();
        float[] time=new float[1];int[] hp=new int[1];
        String screenshot="qol-ordinary-talent-"+talent.name();
        try{
            TopScoresDeviceTest.render(()->{
                previousClass[0]=Dungeon.hero.heroClass;previousSubclass[0]=Dungeon.hero.subClass;
                previousMetamorphs.putAll(Dungeon.hero.metamorphedTalents);
                time[0]=Dungeon.hero.cooldown();hp[0]=Dungeon.hero.HP;
                Dungeon.hero.heroClass=heroClass;Dungeon.hero.subClass=subclass;
                Dungeon.hero.metamorphedTalents.clear();
                WndInfoTalent window=new WndInfoTalent(talent,rank,null);
                GameScene.show(window);shown.set(window);
            });
            Thread.sleep(300);
            TopScoresDeviceTest.render(()->{try{
                assertDescriptionViewport(shown.get());
                ScrollPane scroll=TopScoresDeviceTest.find(shown.get(),ScrollPane.class).get(0);
                boolean found=false;
                for(RenderedTextBlock body:TopScoresDeviceTest.find(scroll.content(),RenderedTextBlock.class)){
                    String text=body.text();
                    if(text.equals(talent.desc(false))){
                        found=true;
                        String plain=text.replace("_","");
                        for(String fragment:expected)assertTrue(talent+" ordinary window contains "+fragment,plain.contains(fragment));
                        assertFalse("Capture stays in the ordinary talent description",text.contains("Calculations and explanation"));
                    }
                    assertFalse("Ordinary talent contains no missing-message placeholder",text.contains("!!!NO TEXT FOUND!!!"));
                }
                assertTrue("Actual ordinary talent text is rendered for "+talent,found);
                scroll.scrollTo(0,0);
                assertEquals(time[0],Dungeon.hero.cooldown(),0);assertEquals(hp[0],Dungeon.hero.HP);
            }catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.screenshot(screenshot+".png");
            TopScoresDeviceTest.render(()->{try{
                ScrollPane scroll=TopScoresDeviceTest.find(shown.get(),ScrollPane.class).get(0);
                float bottom=Math.max(0,scroll.content().height()-scroll.height());scroll.scrollTo(0,bottom);
                assertEquals("Ordinary talent can scroll to its final line",bottom,scroll.content().camera.scroll.y,.001f);
                for(RenderedTextBlock body:TopScoresDeviceTest.find(scroll.content(),RenderedTextBlock.class)){
                    assertTrue("Ordinary talent reserves final-line padding",scroll.content().height()>=Math.ceil(body.height())+2);
                    assertTrue("Ordinary talent final line remains reachable",body.bottom()-bottom<=scroll.content().camera.height-2);
                }
                assertEquals(time[0],Dungeon.hero.cooldown(),0);assertEquals(hp[0],Dungeon.hero.HP);
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(120);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));
            TopScoresDeviceTest.screenshot(screenshot+"-bottom.png");
        }finally{TopScoresDeviceTest.render(()->{
            if(shown.get()!=null)shown.get().hide();
            if(previousClass[0]!=null){
                Dungeon.hero.heroClass=previousClass[0];Dungeon.hero.subClass=previousSubclass[0];
                Dungeon.hero.metamorphedTalents.clear();Dungeon.hero.metamorphedTalents.putAll(previousMetamorphs);
            }
        });}
    }
    private static void ordinaryTalentAuditWindowsRender()throws Exception{
        ordinaryTalentWindowRenders(Talent.VETERANS_INTUITION,1,HeroClass.WARRIOR,HeroSubClass.NONE,
                "weapons 1.75x faster","armor 2x faster","when he equips it");
        ordinaryTalentWindowRenders(Talent.PROVOKED_ANGER,2,HeroClass.WARRIOR,HeroSubClass.NONE,
                "3 bonus damage","5 bonus damage","lasts 5 turns","adds 5 turns to its remaining duration");
        ordinaryTalentWindowRenders(Talent.LINGERING_MAGIC,2,HeroClass.MAGE,HeroSubClass.NONE,
                "1-2 bonus damage","2 bonus damage","lasts 5 turns","Another zap refreshes that duration");
        ordinaryTalentWindowRenders(Talent.EMPOWERED_STRIKE,3,HeroClass.MAGE,HeroSubClass.BATTLEMAGE,
                "one sixth more damage","1.5 times its normal power","one third more damage","2 times its normal power",
                "50% more damage","2.5 times its normal power","lasts 10 turns","another staff zap refreshes it");
        ordinaryTalentWindowRenders(Talent.ARCANE_VISION,2,HeroClass.MAGE,HeroSubClass.NONE,
                "10 turns","15 turns","Transfusion targets allies","wards hit surviving targets",
                "Frost does not trigger this","already frozen");
        ordinaryTalentWindowRenders(Talent.SPEEDY_STEALTH,3,HeroClass.ROGUE,HeroSubClass.FREERUNNER,
                "up to 10","freerunning is inactive and off cooldown","moves at 2x speed");
        ordinaryTalentWindowRenders(Talent.LIQUID_AGILITY,2,HeroClass.DUELIST,HeroSubClass.NONE,
                "3x evasion","infinite evasion","until her next turn","drink-only potions or elixirs");
    }
    private static void counterAbilityNormalWindowRenders()throws Exception{
        AtomicReference<WndInfoTalent> shown=new AtomicReference<>();float[] time=new float[1];int[] hp=new int[1];
        try{
            TopScoresDeviceTest.render(()->{
                time[0]=Dungeon.hero.cooldown();hp[0]=Dungeon.hero.HP;
                WndInfoTalent window=new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.COUNTER_ABILITY,2,null);
                GameScene.show(window);shown.set(window);
            });
            Thread.sleep(300);
            TopScoresDeviceTest.render(()->{try{
                assertDescriptionViewport(shown.get());
                ScrollPane scroll=TopScoresDeviceTest.find(shown.get(),ScrollPane.class).get(0);
                boolean found=false;
                for(RenderedTextBlock body:TopScoresDeviceTest.find(scroll.content(),RenderedTextBlock.class)){
                    String text=body.text();
                    if(text.contains("0.75 charges")){
                        found=true;
                        assertFalse("Counter Ability normal copy does not overstate the rank-two refund",text.contains("0.77"));
                        assertTrue("All four normal ranks remain present",text.contains("0.38 charges")&&text.contains("1.13 charges")&&text.contains("1.5 charges"));
                        assertFalse("Counter Ability capture remains the ordinary description",text.contains("Calculations and explanation"));
                    }
                    assertFalse("Normal Counter Ability contains no missing-message placeholder",text.contains("!!!NO TEXT FOUND!!!"));
                }
                assertTrue("Actual Counter Ability talent window shows the 0.75-charge rank-two refund",found);
                scroll.scrollTo(0,0);
                assertEquals(time[0],Dungeon.hero.cooldown(),0);assertEquals(hp[0],Dungeon.hero.HP);
            }catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.screenshot("qol-counter-ability-normal.png");
            TopScoresDeviceTest.render(()->{try{
                ScrollPane scroll=TopScoresDeviceTest.find(shown.get(),ScrollPane.class).get(0);
                float bottom=Math.max(0,scroll.content().height()-scroll.height());scroll.scrollTo(0,bottom);
                assertEquals("Ordinary Counter Ability can scroll to its final line",bottom,scroll.content().camera.scroll.y,.001f);
                for(RenderedTextBlock body:TopScoresDeviceTest.find(scroll.content(),RenderedTextBlock.class)){
                    assertTrue("Ordinary Counter Ability reserves final-line padding",scroll.content().height()>=Math.ceil(body.height())+2);
                    assertTrue("Ordinary Counter Ability final line remains reachable",body.bottom()-bottom<=scroll.content().camera.height-2);
                }
                assertEquals(time[0],Dungeon.hero.cooldown(),0);assertEquals(hp[0],Dungeon.hero.HP);
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(120);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));
            TopScoresDeviceTest.screenshot("qol-counter-ability-normal-bottom.png");
        }finally{TopScoresDeviceTest.render(()->{if(shown.get()!=null)shown.get().hide();});}
    }
    private static void calculationWindowsRenderAndRespond()throws Exception{
        counterAbilityNormalWindowRenders();
        ordinaryTalentAuditWindowsRender();
        String[] conciseBuffNames={"Haste","Slow","Speed","Chill","Weakness","Vulnerable","Recharging","ArtifactRecharge","Poison","Bleeding","Barrier","Combo","Paralysis","Foresight","AscensionChallenge","Bless","Hex","Daze","ChampionEnemy$Growing","ChampionEnemy$AntiMagic"};
        String[] conciseBuffExpected={"Walk three times as fast","Actions take twice as long","Actions take half their usual time","Current action speed:","Attacks deal 33% less","Attacks deal 33% more","Extra wand charging","Extra artifact charging","Next turn:","randomly weakens","Barrier gradually fades","Time left:","Escape check","Foresight-assisted searching","Amulet's curse","Strengthens attack and dodge rolls","Weakens attack and dodge rolls","Weakens attack and dodge rolls","1 percentage point every four turns","including Hex, Weakness, Charm and Magical Sleep"};
        for(int i=0;i<conciseBuffNames.length;i++){
            final String name=conciseBuffNames[i];
            calculationWindow(()->{
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff effect=(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff)Class.forName("com.shatteredpixel.shatteredpixeldungeon.actors.buffs."+name).getDeclaredConstructor().newInstance();
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff){com.watabou.utils.Bundle b=new com.watabou.utils.Bundle();effect.storeInBundle(b);b.put("time",b.getFloat("time")-effect.cooldown()+5);effect.restoreFromBundle(b);}
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge)effect).set(5);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison)effect).set(9);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding){com.watabou.utils.Bundle b=new com.watabou.utils.Bundle();effect.storeInBundle(b);b.put("level",8f);effect.restoreFromBundle(b);}
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier)effect).setShield(10);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo){com.watabou.utils.Bundle b=new com.watabou.utils.Bundle();effect.storeInBundle(b);b.put("count",8);b.put("combotime",10f);effect.restoreFromBundle(b);}
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge){com.watabou.utils.Bundle b=new com.watabou.utils.Bundle();effect.storeInBundle(b);b.put(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge.STACKS,8.5f);b.put(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge.DAMAGE,.5f);effect.restoreFromBundle(b);}
                effect.target=Dungeon.hero;
                return new WndInfoBuff(effect);
            },conciseBuffExpected[i],"qol-existing-status-clear-concise-"+name+".png");
        }
        calculationWindow(()->{
            com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking.BlockBuff effect=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking.BlockBuff();effect.target=Dungeon.hero;effect.setShield(10);return new WndInfoBuff(effect);
        },"Countdown:","qol-existing-status-clear-concise-Blocking.png");
        String[] statusNames={"Invisibility","Invulnerability","Cripple","Stamina","Blindness","Roots","PinCushion","Barkskin","ArcaneArmor","RevealedArea","GravityChaosTracker","SwarmIntelTracker","Drowsy","MagicalSleep","FireImbue","FrostImbue","Adrenaline","Fury","AdrenalineSurge","Healing","GreaterHaste","WellFed","PhysicalEmpower","WandEmpower","Amok","Charm","Terror","Dread","Vertigo","Light","LifeLink","MagicImmune","BlobImmunity","EnhancedRings","PrismaticGuard","Frost","Levitation","ToxicImbue","LockedFloor","Burning","Corrosion","Ooze","ScrollEmpower","MagicalSight","MindVision","Shadows"};
        String[] statusText={"last known position","ordinary damage","twice as long","two thirds","normal scroll and tome reading","Teleportation","quantity: 3","3 turns apart","3 turns apart","3 × 3","pulse count: 11","visible monsters","Incoming damage","1 HP per turn","50% chance","3 turns of Chill","hero does not gain","health is above 50%","Added strength: 2","Healing reserve: 100 HP","haste units: 6","multiple of 18","Adds 3 damage","Adds 4 damage","visible hero","shortens the effect by 5 turns","regardless of damage amount","Remaining fear updates","eight neighboring directions","base sight","combined raw shares can exceed","Suppressed equipment","Burning and Poison","Might's strength bonus","Stored health: 5","Freezing belongings","sacrificial fire","Gas releases left: 2","Recovery grace","Base damage per turn","Base damage grows each turn","flying prevents water","Wand casts left: 3","Sight through walls","within 2 tiles","Refresh countdown: 2"};
        for(int i=0;i<statusNames.length;i++){
            final String name=statusNames[i];
            calculationWindow(()->{
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff effect=(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff)Class.forName("com.shatteredpixel.shatteredpixeldungeon.actors.buffs."+name).getDeclaredConstructor().newInstance();
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion){ThrowingKnife stack=new ThrowingKnife();stack.quantity(3);((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion)effect).stick(stack);}
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin)effect).set(8,3);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArcaneArmor)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArcaneArmor)effect).set(8,3);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GravityChaosTracker){((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GravityChaosTracker)effect).left=11;((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GravityChaosTracker)effect).positiveOnly=true;}
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FireImbue)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FireImbue)effect).set(10);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AdrenalineSurge)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AdrenalineSurge)effect).reset(2,20);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing)effect).setHeal(100,.25f,2);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreaterHaste)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreaterHaste)effect).set(6);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WellFed)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WellFed)effect).reset();
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PhysicalEmpower)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PhysicalEmpower)effect).set(3,2);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WandEmpower)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WandEmpower)effect).set(4,3);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrismaticGuard){effect.target=Dungeon.hero;((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrismaticGuard)effect).set(5);}
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ToxicImbue)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ToxicImbue)effect).set(2);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning){com.watabou.utils.Bundle data=new com.watabou.utils.Bundle();effect.storeInBundle(data);data.put("left",8f);effect.restoreFromBundle(data);}
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion)effect).set(8,2);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze)effect).set(8);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ScrollEmpower)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ScrollEmpower)effect).reset(3);
                if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Shadows)((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Shadows)effect).prolong();
                return new WndInfoBuff(effect);
            },statusText[i],"qol-existing-status-clear-"+name+".png");
        }

        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows cloak=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows();cloak.levelKnown=cloak.cursedKnown=true;return new WndInfoItem(cloak);},"every 4 turns","qol-cloak-explanation-expanded.png");
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose rose=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose();rose.levelKnown=rose.cursedKnown=true;return new WndInfoItem(rose);},"the ghost’s health percentage","qol-rose-explanation-expanded.png");

        final com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent holding=com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HOLD_FAST;
        java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer> holdTier=Dungeon.hero.talents.get(0);
        for(java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer> tier:Dungeon.hero.talents)if(tier.containsKey(holding)){holdTier=tier;break;}
        final java.util.Map<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer> holdValues=holdTier;final Integer priorHold=holdValues.get(holding);
        try{
            TopScoresDeviceTest.render(()->holdValues.put(holding,2));
            for(boolean moved:new boolean[]{false,true})calculationWindow(()->{
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HoldFast hold=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HoldFast();hold.target=Dungeon.hero;hold.pos=Dungeon.hero.pos+(moved?1:0);return new WndInfoBuff(hold);
            },moved?"currently inactive":"currently active","qol-existing-status-clear-HoldFast-"+(moved?"moved":"holding")+".png");
            final com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass[] beforeHoldClass=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass[1];
            final com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor[] beforeHoldArmor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor[1];
            TopScoresDeviceTest.render(()->{beforeHoldClass[0]=Dungeon.hero.heroClass;beforeHoldArmor[0]=Dungeon.hero.belongings.armor;Dungeon.hero.heroClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.MAGE;});
            try{
                for(int example=0;example<5;example++){
                    final int capCase=example;
                    calculationWindow(()->{
                        com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor worn=capCase==1?null:capCase==0?new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor():capCase==4?new com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor():new com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor();
                        if(worn!=null){worn.level(capCase==3?9:capCase==4?-3:0);worn.levelKnown=worn.cursedKnown=capCase==0||capCase==4;}Dungeon.hero.belongings.armor=worn;
                        com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HoldFast hold=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HoldFast();hold.target=Dungeon.hero;hold.pos=Dungeon.hero.pos;
                        String normal=hold.desc();assertTrue(normal,normal.contains(capCase==0?"_2_":capCase==1?"_0_":capCase==4?"reduce protection by _2_":"Identify your armor"));return new WndInfoBuff(hold);
                    },"currently active","qol-existing-status-clear-HoldFast-cap-"+example+".png");
                }
            }finally{TopScoresDeviceTest.render(()->{Dungeon.hero.heroClass=beforeHoldClass[0];Dungeon.hero.belongings.armor=beforeHoldArmor[0];});}
        }finally{TopScoresDeviceTest.render(()->{if(priorHold==null)holdValues.remove(holding);else holdValues.put(holding,priorHold);});}

        for(int linked=0;linked<3;linked++){
            final int linkState=linked;
            final com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat ally=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();
            try{
                calculationWindow(()->{
                    ally.alignment=com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY;ally.pos=Dungeon.hero.pos+1;
                    if(linkState==1)com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(ally,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay.BeamingRayBoost.class,5);
                    if(linkState==2)com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(ally,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell.LifeLinkSpellBuff.class,5);
                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff power=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.PowerBuff();power.target=ally;
                    return new WndInfoBuff(power);
                },linked==0?"25% more damage":"keeps this effect active beyond its normal duration","qol-existing-power-link-"+linked+".png");
            }finally{TopScoresDeviceTest.render(()->{for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:new java.util.ArrayList<>(ally.buffs()))buff.detach();});}
        }

        calculationWindow(()->{
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker endured=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker();endured.target=Dungeon.hero;endured.enduring=false;endured.damageBonus=7;endured.hitsLeft=2;return new WndInfoBuff(endured);
        },"Hits remaining: _2_","qol-existing-status-clear-Endure-ready.png");

        calculationWindow(()->{
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker endured=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker();endured.target=Dungeon.hero;endured.enduring=false;endured.damageBonus=7;endured.hitsLeft=1;
            assertFalse(endured.desc().contains("1 hits"));return new WndInfoBuff(endured);
        },"Hits remaining: _1_","qol-existing-status-clear-Endure-ready-one-hit.png");
        for(int negative=0;negative<2;negative++){
            final int effect=negative;
            calculationWindow(()->{
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff=effect==0?new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom():new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption();buff.target=new SoulMarkWindowTarget();return new WndInfoBuff(buff);
            },negative==0?"67%":"1%","qol-existing-status-clear-"+(negative==0?"Doom":"Corruption")+".png");
        }

        calculationWindow(()->{
            com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight.HTBoost boost=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight.HTBoost();boost.target=Dungeon.hero;boost.reset();return new WndInfoBuff(boost);
        },"Level-ups remaining: _5 of 5_","qol-existing-status-clear-MightBoost.png");
        calculationWindow(()->{
            com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing healing=new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing();healing.target=Dungeon.hero;healing.set(148);return new WndInfoBuff(healing);
        },"Stored healing reserve: 148 HP","qol-existing-status-clear-AquaticHealing.png");

        final com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor[] beforeDegradedArmor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor[1];
        TopScoresDeviceTest.render(()->beforeDegradedArmor[0]=Dungeon.hero.belongings.armor);
        try{for(int known=0;known<2;known++){
            final boolean knownLevel=known==1;
            calculationWindow(()->{
                com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor armor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor();armor.level(9);armor.levelKnown=armor.cursedKnown=knownLevel;Dungeon.hero.belongings.armor=armor;
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade degraded=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade();degraded.target=Dungeon.hero;
                if(!knownLevel)assertFalse(com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.BuffExplanation.details(degraded).contains("+9 → +5"));return new WndInfoBuff(degraded);
            },knownLevel?"+9 → +5":"Weakened upgrades","qol-existing-status-clear-Degrade-"+(knownLevel?"known":"unknown")+".png");
        }}finally{TopScoresDeviceTest.render(()->Dungeon.hero.belongings.armor=beforeDegradedArmor[0]);}

        final com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator.Action[] previousMomentumAction=new com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator.Action[1];
        TopScoresDeviceTest.render(()->previousMomentumAction[0]=com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator.action);
        try{
        for(int state=0;state<3;state++){
            final int momentumState=state;
            calculationWindow(()->{
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Momentum momentum=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Momentum();momentum.target=Dungeon.hero;
                com.watabou.utils.Bundle data=new com.watabou.utils.Bundle();momentum.storeInBundle(data);data.put("stacks",momentumState==0?5:0);data.put("freerun_turns",momentumState==1?8:0);data.put("freerun_CD",momentumState==0?0:momentumState==1?20:12);momentum.restoreFromBundle(data);return new WndInfoBuff(momentum);
            },"Recovery turns left:","qol-existing-status-clear-Momentum-"+state+".png");
        }
        }finally{TopScoresDeviceTest.render(()->com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator.setAction(previousMomentumAction[0]));}

        for(int hungerLevel:new int[]{300,450})calculationWindow(()->{
            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger hunger=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger();hunger.target=Dungeon.hero;
            com.watabou.utils.Bundle data=new com.watabou.utils.Bundle();hunger.storeInBundle(data);data.put("level",(float)hungerLevel);data.put("partialDamage",.95f);hunger.restoreFromBundle(data);return new WndInfoBuff(hunger);
        },"starving at 450 points","qol-existing-status-clear-Hunger-"+hungerLevel+".png");

        calculationWindow(()->{
            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SoulMark mark=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SoulMark();mark.target=new SoulMarkWindowTarget();String normal=mark.desc();assertTrue(normal,normal.contains("40%"));assertFalse(normal,normal.contains("%s"));return new WndInfoBuff(mark);
        },"Soul Mark healing","qol-existing-status-clear-SoulMark.png");

        final com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor[] beforeRageArmor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor[1];
        TopScoresDeviceTest.render(()->beforeRageArmor[0]=Dungeon.hero.belongings.armor);
        try{
            for(int index=0;index<3;index++){
                final int hiddenOrKnownLevel=index==1?9:index==2?3:0;final boolean knownArmor=index==2;
                calculationWindow(()->{
                    com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor worn=new com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor();worn.level(hiddenOrKnownLevel);worn.levelKnown=worn.cursedKnown=knownArmor;Dungeon.hero.belongings.armor=worn;
                    com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk rage=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk();rage.target=Dungeon.hero;
                    if(!knownArmor)assertFalse("Unidentified armor shielding withheld",rage.desc().contains("Berserk shield at current armor"));return new WndInfoBuff(rage);
                },knownArmor?"Berserk shield at current armor":"Identify your armor","qol-existing-status-clear-Berserk-"+(knownArmor?"known":"unknown")+"-"+hiddenOrKnownLevel+".png");
            }
        }finally{TopScoresDeviceTest.render(()->Dungeon.hero.belongings.armor=beforeRageArmor[0]);}

        calculationWindow(()->new WndInfoBuff(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning.LightningCharge()),"up to 2 tiles","qol-existing-status-clear-LightningCharge.png");

        RingOfArcana arcana=new RingOfArcana();
        TopScoresDeviceTest.render(()->{arcana.level(3);arcana.identify();});
        showItem(arcana,"90.61%","qol-arcana-percentage.png");
        TopScoresDeviceTest.render(()->{
            for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:new java.util.ArrayList<>(Dungeon.hero.buffs(Ring.RingBuff.class)))buff.detach();
            Dungeon.hero.belongings.ring=arcana;arcana.activate(Dungeon.hero);
        });
        AtomicReference<WndInfoItem> luckyWindow=new AtomicReference<>();float[] luckyTime=new float[1];
        TopScoresDeviceTest.render(()->{
            Sword lucky=new Sword();lucky.levelKnown=lucky.cursedKnown=true;lucky.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky());
            assertTrue(lucky.info(),lucky.info().contains("_19.06%_ (_10%_ before your Arcana)"));assertEquals(1,lucky.info().split("19.06%",-1).length-1);
            luckyTime[0]=Dungeon.hero.cooldown();WndInfoItem window=new WndInfoItem(lucky);GameScene.show(window);luckyWindow.set(window);
        });
        Thread.sleep(300);
        TopScoresDeviceTest.render(()->{try{ScrollPane scroll=TopScoresDeviceTest.find(luckyWindow.get(),ScrollPane.class).get(0);scroll.scrollTo(0,Math.max(0,scroll.content().height()-scroll.height()));}catch(Exception e){throw new RuntimeException(e);}});
        Thread.sleep(200);TopScoresDeviceTest.screenshot("qol-lucky-arcana-increase.png");
        TopScoresDeviceTest.render(()->{assertEquals(luckyTime[0],Dungeon.hero.cooldown(),0);luckyWindow.get().hide();});
        Sword kinetic=new Sword();TopScoresDeviceTest.render(()->{kinetic.levelKnown=kinetic.cursedKnown=true;kinetic.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic());});showItem(kinetic,"_190.61%_","qol-kinetic-arcana-storage.png",true,true);
        com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor obfuscationArmor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor();obfuscationArmor.level(3);obfuscationArmor.levelKnown=obfuscationArmor.cursedKnown=true;obfuscationArmor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Obfuscation());showItem(obfuscationArmor,"sleeping enemy 3 tiles away","qol-obfuscation-plain-detection-example.png",true);

        String[] retainedNames={"Blocking","Shocking","Eldritch","Vorpal"},retainedText={"5-turn timer","including neutral creatures","hero witnesses","Replaces immediate hit damage"};
        for(int index=0;index<retainedNames.length;index++){
            final String name=retainedNames[index];
            calculationWindow(()->{Sword blade=new Sword();blade.level(3);blade.levelKnown=blade.cursedKnown=true;blade.enchant((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments."+name).getDeclaredConstructor().newInstance());return new WndInfoItem(blade);},retainedText[index],"qol-retained-"+name+"-clear-details.png");
        }
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier b=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier();b.setShield(17);return new WndInfoBuff(b);},"17 shield points","qol-mechanics-buff-expanded.png");
        String[] reviewedTalentNames={"BARKSKIN","NATURES_AID","PROJECTILE_MOMENTUM","FOLLOWUP_STRIKE","SHARED_UPGRADES","SHARED_ENCHANTMENT","BOUNTY_HUNTER","SHIELDING_DEW","SEARING_LIGHT","AGGRESSIVE_BARRIER","SWIFT_EQUIP","VARIED_CHARGE","EXCESS_CHARGE"};
        String[] reviewedTalentText={"Barkskin protection","Barkskin from a visible plant","Thrown weapons while freerunning","Follow-up hit","about 16.67% damage","Borrowing the bow's enchantment","normal item-drop chance","Shielding cap =","once per Illumination","50% health or less","20 turns","start another pair","round(effective wand level × 0.67 × rank)"};
        for(int index=0;index<reviewedTalentNames.length;index++){
            final String name=reviewedTalentNames[index];
            calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.valueOf(name),2,null),reviewedTalentText[index],"qol-reviewed-talent-clear-"+name+".png");
        }
        final HeroClass[] beforeMealClass=new HeroClass[1];
        final int[] beforeMealLevel=new int[1];
        final java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent> beforeMealMetamorphs=new java.util.LinkedHashMap<>();
        TopScoresDeviceTest.render(()->{beforeMealClass[0]=Dungeon.hero.heroClass;beforeMealLevel[0]=Dungeon.hero.lvl;beforeMealMetamorphs.putAll(Dungeon.hero.metamorphedTalents);});
        String[] mealNames={"ENERGIZING_MEAL","MYSTICAL_MEAL","INVIGORATING_MEAL","SATIATED_SPELLS","FOCUSED_MEAL","ENLIGHTENING_MEAL","EMPOWERING_MEAL","STRENGTHENING_MEAL","COMBINED_LETHALITY","SATIATED_SPELLS","FOCUSED_MEAL","ENLIGHTENING_MEAL"};
        HeroClass[] mealClasses={HeroClass.MAGE,HeroClass.ROGUE,HeroClass.HUNTRESS,HeroClass.CLERIC,HeroClass.DUELIST,HeroClass.CLERIC,HeroClass.MAGE,HeroClass.DUELIST,HeroClass.DUELIST,HeroClass.WARRIOR,HeroClass.WARRIOR,HeroClass.WARRIOR};
        String[] mealExpected={"1 extra charge every 4 turns","including whether it can recharge the Horn","Haste triples movement speed","next qualifying spell cast","_1 weapon-ability charge_","_1 Tome charge_","3 direct-wand damage rolls","total remaining bonus","Bosses and minibosses cannot be executed","delays decay for at least","_+1 damage_","both wand and artifact Recharging"};
        try{for(int index=0;index<mealNames.length;index++){
            final String name=mealNames[index];final HeroClass mealClass=mealClasses[index];final boolean alternate=index>=9;
            TopScoresDeviceTest.render(()->{Dungeon.hero.heroClass=mealClass;Dungeon.hero.lvl=1;Dungeon.hero.metamorphedTalents.clear();if(alternate)Dungeon.hero.metamorphedTalents.put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HEARTY_MEAL,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.valueOf(name));});
            calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.valueOf(name),2,null),mealExpected[index],"qol-reviewed-talent-clear-"+name+(alternate?"-alternate":"-native")+".png");
        }}finally{TopScoresDeviceTest.render(()->{Dungeon.hero.heroClass=beforeMealClass[0];Dungeon.hero.lvl=beforeMealLevel[0];Dungeon.hero.metamorphedTalents.clear();Dungeon.hero.metamorphedTalents.putAll(beforeMealMetamorphs);});}
        calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.CACHED_RATIONS,2,null),"no HP when healing is disabled","qol-reviewed-talent-clear-CACHED_RATIONS-native.png");
        calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.ROGUES_FORESIGHT,2,null),"two on a secrets-feeling floor","qol-reviewed-talent-clear-ROGUES_FORESIGHT-native.png");
        final HeroClass[] beforeRechargeClass=new HeroClass[1];TopScoresDeviceTest.render(()->{beforeRechargeClass[0]=Dungeon.hero.heroClass;Dungeon.hero.heroClass=HeroClass.DUELIST;});
        try{calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.WEAPON_RECHARGING,2,null),"1 extra weapon-ability charge every 10 turns","qol-reviewed-talent-clear-WEAPON_RECHARGING-duelist.png");}
        finally{TopScoresDeviceTest.render(()->Dungeon.hero.heroClass=beforeRechargeClass[0]);}
        final int[] beforeBarkskinLevel=new int[1];TopScoresDeviceTest.render(()->{beforeBarkskinLevel[0]=Dungeon.hero.lvl;Dungeon.hero.lvl=12;});
        try{calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.BARKSKIN,2,null),"0–8 damage protection","qol-reviewed-talent-clear-BARKSKIN-positive.png");}
        finally{TopScoresDeviceTest.render(()->Dungeon.hero.lvl=beforeBarkskinLevel[0]);}
        calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.ENHANCED_RINGS,1,null),"for 3 turns","qol-reviewed-talent-clear-ENHANCED_RINGS.png");
        calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.PROTECTIVE_SHADOWS,2,null),"up to 5 shield points","qol-reviewed-talent-clear-PROTECTIVE_SHADOWS.png");
        calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.SILENT_STEPS,2,null),"2 or more tiles away","qol-reviewed-talent-clear-SILENT_STEPS.png");
        calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.TWIN_UPGRADES,3,null),"weapons of the same tier can share","qol-twin-upgrades-existing-panel-clear.png");
        java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent> priorMetamorphs=new java.util.LinkedHashMap<>(Dungeon.hero.metamorphedTalents);
        try{
            TopScoresDeviceTest.render(()->Dungeon.hero.metamorphedTalents.put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HEARTY_MEAL,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.WEAPON_RECHARGING));
            calculationWindow(()->new WndInfoTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.WEAPON_RECHARGING,2,null),"1.025","qol-weapon-recharging-existing-panel-clear.png");
        }finally{TopScoresDeviceTest.render(()->{Dungeon.hero.metamorphedTalents.clear();Dungeon.hero.metamorphedTalents.putAll(priorMetamorphs);});}
        final com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon[] beforeStrikeDetails=new com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon[1];
        TopScoresDeviceTest.render(()->beforeStrikeDetails[0]=Dungeon.hero.belongings.weapon);
        try{for(int effect=0;effect<2;effect++){
            final boolean bleeding=effect==1;
            calculationWindow(()->{
                Sword sword=new Sword();sword.cursedKnown=true;sword.levelKnown=false;sword.enchantment=bleeding?new com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Sacrificial():new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Crystal();Dungeon.hero.belongings.weapon=sword;
                return new WndInfoArmorAbility(HeroClass.DUELIST,new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike());
            },bleeding?"Makes you and targets bleed":"full weapon durability","qol-elemental-strike-reviewed-"+(bleeding?"Sacrificial":"Crystal")+".png");
        }}finally{TopScoresDeviceTest.render(()->Dungeon.hero.belongings.weapon=beforeStrikeDetails[0]);}
        calculationWindow(()->new WndInfoArmorAbility(HeroClass.WARRIOR,new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify()),"This is the rat’s action time","qol-reviewed-ability-charge-Ratmogrify.png");
        calculationWindow(()->new WndInfoArmorAbility(HeroClass.WARRIOR,new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap()),"Current cost:","qol-reviewed-ability-charge-HeroicLeap.png");
        calculationWindow(()->new WndInfoArmorAbility(HeroClass.CLERIC,new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity()),"no single cost for Trinity","qol-reviewed-ability-charge-Trinity.png");
        reviewedArmorAbilityWindows();
        final HeroClass[] beforeSpellClass=new HeroClass[1];
        final com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[] beforeSpellAbility=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[1];
        final com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass[] beforeSpellSubclass=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass[1];
        final java.util.ArrayList<java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer>> beforeSpellTalents=new java.util.ArrayList<>();
        TopScoresDeviceTest.render(()->{beforeSpellClass[0]=Dungeon.hero.heroClass;beforeSpellAbility[0]=Dungeon.hero.armorAbility;beforeSpellSubclass[0]=Dungeon.hero.subClass;for(java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer> tier:Dungeon.hero.talents)beforeSpellTalents.add(new java.util.LinkedHashMap<>(tier));Dungeon.hero.heroClass=HeroClass.CLERIC;});
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell[] reviewedSpells={com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldOfLight.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Sunray.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BlessSpell.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Cleanse.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Radiance.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Flash.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineIntervention.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MindForm.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MnemonicPrayer.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineSense.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyLance.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Judgement.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE};
        String[] reviewedSpellNames={"GuidingLight","ShieldOfLight","Sunray","Bless","LayOnHands","Cleanse","Radiance","Flash","DivineIntervention","GuidingLight-Priest","HolyIntuition","RecallInscription","BodyForm","MindForm","SpiritForm","LifeLink","BeamingRay","Stasis","HallowedGround","WallOfLight","MnemonicPrayer","DivineSense","HolyLance","Smite","Judgement","AuraOfProtection","HolyWeapon","HolyWard","HolyWeapon-Paladin","HolyWard-Paladin"};String[] reviewedSpellExpected={"2–8 base magic damage","3–6 damage per hit","6–12 base magic damage","10 turns of Bless","20 shield points","20 shield points","first cast","up to _4 tiles_","200 shield points","another creature consumes Illumination with an attack","without removing the curse","300 turns","_27 turns_","_+4 version_","_+6 artifact_","_13 turns_","up to _8 tiles_","_90 turns_","can reduce Barrier","does not trigger spell-cast bonuses","Bleeding gains _4 base damage_","_12 tiles_","_45–83 base magic damage_","_300% of normal enchantment power_","_15–30 base magic damage_","_75% of normal glyph power_","_50 turns_","_50 turns_","_6 damage_","_3 base protection_"};
        try{for(int spellIndex=0;spellIndex<reviewedSpells.length;spellIndex++){
            final com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell spell=reviewedSpells[spellIndex];final boolean priestSpell=reviewedSpellNames[spellIndex].equals("GuidingLight-Priest");final boolean paladinSpell=spell instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite||reviewedSpellNames[spellIndex].endsWith("-Paladin");
            calculationWindow(()->{
                Dungeon.hero.armorAbility=spell instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm||spell instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MindForm||spell instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm?new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity():beforeSpellAbility[0];
                Dungeon.hero.subClass=priestSpell?com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.PRIEST:paladinSpell?com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.PALADIN:com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.NONE;
                for(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent talent:new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent[]{com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.SHIELD_OF_LIGHT,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.SUNRAY,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.BLESS,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.LAY_ON_HANDS,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.CLEANSE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.FLASH,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.DIVINE_INTERVENTION,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HOLY_INTUITION,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.RECALL_INSCRIPTION,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.BODY_FORM,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.MIND_FORM,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.SPIRIT_FORM,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.LIFE_LINK,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.BEAMING_RAY,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.STASIS,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HALLOWED_GROUND,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.WALL_OF_LIGHT,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.MNEMONIC_PRAYER,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.DIVINE_SENSE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HOLY_LANCE,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.JUDGEMENT,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.AURA_OF_PROTECTION}){for(java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer> tier:Dungeon.hero.talents)tier.remove(talent);Dungeon.hero.talents.get(0).put(talent,2);}
                return new WndTitledMessage(new com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon(spell),com.shatteredpixel.shatteredpixeldungeon.messages.Messages.titleCase(spell.name()),spell.desc(),()->com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.details(spell,Dungeon.hero),0);
            },reviewedSpellExpected[spellIndex],"qol-reviewed-spell-clear-"+reviewedSpellNames[spellIndex]+".png");
        }}finally{TopScoresDeviceTest.render(()->{Dungeon.hero.heroClass=beforeSpellClass[0];Dungeon.hero.armorAbility=beforeSpellAbility[0];Dungeon.hero.subClass=beforeSpellSubclass[0];for(int tier=0;tier<beforeSpellTalents.size();tier++){Dungeon.hero.talents.get(tier).clear();Dungeon.hero.talents.get(tier).putAll(beforeSpellTalents.get(tier));}});}
        final com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune[] beforeSpellMagic=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune[1];final int[] beforeSpellHealth=new int[2];final com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor[] beforeMagicArmor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor[1];
        TopScoresDeviceTest.render(()->{beforeSpellMagic[0]=Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune.class);beforeSpellHealth[0]=Dungeon.hero.HP;beforeSpellHealth[1]=Dungeon.hero.HT;beforeMagicArmor[0]=Dungeon.hero.belongings.armor;com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor worn=new com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor();worn.identify();Dungeon.hero.belongings.armor=worn;if(beforeSpellMagic[0]==null)com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune.class,50);});
        try{for(int immuneSpell=0;immuneSpell<2;immuneSpell++){
            final com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell spell=immuneSpell==0?com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE:com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE;
            calculationWindow(()->{
                String detail=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.details(spell,Dungeon.hero);assertFalse(detail,detail.contains("Identify "));
                return new WndTitledMessage(new com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon(spell),com.shatteredpixel.shatteredpixeldungeon.messages.Messages.titleCase(spell.name()),spell.desc(),()->com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.details(spell,Dungeon.hero),0);
            },"Magic immunity currently","qol-reviewed-spell-magic-"+(immuneSpell==0?"HolyWeapon":"HolyWard")+".png");
        }}finally{TopScoresDeviceTest.render(()->{if(beforeSpellMagic[0]==null)com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.detach(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune.class);Dungeon.hero.belongings.armor=beforeMagicArmor[0];Dungeon.hero.HP=beforeSpellHealth[0];Dungeon.hero.HT=beforeSpellHealth[1];});}
        AtomicReference<WndClericSpells> menu=new AtomicReference<>();Point[] spellAt=new Point[1];
        TopScoresDeviceTest.render(()->{try{
            WndClericSpells w=new WndClericSpells(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome(),Dungeon.hero,true);GameScene.show(w);menu.set(w);
            for(WndClericSpells.SpellButton b:TopScoresDeviceTest.find(w,WndClericSpells.SpellButton.class)){java.lang.reflect.Field f=b.getClass().getDeclaredField("spell");f.setAccessible(true);if(f.get(b)==com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE)spellAt[0]=b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2);}
            assertNotNull("Actual spell information button exists",spellAt[0]);
        }catch(Exception e){throw new RuntimeException(e);}});
        touch(spellAt[0]);AtomicReference<Window> spellWindow=new AtomicReference<>();
        TopScoresDeviceTest.render(()->{try{java.util.List<WndTitledMessage> windows=TopScoresDeviceTest.find(Game.scene(),WndTitledMessage.class);assertEquals("Actual spell button opens its information",1,windows.size());spellWindow.set(windows.get(0));StringBuilder normal=new StringBuilder();for(RenderedTextBlock text:TopScoresDeviceTest.find(windows.get(0),RenderedTextBlock.class))normal.append(text.text()).append('\n');assertTrue(normal.toString(),normal.toString().contains("Charge cost: 2"));assertEquals("One live charge cost in the ordinary spell window",1,normal.toString().split("Charge cost:",-1).length-1);assertFalse(normal.toString().contains("Current cost:"));}catch(Exception e){throw new RuntimeException(e);}});
        TopScoresDeviceTest.screenshot("qol-cleric-one-normal-charge-cost.png");
        checkCalculationWindow(spellWindow.get(),"scaled by enchantment power and rounded","qol-mechanics-spell-expanded.png");TopScoresDeviceTest.render(()->menu.get().hide());
    }
    private static void ordinaryEffectsMatchOwnerScreenshots()throws Exception{
        KindofMisc[] previous=new KindofMisc[2];
        com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip whip=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip();
        com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor armor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor();
        com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand fire=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast();
        com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand growth=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth();
        try{
            TopScoresDeviceTest.render(()->{
                previous[0]=Dungeon.hero.belongings.ring;previous[1]=Dungeon.hero.belongings.misc;
                for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:new java.util.ArrayList<>(Dungeon.hero.buffs(Ring.RingBuff.class)))buff.detach();
                RingOfArcana arcana=new RingOfArcana();arcana.identify();arcana.level(0);
                Dungeon.hero.belongings.ring=arcana;Dungeon.hero.belongings.misc=null;arcana.activate(Dungeon.hero);
                whip.level(3);whip.identify();whip.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic());assertTrue(whip.collect());
                armor.level(2);armor.identify();armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity());assertTrue(armor.collect());
                fire.curChargeKnown=false;growth.curChargeKnown=false;assertTrue(fire.collect());assertTrue(growth.collect());
                assertTrue(whip.info(),whip.info().contains("2 tiles"));assertFalse(whip.info().contains("Arcana changes"));
                assertTrue(armor.info(),armor.info().contains("10%"));assertFalse(armor.info().contains("defer power"));assertFalse(armor.info().contains("qualifying"));
            });
            showItem(whip,"58.75%","qol-elastic-concise.png",true);
            showItem(armor,"44.06%","qol-viscosity-concise.png",true);
            showItem(fire,"cone of fire","qol-fireblast-unknown-charges.png");
            showItem(growth,"plants in a cone","qol-regrowth-unknown-charges.png");
        }finally{
            TopScoresDeviceTest.render(()->{
                Dungeon.hero.belongings.backpack.items.remove(whip);Dungeon.hero.belongings.backpack.items.remove(armor);
                Dungeon.hero.belongings.backpack.items.remove(fire);Dungeon.hero.belongings.backpack.items.remove(growth);fire.stopCharging();growth.stopCharging();
                for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:new java.util.ArrayList<>(Dungeon.hero.buffs(Ring.RingBuff.class)))buff.detach();
                Dungeon.hero.belongings.ring=(Ring)previous[0];Dungeon.hero.belongings.misc=previous[1];
                if(previous[0]!=null)previous[0].activate(Dungeon.hero);if(previous[1] instanceof Ring)previous[1].activate(Dungeon.hero);
            });
        }
    }
    private static void ordinaryConsumableDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion haste=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste();
        com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion shielding=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding();
        Item noisemaker=new com.shatteredpixel.shatteredpixeldungeon.items.bombs.Noisemaker(),regrowthBomb=new com.shatteredpixel.shatteredpixeldungeon.items.bombs.RegrowthBomb();
        int previous=Dungeon.challenges;
        try{
            TopScoresDeviceTest.render(()->{haste.anonymize();shielding.anonymize();assertTrue(haste.collect());assertTrue(shielding.collect());assertTrue(noisemaker.collect());assertTrue(regrowthBomb.collect());Dungeon.challenges=0;});
            showItem(haste,"turns","qol-haste-concise.png");
            showItem(shielding,"shielding","qol-shielding-concise.png");
            showItem(noisemaker,"steps onto its tile","qol-noisemaker-trigger-description.png",true);
            showItem(regrowthBomb,"instead of exploding","qol-regrowth-bomb-description.png",true);
            TopScoresDeviceTest.render(()->Dungeon.challenges=Challenges.NO_HEALING);
            showItem(shielding,"poisons you instead","qol-shielding-challenge-warning.png");
            showItem(regrowthBomb,"poisons you instead","qol-regrowth-bomb-challenge-warning.png",true);
        }finally{
            TopScoresDeviceTest.render(()->{Dungeon.challenges=previous;Dungeon.hero.belongings.backpack.items.remove(haste);Dungeon.hero.belongings.backpack.items.remove(shielding);Dungeon.hero.belongings.backpack.items.remove(noisemaker);Dungeon.hero.belongings.backpack.items.remove(regrowthBomb);});
        }
    }
    private static void reviewedTrinketDamageDescriptionsRender()throws Exception{
        for(int level:new int[]{0,3})for(boolean known:new boolean[]{true,false}){
            com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ThirteenLeafClover clover=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ThirteenLeafClover();TopScoresDeviceTest.render(()->{clover.level(level);clover.levelKnown=known;clover.cursedKnown=true;});
            showItem(clover,"weapon and many magical damage rolls","qol-clover-damage-rolls-"+level+"-"+known+".png",true,known);
        }
        com.shatteredpixel.shatteredpixeldungeon.items.trinkets.RatSkull skull=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.RatSkull();TopScoresDeviceTest.render(()->{skull.level(3);skull.levelKnown=skull.cursedKnown=true;});
        showItem(skull,"bonus is halved","qol-rat-skull-half-bonus.png",true,true);
        calculationWindow(()->new WndInfoItem(skull),"only the increase above the ordinary chance is halved","qol-rat-skull-retained-half-bonus.png");
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft tuft=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft();tuft.level(3);tuft.levelKnown=tuft.cursedKnown=true;return new WndInfoItem(tuft);},"actual dodge chance also depends","qol-ferret-existing-detail-clear.png");
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ThirteenLeafClover clover=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ThirteenLeafClover();clover.level(3);clover.levelKnown=clover.cursedKnown=true;return new WndInfoItem(clover);},"other rolls are normal","qol-clover-existing-detail-clear.png");
        com.shatteredpixel.shatteredpixeldungeon.items.trinkets.MimicTooth tooth=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.MimicTooth();TopScoresDeviceTest.render(()->{tooth.level(3);tooth.levelKnown=tooth.cursedKnown=true;});showItem(tooth,"Ordinary floors","qol-mimic-tooth-ordinary-floors.png",true,true);
    }
    private static void retainedTrinketDescriptionsRender()throws Exception{
        Item[] items={new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.PetrifiedSeed(),new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.MossyClump(),new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals(),new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft(),new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt(),new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.CrackedSpyglass(),new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood(),new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SaltCube(),new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion()};
        String[] expected={"seeds or runestones","Excludes Floor 1","cannot be spent as alchemy energy","Increases evasion","sense enemies through walls","Search can uncover it","maximum health","slows natural healing","once for each type"};
        for(int i=0;i<items.length;i++)for(boolean use:new boolean[]{false,true}){
            Item item=items[i];String required=expected[i];AtomicReference<WndInfoItem> shown=new AtomicReference<>();boolean[] temporarilyOwned={false};
            TopScoresDeviceTest.render(()->{item.level(3);item.levelKnown=item.cursedKnown=true;if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft){assertTrue(item.info().contains("_50%_"));assertTrue(item.info().contains("your allies _and your enemies_"));}assertTrue(item.info(),item.info().contains(required));if(use&&!Dungeon.hero.belongings.contains(item)){Dungeon.hero.belongings.backpack.items.add(item);temporarilyOwned[0]=true;}
                WndInfoItem window=use?new WndUseItem(null,item):new WndInfoItem(item);GameScene.show(window);shown.set(window);});Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));
            if(use)TopScoresDeviceTest.screenshot("qol-retained-trinket-"+i+"-normal-description.png");
            TopScoresDeviceTest.render(()->{shown.get().hide();if(temporarilyOwned[0])Dungeon.hero.belongings.backpack.items.remove(item);});
        }
    }
    private static void retainedArtifactDescriptionsRender()throws Exception{
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains();item.level(5);item.levelKnown=item.cursedKnown=true;return new WndInfoItem(item);},"twice the natural recharge limit","qol-chains-distinct-charge-limits.png");
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows();item.levelKnown=item.cursedKnown=item.cursed=true;return new WndInfoItem(item);},"recharge normally","qol-cloak-cursed-existing-detail.png");
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome();item.levelKnown=item.cursedKnown=item.cursed=true;return new WndInfoItem(item);},"recharge normally","qol-tome-cursed-existing-detail.png");

        com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature sandals=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature();TopScoresDeviceTest.render(()->{sandals.level(1);sandals.levelKnown=sandals.cursedKnown=true;sandals.seeds.add(com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Seed.class);sandals.curSeedEffect=com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom.Seed.class;});showItem(sandals,"1 of 6 different seed types","qol-sandals-current-ability-growth.png",true,true);
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook();item.level(3);item.levelKnown=item.cursedKnown=true;return new WndInfoItem(item);},"spend 1 more charge","qol-spellbook-retained-clear.png");
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass();item.levelKnown=item.cursedKnown=true;return new WndInfoItem(item);},"Each charge pays for 2 turns","qol-hourglass-retained-clear.png");
        for(int level:new int[]{5,10})calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit kit=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit();kit.level(level);kit.levelKnown=kit.cursedKnown=true;return new WndInfoItem(kit);},level==10?"Maximum upgrade reached":"25 turns at this level","qol-toolkit-retained-clear-"+level+".png");
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband item=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband();item.level(10);item.levelKnown=item.cursedKnown=true;return new WndInfoItem(item);},"Charges are spent only on success","qol-armband-retained-clear.png");
        com.shatteredpixel.shatteredpixeldungeon.items.KindofMisc previous=Dungeon.hero.belongings.misc;
        Item[] items={new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LloydsBeacon(),new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns()};
        com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature legacySandals=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature();
        TopScoresDeviceTest.render(()->{legacySandals.levelKnown=legacySandals.cursedKnown=true;legacySandals.curSeedEffect=com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush.Seed.class;com.watabou.utils.Bundle oldSave=new com.watabou.utils.Bundle();legacySandals.storeInBundle(oldSave);legacySandals.restoreFromBundle(oldSave);Dungeon.hero.belongings.misc=legacySandals;assertTrue(legacySandals.actions(Dungeon.hero).contains("FEED"));assertFalse(legacySandals.actions(Dungeon.hero).contains("ROOT"));assertFalse(legacySandals.info().contains("Charge needed"));});
        try{showItem(legacySandals,"Living sandals","qol-sandals-legacy-unsupported-safe.png",true,true);}finally{TopScoresDeviceTest.render(()->Dungeon.hero.belongings.misc=previous);}
        String[] expected={"makes you invulnerable","random scroll effect","one chance at loot","Trampling tall grass","spiritual power","can pass through walls","normal ration","1 HP every","Scrying searches","Portable alchemy","3 tile wide","Activation takes","Cleric spells","Set a return point","When active"};
        String[] curseExpected={"10% chance","except Remove Curse and Anti-Magic","20% chance","stops normal seed","hostile wraith","slow your movement","33% less filling","50% longer","Manual Search still works","alchemy pots","5 out of 6","Cannot activate","Cannot cast spells","teleport actions still work","deflection effects still work"};
        try{for(int i=0;i<items.length;i++)for(boolean cursed:new boolean[]{false,true})for(boolean use:new boolean[]{false,true}){
            Item item=items[i];String required=cursed?curseExpected[i]:expected[i];AtomicReference<WndInfoItem> shown=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{Dungeon.hero.belongings.misc=(com.shatteredpixel.shatteredpixeldungeon.items.KindofMisc)item;item.levelKnown=item.cursedKnown=true;item.cursed=cursed;assertTrue(item.info(),item.info().contains(required));assertFalse(item.info().contains("base action time"));WndInfoItem window=use?new WndUseItem(null,item):new WndInfoItem(item);GameScene.show(window);shown.set(window);});Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));
            if(use)TopScoresDeviceTest.screenshot("qol-retained-artifact-"+i+(cursed?"-cursed":"-normal")+"-description.png");
            TopScoresDeviceTest.render(()->shown.get().hide());
        }}finally{TopScoresDeviceTest.render(()->Dungeon.hero.belongings.misc=previous);}
    }
    private static void magicMissileDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();
        TopScoresDeviceTest.render(()->{wand.level(3);wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;});showItem(wand,"next zap","qol-magic-missile-concise.png",true);
        TopScoresDeviceTest.render(()->wand.levelKnown=false);showItem(wand,"weaker wand","qol-magic-missile-unknown.png",true);
        AtomicReference<Window> shown=new AtomicReference<>();
        TopScoresDeviceTest.render(()->{
            com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile.MagicCharge buff=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile.MagicCharge();com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();buff.storeInBundle(saved);saved.put("level",5);saved.put("time",com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now()+4);buff.restoreFromBundle(saved);
            assertTrue(buff.desc(),buff.desc().contains("+5"));assertTrue(buff.desc(),buff.desc().contains("one zap"));Window window=new WndInfoBuff(buff);GameScene.show(window);shown.set(window);
        });Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot("qol-magic-charge-one-zap.png");TopScoresDeviceTest.render(()->shown.get().hide());
    }
    private static void corrosionWandDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion();
        TopScoresDeviceTest.render(()->{wand.level(3);wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;});showItem(wand,"base damage per turn","qol-corrosion-wand-concise.png",true);
        TopScoresDeviceTest.render(()->wand.levelKnown=false);showItem(wand,"can harm you and allies","qol-corrosion-wand-unknown.png",true);
        HeroClass previousClass=Dungeon.hero.heroClass;com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass previousSubclass=Dungeon.hero.subClass;
        com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff();
        try{
            TopScoresDeviceTest.render(()->{try{Dungeon.hero.heroClass=HeroClass.MAGE;Dungeon.hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.BATTLEMAGE;wand.levelKnown=true;wand.charge(Dungeon.hero);staff.level(3);staff.levelKnown=staff.cursedKnown=true;java.lang.reflect.Field field=staff.getClass().getDeclaredField("wand");field.setAccessible(true);field.set(staff,wand);}catch(Exception e){throw new RuntimeException(e);}});
            showItem(staff,"Standing in water washes it off","qol-corrosion-battlemage-concise.png",true);
        }finally{TopScoresDeviceTest.render(()->{wand.stopCharging();Dungeon.hero.heroClass=previousClass;Dungeon.hero.subClass=previousSubclass;});}
    }
    private static void disintegrationWandDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration();
        TopScoresDeviceTest.render(()->{wand.level(3);wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;});showItem(wand,"Can hit allies too","qol-disintegration-wand-concise.png",true);
        TopScoresDeviceTest.render(()->wand.levelKnown=false);showItem(wand,"Unupgraded example (+0)","qol-disintegration-wand-unknown.png",true);
        HeroClass previousClass=Dungeon.hero.heroClass;com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass previousSubclass=Dungeon.hero.subClass;
        com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff();
        try{
            TopScoresDeviceTest.render(()->{try{Dungeon.hero.heroClass=HeroClass.MAGE;Dungeon.hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.BATTLEMAGE;wand.levelKnown=true;wand.charge(Dungeon.hero);staff.level(3);staff.levelKnown=staff.cursedKnown=true;java.lang.reflect.Field field=staff.getClass().getDeclaredField("wand");field.setAccessible(true);field.set(staff,wand);}catch(Exception e){throw new RuntimeException(e);}});
            showItem(staff,"strike enemies farther away","qol-disintegration-battlemage-concise.png",true);
        }finally{TopScoresDeviceTest.render(()->{wand.stopCharging();Dungeon.hero.heroClass=previousClass;Dungeon.hero.subClass=previousSubclass;});}
    }
    private static void lightningAndRegrowthDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning lightning=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning();
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth regrowth=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth();
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast fireblast=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast();
        TopScoresDeviceTest.render(()->{for(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand:new com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand[]{lightning,regrowth,fireblast}){wand.level(3);wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.curCharges=1;}});
        showItem(lightning,"allies are safe unless directly targeted","qol-lightning-concise.png",true);
        TopScoresDeviceTest.render(()->lightning.levelKnown=false);showItem(lightning,"Unupgraded example (+0)","qol-lightning-unknown.png",true);
        showItem(fireblast,"Charge cost: _1_","qol-fireblast-one-charge.png",true);
        showItem(regrowth,"Charge cost: _1_","qol-regrowth-one-charge.png",true,true);
        TopScoresDeviceTest.render(()->{regrowth.level(5);regrowth.curCharges=7;});showItem(regrowth,"Charge cost: _3_","qol-regrowth-three-charges.png",true,true);
        HeroClass previousClass=Dungeon.hero.heroClass;com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass previousSubclass=Dungeon.hero.subClass;
        com.watabou.utils.Bundle previousRules=new com.watabou.utils.Bundle();
        TopScoresDeviceTest.render(()->Dungeon.apdStoreCustomRules(previousRules));
        try{
            TopScoresDeviceTest.render(()->{Dungeon.hero.heroClass=HeroClass.MAGE;Dungeon.hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.BATTLEMAGE;lightning.levelKnown=true;});
            TopScoresDeviceTest.render(()->{regrowth.level(3);regrowth.curCharges=regrowth.maxCharges;});
            for(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand:new com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand[]{lightning,regrowth}){
                com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff();
                TopScoresDeviceTest.render(()->{try{wand.charge(Dungeon.hero);staff.level(3);staff.levelKnown=staff.cursedKnown=true;java.lang.reflect.Field field=staff.getClass().getDeclaredField("wand");field.setAccessible(true);field.set(staff,wand);}catch(Exception e){throw new RuntimeException(e);}});
                try{
                    showItem(staff,wand==lightning?"adds 1 tile":"Stay still","qol-"+(wand==lightning?"lightning":"regrowth")+"-battlemage.png",true);
                    if(wand==regrowth){
                        TopScoresDeviceTest.render(()->{com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),settings=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(settings);all.put("apd_custom_rules",settings);Dungeon.apdRestoreCustomRules(all);});
                        showItem(staff,"rules prevent the Battlemage healing","qol-regrowth-battlemage-healing-zero.png",true);
                    }
                }finally{TopScoresDeviceTest.render(()->wand.stopCharging());}
            }
            AtomicReference<Window> shown=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning.LightningCharge buff=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning.LightningCharge();Window window=new WndInfoBuff(buff);GameScene.show(window);shown.set(window);});
            Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot("qol-lightning-charge-concise.png");TopScoresDeviceTest.render(()->shown.get().hide());
        }finally{TopScoresDeviceTest.render(()->{Dungeon.apdRestoreCustomRules(previousRules);Dungeon.hero.heroClass=previousClass;Dungeon.hero.subClass=previousSubclass;});}
    }
    private static void livingEarthAndPrismaticDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth earth=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth();
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight light=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight();
        TopScoresDeviceTest.render(()->{earth.level(3);light.level(3);for(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand:new com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand[]{earth,light})wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;});
        showItem(earth,"restore its health instead","qol-living-earth-concise.png",true);
        showItem(light,"hidden doors and traps along the beam","qol-prismatic-concise.png",true);
        TopScoresDeviceTest.render(()->{earth.levelKnown=false;light.levelKnown=false;});
        showItem(earth,"Unupgraded example (+0)","qol-living-earth-unknown.png",true);showItem(light,"Unupgraded example (+0)","qol-prismatic-unknown.png",true);
        AtomicReference<Window> shown=new AtomicReference<>();
        TopScoresDeviceTest.render(()->{com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.RockArmor buff=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.RockArmor();com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();buff.storeInBundle(state);state.put("armor",11);state.put("wand_level",3);buff.restoreFromBundle(state);assertTrue(buff.desc(),buff.desc().contains("_11_"));assertTrue(buff.desc(),buff.desc().contains("_20_"));Window window=new WndInfoBuff(buff);GameScene.show(window);shown.set(window);});
        Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot("qol-rock-armor-reserve-concise.png");TopScoresDeviceTest.render(()->shown.get().hide());
        HeroClass priorClass=Dungeon.hero.heroClass;com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass priorSubclass=Dungeon.hero.subClass;
        try{
            TopScoresDeviceTest.render(()->{Dungeon.hero.heroClass=HeroClass.MAGE;Dungeon.hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.BATTLEMAGE;earth.levelKnown=light.levelKnown=true;});
            for(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand:new com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand[]{earth,light}){
                com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff();
                TopScoresDeviceTest.render(()->{try{wand.charge(Dungeon.hero);staff.level(3);staff.levelKnown=staff.cursedKnown=true;java.lang.reflect.Field field=staff.getClass().getDeclaredField("wand");field.setAccessible(true);field.set(staff,wand);}catch(Exception e){throw new RuntimeException(e);}});
                try{showItem(staff,wand==earth?"restore your guardian's health":"slow enemy movement","qol-"+(wand==earth?"earth":"prismatic")+"-battlemage.png",true);}finally{TopScoresDeviceTest.render(()->wand.stopCharging());}
            }
        }finally{TopScoresDeviceTest.render(()->{Dungeon.hero.heroClass=priorClass;Dungeon.hero.subClass=priorSubclass;});}
        for(int kind=0;kind<3;kind++){
            final int type=kind;AtomicReference<Window> buffWindow=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff;com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();
                if(type==0){buff=new com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Armor();buff.storeInBundle(state);state.put("level",11);state.put("pos",Dungeon.hero.pos);buff.restoreFromBundle(state);}
                else if(type==1){buff=new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health();buff.storeInBundle(state);state.put("level",11);state.put("pos",Dungeon.hero.pos);buff.restoreFromBundle(state);}
                else{com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle.TimeBubble time=new com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle.TimeBubble();time.reset();time.processTime(1.25f);buff=time;}
                Window window=new WndInfoBuff(buff);GameScene.show(window);buffWindow.set(window);
            });Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(buffWindow.get()));TopScoresDeviceTest.screenshot("qol-herbal-time-buff-"+type+".png");TopScoresDeviceTest.render(()->buffWindow.get().hide());
        }
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff(),"Imbue a wand to let this staff cast spells.","qol-empty-staff-needs-wand.png",true);
        com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor sealedArmor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor();
        TopScoresDeviceTest.render(()->{sealedArmor.levelKnown=sealedArmor.cursedKnown=true;sealedArmor.affixSeal(new com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal());});
        showItem(sealedArmor,"half health or lower","qol-armor-attached-seal-ready-threshold.png",true);
        HeroSubClass plantSubclass=Dungeon.hero.subClass;
        try{for(HeroSubClass subclass:new HeroSubClass[]{HeroSubClass.NONE,HeroSubClass.WARDEN}){
            TopScoresDeviceTest.render(()->Dungeon.hero.subClass=subclass);
            for(String species:new String[]{"Earthroot","Sungrass","Swiftthistle","Blindweed","Firebloom","Icecap","Starflower","Fadeleaf","Mageroyal","Sorrowmoss","Stormvine","Rotberry","BlandfruitBush","Dewcatcher","Seedpod"}){
                String type=species.equals("Dewcatcher")||species.equals("Seedpod")?"com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth$"+species:"com.shatteredpixel.shatteredpixeldungeon.plants."+species;
                com.shatteredpixel.shatteredpixeldungeon.plants.Plant plant=(com.shatteredpixel.shatteredpixeldungeon.plants.Plant)Class.forName(type).getDeclaredConstructor().newInstance();AtomicReference<Window> plantWindow=new AtomicReference<>();float[] time={0};String[] state={null};com.shatteredpixel.shatteredpixeldungeon.plants.Plant[] priorPlant={null};
                TopScoresDeviceTest.render(()->{plant.pos=Dungeon.hero.pos;priorPlant[0]=Dungeon.level.plants.get(plant.pos);Dungeon.level.plants.put(plant.pos,plant);time[0]=Dungeon.hero.cooldown();com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();plant.storeInBundle(before);state[0]=before.toString();assertFalse(plant.desc().isEmpty());Window window=new com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoPlant(plant);GameScene.show(window);plantWindow.set(window);});
                try{Thread.sleep(300);TopScoresDeviceTest.render(()->{try{assertEquals("Ordinary plants have no calculation button",0,TopScoresDeviceTest.find(plantWindow.get(),RedButton.class).size());for(RenderedTextBlock text:TopScoresDeviceTest.find(plantWindow.get(),RenderedTextBlock.class)){Point first=text.camera().cameraToScreen(text.left(),text.top()),last=text.camera().cameraToScreen(text.right(),text.bottom());assertTrue("Plant text fits "+species,first.x>=0&&first.y>=0&&last.x<=Game.width&&last.y<=Game.height);}assertEquals(time[0],Dungeon.hero.cooldown(),0);com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();plant.storeInBundle(after);assertEquals(state[0],after.toString());}catch(Exception e){throw new RuntimeException(e);}});TopScoresDeviceTest.screenshot("qol-plant-ordinary-"+species+"-"+subclass.name()+".png");}finally{TopScoresDeviceTest.render(()->{plantWindow.get().hide();if(priorPlant[0]==null)Dungeon.level.plants.remove(plant.pos);else Dungeon.level.plants.put(plant.pos,priorPlant[0]);});}
            }
        }}finally{TopScoresDeviceTest.render(()->Dungeon.hero.subClass=plantSubclass);}
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.EarthGuardian guardian=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.EarthGuardian();
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth.Lotus lotus=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth.Lotus();
        TopScoresDeviceTest.render(()->{guardian.setInfo(Dungeon.hero,3,25);com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();lotus.storeInBundle(state);state.put("wand_lvl",5);lotus.restoreFromBundle(state);lotus.HP=lotus.HT=40;});
        for(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob:new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob[]{guardian,lotus}){
            AtomicReference<Window> mobWindow=new AtomicReference<>();float[] time=new float[1];String[] before=new String[1];
            TopScoresDeviceTest.render(()->{com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(mob);time[0]=Dungeon.hero.cooldown();com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();mob.storeInBundle(state);before[0]=state.toString();assertTrue(mob.description(),mob.description().contains(mob==guardian?"_3-12 damage_":"_60% chance_"));Window window=new com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoMob(mob);GameScene.show(window);mobWindow.set(window);});
            Thread.sleep(300);
            TopScoresDeviceTest.render(()->{try{Window window=mobWindow.get();for(RenderedTextBlock text:TopScoresDeviceTest.find(window,RenderedTextBlock.class)){Point a=text.camera().cameraToScreen(text.left(),text.top()),b=text.camera().cameraToScreen(text.right(),text.bottom());assertTrue("NPC text fits screen",a.x>=0&&a.y>=0&&b.x<=Game.width&&b.y<=Game.height);}assertEquals(time[0],Dungeon.hero.cooldown(),0);com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();mob.storeInBundle(after);assertEquals(before[0],after.toString());}catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.screenshot(mob==guardian?"qol-earth-guardian-concise.png":"qol-golden-lotus-concise.png");
            TopScoresDeviceTest.render(()->{mobWindow.get().hide();com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(mob);});
        }
    }
    private static void passiveGlyphArcanaDescriptionsRender()throws Exception{
        KindofMisc[] previous=new KindofMisc[2];
        try{
            TopScoresDeviceTest.render(()->{previous[0]=Dungeon.hero.belongings.ring;previous[1]=Dungeon.hero.belongings.misc;for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:new java.util.ArrayList<>(Dungeon.hero.buffs(Ring.RingBuff.class)))buff.detach();RingOfArcana ring=new RingOfArcana();ring.level(3);ring.identify();Dungeon.hero.belongings.ring=ring;Dungeon.hero.belongings.misc=null;ring.activate(Dungeon.hero);});
            for(String name:new String[]{"Flow","Swiftness","Camouflage","AntiMagic"}){
                com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor armor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor();
                TopScoresDeviceTest.render(()->{try{armor.level(3);armor.levelKnown=armor.cursedKnown=true;armor.inscribe((com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs."+name).getDeclaredConstructor().newInstance());}catch(Exception e){throw new RuntimeException(e);}});
                showItem(armor,"before your Arcana","qol-passive-glyph-"+name+"-arcana.png",true);
            }
        }finally{TopScoresDeviceTest.render(()->{for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:new java.util.ArrayList<>(Dungeon.hero.buffs(Ring.RingBuff.class)))buff.detach();Dungeon.hero.belongings.ring=(Ring)previous[0];Dungeon.hero.belongings.misc=previous[1];if(previous[0]!=null)previous[0].activate(Dungeon.hero);if(previous[1] instanceof Ring)previous[1].activate(Dungeon.hero);});}
    }
    private static void hiddenTwinUpgradeDescriptionsRender()throws Exception{
        HeroClass previousClass=Dungeon.hero.heroClass;com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass previousSubclass=Dungeon.hero.subClass;
        final com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon[] previous=new com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon[2];final java.util.HashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer> talents=new java.util.HashMap<>();
        Sword sword=new Sword();com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow bow=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow();com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword source=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword();
        try{
            TopScoresDeviceTest.render(()->{previous[0]=Dungeon.hero.belongings.weapon;previous[1]=Dungeon.hero.belongings.secondWep;talents.putAll(Dungeon.hero.talents.get(2));Dungeon.hero.heroClass=HeroClass.DUELIST;Dungeon.hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.CHAMPION;Dungeon.hero.talents.get(2).put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.TWIN_UPGRADES,3);sword.level(1);sword.levelKnown=sword.cursedKnown=true;sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic());bow.level(1);bow.levelKnown=bow.cursedKnown=true;source.levelKnown=source.cursedKnown=false;Dungeon.hero.belongings.weapon=sword;Dungeon.hero.belongings.secondWep=source;source.level(0);String before=sword.info();source.level(8);assertEquals(before,sword.info());assertEquals(8,sword.buffedLvl());});
            showItem(sword,"Any Twin Upgrades bonus from your unidentified weapon is not shown","qol-known-sword-hidden-twin-upgrade.png",true);
            com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart();
            TopScoresDeviceTest.render(()->{Dungeon.hero.belongings.weapon=bow;source.level(0);String before=dart.info();source.level(8);assertEquals(before,dart.info());assertEquals(8,bow.buffedLvl());});
            showItem(dart,"Any Twin Upgrades bonus from your unidentified weapon is not shown","qol-known-crossbow-dart-hidden-twin-upgrade.png",true);
            TopScoresDeviceTest.render(()->{source.levelKnown=source.cursedKnown=true;Dungeon.hero.belongings.weapon=sword;assertFalse(sword.info().contains("bonus from your unidentified weapon"));});
            showItem(sword,"cleave","qol-known-sword-identified-twin-upgrade.png",true);
        }finally{TopScoresDeviceTest.render(()->{Dungeon.hero.belongings.weapon=previous[0];Dungeon.hero.belongings.secondWep=previous[1];Dungeon.hero.talents.get(2).clear();Dungeon.hero.talents.get(2).putAll(talents);Dungeon.hero.heroClass=previousClass;Dungeon.hero.subClass=previousSubclass;});}
    }
    private static void gradualHealingNonStackDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing potion=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing();potion.anonymize();showItem(potion,"Repeated doses do not add","qol-healing-potion-no-stacking.png",true);
        AtomicReference<Window> shown=new AtomicReference<>();TopScoresDeviceTest.render(()->{com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing healing=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing();healing.setHeal(100,.25f,0);assertTrue(healing.desc().contains("Next heal: up to 25 HP"));com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();healing.storeInBundle(before);Window window=new WndInfoBuff(healing);GameScene.show(window);shown.set(window);com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();healing.storeInBundle(after);assertEquals(before.toString(),after.toString());});
        try{Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot("qol-gradual-healing-concise-status.png");}finally{TopScoresDeviceTest.render(()->shown.get().hide());}
    }
    private static void liveElementalBlastDescriptionsRender()throws Exception{
        final com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon weapon=Dungeon.hero.belongings.weapon;
        final java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer> talents=new java.util.LinkedHashMap<>(Dungeon.hero.talents.get(3));
        try{for(String name:new String[]{"Lightning","Corrosion","Warding","Transfusion"})for(int rank:new int[]{0,4}){
            final int value=rank;final com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOf"+name).getDeclaredConstructor().newInstance();
            AtomicReference<Window> shown=new AtomicReference<>();TopScoresDeviceTest.render(()->{Dungeon.hero.talents.get(3).put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.ELEMENTAL_POWER,value);Dungeon.hero.talents.get(3).put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.BLAST_RADIUS,value);Dungeon.hero.talents.get(3).put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HEROIC_ENERGY,value);Dungeon.hero.belongings.weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff(wand);com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast ability=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast();String desc=ability.desc();assertTrue(desc,desc.contains("_"+(4+value)+" tiles_"));assertFalse(desc.contains("Charge cost:"));Window window=new WndInfoArmorAbility(HeroClass.MAGE,ability);GameScene.show(window);shown.set(window);});
            try{Thread.sleep(250);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot("qol-live-blast-"+name+"-rank"+rank+".png");}finally{TopScoresDeviceTest.render(()->shown.get().hide());}
        }}finally{TopScoresDeviceTest.render(()->{Dungeon.hero.belongings.weapon=weapon;Dungeon.hero.talents.get(3).clear();Dungeon.hero.talents.get(3).putAll(talents);});}
    }
    private static void conciseHolyDescriptionsRender() throws Exception {
        final com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass oldSubclass = Dungeon.hero.subClass;
        final com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor oldArmor = Dungeon.hero.belongings.armor;
        final com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff[] buffs = {null, null};
        try {
            TopScoresDeviceTest.render(() -> {
                Dungeon.hero.belongings.armor = new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor();
                buffs[0] = com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(Dungeon.hero, com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.HolyWepBuff.class, 50);
                buffs[1] = com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(Dungeon.hero, com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.HolyArmBuff.class, 50);
            });
            for (boolean paladin : new boolean[]{false, true}) {
                TopScoresDeviceTest.render(() -> Dungeon.hero.subClass = paladin ? com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.PALADIN : com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.NONE);
                for (int index = 0; index < 2; index++) {
                    final int which = index;
                    final AtomicReference<Window> shown = new AtomicReference<>();
                    TopScoresDeviceTest.render(() -> {
                        com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell spell = which == 0 ? com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE : com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE;
                        assertTrue(spell.desc(), spell.desc().contains("50 turns"));
                        assertEquals(1, spell.desc().split("Charge cost:", -1).length - 1);
                        Window w = new WndTitledMessage(new com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon(spell), com.shatteredpixel.shatteredpixeldungeon.messages.Messages.titleCase(spell.name()), spell.desc(), () -> com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.SpellExplanation.details(spell, Dungeon.hero), 0);
                        GameScene.show(w); shown.set(w);
                    });
                    try { Thread.sleep(250); TopScoresDeviceTest.render(() -> assertDescriptionViewport(shown.get())); TopScoresDeviceTest.screenshot("qol-holy-" + index + "-paladin" + paladin + "-spell.png"); }
                    finally { TopScoresDeviceTest.render(() -> shown.get().hide()); }
                    TopScoresDeviceTest.render(() -> { assertTrue(buffs[which].desc().contains("Turns remaining:")); Window w = new WndInfoBuff(buffs[which]); GameScene.show(w); shown.set(w); });
                    try { Thread.sleep(250); TopScoresDeviceTest.render(() -> assertDescriptionViewport(shown.get())); TopScoresDeviceTest.screenshot("qol-holy-" + index + "-paladin" + paladin + "-buff.png"); }
                    finally { TopScoresDeviceTest.render(() -> shown.get().hide()); }
                }
            }
        } finally {
            TopScoresDeviceTest.render(() -> { for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff b : buffs) if (b != null) b.detach(); Dungeon.hero.subClass = oldSubclass; Dungeon.hero.belongings.armor = oldArmor; });
        }
    }
    private static void conciseRetainedRingDetailsRender() throws Exception {
        final int hp = Dungeon.hero.HP;
        try {
            TopScoresDeviceTest.render(() -> Dungeon.hero.HP = Math.max(1, Dungeon.hero.HT / 2));
            calculationWindow(() -> { com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce ring = new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce(); ring.anonymize(); ring.level(3); ring.levelKnown = ring.cursedKnown = true; return new WndInfoItem(ring); }, "extra damage", "qol-force-retained-clear.png");
            for (int state = 0; state < 3; state++) {
                final boolean cursed = state > 0;
                final int level = state == 1 ? 0 : 3;
                String expected = state == 0 ? "Damage reduction grows" : state == 1 ? "Extra damage taken grows" : "currently has no effect on damage";
                calculationWindow(() -> { com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity ring = new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity(); ring.anonymize(); ring.level(level); ring.levelKnown = ring.cursedKnown = true; ring.cursed = cursed; return new WndInfoItem(ring); }, expected, "qol-tenacity-positive-negative-zero-" + state + ".png");
            }
        } finally { TopScoresDeviceTest.render(() -> Dungeon.hero.HP = hp); }
    }
    private static void conciseDefensiveBuffDescriptionsRender() throws Exception {
        for (String name : new String[]{"Barkskin", "ArcaneArmor", "Bless", "Hex", "Daze"}) {
            AtomicReference<Window> shown = new AtomicReference<>();
            TopScoresDeviceTest.render(() -> {
                try {
                    com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff = (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff) Class.forName("com.shatteredpixel.shatteredpixeldungeon.actors.buffs." + name).getDeclaredConstructor().newInstance();
                    if (buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin) ((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin) buff).set(7, 5);
                    if (buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArcaneArmor) ((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArcaneArmor) buff).set(9, 5);
                    assertTrue(buff.desc(), buff.desc().length() < 230);
                    Window window = new WndInfoBuff(buff); GameScene.show(window); shown.set(window);
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            try { Thread.sleep(250); TopScoresDeviceTest.render(() -> assertDescriptionViewport(shown.get())); TopScoresDeviceTest.screenshot("qol-plain-defensive-buff-" + name + ".png"); }
            finally { TopScoresDeviceTest.render(() -> shown.get().hide()); }
        }
    }
    private static void conciseRetainedTalismanDetailsRender() throws Exception {
        for (int level : new int[]{3, 10}) {
            calculationWindow(() -> {
                com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight item = new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight();
                item.level(level); item.levelKnown = item.cursedKnown = true;
                java.lang.reflect.Field charge = com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge");
                charge.setAccessible(true); charge.setInt(item, level == 10 ? 100 : 50);
                return new WndInfoItem(item);
            }, level == 10 ? "Maximum upgrade +10 reached" : "Long-cast range", "qol-talisman-retained-clear-" + level + ".png");
        }
    }
    private static void retainedMonkEnergyDetailsRender()throws Exception{
        final com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass heroClass=Dungeon.hero.heroClass;final com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass subclass=Dungeon.hero.subClass;
        final java.util.ArrayList<java.util.LinkedHashMap<com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent,Integer>> talents=Dungeon.hero.talents;
        final com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy[] energy={null};
        TopScoresDeviceTest.render(()->{Dungeon.hero.heroClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.DUELIST;Dungeon.hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.MONK;Dungeon.hero.talents=new java.util.ArrayList<>();com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.initClassTalents(Dungeon.hero);Dungeon.hero.talents.get(2).put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.MONASTIC_VIGOR,2);energy[0]=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy.class);energy[0].energy=9;});
        try{calculationWindow(()->new WndInfoBuff(energy[0]),"Only empowered meditation","qol-monk-existing-detail-clear.png");}finally{TopScoresDeviceTest.render(()->{energy[0].detach();Dungeon.hero.heroClass=heroClass;Dungeon.hero.subClass=subclass;Dungeon.hero.talents=talents;});}
    }
    private static void retainedThreeArtifactDetailsRender()throws Exception{
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns cape=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns();cape.level(3);cape.levelKnown=cape.cursedKnown=true;return new WndInfoItem(cape);},"Charging and deflection","qol-cape-existing-detail-clear.png");
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood chalice=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood();chalice.level(3);chalice.levelKnown=chalice.cursedKnown=true;return new WndInfoItem(chalice);},"this can kill you","qol-chalice-existing-detail-clear.png");
        calculationWindow(()->{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty horn=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty();horn.level(3);horn.levelKnown=horn.cursedKnown=true;return new WndInfoItem(horn);},"Experience recharges the Horn","qol-horn-existing-detail-clear.png");
    }
    private static void additionalTrinketDetailsRender()throws Exception{
        String[] names={"ParchmentScrap","PetrifiedSeed","DimensionalSundial","CrackedSpyglass","MossyClump","TrapMechanism","ChaoticCenser","ExoticCrystals","WondrousResin","ShardOfOblivion"};
        String[] expected={"curses checked first","Dew drops are unchanged","phone, enemies respawn","Search uncovers these items","would otherwise have no special theme","spread across the floor","Some clouds are helpful","even with these crystals","does not remove a wand","Your current Shard bonus"};
        for(int i=0;i<names.length;i++){
            final com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket item=(com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.trinkets."+names[i]).getDeclaredConstructor().newInstance();item.level(3);item.levelKnown=item.cursedKnown=true;
            calculationWindow(()->new WndInfoItem(item),expected[i],"qol-existing-trinket-detail-"+names[i]+".png");
        }
        com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ChaoticCenser unknown=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ChaoticCenser();unknown.level(3);unknown.levelKnown=false;unknown.cursedKnown=true;showItem(unknown,"At +0:","qol-censer-unidentified-example.png",true);
    }
    private static void retainedGrowthAndWardDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding ward=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding();ward.level(3);ward.levelKnown=ward.cursedKnown=ward.curChargeKnown=true;
        calculationWindow(()->new WndInfoItem(ward),"supports 5 points of wards","qol-warding-existing-detail-clear.png");
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth growth=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth();growth.level(3);growth.levelKnown=growth.cursedKnown=growth.curChargeKnown=true;
        calculationWindow(()->new WndInfoItem(growth),"Growth and exhaustion","qol-regrowth-existing-detail-clear.png");
        growth.level(10);calculationWindow(()->new WndInfoItem(growth),"exhaustion is effectively disabled","qol-regrowth-level10-existing-detail.png");
    }
    private static void sightHungerHealingTrinketDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt eye=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt();eye.level(3);eye.levelKnown=eye.cursedKnown=true;showItem(eye,"Stealthy mimics evade this sensing","qol-eye-of-newt-known-sensing-limit.png",true,true);
        calculationWindow(()->new WndInfoItem(eye),"sight factor = 0.875","qol-eye-of-newt-existing-detail-clear.png");
        com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SaltCube salt=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SaltCube();salt.level(3);salt.levelKnown=salt.cursedKnown=true;showItem(salt,"penalty pauses on locked floors","qol-salt-cube-known-locked-limit.png",true,true);
        calculationWindow(()->new WndInfoItem(salt),"penalty pauses on locked floors","qol-salt-cube-existing-detail-clear.png");
        com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood vial=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood();vial.level(3);vial.levelKnown=vial.cursedKnown=true;
        calculationWindow(()->new WndInfoItem(vial),"base limit is","qol-vial-existing-detail-clear.png");
        final com.watabou.utils.Bundle rules=new com.watabou.utils.Bundle();TopScoresDeviceTest.render(()->Dungeon.apdStoreCustomRules(rules));
        try{TopScoresDeviceTest.render(()->{com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),values=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(values);all.put("apd_custom_rules",values);Dungeon.apdRestoreCustomRules(all);});calculationWindow(()->new WndInfoItem(vial),"Your current rules prevent health restoration","qol-vial-existing-detail-healing-zero.png");}finally{TopScoresDeviceTest.render(()->Dungeon.apdRestoreCustomRules(rules));}

    }
    private static void retainedBlastScrollDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution retribution=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution();retribution.anonymize();showItem(retribution,"including allies","qol-retribution-normal-retained.png",true,true);
        calculationWindow(()->new WndInfoItem(retribution),"At your current health","qol-retribution-existing-detail-clear.png");
        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast blast=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast();blast.anonymize();showItem(blast,"Can kill you","qol-psionic-normal-retained.png",true,true);
        calculationWindow(()->new WndInfoItem(blast),"Additional affected creatures can reduce it further","qol-psionic-existing-detail-known-bound.png");
    }
    private static void conciseRechargeDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMysticalEnergy mystical=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMysticalEnergy();mystical.anonymize();showItem(mystical,"30 turns","qol-mystical-energy-duration.png",true);
        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging scroll=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging();scroll.anonymize();showItem(scroll,"30 turns","qol-recharging-known-duration.png",true);
        for(int index=0;index<2;index++){
            final int kind=index;AtomicReference<Window> shown=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff;if(kind==0){com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge recharge=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge();recharge.set(1.5f);buff=recharge;assertTrue(buff.desc().contains("Recharge remaining: 1.5 turns"));}else{buff=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging();com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();buff.storeInBundle(state);state.put("time",com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now()+.5f);buff.restoreFromBundle(state);assertTrue(buff.desc().contains("1 extra charge every 4 turns"));}
                com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();buff.storeInBundle(before);Window window=new WndInfoBuff(buff);GameScene.show(window);shown.set(window);com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();buff.storeInBundle(after);assertEquals(before.toString(),after.toString());
            });
            try{Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot("qol-recharge-fractional-status-"+kind+".png");}finally{TopScoresDeviceTest.render(()->shown.get().hide());}
        }
    }
    private static void conciseScrollStatusDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge scroll=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge();scroll.anonymize();showItem(scroll,"100 turns","qol-challenge-known-duration.png",true);
        for(int index=0;index<4;index++){
            final int kind=index;AtomicReference<Window> shown=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff;com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();
                if(kind==0){buff=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Foresight();buff.storeInBundle(state);state.put("time",com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now()+37);buff.restoreFromBundle(state);assertTrue(buff.desc().contains("8 tiles"));}
                else if(kind==1){buff=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread();buff.storeInBundle(state);state.put("left",13);buff.restoreFromBundle(state);assertTrue(buff.desc().contains("half experience"));}
                else if(kind==2){buff=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge.ChallengeArena();buff.storeInBundle(state);state.put("left",17);buff.restoreFromBundle(state);assertTrue(buff.desc().contains("Leaving ends"));}
                else{buff=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune();buff.storeInBundle(state);state.put("time",com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now()+23);buff.restoreFromBundle(state);assertTrue(buff.desc().contains("Disables your rings"));}
                String before=state.toString();Window window=new WndInfoBuff(buff);GameScene.show(window);shown.set(window);com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();buff.storeInBundle(after);assertEquals(before,after.toString());
            });
            try{Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot("qol-scroll-status-concise-"+kind+".png");}finally{TopScoresDeviceTest.render(()->shown.get().hide());}
        }
    }
    private static void remainingBattlemageDescriptionsRender()throws Exception{
        HeroClass previousClass=Dungeon.hero.heroClass;com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass previousSubclass=Dungeon.hero.subClass;
        com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand[] wands={new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding()};
        String[] expected={"attack nearby creatures","burning creatures and tiles","charmed by you","Basic wards cannot be repaired"};
        try{
            for(int index=0;index<wands.length;index++){
                com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=wands[index];com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff();
                TopScoresDeviceTest.render(()->{try{Dungeon.hero.heroClass=HeroClass.MAGE;Dungeon.hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.BATTLEMAGE;staff.level(3);staff.levelKnown=staff.cursedKnown=true;wand.level(3);wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.charge(Dungeon.hero);java.lang.reflect.Field field=staff.getClass().getDeclaredField("wand");field.setAccessible(true);field.set(staff,wand);}catch(Exception e){throw new RuntimeException(e);}});
                try{showItem(staff,expected[index],"qol-battlemage-normal-"+wand.getClass().getSimpleName()+".png",true);}finally{TopScoresDeviceTest.render(wand::stopCharging);}
            }
        }finally{TopScoresDeviceTest.render(()->{Dungeon.hero.heroClass=previousClass;Dungeon.hero.subClass=previousSubclass;});}
    }
    private static void cleansingStaminaMasteryDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing cleansing=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing();cleansing.anonymize();showItem(cleansing,"most harmful effects","qol-cleansing-known.png",true);
        com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStamina stamina=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStamina();stamina.anonymize();showItem(stamina,"Does not speed up attacks","qol-stamina-known.png",true);
        com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMastery mastery=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMastery();mastery.anonymize();showItem(mastery,"only once","qol-mastery-known.png",true);
        for(int index=0;index<2;index++){
            final int kind=index;AtomicReference<com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> effect=new AtomicReference<>();AtomicReference<Window> shown=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff;if(kind==0){assertNull(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing.Cleanse.class));buff=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing.Cleanse.class,5);assertTrue(buff.desc().contains("most harmful"));}else{assertNull(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stamina.class));buff=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stamina.class,100);assertTrue(buff.desc().contains("50% faster"));}effect.set(buff);Window window=new WndInfoBuff(buff);GameScene.show(window);shown.set(window);});
            try{Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot(kind==0?"qol-cleansed-most-effects.png":"qol-stamina-buff-concise.png");}finally{TopScoresDeviceTest.render(()->{shown.get().hide();effect.get().detach();});}
        }
    }
    private static void divineInspirationChoiceDescriptionRenders()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfEarthenArmor earthen=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfEarthenArmor();earthen.anonymize();showItem(earthen,"Stronger Barkskin takes precedence","qol-earthen-armor-known.png",true);
        com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight sight=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight();sight.anonymize();showItem(sight,"see through walls","qol-magical-sight-known.png",true);
        com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration potion=new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration();potion.anonymize();showItem(potion,"2 extra talent points","qol-divine-inspiration-concise.png",true);
        AtomicReference<Window> shown=new AtomicReference<>();AtomicReference<com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> tracker=new AtomicReference<>();java.lang.reflect.Field[] fields=new java.lang.reflect.Field[3];Object[] previous=new Object[3];long[] epoch={0};float[] time={0};
        try{TopScoresDeviceTest.render(()->{try{epoch[0]=Dungeon.hero.apdWaitCancellationEpoch;time[0]=Dungeon.hero.cooldown();assertNull(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration.DivineInspirationTracker.class));fields[0]=Item.class.getDeclaredField("curUser");fields[1]=Item.class.getDeclaredField("curItem");fields[2]=com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration.class.getDeclaredField("identifiedByUse");for(int i=0;i<fields.length;i++){fields[i].setAccessible(true);previous[i]=fields[i].get(null);}com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration.DivineInspirationTracker effect=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration.DivineInspirationTracker.class);effect.setBoosted(2);tracker.set(effect);Dungeon.hero.belongings.backpack.items.add(potion);potion.execute(Dungeon.hero,"DRINK");java.util.List<com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions> options=TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class);assertEquals(1,options.size());shown.set(options.get(0));}catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(300);TopScoresDeviceTest.render(()->{try{java.util.List<RedButton> buttons=TopScoresDeviceTest.find(shown.get(),RedButton.class);assertEquals(4,buttons.size());for(int i=0;i<4;i++)assertEquals(i!=1,buttons.get(i).active);boolean found=false;for(RenderedTextBlock text:TopScoresDeviceTest.find(shown.get(),RenderedTextBlock.class)){if(text.text().contains("unlocked"))found=true;Point a=text.camera().cameraToScreen(text.left(),text.top()),z=text.camera().cameraToScreen(text.right(),text.bottom());assertTrue(a.x>=0&&a.y>=0&&z.x<=Game.width&&z.y<=Game.height);}assertTrue(found);assertEquals(time[0],Dungeon.hero.cooldown(),0);assertEquals(1,potion.quantity());assertTrue(Dungeon.hero.belongings.backpack.items.contains(potion));}catch(Exception e){throw new RuntimeException(e);}});TopScoresDeviceTest.screenshot("qol-divine-inspiration-tier-choice.png");
        }finally{TopScoresDeviceTest.render(()->{try{if(shown.get()!=null)shown.get().onBackPressed();if(tracker.get()!=null)tracker.get().detach();Dungeon.hero.belongings.backpack.items.remove(potion);Dungeon.hero.apdWaitCancellationEpoch=epoch[0];for(int i=0;i<fields.length;i++)if(fields[i]!=null)fields[i].set(null,previous[i]);}catch(Exception e){throw new RuntimeException(e);}});}
    }
    private static void aquaticHealingBuffDescriptionRenders(String expected,String image)throws Exception{
        AtomicReference<com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing> effect=new AtomicReference<>();AtomicReference<Window> shown=new AtomicReference<>();
        TopScoresDeviceTest.render(()->{assertNull(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing.class));com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing buff=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing.class);buff.set(30);effect.set(buff);assertTrue(buff.desc(),buff.desc().contains(expected));Window window=new WndInfoBuff(buff);GameScene.show(window);shown.set(window);});
        try{Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot(image);}finally{TopScoresDeviceTest.render(()->{shown.get().hide();effect.get().detach();});}
    }
    private static void imbueAndDisabledVialDescriptionsRender()throws Exception{
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood(),"50% chance","qol-dragon-blood-duration-chance.png",true);
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch(),"3 turns","qol-icy-touch-duration-chill.png",true);
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfToxicEssence(),"5 turns","qol-toxic-essence-lingering-protection.png",true);
        for(int index=0;index<3;index++){
            final int kind=index;AtomicReference<com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> effect=new AtomicReference<>();AtomicReference<Window> shown=new AtomicReference<>();String[] saved={null};float[] time={0};
            TopScoresDeviceTest.render(()->{assertNull(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning.class));assertNull(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison.class));assertNull(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill.class));assertNull(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost.class));
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff;if(kind==0){com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FireImbue fire=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FireImbue.class);fire.set(7);buff=fire;}else if(kind==1)buff=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FrostImbue.class,8);else{com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ToxicImbue toxic=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ToxicImbue.class);toxic.set(9);buff=toxic;}effect.set(buff);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();buff.storeInBundle(before);saved[0]=before.toString();time[0]=Dungeon.hero.cooldown();Window window=new WndInfoBuff(buff);GameScene.show(window);shown.set(window);});
            try{Thread.sleep(300);TopScoresDeviceTest.render(()->{assertDescriptionViewport(shown.get());com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();effect.get().storeInBundle(after);assertEquals(saved[0],after.toString());assertEquals(time[0],Dungeon.hero.cooldown(),0);});TopScoresDeviceTest.screenshot("qol-imbue-normal-buff-"+kind+".png");}finally{TopScoresDeviceTest.render(()->{shown.get().hide();effect.get().detach();});}
        }
        aquaticHealingBuffDescriptionRenders("_30 HP_","qol-aquatic-buff-stored-hp.png");
        com.watabou.utils.Bundle original=new com.watabou.utils.Bundle();TopScoresDeviceTest.render(()->{Dungeon.apdStoreCustomRules(original);com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);});
        try{com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood vial=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood();TopScoresDeviceTest.render(()->{vial.level(3);vial.levelKnown=vial.cursedKnown=true;});showItem(vial,"rules disable the healing","qol-blood-vial-disabled-healing.png",true,true);aquaticHealingBuffDescriptionRenders("reserve is still spent","qol-aquatic-buff-disabled-healing.png");}finally{TopScoresDeviceTest.render(()->Dungeon.apdRestoreCustomRules(original));}
    }
    private static void reviewedElixirDescriptionsRender()throws Exception{
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall(),"10 extra turns","qol-feather-fall-extra-time.png",true);
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor(),"does not stack","qol-arcane-armor-no-stack.png",true);
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight(),"five level-ups","qol-might-level-duration.png",true);
        AtomicReference<Window> shown=new AtomicReference<>();AtomicReference<com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> effect=new AtomicReference<>();TopScoresDeviceTest.render(()->{assertNull(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall.FeatherBuff.class));effect.set(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.append(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall.FeatherBuff.class,50));Window window=new WndInfoBuff(effect.get());GameScene.show(window);shown.set(window);});
        try{Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot("qol-feather-buff-concise.png");}finally{TopScoresDeviceTest.render(()->{shown.get().hide();effect.get().detach();});}
        int[] original=new int[3];TopScoresDeviceTest.render(()->{assertNull(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight.HTBoost.class));original[0]=Dungeon.hero.STR;original[1]=Dungeon.hero.HT;original[2]=Dungeon.hero.HP;new com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight().apply(Dungeon.hero);effect.set(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight.HTBoost.class));assertTrue(effect.get().desc().contains("Level-ups remaining: _5_"));Window window=new WndInfoBuff(effect.get());GameScene.show(window);shown.set(window);});
        try{Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot("qol-might-buff-level-count.png");}finally{TopScoresDeviceTest.render(()->{shown.get().hide();effect.get().detach();Dungeon.hero.STR=original[0];Dungeon.hero.HT=original[1];Dungeon.hero.HP=original[2];});}
    }
    private static void forceKnownBonusWithUnknownStrengthRender()throws Exception{
        Ring previous=Dungeon.hero.belongings.ring;com.shatteredpixel.shatteredpixeldungeon.items.KindofMisc misc=Dungeon.hero.belongings.misc;boolean enhanced=Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnhancedRings.class)!=null;
        com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce force=new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce();
        try{TopScoresDeviceTest.render(()->{for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:new java.util.ArrayList<>(Dungeon.hero.buffs(Ring.RingBuff.class)))buff.detach();force.anonymize();force.level(3);force.levelKnown=force.cursedKnown=true;Dungeon.hero.belongings.ring=force;force.activate(Dungeon.hero);com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfMight unknown=new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfMight();unknown.level(9);unknown.levelKnown=false;Dungeon.hero.belongings.misc=unknown;com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnhancedRings.class);assertEquals(5,com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce.armedDamageBonus(Dungeon.hero));});
            showItem(force,"Melee weapon damage bonus: _5_","qol-force-known-bonus-unknown-strength.png",true,true);
            calculationWindow(() -> new WndInfoItem(force), "Identify equipped rings", "qol-force-retained-unknown-strength.png");
        }finally{TopScoresDeviceTest.render(()->{for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:new java.util.ArrayList<>(Dungeon.hero.buffs(Ring.RingBuff.class)))buff.detach();if(!enhanced)com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.detach(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnhancedRings.class);Dungeon.hero.belongings.ring=previous;Dungeon.hero.belongings.misc=misc;if(previous!=null)previous.activate(Dungeon.hero);if(misc instanceof Ring)misc.activate(Dungeon.hero);});}
    }
    private static void reviewedRunestoneTargetDescriptionsRender()throws Exception{
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression(),"alert enemies","qol-aggression-alert-enemies.png",true);
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDeepSleep(),"another creature","qol-deep-sleep-targets.png",true);
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFear(),"no effect on allies","qol-fear-targets.png",true);
    }
    private static void hornMealDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact previous=Dungeon.hero.belongings.artifact;
        try{for(boolean cursed:new boolean[]{false,true})for(int charges:new int[]{0,2}){
            com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty horn=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty();TopScoresDeviceTest.render(()->{try{horn.levelKnown=horn.cursedKnown=true;horn.cursed=cursed;java.lang.reflect.Field charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge");charge.setAccessible(true);charge.setInt(horn,charges);Dungeon.hero.belongings.artifact=horn;assertEquals(charges>0,horn.actions(Dungeon.hero).contains("EAT"));assertFalse(horn.info().contains("cannot be used"));assertFalse(horn.info().contains("eat until you're full"));}catch(Exception e){throw new RuntimeException(e);}});
            showItem(horn,cursed?"Other food is":"multiple charges","qol-horn-meal-"+cursed+"-charges-"+charges+".png",true,true);
        }}finally{TopScoresDeviceTest.render(()->Dungeon.hero.belongings.artifact=previous);}
    }
    private static void chaliceNormalRatesRender()throws Exception{
        com.watabou.utils.Bundle original=new com.watabou.utils.Bundle();
        TopScoresDeviceTest.render(()->{Dungeon.apdStoreCustomRules(original);com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,100).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);});
        try{for(int level:new int[]{0,3,10}){
            com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood chalice=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood();
            TopScoresDeviceTest.render(()->{chalice.level(level);chalice.levelKnown=chalice.cursedKnown=true;String text=chalice.info();assertFalse(text.contains("Pricking upgrades"));assertTrue(text,text.contains("Does not heal while starving"));});
            showItem(chalice,"1 HP every "+com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.ObfuscationExplanation.number(10f-(1.33f+level*.667f))+" turns","qol-chalice-concise-level-"+level+".png",false,true);
            if(level==10)calculationWindow(()->new WndInfoItem(chalice),"cannot be pricked again","qol-chalice-max-upgrade-retained-detail.png");
        }}finally{TopScoresDeviceTest.render(()->Dungeon.apdRestoreCustomRules(original));}
    }
    private static void artifactBuffsAndDisabledHealingRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows cloak=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows();
        com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns cape=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns();
        AtomicReference<com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> activeCape=new AtomicReference<>();
        TopScoresDeviceTest.render(()->{cape.activate(Dungeon.hero);cape.charge(Dungeon.hero,25);activeCape.set(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns.Thorns.class));assertNotNull(activeCape.get());});
        try{for(int kind=0;kind<2;kind++){
            final int type=kind;AtomicReference<Window> shown=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff=activeCape.get();if(type==0){buff=cloak.new cloakStealth();com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();buff.storeInBundle(state);state.put("turnsToCost",3);buff.restoreFromBundle(state);assertTrue(buff.desc(),buff.desc().contains("last known position"));}else assertTrue(buff.desc(),buff.desc().contains("from none to all"));Window window=new WndInfoBuff(buff);GameScene.show(window);shown.set(window);});
            Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot(type==0?"qol-cloak-stealth-concise.png":"qol-cape-thorns-concise.png");TopScoresDeviceTest.render(()->shown.get().hide());
        }}finally{TopScoresDeviceTest.render(()->activeCape.get().detach());}
        com.watabou.utils.Bundle previousRules=new com.watabou.utils.Bundle();TopScoresDeviceTest.render(()->{Dungeon.apdStoreCustomRules(previousRules);com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),settings=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(settings);all.put("apd_custom_rules",settings);Dungeon.apdRestoreCustomRules(all);});
        try{com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood chalice=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood();TopScoresDeviceTest.render(()->{chalice.level(10);chalice.levelKnown=chalice.cursedKnown=true;assertFalse(chalice.info().contains("upgrades the Chalice"));assertFalse(chalice.actions(Dungeon.hero).contains("PRICK"));});showItem(chalice,"rules prevent natural healing","qol-chalice-healing-zero.png",true,true);}finally{TopScoresDeviceTest.render(()->Dungeon.apdRestoreCustomRules(previousRules));}
    }
    private static void catalystAndTransferDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst catalyst=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst();
        showItem(catalyst,"6 alchemy energy","qol-catalyst-concise.png",true);
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal(),"Repairs worn thrown weapons","qol-liquid-metal-reviewed.png",true);
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfGathering(),"beside the downstairs","qol-gathering-reviewed.png",true);
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfForwarding(),"Nothing is sold","qol-forwarding-concise.png",true);
        AtomicReference<Window> shown=new AtomicReference<>();
        TopScoresDeviceTest.render(()->{
            java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket> choices=new java.util.ArrayList<>();choices.add(new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.PetrifiedSeed());choices.add(new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.MossyClump());choices.add(new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals());choices.add(new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft());for(Item item:choices)item.levelKnown=item.cursedKnown=true;
            com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();catalyst.storeInBundle(state);state.put("rolled_trinkets",choices);catalyst.restoreFromBundle(state);Window window=new com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst.WndTrinket(catalyst);GameScene.show(window);shown.set(window);
        });Thread.sleep(300);
        TopScoresDeviceTest.render(()->{try{assertEquals("Actual existing catalyst selection offers four choices",4,TopScoresDeviceTest.find(shown.get(),com.shatteredpixel.shatteredpixeldungeon.ui.ItemButton.class).size());for(RenderedTextBlock text:TopScoresDeviceTest.find(shown.get(),RenderedTextBlock.class)){Point a=text.camera().cameraToScreen(text.left(),text.top()),b=text.camera().cameraToScreen(text.right(),text.bottom());assertTrue("Catalyst choice text fits",a.x>=0&&a.y>=0&&b.x<=Game.width&&b.y<=Game.height);}}catch(Exception e){throw new RuntimeException(e);}});
        TopScoresDeviceTest.screenshot("qol-catalyst-four-choices.png");TopScoresDeviceTest.render(()->shown.get().hide());
    }
    private static void correctedEnchantmentDescriptionsRender()throws Exception{
        Sword vorpal=new Sword();TopScoresDeviceTest.render(()->{vorpal.level(3);vorpal.levelKnown=vorpal.cursedKnown=true;vorpal.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vorpal());});showItem(vorpal,"replace a hit's immediate damage","qol-vorpal-damage-replacement.png",true,true);
        for(int kind=0;kind<2;kind++){
            final int type=kind;AtomicReference<Window> shown=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff;if(type==0){com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking.BlockBuff block=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking.BlockBuff();block.setShield(10);buff=block;}else{com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic.ConservedDamage stored=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic.ConservedDamage();stored.setBonus(12);buff=stored;}Window window=new WndInfoBuff(buff);GameScene.show(window);shown.set(window);});
            Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));TopScoresDeviceTest.screenshot(type==0?"qol-blocking-shield-concise.png":"qol-kinetic-stored-damage-concise.png");TopScoresDeviceTest.render(()->shown.get().hide());
        }
        com.watabou.utils.Bundle previousRules=new com.watabou.utils.Bundle();TopScoresDeviceTest.render(()->{Dungeon.apdStoreCustomRules(previousRules);com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),settings=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(settings);all.put("apd_custom_rules",settings);Dungeon.apdRestoreCustomRules(all);});
        try{for(boolean known:new boolean[]{true,false}){Sword vampire=new Sword();TopScoresDeviceTest.render(()->{vampire.level(3);vampire.levelKnown=known;vampire.cursedKnown=true;vampire.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vampiric());});showItem(vampire,"rules prevent this enchantment's healing","qol-vampiric-healing-zero-"+known+".png",true,known);}}finally{TopScoresDeviceTest.render(()->Dungeon.apdRestoreCustomRules(previousRules));}
    }
    private static void vampiricCustomHealingDescriptionsRender() throws Exception {
        com.watabou.utils.Bundle previous = new com.watabou.utils.Bundle();
        TopScoresDeviceTest.render(() -> Dungeon.apdStoreCustomRules(previous));
        try {
            for (int percent : new int[]{25, 200}) {
                Sword sword = new Sword(); String[] expected = new String[1];
                TopScoresDeviceTest.render(() -> {
                    com.watabou.utils.Bundle all = new com.watabou.utils.Bundle(), settings = new com.watabou.utils.Bundle();
                    com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING, percent).store(settings); all.put("apd_custom_rules", settings); Dungeon.apdRestoreCustomRules(all);
                    sword.level(3); sword.levelKnown = sword.cursedKnown = true; sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vampiric());
                    float chance = (.05f + .25f * (Dungeon.hero.HT - Dungeon.hero.HP) / Dungeon.hero.HT) * com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.EnchantmentExplanation.knownPower(Dungeon.hero);
                    assertTrue(Float.isFinite(chance)); expected[0] = "about _" + com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##", 50 * Math.max(1, chance) * percent / 100f) + "%_";
                });
                showItem(sword, expected[0], "qol-vampiric-custom-healing-" + percent + ".png", true, true);
            }
        } finally { TopScoresDeviceTest.render(() -> Dungeon.apdRestoreCustomRules(previous)); }
    }
    private static void ordinaryEnchantmentCatalogRender()throws Exception{
        for(String name:new String[]{"Blazing","Blocking","Blooming","Chilling","Corrupting","Crystal","Elastic","Eldritch","Grim","Kinetic","Lucky","Projecting","Shocking","Unstable","Vampiric","Venomous","Vorpal"}){
            Sword sword=new Sword();TopScoresDeviceTest.render(()->{try{sword.level(3);sword.levelKnown=sword.cursedKnown=true;sword.enchant((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments."+name).getDeclaredConstructor().newInstance());}catch(Exception e){throw new RuntimeException(e);}});
            showItem(sword,name.equals("Crystal")?"perfect condition":name.equals("Shocking")?"other nearby creatures":name.equals("Eldritch")?"creatures that witness":name.equals("Projecting")?"to melee reach":"enchantment","qol-enchantment-"+name+"-normal.png",true,!name.equals("Elastic"));
        }
        Sword crystalSword=new Sword();TopScoresDeviceTest.render(()->{crystalSword.level(3);crystalSword.levelKnown=crystalSword.cursedKnown=true;com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Crystal crystal=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Crystal();crystal.setDurability(5);crystalSword.enchant(crystal);});showItem(crystalSword,"risk of permanently breaking","qol-crystal-cracked-warning.png",true,true);
    }
    private static void intrinsicReachDescriptionsRender()throws Exception{
        for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon weapon:new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon[]{new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip()})for(boolean known:new boolean[]{false,true}){
            TopScoresDeviceTest.render(()->{weapon.level(3);weapon.levelKnown=known;weapon.cursedKnown=true;});showItem(weapon,"Base reach: _"+weapon.RCH+" tiles_","qol-base-reach-"+weapon.getClass().getSimpleName()+"-"+known+".png",true);
        }
    }
    private static void unidentifiedWandExamplesRender()throws Exception{
        for(String name:new String[]{"BlastWave","Corrosion","Corruption","Disintegration","Fireblast","Frost","Lightning","LivingEarth","MagicMissile","PrismaticLight","Regrowth","Transfusion","Warding"}){
            com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOf"+name).getDeclaredConstructor().newInstance();
            TopScoresDeviceTest.render(()->{wand.level(7);wand.levelKnown=false;wand.cursedKnown=wand.curChargeKnown=true;wand.curCharges=2;});
            showItem(wand,name.equals("Corruption")?"A dark wand topped with a small decorative skull":name.equals("Regrowth")?"charges":"Unupgraded example (+0)","qol-wand-zero-example-"+name+".png",true);
            if(name.equals("Frost")){showItem(wand,"This wand deals less damage to chilled targets","qol-frost-specific-damage-unknown.png",true);TopScoresDeviceTest.render(()->{wand.levelKnown=true;});showItem(wand,"cannot harm frozen targets","qol-frost-specific-damage-known.png",true);}
        }
    }
    private static void staffImbueConfirmationsRender()throws Exception{
        int[][] cases={{0,2,1,2},{3,3,1,4},{3,2,1,3},{3,8,0,3}};
        for(int index=0;index<cases.length;index++){
            int[] values=cases[index];AtomicReference<WndOptions> shown=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{try{
                com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff();staff.level(values[0]);staff.levelKnown=staff.cursedKnown=true;
                com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.level(values[1]);wand.levelKnown=values[2]==1;wand.cursedKnown=wand.curChargeKnown=true;
                com.watabou.utils.Bundle beforeStaff=new com.watabou.utils.Bundle(),beforeWand=new com.watabou.utils.Bundle();staff.storeInBundle(beforeStaff);wand.storeInBundle(beforeWand);
                java.util.List<WndOptions> before=TopScoresDeviceTest.find(Game.scene(),WndOptions.class);
                java.lang.reflect.Field selector=staff.getClass().getDeclaredField("itemSelector");selector.setAccessible(true);((WndBag.ItemSelector)selector.get(staff)).onSelect(wand);
                java.util.List<WndOptions> created=TopScoresDeviceTest.find(Game.scene(),WndOptions.class);created.removeAll(before);assertEquals(1,created.size());WndOptions window=created.get(0);shown.set(window);
                boolean found=false;for(RenderedTextBlock text:TopScoresDeviceTest.find(window,RenderedTextBlock.class))if(text.text().contains("Replace the staff")){
                    assertTrue(text.text(),text.text().contains("if the staff was already upgraded"));assertTrue(text.text(),text.text().contains("+"+values[3]+" staff"));
                    if(values[2]==0){assertTrue(text.text(),text.text().contains("at least"));assertFalse(text.text(),text.text().contains("+8"));assertFalse(text.text(),text.text().contains("+9"));}
                    for(RedButton button:TopScoresDeviceTest.find(window,RedButton.class))assertTrue("Confirmation text does not overlap actions",text.bottom()<button.top());found=true;
                }assertTrue("Actual imbue confirmation body is present",found);
                com.watabou.utils.Bundle afterStaff=new com.watabou.utils.Bundle(),afterWand=new com.watabou.utils.Bundle();staff.storeInBundle(afterStaff);wand.storeInBundle(afterWand);assertEquals(beforeStaff.toString(),afterStaff.toString());assertEquals(beforeWand.toString(),afterWand.toString());
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(300);TopScoresDeviceTest.screenshot("qol-staff-imbue-confirmation-"+index+".png");TopScoresDeviceTest.render(()->shown.get().hide());
        }
    }
    private static void ordinaryFlailSpinDescriptionsRender()throws Exception{
        for(int count=1;count<=3;count++){
            final int spins=count;AtomicReference<Window> shown=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{
                com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail.SpinAbilityTracker buff=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail.SpinAbilityTracker();buff.spins=spins;com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();buff.storeInBundle(saved);saved.put("time",com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now()+3);buff.restoreFromBundle(saved);
                assertTrue(buff.desc(),buff.desc().contains("Spins: "+spins+"/3"));Window window=new WndInfoBuff(buff);GameScene.show(window);shown.set(window);
            });Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));
            TopScoresDeviceTest.screenshot("qol-flail-spin-"+count+"-normal.png");TopScoresDeviceTest.render(()->shown.get().hide());
        }
    }
    private static void ordinaryWarriorShieldDescriptionsRender()throws Exception{
        for(int reserve:new int[]{0,12})for(int time:new int[]{-25,0,40}){
            AtomicReference<Window> shown=new AtomicReference<>();
            AtomicReference<com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield> model=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{try{
                com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield shield=new com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield();shield.setShield(reserve);model.set(shield);
                java.lang.reflect.Field cooldown=shield.getClass().getDeclaredField("cooldown");cooldown.setAccessible(true);cooldown.setInt(shield,time);
                java.lang.reflect.Field original=shield.getClass().getDeclaredField("initialShield");original.setAccessible(true);original.setInt(shield,reserve>0||time>0?12:0);
                assertEquals(reserve>0||time>0?12:0,shield.activationShield());
                assertFalse(shield.desc(),shield.desc().contains("game-time units"));Window window=new WndInfoBuff(shield);GameScene.show(window);shown.set(window);
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));
            TopScoresDeviceTest.screenshot("qol-warrior-shield-"+reserve+"-"+time+"-normal.png");
            TopScoresDeviceTest.render(()->shown.get().hide());
            calculationWindow(()->new WndInfoBuff(model.get()),"Enemy-free decay progress","qol-warrior-shield-"+reserve+"-"+time+"-details.png");
        }
    }
    private static void ordinaryExoticPotionDescriptionsRender()throws Exception{
        String[] names={"PotionOfCorrosiveGas","PotionOfDivineInspiration","PotionOfMastery","PotionOfStormClouds"};
        for(String name:names){
            Item item=(Item)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic."+name).getDeclaredConstructor().newInstance();TopScoresDeviceTest.render(()->item.identify());
            for(boolean use:new boolean[]{false,true}){
                AtomicReference<WndInfoItem> shown=new AtomicReference<>();boolean[] temporarilyOwned={false};
                TopScoresDeviceTest.render(()->{if(use&&!Dungeon.hero.belongings.contains(item)){Dungeon.hero.belongings.backpack.items.add(item);temporarilyOwned[0]=true;}
                WndInfoItem window=use?new WndUseItem(null,item):new WndInfoItem(item);GameScene.show(window);shown.set(window);});
                Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));
                if(use)TopScoresDeviceTest.screenshot("qol-ordinary-potion-"+name+"-description.png");
                TopScoresDeviceTest.render(()->{shown.get().hide();if(temporarilyOwned[0])Dungeon.hero.belongings.backpack.items.remove(item);});
            }
        }
    }
    private static void ordinaryDartDescriptionsRender()throws Exception{
        String[] names={"Dart","AdrenalineDart","BlindingDart","ChillingDart","CleansingDart","DisplacingDart","HealingDart","HolyDart","IncendiaryDart","ParalyticDart","PoisonDart","RotDart","ShockingDart"};
        for(String name:names){
            Item item=(Item)Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts."+name).getDeclaredConstructor().newInstance();
            for(boolean use:new boolean[]{false,true}){
                AtomicReference<WndInfoItem> shown=new AtomicReference<>();boolean[] temporarilyOwned={false};
                TopScoresDeviceTest.render(()->{item.levelKnown=item.cursedKnown=true;if(use&&!Dungeon.hero.belongings.contains(item)){Dungeon.hero.belongings.backpack.items.add(item);temporarilyOwned[0]=true;}
                WndInfoItem window=use?new WndUseItem(null,item):new WndInfoItem(item);GameScene.show(window);shown.set(window);assertFalse(item.info(),item.info().contains("scheduled units"));assertFalse(item.info().contains("scaling floor"));if(name.equals("RotDart")){assertTrue(item.info(),item.info().contains("Sharpshooting damage"));assertFalse(item.info(),item.info().contains("Sharpshooting durability"));}});
                Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));
                if(use)TopScoresDeviceTest.screenshot("qol-ordinary-dart-"+name+"-description.png");
                TopScoresDeviceTest.render(()->{shown.get().hide();if(temporarilyOwned[0])Dungeon.hero.belongings.backpack.items.remove(item);});
            }
        }
    }
    private static void additionalDuelistDescriptionsRender()throws Exception{
        HeroClass previous=Dungeon.hero.heroClass;
        try{
            TopScoresDeviceTest.render(()->Dungeon.hero.heroClass=HeroClass.DUELIST);
            Item[] items={new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sickle(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarScythe(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dirk(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.AssassinsBlade(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sai(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gauntlet(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar() };
            String[] expected={"+7 base damage","base damage per turn","base damage per turn","every enemy within reach","visible empty tile","visible empty tile","visible empty tile","first costs a charge","uses up the combo","uses up the combo","uses up the combo","triples your evasion","After blocking an attack","After blocking an attack","enchantment power","attack speed"};
            for(int i=0;i<items.length;i++){
                Item item=items[i];TopScoresDeviceTest.render(()->{item.level(3);item.levelKnown=item.cursedKnown=true;});showItem(item,expected[i],"qol-duelist-"+i+"-concise.png",true);
                TopScoresDeviceTest.render(()->{item.levelKnown=false;com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon weapon=(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon)item;weapon.augment=com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.DAMAGE;assertTrue(item.info(),item.info().contains("_"+weapon.augment.damageFactor(weapon.min(0))+"-"+weapon.augment.damageFactor(weapon.max(0))+" damage_"));});showItem(item,"Unupgraded example (+0)","qol-duelist-"+i+"-unknown-example.png",true);
            }
        }finally{TopScoresDeviceTest.render(()->Dungeon.hero.heroClass=previous);}
    }
    private static void spiritBowConciseEnchantmentsRender()throws Exception{
        final KindofMisc[] previous=new KindofMisc[2];
        final java.util.Set<com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> original=new java.util.HashSet<>();
        try{
            TopScoresDeviceTest.render(()->{
                previous[0]=Dungeon.hero.belongings.ring;previous[1]=Dungeon.hero.belongings.misc;original.addAll(Dungeon.hero.buffs());
                for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff b:new java.util.ArrayList<>(Dungeon.hero.buffs(Ring.RingBuff.class)))b.detach();
                RingOfArcana arcana=new RingOfArcana();arcana.level(3);arcana.identify();Dungeon.hero.belongings.ring=arcana;Dungeon.hero.belongings.misc=null;arcana.activate(Dungeon.hero);
            });
            for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment effect:new com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment[]{new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vorpal()}){
                com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow bow=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow();
                TopScoresDeviceTest.render(()->{bow.levelKnown=bow.cursedKnown=true;bow.enchant(effect);assertFalse(bow.info(),bow.info().contains("Arcana changes:"));assertFalse(bow.info(),bow.info().contains("Arcana: "));assertFalse("No blank summary paragraph",bow.info().contains("\n\n\n"));});
                showItem(bow,effect instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting?"Shots can pass through walls":"chance","qol-spirit-bow-"+effect.getClass().getSimpleName()+"-concise.png",true,!(effect instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic));
            }
        }finally{TopScoresDeviceTest.render(()->{
            for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff b:new java.util.ArrayList<>(Dungeon.hero.buffs()))if(!original.contains(b))b.detach();
            Dungeon.hero.belongings.ring=(Ring)previous[0];Dungeon.hero.belongings.misc=previous[1];
            if(previous[0]!=null)previous[0].activate(Dungeon.hero);if(previous[1]!=null)previous[1].activate(Dungeon.hero);
        });}
    }
    private static void basicThrownAndDartDescriptionsRender()throws Exception{
        for(Item item:new Item[]{new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Javelin(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Trident()}){
            TopScoresDeviceTest.render(()->{item.level(3);item.levelKnown=item.cursedKnown=true;});
            showItem(item,"thrown weapons deals","qol-basic-thrown-"+item.getClass().getSimpleName()+"-known.png",true);
            TopScoresDeviceTest.render(()->item.levelKnown=false);
            showItem(item,"Unupgraded example (+0)","qol-basic-thrown-"+item.getClass().getSimpleName()+"-unknown.png",true);
        }
        com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart();TopScoresDeviceTest.render(()->assertFalse(dart.info(),dart.info().contains("Sharpshooting durability")));showItem(dart,"do not wear out","qol-plain-dart-reusable.png",true);
    }
    private static void missileEffectDescriptionsRender()throws Exception{
        Item[] items={new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Bolas(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ForceCube(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.HeavyBoomerang(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.FishingSpear()};
        String[] expected={"instant throw","movement speed","including you and allies","Stay there to catch it","Against piranhas"};
        for(int i=0;i<items.length;i++){items[i].levelKnown=items[i].cursedKnown=true;showItem(items[i],expected[i],"qol-missile-"+i+"-concise.png",true);}
    }
    private static void unknownRingMissileDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring old=Dungeon.hero.belongings.ring;
        java.util.Set<com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> before=new java.util.HashSet<>(Dungeon.hero.buffs());
        com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting unknown=new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting();
        com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Tomahawk axe=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Tomahawk();
        try{
            TopScoresDeviceTest.render(()->{
                unknown.anonymize();unknown.levelKnown=unknown.cursedKnown=false;Dungeon.hero.belongings.ring=unknown;unknown.activate(Dungeon.hero);axe.levelKnown=axe.cursedKnown=true;axe.level(3);
                unknown.level(0);String baseline=axe.info();unknown.level(9);assertEquals("Unknown ring cannot change ordinary weapon text",baseline,axe.info());
            });
            showItem(axe,"Weapon alone:","qol-tomahawk-unknown-ring-safe.png",true);
            TopScoresDeviceTest.render(()->unknown.levelKnown=unknown.cursedKnown=true);
            showItem(axe,"damage per turn","qol-tomahawk-known-ring.png",true);
        }finally{TopScoresDeviceTest.render(()->{for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:Dungeon.hero.buffs())if(!before.contains(buff)&&buff instanceof com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring.RingBuff)buff.detach();Dungeon.hero.belongings.ring=old;});}
    }
    private static void tenacityUpgradePreviewsRender()throws Exception{
        for(boolean cursed:new boolean[]{false,true}){
            AtomicReference<Window> shown=new AtomicReference<>();float[] time={0};String[] saved={null};com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity ring=new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity();
            TopScoresDeviceTest.render(()->{ring.anonymize();ring.levelKnown=ring.cursedKnown=true;ring.cursed=cursed;ring.level(0);time[0]=Dungeon.hero.cooldown();com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();ring.storeInBundle(before);saved[0]=before.toString();Window window=new com.shatteredpixel.shatteredpixeldungeon.windows.WndUpgrade(new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade(),ring,false);GameScene.show(window);shown.set(window);});
            try{Thread.sleep(300);TopScoresDeviceTest.render(()->{try{boolean found=false;for(RenderedTextBlock text:TopScoresDeviceTest.find(shown.get(),RenderedTextBlock.class)){if(text.text().contains(cursed?"more":"less"))found=true;Point a=text.camera().cameraToScreen(text.left(),text.top()),z=text.camera().cameraToScreen(text.right(),text.bottom());assertTrue("Upgrade explanation fits",a.x>=0&&a.y>=0&&z.x<=Game.width&&z.y<=Game.height);}assertTrue(found);com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();ring.storeInBundle(after);assertEquals(saved[0],after.toString());assertEquals(time[0],Dungeon.hero.cooldown(),0);}catch(Exception e){throw new RuntimeException(e);}});TopScoresDeviceTest.screenshot("qol-tenacity-upgrade-"+cursed+".png");}finally{TopScoresDeviceTest.render(()->shown.get().hide());}
        }
    }
    private static void tenacityDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity ring=new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity();
        for(boolean cursed:new boolean[]{false,true})for(boolean use:new boolean[]{false,true}){
            AtomicReference<WndInfoItem> shown=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{
                ring.anonymize();ring.level(cursed?0:3);ring.levelKnown=ring.cursedKnown=true;ring.cursed=cursed;
                assertTrue(ring.info(),ring.info().contains(cursed?"more damage taken":"less damage"));
                WndInfoItem window=use?new WndUseItem(null,ring):new WndInfoItem(ring);GameScene.show(window);shown.set(window);
            });Thread.sleep(300);TopScoresDeviceTest.render(()->assertDescriptionViewport(shown.get()));
            if(use)TopScoresDeviceTest.screenshot(cursed?"qol-tenacity-curse-concise.png":"qol-tenacity-protection-concise.png");
            TopScoresDeviceTest.render(()->shown.get().hide());
        }
    }
    private static void questDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal escape=new com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal();
        TopScoresDeviceTest.render(()->{
            escape.storedItems=new com.watabou.utils.Bundle();
            com.watabou.utils.Bundle gear=new com.watabou.utils.Bundle(),slots=new com.watabou.utils.Bundle();
            Dungeon.hero.belongings.storeInBundle(gear);Dungeon.quickslot.storePlaceholders(slots);
            escape.storedItems.put(com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal.BELONGINGS,gear);
            escape.storedItems.put(com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal.QUICKSLOTS,slots);
            escape.storedItems.put(com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal.GOLD,Dungeon.gold);
            escape.storedItems.put(com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal.ENERGY,Dungeon.energy);
        });
        Item[] items={new com.shatteredpixel.shatteredpixeldungeon.items.quest.CeremonialCandle(),new com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust(),escape};
        String[] expected={"north, east, south and west","banishes its wraiths","returns your original equipment"};
        for(int i=0;i<items.length;i++)showItem(items[i],expected[i],"qol-quest-"+i+"-concise.png",true);
    }
    private static void defensiveWeaponSingleBlockDescriptionsRender()throws Exception{
        final int strength=Dungeon.hero.STR;
        try{for(int currentStrength:new int[]{5,30})for(boolean known:new boolean[]{false,true})for(com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon weapon:new com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon[]{new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield(),new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield()}){
            TopScoresDeviceTest.render(()->{Dungeon.hero.STR=currentStrength;weapon.level(3);weapon.levelKnown=known;weapon.cursedKnown=true;weapon.augment=com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Augment.DAMAGE;String text=weapon.info();assertEquals(1,text.split("while wielded normally",-1).length-1);assertFalse(text,text.contains("This weapon blocks"));assertFalse(text,text.contains("\n\n\n"));assertTrue(text,text.contains("augmented"));});
            showItem(weapon,"while wielded normally","qol-defense-single-"+weapon.getClass().getSimpleName()+"-known-"+known+"-str-"+currentStrength+".png",true);
        }}finally{TopScoresDeviceTest.render(()->Dungeon.hero.STR=strength);}
    }
    private static void cursedRemovalAndChampionSlotDescriptionsRender()throws Exception{
        final com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon[] weapons=new com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon[2];final com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor[] armor={null};final Ring[] ring={null};final com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass kind=Dungeon.hero.subClass;final HeroClass heroClass=Dungeon.hero.heroClass;
        TopScoresDeviceTest.render(()->{assertNull(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune.class));weapons[0]=Dungeon.hero.belongings.weapon;weapons[1]=Dungeon.hero.belongings.secondWep;armor[0]=Dungeon.hero.belongings.armor;ring[0]=Dungeon.hero.belongings.ring;});
        try{
            for(com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem item:new com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem[]{new Sword(),new com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor(),new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste()}){
                TopScoresDeviceTest.render(()->{item.levelKnown=item.cursedKnown=true;item.cursed=true;if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon)Dungeon.hero.belongings.weapon=(com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon)item;else if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor)Dungeon.hero.belongings.armor=(com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor)item;else{((Ring)item).anonymize();Dungeon.hero.belongings.ring=(Ring)item;}});
                for(boolean immune:new boolean[]{false,true}){
                    if(immune)TopScoresDeviceTest.render(()->com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune.class));
                    try{showItem(item,"Magic immunity temporarily lets you take it off","qol-cursed-removal-"+item.getClass().getSimpleName()+"-immune-"+immune+".png",true);}
                    finally{if(immune)TopScoresDeviceTest.render(()->com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.detach(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune.class));}
                }
            }
            AtomicReference<Window> shown=new AtomicReference<>();float[] time={0};
            TopScoresDeviceTest.render(()->{Dungeon.hero.subClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.CHAMPION;Dungeon.hero.heroClass=HeroClass.DUELIST;Dungeon.hero.belongings.weapon=new Sword();Dungeon.hero.belongings.secondWep=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger();time[0]=Dungeon.hero.cooldown();new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip().execute(Dungeon.hero,"EQUIP");});
            try{Thread.sleep(300);TopScoresDeviceTest.render(()->{try{java.util.List<com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions> windows=TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class);assertEquals(1,windows.size());shown.set(windows.get(0));boolean found=false;for(RenderedTextBlock text:TopScoresDeviceTest.find(shown.get(),RenderedTextBlock.class))if(text.text().contains("Both share charges"))found=true;assertTrue(found);assertEquals(2,TopScoresDeviceTest.find(shown.get(),RedButton.class).size());assertEquals(time[0],Dungeon.hero.cooldown(),0);}catch(Exception e){throw new RuntimeException(e);}});TopScoresDeviceTest.screenshot("qol-champion-slot-choice-concise.png");}finally{TopScoresDeviceTest.render(()->{if(shown.get()!=null)shown.get().hide();});}
        }finally{TopScoresDeviceTest.render(()->{Dungeon.hero.belongings.weapon=weapons[0];Dungeon.hero.belongings.secondWep=weapons[1];Dungeon.hero.belongings.armor=armor[0];Dungeon.hero.belongings.ring=ring[0];Dungeon.hero.subClass=kind;Dungeon.hero.heroClass=heroClass;});}
    }
    private static void furtherPlantDescriptionsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass previous=Dungeon.hero.subClass;
        try{
            for(boolean warden:new boolean[]{false,true}){
                TopScoresDeviceTest.render(()->Dungeon.hero.subClass=warden?com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.WARDEN:com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.NONE);
                showItem(new com.shatteredpixel.shatteredpixeldungeon.plants.Blindweed.Seed(),warden?"invisible instead":"Blinds and cripples","qol-blindweed-warden-"+warden+".png",true);
                showItem(new com.shatteredpixel.shatteredpixeldungeon.plants.Stormvine.Seed(),warden?"levitates instead":"Keep away from pits","qol-stormvine-warden-"+warden+".png",true);
                showItem(new com.shatteredpixel.shatteredpixeldungeon.plants.Mageroyal.Seed(),warden?"protection from harmful clouds":"Does not restore health","qol-mageroyal-warden-"+warden+".png",true);
            }
            showItem(new com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush.Seed(),"cannot be eaten raw","qol-blandfruit-bush-raw-warning.png",true);
        }finally{TopScoresDeviceTest.render(()->Dungeon.hero.subClass=previous);}
    }
    private static void seedRuleDescriptionsAndDartPickerRender()throws Exception{
        final com.watabou.utils.Bundle rules=new com.watabou.utils.Bundle();final int challenges=Dungeon.challenges;final com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass kind=Dungeon.hero.subClass;
        TopScoresDeviceTest.render(()->Dungeon.apdStoreCustomRules(rules));
        try{
            for(boolean warden:new boolean[]{false,true})for(boolean disabled:new boolean[]{false,true}){
                TopScoresDeviceTest.render(()->{Dungeon.hero.subClass=warden?com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.WARDEN:com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.NONE;com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),selected=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,disabled?0:100).store(selected);all.put("apd_custom_rules",selected);Dungeon.apdRestoreCustomRules(all);});
                showItem(new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed(),disabled?"prevent Sungrass":warden?"keeps healing":"Gradually restores","qol-sungrass-warden-"+warden+"-disabled-"+disabled+".png",true);
            }
            TopScoresDeviceTest.render(()->Dungeon.challenges|=com.shatteredpixel.shatteredpixeldungeon.Challenges.NO_HERBALISM);
            showItem(new com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Seed(),"Barren Land prevents","qol-seed-barren-land-warning.png",true);
        }finally{TopScoresDeviceTest.render(()->{Dungeon.apdRestoreCustomRules(rules);Dungeon.challenges=challenges;Dungeon.hero.subClass=kind;});}
        final java.util.ArrayList<Item> original=new java.util.ArrayList<>();final AtomicReference<WndBag> picker=new AtomicReference<>();final AtomicReference<Window> prompt=new AtomicReference<>();
        final com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart dart=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart();final com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush.Seed unsupported=new com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush.Seed();final com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed supported=new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed();
        final java.lang.reflect.Field user=Item.class.getDeclaredField("curUser"),current=Item.class.getDeclaredField("curItem");user.setAccessible(true);current.setAccessible(true);final Object[] oldContext=new Object[2];final float[] time={0};final Point[] at=new Point[2];
        try{
            TopScoresDeviceTest.render(()->{try{oldContext[0]=user.get(null);oldContext[1]=current.get(null);original.addAll(Dungeon.hero.belongings.backpack.items);Dungeon.hero.belongings.backpack.items.clear();dart.quantity(2);Dungeon.hero.belongings.backpack.items.add(dart);Dungeon.hero.belongings.backpack.items.add(unsupported);Dungeon.hero.belongings.backpack.items.add(supported);time[0]=Dungeon.hero.cooldown();dart.execute(Dungeon.hero,"TIP");}catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(300);TopScoresDeviceTest.render(()->{try{
                java.util.List<WndBag> windows=TopScoresDeviceTest.find(Game.scene(),WndBag.class);assertEquals(1,windows.size());picker.set(windows.get(0));assertFalse(picker.get().getSelector().itemSelectable(unsupported));assertTrue(picker.get().getSelector().itemSelectable(supported));java.lang.reflect.Field field=ItemSlot.class.getDeclaredField("item");field.setAccessible(true);
                for(InventorySlot slot:TopScoresDeviceTest.find(picker.get(),InventorySlot.class)){Object item=field.get(slot);if(item==unsupported){assertFalse("Unsupported seed is disabled in actual picker",slot.active);at[0]=slot.camera().cameraToScreen(slot.left()+slot.width()/2,slot.top()+slot.height()/2);}if(item==supported){assertTrue(slot.active);at[1]=slot.camera().cameraToScreen(slot.left()+slot.width()/2,slot.top()+slot.height()/2);}}assertNotNull(at[0]);assertNotNull(at[1]);
            }catch(Exception e){throw new RuntimeException(e);}});
            touch(at[0]);TopScoresDeviceTest.render(()->{assertNotNull(picker.get().parent);assertEquals(time[0],Dungeon.hero.cooldown(),0);});TopScoresDeviceTest.screenshot("qol-dart-picker-invalid-seed-disabled.png");
            touch(at[1]);TopScoresDeviceTest.render(()->{try{java.util.List<com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions> windows=TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class);assertEquals(1,windows.size());prompt.set(windows.get(0));boolean found=false;for(RenderedTextBlock text:TopScoresDeviceTest.find(prompt.get(),RenderedTextBlock.class))if(text.text().contains("The coating wears off with use"))found=true;assertTrue(found);assertEquals(2,dart.quantity());assertEquals(1,supported.quantity());assertEquals(time[0],Dungeon.hero.cooldown(),0);}catch(Exception e){throw new RuntimeException(e);}});TopScoresDeviceTest.screenshot("qol-dart-real-tipping-prompt.png");
        }finally{TopScoresDeviceTest.render(()->{try{if(prompt.get()!=null)prompt.get().hide();if(picker.get()!=null)picker.get().hide();Dungeon.hero.belongings.backpack.items.clear();Dungeon.hero.belongings.backpack.items.addAll(original);user.set(null,oldContext[0]);current.set(null,oldContext[1]);}catch(Exception e){throw new RuntimeException(e);}});}
    }
    private static void waywardBuffDescriptionRenders()throws Exception{
        final com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Wayward.WaywardBuff[] buff={null};AtomicReference<Window> shown=new AtomicReference<>();String[] saved={null};
        TopScoresDeviceTest.render(()->{buff[0]=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Wayward.WaywardBuff.class,10);com.watabou.utils.Bundle before=new com.watabou.utils.Bundle();buff[0].storeInBundle(before);saved[0]=before.toString();Window window=new WndInfoBuff(buff[0]);GameScene.show(window);shown.set(window);});
        try{
            Thread.sleep(300);TopScoresDeviceTest.render(()->{assertTrue(buff[0].desc(),buff[0].desc().contains("Hitting with it ends this effect"));assertDescriptionViewport(shown.get());com.watabou.utils.Bundle after=new com.watabou.utils.Bundle();buff[0].storeInBundle(after);assertEquals(saved[0],after.toString());});
            TopScoresDeviceTest.screenshot("qol-wayward-buff-concise.png");
        }finally{TopScoresDeviceTest.render(()->{shown.get().hide();buff[0].detach();});}
    }
    private static void zeroHealingCursedTransferPopupRenders()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat rat=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();int[] hp={0};float[] fraction={0},time={0};java.util.Set<com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText> prior=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        try{
            TopScoresDeviceTest.render(()->{try{hp[0]=Dungeon.hero.HP;fraction[0]=Dungeon.hero.apdCustomHealingFraction;time[0]=Dungeon.hero.cooldown();rat.pos=-1;for(int offset:com.watabou.utils.PathFinder.NEIGHBOURS8){int pos=Dungeon.hero.pos+offset;if(Dungeon.level.passable[pos]&&com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(pos)==null){rat.pos=pos;break;}}assertTrue("A legal adjacent effect-test target",rat.pos>=0);rat.HP=rat.HT=100;GameScene.add(rat);Dungeon.hero.HP=Math.max(1,Dungeon.hero.HT/2);for(com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText text:TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.class))if(text.alive)prior.add(text);assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand.HealthTransfer().effect(null,Dungeon.hero,new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(Dungeon.hero.pos,rat.pos,com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE),true));assertEquals(Math.max(1,Dungeon.hero.HT/2),Dungeon.hero.HP);}catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(120);TopScoresDeviceTest.render(()->{try{boolean found=false;for(com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText text:TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.class))if(!prior.contains(text)&&text.alive&&"0".equals(text.text()))found=true;assertTrue("Actual zero healing popup is rendered",found);assertEquals(time[0],Dungeon.hero.cooldown(),0);}catch(Exception e){throw new RuntimeException(e);}});TopScoresDeviceTest.screenshot("qol-cursed-transfer-zero-healing-popup.png");
        }finally{TopScoresDeviceTest.render(()->{rat.destroy();if(rat.sprite!=null)rat.sprite.killAndErase();Dungeon.hero.HP=hp[0];Dungeon.hero.apdCustomHealingFraction=fraction[0];});}
    }
    public static class SoulMarkWindowTarget extends com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob {
        public SoulMarkWindowTarget(){HP=HT=20;state=HUNTING;}
        @Override public boolean surprisedBy(com.shatteredpixel.shatteredpixeldungeon.actors.Char attacker,boolean attacking){return false;}
    }
    private static void zeroHealingSoulMarkPopupRenders()throws Exception{
        int[] hp={0};float[] fraction={0},time={0};
        AtomicReference<SoulMarkWindowTarget> victim=new AtomicReference<>();
        AtomicReference<com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger> appetite=new AtomicReference<>();
        com.watabou.utils.Bundle savedHunger=new com.watabou.utils.Bundle();boolean[] hadHunger={false};
        java.util.Set<com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText> prior=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        try{
            TopScoresDeviceTest.render(()->{try{
                hp[0]=Dungeon.hero.HP;fraction[0]=Dungeon.hero.apdCustomHealingFraction;time[0]=Dungeon.hero.cooldown();Dungeon.hero.HP=Math.max(1,Dungeon.hero.HT/4);
                appetite.set(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger.class));hadHunger[0]=appetite.get()!=null;if(hadHunger[0])appetite.get().storeInBundle(savedHunger);
                for(com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText text:TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.class))if(text.alive)prior.add(text);
                SoulMarkWindowTarget target=new SoulMarkWindowTarget();target.pos=Dungeon.hero.pos+1;victim.set(target);com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(target,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SoulMark.class,10);
                target.defenseProc(Dungeon.hero,10);assertEquals(Math.max(1,Dungeon.hero.HT/4),Dungeon.hero.HP);
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(120);TopScoresDeviceTest.render(()->{try{
                boolean found=false;for(com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText text:TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.class))if(!prior.contains(text)&&text.alive&&"0".equals(text.text()))found=true;
                assertTrue("Actual zero-Healing Soul Mark popup is rendered",found);assertEquals(time[0],Dungeon.hero.cooldown(),0);
            }catch(Exception e){throw new RuntimeException(e);}});TopScoresDeviceTest.screenshot("qol-soulmark-zero-healing-popup.png");
        }finally{TopScoresDeviceTest.render(()->{
            Dungeon.hero.HP=hp[0];Dungeon.hero.apdCustomHealingFraction=fraction[0];
            if(hadHunger[0])appetite.get().restoreFromBundle(savedHunger);else{com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger added=Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger.class);if(added!=null)added.detach();}
            if(victim.get()!=null)for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:new java.util.ArrayList<>(victim.get().buffs()))buff.detach();
        });}
    }
    private static void zeroHealingHeartyMealPopupRenders()throws Exception{
        int[] hp={0};float[] fraction={0},time={0};Integer[] oldRank={null};java.util.Set<com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText> prior=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        try{
            TopScoresDeviceTest.render(()->{try{hp[0]=Dungeon.hero.HP;fraction[0]=Dungeon.hero.apdCustomHealingFraction;time[0]=Dungeon.hero.cooldown();oldRank[0]=Dungeon.hero.talents.get(0).get(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HEARTY_MEAL);Dungeon.hero.talents.get(0).put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HEARTY_MEAL,2);Dungeon.hero.HP=Math.max(1,Dungeon.hero.HT/4);for(com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText text:TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.class))if(text.alive)prior.add(text);com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.onFoodEaten(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger.HUNGRY,new com.shatteredpixel.shatteredpixeldungeon.items.food.Food());assertEquals(Math.max(1,Dungeon.hero.HT/4),Dungeon.hero.HP);}catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(120);TopScoresDeviceTest.render(()->{try{boolean found=false;for(com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText text:TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.class))if(!prior.contains(text)&&text.alive&&"0".equals(text.text()))found=true;assertTrue("Actual zero-Healing Hearty Meal popup is rendered",found);assertEquals(time[0],Dungeon.hero.cooldown(),0);}catch(Exception e){throw new RuntimeException(e);}});TopScoresDeviceTest.screenshot("qol-hearty-meal-zero-healing-popup.png");
        }finally{TopScoresDeviceTest.render(()->{Dungeon.hero.HP=hp[0];Dungeon.hero.apdCustomHealingFraction=fraction[0];if(oldRank[0]==null)Dungeon.hero.talents.get(0).remove(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HEARTY_MEAL);else Dungeon.hero.talents.get(0).put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HEARTY_MEAL,oldRank[0]);});}
    }
    private static void zeroHealingWaterskinConfirmationRenders(com.shatteredpixel.shatteredpixeldungeon.items.Waterskin skin)throws Exception{
        boolean prior=SPDSettings.confirmWaterskin();AtomicReference<com.shatteredpixel.shatteredpixeldungeon.windows.WndActionConfirmation> shown=new AtomicReference<>();float[] time={0};int drops=skin.storedDrops();
        try{
            TopScoresDeviceTest.render(()->{try{SPDSettings.confirmWaterskin(true);time[0]=Dungeon.hero.cooldown();skin.execute(Dungeon.hero,"DRINK");java.lang.reflect.Field field=com.shatteredpixel.shatteredpixeldungeon.items.Waterskin.class.getDeclaredField("drinkPrompt");field.setAccessible(true);shown.set((com.shatteredpixel.shatteredpixeldungeon.windows.WndActionConfirmation)field.get(skin));assertNotNull(shown.get());}catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(300);TopScoresDeviceTest.render(()->{try{boolean found=false;for(RenderedTextBlock text:TopScoresDeviceTest.find(shown.get(),RenderedTextBlock.class))if(text.text().contains("Your current rules prevent healing"))found=true;assertTrue(found);assertEquals(2,TopScoresDeviceTest.find(shown.get(),RedButton.class).size());}catch(Exception e){throw new RuntimeException(e);}});TopScoresDeviceTest.screenshot("qol-waterskin-zero-confirmation.png");
        }finally{TopScoresDeviceTest.render(()->{if(shown.get()!=null)shown.get().onBackPressed();SPDSettings.confirmWaterskin(prior);assertEquals(drops,skin.storedDrops());assertEquals(time[0],Dungeon.hero.cooldown(),0);});}
    }
    private static void inventoryScrollCancellationWarningRenders()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment enchant=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment();TopScoresDeviceTest.render(enchant::anonymize);showItem(enchant,"replacing its current effect","qol-enchantment-replaces-effect.png",true);
        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfSirensSong siren=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfSirensSong();TopScoresDeviceTest.render(siren::anonymize);showItem(siren,"may be charmed instead","qol-siren-immunity-limit.png",true);

        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation teleport=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation();TopScoresDeviceTest.render(teleport::anonymize);showItem(teleport,"Locked treasure rooms are excluded","qol-teleport-room-limit-description.png",true);
        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPassage passage=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPassage();TopScoresDeviceTest.render(passage::anonymize);showItem(passage,"Floor "+com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPassage.destinationDepth(Dungeon.depth),"qol-passage-live-destination.png",true);

        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse cleansing=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse();TopScoresDeviceTest.render(cleansing::anonymize);showItem(cleansing,"frees that spirit instead","qol-remove-curse-existing-priority.png",true);
        com.watabou.utils.Bundle savedRules=new com.watabou.utils.Bundle();TopScoresDeviceTest.render(()->Dungeon.apdStoreCustomRules(savedRules));
        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby lullaby=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby();
        com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop dew=new com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop();com.shatteredpixel.shatteredpixeldungeon.items.Waterskin skin=new com.shatteredpixel.shatteredpixeldungeon.items.Waterskin();TopScoresDeviceTest.render(()->{for(int drop=0;drop<5;drop++)skin.collectDew(new com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop());});
        try{
            TopScoresDeviceTest.render(()->{lullaby.anonymize();Dungeon.apdRestoreCustomRules(new com.watabou.utils.Bundle());});showItem(lullaby,"allies recover health","qol-lullaby-normal-healing.png",true);showItem(dew,"normally restores about 5%","qol-dew-normal-healing.png",true);showItem(skin,"Each drop normally heals","qol-waterskin-normal-healing.png",true);
            TopScoresDeviceTest.render(()->{com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),rules=new com.watabou.utils.Bundle();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);});showItem(lullaby,"prevent restorative sleep for you","qol-lullaby-zero-healing.png",true);showItem(dew,"prevent dew from healing","qol-dew-zero-healing.png",true);showItem(skin,"prevent dew healing","qol-waterskin-zero-healing.png",true);zeroHealingWaterskinConfirmationRenders(skin);zeroHealingCursedTransferPopupRenders();for(com.shatteredpixel.shatteredpixeldungeon.items.food.Food meal:new com.shatteredpixel.shatteredpixeldungeon.items.food.Food[]{new com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio(),new com.shatteredpixel.shatteredpixeldungeon.items.food.PhantomMeat(),new com.shatteredpixel.shatteredpixeldungeon.items.food.MeatPie(),new com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation()})showItem(meal,"rules","qol-zero-healing-food-"+meal.getClass().getSimpleName()+".png",true);zeroHealingHeartyMealPopupRenders();zeroHealingSoulMarkPopupRenders();
        }finally{TopScoresDeviceTest.render(()->Dungeon.apdRestoreCustomRules(savedRules));}

        AtomicReference<Window> shown=new AtomicReference<>();float[] time={0};
        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify scroll=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify();
        TopScoresDeviceTest.render(()->{scroll.anonymize();time[0]=Dungeon.hero.cooldown();Window window=scroll.new WndConfirmCancel();GameScene.show(window);shown.set(window);});
        try{
            Thread.sleep(300);TopScoresDeviceTest.render(()->{try{
                boolean found=false;for(RenderedTextBlock text:TopScoresDeviceTest.find(shown.get(),RenderedTextBlock.class))if(text.text().contains("Reading identified this scroll"))found=true;assertTrue(found);
                assertEquals("Viewing a warning does not itself spend a turn",time[0],Dungeon.hero.cooldown(),0);assertEquals(1,scroll.quantity());
                assertEquals("Only the two existing choices appear",2,TopScoresDeviceTest.find(shown.get(),RedButton.class).size());
            }catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.screenshot("qol-inventory-scroll-cancellation-warning.png");
        }finally{TopScoresDeviceTest.render(()->shown.get().hide());}
    }
    private static void landscapeItemWindowsFit(ActivityScenario<AndroidLauncher> scenario)throws Exception{
        final boolean original=SPDSettings.landscape();
        scenario.onActivity(activity->SPDSettings.landscape(true));
        try{
            waitFor(()->Game.width>Game.height&&Game.scene() instanceof GameScene&&!Game.switchingScene());Thread.sleep(500);
            com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage mirror=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage();
            com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip whip=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip();
            com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor armor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor();
            com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding ward=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding();
            TopScoresDeviceTest.render(()->{mirror.anonymize();mirror.quantity(2);whip.level(3);whip.levelKnown=whip.cursedKnown=true;whip.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic());armor.level(2);armor.levelKnown=armor.cursedKnown=true;armor.glyph=new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity();ward.level(3);ward.levelKnown=ward.cursedKnown=ward.curChargeKnown=true;});
            showItem(mirror,"disappear when damaged","qol-landscape-mirror-full-text.png",true);
            showItem(whip,"elastic enchantment","qol-landscape-elastic-whip.png",true);
            showItem(armor,"glyph of viscosity","qol-landscape-viscosity-armor.png",true);
            showItem(ward,"","qol-landscape-warding-retained-panel.png",true,true);
        }finally{
            scenario.onActivity(activity->{SPDSettings.landscape(original);if(!original)activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);});
            waitFor(()->(original?Game.width>Game.height:Game.height>Game.width)&&Game.scene() instanceof GameScene&&!Game.switchingScene());Thread.sleep(500);
            if(!original)scenario.onActivity(activity->activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED));
        }
    }
    private static void openingWandWindowsDoesNotCleanUpInfusion()throws Exception{
        for(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand:new com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand[]{new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth(),new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding()}){
            TopScoresDeviceTest.render(()->{wand.cursed=true;wand.curseInfusionBonus=true;wand.level(5);wand.levelKnown=wand.cursedKnown=wand.curChargeKnown=true;wand.cursed=false;});
            showItem(wand,"","qol-wand-open-preserves-infusion-"+wand.getClass().getSimpleName()+".png",true,!(wand instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile));
            TopScoresDeviceTest.render(()->{
                assertTrue("Inspection does not run gameplay cleanup",wand.curseInfusionBonus);
                assertEquals("Actual later gameplay query still clears expired infusion",5,wand.level());assertFalse(wand.curseInfusionBonus);
            });
        }
    }
    private static void classArmorCostsAndAquaRender()throws Exception{
        final com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[] previous={null};
        try{
            TopScoresDeviceTest.render(()->{previous[0]=Dungeon.hero.armorAbility;Dungeon.hero.armorAbility=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap();});
            for(com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor armor:new com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor[]{new com.shatteredpixel.shatteredpixeldungeon.items.armor.WarriorArmor(),new com.shatteredpixel.shatteredpixeldungeon.items.armor.MageArmor(),new com.shatteredpixel.shatteredpixeldungeon.items.armor.RogueArmor(),new com.shatteredpixel.shatteredpixeldungeon.items.armor.HuntressArmor(),new com.shatteredpixel.shatteredpixeldungeon.items.armor.DuelistArmor(),new com.shatteredpixel.shatteredpixeldungeon.items.armor.ClericArmor()}){
                TopScoresDeviceTest.render(()->{armor.tier=2;armor.level(2);Dungeon.hero.belongings.backpack.items.add(armor);});
                try{showItem(armor,"% charge","qol-class-armor-"+armor.getClass().getSimpleName()+"-cost.png",true);}
                finally{TopScoresDeviceTest.render(()->Dungeon.hero.belongings.backpack.items.remove(armor));}
            }
            AtomicReference<Window> shown=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{Dungeon.hero.armorAbility=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity();Window window=new WndInfoArmorAbility(HeroClass.CLERIC,Dungeon.hero.armorAbility);GameScene.show(window);shown.set(window);});
            Thread.sleep(300);
            TopScoresDeviceTest.render(()->{try{boolean found=false;for(RenderedTextBlock text:TopScoresDeviceTest.find(shown.get(),RenderedTextBlock.class)){if(text.text().contains("depends on the effect you choose"))found=true;assertFalse(text.text().contains("Armor charge is a resource"));}assertTrue("Trinity does not show one generic current cost",found);assertDescriptionViewport(shown.get());}catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.screenshot("qol-trinity-effect-specific-cost-normal.png");TopScoresDeviceTest.render(()->shown.get().hide());
        }finally{TopScoresDeviceTest.render(()->Dungeon.hero.armorAbility=previous[0]);}
        showItem(new com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew(),"including you and allies","qol-aqua-brew-knockback-warning.png",true);
        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation transmute=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation();TopScoresDeviceTest.render(transmute::anonymize);showItem(transmute,"Armor, plain darts, brews, elixirs and unique artifacts cannot be changed","qol-transmutation-actual-restrictions.png",true);
    }
    private static void amuletChoicesRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.Amulet amulet=new com.shatteredpixel.shatteredpixeldungeon.items.Amulet();
        showItem(amulet,"claim victory now","qol-amulet-normal-choices.png",true);
        final com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge[] ascent={null};
        try{
            TopScoresDeviceTest.render(()->ascent[0]=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge.class));
            showItem(amulet,"cannot activate or discard","qol-amulet-ascent-choices.png",true);
        }finally{TopScoresDeviceTest.render(()->{if(ascent[0]!=null)ascent[0].detach();});}
    }
    private static void utilityDescriptionsRender()throws Exception{
        Item[] items={new com.shatteredpixel.shatteredpixeldungeon.items.ArcaneResin(),new com.shatteredpixel.shatteredpixeldungeon.items.Stylus(),new com.shatteredpixel.shatteredpixeldungeon.items.TengusMask(),new com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown()};
        String[] expected={"one level","random glyph","choose a subclass","choose an armor ability"};
        for(int i=0;i<items.length;i++)showItem(items[i],expected[i],"qol-utility-"+i+"-concise.png",true);
    }
    private static void remainsAndLoreDescriptionsRender()throws Exception{
        int hp=Dungeon.hero.HP;
        com.watabou.utils.Bundle rules=new com.watabou.utils.Bundle();
        TopScoresDeviceTest.render(()->{Dungeon.apdStoreCustomRules(rules);Dungeon.hero.HP=1;});
        try{
            Item page=new com.shatteredpixel.shatteredpixeldungeon.items.remains.TornPage();
            showItem(page,"Would restore","qol-torn-page-current-healing.png",true);
            TopScoresDeviceTest.render(()->{
                com.watabou.utils.Bundle all=new com.watabou.utils.Bundle(),settings=new com.watabou.utils.Bundle();
                com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults().with(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING,0).store(settings);all.put("apd_custom_rules",settings);Dungeon.apdRestoreCustomRules(all);
            });
            showItem(page,"0 HP","qol-torn-page-zero-healing.png",true);
            showItem(new com.shatteredpixel.shatteredpixeldungeon.items.journal.RegionLorePage.Prison(),"read it in your journal","qol-prison-lore-concise.png");
        }finally{TopScoresDeviceTest.render(()->{Dungeon.hero.HP=hp;Dungeon.apdRestoreCustomRules(rules);});}
    }
    private static void craftedSpellVariantsRender()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental spell=new com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental();
        Class<?>[] variants={com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.AllyNewBornElemental.class,com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.FireElemental.class,com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.FrostElemental.class,com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.ShockElemental.class,com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.ChaosElemental.class};
        String[] expected={"no ranged attack","50% chance","one-in-three","including you and your allies","random cursed wand effects"};
        try{TopScoresDeviceTest.render(()->assertTrue(spell.collect()));
            for(int i=0;i<variants.length;i++){final Class<?> variant=variants[i];TopScoresDeviceTest.render(()->{com.watabou.utils.Bundle state=new com.watabou.utils.Bundle();spell.storeInBundle(state);state.put("summon_class",variant);spell.restoreFromBundle(state);});showItem(spell,expected[i],"qol-summon-"+i+"-concise.png",true);}
        }finally{TopScoresDeviceTest.render(()->Dungeon.hero.belongings.backpack.items.remove(spell));}
    }
    static void combinedOriginsAndRatPreferenceRenderAndRespond()throws Exception{
        boolean previous=SPDSettings.ratKingQuickSearch();AtomicReference<WndAPDSettings> shown=new AtomicReference<>();Point[] at=new Point[1];
        try{
            TopScoresDeviceTest.render(()->{
                SPDSettings.ratKingQuickSearch(false);WndAPDSettings window=new WndAPDSettings(com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection.GAMEPLAY);GameScene.show(window);shown.set(window);
                try{
                    java.lang.reflect.Field f=WndAPDSettings.class.getDeclaredField("pane");f.setAccessible(true);APDSettingsPane pane=(APDSettingsPane)f.get(window);
                    f=APDSettingsPane.class.getDeclaredField("chkRatKingSearch");f.setAccessible(true);CheckBox box=(CheckBox)f.get(pane);assertFalse(box.checked());
                    ScrollPane scroll=TopScoresDeviceTest.find(window,ScrollPane.class).get(0);scroll.scrollTo(0,Math.max(0,box.top()-8));
                    at[0]=box.camera().cameraToScreen(box.left()+box.width()/2,box.top()+box.height()/2);
                }catch(Exception e){throw new RuntimeException(e);}
            });Thread.sleep(300);touch(at[0]);TopScoresDeviceTest.render(()->assertTrue("Actual Quick Search checkbox tap enables preference",SPDSettings.ratKingQuickSearch()));
            TopScoresDeviceTest.screenshot("qol-rat-quick-search-setting.png");touch(at[0]);TopScoresDeviceTest.render(()->{assertFalse(SPDSettings.ratKingQuickSearch());shown.get().hide();shown.set(null);});
            Sword unknown=new Sword();unknown.levelKnown=false;unknown.cursedKnown=false;
            unknown.apdFoundOrigin=new com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins.Origin(8,0,com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents.C_LOCKED_CHEST,"TreasuryRoom");
            com.shatteredpixel.shatteredpixeldungeon.apd.qol.GenerationUpgradeRanges.set(unknown,0,3);
            showItem(unknown,"Possible upgrade level: +0 to +3","qol-unidentified-origin-range.png");
            TopScoresDeviceTest.render(()->{assertTrue(unknown.info().contains("Floor 8"));assertTrue(unknown.info().contains("Golden Key"));unknown.identify();assertFalse(unknown.info().contains("Found on"));assertFalse(unknown.info().contains("Possible upgrade level:"));
                try{AttackIndicator attack=TopScoresDeviceTest.find(Game.scene(),AttackIndicator.class).get(0);assertTrue("Attack control remains visible",attack.visible);}catch(Exception e){throw new RuntimeException(e);}
            });
        }finally{TopScoresDeviceTest.render(()->{if(shown.get()!=null)shown.get().hide();SPDSettings.ratKingQuickSearch(previous);});}
    }
    static void verifyOldBookshelfMarksAreHidden(int cell)throws Exception{
        // Real descent can award Pacifist; wait for its normal banner to expire before visual evidence.
        waitFor(()->{
            java.util.concurrent.atomic.AtomicBoolean expired=new java.util.concurrent.atomic.AtomicBoolean();
            TopScoresDeviceTest.render(()->{
                try { expired.set(TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.effects.BadgeBanner.class)
                        .stream().noneMatch(banner->banner.exists&&banner.visible)); }
                catch(Exception error){throw new RuntimeException(error);}
            });
            return expired.get();
        });
        TopScoresDeviceTest.render(()->{
            try {
                int active=0;
                for(com.shatteredpixel.shatteredpixeldungeon.effects.BadgeBanner banner:TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.effects.BadgeBanner.class))
                    if(banner.exists&&banner.visible)active++;
                android.util.Log.i("APD_CAPTURE_BANNERS","globalReferences="+com.shatteredpixel.shatteredpixeldungeon.effects.BadgeBanner.showing.size()+" activeCurrentScene="+active);
                assertEquals("no achievement overlay in current render tree",0,active);
            }catch(Exception error){throw new RuntimeException(error);}
        });
        TopScoresDeviceTest.render(()->{
            Level.set(cell,Terrain.BOOKSHELF);com.watabou.utils.Bundle legacy=new com.watabou.utils.Bundle();legacy.put("apd_searched_walls_v1",new int[]{cell});legacy.put("apd_door_checks_v1",new int[]{cell});
            Dungeon.level.searchedWalls.restore(legacy,Dungeon.level.length());assertTrue("legacy attempt retained",Dungeon.level.searchedWalls.attempted(cell));
            assertFalse("bookcase cannot be a hidden door",com.shatteredpixel.shatteredpixeldungeon.apd.qol.DoorSearchEligibility.candidate(Dungeon.level,cell));
            assertEquals(-1,com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,cell));
            assertEquals(-1,com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,cell-Dungeon.level.width()));GameScene.updateMap();
        });Thread.sleep(300);TopScoresDeviceTest.screenshot("qol-door-ineligible-bookshelf.png");
        TopScoresDeviceTest.render(()->{
            Level.set(cell,Terrain.WALL_DECO);com.watabou.utils.Bundle legacy=new com.watabou.utils.Bundle();legacy.put("apd_searched_walls_v1",new int[]{cell});Dungeon.level.searchedWalls.restore(legacy,Dungeon.level.length());
            assertFalse("decorated walls never hide doors",com.shatteredpixel.shatteredpixeldungeon.apd.qol.DoorSearchEligibility.candidate(Dungeon.level,cell));assertEquals(-1,com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,cell));GameScene.updateMap();
        });Thread.sleep(300);TopScoresDeviceTest.screenshot("qol-door-ineligible-decoration.png");TopScoresDeviceTest.render(()->{Level.set(cell,Terrain.WALL);GameScene.updateMap();});
    }
    /** Actual floor-one exclusion followed by a real transition to a searchable floor. */
    static void prepareSearchableFloor()throws Exception{
        TopScoresDeviceTest.render(()->{
            assertEquals(1,Dungeon.depth);assertFalse(com.shatteredpixel.shatteredpixeldungeon.apd.qol.DoorSearchEligibility.supportsDoors(Dungeon.level));
            com.watabou.utils.Bundle oldMarks=new com.watabou.utils.Bundle();java.util.ArrayList<Integer> walls=new java.util.ArrayList<>();
            for(int p=0;p<Dungeon.level.length();p++)if(com.shatteredpixel.shatteredpixeldungeon.apd.qol.DoorSearchEligibility.wallDisguise(Dungeon.level.map[p])&&Dungeon.level.heroFOV[p])walls.add(p);
            assertFalse("actual visible floor-one walls",walls.isEmpty());int[] cells=new int[walls.size()];for(int n=0;n<cells.length;n++)cells[n]=walls.get(n);
            oldMarks.put("apd_searched_walls_v1",cells);Dungeon.level.searchedWalls.restore(oldMarks,Dungeon.level.length());GameScene.updateMap();
            for(int p=0;p<Dungeon.level.length();p++)assertEquals("no old or new Floor 1 checkmarks",-1,com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,p));
        });TopScoresDeviceTest.screenshot("qol-doorless-floor1.png");
        TopScoresDeviceTest.render(()->{
            InterlevelScene.curTransition=Dungeon.level.getTransition(com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition.Type.REGULAR_EXIT);
            assertNotNull("real downstairs transition",InterlevelScene.curTransition);InterlevelScene.mode=InterlevelScene.Mode.DESCEND;Game.switchScene(InterlevelScene.class);
        });
        waitFor(()->Game.scene() instanceof GameScene&&Dungeon.depth==2&&Dungeon.hero.ready&&!Game.switchingScene());
        TopScoresDeviceTest.render(()->assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.qol.DoorSearchEligibility.supportsDoors(Dungeon.level)));
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
            combinedOriginsAndRatPreferenceRenderAndRespond();
            AtomicReference<WndGrassDewSettings> settings=new AtomicReference<>();Point[] p=new Point[1];
            TopScoresDeviceTest.render(()->{try{
                SPDSettings.autoCollectGrassDew(true);SPDSettings.autoCollectDewWithEnemies(false);
                WndGrassDewSettings w=new WndGrassDewSettings();GameScene.show(w);settings.set(w);
                java.util.List<CheckBox> controls=TopScoresDeviceTest.find(w,CheckBox.class);assertEquals(2,controls.size());
                CheckBox enemy=controls.get(1);assertFalse(enemy.checked());p[0]=enemy.camera().cameraToScreen(enemy.left()+enemy.width()/2,enemy.top()+enemy.height()/2);
            }catch(Exception e){throw new RuntimeException(e);}});
            touch(p[0]);TopScoresDeviceTest.render(()->assertTrue(SPDSettings.autoCollectDewWithEnemies()));
            TopScoresDeviceTest.screenshot("qol-dew-settings.png");TopScoresDeviceTest.render(()->settings.get().hide());
            com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage mirror=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage();
            TopScoresDeviceTest.render(()->{mirror.identify();mirror.quantity(2);assertTrue(mirror.collect());});
            showItem(mirror,"disappear when damaged","qol-mirror-scroll-full-text.png");
            Sword sword=new Sword();sword.levelKnown=true;sword.cursedKnown=true;
            TopScoresDeviceTest.render(()->{RingOfForce r=new RingOfForce();r.anonymize();r.levelKnown=true;r.cursedKnown=true;Dungeon.hero.belongings.ring=r;r.activate(Dungeon.hero);});
            TopScoresDeviceTest.render(()->{
                String info=sword.info();
                assertFalse(info.contains("Attack time is measured"));
                assertFalse(info.contains("Combined identified rings only"));
                assertFalse(info.contains("Enemy evasion and other effects determine"));
                assertTrue(info.contains("Attack time:"));
                assertTrue(info.contains("Accuracy:"));
            });
            showItem(sword,"Force damage:","qol-force-weapon.png");
            ThrowingKnife knife=new ThrowingKnife();knife.levelKnown=true;knife.cursedKnown=true;
            TopScoresDeviceTest.render(()->{RingOfSharpshooting r=new RingOfSharpshooting();r.anonymize();r.levelKnown=true;r.cursedKnown=true;Dungeon.hero.belongings.misc=r;r.activate(Dungeon.hero);});
            showItem(knife,"2-6 → 3-8","qol-sharpshooting-weapon.png");
            com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor scale=new com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor();
            scale.level(6);scale.levelKnown=scale.cursedKnown=true;
            final int[] priorStrength={0};final com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor[] priorArmor={null};
            TopScoresDeviceTest.render(()->{
                priorStrength[0]=Dungeon.hero.STR;priorArmor[0]=Dungeon.hero.belongings.armor;
                Dungeon.hero.STR=16;Dungeon.hero.belongings.armor=scale;
                assertFalse(scale.info().contains("Armor subtracts"));assertFalse(scale.info().contains("points short"));
            });
            showItem(scale,"6-32","qol-scale-armor-concise.png");
            TopScoresDeviceTest.render(()->{Dungeon.hero.STR=priorStrength[0];Dungeon.hero.belongings.armor=priorArmor[0];});
            elementalStrikeLiveTalentDescriptionsRender();
            elementalStrikeUnknownCurseDescriptionRenders();
            calculationWindowsRenderAndRespond();
            ordinaryEffectsMatchOwnerScreenshots();
            ordinaryConsumableDescriptionsRender();
            craftedSpellVariantsRender();
            remainsAndLoreDescriptionsRender();
            openingWandWindowsDoesNotCleanUpInfusion();
            classArmorCostsAndAquaRender();
            amuletChoicesRender();
            utilityDescriptionsRender();
            questDescriptionsRender();
            tenacityUpgradePreviewsRender();
            tenacityDescriptionsRender();
            unknownRingMissileDescriptionsRender();
            spiritBowConciseEnchantmentsRender();
            basicThrownAndDartDescriptionsRender();
            missileEffectDescriptionsRender();
            additionalDuelistDescriptionsRender();
            retainedArtifactDescriptionsRender();
            reviewedTrinketDamageDescriptionsRender();
            retainedTrinketDescriptionsRender();
            ordinaryDartDescriptionsRender();
            ordinaryExoticPotionDescriptionsRender();
            defensiveWeaponSingleBlockDescriptionsRender();
            cursedRemovalAndChampionSlotDescriptionsRender();
            seedRuleDescriptionsAndDartPickerRender();
            furtherPlantDescriptionsRender();
            waywardBuffDescriptionRenders();
            inventoryScrollCancellationWarningRenders();
            landscapeItemWindowsFit(scenario);
            ordinaryWarriorShieldDescriptionsRender();
            ordinaryFlailSpinDescriptionsRender();
            staffImbueConfirmationsRender();
            magicMissileDescriptionsRender();
            corrosionWandDescriptionsRender();
            disintegrationWandDescriptionsRender();
            lightningAndRegrowthDescriptionsRender();
            livingEarthAndPrismaticDescriptionsRender();
            passiveGlyphArcanaDescriptionsRender();
            cleansingStaminaMasteryDescriptionsRender();
            remainingBattlemageDescriptionsRender();
            conciseScrollStatusDescriptionsRender();
            conciseRechargeDescriptionsRender();
            retainedBlastScrollDescriptionsRender();
            sightHungerHealingTrinketDescriptionsRender();
            retainedGrowthAndWardDescriptionsRender();
            additionalTrinketDetailsRender();
            retainedThreeArtifactDetailsRender();
            retainedMonkEnergyDetailsRender();
            conciseHolyDescriptionsRender();
            conciseRetainedRingDetailsRender();
            conciseRetainedTalismanDetailsRender();
            conciseDefensiveBuffDescriptionsRender();
            liveElementalBlastDescriptionsRender();
            gradualHealingNonStackDescriptionsRender();
            hiddenTwinUpgradeDescriptionsRender();
            divineInspirationChoiceDescriptionRenders();
            imbueAndDisabledVialDescriptionsRender();
            reviewedElixirDescriptionsRender();
            forceKnownBonusWithUnknownStrengthRender();
            reviewedRunestoneTargetDescriptionsRender();
            hornMealDescriptionsRender();
            chaliceNormalRatesRender();
            artifactBuffsAndDisabledHealingRender();
            catalystAndTransferDescriptionsRender();
            correctedEnchantmentDescriptionsRender();
            vampiricCustomHealingDescriptionsRender();
            ordinaryEnchantmentCatalogRender();
            intrinsicReachDescriptionsRender();
            unidentifiedWandExamplesRender();
            inventoryOrderShowsAutomatically();
            prepareSearchableFloor();
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
                // Controlled terrain fixture, not a generated room: center the renderer before proof images.
                com.watabou.noosa.Camera.main.snapTo(Dungeon.hero.sprite.center());
                com.watabou.utils.Point heroScreen=com.watabou.noosa.Camera.main.cameraToScreen(Dungeon.hero.sprite.center().x,Dungeon.hero.sprite.center().y);
                assertTrue("controlled fixture hero is inside the viewport",heroScreen.x>0&&heroScreen.x<Game.width&&heroScreen.y>0&&heroScreen.y<Game.height);
            });
            verifyOldBookshelfMarksAreHidden(center[0]-Dungeon.level.width());
            TopScoresDeviceTest.render(()->{
                int w=Dungeon.level.width();
                Dungeon.hero.search(true);GameScene.updateMap();
                assertTrue("north front face",com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,center[0]-w)>0);
                assertEquals("south lip drawn on visible floor above",com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.OVERHANG,com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,center[0]));
                assertEquals("west partial corner end is unmarked",-1,com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,center[0]-1));
                assertEquals("east partial corner end is unmarked",-1,com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level,center[0]+1));
            });
            Thread.sleep(1800);TopScoresDeviceTest.screenshot("qol-searched-wall.png");
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
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);Thread.sleep(100);scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
            waitFor(()->Game.scene() instanceof GameScene&&!Game.switchingScene());
            TopScoresDeviceTest.render(()->assertTrue("searched south wall survives resume",Dungeon.level.searchedWalls.shows(Dungeon.level,center[0]+Dungeon.level.width())));
            System.gc();Thread.sleep(300);Runtime runtime=Runtime.getRuntime();
            android.util.Log.i("APD_MEMORY_PROBE","fixed-search-resume used="+(runtime.totalMemory()-runtime.freeMemory())+" limit="+runtime.maxMemory());
            TopScoresDeviceTest.render(WndGplLicense::show);Thread.sleep(300);TopScoresDeviceTest.screenshot("qol-offline-license.png");
            TopScoresDeviceTest.render(()->Game.switchScene(TitleScene.class));waitFor(()->Game.scene() instanceof TitleScene);
        }finally{SPDSettings.intro(intro);SPDSettings.autoCollectGrassDew(auto);SPDSettings.autoCollectDewWithEnemies(enemies);GamesInProgress.curSlot=slot;GamesInProgress.selectedClass=selected;}
    }
}
