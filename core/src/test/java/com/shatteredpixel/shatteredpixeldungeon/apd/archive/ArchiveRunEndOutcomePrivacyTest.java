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
 * A structurally decodable RUN_END with an unknown outcome does not establish
 * that it is safe to export the run's hidden generation history unredacted.
 */
public class ArchiveRunEndOutcomePrivacyTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private TestLogs run(File root, String fileId, int outcome) {
        TestLogs log = new TestLogs(root, fileId, V, 1000)
                .seeds(424242L, SECRET_SEED)
                .start("logical-" + fileId, 0, "MAGE", RS, 0, 0, false)
                .enter(1)
                .registered("hidden-ring", SECRET_CLASS, 3, true, 0);
        log.end(outcome, "Rat", 1, 1, 100).close(true);
        return log;
    }

    private static Map<String, byte[]> entries(byte[] source) throws Exception {
        Map<String, byte[]> result = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(source))) {
            ZipEntry e;
            byte[] buffer = new byte[4096];
            while ((e = zip.getNextEntry()) != null) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int n;
                while ((n = zip.read(buffer)) != -1) bytes.write(buffer, 0, n);
                result.put(e.getName(), bytes.toByteArray());
            }
        }
        return result;
    }

    @Test public void unknownRunEndOutcomeKeepsHiddenEvidenceRedacted() throws Exception {
        for (int outcome : new int[]{3, 99}) {
            String fileId = "unknown-" + outcome;
            File root = tmp.newFolder(fileId);
            TestLogs log = run(root, fileId, outcome);
            byte[] original = Files.readAllBytes(log.file.toPath());
            assertTrue(contains(original, SECRET_CLASS));
            assertTrue(contains(original, SECRET_SEED));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ArchiveExporter.Report report = ArchiveExporter.export(root, output, "test", 123);
            assertTrue(report.summary(), report.ok);
            assertEquals(1, report.runLogs);
            assertEquals(1, report.redactedRunLogs);
            Map<String, byte[]> zipped = entries(output.toByteArray());
            String path = "runs/" + fileId + ".apdlog";
            ArchiveManifest manifest = ArchiveManifest.parse(zipped.get(ArchiveManifest.ENTRY_NAME));
            ArchiveManifest.Entry entry = manifest.find(path);
            assertNotNull(entry);
            assertTrue("invalid outcome must not prove completion", entry.redacted);
            byte[] copy = zipped.get(path);
            assertNotNull(copy);
            assertTrue(LogRedactor.isRedacted(RunLogReader.parse(copy).header.writerBuild));
            assertFalse(contains(copy, SECRET_CLASS));
            assertFalse(contains(copy, SECRET_SEED));
            assertArrayEquals("export must not touch source history", original, Files.readAllBytes(log.file.toPath()));

            File restored = tmp.newFolder("restore-" + fileId);
            ArchiveImporter.Report imported = ArchiveImporter.importArchive(restored,
                    new ByteArrayInputStream(output.toByteArray()));
            assertTrue(imported.summary(), imported.ok);
            assertEquals(1, imported.imported);
        }
    }

    @Test public void everyDefinedRunEndOutcomeStaysLossless() throws Exception {
        for (int outcome : new int[]{
                LifecycleEvents.OUTCOME_DIED,
                LifecycleEvents.OUTCOME_WON,
                LifecycleEvents.OUTCOME_ABANDONED}) {
            String fileId = "valid-" + outcome;
            File root = tmp.newFolder(fileId);
            TestLogs log = run(root, fileId, outcome);
            byte[] original = Files.readAllBytes(log.file.toPath());

            Map<String, byte[]> zipped = entries(ArchiveSupport.export(root));
            String path = "runs/" + fileId + ".apdlog";
            ArchiveManifest manifest = ArchiveManifest.parse(zipped.get(ArchiveManifest.ENTRY_NAME));
            ArchiveManifest.Entry entry = manifest.find(path);
            assertNotNull(entry);
            assertFalse("valid terminal outcome must remain lossless", entry.redacted);
            assertArrayEquals(original, zipped.get(path));
            assertArrayEquals(original, Files.readAllBytes(log.file.toPath()));
        }
    }
}
