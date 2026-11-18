package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;
public class RowCacheSourceIdentityTest {
 @Rule public TemporaryFolder tmp=new TemporaryFolder();
 private File history(File root,int score)throws Exception {
  SyntheticLog log=new SyntheticLog(root,"same-log",new TelemetryVersion(1,4),100);
  log.start("same-run",0,"WARRIOR","r1-aaaaaaaaaaaa",0,30,false).end(LifecycleEvents.OUTCOME_DIED,"Rat",1,1,score).close(true);
  return log.file;
 }
 private long score(Catalog c){Dataset d=c.dataset(Datasets.RUNS);return d.rows.get(0).num(d.def.indexOf(FactKeys.SCORE));}
 @Test public void sameNameLengthAndTimestampReplacementMustRebuildRows()throws Exception{
  File root=tmp.newFolder("one"),other=tmp.newFolder("two");
  File first=history(root,31),second=history(other,47);
  byte[] replacement=Files.readAllBytes(second.toPath());assertEquals(first.length(),replacement.length);
  DatasetStore store=new DatasetStore(root);assertEquals(31,score(store.load()));assertEquals(1,store.load().logsCached);
  long timestamp=first.lastModified();Files.write(first.toPath(),replacement);assertTrue(first.setLastModified(timestamp));
  Catalog after=store.load();assertEquals(47,score(after));assertEquals(1,after.logsRebuilt);assertEquals(0,after.logsCached);
  assertEquals(1,store.load().logsCached);assertArrayEquals(replacement,Files.readAllBytes(first.toPath()));
 }
 @Test public void unchangedSourceReusesItsCacheWithoutRewritingEitherFile()throws Exception{
  File root=tmp.newFolder("stable");File log=history(root,31);DatasetStore store=new DatasetStore(root);
  Catalog initial=store.load();assertEquals(1,initial.logsRebuilt);
  File[] caches=store.rowsDir().listFiles((d,n)->n.endsWith(".apdr"));assertNotNull(caches);assertEquals(1,caches.length);
  byte[] raw=Files.readAllBytes(log.toPath()),cache=Files.readAllBytes(caches[0].toPath());
  Catalog warm=new DatasetStore(root).load();assertEquals(1,warm.logsCached);assertEquals(0,warm.logsRebuilt);
  assertArrayEquals(raw,Files.readAllBytes(log.toPath()));assertArrayEquals(cache,Files.readAllBytes(caches[0].toPath()));
 }
 @Test public void oldUnboundCacheRebuildsOnceWithoutChangingRawHistory()throws Exception{
  File root=tmp.newFolder("legacy");File log=history(root,31);DatasetStore store=new DatasetStore(root);store.load();
  File[] caches=store.rowsDir().listFiles((d,n)->n.endsWith(".apdr"));assertNotNull(caches);assertEquals(1,caches.length);
  java.lang.reflect.Method definitions=DatasetStore.class.getDeclaredMethod("defs");definitions.setAccessible(true);
  @SuppressWarnings("unchecked") java.util.Map<String,DatasetDef> defs=(java.util.Map<String,DatasetDef>)definitions.invoke(null);
  byte[] raw=Files.readAllBytes(log.toPath());LogRows rows=RowBuilders.fromLog(RunLogReader.read(log),"same-log");
  byte[] old=RowCodec.encode(rows,log.length(),defs);assertEquals(2,old[4]);
  assertNotNull(RowCodec.decode(old,log.length(),defs));Files.write(caches[0].toPath(),old);
  Catalog rebuilt=store.load();assertEquals(1,rebuilt.logsRebuilt);assertEquals(31,score(rebuilt));
  assertEquals(1,store.load().logsCached);assertArrayEquals(raw,Files.readAllBytes(log.toPath()));
 }
 @Test public void sourceBoundCodecRejectsAnotherValidFingerprintAndUnboundBytes(){
  java.util.Map<String,DatasetDef> defs=new java.util.LinkedHashMap<>();DatasetDef def=QuerySupport.def();defs.put(def.id,def);
  LogRows rows=new LogRows();rows.rows.put(def.id,java.util.Arrays.asList(QuerySupport.row(def,"id","shown",10L,0L,0L,true,"r")));
  byte[] one=new byte[32],two=new byte[32];java.util.Arrays.fill(one,(byte)1);java.util.Arrays.fill(two,(byte)2);
  byte[] encoded=RowCodec.encode(rows,5,defs,one);assertEquals(3,encoded[4]);
  assertNotNull(RowCodec.decode(encoded,5,defs,one));assertNull(RowCodec.decode(encoded,5,defs,two));
  assertNull(RowCodec.decode(RowCodec.encode(rows,5,defs),5,defs,one));
  assertNull(RowCodec.decode(encoded,5,defs,new byte[31]));
 }
 @Test public void rowCachesAreCompressedAndOlderPlainCachesAreUpgradedInPlace()throws Exception{
  File root=tmp.newFolder("compressed");File log=history(root,31);DatasetStore store=new DatasetStore(root);store.load();
  File[] caches=store.rowsDir().listFiles((d,n)->n.endsWith(".apdr"));assertNotNull(caches);assertEquals(1,caches.length);
  byte[] stored=Files.readAllBytes(caches[0].toPath());assertTrue(DatasetStore.isGzip(stored));
  byte[] plain=DatasetStore.gunzip(stored);assertTrue(stored.length<plain.length);
  Files.write(caches[0].toPath(),plain);
  Catalog warm=new DatasetStore(root).load();assertEquals(1,warm.logsCached);assertEquals(0,warm.logsRebuilt);assertEquals(31,score(warm));
  assertTrue(DatasetStore.isGzip(Files.readAllBytes(caches[0].toPath())));
 }
}
