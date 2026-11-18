Confirmed follow-up to TEST95 custom-question rate bounds; local fix prepared, not committed/delivered.

A valid lower rate bound12/17 was rounded to >=0.71, which is above the true lower bound(~0.70588). The same nearest rounding can put an upper bound below its true limit. Baseline permanent pipeline case failed0.115s on that lower-bound assertion.

Fix rounds displayed lower bounds downward and upper bounds upward (two decimals), preserving ordinary nearest rounding for exact rates. When both totals are lower bounds, a nonpositive numerator can still establish a valid lower rate bound for a positive denominator; those negative/zero cases are retained instead of shown unknown. Positive numerator+denominator lower bounds remain unknown apart from trivial nonnegativity, with original observations kept in details. Source totals and exact-rate calculations unchanged; underlying double precision remains an existing limitation.

1new test covers outward rounding in both signs and retained negative/zero lower bounds. New case plus existing4rate-bound,Questions,FloorsAndPerUnit,AggregateFirstViews andAggregateExtensions:52unique passes0.527s. TEST95's four earlier regressions remain intact; only their fixture helper becomes package-visible for reuse, no assertions removed.

Full Android/build verification pending. Whole UIMSD audit not complete.
