/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.DerivedStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactTable;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CacheConcurrencyTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private void apdRun( String id, long score ) throws Exception {
		File f = new File(new File(tmp.getRoot(), "runs"), id + ".apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, id, "test", 1);
		LifecycleEvents.RunStart start = new LifecycleEvents.RunStart();
		start.runId = id;
		start.heroClass = "WARRIOR";
		LifecycleEvents.emitRunStart(w, 0, false, start);
		LifecycleEvents.RunEnd end = new LifecycleEvents.RunEnd();
		end.outcome = LifecycleEvents.OUTCOME_DIED;
		end.deepest = 5;
		end.heroLevel = 8;
		end.score = score;
		end.depth = 5;
		end.turns = 100;
		LifecycleEvents.emitRunEnd(w, 5, end);
		w.close(true);
	}

	private <T> List<T> contend( final Callable<T> operation ) throws Exception {
		final int workers = 4;
		final CountDownLatch ready = new CountDownLatch(workers);
		final CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(workers);
		List<Future<T>> futures = new ArrayList<>();
		try {
			for (int i = 0; i < workers; i++){
				futures.add(pool.submit(new Callable<T>(){
					@Override
					public T call() throws Exception {
						ready.countDown();
						assertTrue("all cache writers started", start.await(10, TimeUnit.SECONDS));
						T result = null;
						for (int n = 0; n < 12; n++) result = operation.call();
						return result;
					}
				}));
			}
			assertTrue("all cache writers became ready", ready.await(10, TimeUnit.SECONDS));
			start.countDown();
			List<T> results = new ArrayList<>();
			for (Future<T> future : futures) results.add(future.get(30, TimeUnit.SECONDS));
			return results;
		} finally {
			start.countDown();
			pool.shutdownNow();
			assertTrue("cache workers stopped", pool.awaitTermination(10, TimeUnit.SECONDS));
		}
	}

	@Test
	public void derivedCacheSurvivesIndependentWriters() throws Exception {
		apdRun("r1", 2000);
		final File root = tmp.getRoot();
		for (FactTable table : contend(new Callable<FactTable>(){
			@Override
			public FactTable call(){
				return new DerivedStore(root).rebuild();
			}
		})){
			assertEquals(1, table.apdRows.size());
			assertEquals(2000, table.apdRows.get(0).numOr(FactKeys.SCORE, -1));
		}

		DerivedStore finalStore = new DerivedStore(root);
		assertEquals(1, finalStore.load().apdRows.size());
		assertEquals("the surviving cache remains readable", DerivedStore.Source.CACHE, finalStore.lastSource());
	}

	@Test
	public void rowCacheSurvivesIndependentWriters() throws Exception {
		apdRun("r1", 2000);
		final File root = tmp.getRoot();
		for (Catalog catalog : contend(new Callable<Catalog>(){
			@Override
			public Catalog call(){
				new DatasetStore(root).deleteCache();
				return new DatasetStore(root).load();
			}
		})){
			assertEquals(1, catalog.dataset(Datasets.RUNS).rows.size());
		}

		Catalog cached = new DatasetStore(root).load();
		assertEquals(1, cached.dataset(Datasets.RUNS).rows.size());
		assertEquals("the surviving per-log cache remains readable", 1, cached.logsCached);
		assertEquals(0, cached.logsRebuilt);
	}
    @Test public void clearingRowsDoesNotDestroyAnotherWritersCompletedStagingFile()throws Exception{
        apdRun("r1",2000);DatasetStore store=new DatasetStore(tmp.getRoot());store.load();
        java.lang.reflect.Method directory=DatasetStore.class.getDeclaredMethod("rowsDir");directory.setAccessible(true);
        File[] rows=((File)directory.invoke(store)).listFiles((d,n)->n.endsWith(".apdr"));assertNotNull(rows);assertEquals(1,rows.length);
        byte[] data=java.nio.file.Files.readAllBytes(rows[0].toPath());
        final CountDownLatch staged=new CountDownLatch(1),publish=new CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
        File destination=new File(rows[0].getPath()){
            @Override public boolean exists(){
                staged.countDown();try{assertTrue("staged writer released",publish.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}
                return super.exists();
            }
        };
        java.lang.reflect.Method write=DatasetStore.class.getDeclaredMethod("writeCache",File.class,byte[].class);write.setAccessible(true);
        Thread writer=new Thread(()->{try{write.invoke(store,destination,data);}catch(Throwable t){error.set(t);}},"cache-staged-writer");writer.setDaemon(true);writer.start();
        try{assertTrue("writer has completed staging bytes",staged.await(10,TimeUnit.SECONDS));store.deleteCache();}
        finally{publish.countDown();writer.join(10000);}
        assertFalse(writer.isAlive());assertNull(error.get());Catalog cached=new DatasetStore(tmp.getRoot()).load();
        assertEquals("completed writer still publishes a readable cache after clear",1,cached.logsCached);assertEquals(0,cached.logsRebuilt);
    }

}
