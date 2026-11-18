# APD TEST291: death outcomes and UIMSD table viewport fixes

Status: verified. Both TEST291 APKs built, checksummed and saved for delivery.

Application source: `d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa`. Both APKs use this exact production source. [Full source comparison](https://github.com/APD-Development/ShieldingShouldHaveWorked/compare/cad6c52789beb5c05f07b40031965e09a497b661...d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa).

## Root causes and implemented fixes

1. **Deaths recorded as Abandoned.** For non-Doom damage sources, `Hero.reallyDie` deletes the save inside `Hero.die`, before the caller submits `Dungeon.fail`. Generic save deletion called `ApdRecorder.onSlotDeleted`, which sealed the active log as Abandoned. The later death hook could not reopen it. Permanent death cleanup now uses an explicit `Dungeon.deleteGameAfterDeath` path that removes the same save files without invoking abandonment. The normal death hook records the final outcome, score and exact ranking link. Already-recorded Doom deaths remain sealed once. Explicit manual save deletion still records Abandoned, including deletion of a stored run from the main menu.
2. **Sorting resets the viewport.** Model changes replace the UIMSD table, and its widget explicitly scrolled to `(0, 0)` when rebuilding. The scene and widget now transfer transient viewport state through their shared rebuild path. Horizontal state uses the visible column ID and offset, so changing earlier columns retains the statistic being inspected. The sort key actually changed is kept visible, including secondary and tertiary keys. Signatures use sort rank, so rearranging displayed columns does not falsely count as a sort change. Sorting starts at the first result; filters/settings retain vertical position where possible and clamp it when fewer rows remain. Ascending/descending markers precede long headings so truncation cannot hide them.
3. **CSV outcome omission found during regression testing.** CSV run reports skipped `Report.Facts` blocks, omitting Outcome and other run facts while retaining encounter/item tables. CSV now includes escaped Section/Field/Value/Detail rows for those facts alongside the existing tables, preserving formula protection, provenance and unknown-value marks.

The existing TEST287 Columns fix remains intact: model-driven background rebuilding is deferred while any modal is open. That shared guard covers Columns, Filter, Sort, Options and other UIMSD windows. Repeated native column taps remain in the mandatory Android gate.

## Outcomes, saves and historical records

- Serialized outcomes retain their original values: Died=0, Won=1, Abandoned=2. This is a recording-order fix, not a label substitution.
- Ascensions remain wins for existing win-rate/statistics semantics. New run-end records include an optional explicit Ascended result, available as a filterable/groupable/exportable column. Imported ranking data copies its explicit ascension result when present; older run ends without the field remain unknown.
- Run-end serialization is backward compatible. Existing save paths, run IDs, query/filter formats, saved views, original scores and authoritative raw logs are preserved. Row/fact cache versions advance to rebuild disposable projections.
- **No destructive historical migration is performed.** Older affected logs were explicitly sealed as Abandoned and can lack the subsequent ranking identity. A HERO_DEATH event alone cannot establish permanent death because an Ankh may revive the hero. Ambiguous outcomes remain exactly as stored. A future repair requires definitive identity-linked outcome evidence; no guessing from class, score, seed or approximate time is used.
- Archive tests verify old event compatibility, cold/warm cache behavior and byte-for-byte export/import preservation. Install the APK over the existing application; no uninstall or data clearing is required.

## Verification

| Check | Result |
| --- | --- |
| Developer complete core suite | 1,913 tests, 377 suites, 0 failures/errors/skips |
| Player complete core suite | 1,913 tests, 0 failures/errors/skips |
| API 34 full Android suite | 43 tests, 0 failures/errors/skips; 57 evidence PNGs decoded successfully |
| Exact APK actual-death before/after | TEST287 fails at premature recorder closure; TEST291 passes the same real play/save/load/Hero.die/Dungeon.fail test |
| Signing and application identity | Both variants retain the same package/version code and signing certificate; certificate SHA-256 `067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e` |
| Compiled Changes history | All 322 preceding per-build records preserved; both variants have 324 identical history TSV files including the updated index |
| Variant separation | Developer feature asset present only in developer APK |

Targeted core coverage: 9 TableViewportTest methods, 6 RunOutcomeRegressionTest methods, 2 RunOutcomePersistenceTest methods and 2 ReportCsvCompletenessTest methods. Coverage includes both sort directions, secondary keys, column reordering, short/empty filtered results, cleanup-before-fail, manual abandonment, victories, ascensions, save/load, archival persistence, outcome filters, the Deaths final-result field, CSV and Markdown. Three existing stale-cache fixtures retain their rejection checks with the new cache versions.

The Android gate retains repeated Columns touch propagation checks and tests Sort through real pointer events, including a far-right secondary key, actual ordered values, heading directions, settings and filter refresh. It also exercises actual hero death after saving/loading. Android emulator verification is not a physical-phone claim. The regular player APK has complete core/build/signing/history checks and identical production code; a separate player-variant emulator run is not claimed.

Evidence: [main core/build/Android run](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39297401198), [matching player run](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39297401348), [exact-APK death reproduction and correction](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39297401188). Downloaded ZIP and APK checksums and test XML totals were independently checked.

## APKs

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| ADHD-Pixel-Dungeon-CODEX-TEST-291.apk | 38,641,724 | `d8bfc1741e0c4374ef53c82d60d97e5ce969dbadb160ac94c347eaf4fec1abe8` |
| APD-Player-TEST291.apk | 38,641,547 | `cb30540185a1deb8e0fa116bbb7adbd7013e9a6eaed76fb49540c3847df73ce2` |

Both fixes, the multi-key follow-up and the CSV correction are included in the in-game Bug Fixes entries. No public release or tester distribution was performed.

## Files changed (32)

- [.github/workflows/codex-death-cleanup-device-regression.yml](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/.github/workflows/codex-death-cleanup-device-regression.yml)
- [.github/workflows/codex-sprint-check.yml](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/.github/workflows/codex-sprint-check.yml)
- [android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/PlayRunDeviceTest.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/PlayRunDeviceTest.java)
- [android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/UimsdColumnsModalDeviceTest.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/UimsdColumnsModalDeviceTest.java)
- [core/src/main/assets/apd/apd_changes.txt](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/assets/apd/apd_changes.txt)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Dungeon.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Dungeon.java)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/Datasets.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/Datasets.java)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/RowBuilders.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/RowBuilders.java)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/TableViewport.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/TableViewport.java)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/recorder/LegacyImporter.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/recorder/LegacyImporter.java)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/recorder/RunRecorder.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/recorder/RunRecorder.java)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/report/ReportRenderer.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/report/ReportRenderer.java)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/stats/DerivedStore.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/stats/DerivedStore.java)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/stats/LifecycleEvents.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/stats/LifecycleEvents.java)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/stats/RunFactsBuilder.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/stats/RunFactsBuilder.java)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/UimsdScene.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/UimsdScene.java)
- [core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/UimsdTable.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/UimsdTable.java)
- [core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/QuestBossClassificationTest.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/QuestBossClassificationTest.java)
- [core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/TableViewportTest.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/TableViewportTest.java)
- [core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/recorder/RunOutcomeRegressionTest.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/recorder/RunOutcomeRegressionTest.java)
- [core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/report/ReportCsvCompletenessTest.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/report/ReportCsvCompletenessTest.java)
- [core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/stats/DamagedFactsProvenanceTest.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/stats/DamagedFactsProvenanceTest.java)
- [core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/stats/MalformedPayloadProvenanceTest.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/stats/MalformedPayloadProvenanceTest.java)
- [core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/stats/RunOutcomePersistenceTest.java](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/stats/RunOutcomePersistenceTest.java)
- [docs/APD-CODEX-BUILD-REQUEST.txt](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/docs/APD-CODEX-BUILD-REQUEST.txt)
- [docs/APD-CODEX-CORE-TEST-REQUEST.txt](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/docs/APD-CODEX-CORE-TEST-REQUEST.txt)
- [docs/APD-NATIONAL-PIE-OUTCOMES-VIEWPORT-2026-10-19.md](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/docs/APD-NATIONAL-PIE-OUTCOMES-VIEWPORT-2026-10-19.md)
- [docs/APD-NEXT-BUILD-HISTORY.json](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/docs/APD-NEXT-BUILD-HISTORY.json)
- [docs/HANDOFF.md](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/docs/HANDOFF.md)
- [docs/WORK-COMPLETED-LEDGER.md](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/docs/WORK-COMPLETED-LEDGER.md)
- [tools/run-death-cleanup-device-regression.sh](https://github.com/APD-Development/ShieldingShouldHaveWorked/blob/d7e0ae16b80e7f09b73197217ffdcd7a4a8bc6aa/tools/run-death-cleanup-device-regression.sh)
