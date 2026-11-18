# TEST161 artifact-derived damage and Corruption save-integrity candidate

- Execution checkpoint: 2026-10-14T16:52:55.080Z
- Base head: `27835d898d1a09a462c0cc49f8cfc3915d945497`
- Reconciled and preserved the independent Corruption fractional-save correction already on the branch. TEST160 does not cover that later source change.
- Guiding Light damage from artifact procs now uses the actual hero as Player Damage origin while retaining `GuidingLight.INSTANCE` as the original source.
- Cape of Thorns still samples one deflection roll and reduces incoming damage by that original amount; only its outgoing retaliation is scaled, with the cape buff retained as source.
- Skeleton Key's two Blast Wave paths were read completely and remain non-damaging movement because collision damage is false.
- Added four retained artifact-derived tests for all 20 percentages, neutral next-RNG/source identity, exact caller/ordering guards and Skeleton Key exclusion. Together with four current Corruption save tests, expected core total is 1572.
- No score, save-format key, inventory, signing, application ID, release, delivery or slider-enablement change. Exact-source core/debug/immediate-previous TEST160 history/26 API34/34 PNG verification is pending.


## TEST161 full verification outcome — 2026-10-14T17:13:21.216Z
- Exact source `9088ca30acde28282454598b1e649a3a70156b56`, run `38601864296` (2026-10-14T16:53:05Z–17:11:45Z) completed successfully.
- Downloaded XML proves 1572 core and 26 API34 tests passed with 0 failures, errors or skips. Debug assembly and compiled history against the immediate previous TEST160 APK passed.
- Artifacts: core `11913464893`, ZIP SHA-256 `5448af610d936195b20fa40d209e275c3893ed1c4bcfb04b0fb9c9ca677b9486`; Android `11915615198`, ZIP SHA-256 `e205d77fddff25053e9d9a8bf52f5212cc1d9087574c06aeef5d1f5cd0390276`; APK `11913524552`, ZIP SHA-256 `06b3eced60b35a2d5193c1656dca6fd6214ee4a719c1f354501cfe3aba59f24b`.
- Contained `ADHD-Pixel-Dungeon-CODEX-TEST-161.apk` SHA-256 `b01c72f6643af083a7fbf0d0bbb71dc8bde82152ed588d31e1cae527fb604b0b`; bundled checksum matched.
- All 34 screenshots decoded at 320×640. Contact-sheet review found expected readable changes/history, rules/score, UIMSD, stairs, tiles and gameplay states with no corrupt capture. Emulator evidence only.
- Verified the Corruption fractional save/reload correction plus Guiding Light artifact damage and Cape of Thorns outgoing retaliation. Cape incoming deflection remains based on the original roll; Skeleton Key movement remains non-damaging.
- Private delivery was skipped. TEST123 remains the owner-delivered/signature-compared fallback. Three Damage sliders remain disabled pending harmful-buff-safe attribution and six-class/boss/combination acceptance.
