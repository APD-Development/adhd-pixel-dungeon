# TEST88: restore the approved crystal-path rewards

## Root cause and room identity
The earlier screenshot's six compartments and three crystal keys identify CrystalPathRoom, not CrystalChoiceRoom or CrystalVaultRoom. No new screenshot was attached to the latest report, so the precise floor-22 image cannot independently be matched.

Verified TEST79 source a611c841e64a8bca592831d3200a5a65e1735b61 and current real branch e70f49dbeb3f6fba9becbc015c96027f099a5867 have identical CrystalPathRoom source: original SPD behavior, choosing only one forced Experience/Transmutation reward, allowing exotic substitution, and sorting random rewards into the remaining final compartment. A Retribution scroll in that compartment is permitted by that old code. This is missing implementation on the real branch, not evidence of another placement system overwriting the approved reward.
The stale local checkout contained a proposed both-rewards test, but the actual GitHub branch had no such test. Neither the stale local history nor its uncommitted files were pushed.

## Fix
Reserve one standard Potion of Experience and one standard Scroll of Transmutation before generating the four other items. Reserve their identities for existing duplicate exclusion. Remove these two reserved objects from rarity sorting and place them directly on the two final pedestals. Both final compartments remain behind two crystal doors; three keys, six total rewards, earlier random potion/scroll rooms, geometry, duplicate undoDrop bookkeeping and exploration flags are retained. Standard designated rewards are not replaced by exotic variants. Global Generator probabilities are untouched; necessary room-local random draws differ from the old selection.

Level.drop does not convert or substitute potion/scroll identities. No global drop or exotic system changes were necessary. Existing already-generated floors are not rewritten, and the owner's save/inventory remains untouched. The correction applies to newly generated rooms; it does not replace a Retribution scroll already stored in a save.

## Tests and build status
Untouched baseline failed the repaired fixture: required Transmutation count was zero. An initial headless setup lacked Game.instance and was corrected before the meaningful baseline comparison; that fixture exception was not called a game bug.
Fixed implementation passed one permanent JUnit test with 3,072 room paints: four entrance directions × 64 seeds × six depths (1,6,11,16,22,24) × two exotic settings (0,80). Each checks six total items, exactly one standard Experience and Transmutation, their hidden final-compartment flags, and exactly two crystal-door crossings to each designated prize. The final local run passed in3.562seconds.
Production Java/test compilation passed. Full expected1239core/21API34/18PNG and debug-APK verification are requested for TEST88; pending at this source commit. No TEST88 signed-compatible APK, publication, Android-room screenshot, physical-phone verification or complete normal playthrough is claimed yet.

## UIMSD fixed-point display correction
CellFormatter used Math.abs on a signed long before dividing; Long.MIN_VALUE overflowed, producing a duplicated minus sign and negative fractional digits. Dividing and taking the remainder before absolute values corrects this supported boundary without changing calculations, stored values or formats. Two boundary cases plus existing formatter/query tests pass:26tests,0.099s. Combined unique75crystal/cache/recovery/index/storage/query/formatter checks passed4.512s. Full verification run38323811041 at application source9e4dc5d9ff555a65ef7b3a784b6a6be95b49ac4d is pending at this report revision. No signed/delivered88APK claim yet.
