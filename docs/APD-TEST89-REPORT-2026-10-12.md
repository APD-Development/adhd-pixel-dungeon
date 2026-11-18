# TEST89: crystal rewards with preserved generation draws

TEST88 source9e4dc5d failed full core run38323811041:1239executed,2GoldenLevelHashTest failures; Android/build/delivery skipped. No TEST88 APK was produced. Do not label that candidate verified.
The first implementation changed original random draws, causing additional map/mob/transition differences. It was replaced, not accepted by relaxing fingerprints.

The revised CrystalPathRoom retains the original complete random draws, ExoticCrystals chance checks, generator deck and duplicate undoDrop bookkeeping and sorting. It moves an existing reserved identity (including its exotic counterpart) from an early compartment to the final list position when needed, then explicitly substitutes standard Experience and Transmutation at the final two positions. Four other consumables, six total rewards, three keys, geometry and flags remain. No global probabilities or saves are altered; existing generated floors are not rewritten.

Revised code passes3072room paints3.796s:64seeds,fourentrances,sixdepths including22,exoticsettings0/80; both standard rewards and two-door routes checked.
Canonical snapshots compared across286golden cases. All242previouslystable cases have identical fields outside heaps. Three differences in the44alreadyunstable cases were not used to claim deterministic equivalence.
Rebuilt fingerprints through the existing24fresh-JVM procedure89.686s. All286stable/unstable classifications retained (242stable/44unstable);23stable fingerprints differ for approved reward contents. No GoldenLevelHashTest assertions or test classes were changed.
Normal validation, update flagOFF: both golden checks pass7.641s;242stable cases checked with recorderOFF and again with recorderON. The original rejected candidate's regenerated file was discarded.
TEST89 also retains the two UIMSD fixed-point boundary regressions and division-before-absolute-value correction already in the candidate. Prior26formatter/query and75combined local checks passed. Full expected1239core+21API34+18PNG/debug required and pending at this commit; no signed/delivered89claim. Previous actually produced APK87 sourcef2dc6a54836171acc3c2f9872fa5617da7bbf496/run38316987461.
