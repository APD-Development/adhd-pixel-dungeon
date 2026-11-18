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
import java.util.Collections;

// Invalid, correctly checksummed manifests must not copy megabytes of an
// untrusted line into the error shown to the player. Ordinary short diagnostics
// and all existing archive refusal/history preservation semantics remain intact.
public class ArchiveManifestDamagedLineDetailTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String ERROR_PREFIX = "the manifest has a damaged line: ";
    private static final String OMITTED = "... (truncated)";
    private static final int MAX_EXCERPT = 160;

    private static byte[] manifestWithMalformedLine(String line) {
        ArchiveManifest manifest = new ArchiveManifest();
        String original = manifest.encode();
        String body = original.substring(0, original.lastIndexOf("check\t"));
        String totals = "count\t0\t0\n";
        assertTrue("fixture must start from an empty manifest", body.contains(totals));
        body = body.replace(totals, line + "\n" + totals);
        return (body + "check\t" + ArchiveManifest.sha256Hex(body.getBytes(ArchiveManifest.utf8())) + "\n")
                .getBytes(ArchiveManifest.utf8());
    }

    private static String rejects(byte[] manifest) {
        try {
            ArchiveManifest.parse(manifest);
            fail("accepted a checksummed malformed manifest line");
            return "";
        } catch (IllegalArgumentException expected) {
            return expected.getMessage();
        }
    }

    private static String longMalformedLine() {
        char[] chars = new char[250_000];
        Arrays.fill(chars, 'x');
        return "file\t" + new String(chars);
    }

    @Test
    public void shortMalformedLineRetainsItsExactOriginalDiagnostic() {
        String line = "file\truns/a.apdlog\tnot-a-size";
        assertEquals(ERROR_PREFIX + line, rejects(manifestWithMalformedLine(line)));
    }

    @Test
    public void hugeMalformedLineRetainsOnlyBoundedMarkedExcerpt() {
        String line = longMalformedLine();
        String message = rejects(manifestWithMalformedLine(line));
        assertEquals(ERROR_PREFIX + line.substring(0, MAX_EXCERPT) + OMITTED, message);
        assertEquals(ERROR_PREFIX.length() + MAX_EXCERPT + OMITTED.length(), message.length());
        assertFalse("untrusted rest of line must not enter the report", message.contains("xxxxxxxxxx" + OMITTED + "x"));
    }

    @Test
    public void hugeMalformedManifestInRealZipIsRejectedWithoutHistoryChanges() throws Exception {
        File root = temporaryFolder.newFolder("history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File existing = new File(runs, "existing.apdlog");
        byte[] original = "preexisting player history".getBytes(ArchiveManifest.utf8());
        Files.write(existing.toPath(), original);

        byte[] zip = ArchiveSupport.zip(
                ArchiveSupport.names(ArchiveManifest.ENTRY_NAME),
                Collections.singletonList(manifestWithMalformedLine(longMalformedLine())));
        ArchiveImporter.Report report =
                ArchiveImporter.importArchive(root, new ByteArrayInputStream(zip));

        assertFalse(report.summary(), report.ok);
        assertEquals(ERROR_PREFIX.length() + MAX_EXCERPT + OMITTED.length(), report.problem.length());
        assertTrue(report.problem, report.problem.endsWith(OMITTED));
        assertEquals(0, report.imported);
        assertArrayEquals(original, Files.readAllBytes(existing.toPath()));
        assertFalse(new File(root, "import-tmp").exists());
    }
}
