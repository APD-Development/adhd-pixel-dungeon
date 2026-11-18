## 2026-10-17 — impossible legacy calendar dates normalized into run starts — fixed and fully gated
**Surface:** UIMSD legacy import / `RowBuilders.parseDate`.

**Confirmed failure:** `2025-02-29`, `2024-04-31` and `1900-02-29` were accepted by lenient Calendar normalization and could create false `run.startMillis` values. Reproduction run `39014149854` failed exactly the retained method `legacyDatesRejectImpossibleCalendarDaysButKeepRealLeapDays`.

**Correction:** `d76178a015b68344adfbeab2edfd701201c752a7` validates month/day bounds with Gregorian leap-year rules before constructing the timestamp. Real leap days 2024-02-29 and 2000-02-29 remain valid. Raw history is not rewritten.

**Evidence:** corrected core-only run `39014400159` passed 1,774 methods; full TEST267 run `39014635516` passed 1,774 core methods, 38 API34 methods, debug/history checks and 57 reviewed PNGs. Supplemental run `39014635513` retains a separate short-landscape Hide UI failure; it does not alter the green required gate.

# UIMSD audit — confirmed controlled regressions

Full audit incomplete. These are controlled tests, not claims of observed phone failures.

1. ArchiveImporter ignored ZIP entries bypass existing512MiB limit. Medium resource exhaustion. Exact-source baseline accepts513MiB controlled entry; bounded drain rejects. Five tests cover boundaries/progress/history preservation. Archive-only CI run38143393376 passed, not delivered.
2. RunIndex negative decoded unsigned count accepts empty derived index with valid CRC and hides existing logs. Low display reliability. One-line count guard forces rebuilding; source logs untouched. Baseline failed; regression and empty-index compatibility tests pass.
3. RunLogReader unsigned length overflow accepted as empty valid unknown frame. Low malformed-input integrity. Nonnegative size guard rejects; valid future frames remain supported. Baseline failed, fixed tests pass.
4. RunLogReader resync huge sparse string ID exhausts32MiB heap on tiny valid-checksum input. Medium reader-API robustness. Bound impossible recovery IDs by file length before allocating holes. Exact-source OutOfMemoryError reproduced; fixed small-heap test passed. No production resync caller found; not claimed as an observed phone crash.
5. RunLogWriter create exists/open race truncates competing log. High integrity if race occurs. Controlled baseline changes existing5-byte evidence into28-byte header. Atomic file claim refuses race without altering evidence. Regression passes. No observed player data loss claimed.
6. RunLogWriter close timeout falsely reports completion while worker blocked; repeated close also incorrectly true. Medium API correctness. Require completion latch/repeated worker completion; asynchronous writer still finishes after release. Controlled baseline failed; fixed test preserves accepted event/seal.

Tests:12 new permanent tests. Combined130 local archive/reader/index/writer tests passed, zero failures. Full accumulated-source CI requested; physical-phone checks pending. None of these fixes is in delivered TEST45.

Unresolved: manifest linear find yields quadratic import lookup; cross-instance derived-index temp-file races; huge-file whole-memory reader cost; archive compression wrapper lifetime. These remain investigation items, not claimed normal-run defects. Existing functionality, save formats, gameplay and player statistics preserved. SPD inherited-code investigation remains read-only.

Update 2026-10-14T20:58:00Z: the earlier manifest linear lookup item had already been resolved by the retained first-match map and TEST48-era coverage. TEST171 now addresses the distinct cross-instance `RunIndex` fixed-temp save race with a controlled retained regression; verification is pending. Huge-file whole-memory reader cost and archive compression-wrapper lifetime remain investigation items, not claimed normal-run defects.

Update 2026-10-15T02:40:14.718Z: a distinct current-source review confirmed that separate `SavedViews` objects shared one fixed `saved_views.txt.tmp` without cross-instance serialization. TEST190 serializes only that atomic replacement section and adds a controlled two-writer regression. Exact-source full verification is pending; this is not an observed phone data-loss claim. The huge-file reader cost and archive compression-wrapper lifetime remain investigation items.

Update 2026-10-15T02:58:31.520Z: TEST190 source `97c4848581e2f7fcd979d3965e257bdae59c5a93` / run `38672320647` passed complete core/debug/history and 27/27 API34; runner validated 35 PNG dimensions. Artifact-content extraction and visual review remain pending, so retain TEST189 as the fully reviewed checkpoint and reuse TEST190 artifacts without rebuilding.


Update 2026-10-15T03:41:39Z: the remaining archive compression-wrapper lifetime item is confirmed in current source: both public operations created ZIP wrappers that were never closed, leaving their inflater/deflater cleanup to garbage collection. TEST191 closes owned ZIP wrappers on every path through non-closing adapters so caller-supplied document-provider streams remain caller-owned. Two retained tests cover exporter/importer ownership and direct ZIP-wrapper closure. Exact-source full verification is pending; no observed phone failure or history-format change is claimed.


Update 2026-10-15T04:00:07Z: TEST191 source `7e6cae8900c97eaa920bbe8baebb657ec5e539be` / run `38677900668` passed 1,606 core and 27 API34 tests with zero failures/errors/skips, debug/history, and 35 reviewed screenshots. The archive compression-wrapper lifetime investigation item is resolved. Huge-file whole-memory reader cost remains an investigation item, not a claimed normal-run defect.


