package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipInputStream;

//The archive APIs own their compression wrappers, not the streams supplied by Android's document provider.
public class ArchiveStreamOwnershipTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final class TrackingOutputStream extends ByteArrayOutputStream {
		boolean closed;
		@Override public void close() throws IOException { closed = true; super.close(); }
	}

	private static final class TrackingInputStream extends ByteArrayInputStream {
		boolean closed;
		TrackingInputStream( byte[] bytes ){ super(bytes); }
		@Override public void close() throws IOException { closed = true; super.close(); }
	}

	private static final class TrackingZipInputStream extends ZipInputStream {
		boolean closed;
		TrackingZipInputStream( byte[] bytes ){ super(new ByteArrayInputStream(bytes)); }
		@Override public void close() throws IOException { closed = true; super.close(); }
	}

	@Test
	public void exporterReleasesItsZipWithoutClosingTheCallerStream() throws Exception {
		File source = tmp.newFolder("export-source");
		ArchiveSupport.history(source);
		TrackingOutputStream out = new TrackingOutputStream();
		ArchiveExporter.Report report = ArchiveExporter.export(source, out, "test", 1);
		assertTrue(report.summary(), report.ok);
		assertFalse("the caller retains destination-stream ownership", out.closed);
		out.write(7);
	}

	@Test
	public void importerReleasesItsZipWithoutClosingTheCallerStream() throws Exception {
		File source = tmp.newFolder("import-source");
		ArchiveSupport.history(source);
		byte[] archive = ArchiveSupport.export(source);
		TrackingInputStream in = new TrackingInputStream(archive);
		ArchiveImporter.Report publicReport = ArchiveImporter.importArchive(tmp.newFolder("public-target"), in);
		assertTrue(publicReport.summary(), publicReport.ok);
		assertFalse("the caller retains source-stream ownership", in.closed);
		TrackingZipInputStream zip = new TrackingZipInputStream(archive);
		ArchiveImporter.Report directReport = ArchiveImporter.importZip(tmp.newFolder("direct-target"), zip);
		assertTrue(directReport.summary(), directReport.ok);
		assertTrue("an importer-owned ZIP wrapper is always released", zip.closed);
	}
	@Test
	public void missingArchiveStreamsReturnAReportWithoutCreatingHistory() throws Exception {
		File root = tmp.newFolder("missing-streams");
		ArchiveImporter.Report source = ArchiveImporter.importArchive(root, null);
		assertFalse(source.summary(), source.ok);
		assertEquals(0, source.imported);
		assertTrue(source.summary(), source.problem.contains("source is not available"));
		assertEquals("refusing a missing source must not create files", 0, root.list().length);

		ArchiveImporter.Report zip = ArchiveImporter.importZip(root, null);
		assertFalse(zip.summary(), zip.ok);
		assertEquals(0, zip.imported);
		assertTrue(zip.summary(), zip.problem.contains("ZIP stream is not available"));
		assertEquals("refusing a missing ZIP must not create files", 0, root.list().length);
	}

}
