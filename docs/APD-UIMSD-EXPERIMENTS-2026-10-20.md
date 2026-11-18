# TEST303: named personal experiments

Date: 2026-10-19 ET / 2026-10-20 UTC. Private implementation branch: `codex/apd-phone-fixes-2026-10-09`.
Application and executable build source: `7505a36ae1567458ba854db9427196a893824806`.
Previous accepted milestone: TEST302 exact question persistence.

## Completed milestone

Research lab > Saved experiments now keeps a named question, exact population A/B definitions and a numeric or win-rate metric. Capture the two populations using the existing research pins, choose Save named experiment and a metric, and enter a name/question. Reopen the saved definition to view recalculated results, open either population, inspect the calculation's contributors, rename it or delete only its definition. Matching future records are included when history is reloaded and results reopened.

This extends the existing query, PopulationStudy and ResearchDocument systems. There is no parallel calculation engine: the visible cards, evidence links and existing report exports share the same comparison document. Actual population queries and metric remain explicit. The entered question is a label; its wording does not silently add conditions.

The report includes sample sizes, missing/excluded observations, overlapping populations, uncertainty where justified and recorded version/rule/class/difficulty/challenge conditions. Missing data remains unknown. Different conditions and observational associations do not establish causality. The numeric comparison and win-rate scope are deliberately explicit.

The missing foundation was durable experiment definitions: research pins and the A/B comparison existed, but were temporary. The new independently versioned registry stores definitions separately from raw logs, saved views and pins. CURRENT rules are frozen at capture. Duplicate names cannot overwrite a definition. Bounded files, checksums, validation, atomic replacement and a shared mutation lock protect definitions. Corrupt or future-format files block edits and retain original bytes.

## Compatibility

No gameplay, scoring, loot, RNG, recording format, outcome mapping or save schema changes. No migration, historical outcome inference, deletion or zero-filling. The tests exercise writer close/reload, subsequent ascension, distinct death/abandonment and byte-identical abandoned history. The Waterskin/manual-hotbar persistence, categorized settings, Columns modal and sorting/navigation viewport fixes remain covered.

Both APKs retain the same application signer as TEST302. All 330 previous history TSV records are byte-identical; both variants contain the same 331 historical records. Player APK excludes the private feature asset and passes the actual player settings gate. Install the appropriate variant over the existing application; do not uninstall or clear its data.

## Source changes

| File | Purpose |
| --- | --- |
| core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/research/SavedExperiments.java | Strict independent definition registry, persistence and fresh evaluation |
| core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/research/ExperimentReport.java | Shared comparison document, exact definitions, recorded conditions and evidence |
| core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndUimsdResearch.java | Native save/list/result/population/rename/delete flow |
| core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/research/SavedExperimentsTest.java | Ten recording/query/persistence/RNG/compatibility methods |
| android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/UimsdResearchDeviceTest.java | Native end-to-end save/reload/future-run/evidence/rename/delete acceptance |
| tools/run-codex-android-checks.sh | Retain all prior methods/captures and require two new experiment captures |
| core/src/main/assets/apd/apd_changes.txt | Player-visible Statistics / UIMSD entry |
| docs/APD-UIMSD-INVESTIGATION-CHECKLIST.md | Tracked acceptance and remaining scope |
| docs/APD-CODEX-CORE-TEST-REQUEST.txt; docs/APD-CODEX-BUILD-REQUEST.txt; docs/APD-NEXT-BUILD-HISTORY.json | Checkpoint/build/history tracking |

## Verification

Both developer and player core reports contain **1,988 tests / 390 suites / zero failures, errors or skips**. All previous 1,978 methods are retained; ten new methods cover exact queries, RNG isolation, persistence, future runs, unknowns, disclosure, corruption and concurrent mutations.

The actual full API34 XML contains **53 methods / zero failures, errors or skips**. All previous 52 methods and 77 captures are retained; all 79 PNGs decoded. The new native case creates recorded win/death/abandoned fixtures, uses the actual menus and pointer events to save a definition, reloads it from disk, shows -20, adds a future ascension through the recorder, reloads, shows 0 and two A contributors, opens exact source records/populations with Back, and exercises rename, cancel-delete and confirmed-delete. Raw abandoned bytes and saved queries remain unchanged. Both new 320x640 captures were visually reviewed: text and controls fit without overlap.

