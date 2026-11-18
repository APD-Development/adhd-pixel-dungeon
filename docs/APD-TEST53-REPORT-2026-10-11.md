# APD TEST53 verification report — October 11, 2026

## Candidate and delivered fallback
Candidate source a5350f4837f098cc6ab2a7b0a508b42319cc7b4d, verification run38172769561. Full-source verification is pending; only appended completed evidence may certify this candidate. Delivered TEST50 remains the verified fallback, source14226785732470e5241daaabe92ba4dc7d399df3, APK SHA256ffa42ea87bcbbfa4ed7b600682aea49a703e90bce57f498b2bf36bccd2cd7c7f. No APK rebuilt just for this report. No physical-phone validation is claimed.

## Changes since TEST50
1. Missing main-menu ten-run notice after demo access: UimsdGate treated the database-unlocked flag as proof of ten genuine runs. Demo generation sets that flag too, hiding the instruction. Notice retirement now follows genuine-run tour eligibility; demo access remains available and demo runs still do not count. Normal builds keep their existing behavior. Screenshot alone does not establish the owner's exact saved state; the demo-state path was reproduced in controlled tests.
2. Run comparison arithmetic: subtracting two signed long values could wrap and reverse the B-minus-A sign. BigInteger now renders the exact report difference without changing stored values, ordinary +/-/zero formatting, hidden-information rules or unknown values. Extreme-value failures were controlled edge cases, not observed normal phone records.
3. Boss statistics: stripping nested class names at the first dollar classified YogDzewa's Larva/YogRipper/YogEye/YogScorpio minions as bosses. Classification now uses the actual nested simple name; all six real Yog fists remain bosses. Row-builder revision8→9 invalidates derived caches so existing raw history gets recalculated correctly. No enemy gameplay code was changed.
4. Row-cache allocation guards: tiny malformed caches with huge string/column/row counts caused OutOfMemoryError directly in RowCodec. Counts are now bounded by the remaining encoded bytes before allocating. No arbitrary history-size limit or format change. Important qualification: DatasetStore already catches Throwable and tries raw-log recovery, so an end-to-end phone crash was NOT established. This prevents dangerous allocation attempts instead of claiming a reproduced application-wide crash.

## Permanent regressions and actual results
RunComparisonDifferenceTest:4 new cases; baseline2 failed with reversed +/-1 values; after fix passed.
UimsdDemoNoticeTest:2 new cases; baseline notice test failed with empty text; after fix passed.
UimsdGateTest: existing simulated tenth-run fixture now sets the real tour flag; demo expectation updated to the owner's requested notice. This does not weaken unlocking/access assertions.
BossMinionClassificationTest:3 new cases covering four actual minions, six actual fists, old-cache rebuild and byte-identical raw logs; baseline2 failed, after fix passed.
RowCacheAllocationBoundsTest:6 new cases including three malformed allocation paths, current empty cache, unknown future section and raw-history-preserving cache recovery. Baseline3 direct decoder tests raised OOM in a32MiB JVM; after fix passed.
New Android test demoUnlockKeepsTenRunNoticeVisibleOnTitle checks the actual rendered TitleScene, full instruction text and horizontal bounds after demo flags. It restores preferences and never deletes history; Android execution is pending.
Targeted host suites:22 report checks,43 gate/progression/settings/layout checks, and43 cache/boss/dataset checks passed. Counts overlap; do not add them as unique coverage.
Intermediate TEST51 source dd6ce8b3e048d7890915098b118d66ded6208c95 passed actual1,095 core tests with zero failures/errors/skips and debug assembly. Its API34 checks were still running at report preparation. It excludes the later boss/cache fixes.
TEST52 was pending and GitHub replaced that pending run when TEST53 queued. No TEST52 build/test result exists, and the active TEST51 run was not cancelled. No failed or pending APK has been called verified.

## Risks, compatibility and required phone tests
Install over the existing APD app; never uninstall or clear data. Application ID/signing key/save formats/raw logs unchanged. No RNG, combat, balance, Cleric restriction, server activation or monetization changes.
Derived caches will rebuild once after revision change, which may make the first UIMSD opening slower on a large history. Raw player data remains authoritative and preservation tests passed.
Demo-unlocked players now retain the pink ten-run notice before genuine progression; verify it fits the owner's phone, disappears at the correct stage, and the tenth-run Clean/Adult prompt retires normally.
Test queue/history/filter/sort/export navigation after updating, then save/quit/resume a normal dungeon run. Emulator checks do not prove these behaviors on physical devices.
Future community boss classes still require recorded boss metadata or an explicit classification addition; no arbitrary new-content support is claimed.

## Files and commits
dd6ce8b3e048d7890915098b118d66ded6208c95: report difference and ten-run notice, core tests, Android rendered-menu regression.
9f16eca2b29e70ac6affbf6a058696976a428ff8: nested boss classification, derived cache revision and preservation tests.
e21642b6f75ba4d6054f9ece3c2a3304e152f921: full-source request after boss fix.
a5350f4837f098cc6ab2a7b0a508b42319cc7b4d: cache count bounds,6 regressions and full-source request.
Production files: core/.../apd/report/ReportBuilder.java; core/.../windows/UimsdGate.java; core/.../apd/query/BossClasses.java,RowBuilders.java,RowCodec.java.
Tests: RunComparisonDifferenceTest,UimsdDemoNoticeTest,UimsdGateTest,BossMinionClassificationTest,RowCacheAllocationBoundsTest and android/.../UimsdRestoreDeviceTest.java.
Shared handoff/build request updated on the approved branch; main untouched.

## Unresolved findings and timing
The UIMSD comprehensive audit is incomplete:65/423 manifest components reviewed, not a completed zero-bug certification. Suspected merger false-value provenance, large-file memory, same-size cache staleness and concurrent cache/index writes remain to reproduce.
Three inherited SPD save utility findings remain unpatched under the owner's read-only instruction; see docs/SPD-READ-ONLY-FINDINGS-2026-10-11.md. No claim that every save failure path is safe.
This active turn resumed development after the owner's message; the preceding hours were not background coding. Tool/CI timestamps are evidence of executed operations, not a measured continuous-active-work or zero-downtime total. No persistent background AI worker has been demonstrated.
A first local patch command used an unsupported delete/add pair for the same path and was rejected before changing the file; corrected to a focused update. An attempted read used the wrong workflow filename, then read the real codex-sprint-check.yml. These were setup errors, not application test passes.
