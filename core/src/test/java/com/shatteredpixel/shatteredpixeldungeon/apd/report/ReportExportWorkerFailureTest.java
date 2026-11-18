package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReportExportWorkerFailureTest {

    private static final class TrackedStream extends ByteArrayOutputStream {
        boolean closed;
        @Override public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private abstract static class Provider implements ApdFiles.Provider {
        @Override public void chooseExport(String name, ApdFiles.ExportTarget target) {
            chooseExport(name, "text/plain", target);
        }
        @Override public void chooseImport(ApdFiles.ImportSource source) {
            throw new AssertionError("Report export does not import history");
        }
        @Override public String where(){ return "test provider"; }
    }

    @Test public void rejectedReportThreadClosesStreamAndCompletesOnce() {
        TrackedStream stream = new TrackedStream();
        AtomicInteger completions = new AtomicInteger();
        String[] message = {null};
        boolean[] ran = {false};
        ReportExport.startSaveWorker(stream, text -> {
            completions.incrementAndGet();
            message[0] = text;
        }, () -> ran[0] = true, worker -> {
            throw new IllegalStateException("injected thread failure");
        });
        assertTrue(stream.closed);
        assertFalse(ran[0]);
        assertEquals(1, completions.get());
        assertTrue(message[0], message[0].contains("could not start saving: IllegalStateException"));
        assertFalse(message[0], message[0].startsWith("Saved"));
    }

    @Test public void reportThreadOutOfMemoryClosesStreamAndReportsFailure() {
        TrackedStream stream = new TrackedStream();
        AtomicInteger completions = new AtomicInteger();
        String[] message = {null};
        ReportExport.startSaveWorker(stream, text -> {
            completions.incrementAndGet();
            message[0] = text;
        }, () -> fail("worker should not run after rejected start"), worker -> {
            throw new OutOfMemoryError("injected failure, not real memory exhaustion");
        });
        assertTrue(stream.closed);
        assertEquals(1, completions.get());
        assertTrue(message[0], message[0].contains("memory was exhausted"));
    }

    @Test public void acceptedReportWorkerRetainsStreamOwnershipUntilExecution() {
        TrackedStream stream = new TrackedStream();
        Runnable[] pending = {null};
        AtomicInteger completions = new AtomicInteger();
        ReportExport.startSaveWorker(stream, text -> completions.incrementAndGet(), () -> {
            try { stream.close(); } catch (IOException e) { throw new AssertionError(e); }
            completions.incrementAndGet();
        }, job -> pending[0] = job);
        assertNotNull(pending[0]);
        assertFalse("provider must not close an accepted stream", stream.closed);
        assertEquals(0, completions.get());
        pending[0].run();
        assertTrue(stream.closed);
        assertEquals(1, completions.get());
    }

    @Test public void providerThrowAfterStreamHandoffCannotOverrideSuccessfulExport() throws Exception {
        ApdFiles.Provider previous = ApdFiles.provider;
        TrackedStream stream = new TrackedStream();
        CountDownLatch complete = new CountDownLatch(1);
        AtomicInteger completions = new AtomicInteger();
        String[] result = {null};
        try {
            ApdFiles.provider = new Provider(){
                @Override public void chooseExport(String name, String mime, ApdFiles.ExportTarget target) {
                    target.onStream(stream, "selected-report.txt");
                    throw new IllegalStateException("picker threw after stream was transferred");
                }
            };
            ReportExport.save(new File("."), new Report("Worker test", 1800000000000L, "fixture"),
                    ReportExport.Format.TEXT, message -> {
                        result[0] = message;
                        completions.incrementAndGet();
                        complete.countDown();
                    });
            assertTrue("worker must finish", complete.await(5, TimeUnit.SECONDS));
            assertEquals(1, completions.get());
            assertTrue(result[0], result[0].startsWith("Saved"));
            assertTrue(stream.closed);
            assertTrue(stream.toString(StandardCharsets.UTF_8.name()).contains("WORKER TEST"));
        } finally { ApdFiles.provider = previous; }
    }

    @Test public void workerWriteOutOfMemoryClosesStreamAndReportsFailureOnce() throws Exception {
        ApdFiles.Provider previous = ApdFiles.provider;
        CountDownLatch complete = new CountDownLatch(1);
        AtomicInteger completions = new AtomicInteger();
        String[] result = {null};
        boolean[] closed = {false};
        try {
            ApdFiles.provider = new Provider(){
                @Override public void chooseExport(String name, String mime, ApdFiles.ExportTarget target){
                    target.onStream(new OutputStream() {
                        @Override public void write(int value){ throw new OutOfMemoryError("injected write failure"); }
                        @Override public void write(byte[] bytes, int offset, int length){
                            throw new OutOfMemoryError("injected write failure");
                        }
                        @Override public void close(){ closed[0] = true; }
                    }, "failed-report.txt");
                    target.onFailed("late provider failure after stream transfer");
                }
            };
            ReportExport.save(new File("."), new Report("Write failure", 1800000000000L, "fixture"),
                    ReportExport.Format.TEXT, message -> {
                        result[0] = message;
                        completions.incrementAndGet();
                        complete.countDown();
                    });
            assertTrue(complete.await(5, TimeUnit.SECONDS));
            assertTrue(closed[0]);
            assertEquals(1, completions.get());
            assertTrue(result[0], result[0].contains("memory was exhausted during export"));
        } finally { ApdFiles.provider = previous; }
    }

    @Test public void pickerFailureBeforeStreamStillCompletesExactlyOnce() {
        ApdFiles.Provider previous = ApdFiles.provider;
        AtomicInteger completions = new AtomicInteger();
        String[] result = {null};
        try {
            ApdFiles.provider = new Provider(){
                @Override public void chooseExport(String name, String mime, ApdFiles.ExportTarget target){
                    target.onFailed("picker unavailable");
                    throw new IllegalStateException("same failure again");
                }
            };
            ReportExport.save(new File("."), new Report("Picker failure", 1800000000000L, "fixture"),
                    ReportExport.Format.TEXT, message -> {
                        result[0] = message;
                        completions.incrementAndGet();
                    });
            assertEquals(1, completions.get());
            assertTrue(result[0], result[0].contains("picker unavailable"));
        } finally { ApdFiles.provider = previous; }
    }
}
