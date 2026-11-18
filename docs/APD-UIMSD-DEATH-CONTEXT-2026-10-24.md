# UIMSD recorded death-context first slice — 2026-10-24T22:07:18Z

## Scope
This bounded core-only slice wraps the existing Deaths dataset and public RunTurnBrowser observations. It does not add or rewrite telemetry, raw history, saves, scoring, gameplay, turns or RNG.

## Implemented contract
- An adjustable 1–1000 turn window ends exactly on the recorded death turn and clamps at turn 1.
- Only public observations already recorded for the exact case-sensitive run are listed.
- The recorded death is explicit, including its cause when present.
- Revived, final, non-final and unknown post-death states remain distinct. Contradictory fields fail closed to unknown.
- Missing death turns produce no reconstructed timeline.
- Output states that observations are incomplete lower-bound evidence and do not prove survival, causation or absence of other events.

## Regression gate
Five new DeathContextTest methods require exact boundaries/chronology, exclusion of other runs, revival-versus-final wording, missing older data, bounded inputs, start clamping and qualified non-final output. Full-suite target: 2069 methods /401 suites /0 failures /0 errors /0 skips.

## Core verification
Exact source `dea4a490158be46e24a2bd116c2f158de55de391` passed [run40137272801](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/40137272801): 2,069 methods /401 suites /0 failures /0 errors /0 skips. Dedicated retained XML confirms all five DeathContextTest methods. Artifact `12578106262` is 976,925 bytes with digest `sha256:59b48feefb055637ec0207ebcbd58601920f98deb7f31cae94157c8b63633f03`.

## Acceptance boundary
The core model is accepted. It has no native UIMSD surface, API34/display verification or APK request. TEST312 remains the latest emulator-verified private pair; TEST262 remains the latest actual owner publication. Physical-device Back latency remains unmeasured. The executable APK request stayed byte-identical at `42af2cfe9a72655b5f1e7a2dbdf4777219b809e0`.

## Native row action — 2026-10-25T14:00:56Z
Implemented a Deaths-row action with 5/10/25/50/100-turn presets and validated custom 1–1000 input. It uses the same verified DeathContext authority, rejects stale history, restores through existing window contracts and keeps public-only/missing-data caveats visible. Four focused core methods bring the pending full-suite target to 2,073 methods /402 suites. Native/API34/visual acceptance and any APK remain pending; the executable APK request is unchanged.

## 2026-10-25T14:09:33Z native candidate checkpoint

The native `Death context` row action is full-core green at source `9815d044b9bcd7a9342cd74f09b1ac817aee393f`: run `40200352834` executed 2,073 methods in 402 suites with zero failure, error or skip. Retained XML confirms all four `WndDeathContextTest` cases.

TEST313 now adds a real API34 binary-log path for two distinct recorded deaths in one run: one explicitly revived and one final. It must open both actual Deaths rows, touch the native action and 10-turn choice, retain the outcome/caveat text across scene restoration, capture `uimsd-death-context.png`, and leave the raw log byte-identical. Full debug, 55-method API34, 81-image visual, history, player, identity, save, death-order, cold-restart and display evidence remains pending. TEST312 stays current until all evidence passes; TEST262 remains the newest APK uploaded to GitHub as a release.

## TEST313 native/API34 acceptance — 2026-10-25T15:23:12Z
The native Deaths-row action is accepted on emulator evidence. Application source `eb0053283be631c0286bda7d0efbeb3c13590a11` passed 2,073 core methods /402 suites, API34 55/55 and 81 reviewed PNGs. The real persisted-row method opened both a revived death and a later final fire death through `WndUimsdRow`, selected the 10-turn window, preserved explicit outcome/non-causation wording across scene restoration, captured `uimsd-death-context.png` and proved the raw file byte-identical.

Developer/player TEST313 APKs match source, identity, signing and history requirements. Direct TEST312 comparison preserved all 339 predecessor history records and added only TEST313. Final display run `40205712754` reused the exact TEST313 developer APK and passed all five profiles; earlier emulator GL/input failures remain recorded. TEST313 is now the newest emulator-verified pair in GitHub Actions artifacts. TEST262 remains the newest APK uploaded to GitHub as a release. Nothing was sent to testers and no physical-phone result is claimed.
