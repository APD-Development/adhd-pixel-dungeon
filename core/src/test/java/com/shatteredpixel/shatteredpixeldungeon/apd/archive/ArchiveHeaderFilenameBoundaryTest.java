package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

// An archive's typed RUN_START is the shared logical run ID, not the
// identity of an individual runs/<file-id>.apdlog log segment.
public class ArchiveHeaderFilenameBoundaryTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void matchingTypedRunStartCannotDisguiseWrongHeaderFilename() throws Exception {
		File scratch = tmp.newFolder("scratch-wrong");
		byte[] log = ArchiveSupport.smallLog(scratch, "actual-header", "run-shared");
		File root = tmp.newFolder("destination-wrong");

		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/claimed-file.apdlog", log);
		Map<String, String> ids = new LinkedHashMap<>();
		ids.put("runs/claimed-file.apdlog", "run-shared");
		ArchiveImporter.Report report = ArchiveImporter.importArchive(root,
				new ByteArrayInputStream(ArchiveSupport.archiveOf(files, ids)));

		assertFalse(report.summary(), report.ok);
		assertEquals(0, report.imported);
		assertEquals(1, report.rejected);
		assertFalse(new File(root, "runs/claimed-file.apdlog").exists());
		assertFalse(new File(root, "import-tmp").exists());
	}

	@Test
	public void rejectedHeaderCannotPartiallyImportOtherLogOrModifyExistingHistory() throws Exception {
		File root = tmp.newFolder("destination-atomic");
		File runs = new File(root, "runs");
		assertTrue(runs.mkdir());
		File previous = new File(runs, "existing.apdlog");
		byte[] original = "existing original history".getBytes(ArchiveManifest.utf8());
		Files.write(previous.toPath(), original);

		File scratch = tmp.newFolder("scratch-atomic");
		byte[] legitimate = ArchiveSupport.smallLog(scratch, "valid-file", "run-valid");
		byte[] forged = ArchiveSupport.smallLog(scratch, "actual-header", "run-claimed");
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/valid-file.apdlog", legitimate);
		files.put("runs/claimed-file.apdlog", forged);
		Map<String, String> ids = new LinkedHashMap<>();
		ids.put("runs/valid-file.apdlog", "run-valid");
		ids.put("runs/claimed-file.apdlog", "run-claimed");

		ArchiveImporter.Report report = ArchiveImporter.importArchive(root,
				new ByteArrayInputStream(ArchiveSupport.archiveOf(files, ids)));

		assertFalse(report.summary(), report.ok);
		assertEquals(0, report.imported);
		assertArrayEquals(original, Files.readAllBytes(previous.toPath()));
		assertFalse(new File(runs, "valid-file.apdlog").exists());
		assertFalse(new File(runs, "claimed-file.apdlog").exists());
		assertFalse(new File(root, "import-tmp").exists());
	}

	@Test
	public void distinctSegmentHeadersWithOneSharedRunIdStillImportLosslessly() throws Exception {
		File source = tmp.newFolder("source-segments");
		TestLogs first = new TestLogs(source, "segment-zero", ArchiveSupport.V, 1000)
				.start("run-shared", 0, "WARRIOR", ArchiveSupport.RS, 0, 30, false)
				.enter(1);
		first.close(false);
		TestLogs second = new TestLogs(source, "segment-one", ArchiveSupport.V, 2000)
				.start("run-shared", 1, "WARRIOR", ArchiveSupport.RS, 0, 30, false)
				.enter(2)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 2, 2, 100);
		second.close(true);
		byte[] segment0 = Files.readAllBytes(first.file.toPath());
		byte[] segment1 = Files.readAllBytes(second.file.toPath());
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put("runs/segment-zero.apdlog", segment0);
		files.put("runs/segment-one.apdlog", segment1);
		Map<String, String> ids = new LinkedHashMap<>();
		ids.put("runs/segment-zero.apdlog", "run-shared");
		ids.put("runs/segment-one.apdlog", "run-shared");
		File destination = tmp.newFolder("destination-segments");

		ArchiveImporter.Report report = ArchiveImporter.importArchive(destination,
				new ByteArrayInputStream(ArchiveSupport.archiveOf(files, ids)));
		assertTrue(report.summary(), report.ok);
		assertEquals(2, report.imported);
		assertArrayEquals(segment0, Files.readAllBytes(new File(destination, "runs/segment-zero.apdlog").toPath()));
		assertArrayEquals(segment1, Files.readAllBytes(new File(destination, "runs/segment-one.apdlog").toPath()));
	}
}
