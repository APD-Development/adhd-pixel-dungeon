# Alpha Testers visual playback speeds — November 5, 2026

## Request and root cause

The owner approved the screenshot's full Game speed selector for Alpha Testers. Previously both choice validation and the settings window gated the seven extra speeds behind the Developer testBuild flag, so Alpha Testers had only0.5x,1x,2x.

## Changes

The Alpha variant now packages only an extendedPlayback feature flag and exposes the same saved scrollable0.5x,1x,2x,4x,8x,16x,32x,64x,100x,1000x selector as Developer. No Developer switches are enabled in Alpha Testers. Ordinary non-alpha builds keep the original three choices. Default1x and existing saved choices remain unchanged. This controls visual playback, not turn costs, combat speed rules or game-clock/input timing. Existing extreme-speed hero-idle/audio safeguards remain intact.

The candidate also preserves the newer source-verified UIMSD raw-content identity correction at131e6551a: same-length history replacements with restored timestamps invalidate stale warm catalogs. This work was already on the approved remote branch and must not be overwritten. All door eligibility and prior owner features remain included.

## Potential regressions and limits

Extreme playback can make movement/effects visually abrupt, especially1000x; this is the approved existing Developer behavior. Alpha packaging is verified to contain only the playback flag, with all shared assets matching Developer. Saving/loading speed preferences and ordinary-build gating are covered. Physical-phone performance and install-over acceptance remain pending.

## Verification

Targeted playback tests pass, including an added Alpha-only flag regression exercising every factor, file-backed preference reload, unchanged global frame delta, and no Developer unlocks. Full core and required Android/debug/release/desktop checks plus full native/display/actual-tester checks must pass on the exact candidate before publication. No candidate is called verified based solely on this preparation report. Retain TEST364 and its assets as last verified fallback.

## Retained verification failures

M53 profile job127572477507 passed its title case then lost its emulator before mandatory screenshots could be pulled (device offline); rerun the same profile/APK. Original API36 job127572477572 and its unchanged retry hit the fixture's3.5-second achievement-banner assertion; actual rendering had not completed the banner. Correct only instrumentation to wait for normal banner expiry, retaining the final absence assertion and all original six cases. No game-source or application-artifact change. Alpha four-case native run42120968295 already passed, with the complete selector screenshot reviewed.

Full initial native42120844540 completed70/72: warm UIMSD opening263ms exceeded the unchanged250ms limit, and the fixed banner-delay assertion failed. Retain this failure. Full72-case exact-APK recheck keeps every original latency/turn/save assertion and only fixes the banner observation timing. No publication until a complete successful invocation; do not combine partial case results or weaken the250ms requirement.

## Accepted display and Alpha evidence

Actual Alpha42120968295 passed4/4; reviewed alpha-playback-choices.png shows all10 entries and current4x. Pointer sequence selected1000x then2x, reopened and retained2x. API36 exact-APK instrumentation-only recheck42123049371 passed all6 cases after observing natural banner expiry. Reviewed retained baseline captures. The M53 same-APK retry passed; all other four display profiles passed initially. Full Developer72-case recheck42123644945 remains pending; the private publication workflow waits for its complete success and all signature/source/hash checks before upload. No new application build was made.

Second full same-APK run42123644945 passed71/72, including the unchanged250ms Home-opening check. The remaining case timed out waiting for the global banner registry to empty. Correct the screenshot fixture to observe existing visible banners in the current scene render tree, retaining a final zero-overlay assertion and logging global/current counts; objects outside that tree cannot obscure the capture. Repeat the full inventory on the unchanged APK. Prior failed runs remain unverified and preserved.

## Complete final native acceptance

Same-APK run42125833870 at instrumentation81ef2b938 passed all72 cases in a single571.117-second invocation. Original250ms latency gate retained and passed; no skipped or combined case results. Retained logcat confirms the fixture issue: globalReferences=1 activeCurrentScene=0. The old global banner reference was outside the current render tree; the final zero-visible-overlay assertion passed. Full2500 core cases, all six display profiles, four actual Alpha cases and reviewed real selector screenshot are complete. Signature/source/shared-asset checks and actual download-back remain publication gates. Physical-phone acceptance not claimed.

## TEST365 Alpha Testers playback selector — verified private delivery

Application source `6d55fec660389195b492f1ce2974781cfbc27bcc`, tag `apd-alpha-preview-365`, private release `435867860`. Both variants passed2500 core tests441 suites; full72 Developer native cases in one successful invocation, six display profiles and four actual Alpha native cases passed. Real touches selected0.5x/1x/2x/4x/1000x/back2x and reopened the saved choice; all10 factors/persistence covered headlessly. Reviewed actual Alpha selector capture. Alpha packages only extendedPlayback, without Developer switches; ordinary public builds retain three choices. Preserves newer UIMSD content identity fix and all prior features. No physical-phone claim; install over only, never uninstall or clear data.

Publication `2026-11-05T23:13:15Z`; workflow actual download verification `2026-11-05T23:13:20.975650+00:00`; independent APK download/hash verification `2026-11-05T23:14:01.545587+00:00`. Final response links prepared, not preclaimed as already sent. Receipt APD-TEST365-RELEASE-DELIVERY-RECEIPT.json. No extra application builds for publication/test fixes; original TEST364 release/assets retained as fallback.

Developer: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-365/APD-Developer-365.apk

Alpha Testers: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-365/APD-Alpha-Testers-365.apk

Matching source: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-365/APD-Source-365.zip

