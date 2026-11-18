# Existing optional panels awaiting owner review

This is an explanation of existing controls, not approval for new controls or a universal calculation system. The owner asked to keep the existing mechanic-specific panels pending review and requested an explanation before deciding. None of the groups below was added by this continuation.

| Existing panel | What opening it explains | Current decision |
|---|---|---|
| Ring of Force | How Strength and ring level determine unarmed damage, the separate armed bonus, and Brawler’s Stance. | Existing panel retained pending review. |
| Ring of Tenacity | Why protection grows as health falls, including the actual health-dependent formula. | Existing panel retained pending review; normal text uses less/more damage. |
| Artifacts | Artifact-specific charge generation, consumption, upgrade and special behavior. Examples: Cloak duration/recharge; Rose ghost health/gear; Hourglass stasis/freeze; Horn food; Chains charge limits. | Existing group retained pending review. Normal descriptions are being shortened separately. |
| Trinkets | How each supported trinket’s level changes its particular chance, range or trade-off. | Existing group retained pending review. This does not authorize panels for ordinary consumables. |
| Wand of Warding | Ward growth, limits and how multiple wards/sentries behave. | Existing panel retained pending review. |
| Wand of Regrowth | Plant production and the wand’s cumulative use limits. | Existing panel retained pending review. |
| Retribution / Psionic Blast | Health-dependent blast effects and, for Psionic Blast, target-dependent self-harm. | Existing panels retained pending review. |
| Glyph/enchantment interactions | Existing identified non-curse glyph/enchantment details, including Arcana’s effects. | Existing scope still needs individual complexity review. Elastic and Viscosity have no calculations button. Do not interpret retained scope as permission for every future effect. |
| Separate talent, buff, armor-ability and Cleric-spell windows | Their existing mechanic-specific effects, costs and modifiers. | Explicitly kept pending review; no blanket removal. |

Ring of Arcana itself has a concise percentage-only ordinary description and no calculations button. Ordinary food, seeds, utility items, plain weapons/armor, ordinary wands and routine scrolls do not receive a generic glossary panel. The legacy `MechanicsDetails.item` helper is not the normal item-window entry point; `WndInfoItem` calls the selective `itemCalculations` method. Exported legacy helper text must not be mistaken for a currently displayed panel.

The comprehensive audit remains unfinished. Retaining an existing panel pending review does not mean its wording, every branch or every numerical claim has passed a completed individual audit.
