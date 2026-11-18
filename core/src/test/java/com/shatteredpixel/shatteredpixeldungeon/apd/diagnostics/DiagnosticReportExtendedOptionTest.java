package com.shatteredpixel.shatteredpixeldungeon.apd.diagnostics;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import com.watabou.utils.SupportActionJournal;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** The player must opt in, and ordinary reports must never inherit extended chronology. */
public class DiagnosticReportExtendedOptionTest {
    private abstract static class Provider implements ApdFiles.Provider {
        public void chooseExport(String n,ApdFiles.ExportTarget t){throw new AssertionError("MIME export required");}
        public void chooseImport(ApdFiles.ImportSource source){throw new AssertionError("bug reports do not import history");}
        public String where(){return "test"; }
    }
    private static Map<String,String> unzip(byte[] bytes) throws IOException {
        Map<String,String> entries=new LinkedHashMap<>();
        try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(bytes),StandardCharsets.UTF_8)){
            ZipEntry entry;
            byte[] block=new byte[4096];
            while((entry=in.getNextEntry())!=null){
                ByteArrayOutputStream data=new ByteArrayOutputStream();
                int count;
                while((count=in.read(block))!=-1)data.write(block,0,count);
                entries.put(entry.getName(),data.toString(StandardCharsets.UTF_8.name()));
                in.closeEntry();
            }
        }
        return entries;
    }

    @Test public void standardNeverIncludesExtendedEventsEvenWhenConsentIsOn() throws Exception {
        HeadlessTestSupport.start();
        File root=Files.createTempDirectory("apd-standard-private-").toFile();
        SupportActionJournal.configure(root);
        SupportActionJournal.setEnabled(true);
        SupportActionJournal.record("touch.DOWN","distinct-private-journal-sentinel");
        ApdFiles.Provider old=ApdFiles.provider;
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        CountDownLatch done=new CountDownLatch(1);
        String[] message={null};
        try {
            ApdFiles.provider=new Provider(){
                @Override public void chooseExport(String name,String mime,ApdFiles.ExportTarget t){
                    assertTrue(name.startsWith("APD-diagnostic-"));
                    assertEquals("text/plain",mime);
                    t.onStream(bytes,name);
                }
            };
            DiagnosticReport.export(m->{message[0]=m;done.countDown();});
            assertTrue(done.await(5,TimeUnit.SECONDS));
            assertTrue(message[0].startsWith("Diagnostic report saved"));
            String text=bytes.toString(StandardCharsets.UTF_8.name());
            assertTrue(text.startsWith("APD SUPPORT REPORT v1"));
            assertFalse(text.contains("distinct-private-journal-sentinel"));
            assertFalse(text.contains("APD ACTION HISTORY SINCE LAST SUCCESSFUL REPORT"));
        } finally {
            ApdFiles.provider=old;
            SupportActionJournal.setEnabled(false);
        }
    }

    @Test public void strongestCompressionIsLosslessPortableAndMuchSmallerForRepetitiveEvents() throws Exception {
        HeadlessTestSupport.start();
        File root=Files.createTempDirectory("apd-max-compression-").toFile();
        SupportActionJournal.configure(root);
        SupportActionJournal.setEnabled(true);
        ApdFiles.Provider old=ApdFiles.provider;
        try {
            for(int i=0;i<1500;i++)
                SupportActionJournal.record("hero.action","type=Move floor=3 from=42 target=43");
            ByteArrayOutputStream raw=new ByteArrayOutputStream();
            SupportActionJournal.export(raw,SupportActionJournal.checkpoint());
            ByteArrayOutputStream zipBytes=new ByteArrayOutputStream();
            CountDownLatch done=new CountDownLatch(1);
            String[] status={null};
            ApdFiles.provider=new Provider() {
                @Override public void chooseExport(String filename,String mime,ApdFiles.ExportTarget target) {
                    assertTrue(filename.endsWith("-UTC.zip"));
                    assertEquals("application/zip",mime);
                    target.onStream(zipBytes,filename);
                }
            };
            DiagnosticReport.export(message->{status[0]=message;done.countDown();},true);
            assertTrue(done.await(20,TimeUnit.SECONDS));
            assertTrue(status[0],status[0].contains("saved"));
            Map<String,String> files=unzip(zipBytes.toByteArray());
            assertEquals(2,files.size());
            assertEquals(raw.toString(StandardCharsets.UTF_8.name()),files.get("APD-action-history.txt"));
            assertTrue(files.get("APD-diagnostic.txt").startsWith("APD SUPPORT REPORT v1"));
            assertTrue("Expected strong compression for repetitive diagnostic events",
                    zipBytes.size()<raw.size()/4);
        } finally {
            ApdFiles.provider=old;
            SupportActionJournal.setEnabled(false);
        }
    }

    @Test public void extendedIncludesJournalAndResetsOnlyAfterSuccessfulClose() throws Exception {
        HeadlessTestSupport.start();
        File root=Files.createTempDirectory("apd-extended-private-").toFile();
        SupportActionJournal.configure(root);
        SupportActionJournal.setEnabled(true);
        SupportActionJournal.record("touch.DOWN","chronology-sentinel");
        ApdFiles.Provider old=ApdFiles.provider;
        try {
            ByteArrayOutputStream failed=new ByteArrayOutputStream(){
                @Override public void close() throws IOException {
                    super.close(); throw new IOException("injected close failure");
                }
            };
            CountDownLatch failDone=new CountDownLatch(1);
            ApdFiles.provider=new Provider() {
                @Override public void chooseExport(String n,String mime,ApdFiles.ExportTarget t){t.onStream(failed,n);}
            };
            DiagnosticReport.export(m->{
                assertTrue(m.contains("could not be saved"));
                failDone.countDown();
            },true);
            assertTrue(failDone.await(5,TimeUnit.SECONDS));

            ByteArrayOutputStream successful=new ByteArrayOutputStream();
            CountDownLatch successDone=new CountDownLatch(1);
            ApdFiles.provider=new Provider() {
                @Override public void chooseExport(String n,String mime,ApdFiles.ExportTarget t){t.onStream(successful,n);}
            };
            DiagnosticReport.export(m->{
                assertTrue(m.contains("Compressed Extended bug report saved."));
                successDone.countDown();
            },true);
            assertTrue(successDone.await(5,TimeUnit.SECONDS));
            Map<String,String> files=unzip(successful.toByteArray());
            assertEquals(2,files.size());
            assertTrue(files.get("APD-diagnostic.txt").startsWith("APD SUPPORT REPORT v1"));
            String text=files.get("APD-action-history.txt");
            assertTrue(text.contains("APD ACTION HISTORY SINCE LAST SUCCESSFUL REPORT"));
            assertTrue(text.contains("chronology-sentinel"));
            ByteArrayOutputStream next=new ByteArrayOutputStream();
            SupportActionJournal.export(next,SupportActionJournal.checkpoint());
            assertFalse(next.toString(StandardCharsets.UTF_8.name()).contains("chronology-sentinel"));
        } finally {
            ApdFiles.provider=old;
            SupportActionJournal.setEnabled(false);
        }
    }
}
