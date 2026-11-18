# Fix log: interim phone report, steps 1-22 (private test build 4.0.8-apd-INDEV-bT1-TEST)

Nothing here has been run on a phone. Core tests (847, 0 failing) are the only verification. No new APK has been built.

## Fixed
### 1. Floor-1 start room with no exit (CRITICAL), 2. Main Menu greyed out (HIGH), 3. Difficulty picker missing (HIGH), 5. Guidebook intro repeats (MEDIUM)
- **Cause (one cause for all four):** SPD's first-run tutorial ("intro") was switched on. While it is on, SPD itself (a) hides the floor-1 entrance door (RegularPainter), (b) greys out Main Menu (WndGame), (c) re-shows the first guidebook page prompt until it is read (GameScene/EntranceRoom), and my difficulty bar followed SPD's options button, which is also hidden in the tutorial. SPD ends the tutorial only when the first guide page is read, or hero select opens with a FINISHED run on record. A tester with only abandoned runs (never finished one) never left it.
- **Evidence:** new `FirstFloorExitTest` generates floor 1 for 12 seeds: with the tutorial on, the stairs are not walkable from the start in 12/12; with it off, walkable in 12/12.
- **Change:** `ApdIntro.endIfNotNew()` (called when the app starts and when hero select opens) ends the tutorial for an install that has a finished run, 2+ saved games, or 2+ recorded runs. A real first-time player (no finished run, at most 1 save, at most 1 recorded run) still gets the tutorial unchanged.
- **Verified by:** `FirstFloorExitTest`, `ApdIntroTest` (rule), full suite.
- **Could go wrong:** (a) a first-time player who restarts after two attempts loses the tutorial; (b) a floor-1 run that was ALREADY generated in the tutorial keeps its hidden door: with the tutorial off the toolbar is back, so search (the magnifier / resting 5 turns) can reveal it, or abandon that run and start a new one; (c) the tutorial's no-hunger rule ends with it.

### 6. Grudge counters showed invented history (HIGH)
- **Cause:** the page printed the number of AUTHORED lines as "I'VE COMPLAINED ABOUT CLERIC 221 TIMES" / "APPRECIATED 124 TIMES" (the first two of four counters, from my earlier design). They are line counts, not events, but they read as history.
- **Change:** the page now shows only the two real "heard/witnessed" counts (distinct lines this player was actually shown). No totals. Nothing is invented or pre-filled.
- **Verified by:** `GrudgePageTest` (source check that the totals are not shown), full suite.
- **Could go wrong:** the two remaining counters read low (even 0) for a long time. If you meant to drop the heard counts too, say so.

### 7. Duplicate Adult Humor control (MEDIUM)
- **Cause:** two checkboxes (APD Settings and the Grudge page) wrote the same setting; one window stayed open over the other and showed a stale value.
- **Change:** the Grudge page no longer has a checkbox; it shows "Humor: Clean/Adult. Change it in APD Settings." One control remains (APD Settings).
- **Verified by:** `GrudgePageTest`.
- **Could go wrong:** none expected beyond people looking for the checkbox on the Grudge page.

### 9. Popup frequency (CHANGE REQUEST)
- **Finding:** already met by the design: the gap between popups is stored on the device and survives restarts; Rare = at least 3 days, Rarer (the default) = at least 10 days, Off = never. The "one per app session" wording was only my checklist's upper bound, not the real rate.
- **Change:** none to behaviour; `GrudgePageTest` now pins that every on-setting keeps at least 24 hours. If you want a longer minimum, tell me the numbers.

## Not fixed in this batch
### 4. UIMSD dialog lost after switching Android apps (MEDIUM)
SPD rebuilds the current scene when the app comes back (same path as rotating the phone), which closes dialogs and returns UIMSD to its table. Fixing it means saving and restoring UIMSD's screen/dialog state, which is a larger change to a screen that currently works; I did not want to risk it in a bricking-bug batch. Proposed next: restore the last table, scope and sort; dialogs reopen at their parent menu.

### 8. Grudge switches/buttons need individual testing (INVESTIGATE)
Preference storage for frequency and the five locations is covered by unit tests (`GrudgeSettingsTest`); the buttons themselves have only been exercised by unit tests, never by a finger. Needs your per-control phone test.

### 10. UIMSD counts and scope (INVESTIGATE)
No defect found, no data loss. Explanation from the code: "current rules" means the rules a new Warrior run would have; your earlier runs were abandoned (unfinished), which "current rules" does not count as finished; a Cleric run has its own rules (`cleric=playable`) so it only appears under All (mixed). Both are by design. Open question for you: should an abandoned run show in the default scope? (A wording/empty-state clarification is possible; I changed nothing.)

## Still needs your testing after the next APK
Steps 1-6 of the checklist again from a clean first launch (including: floor 1 start is open, Main Menu usable, difficulty picker visible), plus every Grudge control one by one.


## Update: Cleric Grudge / Lore postponed (owner decision)
Items 6, 7, 8 and 9 above all concern the Grudge page and popups. The whole feature is now off in every build behind one central switch (`ApdFeatures.GRUDGE_LORE_IN_THIS_RELEASE`, false): no Settings button, no page, popups, counters, first-Cleric-run warning, history reading or run-end/tick hooks. Everything is preserved so it can return at public release (items 6, 7 and 9's fixes are kept in the code). Item 8 is therefore moot for the alpha. Cleric stays playable. The Adult humor checkbox in APD Settings stays because Demo Unlock's arrow text uses the same preference.
Related issues: with the Grudge off, the first-Cleric-run warning cannot appear, so starting Cleric is a single tap (no extra window to get wrong).

## Update: floor-2 closed-start risk checked
I flagged that SPD hides the floor-2 entrance door until the Searching guide page is found. Headless check (`FirstFloorExitTest`, 12 seeds, tutorial over, no guide pages found): floor 1 walkable from the start 12/12, floor 2 walkable 12/12. So the risk did not reproduce here; no change made. (The test only measures walkability without searching; it is not a substitute for playing floor 2 on the phone.)
