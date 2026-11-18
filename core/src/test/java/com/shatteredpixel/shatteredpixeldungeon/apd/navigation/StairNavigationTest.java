package com.shatteredpixel.shatteredpixeldungeon.apd.navigation;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.Random;
import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;
public class StairNavigationTest {
 static class Empty extends Level {
  int activations;LevelTransition activatedStair;
  protected boolean build(){return true;}protected void createMobs(){}protected void createItems(){}
  @Override public boolean activateTransition(Hero hero,LevelTransition transition){
   activations++;activatedStair=transition;return true;
  }
 }
 static class Player extends Hero {int enemies;@Override public int visibleEnemies(){return enemies;}}
 private Level previousLevel, level;private Hero previousHero;private Player hero; private Object previousSelector;private boolean previousAutoEnter;
 @Before public void setup() throws Exception {HeadlessTestSupport.start();java.lang.reflect.Field selector=com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("cellSelector");selector.setAccessible(true);previousSelector=selector.get(null);
 java.lang.reflect.Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);sun.misc.Unsafe unsafe=(sun.misc.Unsafe)uf.get(null);selector.set(null,unsafe.allocateInstance(com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector.class));
 previousAutoEnter=com.shatteredpixel.shatteredpixeldungeon.SPDSettings.autoEnterStairs();
 com.shatteredpixel.shatteredpixeldungeon.SPDSettings.autoEnterStairs(false);
 previousLevel=Dungeon.level;previousHero=Dungeon.hero;level=new Empty();level.setSize(5,5);level.transitions=new ArrayList<>();level.mobs=new HashSet<>();level.traps=new com.watabou.utils.SparseArray<>();level.blobs=new HashMap<>();level.heaps=new com.watabou.utils.SparseArray<>();level.plants=new com.watabou.utils.SparseArray<>();Arrays.fill(level.map,Terrain.EMPTY);Arrays.fill(level.visited,true);Arrays.fill(level.passable,true);hero=new Player();hero.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(hero);hero.pos=12;hero.ready=true;hero.fieldOfView=new boolean[25];Dungeon.level=level;Dungeon.hero=hero;}
 @After public void cleanup() throws Exception {com.shatteredpixel.shatteredpixeldungeon.SPDSettings.autoEnterStairs(previousAutoEnter);Dungeon.level=previousLevel;Dungeon.hero=previousHero;java.lang.reflect.Field selector=com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("cellSelector");selector.setAccessible(true);selector.set(null,previousSelector);}
 private void stairs(){level.transitions.add(new LevelTransition(level,11,LevelTransition.Type.REGULAR_ENTRANCE));level.transitions.add(new LevelTransition(level,13,LevelTransition.Type.REGULAR_EXIT));}
 static class InvisibleSprite extends com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite {
  void attach(Hero h){ch=h;h.sprite=this;visible=false;}
  @Override public void place(int cell){} @Override public void move(int from,int to){} @Override public void idle(){} @Override public void showStatus(int color,String text,Object...args){}
  @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){}
  @Override public void showAlert(){} @Override public void hideAlert(){}
 }
 @Test public void damageCancelsEvenWhenNormalDamageInterruptionWasDisabled(){stairs();new InvisibleSprite().attach(hero);assertNull(StairNavigation.begin(hero,false));hero.damageInterrupt=false;int hp=hero.HP;hero.damage(1,new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger());assertTrue(hero.HP<hp);assertNull(hero.curAction);assertNull(hero.lastAction);}
 @Test public void restoredHeroDoesNotResumeTransientNavigation(){stairs();assertNull(StairNavigation.begin(hero,false));com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();hero.storeInBundle(saved);Hero restored=new Hero();restored.restoreFromBundle(saved);assertEquals(hero.pos,restored.pos);assertEquals(hero.HP,restored.HP);assertNull(restored.curAction);assertNull(restored.lastAction);}
 @Test public void walkingPaysTheNormalMovementCostWithoutChangingFloors() throws Exception {
  stairs();new InvisibleSprite().attach(hero);int depth=Dungeon.depth;float before=hero.cooldown();assertNull(StairNavigation.begin(hero,false));
  java.lang.reflect.Method move=Hero.class.getDeclaredMethod("actMove",HeroAction.Move.class);move.setAccessible(true);
  assertTrue((Boolean)move.invoke(hero,hero.curAction));assertEquals(13,hero.pos);assertEquals(before+1f/hero.speed(),hero.cooldown(),0.00001f);
  assertTrue(hero.curAction instanceof HeroAction.Move);assertEquals(depth,Dungeon.depth);
 }
 @Test public void enablingAutoEnterCompletesNormalStairWalkThenTransitionsExactlyOnce() throws Exception {
  stairs();new InvisibleSprite().attach(hero);com.shatteredpixel.shatteredpixeldungeon.SPDSettings.autoEnterStairs(true);
  float before=hero.cooldown();assertNull(StairNavigation.begin(hero,false));
  HeroAction.Move walk=(HeroAction.Move)hero.curAction;
  assertTrue(walk instanceof StairNavigation.Walk);
  assertTrue(((StairNavigation.Walk)walk).enterOnArrival);
  java.lang.reflect.Method move=Hero.class.getDeclaredMethod("actMove",HeroAction.Move.class);move.setAccessible(true);
  assertTrue((Boolean)move.invoke(hero,walk));
  assertEquals(13,hero.pos);
  assertEquals(before+1f/hero.speed(),hero.cooldown(),0.00001f);
  assertEquals(0,((Empty)level).activations);
  assertFalse((Boolean)move.invoke(hero,walk));
  assertEquals(1,((Empty)level).activations);
  assertEquals(LevelTransition.Type.REGULAR_EXIT,((Empty)level).activatedStair.type);
  assertNull(hero.curAction);
 }
 @Test public void disabledAutoEntryStillStopsOnStairs() throws Exception {
  stairs();new InvisibleSprite().attach(hero);
  assertNull(StairNavigation.begin(hero,false));
  java.lang.reflect.Method move=Hero.class.getDeclaredMethod("actMove",HeroAction.Move.class);move.setAccessible(true);
  assertTrue((Boolean)move.invoke(hero,hero.curAction));
  // Headless fixtures have no GameScene HUD/AttackIndicator for Hero.ready().
  // Assert the arrival decision directly; regular in-game completion calls ready().
  assertFalse(StairNavigation.canEnterOnArrival(hero,(StairNavigation.Walk)hero.curAction));
  assertEquals(0,((Empty)level).activations);
  assertTrue(hero.curAction instanceof StairNavigation.Walk);
 }
 @Test public void autoEnterCanUseStairsAlreadyUnderHero() throws Exception {
  stairs();hero.pos=13;hero.ready=true;com.shatteredpixel.shatteredpixeldungeon.SPDSettings.autoEnterStairs(true);
  assertNull(StairNavigation.begin(hero,false));
  assertTrue(hero.curAction instanceof HeroAction.LvlTransition);
  java.lang.reflect.Method transition=Hero.class.getDeclaredMethod("actTransition",HeroAction.LvlTransition.class);transition.setAccessible(true);
  assertFalse((Boolean)transition.invoke(hero,hero.curAction));
  assertEquals(1,((Empty)level).activations);
 }
 @Test public void unsafeOrEnemyArrivalNeverAutoEnters() throws Exception {
  stairs();new InvisibleSprite().attach(hero);com.shatteredpixel.shatteredpixeldungeon.SPDSettings.autoEnterStairs(true);
  assertNull(StairNavigation.begin(hero,true));
  hero.pos=11;hero.enemies=1;
  assertFalse(StairNavigation.canEnterOnArrival(hero,(StairNavigation.Walk)hero.curAction));
  assertEquals(0,((Empty)level).activations);
  hero.interrupt();assertNull(hero.lastAction);
  hero.pos=12;hero.enemies=0;hero.ready=true;
  assertNull(StairNavigation.begin(hero,false));
  hero.pos=13;
  com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap trap=new WornDartTrap();
  trap.pos=13;trap.visible=true;level.traps.put(13,trap);
  assertFalse(StairNavigation.canEnterOnArrival(hero,(StairNavigation.Walk)hero.curAction));
  assertEquals(0,((Empty)level).activations);
 }
 @Test public void autoEntryNeverResumesAfterManualCancelOrDamage(){
  stairs();com.shatteredpixel.shatteredpixeldungeon.SPDSettings.autoEnterStairs(true);
  assertNull(StairNavigation.begin(hero,false));
  hero.interrupt();assertNull(hero.curAction);assertNull(hero.lastAction);
  assertNull(StairNavigation.begin(hero,false));new InvisibleSprite().attach(hero);
  hero.damage(1,new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger());
  assertNull(hero.curAction);assertNull(hero.lastAction);assertEquals(0,((Empty)level).activations);
 }
 @Test public void offscreenAndObscuredKnownStairsStillHaveExactDestinations(){stairs();Arrays.fill(hero.fieldOfView,false);assertNull(StairNavigation.begin(hero,false));assertEquals(13,hero.curAction.dst);assertTrue(com.shatteredpixel.shatteredpixeldungeon.effects.StairHighlight.known(13));level.visited[11]=false;assertFalse(com.shatteredpixel.shatteredpixeldungeon.effects.StairHighlight.known(11));}
 @Test public void bothDirectionsChooseOnlyTheirOwnStair(){stairs();assertEquals(11,StairNavigation.discoveredStair(level,true));assertEquals(13,StairNavigation.discoveredStair(level,false));}
 @Test public void nullTransitionEntryDoesNotCrashOrInventDiscoveredStairs(){
  level.transitions.add(null);
  float before=hero.cooldown();
  assertEquals(-1,StairNavigation.discoveredStair(level,true));
  assertEquals(-1,StairNavigation.discoveredStair(level,false));
  assertEquals(-1,StairNavigation.discoveredTarget(level,StairNavigation.Route.RETURN_TO_MAIN_DUNGEON));
  assertEquals("Those stairs have not been discovered.",StairNavigation.begin(hero,false));
  assertNull(hero.curAction);
  assertEquals(before,hero.cooldown(),0f);
 }
 @Test public void nullTransitionEntryDoesNotHideValidOrMappedStairs(){
  level.transitions.add(null);
  stairs();
  level.transitions.add(null);
  assertEquals(11,StairNavigation.discoveredStair(level,true));
  assertEquals(13,StairNavigation.discoveredStair(level,false));
  level.visited[13]=false;
  level.mapped[13]=true;
  assertEquals(13,StairNavigation.discoveredStair(level,false));
  level.visited[11]=false;
  assertEquals(-1,StairNavigation.discoveredStair(level,true));
 }
 @Test public void undiscoveredStairDoesNotExposeItsLocation(){stairs();level.visited[11]=false;assertEquals(-1,StairNavigation.discoveredStair(level,true));assertEquals(13,StairNavigation.discoveredStair(level,false));}
 @Test public void mappedStairsAreAlreadyKnown(){stairs();level.visited[11]=false;level.mapped[11]=true;assertEquals(11,StairNavigation.discoveredStair(level,true));}
 @Test public void missingDirectionCannotFallBackToOtherStair(){level.transitions.add(new LevelTransition(level,13,LevelTransition.Type.REGULAR_EXIT));assertEquals(-1,StairNavigation.discoveredStair(level,true));}
 @Test public void truncatedVisibilityMetadataBlocksStairShortcutWithoutSpendingATurn(){
  stairs();
  hero.fieldOfView=new boolean[13]; // The remembered exit is at cell 13.
  float before=hero.cooldown();
  assertFalse(StairNavigation.safeStep(level,hero,13));
  assertEquals("No safe discovered route reaches those stairs.",StairNavigation.begin(hero,false));
  assertNull(hero.curAction);
  assertEquals(before,hero.cooldown(),0f);
  hero.fieldOfView=new boolean[level.length()];
  assertTrue(StairNavigation.safeStep(level,hero,13));
  assertNull(StairNavigation.begin(hero,false));
  assertTrue(hero.curAction instanceof StairNavigation.Walk);
 }
 @Test public void missingTrapMetadataBlocksStairShortcutWithoutSpendingATurn(){
  stairs();
  com.watabou.utils.SparseArray<Trap> previous=level.traps;
  level.traps=null;
  float before=hero.cooldown();
  try {
   assertFalse(StairNavigation.safeStep(level,hero,13));
   assertEquals("No safe discovered route reaches those stairs.",StairNavigation.begin(hero,false));
   assertNull(hero.curAction);
   assertEquals(before,hero.cooldown(),0f);
  } finally {level.traps=previous;}
  assertTrue(StairNavigation.safeStep(level,hero,13));
  assertNull(StairNavigation.begin(hero,false));
  assertTrue(hero.curAction instanceof StairNavigation.Walk);
 }
 @Test public void nullHeroNeverAuthorizesStairRouting(){
  assertFalse(StairNavigation.safeStep(level,null,13));
 }
 @Test public void knownTrapsAndPitsAreUnsafeButSecretTrapsAreNotRevealed(){WornDartTrap trap=new WornDartTrap();trap.pos=13;trap.visible=true;level.traps.put(13,trap);assertFalse(StairNavigation.safeStep(level,hero,13));trap.visible=false;assertTrue(StairNavigation.safeStep(level,hero,13));level.pit[13]=true;assertFalse(StairNavigation.safeStep(level,hero,13));}
 @Test public void blockedAvoidedAndUnknownTilesAreUnsafe(){level.passable[13]=false;assertFalse(StairNavigation.safeStep(level,hero,13));level.passable[13]=true;level.avoid[13]=true;assertFalse(StairNavigation.safeStep(level,hero,13));level.avoid[13]=false;level.visited[13]=false;assertFalse(StairNavigation.safeStep(level,hero,13));}
 @Test public void visibleFireIsUnsafeWithoutRevealingUnseenGas(){Fire fire=new Fire();fire.cur=new int[25];fire.cur[13]=1;level.blobs.put(Fire.class,fire);assertTrue(StairNavigation.safeStep(level,hero,13));hero.fieldOfView[13]=true;assertFalse(StairNavigation.safeStep(level,hero,13));}
 @Test public void purityImmunityAllowsCurrentlyProtectedGasButStopsAfterItExpires(){
  stairs();hero.fieldOfView[13]=true;
  ToxicGas gas=new ToxicGas();gas.cur=new int[25];gas.cur[13]=12;level.blobs.put(ToxicGas.class,gas);
  assertFalse(StairNavigation.safeStep(level,hero,13));
  assertEquals("No safe discovered route reaches those stairs.",StairNavigation.begin(hero,false));
  Buff.affect(hero,BlobImmunity.class);
  assertTrue(StairNavigation.safeStep(level,hero,13));
  assertNull(StairNavigation.begin(hero,false));
  Buff.detach(hero,BlobImmunity.class);
  assertFalse(StairNavigation.safeStep(level,hero,13));
 }
 @Test public void zeroDamageDoesNotInterruptRegularTravel(){
  stairs();new InvisibleSprite().attach(hero);
  assertNull(StairNavigation.begin(hero,false)); // create a live route, then test normal travel
  hero.interrupt();hero.ready=true;
  hero.curAction=new HeroAction.Move(13,HeroAction.Move.Mode.TRAVEL);
  hero.damage(0,new ToxicGas());
  assertTrue(hero.curAction instanceof HeroAction.Move);
  assertNull(hero.lastAction);
 }
 @Test public void longDistanceTapsReachChosenTileAcrossVisualPlaybackSpeeds() throws Exception {
  int prior=com.shatteredpixel.shatteredpixeldungeon.SPDSettings.playbackSpeed();
  new InvisibleSprite().attach(hero);
  java.lang.reflect.Method move=Hero.class.getDeclaredMethod("actMove",HeroAction.Move.class);
  move.setAccessible(true);
  try {
   for(int visualSpeed:new int[]{0,1,2}) {
    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.playbackSpeed(visualSpeed);
    // Real pathfinding requires the global map dimensions and impassable border
    // to agree with the fixture. The old 5x5 mock never called PathFinder.setMapSize.
    level.setSize(9,9);
    Arrays.fill(level.map,Terrain.EMPTY);
    Arrays.fill(level.visited,true);
    Arrays.fill(level.passable,true);
    for(int cell=0;cell<81;cell++) {
     if(cell/9==0 || cell/9==8 || cell%9==0 || cell%9==8) level.passable[cell]=false;
    }
    com.watabou.utils.PathFinder.setMapSize(9,9);
    hero.interrupt();hero.pos=70;hero.ready=true;hero.fieldOfView=new boolean[81];
    HeroAction.Move walk=new HeroAction.Move(10,HeroAction.Move.Mode.TRAVEL);
    hero.curAction=walk;
    int steps=0;
    while(hero.pos!=10 && steps<16){
      assertTrue("Nonhazardous travel stopped at speed "+visualSpeed+" pos="+hero.pos,
        (Boolean)move.invoke(hero,walk));
      steps++;
      assertSame(walk,hero.curAction);
      assertNull(hero.lastAction);
    }
    assertEquals("Travel destination at speed "+visualSpeed,10,hero.pos);
    assertTrue("Traversal should span several turns",steps>=6);
   }
  } finally {
   com.shatteredpixel.shatteredpixeldungeon.SPDSettings.playbackSpeed(prior);
   com.watabou.utils.PathFinder.setMapSize(5,5);
  }
 }
 @Test public void enemyPreventsNavigationWithoutUsingATurn(){stairs();hero.enemies=1;float before=hero.cooldown();assertNotNull(StairNavigation.begin(hero,false));assertNull(hero.curAction);assertEquals(before,hero.cooldown(),0);}
 @Test public void normalMoveIsSelectedInsteadOfFloorTransition(){stairs();float before=hero.cooldown();assertNull(StairNavigation.begin(hero,false));assertTrue(hero.curAction instanceof HeroAction.Move);assertFalse(hero.curAction instanceof HeroAction.LvlTransition);assertEquals(13,hero.curAction.dst);assertEquals(before,hero.cooldown(),0);}
 @Test public void manualCancellationCannotResumeAStaleStairRoute(){stairs();assertNull(StairNavigation.begin(hero,false));hero.interrupt();assertNull(hero.curAction);assertNull(hero.lastAction);}
 @Test public void blockedStairHasNoRoute(){stairs();level.passable[13]=false;assertNotNull(StairNavigation.begin(hero,false));assertNull(hero.curAction);}
 @Test public void navigationDoesNotConsumeRandomnessOrChangeTerrain(){stairs();int[] before=level.map.clone();int expected;Random.pushGenerator(3456);try{expected=Random.Int();}finally{Random.popGenerator();}Random.pushGenerator(3456);try{StairNavigation.begin(hero,false);assertEquals(expected,Random.Int());}finally{Random.popGenerator();}assertArrayEquals(before,level.map);}
}
