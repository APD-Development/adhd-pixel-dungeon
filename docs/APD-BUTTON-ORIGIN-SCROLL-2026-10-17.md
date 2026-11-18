# Scroll from buttons and links, pending source candidate (2026-10-17)

**User report:** long pages refuse to scroll if a vertical swipe starts on an interactive button.

**Root cause confirmed in source:** PointerEvent dispatches listeners newest first. Many APD pages deliberately construct their ScrollPane first, then later construct child buttons so ordinary taps work. The later button consumes DOWN; ScrollPane never sees the original event, so it cannot recognize the drag. This is an input gesture ownership problem, not a graphics performance problem.

**Proposed bounded correction:**
- Opt-in `ScrollPane.allowDragFromButtons()` places the non-consuming scroll controller first only for APD Settings, About and Cleric Grudge.
- On drag threshold, marks the shared `PointerEvent.scrolled` flag so all `PointerArea` click targets (ordinary buttons **and** About's raw link hit areas) still release normally but cannot trigger their click action during a swipe.
- If a fast swipe ends before a drag frame, the release path recognizes the displacement.
- An unmoved tap still activates its button. Default ScrollPane ordering and all Custom Dungeon Rules horizontal/vertical slider arbitration, UIMSD row pickers and original SPD gestures are untouched.

**Regressions:** `ScrollClickSuppressionTest` verifies one scrolling pointer does not click, ordinary next pointer clicks, CANCEL does not click, and release handling is preserved. The existing Android `ApdSettingsClickDeviceTest.adultHumorControlWorksWithoutAnAgeProviderAndReopensWithSavedChoice` now checks a 112-screen-pixel upward swipe starting on the Adult humor checkbox scrolls, leaves the setting unchanged, then a separate deliberate tap toggles it, restoring the preference after test.

**Evidence status:** source-only candidate. No core tests, emulator checks, debug APK or physical-phone results yet. The core-only request expects 1,776 methods. Do not label implemented behavior verified until actual CI succeeds, and do not edit the executable APK request until a new application candidate genuinely requires the full build. Existing staged TEST268 and published TEST262 remain unchanged.
