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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;

/** Staff on-hit rules; no target scans, proc calls or casting-level rolls. */
public final class StaffExplanation {
    private StaffExplanation() {}
    private static String n(float value){return ObfuscationExplanation.number(value);}
    private static float probability(float value){return Float.isNaN(value)?0:Math.max(0,Math.min(1,value));}
    private static String chance(float low,float high){return n(100*probability(low))+(low==high?"":"–"+n(100*probability(high)))+"% per eligible hit";}
    public static String details(MagesStaff staff,Wand wand,Hero hero){
        if(wand==null||hero==null)return "";
        if(!staff.levelKnown||!wand.levelKnown)return "Identify the staff's upgrades before its level-dependent on-hit calculations are shown.";
        int[] bounds=wand.previewEffectiveLevels();int low=bounds[0],high=bounds[1],staffLevel=staff.buffedLvl();
        float power=Wand.procChanceMultiplier(hero);
        String result="_Battlemage's imbued staff effect_\n"
            +(hero.subClass==HeroSubClass.BATTLEMAGE?"This subclass requirement is currently met. ":"This effect requires the Battlemage subclass and is not currently active for your subclass. ")
            +"On a qualifying staff hit, a Battlemage first adds 0.5 partial charge to the imbued wand if it is below capacity. The wand-specific effect below is separate from the staff's weapon damage and enchantment. "
            +"Current staff effective level: "+staffLevel+"; imbued wand effective level: "+low+(low==high?"":" to "+high)+". "
            +"Current staff-effect power factor: "+n(power)+". It is 1 normally, or 1 + Empowered Strike rank / 2 while that strike is ready. Different effects use this factor for their own chance, duration or amount; it is not a second multiplier on all damage. "
            +"Where a trigger value exceeds 1, its chance is capped at 100%, and the stated extra strength uses max(1, trigger value).";
        if(wand instanceof WandOfBlastWave)result+="\n\n_Blast Wave_\nA paralyzed target without the burst's three-turn lockout loses its Paralysis and receives an extra magic damage roll from 8 + 2 × wand level to 12 + 3 × wand level. Current raw bounds: "+(8+2*low)+"–"+(12+3*high)+" damage points. Multiply the selected roll by the staff-effect power factor and round to a whole point before later damage rules. This is a condition, not a random activation chance. The burst cannot repeat on that target during its three-turn lockout.";
        else if(wand instanceof WandOfCorrosion){int a=Math.max(0,low),b=Math.max(0,high);float q1=(a+1f)/(a+3f)*power,q2=(b+1f)/(b+3f)*power;result+="\n\n_Corrosion_\nTrigger value = (1 + max(0, wand level)) / (3 + max(0, wand level)) × staff-effect power. Current activation: "+chance(q1,q2)+". On success, offer Ooze for its normal duration × max(1, trigger value). Current offered duration: "+n(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze.DURATION*Math.max(1,q1))+(q1==q2?"":"–"+n(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze.DURATION*Math.max(1,q2)))+" turns. Extra strength extends duration rather than increasing Ooze's damage per tick. A longer existing Ooze duration is kept.";}
        else if(wand instanceof WandOfCorruption){int a=Math.max(0,low),b=Math.max(0,high);float q1=(a+1f)/(a+6f)*power,q2=(b+1f)/(b+6f)*power;result+="\n\n_Corruption_\nTrigger value = (1 + max(0, wand level)) / (6 + max(0, wand level)) × staff-effect power. Current activation: "+chance(q1,q2)+". Offer Amok for round((4 + 2 × max(0, wand level)) × max(1, trigger value)) turns before duration resistance. Current offered duration: "+Math.round((4+2*a)*Math.max(1,q1))+(q1==q2?"":"–"+Math.round((4+2*b)*Math.max(1,q2)))+" turns before resistance. Reapplying keeps the longer scheduled remainder rather than adding every duration.";}
        else if(wand instanceof WandOfDisintegration)result+="\n\n_Disintegration_\nAdds round(staff-effect power) = "+Math.round(power)+" tiles to the staff's melee reach contribution. It has no separate direct on-hit damage effect. Other reach and legal-target restrictions still apply; extra reach does not itself grant vision or an attack through a solid wall.";
        else if(wand instanceof WandOfFireblast)result+="\n\n_Fireblast_\nIn the target's three-by-three area, each tile with a burning creature contributes 0.25 to the initial trigger value; otherwise a tile with fire contributes 0.05. A tile never contributes both. Cap that sum at 1, then multiply by the current staff-effect power. The activation chance is capped at 100%; extra strength is max(1, that final value). A burst can consume nearby Burning and fire and damage enemy-aligned creatures for round(extra strength × a roll from 2 + 2 × wand level to 8 + 4 × wand level). These are raw damage points before later protection, not final HP loss. The damage/clearing step requires the floor's fire field; a burning creature alone can satisfy the trigger without guaranteeing area damage on a floor without such a field.";
        else if(wand instanceof WandOfFrost)result+="\n\n_Frost_\nRequires existing Chill. Trigger value = (floor(remaining scheduled Chill turns) − 1) / 9 × staff-effect power. Values at or below zero cannot trigger; values at or above one always pass this roll. On success, offer Frost for round(normal Frost duration × max(1, trigger value)) turns before resistance. The freeze is delayed until after the staff hit so that the hit itself does not immediately break it. Immunity and other special rules still matter.";
        else if(wand instanceof WandOfLightning){result+="\n\n_Lightning_\nTrigger value = (wand level + 1) / (wand level + 4) × staff-effect power. ";if(low==high)result+="Current activation: "+chance((low+1f)/(low+4f)*power,(low+1f)/(low+4f)*power)+". ";else result+="Wild Magic can choose its branch independently at the two level reads, so a single fixed activation percentage is not shown. ";result+="On success, offer Lightning Charge for 10 × max(1, trigger value) turns. Reapplication keeps the longer remainder. While active, this wand's lightning arcs gain one additional tile of propagation reach; Lightning wand zaps skip charged characters. This does not grant immunity to other lightning-looking damage sources.";}
        else if(wand instanceof WandOfLivingEarth)result+="\n\n_Living Earth_\nRock protection gained = round(damage supplied to this staff effect × 0.33 × staff-effect power). With the current power factor, the unrounded share is "+n(33f*power)+"% of the supplied staff-effect damage. These are reserve armor points, not a flat percentage reduction on every future attack. If an Earth Guardian exists, this amount instead heals it up to maximum HP and can raise its level-dependent maximum. Otherwise it is added to Rock Armor, whose reserve cap is twice (8 + 4 × its stored wand level). Rock Armor can block up to half a qualifying incoming hit, rounded upward, spending reserve points. The guardian threshold is 8 + 4 × stored wand level and is checked on a subsequent qualifying zap.";
        else if(wand instanceof WandOfMagicMissile)result+="\n\n_Magic Missile_\nEach other active wand charger receives 0.5 × staff-effect power = "+n(.5f*power)+" partial charge on the qualifying hit, subject to that wand's charge cap. This excludes this imbued wand, which already received the separate ordinary staff charge contribution. It is added charge, not a multiplied passive recharge speed.";
        else if(wand instanceof WandOfPrismaticLight)result+="\n\n_Prismatic Light_\nOffer Cripple for round((1 + staff effective level) × staff-effect power) = "+Math.round((1+staffLevel)*power)+" turns before target duration resistance. This uses the staff level, not an independently boosted wand casting level. Reapplication keeps the longer scheduled remainder instead of adding all durations.";
        else if(wand instanceof WandOfRegrowth)result+="\n\n_Regrowth_\nRequires grass, tall grass or furrowed grass under either combatant. First round(damage supplied to this effect × (max(0, staff level) + 2) / (max(0, staff level) + 6) / 2); then multiply that rounded amount by staff-effect power and round again. The current pre-power share before the first rounding is "+n(100*(Math.max(0,staffLevel)+2f)/(Math.max(0,staffLevel)+6f)/2f)+"% of the supplied damage. Add the resulting unmodified healing-pool HP to the location-bound herbal healing effect. This is gradual healing, not immediate HP restored, and normal healing modifiers and missing health still matter. Moving ends that pool under its own update rules.";
        else if(wand instanceof WandOfTransfusion)result+="\n\n_Transfusion_\nRequires the target to be charmed specifically by the attacker. Offer a Barrier of round(2 × (5 + wand level) × staff-effect power) points, currently "+Math.round(2*(5+low)*power)+(low==high?"":"–"+Math.round(2*(5+high)*power))+" points. A stronger existing Barrier is kept; the offered amount is not added wholesale to it. It also readies one health-cost exemption for a later qualifying ally/charmed-target Transfusion zap. This does not waive normal action time or wand-charge consumption, and hostile-target zaps do not consume that ally-healing exemption.";
        else if(wand instanceof WandOfWarding){int level=Math.max(0,staffLevel);float q=(level+1f)/(level+5f)*power;result+="\n\n_Warding_\nTrigger value = (1 + max(0, staff level)) / (5 + max(0, staff level)) × staff-effect power. Current activation: "+chance(q,q)+". On success, each active ward is offered repair using max(1, trigger value). Tier 2 repairs round(1 × that factor) spent zap; tier 3 repairs a uniformly rolled 1–2 times that factor, rounded. Sentry tiers 4, 5 and 6 instead heal round(9, 12 or 16 × that factor) HP, limited by missing HP. Tier 1 has no repair amount. A higher staff level can also raise the ward’s stored level before repair. This is a shared trigger for the active wards, not independent activation rolls for each one.";}
        return result;
    }
}
