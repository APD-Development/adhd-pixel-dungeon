# APD statistics: combat and health (Phase 4, telemetry 1.3)

The recorder writes fights, damage, kills, experience, hunger and deaths. It only **reads**: nothing it does changes a character, a
roll or a result, and it never calls the game's random numbers.

## Where it listens (all one-line calls, all read-only)
| Event(s) | Hook | When |
|---|---|---|
| ATTACK (public), ATTACK_DETAIL | `Char.attack` | the moment the roll to hit is known (before the hit sets anything off), and the numbers just before damage is dealt |
| ATTACK (magic) | `Char.hit` (3-argument form) | spells and bolts that roll to hit without going through `Char.attack` |
| DAMAGE, DAMAGE_DETAIL | `Char.damage` | after shielding and HP are applied, before death is processed. Covers every source: creatures, traps, gas, status effects, hunger, wands, bombs |
| KILL, KILL_DETAIL | `Char.die` | any creature other than the hero dying |
| HERO_DEATH, HERO_REVIVED | `Hero.die`, `Hero.resurrect` | HP reached zero; an ankh brought the hero back |
| XP_GAIN | `Hero.earnExp` | experience gained (amount, level, category) |
| HUNGER_CHANGE | `Hunger.affectHunger` | eating and effects, exact amounts |
| LEVEL_UP, MAX_HP_CHANGE, HEAL, SHIELD_GAIN, HUNGER_STATE, HP_CHANGE_INFERRED | looked at between turns | see below |

## What is exact and what is inferred
**Exact:** attacks (hit/miss), damage (requested, absorbed by shielding, taken from HP, before/after), kills, experience, level-ups,
hunger changes by eating/effects, the hero's HP and shield after damage, death and revival.

**Inferred (flag `FLAG_INFERRED` on every such event):** SPD has about 40 separate places that raise HP and no common heal function,
so the recorder compares the hero between turns. A rise in HP is a HEAL (amount exact, source a guess from what the hero just did or
which healing buff is active, overheal unknown = -1, never 0); a rise that came with a higher maximum is a MAX_HP_CHANGE (levelling
up), not a heal; a fall with no damage event is HP_CHANGE_INFERRED; a grown shield effect is SHIELD_GAIN named by the effect.
Exact healing sources arrived in telemetry 1.6 and did not replace these events, they added exact ones (see "Exact healing" below): whatever the
hooks do not report is still noticed here and marked inferred.

Damage the game explained is never reported again as a change: the damage hook resets what "unchanged" means. A load, a new run and
a revival do the same, so none of them invents a heal.

## Public and internal
PUBLIC events say what the player can see: the names the game shows (an unrevealed mimic is named as the chest it looks like), the
damage number, the hero's own HP and shield. A fight is public only if the hero is in it or can see one side; a fight in the dark
has only INTERNAL events (flagged `publicEmitted = false`).

