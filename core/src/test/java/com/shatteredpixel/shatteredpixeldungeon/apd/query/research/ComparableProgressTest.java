package com.shatteredpixel.shatteredpixeldungeon.apd.query.research;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.Filter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Op;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Query;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryCodec;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Scope;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import org.junit.Test;

import static org.junit.Assert.*;

public class ComparableProgressTest {

    @Test public void comparableCohortsReportRecentMinusEarlierWithExactEvidence() {
        ResearchFixture f = new ResearchFixture();
        complete(f, f.add("old-1", "warrior", 0, 10), 1_000);
        complete(f, f.add("old-2", "warrior", 0, 30), 2_000);
        complete(f, f.add("new-1", "warrior", 0, 50), 3_000);
        complete(f, f.add("new-2", "warrior", 1, 70), 4_000);

        ComparableProgress.Result result = ComparableProgress.compare(
                f.result(before(f, 2_000)), f.result(after(f, 3_000)), "result.score");

        assertTrue(result.available());
        assertEquals(40, result.comparison.difference, 0);
        assertEquals(2, result.earlier.n);
        assertEquals(2, result.recent.n);
        assertEquals(1_000, result.earlierFirstMillis);
        assertEquals(2_000, result.earlierLastMillis);
        assertEquals(3_000, result.recentFirstMillis);
        assertEquals(4_000, result.recentLastMillis);
        assertEquals(7, result.conditions.size());
        assertTrue(result.interpretation().contains("not proof of improvement"));
        assertFalse(result.comparison.intervalKnown);
    }

    @Test public void overlapAndInterleavedDatesFailClosed() {
        ResearchFixture f = new ResearchFixture();
        complete(f, f.add("a", "warrior", 0, 10), 1_000);
        complete(f, f.add("b", "warrior", 0, 20), 2_000);
        ComparableProgress.Result overlap = ComparableProgress.compare(f.result(), f.result(), "result.score");
        assertFalse(overlap.available());
        assertTrue(overlap.problem.contains("same run"));

        QueryResult first = f.result(f.query().addFilter(Filter.num(FactKeys.START_MILLIS, Op.EQ, 2_000)));
        QueryResult second = f.result(f.query().addFilter(Filter.num(FactKeys.START_MILLIS, Op.EQ, 1_000)));
        ComparableProgress.Result reversed = ComparableProgress.compare(first, second, "result.score");
        assertFalse(reversed.available());
        assertTrue(reversed.problem.contains("earlier cohort"));
    }

    @Test public void changedOrUnknownRunConditionsAreNeverCalledProgress() {
        ResearchFixture f = new ResearchFixture();
        Row old = f.add("old", "warrior", 0, 10); complete(f, old, 1_000);
        Row changed = f.add("changed", "warrior", 0, 30); complete(f, changed, 2_000);
        f.num(changed, FactKeys.DIFFICULTY, 4);
        ComparableProgress.Result difficulty = ComparableProgress.compare(
                f.result(before(f, 1_000)), f.result(after(f, 2_000)), "result.score");
        assertFalse(difficulty.available());
        assertTrue(difficulty.problem.contains("Difficulty"));

        f.num(changed, FactKeys.DIFFICULTY, 3);
        changed.clear(f.def.indexOf(CustomRuleFacts.IDENTITY));
        ComparableProgress.Result unknown = ComparableProgress.compare(
                f.result(before(f, 1_000)), f.result(after(f, 2_000)), "result.score");
        assertFalse(unknown.available());
        assertTrue(unknown.problem.contains("Exact custom settings"));
    }

    @Test public void missingDatesAndMetricsStayUnknownInsteadOfDefaultsOrZero() {
        ResearchFixture f = new ResearchFixture();
        Row old = f.add("old", "warrior", 0, 10); complete(f, old, 1_000);
        Row recent = f.add("recent", "warrior", 0, 30); complete(f, recent, 2_000);
        recent.clear(f.def.indexOf(FactKeys.START_MILLIS));
        ComparableProgress.Result noDate = ComparableProgress.compare(
                f.result(f.query().addFilter(Filter.num("result.score", Op.EQ, 10))),
                f.result(f.query().addFilter(Filter.num("result.score", Op.EQ, 30))), "result.score");
        assertFalse(noDate.available());
        assertTrue(noDate.problem.contains("start date"));

        f.num(recent, FactKeys.START_MILLIS, 2_000);
        recent.setNum(f.def.indexOf("result.score"), 30, Fact.PROV_INCOMPLETE);
        ComparableProgress.Result noMetric = ComparableProgress.compare(
                f.result(before(f, 1_000)), f.result(after(f, 2_000)), "result.score");
        assertFalse(noMetric.available());
        assertTrue(noMetric.problem.contains("exact recorded metric"));
    }

