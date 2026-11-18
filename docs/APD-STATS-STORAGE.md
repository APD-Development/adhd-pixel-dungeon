# APD statistics, Phase 1: the storage library

Package `com.shatteredpixel.shatteredpixeldungeon.apd.stats` (new files only). Nothing in the game calls it yet: no
gameplay hooks, no save changes (`apd_run_id` and `apd_uid` are later phases). It is plain `java.io`, uses no game
classes, no randomness, and only APIs that exist on Android 5.0 (a deliberately conservative set that was the minimum when the library was written; APD now requires Android 8.0, so the set may be relaxed, see `docs/APD-MIN-ANDROID.md`). Tests enforce all three (`ContractTests`).

## Two separate version numbers
| Name | Meaning | Where |
|---|---|---|
| **Telemetry version** (`TelemetryVersion`, written "Telemetry 1.0") | The version of the *recording system*: which events and fields exist and what they mean. Independent of the APD game version. | log header, run index, `apd_records_history.txt` |
| Container format (`LogFormat.CONTAINER_FORMAT`, now 1) | Only the byte layout of the file. | log header |

Rules:
- A run keeps the telemetry version that recorded it, for ever. Reading never rewrites it, and a log is only ever appended
  to by a writer using the same telemetry version (otherwise `FAILED_VERSION_MISMATCH`, and the log is not touched).
- Minor version (1.0 to 1.1): additive only, old data keeps its meaning. Major version (1.x to 2.0): existing data's
  meaning changes. A log newer than the reader is still readable at frame level and is flagged `newerThanReader`.
- Readers may derive newer representations from old data, but the original version stays knowable.
- Every telemetry version needs a milestone block in `core/src/main/assets/apd/apd_records_history.txt` (a test checks the
  current one is present), so the history can explain why a run recorded under 1.0 differs from one under 1.3.
- **Current: Telemetry 1.0.** It defines the storage layer and one event type (`GAP`). It records no game events yet.

## Files
```
<root>/runs/<runId>.apdlog   one append-only event log per run (the evidence)
<root>/index.apdi            run index (derived, rebuildable)
<log>.tail-<offset>.damaged  bytes cut off a damaged log by a repair (kept, never deleted)
```
`StatsStore` owns this layout; the caller chooses `<root>`. Run ids are letters, digits, `-` and `_` (max 64).

## Log layout (container format 1)
```
file   = header frame*
header = "APDL" | containerFormat u8 | bodyLength u16 | body | crc32
body   = telemetryMajor | telemetryMinor | createdAtMillis | runId | writerBuild | (extra bytes, ignored)
frame  = 0xA7 | frameType u8 | payloadLength (varint) | payload | crc32     (crc covers type, length, payload)
```
Integers are big endian, "varint" is LEB128, signed values are zigzag.

| Frame type | Payload |
|---|---|
| 1 STRING | id (varint), UTF-8 text. Ids are 0,1,2... in order. Each distinct string is stored once per log. |
| 2 EVENT | eventType, flags, clock delta (zigzag), event payload (opaque to the library) |
| 3 SEAL | event count, last clock. Marks a completed run. |
| 4 CLOCK | absolute clock, written first and every 128 events so a reader that lost frames can re-anchor |
| anything else | skipped by readers (so new frame types can be added) |

- Events carry `type`, `flags`, a clock (what it measures is decided by later phases) and a payload written with
  `EventBuilder` (unsigned/signed varints, bool, float, double, bytes, strings via the string table).
- **Unknown event types are delivered and can be skipped** (`ReadOptions.knownTypesOnly`), never an error.
  **Event type ids are never reused**: `EventTypes` is checked against `event_type_manifest.txt` by a test, so a removed,
  renamed or added id fails the build until the manifest is updated on purpose.
- Registered so far: `1 GAP` (recording interrupted; reason + amount). IDs 1-255 are storage infrastructure, 256+ will be
  gameplay events.

## Writing (`RunLogWriter`)
- Callers on the game thread only encode a frame into memory and put it on a queue. A daemon thread
  (`apd-stats-writer`) writes in batches and flushes when idle. `flush(timeout, sync)` waits for what was accepted so far,
  `close(seal)` ends the log (seal = run finished; no seal = left resumable).
- **Failures never reach the caller.** Nothing throws. Any I/O error disables the writer: later calls return false, the
  cause is in `failure()` and the optional `FailureListener`. A caller mistake (a negative number in an unsigned field)
  drops just that event and notifies the listener without disabling recording.
- **A full queue drops events visibly.** Dropped events are counted, and the next accepted event (or `close`) is preceded
  by a `GAP` event carrying the count. An event's new strings are registered only if the event is accepted.
- `create` never overwrites an existing log. `openForAppend` refuses sealed logs, other telemetry versions, and unreadable
  headers; on a damaged tail it repairs first and records a `GAP` (reason "recovered tail").

## Reading (`RunLogReader`) and recovery
`read`/`parse` never throw on bad data. The result has a status, the events before any damage, the string table, and
`validBytes` (end of the last good frame):

| Status | Meaning |
|---|---|
| SEALED | all frames valid, seal present and its event count matches |
| UNSEALED | all frames valid, no seal (in progress, or recording stopped without closing) |
| TRUNCATED | the file ends in the middle of a frame: everything before it is valid |
| CORRUPT | a frame failed its checksum or is malformed: everything before it is valid |
| SEAL_MISMATCH | seal count disagrees with the frames found |
| BAD_HEADER / UNSUPPORTED_CONTAINER | the file cannot be interpreted (damaged header / newer container format) |

- `ReadOptions.resync` keeps reading after damage by scanning for the next checksummed frame. Recovered events are real,
  but the history in between is missing and clocks are marked uncertain (`uncertainClockEvents`) until the next clock frame.
- `LogRecovery.repair(file)` cuts a damaged tail so the log ends on its last valid frame again. The cut bytes are first
  saved to `<log>.tail-<offset>.damaged`. Healthy, sealed and unreadable logs are never changed. A sealed log with junk
  after the seal is trimmed back to the seal.
- `StatsStore.recoverAll()` repairs every log and rebuilds the index. Nothing is deleted.

## Run index (`RunIndex`)
One entry per log: run id, telemetry version, newer-than-reader flag, file name, creation time, status (SEALED / OPEN /
DAMAGED / UNREADABLE), event count, last clock, valid bytes, file length. It is **derived data**: a missing, damaged,
truncated or wrongly versioned index is rebuilt by scanning the logs (unreadable logs still get an UNREADABLE entry so they
stay visible). Saved atomically (temp file, sync, rename). Entries are length-prefixed so later versions can add fields.

## Measured and limits
- Caller cost on a desktop JVM: about 1.2-1.5 microseconds per event (6 fields, one string), 15 bytes per event
  uncompressed in that benchmark. Android will be slower; measure in a real build before drawing conclusions.
- Logs are not compressed yet (per-frame checksums keep partial recovery precise). Compression of finished logs is a later
  storage-tools concern.
- The reader loads the whole log into memory (a long run should be around a megabyte). Streaming can be added without
  changing the format.
- The queue is bounded (default 16384 frames). Under extreme bursts events are dropped and reported, never blocked on.
- Durability: events reach the OS shortly after they are accepted, and are forced to disk only on `flush(.., true)`. An
  OS-level crash can lose the unsynced tail; the log stays valid up to its last complete frame.
