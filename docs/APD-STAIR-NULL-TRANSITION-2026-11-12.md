# APD source continuation: null stair transition safety (November 12, 2026)

This is completed and verified **source-only work**, not a released APK. Check live GitHub branch, PRs, Actions and releases before assigning another APD build number.

## Exact implementation and verification

- Baseline: post-APD460 source head `9f31ba8a1c792f5d4c729b2f5bd7d0e65199b732`, which already includes the merged Foresight scan cache (PR #7).
- Isolated branch: `codex/apd-stair-null-transition-20261112`. It did not modify APD461's parallel branch `codex/apd-search-invalid-terrain-20261112` or its frozen candidate.
- Production fix `4af1306ed9438edf9a7f9f6969d8843b5caab8d5`: `StairNavigation.discoveredTarget` checks `matchesRoute(transition, route)` before dereferencing `transition.cell()`. The existing matching helper already rejects null. This prevents null entries in partially restored/damaged transition lists from crashing the read-only stair destination shortcut.
- JUnit regression `ee665419fe8b92f387faf41817aed84179a9f027`: null-only lists produce no fabricated stair target and no turn/action; null entries before and after valid stairs preserve correct up/down discovery, mapping and exploration visibility.
- CI workflow commit `4dc5c497a86eb3fccbabae6abd5d6e057c0cac72`; run [43244355419](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43244355419) **completed successfully**. Full raw core result: **3,269 tests across 470 suites, zero failures, errors or skips**, including both new regressions. Desktop plus Android debug, release and instrumentation Java compilation passed. The original test-result XML artifact is retained with the run.
- PR [#8](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/8) merged into established development branch at `790bdb7d2e919d27be3479e73664451433f2c5cf`. This commit was re-observed as the active branch head after merge.

## Scope and next safe work

The change does not add routes, relax mapped/visited checks, inspect hidden terrain, change safe-step/trap logic, perform actions or spend turns, or alter balance, scoring, RNG, save schemas, compiled-APK history, application identity or signing. No new Android game APK was assembled or delivered in this session.

At final observation APD460 remained the latest confirmed published release. APD461 malformed-observed-terrain fix (draft PR #6) had a successful source and candidate build but native/publisher workflows 43240714587 and 43240803227 were still in progress; `apd-461` was not published. Do **not** assume their completion or overwrite their candidate. Once APD461 is verified, released and appropriately integrated, any successor signed candidate needs its own independent source, Android native, artifact/signature/hash and publication verification. Preserve APD460 and earlier assets unchanged.
