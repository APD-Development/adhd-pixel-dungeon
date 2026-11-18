package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import java.io.ByteArrayOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunLogFrameLengthValidationTest {
    @Test public void unsignedFrameLengthCannotWrapIntoAnEmptyValidFrame() throws Exception {
        byte[] header=LogHeader.encode(TelemetryVersion.CURRENT,1000,"preserved","test");
        ByteSink frame=new ByteSink().writeByte(LogFormat.SYNC).writeByte(250);
        // 2^63, outside supported nonnegative Java-long sizes; its int cast is zero.
        for(int i=0;i<9;i++)frame.writeByte(128);
        frame.writeByte(1);
        frame.writeInt32((int)LogFormat.crc(frame.rawBuffer(),1,frame.size()-1));
        ByteArrayOutputStream log=new ByteArrayOutputStream();log.write(header);log.write(frame.toByteArray());
        ReadResult result=RunLogReader.parse(log.toByteArray());
        assertEquals("oversized unsigned length must not be accepted as zero",ReadStatus.CORRUPT,result.status);
        assertEquals(header.length,result.validBytes);
        assertEquals(0,result.eventCount);
    }
    @Test public void validFutureFrameRemainsReadableAndSkipped() throws Exception {
        ByteArrayOutputStream log=new ByteArrayOutputStream();
        log.write(LogHeader.encode(TelemetryVersion.CURRENT,1000,"preserved","test"));
        log.write(LogFormat.frame(250,new byte[]{1,2,3}));
        ReadResult result=RunLogReader.parse(log.toByteArray());
        assertEquals(ReadStatus.UNSEALED,result.status);
        assertEquals(log.size(),result.validBytes);
        assertEquals(1,result.skippedUnknownFrames);
    }
}
