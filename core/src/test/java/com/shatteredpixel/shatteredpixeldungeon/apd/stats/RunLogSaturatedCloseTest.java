package com.shatteredpixel.shatteredpixeldungeon.apd.stats;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import static org.junit.Assert.*;
public class RunLogSaturatedCloseTest {
 @Test public void timedOutCloseStillFinishesAfterDiskResumes() throws Exception { exercise(false); }
 @Test public void closePreservesAcceptedEventsAndReportsDropsUnderFlushOverload() throws Exception { exercise(true); }
 private void exercise(boolean withEvents) throws Exception {
  File file=File.createTempFile("apd-close-overload", ".apdlog");file.delete();
  CountDownLatch gate=new CountDownLatch(1);
  RunLogWriter.Options o=new RunLogWriter.Options().queueCapacity(1);o.workerGate=gate;
  RunLogWriter w=RunLogWriter.create(file,TelemetryVersion.CURRENT,"saturated","test",0,o);
  List<Thread> flushers=new ArrayList<>();
  try {
   int accepted=0;
   if(withEvents)for(int i=0;i<100;i++)if(w.append(123,0,i,new byte[]{7}))accepted++;
   for(int i=0;i<10;i++){Thread t=new Thread(()->w.flush(1000,false));t.setDaemon(true);flushers.add(t);t.start();}
   //All requests have either queued and timed out or failed to enqueue.
   //The writer remains gated, so the control queue cannot drain yet.
   for(Thread t:flushers){t.join(2500);assertFalse(t.isAlive());}
   assertFalse(w.close(true,10));
   gate.countDown();w.workerThread().join(1000);
   assertFalse("Close must terminate writer after queued requests drain",w.workerThread().isAlive());
   ReadResult recorded=RunLogReader.read(file);
   assertEquals(ReadStatus.SEALED,recorded.status);
   assertEquals(accepted,recorded.events.stream().filter(e->e.type==123).count());
   if(withEvents){assertTrue(w.eventsDropped()>0);assertTrue(recorded.events.stream().anyMatch(e->e.type==EventTypes.GAP));}
  }finally{gate.countDown();w.workerThread().interrupt();w.workerThread().join(1000);for(Thread t:flushers)t.join(1200);file.delete();}
 }
}
