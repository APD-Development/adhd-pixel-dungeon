# Phone checklist: everything changed since build 69 (RedMagic 11 Pro)

Built from the actual git history (`apd-build-69..HEAD`, 124 commits) and the docs. You last tested **B69**; nothing after it is assumed tested.
Mark each item PASS / BUG. "BUG" lists what would count. A crash or lost data anywhere is always a BUG: note what you tapped just before.

**Install notes.** Install the new APK OVER B69 (do not uninstall: it is signed with the same key, so your runs and statistics stay). Before you start, open Rankings/UIMSD and note your run count so you can confirm nothing was lost.

## What is ON in the private test APK vs OFF
| Feature | Test APK | Normal build |
|---|---|---|
| Cleric playable (un-ban) | ON | OFF (black "removed" screen) |
| Cleric Grudge / Lore (humor, popups, counters, first-Cleric-run warning) | OFF (postponed to public release; central switch `ApdFeatures.GRUDGE_LORE_IN_THIS_RELEASE`) | OFF |
| Demo Unlock gate (UIMSD locked until 10 genuine runs) | ON | OFF (UIMSD open) |
| "PRIVATE TEST BUILD" on the Grudge page; version ends `-TEST` | yes | no |
| Everything from builds 70-80 (Back fix, difficulty picker, Privacy menu, saved views, etc.) | ON (always on) | ON |
| Hero-select pips + win star | NOT BUILT (design awaiting your approval) | not built |
| Backend sending to a server | Exists but OFF by default; nothing can send without you switching it on and entering an address | same |
| Debug-only extras (run sheet > Queue for submission) | present (debug APK) | n/a |

## A. Critical: play and crash checks (do first)
1. **Launch + upgrade.** App opens to the title, no crash. Title looks as in B69. *Bug:* crash, blank title, missing buttons.
2. **Your data survived.** Rankings / UIMSD Runs show your earlier runs. *Bug:* runs missing or counts lower than before.
3. **Warrior full start.** Pick Warrior, Normal difficulty, Start. Walk ~30 turns, fight something, pick up an item, descend one floor. *Bug:* crash, stuck turn, freeze, wrong item.
4. **Save/continue.** Close the app mid-run (swipe away), reopen, Continue. *Bug:* run lost or different.
5. **Die once, win never required.** Let the run end. Death screen appears, then Rankings. *Bug:* crash on the death/results screens, or the run not listed.
6. **Another class** (e.g. Mage): start, 10 turns, back out. *Bug:* crash.

## B. Android Back (build 70)
7. On the **character screen**: open a hero info dialog, press Back → only the dialog closes. Press Back again with a hero picked → leaves toward the title, never closes the app.
8. Back from each title button: Rankings, Journal, About, Changes, News → each returns to the title in one press.
9. In UIMSD: Home → a table → Options → a submenu → Back, Back, Back: each goes to the previous place. The tour: first Back ends it. *Bug:* app closes, jumps straight to the title, or needs two presses.
10. Back from APD Settings and from Ask-a-question table (returns to its answer).

## C. Difficulty selection (build 70)
11. Character screen: a 1-5 picker above Start; tapping changes the label. Start a run at 4, then change the picker later: the started run keeps 4 (check in UIMSD Runs, "Difficulty"). Normal (3) plays exactly like before.
12. APD Settings has **no** Difficulty slider. *Bug:* slider still there, or run uses the wrong level.

## D. Cleric playable (TEST APK ONLY)
13. Character screen: Cleric is selectable (all classes are unlocked in debug builds). Tap Start with Cleric: **no black "removed" screen**. *Bug:* the black screen appears.
14. **No Grudge warning:** the first-Cleric-run warning is part of the postponed Grudge feature, so Cleric must start straight away with nothing extra. *Bug:* any warning or joke popup appears.
15. Play 20+ turns as Cleric: cast a spell, use an ability. Cleric plays exactly as in vanilla (no balance changes). *Bug:* crash, missing spells.
16. Die/quit the Cleric run. In UIMSD → Runs: the run shows with its class; **older runs still appear under their old rules** (Options > rules/scope: your earlier runs are not mixed with the Cleric run). *Bug:* old runs disappear or merge into the Cleric ruleset.
17. Your old non-Cleric stats/win counts are unchanged from step 2.

