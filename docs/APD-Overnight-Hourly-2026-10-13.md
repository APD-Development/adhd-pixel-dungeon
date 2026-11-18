# APD overnight work report — October 12–13, 2026

## Scope and how to read the hourly record

All hours below are New York time (EDT, UTC minus four hours). This report is an evidence snapshot prepared at approximately 4:50 a.m. EDT on October 13. It includes the documented late-afternoon/evening work immediately preceding the overnight session so earlier fixes are not silently omitted. The exact time the owner fell asleep is not available.

Each hourly entry records timestamped commits, workflow results or private releases. A commit timestamp proves the change was recorded then, not that an entire hour was spent coding. Workflow wall time includes automation and waiting; it is not human-equivalent coding time. Local test durations are reported separately where available. There is no reliable complete minute-by-minute execution ledger, CPU-work total or verified overnight downtime total; none is invented here. Gaps between commits are not proof of either uninterrupted work or inactivity.

## October 12 — 4:00–5:00 p.m. EDT

4:46:51 p.m.: committed the native statistics-filter correction keeping uncommon text values reachable. Evidence: commit1d369cff8c047e6ba1ec072785a048873e7a2214. This is the earliest record in the inspected repository window; activity before it is outside this report's reconstructed hourly scope.

## October 12 — 5:00–6:00 p.m. EDT

5:01:14: committed removal of an unused sort-key array that could crash larger saved queries (b43316b5c60d9b605ae07e8e6bfc5d43ac847ad2). 5:17:26: committed row-cache CRC32 corruption detection and compatible rebuilds (f056eec38608d916e3e3b4b3b78f62b304a3b9a0).

5:31:50: committed the default-visible stair shortcut correction (cf2a2bca7a83c0f70bb984b25de413740d1dbe1d). The TEST84 attempt failed an outdated default-Off regression assertion and produced no APK. Existing explicit Off preferences were preserved; failed84 changes were attributed to the next actual produced build,85.

5:51:41: committed TEST85's current-state Chalice damage preview and stair discoverability verification (610d4061a153bc7e6774ce14e713b51516737646). Actual verification began5:51:44 and continued into the next hour. Preview respects the actual prick damage path, inclusive fatal bounds and shielding; it does not draw gameplay RNG or change damage. Life Link is explicitly unpredictable rather than labelled safe.

## October 12 — 6:00–7:00 p.m. EDT

6:01:06: TEST85 verification completed successfully,1226 core plus21 API34 emulator methods, zero failures/errors/skips and16 screenshots. Actual Chalice and stair screenshots were reviewed. 6:04:16: compatible TEST85 APK privately published.

6:04:58: committed Blacksmith reward upgrade-level visibility (beefe620896ca1f29ecb67e039817f97ba4e1854). Offered +0/+1/+2/+3 values use the actual reward objects and native item-slot styling, while unrelated curse/enchantment information and reward generation remain unchanged. TEST86 verification ran6:05:01–6:16:07:1231 core/21 Android,18 screenshots. Private publication6:19:03.

6:29:50: committed TEST87's derived-fact-cache CRC32 integrity fix (f2dc6a54836171acc3c2f9872fa5617da7bbf496). Valid-looking ASCII/numeric cache corruption and trailing bytes had been accepted as valid cached facts. Five new regression cases and cache-only format2 ensure damaged caches rebuild from unchanged raw logs. TEST87 verified6:29:53–6:41:39:1236core/21Android/18screenshots. Private publication6:45:07.

## October 12 — 7:00–8:00 p.m. EDT

7:10:47: recorded the TEST79 armor source comparison and initial10,000-seed measurements (e70f49dbeb3f6fba9becbc015c96027f099a5867); did not alter armor generation.

7:40:00 and7:45:27: committed crystal-path reward coverage and signed-boundary statistics formatting for the intended TEST88 (eb83de162190f1030d34f006dd7f68b91942137d;9e4dc5d9ff555a65ef7b3a784b6a6be95b49ac4d). Generation regression tests detected an RNG/deck-sequence change. That failed attempt was corrected rather than shipped.

