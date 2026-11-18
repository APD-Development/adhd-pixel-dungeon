# Cloak of Shadows explanation continuation

## Scope and actual source

Continue the approved mechanics-description audit under the owner's clarified interface limits. No new panel, control, setting or automatic opt-in. Cloak descriptions use ArtifactExplanation's existing ordinary text and artifact-specific details retained pending owner review. APD372 remains the delivered APK; this continuation is source only and does not consume another game-APK build.

Inspected actual CloakOfShadows.execute, cloakStealth.attachTo/act/dispel/detach, upgrade, passive/external charging and save/restore; Artifact resource methods; Invisibility.dispel and MagicImmune interactions. New CloakExplanation explains invisibility, normal1-base-unit activation, first scheduled charge payment then4-update budget, due-payment exhaustion/interruption, manual versus involuntary cancellation, separate invisibility sources, live level-dependent Cloak progress and threshold, capacity and recharge eligibility. No fixed real-world duration/free-attacks promise. Progress is explicitly not hero XP.

CloakOfShadows production change adds only a read-only active-countdown getter and the required APD copyright below preserved original headers. No formulas, tick order, target selection, actions, costs or serialization keys changed. Current saved countdown is read rather than advanced or inferred from charge count.

## Meaningful regressions

CloakExplanationTest isolates hero/level/rules, item identities, Catalog, Badges, Notes and Journal flags. It reuses the established quiet-renderer fixture and spies only the UI-dependent Hero interruption callback; actual Cloak/Buff/Artifact logic runs.

1. Actual first payment and four-update cadence; repeated description inspection preserves full saved hero/Cloak state and RNG; hero action time does not advance from inspecting.
2. Actual due-payment dispel costs1 versus not-due dispel without additional charge; exhausted effect detaches and invokes interruption; manual detach does not invent a charge cost.
3. Actual paid-update progress and single-upgrade/capacity behavior across6 Cloak levels ×4 hero levels, including levels6/7 transition and maximum upgrade; prediction compared with actual stored XP and consumed threshold.
4. Actual compressed Bundle round-trip retains active countdown, charges and progress; inspection does not alter the restored item.
5. Unknown item/no-hero cases withhold unavailable values.
6. Actual passive charger gain pauses while active, while external recharge power still adds its own fractional progress, then passive gain resumes when uncloaked.

Targeted first5 cases plus existing mechanics regressions passed: /workspace/apd-cloak-targeted.log. Final6-case/full-suite validation: /workspace/apd-cloak-full.log, result recorded below after completion. No native/phone test or new APK claimed. Actual execute animation, visibility against every enemy, special talent/subclass paths and translated presentation remain outside this narrow regression claim. Continue next approved facet after preserving verified progress.

Full local result2946 tests/450 suites, zero failures/errors/skips. Raw XML /workspace/apd-cloak-local-junit.zip. Exact-source Android/desktop/full-core gate requested. No new game APK.
