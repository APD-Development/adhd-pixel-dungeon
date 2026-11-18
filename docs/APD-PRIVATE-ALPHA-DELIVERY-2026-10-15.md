# APD private alpha development delivery — October 15, 2026

## Exact artifact and verification status
This delivery reuses the existing Codex TEST198 APK, compiled by source verification checkpoint TEST202. These are distinct identifiers: the native APK build number is 198; TEST202 is a development-attempt label. Do not label an attempt as a separately produced APK.

Compiled source: 186f5892429ef564ec4a9d59c9885a62fb6f1c1f. Successful build run:38712411656, October15 05:22:07–05:36:13 a.m. New York time. Actual recorded evidence:1615core tests,27API34 Android tests,0failures/errors/skips; debug compilation, compiled offline history and35screenshots passed. Core-only TEST203 later passed1617tests without another APK. Delivery independently rechecks actual XML and exact artifact source, signatures/application ID/version/checksum against published TEST159 before release. No physical-phone or owner-save playthrough is claimed.

Expected APK SHA256:6dd8a5e16e13670e6dfb6abd12783dd7225ef8ed936b12b57f1192b51618936d. Existing TEST159 remains preserved. Install over the existing application; never uninstall or clear data. The release tag is the packaging workflow commit, not a claim that later branch changes are compiled in this APK.

## Implemented features and APD-specific mechanics
All18Custom Dungeon Rules controls are implemented and available for new-run drafts. Existing-run controls remain locked; exact selected values are saved and retained in UIMSD. The proposed upgrade-scroll range stays1–60 with15neutral. Original scoring is unchanged; experimental boundaries/scoring notice remain. Automated acceptance includes six classes, boss-origin replacement, combined HP/defense/damage/low-scroll configurations and all18-value save/load. This does not establish exhaustive balancing or full normal-run survival testing.

Damage-source coverage added since TEST159 includes offensive scrolls, artifact effects, harmful elemental-buff attachment, saved mixed fields, boss delayed effects, Warrior Crush splash, staff and remaining wand hits, independent weapon-spell bonuses and tipped-dart bonuses. Original source objects, mitigation, random rolls and self/environmental exclusions are retained. Derived Shocking/Crystal effects inherit already-scaled hits and must not be multiplied again.

Public canonical recording expanded with levers, visible ally summons and successful alchemy craft details, retaining hidden true item identities internally. Floor feelings already had canonical journal landmarks; additional regressions verified existing behavior instead of inventing duplicate events. These are telemetry/statistics features, not new gameplay abilities.

## Confirmed reliability corrections
Corruption now reloads fractional decay with the correct numeric type. DerivedStore now detects changed raw source content even with equal filenames/lengths. RunIndex serializes conflicting cross-instance temp replacement. Controlled UIMSD audit findings and their exact retained evidence are recorded in the following checkpoint ledger; the broader historical system audit and all discretionary UI/storage work are not declared complete. Raw run history and player saves are preserved; no production compression conversion was performed.

## Partial, disabled and remaining work
Competitive Custom Rules scoring remains deliberately deferred until the owner approves a formula; no experimental coefficients affect earned scores. Unfinished online systems stay disabled. Broader UIMSD redesign/storage optimization and historical reconstruction must not be described as universally complete. Physical-phone touch/playthrough/save-upgrade acceptance is still required. The movement-command telemetry candidate TEST204 and the new Shocking regression are later branch work, excluded from this compiled artifact. Existing credits, private ownership, signing key and application ID remain preserved.

## Timing and preparation failures
Release delivery fell behind verification; previous artifacts accumulated in Actions without release publication. No continuous developer work or exact active/idle duration is claimed. Earlier TEST159 delivery first failed403; an additional workflow retry had duplicate YAML permissions. The corrected delivery succeeded38600583387 and those existing assets remain untouched. Current delivery reuses that successful workflow pattern, including explicit write permissions and separate compiled-source metadata. Exact CI times below are not developer-active hours and repeated runs are not summed into a unique-test count.

## Source-specific changes, roots, regression risks and test results
The preserved checkpoint records below include actual failures, their corrections, sources and limitations. Pending statements are superseded only by a later explicit successful verification. Failed attempts did not necessarily produce APKs. Development-attempt numbers do not necessarily match native APK run numbers. Risks requiring continued attention include delayed/overlapping effect attribution, unfamiliar slider combinations, boss progression and phone-specific UI/lifecycle behavior.

## 2026-10-15T10:40:47Z — TEST204 movement-command telemetry candidate
- Reproduced the movement omission at the real action boundary: the shipped `move.step.travelling` field is true for every ordinary physical step and therefore cannot distinguish an adjacent manual step from point-to-point travel. Its historical values remain unchanged; its description is corrected.
- Additive telemetry 1.15 introduces public `move.command`, emitted once when a `HeroAction.Move` begins and tagged `step`, `travel` or `stairs`, with destination cell and floor width. The transient action-local guard prevents one multi-step route from producing duplicate commands.
- Exact-source review found no general auto-explore mechanic in this game version, so no fictional mode or hook was added. Movement, path selection, interruptions, saves, RNG, turns, scoring and permanent statistics are unchanged.
- Added two retained core regressions plus wiring, manifest, telemetry-version and immutable-history updates. TEST204 requests 1,619 core tests, debug assembly, complete immediate-previous compiled TEST202/Codex TEST 198 history, 27 API34 methods and 35 PNGs.
- Candidate pending. TEST202 remains the verified compiled fallback; TEST123 remains owner-delivered/signature-compared. No delivery or physical-phone claim.


## 2026-10-14T16:36:20.526Z — TEST160 offensive scroll damage candidate
- Reconciled latest real head `62b3027ffdfa46dd9fd67c557e7249257d6e180d`; read current handoff/build request/source guide/ledger and all 99 artifact, potion, scroll and spell sources.
- Implemented Player Damage for Scroll of Retribution and Scroll of Psionic Blast enemy damage only; Psionic Blast hero self-cost stays unscaled.
- Added four retained methods covering all20 percentages, exact-once/neutral RNG/source identity, complete caller wiring and self-cost exclusion.
- No local Gradle workspace is available; 1564core/debug/immediate-previous TEST159 history/26API34/34PNG remains pending.
- Three Damage sliders remain disabled. TEST159 remains latest verified source; TEST123 remains owner-delivered fallback.



## TEST160 full verification outcome — 2026-10-14T16:51:35.140Z
- Exact source `9a071ff3312a6ae311884429c8dca4d6d0510bb9`, run `38599525580` (2026-10-14T16:36:32Z–16:49:08Z) completed successfully.
- Downloaded XML proves 1564 core and 26 API34 tests passed with 0 failures, errors or skips. Debug assembly and compiled history against the immediate previous TEST159 APK passed.
- Artifacts: core `11912327744`, ZIP SHA-256 `28da9888190c468a488bcdb61674e3de2b80d46e2745b79479d2795430b127c3`; Android `11913009126`, ZIP SHA-256 `66da7ed913422c7bc00c7cbe404340ecf1b3c84f4385c45f29a3dcba0d2db8cd`; APK `11912287722`, ZIP SHA-256 `b81b6b7540ad5d4f205f16b0354cd7ab3e0122c7ff03a0a94ff2747da4bea020`.
- Contained `ADHD-Pixel-Dungeon-CODEX-TEST-160.apk` SHA-256 `0be385ddb52c4a0c1c38b55f5327af01d8e3489816e709d41c566101d4b39282`; bundled checksum matched.
- All 34 screenshots decoded at 320×640. Contact-sheet review found expected readable changes/history, rules/score, UIMSD, stairs, tiles and gameplay states with no corrupt capture. Emulator evidence only.
- Verified Scroll of Retribution and Scroll of Psionic Blast enemy damage use Player Damage exactly once while keeping original formulas/sources and leaving Psionic Blast hero self-damage unscaled.
- Current branch head advanced afterward with an independent Corruption save-integrity fix, so TEST160 does not verify that later application change. Its next exact-source checkpoint remains pending.
- Private delivery was skipped. TEST123 remains the owner-delivered/signature-compared fallback. Three Damage sliders remain disabled pending remaining item-derived/harmful-buff boundaries and six-class/boss/combination acceptance.



## 2026-10-14T16:52:55.080Z — TEST161 artifact-derived damage candidate
- Reconciled current head `27835d898d1a09a462c0cc49f8cfc3915d945497`, preserving the independent Corruption float-reload fix and its four tests.
- Implemented Player Damage for Guiding Light artifact procs and Cape of Thorns outgoing retaliation; retained fixed incoming deflection and non-damaging Skeleton Key movement.
- Added four retained methods covering all20 percentages, neutral RNG/source identity, source/order wiring and movement exclusion. Expected current total:1572core.
- Exact-source debug/immediate-previous TEST160 history/26API34/34PNG pending. Three Damage controls remain disabled; TEST123 remains owner-delivered fallback.