7:56:26: committed TEST89's correction preserving the original generator draws while making the two final two-key-path rewards standard Experience and Transmutation (b529832d07a1a2ba30453427edbd346dbf8b4124). Golden coverage exercised286 seeded cases:242 stable and44 unstable. This did not change global potion/scroll probabilities, door/key mechanics or unrelated loot.

## October 12 — 8:00–9:00 p.m. EDT

8:09:13: actual verified TEST89 APK privately published. Delivery receipt recorded8:10:38.

8:18:36: committed saved-query empty-string compatibility (21e4f7579715f42897408efe06535762014cc759). The codec previously lost the distinction between a known empty text value and no filter value; the additive counted encoding retains legacy compatibility. Five new regression methods. TEST90 verified1244core/21Android and privately published8:35:23.

8:26:07: committed the read-only real-SPD-fork source comparison (2b29f52a26255bf6167def6dc5e0b25b8f5c57c2). No app rebuild was needed. At the measured TEST89 source,659 source/config files changed,80,979 lines added and495 deleted; net80,484. These include comments, blanks, tests and tooling, not just gameplay or Codex-authored work. The all-kind ledger contained969 changed files, including binaries without line counts.

8:46:37: committed native game-item sprites for News and Change History (e592264b512bf25a7d4da87bc0510a4ee4f712d3).8:52:31 and8:54:46: corrected historical sprite validation and requested the verification retry (e6569698c29b1b753fb27b076a477e6f5a2570df;3e9b2e2a132cda2dc0331d62c590159b4410f388). TEST91 failed one history validation case and produced no APK; the actual successful candidate was92. ItemSpriteSheet sprites are resolved without creating items or consuming RNG.

## October 12 — 9:00–10:00 p.m. EDT

9:07:57: verified TEST92 privately published,1247core/21Android/18screenshots. The native gold-bordered compact two-column history screenshot and real crystal-key icon were reviewed; descriptions/build attribution were retained.

9:12:05: committed TEST93 comparison-picker pagination and seed-floor allocation improvements (aebb1abf66755c87c175b5663af15586a628e4d5). Removed the first40-record comparison restriction by using native pages without limiting available records;81 choices exercised on Android. Removed repeated per-row Dataset/RunInfo creation. Three new regression methods;1250core/21Android. Private publication9:26:28.

9:28:29: committed TEST94 cache-source namespace isolation (1145e043e9e2ceb0aaf189282ede2f53a9423c8d). Equal-name, equal-length run and imported-ranking files previously shared a derived cache and could lose an imported record. The permanent archive round-trip regression reproduces this; runs-/legacy- cache prefixes preserve both. Removed only an unused private singular helper and assigned-but-unread counter.69 targeted cases passed;1251core/21Android. Private publication9:42:53. This may temporarily increase derived-cache disk usage; it was not advertised as a storage reduction.

9:47:21: committed TEST95 conservative custom-question rate bounds and large-rate display (3ebdb4082adfbbb0de085cb29618da2666d3368a). Incomplete denominator/numerator knowledge is retained rather than producing a misleading exact rate. Four new regression cases and51 targeted checks; full1255core/21Android. Verification continued into the next hour.

## October 12 — 10:00–11:00 p.m. EDT

10:07:56: actual verified TEST95 privately published. No further source commit is timestamped in the remainder of this hour in the inspected branch history.

The later report includes a100,000-seed armor study and local UIMSD investigation, but the evidence available here does not establish their precise start/finish clock times within this hour. Their measured work/results are listed separately below. This gap is not labelled either continuous coding or confirmed inactivity.

## October 12 — 11:00 p.m.–midnight EDT

11:18:39: committed UIMSD initialization, number bounds and empty-filter findings (416f560f094178da3cb23308b295cd0f2147615d).11:20:16: fixed the build ledger to include the preceding delivery-workflow file (a2dce9fe381b211de6a9a11733e4801219c418b2). The TEST96 attempt failed attribution coverage and produced no APK; corrected TEST97 was retained as the actual build.

