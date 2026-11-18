# APD lunch development report — October 13, 2026

## Delivery candidate and verification
Today's candidate is unchanged fallback TEST121, not TEST122. Exact application source: 010c44c877ef03dca7fe5ac2987cde309471d089. Verification run: 38410865529. TEST121 successfully assembled with 1370 unique core tests and 22 API34 emulator tests, all passed; zero failures, errors or skips. Twenty-five screenshot PNGs were captured. The corrected narrow-screen custom-rules scoring notice was manually inspected. Automated/emulator evidence is not physical-phone testing or a complete ordinary playthrough.
The preparation workflow reuses the existing APK artifact 11822370585 and compares its actual signing certificate, application ID and version against verified TEST67. The generated verification attachment supplies the APK checksum and result. Artifact ZIP digest is not an APK checksum. Until this gate succeeds, signing compatibility is pending.
The staging checkout is pinned to TEST121, not the later failed application source. No new APK build, no public release, no online activation and no early delivery.

## Actual changes included in TEST121
Player Healing Effectiveness is implemented across 25 recovery sources using one shared player-only scaling hook. Fractional recovery is persisted safely, preserving small heals; maximum-HP changes, starting HP, revival and shielding remain excluded. At 100 percent the hook preserves original recovery and RNG behavior. Recovery cannot bank overhealing for later use, and zero-healing restorative sleep cannot trap the hero asleep permanently. Updated item/recovery descriptions show scaled recovery where applicable.
Seven permanent core regression methods cover boundaries, fractional/save behavior, neutral RNG, exclusions and actual class recovery. Fifty-six targeted checks passed before the full successful CI run.
The experimental Custom Dungeon Rules notice remains visible above sliders. The narrow scrolled Android capture was corrected to show the scoring paragraph rather than the bottom controls.
The owner-requested Cleric collection cleanup changed 348 entries to exactly 50, retiring 298 duplicate/less distinctive entries while retaining selected entry IDs and intended trigger access. Three new regressions cover count, uniqueness/accessibility and the unchanged release gate. Eighty-two targeted humor checks passed. The early-alpha lore release gate remains off; no Cleric balance changes or hidden content are announced.

## TEST122 was rejected
Equipment Availability source commit 6054e401f901a981eaa21ed7af2e1310a9127889 attempted ordinary floor, room, shop and monster equipment-generation integration without filtering player-owned inventory or special quest/progression rewards.
Its verification run 38416567702 executed 1379 unique core tests: 1376 passed and 3 failed. Android checks were skipped and no APK was produced.
Failures: GuaranteedLootIdentificationTest canonical normal-drop identification-hook placement; ItemHookWiringTest canonical provenance-hook call; CustomEquipmentGenerationTest neutral floor snapshot mismatch. All neutral shop snapshots matched. The floor mismatch is unresolved: complete original TEST121 source was independently reconstructed and matched Git blob identities, and local original/candidate records match each other, but differ from CI floor golden records. This does not justify replacing expected values with failed candidate output.
Local repairs preserve canonical identification/provenance calls; 15 targeted existing/new hook checks passed. They are not yet committed, full-build verified or included in this fallback.
A local isolated crystal-chest extra-loot placement loop was corrected in the failed equipment source by placing the generated batch before sealing; the regression passed locally. TEST122's rejected source is not a deliverable.

## Risks and remaining work
Fourteen of eighteen gameplay slider effects are included in verified TEST121. Equipment is not accepted after TEST122's failed gate. Player, Enemy and Boss offensive Damage effects remain incomplete, including delayed/summoned/explosion/DOT attribution and exactly-once scaling. These must preserve unowned hazard and self-damage exclusions.
Mandatory permanent UIMSD ruleset/count recording, run-detail/filter/comparison/export acceptance and scoring interfaces remain incomplete. Original earned scores remain unchanged; no calibrated custom multiplier has been approved.
Remaining alpha gates include combined modifiers, all six classes, Difficulty1–5, nine challenges, boss phases/progression, save/load locks/recovery and physical-device acceptance.
Healing risks include fractional recovery, zero-healing behaviors and temporary-buff interactions. Equipment risks are isolated from the fallback because that source is not in TEST121. Existing saves/statistics are not edited, cleared or reset. Signing compatibility does not by itself establish every save migration.
Wider UIMSD audit/redesign is deferred per owner direction until sliders; historical changelog reconstruction, sentry timing, seed-specific armor accessibility and inherited audio investigation remain open where documented. No unconfirmed issue is represented as fixed.

## Commits and timing
26bae9b49aa7190cce24cadc7cce71cc9c6752c2: Cleric curation.
010c44c877ef03dca7fe5ac2987cde309471d089: TEST121 healing and corrected Android evidence target.
6f12d0def0f15e7062f95cfee1609d95675da646: verified TEST121 handoff/evidence.
6054e401f901a981eaa21ed7af2e1310a9127889: failed TEST122 equipment candidate.
a4d49f15d8ff1291d0c3fa0d9ed4625d63f7b2e7: owner-requested remaining-task PDF and failed-gate record.
TEST121 CI ran 10:08:54–10:20:32 a.m. New York, 11 minutes 38 seconds. That is CI elapsed time, not active coding time or proof of uninterrupted work. This lunch preparation began about 11:18 a.m. New York. No reliable hour-by-hour active-time ledger exists; idle time is not fabricated.
Friday October16 early alpha remains the target, broader playable alpha November1. Remaining work estimates in the separately requested planning PDF are active engineering estimates, not guaranteed background execution.

## Install and staging
Install the compatible APK over the existing APD application. Never uninstall or clear application data. Preserve the last working APK and player data. Private staging only; the separate 11:55 a.m. PDF and 11:59 a.m. APK delivery tasks must use the final verified staging evidence. No continuous unattended execution has been demonstrated.
