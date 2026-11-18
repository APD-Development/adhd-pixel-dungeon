# TEST199 ally-summon recording candidate — 2026-10-15

## Reproduction
Current source already preserves explicit offensive creator ownership for summoned actors, including save/load and later damage attribution. It did not emit a run-history observation when a player-owned ally was created. Inferring ownership from allied alignment would be wrong because enemy and boss summons, transformed creatures and legacy actors can also be allied or unattributed.

## Correction
Telemetry 1.13 adds the public fact `ally.summon` at `Char.apdInheritOffensiveOrigin`, the existing explicit creation-time ownership boundary. The hook fires only on the first transition to PLAYER ownership and only when the actual creator is the hero or visible to the hero. It stores displayed ally and creator names plus the current floor. Enemy, boss, unattributed, repeated ownership, off-screen-creator and restore-time paths do not emit a public fact.

The hook reads existing state only, consumes no RNG and is wrapped by the recorder's failure isolation. Actor save metadata and offensive attribution are unchanged.

## Retained tests and required verification
`AllySummonRecordingTest` contains two methods covering direct hero creation, a visible player-owned ally creating another ally, exact-once repeated inheritance, actual displayed creator identity, enemy/boss/unattributed exclusion, off-screen creator secrecy and restore suppression. The fact-kind manifest, telemetry version contract, records-history milestone and source-hook count advance together.

Required hosted gate: 1,613 complete core tests, debug assembly, complete reviewed history since immediate-previous compiled TEST197/Codex TEST 194, 27 API34 emulator methods and 35 valid screenshots. Candidate is pending until those checks pass.

No gameplay, damage amount, source, turn, RNG, save field, permanent score, custom-rule, application ID or signing behavior changed. No APK distribution or physical-phone claim. TEST197 remains the verified compiled fallback; TEST123 remains the last owner-delivered/signature-compared APK.


## Hosted failure and TEST200 correction — 2026-10-15T08:48:52Z
TEST199 source `5a937f93e2858bbc85990db2d96642c2ef4764f1`, run [38707847596](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38707847596), executed all 1,613 core tests: 1,612 passed and one failed. The failure was confined to `TelemetryVersionTest.parsesAndPrints`: the fixture parsed literal `1.12` but accidentally expected `1.13`. The new ally-summon regressions and telemetry 1.13 self-test passed. No APK was built; Android/API34 did not run.

TEST200 changes only that mistaken expected value, retaining the production implementation and every assertion. Re-run the complete 1,613-core/debug/immediate-previous compiled TEST197/Codex TEST 194 history/27 API34/35 PNG gate. Failed core artifact `11958859521`, 752,523 bytes, GitHub SHA-256 `8dcf551329e74d6758ae50ee6cb0f302f7892bdfb15c003e9389f55c169dc8f4`.


## TEST200 verified — 2026-10-15T09:05:04Z
Corrected source `094f6fc168c2c46057648542d68da4537739ec9f`, run [38708477712](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38708477712) (08:49:54Z–09:03:04Z), passed the full gate. Downloaded evidence recounts 1,613 core tests in 314 XML suites and 27 API34 methods, with zero failures/errors/skips. Both ally-summon methods and all telemetry-version methods passed. Debug assembly and immediate-previous compiled TEST197/Codex TEST 194 history verification passed.

The internal Codex TEST 196 APK SHA-256 is `5fe49a5e2167dd688c35fcbc04b31e18951873fa42caae2ea645f42039e4c646`. Artifacts: core `11959253024` / ZIP `1c5e5084b05df08fbdef8bff4fee72d85926b90b3d5c4e5a1998761293fe7b17`; APK `11959178061` / ZIP `e93ab8c59a32331207cacce595e0535cea31972a9dc7fd817567f61dcd14f09c`; Android `11959678617` / ZIP `dfdc05d475811335ec68b606d5ae1d0e5eeaa54be3e98bdc88aef93530d49ca6`.

All 35 screenshots are valid 320×640 PNGs and the full contact sheet was visually reviewed clean. Private delivery was skipped; no physical-phone claim. TEST123 remains the last owner-delivered/signature-compared APK. Do not rebuild unchanged TEST200.