TEST97 fixes: FactKinds class-initialization monitor deadlock; very large AVG/MEDIAN formatter saturation; outward rate-bound rounding/nonpositive-denominator uncertainty; known-empty text choices; and test-fixture global-state restoration. Six new regression methods across these areas. Native empty-text selection, existing screens and visual evidence checked. Full1261core/21Android/18screenshots. Private publication11:33:12.

11:38:13: committed identified-Healing confirmation and in-place fact-series decoding for intended TEST98 (44dfeaf8c21cfc2ac019d293a6e2b21b8fee954c).11:42:55: corrected an invalid nested sprite constant using an actual public potion-holder sprite (91db176d323ae465ab04bb10d693f2696dfd51cd). TEST98 failed compilation and produced no APK; TEST99 produced an APK and passed1265core/21Android, but its collector omitted two new required healing screenshots. It was not privately delivered as fully evidenced.

Healing confirmation defaults to normal SPD one-tap behavior (Off), applies only to identified Healing potions, supports0–100 inclusive thresholds, and uses the same inventory/quick-use execution. Cancel consumes no item, turn or resource. It never auto-heals, reveals unidentified identity or changes healing amounts.

## October 13 — midnight–1:00 a.m. EDT

12:01:43: committed TEST100 strict raw statistics UTF-8 validation and cache invalidation (624f6991bae64c3f9437be6a5e6e16535cff374d). Malformed CRC-valid text previously decoded with replacement characters and could be treated as good data. Five permanent cases;90 targeted related checks. Raw/save formats and original bytes are retained; startup repair backs up damaged tails before trimming. Derived/index/row cache versions were refreshed.

12:33:42: committed TEST101's expandable quickslot model, invalid-slot/save restoration protection and rollback-history optimization (d129bed4cbab2cf047a6f616dd8574e13a3b385b). Mobile renders six slots; the game model can retain more. An isolated12-slot model retains slot11 through actual save/continue. Six new slot regressions plus29 existing cases. Invalid indices no longer clear valid assignments or prevent later UID restoration.

RunTimeline now sorts/merges rollback intervals and classifies ordered logs once, retaining the old unordered fallback. Two new and existing timeline/recorder regressions passed; raw data/provenance semantics were preserved. The detailed performance measurements below are host-only.

12:35:31: committed read-only extraction of actual TEST100 artifact history/checksum, without another build or publication (a8c8cecd1dc39acb0b70be95d9db7559e9359606).12:48:40: verified compatible TEST101 privately published,1278core/21Android/20screenshots. Earlier unready screenshots were not described as visual verification.

## October 13 — 1:00–2:00 a.m. EDT

1:09:47: committed TEST102's exact TEST100 history date and frame-ready native capture correction (342e36936f8984f8954e5f94979735a6bb8af329). TEST100's actual APK evidence showed October13; six existing history entries were retained byte-for-byte while correcting its date. Screenshot capture waits for two matching painted native control snapshots and saves that Bitmap.

This corrected evidence readiness, not a confirmed persistent production TextInput rendering bug. No renderer/gameplay change was invented. Full1278core/21Android/20validscreenshots passed. TEST102 privately published1:46:17.

## October 13 — 2:00–3:00 a.m. EDT

2:01:41: committed TEST103's Ring of Arcana Spirit Form fallback preview (3f30d631ec0fa74ba11bcc25c3411526924604a8). Known equipped Arcana with ordinary bonus zero can still receive Spirit Form's real fallback. A read-only preview now follows that actual calculation without exposing unknown/unequipped rings, stacking an extra effect or mutating the virtual ring. Four new+16existing cases;1282core/21Android/20screenshots. Private publication2:15:50.

2:18:06: committed TEST104 comparison integrity (946704b57fac583721449fe95b4559d6716c36b2). ANY=false now retains its worst uncertainty provenance. Exact numeric groups and equal lower-bound groups no longer overwrite one another's denominators. Four new+74 existing cases;1286core/21Android. Private publication2:32:20.

