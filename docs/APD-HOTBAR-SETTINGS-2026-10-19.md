# APD hotbar persistence and categorized Settings — TEST300

Acceptance verified2026-10-19. Developer/player TEST300 are delivered; no public release or tester distribution. The first full-native run retained one fixture failure, resolved by an exact-APK rerun as documented below.

## Source and builds
Application source: `d41631b3e5b43bf832036426a9e7c7a0694d07ed`. Preceding produced APK TEST299 remains preserved, although rejected for a Settings constructor crash. Player and developer variants must share this source, signing identity and bundled build history. Rejected TEST299 was not delivered. TEST300 is provided privately to the owner.

## Hotbar root causes and fix
1. `Dungeon.loadGame` restored serialized item `quickslotpos` values and depleted-item placeholders, then reapplied `WaterskinStartupSlot.applyToCurrentRun`. A startup preference could displace manually saved items or refill a deliberately empty/unassigned Waterskin position. The Settings picker also rearranged an active run. Remove both calls and the public reapplication helper; retain the existing `HeroClass.initHero` new-run initializer. The saved starting-slot value is unchanged and now explicitly described as new-runs-only.
2. Classic `QuickSlotButton.set` changed only the in-memory layout. A full process stop before the next ordinary game save could reload an older arrangement. Commit ready-boundary manual changes through `HotbarPersistence` and the existing safe `Dungeon.saveAll` path. Dead/unready/uninitialized heroes do not trigger this extra save; IO failures are reported visibly. No parallel hotbar save file or migration is introduced.

Controlled TEST285 instrumentation reproduced lost manual assignments and preference overrides. An upgrade preserves the actual old disk layout; it cannot reconstruct manual edits TEST285 never saved. Such a layout may need to be arranged once more. No positions are guessed or inferred from preferences.

Classic SPD, all category presets and Custom retain their existing storage. Custom categories/item-type identifiers continue to use the existing preferences; switching layout does not erase Classic assignments. Empty slots and depleted-stack placeholders retain their existing representations.

## Settings organization
Use a responsive category index and six focused pages: Gameplay; Difficulty & Rules; Hotbar & Controls; Visibility & Navigation; Statistics & History; Advanced. Compact gray pixel-art buttons and consistent labels replace the large red wall. Each setting explains immediate/new-run/restart timing. Both title-screen and in-run entry points use the shared pane.

Retain game speed, healing confirmation, adult humor, Healing-to-Shielding slider, all 18 Custom Rules and current-run read-only rules, five hotbar modes, custom slot configuration, food-in-Healing, Waterskin preference, Cleric quick casts, enhanced tiles, DoT panel, stair navigation and auto-entry, UIMSD, recording toggle, archive import/export, rankings import, self-test and existing developer/lore/demo tools with their previous gates. Do not change defaults or preference keys. Recording version/run identity/event counts/storage diagnostics are read lazily in Advanced.

The scroll host is created before its controls to preserve touch ordering, attached to the Window before camera-dependent layout, and reused across category changes. Hidden controls are inactive. Android Back returns to the index, then closes; category offsets and the current section/offset survive supported restoration. Long pages scroll and narrow widths remain bounded to the UI camera.

## Verification
| Gate | Verified result / source |
| --- | --- |
| Main build and core | [run39327169505](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39327169505):1,959 unique core tests /386 XML suites /0 failures/errors/skips. |
| Player build and core | [run39327169508](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39327169508):same application source,1,959 core tests with0 failures/errors/skips; developer feature asset absent. |
| Full native run |50 unique API34 methods:49 passed,1 compact gesture fixture failed;0 errors/skips. All48 prior TEST294 cases retained. Original report remains failed and is not relabeled. All73 required PNGs decoded. |
| Corrected retained gesture method | [run39330050220](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39330050220):1/1 passed against the exact produced developer APK, no application rebuild. Original swipe and non-activation assertions retained; added touch-origin and remaining-scroll-space assertions. Thus all50 unique native cases are verified across the full run and this corrected-fixture rerun. |
| Exact APK cold restart and player Settings | [run39327169493](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39327169493):TEST285 expected failure in both modes; five passing phases including upgrade with old disk state, corrected new save/restart and player Settings touches. Nine captures decoded and reviewed. |
| Five display profiles | [run39327169552](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39327169552):all5 passed;25 captures decoded. Two first-attempt failures show confirmed Quickstep launcher ANRs over APD. Raw logs/screenshots retained; unchanged APK retry passed. |
| Existing outcome fix | [run39327169520](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39327169520):exact TEST287 expected death-order failure; exact TEST300 passed without rebuilding either app. |
| Subsequent RoW investigation | [run39329677064](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39329677064):1,967 core tests /387 suites /0 failures/errors/skips after8 additional loot regressions; no production loot change. |

The native fixture's forced compact window originally called ScrollPane.setRect before Window.resize, leaving the content camera at the previous window position. Resize first, then lay out the scroll pane, matching production reflow. This fixed the test geometry, not application touch handling. The original50-method XML remains available (ZIP SHA25659b5842f6f6736f988cb7734f51ca21787d9ecd5c1a4b33c321d9cdd828add5c). Corrected gesture artifact ZIP SHA2563d870ab06b68deee4b5977c29beeb70b1f3b7c6cbc5f2cc02c3b5ad8e2386098. Its first successful test run had an unrelated screenshot-collection failure; that collection requirement was removed because all73 full-suite captures already exist, then the gate passed with assertions unchanged.

Cold artifact ZIP SHA256d624b49ed116538888037357913b8c5a17f02f96f5fdaf6e6160dbfbe6d68f95 includes raw expected baseline failures, all passing phases, exact developer/player hashes and the result JSON. Developer/player category index, focused controls, narrow wrapping and resumed Waterskin2/spikes5 were visually reviewed, alongside the equipment-window result.

