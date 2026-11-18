# TEST181 UIMSD scope-wording candidate — 2026-10-15

## Scope and cause
At live parent `1f52c04d9383bfd6d7a711315e6bcc848c85f74d`, the empty Home said to tap Rules for “other runs” but did not explain that Rules selects rule sets only. A run still in progress is controlled separately by Tables > Options > Include runs still going. Source inspection also confirmed an abandoned run emits RUN_END and is therefore finished; it must not be described as unfinished.

## Implemented
- `UimsdHome.emptyMessage()` provides testable singular/plural empty-state wording and keeps different rules separate from run status.
- `UimsdScene` renders that message instead of duplicating weaker inline text.
- `WndUimsdOptions` states that Rules does not change whether a run is finished and points unfinished runs to the dedicated switch.
- The Home scope note includes runs still going among the normally excluded categories.
- `UimsdHomeTourTest.emptyHomeSeparatesRulesFromRunStatus` covers empty history, one finished abandoned run under different rules, singular wording, unfinished-run navigation and all-rules inclusion.

## Verification state
TEST182 exact source `eed8bb85dfb32cffded60a08614cf3223362fb50`, run `38659050102` (00:16:09Z–00:29:24Z), passed 1,603 core tests, debug assembly, immediate-previous TEST180 history, 26 API34 tests and 34 valid 320×640 screenshots. The new regression passed in 0.139 seconds. APK SHA-256 is `ed13da85121f7cd13feeb751050912beba9e09f47f4b6c6867670cfc7f90e7df`.

## Boundaries
No raw history, save, statistics calculation, scoring, gameplay, application ID, signing identity or player data changed. No APK was delivered and no physical-phone result is claimed.

## First hosted attempt
TEST181 source `ba146f3c0ec2e145fa57295af2c398248f497d2a`, run `38658816290`, failed the reviewed-history guard before compilation because the manifest omitted `docs/APD-TEST179-UIMSD-COLUMN-RESTORE-2026-10-14.md`, which is part of the intervening TEST180 evidence commit. No tests ran and no APK was produced. TEST182 corrects only that manifest omission; application/test source is unchanged and full verification remains pending.

## Evidence
Core artifact `11938107765` / ZIP SHA-256 `df2c8d34889cb9f387b97cfabeaebc962445613d191e73a1edffa3cb9c44567f`; APK artifact `11938012933` / ZIP SHA-256 `7bb738e8efa77e6b462e720fee3c0a41d6d91fc7b6b482cb7236e6f1cbdec6d1`; Android artifact `11937863231` / ZIP SHA-256 `aabb3a0ac3246d7573dfbc9d1eef90893653ddd5c6c67c9820ed3244fc2a58ce`. Contact-sheet and focused UIMSD Home/APD Changes review found no blank, corrupt or visibly clipped screenshot. Private delivery was skipped; no physical-phone result is claimed.