## TEST161 full verification outcome — 2026-10-14T17:13:21.216Z
- Exact source `9088ca30acde28282454598b1e649a3a70156b56`, run `38601864296` (2026-10-14T16:53:05Z–17:11:45Z) completed successfully.
- Downloaded XML proves 1572 core and 26 API34 tests passed with 0 failures, errors or skips. Debug assembly and compiled history against the immediate previous TEST160 APK passed.
- Artifacts: core `11913464893`, ZIP SHA-256 `5448af610d936195b20fa40d209e275c3893ed1c4bcfb04b0fb9c9ca677b9486`; Android `11915615198`, ZIP SHA-256 `e205d77fddff25053e9d9a8bf52f5212cc1d9087574c06aeef5d1f5cd0390276`; APK `11913524552`, ZIP SHA-256 `06b3eced60b35a2d5193c1656dca6fd6214ee4a719c1f354501cfe3aba59f24b`.
- Contained `ADHD-Pixel-Dungeon-CODEX-TEST-161.apk` SHA-256 `b01c72f6643af083a7fbf0d0bbb71dc8bde82152ed588d31e1cae527fb604b0b`; bundled checksum matched.
- All 34 screenshots decoded at 320×640. Contact-sheet review found expected readable changes/history, rules/score, UIMSD, stairs, tiles and gameplay states with no corrupt capture. Emulator evidence only.
- Verified the Corruption fractional save/reload correction plus Guiding Light artifact damage and Cape of Thorns outgoing retaliation. Cape incoming deflection remains based on the original roll; Skeleton Key movement remains non-damaging.
- Private delivery was skipped. TEST123 remains the owner-delivered/signature-compared fallback. Three Damage sliders remain disabled pending harmful-buff-safe attribution and six-class/boss/combination acceptance.



## 2026-10-14T17:53:07.187Z — TEST162 harmful-buff attachment candidate
- Reconciled actual head `7fd4e51d4ce6137d9c1baf84f39ac3dceb604fa4` after verified TEST161; reread build request, source guide, handoff and ledger.
- Audited all current production Java sources for Burning attribution and Chill/Frost attachment entry points. Confirmed the vault elemental reacts inside `add(Buff)`, before prior caller-side provenance setters ran.
- Implemented transient pre-attach immutable origin metadata in `Buff`, preconfigured single/mixed Burning causes, and exactly-once vault reaction scaling while preserving the original buff source.
- Added persistent per-cell Freezing provenance with save/reload and legacy-unknown behavior; attributed Frost potion, frost bomb, Elemental Strike and direct wand/imbue/enchantment/dart callers. Environmental traps/rooms remain unscaled.
- Added five retained regressions for player, boss, mixed saved field, legacy environment and neutral RNG/source behavior; expected total 1577 core.
- No real local Gradle workspace is available, so exact-source core/debug/API34/history/visual verification is pending. Three Damage controls remain disabled. TEST161 remains latest verified source; TEST123 remains owner-delivered fallback.



## TEST162 pre-build failure — 2026-10-14T17:56:56.152Z
- Exact source `50e61eb7d517d70566d8a06878a8709d1fd32e65`, run `38610738155` (2026-10-14T17:55:12Z–17:56:02Z) failed in history validation before compilation.
- Demonstrated cause: the comparison from compiled TEST161 source also contains its later verification record `docs/APD-TEST161-ARTIFACT-DERIVED-DAMAGE-2026-10-14.md`; that reviewed path was absent from the TEST162 manifest.
- Core, debug and API34 checks did not run; no APK or screenshots were produced. Application source was not tested and remains candidate-only.
- The manifest now includes that exact already-reviewed evidence path. TEST163 requests the same application source with corrected complete history. TEST161 remains latest verified; three Damage controls remain disabled.



## TEST163 compile failure — 2026-10-14T18:05:22.000Z
- Exact source `620562d4ce3e809aa90d2636ffc16ef3b0d0232d`, run `38611095532` (2026-10-14T17:57:44Z–18:04:15Z) passed complete-history validation, then failed `:core:compileJava`.
- Both errors were in `Buff.appendAttributed`: Java rejected private `apdPrepareAttach` calls through generic receiver type `T extends Buff`.
- The correction casts the receiver to the declaring `Buff` type before the same two private calls. No behavior, state, RNG, source or save-format logic changed.
- Tests, debug assembly and API34 did not run; no APK or screenshots were produced. TEST161 remains latest verified; three Damage controls remain disabled.



## TEST164 verification — 2026-10-14T18:23:48Z
- Exact application source `166c8a287b599fcb0f741a446f039fc33f656201`; GitHub Actions run `38612344678` ran 2026-10-14T18:06:32Z–18:18:07Z and concluded success.
- Core artifact `11917672775` (ZIP SHA-256 `a92f05678a40a517822e8cf3aee8a9e168dffb90078e97f31478b51526cf58b6`) contains 1,577 tests, 0 failures, 0 errors and 0 skipped.
- Debug APK assembled and complete reviewed history matched the immediate previous compiled TEST161 APK. APK artifact `11917912521` (ZIP SHA-256 `b1acddb6d3c46e1e3a1ba5e0b3b9595564402dffed2175359297f2e268ad4f12`) contains `ADHD-Pixel-Dungeon-CODEX-TEST-164.apk`, SHA-256 `5b6f46bebbb54f189142f4e0b1a14dbf7d204bf2a06f5fbae030efac3bee2919`.
- Android artifact `11918874056` (ZIP SHA-256 `99b8ade78132ca383f332901acc71c9180174120cc4ab641105f075f48b8492b`) contains 26 API34 emulator tests, 0 failures, 0 errors and 0 skipped. All 34 PNGs decoded as 320×640; the contact-sheet inspection found no blank, corrupt or visibly clipped capture.
- Verified behavior: harmful Burning/Chill/Frost attachment carries the actual single or mixed creator into immediate reactions and saved Freezing fields; environmental and legacy origin remain unscaled; neutral values preserve source, amount and RNG. No save-format change was introduced.
- Private delivery remained skipped. This is emulator/CI evidence only, not a physical-phone claim. TEST123 remains the last owner-delivered/signature-compared APK. Three Damage controls remain disabled pending six-class, boss-phase and rule-combination acceptance.



## 2026-10-14T18:45:47Z — TEST165 boss-phase damage acceptance candidate
- Reconciled actual head `7a4ff110e04f0005d5afe06bad9a67486d13c81d`; reread the current handoff, build request, completed ledger and source guide. Did not rebuild TEST164 unchanged.
- Audited Goo, Tengu, DM-300, Dwarf King and Yog-Dzewa direct damage sites. Confirmed omitted boss boundaries in Tengu bombs/fire, DM-300 falling rocks, Dwarf King summon collisions and Yog death rays; Tengu shocker used a hard-coded boss origin without saved creator provenance.
- Implemented exactly-once creator scaling with original source/RNG/turn behavior, pre-armor Tengu bomb order, saved mixed Tengu fields and legacy-unknown behavior. Dwarf King self-damage remains unscaled; Goo needed no source change.
- Added four retained tests covering all six hero classes, five bosses, Tengu delayed-field save/legacy behavior and source-contract/mitigation order. Expected core total 1,581.
- Exact-source core/debug/immediate-previous TEST164 history/26 API34/34 PNG verification is pending. Three Damage controls remain disabled; no delivery or phone claim.



## TEST165 core failure / TEST166 correction — 2026-10-14T18:51:04Z
- Exact source `314b678c1aa7c85d4a84f6d5a02ad1929f43ecab`, run `38618183149` (2026-10-14T18:47:45Z–18:49:47Z) passed complete-history validation and compiled all production/test source, then ran 1,581 core tests with 2 failures.
- The new headless Tengu fire regression entered the real visual particle path without a renderer and failed at `CellEmitter.get`; it now exercises the same persisted per-cell origin at the pre-attach Burning entry point, avoiding any weakened assertion.
- Existing acceptance correctly exposed a compatibility regression: a newly created Tengu shocker field seeded without transient context was treated as environmental (16 damage) instead of Tengu-owned (32 at 200% Boss Damage). New Tengu-only fields now default to Boss ownership, while restored historical fields lacking metadata remain explicitly unknown and unscaled.
- Debug, APK, API34 and screenshots did not run; no artifact was produced. TEST164 remains latest verified. TEST166 requests the corrected 1,581-test exact source; three Damage controls remain disabled.



## TEST166 verification — 2026-10-14T19:06:38Z
- Exact source `e252e92e6533ac0a3fa581048475bf55fdd5e3da`; GitHub Actions run `38618850435` ran 2026-10-14T18:52:25Z–19:05:24Z and concluded success.
- Core artifact `11922055137` (ZIP SHA-256 `d6eddd28a21303235b861d504622183fadce7dfd537bfd227d3c7a1eea2c9a0a`) contains 1,581 tests, 0 failures, 0 errors and 0 skipped. The four new boss-phase and eight existing electricity tests passed.
- Debug APK assembly and complete reviewed history against immediate previous compiled TEST164 passed. APK artifact `11921765201` (ZIP SHA-256 `77a377ace0806711bb98726850858029c33f9760c86e52c1b6bf303864b86698`) contains `ADHD-Pixel-Dungeon-CODEX-TEST-166.apk`, SHA-256 `984170746be694a64bc462febcf072292321760ffba3f4e596828f3adec1f614`; bundled checksum matched.
- Android artifact `11922116695` (ZIP SHA-256 `ba39a535b6528081553e1b357b52fe5257ae84a972b0aea041e3e77667ff04d3`) records 26 API34 emulator tests, 0 failed and 0 ignored. All 34 screenshots decoded at 320×640; contact-sheet review found expected readable history/rules/score/UIMSD/stairs/tiles/gameplay states with no blank, corrupt or visibly clipped capture.
- Verified behavior: all six classes and five main bosses select the intended damage origin; Tengu bomb/fire/shocker, DM-300 rocks, Dwarf King summon collisions and Yog rays use exactly one creator boundary. Newly created Tengu fields preserve boss ownership while metadata-less historical fields remain unknown/unscaled. Dwarf King self-damage remains unchanged.
- This is CI/emulator evidence only. Private delivery was skipped; TEST123 remains last owner-delivered/signature-compared. Three Damage controls remain disabled pending high-HP/defense, save/load and cross-rule acceptance.



