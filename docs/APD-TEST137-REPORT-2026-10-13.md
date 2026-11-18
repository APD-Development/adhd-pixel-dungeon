# TEST137 prepared offensive throw boundary

Six production files and six new permanent test methods. Item callbacks capture immutable creator roles when the throw begins and restore temporary attribution on completion or exception. Potion effects use that captured origin; terrain pressed by the throw remains environmental, including inside nested offensive contexts. Corrosive clouds inherit explicitly scoped potion origins. Caustic brew applies saved ooze attribution. Ooze rejects a missing origin without changing existing state. No transient item/context metadata is serialized, no actor references retained, no inventory-capacity or turn-cost changes. Existing global strongest-corrosion-source behavior is retained.

18 targeted checks passed in0.340s, including real caustic-brew application, save/restore, nested/exception cleanup, traps and prior ooze/gas behavior.633 broader recording/query/archive/rollback/inventory tests passed in23.652s with0failures. The initial local class-overlay reflection and outdated message-resource setup produced3failures; merging the compiled fixture tree and using actual branch messages corrected the harness without changing test assertions or game mechanics. Current-source CI is still required. Expected1472core/26API34/33PNG/debug/history.

This is incomplete source coverage, not completion of the three damage controls. Remaining toxic/fire/electric and other mixed/delayed effects, explosions and ability boundaries require investigation and tests. Three Damage controls remain disabled. Original scoring, save compatibility and raw records remain preserved. No release or physical-phone verification is claimed.


## Actual full verification result
Commit60f8451d3eed148da97a0fd078534b7773d09910/run38491431089:1466core tests passed,0failure/error/skip in281XML reports. Debug APK built and its in-APK history verified (artifact11861269013). API34 executed26 tests:25passed,1failed,0error/skip. The new score test expected scrolling without verifying content overflow; screenshots show the complete pending score dialog fits the chosen180-unit viewport. Its test-only constrained viewport is reduced to120 units, with an explicit content-overflow assertion and pre-swipe screenshot. The touchscreen assertion remains. TEST136 is an actual built APK, but NOT fully verified; TEST134 remains last completely passing candidate.

Bomb-source work is separate and not included in this commit.

## Actual verification and next candidate
TEST137 commit d420ee734b4233f244ff5a2a295c8f23c3a4947d/run38493240834 failed before compilation: the history validation rejected the changed ledger itself as unreviewed. No TEST137 tests or APK. Corrected the omitted ledger path without weakening validation. TEST138 retains the exact prepared source and native overflow assertion. Required1472core/26API34/33PNG/debug/history; pending. Last passing candidate remains134.
