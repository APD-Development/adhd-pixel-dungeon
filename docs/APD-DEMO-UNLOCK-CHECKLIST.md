> October 9 Codex correction: this is a historical checklist. Private builds load demoUnlock through ApdFeatures; normal builds keep it off. Adult humor now requires verified eligibility AND preference; the provider is currently unavailable, so only Clean is active. A retired or absent title prompt hides the arrow instead of restoring the drugs fallback. Current tests and limitations are in APD-SPRINT-REPORT-2026-10-09.md. Recorded abandonment currently counts as ended; older "unfinished" wording is disputed, not silently implemented. Physical-phone acceptance is still required.

# Demo Unlock: final checklist (built and tested; NOT active until the owner approves)

The whole feature is behind `UimsdUnlock.ENFORCED = false`. With it off, UIMSD is open for everyone, no pop-up or arrow text changes, and nothing here is visible. To activate: set it to `true`, build, and test on the phone with the steps below.

## What should happen (exact texts are in `windows/UimsdGate.java`)
1. **Fewer than 10 genuine runs.** The UIMSD button on the title screen is locked. Its arrow reads exactly: THIS BUTTON IS EFFECTIVELY USELESS TILL 10 RUNS HAVE BEEN COMPLETED.
2. **Tapping the locked button** (and only then) shows the interstitial-style pop-up (not an ad): title ARE YOU SURE ABOUT THIS?, your warning text, and two choices: I LIKE CONFUSION (generates the 10 demo runs and opens UIMSD) and I'LL PLAY 10 RUNS (closes it). It never appears at startup, onboarding or normal play.
3. **After the demo route is used:** the demo option is gone for good (permanently hidden), UIMSD stays open for good, and the demo data can be deleted later (with confirmation) without closing UIMSD again. Demo data lives in its own marked folder and is never counted in totals, achievements, progression, rankings, win rates, leaderboards or submissions.
4. **The guided tour stays LOCKED until 10 GENUINE runs.** Demo runs never count toward it, even if the player explored UIMSD through the demo. This survives restarts and demo deletion.
5. **After the 10th genuine run:** the run finishes completely first (death/ascension/results/save processing is never interrupted). The title-screen arrow then reads HEY, GO HERE FOR UNLIMITED DOPAMINE. (Clean, the default and what applies when age eligibility is unknown) or HEY, GO HERE FOR DRUGS. (only if the player chose Adult humor; the app never infers age). The arrow stays until the player opens UIMSD for the first time after that run, then retires permanently. It is shown even if the player explored UIMSD through demo runs. The tour becomes available. UIMSD is never opened automatically and navigation is never forced.
6. **Normal route preserved:** 10 genuine runs open UIMSD with no demo at all.

## Verified by automated tests (all passing locally and on the emulators when last run)
Counting (demo runs never count; imported/duplicate/unfinished runs do not count), tour locking, one-time prompt, restart persistence, demo isolation and deletion, both humor modes for the arrow, retirement on first open, hook order after the run is sealed (`UimsdUnlockTest`, `UimsdGateTest`, `UimsdWiringTest`, device `DemoUnlockDeviceTest`).

## Phone checklist for you (physical device; emulator success is not approval)
1. With the gate on and fewer than 10 real runs: the title arrow shows the locked text; tapping the button shows the pop-up with the exact wording and two choices; nothing like it appears anywhere else.
2. Choose I'LL PLAY 10 RUNS: back to the title, still locked, no repeat pop-up until you tap again.
3. Choose I LIKE CONFUSION: UIMSD opens with 10 demo runs; the demo button never returns; the guided tour is still locked.
4. Delete the demo data (UIMSD > Options > Demo data > "Delete all demo data..."): UIMSD stays open; the tour is still locked.
5. Finish your 10th real run (or use a fresh install and play 10 short real runs): the death/win screens finish normally; on returning to the title the arrow shows the dopamine text (Clean); with Adult humor on it shows the drugs text. Open UIMSD once: the arrow returns to its usual text and never comes back. The tour is now available.
6. Restart the app at each step and confirm nothing resets.

## Known edges (no action needed unless you want them changed)
- The flags live in the app settings, so clearing app data resets the feature (same as every other setting).
- Whether the app should hop to the title automatically after the 10th run is NOT built (the arrow appears the next time you reach the title). Say if you want the automatic hop.
- The one-time 18+ question for the humor preference is not built; the Adult setting is a plain checkbox in APD Settings (default off).
