# UIMSD comparable-condition progress foundation — 2026-10-25T22:11:36Z

## Native API34 acceptance checkpoint — 2026-11-02T20:29:14Z

Test-only source `e7fce780ff2ff67fa6e53a9ad96dcc65397cc60e` added a real native-pointer acceptance path for the production Comparable personal progress menu. Against the unchanged exact TEST345 APK, the test captured two exact 30-run cohorts with identical class/version/difficulty/balance/challenges/Custom Rules, displayed Score as `20.00` recent minus earlier, preserved the recent query and original 10/30 scores across scene recreation, opened all 30 recent exact metric contributors, selected `run-30`, and retained Back navigation.

Run 41515078067 attempt 1 passed the new path and 67 other methods except for one pre-existing warm-Home timing miss at 256 ms versus 250 ms. The single unchanged retry passed all 68 methods in 546.978 s. The first and second artifacts are `13176610306` (digest `c6e076…`) and `13177051685` (digest `49d607…`). Exact provenance and limitations are recorded in `APD-UIMSD-COMPARABLE-PROGRESS-ACCEPTANCE-2026-11-02.md`.

The test screenshot was created on-device but omitted from both archives because its new filename was absent from the shared pull/validation allowlist. Therefore native behavioral acceptance is green, while new-screen visual artifact acceptance remains pending. The allowlist is corrected for the next applicable current-source display run. No APK was rebuilt, and no QoL source was relabelled as TEST345 evidence.


## Result

Exact source `a771a64087d1c926c2057d66a1f10946577b9f0e` passed the complete core suite in [run 40238803735](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/40238803735), job `121305970033`: 2,077 methods in 403 suites, zero failures, errors or skips.

Retained artifact: `12615950934`, 977,681 bytes, digest `sha256:bbbf22c54c3840d8d8e70276d76a1f9fb46a29e4ece7c6b152332e2097c197c1`.

## Accepted core behavior

`ComparableProgress` accepts two explicit, ungrouped Runs queries and a metric. A comparison is available only when:

- exact metric contributors have distinct run identities across cohorts;
- every contributor has an exact positive recorded start time;
- the earlier cohort ends before the recent cohort begins;
- every contributor has the same known, unmixed rules identity;
- APD version, class, difficulty, balance revision, challenges and exact Custom Rules identity are recorded exactly and match.

The numeric direction is recent minus earlier. The interpretation explicitly says the result is observational and is not proof of improvement or causation. Unknown conditions, missing dates and incomplete metrics never become current defaults or zero. PopulationStudy retains the existing minimum-sample, overlap, independence, version/rules and uncertainty gates.

## Retained regressions

1. Exact comparable cohorts produce recent-minus-earlier with exact ranges and contributor counts.
2. Shared runs and reversed/interleaved dates fail closed.
3. Changed difficulty and unknown exact Custom Rules identity fail closed.
4. Missing dates and incomplete metrics remain unavailable rather than being inferred.

## Boundaries

This is a read-only core/model foundation. It adds no UI, export, telemetry, gameplay, save/history migration, raw-history mutation, scoring change, Custom Rules change or RNG use. No APK was built. TEST313 remains the newest emulator-verified developer/player APK pair in GitHub Actions artifacts; TEST262 remains the newest APK uploaded to GitHub as a release. Physical-device behavior is unmeasured.


## Evidence/export checkpoint — 2026-10-26T14:07:14Z

Exact source `7c2d46edae9bc884bac1bd1309259249be2335ba` passed the complete core suite in [run 40336065670](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/40336065670), job `121617849976`: 2,080 methods in 403 suites, zero failures, errors or skips. Retained artifact `12655929494`, 979,736 bytes, digest `sha256:21acd6482b14a33c419b5828f2a876e43d953efe6a6dfd333859dfa6ef4512d7`.

The verified model now produces one read-only `ResearchDocument` shared with the existing report exporters. It contains recent-minus-earlier, exact UTC date ranges, exact matching rules/version/class/difficulty/balance/challenges/Custom Rules, sample and exclusion counts, uncertainty, both exact query encodings, separate contributor evidence, and all matching cohort rows. Unknown or uncertain metric rows remain visible in cohort-definition evidence but are not contributors and never become zero. Failed comparisons contain the exact rejection and no contributor evidence.

Three new tests retain the exported disclaimer and run identities, reject unavailable conditions without evidence, and distinguish all matching rows from exact contributors. No native screen or persistence entry was added in this checkpoint. No APK was built; TEST313 remains the newest emulator-verified pair in GitHub Actions artifacts and TEST262 remains the newest APK uploaded to GitHub as a release.


## Persistent navigation checkpoint — 2026-10-27T14:16:30.145Z

Exact source `f286ba762387ffdab7a6f5b221686aeb76eca1a1` passed the complete core suite in [run 40519074675](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/40519074675), job `122240815075`: 2,083 methods in 403 suites, zero failures, errors or skips. Retained artifact `12741671374`, 978,858 bytes, digest `sha256:a5fda5e112e900d60b842bb23be07266d1eac600026c4366d2a9d7e9e3a2046c`.

The Research lab now has a durable **Comparable personal progress** entry. It reuses the existing checksummed population definitions without changing their file format: population A is explicitly earlier and B recent. Capture and reopen actions preserve exact ungrouped query, rules and disclosure choices; opening the document is read-only. Win rate and numeric Runs metrics route into the verified fail-closed model and shared evidence/export document.

Three retained regressions prove that stored definitions reopen to the same exact query encodings without modifying their bytes, hidden-information definitions require the current explicit disclosure choice, and missing/non-Runs snapshots fail closed without invented navigation. Initial run40517582101 is retained as a zero-JUnit test-compilation failure caused by a missing existing import. Run40518242628 then executed all 2,083 methods and retained one failure because the successful fixture did not explicitly allow its intentionally internal-capable query. The final test-only correction kept the independent disclosure-denial assertion and passed the complete suite.

This checkpoint changes source navigation but has no APK, API34, pointer, display, restore or physical-device acceptance. It changes no telemetry, gameplay, saves, raw history, score authority, Custom Rules or RNG. TEST314 remains bounded Cleric-blocker accepted, TEST313 remains the broader emulator fallback, and TEST262 remains the newest APK uploaded to GitHub as a release. The executable APK build request is unchanged.
