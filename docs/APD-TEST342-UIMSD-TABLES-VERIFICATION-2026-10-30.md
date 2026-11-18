# APD TEST342 UIMSD Tables Restoration Verification Report

**Recorded:** 2026-10-30T19:36:11Z  
**Approved branch:** `codex/apd-phone-fixes-2026-10-09`  
**Application change:** `c1f04a0b8edb9aba51008ae66cda7e3b65d614b7`  
**Candidate request/history:** `ff3dae8c6610bcfe033c6c3e7928d08d0b215e91`  
**Disposition:** compiled candidate rejected/pending; not published or distributed

## Reproduced failure and bounded correction

At real head `6eeb5ca52fd3810913822077b26af2eef0763941`, `UimsdScene.pickView` created the parameterized Tables `WndUimsdList` without `restoreWith`. `WndUimsdList` has no zero-argument constructor, so the existing `PixelScene.saveWindows` fallback could not reconstruct it and silently discarded the open chooser during seamless scene recreation.

The production correction adds the same active-scene-checked exact restorer already used by the Conditions and tour menus. It does not alter the catalog, selected view, query, history, scores, saves, all18 Custom Dungeon Rules,357 scoring values/formula, combat, RNG, content, signer, application ID or versionCode. A core source guard protects the exact restoration wiring. The existing Android menu-restoration method now opens Tables reflectively, requires a non-null restorer, rebuilds the scene twice, requires one restored chooser each time and executes the restored first-item callback.

## Evidence

| Gate | Actual result |
|---|---|
| Core-only | Run41112805919/job124259238377:2230 methods,425 suites,0 failures/errors/skips; all6 RunTurnWindow methods |
| Candidate producer | Run41113901529, TEST342; verify-and-build job124263027668 success |
| History | Immediate compiled predecessor TEST341 run40976774216/source`acb69c653033ee0d8df68264395ca27e08eb566d`; reviewed-path and compiled-history verification passed |
| Developer artifact |13003779687; workflow ZIP digest `cc7615e6154503c2373883a563b1699959c9808ccabcff6160cfdf0c0dc50765` |
| Matching player | Run41113901520/job124263028125 success; artifact13005766501 digest `9e384ca3a5a4513179fa52684ebc08c97fbd1c2539d0638153680d1bd30c008a` |
| API34 | Job124265010700:66/66 passed,0 skipped/failed;92 listed320x640 captures |
| API34 artifact |13004844055; digest `d247674ff7020f0e9f02d00ff796417a2c758ea2a3c9251f15373c161c33ecc6` |
| Display profiles | Compact,small,square,short-landscape and M53 passed on attempt2 with the exact TEST342 APK |
| API36 |3 tests run;1 failure in `titleControlsStaySeparateAndNativeChangesAndHideUiRemainUsable` |
| API36 failure | `TopScoresDeviceTest.render` line27 timed out waiting60seconds for render-thread completion; artifact13006246410 digest `a01745bb7546873396b7989371306ce7d196f6b461060a7ca486cea77aba02f4` |

The first display attempt began before the producer artifact existed and failed exact-producer lookup; attempt2 is the measured result. The API36 failure was retained and was not repeatedly rerun until lucky. No physical-phone testing occurred.

Three other automatically triggered legacy/staging paths failed independently of the Tables correction:

- Stage run41113901554 failed its application-tree equality check because its resolved publication inputs were from an older application tree.
- Hotbar run41113901528 and death-order run41113901564 failed before native execution because `git diff` could not resolve object `eda307d567bd7eacd1b304042747b65d0ea4ed72`.
- Their failures do not supply acceptance evidence and were not hidden.

## Current APK status

The latest actually published private pair remains TEST341, release428527455/tag `apd-alpha-preview-341`, published2026-10-30T00:24:54Z:

- Developer SHA256: `ad6281b3e41fe414187ca40824157a5bbcf102d7d86e569dbdb8439e7f47d694`
- Alpha-Testers SHA256: `639ce15ddb4b02d037cd29f995c9f38514e03cf577370cf38488f4602bfd65bd`

TEST330 remains a published fallback. TEST342 is a private workflow artifact only and must not be named as current, verified, delivered or playable while API36 is red.

## Next safe step

Reuse the exact TEST342 APK and retained API36 artifact for a bounded render-thread-timeout diagnosis without rebuilding application source. Do not weaken the60-second assertion, erase the failed attempt or publish TEST342. If that diagnosis is deferred, preserve TEST341 and continue independent read-only UIMSD audit work. Physical-phone verification remains separate and absent.


