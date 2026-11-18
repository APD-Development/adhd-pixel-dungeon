# APD Settings Native-Touch Acceptance — 2026-10-17

## Outcome

The acceptance gap that allowed a visible settings page to reach alpha without complete touch coverage is now bounded by executable native tests.

- Existing application under test: TEST263, source `e3b5cef026f8a9d6827d0eebb5528069c646b00d`, producer run `38980948671`.
- Exact reused APK SHA-256: `3e71c975ddc7f85f22e1f42454aa9e1a420164f8c9ed6c7e226f11e40a08c20d`.
- Final test-only source: `d1d3e359fcec05f064714c30784cc6d189192902`.
- Application rebuilt: no.
- Executable build request edited: no.
- API-34 native result: 2 tests, 2 passed, 0 failures, 0 errors, 0 skips in 8.582 seconds.
- Physical phone: not tested.

## Defect in the prior acceptance process

The existing method `everyControlOfApdSettingsAnswersATouch` covered only an older subset of APD Settings. Its name overstated its scope. Five visible controls had no actual-touch execution in that method:

1. Healing to Shielding chance
2. Enhanced Tile Visibility
3. Automatic Stair Navigation
4. Healing confirmation
5. Custom Dungeon Rules

The new method opens the real APD Settings window, scrolls each control into view, sends real DOWN/UP pointer events, verifies the resulting preference/window change and restores all changed preferences.

## Permanent ownership guard

A reflection-based native test now enumerates every `OptionSlider`, `CheckBox` and `RedButton` field declared by `APDSettingsPane`. It requires the exact 19-field set to map to an executable `@Test` method:

| Fields | Native acceptance owner |
|---|---|
| optExotic, chkTileVisibility, chkStairNavigation, btnHealingConfirmation, btnCustomRules | remainingApdSettingsControlsRespondToActualTouches |
| chkRecord, btnTest, btnExportHistory, btnImportHistory, btnImport, btnUimsd | everyControlOfApdSettingsAnswersATouch |
| chkAdult, btnPlaybackSpeed, btnClericQuickCasts | adultHumorControlWorksWithoutAnAgeProviderAndReopensWithSavedChoice |
| btnCurrentCustomRules | CustomRulesNoticeDeviceTest.noticeStaysAboveUsableControlsAndDoesNotChangeLockedRunOrScores |
| btnGrudge | grudgeControlsRespondToActualTouches |
| btnDemoGenerate, btnDemoRemove, btnDemoView | DemoUnlockDeviceTest.theDemoUnlockMakesTenDemoRunsOpensUimsdForGoodAndItsButtonIsUsedUp |

Adding a future interactive field without assigning an explicit native acceptance owner now fails this manifest.

## Retained executions

| Run | Result |
|---|---|
| `38987653291` | Failed before tests because the emulator action split a multiline instrumentation command. Artifact `12082884529`, digest `sha256:d071820f580464defdee2958673bb89d9b4dd9af966f0651aea0315a6f151e20`. |
| `38988017361` | The 19-field manifest passed and all five native touches completed. The run failed an invalid direct-child slider-count assertion and a split validation heredoc. Artifact `12084300699`, digest `sha256:27da74a59f61a5ff9521ec556eaf6502d6b56a493ec80a4b1a1e90dab69350f2`. |
| `38988388102` | Both corrected methods passed: `OK (2 tests)`, 8.582 seconds. Artifact `12085125334`, digest `sha256:49a6decb59c41e9169b094e0154903cfffaef76c871e0adc809d0dc823055d34`. Overall workflow remained `cancelled` because qemu did not exit after the successful result and the 25-minute ceiling cancelled action cleanup. |

The final artifact contains `settings-touch-result.json`, `acceptance-source.json`, the raw instrumentation output and logcat. It explicitly records application reuse, no application rebuild and no physical-device claim.

## Status boundaries

This evidence closes the known native-touch ownership gap for APD Settings on the API-34 emulator. It does not change TEST263 application bytes and does not constitute physical-phone or owner-save install-over testing. TEST263 remains verified and staged, not published. TEST262 remains the latest actually published owner APK.
