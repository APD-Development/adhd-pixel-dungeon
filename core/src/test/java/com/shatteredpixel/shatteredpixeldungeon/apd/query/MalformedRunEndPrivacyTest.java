package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveExporter;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveManifest;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

public class MalformedRunEndPrivacyTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String SECRET = "hidden-current-run-secret";

	private File malformedEndedLog( File root ) throws Exception {
		File runs = new File(root, "runs");
		assertTrue(runs.isDirectory() || runs.mkdirs());
		File file = new File(runs, "segment.apdlog");
		RunLogWriter writer = RunLogWriter.create(file, TelemetryVersion.CURRENT, "segment", "test", 1);
		assertFalse(writer.isDisabled());

		LifecycleEvents.RunStart start = new LifecycleEvents.RunStart();
		start.runId = "logical-run";
		start.heroClass = "MAGE";
		start.rulesetId = "recorded-rules";
		start.rulesetText = "recorded rules";
		start.exoticChance = 0;
		assertTrue(LifecycleEvents.emitRunStart(writer, 0, false, start));
		assertTrue(writer.event(EventTypes.FACT_INTERNAL).clock(1).string(SECRET).emit());
		//The frame and checksum are valid, but the typed RUN_END payload is absent.
		assertTrue(writer.append(EventTypes.RUN_END, 0, 2, new byte[0]));
		assertTrue(writer.close(true));
		return file;
	}

	@Test
	public void malformedRunEndCannotMarkRunFinished() throws Exception {
		File root = tmp.newFolder("table");
		File file = malformedEndedLog(root);
		ReadResult read = RunLogReader.read(file);
		assertTrue("container remains structurally sealed", read.sealed);
		LogRows rows = RowBuilders.fromLog(read, "segment");
		assertFalse("an unreadable end cannot unlock finished-run visibility", rows.meta.hasRunEnd);
		assertFalse(rows.of(Datasets.RUNS).get(0).finished);
	}

	@Test
	public void malformedRunEndCannotExportHiddenCurrentRunData() throws Exception {
		File root = tmp.newFolder("archive");
		malformedEndedLog(root);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ArchiveExporter.Report report = ArchiveExporter.export(root, bytes, "test", 2);
		assertTrue(report.summary(), report.ok);
		assertEquals("the unreadable end leaves the run unfinished", 1, report.redactedRunLogs);

		Map<String, byte[]> entries = entries(bytes.toByteArray());
		ArchiveManifest manifest = ArchiveManifest.parse(entries.get(ArchiveManifest.ENTRY_NAME));
		assertTrue(manifest.find("runs/segment.apdlog").redacted);
		byte[] exported = entries.get("runs/segment.apdlog");
		assertNotNull(exported);
		assertFalse("hidden strings must stay on the device", contains(exported, SECRET));
	}

	private static Map<String, byte[]> entries( byte[] archive ) throws Exception {
		Map<String, byte[]> out = new LinkedHashMap<>();
		ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive));
		try {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null){
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				byte[] buffer = new byte[4096];
				int count;
				while ((count = zip.read(buffer)) > 0) bytes.write(buffer, 0, count);
				out.put(entry.getName(), bytes.toByteArray());
			}
		} finally {
			zip.close();
		}
		return out;
	}

	private static boolean contains( byte[] data, String text ){
		byte[] target = text.getBytes(StandardCharsets.UTF_8);
		outer:
		for (int i = 0; i + target.length <= data.length; i++){
			for (int j = 0; j < target.length; j++) if (data[i + j] != target[j]) continue outer;
			return true;
		}
		return false;
	}
}
