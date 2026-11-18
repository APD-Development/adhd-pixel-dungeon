# APD derived run-index refresh path boundary, November 12, 2026 (EST)

## Execution and coordination

Run began at **3:15:35 PM EST**. Checked the private repository, current branch `codex/apd-phone-fixes-2026-10-09`, `AGENTS.md`, older handoffs and completed ledger, open PRs, recent merges, source gates and APD releases before editing. The observed authoritative head advanced during reconnaissance from `6dcb0ad0d797ee2519621f44922637dd817554ac` to **`0ca26dd72b7b8151847ad02c3557e4ebe83d945c`**, which already merged the independently owned report-provider save fix. This work branched from the **newer** head, never from `main`, and did not write to shared branches while source changes were unverified.

The isolated work package is `chatgpt/apd-index-refresh-path-20261112-1515`, [draft PR #37](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/37). Distinct pending APD461, hero fade, well, native acceptance and release work claims were left untouched. A separate parallel execution published [APD462](https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/tag/apd-462) at 3:20:21 PM EST; it is not this change and does not contain this source patch.

## Concrete source finding and bounded implementation

`RunIndex.refresh(String runId)` previously directly constructed `new File(runsDir, runId + ".apdlog")` without validating its caller-provided run ID. A path-shaped `../outside` would open a genuine log stored outside `runs/` and could incorporate its summary into the **derived** run index. Existing `StatsStore.isValidRunId` already defines the approved portable run ID contract (letters, numbers, dash and underscore). This was an evidenced source-level filesystem boundary issue, not a claim of corruption on the owner's device.

Changes on isolated branch:
- `4c5ab478af0e149180aafdf5061636a43ab9481d`: refuse invalid refresh IDs via the existing validator **before any file access**, with `IOException` consistent with the existing refresh signature.
- `f077fa9ef4df6de459d351e5f414b1141bccf094`: add real-filesystem regression `refreshRejectsPathLikeRunIdsWithoutReadingOutsideHistory` with genuine valid log bytes outside `runs/`; test traversal, nested slash, empty and null IDs, no external byte changes, no spurious entries and a valid-refresh control.
- `f2ba3a058e7a2d47173105c157217220a3e1c3e3`: isolated full-source workflow, no APK build or release action.

No changes to valid run IDs, gameplay, scoring, RNG, log/index encoding, stored history, original signer, package ID, edition identities or APKs. This does **not** claim to harden every possible filesystem entry point or to validate the physical device.

## Exact verification and limitations

[GitHub Actions run 43299513282](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43299513282), attempt 1, at source/test/workflow commit `f2ba3a058e7a2d47173105c157217220a3e1c3e3` completed successfully at approximately **3:24:42 PM EST**:
- All **3,310 core tests across 475 suites** passed, with zero failures, errors and skips, including the new external-file regression and existing valid `refreshTracksAChangingRun` control.
- Desktop classes and Android debug, release and instrumentation **Java compilation** completed successfully.
- JUnit XML uploaded as artifact `13979979551` (SHA-256 `f033b6ec8bc792ebeb85e01a477c0f79dca32c8465b3c9765a3359e0632d11e1`). Earlier code-only checks and this exact CI must not be confused with a qualified native Android build.

**No application APK was assembled, signed, device-tested or published by this execution.** Existing APD462 and older qualified releases remain untouched. Any later code/head change needs its own fresh verification before a game release.

## Safe continuation

The isolated change is source-verified. Before integrating, freshly recheck both the authoritative branch head and PR head, inspect newly merged competing work, and use a non-force expected-head guarded update. If the base changed, reconcile without overwriting another session. After integration, record its exact merge/ref result here or in a follow-up receipt and retain the CI evidence. No recurring work is implied between executions.

## Final guarded integration receipt, 3:27 PM EST

Freshly rechecked authoritative head `0ca26dd72b7b8151847ad02c3557e4ebe83d945c`, exact successful source workflow head `f2ba3a058e7a2d47173105c157217220a3e1c3e3`, isolated feature head `c6b78fd9adcf8a696eedc9c98c3e8e9802ac3aa6` and the complete five-path diff. The later two feature commits only add this continuation and index it in `AGENTS.md`; no application/test/workflow code changed after the successful source gate.

Created two-parent [merge commit `1b388645`](https://github.com/APD-Development/ShieldingShouldHaveWorked/commit/1b388645f51dcd90f3fdd7a752f86632d689ed12) and advanced authoritative `codex/apd-phone-fixes-2026-10-09` **without force**, with `expected_sha=0ca26dd72b7b8151847ad02c3557e4ebe83d945c`; GitHub reported success. Re-read remote head and both changed production/test files; the run-ID guard and this record were present. [PR #37](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/37) was confirmed **closed and merged** with the exact merge SHA. All other concurrent work and APKs remained intact. The next execution must read the *live* branch again; neither this source gate nor APD462's separate release includes native acceptance for this new index change.
