package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
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

public class RemainingArmorAbilityDamageProvenanceTest {
 private Bundle previousRules;
 @Before public void setup(){HeadlessTestSupport.start();previousRules=new Bundle();Dungeon.apdStoreCustomRules(previousRules);}
 @After public void cleanup(){Dungeon.apdRestoreCustomRules(previousRules);}
 private void rules(int player,int enemy,int boss){Bundle saved=new Bundle(),custom=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,player).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,enemy).with(CustomDungeonRules.Slider.BOSS_DAMAGE,boss).store(custom);saved.put("apd_custom_rules",custom);Dungeon.apdRestoreCustomRules(saved);}
 private static int occurrences(String text,String needle){int count=0;for(int at=0;(at=text.indexOf(needle,at))>=0;at+=needle.length())count++;return count;}
 @Test public void preMitigationAbilityDamageUsesEveryPercentageAndKeepsDefenseFixed(){
  Hero hero=new Hero();
  for(int percentage=25;percentage<=500;percentage+=25){rules(percentage,300,400);int result=Math.round(CustomOffensiveHooks.attackDamage(hero,31))-7;assertEquals(Math.round(31*percentage/100f)-7,result);}
 }
 @Test public void neutralPreMitigationAbilityDamagePreservesAmountAndNextRng(){
  rules(100,500,500);Hero hero=new Hero();Random.pushGenerator(15901);long expected;try{expected=Random.Long();}finally{Random.popGenerator();}
  Random.pushGenerator(15901);try{assertEquals(31,Math.round(CustomOffensiveHooks.attackDamage(hero,31)));assertEquals(expected,Random.Long());}finally{Random.popGenerator();}
 }
 @Test public void heroicLeapScalesBeforeArmorAndCapturesItsDelayedCollision()throws Exception{
  Path root=Paths.get(System.getProperty("apd.game.src"));String leap=Files.readString(root.resolve("actors/hero/abilities/warrior/HeroicLeap.java"));
  assertEquals(1,occurrences(leap,"CustomOffensiveHooks.attackDamage(hero, damage)"));assertTrue(leap.indexOf("CustomOffensiveHooks.attackDamage(hero, damage)")<leap.indexOf("damage -= mob.apdDrRoll()"));
  assertEquals(1,occurrences(leap,"WandOfBlastWave.throwCharAttributed("));assertEquals(1,occurrences(leap,"OffensiveDamageOrigin.capture(hero, null)"));
  assertTrue(leap.contains("mob.damage(damage, hero);"));assertFalse(leap.contains("WandOfBlastWave.throwChar(mob, trajectory"));
 }
 @Test public void shockwaveScalesOnceBeforeArmorForBothExistingBranches()throws Exception{
  Path root=Paths.get(System.getProperty("apd.game.src"));String shock=Files.readString(root.resolve("actors/hero/abilities/warrior/Shockwave.java"));
  assertEquals(1,occurrences(shock,"CustomOffensiveHooks.attackDamage(hero, damage)"));assertTrue(shock.indexOf("CustomOffensiveHooks.attackDamage(hero, damage)")<shock.indexOf("damage -= ch.apdDrRoll()"));
  assertEquals(2,occurrences(shock,"ch.damage(damage, hero);"));assertFalse(shock.contains("CustomOffensiveHooks.raw(ch, damage"));
 }
 @Test public void telefragScalesOnlyTheEnemyAndOtherAbilitiesKeepVerifiedDelegates()throws Exception{
  Path root=Paths.get(System.getProperty("apd.game.src"));
  String warp=Files.readString(root.resolve("actors/hero/abilities/mage/WarpBeacon.java"));String wild=Files.readString(root.resolve("actors/hero/abilities/mage/WildMagic.java"));String blades=Files.readString(root.resolve("actors/hero/abilities/huntress/SpectralBlades.java"));String hawk=Files.readString(root.resolve("actors/hero/abilities/huntress/SpiritHawk.java"));String clone=Files.readString(root.resolve("actors/hero/abilities/rogue/ShadowClone.java"));
  assertEquals(1,occurrences(warp,"CustomOffensiveHooks.raw(existing, damage, WarpBeacon.this, hero)"));assertTrue(warp.contains("hero.damage(Math.min(heroDmg, heroHP-1), WarpBeacon.this);"));assertFalse(warp.contains("existing.damage(damage, WarpBeacon.this);"));
  assertFalse(wild.contains(".damage("));assertTrue(blades.contains("hero.attack( ch, dmgMulti, 0, accmulti )"));assertTrue(hawk.contains("ally.apdInheritOffensiveOrigin(hero)"));assertTrue(clone.contains("ally.apdInheritOffensiveOrigin(hero)"));
 }
}
