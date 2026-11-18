# Current simulator status — October 10, 2026

## Normal-play profile requested by the owner

Implementation `da573548b6e18c66da2d227a83b524be76c85ac6` adds a defined early-game Warrior profile: normal looting and visible chest opening (including ordinary disguised mimics), known usable equipment, actual seal transfer UI choice, legally allocated talents, usable ranged weapons with clear projectile paths, food when starving, and bounded ordinary rest when safe. Healing remains critical-only, as the owner selected. This models one explicit strategy, not every human player's behavior. Unknown gear/potions are not secretly identified; unidentified experimentation, full quest choices, upgrade-scroll targeting, retreat optimization and complete dungeon wins are not implemented.

Raw adjacent melee maxima are conservative triggers, not exact final incoming damage: armor, misses, multiple attackers, poison, ranged attacks and timing can differ. Consumed healing before a normal Fly Healing drop is an observed action, not proof that it prevented death. No Warrior starting identification or game balance is changed.

Rare encounters are retained in per-attempt class counts and raw failure records. All audited standard/rare early rotations, Wraith variants, Mimic variants, EbonyMimic and actual Statue/ArmoredStatue weapon maxima are covered without consuming gameplay RNG. Unknown mechanics produce an explicit incomplete attempt, never a fabricated completed run. Generation seeds are recorded; combat RNG is not seeded, so generation seed alone is not a deterministic combat replay.

## Verified and pending evidence

- Local profile/framework/bounds: 21 tests passed; trace recovery 6 and direct instrumentation safety/provenance 6 passed. Fixtures are never counted as played episodes.
- Current source `455f22b5f14c1ea9886d033b81d0a1270ea5cd20`, run38088088384: actual downloaded core XML totals **993 tests, zero failures/errors/skips** across161 suites; Android app and native test APKs compiled; **all five actual native baseline episodes passed**. JSON shows four deaths and one first-normal-Fly-Healing stop, two consumed held Healing before that stop; rare Albino is recorded. Five episodes are not a population estimate or counterfactual survival proof.
- The Android source-set fix excludes root-level desktop test fixtures while retaining packaged native tests/helpers. Earlier da573 failed native compilation on HeadlessTestSupport; earlier99a4 compiled but failed emulator provisioning. Neither failure is reported as gameplay execution.
- Actual provisioning failures required7372.80MB while only7275MB remained. A4096M partition setting did not shrink the minimum base image. Copying checked APKs before Gradle clean and removing only unused .NET/Haskell SDKs on disposable runners, with a9GiB preflight, now passed actual image setup and gameplay. Gradle, required Android tools, repository/player files and signing identity are preserved.
- Batch run38088289249, workflow `c2abd72335052ea3dd8dacb2883ff5962e27edac`, has passed its exact-source baseline gate. Five disjoint shards are actually executing the other95 seeds with the same precompiled source455 APK pair; baseline five are reused. **The full100-seed result is pending**, not marked passed. Failure traces and rare encounters remain retained. This is GitHub Actions gameplay execution, not continuous autonomous Codex coding.
- Evidence aggregation now has11 permanent Python fixtures: mixed play profiles on one source cannot falsely complete one coherent cohort; per-profile outcomes remain separate and all attempts retained. A fixture first failed before the fix. Commit `4c6ab423dad93e54c64728bcb11c04baf08ba78c`; packaging run38089262932 reran all11 successfully. The pinned batch's fixed profile uses the earlier verified tool; no mixed profiles are possible in this specific fixed-profile invocation. Its evidence can also be reprocessed with the new checker.
- Download package is genuinely published as a **private Actions artifact**, not a phone app: [apd-warrior-simulator-download](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38089262932/artifacts/11683303102),37,495,318bytes, zip SHA256`16580e465d17f08c78eaabd99510f30fc950f5bd1721dee016611d58ff0ca050`. Run38089262932 passed at21:57UTC; delivery/tool source `286dcb481f3a31d8bafe72931333f451e52fcdc9`. Both APK checksums were verified again before packaging. Package includes guide, actual baseline traces, source/checksum manifests and tested tools; it requires GitHub sign-in and expires after seven days. **Use only a new empty computer emulator, never the player's phone installation.**
- APK SHA256`e8ab23b5d2b74c8d376d7557a93032157f07ad388a7bd8bc75d7b9ac710b047e`; paired test APK SHA256`1a0694939bbea9eb72d7da9066e492bc3ffafecbec5dfa04caa0b1ce17551bb1`. These simulator binaries are not a normal playable update.
- Source6b7a5 also passed five real episodes on run38087941429, including Albino/Wraith encounters. Keep those independent results separate from source455, since combat is not fully seeded.
- Old reckless source0a827d398330356512e90b8ea347bea6939dd209 has62 actual attempts/43 completed early episodes,57 unresolved requested seeds. Preserve its failures as historical diagnostics; do not pool it into the normal-profile cohort.
- Earlier run38084780497 recorded a completed episode then hit libGDX/ActivityScenario pause synchronization SIGKILL. The simulator keeps its disposable activity alive through instrumentation completion. This is not a claimed production lifecycle or physical-phone fix.

