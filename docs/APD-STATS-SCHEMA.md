# APD statistics / records: approved architecture requirements

This records the decisions made before implementation. Core rule: **"Record broadly. Query freely. Cross-reference
everything."** Raw evidence is the source of truth. Rollups, fact tables and indexes are caches that can be rebuilt.
The system is named **UIMSD, the Ultimate Infinite Mega Statistics Database** (decided after Phase 2; the title screen
button says "UIMSD"). "Statistics" below still describes what it does. Its name can change again only if the project owner says so.

## Telemetry version
The recording system has its own version, "Telemetry 1.0", 1.1, 2.0 ..., independent of the APD game version
(`TelemetryVersion`). Every run records the version it was recorded under in its log header and keeps it for ever.
Minor versions only add; a major version changes the meaning of existing data. Readers may derive newer representations
but never relabel old runs. Each version has a milestone in the Records history. Details in `docs/APD-STATS-STORAGE.md`.

## Storage and recording
- New files under an `apd/` folder, one append-only, length-prefixed event log per run plus a small run index. SPD's own
  files (`rankings.dat`, `badges.dat`, `journal.dat`, saves) are not changed.
- Recording is ON by default. No per-turn or movement events by default (the event registry must allow adding them).
- The recorder must never alter gameplay, consume SPD's seeded RNG, mutate game state, or make a run fail if it errors.
  Disk writes and heavy derivation stay off the game thread where practical.
- No automatic deletion of raw history. Storage meter, manual archive/export tools, size warnings. Any future automatic
  archival must be opt-in and say what would stop being queryable.
- Export/import (a single ZIP is fine) is required before public 1.0 and must carry raw logs, run records, checkpoints,
  schema/version info, provenance and indexes. Derived caches are not authoritative.

## Save changes (the only two approved)
- `apd_run_id` in the slot's game bundle, linking the run across saves and restarts.
- `apd_uid` on every item (run-scoped ID).
- Both optional and backward compatible: old saves and items without them keep working.

## Item lifecycle (stages are never collapsed)
GENERATED, SEEN, ACQUIRED (picked up), EQUIPPED, UNEQUIPPED, and where applicable USED / CONSUMED / THROWN / DROPPED /
UPGRADED / IDENTIFIED / TRANSFORMED.
- `ITEM_SEEN` means the item became observable to the player under normal gameplay information rules, not merely that it
  existed on the level.
- Preserve the player's knowledge state at that moment. An unidentified ring seen on floor 4 stays "unidentified ring" for
  that event; the later IDENTIFIED event records when the truth became known.
- EQUIPPED has its own turn, depth and context. Repeated equip/unequip cycles must be supported, with equipment intervals
  derived from the events, not inferred from possession.
- Lineage: run-scoped item IDs plus explicit parent/child links for split, merge, duplicate, crafting, transformation and
  similar operations. (`Item.split()` and `duplicate()` copy items through a bundle, so a stored ID must be reassigned.)
- Must support queries such as: carried how long before first equip, which floors it was equipped on, turns equipped, what
  happened while equipped, seen but left behind, acquired but never equipped.

## Healing
HEAL events should carry source, exact amount, overheal, target, turn and context (done in telemetry 1.6: see APD-STATS-COMBAT.md, "Exact healing"). Inferred healing is allowed during
development only if every such event is explicitly marked inferred. Significant sources must be instrumented exactly
before public 1.0, incrementally, with no giant rewrite of HP handling. Rare unavoidable inferred events stay marked.

## Rolled-back timelines
Events after the last valid SAVE marker that disappear because an earlier save was reloaded are kept as an abandoned
branch: excluded from normal statistics by default, never silently merged into the canonical timeline, never silently deleted.

## Rulesets
Separate concepts: ruleset / ruleset variant, official APD gameplay settings, and `externalModsPresent`. These feed the
ruleset identity: APD-0001 (starvation consumes shielding), Cleric blocked, exotic crystal conversion chance, challenge
configuration, future gameplay-affecting APD settings and mods. Changing an official APD setting changes the ruleset but
is not "modded". Queries must not silently mix incompatible rulesets.

## Legacy and incomplete data
Only data that exists is stored. Unknown stays unknown/null, never zero, never guessed without an explicit provenance
marker. Queries show coverage ("17 of 28 runs contain this statistic"). The old `totalNumber` / `wonNumber` appear as
separate aggregate historical information. Importing from a separately installed stock SPD is a later possibility (Android keeps it
in a separate sandbox, so it needs a user-provided file; not done in Phase 6); keep the model able to hold it. Per-field provenance/known status is required.

## Event schema extensibility
Versioned and append-only. Old readers must skip unknown future event types. An event type ID is never reused with a
changed meaning. Stable identifiers for runs, items, actors, event types, rulesets and datasets/fields.

## Checkpoints
Compact summaries, not full save states. At minimum at: run start, level enter, level exit, game save, run end.

## 1.0 scope
Datasets (as views over shared facts, not duplicated storage), where the recorded data supports them: Runs, Characters,
Enemies, Weapons, Armor, Rings, Trinkets, Challenges, Deaths; also Artifacts, Bosses and others if they fall out of the same
model. Sorting, filtering, repeated filters, minimum sample size, useful columns, dataset switching, clear sample counts,
handling of missing data, ruleset/version provenance, local statistics, raw history, export/import.
May wait: global/uploaded statistics, accounts/cloud sync, full custom-expression language, saved queries, comparison UI,
anti-cheat, every dataset, per-turn telemetry. The schema and query format must be able to add them without invalidating
old runs.

## Records history
A versioned, maintainable feature history separate from the APD changelog, explaining what SPD retained, why APD keeps
history, and when raw events, item identity/lineage, new datasets/queries, legacy imports and global statistics arrive, and
why older runs hold less. Stored as `core/src/main/assets/apd/apd_records_history.txt` (one block per milestone, newest
first, milestone ids never reused). No History UI until the UIMSD screen exists.

## Title button
When the system is ready, only the click target of the existing title button changes (from Rankings to the new scene). The
title screen visuals stay frozen.

## Phases
0 harness and baseline (done) - 1 storage library, no game hooks (done) - 2 run lifecycle, `apd_run_id`, rollback handling -
3 item identity and lineage - 4 combat/health events - 5 generation provenance - 6 importer and derivation - 7 query
engine and generic UI - 8 export/import and storage tools - 9 exact healing, benchmark, title button.
Each phase waits for approval.
