# Provisional custom scoring and live previews — October 16, 2026

## Current verified result — TEST249

Exact source37a8f6d16cd497e13ed581e887ed9d4858fe97aa, run38914965700:1729 unique core methods and34 API34 instrumented methods passed, zero failures/errors/skips;44 valid320x640 captures reviewed, including full-resolution live drag, summary, neutral/non-neutral and narrow score windows. Original SPD score routine is byte-identical. All357 supplied decimal values are preserved. The exact owner-uploaded historical run retained its score and unknown settings; SHA-256 remains008cd75a5a2d4f20b6c59a573c1e7a9349e6a1678c046da03016f9b3beb8484f.

APK SHA-256:dcda79a5c6c83b1ac88e5294b3b134c8e1483109929b3a8da5365e7eadc8536e;38,608,287bytes; applicationIDcom.charltonchris29.adhdpixeldungeon.indev, code920, version4.0.8-apd-INDEV-bC249-TEST. Actual signing certificate matches published TEST231. CI timing was2026-10-16T14:20:29Z–14:35:28Z (14m59s); this is measured CI time, not a claim about untracked implementation hours.

Successful private stage38914965883 reused this APK without rebuilding. GitHub owner draft415070992 contains the same hash-verified APK and source/verification assets. It is draft-only, published_at=null, not a public/private published release and not tester distribution. The first draft workflow uploaded valid assets but its tag-based readback returned404; corrected idempotent readback38918857771 succeeded without replacing assets. The PDF remains separately staged for11:55 EDT; the APK draft retains the owner's noon EDT publication hold. Physical-phone install-over and a complete real playthrough are still unverified.

## Earlier implementation checkpoints

The following preserves earlier failed attempts and superseded pending states as dated/source checkpoints, not the current verification state.


Requested by the owner with the supplied complete 357-position v2.6R lookup. This introduces active provisional scoring for newly created runs; it is not a calibrated competitive formula.

## Changes

- One engine-independent lookup stores all supplied decimal digits for all 357 positions and 18 sliders. Draft previews, frozen run scoring, UIMSD validation and score display share this authority.
- Every slider shows its individual multiplier; a pinned combined multiplier displays the ordinary product of all 18 draft values. Horizontal dragging updates immediately, before release. Vertical gestures retain scrolling without changing settings; cancelled horizontal gestures restore the starting draft value.
- New-run scoring authority is frozen and persisted with the exact multiplier and calculation version. Original SPD score components, challenge and victory/ascension calculations stay intact. The adjusted long score is rounded once to the nearest whole point and shown/sorted as the earned score; original scores remain separately recorded.
- Old runs without scoring metadata or with original v1 authority retain original earned scores. Editing preferences never reprices an active or historical run. UIMSD permanently retains original and adjusted scores and exact lookup version, available through its existing fields, filters and exports.
- The score breakdown places Custom Rules Multiplier immediately below Challenge Multiplier. Wide values wrap to a separate line rather than overflowing.
- Current scoring notices and player release notes describe active provisional experimental values. The lifetime slider warning remains. The records-history milestone documents this transition without rewriting earlier milestones.
- Disposable statistics and query caches change derivation versions and rebuild from unchanged raw records. No save format replacement, raw-history deletion, gameplay slider recalibration, or challenge multiplier changes.

## Exact table and rounding

Table: apd-experimental-v2.6R-isolated-product-v1; model: phase6-experimental-resource-v2.6R. BigDecimal retains decimal inputs and uses exact ordinary multiplication; displayed values round to four decimal places, with compact engineering notation for extremes. Only the final integer earned score is rounded, once, HALF_UP. Long score range is tested at the hardest supported endpoints; no int saturation or arbitrary multiplier cap is introduced.

The owner's prose example conflicts with the supplied authoritative table: Enemy HP 125% is 1.0897011360446645, while 150% is 1.1727900665646018. The supplied table is preserved, including 50% = 0.8256281968950766 and neutral = exactly one.

## Verification status at source submission

Local Java production and test compilation succeeded. 55 relevant JUnit methods passed, including all 357 exact positions, 18 neutral displays, mixed/endpoints, ordinary product, original challenges/victory inputs, RNG preservation, frozen saves, real recorder end scores, rankings ordering, all-six-class reload, permanent statistics, queries, exports and archive round trips.

