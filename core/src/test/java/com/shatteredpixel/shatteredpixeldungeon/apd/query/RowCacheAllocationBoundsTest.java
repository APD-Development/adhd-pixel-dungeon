package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ByteSink;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKinds;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import static org.junit.Assert.*;

public class RowCacheAllocationBoundsTest {
    @Rule public TemporaryFolder history = new TemporaryFolder();
    private static ByteSink header() {
        return header(0);
    }
    private static ByteSink header(long length) {
        return new ByteSink(128).writeBytes(new byte[]{'A','P','D','R'})
            .writeUnsigned(2).writeUnsigned(RowBuilders.BUILDER_VERSION)
            .writeString(FactKinds.fingerprint()).writeUnsigned(length);
    }
    private static byte[] complete(ByteSink out){return out.writeInt32(out.crc32()).toByteArray();}
    private static ByteSink section() {
        ByteSink out = header().writeUnsigned(1).writeString("");
        out.writeUnsigned(0).writeUnsigned(0).writeUnsigned(0).writeUnsigned(0).writeUnsigned(0);
        out.writeByte(0).writeByte(0).writeUnsigned(0).writeUnsigned(0);
        return out.writeUnsigned(1).writeString("future-dataset");
    }
    @Test public void tinyCacheCannotAllocateAHugeStringTable() {
        assertNull(RowCodec.decode(complete(header().writeUnsigned(Integer.MAX_VALUE)), 0, Collections.emptyMap()));
    }
    @Test public void tinyUnknownSectionCannotAllocateAHugeColumnMap() {
        assertNull(RowCodec.decode(complete(section().writeUnsigned(Integer.MAX_VALUE)), 0, Collections.emptyMap()));
    }
    @Test public void tinyUnknownSectionCannotAllocateAHugeRowList() {
        assertNull(RowCodec.decode(complete(section().writeUnsigned(0).writeUnsigned(Integer.MAX_VALUE)), 0, Collections.emptyMap()));
    }
    @Test public void legitimateEmptyCacheRemainsReadable() {
        assertNotNull(RowCodec.decode(RowCodec.encode(new LogRows(), 0, Collections.emptyMap()), 0, Collections.emptyMap()));
    }
    @Test public void unknownEmptyFutureSectionRemainsReadable() {
        assertNotNull(RowCodec.decode(complete(section().writeUnsigned(0).writeUnsigned(0)), 0, Collections.emptyMap()));
    }
    @Test public void damagedCacheRebuildsWithoutChangingRawHistory() throws Exception {
        SyntheticLog log = new SyntheticLog(history.getRoot(), "real-source", new TelemetryVersion(1,4), 100);
        log.start("run-source", 0, "WARRIOR", "r1-aaaaaaaaaaaa", 0, 30, false)
            .end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 0).close(true);
        byte[] original = Files.readAllBytes(log.file.toPath());
        DatasetStore store = new DatasetStore(history.getRoot());
        assertEquals(1, store.load().dataset(Datasets.RUNS).rows.size());
        File[] caches = store.rowsDir().listFiles((dir,name) -> name.endsWith(".apdr"));
        assertNotNull(caches); assertEquals(1, caches.length);
        Files.write(caches[0].toPath(), complete(header(original.length).writeUnsigned(Integer.MAX_VALUE)));
        Catalog rebuilt = store.load();
        assertEquals(1, rebuilt.logsRebuilt);
        assertEquals(1, rebuilt.dataset(Datasets.RUNS).rows.size());
        assertArrayEquals(original, Files.readAllBytes(log.file.toPath()));
    }
}
