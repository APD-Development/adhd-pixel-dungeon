# APD archive duplicate-total-line validation, November 12, 2026

## Independent work claim, 11/12/2026 around 21:04 EST

Continuation in the existing regular ChatGPT APD conversation. Authoritative development branch `codex/apd-phone-fixes-2026-10-09` was independently verified at `a11eff710512d4754b23d1d2a2e17b4c8eb2df92`, not `main`. Isolated work branch: `chatgpt/apd-archive-count-duplicate-20261112-2104`. PR #59 (negative event counts) and PR #60 (terminal handoff) were already merged, and there was no PR/issue claim for repeated `count` records at this checkpoint. Open playback/native APK, wall search and other work packages remain untouched.

## Source-evidenced defect and narrow scope

The format-1 `ArchiveManifest.encode` writer produces exactly one `count\t<files>\t<bytes>` line. The `parse` switch currently overwrites `countFiles` and `countBytes` for every recognized `count` line, accepting a checksum-valid manifest with contradictory duplicated totals when only the last matches. This permits inconsistent archive metadata to be silently discarded. Reject a second known `count` record, including two identical totals, while continuing to ignore future *unknown* metadata lines and retaining established first-wins duplicate *file path* compatibility. Existing current/legacy valid manifests, history, saves, scoring, mechanics, application IDs, signer, APK assets and releases must not change.

## Execution status at claim

No source edit, regression, CI, original-signer APK, emulator native acceptance or publication has been performed for this package yet. Local private Git clone access is unavailable because DNS to github.com fails; use scoped GitHub connector changes and a branch-scoped Actions workflow for real Java/JUnit verification, not static assertions alone.

## Safe continuation

Add the parser guard and focused checksum-valid manifest regressions including a real ZIP import non-mutation test, run full core and Android/desktop Java compilation in isolated exact-source CI. Preserve failed receipts if any. Recheck live authoritative and PR heads before a guarded non-force integration; do not merge or publish without verified source and release gates. Document actual Eastern timestamps, commits, JUnit totals, CI result and native/APK boundaries.

## Implementation and verification checkpoint, 11/12/2026 around 21:06 EST