## 2026-10-31 API36 lifecycle diagnosis and guarded rerun

The original API36 artifact was diagnosed rather than rerun unchanged. Its final Back closed the Activity; `AndroidLauncher` then reached PAUSED, STOPPED and DESTROYED before the test's `finally` cleanup called the 60-second render guard. That timeout therefore did not prove a live render-thread freeze.

Test-only commit `a423f98d5532c9b008a34f5828afce618aca2302` made Settings acceptance lifecycle-exact: the first window must close while the Activity remains RESUMED, the reopened window must be a distinct instance, and cleanup cannot mask a stopped Activity. Run41208749842 passed all five API34 profiles. API36 failed earlier at the first Changes-page accessibility Back: no `APD-BACK` route appeared and title restoration timed out. Artifact13045836067 digest `f4ba58d5b63b4b97e43748c1140a5a34d9ff4c8a6d16f6a1f52c1d7dd546285b`.

Test-infrastructure commit `b8e336a427846d35535521e3c2a44dbdd004e843` then:

- requires the Android accessibility service to accept each system-Back request;
- on API36, waits at most180 seconds for Android's broadcast queues to become idle before instrumentation;
- leaves the real system callback path,120-second title condition,60-second render guard and all window/lifecycle assertions unchanged.

Display run41209586264 reused the protected-input-identical TEST342 APK and compiled instrumentation only. Android reported `All broadcast queues are idle!`; the accessibility request returned true; the app's registered priority-0 `OnBackInvokedCallback` was visible in logcat. Nevertheless, no `APD-BACK` route reached the app and the first Changes-page title restoration again timed out at `TitleMenuLayoutDeviceTest.java:150`. Both subsequent API36 scale/stale-input methods passed. Retained artifact13045274020 digest `29dd440b91c264819868c5b8de8e082c33b1a5f64ca94470c7eaa9d67dfe2a97`.

Four API34 profiles completed successfully. The M53 instrumentation itself completed `OK (1 test)`; its emulator went offline afterward during evidence collection, so the screenshot validator correctly rejected a non-PNG and the job remained failed. Artifact13046166155 digest `ff948e1d6ad653b542c8ed317f76e0a18341fd818f2c64e88b3dd76d3b330082`. This is emulator-only evidence.

No application source, executable build request, APK, compiled history, scoring, saves, all18 rules, combat, RNG, signing identity, publication or tester distribution changed. TEST342 remains rejected and unpublished. TEST341 remains the current published private pair; TEST330 remains fallback.


## 2026-10-31T19:03:06Z — Independent Search-results restoration and TEST343 request

After the TEST342 rejection, an independent UIMSD audit reproduced another bounded process-recreation loss. `WndUimsdSearch.results` created a parameterized `WndUimsdList` without an exact restorer, and its opener retained the destroyed scene. Production commit `0298c2f8f3704d0b0ba47b8ae2975ace2ef2152a` restores the exact query/catalog and routes a restored selection only to the rebuilt active `UimsdScene`.

Core-only run41232001787/job124627987345 at head `e9b32d4e87eb4eb3d5bb2f4f79954e3b42011453` passed425 suites/2231 tests with0 failures,0 errors and0 skips, including all6 `RunTurnWindowTest` methods. Artifact13053689442 digest: `ab666858b5eed949f804ea3202195d556bdc2a9c58837bb4fd895d939cf09e73`.

The existing Android restoration scenario now opens a real search, retains the exact ordered results and unchanged query through two consecutive scene rebuilds, and proves the restored selection dirties the rebuilt scene. TEST343 is intentionally requested with compiled TEST342 run41113901529/artifact13003779687 as its immediate predecessor. It remains pending until full producer, history, API34, API36 and display evidence completes. TEST341 remains the current published private pair; TEST330 remains fallback. Nothing new was published or distributed.


## 2026-10-31T19:17:01Z — TEST343 pre-build provenance rejection

Sprint run41232361292 was actual ordinal343 and stopped at its first history gate. `previous_source` had been set to TEST342's application source `c1f04a0b8edb9aba51008ae66cda7e3b65d614b7`, but the workflow correctly requires the exact prior producer head `ff3dae8c6610bcfe033c6c3e7928d08d0b215e91`. Core, APK build and history append were skipped; TEST343 therefore names no compiled APK.

Display run41232361337 kept all six jobs bound to the same protected application inputs and, after its bounded exact-producer lookup, failed without compiling or substituting an APK. The corrected TEST344 request separates `previous_source=ff3dae8c...` from `previous_application_source=c1f04a0b...`. No production or test behavior changed in this correction.
