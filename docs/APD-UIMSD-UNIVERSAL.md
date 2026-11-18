# UIMSD universal recording and querying: survey and design (NOT implemented, awaiting the user's approval)

Requirement (user): if APD can reasonably observe it, UIMSD records it, and anything recorded can be counted, totalled, averaged, sorted, filtered, grouped, compared and
cross-referenced. This is UIMSD's core identity for the WHOLE game. Characters are only one part of it. No question list is hard-coded: "average Guiding Light casts per floor"
must fall out of recorded facts (cast + spell + character + floor + run) being cross-referenceable.

## 1. Does the current architecture support this cleanly?
Partly. What is already right:
- Append-only, versioned event log per run; event type IDs never reused; readers skip types they do not know; runs recorded before a type existed show as unknown (`?`), never as zero.
- Every fact carries provenance; PUBLIC vs INTERNAL visibility is enforced per event type and per column (hidden truth never shown for an unfinished run).
- The query side is data-driven: `ColumnDef`/`DatasetDef` describe columns, so filters, sorts, grouping, aggregates, scopes, minimum-sample rules, saved queries (`QueryCodec`), row
  caches and the table widget all work for any column with no UI change. Reports, export/import archive and legacy import sit on top.
What is NOT clean enough:
- Recording is bespoke. Each new kind of fact needs: a hand-written payload codec (CombatEvents-style class), a tracker/hook in the game code, a `RunFactsBuilder` rollup and a
  `RowBuilders` row builder, plus a telemetry version bump and tests. That is roughly four files per fact. "Record everything" cannot scale on that.
- Only four datasets exist (Runs, Items, Enemies, Deaths). Attacks, damage, heals, shields, XP and hunger are recorded as events but only reach UIMSD as run-level totals, so they cannot be
  sorted/grouped/averaged per floor, per fight or per item.
- There is no Floors dataset and no "per what" (per run / per floor / per turn / per hour) measure, so "X per floor" cannot be asked at all today.
- Not recorded as events at all (as far as the event list shows): spells and abilities used, talents chosen, subclass changes (only the checkpoint snapshot carries it), status effects
  (buffs) gained/lost, wand/staff/artifact use beyond the damage they deal, gold gained/spent and shops, movement/exploration, searching, doors/traps, consumable use as an action.

## 2. Architectural changes needed
A. One generic, self-describing fact event next to the bespoke ones. A "fact kind" = stable string key (interned once per log), actor, subject, a few numeric measures, a few dimension
   tags, visibility. New mechanics add a kind instead of a codec. Hot, structured events (attack, damage, heal) keep their compact bespoke form.
B. A fact-kind registry (code, like `Datasets`): key, label, description, dimensions, measures, visibility, since-version. It drives the log, the dataset list, the Guide text and the
   question builder, so adding a character or mechanic means declaring kinds and one recording call, never touching UIMSD screens.
C. Instrument the central choke points once, keyed by class name, so content nobody has written yet is covered automatically: item actions (Item.execute with its action name),
   buff attach/detach (every status effect, any class), damage/heal/shield (extend what exists), talent upgrades, subclass choice, spell/ability casts, gold changes and shop buy/sell,
   level enter/exit summaries, trap/door triggers. Per-item or per-character code is the exception, not the rule.
D. Event datasets plus a context join. Every kind becomes a dataset (rows = events) that carries run, floor/depth, turn, hero class, subclass, difficulty, challenges and equipped gear.
   Context is resolved at read time from the run timeline (`RunTimeline` already exists) instead of being stored on every event, which keeps the log small.
E. A Floors dataset (one row per level visit) and "per" denominators in the aggregate engine: count/sum/avg/min/max, per run, per floor, per turn, per hour, and ratio of two measures.
   "Average Guiding Light casts per floor" = count(kind=cast, spell=Guiding Light) / floors entered, grouped as the player likes.
