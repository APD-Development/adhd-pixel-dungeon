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

// Format-1 manifests have one totals line; their SHA-256 does not justify accepting conflicting repeats.
public class ArchiveManifestDuplicateCountTest {

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

    private static byte[] repeatTotals(ArchiveManifest m, String extraTotals) {
        String original = m.encode();
        int checkAt = original.lastIndexOf("check\t");
        String body = original.substring(0, checkAt);
        int countAt = body.lastIndexOf("\ncount\t");
        assertTrue("missing original totals line", countAt >= 0);
        String withExtra = body.substring(0, countAt + 1) + extraTotals + "\n" + body.substring(countAt + 1);
        return (withExtra + "check\t" +
                ArchiveManifest.sha256Hex(withExtra.getBytes(ArchiveManifest.utf8())) + "\n")
                .getBytes(ArchiveManifest.utf8());
    }

    private static void assertRefusesRepeated(byte[] manifestBytes) {
        try {
            ArchiveManifest.parse(manifestBytes);
            fail("checksum-valid manifest accepted duplicate totals");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("repeated totals"));
        }
    }

    @Test
    public void contradictoryRepeatedTotalsAreRefusedDespiteValidChecksum() {
        ArchiveManifest manifest = sample(new byte[]{1, 2, 3});
        assertRefusesRepeated(repeatTotals(manifest, "count\t0\t0"));
        assertRefusesRepeated(repeatTotals(manifest, "count\t1\t99"));
    }

    @Test
    public void identicalRepeatedTotalsAreAlsoRefused() {
        byte[] log = new byte[]{1, 2, 3};
        ArchiveManifest manifest = sample(log);
        assertRefusesRepeated(repeatTotals(manifest, "count\t1\t" + log.length));
    }

    @Test
    public void ordinarySingleTotalsRecordStillRoundTrips() {
        ArchiveManifest manifest = sample(new byte[]{1, 2, 3});
        ArchiveManifest parsed = ArchiveManifest.parse(manifest.encode().getBytes(ArchiveManifest.utf8()));
        assertEquals(1, parsed.files.size());
        assertEquals(3, parsed.files.get(0).size);
    }

    @Test
    public void repeatedTotalsStopZipImportBeforeWritingAnyHistory() throws Exception {
        File root = tmp.newFolder("history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File prior = new File(runs, "keep.apdlog");
        byte[] priorBytes = "original history untouched".getBytes(ArchiveManifest.utf8());
        Files.write(prior.toPath(), priorBytes);

        byte[] log = ArchiveSupport.smallLog(tmp.newFolder("scratch"), "a", "run-a");
        byte[] invalidManifest = repeatTotals(sample(log), "count\t0\t0");
        byte[] zip = ArchiveSupport.zip(
                ArchiveSupport.names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"),
                Arrays.asList(invalidManifest, log));

        ArchiveImporter.Report report = ArchiveImporter.importArchive(root, new ByteArrayInputStream(zip));
        assertFalse(report.summary(), report.ok);
        assertTrue(report.problem, report.problem.contains("repeated totals"));
        assertEquals(0, report.imported);
        assertArrayEquals(priorBytes, Files.readAllBytes(prior.toPath()));
        assertFalse(new File(runs, "a.apdlog").exists());
        assertFalse(new File(root, "import-tmp").exists());
    }
}
