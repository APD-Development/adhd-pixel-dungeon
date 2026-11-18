package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
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

import static com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveSupport.*;
import static org.junit.Assert.*;

// An unfinished or ambiguous log cannot turn hidden run facts into a raw ZIP entry
// merely by containing an earlier, syntactically valid RUN_END event.
public class ArchiveTerminalEndPrivacyTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static final class Archived {
        final ArchiveManifest.Entry manifest;
        final byte[] bytes;

        Archived(ArchiveManifest.Entry manifest, byte[] bytes) {
            this.manifest = manifest;
            this.bytes = bytes;
        }
    }

    private Archived archive(File root, String logId) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ArchiveExporter.Report report = ArchiveExporter.export(root, stream, "test", 12345);
        assertTrue(report.summary(), report.ok);
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(stream.toByteArray()))) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = zip.getNextEntry()) != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int n;
                while ((n = zip.read(buffer)) != -1) out.write(buffer, 0, n);
                entries.put(entry.getName(), out.toByteArray());
            }
        }
        String path = "runs/" + logId + ".apdlog";
        ArchiveManifest manifest = ArchiveManifest.parse(entries.get(ArchiveManifest.ENTRY_NAME));
        assertNotNull(manifest.find(path));
        assertNotNull(entries.get(path));
        return new Archived(manifest.find(path), entries.get(path));
    }

    private TestLogs log(File root, String id) {
        TestLogs log = new TestLogs(root, id, V, 1000)
                .seeds(654321, SECRET_SEED)
                .start(id, 0, "MAGE", RS, 0, 0, false)
                .enter(1);
        log.registered("hidden-item", SECRET_CLASS, 3, true, 0);
        return log;
    }

    private void assertProtected(File root, TestLogs log, String id) throws Exception {
        byte[] source = Files.readAllBytes(log.file.toPath());
        assertTrue("fixture contains secret in original log", contains(source, SECRET_CLASS));
        Archived archived = archive(root, id);
        assertTrue("ambiguous completion must redact history", archived.manifest.redacted);
        assertTrue(LogRedactor.isRedacted(RunLogReader.parse(archived.bytes).header.writerBuild));
        assertFalse("the hidden class cannot leave the app", contains(archived.bytes, SECRET_CLASS));
        assertFalse("custom seed cannot leave the app", contains(archived.bytes, SECRET_SEED));
        assertArrayEquals("export must not rewrite original history", source, Files.readAllBytes(log.file.toPath()));
    }

    @Test public void unsealedLogWithReadableRunEndStillRedactsHiddenFacts() throws Exception {
        File root = tmp.newFolder();
        TestLogs log = log(root, "unsealed-end");
        log.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 10).close(false);
        assertProtected(root, log, "unsealed-end");
    }

    @Test public void sealedLogWithEventsAfterRunEndStillRedactsHiddenFacts() throws Exception {
        File root = tmp.newFolder();
        TestLogs log = log(root, "trailing-hidden");
        log.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 10);
        log.registered("later-secret", SECRET_CLASS, 3, false, 0).close(true);
        assertProtected(root, log, "trailing-hidden");
    }

    @Test public void sealedLogWithPostSealGarbageCannotExportHiddenHistoryRaw() throws Exception {
        File root = tmp.newFolder();
        TestLogs log = log(root, "postseal-garbage");
        log.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 10).close(true);
        Files.write(log.file.toPath(), new byte[]{0, 1, 2, 3},
                java.nio.file.StandardOpenOption.APPEND);
        com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult read = RunLogReader.read(log.file);
        assertEquals(com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadStatus.SEALED, read.status);
        assertEquals(4, read.damagedBytes());
        assertProtected(root, log, "postseal-garbage");
    }

    @Test public void validFrameAfterSealCannotExportHiddenHistoryRaw() throws Exception {
        File root = tmp.newFolder();
        TestLogs log = log(root, "postseal-frame");
        log.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 10).close(true);
        byte[] extraFrame = com.shatteredpixel.shatteredpixeldungeon.apd.stats.LogFormat.clockFrame(42);
        Files.write(log.file.toPath(), extraFrame,
                java.nio.file.StandardOpenOption.APPEND);
        com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult read = RunLogReader.read(log.file);
        assertEquals(com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadStatus.SEALED, read.status);
        assertEquals(extraFrame.length, read.damagedBytes());
        assertProtected(root, log, "postseal-frame");
    }

    @Test public void completedRunWithNormalRankingLinkTrailerStaysLossless() throws Exception {
        File root = tmp.newFolder();
        TestLogs log = log(root, "finished-ranked");
        log.end(LifecycleEvents.OUTCOME_WON, "", 26, 20, 500);
        log.rankingLink("matching-ranking-id").close(true);
        byte[] original = Files.readAllBytes(log.file.toPath());
        Archived archived = archive(root, "finished-ranked");
        assertFalse("normal RunRecorder ranking trailer is a valid completed log", archived.manifest.redacted);
        assertArrayEquals("normal completed ranked history stays exact", original, archived.bytes);
        assertArrayEquals(original, Files.readAllBytes(log.file.toPath()));
    }

    @Test public void cleanSealedTerminalRunEndStillExportsOriginalBytes() throws Exception {
        File root = tmp.newFolder();
        TestLogs log = log(root, "ended-cleanly");
        log.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 10).close(true);
        byte[] original = Files.readAllBytes(log.file.toPath());
        Archived archived = archive(root, "ended-cleanly");
        assertFalse(archived.manifest.redacted);
        assertArrayEquals("valid completed history remains lossless", original, archived.bytes);
        assertTrue(contains(archived.bytes, SECRET_CLASS));
        assertArrayEquals(original, Files.readAllBytes(log.file.toPath()));
    }
}
