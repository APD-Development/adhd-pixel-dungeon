package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QueryEngineTest {

	private final DatasetDef def = QuerySupport.def();
	private static final String R = "r-now";

	private Dataset simple(){
		List<Row> rows = new ArrayList<>();
		rows.add(row(def, "1", "alpha", 10L, 0L, 1L, true, R));
		rows.add(row(def, "2", "beta", 30L, 1L, 2L, true, R));
		rows.add(row(def, "3", "gamma", null, 2L, 3L, true, R));          //score unknown
		rows.add(row(def, "4", "delta", 20L, 1L, null, true, R));
		rows.add(row(def, "5", null, 5L, null, 5L, true, R));             //name and kind unknown
		return dataset(def, rows);
	}

	private QueryResult run( Dataset ds, Query q ){
		return QueryEngine.run(ds, q, R);
	}

	@Test
	public void everyOperatorFiltersAndAnUnknownValueNeverMatches(){
		Dataset ds = simple();
		Query base = Query.of("things");
		assertEquals(Arrays.asList("alpha"), names(run(ds, base.addFilter(Filter.num("score", Op.EQ, 10)))));
		assertEquals(3, run(ds, base.addFilter(Filter.num("score", Op.NE, 10))).rows.size());   //beta, delta, and the unnamed one; gamma is unknown
		assertEquals(2, run(ds, base.addFilter(Filter.num("score", Op.LT, 20))).rows.size());
		assertEquals(3, run(ds, base.addFilter(Filter.num("score", Op.LE, 20))).rows.size());
		assertEquals(1, run(ds, base.addFilter(Filter.num("score", Op.GT, 20))).rows.size());
		assertEquals(2, run(ds, base.addFilter(Filter.num("score", Op.GE, 20))).rows.size());
		assertEquals(2, run(ds, base.addFilter(Filter.num("score", Op.BETWEEN, 10, 20))).rows.size());
		assertEquals(2, run(ds, base.addFilter(Filter.num("score", Op.BETWEEN, 20, 10))).rows.size());   //either order
		assertEquals(3, run(ds, base.addFilter(Filter.num("kind", Op.IN, 1, 2))).rows.size());   //beta, gamma, delta
		assertEquals(1, run(ds, base.addFilter(Filter.text("name", Op.EQ, "ALPHA"))).rows.size());   //case does not matter
		assertEquals(4, run(ds, base.addFilter(Filter.text("name", Op.CONTAINS, "a"))).rows.size());
		assertEquals(1, run(ds, base.addFilter(Filter.text("name", Op.IN, "beta", "nobody"))).rows.size());
		assertEquals(4, run(ds, base.addFilter(Filter.known("score"))).rows.size());
		QueryResult unk = run(ds, base.addFilter(Filter.unknown("score")));
		assertEquals(Arrays.asList("gamma"), names(unk));
	}

	@Test
	public void aFilterReportsWhatItCostBecauseOfUnknownValues(){
		QueryResult r = run(simple(), Query.of("things").addFilter(Filter.num("score", Op.GE, 20)));
		QueryResult.FilterReport f = r.stats.filters.get(0);
		assertEquals(5, f.in);
		assertEquals(2, f.matched);
		assertEquals(1, f.unknown);      //gamma has no score: not zero, not a mismatch, just not known
		assertEquals(2, f.notMatched);
		assertEquals(0, f.uncertain);
		assertEquals(0, f.hidden);
	}

	@Test
	public void repeatedFiltersCombineWithAnd(){
		Query q = Query.of("things").addFilter(Filter.num("score", Op.GE, 10)).addFilter(Filter.num("kind", Op.EQ, 1))
				.addFilter(Filter.text("name", Op.CONTAINS, "e"));
		QueryResult r = run(simple(), q);
		assertEquals(Arrays.asList("beta", "delta"), names(r));
		assertEquals(3, r.stats.filters.size());
		assertEquals(3, r.stats.filters.get(0).matched);
		assertEquals(2, r.stats.filters.get(1).matched);
		//the same filter twice is allowed and changes nothing
		assertEquals(2, run(simple(), q.addFilter(Filter.num("kind", Op.EQ, 1))).rows.size());
		//editing and removing
		assertEquals(1, run(simple(), q.replaceFilter(0, Filter.num("score", Op.GE, 30))).rows.size());
		assertEquals(2, run(simple(), q.removeFilter(0).removeFilter(0).removeFilter(0).addFilter(Filter.num("kind", Op.EQ, 1))).rows.size());
		assertEquals(5, run(simple(), q.removeFilter(0).removeFilter(0).removeFilter(0)).rows.size());
	}

	@Test
	public void unknownValuesSortLastWhicheverWayAndAreNeverRankedAsZero(){
		Dataset ds = simple();
		List<String> asc = names(run(ds, Query.of("things").withSorts(Arrays.asList(new Sort("score", false)))));
		//scores: 5 (unnamed), 10 alpha, 20 delta, 30 beta, unknown gamma
		assertEquals("gamma", asc.get(4));
		assertEquals("alpha", asc.get(1));
		List<String> desc = names(run(ds, Query.of("things").withSorts(Arrays.asList(new Sort("score", true)))));
		assertEquals("gamma", desc.get(4));   //still last: unknown is not the smallest
		assertEquals("beta", desc.get(0));
	}

	@Test
	public void sortingWithSeveralKeysIsStable(){
		List<Row> rows = new ArrayList<>();
		rows.add(row(def, "1", "a", 5L, 1L, null, true, R));
		rows.add(row(def, "2", "b", 5L, 0L, null, true, R));
		rows.add(row(def, "3", "c", 5L, 0L, null, true, R));
		rows.add(row(def, "4", "d", 9L, 0L, null, true, R));
		Query q = Query.of("things").withSorts(Arrays.asList(new Sort("score", false), new Sort("kind", false)));
		assertEquals(Arrays.asList("b", "c", "a", "d"), names(run(dataset(def, rows), q)));
		assertEquals(Arrays.asList("d", "b", "c", "a"), names(run(dataset(def, rows),
				Query.of("things").withSorts(Arrays.asList(new Sort("score", true), new Sort("kind", false))))));
	}

	@Test
	public void clickingAHeaderCyclesAscendingDescendingOff(){
		Query q = Query.of("things");
		q = q.cycleSort("score");
		assertEquals(Arrays.asList(new Sort("score", false)), q.sorts);
		q = q.cycleSort("score");
		assertEquals(Arrays.asList(new Sort("score", true)), q.sorts);
		q = q.cycleSort("kind").cycleSort("name").cycleSort("depth");
		assertEquals(3, q.sorts.size());   //at most three keys
		assertEquals("depth", q.sorts.get(0).column);
		q = q.cycleSort("depth").cycleSort("depth");   //descending, then off
		assertFalse(q.sorts.contains(new Sort("depth", false)) || q.sorts.contains(new Sort("depth", true)));
	}

	@Test
	public void aLowerBoundIsNeverTreatedAsExact(){
		List<Row> rows = new ArrayList<>();
		Row lb = row(def, "1", "lb", null, null, null, true, R);
		lb.setNum(SCORE, 12, Fact.PROV_INCOMPLETE);          // at least 12
		rows.add(lb);
		rows.add(row(def, "2", "exact", 12L, null, null, true, R));
		Dataset ds = dataset(def, rows);
		Query b = Query.of("things");
		//>= 10 is true for every value from 12 up: decided
		assertEquals(2, run(ds, b.addFilter(Filter.num("score", Op.GE, 10))).rows.size());
		//>= 20: the lower bound might reach it, nobody knows
		QueryResult ge = run(ds, b.addFilter(Filter.num("score", Op.GE, 20)));
		assertEquals(0, ge.rows.size());
		assertEquals(1, ge.stats.filters.get(0).uncertain);
		//< 5 is false for everything at least 12
		QueryResult lt = run(ds, b.addFilter(Filter.num("score", Op.LT, 5)));
		assertEquals(0, lt.stats.filters.get(0).uncertain);
		//= 12: the exact one matches, the lower bound may be 12 or more
		QueryResult eq = run(ds, b.addFilter(Filter.num("score", Op.EQ, 12)));
		assertEquals(Arrays.asList("exact"), names(eq));
		assertEquals(1, eq.stats.filters.get(0).uncertain);
		//= 7 is impossible for the lower bound
		assertEquals(0, run(ds, b.addFilter(Filter.num("score", Op.EQ, 7))).stats.filters.get(0).uncertain);
		//< 20 cannot be decided for the bound
		assertEquals(1, run(ds, b.addFilter(Filter.num("score", Op.LT, 20))).stats.filters.get(0).uncertain);

	}

	@Test
	public void scopeKeepsRulesetsUnfinishedRunsAndDuplicatesApartAndCountsWhatItLeavesOut(){
		List<Row> rows = new ArrayList<>();
		rows.add(row(def, "1", "now", 1L, 0L, null, true, R));
		rows.add(row(def, "2", "other", 1L, 0L, null, true, "r-old"));
		rows.add(row(def, "3", "unknown", 1L, 0L, null, true, null));
		rows.add(row(def, "4", "mixed", 1L, 0L, null, true, Row.RULESET_MIXED));
		rows.add(row(def, "5", "open", 1L, 0L, null, false, R));
		Row dup = row(def, "6", "dup", 1L, 0L, null, true, R);
		dup.duplicate = true;
		rows.add(dup);
		Dataset ds = dataset(def, rows);

		QueryResult r = run(ds, Query.of("things"));
		assertEquals(Arrays.asList("now"), names(r));
		assertEquals(6, r.stats.total);
		assertEquals(1, r.stats.otherRulesets);
		assertEquals(1, r.stats.unknownRuleset);     //never silently treated as the current ruleset
		assertEquals(1, r.stats.mixedRuleset);
		assertEquals(1, r.stats.unfinished);
		assertEquals(1, r.stats.duplicates);
		assertEquals(1, r.stats.runsInScope);

		Scope s = Scope.DEFAULT;
		assertEquals(2, run(ds, Query.of("things").withScope(s.withUnknownRuleset(true))).rows.size());
		assertEquals(2, run(ds, Query.of("things").withScope(s.withUnfinished(true))).rows.size());
		assertEquals(2, run(ds, Query.of("things").withScope(s.withDuplicates(true))).rows.size());
		assertEquals(2, run(ds, Query.of("things").withScope(s.withMixedRuleset(true))).rows.size());
		assertEquals(2, run(ds, Query.of("things").withScope(s.withRules(Scope.Rules.ALL, ""))).rows.size());   //now + other
		assertEquals(Arrays.asList("other"), names(run(ds, Query.of("things").withScope(s.withRules(Scope.Rules.SPECIFIC, "r-old")))));
		assertEquals(6, run(ds, allScopes(Query.of("things"))).rows.size());
	}

	@Test
	public void theCurrentRulesetBeingUnknownMatchesNothingRatherThanEverything(){
		QueryResult r = QueryEngine.run(simple(), Query.of("things"), null);
		assertEquals(0, r.rows.size());
		assertEquals(5, r.stats.otherRulesets);
	}

	@Test
	public void runsCoveredCountsTheRunsADatasetCanKnowAbout(){
		List<Row> rows = new ArrayList<>();
		rows.add(row(def, "1", "a", 1L, 0L, null, true, R));
		List<Dataset.RunInfo> runs = new ArrayList<>();
		runs.add(new Dataset.RunInfo("r1", true, R, true, false));
		runs.add(new Dataset.RunInfo("r2", true, R, false, false));    //recorded before the dataset existed
		runs.add(new Dataset.RunInfo("r3", true, "r-old", true, false));
		QueryResult r = run(new Dataset(def, rows, runs), Query.of("things"));
		assertEquals(2, r.stats.runsInScope);
		assertEquals(1, r.stats.runsCovered);
	}

	@Test
	public void coverageSaysHowManyCellsAreKnownLowerBoundsAndInferred(){
		List<Row> rows = new ArrayList<>();
		rows.add(row(def, "1", "a", 1L, 0L, null, true, R));
		Row lb = row(def, "2", "b", null, 0L, null, true, R);
		lb.setNum(SCORE, 3, Fact.PROV_INCOMPLETE);
		rows.add(lb);
		Row inf = row(def, "3", "c", null, 0L, null, true, R);
		inf.setNum(SCORE, 4, Fact.PROV_INFERRED);
		rows.add(inf);
		rows.add(row(def, "4", "d", null, 0L, null, true, R));
		QueryResult r = run(dataset(def, rows), Query.of("things"));
		QueryResult.ColumnCoverage c = r.coverageOf("score");
		assertEquals(4, c.total);
		assertEquals(3, c.known);
		assertEquals(1, c.lowerBound);
		assertEquals(1, c.inferred);
		assertNull(r.coverageOf("secret"));   //a hidden column is not even listed
	}

	@Test
	public void groupingCountsAggregatesAndSaysHowManyValuesWentIntoEach(){
		List<Row> rows = new ArrayList<>();
		rows.add(row(def, "1", "a", 10L, 0L, null, true, R));
		rows.add(row(def, "2", "b", 20L, 0L, null, true, R));
		rows.add(row(def, "3", "c", null, 0L, null, true, R));      //unknown score
		rows.add(row(def, "4", "d", 40L, 1L, null, true, R));
		rows.add(row(def, "5", "e", 50L, null, null, true, R));      //unknown kind: its own group
		GroupBy g = new GroupBy(Arrays.asList("kind"), Arrays.asList(Aggregate.count(), Aggregate.of(Aggregate.Fn.AVG, "score"),
				Aggregate.of(Aggregate.Fn.SUM, "score"), Aggregate.of(Aggregate.Fn.COUNT_KNOWN, "score"),
				Aggregate.of(Aggregate.Fn.MAX, "score"), Aggregate.of(Aggregate.Fn.MIN, "score"), Aggregate.rate("kind", 0)));
		QueryResult r = run(dataset(def, rows), Query.of("things").withGroup(g));
		assertTrue(r.isGrouped());
		assertEquals(3, r.groups.size());
		GroupRow red = r.groups.get(0);
		assertEquals(3, red.n);
		AggValue avg = red.aggregates.get(1);
		assertEquals(15.0, avg.value, 0.0001);   //(10+20)/2, the unknown one is not a zero
		assertEquals(2, avg.contributing);
		assertEquals(3, avg.total);
		assertEquals(30.0, red.aggregates.get(2).value, 0.0001);
		assertEquals(2.0, red.aggregates.get(3).value, 0.0001);
		assertEquals(20.0, red.aggregates.get(4).value, 0.0001);
		assertEquals(10.0, red.aggregates.get(5).value, 0.0001);
		AggValue rate = red.aggregates.get(6);
		assertEquals(3, rate.hits);
		assertEquals(3, rate.contributing);
		//the group with an unknown kind is its own group, not folded into any real one
		boolean sawUnknown = false;
		for (GroupRow gr : r.groups) if (!gr.sample.isKnown(KIND)) sawUnknown = true;
		assertTrue(sawUnknown);
	}

	@Test
	public void anAggregateOverNothingKnownIsUnknownNotZero(){
		List<Row> rows = new ArrayList<>();
		rows.add(row(def, "1", "a", null, 0L, null, true, R));
		GroupBy g = new GroupBy(Arrays.asList("kind"), Arrays.asList(Aggregate.of(Aggregate.Fn.AVG, "score")));
		QueryResult r = run(dataset(def, rows), Query.of("things").withGroup(g));
		AggValue v = r.groups.get(0).aggregates.get(0);
		assertFalse(v.known);
		assertEquals(0, v.contributing);
		assertEquals(1, v.total);
		assertEquals("?", CellFormatter.aggregate(def, Aggregate.of(Aggregate.Fn.AVG, "score"), v).text);
	}

	@Test
	public void lowerBoundAndInferredValuesMakeAggregatesLowerBoundAndInferred(){
		List<Row> rows = new ArrayList<>();
		rows.add(row(def, "1", "a", 10L, 0L, null, true, R));
		Row lb = row(def, "2", "b", null, 0L, null, true, R);
		lb.setNum(SCORE, 5, Fact.PROV_INCOMPLETE);
		rows.add(lb);
		Row inf = row(def, "3", "c", null, 1L, null, true, R);
		inf.setNum(SCORE, 7, Fact.PROV_INFERRED);
		rows.add(inf);
		GroupBy g = new GroupBy(Arrays.asList("kind"), Arrays.asList(Aggregate.of(Aggregate.Fn.SUM, "score"), Aggregate.rate("score", 5)));
		QueryResult r = run(dataset(def, rows), Query.of("things").withGroup(g));
		AggValue sum = r.groups.get(0).aggregates.get(0);   //kind 0: 10 exact + >=5
		assertTrue(sum.isLowerBound());
		assertEquals(15.0, sum.value, 0.0001);
		assertEquals(">=15", CellFormatter.aggregate(def, Aggregate.of(Aggregate.Fn.SUM, "score"), sum).text);
		AggValue rate = r.groups.get(0).aggregates.get(1);
		assertEquals("a lower bound cannot be judged against a target, so it is left out", 1, rate.contributing);
		AggValue infSum = r.groups.get(1).aggregates.get(0);
		assertTrue(infSum.isInferred());
		assertEquals("~7", CellFormatter.aggregate(def, Aggregate.of(Aggregate.Fn.SUM, "score"), infSum).text);
	}

	@Test
	public void smallGroupsAreListedAsHiddenNotDropped(){
		List<Row> rows = new ArrayList<>();
		for (int i = 0; i < 5; i++) rows.add(row(def, "a" + i, "x", 1L, 0L, null, true, R));
		rows.add(row(def, "b", "y", 1L, 1L, null, true, R));
		rows.add(row(def, "c", "z", 1L, 2L, null, true, R));
		GroupBy g = new GroupBy(Arrays.asList("kind"), Arrays.asList(Aggregate.count()));
		Query q = Query.of("things").withGroup(g).withMinSample(3);
		QueryResult r = run(dataset(def, rows), q);
		assertEquals(1, r.groups.size());
		assertEquals(2, r.stats.hiddenSmallGroups);
		assertEquals(2, r.stats.hiddenSmallGroupRows);
		assertEquals(3, run(dataset(def, rows), q.withMinSample(0)).groups.size());
	}

	@Test
	public void groupsSortByKeyOrByAnAggregate(){
		List<Row> rows = new ArrayList<>();
		rows.add(row(def, "1", "a", 1L, 0L, null, true, R));
		rows.add(row(def, "2", "b", 1L, 1L, null, true, R));
		rows.add(row(def, "3", "c", 1L, 1L, null, true, R));
		GroupBy g = new GroupBy(Arrays.asList("kind"), Arrays.asList(Aggregate.count()));
		Query q = Query.of("things").withGroup(g).withSorts(Arrays.asList(new Sort("count", true)));
		QueryResult r = run(dataset(def, rows), q);
		assertEquals(2, r.groups.get(0).n);
		r = run(dataset(def, rows), Query.of("things").withGroup(g).withSorts(Arrays.asList(new Sort("kind", true))));
		assertEquals(1, r.groups.get(0).sample.num(KIND));
	}

	@Test
	public void partsOfAQueryThatCannotBeAppliedAreReportedNotIgnoredSilently(){
		Query q = Query.of("things").addFilter(Filter.num("nope", Op.EQ, 1)).addFilter(Filter.text("score", Op.CONTAINS, "x"))
				.addFilter(Filter.num("score", Op.BETWEEN, 1)).addFilter(Filter.num("secret", Op.EQ, 1));
		QueryResult r = run(simple(), q);
		assertEquals(4, r.problems.size());
		assertEquals(5, r.rows.size());   //nothing was applied
	}

	@Test
	public void theQueryRoundTripsThroughText(){
		Query q = Query.of("things").withScope(Scope.DEFAULT.withRules(Scope.Rules.SPECIFIC, "r-old").withUnfinished(true).withDuplicates(true))
				.addFilter(Filter.num("score", Op.BETWEEN, 3, 9)).addFilter(Filter.text("name", Op.IN, "a|b", "c\td", "e\\f"))
				.addFilter(Filter.unknown("kind")).withSorts(Arrays.asList(new Sort("score", true), new Sort("name", false)))
				.withColumns(Arrays.asList("name", "score")).withShowInternal(true).withMinSample(4)
				.withGroup(new GroupBy(Arrays.asList("kind", "name"), Arrays.asList(Aggregate.count(), Aggregate.rate("kind", 2),
						Aggregate.of(Aggregate.Fn.AVG, "score"))));
		Query back = QueryCodec.decode(QueryCodec.encode(q));
		assertEquals(q, back);
		assertEquals(q, QueryCodec.decode(QueryCodec.encode(back)));
		//a newer writer's extra line does not break an older reader
		assertEquals(q, QueryCodec.decode(QueryCodec.encode(q) + "future-thing\tvalue\n"));
		assertEquals(Query.of("things"), QueryCodec.decode(QueryCodec.encode(Query.of("things"))));
	}

    @Test
    public void sortingLongExtremesKeepsUnknownLastAndEqualValuesStable(){
        List<Row> rows=Arrays.asList(
            row(def,"1","minimum",Long.MIN_VALUE,0L,null,true,R),
            row(def,"2","maximum",Long.MAX_VALUE,0L,null,true,R),
            row(def,"3","equal-first",Long.MAX_VALUE,0L,null,true,R),
            row(def,"4","unknown",null,0L,null,true,R),
            row(def,"5","zero",0L,0L,null,true,R));
        Dataset ds=dataset(def,rows);
        assertEquals(Arrays.asList("minimum","zero","maximum","equal-first","unknown"),
            names(run(ds,Query.of("things").withSorts(Arrays.asList(new Sort("score",false))))));
        assertEquals(Arrays.asList("maximum","equal-first","zero","minimum","unknown"),
            names(run(ds,Query.of("things").withSorts(Arrays.asList(new Sort("score",true))))));
        assertEquals("minimum",ds.rows.get(0).text(NAME));
    }

    @Test
    public void extremeBoundaryFilteringAndEmptyResultsDoNotMutateHistory(){
        List<Row> rows=Arrays.asList(
            row(def,"1","minimum",Long.MIN_VALUE,0L,null,true,R),
            row(def,"2","maximum",Long.MAX_VALUE,0L,null,true,R),
            row(def,"3","unknown",null,0L,null,true,R));
        Dataset ds=dataset(def,rows);
        Query q=Query.of("things").withColumns(Arrays.asList("score","name"))
            .withSorts(Arrays.asList(new Sort("score",true)));
        assertEquals(Arrays.asList("maximum"),names(run(ds,q.addFilter(Filter.num("score",Op.GE,Long.MAX_VALUE)))));
        assertEquals(Arrays.asList("minimum"),names(run(ds,q.addFilter(Filter.num("score",Op.LE,Long.MIN_VALUE)))));
        QueryResult empty=run(ds,q.addFilter(Filter.num("score",Op.GT,Long.MAX_VALUE)));
        assertTrue(empty.rows.isEmpty());
        assertEquals(1,empty.stats.filters.get(0).unknown);
        assertEquals(3,run(ds,Query.of("things")).rows.size());
        assertEquals(Long.MIN_VALUE,ds.rows.get(0).num(SCORE));
        assertEquals(Long.MAX_VALUE,ds.rows.get(1).num(SCORE));
    }
	@Test
	public void queryCodecPreservesAllEscapedFieldsAndEmptyTextCardinality(){
		String odd = "tab\tnewline\npipe|slash\\tail";
		Aggregate detailed = new Aggregate(Aggregate.Fn.RATE, odd, Long.MIN_VALUE, "", odd, Long.MAX_VALUE, odd, odd);
		Query q = new Query(odd, Scope.DEFAULT.withRules(Scope.Rules.SPECIFIC, odd),
				Arrays.asList(Filter.text(odd, Op.IN, "", odd), Filter.text("single", Op.EQ, ""), Filter.known("none")),
				Arrays.asList(new Sort(odd, true)), Arrays.asList(odd),
				new GroupBy(Arrays.asList(odd), Arrays.asList(detailed, Aggregate.ratio(odd, "den" + odd))),
				true, Integer.MAX_VALUE);

		Query back = QueryCodec.decode(QueryCodec.encode(q));

		assertEquals(q, back);
		assertEquals(2, back.filters.get(0).texts.length);
		assertEquals("", back.filters.get(0).texts[0]);
		assertEquals(odd, back.filters.get(0).texts[1]);
		assertEquals(1, back.filters.get(1).texts.length);
		assertEquals("", back.filters.get(1).texts[0]);
		assertEquals(0, back.filters.get(2).texts.length);
		assertEquals(odd, back.group.aggregates.get(0).label);
		assertEquals(Long.MIN_VALUE, back.group.aggregates.get(0).target);
		assertEquals(Long.MAX_VALUE, back.group.aggregates.get(0).whereValue);
		assertEquals("den" + odd, back.group.aggregates.get(1).over);
		assertEquals(QueryCodec.encode(q), QueryCodec.encode(back));
	}

}
