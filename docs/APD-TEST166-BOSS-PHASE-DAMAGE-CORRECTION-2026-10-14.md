# TEST166 boss-phase damage correction candidate — 2026-10-14

## Demonstrated failure
- TEST165 source `314b678c1aa7c85d4a84f6d5a02ad1929f43ecab`, run `38618183149` (2026-10-14T18:47:45Z–18:49:47Z), compiled and executed 1,581 core tests with 2 failures.
- The new fire-field regression invoked visual particle emission in the headless suite. Existing electricity acceptance also proved a newly created context-free Tengu shocker field had become unscaled.

## Correction
- A newly created Tengu fire or shocker field with no transient creator context defaults to Boss ownership, matching the only gameplay creator and prior Tengu behavior.
- Restored historical Tengu fields lacking provenance metadata are marked legacy-unknown and remain unscaled. Explicit creator context still overrides the default and supports mixed fields.
- The fire persistence regression now retrieves the restored per-cell provenance and calls the production pre-attach Burning entry point directly. It retains the mixed 300% reaction/source assertion without entering renderer-only particles.
- No gameplay RNG, turns, mitigation, source object or density changed. Other TEST165 production boundaries are unchanged.

## Verification state
- Expected complete core total remains 1,581. Core, debug assembly, immediate-previous TEST164 compiled history, 26 API34 checks and 34 screenshots are pending.
- TEST164 remains latest verified and TEST123 remains last owner-delivered/signature-compared. Three Damage controls remain disabled; no delivery or physical-phone claim.


## TEST166 verification — 2026-10-14T19:06:38Z
- Exact source `e252e92e6533ac0a3fa581048475bf55fdd5e3da`; GitHub Actions run `38618850435` ran 2026-10-14T18:52:25Z–19:05:24Z and concluded success.
- Core artifact `11922055137` (ZIP SHA-256 `d6eddd28a21303235b861d504622183fadce7dfd537bfd227d3c7a1eea2c9a0a`) contains 1,581 tests, 0 failures, 0 errors and 0 skipped. The four new boss-phase and eight existing electricity tests passed.
- Debug APK assembly and complete reviewed history against immediate previous compiled TEST164 passed. APK artifact `11921765201` (ZIP SHA-256 `77a377ace0806711bb98726850858029c33f9760c86e52c1b6bf303864b86698`) contains `ADHD-Pixel-Dungeon-CODEX-TEST-166.apk`, SHA-256 `984170746be694a64bc462febcf072292321760ffba3f4e596828f3adec1f614`; bundled checksum matched.
- Android artifact `11922116695` (ZIP SHA-256 `ba39a535b6528081553e1b357b52fe5257ae84a972b0aea041e3e77667ff04d3`) records 26 API34 emulator tests, 0 failed and 0 ignored. All 34 screenshots decoded at 320×640; contact-sheet review found expected readable history/rules/score/UIMSD/stairs/tiles/gameplay states with no blank, corrupt or visibly clipped capture.
- Verified behavior: all six classes and five main bosses select the intended damage origin; Tengu bomb/fire/shocker, DM-300 rocks, Dwarf King summon collisions and Yog rays use exactly one creator boundary. Newly created Tengu fields preserve boss ownership while metadata-less historical fields remain unknown/unscaled. Dwarf King self-damage remains unchanged.
- This is CI/emulator evidence only. Private delivery was skipped; TEST123 remains last owner-delivered/signature-compared. Three Damage controls remain disabled pending high-HP/defense, save/load and cross-rule acceptance.
