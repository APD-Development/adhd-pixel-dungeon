# TEST110 preparation — precise run navigation and report completeness

## Confirmed bugs and corrections
Run reports used ordinary case-insensitive EQ text searches for case-sensitive run identifiers. Run-A and run-a are both valid stored identifiers, but selecting either could include the other run's item, enemy and death rows. Row links had the same EQ error; group-to-run links used case-insensitive IN and could open extra runs. The report lookup and single row links now use the existing exact text predicate. Group links use an additive exact identifier-membership predicate, outside the normal filter picker. Normal player-entered EQ/IN search semantics remain unchanged. Hidden/unknown filtering and existing scope widening are retained.

CSV reports omitted the omitted-row notice already present in Markdown, text and HTML. Large CSV reports could appear complete despite the existing 1000-row report presentation limit. CSV now discloses each table's omitted count. No new cap, data deletion, truncation of source datasets or gameplay change.

## Regression evidence
Five new methods cover exact single-run reports across item/enemy/death sections and all four report formats, ordinary case-insensitive search, row links in both directions with Back, group links and QueryCodec round trips, and real 1003-row query results with two separately omitted report tables. Three case-identity reproductions and the CSV omission reproduction failed on the previous implementation. Corrected new and existing report/model/group/SavedViews tests:57 unique methods passed,39.602seconds on the host JVM. Initial local harness attempts used a nonexistent QueryCodecTest and omitted the required source-introspection property; these setup errors were corrected without weakening assertions. The final suite uses actual current source fixtures.

Strengthened the existing API34 UIMSD screen test with real PointerEvent taps on native row links and group-to-run actions, checking that case-distinct extra records stay out and Back works. These Android assertions have not run for TEST110 yet. Existing 21 instrumentation methods and22 screenshots remain required; expected core total1309.

## TEST109 evidence and remaining limits
TEST109 source845972359883207d3b2dea41861e25ae3610bab3, verification38367192732,08:07:58–08:19:04UTC. Debug build,1304 unique core methods and21 API34 emulator methods passed with zero failures/errors/skips;22PNG files validated. TEST109 was produced and verified but has not been privately published; the most recent delivered playable fallback remains TEST108. TEST110 compares its history with this actual preceding TEST109 APK, not an arbitrary older build.

Current UIMSD review checkpoint:83/211 scoped production components reviewed; comprehensive audit is incomplete, and integration hooks still need review. Unmatched rollback uncertainty remains unresolved. Current real phone and owner save unavailable. No continuous Codex background worker demonstrated.
