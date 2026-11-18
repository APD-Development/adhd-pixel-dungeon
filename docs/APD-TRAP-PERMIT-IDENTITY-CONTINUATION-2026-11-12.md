# APD continuation: reject incomplete trap confirmation identities (2026-11-12)

## Completed, independently verified source change

This is verified source-only work, not a new Android APK. Resume from the latest live branch and AGENTS.md rather than assuming this checkpoint remains the newest.

- Previous established development head: `bbf7ccbe985126f40bd27140226b8c6156587ae1`, after the verified stair-route metadata safety merge.
- Isolated branch: `codex/apd-trap-permit-null-identity-20261112`.
- Implementation: [`9ed9c01a`](https://github.com/APD-Development/ShieldingShouldHaveWorked/commit/9ed9c01a04aa6c2d2323c1932db1b8295a7db517). `TrapStepPermit.allow` now clears an older grant and refuses to arm when its level, trap, or action identity is missing. `consume` only accepts a stored non-null level, trap, and action with the original exact object identities, origin and destination. The existing one-time clear-after-consume behavior remains.
- Regression tests: [`94d4d65a`](https://github.com/APD-Development/ShieldingShouldHaveWorked/commit/94d4d65ad1b85388cfc78cec8f51072c6194ce31). Four new `TrapStepPermitIdentityTest` cases cover a null level, null trap, invalid confirmation revoking a previously valid grant, and the normal single-use/exact-destination path.
- Isolated CI workflow: [`077a6144`](https://github.com/APD-Development/ShieldingShouldHaveWorked/commit/077a61441e0ed4ca90affb7b51870763efd62875). [Run 43250200835](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43250200835) completed successfully on the exact source. Raw job output reports **3,276 core tests / 471 suites**, zero failures, errors or skips, and passes the four exact new regression cases. Desktop classes plus Android debug, release and instrumentation Java compilation succeeded. The core JUnit artifact is `13959231262` (`apd-trap-permit-identity-core-results`, 524,831 bytes) and the upload log reports artifact SHA-256 `ae56b5f11ea7df374389ca5f9eca20a0f470cedb471d90cbd9284b3316f6d818`.
- Scope comparison with the source baseline was exactly three files: `TrapStepPermit.java`, `TrapStepPermitIdentityTest.java`, and the isolated CI workflow. No other application, asset or scoring file was changed.
- [PR #10](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/10) was verified, marked ready, and merged into the established branch at [`b10ae1d3`](https://github.com/APD-Development/ShieldingShouldHaveWorked/commit/b10ae1d3d5c975f74a2cea4adb77d56ddb641ed9). GitHub readback confirmed PR merged and branch head reached that commit.

## Preserved decisions and remaining dependencies

No gameplay mechanics, turn costs, trap hazard choices on valid state, balance, RNG, saved history/schema, scoring, signer identity, application IDs or previously accepted APKs were intentionally changed. The correction is a fail-closed guard for impossible/incomplete confirmation identities, not a new trap behavior.

No new game APK, source archive or GitHub release was built or published by this session. Last independently confirmed GitHub-published pair at this checkpoint is APD460: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/tag/apd-460.

Leave the unrelated APD461 malformed-observed-terrain candidate and open draft PR #6 isolated. Its source and candidate build gates passed, but [native run 43240714587](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43240714587) failed the Alpha-Testers WallGeometryDeviceTest while the other five native cases passed. Its [publisher 43240803227](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43240803227) failed the resulting prerequisite. Do not silently skip or override those gates, and do not call APD461 released. A successor APK must be assembled from an exact agreed source only after any required prior branch work is reconciled, then pass signer/source/native and release verification.

Physical RedMagic install-over testing and audio remain unverified. Continue independent approved work from the latest verified live repository state; do not revisit completed APD458-460 or stair investigations solely because earlier handoffs remain in the tree.
