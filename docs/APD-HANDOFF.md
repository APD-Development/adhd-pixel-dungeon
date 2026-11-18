# APD handoff (read first in a fresh session; keep this short and current)

Project: private fork of Shattered Pixel Dungeon 4.0.8 = "ADHD Pixel Dungeon" (APD). Repo `charltonchris29/ShieldingShouldHaveWorked`.
Branches: `main` (every push builds an APK release `apd-build-<run number>`) and `claude/adhd-pixel-dungeon-setup-iszc3v` (push both).
Local tests: `./gradlew -p <scratch>/jv :core:test` (see the session's scratchpad); Android code only compiles/tests in CI.
CI: "Build APK" ~3 min, "Android device tests" ~4-5 min (emulators API 26 = minimum, API 34). `.github/ci/run-device-tests.sh` streams
logcat tags APDMIG/APDPERF into the job log and uploads logcat artifacts.

## Status
- **Phase 9 (exact healing + measured optimisation): COMPLETE and verified; waiting for the user's approval before anything else.**
  Verified on commit a19c9c00e = **apd-build-36**: full core suite 501 tests green; device tests green on API 26 and API 34
  (8 tests each, incl. the migration test with heals and UimsdDeviceTimingTest).
- **Home/Guide milestone: COMPLETE, awaiting approval.** Verified on commit 80ef188f8 = **apd-build-37**: core 509 tests green; device tests green API 26 + 34.
- Stability pass (CI green): per-run balance revision persisted/used everywhere; UIMSD-from-game cannot start a floor replay; seed floors refuse imported/unrecorded-challenge/unknown-revision runs. Product notes: none needed. New phone check: none.
- Stability pass 2 (CI green, superseded by the integrity audit below): Catalog synchronized (real race shown by test), reports from demo/sample data carry a SYNTHETIC notice, unfinished-comparison hidden-info regression test. No new phone checks.
- Previous PHONE-CHECK build: apd-build-60 = commit 3a35441a7 (integrity audit; core tests green, device tests green API 26 + 34; replaces build 57). No new phone checks.
- **NEWEST BUILD = apd-build-80 (9447a5e; core tests + device run 110 green on 26+34; Demo Unlock gate OFF). Run 79 (933d472) failed only on a test-ordering bug in UimsdUnlockTest, fixed in 9447a5e, so there is no apd-build-79 release (main 3977dd0d3; device run 102 green on API 26 + 34, Build APK 78 green)**: everything in 76 PLUS Options > Privacy: account export/delete requests, Community votes, shareable report, evidence inbox pruning, redirect hardening. (Build 77 = 22b91c8.) The UIMSD Demo Unlock is NOT in 78; it is on the claude branch with the gate switched off.
- Build 76 (previous): apd-build-76 (main f83839eec; device run 99 green on API 26 + 34, Build APK 76 green)**: everything in 74 PLUS Options > Privacy sending controls (switch, address, Send now), evidence requests (agree/decline), shareable "what is queued and sent" report, session store, telemetry 1.10 quest steps, queue safety fixes. Build 75 (e1ecea2) was the sending switch alone. Automated on emulators: Back on character selection and all six title screens (Rankings, Journal, About, Changes, Start, News) goes to the title in one press; a won run is recorded; the Privacy menu works. A newer main push (22b91c8, run 100 green) builds as 77.
  PHONE CHECKS for 76/77: UIMSD table > Options > Privacy (rows: what stays, sending on/off, address, send now, evidence requests, share report, account, export/delete account, community votes); Android Back on the character screen and every title-screen button.
- Build 74 (previous): apd-build-74 (main 8f13fcf96; core tests green, device run 89 green on API 26 + 34): everything in 73 PLUS queued-run preview and take-back (debug builds, run sheet) and UIMSD Options > Privacy > "What stays on this device". PHONE CHECK: open that Privacy entry and read it. Build 73 notes follow.**
- Build 73 (previous): apd-build-73 (main 76faff841; core tests green, device run 87 green on API 26 + 34 at 513b4fc, which differs from 76faff8 only by docs): everything in 71 PLUS Bosses table, Run type / Modded columns, local submission queue (INDEV run-sheet preview, sends nothing), telemetry 1.9 (hero state per turn, landmarks, keys, secrets, wells, pages), and a real-run device test (PlayRunDeviceTest). Build 71 notes follow.**
- Build 71 (previous): apd-build-71 (main df9350ff1; core + device tests green on main): everything in 70 PLUS UIMSD saved views (Options > Saved views) and column reorder (Options > Arrange the columns). Install 71 for all checks below.**
- Build 70 (previous):  apd-build-70 (main commit a0c8aa55a; code = 328a41a12; core + device tests green API 26 + 34 on main).** Contents: (1) ANDROID BACK, APD-wide. ROOT CAUSE of "Back dumps me out": on UIMSD and the APD text screens the on-screen exit button (ExitButton) listens for the Back key itself, eats it, and jumps straight to the title, so the screen's own Back never ran. Fixed: those exit buttons now call the screen's Back. Also: one press of Back is one Back (BackGate dedupes the key route and the system back-gesture route), open dialogs close first (newest first), UIMSD submenus return to the previous menu, a running tour ends before leaving, Ask-a-question table returns to its answer. (2) DIFFICULTY moved out of APD Settings into run setup: a 1-5 picker on the character screen (HeroSelectScene, above Start), stored per run and locked at run start, 3 = Normal = exact previous balance (NO rebalancing). Tests: core 634 green; device tests API 26 + 34 green (AndroidBackDeviceTest real Back key, RunSetupDifficultyDeviceTest real touches, ApdSettingsClickDeviceTest, UIMSD screens). PHONE CHECKS: (a) press Android Back / back-gesture in: UIMSD Home, a table, a submenu/filter/sort dialog, Ask a question (answer -> builder -> Home), the tour (first Back ends it), APD Settings, Options, Rankings, the character screen: each must go to the obvious previous place, never out of the app; (b) character screen: difficulty picker visible, tapping 1-5 changes the label; start a run, it keeps that difficulty even if you change the picker later; (c) APD Settings has no Difficulty slider. Not done (recorded for later, not approved to start): Cleric un-ban/humor/balance, unusual-query reaction.
- Previous PHONE-CHECK build (superseded): apd-build-69as apd-build-68 = commit 857851043, 69 is the docs-only rebuild carrying this note and is the one to install). Core 628 tests
  green; device tests green API 26 + 34 on main. NEW phone checks: (a) UIMSD Home: an "Ask a question" card under the overview; (b) tap it: the sentence builder (About / Measure / Per / Split by /
  Only where / Which runs / Hidden details / Show the answer), then the answer and "Open as a table"; (c) UIMSD Tables picker has new sections "Play by play" (Floors, Attacks, Damage, Healing,
  Shielding, Experience, Kills, Equipment) and "Details: ..." (steps, rests, item actions, effects, gold, trades ...) once you have played a run on this build (older runs show nothing for the new
  details: unknown, never zero); (d) Guide > "Asking a question"; (e) APD Settings still responds (build 66 fix). Play a run first: the new facts are recorded from this build on.
- Previous PHONE-CHECK build (before the universal architecture): **apd-build-67 = commit 802a8789d** (same code as build 66 / commit 4a39871c1; 67 is the docs-only rebuild and is the one the user installs; replaces build 64, which had a BLOCKER: every action button in APD Settings was dead). Core + device tests green
  (API 26 + API 34), including the new ApdSettingsClickDeviceTest. Everything below for build 64 applies unchanged to build 66, plus the APD Settings click check:
  (0) Options > APD Settings on a fresh launch: Run self-test, Import APD rankings, Export history (ZIP), Import history (ZIP), the "Record run history" tick box and Open UIMSD
  (preview) must each respond (result window / file picker / scene change).
  BUILD 64 BLOCKER, recorded: root cause = input order. WndAPDSettings created the settings pane BEFORE the ScrollPane that holds it. The newest touch area is asked first and the
  ScrollPane's own touch area covers everything inside it, so it was asked before the buttons and swallowed every touch (sliders still drew; UIMSD from the title and Report > Save
  worked because they are not under a ScrollPane). Fix (52b68ae9c): create the ScrollPane first, the controls after. Regression test: ApdSettingsClickDeviceTest sends real touch
  events through the game's own pointer queue to each control (file picker replaced by a recording stand-in installed AFTER the launcher installs the real one). Verified red without
  the fix ("Run self-test did not answer a touch") and green with it. Lesson: a screen-construction smoke test cannot catch dead input; any new window with a ScrollPane needs a click test.
- Previous PHONE-CHECK build (had the APD Settings blocker): **apd-build-64 = commit 74b205f6f** (UIMSD UI/wording polish; core 585 tests green; device tests green API 26 + API 34, including the new
  UimsdScreensDeviceTest that builds every screen/dialog on the engine; replaces build 62). Design notes: docs/APD-UIMSD-UI.md. Phone-check list for it (all subjective, none blocking):
  (a) first-run Home (history empty) reads well and the tour button is prominent; (b) Home cards: grey labels / coloured values; (c) the filter flow: column list grouped, plain
  comparison words, Class offers your real classes with counts, Outcome Won/Died/Abandoned, Difficulty 1-5, "Something else...", "is one of" tick list; (d) tables: two-line headings,
  swipe-sideways line and edge marks, selected row highlight; (e) the "Tables" picker is grouped and marks the current table; (f) Options in sections with [x] switches and
  descriptions; Sort/Columns/Notes dialogs; (g) row sheet and group sheet two-column look; (h) Guide in sections with a glossary; all 15 tour steps read well; (i) MIXED RULES lines are
  amber and visible; (j) nothing is cut off or overlapping in portrait.
