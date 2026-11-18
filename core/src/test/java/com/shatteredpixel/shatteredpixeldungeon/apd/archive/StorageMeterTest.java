package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;

public class StorageMeterTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static void size( File f, long bytes ) throws Exception {
		f.getParentFile().mkdirs();
		RandomAccessFile r = new RandomAccessFile(f, "rw");
		r.setLength(bytes);
		r.close();
	}

	@Test
	public void itCountsEvidenceAndCachesSeparatelyAndNeverTouchesThem() throws Exception {
		File root = tmp.newFolder("apd");
		size(new File(root, "runs/a.apdlog"), 1000);
		size(new File(root, "runs/b.apdlog"), 2000);
		size(new File(root, "runs/b.apdlog.tail-5.damaged"), 30);
		size(new File(root, "legacy/import-1.apdlog"), 400);
		size(new File(root, "derived/rows/a.apdr"), 111);
		size(new File(root, "derived/facts.apdd"), 222);
		size(new File(root, "index.apdi"), 5);
		StorageMeter.Report r = StorageMeter.measure(root);
		assertEquals(2, r.runLogs);
		assertEquals(3000, r.runLogBytes);
		assertEquals(1, r.importedFiles);
		assertEquals(400, r.importedBytes);
		assertEquals(30, r.damagedTailBytes);
		assertEquals(3430, r.historyBytes());
		assertEquals(338, r.derivedBytes);
		assertEquals(3768, r.totalBytes());
		assertTrue(new File(root, "runs/a.apdlog").isFile());
		assertTrue(r.summary().contains("2 runs"));
	}

	@Test
	public void aNoRootOrAnEmptyRootIsFine(){
		assertEquals(0, StorageMeter.measure(null).runLogs);
		assertEquals(0, StorageMeter.measure(new File(tmp.getRoot(), "none")).runLogs);
	}

	@Test
	public void itWarnsAsTheHistoryGrowsButNeverLimitsAnything() throws Exception {
		File small = tmp.newFolder("small");
		size(new File(small, "runs/a.apdlog"), 1000);
		assertFalse(containsLarge(StorageMeter.measure(small)));
		File large = tmp.newFolder("large");
		size(new File(large, "runs/a.apdlog"), StorageMeter.LARGE_BYTES + 1);
		StorageMeter.Report r = StorageMeter.measure(large);
		assertTrue(r.warnings.toString(), containsLarge(r));
		File huge = tmp.newFolder("huge");
		size(new File(huge, "runs/a.apdlog"), StorageMeter.VERY_LARGE_BYTES + 1);
		assertTrue(StorageMeter.measure(huge).warnings.toString().contains("very large"));
	}

	private static boolean containsLarge( StorageMeter.Report r ){
		for (String w : r.warnings) if (w.contains("large")) return true;
		return false;
	}

	@Test
	public void derivedCacheSymlinkCycleAndExternalFilesDoNotInflateTotals() throws Exception {
		File root = tmp.newFolder("meter-caches");
		size(new File(root, "derived/rows/owned.apdr"), 17);
		File external = tmp.newFolder("meter-external-cache");
		File externalFile = new File(external, "outside.apdr");
		size(externalFile, 10000);
		File derived = new File(root, "derived");
		File loop = new File(derived, "loop");
		File outsideLink = new File(derived, "external");
		File indexLink = new File(root, "index.apdi");
		Files.createSymbolicLink(loop.toPath(), derived.toPath());
		Files.createSymbolicLink(outsideLink.toPath(), external.toPath());
		Files.createSymbolicLink(indexLink.toPath(), externalFile.toPath());

		try {
			StorageMeter.Report result = StorageMeter.measure(root);
			assertEquals(17, result.derivedBytes);
			assertEquals(17, result.totalBytes());
			assertTrue(Files.isSymbolicLink(loop.toPath()));
			assertTrue(Files.isSymbolicLink(outsideLink.toPath()));
			assertTrue(Files.isSymbolicLink(indexLink.toPath()));
			assertEquals(10000, externalFile.length());
		} finally {
			// Do not let TemporaryFolder cleanup follow the test's deliberate cycle.
			Files.deleteIfExists(loop.toPath());
			Files.deleteIfExists(outsideLink.toPath());
			Files.deleteIfExists(indexLink.toPath());
		}
	}

	@Test
	public void runAndLegacyAliasesAreNotReportedAsOwnedHistory() throws Exception {
		File root = tmp.newFolder("meter-logs");
		size(new File(root, "runs/real.apdlog"), 21);
		size(new File(root, "runs/real.apdlog.tail-1.damaged"), 3);
		size(new File(root, "legacy/real.apdlog"), 8);
		File external = tmp.newFolder("meter-external-logs");
		File externalLog = new File(external, "outside.apdlog");
		size(externalLog, 1000);
		Files.createSymbolicLink(new File(root, "runs/linked.apdlog").toPath(), externalLog.toPath());
		Files.createSymbolicLink(new File(root, "legacy/linked.apdlog").toPath(), externalLog.toPath());
		Files.createSymbolicLink(new File(root, "runs/linked.tail-1.damaged").toPath(), externalLog.toPath());

		StorageMeter.Report result = StorageMeter.measure(root);
		assertEquals(1, result.runLogs);
		assertEquals(21, result.runLogBytes);
		assertEquals(3, result.damagedTailBytes);
		assertEquals(1, result.importedFiles);
		assertEquals(8, result.importedBytes);
		assertEquals(32, result.historyBytes());
		assertEquals(1000, externalLog.length());

		File aliasRoot = tmp.newFolder("meter-aliased-folder");
		Files.createSymbolicLink(new File(aliasRoot, "runs").toPath(), external.toPath());
		assertEquals(0, StorageMeter.measure(aliasRoot).runLogs);
		assertTrue(new File(aliasRoot, "runs").isDirectory());
	}

	@Test
	public void canonicalStorageRootCanItselfBeASymlink() throws Exception {
		File original = tmp.newFolder("meter-real-root");
		size(new File(original, "runs/owned.apdlog"), 41);
		size(new File(original, "legacy/owned.apdlog"), 11);
		size(new File(original, "derived/owned.apdr"), 7);
		size(new File(original, "index.apdi"), 5);
		File alias = new File(tmp.getRoot(), "meter-root-alias");
		Files.createSymbolicLink(alias.toPath(), original.toPath());

		StorageMeter.Report result = StorageMeter.measure(alias);
		assertEquals(1, result.runLogs);
		assertEquals(41, result.runLogBytes);
		assertEquals(1, result.importedFiles);
		assertEquals(11, result.importedBytes);
		assertEquals(12, result.derivedBytes);
		assertEquals(64, result.totalBytes());
	}

	@Test
	public void sizesAreInWords(){
		assertEquals("512 B", Size.text(512));
		assertEquals("1.5 KB", Size.text(1536));
		assertEquals("2.0 MB", Size.text(2L * 1024 * 1024));
		assertEquals("3.0 GB", Size.text(3L * 1024 * 1024 * 1024));
	}
}
