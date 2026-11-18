# TEST171 — RunIndex cross-instance save serialization

Started: 2026-10-14T20:58:00Z

## Confirmed defect
`RunIndex.save()` used an instance monitor, but separate RunIndex objects targeting the same root all opened `index.apdi.tmp`. Concurrent saves could truncate one another's temp stream and race rename/delete fallback operations. The index is derived and rebuildable, but the race could cause avoidable rebuilds or save failures.

## Bounded correction
- Serialize only the small file-writing and atomic-replacement section across RunIndex instances.
- Preserve index format 2, entry ordering, CRC, load/rebuild behavior and the existing temp filename.
- Do not change raw run logs, player saves, application identity, signing, gameplay or scoring.

## Retained regression
The controlled two-instance test pauses the first writer after opening the temp file, starts a second instance, verifies it cannot open the shared temp concurrently, releases the first writer, and requires both saves plus a complete final second index that loads without rebuild.

## Verification state
No real local execution workspace was available. Exact-source TEST171 requests 1,587 core tests, debug assembly, complete history against immediate previous compiled TEST170, 26 API34 emulator tests and 34 screenshots. Candidate remains unverified until actual evidence passes.

## Verification result
Completed: 2026-10-14T21:08:41Z

Exact source `169d2bec53968f191286e3db6cb77efe7661ef06` passed GitHub Actions run `38636484010` from 2026-10-14T20:58:00Z through 2026-10-14T21:08:41Z.

- Core artifact `11929355257`, ZIP SHA-256 `985ea8ff5e84b41f56671f82ee4c497f400b8eb5aeb3aab37bf3706c0f60f37`: 1,587 tests, 0 failures, 0 errors, 0 skipped. `separateInstancesSerializeTheirAtomicSave` passed in 0.204 seconds.
- Debug assembly and compiled-history validation against the immediate previous compiled TEST170 APK passed.
- APK artifact `11927959848`, ZIP SHA-256 `3e7e5319995a789c71d738816634861f901af72a0d3165e436195ed865d7594f`: `ADHD-Pixel-Dungeon-CODEX-TEST-171.apk` SHA-256 `e5038b2055ca71f7cf0b432d05f1d4e70dddbc8b38325ffd28e1934288c23c22`; bundled checksum matched.
- Android artifact `11929576415`, ZIP SHA-256 `6547a27802d04e527b019cc4fef6ff9b7fa4ace66e1ea724d71d06638f4f5eff`: 26 API34 emulator tests, 0 failures, 0 errors, 0 skipped.
- All 34 screenshots decoded at 320×640. Contact-sheet review found expected readable history, rules, score, UIMSD, stairs, tiles and gameplay states, with no blank, corrupt or visibly clipped capture.

This verifies the RunIndex change at the exact TEST171 source. Private delivery was skipped, and no physical-phone or owner-save claim is made. TEST123 remains the last owner-delivered/signature-compared APK.

The approved branch advanced after TEST171's source to `c684d5070fc26bca6cc2d34b414ea18ecbefbb49`, adding independent Crush splash scaling. That newer application source was preserved and is not covered by TEST171 evidence; it requires separate exact-source verification.
