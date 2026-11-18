package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** The Android file picker owns the destination, but a failing provider must still get one close and one result. */
public class ReportSaveProviderFailureTest {

    private static final class ProviderStream extends OutputStream {
        final boolean rejectWrite;
        final boolean rejectClose;
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final AtomicInteger closes = new AtomicInteger();

        ProviderStream(boolean rejectWrite, boolean rejectClose) {
            this.rejectWrite = rejectWrite;
            this.rejectClose = rejectClose;
        }

        @Override
        public void write(int value) {
            if (rejectWrite) throw new IllegalStateException("provider write rejected");
            bytes.write(value);
        }

        @Override
        public void close() {
            closes.incrementAndGet();
            if (rejectClose) throw new IllegalStateException("provider close rejected");
        }
    }

    private static void verify(boolean rejectWrite, boolean rejectClose, String expected) throws Exception {
        final ProviderStream stream = new ProviderStream(rejectWrite, rejectClose);
        final AtomicInteger callbacks = new AtomicInteger();
        final AtomicReference<String> result = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        final Report report = new Report("provider-failure", 0, "test")
                .add(new Report.Note(Report.NoteKind.INFO, "save text"));
        ApdFiles.Provider previous = ApdFiles.provider;
        try {
            ApdFiles.provider = new ApdFiles.Provider() {
                @Override
                public void chooseExport(String suggestedName, ApdFiles.ExportTarget target) {
                    target.onStream(stream, "chosen report");
                }

                @Override
                public void chooseImport(ApdFiles.ImportSource source) {
                    fail("Saving must not open an importer");
                }

                @Override
                public String where() {
                    return "test";
                }
            };
            ReportExport.save(new File("."), report, ReportExport.Format.TEXT, message -> {
                result.set(message);
                callbacks.incrementAndGet();
                done.countDown();
            });
            assertTrue("A provider failure must notify the player rather than abandon the worker",
                    done.await(12, TimeUnit.SECONDS));
            assertEquals("The player must receive exactly one result", 1, callbacks.get());
            assertEquals(expected, result.get());
            assertEquals("The provider stream must close even when a write fails", 1, stream.closes.get());
            if (!rejectWrite) {
                assertArrayEquals(ReportExport.render(report, ReportExport.Format.TEXT)
                        .getBytes(StandardCharsets.UTF_8), stream.bytes.toByteArray());
            }
        } finally {
            ApdFiles.provider = previous;
        }
    }

    @Test
    public void uncheckedWriteFailureStillClosesAndReportsFailure() throws Exception {
        verify(true, false,
                "The report could not be saved: java.lang.IllegalStateException: provider write rejected");
    }

    @Test
    public void uncheckedCloseFailureOverridesPrematureSuccessMessage() throws Exception {
        verify(false, true,
                "The report could not be saved: java.lang.IllegalStateException: provider close rejected");
    }

    @Test
    public void normalProviderStillSavesAndClosesExactlyOnce() throws Exception {
        verify(false, false, "Saved Plain text report to chosen report ("
                + ReportExport.render(new Report("provider-failure", 0, "test")
                        .add(new Report.Note(Report.NoteKind.INFO, "save text")), ReportExport.Format.TEXT)
                        .getBytes(StandardCharsets.UTF_8).length + " bytes).");
    }
}
