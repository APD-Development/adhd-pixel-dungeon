## APD442 source continuation, November 11, 2026

The latest completed APK remains APD441. This checkpoint records newer source verification, not Android or APK acceptance. The alternate thrown-weapon Seer Shot handler omitted the current branch. A real throw plus real field-of-view regression failed on branch 1 while four controls passed. Adding the existing branch assignment, as already used by SpiritBow, makes all five methods pass. The existing saved branch/depth/position fields, duration, cooldown, eligibility and main-branch behavior are preserved.

Four ordinary talent descriptions now match their existing handlers: Veteran's Intuition rank-one armor identification is 2x; Speedy Stealth gains up to ten momentum only when freerunning is inactive and off cooldown; Provoked Anger lasts five turns and another shield break adds five remaining turns; Lingering Magic lasts five turns and another zap refreshes it. Actual shield-break, wand-use, attack-consumption and scheduler-expiry tests verify the two timers. No damage, identification, movement or timing formula changed.

Fresh production and test compilation followed by the full local core suite passed 3,197 tests across 464 suites, zero failures, errors or skips. Eight additional ordinary-window captures and three actual branch-specific reveal frames are prepared within the existing native cases. Android compilation, exact-APK native execution, original-image review, source scan and delivery remain pending. See APD-442-LOCAL-SOURCE-VERIFICATION-2026-11-11.json and the three bounded review records for exact scope, hashes and retained failure evidence.

The inventory still has 445 source units, 442 complete and three partial. The remaining ordinary talent audit and owner decisions for Cursed Wand and existing mechanic-specific panels are not complete. The historical checkpoints below retain their original context and do not supersede this newer source-only status.

Latest verified source:443source units,443full/partialreviewed,357corrected,435own-sourcecomplete,8open;5346Englishentries/2432itemkeys. Full3138core457all0.415latestpublished;416held after actual Drowsy grammar defect, corrected in417source. Native417 pending. Counts are source units, not distinct items or complete Android coverage.

## Current status — November 9, 2026, active owner-authorized development

Active authorization through16:00EST/21:00UTC. Audit unfinished:442source units,441 fully/partially reviewed,354 corrected,434 own-source complete,8 unresolved;5343 English entries/2432 item entries. Five previously omitted direct talent/secondary-window sources are now explicit: four window sources fully read, actual Talent source partially read. These source-unit counts do not mean distinct items or whole-game Android coverage. Latest published412 (3134core/12targetedAndroid), later source3135core/457suites all pass.413sourcea0c2f60ec building42616639575; later armor seal wording excluded from that cutoff. No physical-phone/acoustic verification. Historical notes below retain exact prior cutoffs.

## Latest continuation — crafted spells, runestones and Duelist examples

223 source units have full or partial reviews; 210 have corrections and 95 source reviews are complete. 332 remain unresolved. All 3,030 core tests / 457 suites passed. The source audit is unfinished. New Summon Elemental variant windows are staged for380 Android checks. Existing379 is delivered; no physical-phone or Prison/Tengu audio-fix claim.

## Latest continuation — bombs, seeds and plants

195 source units now have full or partial reviews; 187 have corrections and 67 source reviews are complete. 360 units remain unresolved. The combined 3,016-test / 456-suite run passed with zero failures/errors/skips, followed by targeted final wording checks. Android window fixtures are prepared but have not been run; no new APK is claimed. See the correction record for scope and exact evidence.

## Latest continuation — curses, class privacy and ability previews

170 source units have full or partial reviews; only 46 are complete and 381 remain unresolved. The combined 3,013-test / 456-suite run passed with no failures/errors/skips. A subsequent targeted check covers the related fifteen-weapon preview correction. See the correction record for exact scope and evidence. No new APK or Android acceptance is claimed. Continue the remaining categories and variant review.

## Wands and potions continuation

Full source check now 3,005 tests / 456 suites with zero failures/errors/skips. Inventory distinguishes 113 full/partial source reviews from 46 completed source reviews; 381 units remain unresolved. Completed source review is not Android acceptance. Known durations, challenge behavior, free Transfusion cost and area-effect descriptions corrected; actual source/unknown-state tests and pending Android windows are detailed in `APD-DESCRIPTION-CORRECTION-2026-11-08.md`. Continue the scroll and remaining-category review. No new APK has been built for this unfinished audit.

## Current continuation — November 8, after APD378

The earlier checkpoints below are historical. Current correction details: `APD-DESCRIPTION-CORRECTION-2026-11-08.md`. Inventory now includes 427 source units, including two previously omitted origin/range providers. Only 34 units are marked complete; partial reviews are counted separately. Full combined 2,997/456 check passed with zero failures/errors/skips. The Guard pull report has a separately documented scheduler correction. Current corrections are not yet Android-verified or delivered in an APK. Continue individual category and variant review; do not claim comprehensive completion.

# Player-language item audit — in progress

Inventory:425source units and2388item-message entries. These are not counts of reviewed item types. Current completed review:11food source units plus PastyExplanation; Blandfruit source reviewed with secondary potion-text dependency unresolved. FoodExplanation/BlandfruitExplanation helper reviews remain partial until that dependency is closed. Food category contains14playable concrete types including chunks/leftovers, plus a recipe placeholder; seasonal/cooked forms are tracked in the notes, not counted as separate source units. Do not call this an entire-item audit.

Rewrote ordinary food descriptions individually after reading actual food classes, hunger/eating paths, raw/frozen/phantom effects, Barkskin/WellFed and seasonal recharge/XP/heal/shield paths. Display nourishment relative to a normal ration and actual talent-dependent meal time; explain food-specific risks and benefits. Remove generic programmer disclaimers and keep state/knowledge/RNG untouched. Plain Chargrilled Meat remains concise. All12cooked fruit mappings and10calendar Pasty forms checked; fruit delegated potion wording remains a known unresolved case.

Full local core suite2957/452 passed with zero failures/errors/skips. Earlier targeted failures were stale prose expectations; actual eating, buffs, knowledge, randomness, nutrition, recipes, calendar and save/load assertions remain. No Android verification or APK publication for these new audit changes. APD376 remains the independently verified delivered correction pair.

Next work: ring prose and combined-stat knowledge leakage found during actual inspection; inspect and test all ring variants and call sites. Then remaining item categories, providers and secondary panels. No new UI/mechanics/panel authorized. Preserve progress and continue.

Ring checkpoint:13ring source units individually reviewed;11complete and Force/Tenacity pending further complex-detail prose work. All12ring types are covered by the new test; the original reproduction failed first on Accuracy’s combined-stat leakage with an unknown companion; Force also leaked unknown Might via Strength-dependent descriptions/upgrade previews; Arcana upgrade preview leaked an unknown curse. Three added tests failed against the original source (/workspace/apd-ring-privacy-reproduction.log), then passed after UI-only knowledge guards. Brief ring benefit labels and a magic-immunity notice added; two neutral localization keys reviewed, inventory5298. Full2961/452 passed zero failures/errors/skips. No game formula changes, no new UI/panel and no APK for audit source. Continue bags, keys and all remaining categories.

Container/key checkpoint:5bags,5key source units and KeyExplanation individually checked and corrected. Backpack had no dedicated description; current capacity now uses actual capacity() (including Backpack’s dynamic accounting), no new control. Holster storage includes bombs and its durability bonus comes from existing constant; no false fixed wand recharge percentage. Protection verified against Frost/Burning’s top-level selection. Keys keep exact lock type and now display recorded floor without floor0 fabrication. Full2962/452 passed zero failures/errors/skips; three added neutral message keys reviewed, total5301. Only descriptions/inspection values changed. Continue utility items; stale references to removed calculation panels found in Waterskin, Ankh and Liquid Metal descriptions and must be removed after checking their actual effects.

Local continuation preserved for corrective377: ordinary armor tutorial/duplicate protection text removed as explicitly requested; normal armor stats and real too-heavy warning retained. Utility, remains, quest and journal source paths inspected; live Torch/Ankh/Waterskin/remains values and invalid-page handling tested. Two display-only numerical errors reproduced and corrected: Pickaxe rounded an augmented sum instead of its separately augmented parts; Force stance upgrade preview omitted the built-in +1 bonus. No actual damage formula changed. Catalog runtime export supports individual review but is not itself completion. Food baseline avoids comparing a normal ration to itself. Full2972/453 includes three new freeze regressions; no Android result claimed until corrective pipeline completes. Inventory review statuses/counts still require reconciliation; do not present exports or stale counts as comprehensive completion.


### Current continuation: delivered379/380; remains and journal review

379 and380 are published with both original signed APK variants, corresponding source and verification receipts.380:3030 core tests/457 suites and12 targeted native cases; five Summon Elemental variant info/use windows verified. Neither release claims the complete audit is finished.

Individually re-read all7 remains and5 journal source units, their inherited item descriptions, actual use/pickup effects and saved page fields. Reconciled their previously pending tracker entries; this is not a claim that all remaining providers are complete. Torn Page now previews the HP it would actually restore at current health/custom healing, without consuming fractional healing credit. The real healing path is unchanged. Five lore descriptions now state what the object is and how to read it, without redundant distance explanations. Existing title/save/inspection tests retained; current healing preview is compared against the actual use effect across all legal healing settings and fractional-credit/health states. Native381 adds real info/use captures for normal and zero healing, plus a lore page.

