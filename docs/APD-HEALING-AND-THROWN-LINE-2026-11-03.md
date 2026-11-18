# Healing Off shortcut and clear thrown line (2026-11-03 UTC)

Owner reports TEST350 Off(one tap) does not work and requests throwing at visible enemies with clear line of fire, without bending around corners.

## Confirmed Off cause and correction

The setting callback already saves -1. However, WndCategoryHotbar.open always opens an item chooser; its direct-drink check existed only after selecting an item from that chooser. Therefore Off did not provide a one-tap quickslot even when the slot had exactly one known healing stack. Move this unambiguous shortcut before menu construction. Keep ambiguous, unknown, non-healing and enabled-threshold selection behavior; no change to potion quantities, healing, turn costs, RNG, storage or preference defaults.

Native TEST350 reproduction41564748055 failed exactly: expected quantity1 after actual Off button + actual healing quickslot tap, observed2 with chooser present. The same case checks threshold setting, Save, warning Cancel (no HP/item/turn change), confirmed normal use, and actual Off preference -1 first. Earlier41564119872 passed the real threshold/confirmation flow;41563626330 failed in test setup by reading preferences before Android startup, corrected without application change. These are separate results, not passed Off acceptance.

## Confirmed thrown-line case and correction

Original unenchanted ThrowingKnife failed with hero(2,2), enemy(6,5), wall(4,4): enemy was visible through actual ShadowCaster; forward Bresenham collision99, target166; reverse ray reached hero66 without collision. The same geometric straight ray can round onto different neighboring grid cells in opposite directions. For ordinary thrown weapons only, retain the original forward collision unless a currently visible enemy has a fully clear reverse projectile ray. This never computes a route around a wall or ignores an intervening creature. Unseen targets retain normal rules. Original Spirit Bow and Projecting enchantment behavior is preserved; no changes to range caps, damage, accuracy, durability, turn costs or combat RNG.

Six real-physics unit tests cover the originally failing tie case, long straight/diagonal visible shots, blocked corner, intervening creature, unseen enemy and original Projecting bypass. The initial fixture needed blobs/traps initialization; fixture errors were corrected before recording the actual throwing assertion failure. The clear-ray assertion failed before the production correction, then all six passed.

## Verification boundary

Local complete core suite:2311 tests,0 failures/errors. Three new Off policy cases protect the single-stack shortcut, ambiguity/non-healing items, and unknown/enabled-threshold exclusions. Native healing test uses actual taps and restores all original hotbar/preferences. Full required Android debug/release/desktop compilation, full prior69 API34 cases plus native Off case, six display profiles and paired tester checks must complete before publication. No phone acceptance claim. Source/build artifacts are not yet prepared or delivered for this correction; retain350 fallback.

Owner cap:3 Developer plus3 Alpha-Testers APKs per New York day. Prepare one351 pair, not another series of application builds. Preserve all350 release assets/signing/application ID and matching source process.

## Owner correction: absolute conversion and current-run upgrade

The previous implementation deliberately took max(trinket chance, slider); its tests validated that mistaken minimum instead of the owner's requirement. Revised policy returns the exact selected Healing-to-Shielding percentage for Healing only. Mob.createLoot now uses the same typed policy for class-based drops, fixing the vampire-bat path that previously consulted the trinket alone. Other potions/scrolls keep their original probabilities and RNG draws. Tooltip/settings wording now states the override explicitly.

Owner explicitly requires the selected chance to apply to the current run after installation. Loading an older save adopts the selected preference once for a run whose prior chance is known; saving records conversion-policy version2. Later changes for new runs do not reprice that upgraded run. Unknown historical chance remains unknown, replay threads do not migrate, raw history is never rewritten, and RunRecorder's existing ruleset-change mechanism records the new absolute-v2 identity. Custom Rules, scoring, inventory and generated loot remain intact. Newly generated future Healing drops obey0 even with +3 Exotic Crystals. Existing Shielding items remain unchanged.

New tests cover all81 slider percentages at all trinket levels,320 seeded actual bat drops at0,1600 exact bat conversion/RNG cases, and five upgrade/no-RNG/replay/persistence cases. Full local2319 tests pass after updating four current-recording expectations to the explicit r4 absolute policy; historical Ruleset.of identities and history readers remain unchanged. Native coverage includes actual old-run continue, retained +3 Crystals/gold/score/custom rules,0% bat drops, persisted upgrade marker and later new-run preference separation. Android results remain pending.

CI351/run41566121314 stopped before any application compilation because its build-history ledger omitted docs/APD-NEXT-BUILD-HISTORY.json from reviewed paths. Correct that metadata without weakening the history gate; no351 APK existed or was delivered. Next attempt uses the actual next ordinal,352, for one pair only.

352/run41568143488:2319 tests ran, with2318 passing and only the generated-history icon check failing (THROWING_KNIFE was used without ITEM_). No APK assembly was reached. Corrected metadata uses ITEM_THROWING_KNIFE, and the history tool now validates actual ChangeIcons/Icons/item-sheet symbols before embedding;20 Python history-tool cases pass. No gameplay code changed after the local2319-green run. Next actual workflow ordinal353, one pair only.

## Concurrent-cache correction discovered during required validation

353/run41569585237 was canceled on owner Stop, then resumed on explicit Continue. Its resumed core run failed CacheConcurrencyTest.rowCacheSurvivesIndependentWriters (cached1 expected,0 observed); no APK assembly occurred. DatasetStore.deleteCache deleted active .tmp files. A writer could then remove a newer committed cache and fail renaming its now-missing staging file, leaving no cache and forcing a rebuild. Raw logs still loaded successfully. New latch-controlled regression reproduces that exact publication window without relying on timing; it failed before the production correction. Invalidation now removes committed .apdr files only, leaving staging files to their owning writers. Full local2320 tests pass after the correction. No tests were weakened or skipped. Preserve the earlier failed result. Next actual producer ordinal354; still only one forthcoming APK pair, no351/352/353 application binaries were produced or delivered.
