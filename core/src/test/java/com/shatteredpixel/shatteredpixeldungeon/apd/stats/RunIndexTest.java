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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RunIndexTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	//one store with: a sealed run, an open run, a damaged run, a run from a newer telemetry version, an unreadable file
	private StatsStore populated() throws Exception {
		StatsStore store = new StatsStore(tmp.newFolder("store"));

		RunLogWriter a = store.startRun("a-sealed", "b", 100);
		for (int i = 0; i < 5; i++) a.event(LogTestSupport.T_PLAIN).clock(i).unsigned(i).emit();
		a.close();
		store.refreshIndex("a-sealed");

		RunLogWriter b = store.startRun("b-open", "b", 200);
		for (int i = 0; i < 7; i++) b.event(LogTestSupport.T_PLAIN).clock(i).unsigned(i).emit();
		b.close(false);
		store.refreshIndex("b-open");

		RunLogWriter c = store.startRun("c-damaged", "b", 300);
		for (int i = 0; i < 9; i++) c.event(LogTestSupport.T_PLAIN).clock(i).unsigned(i).emit();
		c.close(false);
		File cf = store.logFile("c-damaged");
		byte[] cb = LogTestSupport.bytes(cf);
		LogTestSupport.write(cf, Arrays.copyOf(cb, cb.length - 2));
		store.refreshIndex("c-damaged");

		RunLogWriter d = store.startRun("d-future", new TelemetryVersion(9, 2), "b", 400, new RunLogWriter.Options());
		d.event(LogTestSupport.T_PLAIN).unsigned(1).emit();
		d.close();
		store.refreshIndex("d-future");

		LogTestSupport.write(new File(store.runsDir(), "e-junk.apdlog"), new byte[]{1, 2, 3});
		return store;
	}

	@Test
	public void entriesDescribeEachRun() throws Exception {
		StatsStore store = populated();
		store.index().rebuild(); //pick up the junk file
		RunIndex idx = store.index();

		RunIndexEntry a = idx.get("a-sealed");
		assertEquals(RunIndexEntry.Status.SEALED, a.status);
		assertEquals(5, a.eventCount);
		assertEquals(4, a.lastClock);
		assertEquals(100, a.createdAtMillis);
		assertEquals(TelemetryVersion.CURRENT, a.telemetryVersion);
		assertFalse(a.newerThanReader);
		assertEquals("a-sealed.apdlog", a.logFile);

		assertEquals(RunIndexEntry.Status.OPEN, idx.get("b-open").status);
		assertEquals(7, idx.get("b-open").eventCount);

		RunIndexEntry c = idx.get("c-damaged");
		assertEquals(RunIndexEntry.Status.DAMAGED, c.status);
		assertEquals(8, c.eventCount);
		assertTrue(c.validBytes < c.fileLength);

		RunIndexEntry d = idx.get("d-future");
		assertEquals(new TelemetryVersion(9, 2), d.telemetryVersion);
		assertTrue(d.newerThanReader);

		RunIndexEntry e = idx.get("e-junk");
		assertEquals(RunIndexEntry.Status.UNREADABLE, e.status);
		assertNull(e.telemetryVersion);
		assertEquals(5, idx.size());
	}

	@Test
	public void listIsOrderedByCreationTime() throws Exception {
		StatsStore store = populated();
		List<RunIndexEntry> all = store.index().all();
		assertEquals("a-sealed", all.get(0).runId);
		assertEquals("b-open", all.get(1).runId);
		assertEquals("c-damaged", all.get(2).runId);
		assertEquals("d-future", all.get(3).runId);
	}

	@Test
	public void saveAndLoadRoundTrip() throws Exception {
		StatsStore store = populated();
		store.index().save();
		RunIndex loaded = RunIndex.load(new File(store.root(), "index.apdi"), store.runsDir());
		assertFalse(loaded.wasRebuilt());
		assertEquals(store.index().all(), loaded.all());
	}

	@Test
	public void missingIndexIsRebuiltFromTheLogsAndMatches() throws Exception {
		StatsStore store = populated();
		store.index().rebuild();
		store.index().save();
		List<RunIndexEntry> expected = store.index().all();
		File indexFile = new File(store.root(), "index.apdi");
		assertTrue(indexFile.delete());

		RunIndex rebuilt = RunIndex.load(indexFile, store.runsDir());
		assertTrue(rebuilt.wasRebuilt());
		assertEquals(expected, rebuilt.all());
		assertTrue("the rebuilt index is saved again", indexFile.isFile());
	}

	@Test
	public void damagedIndexFilesAreRebuiltNotTrusted() throws Exception {
		StatsStore store = populated();
		store.index().rebuild();
		store.index().save();
		File indexFile = new File(store.root(), "index.apdi");
		byte[] good = LogTestSupport.bytes(indexFile);
		List<RunIndexEntry> expected = store.index().all();

		byte[][] variants = new byte[][]{
				Arrays.copyOf(good, good.length / 2),                          //truncated
				Arrays.copyOf(good, 3),                                        //almost nothing
				new byte[0],                                                   //empty
				flip(good, good.length / 2),                                   //bit rot
				flip(good, 0),                                                 //bad magic
				flip(good, good.length - 1),                                   //bad checksum
				LogTestSupport.concat(good, new byte[]{1}),                    //trailing byte
		};
		for (int i = 0; i < variants.length; i++){
			LogTestSupport.write(indexFile, variants[i]);
			RunIndex idx = RunIndex.load(indexFile, store.runsDir());
			assertTrue("variant " + i, idx.wasRebuilt());
			assertEquals("variant " + i, expected, idx.all());
		}
	}

	private static byte[] flip( byte[] data, int at ){
		byte[] c = data.clone();
		c[at] ^= 0x40;
		return c;
	}

	@Test
	public void leftoverTempFileFromAnInterruptedSaveIsHarmless() throws Exception {
		StatsStore store = populated();
		store.index().save();
		LogTestSupport.write(new File(store.root(), "index.apdi.tmp"), new byte[]{9, 9, 9});
		RunIndex loaded = RunIndex.load(new File(store.root(), "index.apdi"), store.runsDir());
		assertFalse(loaded.wasRebuilt());
		assertEquals(4, loaded.size());
		loaded.save(); //and saving over it works
		assertFalse(new File(store.root(), "index.apdi.tmp").exists());
	}

	@Test
	public void separateInstancesSerializeTheirAtomicSave() throws Exception {
		File root = tmp.newFolder("concurrent-save");
		File runs = new File(root, "runs");
		assertTrue(runs.mkdirs());
		File indexFile = new File(root, "index.apdi");
		RunIndex first = new RunIndex(indexFile, runs);
		RunIndex second = new RunIndex(indexFile, runs);
		first.upsert(new RunIndexEntry("first", TelemetryVersion.CURRENT, false, "first.apdlog", 1,
				RunIndexEntry.Status.OPEN, 0, 0, 0, 1));
		second.upsert(new RunIndexEntry("second", TelemetryVersion.CURRENT, false, "second.apdlog", 2,
				RunIndexEntry.Status.OPEN, 0, 0, 0, 2));

		CountDownLatch firstOpened = new CountDownLatch(1);
		CountDownLatch releaseFirst = new CountDownLatch(1);
		CountDownLatch secondStarted = new CountDownLatch(1);
		CountDownLatch secondOpened = new CountDownLatch(1);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread a = new Thread(new Runnable(){
			@Override
			public void run(){
				try {
					first.save(new RunIndex.SaveHook(){
						@Override
						public void afterTempOpened( File temp ) throws InterruptedException {
							firstOpened.countDown();
							releaseFirst.await();
						}
					});
				} catch (Throwable t){
					failure.compareAndSet(null, t);
				}
			}
		}, "first-index-save");
		Thread b = new Thread(new Runnable(){
			@Override
			public void run(){
				secondStarted.countDown();
				try {
					second.save(new RunIndex.SaveHook(){
						@Override
						public void afterTempOpened( File temp ){
							secondOpened.countDown();
						}
					});
				} catch (Throwable t){
					failure.compareAndSet(null, t);
				}
			}
		}, "second-index-save");

		a.start();
		assertTrue(firstOpened.await(5, TimeUnit.SECONDS));
		b.start();
		assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
		try {
			assertFalse("a second instance must not open the shared temp file", secondOpened.await(200, TimeUnit.MILLISECONDS));
		} finally {
			releaseFirst.countDown();
		}
		a.join(5000);
		b.join(5000);
		assertFalse(a.isAlive());
		assertFalse(b.isAlive());
		assertNull(failure.get());
		assertTrue(secondOpened.await(1, TimeUnit.SECONDS));
		RunIndex loaded = RunIndex.load(indexFile, runs);
		assertFalse(loaded.wasRebuilt());
		assertEquals(1, loaded.size());
		assertNotNull(loaded.get("second"));
	}

	@Test
	public void refreshTracksAChangingRun() throws Exception {
		StatsStore store = new StatsStore(tmp.newFolder("s2"));
		RunLogWriter w = store.startRun("live", "b", 1);
		assertEquals(0, store.index().get("live").eventCount);
		w.event(LogTestSupport.T_PLAIN).clock(3).unsigned(1).emit();
		w.flush();
		store.refreshIndex("live");
		assertEquals(1, store.index().get("live").eventCount);
		assertEquals(RunIndexEntry.Status.OPEN, store.index().get("live").status);
		w.close();
		store.refreshIndex("live");
		assertEquals(RunIndexEntry.Status.SEALED, store.index().get("live").status);
		assertTrue(store.logFile("live").delete());
		assertNull(store.index().refresh("live"));
		assertNull(store.index().get("live"));
	}

	@Test
	public void refreshRejectsPathLikeRunIdsWithoutReadingOutsideHistory() throws Exception {
		StatsStore store = new StatsStore(tmp.newFolder("index-refresh-boundary"));
		RunLogWriter writer = store.startRun("legit", "test", 1);
		writer.close();
		RunIndex index = store.index();
		assertNotNull(index.refresh("legit"));

		// Reuse genuine log bytes outside runs/ so the unguarded refresh would accept this path.
		File outside = new File(store.root(), "outside.apdlog");
		LogTestSupport.write(outside, LogTestSupport.bytes(store.logFile("legit")));
		byte[] original = LogTestSupport.bytes(outside);
		String[] invalidIds = {"../outside", "bad/name", "", null};
		for (String id : invalidIds){
			boolean rejected = false;
			try {
				index.refresh(id);
			} catch (IOException expected){
				rejected = true;
			}
			assertTrue("invalid run ID must be rejected: " + id, rejected);
			assertFalse("store must report an invalid refresh: " + id, store.refreshIndex(id));
			assertEquals(1, index.size());
			assertNull(index.get(id));
		}
		assertTrue(Arrays.equals(original, LogTestSupport.bytes(outside)));
		assertEquals("legit", index.all().get(0).runId);
	}

	@Test
	public void entryDecoderIgnoresFieldsAddedByLaterVersions() throws Exception {
		RunIndexEntry e = new RunIndexEntry("x", new TelemetryVersion(1, 0), false, "x.apdlog", 5,
				RunIndexEntry.Status.OPEN, 3, 4, 5, 6);
		byte[] enc = e.encode();
		byte[] withExtra = LogTestSupport.concat(enc, new byte[]{42, 43, 44});
		assertEquals(e, RunIndexEntry.decode(withExtra));
	}

	@Test
	public void recoveryNeverTruncatesSymlinkedRunLogs() throws Exception {
		StatsStore store = new StatsStore(tmp.newFolder("recovery-link-boundary"));
		RunLogWriter writer = store.startRun("source", "test", 1);
		for (int i = 0; i < 9; i++) writer.event(LogTestSupport.T_PLAIN).clock(i).unsigned(i).emit();
		writer.close(false);
		byte[] original = LogTestSupport.bytes(store.logFile("source"));
		assertTrue(original.length > 2);
		// Close(false) leaves a healthy open log. Damage its tail explicitly, as in
		// the established recoverAll damaged-log control, before testing recovery.
		LogTestSupport.write(store.logFile("source"), Arrays.copyOf(original, original.length - 2));

		// A valid, damaged run log outside runs/ must remain untouched even if linked
		// into runs/ with a plausible name. The old isFile() check followed this link.
		File external = new File(store.root(), "external.apdlog");
		byte[] damagedExternal = Arrays.copyOf(original, original.length - 2);
		LogTestSupport.write(external, damagedExternal);
		File outsideAlias = new File(store.runsDir(), "outside-alias.apdlog");
		Files.createSymbolicLink(outsideAlias.toPath(), external.toPath());

		// An in-directory alias also must not cause the same log to be repaired twice
		// or cause its damage backup to be written under an unrelated run's name.
		File localAlias = new File(store.runsDir(), "local-alias.apdlog");
		Files.createSymbolicLink(localAlias.toPath(), store.logFile("source").toPath());
		List<LogRecovery.RepairReport> reports = store.recoverAll();
		assertEquals(1, reports.size());
		assertEquals(store.logFile("source"), reports.get(0).file);
		assertTrue("real damaged log remains repairable", reports.get(0).truncated);
		assertTrue("local repair retains damaged tail", reports.get(0).savedTail.isFile());
		assertTrue(Arrays.equals(damagedExternal, LogTestSupport.bytes(external)));
		assertTrue(Files.isSymbolicLink(outsideAlias.toPath()));
		assertTrue(Files.isSymbolicLink(localAlias.toPath()));
		assertFalse("external link may not produce damage sidecars",
				new File(outsideAlias.getPath() + ".tail-" + (original.length - 4) + ".damaged").exists());
		for (File child : store.runsDir().listFiles()){
			assertFalse("external alias created a tail file", child.getName().startsWith("outside-alias.apdlog.tail-"));
			assertFalse("local alias created a tail file", child.getName().startsWith("local-alias.apdlog.tail-"));
		}
		assertNotNull("legitimate source remains indexed", store.index().get("source"));
	}

	@Test
	public void recoverAllRepairsDamagedLogsAndNeverDeletesThem() throws Exception {
		StatsStore store = populated();
		List<LogRecovery.RepairReport> reports = store.recoverAll();
		assertEquals(5, reports.size());
		int truncated = 0;
		for (LogRecovery.RepairReport r : reports){
			assertNull(r.error);
			if (r.truncated) truncated++;
		}
		assertEquals(1, truncated);
		assertEquals(RunIndexEntry.Status.OPEN, store.index().get("c-damaged").status);
		assertEquals(8, store.index().get("c-damaged").eventCount);
		assertEquals(5, store.index().size());
		assertTrue(store.logFile("c-damaged").isFile());
		assertNotNull(store.index().get("e-junk"));
	}
}
