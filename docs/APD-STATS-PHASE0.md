# APD statistics, Phase 0: test harness and seeded-level baseline

Phase 0 changes no gameplay and no save format. It adds test infrastructure and records how dungeon generation
behaves *before* any statistics recorder exists, so later phases can prove the recorder does not change it.

## What was added (all test-side, plus docs)
- `core/build.gradle`: test-only dependencies `gdx-backend-headless`, `gdx-platform:natives-desktop` and `org.json`
  (JUnit was already there). The `test` task runs from `core/src/main/assets` (the game loads message bundles through
  `Gdx.files.internal`) and passes the golden-file settings as system properties.
- `core/src/test/java/.../apd/HeadlessTestSupport` starts libGDX without a window. Everything the game writes goes to a
  temp folder, never the repo.
- `LevelGenerationHarness` runs real level generation (`Dungeon.init`, `Dungeon.newLevel`) for a seed and fingerprints
  each level.
- `GoldenCases`, `GoldenChildMain`, `GoldenLevelHashTest`, `LevelGenerationSmokeTest`.
- `core/src/test/resources/apd/golden_levels.txt`, the golden baseline.
- `core/src/main/assets/apd/apd_records_history.txt`, the versioned Records history document (not shown in the game yet).
- `docs/APD-STATS-SCHEMA.md`, the approved architecture requirements for the statistics system.

## Does headless level generation work?
Yes. All 26 depths generate headless for every case, with no game window, no sprites and no Android SDK.

## The baseline
11 dungeons x 26 depths = **286 levels**, generated from the code as of build 8 (commit `3e4c63a0e`).
Cases: five plain seeds, one with the Mage class, one with all challenges, one daily-style seed (above the
user-enterable range), and veteran-journal versions of three of them.

A level's fingerprint is the SHA-256 of its full saved state (map, doors, mobs, heaps and items, plants, blobs...) written
as JSON with sorted keys.

- **242 levels are STABLE**: identical in every one of 24 fresh JVMs that were deliberately perturbed. These are compared.
- **44 levels are UNSTABLE**: they came out differently depending on JVM history. These are recorded but not compared.
  They are mostly the daily-style seed (20 of its 26 levels in the fresh-journal case) plus 2 to 3 levels in each other case.

`GoldenLevelHashTest` runs the generation in a fresh JVM and fails if any STABLE level differs.
It passed 12 times in a row locally in fresh JVMs.

## Findings: level generation is not a pure function of the seed
Each of these was found while trying to get the same seed to give the same fingerprint twice.

1. **Journal state changes levels.** Which guidebook / alchemy / lore pages the player has found
   (`Document`), and `SPDSettings.intro()`, change what gets placed. The harness therefore always starts from one of two
   fixed contexts, FRESH (new install) and VETERAN (intro off, guide pages found).
2. **Unseeded placements.** `EntranceRoom.placeEarlyGuidePages` uses an unseeded generator on purpose, so the floor 1
   guidebook and the floor 2 "Searching" page land somewhere new every time (sometimes stacked on another item).
   These two items are removed before fingerprinting.
3. **`MissileWeapon` `set_id`** is a random identifier that differs per generation. Ignored.
4. **Shop bags.** `ShopRoom.ChooseBag` breaks ties by iterating a `HashMap` keyed by bag objects, so which bag a shop sells
   can differ between runs. Every bag is fingerprinted as just "a bag". The choice also depends on the hero's inventory.
5. **Painting extras.** The traps/decoration/grass pass moves a few dozen tiles between generations. Traps are left out and
   decorated tiles are folded into their plain variant. Layout, doors, water, stairs, items and enemies stay exact.
6. **JVM-history dependence.** Some levels differ depending on the history of the JVM that generated them (consistent with
   identity-hash ordering in hash collections). This is why the baseline is recorded over many perturbed JVMs and why
   44 levels are marked UNSTABLE. The cause was not tracked down further.

What this means for the statistics system: a run's dungeon depends on more than its seed, so the recorder should store the
generation context (journal state, intro flag, hero inventory at shops) next to the seed rather than assume the seed is
enough. Replays of a seed are only approximately reproducible.

## Limitations
- The baseline covers level generation only (layout, items, enemies as placed), not combat, item behaviour or a played run.
- The normalisations in findings 2 to 5 mean a recorder change that only affected those things would not be detected.
- STABLE means stable across 24 perturbed JVMs on one machine. A different JDK or machine might expose more variance.
  If CI ever fails the golden test on an untouched generation, treat that level as UNSTABLE and re-record.
- The test starts child JVMs (recording: 24, comparing: 1), so it needs a `java` that matches `java.home` and takes
  about 10 seconds to compare.

## Commands
- Run everything: `./gradlew :core:test`
- Re-record the baseline (only after a deliberate generation change; takes about 80 seconds):
  `./gradlew :core:test -Papd.golden.update=true`
