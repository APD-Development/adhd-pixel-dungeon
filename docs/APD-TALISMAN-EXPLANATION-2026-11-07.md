# Talisman active-effect explanation — 2026-11-07

## Changes and source

ArtifactExplanation now adds a player-facing active-Scrying summary and expandable TalismanExplanation, alongside existing recharge details. Real TalismanOfForesight.execute, scry.onSelect, maxDist, Foresight.checkAwareness and recharge code were inspected. Production changes are description-only; no cone, discovery, generation, cost, turn, save or recharge logic changed.

Summary explains cone searching versus passive trap warning, actual one-base-turn action, meter resource cost and live awareness duration. Details explain equipped/5-meter/Magic-Immunity requirements; actual public-state long-range cutoff and short-distance exception; angle narrowing with distance; nominal cost and saved fractional arithmetic rather than falsely promising exact rounded debit; invisibility loss; creature/pile awareness and exclusions; separate earned upgrade points and live next threshold. Below5 meter, unavailable wording avoids presenting negative calculated distance as usable range. Hidden world cells are never read by the helper.

## Tests and boundaries

TalismanExplanationTest has two meaningful cases. One compares the pure range to the real private gameplay maxDist method across11 upgrades and8 meter states (88 contexts), with Dungeon.hero and level deliberately null; checks integration, live duration/threshold and saved item state before/after inspection. The other varies hidden levels and curses, ensuring no public range/effect leaks before identification. Existing MechanicsExplanationTest runs alongside it:329 targeted tests passed; log /workspace/apd-talisman-targeted.log. A subsequent wording change for unavailable meter state is included in full validation below.

No actual cone cast/native rendered scene or phone test is claimed. Cone-specific source review is not runtime proof of every source interaction. Full translations and wider mechanics review remain open. No APK built or published for this batch; delivered371 remains current.

Full local suite2932/449 passed, zero failures/errors/skips; /workspace/apd-talisman-full.log and /workspace/apd-talisman-local-junit.zip. Exact combined source421ed48d78a001c5aebb98abd7011da3ba035dd3 passed source-only run42410294358/job128516760866 at2026-11-07T20:20:11Z:2933 tests/449 suites, zero failures/errors/skips, plus Android debug/release/instrumentation Java and desktop compilation. Artifact13589171503 is513119bytes, SHA256 1175ee7074c7a6ca1220028c5d1a8d7cc9f5727ae9d24765ffbfb6d3ee6d3a3e; embedded receipt, exact Talisman tests and all449 raw XML suites downloaded and checked. No APK assembled.