An exploratory manual all-class invocation lacked the repository's Gradle working directory, fixtures, source-path properties and isolated setup; it reported 417 failures in 1724 methods and is not valid full-suite evidence. It also caught a cache-version assertion requiring an update after the intentional cache bump; that assertion is corrected and passes in the targeted run. Its log is retained locally. The first exact-source workflow (TEST245, run38911706996) ran all1728 core methods:1726 passed,2 failed because earlier cache-regression assertions still pinned builder16 instead of the deliberately incremented17. Both assertions are corrected without relaxing cache invalidation or raw-history checks. No APK was produced; Android was skipped. The corrected authoritative complete suite and actual Android assembly/instrumentation remain PENDING at this checkpoint. No new APK is called verified or delivered here.

Required Android evidence: 34 instrumented methods and 44 valid screenshots, including 357 rendered draft positions, a real touchscreen drag checked before release, narrow scrolling, existing gestures, score windows, original menus, save/continue and gameplay checks. Emulator evidence does not establish physical RedMagic 11 Pro / Android 16 behavior.

## Risks and limits

The numerical rewards are explicitly experimental and not competitively calibrated. Existing runs keep their original scoring authority and do not acquire these rewards retrospectively. Original integer SPD scores and legacy ranking serialization remain intact; adjusted scores use a separate long-valued result. Future lookup changes must introduce a new calculation version and preserve old authorities. Physical-device verification remains pending.

Last verified available APK before this candidate: TEST241 from b5d562ca37fff9ac628af3f4ade731e9fa7d83f1, full run 38894980574; 1716 core, 33 API34 methods, 42 screenshots. Rejected TEST242/243 attempts produced no APK. Preserve TEST241 and the published TEST231 fallback if this candidate fails.

## Follow-up verification and hardening

TEST246 (source556647a1e10600d77a0e2e9e8d578e799e9fe94a, run38912500386) produced an APK and passed1728 unique core methods, zero failures/errors/skips. Android instrumentation did not compile because the new visual test called Window.width(), which is not a Window API. No Android methods ran; do not call TEST246 verified or eligible for delivery. The independent current branch correction uses WindowGeometry.width, preserving the existing geometry helper.

Additional read-only probing reproduced two faults in newly added score validation: a negative original score threw IllegalArgumentException, and Long.MAX_VALUE with a >1 multiplier threw ArithmeticException. Corrected validation preserves the raw original record and leaves invalid/overflowing adjusted values unknown. Both probes now retain the record without throwing;13 focused lookup/telemetry methods pass, including a permanent regression using real encoded RUN_START/RUN_END logs and unchanged raw bytes.

The next candidate compares directly with the actually produced TEST246 APK, preserving246's exact technical history even though its Android verification failed. It must pass1729 unique core methods,34 instrumented methods and44 screenshot checks. Verification remains pending at this source checkpoint. TEST241 stays the last verified application; TEST231 stays the published fallback until new evidence and publication are confirmed.

## TEST247 retirement and next gate

TEST247 source `835bf2ba6d25898e2612900d90258b7b1dcf9a6d`, run `38913947244`, passed all 1,728 core methods, debug assembly and compiled-history checks, then produced APK artifact `12050814748` (38,607,556 bytes; SHA-256 `a0c4e72e9b7e401e5b382e0fc5572d69ae35df299428c0c18a86bee3420e9fa9`). A newer malformed-score hardening commit superseded it while its emulator was starting, so Android was cancelled before instrumentation; no Android methods or screenshots are attributed to TEST247 and it is not eligible for delivery. Retirement run `38914520685` preserved TEST247 as the actual previous produced APK. The next full candidate therefore compares against TEST247 and requires 1,729 core methods, 34 API34 methods and 44 valid screenshots.

## TEST249 verified candidate

At 2026-10-16T14:36:09Z, exact source `37a8f6d16cd497e13ed581e887ed9d4858fe97aa` completed full run `38914965700`: 1,729 core methods, 34 API34 methods and 44 valid 320x640 screenshots all passed with zero failures/errors/skips. Debug assembly, compiled-history comparison against actual TEST247 and crash scans passed. The APK is `ADHD-Pixel-Dungeon-CODEX-TEST-249.apk`, 38,608,287 bytes, SHA-256 `dcda79a5c6c83b1ac88e5294b3b134c8e1483109929b3a8da5365e7eadc8536e` (artifact `12051339058`). Full visual review covered all 44 captures, with full-resolution checks of live-drag, saved-summary, narrow non-neutral and neutral score screens.

Private staging run `38914965883` then succeeded without rebuilding the app. It revalidated exact source evidence, signer/application ID, rollback compatibility, Cleric lore enablement and required screenshots, producing staged artifact `12052344817` and evidence artifact `12052693358`. The 357-position table remains explicitly provisional; this evidence verifies implementation consistency and rendering, not numerical calibration. No public/private release or tester distribution occurred, and no physical-phone claim is made.
