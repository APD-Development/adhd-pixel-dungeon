# APD invalid-run-filename destructive recovery boundary (November 12, 2026, Eastern)

## Isolated claim, 11:28 PM EST

Originating regular ChatGPT APD development, not a separate Codex task. Inspected the authoritative `codex/apd-phone-fixes-2026-10-09` at `7ee78e1af25a4e181f92c48755ad416fc5052dfe`, AGENTS.md, current run-index identity/filename and destructive recovery handoffs, latest merged PRs #74–#77, open PRs and work claims, current source CI, and existing APK/native release constraints. This isolated branch is `chatgpt/apd-recovery-invalid-run-id-20261112-2328`. No active PR claims the exact recovery-of-invalid-filenames issue. Do not interfere with separately active native/release, playback, GPL, wall-search or other work packages.

### Evidenced defect and bounded correction

`RunIndex.rebuild()` now deliberately skips files with invalid APD run-ID stems, but `StatsStore.recoverAll()` still selects any canonical direct regular file ending `.apdlog`, even if its stem violates `StatsStore.isValidRunId`. For a parseable damaged log copied under a foreign or invalid filename, `LogRecovery.repair()` backs up and truncates it even though it is not recognized as an APD run by the derived index. Keep the existing valid-ID, canonical-direct-child and symlinked-root rules aligned; refuse destructive repair of invalid-name files. Preserve true valid-ID damaged log recovery, its tail backup, all original history, score/save format, signing, app IDs, editions, releases and gameplay. This is a source-evidenced risk, not a claimed user-phone incident.

### Verification claim at start

No files have been modified other than this claim. No tests, APK assembly, native Android tests, original-signer checks or publication have been performed. Plan: add exact production guard, real-filesystem regression tests, and scoped full-core/desktop/Android Java source CI; review terminal evidence before any integration; use fresh live expected-head non-force update and durable result here. Existing APD464 is an independently published prior build and must not be relabeled or overwritten. No automations may be changed.


## Exact source CI, Thursday November 12, 2026, 11:31 PM EST

Production fix commit `d728566242e956a24a7df100dbf584fe66724a99`; two real-filesystem regressions `1b0f9ccd6ac85b6cf086eace9eb3250d026c3fca`; workflow and exact executable tested head `00abcfc0598c62fc8f674ea463ed4a1f4015a4b6`. [Actions run 43365752452](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43365752452), attempt 1, job `131689641947`, completed **success**. Raw job logs show `BUILD SUCCESSFUL in 2m 12s` for complete `:core:test`, `:desktop:classes`, Android debug/release Java and debug AndroidTest Java compilation. Terminal JUnit XML gate reported **3,373 tests across 492 suites, zero failures/errors/skips**, including both new invalid-name recovery cases and existing symlink, valid damaged-log recovery, filename identity and index cache compatibility regressions. JUnit artifact `14009664805` (`apd-recovery-invalid-run-id-junit`, 545,002 bytes) uploaded successfully. The post-CI documentation commit is **not** the test-executed source head.

Source-only gate: no assembled APK, original signer, Android emulator instrumentation, physical-phone test, install-over/save integrity, manual screenshots, or publication in this execution. Previously published APD464 is unchanged and predates this source fix. Dedicated source CI does not qualify a release.

After CI, fresh readback found authoritative base `7ee78e1af25a4e181f92c48755ad416fc5052dfe`, feature at exact tested head `00abcfc0598c62fc8f674ea463ed4a1f4015a4b6`, and only independent playback, search, GPL or gameplay PRs open. PR #78 was clean/mergeable. Next: record this document, verify a fresh exact base/head and use expected-head non-force integration; inspect resulting merge and leave a terminal receipt. If base changes, reconcile rather than overwrite.


## Final guarded integration, Thursday November 12, 2026, 11:32 PM EST

Terminal exact-source [run 43365752452](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43365752452) and job `131689641947` remain **completed/success** for executable head `00abcfc0598c62fc8f674ea463ed4a1f4015a4b6`: **3,373 tests / 492 suites; zero failures/errors/skips**, plus desktop/Android debug/release/instrumentation Java compilation. JUnit artifact `14009664805` uploaded. Later `d7d1f1f3aa8215df93188ab39cde0ff4e882856b` changed this handoff document only; no executable changes after verified source.

Before integration the live authoritative head was independently `7ee78e1af25a4e181f92c48755ad416fc5052dfe`; PR #78 head was `d7d1f1f3aa8215df93188ab39cde0ff4e882856b`, mergeable without conflicts, with only four scoped changed files and other independent claims untouched. A two-parent non-force merge `3a9cdb43ab90d5c83cf14b884ed988984e0505dd` was built from that exact feature tree and original authoritative head. The remote update used `expected_sha=7ee78e1af25a4e181f92c48755ad416fc5052dfe`, `force=false`, and **succeeded**. Independent readback confirmed authoritative `codex/apd-phone-fixes-2026-10-09` at `3a9cdb43ab90d5c83cf14b884ed988984e0505dd`, [PR #78](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/78) **merged/closed**, and actual production `StatsStore.recoverAll` containing valid-ID and direct-child guards. No main write, force push, other branch overwrite or task automation changes.

**Release status:** This execution created **zero** assembled APKs, original-signed binaries, native-device acceptance reports or releases. Previous [APD464](https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/tag/apd-464) was independently confirmed published with Developer APK and source on November 12, 7:07:51 PM EST, and is unchanged; it does **not** include this new source safeguard. Qualified future APK delivery remains blocked until the separately coordinated original-signer build, native Android acceptance, hash/source, install-over save/history and established publisher gates complete. Source CI is not a substitute.

**Next safe work:** Preserve this integrated repair and the original history; do not redo it. Re-inspect all live branch/PR/work claims to select another user-approved, evidenced and unclaimed fix, or coordinate a separate authorized APK candidate using the complete original signing/native/release procedure. This regular-chat execution ends after this durable final documentation merge. No coding between executions is claimed.