Tracker:235 source units reviewed fully or partially;221 corrected;107 complete;320 still unresolved. Source units are not distinct item types. The source archive exports and string inventories are supporting evidence, not individual review completion. Audio report updated with the owner's SPD-only recollection and bounded real-endpoint emulator measurements; no production audio change or verified phone fix.


### Utility, quest and thrown-weapon continuation after381 source

Read and reconciled ten utility units and nine quest-item units against actual use/pickup/recipe/teleport code; two already clear utility descriptions were retained unchanged. Escape Crystal's normal description is checked, but every score-specific dialogue and legacy error branch is still pending. Shortened resin, stylus, subclass mask, crown and waterskin descriptions; resin costs/limit use the existing live application helpers. Tenacity now says less/more damage rather than negative reduction; its existing optional detail panel remains pending owner review.

Reproduced two ordinary-window privacy failures: Tomahawk bleeding changed from8–15 to12–24 when an unidentified Sharpshooting ring's hidden level changed; ordinary Sword accuracy/strength text exposed unknown Might changes. Corrected description-only value selection in Weapon, MeleeWeapon, Armor, MissileWeapon, SpiritBow and Tomahawk. No combat formula, Strength, accuracy, durability or identification behavior changed. With unknown rings, show labelled weapon-only damage, withhold ring-dependent wear estimates and omit strength-derived warnings/bonuses. When identified, the existing live stats remain available. Core regression covers every catalog thrown-weapon class plus all13 dart classes, ordinary armor, Sword and Spirit Bow against both unknown ring types. Added actual Bolas slowdown and Shuriken free/normal throw availability checks. Source-specific normal descriptions checked for12 thrown weapons; pickup and surprise text already clear was retained. Native382 adds corresponding actual item windows; no Android result claimed before it finishes.

Current counts:268 source units reviewed fully or partially,249 corrected,138 complete,289 unresolved. These are source units, not item types. Primary text completion does not imply all inherited explanation providers or pending-review panels are complete.


Further ability checks reproduced three +0 previews ignoring known augmentation (Sickle, War Scythe, Whip), and Crossbow claiming +3 dart damage where Dart actually adds +4, distinct from +3 extra tip uses. Corrected those description values and concise wording only. The existing upgrade row is labelled Extra Tip Uses to match its unchanged value. No gameplay formulas changed. All3035 core tests/457 suites passed with zero failures/errors/skips; reviewed English inventory5325. Evidence: `/workspace/apd-additional-melee-evidence-20261108/`. Native382 adds known/unknown Duelist item windows; it has not run yet. Current tracker272 reviewed fully/partially,253 corrected,141 complete,286 unresolved. Specific retained panels remain pending review.


Artifact continuation: ordinary Hourglass, Spellbook, Armband and all four Sandals growth-stage descriptions now explain actual uses and drawbacks concisely. Source checks include stasis/freeze charges, blocked scroll exceptions and talent suppression, instant one-charge enemy theft and failed-shop risk, and tall-grass-only charge/drop behavior. Removed duplicate technical cursed summaries only for these four individually reviewed types when their own worn text already explains the curse. Their existing optional mechanic-specific panels remain unchanged pending owner review. All3035 core tests/457 suites passed again, zero failures/errors/skips. Native382 normal/cursed artifact windows are prepared but have not run. Tracker276 reviewed fully/partially,257 corrected,141 complete,286 unresolved; this remains an unfinished comprehensive audit.


Eight-trinket continuation: individually checked ordinary descriptions for Petrified Seed, Mossy Clump, Exotic Crystals, Ferret Tuft, Eye of Newt, Cracked Spyglass, Vial of Blood and Salt Cube. Petrified Seed’s extra chance applies to seeds/runestones, not dew or all items. Mossy Clump states Floor1/boss/side-area exclusions. Vision/evasion/hidden-item effects and hunger/healing trade-off use clear prose. Vial's per-turn displayed maximum includes run Healing and possible fractional carry; actual Healing.act comparisons cover all legal settings and multiple max-HP/level states. No-character catalog text does not invent20maximum HP. No healing calculation changed. All3036 core tests/457 suites passed, zero failures/errors/skips; English inventory5326. Evidence `/workspace/apd-eight-trinkets-evidence-20261108/`. Native383 item windows prepared, not yet verified. Eight source units remain partial because their full secondary/variant audit is not claimed finished. Existing panel groups explained in `APD-EXISTING-PANELS-PENDING-OWNER-REVIEW.md`; no new panels.

APD382 original acceptance failed because the synthetic Escape Crystal had null storedItems when the inspection helper attempted serialization, before opening UI. Initialized the fixture from copies of current gear/quickslots/gold/energy, without clearing inventory or modifying production code. Full Qol retry on unchanged original APKs: run42535769537, producing source d6d21c97d, verification-only source af859a2f3. Original10 other native cases retained and initial failures preserved. Do not publish before successful complete acceptance and visual review.


Follow-up after383 source: shortened weapon/armor strength warnings and excess-Strength bonus wording, preserving actual consequences and qualifying normal weapon attacks. Removed duplicate generic range advice only when the existing live range/adjacent accuracy line already supplies it; unknown-stat advice remains. No empty extra paragraph. All3036core/457suites passed again, no failures/errors/skips. Evidence `/workspace/apd-concise-strength-ranged-evidence-20261108/`. This is source-only progress after383 and has no new Android/APK claim yet. Do not confuse it with the unchanged382/383 acceptance retries.


382/383 delivered November8 at23:59:34/37UTC, both original signed variants and source/hashes/native visual evidence. Accepted runs42536990071/78, stages42537080287/304. Core3035/3036 respectively,457suites each; twelve targeted native cases per pair. All assertions retained. Actual captured review exposed an existing mismatch in unidentified base weapon damage under known augmentation, disclosed in release notes. Subsequent source corrects main melee/thrown +0 damage examples and labels armor/weapon baseline examples explicitly. Eighteen melee examples tested across all known augmentations and hidden0/3/8 levels; all3036core/457suites pass. This newer source has not yet been Android-verified or included in an APK. The complete audit was not finished by7pmEST; development continues under the owner's standing instruction.


### 2026-11-09 continuation: artifact curses and ordinary dart tips

Artifact curse paths checked individually for all15 types. Specific normal text replaces generic/technical duplication; legacy Beacon teleport and Cape deflection are explicitly preserved.3037core tests/457suites pass;384 native pipeline pending. Twelve tipped-dart base descriptions checked against actual proc/status code and rewritten for practical effects: ally safety, beneficial enemy blessing/healing, cleansing exclusions, stronger chill in water, valid teleport destinations and five fixed Rot-tip uses. No new panel, formula or mechanic. Existing inactive legacy helper is not mistaken for an ordinary UI. New actual inspection-window coverage for thirteen dart classes is prepared for the next pair; not yet run on Android. These dart units remain partial because full Charged Shot/class/inherited review is not complete. Tracker now288fully/partially reviewed,280corrected,141complete,286unresolved.


### Sneak and Spin correction

Dagger, Dirk, Assassin's Blade and Flail source-specific ability descriptions shortened after individual code review. Unknown previews explicitly labelled+0. Sneak states visible empty destination/root restriction; Spin states per-spin damage/3spin maximum/firstcharge only. Flail buff now displays actual spins outof3 rather than a percentage misleadingly labelled current spins; old translated percentage argument preserved, new English uses additional count argument. Read-only test verifies1/2/3counts without changing saved progress/charge; full3038core/457suites pass. Evidence `/workspace/apd-sneak-spin-evidence-20261109`. Android verification of these later changes remains pending;384 does not include them.292units fully/partially reviewed,284corrected,141complete,286unresolved.


### Additional Duelist prose and pre-quest Rose warning

Gloves/Sai/Gauntlet, Quarterstaff, both shields, Runic Blade and Scimitar source-specific ability paths checked and ordinary prose shortened, keeping dynamic values and explicit unknown+0 labels. Combo use consumes its tracker; normal melee/thrown hit replenishment verified in Hero, guard-end behavior in Invisibility, accuracy/evasion/speed remain actual values. Rose prequest GameScene branch previously hid its curse warning while equipped; append existing curse text there, no passive/action change. Regression forces incomplete quest and GameScene without GL, restores all statics, verifies worn curse warning and inspection purity. Full3038core/457suites pass (no skips/errors/failures). Evidence `/workspace/apd-rose-prequest-evidence-20261109`. Tracker corrects reviewed artifact curse units to PARTIAL:311reviewed fully/partially,292corrected,141complete,286unresolved.384 final verifier rejected full Qol result despite native job exit0; no384publication or accepted native claim. Diagnostics pending.


### Staff imbue and four exotic potions

Read staff selection/newlevel and existing dynamic result/unknown-bound/wand-loss paths. Confirmation corrects +0staff exception without changing imbue behavior. Four exotic potion ordinary descriptions individually checked: CorrosiveGas damaging exposure/Corrosion, Divine tier bonus/availability, Mastery eligibility/2STRflag, StormCloud water/fire/trap removal. Concise one-paragraph descriptions retain restrictions. Full3038core/457suites pass; source/evidence `/workspace/apd-exotic-potion-staff-evidence-20261109`. Actual four-potion inspection windows added for next Android pair, not yet run;385 does not include these later edits.316source units reviewed fully/partially,297corrected,141complete,286unresolved.


### Warrior Shield normal text and unchanged passive sources

