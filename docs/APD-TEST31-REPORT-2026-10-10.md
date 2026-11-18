# APD bug fixes and Community preview — October 10, 2026

## Candidate and verified status

Final application verification candidate: 25b4a95a802be730f365f82770235edb6bfc68b1 (TEST31 / workflow 38092709292), after the compatibility correction in 9974bde8a80dca7c81b5cf39cbb110f8e67ae787. Actual downloaded XML confirms 1,004 core tests passed with zero failures, errors or skips, and Android debug assembly succeeded. Actual final API 34 checks passed: six test cases, zero failures/errors/skips. Signing, application ID, exact source, checksum and TEST26 compatibility are validated by the attached verification appendix before publication; failure prevents delivery. Last delivered signing-verified fallback is TEST26, source 1483bac7ecd24cf33b64632ca0d4135bb252ad7d. Preserve that APK and all existing assets.

## Bugs fixed and causes

UIMSD status dialogs disappearing: privacy summaries, saved-view feedback and run queue previews/results used WndMessage(String). Scene recreation tried its absent zero-argument constructor and silently lost the window. Five call sites now use a semantic-restorable, wrapped scrolling UIMSD window. Restoring displays the saved payload and never repeats enqueue/remove or other operations. Commit 68ae1fa35c56c7cc0361ee5cd0118ddb93d9127d passed its earlier complete core/build/emulator verification; later combined source must pass separately.

Saved-view data loss: SavedViews.load skipped malformed/duplicate records; subsequent save, rename or delete rewrote the file with only the readable subset. Valid Base64 containing invalid UTF-8 also silently changed characters. The new guard refuses replacement after incomplete reads before writing any temporary file, uses strict UTF-8, and explains the refusal in UIMSD. Existing valid entries remain readable. Structurally valid unsupported future queries remain intact and do not block edits to supported views. Commit 77521ec3d151f05df65c0a8e1e146d0732924ca3.

Raw queue preview overflow and changed literal characters: ordinary word wrapping cannot break an oversized ID/JSON token, and the normal renderer treats underscores/double stars as formatting. Raw messages now preserve literal markers and break overlong tokens at measured font widths without changing the stored or exported payload. Unicode code points remain intact. Structured UIMSD reports/help retain existing formatting. Commit 94cf567594eb7f4fddf2bfe81b33d5323b94f600, corrected for the library compatibility contract by 9974bde8a80dca7c81b5cf39cbb110f8e67ae787.

Verification automatically publishing stale reports: an omitted commit-message marker could enable an older publication path with the October 9 report and no dedicated signer comparison. Verification now cannot publish; a separate delivery job must validate the exact artifacts, signing, application ID and rollback before creating a private prerelease. Commit a568e3ce8aaea8407c708645d883780d0f6a4fed.

## Community work

Each of the eight existing Community pages now describes its own future actions beneath the explicit offline availability statement. The content-selection heading uses separate words to wrap naturally. Existing main-menu placement, navigation and planned submission/threaded-comment/top-16/weekly-tournament/review/mod/event/development content are preserved. No servers, fake functionality, clans, creator secrets, fixed undecided rules or player-facing testing dates were added. Commit 685334b93beb03a6702618d6bd5b2c93a5c47829.

## Tests and failures retained

Existing targeted queue/history-preservation, layout and choice regressions: 35 passed locally. Saved-view/model/combined-scope/settings regressions: 39 passed locally, including two new tests covering nine damaged-file edit cases and unsupported-query preservation. Baseline tests reproduced silent malformed-record loss and invalid UTF-8 replacement before the fixes.

Community unit tests: six passed before title word separation; the full candidate suite must test the exact final source. Seven permanent wrapping tests passed, including proportional widths, raw markers, surrogate safety and a 100,000-character value. The shared corrected width interface plus existing Android-5 source contract passed 11 local tests.

New RegularFloorConnectivityTest passed locally: 240 geometry checks across six classes, the reported BVS-BND-PVG seed and 4242, and regular floors through depth 24. First-floor routes need visible doors; later floors may require searching. Intentional boss gates are excluded. The same retained geometry test also passed in the exact-source complete CI suite. These are generated-map geometry checks, not simulated player victories, quest completion or physical-phone tests.

Expanded API 34 tests exercise real UIMSD status/privacy paths, two scene resets, retained payload/query identity, long raw IDs/literal markers and narrow bounds. Community tests touch every page, recreate each scene, render the actual selected page at 100 by 120 UI units, and return without stacking windows. All six exact-source API 34 checks passed; actual case names and results appear in the verification appendix.

