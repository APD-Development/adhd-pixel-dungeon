# TEST157 player ability and delayed derived-damage candidate

- Execution start: 2026-10-14T14:30:52Z
- Candidate prepared: 2026-10-14T14:39:49.724Z
- Base head: `badbcae625a124a416f02bc9a948ae3f1fa439f1`
- Scope: Elemental Blast, Elemental Strike, and Blast Wave collision damage only.
- Elemental Blast now routes both direct damage branches through Player Damage, attributes Fireblast burning, and captures the player before delayed knockback collision.
- Elemental Strike now routes six secondary direct-damage branches through Player Damage, captures both delayed knockbacks, and gives the conjured explosion the activating player origin.
- Wand of Blast Wave captures its user for delayed collision damage. The original public `throwChar` entry point remains environmental/unattributed for callers that do not provide a creator.
- Original rolls, sources, mitigation, status durations, geometry, turns, and neutral 100% RNG are retained. No scoring, save-format, inventory, signing, application-ID, release, or slider-enablement change.
- Four retained regressions cover all 20 percentages, neutral source/RNG identity, complete Elemental Blast/Strike caller guards, delayed immutable capture, and legacy environmental exclusion.
- The Vault elemental harmful-buff reaction remains deferred: `Buff.attachTo` invokes the target callback before current Burning attribution is assigned, so correcting mixed creator ownership safely requires a separate pre-attach provenance design.
- This connector-only execution has no local Gradle workspace. Compilation, 1555 core tests, debug build/history, 26 API34 checks, and 34 screenshots are pending exact-source CI. Three Damage sliders remain disabled.


## TEST157 CI result and TEST158 fixture correction — 2026-10-14T14:43:42.114Z
- TEST157 source `514ccd2e9090af9881f6cb30b84c156179506e19`, run `38582589811`: history passed; production/tests compiled; 1555 core tests executed, 1554 passed and one new source guard failed because it counted four `throwCharAttributed(` occurrences (two wand call sites, the legacy delegate, and the method declaration) while expecting three.
- Core artifact `11903652610`, ZIP SHA-256 `569e9c6728d9dfdd424bcd30ca35cbb1a1874773960cc417ca9959e156ba3ef9`. Debug APK/history and API34 jobs were skipped; no APK/screenshots were produced.
- TEST158 changes only that expected source count from 3 to 4. Production code and behavioral assertions are unchanged. Exact-source 1555-core/debug/history/26-API34/34-PNG verification remains pending; TEST156 remains the verified source.


## TEST158 full verification outcome — 2026-10-14T15:00:05.913Z
- Exact source `d3ed403a6d6a86f5704ea6ee9c4cc4ac5c9779ee`, run `38583123803` (2026-10-14T14:43:52Z–14:56:54Z) completed successfully.
- Downloaded XML proves 1555 core and 26 API34 tests passed with 0 failures, errors, or skips. Debug assembly and compiled history against the immediate previous TEST156 APK passed.
- Artifacts: core `11903878110`, ZIP SHA-256 `12ab5a2bc392d84fb06a2632676e6cf4a42f39f5dcc00890b113dab715ed549d`; Android `11905076416`, ZIP SHA-256 `3ad192c6228a3996e383de542f48da5e985f72d03e74d99155ab28a438829b38`; APK `11904023089`, ZIP SHA-256 `2f6aff7658490cd472b1835f4c3208c3dc1ea6f28eaa1f13d6eaf7bfd064290d`.
- Contained `ADHD-Pixel-Dungeon-CODEX-TEST-158.apk` SHA-256 `92542d7ca06672e0a72ea51494480ec3ba6f976eb5aa690e6bd1b2c78cf3233b`; the bundled checksum matched.
- All 34 screenshots decoded at 320×640. Contact-sheet review found expected readable menus, dialogs, history/score states, tiles and gameplay frames with no corrupt capture. This is emulator evidence, not physical-phone evidence.
- Verified implementation: Elemental Blast direct damage/Fireblast burning/delayed collision; Elemental Strike six secondary direct sources/two delayed collisions/conjured explosion; Blast Wave immutable collision ownership with legacy callers remaining unowned. Original source objects, mitigation, rolls, RNG, geometry, turns and neutral 100% behavior are retained.
- Private delivery was skipped. TEST123 remains the last owner-delivered/signature-compared APK. Three Damage controls remain disabled pending the deferred harmful-buff reaction, remaining spell/ability/derived sources, and six-class/boss/combination acceptance.
