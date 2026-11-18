# Floor replay (canonical seed floors)

Phase A of the future-floor feature (study: APD-FUTURE-FLOORS-FEASIBILITY.md). Decisions: canonical, always labelled; unreached floors
of FINISHED runs may be interrogated; replay only when no live game; recorded telemetry is authoritative for reached floors.

## What it returns
`FloorReplay.replay(seed, challenges, heroClass, maxDepth)` -> `List<ReplayFloor>` for floors 1..N (max 26, main branch only).
Floor N = what the seed gives after floors 1..N-1 were generated in order under the **reference context** `ref1-classkit`:
- reference hero: the run's class with its starting kit (the kit decides shop bags), no talent points, no bones, no play-time loot;
- reference journal: Adventurer's Guide + Alchemy Guide pages found/read, no lore page found; tutorial off;
- the run's challenges; APD exotic chance as set by the generator.
It is never "what this run would have met". Floors the run reached come from the run's own telemetry (events 291-294).

## Proof of fidelity
`FloorReplayTest.replayMatchesTheGamesOwnGenerationFloorForFloor`: 6 seeds x all 6 classes x 26 floors, SHA-256 of each full saved
level equals the game's own generation (harness context REFERENCE). Also repeatable, prefix-stable, content checks.

## Isolation
- Refuses while a game scene (GameScene/InterlevelScene) is on screen or while another replay runs. Every recording hook is switched off
  while it runs (`ApdRecorder.suspendHooks/resumeHooks`), so an open run log (a run saved and left) can never receive replay events.
  `Dungeon.init`/`loadGame` call `FloorReplay.awaitIdle()` so a game start never overlaps a replay.
- Reference journal/tutorial are answered only to the replay thread (`ReplayContext`): `Document.isPageFound/isPageRead`,
  `SPDSettings.intro`. Nothing is written; `Catalog.setSeen` is a no-op on the replay thread; the kit is built while the hero counts as
  not alive so no badge/discovery is recorded (`neverRecordsDiscoveriesOrBadgesForThePlayer`).
- Puts back: Dungeon fields, the three item identity tables, limited drops, item generator, statistics, quests (reset first: restore
  only sets keys it finds), Imp reward list, notes. `leavesTheDungeonStateAsFound`.
- `ReplayStaticAuditTest` diffs every static in levels/items/actors/journal/... before/after: only three scratch statics may differ
  (SecretRoom.regionSecretsThisRun, ArmoryRoom.prizeCats, SpecialRoom.floorSpecials), each rebuilt before use.
- Bug found by the tests: quest restore left the replay's Ghost enchant/glyph behind (fixed by resetting before restore).

## Limits
Branch levels (mines 11-14, vaults 16-19) are not generated. Bones, daily seeds' calendar state, hero talents and play-time loot are not
modelled. An upstream change to level generation changes results: bump `ENGINE_VERSION` / the golden test guards it.

## Phase B/C: Seed floors in UIMSD
- Entry: Runs row of a finished run -> "Seed floors (canonical)". Needs seed + hero class in the run's row (older/imported runs without them say so).
  The exotic chance and challenges are the run's own (the replay never reads the install's setting: `ReplayContext.exoticChance`).
- Pure assembly `apd.query.SeedFloors` (datasets seedfloors / seedmobs / seedcompare, views of the same names); game-aware
  `apd.replay.SeedFloorsRunner` (replay + in-memory cache of 4 runs). `UimsdModel.setSeedFloorsProvider` keeps the generic model free of game code.
- Provenance: column `Basis` on every row: Recorded (the run's log, authoritative), Canonical (floor never reached), Canonical (reached,
  not recorded), Canonical (reach not known). Canonical cells use the ~ mark (PROV_INFERRED). A floor with any recorded item shows ONLY the log's
  items; its canonical items appear only in the comparison. Creatures are listed for canonical floors only.
- Comparison (seedcompare): per recorded floor, recorded vs canonical item counts, same / only-recorded / only-canonical, agreement %.
- Only finished runs; a loaded/live game gives a notice, never a replay. Everything is INTERNAL (hidden-information) columns, opened with
  hidden information on.
- Tests: SeedFloorsTest (provenance, authority of recorded floors, comparison maths, model flow, live-game notice), device repeatability + assembly.

## Found by CI on API 34: the random-number stack is shared by every thread
`com.watabou.utils.Random` keeps ONE generator stack for the whole process. A replay seeds it, but the render thread draws random numbers
every frame (and pushes/pops generators), so on a running app the replay's numbers were being consumed by the UI: the same seed gave a
different floor 2 the second time. Fix: `Random.isolateThread()/releaseThread()` give the replay thread its own stack (`ReplayContext.enter/exit`);
all other threads keep the shared one. `otherThreadsUsingRandomNumbersCannotDisturbAReplay` fails without it (mutation-checked).