Update 2026-10-15T04:38:01Z: the remaining huge-file whole-memory reader item is confirmed as a controlled API/resource issue. TEST192 streams ordinary file reads through the format-bounded header and one frame at a time, while explicit resynchronization retains the prior random-access parser. Parser-parity and 1 GiB sparse corrupt-tail regressions are added. Exact-source full verification is pending; no observed phone crash or normal-log-size claim is made.


Update 2026-10-15T04:57:30Z: TEST192 source `222189f971dc55597f561eb87aadcd4e34b47a2e` / run `38682901119` passed 1,608 core and 27 API34 tests with zero failures/errors/skips, debug/history, and 35 reviewed screenshots. The huge-file whole-memory ordinary-reader investigation item is resolved; explicit resync intentionally retains random-access buffering.


October15 row-source identity correction: a distinct current DatasetStore cache accepted stale rows after an equal-name/length/timestamp valid raw-log replacement(score31instead47). Raw SHA-bound disposable row format3 and four permanent regressions are integrated;13focused and617broader local tests passed, full current-source1626core/Android/debug gate pending. Raw histories/saves untouched; old caches rebuild. Fixed optional-cache temp paths across distinct DatasetStore/DerivedStore objects remain a separate investigation candidate. See APD-ROW-CACHE-SOURCE-IDENTITY-2026-10-15.md; do not call the entire audit complete.


Update 2026-10-16T12:35:52.018Z: current QueryEngine numeric IN filtering is confirmed to bypass lower-bound uncertainty: a minimum value can be reported as an exact match and higher possible candidates as exact non-matches. TEST242 changes only this branch and adds a retained minimum/higher/all-below regression. Exact-source before/after plus the full1717core/debug/history/33API34/42PNG gate are pending. No raw history, save, cache-format, score or gameplay change is involved.


Correction 2026-10-16T12:43:28.551Z: the proposed TEST242 numeric-IN lower-bound issue is not a production defect. Op.IN is intentionally absent from Op.ops(NUM), so QueryEngine rejects the synthetic numeric filter before eval; IN is valid only for text/enum, while lower-bound semantics apply to numeric values. Dedicated38901726173 reproduced the synthetic failure on TEST241 and then still failed1 of1717 corrected methods, exposing the invalid premise. The application/test/workflow candidate is reverted; TEST241 remains current and no rebuild is warranted.


Update 2026-10-17T01:41:04Z: the October 15 fixed optional-cache temp-path investigation candidate is resolved without a production change. Current `DerivedStore` and `DatasetStore` both create unique per-writer temporary files. The retained low-level `RowCacheConcurrentWriterTest` deterministically covers destination replacement ordering; new end-to-end `CacheConcurrencyTest` drives 96 contested operations through independent complete store instances, verifies every derived table/catalog, and requires a fresh reader to reuse the final caches. Core-only run `38991720872` passed all 1,763 tests in 348 suites, including both new methods, with zero failures/errors/skips. This closes only that cache-concurrency candidate; the broader UIMSD audit remains incomplete. No raw history, save, scoring, gameplay, application source or APK changed.

## 2026-10-17T04:10:11Z — Imported aggregate provenance omitted from display
Current-source review confirmed that `QueryEngine` set `AggValue.LEGACY` whenever an aggregate included imported-ranking data, but `CellFormatter.aggregate()` ignored that flag. Legacy-only grouped statistics therefore appeared exact, and mixed lower-bound/inferred/imported aggregates omitted the imported-data note. The bounded correction in `4d4048f80ab150aca3bc1ce690de48b5532922a0` exposes the existing legacy cell state and adds the imported-ranking detail while preserving lower-bound > inferred > legacy warning priority, numeric values, raw histories and query math.

Three retained `AggValueContractTest` methods cover imported-only and combined provenance, RATE/RATIO hit/out-of/contributing/total metadata, and zero-denominator unknown results. Full TEST264 run `39000124500` passed 1,768 core and 38 API-34 methods with zero failures/errors/skips; 57 required and 25 supplemental display captures were decoded and reviewed. Independent stage `39001343302` reused the exact APK and verified signer/app ID/version compatibility. No physical-phone failure, save mutation, scoring change, publication or tester distribution is claimed.



## 2026-10-17T07:08:00Z — Hidden-details transition left stale internal question parts
Current-source review confirmed that `WndUimsdAsk` only changed its visibility boolean when Hidden details was disabled. Previously selected internal measure, split or condition values remained in the `Question` and its visible sentence, while QueryEngine correctly rejected them. The result could describe a question different from the query actually executed.

Application commit `09ba71aab8fd2cb426679add9c9d0ed2e4d72fb7` adds one visibility transition in `Questions`, removes only internal selections, resets an internal measure to Count, preserves public selections and valid per-unit choices, and sanitizes before both sentence formatting and query construction. Two retained `QuestionsTest` methods cover direct transition behavior and stale-question answer integrity.

Full TEST266 source `e678d38c4c34bc755ed0cd501f7f44c88bc3c465` / run `39011090870` passed 1,773 core and 38 API-34 methods with zero failures/errors/skips, debug/history, and 57 reviewed required PNGs. TEST265 `39010922142` remains a failed-closed history-ledger precursor with no APK. TEST266 staging is pending; no physical-phone, owner-save, publication or distribution claim. Raw histories, saves, caches, scoring, gameplay, turns and RNG are unchanged.