- Previous PHONE-CHECK build (before the polish): **apd-build-62 = commit 56ecf99d9** (Decisions 1-5; core tests green, device tests green API 26 + 34; replaces build 60).
- **Decisions 1-5:**
  1. Exotic setting is part of the run: `Dungeon.apdExotic` snapshot at run start, saved as `apd_exotic`; gameplay and ruleset identity use it, never the live slider
     (`SPDSettings.apdExoticChance()` = the run's value; `apdExoticSetting()` = the slider = next run; slider label says "new runs").
  2. Old/adopted runs (save without `apd_exotic`) keep playing with the live setting but are recorded with NO ruleset and NO exotic value (telemetry: empty ruleset id,
     exotic = 255 "not recorded"): unknown, outside "my rules" scope, refused by seed floors. Logs already written by earlier builds are left as they are.
  3. Broad views say so: status line "MIXED RULESETS ..." whenever the rows in scope span more than one ruleset, Home note + "(mixed rulesets)" on Records/Difficulty
     cards, same text in the lifetime report. Narrowing Rules/difficulty removes it; drill-downs unchanged.
  4. Seed floors: recorded item state = state AT GENERATION (new internal columns item.genLevel/genCursed/genEnchant, only for items first recorded at generation;
     builder version 4 rebuilds caches). Items without it show ? for level/curse/enchant and are compared by kind only ("Same kind, state not recorded"), never a mismatch.
  5. Seed floors remembers the hidden-information choice and restores it on Back / any other view.
  NEW PHONE-CHECK ITEMS: (a) APD Settings exotic slider reads "(new runs)"; (b) a game saved by build 60 or older, continued, shows "exotic not recorded (older run)" in UIMSD
  rules text and its new log is unknown-ruleset; (c) Home with all rules on shows the MIXED RULESETS note; (d) Seed floors comparison table has the new "Same kind, state
  not recorded" column; (e) hidden information is back to your own setting after leaving Seed floors.
- Integrity audit (invariants 1-12; 3 read-only audits, then verified): FIXED (tests added; leak and ruleset-gap reproduced before the fix): hidden-group
  drill-down copied an unfinished run's true value into a visible filter; a ruleset change in the last level / between saves was not recorded until now
  (checked at save and run end too); Challenges view merged difficulties (now also groups by difficulty + balance); seed floors refuse MIXED-rules runs and
  runs with unrecorded exotic setting; canonical seed floor numbers are "~" when grouped; seed comparison table is MIXED provenance in reports; stale
  seed Back places dropped when another run's seed floors open; UIMSD rules label used the wrong challenges from a running game.
  ENFORCED with tests: raw logs only appended (LogRecovery tail-repair keeps the cut bytes), cache rebuild identical, demo isolation, imports never replace,
  finished/unfinished masking in tables/filters/sorts/aggregates/reports/exports, Catalog sync.
  PRODUCT DECISIONS (all five decided and implemented, see "Decisions 1-5" below): exotic setting per run, unknown for old runs, mixed-ruleset labelling,
  as-generated item state in seed floors, hidden-information choice restored after seed floors.
  Known low risks left: unreadable run log is dropped from datasets without a notice; row-cache validity = log length + manual BUILDER_VERSION/DERIVATION_VERSION
  bumps (nothing couples them to code edits); legacy ranking records without gameID can import twice; stale seed-floor worker has no epoch check; tour worker
  writes non-volatile statics; unknown balance revision >=2 plays as revision 1 in live play (replay refuses it).
- Previous PHONE-CHECK build: **apd-build-57 = commit 3212b882a** (stability pass; core 567 tests, device tests green API 26 + 34). Previous: apd-build-53 = commit 9ff719889 (Difficulty balance pass 1): core 560 tests green; device tests green API 26 + 34. Previous: apd-build-51 (difficulty framework), 49 (reports), 8dbbadf69 (floor replay).
- Frozen: Phases 0-7 (Phase 7 UIMSD approved/frozen at build 28). Phase 8 (export/import ZIP, storage meter, Android file picker):
  automated verification complete; NOT permanently frozen until the user's physical picker check.
- Telemetry version now 1.6. UIMSD = one major feature; these phases are milestones inside it.

## Overnight 2026-10-08 (canonical plan = APD_Canonical_Design_and_Implementation_Plan.docx, user-supplied; summary of status vs plan)
- Plan 1.0 stats items: DONE earlier: sortable/filterable tables (3.2-3.5), min-sample (3.6), column select (3.7), datasets (3.10), run history (3.11), raw evidence/turn-level facts (3.12-3.14), version+ruleset provenance (3.15), explicit coverage (3.16), data integrity (17.7). ADDED tonight: **Saved views** (3.9): UIMSD Options > "Saved views" (save this table under a name / open / delete); stored in the history folder as saved_views.txt (local only, a saved view is a question, never data); `SavedViews` + 5 tests. ADDED: column REORDER (3.7): UIMSD Options > Arrange the columns (move left/right/first; `ColumnOrder`, 2 tests). ADDED Bosses table (3.10): UIMSD Tables > Enemies section > Bosses (boss flag only for finished runs; hidden class stays hidden). ADDED recording batch D (telemetry 1.9): hero.state per turn (health/shield/hunger) with a Hero state table in UIMSD; new logs only, older runs show ?. ADDED Runs columns "Run type" (random/custom/daily) and "Modded" (plan 3.4/3.15: official vs modded status is now a query dimension; always No until mods exist; older runs show ?). ADDED `CompletionMarks` (7.7 foundation): per class wins and highest difficulty won, from the Runs table; no screen uses it yet (marker design is open, plan 7.7/18.4). Compare subsets (3.8) is covered by Group by + "Compare with" on a group row (report of two groups) and run-vs-run compare; no separate screen planned unless you want one.
- Plan 1.0 non-code items needing the OWNER: attribution/licensing review (1.2, 17.8) and contacting Evan Debenham before any SPD-importer/distribution work (14.5, 17.9).
- Backend foundations (plan 4.1 "design for eventual upload", sections 5, 8, 9, 10, 11 are EVENTUALLY): docs/APD-BACKEND.md + `apd/backend` (30 tests). Provider-neutral; decisions deferred to the owner are listed in that doc.
- PHONE CHECKS for tonight's UIMSD additions (in the next main build): UIMSD table > Options > Saved views (save under a name, open, delete) and Options > Arrange the columns.

## Source style rule (user, permanent, low priority): comments read like a normal maintained project
Short, natural developer notes (WHY, TODO:, FIXME:, warnings). No chat/prompt-like text, no essays, no references to AI or to the conversation; comments document the software, not how it was made.
Don't add comments just to have comments; don't invent history. Clean up unnatural comments only while already working in that subsystem. Known debt: the new apd/backend, apd/backendbridge and
SavedViews comments are wordier than this standard; trim them next time those files are touched. Never delays bug fixes, tests, builds or features.

## Standing task: codebase-wide "why" comments (user-approved roadmap item, ongoing; behaviour-neutral)
Applies to the WHOLE APD codebase, old and new. Add comments that explain WHY (architecture decisions, UIMSD universal-observability rules, telemetry/history integrity, run lifecycle,
difficulty/ruleset identity, FloorReplay/provenance, backend trust boundaries, security, save compatibility/migrations, legacy behaviour that must stay, Android workarounds, unusual behaviour,
performance/storage choices, places where a "simpler" version would be wrong, temporary code, real TODO/FIXME). NOT narration ("// set difficulty"). Rules: (1) documentation passes must not refactor,
reformat or change behaviour of stable code; a genuine bug found is reported/fixed only if already safe and approved; (2) if the reason for old code cannot be established from the code, tests, git
history, handoff docs or the canonical plan, say so ("reason not recorded; believed to be ...") instead of inventing one; (3) keep comments true: update or delete stale ones; (4) when working in a
subsystem, improve its comments there; use spare autonomous time for broader review. Done so far: backend package, Back/exit-button fix, BackGate, difficulty bar. Next candidates: recorder/RunRecorder
(run lifecycle, rollback by event index), stats/LogFormat + FactKinds (permanent ids), difficulty/Difficulty + Ruleset identity, replay/FloorReplay (fixed reference context), archive importer (all-or-nothing),
WndAPDSettings (ScrollPane creation order), UimsdScene/UimsdModel (back stack, tour statics), AndroidLauncher (two Back routes).

## DECISION NEEDED (not blocking other work): Cleric un-ban vs ruleset identity continuity
Un-banning is approved; the open point is how old history relates to the new rules. Ruleset text today: "base=spd-4.0.0;apd0001=on;cleric=blocked;exotic=N;challenges=M[;difficulty=..;balance=..]", id = "r1-" + 12 hex of SHA-256.
Options: (A) bump to r2 and drop/rename the cleric token: simple and honest, but EVERY new run (all classes) gets a new id, so "my current rules" no longer includes the whole existing history unless the player picks "All rules (mixed)"
(non-Cleric gameplay did not actually change). (B) RECOMMENDED: keep ids for all runs whose gameplay rules are unchanged by treating the old "cleric=blocked" text as equivalent to the new rules for every class that was playable
(normalise old ids on read from the stored rulesetText, bump the row-cache builder version), and give runs that are actually Cleric the new rules id; costs a normalisation step in RowBuilders/Scope + tests, no data rewrite (raw logs untouched).
(C) leave the old text token in place forever and only flip the game rule (Cleric runs would be recorded under a text that says "blocked": rejected, it makes the identity lie).
Nothing is implemented; the Cleric block, the black-screen gag and the badge stay until you choose. After the choice the code change is small (HeroSelectScene check, Ruleset text/id, recorder + row-builder normalisation, tests incl. old-log fixtures).

## Attribution / licence status check (plan 1.2, 17.8; findings only, nothing changed)
OK: LICENSE.txt (GPL) is in the repo; APD About screen credits Evan Debenham and says "Based on Shattered Pixel Dungeon by Evan Debenham, licensed GPLv3"; source headers keep the upstream copyright; applicationId has its own APD identity (".indev" suffix for debug).
FOR THE OWNER (public-release items): (1) upstream first-person support/Patreon text is still reachable in APD: WndSupportPrompt (shown from WornKey, "Hello, I hope you're enjoying Shattered Pixel Dungeon! ... I depend on support"), SupporterScene (opened from the victory window) and TitleScene patreon text speak as Evan about HIS game and link to his support pages. Decide with Evan whether to keep, adapt ("support the original creator") or remove them in APD. (2) Several upstream messages (welcome/update screens) say "Shattered Pixel Dungeon has been updated" in APD builds. (3) Confirm the final app name/icon/package are clearly distinct before any store release. (4) Contact Evan before public release/importer work (plan 14.5/17.9).

## Rolling queue (autonomous, no decisions needed): Q2 trim wordy comments in apd/backend, backendbridge, SavedViews (low); next candidates only if the plan supports them: flush status in APD Settings (needs a click test), Cleric audit follow-up after un-ban decision. Everything else open needs an owner decision (see DECISION sections).

## Known test flake (not a game bug as far as seen)
CombatTelemetryTest.theTruthBehindAFightIsInternalOnly (line ~99, `hpAfter < hpBefore` on the FIRST damage event) failed once in a full core run and passed on rerun. Likely cause: the first random hit can be reduced to 0 by the rat's armor, so no HP is lost. If it recurs, pin the random generator in that test (do not loosen the assertion).

## Review later (decided: NOT to change now)
- **Rankings navigation.** Upstream's title screen had a Rankings button (classic top-scores list, `RankingsScene`). APD's title menu (Play, UIMSD, Badges, Journal, Options, Quit,
  APD Settings, News, Changes, About) has none. `RankingsScene` is still reachable only from inside a game (`WndGame`, the end screens `AmuletScene`/`SurfaceScene`). UIMSD already
  imports APD's rankings file as "imported ranking records". Decide intentionally: keep it on the title, fold it into UIMSD, keep it only where it is now, or retire it.
  Upstream's title-screen Support button, `NewsScene` (replaced by APD's own news text) and the in-title `ChangesScene` link are likewise unwired but still compiled.
