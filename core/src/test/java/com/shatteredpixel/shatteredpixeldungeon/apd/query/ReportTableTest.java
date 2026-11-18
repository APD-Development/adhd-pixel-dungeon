package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.report.Report;
import com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportBuilder;
import com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportRenderer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

//A very large table is cut at ReportBuilder.MAX_ROWS and the report says so; hidden information of unfinished runs stays hidden in a report.
public class ReportTableTest {

	@Test
	public void aHugeTableIsCutAndSaysHowMuchIsMissing() {
		DatasetDef d = QuerySupport.def();
		List<Row> rows = new ArrayList<>();
		for (int i = 0; i < 1500; i++) rows.add(QuerySupport.row(d, "r" + i, "name-" + i, (long) i, 0L, 1L, true, "r"));
		QueryResult q = QueryEngine.run(QuerySupport.dataset(d, rows), QuerySupport.allScopes(Query.of("things")), "r");
		Report r = ReportBuilder.view(q, "Things", "", 0L, "t");
		assertEquals(ReportBuilder.MAX_ROWS, r.firstTable().rows.size());
		assertEquals(500, r.firstTable().omitted);
		assertTrue(ReportRenderer.markdown(r).contains("500 more entries are not listed"));
	}

	@Test
	public void aHugeGroupedTableIsCutByGroupsAndSaysHowMuchIsMissing() {
		DatasetDef d = QuerySupport.def();
		List<Row> rows = new ArrayList<>();
		for (int i = 0; i < 1500; i++) rows.add(QuerySupport.row(d, "r" + i, "name-" + i, (long) i, 0L, 1L, true, "r"));
		Query grouped = QuerySupport.allScopes(Query.of("things")).withGroup(new GroupBy(
				java.util.Collections.singletonList("name"),
				java.util.Collections.singletonList(Aggregate.count())));
		QueryResult q = QueryEngine.run(QuerySupport.dataset(d, rows), grouped, "r");
		assertEquals(1500, q.groups.size());

		Report r = ReportBuilder.view(q, "Grouped things", "", 0L, "t");
		assertEquals(ReportBuilder.MAX_ROWS, r.firstTable().rows.size());
		assertEquals(500, r.firstTable().omitted);
		assertTrue(ReportRenderer.markdown(r).contains("500 more entries are not listed"));
	}

	@Test
	public void unfinishedRunsKeepTheirSecretsOutOfAReportEvenWhenAskedFor() {
		DatasetDef d = QuerySupport.def();
		List<Row> rows = new ArrayList<>();
		rows.add(QuerySupport.row(d, "a", "open", 5L, 0L, 99L, false, "r"));
		rows.add(QuerySupport.row(d, "b", "done", 6L, 0L, 77L, true, "r"));
		Query q = QuerySupport.allScopes(Query.of("things")).withShowInternal(true).withColumns(java.util.Arrays.asList("name", "secret"));
		QueryResult res = QueryEngine.run(QuerySupport.dataset(d, rows), q, "r");
		String md = ReportRenderer.markdown(ReportBuilder.view(res, "Things", "", 0L, "t"));
		assertTrue(md, md.contains("77"));
		assertTrue("the unfinished run's secret must not appear", !md.contains("99"));
		assertTrue(md.contains("hidden"));
	}
}
