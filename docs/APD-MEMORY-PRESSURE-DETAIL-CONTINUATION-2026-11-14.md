# APD memory-pressure detail continuation, November 14, 2026

Recorded Eastern: Saturday, November 14, 2026 at 10:07:53 PM EST

## Scope and ownership

Source baseline: `c8b40031d3802f5c7dcf014aa15f49468df56fd4` on `codex/apd-phone-fixes-2026-10-09`, observed immediately before creating isolated work branch `chatgpt/apd-memory-pressure-trace-20261114-2200`. This is a narrow follow-up to merged PR #147's bounded local heap diagnostics for the owner's C467 OutOfMemoryError. The OOM allocation stack in CharSprite.update does **not** establish the retention source; this change makes future pressure callbacks more useful, without claiming a crash fix.

## Evidenced gap and source changes

The existing bounded memory history persisted `phase=android.trim_memory` but omitted the Android trim **level**; the separately logged `android.trim_memory level=N` recent event could be evicted by high-volume input. The existing `onLowMemory` callback did not record any memory-history sample, so its evidence was likewise lost after input flood. Keep the phase and all old memory sample numeric fields intact. Add an optional, integer-only `trim_level=N` suffix to the Android trim sample, capture the low-memory callback in bounded memory history, and add one actual filesystem regression proving both survive 500 intervening touch events. No gameplay, score, saves, archive, signing, variant, or original APK changes.

## Execution receipts and limits

- Start development head: `c8b40031d3802f5c7dcf014aa15f49468df56fd4`.
- Source code and regression authored on isolated branch through `242e7be77e04c021a91458462e6af9edeafee6f5`.
- This document is written **before** new exact-source CI and Android compilation. No test result is claimed here.
- The earlier C471/P471 signed-pair workflow [43667638390](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43667638390) passed on the separate previous source `02e104dde6f34dc4c0eeb3a713610651058c2d1d`. It does not test this follow-up.
- There is no new signed, natively accepted or published APK in this work package. The C471/P471 candidate and its pending native acceptance remain untouched.
- Next safe step: require a full core suite and desktop/Android Java compilation for the exact source; inspect failure artifacts if needed; only integrate through a current-head-checked non-force fast-forward after success. Keep all other claims and releases intact.

## Final verified outcome

Recorded Eastern: Saturday, November 14, 2026 at 10:13:24 PM EST.

- Source follow-up: `SupportDiagnostics.memoryEvent(String,int)` retains a trim-level suffix in the independent bounded heap-history ring. `AndroidLauncher.onTrimMemory` passes the actual OS trim level, and `onLowMemory` now records a heap sample. There is no change to existing one-argument memory-sample output, gameplay, saved data, scoring or edition boundaries.
- Commits on isolated source branch: implementation `e24a1eb86bea05cf1218422a051b70f735ef8177` and `5a65e0c1cf075a2c411e31edd0fda531008e361d`; filesystem regression `242e7be77e04c021a91458462e6af9edeafee6f5`; initial evidence `aa538a1747c6a0a53fce7e956a79785118f251eb`; exact CI workflow `b00b234ed103e54b51035a62743114de8d072063`.
- Exact [Actions run 43670968548](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43670968548) completed with **3,462 core tests / 519 suites, zero failures, errors or skips** and successful desktop, Android debug/release and Android instrumentation Java compilation. This is the new regression's genuine new-source run, not reused earlier C471 results. Job `132680689284`; JUnit artifact `14143978776`, sha256 `73a6fdeb7c1331680d56239b866f608e79e37d4b799d3d1da8392d8bdef51fca`.
- [PR #148](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/148) confirmed merged at **November 14, 2026, 10:12:38 PM EST** by guarded expected-head, non-force update. Integration commit `a44f8a6a1f55e9b4cf25fb799578aedf5c6b48c3` has parents `c8b40031d3802f5c7dcf014aa15f49468df56fd4` and `b00b234ed103e54b51035a62743114de8d072063` and exact tested tree `d4ee207b84474a7339d418f62cc681c6a9f19f41`. GitHub branch and Android call-site readback confirmed merged behavior.
- **Build/publication:** no new game APK assembled, signed, natively device-tested, install-over-tested, or published in this follow-up. Earlier C471/P471 signed candidates remain in workflow artifact `14142573038` from successful [run 43667638390](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43667638390) on **different** earlier source. They do not contain this change and are not native/release-accepted here. Existing qualified builds are untouched.
- **Blockers and next safe work:** this source-only correction improves future OOM evidence; it does not establish the retained-object cause or fix C467's OutOfMemoryError. Native Android/RedMagic lifecycle acceptance and the existing original-signer/install-over/save-integrity/release procedure remain mandatory before a new APK can be called qualified. Do not duplicate this correction or overwrite concurrent build candidates.
