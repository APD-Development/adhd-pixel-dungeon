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
import java.util.Map;
import java.util.TreeMap;

// A different-size local log never needs to be hashed to keep it untouched.
// Matching sizes still require the complete SHA-256 check.
public class ArchiveImporterExistingSizeFastPathTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void mixedSizeAndHashComparisonsKeepEveryOriginalHistoryFile() throws Exception {
        File archivedScratch = temporaryFolder.newFolder("archive-source");
        File root = temporaryFolder.newFolder("history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());

        Map<String, byte[]> incoming = new TreeMap<>();
        Map<String, String> ids = new TreeMap<>();
        for (String id : new String[]{"larger-local", "same-size-different", "exact-existing", "new-log"}) {
            incoming.put("runs/" + id + ".apdlog",
                    ArchiveSupport.smallLog(archivedScratch, id, "run-" + id));
            ids.put("runs/" + id + ".apdlog", "run-" + id);
        }

        byte[] larger = new byte[3 * 1024 * 1024];
        Arrays.fill(larger, (byte) 0x4d);
        Files.write(new File(runs, "larger-local.apdlog").toPath(), larger);

        byte[] sameSizeDifferent = incoming.get("runs/same-size-different.apdlog").clone();
        sameSizeDifferent[sameSizeDifferent.length - 1] ^= 0x01;
        Files.write(new File(runs, "same-size-different.apdlog").toPath(), sameSizeDifferent);

        byte[] identical = incoming.get("runs/exact-existing.apdlog").clone();
        Files.write(new File(runs, "exact-existing.apdlog").toPath(), identical);

        ArchiveImporter.Report report = ArchiveImporter.importArchive(root,
                new ByteArrayInputStream(ArchiveSupport.archiveOf(incoming, ids)));

        assertTrue(report.summary(), report.ok);
        assertEquals(1, report.imported);
        assertEquals(1, report.skippedAlreadyHere);
        assertEquals(2, report.keptDifferentExisting);
        assertEquals(0, report.rejected);
        assertEquals(2, report.details.size());
        assertArrayEquals(larger, Files.readAllBytes(new File(runs, "larger-local.apdlog").toPath()));
        assertArrayEquals(sameSizeDifferent, Files.readAllBytes(new File(runs, "same-size-different.apdlog").toPath()));
        assertArrayEquals(identical, Files.readAllBytes(new File(runs, "exact-existing.apdlog").toPath()));
        assertArrayEquals(incoming.get("runs/new-log.apdlog"),
                Files.readAllBytes(new File(runs, "new-log.apdlog").toPath()));
        assertFalse(new File(root, "import-tmp").exists());
    }
}
