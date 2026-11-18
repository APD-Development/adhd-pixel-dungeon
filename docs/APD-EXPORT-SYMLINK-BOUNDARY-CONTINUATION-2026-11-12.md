# APD archive-export external symlink boundary, November 12, 2026 (Eastern)

## Actual execution and coordination

Started about 7:00 PM EST, November 12, 2026. Queried the connected GitHub repository, not main or any upstream source. The authoritative `codex/apd-phone-fixes-2026-10-09` branch head was **`bf9b86c697d15237a868c4958473eda1043cae42`** when this independent work began; `main` was `8228fbcb030b3b0a2a787b6e3ea1cb8593e3e13d`. Read root AGENTS.md, the completed-work ledger, known issues, current-state handoff, archive import/recovery symlink receipts and the live open PR list. APD463 is published from a separate frozen pipeline. Independent APD464 personal playback and sprite-animation claims are owned by other work; this source-only package does not modify their APK sources, work branches, signing, native evidence, release procedures or assets. Existing archive *import* and destructive *recovery* symlink guards were already completed and are not repeated.

## Identified existing reliability/privacy gap

`ArchiveExporter.collect` used `File.listFiles` and `File.isFile`, both of which follow symbolic links. A linked `runs/` or `legacy/` folder, or a plain-ID `*.apdlog` symlink inside either, could cause export to read files outside the requested canonical history folder. Import and destructive recovery already had independent canonical-path protections; exporter did not. This is a source-level exposure path, not evidence that an owner's own history was leaked.

## Isolated code, tests and claims

- Branch: `chatgpt/apd-export-symlink-boundary-20261112-1905`, forked from exact authoritative `bf9b86c...`, no shared branch writes.
- Production commit **`9420235648a5902c5621a0bf3713835629c932a1`** adds canonical direct-child checks before traversing `runs/`/`legacy/`, before reading each eligible log, and again when the archive payload is measured and written. A malformed alias fails the export with an explanation rather than producing a misleading partial backup. Legitimate symlink access to the supplied root itself remains supported. No format, redaction, save, mechanics, scoring, balance, application ID, signature or asset changes.
- Test commit **`4692c2436da5092f827a941b1ebdc8378840a880`** adds `ArchiveExporterSymlinkTest`: linked external run/legacy files, linked external run/legacy directories, and normal export through a canonical root alias. Tests insist a refused export has written zero bytes, external and genuine history remain unchanged, and a normal export still works.
- CI workflow commit **`6f28fabf027ad4d58429fbe5fdf4e2699b1a9fe7`** adds a dedicated source gate (full `:core:test`, desktop classes, Android debug/release/instrumentation Java compilation, JUnit XML totals, existing archive duplicate first-wins and output-stream ownership regressions). The workflow has GitHub Actions read-only contents permission and builds no APK.
- [Draft PR #49](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/49) targets the authoritative development branch, **not main**. It must not be merged without actual CI verification and a fresh live-head check.

## Exact successful CI and verification, November 12, 7:06 PM EST

[GitHub Actions run 43334098035](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43334098035) completed **success**, attempt 1, on exact product/test/workflow commit `6f28fabf027ad4d58429fbe5fdf4e2699b1a9fe7`, from a 7:04 PM EST start. The decoded actual job log for job `131585774528` records `BUILD SUCCESSFUL in 1m 44s`, **3,323 core tests across 477 suites, zero failures, zero errors, zero skipped**. The full core suite, desktop classes and Android debug/release/instrumentation Java compilation passed. The JUnit XML acceptance gate passed all three new real-filesystem cases, existing duplicate-manifest first-wins compatibility and exporter stream ownership. XML artifact `13996701951`, 531,483 bytes, `sha256:5dc13c521c2921bbfa74ab73ee4d010c1ff3d357ce8232e295ce4b1f3f608807`.

No local tests ran, because this execution's container cannot resolve github.com to clone the repository; tests ran on GitHub-hosted Actions only. This is **source-only**: no application or instrumentation APK was assembled or signed here, no native emulator or physical-phone acceptance occurred, and no new release was published. APD463 remains an independent already-published qualified APK pair. Its owner and the separately running APD464 personal playback workflow must not be disturbed.

At the source-verification read, authoritative development remained `bf9b86c697d15237a868c4958473eda1043cae42`; raw GitHub PR metadata independently reported draft PR #49 as `mergeable=true`, `mergeable_state=clean`, with the same base SHA. The passing CI precedes documentation-only handoff updates; these updates do not alter production code, test code or workflow. Merge receipt, if any, must record actual GitHub response and current heads, never infer a merge.

## Next safe work

Inspect the latest live authoritative and PR heads; if unchanged and conflict-free, mark #49 ready and use an expected-PR-head non-force merge. Otherwise leave the verified draft intact. Document any actual integration in a follow-up receipt. Preserve all earlier source and release receipts, previous signed APKs, original saved history, and unrelated native/playback work.


## Final verified integration receipt, November 12, 2026, 7:09 PM EST

The exact-source CI run [43334098035](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43334098035) succeeded before integration. Source files and workflow were unchanged after the exact tested SHA `6f28fabf027ad4d58429fbe5fdf4e2699b1a9fe7`; only the handoff text was updated in `6bbb46f59c0cc72b2398e80e9744975dbd5449c5` and `154e98c36ddb62c966f766f3cf378675553f023c`.

The primary authoritative branch was re-read immediately before merge at `bf9b86c697d15237a868c4958473eda1043cae42`. GitHub confirmed [PR #49](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/49) was non-draft, `mergeable=true`, `mergeable_state=clean`, with exact expected head `154e98c36ddb62c966f766f3cf378675553f023c`. The non-force PR merge with `expected_head_sha` succeeded, reporting **merge commit `795e099f0054a440e11f33321263820781e57c6f`**. A subsequent independent readback confirmed the authoritative branch head and PR merge SHA both equal `795e099...`, and PR #49 is merged.

Completed during this execution: one bounded archive export canonical-path fix, three real-filesystem regressions, one isolated CI gate, source handoff and guarded integration. Final verified source test result: **3,323 tests in 477 suites, zero failures/errors/skips**, desktop and Android debug/release/instrumentation Java compilation. No application or instrumentation APK was assembled; no native emulator, real phone or original-signer acceptance was run, and no APK was published. The original APD463 signed release remains intact as verified after merge; the concurrently owned APD464 personal playback build remains outside this package. No change to original saves, logs, game mechanics, scoring, signing identities, app IDs, edition boundaries or old release assets.

This additional documentation-only receipt commit is not part of the earlier exact CI and cannot be called an extra tested source change. Next source work must read the *then-live* authoritative head, AGENTS, current claims and PRs instead of repeating this completed exporter fix. Future release aggregation must follow native signer/save and publication gates on a single separately owned qualified frozen pair.