## 2026-10-14T19:44:18Z — TEST167 all-18-controls acceptance candidate
- Reconciled head `4c309fd7e82ad29ada0b89428f3d046e6252a6f0` and did not rebuild verified TEST166 unchanged.
- Added distinct combination acceptance for all six classes at high enemy HP/defense and low Player Damage/upgrade count; actual enemy-versus-boss replacement damage with high Player Defense and fractional healing; and real all-18-value save/load with damaged high Player HP, difficulty/balance, nine challenges, class and seed.
- Existing all-class generation acceptance still covers spawn/hunger/food/HP/scroll interactions, so it was retained rather than duplicated.
- Marked the three Damage controls available for next-run drafts only. Active-run settings remain locked; original scores, experimental warning and pending-scoring notice are unchanged.
- Expected total: 1,585 core plus debug/immediate-previous TEST166 history/26 API34/34 PNG. Candidate-only until exact-source verification passes; TEST123 remains owner-delivered fallback.



# TEST167 core failure; TEST168 correction — 2026-10-14 19:52 UTC

TEST167source57b29f9ec716aabb20237a963fe8d32e4bbfb930/run38626757231 ran1585core with2failures. New real-attack regression reached headless Hero.interrupt visual path because fixture damageInterrupt remained enabled; application combat behavior was not implicated. History guard also rejected nonexistent V061_CHANGE sprite. TEST168 disables only fixture visual interruption and uses existing V064_CHALLENGES; assertions/coverage unchanged. No167APK/API34/screenshots. TEST166 remains latest verified; all18controls candidate pending TEST168. No delivery/score/save-format/phone claim.



# TEST168 core failure; TEST169 correction — 2026-10-14 19:57 UTC

TEST168source3a70f85f706dcf56553af296c1a44f98635a5af7/run38627408866 ran1585core with1failure. Native history sprite correction passed. Real75-damage attack reached Hero.damage serious-damage feedback, whose interrupt requires live GameScene selector absent from core fixtures. TEST169 no-ops only FixedHero.interrupt; actual attack/hooks/defense/source/healing/assertions remain. No168APK/API34/screenshots. TEST166 latest verified; all18controls candidate pending TEST169. No delivery/score/save-format/phone claim.



# TEST169 verified; all 18 Custom Dungeon Rules controls available — 2026-10-14 20:12 UTC

Verified sourcea160dddb4f39fe2e3eb62a601e13bea71730f064/run38628060548 (19:57:58–20:10:25UTC):1585core+26API34/debug/immediate-previousTEST166history/34PNG all passed. APKsha256ff0d7f10bcef18c7c247bfaee30e7f913ee7ecc699433103fca7a222c3090559. All18controls now verified available for next-run drafts; current-run controls locked. Cross-rule coverage includes all-six-class highHP/defense/lowupgrade attacks, enemy-vs-boss replacement damage with PlayerDefense/healing, and exact all18-value save/load. Original earned scores/scoring identity unchanged; ranges provisional. TEST167/168 fixture/history failures retained. No delivery/phone claim; TEST123 remains last owner-delivered. Next safe step: do not rebuild unchanged TEST169; resume documented post-slider confirmed-bug/reliability backlog.



# TEST170 DerivedStore source-identity correction pending — 2026-10-14 20:38 UTC

Source review confirmed the derived facts cache identified raw run/legacy logs only by filename and byte length. A valid same-name, same-length replacement could therefore leave stale statistics visible until another size change or manual rebuild. The candidate fingerprints each raw log with byte length plus SHA-256, bumps derivation version 7→8 for one automatic cache rebuild, and retains a regression that replaces a valid log with a different valid equal-length log. Raw logs and player saves are not rewritten. Exact-source TEST170 requests 1586core/debug/immediate-previous TEST169 history/26API34/34PNG; all verification is pending. TEST169 remains the latest verified APK and TEST123 remains the last owner-delivered/signature-compared APK. No delivery or phone claim.



# TEST170 verified DerivedStore source identity — 2026-10-14 20:53 UTC

Verified source8da173025d4ab64fea123ee03f9afdbd4efc02f1/run38634072761 (20:40:35–20:51:50UTC):1586core+26API34/debug/immediate-previousTEST169history/34PNG all passed. APKsha256e9117959406a5893069e6f90f626386f335daa2f9b7f3c91c3ef3dd3ecb2e43c. A valid same-name/same-length raw-log replacement now forces a DerivedStore rebuild and exposes replacement facts; unchanged raw sources still use cache. Derivation version8 causes one automatic rebuild; raw logs/saves are not rewritten. No delivery/phone claim; TEST123 remains last owner-delivered. Next safe step: do not rebuild unchanged TEST170; continue the documented UIMSD reliability/performance/storage audit, reproducing a distinct issue before source changes.



# TEST171 RunIndex cross-instance save correction pending — 2026-10-14 20:58 UTC

Current-source review confirmed `RunIndex.save()` synchronized only each instance while every instance used the same `index.apdi.tmp`; two stores targeting one root could truncate/rename/delete that derived temp/index concurrently. The candidate serializes the small on-disk atomic-replacement section across RunIndex instances and adds a controlled two-thread regression that holds the first temp open, proves the second cannot enter, then requires both saves and the final second index to load without rebuild. Raw run logs, player saves, formats, gameplay and scoring are unchanged. Exact-source TEST171 requests1587core/debug/immediate-previousTEST170history/26API34/34PNG; verification pending. TEST170 remains verified fallback; TEST123 remains owner-delivered.


# TEST171 verified; newer Crush candidate not covered — 2026-10-14 21:10 UTC

Exact source169d2bec53968f191286e3db6cb77efe7661ef06/run38636484010 (20:58:00–21:08:41UTC) passed1587core+26API34/debug/immediate-previous TEST170 history/34PNG. APKsha256e5038b2055ca71f7cf0b432d05f1d4e70dddbc8b38325ffd28e1934288c23c22. The deterministic two-RunIndex regression passed; cross-instance writes can no longer concurrently open/truncate the shared derived index temp, and final index load does not rebuild. Raw logs, saves, formats, gameplay and scoring unchanged. No delivery/phone claim; TEST123 remains last owner-delivered. Branch head advanced afterward to c684d5070fc26bca6cc2d34b414ea18ecbefbb49 with independent Crush splash scaling; TEST171 does not verify that newer application source. Preserve it and run exact-source verification once its immediate-previous history is complete.


# TEST172 Crush splash scaling pending exact-source verification — 2026-10-14 21:11 UTC

Preserved concurrent application commit c684d5070fc26bca6cc2d34b414ea18ecbefbb49 on top of verified TEST171. Warrior Crush splash now routes its attributable offensive roll through Player Damage exactly once before the original halving/DR/Vulnerable order; neutral100% retains original rounding and next RNG. Three retained tests cover all permitted percentages, Player-vs-Enemy/Boss selection, pre-armor results and neutral identity. TEST172 requests1590core/debug/immediate-previous TEST171 history/26API34/34PNG. Candidate only until exact-source evidence passes. No save/scoring/delivery/phone change; TEST123 remains owner-delivered.


# TEST172 verified Crush splash Player Damage boundary — 2026-10-14 21:26 UTC

Verified source12795cd9cd3049864e1f33c90b9551e9bda67c8e/run38638453521 (21:12:37–21:25:00UTC):1590core+26API34/debug/immediate-previous TEST171 history/34PNG all passed. APKsha2565a9739829b398ab9fbc4c1ae83c78e06dbddd23806a854406d58442e5375f153. Warrior Crush splash now applies Player Damage exactly once before original halving/DR/Vulnerable order; all percentages and neutral rounding/next RNG passed. No delivery/phone/save/scoring claim; TEST123 remains owner-delivered. Do not rebuild unchanged TEST172; continue a distinct confirmed offensive-source audit item or documented post-slider backlog.


# TEST173 Fireblast staff explosion provenance pending — 2026-10-14 21:36 UTC

Current-source audit found the Mages' Staff Fireblast on-hit explosion still called direct damage although normal Fireblast casts/fields were attributed. The candidate keeps the same hero damage roll/power multiplier and original wand source, but applies the actual attacker factor once before the original direct hit. Three retained tests cover all percentages and player/enemy/boss creators, contradictory settings, one source-preserving hit, neutral amount/next RNG and production wiring. TEST173 requests1593core/debug/immediate-previous TEST172 history/26API34/34PNG. Candidate only; no save/scoring/delivery/phone claim. TEST123 remains owner-delivered.


# TEST173 verified; TEST174 remaining direct wands pending — 2026-10-14 21:51 UTC

TEST173sourcee101e85167ee802cef3a0db31cb8497b83a23328/run38641672668 (21:37:27–21:50:04UTC) passed1593core+26API34/debug/immediate-previousTEST172history/34PNG. APKsha2562f9379b9a38a28996d32bc745af94ba72df6ba0ce63e789bd348dd7d59c95d30. Fireblast staff explosion attribution verified; no delivery/phone claim.

Adjacent audit confirmed seven direct-damage calls in Blast Wave, Living Earth and Prismatic Light. TEST174 routes each through one shared actual-creator boundary while retaining original rolls, targets, source, direct mitigation, turns and RNG. Three retained tests cover all factors/roles, contradictory settings, source/neutral identity and exact wiring. TEST174 requests1596core/debug/immediate-previousTEST173history/26API34/34PNG. Candidate only; TEST123 remains owner-delivered.


## 2026-10-14T21:54:00Z — TEST174 first run failed; test-fixture correction
- Source `6cec278930f087ed6c18b5d04c136a046ff22669`, run `38643522614` (21:52:10Z–21:53:28Z) passed history validation, then failed `:core:compileTestJava`: retained `ProbeWand` lacked Wand's required abstract `onHit` override.
- Zero tests ran; no APK/API34/screenshots/artifact. A duplicate same-source run was already active and cannot verify this known failing source.
- Correction adds only a no-op fixture override; production changes and assertions are unchanged. Corrected TEST174 full verification remains pending; TEST173 remains latest verified and TEST123 remains owner-delivered fallback.


