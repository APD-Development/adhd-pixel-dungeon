# APD exporter filename/header identity continuation, Friday November 13, 2026 (Eastern)

## Isolated work claim, approximately 02:03 EST

Originating regular ChatGPT APD development thread. Verified authoritative branch `codex/apd-phone-fixes-2026-10-09` at `97b66dbee095fbd33cd418a8e16b3f58bccd7712`. This claim's isolated branch is `chatgpt/apd-export-header-identity-20261113-0203` at that exact base. Reviewed root AGENTS.md, completed-work ledger, known issues, preceding archive/import/privacy continuation records, actual source and real-writer tests, relevant branches, open PRs, CI and releases. Existing open playback/hero-selection/native/release/GPL branches remain exclusively owned by their separate work packages, untouched here.

**Evidenced defect:** `ArchiveExporter.collect` treats a parseable runs/<id>.apdlog as readable when `RunLogReader.parse` yields any header, even if the embedded `LogHeader.runId` is a different per-file ID. A same-directory copy renamed to `runs/alias.apdlog` can therefore enter a checksummed exported ZIP as `alias`, although the newly integrated `ArchiveImporter.stage` rejects it because the raw header identifies its original filename. Export and import disagree about a valid archive; misattributed raw history must not be packaged as a different log. This is distinct from the completed import-side check in PR #88 and from RunIndex/recovery boundaries.

**Bounded repair:** In the exporter only, consider a run log readable when its valid embedded header's individual log ID matches the filename stem. Do not apply this rule to imported legacy record files, whose historical identity handling must remain compatible. Preserve distinct per-log file IDs and shared logical typed RUN_START IDs across multi-segment runs. Skip mismatched copied run files as unreadable without mutating them; continue exporting independent valid logs, preserving manifest validity, exact source bytes and zip importability.

Add real-filesystem/real-writer/real-ZIP regressions for a mismatched local run alias excluded from the archive, a normal independently valid run retained and importable, valid distinct segment filename IDs sharing a typed logical run ID, and legacy compatibility. No gameplay mechanics, RNG, scoring, history files, archive schema, signer, app IDs, editions, releases, task schedules or other PRs change.

**Gate:** Full core JUnit and desktop plus Android debug, release and instrumentation Java compilation on the exact isolated source commit. Only merge after terminal successful CI and fresh live remote-head/PR checks with expected-head non-force integration. No native original-signed APK construction, manual screenshots, installed-over saves, physical-device acceptance, or release publication is claimed from source CI.

### Initial status

Work claim only; at this point no production edits, tests, build, publication or merge have occurred. Later sections must record verified commits, actual terminal test results and blockers. No recurring APD tasks changed.

## Source and test implementation, approximately 02:06 EST

