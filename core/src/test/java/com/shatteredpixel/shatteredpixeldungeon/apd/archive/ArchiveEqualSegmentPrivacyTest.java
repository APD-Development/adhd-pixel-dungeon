package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveSupport.*;
import static org.junit.Assert.*;

/**
 * Equal timestamps and run-start segments do not prove which of two distinct
 * logs is the final segment. Filename iteration must never decide privacy.
 */
public class ArchiveEqualSegmentPrivacyTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static final String RUN = "shared-logical-run";

    private TestLogs make(File root, String fileId, int segment, boolean ended) {
        TestLogs log = new TestLogs(root, fileId, V, 1000)
                .seeds(654321, SECRET_SEED)
                .start(RUN, segment, "MAGE", RS, 0, 0, false)
                .enter(1);
        log.registered("hidden-" + fileId, SECRET_CLASS, 3, true, 0);
        if (ended) {
            log.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 10).close(true);
        } else {
            log.close(false);
        }
        return log;
    }

    private static Map<String, byte[]> entries(byte[] source) throws Exception {
        Map<String, byte[]> result = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(source))) {
            ZipEntry e;
            byte[] buffer = new byte[4096];
            while ((e = zip.getNextEntry()) != null) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int read;
                while ((read = zip.read(buffer)) != -1) bytes.write(buffer, 0, read);
                result.put(e.getName(), bytes.toByteArray());
            }
        }
        return result;
    }

    private void assertMixedTieRedacted(String firstId, boolean firstEnded, String secondId, boolean secondEnded) throws Exception {
        File root = tmp.newFolder("mixed-" + firstId);
        TestLogs a = make(root, firstId, 0, firstEnded);
        TestLogs b = make(root, secondId, 0, secondEnded);
        byte[] aOriginal = Files.readAllBytes(a.file.toPath());
        byte[] bOriginal = Files.readAllBytes(b.file.toPath());
        assertTrue(contains(aOriginal, SECRET_CLASS));
        assertTrue(contains(bOriginal, SECRET_CLASS));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ArchiveExporter.Report exported = ArchiveExporter.export(root, output, "test", 123);
        assertTrue(exported.summary(), exported.ok);
        assertEquals(2, exported.runLogs);
        assertEquals(2, exported.redactedRunLogs);
        Map<String, byte[]> zipped = entries(output.toByteArray());
        ArchiveManifest manifest = ArchiveManifest.parse(zipped.get(ArchiveManifest.ENTRY_NAME));
        for (String fileId : new String[]{firstId, secondId}) {
            String path = "runs/" + fileId + ".apdlog";
            ArchiveManifest.Entry e = manifest.find(path);
            assertNotNull(e);
            assertEquals(RUN, e.runId);
            assertTrue("both files must be redacted on an ambiguous completion tie", e.redacted);
            byte[] bytes = zipped.get(path);
            assertNotNull(bytes);
            assertTrue(LogRedactor.isRedacted(RunLogReader.parse(bytes).header.writerBuild));
            assertFalse(contains(bytes, SECRET_CLASS));
            assertFalse(contains(bytes, SECRET_SEED));
        }
        assertArrayEquals(aOriginal, Files.readAllBytes(a.file.toPath()));
        assertArrayEquals(bOriginal, Files.readAllBytes(b.file.toPath()));

        File destination = tmp.newFolder("restored-" + firstId);
        ArchiveImporter.Report imported = ArchiveImporter.importArchive(destination,
                new ByteArrayInputStream(output.toByteArray()));
        assertTrue(imported.summary(), imported.ok);
        assertEquals(2, imported.imported);
    }

    @Test public void finishedAlphabeticallyFirstCannotExposeAmbiguousSameSegmentRun() throws Exception {
        // Current filename-tie behavior chose the completed file and leaked both.
        assertMixedTieRedacted("aa-complete", true, "zz-unfinished", false);
    }

    @Test public void unfinishedAlphabeticallyFirstAlsoRedactsTheWholeTie() throws Exception {
        assertMixedTieRedacted("aa-unfinished", false, "zz-complete", true);
    }

    @Test public void twoCompletedEqualSegmentLogsStillExportLosslessly() throws Exception {
        File root = tmp.newFolder("both-complete");
        TestLogs a = make(root, "aa-complete", 0, true);
        TestLogs b = make(root, "zz-complete", 0, true);
        byte[] originalA = Files.readAllBytes(a.file.toPath());
        byte[] originalB = Files.readAllBytes(b.file.toPath());
        Map<String, byte[]> zipped = entries(ArchiveSupport.export(root));
        ArchiveManifest manifest = ArchiveManifest.parse(zipped.get(ArchiveManifest.ENTRY_NAME));
        assertFalse(manifest.find("runs/aa-complete.apdlog").redacted);
        assertFalse(manifest.find("runs/zz-complete.apdlog").redacted);
        assertArrayEquals(originalA, zipped.get("runs/aa-complete.apdlog"));
        assertArrayEquals(originalB, zipped.get("runs/zz-complete.apdlog"));
    }

    @Test public void higherSegmentStillWinsWhenTimestampIsEqual() throws Exception {
        File root = tmp.newFolder("distinct-segments");
        TestLogs old = make(root, "aa-open-first", 0, false);
        TestLogs last = make(root, "zz-finished-second", 1, true);
        byte[] originalOld = Files.readAllBytes(old.file.toPath());
        byte[] originalLast = Files.readAllBytes(last.file.toPath());
        Map<String, byte[]> zipped = entries(ArchiveSupport.export(root));
        ArchiveManifest manifest = ArchiveManifest.parse(zipped.get(ArchiveManifest.ENTRY_NAME));
        assertFalse(manifest.find("runs/aa-open-first.apdlog").redacted);
        assertFalse(manifest.find("runs/zz-finished-second.apdlog").redacted);
        assertArrayEquals(originalOld, zipped.get("runs/aa-open-first.apdlog"));
        assertArrayEquals(originalLast, zipped.get("runs/zz-finished-second.apdlog"));
    }
}
