package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class FutureLegacyDerivationTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    private File write(File folder,String id,TelemetryVersion version,long value) throws Exception {
        File file=new File(folder,id+".apdlog");
        RunLogWriter w=RunLogWriter.create(file,version,id,"fixture",value);
        try {
            LegacyEvents.Record record=new LegacyEvents.Record();record.gameId=id;
            record.putNum(FactKeys.SCORE,value);assertTrue(LegacyEvents.emitRecord(w,0,record));
            LegacyEvents.Totals totals=new LegacyEvents.Totals();totals.importedAtMillis=value;
            totals.fields.put(FactKeys.SCORE,LegacyEvents.Value.num(value));assertTrue(LegacyEvents.emitTotals(w,1,totals));
            LegacyEvents.DailyScore daily=new LegacyEvents.DailyScore();daily.day=value;daily.score=value;
            assertTrue(LegacyEvents.emitDailyScore(w,2,daily));
        } finally { w.close(); }
        return file;
    }
    private void verify(TelemetryVersion future) throws Exception {
        File root=tmp.newFolder(),legacy=new File(root,"legacy");assertTrue(legacy.mkdir());
        File supported=write(legacy,"a-supported",TelemetryVersion.CURRENT,42);
        File unsupported=write(legacy,"z-future",future,999);
        byte[] oldSupported=Files.readAllBytes(supported.toPath()),oldFuture=Files.readAllBytes(unsupported.toPath());
        assertTrue(RunLogReader.read(unsupported).newerThanReader());
        DerivedStore store=new DerivedStore(root);
        for(int pass=0;pass<3;pass++) {
            FactTable facts=pass==0?store.buildLegacyOnly():store.load();
            assertEquals("Unsupported telemetry must not be interpreted as current statistics",1,facts.legacyRows.size());
            assertEquals("legacy:a-supported",facts.legacyRows.get(0).id);
            assertEquals(42,facts.legacyRows.get(0).numOr(FactKeys.SCORE,-1));
            assertEquals(42,facts.legacyTotals.numOr(FactKeys.SCORE,-1));
            assertEquals(1,facts.dailyScores.size());assertArrayEquals(new long[]{42,42},facts.dailyScores.get(0));
            if(pass==2)assertEquals(DerivedStore.Source.CACHE,store.lastSource());
        }
        assertArrayEquals(oldSupported,Files.readAllBytes(supported.toPath()));
        assertArrayEquals(oldFuture,Files.readAllBytes(unsupported.toPath()));
        assertTrue(unsupported.isFile());
    }
    @Test public void futureMajorLegacyRecordsTotalsAndDailiesAreNotMisread() throws Exception {
        verify(new TelemetryVersion(TelemetryVersion.CURRENT.major+1,0));
    }
    @Test public void futureMinorLegacyDataMatchesTheExistingDatasetRefusalPolicy() throws Exception {
        verify(new TelemetryVersion(TelemetryVersion.CURRENT.major,TelemetryVersion.CURRENT.minor+1));
    }
    @Test public void affectedDerivationFiveCacheIsRebuiltWithoutTouchingRawLogs() throws Exception {
        File root=tmp.newFolder(),legacy=new File(root,"legacy");assertTrue(legacy.mkdir());
        File supported=write(legacy,"a-supported",TelemetryVersion.CURRENT,42);
        File future=write(legacy,"z-future",new TelemetryVersion(TelemetryVersion.CURRENT.major+1,0),999);
        byte[] original=Files.readAllBytes(future.toPath());
        DerivedStore store=new DerivedStore(root);
        FactTable affected=new FactTable();
        for(String id:new String[]{"a-supported","z-future"}) {
            LegacyEvents.Record record=new LegacyEvents.Record();record.gameId=id;
            record.putNum(FactKeys.SCORE,id.startsWith("a")?42:999);
            affected.legacyRows.add(LegacyFacts.fromRecord(record,id));
        }
        java.lang.reflect.Method prints=DerivedStore.class.getDeclaredMethod("fingerprints");prints.setAccessible(true);
        java.lang.reflect.Method writer=DerivedStore.class.getDeclaredMethod("writeCache",FactTable.class,java.util.List.class);writer.setAccessible(true);
        writer.invoke(store,affected,prints.invoke(store));
        String encoded=new String(Files.readAllBytes(store.cacheFile().toPath()),java.nio.charset.StandardCharsets.UTF_8);
        String body=encoded.substring(0,encoded.lastIndexOf("CRC32\t"));
        body=body.replaceFirst("APDD\\t2\\t[0-9]+","APDD\t2\t5"); // Exact version that cached the incorrect interpretation.
        byte[] bytes=body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(bytes);
        Files.write(store.cacheFile().toPath(),(body+String.format(java.util.Locale.ROOT,"CRC32\t%08x\n",crc.getValue())).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        FactTable rebuilt=store.load();assertEquals(DerivedStore.Source.REBUILT_STALE,store.lastSource());
        assertEquals(1,rebuilt.legacyRows.size());assertEquals(42,rebuilt.legacyTotals.numOr(FactKeys.SCORE,-1));
        assertEquals(1,store.load().legacyRows.size());assertEquals(DerivedStore.Source.CACHE,store.lastSource());
        assertArrayEquals(original,Files.readAllBytes(future.toPath()));assertTrue(supported.isFile());
    }
}
