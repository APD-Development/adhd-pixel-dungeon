# APD player release notes — October 15, 2026

## Scope and attribution
The default APD Changes view now reads a separate curated release catalogue, using the existing ChangeInfo/ChangeButton/WndChanges components, paired categories, gold-tinted native frame and paginated version picker. Fourteen concise, individually clickable entries are grouped into an undated APD Alpha Preview. This is a development preview, not an invented public release or publication date. The existing technical build index and every historical record remain unchanged and accessible from Versions > Build records (technical). Technical list headings use APD Test labels; original identifiers and original status remain in the explicit Build record information popup. Back returns to the default player release view.

The initial preview describes implemented content present in verified source cf7a46191d7758f73d4901e6781eb44664f4a8d4 (TEST223 verification run38810321509), plus this presentation correction when its own gate passes. It does not declare a public alpha release, a finalized score multiplier, completed device testing or an enabled online service. No game version, mechanics, saves, raw history, rules, score, application ID or signing key changes.

## Evidence behind preview entries
- Custom Dungeon Rules / Choose your upgrades: apd/difficulty/CustomDungeonRules.java, CustomCombatHooks.java, CustomGenerationHooks.java, UpgradeScrollPlan.java, windows/WndCustomDungeonRules.java. All 18 min/max touch-save-reopen acceptance and full recorded-field filter/export checks are documented in TEST218.
- Your dungeon history / Filters and comparisons / Rules recorded with runs: apd/recorder, apd/stats, apd/query, apd/report and retained CustomRuleFullFieldAcceptanceTest. Historical unknowns and original earned scores are preserved.
- Reliable statistics views: retired async model/replay generations, verified separately in TEST216 and TEST217, included in TEST223.
- Clearer numbers: player-unit fixed-point display, lower-bound rounding and mixed-scale ratios are present in TEST223. This note describes display corrections, not changed raw calculations.
- Blacksmith rewards: windows/WndBlacksmith.java; Chalice preview: apd/ChaliceDamagePreview.java and items/artifacts/ChaliceOfBlood.java. Both already retained by the full Android gate; no mechanics are changed here.
- Healing conversion setting: TEST222/223 Generator.java, ExoticCrystals.java and APDSettingsPane.java; affects generated Healing potions only.
- Adult Humor preference: reviewed AuthoredLanguage alternatives and AboutScene, verified by TEST222/223. The wording deliberately does not claim a universal device-level profanity guarantee.
- Scoring notice: original scores remain unchanged; no calibration or competitive multiplier is claimed.
- Saved entrance repair: ApdIntro.java / Dungeon.java retained TEST195 repair; unrelated secret doors remain untouched.
- Developer Commentary: brief preview context; no release or feature availability invented.

## Future release process
Keep internal APK history complete using the existing reviewed path-by-path ledger. Do not automatically copy internal entries into the player release catalogue. Add actual APD release versions to apd/releases/index.tsv only when their identity and contents are verified; dates must reflect actual publication, and unreleased previews are undated. Curate concise player-facing entries from the exact predecessor/source/artifact comparison before promotion. Existing build tooling now validates player branding, categories, duplicate/empty/oversized entries and preview date honesty before embedding, and verifies the compiled player TSVs byte-for-byte alongside the full technical archive. This keeps future internal TEST numbers out of the default interface without inventing prose or releases.

## Verification
Local Python history/release tooling: 18 tests passed, including six new regressions. All 14 actual preview entries passed build-time validation and their sprite identifiers match native Icons/ItemSpriteSheet constants.
Added seven permanent core Java tests and one Android release-view method; existing full technical-history/icon-popup/large-build tests remain and explicitly opt into technical mode. The Android gate retains the original SPD reference, public preview, technical archive and large-build screenshots: 40 expected PNGs.
This executor currently has no usable javac executable, so Java compilation and Android rendering are pending the requested full gate; no local Java or physical-phone pass is claimed.
Requested full gate: 1676 core methods, debug assembly, complete immediate-previous produced TEST223 history, 32 API34 methods and 40 valid PNGs. No automatic publication or tester distribution. TEST222 remains the actual owner-published playable fallback; TEST223 is separately source/API34-verified.

## Separate Android16 acceptance
An earlier reused-APK run38811530072 failed before game tests because emulator userdata needed7372.80MB while only7038.70MB was available. Newer real branch commits already corrected the AVD configuration and isolated Android16 input fixtures; this change preserves those commits and does not apply the older unmerged disk-cleanup proposal. Current API36 results retain their independent evidence boundary; repeated tests on another OS are not new unique core methods.
