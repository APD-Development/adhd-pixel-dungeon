package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.SCORE;
import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.allScopes;
import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.dataset;
import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.row;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class AggValueContractTest {

	private static final String RULESET = "r";
	private final DatasetDef def = QuerySupport.def();

	private AggValue aggregate( Dataset dataset, Aggregate aggregate ){
		Query query = allScopes(Query.of("things")).withGroup(new GroupBy(Collections.emptyList(), Arrays.asList(aggregate)));
		QueryResult result = QueryEngine.run(dataset, query, RULESET);
		assertEquals(1, result.groups.size());
		return result.groups.get(0).aggregates.get(0);
	}

	@Test
	public void importedAggregateProvenanceIsVisibleAndCombinesWithoutLosingStrongerWarnings(){
		Row recorded = row(def, "recorded", "a", 10L, 0L, null, true, RULESET);
		Row lower = row(def, "lower", "b", 5L, 0L, null, true, RULESET);
		lower.setNum(SCORE, 5, Fact.PROV_INCOMPLETE);
		Row inferred = row(def, "inferred", "c", 7L, 0L, null, true, RULESET);
		inferred.setNum(SCORE, 7, Fact.PROV_INFERRED);
		Row imported = row(def, "imported", "d", 3L, 0L, null, true, RULESET);
		imported.setNum(SCORE, 3, Fact.PROV_LEGACY);
		Aggregate sum = Aggregate.of(Aggregate.Fn.SUM, "score");
		AggValue combined = aggregate(dataset(def, Arrays.asList(recorded, lower, inferred, imported)), sum);

		assertEquals(25.0, combined.value, 0.0);
		assertEquals(4, combined.contributing);
		assertEquals(4, combined.total);
		assertTrue(combined.isLowerBound());
		assertTrue(combined.isInferred());
		assertTrue(combined.isLegacy());
		CellFormatter.AggText text = CellFormatter.aggregate(def, sum, combined);
		assertEquals(">=~25", text.text);
		assertEquals(CellText.LOWER, text.state);
		assertTrue(text.detail.contains("includes imported ranking data"));

		AggValue legacyOnly = aggregate(dataset(def, Arrays.asList(imported)), sum);
		CellFormatter.AggText legacyText = CellFormatter.aggregate(def, sum, legacyOnly);
		assertEquals("3", legacyText.text);
		assertEquals(CellText.LEGACY, legacyText.state);
	}

	@Test
	public void ratesAndRatiosRetainTheirExactCountMetadata(){
		Dataset values = dataset(def, Arrays.asList(
				row(def, "a", "a", 3L, 2L, null, true, RULESET),
				row(def, "b", "b", 1L, 2L, null, true, RULESET),
				row(def, "c", "c", 2L, 4L, null, true, RULESET)));

		AggValue rate = aggregate(values, Aggregate.rate("kind", 2));
		assertEquals(2.0 / 3.0, rate.value, 0.0);
		assertEquals(2, rate.hits);
		assertEquals(3, rate.contributing);
		assertEquals(3, rate.total);

		AggValue ratio = aggregate(values, Aggregate.ratio("score", "kind"));
		assertEquals(0.75, ratio.value, 0.0);
		assertEquals(6, ratio.hits);
		assertEquals(8, ratio.outOf);
		assertEquals(3, ratio.contributing);
		assertEquals(3, ratio.total);
	}

	@Test
	public void zeroDenominatorRatioIsUnknownWithoutPhantomContributions(){
		Aggregate ratio = Aggregate.ratio("score", "kind");
		AggValue value = aggregate(dataset(def, Arrays.asList(row(def, "zero", "z", 3L, 0L, null, true, RULESET))), ratio);

		assertTrue(!value.known);
		assertEquals(0, value.hits);
		assertEquals(0, value.outOf);
		assertEquals(0, value.contributing);
		assertEquals(1, value.total);
		assertEquals("0 of 1 known", CellFormatter.aggregate(def, ratio, value).detail);
	}
}
