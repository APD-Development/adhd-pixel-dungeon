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

public class ArtifactDerivedDamageProvenanceTest {
 private Bundle previousRules;
 private static class Receiver extends Char {
  int received,calls; Object source;
  @Override public void damage(int amount,Object originalSource){received=amount;calls++;source=originalSource;}
 }
 @Before public void setup(){HeadlessTestSupport.start();previousRules=new Bundle();Dungeon.apdStoreCustomRules(previousRules);}
 @After public void cleanup(){Dungeon.apdRestoreCustomRules(previousRules);}
 private void rules(int player,int enemy,int boss){Bundle saved=new Bundle(),custom=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,player).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,enemy).with(CustomDungeonRules.Slider.BOSS_DAMAGE,boss).store(custom);saved.put("apd_custom_rules",custom);Dungeon.apdRestoreCustomRules(saved);}
 private static int occurrences(String text,String needle){int count=0;for(int at=0;(at=text.indexOf(needle,at))>=0;at+=needle.length())count++;return count;}
 @Test public void playerArtifactDamageUsesEveryAuthorizedPercentageExactlyOnce(){
  Hero hero=new Hero();Object source=new Object();
  for(int percentage=25;percentage<=500;percentage+=25){rules(percentage,300,400);Receiver receiver=new Receiver();CustomOffensiveHooks.raw(receiver,13,source,hero);assertEquals(Math.round(13*percentage/100f),receiver.received);assertEquals(1,receiver.calls);assertSame(source,receiver.source);}
 }
 @Test public void neutralArtifactDamagePreservesAmountSourceAndNextRng(){
  rules(100,500,500);Hero hero=new Hero();Receiver receiver=new Receiver();Object source=new Object();
  Random.pushGenerator(16101);long expected;try{expected=Random.Long();}finally{Random.popGenerator();}
  Random.pushGenerator(16101);try{CustomOffensiveHooks.raw(receiver,31,source,hero);assertEquals(expected,Random.Long());}finally{Random.popGenerator();}
  assertEquals(31,receiver.received);assertEquals(1,receiver.calls);assertSame(source,receiver.source);
 }
 @Test public void guidingLightAndThornsRouteOnlyOutgoingDamage()throws Exception{
  Path root=Paths.get(System.getProperty("apd.game.src"));String artifact=Files.readString(root.resolve("items/artifacts/Artifact.java"));String cape=Files.readString(root.resolve("items/artifacts/CapeOfThorns.java"));
  assertEquals(1,occurrences(artifact,"CustomOffensiveHooks.raw(target, 5+Dungeon.hero.lvl, GuidingLight.INSTANCE, Dungeon.hero)"));assertFalse(artifact.contains("target.damage(5+Dungeon.hero.lvl"));
  assertEquals(1,occurrences(cape,"CustomOffensiveHooks.raw(attacker, deflected, this, defender)"));assertFalse(cape.contains("attacker.damage(deflected, this)"));
  assertTrue(cape.indexOf("damage -= deflected")<cape.indexOf("CustomOffensiveHooks.raw(attacker"));
 }
 @Test public void skeletonKeyKnockbackRemainsNonDamagingMovement()throws Exception{
  Path root=Paths.get(System.getProperty("apd.game.src"));String key=Files.readString(root.resolve("items/artifacts/SkeletonKey.java"));
  assertEquals(2,occurrences(key,"WandOfBlastWave.throwChar("));assertEquals(2,occurrences(key,", 1, false, false, this)"));assertFalse(key.contains("WandOfBlastWave.throwCharAttributed("));
 }
}