Normal seal/buff prose shortened, no new interface. Positive wait and positive saved reduction displayed separately; old signed cooldown argument retained for existing translations. Ready/waiting/credit/active states and read-only6state serialization covered;3039core/457suites pass. Old text-specific assertions updated to new wait/credit language, actual mechanism assertions preserved. Existing advanced buff panel remains pending review. Native6normal-state windows prepared for next pair, not yet run. Evidence `/workspace/apd-warrior-shield-evidence-20261109`. Six passive glyphs checked against actual callers/formulas; five basic armor classes/flavor and Ankh resurrection descriptions checked unchanged. These remain partial due inherited/secondary/class scope.329units reviewed fully/partially,298corrected,141complete,286unresolved.


### Rot Dart ring-row correction

Actual385 capture exposed Sharpshooting durability row on RotDart despite unconditional20wear/fiveuses. Reproduced with failing targeted test and preserved log; narrow description guard removes only inapplicable Rot durability row, retains damage row. Expanded test includes harmful cursed and neutral cursed-upgraded ring states (neutral legitimately has no damage row), wear20 at different preview levels, serialized purity. Full3040core/457suites pass. Evidence `/workspace/apd-rot-dart-ring-row-evidence-20261109`. Future Android fixture asserts real Sharpshooting damage is present while Rot durability row is absent; not yet run. Extra twelve Duelist info/use/unknown+0 windows and three normal Flail buff states also prepared; source-only until next accepted pair.


### Shard loot/readiness and remaining ordinary glyph sources

Shard flavor and two tracker tooltips shortened. Ordinary loot text now distinguishes unidentified worn items and one temporary wand/throwable contribution per type; thrown proc requires a hit. Ready item, manual Shard identification and selected Identify-scroll readiness clarified. Real Mob drop-bonus and tracker/readiness sources inspected; existing regression now checks live count/cap and one wand tracker despite repeated applications at0..3levels. Full3040core/457suites pass; evidence `/workspace/apd-shard-loot-wording-evidence-20261109`. Ninth actual trinket window added for next Android pair, not yet run. Seven remaining normal glyph proc sources/supporting paths inspected and concise provider found consistent; advanced/cross-class scope remains partial. Normalized partial status spelling without changing completion claims.336reviewed fully/partially,298corrected,141complete,286unresolved. Latest-tests field now reflects latest source instead of stale3030snapshot;385 remains last published/Androidaccepted.


### Concrete item review closure and later dart limitations

Nine concrete tipped-dart source/message units fully reviewed and corrected: entire effects/charged branches plus actual385 native windows manually read. Five basic armor constructor/flavor units fully reviewed unchanged. Shared Armor, Dart/TippedDart/Missile providers remain open; source-unit completion does not mean those systems are finished. Remaining three dart word changes add meaningful limits (paralysis breaks on damage, Chill extinguishes fire/slows all actions, teleport can bring a distant enemy closer). Ankh says up to two retained items, matching optional fewer-item confirmation.3040core/457suites pass; evidence `/workspace/apd-dart-limitations-evidence-20261109`. Latest four wording changes are not in386 and await future native capture. Healing rule inspected: player-only custom recovery does NOT disable other-creature Healing Dart healing; no inappropriate global warning added.336reviewed source units,299corrected,155complete,272unresolved.386 original3040core/12native/source accepted, expanded backpack-owned Qol coverage pending on immutable APKs;385 remains published fallback.


### Magic Missile, Corrosion and wand uncertainty

Source-specific class/effect/staff and supporting gas/ooze/charge behavior read. Magic Missile ordinary text removes redundant flavor and explains one weaker-wand zap after a creature hit. Charge tooltip shows stored public level and one use; real consumption regression includes Scroll Empower (source5+2=public7) and preserves marker/scroll uses. Corrosion gives base per-turn initial damage and ally/self risk; Battlemage ooze explained with water washing. Existing variable-power note shortened without changing low/high/privacy logic.3040core457suites pass, evidence `/workspace/apd-magic-corrosion-wording-evidence-20261109`; actual MM/charge/Corrosion/staff windows prepared for next pair, not yet Androidverified. Own DamageWand wrapper source completed unchanged; large Wand/derived/secondary scope stays partial.340source units fully/partially reviewed,302corrected,156complete,271unresolved. No new mechanic or panel.


### Honest unidentified wand examples and Disintegration

Unidentified numeric wand stats now labelled +0examples in existing description paragraph. No hidden level read; nonnumeric/charge-only Corruption/Regrowth, unknown-charge Fireblast and no-hero Transfusion excluded after reading each stats path. Regression across13types at hidden0/3/8 verifies same text, label applicability and serialized purity. One English key added (5331total). Disintegration prose checked against actual beam: wall/creature piercing, shared multi-target/wall bonus, finite upgrade-dependent range and ally risk; staff reach no longer equated with a Projecting enchantment. Full3041core/457suites pass. Actual13unknown-wand and Disintegration known/unknown/staff windows added for387, not yet run. Evidence `/workspace/apd-wand-zero-example-evidence-20261109`.341reviewed units,303corrected,156complete,271unresolved.


### 2026-11-09 01:55UTC checkpoint

Lightning and Regrowth continued: concise normal chaining/damage/ally rules backed by real onZap four-scenario regression; Fireblast/Regrowth charge-cost grammar, plant allowance, 3charge Lotus, staff Healing0 warning and Lotus Rot-dart exception corrected. Full3042core457suites pass, no failures/errors/skips. Evidence /workspace/apd-lightning-regrowth-evidence-20261109. Three remaining concrete dart units closed after original387 actual owned windows manually reviewed; shared providers stay open. 342reviewed source units, 304corrected, 159complete, 268unresolved. These later source changes not in387; new Android verification pending.


### Living Earth and Prismatic Light normal descriptions

Read entire two wand sources/supporting paths; normal text shortened and clarified. Living Earth states space restriction and guardian repair; RockArmor reserve/rounding and guardian return explained concisely. Prismatic states narrow secret/terrain reveal, dark-floor light, blind/bonus and staff slow without programmer language. Real staff reserve/guardian matrix and Prismatic actual target/property/controlled-blindness/staff tests pass; full3044core457suites zero failures/errors/skips. Evidence /workspace/apd-livingearth-prismatic-evidence-20261109.344reviewed source units,306corrected,159complete,268unresolved. Source-only after388; actual new Android windows pending. No formulas/mechanics/panels added or removed.


### Herbal and Time Bubble secondary explanations

Read own Earthroot/Sungrass/Swiftthistle buff mechanics and save paths. Short ordinary Sungrass store label removes false upper-cap implication; TimeBubble no redundant paragraph. Existing detailed herbal/rock/time sections use plain language while retaining actual formulas, final-heal/final-block quirks, ascension depth26 and custom healing limitations. Existing retained panels unchanged in scope. Actual Hero.spend intercept, fractional budget/save and herb/block mechanical assertions pass; full3044core457suites zero failures/errors/skips. Evidence /workspace/apd-herbal-buff-human-evidence-20261109. Actual later Android windows pending. Counts unchanged; shared Plant/complete variants remain open.


### Passive glyph Arcana comparisons

Concise normal Flow/Swiftness/Camouflage/AntiMagic text now gives live final effect plus a short without-Arcana comparison only when known and numerically different. Re-read real glyph proc/caller paths; existing actual Flow/Aura and AntiMagic roll bounds retained, new actual Camouflage duration/save/unknown-ring regression. Full3045core457suites pass. Initial ordinary-vs-legacy decimal-precision assertion failure preserved; fixed expected display precision, no source mechanic change. Evidence /workspace/apd-passive-glyph-comparison-evidence-20261109. Native four owned windows prepared; Android verification not yet claimed. Counts344reviewed306corrected159complete268unresolved, complete individual audit still unfinished.


### Eight artifact normal-description continuation

Cloak/Cape/Beacon/Toolkit/Tome/Talisman/Chains/Chalice ordinary text individually checked against actual sources and shortened. Correct cloak enemy-safety promise, Beacon Set/Return restrictions, Toolkit6energy upgrade/warmup, Chains cost per tile moved. Chalice Healing0 warning checked against30real regen ticks; Cape real activation/random deflection/adjacency tested over24seeds. Existing actual cloak/progress/save and all artifact charge tests retained. Initial Cape fixture assumed nonexistent cooldown save key; failure retained and fixture corrected to real activation, no gameplay change. Full3047core457suites zero failures/errors/skips. Evidence /workspace/apd-eight-artifacts-human-evidence-20261109 plus initial /workspace/apd-three-artifacts-human-evidence-20261109. Actual all15normal/cursed native windows plus newbuff/Healing0 fixtures prepared, not yet run. Counts344reviewed306corrected159complete268unresolved; no full-audit claim.


### Catalyst and transfer scrolls

Read Catalyst recipe/four saved choices/confirmation, abstract Trinket/upgrade recipe, LiquidMetal full repair/recycle and both fixed-known loot scrolls/supporting policy. Catalyst/four-choice/legacy random preview concise with actual6energy/+3cap. Forwarding normal prose preserves all routing/delayed delivery/exclusions/bomb nonconsumption. LiquidMetal/Gathering ordinary wording unchanged after actual code review. Abstract Trinket own source complete unchanged; concrete/system scope remains separate. Full3047core457suites zero failures/errors/skips; evidence /workspace/apd-catalyst-transfer-evidence-20261109. New actual Catalyst four-choice and owned item windows pending389. 349fully/partially reviewed, 308corrected, 160complete, 267unresolved source units. No inventory/export automation counted as review.


