# APD TEST39 private test report — October 10, 2026 (New York)

## Purpose and source
Urgent replacement addressing the reported navigation bar blocking hero selection, with the already approved saved customizable hotbar and a confirmed UIMSD tour lifecycle repair. This is private alpha development, not a public release. Source commit: 475c80c22285978bf9eefb682f7a19bd32d994e5 (navigation fix 60c30321e8442cef91d331780d0cc138fea3fe2b plus Android graphics API correction), following hotbar/tour commit 771f4f1511bedae33c9a1b5e6918688e6661c89d. Main and Claude's branch are preserved; commits descend from the real approved Codex branch.

## Highest-priority fix: hero selection and Android navigation
Phone evidence: the tester reports the OS navigation bar covers the six hero choices on Android 17 (CD1A.261005.003). The messaging preview alone initially did not prove application clipping; the subsequent explicit tester confirmation establishes a gameplay blocker.

Root causes in canonical source:
- AndroidPlatformSupport ignored navigation insets whenever the fullscreen preference was enabled. A request for immersive display does not establish that Android actually hid its bar.
- Portrait hero buttons extended into the blocked bottom inset and three pixels below the safe boundary.
- The bottom pointer blocker used camera WIDTH as its vertical coordinate.

Implementation:
Read actual visible navigation-bar insets on modern Android, with the older visible-system-inset path retained. Calculate how much of that region actually overlaps the game surface; do not subtract a navigation area already excluded from the viewport. Reserve navigation overlap in multiwindow as well. Keep entire portrait hero targets above the safe boundary, position Start relative to the corrected row, and fix the blocker coordinate. Existing class unlocks and Cleric restrictions remain intact.

Potential regressions and limitations:
Touches, card position, Start/difficulty spacing, portrait/landscape insets and multiwindow are relevant checks. Android 17 on the tester's hardware has not been exercised here. API34 emulator evidence and an injected navigation region do not substitute for that phone. Insets that an OS incorrectly reports, or bars appearing after a scene is laid out, may require further investigation if the phone still fails.

## Saved customizable hotbar
Classic SPD remains the initial mode and preserves per-run item assignments. Added Categories, Consumables first, Abilities first and Custom modes to APD Settings through Hotbar layout. Six positions may contain a category, a known specific item type, or Empty; repeated categories are supported. Custom preferences and optional Food in Healing persist between runs and app sessions.

Healing contains identified Healing potions; unknown potions remain unidentified in Potions. All potions includes known Healing when no Healing position is configured. Shielding remains a potion rather than being treated as healing. Active artifacts use existing interaction availability. Specific types never identify an unknown potion, scroll or ring merely by assigning it. Unavailable future item types keep their saved preference.

Narrow displays reuse SPD's existing three-slot swap mechanism to reach all six positions, retaining usable button sizes and search/wait/inventory controls. Category selection opens actual carried items and their normal item-action window. Opening/configuring slots spends no turn and does not cast/use an item. Class talents, armor ability and Tome spell selection retain normal actions, costs and targeting.

A prototype defect was caught during implementation: changing one preset position would lose the other preset positions. Copying the current preset before a slot edit fixes that; a retained regression verifies it.

Potential regressions:
Category layout visibility, swap access, inventory updates, target selection, interaction with attack/Cleric controls, and restoration of the old Classic layout need phone testing. This release does NOT implement the separate four saved Cleric quick-cast shortcuts; the existing Tome spell-selection menu remains.

## Confirmed UIMSD tour lifecycle repair
The background tour worker and model listener captured the old scene's dirty flag. If the scene was replaced, completion invalidated the retired scene, potentially leaving the current screen preparing or stale. Two controlled tests failed against the baseline. Render-thread callbacks now look up and invalidate the active scene when delivered, including a second replacement between queuing and delivery; leaving UIMSD is safe. Three retained regressions pass. Tour gates, real-run counts, sample generation and gameplay remain unchanged. Actual native/background/resume acceptance still needs a phone.

## Files changed
- core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/hotbar/CategoryHotbar.java (new)
- core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndCategoryHotbar.java (new)
- core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/SPDSettings.java
- core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/QuickSlotButton.java
- core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/Toolbar.java
- core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/APDSettingsPane.java
- core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/UimsdScene.java
- core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/HeroSelectScene.java
- android/src/main/java/com/shatteredpixel/shatteredpixeldungeon/android/AndroidPlatformSupport.java
- core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/hotbar/CategoryHotbarTest.java (new)
- core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/UimsdTourWorkerLifecycleTest.java (new)
- android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/PlayRunDeviceTest.java
- android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/RunSetupDifficultyDeviceTest.java
- docs/HANDOFF.md and docs/APD-CODEX-BUILD-REQUEST.txt
Private delivery adds this report and the existing artifact-only delivery workflow; it does not rebuild or modify the tested application.

