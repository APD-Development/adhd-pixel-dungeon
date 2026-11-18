/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.shatteredpixel.shatteredpixeldungeon.apd.mechanics;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;

/** Read-only rank and public maximum-HP calculations; no effect or attack handlers. */
public final class TalentExplanation {
    private TalentExplanation() {}
    public static String details(Talent talent, int rank) { return details(talent, rank, false); }
    public static String details(Talent talent, int rank, boolean alternateDescription) {
        if (rank <= 0) return "";
        int hp = Dungeon.hero == null ? 0 : Dungeon.hero.HT;
        switch (talent) {
            case BOUNTY_HUNTER:
                return "_Loot from prepared kills_\nAt your rank, preparation levels 1–4 add "+(2*rank)+"%, "+(4*rank)+"%, "+(8*rank)+"%, or "+(16*rank)+"% of the enemy’s normal item-drop chance. This bonus adds alongside Wealth; it does not add those percentage points directly to the final chance. Enemies with no ordinary loot do not gain item drops.\nExtra drop chance = normal drop chance × 2% × 2^(preparation level − 1) × talent rank.";
            case SHIELDING_DEW:
                return "_Shielding Dew_\nDew left over after its healing allocation adds shielding, up to _"+(20*rank)+"% of maximum HP_ in total. "+(Dungeon.hero==null?"":"Current cap: _"+Math.round(Dungeon.hero.HT*.2f*rank)+" shield points_. ")+"Stronger existing shielding is kept. Healing modifiers and Vial of Blood can change how much healing you receive.\nBase dew benefit = round(maximum HP × 5% × drops). Shielding cap = round(maximum HP × 20% × talent rank).";
            case SEARING_LIGHT:
                return "_Searing Light_\nA hit that consumes an enemy’s Illumination adds _"+(1+2*rank)+" damage_ before later damage bonuses and protection (1 + 2 × talent rank). It applies once per Illumination, not on every hit. If another class gains this talent, eligible wand and artifact effects can illuminate non-allied targets, with a 20-turn cooldown. The Priest’s extra Illumination damage is separate.";
            case AGGRESSIVE_BARRIER:
                return "_Aggressive Barrier_\nAt _50% health or less_, gain _"+(1+2*rank)+" shield points_ when the Duelist starts a weapon ability. Keeps stronger existing shielding. For other classes, triggers when starting an ordinary attack, with a _50-turn cooldown_. The action still costs its usual time and charges.\nShield = 1 + 2 × talent rank.";
            case WEAPON_RECHARGING:
                if(alternateDescription || (Dungeon.hero!=null&&Dungeon.hero.heroClass!=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.DUELIST))
                    return "_Damage while recharging_\nWhile wand or artifact Recharging is active, your physical damage roll gains _2.5%_, plus _"+ObfuscationExplanation.number(.025f*rank)+" damage_, then rounds to a whole point. This small flat addition increases with the talent rank; the percentage stays the same. Other damage effects and enemy defenses apply afterward.\nDamage = round(rolled damage × 1.025 + 0.025 × talent rank).";
                return "_Faster weapon recharge_\nWhile wand or artifact Recharging is active, gain _1 extra weapon-ability charge every "+(20-5*rank)+" turns_, gradually alongside normal recharge, up to capacity. Other recharge modifiers and Brawler’s Stance keep their usual rules.\nExtra charge per turn = 1 ÷ (20 − 5 × talent rank) = "+ObfuscationExplanation.number(1/(20f-5*rank))+".";
            case SWIFT_EQUIP:
                return "_Time-free weapon equipment changes_\nEquip a carried weapon instantly. The first use starts a cooldown of _20 turns_; at rank 2, you can make _one more instant change_ before it ends. Ordinary equipment changes still take time after these uses are spent. Cursed weapons still prevent swapping. This talent makes equipping faster, not attacking.";
            case VARIED_CHARGE:
                return "_Varied Charge_\nAfter using one weapon ability, use an ability from a different weapon type to regain _"+ObfuscationExplanation.number(rank/6f)+" weapon-ability charges_, up to capacity. Two copies of the same weapon type do not count. Gaining charge clears the sequence; start another pair to gain more.\nCharge restored = talent rank ÷ 6.";
            case TWIN_UPGRADES:
                return "_Twin Upgrades_\nAn equipped melee weapon can borrow your other equipped melee weapon’s higher effective level if that weapon is at least _"+(3-rank)+" tiers higher_. The required tier gap is "+(3-rank)+". At rank 3, weapons of the same tier can share. This temporarily improves the weaker weapon without upgrading or identifying either item; it never lowers a weapon’s level.";
            case COMBINED_LETHALITY:
                return "_Execution after switching weapons_\nImmediately after a weapon ability, hit with a different melee weapon to execute an enemy left at or below _"+(rank==3?"40":("about "+ObfuscationExplanation.number(100*.4f*rank/3f)))+"% health_. Bosses and minibosses cannot be executed. Two separate weapons of the same type qualify. The next qualifying hit uses this benefit even if the enemy has too much health to execute.\nThreshold = 40% × talent rank ÷ 3. Normal hit, invulnerability and timing rules still apply.";
            case LIGHT_CLOAK:
            case LIGHT_READING:
                boolean cloak=talent==Talent.LIGHT_CLOAK;
                boolean nativeClass= Dungeon.hero==null || Dungeon.hero.heroClass==(cloak?com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.ROGUE:com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.CLERIC);
                if(alternateDescription || !nativeClass)return "_Alternate charging bonus_\nThe "+(cloak?"artifact":"wand")+" charging factor is multiplied by 1 + 0.2 × rank / 3 = "+ObfuscationExplanation.number(1+.2f*rank/3f)+". This multiplies supported recharge progress, not whole charges granted immediately or all time-based effects. Each item keeps its capacity and charging rules.";
                return "_Using the "+(cloak?"Cloak":"Holy Tome")+" from the backpack_\nThe item can use its supported abilities while carried rather than equipped. Unequipped recharge power is multiplied by 0.75 × rank / 3 = "+ObfuscationExplanation.number(.75f*rank/3f)+", or "+ObfuscationExplanation.number(25f*rank)+"% of the corresponding equipped recharge input. This changes recharge progress, not charge costs, ability strength or time. Existing curse, capacity and lost-inventory rules still apply; equipping restores the ordinary equipped path.";
            case SHARED_UPGRADES:
                return "_Shared Upgrades_\nEach counted upgrade adds _1 turn_ to Sniper's Mark and _about "+com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",100f/6f)+"% damage_ to its special bow shot. Counts up to _"+(2*rank)+" upgrades_.\n\nCount = min(thrown weapon's effective level, "+(2*rank)+"). Damage bonus = count ÷ 6; added duration = count turns. The mark keeps the bonus recorded when it was created. The chosen bow shot applies its own damage rules afterward. Identify contributing equipment to see its exact recorded bonus.";
            case BARKSKIN:
                if(Dungeon.hero!=null&&Dungeon.hero.lvl*rank/3==0)return "_Barkskin protection_\nNo extra protection at your current level and talent rank.";
                return "_Barkskin protection_\n"+(Dungeon.hero==null?"The maximum block is your level × talent rank ÷ 3, rounded down. ":"At your current level, this grants _0–"+(Dungeon.hero.lvl*rank/3)+" damage protection_. ")+"The maximum drops by 1 each turn. A hit rolls a fresh amount between zero and the current maximum; existing stronger Barkskin is kept.";
            case CACHED_RATIONS:
                return "_Cached Rations_\nUp to _"+(1+rank)+" supply rations_ across the run, including rations already placed. New chests are attempted only when eligible floors are first generated and a suitable special-room tile is available. Learning this talent does not refill old floors.\n\nRations satisfy hunger and add 1 charge to a carried Cloak of Shadows, up to capacity. Their normal 5 HP healing is adjusted by healing rules and missing health; they restore no HP when healing is disabled.";
            case ROGUES_FORESIGHT:
                return "_Rogue's Foresight_\nA _"+(25*(2+rank))+"% chance_ to hint that a regular main-dungeon floor contains secrets. Requires at least one secret room, or at least two on a secrets-feeling floor.\n\nThe floor's seeded roll is reused when you return; revisiting does not reroll it. A hint does not reveal an entrance. No hint does not prove that no secrets exist. Secret-room generation is unchanged.";
            case NATURES_BOUNTY:
                return "_A limited berry supply from high grass_\nThe lifetime berry allowance is 2 + 2 × rank = "+(2+2*rank)+", minus berries already recorded as dropped. Each eligible high-grass trample rolls for one berry while any allowance remains. Let T be the saved number already dropped: its target floor is T + 2 when T is below 5, otherwise T + 3. The drop chance is 1 in 90 before that floor, 1 in 30 on it and 1 in 10 afterward. These are attempts, not guaranteed drops on exact floors or a higher dew/seed rate. Existing grass, curse and challenge restrictions still apply.";
            case NATURES_AID:
                return "_Barkskin from a visible plant_\nActivating a plant within sight grants _0–2 damage protection_. The maximum drops by 1 every _"+(1+2*rank)+" turns_. Each hit rolls a fresh amount from zero to the current maximum. Existing stronger Barkskin is kept; the plant's own effect is unchanged.";
            case HEIGHTENED_SENSES:
                return "_Nearby character sensing_\nThis talent supplies a sensing range of "+(1+rank)+" tiles, from 1 + rank, when the normal nearby-sensing path applies. Eligible monsters outside ordinary sight reveal their position and immediate surroundings under that path. Neutral stealthy mimics and object-like characters are excluded. Other senses can replace or increase this range under their own rules; their ranges are not simply added. This is not ordinary vision distance, secret-door discovery or permission to shoot through walls.";
            case DURABLE_PROJECTILES:
                return "_Longer thrown-weapon durability_\nThe ordinary expected-use input is multiplied by 1.25 + 0.25 × rank = "+ObfuscationExplanation.number(1.25f+.25f*rank)+". With other conditions unchanged, this divides durability consumed per use by that factor before the weapon's own rounding and limits. Upgrades, the holster, augmentation and Sharpshooting apply their separate rules. It is not an added damage bonus, a fixed number of extra throws for every weapon or a promise that each throw has an independent break chance.";
            case DURABLE_TIPS:
                return "_Slower dart-tip wear_\nAfter the dart's ordinary durability calculation, divide tip wear per use by 1 + rank = "+(1+rank)+". That multiplies the corresponding unrounded expected-use input by the same factor; then apply lotus preservation and round the resulting expected uses to a whole number. A result of at least 100 uses has no durability wear; otherwise final wear is 100 divided by the rounded uses, plus the existing 0.001 correction. Charged Shot can add its own uses before that limit. This changes tipped-dart wear, not damage, status duration or the underlying ordinary dart's destruction chance. Different tip types keep their own base durability.";
            case SEER_SHOT:
                return "_Temporary sight at an empty impact cell_\nFor Huntress, an eligible bow shot into an unoccupied cell reveals its area for "+(5*rank)+" game-time units, from 5 × rank, then starts a 20-unit cooldown. For another class, the supported thrown-weapon impact uses this rule instead. It is an impact-area reveal, not a permanent map of the floor or guaranteed secret-door discovery; targeting, terrain and the reveal's own area rules still apply. Hitting a character does not qualify as an empty-cell shot.";
            case ENHANCED_LETHALITY:
            case ASSASSINS_REACH:
                if(Dungeon.hero==null)return "";
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation ready=Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation.class);
                StringBuilder prepDetails=new StringBuilder("_Preparation thresholds and reach_\nAt the hero's current trained ranks, the four preparation tiers use these ordinary execution thresholds and blink ranges: ");
                for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation.AttackLevel tier:com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation.AttackLevel.values())prepDetails.append("tier ").append(tier.ordinal()+1).append(": below ").append(ObfuscationExplanation.number(100*tier.KOThreshold())).append("% HP, ").append(tier.blinkDistance()).append(" tiles; ");
                prepDetails.append("Bosses and minibosses use one fifth of each health threshold. Equality does not meet the strictly-below condition. These are remaining-health thresholds, not kill probabilities; no enemy's HP is inspected here. Blink still requires a valid visible target, legal path and suitable landing cell. ");
                if(ready!=null)prepDetails.append(PreparedShotExplanation.preparation(ready));
                return prepDetails.toString();
            case INSCRIBED_STEALTH:
                return "_Invisibility offered by using a scroll_\nAn ordinary full-strength scroll use offers "+(1+2*rank)+" game-time units of Invisibility, from 1 + 2 × rank. Partial-strength effects multiply that offer by their existing scroll-use factor. The offered duration is added to existing Invisibility time; it is not an independent damage bonus. Attacks and other invisibility-breaking actions still break it normally. Sensing, adjacent detection and special enemy rules remain separate; invisibility is not guaranteed immunity to all attacks.";
            case WIDE_SEARCH:
                return "_Larger normal Search area_\nWithout separate Foresight, normal Search uses a distance of 3 tiles for Rogue or 2 tiles for another class with this talent, compared with 2 and 1 without it. Rank 1 uses the game's rounded footprint; rank 2 uses the full square footprint at the same distance, including its corners. Terrain, visibility and existing search eligibility rules still apply. This changes the area checked, not the per-tile discovery probability, normal action cost or proof that a failed check contains no secret. Foresight has its own larger rounded area. The optional Floor 5 Rat King feature remains restricted by its own setting and conditions.";
            case REJUVENATING_STEPS:
                return "_Grass created by stepping_\nEntering ordinary grass or embers while ready creates high grass under the existing regeneration rules and starts a "+(15-5*rank)+"-unit cooldown, from 15 − 5 × rank. Each high-grass creation adds "+(3-rank)+" to the furrowing counter. Once that counter is at least 200, or natural regeneration is unavailable, the created terrain is furrowed grass instead. This is not a guaranteed seed or dew drop, an unlimited loot generator or a change to movement time; grass and challenges retain their normal drop rules.";
            case WAND_PRESERVATION:
                if(alternateDescription || (Dungeon.hero!=null&&Dungeon.hero.heroClass!=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.MAGE))
                    return "_Extra resin from a wand_\nConverting a wand to resin adds "+rank+" resin units to the ordinary yield of 2 × (wand level excluding resin bonuses + 1). This adds units, not a percentage or extra upgrades per resin. The wand is still consumed by conversion and normal eligibility restrictions remain. Hidden wand levels are not disclosed here.";
                return "_Preserving the staff's previous wand_\nWhen imbuing an already imbued staff, the previous wand is returned at +0 if the preservation allowance is unused. It goes into the backpack or drops at the hero if collection fails. Preservation uses its allowance rather than duplicating the incoming wand; existing staff level-transfer rules remain. At rank 2, processing a hero level-up clears the used allowance so it can be used again. Rank 1 has no such level-up refresh. Resin bonuses on the newly imbued wand are removed under the normal imbuing rules.";
            case MYSTICAL_CHARGE:
                return "_Artifact recharge from staff hits_\nA qualifying staff hit offers "+ObfuscationExplanation.number(rank/2f)+" artifact recharge power, from rank / 2. Supported equipped artifacts convert that power using their own charging rates, curse restrictions and capacities. It is not a guarantee of that many whole charges for every artifact, extra wand charges or a free attack. Artifact-specific details show how each item uses recharge power.";
            case EXCESS_CHARGE:
                return "_Shielding from a fully charged imbued wand_\nWhen the hero-owned imbued wand is used while its charges are at least its maximum, it offers round(effective wand level × 0.67 × rank) Barrier points before deducting the cast's charges. At this rank the level factor is "+ObfuscationExplanation.number(.67f*rank)+" shield points per effective level before rounding. This is the imbued wand's effective level, not the hero level or a percentage of health. Stronger existing Barrier is retained rather than adding the whole offer. Other effective-level modifiers follow the normal wand rules; no hidden level is revealed here and the spell still consumes its usual time and charges.";
            case SOUL_SIPHON:
                return "_Soul-mark benefits from another attacker's hit_\nAgainst a soul-marked enemy, qualifying physical damage from someone other than the hero scales the restoration input by 0.4 × rank / 3 = "+ObfuscationExplanation.number(100*.4f*rank/3f)+"%, rounded to whole points. Before that scaling, the input is limited to the smaller of the damage and the victim's current HP plus shielding. A positive result offers healing of ceil(input × 0.4), limited by missing HP and the current healing rules. Soul Eater can separately convert that input to hunger relief. This is not damage amplification, a percentage chance to heal or a claim that all damage sources trigger soul-mark restoration.";
            case SOUL_EATER:
                return "_Soul-mark hunger relief and meal benefits_\nA positive soul-mark restoration input offers input × rank / 3 = "+ObfuscationExplanation.number(rank/3f)+" hunger units per input point. The input is limited by the victim's HP and shielding and, for another attacker's physical hit, Soul Siphon's reduced restoration rule. Hunger relief is not HP healing or a consumed food item. A soul-marked enemy's eligible loot/death processing also has a "+(10*rank)+"% chance (rank out of 10) to trigger the hero's food-eaten talent benefits with zero food energy. That roll does not create food, guarantee a meal effect on every kill or waive any existing effect-specific restrictions.";
            case NECROMANCERS_MINIONS:
                return "_An allied wraith from a soul-marked death_\nA dying soul-marked monster that is not itself a wraith has a 0.4 × rank / 3 = "+ObfuscationExplanation.number(100*.4f*rank/3f)+"% chance to attempt a wraith spawn at its position. A successfully spawned wraith is corrupted to fight for the hero. This is one chance per eligible death, not a guaranteed spawn, permanent ally cap increase or a chance on every attack. Occupancy and the existing spawn rules can prevent a successful summon even when the roll qualifies.";
            case IRON_WILL:
                return "_Seal shielding capacity_\nFor the Warrior, add "+rank+" shield points to the seal's capacity: 3 + 2 × armor tier + rank. Armor upgrades do not increase this capacity. This is shielding capacity, not immediate healing, armor absorption or a damage percentage. The seal keeps its normal activation and recharge rules. For another class, the alternate shield capacity is the talent rank itself.";
            case RUNIC_TRANSFERENCE:
                if(alternateDescription || (Dungeon.hero!=null&&Dungeon.hero.heroClass!=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.WARRIOR))
                    return "_Keeping an armor glyph during upgrades_\nThe ordinary glyph-loss check starts when upgrading from +"+(5+rank)+" instead of +4. Earlier ordinary glyph checks are suppressed, not assigned an extra independent protection chance. Once eligible, the existing chance is min(100%, 10 × 2^(old armor level − 4)%); the level is measured before the upgrade. Curse glyphs and explicitly inscribing upgrades retain their separate rules. This explanation does not disclose your armor's hidden glyph or level.";
                return "_Moving a glyph with the seal_\nRank 1 supports common and uncommon glyphs; rank 2 also supports rare glyphs. The seal must actually carry a glyph for transfer to be available. This moves the existing eligible glyph rather than generating an extra glyph, changing its strength or identifying hidden armor properties.";
            case LETHAL_MOMENTUM:
                return "_A chance for a time-free attack_\nOn an enemy-aligned monster's qualifying death attributed to the hero, a weapon or a weapon enchantment, the trigger chance is 0.34 + 0.33 × rank = "+ObfuscationExplanation.number(100*(.34f+.33f*rank))+"%. Success readies a benefit consumed when a supported attack or throw checks its time cost. This can be the killing action itself. That action spends no ordinary time through this benefit; it still uses ammunition or other normal resources. It does not make every action after any kind of kill free, grant extra damage or stack multiple saved attacks. Inspection never consumes a pending benefit.";
            case IMPROVISED_PROJECTILES:
                return "_Blinding with a thrown non-missile item_\nThrowing an item that is not a thrown weapon at a differently aligned character offers Blindness for "+(1+rank)+" game-time units, from 1 + rank, when this talent is ready. A successful qualifying offer starts a 50-unit cooldown. This is not extra physical damage, a bonus to thrown weapons or immunity to blindness resistance. The throw still spends its normal item and action time unless a separate effect changes that time.";
            case BACKUP_BARRIER:
                return "_Shielding before spending the last available cast_\nA qualifying wand offers "+(1+2*rank)+" Barrier points, from 1 + 2 × rank, before its zap when its charges exactly equal the cast's charge cost and its charger belongs to the hero. For Mage, this applies to the staff's imbued wand rather than a separate carried wand. For another class, the wand must be tied for highest actual wand level in the belongings. This description does not reveal which unidentified wand qualifies. Barrier keeps the stronger amount rather than adding the whole offer to an existing shield. Casting keeps its ordinary charge and time costs.";
            case ARCANE_VISION:
                return "_Tracking a wand-affected character_\nA qualifying wand effect offers awareness of the affected character for "+(5+5*rank)+" game-time units, from 5 + 5 × rank. Awareness tracks that character under the existing sensing rules; it does not reveal every room, identify equipment, discover secret doors or guarantee that a projectile can travel through intervening walls. Individual wand effects retain their own targeting and resistance rules.";
            case ALLY_WARP:
                return "_Trading places with an ally_\nA movable ally can be interacted with from a floor-distance of up to "+(2*rank)+" tiles, from 2 × rank. This is a distance limit, not movement speed or a chance. The warp still requires a connected passable-or-avoidable route, compatible landing space and the existing hazard and immovable-character restrictions. It exchanges positions rather than granting arbitrary teleportation through sealed terrain. Ordinary adjacent interaction remains available under its normal rules.";
            case ENDLESS_RAGE:
                float cap=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk.maximumRage(rank);
                return "_Rage above its ordinary limit_\nMaximum stored rage: "+ObfuscationExplanation.number(100*cap)+"%. Formula: 100 × (1 + 0.1667 × rank). Rage damage still stops at 1.5 times its normal input at 100% rage; the extra storage does not raise that damage cap. When activating berserk above 100%, shield amount is multiplied by the stored rage fraction, and the recovery counters are multiplied by (2 − that fraction), with their normal rounding. This is more shielding and shorter recovery, not permanent maximum HP or a universal damage reduction. Inspecting does not activate berserk or scale its recovery.";
            case ENRAGED_CATALYST:
                return "_Rage strengthens weapon enchantment power_\nAt 100% rage the added contribution is "+ObfuscationExplanation.number(.15f*rank)+" to the enchantment-power factor. At lower rage: min(1, rage fraction) ×0.15×rank. The contribution is added after Arcana's factor, rather than multiplying Arcana's entire result or adding that many percentage points to every trigger probability. Rage above 100% adds no further Catalyst power. Each enchantment or curse uses the resulting power under its own chance, strength and cap rules; there is no universal final trigger percentage. This talent does not add physical weapon damage by that amount.";
            case CLEAVE:
                return "_Combo time after a qualifying kill_\nA combo hit on a dead enemy, or a fully healed corrupted enemy, sets the combo timer to "+ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo.killComboTime(rank))+" game-time units. Formula:15 +15×rank. This replaces the timer rather than adding that many turns to whatever remains. An ordinary hit offers a five-unit minimum under its own rule; it does not always restore this longer kill window. Hold Fast can separately slow supported combo decay. No free attack or extra damage is granted by the longer timer.";
            case LETHAL_DEFENSE:
                int cycle=com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield.cooldownDuration();
                return "_Seal recharge time from combo kills_\nA qualifying combo-finisher takedown (a kill or an enemy converted to your alignment) subtracts "+Math.round(cycle*(rank/3f))+" turns from the broken seal's recharge counter. Formula:round("+cycle+"×rank/3). The counter cannot go below −"+cycle+" turns. This is recharge credit, not immediate shielding or health restored. Activating the shield adds its usual "+cycle+"-turn cycle and floors the new counter at zero, so stored negative credit can make the next shield ready sooner. The finisher retains its normal attack cost; eligible splash kills can also earn credit.";
            case ENERGIZING_MEAL:
                return "_Wand recharge after eating_\nEating grants _"+(2+3*rank)+" turns of wand Recharging_, keeping a longer existing duration. Each eligible wand gains _1 extra charge every 4 turns_, up to capacity; partial turns give proportional charge. Some of this duration can pass during the eating action.\nRecharge duration = 2 + 3 × talent rank.";
            case MYSTICAL_MEAL:
                return "_Artifact recharge after eating_\nEating grants _"+(1+2*rank)+" turns of Artifact Recharging_. Each eligible uncursed artifact recharges at its own rate, up to capacity. Equal or shorter offers keep the current effect, including whether it can recharge the Horn. A new effect granted by eating from the Horn cannot recharge that Horn.\nRecharge duration = 1 + 2 × talent rank.";
            case INVIGORATING_MEAL:
                return "_Haste after eating_\nEating offers _"+ObfuscationExplanation.number(.67f+rank)+" turns of Haste_, keeping a longer existing effect. Haste triples movement speed. Some of this time can pass during eating and other actions, so it does not promise a fixed number of steps. Other action speeds are unchanged.\nHaste duration offered = 0.67 + talent rank.";
            case SATIATED_SPELLS:
                int mealShieldPoints=1+2*rank;
                if(alternateDescription || (Dungeon.hero!=null&&Dungeon.hero.heroClass!=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.CLERIC))
                    return "_Shielding after eating_\nEating sets your Barrier to _"+mealShieldPoints+" shield points_ if it has that many or fewer, then delays decay for at least _10 turns_. A larger Barrier is left unchanged, including its decay timing. Keeps a longer existing delay; the shielding amounts do not add together.\nShield = 1 + 2 × talent rank.";
                return "_Shielding after a spell_\nEating readies _"+mealShieldPoints+" shield points_ for your next qualifying spell cast, keeping stronger existing shielding. The benefit lasts until used; eating several meals does not stack it. A linked powered ally can receive the same shield. Clearing an existing light wall leaves the benefit ready.\nShield = 1 + 2 × talent rank.";
            case FARSIGHT:
                float visionFactor=1+.25f*rank;
                return "_Normal vision range_\nAt this rank, multiply the normal hero vision-distance input by 1+0.25×rank = "+ObfuscationExplanation.number(visionFactor)+". This changes a range in tiles, not a probability to notice a secret or a guarantee to see through walls. Later vision modifiers apply before rounding the normal casting radius to the nearest tile; normal shadow casting caps that radius at "+com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster.MAX_DISTANCE+" tiles. Terrain, visibility and floor boundaries still limit what is visible."
                    +(Dungeon.hero==null?"":" Current base vision-distance input: "+Dungeon.hero.viewDistance+" tiles; after this talent alone: "+ObfuscationExplanation.number(Dungeon.hero.viewDistance*visionFactor)+" tiles before other modifiers, rounding and the cap.")
                    +" Blindness or shadowmeld can disable this normal distant-sight path. Mind Vision, magical sensing and mapping keep separate rules; their ranges are not simply added to this one. Inspection does not reveal terrain or discover a hidden door.";
            case ENHANCED_COMBO:
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo combo=Dungeon.hero==null?null:Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo.class);
                return "_Conditional combo improvements_\nAt rank 1 or higher with at least 7 recorded combo hits, a living Clobber target is offered 3 tiles of knockback instead of 2 and 3 game-time units of Vertigo. That enhanced branch does not use the ordinary non-flying-target pit-avoidance restriction. Obstacles and displacement rules still determine actual movement. The pre-hit count controls this check.\nAt rank 2 or higher and at least 9 combo, a qualifying parry leaves the ready Parry effect present for its remaining normal window, rather than consuming it after the first parry. It is not permanent invulnerability or protection against every damage source.\nAt rank 3, when a selected Slam, Crush or Fury target is outside ordinary attack reach, the leap check permits target grid distance ≤1+floor(combo count/3). It needs a visible valid target, a clear projectile path and a legal landing tile just before that target. Rooting blocks the leap; ordinary in-reach finisher rules remain separate. The leap is part of the existing finisher action, whose normal turn costs and combo-use rules are unchanged. Combo requirements and Clobber/Parry already-used restrictions still apply.\n"
                    +(combo==null?"There is no active recorded combo for a personal count preview.":"Current recorded combo: "+combo.getComboCount()+"; maximum target-distance limit for the rank 3 leap check would be "+(1+combo.getComboCount()/3)+" tiles before path and move-eligibility checks.");
            case HEROIC_ENERGY:
                float costFactor=com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility.heroicEnergyFactor(rank);
                return "_Armor-ability charge cost_\nAt this rank, the common armor-ability cost step multiplies its base charge-point cost by "+ObfuscationExplanation.number(costFactor)+", keeping "+ObfuscationExplanation.number(100*costFactor)+"% of that cost. This reduces charge spent per use; it does not multiply recharge speed, increase charge capacity or shorten the action's turn cost. Individual abilities can apply additional conditional cost rules, so their own current calculation panel gives the final applicable cost. The factor is applied before those ability-specific rules rather than subtracting that many charge points.";
            case DEATHLESS_FURY:
                return "_Emergency berserk and level-progress recovery_\nAt 0 HP, the normal automatic check can start berserk only while rage is at least 100% and its state is ready, with this talent present. Base recovery afterward is "+ObfuscationExplanation.number(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk.baseLevelRecovery(rank))+" hero-level equivalents of experience progress, from 4−rank. Partial level progress reduces that amount; these are not dungeon floors or turns. Rage above 100% can separately multiply recovery by(2−rage fraction), and the usual shield formula determines the protection offered. This grants temporary shielding rather than restoring HP. The Berserker can still die if protection ends at 0 HP. Inspecting does not run the automatic activation check, grant a shield or reset recovery.";
            case EVASIVE_ARMOR:
                String evasion="_Added evasion while freerunning_\nThe talent adds rank × max(0,strength−armor requirement) evasion score points in the ordinary worn-armor path while freerunning. At this rank that is "+rank+" points per excess strength point. This is added after the armor's missing-strength penalty; augmentation can add its own points afterward. The separate basic freerun bonus is floor(hero level/2). A score bonus is not a fixed dodge percentage; the attacker's accuracy and other effects still matter. Without worn armor, this armor-path contribution is not applied. Merely gaining invisible movement speed at Speedy Stealth rank 3 does not activate freerun evasion.";
                if(Dungeon.hero!=null){
                    com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor worn=Dungeon.hero.belongings.armor();Integer strength=ArmorExplanation.knownStrength(Dungeon.hero);
                    if(worn==null)evasion+=" No armor is currently worn, so this armor-path contribution is zero.";
                    else if(worn.levelKnown&&strength!=null)evasion+=" With your known strength and armor at this described talent rank, the extra Evasive Armor contribution would be "+(rank*Math.max(0,strength-worn.STRReq()))+" score points while freerunning.";
                    else evasion+=" A personal armor-strength number is withheld while contributing upgrades or bonuses are unknown or unavailable.";
                }
                return evasion;
            case UNENCUMBERED_SPIRIT:
                return "_Equipment-tier energy gain_\nMultiply the base kill-energy reward by 1 plus separate worn-armor and melee-weapon bonuses. At rank 3, tier 1 or lower contributes 1; otherwise at rank 2 or higher, tier 2 or lower contributes 0.75; otherwise at rank 1 or higher, tier 3 or lower contributes 0.5. Higher tiers or absent items contribute zero. Armor and weapon contributions add before multiplying the reward, rather than multiplying each other. Active Brawler's Stance excludes the weapon contribution. Item tier is not its hidden upgrade level; upgrades do not turn a high-tier item into a qualifying low-tier item. This increases energy gained, not capacity or damage. The ordinary cap and temporary unarmed-ability overflow rules still apply. Training rank 3 also offers the existing identified cloth armor and gloves; inspection grants neither item.";
            case MONASTIC_VIGOR:
                float ratio=1.2f-.2f*rank;
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy monk=Dungeon.hero==null?null:Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy.class);
                return "_Energy ratio and empowered abilities_\nThe nominal threshold is "+ObfuscationExplanation.number(100*ratio)+"% of capacity. The actual comparison is current energy / capacity ≥ (1.2 −0.2×rank), evaluated with the original floating-point values. Current stored comparison ratio: "+Float.toString(ratio)+". At rank 2 this is slightly greater than 0.8, so exactly 80% does not satisfy the original comparison; the rounded percentage label alone is insufficient. "
                    +(monk==null?"No active Monk energy state is being created for this preview.":"Current energy quotient: "+Float.toString(monk.energy/monk.energyCap())+"; empowered now: "+(monk.abilitiesEmpowered(Dungeon.hero)?"yes":"no")+".")
                    +" Empowerment changes each ability under its own rule; it does not spend a second energy charge for the bonus. Flurry enables enchantment use, Focus removes setup time, Dash adds 4 tiles of range, Kick increases its main damage factor from 6 to 9 and attempts adjacent-enemy pushes, and Meditate offers its conditional healing and attack-resistance effects. Those effects retain their individual caps, targets and protections.";
            case COMBINED_ENERGY:
                return "_Pairing ability types for one energy refund_\nA qualifying Monk ability must cost at least "+(5-rank)+" energy units, from 5−rank. Pair it with a weapon ability within the offered five-game-time-unit tracking window, in either order. Other effects that alter timers retain their own rules. The first type records its own flag; completing the opposite type restores 1 energy and removes that record. Repeating only the same type refreshes the window but does not itself grant the refund. Lower-cost Monk abilities do not qualify. The refund stops at the current Monk energy capacity; excess is not stored, and it does not refund weapon-ability charges or action time. The two flags persist through normal save/load, with their remaining effect timer. Inspecting does not set either flag, refund energy or extend the window.";
            case LETHAL_HASTE:
                return "_Movement after an ability kill_\nA qualifying weapon-ability kill of an enemy sets the Greater Haste budget to 2 + 2 × rank = "+(2+2*rank)+" units. This replaces the old remaining budget rather than adding that amount. A normal movement step spends one unit and takes zero movement time; scheduled updates also spend one unit. The next update can therefore reduce the offered budget before you move, and attacks retain their normal time costs. This is not a promise of "+(2+2*rank)+" free steps after other actions.";

            case FOCUSED_MEAL:
                if(alternateDescription || (Dungeon.hero!=null&&Dungeon.hero.heroClass!=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.DUELIST))
                    return "_Alternate next-hit damage_\nEating offers bonus damage on your next physical damage roll. "+(Dungeon.hero==null?"":"At your current level: _+"+Math.round(Dungeon.hero.lvl/(4f-rank))+" damage_. ")+"Replaces an existing Physical Empower benefit only when the total remaining bonus would increase; the benefits do not add together.\nBonus = round(hero level ÷ (4 − talent rank)).";
                return "_Weapon charge after eating_\nEating restores _"+ObfuscationExplanation.number((rank+1)/3f)+" weapon-ability "+(rank==2?"charge":"charges")+"_, up to capacity. Fractions accumulate toward a full charge; excess progress is discarded when full.\nCharge restored = (talent rank + 1) ÷ 3.";
            case ENLIGHTENING_MEAL:
                if(alternateDescription || (Dungeon.hero!=null&&Dungeon.hero.heroClass!=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.CLERIC))
                    return "_Recharge after eating_\nEating grants _"+(1+rank)+" turns_ of both wand and artifact Recharging, keeping a longer existing duration for each. Artifacts keep their own rates and capacities. A new Horn-granted recharge effect cannot recharge that Horn; an equal or longer existing artifact effect keeps its current restriction.";
                return "_Tome charge after eating_\nWith a Holy Tome in your belongings, eating restores _"+ObfuscationExplanation.number((rank+1)/3f)+" Tome "+(rank==2?"charge":"charges")+"_, up to capacity. Fractions accumulate toward a full charge; excess progress is discarded when full.\nCharge restored = (talent rank + 1) ÷ 3.";
            case EMPOWERING_MEAL:
                return "_Wand damage after eating_\nEating adds _"+(1+rank)+" damage_ to your next _3 direct-wand damage rolls_. A zap with several damage rolls can use more than one. Eating again sets the bonus and keeps the larger remaining roll count instead of adding three more. Later damage bonuses and defenses still apply.\nBonus damage = 1 + talent rank.";
            case STRENGTHENING_MEAL:
                return "_Physical damage after eating_\nEating offers _+3 damage_ on your next "+(1+rank)+" physical damage rolls. Replaces an existing Physical Empower benefit only when the total remaining bonus would increase; the benefits do not add together. Later damage bonuses and defenses can change the final damage.\nRolls offered = 1 + talent rank.";
            case VETERANS_INTUITION:
            case ADVENTURERS_INTUITION:
            case SCHOLARS_INTUITION:
            case SURVIVALISTS_INTUITION:
            case THIEFS_INTUITION:
                String effect;
                switch(talent){
                    case VETERANS_INTUITION:
                        effect = "Melee-weapon use-identification progress is multiplied by " + ObfuscationExplanation.number(1f+.75f*rank) + "; armor progress by " + (1+rank) + ". At rank two, equipping armor also completes identification, subject to passive-identification restrictions."; break;
                    case ADVENTURERS_INTUITION:
                        effect = "Melee-weapon use-identification progress is multiplied by " + ObfuscationExplanation.number(1f+1.5f*rank) + "; armor progress by " + ObfuscationExplanation.number(1f+.75f*rank) + ". At rank two, equipping a weapon also completes identification, subject to passive-identification restrictions."; break;
                    case SCHOLARS_INTUITION:
                        effect = "Wand use-identification progress is multiplied by " + (1+2*rank) + ". At rank two, a qualifying wand use completes identification, subject to passive-identification restrictions."; break;
                    case SURVIVALISTS_INTUITION:
                        effect = "Thrown-weapon use-identification progress is multiplied by " + (1+2*rank) + ". At rank two, a qualifying thrown-weapon hit readies identification through the normal weapon-use handler."; break;
                    default:
                        effect = "Ring use-identification progress is multiplied by " + (1+rank) + ". Equipping reveals its ring type; at rank two, collecting also reveals the type and equipping completes identification, subject to passive-identification restrictions. Revealing a type alone does not reveal hidden upgrade or curse properties.";
                }
                return "_Identification progress_\n" + effect + " These factors multiply progress earned by eligible uses, not damage, drop chances, or a fixed number of turns saved. Other applicable intuition factors multiply together. Available identification progress and the item's own usage rules can limit progress from an individual action. Shard of Oblivion can mark identification ready instead of revealing the item. No hidden item values are used to produce this explanation.";
            case HOLD_FAST:
                return "_Holding position_\nWaiting readies extra protection until you move. The Warrior must still have the seal's shield status, including its recovery period. The protection roll is _"+rank+"–"+(2*rank)+" damage per hit_, capped at 6 for the Warrior. Other heroes are capped by armor tier plus current upgrades, or zero without armor. Very weak armor can make that cap negative.\n\n_Keeping your benefits_\nCombo, Barrier, Blocking, the seal's shield-expiry delay and Berserk shield loss "+(rank==1?"run at half their normal rate":rank==2?"run at a quarter of their normal rate":rank==3?"are paused":"use their normal rate")+" while you hold position. Their other conditions still apply; this does not pause every status effect.";
            case HEARTY_MEAL:
                return "_Healing from eating_\nEating while current HP divided by maximum HP is strictly below 0.334 offers " + (2+2*rank)
                    + " HP of healing before any custom healing modifier. This is a flat amount, not a percentage of maximum HP. At or above that threshold, this talent offers no healing. Actual health restored is limited by missing HP and the current run's healing rules; excess is not converted to shielding. Eating still spends the food and its normal action time.";
            case IRON_STOMACH:
                return "_Protection during the eating action_\nEating grants protection only while the hero has positive remaining action time from that meal. At this rank, incoming damage at the food-protection step is "
                    + (rank == 2 ? "set to zero" : "divided by four, leaving 25% before rounding")
                    + ". This is conditional protection during that action, not permanent armor or a shield-point reserve. Damage is rounded to whole points afterward, and later damage rules still apply. The effect does not make eating free or stop hunger from advancing.";
            case LIQUID_WILLPOWER:
                float fraction = .030f + .035f * rank;
                return "_Potion shielding_\nAn ordinary potion offers a Barrier equal to "
                    + ObfuscationExplanation.number(100 * fraction) + "% of maximum HP, rounded to the nearest shield point. "
                    + (hp > 0 ? "At your current " + hp + " maximum HP: " + Math.round(hp * fraction) + " shield points. " : "")
                    + "Formula: round(maximum HP × (0.030 + 0.035 × rank) × the consumed item's talent-effect factor). Some potion-derived items have a different factor. This replaces a weaker Barrier; it does not add the whole amount to an existing stronger Barrier. It protects health rather than healing it.";
            case SHIELD_BATTERY:
                return "_Converting wand charges_\nTarget yourself with a charged wand to consume all its charges and offer a Barrier. Each charge contributes "
                    + (rank == 2 ? "6" : "4") + "% of maximum HP; round only the final total to the nearest shield point. "
                    + "Formula: round(maximum HP × 0.04 × charges consumed" + (rank == 2 ? " × 1.5" : "") + "). "
                    + "The second rank multiplies the shield amount by 1.5, rather than adding 1.5 shield points. A stronger existing Barrier remains stronger; the amounts are not added. This action costs one turn and cannot be used while magic is disabled or the wand has no charges.";
            case INSCRIBED_POWER:
                return "_Empowered wand uses_\nAn ordinary scroll offers " + (1 + rank)
                    + " empowered wand uses. Each qualifying use gains +2 effective wand levels, rather than +2 damage points. Damage and other level-dependent effects follow that wand's own rules. "
                    + "Uses offered = truncate((1 + rank) × the consumed item's talent-effect factor). A new application keeps the larger remaining use count instead of adding the counts. It does not permanently upgrade the wand.";
            case SHIELD_OF_LIGHT:
                if(alternateDescription || (Dungeon.hero!=null&&Dungeon.hero.heroClass!=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.CLERIC))
                    return "_Alternate targeted protection_\nWhen a qualifying incoming attack comes from the currently selected enemy, the six-outcome check succeeds on 1 + rank outcomes: current chance " + ObfuscationExplanation.number(100f*(1+rank)/6f)
                        + "% for that matching attack. A success subtracts one damage point at this defense step. This is not a percentage reduction in the whole hit, and attacks from other enemies do not gain this passive check. It is the non-Cleric benefit rather than granting the Cleric's targeted spell.";
                return "_Targeted spell protection_\nThe spell offers a middle-favoring block roll from " + (1+rank) + " to " + (2*(1+rank))
                    + " damage points against its selected enemy. This is armor blocking at that step rather than Barrier points or a reduction against every attacker. The spell costs one Tome charge and takes no game time to cast; its four scheduled units normally appear as five on the counter because of the following status update.";
            case BLESS:
                if(alternateDescription || (Dungeon.hero!=null&&Dungeon.hero.heroClass!=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.CLERIC))
                    return "_Alternate allied accuracy and evasion_\nAllied characters, including the hero, multiply the normal accuracy and evasion rolls by 1.01 + 0.02 × rank = " + ObfuscationExplanation.number(1.01f+.02f*rank)
                        + ". These are factors in the opposed hit contest, not added percentage points to hit or dodge chance. Each side's bonuses and special rules still matter. This passive does not require or grant the Cleric's Bless spell, and it is distinct from the temporary Bless status's 1.25 factor.";
                return "_Bless spell amounts_\nOn the hero, offer " + (2+4*rank) + " scheduled turns of Bless and " + (5+5*rank)
                    + " Barrier points. On another eligible creature, offer " + (5+5*rank) + " turns of Bless and up to " + (5+5*rank)
                    + " HP healing, with unused healing offered as Barrier points. A stronger Barrier is kept instead of simply adding the full offer. The temporary Bless status multiplies accuracy and evasion contest scores by 1.25; it does not add 25 percentage points to either chance.";
            case STRONGMAN:
                float strengthFraction = .03f + .05f * rank;
                return "_Added strength points_\nAdds floor(base strength × " + ObfuscationExplanation.number(strengthFraction)
                    + ") strength points. The percentage uses base strength, excluding Ring of Might, Adrenaline Surge and this talent's own bonus. Those separate added bonuses are not multiplied again. "
                    + (Dungeon.hero == null ? "" : "Current base strength: " + Dungeon.hero.STR + " points; this rank adds " + (int)Math.floor(Dungeon.hero.STR * strengthFraction) + " strength points. ")
                    + "The result is rounded down to whole strength points and can stay unchanged until enough base strength is gained. Strength requirements and strength-based damage effects use their own rules; this is not a direct damage percentage.";
            case DESPERATE_POWER:
                return "_Last-charge wand level_\nWhen a charged wand is down to exactly one charge, this talent adds " + rank
                    + " effective wand levels to its cast. It does not add that many damage points or permanent upgrades. The wand's level-dependent effects determine the result. A stronger Magic Charge and Wild Magic's own cap/order can affect the resulting effective level; wand details show the current result without spending the charge.";
            case WILD_POWER:
                return "_Wild Magic's temporary level bonus_\nBefore the cap, the two equally likely bonus branches add " + ((4+rank)/2)
                    + " or " + ((5+rank)/2) + " effective wand levels. When both amounts are equal, there is no difference between the branches. "
                    + "If the incoming effective level is below " + (3+rank) + ", add the selected bonus but stop at " + (3+rank)
                    + ". A wand already at or above that level is not lowered by this cap. A stronger Magic Charge is considered afterward. These are temporary casting levels, not permanent upgrades or added damage points.";
            case POINT_BLANK:
                return "_Thrown-attack accuracy by range_\nAt adjacent range, multiply the bow or thrown weapon's accuracy contribution by "
                    + ObfuscationExplanation.number(.5f+.25f*rank) + "; without the talent it is 0.5. At non-adjacent range, the factor remains 1.5. "
                    + "Formula at adjacent range: 0.5 + 0.25 × rank. These are accuracy-score factors, not fixed hit percentages. Target evasion, other accuracy effects and special hit rules still matter.";
            case PROJECTILE_MOMENTUM:
                return "_Thrown weapons while freerunning_\n_"+(50*rank)+"% more accuracy_ and _"+(15*rank)+"% more damage_. The damage bonus includes extra damage from strength and is rounded to the nearest point. Stored momentum alone does not activate these bonuses. Accuracy improves the attack's chance against enemy evasion; it is not a guaranteed hit percentage.";
            case SPEEDY_STEALTH:
                return "_Invisible momentum and movement_\nWhile invisible, with freerunning ready and not already active, each normal one-turn momentum update adds two stacks, capped at ten. Cooldown blocks this stack gain. "
                    + (rank >= 2 ? "At this rank, invisibility also pauses the active freerunning countdown. " : "This rank does not pause the freerunning countdown. ")
                    + (rank == 3 ? "Invisible movement uses a speed factor of 2 even outside freerunning, so otherwise unchanged steps take half their usual game time. It is the same factor while freerunning, not 2 × 2. " : "This rank does not separately double movement speed outside freerunning. ")
                    + "This movement benefit does not multiply attack speed or damage.";
            case EMPOWERED_STRIKE:
                return "_Ready staff strike_\nThe ready staff strike multiplies damage at its staff-effect step by "
                    + ObfuscationExplanation.number(1f+rank/6f) + " and rounds to a whole damage point. Formula: round(damage entering this step × (1 + rank / 6)). "
                    + "Its supported staff on-hit effect uses a separate power factor of " + ObfuscationExplanation.number(1f+rank/2f)
                    + ". That second factor strengthens the particular wand's staff effect; it does not multiply all weapon damage again. Chance, duration or amount can use it differently for different wands. The ready effect is consumed by the qualifying staff strike, with delayed resolution where required.";
            case SHARED_ENCHANTMENT:
                return "_Borrowing the bow's enchantment_\nOn each eligible borrowing check, chance = talent rank / 3. Current chance "
                    + ObfuscationExplanation.number(100f*rank/3f) + "%. This offers the bow's enchantment an opportunity to run; the enchantment's own activation roll and target restrictions still apply. "
                    + "At rank three, borrowing is guaranteed, but the enchantment's effect is not necessarily guaranteed. Magic immunity prevents the borrowed on-hit effect. The thrown weapon's own enchantment can also apply, and Projecting has a separate range check.";
            case PROVOKED_ANGER:
                return "_Added attack damage_\nA ready benefit adds " + (1 + 2 * rank)
                    + " damage points to the next qualifying attack at the talent damage stage, then is consumed. Formula: 1 + 2 × rank. It is an added amount, not a percentage. The ready benefit lasts five turns after damage breaks a shielding buff; later defenses, shielding and attack effects can still change final HP loss.";
            case LINGERING_MAGIC:
            case PATIENT_STRIKE:
            case SUCKER_PUNCH:
                return "_Added attack damage_\nA qualifying attack adds a uniformly rolled " + rank
                    + "–2 damage points at the talent damage stage. At rank two this is always two points, rather than another roll on top of rank one's benefit. "
                    + (talent == Talent.LINGERING_MAGIC ? "A wand or staff zap readies this benefit for five turns; the next qualifying physical attack consumes it. "
                        : talent == Talent.PATIENT_STRIKE ? "Waiting readies this benefit while you hold that position; moving ends it, and the next qualifying non-thrown attack consumes it. "
                        : "This requires a surprise attack against a monster which has not already received this talent's bonus. The bonus is recorded per monster, rather than repeating on every surprise hit. ")
                    + "These are added damage points, not a hit chance or permanent weapon upgrade. Later attack effects, shielding and defenses can change the final HP loss.";
            case FOLLOWUP_STRIKE:
                return "_Follow-up hit_\nA bow or thrown-weapon hit marks that enemy for _5 turns_. Your next melee hit against it adds _"+(1+rank)+" damage_ and removes the mark. A bow or thrown-weapon hit against another enemy switches the target.";
            case DEADLY_FOLLOWUP:
                return "_Marked-enemy damage multiplier_\nA thrown-weapon hit, excluding the Spirit Bow's arrow, marks that enemy for five turns. Qualifying non-thrown attacks against the same marked enemy multiply damage at this talent's stage by "
                    + ObfuscationExplanation.number(1f + .1f * rank)
                    + " and round to the nearest damage point. Formula: round(damage entering this step × (1 + 0.1 × rank)). "
                    + "This step follows the flat attack-talent bonuses, so it also scales those applicable bonuses. It is not added damage points or a guaranteed percentage increase in final HP loss. The mark lasts until its timer expires or a new thrown target replaces it; one melee hit does not consume it.";
            case LIQUID_AGILITY:
                return "_Potion accuracy and evasion_\nAt this rank, the temporary evasion benefit "
                    + (rank == 2 ? "guarantees a dodge in the normal accuracy-versus-evasion check" : "multiplies the evasion score by 3; it does not add a flat dodge percentage")
                    + ". It lasts through the current action's remaining time, with extra time if the consumed item's talent-effect factor exceeds one. "
                    + "The next qualifying ordinary non-thrown attack "
                    + (rank == 2 ? "uses guaranteed-hit accuracy" : "multiplies its accuracy score by 3")
                    + ", available for five turns after an ordinary potion. Ordinary potions offer one such use; other factors offer round(factor) uses only when the factor is at least 0.5. "
                    + "Weapon abilities, thrown attacks, a spinning Flail and a charged Crossbow shot do not consume this accuracy benefit. A ready Precise Assault benefit takes priority instead of multiplying these two temporary bonuses. Guaranteed evasion wins over guaranteed-hit accuracy in the normal contest; other special attack rules still apply.";
            case PRECISE_ASSAULT:
                return "_Regular-attack accuracy_\nAfter a qualifying weapon ability, the ready benefit changes the next eligible ordinary non-thrown attack's accuracy to "
                    + (rank == 3 ? "guaranteed-hit accuracy" : (rank == 2 ? "5" : "2") + " times its otherwise calculated score")
                    + ". It is not a flat hit percentage. This takes priority over a ready Liquid Agility accuracy benefit; the two temporary bonuses do not multiply together. "
                    + "For a hero other than the Duelist, the separate persistent bonus multiplies qualifying ordinary non-thrown accuracy by "
                    + ObfuscationExplanation.number(1f + .1f * rank)
                    + ". Weapon abilities, a spinning Flail and a charged Crossbow shot have separate rules. Guaranteed evasion wins over guaranteed-hit accuracy in the normal contest.";
            case LIQUID_NATURE:
                return "_Nearby roots and grass_\nFor an ordinary potion, nearby enemies are offered " + rank
                    + " turns of Roots and up to " + (2 + 2 * rank) + " nearby tiles are selected for tall grass. "
                    + "The affected area is the potion's tile and its eight neighbors. Only suitable terrain without a plant can become tall grass, so the selected count is not guaranteed grass. "
                    + "Roots duration = rank × the consumed item's talent-effect factor; selected grass tiles = truncate((2 + 2 × rank) × that factor), limited by the nine-tile area. Other eligible empty tiles in the area become short grass. No extra movement or free searching is granted.";
            default:
                if(!alternateDescription && Dungeon.hero!=null && Dungeon.hero.armorAbility!=null){
                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility ability=Dungeon.hero.armorAbility;
                    for(Talent associated:ability.talents())if(associated==talent){
                        String current=AbilityExplanation.effects(ability,Dungeon.hero);
                        if(!current.isEmpty())return "_Current armor-ability rules and results_\nThese calculations use your current trained ranks and known equipment. Training the talent refreshes the results under the ability's own formulas and conditions.\n\n"+current;
                    }
                }
                return "";
        }
    }
}
