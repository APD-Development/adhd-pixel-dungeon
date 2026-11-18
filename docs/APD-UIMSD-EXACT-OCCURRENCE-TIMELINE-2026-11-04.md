# UIMSD exact-occurrence timeline evidence bridge — source verified 2026-11-04

## Result

Exact accepted source head `b5cd56716b2363a011f7e05876178be969fe9e44` (production implementation commit `dd112ee122fc21657b7583327214341d5df59494`) adds a bounded read-only bridge from an eligible UIMSD occurrence row to the existing canonical public run timeline at that occurrence's exact recorded turn.

The row details sheet now offers **Run history at this turn** only when all authority checks pass. Ordinary event rows use their canonical event turn; floor visits use the exact entry turn; equipment rows use the exact put-on turn; Deaths use the exact death turn; registered fact rows use their exact fact turn. The matching Runs row must be unique, recorded, public, non-imported and non-duplicate. Hidden, uncertain, stale, imported, duplicate, non-occurrence, missing-run and ambiguous-run inputs fail closed. The bridge does not create an event, reinterpret a filter, infer an older turn or widen disclosure.

## Permanent regression coverage

Two new `RunTurnBrowserTest` methods cover:

- exact run identity and exact turn opening for an attack, equipment start, floor entry and death;
- rejection of uncertain clocks, hidden/imported rows, stale selections, missing run ownership and ambiguous recorded-run ownership.

The complete guarded inventory increased from 2,332 to **2,334** tests. No guard or prior test was removed or weakened.

## Verification

Source-only run [41904846210](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/41904846210), job `126836627376`, completed at `2026-11-04T20:18:20Z` (15:18:20 America/New_York):

- 2,334 tests in 434 suites;
- 0 failures, 0 errors, 0 skips;
- Android debug Java, Android release Java and Android instrumentation Java compiled;
- desktop classes compiled;
- build successful in 2m32s;
- artifact `13358689154`, 450,037 bytes, `sha256:4b360612d8c68c5a57b486f1a1bf3da460a99ed16bf4759d6502e251736f8cec`.

The first run [41904051619](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/41904051619), job `126833813884`, is retained as failed evidence. It executed all 2,334 tests; only the two new tests failed because their synthetic selected rows were unfinished with unknown rules while the fixture query excluded both categories. Production compiled. The fixture scope was corrected without changing production. Its artifact is `13359956874`, 451,060 bytes, `sha256:003541d6b8673b99d1139b1b52ff0793d3452d6ed6c47ebc2140507b6ce0cfdb`.

## Boundaries and next step

No APK was assembled, installed, published or distributed. No emulator/native UI or physical-device claim is made. Raw history, saves, permanent statistics, all18 Custom Rules, active v2.6R lookup/scoring, gameplay, turns, RNG, signing key/application ID and existing UIMSD calculations are unchanged. `docs/APD-CODEX-BUILD-REQUEST.txt` remains blob `7f826f03c4000b3b191f22df0251d3db827eddf4`.

TEST358 remains the latest actual private release/fallback. TEST360 remains the newer emulator-accepted candidate and is not a release. Keep the TEST254 fire report open unless reproduced on a current build.

Next safe step: retain this bridge for the next intentional application candidate and include a real pointer-driven occurrence-row/open-timeline/scene-restoration check there. Continue the still-partial broader correlated-window and experiment work and representative Android history storage/latency profiling as separate bounded packages; do not rebuild or publish solely for this source-only evidence.
