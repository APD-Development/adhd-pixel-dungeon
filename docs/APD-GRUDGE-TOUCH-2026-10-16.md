# Grudge settings touch regression — 2026-10-16 (primary gate verified; supplemental reconciliation pending)
Owner screenshot shows Grudge open over APD Settings; frequency, location checkboxes and history confirmation did not respond to taps. This report does not assert a failure of Back or window dismissal.

## Supported cause
WndClericGrudge registered all its interactive controls before constructing ScrollPane. ScrollPane creates a full-area PointerController, which then receives the first touch and swallows clicks; the earlier settings pane WndAPDSettings explicitly fixed the same order. No evidence indicates a preference-storage, lore selection or UI-rendering failure.

## Bounded correction
Construct the ScrollPane directly after the empty content Component, BEFORE any RedButton/CheckBox is created. Fill and size content exactly as before, then resize/window/place the already-constructed pane in the original order. No changes to Grudge frequency rules, location defaults, humor text, message counters, reset semantics, application ID, saves, history, scoring, RNG or combat.

## Regression and safe test operation
ApdSettingsClickDeviceTest.grudgeControlsRespondToActualTouches opens APD Settings, actually taps the Grudge entry, taps and checks a persisted frequency change, taps and checks the Game startup checkbox, and taps the history-reset control only far enough to show the confirmation; it never accepts the reset. Both changed preferences are restored in a finally block. Core Grudge preferences and prior full-suite tests remain required. Full Android38, core1761, debug/history, existing57 required images and supplemental displays are PENDING until real execution.

Attribution: first-alpha owner screenshot, no physical owner-device reproduction by an automated test; source-level root cause established from the input listener creation order. Current published fallback TEST262. No TEST263 APK is verified or published by this document.

## Executed verification — 2026-10-16T23:42:01Z
Exact source `e3b5cef026f8a9d6827d0eebb5528069c646b00d` / run `38980948671` passed 1,761 core methods and 38 required API-34 emulator methods with zero failures, errors or skips, plus debug assembly and complete immediate-previous TEST262 compiled history. The actual-touch Grudge method passed and restores the frequency/startup preferences in its finally block without accepting the reset. All 57 required 320×640 screenshots decoded and were contact-sheet reviewed. APK is 38,591,540 bytes, SHA-256 `3e71c975ddc7f85f22e1f42454aa9e1a420164f8c9ed6c7e226f11e40a08c20d`.

Supplemental display run `38980948747` separately retained three passes and two failures: square timed out waiting for TitleScene; short-landscape reported an overlapping title control. Failed-only retry was requested against the same APK. Staging `38980948665` separately failed because its profile still expected 37 methods; the profile/manifest correction pins this exact source and requests 38 without rebuilding. No physical-phone, owner-save install-over, staging, delivery or publication claim. TEST262 remains the latest published fallback.

## Terminal acceptance — 2026-10-16T23:48:13Z
Staging run `38982859447` succeeded from 23:42:46Z through 23:43:30Z without rebuilding, validating the exact TEST263 APK, 1,761 core/38 Android XML, signing/application identity, private Cleric flags, 57 required images, source archive and PDF. Supplemental failed-only display attempt2 `38980948747` succeeded at 23:46:22Z for both square720×720 and short-landscape960×320; required frames decoded and were visually reviewed. Original attempt failures remain preserved. TEST263 is verified and staged only, not published; TEST262 is still the latest published owner APK.
