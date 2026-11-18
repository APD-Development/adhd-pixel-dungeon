# APD run-index filename boundary, November 12, 2026 (Eastern)

## Isolated claim, Thursday November 12, 2026, approximately 11:00 PM EST

Regular ChatGPT APD continuation, not a separate Codex task. Starting authoritative branch `codex/apd-phone-fixes-2026-10-09` at `9fa38c96de89c9074d8801134c65bf0955bc986d`, after checking AGENTS.md, archive/history handoffs, recent merged PRs #70-#73, active draft playback PRs, open claims, existing source CI workflows and gated release evidence. This work is isolated on `chatgpt/apd-index-invalid-log-filename-20261112-2300`; do not overlap or merge into separate playback/native/release branches. Previous release APD464 and all earlier builds remain unchanged.

### Verified source gap

`RunIndex.rebuild()` scans direct `.apdlog` files but does not require a valid APD run-ID stem. Invalid names can be displayed as fabricated unreadable entries, or their valid copied log headers can masquerade as actual runs. `RunIndex.readFrom()` likewise previously trusted a checksum-valid old cache entry naming an invalid log. Strict `StatsStore.isValidRunId` already defines the valid on-disk identity and is used by `RunIndex.refresh`.

### Bounded correction

Require valid direct `runs/<id>.apdlog` filenames during rebuild and when decoding cached entries. A bad cached name causes the derived index to rebuild from immutable original logs. Preserve genuine damaged log files with valid names as visible UNREADABLE records, preserve symlink-root support and existing link boundaries. Do not change game mechanics, score, RNG, seeds, archive formats, history bytes, save compatibility, signing, application IDs, editions or prior builds.

Four regression tests cover invalid names and raw bytes, a valid-header alias under an invalid filename, a checksum-valid outdated derived cache, and legitimate damaged-log visibility. The isolated workflow runs full core JUnit, desktop Java and Android debug/release/instrumentation Java compilation. Any pending or failed CI remains distinct from a verified pass.

### At creation

- Baseline: `9fa38c96de89c9074d8801134c65bf0955bc986d`
- CI: pending until GitHub Actions finishes; no results claimed yet
- APK: no newly assembled/signed/native-accepted/published APK claimed
- Next: inspect exact CI results, only integrate after terminal success and fresh non-force expected-head check, then record exact receipts and safe work


## Terminal verified source and integration, Thursday November 12, 2026, 11:08 PM EST

Source/test/workflow/initial handoff commit: `574a7cf2a856df695618ae110e11f5d7e2985f1f`, based on authoritative head `9fa38c96de89c9074d8801134c65bf0955bc986d`. [Exact GitHub Actions run 43363065127](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43363065127) completed successfully, attempt 1. Job `131680880032` has terminal conclusion **success**. Its raw job log records **3,368 core tests across 490 suites**, with **0 failures, 0 errors and 0 skipped**. The checked-out exact commit passed the source/fixture guard, full `:core:test`, `:desktop:classes`, Android `compileDebugJavaWithJavac`, `compileReleaseJavaWithJavac` and `compileDebugAndroidTestJavaWithJavac`, the JUnit report gate (including four new regressions and four pre-existing index/recovery controls) and JUnit artifact upload. The Gradle log says `BUILD SUCCESSFUL in 2m 12s`. This is **source-level and Java compilation verification only**, not native emulator, physical phone, signing, two-APK assembly or release acceptance.

[PR #74](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/74) was marked ready after terminal success, then integrated into authoritative `codex/apd-phone-fixes-2026-10-09` via a real two-parent merge commit `c1ecb39ba49668e163f5343e98a365d599df072e`. The live remote head was read as `9fa38c96de89c9074d8801134c65bf0955bc986d` immediately before the update; the non-force ref update required `expected_sha` equal to that value and succeeded. Independent GitHub PR and branch readbacks both confirmed the merge commit and PR #74 merged/closed. Only `RunIndex.java`, the new four-test class, isolated source-gate workflow and this claim handoff were included in the source PR.

No new APK, private original signer, native Android case, physical-device case, save install-over gate, or publication was attempted or passed in this execution. GitHub release API separately confirmed previously published `apd-464` (published November 12, 2026 at 7:07:51 PM EST) with its Developer APK and matching source; it does **not** include this newer fix and remains unchanged. No gameplay mechanics, balance, replay scoring, player logs/history bytes, external resources, unrelated work packages, or recurring APD automation schedules were modified.

Next safe work: treat `c1ecb39ba49668e163f5343e98a365d599df072e` as the new authoritative source start until superseded; re-check concurrent work claims, release gates and current head before editing. Qualify a future owner-authorized APK only on the exact assembled original-signer pair through existing native and publication gates, without treating this source CI as device acceptance. Do not duplicate this completed filename-boundary work.
