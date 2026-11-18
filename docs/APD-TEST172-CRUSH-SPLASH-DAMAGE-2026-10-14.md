# TEST172 — Crush splash Player Damage scaling

Started: 2026-10-14T21:11:59Z

## Reconciled source
The approved branch advanced after TEST171 to `c684d5070fc26bca6cc2d34b414ea18ecbefbb49`. That commit was preserved. TEST171 evidence applies only to its exact source and does not verify this newer application change.

## Bounded correction
Warrior Crush splash now routes the hero-owned offensive splash roll through the existing Player Damage hook exactly once. Scaling occurs before the original halving, target armor roll and Vulnerable adjustment. Enemy Damage and Boss Damage do not replace Player Damage for this hero-created effect.

At neutral 100%, the calculation retains the original `Math.round(damageRoll * 0.25f * comboCount)` result and consumes no new RNG. The original combo count, target selection, DR, Vulnerable effect, turns and source behavior remain.

## Retained regressions
Three tests require:
- 200% and 50% Player Damage results before the original fixed-armor order;
- original rounding for raw rolls 0–99 and combo counts 1–24 at neutral, plus identical next RNG;
- every permitted 25%–500% Player Damage value, while contradictory Enemy/Boss values cannot affect the hero-owned result.

## Verification request
TEST172 requests 1,590 complete core tests, debug assembly, compiled history against the immediate previous TEST171 APK, 26 API34 emulator checks and 34 valid screenshots. This source is candidate-only until that exact evidence passes.

No player-save, run-history, scoring, application identity, signing, delivery or physical-phone claim is made. TEST123 remains the last owner-delivered/signature-compared APK.

## Verification result
Completed: 2026-10-14T21:25:00Z

Exact source `12795cd9cd3049864e1f33c90b9551e9bda67c8e` passed GitHub Actions run `38638453521` from 2026-10-14T21:12:37Z through 2026-10-14T21:25:00Z.

- Core artifact `11928792925`, ZIP SHA-256 `d0a82f9c7f2e3abd30f23fb99385c15c436c9ad20fe98f270c6e48b6e97b8851`: 1,590 tests, 0 failures, 0 errors, 0 skipped. The three `ComboDamageScalingTest` methods passed in 0.019 seconds.
- Debug assembly and compiled-history validation against the immediate previous TEST171 APK passed.
- APK artifact `11929357443`, ZIP SHA-256 `1636835f4aa85a5ea95565fe7ec658688a67bbf9b0483d2bc921df73ef458ac8`: `ADHD-Pixel-Dungeon-CODEX-TEST-172.apk` SHA-256 `5a9739829b398ab9fbc4c1ae83c78e06dbddd23806a854406d58442e5375f153`; bundled checksum matched.
- Android artifact `11929688691`, ZIP SHA-256 `ff348102d2c2705af15e303d98e498c5d3a12483763df0ed09d9e1415a72ab46`: 26 API34 emulator tests, 0 failures, 0 errors, 0 skipped.
- All 34 screenshots decoded at 320×640. Contact-sheet review found expected readable TEST172 history, rules, score, UIMSD, stairs, tiles and gameplay states, with no blank, corrupt or visibly clipped capture.

This verifies that Warrior Crush splash uses Player Damage exactly once before its original halving, armor and Vulnerable processing; all permitted percentages and neutral rounding/next-RNG identity passed.

Private delivery was skipped. This is CI/emulator evidence only, not a physical-phone or owner-save claim. No save format, scoring, application identity or signing change was made. TEST123 remains the last owner-delivered/signature-compared APK.
