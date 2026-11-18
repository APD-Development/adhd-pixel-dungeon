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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WandEmpower;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;

/** Known wand contributions, without casting, selecting a random level or consuming a use. */
public final class WandExplanation {
    private WandExplanation() {}
    private static String corruptionGroup(boolean major, boolean showWeights) {
        java.util.Map<Class<? extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff>, Float> pool=WandOfCorruption.explanationEffectPool(major);
        java.util.ArrayList<Class<? extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff>> types=new java.util.ArrayList<>(pool.keySet());
        types.sort(java.util.Comparator.comparing(Class::getName));StringBuilder text=new StringBuilder();
        for(Class<?> type:types){float weight=pool.get(type);if(showWeights&&weight<=0)continue;if(text.length()>0)text.append(", ");text.append(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(type,"name"));if(showWeights)text.append(" (").append(ObfuscationExplanation.number(weight)).append(")");}
        return text.toString();
    }
    public static String details(Wand wand) {
        if (!wand.isIdentified()) return "Identify the wand before its upgrade-dependent calculations are shown.";
        int[] levels = wand.previewEffectiveLevels();
        String result = "_Next cast_\nWand level for this cast: "
            + levels[0] + (levels[0] == levels[1] ? "" : "–" + levels[1])
            + ". Charges: " + wand.curCharges + "/" + wand.maxCharges
            + ". Temporary bonuses can strengthen this cast without upgrading the wand. "
            + "Upgrading adds one permanent level, uses up one resin bonus if present, and may remove a curse.";
        if (levels[0] != levels[1]) result += "\nWild Magic gives two equally likely level bonuses. Its cap and any stronger Magic Charge are already included in this range.";
        if(Dungeon.hero!=null&&Dungeon.hero.subClass==com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.WARLOCK){
            result+="\n\n_Warlock Soul Mark_\nOn a qualifying effect check against a character other than the hero, mark chance = clamp(1 − (0.92 raised to (effective wand level × effect charge count + 1) − 0.07), 0, 1). This is a per-character effect check, not a guarantee that a cast finds a valid target. The effect's charge count can differ from Wild Magic's fractional resource payment.";
            if(wand instanceof WandOfWarding)result+=" A ward checks using its own stored wand level when it attacks; current casting level is not treated as every existing ward's level.";
            else{int cost=wand.previewChargeCost();if(cost>=0){float low=(float)Math.max(0,Math.min(1,1-Wand.soulMarkThreshold(levels[0],cost))),high=(float)Math.max(0,Math.min(1,1-Wand.soulMarkThreshold(levels[1],cost)));result+=" With the current standard effect charge count "+cost+": "+ObfuscationExplanation.number(100*low)+(levels[0]==levels[1]?"":"–"+ObfuscationExplanation.number(100*high))+"% per qualifying check; offered duration "+(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SoulMark.DURATION+levels[0])+(levels[0]==levels[1]?"":"–"+(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SoulMark.DURATION+levels[1]))+" turns, before target resistances.";}}
        }
        if (wand instanceof DamageWand) {
            DamageWand direct = (DamageWand)wand;
            int min = direct.min(levels[0]), max = direct.max(levels[1]);
            result += "\n\n_Base direct-damage roll_\nCurrent roll bounds: " + min + "–" + max
                + " damage points before direct-roll bonuses, the wand's situational effects and the target's defenses. "
                + "This is not a guaranteed amount of health lost. The game's damage-roll rules determine which result is selected.";
            // These built-in wands have affine level bounds; do not infer formulas for custom subclasses.
            if (wand.getClass().getPackage().equals(DamageWand.class.getPackage())
                    && (wand instanceof WandOfBlastWave || wand instanceof WandOfFireblast
                    || wand instanceof WandOfLightning || wand instanceof WandOfMagicMissile
                    || wand instanceof WandOfPrismaticLight || wand instanceof WandOfDisintegration
                    || wand instanceof WandOfTransfusion || wand instanceof WandOfLivingEarth
                    || wand instanceof WandOfFrost)) {
                result += "\nFor the current charge configuration, minimum = " + direct.min(0) + " + "
                    + (direct.min(1)-direct.min(0)) + " × effective level; maximum = " + direct.max(0)
                    + " + " + (direct.max(1)-direct.max(0)) + " × effective level. "
                    + "Multi-charge Fireblast, penetrating beams, water, target type and other situational rules can change the actual effect; those are not a universal damage multiplier.";
            }
            WandEmpower empower = Dungeon.hero == null ? null : Dungeon.hero.buff(WandEmpower.class);
            if (empower != null) result += "\nCurrent Wand Empower adds " + empower.dmgBoost
                + " damage points to a qualifying direct-damage roll, with " + empower.left
                + " such rolls remaining. With that bonus, these roll bounds are " + (min+empower.dmgBoost)
                + "–" + (max+empower.dmgBoost) + " damage points before later adjustments. Multiple rolls or targets can consume more than one use.";
        } else if (wand instanceof WandOfCorrosion) {
            result += "\n\n_Corrosive gas_\nStarting unmodified gas damage uses 2 + effective wand level: "
                + (2+levels[0]) + (levels[0]==levels[1]?"":" to "+(2+levels[1]))
                + " damage points. This is the starting tick strength, not total damage from the entire cloud. Remaining in the gas and later Corrosion ticks have their own timing and increasing-strength rules; immunity and protection can change actual HP loss.";
        }
        if (wand instanceof WandOfCorruption) {
            result += "\n\n_Corruption strength, not damage_\nCorrupting strength = 3 + effective wand level / 3. Current strength: "
                + ObfuscationExplanation.number(WandOfCorruption.corruptionPower(levels[0]))
                + (levels[0]==levels[1]?"":" to "+ObfuscationExplanation.number(WandOfCorruption.corruptionPower(levels[1])))
                + ". This is compared with the target's corruption resistance; it is not a damage amount or a universal success percentage. "
                + "Resistance uses a monster-specific base, multiplied by 1 + 4 × (current HP / maximum HP)². Each supported major negative effect halves that resistance; a minor or other qualifying negative effect multiplies it by 0.75. These reductions multiply, rather than adding percentage points. "
                + "Ordinary base resistance is 1 + the monster's experience value, with ascension adjustments. Mimics and statues instead use 1 + floor depth; piranhas and bees use 1 + floor depth / 2; wraiths use (1 + scaling depth / 4) / 5; swarms use their special experience fallback. "
                + "When strength is strictly greater than the adjusted resistance, attempt corruption; immunity can substitute Doom. Otherwise strength / resistance is the chance to attempt a major rather than minor negative effect. Unavailable or immune effects can cause escalation, so this ratio alone is not the final corruption chance. Already corrupted or doomed targets follow their special restriction. The ratio is compared with a random fraction from 0 up to 1: a ratio at or below zero never passes, and a ratio of one or more always passes that check. "
                + "A selected ordinary negative effect is offered 6 + 3 × effective wand level turns before its resistance adjustment."
                + "\nEffects in the half-resistance group: "+corruptionGroup(true,false)+". Effects in the quarter-reduction group: "+corruptionGroup(false,false)+"; other negative effects also use the quarter reduction."
                + "\nPossible major effects and their relative selection weights: "+corruptionGroup(true,true)+". Minor effects and weights: "+corruptionGroup(false,true)+". Existing or immune effects are removed from selection; among remaining effects, weight two is twice as likely as weight one. Some statuses weaken resistance but are not offered by this wand itself.";
        }
        if (wand instanceof WandOfWarding) {
            result += "\n\n_Ward capacity_\nThis wand supports " + (2+wand.previewBaseLevel())
                + " points of wards: 2 + its wand level, including resin and curse infusion. Each ward uses points equal to its tier; other charging Warding wands add their own capacity. "
                + "You need a free point to create or upgrade a ward. At full capacity, zapping a tier 4–6 sentry heals it instead; tier 1–3 wards cannot be zapped. "
                + "Temporary casting levels do not increase capacity, but can strengthen ward damage and healing.";

        }
        if (wand instanceof WandOfRegrowth && Dungeon.hero != null) {
            WandOfRegrowth growth=(WandOfRegrowth)wand;int cost=wand.previewChargeCost();int limit=growth.previewChargeLimit(Dungeon.hero.lvl);
            result += "\n\n_Growth and exhaustion_\nThe next cast uses "+cost+" charges and tries to grow tall grass on "
                + Math.round((3.67f+levels[0]/3f)*cost) + (levels[0]==levels[1]?"":"–"+Math.round((3.67f+levels[1]/3f)*cost))
                + " tiles: round((3.67 + casting level / 3) × charges). Terrain, plants and creatures can leave less room. Creatures on suitable tiles are rooted for up to "+(4*cost)+" turns (4 × charges), before resistance. "
                + "With space available, separate rolls can grow a seedpod or dewcatcher (charges / 6 chance) and a random plant (charges / 3 chance). Exhaustion can prevent either. "
                + "\n\n"+(wand.previewBaseLevel()>=10 ? "At wand level +10 or higher, exhaustion is effectively disabled. " : "The wand becomes exhausted after "+limit+" charges of growth; "+growth.usedGrowthCharges()+" are recorded toward that limit. ")+""
                + (wand.previewBaseLevel()>=10 ? "" : "Limit = round(20 + hero level × (2 + wand level) × (1 + whole-number quotient of wand level / (50 − 5 × wand level))). Resin and curse infusion count toward wand level; temporary casting bonuses do not. ")
                + "Once exhausted, grass can be furrowed instead of tall and give no dew or seeds. Chance per grass tile: min(100%, (charges beyond the limit + 1) × 20%) — currently "+ObfuscationExplanation.number(100*growth.previewFurrowedChance(Dungeon.hero.lvl))+"%.";

        }
        return result;
    }
}
