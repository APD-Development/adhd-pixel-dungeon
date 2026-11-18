# Short ordinary weapon tooltips

Owner asked to shorten the ordinary sword description shown in APD374. Removed generic combat tutorial paragraph and repeated ring-effect disclaimer. Kept live normal attack time, concise accuracy (explicitly not hit chance), and damage before/after. Force and Sharpshooting summaries now directly name the ring and damage change. Existing relevant mechanic-specific details and all gameplay calculations remain unchanged; no new interface added.

Changed production files: WeaponExplanation.java, RingItemEffects.java, items.properties. Regression assertions cover concise normal Sword timing with actual delayFactor, short Force damage range with no boilerplate, existing ranged accuracy/unknown-value safeguards and existing mechanics descriptions. Targeted tests passed. Full core results recorded below after completion. No APK built or published for this change; APD374 does not contain it. No extra APK release authorized beyond the recorded daily limit; do not relabel APD374 or overwrite its assets. Preserve this source for next authorized APK delivery.

Full local core suite: 2955 tests /451 suites, zero failures/errors/skips. Log /workspace/apd-short-weapon-full.log. Android build/device verification not performed for this source change.
