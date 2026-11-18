# Berry saved-counter acceptance

Test-only continuation after the verified seasonal-food package. Application source and Berry behavior are unchanged.

## Actual regression

The new real-path test invokes Berry.satisfy twice on an isolated level with the real Generator default seed table:

- the first meal creates a SeedCounter at exactly1 and does not drop an item;
- the counter is revive-persistent and its compressed Bundle round-trip preserves count1;
- repeated inspection preserves the hero, counter and world and does not choose or drop a seed;
- the second meal drops exactly one real Plant.Seed and detaches the counter.

The test uses a quiet item sprite only to suppress rendering. It does not replace Berry, Generator, CounterBuff, Hunger or item-drop behavior.

## Verification state

Exact test sourcea7d48990f4a74551acff6a5b21e4732753a88748 passed source-only run42409994980/job128515841166 at2026-11-07T20:16:23Z:2931 tests/448 suites, zero failures/errors/skips, plus Android debug/release/instrumentation Java and desktop compilation. Artifact13588724425 is513069bytes, SHA256 fdf5a0bc6e40c8a7ae2772a1bf33979a6b633d96abe7bf7849b5963001c217eb; embedded receipt, exact Berry case and all448 XML suites downloaded and checked. The test passed again in combined421ed48d and c8faf594 gates. No APK, release, physical-phone or rendered-native claim.
