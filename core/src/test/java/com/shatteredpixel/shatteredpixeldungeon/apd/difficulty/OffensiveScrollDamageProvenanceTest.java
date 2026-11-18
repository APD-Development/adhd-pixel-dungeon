package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class OffensiveScrollDamageProvenanceTest {
 private Bundle previousRules;
 private static class Receiver extends Char {
  int received,calls; Object source;
  @Override public void damage(int amount,Object originalSource){received=amount;calls++;source=originalSource;}
 }
 @Before public void setup(){HeadlessTestSupport.start();previousRules=new Bundle();Dungeon.apdStoreCustomRules(previousRules);}
 @After public void cleanup(){Dungeon.apdRestoreCustomRules(previousRules);}
 private void rules(int player,int enemy,int boss){Bundle saved=new Bundle(),custom=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,player).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,enemy).with(CustomDungeonRules.Slider.BOSS_DAMAGE,boss).store(custom);saved.put("apd_custom_rules",custom);Dungeon.apdRestoreCustomRules(saved);}
 private static int occurrences(String text,String needle){int count=0;for(int at=0;(at=text.indexOf(needle,at))>=0;at+=needle.length())count++;return count;}
 @Test public void playerScrollDamageUsesEveryAuthorizedPercentageExactlyOnce(){
  Hero hero=new Hero();Object source=new Object();
  for(int percentage=25;percentage<=500;percentage+=25){rules(percentage,300,400);Receiver receiver=new Receiver();CustomOffensiveHooks.raw(receiver,19,source,hero);assertEquals(Math.round(19*percentage/100f),receiver.received);assertEquals(1,receiver.calls);assertSame(source,receiver.source);}
 }
 @Test public void neutralScrollDamagePreservesAmountSourceAndNextRng(){
  rules(100,500,500);Hero hero=new Hero();Receiver receiver=new Receiver();Object source=new Object();
  Random.pushGenerator(16001);long expected;try{expected=Random.Long();}finally{Random.popGenerator();}
  Random.pushGenerator(16001);try{CustomOffensiveHooks.raw(receiver,29,source,hero);assertEquals(expected,Random.Long());}finally{Random.popGenerator();}
  assertEquals(29,receiver.received);assertEquals(1,receiver.calls);assertSame(source,receiver.source);
 }
 @Test public void retributionRoutesOnlyEnemyDamageThroughPlayerBoundary()throws Exception{
  Path root=Paths.get(System.getProperty("apd.game.src"));String source=Files.readString(root.resolve("items/scrolls/ScrollOfRetribution.java"));
  assertEquals(1,occurrences(source,"CustomOffensiveHooks.raw(mob, Math.round(mob.HT/10f + (mob.HP * power * 0.225f)), this, curUser)"));
  assertFalse(source.contains("mob.damage(Math.round(mob.HT/10f"));
 }
 @Test public void psionicBlastRoutesEnemyDamageButKeepsHeroSelfCostUnscaled()throws Exception{
  Path root=Paths.get(System.getProperty("apd.game.src"));String source=Files.readString(root.resolve("items/scrolls/exotic/ScrollOfPsionicBlast.java"));
  assertEquals(1,occurrences(source,"CustomOffensiveHooks.raw(mob, Math.round(mob.HT/2f + mob.HP/2f), this, curUser)"));
  assertEquals(1,occurrences(source,"curUser.damage(Math.max(0, Math.round(curUser.HT*(0.5f * (float)Math.pow(0.9, targets.size())))), this)"));
  assertFalse(source.contains("mob.damage(Math.round(mob.HT/2f"));
  assertFalse(source.contains("CustomOffensiveHooks.raw(curUser,"));
 }
}
