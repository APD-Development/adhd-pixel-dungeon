package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;
import java.util.Random;

//The generic fact events (telemetry 1.8): a FACT is one observation, a SERIES is many observations of one kind written compactly. A series
//expands to EXACTLY the observations that went in (nothing summarised), and a damaged payload is refused, never trusted.
public class FactEventsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private RunLogWriter writer( String id ){
		return RunLogWriter.create(new File(tmp.getRoot(), id + ".apdlog"), TelemetryVersion.CURRENT, id, "test", 1);
	}

	private static List<ApdEvent> factEvents( ReadResult r ){
		List<ApdEvent> out = new java.util.ArrayList<>();
		for (ApdEvent e : r.events) if (FactEvents.isFactEvent(e.type)) out.add(e);
		return out;
	}

	@Test
	public void aSingleFactRoundTripsWithEverything() throws Exception {
		RunLogWriter w = writer("one");
		FactEvents.Observation o = new FactEvents.Observation("test.one", "GuidingLight", "Cleric", 42, new long[]{ 7, -3, 1L << 40 }, new String[]{ "a", "", "c" });
		assertTrue(FactEvents.emit(w, EventTypes.FACT, 42, o));
		w.close(false, 5000);
		ReadResult r = RunLogReader.read(new File(tmp.getRoot(), "one.apdlog"));
		List<ApdEvent> es = factEvents(r);
		assertEquals(1, es.size());
		assertEquals(EventTypes.FACT, es.get(0).type);
		List<FactEvents.Observation> back = FactEvents.decode(es.get(0), r);
		assertEquals(1, back.size());
		FactEvents.Observation b = back.get(0);
		assertEquals("test.one", b.key);
		assertEquals("GuidingLight", b.subject);
		assertEquals("Cleric", b.actor);
		assertEquals(42, b.clock);
		assertArrayEquals(new long[]{ 7, -3, 1L << 40 }, b.values);
		assertArrayEquals(new String[]{ "a", "", "c" }, b.tags);
	}

	@Test
	public void aSeriesExpandsToExactlyTheObservationsThatWentIn() throws Exception {
		Random rnd = new Random(5);
		int n = 3000, nv = 3;
		long[] clocks = new long[n];
		long[][] rows = new long[n][nv];
		long c = 1000;
		for (int i = 0; i < n; i++){
			c += rnd.nextInt(4);   //several steps may share a clock value
			clocks[i] = c;
			rows[i][0] = rnd.nextInt(1000);          //jumps both ways
			rows[i][1] = i == 0 ? 17 : rows[i - 1][1] + rnd.nextInt(3) - 1;   //slow drift
			rows[i][2] = rnd.nextLong() >> 8;        //large values
		}
		RunLogWriter w = writer("series");
		assertTrue(FactEvents.emitSeries(w, EventTypes.FACT_SERIES, "test.steps", "", "", new String[]{ "t" }, nv, clocks, rows, n));
		w.close(false, 5000);
		ReadResult r = RunLogReader.read(new File(tmp.getRoot(), "series.apdlog"));
		List<ApdEvent> es = factEvents(r);
		assertEquals(1, es.size());
		List<FactEvents.Observation> back = FactEvents.decode(es.get(0), r);
		assertEquals(n, back.size());
		for (int i = 0; i < n; i++){
			assertEquals("clock " + i, clocks[i], back.get(i).clock);
			assertArrayEquals("row " + i, rows[i], back.get(i).values);
			assertArrayEquals(new String[]{ "t" }, back.get(i).tags);
			assertEquals("test.steps", back.get(i).key);
		}
	}

	@Test
	public void aSeriesOfSlowlyChangingStepsIsMuchSmallerThanSeparateEvents(){
		//a hero walking: one cell at a time, about one clock tick per step
		int n = 4000;
		long[] clocks = new long[n];
		long[][] rows = new long[n][2];
		for (int i = 0; i < n; i++){
			clocks[i] = 100 + i;
			rows[i][0] = 300 + (i % 20) - (i % 7);
			rows[i][1] = 1 + (i / 50);
		}
		RunLogWriter w = writer("small");
		FactEvents.emitSeries(w, EventTypes.FACT_SERIES, "test.walk", "", "", new String[0], 2, clocks, rows, n);
		w.close(false, 5000);
		File f = new File(tmp.getRoot(), "small.apdlog");
		long perStep = f.length() / n;
		assertTrue("a series costs a few bytes per step, was " + perStep, perStep <= 4);
	}

	@Test
	public void internalFactsAreHiddenByTheirEventType(){
		assertEquals(EventTypes.Visibility.PUBLIC, EventTypes.visibility(EventTypes.FACT));
		assertEquals(EventTypes.Visibility.PUBLIC, EventTypes.visibility(EventTypes.FACT_SERIES));
		assertEquals(EventTypes.Visibility.INTERNAL, EventTypes.visibility(EventTypes.FACT_INTERNAL));
		assertEquals(EventTypes.Visibility.INTERNAL, EventTypes.visibility(EventTypes.FACT_SERIES_INTERNAL));
	}

	@Test
	public void aDamagedPayloadIsRefusedNotTrusted() throws Exception {
		RunLogWriter w = writer("bad");
		//a series claiming a billion rows, and a fact claiming a million values
		w.event(EventTypes.FACT_SERIES).clock(1).string("test.x").string("").string("").unsigned(0).unsigned(2).unsigned(1_000_000_000L).emit();
		w.event(EventTypes.FACT).clock(2).string("test.x").string("").string("").unsigned(1_000_000).emit();
		w.event(EventTypes.FACT).clock(3).string("test.x").emit();   //truncated
		w.close(false, 5000);
		ReadResult r = RunLogReader.read(new File(tmp.getRoot(), "bad.apdlog"));
		int refused = 0;
		for (ApdEvent e : factEvents(r)){
			try {
				FactEvents.decode(e, r);
				fail("accepted a damaged payload");
			} catch (RuntimeException expected){
				refused++;
			}
		}
		assertEquals(3, refused);
	}

	@Test
	public void emptySeriesWritesNothing() throws Exception {
		RunLogWriter w = writer("empty");
		assertTrue(FactEvents.emitSeries(w, EventTypes.FACT_SERIES, "test.e", "", "", new String[0], 1, new long[0], new long[0][], 0));
		w.close(false, 5000);
		assertFalse(factEvents(RunLogReader.read(new File(tmp.getRoot(), "empty.apdlog"))).size() > 0);
	}
    @Test public void seriesPreservesExtremeSignedValuesAndDeltaOverflow() throws Exception {
        int n=512;long[] clocks=new long[n];long[][] rows=new long[n][4];
        for(int i=0;i<n;i++) {
            clocks[i]=i;
            rows[i]=new long[]{i%2==0?Long.MIN_VALUE:Long.MAX_VALUE,
                    Long.MAX_VALUE-i, Long.MIN_VALUE+i, i/32};
        }
        RunLogWriter w=writer("extremes");
        assertTrue(FactEvents.emitSeries(w,EventTypes.FACT_SERIES,"test.extremes","","",new String[0],4,clocks,rows,n));
        w.close(false,5000);
        ReadResult read=RunLogReader.read(new File(tmp.getRoot(),"extremes.apdlog"));
        java.util.List<ApdEvent> encoded=factEvents(read);assertEquals(1,encoded.size());
        java.util.List<FactEvents.Observation> decoded=FactEvents.decode(encoded.get(0),read);
        assertEquals(n,decoded.size());
        for(int i=0;i<n;i++) {assertEquals(clocks[i],decoded.get(i).clock);assertArrayEquals(rows[i],decoded.get(i).values);}
    }

}
