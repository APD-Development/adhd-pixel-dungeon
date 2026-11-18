package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class RowCacheTextValidationTest {
 @Rule public TemporaryFolder history=new TemporaryFolder();
 private Map<String,DatasetDef> definitions(){Map<String,DatasetDef> defs=new LinkedHashMap<>();defs.put("things",QuerySupport.def());return defs;}
 private byte[] cache(String name){DatasetDef d=QuerySupport.def();LogRows lr=new LogRows();
  lr.rows.put(d.id,Arrays.asList(QuerySupport.row(d,"row-marker",name,10L,0L,0L,true,"r")));
  return RowCodec.encode(lr,5,definitions());
 }
 private byte[] damage(byte[] data,String marker){byte[] target=marker.getBytes(StandardCharsets.UTF_8);
  outer:for(int i=0;i<=data.length-target.length;i++){for(int j=0;j<target.length;j++)if(data[i+j]!=target[j])continue outer;
   byte[] copy=data.clone();copy[i]=(byte)0xff;return copy;}throw new AssertionError("fixture marker not found");
 }
 @Test public void malformedStringTableMustRebuildInsteadOfInventingReplacementText(){
  assertNull(RowCodec.decode(damage(cache("visible-name"),"visible-name"),5,definitions()));
 }
 @Test public void validInternationalTextSurvivesUnchanged(){
  String name="épée 日本語 🗡";LogRows lr=RowCodec.decode(cache(name),5,definitions());
  assertNotNull(lr);assertEquals(name,lr.rows.get("things").get(0).text(QuerySupport.NAME));
 }
 @Test public void aRemovedDatasetDoesNotMisalignTheFollowingKnownDataset(){
  DatasetDef gone=new DatasetDef("gone","Gone","");gone.add(ColumnDef.num("old","Old").shown());
  DatasetDef kept=QuerySupport.def();Map<String,DatasetDef> original=new LinkedHashMap<>();original.put("gone",gone);original.put(kept.id,kept);
  LogRows lr=new LogRows();
  lr.rows.put("gone",Collections.<Row>emptyList());lr.rows.put("things",Arrays.asList(QuerySupport.row(kept,"new","kept",11L,0L,0L,true,"r")));
  LogRows decoded=RowCodec.decode(RowCodec.encode(lr,5,original),5,definitions());
  assertNotNull(decoded);assertFalse(decoded.rows.containsKey("gone"));
  assertEquals("kept",decoded.rows.get("things").get(0).text(QuerySupport.NAME));
 }
 @Test public void malformedCacheTextRebuildsAndLeavesRawHistoryByteForByteUnchanged() throws Exception {
  SyntheticLog log=new SyntheticLog(history.getRoot(),"real-source",new TelemetryVersion(1,4),100);
  log.start("run-source",0,"WARRIOR","r1-aaaaaaaaaaaa",0,30,false).end(LifecycleEvents.OUTCOME_DIED,"Rat",1,1,0).close(true);
  byte[] raw=Files.readAllBytes(log.file.toPath());DatasetStore store=new DatasetStore(history.getRoot());store.load();
  File[] caches=store.rowsDir().listFiles((dir,name)->name.endsWith(".apdr"));assertNotNull(caches);assertEquals(1,caches.length);
  Files.write(caches[0].toPath(),damage(com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore.plainCache(Files.readAllBytes(caches[0].toPath())),"real-source"));
  Catalog rebuilt=store.load();assertEquals(1,rebuilt.logsRebuilt);assertEquals(0,rebuilt.logsCached);
  assertEquals(1,rebuilt.dataset(Datasets.RUNS).rows.size());assertArrayEquals(raw,Files.readAllBytes(log.file.toPath()));
 }
}
