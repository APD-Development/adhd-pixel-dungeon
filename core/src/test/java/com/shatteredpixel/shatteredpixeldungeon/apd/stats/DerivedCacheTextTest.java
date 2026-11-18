package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class DerivedCacheTextTest {
 @Rule public TemporaryFolder tmp = new TemporaryFolder();
 private File source(String seed) throws Exception {
  File log = new File(new File(tmp.getRoot(), "legacy"), "import.apdlog");
  RunLogWriter writer = RunLogWriter.create(log, TelemetryVersion.CURRENT, "import", "test", 1);
  LegacyEvents.Record record = new LegacyEvents.Record();
  record.gameId="g1"; record.source="rankings.dat";
  record.putText(FactKeys.CUSTOM_SEED, seed);
  LegacyEvents.emitRecord(writer,0,record); writer.close(true); return log;
 }
 @Test public void damagedUtf8CacheRebuildsWithoutChangingRawHistory() throws Exception {
  File log=source("UNIQUESEED"); byte[] raw=Files.readAllBytes(log.toPath());
  DerivedStore store=new DerivedStore(tmp.getRoot()); store.load();
  byte[] cache=Files.readAllBytes(store.cacheFile().toPath());
  String text=new String(cache,"UTF-8"); int offset=text.indexOf("UNIQUESEED"); assertTrue(offset>=0);
  cache[offset]=(byte)0xff; Files.write(store.cacheFile().toPath(),cache);
  FactTable table=store.load();
  assertEquals(DerivedStore.Source.REBUILT_BAD_CACHE,store.lastSource());
  assertEquals("UNIQUESEED",table.legacyRows.get(0).textOr(FactKeys.CUSTOM_SEED,""));
  assertArrayEquals(raw,Files.readAllBytes(log.toPath()));
 }
 @Test public void internationalTextRemainsCacheable() throws Exception {
  String seed="é_日本_😀"; source(seed); DerivedStore store=new DerivedStore(tmp.getRoot());
  store.load(); FactTable table=store.load(); assertEquals(DerivedStore.Source.CACHE,store.lastSource());
  assertEquals(seed,table.legacyRows.get(0).textOr(FactKeys.CUSTOM_SEED,""));
 }
}
