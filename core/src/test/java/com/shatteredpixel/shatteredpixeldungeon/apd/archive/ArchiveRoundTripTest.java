package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveSupport.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellFormatter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Dataset;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Query;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Scope;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

//Export, then import on another device: the history comes across, the derived datasets rebuild to the same thing, nothing is ever
//overwritten, and what is hidden in a run that is not finished never leaves.
public class ArchiveRoundTripTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File fresh( String name ) throws Exception {
		return tmp.newFolder(name);
	}

	private static ArchiveImporter.Report importInto( File root, byte[] archive ){
		return ArchiveImporter.importArchive(root, new ByteArrayInputStream(archive));
	}

	//every row of every dataset, with the cell text of every PUBLIC column: what a player can read in UIMSD
	private static String publicDump( File root ){
		Catalog c = new DatasetStore(root).load();
		StringBuilder b = new StringBuilder();
		for (Dataset ds : c.datasets()){
			Query q = Query.of(ds.def.id).withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
			List<String> lines = new ArrayList<>();
			for (Row r : ds.rows){
				if (!r.publicRow) continue;
				StringBuilder l = new StringBuilder(r.id).append(r.finished ? " F " : " U ");
				for (ColumnDef col : ds.def.columns()){
					if (col.isInternal()) continue;
					l.append(CellFormatter.cell(q, r, col).text).append('|');
				}
				lines.add(l.toString());
			}
			java.util.Collections.sort(lines);
			b.append(ds.def.id).append(':').append(lines.size()).append('\n');
			for (String l : lines) b.append(l).append('\n');
		}
		return b.toString();
	}

	private static Map<String, byte[]> entries( byte[] archive ) throws Exception {
		Map<String, byte[]> out = new TreeMap<>();
		ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(archive));
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

	@Test
	public void theHistoryMovesToAnotherDeviceAndRebuildsToTheSameThing() throws Exception {
		File a = fresh("a");
		history(a);
		byte[] archive = export(a);

		File b = fresh("b");
		ArchiveImporter.Report r = importInto(b, archive);
		assertTrue(r.summary(), r.ok);
		assertEquals(r.summary(), 6, r.imported);
		assertEquals(0, r.rejected);

		Map<String, byte[]> src = files(a), dst = files(b);
		//finished runs, their segments, the imported records and the old run are byte for byte the same files
		for (String keep : new String[]{ "runs/fin.apdlog", "runs/two-a.apdlog", "runs/two-b.apdlog", "legacy/import-1.apdlog", "runs/old.apdlog" }){
			assertArrayEquals(keep, src.get(keep), dst.get(keep));
		}
		//the run that is not finished comes only as a redacted copy
		assertFalse(java.util.Arrays.equals(src.get("runs/open.apdlog"), dst.get("runs/open.apdlog")));
		assertEquals(1, r.redactedImported);
		//and everything a player can read in UIMSD is the same on both devices
		assertEquals(publicDump(a), publicDump(b));
	}

	@Test
	public void theHiddenInformationOfARunInProgressNeverLeavesTheApp() throws Exception {
		File a = fresh("a");
		history(a);
		Map<String, byte[]> parts = entries(export(a));
		byte[] open = parts.get("runs/open.apdlog");
		assertNotNull(open);
		for (String hidden : new String[]{ SECRET_CLASS, SECRET_ROOM, SECRET_SEED, "actors.mobs.Goo", "items.armor.PlateArmor" }){
			assertFalse(hidden + " is in the archive entry of the unfinished run", contains(open, hidden));
		}
		//the raw log of the same run does hold it
		assertTrue(contains(files(a).get("runs/open.apdlog"), SECRET_CLASS));
		//the finished run's own hidden information travels (the player may read it once the run is over)
		assertTrue(contains(parts.get("runs/fin.apdlog"), "items.weapon.melee.Sword"));
		//no entry of the archive, manifest included, mentions the unfinished run's secrets
		for (Map.Entry<String, byte[]> e : parts.entrySet()){
			for (String hidden : new String[]{ SECRET_CLASS, SECRET_ROOM, SECRET_SEED }) assertFalse(e.getKey(), contains(e.getValue(), hidden));
		}
	}

	@Test
	public void theImportedRedactedRunHasNoSeedNotAZeroSeed() throws Exception {
		File a = fresh("a");
		history(a);
		File b = fresh("b");
		importInto(b, export(a));
		Dataset runs = new DatasetStore(b).load().dataset("runs");
		ColumnDef seed = runs.def.column("run.seed");
		boolean sawOpen = false, sawFinished = false;
		for (Row r : runs.rows){
			if (r.runId.equals("run-open")){
				sawOpen = true;
				assertFalse("the redacted run's seed is unknown, never 0", r.isKnown(seed.index()));
			}
			if (r.runId.equals("run-fin")){
				sawFinished = true;
				assertEquals(777L, r.num(seed.index()));
			}
		}
		assertTrue(sawOpen && sawFinished);
	}

	@Test
	public void importingTwiceAddsNothingAndChangesNothing() throws Exception {
		File a = fresh("a");
		history(a);
		byte[] archive = export(a);
		File b = fresh("b");
		importInto(b, archive);
		Map<String, byte[]> once = files(b);
		ArchiveImporter.Report again = importInto(b, archive);
		assertEquals(0, again.imported);
		assertEquals(6, again.skippedAlreadyHere);
		assertEquals(0, again.keptDifferentExisting);
		Map<String, byte[]> twice = files(b);
		assertEquals(once.keySet(), twice.keySet());
		for (String k : once.keySet()) if (k.startsWith("runs/") || k.startsWith("legacy/")) assertArrayEquals(k, once.get(k), twice.get(k));
	}

	@Test
	public void reimportIntoTheSourceKeepsOriginalUnfinishedSecretsAndExplainsDifferences() throws Exception {
		File root = fresh("source");
		history(root);
		byte[] archive = export(root);
		Map<String, byte[]> original = files(root);
		for (int attempt = 0; attempt < 3; attempt++){
			ArchiveImporter.Report report = importInto(root, archive);
			assertTrue(report.summary(), report.ok);
			assertEquals(0, report.imported);
			assertEquals(5, report.skippedAlreadyHere);
			assertEquals(1, report.keptDifferentExisting);
			assertEquals(1, report.redactedDifferentExisting);
			assertTrue(report.summary().contains("hidden information removed"));
			Map<String, byte[]> after = files(root);
			for (String path : original.keySet()){
				if (path.startsWith("runs/") || path.startsWith("legacy/")) assertArrayEquals(path, original.get(path), after.get(path));
			}
			assertTrue(contains(after.get("runs/open.apdlog"), SECRET_SEED));
		}
	}

	@Test
	public void aDifferentLogAlreadyHereIsNeverReplaced() throws Exception {
		File a = fresh("a");
		history(a);
		byte[] archive = export(a);
		File b = fresh("b");
		byte[] mine = smallLog(tmp.newFolder("scratch"), "fin", "run-fin");   //a different log with the same name
		new File(b, "runs").mkdirs();
		java.nio.file.Files.write(new File(b, "runs/fin.apdlog").toPath(), mine);
		ArchiveImporter.Report r = importInto(b, archive);
		assertEquals(1, r.keptDifferentExisting);
		assertEquals(5, r.imported);
		assertArrayEquals("the log that was here is untouched", mine, files(b).get("runs/fin.apdlog"));
	}

	@Test
	public void importMergesIntoAHistoryThatAlreadyHasOtherRuns() throws Exception {
		File a = fresh("a");
		history(a);
		File b = fresh("b");
		byte[] other = smallLog(b, "local-run", "run-local");
		ArchiveImporter.Report r = importInto(b, export(a));
		assertEquals(6, r.imported);
		assertArrayEquals(other, files(b).get("runs/local-run.apdlog"));
		Catalog c = new DatasetStore(b).load();
		assertEquals(5, c.dataset("runs").rows.size());   //fin, open, two (merged), old, local
	}

	@Test
	public void exportingNeverChangesTheHistory() throws Exception {
		File a = fresh("a");
		history(a);
		Map<String, byte[]> before = files(a);
		export(a);
		Map<String, byte[]> after = files(a);
		assertEquals(before.keySet(), after.keySet());
		for (String k : before.keySet()) assertArrayEquals(k, before.get(k), after.get(k));
	}

	@Test
	public void exportWhileARunIsBeingRecordedIsConsistent() throws Exception {
		File a = fresh("a");
		history(a);
		//a live recording: a writer that keeps appending while the export reads
		final TestLogs live = new TestLogs(a, "live", V, 9000).start("run-live", 0, "MAGE", RS, 0, 30, false).enter(1);
		//The writer is asynchronous: establish the two initial public events before testing concurrent appends.
		assertTrue("initial live events reached disk", live.w.flush());
		final java.util.concurrent.atomic.AtomicBoolean stop = new java.util.concurrent.atomic.AtomicBoolean();
		Thread t = new Thread(new Runnable(){
			@Override
			public void run(){
				int i = 0;
				while (!stop.get() && i < 20000){
					live.heroAttacks("rat", "actors.mobs.Rat", i % 2 == 0);
					live.registered("x" + i, SECRET_CLASS, 1, false, 0);
					i++;
				}
			}
		});
		t.start();
		byte[] archive;
		try {
			archive = export(a);
		} finally {
			stop.set(true);
			t.join();
			live.close(false);
		}
		File b = fresh("b");
		ArchiveImporter.Report r = importInto(b, archive);
		assertTrue(r.summary(), r.ok);
		assertEquals(0, r.rejected);
		ReadResult red = RunLogReader.read(new File(b, "runs/live.apdlog"));
		assertTrue(red.events.size() > 1);
		assertFalse(contains(files(b).get("runs/live.apdlog"), SECRET_CLASS));
	}

	@Test
	public void aChangedFinishedLogNeverReportsASuccessfulIncompleteArchive() throws Exception {
		File root = fresh("changed-export");
		history(root);
		final File changed = new File(root, "runs/fin.apdlog");
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		final boolean[] mutated = {false};
		java.io.OutputStream out = new java.io.OutputStream(){
			private void mutate() throws java.io.IOException {
				if (mutated[0]) return;
				mutated[0] = true;
				//The first ZIP write follows manifest planning: simulate a file changing before its second read.
				try (java.io.FileOutputStream append = new java.io.FileOutputStream(changed, true)) { append.write(0); }
			}
			@Override public void write(int value) throws java.io.IOException { mutate(); bytes.write(value); }
			@Override public void write(byte[] data, int off, int len) throws java.io.IOException { mutate(); bytes.write(data, off, len); }
		};
		ArchiveExporter.Report report = ArchiveExporter.export(root, out, "test", 1);
		assertTrue("controlled mutation ran after manifest planning", mutated[0]);
		assertFalse("an archive missing a manifest-listed log must not be reported successful", report.ok);
		assertTrue("failure explains retry", report.problem.contains("changed") && report.problem.contains("export again"));
	}

	@Test
	public void aRunWhoseNewestSegmentIsNotFinishedIsRedactedInEverySegment() throws Exception {
		File a = fresh("a");
		TestLogs s0 = new TestLogs(a, "p-a", V, 100).start("run-p", 0, "MAGE", RS, 0, 0, false).enter(1);
		s0.registered("q", SECRET_CLASS, 1, false, 0).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		TestLogs s1 = new TestLogs(a, "p-b", V, 200).start("run-p", 1, "MAGE", RS, 0, 0, false).enter(1);   //play went on after the end was undone
		s1.registered("r", SECRET_CLASS, 1, false, 0).close(false);
		Map<String, byte[]> parts = entries(export(a));
		assertFalse(contains(parts.get("runs/p-a.apdlog"), SECRET_CLASS));
		assertFalse(contains(parts.get("runs/p-b.apdlog"), SECRET_CLASS));
	}

	@Test
	public void theArchiveListsEverythingAndItsFirstEntryIsTheManifest() throws Exception {
		File a = fresh("a");
		history(a);
		byte[] archive = export(a);
		ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(archive));
		assertEquals(ArchiveManifest.ENTRY_NAME, z.getNextEntry().getName());
		Map<String, byte[]> parts = entries(archive);
		ArchiveManifest m = ArchiveManifest.parse(parts.get(ArchiveManifest.ENTRY_NAME));
		assertEquals(6, m.files.size());
		for (ArchiveManifest.Entry e : m.files){
			assertEquals(e.path, e.size, parts.get(e.path).length);
			assertEquals(e.path, e.sha256, ArchiveManifest.sha256Hex(parts.get(e.path)));
		}
		assertEquals(com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion.CURRENT.toString(), m.telemetry);
		assertTrue(m.find("runs/open.apdlog").redacted);
		assertFalse(m.find("runs/fin.apdlog").redacted);
		assertEquals("1.1", m.find("runs/old.apdlog").telemetry);
		//derived data is not exported
		for (String name : parts.keySet()) assertTrue(name, name.equals(ArchiveManifest.ENTRY_NAME) || name.startsWith("runs/") || name.startsWith("legacy/"));
	}

	@Test
	public void derivedCachesAreNotExportedAndTheImportRebuildsThem() throws Exception {
		File a = fresh("a");
		history(a);
		new DatasetStore(a).load();   //builds the caches
		assertTrue(files(a).keySet().toString(), files(a).keySet().toString().contains("derived/"));
		byte[] archive = export(a);
		for (String name : entries(archive).keySet()) assertFalse(name, name.startsWith("derived"));
		File b = fresh("b");
		importInto(b, archive);
		assertFalse(files(b).keySet().toString().contains("derived/"));
		assertEquals(publicDump(a), publicDump(b));
		assertTrue(files(b).keySet().toString().contains("derived/"));   //rebuilt on first read
	}

	@Test
	public void anEmptyOrMissingHistoryExportsNothingSafely() throws Exception {
		ArchiveExporter.Report r = ArchiveExporter.export(new File(tmp.getRoot(), "nothing"), new ByteArrayOutputStream(), "t", 1);
		assertFalse(r.ok);
		assertFalse(ArchiveExporter.export(null, new ByteArrayOutputStream(), "t", 1).ok);
		File empty = fresh("empty");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue(ArchiveExporter.export(empty, out, "t", 1).ok);
		File b = fresh("b");
		ArchiveImporter.Report imp = importInto(b, out.toByteArray());
		assertTrue(imp.ok);
		assertEquals(0, imp.imported);
	}

	@Test
	public void oldTelemetryHistoryIsExportedAndImportedAsItIs() throws Exception {
		File a = fresh("a");
		history(a);
		File b = fresh("b");
		importInto(b, export(a));
		ReadResult r = RunLogReader.read(new File(b, "runs/old.apdlog"));
		assertEquals("1.1", r.header.telemetryVersion.toString());
		boolean ended = false;
		for (ApdEvent e : r.events) if (e.type == EventTypes.RUN_END) ended = true;
		assertTrue(ended);
	}

	@Test
	public void storageMeterSeesTheImportedHistory() throws Exception {
		File a = fresh("a");
		history(a);
		File b = fresh("b");
		importInto(b, export(a));
		StorageMeter.Report r = StorageMeter.measure(b);
		assertEquals(5, r.runLogs);
		assertEquals(1, r.importedFiles);
	}


	private File malformedThenReadableStart( File root, String logId, String runId ) throws Exception {
		TestLogs log = new TestLogs(root, logId, V, 1000);
		log.w.event(EventTypes.RUN_START).clock(log.clock++).emit();
		log.start(runId, 0, "CLERIC", RS, 0, 0, false).enter(1);
		log.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		return log.file;
	}

	@Test
	public void malformedFirstStartDoesNotHideReadableRunFromExportManifest() throws Exception {
		File root = fresh("export-malformed-start");
		File log = malformedThenReadableStart(root, "physical-log", "logical-run");
		byte[] original = java.nio.file.Files.readAllBytes(log.toPath());
		Map<String, byte[]> parts = entries(export(root));
		ArchiveManifest manifest = ArchiveManifest.parse(parts.get(ArchiveManifest.ENTRY_NAME));
		assertEquals("logical-run", manifest.find("runs/physical-log.apdlog").runId);
		assertArrayEquals("a finished log still exports byte for byte", original, parts.get("runs/physical-log.apdlog"));
	}

	@Test
	public void importUsesFirstReadableStartAfterMalformedStart() throws Exception {
		File source = fresh("import-readable-source");
		byte[] log = java.nio.file.Files.readAllBytes(
				malformedThenReadableStart(source, "physical-log", "logical-run").toPath());
		Map<String, byte[]> files = new java.util.LinkedHashMap<>();
		files.put("runs/physical-log.apdlog", log);
		ArchiveImporter.Report report = importInto(fresh("import-readable-target"),
				archiveOf(files, java.util.Collections.singletonMap("runs/physical-log.apdlog", "logical-run")));
		assertTrue(report.summary(), report.ok);
		assertEquals(1, report.imported);
	}

	@Test
	public void importRejectsManifestThatIgnoresLaterReadableStart() throws Exception {
		File source = fresh("import-mismatch-source");
		byte[] log = java.nio.file.Files.readAllBytes(
				malformedThenReadableStart(source, "physical-log", "logical-run").toPath());
		Map<String, byte[]> files = new java.util.LinkedHashMap<>();
		files.put("runs/physical-log.apdlog", log);
		File target = fresh("import-mismatch-target");
		ArchiveImporter.Report report = importInto(target,
				archiveOf(files, java.util.Collections.singletonMap("runs/physical-log.apdlog", "physical-log")));
		assertFalse(report.summary(), report.ok);
		assertEquals(1, report.rejected);
		assertFalse(new File(target, "runs/physical-log.apdlog").exists());
	}

	private File emptyThenReadableStart( File root, String logId, String runId ) throws Exception {
		TestLogs log = new TestLogs(root, logId, V, 1000);
		log.start("", 0, "CLERIC", RS, 0, 0, false);
		log.start(runId, 1, "CLERIC", RS, 0, 0, true).enter(1);
		log.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		return log.file;
	}

	@Test
	public void emptyFirstStartDoesNotHideValidRunFromExportManifest() throws Exception {
		File root = fresh("export-empty-start");
		File log = emptyThenReadableStart(root, "physical-log", "logical-run");
		byte[] original = java.nio.file.Files.readAllBytes(log.toPath());
		Map<String, byte[]> parts = entries(export(root));
		ArchiveManifest manifest = ArchiveManifest.parse(parts.get(ArchiveManifest.ENTRY_NAME));
		assertEquals("logical-run", manifest.find("runs/physical-log.apdlog").runId);
		assertArrayEquals("a finished log still exports byte for byte", original, parts.get("runs/physical-log.apdlog"));
	}

	@Test
	public void importUsesFirstValidIdentityAfterEmptyStart() throws Exception {
		File source = fresh("import-empty-source");
		byte[] log = java.nio.file.Files.readAllBytes(
				emptyThenReadableStart(source, "physical-log", "logical-run").toPath());
		Map<String, byte[]> files = new java.util.LinkedHashMap<>();
		files.put("runs/physical-log.apdlog", log);

		ArchiveImporter.Report correct = importInto(fresh("import-empty-correct"),
				archiveOf(files, java.util.Collections.singletonMap("runs/physical-log.apdlog", "logical-run")));
		assertTrue(correct.summary(), correct.ok);
		assertEquals(1, correct.imported);

		File wrongTarget = fresh("import-empty-wrong");
		ArchiveImporter.Report wrong = importInto(wrongTarget,
				archiveOf(files, java.util.Collections.singletonMap("runs/physical-log.apdlog", "physical-log")));
		assertFalse(wrong.summary(), wrong.ok);
		assertEquals(1, wrong.rejected);
		assertFalse(new File(wrongTarget, "runs/physical-log.apdlog").exists());
	}

}
