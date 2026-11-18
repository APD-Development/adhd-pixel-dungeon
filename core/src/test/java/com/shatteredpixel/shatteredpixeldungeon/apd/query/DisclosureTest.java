package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//The rule that matters most in UIMSD: hidden (INTERNAL) information of a run that is not permanently finished can never be
//seen, not even indirectly through counts, sorting, filtering or grouping, whatever the player switches on.
public class DisclosureTest {

	private static final String R = "r-now";
	private final DatasetDef def = QuerySupport.def();

	//finished and unfinished runs; some rows exist only as hidden information (not public rows)
	private Dataset data( long unfinishedSecret, boolean withUnfinishedHiddenRows ){
		List<Row> rows = new ArrayList<>();
		rows.add(rowIn(def, "fin", "f1", "fin-public-1", 10L, 0L, 100L, true, R));
		rows.add(rowIn(def, "fin", "f2", "fin-public-2", 20L, 1L, 200L, true, R));
		Row fh = rowIn(def, "fin", "f3", "fin-hidden", 30L, 2L, 300L, true, R);
		fh.publicRow = false;                       //e.g. an item the player never saw, in a finished run
		rows.add(fh);
		rows.add(rowIn(def, "open", "u1", "open-public-1", 11L, 0L, unfinishedSecret, false, R));
		rows.add(rowIn(def, "open", "u2", "open-public-2", 21L, 1L, unfinishedSecret + 1, false, R));
		if (withUnfinishedHiddenRows){
			Row uh = rowIn(def, "open", "u3", "open-hidden", 31L, 2L, unfinishedSecret + 2, false, R);
			uh.publicRow = false;                   //an item on an undiscovered floor of the run being played
			rows.add(uh);
			Row uh2 = rowIn(def, "open", "u4", "open-hidden-2", 41L, 2L, unfinishedSecret + 3, false, R);
			uh2.publicRow = false;
			rows.add(uh2);
		}
		return dataset(def, rows);
	}

	@Test
	public void withHiddenInformationOffInternalColumnsAreNotOfferedOrUsable(){
		Query q = Query.of("things").withColumns(Arrays.asList("name", "secret", "note"))
				.addFilter(Filter.num("secret", Op.GE, 1)).withSorts(Arrays.asList(new Sort("secret", false)));
		QueryResult r = QueryEngine.run(data(1000, true), q, R);
		assertEquals(1, r.columns.size());          //only "name": secret and note are hidden columns
		assertTrue(r.problems.toString(), r.problems.size() >= 1);
		assertEquals(2, r.rows.size());             //the finished public rows, no filter was applied
		//a group by or aggregate on a hidden column is left out too
		QueryResult g = QueryEngine.run(data(1000, true), Query.of("things").withGroup(new GroupBy(Arrays.asList("secret"),
				Arrays.asList(Aggregate.of(Aggregate.Fn.SUM, "secret")))), R);
		assertFalse(g.isGrouped());
	}

	@Test
	public void rowsThatExistOnlyAsHiddenInformationAreCountedOnlyForFinishedRuns(){
		QueryResult r = QueryEngine.run(data(1000, true), allScopes(Query.of("things")), R);
		assertEquals("only the finished hidden row is counted, the unfinished run's two are not even mentioned", 1, r.stats.hiddenRows);
		//the unfinished run's hidden rows leave no trace in any count
		QueryResult without = QueryEngine.run(data(1000, false), allScopes(Query.of("things")), R);
		assertEquals(without.stats.total, r.stats.total);
		assertEquals(without.stats.hiddenRows, r.stats.hiddenRows);
		assertEquals(without.rows.size(), r.rows.size());
	}

