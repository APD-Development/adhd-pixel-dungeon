package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import org.junit.Test;
import static org.junit.Assert.*;
public class OffensiveInvocationTest {
 @Test public void delayedCallbackUsesCastTimeOriginEvenAfterAllegianceChangesAndReleasesIt(){
  Rat actor=new Rat();OffensiveDamageOrigin captured=OffensiveDamageOrigin.capture(actor,null);actor.alignment=Char.Alignment.ALLY;
  try(OffensiveInvocation scope=OffensiveInvocation.enter(actor,captured)){assertEquals(OffensiveDamageOrigin.ENEMY,OffensiveDamageOrigin.capture(actor,null));}
  assertNull(OffensiveInvocation.captured(actor));assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,OffensiveDamageOrigin.capture(actor,null));
 }
 @Test public void nestedAndThrowingCallbacksRestorePreviousScopeAndDoNotLeakAcrossThreads()throws Exception {
  Rat first=new Rat(),second=new Rat();
  try(OffensiveInvocation outer=OffensiveInvocation.enter(first,OffensiveDamageOrigin.PLAYER)){
   assertThrows(IllegalStateException.class,()->{try(OffensiveInvocation inner=OffensiveInvocation.enter(first,OffensiveDamageOrigin.BOSS)){assertEquals(OffensiveDamageOrigin.BOSS,OffensiveDamageOrigin.capture(first,null));throw new IllegalStateException("callback failed");}});
   assertEquals(OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.capture(first,null));
   try(OffensiveInvocation other=OffensiveInvocation.enter(second,OffensiveDamageOrigin.BOSS)){assertEquals(OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.capture(first,null));}
   java.util.concurrent.atomic.AtomicReference<OffensiveDamageOrigin> result=new java.util.concurrent.atomic.AtomicReference<>();Thread t=new Thread(()->result.set(OffensiveDamageOrigin.capture(first,null)));t.start();t.join();assertEquals(OffensiveDamageOrigin.ENEMY,result.get());
  }
  assertNull(OffensiveInvocation.captured(first));
 }
}