## 2026-10-14T22:06:30Z — TEST174 verified remaining direct-wand damage provenance
- Corrected exact source `5913850f400dbfe9fd0b547abbae71c20cd9992c`, run `38643884380` (21:55:06Z–22:05:52Z), concluded success.
- Core artifact `11931736651` / ZIP SHA-256 `ab58ce2a031c47f2bb5b3f745a0eedcf7a9399542063b3965191d9e158c1bf27`: 1,596 tests, 0 failures/errors/skips. APK artifact `11931766535` / ZIP SHA-256 `4801b69528ddb80f57603ea4b1dd191bfa9e2f0b80d3acfcbf143316ed16416d`: APK SHA-256 `6cbce087ebdacfbb4a621d1689f72b86b2cad71274efe6f22a8a4c98b78feb29`; checksum/debug/history passed.
- Android artifact `11931807905` / ZIP SHA-256 `8431b9c85cc88a39957ce5b7aeefb035dbf3f58d29f8370f0792d6b895c45e9f`: 26 API34 tests passed; 34/34 PNGs decoded 320×640 and passed contact-sheet review.
- Verified seven Blast Wave/Living Earth/Prismatic Light damage calls use the actual creator factor exactly once; neutral amount/source/RNG identity passed. Duplicate old-source run `38643546560` only repeated the fixture compile failure and is not verification.
- Private delivery skipped; no physical-phone claim. TEST123 remains owner-delivered fallback. Do not rebuild unchanged TEST174; continue a distinct approved audit task.


## 2026-10-14T22:08:00Z — TEST177 weapon spell-bonus candidate
- After verified TEST174, audited weapon enchantments and hero abilities. Crystal/Shocking-derived damage already inherits the once-scaled post-mitigation base hit and remains unchanged.
- Confirmed Holy Weapon's flat bonus and Smite's independent roll bypassed the custom boundary. Routed only those two bonuses through the actual attacker once, retaining original rolls, multiplier, sources and trigger order.
- Added three retained tests for every factor/creator role, contradictory settings, single source-preserving damage, neutral RNG identity and exact production wiring. Expected total 1,599 core.
- Exact-source core/debug/immediate-previous TEST176 history/26 API34/34 PNG verification is pending. No delivery or phone claim; TEST123 remains owner-delivered fallback.


## 2026-10-14T22:24:00Z — TEST177 verified weapon-triggered spell bonuses
- Exact source `7b190b54c1aaed329e6e185c592ee686c02385ea`, run `38645784290` (22:10:39Z–22:23:35Z), concluded success.
- Core artifact `11932816785` / ZIP SHA-256 `63fd2f7af4ad6b689b4e58a0149004d8372b9c516746a73f6217ffba3277086e`: 1,599 tests, 0 failures/errors/skips. APK artifact `11932966600` / ZIP SHA-256 `f7e358e1b52e5f0892fe53f98e0cbb747fce540b32b258a62966cbccf0ecd0a9`: APK SHA-256 `ea2099d787daeacca90d2b19d7e44263c571ed8517bc3d49229a530054a23e65`; checksum/debug/history passed.
- Android artifact `11932673053` / ZIP SHA-256 `6b7c38505ee99ba17bbc3c3ca5861a051d1d1ce6db1b1d6fe44ac313b2a45d7b`: 26 API34 tests passed; 34/34 PNGs decoded 320×640 and passed contact-sheet/focused review.
- Verified Holy Weapon and Smite independent bonuses use actual attacker scaling exactly once. Crystal/Shocking derived damage remains singly scaled through the base hit.
- Private delivery skipped; no physical-phone claim. TEST123 remains owner-delivered fallback. Do not rebuild unchanged TEST177.



## 2026-10-14T22:37:24Z — TEST178 tipped-dart bonus candidate
- Re-read verified TEST177 and audited a distinct offensive item/projectile category rather than rebuilding it unchanged.
- Confirmed Shocking Dart's electrical burst and Holy Dart's anti-undead/demonic bonus are independent post-hit rolls that bypassed creator scaling.
- Routed only those two bonuses through the actual attacker once, preserving their original ranges, sources, gates, visuals, mitigation and RNG.
- Added three retained tests for every permitted factor and creator role, contradictory settings, one source-preserving hit, neutral next-RNG identity and exact production wiring. Expected total: 1,602 core.
- Exact-source core/debug/immediate-previous TEST177 history/26 API34/34 PNG verification is pending. No delivery or physical-phone claim; TEST123 remains owner-delivered fallback.



## 2026-10-14T22:51:05Z — TEST178 verified tipped-dart bonus attribution
- Exact source `a6d757fcaecc69fb0bdb38a6dd207c3aea6ccf22`, run `38649035059` (22:38:37Z–22:51:05Z), concluded success.
- Core artifact `11933781595` / ZIP SHA-256 `d9ccd829d674db97d0926421ee71e3780170a1a2e33c11414883185f2d5866e3`: 1,602 tests, 0 failures/errors/skips; all three new methods passed.
- APK artifact `11933846357` / ZIP SHA-256 `5cbfffea672bdfb047e1ed4a3e222380ff022f731476a8f6acf566f5f1d5657d`: APK SHA-256 `082e2ad55ce382fe49b348d152145da1103cdee557a80c1ddb2a582aa5029e74`; checksum/debug/immediate-previous TEST177 history passed.
- Android artifact `11934865932` / ZIP SHA-256 `7a5b2ef852f5c10ba87870a661b4c4dc0076437e8b5512f2b77ee1edb5aed238`: 26 API34 tests passed; 34/34 PNGs decoded at 320×640 and passed contact-sheet/focused history review.
- Verified Shocking Dart electrical burst and Holy Dart anti-undead/demonic bonus use the actual attacker factor exactly once; neutral amount/source/next-RNG identity passed.
- Private delivery skipped; no physical-phone claim. TEST123 remains owner-delivered fallback. Do not rebuild unchanged TEST178; continue a distinct approved backlog task.



## 2026-10-14T23:39:51Z — TEST179 UIMSD Column order recreation candidate
- Re-read the documented Arrange-columns lifecycle issue after verified TEST178.
- Current production windows already provide exact restorers, but API34 coverage only recreated Columns selection and never the distinct Column order or Move column steps.
- Strengthened the retained device test to recreate both steps exactly once, invoke Make first through the restored callback, verify the selected second column becomes first, then restore the original query.
- No production query/window, save, history, scoring, identity or signing behavior changed. TEST179 requests 1,602 core/debug/immediate-previous TEST178 history/26 strengthened API34 methods/34 PNG. Candidate remains pending; no delivery or physical-phone claim.



## 2026-10-14T23:53:14Z — TEST179 fixture failure; TEST180 correction pending
- TEST179 exact source `f6043c7d16cd52e57276113fb038af046ff4ec48`, run `38655567956` (23:40:55Z–23:52:01Z), passed 1,602 core tests, debug assembly, immediate-previous TEST178 history and APK packaging.
- API34 completed all 26 methods with 25 pass / 1 fail. The only failure was the new `column-order fixture has at least two columns` assertion. Runs uses an empty explicit column list to mean dataset defaults; the fixture incorrectly treated that valid state as zero effective columns.
- Android artifact `11937227267` / ZIP SHA-256 `5e80aad0730b6507fe0cfba1862fae175636d70362eeac2c8f25f5508718a3e8` retains the failure. TEST179 is not verified or delivered.
- TEST180 corrects only the device-test setup: derive an effective multi-column list from dataset defaults, exercise Column order and Move column recreation, then restore the original explicit list including empty/default semantics.
- Exact-source TEST180 requests 1,602 core/debug/immediate-previous compiled TEST179 history/26 API34/34 PNG. Production query/window/save/scoring code remains unchanged. No delivery or physical-phone claim; TEST123 remains owner-delivered fallback.



## 2026-10-15T00:08:33Z — TEST180 verified UIMSD column-dialog recreation regression
- Exact source `acf8592e2db3c30684e09a05bd22c097c0d5f077`, run `38656873159` (2026-10-14T23:54:11Z–2026-10-15T00:06:25Z), concluded success.
- Core artifact `11937556865` / ZIP SHA-256 `3e33f34ed08a3905d67d97652a25fa627dff485aa08134686af4afad8fa1fa2b`: 1,602 tests, 0 failures/errors. APK artifact `11937113364` / ZIP SHA-256 `2445194a97a19a118ff453f214a849dd24488fcddd529e5e0d0743a05383dde7`: APK SHA-256 `279f6691874d32a892fc2743afe5946b3bdf371f315372b7541ffec646ab8332`; checksum/debug/immediate-previous compiled TEST179 history passed.
- Android artifact `11937448955` / ZIP SHA-256 `a9638818a3f7afacbeaa05596fc1fb3b8875e3c06d1320220e2da1f64dff46b9`: 26 API34 methods passed; the strengthened two-step recreation method passed in 8.33 seconds.
- 34/34 screenshots decoded at 320×640; contact-sheet/focused APD Changes review found the expected TEST180→TEST179→TEST178 sequence and no blank, corrupt or visibly clipped image.
- Verified retained coverage: Column order and its parameterized Move column child each restore exactly once across scene recreation, and Make first executes through the restored callback while original explicit/default query semantics are restored afterward.
- Production query/window, save, player-history, scoring, identity and signing code were unchanged. Private delivery skipped; no physical-phone claim. TEST123 remains last owner-delivered/signature-compared. Do not rebuild unchanged TEST180; continue a distinct documented post-slider backlog item.



