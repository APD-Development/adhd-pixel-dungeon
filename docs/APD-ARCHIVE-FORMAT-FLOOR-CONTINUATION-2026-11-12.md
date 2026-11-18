# APD archive manifest nonpositive-format boundary, November 12, 2026 (Eastern)

## Work claim and source
Started at approximately 7:21 PM EST on November 12, 2026. Independent source-only work branch `chatgpt/apd-archive-format-floor-20261112-1921` begins from authoritative development commit `d6a737b4e82e5740f7559a63f1a491e29f5be298`. This is not a main-branch, playback, wall-search, APK, native-acceptance, signing, or release work package. Prior manifest-size overflow and duplicate-manifest-path compatibility investigations remain complete and untouched.

The owner's ArchiveManifest.FORMAT is 1 and the parser rejects newer formats, but a checksum-valid `APD-ARCHIVE\t0` or negative format currently passes that check. The source contract supports format 1, not invented pre-version formats. An independent regression will prove rejection before the importer writes files, while ordinary current-format manifest round trips and unknown future metadata-line compatibility remain unchanged.

## Status
Work claimed, implementation and source CI not yet run. No APK built, signed, accepted or published. Do not infer device or native acceptance from a source test.

## Next steps
Add narrowly scoped parser validation and tests; run isolated source-only CI with full core tests and required desktop/Android Java compilation; inspect terminal results before any integration. Read authoritative head and active claims again before writes or merge; preserve all accepted and blocked application builds and the original historical files. Record exact source commits, tests and limitations here after verification.

## Verified isolated source result, 7:25 PM EST

- Production fix: `52459d17f131a23ef1dd3f19dcc0d48a703db923`. Parser now rejects nonpositive versions with an explicit error, while retaining the existing newer-version error and version-1 behavior.
- New real-filesystem regression: `be82503f2ac890bdc94cc59a2ee4947ca121f178`, `ArchiveManifestFormatFloorTest`, three test methods.
- Dedicated source gate: `2d06da0dafd53a8e7d80d97f5ed4817955ba1002`. GitHub Actions [run 43336521775](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43336521775), attempt 1, **completed SUCCESS** at 7:25:25 PM EST. Exact job `131593969644` log reports `BUILD SUCCESSFUL in 2m 35s`, **3,326 JUnit core tests across 478 suites, zero failures, errors, or skipped**, and successful desktop, Android debug, Android release and instrumentation Java compilation. JUnit acceptance checked all three new cases plus pre-existing manifest round-trip, newer-format/unknown-line compatibility and repeated-path first-wins compatibility. Artifact `13998605081`, size 534477 bytes, SHA-256 `9720360461c44558b6a24512fec87919b8b1ed44fdae54e77689eb6bc17fbdda`.
- Draft [PR #51](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/51) targets the authoritative development branch. This documentation-only update follows the tested source, with no changes to game/test/workflow code. Recheck live PR/base heads before deciding on merge.
- Exact run: no local Gradle execution, because this runtime does not contain the private repository or original Android signing secrets; verification ran on GitHub-hosted Actions. No new APK assembled, signed, native/emulator tested or published. Existing APD462, APD463, separate APD464 and all held candidates remain unchanged. Passing source CI cannot substitute for original-signer/native/manual gates.

Next safe work: if PR #51 is merged cleanly after a fresh lease check, record the resulting authoritative merge SHA. Do not rebuild unchanged APKs, invent a new application number or interfere with separate personal playback/native owner claims.

## Final source integration and handoff, 7:26 PM EST November 12, 2026

Immediately before the merge, fresh GitHub reads showed authoritative `codex/apd-phone-fixes-2026-10-09` at `d6a737b4e82e5740f7559a63f1a491e29f5be298`, [PR #51](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/51) at head `afb8259e5ca78c1f8e3bf38611452955c98c4d35`, identical base, and `mergeable=true / clean`. GitHub merge used the PR's exact expected head, without a force update. API confirmed a successful merge and [authoritative two-parent commit `4240e361c46e3c020f9f4d7ce4f20d49c45d7ae8`](https://github.com/APD-Development/ShieldingShouldHaveWorked/commit/4240e361c46e3c020f9f4d7ce4f20d49c45d7ae8) at 7:26:47 PM EST. Post-merge reads independently confirmed the branch ref is precisely that SHA, PR #51 is closed and merged, and the production parser has the new nonpositive-format guard.

Start authoritative commit: `d6a737b4e82e5740f7559a63f1a491e29f5be298`. Production change commit: `52459d17f131a23ef1dd3f19dcc0d48a703db923`. Source-tested commit: `2d06da0dafd53a8e7d80d97f5ed4817955ba1002`. Tested and successful Actions run: `43336521775` (3,326 core / 478 suites, 0 failures/errors/skips, desktop/Android debug/release/instrumentation Java compile). Integrated branch commit: `4240e361c46e3c020f9f4d7ce4f20d49c45d7ae8`. Documentation-only head after source test: `afb8259e5ca78c1f8e3bf38611452955c98c4d35`. The final receipt itself is documentation-only and is not a claim that CI reran after its creation.

No gameplay, save, history serialization, difficulty, scoring, signing, application identity, edition boundary, or release workflow mechanics changed. No APK was built or published in this package. The current execution cannot independently supply the private signing identity, verified device/native acceptance or manual image review. Keep this source change staged for a future gated successor APK without rerunning an unapproved native/signing/publication procedure. Preserve all prior accepted APD releases, held candidates and independently ongoing APD playback packages. This run does not imply continuous coding between executions.