## Application delivery

TEST26 is the verified delivered playable application: source1483bac7ecd24cf33b64632ca0d4135bb252ad7d, 972 core tests and six API34 device checks passed, signature/appID/checksum verified, private publication2026-10-10T20:20:14Z. Installation-over compatibility is established by identity/version checks; physical-phone save/upgrade testing is not claimed. Simulator APKs are disposable test infrastructure and must not be substituted for this APK.

Simulator line-count report is deferred until completion, as requested. Actual job/episode timestamps are recorded; summed parallel episode time is not developer active time or idle time. The native baseline/package are verified; a completed same-source/profile 100-seed cohort remains required before declaring the requested experiment finished.

---

## Earlier implementation history (historical statements below)

# Warrior early-healing experiment — development evidence

This simulator is test infrastructure, not a balance recommendation or automatic balancing system. It drives the actual Android GameScene, actor scheduler, combat, movement, item pickup and healing actions. It never generates a replacement probability model or counts generated floors as played runs.

## Scope and policy

Warrior, Standard difficulty (3), no challenges, existing starting equipment. The current reckless policy attacks adjacent visible enemies, pursues visible enemies, explores discovered traversable tiles, picks up ordinary heaps and eats when starving. It drinks a held Potion of Healing when current HP is at or below the audited selected adjacent enemy's raw melee damage maximum. It does not retreat, optimize surprise attacks, spend talents or equip found upgrades. Optional/unknown encounters, unhandled dialogs, blocked actions and timeouts are recorded as incomplete.

An episode ends at death, first recorded normal Fly Healing drop, or floor 5. These are early-game episodes, not complete dungeon victories. Consuming healing in a threatening situation is an observation; it does not prove that the same action without healing would have caused death. Results do not establish the owner's playstyle, a population rate or a reason to change Warrior's starting identification.

Only actual `ITEM_GENERATED` evidence with container `C_MOB_LOOT`, owner `actors.mobs.Swarm` and true class `items.potions.PotionOfHealing` qualifies as normal Fly Healing. Bonus loot and Shielding are not mislabeled as Healing. The log is flushed/read only when the normal Swarm drop counter changes. No damage-roll/RNG call is made to predict damage; gameplay still consumes its normal RNG.

The batch uses explicitly recorded seed ranges, preserves per-action state traces, terminal HP/depth, completion/failure reason, normal Fly Healing observation, wall-clock start/end and monotonic elapsed time. Two consecutive adapter failures stop a shard instead of repeating a broken setup indefinitely. Missing, duplicate, unexpected or incomplete seed records cannot become a successful 100-run report.

## Isolation