- **Upstream sync.** APD is forked from upstream tag v4.0.0 (2bb34a4e9, the last upstream commit in the history). Upstream has v4.0.1 and v4.0.2 which are NOT merged. Do a
  deliberate upstream-sync review later (read their changes, merge or port selectively); do not merge as part of other work.
- **APK size (investigated, no action).** APD ships a DEBUG APK (CI `assembleDebug`): no R8 or resource shrinking, all four CPU architectures, ~38.0 MB; the number Android shows
  (38.04 MB) is essentially the APK file. SPD from Play is a release build (R8 + resource shrinking + one architecture) plus compiled-code files, so the two "App size" numbers
  are not comparable. Versus upstream v4.0.0: zero files deleted or renamed, no build setting strips content, APD adds ~1.4 MB of class bytes (~0.8 MB dex) and ~0.3 MB of
  images. User data (2.3 MB vs 274 KB) is APD's run history and caches. Size optimisation is not a priority; do not remove unused upstream code or resources for size.

## Pending manual checks (user)
- Phase 8: physical phone check of the Android system file-picker screens (export save, import open). Everything else is automated.

## Unresolved issues
- None known. Build APK run 31 once failed only in "Publish release" (sub-second, transient; no release apd-build-31 exists).

## Approved requirements that affect current/near work
- Minimum Android = 8.0 (API 26). Do not reintroduce API 21-25 support. Harmless dead compat code (listed in docs/APD-MIN-ANDROID.md)
  is NOT to be cleaned up now.
- Do NOT implement archive-and-remove (deletes raw history): needs the user's separate decision.
- **Adult-humor / content preference (implement before public release, not before Phase 9 ends; do not derail current work):**
  one-time question "are you 18 or older?"; store ONLY a local content preference (no birthday, no age). 18+: title text stays
  "HEY, GO HERE FOR DRUGS"; under 18 / clean: "HEY, GO HERE FOR UNLIMITED DOPAMINE". Both still open UIMSD exactly as now. UIMSD itself
  is never age-restricted. Add an "Adult Humor" setting in APD Settings to change it later. Build it as ONE shared preference that any
  future adult joke reads (no scattered age checks).
- Future UIMSD work (tracked, not started): plain-English Summary/Home view; interactive tutorial/guide (datasets, drill-downs, filters,
  sorting, meaning of ? >= ~ coverage); final language/UX polish pass; shareable/PDF reports; post-run whole-seed/future-floor
  interrogation (needs a proper technical feasibility study first).
- **UIMSD CORE PHILOSOPHY (REQUIREMENT from the user, applies to THE ENTIRE GAME; NOT a "Character UIMSD" feature; survey/design first, NO coding until the user approves; design in docs/APD-UIMSD-UNIVERSAL.md):**
  If APD can reasonably observe something, UIMSD records it. If it can be counted, totalled, averaged, sorted, filtered, grouped, compared or cross-referenced, the player can do that.
  Applies to everything: characters and their unique mechanics (EVERY character, incl. future APD characters), items, equipment actually worn, upgrades, attacks, damage, misses,
  healing, shielding, enemies, floors, movement, spells, abilities, talents, subclasses, consumables, hunger, status effects, shops, gold, identification, bosses, deaths, victories,
  challenges, difficulty, turns, time, generation, and anything else recordable WITHOUT harming gameplay/performance/storage/privacy or revealing still-hidden information.
  The examples are NOT a requirements list and capabilities must NOT be limited by the questions we think of during development: "average Guiding Light casts per floor" must emerge
  from recorded facts (spell casts + character + floor + run) being cross-referenceable, not from anyone predicting that question. Character statistics are one part of the one
  universal data model, never a separate feature or database. Long-term goal: any reasonable question about the player's APD history that APD had the information to answer, UIMSD can answer.
- **UIMSD UNIVERSAL ARCHITECTURE: APPROVED by the user, in progress (design: docs/APD-UIMSD-UNIVERSAL.md). Order: (1) fact-kind registry + generic fact event, (2) Floors dataset + per-run/
  per-floor/per-turn/per-hour/ratio measures, (3) expand recording in batches, measuring storage/performance after each, (4) question/sentence builder as the primary UI. Advanced
  tables/query tools STAY and coexist with the simple interface. Hidden information stays protected while a run is active ("record everything" is not "reveal everything").
  LOSSLESS RULE (user, permanent): RAW HISTORY AS LOSSLESS AS REASONABLY PRACTICAL; summaries/caches exist only to make it FAST; the player UI exists to make it UNDERSTANDABLE. Never
  replace raw high-frequency facts (movement etc.) with summaries just because no query needs them yet: try compact encoding, batching, delta encoding, compression of completed
  runs, rebuildable summaries/indexes first. If full resolution would truly hurt performance, storage, save integrity or gameplay, STOP and show the user the tradeoff before
  discarding anything. Survey the WHOLE game for observable facts; the examples (spells, movement, shops, talents, status effects) are not the boundary; the registry must let future
  mechanics register facts with no UIMSD surgery. Stop for the user only on a real product decision, destructive change, architectural conflict or a lossy tradeoff.
  FUTURE UI EASTER EGG (recorded, NOT to be built in this pass): if a player constructs an exceptionally absurd/complex query, UIMSD may VERY occasionally say "Are you a fucking
  lunatic?" and then give the answer normally. Triggered by query COMPLEXITY (not a hand-written list), rare, respects the Adult/Clean humor setting (clean mode: a mild line), never
  interferes with the result.
- **UIMSD universal architecture: IMPLEMENTED on the claude branch (U1-U4), see docs/APD-UIMSD-UNIVERSAL.md "Status".** Telemetry 1.8. Core tests 628 green, device tests green on
  API 26 + 34 for U1-U3 (U4 running). Remaining: more recording batches (backlog list in that doc), the absurd-query easter egg, and the user's physical check of the Ask card.
- **BUILD 69 PHONE CHECK IN PROGRESS (user). FROZEN until the user's feedback: do not change the universal-UIMSD architecture from this pass unless the check finds an actual problem; do not
  start new code.** Being checked: Home > Ask a question (builder, answer, Open as a table carrying into the advanced tools); Tables picker sections (Floors, Attacks, Damage, Healing,
  Shielding, Experience, Kills, Equipment, new fact tables); APD Settings responds to touch; plus broken sorting, bad wrapping, awkward navigation, wrong/empty results, clunky feel, and any
  hidden information exposed. Older runs showing the new details as unknown is expected.
- **NEXT PRODUCT CHANGE (approved, NOT started; wait for Build 69 feedback): DIFFICULTY MOVES TO RUN SETUP.** Difficulty is a run configuration choice, not an application setting.
  Flow: Play > choose character > choose Difficulty 1-5 > start the run; ideally difficulty is directly visible on the character/run-start screen, not in another menu.
  Requirements: remove "Difficulty (new runs)" from normal/public APD Settings; difficulty stays stored per run; once a run begins its difficulty is LOCKED and changing the future selection
  never modifies an active run; existing/legacy run behavior stays compatible; difficulty 3 is clearly labelled Normal and stays EXACTLY the current normal SPD/APD balance; do NOT rebalance
  1-5 in this change; UIMSD/telemetry keep recording the actual selected difficulty and balance revision; private INDEV builds may keep direct difficulty controls elsewhere for testing.
  General rule: APD Settings = application preferences, storage/data, accessibility and appropriate developer controls. Run setup = choices that define the upcoming run: character,
  difficulty, challenges and future run-specific modifiers.
- **CLERIC IS NO LONGER BANNED (user decision; supersedes any earlier requirement).** Remove any design requirement that Cleric be banned or excluded from APD. Cleric must be fully playable,
  legitimately supported, fairly balanced, eligible for normal statistics/progression, and mechanically treated like every other character. The user's hatred of Cleric becomes APD HUMOR AND
  LORE, never a gameplay punishment. NOTE FOR THE IMPLEMENTER: the current build still blocks Cleric and the ruleset identity text contains "cleric=blocked" (recorder/Ruleset.java). Un-blocking
  Cleric changes that rule text, so ruleset ids of old and new runs would differ unless handled deliberately (a ruleset id version bump with a documented compatibility story). That is a design
  step to propose first, not something to do silently. Not started.
