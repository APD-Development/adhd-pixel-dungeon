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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.watabou.utils.*;
import com.watabou.utils.Random;
import com.watabou.input.GameAction;
import org.junit.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

/** Real search, map discovery, cached paths, movement and trap triggering; only render effects are stubbed. */
public class TrapDiscoveryMovementTest {
    public static class Empty extends Level {
        protected boolean build(){return true;} protected void createMobs(){} protected void createItems(){}
    }
    public static class CountingTrap extends Trap {
        int activations;
        @Override public void activate(){activations++;Dungeon.hero.HP-=3;}
    }
    public static class Player extends Hero {
        ConfirmationDecision warning;
        @Override public void next(){}
        @Override protected void showSearchDiscoveryTile(int cell,int oldValue){}
        @Override protected void showSearchDiscoveryParticles(int cell){}
        @Override protected void requestKnownTrapConfirmation(Trap trap,ConfirmationDecision decision){warning=decision;}
        void route(PathFinder.Path value){path=value;} PathFinder.Path route(){return path;}
    }
    static class QuietSprite extends HeroSprite {
        void attach(Hero hero){ch=hero;hero.sprite=this;visible=false;}
        @Override public void move(int from,int to){} @Override public void place(int cell){}
        @Override public void operate(int cell){} @Override public void idle(){}
        @Override public void showStatus(int color,String text,Object...args){}
        @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){}
    }
    private Level oldLevel;private Hero oldHero;private int oldDepth,oldBranch,oldChallenges;private Bundle oldRules;
    private Object oldScene,oldSelector;private Preferences oldPrefs;private Field sceneField,selectorField;
    private Empty level;private Player hero;private CellSelector selector;private int start,target;
    private Method closer;
    @Before public void setup()throws Exception{
        HeadlessTestSupport.start();oldLevel=Dungeon.level;oldHero=Dungeon.hero;oldDepth=Dungeon.depth;oldBranch=Dungeon.branch;oldChallenges=Dungeon.challenges;Dungeon.challenges=0;oldRules=new Bundle();Dungeon.apdStoreCustomRules(oldRules);Dungeon.apdRestoreCustomRules(new Bundle());
        Field prefs=GameSettings.class.getDeclaredField("prefs");prefs.setAccessible(true);oldPrefs=(Preferences)prefs.get(null);
        GameSettings.set(new HeadlessPreferences(new FileHandle(java.nio.file.Files.createTempDirectory("apd-trap-prefs-").resolve("settings.xml").toFile())));
        sceneField=GameScene.class.getDeclaredField("scene");sceneField.setAccessible(true);oldScene=sceneField.get(null);sceneField.set(null,null);
        selectorField=GameScene.class.getDeclaredField("cellSelector");selectorField.setAccessible(true);oldSelector=selectorField.get(null);
        Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);selector=(CellSelector)((sun.misc.Unsafe)uf.get(null)).allocateInstance(CellSelector.class);selectorField.set(null,selector);
        level=new Empty();level.setSize(33,33);level.transitions=new ArrayList<>();level.mobs=new HashSet<>();level.traps=new SparseArray<>();level.heaps=new SparseArray<>();level.plants=new SparseArray<>();level.blobs=new HashMap<>();level.customTiles=new ArrayList<>();level.customWalls=new ArrayList<>();
        Arrays.fill(level.map,Terrain.EMPTY);for(int i=0;i<33;i++){level.map[i]=level.map[32*33+i]=level.map[33*i]=level.map[33*i+32]=Terrain.WALL;}level.buildFlagMaps();Arrays.fill(level.visited,true);Arrays.fill(level.heroFOV,true);
        Dungeon.level=level;Dungeon.depth=3;Dungeon.branch=0;hero=new Player();hero.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(hero);hero.HP=hero.HT=100;hero.pos=start=16*33+12;target=start+7;hero.ready=true;hero.fieldOfView=level.heroFOV;Dungeon.hero=hero;new QuietSprite().attach(hero);
        SPDSettings.confirmKnownTraps(false);Buff.affect(hero,Foresight.class);closer=Hero.class.getDeclaredMethod("getCloser",int.class);closer.setAccessible(true);
    }
    @After public void cleanup()throws Exception{GameSettings.set(oldPrefs);Dungeon.level=oldLevel;Dungeon.hero=oldHero;Dungeon.depth=oldDepth;Dungeon.branch=oldBranch;Dungeon.challenges=oldChallenges;Dungeon.apdRestoreCustomRules(oldRules);sceneField.set(null,oldScene);selectorField.set(null,oldSelector);}
    private CountingTrap hidden(int cell){CountingTrap trap=new CountingTrap();trap.set(cell);level.traps.put(cell,trap);Level.set(cell,Terrain.SECRET_TRAP,level);return trap;}
    private HeroAction.Move travel(){HeroAction.Move action=new HeroAction.Move(target,HeroAction.Move.Mode.TRAVEL);hero.curAction=action;PathFinder.Path path=new PathFinder.Path();for(int at=hero.pos+1;at<=target;at++)path.add(at);hero.route(path);return action;}
    private boolean move()throws Exception{return (Boolean)closer.invoke(hero,hero.curAction.dst);}
    private Field heldField()throws Exception{Field f=CellSelector.class.getDeclaredField("heldAction1");f.setAccessible(true);return f;}
    @Test public void missingPreferencePreservesOriginalStop(){assertFalse(SPDSettings.continueOnTrapDiscovery());}
    @Test public void disabledDiscoveryStopsActualTravelAndClearsHeldArrow()throws Exception{
        HeroAction.Move action=travel();CountingTrap trap=hidden(start+34);heldField().set(selector,SPDAction.E);
        assertTrue(move());assertEquals(start+1,hero.pos);assertTrue(trap.visible);assertEquals(0,trap.activations);assertNull(hero.curAction);assertSame(action,hero.lastAction);assertEquals(SPDAction.NONE,heldField().get(selector));
    }
    @Test public void enabledDiscoveryKeepsDestinationCachedQueueAndHeldArrow()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);HeroAction.Move action=travel();PathFinder.Path queued=hero.route();CountingTrap trap=hidden(start+34);heldField().set(selector,SPDAction.E);float before=hero.cooldown();
        assertTrue(move());assertEquals(start+1,hero.pos);assertTrue(trap.visible);assertTrue(trap.active);assertEquals(0,trap.activations);assertSame(action,hero.curAction);assertNull(hero.lastAction);assertSame(queued,hero.route());assertEquals(target,hero.route().getLast().intValue());assertEquals(SPDAction.E,heldField().get(selector));assertEquals(before+1/hero.speed(),hero.cooldown(),.00001f);
        assertTrue(move());assertEquals(start+2,hero.pos);assertSame(action,hero.curAction);
    }
    @Test public void trapOnCachedRouteIsReplannedAroundWithoutActivation()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);HeroAction.Move action=travel();CountingTrap trap=hidden(start+2);assertTrue(move());assertTrue(trap.visible);assertFalse(level.passable[trap.pos]);assertTrue(level.avoid[trap.pos]);
        int steps=1;while(hero.pos!=target&&steps++<15){assertTrue(move());assertNotEquals(trap.pos,hero.pos);assertSame(action,hero.curAction);}assertEquals(target,hero.pos);assertEquals(0,trap.activations);
    }
    @Test public void trapBlockingOnlyRouteStillPreventsAutomaticCrossing()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);Arrays.fill(level.map,Terrain.WALL);for(int i=start;i<=target;i++)level.map[i]=Terrain.EMPTY;level.buildFlagMaps();travel();CountingTrap trap=hidden(start+2);
        assertTrue(move());assertEquals(start+1,hero.pos);assertTrue(trap.visible);assertFalse(move());assertEquals(start+1,hero.pos);assertEquals(0,trap.activations);
    }
    @Test public void actualSteppingOnRevealedTrapStillInterruptsAndTriggers()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);CountingTrap trap=hidden(start+1);level.discover(trap.pos);hero.curAction=new HeroAction.Move(trap.pos);float before=hero.cooldown();assertTrue(move());assertEquals(trap.pos,hero.pos);assertNull(hero.curAction);assertEquals(1,trap.activations);assertEquals(97,hero.HP);assertFalse(trap.active);assertTrue(hero.cooldown()>before);
    }
    @Test public void steppingOnUndetectedTrapStillTriggersAndInterrupts()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);Buff.detach(hero,Foresight.class);CountingTrap trap=hidden(start+1);hero.curAction=new HeroAction.Move(trap.pos);assertTrue(move());assertEquals(1,trap.activations);assertNull(hero.curAction);assertEquals(97,hero.HP);
    }
    @Test public void knownTrapConfirmationIsNotBypassed()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);SPDSettings.confirmKnownTraps(true);CountingTrap trap=hidden(start+1);level.discover(trap.pos);hero.curAction=new HeroAction.Move(trap.pos);assertFalse(move());assertNotNull(hero.warning);assertEquals(start,hero.pos);assertEquals(0,trap.activations);assertNull(hero.curAction);hero.warning.choose(false);assertNull(hero.curAction);
    }
    @Test public void doorDiscoveryTogetherWithTrapStillStopsTravel()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);travel();CountingTrap trap=hidden(start+34);Level.set(start+35,Terrain.SECRET_DOOR,level);assertTrue(move());assertTrue(trap.visible);assertEquals(Terrain.DOOR,level.map[start+35]);assertNull(hero.curAction);
    }
    @Test public void manualSearchAndWaitingKeepTheirOriginalInterruptions(){
        SPDSettings.continueOnTrapDiscovery(true);travel();hidden(start+1);assertTrue(hero.search(true));assertNull(hero.curAction);
        hidden(start+2);hero.resting=true;assertTrue(hero.search(false));assertFalse(hero.resting);
    }
    @Test public void movementTowardItemAndChestKeepsItsOriginalAction()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);for(HeroAction action:new HeroAction[]{new HeroAction.PickUp(target),new HeroAction.OpenChest(target),new HeroAction.LvlTransition(target)}){hero.pos=start;hero.route(null);hero.curAction=action;hidden(start+34);assertTrue(move());assertSame(action,hero.curAction);}
    }
    @Test public void damageAndPlayerCancellationStillCancelMovement()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);travel();hero.damage(1,Object.class);assertNull(hero.curAction);travel();hero.interrupt();assertNull(hero.curAction);
    }
    @Test public void newlyVisibleEnemyStillInterrupts()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);travel();Rat rat=new Rat();rat.pos=start+34;level.mobs.add(rat);Object prior=com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.lastTarget;com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.lastTarget=rat;
        try{Method check=Hero.class.getDeclaredMethod("checkVisibleMobs");check.setAccessible(true);check.invoke(hero);assertNull(hero.curAction);}finally{com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.lastTarget=(com.shatteredpixel.shatteredpixeldungeon.actors.Char)prior;}
    }
    @Test public void stairNavigationStillRejectsUnsafeNextStep()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);CountingTrap trap=hidden(start+1);level.discover(trap.pos);assertFalse(com.shatteredpixel.shatteredpixeldungeon.apd.navigation.StairNavigation.safeStep(level,hero,trap.pos));assertTrue(trap.active);assertEquals(0,trap.activations);
    }
    @Test public void preferenceSurvivesFreshPreferenceInstanceAndDoesNotBecomeADungeonRule()throws Exception{
        FileHandle file=new FileHandle(java.nio.file.Files.createTempDirectory("apd-trap-reload-").resolve("settings.xml").toFile());GameSettings.set(new HeadlessPreferences(file));Bundle before=new Bundle(),after=new Bundle();Dungeon.apdStoreCustomRules(before);assertFalse(SPDSettings.continueOnTrapDiscovery());SPDSettings.continueOnTrapDiscovery(true);GameSettings.set(new HeadlessPreferences(file));assertTrue(SPDSettings.continueOnTrapDiscovery());Dungeon.apdStoreCustomRules(after);assertEquals(before.toString(),after.toString());SPDSettings.continueOnTrapDiscovery(false);GameSettings.set(new HeadlessPreferences(file));assertFalse(SPDSettings.continueOnTrapDiscovery());
    }
    @Test public void selectedExplorationDestinationAndSingleArrowStepsContinue()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);for(int destination:new int[]{start+1,target}){hero.pos=start;hero.route(null);level.visited[destination]=false;level.mapped[destination]=false;assertTrue(hero.handle(destination));HeroAction selected=hero.curAction;CountingTrap trap=hidden(start+34);heldField().set(selector,SPDAction.E);assertTrue(move());assertTrue(trap.visible);assertSame(selected,hero.curAction);assertEquals(destination,hero.curAction.dst);assertEquals(SPDAction.E,heldField().get(selector));}
    }
    @Test public void realObstacleStillBlocksTheRoute()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);Arrays.fill(level.map,Terrain.WALL);level.map[start]=level.map[target]=Terrain.EMPTY;level.buildFlagMaps();travel();assertFalse(move());assertEquals(start,hero.pos);
    }
    @Test public void flightRetainsItsEstablishedTrapHandling()throws Exception{
        for(boolean enabled:new boolean[]{false,true}){SPDSettings.continueOnTrapDiscovery(enabled);hero.pos=start;hero.flying=true;CountingTrap trap=hidden(start+1);level.discover(trap.pos);hero.curAction=new HeroAction.Move(trap.pos);assertTrue(move());assertEquals(trap.pos,hero.pos);assertEquals(0,trap.activations);assertTrue(trap.active);assertEquals(100,hero.HP);}
    }
    @Test public void alreadyVisibleTrapSelectedInUnvisitedFogRetainsItsStop()throws Exception{
        SPDSettings.continueOnTrapDiscovery(true);int destination=start+3;CountingTrap trap=hidden(destination);level.discover(destination);level.visited[destination]=level.mapped[destination]=false;hero.fieldOfView[destination]=false;assertTrue(hero.handle(destination));assertTrue(move());assertTrue(move());assertFalse(move());assertEquals(destination-1,hero.pos);assertEquals(0,trap.activations);
    }
    @Test public void heroSaveRoundTripKeepsRunDataAndDoesNotOverrideTheGlobalPreference(){
        SPDSettings.continueOnTrapDiscovery(true);travel();Bundle saved=new Bundle();hero.storeInBundle(saved);Hero restored=new Hero();restored.restoreFromBundle(saved);assertEquals(hero.HP,restored.HP);assertEquals(hero.HT,restored.HT);assertEquals(hero.pos,restored.pos);assertNull(restored.curAction);assertNull(restored.lastAction);assertTrue(SPDSettings.continueOnTrapDiscovery());SPDSettings.continueOnTrapDiscovery(false);restored.restoreFromBundle(saved);assertFalse(SPDSettings.continueOnTrapDiscovery());
    }
    @Test public void preferenceCanChangeDuringTheCurrentRun()throws Exception{
        travel();SPDSettings.continueOnTrapDiscovery(true);CountingTrap trap=hidden(start+34);assertTrue(move());assertTrue(trap.visible);assertNotNull(hero.curAction);SPDSettings.continueOnTrapDiscovery(false);hidden(start+35);assertTrue(move());assertNull(hero.curAction);
    }
    @Test public void discoveryProbabilityAndRandomConsumptionAreUnchanged(){
        Buff.detach(hero,Foresight.class);for(int seed=0;seed<40;seed++){boolean[] found=new boolean[2];long[] next=new long[2];for(int mode=0;mode<2;mode++){SPDSettings.continueOnTrapDiscovery(mode==1);hero.curAction=new HeroAction.Move(target);CountingTrap trap=hidden(start+1);Random.pushGenerator(seed);try{found[mode]=hero.search(false);next[mode]=Random.Long();assertEquals(found[mode],trap.visible);}finally{Random.popGenerator();}}assertEquals(found[0],found[1]);assertEquals(next[0],next[1]);}
    }
}
