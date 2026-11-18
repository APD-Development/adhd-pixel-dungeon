package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Controlled ZIP stream: no huge allocation or real decompression is required to
 * reproduce getNextEntry automatically draining an ignored entry. */
public class ArchiveIgnoredEntryLimitTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    private static class IgnoredEntry extends ZipInputStream {
        final byte[] manifest=new ArchiveManifest().encode().getBytes(ArchiveManifest.utf8());
        final long ignoredSize;
        int entry=-1, manifestOffset;
        long consumed;
        IgnoredEntry(long size){super(new ByteArrayInputStream(new byte[0]));ignoredSize=size;}
        @Override public ZipEntry getNextEntry() throws IOException {
            if(entry==1){byte[] buffer=new byte[16384];while(read(buffer,0,buffer.length)>0){} }
            entry++;
            if(entry==0)return new ZipEntry(ArchiveManifest.ENTRY_NAME);
            if(entry==1)return new ZipEntry("unlisted-extra.bin");
            return null;
        }
        @Override public int read(byte[] buffer,int off,int len){
            if(len==0)return 0;
            if(entry==0){
                if(manifestOffset==manifest.length)return -1;
                int n=Math.min(len,manifest.length-manifestOffset);
                System.arraycopy(manifest,manifestOffset,buffer,off,n);manifestOffset+=n;return n;
            }
            if(entry==1){
                if(consumed==ignoredSize)return -1;
                int n=(int)Math.min(len,ignoredSize-consumed);consumed+=n;return n;
            }
            return -1;
        }
    }
    @Test public void ignoredEntryCannotBypassExistingPerFileLimit() throws Exception {
        IgnoredEntry zip=new IgnoredEntry(ArchiveImporter.MAX_FILE_BYTES+1024*1024);
        ArchiveImporter.Report report=ArchiveImporter.importZip(tmp.newFolder(),zip);
        assertFalse("unlisted decompressed payload must not evade file-size protections",report.ok);
        assertTrue(report.problem,report.problem.contains("too large"));
        assertTrue("stop at first read beyond the existing limit",zip.consumed<=ArchiveImporter.MAX_FILE_BYTES+16384);
        assertEquals(0,report.imported);
    }
    @Test public void smallIgnoredEntryKeepsExistingCompatibility() throws Exception {
        IgnoredEntry zip=new IgnoredEntry(1000);
        ArchiveImporter.Report report=ArchiveImporter.importZip(tmp.newFolder(),zip);
        assertTrue(report.summary(),report.ok);assertEquals(1000,zip.consumed);assertEquals(0,report.imported);
    }
    @Test public void zeroProgressIgnoredEntryStopsWithoutWritingHistory() throws Exception {
        IgnoredEntry zip=new IgnoredEntry(1000){
            @Override public int read(byte[] buffer,int off,int len){
                return entry==1 ? 0 : super.read(buffer,off,len);
            }
        };
        java.io.File root=tmp.newFolder();
        java.io.File existing=new java.io.File(root,"owner-statistics.txt");
        java.nio.file.Files.write(existing.toPath(),new byte[]{3,7,9});
        ArchiveImporter.Report report=ArchiveImporter.importZip(root,zip);
        assertFalse(report.ok);
        assertTrue(report.problem,report.problem.contains("damaged"));
        assertEquals(0,report.imported);
        assertArrayEquals(new byte[]{3,7,9},java.nio.file.Files.readAllBytes(existing.toPath()));
        assertFalse(new java.io.File(root,"import-tmp").exists());
    }
    @Test public void ignoredEntryAtExactLimitRemainsCompatible() throws Exception {
        IgnoredEntry zip=new IgnoredEntry(ArchiveImporter.MAX_FILE_BYTES);
        ArchiveImporter.Report report=ArchiveImporter.importZip(tmp.newFolder(),zip);
        assertTrue(report.summary(),report.ok);
        assertEquals(ArchiveImporter.MAX_FILE_BYTES,zip.consumed);
        assertEquals(0,report.imported);
    }
    @Test public void rejectedOversizedExtraPreservesExistingHistoryBytes() throws Exception {
        java.io.File root=tmp.newFolder();
        java.io.File runs=new java.io.File(root,"runs");assertTrue(runs.mkdir());
        java.io.File existing=new java.io.File(runs,"original.apdlog");
        byte[] bytes={11,22,33,44};java.nio.file.Files.write(existing.toPath(),bytes);
        ArchiveImporter.Report report=ArchiveImporter.importZip(root,
                new IgnoredEntry(ArchiveImporter.MAX_FILE_BYTES+1));
        assertFalse(report.ok);
        assertArrayEquals(bytes,java.nio.file.Files.readAllBytes(existing.toPath()));
        assertFalse(new java.io.File(root,"import-tmp").exists());
    }
}
