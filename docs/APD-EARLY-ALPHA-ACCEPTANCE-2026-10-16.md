# APD early-alpha acceptance — October 16, 2026

Private tester preparation, not authorization to distribute automatically. The target remains Friday at 4 p.m. New York time. TEST249 enables provisional experimental Custom Rules scoring for new runs; no competitively calibrated formula is claimed.

## Current evidence boundary

TEST249 source37a8f6d16cd497e13ed581e887ed9d4858fe97aa passed1729 core/34 API34 methods and44 screenshot checks. Separate six-class locked-rule/scoring save acceptance passed in run38918464322. The published fallback is TEST231, not the earlier checkpoints below. Physical-device checks remain open.

## Historical evidence checkpoints

Published owner fallback: TEST224 application source `3e886234e69ab128a861cfbc17bca549ba3a3456`, verification run `38823705331`. Independently checked 1,676 unique core methods in330 suites and32 API34 methods, zero failures/errors/skips. Debug assembly, compiled history, all40 screenshot reviews, signing/application-ID compatibility and SHA-256 checks passed. One distinct supplemental native-navigation method passed against the same APK in run38826006010; do not add32+33. Private owner release: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/tag/apd-alpha-preview-224 . No automatic tester distribution.

Next candidate TEST226: application source `a6e2f5e9aafb370ced629db01be300f6fab3f31e`, full verification run `38827685570`. It includes two archive integrity fixes and permanent native release-navigation coverage. Expected1,683core/33API34/42images are requirements, not completed results until the exact run succeeds and evidence is inspected. The separate exact-original/corrected acceptance already reproduced six failures plus one clean control on224 and passed79 targeted methods after correction. Staging must reuse the exact226 artifact only after full success and directly compare compatibility against published224; failures retain224.

The four successful Android16 targeted checks and two cold-restart phases exercised the exact existing TEST222 application, not TEST224. The four targeted methods repeat existing acceptance methods on another OS; do not add them to a unique-method count. The earlier cold-restart screenshots were obscured by Android's first-use fullscreen tip. Reused-APK run38824287616 passed both force-stop phases; artifact12014606742 (SHA-256 bdfe10d25bd297d90e3487dbca9bb48c6896899f71687a50ea7a4dcf0e0b16e7) was independently checked, and the unobstructed About capture was visually reviewed. No application rebuild was performed.

All emulator evidence remains separate from actual RedMagic/Pixel hardware and the owner's existing save. An absence of test failures is not a guarantee of no game-breaking bugs.

## Installation and preservation

- Use the signed-compatible private APK only after its source, package, certificate and SHA-256 are recorded. Install over APD; never uninstall or clear app data.
- Confirm existing run slots, rankings and UIMSD history remain present before starting a new test run. A real history export can be retained as a backup; never erase the original to test recovery.
- Test recovery/corruption scenarios only on disposable fixture copies, never the owner's working save.

## Physical-device checks still required

