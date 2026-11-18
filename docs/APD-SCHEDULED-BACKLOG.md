# APD scheduled work backlog

Owner schedule (set Nov 17, 2026): hourly coding runs 6 AM-2 PM Eastern, one APK a day started at 3 PM and delivered by 4 PM.
Hourly runs work top-down, commit and push to `fix/apd-trap-room-search-20261116`, and never edit
`.github/workflows/apd488t-trap-room-search-apk.yml` (that file triggers the APK build; only the 3 PM run touches it).
Chris, Nov 18: ChatGPT is not touching this branch. Fix real bugs a test finds right away. When a decision comes up,
make the reasonable call yourself, list it in that run's summary, and revert it if Chris later says no. Still no big
unapproved features. Each run codes for at least 30 minutes (Chris, Nov 18).

## Approved, in order
1. Tests for this week's features:
   - [x] room-wide Search tap (Nov 18, 6 AM) (`SearchControlContext.roomHasSearchableWall`, `fullSearchUseful`)
   - [x] `Hunger.turnsUntilHungry` / `perTurn` (Nov 18, 6 AM; rate<=0 case not forced) (Salt Cube, custom hunger rule, Hungry, Starving, never)
   - [x] hunger text in the status tab and top-right (values, yellow at 300, red at 450)
   - [x] Bug Notes inside both report formats (Standard path; Extended ZIP not unit-tested); Standard report byte-identical when no notes
   - [x] Bug Note button Developer-only
   - [x] heavy armor in the real `Armor.info()` (identified exact, unidentified +0, no level leak)
   - [x] every food's "Refills N hunger" equals what eating removes (incl. On Diet)
   - [x] Gemstone Dust apply/recipe on real rings; trophy outline and Bestiary line; Amulet layout reveal on a real level
2. [x] (Nov 18 7 AM batch; GenerationTracker covers draw counting only) Tests for the 17 untested APD classes: ApdFacts, BuiltinFactKinds, CustomBossHealthHooks, CustomDefenseHooks,
   DewExplanation, EventBuilder, GameSnapshot, GenerationTracker, HotbarPersistence, KeyExplanation, Mailbox,
   PositionedStringLookup, ProgressGuardInputStream, ReplayScan, StaffExplanation, StringLookup, Utf8.
3. (Nov 18 7 AM: traps (text, saving, gas/weakening activation), blobs, Bestiary mobs, every item/buff/creature/badge/talent/class text, all recipes, floor walkability 1-24 incl. all challenges, Remove Curse, wands, rings, armor/weapon strength, hero basics, dew math done. Nov 18 8 AM: floor contents placement 1-24 (mobs, heaps, traps, room overlap), thrown potions' clouds, exotic potion/scroll twins, scroll-to-stone, tipped darts, spell targets, random enchant/glyph/curse, loot tables done. Next: drink effects that need a hero sprite, remaining spell behavior) Behavior tests for untested SPD gameplay classes: traps, rooms (valid generation on every floor), mobs, spells,
   scrolls, blobs.
4. Construction smoke tests for sprites, windows and UI classes where the headless runner allows.

## Waiting on Chris (do not build)
- Tracked voluntary rules / conducts (said "not yet").
- Gemstone Dust custom sprite and alchemy-guide entry (offered, not approved).
- Amulet reveal opening secret doors (currently map only).
- Stronger enemies / better loot for trophy tiers (undecided).

## Mid-day test runs
The source-only workflow `apd-extended-bug-report-ci.yml` can be dispatched on this branch (`gh workflow run ... --ref`) to run the full core suite without building an APK.
