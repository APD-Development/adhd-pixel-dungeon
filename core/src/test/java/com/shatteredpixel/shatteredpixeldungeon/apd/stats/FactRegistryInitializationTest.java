package com.shatteredpixel.shatteredpixeldungeon.apd.stats;
import org.junit.Test;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
public class FactRegistryInitializationTest {
 @Test public void registeringGameFactsCannotDeadlockAConcurrentRegistryRead()throws Exception {
  CountDownLatch builderLoading=new CountDownLatch(1),releaseBuilder=new CountDownLatch(1),done=new CountDownLatch(2);
  AtomicReference<Throwable> failure=new AtomicReference<>();
  URL[] paths={FactKinds.class.getProtectionDomain().getCodeSource().getLocation(),GameFacts.class.getProtectionDomain().getCodeSource().getLocation()};
  try(URLClassLoader loader=new URLClassLoader(paths,getClass().getClassLoader()){
   @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
    synchronized(getClassLoadingLock(name)){
     Class<?> type=findLoadedClass(name);
     if(type==null&&name.startsWith("com.shatteredpixel.shatteredpixeldungeon.apd.stats.")){
      if(name.endsWith(".FactKind$Builder")&&Thread.currentThread().getName().equals("gamefacts-init")){
       builderLoading.countDown();try{if(!releaseBuilder.await(10,TimeUnit.SECONDS))throw new ClassNotFoundException("fixture gate timeout");}catch(InterruptedException e){throw new ClassNotFoundException(name,e);}
      }
      try{type=findClass(name);}catch(ClassNotFoundException ignored){}
     }
     if(type==null)type=super.loadClass(name,false);if(resolve)resolveClass(type);return type;
    }
   }
  }){
   Class<?> registry=Class.forName(FactKinds.class.getName(),true,loader);
   java.lang.reflect.Method readAll=registry.getMethod("all");
   Thread initializer=new Thread(()->{try{Class.forName(GameFacts.class.getName(),true,loader);}catch(Throwable e){failure.set(e);}finally{done.countDown();}},"gamefacts-init");
   Thread reader=new Thread(()->{try{readAll.invoke(null);}catch(Throwable e){failure.set(e);}finally{done.countDown();}},"registry-read");
   initializer.setDaemon(true);reader.setDaemon(true);initializer.start();
   assertTrue("real GameFacts initialization reached its first FactKind builder",builderLoading.await(5,TimeUnit.SECONDS));reader.start();
   long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean reading=false;
   while(!reading&&System.nanoTime()<deadline){for(StackTraceElement frame:reader.getStackTrace())if(frame.getClassName().equals(FactKinds.class.getName())&&frame.getMethodName().equals("all"))reading=true;if(!reading)Thread.sleep(5);}
   assertTrue("registry reader reached actual all() while GameFacts initializes",reading);
   releaseBuilder.countDown();
   assertTrue("GameFacts initialization and registry read must both complete, without opposite lock order",done.await(5,TimeUnit.SECONDS));
   assertNull(failure.get());
   java.util.List<?> kinds=(java.util.List<?>)readAll.invoke(null);assertTrue("built-in facts were fully registered",kinds.size()>20);
   assertNotNull(registry.getMethod("get",String.class).invoke(null,"move.step"));
  }finally{releaseBuilder.countDown();}
 }
}