## 2026-10-15T00:12:37Z — TEST181 UIMSD scope-wording candidate
- Re-read the verified TEST180 head and reproduced the documented empty-Home ambiguity in production source: the Rules control changes rule sets, while unfinished runs are a separate Tables > Options switch.
- Added a pure Home empty-state message that names finished runs under other rules separately from runs still going, and routed the native Home through it. Options now explicitly says Rules does not change finished status; the Home scope note names runs still going among exclusions.
- Added one retained regression method covering an empty history, a finished abandoned run under different rules, singular wording, the unfinished-run route, and all-rules inclusion. Expected total: 1,603 core tests.
- Exact-source core/debug/immediate-previous TEST180 history/26 API34/34 PNG verification is pending. This is wording/query-scope UI only: no raw history, save, statistics, scoring, gameplay, application ID or signing behavior changed. No delivery or physical-phone claim; TEST123 remains owner-delivered fallback.


## 2026-10-15T00:15:34Z — TEST181 history guard failure; TEST182 correction pending
- TEST181 source `ba146f3c0ec2e145fa57295af2c398248f497d2a`, run `38658816290`, stopped before compilation at the reviewed-history guard. It reported one omitted path: `docs/APD-TEST179-UIMSD-COLUMN-RESTORE-2026-10-14.md`, added by the intervening TEST180 evidence commit.
- No core, debug or Android tests ran; TEST181 is not verified and produced no APK. The UIMSD application/test source is unchanged for TEST182.
- TEST182 adds the omitted existing documentation path to the complete comparison manifest and requests the full 1,603-core/debug/immediate-previous TEST180 history/26 API34/34 PNG gate. No delivery or physical-phone claim.


## 2026-10-15T00:30:48Z — TEST182 verified UIMSD rule/status scope wording
- Exact source `eed8bb85dfb32cffded60a08614cf3223362fb50`, run `38659050102` (2026-10-15T00:16:09Z–00:29:24Z), concluded success.
- Core artifact `11938107765` / ZIP SHA-256 `df2c8d34889cb9f387b97cfabeaebc962445613d191e73a1edffa3cb9c44567f`: 1,603 tests, 0 failures/errors/skips. The new `emptyHomeSeparatesRulesFromRunStatus` method passed in 0.139 seconds.
- APK artifact `11938012933` / ZIP SHA-256 `7bb738e8efa77e6b462e720fee3c0a41d6d91fc7b6b482cb7236e6f1cbdec6d1`: APK SHA-256 `ed13da85121f7cd13feeb751050912beba9e09f47f4b6c6867670cfc7f90e7df`; debug and immediate-previous TEST180 history passed. The compiled Changes ledger contains TEST182, then TEST180 and TEST179; TEST181 correctly has no compiled record because it stopped before compilation.
- Android artifact `11937863231` / ZIP SHA-256 `aabb3a0ac3246d7573dfbc9d1eef90893653ddd5c6c67c9820ed3244fc2a58ce`: 26 API34 tests passed. All 34 screenshots decoded at 320×640; contact-sheet and focused UIMSD Home/APD Changes review found no blank, corrupt or visibly clipped image.
- Verified behavior: Home and Options distinguish finished runs under other rules from runs still going; an abandoned run remains a finished result. No raw-history/save/statistics/scoring/gameplay/identity/signing behavior changed.
- Private delivery skipped; emulator evidence is not a physical-phone claim. TEST123 remains last owner-delivered/signature-compared. Do not rebuild unchanged TEST182.


## 2026-10-15T00:42:18.901Z — TEST183 native empty-Home regression candidate
- Re-read verified TEST182 and found its API34 suite opened the real first-run Home but did not wait for the empty model, assert the rendered rule/status wording or retain a screenshot before switching to demo data.
- Strengthened the existing native method to require an empty fresh-emulator Home, inspect the actual rendered text for “No finished runs yet” and the dedicated Tables > Options route, reject the obsolete Rules/run-status conflation, and retain `uimsd-empty-home.png`.
- Production gameplay, UIMSD query/model, raw history, saves, statistics, scoring, application identity and signing are unchanged.
- TEST183 requests 1,603 core tests, debug assembly, immediate-previous TEST182 history, 26 API34 methods and 35 valid screenshots. Candidate only until hosted evidence passes; no delivery or physical-phone claim. TEST123 remains owner-delivered fallback.



## 2026-10-15T00:54:48.241Z — TEST183 API34 fixture failure; TEST184 correction pending
- TEST183 source `68bea4b4d57aef872cab684e13f134b91f2d258c`, run `38661559728` (00:42:30Z–00:53:13Z), passed 1,603 core tests, debug assembly and immediate-previous TEST182 history. APK SHA-256 `05140836f2274dd4d560476369282a746eeb3518e4c39347f7cccf979f8965e8`.
- API34 completed 26 methods with 25 pass / 1 fail: the strengthened method assumed the shared emulator still had no finished history, but earlier device methods intentionally created finished-run fixtures. The empty-Home assertion failed before its screenshot; TEST183 is not verified or delivered.
- TEST184 changes only the device fixture: render a controlled in-memory empty Home through the real scene, assert/capture it, then restore the original cache without deleting or rewriting history.
- Exact-source TEST184 requests 1,603 core/debug/immediate-previous compiled TEST183 history/26 API34/35 PNG. Production gameplay/UIMSD model/save/statistics/scoring/identity/signing remain unchanged; no delivery or physical-phone claim.



## 2026-10-15T01:08:28.843Z — TEST184 native checks passed; TEST185 visual-retention correction
- TEST184 source `e883710fa759a41a1ffa7754f42c021f0f8121ec`, run `38662728056` (00:55:01Z–01:06:10Z), passed 1,603 core tests, debug assembly, immediate-previous compiled TEST183 history and all 26 API34 methods. APK SHA-256 `a6b121104b14d64dfa2576d10efa3772383dd500d55e4ddf880c9cd0ba950203`.
- The controlled empty-Home assertion passed and logcat records creation of `uimsd-empty-home.png`, but the evidence script's explicit list retained only the prior 34 images. The requested new screenshot could not be reviewed, so TEST184 is not the completed 35-PNG checkpoint.
- TEST185 adds that one filename to the runner's pull and PNG validator lists. Exact-source verification requests 1,603 core/debug/immediate-previous compiled TEST184 history/26 API34/35 PNG.
- Production gameplay/UIMSD model/save/statistics/scoring/identity/signing remain unchanged. No delivery or physical-phone claim; TEST182 remains the last fully verified fallback and TEST123 the last owner-delivered APK.



## 2026-10-15T01:12:37.816Z — TEST185 push produced no run; TEST186 request
- Branch commit `80506e4deb7be7a7ed1fcbbd4ee37fac17972329` contains the one-line-per-list evidence-script correction and an updated `docs/APD-CODEX-BUILD-REQUEST.txt`, but GitHub created no workflow run or check suite for that exact source during this invocation.
- No TEST185 build, core tests, emulator tests, screenshots or APK exist; do not infer completion from the branch commit.
- TEST186 re-records the exact request while keeping immediate-previous compiled TEST184 as the comparison base: 1,603 core/debug/TEST184 history/26 API34/35 PNG.
- Production source remains unchanged. No delivery or physical-phone claim; TEST182 remains the last fully verified fallback and TEST123 the last owner-delivered APK.



## 2026-10-15T01:29:34.915Z — TEST185 verified after reconciled request
- The initial TEST185 request commit `80506e4deb7be7a7ed1fcbbd4ee37fac17972329` produced no Actions run or check suite. Reconciled request source `40d31d81de4c2fcc1ae154e9ec0d1c8f6a28d984` then compiled under the workflow's actual `Codex TEST 185` label; there is no separate TEST186 APK.
- Hosted run [38664406486](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38664406486) ran from 2026-10-15T01:12:48Z through 2026-10-15T01:25:32Z and passed 1,603 core tests, debug assembly, immediate-previous compiled TEST184 history, and all 26 API34 methods with zero failures/errors/skips.
- Downloaded artifacts: core `11940232030`, APK `11940212006`, Android evidence `11941165163`. APK SHA-256 is `cc3c211a67d179f703e0d468ef54efdb5d62b1eec99f44cb54ececed2a519c1e`.
- The Android artifact contains all 35 expected 320x640 PNGs. The complete contact sheet was reviewed; the native empty-History Home is readable and correctly separates finished runs from the Tables > Options route for runs still going, and APD Changes shows the immediate TEST184/TEST185 sequence without clipping or corruption.
- TEST183's shared-emulator fixture failure and TEST184's omitted screenshot-pull entry remain recorded as failed/incomplete checkpoints. TEST185 is the latest fully verified source checkpoint. No application production code, gameplay, raw history, save, permanent statistics, scoring, application ID or signing identity changed in this evidence batch.
- This is hosted build/emulator evidence, not physical-phone or owner-save verification. No APK was distributed or published; TEST123 remains the last owner-delivered/signature-compared APK. Do not rebuild unchanged TEST185. Continue the next distinct approved post-slider UIMSD reliability/backlog task.



## 2026-10-15T01:40:02.195Z — TEST186 UIMSD preparing-tour Back correction pending
- Re-read verified TEST185 and reproduced a navigation no-op in source: `onBackPressed()` returned while `tutorialPreparing`, so Android Back and the visible exit control could not cancel sample generation/loading.
- Changed that branch to end the preparing tour, return to Home, and advance `tourGeneration`; the stale worker therefore cannot publish after cancellation. Existing serialized sample cleanup and real-history guards remain intact.
- Strengthened `AndroidBackDeviceTest` to start a real sample preparation, cancel it in the preparing state, require immediate main-model/Home restoration and require the worker generation to be invalidated before the existing normal-tour Back flow.
- TEST186 exact-source verification requests 1,603 core tests, debug assembly, immediate-previous compiled TEST185 history, all 26 API34 methods and 35 screenshots. Candidate is pending until hosted evidence passes.
- No gameplay, scoring, raw history, save, permanent statistics, identity or signing behavior changed. No delivery or physical-phone claim; TEST185 remains the verified fallback and TEST123 the owner-delivered fallback.



