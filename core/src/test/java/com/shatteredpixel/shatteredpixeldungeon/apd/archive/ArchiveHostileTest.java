package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveSupport.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//An archive is a file from outside: a damaged, tampered or hostile one must never write outside the history, never replace anything,
//never crash, and never leave half-written files behind.
public class ArchiveHostileTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File root;

	private ArchiveImporter.Report run( byte[] archive ) throws Exception {
		root = tmp.newFolder("device-" + System.nanoTime());
		return ArchiveImporter.importArchive(root, new ByteArrayInputStream(archive));
	}

	private void assertNothingWritten() throws Exception {
		Map<String, byte[]> f = files(root);
		for (String k : f.keySet()) assertFalse("left behind: " + k, k.startsWith("runs/") || k.startsWith("legacy/") || k.startsWith("import-tmp"));
		assertFalse(new File(root, "import-tmp").exists());
	}

	private byte[] good( String id, String runId ) throws Exception {
		return smallLog(tmp.newFolder("scratch-" + id), id, runId);
	}

	@Test
	public void garbageAndEmptyInputsAreRefusedWithoutThrowing() throws Exception {
		for (byte[] bad : new byte[][]{ new byte[0], "not a zip at all".getBytes("UTF-8"), new byte[]{ 'P', 'K', 3, 4, 0, 0 }, new byte[2000] }){
			ArchiveImporter.Report r = run(bad);
			assertFalse(r.ok);
			assertEquals(0, r.imported);
			assertNothingWritten();
		}
	}

	@Test
	public void aSymlinkedHistoryDirectoryCannotSendImportedLogsOutsideTheRoot() throws Exception {
		byte[] log = good("a", "run-a");
		File history = tmp.newFolder("symlink-history");
		File external = tmp.newFolder("symlink-external");
		File marker = new File(external, "existing.txt");
		byte[] original = "keep untouched".getBytes("UTF-8");
		Files.write(marker.toPath(), original);
		Files.createSymbolicLink(new File(history, "runs").toPath(), external.toPath());

		Map<String, byte[]> logs = new LinkedHashMap<>();
		logs.put("runs/a.apdlog", log);
		byte[] archive = archiveOf(logs, java.util.Collections.singletonMap("runs/a.apdlog", "run-a"));
		ArchiveImporter.Report result = ArchiveImporter.importArchive(history, new ByteArrayInputStream(archive));
		assertFalse(result.summary(), result.ok);
		assertEquals(0, result.imported);
		assertFalse("symlinked folder must not receive a log", new File(external, "a.apdlog").exists());
		assertArrayEquals(original, Files.readAllBytes(marker.toPath()));
		assertFalse(new File(history, "import-tmp").exists());
	}

	@Test
	public void anExistingSymlinkedLogCannotBeReadAsAlreadyImported() throws Exception {
		byte[] log = good("a", "run-a");
		File history = tmp.newFolder("symlink-log-history");
		File runs = new File(history, "runs");
		assertTrue(runs.mkdir());
		File external = tmp.newFolder("symlink-log-external");
		File externalLog = new File(external, "outside.apdlog");
		Files.write(externalLog.toPath(), log);
		Files.createSymbolicLink(new File(runs, "a.apdlog").toPath(), externalLog.toPath());

		Map<String, byte[]> logs = new LinkedHashMap<>();
		logs.put("runs/a.apdlog", log);
		byte[] archive = archiveOf(logs, java.util.Collections.singletonMap("runs/a.apdlog", "run-a"));
		ArchiveImporter.Report result = ArchiveImporter.importArchive(history, new ByteArrayInputStream(archive));
		assertFalse(result.summary(), result.ok);
		assertEquals(0, result.imported);
		assertEquals("a symlink is not an already-imported history log", 0, result.skippedAlreadyHere);
		assertArrayEquals(log, Files.readAllBytes(externalLog.toPath()));
		assertTrue("the original link remains untouched", Files.isSymbolicLink(new File(runs, "a.apdlog").toPath()));
		assertFalse(new File(history, "import-tmp").exists());
	}


	@Test
	public void importTempDirectorySymlinkDoesNotDeleteExternalFiles() throws Exception {
		byte[] log = good("a", "run-a");
		File history = tmp.newFolder("temp-link-history");
		File external = tmp.newFolder("temp-link-external");
		File marker = new File(external, "precious.txt");
		byte[] original = "original external content".getBytes("UTF-8");
		Files.write(marker.toPath(), original);
		Files.createSymbolicLink(new File(history, "import-tmp").toPath(), external.toPath());
		Map<String, byte[]> logs = new LinkedHashMap<>();
		logs.put("runs/a.apdlog", log);
		ArchiveImporter.Report result = ArchiveImporter.importArchive(history, new ByteArrayInputStream(
				archiveOf(logs, java.util.Collections.singletonMap("runs/a.apdlog", "run-a"))));
		assertTrue(result.summary(), result.ok);
		assertEquals(1, result.imported);
		assertArrayEquals(original, Files.readAllBytes(marker.toPath()));
		assertTrue(new File(history, "runs/a.apdlog").isFile());
		assertFalse(new File(history, "import-tmp").exists());
	}

	@Test
	public void nestedStaleImportTempSymlinkDoesNotDeleteExternalFiles() throws Exception {
		byte[] log = good("a", "run-a");
		File history = tmp.newFolder("nested-temp-history");
		File scratch = new File(history, "import-tmp");
		assertTrue(scratch.mkdir());
		Files.write(new File(scratch, "leftover.tmp").toPath(), "stale".getBytes("UTF-8"));
		File external = tmp.newFolder("nested-temp-external");
		File marker = new File(external, "keep.txt");
		byte[] original = "must survive import cleanup".getBytes("UTF-8");
		Files.write(marker.toPath(), original);
		Files.createSymbolicLink(new File(scratch, "external").toPath(), external.toPath());
		Map<String, byte[]> logs = new LinkedHashMap<>();
		logs.put("runs/a.apdlog", log);
		ArchiveImporter.Report result = ArchiveImporter.importArchive(history, new ByteArrayInputStream(
				archiveOf(logs, java.util.Collections.singletonMap("runs/a.apdlog", "run-a"))));
		assertTrue(result.summary(), result.ok);
		assertEquals(1, result.imported);
		assertArrayEquals(original, Files.readAllBytes(marker.toPath()));
		assertTrue(new File(history, "runs/a.apdlog").isFile());
		assertFalse(scratch.exists());
	}

	@Test
	public void aZipWithoutAManifestFirstIsRefused() throws Exception {
		byte[] log = good("a", "run-a");
		ArchiveImporter.Report r = run(zip(names("runs/a.apdlog"), Arrays.asList(log)));
		assertFalse(r.ok);
		assertNothingWritten();
	}

	@Test
	public void aTamperedManifestStopsEverythingBeforeAnythingIsWritten() throws Exception {
		byte[] log = good("a", "run-a");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/a.apdlog", log);
		byte[] archive = archiveOf(files, java.util.Collections.singletonMap("runs/a.apdlog", "run-a"));
		Map<String, byte[]> parts = new ArchiveRoundTripEntries().read(archive);
		//change one character of the manifest text (a size digit), keeping the old checksum line
		String text = new String(parts.get(ArchiveManifest.ENTRY_NAME), "UTF-8");
		String tamperedText = text.replace("run-a", "run-b");
		ArchiveImporter.Report r = run(zip(names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"), Arrays.asList(tamperedText.getBytes("UTF-8"), log)));
		assertFalse(r.summary(), r.ok);
		assertEquals(0, r.imported);
		assertNothingWritten();
	}

	private static int indexOf( byte[] data, byte[] t ){
		outer:
		for (int i = 0; i + t.length <= data.length; i++){
			for (int j = 0; j < t.length; j++) if (data[i + j] != t[j]) continue outer;
			return i;
		}
		throw new IllegalStateException("marker not found");
	}

	@Test
	public void aManifestThatListsAPathOutsideTheHistoryIsRefusedWhole() throws Exception {
		byte[] log = good("a", "run-a");
		for (String evil : new String[]{ "../evil.apdlog", "runs/../../evil.apdlog", "/etc/evil.apdlog", "derived/rows/x.apdr", "index.apdi", "runs/sub/x.apdlog" }){
			ArchiveManifest m = new ArchiveManifest();
			ArchiveManifest.Entry e = new ArchiveManifest.Entry();
			e.path = evil;
			e.size = log.length;
			e.sha256 = ArchiveManifest.sha256Hex(log);
			e.runId = "run-a";
			m.files.add(e);
			byte[] archive = zip(names(ArchiveManifest.ENTRY_NAME, evil), Arrays.asList(m.encode().getBytes(ArchiveManifest.utf8()), log));
			ArchiveImporter.Report r = run(archive);
			assertFalse(evil, r.ok);
			assertEquals(evil, 0, r.imported);
			assertNothingWritten();
			assertFalse(new File(root.getParentFile(), "evil.apdlog").exists());
			assertFalse(new File("/etc/evil.apdlog").exists());
		}
	}

	@Test
	public void anEntryTheManifestDoesNotListIsIgnoredEvenWithAnInnocentName() throws Exception {
		byte[] a = good("a", "run-a"), extra = good("extra", "run-extra");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/a.apdlog", a);
		byte[] base = archiveOf(files, java.util.Collections.singletonMap("runs/a.apdlog", "run-a"));
		//re-zip the same entries and add one more that the manifest never mentions
		List<String> n = new ArrayList<>(names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog", "runs/extra.apdlog", "../../outside.txt"));
		Map<String, byte[]> parts = new ArchiveRoundTripEntries().read(base);
		ArchiveImporter.Report r = run(zip(n, Arrays.asList(parts.get(ArchiveManifest.ENTRY_NAME), a, extra, "x".getBytes("UTF-8"))));
		assertTrue(r.summary(), r.ok);
		assertEquals(1, r.imported);
		assertEquals("unlisted entries are ignored, they do not spoil the rest", 0, r.rejected);
		assertTrue(new File(root, "runs/a.apdlog").isFile());
		assertFalse(new File(root, "runs/extra.apdlog").exists());
		assertFalse(new File(root.getParentFile(), "outside.txt").exists());
	}

	@Test
	public void oneFileThatDoesNotMatchItsChecksumMeansNothingAtAllIsImported() throws Exception {
		byte[] a = good("a", "run-a"), b = good("b", "run-b");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/a.apdlog", a);
		files.put("runs/b.apdlog", b);
		Map<String, String> ids = new LinkedHashMap<>();
		ids.put("runs/a.apdlog", "run-a");
		ids.put("runs/b.apdlog", "run-b");
		byte[] archive = archiveOf(files, ids);
		Map<String, byte[]> parts = new ArchiveRoundTripEntries().read(archive);
		byte[] bad = a.clone();
		bad[bad.length - 9] ^= 0x55;
		ArchiveImporter.Report r = run(zip(names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog", "runs/b.apdlog"),
				Arrays.asList(parts.get(ArchiveManifest.ENTRY_NAME), bad, b)));
		assertFalse(r.ok);
		assertEquals(0, r.imported);
		assertEquals(1, r.rejected);
		assertNothingWritten();   //b was perfectly good, and is not imported either
	}

	@Test
	public void aFileOfTheWrongSizeIsDropped() throws Exception {
		byte[] a = good("a", "run-a");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/a.apdlog", a);
		byte[] archive = archiveOf(files, java.util.Collections.singletonMap("runs/a.apdlog", "run-a"));
		Map<String, byte[]> parts = new ArchiveRoundTripEntries().read(archive);
		byte[] longer = Arrays.copyOf(a, a.length + 10);
		ArchiveImporter.Report shorter = run(zip(names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"), Arrays.asList(parts.get(ArchiveManifest.ENTRY_NAME), Arrays.copyOf(a, a.length - 3))));
		assertEquals(1, shorter.rejected);
		ArchiveImporter.Report bigger = run(zip(names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"), Arrays.asList(parts.get(ArchiveManifest.ENTRY_NAME), longer)));
		assertEquals(1, bigger.rejected);
		assertNothingWritten();
	}

	@Test
	public void aFileWhoseHeaderNamesAnotherRunIsDropped() throws Exception {
		byte[] a = good("a", "run-actually-a");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/a.apdlog", a);
		ArchiveImporter.Report r = run(archiveOf(files, java.util.Collections.singletonMap("runs/a.apdlog", "run-says-something-else")));
		assertEquals(1, r.rejected);
		assertEquals(0, r.imported);
		assertNothingWritten();
	}

	@Test
	public void somethingThatIsNotALogIsDropped() throws Exception {
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/junk.apdlog", "this is definitely not an APD log".getBytes("UTF-8"));
		ArchiveImporter.Report r = run(archiveOf(files, java.util.Collections.<String, String>emptyMap()));
		assertEquals(1, r.rejected);
		assertNothingWritten();
	}

	@Test
	public void aHugeEntryIsNeverBufferedOrWritten() throws Exception {
		//declared tiny, actually 70 MB of zeros (a few dozen KB zipped): the import stops reading it and writes nothing
		byte[] a = good("a", "run-a");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/a.apdlog", a);
		byte[] archive = archiveOf(files, java.util.Collections.singletonMap("runs/a.apdlog", "run-a"));
		Map<String, byte[]> parts = new ArchiveRoundTripEntries().read(archive);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipOutputStream z = new ZipOutputStream(out);
		z.putNextEntry(new ZipEntry(ArchiveManifest.ENTRY_NAME));
		z.write(parts.get(ArchiveManifest.ENTRY_NAME));
		z.closeEntry();
		z.putNextEntry(new ZipEntry("runs/a.apdlog"));
		byte[] zeros = new byte[1 << 20];
		for (int i = 0; i < 70; i++) z.write(zeros);
		z.closeEntry();
		z.finish();
		long start = System.currentTimeMillis();
		ArchiveImporter.Report r = run(out.toByteArray());
		assertTrue("took " + (System.currentTimeMillis() - start) + " ms", System.currentTimeMillis() - start < 30000);
		assertFalse(r.ok);
		assertEquals(0, r.imported);
		assertNothingWritten();
	}

	@Test
	public void aTruncatedArchiveChangesNothingAndLeavesNoLitter() throws Exception {
		byte[] a = good("a", "run-a"), b = good("b", "run-b");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/a.apdlog", a);
		files.put("runs/b.apdlog", b);
		Map<String, String> ids = new LinkedHashMap<>();
		ids.put("runs/a.apdlog", "run-a");
		ids.put("runs/b.apdlog", "run-b");
		byte[] archive = archiveOf(files, ids);
		for (int cut : new int[]{ archive.length - 5, archive.length / 2, archive.length / 3, 60, 10 }){
			ArchiveImporter.Report r = run(Arrays.copyOf(archive, cut));
			if (r.ok){
				//only the zip's table of contents at the very end was cut: every file arrived whole and verified, so all of it is imported
				assertEquals("cut " + cut, 2, r.imported);
				assertArrayEquals(a, files(root).get("runs/a.apdlog"));
				assertArrayEquals(b, files(root).get("runs/b.apdlog"));
			} else {
				assertEquals("cut " + cut, 0, r.imported);
				assertNothingWritten();
			}
			assertFalse(new File(root, "import-tmp").exists());
		}
	}

	@Test
	public void aCommitThatFailsHalfWayTakesBackEverythingItPlaced() throws Exception {
		//the second log's folder cannot be created (a plain file is in the way), so putting the files in place fails after the first one
		byte[] a = good("a", "run-a"), b = good("b", "run-b");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("legacy/a.apdlog", a);
		files.put("runs/b.apdlog", b);
		Map<String, String> ids = new LinkedHashMap<>();
		ids.put("legacy/a.apdlog", "run-a");
		ids.put("runs/b.apdlog", "run-b");
		byte[] archive = archiveOf(files, ids);
		root = tmp.newFolder("device-block");
		java.nio.file.Files.write(new File(root, "runs").toPath(), new byte[]{ 1 });   //"runs" is a file, so runs/b.apdlog cannot be created
		ArchiveImporter.Report r = ArchiveImporter.importArchive(root, new ByteArrayInputStream(archive));
		assertFalse(r.ok);
		assertEquals(0, r.imported);
		assertFalse("the first file was taken out again", new File(root, "legacy/a.apdlog").exists());
		assertFalse(new File(root, "import-tmp").exists());
	}

	@Test
	public void aNewerArchiveFormatIsRefused() throws Exception {
		ArchiveManifest m = new ArchiveManifest();
		m.format = 7;
		ArchiveImporter.Report r = run(zip(names(ArchiveManifest.ENTRY_NAME), Arrays.asList(m.encode().getBytes(ArchiveManifest.utf8()))));
		assertFalse(r.ok);
		assertTrue(r.problem, r.problem.contains("newer"));
		assertNothingWritten();
	}

	@Test
	public void checksumValidManifestSizeOverflowCannotTouchHistory() throws Exception {
		ArchiveManifest m = new ArchiveManifest();
		ArchiveManifest.Entry first = new ArchiveManifest.Entry();
		first.path = "runs/one.apdlog";
		first.size = Long.MAX_VALUE;
		m.files.add(first);
		ArchiveManifest.Entry second = new ArchiveManifest.Entry();
		second.path = "runs/two.apdlog";
		second.size = 1;
		m.files.add(second);
		byte[] archive = zip(names(ArchiveManifest.ENTRY_NAME),
				Arrays.asList(m.encode().getBytes(ArchiveManifest.utf8())));
		ArchiveImporter.Report result = run(archive);
		assertFalse(result.summary(), result.ok);
		assertEquals(0, result.imported);
		assertTrue(result.summary(), result.problem.contains("file sizes overflow"));
		assertNothingWritten();
	}

	@Test
	public void aListedFileThatNeverArrivedIsReportedAndNothingIsImported() throws Exception {
		byte[] a = good("a", "run-a"), b = good("b", "run-b");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/a.apdlog", a);
		files.put("runs/b.apdlog", b);
		Map<String, String> ids = new LinkedHashMap<>();
		ids.put("runs/a.apdlog", "run-a");
		ids.put("runs/b.apdlog", "run-b");
		Map<String, byte[]> parts = new ArchiveRoundTripEntries().read(archiveOf(files, ids));
		ArchiveImporter.Report r = run(zip(names(ArchiveManifest.ENTRY_NAME, "runs/a.apdlog"), Arrays.asList(parts.get(ArchiveManifest.ENTRY_NAME), a)));
		assertFalse(r.ok);
		assertEquals(0, r.imported);
		assertEquals(1, r.rejected);
		assertTrue(r.summary().contains("missing"));
		assertNothingWritten();
	}

	// ---- synthetic demo data never travels ----

	@Test
	public void demoDataIsNeverExportedAndNeverImported() throws Exception {
		File a = tmp.newFolder("real");
		history(a);
		//a demo-named log that ended up among real logs
		java.nio.file.Files.copy(new File(a, "runs/fin.apdlog").toPath(), new File(a, "runs/demo-000.apdlog").toPath());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ArchiveExporter.Report r = ArchiveExporter.export(a, out, "t", 1);
		assertTrue(r.ok);
		assertEquals(1, r.skippedDemo);
		assertFalse(new ArchiveRoundTripEntries().read(out.toByteArray()).containsKey("runs/demo-000.apdlog"));

		//a folder that carries the demo marker can be neither exported nor imported into
		File demo = tmp.newFolder("apd-demo-data");
		new File(demo, DatasetStore.DEMO_MARKER_FILE).createNewFile();
		history(demo);
		ArchiveExporter.Report refused = ArchiveExporter.export(demo, new ByteArrayOutputStream(), "t", 1);
		assertFalse(refused.ok);
		ArchiveImporter.Report into = ArchiveImporter.importArchive(demo, new ByteArrayInputStream(out.toByteArray()));
		assertFalse(into.ok);

		//an archive that holds demo data is refused whole
		byte[] demoLog = good("demo-007", "demo-007");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/demo-007.apdlog", demoLog);
		ArchiveImporter.Report bad = run(archiveOf(files, java.util.Collections.singletonMap("runs/demo-007.apdlog", "demo-007")));
		assertFalse(bad.ok);
		assertNothingWritten();
	}

	@Test
	public void aLogMarkedAsSyntheticInItsHeaderIsDroppedEvenWithAPlainName() throws Exception {
		File scratch = tmp.newFolder("scratch-demo");
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter w = com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter.create(
				new File(new File(scratch, "runs"), "plain.apdlog"), ArchiveSupport.V, "plain", "SYNTHETIC-DEMO-DATA", 5);
		w.event(1).unsigned(1).emit();
		w.close(true);
		byte[] bytes = java.nio.file.Files.readAllBytes(new File(scratch, "runs/plain.apdlog").toPath());
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/plain.apdlog", bytes);
		ArchiveImporter.Report r = run(archiveOf(files, java.util.Collections.<String, String>emptyMap()));
		assertEquals(1, r.rejected);
		assertNothingWritten();
	}

	//reads the entries of an archive (the tests' own tool)
	static final class ArchiveRoundTripEntries {
		Map<String, byte[]> read( byte[] archive ) throws Exception {
			Map<String, byte[]> out = new LinkedHashMap<>();
			java.util.zip.ZipInputStream z = new java.util.zip.ZipInputStream(new ByteArrayInputStream(archive));
			ZipEntry e;
			while ((e = z.getNextEntry()) != null){
				ByteArrayOutputStream o = new ByteArrayOutputStream();
				byte[] buf = new byte[4096];
				int n;
				while ((n = z.read(buf)) > 0) o.write(buf, 0, n);
				out.put(e.getName(), o.toByteArray());
			}
			return out;
		}
	}

	//Android 5's ZipInputStream answers a read of a CUT entry with 0 bytes (not -1), forever; and its getNextEntry() then skips the
	//rest of the entry by reading until it gets -1, which never comes: the import thread spun for ever, holding the importer's lock,
	//so every later import hung too (seen on the API 21 emulator). A zero-byte answer to a read that asked for bytes breaks the stream
	//contract and means the archive is cut or damaged: the import must stop right there, never call getNextEntry() after it.
	private static final class StallsAfter extends java.util.zip.ZipInputStream {
		private final int limit;
		private int served;
		boolean stalled;

		StallsAfter( InputStream in, int limit ){
			super(in);
			this.limit = limit;
		}

		@Override
		public int read( byte[] b, int off, int len ) throws java.io.IOException {
			if (served >= limit){
				stalled = true;
				return 0;
			}
			int n = super.read(b, off, Math.min(len, limit - served));
			if (n > 0) served += n;
			return n;
		}

		@Override
		public ZipEntry getNextEntry() throws java.io.IOException {
			if (stalled) throw new AssertionError("getNextEntry() after a stalled read: old Android's closeEntry() would spin here for ever");
			return super.getNextEntry();
		}
	}

	@Test
	public void aStreamThatStallsMidEntryStopsTheImportInsteadOfSpinning() throws Exception {
		byte[] log = good("a", "run-a");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/a.apdlog", log);
		byte[] archive = archiveOf(files, java.util.Collections.singletonMap("runs/a.apdlog", "run-a"));
		int manifestBytes = new ArchiveRoundTripEntries().read(archive).get(ArchiveManifest.ENTRY_NAME).length;
		assertTrue(log.length > 100);

		root = tmp.newFolder("device-stall");
		StallsAfter zip = new StallsAfter(new ByteArrayInputStream(archive), manifestBytes + 50);
		ArchiveImporter.Report r = ArchiveImporter.importZip(root, zip);
		assertTrue("the read did stall", zip.stalled);
		assertFalse(r.summary(), r.ok);
		assertEquals(0, r.imported);
		assertFalse("stopped for the right reason: " + r.problem, r.problem.contains("AssertionError"));
		assertTrue(r.problem, r.problem.contains("cut short"));
		assertNothingWritten();
		//and the importer is free again for the next archive
		ArchiveImporter.Report again = ArchiveImporter.importArchive(tmp.newFolder("device-after-stall"), new ByteArrayInputStream(archive));
		assertTrue(again.summary(), again.ok);
	}

	//The raw source can stop answering too (a file picker's stream, a damaged file): it gives 0 bytes for ever instead of data or -1.
	//Nothing in the zip classes copes with that, and the import must neither spin nor keep its lock.
	private static final class StallingSource extends InputStream {
		private final byte[] data;
		private final int limit;
		private int pos;

		StallingSource( byte[] data, int limit ){
			this.data = data;
			this.limit = limit;
		}

		@Override
		public int read() {
			return pos < limit ? (data[pos++] & 0xff) : -1;
		}

		@Override
		public int read( byte[] b, int off, int len ) {
			if (len == 0) return 0;
			if (pos >= limit) return 0;   //the stall
			int n = Math.min(len, limit - pos);
			System.arraycopy(data, pos, b, off, n);
			pos += n;
			return n;
		}
	}

	@Test(timeout = 30000)
	public void aSourceThatStopsAnsweringStopsTheImportInsteadOfSpinning() throws Exception {
		byte[] log = good("a", "run-a");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/a.apdlog", log);
		byte[] archive = archiveOf(files, java.util.Collections.singletonMap("runs/a.apdlog", "run-a"));
		//cut inside the manifest or inside a file's data: nothing at all may be imported
		for (int cut : new int[]{ 10, 60, archive.length / 3, archive.length / 2 }){
			root = tmp.newFolder("device-stall-source-" + cut);
			ArchiveImporter.Report r = ArchiveImporter.importArchive(root, new StallingSource(archive, cut));
			assertFalse("cut at " + cut + ": " + r.summary(), r.ok);
			assertEquals(0, r.imported);
			assertNothingWritten();
		}
		//cut only in the zip's trailing directory (every file already arrived and was verified): all or nothing, never half
		root = tmp.newFolder("device-stall-source-tail");
		ArchiveImporter.Report tail = ArchiveImporter.importArchive(root, new StallingSource(archive, archive.length - 30));
		if (tail.ok){
			assertEquals(1, tail.imported);
			assertArrayEquals(log, java.nio.file.Files.readAllBytes(new File(root, "runs/a.apdlog").toPath()));
		} else {
			assertEquals(0, tail.imported);
			assertNothingWritten();
		}
		//the importer is free for a good archive afterwards
		assertTrue(ArchiveImporter.importArchive(tmp.newFolder("device-after-source-stall"), new ByteArrayInputStream(archive)).ok);
	}


	private byte[] archiveWithKind( String path, String kind, String manifestRunId, byte[] log ) throws Exception {
		ArchiveManifest manifest = new ArchiveManifest();
		ArchiveManifest.Entry entry = new ArchiveManifest.Entry();
		entry.path = path;
		entry.kind = kind;
		entry.runId = manifestRunId;
		entry.size = log.length;
		entry.sha256 = ArchiveManifest.sha256Hex(log);
		manifest.files.add(entry);
		return zip(names(ArchiveManifest.ENTRY_NAME, path),
				Arrays.asList(manifest.encode().getBytes(ArchiveManifest.utf8()), log));
	}

	@Test
	public void aRunPathCannotMasqueradeAsLegacyToBypassRunIdentity() throws Exception {
		byte[] log = good("a", "run-actually-a");
		ArchiveImporter.Report report = run(archiveWithKind("runs/a.apdlog", ArchiveManifest.KIND_IMPORTED,
				"run-says-something-else", log));
		assertFalse(report.summary(), report.ok);
		assertEquals(0, report.imported);
		assertNothingWritten();
	}

	@Test
	public void aLegacyPathCannotMasqueradeAsARun() throws Exception {
		byte[] log = good("a", "run-a");
		ArchiveImporter.Report report = run(archiveWithKind("legacy/a.apdlog", ArchiveManifest.KIND_RUN,
				"run-a", log));
		assertFalse(report.summary(), report.ok);
		assertEquals(0, report.imported);
		assertNothingWritten();
	}
}