INTERNAL events (`ATTACK_DETAIL`, `DAMAGE_DETAIL`, `KILL_DETAIL`) hold the truth: true classes, creature ids, exact enemy HP and
maximum, armor reduction and rolled damage, the experience a creature is worth. A public damage event has no field for an enemy's
HP (only the hero's, which is always on screen). Player-facing views, player exports and replay packages must leave INTERNAL
events out.

## Things that behave differently from what you might expect
- `hpLost` is the game's own number, so a killing blow can exceed the HP that was left; the before/after HP in the detail event shows the clamp.
- Boss HP is clamped by the boss's own code after `Char.damage` returns (DM300, Tengu, Geomancer, Yog and others), so the detail event's
  "HP after" is as the damage hook saw it. Damage that a creature swallows itself (a Great Crab blocking wand damage, the Dwarf King's
  deferred damage) never reaches `Char.damage` and is not seen.
- Creatures that replace `damage()` entirely (no-op NPCs) record nothing, which matches the game.
- Several damage events can belong to one attack (armor glyphs, enchantments, chain effects); each is its own event.
- Allies and summons fight too: `kind` says hero, ally, enemy or neutral at the moment of the event.

## Rollback and resume
Combat events after a matched save are the abandoned branch like any others (kept, excluded by default). On resume the hero's loaded
state becomes "unchanged": whatever happened while the game was closed is not guessed.

## Cost
About 100-140 bytes per attack including its damage (4-5 events). Measured headless on a desktop: about 15 microseconds more per
attack with recording on (148 vs 163 us in a test that runs the real combat code). The in-game self-test also times event encoding on the device.

## Limitations
- Movement, per-turn state, enemy AI state (asleep, hunting) and individual status effects are not recorded.
- Wand, spell and bomb damage appears as DAMAGE with the item as source, but has no ATTACK event (they do not roll to hit).
- Thrown weapons: the weapon name and apd_uid are on the ATTACK event; if the thrown piece had no id the recorder gives it one.
- Hero death itself needs the game scene and is covered by hook-wiring tests rather than a full headless death.

## Exact healing (Phase 9, telemetry 1.6)
Every place in the game that raises the **hero's** HP now calls `ApdCombat.onHeal(target, hpBefore, source, requested)` right after the heal
(`hpBefore` is captured on the line before, so the recorder reads the amount the game really applied, the maximum HP cap included). The recorder
writes, for a heal of the hero:

| Event | Visibility | Holds |
|---|---|---|
| `HEAL` (279) | PUBLIC | the same payload as the inferred one: amount, HP after, maximum, the visible-context hint. **No `FLAG_INFERRED`** means exact. |
| `HEAL_DETAIL` (299) | INTERNAL | the true source key (`ApdCombat.HEAL_*`, stored in the log, never renamed), what the source asked for before the cap (`requested`, -1 if it has no such number), and `applied` (repeats the HEAL's amount). It is written right after its HEAL and says only what the HEAL does not, which keeps it to about 14 bytes. |

The source is hidden information (an unidentified potion or enchantment would give itself away through "healed by Vampiric"), so it lives only in the
INTERNAL event: `LogRedactor` leaves it out of the copy of an unfinished run, UIMSD masks it until the run is finished. The look between turns moves its
baseline past every reported heal, so nothing is counted twice, and whatever the hooks do not know about is still found and marked inferred. A damage
event no longer swallows an earlier heal of the same turn for the reported sources.

**Sources hooked (key -> where):** `regen` (Regeneration), `potion.healing` (the Healing effect: Potion of Healing and Vial of Blood dewdrop bursts),
`potion.aquaticElixir`, `food.wellFed`, `food.pasty`, `food.supplyRation`, `food.phantomMeat`, `food.frozenCarpaccio`, `plant.sungrass`, `plant.dewdrop`,
`gear.chalice`, `gear.vampiric`, `gear.metabolism`, `gear.tornPage`, `skill.heartyMeal`, `skill.soulSiphon`, `skill.invigoratingVictory`,
`skill.elementalStrike`, `wand.transfusion`, `wand.cursed`, `other.sleep`, `other.fullHeal` (the vault exits), `other.wellOfHealth`. Bless, Lay on Hands and
Hallowed Ground only ever heal allies (the hero gets a shield), and heals of other creatures are not recorded at all. `ExactHealingWiringTest` reads the
game's source and fails if any line that raises an HP is neither one of these hooks nor an explained exemption.

**Facts and UIMSD (Runs dataset, additive):** `Healed` (exact when the run was recorded since 1.6 and nothing was left to infer, `~` when part of it was
guessed, shown as a lower bound when events were lost), `Healed (guessed part)`, and eight INTERNAL columns "Healed by <kind>" (regeneration, potions and
elixirs, food, plants, gear, skills and talents, wands, other), shown only for finished runs, summed across segments. Runs from before 1.6 keep their
inferred healing and show the per-kind columns as unknown (`?`), never zero; a redacted copy keeps the healed amounts but cannot say what healed.

**Cost (measured, `HealingVolumeTest`):** a 20,000-turn run with natural regeneration (one HP every 9 turns, the biggest source by far) writes 2,222 heals:
18 bytes per heal as inferred events (before 1.6) and 32 bytes per heal as an exact HEAL plus HEAL_DETAIL, about 70 KiB of log for the whole run. The
per-event frame overhead is most of what is left, so going lower would mean writing fewer events (for instance one summary event per stretch of
regeneration), which would trade away per-tick history; that is a decision for the owner, not something Phase 9 did.
