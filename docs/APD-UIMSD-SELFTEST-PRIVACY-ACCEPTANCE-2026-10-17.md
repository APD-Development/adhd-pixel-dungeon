# UIMSD self-test privacy contract acceptance — 2026-10-17

## Scope and result

Complete current-source review of `UimsdSelfTest.java` and its `DatasetStore`, `QueryEngine`, `CellFormatter` and `RunLogWriter` interactions found no production defect. The device self-test already generated both finished and unfinished histories, but its retained unit coverage only asserted the combined pass result. Test-only commit `5a69400536f44ea04efe35edfb5398aefc0e4917` adds an independent two-run contract proving both sides of the privacy rule: an internal item identity is readable after a run finishes, while the identical field remains hidden for a run still in progress even when the query requests internal details.

## Verification

Core-only run [39001854306](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39001854306) at request head `d9238c2cb1e8a9345deee4c3b39b2963318066d5` succeeded from 2026-10-17T04:15:05Z through 04:17:41Z.

- Exact XML inventory: 350 suites, 1,769 tests, 0 failures, 0 errors and 0 skipped.
- `UimsdSelfTestTest`: 3 methods, 0 failures/errors/skips. The new finished/open privacy method passed in 0.005 seconds; the existing isolated-history cleanup and unwritable-directory failure checks also passed.
- Artifact `12089406326`: 829,331 bytes; digest and independently downloaded ZIP SHA-256 `sha256:8beb240b624e6b48e1578edab42fccbcf3cf28aac855960d5cf1b167dbb3d1d8`.

## Preserved state and limits

No production application source, executable APK build request, APK bytes, raw history, cache, save, statistics, scoring, gameplay, turns, RNG, signing identity or application ID changed. TEST264 remains the exact verified/staged private candidate; TEST262 remains the latest actually published owner APK. No physical-phone, owner-save install-over, publication or tester-distribution claim is made. This closes only the current `UimsdSelfTest.java` boundary, not the broader UIMSD audit.