Unsuccessful checks: initial local fixture generation had a regex escape error; two local invocations used nonexistent test class names and ran no substantive tests. These were corrected; only subsequent passing runs are evidence. One future-format fixture initially used a deliberately ignored unknown line and was corrected to an unsupported rules enum without changing the codec. The local trimmed JDK could not use --release 11 for syntax parsing; the preflight now uses source/target 11. Combined run 38092210983 executed 1,003 core tests with one compatibility-contract failure; APK/emulator stages were skipped. Its candidate was not delivered. The new helper dependency was removed rather than weakening the contract. TEST29 verification was intentionally superseded when the unsafe automatic publication path was disabled. A separate simulator run caught an invalid character literal in the new native long-ID fixture; shared commit 25b4a95a802be730f365f82770235edb6bfc68b1 corrected it, superseding TEST30. Neither canceled run is counted as verified.

Added a cheap JDK-only Java syntax preflight in commit 424963bdf579a89e2a71ce1623beca0314fa7613. Three permanent fixtures pass; all 13 changed application/core/native-test Java files parse successfully. It catches literal/source-syntax errors before remote builds and explicitly does not replace type/API checking. Eleven existing artifact-delivery verifier fixtures also pass locally.

## Changed files

Production: windows/WndUimsdRow.java, WndUimsdOptions.java, WndUimsdText.java, UimsdText.java; apd/query/SavedViews.java and LongWordWrap.java; apd/community/CommunityPreview.java.

Tests: apd/query/SavedViewsTest.java and LongWordWrapTest.java; apd/community/CommunityPreviewTest.java; apd/RegularFloorConnectivityTest.java; Android UimsdRestoreDeviceTest.java and ApdSettingsClickDeviceTest.java. Tools: tools/JavaSyntaxCheck.java and test_java_syntax_check.py. Verification workflow codex-sprint-check.yml and shared handoff/report/build-request documentation were updated.

## Regression risks and unresolved work

The new status presentation scrolls and has a UIMSD heading. Verify Back, long previews and actual app switching on RedMagic. Scroll offset and process-death restoration are not implemented by this fix. A damaged saved-view file is retained and readable entries remain available, but edits are paused; an explicit repair/recovery interface remains future work.

All physical-phone acceptance and a real update over an old installation with existing saves/statistics remain unverified. Emulator save/continue and controlled death/win checks are not proof of an entire normal dungeon run, all bosses or quests. No new confirmed gameplay-generation failure appeared in the 240 geometry checks.

Simulator expansion is parked under the owner's latest bug/Community instruction. Existing background experiments and other shared simulator-only commits are preserved, not claimed completed by this task. Animation-speed options, customizable hotbar, Cleric shortcuts, healing confirmation, guaranteed crystal-room items, Metamorphosis choices and other approved pending features are not newly implemented by these changes.

Signing key, application ID, player save/history formats, Claude's branch and main are unchanged. No paid service, public release, backend activation, monetization, balance change or Cleric restriction change is introduced.

## Phone checklist

1. Install over the existing APD test app; never uninstall or clear data.
2. Open UIMSD privacy/status dialogs and a queued run preview; switch apps and return, then use Back. Check that long IDs and the whole preview stay inside the dialog and scroll.
3. Add/inspect/remove a local queue entry; verify run history remains and no message says a transmission occurred.
4. Create, reopen, rename and delete a normal saved view; confirm filters, sort and columns persist.
5. Explore every Community page, scroll its availability/comments sections and return to the hub/main menu; check narrow or landscape layouts.
6. Start/resume a real dungeon run and report any progression/save/controls failure immediately.

## Execution and timing

The available audit record starts at 2026-10-10 22:19:37 UTC (6:19:37 p.m. New York). Work in this recorded window includes code changes, targeted red/green tests, Community polish, new geometry regressions, actual GitHub verification failures and corrected verification requests. CI execution timestamps and final artifact hashes are recorded in the verification appendix/JSON.

Exact coding/research/idle totals were not independently metered; no zero-downtime or overnight-work claim is made. CI waits overlapped independent testing. The compatibility failure and intentional verification cancellation are recorded above; the publication workflow reuses the final verified APK instead of rebuilding it. Today's earlier lunch delivery deadline was missed; this is an additional evening test build.

Report prepared at 2026-10-10 23:02 UTC (7:02 p.m. New York); 42 minutes after the first available audit timestamp. This elapsed window is not a measurement of uninterrupted active coding time. Final build/publication timestamps are recorded separately in the verification evidence.
