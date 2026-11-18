# Follow-up: conservative rounded aggregate bounds — October 15, 2026

Continued review after the private TEST204 delivery and scaled-unit candidate TEST205 identified a separate correctness edge. AVG/MEDIAN used nearest rounding even for a lower-bound result: a known minimum2.46 became >=2.5, which claims more than is known. Negative values have the same issue. This affected ordinary and scaled displays; no raw statistic was altered.

Added the fifth ScaledAggregateDisplayTest method: both mean and median, scaled and ordinary positive bounds, negative bounds, and unchanged nearest rounding for exact results. Before correction:4methods pass/1fails (expected>=2.4,actual>=2.5). Corrected lower-bound presentation uses FLOOR at the existing one-decimal precision; normal exact-result rounding remains unchanged. Final40focused methods pass0.302s and623broader pass27.964s, with baseline121source-guard limitation.

Production e77a1c47 and regression6e93ca2c. No query-math, history, save, cache, scoring, RNG, UI framework, gameplay or storage-format change. TEST205 exactsource4ddd6e415247bccb60e30d8af8f4b92335219802/run38751801184 completed its existing gates successfully but is not delivered; this added boundary case was found after that source cut. Keep the actual delivered TEST204/sha0377d2fafe89ba6ed4741e3df18e574b46db4ff41377c75a53432e6294fb6ce3 as owner fallback. Next complete gate expects1636core/debug/28API34/38PNG against immediate preceding producedTEST205, preserving each build's exact history. Do not claim this follow-up is in205 or in delivered204.


## Full source verification

Exact source `f33591d364598f75b07d62394f5a47bf6ba6432e` passed workflow [38754451109](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38754451109), which started at 2026-10-15T14:46:51Z. Independent artifact inspection confirmed:

- 1,636 core tests in 324 suites and 28 API34 tests in 12 suites; zero failures, errors or skips.
- Debug assembly and complete immediate-previous compiled TEST205 history passed.
- Core artifact `11982772591` (GitHub digest `2f7c5cd63801552ef932d5c476bb88a43feb443d3a663e336c5645ba2d2d8006`).
- APK artifact `11982687643` (GitHub digest `158cc8779378cd1d7268416697706fb2f29fed35a9d5078548d4a3c8962fa598`); internal Codex TEST206 APK SHA-256 `566e29174c17c19f9a19f7429410d872a097c09c2aa4a1e4835548617a6c809c`.
- Android artifact `11983063448` (GitHub digest and independently downloaded ZIP SHA-256 `56329f954587e00a1abfee8a54b635688ebd60dc2262e2afc0b643797ecbc13a`). Its XML timestamp is 2026-10-15T14:58:54Z.
- All 38 screenshots are valid 320×640 PNGs; the complete contact sheet was opened and reviewed clean.

Private delivery remained skipped. This is API34 emulator/source evidence only, not physical-phone, owner-save, release-signature or owner-delivery evidence. Owner-delivered TEST204 remains the playable fallback. Raw statistics, history, cache/save formats, scores, slider limits, gameplay and RNG remain unchanged.