## 2026-10-15T01:42:48.021Z — TEST186 history-guard failure; TEST187 correction pending
- TEST186 source `c1e09d72b48cdd8e534f93818cb763ade84407c9`, run [38666897410](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38666897410) (01:40:13Z–01:41:01Z), stopped before compilation because the reviewed-history manifest omitted `docs/APD-TEST183-UIMSD-EMPTY-HOME-NATIVE-2026-10-15.md` from the intervening TEST185 evidence commit.
- No core tests, APK, Android tests or screenshots were produced; TEST186 is not verified. The exact failure is retained in `docs/APD-TEST186-UIMSD-TOUR-CANCEL-2026-10-15.md`.
- TEST187 adds only the omitted existing evidence path. UIMSD application and Android regression source remain unchanged; verify 1,603 core/debug/immediate-previous compiled TEST185 history/26 API34/35 PNG.
- No gameplay/save/statistics/scoring/identity/signing/delivery/physical-phone change. TEST185 remains the verified fallback and TEST123 the owner-delivered fallback.



## 2026-10-15T01:57:45.673Z — TEST187 passed configured gate but omitted changed Back class; TEST188 pending
- TEST187 source `44cf2b195229f8909021ed9998f65aba52acb7ce`, run [38667136514](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38667136514) (01:42:55Z–01:55:00Z), passed 1,603 core tests, debug assembly, immediate-previous compiled TEST185 history, 26 configured API34 methods and 35 validated 320x640 screenshots. APK SHA-256 `e75bb0598f051ea818964010447168a18779190149713cf6e40db16cf05f80ed`.
- Downloaded XML audit showed `AndroidBackDeviceTest` was not in the runner's explicit class list. The new regression compiled but did not execute; do not call TEST187 verification of the Back fix.
- TEST188 adds the retained class to the API34 command, increasing the required count to 27 while keeping application/test source unchanged. Compare against the immediate previous compiled TEST187 APK and retain the existing 35 screenshots.
- No delivery or physical-phone claim. TEST185 remains the last fully verified fallback; TEST123 remains the owner-delivered/signature-compared fallback.

TEST189 fixture-only correction (2026-10-15T02:13:47.780Z): TEST188 sourcee251bb7a760db8274babdeb3e5356425853670d1/run38668463084 passed1603core/debug/immediate-previous compiledTEST187history and retained35validPNG, but API34 executed27 with26pass/1fixture failure. AndroidBackDeviceTest used public UimsdScene.open while the private test APK intentionally enables the demo-unlock gate, so it timed out waiting for UIMSD before reaching any tour assertion. Enter the already-authorized scene directly with openNow for this scene-navigation test; production gate and Back correction are unchanged. Verify1603core/debug/immediate-previous compiledTEST188history/27API34/35PNG. TEST188 is not accepted; TEST185 remains fully verified and TEST123 owner-delivered. No delivery/physical-phone claim.

TEST189 verified (2026-10-15T02:29:53.163Z): sourceea1062079c6a43aadaf80dbf0197f3fcff4769cc/run38669986563 passed1603core in309XML+27API34/debug/immediate-previous compiledTEST188history/35PNG,0failure/error/skip. Android XML explicitly contains AndroidBackDeviceTest.backWalksBackwardsOneStepPerPress (28.019s); its log records preparing-tour cancellation/worker invalidation and the later completed-tour/title Back sequence. APK SHA256 f4f45c841f359530a81818364ae0d79d4989078477103555e26c5d747e05b4c6. Full35-image contact sheet reviewed clean; APD Changes shows TEST189/188/187/185. Production Back cancellation is verified; the fixture-only openNow isolates it from the intentional private-build unlock gate without changing that gate. No application build pending; do not rebuild unchanged TEST189. No delivery/physical-phone claim; TEST123 remains owner-delivered/signature-compared.



## 2026-10-15T02:40:14.718Z — TEST190 SavedViews cross-instance store correction pending
- Re-read verified TEST189 and reproduced a distinct UIMSD storage race: separate `SavedViews` instances shared `saved_views.txt.tmp` without cross-instance serialization, so concurrent stores could truncate or rename the same temporary file.
- Serialized only the atomic replacement section and added a deterministic two-writer regression that pauses the first open, proves the second cannot enter, then requires the final second view to reload cleanly with no leftover temp file.
- Exact-source TEST190 requests 1,604 core tests, debug assembly, immediate-previous compiled TEST189 history, 27 API34 methods and 35 PNGs. Candidate pending; no delivery or physical-phone claim.
- Saved-view format/query encoding, raw history, saves, permanent statistics, gameplay, scoring, identity and signing are unchanged. TEST189 remains verified fallback; TEST123 remains owner-delivered fallback.



## 2026-10-15T02:58:31.520Z — TEST190 hosted gates passed; artifact-content review pending
- Exact source `97c4848581e2f7fcd979d3965e257bdae59c5a93`, run [38672320647](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38672320647) (02:41:30Z–02:55:08Z), passed the complete core job, debug assembly, immediate-previous compiled TEST189 history and all 27 API34 methods (0 skipped/failed). The runner validated all 35 PNGs at 320×640.
- Artifact ZIP digests: core `7b8a3adfb3fffed6ff5a592830f2c839d19db22339347fdd3356f78d3526504d`, APK `09e0246462b86d4d50793e09e126791d16c0f1aa08a1cef9f3f751de4d3a5f93`, Android `5152ec875abf965a3e64176daeb2fd6fe48bd11ee08531747490de2925e67431`.
- Private ZIP payload access was unavailable in this runtime, so XML-total parsing, contained-APK SHA read and contact-sheet content review remain pending. Do not rebuild unchanged TEST190; reuse artifacts `11944263722`, `11944698529`, `11945087238` for that audit. TEST189 remains the fully reviewed hosted/emulator checkpoint.
- Production correction is confined to cross-instance SavedViews atomic replacement. No raw history/save/scoring/gameplay/identity/signing/delivery/physical-phone change; TEST123 remains owner-delivered fallback.



## 2026-10-15T03:41:39Z — TEST191 archive compression-wrapper lifetime candidate
- Current source created `ZipOutputStream` and `ZipInputStream` wrappers but did not close them; callers later closed only document-provider streams, leaving compressor/inflater cleanup nondeterministic.
- Export/import now close owned ZIP wrappers on every path through non-closing adapters. Supplied streams remain caller-owned.
- Two retained regressions cover caller-stream ownership and direct importer ZIP closure. TEST191 requests 1,606 core/debug/immediate-previous compiled TEST190 history/27 API34/35 PNG; pending.
- Archive format, raw history, saves, statistics, scoring, gameplay, identity and signing are unchanged. No delivery/phone claim; TEST189 remains fully reviewed and TEST123 owner-delivered.



## 2026-10-15T04:00:07Z — TEST191 verified archive wrapper lifetime correction
- Source `7e6cae8900c97eaa920bbe8baebb657ec5e539be`, run [38677900668](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38677900668), ran 03:45:18Z–03:56:59Z and completed successfully.
- Downloaded core evidence contains 310 XML suites / 1,606 tests; API34 XML contains 27 tests. Both have zero failures, errors and skips. `ArchiveStreamOwnershipTest` ran both retained methods.
- Debug assembly and immediate-previous compiled TEST190 history passed. APK SHA-256 is `b88b4ddc71f12abaac8d216d3c553968577b254b9f55f3ecff2522ef0046329b`.
- All 35 screenshots are present at 320×640; the full contact sheet was visually reviewed clean.
- Artifacts: core `11946906120` / ZIP `48b92ba33cb0ec901db327f1f9de6099e4ff4e411c9d3b164cdfcd31ef3e66d9`; APK `11946966071` / ZIP `89c853dd6650c38cc75ebbef3eee3a7a32f58091a10f534de2232b9c967d5099`; Android `11947432170` / ZIP `e53779a6a8502d817d0fdb253bbcd303baa9dab8a6bd8ae8bdc0bdaa6a19c830`.
- Hosted/emulator verification is complete; no physical phone or delivery. TEST123 remains owner-delivered. Do not rebuild unchanged TEST191.



## 2026-10-15T04:38:01Z — TEST192 streaming run-log reader candidate
- Confirmed normal file reads buffered the entire log before parsing; a controlled 1 GiB sparse bad tail made allocation follow irrelevant damaged bytes after the first invalid marker.
- Ordinary reads now stream one checksum-validated, 16 MiB-capped frame at a time. Explicit resync retains the existing byte-array path so recovery semantics are unchanged.
- Added parser-parity and sparse-tail regressions. Verification pending: 1,608 core/debug/immediate-previous compiled TEST191 history/27 API34/35 PNG.
- No observed phone crash is claimed. Formats, raw history, saves, statistics, scoring, gameplay, identity and signing are unchanged. No delivery/phone claim; TEST191 remains fully reviewed and TEST123 owner-delivered.