F. Same rules everywhere: additive telemetry versions only, `?`/`>=`/`~` provenance and "since" columns for older logs, per-kind visibility with a leak test (no unidentified-item or
   hidden-generation truth through any new kind), manifest test for kind keys (never reused), golden + round-trip + merge tests per kind, performance gate (APDPERF) per batch.

## 3. Performance and storage expectations (estimates, to be measured before and after each batch)
- Today: about 70 MB per 1,000 runs (archive estimate); a bespoke event costs 12-35 bytes, an attack with its damage 100-140 bytes; CPU ~1.5 microseconds per event.
- Instrumenting every action, buff change, gold change and cast would roughly double to triple log size (about 150-250 MB per 1,000 runs). Per-step movement is the one thing that
  would dominate (hundreds of KiB per long run), so movement, searching and regeneration-like high-frequency facts are recorded as per-floor summaries, not per step.
- Mitigations: string interning per log (kind keys cost a few bytes), varints, per-floor summary events for high-frequency facts, optional compression of sealed logs (this kind of data
  normally compresses several times over; to be measured), and the existing storage meter and export/archive tools. Nothing leaves the device.
- Query cost scales with rows (an Attacks dataset could reach 10^5-10^6 rows over many runs). Mitigations already in place and to extend: aggregate-first tables, per-dataset lazy build,
  row caches (reload today 20-60 ms), background build with the existing progress UI. Rule: every batch must stay inside today's budgets (phone-level view times, no gameplay hitch).

## 4. Exposing the depth on a phone without a desktop-database feel
- Ask, do not browse tables. A sentence builder as the front door: "Average [casts] of [Guiding Light] per [floor] by [difficulty]", each blank a chip picker that offers only what the
  player really has (with counts); the result is one sentence plus a small chart, the table underneath.
- Topic pages generated from the registry (Combat, Items, Character, Exploration, Economy, Deaths, ...), so a new character or mechanic appears by itself; empty kinds stay hidden.
- Any number is tappable down to the rows behind it (provenance preserved); compare two groups side by side; save and pin questions; short search over kind names.
- Progressive disclosure: simple by default, filters/group/sort/columns under "Options" exactly as they are now.

## 5. Preserve versus eventually redesign
Preserve: the event log format and versioning rules, skip-unknown, provenance marks, PUBLIC/INTERNAL enforcement, segment merging, the query engine core (filters, sorts, groups,
aggregates, scopes, minimum sample), the data-driven `ColumnDef`/`DatasetDef` approach, row cache, reports, archive export/import, legacy importer, Seed floors and floor replay,
ruleset/difficulty identity, self-tests, and the polished phone components (lists, dialogs, tour, guide).
Redesign over time: the four hard-coded datasets become registry-driven; the hand-written per-run rollups (`RunFactsBuilder`) become generic measures; the Runs table stops being the
front door in favour of the question builder; Guide/tour text generated from the registry; the fixed statistic sheets become instances of the measure engine.

## Suggested order once approved (nothing started)
U1 registry + generic fact event + context join + manifest/leak tests (no new gameplay hooks). U2 Floors dataset + "per" measures + event datasets for what is already recorded
(attacks, damage, heals, shields, XP, hunger). U3 choke-point instrumentation in batches, each with a perf/storage gate (actions, buffs, talents/subclass, gold/shops, spells,
level summaries). U4 the question builder and topic pages. Each step ends with core + API 26 + API 34 tests and one PHONE-CHECK build.

---

# Status: implemented on the claude branch (U1-U4), see below

## What exists now
- **Registry and generic fact event (telemetry 1.8).** `stats/FactKind`, `FactKinds`, `GameFacts` (the game's own kinds), `FactEvents` (FACT / FACT_INTERNAL / FACT_SERIES /
  FACT_SERIES_INTERNAL). A kind is declared once; its dataset, columns, filters, aggregates, topic table, question-builder choices and guide wording follow from the declaration.
