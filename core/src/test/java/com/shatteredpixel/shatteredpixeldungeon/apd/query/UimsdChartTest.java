package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UimsdChartTest {

	private final DatasetDef def = QuerySupport.def();

	private QueryResult grouped( List<Row> rows, List<String> keys, List<Aggregate> aggregates, boolean hidden ){
		Query query = QuerySupport.allScopes(Query.of(def.id))
				.withShowInternal(hidden)
				.withGroup(new GroupBy(keys, aggregates));
		return QueryEngine.run(QuerySupport.dataset(def, rows), query, "rules");
	}

	@Test public void chartUsesGroupedResultOrderAndCapsWithoutInventingAnOtherGroup(){
		List<Row> rows = Arrays.asList(
				QuerySupport.row(def, "r1", "first", 9L, 0L, 10L, true, "rules"),
				QuerySupport.row(def, "r2", "second", 8L, 0L, 11L, true, "rules"),
				QuerySupport.row(def, "g1", "third", 7L, 1L, 12L, true, "rules"),
				QuerySupport.row(def, "b1", "fourth", 6L, 2L, 13L, true, "rules"));
		QueryResult result = grouped(rows, Arrays.asList("kind"),
				Arrays.asList(Aggregate.count(), Aggregate.of(Aggregate.Fn.AVG, "score")), false);

		List<UimsdChart.Measure> measures = UimsdChart.measures(result);
		assertEquals(Arrays.asList("count", "avg:score"),
				Arrays.asList(measures.get(0).id, measures.get(1).id));
		UimsdChart.Series chart = UimsdChart.build(result, "count", 2);
		assertEquals(3, chart.sourceGroups);
		assertEquals(1, chart.omitted);
		assertTrue(chart.isTruncated());
		assertEquals("red", chart.points.get(0).label);
		assertEquals(2d, chart.points.get(0).value, 0d);
		assertEquals("green", chart.points.get(1).label);
		assertEquals(1d, chart.min, 0d);
		assertEquals(2d, chart.max, 0d);
		assertEquals("the cap omits rather than fabricating a combined category", 2, chart.points.size());
	}

	@Test public void aggregateChartRetainsUnknownLowerBoundAndSmallSampleMeaning(){
		Row lower = QuerySupport.row(def, "lower", "lower", 20L, 0L, 1L, true, "rules");
		lower.setNum(QuerySupport.SCORE, 20, Fact.PROV_INCOMPLETE);
		Row unknown = QuerySupport.row(def, "unknown", "unknown", null, 1L, 2L, true, "rules");
		QueryResult result = grouped(Arrays.asList(lower, unknown), Arrays.asList("kind"),
				Arrays.asList(Aggregate.of(Aggregate.Fn.AVG, "score")), false);

		UimsdChart.Series chart = UimsdChart.build(result, "avg:score", 10);
		assertNotNull(chart);
		assertEquals(2, chart.points.size());
		UimsdChart.Point first = chart.points.get(0);
		assertTrue(first.known);
		assertEquals(CellText.LOWER, first.state);
		assertTrue(first.shown, first.shown.startsWith(">="));
		assertTrue(first.small);
		UimsdChart.Point second = chart.points.get(1);
		assertFalse(second.known);
		assertEquals("?", second.shown);
		assertEquals(CellText.UNKNOWN, second.state);
		assertEquals("unknown points do not distort chart bounds", 20d, chart.min, 0d);
		assertEquals(20d, chart.max, 0d);
	}

	@Test public void unavailableInternalAggregateNeverBecomesAChartMeasure(){
		List<Row> rows = Arrays.asList(
				QuerySupport.row(def, "one", "one", 1L, 0L, 999L, true, "rules"),
				QuerySupport.row(def, "two", "two", 2L, 1L, 888L, true, "rules"));
		QueryResult result = grouped(rows, Arrays.asList("kind"),
				Arrays.asList(Aggregate.of(Aggregate.Fn.AVG, "secret")), false);

		List<UimsdChart.Measure> measures = UimsdChart.measures(result);
		assertEquals(1, measures.size());
		assertEquals("count", measures.get(0).id);
		assertNull(UimsdChart.build(result, "avg:secret", 10));
	}

	@Test public void unfinishedInternalGroupUsesHiddenLabelNotItsRawValue(){
		Row unfinished = QuerySupport.row(def, "live", "live", 1L, 0L, 999L, false, "rules");
		Row finished = QuerySupport.row(def, "done", "done", 1L, 0L, 42L, true, "rules");
		QueryResult result = grouped(Arrays.asList(unfinished, finished), Arrays.asList("secret"),
				Arrays.asList(Aggregate.count()), true);

		UimsdChart.Series chart = UimsdChart.build(result, "count", 10);
		assertEquals(2, chart.points.size());
		List<String> labels = new ArrayList<>();
		for (UimsdChart.Point point : chart.points) labels.add(point.label);
		assertTrue(labels.contains("hidden"));
		assertTrue(labels.contains("42"));
		assertFalse(labels.contains("999"));
		for (UimsdChart.Point point : chart.points){
			if (point.label.equals("hidden")) assertEquals(CellText.HIDDEN, point.labelState);
		}
	}

	@Test public void ungroupedAndUnknownMeasuresFailClosed(){
		QueryResult plain = QueryEngine.run(QuerySupport.dataset(def, Arrays.asList(
				QuerySupport.row(def, "one", "one", 1L, 0L, 1L, true, "rules"))),
				QuerySupport.allScopes(Query.of(def.id)), "rules");
		assertTrue(UimsdChart.measures(plain).isEmpty());
		assertNull(UimsdChart.build(plain, "count", 5));
	}
}
