Confirmed custom-question rate/display defects — local verified fix, not committed or delivered yet.

Actual Questions.answer and QueryEngine/PerUnit pipeline reproduced four failing regression cases against current source (4 failures in0.160s):
- Both numerator and denominator marked incomplete: displayed >=4.00 or less, which cannot establish a useful rate bound.
- Lower-bound numerator with exact positive denominator: text >=4.00 but Line.lowerBound incorrectly false (hardcoded && false).
- Exact negative numerator with lower-bound positive denominator: displayed or less when the correct direction is or more.
- Large valid numeric result: Math.round(rate*100) saturated and displayed 92233720368547758.07 instead of the query engine's represented 9223372036854776000.00.

Fix only Questions presentation/knowledge handling: retain individual valid bounds, show ? and NaN sort value for two incomplete totals with an explicit explanation, reverse denominator bound for negative numerators, keep zero exact when denominator is positive, report denominator as at least when incomplete. Ordinary two-decimal rounding remains on its original path; only out-of-long-range results use BigDecimal formatting. Existing floating-point query-engine precision is unchanged; the large-value result is not a claim to recover precision lost by the existing double-based calculation.

New 4 permanent cases include exact/upper/lower/both-incomplete, negative/zero, Long.MAX_VALUE and Long.MIN_VALUE source totals. Updated cases plus existing QuestionsTest, FloorsAndPerUnitTest, AggregateFirstViewsTest and AggregateExtensionsTest:51unique cases passed0.540s. No test weakened. One early local compile named a nonexistent fixture class; corrected test selection before successful compilation/execution.

No raw logs, saves, formulas for exact rates, query operators, game RNG or gameplay changed. Unknown answers preserve the observed numerator/denominator facts in details. Android/full suite/build pending. Whole UIMSD audit remains incomplete.
