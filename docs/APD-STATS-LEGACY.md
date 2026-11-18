# APD stats: rankings import and derived facts (telemetry 1.5)

## Scope: APD's own rankings only
The "Import APD rankings" button and the automatic import read **APD's own `rankings.dat`**: the file APD's copy of SPD's `Rankings`
class writes inside APD's private app storage when a run is finished in APD. They do **not** read official Shattered Pixel Dungeon.
Verified in this repo: official SPD's application id is `com.shatteredpixel.shatteredpixeldungeon`; APD's is
`com.charltonchris29.adhdpixeldungeon` (the downloadable debug build adds `.indev`). They are separate Android apps. On Android,
`AndroidLauncher` sets the game's file API to libGDX "Local" storage (the app's private internal storage), APD declares no storage
permission, and an app cannot read another app's private data on an unrooted phone. So the official app's history cannot be imported
directly, and there is nothing in APD that tries to. If APD's rankings file does not exist yet (no run finished in APD) the button says so.
"Legacy" in the code (`LEGACY_*` events, `legacy/` folder, `legacy.*` facts) is only the name of this import path; those identifiers are
stable and unchanged. A possible import of external official-SPD history is deliberately not part of Phase 6.

Phase 6. Two jobs: keep what SPD itself remembered about old runs, and derive a table of per-run facts from the raw data.
Raw data stays the source of truth. Everything derived can be deleted and rebuilt, and rebuilding gives the same answer.

## What the rankings file holds (survey of the SPD code APD is built on)
- `rankings.dat`: the best **11** finished runs (a "Record" each: class, win, depth, level, armor tier, score, killer, custom seed,
  daily flag, date and SPD version when the record is recent enough, a game id), plus `totalNumber` / `wonNumber` counters that
  keep counting runs whose records were dropped, plus the latest daily record and the daily score history.
- Each record carries `gameData`: the hero (with equipped items), the game's `Statistics` (gold, deepest floor, kills, food,
  crafting, upgrades, duration, score parts...), badges, handlers, challenges, game version and (newer records) the seed.
- Runs that were abandoned never get a record. Runs older than the 11th are gone for good: the import preserves what is still there.
- A record can lack fields (no date/version, no id, none of the newer statistics, no seed) if the file holds records written by older
  versions of the code. SPD's own readers turn a missing number into 0. The importer does not.

## Import (`LegacyImporter`, "Import APD rankings" in APD Settings)
- Reads the rankings bundle as plain data. No game object is created, `Rankings` is not touched, `rankings.dat` is not changed.
- Writes raw import events (`LEGACY_RECORD`, `LEGACY_TOTALS`, `LEGACY_DAILY_SCORE`, events 295-297) into a new
  `legacy/import-<time>.apdlog`. Import files are immutable like run logs; an import with nothing new writes no file.
- A field is written as (key, kind, value). A field the old record did not contain is written as **UNKNOWN**; a recorded zero is a zero.
- Records are recognised by SPD's game id, so importing twice adds nothing. A record without an id gets a stable id from its content.
- Totals: SPD cannot tell a total of 0 from "never stored" (it recounts when 0), so a total of 0 is imported as unknown.
- Imported statistics keep SPD's names under `legacy.*` (`legacy.enemiesSlain` is SPD's counter, not APD's kill count). Overlapping
  meaning uses the shared names (`result.score`, `run.heroClass`, ...). Final equipment is `loadout.<slot>` with level, curse and enchant.
- Not a button on the title screen (frozen). The button in APD Settings is still there.
- **Automatic import.** SPD only keeps 11 records, and `Rankings.submit` trims the table when a run ends. So the rankings file is read
  twice per finished run: right before `Rankings.submit` (`Dungeon.fail` / `Dungeon.win` call `ApdRecorder.onBeforeRankingSubmit()`), so a
  record about to be dropped is copied first, and again after the run's log is closed, to pick up the new record. The file is read
  on the game thread (a few small reads, read-only); the history files are written on a background thread. Writes are serialised and
  records recognised by id, so the button, the two automatic imports and repeats can overlap without writing a record twice.
  Any problem (missing or broken file, unwritable history) is dropped silently: it is not counted as a recorder error and never
  touches the run. Nothing is imported when recording is off. Runs APD was not following still get their record imported.

## Linking a record to an APD run
APD runs now write a `RANKING_LINK` event (298, INTERNAL) right after `RUN_END` with the id of the record SPD just wrote, so the
same run is never counted twice (exact link). Older APD runs have no id: a record is linked as PROBABLE only if exactly one finished
APD run has the same class, outcome, deepest floor, level and score. Ambiguous matches stay unlinked. Links are marks on derived rows;
nothing is merged or deleted, and `FactTable.rows()` counts a linked run once while `allRows()` keeps every row.

## Derived facts (`RunFactsBuilder`, `FactKeys`, `FactTable`, `DerivedStore`)
- One `RunFacts` row per run log segment and per imported record. A fact is a number or text plus provenance:
  RECORDED (counted from APD events), LEGACY (the number from the rankings file), INFERRED (from events the recorder flagged inferred, healing),
  INCOMPLETE (recorded, but the log has gaps or starts mid-run: a lower bound).
- Only known facts exist. Statistics the run's telemetry version could not record are absent (unknown), never zero: a telemetry
  1.1 run has no combat, item or generation facts. A current-version run with no fights has a known 0 attacks.
- Only the canonical timeline counts; rolled-back play is left out and its size is itself a fact (`run.abandonedEvents`).
- An open run has no result facts. A `RUN_END` with only an outcome keeps the other result fields unknown.
- Coverage: `FactTable.coverage(rows, key)` gives "N of M runs contain X"; `incomplete()` counts lower bounds.
- Cache: `derived/runfacts.apdd` (text). It is trusted only while the size of every source log is unchanged; a missing, stale,
  damaged or older-derivation cache is rebuilt silently and rewritten atomically. `DERIVATION_VERSION` invalidates old caches.
  A rebuilt table equals a cached one (tested). Deleting the folder loses nothing.

## Information state
Imported records are the summaries the rankings screens show (items there are identified), so LEGACY_* events are PUBLIC.
`RANKING_LINK` is INTERNAL. Derived facts are aggregates; fields built from INTERNAL events carry no names of hidden things.
Phase 7's query layer must still filter by event visibility when it shows per-entity truth.

## Findings
- The rankings file is a lossy source: 11 records, no abandoned runs, fields can vary by the version that wrote each record, and SPD's
  own loader turns missing numbers into 0.
- A record has no link to an APD run (its id is random and made after the run is over). The `RANKING_LINK` event fixes this for
  new runs; older APD runs can only be matched heuristically.
- Rows are per log segment: a run recorded across several telemetry versions has several rows with the same `run.id`.

## Limitations
- No import of official Shattered Pixel Dungeon's history (a different app, not readable from APD; see Scope).
- Per-segment rows are not merged into one row per run yet.
- Per-item and per-enemy rows (the Weapons / Armor / Enemies datasets) are Phase 7 views over the item and combat events.
- Phase 4 limitations (inferred healing, unattributed damage sources) are unchanged and still documented in APD-STATS-COMBAT.md.
- The cache check uses file sizes: a log edited in place without changing its size would not be noticed (logs are append-only).
