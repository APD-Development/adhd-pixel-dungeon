# TEST122 equipment preparation — October13,2026

Previous produced/verified APK:TEST121source010c44c877ef03dca7fe5ac2987cde309471d089/run38410865529. Work continues on real parent6f12d0def0f15e7062f95cfee1609d95675da646. Stage only; no automatic tester distribution or public release.

## Implementation

Equipment Availability affects ordinary generated weapons(including missiles),armor,rings and artifacts.25–75%retains each eligible generation opportunity at the selected chance;125–175%adds a fresh eligible item at the excess chance;200%adds one fresh item.100%keeps original factories/items/RNG and does not call an extra factory. Artifact extras use the existing unique artifact generator and its normal ring fallback rather than cloning an artifact. Wands,trinkets,bags,consumables,keys,strength potions and scheduled upgrade scrolls are outside this slider. Shop extras retain normal zero-upgrade/uncursed sale conventions. More ordinary item opportunities retain normal chest/mimic placement mechanics; these interactions are not independently nerfed.

Boundaries:ordinary floor and hidden-book loot;shop stock;regular monster,wealth/lucky bonus loot;statue/armored-statue drops without changing combat gear;HermitCrabarmor;new mimic bonus prizes;armory/crypt/pool/trap/sentry/pit/crystal and named secret-room equipment;grassy graves;incidental mass-grave and blacksmith-room floor equipment. Existing instances,thief-stolen items,Mimiccarried prizes and recovered player gear are never passed through a general-drop filter. NPCquest reward selection,progression items and special interaction rewards(Sacrifice/TormentedSpirit)remain original. No general Level.drop modification,inventory capacity change or player-data rewrite.

Local defect caught before commit:adding extras after settingCRYSTAL_CHESTcaused Level.drop to reroute into adjacent cells;an isolated pedestal had no legal adjacent placement and looped indefinitely. Stackdump identified Level.drop1046 viaCrystalVaultRoom.paint. Fixed by dropping complete batch before sealing the heap. Permanent3000msregression verifies two distinct items in one isolated crystal chest. This defect was in the local equipment draft,notTEST121; no claimed shipped-player fix.

Fifteen gameplay-effect implementations now exist. Player/Enemy/BossDamage remain incomplete/disabled. Permanent UIMSDselected/actual-generated count recording,run details/filter/export acceptance and scoring interface still need completion. Original earned scores and provisional ranges unchanged. This is not alpha completion.

## Tests

Nine new equipment methods passed locally,0failed,2.560seconds:all eight percentages/neutralRNG/freshobjects/non-equipment protection;all six classes' actual floor/expanded-shop generation;actual monster-drop expansion;existing-loot and stolen-player item preservation;isolated chest batch and24neutral snapshots. Neutral golden snapshots came from actual originalTEST121scope sources with fixed seeds and one eligible bag;12floors+12shop stocks and next RNG match. Existing identity-keyed shop-bag ties were not changed. Earlier comparison fixture seeded before Dungeon.init(reset),then corrected; stale local CrystalPath classes were replaced with actual retrievedTEST121sources before final baseline comparison. The existing crystal reward correction is preserved,not reimplemented. Initial fixture omitted heaps/activeGame context; fixed fixture to match room painting with Dungeon.levelnull. A missing Heap import and SparseArray fixture accessor were corrected; failed compilation was not counted as successful testing.

Related generation/source tests also passed14cases in11.859seconds before the final permanent neutral/drop regressions. Full required core(expected1379unique),23API34checks,26validPNG,debug assembly and screenshot review remain pending. New Android check touches equipment200%,saves next-run preferences and verifies active configuration/score unchanged. No physical RedMagic/Android16 session performed; no fullsix-class/five-boss custom-rules playthrough claimed.

## Timing/artifacts

Local final9case suite2.560seconds; CI timing and actual totals will replace pending status after reports are downloaded. No reliable total active/idle clock exists. Last staged playable verifiedAPK remainsTEST121 until this package passes. Do not uninstall/cleardata. No signing/applicationID files changed; no signed-compatible publication receipt or deliveredAPK is claimed.