## 2026-10-15T04:57:30Z — TEST192 streaming run-log reader verified
- Source `222189f971dc55597f561eb87aadcd4e34b47a2e`, run [38682901119](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38682901119), ran 04:42:48Z–04:55:53Z and completed successfully.
- Downloaded core evidence contains 311 XML suites / 1,608 tests; API34 contains 27 tests. Both have zero failures, errors and skips; both retained `RunLogStreamingReadTest` methods passed.
- Debug assembly and immediate-previous compiled TEST191 history passed. APK SHA-256 is `129626b1bd0a65453c207c9fa5d06075a048b2e4d63e30ee034d0e1b15ca846f`.
- All 35 screenshots are present at 320×640; the full contact sheet was visually reviewed clean.
- Artifacts: core `11949475185` / ZIP `08b08aff0bc224fa0ade8d9097985f62e4b882bd82c3705a313313e72f544f12`; APK `11949125498` / ZIP `250c7d0329d61cea59e3db995dce5d3dcd936eba744e634b86fa8fd0393f9495`; Android `11949591656` / ZIP `7c401ca09f78fa0111d27b7472271029c916a91a53e27ca5005eb6559a7b4869`.
- Hosted/emulator verification is complete; no physical phone or delivery. TEST123 remains owner-delivered. Do not rebuild unchanged TEST192.



## TEST192 next safe step
- No application build is pending; do not rebuild unchanged source `222189f971dc55597f561eb87aadcd4e34b47a2e`.
- Remaining limitations are unchanged: no physical-phone/owner-save verification and no tester delivery; TEST123 remains owner-delivered/signature-compared.
- The controlled UIMSD audit items tracked in `docs/UIMSD-AUDIT-BUG-LOG.md` are now resolved, but the broader approved post-slider backlog is not complete. Re-read the latest head and continue the next distinct documented item without revisiting scoring, Cleric, lore, online activation or distribution.



## TEST193 fixture failure; TEST194 correction pending — 2026-10-15 05:44 UTC

Current-source review established that the old tutorial floor-1 hidden-door backlog entry was stale: the narrow saved entrance-boundary repair has been present since the October 9 tutorial correction, but lacked a retained current-branch regression. TEST193 source `34eb3d9e25177d4bff8a754c2387cabe8ef84a10`, core-only run [38688177613](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38688177613), executed 1,609 tests; 1,608 passed and the new test failed before its assertion because the synthetic level omitted the normal trap collection required by `Level.set`. No APK was built.

TEST194 initializes only the fixture's trap/blob collections and reruns the same complete 1,609-test core gate. Production application source remains exact verified TEST192 source `222189f971dc55597f561eb87aadcd4e34b47a2e`; reuse its verified APK evidence. TEST123 remains owner-delivered. Pending TEST194 evidence; do not mark the backlog item closed yet.



## TEST194 fixture-geometry failure; TEST195 correction pending — 2026-10-15 05:49 UTC

TEST194 source `0fa7763a36e4ebc4994629910cab6d285d3d0af5`, core-only run [38688555523](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38688555523), executed 1,609 tests; 1,608 passed. The fixture now had normal collections but placed its synthetic entrance one cell from the map border, outside generated-level geometry; `updateOpenSpace` indexed beyond that unrealistic margin before any repair assertion. No APK was built. TEST195 moves only the synthetic room to a production-realistic interior margin and reruns all 1,609 core tests. Production application source remains exact TEST192 and its verified APK remains the reusable application artifact. Pending TEST195; TEST123 remains owner-delivered.



## TEST195 verified legacy tutorial entrance repair regression — 2026-10-15 05:54 UTC

Exact test/docs source `f7049868cd7648b29c7606b1ade3c9c8e8de0fdd`, core-only run [38688955099](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38688955099) (05:50:19–05:53:03 UTC), passed 1,609 tests in 312 XML suites with zero failures, errors or skips. The retained regression proves the October 9 saved-floor-1 repair exposes only a legacy `SECRET_DOOR` on the saved entrance-room boundary, updates its passable/secret flags, and preserves an unrelated secret door plus ordinary entrance wall. Core artifact `11951151594`; downloaded ZIP SHA-256 `20a37fe25701527b8be522b3168f3beae4622c4ce27b413dcd24adc47ccef35d`.

TEST193 and TEST194 fixture-only failures remain recorded. Production application source did not change: exact TEST192 source `222189f971dc55597f561eb87aadcd4e34b47a2e`, run `38682901119`, and APK SHA-256 `129626b1bd0a65453c207c9fa5d06075a048b2e4d63e30ee034d0e1b15ca846f` remain the latest verified compiled application evidence. No APK was rebuilt or distributed; TEST123 remains the last owner-delivered/signature-compared APK. Physical legacy-save acceptance remains unverified; never abandon/delete a save as a workaround.

The ordered legacy-door item is closed. Hero markers still require explicit approval; remaining phone checklist/tour review and backend/contact decisions require the owner. Next autonomous work: re-read latest head, then take the first approved unblocked canonical recording candidate (levers) only after reproducing a concrete omission; do not revisit scoring, Cleric, lore, online activation or distribution.



## 2026-10-15T06:35:47Z — TEST196 canonical NPC interaction recording candidate
- Reproduced the queued lever item as non-applicable to this source: no lever class, terrain, message key or interaction path exists, so no fictional event was added.
- Confirmed the next omission: accepted hero/NPC interactions were not recorded unless a quest state advanced. Added public telemetry 1.12 fact `npc.interact` at the central hero interaction path, containing only the name shown to the player and current floor; `npc.quest` remains separate.
- Added a retained real-central-path regression requiring one accepted interaction event and no event for a rejected interaction.
- TEST196 requests 1,610 core/debug/immediate-previous compiled TEST192 history/27 API34/35 PNG. Candidate pending; TEST192 remains verified fallback and TEST123 owner-delivered.
- No gameplay result, turn, RNG, save, permanent-statistics, scoring, custom-rule, identity, signing, delivery or physical-phone change.



## 2026-10-15T06:40:00Z — TEST196 integration-contract failures; TEST197 correction pending
- TEST196 source `37da2dcd7ef3512402c2d8622be73e075d2278ba` / run [38693568938](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38693568938) executed 1,610 core tests: 1,607 passed, 3 failed before APK compilation.
- Failures were the expected new Hero hook count, missing required telemetry 1.12 records-history milestone, and a headless regression entering pre-existing `Hero.ready()` UI code before the interaction.
- TEST197 declares both permanent contracts and has the real central path call a private call-plus-record helper that the regression can invoke without UI. Production interaction result, turn and recorder behavior are unchanged.
- Core artifact `11952904481`, ZIP SHA-256 `7a11eab5b211a9190a02fc28920f2bd1de1ca718d94cab7f65944539aa270f34`. No TEST196 APK. Verify 1,610 core/debug/immediate-previous compiled TEST192 history/27 API34/35 PNG.



## 2026-10-15T06:55:59Z — TEST197 NPC interaction telemetry verified
- Source `364fd4724b79308fb6a13478a211bcd7599b2f7b`, run [38694008236](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38694008236) (06:42:17Z–06:54:46Z), passed 1,610 core tests in 313 XML suites, debug assembly, immediate-previous compiled TEST192 history, all 27 API34 methods and 35 validated 320×640 screenshots; zero failures/errors/skips.
- Full 35-image contact sheet reviewed clean. APK SHA-256 `8fc77119f6f16193a4fb42ca539b53bbb8d75e26c23c1112a6708b51a2615beb`.
- Artifacts: core `11953586184` / ZIP `3f2fdb0c4bde49d7a9162ab83e797d9273e191229a81da64708906d1ae48cb24`; APK `11953825841` / ZIP `312b0e8240fe14cb55a989fa0cf045f53f789a4aafa6c3edcc8d4145f1653377`; Android `11954147276` / ZIP `8f18ea2bfb5b13600bbb3a6212ebbdba6c64847bee005d89a896473f968ce1df`.
- The source milestone is TEST197; the sprint workflow's independent run counter named the APK Codex TEST 194. Preserve both exact labels. TEST196 remains failed; no TEST196 APK existed.
- Telemetry 1.12 now records accepted NPC interactions exactly once using only player-visible name and floor. Rejected interactions emit nothing; quest steps remain separate. The lever candidate was correctly omitted because current source has no lever mechanic.
- No physical-phone/delivery claim; TEST123 remains owner-delivered. Do not rebuild unchanged source. Next autonomous recording candidate: shop stock seen, after reproducing a concrete omission and hidden-information review.



## 2026-10-15T07:35:03Z — TEST198 shop-stock visibility boundary pending
- Reproduced the queued candidate in current source: visible `FOR_SALE` heaps already emit canonical `ITEM_SEEN` with `SEEN_SHOP` through `ItemTracker.scanFloor`; unseen, hidden-outline, out-of-FOV and closed-container contents remain excluded.
- The concrete omission was retained coverage for shop classification plus unidentified-item secrecy. Added one regression requiring exactly one public shop-seen event with only displayed state, while true class, +4 level and curse remain in INTERNAL registration.
- Production application source remains verified TEST197 `364fd4724b79308fb6a13478a211bcd7599b2f7b`. Request complete 1,611-core verification only; APK rebuild is forbidden.
- No gameplay/save/statistics format/scoring/custom-rule/identity/signing/delivery/physical-phone change. TEST123 remains owner-delivered. Pending hosted evidence.



## 2026-10-15T07:41:19Z — TEST198 shop-stock visibility boundary gate passed
- Exact test/docs source `bed2aa8513e1ab7ac5b307f8a04d9f0fda9a2d33`; core-only run [38700157769](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38700157769) completed successfully (07:38:53Z–07:41:19Z). `:core:test` reported `BUILD SUCCESSFUL`.
- Retained shop regression binds `FOR_SALE` visibility to exactly one public `SEEN_SHOP` event containing only displayed unidentified state; true class, +4 level and curse remain INTERNAL.
- Artifact `11955827287`, 745,760 bytes, GitHub SHA-256 `2a0d77bf965610aefb181a6d12355697f39b8a86a449e6cbcc6e7818f5e3d2a1`.
- Requested count was 1,611 (prior verified 1,610 plus one test). Independent XML recount remains unavailable because the artifact host proxy returned repeated HTTP 502; this limitation is retained rather than converting the requested count into a parsed claim.
- No APK job ran. Application source remains verified TEST197 `364fd4724b79308fb6a13478a211bcd7599b2f7b`; TEST123 remains owner-delivered. Next approved candidate: ally summons, only after current-source reproduction and creator/visibility review.



