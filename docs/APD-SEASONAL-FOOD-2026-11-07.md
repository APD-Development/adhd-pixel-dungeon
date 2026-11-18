# Seasonal-food explanation continuation — 2026-11-07

## Actual change

FoodExplanation now routes Pasty to new PastyExplanation for both the ordinary summary and expandable details. It reads the same current Holiday used by Pasty.satisfy, rather than listing every possible holiday benefit as one long explanation. No Pasty, Food, Wand, Hero, Barrier or artifact calculation changed.

The current form explains its own effect: ordinary form has no seasonal bonus; Lunar creates a separate meal with live challenge nutrition; April/Easter supply artifact recharge power (not guaranteed finished charges); Pride conditionally Charms an adjacent eligible non-boss; birthdays use the live full level requirement; Halloween uses current maximum HP and labels base healing before modifiers/cap; winter adds fractional wand charges; New Year keeps the stronger Barrier rather than adding reserves. Null-hero contexts withhold personal values. Inspection never applies effects or selects hidden targets.

## Regression evidence

Two new tests in BlandfruitExplanationTest reuse the existing isolated real-effect fixture. Actual Pasty.satisfy is invoked for four maximum-health values, verifying real healing and stronger Barrier retention; all calendar forms and no-hero descriptions are inspected without changing HP/meal energy. Actual birthday XP and five consecutive winter meals verify fractional charging, capacity, and no change from inspection. Quiet fixture suppresses visual particles only. Existing lunar nutrition/nonmutation regression remains.

Targeted first seasonal test and follow-up both passed; final targeted result338 tests. Logs /workspace/apd-seasonal-targeted.log and /workspace/apd-seasonal-targeted-2.log. Full-core /workspace/apd-seasonal-full.log is recorded below when complete. Required source-only Android debug/release/instrumentation Java and desktop compilation is requested separately; this is not Android runtime or physical-phone evidence. No APK assembly/version/publication, balance, save, identity or signing change.

Prior cooked-fruit source gate42409005559 succeeded on8c9d6a1675dc4443cda9eee9273596ef744f2adc. Downloaded artifact13588159508 SHA256a5c260bb85313eae338db0ee5cfcdb98fef25176b922e01842e19bd0bdb7486a contains exact source and all448 XML suites/2928 tests, zero failure/error/skip. Receipt registry updated. APD371 remains delivered playable build; these newer description changes are source only.

## Open scope

No claim of seasonal target-selection, every artifact-specific recharge interaction, translated UI, emulator rendering or physical-phone acceptance in this batch. Full project description audit remains in progress. Continue next approved facet after backing up verified work.

Full local result:2930 tests/448 suites, zero failures/errors/skips. Raw XML /workspace/apd-seasonal-local-junit.zip. Exact source49a933bb114cff4ab4c593635e41f658268f4dac passed source-only run42409618215/job128514705701 at2026-11-07T20:11:20Z:2930 tests/448 suites, zero failures/errors/skips, with Android debug/release/instrumentation Java and desktop compilation. Artifact13588064378 is510327bytes, SHA256 8e57be5e06982bcfb137c92da475fedfac665dabbe6d60620612d1b2f4933c1d; downloaded receipt and all448 raw XML suites independently checked. No APK assembled; no physical-phone claim.
