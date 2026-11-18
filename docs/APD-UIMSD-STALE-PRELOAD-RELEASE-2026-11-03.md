# UIMSD stale title-preload release — 2026-11-03

## Outcome

A bounded UIMSD memory-reliability defect is repaired and source-verified at commit `56b0843fd94700c16bce67a4990a9a115725aa82`.

The title-menu warm cache correctly rejected prepared statistics when any raw history metadata changed. Its stale-entry path removed the global cache reference but did not explicitly release the prepared model. A cold replacement could begin immediately, leaving both complete derived catalogs resident until garbage collection. This was confirmed from the current source and the controlled raw-history invalidation path; it is not an attribution for any reported device crash.

## Change

`UimsdWarmCache.takeReady` now:

1. validates freshness and READY state while owning the cache lock;
2. atomically removes the single-owner entry;
3. releases the rejected model outside the cache lock with `releaseForGameplay()`; and
4. returns only the exact fresh READY model, otherwise the unchanged cold-fallback signal.

The existing raw-history invalidation regression now retains the rejected model and verifies both `State.EMPTY` and a null catalog before fallback. Test inventory is unchanged.

## Exact verification

- Workflow: [41710029120](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/41710029120)
- Job: `126175380357`
- Source: `56b0843fd94700c16bce67a4990a9a115725aa82`
- Started: `2026-11-03T20:10:02Z`
- Completed: `2026-11-03T20:13:51Z` (`2026-11-03T15:13:51-05:00`, America/New_York)
- Core: 2,331 tests / 434 suites; 0 failures, 0 errors, 0 skips
- Compilation: Android debug Java, Android release Java, Android instrumentation Java, desktop classes
- Gradle: successful in 2m38s
- Artifact: `13265914408`, 450,032 bytes
- Artifact digest: `sha256:cb10485bf249aaab936106d825c7bb9936f8c01a86b6164144ba8281d71d25a9`

## Preserved behavior and limits

No raw history, save, permanent statistic, view, export, scoring, gameplay, turn, RNG, signing identity, application ID or Custom Dungeon Rule changed. The fix does not alter fresh warm-cache ownership or cold catalog construction.

No APK was assembled, no native/emulator or physical-device test was run, and nothing was published or distributed. The executable build request was not edited. TEST358 remains the latest actual private release; TEST360 remains a newer emulator-accepted candidate but is not an actual release. The separate TEST254 fire report remains open and unconfirmed.
