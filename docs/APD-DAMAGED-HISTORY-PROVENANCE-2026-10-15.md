# Damaged-history count provenance correction — 2026-10-15

## Scope

A damaged telemetry log can retain a valid event prefix. Before this correction, RunFactsBuilder and RowBuilders treated counts derived from that surviving prefix as exact whenever the log had no explicit GAP event and did not start partial. TRUNCATED, CORRUPT, and SEAL_MISMATCH histories could therefore expose lower bounds as exact values.

## Correction

- Count provenance is incomplete whenever ReadStatus is not clean.
- Clean UNSEALED histories remain exact.
- Valid SEALED histories remain exact even when unrelated bytes trail the seal; the reader intentionally keeps status SEALED and reports the extra damaged bytes.
- RunFactsBuilder derivation version advances 8 to 9.
- RowBuilders cache version advances 12 to 13.
- Raw logs, saves, score values, rules, gameplay, RNG, slider values, and cache formats are unchanged. Only disposable derived caches are invalidated and rebuilt.

## Retained regression coverage

Six methods cover clean unsealed history, truncated tail, corrupt tail, seal-count mismatch, valid seal with trailing junk, and both cache-version contracts. Each damaged case asserts both the run-level ITEMS_ACQUIRED fact and the item-row item.acquired cell use incomplete provenance while preserving the surviving count.

The handoff records the earlier four-method reproduction as three failures before the fix. This integrated six-method suite and the full 1,645-core/debug/API34/history/visual gate are pending on candidate source creation; no result is claimed in advance.


## Actual full-suite failure and corrected fixture
TEST211 source fcc100ba56af247b2e15a3f406e49d5bd5eb082b / run38771457001 executed1,645unique core methods:1,644passed,onefailed,zeroerror/skip. Allsix new provenance methods passed. The failure was QuestBossClassificationTest.oldBuilderTenCacheIsRejectedAndRebuiltWithoutChangingRawEvidence: two assertions still required the preceding row-builder version12, while the approved provenance correction deliberately invalidates old caches with version13. No APK or Android test was produced by211. Failedcoreartifact11991716991 was independentlydownloadedandparsed,matchingSHA2560d6087827629f7702e454cc1ee8a30c90df70a14359ac287e0aaec145ff4319d.

Corrected both current-version expectations to13, preserving the synthetic version10cache, rejection/rebuild assertions, boss classification and byte-identical raw-evidence checks. No assertion was removed or weakened. Added a seventh provenance method that actually corrupts a frame, recovers a later item frame via resync, asserts the skipped bytes and recovered event count, and retains incomplete provenance in both builders. The revised provenance suite plus allthree classification methods passed10tests in0.297s locally. Existing local631-suite evidence precedes this strengthened fixture; fullcurrent-source verification remainsrequired.

## Previous source verification independently reviewed
TEST208 source13e4c03baad7e4471a172aedf7aff4222ca7e749/run38763307954 completed successfully. Downloadedcoreartifact11988175900 matched33e1a941911dd1f775a8f86136adb5aa832e075b16ae2f6a5420058ec1d5f96b;Androidartifact11988237436 matched32d7d792c505822e05ba1c8ab0064156b61be144b558f8eb195a7ab871c4ad3b. IndependentXMLrecount1639unique core+28uniqueAPI34,zero failures/errors/skips.38valid320x640PNGs;fullcontactsheetopenedandreviwedreadable. This supersedes the earlier executor's artifact-unavailable note, not its truthful retrieval failure. No208APKpayloadhash/signaturecomparison/publicationorphoneclaim. Published206remainsdeliveredfallback.

The next meaningful full retry requires1,646core/debug/completeimmediate-previousproducedTEST208history/28API34/38PNG. Preserve allinterveningdelivery/reportpaths; do not trigger another full run merely to correct prose timestamps or restate evidence.
