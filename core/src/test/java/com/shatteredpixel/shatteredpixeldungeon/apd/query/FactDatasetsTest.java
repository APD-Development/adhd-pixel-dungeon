package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKind;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKinds;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;

//A kind declared in the registry becomes a dataset with no UIMSD change: one row per observation, the floor and the run's facts joined in,
//the same query engine, and hidden kinds hidden exactly as hidden events are.
public class FactDatasetsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String RS = "r1-aaaaaaaaaaaa";

	private static final FactKind CAST = FactKinds.register(FactKind.builder("test.fd.cast", "Spell cast").area("Magic").subject("Spell")
			.value("mana", "Mana spent").value("targets", "Targets").tag("outcome", "Outcome").build());
	private static final FactKind STEP = FactKinds.register(FactKind.builder("test.fd.step", "Step").area("Movement").value("cell", "Cell").series().build());
	private static final FactKind SECRET = FactKinds.register(FactKind.builder("test.fd.secret", "Hidden thing").area("Hidden").subject("Truth").internal().build());

	private SyntheticLog log( String id, String runId ){
		return new SyntheticLog(tmp.getRoot(), id, TelemetryVersion.CURRENT, 100).start(runId, 0, "CLERIC", RS, 0, 30, false);
	}

	private Catalog catalog(){
		return new DatasetStore(tmp.getRoot()).load();
	}

	private static Query all( String dataset ){
		return Query.of(dataset).withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
	}

	@Test
	public void everyObservationIsARowWithItsFloorAndItsRunsFacts(){
		log("a", "run-a").enter(1).fact(CAST, "GuidingLight", 3, 1).fact(CAST, "GuidingLight", 3, 1).enter(2).fact(CAST, "Smite", 5, 2)
				.end(LifecycleEvents.OUTCOME_WON, "none", 2, 4, 100).close(true);
		Dataset ds = catalog().dataset(FactDatasets.idOf(CAST));
		assertNotNull(ds);
		assertEquals(3, ds.rows.size());
		DatasetDef d = ds.def;
		assertEquals("Spell casts", d.countLabel);
		int depth = d.indexOf(FactDatasets.DEPTH), subj = d.indexOf(FactDatasets.SUBJECT), mana = d.indexOf("fact.v.mana"), cls = d.indexOf("fact.runClass");
		long[] depths = new long[3];
		for (int i = 0; i < 3; i++){
			Row r = ds.rows.get(i);
			depths[i] = r.num(depth);
			assertTrue(r.isKnown(subj));
			assertEquals("CLERIC", r.text(cls));
		}
		assertTrue(Arrays.equals(new long[]{ 1, 1, 2 }, depths));
		assertEquals("Smite", ds.rows.get(2).text(subj));
		assertEquals(5, ds.rows.get(2).num(mana));
	}

	@Test
	public void theUsualQuestionsAreAnsweredByTheNormalEngineWithNoSpecialCode(){
		//"how many casts of each spell, and the average mana": a count, a sum and an average grouped by the subject column
		log("a", "run-a").enter(1).fact(CAST, "GuidingLight", 3, 1).fact(CAST, "GuidingLight", 5, 1).fact(CAST, "Smite", 8, 2)
				.end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 1).close(true);
		Dataset ds = catalog().dataset(FactDatasets.idOf(CAST));
		Query q = all(FactDatasets.idOf(CAST)).withGroup(new GroupBy(Arrays.asList(FactDatasets.SUBJECT), Arrays.asList(
				Aggregate.count(), Aggregate.of(Aggregate.Fn.SUM, "fact.v.mana"), Aggregate.of(Aggregate.Fn.AVG, "fact.v.mana"))));
		QueryResult res = QueryEngine.run(ds, q, RS);
		assertEquals(2, res.groups.size());
		GroupRow guiding = null;
		for (GroupRow g : res.groups) if (g.sample.text(ds.def.indexOf(FactDatasets.SUBJECT)).equals("GuidingLight")) guiding = g;
		assertNotNull(guiding);
		assertEquals(2.0, guiding.aggregates.get(0).value, 0.0);
		assertEquals(8.0, guiding.aggregates.get(1).value, 0.0);
		assertEquals(4.0, guiding.aggregates.get(2).value, 0.0);
	}

	@Test
	public void aSeriesGivesOneRowPerObservationNothingSummarised(){
		long[][] rows = new long[600][1];
		for (int i = 0; i < rows.length; i++) rows[i][0] = 100 + (i % 9);
		log("a", "run-a").enter(1).factSeries(STEP, "", rows).enter(2).factSeries(STEP, "", new long[][]{ { 5 }, { 6 } })
				.end(LifecycleEvents.OUTCOME_DIED, "rat", 2, 1, 1).close(true);
		Dataset ds = catalog().dataset(FactDatasets.idOf(STEP));
		assertEquals(602, ds.rows.size());
		int cell = ds.def.indexOf("fact.v.cell"), depth = ds.def.indexOf(FactDatasets.DEPTH);
		for (int i = 0; i < 600; i++){
			assertEquals(100 + (i % 9), ds.rows.get(i).num(cell));
			assertEquals(1, ds.rows.get(i).num(depth));
		}
		assertEquals(2, ds.rows.get(601).num(depth));
		assertEquals(6, ds.rows.get(601).num(cell));
	}

	@Test
	public void aHiddenKindIsNeverAPublicRowAndOnlyAFinishedRunShowsItWhenHiddenInformationIsOn(){
		log("fin", "run-fin").enter(1).fact(SECRET, "trapAt7").end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 1).close(true);
		log("open", "run-open").enter(1).fact(SECRET, "trapAt9").close(false);
		Dataset ds = catalog().dataset(FactDatasets.idOf(SECRET));
		assertEquals(2, ds.rows.size());
		for (Row r : ds.rows) assertFalse("hidden truth must not be a public row", r.publicRow);
		QueryResult publicView = QueryEngine.run(ds, all(FactDatasets.idOf(SECRET)), RS);
		assertEquals(0, publicView.rows.size());
		QueryResult hiddenOn = QueryEngine.run(ds, all(FactDatasets.idOf(SECRET)).withShowInternal(true), RS);
		assertEquals("only the finished run's hidden facts", 1, hiddenOn.rows.size());
		assertEquals("run-fin", hiddenOn.rows.get(0).runId);
	}

	@Test
	public void aKindThisBuildDoesNotKnowIsSkippedNotAnError(){
		FactKind later = FactKind.builder("test.fd.fromTheFuture", "Future").build();   //never registered
		log("a", "run-a").enter(1).fact(later, "x").fact(CAST, "Smite", 1, 1).end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 1).close(true);
		Catalog c = catalog();
		assertEquals(1, c.dataset(FactDatasets.idOf(CAST)).rows.size());
		assertNull(FactDatasets.def(FactDatasets.PREFIX + "test.fd.fromTheFuture"));
	}

	@Test
	public void olderLogsHaveNoFactRowsAndAreNotBroken(){
		new SyntheticLog(tmp.getRoot(), "old", new TelemetryVersion(1, 7), 100).start("run-old", 0, "MAGE", RS, 0, 30, false).enter(1)
				.end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 1).close(true);
		Catalog c = catalog();
		assertEquals(1, c.dataset(Datasets.RUNS).rows.size());
		Dataset ds = c.dataset(FactDatasets.idOf(CAST));
		assertTrue(ds == null || ds.rows.isEmpty());
	}

	@Test
	public void theRowCacheSurvivesAndRebuildsWhenTheRegistryChanges(){
		log("a", "run-a").enter(1).fact(CAST, "Smite", 1, 1).end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 1).close(true);
		assertEquals(1, catalog().dataset(FactDatasets.idOf(CAST)).rows.size());
		assertEquals("second load (from the cache)", 1, catalog().dataset(FactDatasets.idOf(CAST)).rows.size());
		FactKinds.register(FactKind.builder("test.fd.laterKind", "Later").build());   //a different registry: the cache is rebuilt, not trusted
		assertEquals(1, catalog().dataset(FactDatasets.idOf(CAST)).rows.size());
	}
}
