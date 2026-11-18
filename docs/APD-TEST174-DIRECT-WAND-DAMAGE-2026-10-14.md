# TEST174 — Remaining direct-damage wand provenance

Started: 2026-10-14T21:51:03Z

## Confirmed omissions
Current-source audit after TEST173 found seven direct-damage calls that did not use the custom offensive boundary:
- Blast Wave's two normal-zap targets and its Mages' Staff paralysis detonation;
- Living Earth's two normal-zap damage branches;
- Prismatic Light's ordinary and demonic/undead target branches.

These rolls are distinct from already-attributed Blast Wave collision damage, Living Earth guardian attacks and Prismatic's non-damaging staff cripple.

## Bounded correction
A shared direct-wand helper applies the actual creator's Player, Enemy or Boss Damage factor once, then calls the original target with the original wand source. Each caller retains its existing damage roll, proc chance, target filtering, direct-damage mitigation, visual effects, turns and RNG order.

Neutral 100% is identity. No field, guardian, collision, buff, save-format or score behavior changes.

## Retained regressions
Three tests require:
- all permitted 25%–500% values for player, ordinary-enemy and boss creators with contradictory other settings;
- exactly one damage call with the original source;
- neutral amount and next RNG identity;
- all seven production calls to use the shared boundary and the old direct calls to be absent.

## Verification request
TEST174 requests 1,596 complete core tests, debug assembly, compiled history against the immediate previous TEST173 APK, 26 API34 emulator checks and 34 valid screenshots. Candidate remains unverified until exact-source evidence passes.

No player-save, run-history, scoring, application identity, signing, delivery or physical-phone claim is made. TEST123 remains the last owner-delivered/signature-compared APK.

## First exact-source failure and bounded correction
Run [38643522614](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38643522614) checked out exact source `6cec278930f087ed6c18b5d04c136a046ff22669` and ran from 2026-10-14T21:52:10Z through 2026-10-14T21:53:28Z. Reviewed history validation passed, then `:core:compileTestJava` failed before any tests ran because the retained `ProbeWand` fixture omitted Wand's abstract `onHit(MagesStaff, Char, Char, int)` method. No APK, API34 result, screenshot or artifact was produced.

The correction adds only the required no-op abstract-method implementation to the test fixture; the seven production call-site corrections and all assertions are unchanged. A duplicate same-source run was already active before the correction and is not new verification. The corrected exact source remains pending the full TEST174 gate.

## Corrected exact-source verification
Corrected source `5913850f400dbfe9fd0b547abbae71c20cd9992c` was verified by [run 38643884380](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38643884380) from 2026-10-14T21:55:06Z through 2026-10-14T22:05:52Z.

- Core artifact `11931736651`, ZIP SHA-256 `ab58ce2a031c47f2bb5b3f745a0eedcf7a9399542063b3965191d9e158c1bf27`: 1,596 tests, 0 failures, 0 errors, 0 skipped. All three `RemainingDirectWandDamageProvenanceTest` methods passed in 0.002 seconds.
- APK artifact `11931766535`, ZIP SHA-256 `4801b69528ddb80f57603ea4b1dd191bfa9e2f0b80d3acfcbf143316ed16416d`: `ADHD-Pixel-Dungeon-CODEX-TEST-176.apk`, SHA-256 `6cbce087ebdacfbb4a621d1689f72b86b2cad71274efe6f22a8a4c98b78feb29`; bundled checksum matched. Debug assembly and compiled-history validation against immediate previous compiled TEST173 passed.
- Android artifact `11931807905`, ZIP SHA-256 `8431b9c85cc88a39957ce5b7aeefb035dbf3f58d29f8370f0792d6b895c45e9f`: 26 API34 emulator tests, 0 failures, 0 errors, 0 skipped. All 34 PNGs decoded at 320×640; contact-sheet review found the expected readable TEST176 history, rules, score, UIMSD, stairs, tiles and gameplay states with no blank, corrupt or visibly clipped capture.
- A duplicate same-source run `38643546560` (run number 175, 2026-10-14T21:52:21Z–21:54:58Z) reproduced the already-recorded fixture compile failure and produced no APK/Android evidence; it is not counted as verification.

Verified behavior: Blast Wave's two normal-zap hits and staff detonation, Living Earth's two normal-zap branches, and Prismatic Light's two damage branches select the actual player, ordinary-enemy or boss creator and apply that damage factor exactly once while retaining original rolls, proc chances, source, direct mitigation, visuals, turns and RNG. Neutral 100% retains the original amount/source/next RNG.

Private delivery was skipped. This is CI/emulator evidence only, not physical-phone or owner-save verification. TEST123 remains the last owner-delivered/signature-compared APK. Do not rebuild unchanged TEST174; continue the distinct current-source offensive audit/backlog.
