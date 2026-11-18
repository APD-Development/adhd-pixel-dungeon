package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

// Real-filesystem export boundaries; the importer already has its own separate link guards.
public class ArchiveExporterSymlinkTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void linkedRunAndLegacyLogsCannotExportExternalHistory() throws Exception {
		for (String folder : new String[]{"runs", "legacy"}){
			File root = tmp.newFolder("file-history-" + folder);
			ArchiveSupport.history(root);
			File original = new File(root, folder + "/" + (folder.equals("runs") ? "fin" : "import-1") + ".apdlog");
			byte[] originalBytes = Files.readAllBytes(original.toPath());
			File outside = new File(tmp.newFolder("external-log-" + folder), "outside.apdlog");
			Files.write(outside.toPath(), originalBytes);
			File link = new File(root, folder + "/linked-history.apdlog");
			Files.createSymbolicLink(link.toPath(), outside.toPath());

			ByteArrayOutputStream refusedBytes = new ByteArrayOutputStream();
			ArchiveExporter.Report refused = ArchiveExporter.export(root, refusedBytes, "test", 7);
			assertFalse(refused.summary(), refused.ok);
			assertTrue(refused.problem, refused.problem.contains("outside its designated path"));
			assertEquals("do not even start a misleading partial ZIP", 0, refusedBytes.size());
			assertTrue(Files.isSymbolicLink(link.toPath()));
			assertArrayEquals(originalBytes, Files.readAllBytes(outside.toPath()));
			assertArrayEquals(originalBytes, Files.readAllBytes(original.toPath()));

			Files.delete(link.toPath());
			ByteArrayOutputStream acceptedBytes = new ByteArrayOutputStream();
			ArchiveExporter.Report accepted = ArchiveExporter.export(root, acceptedBytes, "test", 7);
			assertTrue(accepted.summary(), accepted.ok);
			assertTrue("unlinked original history still exports", acceptedBytes.size() > 0);
		}
	}

	@Test
	public void linkedRunAndLegacyDirectoriesCannotExportOutsideHistoryRoot() throws Exception {
		File original = tmp.newFolder("real-history");
		ArchiveSupport.history(original);
		for (String folder : new String[]{"runs", "legacy"}){
			File root = tmp.newFolder("folder-history-" + folder);
			File link = new File(root, folder);
			Files.createSymbolicLink(link.toPath(), new File(original, folder).toPath());
			byte[] witness = Files.readAllBytes(new File(original, folder + "/" +
					(folder.equals("runs") ? "fin" : "import-1") + ".apdlog").toPath());
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			ArchiveExporter.Report refused = ArchiveExporter.export(root, bytes, "test", 9);
			assertFalse(refused.summary(), refused.ok);
			assertTrue(refused.problem, refused.problem.contains("outside its designated path"));
			assertEquals(0, bytes.size());
			assertTrue(Files.isSymbolicLink(link.toPath()));
			assertArrayEquals(witness, Files.readAllBytes(new File(original, folder + "/" +
					(folder.equals("runs") ? "fin" : "import-1") + ".apdlog").toPath()));
		}
	}

	@Test
	public void callerSuppliedCanonicalRootAliasStillExportsNormalHistory() throws Exception {
		File root = tmp.newFolder("actual-root");
		ArchiveSupport.history(root);
		File alias = new File(tmp.getRoot(), "alias-root");
		Files.createSymbolicLink(alias.toPath(), root.toPath());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ArchiveExporter.Report report = ArchiveExporter.export(alias, out, "test", 11);
		assertTrue(report.summary(), report.ok);
		assertEquals(5, report.runLogs);
		assertEquals(1, report.importedFiles);
		assertTrue(out.size() > 0);
	}
}
