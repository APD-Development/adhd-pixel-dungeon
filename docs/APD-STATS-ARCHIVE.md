# APD history archive, export/import and the storage meter (Phase 8)

Telemetry stays at 1.5. Nothing recorded changes; this adds a way to carry the raw history between devices, and a meter.
Package `apd.archive` (pure Java like the other libraries, `ContractTests` checks it), the Android file picker in the `android` module.

## What an archive is
One ZIP. Its first entry is `MANIFEST.txt`; every other entry is `runs/<id>.apdlog` or `legacy/<id>.apdlog`, the raw evidence.

```
APD-ARCHIVE <format 1>
created <millis> | app <version> | telemetry <exporting build's current> | container <log container format>
file <path> <size> <sha256> <kind> <runId> <telemetry> <sealed> <redacted> <events> <status>      (one per file)
note <text>
count <files> <total bytes>
check <sha256 of every line above>
```
Unknown lines are ignored and a newer `format` is refused, so the manifest can grow. **Derived data is never exported** (the index and the row/fact
caches): the importing app rebuilds them from the logs, which is what UIMSD already does for any log it has not seen. Checkpoints, run records, provenance
and schema/version information are in the logs and the manifest (telemetry version per file).

## Hidden information
* A **finished** run (its newest log ends with the run's end) is exported **byte for byte**, hidden information included: the player may read it once the run
  is over, and it is theirs to keep.
* A run that is **not finished** (including the run being played right now) is exported only as a **redacted copy** (`LogRedactor`): every INTERNAL event is
  left out, the clock of the rest is kept exactly, the run start's **seed and custom seed are removed** (the one PUBLIC event that mixes in hidden facts),
  and the copy's string table is renumbered to hold only what its own events use, so neither a hidden string nor how many hidden strings exist can be read.
  The copy is a function of what the player could know: two runs that look identical to the player give byte-identical copies (a test checks it).
  Its header says `REDACTED:<writer build>`; UIMSD then shows that run's seed as unknown, never 0.
* An event type that has not been audited is treated as INTERNAL whatever its registered visibility (`LogRedactor.isAudited`; a test fails if a PUBLIC
  type is not on the list, so a new PUBLIC type must be audited before it can ever be exported).
* Synthetic demo data is never exported (a folder with the demo marker is refused, `demo-` logs are skipped) and never imported.

## Import (`ArchiveImporter`)
Only ever adds, and is **all or nothing**: it stages and verifies everything first, and only then places any file. The manifest is checked first (its
checksum, format, totals, that every listed path is `runs/<id>.apdlog` or `legacy/<id>.apdlog` with a plain id, sizes, no demo data, enough free space); a bad
manifest stops everything before anything is written. Each file is then streamed to a temporary folder (one at a time, never held in memory), and must match the
manifest's size and SHA-256 and be a readable APD log of the run the manifest says. If **any** file fails (damaged, mismatching, missing from the archive,
truncated, demo-named) nothing at all is imported, the temporary folder is removed and the problems are reported. Only a fully verified archive is committed:
a log that already exists is **never replaced** (an identical one is skipped, a different one is kept as it is and reported; the name is claimed atomically),
and if placing a file fails half way, everything this import placed is taken back. Entries the manifest does not list are ignored, whatever their name. A zip stream that answers a read with zero bytes (Android 5 did, for an entry that is cut short, and its `getNextEntry()` then spun for ever; APD no longer supports Android 5, but the guard stays: a stream is never trusted to make progress) is treated as a damaged archive and stops the import at once (`readChunk`, and `ProgressGuardInputStream` around the raw source; regression tests `aStreamThatStallsMidEntryStopsTheImportInsteadOfSpinning` and `aSourceThatStopsAnsweringStopsTheImportInsteadOfSpinning`). The
index is rebuilt afterwards. Recording is never interrupted: the importer only creates files that do not exist.

## Storage meter (`StorageMeter`)
Shown in APD Settings: runs and bytes of evidence, imported records, rebuildable caches, free space on the device. Warnings, never limits: the history is
"getting large" from 100 MB and "very large" from 500 MB (the estimate is 70 MB per 1,000 runs), and the device is "low" under 200 MB free or less than twice
the history. **Nothing is ever deleted or archived automatically.** (A manual "archive and remove" is not part of this phase: it would delete raw history and
needs its own decision.)

## Android
`AndroidFileProvider` uses the system file picker (`ACTION_CREATE_DOCUMENT` to save, `ACTION_OPEN_DOCUMENT` to read): the player chooses the file and the app
gets a stream to exactly that file; no storage permission is needed. The core only knows `ApdFiles.Provider`; without a platform provider a folder fallback
(`exports/`, `imports/`) is used, so desktop and tests work.

## Tests
`LogRedactorTest` (nothing hidden in the copy, byte-identical for identical player views, clocks exact, seed removed, layout pinned, audit list),
`ArchiveManifestTest`, `ArchiveRoundTripTest` (a history moves to another device and UIMSD reads the same; finished runs identical files; unfinished runs
redacted; twice adds nothing; never overwrites; merges; exporting while recording; caches rebuilt), `ArchiveHostileTest` (garbage, tampered manifest or file,
zip-slip paths, unlisted entries, wrong size or header, a 70 MB entry, truncation, newer format, missing files, demo data), `StorageMeterTest`,
`HistoryTransferTest`, `ArchiveSelfTestTest`;
`ArchiveMigrationDeviceTest` runs the whole migration on the emulators (API 26, the oldest supported, and 34): realistic history, export through `HistoryTransfer` and the same
`ContentResolver` stream code the file picker's answer uses (`AndroidFileProvider.deliverExport/deliverImport`, with the file chosen by the test), ZIP and
manifest checks, import into a clean install, derived data deleted and rebuilt, UIMSD-visible history compared with the source, second import changes nothing,
unfinished-run redaction, lossless finished runs, demo data never in or out, damaged archives changing nothing, cancel and error paths, then the self-test.
**Not automated:** only the system file picker's own screens (showing and tapping them) remain for a physical-phone check; `ArchiveSelfTest` also runs on the emulator (`ApdStatsDeviceTest`) and from "Run self-test".
