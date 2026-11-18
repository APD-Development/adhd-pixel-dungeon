# APD local backend-record integrity continuation, November 12, 2026 (Eastern)

## Verified starting state and isolated ownership

Started the coding execution about 3:46 PM EST in the existing regular APD conversation. Read the owner's private repository, AGENTS.md, the active handoffs and ledger, open PRs, CI, and published releases. Authoritative branch codex/apd-phone-fixes-2026-10-09 had exact HEAD 51192feb191c092b4c81a0f13aa54861b2d5ce0b when this isolated work branched. Main was not used. Previously completed report-provider/index/search/archive fixes and parallel animation, APK/native and gated release claims were not touched. No upstream resources were accessed.

Isolated branch: chatgpt/apd-tabfile-integrity-20261112-1545. Draft PR #40: https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/40 . GitHub ref writes used expected-head, non-force updates and never wrote to the shared development branch.

## Evidenced source findings and changes

The existing backendbridge/TabFile.java loads Base64-encoded fields using UTF-8 replacement decoding, allowing invalid bytes to silently become U+FFFD instead of rejecting that damaged record. Its write fallback deletes the existing destination if a sibling rename fails, before another rename that may also fail. This creates a source-level record-integrity risk for optional local session, submission and receipt records, not a verified user-device incident.

Commit 6a0d8b509182e1fca7ee330562ca722ad4637919: Strict UTF-8 field decode; skip only damaged lines; refuse directory destinations; never delete existing records after a failed rename. Four new JUnit cases check malformed UTF-8 between intact Unicode records, normal Unicode replacement, directory-target preservation, and byte preservation on failed temporary writing. Valid Base64 encoding, schema, successful record reads/writes, gameplay, balance, saves, scores, history, original signer, app IDs, and edition boundaries are unchanged.

Original run https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43304596378 failed in new test-source compilation. Raw logs identify five incompatible Arrays.asList(String[]) generic row types. This failure is preserved and is not passing CI.

Corrective commit 804082cbd5e644bc574357251438fb1aedbfa50b replaces those five fixtures with Collections.singletonList(String[]) and adjusts CI path filters so corrected source/test pushes retrigger verification. Exact corrected source run https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43304707388 was in progress at this handoff checkpoint. No clean test counts or Android Java compile success is claimed before terminal evidence.

## APK/publication and next safe execution

No signed APK was built, tested natively, published or installed by this execution at the initial handoff. Separately verified release APD462 was published at 2026-11-12T20:20:21Z with Developer, Alpha-Testers and matching source assets: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/tag/apd-462 . It does not contain this local record fix, and APD461 and PR #38 remain separate.

Next safe step: inspect run 43304707388 at its exact source SHA and its raw jobs/JUnit XML, requiring at least 3,315 clean core cases including all four new methods and desktop plus three Android Java compilation targets. Correct demonstrated failures only. Before any merge, reread the live authoritative head, PR #40 head and open work claims; integrate without force/overwrite only after exact CI success. Keep this receipt updated with integration/test results and distinguish source verification from native/signed APK gates.

## Terminal source verification and guarded integration, 3:56–3:58 PM EST

The exact corrected [GitHub source run 43304707388](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43304707388), attempt 1, completed **success** at approximately **3:56:41 PM EST** on production/test/workflow commit **804082cbd5e644bc574357251438fb1aedbfa50b**. Raw job 131485194606 logs say BUILD SUCCESSFUL in 2m 10s after the complete core test task and desktop, Android debug/release and Android instrumentation Java compilation targets. Independent JUnit XML gate reported exactly **3,315 tests across 476 suites, zero failures, zero errors, zero skips**, and explicitly checked all four new TabFile regressions. Artifact upload also succeeded with artifact ID **13983337128**. Later changes to this Markdown handoff did not change the tested code. The original test-source compilation failure in run 43304596378 remains preserved and is not retrospectively passed.

Before integration, freshly read the authoritative development head, isolated feature head, draft/mergeability status and exact four-file diff. Base was still **51192feb191c092b4c81a0f13aa54861b2d5ce0b**, ready clean [PR #40](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/40) head was **67af637b0ac32145ffe531830c1dfdffb24f7a5f**, and PR mergeable state was clean. Created the two-parent exact-tree merge commit [2a91d403](https://github.com/APD-Development/ShieldingShouldHaveWorked/commit/2a91d403fa9a4dfec0cad6478dbcd09bd6f58ed5) and advanced the authoritative branch **without force** using expected_sha=51192feb191c092b4c81a0f13aa54861b2d5ce0b. Independent immediate GitHub readback confirmed branch **2a91d403...**, PR #40 **closed and merged** with the same merge SHA at **3:58:09 PM EST**, and all three production code protections present.

**Exact start development commit:** 51192feb191c092b4c81a0f13aa54861b2d5ce0b. **Verified code commit:** 804082cbd5e644bc574357251438fb1aedbfa50b. **Integrated code merge:** 2a91d403fa9a4dfec0cad6478dbcd09bd6f58ed5. **This final documentation-only continuation commit** is later and must not be misreported as another source CI gate. Four files were integrated: the TabFile production change, four-case TabFile test, scoped source workflow, and this handoff.

**APK/build/publication result for this execution:** no application APK was assembled, signed, emulator-tested, physically tested, published or delivered. Source/core Java verification does not satisfy native Android acceptance, save/signature identity review, or existing gated APK publisher. The independently already published [APD462](https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/tag/apd-462) remains available and unchanged; its binary does **not** contain this later TabFile correction. Preserve all APD461 held receipts, parallel APD462 integration PR #38, other open work claims, and all old accepted releases.

Next safe work: read the newest branch head and current PR/work claims before choosing a separate evidenced owner-approved issue. Do not repeat this TabFile issue or its four tests. Only publish a new playable APK once the independent original-signer, native, manual screenshot and release gates actually pass. No between-execution coding is implied.