### Enchantment replacement, loot and disabled healing

Entire Lucky/Elastic/Blocking/Kinetic/Vorpal/Vampiric/Venomous/Unstable source paths read. Lucky includes gold, Vorpal replaces immediate damage, Vampiric warns at Healing0 even unknown upgrades, Blocking/Kinetic buff text concise. New actual proc→Char.damage HP/shield/immunity regression and seeded actual disabled-heal cases pass; existing Arcana/Lucky/HoldFast tests retained. Headless sprite scale fixture failure preserved; initialized rendering scale without weakening mechanic assertions. Full3049core457suites zero failures/errors/skips. Evidence /workspace/apd-post389-enchantment-evidence-20261109.357reviewed source units/313corrected/160complete/267unresolved; Elemental Strike/complete shared scope remains open.389 rejected Android-test compilation, no publication; corrected checked-exception wrappers and new actual windows pending390.


### Kinetic live storage comparison

Actual Kinetic/Unstable/Char.damage path checked: Arcana scales newly created storage, subtracting previously reused bonus. Normal text shows live new-storage percent and short without-Arcana comparison only when known; no new interface. New real proc→damage regression covers ring-2/0/3 and reused0/10 with actual overkill. Full3050core457suites zero failures/errors/skips; evidence /workspace/apd-kinetic-arcana-storage-evidence-20261109. New190.61% actual window fixture prepared, not yet Androidverified and not in390.357reviewed313corrected160complete267unresolved.


### Neutral effects, Crystal condition, Projecting contribution and manual Chalice cap

Seven remaining concrete enchantment sources plus Crystal/Projecting reviewed individually. Actual Shocking/Eldritch proc tests confirm neutral creatures affected while allies/primary spared. Crystal ordinary provider had hidden its visible wear warning; read-only shared condition restores six states without revealing exact wear. Projecting describes contribution rather than a false total under Brawler/Ascend; actual four-state reach/purity verified. Initial local name-shadow compile and unknown-Force fixture failures preserved, fixed without weakening assertions.390passed3049core/12native but manual +10Chalice Healing0 window falsely promised further upgrade, so held unpublished; cap fix checked0/3/10 versus real action and30regen ticks each. Full3054core457suites zero failures/errors/skips. Evidence /workspace/apd-enchantment-final-pass-evidence-20261109.366reviewed/318corrected/160complete/267unresolved source units. New all17actual enchantment windows prepared; native391pending.


### Intrinsic reach prose

Spear/Glaive ordinary stats now state base2tiles; Whip base3tiles and concise flavor. These are their actual upgrade-independent constructor RCH values, distinguished from enchantment/spell bonuses. Known/unknown0/3/8 regression compares source RCH to info text, preserves all existing calculations. Full3055core457suites zero failures/errors/skips; evidence /workspace/apd-intrinsic-reach-evidence-20261109. Six native item windows prepared for next pair, not in391. Counts366reviewed318corrected160complete267unresolved unchanged.


### Post391 bow, dart and Amulet correction

Actual normal ElementalStrike.desc exposed hidden curse identity; narrow guard tested all ten hidden curse types, exact equality to ordinary generic output and known-disclosure restoration. New actual ability windows prepared. Reusable dart prompt no longer promises single-use tips when talents/bonuses preserve them; normal and extreme bonus wear tested. Amulet choices match actual END/actions before/during Ascension. Normal SpiritBow was sole ordinary caller of legacy Arcana itemSummary; replaced with existing concise playerDescription, preserving owner-retained specific panels and projectile geometry. Known four-enchantment checks, unknown ring -2/0/8 exact invariant text, saved state and real Smite tracker preserved. Full3060core/457suites pass; evidence under apd-spiritbow-description-evidence-20261109, dart-coating and amulet directories.374reviewed source units/321corrected/160complete/267unresolved. Native windows pending392; no game balance or new UI. Initial bow fixture missing fully qualified Weapon import and dart fixture insufficient+10bonus preserved, corrected fixtures without weakening assertions.


### Aqua and class armor normal cost

Read actual Brew/Elixir/Potion/ExoticPotion and complete ClassArmor source/six sprite subclasses. Aqua ordinary warning includes you/allies and existing two-tile push, no claim only fiery creatures can be hurt indirectly. Actual shatter test floods/removes hidden trap/clears fire/retains wall. ClassArmor cost shows percent of existing0–100meter. Short normal ability summary uses current cost; Warp keeps charge requirement despite free placement and Trinity avoids false single fixed cost. Existing details unchanged. Full3062core/457suites pass. Existing conditional/unknown/roundtrip tests retained. New native windows pending393, not392.387reviewed324corrected160complete267unresolved source units; abstract/shared dependency and full advanced effects not closed.


### Origin wording, actual Transmutation and window purity

Actual decorated-room and mine source subclasses read before human labels changed; key/source/classification/history/range rules unchanged. Range note shortened. Transmutation eligible-item selector/full replacement branches read, ordinary text no longer promises armor/glyph changes, actual known/unknown weapon property transfer regression passes. WndInfoItem title-color path called mutating Wand.level(); uses existing visiblyUpgraded read-only value instead. Existing core infusion previews preserve state, new actual MagicMissile/Regrowth/Warding windows pending. Manual392captures showed blank Weapon summary paragraphs and redundant no-wear Dart durability row; omit empty summary and zero-wear multiplier, finite Knife/tip bonuses retained and tested. Full3064core/457suites pass. Reproduction/fixture failures archived in docs/evidence/APD-392-followup-local-reproductions.zip; old wording-only assertion updated while all numeric/range checks preserved.393native will include portrait plus actual landscape preference/scroll/action checks, not yet verified. ArmorExplanation provider closed unchanged after full underlying formula review; ordinary generic armor math remains absent.


### Shared scroll warnings and retained detail clarity

Shared Scroll/InventoryScroll/ExoticScroll source reviewed. Cancellation warning explains prior identification consumes the scroll even if cancelled. Cursed-book warning correctly names RemoveCurse and AntiMagic exceptions. Wayward buff prose short, real accuracy/hit-clear rules unchanged. Existing pending-review panels retained; only Blocking timer, Shocking neutral chain targets, Eldritch witness exception and Vorpal direct-damage replacement descriptions corrected after actual source review. No new buttons, formula/effect changes. Full3064core457suites pass, evidence /workspace/apd-scroll-retained-detail-evidence-20261109. Actual warning/Wayward/expanded windows prepared for394, not393.398reviewed329corrected161complete266unresolved source units; full audit remains unfinished.


### Live Elemental Strike ordinary descriptions

Source normal ability quoted base4tile/65degree and fixed rank-sensitive effects. Concise existing-provider text now uses current ElementalReach/StrikingForce across27built-in effects, no actual hidden level/target inspection/roll. Base damage/shielding labeled; existing window supplies live charge cost once. No gameplay or new panel. Exact unknown curse output same for all10curse types and hidden-2/0/8, known effect names retained. Existing28effect matrix strengthened to actually reset0before12eachtype. Real perChar cone damage matches five talent ranks; real preattack shielding checked against ordinary value. Full3065core457suites pass. Initial stale known-wording assertion failure preserved in docs/evidence/APD-393-followup-strike-wording-reproduction.zip, equality/privacy tests retained. Actual twelve normal ability windows pending394, not393.399reviewed330corrected161complete266unresolved source units.


### Seed rule wording and unsupported dart target

Sungrass actual normal/Warden activation and six real healing ticks under HEALING0 preserve20HP; player plant/seed/herbal text warns, NPC still heals and no-hero preview stays generic. BarrenLand existing no-growth behavior tested and seed wrapper now warns. Actual Dart item selector offered Blandfruit seed despite no TippedDart.types mapping, leading to missing-result path. Reproduced acceptance, now selector filters supported mapping only; all existing recipes still create expected class/quantity. No recipe/gameplay balance/turn changes. Full3068core/457suites pass; English inventory5334 includes two new rule notices. Reproduction and initial checked-exception fixture error preserved in docs/evidence/APD-394-seed-selector-reproduction.zip. Android actual disabled-slot tap/valid tip prompt and four Sungrass/oneBarren seed windows pending394. Full audit remains unfinished.


###394 published / shared equipment and defensive weapon review

394sourceb68cd4a96:3068core457suites,12targeted API34cases, changed real windows manually reviewed. Later source3070core full pass; native windows pending395. Cursed removal explains actual temporary magic-immunity exception; Champion choice concise; five passive-block weapons show one accurate strength-adjusted range instead of duplicate unadjusted text. No gameplay or panel rule changed. Reconciled nine individually reviewed source units after accepted393/394 actual windows; 402full/partial reviews,332corrected,170complete,257unresolved of427source units. Full audit remains unfinished.


### Chalice maximum and shared source review

Chalice normal interval concise, compared with actual Regeneration levels0/3/9/10; retained max-level details correctly state no next prick. All3070core457suites passed; new actual native windows pending395. Reviewed six sprite/flavor-only class armor subclasses, Artifact base, ItemStatusHandler and Recipe base individually; no own description corrections required. Inherited ability/provider review remains separate. 408full/partial source reviews,332corrected,179complete,248unresolved. Three legacy detail providers now partial, never counted complete just because not exposed in normal UI.


