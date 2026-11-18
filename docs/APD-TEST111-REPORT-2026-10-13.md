# TEST111 — restore quest bosses to UIMSD boss statistics

## Confirmed cause and fix
BossClasses claimed to mirror the actual Char.Property.BOSS classes, but omitted CrystalSpire, GnollGeomancer, FungalCore and VaultBossElemental. Those actual game classes already carry BOSS. Finished recorded encounters were incorrectly assigned enemy.boss=0 and filtered out of the Bosses table. This was an APD statistics classification bug, not an enemy generation or balance bug.

Added the four existing quest boss classes to the derived classification. RowBuilders.BUILDER_VERSION10→11 invalidates old derived row caches so existing history rebuilds correctly. Raw logs, save formats, original statistics evidence, enemies, loot, mechanics and RNG behavior are unchanged. No unfinished online service activated.

## Tests
Three new regression methods verify classification against instantiated actual game mobs' BOSS properties, ordinary Rat/Pylon exclusions and no classifier RNG draws; encoded combat/lifecycle logs through cold/warm DatasetStore loads and Bosses queries, with unfinished classification still unknown; and a CRC-valid obsolete builder10 cache containing the previously incorrect boss=0 values, which is rebuilt to correct values without changing raw bytes. All three failed against the previous implementation, including direct actual CrystalSpire classification and the real derived enemy row. New+existing boss privacy, cache checksum/allocation/text/namespace checks:22 unique host JVM methods passed in0.576seconds.

The initial local cache-suite run used an already-compiled test whose final constant still contained builder10; recompiled the unchanged tests against builder11, and all22passed. Assertions were not weakened. This is host evidence; full Android compilation and the existing21 API34 screen/game tests plus22PNG files remain required. Expected core total1312. No physical-phone or owner's save test performed.

## Previous candidates and continuing audit
TEST109 source845972359883207d3b2dea41861e25ae3610bab3, run38367192732:1304core/21API34/22validPNG and successful debug build verified08:19:04UTC, not privately published. TEST110 sourcef243122caee015a1dc2d15150908cef850f55507/run38368759449 produced an APK and passed its core/debug job; native Android job still pending when this preparation record was written. It contains precise run identity/report/row/group lookup corrections and CSV omitted-row disclosure. TEST111 history compares to the actual immediately preceding110 APK, retaining each candidate's own attribution.

Most recent privately delivered playable fallback remains TEST108. The audit checkpoint now records109/211 reviewed scoped production components, with remaining components and game integration hooks pending. Original pinned scoped inventory35013 physical lines is not a completed before/after audit. Unmatched rollback uncertainty remains under investigation. No demonstrated continuous Codex coding worker or paid API dependency.

## Actually verified and delivered
Actualsource976c1c2899d37d687f9978da02193c0f4543f5bd/run38369863635,08:31:43–08:44:34UTC. DownloadedXML1312core/21API34,0f/e/s;22validPNG. Direct signing/appID/source-tree validation passed; delivery38370018678, privatepublication08:45:26UTC. APK SHA25675fee72653a9e191dac8c082cd8418d673485de5400ec42a3b0b409b40d155ec. No physicalphoneclaim. 109/110precedingAPKhistoriesremainseparate; actual108→111deliveryskipsdo noteraseproducedcandidatehistory.
