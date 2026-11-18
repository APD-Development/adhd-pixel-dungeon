package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//The generic aggregates behind the aggregate-first views: distinct counts, medians, ratios, "only rows where", and "one per
//distinct value". They keep the rules of every other aggregate: unknown is not zero, lower bounds are flagged, hidden columns of
//unfinished runs are never looked at.
public class AggregateExtensionsTest {

	private static final String R = "r";
	private final DatasetDef def = QuerySupport.def();

	private QueryResult run( Dataset ds, Aggregate... aggs ){
		Query q = allScopes(Query.of("things")).withGroup(new GroupBy(Arrays.asList("depth"), Arrays.asList(aggs)));
		return QueryEngine.run(ds, q, R);
	}

	//everything in one group: depth is the same on every row
	private Row r( String runId, String name, Long score, Long kind, Long secret ){
		Row row = rowIn(def, runId, runId + "/" + name + score, name, score, kind, secret, true, R);
		row.setNum(DEPTH, 1, Fact.PROV_RECORDED);
		return row;
	}

	private AggValue only( QueryResult res, int index ){
		assertEquals(1, res.groups.size());
		return res.groups.get(0).aggregates.get(index);
	}

	@Test
	public void distinctCountCountsDifferentValuesOfTextOrNumbers(){
		Dataset ds = dataset(def, Arrays.asList(r("a", "x", 1L, 0L, null), r("a", "x", 2L, 0L, null), r("b", "y", 3L, 1L, null), r("c", "y", 4L, 1L, null)));
		QueryResult res = run(ds, Aggregate.distinct("name"), Aggregate.distinct("kind"));
		assertEquals(2, (int) only(res, 0).value);
		assertEquals(4, only(res, 0).contributing);
		assertEquals(2, (int) only(res, 1).value);
	}

	@Test
	public void distinctCountIgnoresUnknownValuesAndSaysSo(){
		Row unknownName = r("c", null, 5L, 0L, null);
		Dataset ds = dataset(def, Arrays.asList(r("a", "x", 1L, 0L, null), unknownName));
		AggValue v = only(run(ds, Aggregate.distinct("name")), 0);
		assertEquals(1, (int) v.value);
		assertEquals(1, v.contributing);
		assertEquals(2, v.total);
	}

	@Test
	public void medianOfOddAndEvenCounts(){
		Dataset odd = dataset(def, Arrays.asList(r("a", "x", 1L, 0L, null), r("b", "x", 10L, 0L, null), r("c", "x", 2L, 0L, null)));
		assertEquals(2.0, only(run(odd, Aggregate.of(Aggregate.Fn.MEDIAN, "score")), 0).value, 0.0001);
		Dataset even = dataset(def, Arrays.asList(r("a", "x", 1L, 0L, null), r("b", "x", 3L, 0L, null)));
		assertEquals(2.0, only(run(even, Aggregate.of(Aggregate.Fn.MEDIAN, "score")), 0).value, 0.0001);
	}

	@Test
	public void medianUsesOnlyKnownValuesAndIsUnknownNotZeroWhenThereAreNone(){
		Dataset some = dataset(def, Arrays.asList(r("a", "x", 7L, 0L, null), r("b", "x", null, 0L, null), r("c", "x", 9L, 0L, null)));
		AggValue v = only(run(some, Aggregate.of(Aggregate.Fn.MEDIAN, "score")), 0);
		assertEquals(8.0, v.value, 0.0001);
		assertEquals(2, v.contributing);
		assertEquals(3, v.total);
		Dataset none = dataset(def, Arrays.asList(r("a", "x", null, 0L, null)));
		AggValue u = only(run(none, Aggregate.of(Aggregate.Fn.MEDIAN, "score")), 0);
		assertFalse(u.known);
		assertEquals("?", CellFormatter.aggregate(def, Aggregate.of(Aggregate.Fn.MEDIAN, "score"), u).text);
	}

	@Test
	public void aMedianOfLowerBoundsIsMarkedAsAtLeast(){
		Row lower = r("b", "x", 4L, 0L, null);
		lower.setNum(SCORE, 4, Fact.PROV_INCOMPLETE);
		Dataset ds = dataset(def, Arrays.asList(r("a", "x", 2L, 0L, null), lower, r("c", "x", 6L, 0L, null)));
		AggValue v = only(run(ds, Aggregate.of(Aggregate.Fn.MEDIAN, "score")), 0);
		assertTrue(v.isLowerBound());
		assertTrue(CellFormatter.aggregate(def, Aggregate.of(Aggregate.Fn.MEDIAN, "score"), v).text.startsWith(">="));
	}

