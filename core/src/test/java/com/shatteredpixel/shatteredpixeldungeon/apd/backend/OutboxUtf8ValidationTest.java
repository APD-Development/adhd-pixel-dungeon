package com.shatteredpixel.shatteredpixeldungeon.apd.backend;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
public class OutboxUtf8ValidationTest {
 @Test public void malformedEncodedDetailIsReportedWithoutChangingStoredBytes() throws Exception {
  Path file=Files.createTempFile("outbox-utf8", ".txt");
  try {
   String payload=Base64.getEncoder().encodeToString("report".getBytes(StandardCharsets.UTF_8));
   String detail=Base64.getEncoder().encodeToString(new byte[]{(byte)0xc3,0x28});
   byte[] original=("ob_1\t1\t0\t0\tapd.report\t1\t0\t"+payload+"\t"+detail+"\n").getBytes(StandardCharsets.UTF_8);
   Files.write(file,original);
   Outbox box=new Outbox(); OutboxStore.Loaded loaded=OutboxStore.load(box,file.toFile());
   assertEquals(1,loaded.skipped); assertEquals(0,loaded.items); assertEquals(0,box.size());
   assertArrayEquals(original,Files.readAllBytes(file));
  } finally { Files.deleteIfExists(file); }
 }
 @Test public void validUnicodeRoundTripsExactly() throws Exception {
  Path file=Files.createTempFile("outbox-unicode", ".txt");
  try {
   Outbox box=new Outbox(); Outbox.Item item=box.enqueue(Id.of(Id.Kind.OUTBOX,"ob_1"),new Envelope("apd.report",1,0,"caf\u00e9 \ud83c\udf0e"),1);
   item.lastDetail="\u65e5\u672c\u8a9e"; OutboxStore.save(box,file.toFile());
   Outbox restored=new Outbox(); OutboxStore.Loaded loaded=OutboxStore.load(restored,file.toFile());
   assertEquals(1,loaded.items); assertEquals(0,loaded.skipped);
   assertEquals(item.envelope.payload,restored.items().get(0).envelope.payload);
   assertEquals(item.lastDetail,restored.items().get(0).lastDetail);
  } finally { Files.deleteIfExists(file); }
 }
}
