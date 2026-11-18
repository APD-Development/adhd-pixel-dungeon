# APD malformed run-start fact recovery — 2026-10-16T05:39:30Z

## Confirmed defect
A checksum-valid typed `RUN_START` with an unreadable payload set `RunFactsBuilder.started` before decoding. A later readable start in the canonical timeline was therefore skipped, permanently omitting recorded run identity, class, seed, difficulty and all 18 Custom Dungeon Rules values from UIMSD-derived facts. The malformed raw frame itself remains retained.

## Bounded correction
`RunFactsBuilder` now claims the first start only after `LifecycleEvents.decodeRunStart` succeeds. A later readable start may recover setup facts after malformed input, while any already decoded start still prevents later segments from relabelling the run. The existing exception path marks count facts incomplete; it does not invent missing values or rewrite history.

## Retained acceptance
`malformedFirstStartDoesNotHideLaterReadableStart` writes a malformed typed start followed by a valid Cleric start with an explicit seed, difficulty and the frozen 18-rule snapshot. Exact TEST234 source must fail the method. The corrected source must retain the readable metadata, the default upgrade-scroll count of 15, and incomplete provenance for event counts.

Expected candidate gate: one intended TEST234 failure, then 1,698 corrected core methods, debug assembly, complete immediate-previous TEST234 APK history, 33 API34 emulator methods and 42 valid screenshots. No publication, tester distribution, physical-phone result or owner install-over/save claim is authorized by this record.

## Verified result
Exact TEST234 reproduced the one intended loss-of-facts failure. Corrected TEST235 passed 1,698 core methods in 335 suites, debug assembly, complete TEST234 history, and 33 API34 methods. The first Android artifact was rejected because one screenshot was malformed despite the methods passing; the Android-only rerun passed 33 methods and all 42 320×640 captures passed manual review. APK SHA-256 is `6b2de5d8906d01114b01521ea2eeef6951d7b50c73f9076be05a7b9693782e27`; application ID and v2 signing certificate match TEST234. No publication or physical-phone claim.
