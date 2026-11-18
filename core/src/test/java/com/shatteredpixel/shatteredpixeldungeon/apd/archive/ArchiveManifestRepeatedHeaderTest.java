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

// A matching checksum does not make repeated format headers unambiguous.
public class ArchiveManifestRepeatedHeaderTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static ArchiveManifest sample(byte[] log) {
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
        entry.events = 1;
        manifest.files.add(entry);
        return manifest;
    }

    private static byte[] withExtraLine(ArchiveManifest manifest, String extraLine) {
        String original = manifest.encode();
        String body = original.substring(0, original.lastIndexOf("check\t"));
        int countAt = body.lastIndexOf("\ncount\t");
        assertTrue("expected single count line", countAt >= 0);
        String changed = body.substring(0, countAt + 1) + extraLine + "\n" + body.substring(countAt + 1);
        return (changed + "check\t" +
                ArchiveManifest.sha256Hex(changed.getBytes(ArchiveManifest.utf8())) + "\n")
                .getBytes(ArchiveManifest.utf8());
    }

    private static void assertRejectsDuplicate(byte[] bytes) {
        try {
            ArchiveManifest.parse(bytes);
            fail("checksum-valid archive accepted a second format header");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("repeated headers"));
        }
    }

    @Test
    public void identicalRepeatedHeadersAreRejectedWithValidChecksum() {
        assertRejectsDuplicate(withExtraLine(sample(new byte[]{1, 2, 3}), "APD-ARCHIVE\t1"));
    }

    @Test
    public void conflictingRepeatedHeadersAreRejectedBeforeChangingFormat() {
        assertRejectsDuplicate(withExtraLine(sample(new byte[]{1, 2, 3}), "APD-ARCHIVE\t0"));
        assertRejectsDuplicate(withExtraLine(sample(new byte[]{1, 2, 3}), "APD-ARCHIVE\t99"));
    }

    @Test
    public void singleHeaderAndUnknownFutureMetadataStillRoundTrip() {
        ArchiveManifest m = sample(new byte[]{1, 2, 3});
        ArchiveManifest ordinary = ArchiveManifest.parse(m.encode().getBytes(ArchiveManifest.utf8()));
        assertEquals(1, ordinary.format);
        assertEquals(1, ordinary.files.size());
        ArchiveManifest extended = ArchiveManifest.parse(withExtraLine(m, "future-record\tvalue"));
        assertEquals(1, extended.format);
        assertEquals(1, extended.files.size());
        assertEquals(m.files.get(0).sha256, extended.files.get(0).sha256);
    }

    @Test
    public void repeatedHeaderZipImportPreservesOriginalHistory() throws Exception {
        File root = tmp.newFolder("history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File prior = new File(runs, "keep.apdlog");
        byte[] original = "existing player history".getBytes(ArchiveManifest.utf8());
        Files.write(prior.toPath(), original);

        byte[] log = ArchiveSupport.smallLog(tmp.newFolder("scratch"), "a", "run-a");
        byte[] malformed = withExtraLine(sample(log), "APD-ARCHIVE\t1");
        byte[] zip = ArchiveSupport.zip(
                ArchiveSupport.names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"),
                Arrays.asList(malformed, log));

        ArchiveImporter.Report report = ArchiveImporter.importArchive(root, new ByteArrayInputStream(zip));
        assertFalse(report.summary(), report.ok);
        assertTrue(report.problem, report.problem.contains("repeated headers"));
        assertEquals(0, report.imported);
        assertArrayEquals(original, Files.readAllBytes(prior.toPath()));
        assertFalse(new File(runs, "a.apdlog").exists());
        assertFalse(new File(root, "import-tmp").exists());
    }
}
