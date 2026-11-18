package com.shatteredpixel.shatteredpixeldungeon.apd.query.research;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Datasets;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A fail-closed comparison of an earlier and a later set of finished runs.
 * This only describes recorded cohorts; it never treats a difference as player improvement.
 */
public final class ComparableProgress {

    private ComparableProgress() {}

    private static final String[] CONDITIONS = {
            FactKeys.APD_VERSION,
            FactKeys.HERO_CLASS,
            FactKeys.DIFFICULTY,
            FactKeys.DIFFICULTY_BALANCE,
            FactKeys.CHALLENGES,
            CustomRuleFacts.IDENTITY
    };

    public static final class Result {
        public final PopulationStudy.Sample earlier;
        public final PopulationStudy.Sample recent;
        /** Population A is recent and B is earlier, so difference is recent minus earlier. */
        public final PopulationStudy.Comparison comparison;
        public final Map<String, String> conditions;
        public final long earlierFirstMillis, earlierLastMillis, recentFirstMillis, recentLastMillis;
        public final String problem;

        private Result(PopulationStudy.Sample earlier, PopulationStudy.Sample recent,
                       PopulationStudy.Comparison comparison, Map<String, String> conditions,
                       long earlierFirstMillis, long earlierLastMillis,
                       long recentFirstMillis, long recentLastMillis, String problem) {
            this.earlier = earlier;
            this.recent = recent;
            this.comparison = comparison;
            this.conditions = Collections.unmodifiableMap(new LinkedHashMap<>(conditions));
            this.earlierFirstMillis = earlierFirstMillis;
            this.earlierLastMillis = earlierLastMillis;
            this.recentFirstMillis = recentFirstMillis;
            this.recentLastMillis = recentLastMillis;
            this.problem = problem;
        }

        public boolean available() {
            return problem.isEmpty();
        }

        public String interpretation() {
            if (!available()) return problem;
            return "Recent minus earlier for the same recorded APD version, class, difficulty, "
                    + "balance, challenges, exact Custom Rules and rules identity. This is an "
                    + "observational difference, not proof of improvement or causation. Unknown "
                    + "conditions and metric values are not treated as defaults or zero.";
        }
    }

    public static Result compare(QueryResult earlierRows, QueryResult recentRows, String measure) {
        String inputProblem = inputProblem(earlierRows, recentRows);
        if (!inputProblem.isEmpty()) return failed(inputProblem);

        PopulationStudy.Sample earlier = PopulationStudy.sample(earlierRows, measure);
        PopulationStudy.Sample recent = PopulationStudy.sample(recentRows, measure);
        if (earlier == null || recent == null)
            return failed("The selected metric is unavailable for one of these run cohorts.");
        if (earlier.n == 0 || recent.n == 0)
            return failed("Each cohort needs at least one exact recorded metric value.");

        PopulationStudy.Comparison comparison = PopulationStudy.compare(recent, earlier);
        if (comparison == null)
            return failed("The two cohorts do not use the same run dataset and metric.");
        if (comparison.overlappingRuns != 0)
            return failed("Earlier and recent cohorts must not contain the same run.");

        Map<String, String> conditions = new LinkedHashMap<>();
        String rules = sharedRules(earlier.contributors, recent.contributors);
        if (rules == null)
            return failed("Every metric contributor needs the same known, unmixed rules identity.");
        conditions.put("run.ruleset", rules);

        for (String key : CONDITIONS) {
            String value = sharedExact(earlierRows, earlier.contributors, recentRows, recent.contributors, key);
            if (value == null)
                return failed("Every metric contributor needs the same exact recorded " + label(earlierRows, key) + ".");
            conditions.put(key, value);
        }

        long[] earlierRange = dateRange(earlierRows, earlier.contributors);
        long[] recentRange = dateRange(recentRows, recent.contributors);
        if (earlierRange == null || recentRange == null)
            return failed("Every metric contributor needs an exact positive recorded start date.");
        if (earlierRange[1] >= recentRange[0])
            return failed("The earlier cohort must end before the recent cohort begins.");

        return new Result(earlier, recent, comparison, conditions,
                earlierRange[0], earlierRange[1], recentRange[0], recentRange[1], "");
    }

    private static String inputProblem(QueryResult earlier, QueryResult recent) {
        if (earlier == null || recent == null) return "Both run cohorts must be available.";
        if (earlier.isGrouped() || recent.isGrouped() || !earlier.problems.isEmpty() || !recent.problems.isEmpty())
            return "Progress cohorts must be ungrouped exact queries with no ignored conditions.";
        if (!Datasets.RUNS.equals(earlier.def.id) || !Datasets.RUNS.equals(recent.def.id))
            return "Comparable progress currently accepts only the Runs dataset.";
        return "";
    }

    private static String sharedRules(List<Row> earlier, List<Row> recent) {
        String value = null;
        for (Row row : concat(earlier, recent)) {
            if (!row.finished || row.ruleset == null || row.ruleset.isEmpty() || Row.RULESET_MIXED.equals(row.ruleset))
                return null;
            if (value == null) value = row.ruleset;
            else if (!value.equals(row.ruleset)) return null;
        }
        return value;
    }

    private static String sharedExact(QueryResult earlierResult, List<Row> earlier,
                                      QueryResult recentResult, List<Row> recent, String key) {
        String value = null;
        for (Row row : earlier) {
            String next = exactValue(earlierResult, row, key);
            if (next == null) return null;
            if (value == null) value = next;
            else if (!value.equals(next)) return null;
        }
        for (Row row : recent) {
            String next = exactValue(recentResult, row, key);
            if (next == null) return null;
            if (value == null) value = next;
            else if (!value.equals(next)) return null;
        }
        return value;
    }

    private static String exactValue(QueryResult result, Row row, String key) {
        ColumnDef column = result.def.column(key);
        if (column == null || !ResearchData.exact(result, row, key)) return null;
        if (column.type == ColumnDef.Type.TEXT) return "t:" + Objects.toString(ResearchData.text(result, row, key), "");
        Long value = ResearchData.number(result, row, key);
        return value == null ? null : "n:" + value;
    }

    private static long[] dateRange(QueryResult result, List<Row> rows) {
        long first = Long.MAX_VALUE, last = Long.MIN_VALUE;
        for (Row row : rows) {
            if (!ResearchData.exact(result, row, FactKeys.START_MILLIS)) return null;
            Long value = ResearchData.number(result, row, FactKeys.START_MILLIS);
            if (value == null || value <= 0) return null;
            first = Math.min(first, value);
            last = Math.max(last, value);
        }
        return first == Long.MAX_VALUE ? null : new long[]{first, last};
    }

    private static String label(QueryResult result, String key) {
        ColumnDef column = result.def.column(key);
        return column == null ? key : column.label;
    }

    private static List<Row> concat(List<Row> first, List<Row> second) {
        java.util.ArrayList<Row> all = new java.util.ArrayList<>(first.size() + second.size());
        all.addAll(first);
        all.addAll(second);
        return all;
    }

    private static Result failed(String problem) {
        return new Result(null, null, null, Collections.emptyMap(), 0, 0, 0, 0, problem);
    }
}
