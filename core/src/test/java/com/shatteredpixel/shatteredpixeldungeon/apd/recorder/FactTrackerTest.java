package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKind;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKinds;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//The recorder side of the generic facts: every observation is kept (series are only packed), nothing is written until it can be
//rolled back consistently, and a careless call cannot produce a payload of the wrong shape.
public class FactTrackerTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final FactKind STEP = FactKinds.register(FactKind.builder("test.ft.step", "Step").value("cell", "Cell").value("depth", "Depth").series().build());
	private static final FactKind CAST = FactKinds.register(FactKind.builder("test.ft.cast", "Cast").subject("Spell").value("mana", "Mana").tag("outcome", "Outcome").build());

	private RunLogWriter writer(){
		return RunLogWriter.create(new File(tmp.getRoot(), "t.apdlog"), TelemetryVersion.CURRENT, "t", "test", 1);
	}

	private List<FactEvents.Observation> read( RunLogWriter w ) throws Exception {
		w.close(false, 5000);
		ReadResult r = RunLogReader.read(new File(tmp.getRoot(), "t.apdlog"));
		List<FactEvents.Observation> out = new ArrayList<>();
		for (ApdEvent e : r.events) if (FactEvents.isFactEvent(e.type)) out.addAll(FactEvents.decode(e, r));
		return out;
	}

	@Test
	public void everyStepOfALongWalkIsKeptAcrossTheAutomaticPieces() throws Exception {
		RunLogWriter w = writer();
		FactTracker t = new FactTracker(w);
		int n = 5000;   // more than several automatic flushes
		for (int i = 0; i < n; i++) t.record(STEP, 100 + i / 2, "", "", new long[]{ 1000 + (i * 7) % 33, 3 }, null);
		t.flushAll();
		List<FactEvents.Observation> got = read(w);
		assertEquals(n, got.size());
		for (int i = 0; i < n; i++){
			assertEquals(100 + i / 2, got.get(i).clock);
			assertArrayEquals(new long[]{ 1000 + (i * 7) % 33, 3 }, got.get(i).values);
		}
	}

	@Test
	public void nothingOfASeriesIsWrittenUntilItIsFlushedAndFlushingTwiceAddsNothing() throws Exception {
		RunLogWriter w = writer();
		FactTracker t = new FactTracker(w);
		for (int i = 0; i < 10; i++) t.record(STEP, i, "", "", new long[]{ i, 1 }, null);
		t.flushAll();
		t.flushAll();
		assertEquals(10, read(w).size());
	}

	@Test
	public void aPlainKindIsWrittenAtOnceWithItsSubjectAndTags() throws Exception {
		RunLogWriter w = writer();
		FactTracker t = new FactTracker(w);
		t.record(CAST, 7, "GuidingLight", "", new long[]{ 3 }, new String[]{ "ok" });
		List<FactEvents.Observation> got = read(w);
		assertEquals(1, got.size());
		assertEquals("GuidingLight", got.get(0).subject);
		assertArrayEquals(new String[]{ "ok" }, got.get(0).tags);
	}

	@Test
	public void aCarelessCallIsFittedToTheDeclaredShape() throws Exception {
		RunLogWriter w = writer();
		FactTracker t = new FactTracker(w);
		t.record(CAST, 1, "x", null, new long[]{ 3, 99, 100 }, new String[]{ "a", "b", "c" });   // too many
		t.record(CAST, 2, "y", null, null, null);                                                  // none at all
		List<FactEvents.Observation> got = read(w);
		assertEquals(2, got.size());
		assertArrayEquals(new long[]{ 3 }, got.get(0).values);
		assertArrayEquals(new String[]{ "a" }, got.get(0).tags);
		assertArrayEquals(new long[]{ 0 }, got.get(1).values);
		assertArrayEquals(new String[]{ "" }, got.get(1).tags);
		assertTrue(got.get(1).actor.isEmpty());
	}
}
