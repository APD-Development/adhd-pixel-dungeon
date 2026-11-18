# Registry fingerprint memoization — October 15, 2026

## Measured cause and correction
FactDatasets resolves registry identity repeatedly while reading observations. FactKinds.fingerprint previously walked all declarations and allocated concatenated field strings on every call, even though FactKind and its fields/lists are immutable. The approved branch now retains the identical fingerprint under the existing registration monitor and invalidates it only after a new declaration is accepted. Hash algorithm, field ordering, cache identity and storage formats remain unchanged. Built-in class initialization still occurs outside the registry monitor, preserving the earlier startup-deadlock fix.

Production commit 9a416d84840623c36eed84b6c238a5cdcf1468aa; retained tests a7c2941240c6798d4740d5d76e2edb51505ef13f. No raw-history deletion, save migration, scoring, gameplay, RNG, slider or online-feature change.

## Evidence
Fresh current-source fixture has 31 built-in kinds. After 1,000 warmup calls, 20,000 original calls measured 212.746395 ms wall / 211.377398 ms thread CPU / 82,811,880 thread-allocated bytes. The corrected fixture measured 1.065299 ms wall / 1.074950 ms CPU / 224 allocated bytes including measurement overhead. Both produced fingerprint 1beb28f016f77e54. These are Java21 local microbenchmarks, not Android/physical-phone/end-to-end performance or bytes-per-turn measurements.

15 focused registry/dataset tests passed in 0.238 s. The retained compatibility test checks the original uncached hash after registration, same-instance registration and duplicate rejection; it fails against the original repeated-allocation implementation and passes after correction. The concurrent test coordinates 20 registrations with an independent reader and propagates worker failures through a Future, checking completed registrations never leave a stale identity. Existing row-cache invalidation tests remain intact.

The existing isolated-classloader cold-initialization deadlock regression passed separately in 0.099 s. Broader local suite passed 625 tests in 29.496 s; its source guards use baseline121, so it does not replace full current-source CI.

An initial focused run passed14/15 but failed the manifest guard because the local resource fixture predated new move.command telemetry. Replacing only that local fixture with the unchanged current repository manifest yielded15/15; no production assertion or manifest was weakened.

## Verification boundary
Full current-source Android/debug/history/core verification remains required for this meaningful production batch. The ongoing TEST207 source gate predates these commits and must not be credited as testing memoization. Preserve its immediate-predecessor history if it produces an APK. Do not alter or rebuild the already staged lunch TEST206. No memoization APK has been published; no physical-device or owner-save claim.
