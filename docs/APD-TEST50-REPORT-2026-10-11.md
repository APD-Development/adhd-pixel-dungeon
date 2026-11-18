# APD lunch candidate TEST50 development report — October 11, 2026
## Source and delivery status
Today's candidate is TEST50, source14226785732470e5241daaabe92ba4dc7d399df3, run38151255573. Full-source run38151255573 succeeded: actual XML records 1,089 core tests and 13 API34 Android tests, zero failures/errors/skips. Signing/checksum validation and artifact staging are separate; final appended validator evidence records their completion. Candidate APK is ADHD-Pixel-Dungeon-CODEX-TEST-50.apk.
Verified fallback: TEST48 source d86d9713e9a2045d614b1f5645667b0a5d499665, source run38145456328, signing/checksum/source validation run38150379303, private staged artifact11706672718. APK SHA25671752691c020b2af3e698153d0ae1f74aa20485fa1002cdb7704cc241b9207f4. AppIDcom.charltonchris29.adhdpixeldungeon.indev and certificate067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e unchanged. TEST45 remains delivered rollback; neither fallback is to be deleted. No early PDF/APK publication.

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

11. ReportExport: try-with-resources includes destination close in the success decision. Previously close IOException was swallowed and the user saw 'Saved'. Three permanent tests cover close failure, write failure and successful completion. No source history or saves are modified by this fix.
12. ReportRenderer: retain original CSV quoting after adding a formula-safety apostrophe. Redundant recomputation forgot carriage return, splitting formula-like text into unquoted multiline CSV data. Three permanent tests cover formula-like/ordinary CR, signed numeric values and embedded quotes. Numerical meanings and anti-formula behavior preserved.

## Tests and unsuccessful checks
Actual downloaded TEST48 XML:180 core reports,1083 tests,0 failures/errors/skips; API34 emulator13 tests,0 failures/errors/skips.
Targeted tests:130 archive/reader/index/writer tests;90 archive/query tests;37 queue tests. These suites overlap; do not sum them as unique coverage.
Twenty-two new permanent unit tests since TEST45; final full-source counts are supplied by the appended validator. Eighteen targeted report tests passed locally, including six new tests. TEST49's full core/build/API34 workflow succeeded for the close fix but did not include the later CSV fix; TEST50 checks both.
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

## Further unresolved inherited SPD findings — production code unchanged
Read-only review of upstream57a4e06a4caf162446d1c28caa7983f0493fecf0 reproduced three defects using exact unchanged upstreamFileUtils andBundle (byte-identical to APD):
- HIGH: FileUtils.java:222 ignoresBundle.write(false), then lines208–210 delete the original and promote the failed temp. Fault injection produced a0-byte replacement of a previously valid save with noIOException to caller. Bundle.java:550–552 catches writeIOException and returnsfalse. This is a controlled failing-disk reproduction, not an observed phone data-loss report.
- CONDITIONAL: FileUtils.java:84 joins nested directory names without separators. A validslot/nested/game.dat.spdtmp was skipped instead of recovered. Ordinary one-level slots were not shown affected.
- CONDITIONAL: FileUtils.java:191–194 skips input.close whenBundle.read fails. A tracked read-failing stream remained open; phone descriptor exhaustion was not measured.
Three reproduction tests passed, meaning these BAD behaviors were reproduced, not fixed. Initial harness tried to subclass finalHeadlessFiles; switching to a Files interface proxy fixed compilation. Expected fault exceptions appear in evidence logs.
No inherited production source changed because the owner specifically requested read-only SPD investigation. These unresolved save-integrity findings require reconsidering that instruction before a patch; do not hide them or claim alpha stability verified. Full report/source links and reproducer are committed atbb2f711a25fb50b2f9dd1aa4cf5c8a9f104ebe47 in docs/SPD-READ-ONLY-FINDINGS-2026-10-11.md and tools/spd-audit/UpstreamFileUtilsReproductionTest.java. Tests touched only disposable directories.

## Additional commits and preparation accounting
37d8e896d0d459834244febcf8d6fbf1401a2eda: ReportExport completion fix.
b4874dbd920d065813b981c2e8913c6462f06bd0: CSV CR quoting.
bb2f711a25fb50b2f9dd1aa4cf5c8a9f104ebe47: read-only inherited findings.
14226785732470e5241daaabe92ba4dc7d399df3: full current-source verification request; concurrency now queues explicit requests rather than canceling active emulator checks.
TEST48 signing/source/checksum staging succeeded at11:13AM Eastern. An incorrect Android artifact ID initially caused safe refusal; corrected without rebuilding. Stale delivery-helper prose claiming a missed noon/release tag before publication was corrected and staging refreshed. No release was published by those staging runs.
At11:23AM Eastern, source TEST49 Android checks were still executing; later completed successfully. TEST50 is a necessary build after an actual application-source CSV change, not an invented daily rebuild.
No complete active-development/idle-time measurement exists. CI timestamps measure workflow elapsed time, not continuous coding. Host regression timings and actual Actions timestamps are retained; no zero-downtime or overnight-work claim. Physical phone and whole-game victory remain untested. Audit42/423 baseline UIMSD components reviewed; not complete.

## PDF preparation correction
Local PDF extraction exposed a renderer defect: a title directly adjacent to the source-status heading was treated as one block and skipped, dropping the source-status section. The renderer now separates headings before block handling. Four permanent tool regressions pass; regenerated PDF text confirms source status, inherited findings and reproduction evidence are present. This is reporting tooling, not an application-source change or reason to rebuild the APK. No broken preview was delivered.


## Final preparation checks — 11:38 AM EDT
Native XML artifact11706752278 confirms all 13 expected methods passed; core artifact11706293706 confirms 1089 tests passed. APK artifact11706063940 is the exact source142 candidate, not rebuilt for documentation. Additional offline-gateway/features host checks:9 passed; backend foundations/outbox checks:19 passed after correcting the test working directory to core. Two first attempts from workspace/repository root failed only because the source-scanning architecture test requires core; this was not hidden. UIMSD review now53/423 manifest components, incomplete; no new gameplay change from these reviews. No physical-phone validation or full human-played victory is claimed.
