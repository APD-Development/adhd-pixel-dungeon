package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

// A syntactically valid, checksummed manifest may still name an invalid path.
// Refusing it must never copy a gigantic path into the visible error report,
// and must never modify the player's existing history.
public class ArchiveInvalidPathDiagnosticTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String PREFIX = "the archive lists a file it is not allowed to write: ";
    private static final String OMITTED = "... (truncated)";
    private static final int MAX_EXCERPT = 160;

    private static byte[] manifestWithPath(String path) {
        ArchiveManifest manifest = new ArchiveManifest();
        ArchiveManifest.Entry file = new ArchiveManifest.Entry();
        file.path = path;
        file.kind = ArchiveManifest.KIND_RUN;
        file.runId = "run-1";
        file.size = 0;
        file.sha256 = ArchiveManifest.sha256Hex(new byte[0]);
        manifest.files.add(file);
        return manifest.encode().getBytes(ArchiveManifest.utf8());
    }

    private static byte[] zipWithPath(String path) throws Exception {
        return ArchiveSupport.zip(
                ArchiveSupport.names(ArchiveManifest.ENTRY_NAME),
                Collections.singletonList(manifestWithPath(path)));
    }

    private static String veryLongInvalidPath() {
        char[] chars = new char[250_000];
        Arrays.fill(chars, 'x');
        return "outside/" + new String(chars) + ".apdlog";
    }

    @Test
    public void shortInvalidPathKeepsExactExistingRefusal() throws Exception {
        String path = "outside/run-1.apdlog";
        File root = temporaryFolder.newFolder("short");
        assertFalse(ArchiveManifest.isValidPath(path));
        ArchiveImporter.Report report = ArchiveImporter.importArchive(
                root, new ByteArrayInputStream(zipWithPath(path)));
        assertFalse(report.summary(), report.ok);
        assertEquals(PREFIX + path, report.problem);
        assertEquals(0, report.imported);
        assertFalse(new File(root, "import-tmp").exists());
    }

    @Test
    public void checksummedLongInvalidPathHasBoundedMarkedRefusal() throws Exception {
        String path = veryLongInvalidPath();
        byte[] manifest = manifestWithPath(path);
        // The archive is structurally valid. Path authorization, not the
        // checksum or malformed-line parser, must refuse this input.
        assertEquals(path, ArchiveManifest.parse(manifest).files.get(0).path);
        assertFalse(ArchiveManifest.isValidPath(path));

        File root = temporaryFolder.newFolder("long");
        ArchiveImporter.Report report = ArchiveImporter.importArchive(
                root, new ByteArrayInputStream(zipWithPath(path)));
        assertFalse(report.summary(), report.ok);
        assertEquals(PREFIX + path.substring(0, MAX_EXCERPT) + OMITTED, report.problem);
        assertEquals(PREFIX.length() + MAX_EXCERPT + OMITTED.length(), report.problem.length());
        assertEquals(0, report.imported);
        assertFalse(new File(root, "import-tmp").exists());
    }

    @Test
    public void rejectedLongPathZipPreservesExistingHistoryBytes() throws Exception {
        File root = temporaryFolder.newFolder("existing");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File saved = new File(runs, "run-1.apdlog");
        byte[] original = "original player history, never overwrite".getBytes(ArchiveManifest.utf8());
        Files.write(saved.toPath(), original);

        ArchiveImporter.Report report = ArchiveImporter.importArchive(
                root, new ByteArrayInputStream(zipWithPath(veryLongInvalidPath())));
        assertFalse(report.summary(), report.ok);
        assertTrue(report.problem.endsWith(OMITTED));
        assertTrue(report.problem.length() <= PREFIX.length() + MAX_EXCERPT + OMITTED.length());
        assertEquals(0, report.imported);
        assertArrayEquals(original, Files.readAllBytes(saved.toPath()));
        assertFalse(new File(root, "import-tmp").exists());
    }
}
