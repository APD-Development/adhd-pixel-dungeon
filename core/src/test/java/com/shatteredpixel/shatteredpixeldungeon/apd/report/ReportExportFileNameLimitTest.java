package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class ReportExportFileNameLimitTest {

    @Test
    public void everyFormatHasABoundedFilenameEvenForVeryLongTitles() {
        Report report = new Report("Very long " + "L".repeat(1000), 0, "test");
        for (ReportExport.Format format : ReportExport.Format.values()) {
            String name = ReportExport.fileName(report, format);
            assertTrue(name, name.startsWith("apd-very-long-"));
            assertTrue(name, name.endsWith("-19700101-0000." + format.extension));
            assertTrue("portable filename byte length " + name.length(),
                    name.getBytes(StandardCharsets.UTF_8).length <= 240);
            assertEquals("the ASCII slug must be capped without changing the suffix",
                    180, name.substring(4, name.indexOf("-19700101-0000.")).length());
            assertFalse(name, name.contains("/"));
            assertFalse(name, name.contains("\\\\"));
        }
    }

    @Test
    public void longTitleCanActuallyOpenThroughTheExistingFolderExportProvider() throws Exception {
        File root = Files.createTempDirectory("apd-report-long-name-").toFile();
        String name = ReportExport.fileName(new Report("X".repeat(600), 0, "test"),
                ReportExport.Format.HTML);
        File exportDir = new File(root, "exports");
        File exported = new File(exportDir, name);
        final int[] opened = {0};
        try {
            new ApdFiles.FolderProvider(root).chooseExport(name, new ApdFiles.ExportTarget() {
                @Override
                public void onStream(OutputStream out, String displayName) {
                    try (OutputStream stream = out) {
                        stream.write(new byte[] {65, 80, 68});
                        opened[0]++;
                    } catch (IOException e) {
                        throw new AssertionError("the bounded filename must be writable", e);
                    }
                }

                @Override
                public void onCancelled() {
                    fail("folder export must not cancel");
                }

                @Override
                public void onFailed(String why) {
                    fail("folder export must accept bounded filename: " + why);
                }
            });
            assertEquals(1, opened[0]);
            assertArrayEquals(new byte[] {65, 80, 68}, Files.readAllBytes(exported.toPath()));
        } finally {
            Files.deleteIfExists(exported.toPath());
            Files.deleteIfExists(exportDir.toPath());
            Files.deleteIfExists(root.toPath());
        }
    }
}
