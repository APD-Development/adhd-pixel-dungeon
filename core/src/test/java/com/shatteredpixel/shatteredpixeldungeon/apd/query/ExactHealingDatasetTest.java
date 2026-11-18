package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

//Exact healing in UIMSD: the amounts are public, the sources are hidden information (an unidentified potion or enchantment would
//otherwise give itself away), and runs that cannot say what healed show unknown, never zero.
public class ExactHealingDatasetTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String RS = "r1-aaaaaaaaaaaa";

	private SyntheticLog log( String id, String runId, int segment, TelemetryVersion v ){
		return new SyntheticLog(tmp.getRoot(), id, v, 100).start(runId, segment, "WARRIOR", RS, 0, 30, false).enter(1);
	}

	private Dataset runs(){
		return new DatasetStore(tmp.getRoot()).load().dataset(Datasets.RUNS);
	}

	private static Query q(){
		return Query.of(Datasets.RUNS).withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true)).withShowInternal(true);
	}

	private static CellText cell( Dataset ds, Row r, String col ){
		return CellFormatter.cell(q(), r, ds.def.column(col));
	}

	private static Row run( Dataset ds, String runId ){
		for (Row r : ds.rows) if (r.runId.equals(runId)) return r;
		throw new AssertionError("no run " + runId + " in " + ds.rows.size() + " rows");
	}

	@Test
	public void theAmountsArePublicAndEveryColumnThatNamesASourceIsHidden(){
		DatasetDef d = Datasets.runs();
		assertFalse(d.column(FactKeys.HEALED).isInternal());
		assertFalse(d.column(FactKeys.HEALED_INFERRED).isInternal());
		for (String c : CombatEvents.HEAL_CATEGORIES){
			ColumnDef col = d.column(FactKeys.healedFrom(c));
			assertTrue(c + " must exist", col != null);
			assertTrue("which source healed can be hidden information: " + c, col.isInternal());
		}
	}

	@Test
	public void aFinishedRunShowsExactTotalsAndSourcesAnUnfinishedRunShowsOnlyTheAmount(){
		log("fin", "run-fin", 0, TelemetryVersion.CURRENT).heal(3, true, "regen").heal(5, true, "food.pasty").heal(2, true, "gear.vampiric")
				.end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 10).close(true);
		log("open", "run-open", 0, TelemetryVersion.CURRENT).heal(3, true, "regen").heal(5, true, "food.pasty").heal(2, true, "gear.vampiric").close(false);
		Dataset ds = runs();
		Row fin = run(ds, "run-fin"), open = run(ds, "run-open");
		assertTrue(fin.finished);
		assertFalse(open.finished);

		//finished: everything, exact (no ~)
		assertEquals("10", cell(ds, fin, FactKeys.HEALED).text);
		assertEquals(CellText.EXACT, cell(ds, fin, FactKeys.HEALED).state);
		assertEquals("0", cell(ds, fin, FactKeys.HEALED_INFERRED).text);
		assertEquals("3", cell(ds, fin, FactKeys.healedFrom(CombatEvents.HC_REGEN)).text);
		assertEquals("5", cell(ds, fin, FactKeys.healedFrom(CombatEvents.HC_FOOD)).text);
		assertEquals("2", cell(ds, fin, FactKeys.healedFrom(CombatEvents.HC_GEAR)).text);

		//unfinished: the amount is the player's own, the sources are masked even with hidden information switched on
		assertEquals("10", cell(ds, open, FactKeys.HEALED).text);
		for (String c : CombatEvents.HEAL_CATEGORIES){
			CellText t = cell(ds, open, FactKeys.healedFrom(c));
			assertEquals("the source of a heal in a run being played stays hidden: " + c, CellText.HIDDEN, t.state);
			assertEquals("hidden", t.text);
		}
	}

	@Test
	public void whatEverHealedAnUnfinishedRunCannotBeToldFromAnythingTheViewShows(){
		//two runs in progress, identical to the player in every way but what healed them
		log("a", "run-a", 0, TelemetryVersion.CURRENT).heal(4, true, "regen").heal(6, true, "gear.vampiric").close(false);
		log("b", "run-b", 0, TelemetryVersion.CURRENT).heal(4, true, "potion.healing").heal(6, true, "wand.cursed").close(false);
		Dataset ds = runs();
		Row a = run(ds, "run-a"), b = run(ds, "run-b");
		Query query = q();
		for (ColumnDef col : ds.def.columns()){
			CellText x = CellFormatter.cell(query, a, col), y = CellFormatter.cell(query, b, col);
			if (col.id.equals(FactKeys.LOG_ID) || col.id.equals(FactKeys.RUN_ID)) continue;   //their names differ by design
			assertEquals("column " + col.id + " must not depend on the hidden source", x.text, y.text);
			assertEquals(col.id, x.state, y.state);
		}
		//grouping and aggregating over the hidden columns of unfinished runs reveals nothing either
		QueryResult g = QueryEngine.run(ds, q().withGroup(new GroupBy(java.util.Arrays.asList(FactKeys.HERO_CLASS),
				java.util.Arrays.asList(Aggregate.of(Aggregate.Fn.SUM, FactKeys.healedFrom(CombatEvents.HC_REGEN))))), "x");
		for (GroupRow gr : g.groups) assertEquals("no open value contributes", 0, gr.aggregates.get(0).contributing);
	}

	@Test
	public void runsFromBeforeExactHealingShowUnknownSourcesAndAGuessedTotal(){
		log("old", "run-old", 0, new TelemetryVersion(1, 5)).heal(3, false, "").heal(4, false, "").end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 10).close(true);
		Dataset ds = runs();
		Row r = run(ds, "run-old");
		CellText total = cell(ds, r, FactKeys.HEALED);
		assertEquals(CellText.INFERRED, total.state);
		assertTrue("a guessed total is marked: " + total.text, total.text.contains("~"));
		assertEquals(CellText.UNKNOWN, cell(ds, r, FactKeys.healedFrom(CombatEvents.HC_REGEN)).state);
		assertEquals("?", cell(ds, r, FactKeys.healedFrom(CombatEvents.HC_REGEN)).text);
	}

	@Test
	public void theSegmentsOfOneRunAddUpTheirHealingBySource(){
		log("s0", "run-seg", 0, TelemetryVersion.CURRENT).heal(3, true, "regen").heal(2, true, "plant.sungrass").close(false);
		log("s1", "run-seg", 1, TelemetryVersion.CURRENT).heal(4, true, "regen").end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 10).close(true);
		Dataset ds = runs();
		Row r = run(ds, "run-seg");
		assertEquals("9", cell(ds, r, FactKeys.HEALED).text);
		assertEquals("7", cell(ds, r, FactKeys.healedFrom(CombatEvents.HC_REGEN)).text);
		assertEquals("2", cell(ds, r, FactKeys.healedFrom(CombatEvents.HC_PLANT)).text);
	}
}
