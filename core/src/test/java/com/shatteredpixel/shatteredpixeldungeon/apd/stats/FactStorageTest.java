package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;
import java.util.Random;

//The storage cost of keeping high-frequency facts at FULL resolution (every step, every rest). These bounds are the gate that lets APD keep
//the raw history instead of a summary: if a change makes full resolution too expensive this fails and the trade-off has to be looked at.
public class FactStorageTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final int WIDTH = 32;

	//a hero walking about a 32-wide floor: mostly straight runs, now and then a turn, sometimes standing still for a few turns
	private static long[][] walk( int steps, long seed ){
		Random rnd = new Random(seed);
		long[][] rows = new long[steps][];
		int cell = 500;
		int[] dirs = { 1, -1, WIDTH, -WIDTH, WIDTH + 1, WIDTH - 1, -WIDTH + 1, -WIDTH - 1 };
		int dir = 0;
		for (int i = 0; i < steps; i++){
			if (rnd.nextInt(8) == 0) dir = rnd.nextInt(dirs.length);
			cell = Math.max(33, Math.min(WIDTH * WIDTH - 33, cell + dirs[dir]));
			rows[i] = new long[]{ cell, WIDTH, rnd.nextInt(10) < 8 ? 1 : 0 };
		}
		return rows;
	}

	@Test
	public void everyStepOfALongRunCostsAFewBytesAndComesBackExactly() throws Exception {
		int steps = 20000;
		long[][] rows = walk(steps, 11);
		long[] clocks = new long[steps];
		long c = 0;
		Random rnd = new Random(3);
		for (int i = 0; i < steps; i++){
			c += rnd.nextInt(10) == 0 ? 2 : 1;      //an occasional slow step
			clocks[i] = c;
		}
		File f = new File(tmp.getRoot(), "walk.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "walk", "test", 1);
		int piece = 512;
		for (int at = 0; at < steps; at += piece){
			int n = Math.min(piece, steps - at);
			long[] cl = new long[n];
			long[][] rr = new long[n][];
			System.arraycopy(clocks, at, cl, 0, n);
			System.arraycopy(rows, at, rr, 0, n);
			assertTrue(FactEvents.emitSeries(w, EventTypes.FACT_SERIES, "move.step", "", "", new String[0], 3, cl, rr, n));
		}
		w.close(false, 5000);
		double perStep = f.length() / (double) steps;
		System.out.println("APDSTORAGE steps: " + steps + " steps cost " + f.length() + " bytes = " + String.format("%.2f", perStep) + " bytes per step");
		assertTrue("a step must stay cheap, was " + perStep + " bytes", perStep <= 3.5);

		ReadResult r = RunLogReader.read(f);
		int got = 0;
		for (ApdEvent e : r.events){
			if (!FactEvents.isFactEvent(e.type)) continue;
			for (FactEvents.Observation o : FactEvents.decode(e, r)){
				assertEquals(clocks[got], o.clock);
				assertEquals(rows[got][0], o.values[0]);
				assertEquals(rows[got][1], o.values[1]);
				assertEquals(rows[got][2], o.values[2]);
				got++;
			}
		}
		assertEquals("nothing was dropped or summarised", steps, got);
	}

	@Test
	public void aWholeLongRunOfFactsStaysSmall() throws Exception {
		//20,000 turns: 12,000 steps, 6,000 rest turns, 500 item actions, 300 effect changes, 200 gold changes
		File f = new File(tmp.getRoot(), "all.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "all", "test", 1);
		long[][] steps = walk(12000, 5);
		long[] cl = new long[12000];
		for (int i = 0; i < cl.length; i++) cl[i] = i + i / 2;
		for (int at = 0; at < 12000; at += 512){
			int n = Math.min(512, 12000 - at);
			long[] c2 = new long[n];
			long[][] r2 = new long[n][];
			System.arraycopy(cl, at, c2, 0, n);
			System.arraycopy(steps, at, r2, 0, n);
			FactEvents.emitSeries(w, EventTypes.FACT_SERIES, "move.step", "", "", new String[0], 3, c2, r2, n);
		}
		long[] rc = new long[6000];
		long[][] rr = new long[6000][];
		for (int i = 0; i < 6000; i++){ rc[i] = 20000 + i * 2; rr[i] = new long[]{ i % 3 == 0 ? 0 : 1 }; }
		for (int at = 0; at < 6000; at += 512){
			int n = Math.min(512, 6000 - at);
			long[] c2 = new long[n];
			long[][] r2 = new long[n][];
			System.arraycopy(rc, at, c2, 0, n);
			System.arraycopy(rr, at, r2, 0, n);
			FactEvents.emitSeries(w, EventTypes.FACT_SERIES, "explore.rest", "", "", new String[0], 1, c2, r2, n);
		}
		Random rnd = new Random(9);
		for (int i = 0; i < 500; i++) FactEvents.emit(w, EventTypes.FACT, 100 + i * 30, new FactEvents.Observation("item.use", "Potion of Healing", "", 0, new long[]{ 1 }, new String[]{ "DRINK" }));
		for (int i = 0; i < 500; i++) FactEvents.emit(w, EventTypes.FACT_INTERNAL, 100 + i * 30, new FactEvents.Observation("item.useTruth", "PotionOfHealing", "", 0, new long[]{ 0, -1 }, new String[]{ "DRINK" }));
		for (int i = 0; i < 300; i++) FactEvents.emit(w, EventTypes.FACT, 100 + i * 60, new FactEvents.Observation("status.start", "Haste", "", 0, new long[]{ 10 }, new String[0]));
		for (int i = 0; i < 200; i++) FactEvents.emit(w, EventTypes.FACT, 100 + i * 90, new FactEvents.Observation("economy.gold", "", "", 0, new long[]{ rnd.nextInt(50), 1000 + i }, new String[]{ "pickup" }));
		w.close(false, 5000);
		System.out.println("APDSTORAGE whole run of facts: " + f.length() + " bytes");
		assertTrue("all the new facts of a 20,000 turn run should stay under 120 KiB, was " + f.length(), f.length() < 120 * 1024);
	}

	@Test
	public void theHeroStateOfEveryTurnOfALongRunStaysCheap() throws Exception {
		int turns = 20000;
		Random rnd = new Random(21);
		long[] clocks = new long[turns];
		long[][] rows = new long[turns][];
		long hp = 30, hunger = 0;
		for (int i = 0; i < turns; i++){
			clocks[i] = i + 1;
			if (rnd.nextInt(6) == 0) hp = Math.max(1, Math.min(30, hp + rnd.nextInt(7) - 3));   //fights and regeneration
			hunger = (hunger + 1) % 1800;                                                      //eating now and then
			rows[i] = new long[]{ hp, 30, i % 500 < 40 ? 10 : 0, hunger };
		}
		File f = new File(tmp.getRoot(), "state.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "state", "test", 1);
		for (int at = 0; at < turns; at += 512){
			int n = Math.min(512, turns - at);
			long[] cl = new long[n];
			long[][] rr = new long[n][];
			System.arraycopy(clocks, at, cl, 0, n);
			System.arraycopy(rows, at, rr, 0, n);
			assertTrue(FactEvents.emitSeries(w, EventTypes.FACT_SERIES, "hero.state", "", "", new String[0], 4, cl, rr, n));
		}
		w.close(false, 5000);
		double perTurn = f.length() / (double) turns;
		System.out.println("APDSTORAGE hero state: " + turns + " turns cost " + f.length() + " bytes = " + String.format("%.2f", perTurn) + " bytes per turn");
		assertTrue("the hero's state must stay cheap, was " + perTurn + " bytes per turn", perTurn <= 2.0);
		ReadResult r = RunLogReader.read(f);
		int got = 0;
		for (ApdEvent e : r.events){
			if (!FactEvents.isFactEvent(e.type)) continue;
			for (FactEvents.Observation o : FactEvents.decode(e, r)){
				assertEquals(rows[got][0], o.values[0]);
				assertEquals(rows[got][3], o.values[3]);
				got++;
			}
		}
		assertEquals("every turn is there, none summarised", turns, got);
	}
}