## E. Cleric Grudge (POSTPONED: skip this whole section in the next APK; none of it is available)
18. Settings → APD → **About / Developer: The Grudge**. The page title says "(PRIVATE TEST BUILD)". See the four counters, frequency (Off/Rarer/Rare), five location switches (startup, main menu, during runs, after a run, statistics; defaults on/on/on/on/off), the Adult checkbox, reset.
19. Toggle each location; leave and return: the choices persist. Frequency Off: nothing ever appears.
20. Play normally for a while. At most **one** unsolicited popup per app session; none in the first ~20 minutes after install; never mid-fight, on a boss floor, or over an open window. Each popup has **Fine.** and **Turn off these messages** (tap it: all grudge messages stop at once; check the page shows it off).
21. Text voice: first person (I/me/my), Cleric the only target, no invented statistics. *Bug:* "we/our/the developer", jokes at players who like Cleric, claimed patch notes/stats, anything shown while Cleric is not the topic, a line that is cut off or shows `{braces}`.
22. Adult checkbox off (default): all lines are Clean. On: some lines are edgier. *Bug:* edgy text while Adult is off.

## F. Demo Unlock (TEST APK ONLY)
23. (Fewer than 10 real runs.) Title arrow reads exactly: THIS BUTTON IS EFFECTIVELY USELESS TILL 10 RUNS HAVE BEEN COMPLETED. *Bug:* wrong text; or if you already have 10+ real runs, UIMSD should simply be open and no lock text (that is correct).
24. Tap the locked UIMSD button: a pop-up ARE YOU SURE ABOUT THIS? with **I LIKE CONFUSION** and **I'LL PLAY 10 RUNS**. It must not appear at startup or in normal play.
25. I'LL PLAY 10 RUNS: back to title, still locked, no repeat until you tap again.
26. I LIKE CONFUSION: UIMSD opens with 10 demo runs; the demo option never returns; the guided tour stays locked; demo runs are not counted in your Rankings/totals.
27. UIMSD > Options > Demo data > Delete all demo data (confirm): UIMSD stays open, tour still locked.
28. 10th real run: finish it; death/win screens complete normally; back at the title the arrow reads HEY, GO HERE FOR UNLIMITED DOPAMINE. (Clean; DRUGS text only with Adult on). Open UIMSD once: arrow returns to normal and never comes back; the tour is now available. Restart the app between steps: nothing resets.

## G. UIMSD and statistics (builds 70-80)
29. UIMSD opens (if not locked), Home shows. **Saved views:** Options > Saved views: save a view, reopen, delete it.
30. **Arrange the columns:** Options > Arrange the columns: reorder, it persists after leaving.
31. **New tables/columns:** Bosses table (finished runs only); Runs columns "Run type" and "Modded".
32. Sorting a column works and the row order matches (earlier sorting bug).
33. Telemetry additions (hero state per turn, landmarks, keys, secrets, wells, journal pages, quest steps): just play; *Bug:* slowdown, stutter each turn, big storage jump (check the storage meter in APD Settings stays sensible).
34. Storage meter + archive export/import (APD Settings): export, then import the export. *Bug:* import fails or changes existing runs.

## H. Options > Privacy / backend (builds 73-80; nothing leaves the phone by itself)
35. UIMSD > Options > **Privacy**: rows: what stays on this device, sending (shows **Off**), address, Send now, Evidence requests (0), share report, account, export/delete account, Community votes.
36. Turn sending on, leave the address empty, press Send now: it says there is no usable address. Nothing else happens. Turn sending back off.
37. "What stays on this device" text is readable and makes sense.
38. *Bug:* any network prompt, any "sent" claim, crashes in these screens.

## I. Smaller changes
39. Sad ghost (quest NPC) still gives and reveals its quest normally; Wandmaker, Blacksmith, Imp quests normal.
40. APD Settings: Adult/Clean humor checkbox exists (default off).
41. Debug-only (this APK is a debug build): run sheet in UIMSD has Queue for submission / Show what is queued / Take this run out of the queue; they only touch a local list.

## Still OFF / unavailable (do not expect these)
Hero-select pips + win star (awaiting your approval); any server/backend; the Cleric un-ban, Grudge and Demo Unlock in a normal build; clan/community features; an automatic jump to the title after the 10th run.
