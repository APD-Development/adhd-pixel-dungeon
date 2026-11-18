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

public class PlayerAbilityDamageProvenanceTest {
 private Bundle previousRules;
 private static class Receiver extends Char {
  int received,calls; Object source;
  @Override public void damage(int amount,Object originalSource){received=amount;calls++;source=originalSource;}
 }
 @Before public void setup(){HeadlessTestSupport.start();previousRules=new Bundle();Dungeon.apdStoreCustomRules(previousRules);}
 @After public void cleanup(){Dungeon.apdRestoreCustomRules(previousRules);}
 private void rules(int player,int enemy,int boss){Bundle saved=new Bundle(),custom=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,player).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,enemy).with(CustomDungeonRules.Slider.BOSS_DAMAGE,boss).store(custom);saved.put("apd_custom_rules",custom);Dungeon.apdRestoreCustomRules(saved);}
 private static int occurrences(String text,String needle){int count=0;for(int at=0;(at=text.indexOf(needle,at))>=0;at+=needle.length())count++;return count;}
 @Test public void concretePlayerAbilityBoundaryUsesEveryAuthorizedPercentageOnce(){
  Hero hero=new Hero();Object source=new Object();
  for(int percentage=25;percentage<=500;percentage+=25){rules(percentage,300,400);Receiver receiver=new Receiver();CustomOffensiveHooks.raw(receiver,17,source,hero);assertEquals(Math.round(17*percentage/100f),receiver.received);assertEquals(1,receiver.calls);assertSame(source,receiver.source);}
 }
 @Test public void neutralPlayerAbilityPreservesAmountSourceAndNextRng(){
  rules(100,500,500);Hero hero=new Hero();Receiver receiver=new Receiver();Object source=new Object();
  Random.pushGenerator(15701);long expected;try{expected=Random.Long();}finally{Random.popGenerator();}
  Random.pushGenerator(15701);try{CustomOffensiveHooks.raw(receiver,23,source,hero);assertEquals(expected,Random.Long());}finally{Random.popGenerator();}
  assertEquals(23,receiver.received);assertEquals(1,receiver.calls);assertSame(source,receiver.source);
 }
 @Test public void elementalBlastAndStrikeDeclareEveryKnownSecondaryBoundary()throws Exception{
  Path root=Paths.get(System.getProperty("apd.game.src"));String blast=Files.readString(root.resolve("actors/hero/abilities/mage/ElementalBlast.java"));String strike=Files.readString(root.resolve("actors/hero/abilities/duelist/ElementalStrike.java"));
  assertEquals(2,occurrences(blast,"CustomOffensiveHooks.raw(mob, damage, Reflection.newInstance(finalWandCls), hero)"));assertEquals(1,occurrences(blast,"Burning.affectAttributed(mob, hero)"));assertEquals(1,occurrences(blast,"WandOfBlastWave.throwCharAttributed("));
  assertEquals(6,occurrences(strike,"CustomOffensiveHooks.raw(ch,"));assertEquals(2,occurrences(strike,"WandOfBlastWave.throwCharAttributed("));assertEquals(1,occurrences(strike,"apdExplodeAttributed("));
  assertFalse(blast.contains("Buff.affect( mob, Burning.class ).reignite( mob );"));assertFalse(strike.contains("new Bomb.ConjuredBomb().explode(exploding.pos)"));assertFalse(strike.contains("ch.damage( ch.HP, Grim.class );"));
 }
 @Test public void blastWaveCapturesBeforeDelayedCollisionAndKeepsLegacyCallersUnowned()throws Exception{
  Path root=Paths.get(System.getProperty("apd.game.src"));String wave=Files.readString(root.resolve("items/wands/WandOfBlastWave.java"));
  assertEquals(4,occurrences(wave,"throwCharAttributed("));assertEquals(2,occurrences(wave,"OffensiveDamageOrigin.capture(curUser, null)"));assertEquals(1,occurrences(wave,"OffensiveDamageOrigin.UNATTRIBUTED"));assertEquals(1,occurrences(wave,"OffensiveDamage.raw("));assertTrue(wave.contains("new Knockback(), damageOrigin).apply(ch);"));assertFalse(wave.contains("ch.damage(Random.NormalIntRange(finalDist, 2*finalDist), new Knockback());"));
 }
}
