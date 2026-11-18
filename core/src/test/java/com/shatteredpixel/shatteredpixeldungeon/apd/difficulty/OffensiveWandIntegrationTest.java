package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class OffensiveWandIntegrationTest {
 static class Quiet extends CharSprite {
  @Override public void showStatus(int c,String s,Object...a){}
  @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}
  @Override public void add(State s){} @Override public void remove(State s){}
 }
 static class TestHero extends Hero {@Override public void interrupt(){}}
 static class Board extends Level {protected boolean build(){return true;}protected void createMobs(){}protected void createItems(){}}
 static class Target extends Char {
  Object source;Target(){HP=HT=1000;pos=24;alignment=Alignment.ENEMY;sprite=new Quiet();sprite.visible=false;}
  void immune(Class<?> c){immunities.add(c);}void resistant(Class<?> c){resistances.add(c);}
  @Override public void damage(int value,Object from){source=from;super.damage(value,from);}
 }
 static class Missile extends WandOfMagicMissile {void user(Hero h){curUser=h;}}
 static class Cold extends WandOfFrost {void user(Hero h){curUser=h;}}
 static class Transfusion extends WandOfTransfusion {void user(Hero h){curUser=h;}}
 private Hero priorHero;private Level priorLevel;private boolean recording;
 @Before public void setup(){HeadlessTestSupport.start();priorHero=Dungeon.hero;priorLevel=Dungeon.level;recording=SPDSettings.apdRecording();SPDSettings.apdRecording(false);}
 @After public void clear(){Actor.clear();Dungeon.apdRestoreCustomRules(new Bundle());Dungeon.hero=priorHero;Dungeon.level=priorLevel;SPDSettings.apdRecording(recording);}
 private void rules(int p,int e,int b){Bundle all=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,p).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,e).with(CustomDungeonRules.Slider.BOSS_DAMAGE,b).store(r);all.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(all);}
 private Target board(){Actor.clear();Hero hero=new TestHero();hero.HP=hero.HT=1000;hero.pos=23;hero.sprite=new Quiet();hero.sprite.visible=false;hero.fieldOfView=new boolean[49];Dungeon.hero=hero;
  Board b=new Board();b.setSize(7,7);b.heaps=new SparseArray<>();b.blobs=new java.util.HashMap<>();b.mobs=new java.util.HashSet<>();Dungeon.level=b;Target t=new Target();Actor.add(t);return t;}
 private long zap(Wand wand,Target target){if(wand instanceof Missile)((Missile)wand).user(Dungeon.hero);else ((Cold)wand).user(Dungeon.hero);
  Random.pushGenerator(5397);try{wand.onZap(new Ballistica(23,24,Ballistica.PROJECTILE));return Random.Long();}finally{Random.popGenerator();}}
 @Test public void actualMissileAndFrostCastsScaleOnceAndKeepOriginalSources(){
  for(boolean cold:new boolean[]{false,true}){
   int neutral=0;
   for(int percent:new int[]{100,25,200,500}){
    rules(percent,500,500);Target target=board();Wand wand=cold?new Cold():new Missile();zap(wand,target);
    if(percent==100)neutral=1000-target.HP;
    assertEquals(Math.round(neutral*percent/100f),1000-target.HP);assertSame(wand,target.source);
   }
  }
 }
 @Test public void neutralPlayerCastsIgnoreEnemyBossFactorsAndPreserveNextRng(){
  for(boolean cold:new boolean[]{false,true}){
   rules(100,100,100);Target first=board();long next=zap(cold?new Cold():new Missile(),first);int hp=first.HP;
   rules(100,500,500);Target second=board();assertEquals(next,zap(cold?new Cold():new Missile(),second));assertEquals(hp,second.HP);
  }
 }
 @Test public void realSpellSourceImmunityAndResistanceStillApply(){
  rules(500,100,100);Target normal=board();Missile wand=new Missile();zap(wand,normal);int amount=1000-normal.HP;
  Target immune=board();immune.immune(Missile.class);zap(new Missile(),immune);assertEquals(1000,immune.HP);
  Target resistant=board();resistant.resistant(Missile.class);zap(new Missile(),resistant);assertEquals(1000-Math.round(amount*0.5f),resistant.HP);
 }
 @Test public void transfusionHealthCostRemainsUnscaled()throws Exception {
  for(int percent:new int[]{25,100,500}){
   rules(percent,500,500);board();Transfusion wand=new Transfusion();wand.user(Dungeon.hero);
   java.lang.reflect.Method cost=WandOfTransfusion.class.getDeclaredMethod("damageHero",int.class);cost.setAccessible(true);cost.invoke(wand,20);
   assertEquals(980,Dungeon.hero.HP);
  }
 }
}
