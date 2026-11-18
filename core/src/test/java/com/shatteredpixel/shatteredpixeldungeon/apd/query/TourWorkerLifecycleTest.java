package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKinds;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

public class TourWorkerLifecycleTest {
 @Rule public TemporaryFolder folder=new TemporaryFolder();
 private final Map<Field,Object> saved=new LinkedHashMap<>();
 private Object scene;
 private Field field(String key)throws Exception{Field f=UimsdScene.class.getDeclaredField(key);f.setAccessible(true);return f;}
 private Object get(String key)throws Exception{return field(key).get(null);}
 private void set(String key,Object value)throws Exception{field(key).set(null,value);}
 @Before public void setup()throws Exception{
  HeadlessTestSupport.start();FactKinds.all();
  for(String key:new String[]{"mainRoot","mainModel","model","rulesId","rulesLabel","tutorial","tutorialPreparing","showingSample","sampleRoot","atHome"}){
   Field f=field(key);saved.put(f,f.get(null));
  }
  File root=folder.newFolder("real");UimsdModel original=new UimsdModel("r1-test","fixture");
  original.setCatalog(new DatasetStore(root).load());set("mainRoot",root);set("mainModel",original);set("model",original);
  set("rulesId","r1-test");set("rulesLabel","fixture");set("tutorial",null);set("tutorialPreparing",false);set("showingSample",false);set("sampleRoot",null);set("atHome",true);
  Class<?> unsafe=Class.forName("sun.misc.Unsafe");Field singleton=unsafe.getDeclaredField("theUnsafe");singleton.setAccessible(true);
  scene=unsafe.getMethod("allocateInstance",Class.class).invoke(singleton.get(null),UimsdScene.class);
 }
 @After public void restore()throws Exception{
  Method end=UimsdScene.class.getDeclaredMethod("endTutorial");end.setAccessible(true);end.invoke(null);
  for(Map.Entry<Field,Object> value:saved.entrySet())value.getKey().set(null,value.getValue());
 }
 private void prepare(boolean cancel)throws Exception{
  Object original=get("model");Method begin=UimsdScene.class.getDeclaredMethod("beginTour",int.class,boolean.class);begin.setAccessible(true);
  Thread worker=null;
  synchronized(FactKinds.class){
   begin.invoke(scene,0,false);long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
   boolean blocked=false;
   while(System.nanoTime()<deadline&&!blocked){
    for(Thread thread:Thread.getAllStackTraces().keySet()){
     if(thread.getName().equals("apd-uimsd-tour"))worker=thread;
     if(thread.getName().equals("uimsd-load")&&thread.getState()==Thread.State.BLOCKED)
      for(StackTraceElement frame:thread.getStackTrace())if(frame.getClassName().equals(FactKinds.class.getName()))blocked=true;
    }
    if(!blocked)Thread.sleep(10);
   }
   assertTrue("actual tour catalog loading is held by the registry",blocked);assertNotNull(worker);
   if(cancel){Method end=UimsdScene.class.getDeclaredMethod("endTutorial");end.setAccessible(true);end.invoke(null);assertSame(original,get("model"));}
  }
  worker.join(15000);assertFalse("tour worker finishes",worker.isAlive());
  if(cancel){assertSame("cancelled worker must not replace the player's model",original,get("model"));assertNull(get("tutorial"));assertEquals(false,get("showingSample"));}
  else{assertNotSame(original,get("model"));assertNotNull(get("tutorial"));assertEquals(true,get("showingSample"));}
  assertEquals(false,get("tutorialPreparing"));
 }
 @Test public void cancelledTourWorkerCannotPublishOverThePlayersModel()throws Exception{prepare(true);}
 @Test public void currentTourWorkerStillPublishesItsSampleModel()throws Exception{prepare(false);}
}

