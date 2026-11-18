package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DerivedStoreLogLinkBoundaryTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private File legacyLog( File dir, String fileId, String gameId ) throws Exception {
		if (!dir.isDirectory()) assertTrue(dir.mkdirs());
		File file = new File(dir, fileId + RunIndex.LOG_SUFFIX);
		RunLogWriter writer = RunLogWriter.create(file, TelemetryVersion.CURRENT, fileId, "test", 1);
		LegacyEvents.Record record = new LegacyEvents.Record();
		record.gameId = gameId;
		record.source = "rankings.dat";
		record.putNum(FactKeys.SCORE, 123);
		LegacyEvents.emitRecord(writer, 0, record);
		writer.close(true);
		return file;
	}

	@Test
	public void apdDerivedFactsIgnoreExternalLinksAndInDirectoryAliases() throws Exception {
		File root = temp.newFolder("apd-store");
		File runs = new File(root, "runs");
		assertTrue(runs.mkdirs());
		File kept = LogTestSupport.writeLog(runs, "kept", 1, true);
		File external = LogTestSupport.writeLog(temp.newFolder("outside-runs"), "foreign", 1, true);
		byte[] keptBefore = Files.readAllBytes(kept.toPath());
		byte[] externalBefore = Files.readAllBytes(external.toPath());
		Files.createSymbolicLink(new File(runs, "external.apdlog").toPath(), external.toPath());
		Files.createSymbolicLink(new File(runs, "alias.apdlog").toPath(), kept.toPath());

		DerivedStore store = new DerivedStore(root);
		FactTable built = store.load();
		assertEquals(DerivedStore.Source.REBUILT_NO_CACHE, store.lastSource());
		assertEquals(1, built.apdRows.size());
		assertEquals("kept", built.apdRows.get(0).id);
		FactTable cached = store.load();
		assertEquals(DerivedStore.Source.CACHE, store.lastSource());
		assertEquals(1, cached.apdRows.size());
		assertEquals("kept", cached.apdRows.get(0).id);
		assertArrayEquals(keptBefore, Files.readAllBytes(kept.toPath()));
		assertArrayEquals(externalBefore, Files.readAllBytes(external.toPath()));
	}

	@Test
	public void legacyDerivedFactsIgnoreExternalLinksAndInDirectoryAliases() throws Exception {
		File root = temp.newFolder("legacy-store");
		File legacy = new File(root, "legacy");
		File kept = legacyLog(legacy, "import-kept", "game-kept");
		File external = legacyLog(temp.newFolder("outside-legacy"), "import-foreign", "game-foreign");
		byte[] externalBefore = Files.readAllBytes(external.toPath());
		Files.createSymbolicLink(new File(legacy, "external.apdlog").toPath(), external.toPath());
		Files.createSymbolicLink(new File(legacy, "alias.apdlog").toPath(), kept.toPath());

		DerivedStore store = new DerivedStore(root);
		FactTable built = store.load();
		assertEquals(1, built.legacyRows.size());
		assertEquals("legacy:game-kept", built.legacyRows.get(0).id);
		assertEquals(1, store.buildLegacyOnly().legacyRows.size());
		assertEquals(1, store.load().legacyRows.size());
		assertEquals(DerivedStore.Source.CACHE, store.lastSource());
		assertArrayEquals(externalBefore, Files.readAllBytes(external.toPath()));
	}

	@Test
	public void canonicalDirectChildrenRemainReadableUnderLinkedHistoryRoot() throws Exception {
		File realRoot = temp.newFolder("real-history");
		File linkedRoot = new File(temp.getRoot(), "linked-history");
		Files.createSymbolicLink(linkedRoot.toPath(), realRoot.toPath());
		File runs = new File(linkedRoot, "runs");
		assertTrue(runs.mkdirs());
		File kept = LogTestSupport.writeLog(runs, "kept", 1, true);
		legacyLog(new File(linkedRoot, "legacy"), "import-kept", "game-kept");
		byte[] before = Files.readAllBytes(kept.toPath());

		DerivedStore store = new DerivedStore(linkedRoot);
		FactTable table = store.load();
		assertEquals(1, table.apdRows.size());
		assertEquals("kept", table.apdRows.get(0).id);
		assertEquals(1, table.legacyRows.size());
		assertEquals("legacy:game-kept", table.legacyRows.get(0).id);
		assertEquals(1, store.load().apdRows.size());
		assertEquals(DerivedStore.Source.CACHE, store.lastSource());
		assertArrayEquals(before, Files.readAllBytes(kept.toPath()));
	}
}
