# TEST228 saved-column layout compatibility candidate

Timestamp: 2026-10-16T01:18:25Z (2026-10-15 9:18:25 p.m. EDT)

## Reproduced defects

Current verified TEST227 production accepted a saved column layout without checking it against the current dataset and hidden-information setting. Two retained regressions reproduced blank UIMSD tables:

- a view saved with only an internal column while hidden information was enabled reopened with zero headers after hidden information was disabled;
- a layout containing only a column removed by a newer build reopened with zero headers.

Against unchanged production, the two new methods failed exactly at the expected four-versus-zero header assertions; the other 22 UimsdModelTest methods passed.

## Correction

UimsdModel now filters a saved layout through the current dataset before applying it, retains valid columns in their saved order, rejects duplicate/retired IDs, and excludes internal columns when hidden information is off. If no usable saved column remains, the view uses its current public defaults. The saved-layout file is not rewritten by this compatibility fallback.

## Local verification

- corrected UimsdModelTest: 24/24 passed;
- broader dependency-light query/UIMSD suite: 294/294 passed, including 28,254 randomized safety comparisons;
- source SHA-256: `80d39f90675e54722cc8eb5a0b6e7d55ebe1094fc7cdef1185475d16ab308925`;
- test SHA-256: `0b52a9d91164e8e58f255431d6c63ff6614619c1458b8146a8017329417df95e`.

These local counts are subsets of the requested complete gate, not additive to it.

## Required full gate

Verify the exact candidate with 1,687 core methods, debug assembly, complete immediate-previous produced TEST227 history, 33 API34 emulator methods and 42 valid screenshots. Until that evidence succeeds, this is a pending source candidate only. TEST226 remains the latest privately delivered APK and TEST227 remains the verified CI fallback.

No release, tester distribution, physical-phone claim, raw-history/save/cache-format/scoring/custom-rule/gameplay/turn/RNG/signing/application-ID change, hidden-lore disclosure or production storage migration is included. Owner-device install-over/save acceptance, audio and unconfirmed armor/sentry investigation remain open. Unmeasured durations remain unknown.


## Full-gate evidence

Exact source `dba5e7478418c0657b1fff7f1c117f2eb6b1c3c4` completed [run 38834508461](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38834508461) successfully from 2026-10-16T01:19:43Z through 2026-10-16T01:34:41Z.

- Core artifact `12018143883`, ZIP SHA-256 `0cc4611fdfa0ffe05f791c054e626056ff686b64c965feadd79c45b92b962c76`: independently recounted 1,687 tests across 332 suites, 0 failures/errors/skips; UimsdModelTest is 24/24.
- APK/history artifact `12017564794`, ZIP SHA-256 `28ba68a91e6a6655a862ab231c6b2eb0471ebd673f7ea53f346e1902adb36bc6`: debug assembly and complete immediate-previous produced TEST227 history passed. APK is 38,584,468 bytes with SHA-256 `43349772362ec5b1a708294130e6f9b271b2fbf216b5527a5554d57dc16d3352`.
- Android artifact `12019700309`, ZIP SHA-256 `4c5f11eb59dabadc7014d2d7380877844fb8dea9f9ac1e3b3ac8a44ba503103c`: independently recounted 33 API34 emulator methods, 0 failures/errors/skips. All 42 PNGs decode at 320×640; the full contact sheet and key UIMSD/history views were reviewed without blank windows, crashes or obvious clipping.
- APK identity is `4.0.8-apd-INDEV-bC228-TEST` / `com.charltonchris29.adhdpixeldungeon.indev`. V2 certificate SHA-256 `067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e` matches delivered TEST226 and verified TEST227.

TEST228 is therefore a verified CI candidate. Private delivery was skipped. TEST226 remains the latest owner-delivered APK; no physical-phone or owner-save-install-over result is claimed. The later typed-payload provenance source at `f0b0691362f9ef9f4e753cbc12318a857a570238` is not inside TEST228 and must not borrow this Android/APK evidence.
