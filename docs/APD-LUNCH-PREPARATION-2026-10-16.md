# Daily lunch preparation — October 16, 2026

## Current status
Prepared during the 11:20 a.m. America/New_York lunch-preparation session. Preparation is not delivery. PDF delivery is scheduled for 11:55 a.m.; APK delivery is noon. No early publication is authorized. Scheduled instructions do not guarantee platform execution.

TEST249 is the current CI-verified candidate, not a published release. Application source: 37a8f6d16cd497e13ed581e887ed9d4858fe97aa. Full verification run38914965700 passed 1,729 unique core methods and34 API34 emulator methods, zero failures/errors/skips, debug assembly and compiled-history checks. All44 screenshots were reviewed. The separate six-class save/continue acceptance run38918464322 passed one additional instrumented method; it is separate from the34-method suite and is not added to that suite's total.

The APK is already staged in private draft release415070992, tag apd-alpha-preview-249; draft=true and published_at=null were rechecked at11:19 a.m. No application source changed or APK was rebuilt during this lunch preparation. No new fixes are claimed from preparation itself.

## Preserved published fallback
The existing published private TEST231 release and all its assets remain intact. If TEST249 publication fails or its evidence becomes unavailable, deliver this already-published install-compatible fallback, clearly labeled unchanged.

APK fallback: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-231/ADHD-Pixel-Dungeon-Alpha-Preview-231.apk

Release: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/tag/apd-alpha-preview-231

Fallback APK SHA-256:20f1d87ab58afd21cd37ae4027e6ebb79c01fcf802d0869801e6ffa03943bff9;38,585,399 bytes. Source5134ef6438f9562a37b3f8db727d5d004fa93527. Its verification passed1,693 core methods and33 API34 methods with42 screenshots. This fallback lacks the later TEST249 changes described below.

Candidate APK SHA-256:dcda79a5c6c83b1ac88e5294b3b134c8e1483109929b3a8da5365e7eadc8536e;38,608,287 bytes. Candidate and fallback application ID is com.charltonchris29.adhdpixeldungeon.indev, signing certificate SHA-256067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e. Direct compatibility checks passed. Install over the existing app; never uninstall or clear data. Actual owner-phone install-over remains untested.

## Included changes and risks
TEST249 adds live individual and combined scoring previews using all357 unchanged supplied provisional values. New runs freeze the selected scoring authority; historical and legacy active runs retain original scores. Custom scoring is active for new version2 runs, experimental and not competitively calibrated. Original SPD score categories/challenge calculations remain intact. Malformed or overflowing recorded score metadata no longer crashes reconstruction; raw history is retained.

Changes since published TEST231 also include reproduced UIMSD lifecycle, archive identity/validation and query-disclosure fixes, plus the confirmed circular Vault Sentry cooldown correction. Detailed root causes, regression evidence and risks follow. Derived-cache versions changed; caches rebuild from preserved raw histories. Query results, archive handling, rankings ordering, gesture recognition and legacy score display are important regression areas.

## Failures and unresolved work
Earlier candidates242–248 were superseded or failed required checks; they are not delivery candidates. Failures included history attribution guards, an invalid numeric-filter test, stale cache-version assertions and an Android test-compilation error; later attempts were cancelled before complete Android evidence. TEST249 passed the complete required gate. The owner-draft upload initially succeeded but tag-based readback returned404; draft-aware idempotent verification then passed without replacing assets.

Physical RedMagic11Pro/Android16 testing, original-owner-save install-over continuity and complete human-played class/boss runs remain unverified. The whole UIMSD component audit and historical-build reconstruction are not complete. The owner's exact armor-availability and rotating-sentry reports are not fully reproduced; the Vault Circle fix is a specifically confirmed defect, not proof of the reported actor. Delayed upstream attack audio remains unconfirmed in APD. Storage-format optimization remains research without an approved production migration.

## Honest timing and reading this report
The successful full CI run lasted14m59s, 10:20:29–10:35:28 a.m. EDT. This is measured CI duration, not total implementation time or a claim of uninterrupted overnight work. The documentation-only refresh reused the existing verified application. Subsequent pages retain earlier source-specific checkpoints and failures for traceability; their pending statements describe those earlier attempts, not current TEST249 status above.
