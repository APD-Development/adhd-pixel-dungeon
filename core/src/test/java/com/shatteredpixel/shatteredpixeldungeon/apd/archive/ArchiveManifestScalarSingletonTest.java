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

// Format-1 writer emits each scalar provenance record once. Existing omitted
// optional records and unknown future metadata remain backward-compatible.
public class ArchiveManifestScalarSingletonTest {

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

    private static byte[] signedBody(String body) {
        return (body + "check\t" +
                ArchiveManifest.sha256Hex(body.getBytes(ArchiveManifest.utf8())) + "\n")
                .getBytes(ArchiveManifest.utf8());
    }

    private static String body(ArchiveManifest m) {
        String encoded = m.encode();
        return encoded.substring(0, encoded.lastIndexOf("check\t"));
    }

    private static byte[] appendKnownRecord(ArchiveManifest m, String line) {
        String before = body(m);
        int at = before.lastIndexOf("\ncount\t");
        assertTrue("missing count marker", at >= 0);
        return signedBody(before.substring(0, at + 1) + line + "\n" + before.substring(at + 1));
    }

    private static void refuses(byte[] data, String key) {
        try {
            ArchiveManifest.parse(data);
            fail("accepted checksummed repeated " + key + " metadata");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("repeated " + key + " metadata"));
        }
    }

    @Test
    public void checksumValidDuplicateCreatedRecordsAreRefused() {
        ArchiveManifest m = sample(new byte[]{1, 2, 3});
        refuses(appendKnownRecord(m, "created\t9999"), "created");
        refuses(appendKnownRecord(m, "created\t1234"), "created");
    }

    @Test
    public void checksumValidDuplicateAppRecordsAreRefused() {
        ArchiveManifest m = sample(new byte[]{1, 2, 3});
        refuses(appendKnownRecord(m, "app\tOTHER"), "app");
        refuses(appendKnownRecord(m, "app\tAPD"), "app");
    }

    @Test
    public void checksumValidDuplicateTelemetryRecordsAreRefused() {
        ArchiveManifest m = sample(new byte[]{1, 2, 3});
        refuses(appendKnownRecord(m, "telemetry\t9.9"), "telemetry");
        refuses(appendKnownRecord(m, "telemetry\t1.5"), "telemetry");
    }

    @Test
    public void normalUnknownAndOmittedOptionalRecordsRemainCompatible() {
        ArchiveManifest m = sample(new byte[]{1, 2, 3});
        ArchiveManifest parsed = ArchiveManifest.parse(m.encode().getBytes(ArchiveManifest.utf8()));
        assertEquals(1234, parsed.createdMillis);
        assertEquals("APD", parsed.app);
        assertEquals("1.5", parsed.telemetry);
        assertEquals(1, parsed.files.size());

        ArchiveManifest unknown = ArchiveManifest.parse(appendKnownRecord(m, "future-source\tvalue"));
        assertEquals(1234, unknown.createdMillis);
        assertEquals("APD", unknown.app);
        assertEquals("1.5", unknown.telemetry);

        assertEquals(0, ArchiveManifest.parse(signedBody(body(m).replace("created\t1234\n", ""))).createdMillis);
        assertEquals("", ArchiveManifest.parse(signedBody(body(m).replace("app\tAPD\n", ""))).app);
        assertEquals("", ArchiveManifest.parse(signedBody(body(m).replace("telemetry\t1.5\n", ""))).telemetry);
    }

    @Test
    public void duplicateScalarMetadataInZipRefusesWithoutWritingHistory() throws Exception {
        File root = tmp.newFolder("history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File prior = new File(runs, "keep.apdlog");
        byte[] before = "existing player history".getBytes(ArchiveManifest.utf8());
        Files.write(prior.toPath(), before);
        byte[] log = ArchiveSupport.smallLog(tmp.newFolder("scratch"), "a", "run-a");
        ArchiveManifest m = sample(log);

        for (String duplicate : new String[]{"created\t9999", "app\tOTHER", "telemetry\t9.9"}) {
            byte[] manifest = appendKnownRecord(m, duplicate);
            byte[] zip = ArchiveSupport.zip(
                    ArchiveSupport.names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"),
                    Arrays.asList(manifest, log));
            ArchiveImporter.Report result = ArchiveImporter.importArchive(root, new ByteArrayInputStream(zip));
            assertFalse(result.summary(), result.ok);
            assertTrue(result.problem, result.problem.contains("repeated "));
            assertEquals(0, result.imported);
            assertArrayEquals(before, Files.readAllBytes(prior.toPath()));
            assertFalse(new File(runs, "a.apdlog").exists());
            assertFalse(new File(root, "import-tmp").exists());
        }
    }
}
