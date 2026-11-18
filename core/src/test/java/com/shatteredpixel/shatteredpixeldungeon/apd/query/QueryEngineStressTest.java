package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.KIND;
import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.NAME;
import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.SCORE;
import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.dataset;
import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.names;
import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.row;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QueryEngineStressTest {

	private static final String RULESET = "r-now";
	private static final int ROWS = 10_000;

	private Dataset corpus( DatasetDef def ){
		List<Row> rows = new ArrayList<>(ROWS);
		for (int i = 0; i < ROWS; i++){
			Long score = i % 37 == 0 ? null : (long) ((i * 7919) % 1001);
			rows.add(row(def, Integer.toString(i), String.format(Locale.ROOT, "row-%05d", i),
					score, (long) (i % 3), null, true, RULESET));
		}
		return dataset(def, rows);
	}

	@Test
	public void tenThousandRowsFilterSortAndCoverageMatchIndependentModel(){
		DatasetDef def = QuerySupport.def();
		Dataset ds = corpus(def);
		final Map<String, Long> expectedScores = new HashMap<>();
		for (Row r : ds.rows){
			if (!r.isKnown(SCORE)) continue;
			long score = r.num(SCORE), kind = r.num(KIND);
			if (score >= 200 && score <= 800 && (kind == 0 || kind == 2)){
				expectedScores.put(r.text(NAME), score);
			}
		}
		List<String> expected = new ArrayList<>(expectedScores.keySet());
		Collections.sort(expected, new Comparator<String>(){
			@Override
			public int compare( String a, String b ){
				int score = Long.compare(expectedScores.get(b), expectedScores.get(a));
				return score != 0 ? score : a.compareToIgnoreCase(b);
			}
		});

		Query q = Query.of("things")
				.addFilter(Filter.num("score", Op.GE, 200))
				.addFilter(Filter.num("score", Op.LE, 800))
				.addFilter(Filter.num("kind", Op.IN, 0, 2))
				.withSorts(Arrays.asList(new Sort("score", true), new Sort("name", false)));
		QueryResult result = QueryEngine.run(ds, q, RULESET);

		assertEquals(expected, names(result));
		assertEquals(expected.size(), result.stats.matching);
		assertEquals(expected.size(), result.coverageOf("score").known);
		assertEquals(3, result.stats.filters.size());
		assertFalse(result.problems.toString(), !result.problems.isEmpty());
		assertEquals("row-00000", ds.rows.get(0).text(NAME));
		assertEquals("row-09999", ds.rows.get(ROWS - 1).text(NAME));
		assertNull(ds.rows.get(0).isKnown(SCORE) ? Long.valueOf(ds.rows.get(0).num(SCORE)) : null);
	}

	@Test
	public void tenThousandRowsGroupedAggregatesMatchIndependentModel(){
		DatasetDef def = QuerySupport.def();
		Dataset ds = corpus(def);
		long[] counts = new long[3], sums = new long[3], mins = {Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE},
				maxes = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
		for (Row r : ds.rows){
			if (!r.isKnown(SCORE)) continue;
			long score = r.num(SCORE);
			if (score < 200 || score > 800) continue;
			int kind = (int) r.num(KIND);
			counts[kind]++;
			sums[kind] += score;
			mins[kind] = Math.min(mins[kind], score);
			maxes[kind] = Math.max(maxes[kind], score);
		}
		List<Aggregate> aggregates = Arrays.asList(Aggregate.count(), Aggregate.of(Aggregate.Fn.SUM, "score"),
				Aggregate.of(Aggregate.Fn.AVG, "score"), Aggregate.of(Aggregate.Fn.MIN, "score"),
				Aggregate.of(Aggregate.Fn.MAX, "score"));
		Query q = Query.of("things")
				.addFilter(Filter.num("score", Op.BETWEEN, 200, 800))
				.withGroup(new GroupBy(Arrays.asList("kind"), aggregates))
				.withSorts(Arrays.asList(new Sort("kind", false)));
		QueryResult result = QueryEngine.run(ds, q, RULESET);

		assertEquals(3, result.groups.size());
		for (int kind = 0; kind < 3; kind++){
			GroupRow group = result.groups.get(kind);
			assertEquals(kind, group.sample.num(KIND));
			assertEquals(counts[kind], group.n);
			assertEquals(counts[kind], group.aggregates.get(0).value, 0.0);
			assertEquals(sums[kind], group.aggregates.get(1).value, 0.0);
			assertEquals((double) sums[kind] / counts[kind], group.aggregates.get(2).value, 0.0000001);
			assertEquals(mins[kind], group.aggregates.get(3).value, 0.0);
			assertEquals(maxes[kind], group.aggregates.get(4).value, 0.0);
		}
		assertEquals(ROWS, ds.rows.size());
		assertEquals("row-00000", ds.rows.get(0).text(NAME));
	}
}
