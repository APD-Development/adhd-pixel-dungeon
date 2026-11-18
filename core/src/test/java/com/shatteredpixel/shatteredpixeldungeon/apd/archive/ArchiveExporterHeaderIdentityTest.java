package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// Run-log headers identify each file; typed RUN_START can independently name the
// shared logical run spanning several distinct log files.
public class ArchiveExporterHeaderIdentityTest {

    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static Map<String, byte[]> entries(byte[] bytes) throws Exception {
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = zip.getNextEntry()) != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int n;
                while ((n = zip.read(buffer)) != -1) out.write(buffer, 0, n);
                files.put(entry.getName(), out.toByteArray());
            }
        }
        return files;
    }

    @Test public void copiedLogWithWrongFilenameIsExcludedAndValidNeighborsRoundTrip() throws Exception {
        File source = tmp.newFolder("source");
        byte[] original = ArchiveSupport.smallLog(source, "actual-header", "shared-logical-run");
        byte[] other = ArchiveSupport.smallLog(source, "independent", "other-logical-run");
        File alias = new File(source, "runs/copied-alias.apdlog");
        Files.write(alias.toPath(), original);
        byte[] aliasBefore = Files.readAllBytes(alias.toPath());

        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        ArchiveExporter.Report export = ArchiveExporter.export(source, archive, "test", 5);
        assertTrue(export.summary(), export.ok);
        assertEquals("only real log identities exported", 2, export.runLogs);
        assertEquals("copied header/filename mismatch is unreadable", 1, export.skippedUnreadable);

        Map<String, byte[]> zipped = entries(archive.toByteArray());
        ArchiveManifest manifest = ArchiveManifest.parse(zipped.get(ArchiveManifest.ENTRY_NAME));
        assertEquals(2, manifest.files.size());
        assertNull(manifest.find("runs/copied-alias.apdlog"));
        assertFalse(zipped.containsKey("runs/copied-alias.apdlog"));
        assertArrayEquals(original, zipped.get("runs/actual-header.apdlog"));
        assertArrayEquals(other, zipped.get("runs/independent.apdlog"));

        File destination = tmp.newFolder("destination");
        ArchiveImporter.Report imported = ArchiveImporter.importArchive(destination,
                new ByteArrayInputStream(archive.toByteArray()));
        assertTrue(imported.summary(), imported.ok);
        assertEquals(2, imported.imported);
        assertArrayEquals(original, Files.readAllBytes(new File(destination, "runs/actual-header.apdlog").toPath()));
        assertArrayEquals(other, Files.readAllBytes(new File(destination, "runs/independent.apdlog").toPath()));
        assertArrayEquals("original copied history must not be rewritten", aliasBefore, Files.readAllBytes(alias.toPath()));
        assertArrayEquals(original, Files.readAllBytes(new File(source, "runs/actual-header.apdlog").toPath()));
    }

    @Test public void onlyCopiedLogIsSafelySkippedWithoutLeakingItsBytes() throws Exception {
        File scratch = tmp.newFolder("scratch");
        byte[] original = ArchiveSupport.smallLog(scratch, "real-id", "logical-id");
        File source = tmp.newFolder("copies-only");
        File runs = new File(source, "runs");
        assertTrue(runs.mkdir());
        File alias = new File(runs, "wrong-id.apdlog");
        Files.write(alias.toPath(), original);

        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        ArchiveExporter.Report export = ArchiveExporter.export(source, archive, "test", 6);
        assertTrue(export.summary(), export.ok);
        assertEquals(0, export.runLogs);
        assertEquals(1, export.skippedUnreadable);
        Map<String, byte[]> zipped = entries(archive.toByteArray());
        ArchiveManifest manifest = ArchiveManifest.parse(zipped.get(ArchiveManifest.ENTRY_NAME));
        assertTrue(manifest.files.isEmpty());
        assertFalse(zipped.containsKey("runs/wrong-id.apdlog"));
        assertArrayEquals(original, Files.readAllBytes(alias.toPath()));
    }

    @Test public void distinctSegmentFileIdsAndSharedLogicalRunRemainImportable() throws Exception {
        File source = tmp.newFolder("segments");
        TestLogs first = new TestLogs(source, "segment-first", ArchiveSupport.V, 1000)
                .start("one-logical-run", 0, "ROGUE", ArchiveSupport.RS, 0, 30, false)
                .enter(1);
        first.close(false);
        TestLogs last = new TestLogs(source, "segment-second", ArchiveSupport.V, 2000)
                .start("one-logical-run", 1, "ROGUE", ArchiveSupport.RS, 0, 30, false)
                .enter(2)
                .end(LifecycleEvents.OUTCOME_DIED, "Rat", 2, 2, 100);
        last.close(true);
        byte[] a = Files.readAllBytes(first.file.toPath());
        byte[] b = Files.readAllBytes(last.file.toPath());

        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        ArchiveExporter.Report export = ArchiveExporter.export(source, archive, "test", 7);
        assertTrue(export.summary(), export.ok);
        assertEquals(2, export.runLogs);
        assertEquals(0, export.skippedUnreadable);
        Map<String, byte[]> zipped = entries(archive.toByteArray());
        ArchiveManifest manifest = ArchiveManifest.parse(zipped.get(ArchiveManifest.ENTRY_NAME));
        assertEquals("one-logical-run", manifest.find("runs/segment-first.apdlog").runId);
        assertEquals("one-logical-run", manifest.find("runs/segment-second.apdlog").runId);

        File destination = tmp.newFolder("segment-destination");
        ArchiveImporter.Report imported = ArchiveImporter.importArchive(destination,
                new ByteArrayInputStream(archive.toByteArray()));
        assertTrue(imported.summary(), imported.ok);
        assertEquals(2, imported.imported);
        assertArrayEquals(a, Files.readAllBytes(new File(destination, "runs/segment-first.apdlog").toPath()));
        assertArrayEquals(b, Files.readAllBytes(new File(destination, "runs/segment-second.apdlog").toPath()));
    }

    @Test public void existingLegacyImportsRemainUnchanged() throws Exception {
        File source = tmp.newFolder("legacy-history");
        ArchiveSupport.history(source);
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        ArchiveExporter.Report export = ArchiveExporter.export(source, archive, "test", 8);
        assertTrue(export.summary(), export.ok);
        assertEquals(5, export.runLogs);
        assertEquals(1, export.importedFiles);
        assertEquals(0, export.skippedUnreadable);
        Map<String, byte[]> zipped = entries(archive.toByteArray());
        assertNotNull(zipped.get("legacy/import-1.apdlog"));
    }
}
