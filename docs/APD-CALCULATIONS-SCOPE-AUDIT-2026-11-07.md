# Calculations interface scope correction — 2026-11-07

## Authorization and owner correction

The owner's Ring of Arcana/Obfuscation request was not blanket authorization to redesign APD item inspection. The earlier description instruction asked for clear ordinary explanations and relevant expandable mathematical details. No explicit approval for a universal item calculation system was found; its visible conversation timestamp is unavailable. Commit9749e461ce804328c42e64a9558201d8aaeb04fa dated2026-11-06T01:33:06Z establishes implementation provenance only. Its author/account does not prove approval.

The owner's current clarification supersedes the preceding blanket item-panel removal: retain genuinely complicated math, remove ordinary-item panels, flag ambiguous cases. In the follow-up question about artifact/trinket resource formulas and separate talent, buff, armor-ability and Cleric-spell panels, the owner answered: “Keep existing mechanic-specific panels pending review.” This is retention pending review, not a finding that every retained instance needs a panel permanently.

## Every interface entry point audited

| Entry point | Correction / retained scope | Reason |
|---|---|---|
| WndInfoItem.fillFields/layoutFields, inherited by WndUseItem | Calls itemCalculations instead of the generic item dispatcher. Ordinary item.info text/actions/scrolling unchanged. | No metadata/keyword/glossary trigger for ordinary items. |
| MechanicsDetails.item / itemInContext | Preserved as internal read-only explanation code/tests; disconnected from automatic item UI. | Preserve approved explanation work without turning all helpers into buttons. |
| WndInfoBuff | Existing specific buff explanations retained pending owner review; glossary-only and duration-only fallback cannot create panels. | A generic timed counter or word in a description is insufficient. |
| WndInfoTalent | Existing specific talent calculations retained pending review; rank metadata/glossary alone cannot create panels. | Current rank and common vocabulary do not justify another interface. |
| WndInfoArmorAbility via WndTitledMessage | Existing ability charge/modifier/recharge/effect details retained pending review. | Owner explicitly chose to keep this group pending review. |
| WndClericSpells.SpellButton.onClick | Existing spell-specific optional detail supplier retained pending review. | Same owner decision; no Cleric gameplay change. |
| WndClericSpells second information path | Same existing spell-specific supplier retained pending review. | Both spell-info construction sites inspected, not only one. |
| WndTitledMessage optional-detail constructor | Preserved; ordinary constructor still has no detail supplier. | Shared plumbing does not itself opt ordinary messages in. |
| MechanicsInfoPanel.add | Preserved bounded scrolling and existing opt-in expander; empty supplier output creates no control. | No new control, preference, label or universal display rule added. |

## Item-specific result

**Removed:** automatic panels on seeds, food (including cooked/seasonal food), routine potions/scrolls, keys, currencies, ordinary utility/crafting items/bombs, unenchanted weapons and unglyphed armor. Ordinary descriptions and their live numbers, before/after ring contributions, provenance and unidentified-source ranges remain intact. No blanket glossary is appended to retained item panels.

**Clearly retained:** identified Ring of Arcana (exponential power, chance cap versus effect strength); known beneficial armor glyph and weapon enchantment calculations (including Obfuscation detection, proc caps and Arcana interactions), without unrelated base armor/weapon/ability/general glossary sections; Force (strength-derived tier, armed/unarmed/Brawler modes); Tenacity (missing-health-dependent exponent); Warding and Regrowth wands (separate ward lifecycles and cumulative depletion); Retribution and Psionic Blast scrolls (missing-health-dependent attack power and crowd-dependent exponential self-damage).

**Retained pending owner review, per explicit reply:** identified artifacts and trinkets use only their existing mechanic-specific helpers. Especially clear examples are Chalice nonlinear prick damage and healing interval, Chains soft/acceptance limits, Hourglass saved time budget/tolerance, Talisman range/cone/resource relationships. The rest of this group is not declared permanently warranted. Existing specific talent/buff/armor-ability/spell details also remain pending review. Generic buff/talent fallback text was removed from the interface path.

**Flagged borderline instances:** other standalone ring panels where plain descriptions may suffice, routine wands versus their Warlock/charge interactions, and bomb/crafting distributions. Their underlying helpers are preserved; this change does not invent new panels for those borderline instances. Owner can select them individually. Existing simple before/after numbers stay in ordinary descriptions. No new global setting or automatic complexity score was introduced.

## Implementation and tests

Production changes are limited to MechanicsDetails and WndInfoItem. The item path calls only the specific retained helpers; generic glossary dispatch no longer controls button visibility. Buff generic duration text is available only alongside a specific effect; glossary-only buffs/talents return no panel and retained panels do not append the universal glossary. No combat, generation, resources, hidden identity, balance, save or identification calculations changed.

