# APD TEST22 private development report — October 10, 2026

## Delivery status

This report accompanies today's existing TEST22 APK, built from real GitHub commit 6ffd9668b853bbb94922888e6880611aebe1a2f8. Verification run 38071699386 successfully built and tested it. Its publication job was intentionally skipped by the commit's [verification-only] marker; no TEST22 release existed when this delivery recovery began. The lunch deadlines were missed. Recovery validates and publishes the existing artifact without another application build. The attached build-verification section records the final checksum, signing comparison and actual test results.

## What changed since the delivered TEST20 APK

Blacksmith quest restoration now clears optional fields before reading another save. Previously, absent legacy fields could retain values from the previous loaded run. Three targeted regressions cover isolation between saves. This changes restoration defaults, not quest rewards, gameplay balance or the save schema. Older real player saves still need phone upgrade testing.

UIMSD exclusion messages now use the correct table units. Counts of excluded item or other table rows were incorrectly described as counts of runs. Two regressions check the corrected unit and singular/plural wording. Filtering and stored statistics are unchanged.

Submission-queue mutations and sending now refuse partially unreadable queues. The loader already counted damaged entries, but later operations ignored that warning and could rewrite the file, dropping unreadable entries. Add, remove and send operations now preserve the original bytes and display the problem. Tests also simulate corruption during a gateway acknowledgement: the actual acknowledgement remains recorded and reported while damaged queue bytes are retained. These tests use a fake gateway; no server has been activated.

Persisted duplicate queue IDs now preserve the first successfully restored entry and report subsequent duplicates. Previously, duplicate IDs could overwrite the first entry's retry metadata and inflate queue counts. Three additional regressions verify metadata protection, invalid-first/valid-later recovery and refused queue removal with byte preservation. This deliberately makes damaged duplicate-ID files read-only until a safe repair is designed; it does not silently delete or repair them.

The backend client refuses an already damaged queue before starting background network work, and skips later mailbox work if sending discovers corruption. The regression checks the synchronous refusal. This protects existing offline queue data; it does not enable an unfinished backend.

Privacy summaries now warn about unreadable queues instead of showing a misleading empty queue, and describe queued item types accurately. Explicitly consented full-log evidence is distinguished from run summaries. UIMSD Home explains exclusions for unknown/changed rules and repeated imported copies. These are diagnostic changes, not history deletion or fabricated statistics.

## Files changed in the application and tests

Application files: actors/mobs/npcs/Blacksmith.java; apd/backend/OutboxStore.java; apd/backendbridge/BackendClient.java; apd/backendbridge/PrivacySummary.java; apd/backendbridge/SubmissionQueue.java; apd/query/StatusText.java; apd/query/UimsdHome.java. Paths are relative to core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/.

Regression files: actors/mobs/npcs/BlacksmithQuestPersistenceTest.java; apd/backend/OutboxStoreTest.java; apd/backendbridge/SubmissionQueueTest.java; apd/query/StatusTextCountsTest.java; apd/query/UimsdCombinedScopeAuditTest.java. Paths are relative to core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/.

Documentation changes include the shared handoff, UIMSD audit, bug log, sprint report and explicit build request. Delivery recovery adds only a report, an existing-artifact validation script and a delivery-only workflow. It does not alter application source or rebuild TEST22.

## Actual verification

Downloaded core and Android evidence archives matched GitHub's recorded archive SHA-256 digests. Actual XML reports contain 931 core tests across 149 suites, with zero failures, errors or skips. API 34 emulator XML contains four passing checks with zero failures, errors or skips: UIMSD scene-recreation/narrow action containment; play/save/continue/controlled death with history; controlled victory recording; and hero difficulty/setup with the gear menu. Total XML test time is 58.688 seconds for core and 217.468 seconds for Android. Test duration is not a measurement of coding time.

Deterministic combined-query regressions cover 192 scope/filter/sort/column arrangements after saved-view disk reloads. Additional hidden-information comparisons are loops within tests, not thousands of separately counted tests. Local C30 acceptance previously passed 28 targeted queue tests in 0.720 seconds and 560 non-native checks in 19.689 seconds. The full CI suite now provides native/core/build evidence for the exact TEST22 source.

