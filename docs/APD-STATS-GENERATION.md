# APD stats: generation provenance (telemetry 1.4)

Phase 5. Records what the game generated for each level and where every generated item came from. Everything here is
**INTERNAL** (never player-visible): it carries true identities. The player-facing item events (Phase 3) are unchanged.

## Events
| id | name | meaning |
|----|------|---------|
| 291 | LEVEL_GENERATED | one per freshly built level: level class, feeling, build attempts, draw count, heap/item/mob/trap counts, intro flag, exotic chance, challenges, guide pages found, hero items, room list, `LimitedDrops` counters, category deck counts |
| 292 | ITEM_GENERATED | one per item on the level: `apd_uid`, container (heap, chest, locked chest, crystal chest, skeleton, tomb, mimic, statue, armored statue, quest reward, mob loot, bonus drop), position, heap type, room, owner, build stage, draw kind (random / category / defaults / class / artifact / none), requested and actual category, deck count, true class, true level, cursed, enchant, quantity |
| 293 | MOB_GENERATED | one per mob: class, position, hp, state, alignment, champion types, room, carried item count |
| 294 | MOB_ID_ASSIGNED | the game's actor id for a mob, emitted the first time the game assigns it (join key for combat events) |

Generated items also get `ITEM_REGISTERED` with origin `GENERATED` (ItemEvents.ORIGIN_GENERATED = 7), so the item lifecycle
GENERATED -> SEEN -> ACQUIRED -> EQUIPPED... joins on one uid. A generated item is *not* recorded as seen.

## Hooks (all behind `ApdGen.active`, all never-throw)
- `Dungeon.newLevel()`: `ApdGen.onLevelGenerated(level)` right after `level.create()`; the finished level is scanned read-only.
- `Level.create()`: start marker, per-attempt marker in the build retry loop, stage labels (mobs / items).
- `Generator`: the six public draw methods are thin wrappers around the original code (renamed `...Impl`). Only the outermost
  draw is recorded; nested draws are folded into it. This is the only place the requested category and deck count are knowable.
- `Mob.rollToDropLoot`, `Heap.open` (bonus drops): loot and bonus items.

## Not recorded / limitations
- The level seed is not stored: it is derivable from the run seed and depth (replicating it would need the game's RNG).
- Play-time loot (monster drops, chasm falls, ImpShop, resurrection regeneration) is unseeded in the game and only
  covered where hooked (mob loot, bonus drops).
- A level build that is retried discards its draws; only the final attempt is described (`buildAttempts` says how many).
- Hero-dependent and unseeded randomness is what it is: this records the outcome, it never tries to reproduce it.

## Findings worth knowing
- `Actor.id()` lazily assigns ids from a global counter that is saved game state. Calling it from telemetry changed the
  game. The recorder now uses `Actor.apdPeekId()` (read-only) everywhere, and `RecorderIsolationTest` bans `.id()` in the
  recorder package. Caught by the recorder-on golden test.
- Generated items now carry `apd_uid` in their bundle. The golden harness skips that key when fingerprinting (like
  `set_id`); the golden file itself is unchanged.
- Which mob receives which actor id is not stable between JVM runs even without the recorder (the set of ids is).

## Safety
Noninterference is tested by `GenerationSafetyTest` (decks, limited-drop counters, mob id set, next random numbers
identical with recording on, off and failing), the recorder-on golden test, and the isolation source scan.
