# Waiting interruptions and Auto-Eat While Waiting

## Implemented source behavior

APD Settings > Gameplay has Waiting interruptions, Auto-Eat While Waiting, and an independent Hungry/Starving food trigger. Defaults retain full-health regeneration and starvation-onset stops. The new percentage-based critical-health stop is off by default, so no new early stop is imposed on existing players; existing damage/enemy/manual safeguards remain. Its configurable threshold is10–50% HP. The explicit Disable these three stops preset only disables those three preferences and leaves Auto-Eat/its trigger untouched. Continuing through starvation can kill the hero; normal hunger damage remains.

Auto-Eat defaults off and runs only during continuous waiting, without visible enemies, paralysis, a locked floor or an incompatible action. It chooses the smallest-energy available exact ordinary Food/SmallRation/ChargrilledMeat/StewedMeat class; unidentified, cursed, raw, unknown-subclass and special foods are excluded. Berry, pasty, frozen/cooked-fruit and other special foods remain manual choices. Actual Food.execute(EAT) consumes one unit and runs normal hunger/challenge/talent/catalog/statistics/time/animation handling. There are no free actions or hunger changes. Default3-turn eating or existing talent-shortened1-turn eating is preserved. A cancellation epoch prevents continuation from overwriting a user/danger interrupt during the meal.

At starvation onset, normal damage and hunger time still happen. If appropriate food is available and Auto-Eat is enabled, only the waiting starvation stop is deferred to the next ordinary hero action. If food becomes unavailable, the selected starvation stop is honored. Critical-health stop has priority over eating. Disabling all three removes those automatic stops, not other damage/enemy/manual safeguards. No active wait/eating command is serialized or restarted on load; preferences persist independently. Single-tap Search and all current source/marker work remain intact.

## Tests and boundaries

20 new headless regressions exercise defaults, independent persisted preferences and actual file reload, threshold validation, real regeneration, real starvation damage, all-three-off waiting, critical health, normal food execution/quantity/statistics/3-turn and talent1-turn costs, Hungry versus Starving triggers, unavailable/exhausted food, dangerous/unknown/cursed/special food rejection, food selection, no eating when off/not waiting, critical priority, cancellation, locked/paralysed cases, ordinary-damage interruption, and hunger/food Bundle reload. Local full2525 tests443 suites pass. Native interface tests are extended for the three new parent controls, all three independent checkboxes, no-stop preset, independent Auto-Eat/trigger state, and reopening saved controls. These native assertions are staged/compiled, not claimed executed against a new APK.

APK365 remains the last released verified pair and does not include this source. The conservative November5 NewYork three-pair allowance is already used. No application build request/version bump or fourth pair is created. Preserve private release assets, signing/appID and player saves/statistics. Required Android debug/release/instrumentation Java plus desktop source checks are recorded after actual completion.

## Continuing work

Do not stop at this feature. Continue the owner's descriptions/formulas audit beginning with Obfuscation/Arcana, and retain the disappearing-marker investigation. The door-adjacent corner's disappearing mark is reproduced and corrected in earlier verified source878a05f1d; valid saved-marker retention tests pass. A separate unexplained loss of valid marks remains unconfirmed, and the next permitted native candidate must check rendered camera/lifecycle behavior rather than claim it fixed from model checks alone.

## Follow-up audit

Fractional hunger thresholds now use actual Hungry/Starving predicates instead of ceiling-rounded display values. Subsequent healing audit reproduced four additional full-health interruption paths ignoring the preference, and tested the potion Healing last-tick path too. Healing, Sungrass, aquatic rejuvenation, Well Fed and Chalice healing now honor the full-health preference; completion below full HP retains its original interruption. Healing amounts/time/RNG remain unchanged. Six more real-effect tests plus the three fractional/challenge cases are retained. Final follow-up source/Android gate must be recorded after completion, separate from the prior2525 source gate.