2:34:17: committed TEST105 exact group drilldowns (c15313c6d128bcd3557ff8df0b49ffe24258c8d5). Lower-bound/enum/text groups previously opened different records under ordinary EQ. Group-specific predicates retain exact membership, unknown/hidden guards and normal player filter semantics; saved views/Back round trips preserved. Four new cases;75 targeted plus11 SavedViews cases. Android PointerEvents actually opened the native group sheets and their corresponding records.1290core/21Android/20screenshots. Private publication2:49:22.

2:52:10: committed TEST106 newer legacy telemetry refusal in derived facts (015d0392f5ff607746a6db0a2879fd4bbd4c3c45). Unsupported major/minor ranking totals/dailies are preserved as raw evidence without being interpreted under current schemas. Derivation5→6 rebuilds previously wrong caches. Three new+28existing cases;1293core/21Android. Verification completed next hour.

## October 13 — 3:00–4:00 a.m. EDT

3:04:11: TEST106 full verification completed; privately published3:09:42.

3:16:48: committed deterministic same-position metamorphosis choices (cdd9715fac40dd1c88cd4a2ad34b23fcb9da4ea4). The native chooser shows the corresponding talent from each of the other five classes; points/order persist, selection uses no RNG. Already-owned choices remain visible but unavailable, preserving SPD duplicate rules rather than silently substituting another position or inventing a refund rule. Seven permanent methods cover330 class/position application scenarios inside a method, not330 separately counted tests.

TEST107 workflow ran3:16:52–3:23:27. Its APK/core job succeeded (1300core), but Android test compilation failed because assertNotEquals lacked a static import. No Android tests or valid screenshots ran; TEST107 was not called verified or privately delivered.

3:31:10: committed the missing import correction (4b8b3488a88d68baccbfcbd7ee4280639193b00e). TEST108 ran3:31:14–3:42:47:1300unique core/21API34,zero failure/error/skip,22validPNG. Real native talent icon/detail/confirm touches, item consumption, game save and continuation passed. The five-choice and source-class detail screenshots were manually reviewed. TEST108 privately published3:53:01. Feature attribution remains TEST107 (its first produced APK); the verification-only correction belongs to108.

## October 13 — 4:00–approximately4:50 a.m. EDT

4:07:53: committed TEST109 item-history reader corrections and the short offline metamorphosis News announcement (845972359883207d3b2dea41861e25ae3610bab3). Cyclic lineage no longer makes an item its own ancestor; identifiers mentioned only in later item events are retained. Four new regressions failed on original code;40 related item lifecycle/identity/rollback methods passed locally. These utilities currently have no production callers; this is not a newly reproduced backpack bug. TEST109 ran4:07:58–4:19:04:1304core/21Android/22validPNG,zero failures/errors/skips. Produced and verified, not privately published.

4:21:54: committed TEST110 precise run identifiers and CSV completeness (f243122caee015a1dc2d15150908cef850f55507). Case-distinct Run-A/run-a records had mixed in single-run reports, row links and group-to-run navigation. Exact lookup fixes preserve ordinary case-insensitive player searches. CSV now states when the existing large-report limit omits rows; no new cap/source truncation. Five new regression methods;57 related host methods passed. Native row/group PointerEvent checks passed. TEST110 ran4:21:57–4:32:40:1309core/21Android/22validPNG,zero failures/errors/skips. Produced and verified, not privately published.

4:31:39: committed TEST111 restoring four actual quest BOSS classes to UIMSD (976c1c2899d37d687f9978da02193c0f4543f5bd): Crystal Spire, Gnoll Geomancer, Fungal Core and Vault Elemental. Existing gameplay already marks these as bosses; finished statistics had classified them as ordinary enemies. Three new regressions failed before correction;22 related cases passed. Row builder10→11 rejects obsolete incorrect caches and rebuilds from unchanged raw evidence. TEST111 full verification ran4:31:43–4:44:34 and passed1312unique core/21API34 methods,zero failures/errors/skips,22validPNG. Independently validated signing/appID/checksum and identical application source inputs; privately published4:45:26. Delivery commit9a8778ee471f3b6cbdf9530e4d5db3bdaaa08b61/run38370018678. APK SHA25675fee72653a9e191dac8c082cd8418d673485de5400ec42a3b0b409b40d155ec; unchanged signer067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e and applicationIDcom.charltonchris29.adhdpixeldungeon.indev. This is emulator evidence, not physical-phone testing.

