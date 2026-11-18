# APD archive empty run-identity recovery — 2026-10-16T08:35:41Z

## Confirmed defect
A syntactically decodable `RUN_START` with an empty run ID was treated as the authoritative typed start. It could hide a later valid logical run identity, causing export to label the log with its physical header ID, reject a correctly labelled import, or accept an import labelled only with that header ID.

## Bounded correction
Export and import now claim a typed start identity only when its decoded run ID satisfies the existing `StatsStore.isValidRunId` contract. Empty or invalid identities are skipped while scanning for the first valid typed start. Raw bytes, event order, finished-state detection and valid existing archives are unchanged.

## Retained acceptance
Two `ArchiveRoundTripTest` methods cover export labelling and both import decisions. Exact TEST237 application source must fail both methods. Corrected source must pass all 1,705 core methods.

Expected candidate gate: two intended TEST237 failures, then 1,705 corrected core methods, debug assembly, complete immediate-previous TEST237 APK history, 33 API34 emulator methods and 42 valid screenshots. No publication, tester distribution, physical-phone result or owner install-over/save claim is authorized by this record.

## Verified result — 2026-10-16T08:51:20Z
Dedicated run [38874924003](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38874924003) reproduced exactly the two intended failures on unchanged TEST237 source and passed the corrected 1,705-method core suite. Full TEST238 run [38874923900](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38874923900) passed 1,705 core methods in 335 suites, debug assembly, complete immediate-previous TEST237 history, and 33 API 34 emulator methods with 42 valid 320×640 screenshots. Independent artifact recount, app-crash scan and complete contact-sheet review passed.

The private workflow APK is 38,588,580 bytes with SHA-256 `e0d657026c7889806b669e9032ed0b6b7992b081756baefb48820d36ef1c9025`; its `.indev` application ID and v2 certificate match TEST237. This is a CI-verified candidate only: no publication, tester distribution, physical-phone result or owner install-over/save acceptance occurred. TEST231 remains the latest privately delivered fallback.