- **CLERIC HUMOR (recorded for later; none of it implemented; none of it may change anything mechanically or distract from stabilization):**
  (1) FIRST CLERIC RUN WARNING, shown once, the first time the player commits to entering the dungeon as Cleric: a developer message along the lines of "You have chosen Cleric. I fucking hate
  Cleric. It took me 225 attempts to finally ascend with this fucking class. I'm not going to stop you from playing it because apparently some people enjoy suffering. Good luck. You'll need
  it." (wording/UI polished later). Not every Cleric run.
  (2) RANDOM CLERIC SLANDER: very rare developer announcements ("IMPORTANT DEVELOPER ANNOUNCEMENT: It has been 47 days since I last reminded you that Cleric should not be a fucking class. I still
  believe this. Thank you.", "Developer Reminder: Just checking in. I still fucking hate Cleric. Carry on.", "IMPORTANT APD ANNOUNCEMENT: Cleric still sucks. Thank you for your attention.", and unrelated
  moments like "You're playing Warrior. This has nothing to do with your run. I just remembered Cleric exists and got annoyed."; contextual milestones such as "You have played Cleric 100 times. I
  fundamentally do not understand you."). Needs: MUCH rarer than once per 10-20 runs: real-world days, weeks or months apart, long randomized real-time cooldowns (not per-run spam) so the player
  forgets the system exists; the player need not be playing Cleric; NEVER during combat, bosses or important dialogue/interactions, wait for a safe moment; the player can disable
  Cleric-slander messages; respects Adult/Clean humor (clean mode needs alternate wording). VARIETY IS NOT A FIXED POOL: see (2b).
  (2b) EFFECTIVELY UNLIMITED VARIETY, EXTREMELY LIMITED FREQUENCY (user requirement). Not a small fixed pool a player can exhaust; design so new jokes can be added indefinitely with no redesign, and
  so that years of play make MORE jokes possible, never fewer. Combine: authored messages, contextual TEMPLATES, milestone messages, player-history-driven messages, current-character context, run
  outcome context, time since the last Cleric reminder, number of Cleric runs, spell/ability usage, UIMSD-recorded Cleric history, and other safe recorded context. Examples: "You have cast
  Guiding Light 4,812 times. This has not improved my opinion of Cleric."; "You have now won 50 Cleric runs. I am beginning to suspect the problem may be me. I still hate Cleric."; "You are
  currently playing Huntress. Unfortunately, Cleric still exists." Several tone/categories so messages are not all structurally identical. Strong anti-repeat (avoid recently seen lines AND
  templates, no obvious cycling order); templates x context greatly expand the combinations; new messages/templates addable later without changing the system. UIMSD history may personalize jokes
  ONLY where safe and accurate: NEVER fabricate a statistic (any number or history fact in a joke must come from actual recorded data), NEVER expose hidden information through a joke, and
  Adult/Clean presentation applies to every generated/contextual variant. A player may see one message every few weeks or months but, even after years, should still have a realistic chance of
  seeing one they have never seen. Intentionally absurd recurring lore, not a notification system to become numb to.
  (3) CLERIC DIFFICULTY DISPLAY GAG: once difficulty is visible at run setup, selecting Cleric may occasionally VISUALLY mess with the display (real difficulty 3 but the UI briefly shows
  something absurd like "Difficulty: 5.0 / 10", or behaves as if APD disapproves). PRESENTATION ONLY. ABSOLUTE RULE: selecting Cleric must NEVER change the actual selected difficulty; it may snap
  back, disappear, or come with a developer message. Players must never have to wonder whether their run was secretly modified.
  (4) CLERIC LORE: occasional harmless references in flavor text, descriptions, very rare messages or developer notes, as recurring lore. The joke is the absurd disproportion (Cleric is a fair,
  legitimate character; APD lore treats it as a historical catastrophe). Do not flood the game; rare finds are funnier.
- **CLERIC HUMOR: PERMANENT CREATIVE DIRECTION (owner, 2026-10-08; supersedes anything above that conflicts).** The Cleric Vendetta AND Appreciation systems are built (OFF until the owner has reviewed the lines and tried them on a phone); see docs/APD-VENDETTA.md, especially its PERMANENT CREATIVE DIRECTION section: developer voice is first person (I/me/my, never we/us/our), a permanent parallel priority that never delays gameplay, UIMSD, backend, fixes, tests or releases, self-aware layers added gradually across updates, library never listed, no invented or quoted community, Cleric the exclusive target.
- **CLERIC BALANCE IS SEPARATE FROM CLERIC HUMOR.** The user's actual gameplay complaint is Guiding Light (it displayed/did 2-8 damage without the Tome-level damage progression expected). Do NOT
  invent a formula from that. Before any change: inspect the actual current Cleric source, Guiding Light, Tome progression, every Cleric spell, talents, subclasses, interactions and existing
  scaling, then PROPOSE a balance pass and wait for approval. The user liked some Cleric spells: "I hate Cleric" does not mean buff everything or redesign everything. The humor is subjective,
  the balance work is evidence/source-driven.
