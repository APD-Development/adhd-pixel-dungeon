# APD derived run-history index direct-log boundary, November 12, 2026 (EST)

## Execution and coordination

This actual regular ChatGPT execution began around **7:35 PM EST** on November 12, 2026, using the owner's connected GitHub repository only. Inspected the live authoritative development ref, current AGENTS.md and history/recovery/archive handoffs, active PRs, recent commits, CI/source receipts, APK release metadata and parallel claims. The authoritative starting ref was **`3d1a0b34b1542044b6f6639c31a2173be31bcdff`**, not main. The existing personal animation/playback PRs #38, #47, #48, wall-search PR #6 and their separate native/release ownership remain out of scope. Published APD463/APD464 and all held APK candidates remain untouched.

Work package: isolated branch **`chatgpt/apd-run-index-direct-log-guard-20261112`**, [draft PR #53](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/53), targeting the established development branch. No conflicting shared branch writes or new app-number claims.

## Evidenced code gap and bounded fix

The previously merged `StatsStore.recoverAll` symlink guard stops destructive recovery from following linked individual log entries, but `RunIndex.refresh` and `RunIndex.rebuild` still used `File.isFile()`, which follows those same links. An external `*.apdlog` file linked into `runs/`, or an alias of another local log, could therefore appear as its own derived history entry. This is a source-demonstrated boundary flaw, not an allegation that owner history was actually exposed or changed.

- Production commit **`2ef14043e0c77aa7b16f09a8a42e7c614bf79c04`** adds the same canonical direct-child and exact canonical-name rule to derived index explicit refresh and directory rebuild. Linked individual logs are refused or ignored; genuine damaged direct-child logs still produce UNREADABLE summaries. Configured symlinked storage roots are allowed. No log bytes, index encoding, gameplay, scoring, RNG, save layout, signer, app ID or edition identity change.
- Test commit **`8b51ac10ab1c68f99cc6347f4f09eb8733bee050`** adds `RunIndexLinkBoundaryTest` with two real-filesystem cases covering external same-named symlink targets, local aliases, normal refresh, corrupt direct-child visibility, missing-index rebuild and valid symlinked storage roots. External source bytes and links must remain unchanged.
- CI workflow commit **`d4fad453ac2216a5ede4716daffc3fb71106dc01`** adds an isolated no-APK full-core and desktop/Android Java source gate, requiring at least 3,328 tests with no failures/errors/skips and existing recovery/refresh compatibility cases.

## Verification status at 7:38 PM EST

GitHub Actions [run 43338572640](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43338572640) started at 7:37:51 PM EST on exact source/test/workflow commit `d4fad453ac2216a5ede4716daffc3fb71106dc01` and was **in progress** at this timestamp. Do not claim that tests passed before a terminal run and JUnit evidence are read. No local Gradle execution, Android native emulator/device acceptance, original-signed APK assembly, or gated game release has occurred in this work.

## Next safe action

Read the terminal source CI result and exact logs, fix evidenced failures if necessary without relaxing tests, read fresh primary and PR heads and conflict state, and use only a guarded non-force integration after verification. Record actual integration, verification and blockers in this continuation. Never overwrite independently claimed APD native/releases or confuse source CI with a qualified new APK. This execution does not imply continuous development between invocations.

## Exact source CI result, 7:41 PM EST November 12

[GitHub Actions run 43338572640](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43338572640) on exact source/test/workflow commit **`d4fad453ac2216a5ede4716daffc3fb71106dc01`**, attempt 1, finished **success**. Decoded job `131600901656` log records `BUILD SUCCESSFUL in 2m 52s` followed by the complete XML gate: **3,328 core tests across 479 suites, zero failures, errors or skipped tests**. The workflow also passed desktop classes and Android debug, release and instrumentation Java compilation. Both new tests and existing canonical-refresh/destructive-recovery compatibility regressions were required by that gate.

JUnit artifact `13998892025`, `apd-run-index-direct-log-junit`, 537,388 bytes, GitHub SHA-256 `dd536808b104a9faeacca1985d17db5e8cbb806c2f8ae88046dc414abcfce680`, was uploaded successfully. This is exact tested source **before** documentary continuation updates. No local Gradle execution, Android emulator/native acceptance, physical-device install-over, original signing, APK assembly, or APK publication was performed.

The isolated PR must still be merged only after a fresh live authoritative-head and candidate-head check. This source CI does not qualify a new signed APK.

## Final guarded source integration and continuation, 7:43 PM EST

Immediately before integration, fresh remote reads showed authoritative development branch `codex/apd-phone-fixes-2026-10-09` at unchanged `3d1a0b34b1542044b6f6639c31a2173be31bcdff`, isolated PR #53 at `3387ea4f1d916fee65a838d5585d3bd8df394a82`, and GitHub `mergeable=true`. PR #53 was marked ready only after completed exact CI, then merged via the GitHub API with **`expected_head_sha=3387ea4f1d916fee65a838d5585d3bd8df394a82`**, normal non-force merge. The API returned **`merged=true`**, merge commit **`07b6a32b7d0d22a56b739ddfcd8e5b944df47ae4`**. Independent readback confirmed primary remote head exactly equals that merge commit and PR #53 is closed and merged with the same SHA. The receipt was checked around 7:43 PM EST.

Exact source-verified commit: `d4fad453ac2216a5ede4716daffc3fb71106dc01`. Subsequent isolated source-neutral documentation commits: `201cce8f41e8b9b0bb5d42e75d009104d8d7d0da`, `11251aad1517ab2ac4ad0327003fa981529ad1bc` and AGENTS index `3387ea4f1d916fee65a838d5585d3bd8df394a82`. Source CI did not rerun after these text-only updates; no gameplay/test/workflow code changed after exact successful CI. PR #53 diff was reviewed as only `RunIndex.java`, new `RunIndexLinkBoundaryTest.java`, new dedicated source workflow, continuation file and AGENTS.md.

All unrelated APD branches, pending animation and wall-search PRs, held native/APK candidates and older builds remained untouched. Separate [APD464](https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/tag/apd-464) was confirmed published earlier at 7:07 PM EST with Developer APK/source assets, but it is **not** a build of this later index correction. No newly assembled/signed APK, original-signer test, emulator/physical-phone acceptance, release or direct download asset is claimed. Future application inclusion needs separately owned complete native/release validation, not just this successful source CI.

**Next safe work:** inspect newly current authoritative head/PR claims before changing code, then take another unclaimed, evidenced, previously approved issue on an isolated branch. Do not redo RunIndex refresh/rebuild symlink protections, alter index or log encoding, or assume physical-device acceptance. Keep this historical CI and merge evidence intact. Development runs only when actually triggered and executed.
