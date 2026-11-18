import org.junit.Test;
import static org.junit.Assert.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.backends.headless.HeadlessFiles;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;

// Reproduces defects in unchanged upstream source using disposable temporary files only.
public class UpstreamFileUtilsReproductionTest {
 @Test public void nestedTemporarySaveIsSkippedByRecursiveCleanup() throws Exception {
  Path root=Files.createTempDirectory("spd-cleanup-repro");
  Gdx.files=new HeadlessFiles(); FileUtils.setDefaultFileProperties(FileType.Absolute,root+"/");
  try {
   Bundle b=new Bundle();b.put("proof",123);
   FileUtils.bundleToFile("slot/nested/game.dat.spdtmp",b);
   assertTrue(Files.exists(root.resolve("slot/nested/game.dat.spdtmp")));
   assertFalse("bug: cleanup fails to see nested temp",FileUtils.cleanTempFiles());
   assertTrue("bug: nested temp remains",Files.exists(root.resolve("slot/nested/game.dat.spdtmp")));
   assertFalse("bug: original is not recovered",Files.exists(root.resolve("slot/nested/game.dat")));
  } finally {delete(root);}
 }
 @Test public void failedTempWriteCanReplaceAnExistingValidSaveWithEmptyBytes() throws Exception {
  Path root=Files.createTempDirectory("spd-write-repro");
  Gdx.files=new HeadlessFiles(); FileUtils.setDefaultFileProperties(FileType.Absolute,root+"/");
  try {
   Bundle old=new Bundle();old.put("proof",123);FileUtils.bundleToFile("game.dat",old);
   byte[] before=Files.readAllBytes(root.resolve("game.dat"));assertTrue(before.length>0);
   HeadlessFiles delegate=new HeadlessFiles();
   Gdx.files=(com.badlogic.gdx.Files)java.lang.reflect.Proxy.newProxyInstance(
    com.badlogic.gdx.Files.class.getClassLoader(),new Class<?>[]{com.badlogic.gdx.Files.class},(proxy,method,args)->{
    if(!method.getName().equals("absolute") || !((String)args[0]).endsWith(".spdtmp"))return method.invoke(delegate,args);
    String path=(String)args[0];
    return new FileHandle(path){@Override public OutputStream write(boolean append){
     OutputStream real=super.write(append);
     return new OutputStream(){public void write(int b)throws IOException{throw new IOException("controlled disk-write failure");}
      @Override public void close()throws IOException{real.close();}};
    }};
   });
   Bundle replacement=new Bundle();replacement.put("proof",456);
   FileUtils.bundleToFile("game.dat",replacement); // Bug: no IOException reaches caller.
   byte[] after=Files.readAllBytes(root.resolve("game.dat"));
   assertEquals("bug: existing valid save replaced with empty failed temp",0,after.length);
   assertFalse(Arrays.equals(before,after));
  } finally {delete(root);}
 }
 @Test public void failedBundleReadLeavesItsOwnedInputStreamOpen()throws Exception {
  class Tracked extends InputStream {boolean closed;public int read()throws IOException{throw new IOException("controlled read failure");}public void close(){closed=true;}}
  Tracked input=new Tracked();
  java.lang.reflect.Method method=FileUtils.class.getDeclaredMethod("bundleFromStream",InputStream.class);method.setAccessible(true);
  try{method.invoke(null,input);fail("expected read failure");}catch(java.lang.reflect.InvocationTargetException expected){assertTrue(expected.getCause() instanceof IOException);}
  assertFalse("bug: failure path does not close owned stream",input.closed);
 }
 private static void delete(Path root)throws IOException{try(java.util.stream.Stream<Path> paths=Files.walk(root)){for(Path p:(Iterable<Path>)paths.sorted(java.util.Comparator.reverseOrder())::iterator)Files.deleteIfExists(p);}}
}