    @Test public void progressDocumentExportsDefinitionsDatesConditionsAndExactContributors() {
        ResearchFixture f = new ResearchFixture();
        complete(f, f.add("old", "warrior", 0, 10), 86_400_000L);
        complete(f, f.add("new", "warrior", 1, 30), 172_800_000L);
        QueryResult earlier = f.result(before(f, 86_400_000L)), recent = f.result(after(f, 172_800_000L));
        ComparableProgress.Result result = ComparableProgress.compare(earlier, recent, "result.score");
        ResearchDocument document = ResearchDocument.progress(earlier, recent, result);
        assertEquals("Comparable personal progress", document.title);
        assertEquals(4, document.cards.size());
        assertTrue(document.cards.get(0).label.contains("recent minus earlier"));
        assertTrue(document.cards.get(0).detail.contains("1970-01-02"));
        assertTrue(document.cards.get(0).detail.contains("1970-01-03"));
        assertEquals("run-new", document.cards.get(0).evidence.get(0).rows.get(0).runId);
        assertEquals("run-old", document.cards.get(0).evidence.get(1).rows.get(0).runId);
        String csv = com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportRenderer.csv(document.report(1, "test", false));
        assertTrue(csv.contains("not proof of improvement or causation"));
        assertTrue(csv.contains("run-new"));
        assertTrue(csv.contains("run-old"));
        assertTrue(csv.contains("Exact query"));
    }

