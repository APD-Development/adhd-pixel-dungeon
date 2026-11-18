# TEST150 Inferno and fire-transfer provenance

Status: implementation verified by TEST151; superseded by the verified TEST152 source-safety correction.

## Scope

- Inferno now retains immutable PLAYER/ENEMY/BOSS/UNATTRIBUTED mixtures through seed, diffusion, save/restore, direct Burning, and neighboring Fire creation.
- Infernal Brew captures its actual thrower once and uses that origin for every seeded Inferno cell.
- Chaotic Censer's existing delayed saved origin is consumed by Inferno through the existing lexical effect context.
- Attributed Burning transfers the ownership mixture for its current damage tick when it ignites flammable terrain.
- Legacy Inferno saves without the additive metadata restore as UNATTRIBUTED; no attacker object is retained.

## Preserved behavior

The change does not alter field density, diffusion, terrain destruction, duration, turn ordering, damage source objects, mitigation, or RNG calls. At neutral 100% it is intended to be behaviorally identical. Unowned environmental Inferno and fire remain unscaled.

## Retained regression coverage

`InfernoDamageProvenanceTest` covers all 20 percentages for PLAYER/ENEMY/BOSS direct Inferno contact, exact-once scaling, source identity, Inferno save/reload and legacy fallback, neighboring Fire transfer, Burning-to-terrain transfer for all three roles, neutral density/volume/next-RNG identity, and production wiring for Infernal Brew and delayed Chaotic Censer.

## Verification record

- First independently captured clock during this bounded invocation: `2026-10-14T11:39:59Z`; earlier active duration is unknown.
- Source was read from real branch head `116fc601549e83e02742b8541d3767f559ee2d85`.
- Candidate application commit: `57b3b5706f7478f742164d11c5a5ea74de94ed36` (expected-head lease from `116fc601549e83e02742b8541d3767f559ee2d85`).
- Local source inspection and exact replacement guards completed. A real local Java/Gradle repository is not present in this execution workspace, so no local Java test result is claimed.
- Full core, debug APK, compiled immediate-previous history, API34 emulator, and screenshot evidence remain pending until the exact application source is requested through the approved existing workflow.

### TEST150 gate result

Run `38558479124` reached the compiled-history input gate and failed at `2026-10-14T11:43:11Z`: the manifest omitted the already reviewed `docs/APD-TEST149-DRAGONS-BREATH-2026-10-14.md` evidence path from the TEST149-to-current comparison. No core test, APK build, compiled-history check, API34 test, or screenshot ran. The correction adds that path only; application source remains candidate `57b3b5706f7478f742164d11c5a5ea74de94ed36` and is still unverified.

Three Damage controls remain disabled. Firebloom, vault/environment boundaries, remaining offensive sources, and complete class/boss/combination acceptance are still unresolved.



### TEST151 exact-source verification outcome

Run `38558697915` verified exact workflow source `fe6527fe009234cdcf81edd60eebaba1ea5b0b7a` (unchanged application candidate `57b3b5706f7478f742164d11c5a5ea74de94ed36`) from `2026-10-14T11:44:25Z` through `2026-10-14T11:55:00Z`. Downloaded XML proves 1536 core and 26 API34 tests passed with 0 failures, errors, or skips. Debug assembly and compiled immediate-previous-APK history against TEST149 passed. All 34 PNG captures decoded at 320×640; contact-sheet review found the expected readable menus, dialogs, and gameplay frames with no corrupt capture.

Artifacts: core `11891361617`, ZIP SHA-256 `20c2017a2b428ce702ad58e878a61e72556aa3b88c2e3e4a7c0897cd0cb7200e`; Android `11891777754`, ZIP SHA-256 `5ebb39515a88eb10d20b6e63b266c8546fba0b7d3b223c6811066d0eef0e8dc5`; APK `11891381527`, ZIP SHA-256 `a2cc1fa7b9d982e7ef34fbe70a274f3ad6b1be42ccf34caa2a08af939b56d9b1`, contained APK SHA-256 `19da0e373010fbe2fe94acc4d9992644834ed89e225a5505455198c1cd4d29e1` with checksum passed. Private delivery was skipped. This is emulator/CI evidence, not physical-phone or owner-save evidence. TEST123 remains the last owner-delivered/signature-compared APK.
