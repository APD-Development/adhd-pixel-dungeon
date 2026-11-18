# TEST204 movement-command telemetry candidate — 2026-10-15

## Reproduction
The existing public `move.step` series records one row per cell change. Its `travelling` value comes from `Char.move(step)`, so every ordinary physical walk—including a one-cell tap and each step of a distant route—has value 1. It does not distinguish manual steps, point-to-point travel, or the APD safe stair-navigation action.

## Candidate
Telemetry 1.15 adds public `move.command` after the first successful step of a transient `HeroAction.Move`. Each accepted action records exactly one destination, the current floor width, and one public mode:
- `step`: an adjacent movement tap;
- `travel`: a non-adjacent point-to-point movement tap;
- `stairs`: APD's safe navigation to discovered stairs.

The existing `move.step` schema and values are unchanged; only its description is corrected to its actual physical-walk/instant-relocation meaning. Current-source search found no general auto-explore action, UI, or execution path, so no auto-explore event was invented.

## Safety and verification request
The hook runs only after `getCloser` succeeds, so blocked and same-cell actions emit nothing. The guard is stored only on the transient action object. No save format, route, interruption, turn cost, random call, gameplay result, score, custom rule, permanent statistic, application identity, or signing input changes.

Retained tests cover stable mode tags, exact-once action marking, the public fact payload, wiring, permanent manifest, telemetry 1.15, and records-history placement. Run the complete TEST204 gate: 1,619 core tests, debug assembly, immediate-previous compiled TEST202/Codex TEST 198 history, 27 API34 methods, and 35 valid screenshots.

Status: candidate pending. No delivery or physical-phone claim. TEST202 remains the verified compiled fallback; TEST123 remains owner-delivered/signature-compared.
