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

// A checksum proves the manifest bytes have not changed; it cannot make a
// negative on-disk (unsigned-byte) log-container version meaningful.
// Previously accepted zero is retained for existing format-1 archives.
public class ArchiveManifestNegativeContainerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static ArchiveManifest sample(byte[] log, int version) {
        ArchiveManifest m = new ArchiveManifest();
        m.container = version;
        ArchiveManifest.Entry e = new ArchiveManifest.Entry();
        e.path = "runs/incoming.apdlog";
        e.kind = ArchiveManifest.KIND_RUN;
        e.runId = "logical-incoming";
        e.size = log.length;
        e.sha256 = ArchiveManifest.sha256Hex(log);
        e.events = 1;
        m.files.add(e);
        return m;
    }

    @Test
    public void checksumValidNegativeContainerVersionsAreRejectedButZeroAndOneRemainValid() {
        for (int version : new int[]{-1, Integer.MIN_VALUE}) {
            ArchiveManifest m = new ArchiveManifest();
            m.container = version;
            try {
                ArchiveManifest.parse(m.encode().getBytes(ArchiveManifest.utf8()));
                fail("accepted negative container version " + version);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("invalid container version"));
            }
        }
        for (int version : new int[]{0, 1}) {
            ArchiveManifest m = new ArchiveManifest();
            m.container = version;
            assertEquals(version, ArchiveManifest.parse(m.encode().getBytes(ArchiveManifest.utf8())).container);
        }
    }

    @Test
    public void negativeContainerInRealZipRefusesImportWithoutTouchingExistingHistory() throws Exception {
        File root = temporaryFolder.newFolder("history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File existing = new File(runs, "keep.apdlog");
        byte[] original = "real history must be preserved".getBytes(ArchiveManifest.utf8());
        Files.write(existing.toPath(), original);

        byte[] incoming = ArchiveSupport.smallLog(temporaryFolder.newFolder("scratch"),
                "incoming", "logical-incoming");
        ArchiveManifest manifest = sample(incoming, -1);
        byte[] archive = ArchiveSupport.zip(
                ArchiveSupport.names(ArchiveManifest.ENTRY_NAME, "runs/incoming.apdlog"),
                Arrays.asList(manifest.encode().getBytes(ArchiveManifest.utf8()), incoming));

        ArchiveImporter.Report result = ArchiveImporter.importArchive(root, new ByteArrayInputStream(archive));
        assertFalse(result.summary(), result.ok);
        assertTrue(result.problem, result.problem.contains("invalid container version"));
        assertEquals(0, result.imported);
        assertArrayEquals(original, Files.readAllBytes(existing.toPath()));
        assertFalse(new File(runs, "incoming.apdlog").exists());
        assertFalse(new File(root, "import-tmp").exists());
    }
}
