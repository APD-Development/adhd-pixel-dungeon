package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import java.io.*;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class RowCacheNamespaceTest {
 @Rule public TemporaryFolder history=new TemporaryFolder();
 @Test public void equalLengthLogsInDifferentSourceFoldersKeepBothRecords()throws Exception {
  File root=history.getRoot();
  SyntheticLog fixture=new SyntheticLog(root,"collision",new TelemetryVersion(1,4),100).start("real-run",0,"WARRIOR","r1-aaaaaaaaaaaa",0,0,false).enter(1).end(LifecycleEvents.OUTCOME_DIED,"Rat",3,5,1234);
  fixture.close(true);File run=fixture.file;
  File legacy=new File(new File(root,"legacy"),"collision.apdlog");boolean same=false;
  for(int padding=0;padding<4096;padding++){
   if(legacy.exists())assertTrue(legacy.delete());
   RunLogWriter writer=RunLogWriter.create(legacy,TelemetryVersion.CURRENT,"collision","test",200);
   LegacyEvents.Record record=new LegacyEvents.Record();record.gameId="older-ranking";record.source="rankings"+"x".repeat(padding);
   record.putText(FactKeys.HERO_CLASS,"MAGE");record.putNum(FactKeys.OUTCOME,LifecycleEvents.OUTCOME_DIED);record.putNum(FactKeys.SCORE,9999);
   LegacyEvents.emitRecord(writer,1,record);writer.close(true);
   if(legacy.length()==run.length()){same=true;break;}
   if(legacy.length()>run.length())break;
  }
  assertTrue("fixture creates two different valid logs with the same source-relative ID and byte length",same);
  byte[] recordedBytes=java.nio.file.Files.readAllBytes(run.toPath());
  byte[] importedBytes=java.nio.file.Files.readAllBytes(legacy.toPath());
  ByteArrayOutputStream archive=new ByteArrayOutputStream();
  com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveExporter.Report exported=
   com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveExporter.export(root,archive,"test",300);
  assertTrue(exported.summary(),exported.ok);
  File destination=history.newFolder("restored");
  com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveImporter.Report restored=
   com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveImporter.importArchive(destination,new ByteArrayInputStream(archive.toByteArray()));
  assertTrue(restored.summary(),restored.ok);assertEquals(2,restored.imported);
  assertArrayEquals(recordedBytes,java.nio.file.Files.readAllBytes(new File(destination,"runs/collision.apdlog").toPath()));
  assertArrayEquals(importedBytes,java.nio.file.Files.readAllBytes(new File(destination,"legacy/collision.apdlog").toPath()));
  DatasetStore store=new DatasetStore(destination);
  for(int pass=0;pass<2;pass++){
   Catalog cat=store.load();Dataset runs=cat.dataset(Datasets.RUNS);
   assertEquals("recorded run and distinct imported ranking must both remain",2,runs.rows.size());
   boolean recorded=false,imported=false;
   for(Row row:runs.rows){recorded|=row.runId.equals("real-run")&&!row.imported;imported|=row.id.equals("legacy:older-ranking")&&row.imported;}
   assertTrue(recorded);assertTrue("imported ranking must not be replaced by the recorded run's cache",imported);
   if(pass==0){
    File obsolete=new File(store.rowsDir(),"collision.apdr");
    java.nio.file.Files.copy(new File(store.rowsDir(),"runs-collision.apdr").toPath(),obsolete.toPath());
   }
   if(pass==1)assertEquals("both source caches can be reused while old ambiguous caches are ignored",2,cat.logsCached);
   assertArrayEquals(recordedBytes,java.nio.file.Files.readAllBytes(new File(destination,"runs/collision.apdlog").toPath()));
   assertArrayEquals(importedBytes,java.nio.file.Files.readAllBytes(new File(destination,"legacy/collision.apdlog").toPath()));
  }
 }
}