	@Test
	public void turningHiddenInformationOnShowsFinishedRunsButNeverTheUnfinishedOne(){
		Query q = allScopes(Query.of("things")).withShowInternal(true).withColumns(Arrays.asList("name", "secret"));
		QueryResult r = QueryEngine.run(data(1000, true), q, R);
		List<String> names = names(r);
		assertTrue(names.contains("fin-hidden"));                 //finished run: hidden row revealed
		assertFalse(names.contains("open-hidden"));               //unfinished run: hidden rows stay hidden
		assertFalse(names.contains("open-hidden-2"));
		assertEquals(5, r.rows.size());
		for (Row row : r.rows){
			CellText secret = CellFormatter.cell(q, row, def.column("secret"));
			if (row.finished){
				assertEquals(CellText.EXACT, secret.state);
			} else {
				assertEquals("an unfinished run's hidden value is masked", CellText.HIDDEN, secret.state);
				assertEquals("hidden", secret.text);
			}
		}
	}

	@Test
	public void maskedValuesCannotBeFilteredSortedGroupedOrAggregated(){
		Query base = allScopes(Query.of("things")).withShowInternal(true);
		//filter: the unfinished rows are reported as hidden, never matched or "not matched"
		QueryResult f = QueryEngine.run(data(1000, true), base.addFilter(Filter.num("secret", Op.GE, 1)), R);
		QueryResult.FilterReport rep = f.stats.filters.get(0);
		assertEquals(2, rep.hidden);
		assertEquals(0, rep.unknown);
		assertEquals(0, rep.notMatched);
		for (Row row : f.rows) assertTrue(row.finished);
		//"is unknown" does not reveal which hidden values are unknown either
		QueryResult u = QueryEngine.run(data(1000, true), base.addFilter(Filter.unknown("secret")), R);
		assertEquals(0, u.rows.size());
		assertEquals(2, u.stats.filters.get(0).hidden);
		//aggregates only use open cells
		QueryResult g = QueryEngine.run(data(1000, true), base.withGroup(new GroupBy(Arrays.asList("kind"),
				Arrays.asList(Aggregate.of(Aggregate.Fn.SUM, "secret"), Aggregate.of(Aggregate.Fn.COUNT_KNOWN, "secret")))), R);
		for (GroupRow gr : g.groups){
			int finishedInGroup = 0;
			for (Row row : gr.rows) if (row.finished) finishedInGroup++;
			AggValue known = gr.aggregates.get(1);
			assertEquals("only finished runs' values are counted", finishedInGroup, known.contributing);
		}
		//coverage lists the hidden cells apart from the unknown ones
		QueryResult c = QueryEngine.run(data(1000, true), base, R);
		QueryResult.ColumnCoverage cov = c.coverageOf("secret");
		assertNull("secret is not a default column", cov);
		QueryResult c2 = QueryEngine.run(data(1000, true), base.withColumns(Arrays.asList("name", "secret")), R);
		assertEquals(2, c2.coverageOf("secret").hidden);
	}

	//the strongest check: change everything about an unfinished run's hidden information and nothing the player can observe changes
	private static String observe( QueryResult r ){
		StringBuilder b = new StringBuilder();
		b.append(r.columns.size()).append('|').append(r.problems).append('|');
		QueryResult.Stats s = r.stats;
		b.append(s.total).append(',').append(s.inScope).append(',').append(s.matching).append(',').append(s.otherRulesets)
				.append(',').append(s.unknownRuleset).append(',').append(s.mixedRuleset).append(',').append(s.unfinished).append(',')
				.append(s.duplicates).append(',').append(s.hiddenRows).append(',').append(s.hiddenSmallGroups).append(',')
				.append(s.hiddenSmallGroupRows).append(',').append(s.runsInScope).append(',').append(s.runsCovered).append('|');
		for (QueryResult.FilterReport f : s.filters){
			b.append(f.in).append(':').append(f.matched).append(':').append(f.notMatched).append(':').append(f.unknown).append(':')
					.append(f.uncertain).append(':').append(f.hidden).append(';');
		}
		b.append('|');
		for (QueryResult.ColumnCoverage c : r.coverage){
			b.append(c.column.id).append('=').append(c.total).append('/').append(c.known).append('/').append(c.lowerBound)
					.append('/').append(c.inferred).append('/').append(c.hidden).append(';');
		}
		b.append('|');
		for (Row row : r.rows){
			b.append(row.id).append('[');
			for (ColumnDef c : r.columns) b.append(CellFormatter.cell(r.query, row, c).text).append(',');
			b.append(']');
		}
		if (r.isGrouped()){
			for (GroupRow g : r.groups){
				b.append('{').append(g.n).append(':');
				for (ColumnDef k : r.groupKeys) b.append(CellFormatter.cell(r.query, g.sample, k).text).append(',');
				for (int i = 0; i < g.aggregates.size(); i++){
					b.append(CellFormatter.aggregate(r.def, r.aggregates.get(i), g.aggregates.get(i)).text).append('/')
							.append(g.aggregates.get(i).contributing).append(',');
				}
				b.append('}');
			}
		}
		return b.toString();
	}