Execution requires explicit simulation opt-in and a disposable Android emulator. The adapter refuses physical hardware, existing saves, existing rankings or existing run logs. Tests affect only that disposable emulator's state. No owner saves, signing identity, application ID, Claude branch or main are changed. No simulated run log is imported into the owner's UIMSD history. Simulator APKs are test infrastructure and are not claimed to be lunch-test deliveries.

## Actual implementation and checks

- `61d3ba9b86d82b55ee40359ca25aff749718b718`: actual Android adapter and known-map navigation. Twelve local navigation/policy tests passed (0.068 seconds). Run `38079881160`: Android debug/test compilation and one connected check passed (1 test, zero failures/errors/skips, XML duration 28.41 seconds). The overall job failed afterward: result extraction ran after emulator shutdown, leaving an empty JSON file. No per-seed result or percentage is claimed from this incomplete evidence.
- `4bd6b536e2cf485eb252e33dc9f3a525d4195c0a`: corrected recorder class-name handling and added existing-data/emulator guards. Fifteen local cases passed (0.062 seconds). Complete CI core XML: 962 tests, zero failures/errors/skips, artifact `11679842395`, run `38080008651`. Android setup failed downloading the default API34 image (`Error on ZipFile unknown archive`); simulator execution did not begin.
- `5a78b74a2374dd493b561a3f89f0500307af6a73`: actual serialized-recording regression, improved failure/terminal-state diagnostics, supported google_apis image recovery. Sixteen targeted local tests passed (0.391 seconds). Its uncompleted workflow was superseded by the extraction-order correction.
- `3402d7cabf5f5c08ffbd2d65a6b9713351cbb911`: extracts traces before shutting the emulator down; preserves test failure status and rejects missing evidence. Shell syntax passed. Run `38080454423` is pending at this report checkpoint; no successful corrected episode or completed 100-seed batch is claimed yet.

## Efficiency work

Large-batch preparation compiles one application/test APK pair, preserves their checksums/source manifest, and reuses them across five disjoint 20-seed emulator shards. Each shard installs that pair without Gradle rebuilding. This batch workflow is being prepared; syntax validation alone is not execution evidence. The first usable compiled pair must be preserved: earlier checks did not retain these binaries, so those discarded outputs cannot be reused.

Current limitations: only audited normal Sewers enemy bounds, no full counterfactual replay, optional quests/unknown enemies may interrupt an episode, fixed basic strategy rather than a complete configurable gameplay bot, action limits, no physical-phone acceptance. No Warrior identification change is implemented.

## Timing and delivery distinction

Repository commits, workflow timestamps, XML durations and per-episode clocks are evidence. Exact accumulated active coding time, time typing versus thinking and historical idle periods cannot be reconstructed from commits; no invented overnight time accounting is supplied.

TEST25 remains the latest delivered APK: source `42aa5ff0a332eab3afe12f036c9beeeb1bfb5e71`. It contains the Community privacy correction and has 948 core plus six API34 checks and verified signing/checksum evidence. New normal enemy-drop identification source `63fe8a693794b3421cebeb9dc62550ba97ba185b` passed 955 core tests but is not in TEST25. Simulator checks are not a substitute for the required playable APK regression and signing/delivery checks.

## Concurrent scheduled execution observed

Scheduled development advanced the branch to d5957b5cb370e3f2bcfc97aefa4cad4510a98d26 while this active turn was preparing a correction for the same evidence-loss problem. Its test-only Base64 log-chunk emission and strict reconstruction script are preserved. This is confirmed unattended code/evidence-plumbing work, not proof of continuous overnight coding or a tested new Android result. An unpublished alternative commit eb8c9cbd0d33fd9d2445c16ba861c1b848344628 was not applied; no synthetic/local history was pushed. The latest workflow retains APK binaries for subsequent direct-instrumentation batch reuse, and requests a new verified single episode with the preserved scheduled fix.
