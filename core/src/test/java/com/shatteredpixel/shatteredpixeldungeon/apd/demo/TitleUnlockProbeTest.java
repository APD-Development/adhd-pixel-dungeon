package com.shatteredpixel.shatteredpixeldungeon.apd.demo;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.util.ArrayList;
public class TitleUnlockProbeTest {
 @Test public void renderingDoesNotWaitForCountAndRepeatedRebuildsCoalesce(){
  ArrayList<Runnable> tasks=new ArrayList<>();int[] reads={0};
  TitleUnlockProbe probe=new TitleUnlockProbe(tasks::add,root->{reads[0]++;return 2;});File root=new File("history");
  for(int i=0;i<20;i++){probe.request(root);assertFalse(probe.earned(root));}
  assertEquals(0,reads[0]);assertEquals(1,tasks.size());tasks.remove(0).run();assertEquals(1,reads[0]);
  probe.request(root);assertEquals(0,tasks.size());assertFalse(probe.earned(root));
 }
 @Test public void staleRootCannotUnlockNewHistoryAndNewRunInvalidatesCount(){
  ArrayList<Runnable> tasks=new ArrayList<>();TitleUnlockProbe probe=new TitleUnlockProbe(tasks::add,root->10);
  File a=new File("a"),b=new File("b");probe.request(a);probe.request(b);tasks.remove(0).run();assertFalse(probe.earned(b));
  tasks.remove(0).run();assertTrue(probe.earned(b));assertFalse(probe.earned(a));probe.invalidate();assertFalse(probe.earned(b));
  probe.request(b);assertEquals(1,tasks.size());
 }
 @Test public void readFailureNeverGrantsUnlockAndAllowsRetry(){
  ArrayList<Runnable> tasks=new ArrayList<>();TitleUnlockProbe probe=new TitleUnlockProbe(tasks::add,root->{throw new IllegalStateException("corrupt history");});
  File root=new File("history");probe.request(root);tasks.remove(0).run();assertFalse(probe.earned(root));probe.request(root);assertEquals(1,tasks.size());
 }
}
