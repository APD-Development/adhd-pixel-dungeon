# APD statistics: item identity and lineage (Phase 3, telemetry 1.2)

Every item the recorder meets gets a persistent id, `apd_uid`. Events about items refer to it, so one item can be followed
across pickups, equipment changes, saves, level changes and app restarts. Nothing here changes gameplay or uses SPD's seeded random numbers.

## The one new save field
`apd_uid` (string, 16 hex digits) on an item's bundle, written only when the item has one. Old saves have none, and items
simply stay without an id until the recorder meets them. This is the second (and last) save change APD makes for statistics.
Ids come from `UUID.randomUUID()` (its own generator), never from `Random`, and are checked for clashes within the run.

## When an item gets an id
Lazily, the first time it is observed: held by the hero, seen on the floor, split, merged, transformed, crafted or detached.
Items nobody has met carry nothing (generation-time provenance is Phase 5). Quickslot placeholders (quantity 0), Gold, Dewdrops,
Energy Crystals and Keys have no identity (they are never separate objects in the backpack).

## What "seen" means
`ITEM_SEEN` is written once per item, only when all of these hold: the heap is of type HEAP or FOR_SALE (chests, locked and
crystal chests, tombs, skeletons and remains show no contents), the heap has been seen and is not just the faint outline of a
spyglass hint, its cell is in the hero's view right now, and the item is the top of the pile (items underneath are seen once
uncovered). Items carried by monsters, inside containers, or under magic mapping are not "seen". Crystal chests tell a tapped player the
category of their top item; that is not recorded.

## Public and internal events
| Event | Visibility | Meaning |
|---|---|---|
| ITEM_SEEN, ITEM_ACQUIRED, ITEM_LEFT | PUBLIC | became visible / entered / left the hero's hands (reason and action when known) |
| ITEM_EQUIPPED, ITEM_UNEQUIPPED | PUBLIC | one event each, with slot, depth, turn and the state right after the change |
| ITEM_KNOWLEDGE | PUBLIC | what the player knows about a held item changed (level revealed, curse revealed, identified) |
| ITEM_CLASS_KNOWN | PUBLIC | the player learned what a whole potion colour, scroll rune or ring gem is |
| ITEM_REGISTERED | INTERNAL | the item got its id; records its true class, level, curse and enchant |
| ITEM_LINEAGE | INTERNAL | split, merge, copy, transform, craft links between ids |
| ITEM_TRUTH_CHANGED | INTERNAL | the true level, curse or enchant changed (the player may not know) |
| CHECKPOINT (from 1.1) | INTERNAL | stores true item levels |

Every PUBLIC item event carries a **visible state**: category, the name the game shows ("ruby ring" until identified), quantity,
and level or curse only when the player knows them. The encoder refuses to store an unknown level or curse. Old events are never
rewritten: an unidentified ring seen on floor 4 stays "ruby ring" in the log even after it is identified as a +3 Ring of Wealth;
the identification is a later `ITEM_KNOWLEDGE` event. `EventTypes.visibility(type)` classifies every event; player-facing
views, player exports and run-replication packages must drop INTERNAL events or filter them with the knowledge events first.

## Identity-changing operations (what SPD does, and what is recorded)
| Operation | SPD behaviour | Recorded |
|---|---|---|
| `split(n)` (throw one, drink one, sell one, alchemy slots) | copies the item through a bundle, which would copy the id | the piece gets a **new** id, `ITEM_LINEAGE` SPLIT parent -> piece |
| stack merge (`collect`, `Heap.drop`, `Bag` restore) | the absorbed object disappears | MERGE absorbed -> survivor; the survivor keeps its id, the absorbed id is retired |
| `duplicate()` | bundle copy, also used for throwaway previews (alchemy refresh) | the copy has no id until observed; then its own id and a COPY link. Previews never become items |
| transmutation, class armor upgrade | a new item replaces the old | TRANSFORM old -> new |
| alchemy | ingredients are consumed, a product is made | CRAFT ingredients -> product, ingredients leave as "crafted" |
| two live objects with one id (monster clones made by bundle round trip) | | the second becomes a COPY with a new id |
| bones from another run | the dead hero's items carry that run's ids | `Item.reset()` (which bones call) clears the id |
| in-place changes (upgrade scroll, enchant, reforge survivor) | same object | same id, ITEM_TRUTH_CHANGED / ITEM_KNOWLEDGE |

## How changes are found
SPD has no single place where equipment slots change (about 40 writes bypass `doEquip`/`doUnequip`), so the recorder looks at
the hero's belongings and equipment slots at the start of every hero turn, before every save, at every level change and at run
end (cost: about 40 microseconds on a desktop with 20 items). Small hooks in `Item` report what looks are blind to: splits, merges,
detaches (with the action that caused them), throws and drops. `Hero.act`, `Dungeon.saveGame`, `AlchemyScene`,
`ScrollOfTransmutation`, `ClassArmor` and the potion/scroll/ring discovery methods have one-line hooks as well.
`ItemHookWiringTest` fails the build if one is removed.

## Rollback and resume
On resume the tracker rebuilds its memory from the canonical (not abandoned) events up to the loaded save, then compares it with
the game. Differences are written as events flagged `FLAG_RECONCILED` (the time is only an upper bound). Events after a matched
SAVE are the abandoned branch: kept in the log, excluded from `RunTimeline.canonical()` and so from `ItemHistory` by default.
Ids are never reused, so an item first met in an abandoned branch cannot collide with one met afterwards.

## Derived views
`ItemHistory` (`visibleStateAt`, `equipIntervals`, `ancestors`, `allUids`) is computed from the events every time and stores nothing.

## Limitations
- Items nobody observed have no id and no events (by design until Phase 5).
- `ITEM_LEFT` reasons depend on `Item.execute` announcing the action; a subclass that acts before calling `super.execute`
  leaves reason UNKNOWN. Selling, stealing and burning are not named yet.
- The checkpoint's item list has no ids (its format is from 1.1 and is not extended).
- Items inside a Mage's staff (the wand), a Blandfruit's potion, a sealed armor seal and similar nested items are not tracked separately.
- Items held by monsters, Thief loot, Mimic contents and Statue weapons are not seen until dropped.
- Missile weapons can lose quantity in SPD's own durability merge rules; the MERGE event records the absorbed amount.
- A run recorded under telemetry 1.1 continues in a new log segment after updating (a run keeps its telemetry version).
- Real device behaviour of the hooks (as opposed to the storage self-test) has only been exercised through the game classes in headless tests.
