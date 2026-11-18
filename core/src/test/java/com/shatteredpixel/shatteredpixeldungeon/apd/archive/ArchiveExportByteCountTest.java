package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

// The owner's export receipt describes bytes "in the archive", not the sum of
// uncompressed log payloads. Count the exact ZIP the player receives.
public class ArchiveExportByteCountTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void fullHistoryReportsTheExactCompressedZipLength() throws Exception {
		File history = temp.newFolder("history");
		ArchiveSupport.history(history);
		ByteArrayOutputStream written = new ByteArrayOutputStream();
		ArchiveExporter.Report report = ArchiveExporter.export(history, written, "test", 123456L);

		assertTrue(report.summary(), report.ok);
		assertTrue("a nonempty history is included", report.runLogs > 0);
		assertEquals("the archive receipt must equal its actual saved ZIP bytes",
				(long) written.size(), report.bytes);
		assertTrue("summary shows the same physical archive size",
				report.summary().contains("(" + Size.text(written.size()) + " in the archive)"));

		ArchiveImporter.Report restored = ArchiveImporter.importArchive(
				temp.newFolder("restored"), new ByteArrayInputStream(written.toByteArray()));
		assertTrue("the counted archive must still be importable: " + restored.summary(), restored.ok);
	}

	@Test
	public void emptyHistoryStillCountsTheManifestAndZipDirectory() throws Exception {
		File history = temp.newFolder("empty");
		ByteArrayOutputStream written = new ByteArrayOutputStream();
		ArchiveExporter.Report report = ArchiveExporter.export(history, written, "test", 123456L);

		assertTrue(report.summary(), report.ok);
		assertEquals(0, report.runLogs);
		assertEquals(0, report.importedFiles);
		assertTrue("even an empty archive has a manifest and ZIP structure", written.size() > 0);
		assertEquals((long) written.size(), report.bytes);
	}
}
