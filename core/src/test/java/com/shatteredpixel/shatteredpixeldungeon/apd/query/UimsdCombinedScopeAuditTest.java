package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class UimsdCombinedScopeAuditTest {
	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void emptyHomeExplainsExcludedHistoryWithoutDeletingIt() {
		DatasetDef def = new DatasetDef(Datasets.RUNS, "Runs", "History");
		List<Row> rows = new ArrayList<>();
		for (String rules : new String[]{null, Row.RULESET_MIXED, "current"}) {
			Row row = new Row("row-"+rows.size(), null, "run-"+rows.size(), 0);
			row.finished = true;
			row.ruleset = rules;
			row.duplicate = "current".equals(rules);
			rows.add(row);
		}
		Dataset ds = QuerySupport.dataset(def, rows);
		UimsdHome home = UimsdHome.build(new Catalog().add(ds), "current", true);
		assertTrue(home.isEmpty());
		assertTrue(home.scopeNote, home.scopeNote.contains("unknown or changed rules"));
		assertTrue(home.scopeNote, home.scopeNote.contains("repeated imported copies"));
		assertTrue(home.scopeNote, home.scopeNote.contains("table Options"));
		assertEquals(3, QueryEngine.run(ds, QuerySupport.allScopes(Query.of(Datasets.RUNS)), "current").rows.size());
		assertEquals(3, rows.size());
	}

	@Test
	public void scopedFilteredSortedReorderedViewsKeepCorrectRowsAcrossDiskReload() throws Exception {
		DatasetDef def = QuerySupport.def();
		List<Row> rows = new ArrayList<>();
		String[] rules = {"current", "other", null, Row.RULESET_MIXED};
		for (int i = 0; i < 128; i++) {
			Row row = QuerySupport.row(def, "row-"+i, "name-"+(i%3),
					i%13 == 0 ? null : (long)(i%11), 0L, null, (i&4)==0, rules[i%4]);
			row.duplicate = (i&8)!=0;
			row.publicRow = (i&16)==0;
			rows.add(row);
		}
		Dataset ds = QuerySupport.dataset(def, rows);
		File file = SavedViews.fileIn(tmp.getRoot());
		int checked = 0;
		for (int flags = 0; flags < 64; flags++) {
			Scope scope = new Scope((flags&1)==0 ? Scope.Rules.CURRENT : Scope.Rules.ALL,
					"", (flags&2)!=0, (flags&4)!=0, (flags&8)!=0, (flags&16)!=0);
			final boolean descending = (flags&32)!=0;
			List<Row> expected = new ArrayList<>();
			for (Row row : rows) {
				if (!row.publicRow || (row.duplicate && !scope.includeDuplicates)
						|| (!row.finished && !scope.includeUnfinished)) continue;
				if (row.ruleset == null) { if (!scope.includeUnknownRuleset) continue; }
				else if (row.ruleset.equals(Row.RULESET_MIXED)) { if (!scope.includeMixedRuleset) continue; }
				else if (scope.rules == Scope.Rules.CURRENT && !row.ruleset.equals("current")) continue;
				if (row.isKnown(QuerySupport.SCORE) && row.num(QuerySupport.SCORE) >= 5) expected.add(row);
			}
			expected.sort((a,b)-> {
				int score = Long.compare(a.num(QuerySupport.SCORE), b.num(QuerySupport.SCORE));
				if (score != 0) return descending ? -score : score;
				return a.text(QuerySupport.NAME).compareTo(b.text(QuerySupport.NAME));
			});
			for (List<String> columns : Arrays.asList(Arrays.asList("name","score"),
					Arrays.asList("score","name"), Arrays.asList("kind","name","score"))) {
				Query query = Query.of("things").withScope(scope).addFilter(Filter.num("score", Op.GE, 5))
						.withSorts(Arrays.asList(new Sort("score", descending), new Sort("name", false)))
						.withColumns(columns);
				SavedViews saved = new SavedViews();
				saved.save("audit", query, 1);
				saved.store(file);
				Query restored = SavedViews.load(file).get("audit").query();
				assertNotNull(restored);
				assertEquals(QueryCodec.encode(query), QueryCodec.encode(restored));
				QueryResult result = QueryEngine.run(ds, restored, "current");
				assertEquals("selection/order flags="+flags+" columns="+columns, expected, result.rows);
				assertEquals(columns.size(), result.columns.size());
				for (int i=0; i<columns.size(); i++) assertEquals(columns.get(i), result.columns.get(i).id);
				checked++;
			}
		}
		assertEquals(192, checked);
	}
}
