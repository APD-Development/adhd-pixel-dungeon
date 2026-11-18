# APD archive single-check record guard, November 12, 2026 (Eastern)

## Isolated work claim (Thursday, November 12, 2026, 10:35 PM EST)

Owner-directed continuation in the originating regular ChatGPT project thread. Verified authoritative branch `codex/apd-phone-fixes-2026-10-09` at `e70424ad41d1ccc36991497e703a2334be877214` after inspecting AGENTS.md, current archive handoffs, recent commits, open PRs, CI/workflows, previous build/release receipts and the parser/test source. `main` is not the baseline. This work is isolated on `chatgpt/apd-archive-single-check-20261112-2235`; no conflicting open manifest/checksum work package was identified. Independently active playback, native, release, wall-search and GPL work remains untouched.

### Source-evidenced defect and exact scope

The v1 writer produces one terminal `check\t<sha256>` line. `ArchiveManifest.parse` currently locates the **last** `check\t` and accepts earlier `check` records in the checksum-protected body as unknown forward records. A malicious or malformed archive can append a new correct final checksum to an already checksummed manifest, causing the parser to accept two authoritative checksum records. Refuse any additional recognized `check` record inside the body even when the final checksum is valid. Preserve the single-check original format, unknown extension records, old history, first-wins duplicate path semantics, legitimate Unicode, scoring, saves, signer, app IDs, editions and all prior qualified builds.

Add checksum-valid manifest regression cases plus a ZIP import safety regression that proves original history bytes remain unchanged. Full :core:test, desktop classes and Android debug/release/instrumentation Java compile are required on the exact proposed source. Do not describe Java compilation as native Android acceptance.

### Execution checkpoint

At claim time: no production change, tests, APK build, signer test, native device acceptance, release or publication has occurred. APD464 is an earlier published release; this package must not overwrite it or independently claim a new APK. Use an isolated PR, inspect live heads, and merge only after exact-source verified successful CI via non-force expected-head integration. Record all actual results, blockers and next work below.

## Source and verification checkpoint (Thursday, November 12, 2026, 10:38 PM EST)

- Initial work claim `701be480227c2f24af034de0529ef08bee33d91c`.
- Production parser fix `bb65aa47cd32e76f351653802dc81f3d179b594f`: explicit refusal of a second `check` record in the protected body, without modifying the normal final checksum computation or format.
- New four-method regression class `22aa5bbce86b8b763f1142c0e236f293c89ea9b8`: duplicated correct final check, conflicting earlier check with a newly correct final check, standard/unknown metadata compatibility, and ZIP refusal preserving existing history.
- Dedicated full core and desktop/Android Java source gate at `4a1e3710658282d70e81e40531c67f585f82a1e5`; [Actions run 43360199699](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43360199699) was **in progress** at this checkpoint, not yet a passed test claim.
- [Draft PR #72](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/72), four scoped paths only. Compare with original authoritative head: four commits ahead, zero behind, no unrelated files.
- No APK assembled or signed, no native device tests run, no new release published, no recurring APD tasks changed. Prior APD464 remains intact.

Next: obtain terminal exact-source CI evidence including actual JUnit counts; fix evidenced failures if any. Only then re-read live authoritative target, open PR claims and tested source head, and integrate non-force with expected-head guard. Preserve failure records and independent native/release work.

## Exact terminal source CI (Thursday, November 12, 2026, 10:41 PM EST)

[Actions run 43360199699](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43360199699) on **exact executable source/test/workflow commit** `4a1e3710658282d70e81e40531c67f585f82a1e5`, attempt 1, completed **success**. Job `131671609118` logs: `BUILD SUCCESSFUL in 2m 22s` for `:core:test`, `:desktop:classes`, `:android:compileDebugJavaWithJavac`, `:android:compileReleaseJavaWithJavac`, `:android:compileDebugAndroidTestJavaWithJavac`. Terminal JUnit XML gate: **3,364 tests across 489 suites; 0 failures, 0 errors, 0 skipped**, including all four new checksum singleton regressions, the existing strict-UTF8 manifest regression, old scalar metadata and duplicate-path first-wins compatibility tests. Evidence artifact `14007463319` (`apd-archive-single-check-junit`, 543,150 bytes) uploaded successfully.

Post-CI documentation-only commit `78f283b13d58f884a6f6aa4d4a55e6ed441a69e2` records initial pending status and is not the source tested by CI. The next documentation commit similarly does not modify compiled code. A fresh pre-merge read confirmed authoritative branch still at `e70424ad41d1ccc36991497e703a2334be877214`, only this checksum PR claims the area, and proposed diff is limited to the parser, test, dedicated workflow and handoff.

**Not executed:** no assembled or original-signed APK, no Android instrumentation/emulator or physical phone tests, no install-over signer/save integrity acceptance, and no release publication. Previous APD464 remains unchanged. Next: make PR review-ready, perform non-force expected-head merge after another live PR/base check, then verify integration and record final receipt. 

## Guarded integration and final execution receipt (Thursday, November 12, 2026, 10:42 PM EST)

After terminal passing CI, refreshed authoritative target and concurrent open PR list. The base remained `e70424ad41d1ccc36991497e703a2334be877214`. The source-only work did not touch the other open playback, native, search, GPL, or release packages. PR #72 became review-ready, GitHub confirmed it was clean/mergeable with four scoped paths and exact PR head `30599daae57c031da5332540b76c9daa526b0b9a`. A standard **non-force** merge using `expected_head_sha=30599daae57c031da5332540b76c9daa526b0b9a` returned `merged=true` at `4a9b158efc6406bada1b2331ac37c1d99acaf783`. Independent remote re-read confirmed authoritative branch at that integration SHA, PR #72 closed/merged there at 10:41:52 PM EST, and the actual `case "check":` guard present in merged source.

**Complete source chain:** authoritative start `e70424ad41d1ccc36991497e703a2334be877214`; isolated claim `701be480227c2f24af034de0529ef08bee33d91c`; production `bb65aa47cd32e76f351653802dc81f3d179b594f`; regression `22aa5bbce86b8b763f1142c0e236f293c89ea9b8`; fully tested executable source `4a1e3710658282d70e81e40531c67f585f82a1e5`; interim docs `78f283b13d58f884a6f6aa4d4a55e6ed441a69e2`; terminal evidence docs `30599daae57c031da5332540b76c9daa526b0b9a`; production integration `4a9b158efc6406bada1b2331ac37c1d99acaf783`. Exact passing Actions run `43360199699` and JUnit artifact `14007463319`: **3,364 tests / 489 suites, no failures/errors/skips**, required desktop and Android Java compilation complete. Post-source docs were not rerun as executable CI.

**APK and publication blocker:** This execution assembled, signed, natively tested and published **zero** APKs. An original-signed build pair and its exact-source native Android/owner-signer/save install-over evidence do not exist for this correction; the established acceptance/publisher gates therefore cannot be passed by this source-only workflow. APD464 remains untouched and does not include the newly merged fix. Do not label Java compilation as native acceptance, reuse an old APK as this new source, or publish without complete gates.

**Next safe work:** a separately coordinated native/signing release package may pick up this integrated source only after exact-source acceptance on the original signed pair. Otherwise proceed on another owner-evidenced, unclaimed source bug; check live remote heads/work claims first. This regular ChatGPT execution finished its source package and did not change any APD recurring task or schedule. No work continues between scheduled executions absent an actual running mechanism.
