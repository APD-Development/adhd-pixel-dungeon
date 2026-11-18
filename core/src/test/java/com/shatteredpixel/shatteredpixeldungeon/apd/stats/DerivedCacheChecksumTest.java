package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class DerivedCacheChecksumTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private File source(int count) throws Exception {
        File file=new File(new File(temp.getRoot(),"legacy"),"import.apdlog");
        RunLogWriter writer=RunLogWriter.create(file,TelemetryVersion.CURRENT,"import","test",1);
        try {
            for(int i=0;i<count;i++){
                LegacyEvents.Record record=new LegacyEvents.Record();record.gameId="g"+i;record.source="rankings.dat";
                record.putText(FactKeys.CUSTOM_SEED,"UNIQUESEED_é_日本_😀_"+i);record.putNum(FactKeys.SCORE,100);
                LegacyEvents.emitRecord(writer,i,record);
            }
        }finally{writer.close(true);}
        return file;
    }
    @Test public void asciiBitFlipRebuildsWithoutChangingRawHistory() throws Exception {
        File raw=source(1);byte[] original=Files.readAllBytes(raw.toPath());
        DerivedStore store=new DerivedStore(temp.getRoot());FactTable before=store.load();
        byte[] cache=Files.readAllBytes(store.cacheFile().toPath());
        int offset=new String(cache,StandardCharsets.UTF_8).indexOf("UNIQUESEED");assertTrue(offset>=0);
        cache[offset]^=1;Files.write(store.cacheFile().toPath(),cache);
        FactTable after=store.load();assertEquals(DerivedStore.Source.REBUILT_BAD_CACHE,store.lastSource());
        assertEquals(before.legacyRows.get(0).textOr(FactKeys.CUSTOM_SEED,""),after.legacyRows.get(0).textOr(FactKeys.CUSTOM_SEED,""));
        assertArrayEquals(original,Files.readAllBytes(raw.toPath()));
        store.load();assertEquals(DerivedStore.Source.CACHE,store.lastSource());
    }
    @Test public void validNumericCorruptionDoesNotChangeStatistics() throws Exception {
        File raw=source(1);byte[] original=Files.readAllBytes(raw.toPath());DerivedStore store=new DerivedStore(temp.getRoot());store.load();
        String cache=new String(Files.readAllBytes(store.cacheFile().toPath()),StandardCharsets.UTF_8);
        assertTrue(cache.contains("\t100\n"));
        Files.write(store.cacheFile().toPath(),cache.replace("\t100\n","\t101\n").getBytes(StandardCharsets.UTF_8));
        FactTable table=store.load();assertEquals(DerivedStore.Source.REBUILT_BAD_CACHE,store.lastSource());
        assertEquals(100,table.legacyRows.get(0).numOr(FactKeys.SCORE,-1));assertArrayEquals(original,Files.readAllBytes(raw.toPath()));
    }
    @Test public void bytesAppendedAfterEndAreRejected() throws Exception {
        File raw=source(1);byte[] original=Files.readAllBytes(raw.toPath());DerivedStore store=new DerivedStore(temp.getRoot());store.load();
        Files.write(store.cacheFile().toPath(),"EXTRA\n".getBytes(StandardCharsets.UTF_8),java.nio.file.StandardOpenOption.APPEND);
        assertEquals(1,store.load().legacyRows.size());assertEquals(DerivedStore.Source.REBUILT_BAD_CACHE,store.lastSource());
        assertArrayEquals(original,Files.readAllBytes(raw.toPath()));
    }
    @Test public void oldUnchecksummedCacheMigratesWithoutChangingRawHistory() throws Exception {
        File raw=source(1);byte[] original=Files.readAllBytes(raw.toPath());DerivedStore store=new DerivedStore(temp.getRoot());store.load();
        String text=new String(Files.readAllBytes(store.cacheFile().toPath()),StandardCharsets.UTF_8);
        int footer=text.lastIndexOf("CRC32\t");assertTrue(footer>0);
        String old=text.substring(0,footer).replace("APDD\t2\t","APDD\t1\t");
        Files.write(store.cacheFile().toPath(),old.getBytes(StandardCharsets.UTF_8));
        assertEquals(1,store.load().legacyRows.size());assertEquals(DerivedStore.Source.REBUILT_STALE,store.lastSource());
        assertArrayEquals(original,Files.readAllBytes(raw.toPath()));
        store.load();assertEquals(DerivedStore.Source.CACHE,store.lastSource());
    }
    @Test public void unicodeCacheBeyondReaderBufferKeepsEveryRecord() throws Exception {
        source(200);DerivedStore store=new DerivedStore(temp.getRoot());FactTable before=store.load();
        assertTrue(store.cacheFile().length()>8192);FactTable after=store.load();assertEquals(DerivedStore.Source.CACHE,store.lastSource());
        assertEquals(200,after.legacyRows.size());
        for(int i=0;i<200;i++)assertEquals(before.legacyRows.get(i).textOr(FactKeys.CUSTOM_SEED,""),after.legacyRows.get(i).textOr(FactKeys.CUSTOM_SEED,""));
    }
}
