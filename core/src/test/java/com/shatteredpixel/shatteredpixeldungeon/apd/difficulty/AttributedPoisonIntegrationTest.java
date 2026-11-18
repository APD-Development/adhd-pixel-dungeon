package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class AttributedPoisonIntegrationTest {
 static class Quiet extends CharSprite {@Override public void showStatus(int c,String s,Object...a){} @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}}
 static class Target extends Char {Object source;Target(){HP=HT=1000;alignment=Alignment.ENEMY;sprite=new Quiet();sprite.visible=false;}
  @Override public void damage(int value,Object original){source=original;super.damage(value,original);}}
 // Bind without rendering the unchanged attachment splash; exercise the actual poison act/damage/save methods.
 static class TestPoison extends Poison {void bind(Char target){this.target=target;}float left(){return left;}}
 private Hero priorHero;
 @Before public void setup(){HeadlessTestSupport.start();priorHero=Dungeon.hero;Dungeon.hero=new Hero();}
 @After public void clear(){Dungeon.apdRestoreCustomRules(new Bundle());Dungeon.hero=priorHero;}
 private void rules(int p,int e,int b){Bundle all=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,p).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,e).with(CustomDungeonRules.Slider.BOSS_DAMAGE,b).store(r);all.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(all);}
 @Test public void realMixedPoisonTicksAndReloadKeepCauseSharesAndOriginalSource(){
  rules(200,300,500);Target a=new Target();TestPoison poison=new TestPoison();poison.bind(a);poison.setAttributed(10,new Rat());poison.extendAttributed(5,new Hero());poison.act();
  assertEquals(984,a.HP);assertSame(poison,a.source);assertEquals(14,poison.left(),0);
  Bundle saved=new Bundle();poison.storeInBundle(saved);TestPoison reloaded=new TestPoison();reloaded.restoreFromBundle(saved);Target b=new Target();reloaded.bind(b);reloaded.act();assertEquals(986,b.HP);assertEquals(13,reloaded.left(),0);
 }
 @Test public void weakerReapplicationDoesNotStealOwnershipButStrongerBossReplacesIt(){
  rules(200,300,500);Target a=new Target();TestPoison poison=new TestPoison();poison.bind(a);poison.setAttributed(10,new Rat());poison.setAttributed(4,new Goo());poison.act();assertEquals(988,a.HP);
  poison.setAttributed(15,new Goo());poison.act();assertEquals(958,a.HP);assertEquals(14,poison.left(),0);
 }
 @Test public void neutralActualTicksKeepOriginalDurationDamageAndRng(){
  rules(100,100,100);Target a=new Target();TestPoison poison=new TestPoison();poison.bind(a);poison.setAttributed(10,new Rat());poison.extendAttributed(5,new Hero());
  Random.pushGenerator(59273);long expected;try{expected=Random.Long();}finally{Random.popGenerator();}
  Random.pushGenerator(59273);try{int total=0;for(int left=15;left>0;left--){total+=left/3+1;poison.act();assertEquals(1000-total,a.HP);assertEquals(left-1,poison.left(),0);}assertEquals(expected,Random.Long());}finally{Random.popGenerator();}
 }
 @Test public void seepingUsesPoisonCreatorsAndLegacyEffectsNeverInferVictimOwnership(){
  rules(200,300,500);TestPoison parent=new TestPoison();parent.setAttributed(10,new Hero());Target a=new Target();TestPoison child=new TestPoison();child.bind(a);child.copyAttributed(parent,2);child.act();assertEquals(998,a.HP);
  Bundle old=new Bundle();old.put("left",10f);TestPoison legacy=new TestPoison();legacy.restoreFromBundle(old);Target b=new Target();legacy.bind(b);legacy.act();assertEquals(996,b.HP);
  Target c=new Target();TestPoison zero=new TestPoison();zero.bind(c);zero.setAttributed(0,new Goo());zero.act();assertEquals(995,c.HP);
 }

 @Test public void modifiedIncomingForecastMatchesActualTicksAndDoesNotConsumeStateOrRng(){
  rules(200,300,500);Target target=new Target();TestPoison poison=new TestPoison();poison.bind(target);poison.setAttributed(10,new Rat());poison.extendAttributed(5,new Hero());
  Random.pushGenerator(72591);long next;try{next=Random.Long();}finally{Random.popGenerator();}
  Random.pushGenerator(72591);int predicted;try{predicted=poison.totalIncomingDMG();assertEquals(next,Random.Long());}finally{Random.popGenerator();}
  assertEquals(15,poison.left(),0);int before=target.HP;
  for(int i=0;i<15;i++)poison.act();assertEquals(predicted,before-target.HP);
  Target zeroTarget=new Target();TestPoison zero=new TestPoison();zero.bind(zeroTarget);zero.setAttributed(0,new Goo());assertEquals(5,zero.totalIncomingDMG());
 }
}
