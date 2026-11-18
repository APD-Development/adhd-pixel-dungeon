# TEST160 offensive scroll damage candidate

- Execution start: 2026-10-14T16:32:59Z
- Candidate prepared: 2026-10-14T16:36:20.526Z
- Base head: `62b3027ffdfa46dd9fd67c557e7249257d6e180d`
- Read all 99 current artifact, potion, scroll and spell Java sources and classified direct damage, harmful effects, delayed fields and environmental boundaries.
- Confirmed Scroll of Retribution and Scroll of Psionic Blast were player-owned direct enemy damage paths still calling `mob.damage` directly.
- Candidate routes only their enemy damage through the existing Player Damage boundary. Original damage formulas, source objects, target collection, blindness/weakness, turn flow and RNG remain unchanged.
- Psionic Blast's hero self-damage remains deliberately unscaled. Chalice self-damage, starvation and unowned traps/environment remain excluded.
- Four retained tests cover all 20 authorized percentages, exact-once scaling, neutral next-RNG/source identity, both caller guards and the self-cost exclusion.
- No score, save format, inventory, signing, application ID, release, delivery or slider-enablement change. This connector-only workspace has no local Gradle checkout.
- Exact-source 1564 core/debug/immediate-previous TEST159 history/26 API34/34 PNG verification is pending. Three Damage controls remain disabled; harmful-buff pre-attach work and all-class/boss combination acceptance remain separate.


## TEST160 full verification outcome — 2026-10-14T16:51:35.140Z
- Exact source `9a071ff3312a6ae311884429c8dca4d6d0510bb9`, run `38599525580` (2026-10-14T16:36:32Z–16:49:08Z) completed successfully.
- Downloaded XML proves 1564 core and 26 API34 tests passed with 0 failures, errors or skips. Debug assembly and compiled history against the immediate previous TEST159 APK passed.
- Artifacts: core `11912327744`, ZIP SHA-256 `28da9888190c468a488bcdb61674e3de2b80d46e2745b79479d2795430b127c3`; Android `11913009126`, ZIP SHA-256 `66da7ed913422c7bc00c7cbe404340ecf1b3c84f4385c45f29a3dcba0d2db8cd`; APK `11912287722`, ZIP SHA-256 `b81b6b7540ad5d4f205f16b0354cd7ab3e0122c7ff03a0a94ff2747da4bea020`.
- Contained `ADHD-Pixel-Dungeon-CODEX-TEST-160.apk` SHA-256 `0be385ddb52c4a0c1c38b55f5327af01d8e3489816e709d41c566101d4b39282`; bundled checksum matched.
- All 34 screenshots decoded at 320×640. Contact-sheet review found expected readable changes/history, rules/score, UIMSD, stairs, tiles and gameplay states with no corrupt capture. Emulator evidence only.
- Verified Scroll of Retribution and Scroll of Psionic Blast enemy damage use Player Damage exactly once while keeping original formulas/sources and leaving Psionic Blast hero self-damage unscaled.
- Current branch head advanced afterward with an independent Corruption save-integrity fix, so TEST160 does not verify that later application change. Its next exact-source checkpoint remains pending.
- Private delivery was skipped. TEST123 remains the owner-delivered/signature-compared fallback. Three Damage sliders remain disabled pending remaining item-derived/harmful-buff boundaries and six-class/boss/combination acceptance.
