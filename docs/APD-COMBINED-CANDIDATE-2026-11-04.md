# Combined owner candidate — November 4 evening / November 5 UTC

Target: November 4, 8:15 p.m. America/New_York (01:15 UTC). This is a target, not a promise of runner execution or a delivered APK. At the original source checkpoint, no new APK had been created. TEST361 subsequently stopped before assembly; no APK was created by that failed attempt.

## Changes and causes

- Prior combined grass loot collection is preserved: dew already on a traversed tile, grass dew, seeds and Petrified Seed runestones, including multiple drops on the same tile. Original item pickup costs, vial limits, enemy policy and drop RNG remain.
- Unidentified equipment descriptions record original floor and actual room/container/monster/shop/quest source. Key-specific labels use the real classes. Already-owned gear recovers only from reliable retained generation and public observation/acquisition history. Missing historical information stays unknown; no current pickup invents a past origin. Identification hides the descriptions. Dropping, transferring and save/load retain origin.
- Possible upgrade ranges describe original generation support, independently of the actual hidden level, curse or enchantment. Actual prize factories and history projections supply source-specific bounds. Later upgrades do not narrow original bounds. Unknown legacy generation conditions retain the full supported union or omit unsupported bounds.
- Attack stays visible at the bottom. Duelist ability occupies the next outer slot only for Duelist; stairs takes that slot for other classes. Cleric buttons use the two inward columns only for Cleric. Other actions and resume remain available.
- Ordinary wall search had no retained target/action context and marked walls before detection resolved. Hero now saves explicit wall/position and count of actual manual searches. Resume returns safely through known walkable cells and performs normal Search, stopping for enemies/unsafe routes. Passive searches do not create manual progress. New confirmed-negative evidence is recorded only after a genuinely guaranteed ordinary-door check completes. Older search marks are not retroactively proof. Terrain changes invalidate proof. Feedback distinguishes attempted, confirmed ordinary-door check and out-of-reach/unavailable; it never promises the absence of every kind of secret.
- Floor 5 Rat King room was already internally 5x5. It now puts the king on the furthest walkable tile from its entrance and all original gold in one unlocked center chest (internal 3,3). It consumes the exact original gold rolls in the original order. Other floors retain the old behavior. Existing generated floors are not repainted or rerolled.
- New ordinary Gameplay setting **Rat King Quick Search**, OFF by default and persisted independently of all18 rules/scoring. Only real main-branch Floor5 after Goo is defeated can invoke it. Disabled, Goo-alive, already-known entrance and every other floor use ordinary Search. The hero remains in place while the calculated ordinary walking-plus-search time runs through the real Actor scheduler; hunger/status ticks run, and only the Rat King door is revealed after full payment. Combat/interrupt discards a stale price without refunding elapsed time. Saved partial state cannot autonomously resume.

## Explicit boundaries and risks

Quick Search prices ordinary legal walking and normal search reach, including class/talents, terrain, equipment speed, common positive speed buffs and their expiry. It does not invent a minimum for unsupported changing effects (e.g. Slow/Cripple, momentum, time-freeze or shifting ring effects): it refuses with an explanation until they expire, or the player can turn it off for ordinary Search. No teleport or alternate ability optimization is claimed. Physical-phone timing and acceptance remain pending. Completed emulator checks are reported separately with the release verification. No phone reproduction is claimed. Existing gear without adequate retained history cannot accurately show an original room or generation range.

## Evidence

Origin-only source ea9fbeeaea8689766668ced7387c8c9842a40ca4 passed source-only run41931358660/job126928773316:2392 tests436 suites and Android debug/release/instrumentation/desktop compilation. Generation ranges/layout checkpoint3d07079cd passed local2431 cases before the latest wall/Rat changes. Combined local2473 cases passed after the approved Floor5 fixture update; three additional interruption cases and native UI assertions were subsequently added and are undergoing the final check. The first combined run failed only the two historical Floor5 golden comparisons; original archive retained, eleven new reviewed Floor5 references added, every other stable floor checked unchanged. These failures are retained, not hidden.

New tests cover real prize factories, no hidden-level reads, key-specific origins, ongoing already-equipped recovery, save/load/transfers/identification, class layout, manual vs passive evidence, remembered search/combat interruption, strict floor/branch gates, original gold/RNG conservation across seeds/orientations, shortest weighted walking/search cost, temporary speed expiry, real Actor elapsed time/hunger and unrelated secret preservation.

Actual released fallback remains private TEST358 (source51a0819552a1b8d6c888cb273057f50dc79f2635). TEST360 is accepted but unreleased. Preserve all existing assets. No candidate is verified or delivered merely because source tests pass; signing/application ID, full native evidence, display profiles and actual private downloaded assets remain required. Install over only; never uninstall or clear saves/statistics.