- **UIMSD ABSURD-QUERY EASTER EGG (preserved; not built):** if a player constructs an exceptionally absurd/complex query, UIMSD may VERY occasionally acknowledge it before the result ("Are you a
  fucking lunatic?", or longer: "...You just asked UIMSD to cross-reference 7 different things. Anyway, the answer is 3."), then answers normally. Based on query complexity (Question.complexity()),
  never a hard-coded list; rare enough that power users do not constantly see it; never interferes with the result; respects Adult/Clean. Philosophy: most absurd edge-case queries will never be
  used, but if a deranged question is asked and the facts were recorded, UIMSD should have a serious chance of answering it.
- **UIMSD UNUSUAL-QUERY REACTION: PERSONALITY AND TRIGGER CLARIFIED (user; supersedes the "lunatic" framing above; future feature, not started, do not interrupt current work).**
  PERSONALITY: UIMSD itself is SURPRISED by a question it never expected anyone to ask (it was built to answer anything constructible from recorded facts, yet the player managed something so
  unusual that even UIMSD is taken aback). Example: "Wait... nobody's ever asked me that before. Give me a second. ...huh. The answer is 3." The joke is NOT "you are a lunatic for using too many
  filters". Never fake a global claim ("no player has ever asked this") unless APD really has global data; for a local-only system phrase it as UIMSD meeting a question/query SHAPE it has not
  seen before. TRIGGER = sufficiently COMPLEX AND sufficiently NOVEL: (1) the query exceeds a meaningful complexity/specificity floor (complexity is NOT just a filter count and must not be hard-coded
  as "3 filters = simple"; it can include filters, dimensions, grouping, measures, cross-referenced fact types, unusual conditions, per-floor/per-run/per-hour calculations ...), AND (2) the query's
  structure/combination is genuinely novel according to whatever history UIMSD can legitimately know (e.g. a locally remembered fingerprint of query shapes it has already answered). Novelty
  alone is never enough: a basic question with 2-3 filters must not make UIMSD act surprised just because that exact combination is new. The floor is high so the reaction feels exceptional and
  ordinary power-user queries never trigger it; very rare so players can forget it exists. Never interferes with the result (the answer still follows); respects Adult/Clean. Question.complexity()
  stays as one INPUT to the complexity floor, not the trigger. This does not change the universal UIMSD architecture.
- **OPEN BUG (user-confirmed; do not assume the universal-UIMSD work fixed it):** the previously reported UIMSD sorting problem when sorting highest-to-lowest. Treat as confirmed until a build
  specifically fixes it and a test pins the behaviour. Build 69 did not specifically address it.
- **CURRENT PRIORITY ORDER:** (1) Build 69 is the PHONE-CHECK build. (2) Wait for the user's physical Build 69 feedback. (3) Fix what the phone check exposes (including the sorting bug).
  (4) Then the difficulty / run-setup change. (5) Cleric humor and lore are preserved for later implementation. (6) Cleric balance is a separate future source audit and proposal. (7) Jokes never
  interfere with legitimate gameplay, statistics, balance or UIMSD integrity.
- **ANDROID BACK, APD-WIDE (user requirement; implemented on the claude branch, pending phone check):** Android's Back button/gesture must work wherever there is a meaningful previous
  screen, menu, popup, dialog, submenu or navigation state: a dialog closes before the screen under it; a menu opened from a menu returns to that menu with its state; inside UIMSD Back walks
  backward through UIMSD history before leaving it; a tour ends before UIMSD is left; a table opened from a question's answer returns to that answer; outside UIMSD Back behaves naturally; it
  matches the visible Back/Close action; it never dumps the player out of APD when there is an obvious internal place to go back to. Implementation: one BackGate (SPD-classes) so one press is
  ONE Back whichever route delivers it (the game's key path or the system back-gesture callback); WndUimsdList menus can stay open under the next one (keepOpen/closeAll) so Back returns to the
  previous step; Done and Back are different (Back never applies a half-made choice); AndroidBackDeviceTest injects the real Back key. Treat any remaining Back problem as a bug.
- **DIFFICULTY IS NOW CHOSEN AT RUN SETUP (implemented on the claude branch, pending phone check):** a 1-5 picker (ui/DifficultyBar) sits right above Start on the character screen ("Difficulty 3:
  Normal"; tap the label for an explanation); "Difficulty (new runs)" is gone from APD Settings. It sets SPDSettings.apdDifficulty, which only the NEXT run reads (Dungeon.init); a run in progress
  keeps Dungeon.difficulty, saved with it. Difficulty 3 is unchanged and still the baseline; nothing was rebalanced; telemetry and rulesets still record the real level and balance revision. Hidden
  during the first-run intro. Cleric is still blocked in the build (WndClericRemoved) until the separate un-ban design step.
- Owner decision left open by Phase 9: natural regeneration is the biggest log cost (32 B/heal exact vs 18 B inferred, ~70 KiB per
  20k-turn run). Possible later trade-off: one summary event per stretch of regeneration (loses per-tick history). Not done.

## Decisions that must not be accidentally reversed
- **Non-P2W (permanent design constraint): money may never provide gameplay, progression, statistical, competitive or UIMSD advantages.**
  Applies to every feature, including reports/export, UIMSD and any future store, supporter perk or donation flow.
- Raw per-run logs are the source of truth; every cache/index/fact is derived and rebuildable. Never weaken that.
- Hidden information: INTERNAL events/columns are never shown for unfinished runs; exports carry unfinished runs only as redacted
  copies (INTERNAL events dropped, seed removed). New event types default to INTERNAL; PUBLIC ones must be audited in LogRedactor.
  Exact healing source lives only in INTERNAL HEAL_DETAIL (event 299); the public HEAL stays player-visible information.
- Import is all-or-nothing, never overwrites, never partially modifies history. The ZIP importer's guards stay on every Android
  version: `readChunk` (a 0-byte read = damaged archive) and `ProgressGuardInputStream` (a source that stops answering fails the
  import). Found because Android 5's ZipInputStream spun forever on a cut entry and held the importer lock.
- Demo data (INDEV only) lives in its own marked folder and never enters real history/exports/imports.
- Telemetry changes are additive minors; old logs keep their meaning (older data shows unknown/inferred, never zero).
- Only the hero's HP is recorded for healing; every HP-raising line in the game is either hooked or an explained exemption
  (`ExactHealingWiringTest` fails on a new one).
- Title button label/target: the UIMSD button opens UimsdScene (TitleButtonWiringTest).

## Next approved work
Home/Guide milestone implemented (see docs/APD-UIMSD-HOME-GUIDE.md): UimsdHome, UimsdGuide, UimsdTutorial, tour sample data,
UimsdScene Home + tour overlay. Awaiting user approval of the milestone report. Wording is placeholder (final polish pass pending).
Still pending (user, non-blocking): Phase 8 picker check, Phase 9 healing-column phone check, plus a phone look at Home and the tour.
Not started: adult-humor preference, language polish, shareable/PDF reports, whole-seed interrogation, archive-and-remove.
Measured in Phase 9 (for reference): desktop 50,000-row scan 50 ms; emulator demo history first build 183 ms (API 26) / 630 ms (API 34),
cache reload 22 / 61 ms, slowest view 11 / 54 ms; exact healing 32 B per heal (inferred was 18 B), ~70 KiB per 20k-turn run.
No optimisation was warranted beyond slimming HEAL_DETAIL (19 -> 14 B).

## Future-floor feasibility (study done, not implemented)
docs/APD-FUTURE-FLOORS-FEASIBILITY.md: floors are not f(seed,depth); exact only as in-order replay 1..N under a fixed reference context
(harness proves it). Needs replay-only init + statics snapshot/restore; finished runs only. Awaiting user decisions listed there.

## Phone checks (user workflow)
Do NOT ask the user to install/test each build; intermediate builds need no install. Keep automated core/device/CI testing as strong.
Request a physical test only when (1) blocked without it, (2) a major milestone needs final physical approval, or (3) several pending
checks can be bundled into one designated PHONE-CHECK build. Accumulate non-blocking checks here; keep working meanwhile.
Pending, bundled for the next PHONE-CHECK build (none blocking):
- Phase 8 system file picker (export/import)
- Phase 9 healing columns in UIMSD
- Home layout
- Guide/tour overlay and interaction
- Seed floors screen (Runs row -> Seed floors (canonical)): labels, ~ marks, notice line, comparison table
- APD Settings: Difficulty slider (1-5) and its explanatory text fit and work
- Reports: Home Report button, Options -> Share a report, run/group window report buttons, the share sheet and saving each format to a file

## Floor replay (user approved all 3 decisions; Phase A -> B -> C autonomous; A green at 4e43ce250, B+C implemented and verified at 8dbbadf69 (milestone complete, awaiting approval), see docs/APD-FLOOR-REPLAY.md; physical check of the Seed floors screen joins the PHONE-CHECK bundle)
Decisions: canonical seed floors always labelled (never "the exact counterfactual"); unreached floors of FINISHED runs may be
interrogated; replay only when no live game is loaded; for reached floors recorded telemetry is authoritative (canonical only fills
floors that never existed). Phase A (FloorReplay + isolation + device test) -> docs/APD-FLOOR-REPLAY.md. Phase B: "Seed floors" dataset;
Phase C: recorded-vs-canonical comparison. Keep provenance (RECORDED vs CANONICAL) explicit everywhere.

## ETA rule (permanent)
Before starting any task expected to produce a build, output exactly one short line: "ETA: ~X-Y min" (based on real work/tests/CI), then begin.
If the rule is learned mid-work, give one estimate for the remaining time. Update only if a material problem changes it; no routine
progress reports or repeated ETA checks. When the build finishes, report actual elapsed time if known.

## UIMSD reports/export (milestone COMPLETE at 0994586d1, awaiting approval; see docs/APD-REPORTS.md)
Format: Markdown / plain text / HTML (print to PDF) / CSV from one `Report` model; share as text or save via the system picker; no native PDF
(investigated). Reports: lifetime, view, run, group, run comparison, seed floors (canonical clearly separated). Marks + legend + canonical
notice in every format. Physical check joins the PHONE-CHECK bundle: Report buttons/windows, share sheet, saving a file.

## Difficulty system (balance pass 1 IMPLEMENTED and verified at 9ff719889; see docs/APD-DIFFICULTY.md)
Levels 1-5, 3 = baseline (never changes, revision 0). Revision 1 (levels 1,2,4,5): enemy HP, enemy damage, ordinary-item count at 70/70/140, 85/85/120,
120/120/85, 150/150/70 %. Healing, food, spawns, bosses, champions, traps stay neutral (do NOT add effects without approval). Setting = new runs only; run keeps its
difficulty (saved); legacy runs unknown (?) and play as 3. Telemetry 1.7; UIMSD columns + "Difficulty" view; FloorReplay uses the run's difficulty.
Physical check joins PHONE-CHECK: difficulty slider text; feel of levels 1 and 5 in a real run (enemy toughness, loot amount). Non-P2W applies.

## UIMSD sweep (safe work done; see below for what is NOT done)
Done: Home difficulty card; difficulty named in the rules list/notes/reports; class names as words; Guide topics for reports, seed floors/canonical data and
difficulty; tour reports step; run and group comparisons in reports; wording lint test (UimsdWordingTest); docs refreshed. Reviewed for TODOs: none open.
NOT done on purpose (each needs the owner's decision, none is a TODO in the code): bosses in UIMSD (boss flag is not recorded: telemetry change); saved
queries UI; richer multi-column grouping UI; recorded per-floor creatures in seed floors (needs a new row source); final language/style pass (the text is
functional and lint-clean, not final); archive-and-remove; adult-humor preference (not UIMSD); exact hooks for other inferred signals.
**PHONE-CHECK build** = the CI build of the commit named in "Latest known-good" below; it carries the whole bundled checklist (Phase 8 file picker, Phase 9
healing columns, Home layout (incl. Report and Difficulty card), Guide/tour overlay (15 steps), Seed floors screen, Reports (buttons, share sheet, saving each
format, comparisons), APD Settings difficulty slider and text, feel of difficulty levels 1 and 5). Do not install intermediate builds.

## Working rules the user set (usage efficiency)
Minimal narration; batch related work; do not poll CI repeatedly (estimate the finish, check once around then, do independent approved
work meanwhile); continue autonomously inside approved scope and stop only for genuine product/design decisions, destructive actions,
architectural changes, conflicts with requirements or the end of a phase; testing standards stay exactly as strong (full core suite,
API 26 + API 34 device tests, no weakened assertions or hidden-by-timeout fixes); keep this file updated.

Communication: ultra-terse by default. No narration of routine actions, obvious commands/edits/tests/CI states, or repeats of known
info; no running commentary unless attention is needed. Routine updates 1-3 short sentences ("Implemented. 509 tests green. CI running.").
Tell the user once when waiting on CI, check at an appropriate time, don't re-narrate. Continue autonomously in approved scope; don't ask
needless confirmation. Full detail only for: decisions needed, bugs/risks/unexpected behavior, milestone/phase reports, details needed to
resume safely. Never shorten code, tests, reasoning, verification or docs to save usage. Keep this file concise and authoritative.

## Splitting work with ChatGPT (user's rule, to save Claude usage)
ChatGPT has no access to this session or repo; every handoff must be self-contained. Offload only when it clearly saves effort:
architecture/design review, specs and requirements, test-plan and regression-test design (before implementing), edge-case analysis,
review of a diff or of Claude's completion report, documentation drafting, bug analysis from logs/code the user pastes.
Keep for Claude: editing the repo, repo-wide searches for implementation, building, running tests, fixing compile/test failures,
git, CI/releases, implementation that needs the surrounding source. At the start of a substantial task, say in one or two lines whether
a meaningful part can be offloaded; if so give ONE copy/paste package (task, context, which files/diffs to attach). No pointless
handoffs. Splitting never reduces tests, verification or quality.
 (UIMSD universal architecture; same code as apd-build-68 = commit 857851043, 69 is the docs-only rebuild carrying this note and is the one to install). Core 628 tests
  green; device tests green API 26 + 34 on main. NEW phone checks: (a) UIMSD Home: an "Ask a question" card under the overview; (b) tap it: the sentence builder (About / Measure / Per / Split by /
  Only where / Which runs / Hidden details / Show the answer), then the answer and "Open as a table"; (c) UIMSD Tables picker has new sections "Play by play" (Floors, Attacks, Damage, Healing,
  Shielding, Experience, Kills, Equipment) and "Details: ..." (steps, rests, item actions, effects, gold, trades ...) once you have played a run on this build (older runs show nothing for the new
  details: unknown, never zero); (d) Guide > "Asking a question"; (e) APD Settings still responds (build 66 fix). Play a run first: the new facts are recorded from this build on.
- Previous PHONE-CHECK build (before the universal architecture): **apd-build-67 = commit 802a8789d** (same code as build 66 / commit 4a39871c1; 67 is the docs-only rebuild and is the one the user installs; replaces build 64, which had a BLOCKER: every action button in APD Settings was dead). Core + device tests green
  (API 26 + API 34), including the new ApdSettingsClickDeviceTest. Everything below for build 64 applies unchanged to build 66, plus the APD Settings click check:
  (0) Options > APD Settings on a fresh launch: Run self-test, Import APD rankings, Export history (ZIP), Import history (ZIP), the "Record run history" tick box and Open UIMSD
  (preview) must each respond (result window / file picker / scene change).
  BUILD 64 BLOCKER, recorded: root cause = input order. WndAPDSettings created the settings pane BEFORE the ScrollPane that holds it. The newest touch area is asked first and the
  ScrollPane's own touch area covers everything inside it, so it was asked before the buttons and swallowed every touch (sliders still drew; UIMSD from the title and Report > Save
  worked because they are not under a ScrollPane). Fix (52b68ae9c): create the ScrollPane first, the controls after. Regression test: ApdSettingsClickDeviceTest sends real touch
  events through the game's own pointer queue to each control (file picker replaced by a recording stand-in installed AFTER the launcher installs the real one). Verified red without
  the fix ("Run self-test did not answer a touch") and green with it. Lesson: a screen-construction smoke test cannot catch dead input; any new window with a ScrollPane needs a click test.
- Previous PHONE-CHECK build (had the APD Settings blocker): **apd-build-64 = commit 74b205f6f** (UIMSD UI/wording polish; core 585 tests green; device tests green API 26 + API 34, including the new
  UimsdScreensDeviceTest that builds every screen/dialog on the engine; replaces build 62). Design notes: docs/APD-UIMSD-UI.md. Phone-check list for it (all subjective, none blocking):
  (a) first-run Home (history empty) reads well and the tour button is prominent; (b) Home cards: grey labels / coloured values; (c) the filter flow: column list grouped, plain
  comparison words, Class offers your real classes with counts, Outcome Won/Died/Abandoned, Difficulty 1-5, "Something else...", "is one of" tick list; (d) tables: two-line headings,
  swipe-sideways line and edge marks, selected row highlight; (e) the "Tables" picker is grouped and marks the current table; (f) Options in sections with [x] switches and
  descriptions; Sort/Columns/Notes dialogs; (g) row sheet and group sheet two-column look; (h) Guide in sections with a glossary; all 15 tour steps read well; (i) MIXED RULES lines are
  amber and visible; (j) nothing is cut off or overlapping in portrait.
- Previous PHONE-CHECK build (before the polish): **apd-build-62 = commit 56ecf99d9** (Decisions 1-5; core tests green, device tests green API 26 + 34; replaces build 60).
- **Decisions 1-5:**
  1. Exotic setting is part of the run: `Dungeon.apdExotic` snapshot at run start, saved as `apd_exotic`; gameplay and ruleset identity use it, never the live slider
     (`SPDSettings.apdExoticChance()` = the run's value; `apdExoticSetting()` = the slider = next run; slider label says "new runs").
  2. Old/adopted runs (save without `apd_exotic`) keep playing with the live setting but are recorded with NO ruleset and NO exotic value (telemetry: empty ruleset id,
     exotic = 255 "not recorded"): unknown, outside "my rules" scope, refused by seed floors. Logs already written by earlier builds are left as they are.
  3. Broad views say so: status line "MIXED RULESETS ..." whenever the rows in scope span more than one ruleset, Home note + "(mixed rulesets)" on Records/Difficulty
     cards, same text in the lifetime report. Narrowing Rules/difficulty removes it; drill-downs unchanged.
  4. Seed floors: recorded item state = state AT GENERATION (new internal columns item.genLevel/genCursed/genEnchant, only for items first recorded at generation;
     builder version 4 rebuilds caches). Items without it show ? for level/curse/enchant and are compared by kind only ("Same kind, state not recorded"), never a mismatch.
  5. Seed floors remembers the hidden-information choice and restores it on Back / any other view.
  NEW PHONE-CHECK ITEMS: (a) APD Settings exotic slider reads "(new runs)"; (b) a game saved by build 60 or older, continued, shows "exotic not recorded (older run)" in UIMSD
  rules text and its new log is unknown-ruleset; (c) Home with all rules on shows the MIXED RULESETS note; (d) Seed floors comparison table has the new "Same kind, state
  not recorded" column; (e) hidden information is back to your own setting after leaving Seed floors.
- Integrity audit (invariants 1-12; 3 read-only audits, then verified): FIXED (tests added; leak and ruleset-gap reproduced before the fix): hidden-group
  drill-down copied an unfinished run's true value into a visible filter; a ruleset change in the last level / between saves was not recorded until now
  (checked at save and run end too); Challenges view merged difficulties (now also groups by difficulty + balance); seed floors refuse MIXED-rules runs and
  runs with unrecorded exotic setting; canonical seed floor numbers are "~" when grouped; seed comparison table is MIXED provenance in reports; stale
  seed Back places dropped when another run's seed floors open; UIMSD rules label used the wrong challenges from a running game.
  ENFORCED with tests: raw logs only appended (LogRecovery tail-repair keeps the cut bytes), cache rebuild identical, demo isolation, imports never replace,
  finished/unfinished masking in tables/filters/sorts/aggregates/reports/exports, Catalog sync.
  PRODUCT DECISIONS (all five decided and implemented, see "Decisions 1-5" below): exotic setting per run, unknown for old runs, mixed-ruleset labelling,
  as-generated item state in seed floors, hidden-information choice restored after seed floors.
  Known low risks left: unreadable run log is dropped from datasets without a notice; row-cache validity = log length + manual BUILDER_VERSION/DERIVATION_VERSION
  bumps (nothing couples them to code edits); legacy ranking records without gameID can import twice; stale seed-floor worker has no epoch check; tour worker
  writes non-volatile statics; unknown balance revision >=2 plays as revision 1 in live play (replay refuses it).
- Previous PHONE-CHECK build: **apd-build-57 = commit 3212b882a** (stability pass; core 567 tests, device tests green API 26 + 34). Previous: apd-build-53 = commit 9ff719889 (Difficulty balance pass 1): core 560 tests green; device tests green API 26 + 34. Previous: apd-build-51 (difficulty framework), 49 (reports), 8dbbadf69 (floor replay).
- Frozen: Phases 0-7 (Phase 7 UIMSD approved/frozen at build 28). Phase 8 (export/import ZIP, storage meter, Android file picker):
  automated verification complete; NOT permanently frozen until the user's physical picker check.
- Telemetry version now 1.6. UIMSD = one major feature; these phases are milestones inside it.

## Review later (decided: NOT to change now)
- **Rankings navigation.** Upstream's title screen had a Rankings button (classic top-scores list, `RankingsScene`). APD's title menu (Play, UIMSD, Badges, Journal, Options, Quit,
  APD Settings, News, Changes, About) has none. `RankingsScene` is still reachable only from inside a game (`WndGame`, the end screens `AmuletScene`/`SurfaceScene`). UIMSD already
  imports APD's rankings file as "imported ranking records". Decide intentionally: keep it on the title, fold it into UIMSD, keep it only where it is now, or retire it.
  Upstream's title-screen Support button, `NewsScene` (replaced by APD's own news text) and the in-title `ChangesScene` link are likewise unwired but still compiled.
- **Upstream sync.** APD is forked from upstream tag v4.0.0 (2bb34a4e9, the last upstream commit in the history). Upstream has v4.0.1 and v4.0.2 which are NOT merged. Do a
  deliberate upstream-sync review later (read their changes, merge or port selectively); do not merge as part of other work.
- **APK size (investigated, no action).** APD ships a DEBUG APK (CI `assembleDebug`): no R8 or resource shrinking, all four CPU architectures, ~38.0 MB; the number Android shows
  (38.04 MB) is essentially the APK file. SPD from Play is a release build (R8 + resource shrinking + one architecture) plus compiled-code files, so the two "App size" numbers
  are not comparable. Versus upstream v4.0.0: zero files deleted or renamed, no build setting strips content, APD adds ~1.4 MB of class bytes (~0.8 MB dex) and ~0.3 MB of
  images. User data (2.3 MB vs 274 KB) is APD's run history and caches. Size optimisation is not a priority; do not remove unused upstream code or resources for size.

## Pending manual checks (user)
- Phase 8: physical phone check of the Android system file-picker screens (export save, import open). Everything else is automated.

## Unresolved issues
- None known. Build APK run 31 once failed only in "Publish release" (sub-second, transient; no release apd-build-31 exists).

## Approved requirements that affect current/near work
- Minimum Android = 8.0 (API 26). Do not reintroduce API 21-25 support. Harmless dead compat code (listed in docs/APD-MIN-ANDROID.md)
  is NOT to be cleaned up now.
- Do NOT implement archive-and-remove (deletes raw history): needs the user's separate decision.
- **Adult-humor / content preference (implement before public release, not before Phase 9 ends; do not derail current work):**
  one-time question "are you 18 or older?"; store ONLY a local content preference (no birthday, no age). 18+: title text stays
  "HEY, GO HERE FOR DRUGS"; under 18 / clean: "HEY, GO HERE FOR UNLIMITED DOPAMINE". Both still open UIMSD exactly as now. UIMSD itself
  is never age-restricted. Add an "Adult Humor" setting in APD Settings to change it later. Build it as ONE shared preference that any
  future adult joke reads (no scattered age checks).
- Future UIMSD work (tracked, not started): plain-English Summary/Home view; interactive tutorial/guide (datasets, drill-downs, filters,
  sorting, meaning of ? >= ~ coverage); final language/UX polish pass; shareable/PDF reports; post-run whole-seed/future-floor
  interrogation (needs a proper technical feasibility study first).
- **UIMSD CORE PHILOSOPHY (REQUIREMENT from the user, applies to THE ENTIRE GAME; NOT a "Character UIMSD" feature; survey/design first, NO coding until the user approves; design in docs/APD-UIMSD-UNIVERSAL.md):**
  If APD can reasonably observe something, UIMSD records it. If it can be counted, totalled, averaged, sorted, filtered, grouped, compared or cross-referenced, the player can do that.
  Applies to everything: characters and their unique mechanics (EVERY character, incl. future APD characters), items, equipment actually worn, upgrades, attacks, damage, misses,
  healing, shielding, enemies, floors, movement, spells, abilities, talents, subclasses, consumables, hunger, status effects, shops, gold, identification, bosses, deaths, victories,
  challenges, difficulty, turns, time, generation, and anything else recordable WITHOUT harming gameplay/performance/storage/privacy or revealing still-hidden information.
  The examples are NOT a requirements list and capabilities must NOT be limited by the questions we think of during development: "average Guiding Light casts per floor" must emerge
  from recorded facts (spell casts + character + floor + run) being cross-referenceable, not from anyone predicting that question. Character statistics are one part of the one
  universal data model, never a separate feature or database. Long-term goal: any reasonable question about the player's APD history that APD had the information to answer, UIMSD can answer.
- **UIMSD UNIVERSAL ARCHITECTURE: APPROVED by the user, in progress (design: docs/APD-UIMSD-UNIVERSAL.md). Order: (1) fact-kind registry + generic fact event, (2) Floors dataset + per-run/
  per-floor/per-turn/per-hour/ratio measures, (3) expand recording in batches, measuring storage/performance after each, (4) question/sentence builder as the primary UI. Advanced
  tables/query tools STAY and coexist with the simple interface. Hidden information stays protected while a run is active ("record everything" is not "reveal everything").
  LOSSLESS RULE (user, permanent): RAW HISTORY AS LOSSLESS AS REASONABLY PRACTICAL; summaries/caches exist only to make it FAST; the player UI exists to make it UNDERSTANDABLE. Never
  replace raw high-frequency facts (movement etc.) with summaries just because no query needs them yet: try compact encoding, batching, delta encoding, compression of completed
  runs, rebuildable summaries/indexes first. If full resolution would truly hurt performance, storage, save integrity or gameplay, STOP and show the user the tradeoff before
  discarding anything. Survey the WHOLE game for observable facts; the examples (spells, movement, shops, talents, status effects) are not the boundary; the registry must let future
  mechanics register facts with no UIMSD surgery. Stop for the user only on a real product decision, destructive change, architectural conflict or a lossy tradeoff.
  FUTURE UI EASTER EGG (recorded, NOT to be built in this pass): if a player constructs an exceptionally absurd/complex query, UIMSD may VERY occasionally say "Are you a fucking
  lunatic?" and then give the answer normally. Triggered by query COMPLEXITY (not a hand-written list), rare, respects the Adult/Clean humor setting (clean mode: a mild line), never
  interferes with the result.
- **UIMSD universal architecture: IMPLEMENTED on the claude branch (U1-U4), see docs/APD-UIMSD-UNIVERSAL.md "Status".** Telemetry 1.8. Core tests 628 green, device tests green on
  API 26 + 34 for U1-U3 (U4 running). Remaining: more recording batches (backlog list in that doc), the absurd-query easter egg, and the user's physical check of the Ask card.
- **BUILD 69 PHONE CHECK IN PROGRESS (user). FROZEN until the user's feedback: do not change the universal-UIMSD architecture from this pass unless the check finds an actual problem; do not
  start new code.** Being checked: Home > Ask a question (builder, answer, Open as a table carrying into the advanced tools); Tables picker sections (Floors, Attacks, Damage, Healing,
  Shielding, Experience, Kills, Equipment, new fact tables); APD Settings responds to touch; plus broken sorting, bad wrapping, awkward navigation, wrong/empty results, clunky feel, and any
  hidden information exposed. Older runs showing the new details as unknown is expected.
- **NEXT PRODUCT CHANGE (approved, NOT started; wait for Build 69 feedback): DIFFICULTY MOVES TO RUN SETUP.** Difficulty is a run configuration choice, not an application setting.
  Flow: Play > choose character > choose Difficulty 1-5 > start the run; ideally difficulty is directly visible on the character/run-start screen, not in another menu.
  Requirements: remove "Difficulty (new runs)" from normal/public APD Settings; difficulty stays stored per run; once a run begins its difficulty is LOCKED and changing the future selection
  never modifies an active run; existing/legacy run behavior stays compatible; difficulty 3 is clearly labelled Normal and stays EXACTLY the current normal SPD/APD balance; do NOT rebalance
  1-5 in this change; UIMSD/telemetry keep recording the actual selected difficulty and balance revision; private INDEV builds may keep direct difficulty controls elsewhere for testing.
  General rule: APD Settings = application preferences, storage/data, accessibility and appropriate developer controls. Run setup = choices that define the upcoming run: character,
  difficulty, challenges and future run-specific modifiers.
- **CLERIC IS NO LONGER BANNED (user decision; supersedes any earlier requirement).** Remove any design requirement that Cleric be banned or excluded from APD. Cleric must be fully playable,
  legitimately supported, fairly balanced, eligible for normal statistics/progression, and mechanically treated like every other character. The user's hatred of Cleric becomes APD HUMOR AND
  LORE, never a gameplay punishment. NOTE FOR THE IMPLEMENTER: the current build still blocks Cleric and the ruleset identity text contains "cleric=blocked" (recorder/Ruleset.java). Un-blocking
  Cleric changes that rule text, so ruleset ids of old and new runs would differ unless handled deliberately (a ruleset id version bump with a documented compatibility story). That is a design
  step to propose first, not something to do silently. Not started.
- **CLERIC HUMOR (recorded for later; none of it implemented; none of it may change anything mechanically or distract from stabilization):**
  (1) FIRST CLERIC RUN WARNING, shown once, the first time the player commits to entering the dungeon as Cleric: a developer message along the lines of "You have chosen Cleric. I fucking hate
  Cleric. It took me 225 attempts to finally ascend with this fucking class. I'm not going to stop you from playing it because apparently some people enjoy suffering. Good luck. You'll need
  it." (wording/UI polished later). Not every Cleric run.
  (2) RANDOM CLERIC SLANDER: very rare developer announcements ("IMPORTANT DEVELOPER ANNOUNCEMENT: It has been 47 days since I last reminded you that Cleric should not be a fucking class. I still
  believe this. Thank you.", "Developer Reminder: Just checking in. I still fucking hate Cleric. Carry on.", "IMPORTANT APD ANNOUNCEMENT: Cleric still sucks. Thank you for your attention.", and unrelated
  moments like "You're playing Warrior. This has nothing to do with your run. I just remembered Cleric exists and got annoyed."; contextual milestones such as "You have played Cleric 100 times. I
  fundamentally do not understand you."). Needs: MUCH rarer than once per 10-20 runs: real-world days, weeks or months apart, long randomized real-time cooldowns (not per-run spam) so the player
  forgets the system exists; the player need not be playing Cleric; NEVER during combat, bosses or important dialogue/interactions, wait for a safe moment; the player can disable
  Cleric-slander messages; respects Adult/Clean humor (clean mode needs alternate wording). VARIETY IS NOT A FIXED POOL: see (2b).
  (2b) EFFECTIVELY UNLIMITED VARIETY, EXTREMELY LIMITED FREQUENCY (user requirement). Not a small fixed pool a player can exhaust; design so new jokes can be added indefinitely with no redesign, and
  so that years of play make MORE jokes possible, never fewer. Combine: authored messages, contextual TEMPLATES, milestone messages, player-history-driven messages, current-character context, run
  outcome context, time since the last Cleric reminder, number of Cleric runs, spell/ability usage, UIMSD-recorded Cleric history, and other safe recorded context. Examples: "You have cast
  Guiding Light 4,812 times. This has not improved my opinion of Cleric."; "You have now won 50 Cleric runs. I am beginning to suspect the problem may be me. I still hate Cleric."; "You are
  currently playing Huntress. Unfortunately, Cleric still exists." Several tone/categories so messages are not all structurally identical. Strong anti-repeat (avoid recently seen lines AND
  templates, no obvious cycling order); templates x context greatly expand the combinations; new messages/templates addable later without changing the system. UIMSD history may personalize jokes
  ONLY where safe and accurate: NEVER fabricate a statistic (any number or history fact in a joke must come from actual recorded data), NEVER expose hidden information through a joke, and
  Adult/Clean presentation applies to every generated/contextual variant. A player may see one message every few weeks or months but, even after years, should still have a realistic chance of
  seeing one they have never seen. Intentionally absurd recurring lore, not a notification system to become numb to.
  (3) CLERIC DIFFICULTY DISPLAY GAG: once difficulty is visible at run setup, selecting Cleric may occasionally VISUALLY mess with the display (real difficulty 3 but the UI briefly shows
  something absurd like "Difficulty: 5.0 / 10", or behaves as if APD disapproves). PRESENTATION ONLY. ABSOLUTE RULE: selecting Cleric must NEVER change the actual selected difficulty; it may snap
  back, disappear, or come with a developer message. Players must never have to wonder whether their run was secretly modified.
  (4) CLERIC LORE: occasional harmless references in flavor text, descriptions, very rare messages or developer notes, as recurring lore. The joke is the absurd disproportion (Cleric is a fair,
  legitimate character; APD lore treats it as a historical catastrophe). Do not flood the game; rare finds are funnier.
- **CLERIC BALANCE IS SEPARATE FROM CLERIC HUMOR.** The user's actual gameplay complaint is Guiding Light (it displayed/did 2-8 damage without the Tome-level damage progression expected). Do NOT
  invent a formula from that. Before any change: inspect the actual current Cleric source, Guiding Light, Tome progression, every Cleric spell, talents, subclasses, interactions and existing
  scaling, then PROPOSE a balance pass and wait for approval. The user liked some Cleric spells: "I hate Cleric" does not mean buff everything or redesign everything. The humor is subjective,
  the balance work is evidence/source-driven.
- **UIMSD ABSURD-QUERY EASTER EGG (preserved; not built):** if a player constructs an exceptionally absurd/complex query, UIMSD may VERY occasionally acknowledge it before the result ("Are you a
  fucking lunatic?", or longer: "...You just asked UIMSD to cross-reference 7 different things. Anyway, the answer is 3."), then answers normally. Based on query complexity (Question.complexity()),
  never a hard-coded list; rare enough that power users do not constantly see it; never interferes with the result; respects Adult/Clean. Philosophy: most absurd edge-case queries will never be
  used, but if a deranged question is asked and the facts were recorded, UIMSD should have a serious chance of answering it.
- **UIMSD UNUSUAL-QUERY REACTION: PERSONALITY AND TRIGGER CLARIFIED (user; supersedes the "lunatic" framing above; future feature, not started, do not interrupt current work).**
  PERSONALITY: UIMSD itself is SURPRISED by a question it never expected anyone to ask (it was built to answer anything constructible from recorded facts, yet the player managed something so
  unusual that even UIMSD is taken aback). Example: "Wait... nobody's ever asked me that before. Give me a second. ...huh. The answer is 3." The joke is NOT "you are a lunatic for using too many
  filters". Never fake a global claim ("no player has ever asked this") unless APD really has global data; for a local-only system phrase it as UIMSD meeting a question/query SHAPE it has not
  seen before. TRIGGER = sufficiently COMPLEX AND sufficiently NOVEL: (1) the query exceeds a meaningful complexity/specificity floor (complexity is NOT just a filter count and must not be hard-coded
  as "3 filters = simple"; it can include filters, dimensions, grouping, measures, cross-referenced fact types, unusual conditions, per-floor/per-run/per-hour calculations ...), AND (2) the query's
  structure/combination is genuinely novel according to whatever history UIMSD can legitimately know (e.g. a locally remembered fingerprint of query shapes it has already answered). Novelty
  alone is never enough: a basic question with 2-3 filters must not make UIMSD act surprised just because that exact combination is new. The floor is high so the reaction feels exceptional and
  ordinary power-user queries never trigger it; very rare so players can forget it exists. Never interferes with the result (the answer still follows); respects Adult/Clean. Question.complexity()
  stays as one INPUT to the complexity floor, not the trigger. This does not change the universal UIMSD architecture.
- **OPEN BUG (user-confirmed; do not assume the universal-UIMSD work fixed it):** the previously reported UIMSD sorting problem when sorting highest-to-lowest. Treat as confirmed until a build
  specifically fixes it and a test pins the behaviour. Build 69 did not specifically address it.
- **CURRENT PRIORITY ORDER:** (1) Build 69 is the PHONE-CHECK build. (2) Wait for the user's physical Build 69 feedback. (3) Fix what the phone check exposes (including the sorting bug).
  (4) Then the difficulty / run-setup change. (5) Cleric humor and lore are preserved for later implementation. (6) Cleric balance is a separate future source audit and proposal. (7) Jokes never
  interfere with legitimate gameplay, statistics, balance or UIMSD integrity.
- **ANDROID BACK, APD-WIDE (user requirement; implemented on the claude branch, pending phone check):** Android's Back button/gesture must work wherever there is a meaningful previous
  screen, menu, popup, dialog, submenu or navigation state: a dialog closes before the screen under it; a menu opened from a menu returns to that menu with its state; inside UIMSD Back walks
  backward through UIMSD history before leaving it; a tour ends before UIMSD is left; a table opened from a question's answer returns to that answer; outside UIMSD Back behaves naturally; it
  matches the visible Back/Close action; it never dumps the player out of APD when there is an obvious internal place to go back to. Implementation: one BackGate (SPD-classes) so one press is
  ONE Back whichever route delivers it (the game's key path or the system back-gesture callback); WndUimsdList menus can stay open under the next one (keepOpen/closeAll) so Back returns to the
  previous step; Done and Back are different (Back never applies a half-made choice); AndroidBackDeviceTest injects the real Back key. Treat any remaining Back problem as a bug.
- **DIFFICULTY IS NOW CHOSEN AT RUN SETUP (implemented on the claude branch, pending phone check):** a 1-5 picker (ui/DifficultyBar) sits right above Start on the character screen ("Difficulty 3:
  Normal"; tap the label for an explanation); "Difficulty (new runs)" is gone from APD Settings. It sets SPDSettings.apdDifficulty, which only the NEXT run reads (Dungeon.init); a run in progress
  keeps Dungeon.difficulty, saved with it. Difficulty 3 is unchanged and still the baseline; nothing was rebalanced; telemetry and rulesets still record the real level and balance revision. Hidden
  during the first-run intro. Cleric is still blocked in the build (WndClericRemoved) until the separate un-ban design step.
- Owner decision left open by Phase 9: natural regeneration is the biggest log cost (32 B/heal exact vs 18 B inferred, ~70 KiB per
  20k-turn run). Possible later trade-off: one summary event per stretch of regeneration (loses per-tick history). Not done.

## Decisions that must not be accidentally reversed
- **Non-P2W (permanent design constraint): money may never provide gameplay, progression, statistical, competitive or UIMSD advantages.**
  Applies to every feature, including reports/export, UIMSD and any future store, supporter perk or donation flow.
- Raw per-run logs are the source of truth; every cache/index/fact is derived and rebuildable. Never weaken that.
- Hidden information: INTERNAL events/columns are never shown for unfinished runs; exports carry unfinished runs only as redacted
  copies (INTERNAL events dropped, seed removed). New event types default to INTERNAL; PUBLIC ones must be audited in LogRedactor.
  Exact healing source lives only in INTERNAL HEAL_DETAIL (event 299); the public HEAL stays player-visible information.
- Import is all-or-nothing, never overwrites, never partially modifies history. The ZIP importer's guards stay on every Android
  version: `readChunk` (a 0-byte read = damaged archive) and `ProgressGuardInputStream` (a source that stops answering fails the
  import). Found because Android 5's ZipInputStream spun forever on a cut entry and held the importer lock.
- Demo data (INDEV only) lives in its own marked folder and never enters real history/exports/imports.
- Telemetry changes are additive minors; old logs keep their meaning (older data shows unknown/inferred, never zero).
- Only the hero's HP is recorded for healing; every HP-raising line in the game is either hooked or an explained exemption
  (`ExactHealingWiringTest` fails on a new one).
- Title button label/target: the UIMSD button opens UimsdScene (TitleButtonWiringTest).

## Next approved work
Home/Guide milestone implemented (see docs/APD-UIMSD-HOME-GUIDE.md): UimsdHome, UimsdGuide, UimsdTutorial, tour sample data,
UimsdScene Home + tour overlay. Awaiting user approval of the milestone report. Wording is placeholder (final polish pass pending).
Still pending (user, non-blocking): Phase 8 picker check, Phase 9 healing-column phone check, plus a phone look at Home and the tour.
Not started: adult-humor preference, language polish, shareable/PDF reports, whole-seed interrogation, archive-and-remove.
Measured in Phase 9 (for reference): desktop 50,000-row scan 50 ms; emulator demo history first build 183 ms (API 26) / 630 ms (API 34),
cache reload 22 / 61 ms, slowest view 11 / 54 ms; exact healing 32 B per heal (inferred was 18 B), ~70 KiB per 20k-turn run.
No optimisation was warranted beyond slimming HEAL_DETAIL (19 -> 14 B).

## Future-floor feasibility (study done, not implemented)
docs/APD-FUTURE-FLOORS-FEASIBILITY.md: floors are not f(seed,depth); exact only as in-order replay 1..N under a fixed reference context
(harness proves it). Needs replay-only init + statics snapshot/restore; finished runs only. Awaiting user decisions listed there.

## Phone checks (user workflow)
Do NOT ask the user to install/test each build; intermediate builds need no install. Keep automated core/device/CI testing as strong.
Request a physical test only when (1) blocked without it, (2) a major milestone needs final physical approval, or (3) several pending
checks can be bundled into one designated PHONE-CHECK build. Accumulate non-blocking checks here; keep working meanwhile.
Pending, bundled for the next PHONE-CHECK build (none blocking):
- Phase 8 system file picker (export/import)
- Phase 9 healing columns in UIMSD
- Home layout
- Guide/tour overlay and interaction
- Seed floors screen (Runs row -> Seed floors (canonical)): labels, ~ marks, notice line, comparison table
- APD Settings: Difficulty slider (1-5) and its explanatory text fit and work
- Reports: Home Report button, Options -> Share a report, run/group window report buttons, the share sheet and saving each format to a file

## Floor replay (user approved all 3 decisions; Phase A -> B -> C autonomous; A green at 4e43ce250, B+C implemented and verified at 8dbbadf69 (milestone complete, awaiting approval), see docs/APD-FLOOR-REPLAY.md; physical check of the Seed floors screen joins the PHONE-CHECK bundle)
Decisions: canonical seed floors always labelled (never "the exact counterfactual"); unreached floors of FINISHED runs may be
interrogated; replay only when no live game is loaded; for reached floors recorded telemetry is authoritative (canonical only fills
floors that never existed). Phase A (FloorReplay + isolation + device test) -> docs/APD-FLOOR-REPLAY.md. Phase B: "Seed floors" dataset;
Phase C: recorded-vs-canonical comparison. Keep provenance (RECORDED vs CANONICAL) explicit everywhere.

## ETA rule (permanent)
Before starting any task expected to produce a build, output exactly one short line: "ETA: ~X-Y min" (based on real work/tests/CI), then begin.
If the rule is learned mid-work, give one estimate for the remaining time. Update only if a material problem changes it; no routine
progress reports or repeated ETA checks. When the build finishes, report actual elapsed time if known.

## UIMSD reports/export (milestone COMPLETE at 0994586d1, awaiting approval; see docs/APD-REPORTS.md)
Format: Markdown / plain text / HTML (print to PDF) / CSV from one `Report` model; share as text or save via the system picker; no native PDF
(investigated). Reports: lifetime, view, run, group, run comparison, seed floors (canonical clearly separated). Marks + legend + canonical
notice in every format. Physical check joins the PHONE-CHECK bundle: Report buttons/windows, share sheet, saving a file.

## Difficulty system (balance pass 1 IMPLEMENTED and verified at 9ff719889; see docs/APD-DIFFICULTY.md)
Levels 1-5, 3 = baseline (never changes, revision 0). Revision 1 (levels 1,2,4,5): enemy HP, enemy damage, ordinary-item count at 70/70/140, 85/85/120,
120/120/85, 150/150/70 %. Healing, food, spawns, bosses, champions, traps stay neutral (do NOT add effects without approval). Setting = new runs only; run keeps its
difficulty (saved); legacy runs unknown (?) and play as 3. Telemetry 1.7; UIMSD columns + "Difficulty" view; FloorReplay uses the run's difficulty.
Physical check joins PHONE-CHECK: difficulty slider text; feel of levels 1 and 5 in a real run (enemy toughness, loot amount). Non-P2W applies.

## UIMSD sweep (safe work done; see below for what is NOT done)
Done: Home difficulty card; difficulty named in the rules list/notes/reports; class names as words; Guide topics for reports, seed floors/canonical data and
difficulty; tour reports step; run and group comparisons in reports; wording lint test (UimsdWordingTest); docs refreshed. Reviewed for TODOs: none open.
NOT done on purpose (each needs the owner's decision, none is a TODO in the code): bosses in UIMSD (boss flag is not recorded: telemetry change); saved
queries UI; richer multi-column grouping UI; recorded per-floor creatures in seed floors (needs a new row source); final language/style pass (the text is
functional and lint-clean, not final); archive-and-remove; adult-humor preference (not UIMSD); exact hooks for other inferred signals.
**PHONE-CHECK build** = the CI build of the commit named in "Latest known-good" below; it carries the whole bundled checklist (Phase 8 file picker, Phase 9
healing columns, Home layout (incl. Report and Difficulty card), Guide/tour overlay (15 steps), Seed floors screen, Reports (buttons, share sheet, saving each
format, comparisons), APD Settings difficulty slider and text, feel of difficulty levels 1 and 5). Do not install intermediate builds.

## Working rules the user set (usage efficiency)
Minimal narration; batch related work; do not poll CI repeatedly (estimate the finish, check once around then, do independent approved
work meanwhile); continue autonomously inside approved scope and stop only for genuine product/design decisions, destructive actions,
architectural changes, conflicts with requirements or the end of a phase; testing standards stay exactly as strong (full core suite,
API 26 + API 34 device tests, no weakened assertions or hidden-by-timeout fixes); keep this file updated.

Communication: ultra-terse by default. No narration of routine actions, obvious commands/edits/tests/CI states, or repeats of known
info; no running commentary unless attention is needed. Routine updates 1-3 short sentences ("Implemented. 509 tests green. CI running.").
Tell the user once when waiting on CI, check at an appropriate time, don't re-narrate. Continue autonomously in approved scope; don't ask
needless confirmation. Full detail only for: decisions needed, bugs/risks/unexpected behavior, milestone/phase reports, details needed to
resume safely. Never shorten code, tests, reasoning, verification or docs to save usage. Keep this file concise and authoritative.

## Splitting work with ChatGPT (user's rule, to save Claude usage)
ChatGPT has no access to this session or repo; every handoff must be self-contained. Offload only when it clearly saves effort:
architecture/design review, specs and requirements, test-plan and regression-test design (before implementing), edge-case analysis,
review of a diff or of Claude's completion report, documentation drafting, bug analysis from logs/code the user pastes.
Keep for Claude: editing the repo, repo-wide searches for implementation, building, running tests, fixing compile/test failures,
git, CI/releases, implementation that needs the surrounding source. At the start of a substantial task, say in one or two lines whether
a meaningful part can be offloaded; if so give ONE copy/paste package (task, context, which files/diffs to attach). No pointless
handoffs. Splitting never reduces tests, verification or quality.
