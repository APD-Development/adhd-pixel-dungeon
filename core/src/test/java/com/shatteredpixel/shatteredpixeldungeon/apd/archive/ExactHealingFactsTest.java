package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFactsBuilder;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;

//What exact healing (telemetry 1.6) becomes in the derived facts: exact when nothing was guessed, honest about the part that was, a lower
//bound when events were lost, unknown (never zero) where a log cannot say, and nothing hidden in what a redacted copy can show.
public class ExactHealingFactsTest {

	private static final String RS = "r1-aaaaaaaaaaaa";

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private TestLogs log( String id, TelemetryVersion v ) throws Exception {
		return new TestLogs(tmp.getRoot(), id, v, 1000).start("run-" + id, 0, "WARRIOR", RS, 0, 30, false).enter(1);
	}

	private static RunFacts facts( File f, String id ) throws Exception {
		return RunFactsBuilder.fromLog(RunLogReader.read(f), id);
	}

	private static long from( RunFacts f, String category ){
		return f.numOr(FactKeys.healedFrom(category), -99);
	}

	@Test
	public void aRunWhereEveryHealWasReportedHasExactTotalsAndPerSourceTotals() throws Exception {
		File f = log("exact", TelemetryVersion.CURRENT).heal(3, true, "regen").heal(5, true, "food.pasty").heal(2, true, "potion.healing")
				.heal(4, true, "regen").end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 10).close(true);
		RunFacts r = facts(f, "exact");
		assertEquals(14, r.numOr(FactKeys.HEALED, -1));
		assertEquals("nothing was guessed, so the total is recorded, not inferred", Fact.PROV_RECORDED, r.get(FactKeys.HEALED).prov);
		assertEquals(0, r.numOr(FactKeys.HEALED_INFERRED, -1));
		assertEquals(7, from(r, CombatEvents.HC_REGEN));
		assertEquals(5, from(r, CombatEvents.HC_FOOD));
		assertEquals(2, from(r, CombatEvents.HC_POTION));
		assertEquals("a kind that never healed is a recorded zero in a run that could report it", 0, from(r, CombatEvents.HC_WAND));
	}

	@Test
	public void aHealThatWasOnlyInferredKeepsTheTotalHonestAndIsCountedSeparately() throws Exception {
		File f = log("mixed", TelemetryVersion.CURRENT).heal(3, true, "regen").heal(4, false, "").heal(6, true, "plant.sungrass")
				.end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 10).close(true);
		RunFacts r = facts(f, "mixed");
		assertEquals(13, r.numOr(FactKeys.HEALED, -1));
		assertEquals("part of it was guessed", Fact.PROV_INFERRED, r.get(FactKeys.HEALED).prov);
		assertEquals(4, r.numOr(FactKeys.HEALED_INFERRED, -1));
		assertEquals(3, from(r, CombatEvents.HC_REGEN));
		assertEquals(6, from(r, CombatEvents.HC_PLANT));
		long attributed = 0;
		for (String c : CombatEvents.HEAL_CATEGORIES) attributed += from(r, c);
		assertEquals("the exact part is fully attributed to sources", 13 - 4, attributed);
	}

	@Test
	public void runsFromBeforeExactHealingKeepTheirInferredHealingAndHaveNoSourceFacts() throws Exception {
		for (TelemetryVersion v : new TelemetryVersion[]{ new TelemetryVersion(1, 3), new TelemetryVersion(1, 5) }){
			String id = "old" + v.minor;
			File f = log(id, v).heal(3, false, "").heal(5, false, "").end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 10).close(true);
			RunFacts r = facts(f, id);
			assertEquals(8, r.numOr(FactKeys.HEALED, -1));
			assertEquals(Fact.PROV_INFERRED, r.get(FactKeys.HEALED).prov);
			assertEquals(8, r.numOr(FactKeys.HEALED_INFERRED, -1));
			for (String c : CombatEvents.HEAL_CATEGORIES) assertFalse("unknown, never zero: " + c, r.isKnown(FactKeys.healedFrom(c)));
		}
		//and before combat was recorded at all there is nothing about healing
		File f = log("older", new TelemetryVersion(1, 2)).end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 10).close(true);
		assertFalse(facts(f, "older").isKnown(FactKeys.HEALED));
	}

	@Test
	public void lostEventsMakeEveryHealingNumberALowerBound() throws Exception {
		File f = log("gappy", TelemetryVersion.CURRENT).heal(3, true, "regen").gap().heal(2, true, "regen")
				.end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 10).close(true);
		RunFacts r = facts(f, "gappy");
		assertEquals(Fact.PROV_INCOMPLETE, r.get(FactKeys.HEALED).prov);
		assertEquals(Fact.PROV_INCOMPLETE, r.get(FactKeys.healedFrom(CombatEvents.HC_REGEN)).prov);
	}

	@Test
	public void aSourceFromANewerBuildCountsAsOtherAndNeverBreaksTheRun() throws Exception {
		File f = log("future", TelemetryVersion.CURRENT).heal(4, true, "gadget.futureThing").heal(1, true, "regen")
				.end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 10).close(true);
		RunFacts r = facts(f, "future");
		assertEquals(4, from(r, CombatEvents.HC_OTHER));
		assertEquals(1, from(r, CombatEvents.HC_REGEN));
		assertEquals(5, r.numOr(FactKeys.HEALED, -1));
	}

	@Test
	public void aRedactedCopyKeepsTheHealedAmountsButNothingAboutWhatHealed() throws Exception {
		File f = log("open", TelemetryVersion.CURRENT).heal(3, true, "gear.vampiric").heal(5, true, "potion.healing").close(false);
		byte[] raw = Files.readAllBytes(f.toPath());
		assertTrue("the raw log does hold the sources", ArchiveSupport.contains(raw, "gear.vampiric"));

		LogRedactor.Result red = LogRedactor.redact(raw);
		assertFalse("no source name survives in the copy", ArchiveSupport.contains(red.bytes, "vampiric"));
		assertFalse(ArchiveSupport.contains(red.bytes, "potion.healing"));
		ReadResult rr = RunLogReader.parse(red.bytes);
		int heals = 0;
		for (ApdEvent e : rr.events){
			assertFalse("the hidden detail events are left out", e.type == EventTypes.HEAL_DETAIL);
			if (e.type == EventTypes.HEAL){
				heals++;
				assertEquals("the visible heals stay exact", 0, e.flags & CombatEvents.FLAG_INFERRED);
			}
		}
		assertEquals(2, heals);

		RunFacts r = RunFactsBuilder.fromLog(rr, "open");
		assertEquals("how much was healed is something the player saw", 8, r.numOr(FactKeys.HEALED, -1));
		assertEquals(Fact.PROV_RECORDED, r.get(FactKeys.HEALED).prov);
		for (String c : CombatEvents.HEAL_CATEGORIES){
			assertFalse("a redacted copy cannot say what healed, and must not say zero: " + c, r.isKnown(FactKeys.healedFrom(c)));
		}
	}
}
