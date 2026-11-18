package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

// Hash comparisons may share scratch bytes, but must never replace local history.
public class ArchiveImporterExistingHashBufferTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void repeatedExistingHashChecksPreserveSkipCollisionAndNewLogBytes() throws Exception {
        File archivedScratch = temporaryFolder.newFolder("archived-scratch");
        File localScratch = temporaryFolder.newFolder("local-scratch");
        File root = temporaryFolder.newFolder("history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());

        Map<String, byte[]> archived = new TreeMap<>();
        Map<String, String> runIds = new TreeMap<>();
        Map<String, byte[]> originals = new TreeMap<>();
        final int identical = 96;
        final int different = 12;

        for (int i = 0; i < identical + different; i++) {
            String id = String.format(java.util.Locale.ROOT, "local-%03d", i);
            String path = "runs/" + id + ".apdlog";
            String runId = "archived-" + id;
            byte[] incoming = ArchiveSupport.smallLog(archivedScratch, id, runId);
            byte[] existing = i < identical ? incoming.clone()
                    : ArchiveSupport.smallLog(localScratch, id, "different-" + id);
            archived.put(path, incoming);
            runIds.put(path, runId);
            originals.put(path, existing);
            Files.write(new File(runs, id + ".apdlog").toPath(), existing);
        }

        byte[] newLog = ArchiveSupport.smallLog(archivedScratch, "new-log", "new-run");
        archived.put("runs/new-log.apdlog", newLog);
        runIds.put("runs/new-log.apdlog", "new-run");

        ArchiveImporter.Report report = ArchiveImporter.importArchive(root,
                new ByteArrayInputStream(ArchiveSupport.archiveOf(archived, runIds)));

        assertTrue(report.summary(), report.ok);
        assertEquals(1, report.imported);
        assertEquals(identical, report.skippedAlreadyHere);
        assertEquals(different, report.keptDifferentExisting);
        assertEquals(0, report.rejected);
        assertEquals(different, report.details.size());
        for (Map.Entry<String, byte[]> entry : originals.entrySet()) {
            assertArrayEquals(entry.getKey(), entry.getValue(),
                    Files.readAllBytes(new File(root, entry.getKey()).toPath()));
        }
        assertArrayEquals(newLog, Files.readAllBytes(new File(runs, "new-log.apdlog").toPath()));
        assertFalse(new File(root, "import-tmp").exists());
    }
}
