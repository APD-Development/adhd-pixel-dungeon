# APD continuation: fail closed on incomplete stair navigation metadata, November 12, 2026

## Completed and verified source work

This is new source-only work, not an assembled APK. The last independently verified published pair at the time of this handoff is APD460 (release `apd-460`). Check live GitHub release and Actions status again before planning any new build.

- Established source baseline: `ed65b8ba4bfa430e2b06b6ea8ecad816a16f3018`, after the verified stair null-transition fix (PR #8).
- Isolated branch: `codex/apd-stair-incomplete-metadata-20261112`.
- Implementation `06355deb11e03af79d47f9e33983472191a47516`: `StairNavigation.safeStep` refuses a route if the hero is null, the level's trap collection is unavailable, or the hero's non-null FOV array does not cover the selected cell. Previously those incomplete states could dereference null or index past the visibility array. The refusal is read-only and does not spend a turn or authorize a path. Fully initialized, valid state retains its prior behavior.
- Regression `3d336dd944a4c83ac8a4baf9b943df8ceebe4968`: three `StairNavigationTest` cases verify truncated FOV, missing trap storage, and null hero. The first two also confirm the same valid stair route becomes available when metadata is restored; neither failure schedules movement or consumes time.
- CI workflow `121a1563d8171385223af38e2f0c306fc7e3eca7`: [run 43247774516](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43247774516) completed successfully on the isolated branch. The actual workflow log confirms **3,272 core tests across 470 suites; zero failures, errors or skips**, and successful desktop, Android debug, Android release and Android instrumentation Java compilation. All three new regression cases are present in the required JUnit XML results.
- Scope comparison from the baseline to this tested head contained exactly three files: the production guard, regression suite, and isolated CI workflow.
- PR [#9](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/9) was made ready and then merged into the established development branch at `23364f1ede5752d23de34e6138973873de473187`. The branch head and PR merge status were read back from GitHub.

## Preserved constraints and independent pending work

No game mechanics, Search/talent range, hazard rules on complete state, scoring values, RNG, turn costs, save format, application ID, versionCode, original signer, existing APKs or prior build history were deliberately changed. This guard merely prevents the optional stair shortcut from proceeding with incomplete runtime metadata.

This session built **no new application APK** and published **no new release**. The frozen APD460 Developer/Alpha/source pair remains available at https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/tag/apd-460, but is not relabeled with this fix. The independent APD461 malformed-observed-terrain effort remains on draft PR #6 and its own branch. Source and candidate build gates succeeded, but Android run `43240714587` ended in failure because Alpha-Testers `WallGeometryDeviceTest` failed while five other cases passed; publication `43240803227` then failed its prerequisite check. This session did not edit, retry or override that separate workflow. Do not claim APD461 delivered without new successful native and publisher evidence.

Future sessions should inspect current branch, merge, release and run records before acting. Continue approved, independent source work, coordinate with PR #6, and create a new signed Developer/Alpha build only after its exact source and required Android/asset verification gates. Physical RedMagic install-over and sound timing remain unverified.
