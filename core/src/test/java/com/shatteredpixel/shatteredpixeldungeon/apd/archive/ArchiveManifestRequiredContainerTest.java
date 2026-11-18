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

// Format 1 requires a container record, even when a checksummed manifest has
// no logs. Optional provenance scalars and unknown extension records remain optional.
public class ArchiveManifestRequiredContainerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static ArchiveManifest sample(byte[] log) {
        ArchiveManifest manifest = new ArchiveManifest();
        manifest.createdMillis = 1234;
        manifest.app = "APD";
        manifest.telemetry = "1.5";
        manifest.container = 1;
        ArchiveManifest.Entry entry = new ArchiveManifest.Entry();
        entry.path = "runs/a.apdlog";
        entry.kind = ArchiveManifest.KIND_RUN;
        entry.runId = "run-a";
        entry.size = log.length;
        entry.sha256 = ArchiveManifest.sha256Hex(log);
        entry.events = 1;
        manifest.files.add(entry);
        return manifest;
    }

    private static String body(ArchiveManifest manifest) {
        String encoded = manifest.encode();
        return encoded.substring(0, encoded.lastIndexOf("check\t"));
    }

    private static byte[] signed(String body) {
        return (body + "check\t" +
                ArchiveManifest.sha256Hex(body.getBytes(ArchiveManifest.utf8())) + "\n")
                .getBytes(ArchiveManifest.utf8());
    }

    private static byte[] withoutContainer(ArchiveManifest manifest) {
        String original = body(manifest);
        String removed = original.replace("container\t" + manifest.container + "\n", "");
        assertFalse("fixture must remove its actual container record", removed.equals(original));
        return signed(removed);
    }

    private static void rejectsMissingContainer(byte[] bytes) {
        try {
            ArchiveManifest.parse(bytes);
            fail("accepted a checksummed format-1 manifest without a container record");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(),
                    expected.getMessage().contains("no container metadata"));
        }
    }

    @Test
    public void missingContainerIsRejectedWithCorrectChecksumForEmptyAndNonemptyManifests() {
        ArchiveManifest empty = new ArchiveManifest();
        assertEquals(0, ArchiveManifest.parse(empty.encode().getBytes(ArchiveManifest.utf8())).container);
        rejectsMissingContainer(withoutContainer(empty));
        ArchiveManifest nonempty = sample(new byte[]{1, 2, 3});
        assertEquals(1, ArchiveManifest.parse(nonempty.encode().getBytes(ArchiveManifest.utf8())).container);
        rejectsMissingContainer(withoutContainer(nonempty));
    }

    @Test
    public void optionalScalarOmissionsAndUnknownChecksummedExtensionRemainSupported() {
        ArchiveManifest manifest = sample(new byte[]{3, 2, 1});
        String optional = body(manifest)
                .replace("created\t1234\n", "")
                .replace("app\tAPD\n", "")
                .replace("telemetry\t1.5\n", "");
        String extended = optional.replace("container\t1\n",
                "container\t1\nfuture-container-detail\tvalue\n");
        ArchiveManifest parsed = ArchiveManifest.parse(signed(extended));
        assertEquals(1, parsed.container);
        assertEquals(0, parsed.createdMillis);
        assertEquals("", parsed.app);
        assertEquals("", parsed.telemetry);
        assertEquals(1, parsed.files.size());
        assertEquals(manifest.files.get(0).sha256, parsed.files.get(0).sha256);
    }

    @Test
    public void missingContainerZipImportLeavesExistingHistoryUntouched() throws Exception {
        File root = temporaryFolder.newFolder("history");
        File runs = new File(root, "runs");
        assertTrue(runs.mkdir());
        File prior = new File(runs, "keep.apdlog");
        byte[] original = "existing player history".getBytes(ArchiveManifest.utf8());
        Files.write(prior.toPath(), original);

        byte[] incoming = ArchiveSupport.smallLog(temporaryFolder.newFolder("scratch"), "a", "run-a");
        byte[] archive = ArchiveSupport.zip(
                ArchiveSupport.names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"),
                Arrays.asList(withoutContainer(sample(incoming)), incoming));
        ArchiveImporter.Report result = ArchiveImporter.importArchive(root, new ByteArrayInputStream(archive));

        assertFalse(result.summary(), result.ok);
        assertTrue(result.problem, result.problem.contains("no container metadata"));
        assertEquals(0, result.imported);
        assertArrayEquals(original, Files.readAllBytes(prior.toPath()));
        assertFalse(new File(runs, "a.apdlog").exists());
        assertFalse(new File(root, "import-tmp").exists());
    }
}
