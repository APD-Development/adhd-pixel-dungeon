# Blessed Ankh revival explanation — source verified

Prepared: 2026-11-06T20:00Z  
Source commit: `bc88ced738d2152031fae053bad308b9c892ade3`  
Parent: `c982143c3ff1c5bbeb0c1651bb02ffea426ae7f0`  
Status: source-verified; no APK

The live blessed-Ankh path previously assigned `HT / 4` directly inside `Hero.die`, while the detailed Ankh panel independently repeated that arithmetic. The verified correction exposes the identical integer-division expression as `Ankh.blessedReviveHP(maximumHP)` and uses it at the original live assignment point and in the read-only panel. No operation order, side effect, source attribution, turn, RNG draw, cure, invulnerability, item consumption, save data or balance value changed.

The panel now states the approved rule explicitly: Ankh revival is a revival event, not ordinary HP recovery, so the Custom Dungeon Rules Healing percentage does not multiply it. Regression coverage exercises Healing 0%, 25%, 100% and 200% across maximum HP 20, 21, 99, 100, 101 and 1000, checks exact integer truncation and confirms inspection does not execute the revival.

## Verification

- Workflow: [42285061371](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/42285061371)
- Job: `128123626611`
- Completed: 2026-11-06T20:05:50Z
- Core: 2,859 tests / 446 suites / 0 failures / 0 errors / 0 skipped
- Compilation: desktop classes; Android debug, release and instrumentation Java — all successful
- Artifact: `13537291623`, 502,465 bytes
- Artifact SHA-256: `98a7c4b2aaf489fe9d082edb8866124f74003b646870a70506ddfe6caefc6302`
- Download inspection: digest matched; embedded `qol-verification.json` matched the source commit and totals; the new `CustomHealingSliderTest` case was present and green
- APK assembled: false
- Physical phone tested: false

TEST369 remains the current delivered release and does not contain this later source correction. No APK build request, publication or distribution was performed.
