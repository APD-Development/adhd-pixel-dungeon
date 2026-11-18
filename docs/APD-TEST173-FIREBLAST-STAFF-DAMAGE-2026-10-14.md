# TEST173 — Mages' Staff Fireblast explosion provenance

Started: 2026-10-14T21:36:33Z

## Confirmed omission
The normal Fireblast wand cast and its fire fields already carried the wand user's origin. The Mages' Staff on-hit explosion was a separate offensive roll and still called `Char.damage` directly for each nearby enemy, so non-neutral Player Damage did not apply to that staff-only secondary event.

## Bounded correction
The original `Hero.heroDamageIntRange` roll and power multiplier remain in the same location and order. Its final raw integer now passes through the existing offensive hook with the actual staff attacker, original Wand of Fireblast source object and original target.

The hook runs once before the existing direct-damage call. It adds no RNG, armor roll, turn, source substitution, field change or save-format state. Neutral 100% is identity.

## Retained regressions
Three tests require:
- every permitted 25%–500% factor for player, ordinary-enemy and boss attackers, with contradictory other slider values;
- exactly one damage call with the original source;
- neutral amount and next RNG identity;
- the production staff path to use one attributed boundary and remove the old direct call.

## Verification request
TEST173 requests 1,593 complete core tests, debug assembly, compiled history against the immediate previous TEST172 APK, 26 API34 emulator checks and 34 valid screenshots. Candidate remains unverified until exact-source evidence passes.

No player-save, run-history, scoring, application identity, signing, delivery or physical-phone claim is made. TEST123 remains the last owner-delivered/signature-compared APK.

## Verification result
Completed: 2026-10-14T21:50:04Z

Exact source `e101e85167ee802cef3a0db31cb8497b83a23328` passed GitHub Actions run `38641672668` from 2026-10-14T21:37:27Z through 2026-10-14T21:50:04Z.

- Core artifact `11931431183`, ZIP SHA-256 `7090fcda3c677e0373b08a421634229e727e252110073eb3ab4510aa3eaf402b`: 1,593 tests, 0 failures, 0 errors, 0 skipped. The three Fireblast staff methods passed in 0.003 seconds.
- Debug assembly and compiled-history validation against the immediate previous TEST172 APK passed.
- APK artifact `11931396427`, ZIP SHA-256 `897d768650d788653c5df751e15c26ec0ddd3b0098ec1b32de249ddfe43c3bfc`: `ADHD-Pixel-Dungeon-CODEX-TEST-173.apk` SHA-256 `2f9379b9a38a28996d32bc745af94ba72df6ba0ce63e789bd348dd7d59c95d30`; bundled checksum matched.
- Android artifact `11931062722`, ZIP SHA-256 `f22708aa7c1febcaa022859c56c45e4409619a5554f8e5d3118947d7151d6271`: 26 API34 emulator tests, 0 failures, 0 errors, 0 skipped.
- All 34 screenshots decoded at 320×640. Contact-sheet review found expected readable TEST173 history, rules, score, UIMSD, stairs, tiles and gameplay states with no blank, corrupt or visibly clipped capture.

This verifies the Mages' Staff Fireblast secondary explosion applies its actual attacker factor once while preserving the original roll, source and neutral RNG.

Private delivery was skipped. This is CI/emulator evidence only, not a physical-phone or owner-save claim. TEST123 remains the last owner-delivered/signature-compared APK.
