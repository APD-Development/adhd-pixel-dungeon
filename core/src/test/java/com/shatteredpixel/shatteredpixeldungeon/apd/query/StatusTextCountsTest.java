package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import org.junit.Test;
import static org.junit.Assert.*;

public class StatusTextCountsTest {
	private String mixedNotice(String units, int rows) {
		DatasetDef def = QuerySupport.def();
		def.countLabel = units;
		java.util.List<Row> data = new java.util.ArrayList<>();
		for (int i = 0; i < rows; i++) {
			data.add(QuerySupport.rowIn(def, "one-run", "item-"+i, "item", 1L, 0L,
					null, true, Row.RULESET_MIXED));
		}
		QueryResult result = QueryEngine.run(QuerySupport.dataset(def, data), Query.of("things"), "current");
		assertEquals(rows, result.stats.mixedRuleset);
		for (StatusText.Line line : StatusText.lines(result, "Items", "")) {
			if (line.action == StatusText.ACT_MIXED_RULESET) return line.text;
		}
		throw new AssertionError("Missing mixed-rules exclusion explanation");
	}

	@Test
	public void multipleItemsFromOneRunAreNotPresentedAsMultipleRuns() {
		String text = mixedNotice("Items", 5);
		assertTrue(text, text.startsWith("5 items"));
		assertFalse(text, text.contains("5 runs"));
	}

	@Test
	public void singleItemUsesSingularWording() {
		String text = mixedNotice("Items", 1);
		assertTrue(text, text.startsWith("1 item belongs"));
	}
}
