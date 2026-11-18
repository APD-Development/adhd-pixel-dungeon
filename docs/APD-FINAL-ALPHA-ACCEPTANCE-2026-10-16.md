# Current verified owner delivery

## Verified owner delivery — TEST254 — 2026-10-16 19:49:53 UTC

Owner links were sent in chat between 19:48:42 and 19:49:11 UTC (3:48–3:49 p.m. EDT), before the 4 p.m. deadline. This is not a claim that all approved backlog work is finished.

- APK: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-254/ADHD-Pixel-Dungeon-Alpha-Preview-254.apk
- Current report: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-254/APD-Alpha-Preview-254.pdf
- Exact application source: `832c1a5679a8cd216e8c57638f1bc0fe2442fdfe`; SHA-256 `131d205b17a2c118abd794cf49631c21b52166cb3b4586fb2545cbbab3e53a72`. App ID, certificate and version compatibility were rechecked against retained TEST250. Install over APD; never uninstall or clear data.
- Published 19:47:02Z. Actual published APK download rehashed 19:48:38.800460Z; published PDF/receipt/status downloaded and checked too. Receipt workflow 38957537766 succeeded. Tag targets publication wrapper 51bdf6b18b1abb3c861ee80186f67def72e17df5; the APK independently retains source 832c1a. No application rebuild during publication.
- Primary source build 38949555542: 1,736 unique core tests and 36 Android14 methods, no failures/errors/skips; 49 images reviewed. Six-class save/continue adds one method; Android16 selected eight methods plus final two native methods passed. Across accepted runs: 37 distinct Android methods, 47 executions, 71 reviewed images. Repeats and the 2,400 boss parameter cases are not counted as new methods. Ten existing Python delivery-profile tests passed separately.
- Final native run 38956379747 passed both methods and all three required images. Reviewed held-finger preview shows Enemy HP350%=1.7062× and Combined1.7062× before release. Actual pager/play/save/continue/end passed; HUD message baseline clears raised pager.

### Changes since delivered TEST250
TEST251: optional native scoring-vision popup, separate from slider controls. TEST252: daily calendar uses frozen earned long scores rather than displaying original scores (reproduced133 vs266), preserving historical original integer data; unknown old earned scores are not fabricated. TEST253: narrow 135-logical-pixel quickslot pager moved above overflowing row; rejected after manual review found message overlap. TEST254: reserve message placement above raised pager and refresh on toolbar layout changes. Seven production files changed; all357 provisional lookup values, ordinary multiplication, original SPD/challenge calculations, slider gameplay and RNG remain unchanged.

### Failures and limits retained
Android16 attempts passed7/8,8/8,7/8,0/1,1/2,1/2, then2/2; together with primary36 and roster1, 68 executions=63passes+5failures. The System UI ANR and wrong-window input rejection are photographed/logged; Google Play services ANRs do not prove the exact pager root cause. The resource-isolated emulator disables only its failing Google Play services and stops the unused Gradle daemon; it does not hide System UI or alter APD. The preview test's destroyed-window NPE was fixed by requiring a new settled Title before attaching that fixture. Canceled superseded run38956301873 never reached application tests. Original failures are preserved in the PDF and supplemental archive; none is relabeled passed.

Initial report publication38956652301 failed with HTTP403 on release creation targeting a historical commit. Using the authorized workflow's current commit for its report tag succeeded38957117731; actual APK provenance remains explicit. TEST254 publication38957334439 uploaded all nine assets, then its stale target guard failed because it expected application source instead of wrapper. Corrected receipt38957537766 verified the exact existing tag, actual signed APK, PDF and evidence without replacing any assets. No permission was requested or bypassed.

No physical phone was attached. Owner-save install-over, full human boss/class strategies, broader UIMSD audit/storage research, actual armor-playthrough reproduction, inherited audio investigation and unverified historical build gaps remain open. This package does not establish that every approved task is complete. TEST250 and231 fallback assets remain intact; earlier pending afternoon PDF remains an honest timestamped snapshot, superseded by the current TEST254 report.

Four existing APD automations were resumed with their schedules preserved; hourly instructions now preserve active357-value v2.6R scoring and actual current receipts. The API confirms enabled but returns null next_run_time; no future execution or exact-time delivery is claimed from that metadata. Scoring-review and unrelated automations were left untouched.

Machine-readable owner receipt: `docs/APD-OWNER-AFTERNOON-DELIVERY-2026-10-16.json`. Next developer: read this record and latest real branch before work. Reuse TEST254 when no application source changes exist; next compiled APK history compares actual TEST254 source832c1a/run38949555542, including reviewed post-build test/tool changes. Preserve frozen run scores/settings and player raw history.


---

Earlier checkpoints below are preserved as historical evidence, not current status.

# Early-alpha owner acceptance — October 16, 2026

