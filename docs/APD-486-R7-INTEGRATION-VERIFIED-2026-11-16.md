# APD486: verified complete R7 plus C485 integration

## Authorization and source identity
Owner explicitly requested all of C466R7 be integrated into the newer 485/486 line. This was a single authorized execution. Recurring APD development remains stopped; no other game's tasks, research schedules, existing branches, candidates or releases were modified.

Execution started approximately 11:03 AM Eastern, November 16, 2026. The exact full verification/build job ran 11:17:46 AM through 11:23:12 AM Eastern. Additional downloaded-byte verification and documentation followed in the same execution.

- Authoritative starting head: `18c8c3e74209fcaa6434399f5bbb681214455733`.
- Frozen C485 production source: `c266b30559a7427b435749837006907ec1a682f6`.
- Exact C466R7 producing source: `34a1471ccd2f56ee21bb8c2579cb154ceb5e21c7`.
- Actual common ancestor: `8c74997210585191d12603ad5e2ee398b1c54fe7`.
- Tested two-parent integration commit: `c6fa033167789ef9f2fc6f31bcff39c824dfb4db`.
- Tested and built source tree: `5197493b86a7df08b84645a334294f25f819a451`.
- Isolated integration branch: `chatgpt/apd486-complete-r7-integration-20261116`.

Do not resume from C485 alone or discard the R7 merge parent. Future successors must preserve this combined baseline. Documentation-only commits after this source commit do not change the APK contents.

## Complete change accounting
All 60 R7 changed paths were enumerated against the actual common ancestor, not guessed from a build number or a list of features. This includes 38 production files: 33 match R7 byte-for-byte; five preserve reviewed R7 behavior plus C485 improvements. Every C485 production file outside that R7 change set remains byte-identical.

Included R7 functionality: Tanto, Wakizashi, Tachi, Nodachi; Buckler, Targe, KiteShield; original item sprite sheet, messages, tier registration and drop weights; Katana guard scaling and upgrade previews; FullPackSwap; ThrowAim; original/Greater Metamorphosis choices, application and recipe; adaptive magnifier reciprocal tap/hold inspection/search; special-room wall restrictions and resume handling; difficulty fade; original R7 playback with its five normal-speed visual exceptions. Exact R7 mechanics and numerical rules were preserved, not redesigned.

Retained C485 functionality includes exact rational SeedReuseScore VERSION 6 and migrations, identity-only log reopening, streaming history-reader/import allocation and I/O optimizations, diagnostics/export failure handling, archive validation and history integrity.

Eight merge-conflict files were resolved explicitly. SearchControlContext retains R7's actual-wall/current-session predicate and special-room rules, plus C485's one-array chooser, bounded search footprint, cached Foresight and malformed-metadata guards. The legacy openRoomInterior geometry helper remains available but is NOT an R7 wall-search eligibility gate. WallSearchSession combines R7 doorless/special-room guards with C485 visibility bounds. Both wellEmitter/well and normalSpeedEmitter/normalSpeed API entry points are retained. R7 well effects retain sprite positioning and the unscaled visual layer. The chooser window uses the actual available actions. Both independently added regression methods were preserved.

Three chooser assertions were reconciled with the explicitly requested R7 semantics: saved memory alone does not expose Wall Search without a meaningful current wall. The single-array allocation, no-alias/no-hole, invalid-metadata and doorless-floor invariants remain tested, including positive wall controls. No tests were skipped or disabled to obtain a pass.

## Tests actually executed in this session
Probe run `43903983133`, artifact `14243867173`, recorded exact inputs and reproduced ten missing named R7 classes on C485.

Combined run: https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43905816255
The new permanent R7IntegrationCompletenessTest first ran against C485 and failed with the required missing-Tanto assertion. That expected red result is preserved as baseline-red.xml/log. The same test then passed in the integrated full suite.

**3,540 core tests / 539 suites; zero failures, errors or skips.** Desktop classes and Android debug, release and instrumentation Java compilation passed. This includes R7 search/playback coverage, C485 writer reopen/reader/archive/scoring coverage, and new completeness tests for actual classes, exact R7 weapon tier/guard/drop/sprite rules and retained v6 scoring/recipe cost.

The JUnit XML was downloaded and independently totaled after completion, agreeing exactly with the build receipt. Evidence artifact `14246670022`, SHA-256 `8187c4ed748f73a403ae6d66fab796d5ef3ed1719a5a16d07fe99fedaaae680c`.

## Frozen signed APKs
Original certificate SHA-256: `067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e`.
Both preserve application ID `com.charltonchris29.adhdpixeldungeon.indev` and versionCode `920`.

Developer `APD-Developer-CompleteR7-C486.apk`: 39,230,564 bytes; SHA-256 `65ad352b96146aa68206b942ef7dadf6b194759d36cd0fac869fd841a5eab7c6`. Version `4.0.8-apd-INDEV-bC486-TEST`. Flags: testBuild, grudge, demoUnlock, clericPlayable.

Alpha `APD-Alpha-Testers-CompleteR7-P486.apk`: 39,230,540 bytes; SHA-256 `275ebffeaf9c406f6068d4bc3b172b45203af6982c09cc24dc6897f3468f09b3`. Version `4.0.8-apd-INDEV-bP486`. Flags: extendedPlayback, grudge.

Build artifact `14244034917`, `APD486-Complete-R7-and-C485-Original-Signed`, contains both APKs, matching frozen source, checksums, build receipt and per-file source-completeness evidence. Bundle SHA-256 `fe16cef29f78af19e90ec58b5bff5152163111f144d78e1572f2f49e395eae1a`.

The artifact and individual APK hashes were verified again after downloading. Actual DEX class-definition tables, not merely filenames or string searches, were parsed: each APK defines 9,798 classes and contains every class defined in C485. All previously missing R7 feature classes are present. The only R7 class absent is obsolete anonymous `apd/report/ReportExport$1$1`, already replaced by the preserved C485 exporter implementation; it is not a missing gameplay feature. All Developer assets match the original R7 APK byte-for-byte. Alpha differs only in its intended `assets/apd/features.txt` edition flags.

## Limits and next safe work
These are original-signed integrated candidates. Native Android instrumentation execution, install-over/save-history runtime verification, visual acceptance and qualified release publication have NOT been performed for these exact bytes. Java compilation, static APK verification and successful core tests are not substitutes. No physical RedMagic acceptance or resolution of C467's underlying OutOfMemoryError is claimed.

Preserve exact C486/P486 bytes for subsequent native acceptance rather than rebuilding to test a different binary. Existing C480/P480 native claims #211/#212 and all earlier candidates remain untouched. Do not re-enable scheduled development unless the owner separately authorizes it. Future coding starts from the guarded integration of this combined source, not the older incomplete C485 or default main.
