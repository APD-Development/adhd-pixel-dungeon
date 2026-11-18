# TEST266 UIMSD hidden-question consistency correction — 2026-10-17

## Result

TEST266 is a verified and independently staged private candidate at exact workflow source `e678d38c4c34bc755ed0cd501f7f44c88bc3c465`, containing application correction `09ba71aab8fd2cb426679add9c9d0ed2e4d72fb7`. Full run [39011090870](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39011090870) succeeded from 2026-10-17T06:45:29Z through 06:58:21Z.

The correction closes a real builder/execution mismatch. A player could enable Hidden details, choose an internal measure, split or condition, and then disable Hidden details. The visible question sentence retained those internal selections while QueryEngine correctly rejected them, so the displayed question and executed query could disagree. `Questions.withInternalVisibility` now removes only internal parts when hiding, resets an internal measure to Count, retains public selections and any valid per-unit choice, and the answer path defensively uses one sanitized question for its sentence, query and detail line.

## Verification

- Core artifact `12093435040`, digest `sha256:622ab3528f77885316f2b67301b503934ff5e02a2b0d4ad52ca12692ab33e5d6`: 1,773 tests in 350 suites, 0 failures, 0 errors, 0 skipped.
- All 9 `QuestionsTest` methods passed, including `turningOffHiddenDetailsRemovesOnlyInternalQuestionParts` and `staleHiddenSelectionsCannotDisagreeWithTheAnswer`.
- Android artifact `12092578601`, digest `sha256:a2c6e6b53b9f8e55eb03b2303f37631c39c8e3f2d7c94fdb54683a08e17c42df`: 38 methods in 14 suites, 0 failures, 0 errors, 0 skipped.
- Debug assembly and complete immediate-previous compiled TEST264 history validation passed.
- All 57 required PNGs decoded as 320×640 and were reviewed across four contact sheets. No blank or corrupt frame and no obvious clipping/overlap regression beyond intentional scroll content was found.
- APK artifact `12093171647`, digest `sha256:f36abd07421e078440e01de0d331ea349fbea24e3ab22abd59b84ba102183afd`, contains `ADHD-Pixel-Dungeon-CODEX-TEST-266.apk`, 38,592,446 bytes, raw SHA-256 `7ba0bf6d2d93fb2e4c7b27cf6caf718e3c74e960abfa0274d434c9869089674c`.

## Retained failures and supplemental evidence

TEST265 run [39010922142](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39010922142) failed closed before compilation because the history ledger omitted 11 paths. It produced no TEST265 APK. TEST266 is the unchanged-application retry after correcting the immediate-previous history review.

Optional display run [39011090883](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39011090883) passed short-landscape, small, compact and square profiles. Its M53-representative job timed out waiting for the native title condition and emitted a non-PNG placeholder; that supplemental result remains failed and is not relabeled green. The required 57-image primary gate passed.

Automatic stage run [39011090988](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39011090988) correctly refused both attempts because the preparation profile still pinned TEST264. It produced no staged artifact and no publication; that failure remains preserved.

## Independent staging

Independent staging run [39012540034](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39012540034) succeeded from 2026-10-17T07:08:51Z through 07:09:39Z at wrapper source `d5e3a87c0cb7ef128818973bac7fe890fc92c7de`. It reused the exact tested APK without rebuilding.

Staged preview artifact `12093094377` is 44,528,550 bytes with digest `sha256:629a784f55e062513fca5cf9b2d4f82d36e738f87942ee686661c754f905827a`; staged evidence artifact `12093755910` is 8,426,017 bytes with digest `sha256:1e3bfd3d660b0b36e0c040faa7d79311417a2f51a6121877cd55e695b3042336`.

Independent checks preserved raw APK SHA-256 `7ba0bf6d2d93fb2e4c7b27cf6caf718e3c74e960abfa0274d434c9869089674c`. APK signatures verified. Application ID `com.charltonchris29.adhdpixeldungeon.indev`, version `4.0.8-apd-INDEV-bC266-TEST`, and certificate SHA-256 `067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e` match the TEST262 install line, and the version code permits install-over. The owner Cleric-lore feature marker and exactly 57 required screenshots were verified. This is CI compatibility evidence, not a physical-phone or owner-save install-over result.

## Preserved behavior and limits

Raw history, derived caches, save formats, all 18 Custom Dungeon Rules, active 357-value v2.6R scoring and all 357 lookup values, original scores, gameplay, turns, RNG, all 50 Cleric lore entries, private Grudge enablement, opt-out and collection ledger are unchanged.

This is emulator/CI evidence, not physical-phone or owner-save install-over evidence. It does not claim the broader UIMSD audit or remaining gameplay backlog complete. No tester distribution or publication occurred. TEST266 is staged only, not published. TEST264 remains the preceding verified/staged fallback and TEST262 remains the latest actually published owner APK.
