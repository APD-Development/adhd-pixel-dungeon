# Gathering scroll pair — implementation checkpoint
Owner-authorized behavior:
- Regular: all eligible loose ground HEAP items on current floor into one pile beside actual downstairs; no unopened containers, FOR_SALE stock, carried inventory or enemy inventory.
- Exotic: same eligible ground items, one ordinary pile outside destination shop entrance; no sale/gold. Floor8 ->11. Imp is real floor20 CityBossLevel. In DemonHalls an existing scroll sends back to20, not nonexistentfuture shop.
- Neither variant may generate after19. Reading a previously acquired scroll after19 remains permitted.
- Imp counts; pending delivery waits for shop opening. LostBackpack is not ordinary loose loot and stays. Keys/required quest item transport decision remains pending.
- Names, rarity/dropweights and alchemycosts remain undecided; no player-facing placeholder names or inventedrates enabled.
Implemented, not live:
GroundLootPlan captures actual Item references from HEAP only, without pickup/drop/merge/RNG/containeropening. Source identity/quantity/type revalidation guards stale plans; invalid/aliasedground objects failclosed. No inventorycapacity change/limit on100stacks.
LootShipment serializes complete Item bundles and exactsource/branch/destination/transferID, validatesformat+stack/quantitycounts; missing/future/truncatedpayloads failclosed rather thandiscarding items. Preservesupgrade/enchantment/curse/identification/quantity/UID through repeatedrestore. Payload alone is NOT exactly-once transfer.
LootScrollRules separates generation1..19 fromrouting1..5->6,6..10->11,11..15->16,16..26->20. Backwards/same20shipment validation covered by newer rerun.
Verification:
cbe85b1a48ee12c41760e6582c90155485b175cb /40763059003 passed actual2132unique core cases,0fail/error/skip. Artifact12848996634 ZIP74a682dfba52d23f8dda8205cc380551ea1013836fdede9671eaee50f5808d26 rehashed and XMLparsed locally.
First40762333121 ran2127cases,only1failed: earlierwarm-title sourceguard usedwrongrelative testworkingdir. All5newlootplan cases passed. Correctedfixture searchesrepoancestors and retains actualhookassertion; no productionfix hidden bythis correction.
45f856f9c6aae2843fb6b842edf64b11ff945b33 /40763648705 passed2134unique coremethods,0fail/error/skip. Artifact12849092139 ZIPfda585cdf4feb64ef06ae0c7d30654964ca1216ae7c3ccfbbea3958166bc537e rehashed andparsed locally. Includesall2132priorcases plus2newroute/latepayloadcases.
Required next implementation:
1. Dedicated journaled compound commit for scrollconsumption/runstate+sourcefloor/pendingqueue and destinationheap/receipt. Existing saveGame/saveLevel are separate, so an ordinary queue alone can duplicate/lose afterinterruption.
2. Recover durable prepared transfers before run/floor load and before later saves; preserve original bytes oncorrupt/futurejournal. Persist deliveredIDs with destination so repeatedload/revisit cannotredeliver.
3. Safe deterministic destination geometry: use actualREGULAR_EXIT, not Level.getTransition fallback-to-entrance. Exclude pits/traps/containercollision/hazards; no automatic terrainpress/drop/scatter/identification. ForImp20 use exteriorapproach toactualshopfootprint (Room.entrance fallback is inside!), notshopkeeper coordinates.
4. Supportalreadygenerateddestination bysafe disk-level transaction without swappingliveDungeon.level/actorstate; notonlygenerationhook. Ifnosuitabletile keeppending; ifnolegitimate route nonconsume.
5. Scrollclasses/normal+exotic ID integration must preserve12existing runelabel assignments whennewclassadded. Do not decide economicgeneration/alchemyvalues silently.
6. Regression/savefaultinjection + Android inventory/read/itempopup/regular-exoticbehavior verification,thenappropriatenextuniqueAPK. No APK buildor gameplayactivationhasoccurredforthispackage. Do notsayfeaturecomplete.
All existing saves/rawhistory/UIMSDstats/capacity/gameplay preserved. Item relocation must not recordgeneration/pickup/sale. Armed/timedground objects and progressionkeys requireexplicitbehavior beforelivecrossfloorcasts.
