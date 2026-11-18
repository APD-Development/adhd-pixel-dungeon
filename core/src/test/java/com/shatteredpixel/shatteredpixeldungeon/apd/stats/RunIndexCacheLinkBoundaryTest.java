package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.*;

public class RunIndexCacheLinkBoundaryTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void linkedTempMustNotOverwriteExternalBytesOrSavedIndex() throws Exception {
		File root = temp.newFolder("index-save-link");
		File runs = new File(root, "runs");
		assertTrue(runs.mkdirs());
		File indexPath = new File(root, "index.apdi");
		RunIndex index = new RunIndex(indexPath, runs);
		index.upsert(new RunIndexEntry("real", TelemetryVersion.CURRENT, false, "real.apdlog",
				1, RunIndexEntry.Status.OPEN, 0, 0, 0, 1));
		index.save();
		byte[] originalIndex = Files.readAllBytes(indexPath.toPath());

		File external = new File(temp.getRoot(), "untouchable-index-target.bin");
		byte[] originalExternal = {11, 22, 33, 44, 55, 66};
		Files.write(external.toPath(), originalExternal);
		File alias = new File(indexPath.getPath() + ".tmp");
		Files.createSymbolicLink(alias.toPath(), external.toPath());

		boolean rejected = false;
		try {
			index.save();
		} catch (IOException expected) {
			rejected = true;
		}
		assertTrue("linked temp path must be refused before FileOutputStream truncates it", rejected);
		assertTrue("external bytes must be untouched", Arrays.equals(originalExternal, Files.readAllBytes(external.toPath())));
		assertTrue("saved local index must be untouched", Arrays.equals(originalIndex, Files.readAllBytes(indexPath.toPath())));
		assertTrue("refusal must not remove the pre-existing link", Files.isSymbolicLink(alias.toPath()));
	}

	@Test
	public void linkedCacheWithValidChecksumCannotHideActualHistory() throws Exception {
		StatsStore store = new StatsStore(temp.newFolder("index-load-link"));
		RunLogWriter writer = store.startRun("genuine", "test", 123);
		writer.close();
		assertTrue(store.refreshIndex("genuine"));
		store.index().save();
		File localIndex = new File(store.root(), "index.apdi");
		assertTrue(localIndex.delete());

		File foreign = new File(temp.getRoot(), "foreign-valid-index.apdi");
		RunIndex unrelated = new RunIndex(foreign, store.runsDir());
		unrelated.upsert(new RunIndexEntry("fabricated", TelemetryVersion.CURRENT, false,
				"fabricated.apdlog", 9, RunIndexEntry.Status.OPEN, 0, 0, 0, 0));
		unrelated.save();
		byte[] originalForeign = Files.readAllBytes(foreign.toPath());
		Files.createSymbolicLink(localIndex.toPath(), foreign.toPath());

		RunIndex actual = RunIndex.load(localIndex, store.runsDir());
		assertTrue("a linked derived index cannot be trusted even with a valid CRC", actual.wasRebuilt());
		assertNotNull("the real log must remain visible", actual.get("genuine"));
		assertNull("the unrelated index must not invent local history", actual.get("fabricated"));
		assertTrue("external derived data must not be rewritten", Arrays.equals(originalForeign, Files.readAllBytes(foreign.toPath())));
		assertTrue(store.logFile("genuine").isFile());
	}

	@Test
	public void symlinkedStorageRootStillSupportsOrdinaryIndexSaveAndLoad() throws Exception {
		File realRoot = temp.newFolder("index-real-root");
		File runs = new File(realRoot, "runs");
		assertTrue(runs.mkdirs());
		File linkedRoot = new File(temp.getRoot(), "index-linked-root");
		Files.createSymbolicLink(linkedRoot.toPath(), realRoot.toPath());
		File indexFile = new File(linkedRoot, "index.apdi");
		RunIndex index = new RunIndex(indexFile, new File(linkedRoot, "runs"));
		index.upsert(new RunIndexEntry("real", TelemetryVersion.CURRENT, false, "real.apdlog",
				1, RunIndexEntry.Status.OPEN, 0, 0, 0, 0));
		index.save();
		RunIndex loaded = RunIndex.load(indexFile, new File(linkedRoot, "runs"));
		assertFalse("linked directory is allowed; the individual index is direct", loaded.wasRebuilt());
		assertNotNull(loaded.get("real"));
	}
}
