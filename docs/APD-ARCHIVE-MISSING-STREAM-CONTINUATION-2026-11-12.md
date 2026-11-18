# APD archive missing-stream refusal, November 12, 2026

## Verified source-only development

**Time (America/New_York, EST):** started about 12:44 PM, source and isolated CI submitted 12:45 PM, source verification completed at **12:47:43 PM**. No unattended work between invocations is claimed.

**Starting authoritative development branch:** `codex/apd-phone-fixes-2026-10-09` at `5e2c1410a5985aa55c4bf713d5a52d392cac7bc3`. This was checked live; `main` was not used. Isolated branch: `chatgpt/apd-archive-null-stream-20261112`.

**Issue discovered in owner repository:** `ArchiveImporter.importArchive(root, null)` built its ZIP input wrapper before entering the importer error-report handler; a missing input stream could therefore throw instead of returning the promised refusal `Report`. The package-level already-open ZIP entry point had the same issue for a null stream, including a null dereference in its cleanup path. No real device crash was observed, so this is a proved code-path defect, not a claimed reproduced Android incident.

**Changes:**
- Production commit `f3ebd6ce7806d9982b6422c1051490e2370a720d`: refuse a missing raw source and a missing ZIP stream with a failed report before constructing or reading the wrapper. Existing nonnull paths, input ownership and validation stay untouched.
- Test commit `17c701da0f30e5b3fbc4dd469a718226b15c065e`: add `ArchiveStreamOwnershipTest.missingArchiveStreamsReturnAReportWithoutCreatingHistory`; checks both entry points return `ok=false`, zero imported logs, diagnostic problem and no history-directory mutation.
- CI workflow commit `2f70c5420505c77a7dd1ffcfcb7873ac5fcea49e`: isolated push-triggered source gate for this branch only, with complete core tests and desktop/Android Java compilation.

**Exact verification evidence:** [GitHub Actions run 43272737266](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43272737266), attempt 1, on exact source/workflow commit `2f70c5420505c77a7dd1ffcfcb7873ac5fcea49e`, finished **success** at 2026-11-12 17:47:43 UTC. Job `131373148418` logged `BUILD SUCCESSFUL in 1m 58s`. The JUnit XML gate confirmed **3,298 tests in 472 suites; zero failures, errors or skips**, including the newly added regression. The same workflow completed `:desktop:classes`, `:android:compileDebugJavaWithJavac`, `:android:compileReleaseJavaWithJavac` and `:android:compileDebugAndroidTestJavaWithJavac`. This is source/Java verification; it does not establish emulator-native UI acceptance.

**Preservation:** No scoring, seed reuse, gameplay mechanics, difficulty, numerical balance, run data, history format, save compatibility, signing, application IDs or original release assets changed. `ArchiveImporter` does not close caller-provided raw streams in the normal path, as before. This change does not touch Search/WallGeometry, the merged PR #16 or the separately blocked APD461 draft PR #6.

**Build/publication boundary:** No new APK was built or published in this session. APD460 remains the independently verified published Developer/Alpha pair in the checked release record. APD461 is a separate candidate with an unresolved Alpha WallGeometry native-acceptance failure and rejected publisher gate; this session neither altered nor bypassed those checks. Physical RedMagic install-over and audio are not verified.

## Next safe continuation

Verify the live development branch and any active work claims before merging/integrating this isolated source work; do not force-push or rewrite newer work. If the development branch is unchanged and PR review/mergeability permit, integrate the bounded change with expected head SHA. Then resume independent approved issues, without repeating PRs #7–#16 or touching APD461's native failure investigation unless it is unclaimed. Preserve this CI evidence and all prior assets.
