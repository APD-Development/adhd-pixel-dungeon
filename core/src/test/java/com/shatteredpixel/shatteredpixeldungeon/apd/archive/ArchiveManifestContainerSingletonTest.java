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

// Format-1 archives have one authoritative raw-log container-format record.
public class ArchiveManifestContainerSingletonTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static ArchiveManifest sample(byte[] log) {
        ArchiveManifest m = new ArchiveManifest();
        m.createdMillis = 1234;
        m.app = "APD";
        m.telemetry = "1.5";
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

    private static byte[] insertBeforeTotals(ArchiveManifest m, String added) {
        String encoded = m.encode();
        String body = encoded.substring(0, encoded.lastIndexOf("check\t"));
        int position = body.lastIndexOf("\ncount\t");
        assertTrue("missing original totals record", position >= 0);
        String changed = body.substring(0, position + 1) + added + "\n" + body.substring(position + 1);
        return (changed + "check\t" +
                ArchiveManifest.sha256Hex(changed.getBytes(ArchiveManifest.utf8())) + "\n")
                .getBytes(ArchiveManifest.utf8());
    }

    private static void rejectsDuplicate(byte[] data) {
        try {
            ArchiveManifest.parse(data);
            fail("accepted a checksummed duplicate container record");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(),
                    expected.getMessage().contains("repeated container metadata"));
        }
    }

    @Test
    public void contradictoryContainerRecordsAreRefusedWithValidChecksum() {
        ArchiveManifest m = sample(new byte[]{1, 2, 3});
        rejectsDuplicate(insertBeforeTotals(m, "container\t0"));
        rejectsDuplicate(insertBeforeTotals(m, "container\t99"));
    }

    @Test
    public void identicalContainerRecordIsAlsoRefused() {
        rejectsDuplicate(insertBeforeTotals(sample(new byte[]{1, 2, 3}), "container\t1"));
    }

    @Test
    public void normalContainerAndUnknownFutureMetadataStillParse() {
        ArchiveManifest m = sample(new byte[]{1, 2, 3});
        ArchiveManifest plain = ArchiveManifest.parse(m.encode().getBytes(ArchiveManifest.utf8()));
        assertEquals(1, plain.container);
        assertEquals(1, plain.files.size());
        ArchiveManifest extended = ArchiveManifest.parse(insertBeforeTotals(m, "future-container-info\tvalue"));
        assertEquals(1, extended.container);
        assertEquals(1, extended.files.size());
        assertEquals(m.files.get(0).sha256, extended.files.get(0).sha256);
    }

    @Test
    public void repeatedContainerZipImportPreservesOriginalHistory() throws Exception {
        File root = tmp.newFolder("history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File prior = new File(runs, "keep.apdlog");
        byte[] before = "existing player history".getBytes(ArchiveManifest.utf8());
        Files.write(prior.toPath(), before);

        byte[] log = ArchiveSupport.smallLog(tmp.newFolder("scratch"), "a", "run-a");
        byte[] badManifest = insertBeforeTotals(sample(log), "container\t99");
        byte[] zip = ArchiveSupport.zip(
                ArchiveSupport.names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"),
                Arrays.asList(badManifest, log));

        ArchiveImporter.Report report = ArchiveImporter.importArchive(root, new ByteArrayInputStream(zip));
        assertFalse(report.summary(), report.ok);
        assertTrue(report.problem, report.problem.contains("repeated container metadata"));
        assertEquals(0, report.imported);
        assertArrayEquals(before, Files.readAllBytes(prior.toPath()));
        assertFalse(new File(runs, "a.apdlog").exists());
        assertFalse(new File(root, "import-tmp").exists());
    }
}
