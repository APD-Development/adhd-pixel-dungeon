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
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RatKing;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.RatKingRoom;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.watabou.noosa.*;
import com.watabou.utils.*;
import com.watabou.utils.Random;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class RatKingRoomAndSearchTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Hero oldHero;private Level oldLevel;private int oldDepth,oldBranch;private boolean oldIntro;
    private Preferences oldPrefs;private java.lang.reflect.Field selectorField;private Object oldSelector;private Bundle oldRules;
    private Fixture level;private QuietHero hero;
    public static class Fixture extends SewerBossLevel {
        public void room(Room room){rooms=new ArrayList<>();rooms.add(room);}
        @Override public Heap drop(Item item,int cell){Heap heap=heaps.get(cell);if(heap==null){heap=new Heap();heap.pos=cell;heaps.put(cell,heap);}heap.drop(item);return heap;}
        @Override public void discover(int cell){Level.set(cell,Terrain.discover(map[cell]),this);}
    }
    public static class QuietSprite extends HeroSprite{
        @Override public void operate(int p){} @Override public void showStatus(int color,String s,Object...args){} @Override public void idle(){}
    }
    public static class QuietHero extends Hero{
        @Override public void finishRatQuickSearch(){ready=true;curAction=null;}
        @Override public boolean act(){if(curAction instanceof RatKingQuickSearch.Action){RatKingQuickSearch.advance(this,(RatKingQuickSearch.Action)curAction);return true;}Actor.keepActorThreadAlive=false;return true;}
    }
    private static class HeadlessGame extends Game{
        HeadlessGame(){super(sceneClass,Game.platform);requestedReset=false;}
        static Class<? extends Scene> sceneType(){return sceneClass;}
        static void restoreType(Class<? extends Scene> value){sceneClass=value;}
    }
    @Before public void setup()throws Exception{
        HeadlessTestSupport.start();oldHero=Dungeon.hero;oldLevel=Dungeon.level;oldDepth=Dungeon.depth;oldBranch=Dungeon.branch;oldIntro=SPDSettings.intro();
        java.lang.reflect.Field prefs=GameSettings.class.getDeclaredField("prefs");prefs.setAccessible(true);oldPrefs=(Preferences)prefs.get(null);
        GameSettings.set(new HeadlessPreferences(new FileHandle(temp.newFile("settings.xml"))));SPDSettings.intro(false);
        oldRules=new Bundle();Dungeon.apdStoreCustomRules(oldRules);Dungeon.apdRestoreCustomRules(new Bundle());Actor.clear();
        Dungeon.depth=5;Dungeon.branch=0;level=floor();Dungeon.level=level;
        hero=new QuietHero();hero.heroClass=HeroClass.WARRIOR;hero.pos=2+6*14;hero.fieldOfView=level.heroFOV;hero.ready=true;Dungeon.hero=hero;
        java.lang.reflect.Field uf=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");uf.setAccessible(true);Object unsafe=uf.get(null);
        hero.sprite=(QuietSprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,QuietSprite.class);
        selectorField=GameScene.class.getDeclaredField("cellSelector");selectorField.setAccessible(true);oldSelector=selectorField.get(null);selectorField.set(null,unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,CellSelector.class));
    }
    @After public void cleanup()throws Exception{Actor.keepActorThreadAlive=false;Actor.clear();selectorField.set(null,oldSelector);GameSettings.set(oldPrefs);SPDSettings.intro(oldIntro);Dungeon.apdRestoreCustomRules(oldRules);Dungeon.hero=oldHero;Dungeon.level=oldLevel;Dungeon.depth=oldDepth;Dungeon.branch=oldBranch;}
    private Fixture floor(){Fixture f=new Fixture();f.setSize(14,14);f.mobs=new HashSet<>();f.heaps=new SparseArray<>();f.traps=new SparseArray<>();f.plants=new SparseArray<>();f.blobs=new HashMap<>();f.customTiles=new ArrayList<>();f.customTerrain=new ArrayList<>();f.customWalls=new ArrayList<>();Arrays.fill(f.map,Terrain.WALL);for(int x=1;x<=5;x++)f.map[x+6*14]=Terrain.EMPTY;RatKingRoom room=new RatKingRoom();room.set(6,3,12,9);f.room(room);f.map[6+6*14]=Terrain.SECRET_DOOR;f.buildFlagMaps();Arrays.fill(f.visited,true);Arrays.fill(f.heroFOV,true);return f;}
    private int door(){return 6+6*14;}
    private void enable(){SPDSettings.ratKingQuickSearch(true);level.apdGooDefeated=true;}
    private int gold(Fixture l){int n=0;for(Heap heap:l.heaps.valueList())for(Item item:heap.items)if(item instanceof Gold)n+=item.quantity();return n;}
    private Fixture painted(int depth,int side,int offset,long seed){
        Dungeon.depth=depth;Fixture f=floor();Dungeon.level=f;int[][] points={{2,2+offset},{8,2+offset},{2+offset,2},{2+offset,8}};int[] point=points[side];
        RatKingRoom room=new RatKingRoom(){private Room.Door entry=new Room.Door(point[0],point[1]);@Override public Room.Door entrance(){return entry;}};room.set(2,2,8,8);f.room(room);
        Random.pushGenerator(seed);try{room.paint(f);}finally{Random.popGenerator();}return f;
    }
    @Test public void internalRoomSizeWasAndRemainsExactlyFive(){RatKingRoom r=new RatKingRoom();assertEquals(7,r.minWidth());assertEquals(7,r.maxWidth());assertEquals(7,r.minHeight());assertEquals(7,r.maxHeight());}
    @Test public void allOrientationsHaveExactlyOneUnlockedGoldChestAtThreeThree(){for(int side=0;side<4;side++)for(int offset=1;offset<=5;offset++){Fixture f=painted(5,side,offset,742);assertEquals(1,f.heaps.valueList().size());Heap chest=f.heaps.get(5+5*14);assertNotNull(chest);assertEquals(Heap.Type.CHEST,chest.type);assertEquals(1,chest.items.size());assertTrue(chest.peek() instanceof Gold);assertTrue(gold(f)>0);}}
    @Test public void originalTotalGoldIsConservedAcrossSeedsAndOrientations(){for(int side=0;side<4;side++)for(int seed=0;seed<64;seed++){Fixture original=painted(6,side,3,seed),changed=painted(5,side,3,seed);assertEquals(gold(original),gold(changed));}}
    @Test public void consolidationConsumesExactlyTheOriginalRandomDraws(){for(int side=0;side<4;side++){Random.pushGenerator(900+side);try{Fixture a=floor();Dungeon.depth=6;Dungeon.level=a;final int x=side<2?(side==0?2:8):5,y=side>=2?(side==2?2:8):5;RatKingRoom r=new RatKingRoom(){@Override public Room.Door entrance(){return new Room.Door(x,y);}};r.set(2,2,8,8);r.paint(a);long expected=Random.Long();Random.popGenerator();Random.pushGenerator(900+side);Fixture b=floor();Dungeon.depth=5;Dungeon.level=b;r.paint(b);assertEquals(expected,Random.Long());}finally{Random.popGenerator();}}}
    @Test public void kingIsFurthestReachableAndDoesNotStandOnCenterChest(){for(int side=0;side<4;side++)for(int offset=1;offset<=5;offset++){Fixture f=painted(5,side,offset,100);RatKing king=null;for(Mob m:f.mobs)if(m instanceof RatKing)king=(RatKing)m;assertNotNull(king);int[][] entries={{2,2+offset},{8,2+offset},{2+offset,2},{2+offset,8}};int entry=entries[side][0]+entries[side][1]*14;assertEquals(RatKingRoom.farthestWalkable(f,((RegularLevel)f).room(RatKingRoom.class),entry),king.pos);assertNotEquals(5+5*14,king.pos);assertTrue((Terrain.flags[f.map[king.pos]]&Terrain.PASSABLE)!=0);}}
    @Test public void otherFloorsKeepOriginalScatteredGoldAndCenteredKing(){for(int depth:new int[]{1,4,6,10,20,25}){Fixture f=painted(depth,0,3,77);assertTrue(f.heaps.valueList().size()>1);for(Heap h:f.heaps.valueList())assertEquals(Heap.Type.HEAP,h.type);for(Mob m:f.mobs)if(m instanceof RatKing)assertEquals(5+5*14,m.pos);}}
    @Test public void quickPreferenceIsOffByDefaultAndPersistsAcrossRestart(){assertFalse(SPDSettings.ratKingQuickSearch());SPDSettings.ratKingQuickSearch(true);Preferences current;try{java.lang.reflect.Field p=GameSettings.class.getDeclaredField("prefs");p.setAccessible(true);current=(Preferences)p.get(null);current.flush();}catch(Exception e){throw new AssertionError(e);}GameSettings.set(new HeadlessPreferences(new FileHandle(temp.getRoot().toPath().resolve("settings.xml").toFile())));assertTrue(SPDSettings.ratKingQuickSearch());}
    @Test public void roomRedesignDoesNotDependOnQuickSearchSetting(){SPDSettings.ratKingQuickSearch(false);assertEquals(1,painted(5,0,3,1).heaps.valueList().size());}
    @Test public void unlockedBeforeFightDoesNotMeanGooWasDefeated(){SPDSettings.ratKingQuickSearch(true);level.locked=false;assertFalse(RatKingQuickSearch.unlocked(level));level.apdGooDefeated=true;level.mobs.add(new Goo());assertFalse(RatKingQuickSearch.unlocked(level));}
    @Test public void enabledDefeatedGooUnlocksOnlyRealFloorFiveMainBranch(){enable();for(int depth=1;depth<=26;depth++){Dungeon.depth=depth;for(int branch=0;branch<=1;branch++){Dungeon.branch=branch;assertEquals(depth==5&&branch==0,RatKingQuickSearch.unlocked(level));}}}
    @Test public void offSettingKeepsOriginalSearchAtFloorFive(){level.apdGooDefeated=true;float before=hero.cooldown();assertFalse(SPDSettings.ratKingQuickSearch());hero.search(true);assertNull(hero.ratSearch);assertEquals(2,hero.cooldown()-before,0.001f);assertEquals(Terrain.SECRET_DOOR,level.map[door()]);}
    @Test public void floorSixNeverCreatesAPaymentEvenWhenToggleAndDefeatAreTrue(){enable();Dungeon.depth=6;float before=hero.cooldown();hero.search(true);assertNull(hero.ratSearch);assertEquals(2,hero.cooldown()-before,0.001f);assertEquals(Terrain.SECRET_DOOR,level.map[door()]);}
    @Test public void knownEntrancePreservesNormalSearch(){enable();level.map[door()]=Terrain.DOOR;level.buildFlagMaps();float before=hero.cooldown();hero.search(true);assertNull(hero.ratSearch);assertEquals(2,hero.cooldown()-before,0.001f);}
    @Test public void minimumOrdinaryWalkingPlusSearchUsesActualSearchReach(){enable();assertEquals(5,RatKingQuickSearch.quote(hero,level,door()),0.001f);hero.heroClass=HeroClass.ROGUE;assertEquals(4,RatKingQuickSearch.quote(hero,level,door()),0.001f);}
    @Test public void weightedSolverFindsTheMinimumLegalRouteInsteadOfManhattanDistance(){boolean[] legal=new boolean[25];Arrays.fill(legal,true);legal[7]=false;assertEquals(6,RatKingQuickSearch.minimum(5,legal,0,(a,b,t)->1,(p,t)->p==24),0.001f);}
    @Test public void weightedSolverAccountsForDifferentLegitimateStepCosts(){boolean[] legal=new boolean[9];Arrays.fill(legal,true);float cost=RatKingQuickSearch.minimum(3,legal,3,(a,b,t)->b==4?100:1,(p,t)->p==5);assertEquals(4,cost,0.001f);}
    @Test public void noRouteCannotRevealOrChargeAnInventedPrice(){Arrays.fill(level.passable,false);level.passable[hero.pos]=true;enable();assertTrue(Float.isInfinite(RatKingQuickSearch.quote(hero,level,door())));assertTrue(RatKingQuickSearch.start(hero));assertNull(hero.ratSearch);assertEquals(0,hero.cooldown(),0.001f);}
    @Test public void purePlanningDoesNotConsumeRngOrMoveOrRevealAnything(){enable();int position=hero.pos;int[] map=level.map.clone();Random.pushGenerator(171);long expected=Random.Long();Random.popGenerator();Random.pushGenerator(171);try{RatKingQuickSearch.quote(hero,level,door());assertEquals(expected,Random.Long());}finally{Random.popGenerator();}assertEquals(position,hero.pos);assertArrayEquals(map,level.map);}
    @Test public void hasteExpiryIsIncludedInsteadOfAssumingItLastsForever(){enable();hero.pos=1+6*14;Buff.affect(hero,Haste.class,1f);assertEquals(4,RatKingQuickSearch.quote(hero,level,door()),0.001f);}
    @Test public void ordinarySpeedBuffAlsoScalesTheManualSearchCost(){enable();Buff.affect(hero,Speed.class,20f);assertEquals(2.5f,RatKingQuickSearch.quote(hero,level,door()),0.001f);}
    @Test public void unpriceableEffectsDoNotProduceAFalselyCheapQuote(){enable();Buff.affect(hero,Cripple.class,10f);assertTrue(RatKingQuickSearch.start(hero));assertNull(hero.ratSearch);assertEquals(0,hero.cooldown(),0.001f);}
    @Test public void paymentAndPreferenceAreNotCustomDungeonRules(){enable();Bundle before=new Bundle();Dungeon.apdStoreCustomRules(before);RatKingQuickSearch.start(hero);Bundle after=new Bundle();Dungeon.apdStoreCustomRules(after);assertEquals(before.toString(),after.toString());assertEquals(18,com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.values().length);}
    @Test public void partialPaymentRoundTripsWithoutAnActiveAction(){enable();RatKingQuickSearch.Payment p=new RatKingQuickSearch.Payment(door(),5,hero.pos,14,14);p.remaining=3;hero.ratSearch=p;Bundle save=new Bundle();RatKingQuickSearch.store(hero,save);hero.ratSearch=null;RatKingQuickSearch.restore(hero,save);assertEquals(3,hero.ratSearch.remaining,0.001f);assertNull(hero.curAction);}
    // A partially written payment must never become a zero-cell / zero-time action.
    // All required fields have existed since this payment format was introduced;
    // only search_cost is optional for older saves.
    private Bundle paymentMissing(String omitted) {
        Bundle payment=new Bundle();
        if(!"door".equals(omitted))payment.put("door",door());
        if(!"anchor".equals(omitted))payment.put("anchor",hero.pos);
        if(!"width".equals(omitted))payment.put("width",14);
        if(!"height".equals(omitted))payment.put("height",14);
        if(!"total".equals(omitted))payment.put("total",5f);
        if(!"search_cost".equals(omitted))payment.put("search_cost",2f);
        if(!"remaining".equals(omitted))payment.put("remaining",3f);
        if(!"hunger".equals(omitted))payment.put("hunger",true);
        Bundle save=new Bundle();
        save.put("apd_rat_search_payment_v1",payment);
        return save;
    }
    private void assertInvalidPaymentIsDiscarded(String field) {
        hero.ratSearch=new RatKingQuickSearch.Payment(door(),5f,hero.pos,14,14);
        float before=hero.cooldown();
        int[] terrain=level.map.clone();
        RatKingQuickSearch.restore(hero,paymentMissing(field));
        assertNull("Incomplete "+field+" must not restore pending payment",hero.ratSearch);
        assertNull("Loading memory never starts an action",hero.curAction);
        assertEquals(before,hero.cooldown(),0.001f);
        assertArrayEquals(terrain,level.map);
    }
    @Test public void missingDoorCannotRestorePaidQuickSearch(){assertInvalidPaymentIsDiscarded("door");}
    @Test public void missingAnchorCannotRestorePaidQuickSearch(){assertInvalidPaymentIsDiscarded("anchor");}
    @Test public void missingRemainingCannotRestoreFreeCompletion(){assertInvalidPaymentIsDiscarded("remaining");}
    @Test public void missingHungerStateCannotRestorePaidQuickSearch(){assertInvalidPaymentIsDiscarded("hunger");}
    @Test public void legacyPaidSearchWithoutOptionalSearchCostStillRestores(){
        hero.ratSearch=null;
        RatKingQuickSearch.restore(hero,paymentMissing("search_cost"));
        assertNotNull(hero.ratSearch);
        assertEquals(door(),hero.ratSearch.door);
        assertEquals(hero.pos,hero.ratSearch.anchor);
        assertEquals(5f,hero.ratSearch.total,0.001f);
        assertEquals(3f,hero.ratSearch.remaining,0.001f);
        assertEquals(2f,hero.ratSearch.searchCost,0.001f);
        assertTrue(hero.ratSearch.hungerCharged);
        assertNull(hero.curAction);
    }
    @Test public void malformedOrLegacySaveHasNoInventedPaidSearch(){RatKingQuickSearch.restore(hero,new Bundle());assertNull(hero.ratSearch);Bundle b=new Bundle();b.put("apd_rat_search_payment_v1",new Bundle());RatKingQuickSearch.restore(hero,b);assertNull(hero.ratSearch);}
    @Test public void quickSearchRevealsOnlyTargetAfterFullActorClockPaymentAndHungerRuns()throws Exception{runClock(false);}
    @Test public void timedStatusExpiresDuringRealSearchPayment()throws Exception{runClock(true);}
    @Test public void partialPaymentDoesNotRevealBeforeTimeHasActuallyElapsed(){enable();RatKingQuickSearch.start(hero);RatKingQuickSearch.Action action=(RatKingQuickSearch.Action)hero.curAction;RatKingQuickSearch.advance(hero,action);assertEquals(Terrain.SECRET_DOOR,level.map[door()]);assertTrue(action.payment.remaining>0);assertEquals(1,hero.cooldown(),0.001f);}
    @Test public void combatInterruptionKeepsElapsedCostButDiscardsAStaleQuickSearchQuote(){enable();RatKingQuickSearch.start(hero);RatKingQuickSearch.advance(hero,(RatKingQuickSearch.Action)hero.curAction);float paid=hero.cooldown();hero.interrupt();assertNull(hero.ratSearch);assertNull(hero.curAction);assertEquals(paid,hero.cooldown(),0.001f);assertEquals(Terrain.SECRET_DOOR,level.map[door()]);}
    @Test public void disablingDuringPaymentDoesNotRevealOrChargeMore(){enable();RatKingQuickSearch.start(hero);RatKingQuickSearch.Action action=(RatKingQuickSearch.Action)hero.curAction;SPDSettings.ratKingQuickSearch(false);RatKingQuickSearch.advance(hero,action);assertNull(hero.ratSearch);assertEquals(0,hero.cooldown(),0.001f);assertEquals(Terrain.SECRET_DOOR,level.map[door()]);}
    private void runClock(boolean status)throws Exception{
        enable();level.map[1+5*14]=Terrain.SECRET_DOOR;level.buildFlagMaps();int unrelated=1+5*14;int start=hero.pos;Hunger hunger=Buff.affect(hero,Hunger.class);
        if(status)Buff.affect(hero,Haste.class,1f);
        assertTrue(RatKingQuickSearch.start(hero));float cost=hero.ratSearch.total;assertEquals(Terrain.SECRET_DOOR,level.map[door()]);
        Game original=Game.instance;Class<? extends Scene> previous=HeadlessGame.sceneType();new HeadlessGame();
        AtomicReference<Throwable> error=new AtomicReference<>();Actor.add(hero);Actor.keepActorThreadAlive=true;
        Thread worker=new Thread(Actor::process,"rat-search-clock-regression");worker.setDaemon(true);worker.setUncaughtExceptionHandler((t,e)->error.set(e));
        try{worker.start();worker.join(4000);assertNull(error.get());assertFalse("actor payment terminated",worker.isAlive());assertEquals(cost,Actor.now(),0.001f);assertEquals(start,hero.pos);assertEquals(Terrain.DOOR,level.map[door()]);assertEquals(Terrain.SECRET_DOOR,level.map[unrelated]);assertNull(hero.ratSearch);Bundle state=new Bundle();hunger.storeInBundle(state);assertTrue("normal hunger and manual-search surcharge ran",state.getFloat("level")>=cost+3);if(status)assertNull(hero.buff(Haste.class));}
        finally{Actor.keepActorThreadAlive=false;worker.interrupt();synchronized(worker){worker.notifyAll();}worker.join(1000);Game.instance=original;HeadlessGame.restoreType(previous);}
    }
}
