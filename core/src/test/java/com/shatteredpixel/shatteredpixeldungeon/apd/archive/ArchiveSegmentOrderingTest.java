package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveSupport.*;

public class ArchiveSegmentOrderingTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private void check(boolean newestFinished) throws Exception {
        // Exercise both file traversal orders: segment number, not filename, breaks a timestamp tie.
        for (boolean reverseNames : new boolean[]{false, true}) {
            File root = tmp.newFolder();
            String older = reverseNames ? "z-old" : "a-old";
            String newer = reverseNames ? "a-new" : "z-new";
            TestLogs first = new TestLogs(root, older, V, 1000)
                    .seeds(777, SECRET_SEED).start("same-run", 0, "MAGE", RS, 0, 0, false).enter(1);
            first.registered("first", SECRET_CLASS, 1, false, 0);
            if (!newestFinished) first.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1);
            first.close(!newestFinished);
            TestLogs second = new TestLogs(root, newer, V, 1000)
                    .seeds(777, SECRET_SEED).start("same-run", 1, "MAGE", RS, 0, 0, false).enter(1);
            second.registered("second", SECRET_CLASS, 1, false, 0);
            if (newestFinished) second.end(LifecycleEvents.OUTCOME_WON, "", 26, 20, 100);
            second.close(newestFinished);
            Map<String, byte[]> before = files(root);
            Map<String, byte[]> entries = new TreeMap<>();
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(export(root)))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = zip.read(buffer)) != -1) bytes.write(buffer, 0, count);
                    entries.put(entry.getName(), bytes.toByteArray());
                }
            }
            ArchiveManifest manifest = ArchiveManifest.parse(entries.get(ArchiveManifest.ENTRY_NAME));
            for (String id : new String[]{older, newer}) {
                String path = "runs/" + id + ".apdlog";
                byte[] archived = entries.get(path);
                assertNotNull(archived);
                assertEquals("redaction follows the newest segment", !newestFinished, manifest.find(path).redacted);
                assertEquals(!newestFinished, LogRedactor.isRedacted(RunLogReader.parse(archived).header.writerBuild));
                if (newestFinished) {
                    assertArrayEquals("finished segments remain lossless", before.get(path), archived);
                    assertTrue(contains(archived, SECRET_CLASS));
                } else {
                    assertFalse("unfinished secrets must not leave", contains(archived, SECRET_CLASS));
                    assertFalse("unfinished seed must not leave", contains(archived, SECRET_SEED));
                }
            }
            Map<String, byte[]> after = files(root);
            assertEquals(before.keySet(), after.keySet());
            for (String path : before.keySet()) assertArrayEquals("source history is never rewritten", before.get(path), after.get(path));
        }
    }

    @Test public void resumedRunWithSameTimestampKeepsEverySegmentRedacted() throws Exception { check(false); }
    @Test public void finishedRunWithSameTimestampKeepsEverySegmentLossless() throws Exception { check(true); }
}

