# Emulator-free simulator foundation — October 10, 2026

## Why this exists

The measured Android adapter is correct enough to exercise the real rendered game, but it is not a viable bulk-statistics engine. Reapplying a 100x visual time scale improved measured action throughput only from about 3.19 to 4.87 actions/second, roughly 1.53x. Emulator boot, Android instrumentation, the render thread and animation callbacks remain dominant costs.

The bulk path therefore has to run the same core game rules directly on the JVM. Android emulator episodes remain the parity gate for a much smaller sample; they are not the large-batch engine.

## Source-pinned Android result

Workflow [38094049032](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38094049032) completed successfully against source `ac496e4a118735dfca853de1b9ced82f12f856a6`. Its five native episodes all completed with nonempty traces: 2,446 recorded actions in 609,546 milliseconds, or 4.013 actions/second. Every episode ended in death on depths 1 through 3.

None of those five traces issued `SEARCH`. The run therefore verifies the repaired adapter source in the existing five-seed gate, but it does not directly replay the formerly stalled seed `1000048` or prove that exact search fallback. That seed needs a separate targeted replay or inclusion in the next evidence set.

## First bounded execution seam

`HeadlessActorLoopProbeTest` uses the existing throw-away libGDX headless environment and real APD classes. It adds no production simulator switch and changes no gameplay source.

It checks two separate facts:

1. One hundred distinct Warrior first floors can be initialized with the real `Dungeon.init()` and `Dungeon.newLevel()` path without Android or a renderer. The test prints the actual elapsed milliseconds and total generated enemy count as evidence, but has no brittle performance threshold.
2. A real entered floor can run the actual `Actor.process()` clock through one complete player wait and all intervening actors using invisible test-only sprites. The scheduler must reach the hero's next turn with the same position, positive HP and at least one unit of actor time consumed. The probe pauses immediately before that hero act because the subsequent `Hero.ready()` method updates render-only UI singletons that do not exist headlessly.

The actor clock normally asks the noosa `Game` shell whether a scene switch is pending. The existing libGDX headless test fixture intentionally creates no noosa game instance, so the probe supplies a minimal test-only shell with scene switching marked complete and restores the previous global instance afterward. It does not create or render a scene. A rendered `GameScene` normally marks the newly entered hero input-ready after its first UI refresh; the headless probe explicitly starts at that already-established input boundary before issuing the real wait.

This is deliberately not described as a completed headless Warrior simulator. It does not yet run the normal-play policy, combat actions, item animations, level transitions, recorder result extraction or paired Android/JVM parity seeds.

## Exact-source verification

Workflow [38095765371](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38095765371) passed the complete core suite for source `a9b55e59152c8603e822dc1e2bcbf6ec4afebeda`, including both headless probe cases. On that hosted two-core runner, the generation case created 100 real Warrior first floors containing 844 mobs in 174 milliseconds. This is direct floor-generation timing only, not complete-episode speed or a 10,000x claim.

## Shared policy/adapter boundary (verified)

The first synchronous command family is now adapter-neutral. `NormalWarriorTurnPolicy` selects rooted and bounded recovery waits without Android, rendering or RNG. It returns a `WarriorTurnCommand`, and both the Android session and JVM headless driver execute that same `WAIT` command boundary through the real `Hero.rest(false)` action.

This extraction preserves the Android profile's existing ordering and limits: rooted waits still happen first; food, known Strength, and eligible equipment still take priority over recovery; recovery still requires no visible enemies, non-starving hunger, below-half HP and fewer than 30 prior safe rests. It does not yet share movement, combat, item use, transitions, complete episodes or result extraction.

Exact-source workflow [38096301603](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38096301603) passed for source `ccf347179ce382f3ffdd1df4d81b59dcea549d2c`. The complete core artifact contains 1,009 tests across 165 suites with zero failures, errors or skips. The policy selector passed all three new cases and the two real headless actor cases passed. Android app/test APK compilation succeeded, followed by five complete native episodes in 499.755 seconds. Their 2,881 trace actions included 157 bounded recovery `REST` commands through the shared boundary. The fixed episodes did not encounter a rooted wait, so the rooted selector is core-verified but not exercised by this native sample. Different episode outcomes and unseeded combat RNG make this unsuitable for a causal speed comparison.

## Next exact steps

1. Keep the Android five-seed workflow as the source-pinned behavioral oracle.
2. Verify the shared passive-wait command boundary on the exact source in both the JVM suite and Android adapter.
3. Add synchronous test-only completion for movement, then melee, item use and transitions one bounded action family at a time.
4. Require identical generation fingerprints, action commands and terminal evidence for a small fixed seed set before allowing a JVM batch result into the evidence summarizer.
5. Benchmark episodes/second only after a complete seed can terminate on both adapters. Do not extrapolate the first-floor or one-wait probe into an episode-speed claim.

## Isolation

The probe writes only under the existing temporary headless test directory. It does not touch phone saves, statistics, signing, application ID, production feature flags, `main`, Claude's branch, backend state or releases. It uses no paid service or API.
