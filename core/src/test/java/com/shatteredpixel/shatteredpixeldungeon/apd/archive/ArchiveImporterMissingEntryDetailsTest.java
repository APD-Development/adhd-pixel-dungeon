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
import java.util.Arrays;

// A checksum-valid manifest may list far more entries than a ZIP contains.
// Refuse the entire import and count every absence, without retaining a
// potentially enormous list of individual names in the report.
public class ArchiveImporterMissingEntryDetailsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static byte[] archiveWithMissing(File scratch, int missing) throws Exception {
        byte[] valid = ArchiveSupport.smallLog(scratch, "present", "run-present");
        ArchiveManifest manifest = new ArchiveManifest();
        manifest.container = 1;

        ArchiveManifest.Entry present = new ArchiveManifest.Entry();
        present.path = "runs/present.apdlog";
        present.runId = "run-present";
        present.kind = ArchiveManifest.KIND_RUN;
        present.size = valid.length;
        present.sha256 = ArchiveManifest.sha256Hex(valid);
        manifest.files.add(present);

        for (int i = 0; i < missing; i++) {
            ArchiveManifest.Entry absent = new ArchiveManifest.Entry();
            absent.path = "runs/absent-" + i + ".apdlog";
            absent.runId = "run-absent-" + i;
            absent.kind = ArchiveManifest.KIND_RUN;
            absent.size = 0;
            absent.sha256 = ArchiveManifest.sha256Hex(new byte[0]);
            manifest.files.add(absent);
        }

        return ArchiveSupport.zip(
                Arrays.asList(ArchiveManifest.ENTRY_NAME, present.path),
                Arrays.asList(manifest.encode().getBytes(ArchiveManifest.utf8()), valid));
    }

    @Test
    public void fewMissingEntriesRetainEveryOriginalDiagnostic() throws Exception {
        byte[] zip = archiveWithMissing(temporaryFolder.newFolder("scratch-small"), 2);
        File root = temporaryFolder.newFolder("history-small");
        ArchiveImporter.Report report = ArchiveImporter.importArchive(root, new ByteArrayInputStream(zip));

        assertFalse(report.ok);
        assertEquals(0, report.imported);
        assertEquals(2, report.rejected);
        assertEquals(2, report.details.size());
        assertEquals("listed but missing from the archive: runs/absent-0.apdlog", report.details.get(0));
        assertEquals("listed but missing from the archive: runs/absent-1.apdlog", report.details.get(1));
        assertFalse(new File(root, "runs/present.apdlog").exists());
        assertFalse(new File(root, "import-tmp").exists());
    }

    @Test
    public void hundredsOfMissingEntriesAreAllRejectedWithoutGrowingReportOrChangingHistory() throws Exception {
        byte[] zip = archiveWithMissing(temporaryFolder.newFolder("scratch-many"), 250);
        File root = temporaryFolder.newFolder("history-many");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File original = new File(runs, "existing.apdlog");
        byte[] originalBytes = new byte[]{7, 0, 9, 2, 6};
        Files.write(original.toPath(), originalBytes);

        ArchiveImporter.Report report = ArchiveImporter.importArchive(root, new ByteArrayInputStream(zip));

        assertFalse(report.ok);
        assertEquals(0, report.imported);
        assertEquals(250, report.rejected);
        assertTrue(report.problem, report.problem.contains("250 problems"));
        assertEquals(101, report.details.size());
        assertEquals("listed but missing from the archive: runs/absent-0.apdlog", report.details.get(0));
        assertEquals("listed but missing from the archive: runs/absent-99.apdlog", report.details.get(99));
        assertEquals("Additional manifest-listed files were missing (details omitted).", report.details.get(100));
        assertFalse(report.summary().contains("runs/absent-249.apdlog"));
        assertArrayEquals(originalBytes, Files.readAllBytes(original.toPath()));
        assertFalse(new File(runs, "present.apdlog").exists());
        assertFalse(new File(root, "import-tmp").exists());
    }
}