### Actual stealth detection and trinket roll/generation audit

All3073core457suites passed. Actual Mob sleeping/wandering chance handlers match conditional Obfuscation explanation across distances, armor and Arcana. Clover text now describes independent weapon/many magic damage rolls, not complete attack extremes;400seeded actual rolls and next RNG match. RatSkull details clarify half only of bonus above baseline,400seeded actual Statue generations match; CrystalVault branch separately source-reviewed. Ferret duplicate effect removed; MimicTooth ebony chance restricted to ordinary floors in wording after RegularLevel/LastShop/boss creation read. New trinket actual windows pending396;395is earlier3070source. One new test-fixture compile error (nonexistent deactivate helper) preserved; corrected by actual RingBuff detach fixture without weakening tests. 409full/partial,333corrected,181complete,246unresolved source units. No gameplay changes or full audit claim.


### Passive glyph conditions and Horn meals

All3075core457suites passed. Added actual Swiftness path-condition test including wall/trap/chasm barriers; description unchanged across unseen enemy states. Four passive glyph own source reviews closed after prior accepted actual windows and live handler checks; shared provider separately partial. Horn source showed EAT truncates charge choice and can leave hunger; cursed Horn still eats stored food. Wording corrected without changing mechanics;16actual EAT cases verify resource/time and separate actual ordinary-food magic-immunity exception. Final Store hint states capacity/recharge benefit after exact upgrade rules read. New Horn native windows pending396.409full/partial reviews,333corrected,185complete,242unresolved. Two missing headless fixture dependencies preserved in failure ZIP, fixed fixtures only; no weakened assertions.


### Alert targeting and truthful ring upgrade descriptions

All3078core457suites passed. Aggression real Mob targeting regression confirms alert/visibility condition and no waking sleepers; normal text updated, Fear/DeepSleep targets source-reviewed. Reproduced Force4instead5with temporary EnhancedRings and an unknown companion; known own melee bonus now correct without exposing unarmed range or companion level. Tenacity existing upgrade preview names more/less damage, preserving formula and existing interface. New actual Force and upgrade/stone windows pending396. Ring panels stay as explicitly requested. English message inventory5336 after two neutral upgrade-value messages.395published3070core/12targeted Android cases; none of these later changes claimed in395. Full individual audit unfinished.


### Elixir stacking and fall protection source review

All3080core457suites passed. Actual Chasm landing with FeatherFall preserves HP and prevents fall bleed/cripple, consumes10extra protection turns. Normal duration50/fall cost10 now share unchanged constants. Actual ArcaneArmor replacement shows why stronger-in-strength does not always stay; normal elixir says no stacking. Might buff uses concise base HP and remaining level-up labels, with existing decay/save checks. Full three elixir own sources/recipes and shared target handlers read; new normal/buff native windows pending396. 410full/partial source reviews,335corrected,185complete,242unresolved; source units are not item counts.395remains published3070source, none of later source claimed delivered.


### Imbue durations and Vial Healing0

All3080core457suites pass after expanded existing actual proc/expiry/healing-matrix assertions. Fire/Icy item wording separates50turn effect duration from50percent ignition chance/3turn Chill per hit. Toxic normal text states5turn protection grace and lingering gas risk. Actual unchanged RNG bound2, Chill3 and grace5 use named constants; no random sequence, balance or save field changed. Normal buff prose concise, existing specific details retained. Vial Healing0 warns clearly while actual108healing cases still verify all rates/caps and unchanged credit. Three buff callers outside427item/provider source inventory also corrected; not counted as additional reviewed item types. 411full/partial reviews,336corrected,185complete242unresolved. English messages5337. New windows pending397, not the396candidate already building.


### Aquatic reserve and Honeyed cures

All3080core457suites pass. Aquatic buff now defines remaining reserve as HP before run adjustments and states full/dry/flying pauses. UnderHealing0, warns reserve is still spent while hurt and standing in water; actual2reserve debit and0health gain verified. Honeyed normal text includes common ailment cures, actual Weakness/Cripple and150hunger relief atHealing0 tested. Initial Poison headless visual dependency preserved; actual cure path remains tested with two other listed ailments. New normal/zero buff windows pending397. English inventory5338.396held for full12case retry after stale Ferret native flavor assertion; original APKs unchanged, failures and patch preserved.395published fallback.


### Exotic potion source and real talent gates

All3081core457suites pass. Divine grants exactly2once per tier, supports saving future-tier choices and waits for actual level/subclass/armor ability unlocks; new actual four-tier point and save checks verify existing concise description. EarthenArmor maximum Barkskin and MagicalSight sensing/no hidden-door discovery individually reconciled with handlers and existing actual tests. Three own-source reviews complete, new actual windows and Divine choice dialog pending397.411full/partial336corrected,188complete,239unresolved source units. New native helpers sent for source-only Android/desktop compilation before next APK; no Androidruntime claim for post396source.


### Cleansing, Stamina and Mastery source completion

All3084core457suites passed. Real cleansing hunger relief begins at300hunger, removes listed ailments and rejects new negative status while preserving LostInventory and ally relationships. Normal Cleansed text correctly says most effects. Stamina normal status shortened; actual1.5movement and unchanged attack/action timing tested. Mastery actual selector and Sword/Dart/Armor2STR reduction plus saved flag checked; no success/cancel action timing changed or claimed. Own three source reviews completed; new actual windows pending397.411full/partial,336corrected,191complete,236unresolved.5338English entries.396passed all12native cases; official scannerAPI recovered repeated504 without removing checks or rebuilding APKs. Final release staging/review still required;395published.


### Staff and plant description reconciliation

All3089core/457suites pass. Four concise Battlemage lines corrected: Corruption frenzy, Fireblast burning creatures/tiles, Transfusion charm ownership and one health exemption, Warding spent attacks versus sentry HP/no basic repair. Actual Transfusion checks no enemy-owned charm trigger/no charge/time waiver, six ward tiers use actual wandHeal. Blandfruit Bush seed now states raw fruit cannot be eaten, real attempt preserves food/time/item. Actual Blindweed/Stormvine/Mageroyal normal/Warden statuses/durations verified; their existing concise descriptions retained. Renderer/serialization test-fixture failures preserved in docs/evidence; no production effects changed.417full/partial source reviews,336corrected,191complete,236unresolved; six legacy/specific providers read but remain partial, no new panels.5338English entries. New actual windows pending397.396published originalpair3080core/12native; later source is not in396.


### Concise scroll status descriptions

All3092core457suites pass. Challenge known item shows original100turn arena duration; normal status shortened. Actual Hunger pause,100turn/exit expiration, save and real prearmor combat reduction verified. Dread normal status shorter; actual2xmovement, lost-sight AND6tile condition, half XP input/no ordinary death branch and fear expiry/recovery checked. Foresight normal status now uses live8tile distance, actualmapping/time tests retained. MagicImmune normal status concise with suppression drawback and no blanket-immunity promise; actual listed/unlisted immunity tests retained. Four own scroll source reviews completed,417full/partial336corrected195complete232unresolved; no new panels or gameplay/saveformat changes. Additional Buff caller outsidecatalog not counted as an item. New actual native item/status windows pending398, not397.397source20327251a build42570898637passed compilation/app assembly and3089core, both full native matrices running;396published fallback.


### Recharge descriptions match beneficial time

All3094core457suites pass. Artifact normal status no longer includes final empty update in remaining beneficial recharge; actor timing/icon/effect unchanged. Mystical normal item duration now uses existing30constant also passed to unchanged effect. Actual Horn/Cloak48cases cover1.5/30power, cursed/full/MagicImmune; read-only serialization preserved. Wand normal status says1extra charge every4turns via same unchanged0.25constant as actual charger, not a universal artifact rate. Actual partial tick/full/immunity deltas checked. Existing specific buff calculations use same shared value and remain pending owner review; no new panels. Two own scroll reviews complete,417full/partial336corrected197complete230unresolved,5338English entries. Additional Buff callers outsidecatalog not counted as items. New native windows pending398, not397.396published fallback;397native acceptance still running.


### Blast preview privacy and397 publication

All3096core457suites pass. Reproduced hidden-count leak in existing optional Psionic details; invisible Rat changed0/50to1/45, disguised Mimic could disclose chest identity. Preview now only uses apparent creatures and ALWAYS states conservative base self-damage upper bound, independent of hidden creature presence. Actual scroll doRead still hits invisible creatures/allies and uses original full-count self-cost/formulas; verified through real handlers. Both retained blast details use clear prose plus existing formulas/aftereffects, no new panels. Source provider partial, two own scroll reviews complete;418full/partial337corrected199complete228unresolved,5338English entries. New normal/expanded native windows pending398.

397published2026-11-09T06:23:50Z, source20327251aab6b3f05008dace5edd308942f142c2 build42570898637 stage42570936402.3089core/457 and12distinct API34cases passed; both original APK/source hashes plus supplemental scan independently checked, nine actual new captures manually reviewed. Receipt APD-397-DELIVERY-RECEIPT.json. The later scroll/status/recharge/privacy corrections are NOT397; known optional Psionic finding disclosed in release notes.396preserved. Next cadence target06:53:50UTC; start398 around06:39–06:40 with current source and full native acceptance. Continue source audit, no milestone stop.


### Four trinket secondary explanations

