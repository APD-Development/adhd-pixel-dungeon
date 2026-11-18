# Native UIMSD charts

Status: verified. Both matched TEST292 APKs are saved for delivery.

Options > Charts opens a scrollable bar chart of the authorized grouped query result. Choose count or an aggregate already offered by the table; tap a bar for the same group detail sheet as the table. The chart preserves scope, filters, ordering, disclosure and provenance. Counts and aggregates retain their own formatted text, unknown values have no bar, lower bounds retain their marks, and small samples are explicitly labeled. The display caps at 24 groups and states the omission count without manufacturing an Other group.

Geometry uses a shared zero baseline, supports negative values and normalizes before subtracting to avoid extreme-span overflow. The selected measure has a wrapping heading; all group labels, values and details wrap inside the clipped native ScrollPane. The restore hook retains the selected measure and clamped vertical offset. Group clicks reject stale result identity before opening details.

Four new core tests cover signed geometry, zero, extreme values and invalid/unknown inputs. The existing five chart-model tests remain. New Android acceptance opens Charts from Options through native pointer input, changes measures twice, checks lower-bound and unknown results, records two screenshots, restores scroll and taps a scrolled group. It asserts query identity throughout. The complete core and API 34 suites passed; no physical-phone claim.

No gameplay, score, raw-history, saved-view or save-format changes. TEST291 remains the verified fallback. The TEST292 pair retains all previous Changes entries, signing and application identity.

## Verification and APK identity

Application source: `e9f028a35285d919258088cd5360d954ceb429f4`.

- Complete developer and player core suites independently parsed: **1,917 unique tests, 378 suites, zero failures/errors/skips**. This includes all five existing UimsdChartTest methods and four new ChartBarTest methods.
- Android: **44 unique tests passed, zero failures/errors/skips; all 59 PNG captures decoded successfully.** Both chart images were visually reviewed, including the negative-value baseline, unknown blank track, lower-bound mark and count bars. All 43 prior Android case identities are retained. The full gate retains all previous tests plus the real-pointer chart acceptance method and requires both new chart screenshots.
- Exact-APK actual-death before/after comparison passed again against TEST292; TEST287 still reproduces the original premature closure.
- Both APKs retain signing certificate SHA-256 `067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e`; workflow checks retain package and version-code identity. Developer feature asset is present only in developer variant.
- Both variants contain 325 identical compiled history TSVs. All prior TEST291 per-build files are byte-identical, and its index rows are preserved with the new build row added.

| APK | Bytes | SHA-256 |
| --- | ---: | --- |
| ADHD-Pixel-Dungeon-CODEX-TEST-292.apk | 38,658,999 | `88a87eccd6271aa51665934398d475b46f52d289c273318b764bdeaf80793f33` |
| APD-Player-TEST292.apk | 38,658,830 | `7f05925cdabea13a3e4db73bad37e223ff0d8b6a84ac8386d77d975cd8258522` |

[Main build, core and Android evidence](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39309774810) | [Matching player build](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39309774623) | [Exact-APK death regression](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39309774763).

No physical handset verification or separate player emulator run is claimed. No public release or tester distribution was performed.

## Changed application and regression files

- `apd/query/ChartBar.java`: signed zero-based geometry with finite input checks.
- `windows/WndUimsdChart.java`: scrollable chart, measure picker, group details and restoration.
- `windows/WndUimsdOptions.java`: Charts entry.
- `apd/query/UimsdGuide.java`: chart usage topic.
- `core/src/main/assets/apd/apd_changes.txt`: in-game chart entry.
- `ChartBarTest.java`: four geometry regression methods.
- `UimsdChartDeviceTest.java`: native chart menu/measure/scroll/restore/detail acceptance.
- `tools/run-codex-android-checks.sh`: retains existing cases and adds the chart class and two required captures.
- Build history/request and handoff/ledger documents record this candidate and its preceding verified build. Exact-source Android review passed.

Supplemental title-display matrix: three profiles initially passed. Short landscape failed before emulator launch due a corrupt SDK download; smallest display failed while Android Quickstep launcher displayed an ANR dialog (confirmed screenshot and logcat), not in the chart. Both failures are retained. The same APK passed both retried jobs without application or assertion changes. All five title-display profiles completed successfully: 320x480, 360x560, 720x720, 960x320 and 1080x2408. These supplemental checks exercise title controls; chart interaction and screenshots are covered by the full API 34 gate. [Matrix evidence](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39309774613).

[Application and test changes](https://github.com/APD-Development/ShieldingShouldHaveWorked/compare/d6f714c4d6871779397373976d0a6bed0998e38c...e9f028a35285d919258088cd5360d954ceb429f4).
