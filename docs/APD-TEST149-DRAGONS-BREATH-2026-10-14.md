# TEST149 Dragon's Breath provenance candidate — 2026-10-14

Started: 2026-10-14T10:34:33Z

Read the complete current PotionOfDragonsBreath, Burning, Fire, FireImbue, Firebloom, Plant and existing fire-provenance tests at source `b80b9ec4e6dbc8aeedf53dd914f50b18640b35e2`.

Implemented a single immutable offensive-origin snapshot when Dragon's Breath commits to its animation. Both primary and adjacent Fire fields and direct Burning now receive that same snapshot. The original cone, door opening, target selection, field strength/duration, cripple, catalog/talent behavior, turn cost and RNG calls are unchanged. Burning gained an explicit-origin overload; null remains unowned/environmental rather than inventing ownership. Existing once-only timed scaling remains the only damage scaling boundary.

Four retained tests cover all 20 percentages for player/enemy/boss, legacy and null environmental ownership, neutral damage/next-RNG identity, and exact real-potion caller wiring. This source-only candidate is not compiled or verified yet. TEST148 run `38549433962` was already active and was not cancelled or replaced. Three Damage controls remain disabled. Inferno propagation, Burning-to-terrain transfer, Firebloom/other plants, vault fire, remaining abilities/derived sources and full class/boss/combination acceptance remain open. No save-format, scoring, balance-range, signing, application-ID, release or distribution change.


## Verification request — 2026-10-14T10:45:49Z
TEST148 completed successfully and is the immediate previous produced APK: source `b80b9ec4e6dbc8aeedf53dd914f50b18640b35e2`, run `38549433962`, 1526 core + 26 API34 + debug/history + 34 valid screenshots. TEST149 candidate `b66a31ada9c13beba67a3c8cc430668a5b8c9975` now requests 1530 core, debug/history against TEST148, and 26 API34/34 screenshots. Candidate remains pending; private delivery stays off.


## Verified result — 2026-10-14T11:00:37Z
TEST149 exact source `af3fea7b8870be3e4f2e045e0bc69edfa1b4c0de`, run `38551940075`, passed 1530 core + 26 API34 methods with zero failures/errors/skips, private debug assembly and compiled history against TEST148. Artifacts: core `11886584059`/`517209dd85f751abac72382a50ba6a850cb3c43e2825aa36a93e07e9f71a5e33`; Android `11887522747`/`eb21e6bc5e54c3d7399643646417e1b1bff64b7756d4f0f6e4b868c0448ab82f`; APK `11887411460`, contained APK SHA-256 `5a786a5c15fa9562f2fc4f6165d11f91cc85eaf9a2b7d4afa7a83ff0639feee7`. All 34 screenshots decoded and were visually reviewed. Private delivery skipped; no signature-comparison or phone claim. Three Damage controls remain disabled for the documented remaining audits.
