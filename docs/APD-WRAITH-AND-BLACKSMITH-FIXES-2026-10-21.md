# APD two reported bugs: Wraith combat targeting and Blacksmith return stairs

**Owner instruction:** both reported bugs must be fixed, not just investigated. Preserve previous paired TEST303 APKs and game data. This is the source checkpoint for next uniquely numbered developer and regular-player pair.

## Wraith cannot be damaged or examined
Discord `#bug-reports` shows visible Wraiths that are not attackable, missiles strike the floor without reporting a miss, bombs and burning have no effect and Examine gives no mob panel. Yet Scroll of Dread removes them. Underlying code reads `Actor.chars` for all cell-targeting combat, spells, environmental fire, bombs and Examine, but fear/Dread also traverses `Dungeon.level.mobs`. A live Wraith remaining in `level.mobs` but missing from `Actor.chars` therefore explains this exact symptom pattern. The normal Wraith still has one HP and intended high evasion, so simply lowering defense or increasing HP would mask symptoms and change gameplay.

Production fix `649ca3eb626ee0fb85fc6fb2e9caae30222fd2bc` (guard corrected `137f2ffaf5cc1d4b7fee331c9439bd2fc15e24e1`) gives `Actor.findChar` a tightly scoped recovery only when the game is actively displaying a live dungeon. The exact existing living Wraith/TormentedSpirit must be in the current level's authoritative mob collection and no registered character may occupy that tile. Register this SAME monster in the normal scheduler, with unchanged location, HP/HT, attack/evasion, buffs, loot and RNG, never create a duplicate or revive dead mobs. Headless/non-playing contexts are not touched. Six new `WraithActorRegistrationTest` methods check same actor reference, idempotence, player damageability, alt Spirit, false contexts, non-Wraiths, occupied cells and dead mobs.

First regression run `39540536252` compiled 2025 methods: all six new Wraith tests passed but 61 previous headless tests failed because the new `Game.scene()` check dereferenced a null `Game.instance`. Corrected with explicit null guard, and rerun `39540941365` is authoritative for complete core acceptance. Do not claim actual user-phone reproduction or Android acceptance until tested.

## Blacksmith quest: no misleading Descend
Production source `f6c0552e31fc23853521d36b90723eeac8e3cef4` made the quest's branch-entrance transition a single **Return to Main Dungeon** route rather than a nonfunctional Descend. Six regressions verify branch route, safe path, no discovery leakage, normal main-floor up/down routes, exactly-once auto-entry, and changing exit identity. Completed core run `39506031984` **2019/2019 tests, zero failures, errors or skips**. This production code has not yet appeared in a delivered APK.

## Protection and delivery
Preserve all existing APK history files and the immediate produced TEST303 owner pair (producer source `7505a36ae1567458ba854db9427196a893824806`, GitHub run `39352224211`, APK SHA256 `a7a665f79d1e6376c790ea840da7ccf9a83040f4d714263625dcbdc8899795e7`, TEST303 player `91de724cd14c40585ce20477094907c102702dd4e431f9b195605ce85357391a`). Next build MUST include both developer and regular-player APKs from the exact same source and preserve 331 previous history entries, application ID, signing, all 18 Custom Dungeon Rules and active scoring, legacy saves and permanent UIMSD raw history. Do not stage/publish/distribute to other users. Native Android and actual-device acceptance remain separate from core tests.


## TEST304 private candidate verification — 2026-10-21

Exact application source `b5faf8f951001489f7004a507efbf37df6e9e070` produced both variants. Run `39541268516` passed 2,025 core methods in 394 suites and 53 API34 methods with zero failures/errors/skips. Matching-player run `39541268552` repeated 2,025 core methods and verified identical application identity, signer and compiled history while excluding developer-only flags.

- Developer: `ADHD-Pixel-Dungeon-CODEX-TEST-304.apk`, 38,731,548 bytes, SHA-256 `284739d810a341c6fe920c3acb191d9e8bb018b9353e415c5f38923ff0f76de6`, artifact `12319648800`.
- Player: `APD-Player-TEST304.apk`, 38,731,375 bytes, SHA-256 `85613951baed76829f36009020d388efafd6996d012c69f7eae71f5a58830f8e`, artifact `12319383278`.
- History: all 330 indexed TEST303 records are byte-identical in TEST304; its own record raises the index to 331, with 332 TSV assets including `index.tsv`. Developer/player histories match.
- Displays: original `39541268481` retained a square-profile failure caused by Android's Quickstep ANR dialog covering the rendered app. Harness commit `dd8f55b355e036facc9d532173395cfa9585cf9d` prevents those system dialogs and supports evidence-only reuse of an ancestral verified APK. Run `39614034598` passed 320×480, 360×560, 720×720, 960×320 and 1080×2408 against the unchanged APK; the square artifact proves application source/hash, five valid PNGs and a clean native test.
- Publication: staging `39541268428` failed closed on mismatched preparation/application inputs. No stage, release, tester distribution or physical-device claim. TEST262, published 2026-10-16T23:14:03Z, remains the latest actual owner publication; TEST303 remains the immediate private fallback.

Core tests demonstrate the same living Wraith/TormentedSpirit is re-registered without duplication/stat/RNG changes and that Blacksmith quest exits route safely back to the main dungeon. Actual owner-phone install-over and direct reporter gameplay remain separate pending evidence.
