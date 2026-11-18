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
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.*;

/** Uses pure level-indexed rules, never floor-feeling selectors, damage rolls or gas generation. */
public final class TrinketExplanation {
    private TrinketExplanation(){}
    private static String n(float value){return ObfuscationExplanation.number(value);}
    private static String percent(float chance){return n(100*Math.min(1,Math.max(0,chance)))+"%";}
    public static String details(Trinket trinket){
        if(!trinket.isIdentified())return "Identify this trinket to see its current effect.";
        int level=trinket.buffedLvl();String text;
        if(trinket instanceof EyeOfNewt)text="Normal sight range is multiplied by _"+n(EyeOfNewt.visionRangeMultiplier(level))+"_. You can sense creatures within _"+EyeOfNewt.mindVisionRange(level)+" tiles_, including through walls. Formula: sight factor = 0.875 − 0.125 × level; sensing range = 2 + level. Stronger sensing effects can extend the range. Stealthy mimics evade this sensing, and it does not reveal secret doors or let projectiles pass through walls.";
        else if(trinket instanceof FerretTuft)text="You, your allies and your enemies have _"+percent(FerretTuft.evasionMultiplier(level)-1)+" more evasion_. Formula: evasion × (1 + 0.125 × (level + 1)), currently ×"+n(FerretTuft.evasionMultiplier(level))+". This makes attacks more likely to miss; the actual dodge chance also depends on the attacker's accuracy.";
        else if(trinket instanceof SaltCube)text="The same food lasts _"+n(1/SaltCube.hungerGainMultiplier(level))+"×_ as long. Hunger grows at _"+percent(SaltCube.hungerGainMultiplier(level))+"_ of its usual rate. Formula: hunger rate × 1 / (1 + 0.25 × (level + 1)). Natural healing runs at _"+percent(SaltCube.healthRegenMultiplier(level))+"_ of its usual rate; this penalty pauses on locked floors. Potions and other direct healing keep their normal effects.";
        else if(trinket instanceof VialOfBlood)text="Healing potions, waterskins and wells restore _"+percent(VialOfBlood.totalHealMultiplier(level)-1)+" more total HP_, spread over time. Formula: healing × (1 + 0.125 × (level + 1)), currently ×"+n(VialOfBlood.totalHealMultiplier(level))+". "+(Dungeon.hero==null?"The per-turn limit depends on your maximum health.":"At your current maximum health, the base limit is _"+VialOfBlood.maxHealPerTurn(level)+" HP per turn_ before your run's healing adjustments.")+" Remaining healing and missing HP can lower each heal. "+(Dungeon.hero!=null&&com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.disabled()?"Your current rules prevent health restoration.":"");
        else if(trinket instanceof ExoticCrystals)text="Normally converts _"+percent(ExoticCrystals.consumableExoticChance(level))+"_ of eligible potion and scroll drops into their exotic versions: 20% × (level + 1). Strength potions, upgrade scrolls and items needed for hazard rooms are excluded. Healing potions instead use your selected Healing-to-Shielding chance: _"+percent(ExoticCrystals.consumableExoticChance(com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing.class))+"_. At 0%, Healing never converts, even with these crystals. Existing items do not change.";
        else if(trinket instanceof RatSkull)text="Rare enemy variants become _"+n(RatSkull.exoticChanceMultiplier(level))+"×_ as likely where available. For crystal mimics and armored statues, only the increase above the ordinary chance is halved; their effective factor at this level is "+n((RatSkull.exoticChanceMultiplier(level)+1)/2f)+". Formula: ordinary factor = 2 + level; smaller factor = 1 + (ordinary factor − 1) / 2. Each variant keeps its own starting chance. Existing monsters do not change.";
        else if(trinket instanceof MimicTooth)text="Normal mimic chance is multiplied by _"+n(MimicTooth.mimicChanceMultiplier(level))+"_. Formula: 1.5 + 0.5 × level. Ordinary floors also have a _"+percent(MimicTooth.ebonyMimicChance(level))+"_ ebony-mimic chance: 12.5% × (level + 1). Mimics become harder to spot and can evade ordinary sensing. These chances apply when the floor is generated; existing chests do not change.";
        else if(trinket instanceof ParchmentScrap)text="Random weapon enchantments and armor glyphs use _"+n(ParchmentScrap.enchantChanceMultiplier(level))+"×_ their usual chance. Random weapon and armor curses use _"+n(ParchmentScrap.curseChanceMultiplier(level))+"×_. Each loot source keeps its own starting chance, with curses checked first. This changes newly generated equipment, not the strength of an existing enchantment or glyph.";
        else if(trinket instanceof PetrifiedSeed)text="When grass produces a seed, it has a _"+percent(PetrifiedSeed.stoneInsteadOfSeedChance(level))+"_ chance to produce a runestone instead. The seed-or-stone drop chance is also multiplied by _"+n(PetrifiedSeed.grassLootMultiplier(level))+"_ (1 + level / 12). These are separate rolls: most grass still drops neither. Dew drops are unchanged.";
        else if(trinket instanceof ThirteenLeafClover){float chance=Math.max(0,Math.min(1,ThirteenLeafClover.alterHeroDamageChance(level)));text="Each affected damage roll has a _"+percent(chance)+"_ chance to use the special roll. Within that roll, 60% choose maximum damage and 40% choose minimum damage. Overall: _"+percent(.6f*chance)+" maximum_ and _"+percent(.4f*chance)+" minimum_ from this effect; other rolls are normal. Formula: special-roll chance = 25% × (level + 1), capped at 100%. Enemy defenses still apply afterward.";}
        else if(trinket instanceof DimensionalSundial)text="From 8am to 8pm on your phone, enemies respawn at _"+n(DimensionalSundial.enemySpawnMultiplierDaytime(level))+"×_ their usual rate; at night, _"+n(DimensionalSundial.enemySpawnMultiplierNighttime(level))+"×_. Day rate = 0.95 − 0.05 × level; night rate = 1.25 + 0.25 × level. The time between spawn attempts is divided by that rate. Floor population limits, available spawn locations and your run’s spawn settings still apply.";
        else if(trinket instanceof CrackedSpyglass){float extra=CrackedSpyglass.extraLootChance(level);int whole=(int)extra;text="On each new non-boss floor, normally adds _"+whole+" hidden "+(whole==1?"item":"items")+"_, with a _"+percent(extra-whole)+"_ chance of one more. Formula: 0.375 × (level + 1); the whole part gives guaranteed drops and the fraction gives the chance of another. Search uncovers these items. Your run’s equipment-drop settings can change equipment rewards; existing floors are unchanged.";}
        else if(trinket instanceof MossyClump)text="An eligible new floor that would otherwise have no special theme has a _"+percent(MossyClump.overrideNormalLevelChance(level))+"_ chance to become grassy or watery. Formula: 25% × (level + 1). Excludes Floor 1, bosses and side areas. Already generated floors do not change.";
        else if(trinket instanceof TrapMechanism)text="An eligible new, otherwise unthemed floor has a _"+percent(TrapMechanism.overrideNormalLevelChance(level))+"_ chance to become trap-filled or broken by chasms: 25% × (level + 1). Separately, about _"+percent(TrapMechanism.revealHiddenTrapChance(level))+"_ of normally hidden traps start visible: 10% × (level + 1). These reveals are spread across the floor rather than rolled separately for each trap. Extra traps on trap-filled floors start visible. Existing floors and hidden-door searching are unchanged.";
        else if(trinket instanceof ChaoticCenser)text="During combat, releases a random gas about every _"+ChaoticCenser.averageTurnsUntilGas(level)+" turns_. Average interval = 300 / (level + 1), rounded down. Release waits for an active enemy target and a suitable visible tile 2–6 tiles from you, so it can take longer. A warning names the gas before it appears. Some clouds are helpful; others can harm you. Upgrades shorten the interval and favor exotic gases.";
        else if(trinket instanceof WondrousResin)text="A cursed wand has a _"+percent(WondrousResin.positiveCurseEffectChance(level))+"_ chance to restrict its random effect to neutral or helpful results: 25% × (level + 1). An uncursed wand has a separate _"+percent(WondrousResin.extraCurseEffectChance(level))+"_ chance to add one such effect to its normal zap: 12.5% × (level + 1). Effects still depend on the target and terrain; this does not remove a wand’s curse.";
        else if(trinket instanceof ShardOfOblivion)text="Gear no longer identifies itself through use. Use the shard to identify gear once it is ready; an Identify scroll makes it ready immediately. Each unidentified equipped item adds a _20% monster-loot bonus_, counting up to _"+(level+1)+" contributions_. Using an unidentified wand and hitting with an unidentified thrown weapon each count once for _50 turns_. "+(Dungeon.hero==null?"":"Your current Shard bonus: _"+n(100*(ShardOfOblivion.lootChanceMultiplier(level)-1))+"%_. ")+"These bonuses add to other loot bonuses and multiply the enemy’s usual drop chance; they do not guarantee a drop from every enemy.";
        else return "";
        return "_Trinket details_\nTrinket level: "+level+". "+text;
    }
}
