# UIMSD title-preload supersession — 2026-11-04

## Outcome

A bounded UIMSD concurrency and memory-reliability defect is repaired and source-verified at commit `025fb77f9f2139ddf720b923b4740f573cd29a1e`.

A second optional title prewarm for a different root, ruleset or Home scope previously replaced the shared preparation pointer without stopping the first worker. The retired worker could continue parsing and deriving a complete history alongside the replacement. A current job that failed freshness, loading or timeout checks also left its derived catalog for garbage collection. These are controlled source findings, not attribution for an observed phone crash.

## Change

`UimsdWarmCache.prewarm` now:

1. constructs and publishes each job and worker atomically;
2. removes a narrowly published ready entry if it belongs to the job being superseded;
3. releases the superseded model and interrupts its optional worker;
4. starts a replacement only while that job still owns the preparation generation; and
5. explicitly releases an unsuccessful current preload instead of waiting for garbage collection.

Same-key repeat calls continue sharing the one in-flight job. Exact source stamps, READY checks, model single ownership and the explicit cold-open path are unchanged.

## Regression

The new test holds the cache monitor while starting an initial worker, replaces it with a different root/ruleset/scope, and requires the old thread to be interrupted before either worker can advance. After releasing the monitor it requires the old thread to stop, rejects the first key and claims an exact READY replacement. This raises the complete inventory from 2,331 to 2,332 tests. The exact-count source gate was increased accordingly; no assertion or failure guard was weakened.

## Exact verification

- Workflow: [41854534675](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/41854534675)
- Job: `126659449112`
- Source: `025fb77f9f2139ddf720b923b4740f573cd29a1e`
- Started: `2026-11-04T15:07:55Z`
- Completed: `2026-11-04T15:10:00Z` (`2026-11-04T10:10:00-05:00`, America/New_York)
- Core: 2,332 tests / 434 suites; 0 failures, 0 errors, 0 skips
- Compilation: Android debug Java, Android release Java, Android instrumentation Java, desktop classes
- Gradle: successful in 1m31s
- Artifact: `13337156498`, 445,570 bytes
- Artifact digest: `sha256:2babab1182c4831024b74c69688af2e8d1e66b0c02d73d5f4497a55c40a63bf2`

## Preserved behavior and limits

No raw history, save, permanent statistic, saved view, export, scoring, gameplay, turn, RNG, signing identity, application ID or Custom Dungeon Rule changed. No APK was assembled and no emulator/native or physical-device test was run. Nothing was published or distributed. The executable build request remained unchanged.

TEST358 remains the latest actual private release. TEST360 remains a newer emulator-accepted candidate but is not published. The separate probable TEST254 fire crash remains open and unconfirmed.