All3096core457suites pass. Existing Ferret/Clover/RatSkull/MimicTooth detail paragraphs shortened with formulas and meaningful limits kept. Current level label plain; shared redundant level-indexed footer removed. Unknown guards/RNG/save checks retained. Four own legitimate0–3source reviews complete after actual combat/roll/generation handlers and manually viewed397normal/expanded captures; shared remaining provider branches partial. Existing controls retained pending owner review, no new panels.418full/partial337corrected203complete224unresolved,5338English entries. New concise expanded windows pending398; queue next pair from current verified source around06:37–06:39 for06:53:50cadence while continuing audit.397published fallback.


### Sight, hunger and healing trinkets

All3098core457suites pass. Eye normal description names stealthy-Mimic sensing exclusion; actual walls/inside/outside range verified at0–3. Salt real hunger and regeneration rates checked including locked-floor exception; existing detail now states it too. Vial108real healing cases also verify clear zero-Healing detail and base cap, preserve fraction credit; no invented no-hero HP. Three existing detail paragraphs shortened and actual normal/expanded/zeroHealing window fixtures prepared for399, not398alreadybuilding.418full/partial337corrected206complete221unresolved,5338English entries.398sourceb7829a700bcfa396487a62f9985863d81208543f build42574111089, stage request committed;397published fallback. Continue audit while398native runs.


### Gradual healing nonstacking warning

All3099core457suites pass. Normal Potion of Healing warns repeated doses do not add their healing together; normal Healing status concise, original two base HP amounts retained with clear before-bonus/penalty label. Actual potion.heal after tick restores larger remaining pool, not sum; independent rate/flat strongest-property merge checked. No actual healing or calculation logic changed. Counts418full/partial337corrected206complete221unresolved,5338English. New actual item/status windows pending399; source-only Android/desktop/instrumentation compilation requested for latestsource.398build42574111089 passed3096core/full12API34, stage42574157508uploading. Need rootdownload hashes/source scan and manual new scroll/status/trinket expanded captures then publish398. Target06:53:50cadence;397fallbackpublished. Continue audit.


### Twin Upgrades inspection privacy

All3103core457suites pass. Reproduced known Sword and crossbow Dart tooltip changes from unidentified companion level0to8. Existing Dart thread-local inspection context now also guards weapon-derived Twin upgrade queries; normal range/ability/proc/block/ring/retained details omit unknown contribution and explain omission without consulting its hidden level. Outside inspection, combat still gets8; separate actor thread unaffected and exceptions restore scope. Source-only context, no gameplay/save/targeting/panel changes. Native new owned Sword/Dart/identified-companion windows prepared for399. Counts418full/partial337corrected206complete221unresolved,5338English unchanged. Later source-only compile42575862693passed3099core/debug/release/instrumentation/desktop ondd9eccc670, before Twin changes; not a runtime claim for current source.

398published2026-11-09T06:55:35Z; sourceb7829a700bcfa396487a62f9985863d81208543f build42574111089 stage42574157508.3096core457suites/full12API34cases passed; original pair/source hashes and supplemental scan independently checked, nine new actual captures manually reviewed. Receipt APD-398-DELIVERY-RECEIPT.json. Later Eye/Salt/Vial/healing/Twin source NOT398. Next399target about07:25:36UTC. Queue current3103source pair immediately, full native verification; continue audit after checkpoint.


### Sword-family source reconciliation

Read complete own five Sword-family sources and shared Cleave branch behavior. Existing concise paid/free-followup/zero-time-kill descriptions correct; actual18family deferred-bonus inputs and15family augmentation/rounding/known/unknown preview tests remain accepted. Five own review units completed; inherited/provider review separate. Extra headless accounting fixture cannot construct real AttackIndicator, exact candidate/log preserved in APD-400-cleave-headless-ui-fixture.zip and excluded; no application failure or successful kill-turn-cost runtime claim. Production source/tests remain accepted3103core; no extra mechanics or unnecessary wording added.419full/partial337corrected211complete216unresolved source units,5338English.399source58f6b67d3ced039c85a612923909efd7d9c2277f build42578107734 native in progress;398published fallback. Continue remaining weapon-family review.


### Retained growth and ward details

Warding/Regrowth existing panels shorter, no new controls. Capacity counts resin/curse infusion, excludes temporary casting levels; Regrowth whole-number exhaustion formula/saved counters preserved and +10 described as effectively disabled rather than2147483647charges. All3103core457suites passed. New expanded native windows pending400. Seven own HeavyBlow/Spike source reviews closed after individual source reconciliation; inherited/shared review remains separate.420full/partial338corrected218complete209unresolved,5338English. Whole audit unfinished.399published fallback; continue.


### Seven additional trinket details and armor curse reconciliation

Seven existing trinket panels individually shortened, with actual spawn timing, hidden-loot whole/fraction count, theme eligibility, grass seed/stone versus dew and distributed trap reveals. Censer can also make helpful gases; ordinary label corrected, unknown uses existing At+0 example. Real painter matrix verifies20normal/100trap-themed totals and2/4/6/8base reveals; no painter changes.3105core457suites pass; two unaccepted fixture mistakes preserved separately. Seven own trinket and eight own armor-curse reviews completed; provider/class reviews still separate. 420 reviewed_units, 338 corrected_units, 233 completed_units, 194 unresolved_units. New eight Android windows pending401;400in verification contains prior growth/ward changes only. No physical-phone/audio or whole-audit completion claim.


### Final trinket branches and Lunge limits

All17existing TrinketExplanation branches individually reconciled; three final details shortened. Exotic explicitly retains current Healing0 override, Resin separate neutral/helpful and extra-zap chances, Shard actual capped/additive loot contribution and50turn use trackers. Real Mob.lootChance matrix36cases verifies cap and hidden-upgrade invariance;3106core457pass. Shard unknown uses existing At+0 key. Lunge normal fourstrings names rooted/landing and empty-space-charge limits; ownRapier/Katana/Greataxe reviews closed, shared/class scope separate.420full/partial338corrected240complete187unresolved,5338English. New source-only compilation requested; current400native checks remain pending. Continue approved work; whole audit unfinished.


### Three retained artifact details

Cape/Chalice/Horn optional math prose and common charge header shorter, preserving effect/bound formulas. Entire own source reconciled, actual existing reflection/regen/food/time/experience/Healing0/save tests pass in3106core457suites. Obsolete Rose-header wording assertion failure retained; corrected assertion preserves actual50%ghost health versus25/100summoning state and save purity. Three new expanded Android windows plus end-of-scroll camera assertion/bottom screenshots added to all existing calculation-window fixtures; runtime pending401.420full/partial338corrected243complete184unresolved,5338English. Whole audit unfinished.


### APK401 rejection and continued Cleric correction

401build42582332906 failed instrumentation Java compilation before assembly: scroll-end lambda omitted checked-exception handling. Exact log/source preserved in APD-401-android-scroll-fixture-compile.zip, original401draft now explicitly rejected, noAPK produced or published. Fixed test helper catch in later source. Both real Cleric information paths now omit redundant CurrentCost paragraph, retaining original ChargeCost and existing details control.243descriptions across27spells/3subclasses/3valid-rank settings pass one-cost/RNG/save checks in3107fullcore457suites. Android real ordinaryHolyWeapon single-cost capture/assert added, pending402. Source-only42581691955passed3106/desktop/Androiddebug/release/testJava at2f4b15aff before later artifact/bottom changes; no runtime claim. Monk provider plus5actual abilities read; advanced Meditate resistance conditional phrasing still needs correction.422full/partial338corrected243complete184unresolved,5338English; continue approved work.


### Monk resource/ability clarification and actual Meditate test

3108core457suites pass, allfail/error/skipzero. Monk retained details shorter, exact damage/range/energy/cap/additive gear/float-boundary rules preserved; Meditate resistance explicitly empowered-only. Actual4state empowered/Healing0 execution verifies5turn/5energy,2HPbasepool ornone,resistanceconditional, delayedcallbackscheduledatremainingdelay−1andactual8turnwand/artifactrechargewithHorn. New actual Monk energy expanded/bottom capture prepared, not yet Android-verified. Failed fixture API and three obsolete wording assertions preserved; accepted real-effect tests unchanged. Three own dagger-source reviews closed without unnecessary prose changes. Added previously missed WndClericSpells UI source to inventory:428sourceunits,423full/partial340corrected248complete180unresolved,5338English. Full audit remains unfinished.402originalAPKs retry42584030587 uses test-only Rose wording patch, no app rebuild; native pending. Continue approved work.


### Live normal Elemental Blast descriptions

Existing English GameScene ability window replaces stale4tile/15–25/fixedstatus promises with current publicstafftype/talents/challenge values. All13actual activation branches and effect consumers reconciled. Corrects guaranteed outward displacement and basic-ward-repair wording, keeps fire/electricity ally hazard warning. No hidden levels/targets inspected, no effect tables/casts/damage/charge/save changes, no new panel. Existing ability window supplies current charge cost so redundant35base-cost line omitted only in this live English branch. Other languages/out-of-game fallback preserved. New message key increases English inventory5338to5339, original inventory-failure evidence retained.3109fullcore457pass,65actualGameScene description states plus existing ward repairs. Eight actual native windows pending403. Added omitted direct ability source:429units,426full/partial342corrected250complete179unresolved; full audit incomplete. Heap info dispatcher partially reviewed, not full opening/haunting/save logic.402Developer missing scheduler recovery requested after emulator disconnect; no publication claimed.


