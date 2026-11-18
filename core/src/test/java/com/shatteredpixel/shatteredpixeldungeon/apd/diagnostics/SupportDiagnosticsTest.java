/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.shatteredpixel.shatteredpixeldungeon.apd.diagnostics;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import com.watabou.utils.SupportDiagnostics;
import org.junit.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class SupportDiagnosticsTest {
    @Test public void diagnosticCaptureCannotReplaceTheOriginalFailureWhenStackAllocationFails()throws Exception{
        File dir=Files.createTempDirectory("apd-diag-low-memory-").toFile();SupportDiagnostics.configure(dir,"capture failure fixture");
        RuntimeException original=new RuntimeException("original failure"){
            @Override public StackTraceElement[] getStackTrace(){throw new OutOfMemoryError("injected stack allocation failure");}
        };
        SupportDiagnostics.error(original);
        String report=SupportDiagnostics.report("");
        assertTrue(report.contains("original failure"));
        assertTrue(report.contains("Error diagnostic capture unavailable"));
    }
    @Test public void traceIsBoundedAndKeepsTheMostRecentEvents()throws Exception{
        File dir=Files.createTempDirectory("apd-diagnostic-test-").toFile();SupportDiagnostics.configure(dir,"TEST fixture build / fixture device");
        for(int i=0;i<1000;i++)SupportDiagnostics.event("fixture."+i,String.join("",java.util.Collections.nCopies(100,"detail")));
        assertTrue(SupportDiagnostics.awaitWrites(5000));
        String report=SupportDiagnostics.report("fixture memory / exit reasons");
        assertTrue(report.contains("fixture.999"));assertFalse(report.contains("fixture.0 "));
        assertTrue(new File(dir,"session.txt").length()<=65536);
        assertTrue(report.contains("Java heap bytes: used="));assertTrue(report.contains("TEST fixture build / fixture device"));
    }
    @Test public void previousSessionAndUncaughtEvidenceSurviveRestart()throws Exception{
        File first=Files.createTempDirectory("apd-diag-first-").toFile(),other=Files.createTempDirectory("apd-diag-other-").toFile();
        SupportDiagnostics.configure(first,"first build");SupportDiagnostics.event("before.failure","native trace fixture");assertTrue(SupportDiagnostics.awaitWrites(5000));
        SupportDiagnostics.uncaught(Thread.currentThread(),new OutOfMemoryError("controlled test, not a real allocation failure"));
        SupportDiagnostics.configure(other,"other process fixture");assertTrue(SupportDiagnostics.awaitWrites(5000));
        SupportDiagnostics.configure(first,"new build");String report=SupportDiagnostics.report("OS reasons unavailable in a core test");
        assertTrue(report.contains("PREVIOUS SESSION\nAPD diagnostic session"));assertTrue(report.contains("before.failure"));
        assertTrue(report.contains("java.lang.OutOfMemoryError"));assertTrue(report.contains("controlled test"));
    }
    @Test public void activityReconfigurationDoesNotEraseTheCurrentSession()throws Exception{
        File dir=Files.createTempDirectory("apd-diag-recreate-").toFile();SupportDiagnostics.configure(dir,"same process");
        SupportDiagnostics.event("before.recreate","kept");SupportDiagnostics.configure(dir,"same process");
        assertTrue(SupportDiagnostics.report("").contains("before.recreate"));
    }
    @Test public void exportWritesReadableTextClosesStreamAndReportsRealSuccess()throws Exception{
        HeadlessTestSupport.start();ApdFiles.Provider original=ApdFiles.provider;
        CountDownLatch done=new CountDownLatch(1);String[] message={null};boolean[] closed={false};
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(){@Override public void close()throws IOException{closed[0]=true;super.close();}};
        try{
            ApdFiles.provider=new Provider(){@Override public void chooseExport(String name,String mime,ApdFiles.ExportTarget target){
                assertTrue(name.startsWith("APD-diagnostic-"));assertTrue(name.endsWith("-UTC.txt"));assertEquals("text/plain",mime);target.onStream(bytes,name);
            }};
            DiagnosticReport.export(s->{message[0]=s;done.countDown();});assertTrue(done.await(5,TimeUnit.SECONDS));
            assertTrue(closed[0]);assertTrue(message[0].startsWith("Diagnostic report saved."));
            String text=bytes.toString(StandardCharsets.UTF_8.name());assertTrue(text.startsWith("APD SUPPORT REPORT v1"));assertTrue(text.contains("CURRENT SESSION"));
        }finally{ApdFiles.provider=original;}
    }
    @Test public void cancellationAndFailedWriteNeverClaimAReportWasSaved()throws Exception{
        HeadlessTestSupport.start();ApdFiles.Provider original=ApdFiles.provider;String[] message={null};CountDownLatch failed=new CountDownLatch(1);
        try{
            ApdFiles.provider=new Provider(){@Override public void chooseExport(String n,String m,ApdFiles.ExportTarget t){t.onCancelled();}};
            DiagnosticReport.export(s->message[0]=s);assertTrue(message[0].contains("No report was saved"));
            ApdFiles.provider=new Provider(){@Override public void chooseExport(String n,String m,ApdFiles.ExportTarget t){t.onStream(new OutputStream(){public void write(int b)throws IOException{throw new IOException("controlled failure");}},n);}};
            DiagnosticReport.export(s->{message[0]=s;failed.countDown();});assertTrue(failed.await(5,TimeUnit.SECONDS));assertTrue(message[0].contains("could not be saved"));
        }finally{ApdFiles.provider=original;}
    }
    @Test public void phaseHeapSamplesHaveTimestampsUnitsAndSurviveExport()throws Exception{
        File dir=Files.createTempDirectory("apd-diag-memory-phase-").toFile();
        SupportDiagnostics.configure(dir,"heap phase fixture");
        SupportDiagnostics.memoryEvent("uimsd.open_begin");SupportDiagnostics.memoryEvent("uimsd.model_READY");
        assertTrue(SupportDiagnostics.awaitWrites(5000));
        String report=SupportDiagnostics.report("");
        java.util.regex.Matcher samples=java.util.regex.Pattern.compile("(?m)^(\\d+) memory.sample phase=(uimsd\\.\\w+) used_bytes=(\\d+) committed_bytes=(\\d+) limit_bytes=(\\d+)$").matcher(report.substring(report.indexOf("MEMORY HISTORY"),report.indexOf("RECENT EVENTS")));
        int count=0;
        while(samples.find()){
            assertTrue(Long.parseLong(samples.group(1))>0);
            long used=Long.parseLong(samples.group(3)),committed=Long.parseLong(samples.group(4)),limit=Long.parseLong(samples.group(5));
            assertTrue(used>=0&&used<=committed);assertTrue(committed<=limit);count++;
        }
        assertEquals(2,count);
        String persisted=new String(Files.readAllBytes(new File(dir,"session.txt").toPath()),StandardCharsets.UTF_8);
        assertTrue(persisted.contains("phase=uimsd.open_begin"));assertTrue(persisted.contains("phase=uimsd.model_READY"));
    }
    @Test public void repeatedHeapSamplingRetainsOnlyBoundedRecentPhases()throws Exception{
        File dir=Files.createTempDirectory("apd-diag-memory-bound-").toFile();SupportDiagnostics.configure(dir,"bounded phase fixture");
        for(int i=0;i<1000;i++)SupportDiagnostics.memoryEvent("phase_"+i);
        assertTrue(SupportDiagnostics.awaitWrites(5000));String report=SupportDiagnostics.report("");
        assertTrue(report.contains("phase=phase_999 "));assertFalse(report.contains("phase=phase_0 "));
        assertTrue(new File(dir,"session.txt").length()<=65536);
    }
    @Test public void heapHistorySurvivesBurstOfInputWithoutUnboundedRetention()throws Exception{
        File dir=Files.createTempDirectory("apd-heap-history-").toFile();
        SupportDiagnostics.configure(dir,"memory history fixture");
        SupportDiagnostics.memoryEvent("activity.pause");
        SupportDiagnostics.memoryEvent("activity.resume");
        SupportDiagnostics.memoryEvent("game.active");
        for(int i=0;i<500;i++)SupportDiagnostics.event("input.dispatched","id="+i+" kind=touch.DOWN");
        assertTrue(SupportDiagnostics.awaitWrites(5000));
        String report=SupportDiagnostics.report("");
        assertTrue(report.contains("MEMORY HISTORY"));
        assertTrue(report.contains("phase=activity.pause "));
        assertTrue(report.contains("phase=activity.resume "));
        assertTrue(report.contains("phase=game.active "));
        assertTrue(report.contains("input.dispatched id=499"));
        assertFalse(report.contains("input.dispatched id=0 "));
        String persisted=new String(Files.readAllBytes(new File(dir,"session.txt").toPath()),StandardCharsets.UTF_8);
        assertTrue(persisted.contains("MEMORY HISTORY"));
        assertTrue(persisted.contains("phase=activity.pause "));
        assertTrue(new File(dir,"session.txt").length()<=65536);
    }

    @Test public void periodicMemorySamplingIsRateLimited()throws Exception{
        File dir=Files.createTempDirectory("apd-rate-limited-memory-").toFile();
        SupportDiagnostics.configure(dir,"periodic fixture");
        SupportDiagnostics.periodicMemoryEvent();
        SupportDiagnostics.periodicMemoryEvent();
        SupportDiagnostics.periodicMemoryEvent();
        assertTrue(SupportDiagnostics.awaitWrites(5000));
        String report=SupportDiagnostics.report("");
        String history=report.substring(report.indexOf("MEMORY HISTORY"),report.indexOf("RECENT EVENTS"));
        assertEquals(1,history.split("phase=game.active",-1).length-1);
        assertTrue(history.contains("limit_bytes="));
    }

    @Test public void androidMemoryPressureSeveritySurvivesNoisyInputHistory()throws Exception{
        File dir=Files.createTempDirectory("apd-android-pressure-").toFile();
        SupportDiagnostics.configure(dir,"trim severity fixture");
        SupportDiagnostics.memoryEvent("android.trim_memory",80);
        SupportDiagnostics.memoryEvent("android.low_memory");
        for(int i=0;i<500;i++)SupportDiagnostics.event("input.dispatched","id="+i+" kind=touch.DOWN");
        assertTrue(SupportDiagnostics.awaitWrites(5000));
        String report=SupportDiagnostics.report("");
        String history=report.substring(report.indexOf("MEMORY HISTORY"),report.indexOf("RECENT EVENTS"));
        boolean trimFound=false,lowFound=false;
        for(String line:history.split("\n")){
            if(line.contains("phase=android.trim_memory ")){
                assertTrue(line.contains(" used_bytes="));
                assertTrue(line.endsWith(" trim_level=80"));
                trimFound=true;
            }
            if(line.contains("phase=android.low_memory ")){
                assertTrue(line.contains(" used_bytes="));
                assertFalse(line.contains("trim_level="));
                lowFound=true;
            }
        }
        assertTrue(trimFound);assertTrue(lowFound);
        assertFalse(history.contains("input.dispatched"));
        String persisted=new String(Files.readAllBytes(new File(dir,"session.txt").toPath()),StandardCharsets.UTF_8);
        assertTrue(persisted.contains("trim_level=80"));
        assertTrue(persisted.contains("phase=android.low_memory "));
        assertTrue(new File(dir,"session.txt").length()<=65536);
    }

    @Test public void eventAndExceptionLineBreaksCannotForgeDiagnosticRecords()throws Exception{
        File dir=Files.createTempDirectory("apd-diag-line-boundary-").toFile();
        SupportDiagnostics.configure(dir,"line integrity fixture");
        SupportDiagnostics.event("one\rforged.kind\nmore.kind",
                "message\r123456 forged.entry\nstill detail");
        SupportDiagnostics.error(new IllegalStateException("boom\r999999 forged.crash\ncontinued"));
        assertTrue(SupportDiagnostics.awaitWrites(5000));
        String persisted=new String(Files.readAllBytes(new File(dir,"session.txt").toPath()),StandardCharsets.UTF_8);
        String events=persisted.substring(persisted.indexOf("RECENT EVENTS\n")+"RECENT EVENTS\n".length());
        assertFalse(events.contains("\r"));
        assertTrue(events.contains("one forged.kind more.kind message 123456 forged.entry still detail"));
        assertTrue(events.contains("error java.lang.IllegalStateException: boom 999999 forged.crash continued"));
        int eventLines=0,exceptionLines=0;
        for(String line:events.split("\n")){
            if(line.contains("forged.kind")){
                assertTrue(line.matches("\\d+ one forged\\.kind more\\.kind message 123456 forged\\.entry still detail"));
                eventLines++;
            }
            if(line.contains("forged.crash")){
                assertTrue(line.matches("\\d+ error java\\.lang\\.IllegalStateException: boom 999999 forged\\.crash continued"));
                exceptionLines++;
            }
        }
        assertEquals(1,eventLines);
        assertEquals(1,exceptionLines);
    }

    @Test public void diagnosticSymlinkAliasesCannotLeakOrOverwriteExternalFiles()throws Exception{
        File dir=Files.createTempDirectory("apd-diag-alias-").toFile();
        File outside=Files.createTempDirectory("apd-diag-external-").toFile();
        File privateHistory=new File(outside,"unrelated-history.txt");
        File privateCrash=new File(outside,"unrelated-save.txt");
        byte[] historyBytes="private-history-sentinel".getBytes(StandardCharsets.UTF_8);
        byte[] crashBytes="private-save-sentinel".getBytes(StandardCharsets.UTF_8);
        Files.write(privateHistory.toPath(),historyBytes);
        Files.write(privateCrash.toPath(),crashBytes);
        Files.createSymbolicLink(new File(dir,"session.txt").toPath(),privateHistory.toPath());
        Files.createSymbolicLink(new File(dir,"last-crash.txt").toPath(),privateCrash.toPath());

        SupportDiagnostics.configure(dir,"symlink privacy fixture");
        assertTrue(SupportDiagnostics.awaitWrites(5000));
        String report=SupportDiagnostics.report("");
        assertFalse(report.contains("private-history-sentinel"));
        assertFalse(report.contains("private-save-sentinel"));
        SupportDiagnostics.uncaught(Thread.currentThread(),new IllegalStateException("controlled crash capture"));
        assertTrue(SupportDiagnostics.awaitWrites(5000));
        assertArrayEquals(historyBytes,Files.readAllBytes(privateHistory.toPath()));
        assertArrayEquals(crashBytes,Files.readAllBytes(privateCrash.toPath()));
    }

    @Test public void diagnosticTemporarySymlinkDoesNotTruncateExternalFile()throws Exception{
        File dir=Files.createTempDirectory("apd-diag-temp-alias-").toFile();
        File outside=Files.createTempFile("apd-diag-foreign-temp-",".txt").toFile();
        byte[] original="another-file-must-stay-unchanged".getBytes(StandardCharsets.UTF_8);
        Files.write(outside.toPath(),original);
        Files.createSymbolicLink(new File(dir,"session.txt.tmp").toPath(),outside.toPath());
        SupportDiagnostics.configure(dir,"temporary file alias fixture");
        SupportDiagnostics.event("alias.test","writer must not follow temporary symlinks");
        assertTrue(SupportDiagnostics.awaitWrites(5000));
        assertArrayEquals(original,Files.readAllBytes(outside.toPath()));
    }

    @Test public void canonicalizedSymlinkedDiagnosticsDirectoryStillWorks()throws Exception{
        File root=Files.createTempDirectory("apd-diag-real-root-").toFile();
        File parent=Files.createTempDirectory("apd-diag-root-alias-").toFile();
        File alias=new File(parent,"diagnostics");
        Files.createSymbolicLink(alias.toPath(),root.toPath());
        SupportDiagnostics.configure(alias,"valid symlinked root fixture");
        SupportDiagnostics.event("valid.root","persisted safely");
        assertTrue(SupportDiagnostics.awaitWrites(5000));
        String persisted=new String(Files.readAllBytes(new File(root,"session.txt").toPath()),StandardCharsets.UTF_8);
        assertTrue(persisted.contains("valid.root persisted safely"));
    }

    @Test public void diagnosticPickerExceptionBeforeCallbackReportsOneFailure()throws Exception{
        HeadlessTestSupport.start();
        ApdFiles.Provider original=ApdFiles.provider;
        AtomicInteger callbacks=new AtomicInteger();String[] message={null};
        try{
            ApdFiles.provider=new Provider(){@Override public void chooseExport(String name,String mime,ApdFiles.ExportTarget target){
                throw new IllegalStateException("picker unavailable");
            }};
            DiagnosticReport.export(result->{message[0]=result;callbacks.incrementAndGet();});
            assertEquals(1,callbacks.get());
            assertEquals("The diagnostic report could not be opened for saving: IllegalStateException",message[0]);
        }finally{ApdFiles.provider=original;}
    }

    @Test public void diagnosticPickerCallbackThenExceptionDoesNotDuplicateCompletion()throws Exception{
        HeadlessTestSupport.start();
        ApdFiles.Provider original=ApdFiles.provider;
        AtomicInteger callbacks=new AtomicInteger();String[] message={null};
        try{
            ApdFiles.provider=new Provider(){@Override public void chooseExport(String name,String mime,ApdFiles.ExportTarget target){
                target.onFailed("permission denied");
                throw new IllegalStateException("subsequent picker failure");
            }};
            DiagnosticReport.export(result->{message[0]=result;callbacks.incrementAndGet();});
            assertEquals(1,callbacks.get());
            assertEquals("The diagnostic report could not be opened for saving: permission denied",message[0]);
        }finally{ApdFiles.provider=original;}
    }

    @Test public void diagnosticPickerExceptionAfterStreamHandoffKeepsRealSaveResult()throws Exception{
        HeadlessTestSupport.start();
        ApdFiles.Provider original=ApdFiles.provider;
        AtomicInteger callbacks=new AtomicInteger();CountDownLatch done=new CountDownLatch(1);
        String[] message={null};ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try{
            ApdFiles.provider=new Provider(){@Override public void chooseExport(String name,String mime,ApdFiles.ExportTarget target){
                target.onStream(bytes,"diagnostic result");
                throw new IllegalStateException("picker threw after handing off stream");
            }};
            DiagnosticReport.export(result->{message[0]=result;callbacks.incrementAndGet();done.countDown();});
            assertTrue("The accepted stream worker should report its real outcome",done.await(5,TimeUnit.SECONDS));
            assertEquals(1,callbacks.get());
            assertTrue(message[0],message[0].startsWith("Diagnostic report saved."));
            assertTrue(bytes.toString(StandardCharsets.UTF_8.name()).startsWith("APD SUPPORT REPORT v1"));
        }finally{ApdFiles.provider=original;}
    }

    private static class Provider implements ApdFiles.Provider{
        public void chooseExport(String n,ApdFiles.ExportTarget t){throw new AssertionError("MIME-aware export expected");}
        public void chooseImport(ApdFiles.ImportSource s){throw new AssertionError("Diagnostics never import or read history");}
        public String where(){return "test stream";}
    }
}
