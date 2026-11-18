# APD hidden aggregate cleanup — 2026-10-16T09:36:49Z

## Confirmed defect
Turning UIMSD hidden information off removed hidden filters, sorts, displayed columns, group keys and aggregates whose primary value column was hidden, but it retained aggregates whose denominator, condition or distinct-by column was hidden. The query therefore still depended on hidden data while the engine silently omitted those aggregates from the displayed result.

## Bounded correction
Hidden-information shutdown now checks all four aggregate column references: value, ratio denominator, conditional column and distinct-by column. An aggregate is removed when any resolved reference is internal. Public aggregates and unknown forward-compatible references keep their existing behavior. Recorded history, derived cache formats, saved-view files and scoring are unchanged.

## Retained acceptance
`UimsdModelTest.disablingHiddenInformationRemovesEveryHiddenAggregateDependency` covers a public aggregate plus ratio-denominator, condition and distinct-by dependencies. Exact TEST238 application source must fail the method; corrected source must pass all 1,706 core methods.

Expected TEST239 gate: one intended TEST238 failure, then 1,706 corrected core methods, debug assembly, complete immediate-previous TEST238 APK history, 33 API34 emulator methods and 42 valid screenshots. No publication, tester distribution, physical-phone result or owner install-over/save claim is authorized by this record.

## 2026-10-16T09:52:42Z — TEST239 hidden aggregate cleanup fully verified
- Exact source `6182b042b1d93b64341f41a516a49ef3d12c93ee` completed [full run 38881765134](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38881765134) successfully from 2026-10-16T09:37:49Z through 2026-10-16T09:52:42Z.
- Dedicated [run 38881765302](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38881765302) reproduced exactly one intended failure on exact TEST238 application source `d39de932c69c23bcb013a8ee86df930718ad39c8`: `disablingHiddenInformationRemovesEveryHiddenAggregateDependency`. Corrected source passed all 1,706 core methods across 335 suites with 0 failures, errors or skips.
- Debug assembly and complete immediate-previous produced TEST238 history passed. The TEST239 APK contains 285 history TSV records and its new record has both required categories: verified TEST238 evidence and the hidden-aggregate correction.
- Independent artifact recounts confirmed 33 API34 emulator methods with 0 failures, errors or skips and exactly 42 PNGs, all 320×640. The complete 42-image contact sheet was reviewed without a blank, malformed or visibly clipped capture; the app log contained no fatal app-exception, ANR or native-abort signature.
- Produced APK: `ADHD-Pixel-Dungeon-CODEX-TEST-239.apk`, 38,589,027 bytes, SHA-256 `a9d634a1b9c791e971ccf94e1c864f3864144d6e908a68e2f515ecbaf50c30bf`. Compiled version `4.0.8-apd-INDEV-bC239-TEST`, application ID `com.charltonchris29.adhdpixeldungeon.indev`, and extracted v2 certificate SHA-256 `067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e` match TEST238 identity.
- Artifact IDs / independently downloaded ZIP SHA-256: targeted `12037697502` / `482d8a7f5f60b158426b6b9929ac011766004314f6d2109b33bdafc2ef41b08b`; core `12037837605` / `3aef277fc6c50b65eba3aad3bf9a06c667994dc730d59b6baaa49b79e7a1d48c`; APK/history `12037962338` / `18ec1f0391a2085edbf8db967945e5d434933df512962156c5160e4035c9fda1`; Android/visual `12038079510` / `cbe9be1aa815eac9533f19c4feadc4176a29459594d8b492088d2d43d14ed8ef`.
- Scope remains only cleanup of hidden aggregate dependencies when hidden information is disabled. Raw histories, saved views, derived caches, saves, original scores, gameplay, all 18 rules, turns, RNG, all 50 Cleric lore entries/private grudge/opt-out, signing configuration and application-ID configuration were not changed.
- TEST239 is a fully CI-verified candidate only. No publication, tester distribution, physical-phone result or owner-device install-over/save acceptance occurred. TEST231 remains the latest privately delivered fallback.
- Do not rebuild unchanged TEST239 solely to record evidence. Continue the broad UIMSD reliability audit; audio and unconfirmed armor/sentry acceptance remain open.