### Concise Holy Weapon and Holy Ward

3110core457suites pass; full raw XML/log and initial String/float display-helper compile failure preserved. Existing normal English spell and active-buff descriptions now show current known magic damage/protection, duration, instant cast and original charge cost once. Paladin retention/extension, unknown-ring safeguards, unarmored attacker dependence and MagicImmune weapon/armor distinctions preserved. Actual combat proc matrix verifies numbers; existing advanced panels untouched. Eight actual normal spell/buff Android windows prepared, pending404. Added two omitted directly edited sources;431units,428full/partial344corrected256complete175unresolved;5339English keys. Four complete own melee reviews closed with existing concise prose unchanged; shared/class providers still separate. Whole audit incomplete; continue approved work.


### Fifteen further own-source reviews

All10weapon curses plus Gloves/Sai/Gauntlet/RoundShield/Greatshield individually reconciled with proc/status/Geyser/Guard/ComboStrike consumers and save branches. Existing normal prose remains concise and accurate, so no gratuitous wording or gameplay edits. Own source reviews closed; shared effects/class/generation/ElementalStrike/CursedWand providers remain separate, not implicitly completed.431sourceunits,428full/partial344corrected271complete160unresolved;5339English entries.3110acceptedcore457raw retained. These15review closures are later than404producing source; no app changes. Whole audit unfinished.


### Retained Force/Tenacity detail correction and twelve more own-source reviews

Existing retained panels shorter; Force Strength/endpoints/Brawler formulas and actualknownbonus preserved, cursedarmedloss phrased naturally,50turn recharge downside explicit. Tenacity currentless/moredamage and nearzeroHP maximum distinguished. No newpanel or gameplay/save edits.3112core457pass,18health/curse/upgrade multiplier states and24seeded actual stance differences, existing hiddenStrength/unknownring/MagicImmune/state tests retained. Eight new expanded/bottom Android captures prepared, pending405. Full raw evidence committed. Eight glyphs/ClassArmor/Crossbow/Force/Tenacity own-source reviews closed with consumers/save branches read, shared/class providers still separate.431units428full/partial345corrected283complete148unresolved,5339English entries. Whole audit unfinished; continue approved work.


### Five ordinary defensive/status buff descriptions

Barkskin/ArcaneArmor ordinary text shorter with live range and next weakening; no repeated nonpercentage/permanent-upgrade disclaimers. Bless/Hex/Daze ordinary text shorter and displayed percentages derive from actual ROLL_FACTOR rather than literal25/20/50. Real8mask×100seed×2direction hit-contest matrix now checks normal displayed percentages alongside existing exact outcomes. Actual decay/save/read-only test at1/5intervals added.3113core457pass, rawfull retained. Existing detailed panels/otherlanguages/gameplay/save formulas unchanged. Five previously omitted buff sources added:436units433full/partial350corrected288complete148unresolved;5339English keys. Five ordinary nativewindows pending406.404failed prematurecamera assertion preserved;405retry changes only timing, identical assertion on originalAPKs.403published fallback. Continue approved work.


### Dart and coating own-source closure

Full Dart/TippedDart own paths individually reconciled: crossbow cache/knowledge/thread isolation, charged damage versus tipuses/AOE, coating2perseed/1turn, wear and source/target Lotus/DurableTips/100usecap, cleaning/impactconversion/merge waste/seedmap. Existing concise prose correct; no additional gameplay or wording changes. Individual tips, MissileWeapon and shared effects remain separately tracked.436units433full/partial350corrected290complete146unresolved;5339English keys. Later than406producing source; review closures only.


### Vampiric custom-healing description regression and seventeen own enchantment reviews

Full17own source/trackers/condition/save paths individually read with actualChar/Mob/resource/effect consumers. Shared/class/legacy providers remain separate. Found Vampiric ordinary healpercent ignored nonzero customhealing setting; real25%rule regression showed50%. Corrected read-only nominalpercentage with about/HPcap wording for existing round/fractionalcredit mechanics.36actualactivations across25/50/100/200healing,Arcana0/3/12,damage5/12/20 pass and inspection preservescredit/state.3114core457allpass; initialfailure and acceptedfullXML/log preserved. Two actual25/200nativewindows prepared, NOT run; newestchange notin406. Source-only debug/release/testJava/desktop verification requested, no newAPK.436units433full/partial350corrected307complete129unresolved;5339Englishkeys. Wholeauditstillunfinished.


### Manual review rejected405/406; zero-value Tenacity corrected in source

405originals passed3112core457/12actualAPI34aftertest-onlytimingretry. IndependentAPK/source/sidecar/scan and4453sourcefilehashchecks passed;32newHoly/ringactualcaptures reviewed. Cursed+3Tenacityshowscorrect0%andringvalue0butgenerictextclaims cursedharmandnearzeroHPmaximum: incorrectforneutralzero. Rejectpublication.406containsidenticalaffectedprovider deb35654c26d40c2a8dd1f7db2a4dcd63dc3f75e4c87ad0edd65c63f2492d02d;alsoheldprivate despite3113core/12nativepasses. Bothoriginalpairs/evidence retained; no successfuldelivery claim. Latestsource differentiatespositive/negative/zeroandonlymentionsmaxnearzeroHPfornonzero. Existing18stateactualmultiplierteststrengthened;3115fullcore457allpass. ScreenshotrejectionandrawfullXML/logcommitted. CorrectedTenacityandnewVampiricarenotinAPKyet; source-onlycompile requested.403remainslatestpublished. Wholeaudit436units433fullpartial350corrected307complete129unresolved; continueuntil~09:30UTC.


### Resumed authorization and Talisman detail review

OwnerextendsactiveAPDworkthrough2026-11-09 16:00EST/21:00UTC, verified30minuteGitHubcadence/Easternmetadata, separatefailurebranches.404–406sourcebranches pushed;403fallback retained.407originalcorrectedpairinAndroidacceptance. Talismanfullownsource/read-onlyprovider reviewed; retaineddetailshorter, live range/cost/cone/progress formulas preserved, +10cap explicit, duplicate resource prose removed.27actualempty-floorcastsverifyrange/wholefractiondebit/1turn/selfcancel;8actualwarningstatesverifyvisibility/distance/searchability/type/curse/immunity.3117fullcore457pass, rawXML/logcommitted. New+3/+10expanded/bottomnativefixtureprepared, NOTyetcompiled/run.436units433fullpartial350corrected309complete127unresolved; no newpanel/gameplay/savechange. Continueapprovedwork.


### Cloak/Rose retained explanations and four exotic-potion own reviews

Cloak natural recharge versus external charge distinguished; retainedpayment/countdown/progress/targetlevel/cap formulas shorter, no state changes. Rose retainedtext shorter with exactknownstats/gear limits/8HPoldcap/nextrefresh and important1damagebeforeeachghostaction whenunequipped orMagicImmune. FourstateactualNoRoseDamageguardprobe verifies offeredamount/source; existing24cloakprogress/33ghoststat/rawbounds/dispel/recharge/status/privategear/savechecksretained.3118fullcore457pass, rawXML/logcommitted. ExistingCloak/Roseexpanded/bottomcaptureswillverifynewproseinthenextAPK; no deviceclaimyet. Four exoticpotion own classes read with actualshield/freeze/fog/breath consumers, currentconciseproseaccurate andunchanged.436units433fullpartial350corrected317complete119unresolved.407passed3115/12Android, stagingpendingmanualreview; continueapprovedwork.

### APK407 published 2026-11-09T05:17:43-05:00
Original source 11e73f663f8bccf1e2fd8b2fbb7c4d935bf31c6a; 3115core/457suites and12distinctAPI34cases passed. Independent files/4479manifest hashes checked;20actual Tenacity/Vampiric captures reviewed. Zero-state prose corrected,25/200healing displays match live rules.403/prior releases retained;404–406 remain rejected private. Latest later source ec95db133 separately passes3118core and source compile42599197355; Cloak/Rose/Talisman actual next-APK windows not yet verified. Audit317complete/119unresolved of436units,433partial/full reviewed,350corrected,5339Englishentries. Continue through21UTC owner window.

### Eight elixir own-source reviews completed
ArcaneArmor, DragonsBlood, IcyTouch, FeatherFall, AquaticRejuvenation, HoneyedHealing, Might and ToxicEssence individually checked with recipes/nestedbuffs/actual consumers. Existing concise live prose accurate; no gratuitous rewrites or mechanics changes. Prior effect/hidden-state/custom-rule/save tests and actual native windows retained. Own-source completion only; separately tracked inherited/shared providers remain open. Audit436units433partial/full350corrected325complete111unresolved,5339Englishentries.

### Brew and exotic base/source reviews
Seven Brew classes, Elixir/ExoticPotion bases, CorrosiveGas/StormClouds potions reviewed individually with actual cloud/status/terrain/recipe consumers. Current concise ordinary prose retained, no new panels, no gameplay changes. Existing Aqua actual flood/trap/fire and other relevant regression evidence preserved. Own-source completion only; shared Potion/provider reviews remain open. Audit436units433partial/full350corrected336complete100unresolved,5339Englishentries.408originalpairbuilding at77950dd132349381a70928bcc7c9180910ff9ea3.

