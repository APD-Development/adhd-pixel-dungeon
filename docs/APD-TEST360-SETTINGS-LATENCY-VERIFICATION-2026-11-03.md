# TEST360 APD Settings latency verification

Final status at 2026-11-03T15:53:39Z: accepted as a private emulator-verified candidate. It was not published or distributed. TEST358 remains the latest actual private release and fallback.

## Exact application and build evidence

- Application/build source: `88b2e837e10a73290791861824542c9d8616d8e3`; only production input differing from TEST358 is `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/APDSettingsPane.java`.
- Producer run `41661720393`, job `126006339720`: success. Complete 434-suite/2,331-core inventory, desktop classes, Android debug/release compilation, reviewed immediate-predecessor history and private APK assembly passed.
- Developer APK SHA256 `bfc829fc92b950b6c13228922eb2fe4cc1f6aa802e6427be961b12f97a94a07e`; artifact `13241313903`, ZIP digest `sha256:fb2e0b166a703b67954ff0284f5f3035920071f33716418de37da279e8e06bfd`.
- Player run `41661719864`, job `126006336766`: success with all 2,331 core cases and matched signer, application ID and embedded history. Player APK SHA256 `8d40c084e51a86c424f96913a630739cb69313df66a570806129855f4dd7d221`; artifact `13243216100`, ZIP digest `sha256:8b906cd5b5602aa2b5356818bd8d8814a215769b7b00f76aadffd1b3070a9378`.

## Native and display acceptance

- Original API34 job `126008868337` ran all 71 cases in 561.655 seconds and passed 70. The sole failure was a test-only expectation: the fixture treated title-only `btnCurrentCustomRules` as eligible, although production deliberately skips it outside `GameScene`. The aborted run then lacked one valid screenshot. Both failures remain retained.
- Test-only commit `82e0687cc9301d392600ae6c989294b37622c03b` requires materialization for eligible visited rows and zero allocation for unvisited, empty and context-ineligible rows.
- Exact-APK run `41666064548`, job `126021584536`: success. It proved application-input identity with the original TEST360 source, downloaded and rehashed the original Developer APK, assembled instrumentation only, and passed `OK (71 tests)` in 648.911 seconds. All 104 required PNGs validated at 320×640.
- Exact-APK evidence artifact `13246513726`: 7,849,285 bytes, digest `sha256:0b5c66cdaea08061bed80e418c7fba52b4cb22e46888255d939d0d6b9746468a`.
- Display run `41661720020`: attempt 1 retained two infrastructure/capture failures (small-profile GitHub API stream cancellation; short-landscape native test passed before emulator-offline corrupt capture). Failed-job-only attempt 2 completed at `2026-11-03T15:36:20Z`; all six exact-APK profiles passed: small, compact, square, short landscape, M53 representative and RedMagic/API36 representative.

## Result and boundaries

The category root creates zero hidden description blocks. Eligible non-empty descriptions materialize on first category visit and remain retained. The unavailable current-run rules row remains lazy on the title screen. All settings, labels, preferences, touch paths, six categories, scene restoration and narrow-screen behavior remain covered.

Automated emulator evidence only. No physical-device acceptance, publication, tester distribution, public release, paid service, save/statistics/rules/scoring/RNG/gameplay/signing/application-ID change or application rebuild occurred during the corrected retest. The executable build request was not changed for verification-only evidence.
