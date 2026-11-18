# UIMSD query review continuation — October 15, 2026

This records a bounded continuation of the approved audit, not completion of the entire system. No gameplay, query behavior, raw history, save format or storage format was changed.

- core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/QueryCodec.java — Git blob `3a643d13367ca9ef51cc109667fc7bf7b52ee368`, 180 source lines read in full.
- core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/Question.java — Git blob `6074335406487d291f8958066d6df39d052a2d7b`, 133 source lines read in full.
- core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/Questions.java — Git blob `6d484613e55379efcbbff6bf30d0bc36922146c3`, 245 source lines read in full.

QueryCodec: reviewed escaped filter texts including literal pipes/backslashes/empty values, numeric lists, complete scope bits, malformed-line rejection, forward-compatible unknown lines, group/aggregate round trips and retained locale-alias recovery. Existing empty-text persistence checks are retained. No new confirmed defect.

Question: reviewed defensive copies, immutable filter/group lists, dataset-change reset behavior, preservation of scope/internal visibility, valid per-unit combinations and edit methods. Invalid API indexes/null lists are not accepted as gameplay defects without a supported caller path; no speculative change made.

Questions: reviewed dataset/column eligibility, query conversion, empty/missing catalog behavior, grouped labels, numerator/denominator lower-bound combinations, unknown rates, conservative rounding, large finite number formatting and stable answer ordering. Checked PerUnit.Result.per's zero/unknown denominator behavior as an interaction, not a claim of rereviewing the entire PerUnit file. No new confirmed defect.

Existing TEST204 exact-source core XML: all1631 methods pass. Relevant retained suites include QueryEmptyTextPersistenceTest5, QuestionRateBoundsTest4 and QuestionsTest7; all16 passed with zero failures/errors/skips. These tests are existing coverage, not newly added tests or proof of every imaginable case. Full Android acceptance for TEST204 is recorded separately.

The repository checkpoint reports109reviewed of211 at an older source; the existing local review checkpoint preserves135 reviewed entries at that source. Do not silently reset or merge those numbers into a current-source completion claim. These three current-hash reviews are recorded separately pending checkpoint reconciliation. Entire UIMSD audit, current-source drift checks and core-game integration review remain incomplete. Continue from retained work; do not delete raw history to resolve derived cache problems.


Additional complete current-file reviews: TableGeometry (column sizing, frozen-first-column width, row/column hit tests, bounded header wrapping) and WindowGeometry (narrow viewport width, wrapped action heights, pane sizing and row stacking). No supported-input defect was reproduced; no speculative behavior or table-size limit was introduced. TableGeometryTest7 and WindowGeometryTest10 passed in the same exact-source core XML. All five reviewed Git blobs were separately matched against tested TEST204 source245b21b7. Total retained targeted coverage cited here is33 existing methods, not33new tests.
