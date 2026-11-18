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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.*;

/** One identified ring's contribution, using the same pure functions as combat. */
public final class RingExplanation {
    private RingExplanation(){}
    private static String n(float value){return ObfuscationExplanation.number(value);}
    public static String details(Ring ring){
        if(!ring.isIdentified())return "Identify this ring before viewing its exact effect or upgrade-dependent calculations.";
        if(ring instanceof RingOfArcana)return ((RingOfArcana)ring).statsInfo();
        int b=ring.soloBuffedBonus(),plain=ring.soloBonus();String effect;
        if(ring instanceof RingOfAccuracy)effect="Attack accuracy score × 1.3^bonus = × "+n(RingOfAccuracy.accuracyMultiplier(b))+". This scales the attack score, not a fixed hit probability. The enemy's evasion still matters.";
        else if(ring instanceof RingOfEvasion)effect="Evasion score × 1.125^bonus = × "+n(RingOfEvasion.evasionMultiplier(b))+". This scales the defense score, not a fixed dodge probability. The enemy's accuracy still matters.";
        else if(ring instanceof RingOfElements)effect="Supported elemental damage or duration × 0.825^bonus = × "+n(RingOfElements.elementalMultiplier(b))+". A factor below one reduces the supported amount; this is not protection from every kind of damage or every debuff.";
        else if(ring instanceof RingOfEnergy)effect="Wand/artifact/heroic-armor recharge rate × 1.175^bonus = × "+n(RingOfEnergy.ringChargeMultiplier(b))+". For an otherwise unchanged recharge, time is divided by this factor. This does not add that many charges or lower a charge cost.";
        else if(ring instanceof RingOfFuror)effect="Attack rate × 1.09051^bonus = × "+n(RingOfFuror.attackSpeedMultiplier(b))+". Attack time is divided by this factor; damage per hit and movement speed are separate.";
        else if(ring instanceof RingOfHaste)effect="Movement rate × 1.15^bonus = × "+n(RingOfHaste.speedMultiplier(b))+". Ordinary movement time is divided by this factor; attacks are separate.";
        else if(ring instanceof RingOfMight)effect="Adds "+plain+" strength points. Maximum-HP factor = 1.035^bonus = × "+n(RingOfMight.HTMultiplier(b))+" before the maximum-HP calculation's integer rounding. Strength uses the ring's ordinary bonus; temporary effective-ring bonuses can affect HP separately. This does not heal the same amount as its percentage.";
        else if(ring instanceof RingOfSharpshooting)effect="Adds "+b+" effective upgrade levels to thrown-weapon damage calculations. Durability factor = 1.2^ordinary bonus = × "+n(RingOfSharpshooting.durabilityMultiplier(plain))+". This changes durability consumption, not damage by the same percentage; each thrown weapon's own rules and rounding still apply.";
        else if(ring instanceof RingOfTenacity){
            effect=b>0?"Damage reduction grows as you lose HP. ":b<0?"Extra damage taken grows as you lose HP. ":"This ring currently has no effect on damage. ";
            if(Dungeon.hero!=null&&Dungeon.hero.HT>0){
                float missing=(Dungeon.hero.HT-Dungeon.hero.HP)/(float)Dungeon.hero.HT;
                float factor=RingOfTenacity.damageMultiplier(b,missing);
                effect+="At your current health: _"+n(Math.abs(100*(1-factor)))+"% "+(factor>1?"more":"less")+" damage_ (× "+n(factor)+"). ";
            } else effect+="Inspect during a run for the result at your current health. ";
            effect+="Full health gives no reduction or increase. ";
            if(b!=0)effect+="The listed maximum is approached only near zero HP; it is not your "+(b>0?"reduction":"increase")+" on every hit. ";
            effect+="\n\nDamage multiplier = 0.85^(ring value × fraction of HP missing). Other defenses and rounding still apply.";
        }
        else if(ring instanceof RingOfWealth)effect="Qualifying ordinary loot chance × 1.20^bonus = × "+n((float)Math.pow(1.20,b))+". This multiplies an eligible source's chance; it does not add that percentage to every drop. Special bonus-loot counters and their guarantees follow separate rules, and some sources ignore this ring.";
        else if(ring instanceof RingOfForce){
            Integer strength=ArmorExplanation.knownStrength(Dungeon.hero);
            if(strength==null)effect="Identify equipped rings to see your unarmed damage. ";
            else effect="At _"+strength+" Strength_, this ring alone gives _"+RingOfForce.unarmedMinimum(strength,b)+"–"+RingOfForce.unarmedMaximum(strength,b)+" damage points_ unarmed. ";
            effect+=(b<0?"With a melee weapon, it reduces damage by _"+(-b)+"_. ":"With a melee weapon, it adds _"+b+" damage_. ")+"These amounts are before other damage bonuses and defenses.\n\n";
            effect+="For the formulas below, Strength gives T = max(1, (Strength − 8) / 2). Above T = 5, keep only half the excess. Use D = T for a positive ring value, or D = 1 otherwise. Unarmed minimum = max(0, round(D + ring value)); maximum = max(0, round((D + 1) × (5 + ring value))).\n\n";
            effect+="Duelist’s Brawler’s Stance adds round(3 + T + ring value × (4 + 2T) / 8) damage";
            if(strength!=null){float tier=RingOfForce.unarmedTier(strength);effect+=" — _"+Math.round(3+tier+b*(4+2*tier)/8f)+" extra damage_ at your known Strength";}
            effect+=". The weapon’s augmentation and enchantment can still apply. Normal weapon-ability recharging is halved for at least 50 turns after activating the stance, even if you turn it off early.";
        }
        else return "";
        String state= Dungeon.hero!=null&&Dungeon.hero.buff(MagicImmune.class)!=null?"Magic immunity currently suppresses ring effects. These are identified equipment contributions, not active benefits.\n\n":"";
        return state+"_This ring's identified contribution_\n"+effect+"\n\nRing value used in these formulas: "+b+". The calculations show this ring by itself. For two rings of the same type, add their ring values before applying the formula. An uncursed +0 ring has a value of 1; each upgrade adds 1. Curses and temporary effects can change that value. Displayed results are rounded.";
    }
}
