# Remove inherited donation promotions — October 16, 2026

## Confirmed cause
WornKey.doPickUp checks the inherited supportNagged preference. When false, it saves and schedules WndSupportPrompt before calling the normal Key pickup. That window combines an SPD developer greeting with SupporterScene's Patreon appeal and an external Patreon button. This explains a promotion after Goo's key independently of boss or reward-generation logic.

A second active route is WndVictoryCongrats:its final paragraph asks for support and its button opens SupporterScene. That scene contains the other Patreon URL. A source/reference search found no other active direct donation endpoints. Old title-page Patreon strings were also present but not used by APD's current TitleScene.

## Bounded correction
WornKey now inherits the existing Key.doPickUp unchanged. Journal registration, quantity, normal pickup turn cost and Skeleton Key interactions remain in that shared path. No boss, loot, key, save format or preference value is modified. The promotional extra pre-pickup save is removed with the promotional hook; ordinary game saving is unchanged.

The victory dialog retains its normal content and thank-you text, removes the support appeal/button, and uses the existing Close button at full width. Legacy WndSupportPrompt and SupporterScene class names remain compatible:the old window contains only neutral upstream attribution and the old scene routes to the existing About screen. Neither contains a donation link. No new promotion or payment feature is added.

Removed276 obsolete promotional localization keys from46 source-verified resource files:23 scene bundles and23 window bundles. Each local original matched the real approved branch's Git blob before editing. Removal is limited to SupporterScene, old title Patreon keys, the old prompt keys and victory support keys. Legitimate About credits, copyright headers, GPL notices, artists, music/sound credits and all their links are unchanged. Historical SPD change notes mentioning old donation features remain historical facts rather than active donation appeals; no donation link is retained there.

## Regression and verification
Two new permanent core methods cover actual WornKey pickup with either old support preference, journal key count, one normal pickup action/cost, unchanged old preference, runtime-source donation URLs/localization appeals, and required attribution/license text. Both pass locally. The first local attempts exposed only a missing native-library classpath and uninitialized test Notes; the isolated fixture was corrected and rerun. Do not call those setup failures passing tests or production regressions.

The existing full Android gameplay method now picks up WornKey with both preference values and verifies no support window appears. It opens the victory dialog, checks its sole Close action, captures boss-key and victory screens, then returns to gameplay. Expected complete next gate:1756 core methods,37 Android methods,57 required screenshots, debug assembly and exact previous TEST259 APK history. Candidate pending until actual CI and visual evidence pass. TEST258 remains the verified owner-delivered fallback.

## Timing and limits
This urgent tester report was handled while TEST259's prior Markdown fix was already built and undergoing Android verification. The next unique application build must compare against actual TEST259 source8d9381e5116678f51fb2f2d70b23a681102a85bd/run38973264847, not relabel the Markdown fix as new. Android evidence is emulator-only; the tester's phone and existing owner save are not attached. No public release or automatic tester distribution.

## Verification tooling correction
The prior TEST259 display matrix stopped before application tests because its producer wrapper did not create acceptance-source.json required by the shared runner. It is retained as a failed check, not an application defect or passing display test. This wrapper now verifies ZIP/APK hashes, writes the receipt, checks effective sizes on a tall emulator profile and pre-confirms Android's first-use full-screen notice. No gameplay or rendering logic is changed. This correction is recorded in the next build's technical history.

## TEST260 failure and test-only correction
Actual sourcecd4879a0381c55a0c88104252470ed59e7917854/run38974830422 ran1756 core methods:1754 passed,2 failed,0errors/skips. No260APK or Android gameplay result exists. The new scan assumed the checkout working directory, but Gradle runs tests from core/src/main/assets; it now uses the existing apd.game.src property. The existing English safety test scanned every retained message successfully, then failed its stale >5000 inventory sanity check after12 obsolete English promotion keys were removed. The exact current inventory is4998 in9 files (previously5010); all per-message profanity/clean/adult assertions remain unchanged and the count is now exact. Corrected local checks passed from Gradle's actual assets working directory:the2 promotion methods and the full English message safety method. No production correction or guard removal was needed. Full repeated1756core/debug/37Android/57PNG is pending; preceding actual APK remains259, delivered owner fallback258.

## Verified and delivered
TEST261 verification/publication completed as recorded in docs/APD-TEST261-OWNER-DELIVERY-2026-10-16.json. Core1756 and corrected same-APK required native37 passed;77required images reviewed over five accepted display configurations. Original native compilation failure and one Quickstep-ANR display failure remain failed, with corrected fixture and clean small emulator retries separately identified. Original signing/appID preserved; physical-phone and owner active-save install-over remain unverified. PrivateAPK/PDF direct links delivered in chat between22:45:28Z and22:46:46Z, after actual download checks. TEST258 assets preserved.
