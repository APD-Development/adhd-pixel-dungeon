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

// The last correctly checksummed record cannot legitimize an earlier "check" record.
public class ArchiveManifestChecksumSingletonTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static ArchiveManifest sample(byte[] log) {
        ArchiveManifest m = new ArchiveManifest();
        m.createdMillis = 1234;
        m.app = "APD";
        m.container = 1;
        ArchiveManifest.Entry e = new ArchiveManifest.Entry();
        e.path = "runs/a.apdlog";
        e.size = log.length;
        e.sha256 = ArchiveManifest.sha256Hex(log);
        e.kind = ArchiveManifest.KIND_RUN;
        e.runId = "run-a";
        e.events = 1;
        m.files.add(e);
        return m;
    }

    private static byte[] appendValidFinalChecksum(String existingManifest) {
        String finalChecksum = ArchiveManifest.sha256Hex(
                existingManifest.getBytes(ArchiveManifest.utf8()));
        return (existingManifest + "check\t" + finalChecksum + "\n")
                .getBytes(ArchiveManifest.utf8());
    }

    private static void refuses(byte[] bytes) {
        try {
            ArchiveManifest.parse(bytes);
            fail("accepted archive with two check records and a valid final checksum");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("repeated checksums"));
        }
    }

    @Test
    public void secondCorrectFinalChecksumDoesNotLegitimizeFirstChecksum() {
        ArchiveManifest m = sample(new byte[]{1, 2, 3});
        refuses(appendValidFinalChecksum(m.encode()));
    }

    @Test
    public void secondValidChecksumCannotHideConflictingEarlierChecksum() {
        ArchiveManifest m = sample(new byte[]{1, 2, 3});
        String encoded = m.encode();
        String wrongFirst = encoded.replaceFirst("check\\t[0-9a-f]{64}\\n$",
                "check\t" + new String(new char[64]).replace('\0', '0') + "\n");
        assertFalse("fixture must alter first checksum", wrongFirst.equals(encoded));
        refuses(appendValidFinalChecksum(wrongFirst));
    }

    @Test
    public void ordinarySingleChecksumAndUnknownExtensionStillRoundTrip() {
        ArchiveManifest m = sample(new byte[]{1, 2, 3});
        ArchiveManifest ordinary = ArchiveManifest.parse(m.encode().getBytes(ArchiveManifest.utf8()));
        assertEquals(1, ordinary.files.size());
        String original = m.encode();
        int checksumAt = original.lastIndexOf("check\t");
        String body = original.substring(0, checksumAt);
        String extended = body + "future-check-variant\tvalue\n";
        byte[] encoded = (extended + "check\t"
                + ArchiveManifest.sha256Hex(extended.getBytes(ArchiveManifest.utf8())) + "\n")
                .getBytes(ArchiveManifest.utf8());
        ArchiveManifest parsed = ArchiveManifest.parse(encoded);
        assertEquals(1, parsed.files.size());
        assertEquals("runs/a.apdlog", parsed.files.get(0).path);
    }

    @Test
    public void duplicateChecksumZipImportRefusesWithoutChangingOldHistory() throws Exception {
        File root = tmp.newFolder("history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File prior = new File(runs, "keep.apdlog");
        byte[] priorBytes = "existing history must remain unchanged"
                .getBytes(ArchiveManifest.utf8());
        Files.write(prior.toPath(), priorBytes);

        byte[] log = ArchiveSupport.smallLog(tmp.newFolder("scratch"), "a", "run-a");
        byte[] manifest = appendValidFinalChecksum(sample(log).encode());
        byte[] zip = ArchiveSupport.zip(
                ArchiveSupport.names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"),
                Arrays.asList(manifest, log));
        ArchiveImporter.Report report = ArchiveImporter.importArchive(root, new ByteArrayInputStream(zip));
        assertFalse(report.summary(), report.ok);
        assertTrue(report.problem, report.problem.contains("repeated checksums"));
        assertEquals(0, report.imported);
        assertArrayEquals(priorBytes, Files.readAllBytes(prior.toPath()));
        assertFalse(new File(runs, "a.apdlog").exists());
        assertFalse(new File(root, "import-tmp").exists());
    }
}