1. Open Custom Dungeon Rules on a small portrait display. Read the experimental/scoring notice; reach all18 controls. Vertical and diagonal scrolling must not accidentally change values; horizontal drags and taps must change only the chosen control.
2. Verify saved preferences reopen exactly, including every whole upgrade-scroll count1–60. Starting a run locks its actual configuration; changing next-run preferences must not change the existing run, score or recorded rules.
3. At neutral100% and15 upgrade scrolls, check ordinary play and compare familiar SPD mechanics. Non-neutral tests should include high enemy HP/defense plus low upgrades; high damage plus low player HP/healing; spawn/hunger combinations; boss HP plus low player damage; and extra upgrades combined with stronger enemies.
4. Exercise Warrior, Mage, Rogue, Huntress, Duelist and Cleric attacks/abilities. Check direct, spell, summoned, explosive and persistent damage sources without double scaling. Boss Damage replaces Enemy Damage; unowned traps, starvation and Chalice self-damage retain their own rules.
5. Play all five boss encounters with customized HP/damage. Mandatory phases, invulnerability, summons/pylons and progression must still work. Existing automated boundary/phase tests are not a substitute for five complete human-played boss fights.
6. Save/restart/continue an ongoing customized run, including active damage effects and a damaged hero. Check its original seed, class, difficulty, challenges and selected rules afterward.
7. In UIMSD, inspect the exact recorded configuration and actual item generation counts. Try filters, comparisons and exports. Historical unknown settings must stay unknown; historical earned scores remain unchanged. New version2 runs must show their frozen provisional multiplier and adjusted score; legacy runs retain their recorded scoring authority.
8. Enable Automatic Stair Navigation, find its compact control, and navigate only to already discovered stairs. Normal turns/risk apply; visible enemies, damage, detected hazards, blockage and manual cancellation interrupt travel. Reaching stairs must not automatically change floors. Hidden traps retain normal exploration risk.
9. Compare Enhanced Tile Visibility on/off at different zoom levels. It must not expose unexplored terrain or obscure creatures, targeting and fog of war.
10. Check Cleric slots2–4 by tapping an empty slot, assigning a spell, casting, cancelling and reopening. Confirm optional first-Cleric lore works without changing Cleric balance or exposing future private content.
11. Check Blacksmith reward upgrade labels, the Chalice damage preview and Healing-to-Shielding settings wording. Other potion/scroll conversion behavior must remain normal.
12. With Adult Humor off, inspect About and applicable authored content, restart and verify the preference remains off. Adult originals should return only when enabled; player-entered text is not censored.
13. Open APD Changes: the default is one concise APD Alpha Preview with categorized native icons. Open each popup, scroll, use Versions and Back. Optional technical records must preserve exact historical entries and honest gaps. News remains separate and works offline.
14. Open Top Scores and existing run details/score breakdowns. Preserve historical rankings. Check0.5×/1×/2× playback changes presentation rather than turn costs or damage.

## Reporting a failure

Record the exact APK/version, device/Android version, class, seed, floor, difficulty/challenges/custom configuration, steps and screenshot. Mark whether it is reproducible. Preserve the run and export evidence where possible; do not alter gameplay or player data merely to make a test pass.

## Release decision

Do not distribute to the approximately10 testers automatically. The owner approves distribution after reviewing the actual staged APK/report and remaining physical-device limitations. Do not claim the broader UIMSD audit, unconfirmed sentry/audio investigation, competitive scoring calibration or future online systems are complete.


## TEST226 verified staging update
Exactsourcea6e2f5e9aafb370ced629db01be300f6fab3f31e/run38827685570 passed1683core+33API34,0fail/error/skip,debug and fullhistory checks. All42valid screenshots were independently reviewed; newdefault/reopenedpreview and originalSPDreference were viewed full resolution. Stage38827870744 directly verified signing/applicationID/version/checksum compatibility against actuallypublished224. CandidateAPK SHA2569a93691ca9417b4f3dd63c3724ba4702d5df10cca747f3db03dd67841fee3bd6; PDF SHA4ee08d9cfc1b36dd40f9144e74ce51df399a9db1c7ba808595ba7e450eddb127. These completed results supersede the earlier pending statuses. Ownerpublication remains pending until a real release is confirmed. All physical-device checks and distribution approval above remain open.


## TEST226 actually delivered privately — 2026-10-15 8:26:30 p.m. EDT
Release https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/tag/apd-alpha-preview-226 was confirmed through actual GitHub release metadata at2026-10-16T00:26:30Z. Publication-only run38829450668 succeeded at real wrapperb71ee501f810ab2a9fce40b75d9e418a3d546d7c; application tree matches testedsourcea6e2f5e9aafb370ced629db01be300f6fab3f31e. No APK rebuild or public/tester distribution. APK38583318bytes/GitHub SHA2569a93691ca9417b4f3dd63c3724ba4702d5df10cca747f3db03dd67841fee3bd6, PDF17902bytes/SHA4ee08d9cfc1b36dd40f9144e74ce51df399a9db1c7ba808595ba7e450eddb127. Direct download filenames ADHD-Pixel-Dungeon-Alpha-Preview-226.apk and APD-Alpha-Preview-226.pdf; original internal226 trace preserved in receipt. SourceZIP4435905bytes/SHAce9211f89f0805c0b22d0b2e116228873482f357f129b10948dceaf65eea41a3. All old assets including224 remain intact. Install over, never uninstall/clear data. No physical-phone/owner-save acceptance, complete UIMSD audit or unconfirmed sentry/audio fix claimed. This actual delivery supersedes earlier pending publication statuses. Only two reproduced archive fixes were added; not a claim that the full backlog is complete.