    @Test public void unavailableProgressDocumentExportsTheExactFailureWithoutEvidence() {
        ResearchFixture f = new ResearchFixture();
        Row old = f.add("old", "warrior", 0, 10); complete(f, old, 1_000);
        Row recent = f.add("recent", "warrior", 0, 30); complete(f, recent, 2_000); f.num(recent, FactKeys.DIFFICULTY, 4);
        QueryResult earlier = f.result(before(f, 1_000)), newer = f.result(after(f, 2_000));
        ComparableProgress.Result result = ComparableProgress.compare(earlier, newer, "result.score");
        ResearchDocument document = ResearchDocument.progress(earlier, newer, result);
        assertFalse(result.available());
        assertTrue(document.cards.isEmpty());
        assertTrue(document.notes.get(0).contains("Difficulty"));
        assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportRenderer.csv(document.report(1, "test", false)).contains("Difficulty"));
    }

    @Test public void excludedMetricRowsRemainInspectibleButNotContributors() {
        ResearchFixture f = new ResearchFixture();
        complete(f, f.add("old", "warrior", 0, 10), 1_000);
        complete(f, f.add("new", "warrior", 0, 30), 2_000);
        Row unknown = f.add("new-unknown", "warrior", 0, 999); complete(f, unknown, 3_000); unknown.clear(f.def.indexOf("result.score"));
        QueryResult earlier = f.result(before(f, 1_000)), recent = f.result(after(f, 2_000));
        ComparableProgress.Result result = ComparableProgress.compare(earlier, recent, "result.score");
        ResearchDocument document = ResearchDocument.progress(earlier, recent, result);
        assertTrue(result.available());
        assertEquals(1, result.recent.n); assertEquals(1, result.recent.excluded);
        assertEquals(1, document.cards.get(0).evidence.get(0).rows.size());
        assertEquals(2, document.cards.get(3).evidence.get(0).rows.size());
        assertTrue(document.cards.get(3).detail.contains("1 matching rows excluded"));
        assertTrue(document.cards.get(3).detail.contains(QueryCodec.encode(recent.query)));
    }

    @Test public void persistentPopulationPinsReopenTheSameReadOnlyProgressDocument() throws Exception {
        ResearchFixture f = new ResearchFixture();
        complete(f, f.add("old", "warrior", 0, 10), 1_000);
        complete(f, f.add("new", "warrior", 1, 30), 2_000);
        java.io.File root = java.nio.file.Files.createTempDirectory("progress-entry-").toFile();
        java.io.File file = ResearchPins.file(root);
        ResearchPins stored = new ResearchPins(null, null)
                .with(true, before(f, 1_000), "rules")
                .with(false, after(f, 2_000), "rules");
        stored.store(file);
        byte[] before = java.nio.file.Files.readAllBytes(file.toPath());

        ResearchPins reopened = ResearchPins.load(file);
        ComparableProgressEntry.Opened opened = ComparableProgressEntry.open(
                f.catalog(), reopened, "different-current-rules", true, "result.score");
        ResearchDocument document = opened.document();

        assertTrue(opened.result.available());
        assertEquals("Comparable personal progress", document.title);
        assertEquals(4, document.cards.size());
        assertEquals(QueryCodec.encode(stored.a), QueryCodec.encode(opened.earlier.query));
        assertEquals(QueryCodec.encode(stored.b), QueryCodec.encode(opened.recent.query));
        assertTrue(document.cards.get(0).label.contains("recent minus earlier"));
        assertArrayEquals(before, java.nio.file.Files.readAllBytes(file.toPath()));
    }

    @Test public void progressEntryRequiresExplicitHiddenDisclosureWithoutChangingPins() throws Exception {
        ResearchFixture f = new ResearchFixture();
        complete(f, f.add("old", "warrior", 0, 10), 1_000);
        complete(f, f.add("new", "warrior", 0, 30), 2_000);
        ResearchPins pins = new ResearchPins(null, null)
                .with(true, before(f, 1_000).withShowInternal(true), "rules")
                .with(false, after(f, 2_000), "rules");
        String earlier = QueryCodec.encode(pins.a), recent = QueryCodec.encode(pins.b);

        ComparableProgressEntry.Opened blocked = ComparableProgressEntry.open(
                f.catalog(), pins, "rules", false, "result.score");

        assertNull(blocked.result);
        assertTrue(blocked.document().cards.isEmpty());
        assertTrue(blocked.document().notes.get(0).contains("Explicitly enable"));
        assertEquals(earlier, QueryCodec.encode(pins.a));
        assertEquals(recent, QueryCodec.encode(pins.b));
    }

    @Test public void progressEntryRejectsMissingAndNonRunSnapshotsWithoutInventingNavigation() {
        ResearchFixture f = new ResearchFixture();
        ComparableProgressEntry.Opened missing = ComparableProgressEntry.open(
                f.catalog(), new ResearchPins(null, null), "rules", false, "result.score");
        assertTrue(missing.document().notes.get(0).contains("population A is earlier"));
        ResearchPins other = new ResearchPins(
                Query.of("items").withScope(Scope.DEFAULT.withRules(Scope.Rules.SPECIFIC, "rules")),
                Query.of("items").withScope(Scope.DEFAULT.withRules(Scope.Rules.SPECIFIC, "rules")));
        ComparableProgressEntry.Opened wrong = ComparableProgressEntry.open(
                f.catalog(), other, "rules", false, "result.score");
        assertTrue(wrong.document().notes.get(0).contains("Runs dataset"));
        assertNull(wrong.earlier);
        assertNull(wrong.recent);
    }

    private static void complete(ResearchFixture f, Row row, long started) {
        f.num(row, FactKeys.START_MILLIS, started);
        f.num(row, FactKeys.DIFFICULTY, 3);
        f.num(row, FactKeys.DIFFICULTY_BALANCE, 0);
        f.num(row, FactKeys.CHALLENGES, 0);
    }

    private static Query before(ResearchFixture f, long inclusive) {
        return f.query().addFilter(Filter.num(FactKeys.START_MILLIS, Op.LE, inclusive));
    }

    private static Query after(ResearchFixture f, long inclusive) {
        return f.query().addFilter(Filter.num(FactKeys.START_MILLIS, Op.GE, inclusive));
    }
}