	@Test
	public void nothingObservableDependsOnAnUnfinishedRunsHiddenInformation(){
		//A and B differ in the unfinished run's hidden values, and in whether it has hidden-only rows at all
		Dataset a = data(1000, false);
		Dataset b = data(7, true);
		Dataset c = data(-5, true);
		List<Query> queries = new ArrayList<>();
		Scope[] scopes = { Scope.DEFAULT, Scope.DEFAULT.withUnfinished(true), new Scope(Scope.Rules.ALL, "", true, true, true, true) };
		for (Scope sc : scopes){
			for (boolean show : new boolean[]{false, true}){
				Query base = Query.of("things").withScope(sc).withShowInternal(show);
				List<String> cols = Arrays.asList("name", "score", "kind", "secret", "note");
				queries.add(base.withColumns(cols));
				queries.add(base.withColumns(cols).addFilter(Filter.num("secret", Op.GE, 0)));
				queries.add(base.withColumns(cols).addFilter(Filter.num("secret", Op.LT, 50)));
				queries.add(base.withColumns(cols).addFilter(Filter.unknown("secret")));
				queries.add(base.withColumns(cols).addFilter(Filter.known("note")));
				queries.add(base.withColumns(cols).withSorts(Arrays.asList(new Sort("secret", false))));
				queries.add(base.withColumns(cols).withSorts(Arrays.asList(new Sort("secret", true), new Sort("name", false))));
				queries.add(base.withGroup(new GroupBy(Arrays.asList("secret"), Arrays.asList(Aggregate.count()))));
				queries.add(base.withGroup(new GroupBy(Arrays.asList("kind"), Arrays.asList(Aggregate.count(),
						Aggregate.of(Aggregate.Fn.SUM, "secret"), Aggregate.of(Aggregate.Fn.AVG, "secret"),
						Aggregate.of(Aggregate.Fn.MAX, "secret"), Aggregate.of(Aggregate.Fn.COUNT_KNOWN, "secret"),
						Aggregate.of(Aggregate.Fn.MIN, "score")))).withMinSample(2));
			}
		}
		for (Query q : queries){
			String oa = observe(QueryEngine.run(a, q, R));
			assertEquals("hidden information leaked through: " + QueryCodec.encode(q), oa, observe(QueryEngine.run(b, q, R)));
			assertEquals("hidden information leaked through: " + QueryCodec.encode(q), oa, observe(QueryEngine.run(c, q, R)));
		}
	}

	@Test
	public void onceTheRunIsFinishedItsHiddenInformationCanBeRead(){
		//the same unfinished data, but now the runs are permanently finished: now the values may be shown
		List<Row> rows = new ArrayList<>();
		Row r1 = rowIn(def, "open", "u1", "was-open", 11L, 0L, 4242L, true, R);
		rows.add(r1);
		Row hidden = rowIn(def, "open", "u2", "was-open-hidden", 31L, 2L, 777L, true, R);
		hidden.publicRow = false;
		rows.add(hidden);
		Query q = Query.of("things").withShowInternal(true).withColumns(Arrays.asList("name", "secret"));
		QueryResult r = QueryEngine.run(dataset(def, rows), q, R);
		assertEquals(2, r.rows.size());
		assertEquals("4242", CellFormatter.cell(q, r.rows.get(0), def.column("secret")).text);
		//and with the switch off they still are not shown
		QueryResult off = QueryEngine.run(dataset(def, rows), q.withShowInternal(false), R);
		assertEquals(1, off.rows.size());
		assertEquals(1, off.stats.hiddenRows);
	}
}
