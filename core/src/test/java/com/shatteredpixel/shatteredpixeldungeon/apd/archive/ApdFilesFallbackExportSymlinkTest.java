package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class ApdFilesFallbackExportSymlinkTest {

    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final byte[] REPORT = "safe fallback export".getBytes(StandardCharsets.UTF_8);

    private static final class Result {
        boolean accepted;
        String failure;
    }

    private Result save(File base, String name) {
        Result result = new Result();
        new ApdFiles.FolderProvider(base).chooseExport(name, new ApdFiles.ExportTarget() {
            @Override public void onStream(OutputStream out, String displayName) {
                result.accepted = true;
                try (OutputStream owned = out) {
                    owned.write(REPORT);
                } catch (IOException error) {
                    result.failure = error.toString();
                }
            }
            @Override public void onCancelled() {
                result.failure = "unexpected cancellation";
            }
            @Override public void onFailed(String why) {
                result.failure = why;
            }
        });
        return result;
    }

    @Test public void existingExportFileSymlinkCannotTruncateOutsideData() throws Exception {
        File base = temporaryFolder.newFolder("output-alias");
        File dir = new File(base, "exports");
        assertTrue(dir.mkdir());
        File outside = temporaryFolder.newFile("private-original.txt");
        byte[] original = "private contents must survive".getBytes(StandardCharsets.UTF_8);
        Files.write(outside.toPath(), original);
        Files.createSymbolicLink(new File(dir, "report.txt").toPath(), outside.toPath());

        Result result = save(base, "report.txt");
        assertFalse(result.accepted);
        assertNotNull(result.failure);
        assertTrue(result.failure.contains("unsafe export destination"));
        assertArrayEquals(original, Files.readAllBytes(outside.toPath()));
    }

    @Test public void symlinkedExportsDirectoryCannotWriteOutsideStorageRoot() throws Exception {
        File base = temporaryFolder.newFolder("directory-alias");
        File outside = temporaryFolder.newFolder("external-exports");
        Files.createSymbolicLink(new File(base, "exports").toPath(), outside.toPath());

        Result result = save(base, "report.txt");
        assertFalse(result.accepted);
        assertNotNull(result.failure);
        assertTrue(result.failure.contains("unsafe export destination"));
        assertFalse(new File(outside, "report.txt").exists());
    }

    @Test public void traversalNameCannotEscapeExports() throws Exception {
        File base = temporaryFolder.newFolder("traversal");
        File outside = new File(base, "outside.txt");
        Files.write(outside.toPath(), "retain".getBytes(StandardCharsets.UTF_8));

        Result result = save(base, "../outside.txt");
        assertFalse(result.accepted);
        assertNotNull(result.failure);
        assertArrayEquals("retain".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(outside.toPath()));
    }

    @Test public void symlinkedBaseRootStillWritesValidDirectExport() throws Exception {
        File realBase = temporaryFolder.newFolder("real-storage");
        File alias = new File(temporaryFolder.getRoot(), "storage-alias");
        Files.createSymbolicLink(alias.toPath(), realBase.toPath());

        Result result = save(alias, "report.txt");
        assertTrue(result.failure, result.accepted);
        assertNull(result.failure);
        assertArrayEquals(REPORT, Files.readAllBytes(new File(realBase, "exports/report.txt").toPath()));
    }
}
