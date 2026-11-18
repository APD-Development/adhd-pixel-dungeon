# APD TEST48 development report — October 11, 2026
## Source and delivery status
Source commit d86d9713e9a2045d614b1f5645667b0a5d499665 on codex/apd-phone-fixes-2026-10-09. Source Actions run38145456328 succeeded. APK candidate is ADHD-Pixel-Dungeon-CODEX-TEST-48.apk, built from this exact source. Private staging reuses that APK; no redundant Gradle rebuild. Signing/application ID/checksum evidence is appended by the delivery validator. Until validator succeeds, signing compatibility is pending. TEST45 remains the last delivered verified fallback; previous assets are preserved.

## Changes since delivered TEST45
1. ArchiveImporter: explicitly bounded draining of ignored/repeated ZIP entries. Previously getNextEntry could drain entries beyond the existing512MiB resource limit. Five tests cover oversized/exact-limit/small/zero-progress entries, existing-history preservation and temporary-file cleanup.
2. RunIndex: reject unsigned record counts that overflow a signed long or exceed remaining bytes. A controlled valid-CRC malformed index was accepted as empty instead of rebuilding. Two tests preserve original log bytes and legitimate empty indexes.
3. RunLogReader: reject overflowed unsigned frame lengths. Previously a2^63 length could cast to zero and appear to be an empty future frame. Two tests cover rejection and legitimate future-frame compatibility.
4. RunLogReader: bound recovery string IDs by actual file size before allocating placeholder entries. A tiny valid-CRC input caused OutOfMemoryError in an isolated32MiB baseline JVM. One permanent recovery regression passes after the guard. No production resync caller was found; this is recovery API hardening.
5. RunLogWriter: atomically claim new log paths with createNewFile before opening a truncating stream. A controlled exists/open race overwrote existing bytes. One test reproduces the race and verifies preservation.
6. RunLogWriter: close no longer reports success before its queued close request completes or while the worker remains active. One gated-worker regression tests timeout/repeated-close/final completion and sealed log contents.
7. QueryEngine: length-prefix text group-key components. Embedded separator characters previously merged two distinct text tuples. One permanent regression plus existing grouping tests passes.
8. OutboxStore: strictly decode UTF-8 inside Base64 fields. Malformed bytes were silently replaced rather than reported as damaged queue records. Two tests verify rejection without rewriting the file and exact valid Unicode round trips. Queue mutations already refuse partially damaged stores; no online features were enabled.
9. ArchiveImporter: build a per-import path lookup instead of linearly scanning the entire manifest for every entry. First-match duplicate semantics preserved. One permanent compatibility/idempotence test added. A local20,000-entry lookup microbenchmark measured856.736ms linear versus13.493ms map construction+lookup; this is not an end-to-end or phone performance claim.
10. Android gameplay test: waits for actual rendered control readiness after actor-thread hero.ready, retaining Search/Wait/Inventory usability assertions and reporting persistent disabled states. No production gameplay change was made for this synchronization hypothesis.

## Tests and unsuccessful checks
Actual downloaded TEST48 XML:180 core reports,1083 tests,0 failures/errors/skips; API34 emulator13 tests,0 failures/errors/skips.
Targeted tests:130 archive/reader/index/writer tests;90 archive/query tests;37 queue tests. These suites overlap; do not sum them as unique coverage.
Sixteen new permanent unit tests since TEST45.
Baseline controlled failures established the malformed index/frame/recovery allocation/writer race/close/grouping/UTF8 defects. One32MiB JVM reproduced the reader allocation failure.
TEST47 built and passed1079 core tests but FAILED the Android save/resume gameplay check because Search was inactive immediately after resume. It was not called verified or published. TEST48 waits for rendered readiness and passes the unchanged control assertions; this supports a synchronization race but does not prove every possible phone timing case.
An initial local queue run used a stale compiled Outbox class and failed two existing malformed-gateway tests; compiling the canonical implementation resolved both. Initial archive compilation needed existing test support classes. These setup failures were corrected, not counted as passes.
Physical phone testing and a complete normal victory run have NOT been performed here. Synthetic run-win recording is not a full victory playthrough.

## Compatibility and potential regressions
No application ID/signing identity changes, save reset, statistics deletion, gameplay balance/RNG changes, server activation or new paid dependencies. Existing history byte-preservation tests pass.
Strict malformed-data rejection may now surface repair/damage messages for files previously silently accepted. Legitimate future frames, empty indexes, Unicode queue fields and duplicate manifest first-match behavior are tested.
Writer close may correctly return false where it formerly reported premature success; callers should treat this as incomplete shutdown, not proof of data loss.
Archive lookup uses additional O(number of manifest entries) references while removing quadratic search work.
Owner phone checks: upgrade in place; do not uninstall or clear data. Test start/play/search/wait/save/resume; UIMSD history/export/reimport/queue add-inspect-remove; known Arcana descriptions from TEST45.

## Commits and files
b76bc1cd5e48899b721ca6168cc0bcaabcd3557b: archive bounded drain.
0b5088f4b49d4a8bef865494b14db78dd21db5b3: reader/index validation.
33209e74267613d632dcf81f6f7c3b08d3adec05: writer creation/close fixes.
79b62d3d415d0fb331a7338bb47a30b12d54206e: grouping/queue/archive lookup.
d86d9713e9a2045d614b1f5645667b0a5d499665: rendered-readiness Android test and verification request.
Production: core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/{archive/ArchiveImporter.java,stats/RunIndex.java,stats/RunLogReader.java,stats/RunLogWriter.java,query/QueryEngine.java,backend/OutboxStore.java}.
New regression classes: ArchiveIgnoredEntryLimitTest,RunIndexCountValidationTest,RunLogFrameLengthValidationTest,RunLogResyncAllocationTest,RunLogCreationRaceTest,RunLogCloseCompletionTest,GroupingTextBoundaryTest,ArchiveManifestLookupCompatibilityTest,OutboxUtf8ValidationTest.
Android: android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/PlayRunDeviceTest.java. Shared handoff/bug log/build request updated.

## Remaining work and honest timing
Comprehensive UIMSD audit is incomplete; every component has not yet been reviewed. Cross-instance index temporary-file races, concurrent recovery tail creation and large whole-file reader memory use remain suspicions without confirmed loss evidence.
Internal hotbar capacity extension and other approved backlog work remain separate; do not imply implemented by this build.
No inherited SPD code was changed during this audit batch.
GitHub Actions confirms completed builds/tests, not continuous Codex coding. The new hourly main workflow runs core tests, not an autonomous AI developer. No persistent unattended coding mechanism has been demonstrated.
Available timestamps identify workflow execution, not total active coding or overnight downtime. No defensible15-hour active-work or zero-idle measurement exists; none is claimed. TEST48 artifacts were created10:03–10:12AM Eastern on October11. Final staging timestamps are recorded in Actions. Daily publication is separate from preparation.

Preparation failure: staging run38150027918 correctly refused an incorrect Android evidence artifact ID copied in the workflow template. Corrected to11704457665; APK and source tests unchanged, no rebuild. Signing/publication remain pending until corrected staging passes.
