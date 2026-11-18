package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// Direct-file safety in the desktop/fallback ZIP picker. Android uses its own SAF picker.
public class ApdFilesFallbackSymlinkTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final class Selection {
        byte[] bytes;
        String failure;
    }

    private Selection select(File base) {
        Selection selected = new Selection();
        new ApdFiles.FolderProvider(base).chooseImport(new ApdFiles.ImportSource() {
            @Override
            public void onStream(InputStream in, String displayName) {
                try (InputStream stream = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[256];
                    int read;
                    while ((read = stream.read(buffer)) != -1) out.write(buffer, 0, read);
                    selected.bytes = out.toByteArray();
                } catch (IOException unreadable) {
                    selected.failure = unreadable.toString();
                }
            }

            @Override public void onCancelled() { selected.failure = "unexpected cancellation"; }
            @Override public void onFailed(String why) { selected.failure = why; }
        });
        return selected;
    }

    @Test
    public void newerExternalZipSymlinkCannotOutrankLocalZip() throws Exception {
        File base = temporaryFolder.newFolder("external-alias");
        File imports = new File(base, "imports");
        assertTrue(imports.mkdir());
        File local = new File(imports, "a-local.zip");
        File outside = temporaryFolder.newFile("outside.zip");
        Files.write(local.toPath(), new byte[]{11});
        Files.write(outside.toPath(), new byte[]{22});
        assertTrue(local.setLastModified(1700000000000L));
        assertTrue(outside.setLastModified(1700000100000L));
        Files.createSymbolicLink(new File(imports, "z-external.zip").toPath(), outside.toPath());

        Selection selected = select(base);
        assertNull("valid local ZIP remains selectable", selected.failure);
        assertArrayEquals("a newer alias must not import bytes from outside imports/", new byte[]{11}, selected.bytes);
    }

    @Test
    public void externalZipSymlinkAloneDoesNotCountAsImportFile() throws Exception {
        File base = temporaryFolder.newFolder("alias-only");
        File imports = new File(base, "imports");
        assertTrue(imports.mkdir());
        File outside = temporaryFolder.newFile("only-outside.zip");
        Files.write(outside.toPath(), new byte[]{33});
        Files.createSymbolicLink(new File(imports, "alias.zip").toPath(), outside.toPath());

        Selection selected = select(base);
        assertNull("an external alias must not supply data", selected.bytes);
        assertNotNull("no direct ZIP must report the normal missing-archive result", selected.failure);
        assertTrue(selected.failure.contains("put an archive"));
    }

    @Test
    public void symlinkedBaseRootStillAllowsDirectLocalZip() throws Exception {
        File realBase = temporaryFolder.newFolder("real-base");
        File imports = new File(realBase, "imports");
        assertTrue(imports.mkdir());
        Files.write(new File(imports, "local.zip").toPath(), new byte[]{44});
        File aliasBase = new File(temporaryFolder.getRoot(), "linked-base");
        Files.createSymbolicLink(aliasBase.toPath(), realBase.toPath());

        Selection selected = select(aliasBase);
        assertNull("a symlinked storage root remains supported", selected.failure);
        assertArrayEquals(new byte[]{44}, selected.bytes);
    }
    @Test
    public void symlinkedImportsDirectoryCannotReadExternalZip() throws Exception {
        File base = temporaryFolder.newFolder("import-directory-alias");
        File external = temporaryFolder.newFolder("unrelated-data");
        File outside = new File(external, "external.zip");
        Files.write(outside.toPath(), new byte[]{55, 66});
        Files.createSymbolicLink(new File(base, "imports").toPath(), external.toPath());

        Selection selected = select(base);
        assertNull("aliased imports/ must not hand an external ZIP to the importer", selected.bytes);
        assertNotNull("aliased directory must report a failure", selected.failure);
        assertTrue(selected.failure.contains("unsafe import directory"));
        assertArrayEquals("external data must remain untouched", new byte[]{55, 66}, Files.readAllBytes(outside.toPath()));
    }

}
