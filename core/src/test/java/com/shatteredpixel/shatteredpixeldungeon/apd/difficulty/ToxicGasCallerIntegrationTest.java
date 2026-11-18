package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ChaoticCenser;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Group;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class ToxicGasCallerIntegrationTest{
 static class TestHero extends Hero{@Override public void interrupt(){}}
 static class Quiet extends CharSprite{@Override public void showStatus(int c,String s,Object...a){} @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}}
 static class Target extends Char{Target(){HP=HT=1000;pos=12;sprite=new Quiet();}}
 static class TestDM extends DM200{void fire(Char target)throws Exception{enemy=target;java.lang.reflect.Method method=DM200.class.getDeclaredMethod("zap");method.setAccessible(true);method.invoke(this);}}
 static class TestFist extends YogFist.RottingFist{void fire(Char target){enemy=target;zap();}}
 Hero hero;Level level;int depth;Bundle run;java.util.Set<Actor> actors;
 @Before public void setup()throws Exception{HeadlessTestSupport.start();hero=Dungeon.hero;level=Dungeon.level;depth=Dungeon.depth;actors=Actor.all();run=new Bundle();Dungeon.apdStoreCustomRules(run);Dungeon.hero=new TestHero();Dungeon.hero.HP=20;Dungeon.hero.pos=18;Dungeon.hero.sprite=new Quiet();new Group().add(Dungeon.hero.sprite);Dungeon.depth=14;Dungeon.level=new SewerLevel();java.lang.reflect.Method size=Level.class.getDeclaredMethod("setSize",int.class,int.class);size.setAccessible(true);size.invoke(Dungeon.level,5,5);java.util.Arrays.fill(Dungeon.level.map,Terrain.EMPTY);java.util.Arrays.fill(Dungeon.level.passable,true);Dungeon.level.blobs=new java.util.HashMap<>();Bundle saved=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,200).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,300).with(CustomDungeonRules.Slider.BOSS_DAMAGE,500).store(r);saved.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(saved);}
 @After public void cleanup(){for(Actor a:Actor.all())if(!actors.contains(a))Actor.remove(a);Dungeon.hero=hero;Dungeon.level=level;Dungeon.depth=depth;Dungeon.apdRestoreCustomRules(run);assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,OffensiveEffectContext.current());}
 @Test public void actualDM200AndRottingFistUseEnemyAndBossDamage()throws Exception{Target t=new Target();Actor.add(t);TestDM dm=new TestDM();dm.pos=6;dm.fire(t);ToxicGas gas=(ToxicGas)Dungeon.level.blobs.get(ToxicGas.class);assertNotNull(gas);gas.act();assertEquals(991,t.HP);Actor.remove(gas);Dungeon.level.blobs.clear();Target u=new Target();Actor.remove(t);Actor.add(u);TestFist fist=new TestFist();fist.pos=6;fist.fire(u);gas=(ToxicGas)Dungeon.level.blobs.get(ToxicGas.class);gas.act();assertEquals(985,u.HP);}
 @Test public void delayedDM300KeepsCapturedBossOwnershipAfterTheCasterChanges()throws Exception{Target t=new Target();Actor.add(t);DM300 boss=new DM300();boss.pos=6;java.util.Set<Actor> before=Actor.all();boss.ventGas(t);boss.apdInheritOffensiveOrigin(Dungeon.hero);boss.HP=0;Actor delayed=null;for(Actor a:Actor.all())if(!before.contains(a)&&a.getClass().getEnclosingClass()==DM300.class)delayed=a;assertNotNull(delayed);java.lang.reflect.Method tick=delayed.getClass().getDeclaredMethod("act");tick.setAccessible(true);tick.invoke(delayed);ToxicGas gas=(ToxicGas)Dungeon.level.blobs.get(ToxicGas.class);assertNotNull(gas);gas.act();assertEquals(985,t.HP);}
 @Test public void actualCursedGasUsesTheKnownBossCaster(){Target t=new Target();Actor.add(t);long seed=0;for(;;seed++){Random.pushGenerator(seed);boolean found;try{found=Random.Int(3)==1;}finally{Random.popGenerator();}if(found)break;}com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica bolt=new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(6,12,com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE);Random.pushGenerator(seed);try{assertTrue(new CursedWand.RandomGas().effect(new Bomb(),new Goo(),bolt,false));}finally{Random.popGenerator();}ToxicGas gas=(ToxicGas)Dungeon.level.blobs.get(ToxicGas.class);assertNotNull(gas);gas.act();assertEquals(985,t.HP);}
 @Test public void delayedCenserStoresItsPlayerOriginAndLegacyDoesNotAcquireOne(){ChaoticCenser.GasSpewer current=new ChaoticCenser.GasSpewer();assertTrue(current.attachTo(Dungeon.hero));current.set(12,ToxicGas.class,100);Bundle saved=new Bundle();current.storeInBundle(saved);assertEquals("PLAYER",saved.getString("apd_censer_origin"));current.detach();ChaoticCenser.GasSpewer restored=new ChaoticCenser.GasSpewer();restored.restoreFromBundle(saved);assertTrue(restored.attachTo(Dungeon.hero));Target t=new Target();Actor.add(t);restored.act();ToxicGas gas=(ToxicGas)Dungeon.level.blobs.get(ToxicGas.class);assertNotNull(gas);gas.act();assertEquals(994,t.HP);saved.remove("apd_censer_origin");ChaoticCenser.GasSpewer legacy=new ChaoticCenser.GasSpewer();legacy.restoreFromBundle(saved);Bundle again=new Bundle();legacy.storeInBundle(again);assertFalse(again.contains("apd_censer_origin"));}
 @Test public void actualRotHeartAndStenchKeepTheEmittingCreatureAndWearer(){
  Target t=new Target();Actor.add(t);RotHeart heart=new RotHeart();heart.pos=12;heart.defenseProc(t,1);ToxicGas gas=(ToxicGas)Dungeon.level.blobs.get(ToxicGas.class);assertNotNull(gas);gas.act();assertEquals(991,t.HP);
  Actor.remove(gas);Dungeon.level.blobs.clear();Actor.remove(t);Target u=new Target();Actor.add(u);Dungeon.hero.pos=12;
  long seed=0;for(;;seed++){Random.pushGenerator(seed);boolean found;try{found=Random.Float()<.125f;}finally{Random.popGenerator();}if(found)break;}
  Random.pushGenerator(seed);try{assertEquals(1,new com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Stench().proc(new com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor(),new Rat(),Dungeon.hero,1));}finally{Random.popGenerator();}
  gas=(ToxicGas)Dungeon.level.blobs.get(ToxicGas.class);assertNotNull(gas);gas.act();assertEquals(994,u.HP);
 }
}