## Retained TEST361 prebuild failure and correction

Run41941525796/job126963269467 failed before APK assembly:2475/2476 cases passed, UimsdWarmCacheTest.changedPrewarmCancelsSupersededWorkerBeforeStartingReplacement failed at replacement non-null. Root cause: a retired worker could complete its filesystem stamp late, enter the cache lock and clear a newer READY entry before checking ownership. The worker now checks ownership before touching any cache entry. A deterministic paused-stamp regression reproduces the catalog destruction without the guard and passes with it. No original assertion is removed or weakened. Final suite inventory2477. Original run and failure logs are retained. No TEST361 Developer or Alpha-Testers APK exists.

The display job additionally failed reading an unrelated older run with GitHub HTTP502. Artifact discovery now selects only the exact requested source and retries only bounded transient502/503/504/429 responses; authorization errors remain failures. Remaining pointless old display work was cancelled after the producer failed.

## Actual artifact preparation — 00:51 UTC

TEST362 producing source34f5ffd2ea08bef701f60c95298edcfacc239e0c, exact private tagapd-alpha-preview-362. Developer producer41942317979/job126965923296 passed full2477 cases440 suites, Android debug/release/desktop compilation, assembly, JSON ABI and compiled-history checks. Alpha-Testers41942317989 passed full2477 cases and assembly. Artifact ZIP digests and APK sidecars independently checked after download. Developer SHA2561085bb39899a9c3c1ff5a9dd75ccaa0a73f05833aac8340b963bfdac16609f5a; Alpha-Testers SHA2569baa801d600ab72a4b9563d6bd687166eb9a7d4957f6c4503800540fbc764d41. V2 certificate matches067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e on both; shared assets are byte-identical except the expected Developer feature flag. Full apksigner/application-ID acceptance is a publication gate, not inferred from certificate extraction.

Display41942317994 passed all six profiles (five single-case profiles plus six-case API36 acceptance). Actual Alpha-Testers acceptance41942434237 passed three cases on the existing APK without rebuilding. New setting/description assertions ran on Developer API36 and Alpha-Testers API34. Eight existing API36 captures reviewed; the two new captures were reviewed from the actual320x640 tester artifact because the older API36 capture selector did not archive their names. This is recorded in the delivery evidence, not presented as API36 visual review of missing files. The full Developer API34 native run41942317979/job126967043040 is still pending at this preparation timestamp; private publication remains gated on all71 native terminal cases passing and actual new captures existing. No asset delivery is claimed from this preparation entry.

Existing TEST358 private release/assets preserved. No new source changes or APK rebuild for delivery tooling. Invocation timestamp is not independently available; workflow invocation, verification, later actual asset publication/download and final response link delivery are separate events. Source package excludes signing material and reports scans without rewriting history.

## Retained native fixture failure and exact-APK rerun — 00:55 UTC

Original TEST362 native run41942317979/job126967043040 completed70/71 cases. Its Cleric case still asserted one vertical column, conflicting with the approved two inward columns/two rows. Fixture sourcecb169aebc66ea1708f5772e267c1a84b2c97aa83 now checks that grid and explicit pairwise non-overlap. All original casting-cost, targeting, assignment, viewport, flipping and save/load assertions and all71 case identities remain. Full exact-APK rerun41944633239 is pending; it proves unchanged application inputs/source/hash and no application rebuild. Failed preparation41944467693 correctly stopped before publication on failed native evidence. No permission blocker and no new APK/version. Publication remains gated on the corrected full suite.

## Final verified publication

## TEST362 privately released and downloaded back — 2026-11-05T01:11:27.165513+00:00

Exact application source `34f5ffd2ea08bef701f60c95298edcfacc239e0c`, private tag `apd-alpha-preview-362`, release `434647799`. Both existing APKs passed2477 core cases440 suites; corrected complete71-case Developer API34 run41944633239 passed in one invocation on the same APK. Original70/71 geometry failure and TEST361 prebuild failure remain recorded. Six display profiles and three actual tester cases passed. Original signing/app ID preserved; no physical-phone claim. Publication `2026-11-05T01:09:57Z`; actual download-back verification `2026-11-05T01:10:01.747920+00:00`; final-response links prepared `2026-11-05T01:11:27.165513+00:00`. Invocation/scheduler metadata is not used as proof of delivery. Actual owner link sending is the final response, not preclaimed here.

Developer: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-362/APD-Developer-362.apk

Alpha-Testers: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-362/APD-Alpha-Testers-362.apk

Matching source: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-362/APD-Source-362.zip

Install over only; never uninstall or clear player saves/statistics. TEST358 fallback and all existing assets remain intact. Exact receipt: APD-TEST362-RELEASE-DELIVERY-RECEIPT.json. No additional APK assembled for fixture corrections or publication.

