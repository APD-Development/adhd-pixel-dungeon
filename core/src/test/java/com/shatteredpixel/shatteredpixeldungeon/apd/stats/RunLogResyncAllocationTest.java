package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import java.io.ByteArrayOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class RunLogResyncAllocationTest {
    @Test public void enormousSparseStringIdIsRejectedDuringRecovery() throws Exception {
        ByteArrayOutputStream log=new ByteArrayOutputStream();
        log.write(LogHeader.encode(TelemetryVersion.CURRENT,1000,"preserved","test"));
        log.write(LogFormat.stringFrame(Integer.MAX_VALUE,"known text"));
        ReadResult result=RunLogReader.parse(log.toByteArray(),new ReadOptions().resync(true));
        assertEquals(ReadStatus.CORRUPT,result.status);
        assertEquals(0,result.strings.size());
    }
}
