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
import java.util.ArrayList;
import java.util.List;

// Present, manifest-listed ZIP entries can each fail size/checksum validation.
// They must all be rejected without retaining hundreds of diagnostic strings.
public class ArchiveImporterInvalidEntryDetailsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static byte[] archiveWithInvalidEntries(File scratch, int invalidCount) throws Exception {
        byte[] valid = ArchiveSupport.smallLog(scratch, "valid", "run-valid");
        ArchiveManifest manifest = new ArchiveManifest();
        manifest.container = 1;

        ArchiveManifest.Entry present = new ArchiveManifest.Entry();
        present.path = "runs/valid.apdlog";
        present.runId = "run-valid";
        present.kind = ArchiveManifest.KIND_RUN;
        present.size = valid.length;
        present.sha256 = ArchiveManifest.sha256Hex(valid);
        manifest.files.add(present);

        List<String> names = new ArrayList<>();
        List<byte[]> contents = new ArrayList<>();
        names.add(ArchiveManifest.ENTRY_NAME);
        contents.add(null); // Fill after all manifest entries are known.
        names.add(present.path);
        contents.add(valid);

        for (int i = 0; i < invalidCount; i++) {
            String path = "runs/invalid-" + i + ".apdlog";
            byte[] payload = new byte[]{1, 2};
            ArchiveManifest.Entry wrong = new ArchiveManifest.Entry();
            wrong.path = path;
            wrong.runId = "run-invalid-" + i;
            wrong.kind = ArchiveManifest.KIND_RUN;
            wrong.size = payload.length;
            wrong.sha256 = ArchiveManifest.sha256Hex(new byte[]{3, 4});
            manifest.files.add(wrong);
            names.add(path);
            contents.add(payload);
        }
        contents.set(0, manifest.encode().getBytes(ArchiveManifest.utf8()));
        return ArchiveSupport.zip(names, contents);
    }

    @Test
    public void fewInvalidPresentEntriesRetainOriginalDiagnosticNames() throws Exception {
        byte[] zip = archiveWithInvalidEntries(temporaryFolder.newFolder("scratch-few"), 2);
        File history = temporaryFolder.newFolder("history-few");

        ArchiveImporter.Report report = ArchiveImporter.importArchive(history, new ByteArrayInputStream(zip));

        assertFalse(report.ok);
        assertEquals(0, report.imported);
        assertEquals(2, report.rejected);
        assertEquals(2, report.details.size());
        assertEquals("runs/invalid-0.apdlog: does not match the manifest (size or checksum).",
                report.details.get(0));
        assertEquals("runs/invalid-1.apdlog: does not match the manifest (size or checksum).",
                report.details.get(1));
        assertFalse(new File(history, "runs/valid.apdlog").exists());
        assertFalse(new File(history, "import-tmp").exists());
    }

    @Test
    public void manyInvalidPresentEntriesKeepExactRejectionCountAndOriginalHistory() throws Exception {
        byte[] zip = archiveWithInvalidEntries(temporaryFolder.newFolder("scratch-many"), 250);
        File history = temporaryFolder.newFolder("history-many");
        File runs = new File(history, "runs");
        assertTrue(runs.mkdir());
        File existing = new File(runs, "existing.apdlog");
        byte[] original = new byte[]{7, 8, 9, 0};
        Files.write(existing.toPath(), original);

        ArchiveImporter.Report report = ArchiveImporter.importArchive(history, new ByteArrayInputStream(zip));

        assertFalse(report.ok);
        assertEquals(0, report.imported);
        assertEquals(250, report.rejected);
        assertTrue(report.problem, report.problem.contains("250 problems"));
        assertEquals(101, report.details.size());
        assertEquals("runs/invalid-0.apdlog: does not match the manifest (size or checksum).",
                report.details.get(0));
        assertEquals("runs/invalid-99.apdlog: does not match the manifest (size or checksum).",
                report.details.get(99));
        assertEquals("Additional invalid manifest-listed archive files were rejected (details omitted).",
                report.details.get(100));
        assertFalse(report.summary().contains("runs/invalid-249.apdlog"));
        assertArrayEquals(original, Files.readAllBytes(existing.toPath()));
        assertFalse(new File(runs, "valid.apdlog").exists());
        assertFalse(new File(history, "import-tmp").exists());
    }
}
