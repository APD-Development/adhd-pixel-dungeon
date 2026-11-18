package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.watabou.utils.Bundle;
import org.junit.*;
import static org.junit.Assert.*;
public class ThrowDamageProvenanceTest{
 static class ProbeItem extends Item{OffensiveDamageOrigin observed;boolean fail;@Override protected void onThrow(int cell){observed=apdThrownOrigin();if(fail)throw new IllegalStateException("test");}OffensiveDamageOrigin pending(){return apdThrownOrigin();}}
 static class ProbeLevel extends SewerLevel{OffensiveDamageOrigin trapOrigin;CorrosiveGas trapGas;@Override public void pressCell(int cell){trapOrigin=OffensiveEffectContext.current();trapGas=new CorrosiveGas();trapGas.setStrength(7);}}
 static class ProbePotion extends Potion{OffensiveDamageOrigin effect;CorrosiveGas cloud;boolean fail;ProbePotion(){anonymous=true;}@Override public void shatter(int cell){effect=OffensiveEffectContext.current();cloud=new CorrosiveGas();cloud.setStrength(7);if(fail)throw new IllegalStateException("test");}}
 Hero hero;Level level;
 @Before public void setup()throws Exception{HeadlessTestSupport.start();hero=Dungeon.hero;level=Dungeon.level;Dungeon.hero=new Hero();ProbeLevel l=new ProbeLevel();java.lang.reflect.Method size=Level.class.getDeclaredMethod("setSize",int.class,int.class);size.setAccessible(true);size.invoke(l,5,5);java.util.Arrays.fill(l.map,Terrain.EMPTY);Dungeon.level=l;}
 @After public void cleanup(){Dungeon.hero=hero;Dungeon.level=level;assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,OffensiveEffectContext.current());}
 @Test public void transientThrowOriginClearsOnSuccessAndExceptionWithoutUsingCurUser(){ProbeItem item=new ProbeItem();item.apdThrowAttributed(12,OffensiveDamageOrigin.BOSS);assertEquals(OffensiveDamageOrigin.BOSS,item.observed);assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,item.pending());item.fail=true;try{item.apdThrowAttributed(12,OffensiveDamageOrigin.ENEMY);fail();}catch(IllegalStateException expected){}assertEquals(OffensiveDamageOrigin.ENEMY,item.observed);assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,item.pending());}
 @Test public void actualPotionThrowAttributesItsCloudButPressedTrapRemainsEnvironmental(){ProbePotion potion=new ProbePotion();try(OffensiveEffectContext ambient=OffensiveEffectContext.enter(OffensiveDamageOrigin.BOSS)){potion.apdThrowAttributed(12,OffensiveDamageOrigin.PLAYER);assertEquals(OffensiveDamageOrigin.BOSS,OffensiveEffectContext.current());}assertEquals(OffensiveDamageOrigin.PLAYER,potion.effect);ProbeLevel l=(ProbeLevel)Dungeon.level;assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,l.trapOrigin);Bundle a=new Bundle(),b=new Bundle();potion.cloud.storeInBundle(a);l.trapGas.storeInBundle(b);assertEquals("PLAYER",a.getString("apd_corrosive_gas_origin"));assertFalse(b.contains("apd_corrosive_gas_origin"));}
 @Test public void environmentalShatteringAndDrinkingKeepDistinctOrigins(){ProbePotion p=new ProbePotion();p.shatter(12);assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,p.effect);p.apply(Dungeon.hero);assertEquals(OffensiveDamageOrigin.PLAYER,p.effect);assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,OffensiveEffectContext.current());}
 @Test public void nestedContextsAndPotionFailuresRestorePreviousCause(){try(OffensiveEffectContext outer=OffensiveEffectContext.enter(OffensiveDamageOrigin.ENEMY)){try(OffensiveEffectContext inner=OffensiveEffectContext.enter(OffensiveDamageOrigin.BOSS)){assertEquals(OffensiveDamageOrigin.BOSS,OffensiveEffectContext.current());}assertEquals(OffensiveDamageOrigin.ENEMY,OffensiveEffectContext.current());ProbePotion p=new ProbePotion();p.fail=true;try{p.apdThrowAttributed(12,OffensiveDamageOrigin.PLAYER);fail();}catch(IllegalStateException expected){}assertEquals(OffensiveDamageOrigin.ENEMY,OffensiveEffectContext.current());}}
 @Test public void causticBrewUsesThrowOriginAndRetainsItAcrossSave(){
  com.shatteredpixel.shatteredpixeldungeon.actors.Char target=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();target.pos=12;
  com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(target);
  try{
   com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.CausticBrew brew=new com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.CausticBrew(){{anonymous=true;}@Override protected void splash(int cell){}};
   brew.apdThrowAttributed(12,OffensiveDamageOrigin.BOSS);
   com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze ooze=target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze.class);assertNotNull(ooze);
   Bundle saved=new Bundle();ooze.storeInBundle(saved);assertEquals("BOSS",saved.getString("apd_ooze_origin"));
   com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze restored=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze();restored.restoreFromBundle(saved);Bundle again=new Bundle();restored.storeInBundle(again);assertEquals("BOSS",again.getString("apd_ooze_origin"));
  }finally{for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff b:target.buffs())b.detach();com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(target);}
 }
 @Test public void missingOriginIsRejectedWithoutChangingExistingOoze(){
  com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze ooze=new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze();ooze.setWithOrigin(4,OffensiveDamageOrigin.PLAYER);
  try{ooze.setWithOrigin(9,null);fail();}catch(IllegalArgumentException expected){}
  Bundle saved=new Bundle();ooze.storeInBundle(saved);assertEquals("PLAYER",saved.getString("apd_ooze_origin"));assertEquals(4f,saved.getFloat("left"),0);
 }
}