	@Test
	public void ratioIsOneSumOverAnotherAndUnknownWhenThereIsNothingToDivideBy(){
		Dataset ds = dataset(def, Arrays.asList(r("a", "x", 3L, 2L, null), r("b", "x", 1L, 2L, null), r("c", "x", 2L, 4L, null)));
		//score / kind: 6 over 8
		AggValue v = only(run(ds, Aggregate.ratio("score", "kind")), 0);
		assertEquals(0.75, v.value, 0.0001);
		Dataset zero = dataset(def, Arrays.asList(r("a", "x", 3L, 0L, null)));
		assertFalse(only(run(zero, Aggregate.ratio("score", "kind")), 0).known);
	}

	@Test
	public void ratioSkipsLowerBoundValuesBecauseTheyCannotBeDivided(){
		Row lb = r("b", "x", 100L, 1L, null);
		lb.setNum(SCORE, 100, Fact.PROV_INCOMPLETE);
		Dataset ds = dataset(def, Arrays.asList(r("a", "x", 3L, 1L, null), lb));
		AggValue v = only(run(ds, Aggregate.ratio("score", "kind")), 0);
		assertEquals(3.0, v.value, 0.0001);
		assertEquals(1, v.contributing);
	}

	@Test
	public void whereLooksOnlyAtRowsWithTheGivenValue(){
		Dataset ds = dataset(def, Arrays.asList(r("a", "x", 10L, 0L, null), r("b", "x", 20L, 1L, null), r("c", "x", 40L, 1L, null)));
		AggValue v = only(run(ds, Aggregate.of(Aggregate.Fn.AVG, "score").where("kind", 1)), 0);
		assertEquals(30.0, v.value, 0.0001);
		assertEquals(2, v.total);
		assertFalse(v.isUnsure());
	}

	@Test
	public void rowsThatCannotBeJudgedAgainstTheConditionAreLeftOutAndTheResultSaysSo(){
		Row unknownKind = r("a", "x", 1000L, null, null);
		Row lowerKind = r("b", "x", 2000L, 1L, null);
		lowerKind.setNum(KIND, 1, Fact.PROV_INCOMPLETE);
		Dataset ds = dataset(def, Arrays.asList(unknownKind, lowerKind, r("c", "x", 10L, 1L, null)));
		AggValue v = only(run(ds, Aggregate.of(Aggregate.Fn.SUM, "score").where("kind", 1)), 0);
		assertEquals(10.0, v.value, 0.0001);
		assertTrue(v.isUnsure());
		assertTrue(CellFormatter.aggregate(def, Aggregate.of(Aggregate.Fn.SUM, "score").where("kind", 1), v).detail.contains("could not be judged"));
	}

	@Test
	public void perDistinctValueCountsARunOnceHoweverManyRowsItHas(){
		//a run with three rows counts once; "rate" is then a rate over runs, not rows
		Dataset ds = dataset(def, Arrays.asList(r("a", "won", 1L, 1L, null), r("a", "won", 2L, 1L, null), r("a", "won", 3L, 1L, null),
				r("b", "lost", 4L, 0L, null)));
		Aggregate perRow = Aggregate.rate("kind", 1);
		Aggregate perRun = Aggregate.rate("kind", 1).perDistinct("name");
		QueryResult res = run(ds, perRow, perRun, Aggregate.count().perDistinct("name"));
		assertEquals(0.75, only(res, 0).value, 0.0001);
		assertEquals(0.5, only(res, 1).value, 0.0001);
		assertEquals(2, (int) only(res, 2).value);
	}

	@Test
	public void whereAndPerDistinctCombine(){
		Dataset ds = dataset(def, Arrays.asList(r("a", "run1", 1L, 1L, null), r("a", "run1", 1L, 1L, null), r("b", "run2", 1L, 0L, null),
				r("c", "run3", 1L, 1L, null)));
		//distinct runs among the rows where kind = 1
		AggValue v = only(run(ds, Aggregate.distinct("name").where("kind", 1)), 0);
		assertEquals(2, (int) v.value);
		assertEquals(3, v.total);
	}

	@Test
	public void rowsWithNoValueToShareAreNotMergedByTheDistinctRule(){
		Dataset ds = dataset(def, Arrays.asList(r("a", null, 1L, 0L, null), r("b", null, 2L, 0L, null), r("c", "x", 3L, 0L, null), r("d", "x", 4L, 0L, null)));
		Aggregate aggregate = Aggregate.count().perDistinct("name");
		AggValue value = only(run(ds, aggregate), 0);
		assertEquals(3, (int) value.value);
		assertTrue("unknown distinct keys must not make the result look exact", value.isUnsure());
		assertTrue(CellFormatter.aggregate(def, aggregate, value).detail.contains("could not be judged"));
	}

	@Test
	public void lowerBoundDistinctKeysAreNotTreatedAsExactIdentities(){
		Row lower = r("a", "lower", 1L, 1L, null);
		lower.setNum(KIND, 1, Fact.PROV_INCOMPLETE);
		Dataset ds = dataset(def, Arrays.asList(lower, r("b", "exact", 2L, 1L, null)));
		Aggregate aggregate = Aggregate.count().perDistinct("kind");
		AggValue value = only(run(ds, aggregate), 0);
		assertEquals("the inexact key still stands for its own row", 2, (int) value.value);
		assertTrue("a lower-bound identity cannot produce an exact distinct result", value.isUnsure());
	}