## 2026-10-15T08:36:50Z — TEST199 ally-summon recording candidate
- Reproduced a concrete omission after TEST198: explicit offensive creator ownership already survived summon creation and save/load, but run history emitted no summon fact.
- Telemetry 1.13 adds public `ally.summon` at that existing creation boundary. It records only the displayed summoned name, displayed actual creator name and current floor.
- A summon is recorded only when ownership first becomes PLAYER and its actual creator is the hero or another creature currently visible to the hero. Repeated inheritance, enemy, boss, unattributed, off-screen-creator and restore-time paths emit no public summon event.
- Added two retained regressions for exact-once direct/chained player summons, actual creator identity, exclusions and restoration suppression. TEST199 requests 1,613 core tests, debug assembly, complete history from immediate-previous compiled TEST197/Codex TEST 194, 27 API34 methods and 35 PNGs.
- No damage amount, turn, RNG, actor save metadata, scoring, custom-rule, application ID or signing behavior changed. Candidate pending; no delivery or physical-phone claim. TEST197 remains the verified compiled fallback and TEST123 the owner-delivered fallback.



## 2026-10-15T08:48:52Z — TEST199 fixture assertion failure; TEST200 correction pending
- TEST199 source `5a937f93e2858bbc85990db2d96642c2ef4764f1` / run [38707847596](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38707847596) executed 1,613 core tests: 1,612 passed and one failed.
- `TelemetryVersionTest.parsesAndPrints` parsed the literal `1.12` but mistakenly expected `1.13`. The production telemetry 1.13 self-test and both new ally-summon regressions passed.
- History preservation/validation passed. Debug APK and API34 were skipped; no TEST199 APK exists. Failed core artifact `11958859521`, 752,523 bytes, digest `8dcf551329e74d6758ae50ee6cb0f302f7892bdfb15c003e9389f55c169dc8f4`.
- TEST200 corrects only the fixture expectation to `1.12`; production implementation and assertions are retained. Full 1,613-core/debug/TEST197-history/27-API34/35-PNG gate pending. TEST197 remains verified compiled fallback; TEST123 remains owner-delivered.



## 2026-10-15T09:05:04Z — TEST200 ally-summon telemetry verified
- Corrected source `094f6fc168c2c46057648542d68da4537739ec9f`, run [38708477712](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38708477712) (08:49:54Z–09:03:04Z), passed 1,613 core tests in 314 XML suites, debug assembly, complete immediate-previous compiled TEST197/Codex TEST 194 history, 27 API34 methods and 35 screenshots; zero failures/errors/skips.
- Both `AllySummonRecordingTest` methods and all four `TelemetryVersionTest` methods passed. Telemetry 1.13 public `ally.summon` is accepted at the explicit creator-ownership boundary with exact-once and visibility/privacy exclusions retained.
- Internal Codex TEST 196 APK SHA-256 `5fe49a5e2167dd688c35fcbc04b31e18951873fa42caae2ea645f42039e4c646`.
- Artifacts: core `11959253024` / ZIP `1c5e5084…fe7b17`; APK `11959178061` / ZIP `e93ab8c5…4f09c`; Android `11959678617` / ZIP `dfdc05d4…d49ca6`.
- All 35 PNGs are valid 320×640 and the full contact sheet reviewed clean. Private delivery was skipped; no physical-phone claim. TEST123 remains owner-delivered/signature-compared.
- No application build is pending; do not rebuild unchanged TEST200. Re-read the latest head, then continue the next distinct approved canonical recording item in documented order (alchemy-use details before level feelings) only after reproducing a concrete omission and reviewing hidden-information boundaries.



## 2026-10-15T09:14:47Z — TEST201 visible alchemy details candidate
- Reproduced the next canonical-recording omission: successful alchemy already retained INTERNAL ingredient→product lineage, but emitted no PUBLIC craft observation.
- Telemetry 1.14 adds `alchemy.craft` only after `Recipe.brew` returns a result. It uses the existing pre-brew duplicates and records displayed input/output names, quantities, floor, and exact total/main-pool/toolkit energy split.
- True unidentified classes remain INTERNAL; failed brews and restore-time reconstruction emit nothing. The existing lineage hook remains unchanged.
- Added two retained regressions plus wiring, manifest, version and permanent-history contracts. TEST201 requests 1,615 core tests, debug assembly, complete immediate-previous compiled TEST200/Codex TEST 196 history, 27 API34 methods and 35 PNGs.
- Candidate pending. No recipe, inventory, energy, RNG, gameplay, save, score, custom-rule, application-ID, signing, delivery or physical-phone change. TEST200 remains the verified compiled fallback; TEST123 remains owner-delivered/signature-compared.



## 2026-10-15T09:21:06Z — TEST201 records-history contract failure; TEST202 correction pending
- TEST201 source `776910414b7574b42cffa0d61e9bb7d4f1e83ca7` / run [38711834653](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38711834653) executed all 1,615 core tests: 1,614 passed and one failed.
- `ContractTests.recordsHistoryDescribesTheCurrentTelemetryVersion` correctly rejected the telemetry 1.14 milestone because it preceded the immutable `APD records history, format 2` preamble. Production compilation and the two new alchemy regressions passed.
- History preservation/validation passed. APK and API34 were skipped; no TEST201 APK exists. Failed core artifact `11960867915`, 752,666 bytes, SHA-256 `e040a5f4330624fd74a0739cf914eb553d34957dcda5d8464acb39f9e6f8befe`.
- TEST202 moves only the new milestone beneath the preamble. Production source, telemetry payload and assertions are unchanged. Full 1,615-core/debug/TEST200-history/27-API34/35-PNG gate pending. TEST200 remains verified compiled; TEST123 remains owner-delivered.



## 2026-10-15T09:36:13Z — TEST202 visible alchemy telemetry verified
- Corrected source `186f5892429ef564ec4a9d59c9885a62fb6f1c1f`, run [38712411656](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38712411656) (09:22:07Z–09:36:13Z), passed 1,615 core tests in 315 XML suites, debug assembly, complete immediate-previous compiled TEST200/Codex TEST 196 history, 27 API34 methods and 35 screenshots; zero failures/errors/skips.
- Both `AlchemyRecordingTest` methods, the wiring guard, fact manifest, telemetry-version test and immutable records-history contract passed.
- Internal Codex TEST 198 APK SHA-256 `6dd8a5e16e13670e6dfb6abd12783dd7225ef8ed936b12b57f1192b51618936d`.
- Artifacts: core `11960394313` / ZIP `639308a8…e5d1f0`; APK `11960578879` / ZIP `f1498ed2…1d3170`; Android `11962595029` / ZIP `ebe2d311…782b5b`.
- All 35 PNGs are valid 320×640 and the full contact sheet reviewed clean. Private delivery skipped; no physical-phone claim. TEST123 remains owner-delivered/signature-compared.
- Telemetry 1.14 now records successful alchemy crafts using only displayed inputs/output, exact quantities, floor and energy split; hidden true classes remain INTERNAL, and failed/restore paths emit nothing.
- No application build is pending; do not rebuild unchanged TEST202. Re-read the latest head before the next distinct approved canonical recording item (level feelings is next in documented order).



## 2026-10-15T09:46:19Z — TEST203 level-feeling coverage gate pending
- Reproduced the documented level-feeling candidate against the exact TEST202 evidence head. There is no missing production event: `GameScene` displays each of CHASM/WATER/GRASS/DARK/LARGE/TRAPS/SECRETS and immediately adds the matching floor-feeling journal landmark; `Notes.add` already emits the public, floor-specific `journal.landmark` fact only on the first insertion.
- Added two retained core regressions: one exercises all seven feeling landmarks and proves exact-once/depth/action payloads; the other guards the seven display→landmark mappings in `GameScene`. Neutral `NONE` still emits nothing and no hidden generation state is exposed.
- Production application source, telemetry 1.14, APK history and compiled artifact are unchanged. TEST203 requests a 1,617-test core-only gate; APK rebuild is forbidden and verified TEST202/Codex TEST 198 evidence must be reused.
- Candidate pending. TEST123 remains the last owner-delivered/signature-compared APK. After this gate, continue the next distinct canonical omission (auto-explore versus travel is next) only after reproducing it.



## 2026-10-15T09:53:26Z — TEST203 level-feeling coverage verified
- Source/test commit `b72e122fbb2e7de02bc03ea42b607197f6105f8a`, core-only run [38715813806](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38715813806) (09:49:46Z–09:52:16Z), passed 1,617 core tests in 315 XML suites with zero failures, errors or skips.
- Both new `JournalRecordingTest` regressions passed: all seven exact public floor-feeling landmark payloads with duplicate suppression, and the complete `GameScene` display→landmark wiring map.
- Artifact `11962917529`, 750,461 bytes, SHA-256 `054cf9b8b999940a9e9628725a5d3a91f983206dc62606a406a7344b32954265`.
- No APK was built, as required. Production application source remains `186f5892429ef564ec4a9d59c9885a62fb6f1c1f`; reuse verified TEST202/Codex TEST 198 APK/API34/visual evidence. TEST123 remains owner-delivered/signature-compared.
- Level feelings are removed from the missing-telemetry backlog. Next safe step is to reproduce the distinct auto-explore-versus-travel omission before adding any event.
