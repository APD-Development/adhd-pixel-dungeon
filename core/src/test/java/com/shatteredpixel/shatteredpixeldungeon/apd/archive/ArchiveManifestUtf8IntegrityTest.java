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

// A valid checksum of replacement-decoded text cannot authorize invalid bytes
// in the original UTF-8 manifest. Genuine writer-produced Unicode remains valid.
public class ArchiveManifestUtf8IntegrityTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private static ArchiveManifest manifest(byte[] log) {
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

    private static byte[] replacementChecksummedInvalidUtf8(ArchiveManifest m) {
        String encoded = m.encode();
        String body = encoded.substring(0, encoded.lastIndexOf("check\t"));
        int at = body.indexOf("app\tAPD");
        assertTrue("the sample app line is missing", at >= 0);

        byte[] prefix = body.substring(0, at + "app\t".length()).getBytes(ArchiveManifest.utf8());
        byte[] suffix = body.substring(at + "app\tAPD".length()).getBytes(ArchiveManifest.utf8());
        byte[] damagedBody = new byte[prefix.length + 2 + suffix.length];
        System.arraycopy(prefix, 0, damagedBody, 0, prefix.length);
        damagedBody[prefix.length] = (byte) 0xC3; // invalid UTF-8 lead byte followed by '('
        damagedBody[prefix.length + 1] = (byte) 0x28;
        System.arraycopy(suffix, 0, damagedBody, prefix.length + 2, suffix.length);

        // The old replacement decoder checked this normalized text rather than
        // the original bytes; its checksum would match despite corruption.
        String replacementDecoded = new String(damagedBody, ArchiveManifest.utf8());
        String footer = "check\t" +
                ArchiveManifest.sha256Hex(replacementDecoded.getBytes(ArchiveManifest.utf8())) + "\n";
        byte[] checksum = footer.getBytes(ArchiveManifest.utf8());
        byte[] complete = Arrays.copyOf(damagedBody, damagedBody.length + checksum.length);
        System.arraycopy(checksum, 0, complete, damagedBody.length, checksum.length);
        return complete;
    }

    @Test
    public void invalidUtf8IsRejectedEvenWithAReplacementBasedValidChecksum() {
        byte[] invalid = replacementChecksummedInvalidUtf8(manifest(new byte[]{1, 2, 3}));
        // This valid UTF-8 replacement version can genuinely pass the manifest
        // checksum, showing why checksum comparison alone was insufficient.
        ArchiveManifest replaced = ArchiveManifest.parse(
                new String(invalid, ArchiveManifest.utf8()).getBytes(ArchiveManifest.utf8()));
        assertEquals("\uFFFD(", replaced.app);
        try {
            ArchiveManifest.parse(invalid);
            fail("invalid UTF-8 bytes were accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("invalid UTF-8"));
        }
    }

    @Test
    public void legitimateUnicodeManifestMetadataStillRoundTrips() {
        ArchiveManifest sample = manifest(new byte[]{1, 2, 3});
        sample.app = "APD café \uD83D\uDC00";
        sample.telemetry = "v1 π";
        sample.notes.add("owner's history äöü");
        ArchiveManifest parsed = ArchiveManifest.parse(sample.encode().getBytes(ArchiveManifest.utf8()));
        assertEquals(sample.app, parsed.app);
        assertEquals(sample.telemetry, parsed.telemetry);
        assertEquals(sample.notes, parsed.notes);
        assertEquals(1, parsed.files.size());
    }

    @Test
    public void invalidUtf8ZipImportRefusesWithoutTouchingExistingHistory() throws Exception {
        File root = temp.newFolder("history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File previous = new File(runs, "keep.apdlog");
        byte[] original = "existing APD history must remain".getBytes(ArchiveManifest.utf8());
        Files.write(previous.toPath(), original);

        byte[] log = ArchiveSupport.smallLog(temp.newFolder("source"), "a", "run-a");
        byte[] damaged = replacementChecksummedInvalidUtf8(manifest(log));
        byte[] zip = ArchiveSupport.zip(
                ArchiveSupport.names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"),
                Arrays.asList(damaged, log));

        ArchiveImporter.Report report = ArchiveImporter.importArchive(root, new ByteArrayInputStream(zip));
        assertFalse(report.summary(), report.ok);
        assertTrue(report.problem, report.problem.contains("invalid UTF-8"));
        assertEquals(0, report.imported);
        assertArrayEquals(original, Files.readAllBytes(previous.toPath()));
        assertFalse(new File(runs, "a.apdlog").exists());
        assertFalse(new File(root, "import-tmp").exists());
    }
}
