# UIMSD shared exact run/turn-window checkpoint — core verified 2026-10-21

## Result

Application source `a66182becd893574977193b878cd7c6cd3362954` introduces immutable `RunTurnWindow` and routes the accepted equipment-following-damage calculation through it. The specification owns:

- exact case-sensitive run identity;
- an excluded anchor turn converted once to canonical inclusive bounds `anchor+1 ... anchor+N`;
- overflow and invalid-input rejection;
- the visible “strictly after … through … inclusive” interpretation;
- appending run/turn filters without replacing dataset, scope, disclosure choice or existing predicates.

The concrete `EquipmentWindow` keeps its existing N=1–1000 guard, hero-target damage rule, shield-inclusive recorded amount, coverage checks, exact contributors, lower-bound wording and unknown/overflow behavior. Equipment predicates still choose the anchor and are not silently copied onto damage rows.

## Verification

Candidate run [39683649159](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39683649159) completed successfully on `a66182b` and produced artifact `12384748603` (957,364 bytes; `sha256:2bf29de3d7844272b10e4b2d731c6147b0c8914f9180d0121f9bfc4649e55393`). The artifact-download handoff returned HTTP 503 on repeated retrieval attempts; that transient delivery failure is retained rather than treated as test failure or silently ignored.

Evidence-only commit `c2eec3d2e321d71ac018a51cd3be1c4280efdc56` added a fail-closed XML summary step. It does not change application source. Exact-source rerun [39684507117](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39684507117) passed and emitted:

- **395 suites / 2,031 tests / 0 failures / 0 errors / 0 skipped**;
- all six focused cases: exact exclusive/inclusive boundaries; case-sensitive run identity; scope/disclosure/existing-filter preservation; QueryCodec round-trip at `Long.MAX_VALUE`; invalid/overflow/missing-part rejection; shared interpretation wording;
- artifact `12386420312`, 959,407 bytes, digest `sha256:87ba0569d7835c776ec5b7c5cb3bc5ef02762507470fe0596dcf83a379eb9718`.

The original six-method `EquipmentWindowTest` suite and all earlier core regressions are part of the same successful full run. No tests were disabled or weakened.

## Scope and remaining work

This is a reusable backend specification plus one retained integration, not a general temporal-question feature. It does not add another anchor→target pair, parsed temporal language, new UI, recording fields or historical migration. General correlated questions, timeline/death context, broader experiments and Android-native acceptance remain pending.

No APK was built, so no new API34, visual, history-diff, player-gate, signer or physical-phone evidence exists for this checkpoint. The executable APK build request stayed at blob `b618df67d45a4b07e5105b3e6c222e8e8ad1ac2a`. TEST304 remains the newest produced/emulator-verified private pair and TEST262 remains the newest actually published owner APK. No staging, publication or distribution occurred.

Next safe step: connect a second explicit recorded anchor and target dataset through `RunTurnWindow`, retain exact evidence/unknown semantics, and add native acceptance before requesting a new candidate.
