package com.shatteredpixel.shatteredpixeldungeon.apd.stats;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;
public class DerivedCacheConcurrentWriterTest {
 @Rule public TemporaryFolder tmp=new TemporaryFolder();
 @Test public void overlappingFactStoresMustLeaveACompleteCache()throws Exception{
  File root=tmp.newFolder("facts");DerivedStore firstStore=new DerivedStore(root),secondStore=new DerivedStore(root);
  assertTrue(firstStore.cacheFile().getParentFile().mkdirs());
  CountDownLatch opened=new CountDownLatch(1),release=new CountDownLatch(1);
  List<String> firstPrints=new AbstractList<String>(){
   @Override public int size(){return 1;}
   @Override public String get(int index){if(index!=0)throw new IndexOutOfBoundsException();opened.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("release timeout");}catch(InterruptedException e){throw new AssertionError(e);}return "first";}
  };
  Method write=DerivedStore.class.getDeclaredMethod("writeCache",FactTable.class,List.class);write.setAccessible(true);
  ExecutorService pool=Executors.newFixedThreadPool(2);
  try{
   Future<?> first=pool.submit(()->{try{write.invoke(firstStore,new FactTable(),firstPrints);}catch(Exception e){throw new RuntimeException(e);}});
   assertTrue(opened.await(5,TimeUnit.SECONDS));
   Future<?> second=pool.submit(()->{try{write.invoke(secondStore,new FactTable(),Collections.singletonList("second"));}catch(Exception e){throw new RuntimeException(e);}});
   second.get(5,TimeUnit.SECONDS);release.countDown();first.get(5,TimeUnit.SECONDS);
   assertTrue("Both completed writers must leave a usable fact cache",firstStore.cacheFile().isFile());
   Method read=DerivedStore.class.getDeclaredMethod("readCache",File.class,List.class);read.setAccessible(true);
   assertNotNull(read.invoke(firstStore,firstStore.cacheFile(),Collections.singletonList("first")));
   File[] leftovers=firstStore.cacheFile().getParentFile().listFiles((d,n)->n.endsWith(".tmp"));assertNotNull(leftovers);assertEquals(0,leftovers.length);
  }finally{release.countDown();pool.shutdownNow();}
 }
}

