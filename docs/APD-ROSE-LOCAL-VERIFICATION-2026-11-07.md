> Delivered as APD373 after the owner-approved exception. Full2951 core/10 API34 and signing/source/download checks passed. See APD-373-OWNER-DELIVERY-RECEIPT.json. Earlier local-only and quota-blocker paragraphs below are historical.

# Dried Rose explanation work — local, not delivered

The user requested continued approved work, then explicitly stopped source-only uploads and required actual APK delivery. No Rose source commit has been uploaded. APD372 remains the delivered game; neither newer Cloak nor Rose text is included there.

Changes: RoseExplanation derives only known Rose-level/public hero-level base HP, equipment strength, unarmed damage and attack/evasion inputs. It explains summoning prerequisites/time/resource, equipment transfer eligibility, hidden-gear withholding and upgrade health timing. ArtifactExplanation corrects the generic percentage claim: with a living ghost the icon is health%, while stored summon charge is a separate resource. Existing ordinary/artifact-specific interface reused; no new panel/control or gameplay/save changes.

Actual source inspected: DriedRose execute/status/ghostStrength/upgrade/save/restore, GhostHero construction/updateRose/attackSkill/damageRoll/defenseSkill, equipment selectors and passive/external recharge. Tests invoke real GhostHero methods across all11 Rose upgrades and3 hero levels, sample actual unarmed bounds under controlled RNG, verify health-icon versus stored-meter mismatch, preserve actual pre-refresh upgrade healing/cap behavior, compare hidden transferred weapon levels, and exercise compressed Rose save/load plus inspection/RNG/state purity.

Local targeted Rose/Cloak/mechanics tests passed: /workspace/apd-rose-targeted.log. Final full suite2951 tests/451 suites passed, zero failures/errors/skips: /workspace/apd-rose-full.log and /workspace/apd-rose-local-junit.zip. These are JVM/compilation results, not a newly verified Android APK or native/physical-phone acceptance. Cloak's earlier source-only platform run42416196043 separately succeeded ondcad47246; it is not evidence for these later uncommitted Rose changes.

Packaging blocker: recorded daily policy says10 total APKs, five Developer/five Alpha. The preserved handoff/372 receipt reports eight actual game APKs today, five Developer and three Alpha, including earlier internal trap builds. A further Developer APK needs a specific exception to that recorded per-type cap. No additional source-only push or game APK build performed under this uncertainty. Local source/tests are preserved for the next authorized APK delivery.

Owner subsequently explicitly approved the additional Developer build and ordered publication. Proceed as373 with this Rose work and the previously verified Cloak source, one pair only, within10 total. The earlier per-type blocker is superseded for this one delivery.
