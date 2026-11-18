# Cursed weapon electricity and disguised mimics: whynot36666 investigation
Date: 2026-10-20 (America/New_York). Status: source investigation and deterministic regression verification complete; no confirmed gameplay bug.

## Finding
No gameplay defect has been established. The source distinguishes an instantaneous cursed lightning burst from a persistent, water-conducting electricity field. A deterministic Wondrous weapon scenario reproduces an unharmed, still-disguised mimic in the same connected water pool when it stands outside both burst areas. That matches the report's general description without requiring failed damage or failed mimic activation.

The supplied screenshot is a conversation, not a dungeon map or damage trace. It does not establish the mimic's coordinates, whether its tile was water, which random effect occurred, the tester's build, or any HP change. Accordingly, this investigation does not assert that the exact unrecorded incident has been reconstructed. If a normal disguised mimic actually received this damage call or an accepted negative effect and stayed disguised, that would contradict the tested behavior.

The earlier reply that zero damage does not wake a mimic was incorrect for these code paths: zero HP loss does not prevent reveal. A missed/out-of-range effect is different from calling the damage handler with zero.

## Source baselines
Audited APD branch: `codex/apd-phone-fixes-2026-10-09`, starting head `cf575540bc7a28e1bb7a630dbeb5ed3717838d9f`; current produced application TEST303 source `7505a36ae1567458ba854db9427196a893824806`. The four relevant production files (Wondrous, CursedWand, Electricity, Mimic) are byte-identical between checked TEST285 source `0652d715cea86fb0242f4470cf870b3761672d0f` and TEST303.

Compared with upstream SPD releases v4.0.0 (`2bb34a4e91d29c8785a9363cad6ddfe5122b1d4f`) and v4.0.2 (`57a4e06a4caf162446d1c28caa7983f0493fecf0`); the five checked upstream files are identical across these two releases. APD's internal version label is not evidence of a corresponding upstream v4.0.8 tag.

