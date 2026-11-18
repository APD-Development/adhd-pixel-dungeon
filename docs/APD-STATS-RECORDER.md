# APD statistics: run lifecycle recorder (Phase 2, telemetry 1.1)

The recorder turns the Phase 1 storage library into a record of each run. It only **reads** the game.

## Rules it follows
- Never changes gameplay, never uses SPD's random numbers, never changes game state (checked by `RecorderNeverAffectsTheGameTest`,
  the recorder-on golden test, and `RecorderIsolationTest`, which scans the recorder source).
- Never fails a run: every hook catches everything. After 5 errors it switches itself off for the session (`ApdRecorder.MAX_ERRORS`).
- Recording is on by default. Title screen -> "APD Settings" (also in-game menu -> "APD Settings") -> "Record run history" turns it off (applies from the next start).
- Raw logs are never deleted automatically. Unknown values stay unknown (a partial `RUN_START`/`RUN_END` carries a flag, never a fake zero).

## The one save change
`apd_run_id` (string) in the slot's game bundle. Absent in old saves (those runs are adopted as *partial* runs on first load).
Nothing else in any save changed; `apd_uid` on items is Phase 3.

## Hooks (all in `Dungeon`, one line each)
| Hook | Where | Effect |
|---|---|---|
| `onNewRun` | end of `Dungeon.init()` | new run id, `RUN_START`, first `LEVEL_ENTER` is on the first level switch |
| `onGameLoaded` | end of `Dungeon.loadGame` (full load) | resume the run for the save's `apd_run_id`, detect rollback / gap |
| `onGameSaved` | after `saveGame` writes | `SAVE` marker (slot, depth, turn, `last_played`) |
| `onLevelSwitched` | `switchLevel` | `LEVEL_EXIT` (checkpoint of the hero/items) + `LEVEL_ENTER` |
| `onRunEnded` | `fail` / `win` | `RUN_END` (DIED/WON, cause) and seal |
| `onSlotDeleted` | `deleteGame` | marks the run ABANDONED (log is kept) |

## Events (ids 256-264)
`RUN_START, RUN_END, SAVE, LEVEL_ENTER, LEVEL_EXIT, CHECKPOINT, SESSION_START, ROLLBACK, RULESET_CHANGE`. Layouts are in `LifecycleEvents`.
The clock is whole turns (`Statistics.duration`); nothing is recorded per turn or per move.

## Run id, logs and segments
A run id is a random UUID (`UUID.randomUUID`, which does not touch SPD's generator). The log file id equals the run id, or
`<runId>_<n>` for a continuation segment (used when the previous log is sealed or was written by another telemetry version).
A run keeps the telemetry version that recorded it.

## Rollback and abandoned branches
SPD saves are whole-file; a player can copy an old save back. On load the recorder compares the save's `last_played`, turn and
depth with the `SAVE` markers in the log:
- **Matched**: events after the matched `SAVE` belong to a timeline that no longer exists. They are kept, and a `ROLLBACK(MATCHED)`
  event is written. `RunTimeline` classifies them as *abandoned*.
- **Unmatched**: `ROLLBACK(UNMATCHED)`; everything stays, and the timeline reports `uncertainFromIndex`.
- **Recorder behind the save** (recorder was off, crashed, or log lost): `GAP(RECORDING_BEHIND_SAVE, turns)`.
- **No log for the save's run id**: a new log with the same id is started as partial (`LOG_MISSING`); a save with no `apd_run_id` is adopted (`ADOPTED_RUN`).
`RunTimeline.canonical()` is the default view for statistics (abandoned excluded); `abandoned()` is always available. Nothing is merged or deleted.

## Ruleset
`Ruleset` id `r1-<12 hex of SHA-256>` of `base=spd-4.0.0;apd0001=on;cleric=blocked;exotic=<n>;challenges=<mask>`. A change mid-run (for
example the exotic setting) writes `RULESET_CHANGE`.

## Validation on a real Android runtime
- `SelfTest` (storage library: write/read, lifecycle codec, truncation/corruption/repair, concurrency, index rebuild) runs on desktop in `:core:test`,
  inside the game (title screen -> "APD Settings" -> "Run self-test"), and as an instrumented test (`ApdStatsDeviceTest`) on emulators API 26 (the minimum supported) and 34 via
  `.github/workflows/android-tests.yml` (separate from the release build).
- A physical phone run is the in-game button; emulator results are not a substitute for it.

## Item identity (telemetry 1.2)
See `docs/APD-STATS-ITEMS.md`. The recorder also writes item events and saves `apd_uid` on items; `Hero.act`, `Dungeon.saveGame`
and a few item methods call the recorder's `ApdItems` entry points.

## Combat and health (telemetry 1.3)
See `docs/APD-STATS-COMBAT.md`.

## Generation provenance (telemetry 1.4)
See `docs/APD-STATS-GENERATION.md`.

## APD rankings import and derived facts (telemetry 1.5)
See `docs/APD-STATS-LEGACY.md`. The import reads APD's own `rankings.dat` only, not official SPD's.

## Query engine and the UIMSD screen (Phase 7)
See `docs/APD-STATS-UIMSD.md`.

## Known limitations
- A log left OPEN with no save slot (crash before the first save) is not yet classified as orphaned.
- The exit checkpoint is captured when the next level is entered, so a death on a level has its last checkpoint from the level entry.
- Per-event combat, health and item detail arrive in Phases 3-4.
