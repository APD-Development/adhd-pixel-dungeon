# APD normal-speed environmental overlays, November 12, 2026 (Eastern)

## Owner report
At maximum animation playback speed, status symbols and enemy-notice alert animate too fast; both magical wells (Health and Awareness) and the gas-room visual effect must remain at normal 1x visual rate. Owner provided a screenshot from an INDEV TEST build.

## Baseline and isolation
Verified established development head at start: `46d16489bf0069d8981fea6f285f13942bc1f24f` on `codex/apd-phone-fixes-2026-10-09`. Inspected AGENTS.md, existing PlaybackGroup tests, GameScene scene wiring, EmoIcon, FloatingText, BlobEmitter, WellWater, MagicWellRoom and ToxicGasRoom. Existing open PR #6 and APD461 candidate remain separate. No upstream searches were used. Work isolated on `chatgpt/apd-normal-speed-symbols-wells-gas-20261112`, draft PR #19. Do not write to the shared development branch until a fully verified merge.

## Source edits
- `237e6431cfd162071556e672d89db76b92f8ddc5`: introduce `PlaybackGroup.normalSpeedGroup()`, which creates an ordinary `Group` and therefore uses the unscaled original frame time.
- `818316087dc6fafd7536770ac5edabba14615746`: wire GameScene's `statuses`, `emoicons`, and `gases` groups to the normal-speed group. Floating status text lives in `statuses`; awareness icons in `emoicons`; gas particles AND both water-of-health and water-of-awareness well effects are BlobEmitters under `gases`. Other visual groups, actor turns, balance, save history and global game timing are unchanged.
- `65091f60b51a06d9ac2e753e490071c3f5520be9`: add `PlaybackSpeedTest.persistentDungeonOverlaysKeepNormalSpeedAtEveryPlaybackSetting` covering all ten 0.5x–1000x settings and asserting unrelated animated visual timing remains accelerated.
- `bb430111d22c7426688763fd720a834e2e24a730`: isolated CI gate workflow, GitHub Actions run [43277685870](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43277685870).

## Verification and release state
At the initial handoff check, CI run 43277685870 was in progress, without a conclusion. It is configured to run complete core tests, Android debug/release/instrumentation Java compilation, desktop classes, and assert required group wiring and the new regression test in JUnit XML. Do not claim it passed until confirming GitHub result. This session did not run tests locally, produce APKs, or publish a release. Latest independently verified published pair at the inspected checkpoint was APD460. Preserve APD461's independent native/publication requirements, original signer/application IDs, known-good builds and saved games.

## Next safe action
Check exact workflow run 43277685870 and its logs/reports. On failure, fix only demonstrated issue on isolated branch and rerun full suite. On success, recheck the shared development branch head, inspect any overlapping work claims, mark PR #19 ready and integrate without a force update. Ensure resulting shared branch has the verified source. Do not equate source-only Java compile with signed APK/native device acceptance. If eligible, run existing original-signer gated Android APK build/publish process separately. Keep the user informed of verified changes only.

## Verified completion, November 12, 2026, 1:19 PM Eastern

GitHub Actions [43277685870](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43277685870) completed **success** on exact code/workflow commit `bb430111d22c7426688763fd720a834e2e24a730` at 1:17:38 PM Eastern. The CI wiring assertion passed, the complete Gradle core test suite and desktop/Android debug/release/instrumentation Java compilation step passed, and the independent final XML assertion passed, requiring at least **3,301 core tests**, zero errors/failures/skips and the new `persistentDungeonOverlaysKeepNormalSpeedAtEveryPlaybackSetting` regression. The CI artifact-upload step also succeeded. The only commit between the tested head and PR head was this Markdown handoff, with no source/test/workflow changes.

[PR #19](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/19) was marked ready and merged using expected head `e65077b8306b5b3b6c75e501a6d5a9ee82d164bb` at **1:18:41 PM Eastern**. The verified new authoritative development-branch merge head immediately after was `41ef71ae4be981875335be22d6f7d17c002425c0`; initial authoritative development head was `46d16489bf0069d8981fea6f285f13942bc1f24f`. The merged code changes are limited to the three GameScene visual groups and the supporting PlaybackGroup helper/test. All existing speed selections are retained.

This is a **source-only merge**, not an APK acceptance, signed build, native device demonstration, or release. APD460 remains the latest independently verified published Developer/Alpha pair, and the separate APD461 candidate/publication gate remains untouched. No original signing input, app ID, history, save schema, gameplay turn timing or scoring was changed. Physical phone animation readability at 1000x is pending owner validation on a future qualified APK. No new APK was built or published in this visual-fix execution.

**Next safe step:** do not reopen/duplicate the completed 1x overlay work. When qualified original-signer build environment and the APD461 independent acceptance state permit, incorporate the authoritative merged source into an independently gated next APK candidate, require full native acceptance and publish only after all original signer/save integrity conditions are met. Do not treat this source CI as such qualification.
