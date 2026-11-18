package com.shatteredpixel.shatteredpixeldungeon.apd.stats;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

public class RawUtf8IntegrityTest {
    private static final byte[][] INVALID={
        {(byte)0x80}, {(byte)0xC0,(byte)0xAF}, {(byte)0xE2,(byte)0x82},
        {(byte)0xED,(byte)0xA0,(byte)0x80}, {(byte)0xF4,(byte)0x90,(byte)0x80,(byte)0x80}, {(byte)0xFF}
    };
    @Test public void malformedLengthPrefixedTextIsRefusedRatherThanReplaced() {
        for(byte[] bytes:INVALID) {
            ByteSink data=new ByteSink().writeUnsigned(bytes.length).writeBytes(bytes);
            try {new ByteSource(data.toByteArray()).readString();fail("invalid UTF-8 accepted");}
            catch(FormatException expected){}
        }
    }
    @Test public void checksummedMalformedStringFrameMarksDamageWithoutChangingSourceBytes() {
        for(byte[] bytes:INVALID) {
            byte[] header=LogHeader.encode(TelemetryVersion.CURRENT,1,"unicode","test");
            byte[] file=new ByteSink().writeBytes(header).writeBytes(LogFormat.stringFrame(0,"valid"))
                    .writeBytes(LogFormat.frame(LogFormat.FRAME_STRING,new ByteSink().writeUnsigned(1).writeBytes(bytes).toByteArray()))
                    .writeBytes(LogFormat.sealFrame(0,0)).toByteArray();
            byte[] original=file.clone();ReadResult read=RunLogReader.parse(file);
            assertEquals(ReadStatus.CORRUPT,read.status);
            assertEquals("valid",read.get(0));assertNull(read.get(1));
            assertArrayEquals(original,file);assertTrue(read.damagedBytes()>0);
        }
    }
    @Test public void malformedChecksummedBuildProvenanceIsNotAcceptedAsAValidHeader() {
        ByteSink body=new ByteSink().writeUnsigned(TelemetryVersion.CURRENT.major)
                .writeUnsigned(TelemetryVersion.CURRENT.minor).writeUnsigned(1).writeString("run")
                .writeUnsigned(2).writeBytes(new byte[]{(byte)0xC0,(byte)0xAF});
        ByteSink file=new ByteSink().writeBytes(LogFormat.MAGIC).writeByte(LogFormat.CONTAINER_FORMAT)
                .writeByte(body.size()>>>8).writeByte(body.size()).writeBytes(body.toByteArray());
        file.writeInt32((int)LogFormat.crc(file.rawBuffer(),LogFormat.MAGIC.length,file.size()-LogFormat.MAGIC.length));
        assertEquals(ReadStatus.BAD_HEADER,RunLogReader.parse(file.toByteArray()).status);
    }
    @Test public void validUnicodeIncludingLiteralReplacementCharactersRemainsReadable() {
        for(String value:new String[]{"","ASCII","héllo 世界 😀","\uFFFD",new String(Character.toChars(0x10FFFF)),"\u0000"}) {
            assertEquals(value,new ByteSource(new ByteSink().writeString(value).toByteArray()).readString());
            byte[] file=new ByteSink().writeBytes(LogHeader.encode(TelemetryVersion.CURRENT,1,"run",value))
                    .writeBytes(LogFormat.stringFrame(0,value)).writeBytes(LogFormat.sealFrame(0,0)).toByteArray();
            ReadResult read=RunLogReader.parse(file);assertEquals(ReadStatus.SEALED,read.status);
            assertEquals(value,read.header.writerBuild);assertEquals(value,read.get(0));
        }
    }
    @Test public void recoveryPreservesEveryDamagedByteInItsTailBackup() throws Exception {
        byte[] header=LogHeader.encode(TelemetryVersion.CURRENT,1,"run","test");
        byte[] data=new ByteSink().writeBytes(header).writeBytes(LogFormat.stringFrame(0,"valid"))
                .writeBytes(LogFormat.frame(LogFormat.FRAME_STRING,new ByteSink().writeUnsigned(1).writeBytes(INVALID[1]).toByteArray()))
                .writeBytes(LogFormat.sealFrame(0,0)).toByteArray();
        java.io.File file=java.io.File.createTempFile("utf8-recovery-",".apdlog");
        java.nio.file.Files.write(file.toPath(),data);
        LogRecovery.RepairReport report=null;
        try {
            report=LogRecovery.repair(file);assertTrue(report.truncated);assertNull(report.error);assertNotNull(report.savedTail);
            byte[] prefix=java.nio.file.Files.readAllBytes(file.toPath());
            byte[] tail=java.nio.file.Files.readAllBytes(report.savedTail.toPath());
            assertArrayEquals(data,new ByteSink().writeBytes(prefix).writeBytes(tail).toByteArray());
            assertEquals("valid",RunLogReader.parse(prefix).get(0));
        } finally {file.delete();if(report!=null && report.savedTail!=null)report.savedTail.delete();}
    }

}