Approximately4:36–4:39: the active local tools reproduced a provider-neutral future SyncReplica acknowledgement bug: two edits in one millisecond could let an old acknowledgement clear the newer edit/deletion. Four new methods failed on original code; corrected new+existing13methods passed0.079s. Local source/tests are in /workspace/sync-ack-fix. Not committed, not in TEST111, not Android-verified. No online sync was enabled and no claim is made that live player settings were lost.

Also reviewed additional UIMSD/backend/UI components while CI ran. Current local review128/211 scoped components, with integration hooks and remaining components pending; committed TEST111 checkpoint records the earlier109/211 snapshot. The comprehensive audit is not complete.

## Armor-generation investigation — measured work, not an invented hourly placement

Completed100,000 seeded actual TEST79 dungeon-generation runs,2.2million floors through depth22, seeds202610120000–202610219999. Measured wall time2885.705seconds (48min05.7sec); exact start/finish clock timestamps cannot be established from the available result file.

No armor above leather across all generated heaps including shops:0/100000. Excluding shops:0/100000. No above-leather armor freely reachable under the stated flood-fill approximation:1770/100000 (1.77%). Shops stock mail on11, scale on16 and plate on21. The study did not simulate what the owner actually saw, combat survival or every reward/held item/branch. Zero events in100,000 seeds implies approximately0.003%95%upper bound under this scenario, not a universal guarantee.

Warrior, difficulty3, no challenges, exotic0 and introduction off were used. The owner's exact seed/challenges/difficulty/shop visits remain unavailable. A separate500,000 direct armor-tier draws and APD/SPD generator source comparison showed no probability change. No loot-rate increase or guaranteed armor workaround was implemented.

## Performance and regression evidence

Fact-series decoder reuses its consumed delta array instead of allocating another long array. The host10,000-point/five-series/25decode experiment retained the same checksum and removed400,080bytes per decode. Seven related regression methods; no recording/RNG behavior changed.

RunTimeline ordered rollback analysis:41,001events/1,000rollbacks, unchanged checksum−6835895199601675244. Three host analyses allocated about2.887GB before versus19.37MB after; measured wall time about1.79s versus0.059s. This is a local JVM fixture, not an Android frame-rate claim.

Seed-floor row interpretation removed about480,000bytes of temporary allocations per10,000rows.10,000tiny logs loaded under a256MBhost heap: approximately6.031scold/2.523scached. Tiny synthetic files do not establish phone performance for large real histories. The cache CRC example read a60-run/270,120-event history in317msfromraw versus10mscached on this workspace; this is not a statistically controlled universal speedup.

Full-suite counts above are unique test-method results per build, not cumulative unique tests summed across repeated builds. Android21methods are separate from core counts. All Android evidence is emulator/instrumentation; no physical-phone or owner's actual save validation is claimed.

## Failures and evidence limitations retained

TEST84: old default-Off test expectation failed; no APK. TEST88: RNG/deck-sequence regressions caught and corrected before actual89. TEST91: historical sprite validation failed; no APK. TEST96: build ledger missed a delivery-workflow path; no APK. TEST98: invalid nested potion sprite constant failed compilation; no APK. TEST99: required screenshot collection incomplete; not privately delivered. TEST100: screenshot readiness insufficient; no visual-success claim. TEST107: missing static test import;1300core/debug passed but0Android tests/0validPNG. All successful corrections keep their actual build attribution.

