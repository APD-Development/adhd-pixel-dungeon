package com.shatteredpixel.shatteredpixeldungeon.apd.query.research;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Datasets;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryResult;

/**
 * Stable read-only opener for the comparable-progress document.
 * Population A is always earlier and B is always recent; opening never rewrites either snapshot.
 */
public final class ComparableProgressEntry {
    public static final String TITLE = "Comparable personal progress";

    private ComparableProgressEntry() {}

    public static final class Opened {
        public final QueryResult earlier, recent;
        public final ComparableProgress.Result result;
        public final String problem;

        private Opened(QueryResult earlier, QueryResult recent,
                       ComparableProgress.Result result, String problem) {
            this.earlier = earlier;
            this.recent = recent;
            this.result = result;
            this.problem = problem;
        }

        public ResearchDocument document() {
            if (!problem.isEmpty()) {
                ResearchDocument document = new ResearchDocument(TITLE);
                document.notes.add(problem);
                return document;
            }
            return ResearchDocument.progress(earlier, recent, result);
        }
    }

    public static Opened open(Catalog catalog, ResearchPins pins, String currentRules,
                              boolean allowInternal, String measure) {
        if (catalog == null) return failed("Recorded history is unavailable.");
        if (pins == null || pins.a == null || pins.b == null)
            return failed("Capture both persistent cohorts first: population A is earlier and B is recent.");
        if (!Datasets.RUNS.equals(pins.a.dataset) || !Datasets.RUNS.equals(pins.b.dataset))
            return failed("Both stored cohorts must use the Runs dataset. No other-table condition was ignored.");
        if (measure == null || measure.isEmpty())
            return failed("Choose a recorded numeric metric or win rate.");
        if ((pins.a.showInternal || pins.b.showInternal) && !allowInternal)
            return failed("A stored cohort includes hidden-information viewing. Explicitly enable it in table Options first; unfinished-run secrets remain protected.");

        QueryResult earlier = ResearchData.select(catalog, pins.a, currentRules);
        QueryResult recent = ResearchData.select(catalog, pins.b, currentRules);
        if (earlier == null || recent == null)
            return failed("A stored dataset, condition or rules identity is unavailable in this build. No condition was ignored and no answer was invented.");
        return new Opened(earlier, recent, ComparableProgress.compare(earlier, recent, measure), "");
    }

    private static Opened failed(String problem) {
        return new Opened(null, null, null, problem);
    }
}
