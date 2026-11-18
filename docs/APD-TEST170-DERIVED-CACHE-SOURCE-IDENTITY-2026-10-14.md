# TEST170 — Derived cache raw-source identity correction

Started: 2026-10-14T20:38:38Z

## Confirmed defect
`DerivedStore.fingerprints()` recorded only source kind, filename and byte length. A valid raw log replaced by different valid content of the same length retained the same cache identity, allowing stale derived statistics to be returned.

## Bounded correction
- Fingerprint every run and legacy source with its actual bytes: length plus SHA-256.
- Use the bytes actually read for the recorded length.
- Make an unreadable source fingerprint deliberately non-reusable so a failed read cannot establish a trusted cache.
- Bump derivation version from 7 to 8 for one automatic rebuild.
- Do not rewrite or migrate raw logs, player saves, scores, signing identity or application ID.

## Retained regression
`aSameLengthRawReplacementInvalidatesTheCache` creates and caches one valid run log, replaces it with a different valid equal-length log under the same name, and requires stale-cache rebuild, replacement facts, then normal cache reuse.

## Verification state
No real execution workspace was available in this invocation, so no local command/test result is claimed. Exact-source TEST170 requests 1,586 core tests, debug assembly, complete history against immediate previous compiled TEST169, 26 API34 emulator checks and 34 screenshots. The candidate is unverified until that run completes. TEST169 remains the verified fallback; TEST123 remains the owner-delivered/signature-compared fallback.

## Verified result — 2026-10-14T20:53:32Z
- Exact source `8da173025d4ab64fea123ee03f9afdbd4efc02f1`; [TEST170 run `38634072761`](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38634072761) ran 2026-10-14T20:40:35Z–20:51:50Z and concluded success.
- Core artifact `11926858030` (ZIP SHA-256 `d0ea132583f074fe7661cecb11fa0c9c2aa6255d02e15be7b87297d6c660799e`) contains 1,586 tests, 0 failures, 0 errors and 0 skipped. `DerivedStoreTest` has 13 passing methods; the new equal-length replacement regression passed.
- Debug APK assembly and complete reviewed history against immediate previous compiled TEST169 passed. APK artifact `11926349387` (ZIP SHA-256 `b1168659dedee4441b448591275ead4cd0f3beb83c46bfb0d96d98b158c81ddf`) contains `ADHD-Pixel-Dungeon-CODEX-TEST-170.apk`, SHA-256 `e9117959406a5893069e6f90f626386f335daa2f9b7f3c91c3ef3dd3ecb2e43c`; its bundled checksum matched.
- Android artifact `11927293898` (ZIP SHA-256 `774f8999ff0efe1d3491036f1a26d2a3fe20cb062a9dd9f60fde45f726da2b6c`) contains 26 API34 emulator tests with 0 failures/errors/skips. All 34 PNGs decoded at 320×640; contact-sheet review found no blank, corrupt or visibly clipped capture.
- Verified behavior: a different valid raw log with the same filename and byte length invalidates the derived cache and exposes the replacement facts; unchanged sources still reuse the cache. Version 8 causes one automatic derived-cache rebuild. Raw logs and player saves remain untouched.
- Private delivery was skipped. This is CI/emulator evidence only, not a physical-phone claim. TEST123 remains the last owner-delivered/signature-compared APK.

