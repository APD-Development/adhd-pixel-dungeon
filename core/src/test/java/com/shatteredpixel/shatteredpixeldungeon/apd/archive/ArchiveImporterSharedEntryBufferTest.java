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
import java.util.ArrayList;
import java.util.List;

// The importer must reuse scratch memory across normal and ignored ZIP members
// without contaminating history bytes or weakening all-or-nothing import.
public class ArchiveImporterSharedEntryBufferTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final class Fixture {
        final List<byte[]> originalLogs = new ArrayList<>();
        final List<String> paths = new ArrayList<>();
        byte[] zip;
    }

    private static Fixture interleavedArchive(File scratch, boolean corruptLast) throws Exception {
        Fixture fixture = new Fixture();
        ArchiveManifest manifest = new ArchiveManifest();
        manifest.container = 1;
        List<String> names = new ArrayList<>();
        List<byte[]> contents = new ArrayList<>();
        names.add(ArchiveManifest.ENTRY_NAME);
        contents.add(null);

        for (int i = 0; i < 3; i++) {
            String id = "seg" + i;
            byte[] original = ArchiveSupport.smallLog(scratch, id, "run-" + id);
            String path = "runs/" + id + ".apdlog";
            fixture.originalLogs.add(original);
            fixture.paths.add(path);

            ArchiveManifest.Entry entry = new ArchiveManifest.Entry();
            entry.path = path;
            entry.kind = ArchiveManifest.KIND_RUN;
            entry.runId = "run-" + id;
            entry.size = original.length;
            entry.sha256 = ArchiveManifest.sha256Hex(original);
            manifest.files.add(entry);

            // Each ignored member is larger than the 16 KiB shared read buffer.
            byte[] ignored = new byte[48_321];
            for (int j = 0; j < ignored.length; j++) ignored[j] = (byte) (i + j);
            names.add("ignored/payload-" + i);
            contents.add(ignored);

            names.add(path);
            byte[] written = original.clone();
            if (corruptLast && i == 2) written[written.length - 1] ^= 0x7f;
            contents.add(written);
        }
        contents.set(0, manifest.encode().getBytes(ArchiveManifest.utf8()));
        fixture.zip = ArchiveSupport.zip(names, contents);
        return fixture;
    }

    @Test
    public void mixedIgnoredAndValidMembersPreserveEveryLogByte() throws Exception {
        Fixture fixture = interleavedArchive(temporaryFolder.newFolder("scratch-valid"), false);
        File history = temporaryFolder.newFolder("history-valid");
        ArchiveImporter.Report report = ArchiveImporter.importArchive(history,
                new ByteArrayInputStream(fixture.zip));

        assertTrue(report.summary(), report.ok);
        assertEquals(3, report.imported);
        assertEquals(0, report.rejected);
        assertEquals(3, report.details.size());
        assertEquals("not in the manifest or repeated, ignored: ignored/payload-2",
                report.details.get(2));
        for (int i = 0; i < fixture.paths.size(); i++) {
            assertArrayEquals(fixture.originalLogs.get(i),
                    Files.readAllBytes(new File(history, fixture.paths.get(i)).toPath()));
        }
        assertFalse(new File(history, "import-tmp").exists());
    }

    @Test
    public void corruptFinalMemberStillRollsBackAllPriorStagedLogs() throws Exception {
        Fixture fixture = interleavedArchive(temporaryFolder.newFolder("scratch-corrupt"), true);
        File history = temporaryFolder.newFolder("history-corrupt");
        File runs = new File(history, "runs");
        assertTrue(runs.mkdir());
        File existing = new File(runs, "unchanged.apdlog");
        byte[] original = new byte[]{6, 7, 8, 9};
        Files.write(existing.toPath(), original);

        ArchiveImporter.Report report = ArchiveImporter.importArchive(history,
                new ByteArrayInputStream(fixture.zip));

        assertFalse(report.ok);
        assertEquals(0, report.imported);
        assertEquals(1, report.rejected);
        assertTrue(report.summary(), report.problem.contains("1 problem"));
        assertArrayEquals(original, Files.readAllBytes(existing.toPath()));
        for (String path : fixture.paths) {
            assertFalse(new File(history, path).exists());
        }
        assertFalse(new File(history, "import-tmp").exists());
    }
}