- Parser guard commit `0519349cfae409065664415d2b1874dd55d6da6f`: a second recognized `count` totals record now raises `IllegalArgumentException("the manifest has repeated totals")` rather than overriding the prior totals. Existing first-wins duplicate *file paths* are unmodified.
- Four regression tests in `ArchiveManifestDuplicateCountTest.java`, commit `730588b3e91fcedbd5ec2b0d20d0d1f947af16f8`: contradictory checksum-valid totals, identical duplicate totals, valid single-count round trip, and real ZIP rejection without altering an existing history log.
- Dedicated source-only CI workflow commit `3d039b20bf5b72656ee421eee39696c82e77cd12`. [Actions run 43349653095](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43349653095), exact source/test/workflow SHA `3d039b20bf5b72656ee421eee39696c82e77cd12`, was **in progress** when checked. Static scope preflight passed; full Gradle JUnit + desktop/Android Java checks had not yet finished. Do not interpret pending tests as passed.
- [Draft PR #61](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/61) targets only `codex/apd-phone-fixes-2026-10-09`. Fresh compare showed exactly four changed paths: this handoff, the parser, test class and isolated workflow. No changes to gameplay, scoring, saves, original log formats, old releases, personal animation or native APK work. No APK was assembled, signed, native accepted or published.

**Next:** inspect terminal CI, decoded JUnit totals, and all required Java targets. If passing, recheck live head, open claims and PR diff before a non-force expected-head merge; otherwise preserve the failure and fix the specific test. Record the final branch and publication boundaries.

## Terminal isolated source verification, 11/12/2026 at 21:09 EST

[GitHub Actions 43349653095](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43349653095), attempt 1, job `131637326831`, **completed success** on exact source/test/workflow SHA `3d039b20bf5b72656ee421eee39696c82e77cd12`. Decoded job logs prove `BUILD SUCCESSFUL in 2m 47s` for `:core:test`, `:desktop:classes`, `:android:compileDebugJavaWithJavac`, `:android:compileReleaseJavaWithJavac`, and `:android:compileDebugAndroidTestJavaWithJavac`. The JUnit acceptance step reports **3,341 core tests / 483 suites, failures 0, errors 0, skipped 0**. All four new duplicated-totals tests and existing manifest/event/boolean/first-wins compatibility checks are present in JUnit. Static scope gate and artifact upload passed. Artifact `14003262575`, name `apd-archive-duplicate-count-junit`, size 540,283 bytes, SHA-256 `c5516e0a53baff343682544b980b7a565fce21cf6e61d4b6251b12c59c70cf4b`.

This is **real Java source-only verification**, **not** a built/signed APK, native emulator or physical RedMagic acceptance, screenshots, save-install verification or release publication. Existing published APD464 and all prior releases are unchanged. This documentation commit is not a second CI run. The dedicated scoped CI workflow triggers on future changes to parser/test/workflow, not on documentation-only updates.

**Safe integration next:** independently re-read authoritative live head, PR head/diff and current claims, remove draft only if mergeable and exact-source CI still good, then use GitHub merge with `expected_head_sha`. Do not overwrite a moved target or competing work. Afterward re-read the resulting merge SHA and update this continuation with a terminal receipt. No native/APK gate has been run.

## Final verified integration, 11/12/2026 at approximately 21:10 EST

After [terminal exact-source CI 43349653095](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43349653095), full PR scope and competing claims were checked again. PR #61 contained exactly the one-file parser guard, four-method regression class, isolated source-only workflow and this handoff. The live authoritative development head was still the original `a11eff710512d4754b23d1d2a2e17b4c8eb2df92`. The final PR head was `055bd37d81b7c240c95799a3647bd9e525d58e12` (documentation-only descendants of the tested source). GitHub reported `mergeable=true`, `mergeable_state=clean`; the draft PR was marked ready, then merged non-force with its exact `expected_head_sha`. GitHub returned `merged=true` and merge SHA [`0724919f0158f4ad5576f422f04230c1a52ee04a`](https://github.com/APD-Development/ShieldingShouldHaveWorked/commit/0724919f0158f4ad5576f422f04230c1a52ee04a). Independent remote readback confirmed PR #61 closed/merged at that SHA, branch head at that SHA, and the production guard present on the authoritative branch. This documentation-only terminal receipt is not another tested source head.

**Exact source history:** authoritative starting SHA `a11eff710512d4754b23d1d2a2e17b4c8eb2df92`; parser commit `0519349cfae409065664415d2b1874dd55d6da6f`; test commit `730588b3e91fcedbd5ec2b0d20d0d1f947af16f8`; exact passing source/test/workflow SHA `3d039b20bf5b72656ee421eee39696c82e77cd12`; successful merged SHA `0724919f0158f4ad5576f422f04230c1a52ee04a`. Actual execution: **3,341 core JUnit tests / 483 suites, zero failed/errors/skipped; full desktop and Android debug/release/instrumentation Java compilation passed**. Static checks and artifact upload passed. No editor-local Gradle execution claimed; all actual tests were on GitHub Actions. Compilation is not a built APK.

**Native/release limits:** no new game APK was assembled, originally signed, installed or run in emulator/physical RedMagic, manually image-accepted or published. APD464 (published earlier by an independent package) does not contain this source-only change. No direct download URL for this fix is available. Existing saved histories, previous released APKs, edition boundaries, stats/scoring/gameplay, private signing inputs and other active playback/Android work were untouched.

**Next safe work:** inspect live authoritative head/claims before any further work, then choose a separate previously approved defect or coordinate a future successor through the original private-signer, native, screenshot/manual and release qualification workflows. Do not use this source-only CI receipt as permission to publish. Do not repeat the duplicate-totals guard. Existing APD task schedules were not modified.
