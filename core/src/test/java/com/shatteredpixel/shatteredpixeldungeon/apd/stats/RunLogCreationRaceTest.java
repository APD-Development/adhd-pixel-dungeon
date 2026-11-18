package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import java.io.File;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class RunLogCreationRaceTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    @Test public void aLogAppearingAfterExistenceCheckIsNeverTruncated() throws Exception {
        File actual=new File(tmp.getRoot(),"existing.apdlog");
        byte[] evidence={9,8,7,6,5};Files.write(actual.toPath(),evidence);
        // Simulate a competing writer claiming the path after the existence check.
        File checked=new File(actual.getPath()){
            @Override public boolean exists(){return false;}
        };
        RunLogWriter writer=RunLogWriter.create(checked,TelemetryVersion.CURRENT,"existing","test",1);
        boolean disabled=writer.isDisabled();
        RunLogWriter.OpenStatus status=writer.openStatus();writer.close(false);
        assertArrayEquals("competing log evidence must survive",evidence,Files.readAllBytes(actual.toPath()));
        assertTrue("creation must atomically claim the path before opening a truncating stream",disabled);
        assertEquals(RunLogWriter.OpenStatus.FAILED_EXISTS,status);
        assertArrayEquals(evidence,Files.readAllBytes(actual.toPath()));
    }
}
