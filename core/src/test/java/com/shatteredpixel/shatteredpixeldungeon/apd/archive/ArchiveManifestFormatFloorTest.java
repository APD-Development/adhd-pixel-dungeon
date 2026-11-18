package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//A checksum proves the bytes are intact, not that an invented pre-version
//manifest is an APD history format supported by this importer.
public class ArchiveManifestFormatFloorTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private byte[] archiveWithManifest( ArchiveManifest manifest ) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(out);
		try {
			zip.putNextEntry(new ZipEntry(ArchiveManifest.ENTRY_NAME));
			zip.write(manifest.encode().getBytes(ArchiveManifest.utf8()));
			zip.closeEntry();
			zip.finish();
		} finally {
			zip.close();
		}
		return out.toByteArray();
	}

	@Test
	public void checksumValidNonpositiveFormatsAreRejected() {
		for (int version : new int[]{ 0, -1, Integer.MIN_VALUE }){
			ArchiveManifest manifest = new ArchiveManifest();
			manifest.format = version;
			try {
				ArchiveManifest.parse(manifest.encode().getBytes(ArchiveManifest.utf8()));
				fail("accepted checksum-valid unsupported archive format " + version);
			} catch (IllegalArgumentException expected){
				assertTrue(expected.getMessage(), expected.getMessage().contains("invalid format version"));
			}
		}
	}

	@Test
	public void nonpositiveManifestFormatStopsImportBeforeHistoryMutation() throws Exception {
		File root = temporaryFolder.newFolder("history");
		File runs = new File(root, "runs");
		assertTrue(runs.mkdir());
		File existing = new File(runs, "existing.apdlog");
		byte[] original = new byte[]{ 10, 20, 30, 40 };
		Files.write(existing.toPath(), original);

		for (int version : new int[]{ 0, -1 }){
			ArchiveManifest manifest = new ArchiveManifest();
			manifest.format = version;
			ArchiveImporter.Report result = ArchiveImporter.importArchive(root,
					new ByteArrayInputStream(archiveWithManifest(manifest)));
			assertFalse(result.summary(), result.ok);
			assertTrue(result.problem, result.problem.contains("invalid format version"));
			assertEquals(0, result.imported);
			assertArrayEquals("existing history must remain byte-for-byte unchanged", original,
					Files.readAllBytes(existing.toPath()));
			assertFalse("rejected import must not create a staging directory",
					new File(root, "import-tmp").exists());
		}
	}

	@Test
	public void currentFormatAndEmptyManifestRemainValid() {
		ArchiveManifest manifest = new ArchiveManifest();
		assertEquals(ArchiveManifest.FORMAT,
				ArchiveManifest.parse(manifest.encode().getBytes(ArchiveManifest.utf8())).format);
	}
}
