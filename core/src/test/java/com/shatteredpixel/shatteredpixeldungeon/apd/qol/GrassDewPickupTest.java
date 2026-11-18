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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameSettings;
import com.watabou.utils.Random;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.lang.reflect.Field;
import java.util.HashSet;
import static org.junit.Assert.*;

public class GrassDewPickupTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private Hero oldHero;
    private Level oldLevel;
    private Preferences oldPreferences;
    private FileHandle prefsFile;
    private TestHero hero;
    private TestLevel level;
    private Waterskin skin;
    private Dewdrop dew;
    private Heap heap;
    private GrassDewPickup pickup;
    public static class TestLevel extends Level {
        @Override protected boolean build(){return true;}
        @Override protected void createMobs(){}
        @Override protected void createItems(){}
    }
    private static class TestHero extends Hero {
        int nextCalls;
        @Override public void next(){nextCalls++;}
    }
    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        oldHero=Dungeon.hero; oldLevel=Dungeon.level;
        Field p=GameSettings.class.getDeclaredField("prefs");p.setAccessible(true);oldPreferences=(Preferences)p.get(null);
        prefsFile=new FileHandle(temp.newFile("settings.xml"));prefsFile.delete();
        SPDSettings.set(new HeadlessPreferences(prefsFile));
        SPDSettings.autoCollectGrassDew(true); //off by default since Nov 18; these tests exercise the feature
        level=new TestLevel();level.setSize(5,5);level.mobs=new HashSet<>();level.heaps=new com.watabou.utils.SparseArray<>();Dungeon.level=level;
        hero=new TestHero();hero.pos=12;hero.HP=10;hero.HT=20;hero.curAction=new HeroAction.Move(12);Dungeon.hero=hero;
        skin=new Waterskin();hero.belongings.backpack.items.add(skin);
        dew=new Dewdrop();heap=new Heap();heap.pos=12;heap.type=Heap.Type.HEAP;heap.items.add(dew);level.heaps.put(12,heap);
        pickup=new GrassDewPickup();
    }
    @After public void cleanup(){Dungeon.hero=oldHero;Dungeon.level=oldLevel;SPDSettings.set(oldPreferences);}
    private void offer(){pickup.offer(hero,level,12,heap);}
    private Rat enemy(boolean visible){Rat r=new Rat();r.pos=13;level.mobs.add(r);level.heroFOV[13]=visible;return r;}
    @Test public void movementAndNormalPickupAreTwoSeparateTurnsAndNeverHeal(){
        offer();assertEquals("0/20",skin.status());assertEquals(0,hero.cooldown(),0);
        hero.spend(1);assertEquals(1,hero.cooldown(),0);assertEquals("0/20",skin.status());
        assertTrue(pickup.collectIfSafe(hero));assertEquals(2,hero.cooldown(),0);assertEquals(1,hero.nextCalls);
        assertEquals("1/20",skin.status());assertEquals(10,hero.HP);assertNull(level.heaps.get(12));
        assertFalse(pickup.collectIfSafe(hero));assertEquals(2,hero.cooldown(),0);
    }
    @Test public void visibleEnemyBlocksWithoutConsumingDewOrPickupTurn(){offer();enemy(true);assertFalse(pickup.collectIfSafe(hero));assertSame(dew,heap.peek());assertEquals("0/20",skin.status());assertEquals(0,hero.cooldown(),0);}
    @Test public void optionAllowsCollectionWithVisibleEnemy(){SPDSettings.autoCollectDewWithEnemies(true);enemy(true);offer();assertTrue(pickup.collectIfSafe(hero));assertEquals(1,hero.cooldown(),0);}
    @Test public void unseenEnemyDoesNotRevealItselfByBlockingCollection(){enemy(false);offer();assertTrue(pickup.collectIfSafe(hero));}
    @Test public void allyDoesNotBlockCollection(){Rat r=enemy(true);r.alignment=Char.Alignment.ALLY;offer();assertTrue(pickup.collectIfSafe(hero));}
    @Test public void freshlyRevealedEnemyAfterMovementCancelsOneShotRequest(){offer();Rat r=enemy(false);level.heroFOV[r.pos]=true;assertFalse(pickup.collectIfSafe(hero));level.heroFOV[r.pos]=false;assertFalse(pickup.collectIfSafe(hero));assertSame(dew,heap.peek());}
    @Test public void fullWaterskinLeavesDewAndDoesNotAutoDrink(){skin.fill();offer();assertFalse(pickup.collectIfSafe(hero));assertEquals("20/20",skin.status());assertEquals(10,hero.HP);assertEquals(0,hero.cooldown(),0);assertSame(dew,heap.peek());}
    @Test public void reachingFullBetweenOfferAndCollectionIsSafe(){offer();skin.fill();assertFalse(pickup.collectIfSafe(hero));assertSame(dew,heap.peek());assertEquals(10,hero.HP);}
    @Test public void nineteenDropsBecomeExactlyTwentyAndVolumeSurvivesSaving(){for(int i=0;i<19;i++)skin.collectDew(new Dewdrop());offer();assertTrue(pickup.collectIfSafe(hero));assertTrue(skin.isFull());Bundle b=new Bundle();skin.storeInBundle(b);Waterskin loaded=new Waterskin();loaded.restoreFromBundle(b);assertEquals("20/20",loaded.status());}
    @Test public void missingOrLostWaterskinNeverConsumesDew(){hero.belongings.backpack.items.clear();offer();assertFalse(pickup.collectIfSafe(hero));hero.belongings.backpack.items.add(skin);hero.belongings.lostInventory(true);offer();assertFalse(pickup.collectIfSafe(hero));assertSame(dew,heap.peek());}
    @Test public void disabledCollectionAndChangedPreferenceLeaveGroundItem(){SPDSettings.autoCollectGrassDew(false);offer();assertFalse(pickup.collectIfSafe(hero));SPDSettings.autoCollectGrassDew(true);offer();SPDSettings.autoCollectGrassDew(false);assertFalse(pickup.collectIfSafe(hero));assertSame(dew,heap.peek());}
    @Test public void preferencesSurviveDiskReloadIndependently(){SPDSettings.set(new HeadlessPreferences(prefsFile));assertTrue(SPDSettings.autoCollectGrassDew());assertFalse(SPDSettings.autoCollectDewWithEnemies());SPDSettings.autoCollectGrassDew(false);SPDSettings.autoCollectDewWithEnemies(true);SPDSettings.set(new HeadlessPreferences(prefsFile));assertFalse(SPDSettings.autoCollectGrassDew());assertTrue(SPDSettings.autoCollectDewWithEnemies());}
    @Test public void onlyDewIsRemovedFromMixedPile(){Food food=new Food();heap.items.addFirst(food);offer();assertTrue(pickup.collectIfSafe(hero));assertSame(heap,level.heaps.get(12));assertSame(food,heap.peek());assertEquals(1,heap.size());}
    @Test public void closedOrSaleHeapIsNeverCollected(){for(Heap.Type type:new Heap.Type[]{Heap.Type.CHEST,Heap.Type.FOR_SALE,Heap.Type.LOCKED_CHEST}){heap.type=type;offer();assertFalse(pickup.collectIfSafe(hero));}assertSame(dew,heap.peek());}
    @Test public void changedHeroPositionLevelOrHeapInvalidatesRequest(){offer();hero.pos=11;assertFalse(pickup.collectIfSafe(hero));hero.pos=12;offer();Dungeon.level=new TestLevel();assertFalse(pickup.collectIfSafe(hero));Dungeon.level=level;offer();heap.items.remove(dew);heap.items.add(new Dewdrop());assertFalse(pickup.collectIfSafe(hero));assertEquals("0/20",skin.status());}
    @Test public void newManualActionSupersedesPickupAndTravelActionSurvivesIt(){offer();hero.curAction=new HeroAction.Move(13);assertFalse(pickup.collectIfSafe(hero));offer();HeroAction travel=hero.curAction;assertTrue(pickup.collectIfSafe(hero));assertSame(travel,hero.curAction);}
    @Test public void paralysisAndDeathDoNotExecuteQueuedPickup(){offer();hero.paralysed=1;assertFalse(pickup.collectIfSafe(hero));hero.paralysed=0;offer();hero.HP=0;assertFalse(pickup.collectIfSafe(hero));assertSame(dew,heap.peek());}
    @Test public void bookkeepingAndPickupUseNoAdditionalRandomDraws(){Random.pushGenerator(459);long expected=Random.Long();Random.popGenerator();Random.pushGenerator(459);try{offer();assertTrue(pickup.collectIfSafe(hero));assertEquals(expected,Random.Long());}finally{Random.popGenerator();}}
    private com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed seed(){
        com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed item=new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed();heap.items.addFirst(item);return item;
    }
    private com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression stone(){
        com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression item=new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression();heap.items.addFirst(item);return item;
    }
    @Test public void preexistingDewOnOrdinaryFloorIsEligibleAfterWalking(){
        level.map[12]=com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY;
        pickup.offerDewOnStep(hero,level,12,heap);hero.spend(1);
        assertTrue(pickup.collectIfSafe(hero));assertEquals(2,hero.cooldown(),0);assertEquals("1/20",skin.status());
    }
    @Test public void ordinaryDewStepDoesNotVacuumSeedsOrStonesFromUnrelatedPiles(){
        Item seed=seed(),stone=stone();pickup.offerDewOnStep(hero,level,12,heap);
        assertTrue(pickup.collectIfSafe(hero));assertFalse(pickup.collectIfSafe(hero));
        assertTrue(heap.items.contains(seed));assertTrue(heap.items.contains(stone));
    }
    @Test public void grassSeedAndDewBothCollectAsSeparateNormalActions(){
        Item seed=seed();offer();hero.spend(1);
        assertTrue(pickup.collectIfSafe(hero));assertEquals(2,hero.cooldown(),0);assertTrue(hero.belongings.contains(seed));
        assertTrue(pickup.collectIfSafe(hero));assertEquals(3,hero.cooldown(),0);assertEquals("1/20",skin.status());
        assertNull(level.heaps.get(12));assertFalse(pickup.collectIfSafe(hero));assertEquals(2,hero.nextCalls);
    }
    @Test public void petrifiedRunestoneAndDewBothCollectAsSeparateNormalActions(){
        Item stone=stone();offer();hero.spend(1);
        assertTrue(pickup.collectIfSafe(hero));assertTrue(hero.belongings.contains(stone));
        assertTrue(pickup.collectIfSafe(hero));assertEquals(3,hero.cooldown(),0);assertEquals("1/20",skin.status());
    }
    @Test public void aSecondGrassDropAndStepOfferDoNotReplaceOrDuplicateTheFirst(){
        heap.items.remove(dew);Item seed=seed();offer();heap.items.add(dew);offer();
        pickup.offerDewOnStep(hero,level,12,heap);
        assertTrue(pickup.collectIfSafe(hero));assertTrue(pickup.collectIfSafe(hero));assertFalse(pickup.collectIfSafe(hero));
        assertTrue(hero.belongings.contains(seed));assertEquals("1/20",skin.status());assertEquals(2,hero.cooldown(),0);
    }
    @Test public void seedAndStoneDoNotRequireAnyDewToDrop(){
        heap.items.remove(dew);Item seed=seed(),stone=stone();offer();
        assertTrue(pickup.collectIfSafe(hero));assertTrue(pickup.collectIfSafe(hero));assertFalse(pickup.collectIfSafe(hero));
        assertTrue(hero.belongings.contains(seed));assertTrue(hero.belongings.contains(stone));assertEquals("0/20",skin.status());
    }
    @Test public void fullWaterskinStillAllowsGrassSeedPickupWithoutHealing(){
        skin.fill();Item seed=seed();offer();assertTrue(pickup.collectIfSafe(hero));assertFalse(pickup.collectIfSafe(hero));
        assertTrue(hero.belongings.contains(seed));assertSame(dew,heap.peek());assertEquals(10,hero.HP);assertEquals("20/20",skin.status());
    }
    @Test public void missingWaterskinStillAllowsRunestoneButNeverConsumesDew(){
        hero.belongings.backpack.items.clear();Item stone=stone();offer();
        assertTrue(pickup.collectIfSafe(hero));assertFalse(pickup.collectIfSafe(hero));assertTrue(hero.belongings.contains(stone));
        assertTrue(heap.items.contains(dew));assertEquals(10,hero.HP);
    }
    private void fillBag(){while(hero.belongings.backpack.items.size()<hero.belongings.backpack.capacity())hero.belongings.backpack.items.add(new Food());}
    @Test public void fullInventoryLeavesSeedWithoutSpendingAPickupTurnOrRetrying(){
        heap.items.remove(dew);Item seed=seed();fillBag();offer();
        assertFalse(pickup.collectIfSafe(hero));assertTrue(heap.items.contains(seed));assertEquals(0,hero.cooldown(),0);
        hero.belongings.backpack.items.remove(hero.belongings.backpack.items.size()-1);
        assertFalse(pickup.collectIfSafe(hero));assertTrue(heap.items.contains(seed));
    }
    @Test public void fullInventoryStillAllowsMergingAnExistingSeedStack(){
        com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed existing=new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed();existing.quantity(4);
        hero.belongings.backpack.items.add(existing);fillBag();heap.items.remove(dew);Item incoming=seed();incoming.quantity(3);offer();
        assertTrue(pickup.collectIfSafe(hero));assertEquals(7,existing.quantity());assertNull(level.heaps.get(12));
        assertEquals(hero.belongings.backpack.capacity(),hero.belongings.backpack.items.size());
    }
    @Test public void fullInventoryDoesNotStopDewFromFillingTheWaterskin(){
        Item seed=seed();fillBag();offer();assertTrue(pickup.collectIfSafe(hero));assertEquals("1/20",skin.status());
        assertTrue(heap.items.contains(seed));assertEquals(1,hero.cooldown(),0);
    }
    @Test public void visibleEnemyBlocksBothGrassLootTypesAndTheWholeQueue(){
        heap.items.remove(dew);Item seed=seed(),stone=stone();offer();enemy(true);
        assertFalse(pickup.collectIfSafe(hero));assertEquals(0,hero.cooldown(),0);assertTrue(heap.items.contains(seed));assertTrue(heap.items.contains(stone));
    }
    @Test public void enemyAppearingBetweenPickupsStopsTheRemainingItem(){
        Item seed=seed();offer();assertTrue(pickup.collectIfSafe(hero));assertTrue(hero.belongings.contains(seed));enemy(true);
        assertFalse(pickup.collectIfSafe(hero));assertTrue(heap.items.contains(dew));assertEquals("0/20",skin.status());assertEquals(1,hero.cooldown(),0);
    }
    @Test public void existingEnemyOverrideAllowsBothKindsOfGrassLoot(){
        SPDSettings.autoCollectDewWithEnemies(true);enemy(true);Item seed=seed();offer();
        assertTrue(pickup.collectIfSafe(hero));assertTrue(pickup.collectIfSafe(hero));assertTrue(hero.belongings.contains(seed));assertEquals("1/20",skin.status());
    }
    @Test public void savedMasterOffDisablesDewSeedsAndStonesTogether(){
        Item seed=seed(),stone=stone();SPDSettings.autoCollectGrassDew(false);SPDSettings.set(new HeadlessPreferences(prefsFile));offer();
        assertFalse(pickup.collectIfSafe(hero));assertTrue(heap.items.contains(seed));assertTrue(heap.items.contains(stone));assertTrue(heap.items.contains(dew));
    }
    @Test public void nonMovementGrassNotificationDoesNotQueueAForcedPickup()throws Exception{
        hero.queueGrassDew(level,12,heap);
        Field field=Hero.class.getDeclaredField("grassDewPickup");field.setAccessible(true);
        assertFalse(((GrassDewPickup)field.get(hero)).collectIfSafe(hero));assertSame(dew,heap.peek());
    }
    @Test public void emptyOrFinishedRequestsReleaseTheirLevelAndHeapReferences()throws Exception{
        pickup.offerDewOnStep(hero,level,12,heap);assertTrue(pickup.collectIfSafe(hero));
        for(String name:new String[]{"level","heap","sourceAction"}){Field field=GrassDewPickup.class.getDeclaredField(name);field.setAccessible(true);assertNull(name,field.get(pickup));}
    }

    public static class WalkingSprite extends com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite {
        @Override public void move(int from,int to){}
        @Override public void place(int cell){}
        @Override public void idle(){}
        @Override public void sprint(float speed){}
    }
    private void prepareWalkingHero()throws Exception{
        level.blobs=new java.util.HashMap<>();level.traps=new com.watabou.utils.SparseArray<>();level.plants=new com.watabou.utils.SparseArray<>();
        java.lang.reflect.Field u=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");u.setAccessible(true);Object unsafe=u.get(null);
        hero.sprite=(WalkingSprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,WalkingSprite.class);
        hero.heroClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.WARRIOR;
        java.util.Arrays.fill(level.map,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);
        java.util.Arrays.fill(level.passable,true);java.util.Arrays.fill(level.heroFOV,true);java.util.Arrays.fill(level.visited,true);
        hero.fieldOfView=level.heroFOV;hero.pos=11;hero.curAction=new HeroAction.Move(12);
    }
    @Test public void realVoluntaryStepQueuesPreexistingDewAfterItsMovementTurn()throws Exception{
        prepareWalkingHero();java.lang.reflect.Method closer=Hero.class.getDeclaredMethod("getCloser",int.class);closer.setAccessible(true);
        assertTrue((Boolean)closer.invoke(hero,12));assertEquals(12,hero.pos);assertEquals(1,hero.cooldown(),0);assertEquals("0/20",skin.status());
        Field f=Hero.class.getDeclaredField("grassDewPickup");f.setAccessible(true);GrassDewPickup queued=(GrassDewPickup)f.get(hero);
        assertTrue(queued.collectIfSafe(hero));assertEquals(2,hero.cooldown(),0);assertEquals("1/20",skin.status());
    }
    @Test public void forcedMovementDoesNotQueueExistingDew()throws Exception{
        prepareWalkingHero();hero.move(12,false);
        Field f=Hero.class.getDeclaredField("grassDewPickup");f.setAccessible(true);
        assertFalse(((GrassDewPickup)f.get(hero)).collectIfSafe(hero));assertSame(dew,heap.peek());assertEquals("0/20",skin.status());
    }

}