Primary upstream files:
- [Wondrous](https://github.com/00-Evan/shattered-pixel-dungeon/blob/57a4e06a4caf162446d1c28caa7983f0493fecf0/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/weapon/curses/Wondrous.java)
- [CursedWand](https://github.com/00-Evan/shattered-pixel-dungeon/blob/57a4e06a4caf162446d1c28caa7983f0493fecf0/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/wands/CursedWand.java)
- [Electricity](https://github.com/00-Evan/shattered-pixel-dungeon/blob/57a4e06a4caf162446d1c28caa7983f0493fecf0/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/blobs/Electricity.java)
- [Mimic](https://github.com/00-Evan/shattered-pixel-dungeon/blob/57a4e06a4caf162446d1c28caa7983f0493fecf0/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/Mimic.java)
- [ShockingTrap](https://github.com/00-Evan/shattered-pixel-dungeon/blob/57a4e06a4caf162446d1c28caa7983f0493fecf0/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/traps/ShockingTrap.java)

## Actual paths and expected behavior

### Weapon curse to random effect
`items/weapon/curses/Wondrous.java#proc` has a base 1/8 proc chance, multiplied by existing enchantment modifiers. It builds a STOP_TARGET bolt from attacker to defender and invokes a valid CursedWand effect directly. This file is identical to upstream. WondrousResin can alter positive-only selection; it does not make every electric-looking effect a conductive field.

Two relevant random branches have different spatial rules:
- `CursedWand.LightningBolt.effect` visits the 3x3 cells around the user and around the bolt collision position, deduplicates characters, applies direct Electricity-source damage and a short paralysis. It never examines the water map or seeds an Electricity field. Merely passing `new Electricity()` as a damage source does not run the field's spreading algorithm. At scaling depth D its base damage roll is NormalIntRange(5 + floor(D/4), 10 + floor(D/4)), before applicable scaling/mitigation. Positive-only skips allies; a disguised neutral mimic is still eligible.
- `CursedWand.RandomAreaEffect.effect` can activate a ShockingTrap at the target. That trap seeds density 10 in its nonsolid 3x3 area; those persistent fields subsequently spread through cardinally connected water. Thus an electric curse can conduct, but this is a different branch.

`CursedWand.RandomWand.valid` requires an actual wand origin, so an arbitrary normal WandOfLightning zap is not a valid Wondrous weapon effect. The ordinary lightning wand has its own character-arc and water-range rules and should not be substituted for either branch above.

### Field reach, damage and reveal
`actors/blobs/Electricity.java#evolve/spreadFromCell` spreads first via NEIGHBOURS4 into water, then processes occupied cells. There is no diagonal-only or dry-tile conduction. A dry original seed can enter adjacent water; the trap's directly seeded dry cells can also shock, independently of later conduction. For a nonimmune occupant, the field attempts to apply Paralysis and calls damage only on odd remaining density. The raw roll is round(Random.Float(2 + D/5f)), so it can be zero. An even-density tick can therefore reach a mimic and apply a negative status without making any damage call.

An electrically immune occupant is skipped before either the field's paralysis or damage dispatch. A normal mimic has no built-in electrical immunity. A hypothetical electricity-immune mimic can remain hidden in a reached field; a direct call to its Mimic.damage still reveals before immunity can eliminate HP damage. The immunity fixtures are controlled tests, not a claim the tester's mimic was immune.

`actors/mobs/Mimic.java` initializes hidden mimics as NEUTRAL/PASSIVE, not the ordinary mob SLEEPING state. Its damage and defenseProc overrides leave PASSIVE and reveal before delegating to HP mitigation; no positive-damage threshold exists. Accepted negative buffs also reveal immediately. Positive buffs and beckoning do not automatically reveal it. Ordinary mobs' separate sleep response remains unchanged.

`Char.attack -> Mimic.defenseProc -> Mimic.damage -> Mob.damage -> Char.damage` distinguishes a miss (no handler reached), an attack reduced to zero, a zero direct call, resistance/immunity and shield absorption. The probe records requested damage and actual HP loss separately. The real MimicSprite switches out of its chest animation on reveal. Crystal mimics retain FLEEING; other tested mimic variants retain HUNTING. No generic enemy behavior was changed.

### APD modifications checked
- Wondrous and ShockingTrap are byte-identical to upstream.
- Mimic's APD diff only adjusts generated rewards through CustomEquipmentHooks. Its reveal, damage, buff, sprite and persistence methods match upstream.
- LightningBolt's only APD change wraps the original damage in CustomOffensiveHooks.raw. Neutral rules call the original virtual damage method; nonneutral rules scale once and dispatch through OffensiveDamage.apply to the same virtual method/source. They do not bypass Mimic.damage.
- Electricity adds saved cell ownership and damage scaling. It retains upstream cardinal water topology, priority, density countdown, odd damage ticks, paralysis, source class and random roll. Existing eight ElectricityDamageProvenanceTest methods cover role scaling, environment, mixed ownership, water/save/load, malformed metadata, neutral topology/turn/RNG equality and other electric systems.
- DifficultyHooks scales ordinary hostile attacks and created-mob health/resources; it does not redefine the cursed burst's target area or the mimic's reveal threshold. Custom offensive percentages can alter numbers, including rounding small amounts to zero, without changing virtual reveal dispatch. Char.damage calls ApdCombat.onDamage after mitigation with requested amount, prior HP/shield and absorbed/HP loss; ApdCombat conditionally forwards it to the recorder. That observer does not decide whether to reveal the mimic, which has already occurred in its override. No new historical event is invented by this investigation.

## Deterministic regression cases
New test: `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/combat/MimicElectricityInteractionTest.java`. All calls use actual production curse/field/mimic code; the headless sprite adapter suppresses status rendering, retains real emitter-start RNG and real lightning construction, and a separate assertion uses the real MimicSprite. Synthetic fixtures stay in disposable test state, never player saves/history.

| Case | Expected and asserted result |
| --- | --- |
| Actual seeded Wondrous LightningBolt proc; far mimic in connected water | Nearby mimic damaged/revealed; far mimic no damage call, unchanged HP, still hidden; no field seeded |
| Direct target and both burst neighborhoods on dry ground | Each selected mimic receives electrical damage and reveals |
| Overlap between burst neighborhoods | Exactly one damage call per character |
| Same far position, connected/disconnected/dry terrain | Outside-area burst reach unchanged |
| Positive-only burst | Hero unharmed; neutral hidden mimic still hit |
| Conductive field, distant connected water | First even tick applies paralysis/reveals with no HP damage; following odd tick damages |
| Disconnected pool and diagonal-only water | No field, no status, no damage, no reveal |
| Mimic on dry land beside energized far water | No propagation onto its dry tile |
| Direct zero electrical damage | Handler called, HP unchanged, real chest animation ends |
| Actual missed attack followed by successful zero-damage attack | Miss stays hidden; landed zero-damage hit reveals |
| Full shield absorption | No HP loss, but reveal occurs |
| Electrical immunity | Field reaches tile but skips occupant; direct handler still reveals even if HP damage becomes zero |
| Paralysis immunity plus zero field damage roll | Even tick stays hidden; odd zero-damage call reveals |
| Positive and negative buffs | Bless leaves disguise; paralysis reveals without a damage call |
| Beckoning | Hidden mimic remains hidden |
| Actual RandomAreaEffect ShockingTrap branch | Conductive field reaches distant connected mimic, unlike LightningBolt |
| BurnAndFreeze / RandomWand validity | Actual negative status reveals; normal wand effect rejected for weapon origin |
| Custom player damage 25/100/500 percent | Original electrical source and virtual mimic reveal retained, amount scaled once |
| Normal/golden/crystal/ebony variants | Zero damage reveals each; crystal reaction remains fleeing |
| Save/load hidden and revealed states | HP/HT/position and appropriate state/alignment survive existing Bundle format |

## Actual test results and retained failures
Final test source: `332a3e4dc2264cfe8a43bc1fc18a943b2e218540`.
[Full core run39392806546](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39392806546), job118528621088, passed.
Downloaded artifact12250093631 ZIP SHA-256: `1daaaab21e30f7f892736a41b83f518c23792d3a49077148705c712a7a152063`.

The actual JUnit XML contains **2,008 unique tests / 391 suites / zero failures, errors or skips**. Comparison against TEST303's actual report confirms all1,988 previous methods remain present and passing, plus the20 new cases. The new suite took0.069seconds in this headless CI environment. Its stdout confirms real Wondrous lightning seed87 (adjacent damage calls1, far calls0) and RandomAreaEffect shocking branch seed5. These are fixture seeds with explicitly controlled state and render scheduling, not instructions to reproduce the tester's historical dungeon seed.

The first run [39392254669](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39392254669), sourcefb4231f054652421268786b2829cab1032550669, retained2008cases with2new fixture errors: missing synthetic Level.plants and Level.traps maps. Its artifact12249588514, ZIP SHA-256 `4eaf272be8922054c4a7620554cea2baf652dc4a8d25242d7c9ba805d91373b3`, was downloaded and inspected. No production defect caused those NPEs. Initializing the maps as real floors do fixed both without deleting cases or loosening assertions. The corrected full run [39392584054](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39392584054), source7d84ef744050a985950f0c6820002bf427b400f3, passed. That revision also strengthened the zero-attack case to use actual missed and landed Char.attack calls.

The final revision additionally retains the real Emitter.start RNG draw and verifies actual MimicSprite animation rather than relying only on a quiet sprite adapter. Final XML/capture-free core evidence is checked independently of the green workflow badge. The existing TEST303 native Android evidence is not relabeled as a new Android mimic test.

A source comparison from starting head through the final test source contains only the new test and executable core-test request. Both already-delivered TEST303 APK hashes were rechecked unchanged. Their original build acceptance remains1,988core/53Android; the new2008 core result belongs to this investigation source, not a rebuilt APK.

## Changes and disposition
Only the new regression test, `docs/APD-CODEX-CORE-TEST-REQUEST.txt`, this report and handoff/completed-work ledger were changed. No production class, preference, raw record, saved view, save format, RNG algorithm or balance change. No fix or migration is justified by the reproduced behavior.

No new APK number is consumed and no misleading Bug Fixes entry is added for unchanged intended mechanics. The verified TEST303 developer/player APKs remain the latest produced pair, with all331 historical records and ongoing UIMSD work preserved. The next actual application build must still use TEST303 producer/source as predecessor and include this investigation's test/documentation paths in its change manifest.

Limits: these are real-engine headless core tests and source comparison, not a reconstruction from the tester's save, native Android acceptance or physical-device verification. The exact historical event remains unproven because the necessary scene/effect/hit evidence was not recorded in the supplied conversation.