## Tests and verification
Local Java11 compilation passed for all six hotbar sources and the modified hero scene. Three navigation-fix Java files passed syntax validation; syntax alone is not Android API verification. Eleven hotbar regressions cover defaults, preset edits, restart persistence, unknown identities, Healing/Food/Shielding classification, Classic preservation, dynamic inventory, quantities/health/RNG, invalid slots and absent future types. Together with three real asynchronous tour regressions and existing storage/settings checks, the latest local combined run passed 21 tests (0 failures), 0.285 seconds.

Setup failures were corrected: an incorrect GameSettings package import and an earlier WndUimsdText constructor mismatch prevented initial compilation. No failed setup run was counted as passing.

Native tests added: actual rendered category target bounds/control separation and Classic preservation within the existing play/save/resume test; visible navigation insets even with fullscreen requested, no double padding, partial/side overlap; and six complete rendered hero targets above an injected navigation region with a real pointer selecting Warrior.

TEST38 run38101304306 passed core tests but Android compilation failed at Graphics.getView(); corrected to the AndroidGraphics backend in commit475c80c22285978bf9eefb682f7a19bd32d994e5. It produced no APK and its native checks were skipped; it was never published.

Superseded source run 38100981859 (TEST37) completed full core checks and debug assembly, but its native job was canceled when the urgent navigation source superseded it. TEST37 is not represented as fully verified or delivered.

TEST39 exact-source workflow: 38101578800. Actual downloaded core XML: 1,033 tests, 0 failures/errors/skips. Actual downloaded Android XML: 11 tests, 0 failures/errors/skips, including real pointer hero selection and fullscreen/visible-navigation overlap assertions. Exact-source workflow completed successfully. APK checksum, signing comparison, app ID and source match are independently enforced by the appended publication verification. No physical-device testing or full normal dungeon completion has been performed for these changes.

## Data and distribution
No saves/statistics are deleted, reset or migrated. No application ID or signing key change is intended; publication requires the verifier's direct comparison to the actual TEST36 rollback. Install as an update over the existing private build. Do not uninstall or clear data. If Android refuses the update, preserve the existing installation and report the refusal.

TEST36 remains preserved with its original assets. No servers, community submission/voting/comment transmission, public distribution or iOS development were activated. iOS was explicitly deferred by the owner to prioritize this blocker.

## Unresolved and next phone checks
1. On the reporting Android17 phone, open Choose Your Hero with the navigation bar visible: all six cards must be fully visible and selectable; verify Start and difficulty controls.
2. Check portrait/landscape and gesture/three-button navigation on available phones. Check returning from another app.
3. APD Settings > Hotbar layout: try each preset, duplicate categories, a specific artifact plus all potions, Food preference, Empty slots and restart persistence.
4. Switch back to Classic and confirm previous assignments survive. Throw/use a wand and check targeting; search, wait, inventory and attack must remain accessible.
5. Open a UIMSD tour and switch apps/recreate the screen during preparation; it must finish on the current screen.

Four saved Cleric shortcuts, other approved unfinished backlog and actual Android17 device acceptance remain incomplete. Do not infer the full game is bug-free from this report.

## Honest timing and interruptions
Hotbar/tour checkpoint committed at approximately October10 9:11PM New York; the urgent navigation change committed around 9:16PM. GitHub timestamps and workflow evidence provide exact build/delivery start/end times in the appendix. Active development and idle totals were not independently metered, so no zero-downtime or overnight continuous-work claim is made. TEST37 native verification was intentionally superseded by the urgent blocker; its successful core/build work is retained as evidence, not substituted for TEST39 verification. Existing hourly automation metadata is not proof of continuous execution between tasks.


## Additional Bluetooth phone evidence
The owner clarified that turning Bluetooth off restored hero selection, and re-enabling it apparently kept selection working. This does not establish Bluetooth as the root cause or identify the connected peripheral. No Bluetooth disabling, permission or speculative controller behavior change was implemented. Investigated canonical controller/pointer paths and Android keyboard/configuration declarations. Asked which Bluetooth device was connected; owner does not know, possibly a watch or headphones. Do not assume a controller or keyboard. The navigation overlap repairs address confirmed unsafe layout, but this phone-specific correlation remains unresolved until actual hardware retesting/evidence.

Recorded explicit CI waits during this work: 215 seconds. This is not a measurement of total idle time.

Publication attempt38102268775: source/tests/signing/checksum validation and PDF staging succeeded, but release upload referenced TEST36 filenames instead of actual TEST39 files and failed before creating a release. Corrected only the publication filenames/title; existing source APK is reused without rebuilding. No prior release/assets were overwritten.
