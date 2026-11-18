package com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

/** Preserve local session/submission records when malformed bytes or a file target is encountered. */
public class TabFileIntegrityTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String b64(String value) {
        return b64(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test public void malformedUtf8LineIsSkippedWithoutInventingReplacementCharacters() throws Exception {
        File target = new File(tmp.getRoot(), "ledger.txt");
        String goodFirst = b64("run-one") + "\t" + b64("caf\u00e9") + "\n";
        String damaged = b64("run-damaged") + "\t" + b64(new byte[]{(byte)0xc3, (byte)0x28}) + "\n";
        String goodLast = b64("run-two") + "\t" + b64("\u96ea") + "\n";
        byte[] original = (goodFirst + damaged + goodLast).getBytes(StandardCharsets.US_ASCII);
        Files.write(target.toPath(), original);

        List<String[]> rows = TabFile.read(target, 2);
        assertEquals("valid neighbors must survive", 2, rows.size());
        assertArrayEquals(new String[]{"run-one", "caf\u00e9"}, rows.get(0));
        assertArrayEquals(new String[]{"run-two", "\u96ea"}, rows.get(1));
        assertArrayEquals("reading cannot silently rewrite damaged source bytes", original, Files.readAllBytes(target.toPath()));
    }

    @Test public void replacementPreservesValidUnicodeAndLeavesNoTemporaryFile() throws Exception {
        File target = new File(tmp.getRoot(), "session.txt");
        TabFile.write(target, java.util.Collections.singletonList(new String[]{"first", "na\u00efve"}));
        assertArrayEquals(new String[]{"first", "na\u00efve"}, TabFile.read(target, 2).get(0));

        TabFile.write(target, java.util.Collections.singletonList(new String[]{"second", "\u96ea"}));
        List<String[]> back = TabFile.read(target, 2);
        assertEquals(1, back.size());
        assertArrayEquals(new String[]{"second", "\u96ea"}, back.get(0));
        assertFalse(new File(target.getPath() + ".tmp").exists());
    }

    @Test public void directoryDestinationIsNotDeletedOrReplaced() throws Exception {
        File destination = tmp.newFolder("session.txt");
        try {
            TabFile.write(destination, java.util.Collections.singletonList(new String[]{"session", "token"}));
            fail("A directory is not a record file");
        } catch (IOException expected) {
            assertTrue(destination.isDirectory());
            assertFalse(new File(destination.getPath() + ".tmp").exists());
        }
    }

    @Test public void failedTemporaryWriteRetainsOriginalRecordBytes() throws Exception {
        File target = new File(tmp.getRoot(), "submitted_runs.txt");
        TabFile.write(target, java.util.Collections.singletonList(new String[]{"run-original", "digest-original"}));
        byte[] original = Files.readAllBytes(target.toPath());
        File unavailableTemp = new File(target.getPath() + ".tmp");
        assertTrue(unavailableTemp.mkdir());
        try {
            TabFile.write(target, java.util.Collections.singletonList(new String[]{"run-replacement", "digest-replacement"}));
            fail("The temporary path is a directory");
        } catch (IOException expected) {
            assertArrayEquals(original, Files.readAllBytes(target.toPath()));
            assertTrue(unavailableTemp.isDirectory());
        }
    }
}
