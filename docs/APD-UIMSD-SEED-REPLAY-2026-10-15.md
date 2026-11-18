# UIMSD stale seed-replay handoff guard — October 15, 2026

## Confirmed failure

`startSeedFloors` captured a catalogue and ran the expensive seed rebuild on a worker, but its completion installed datasets and navigated unconditionally. If a newer catalogue became READY first, the old replay could still replace its view/query/back-stack state and emit stale listener notifications.

The retained regression uses a blocking `SeedFloors.Provider`: it starts the old replay, installs a distinct newer catalogue while the provider is blocked, releases the replay, then joins its worker. Before the correction the model opens the stale seed view and notifies again. No sleep or probabilistic ordering is used.

## Bounded correction

- Each replay receives an in-memory generation.
- A newer replay, catalogue load, explicit catalogue installation or provider replacement retires older replay workers.
- Retired workers install neither data nor failure notices and do not navigate or notify.
- Seed generation, canonical/recorded provenance, raw history, derived formats, saves, scores, all 18 rules, gameplay, turns and RNG are unchanged.

## Verification state

TEST217 is verified at source `d2c28cb076d9918632d313e5a4f9655d36c9433b` by [run 38795254303](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38795254303), completed 2026-10-15T19:48:25Z. The run passed 1,654 core tests in 326 suites, debug assembly, and complete immediate-previous TEST216 history. Its API 34 artifact passed 28 methods with zero failures/errors/skips and contains 38 valid 320×640 PNGs; the complete contact sheet was reviewed readable.

Artifacts: core `12000959379` / SHA-256 `9f1c30afee684d7cbf0192ee40e75845e086f8c5ccf621a119831f42033d459f`; APK/history `12001119270` / `0322280cdbe6e0f9e995c8940eee308353f149b889df1433d86b264b4d40fc56`; Android `12002566209` / `c15d8a4b3f4c0639b534fc2cb5ddaaac6be0a7de65f21202fbaa796d03e1663b`. APK payload SHA-256 is `4db6d38ad6ff0a6a5e375c43fadfa990b52172b2b14aac4bb2dafce9d9e6a6e9`.

Private delivery was skipped. No release, signing comparison, owner-device install or physical-phone result is claimed. TEST214 remains the signing-compatible staged candidate and TEST206 the published fallback.
