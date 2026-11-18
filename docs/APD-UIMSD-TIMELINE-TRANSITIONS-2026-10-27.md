# APD UIMSD exact floor/equipment timeline transitions — 2026-10-27

## Accepted source and evidence

- Source commit: `4ebea36d2c33a85d3fcc631663686c7008843cdf`
- Source tree: `517568b711d4d9e80d6c3768199b49b1815dcdef`
- Core workflow: [40591652701](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/40591652701)
- Core job: `122497146085`
- Result: **2,086 methods / 403 suites / 0 failures / 0 errors / 0 skips**
- Retained report artifact: `12773574394`, 978,037 bytes
- Artifact digest: `sha256:7af9e01d63b69120cae12a577e2140ab1d44a672ff0a31ff0d0c2bd181606dbd`
- Six shared `RunTurnWindowTest` methods remain present and green.

## Implemented behavior

`RunTurnBrowser` previously skipped the canonical Equipment and Floors interval datasets. It now emits only four exact recorded transition types:

1. `Equipment put on` at known `equip.start`.
2. `Equipment taken off` at known `equip.end`.
3. `Floor entered` at known `floor.enterTurn`.
4. `Floor left` at known `floor.exitTurn`.

The lines include only existing public cells. Case-sensitive run identity and `publicRow` remain mandatory. Missing interval ends remain unknown; an item still worn or a floor still occupied does not receive an invented exit. No new event is recorded, no old record is rewritten, and no additional RNG call occurs.

Because `DeathContext` already consumes `RunTurnBrowser`, the same exact transitions now appear inside its bounded public chronology while its existing non-causation and incomplete-record warnings remain unchanged.

## New regression coverage

- Exact floor/equipment transitions appear on their recorded turns.
- A non-public equipment row is absent.
- Open equipment and floor intervals do not invent an end transition.
- A bounded death window includes equipment-on, floor-exit, equipment-off and the exact death entry in chronological order.

## Retained measurement evidence

The unchanged full suite also reported the following Linux CI probes:

| Probe | Result |
|---|---:|
| 20,000 recorded steps | 27,132 bytes (1.36 bytes/step) |
| 20,000 hero-state turns | 7,501 bytes (0.38 bytes/turn) |
| Whole synthetic long-run facts | 72,262 bytes |
| Rebuild 60 runs / 270,120 events | 1,080 ms |
| Cached read | 8 ms |
| Cache file | 91 KiB |

These measurements are not Android, physical-device, or transition-specific latency acceptance.

## Boundaries and next work

This is source/core acceptance only. No APK was requested or built, the executable APK request remained blob `dcaa4a08056fbf6a06dc067d7a58ba17e55ceac2`, and there is no new native/API34/display/physical-phone claim. TEST314, TEST313 and TEST262 retain their documented roles.

Still open: broader sequence/window questions, additional metrics and experiments, benchmark comparison before uniqueness claims, and Android/physical storage-latency profiling. Native transition presentation should be accepted with a deliberate necessary application candidate rather than a redundant build solely for this core slice.
