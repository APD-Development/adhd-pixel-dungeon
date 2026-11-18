# UIMSD dataset schema acceptance — 2026-10-17

## Result

The complete current `Datasets.java` blob (`2dfad3e175044300e5cfcf9cea1326a5161a1274`) and its schema/view/navigation callers were read against approved-branch source `5eb573d98fb154b13e6f7d35a4cd95aac740882d`. No production defect was confirmed. One missing acceptance invariant was retained as test-only source `49c58cf175e6742f5499d9e1b243847eac4aad27`.

`DatasetSchemaContractTest` now fails if any core, event or fact dataset id is duplicated; a shipped view names a missing dataset; an aggregate-first instance query changes datasets; a navigation link names a missing source column, target view, target dataset or target column; or the permanent run schema stops containing all 18 frozen custom settings as numeric columns. This is referential-integrity coverage, not a data migration.

## Exact reviewed blobs

| Component | Git blob | Review role |
|---|---|---|
| `Datasets.java` | `2dfad3e175044300e5cfcf9cea1326a5161a1274` | Complete schema, statistics-sheet and 13 shipped-view read |
| `DatasetDef.java` | `a12fc0574602f4e9e0efa6f87401cd8a6c37f8f9` | Column uniqueness/defaults |
| `ViewDef.java` | `17acb966caaebf7c2acac6b6571e845ead048f81` | Initial/instance query ownership |
| `LinkDef.java` | `0da9b02d6755474a9d468ed697ecc7845a4e122f` | Navigation reference contract |
| `DatasetStore.java` | `b96b39de60d1e72fb93245d2f71a0b616e851fa7` | Catalog assembly and generated views |
| `Catalog.java` | `126535930994090260cc86d784085f48e0342129` | View-to-dataset resolution |
| `UimsdModel.java` | `2ab5db93adc6a56ff6fd7e97a7de56c6f697ae7c` | Link dereference and query construction |
| `WndUimsdRow.java` | `12fe6ab88d1845eaedb310fbf90949acec7b57f3` | Player-facing link availability |
| `DatasetBuildersTest.java` | `21e4e2b0aaf9677721a22e0266eca1da2a33566c` | All shipped views execute on real rows |
| `CustomRuleFullFieldAcceptanceTest.java` | `b436d59c10ab04a3ab5a605990be14e36c1f25ca` | Existing end-to-end 18-setting query/report/export coverage |
| `DatasetSchemaContractTest.java` | `7d195b673f7400a930b9049913d5fb9adf2f092a` | New structural contract |

## Test evidence

Core-only request `5eb573d98fb154b13e6f7d35a4cd95aac740882d` ran [workflow 38995668774](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38995668774) from 2026-10-17T02:37:45Z through 02:40:24Z.

- complete `:core:test`: 1,765 methods in 349 suites;
- failures 0, errors 0, skipped 0;
- both new methods passed in 0.002 seconds;
- artifact `12086749353`: 827,712 bytes;
- artifact digest `sha256:df6772b7fa738cb5c7bada7f799ce360ac2aee661ab179bc392387c701049ea8`.

The expected stderr from `LegacyImporterTest.theImporterNeverThrowsOnGarbage` remains a passing malformed-input fixture, not a new failure.

## Boundary and next step

This closes only the current `Datasets.java` schema/view boundary. It does not declare all UIMSD reviewed. The conservative 211-path checkpoint reconciliation had 96 exact current-blob machine entries before this review; this record adds one exact current production blob. Prose-only completed reviews still need conversion/reconciliation without re-reading finished work. Continue from the first genuinely unreviewed changed component and its integration callers.

No production application source, executable APK build request, APK bytes, release or publication changed. The schemas, rows, caches, raw histories, saves, original scores, approved 357-value v2.6R scoring, gameplay and UI behavior are unchanged. TEST263 remains verified/staged; TEST262 remains the latest actually published owner APK. No physical-phone or owner-save install-over claim is made.
