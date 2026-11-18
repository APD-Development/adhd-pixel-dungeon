# APD lunch-test preparation — October 15, 2026
Prepared at 11:26 a.m. EDT from the real approved Codex branch. Publication is reserved for the separate 11:55 a.m. PDF and 11:59 a.m. APK tasks; this preparation does not publish a release.

## Candidate and fallback
Reuse native TEST206, compiled source f33591d364598f75b07d62394f5a47bf6ba6432e, successful source workflow 38754451109 (14:46:51–14:59:03 UTC; 10:46:51–10:59:03 EDT). No new application build is needed. Candidate APK checksum recorded by the completed build: 566e29174c17c19f9a19f7429410d872a097c09c2aa4a1e4835548617a6c809c. The preparation workflow must independently compare actual signing, application ID, version and checksum against published TEST204 before accepting it for delivery.

Published TEST204 remains the verified playable fallback: source 245b21b7064a03b916e68f1992f94ca017f22fc0, build 38747671347, private delivery 38749970760, published 10:17:46 a.m. EDT. APK SHA256 0377d2fafe89ba6ed4741e3df18e574b46db4ff41377c75a53432e6294fb6ce3. Existing assets must remain intact. A failed preparation must not relabel TEST206 as delivered or verified for installation.

## Changes since delivered TEST204
Fixed fixed-point statistics presentation: means, medians, custom-question totals and rate numerators previously displayed raw stored hundredths/thousandths, making values 100 or 1,000 times too large. Presentation now converts to player units. Raw calculations and recorded values are unchanged. Production commits 327f2f040d983a7f4221116d86727f036f02563d and 4d92fd7b3970b6158b50c68302b00531c304c779; four retained regressions in 66e7188dcda2ff575132e23f2ef70b85861b1220.

Fixed lower-bound rounding: a known minimum of 2.46 could display as >=2.5. Lower bounds now round downward at the existing display precision, including negative, scaled and ordinary means/medians; exact values keep ordinary nearest rounding. Production e77a1c47c7f72e1f172c32fd8f5c4a6fe37f4af4; fifth regression 6e93ca2c80c04f9cdb7178f43bfbbe866b68fa54. TEST205 and TEST206 retain separate offline histories rather than attributing both batches to one APK.

## Earlier changes retained
TEST204 restores private Cleric lore activation with exactly 50 existing entries, preserves collection history and opt-out, and fixes inactive quick-cast buttons 2–4 after increasing the visible count. Actual pointer tests select each empty slot; normal casting, costs, targeting, cancellation and save behavior remain covered.
Source-bound disposable row caches reject same-name/size/timestamp raw-log replacement; independent cache writers use their own temporary files. Raw player history and saves are not deleted or migrated. Existing movement-command and visible boss-phase telemetry remains present.

## Tests and failures
TEST206 actual downloaded XML: 1,636 unique core tests and 28 unique API34 instrumentation tests, zero failures, errors or skips. Debug assembly and immediate-predecessor TEST205 history checks passed. All 38 screenshots decode at 320x640; the full contact sheet was visually reviewed. Automated Android/emulator evidence is not physical-phone evidence or a complete player run.
The five retained display regressions include before/after failures. Local corrected focused suite: 40 passed in 0.302 seconds; broader 623 passed in 27.964 seconds. Those local broad source guards use baseline121; the separate complete current-source CI above is the authoritative full check.
During development, an initial partial unit-conversion patch missed the per-unit question path and one regression failed; both sites were corrected before integration. The lower-bound regression failed before the rounding correction. Earlier Cleric Android fixtures manually updated hidden tags and masked inactivity; the retained test now uses actual scene frames and input. No failed run is presented as a passing build.

## Risks and remaining work
Check unusual negative/fixed-point statistics and conservative bounds on the owner's phone. Mixed-scale RATIO presentation is under investigation, not a confirmed fix. The entire UIMSD audit is incomplete: repository and local review checkpoints differ and require per-blob reconciliation. Historical completeness, broad gameplay combinations and remaining approved acceptance work must not be inferred from these test totals.
Registry fingerprint memoization is only a local benchmark prototype, not shipped. No production raw-history storage migration or compression change is authorized or included. Custom scoring remains deferred; original earned scores, all 18 controls, provisional limits, gameplay, RNG and save formats remain unchanged. No new online service, public release or paid build is activated.
Physical RedMagic/Pixel acceptance and the owner's actual save upgrade remain unverified. Install over the existing APD application; never uninstall or clear data. Preserve application ID com.charltonchris29.adhdpixeldungeon.indev and the existing signer.

## Timing and delivery state
This report records actual source checks completed before preparation. Workflow duration is elapsed CI time, not a claim of developer activity throughout inactive sessions. Signing compatibility and preparation are pending until the separate artifact-validation job succeeds; its verification appendix then records the actual result. Private staging alone is not publication. If validation fails, retain published TEST204 and report that failure in the handoff and delivery report.


## 11:55 a.m. EDT publication update
Lunch staging run38761060611 completed successfully at11:32a.m.EDT. It independently verified the actual TEST206 APK checksum, signatures, application ID and version compatibility against published TEST204; the candidate is ready for physical-phone testing, not physically tested. Staged artifact11985980608 contains the APK; evidence11985910681 contains the PDF, verification JSON, source counts and38screenshots. Both were retained privately; no APK is published by this PDF task.

Continued approved work after preparation: mixed-scale RATIO formatting was corrected in source f0b0cf7708a8d6af63cdb69392b22ab9e3962717, workflow38762160592. That workflow completed core/debug/history and Android jobs successfully; independent artifact recount remains separate. This correction is not in lunch TEST206.

Registry fingerprint memoization was committed in9a416d84840623c36eed84b6c238a5cdcf1468aa with two permanent compatibility/concurrency regressions in a7c2941240c6798d4740d5d76e2edb51505ef13f. Existing fingerprints and registration/startup ordering are preserved. The current31-kind local fixture measured20,000calls at212.746ms and82,811,880allocatedbytes before, versus1.065ms and224bytes after, with identical hash. Fifteen focused tests, one isolated cold-start deadlock test, and625broader local checks passed; broad source guards use baseline121. Full TEST208 source13e4c03baad7e4471a172aedf7aff4222ca7e749/workflow38763307954 remains pending at this report cutline. No Android performance, phone or publication claim; this change is absent from206.

Audit checkpoints were reconciled by exact current Gitblob:116inherited matching reviews,19changed files needing re-review and76pending,total211. Later separate review records still need merging, so this is not a final reviewed count or an audit-complete claim. Original109repository and135local checkpoints were preserved.

Another confirmed issue is LOCAL ONLY: a truncated/corrupt log or mismatched seal could label surviving attack counts as exact rather than lower bounds. Three original regressions failed; a local correction and six controls now pass0.142s, including enemy-table counts, clean unsealed logs and harmless garbage after a valid seal. No raw history was rewritten. This correction has not been committed or included in any candidate; cache invalidation/full regression/Android verification remain required. This report does not claim it is fixed in TEST206,207or208.

No application rebuild was performed to deliver this report. Production raw-history compression, experimental scoring and unfinished online services remain unchanged. These later investigations do not invalidate the separately verified lunch206artifact. Preserve204as the install-compatible delivered fallback.
