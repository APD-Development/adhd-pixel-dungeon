# Supply Ration scaled-healing feedback — source verified

Prepared: 2026-11-07T15:04Z  
Source commit: `d94ed511ec99d90c9fcd99470e852b0120c7be1e`  
Parent: `e5845504982a0cb3f9f8cf985d77cce29556c6cc`  
Status: source-verified; no APK

## Confirmed defect

`SupplyRation.satisfy` already scaled its base 5 HP offer exactly once through `CustomHealingHooks.amount`, then capped at maximum HP. Its floating healing text nevertheless always displayed `5`. Healing 0%, 25%, 200%, and capped non-neutral heals therefore reported a value different from the actual HP gained.

## Bounded correction

Only the displayed value now uses the existing pure `CustomHealingHooks.display(hero, before, 5)` helper. Non-neutral settings display the actual HP delta; neutral 100% preserves the original upstream text even when capped. The base request, fractional-credit accumulation, final cap, combat telemetry, nutrition, Cloak recharge, action time, talents, RNG, turns and saves are unchanged.

## Verification

- Workflow: [42384738067](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/42384738067)
- Job: `128439632946`
- Completed: 2026-11-07T15:09:39Z
- Core: 2,887 tests / 446 suites / 0 failures / 0 errors / 0 skipped
- Compilation: desktop classes; Android debug, release and instrumentation Java — all successful
- Artifact: `13579340583`, 508,428 bytes
- Artifact SHA-256: `a1db3504c08abd6e856b709ef2dd6cbfaaf9c2ad7ba1efa3ef1eeb9a406b644f`
- Download inspection: digest matched; embedded `qol-verification.json` matched the source commit and totals; all 446 XML suites were present; `supplyRationShowsScaledAndCappedHealingWhileNeutralKeepsOriginalText` was present and green
- APK assembled: false
- Physical phone tested: false

The real protected `satisfy` path is covered at Healing 0%, 25%, 100% and 200%, including actual HP, floating text, capped 200% reporting 1, and capped neutral retaining the original 5. Owner APK hold remains active; TEST369 remains current and does not contain this post369 source correction.
