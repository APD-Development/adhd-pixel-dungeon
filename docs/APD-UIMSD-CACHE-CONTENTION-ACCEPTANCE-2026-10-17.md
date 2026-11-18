# UIMSD Optional-Cache Cross-Instance Acceptance — 2026-10-17

## Outcome

The remaining optional-cache cross-instance investigation candidate is closed without a production change.

- Test source: `759d8ef3cc55e2504fc902d71f6242876f3dc0f9`
- Core-only request head: `49d21a1bf83cb33a4ab4cd7bb1573eae874ea9c7`
- Workflow: `38991720872`
- Started: 2026-10-17T01:38:28Z
- Completed: 2026-10-17T01:41:04Z
- Result: success
- Complete core suite: 1,763 tests in 348 suites; 0 failures, 0 errors, 0 skips
- New regression: 2 tests; 0 failures, 0 errors, 0 skips; 0.093 seconds
- Artifact: `12085507234`, 826,001 bytes
- Artifact digest: `sha256:3213dcc47b5cfea050807bc3e4b4cebd5ec16ad7fe67a542494011099e391d3d`

## Accepted behavior

Both current optional cache writers use `File.createTempFile` in the destination directory, giving each writer its own temporary file. The new end-to-end regression verifies behavior above that implementation detail:

1. Four independent `DerivedStore` objects simultaneously rebuild one history 12 times each.
2. Every returned table contains the exact raw run and score.
3. A fresh `DerivedStore` reads the surviving cache rather than rebuilding.
4. Four independent `DatasetStore` objects simultaneously delete/rebuild the per-log cache 12 times each.
5. Every returned catalog contains the exact run row.
6. A fresh `DatasetStore` reuses one valid per-log cache with zero rebuilds.

That is 96 contested full-store operations. The existing deterministic `RowCacheConcurrentWriterTest` remains in place to test low-level destination replacement ordering and temporary-file cleanup; the new test adds real raw-log parsing, complete store instances, returned-data checks and final cache reuse.

## Boundaries

This is test-only acceptance. It does not change cache formats, raw logs, saves, permanent statistics, active v2.6R scoring, gameplay, RNG, signing identity, application ID or any of the 18 Custom Dungeon Rules. The executable APK build request was not edited. No APK was built, staged, published or distributed, and no emulator result is presented as physical-phone evidence.

TEST263 remains the verified and staged candidate. TEST262 remains the latest actually published owner APK.

## Remaining work and next safe step

The broader UIMSD audit is still incomplete. Later per-file review records have not yet been reconciled into one current-blob inventory, and game integration callers remain a separate coverage surface. The next safe bounded package is to merge those later review records by exact current blob SHA, select the first genuinely unreviewed changed component, and review it together with its integration callers. Completed cache, archive, reader and APD Settings acceptance must not be restarted.
