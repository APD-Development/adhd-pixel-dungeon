# TEST79 armor generation investigation — October 12, 2026

## Finding
No armor tier-selection regression was found. This does not establish why the owner failed to encounter higher-tier armor in their particular run. TEST79 was confirmed by the owner; difficulty, challenges, shop visits, and seed remain unknown.

Verified TEST79 application source: a611c841e64a8bca592831d3200a5a65e1735b61, from its actual private release records. Compared with current approved source 9d3ce21bfc4209d5a54c0fd6c7c54409aed9f936 and the repository's SPD baseline 2bb34a4e91d29c8785a9363cad6ddfe5122b1d4f. This is a source/headless investigation, not execution of the TEST79 APK on a phone.

## Code comparison
TEST79 and current Generator, RegularLevel, ShopRoom and Armor sources match. Armor tier weights and randomArmor selection match SPD. Generator changes add recorder wrappers. Armor changes append Arcana descriptions. ShopRoom changes only bag tie ordering during replay. RegularLevel changes include the intentional difficulty resource multiplier and floor-1 door repair.

Armor-producing Crypt, Armory, Pool, Traps, Pit, Secret Maze and Mass Grave rooms match SPD. Guard, Armored Brute, Golem, DM200, Armored Statue, Hermit Crab and Mimic sources match SPD. This is not a claim that every item-related source file has been audited.

Conditional on an armor draw:
| Region | Leather | Mail | Scale | Plate |
|---|---:|---:|---:|---:|
| Sewers | 75% | 20% | 4% | 1% |
| Prison | 25% | 50% | 20% | 5% |
| Caves | 0% | 40% | 50% | 10% |
| City | 0% | 20% | 40% | 40% |
| Halls | 0% | 0% | 20% | 80% |

The code uses depth / 5, clamped to the table, so boss-floor boundary indexing should not be confused with a separate region rule. These are tier probabilities given an armor draw, not probabilities of any particular floor spawning armor. General loot uses alternating 35-item decks with two armor entries in one and one in the other, unchanged from SPD.

Shops explicitly stock leather at 6, mail at 11, scale at 16, and plate at 21. A headless test of all four stock-generation cases passed. This does not imply the player discovered or bought that stock.

Difficulty 3 is neutral. Existing difficulty 4 and 5 resource multipliers are 0.85 and 0.70; they affect ordinary loot quantity, not tier weights. No probabilities were changed.

## Measurements
Fixed seed interval 202610120000 through 202610129999, Warrior, fresh temporary headless settings, difficulty 3, no challenges, exotic chance 0; main branch floors 1–22. No player movement, combat, survival, quest completion, or collection policy was simulated.
- 10,000 dungeon-generation sequences, 220,000 generated floors: zero without higher-than-leather armor in generated heaps.
- Excluding FOR_SALE heaps: also zero without higher-than-leather armor.
- Simplified entrance-reachability model, excluding locked/crystal chests, secret/locked routes and shop stock: 184 / 10,000 (1.84%) lacked higher-tier armor in that restricted category.
- Runtime: 368.753 seconds. The requested 100,000 full dungeon sequences were not completed. At this measured rate they would take approximately 61 minutes; the larger sample requirement remains unmet, rather than being relabeled as completed.
- Zero observations in 10,000 gives a one-sided 95% binomial upper bound of approximately 0.030% for the defined generated-heap outcome under these fixed conditions. It does not prove a universal zero probability or quantify player encounters.
- Separate difficulty-5 cohort: 1,000 seeds, zero without higher-tier generated or non-shop heap armor, 27 without higher-tier armor in the restricted reachability category; 49.720 seconds.
- APD generator versus unmodified SPD generator, each inside the same APD headless world: 1,000 matching seeds produced identical tier totals and first-higher-armor-by-depth totals. This is not a full vanilla-SPD gameplay simulation.
- Each generator also completed 100,000 direct armor draws per region (500,000 draws each), with identical observed tier counts and the expected proportions. Armor draws are not dungeon-run simulations.

Generated-heap armor totals in the 10,000 neutral sequences: leather 20,845; mail 33,771; scale 36,894; plate 26,458. These exclude NPC reward inventories, statue/mimic-carried equipment, later monster drops and side branches. Because higher-tier heap armor was already present in every sequence, omitted additional armor sources cannot turn these sequences into no-generated-armor cases.

Reachability ignores combat, traps, affordability, fog and actual exploration decisions. Keys and searching could make additional armor obtainable. It is not an encounter or safe-acquisition probability. The actual run cannot be reproduced without its seed and conditions.

## Changes and limits
Only investigation tools, measured evidence and this report were added. No gameplay, generation, RNG, armor rates, capacities, saves, signing identity or application ID was changed. No APK rebuild is needed for this documentation-only batch. No fix or physical-phone verification is claimed.

The standalone shop-check initially needed its fixture's hero class initialized; after correcting that setup, the check passed. A null fixture was not classified as a game bug.

Rerun source: tools/armor-study/ArmorStudy.java and ArmorShopTest.java, using existing HeadlessTestSupport and game/dependency classes. The study accepts run count and optional difficulty. Raw result summaries are retained in the adjacent evidence JSON.
