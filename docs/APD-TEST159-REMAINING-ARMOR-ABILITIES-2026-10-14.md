# TEST159 remaining armor-ability damage candidate

- Execution start: 2026-10-14T15:32:44Z
- Candidate prepared: 2026-10-14T15:35:09.392Z
- Base head: `9268f8daf939ae75439078370c63d90634eaa6c5`
- Heroic Leap Body Slam and Shockwave now apply Player Damage before their existing manual armor roll, preserving armor as a fixed mitigation step. Their original source objects and attack-proc ordering remain unchanged.
- Heroic Leap Impact Wave captures the player before delayed Blast Wave collision damage.
- Warp Beacon applies Player Damage to the enemy telefrag only. Its matching hero HP/shield self-cost remains deliberately unscaled.
- Read-only classification confirmed Wild Magic delegates through verified wand paths, Spectral Blades through `Hero.attack`, and Spirit Hawk/Shadow Clone through saved inherited player ownership.
- Five retained tests cover all 20 percentages, fixed pre-mitigation armor, neutral next-RNG identity, exact caller ordering/completeness, self-cost exclusion, and delegate/inherited-owner guards.
- No score, save-format, inventory, signing, application-ID, release, or slider-enablement change. This connector-only workspace has no local Gradle checkout.
- Exact-source 1560 core/debug/immediate-previous TEST158 history/26 API34/34 PNG verification is pending. Three Damage controls remain disabled; the pre-attach harmful-buff reaction remains separately deferred.


## TEST159 full verification outcome — 2026-10-14T15:51:12.335Z
- Exact source `8b5882b21d1c957846312296e471724c5a7207a0`, run `38590690277` (2026-10-14T15:35:36Z–15:49:14Z) completed successfully.
- Downloaded XML proves 1560 core and 26 API34 tests passed with 0 failures, errors or skips. Debug assembly and compiled history against the immediate previous TEST158 APK passed.
- Artifacts: core `11906709551`, ZIP SHA-256 `08c40465144105e00c1f0ebe545b4c09688fc5d34c717dbb3072fd661bb2d8da`; Android `11908206800`, ZIP SHA-256 `56a4ded6e107d2e0153737ed52ce36392125737fafb3ba5e65b1611909f2315c`; APK `11906599749`, ZIP SHA-256 `2476f022c8992f2943d9ccb638f25b660006c1b570ee38b8978b7405c462e227`.
- Contained `ADHD-Pixel-Dungeon-CODEX-TEST-159.apk` SHA-256 `dc5cb56b015a7d9cf98a5f3d246979e2c1f2b0dd562291c489797057ffd16fc7`; bundled checksum matched.
- All 34 screenshots decoded at 320×640. Contact-sheet review found expected readable menus, dialogs, history/score states, stairs, tiles and gameplay frames with no corrupt capture. Emulator evidence only.
- Verified Heroic Leap Body Slam/Impact Wave, both Shockwave damage branches and enemy-only Warp Beacon telefrag. Manual armor remains after scaling, hero telefrag self-cost stays unscaled, and Wild Magic/Spectral Blades/Hawk/Shadow Clone retain their verified delegated or inherited paths.
- Private delivery was skipped. TEST123 remains the owner-delivered/signature-compared fallback. Three Damage sliders remain disabled pending the deferred harmful-buff boundary, other non-armor offensive sources, and six-class/boss/combination acceptance.