Core artifact `12233562793` ZIP SHA-256: `d8a1b5590dd4f27d07d41f40f3370dd0526394b47a5034ba945be1a0bd1d7683`.
Android artifact `12234307373` ZIP SHA-256: `2fc269a8240dc9cc602ecf6ed3090b1ae8e90a3218c4df25a4a79eacb25bd393`.
Player APK/core artifact `12234450705` ZIP SHA-256: `05ea9197e537d50e7179c4917061b4116a602fcb7bc56acc9c488e0b3bf0a8f4`.

- Main developer build and Android run: https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39352224211
- Player build: https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39352224391
- Exact-APK cold-restart/player-settings gate: https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39352224343
- Actual-death TEST287 before / TEST303 after: https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39352224386
- Five-profile exact-APK display matrix: https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39352224409

Cold-restart evidence confirms TEST285 reproduced, old on-disk assignments preserved through upgrade, new manual edits persisted through complete process force-stop, no data clearing between restarts, regular-player settings passed and test features absent. Nine captures decoded. Actual-death evidence passes after TEST303 and retains the expected failing TEST287 baseline.

Display matrix passes all five profiles: 320x480, 360x560, 720x720, 960x320 and representative 1080x2408. The initial small/compact failures are retained: logs show com.android.launcher3 Quickstep ANRs and the small capture visibly shows the Android launcher error dialog. Only failed jobs were rerun, without changing source, APK or assertions. Both then passed; final artifacts and source/APK hashes were checked, including all five PNGs per profile. These are emulator results, not physical-phone verification.

Small warm core fixture: one persisted definition is 518 bytes. One hundred evaluations over four rows took 3,623,958 ns in developer CI and 3,293,205 ns in player CI; the experiment suites took 0.066 and 0.076 seconds respectively. This is not representative phone or large-history performance. Added raw classes8.dex bytes: 30,696; compressed increment: 10,748 bytes. Net APK size changes versus TEST302 are +711 developer/+715 player bytes because ZIP alignment/padding also changed. No new events or recording overhead are introduced by this milestone; larger-history and Android latency/storage profiling remains pending.

## Paired private APKs

| Variant | File | Bytes | SHA-256 |
| --- | --- | ---: | --- |
| Developer | ADHD-Pixel-Dungeon-CODEX-TEST-303.apk | 38,714,105 | a7a665f79d1e6376c790ea840da7ccf9a83040f4d714263625dcbdc8899795e7 |
| Player | APD-Player-TEST303.apk | 38,713,932 | 91de724cd14c40585ce20477094907c102702dd4e431f9b195605ce85357391a |

Signing certificate SHA-256: `067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e`.
Both files were saved successfully and their saved identities and unchanged hashes verified: developer `libfile_411ad313827c81919bfb308c37edf879`; player `libfile_9ecaaeb80e4481918a2cf98bbb2937ea`. No prior file was overwritten.

No release, tester distribution or staging was performed. The unrelated automatic alpha-staging workflow failed its old preparation configuration; it is not an acceptance gate and staged nothing.

## Completed, partial and pending

Completed in TEST303: the named numeric/win-rate experiment workflow above, including visible results, exact evidence, persistence and qualifying future-run recalculation. TEST302 already completed exact saved-question persistence; TEST301 navigation checkpoints and TEST300 equipment-following windows remain intact.

Partial: general questions/interpretation and translated terminology; comprehensive statistic-to-run/floor/fight/turn navigation; timeline/death context; broader experiment execution beyond saved exact populations and current metrics. The question label does not execute arbitrary natural language, sequence conditions or temporal windows.

Pending: comparable-condition personal progress analysis, extensible language/aliases, comprehensive forward-only context snapshots and representative performance/storage profiling. All five requested capabilities are not complete, and no uniqueness claim is made.

Next build must preserve produced TEST303 source/run `7505a36ae1567458ba854db9427196a893824806` / `39352224211`, all 331 prior history records and both private APKs. See the connected implementation checklist for remaining acceptance criteria.