	@Test
	public void hiddenColumnsInTheNewAggregatesFollowTheHiddenInformationRules(){
		List<Row> rows = new ArrayList<>();
		Row fin = rowIn(def, "fin", "f", "x", 1L, 1L, 50L, true, R);
		fin.setNum(DEPTH, 1, Fact.PROV_RECORDED);
		Row open = rowIn(def, "open", "o", "x", 1L, 1L, 9999L, false, R);
		open.setNum(DEPTH, 1, Fact.PROV_RECORDED);
		rows.add(fin);
		rows.add(open);
		Dataset ds = dataset(def, rows);
		//hidden information off: aggregates over hidden columns are not computed at all
		for (Aggregate a : new Aggregate[]{ Aggregate.of(Aggregate.Fn.MEDIAN, "secret"), Aggregate.distinct("secret"),
				Aggregate.of(Aggregate.Fn.SUM, "score").where("secret", 50), Aggregate.count().perDistinct("secret"), Aggregate.ratio("score", "secret") }){
			Query q = allScopes(Query.of("things")).withGroup(new GroupBy(Arrays.asList("depth"), Arrays.asList(a)));
			QueryResult res = QueryEngine.run(ds, q, R);
			assertTrue(a.id(), res.groups.isEmpty() || res.aggregates.isEmpty());
		}
		//hidden information on: the unfinished run's hidden values never take part
		Query on = allScopes(Query.of("things")).withShowInternal(true).withGroup(new GroupBy(Arrays.asList("depth"),
				Arrays.asList(Aggregate.of(Aggregate.Fn.MEDIAN, "secret"), Aggregate.distinct("secret"), Aggregate.of(Aggregate.Fn.MAX, "secret"))));
		QueryResult res = QueryEngine.run(ds, on, R);
		assertEquals(50.0, only(res, 0).value, 0.0001);
		assertEquals(1, (int) only(res, 1).value);
		assertEquals(50.0, only(res, 2).value, 0.0001);
		assertEquals(1, only(res, 0).contributing);
	}

	@Test
	public void smallSamplesAreMarkedAndLargeOnesAreNot(){
		List<Row> rows = new ArrayList<>();
		for (int i = 0; i < 6; i++) rows.add(r("run" + i, "x", (long) i, 0L, null));
		Dataset big = dataset(def, rows);
		assertFalse(CellFormatter.aggregate(def, Aggregate.of(Aggregate.Fn.AVG, "score"), only(run(big, Aggregate.of(Aggregate.Fn.AVG, "score")), 0)).small);
		Dataset small = dataset(def, rows.subList(0, 3));
		assertTrue(CellFormatter.aggregate(def, Aggregate.of(Aggregate.Fn.AVG, "score"), only(run(small, Aggregate.of(Aggregate.Fn.AVG, "score")), 0)).small);
		//a plain count is never "small": it is a count, not an estimate
		assertFalse(CellFormatter.aggregate(def, Aggregate.count(), only(run(small, Aggregate.count()), 0)).small);
	}

	@Test
	public void theNewAggregatesSurviveTheQueryTextFormAndOldLinesStillDecode(){
		Aggregate a = Aggregate.rate("kind", 1).where("score", 3).perDistinct("name").labeled("Win %");
		Aggregate b = Aggregate.ratio("score", "kind");
		Aggregate c = Aggregate.of(Aggregate.Fn.MEDIAN, "score");
		Query q = Query.of("things").withGroup(new GroupBy(Arrays.asList("depth"), Arrays.asList(Aggregate.count(), a, b, c, Aggregate.distinct("name"))));
		Query back = QueryCodec.decode(QueryCodec.encode(q));
		assertEquals(q.group.aggregates, back.group.aggregates);
		Aggregate decoded = back.group.aggregates.get(1);
		assertEquals("Win %", decoded.label);
		assertEquals("score", decoded.whereColumn);
		assertEquals(3, decoded.whereValue);
		assertEquals("name", decoded.distinctBy);
		assertEquals("kind", back.group.aggregates.get(2).over);
		//a query written before these existed
		Query old = QueryCodec.decode("dataset\tthings\ngroup\tdepth\nagg\tRATE\tkind\t1\n");
		assertEquals(Aggregate.rate("kind", 1), old.group.aggregates.get(0));
	}

	@Test
	public void aggregatesWithDifferentConditionsAreDifferentColumns(){
		Aggregate plain = Aggregate.rate("kind", 1);
		assertFalse(plain.id().equals(plain.where("score", 1).id()));
		assertFalse(plain.id().equals(plain.perDistinct("name").id()));
		assertTrue(plain.equals(plain.labeled("anything")));
	}
}
