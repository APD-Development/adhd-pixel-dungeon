package com.shatteredpixel.shatteredpixeldungeon.apd.backend;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SyncAcknowledgementIdentityTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    private void retainsChangedRecord(boolean deleteFirst,boolean deleteSecond) throws Exception {
        SyncReplica r=new SyncReplica("device-a");r.put("settings","k","old",100);
        if(deleteFirst)r.delete("settings","k",100);
        SyncRecord sent=r.pending().get(0);
        if(deleteSecond)r.delete("settings","k",100);else r.put("settings","k","new",100);
        r.acknowledge(sent,5);
        assertEquals("Only the exact submitted content can be acknowledged",1,r.pending().size());
        assertEquals(deleteSecond,r.pending().get(0).deleted);
        assertEquals(deleteSecond?null:"new",r.get("settings","k"));
        File f=tmp.newFile();r.save(f);SyncReplica restored=new SyncReplica("device-a");assertEquals(0,restored.load(f));
        assertEquals(1,restored.pending().size());assertEquals(deleteSecond,restored.pending().get(0).deleted);
        assertEquals(deleteSecond?null:"new",restored.get("settings","k"));
        restored.acknowledge(restored.pending().get(0),6);assertTrue(restored.pending().isEmpty());
    }
    @Test public void anotherEditInTheSameMillisecondRemainsPending() throws Exception {retainsChangedRecord(false,false);}
    @Test public void deletionInTheSameMillisecondIsNotAcknowledgedByAnOldValue() throws Exception {retainsChangedRecord(false,true);}
    @Test public void replacingATombstoneInTheSameMillisecondRemainsPending() throws Exception {retainsChangedRecord(true,false);}
    @Test public void acknowledgementFromAnotherDeviceCannotClearThisDevicesEdit() {
        SyncReplica r=new SyncReplica("device-a");r.put("settings","k","same",100);
        r.acknowledge(new SyncRecord("settings","k",0,100,"device-b",false,"same"),5);
        assertEquals(1,r.pending().size());r.acknowledge(r.pending().get(0),6);assertTrue(r.pending().isEmpty());
    }
}
