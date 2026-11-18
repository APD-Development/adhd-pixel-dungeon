# Cleric balance audit (facts from the source + proposal options; NOTHING changed)

Scope: the owner's complaint that Guiding Light (2-8 damage) does not scale with the Tome's progression, and that Cleric should be fairly balanced and normal-statted.
Status: audit only. No formulas are proposed from thin air; each option below says what it would change and what must be measured first. Balance changes need approval and
must follow the difficulty rules (the baseline, difficulty 3, only changes through a deliberate, versioned ruleset decision; see the Cleric ruleset decision in the handoff).

## What the source says (SPD 4.0.0 base, unmodified by APD)
- Start kit (`HeroClass.initCleric`): Cudgel, Holy Tome (identified, in quickslot 0), identified Potion of Purity and Scroll of Remove Curse.
- Tome charges: starts with `min(level+3, 10)` charges, cap `min(level+3, 10)`; the Tome has its own level (exp from charges spent, so it levels by being used). Recharge: one charge per `(45 - missing charges)` turns (more missing = faster), scaled by Ring of Energy; Light Reading talent adjusts it.
- Guiding Light (`GuidingLight`): costs 1 charge (free once per 50 turns for the Priest subclass); ranged bolt, `Hero.heroDamageIntRange(2, 8)`; applies Illuminated (next non-encumbered physical attack against it always hits). The damage range is a FIXED 2-8: it does not read the Tome's level, the hero's level, or talents. Priest: when Illuminated is consumed by Radiance, wands, allies or some artifacts it deals bonus magic damage = hero level + 5.
- Other damaging spells: Sunray (fixed 6-12 / 4-8 / 12 / 8 depending on target state), Holy Lance (min/max computed from state), Judgement (base..2*base on all visible enemies), Smite (computed range + guaranteed hit), Radiance (hero level + 5, with stun).
- Subclass spell/ability lists: Priest / Paladin; armor abilities Ascended Form, Trinity, Power of Many.
- APD changes today: none to Cleric mechanics (only the black-screen block, which is to be removed; see the handoff decision).

## Observations (facts, not conclusions)
1. Guiding Light is the only early damaging spell and its damage never grows. Most other damage sources (weapons, wands) grow with upgrades or level, so a fixed 2-8 bolt falls behind as the run goes on (not measured yet: needs real Cleric run data). The spell's lasting value is then the guaranteed hit (Illuminated). Whether that is a problem depends on how the owner wants the spell to feel: a utility spell (accuracy setup) or a damage spell.
2. Tome level grows by use (exp from charges spent) and raises charge cap/start charges, so progression exists, but only as MORE casts, never as stronger casts for Guiding Light.
3. Several spells that scale (Judgement, Holy Lance, Smite) are unlocked through talents/subclass, so the early game relies on Guiding Light alone.

## Proposal options (each needs a decision; none is implemented)
A. Leave as is, document Guiding Light as a hit-guarantee utility spell (zero risk, changes nothing).
B. Make Guiding Light scale with the Tome's level or hero level (direction of the owner's complaint). Needs: pick the variable (Tome level vs hero level), measure Cleric win rate / damage share per depth with UIMSD from real runs first (Cleric runs cannot exist until the un-ban ships), then pick numbers from the data rather than guessing.
C. Keep 2-8 but make the Illuminated bonus (the Priest-only level + 5 bonus) available to every Cleric. Smaller change, uses an existing scaling term.
D. Defer all balance until enough Cleric runs exist in UIMSD (the plan's "evidence, not conclusions" principle).

## Recommended order
1. Ship the un-ban with the ruleset decision (handoff), 2. collect Cleric runs, 3. use UIMSD (Floors / Damage / Attacks datasets already record per-spell damage facts) to measure Guiding Light's share and Cleric clear rates, 4. choose between B and C with numbers, 5. ship as a new balance revision so old runs keep their identity.
Spells the owner said they like are not touched by any option.
