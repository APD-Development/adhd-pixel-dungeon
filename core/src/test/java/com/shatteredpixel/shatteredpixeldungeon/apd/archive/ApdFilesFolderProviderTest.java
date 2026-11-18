package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

// Desktop/fallback imports promise the newest ZIP, not the alphabetically last ZIP.
public class ApdFilesFolderProviderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private byte[] chosenBytes(File base) {
        final byte[][] selected = {null};
        final String[] failure = {null};
        new ApdFiles.FolderProvider(base).chooseImport(new ApdFiles.ImportSource() {
            @Override
            public void onStream(InputStream in, String displayName) {
                try (InputStream owned = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[256];
                    int n;
                    while ((n = owned.read(buffer)) != -1) out.write(buffer, 0, n);
                    selected[0] = out.toByteArray();
                } catch (IOException e) {
                    failure[0] = e.toString();
                }
            }
            @Override
            public void onCancelled() {
                failure[0] = "unexpected cancellation";
            }
            @Override
            public void onFailed(String why) {
                failure[0] = why;
            }
        });
        assertNull("fallback import must not fail", failure[0]);
        assertNotNull("fallback import must supply a ZIP", selected[0]);
        return selected[0];
    }

    @Test
    public void newerZipWinsEvenWhenItsNameSortsFirst() throws Exception {
        File base = temporaryFolder.newFolder("newer");
        File imports = new File(base, "imports");
        assertTrue(imports.mkdir());
        File older = new File(imports, "z-older.zip");
        File newer = new File(imports, "a-newer.zip");
        File nonZip = new File(imports, "zz-newest.txt");
        Files.write(older.toPath(), new byte[]{1});
        Files.write(newer.toPath(), new byte[]{2});
        Files.write(nonZip.toPath(), new byte[]{3});
        assertTrue(older.setLastModified(1700000000000L));
        assertTrue(newer.setLastModified(1700000100000L));
        assertTrue(nonZip.setLastModified(1700000200000L));

        assertArrayEquals("the most recently modified ZIP must be selected", new byte[]{2}, chosenBytes(base));
    }

    @Test
    public void equalModificationTimesPreserveLexicographicFallback() throws Exception {
        File base = temporaryFolder.newFolder("same-time");
        File imports = new File(base, "imports");
        assertTrue(imports.mkdir());
        File first = new File(imports, "a.zip");
        File last = new File(imports, "z.zip");
        Files.write(first.toPath(), new byte[]{4});
        Files.write(last.toPath(), new byte[]{5});
        assertTrue(first.setLastModified(1700000000000L));
        assertTrue(last.setLastModified(1700000000000L));

        assertArrayEquals("a timestamp tie retains the previous last-name choice", new byte[]{5}, chosenBytes(base));
    }
}
