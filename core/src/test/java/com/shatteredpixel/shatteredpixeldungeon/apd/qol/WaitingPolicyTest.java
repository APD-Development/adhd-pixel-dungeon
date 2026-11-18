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

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.watabou.utils.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

public class WaitingPolicyTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 private Preferences oldPrefs;private Hero oldHero;private com.shatteredpixel.shatteredpixeldungeon.levels.Level oldLevel;
 private Object oldSelector;private Field selector;private int oldFood,oldChallenges;private TestHero hero;private FileHandle file;
 public static class QuietSprite extends HeroSprite {
  @Override public void add(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.State state){}
  @Override public void remove(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.State state){}
  @Override public void operate(int pos){} @Override public void showStatus(int color,String text,Object...args){}
  @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){}
  @Override public void flash(){} @Override public void showAlert(){} @Override public void hideAlert(){}
 }
 public static class QuietSelector extends CellSelector {public QuietSelector(){super(null);}@Override public void resetKeyHold(){}}
 private static class TestHero extends Hero {
  boolean cancelDuringSpend;
  @Override public void next(){}
  @Override public void spend(float time){super.spend(time);if(cancelDuringSpend)interrupt();}
 }
 private static Object allocate(Class<?> type)throws Exception{
  Field f=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");f.setAccessible(true);Object unsafe=f.get(null);
  return unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,type);
 }
 @Before public void setup()throws Exception{
  HeadlessTestSupport.start();Field prefs=GameSettings.class.getDeclaredField("prefs");prefs.setAccessible(true);oldPrefs=(Preferences)prefs.get(null);
  file=new FileHandle(temp.newFile("prefs.xml"));file.delete();GameSettings.set(new HeadlessPreferences(file));
  oldHero=Dungeon.hero;oldLevel=Dungeon.level;oldFood=Statistics.foodEaten;oldChallenges=Dungeon.challenges;Dungeon.challenges=0;
  selector=GameScene.class.getDeclaredField("cellSelector");selector.setAccessible(true);oldSelector=selector.get(null);selector.set(null,allocate(QuietSelector.class));
  Dungeon.level=new GrassDewPickupTest.TestLevel();Dungeon.level.setSize(7,7);
  hero=new TestHero();hero.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(hero);hero.pos=24;hero.HT=100;hero.HP=80;hero.resting=true;
  hero.sprite=(QuietSprite)allocate(QuietSprite.class);hero.sprite.visible=false;Dungeon.hero=hero;
  Buff.affect(hero,Hunger.class);SPDSettings.intro(false);
 }
 @After public void restore()throws Exception{selector.set(null,oldSelector);Dungeon.hero=oldHero;Dungeon.level=oldLevel;Statistics.foodEaten=oldFood;Dungeon.challenges=oldChallenges;GameSettings.set(oldPrefs);}
 private void hunger(float value)throws Exception{Field f=Hunger.class.getDeclaredField("level");f.setAccessible(true);f.setFloat(hero.buff(Hunger.class),value);}
 private void food(Food food){hero.belongings.backpack.items.add(food);}
 @Test public void defaultsPreserveOriginalStopsAndDoNotEnableAutomation(){
  assertTrue(SPDSettings.waitStopFullHealth());assertTrue(SPDSettings.waitStopStarving());assertFalse(SPDSettings.waitStopCritical());assertFalse(SPDSettings.autoEatWhileWaiting());assertFalse(SPDSettings.autoEatWhenHungry());
 }
 @Test public void allPreferencesPersistIndependentlyAcrossRealFileReload(){
  SPDSettings.waitStopFullHealth(false);SPDSettings.waitStopStarving(false);SPDSettings.waitStopCritical(true);SPDSettings.waitCriticalPercent(30);SPDSettings.autoEatWhileWaiting(true);SPDSettings.autoEatWhenHungry(true);SPDSettings.singleTapSearch(true);
  GameSettings.set(new HeadlessPreferences(file));assertFalse(SPDSettings.waitStopFullHealth());assertFalse(SPDSettings.waitStopStarving());assertTrue(SPDSettings.waitStopCritical());assertEquals(30,SPDSettings.waitCriticalPercent());assertTrue(SPDSettings.autoEatWhileWaiting());assertTrue(SPDSettings.autoEatWhenHungry());assertTrue(SPDSettings.singleTapSearch());
  SPDSettings.waitStopFullHealth(true);assertTrue(SPDSettings.autoEatWhileWaiting());assertTrue(SPDSettings.waitStopCritical());
 }
 @Test public void invalidCriticalThresholdFallsBackWithoutChangingOtherPreferences(){SPDSettings.waitCriticalPercent(-1);assertEquals(20,SPDSettings.waitCriticalPercent());GameSettings.put("apd_wait_critical_percent",999);assertEquals(20,SPDSettings.waitCriticalPercent());assertFalse(SPDSettings.autoEatWhileWaiting());}
 @Test public void actualRegenerationFullHealthStopCanBeDisabled(){
  for(boolean stop:new boolean[]{true,false}){SPDSettings.waitStopFullHealth(stop);hero.HP=99;hero.resting=true;Regeneration regen=Buff.affect(hero,Regeneration.class);Bundle b=new Bundle();regen.storeInBundle(b);b.put(Regeneration.PARTIAL_REGEN,.99f);regen.restoreFromBundle(b);regen.act();assertEquals(100,hero.HP);assertEquals(!stop,hero.resting);}
 }
 @Test public void actualStarvationOnsetKeepsNormalDamageButRespectsStopChoice()throws Exception{
  for(boolean stop:new boolean[]{true,false}){SPDSettings.waitStopStarving(stop);hero.HP=80;hero.resting=true;hunger(449);hero.buff(Hunger.class).act();assertEquals(450,hero.buff(Hunger.class).hunger());assertEquals(79,hero.HP);assertEquals(!stop,hero.resting);}
 }
 @Test public void criticalStopIsIndependentAndUsesActualHpPercentage(){SPDSettings.waitStopCritical(true);hero.HP=21;assertEquals(WaitingPolicy.Turn.CONTINUE,WaitingPolicy.beforeWaitTurn(hero));hero.HP=20;assertEquals(WaitingPolicy.Turn.STOP,WaitingPolicy.beforeWaitTurn(hero));SPDSettings.waitStopCritical(false);assertEquals(WaitingPolicy.Turn.CONTINUE,WaitingPolicy.beforeWaitTurn(hero));}
 @Test public void disablingAllThreeAllowsStarvingLowHpWaitingWithoutEnablingFood()throws Exception{
  SPDSettings.waitStopFullHealth(false);SPDSettings.waitStopStarving(false);SPDSettings.waitStopCritical(false);hunger(450);hero.HP=1;
  assertEquals(WaitingPolicy.Turn.CONTINUE,WaitingPolicy.beforeWaitTurn(hero));assertFalse(WaitingPolicy.interruptOnStarvationDamage(hero));assertFalse(SPDSettings.autoEatWhileWaiting());
 }
 @Test public void normalManualFoodExecutionConsumesOneAndChargesThreeTurns()throws Exception{
  SPDSettings.autoEatWhileWaiting(true);hunger(450);Food food=new Food();food.quantity(2);food(food);float before=hero.cooldown();int meals=Statistics.foodEaten;
  assertEquals(WaitingPolicy.Turn.ATE,WaitingPolicy.beforeWaitTurn(hero));assertEquals(1,food.quantity());assertEquals(before+3,hero.cooldown(),0);assertEquals(150,hero.buff(Hunger.class).hunger());assertEquals(meals+1,Statistics.foodEaten);assertTrue(hero.resting);
 }
 @Test public void eatingUsesOriginalTalentTimeRatherThanAFreeOrFixedAction()throws Exception{
  hero.talents.get(0).put(Talent.IRON_STOMACH,1);SPDSettings.autoEatWhileWaiting(true);hunger(450);food(new Food());float before=hero.cooldown();
  assertEquals(WaitingPolicy.Turn.ATE,WaitingPolicy.beforeWaitTurn(hero));assertEquals(before+1,hero.cooldown(),0);
 }
 @Test public void hungryAndStarvingTriggersAreDistinct()throws Exception{
  SPDSettings.autoEatWhileWaiting(true);hunger(300);food(new SmallRation());assertFalse(WaitingPolicy.canAutoEat(hero));SPDSettings.autoEatWhenHungry(true);assertTrue(WaitingPolicy.canAutoEat(hero));assertEquals(WaitingPolicy.Turn.ATE,WaitingPolicy.beforeWaitTurn(hero));assertEquals(150,hero.buff(Hunger.class).hunger());
 }
 @Test public void starvationInterruptDefersToFoodThenStopsWhenFoodRunsOut()throws Exception{
  SPDSettings.autoEatWhileWaiting(true);hunger(449);food(new SmallRation());hero.buff(Hunger.class).act();assertTrue(hero.resting);assertEquals(79,hero.HP);assertEquals(WaitingPolicy.Turn.ATE,WaitingPolicy.beforeWaitTurn(hero));
  hunger(450);assertNull(WaitingPolicy.availableFood(hero));assertEquals(WaitingPolicy.Turn.STOP,WaitingPolicy.beforeWaitTurn(hero));SPDSettings.waitStopStarving(false);assertEquals(WaitingPolicy.Turn.CONTINUE,WaitingPolicy.beforeWaitTurn(hero));
 }
 @Test public void inappropriateRawSpecialUnknownAndCursedFoodAreNeverSelected(){
  class Unknown extends Food {@Override public boolean isIdentified(){return false;}}
  for(Food food:new Food[]{new MysteryMeat(),new FrozenCarpaccio(),new Pasty(),new PhantomMeat(),new Blandfruit(),new Unknown()}){food(food);assertFalse(WaitingPolicy.safeFood(food));}
  Food cursed=new Food();cursed.cursed=true;food(cursed);assertNull(WaitingPolicy.availableFood(hero));
 }
 @Test public void smallestSuitableOrdinaryFoodIsChosenWithoutTouchingSpecialFood(){Food ration=new Food();food(ration);food(new Pasty());SmallRation small=new SmallRation();food(small);assertSame(small,WaitingPolicy.availableFood(hero));}
 @Test public void autoEatOffOrNotWaitingNeverConsumesFood()throws Exception{
  hunger(450);Food food=new Food();food(food);assertEquals(WaitingPolicy.Turn.CONTINUE,WaitingPolicy.beforeWaitTurn(hero));SPDSettings.autoEatWhileWaiting(true);hero.resting=false;assertEquals(WaitingPolicy.Turn.STOP,WaitingPolicy.beforeWaitTurn(hero));assertEquals(1,food.quantity());
 }
 @Test public void independentCriticalStopTakesPriorityOverEating()throws Exception{
  SPDSettings.autoEatWhileWaiting(true);SPDSettings.waitStopCritical(true);hero.HP=20;hunger(450);Food food=new Food();food(food);assertEquals(WaitingPolicy.Turn.STOP,WaitingPolicy.beforeWaitTurn(hero));assertEquals(1,food.quantity());
 }
 @Test public void cancellationDuringEatingCannotRestartWaiting()throws Exception{
  SPDSettings.autoEatWhileWaiting(true);hunger(450);food(new Food());hero.cancelDuringSpend=true;assertEquals(WaitingPolicy.Turn.ATE,WaitingPolicy.beforeWaitTurn(hero));assertFalse(hero.resting);
 }
 @Test public void actualStarvationDamageRespectsCriticalSwitchWithoutChangingDamage()throws Exception{
  SPDSettings.waitStopStarving(false);
  for(boolean critical:new boolean[]{true,false}){SPDSettings.waitStopCritical(critical);hero.HP=21;hero.resting=true;hero.damage(1,hero.buff(Hunger.class));assertEquals(20,hero.HP);assertEquals(!critical,hero.resting);}
 }
 @Test public void actualOrdinaryDamageStillInterruptsEvenWithAllThreeStopsDisabled(){
  SPDSettings.waitStopFullHealth(false);SPDSettings.waitStopStarving(false);SPDSettings.waitStopCritical(false);hero.damageInterrupt=true;
  hero.damage(1,new Object());assertEquals(79,hero.HP);assertFalse(hero.resting);
 }
 @Test public void foodAndHungerBundleReloadDoNotInventOrConsumeAnything()throws Exception{
  SPDSettings.autoEatWhileWaiting(true);hunger(450);Food food=new Food();food.quantity(2);food(food);
  assertEquals(WaitingPolicy.Turn.ATE,WaitingPolicy.beforeWaitTurn(hero));Bundle savedFood=new Bundle(),savedHunger=new Bundle();food.storeInBundle(savedFood);hero.buff(Hunger.class).storeInBundle(savedHunger);
  Food restored=new Food();restored.restoreFromBundle(savedFood);assertEquals(1,restored.quantity());int hungerBefore=hero.buff(Hunger.class).hunger();hero.buff(Hunger.class).restoreFromBundle(savedHunger);assertEquals(hungerBefore,hero.buff(Hunger.class).hunger());assertTrue(SPDSettings.autoEatWhileWaiting());
 }
 @Test public void starvationTriggerDoesNotUseRoundedUpDisplayHunger()throws Exception{
  SPDSettings.autoEatWhileWaiting(true);food(new Food());hunger(449.5f);assertEquals(450,hero.buff(Hunger.class).hunger());assertFalse(hero.buff(Hunger.class).isStarving());assertFalse(WaitingPolicy.canAutoEat(hero));hunger(450);assertTrue(WaitingPolicy.canAutoEat(hero));
 }
 @Test public void hungryTriggerDoesNotUseRoundedUpDisplayHunger()throws Exception{
  SPDSettings.autoEatWhileWaiting(true);SPDSettings.autoEatWhenHungry(true);food(new Food());hunger(299.5f);assertEquals(300,hero.buff(Hunger.class).hunger());assertFalse(WaitingPolicy.canAutoEat(hero));hunger(300);assertTrue(WaitingPolicy.canAutoEat(hero));
 }
 @Test public void normalNoFoodChallengeReductionStillAppliesToTheMeal()throws Exception{
  Dungeon.challenges=Challenges.NO_FOOD;SPDSettings.autoEatWhileWaiting(true);hunger(450);food(new Food());float before=hero.cooldown();assertEquals(WaitingPolicy.Turn.ATE,WaitingPolicy.beforeWaitTurn(hero));assertEquals(350,hero.buff(Hunger.class).hunger());assertEquals(before+3,hero.cooldown(),0);
 }
 @Test public void lockedFloorOrParalysisDoesNotConsumeFood()throws Exception{
  SPDSettings.autoEatWhileWaiting(true);hunger(450);Food food=new Food();food(food);Dungeon.level.locked=true;assertFalse(WaitingPolicy.canAutoEat(hero));Dungeon.level.locked=false;hero.paralysed=1;assertFalse(WaitingPolicy.canAutoEat(hero));assertEquals(1,food.quantity());
 }
 @Test public void healingPotionFullHealthAndLastTickHonorWaitPreference(){
  for(boolean stop:new boolean[]{true,false}){SPDSettings.waitStopFullHealth(stop);hero.HP=99;hero.resting=true;Healing heal=Buff.affect(hero,Healing.class);heal.setHeal(1,0,1);heal.act();assertEquals(100,hero.HP);assertEquals(!stop,hero.resting);}
 }
 @Test public void sungrassFullHealthHonorsWaitPreference(){
  for(boolean stop:new boolean[]{true,false}){SPDSettings.waitStopFullHealth(stop);hero.HP=99;hero.resting=true;com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health heal=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health.class);heal.boost(100);heal.act();heal.act();assertEquals(100,hero.HP);assertEquals(!stop,hero.resting);heal.detach();}
 }
 @Test public void aquaticLastHealingTickAtFullHealthHonorsWaitPreference(){
  Dungeon.level.water[hero.pos]=true;
  for(boolean stop:new boolean[]{true,false}){SPDSettings.waitStopFullHealth(stop);hero.HP=99;hero.resting=true;com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing heal=Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation.AquaHealing.class);heal.set(1);heal.act();assertEquals(100,hero.HP);assertEquals(!stop,hero.resting);}
 }
 @Test public void wellFedFullHealthHonorsWaitPreference()throws Exception{
  for(boolean stop:new boolean[]{true,false}){SPDSettings.waitStopFullHealth(stop);hero.HP=99;hero.resting=true;WellFed fed=Buff.affect(hero,WellFed.class);Field f=WellFed.class.getDeclaredField("left");f.setAccessible(true);f.setInt(fed,19);fed.act();assertEquals(100,hero.HP);assertEquals(!stop,hero.resting);fed.detach();}
 }
 @Test public void chaliceInstantHealingAtFullHealthHonorsWaitPreference(){
  for(boolean stop:new boolean[]{true,false}){SPDSettings.waitStopFullHealth(stop);hero.HP=99;hero.resting=true;new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood().charge(hero,10);assertEquals(100,hero.HP);assertEquals(!stop,hero.resting);}
 }
 @Test public void healingEffectExhaustionBelowFullHealthRetainsLegacyStop(){SPDSettings.waitStopFullHealth(false);hero.HP=50;hero.resting=true;Healing heal=Buff.affect(hero,Healing.class);heal.setHeal(1,0,1);heal.act();assertEquals(51,hero.HP);assertFalse(hero.resting);}
}
