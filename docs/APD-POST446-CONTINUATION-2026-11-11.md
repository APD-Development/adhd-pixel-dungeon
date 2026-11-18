# APD continuation after accepted 446, November 11, 2026

This is a bounded continuation record. Read AGENTS.md and docs/APD-446-DELIVERY-2026-11-11.json before treating any older status as current. APD446 remains the last independently verified and published prerelease. Existing APKs, the original signer, save compatibility, mechanics, scoring formulas, challenge balance, and source archive are unchanged by this work.

## Completed and independently verified

- The previously accepted APD446 Developer and Alpha-Testers APKs and corresponding source were published at https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/tag/apd-446, separately from the stable build 84. GitHub release asset SHA-256s and their release readback matched the accepted delivery receipt. The publication workflow succeeded in run 43093359267. Commit 7e5d1fcf107159dce6780378e94a48fca1edbc67 preserves publication metadata.
- Screenshot transport reliability change in tools/apd_native_screenshot_transfer.py: a bounded three-attempt retry only for transient adb manifest transport errors; corrupted manifests, missing/changed/extra files, invalid PNGs and mismatched SHA-256 remain failures. The three new unit regressions and original recovery safeguards passed in GitHub Actions run 43094350971. Implementation commit 0e1608827ad982507f99234ace6a3e0ed0dfcbeb; tests finalized at b9c38304973ed7c9daf4c380ea4b419cfe6c84bf.
- Seed-history bug correction in core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/recorder/SeedHistory.java: an explicitly entered numeric -1 must not be confused with the empty-selection sentinel. The read-only exact history lookup now permits numeric -1 recorded attempts while the blank selection retains normal local seed generation. The regression uses real raw lifecycle logs, verifies deepest depth and non-mutation, and guards unrelated -2. Implementation commit 3dcb9f42bab9dd4c2f7a1d71abde823bf80f8c4e, syntax/comment correction 4921cca47e185b5c77d9fbfb9b3959c5fda773fb, JUnit regression commit 81272a9288a7f8e4ae6e83c95bd001b3d6826c08.
- GitHub source CI run 43094730536 passed its actual complete core/Android/desktop compilation step and the explicit >=3248 tests, zero failures/errors/skips, exact new SeedHistoryTest case gate. At the last observation, the overall workflow remained in progress uploading core XML artifacts. Do not call the entire workflow successful until terminal state is checked.

## Pending build and owner acceptance

- Source verification exact head: 05e499fd31ecf9d805a05174eea5b3defa480941 (workflow created for this scope).
- Isolated APD447 candidate-building workflow commit 4b4455da1299afd61ff727b1e42876155bbec477, run 43095464581. Its production job requires that exact source verification run to conclude success and checks it has no intervening application edits before building frozen Developer C447 and Alpha-Testers P447 APKs with the existing original signing certificate and versionCode 920. It also records APK SHA-256 and the exact source zip. On last observation the run was queued. Candidate assembly is not a release, does not claim native/manual/device acceptance, and must not overwrite accepted 446.
- After the source and candidate jobs finish, inspect raw run conclusions, evidence artifact, signer/package/variant/digest receipts and any failure logs. Run exact scoped native Android tests and manual visual/source acceptance before any APD447 owner delivery or GitHub release. Never invent a successful APK or artifact URL.
- The imported-seed multiplier and personal seed-replay/depth reward numeric formulas remain owner decisions. Existing scores and saved history schema were not changed. Do not implement guessed values.
- Continue from the latest verified source after checking these runs, avoid reopening completed 446 investigations. Remaining physical RedMagic install-over and audio coverage are still unverified and cannot be claimed from an emulator run alone.

## Verification links

- Screenshot transport CI: https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43094350971
- Full source CI: https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43094730536
- APD447 candidate build: https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43095464581
