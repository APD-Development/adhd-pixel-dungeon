package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import java.nio.file.*;
import static org.junit.Assert.*;
public class DragonBreathProvenanceTest {
 FireCallerProvenanceTest fixture=new FireCallerProvenanceTest();
 @Before public void setup()throws Exception{fixture.setup();}
 @After public void cleanup()throws Exception{fixture.cleanup();}
 int attributedHit(OffensiveDamageOrigin origin,int percentage)throws Exception{
  fixture.fixture.rules(origin==OffensiveDamageOrigin.PLAYER?percentage:275,origin==OffensiveDamageOrigin.ENEMY?percentage:325,origin==OffensiveDamageOrigin.BOSS?percentage:425);
  SpecialBombDamageProvenanceTest.Target target=new SpecialBombDamageProvenanceTest.Target(false);Actor.add(target);
  try{
   Burning burning=Buff.affect(target,Burning.class);burning.reigniteAttributedOrigin(target,origin);
   Bundle state=new Bundle();burning.storeInBundle(state);state.put("next_dmg",4);burning.restoreFromBundle(state);
   burning.act();assertEquals(1,target.hits.size());assertSame(burning,target.sources.get(0));return target.hits.get(0);
  }finally{Actor.remove(target);}
 }
 @Test public void explicitDragonBreathOriginScalesEveryRoleAcrossTheWholeRange()throws Exception{
  for(OffensiveDamageOrigin origin:new OffensiveDamageOrigin[]{OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.ENEMY,OffensiveDamageOrigin.BOSS})
   for(int percentage=25;percentage<=500;percentage+=25)
    assertEquals(Math.round(4*percentage/100f),attributedHit(origin,percentage));
 }
 @Test public void legacyAndNullDragonBreathOriginsStayEnvironmental()throws Exception{
  fixture.fixture.rules(500,500,500);
  assertEquals(4,attributedHit(OffensiveDamageOrigin.UNATTRIBUTED,100));
  assertEquals(4,attributedHit(null,100));
 }
 @Test public void neutralExplicitOriginPreservesBurningDamageAndNextRng()throws Exception{
  fixture.fixture.rules(100,100,100);
  long attributedNext,legacyNext;int attributedDamage,legacyDamage;
  Random.pushGenerator(914);try{attributedDamage=attributedHit(OffensiveDamageOrigin.PLAYER,100);attributedNext=Random.Long();}finally{Random.popGenerator();}
  Random.pushGenerator(914);try{legacyDamage=attributedHit(OffensiveDamageOrigin.UNATTRIBUTED,100);legacyNext=Random.Long();}finally{Random.popGenerator();}
  assertEquals(legacyDamage,attributedDamage);assertEquals(legacyNext,attributedNext);
 }
 @Test public void realPotionCapturesOnceBeforeAnimationAndUsesOneOriginForFieldsAndBurning()throws Exception{
  Path source=Paths.get(System.getProperty("apd.game.src")).resolve("items/potions/exotic/PotionOfDragonsBreath.java");
  String text=Files.readString(source);
  assertTrue(text.contains("final OffensiveDamageOrigin apdOrigin = OffensiveDamageOrigin.capture(curUser, null);"));
  assertTrue(text.contains("Fire.seedAttributed( cell, 5, apdOrigin )"));
  assertTrue(text.contains("Fire.seedAttributed( cell+i, 5, apdOrigin )"));
  assertTrue(text.contains("Burning.affectAttributedOrigin(ch, apdOrigin)"));
  assertFalse(text.contains("Blob.seed( cell, 5, Fire.class )"));
  assertFalse(text.contains("Buff.affect( ch, Burning.class ).reignite( ch )"));
 }
}
