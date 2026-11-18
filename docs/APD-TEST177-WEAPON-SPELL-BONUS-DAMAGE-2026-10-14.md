# TEST177 — Weapon-triggered spell bonus provenance

Started: 2026-10-14T22:08:00Z

## Confirmed omissions
Current-source audit after verified TEST174 found two independent offensive bonuses in `Weapon.proc` that bypassed the custom damage boundary: Holy Weapon's flat post-enchantment bonus and Smite's independently rolled spell bonus. Both are produced by the actual hero attacker and are separate from the already-scaled base weapon hit.

Crystal and Shocking enchantment damage deliberately remain unchanged: each derives its amount from the already once-scaled, post-mitigation `damage` argument, so applying another custom boundary would double-scale it.

## Bounded correction
A shared helper applies the actual creator's Player, Enemy or Boss Damage factor once to those independent bonuses and preserves each original roll, proc multiplier, source object, trigger order and direct-damage behavior. Neutral 100% is identity. No attack roll, base weapon damage, armor, score, save format, class mechanic or RNG order changes.

## Retained regressions
Three methods require all permitted 25%–500% factors and creator roles with contradictory settings, exactly one source-preserving hit, neutral amount/next-RNG identity, and exact wiring of both production calls with the old direct calls absent.

## Verification request
TEST177 requests 1,599 complete core tests, debug assembly, compiled history against immediate previous compiled TEST176, 26 API34 emulator tests and 34 valid screenshots. Candidate remains unverified until exact-source evidence passes.

Private delivery is not requested. TEST123 remains the last owner-delivered/signature-compared APK.

## Exact-source verification
Source `7b190b54c1aaed329e6e185c592ee686c02385ea` was verified by [run 38645784290](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38645784290) from 2026-10-14T22:10:39Z through 2026-10-14T22:23:35Z.

- Core artifact `11932816785`, ZIP SHA-256 `63fd2f7af4ad6b689b4e58a0149004d8372b9c516746a73f6217ffba3277086e`: 1,599 tests, 0 failures, 0 errors, 0 skipped. All three new methods passed in 0.003 seconds.
- APK artifact `11932966600`, ZIP SHA-256 `f7e358e1b52e5f0892fe53f98e0cbb747fce540b32b258a62966cbccf0ecd0a9`: `ADHD-Pixel-Dungeon-CODEX-TEST-177.apk`, SHA-256 `ea2099d787daeacca90d2b19d7e44263c571ed8517bc3d49229a530054a23e65`; bundled checksum matched. Debug assembly and compiled-history validation against immediate previous compiled TEST176 passed.
- Android artifact `11932673053`, ZIP SHA-256 `6b7c38505ee99ba17bbc3c3ca5861a051d1d1ce6db1b1d6fe44ac313b2a45d7b`: 26 API34 emulator tests, 0 failures, 0 errors, 0 skipped. All 34 PNGs decoded at 320×640; contact-sheet and focused full-resolution review found the expected readable TEST177 history, rules, score, UIMSD, stairs, tiles and gameplay states with no blank, corrupt or visibly clipped capture.

Verified behavior: Holy Weapon's flat bonus and Smite's independent bonus use the actual attacker factor exactly once while preserving original roll/proc multiplier/source/trigger order. Neutral 100% preserves amount/source/next RNG. Crystal and Shocking derived damage remain intentionally unchanged because they already inherit the once-scaled post-mitigation base hit.

Private delivery was skipped. This is CI/emulator evidence only, not physical-phone or owner-save verification. TEST123 remains the last owner-delivered/signature-compared APK. Do not rebuild unchanged TEST177; continue the distinct approved backlog.
