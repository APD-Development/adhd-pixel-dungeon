# TEST302 exact UIMSD question persistence — verification record

Recorded: 2026-10-19T23:28Z

## Scope

TEST302 persists a friendly UIMSD question beside its executable query and restores the exact measure, measured column, per-unit choice, split fields, filters, run scope and hidden-information choice. It preserves legacy three-field saved views and fails closed for unreadable future optional question bytes without discarding the executable table query. No gameplay, balance, RNG, save, permanent raw-history, scoring, signing or application-ID behavior changed.

## Source chain

- Production implementation: `343908aedf7c4b3d9675b98a84381fa2127c77f2`
- Core request: `8db2769320afe4826cbd3249de790327ba83bfe4`
- Compile-only test-fixture correction: `3c73c7c06c36a912e4d8b736a847b42078c3a410`
- Native actual-pointer acceptance and player-facing Changes: `c8614a8034b5065cba52456315d7d0898629845f`
- Exact TEST302 request/history head: `49b85e20b6eb5ec9152d988d304788cf574d78b2`
- Immediate previous compiled candidate: TEST301 application source `5c11b1fc346efd8f398ab2cf2872e03b15b29dc0`, producer `39332706151`

The first core request run `39336355700` failed only because its new fixture used nonexistent `Op.GTE`; no application behavior failed. The corrected exact-source core run `39336534735` passed 1,978 methods in 389 suites with zero failures/errors/skips.

## Acceptance evidence

| Gate | Result |
| --- | --- |
| Main producer | Run `39337392637`: verify/build success; Android smoke success; private delivery skipped |
| Core | Artifact `12227726158`: 1,978 tests, 389 suites, 0 failure/error/skip |
| API34 native | Artifact `12228017964`: 52 methods, 0 failure/error/skip |
| Visual evidence | 77 valid 320×640 PNGs; new saved-question capture manually reviewed |
| Regular player | Run `39337392681`: success |
| Exact death order | Run `39337392672`: success |
| Cold restart/settings | Run `39337392762`: success |
| Five display profiles | Run `39337392510`: all five terminal successes on the exact APK |
| Physical handset | Not run; no physical-device claim |

The native `exactQuestionSavesAndReopensThroughNativeMenus` case uses actual pointer events through Answer, Save this question, the naming dialog, Options, Open saved view and Open question. It verifies the restored friendly selection labels rather than only testing the codec.

The display run retained earlier failed attempts. The 960×320 evidence visibly showed Android's Quickstep launcher ANR over a correctly rendered APD title; placeholder files were correctly rejected as non-PNG. Failure movement between unchanged profiles and later same-source success establish emulator/launcher flakiness, not a source change. Successful 320×480, 360×560, 720×720, 960×320 and 1080×2408 title captures were decoded and manually reviewed.

## APK identity

| Variant | Artifact | Size | SHA-256 | Library |
| --- | ---: | ---: | --- | --- |
| Developer | `12227705981` | 38,713,394 | `e0397418b9ed0a91ac05a4b29f4e4696cc5dee48cb770ce9ea49458b867d89fa` | `libfile_698f55d110e881919323e9d2520065d9` |
| Player | `12227901308` | 38,713,217 | `055d652ff2dcb21eb343b17fff8eb8bb4368d29c5fc78234d3b4c5e8fd985de5` | `libfile_592c7a587e1c819190e523d6fc6429e6` |

Both variants embed 330 identical reviewed history TSVs. Player provenance binds it to the same source and developer digest; player correctly excludes `assets/apd/features.txt`. Existing signer/application identity is preserved.

## Release state and next step

No public/private release publication, staging, automatic tester distribution or owner-phone acceptance was performed. The newest actually published owner APK remains TEST262 from 2026-10-16. TEST301 remains the immediate verified fallback.

Do not rebuild unchanged TEST302. Continue the tracked UIMSD checklist from the next still-partial milestone. A later application build must compare against TEST302 application source `c8614a8034b5065cba52456315d7d0898629845f`, producer `39337392637`, preserve all 330 history TSVs and include this report plus the evidence-only handoff/ledger commit in strict history review.
