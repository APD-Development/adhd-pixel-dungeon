package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

// An archive checksum authenticates its bytes, not impossible event metadata.
public class ArchiveManifestEventCountTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static ArchiveManifest sample(byte[] log, long count) {
        ArchiveManifest manifest = new ArchiveManifest();
        manifest.createdMillis = 1234;
        manifest.app = "APD";
        manifest.container = 1;
        ArchiveManifest.Entry entry = new ArchiveManifest.Entry();
        entry.path = "runs/a.apdlog";
        entry.size = log.length;
        entry.sha256 = ArchiveManifest.sha256Hex(log);
        entry.kind = ArchiveManifest.KIND_RUN;
        entry.runId = "run-a";
        entry.events = count;
        entry.status = "SEALED";
        manifest.files.add(entry);
        return manifest;
    }

    @Test
    public void negativeEventCountsAreRefusedWithValidChecksums() {
        for (long count : new long[]{-1L, -2L, Long.MIN_VALUE}) {
            ArchiveManifest malformed = sample(new byte[]{1}, count);
            try {
                ArchiveManifest.parse(malformed.encode().getBytes(ArchiveManifest.utf8()));
                fail("checksum-valid manifest accepted negative event count " + count);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("negative event count"));
            }
        }
    }

    @Test
    public void nonnegativeEventCountsRoundTripUnchanged() {
        for (long count : new long[]{0L, 1L, 12345L, Long.MAX_VALUE}) {
            ArchiveManifest original = sample(new byte[]{1}, count);
            ArchiveManifest restored = ArchiveManifest.parse(original.encode().getBytes(ArchiveManifest.utf8()));
            assertEquals(1, restored.files.size());
            assertEquals(count, restored.files.get(0).events);
            assertEquals(original.files.get(0).sha256, restored.files.get(0).sha256);
        }
    }

    @Test
    public void negativeEventCountStopsImportWithoutHistoryMutation() throws Exception {
        File root = tmp.newFolder("history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File prior = new File(runs, "keep.apdlog");
        byte[] original = "prior player history".getBytes(ArchiveManifest.utf8());
        Files.write(prior.toPath(), original);

        byte[] log = ArchiveSupport.smallLog(tmp.newFolder("scratch"), "a", "run-a");
        ArchiveManifest malformed = sample(log, -1);
        byte[] zip = ArchiveSupport.zip(
                ArchiveSupport.names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"),
                Arrays.asList(malformed.encode().getBytes(ArchiveManifest.utf8()), log));

        ArchiveImporter.Report report = ArchiveImporter.importArchive(root, new ByteArrayInputStream(zip));
        assertFalse(report.summary(), report.ok);
        assertTrue(report.problem, report.problem.contains("negative event count"));
        assertEquals(0, report.imported);
        assertArrayEquals(original, Files.readAllBytes(prior.toPath()));
        assertFalse(new File(runs, "a.apdlog").exists());
        assertFalse(new File(root, "import-tmp").exists());
    }
}
