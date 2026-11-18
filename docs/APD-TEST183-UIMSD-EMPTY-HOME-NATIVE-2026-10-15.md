# TEST183 native empty-Home regression candidate — 2026-10-15

## Reproduced coverage gap
At verified evidence parent `a02b6ed01af2e3bc48b89f417392cc98bbf46e76`, `UimsdScreensDeviceTest.everyUimsdScreenAndDialogCanBeBuilt` opened the real UIMSD scene on a fresh emulator, slept for a few frames, then immediately replaced the Home with tour/demo data. TEST182's core regression proved the model string, but the API34 suite neither required the empty Home to finish loading, inspected the rendered wording nor captured that native state.

## Implemented
- Wait for the real scene's `cachedHome`.
- Require that the fresh-emulator Home is actually empty before continuing.
- Inspect the native `RenderedTextBlock` children and require “No finished runs yet” plus “Tables > Options > Include runs still going”.
- Reject the obsolete “Tap Rules to include” wording in the no-history state.
- Capture `uimsd-empty-home.png` before the tour injects its made-up history.

## Verification request
TEST183 requests the unchanged 1,603 core tests, debug assembly, immediate-previous TEST182 APK-history review, all 26 API34 methods and 35 valid screenshots. The strengthened assertion and new screenshot are pending until the exact candidate source completes the hosted gate.

## Boundaries
Only the retained Android device test and build/evidence documentation changed. Production gameplay, UIMSD query/model behavior, raw history, saves, permanent statistics, scoring, application ID and signing identity are unchanged. No APK delivery or physical-phone result is claimed.


## TEST183 hosted result
Source `68bea4b4d57aef872cab684e13f134b91f2d258c`, run `38661559728` (00:42:30Z–00:53:13Z) passed 1,603 core tests, debug assembly and immediate-previous TEST182 history, but API34 finished 25/26 methods with one failure in the new assertion: earlier device methods had already created finished-run fixtures, so this method's emulator was not an empty-history state. The assertion failed before `uimsd-empty-home.png` could be captured. TEST183 is not verified or delivered.

TEST184 corrects only the retained fixture. It injects an in-memory empty Home into the real scene/cache, renders and inspects it, then restores the original model/cache without deleting or rewriting device history. Production source and the wording assertions are unchanged. Full 1,603-core/debug/immediate-previous compiled TEST183 history/26 API34/35 PNG verification is pending.


## TEST184 hosted result and TEST185 evidence correction
TEST184 source `e883710fa759a41a1ffa7754f42c021f0f8121ec`, run `38662728056` (00:55:01Z–01:06:10Z), passed 1,603 core tests, debug assembly, immediate-previous compiled TEST183 history and all 26 API34 methods. The corrected controlled empty-Home assertion passed and logcat confirms `uimsd-empty-home.png` was written on-device. Evidence audit found `tools/run-codex-android-checks.sh` still had an explicit 34-name pull/validation list, so the new file was neither retained nor visually reviewable. TEST184 therefore does not satisfy its 35-PNG request and is not the completed checkpoint.

TEST185 adds only `uimsd-empty-home.png` to both the pull and validator lists. Full 1,603-core/debug/immediate-previous compiled TEST184 history/26 API34/35 PNG verification is pending. No production code, gameplay, save, statistics, scoring, application identity or signing behavior changed.


## TEST185 trigger result and TEST186 request
Commit `80506e4deb7be7a7ed1fcbbd4ee37fac17972329` added the missing screenshot to the existing pull/validator lists and changed the explicit build-request path, but GitHub created no workflow run or check suite for that push during this invocation. No TEST185 build or tests occurred. TEST186 records the missing trigger and requests verification of the same uncompiled evidence-script correction against immediate-previous compiled TEST184. This is one reconciled request, not a claim that TEST185 ran.


## TEST185 verified result
The initial TEST185 request source `80506e4deb7be7a7ed1fcbbd4ee37fac17972329` produced no Actions run. The reconciled request source `40d31d81de4c2fcc1ae154e9ec0d1c8f6a28d984` compiled under the workflow's actual TEST185 label in run [38664406486](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38664406486) from 01:12:48Z through 01:25:32Z. It passed 1,603 core tests, debug assembly, immediate-previous compiled TEST184 history and all 26 API34 methods with zero failures/errors/skips.

The downloaded Android artifact `11941165163` retained all 35 expected 320x640 PNGs. The full contact sheet and focused `uimsd-empty-home.png` were reviewed: the native empty Home is readable, shows “No finished runs yet”, and sends runs still going to Tables > Options without obsolete Rules/run-status conflation. APK artifact `11940212006` has SHA-256 `cc3c211a67d179f703e0d468ef54efdb5d62b1eec99f44cb54ececed2a519c1e`; core artifact is `11940232030`.

TEST185 is verified hosted build/emulator evidence. No production source, raw history, save, permanent statistics, gameplay, scoring, application ID or signing behavior changed. No APK was delivered, and no physical-phone or owner-save result is claimed; TEST123 remains the last owner-delivered/signature-compared APK.
