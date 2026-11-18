package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;
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

/** A valid terminal RUN_END does not establish completion without a valid typed RUN_START. */
public class ArchiveRunStartPrivacyTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static Map<String, byte[]> contents(byte[] source) throws Exception {
        Map<String, byte[]> result = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(source))) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = zip.getNextEntry()) != null) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int read;
                while ((read = zip.read(buffer)) != -1) bytes.write(buffer, 0, read);
                result.put(entry.getName(), bytes.toByteArray());
            }
        }
        return result;
    }

    private void assertIncompleteStartRemainsPrivate(String fileId, boolean invalidStart) throws Exception {
        File root = tmp.newFolder(fileId);
        TestLogs log = new TestLogs(root, fileId, V, 1000).seeds(424242L, SECRET_SEED);
        if (invalidStart) {
            assertFalse(StatsStore.isValidRunId("invalid/run"));
            log.start("invalid/run", 0, "MAGE", RS, 0, 0, false);
        }
        log.enter(1).registered("hidden-ring", SECRET_CLASS, 3, true, 0)
                .end(LifecycleEvents.OUTCOME_WON, "", 26, 20, 100).close(true);
        byte[] original = Files.readAllBytes(log.file.toPath());
        assertTrue(contains(original, SECRET_CLASS));
        if (invalidStart) assertTrue(contains(original, SECRET_SEED));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ArchiveExporter.Report report = ArchiveExporter.export(root, output, "test", 123);
        assertTrue(report.summary(), report.ok);
        assertEquals(1, report.runLogs);
        assertEquals(1, report.redactedRunLogs);
        Map<String, byte[]> archive = contents(output.toByteArray());
        String entryPath = "runs/" + fileId + ".apdlog";
        ArchiveManifest.Entry manifestEntry = ArchiveManifest.parse(archive.get("MANIFEST.txt")).find(entryPath);
        assertNotNull(manifestEntry);
        assertTrue("unproven start must not certify completion", manifestEntry.redacted);
        byte[] copy = archive.get(entryPath);
        assertNotNull(copy);
        assertTrue(LogRedactor.isRedacted(RunLogReader.parse(copy).header.writerBuild));
        assertFalse(contains(copy, SECRET_CLASS));
        assertFalse(contains(copy, SECRET_SEED));
        assertArrayEquals("archive must not alter stored history", original, Files.readAllBytes(log.file.toPath()));

        File restored = tmp.newFolder("restored-" + fileId);
        ArchiveImporter.Report imported = ArchiveImporter.importArchive(restored,
                new ByteArrayInputStream(output.toByteArray()));
        assertTrue(imported.summary(), imported.ok);
        assertEquals(1, imported.imported);
    }

    @Test public void sealedTerminalEndWithoutAnyRunStartCannotExportSecrets() throws Exception {
        assertIncompleteStartRemainsPrivate("missing-start", false);
    }

    @Test public void malformedTypedRunStartCannotCertifyFinishedRun() throws Exception {
        assertIncompleteStartRemainsPrivate("invalid-start", true);
    }

    @Test public void validTypedRunStartAndNormalRankingTrailerStayLossless() throws Exception {
        File root = tmp.newFolder("legitimate-finished");
        TestLogs log = new TestLogs(root, "finished", V, 1000)
                .seeds(424242L, SECRET_SEED)
                .start("shared-logical-run", 2, "MAGE", RS, 0, 0, false)
                .enter(1).registered("hidden-ring", SECRET_CLASS, 3, true, 0)
                .end(LifecycleEvents.OUTCOME_WON, "", 26, 20, 100)
                .rankingLink("ranking-id");
        log.close(true);
        byte[] original = Files.readAllBytes(log.file.toPath());
        Map<String, byte[]> archive = contents(ArchiveSupport.export(root));
        String entryPath = "runs/finished.apdlog";
        ArchiveManifest.Entry entry = ArchiveManifest.parse(archive.get("MANIFEST.txt")).find(entryPath);
        assertNotNull(entry);
        assertEquals("shared-logical-run", entry.runId);
        assertFalse("normal finished segments must remain lossless", entry.redacted);
        assertArrayEquals(original, archive.get(entryPath));
        assertArrayEquals(original, Files.readAllBytes(log.file.toPath()));
    }
}
