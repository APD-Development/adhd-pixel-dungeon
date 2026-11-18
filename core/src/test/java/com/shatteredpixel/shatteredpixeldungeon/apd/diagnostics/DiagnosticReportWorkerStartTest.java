package com.shatteredpixel.shatteredpixeldungeon.apd.diagnostics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class DiagnosticReportWorkerStartTest {

    private static final class TrackedStream extends ByteArrayOutputStream {
        boolean closed;
        @Override public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    @Test public void rejectedThreadStartClosesAcceptedStreamAndReportsFailureOnce() {
        TrackedStream stream = new TrackedStream();
        int[] completions = {0};
        String[] message = {null};
        boolean[] ran = {false};
        DiagnosticReport.launchExportWorker(stream, text -> {
            completions[0]++;
            message[0] = text;
        }, () -> ran[0] = true, job -> {
            throw new IllegalStateException("injected worker-start failure");
        });
        assertTrue("accepted stream must be released after launch failure", stream.closed);
        assertFalse("failed worker must not execute", ran[0]);
        assertEquals("only one terminal result", 1, completions[0]);
        assertTrue(message[0], message[0].contains("could not start: IllegalStateException"));
        assertFalse(message[0], message[0].contains("saved"));
    }

    @Test public void memoryExhaustionOnWorkerStartAlsoClosesAndReports() {
        TrackedStream stream = new TrackedStream();
        int[] completions = {0};
        String[] message = {null};
        DiagnosticReport.launchExportWorker(stream, text -> {
            completions[0]++;
            message[0] = text;
        }, () -> fail("worker must not run after rejected start"), job -> {
            throw new OutOfMemoryError("injected thread allocation failure");
        });
        assertTrue(stream.closed);
        assertEquals(1, completions[0]);
        assertTrue(message[0], message[0].contains("memory was exhausted"));
        assertFalse(message[0], message[0].contains("saved"));
    }

    @Test public void successfulLaunchPreservesStreamOwnershipUntilWorkerRuns() {
        TrackedStream stream = new TrackedStream();
        int[] completions = {0};
        boolean[] launched = {false};
        DiagnosticReport.launchExportWorker(stream, text -> completions[0]++,
                () -> { launched[0] = true; }, Runnable::run);
        assertTrue(launched[0]);
        assertFalse("provider must not close a successfully handed-off stream", stream.closed);
        assertEquals(0, completions[0]);
    }
}
