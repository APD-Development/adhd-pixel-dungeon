package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.lang.reflect.Method;
import static org.junit.Assert.*;
public class RowCacheConcurrentWriterTest {
 @Rule public TemporaryFolder tmp=new TemporaryFolder();
 @Test public void overlappingStoresMustNotDeleteEachOthersCompletedCache()throws Exception{
  File root=tmp.newFolder("root"),dir=new File(root,"derived/rows");assertTrue(dir.mkdirs());
  File cache=new File(dir,"runs-same.apdr");CountDownLatch firstClosed=new CountDownLatch(1),release=new CountDownLatch(1);
  File paused=new File(cache.getPath()){
   @Override public boolean exists(){firstClosed.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("release timeout");}catch(InterruptedException e){throw new AssertionError(e);}return super.exists();}
  };
  Method write=DatasetStore.class.getDeclaredMethod("writeCache",File.class,byte[].class);write.setAccessible(true);
  ExecutorService pool=Executors.newFixedThreadPool(2);
  try{
   Future<?> first=pool.submit(()->{try{write.invoke(new DatasetStore(root),paused,new byte[]{1,2,3});}catch(Exception e){throw new RuntimeException(e);}});
   assertTrue(firstClosed.await(5,TimeUnit.SECONDS));
   Future<?> second=pool.submit(()->{try{write.invoke(new DatasetStore(root),cache,new byte[]{4,5,6});}catch(Exception e){throw new RuntimeException(e);}});
   second.get(5,TimeUnit.SECONDS);release.countDown();first.get(5,TimeUnit.SECONDS);
   assertTrue("Both completed writers must leave a usable cache",cache.isFile());
   assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(cache.toPath()));
   File[] leftovers=dir.listFiles((d,n)->n.endsWith(".tmp"));assertNotNull(leftovers);assertEquals(0,leftovers.length);
  }finally{release.countDown();pool.shutdownNow();}
 }
}