- **Lossless high-frequency facts.** A SERIES holds every observation (each step, each rest turn). Columns are written as steps between rows with zero runs and a choice of
  steps-of-steps, so a step costs about 1.4 bytes (FactStorageTest: 20,000 steps = 27 KB) and the new facts of a 20,000-turn run total about 72 KB. Nothing is summarised, and no
  lossy trade-off was needed. Pending series are written before every save, floor change and run end, so a series never crosses a save marker (rollback depends on it).
- **Datasets:** Floors (every visit, quiet ones included, with counts of every fact kind and time played), Attacks, Damage, Healing, Shielding, Experience, Kills, Equipment
  (what was worn and for how long), and one dataset per registered fact kind. Each gets a table in the Tables picker automatically (sections "Play by play" and "Details: <topic>").
- **Measures:** count, total, average, highest, lowest, median over any recorded number, grouped by any recorded column, filtered by anything; `PerUnit` divides by runs, floor
  visits, turns, 1,000 turns or hours played, measured against the Floors table so quiet floors count.
- **Question builder:** Home card "Ask a question" (`WndUimsdAsk`, `Question`, `Questions`); "Open as a table" hands the same question to the full table tools (`UimsdModel.showQuery`). `UimsdTerminology` supplies one immutable offline vocabulary to view search, topic discovery and exact interpretation. Dataset/public-column/measure/connector aliases extend by registry copy rather than parser edits; internal columns and recorded values never enter the vocabulary, and unsupported or ambiguous wording fails closed (core verified Oct20, run39437010997).
- **Recorded in this pass (PUBLIC unless noted):** steps, rest turns, searches, containers opened, traps triggered, item actions (+ INTERNAL true item, level, charges), status effects
  by their shown name (+ INTERNAL true class of every effect), talent points, subclass and armor-ability choices, spell casts, armor-ability uses, every gold change with its cause, shop
  trades, played time. Plus row-level views of everything already recorded.

## How a new mechanic or character adds its history (no UIMSD change)
1. Declare a kind: `FactKinds.register(FactKind.builder("area.name", "Label").area("Topic").subject("What").value("key", "Label")...build())` (see `GameFacts`). A kind with hidden
   truth is `.internal()`; a high-frequency one is `.series()`. Add a line to `core/src/test/resources/apd/fact_kind_manifest.txt` (keys are permanent, fields only get added at the END).
