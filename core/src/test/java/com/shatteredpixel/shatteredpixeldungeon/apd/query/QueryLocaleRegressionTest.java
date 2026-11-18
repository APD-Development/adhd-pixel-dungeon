package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.*;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class QueryLocaleRegressionTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private Dataset fixture() {
        DatasetDef def = def();
        return dataset(def, Arrays.asList(
                row(def, "upper", "RING OF MIGHT", 20L, 0L, null, true, "rules"),
                row(def, "lower", "ring of accuracy", 20L, 0L, null, true, "rules"),
                row(def, "other", "Sword", 100L, 0L, null, true, "rules"),
                row(def, "unknown", null, 999L, 0L, null, true, "rules")));
    }

    @Test public void containsMatchesTheSameRecordedNamesUnderDifferentDeviceLocales() {
        Locale previous = Locale.getDefault();
        try {
            Dataset data = fixture();
            for (Locale locale : new Locale[]{Locale.ROOT, Locale.ENGLISH, Locale.forLanguageTag("tr-TR"), Locale.forLanguageTag("az-AZ")}) {
                Locale.setDefault(locale);
                for (String search : new String[]{"ring", "RING", "RiNg"}) {
                    QueryResult result = QueryEngine.run(data, Query.of("things")
                            .addFilter(Filter.text("name", Op.CONTAINS, search)), "rules");
                    assertTrue(result.problems.toString(), result.problems.isEmpty());
                    assertEquals("device locale " + locale + ", search " + search,
                            Arrays.asList("RING OF MIGHT", "ring of accuracy"), names(result));
                    assertEquals("unknown names stay unknown", 1, result.stats.filters.get(0).unknown);
                }
            }
        } finally { Locale.setDefault(previous); }
    }

    @Test public void savedFilterSortAndColumnOrderAreStableAfterChangingTheDeviceLocale() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            Dataset data = fixture();
            Query query = Query.of("things").addFilter(Filter.text("name", Op.CONTAINS, "ring"))
                    .withSorts(Arrays.asList(new Sort("score", true)))
                    .withColumns(Arrays.asList("score", "name"));
            File file = SavedViews.fileIn(tmp.getRoot());
            SavedViews views = new SavedViews();
            views.save("Ring equipment", query, 10);
            views.store(file);
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Query reopened = SavedViews.load(file).get("Ring equipment").query();
            assertEquals(query, reopened);
            QueryResult result = QueryEngine.run(data, reopened, "rules");
            assertEquals("equal scores keep stable ordering", Arrays.asList("RING OF MIGHT", "ring of accuracy"), names(result));
            assertEquals("score", result.columns.get(0).id);
            assertEquals("name", result.columns.get(1).id);
            assertEquals("recorded names never change", "RING OF MIGHT", data.rows.get(0).text(NAME));
        } finally { Locale.setDefault(previous); }
    }

    @Test public void savedTextFiltersPreserveLiteralSeparatorsUnicodeAndCase() throws Exception {
        String[] literals = {"back\\slash", "literal\\n", "tab\there", "line\nbreak", "pipe|value",
                "élixir", "药水", "😀", "__literal__", "**literal**", " leading", "trailing "};
        File file = SavedViews.fileIn(tmp.getRoot());
        SavedViews views = new SavedViews();
        Query query = Query.of("things").addFilter(Filter.text("name", Op.IN, literals));
        views.save("Literal values", query, 20);
        views.store(file);
        assertEquals(query, SavedViews.load(file).get("Literal values").query());
    }

    @Test public void aggregateIdentityAndHashKeysNeverDependOnTheDeviceLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            java.util.Map<Aggregate, String> lookup = new java.util.HashMap<>();
            java.util.List<String> ids = new java.util.ArrayList<>();
            java.util.List<Aggregate> aggregates = new java.util.ArrayList<>();
            for (Aggregate.Fn function : Aggregate.Fn.values()) {
                Aggregate aggregate = Aggregate.of(function, "score");
                aggregates.add(aggregate); ids.add(aggregate.id()); lookup.put(aggregate, aggregate.id());
            }
            for (Locale locale : new Locale[]{Locale.forLanguageTag("tr-TR"), Locale.forLanguageTag("az-AZ"), Locale.ROOT}) {
                Locale.setDefault(locale);
                for (int i = 0; i < aggregates.size(); i++) {
                    assertEquals("stable function identifier under " + locale, ids.get(i), aggregates.get(i).id());
                    assertEquals("existing hash lookup survives language change", ids.get(i), lookup.get(aggregates.get(i)));
                }
            }
        } finally { Locale.setDefault(previous); }
    }

    @Test public void groupedMinimumSortingSurvivesSavedViewReloadUnderTurkishLocale() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            DatasetDef def = def();
            Dataset data = dataset(def, Arrays.asList(
                    row(def, "high", "a", 100L, 0L, null, true, "rules"),
                    row(def, "low", "b", 2L, 1L, null, true, "rules")));
            Query query = Query.of("things").withGroup(new GroupBy(Arrays.asList("kind"),
                    Arrays.asList(Aggregate.of(Aggregate.Fn.MIN, "score"))))
                    .withSorts(Arrays.asList(new Sort("min:score", false)));
            SavedViews views = new SavedViews(); views.save("Grouped minimum", query, 30);
            File file = SavedViews.fileIn(tmp.getRoot()); views.store(file);
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Query reopened = SavedViews.load(file).get("Grouped minimum").query();
            QueryResult result = QueryEngine.run(data, reopened, "rules");
            assertTrue(result.problems.toString(), result.problems.isEmpty());
            assertEquals("minimum, not original insertion order", 1L, result.groups.get(0).sample.num(KIND));
            assertEquals(0L, result.groups.get(1).sample.num(KIND));
        } finally { Locale.setDefault(previous); }
    }

    @Test public void historicalTurkishAggregateSortsReopenWithoutRewritingStoredViews() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            DatasetDef def = def();
            Dataset data = dataset(def, Arrays.asList(
                    row(def, "high-1", "a", 100L, 0L, null, true, "rules"),
                    row(def, "high-2", "b", 99L, 0L, null, true, "rules"),
                    row(def, "low-1", "c", 2L, 1L, null, true, "rules"),
                    row(def, "low-2", "d", 2L, 1L, null, true, "rules")));
            Aggregate.Fn[] functions = {Aggregate.Fn.MIN, Aggregate.Fn.MEDIAN, Aggregate.Fn.COUNT_DISTINCT};
            String[] oldIds = {"m\u0131n:score", "med\u0131an:score", "count_d\u0131st\u0131nct:score"};
            for (int i = 0; i < functions.length; i++) for (boolean descending : new boolean[]{false, true}) {
                Aggregate aggregate = Aggregate.of(functions[i], "score");
                Query legacy = Query.of("things").withGroup(new GroupBy(Arrays.asList("kind"), Arrays.asList(aggregate)))
                        .withSorts(Arrays.asList(new Sort(oldIds[i], descending)));
                File file = tmp.newFile(); SavedViews views = new SavedViews();
                views.save("Existing Turkish view", legacy, 30); views.store(file);
                byte[] before = java.nio.file.Files.readAllBytes(file.toPath());
                Query reopened = SavedViews.load(file).get("Existing Turkish view").query();
                QueryResult result = QueryEngine.run(data, reopened, "rules");
                assertTrue(result.problems.toString(), result.problems.isEmpty());
                assertEquals("historical " + oldIds[i] + ", descending " + descending,
                        descending ? 0L : 1L, result.groups.get(0).sample.num(KIND));
                assertEquals("only the known historical identifier resolves", aggregate.id(), reopened.sorts.get(0).column);
                assertArrayEquals("opening a legacy view does not rewrite the file", before,
                        java.nio.file.Files.readAllBytes(file.toPath()));
            }
        } finally { Locale.setDefault(previous); }
    }

    @Test public void historicalAggregateResolutionLeavesUnknownSortsAndLiteralGroupKeysAlone() {
        Query query = Query.of("things").withGroup(new GroupBy(Arrays.asList("m\u0131n:score"),
                Arrays.asList(Aggregate.of(Aggregate.Fn.MIN, "score"))))
                .withSorts(Arrays.asList(new Sort("m\u0131n:score", true), new Sort("m\u0131n:future-column", false),
                        new Sort("future:score", false)));
        assertEquals(query, QueryCodec.decode(QueryCodec.encode(query)));
    }
}