These emulator checks do not represent a full normal dungeon run, all quests/bosses, every screen size, Android process death, an upgrade of the owner's actual save files or a physical RedMagic test. Signing and package compatibility are checked directly against TEST20 before publication. Player-data preservation after an actual phone upgrade remains unverified.

## Potential regressions and remaining risks

Damaged or duplicate-ID queues now block operations that previously appeared to work while losing entries. Players may see a new damage warning and be unable to add/remove/send entries in that queue. No automatic repair is included. A remotely acknowledged entry retained after corruption could be retried later; this requires the existing idempotent submission handling. No real server was used in this test.

Blacksmith legacy-field defaults and privacy wording need phone checks with real history. No new failing regression was observed in the TEST22 reports. Passing those checks is not proof that APD contains no bugs. Saves, history, app ID and signing key must remain intact; never uninstall or clear app data to test this update.

## Work not included in TEST22

The Warrior simulator has a policy/experiment framework with four passing fixture tests, but no real APD game-session adapter and no 100 actual Warrior simulations. There are no measured Warrior survival or early-healing frequency results.

Local Community preview, guaranteed Crystal Path rewards, weapon accuracy information, movement-speed settings, healing confirmation and guaranteed healing-drop identification are not in this source commit. Several are partial or lack required native/device verification. The category hotbar and Cleric one-to-four direct spell controls remain incomplete. No claim is made that these features are delivered. The reported fast movement root cause remains unresolved. The unusual diagonal wall connection was reported traversable and remains a lower-priority visual investigation.

No online voting, cloud account system, clans, hidden achievement, paid advantage or unrelated balance change is activated. Creator-controlled Cleric restrictions/lore are preserved.

## Commit evidence

Rollback TEST20 source: e4ad30fd84362589db3ba1180a785ee35344472e. Blacksmith fix: afa6f0177516a754c8a89e55caeb8774d0e61a15. UIMSD audit: 09c0f36113a75f51fb228b83ad03b02eff9ba565. Transferred-repository safeguard checkpoint: 04294dbc1fe31bc0a01e9a56f3ebb875812e361e. TEST22 source and duplicate-ID fix: 6ffd9668b853bbb94922888e6880611aebe1a2f8. Delivery documentation/workflow has separate Git history and is not application build source. Local synthetic history is never pushed; main and Claude's branch are untouched.

## Timing and unsuccessful preparation

The TEST22 CI run started October 10 at 1:25:48 p.m. EDT and completed at 1:36:52 p.m. EDT, an elapsed 11 minutes 4 seconds. Publication was skipped. Earlier TEST21 delivery failed before executing a step. Local Android assembly was unavailable because the workspace lacks the Android SDK; some local game tests also lacked desktop libgdx natives. These limitations did not prevent the later successful TEST22 GitHub verification.

There is no complete timestamped activity ledger for the prior overnight period. Exact active development time, coding/debugging/research totals and the longest idle interval cannot honestly be reconstructed. Commit gaps do not prove inactivity. No continuous overnight execution is claimed. Current recovery checks the already completed remote run rather than pretending a chat remained active after its response ended.

The first delivery-recovery workflow (38072882786) stopped at signing compatibility validation before publication. Artifact retrieval and test checks succeeded, but the certificate comparison did not pass. Public certificate diagnostics were added to distinguish an actual signing mismatch from a parser issue. No failed validation is bypassed and no incompatible APK is delivered as an update.

Diagnostic run 38072974821 established that both APK signatures verify and have the same certificate SHA-256, 067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e. The delivery script expected the SDK label "Signer #1", but this SDK prints "V2 Signer:"; therefore it incorrectly parsed both fingerprints as empty and refused publication. The parser now supports both verified signer labels while rejecting missing, malformed and source-stamp certificates. Three permanent delivery-tool tests cover the observed format and refusal cases. Actual compatibility checks remain mandatory. This recovery bug affected publication, not game behavior or the APK's signing key.

## What to test on the phone next

Install over existing APD and verify existing saves and statistics remain visible. Start a fresh run: check exits, Search/Wait, quickslots, attack controls and Main Menu navigation. Play, save, quit and resume. Open UIMSD, change filters/sorts/columns, leave and reopen, inspect saved views, and add/view/remove a local queued run. Check excluded-row wording and truthful privacy messages. Switch apps while a UIMSD dialog is open. Report the installed C22 version, reproducible steps and screenshots for any failure. Never delete real history merely to test a queue or import warning.
