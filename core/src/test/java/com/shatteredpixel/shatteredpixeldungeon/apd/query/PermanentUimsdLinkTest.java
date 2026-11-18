package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.UimsdUnlock;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.UimsdGate;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class PermanentUimsdLinkTest {
 @Rule public TemporaryFolder tmp = new TemporaryFolder();
 private static class Flags implements UimsdUnlock.Flags {
  boolean tour,unlocked,shown;
  public boolean unlocked(){return unlocked;} public void setUnlocked(){unlocked=true;}
  public boolean demoUsed(){return true;} public void setDemoUsed(){}
  public boolean tourUnlocked(){return tour;} public void setTourUnlocked(){tour=true;}
  public boolean promptPending(){return true;} public void setPromptPending(){}
  public boolean promptShown(){return shown;} public void setPromptShown(){shown=true;}
 }
 @Test public void nineTenAndVisitedRestartUseRealHistory()throws Exception {
  java.io.File root=tmp.newFolder();
  java.lang.reflect.Method annotation=UimsdGate.class.getDeclaredMethod("annotationFor",UimsdUnlock.Flags.class,java.io.File.class,boolean.class,boolean.class,String.class);
  annotation.setAccessible(true);
  for(int i=0;i<9;i++)finish(root,i);
  Flags flags=new Flags(); flags.unlocked=true;
  assertTrue(((String)annotation.invoke(null,flags,root,true,true,"")).contains("EFFECTIVELY USELESS"));
  finish(root,9);
  assertEquals("HEY, GO HERE FOR DRUGS.",annotation.invoke(null,flags,root,true,true,""));
  flags.shown=true;
  assertEquals("HEY, GO HERE FOR DRUGS.",annotation.invoke(null,flags,root,true,true,""));
  Flags restart=new Flags(); restart.shown=true;
  assertEquals("HEY, GO HERE FOR DRUGS.",annotation.invoke(null,restart,root,true,true,""));
  assertEquals("HEY, GO HERE FOR UNLIMITED DOPAMINE.",annotation.invoke(null,restart,root,true,false,""));
 }
 private void finish(java.io.File root,int i){
  SyntheticLog l=new SyntheticLog(root,"real-"+i,TelemetryVersion.CURRENT,1000L*i).start("run-"+i,0,"WARRIOR","test",0,0,false).enter(1);
  l.end(LifecycleEvents.OUTCOME_DIED,"Rat",1,1,10);l.close(true);
 }
}