- Claim commit: `abe40c697199d5f49672d74cf4465eafa5685826`.
- One production-file guard commit: `3281382368663820dcb435e6050fbcb0e5636b95`. `ArchiveExporter.collect` requires `id.equals(r.header.runId)` only for `runs/` logs, without changing the public archive format or the `legacy/` compatibility path.
- Regression commit: `113cf64f3418bfef6a63400b1a4c1dc619e8eb23`. New `ArchiveExporterHeaderIdentityTest` contains four real file/writer/ZIP cases: copied alias with two valid neighboring logs and import roundtrip; alias-only refusal without source mutation; genuine two-segment shared logical run export/import; unchanged legacy export.
- Exact CI workflow commit: `295560da0d38e82d04d85c6579307afc758a85e3`. Dedicated [Actions run 43383321461](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43383321461) was seen **in progress**, not passed, at this checkpoint. Its gate runs all `:core:test`, `:desktop:classes`, Android debug/release/instrumentation Java compilation, JUnit XML totals and each named new test.
- [Draft PR #91](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/91) targets the authoritative branch, from the unchanged base SHA `97b66dbee095fbd33cd418a8e16b3f58bccd7712`. Do not merge until exact-source terminal success, renewed live remote-head checks and claim reconciliation. No new APK, signer, native/phone acceptance or release.

## Exact terminal source gate, approximately 02:08:40 EST

[Actions run 43383321461](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43383321461), attempt 1, completed with terminal **success** at 2026-11-13T07:08:40Z, testing the exact executable/test/CI commit `295560da0d38e82d04d85c6579307afc758a85e3`. Its job `131745903907` passed the exact guard check, full `:core:test`, `:desktop:classes`, `:android:compileDebugJavaWithJavac`, `:android:compileReleaseJavaWithJavac`, `:android:compileDebugAndroidTestJavaWithJavac`, and the separate XML-report assertion. Actual parsed JUnit output is **3,394 tests across 497 suites; 0 failures, 0 errors, 0 skips**, including all four new named real-writer/file/ZIP regressions. All inherited core suites were executed, not only the four targeted tests.

JUnit artifact ID `14017039253`, `apd-export-header-identity-junit`, size 548,557 bytes, digest `sha256:7a3cd17fb39601214cd63c5c6c2c8b22f64e71e949fc229a0c10600cb3f9aba2`, successfully uploaded. No executable files were changed by the documentation-only commits after the tested source commit.

**Still gated:** refreshed authoritative branch/claims and expected-head non-force integration, then independent readback. This is source/Java compile evidence only, **not** assembled APK, original private signer acceptance, native Android screenshot/device suite, physical RedMagic installed-over save acceptance, or release qualification. No APK or release is created by this run.

## Verified guarded source integration, Friday November 13, 2026, 02:10:22 EST

Before integration, independently fetched the live authoritative base `97b66dbee095fbd33cd418a8e16b3f58bccd7712`, isolated head `d48eda0573c2b0f8c5bf8e16184e1b3a541c65ae`, PR #91, full exact-source CI success and comparison. PR was 6 commits ahead, 0 behind; the only changed paths were this handoff, the single archive exporter production file, one new regression class and its dedicated source-test workflow. Since CI commit `295560da0d38e82d04d85c6579307afc758a85e3`, exactly two documentation-only commits had been added, without executable/test/workflow changes. No overlapping open work packages touched these paths.

Created two-parent integration commit `4b6b4ee107357245bdafc8a1991b1226e019f742` with exact parents `97b66dbee095fbd33cd418a8e16b3f58bccd7712` and `d48eda0573c2b0f8c5bf8e16184e1b3a541c65ae`, using the already-tested feature tree plus its documentation receipts. Ref moved with `force=false`, `expected_sha=97b66dbee095fbd33cd418a8e16b3f58bccd7712` and returned `success=true`. Independent branch readback confirmed exact resulting authoritative head `4b6b4ee107357245bdafc8a1991b1226e019f742`, the production guard in place, and [PR #91](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/91) closed as **merged** with that exact merge SHA. Commit timestamp `2026-11-13T07:10:22Z` = 02:10:22 EST.

**Execution summary:** authoritative starting commit `97b66dbee095fbd33cd418a8e16b3f58bccd7712`; production guard `3281382368663820dcb435e6050fbcb0e5636b95`; regression tests `113cf64f3418bfef6a63400b1a4c1dc619e8eb23`; exact CI source `295560da0d38e82d04d85c6579307afc758a85e3`; final code integration `4b6b4ee107357245bdafc8a1991b1226e019f742`. Core suite 3,394/497 passed, 0 failures/errors/skips; required desktop + Android debug/release/instrumentation Java compilation passed; artifact `14017039253` and run `43383321461` preserved.

**APK/publication blocker:** this source-only job does not assemble an APK, use original private signing inputs, run native Android visual/device acceptance, verify install-over/save integrity, or execute the gated publisher. Existing APD464 and every failed/held/published candidate remain unchanged. Native acceptance and private original signer availability must be established in the authorized build work package before another APD version can be published. No APK links are claimed. No gameplay formula, loot, scoring, save format, player history, edition boundary, app identity, task schedule, or concurrently owned branch was modified.

**Next safe work:** recheck the authoritative live head and active claims; work on a separate evidenced and approved defect, or when independent native release work becomes available, qualify this exact integrated source with original signer and required native/install-over gates before any next release. Do not re-open this exporter identity repair without a new reproduction.
