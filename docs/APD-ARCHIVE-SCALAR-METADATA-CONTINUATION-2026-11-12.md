# APD archive scalar metadata singleton validation, November 12, 2026 (Eastern)

## Isolated work claim (Thursday, November 12, 2026 at 9:42:45 PM EST)

Owner-directed regular ChatGPT APD execution. Live authoritative branch `codex/apd-phone-fixes-2026-10-09` was verified at `c22d240a9b597bb693f5bac6b6b827774e0f06e0`; `main` is not the development baseline. AGENTS.md, recent archive singleton handoffs, the current archive parser and tests, active PRs, branch commits, CI and release records were inspected. Independent branch: `chatgpt/apd-archive-scalar-singletons-20261112-2142`. No open pull request claims scalar `created`, `app`, or `telemetry` manifest duplicate validation. Preserve unrelated active playback, native Android, wall search, original signing and release work without shared-branch writes.

Source-evidenced defect: `ArchiveManifest.encode()` emits exactly one of each recognized `created`, `app`, and `telemetry` metadata records, but `ArchiveManifest.parse()` accepts repeated checksum-valid records and silently replaces the prior values. This allows ambiguous archive provenance/timestamps. Scope is strictly to reject a second recognized line for each of these three keys, whether identical or contradictory. Do not require fields that historical manifests may omit, alter the v1 file format, unknown future metadata, duplicate log-path first-wins compatibility, saves, gameplay, history, scoring, signer, app IDs, or edition boundaries. Provide real checksum-valid unit and non-mutating ZIP import regressions.

At claim: no production change, no tests executed, no APK assembly, no native Android/phone acceptance, no original signer and no publication. Create a dedicated source-only CI gate for complete `:core:test`, desktop and Android Java compiles. Merge only after exact-source terminal success and fresh live authoritative and PR checks with expected-head non-force merge. Previously published APD464 remains unchanged; release records checked independently. No recurring tasks changed.

## Continuation
The initial work claim above is preserved; verification results and integration are documented below.

## Production and source CI checkpoint (Thursday, November 12, 2026 at 9:45:21 PM EST)

- Work-claim commit: `f5285e04872fbff12c97ccb59a5ac242852ff44c`.
- Production parser correction: `25ad2ca71a5350776ed8644a0f9bf192ffc2c1cb`. The three scalar metadata keys now have independent seen guards. No format, scoring, save, gameplay, Android identity or asset changes.
- Regression class: `b9d0bf16dd92313b9ac18058cdbf096aca6f0cac`. Five test methods cover all three checksum-valid duplicate record types (identical and contradictory), unknown/omitted key compatibility, and ZIP import refusal without history mutation.
- Dedicated source/Java verification workflow: `88bed85317474b494068469ffc9803adbd01736f`. [Actions run 43354238559](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43354238559), first attempt and exact source head `88bed85317474b494068469ffc9803adbd01736f`, **still in progress** at this checkpoint; no success or full test-count claim.
- [Draft PR #66](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/66), exactly four scoped paths: parser, regression test, workflow and this handoff. Compare against original authoritative head showed four commits ahead with no unrelated changes.
- APK/native/publisher boundary: no original-signed APK built, Android native/device acceptance performed or release published in this package. APD464 is the previously verified release and does not include this source change. No other active PR or automation changed.

Next: inspect terminal CI and JUnit XML/job logs; if exact source passes, perform fresh authoritative-branch and PR-head checks, integrate only via expected-head non-force merge; record terminal result and commit in this document. If source CI fails, correct only demonstrated test/source errors, preserving failure evidence. 

## Terminal exact-source verification (Thursday, November 12, 2026 at 9:47:49 PM EST)

[GitHub Actions run 43354238559](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43354238559), attempt 1, completed **success** on exact source/test/workflow SHA `88bed85317474b494068469ffc9803adbd01736f`, job `131652310103`. Decoded job logs confirm `BUILD SUCCESSFUL in 2m 10s` for the full `:core:test`, `:desktop:classes`, `:android:compileDebugJavaWithJavac`, `:android:compileReleaseJavaWithJavac`, and `:android:compileDebugAndroidTestJavaWithJavac` gate. JUnit XML acceptance reported **3,354 tests across 486 suites, zero failures, zero errors and zero skipped**. All five new singleton regression methods and the previously required archive compatibility methods passed. Artifact: `14005521876`, `apd-archive-scalar-singletons-junit`, 540,314 bytes; uploaded successfully. This document-only update is NOT a newly compiled source head and does not re-run CI.

Only the parser, scoped regression class, dedicated workflow and this handoff are in PR #66. No APK was assembled or signed, no Android instrumentation/emulator or physical RedMagic tests executed, and no native manual/signer/save-compatibility/publication gate passed in this execution. Source verification must not be mistaken for new qualified APK delivery. Preserve existing APD464 and all independent pending release work.

Next: fresh live authoritative and PR-head check, expected-head non-force merge if clean and no competing package, then verify actual integration commit and append exact completion receipt.

## Guarded integration and terminal execution receipt (Thursday, November 12, 2026 at 9:49:24 PM EST)

After complete source CI success, [PR #66](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/66) was re-read alongside all active work and the live authoritative branch. The four-path diff remained independently scoped, the PR was mergeable/clean and there was no competing scalar-metadata PR. Live authoritative target remained `c22d240a9b597bb693f5bac6b6b827774e0f06e0`. PR documentation-only head was `bd68b22828a123371ac7e46de31e28124c94577b`. GitHub received an ordinary **non-force** merge with `expected_head_sha=bd68b22828a123371ac7e46de31e28124c94577b` and returned `merged=true` at integration commit `4fbf5f4da1b88f67c8869299a18f65824e77d438`.

Independent readback confirmed PR #66 closed and merged at that SHA, the authoritative branch at the same SHA immediately after integration, and all three parser safeguards present in the merged source. No other open PR was merged, no competing work claim or shared branch was overwritten and no automation schedule was changed.

**Exact receipts:** start `c22d240a9b597bb693f5bac6b6b827774e0f06e0`; claim `f5285e04872fbff12c97ccb59a5ac242852ff44c`; production `25ad2ca71a5350776ed8644a0f9bf192ffc2c1cb`; tests `b9d0bf16dd92313b9ac18058cdbf096aca6f0cac`; workflow and fully tested source `88bed85317474b494068469ffc9803adbd01736f`; pre-merge source-gate receipt `bd68b22828a123371ac7e46de31e28124c94577b`; integrated source `4fbf5f4da1b88f67c8869299a18f65824e77d438`. Exact [source CI 43354238559](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43354238559) **3,354 JUnit tests / 486 suites, 0 failures/errors/skips**, desktop and Android Java compile success, JUnit artifact 14005521876. Source CI was run on the workflow commit, not the later documentation commits; documentation contains no executable changes.

**Build/publication:** No APK was assembled or signed during this execution, no Android native instrumentation test was executed, no signer/save install-over validation passed and no new release was published. Original APD464 and all prior qualified releases remain unchanged. Source integration is not acceptance for the native APK publication gates.

**Next safe work:** Check the fresh authoritative head and concurrent work claims. Continue independently evidenced APD reliability/performance fixes, or let the separately owned original-signed APK/native release package integrate this fix into a later qualified build. Do not resurrect completed manifest singleton fixes or bypass native, signing or publication gates. The originating regular ChatGPT execution is now finished; it does not continue coding between scheduled runs.
