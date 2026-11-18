import org.junit.Test;
import org.junit.Before;
import org.junit.After;
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

// Save safety regression coverage uses disposable temporary files only.
public class FileUtilsSaveSafetyTest {
 private com.badlogic.gdx.Files previousFiles;
 private FileType previousType;
 private String previousPath;
 @Before public void rememberFileServices()throws Exception {
  previousFiles=Gdx.files;
  java.lang.reflect.Field type=FileUtils.class.getDeclaredField("defaultFileType");type.setAccessible(true);previousType=(FileType)type.get(null);
  java.lang.reflect.Field path=FileUtils.class.getDeclaredField("defaultPath");path.setAccessible(true);previousPath=(String)path.get(null);
 }
 @After public void restoreFileServices(){
  Gdx.files=previousFiles;FileUtils.setDefaultFileProperties(previousType,previousPath);
 }
 @Test public void nestedTemporarySaveIsRecoveredByRecursiveCleanup() throws Exception {
  Path root=Files.createTempDirectory("spd-cleanup-repro");
  Gdx.files=new HeadlessFiles(); FileUtils.setDefaultFileProperties(FileType.Absolute,root+"/");
  try {
   Bundle b=new Bundle();b.put("proof",123);
   FileUtils.bundleToFile("slot/nested/game.dat.spdtmp",b);
   assertTrue(Files.exists(root.resolve("slot/nested/game.dat.spdtmp")));
   assertTrue(FileUtils.cleanTempFiles());
   assertFalse(Files.exists(root.resolve("slot/nested/game.dat.spdtmp")));
   assertEquals(123,FileUtils.bundleFromFile("slot/nested/game.dat").getInt("proof"));
  } finally {delete(root);}
 }
 @Test public void failedTemporaryWritePreservesTheExistingValidSave() throws Exception {
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
   try {FileUtils.bundleToFile("game.dat",replacement);fail("write must fail");} catch(IOException expected) {}
   byte[] after=Files.readAllBytes(root.resolve("game.dat"));
   assertArrayEquals(before,after);
   assertEquals(123,FileUtils.bundleFromFile("game.dat").getInt("proof"));
  } finally {delete(root);}
 }
 @Test public void failedBundleReadClosesItsOwnedInputStream()throws Exception {
  class Tracked extends InputStream {boolean closed;public int read()throws IOException{throw new IOException("controlled read failure");}public void close(){closed=true;}}
  Tracked input=new Tracked();
  java.lang.reflect.Method method=FileUtils.class.getDeclaredMethod("bundleFromStream",InputStream.class);method.setAccessible(true);
  try{method.invoke(null,input);fail("expected read failure");}catch(java.lang.reflect.InvocationTargetException expected){assertTrue(expected.getCause() instanceof IOException);}
  assertTrue("owned stream closes on read failure",input.closed);
 }
 @Test public void successfulSaveAndReplacementRemainReadable()throws Exception {
  Path root=Files.createTempDirectory("apd-save-roundtrip");
  Gdx.files=new HeadlessFiles();FileUtils.setDefaultFileProperties(FileType.Absolute,root+"/");
  try {Bundle first=new Bundle();first.put("proof",123);FileUtils.bundleToFile("game.dat",first);
   assertEquals(123,FileUtils.bundleFromFile("game.dat").getInt("proof"));
   Bundle second=new Bundle();second.put("proof",456);FileUtils.bundleToFile("game.dat",second);
   assertEquals(456,FileUtils.bundleFromFile("game.dat").getInt("proof"));
   assertFalse(Files.exists(root.resolve("game.dat.spdtmp")));
  }finally{delete(root);}
 }
 private static void delete(Path root)throws IOException{try(java.util.stream.Stream<Path> paths=Files.walk(root)){for(Path p:(Iterable<Path>)paths.sorted(java.util.Comparator.reverseOrder())::iterator)Files.deleteIfExists(p);}}
}
