# APD difficulty system

Levels 1-5. **3 = the game as it is** (the baseline; it never changes). 1-2 easier, 4-5 harder.
**Balance revision 1 (first real pass)** is active for levels 1, 2, 4 and 5; level 3 stays revision 0 and exactly current gameplay.

## The first balance pass (approved values)
| Level | Enemy HP | Enemy damage | Resources |
|---|---|---|---|
| 1 | 70% | 70% | 140% |
| 2 | 85% | 85% | 120% |
| 3 | 100% | 100% | 100% (exactly current APD) |
| 4 | 120% | 120% | 85% |
| 5 | 150% | 150% | 70% |
Neutral on purpose (not part of this pass): healing, food, enemy spawn rate/number, bosses, champions, traps. Challenges are a separate system and unchanged.

## Exactly what each active modifier touches
- **ENEMY_HP**: the health of ordinary creatures when `Level.createMob` makes them (a level's population and later respawns). Applied once, at creation;
  a loaded level keeps the health its creatures have. Not bosses, not creatures made another way (mimics, statues, quest givers, summoned minions).
  Champion bonuses are applied before scaling, so a champion's health scales with the rest.
- **ENEMY_DAMAGE**: the damage of an attack resolved by `Char.attack` made by a hostile creature that is not a boss. Not magic, gas, fire, traps or other
  damage that bypasses `Char.attack`, and never the hero's or allies' attacks.
- **RESOURCES**: the NUMBER of ordinary random items `RegularLevel.createItems` places (its 3-5 Generator draws, +2 on a Large level), changed before
  the draws. NOT scaled: the guaranteed per-floor items (food, strength potions, scrolls of upgrade, stylus, enchantment/intuition stones, trinket catalyst),
  special rooms and shops, quest rewards, bones, journal pages, monster loot, gold, unique items and boss floors. So no seed can lose progression items.
- **Rounding**: `round-half-up(value * modifier)` with a 1e-9 allowance so exact halves (5 x 0.7 = 3.5) always round up; item counts never drop below 1; health
  never below 1; damage is a float. Deterministic: no random number is drawn, so difficulty 3 consumes exactly the random numbers it always did. Every
  integer 0-600 is pinned for each percentage in the tests.

## Architecture (one place to edit)
- `apd.difficulty.Difficulty`: levels, `Modifier` enum, the table, balance revisions. Level 3 returns 1.0 whatever the table holds (tested with a corrupted table).
- `apd.difficulty.DifficultyHooks`: the three seams: `Level.createMob` (HP), `Char.attack` (damage), `RegularLevel.createItems` (item count). Gameplay asks
  for a Modifier, never for the level; a test fails if gameplay code reads `Dungeon.difficulty` directly.
- Next systems (healing, food, spawns, bosses, champions, traps) = set the table, add one hook call, bump the revision.

## Identity and persistence
- Setting `SPDSettings.apdDifficulty()` (new runs only). `Dungeon.difficulty` is set when a run starts, saved with the run (`apd_difficulty`), and never follows the
  setting again. A save from before difficulty existed loads as `difficultyKnown = false`: it plays as 3, is never labelled with a difficulty.
- Ruleset: difficulty 3 adds nothing to the rule text (its id is the id those rules had before). Other levels add `difficulty=N;balance=R` (R = 1 now), so each
  level and each balance revision is its own ruleset; runs recorded under the earlier placeholder revision 0 stay revision 0 and are never mixed with revision 1.
- Telemetry 1.7 stores difficulty and balance revision in RUN_START (0 = not recorded). UIMSD: runs columns Difficulty and Balance revision (`?` for older runs),
  view "Difficulty" (grouped by both).
- `FloorReplay` uses the run's difficulty (`ReplayContext`), so seed floors and the recorded-vs-canonical comparison follow the run's rules; a replay is
  tested equal to the game's own generation at every level.

## Verification
Pinned values and revisions; rounding for every integer and every boundary; item counts for bases 3-7 at all levels; HP and damage at all levels (bosses and
allies unaffected); level generation at 3 unchanged (golden); other levels change only creature health and ordinary loot (guaranteed items identical on every
floor, floor-1 creatures identical); loot totals fall as difficulty rises; replay equals the game at every level; save/load keeps a run's difficulty when the
setting changes; legacy saves stay unlabelled.

## Per-run balance revision (stability pass)
A run stores its difficulty AND the balance revision it started with (`Dungeon.difficultyBalance`, save key `apd_difficulty_balance`; a save with a
difficulty but no revision = revision 0 = no modifiers; a save with neither = legacy, plays as 3, unlabelled). Modifiers, ruleset id, telemetry and floor
replay all use the run's own revision, so a later balance pass never changes a run in progress or its replay. `Difficulty.modifier(level, revision, m)`;
old revisions stay in the class. Seed floors refuse imported records, runs whose challenge settings were not recorded, and unknown revisions.
UIMSD opened from a running game never starts a floor replay (live-game check).

## Exotic setting identity
Like difficulty, the exotic chance is part of the run (`Dungeon.apdExotic`, `apd_exotic` in the save). Saves without it are "not recorded" (never today's value).

## Non-P2W (permanent)
Difficulty is a free gameplay setting. Nothing may ever be sold that selects, unlocks or alters it or its results.
