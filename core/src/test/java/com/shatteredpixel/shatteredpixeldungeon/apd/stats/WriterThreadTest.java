package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class WriterThreadTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private final List<RunLogWriter> opened = new ArrayList<>();

	@After
	public void closeEverything(){
		for (RunLogWriter w : opened) w.close(false);
	}

	private RunLogWriter create( String id, RunLogWriter.Options options ){
		RunLogWriter w = RunLogWriter.create(new File(tmp.getRoot(), id + ".apdlog"), TelemetryVersion.CURRENT, id, "t", 0, options);
		opened.add(w);
		return w;
	}

	@Test
	public void flushMakesEverythingVisibleInTheFile() throws Exception {
		RunLogWriter w = create("flush", new RunLogWriter.Options());
		for (int i = 0; i < 1000; i++) assertTrue(w.event(LogTestSupport.T_PLAIN).clock(i).unsigned(i).emit());
		assertTrue(w.flush(10_000, true));
		ReadResult r = RunLogReader.read(w.file());
		assertEquals(1000, r.events.size());
		assertEquals(ReadStatus.UNSEALED, r.status);
		//keep going after a flush
		assertTrue(w.event(LogTestSupport.T_PLAIN).clock(1000).unsigned(1000).emit());
		assertTrue(w.flush());
		assertEquals(1001, RunLogReader.read(w.file()).events.size());
	}

	@Test
	public void closeSealsStopsTheThreadAndRefusesLaterEvents() throws Exception {
		//The writer's OWN thread is checked, never a count of all writer threads: another writer's thread (of an earlier test) may still
		//be finishing when this test starts, so a global count races with it. close() returns only after joining its own thread.
		RunLogWriter w = create("close", new RunLogWriter.Options());
		Thread worker = w.workerThread();
		assertTrue(worker != null && worker.isAlive());
		for (int i = 0; i < 50; i++) w.event(LogTestSupport.T_PLAIN).unsigned(i).emit();
		assertTrue(w.close());
		assertFalse("worker thread must be gone after close", worker.isAlive());

		ReadResult r = RunLogReader.read(w.file());
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(50, r.events.size());

		assertFalse(w.event(LogTestSupport.T_PLAIN).unsigned(1).emit());
		assertFalse(w.append(LogTestSupport.T_PLAIN, 0, 0, new byte[0]));
		assertFalse(w.flush());
		assertTrue("closing twice is harmless", w.close());
		assertEquals(50, RunLogReader.read(w.file()).events.size());
		assertFalse(w.isDisabled());
	}

	//---- persistence: everything accepted before the close is in the file, in order, before the seal; nothing after it is ----

	@Test
	public void everyEventAcceptedBeforeCloseIsPersistedBeforeTheSealAndLaterOnesAreRefused() throws Exception {
		//the writer thread is held back, so ALL events are still queued when close() is called: the worst case for losing them
		CountDownLatch gate = new CountDownLatch(1);
		RunLogWriter.Options o = new RunLogWriter.Options().queueCapacity(1 << 16);
		o.workerGate = gate;
		final RunLogWriter w = create("queued", o);
		for (int i = 0; i < 5000; i++) assertTrue(w.event(LogTestSupport.T_PLAIN).clock(i).unsigned(i).emit());
		Thread closer = new Thread(() -> w.close(true, 20_000));
		closer.start();
		//close() refuses new events as soon as it begins: keep offering until that is seen. Offers that were accepted before it began
		//count too, and must be in the file like the first 5000
		int extra = 0;
		while (w.event(LogTestSupport.T_PLAIN).unsigned(999_999).emit()) extra++;
		gate.countDown();
		closer.join(20_000);
		assertFalse(closer.isAlive());
		assertFalse("the writer thread has stopped", w.workerThread().isAlive());
		ReadResult r = RunLogReader.read(w.file());
		assertEquals("sealed, so the seal came after every event", ReadStatus.SEALED, r.status);
		assertEquals("every event accepted before the close is in the file", 5000 + extra, r.events.size());
		for (int i = 0; i < 5000; i++) assertEquals(i, r.events.get(i).reader(r).unsigned());
		for (int i = 5000; i < 5000 + extra; i++) assertEquals(999_999, r.events.get(i).reader(r).unsigned());
		assertFalse(w.event(LogTestSupport.T_PLAIN).unsigned(1).emit());
	}

	@Test
	public void producersRacingAGapCloseNeverLoseAnAcceptedEvent() throws Exception {
		//Producers keep emitting while another thread closes the writer. However they interleave, every emit() that returned true is in
		//the file when close() has returned, every one that returned false is not, and the log is sealed. Repeated to shake out races.
		for (int round = 0; round < 60; round++){
			final RunLogWriter.Options o = new RunLogWriter.Options().queueCapacity(1 << 18);
			final RunLogWriter w = create("race" + round, o);
			final int producers = 4;
			final CountDownLatch go = new CountDownLatch(1);
			final AtomicInteger accepted = new AtomicInteger();
			final AtomicInteger refusedAfterClose = new AtomicInteger();
			final java.util.concurrent.atomic.AtomicBoolean closeReturned = new java.util.concurrent.atomic.AtomicBoolean();
			List<Thread> ts = new ArrayList<>();
			for (int t = 0; t < producers; t++){
				final int id = t;
				Thread th = new Thread(() -> {
					try { go.await(); } catch (InterruptedException e){ return; }
					for (int i = 0; i < 3000; i++){
						boolean afterClose = closeReturned.get();
						if (w.event(LogTestSupport.T_PLAIN).unsigned(id).emit()){
							accepted.incrementAndGet();
							if (afterClose) refusedAfterClose.incrementAndGet();   //accepted although close() had already returned: must never happen
						}
					}
				});
				ts.add(th);
				th.start();
			}
			Thread closer = new Thread(() -> {
				try { go.await(); } catch (InterruptedException e){ return; }
				w.close(true, 20_000);
				closeReturned.set(true);
			});
			closer.start();
			go.countDown();
			for (Thread th : ts) th.join();
			closer.join();
			assertEquals("an event was accepted after close() returned (round " + round + ")", 0, refusedAfterClose.get());
			assertFalse(w.workerThread().isAlive());
			ReadResult r = RunLogReader.read(w.file());
			assertEquals(ReadStatus.SEALED, r.status);
			assertEquals("accepted events and persisted events differ (round " + round + ")", accepted.get(), r.events.size());
		}
	}

	@Test
	public void closeWithoutSealLeavesAResumableLog() throws Exception {
		RunLogWriter w = create("noseal", new RunLogWriter.Options());
		w.event(LogTestSupport.T_PLAIN).unsigned(1).emit();
		assertTrue(w.close(false));
		assertEquals(ReadStatus.UNSEALED, RunLogReader.read(w.file()).status);
	}

	@Test
	public void concurrentProducersLoseNothingAndKeepPerThreadOrder() throws Exception {
		final int threads = 8, perThread = 2000;
		RunLogWriter.Options o = new RunLogWriter.Options().queueCapacity(1 << 20);
		final RunLogWriter w = create("conc", o);
		final CountDownLatch start = new CountDownLatch(1);
		final AtomicInteger accepted = new AtomicInteger();
		List<Thread> ts = new ArrayList<>();
		for (int t = 0; t < threads; t++){
			final int id = t;
			Thread th = new Thread(() -> {
				try { start.await(); } catch (InterruptedException e){ return; }
				for (int i = 0; i < perThread; i++){
					if (w.event(LogTestSupport.T_STRINGS).unsigned(id).string("thread" + id).signed(i).emit()) accepted.incrementAndGet();
				}
			});
			ts.add(th);
			th.start();
		}
		start.countDown();
		for (Thread th : ts) th.join();
		assertTrue(w.close());
		assertEquals(threads * perThread, accepted.get());

		ReadResult r = RunLogReader.read(w.file());
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(threads * perThread, r.events.size());
		assertEquals(threads, r.strings.size());
		int[] next = new int[threads];
		for (ApdEvent e : r.events){
			PayloadReader p = e.reader(r);
			int id = (int) p.unsigned();
			assertEquals("thread" + id, p.string()); //string definitions always arrive before their first use
			assertEquals(next[id]++, (int) p.signed());
		}
		for (int n : next) assertEquals(perThread, n);
	}

	@Test
	public void aFullQueueDropsEventsCountsThemAndWritesAGap() throws Exception {
		CountDownLatch gate = new CountDownLatch(1);
		RunLogWriter.Options o = new RunLogWriter.Options().queueCapacity(24);
		o.workerGate = gate; //the writer thread is held back, so the queue fills up
		RunLogWriter w = create("full", o);

		int accepted = 0;
		for (int i = 0; i < 200; i++){
			if (w.event(LogTestSupport.T_PLAIN).clock(i).unsigned(i).emit()) accepted++;
		}
		assertTrue(accepted > 0 && accepted < 200);
		assertEquals(accepted, w.eventsAccepted());
		assertEquals(200 - accepted, w.eventsDropped());
		assertFalse("a full queue is not a failure", w.isDisabled());

		gate.countDown();
		assertTrue(w.flush(10_000, false));
		//the next accepted event is preceded by a GAP that says how many were lost
		assertTrue(w.event(LogTestSupport.T_PLAIN).clock(500).unsigned(500).emit());
		assertTrue(w.close());

		ReadResult r = RunLogReader.read(w.file());
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(accepted + 1 /*gap*/ + 1 /*last*/, r.events.size());
		ApdEvent gap = r.events.get(accepted);
		assertEquals(EventTypes.GAP, gap.type);
		PayloadReader p = gap.reader(r);
		assertEquals(EventTypes.GAP_DROPPED_EVENTS, p.unsigned());
		assertEquals(200 - accepted, p.unsigned());
		//the events that were accepted are exactly the first ones, in order
		for (int i = 0; i < accepted; i++) assertEquals(i, r.events.get(i).reader(r).unsigned());
	}

	@Test
	public void droppedEventsAreStillReportedIfTheLogIsClosedFirst() throws Exception {
		CountDownLatch gate = new CountDownLatch(1);
		RunLogWriter.Options o = new RunLogWriter.Options().queueCapacity(16);
		o.workerGate = gate;
		RunLogWriter w = create("dropclose", o);
		int accepted = 0;
		for (int i = 0; i < 100; i++) if (w.event(LogTestSupport.T_PLAIN).unsigned(i).emit()) accepted++;
		long dropped = 100 - accepted;
		assertTrue(dropped > 0);
		gate.countDown();
		assertTrue(w.close());
		ReadResult r = RunLogReader.read(w.file());
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(1, r.gapEvents);
		ApdEvent gap = r.events.get(r.events.size() - 1);
		PayloadReader p = gap.reader(r);
		assertEquals(EventTypes.GAP_DROPPED_EVENTS, p.unsigned());
		assertEquals(dropped, p.unsigned());
	}

	@Test
	public void stringsInADroppedEventDoNotBreakLaterEvents() throws Exception {
		CountDownLatch gate = new CountDownLatch(1);
		RunLogWriter.Options o = new RunLogWriter.Options().queueCapacity(12);
		o.workerGate = gate;
		RunLogWriter w = create("dropstr", o);
		for (int i = 0; i < 60; i++) w.event(LogTestSupport.T_STRINGS).unsigned(i).string("unique-" + i).signed(i).emit();
		gate.countDown();
		w.flush();
		for (int i = 100; i < 105; i++){
			assertTrue(w.event(LogTestSupport.T_STRINGS).unsigned(i).string("late-" + i).signed(i).emit());
			assertTrue(w.flush()); //keeps the tiny queue empty so these are not dropped by a race with the writer thread
		}
		assertTrue(w.close());
		ReadResult r = RunLogReader.read(w.file());
		assertEquals(ReadStatus.SEALED, r.status);
		//every string reference in every surviving event resolves
		for (ApdEvent e : r.events){
			if (e.type != LogTestSupport.T_STRINGS) continue;
			PayloadReader p = e.reader(r);
			p.unsigned();
			assertTrue(p.string() != null);
		}
	}

	@Test
	public void manyOpenAndCloseCyclesLeakNoThreads() throws Exception {
		for (int i = 0; i < 30; i++){
			RunLogWriter w = create("cycle" + i, new RunLogWriter.Options());
			Thread worker = w.workerThread();
			w.event(LogTestSupport.T_PLAIN).unsigned(i).emit();
			assertTrue(w.close());
			assertFalse("cycle " + i + " left its writer thread running", worker.isAlive());
		}
	}

	@Test
	public void closeWithATimeoutDoesNotHoldTheCallerWhenTheWriterThreadIsStuck() throws Exception {
		CountDownLatch gate = new CountDownLatch(1);
		RunLogWriter.Options o = new RunLogWriter.Options();
		o.workerGate = gate; //the writer thread is "stuck"
		RunLogWriter w = create("stuck", o);
		w.event(LogTestSupport.T_PLAIN).unsigned(1).emit();
		long start = System.currentTimeMillis();
		w.close(true, 150);
		long took = System.currentTimeMillis() - start;
		assertTrue("close must give up waiting: took " + took + " ms", took < 3000);

		//when the thread recovers it still finishes the job: events written and the log sealed
		gate.countDown();
		ReadResult r = null;
		for (int i = 0; i < 100; i++){
			r = RunLogReader.read(w.file());
			if (r.status == ReadStatus.SEALED) break;
			Thread.sleep(50);
		}
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(1, r.events.size());
		//and its thread ends: wait for exactly that thread, so it is not still finishing when the next test starts
		w.workerThread().join(10_000);
		assertFalse(w.workerThread().isAlive());
	}
}
