package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.*;
import org.junit.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import static org.junit.Assert.*;
public class SpecialBombDamageProvenanceTest {
 static class Quiet extends CharSprite {
  @Override public void showStatus(int c,String s,Object...a){}
  @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}
  @Override public Emitter emitter(){return new Emitter();}
 }
 static class Target extends Char {
  ArrayList<Integer> hits=new ArrayList<>();ArrayList<Object> sources=new ArrayList<>();
  Target(boolean undead){HP=HT=100000;pos=312;sprite=new Quiet();if(undead)properties.add(Property.UNDEAD);}
  @Override public int drRoll(){return 3;}
  @Override public void damage(int d,Object s){hits.add(d);sources.add(s);}
 }
 Hero oldHero;Level oldLevel;int oldDepth;Object oldScene;Field scene;Bundle oldRules;
 @Before public void setup()throws Exception {
  HeadlessTestSupport.start();oldHero=Dungeon.hero;oldLevel=Dungeon.level;oldDepth=Dungeon.depth;
  oldRules=new Bundle();Dungeon.apdStoreCustomRules(oldRules);
  scene=GameScene.class.getDeclaredField("scene");scene.setAccessible(true);oldScene=scene.get(null);
  GameScene mock=new GameScene();Field emitters=GameScene.class.getDeclaredField("emitters");emitters.setAccessible(true);emitters.set(mock,new Group());scene.set(null,mock);
  Dungeon.hero=new Hero();Dungeon.depth=4;Dungeon.level=new SewerLevel();
  java.lang.reflect.Method size=Level.class.getDeclaredMethod("setSize",int.class,int.class);size.setAccessible(true);size.invoke(Dungeon.level,25,25);
  java.util.Arrays.fill(Dungeon.level.map,Terrain.EMPTY);Dungeon.level.heaps=new SparseArray<>();
 }
 @After public void cleanup()throws Exception {scene.set(null,oldScene);Dungeon.hero=oldHero;Dungeon.level=oldLevel;Dungeon.depth=oldDepth;Dungeon.apdRestoreCustomRules(oldRules);}
 void rules(int p,int e,int b){Bundle save=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,p).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,e).with(CustomDungeonRules.Slider.BOSS_DAMAGE,b).store(r);save.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(save);}
 int scaled(int raw,int p){return p==100?raw:Math.round(raw*p/100f);}
 void check(Bomb bomb,OffensiveDamageOrigin role,int percentage,boolean restored)throws Exception {
  rules(100,100,100);
  Target reference=new Target(bomb instanceof HolyBomb);Actor.add(reference);
  int raw,bonus=0;long next;
  Random.pushGenerator(125);try{bomb.explode(312);next=Random.Long();}finally{Random.popGenerator();Actor.remove(reference);}
  assertEquals(bomb instanceof HolyBomb?2:1,reference.hits.size());
  raw=reference.hits.get(0)+(bomb instanceof ArcaneBomb?0:3);
  if(bomb instanceof HolyBomb)bonus=reference.hits.get(1);
  rules(role==OffensiveDamageOrigin.PLAYER?percentage:275,role==OffensiveDamageOrigin.ENEMY?percentage:325,role==OffensiveDamageOrigin.BOSS?percentage:425);
  Target target=new Target(bomb instanceof HolyBomb);Actor.add(target);
  try{
   Random.pushGenerator(125);try{
    if(restored){Bundle save=new Bundle();bomb.storeInBundle(save);save.put("apd_bomb_origin",role.name());bomb.restoreFromBundle(save);bomb.explode(312);}else bomb.apdExplodeAttributed(312,role);
    int first=scaled(raw,percentage)-(bomb instanceof ArcaneBomb?0:3);
    if(bomb instanceof HolyBomb){assertEquals(first>0?2:1,target.hits.size());if(first>0){assertEquals(first,(int)target.hits.get(0));assertSame(bomb,target.sources.get(0));}assertEquals(scaled(bonus,percentage),(int)target.hits.get(target.hits.size()-1));assertTrue(target.sources.get(target.sources.size()-1) instanceof HolyBomb.HolyDamage);}
    else {assertEquals(1,target.hits.size());assertEquals(first,(int)target.hits.get(0));assertSame(bomb,target.sources.get(0));}
    assertEquals("No added RNG draws",next,Random.Long());
   }finally{Random.popGenerator();}
  }finally{Actor.remove(target);}
 }
 @Test public void arcaneUsesEachCreatorExactlyOnceAndStillPiercesArmor()throws Exception {for(OffensiveDamageOrigin role:new OffensiveDamageOrigin[]{OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.ENEMY,OffensiveDamageOrigin.BOSS})for(int p=25;p<=500;p+=25)check(new ArcaneBomb(),role,p,false);}
 @Test public void shrapnelScalesBeforeExistingArmorAndKeepsOriginalSource()throws Exception {for(OffensiveDamageOrigin role:new OffensiveDamageOrigin[]{OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.ENEMY,OffensiveDamageOrigin.BOSS})for(int p=25;p<=500;p+=25)check(new ShrapnelBomb(),role,p,false);}
 @Test public void holyBonusAndBaseUseSameCreatorButRemainSeparateOriginalHits()throws Exception {for(OffensiveDamageOrigin role:new OffensiveDamageOrigin[]{OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.ENEMY,OffensiveDamageOrigin.BOSS})for(int p=25;p<=500;p+=25)check(new HolyBomb(),role,p,false);}
 @Test public void legacyUnownedSpecialBombsRemainUnscaled()throws Exception {for(Bomb b:new Bomb[]{new ArcaneBomb(),new ShrapnelBomb(),new HolyBomb()})check(b,OffensiveDamageOrigin.UNATTRIBUTED,100,false);}
 @Test public void savedSpecialBombCreatorSurvivesRestore()throws Exception {for(Bomb b:new Bomb[]{new ArcaneBomb(),new ShrapnelBomb(),new HolyBomb()})check(b,OffensiveDamageOrigin.BOSS,425,true);}
 @Test public void neutralSpecialBombsPreserveDamageAndRng()throws Exception {for(Bomb b:new Bomb[]{new ArcaneBomb(),new ShrapnelBomb(),new HolyBomb()})check(b,OffensiveDamageOrigin.PLAYER,100,false);}
}
