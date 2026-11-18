package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

//How much log exact healing adds. Natural regeneration is the biggest source of heals (one HP every few turns), so a long run is written
//three ways: no heals, inferred heals (what telemetry 1.3 to 1.5 did), and exact heals with their hidden detail (1.6). The numbers are
//printed for the phase report; the bounds below are the budget.
public class HealingVolumeTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String RS = "r1-aaaaaaaaaaaa";
	private static final int TURNS = 20000;
	private static final int REGEN_EVERY = 9;          //about one HP every 9 turns at the start of a run (faster later), natural regeneration

	private long size( String id, int mode ) throws Exception {
		TestLogs l = new TestLogs(tmp.newFolder(id), id, TelemetryVersion.CURRENT, 1000).start("run-" + id, 0, "WARRIOR", RS, 0, 30, false).enter(1);
		for (int t = 1; t <= TURNS; t++){
			l.tick(1);
			if (t % REGEN_EVERY == 0){
				if (mode == 1) l.heal(1, false, "");
				else if (mode == 2) l.heal(1, true, "regen");
			}
			if (t % 400 == 0) l.heroAttacks("rat", "actors.mobs.Rat", true).damageFromHero("rat", "actors.mobs.Rat", 3);   //a little other traffic
		}
		File f = l.end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 10).close(true);
		return f.length();
	}

	@Test
	public void exactHealingCostsFewBytesPerHealAndLittlePerRun() throws Exception {
		long none = size("none", 0), inferred = size("inferred", 1), exact = size("exact", 2);
		int heals = TURNS / REGEN_EVERY;
		double perInferred = (inferred - none) / (double) heals, perExact = (exact - none) / (double) heals;
		System.out.println("APD-PERF heal volume: " + TURNS + " turns, " + heals + " regeneration heals: no heals " + none / 1024 + " KiB, inferred heals "
				+ inferred / 1024 + " KiB (" + String.format("%.1f", perInferred) + " B per heal), exact heals + detail " + exact / 1024 + " KiB ("
				+ String.format("%.1f", perExact) + " B per heal)");
		assertTrue("an exact heal and its detail must stay small: " + perExact, perExact < 40);
		assertTrue("exact healing may cost at most about twice what the inferred events cost: " + perExact + " vs " + perInferred, perExact < perInferred * 2.0);
		assertTrue("healing of a 20,000-turn run must stay under 100 KiB", (exact - none) < 100 * 1024);
	}
}
