# Daily calendar score correction — October 16, 2026

## Reproduced defect
A real headless daily-run submission with Enemy HP500% recorded original133 and earned266, but the calendar's dailyScoreHistory entry was133. WndDailies rendered that old integer field directly. The exact old probe failed expected266/actual133; corrected lookup returns266. This is a display/persistence consistency defect, not a multiplier calibration change.

## Correction and compatibility
Keep original integer daily history and its original bundle arrays unchanged for compatibility. Store an optional versioned pair of date/long earned-score arrays for new records. Display the frozen latest daily authority or saved earned value, falling back to original scores when older history has no recorded adjustment. Recover the most recent TEST249/250 daily from its existing frozen record without applying today's preferences, and preserve that value when later dailies replace the latest-record slot. Missing, mismatched, orphaned or negative optional metadata cannot replace original entries. A long calendar value moves below its date when needed.

All357 values, the scoring formula, challenge factors, gameplay rules and original scores are unchanged. Older custom daily days whose adjusted values were never stored and whose detailed record is no longer retained cannot be reconstructed; retain their existing original values rather than guessing. No original history is deleted, migrated destructively or repriced.

## Regression evidence
Five new permanent core methods cover actual daily submission/save/load, long values above integer range with normalized daily dates, legacy score preservation, recovery of the latest frozen record after another daily, and malformed optional metadata. An initial local implementation failed two legacy-loading tests because missing Bundle arrays return null; this was fixed before remote publication. Sixteen relevant methods then passed locally, including existing score-award and357-value regressions. New Android coverage renders a large earned score, checks date/value bounds, scrolls and uses Android Back.

Full1734core/debug/history/36API34/48PNG gate is pending at this source checkpoint. Compare the actual preceding produced TEST251, even if its later Android checks fail; producing an APK is not proof of playability. The published verified TEST250 remains the fallback. Physical-phone install-over, existing-owner-save continuity and human boss playthroughs remain unverified.
