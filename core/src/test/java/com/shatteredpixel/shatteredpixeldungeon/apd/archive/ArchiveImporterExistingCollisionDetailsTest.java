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

// Every checksum-valid archive entry must still be verified even if many
// existing filenames have different bytes. No original log may be replaced;
// the report should not retain an unbounded list of collision filenames.
public class ArchiveImporterExistingCollisionDetailsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final class Fixture {
        byte[] zip;
        byte[] newBytes;
        final Map<String, byte[]> originals = new TreeMap<>();
    }

    private Fixture fixture(File root, String suffix, int collisions) throws Exception {
        File archiveScratch = temporaryFolder.newFolder("archive-" + suffix);
        File originalScratch = temporaryFolder.newFolder("original-" + suffix);
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());

        Map<String, byte[]> archived = new TreeMap<>();
        Map<String, String> runIds = new TreeMap<>();
        Fixture f = new Fixture();

        for (int i = 0; i < collisions; i++) {
            String id = String.format(java.util.Locale.ROOT, "collision-%03d", i);
            String path = "runs/" + id + ".apdlog";
            String archivedRun = "archived-run-" + id;
            byte[] archivedBytes = ArchiveSupport.smallLog(archiveScratch, id, archivedRun);
            byte[] originalBytes = ArchiveSupport.smallLog(originalScratch, id, "local-run-" + id);
            archived.put(path, archivedBytes);
            runIds.put(path, archivedRun);
            Files.write(new File(runs, id + ".apdlog").toPath(), originalBytes);
            f.originals.put(path, originalBytes);
        }

        f.newBytes = ArchiveSupport.smallLog(archiveScratch, "new-record", "new-run");
        archived.put("runs/new-record.apdlog", f.newBytes);
        runIds.put("runs/new-record.apdlog", "new-run");
        f.zip = ArchiveSupport.archiveOf(archived, runIds);
        return f;
    }

    private void assertHistoryPreserved(File root, Fixture f) throws Exception {
        for (Map.Entry<String, byte[]> e : f.originals.entrySet()) {
            assertArrayEquals(e.getKey(), e.getValue(),
                    Files.readAllBytes(new File(root, e.getKey()).toPath()));
        }
        assertArrayEquals(f.newBytes, Files.readAllBytes(new File(root, "runs/new-record.apdlog").toPath()));
        assertFalse(new File(root, "import-tmp").exists());
    }

    @Test
    public void twoDifferentExistingLogsRetainOriginalMessagesAndBytes() throws Exception {
        File root = temporaryFolder.newFolder("history-small");
        Fixture f = fixture(root, "small", 2);

        ArchiveImporter.Report report = ArchiveImporter.importArchive(root, new ByteArrayInputStream(f.zip));

        assertTrue(report.problem, report.ok);
        assertEquals(1, report.imported);
        assertEquals(2, report.keptDifferentExisting);
        assertEquals(0, report.redactedDifferentExisting);
        assertEquals(0, report.rejected);
        assertEquals(2, report.details.size());
        assertEquals("runs/collision-000.apdlog: a different log with this name is already here, yours was kept.",
                report.details.get(0));
        assertEquals("runs/collision-001.apdlog: a different log with this name is already here, yours was kept.",
                report.details.get(1));
        assertHistoryPreserved(root, f);
    }

    @Test
    public void hundredsOfDifferentExistingLogsPreserveEveryOriginalAndBoundReport() throws Exception {
        File root = temporaryFolder.newFolder("history-many");
        Fixture f = fixture(root, "many", 250);

        ArchiveImporter.Report report = ArchiveImporter.importArchive(root, new ByteArrayInputStream(f.zip));

        assertTrue(report.problem, report.ok);
        assertEquals(1, report.imported);
        assertEquals(250, report.keptDifferentExisting);
        assertEquals(0, report.redactedDifferentExisting);
        assertEquals(0, report.rejected);
        assertEquals(101, report.details.size());
        assertEquals("runs/collision-000.apdlog: a different log with this name is already here, yours was kept.",
                report.details.get(0));
        assertEquals("runs/collision-099.apdlog: a different log with this name is already here, yours was kept.",
                report.details.get(99));
        assertEquals("Additional archived files differed from existing history; original files were kept (details omitted).",
                report.details.get(100));
        assertTrue(report.summary().contains("250 differ from a log already here"));
        assertFalse(report.summary().contains("runs/collision-249.apdlog"));
        assertHistoryPreserved(root, f);
    }
}
