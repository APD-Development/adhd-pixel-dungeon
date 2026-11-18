# TEST156 Vault boss damage provenance

Status: verified by TEST156; not owner-delivered or physical-phone tested.

## Scope read and classification

Read the complete current VaultBossElemental, VaultSentry, VaultFlameTraps and VaultLaser implementations, including their delayed buffs and save/restore paths. Vault Sentry scans, laser beams and flame machinery are quest hazards with hazard-freebie/quest-score behavior; they remain unattributed environmental damage. Vault Boss Elemental is a BOSS actor, so its retaliation, fireball field and direct Burning, moving firewall, frost cone, lightning bolt and delayed lightning chase use Boss Damage instead of Enemy Damage.

## Implementation

Four direct-damage sites now enter CustomOffensiveHooks exactly once while retaining the original Shocking, Frost or Electricity source object. Fireball uses attributed Fire plus attributed Burning, and FireWall uses its saved Buff target (the actual boss) for delayed Burning. FrostCone and LightningChase likewise use their saved Buff target, so ordinary actor/buff serialization retains the creator without a new save field. Damage rolls, mitigation, paralysis/frost/burning duration, attack geometry, quest scoring, turns and RNG calls are unchanged. Neutral 100% follows the existing identity path.

VaultSentry, VaultFlameTraps and VaultLaser were intentionally not changed. The elemental's incoming harmful-buff reaction at VaultBossElemental.damage/add is not silently reclassified in this batch; its player-derived ownership still needs the wider derived-damage audit.

## Retained regressions

VaultBossDamageProvenanceTest adds four methods: concrete boss replacement across all 20 percentages while Player/Enemy differ; neutral source/amount/next-RNG identity; every known Vault boss caller boundary; and explicit preservation of the three environmental machinery paths.

No local Gradle checkout exists in this connector-only execution workspace. Compilation, 1551 core tests, debug assembly, immediate-previous TEST155 APK history, 26 API34 emulator tests and 34 screenshots are pending exact-source CI. Three Damage sliders remain disabled. No private delivery, signer comparison, physical-phone claim, scoring change, application-ID change or save-format change.


## TEST156 full verification outcome

Exact source `2af7a808c53f2325c56d88124b8eb71de1e2b526`, run `38573246711`, ran from `2026-10-14T13:36:49Z` through `2026-10-14T13:49:13Z`. Downloaded XML proves 1551 core and 26 API34 tests passed with 0 failures, errors or skips. Debug assembly and compiled history against the immediate previous TEST155 APK passed.

Artifacts: core `11898621235`, ZIP SHA-256 `74794fab9c0f0038c72bda6bbbda68a6d9af45fe7c842ea68630a92b73553e24`; Android `11899122092`, ZIP SHA-256 `cc34aa6e9e69a0de5ca98920e5510fe9595c830c85d1571f950f7f97792b915a`; APK `11899205759`, ZIP SHA-256 `1caa8befa972770a9169e2eb3cdb95eed15cec37687b37173138e720912cd61d`. Contained `ADHD-Pixel-Dungeon-CODEX-TEST-156.apk` SHA-256 `ee5307c5738313d6b82f46fab53c9e6dda07713d427f0eb6d1d01f389004d817`; checksum passed.

All 34 screenshots decoded at 320×640. Contact-sheet review found expected readable menus, dialogs, score states, tile/gameplay frames and no corrupt capture. This is emulator evidence, not physical-phone evidence.

TEST156 verifies the Vault boss attribution and the preserved environmental hazard boundary. Private delivery was skipped. TEST123 remains the last owner-delivered/signature-compared APK. The three Damage sliders remain disabled pending harmful-buff/other derived damage, remaining spells/abilities, and six-class/boss/combination acceptance.
