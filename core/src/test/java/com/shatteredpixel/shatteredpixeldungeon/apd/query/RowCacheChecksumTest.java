package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class RowCacheChecksumTest {
 @Rule public TemporaryFolder history=new TemporaryFolder();
 private Map<String,DatasetDef> definitions(){Map<String,DatasetDef> d=new LinkedHashMap<>();d.put("things",QuerySupport.def());return d;}
 private byte[] encoded(){DatasetDef d=QuerySupport.def();LogRows lr=new LogRows();lr.rows.put(d.id,Arrays.asList(QuerySupport.row(d,"row-marker","visible-name",10L,0L,0L,true,"r")));return RowCodec.encode(lr,5,definitions());}
 private byte[] flipText(byte[] input,String marker){byte[] wanted=marker.getBytes(StandardCharsets.UTF_8);outer:for(int i=0;i<=input.length-wanted.length;i++){for(int j=0;j<wanted.length;j++)if(input[i+j]!=wanted[j])continue outer;byte[] output=input.clone();output[i]^=1;return output;}throw new AssertionError("marker missing");}
 @Test public void validUtf8BitFlipCannotSilentlyChangeCachedValues(){assertNull(RowCodec.decode(flipText(encoded(),"visible-name"),5,definitions()));}
 @Test public void everySingleBitChangeInEachCacheByteIsRejected(){byte[] valid=encoded();assertNotNull(RowCodec.decode(valid,5,definitions()));for(int i=0;i<valid.length;i++)for(int bit=0;bit<8;bit++){byte[] bad=valid.clone();bad[i]^=(1<<bit);assertNull("changed byte "+i+" bit "+bit,RowCodec.decode(bad,5,definitions()));}}
 private File[] createHistory() throws Exception {
  SyntheticLog log=new SyntheticLog(history.getRoot(),"real-source",new TelemetryVersion(1,4),100);
  log.start("run-source",0,"WARRIOR","r1-aaaaaaaaaaaa",0,30,false).end(LifecycleEvents.OUTCOME_DIED,"Rat",1,1,0).close(true);
  DatasetStore store=new DatasetStore(history.getRoot());store.load();File[] cached=store.rowsDir().listFiles((dir,name)->name.endsWith(".apdr"));assertNotNull(cached);assertEquals(1,cached.length);
  return new File[]{log.file,cached[0]};
 }
 private void rebuiltWithoutChangingSource(File[] files,byte[] raw) throws Exception {
  DatasetStore store=new DatasetStore(history.getRoot());Catalog rebuilt=store.load();assertEquals(1,rebuilt.logsRebuilt);assertEquals(0,rebuilt.logsCached);
  assertEquals(1,rebuilt.dataset(Datasets.RUNS).rows.size());assertArrayEquals(raw,Files.readAllBytes(files[0].toPath()));
  Catalog warm=store.load();assertEquals(0,warm.logsRebuilt);assertEquals(1,warm.logsCached);assertArrayEquals(raw,Files.readAllBytes(files[0].toPath()));
 }
 @Test public void damagedAsciiCacheRebuildsFromUnchangedGenuineHistory() throws Exception {
  File[] f=createHistory();byte[] raw=Files.readAllBytes(f[0].toPath());Files.write(f[1].toPath(),flipText(DatasetStore.plainCache(Files.readAllBytes(f[1].toPath())),"real-source"));rebuiltWithoutChangingSource(f,raw);
 }
 @Test public void olderUnchecksummedCachesRebuildAndRemainWarmOnTheNextLoad() throws Exception {
  File[] f=createHistory();byte[] raw=Files.readAllBytes(f[0].toPath()),current=DatasetStore.plainCache(Files.readAllBytes(f[1].toPath()));
  byte[] old=current[4]==2?Arrays.copyOf(current,current.length-4):current.clone();old[4]=1;Files.write(f[1].toPath(),old);rebuiltWithoutChangingSource(f,raw);
 }
}
