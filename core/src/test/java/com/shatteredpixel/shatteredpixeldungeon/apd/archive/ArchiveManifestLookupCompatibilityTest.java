package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveSupport.*;

public class ArchiveManifestLookupCompatibilityTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    @Test public void repeatedManifestPathKeepsFirstMatchAndNeverOverwritesHistory() throws Exception {
        byte[] log=smallLog(tmp.newFolder(),"a","run-a");
        ArchiveManifest manifest=new ArchiveManifest();
        ArchiveManifest.Entry first=new ArchiveManifest.Entry();
        first.path="runs/a.apdlog";first.size=log.length;
        first.sha256=ArchiveManifest.sha256Hex(log);first.runId="run-a";manifest.files.add(first);
        ArchiveManifest.Entry later=new ArchiveManifest.Entry();
        later.path=first.path;later.size=0;later.sha256=ArchiveManifest.sha256Hex(new byte[0]);
        later.runId=first.runId;manifest.files.add(later);
        byte[] archive=zip(Arrays.asList(ArchiveManifest.ENTRY_NAME,first.path),
                Arrays.asList(manifest.encode().getBytes(ArchiveManifest.utf8()),log));
        File root=tmp.newFolder();
        ArchiveImporter.Report imported=ArchiveImporter.importArchive(root,new ByteArrayInputStream(archive));
        assertTrue(imported.summary(),imported.ok);assertEquals(1,imported.imported);
        assertArrayEquals(log,Files.readAllBytes(new File(root,first.path).toPath()));
        ArchiveImporter.Report repeated=ArchiveImporter.importArchive(root,new ByteArrayInputStream(archive));
        assertTrue(repeated.summary(),repeated.ok);assertEquals(0,repeated.imported);
        assertEquals(1,repeated.skippedAlreadyHere);
        assertArrayEquals(log,Files.readAllBytes(new File(root,first.path).toPath()));
    }
}