## Implemented since the delivered TEST250
The optional Experimental scoring popup explains my long-term ideas for interactions among all 18 rules, classes, subclasses, crown choices and progression. It distinguishes those ideas from today's unchanged provisional lookup multiplication and does not reward or penalize random loot. It is separate from the slider controls and uses the existing scrollable Pixel Dungeon window.

A real daily run exposed a calendar inconsistency: original score 133, earned score 266, displayed calendar score 133. Daily history now reads the frozen earned value and retains compatible optional long-score metadata. Existing integer scores remain intact. The latest retained older frozen daily can be recovered, but older days whose adjusted values were never stored cannot be invented. Large earned values wrap beneath dates when needed.

## Current narrow-toolbar correction — TEST253 candidate
At the minimum 135-pixel logical portrait width, the original three quickslot frames require 57 pixels, the normal action buttons 64, and the page switch 19 additional pixels: 140 total. The centering code shifts that overflowing row into Inventory. A local correction moves only the page switch above the quickslots when its row cannot fit, preserves all original control sizes, and keeps that placement when the toolbar is flipped. Wider layouts are unchanged.

An expanded native fixture covers 72 combinations: six widths, three original layouts, both toolbar sides and both pages. It also captures the actual toolbar and taps the pager using Android touchscreen events. The daily-calendar fixture now explicitly waits for a newly created TitleScene before adding its popup; fresh-process diagnostics distinguish lifecycle interference from an application Back defect.

The changed Toolbar compiles locally. Seventeen targeted methods pass after providing the standard source-root test property; an initial manual invocation omitted that property and failed one source-inspection fixture before it could inspect source. No test assertion was weakened. The next exact-source gate requires 1,736 core methods, 36 Android 14 methods, 49 reviewed captures, debug assembly and complete history against the actual preceding TEST252. All candidate checks remain pending at this source checkpoint; do not publish before success and Android 16 re-verification.

## Completed TEST252 verification before supplemental Android 16 checks
TEST252 source fa39ad824347a67624a93efbb4a3fb358c7477fe passed 1,734 unique core methods and 36 Android 14 instrumentation methods with zero failures, errors or skips. Debug assembly and exact preceding-APK history checks passed. All 48 primary screenshots were reviewed. CI ran 12:56:19–1:13:20 p.m. EDT, 17 minutes 1 second; that is workflow elapsed time, not developer-active time.

The exact APK SHA-256 is 333086ba911b7614f8562df21a8b50e1d8f18cb362b647bfa7032273d64a7f51. Application ID com.charltonchris29.adhdpixeldungeon.indev, version code 920, signing certificate 067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e match the published TEST250 install identity. TEST252 has not been published while additional acceptance is unresolved.

## Supplemental class and boss evidence
The same signed TEST252 APK passed a real Android 14 new-run/save/continue scenario for all six classes using locked non-neutral rules, Difficulty 5 and all nine challenges. Six continued-game screenshots were reviewed. This is one parameterized instrumentation method, not six additional methods.

Seventeen targeted headless methods passed: fifteen repeat methods from the primary suite and two new permanent boss phase/save methods. The new methods cover 1,200 Tengu and 1,200 DM-300 configurations across six class identities, five difficulty levels, twenty Boss HP settings and both stronger-boss states. They exercise actual boss damage/phase/save logic with visual callbacks isolated. They do not establish full human-played boss victories or class-specific strategies. The complete suite will contain 1,736 methods once these two methods are included in a later full run; do not call the TEST252 primary run 1,736 tests.

## Unsuccessful checks retained
The first Android 16 attempt ran no app tests: the emulator needed 7,372.80 MB for userdata and the runner had 7,036.54 MB available. Removing unused toolchains from the disposable runner allowed the next emulator to boot without modifying the application.

That retry ran seven methods: five passed, the daily-calendar test reached its correct value/layout/screenshots then lost the render thread after Back closed the activity, and the real-run test reported category-hotbar overlap with Inventory. The candidate was not called Android 16 verified or published. A subsequent diagnostic attempt failed before any app tests because the emulator action split a Python heredoc into independent shell commands. It was corrected to execute a script file. Fresh-process diagnostics remain pending at this report checkpoint.

## Remaining acceptance and limitations
Investigate and resolve the two Android 16 failures, recheck actual screenshots, complete any required full gate after production changes, and publish the verified private owner APK with its exact source, signature and checksum evidence. The published TEST250 APK remains the fallback and its assets are preserved.

No physical phone is connected to this execution environment. Owner-phone install-over, the owner's active-save continuity and physical touchscreen acceptance remain unverified. Emulator testing does not satisfy those checks. Broad human-played class/boss combinations, the entire UIMSD audit, audio-delay reproduction and the original armor-availability playthrough remain open; they are not claimed complete.

All 357 multipliers, ordinary multiplication, original SPD score calculation, nine challenge factors and 18 slider mechanics remain unchanged by the explanation and daily-calendar fixes. Future dynamic/class-specific scoring is not implemented. No public release, paid service, unfinished online activation, automatic tester distribution, uninstall or player-data clearing is performed.
