package com.shatteredpixel.shatteredpixeldungeon.apd.replay;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import org.junit.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

public class ReplayIdleBarrierTest {
 @Before public void setup(){HeadlessTestSupport.start();FloorReplay.setLiveGameCheck(null);}
 @After public void cleanup(){FloorReplay.setLiveGameCheck(null);}
 private void heldReplay(boolean interruptCaller) throws Exception {
  CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1),entered=new CountDownLatch(1),returned=new CountDownLatch(1);
  AtomicReference<Throwable> failure=new AtomicReference<>();AtomicBoolean interruptedAfter=new AtomicBoolean();
  Thread replay=new Thread(()->{try{FloorReplay.replay(4919,0,HeroClass.WARRIOR,1,(depth,level)->{
   held.countDown();try{if(!release.await(15,TimeUnit.SECONDS))throw new AssertionError("fixture timeout");}
   catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
  });}catch(Throwable t){failure.compareAndSet(null,t);}},"held-floor-replay");
  Thread caller=new Thread(()->{try{
   if(interruptCaller)Thread.currentThread().interrupt();entered.countDown();
   FloorReplay.awaitIdle();interruptedAfter.set(Thread.currentThread().isInterrupted());
  }catch(Throwable t){failure.compareAndSet(null,t);}finally{returned.countDown();}},"game-load-wait");
  replay.setDaemon(true);caller.setDaemon(true);replay.start();
  try{
   assertTrue("real replay reached its observer",held.await(15,TimeUnit.SECONDS));
   assertTrue(FloorReplay.isRunning());caller.start();assertTrue(entered.await(5,TimeUnit.SECONDS));
   assertFalse("loading must not proceed while replay owns dungeon state",returned.await(150,TimeUnit.MILLISECONDS));
   release.countDown();assertTrue(returned.await(15,TimeUnit.SECONDS));replay.join(15000);
   assertFalse(FloorReplay.isRunning());assertEquals(interruptCaller,interruptedAfter.get());assertNull(failure.get());
  }finally{release.countDown();replay.join(15000);if(caller.isAlive())caller.join(15000);}
 }
 @Test public void interruptedLoadWaitCannotBypassActiveReplayRestoration() throws Exception{heldReplay(true);}
 @Test public void normalLoadWaitAlsoWaitsForActiveReplayRestoration() throws Exception{heldReplay(false);}
 @Test public void anIdleWaitPreservesExistingInterruptStatus() throws Exception{
  AtomicBoolean kept=new AtomicBoolean();Thread t=new Thread(()->{Thread.currentThread().interrupt();FloorReplay.awaitIdle();kept.set(Thread.currentThread().isInterrupted());});
  t.start();t.join(10000);assertFalse(t.isAlive());assertTrue(kept.get());
 }
}
