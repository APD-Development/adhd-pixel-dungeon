# Malformed run-end privacy boundary — TEST234 candidate

Recorded: 2026-10-16T04:42:39.352Z

## Reproduced defect

A checksum-valid event frame can still carry a malformed typed payload. Both the UIMSD row builder and history exporter treated any canonical `RUN_END` event type as a completed run without decoding its required fields.

A malformed empty `RUN_END` therefore had two incorrect effects:

- the statistics catalogue marked the run finished, enabling finished-run visibility;
- a history export copied the raw current-run log instead of redacting its hidden fields.

The raw log itself remains the source of truth and is not modified.

## Bounded correction

`RowBuilders` and `ArchiveExporter` now require `LifecycleEvents.decodeRunEnd` to succeed before accepting a run end. A malformed end remains in raw history, but it cannot mark the run finished or authorize hidden-data export.

Two retained regressions create a structurally sealed log with a valid start, hidden current-run content and an empty typed end. Exact TEST233 must fail both; corrected source must keep the UIMSD run unfinished and export a redacted copy without the hidden string.

## Preserved behavior

Valid run endings, completed-run visibility, normal archive contents, raw history, saves, scores, gameplay, all 18 Custom Dungeon Rules, turns, RNG, Cleric lore/opt-out, signing and application-ID configuration are unchanged.

## Acceptance boundary

The dedicated workflow must reproduce exactly two intended failures on exact TEST233 source `c6bed1f12f017d3f2fa92adf2c29d793f5e004ab` and pass the complete corrected 1,697-method core suite. The normal TEST234 gate must then pass debug assembly, complete immediate-previous produced TEST233 history, 33 API34 emulator methods and 42 valid screenshots. No automatic publication, tester distribution or physical-phone claim is authorized.