2. Call it where it happens: `ApdFacts.record(kind, subject, values...)` (guarded, never throws, never touches the game's random numbers).
3. Done: it appears as a dataset, a table, a Floors count column, a choice in the question builder, and in reports' generic machinery. Public kinds must carry only what the player
   could know at that moment.

## Recording batches since U3
- Batch D (telemetry 1.9): `hero.state`, one observation per hero turn with health, max health, shield and hunger counter (series, full resolution). Measured 0.38 bytes/turn in the storage gate (FactStorageTest). Hunger is already public in the log (HUNGER_CHANGE), so the value is not new information. Hook: `PlayTracker.onHeroTurn` (no new game-code hook).
- Batch E (telemetry 1.9): `journal.landmark` (shops, alchemy, wells, floor feelings noted or dropped in the journal) and `journal.key` (keys picked up or used). Hook: 4 calls in `Notes` (`ApdPlay.onLandmark/onKey`); only what the journal already shows the player.
- Batch F (telemetry 1.9): `explore.discover` (secret doors/traps revealed, by searching or magic; hook in `Level.discover`) and `explore.well` (a well was used; hook in `WellWater.affect`). Revealed at the moment the player sees them, so nothing hidden is exposed early.
- Batch G (telemetry 1.9): `journal.page` (a journal page found, or read for the first time; hook in `Document.findPage/readPage`).
- Batch H (telemetry 1.12): `npc.interact` (each accepted hero interaction with an NPC, using only the name shown to the player and current floor; hook in the central `Hero.actInteract`). Quest progress remains the separate `npc.quest` event.
- The queued lever candidate was checked against the current source before Batch H. This game version has no lever class, terrain, message key or interaction path, so no fictional lever event was added.
- Coverage review (TEST203, no telemetry bump): all seven non-neutral level feelings are already shown in `GameScene` and immediately passed to their matching `Notes.Landmark.*_FLOOR`. The existing `journal.landmark` fact records that public, floor-specific observation exactly once; `NONE` emits nothing and revisiting an already-noted feeling cannot duplicate the fact. A retained behavioral test plus source-wiring guard now protects this boundary.
- Batch I (telemetry 1.15): `move.command` records each manual step, point-to-point travel or safe stair-navigation command once with its public destination, floor width and mode. The older per-cell `move.step.travelling` field is preserved and clarified: it marks physical walking versus instantaneous relocation. Exact-source review found no general auto-explore mechanic in this game version, so none was invented.
- Batch J (telemetry 1.16): `boss.phase` records the visible transitions of Goo, Tengu, DM-300, Dwarf King and Yog using only the displayed boss name, boss-specific phase number, floor and a public state label. Initial encounter state is implicit; restore-time reconstruction emits nothing.

## Not yet recorded (observable; candidates for the next batches, each needs its own hook and a hidden-information review)
Per-enemy state changes and enemy abilities, artifact charge (no public
accessor), plant and seed interactions, throws and targeting, shop stock seen, ally fates,
fall and trap damage sources beyond damage events, and anything a future character adds.

## UIMSD Demo Unlock (roadmap addition, owner request 2026-10-08; NOT started; lower priority than backend work and pending tests)
Players normally unlock UIMSD after 10 genuine runs. Alternative: unlock it immediately by generating 10 demonstration runs.

Warning shown first (exact text):
> ARE YOU SURE ABOUT THIS?
>
> UIMSD is an absurdly detailed statistics database. You can unlock it immediately with 10 demo runs, but there's a very good chance you'll have absolutely no idea what you're looking at.
>
> I strongly recommend playing 10 actual runs first.

Choices: **I LIKE CONFUSION** (generate demo runs) / **I'LL PLAY 10 RUNS** (decline, return to normal gameplay).

Requirements (owner's list):
1. Generate 10 varied, internally consistent demo runs that show UIMSD's statistics, filters, tables, grouping and cross-referencing.
2. Every generated run is marked DEMO throughout the data system.
3. Demo runs never count toward genuine run totals, achievements, progression, win rates, rankings, leaderboards or community submissions.
4. After a successful generation of the 10 runs the demo-unlock button is removed for good: it never returns, including after a restart or after the demo data is deleted.
5. UIMSD stays unlocked permanently after a successful generation, even if every demo run is deleted.
6. The player can delete all generated demo data at any time, with confirmation.
7. Deleting demo data never touches genuine histories, statistics or active saves.
8. Demo data is clearly identifiable and can be excluded from queries and tables.
9. Generation is offline, efficient and deterministic where practical; no ten full gameplay sessions are simulated.
10. Automated tests: demo isolation, unlock persistence, deletion safety, filtering, restart behaviour.
11. The normal 10-run unlock method is preserved.

What the repository already has (verified 2026-10-08, so this is less than a from-scratch build):
- `apd/demo/DemoData`, `DemoGenerator`, `DemoLog`: an INDEV-only synthetic history generator. Demo data lives in its OWN folder (a sibling of the real `apd` folder) with a marker file `SYNTHETIC-DEMO-DATA.txt`; every demo log is named `demo-...` and says SYNTHETIC-DEMO-DATA in its header, run start and version; the recorder refuses to record into a marked folder, the importer refuses to read or write one, the real dataset store skips any `demo-` log, exports/uploads check `isSyntheticRoot()`/`isSyntheticLogId()`, and `delete()` only removes a folder that has the marker AND the demo folder's name. Reports built from demo data say so (synthetic notice).
- The generator currently builds a larger sample history (about 68 runs for the UIMSD tour). The requested 10-run set is a smaller, deterministic variant of it.

Built so far (library + tests, no screen yet): `UimsdUnlock` (permanent unlocked flag, permanent demo-used flag, genuine-run count that excludes demo and unfinished runs, generate-10 with rollback on failure, delete demo data), `DemoGenerator.generateUnlockSet` (10 deterministic varied demo runs: nine finished, one still going, several classes, wins and losses; byte-identical every time), `UimsdUnlockTest` (7 tests: marked and varied, deterministic, never counted or queued for submission, button used up for good, unlock survives deleting the demo data and a restart, 10 genuine runs unlock for good, a failed generation changes nothing and never touches a foreign folder, deleting leaves genuine history and saves untouched). The gate is kept OFF by `UimsdUnlock.ENFORCED = false`, so nobody is locked out of UIMSD (for example while testing) until the owner says to switch it on.

Screens (written, gate still OFF): `UimsdGate` (locked window with progress and the demo route, the owner's warning and two choices word for word, generation on a background thread, then opens UIMSD showing the demo data), permanent flags in `SPDSettings` (`apd_uimsd_unlocked`, `apd_demo_unlock_used`), UIMSD Options > Demo data (switch between demo and your own history, delete all demo data with confirmation), demo data viewable in release builds once it exists. Every way into UIMSD goes through `UimsdScene.open`, which now asks the gate (the frozen title screen is unchanged). `UimsdGateTest` pins the wording. The gate being off means these screens cannot appear yet: switching `UimsdUnlock.ENFORCED` on is the owner's call, and the phone check below applies from then.

Clarifications from the owner (2026-10-08), built (gate still OFF):
- The title-screen arrow keeps its text and behaviour; while UIMSD is locked its message is exactly "THIS BUTTON IS EFFECTIVELY USELESS TILL 10 RUNS HAVE BEEN COMPLETED." (`UimsdGate.titleAnnotation`; the frozen title screen is otherwise unchanged and its hash test still holds).
- The warning appears ONLY when a player with fewer than ten genuine finished runs taps the locked UIMSD button: a pop-up card in front of everything (`WndDemoUnlock`; not an advertisement; no close button; Back means "I'LL PLAY 10 RUNS"). Never at startup, onboarding or in play. Buttons: "I LIKE CONFUSION" and "I'LL PLAY 10 RUNS".
- After a successful demo generation the option is gone for good and the button opens UIMSD normally; ten genuine runs also open it normally with no warning.
- The guided tour stays LOCKED until ten GENUINE runs. The demo route opens UIMSD but not the tour; demo runs never count toward it; once ten genuine runs exist the tour opens (also for a player who got in through the demo), and that persists across restarts, deleted runs and deleted demo data (`UimsdUnlock.isTourUnlocked`, flag `apd_uimsd_tour_unlocked`). The first-open tour offer is held back until it is available; asking for the tour while closed shows a short message. The reading Guide is not part of the tour and stays available. 3 tests plus the device test.

Second clarification (owner, 2026-10-08), built (gate still OFF):
- The 10th GENUINE completed run triggers a one-time title-screen prompt: the existing arrow points at UIMSD with "HEY, YOU SHOULD PROBABLY GO HERE NOW." Never for demo runs, never for the 11th or later, shown once and remembered (`apd_tenth_run_prompt_pending` / `_shown`), and it survives closing the game before the title was seen. UIMSD is not opened for the player; the tour is available when they enter it.
- How the "safe return to the title" is done: nothing is forced. `Dungeon.fail`/`Dungeon.win` call `UimsdGate.onRunEnded()` as their very last step, after the run's record is sealed; it only records the tenth-run state on its own thread and never changes scene, touches the save or interrupts death/ascension/ranking work. Every exit from the results screen (the exit button and Back) already goes to the title, so the player lands on the title by the game's own route and sees the prompt there (checked on emulators: Back from Rankings reaches the title in one press). If you want an automatic hop to the title in addition, say so; it would have to wait until the results screen is closed, to avoid cutting the results off.
- The pre-unlock arrow message "THIS BUTTON IS EFFECTIVELY USELESS TILL 10 RUNS HAVE BEEN COMPLETED." is unchanged. Tests: `UimsdUnlockTest` (genuine counting, demo exclusion, tour locking and unlocking, restart persistence, one-time prompt), `UimsdGateTest` (arrow texts in order: locked message, then the prompt once, then the usual text), `UimsdWiringTest.theTenthRunHookRunsOnlyAfterTheRunIsSealedAndNeverNavigates` (safe post-run behaviour).

Third clarification (owner, 2026-10-08), built (gate still OFF):
- The arrow after the tenth GENUINE run says "HEY, GO HERE FOR DRUGS." in Adult humor and "HEY, GO HERE FOR UNLIMITED DOPAMINE." in Clean humor. It replaces "HEY, YOU SHOULD PROBABLY GO HERE NOW." and stays on the title screen until the player opens UIMSD for the first time after that run; opening UIMSD retires it for good (`UimsdUnlock.promptActive` / `retirePrompt`). Before ten genuine runs the arrow keeps its locked message exactly.
- A player who got into UIMSD through the demo shortcut still gets this arrow (and the tour) at their tenth genuine run; exploring through the demo does not retire it, because it only exists once the tenth genuine run has happened. Demo runs never trigger it, count toward the ten, or open the tour.
- Humor setting: the handoff described an Adult/Clean preference but it was not in the code. It now exists as ONE shared preference (`SPDSettings.apdAdultHumor`, default false = Clean, used whenever age eligibility is unknown or unavailable; nothing infers it; only the player's own choice in APD Settings > "Adult humor (18+)" sets it). The one-time "are you 18 or older?" question described in the handoff is still not built; until then Clean is what everyone gets unless they tick the box.
- Tests: `UimsdGateTest` (both humor messages, locked message, persistence of the arrow until UIMSD is opened, demo-shortcut player), `UimsdUnlockTest` (arrow due until retired, restart persistence, demo exclusion), the APD Settings click test taps the new checkbox on a device.

What is NOT there yet (the work):
- **The 10-genuine-run unlock gate does not exist in the code today** (UIMSD is always reachable). Requirement 11 ("preserve the normal method") therefore means building that gate first. Working assumption until the owner says otherwise: a genuine run counts when APD recorded it completely and it finished (won, died or abandoned), imported legacy records and demo runs do not count. This is a small product decision recorded here, not made silently.
- A permanent unlock flag and a separate, permanent "demo unlock used" flag, both in settings, neither derived from the presence of demo files (so deleting demo data cannot re-lock UIMSD or bring the button back).
- The two-choice confirmation window with the exact text above, shown only while UIMSD is locked and the button has never been used.
- A 10-run generator mode that is available in release builds (the current one is INDEV-only), deterministic from a fixed seed, varied across hero classes, outcomes, depths and difficulties, and consistent (item, combat and floor facts agree).
- "Delete all demo data" with confirmation in UIMSD (uses `DemoData.delete()`), plus a toggle/filter to hide demo data in tables.
- Tests: demo isolation (no demo run in genuine counts, win rates, rankings, submissions, exports, the submission queue and the backend factory), unlock persistence across restart and after deleting demo data, the button never returning, deletion safety (genuine logs and an active save untouched), filtering, and the 10-run gate itself.
- Phone check: the warning text, both buttons, the unlocked state, and deleting the demo data.

Order of work: after the current backend and device-test items. Do not start it before the owner-approved higher-priority items are done or blocked.

## Absurd-query easter egg (recorded requirement, not built)
`Question.complexity()` is the hook: it grows with everything a question asks for. A rare acknowledgement line for an extreme score, Adult/Clean aware, is still to be built.
