package com.shatteredpixel.shatteredpixeldungeon.apd.report;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public class ReportExportCompletionTest {
 private static class Sink extends OutputStream {
  final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
  final boolean failWrite,failClose; int closes;
  Sink(boolean w,boolean c){failWrite=w;failClose=c;}
  @Override public void write(int b) throws IOException {if(failWrite) throw new IOException("write failure");bytes.write(b);}
  @Override public void write(byte[] b,int o,int n) throws IOException {if(failWrite) throw new IOException("write failure");bytes.write(b,o,n);}
  @Override public void close() throws IOException {closes++; if(failClose)throw new IOException("close failure");super.close();}
 }
 private String save(Sink sink) throws Exception {
  ApdFiles.Provider old=ApdFiles.provider;
  CountDownLatch done=new CountDownLatch(1); AtomicReference<String> message=new AtomicReference<>();
  try {
   ApdFiles.provider=new ApdFiles.Provider(){
    public void chooseExport(String name,ApdFiles.ExportTarget target){target.onStream(sink,"chosen.txt");}
    public void chooseImport(ApdFiles.ImportSource source){throw new AssertionError("no import");}
    public String where(){return "test";}
   };
   ReportExport.save(new File("."),new Report("Test",0,"test"),ReportExport.Format.TEXT,s->{message.set(s);done.countDown();});
   assertTrue("export completion callback",done.await(5,TimeUnit.SECONDS));
   return message.get();
  } finally {ApdFiles.provider=old;}
 }
 @Test public void closeFailureMustNotClaimSaved() throws Exception {
  Sink sink=new Sink(false,true);String result=save(sink);
  assertTrue(result,result.startsWith("The report could not be saved:"));assertEquals(1,sink.closes);
 }
 @Test public void writeFailureClosesAndReportsFailure() throws Exception {
  Sink sink=new Sink(true,false);String result=save(sink);
  assertTrue(result,result.startsWith("The report could not be saved:"));assertEquals(1,sink.closes);
 }
 @Test public void successfulExportClosesBeforeReportingSuccess() throws Exception {
  Sink sink=new Sink(false,false);String result=save(sink);
  assertTrue(result,result.startsWith("Saved Plain text report"));assertEquals(1,sink.closes);assertTrue(sink.bytes.size()>0);
 }
}