Some local harness attempts lacked compiled dependencies, referenced a nonexistent QueryCodecTest, omitted the source-introspection property, or reused an inlined old cache-version constant. Corrected the harness/source compilation without weakening assertions. They are not falsely reported as game bug fixes.

No exact active-coding-hours total or longest verified inactivity interval can be reconstructed from this evidence. Commit/release/workflow timestamps and benchmark durations are provided instead. No uninterrupted24-hour or overnight Codex worker has been demonstrated. This active turn and finite automation jobs do not establish continued coding after a final response.

## Remaining approved work and deliberately unchanged behavior

Entire UIMSD component/integration audit and full visual acceptance are incomplete; unmatched rollback uncertainty still requires careful handling because an unlocated branch may overcount and cannot safely be called a lower bound. Full general-purpose all-class simulator is not completed; the previous downloadable simulator was Warrior-focused. Custom Dungeon Rules ranges/scoring remain unapproved; no permanent multipliers invented. Online Community/age-provider configuration remains unavailable and inactive. Physical-phone/Bluetooth/nav-bar owner acceptance and original inventory save are unavailable.

Sentry/DM-300 gameplay timing was not reproduced as a twice-per-turn regression; no arbitrary animation-halving applied. Armor generation was not changed. No existing saves/raw history were deleted, no inventory-capacity workaround, no main overwrite/force-push, no new Cleric restriction, no hidden creator lore exposed, no paid API or public release. Signing certificate/application ID preserved in verified deliveries. Install APKs over the existing app; never uninstall or clear data.

This report documents actual progress, not completion of every approved task.


## Main implementation files by work package

Stair/Chalice: SPDSettings.java, APDSettingsPane.java, ChaliceDamagePreview.java, ChaliceOfBlood.java, Earthroot.java, WandOfLivingEarth.java, CapeOfThorns.java and native PlayRunDeviceTest.java. Blacksmith: Blacksmith.java, WndBlacksmith.java, ItemSlot.java and BlacksmithRewardVisibilityTest.java.

Statistics integrity: DerivedStore.java, RowCodec.java, DatasetStore.java, ByteSource.java, RunLogReader.java, RunIndex.java, RowBuilders.java and the cache/checksum/version/namespace regression classes. Queries/comparisons: QueryCodec.java, Op.java, QueryEngine.java, UimsdModel.java, RowMerger.java, PerUnit.java, CellFormatter.java, CellText.java, ChoicePage.java and corresponding query/model/report tests. Read-only item histories: ItemHistory.java and ItemLineageCycleTest.java. Boss classification: BossClasses.java, RowBuilders.java and QuestBossClassificationTest.java.

Gameplay/reward information: CrystalChoiceRoom.java and seeded crystal/RNG regressions; ArcanaInfoDisplay.java, SpiritForm.java and ArcanaSpiritForm regression coverage; QuickSlot.java, Item.java and quickslot capacity/restoration regressions. Healing confirmation: the Healing execution/confirmation setting and native control tests documented in TEST98–101 source records. Rollback performance: RunTimeline.java and timeline/recorder tests. Series allocation: FactEvents.java and series regressions.

Metamorphosis: MetamorphosisChoices.java, ScrollOfMetamorphosis.java, TalentButton.java, WndInfoTalent.java, English misc.properties, MetamorphosisChoicesTest.java and PlayRunDeviceTest.java. News: core/src/main/assets/apd/news.tsv and AboutDeviceTest.java. Report correctness: ReportBuilder.java, ReportRenderer.java, ReportRunIdentityTest.java, ReportCsvCompletenessTest.java and LinkIdentityTest.java. Native UIMSD regression coverage: UimsdScreensDeviceTest.java. Existing component style, save compatibility and raw statistics handling were preserved.

Build records, complete build-ledger paths, screenshot collector/gates and docs/HANDOFF.md were updated with each corresponding candidate. Every exact source SHA above is available in the private repository; the accompanying evidence JSON lists all57 inspected commits and actual private release timestamps. Local SyncReplica.java/SyncAcknowledgementIdentityTest.java changes remain uncommitted and outside delivered TEST111.