Retain every prior core and native method. Core hotbar coverage uses real game files, a new QuickSlot and reopened preference files; exact Waterskin2/spikes5 plus additional items; conflicting/Automatic/Off startup settings; all five modes and custom identifiers; empty slots/depleted placeholders; actual manual UI commands reaching disk without a pause or explicit test save. It also checks unchanged HP/turn/RNG.

Cold-process instrumentation prepares five real saves on exact TEST285, force-stops the application, records baseline failures, installs the exact corrected signed APK without clearing data, checks the old persisted state, then creates corrected saves and force-stops/reopens them. Separate regular-player pointer checks exercise all categories, seven mode selections, custom slots, preferences, Back/restoration and a narrow scrolling screen. Full native suite retains UIMSD Columns, sort viewport, outcomes, recorded-window query/evidence and previous gameplay checks.

Rejected evidence is retained: TEST299 initial native compile rejected float-to-integer camera restoration in the test fixture; corrected instrumentation then exposed the real scroll-camera constructor crash. Cold fixture presentation also needed standard CONTINUE scene state, and upgrade assertions were corrected to use recorded old disk evidence rather than impossible recovery of unsaved edits. The checkbox swipe uses a compact viewport so it begins before the shorter Gameplay page's scroll limit; scroll and non-activation assertions remain unchanged. No production assertions were removed or tests skipped.

## Preservation, overhead and limits
No player-save, raw-log, outcome, archive or saved-query schema change; no UIMSD history migration or inferred historical-outcome overwrite. All previously compiled per-build history records are compared byte-for-byte, with the old index rows retained, and the two variants' history is identical. Previous APKs remain separate files. Player developer-feature flags remain absent.

Two ready-boundary manual commands: 13,723,744 ns aggregate, 3,328-byte synthetic game save; CI JVM only. This is a small synthetic CI measurement, not a physical-phone/deep-run latency guarantee. Developer APK38,712,060 bytes (+507 versus produced TEST299); player38,711,883 (+503). Compared with accepted TEST294, the combined milestone adds18,402/18,398 bytes respectively. No new event-recording schema or query-generated raw-log writes. This combined build also preserves the equipment-window UIMSD milestone and its explicit unknown/lower-bound semantics; the broader five-capability program remains partial/pending in the connected checklist.

API34 emulator verification is Android evidence, not physical-device verification. No user data was cleared; only isolated emulator fixtures were initialized, and no clearing occurs between the cold restart or upgrade phases.

## APK identity
Developer SHA256 `12687b32034edaeeac8d9bc9d62bda2ba74f17b65e3f30fc4f72a13d4a3ee26d`; player `33461124687e9a7633eaf074e27006d4b49206c0fd917f365e4b937afb989d55`. Both signer SHA256 `067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e`. All327 previous TEST299 history TSVs retained; each current variant has328 identical history TSVs, with prior index rows preserved. These are bundled build-history records, distinct from player UIMSD data, whose format is unchanged.

Gameplay cheats retain their existing test-build gates and are disabled in the player artifact. Existing INDEV synthetic-demo statistics controls retain their previous availability under Advanced; these do not grant gameplay items or modify real history.

## Changed files
Application/tests/docs since accepted TEST294, including the subsequent test-only RoW investigation and acceptance workflows:
- `.github/workflows/codex-hotbar-cold-restart.yml`
- `.github/workflows/codex-test299-acceptance.yml`
- `.github/workflows/codex-test299-death-regression.yml`
- `.github/workflows/codex-test299-full-native.yml`
- `.github/workflows/codex-test299-title-matrix.yml`
- `.github/workflows/codex-test300-settings-gesture.yml`
- `android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/ApdSettingsClickDeviceTest.java`
- `android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/CustomRulesNoticeDeviceTest.java`
- `android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/HotbarColdRestartDeviceTest.java`
- `android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/UimsdResearchDeviceTest.java`
- `core/src/main/assets/apd/apd_changes.txt`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Dungeon.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/hotbar/HotbarPersistence.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/hotbar/WaterskinStartupSlot.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/UimsdModel.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/research/EquipmentWindow.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/settings/ApdSettingsSection.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/QuickSlotButton.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/APDSettingsPane.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndAPDSettings.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndUimsdAsk.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndUimsdResearch.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndUimsdRow.java`
- `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/HotbarResumePersistenceTest.java`
- `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/RingOfWealthLootInvestigationTest.java`
- `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/UiEntryPointsTest.java`
- `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/EquipmentWindowTest.java`
- `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/SeedFloorsTest.java`
- `docs/APD-CODEX-BUILD-REQUEST.txt`
- `docs/APD-CODEX-CORE-TEST-REQUEST.txt`
- `docs/APD-HOTBAR-SETTINGS-2026-10-19.md`
- `docs/APD-NEXT-BUILD-HISTORY.json`
- `docs/APD-RING-WEALTH-INVESTIGATION-2026-10-19.md`
- `docs/APD-RING-WEALTH-SIMULATION-2026-10-19.txt`
- `docs/APD-UIMSD-EQUIPMENT-WINDOW-2026-10-19.md`
- `docs/APD-UIMSD-INVESTIGATION-CHECKLIST.md`
- `docs/APD-UIMSD-RESEARCH-ACCEPTANCE-2026-10-19.md`
- `docs/HANDOFF.md`
- `docs/WORK-COMPLETED-LEDGER.md`
- `tools/run-codex-android-checks.sh`
- `tools/run-hotbar-cold-restart.sh`
- `tools/run-test299-full-native.sh`
- `tools/run-test300-settings-gesture.sh`
