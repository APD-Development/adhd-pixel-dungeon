# Dew explanation audit — source only

Owner APK hold remains active; delivered369 is unchanged. This batch is not a completion claim for the whole mechanics audit.

The previous dew explanation mixed internal terms with vague references to caps. The regular Waterskin tooltip also promised instant healing, although Vial of Blood can deliver multi-drop recovery over time.

The new optional calculation panel explains drops as resource units, base healing in HP, whole-point rounding, separate temporary Barrier points and their live talent cap. It shows actual current maximum HP, one-drop and five-drop base values, the inspected skin's stored volume and current Barrier. It explicitly distinguishes base value from final recovered HP and explains missing-health limits, Vial timing, fractional run-healing rules and automated collection safeguards. No sample hero is fabricated in catalog inspection.

Actual dew use and the calculation panel share the unchanged base-effect and talent-cap formulas. The Waterskin's existing drop-selection, consumption, confirmation, timing and serialization paths remain unchanged; getters expose its existing capacity, volume and drinking time. The same talent-cap helper replaces the identical inline formula in drop selection. There is no new RNG or save field.

Targeted tests passed in `/workspace/apd-dew-explanation-targeted.log`: 64 maximum-HP/drop-count/talent combinations compare real healing/shielding with the original formulas. Live storage/HP/cap readouts and item/Hero/RNG/fractional-credit inspection purity are tested. Existing CustomHealingSliderTest also passed. Full local suite passed 2,874 tests / 446 suites with no failures/errors/skips; raw XML `/workspace/apd-dew-explanation-local-junit.zip`. The final English Waterskin wording correction followed that run and will be covered by the required remote full source check; remote source compilation remains pending. Vial's complete cross-product and native/phone text acceptance are not claimed by this batch.