### Armband retained panel simplified and actual shop outcomes verified
Fullownsource plus tradeconsumer read. Existing panel now distinguishes enemy/shoptheft, liveblind/cripple turns, basevaluechargebudget, success-onlycharge and failedshopflight. No new panel/effects. Existing9previewbudget cases plus288seededactualshopoutcomes, inspectionpurity/no-time/cap checks;3119core457pass, rawfullXML/log preserved. Nativeexpanded/bottomfixtureprepared fornextAPKafter408, notyetdeviceverified. Audit337complete99unresolved of436units;433full/partial350corrected,5339Englishentries.

### Toolkit prompt/detail and Beacon own-source review
Toolkit Energize prompt correctly names experience-generatedenergy; existing detail shortened with liveenergy/rate/warmup/max. Nine actualwarmup runs include initialsetup/partialprogress/+10 andsave, separatecurse/immunitypause;3121core457allpass, rawXML/log preserved. Two expanded/bottomnativefixturespendingnextAPK. Beacon fullownsource reviewed, conciseproseunchanged, legacy exceptions preserved. Audit339complete97unresolvedof436,433full/partial350corrected,5339Englishentries. No gameplay/save/newpanel changes.

### Hourglass retained detail and Chains own-source review
Hourglass existingpanel gives5turnstasis/2chargecap and2turnactionbudget/partialcarry/deferredhazards, removing implementationroundingjargon. Ten actualbudgetsequences includeprecisionboundary andsavecontinuation;3122core457allpass, rawXML/log preserved. Nativeexpanded/bottompending. Chainsfullownsource/currentprose reviewed, no rewrite needed. Audit341complete95unresolvedof436units,433partial/full350corrected,5339Englishentries. Sharedprovidersremainopen.

### Spellbook and Sandals description followup
Existing Spellbook detail shorter, correctrandom1charge/extra1exoticchoice and2turnscrollfeed. Sandals ordinaryability namesvisibletile/3tilerange andlivefed/requiredcounts, hidesnextgrowthclaimatcap.18actualfeedsverify3/6/9thresholds,consumption/1turn/finalform;3123core457allpass, rawfullXML/log preserved. Newactualwindows pending. Audit342complete94unresolvedof436units; Sandalsremainspartialpendingunsupportedseedinvestigation, no gameplayfixclaimyet.

### Reproduced and guarded unsupported Sandals seed crash
Blandfruit lacks existingcost/color/ability entries butwasoffered; two redregressions preserve selectionfailure andlegacyactionsNPE. Existingmapsnowguardselector/directfeed/actions/description/glow/cellcallback. Historicalselection/listpreserved byte-for-byte throughinspection; validreplacementallowed; all12validseeds and18actualfeedsretained. No validabilitycost/effect/charge/time changed. Initialfixtureaccesscompileerror andinvalidintermediateoverlappingfullruns preserved; misseddescguardfixed. Freshserial--rerun-tasks3125core457passed, zeroerrors/failures/skips; rawfullXML/logretained. NativelegacysavewindowpendingnextAPK, noAndroidclaimyet. HolyTomefullownsource/currentprose reviewedunchanged; individualspells/sharedprovidersremainopen. Audit344complete92unresolvedof436units,433partial/full350corrected,5339Englishentries.408originalpairpassed3118/12native, staging/manualreviewpending.

### APK408 published 2026-11-09T05:39:34-05:00
Original77950dd132349381a70928bcc7c9180910ff9ea3;3118core457 and12distinctAPI34casespassed. Independentfiles/4488manifesthasheschecked;22actualCloak/Rose/Talisman normal/expanded/bottomcapturesreviewed. Signedoriginalpair/source/evidenceattached, previousreleasespreserved. Laterartifact/Sandalschangesnotincluded; latestsource3125core457pass, actualnextAndroidwindowsstillpending. Audit344complete92unresolvedof436. Continueownerwindowthrough21UTC.

### Skeleton Key and revival/victory item own reviews
Key purpose/lockcosts/limitations/wallcost-duration prose concise; actualIron1/freeown-door/cardinalwall2charges/3cells/1turn/10turnexpiry verified;Golden/Crystal source paths read butnotheadlessactivated. FullAnkh/Amulet ownclasses andrelevantrevival/backpack/choice/victory/Ascension consumers read, currentproseaccurateunchanged.3126core457allpass, rawXML/logpreserved. Audit347complete89unresolvedof436units,433partial/full350corrected5339Englishentries.409originalpairwillinclude laterartifactandSandalscorrections; actualnewAndroidwindowsnotyetverified.408publishedfallbackpreserved; continueownerwindowthrough21UTC.

### Lullaby Healing0 and seven scroll own reviews
Known Lullaby nowstates currentrulespreventrestorativesleep; actualDrowsy heroendswithouthealing, allystillheals1HP. Hidden/nohero/inspectionpure; no mechanicschange. Initialred and strictEnglishinventorycount failure preserved; newneutralkey explicitlycounted5340, allAdultOFF/ON/formatchecks retained.3127core457allpass, rawfullXML/logpreserved. InventoryScroll,Identify,MagicMapping,MirrorImage,Lullaby,Rage,Terror ownsource/consumers individuallyreviewed; existingotherconciseproseretained. Audit354complete82unresolved,436units433partial/full350corrected; topcompleted_units andlateststatussynchronized (older317fieldhadlaggedappendix). NativeLullabywindowsreadyforlaterbuild, not409.

### Remove Curse existing behavior clarified
Fullownsource andTormentedSpiritconsumer reviewed. Ordinary text namesDegradecure andadjacent-spiritpriorityinstead ofimplyinggear-selectionalways.3127core457allpass, rawXML/logpreserved; knowninspectionwindowpending410. No mechanics/interface/newpanel change. Audit355complete81unresolvedof436,433partial/full350corrected,5340Englishkeys.

### Teleport/Passage and shared scroll base reviews
Regularteleporttext no longerclaims genericpathblocking throughlockeddoors/barricades; actual special/vaultlockedroomexclusion preserved. Passageknowntextshowsactualregionreturnfloor, exactoriginalformula sharedwithoutchange; explicit26floorboundarytable/realunknownidentity/nohero/inspectionpure verified. InitialunmappedanonymousExoticfixturefailurepreserved/correctedwithrealidentitytable. FullScroll/ExoticScrollbases/recipesread.3128core457allpass, rawXML/logretained. Audit359complete77unresolvedof436,433partial/full350corrected,5341Englishkeys2430itemkeys. Newactualwindowsnotin409.409failednativebothvariantsbecauseSandalsfixturesusedordinaryno-panelmode; exact2callpatchrequiresoneowner-retainedpanelwithallotherassertions unchanged. Original9fdsourceonfailurebranch; retryinstrumentation-only onexactoriginalAPKs, initialevidence/preservationrequested.408latestpublished; no409successclaim.

### Five exotic scrolls and shared artifact providers
FullDivination/Enchantment/Metamorphosis/PrismaticImage/SirensSong ownclasses/consumers read; concisecurrentprose retainedexceptreplacementclarity/charmafterimmunitymaynotguarantee. ArtifactExplanation/Economyfullprovider reviews include allownartifacts/RingEnergy/unknownring/classrates. Secondarypanelskeep owner-retainedavailability/formulas, removeirrelevantCloak/Tomeboilerplatefromotherartifacts andduplicatelevelfooterfor10stageitems; othercapsretaingrowth/displayalias. Cursepanelsreusepreciseexistingprose. Cloak/Tomeordinarycurse saysnormally: actualSupplyRation/EnlighteningMeal chargeundercurse/MagicImmune tested while ordinaryrechargeblocked. ActualChains15->21->25XP and29->34externaloverflow/refusal verifydistinctlimits.3130core457allpass, rawXML/logretained; newUIcapturespending410. Audit366complete70unresolvedof436,433partial/full350corrected,5341English2430itemkeys.409test-onlyoriginalAPKretryrunning; preservationrun42604779871success.408latestpublished.

November9 07:59EST:442 units fully/partially reviewed,434 own-sourcecomplete,356 corrected,8 open. Seven status explanations shortened;3136core457all0. Android414 pending; audit unfinished.


2026-11-09 2026-11-09T17:21:02.751643+00:00: full ElementalStrike normal/existing detail branches reviewed against actual681-line handler plus Bleeding/Crystal.438own-sourcecomplete,7open,445units,359corrected;5353Englishentries/2432itemkeys.3151core457suites0failures/errors/skips; Android3151sourcecompile requested.423 pair source2a98ed3e running and staging remains private pending manual acceptance.418latestpublished;420/421/422hold. No claim of complete whole audit, phone/acoustic verification or gameplay change.


2026-11-09T18:34:45.455487+00:00: completed individual own-source review of all27current SpellExplanation branches and resources/class/talent mappings.3155core457all0;32realspellcontexts prepared (Priest,Paladin,MagicImmunity) withstate restoration.439own-sourcecomplete,6open,445units,359corrected;5353Englishentries/2432itemkeys. No gameplay/balance/save/UI availability changes. Native/manual stillpending;425usesolder14branch/font-correctedsource andis stagedprivate. No complete-project claim.

November9 2026-11-09T19:08:00.182426+00:00: MechanicsDetails and BuffExplanation own-source review closed against actual handlers and tests.441/445 own-source units complete,359 corrected,4 unresolved. Existing mechanic panels retained pending owner review.3157core457allzero; newer428Androidsource/native and manual verification pending.426original native failure/fixture-only retry and427pre-assembly metadata failure preserved.425remains latestpublished at this checkpoint.
