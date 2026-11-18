# Ring of Wealth loot investigation — TEST285
Accepted 2026-10-19. **No loot-generation defect was found in the audited paths or seeded tests. No production loot/balance change is necessary on this evidence.**

## Source and reproducibility
TEST285: `0652d715cea86fb0242f4470cf870b3761672d0f`. Current delivered TEST300 application: `d41631b3e5b43bf832036426a9e7c7a0694d07ed`. RingOfWealth, Generator, Mob and Goo are unchanged between these builds.
[Official SPD RingOfWealth at inspected upstream commit](https://github.com/00-Evan/shattered-pixel-dungeon/blob/57a4e06a4caf162446d1c28caa7983f0493fecf0/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/rings/RingOfWealth.java) has identical source to TEST285. The shared algorithm was checked rather than assumed correct from its origin.

[Regression commit](https://github.com/APD-Development/ShieldingShouldHaveWorked/commit/694ad29ca1903c29f9e173c3040e912d3b0a3eff) adds eight methods in `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/RingOfWealthLootInvestigationTest.java`. [Complete core run39329677064](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39329677064): **1,967 unique tests, zero failures/errors/skips**. New eight-method suite: 1.483s on CI. Artifact12223342606 ZIP SHA256 `b67a8b7e59078d6ebacac2976c6ef1c10fbde02d7cdea9f6c92a699344cd7049`. Seeds and assertions are in the test; [full measured output](APD-RING-WEALTH-SIMULATION-2026-10-19.txt) includes all regions, roles, species and potion classes.

## The three loot channels
- **Ordinary enemy loot:** a source-specific table. Flies, bats and necromancers normally select Healing only. Wealth increases the chance of this existing drop; it does not replace that source's table with a universal potion table.
- **Guaranteed boss rewards:** Goo's death code separately creates its Worn Key and 2–4 Goo Blobs. **Goo has no guaranteed ordinary Healing potion in TEST285.** With no ring or other bonus effect, 100 actual Goo loot rolls produced no ordinary potion.
- **RoW bonus rewards:** separate `tryForBonusDrop` counters and consumable/equipment generators. These do not call the killed enemy's `createLoot` table. A colored RoW flare marks bonus activation. Ordinary and bonus rewards may coexist in one heap, so a healing item alone is not a provenance marker. Lucky enchantment is another independent bonus source.

APD's `GuaranteedLootIdentification` identifies only normal Healing/Shielding drops from flies, bats and necromancers. It changes knowledge, not item class or selection. Potion knowledge then applies globally, so later Healing drops can also be recognized. Other unidentified potions cannot be assigned a type from their appearance alone.

## Ordinary Healing drops and caps
With no other loot modifier, noncursed ring level L multiplies ordinary drop chance by `1.2^(L+1)`: +0=1.20, +1=1.44, +3=2.0736, +5=2.985984. A probability above one makes that eligible ordinary roll certain, while source caps still apply. Without a ring, the multiplier is1.0.

| Source | Starting ordinary chance without RoW | Selection and limit |
| --- | --- | --- |
| Fly / Swarm | 1/6 for an unsplit swarm | Healing only; chance falls with split generation and the 5-potion run counter. |
| Bat | 0.1667 | Healing only; diminishes to zero at its 7-potion counter. |
| Necromancer | 0.20 | Healing only; diminishes to zero at its 6-potion counter. |
| Warlock | 0.50 potion-drop chance | Special Healing check, then random non-Healing potion fallback. At counter c, conditional Healing chance is max(0,7-c)/24; the source stops giving Healing at c=7. |
| Goo | 0 ordinary Healing | Key/blobs are separate guaranteed rewards; boss bonus weight is15. |
| Rat / Crab / DM100 / Skeleton | Their own ordinary tables | Not a universal potion roll; no normal potion appeared from these species in the trials. |

160,000 actual ordinary-kill rolls (8 species × 5,000 × no ring/+0/+3/+10) used an explicit very large bonus counter to isolate this channel. All fly/bat/necro runs reached their exact5/7/6 Healing caps and produced zero other ordinary potion types. Warlocks produced7 Healing plus2,482 other potions without RoW, and7 plus2,999 with +0. DM100, Skeleton, Crab and Rat produced zero ordinary potions. Source-dependent diminishing rates mean these long-run capped totals must not be compared to a constant per-kill percentage.

`Mob.rollToDropLoot` also checks hero level eligibility before either channel. Other talents/trinkets can modify ordinary chance; those were disabled in the controlled baseline.

## Weighted potion selection
`Generator.randomUsingDefaults(POTION)` samples the combined weights of two15-card decks without depleting them. This differs from floor-generation deck draws.

| Type | Expected default weight/share | Observed in150,000 draws |
| --- | --- | --- |
| Strength | 0/30 =0%; dedicated floor scheduling | 0 |
| Healing | 6/30 =20% | 29,864 (19.91%) |
| Mind Vision | 4/30 =13.33% | 20,130 |
| Frost | 3/30 =10% | 14,856 |
| Liquid Flame | 3/30 =10% | 15,165 |
| Toxic Gas | 3/30 =10% | 15,003 |
| Haste | 2/30 =6.67% | 10,063 |
| Invisibility | 2/30 =6.67% | 9,947 |
| Levitation | 2/30 =6.67% | 10,046 |
| Paralytic Gas | 2/30 =6.67% | 10,001 |
| Purity | 2/30 =6.67% | 9,927 |
| Experience | 1/30 =3.33% | 4,998 |

All positive-weight types appeared, Strength remained absent by design, and replaying the same seed reproduced the exact sequence hash. No equal-probability assumption was used.

## What upgrading changes
The bonus counter draws a normally distributed integer0–20 delay, mean10 weighted tries. Normal enemies contribute1, minibosses5 and bosses15. This is a renewal counter, not an independent10% coin flip per kill; one boss can yield multiple rewards. Ring level does not multiply this counter's rate.

136,800 bonus-only eligible kills across ring levels0/1/3/5/10/15, floors1/6/11/16/21 and all three roles supplied252,000 weighted tries. These use the actual Mob bonus path and actual item generator:

| Ring level | Weighted tries | Expected mean rewards | Observed rewards |
| --- | --- | --- | --- |
| +0 | 42,000 | 4,200 | 4,224 |
| +1 | 42,000 | 4,200 | 4,196 |
| +3 | 42,000 | 4,200 | 4,199 |
| +5 | 42,000 | 4,200 | 4,217 |
| +10 | 42,000 | 4,200 | 4,179 |
| +15 | 42,000 | 4,200 | 4,198 |

Upgrade level changes consumable tiers, stack quantities and equipment quality. Low/mid/high consumable tier weights change from60/30/10% at +0 to0/60/40% at +15. Equipment is interspersed after5–10 consumable rewards; its minimum upgrade, tier and enchantment chances scale with the ring, with the existing second-ring cap retained.

720,000 actual consumable draws (120,000 per level) confirmed the tier mixtures. Not all potion rewards come from the generic category: the high tier has an additional Experience route, while the mid tier can generate exotic potions. Thus the Healing share among regular-potion reward stacks changes with level:

| Ring | Expected Healing share among regular-potion stacks | Observed Healing/regular stacks |
| --- | --- | --- |
| +0 | 17.348% | 3,896/22,612 =17.230% |
| +1 | 16.749% | 3,810/22,166 =17.188% |
| +3 | 15.472% | 3,292/21,118 =15.589% |
| +5 | 14.074% | 2,851/19,969 =14.277% |
| +10 | 9.930% | 1,722/17,908 =9.616% |
| +15 | 4.516% | 720/15,643 =4.603% |

These are stack/event counts, not individual units; the simulation also records quantities. Conditions: no Exotic Crystals, challenges, extra loot effects or rule changes in this baseline.

## Four consecutive Healing drops
- Four independent **default potion-category draws** all being Healing: `0.2^4=0.0016`, **0.16%,1 in625**.
- Four successful ordinary potion drops from Healing-only sources: **100% conditional on those source drops**, not 1 in625. This says nothing about how many kills are needed to obtain them.
- Four successive regular-potion RoW reward stacks at +0, with the baseline conditions above: approximately `0.17348066^4`, **0.0906%, about1 in1,104**. At +3 it is about0.0573%; this is a different conditioning from the category draw.
- Counting Goo's key/blobs as Healing or counting every ordinary potion as an RoW bonus would be incorrect. In2,000 actual Goo bonus-loot trials with +0 RoW, there were80 Healing bonus stacks and2,954 other bonus stacks, with zero ordinary Healing.

The player's four observed drops cannot be assigned exact individual provenance or an exact run-specific probability without their enemy identities, ring/trinket state and prior cap counters. Their reported count is compatible with the implemented tables; this conclusion is supported by source tracing and seeded measurements rather than an assumption that any pattern is RNG.

## APD modifications and preservation
Five difficulty presets and both endpoints of all18 Custom Rules produced the same1,000-draw consumable sequence under each controlled seed (41,000 compared draws). Equipment quota hooks leave potion instances unchanged; their intended effect can remove/add fresh equipment rewards, and additional generation can advance the shared RNG. Enemy spawn/HP and floor-resource rules affect opportunities or floor items, not a conversion of every enemy potion into Healing.

The separate Healing-to-Shielding override is only invoked at its typed Exotic Crystals conversion seams. The combined-default potion route used here returns regular types directly, matching SPD; RoW's tier-specific exotic rewards use their existing separate paths. No rewrite to Healing was found. Normal class-specific enemy loot uses its original trinket conversion path. These coverage distinctions were audited, not silently changed. Challenges' item-block check does not replace potion classes.

Ring level plus both bonus counters and generator state were serialized/restored and reproduced the next500-attempt reward sequence exactly. All existing tests also passed. Production loot, probabilities, potion identification, RNG, saves and histories remain unchanged; only tests and investigation documents were added. No loot migration, balancing change, new loot APK or in-game bug-fix claim is warranted. TEST300's separately verified hotbar/settings fixes are delivered as their own milestone.
