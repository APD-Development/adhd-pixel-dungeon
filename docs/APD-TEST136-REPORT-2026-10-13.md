# TEST136 verification correction

TEST135 source 6c31c64dda79bf09f320cd39bfbfe6fffa2c03e5 failed run38489633963:1464 core tests,3 failures; no APK or Android execution. Failures were a runtime-game dependency in statistics/query code, two stale cache-version assertions in one test, and missing telemetry1.11 milestone.

The correction introduces a frozen pure-standard-library version1 rules codec; statistics/query code no longer imports game rules or scoring classes. Existing history, missing metadata behavior, and original earned scores are preserved. Cache tests continue to exercise rejection of builder10 and byte-preserving reconstruction, with current builder12 asserted. The additive1.11 milestone is documented.

15 focused checks passed locally in0.498s, including all affected contracts and recording/query checks. Two permanent codec tests cover frozen keys, all60 scroll counts, unsupported settings/scoring metadata and missing authority. Full current-source core/Android/build checks remain pending. Expected1466 core,26API34,33PNG. Three Damage controls remain disabled; no scoring awards or production compression changes.

Last passing application candidate TEST134 eb5ba00c6705d681c13e609c65f70dfb331695aa:1437 core and25API34 tests passed,29PNG; no physical-phone claim. Last delivered compatible APK remains TEST123.

## Actual full verification result
Commit60f8451d3eed148da97a0fd078534b7773d09910/run38491431089:1466core tests passed,0failure/error/skip in281XML reports. Debug APK built and its in-APK history verified (artifact11861269013). API34 executed26 tests:25passed,1failed,0error/skip. The new score test expected scrolling without verifying content overflow; screenshots show the complete pending score dialog fits the chosen180-unit viewport. Its test-only constrained viewport is reduced to120 units, with an explicit content-overflow assertion and pre-swipe screenshot. The touchscreen assertion remains. TEST136 is an actual built APK, but NOT fully verified; TEST134 remains last completely passing candidate.
