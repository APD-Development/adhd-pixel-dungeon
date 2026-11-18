# TEST112 — exact acknowledgement of provider-neutral preference edits

## Confirmed model bug
SyncReplica.acknowledge compared timestamps and versions but not the actual content submitted. If a value was edited or deleted again in the same millisecond, an acknowledgement for the older value could clear the newer edit's dirty state. A tombstone replaced in that same millisecond had the same issue. An acknowledgement carrying another device's record could also clear it.

Require equality of value, deleted state and device identity, in addition to the existing timestamp/version guards, before marking the current record acknowledged. Valid matching acknowledgements still clear pending state. Existing conflict policy, tombstone behavior, file formats, limits and original player records remain unchanged. No network provider is enabled. There are currently no production SyncReplica callers; this is a provider-neutral foundation correction, not a claim of current player settings loss.

## Permanent regressions and verification
Four new methods reproduce same-millisecond edit, delete, tombstone replacement and mismatched device identity. The first three verify actual file save/load retains the pending updated record and a later correct acknowledgement succeeds. All4failed on the original implementation in0.060s. Corrected new+existing SyncReplica/collection tests:13unique methods passed0.079s. No assertions weakened. Full application checks remain pending: expected1316core methods,21API34 instrumentation methods,22validPNG and successful debug build. No physical-phone test or verified TEST112 claim.

## Delivered fallback and hourly report
TEST111 is actually privately published08:45:26UTC (04:45:26EDT), source976c1c2899d37d687f9978da02193c0f4543f5bd, verification38369863635 (1312core/21API34/22validPNG,0failed/error/skipped), delivery38370018678. APK SHA25675fee72653a9e191dac8c082cd8418d673485de5400ec42a3b0b409b40d155ec. Signer/appID match TEST67, publication app-input tree equals tested source. Keep111 as fallback until112 passes independently.

Owner-requested8pagehour-by-hour EDT PDF published08:53:42UTC at apd-overnight-report-2026-10-13-0450-edt. Report-only run38372382232 succeeded, commitf4d62c66ee53e1622ba0bba2022d633c2da677c4. No application rebuild for PDF. Its04:50EDTcutoff states the then-local112fix was not in111, retaining accurate evidence. No asset overwritten.

The comprehensive UIMSD audit remains incomplete;128/211locally reviewed scoped components plus pending integration hooks. Committed111checkpoint109 is an earlier honest snapshot. Unmatched rollback uncertainty and other approved backlog tasks remain open. No persistent background coding worker configured or demonstrated. No invented active-execution hours or downtime total.