Seven new core regressions cover: normal food/seed/potion/scroll/plain gear panel absence with ordinary description and Bundle-state preservation; Arcana/glyph/enchantment math retention and hidden-upgrade gating; no glossary/duration-only buff panels; Force/Tenacity/Chalice nonlinear cases; Chains/Hourglass/Talisman cases; explicitly retained artifact/trinket cases and Warding; nonlinear blast-scroll retention versus ordinary scroll exclusion. Existing Arcana effects, underlying formulas, description purity and save tests remain.

Android QolItemFeedbackDeviceTest retains ordinary inspect/use absence/text/action/state assertions and adds actual-pointer expansion checks for Arcana and Obfuscation. These native checks are not claimed executed without a completed emulator/device run. Required platform compilation and complete core results are recorded below. No APK/release or physical-phone claim.

Verification history: targeted MechanicsExplanation/ArcanaInfoDisplay/ItemDescriptionPurity passed before the final owner retention change. The first full suite after that change had one new fixture failure: the Warding test marked level/curse known but omitted curChargeKnown, so production correctly withheld the panel. The fixture now explicitly marks a fully identified wand and separately asserts that clearing charge knowledge suppresses the panel. No production privacy guard was weakened. Earlier successful local runs are retained in /workspace/apd-selective-calculations-targeted.log, /workspace/apd-selective-calculations-full.log and /workspace/apd-selective-calculations-final-full.log. The failed run is /workspace/apd-selective-calculations-owner-final.log. Current final rerun /workspace/apd-selective-calculations-owner-final-2.log.

Final local result:2939 tests/449 suites, zero failures/errors/skips. Raw XML archived /workspace/apd-selective-calculations-owner-final-junit.zip. Required exact-source full-core/Android-debug-release-instrumentation-Java/desktop gate requested. No native test execution or physical-phone claim.

Final completed blast-scroll-inclusive local result supersedes2939:2940 tests/449 suites, zero failures/errors/skips. Log /workspace/apd-selective-calculations-blast-final.log and raw XML /workspace/apd-selective-calculations-final-2940-junit.zip. Final exact-source platform gate requested; do not substitute the earlier2939 gate as evidence for the added scroll dispatch.

## Complete former item-dispatch branch inventory

| Former branch | Current item-interface outcome |
|---|---|
| Food, including cooked/seasonal food | No calculation panel; ordinary description improvements retained. |
| Plant.Seed | No calculation panel. |
| Wand | Warding/Regrowth retained; routine wand panels withheld and borderline modifier interactions flagged for review. |
| Artifact | Existing specific helper retained pending owner review. |
| Ring | Arcana/Force/Tenacity retained; other standalone ring panels flagged, existing affected-item before/after text retained. |
| Trinket | Existing specific helper retained pending owner review. |
| Potion | No automatic panel; exceptional potion cases can be reviewed individually. |
| Scroll | Retribution/Psionic Blast retained; routine scrolls do not get panels. |
| UtilityItemExplanation | No automatic item panel; unusual Ankh/Seal/Waterskin cases remain available in their underlying helpers for individual review. |
| Bomb / CraftingEffectExplanation | No automatic item panel; unusual distribution/recipe cases flagged. |
| Armor/ClassArmor | Known beneficial glyph detail only in item panel; ordinary base statistics and unidentified origins remain in normal text; separate armor-ability panel retained pending review. |
| MagesStaff | No automatic embedded-wand calculation panel; ordinary staff/wand description remains; detailed helper preserved for review. |
| MeleeWeapon ability | No automatic panel just because a weapon has an ability; helper preserved for individual review. |
| MissileWeapon | No automatic panel; approved affected-item before/after ring values remain ordinary text. |
| Weapon | Known beneficial enchantment detail only; no generic base-weapon/glossary panel. Curse-specific advanced panels flagged for review rather than newly enabled. |
| Generic glossary | No longer enables any ordinary item control, and no longer appended to buff/talent interface details. |

Final source verification completed: b447729795c4d975d45e061ea275780dffee1e04 passed run42411569092/job128520629618. All2940 tests/449 suites have zero failures/errors/skips; required Android debug/release/instrumentation Java and desktop compilation succeeded. Artifact13590115511 is513653bytes, SHA2565f90229279bcd9c62f68e2af341abb6186a1213e91bf8beda7cc0d63f3db7d38; downloaded archive, embedded source and all raw XML independently checked. This is compilation/core evidence, not execution of native UI assertions or physical-phone acceptance. No APK assembled/published.
