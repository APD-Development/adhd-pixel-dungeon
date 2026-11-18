# TEST178 — Tipped-dart bonus damage provenance

Started: 2026-10-14T22:37:24Z

## Confirmed omissions
The post-TEST177 current-source audit found two independent tipped-dart bonus rolls that bypassed the custom offensive boundary: Shocking Dart's electrical burst and Holy Dart's anti-undead/demonic bonus. Both occur after the base missile hit and are therefore not represented by the already-scaled base attack damage.

## Bounded correction
A shared tipped-dart helper applies the actual attacker's Player, Enemy or Boss Damage factor once to these two independent rolls. Original random ranges, Shocking Dart's Electricity source, Holy Dart's dart source, charged-shot/alliance gates, visuals, direct mitigation and RNG order remain unchanged. At neutral 100%, amount, source and next RNG are identical.

## Retained regressions
Three methods require all permitted 25%–500% factors for player, enemy and boss creators under contradictory settings; exactly one source-preserving damage call; neutral amount and next-RNG identity; and exact production wiring with the two old direct calls absent.

## Verification request
TEST178 requests 1,602 complete core tests, debug assembly, compiled history against immediate previous TEST177, 26 API34 emulator tests and 34 valid screenshots. Candidate is not verified until exact-source evidence passes.

Private delivery is not requested. TEST123 remains the last owner-delivered/signature-compared APK.


## Verified evidence
Exact source `a6d757fcaecc69fb0bdb38a6dd207c3aea6ccf22` passed GitHub Actions run `38649035059` from 2026-10-14T22:38:37Z through 2026-10-14T22:51:05Z.

- Core artifact `11933781595`, ZIP SHA-256 `d9ccd829d674db97d0926421ee71e3780170a1a2e33c11414883185f2d5866e3`: 1,602 tests, 0 failures/errors/skips. All three new tipped-dart methods passed.
- APK artifact `11933846357`, ZIP SHA-256 `5cbfffea672bdfb047e1ed4a3e222380ff022f731476a8f6acf566f5f1d5657d`: `ADHD-Pixel-Dungeon-CODEX-TEST-178.apk` SHA-256 `082e2ad55ce382fe49b348d152145da1103cdee557a80c1ddb2a582aa5029e74`; bundled checksum matched. Debug assembly and complete compiled history against immediate previous TEST177 passed.
- Android artifact `11934865932`, ZIP SHA-256 `7a5b2ef852f5c10ba87870a661b4c4dc0076437e8b5512f2b77ee1edb5aed238`: 26 API34 emulator tests, 0 failures/errors/skips. All 34 screenshots decoded at 320×640; contact-sheet and focused TEST178 history review found the expected readable history/rules/score/UIMSD/stairs/tiles/gameplay states without blank, corrupt or visibly clipped capture.
- Verified Shocking Dart and Holy Dart independent bonus rolls use the actual attacker factor exactly once while preserving original source, gates and neutral RNG identity.

Private delivery was skipped. This is CI/emulator evidence, not a physical-phone claim. TEST123 remains the last owner-delivered/signature-compared APK.
