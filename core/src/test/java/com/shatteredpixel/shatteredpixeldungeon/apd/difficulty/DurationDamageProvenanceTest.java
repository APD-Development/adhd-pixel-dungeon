package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class DurationDamageProvenanceTest {
 @After public void clear(){Dungeon.apdRestoreCustomRules(new Bundle());}
 private void rules(int p,int e,int b){Bundle all=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,p).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,e).with(CustomDungeonRules.Slider.BOSS_DAMAGE,b).store(r);all.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(all);}
 @Test public void neutralExtendedEffectsKeepEveryTickAndNextRngExactly(){
  rules(100,100,100);DurationDamageProvenance profile=new DurationDamageProvenance();profile.reset(10,OffensiveDamageOrigin.ENEMY);profile.extend(5,OffensiveDamageOrigin.PLAYER);
  Random.pushGenerator(1837);long expected;try{expected=Random.Long();}finally{Random.popGenerator();}
  Random.pushGenerator(1837);try{for(float left=15;left>0;left--){assertEquals((int)(left/3)+1,profile.scaled(left));profile.consume(1);}assertEquals(expected,Random.Long());}finally{Random.popGenerator();}
 }
 @Test public void mixedExtensionScalesOnlyItsAdditionalDamageAndBossDoesNotUseEnemyFactor(){
  rules(200,300,500);DurationDamageProvenance profile=new DurationDamageProvenance();profile.reset(10,OffensiveDamageOrigin.ENEMY);profile.extend(5,OffensiveDamageOrigin.PLAYER);
  // Original duration10 causes4; extending to15 adds2. 4*3 +2*2 =16, not6*both factors.
  assertEquals(16,profile.scaled(15));profile.consume(1);assertEquals(14,profile.scaled(14));
  DurationDamageProvenance boss=new DurationDamageProvenance();boss.reset(10,OffensiveDamageOrigin.BOSS);assertEquals(20,boss.scaled(10));
 }
 @Test public void saveLoadAndSeepingCopyPreserveContributorsAndLegacyRemainsUnknown(){
  rules(200,300,500);DurationDamageProvenance profile=new DurationDamageProvenance();profile.reset(10,OffensiveDamageOrigin.ENEMY);profile.extend(5,OffensiveDamageOrigin.PLAYER);
  Bundle b=new Bundle();profile.store(b,"poison");DurationDamageProvenance loaded=DurationDamageProvenance.restore(b,"poison",15);assertEquals(profile.scaled(15),loaded.scaled(15));
  DurationDamageProvenance clone=loaded.copyFor(15,2);assertEquals(3,clone.scaled(2));Bundle c=new Bundle();clone.store(c,"poison");assertEquals(3,DurationDamageProvenance.restore(c,"poison",2).scaled(2));
  assertNull(DurationDamageProvenance.restore(new Bundle(),"poison",15));
  DurationDamageProvenance old=new DurationDamageProvenance();old.reset(15,OffensiveDamageOrigin.UNATTRIBUTED);assertEquals(6,old.scaled(15));
 }
 @Test public void corruptPartialOrMismatchedMetadataRejectsWithoutOverwriting(){
  Bundle b=new Bundle();b.put("p_version",1);assertThrows(IllegalArgumentException.class,()->DurationDamageProvenance.restore(b,"p",10));assertEquals(1,b.getInt("p_version"));
  DurationDamageProvenance profile=new DurationDamageProvenance();profile.reset(10,OffensiveDamageOrigin.PLAYER);profile.store(b,"p");assertThrows(IllegalArgumentException.class,()->DurationDamageProvenance.restore(b,"p",20));
  b.put("p_weights",new String[]{"NaN","0","0","0"});assertThrows(IllegalArgumentException.class,()->DurationDamageProvenance.restore(b,"p",10));
 }

 @Test public void everyPermittedFactorAndFractionalDurationRoundtripPreservesItsRole(){
  for(int percent=25;percent<=500;percent+=25){rules(percent,percent,percent);
   for(OffensiveDamageOrigin role:OffensiveDamageOrigin.values())for(float left:new float[]{0,0.3f,1,2.5f,15,100}){
    DurationDamageProvenance profile=new DurationDamageProvenance();profile.reset(left,role);int expected=Math.round(((int)(left/3)+1)*(role==OffensiveDamageOrigin.UNATTRIBUTED?1:percent/100f));
    assertEquals(expected,profile.scaled(left));Bundle b=new Bundle();profile.store(b,"p");assertEquals(expected,DurationDamageProvenance.restore(b,"p",left).scaled(left));
   }
  }
  rules(200,300,500);DurationDamageProvenance profile=new DurationDamageProvenance();profile.reset(10,OffensiveDamageOrigin.ENEMY);profile.extend(5,OffensiveDamageOrigin.PLAYER);profile.shorten(5);assertEquals(12,profile.scaled(10));
 }
}
