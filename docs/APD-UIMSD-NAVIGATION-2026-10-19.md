# UIMSD evidence navigation — TEST301 — 2026-10-19

## Completed milestone
Back now restores each inspected table's query, filters, sorting, page, horizontal column anchor and vertical offset. Independent evidence tables start at their own origin, even when parent and child use the same dataset. Scene recreation checkpoints and restores the surviving model's position. This completes the navigation-position milestone within capability 2; it does not complete the five-capability investigation system.

Application commit: [5c11b1fc346efd8f398ab2cf2872e03b15b29dc0](https://github.com/APD-Development/ShieldingShouldHaveWorked/commit/5c11b1fc346efd8f398ab2cf2872e03b15b29dc0). Core implementation first committed in 6293d7aa5fae159d98ddebdd10efa08545822ae0. TEST301 developer and player builds use the same application commit. Strengthened test-only source: d988d1308c46afc0315267c01a1afe08e0f99f06.

## Root cause
The navigation stack retained view/query/page, while the viewport lived in the disposable table cameras. Scene rebuilding transferred the outgoing viewport only when dataset and grouped/individual shape matched. Cross-dataset Back therefore lost the parent's position; unrelated places sharing a dataset could inherit the child's position. Scene recreation also discarded the cameras.

Each navigation place now has a unique transient token and an immutable viewport checkpoint. Before rebuilding or destroying a scene, its displayed table checkpoints against its own model/token. If navigation already occurred, the model routes that late checkpoint to the corresponding Back entry, never to the new evidence table. All link, group, research-evidence and seed-replay navigation uses the same stack helper. Back restores the saved query/page and viewport. New roots/catalogues invalidate obsolete tokens. Existing viewport geometry handles column anchors, removed columns, clamping and changed sort keys. Grouped/individual shape changes reset safely.

The earlier Columns touch bug had a separate input consequence of rebuilding: new background listeners could capture modal taps. The existing rule to defer dirty rebuilds while any window is open remains intact. The same table rebuild path now restores per-place navigation state. No popup-specific touch workaround was introduced.

## Files changed for this milestone
| Purpose | Files |
| --- | --- |
| Shared navigation state | core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/UimsdModel.java |
| Geometry/shape safety | core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/TableViewport.java |
| Rebuild and scene-destruction checkpoints | core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/UimsdScene.java |
| Player-facing Bug Fixes | core/src/main/assets/apd/apd_changes.txt |
| Six core regressions | core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/NavigationViewportTest.java |
| Actual-pointer navigation and frozen-pane assertions | android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/UimsdColumnsModalDeviceTest.java |
| Full suite and three extra captures | tools/run-codex-android-checks.sh |
| Exact produced APK replay | .github/workflows/codex-test301-navigation.yml; tools/run-test301-navigation.sh |
| Release tracking | docs/APD-NEXT-BUILD-HISTORY.json; docs/APD-CODEX-BUILD-REQUEST.txt; docs/APD-CODEX-CORE-TEST-REQUEST.txt; docs/APD-UIMSD-INVESTIGATION-CHECKLIST.md; this report, HANDOFF.md and WORK-COMPLETED-LEDGER.md |

The build comparison also preserves post-TEST300 reports, the corrected settings-gesture fixture and eight Ring of Wealth test methods. No Ring of Wealth/loot production code changed.

## Verification
- Core checkpoint 39332445287:1,973 unique methods /388 suites,0 failures/errors/skips; all six new navigation regressions passed. Actual XML and ZIP digest checked.
- Developer producer 39332706151 and player producer 39332706084 each passed all 1,973 core methods. Compiled history checks passed.
- Full API34 emulator suite 39332706151:51 unique methods,0 failures/errors/skips; every previous 50 method and all 73 previous captures retained. All 76 PNGs decoded. The new pointer case follows parent -> child -> a new parent place, uses visible Back twice, asserts original filters/sort/page/offsets, and recreates the scene. Its original full-suite execution took6.215s.
- Visual review found the initial fixture capture could precede synchronization of the frozen panes after a direct programmatic scroll. No application failure was suppressed: the full51-case run was already green. The retained case now waits for a frame and additionally asserts frozen header X and first-column Y, with the guide preference explicitly initialized for standalone execution. Its exact-APK replay result is recorded below.
- Exact TEST285 -> TEST301 cold restart 39332706102 passed: baseline defect reproduced, actual old disk state preserved on upgrade, newly saved manual layouts survived force-stops, regular-player settings worked and private feature asset was absent.9 PNGs decoded. Unsaved old TEST285 edits cannot be reconstructed.
- Actual death-order 39332706176: expected TEST287 baseline failure and corrected TEST301 pass; exact source/APK hashes checked. No outcome regression introduced.
- Display matrix 39332706221: all 5 profiles passed,25 PNGs decoded. First attempt had 2 passes/3 failures: compact and small profiles show Quickstep launcher ANRs; M53 hit a render-callback timeout whose cause was not established. Failed logs/captures remain retained. All 3 failed profiles passed unchanged-APK retries; no test assertions or gameplay code were changed to obtain those passes.
- These are API34 emulator results, not physical-phone verification.

Strengthened exact-APK replay 39335009747 passed 1/1 case with all original assertions plus frozen-pane alignment, no app rebuild, and 3 decoded/reviewed stable captures. Exact app SHA256 and unchanged protected application inputs were verified. Stable parent/restored tables show the same column anchor, vertical offset, selected sort marker and page. The three stable replay captures supersede the initial full-suite navigation captures for visual comparison.

## APK identity and compatibility
| Variant | APK | Bytes | SHA256 |
| --- | --- | ---: | --- |
| Developer | ADHD-Pixel-Dungeon-CODEX-TEST-301.apk | 38712703 | cb998bc22f52e9bc9526f0fca0a4eac7009357073fe63931e6dee24b8f9ca690 |
| Player | APD-Player-TEST301.apk | 38712534 | ad9d2ab9d8356e93dcb50c8491ee11d0a319fa3bf38d08ac6f450c648f737a4c |

Both signers:067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e. All 328 TEST300 history TSV files are preserved (existing records byte-identical, index rows retained); current 329 TSVs match across variants. Player assets/apd/features.txt is absent; the actual player settings/cold gate passed. In-game Changes includes the fix. Previous APKs were not overwritten. No public release or tester distribution. Both verified APKs were saved for private owner delivery.

No save/raw-log/query-codec/outcome schema changes and no historical migration. No events, zero values or outcomes were invented. Existing tables, saved views, filters, exports and accurate abandonment/death/victory/ascension behavior remain intact. Viewport checkpoints are session memory; they survive scene recreation while the model survives, not complete process termination.

Developer APK grew 643 bytes over TEST300; player grew 651 bytes. The six new core regressions took 0.015s aggregate on CI; this is test execution time, not a representative-history/phone performance benchmark. The change records no additional facts and writes no viewport state to player data. Broader recording/storage/latency profiling remains open.

## Partial and pending work
Capability 1 remains partial (including the accepted equipment-following damage window); general interpreted questions, correlated sequences and extensible terminology are unfinished. Capability 2 remains partial beyond this position fix: comprehensive statistic -> contributor -> run/floor/fight/turn coverage remains open. Capability 3 has a basic turn-browser foundation; full death context/resources/final-turn windows are unfinished. Capability 4 has A/B research foundations; named persisted experiments/future-run reevaluation are unfinished. Capability 5 comparable-condition longitudinal progress is pending. No uniqueness claim is supported.
