package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class RunIndexLinkBoundaryTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void refreshAndRebuildNeverReadLinkedIndividualRunLogs() throws Exception {
		StatsStore store = new StatsStore(tmp.newFolder("real-store"));
		RunLogWriter writer = store.startRun("original", "test", 10);
		writer.event(LogTestSupport.T_PLAIN).clock(1).unsigned(7).emit();
		writer.close();
		assertTrue(store.refreshIndex("original"));

		File external = new File(tmp.newFolder("outside"), "external.apdlog");
		LogTestSupport.write(external, LogTestSupport.bytes(store.logFile("original")));
		byte[] externalBytes = LogTestSupport.bytes(external);
		File outsideLink = new File(store.runsDir(), "external.apdlog");
		Files.createSymbolicLink(outsideLink.toPath(), external.toPath());
		File localAlias = new File(store.runsDir(), "alias.apdlog");
		Files.createSymbolicLink(localAlias.toPath(), store.logFile("original").toPath());

		// A real, corrupt direct-child log must still be visible as UNREADABLE.
		File damaged = new File(store.runsDir(), "damaged.apdlog");
		LogTestSupport.write(damaged, new byte[]{1, 2, 3});

		RunIndex index = store.index();
		index.rebuild();
		assertEquals(2, index.size());
		assertNotNull(index.get("original"));
		assertEquals(RunIndexEntry.Status.UNREADABLE, index.get("damaged").status);
		assertNull(index.get("external"));
		assertNull(index.get("alias"));

		for (String id : new String[]{"external", "alias"}){
			boolean refused = false;
			try {
				index.refresh(id);
			} catch (IOException expected){
				refused = true;
			}
			assertTrue("linked log must be refused: " + id, refused);
			assertFalse("store must not report refreshing a linked log: " + id, store.refreshIndex(id));
			assertNull(index.get(id));
		}
		assertEquals(2, index.size());
		assertNotNull(index.refresh("original"));
		assertTrue(Arrays.equals(externalBytes, LogTestSupport.bytes(external)));
		assertTrue(Files.isSymbolicLink(outsideLink.toPath()));
		assertTrue(Files.isSymbolicLink(localAlias.toPath()));

		// Missing-index recovery must obey the same boundary as manual rebuild.
		File indexFile = new File(store.root(), "index.apdi");
		assertTrue(indexFile.delete());
		RunIndex rebuilt = RunIndex.load(indexFile, store.runsDir());
		assertTrue(rebuilt.wasRebuilt());
		assertEquals(2, rebuilt.size());
		assertNull(rebuilt.get("external"));
		assertNull(rebuilt.get("alias"));
		assertNotNull(rebuilt.get("original"));
	}

	@Test
	public void symlinkedHistoryRootRemainsCompatible() throws Exception {
		File realRoot = tmp.newFolder("real-root");
		File linkedRoot = new File(tmp.getRoot(), "linked-root");
		Files.createSymbolicLink(linkedRoot.toPath(), realRoot.toPath());
		StatsStore store = new StatsStore(linkedRoot);
		RunLogWriter writer = store.startRun("real", "test", 1);
		writer.close();
		assertTrue(store.refreshIndex("real"));
		RunIndex index = store.index();
		index.rebuild();
		assertEquals(1, index.size());
		assertNotNull(index.refresh("real"));
	}
}
