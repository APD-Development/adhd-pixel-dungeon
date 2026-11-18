package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadOptions;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

// The import checks every frame while retaining only the typed start identity.
// Normal statistics readers must still receive every gameplay event.
public class ArchiveImporterRunStartOnlyTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test public void thousandsOfGameplayEventsDoNotNeedToRemainInImportIdentityRead() throws Exception {
        File source = tmp.newFolder("source");
        File root = tmp.newFolder("history");
        TestLogs log = new TestLogs(source, "part-one", ArchiveSupport.V, 8000)
                .start("logical-long-run", 0, "WARRIOR", ArchiveSupport.RS, 0, 0, false)
                .enter(1);
        for (int i = 0; i < 1000; i++) {
            log.heroAttacks("rat", "actors.mobs.Rat", true);
        }
        log.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 5).close(true);
        byte[] original = Files.readAllBytes(log.file.toPath());

        ReadResult ordinary = RunLogReader.read(log.file);
        ReadResult identity = RunLogReader.read(log.file, new ReadOptions().onlyRunStartEvents(true));
        assertTrue("ordinary reader retained gameplay events", ordinary.events.size() > 1000);
        assertEquals(ordinary.status, identity.status);
        assertEquals(ordinary.eventCount, identity.eventCount);
        assertEquals(ordinary.sealEventCount, identity.sealEventCount);
        assertEquals(ordinary.validBytes, identity.validBytes);
        assertEquals(ordinary.gapEvents, identity.gapEvents);
        assertEquals(ordinary.strings, identity.strings);
        assertEquals(1, identity.events.size());
        ApdEvent start = identity.events.get(0);
        assertEquals(EventTypes.RUN_START, start.type);
        assertEquals("logical-long-run", LifecycleEvents.decodeRunStart(start, identity).runId);

        Map<String, byte[]> files = new TreeMap<>();
        files.put("runs/part-one.apdlog", original);
        Map<String, String> runIds = new TreeMap<>();
        runIds.put("runs/part-one.apdlog", "logical-long-run");
        ArchiveImporter.Report result = ArchiveImporter.importArchive(root,
                new ByteArrayInputStream(ArchiveSupport.archiveOf(files, runIds)));
        assertTrue(result.summary(), result.ok);
        assertEquals(1, result.imported);
        assertEquals(0, result.rejected);
        assertArrayEquals(original, Files.readAllBytes(new File(root, "runs/part-one.apdlog").toPath()));
        assertFalse(new File(root, "import-tmp").exists());
    }

    @Test public void noRunStartLegacyLogAndDefaultReaderStillKeepExistingBehavior() throws Exception {
        File source = tmp.newFolder("legacy-source");
        File root = tmp.newFolder("legacy-history");
        TestLogs log = new TestLogs(source, "legacy-identity", ArchiveSupport.V, 8001);
        log.enter(1).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 5).close(true);
        ReadResult ordinary = RunLogReader.read(log.file);
        ReadResult optedOut = RunLogReader.read(log.file, new ReadOptions().onlyRunStartEvents(false));
        ReadResult identity = RunLogReader.read(log.file, new ReadOptions().onlyRunStartEvents(true));
        assertEquals(ordinary.events.size(), optedOut.events.size());
        assertTrue(ordinary.events.size() > 0);
        assertEquals(0, identity.events.size());
        assertEquals(ordinary.eventCount, identity.eventCount);
        assertEquals(ordinary.status, identity.status);

        byte[] original = Files.readAllBytes(log.file.toPath());
        Map<String, byte[]> files = Collections.singletonMap("legacy/legacy-identity.apdlog", original);
        ArchiveImporter.Report result = ArchiveImporter.importArchive(root,
                new ByteArrayInputStream(ArchiveSupport.archiveOf(files, Collections.<String,String>emptyMap())));
        assertTrue(result.summary(), result.ok);
        assertEquals(1, result.imported);
        assertArrayEquals(original, Files.readAllBytes(new File(root,
                "legacy/legacy-identity.apdlog").toPath()));
        assertFalse(new File(root, "import-tmp").exists());
    }
}
