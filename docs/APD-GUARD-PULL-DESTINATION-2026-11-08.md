# Guard pull destination validation

Owner report: APD378, Floor 7, screenshot 19142.jpg. The Prison Guard pulled the hero onto its own tile. The affected save and animation recording are unavailable; the exact sequence on that phone has not been reproduced.

Inspected `Guard.chain`, both animation callbacks, `Pushing.Effect.update`, `Actor.findChar` and `Level.occupyCell`. The chain checks occupancy when selecting a destination. Its later arrival callback previously assigned the selected position without checking occupancy or terrain again. The guard releases its turn from the chain callback before the pushing animation finishes, leaving a stale destination possible.

Four regressions invoke the real arrival method: guard now occupies destination; another floor mob now occupies it; destination becomes solid; ordinary open destination. Before the production change, the first three reproduced invalid placement and failed, while the ordinary pull passed. An initial fixture error (uninitialized floor mob collection) was corrected before recording that reproduction. Logs: `/workspace/apd-guard-pull-before-fixed-fixture.log` and `/workspace/apd-guard-and-description-check.log`.

Arrival now checks the guard position, registered actor occupancy, current hero position, authoritative floor mobs, solid terrain, bounds and large-character space. If invalid, it leaves the character on its current tile and restores its sprite there. It does not choose a replacement tile or apply landing effects/Cripple. Legal pulls still call the original occupy-cell handling and apply Cripple. No generation, save, damage, chain-use or turn-cost formulas changed.

Headless regression checks passed. Android rendering, actual device reproduction and an APK containing this correction remain unverified. This is not a claim that the entire item-description audit is complete.

## Underlying scheduler correction

Further inspection identified an APD377 regression: `Actor.remove` clears the current actor to recover cancelled character animations. `Pushing.act` deliberately removes itself before its visual finishes, while its landing callback is still pending. Applying that cancellation recovery to `Pushing` prematurely reports the queue idle, permitting another actor to enter the landing destination.

`Actor.remove` now preserves that intentional Pushing wait. The existing landing callback still calls `next` at completion. Other removed actors retain the APD377 cancellation recovery. A real `Pushing` test reproduced premature release before correction; the corrected test also exercises the actual scheduler thread, real visual update, landing callback, and next actor at its original turn timestamp. Cancelled-Ghoul recovery and live-attack controls remain tested. Source arrival validation remains as a second safeguard.

Initial new test compilation used protected Group members directly; the test fixture was corrected without changing production code. Before-fix failure: `/workspace/apd-pushing-wait-before-fixed-fixture.log`. Targeted corrected checks: `/workspace/apd-guard-pushing-root-fix.log`. Full combined-suite result is recorded separately when complete. No APK or Android acceptance is claimed by these headless checks.

Full combined check passed: 2,997 tests across 456 suites; zero failures, errors or skips. XML and log copied to `/workspace/apd-description-guard-evidence-20261108`. This includes the actual scheduler/Pushing regression, original removed-Ghoul tests, four destination tests, current description tests and unrelated gameplay regressions. No Android SDK/emulator is present in this workspace.
