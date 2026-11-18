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

// The manifest's one terminal checksum cannot authenticate bytes appended after it.
public class ArchiveManifestChecksumFooterTest {

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

    private static void refuses(String text) {
        try {
            ArchiveManifest.parse(text.getBytes(ArchiveManifest.utf8()));
            fail("accepted unchecked bytes after the terminal archive checksum");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("damaged checksum footer"));
        }
    }

    @Test
    public void exactWriterFooterAndExistingLineEndingVariantsStillParse() {
        String encoded = sample(new byte[]{1, 2, 3}).encode();
        assertEquals(1, ArchiveManifest.parse(encoded.getBytes(ArchiveManifest.utf8())).files.size());
        String noNewline = encoded.substring(0, encoded.length() - 1);
        assertEquals(1, ArchiveManifest.parse(noNewline.getBytes(ArchiveManifest.utf8())).files.size());
        assertEquals(1, ArchiveManifest.parse((noNewline + "\r\n").getBytes(ArchiveManifest.utf8())).files.size());
        int checkAt = noNewline.lastIndexOf("check\t");
        String uppercaseChecksum = noNewline.substring(0, checkAt + "check\t".length())
                + noNewline.substring(checkAt + "check\t".length()).toUpperCase(java.util.Locale.ROOT);
        assertEquals(1, ArchiveManifest.parse(uppercaseChecksum.getBytes(ArchiveManifest.utf8())).files.size());
    }

    @Test
    public void uncheckedBlankRecordAndTrailingSpacesCannotBeTrimmedAway() {
        String encoded = sample(new byte[]{1, 2, 3}).encode();
        refuses(encoded + "\n");
        refuses(encoded + "\n\n");
        refuses(encoded + "  ");
        refuses(encoded + "\t");
        refuses(encoded.substring(0, encoded.length() - 1) + " \n");
    }

    @Test
    public void uncheckedSuffixInZipRefusesWithoutTouchingOriginalHistory() throws Exception {
        File root = tmp.newFolder("existing-history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File prior = new File(runs, "keep.apdlog");
        byte[] original = "existing APD history must remain unchanged".getBytes(ArchiveManifest.utf8());
        Files.write(prior.toPath(), original);

        byte[] incoming = ArchiveSupport.smallLog(tmp.newFolder("incoming-source"), "a", "run-a");
        byte[] damaged = (sample(incoming).encode() + "\n").getBytes(ArchiveManifest.utf8());
        byte[] archive = ArchiveSupport.zip(
                ArchiveSupport.names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"),
                Arrays.asList(damaged, incoming));
        ArchiveImporter.Report result = ArchiveImporter.importArchive(root, new ByteArrayInputStream(archive));
        assertFalse(result.summary(), result.ok);
        assertTrue(result.problem, result.problem.contains("damaged checksum footer"));
        assertEquals(0, result.imported);
        assertArrayEquals(original, Files.readAllBytes(prior.toPath()));
        assertFalse(new File(runs, "a.apdlog").exists());
        assertFalse(new File(root, "import-tmp").exists());
    }
}
