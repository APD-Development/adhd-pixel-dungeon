# TEST255 UIMSD repeated-label group comparison correction — 2026-10-16

## Controlled reproduction

The current report comparator converted group B's detail lines to a map keyed by the player-facing label. Dataset definitions allow the same label in different sections, so the later line replaced the earlier one. A supported two-group fixture with two separate statistics both labeled `Average` expected group B values `20` and `200`; the first row instead displayed `200`.

Diagnostic commit `df6d2a4f0e898f52c0e1227ed19c7c71b23b2c63` ran the one-method regression in workflow [38964285358](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38964285358). JUnit recorded one test, one failure and zero errors: expected `20`, received `200`. Artifact `12073194262` (ZIP SHA-256 `137c7a8b59b13049b65e0bee815c57069668bb0bb1f69dcb67d95479d7ea9669`) preserves the XML and HTML report. The diagnostic workflow itself concluded failure because its post-check expected JUnit's compact diff text rather than the actual bracketed diff; this does not relabel the retained application assertion as passed.

## Bounded correction

`ReportBuilder.compareGroups` now pairs the two detail sheets by their shared dataset-definition order. Section headings remain omitted from the comparison table, repeated display labels remain supported, and a missing counterpart still displays unknown. No raw history, query result, cache, save, gameplay, RNG, turn, slider or score calculation changes.

The retained regression remains in the normal core suite. The full candidate gate must compare the immediate previous compiled TEST254 source `832c1a5679a8cd216e8c57638f1bc0fe2442fdfe` / run `38949555542`, verify all reviewed history paths, build debug, and rerun the existing API 34 visual/play acceptance. Until that completes, this is a pending source candidate; privately published TEST254 remains the playable fallback. No physical-phone, owner-save or automatic tester-distribution claim.

## Full TEST255 verification

Source `a11b32f8067765e6229830c929d07d262de34800` completed [run 38964629976](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38964629976) from 2026-10-16T20:46:15Z through 2026-10-16T21:01:40Z with conclusion success.

- Core: 1,737 tests, zero failures/errors/skips; artifact `12074118076`, digest `sha256:c3edd523764e186eac6b6dbd5a30d3f6914f5eb2b9bf1efaaea356148972147b`.
- Build/history: debug APK assembled; embedded record and complete immediate-previous compiled TEST254 history passed.
- API34: 36 methods, zero failures/errors/skips; Android artifact `12073874781`, digest `sha256:38a7f49e8a8facd2b4ba07ab1de85b5e3b7ba30578568fe8526b38cb11fbd1ba`.
- Visual: 49 PNG captures, each 320×640. All 49 were manually reviewed with no blank/corrupt or obvious clipped-regression frame.
- APK: artifact `12073883782`, digest `sha256:8d19fa3cd704e5ab6500ee8b4e11fca7dd0d22d83fbdeb306212179c6f0ed21b`; `ADHD-Pixel-Dungeon-CODEX-TEST-255.apk`, 38,611,585 bytes, SHA-256 `71f0f1d77a0952f9fca121ba5e7ac31bf3488900023109dfe9bcea5812da32d5`.

## Staging outcome and limits

[Staging run 38964630161](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38964630161) failed before packaging/publication in both its initial attempt and failed-job retry. Its evidence selector downloaded stale preparation artifacts; `tools/apd_existing_build_evidence.py` then rejected unequal application trees at line 67 with `Publication commit changes application/build inputs`. Packaging and publication remained skipped. Do not rerun the unchanged stage.

TEST255 is verified as a private workflow artifact, not a published owner delivery. Published TEST254 remains current and TEST250 remains a prior fallback. No tester distribution, physical-phone, owner-save or full human boss/class playthrough claim. The correction does not change raw history, caches, saves, all 18 sliders, active v2.6R scoring's 357 exact values/ordinary multiplication, original scores, gameplay, mitigation, RNG or turns.

