package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class UimsdTopicSearchTest {
    private Catalog catalog() {
        DatasetDef def = QuerySupport.def();
        Row visible = QuerySupport.row(def, "one", "épée", 13L, 1L, 77L, false, "r");
        visible.setText(QuerySupport.NOTE, "secret internal note", Fact.PROV_RECORDED);
        Catalog c = new Catalog();
        c.add(QuerySupport.dataset(def, Arrays.asList(visible)));
        DatasetDef empty = new DatasetDef("empty", "Empty topic", "Should not be listed");
        empty.add(ColumnDef.num("money", "Coins"));
        c.add(QuerySupport.dataset(empty, Collections.<Row>emptyList()));
        return c;
    }

    @Test public void findsRegisteredTopicAndPublicFieldsWithoutSearchingRunValues() {
        Catalog c = catalog();
        List<UimsdTopicSearch.Hit> topics = UimsdTopicSearch.find(c, " THINGS ");
        assertEquals(1, topics.size());
        assertEquals("things", topics.get(0).datasetId);
        assertEquals("", topics.get(0).columnId);
        List<UimsdTopicSearch.Hit> scores = UimsdTopicSearch.find(c, "SCORE");
        assertEquals(1, scores.size());
        assertEquals("score", scores.get(0).columnId);
        assertTrue("individual text is never indexed", UimsdTopicSearch.find(c, "épée").isEmpty());
        assertTrue("unrecorded dataset excluded", UimsdTopicSearch.find(c, "Coins").isEmpty());
    }

    @Test public void internalColumnsAndValuesNeverEnterPublicSearch() {
        Catalog c = catalog();
        for (String term : new String[]{"secret", "note", "77", "internal note"})
            assertTrue(term, UimsdTopicSearch.find(c, term).isEmpty());
    }

    @Test public void accentFoldingMultipleTermsAndCaseAreDeterministic() {
        DatasetDef def = new DatasetDef("alchemy", "Épée statistics", "Special content");
        def.add(ColumnDef.text("weapon", "Weapon class").shown());
        Catalog c = new Catalog().add(QuerySupport.dataset(def, Arrays.asList(new Row("a", null, "r", 1))));
        assertEquals("alchemy", UimsdTopicSearch.find(c, "EPEE STATISTICS").get(0).datasetId);
        assertEquals("weapon", UimsdTopicSearch.find(c, "  WEAPON   CLASS ").get(0).columnId);
        assertTrue(UimsdTopicSearch.find(c, "unrelated").isEmpty());
        assertTrue(UimsdTopicSearch.find(c, "   ").isEmpty());
        assertTrue(UimsdTopicSearch.find(null, "weapon").isEmpty());
        assertEquals("alchemy", UimsdTopicSearch.find(c, "EPEE").get(0).datasetId);
    }

    @Test public void resultsOpenRealQuestionBuilderWithoutElevatingVisibility() {
        Catalog c = catalog();
        DatasetDef def = c.dataset("things").def;
        Question before = Question.about("other").withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
        UimsdTopicSearch.Hit score = UimsdTopicSearch.find(c, "score").get(0);
        Question total = score.question(before, def);
        assertEquals("things", total.dataset);
        assertEquals(Question.Measure.TOTAL, total.measure);
        assertEquals("score", total.column);
        assertEquals(before.scope.rules, total.scope.rules);
        assertFalse(total.showInternal);
        UimsdTopicSearch.Hit name = UimsdTopicSearch.find(c, "name").get(0);
        Question grouped = name.question(before, def);
        assertEquals(Arrays.asList("name"), grouped.groupBy);
        assertFalse(grouped.showInternal);
    }

    @Test public void resultLimitPreservesCatalogAndColumnOrder() {
        Catalog c = new Catalog();
        for (int i = 0; i < 30; i++) {
            DatasetDef def = new DatasetDef("match-" + i, "Match " + i, "Catalog match");
            def.add(ColumnDef.text("match-column", "Match column"));
            c.add(QuerySupport.dataset(def, Collections.singletonList(new Row("row-" + i, null, "run-" + i, 1))));
        }
        List<UimsdTopicSearch.Hit> hits = UimsdTopicSearch.find(c, "match");
        assertEquals(48, hits.size());
        assertEquals("match-0", hits.get(0).datasetId);
        assertEquals("", hits.get(0).columnId);
        assertEquals("match-column", hits.get(1).columnId);
        assertEquals("match-23", hits.get(46).datasetId);
        assertEquals("", hits.get(46).columnId);
        assertEquals("match-column", hits.get(47).columnId);
    }

    @Test public void datasetTopicClearsStaleQuestionPartsBeforeOpeningBuilder() {
        Catalog c = catalog();
        Question before = Question.about("old")
                .withMeasure(Question.Measure.TOTAL, "old-total")
                .withPer(PerUnit.Unit.RUN)
                .withGroupBy(Collections.singletonList("old-group"))
                .withScope(new Scope(Scope.Rules.ALL, "rules", true, true, true, true));
        UimsdTopicSearch.Hit topic = UimsdTopicSearch.find(c, "things").get(0);
        Question opened = topic.question(before, c.dataset("things").def);
        assertEquals("things", opened.dataset);
        assertEquals(Question.Measure.COUNT, opened.measure);
        assertEquals("", opened.column);
        assertNull(opened.per);
        assertTrue(opened.groupBy.isEmpty());
        assertTrue(opened.filters.isEmpty());
        assertSame(before.scope, opened.scope);
        assertFalse(opened.showInternal);
    }

    @Test public void offlineGrammarCreatesExactCountAndGroupedAggregateQueries() {
        Catalog c = catalog();
        Question start = Question.about("prior").withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
        Question count = UimsdTopicSearch.interpret(c, "How many Things by Name?", start);
        assertNotNull(count);
        assertEquals("things", count.dataset);
        assertEquals(Question.Measure.COUNT, count.measure);
        assertEquals(Collections.singletonList("name"), count.groupBy);
        assertSame(start.scope, count.scope);
        assertFalse(count.showInternal);

        Question average = UimsdTopicSearch.interpret(c, "AVERAGE SCORE OF THINGS BY NAME", start);
        assertNotNull(average);
        assertEquals(Question.Measure.AVERAGE, average.measure);
        assertEquals("score", average.column);
        assertEquals(Collections.singletonList("name"), average.groupBy);
        assertEquals(Aggregate.Fn.AVG, Questions.toQuery(average, c.dataset("things").def).group.aggregates.get(0).fn);

        Question total = UimsdTopicSearch.interpret(c, "total score of things", start);
        assertNotNull(total);
        assertEquals(Question.Measure.TOTAL, total.measure);
        assertTrue(total.groupBy.isEmpty());
    }

    @Test public void unsupportedAndHiddenQuestionPartsAreNeverDiscardedOrExposed() {
        Catalog c = catalog();
        Question start = Question.about("prior").withShowInternal(true);
        for (String phrase : new String[]{
                "how many things by secret", "average secret of things",
                "how many things where score is 2",
                "average score of things by secret",
                "average score of things by unknown",
                "how many things by name by kind",
                "average score of things of something",
                "average score of things by name where score is 2",
                "how many empty topic", "how many things from yesterday"
        }) assertNull(phrase, UimsdTopicSearch.interpret(c, phrase, start));
        Question accepted = UimsdTopicSearch.interpret(c, "how many things", start);
        assertNotNull(accepted);
        assertFalse("parsed questions do not inherit internal visibility", accepted.showInternal);
    }

    @Test public void ambiguousSchemaLabelsDoNotSilentlyChooseAColumnOrDataset() {
        DatasetDef d = new DatasetDef("sample", "Samples", "");
        d.add(ColumnDef.num("a", "Value"));
        d.add(ColumnDef.num("b", "Value"));
        d.add(ColumnDef.text("group1", "Kind"));
        d.add(ColumnDef.text("group2", "Kind"));
        Catalog cat = new Catalog().add(QuerySupport.dataset(d, Collections.singletonList(new Row("a", null, "r", d.size()))));
        assertNull(UimsdTopicSearch.interpret(cat, "total value of samples", Question.about("sample")));
        assertNull(UimsdTopicSearch.interpret(cat, "how many samples by kind", Question.about("sample")));
        DatasetDef another = new DatasetDef("other", "Samples", "");
        another.add(ColumnDef.num("number", "Number"));
        cat.add(QuerySupport.dataset(another, Collections.singletonList(new Row("b", null, "r", another.size()))));
        assertNull(UimsdTopicSearch.interpret(cat, "how many samples", Question.about("sample")));
    }

    @Test public void registeredDatasetsRemainQueryableWithoutHardcodedTopicNames() {
        DatasetDef created = new DatasetDef("new-spell", "Spell casts", "New registered events");
        created.add(ColumnDef.num("spent", "Mana spent"));
        created.add(ColumnDef.text("spell", "Spell"));
        Catalog c = new Catalog().add(QuerySupport.dataset(created, Collections.singletonList(new Row("a", null, "r", created.size()))));
        Question q = UimsdTopicSearch.interpret(c, "mean mana spent of spell casts by spell", Question.about("old"));
        assertNotNull(q);
        assertEquals("new-spell", q.dataset);
        assertEquals(Question.Measure.AVERAGE, q.measure);
        assertEquals("spent", q.column);
        assertEquals(Collections.singletonList("spell"), q.groupBy);
    }
}

