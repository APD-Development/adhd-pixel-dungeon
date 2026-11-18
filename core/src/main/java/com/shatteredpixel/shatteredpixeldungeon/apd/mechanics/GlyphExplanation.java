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

import com.shatteredpixel.shatteredpixeldungeon.apd.ArcanaInfoDisplay;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.*;

/** Describes built-in ordinary glyphs without executing procs, querying enemies or rolling damage. */
public final class GlyphExplanation {
    private GlyphExplanation(){}
    private static String n(float x){return ObfuscationExplanation.number(x);}
    private static String pct(float x){return n(100*Math.max(0,Math.min(1,x)))+"%";}
    public static float brimstoneExpectedGain(float power,int currentBarrier){
        float amount=2*(power-1);int cap=Math.round(amount*4);if(cap<=0||amount<=0)return 0;
        double room=Math.max(0L,(long)cap-currentBarrier),whole=Math.min(Integer.MAX_VALUE,(int)amount);float fraction=amount%1;
        return (float)(Math.min(room,whole)*(1-fraction)+Math.min(room,whole+1)*fraction);
    }
    private static String brimstone(float power,Hero hero,boolean detailed){
        String text="Protects against Burning status damage while active, not every fire-themed attack. Extra glyph power can also grant Barrier shielding when something tries to ignite you; this is separate from immunity.";
        if(!Float.isFinite(power))return text+" Identify contributing bonuses to see the shield preview.";
        int current=hero==null||hero.buff(Barrier.class)==null?0:hero.buff(Barrier.class).shielding();int cap=Math.max(0,Math.round(8*(power-1)));
        text+=" Current Barrier gain limit for this source: "+cap+" shield points. With your current Barrier, expected gain per eligible ignition attempt is "+n(brimstoneExpectedGain(power,current))+" shield points; a random fractional gain is not guaranteed each time.";
        if(detailed)text+="\n\nGain offered = floor(2 × (power − 1)), with one extra point if a random roll passes the fractional remainder. Limit = round(8 × (power − 1)). Gain is added only when the limit and rolled gain are positive, and only up to the space below that total Barrier limit. Larger existing Barriers are not reduced. Armor upgrades do not directly enter this extra-shield formula, beyond needing the glyph to remain active. This check is per attempted Burning application, not a universal once-per-turn heal.";
        return text;
    }
    private static String playerNumber(float value){return com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat(value!=0&&Math.abs(value)<.01f?"0.##E0":"0.##",value);}
    private static String arcanaComparison(float before,float after,String unit,Hero hero){
        return ""; //APD: show only the value the player actually has
    }
    private static String playerChance(float base,float power,Hero hero){
        String chance="_"+playerNumber(100*Math.max(0,Math.min(1,base*power)))+"%_";
        float ring=ArcanaInfoDisplay.knownArcanaPower(hero),without=1+ObfuscationExplanation.aura(hero);
        if(Float.isFinite(ring)&&ArcanaInfoDisplay.hasKnownArcana(hero)&&Math.abs(Math.min(1,base*power)-Math.min(1,base*without))>.00001f)
            {} //APD: show only the value the player actually has
        return chance;
    }
    /** Ordinary glyph explanation, replacing repeated flavor plus mathematical summaries. */
    public static String playerDescription(Armor armor,Hero hero){
        if(armor.glyph==null)return "";
        String fallback=armor.glyph.desc();
        if(armor.glyph.curse()){
            if(!armor.cursedKnown)return "";
            if(hero!=null&&(hero.buff(MagicImmune.class)!=null||!armor.hasGlyph(armor.glyph.getClass(),hero)))return fallback+" The curse is currently suppressed.";
            if(armor.glyph.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Bulk.class&&armor.levelKnown){
                if(armor.buffedLvl()<0)return "This curse currently has no effect.";
                float ring=ArcanaInfoDisplay.knownArcanaPower(hero),aura=ObfuscationExplanation.aura(hero);
                if(Float.isFinite(ring)){
                    String text="Move at _"+playerNumber(100*(ring+aura)/3f)+"%_ of normal speed in doorways";
                    return text+". Adds no protection.";
                }
            }
            if(hero!=null&&armor.glyph.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Metabolism.class&&com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.disabled())return "Can consume food when you are hurt, but your current rules prevent the healing.";
            return fallback;
        }
        if(!armor.glyph.getClass().getPackage().getName().equals(Flow.class.getPackage().getName()))return fallback;
        if(armor.glyph instanceof Obfuscation)return ObfuscationExplanation.summary(armor,hero);
        if(hero!=null&&hero.buff(MagicImmune.class)!=null)return fallback+" Magic immunity currently disables this glyph.";
        if(hero!=null&&!armor.hasGlyph(armor.glyph.getClass(),hero))return fallback+" Your current effects replace this glyph.";
        if(!armor.levelKnown)return fallback;
        int raw=armor.buffedLvl(),level=Math.max(0,raw);float ring=ArcanaInfoDisplay.knownArcanaPower(hero),power=ring+ObfuscationExplanation.aura(hero);
        if(!Float.isFinite(power))return fallback;
        if((armor.glyph instanceof Flow||armor.glyph instanceof Swiftness||armor.glyph instanceof Camouflage||armor.glyph instanceof AntiMagic||armor.glyph instanceof Brimstone)&&raw<0)return "This glyph is inactive at the armor's current level.";
        if(armor.glyph instanceof Flow){float base=(2+.5f*raw)*(1+ObfuscationExplanation.aura(hero)),current=(2+.5f*raw)*power;return "Move at _"+playerNumber(current)+"×_ normal speed in water"+arcanaComparison(base,current,"×",hero)+".";}
        if(armor.glyph instanceof Swiftness){float base=(1.2f+.04f*raw)*(1+ObfuscationExplanation.aura(hero)),current=(1.2f+.04f*raw)*power;return "Move at _"+playerNumber(current)+"×_ normal speed when no enemy is within a _3-tile walking path_"+arcanaComparison(base,current,"×",hero)+".";}
        if(armor.glyph instanceof Camouflage){int base=Math.round((3+raw/2f)*(1+ObfuscationExplanation.aura(hero))),current=Math.round((3+raw/2f)*power);return "Trampling tall grass grants _"+current+" turns_ of invisibility"+arcanaComparison(base,current," turns",hero)+". Attacking or using magic can end it early; repeated activations refresh it rather than adding their durations.";}
        if(armor.glyph instanceof AntiMagic){
            int low=Math.round(raw*power),high=Math.round((3+1.5f*raw)*power);float without=1+ObfuscationExplanation.aura(hero);int baseLow=Math.round(raw*without),baseHigh=Math.round((3+1.5f*raw)*without);
            return "Blocks an extra _"+low+"–"+high+" damage_ from certain magical attacks"+".";
        }
        if(armor.glyph instanceof Stone)return "Replaces dodging with damage reduction. Attacks that would be easier to dodge deal less damage, up to _75%_ less.";
        if(armor.glyph instanceof Brimstone){
            String text="Prevents damage from Burning.";float gain=2*(power-1);int cap=Math.round(8*(power-1));
            if(gain>0&&cap>0)text+=" When you would catch fire, it can also grant up to _"+Math.min(cap,(int)Math.ceil(gain))+" shielding_. It stops adding shielding while you already have _"+cap+" or more_.";
            return text;
        }
        if(armor.glyph instanceof Viscosity){
            float fraction=(level+1f)/(level+6f)*power;
            String text=fraction<=1?"Delays "+playerChance((level+1f)/(level+6f),power,hero)+" of attack damage after armor, rounded up.":"Delays all attack damage after armor and reduces the stored amount by about _"+playerNumber(100*(1-1/fraction))+"%_.";
            return text+" The first damage arrives _1 turn_ later. You then take about _10%_ of the remaining stored damage each turn, at least _1 damage_. Delayed damage can still kill you.";
        }
        float base=armor.glyph instanceof Potential?(level+1f)/(level+6f):armor.glyph instanceof Entanglement?.25f:armor.glyph instanceof Repulsion?(level+1f)/(level+5f):armor.glyph instanceof Affection?(level+3f)/(level+20f):armor.glyph instanceof Thorns?(level+2f)/(level+12f):Float.NaN;
        if(!Float.isFinite(base))return fallback;
        String chance=playerChance(base,power,hero);float strength=Math.max(1,base*power);
        if(armor.glyph instanceof Potential)return "Has a "+chance+" chance when you are hit to restore up to _"+playerNumber(strength)+" charge_ to each wand.";
        if(armor.glyph instanceof Entanglement)return "Has a "+chance+" chance when you are hit to surround you with protective roots. They block up to _"+new com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Armor().perHitLimit()+" damage per hit_, absorbing _"+Math.round((5+2*level)*strength)+" damage_ or more before breaking. Moving ends the protection; repeated activations can refresh it.";
        if(armor.glyph instanceof Repulsion)return "Has a "+chance+" chance to push an adjacent attacker back up to _"+Math.round(2*strength)+" tiles_.";
        if(armor.glyph instanceof Affection)return "Has a "+chance+" chance to charm your attacker for _"+Math.round(Charm.DURATION*strength)+" turns_, stopping it from attacking you. Damage can shorten the charm.";
        return "Has a "+chance+" chance to make an attacker bleed. Upgrades strengthen the bleeding; extra power beyond a guaranteed activation strengthens it further.";
    }
    public static String summary(Armor armor,Hero hero){
        if(armor.glyph==null||armor.glyph.curse()||armor.glyph instanceof Obfuscation||!armor.glyph.getClass().getPackage().getName().equals(Flow.class.getPackage().getName()))return "";
        if(hero!=null&&(!armor.hasGlyph(armor.glyph.getClass(),hero)||hero.buff(MagicImmune.class)!=null))return "This glyph is currently suppressed or overridden.";
        if(!armor.levelKnown)return "Identify the armor's upgrades to view its level-dependent glyph values.";
        int raw=armor.buffedLvl(),level=Math.max(0,raw);float power=ArcanaInfoDisplay.knownArcanaPower(hero);
        if(Float.isFinite(power))power+=ObfuscationExplanation.aura(hero);
        if((armor.glyph instanceof Flow||armor.glyph instanceof Swiftness||armor.glyph instanceof Camouflage||armor.glyph instanceof AntiMagic||armor.glyph instanceof Brimstone)&&raw<0)return "This passive glyph is inactive at the current effective armor level.";
        if(armor.glyph instanceof Brimstone)return brimstone(power,hero,false);
        if(armor.glyph instanceof Stone)return "Replaces normal evasion-based dodging with conditional damage reduction. Better would-be evasion helps, but the attacker matters. This glyph's own reduction step cannot remove more than 75% of damage; it is not a fixed 75% reduction.";
        if(!Float.isFinite(power))return "Combined glyph values are withheld while contributing bonuses are unidentified or unavailable.";
        if(armor.glyph instanceof Flow)return "When moving through water, this glyph's current movement rate is "+n((2+.5f*raw)*power)+" times normal before other movement effects. Higher rate means less game time per step, not faster attacks.";
        if(armor.glyph instanceof Swiftness)return "When no enemy is within a 3-tile passable path, this glyph's current movement rate is "+n((1.2f+.04f*raw)*power)+" times normal before other movement effects. This preview does not tell you whether an unseen enemy is nearby.";
        if(armor.glyph instanceof Camouflage)return "Eligible grass activation offers "+Math.round((3+raw/2f)*power)+" turns of invisibility. Repeated activations keep the longer remaining duration rather than adding every duration together. Attacking or using magic can end invisibility early.";
        if(armor.glyph instanceof AntiMagic)return "Adds a "+Math.round(raw*power)+"–"+Math.round((3+1.5f*raw)*power)+" damage-point protection roll against supported magical sources. This is an amount blocked, not a percentage or universal magic immunity.";
        if(armor.glyph instanceof Viscosity){float q=(level+1f)/(level+6f)*power;return q<=1?"Delays "+pct(q)+" of remaining qualifying attack damage, rounded upward to whole damage points. This is delayed harm, not automatic damage cancellation.":"All remaining qualifying attack damage is deferred, and its deferred amount is reduced before later damage ticks. Delayed harm can still kill you.";}
        float chance=armor.glyph instanceof Potential?(level+1f)/(level+6f):armor.glyph instanceof Entanglement?.25f:armor.glyph instanceof Repulsion?(level+1f)/(level+5f):armor.glyph instanceof Affection?(level+3f)/(level+20f):armor.glyph instanceof Thorns?(level+2f)/(level+12f):Float.NaN;
        if(Float.isNaN(chance))return "";
        float multiplied=chance*power;String strength;
        if(armor.glyph instanceof Potential)strength="Offers "+n(Math.max(1,multiplied))+" charge units to each eligible wand, limited by its capacity.";
        else if(armor.glyph instanceof Entanglement)strength="Offers "+Math.round((5+2*level)*Math.max(1,multiplied))+" Earthroot reserve points, separate from its per-hit block limit. Moving ends that protection.";
        else if(armor.glyph instanceof Repulsion)strength="Only adjacent attackers qualify; push distance is up to "+Math.round(2*Math.max(1,multiplied))+" tiles before collisions and target restrictions.";
        else if(armor.glyph instanceof Affection)strength="Offers "+Math.round(Charm.DURATION*Math.max(1,multiplied))+" charm turns before resistance. The attacker cannot directly attack its charmer; damage can shorten the effect.";
        else strength="Only opposing-alignment attackers qualify; offers "+Math.round((4+level)*Math.max(1,multiplied))+" bleeding input points before its random initial reduction and diminishing ticks. This is not fixed total HP loss.";
        return "Current activation chance: "+pct(multiplied)+" on each qualifying armor activation. "+strength;
    }
    public static String details(Armor armor,Hero hero){
        if(armor.glyph==null||armor.glyph.curse())return "";
        if(armor.glyph instanceof Obfuscation)return ObfuscationExplanation.details(armor,hero);
        String name=armor.glyph.getClass().getSimpleName();
        if(!armor.glyph.getClass().getPackage().getName().equals(Flow.class.getPackage().getName()))return "";
        if(hero!=null&&(!armor.hasGlyph(armor.glyph.getClass(),hero)||hero.buff(MagicImmune.class)!=null))return "This glyph is currently suppressed or overridden. No active benefit is claimed.";
        if(!armor.levelKnown)return "Identify the armor's upgrade level before viewing level-dependent glyph calculations. No hidden upgrade or curse is inferred.";
        int raw=armor.buffedLvl(),level=Math.max(0,raw);float power=ArcanaInfoDisplay.knownArcanaPower(hero);
        if(Float.isFinite(power))power+=ObfuscationExplanation.aura(hero);
        boolean passive=armor.glyph instanceof Flow||armor.glyph instanceof Swiftness||armor.glyph instanceof Camouflage||armor.glyph instanceof AntiMagic||armor.glyph instanceof Brimstone;
        if(passive&&raw<0)return "This passive glyph is inactive at the current effective armor level.";
        String amount="Known Arcana and current protective aura together give glyph power "+n(power)+". Arcana multiplies its own ring bonuses; the protective aura adds to that power before the glyph applies it. Other sources with hidden identity or upgrades are withheld.\n\n";
        if(!Float.isFinite(power))amount="Combined power is withheld because contributing equipment is unidentified or unavailable. Base formulas below do not include those hidden bonuses.\n\n";
        String effect;
        if(armor.glyph instanceof Flow)effect="_Flow_\nWhen moving through water, movement-rate contribution = (2 + 0.5 × armor level) × power. Base before power: "+n(2+.5f*raw)+" times normal."+(Float.isFinite(power)?" With current known power: "+n((2+.5f*raw)*power)+" times normal.":"")+" Divide an otherwise unchanged step's time by this factor. Dry ground and attacks do not gain this movement benefit. Each effective level adds 0.5 to the base factor.";
        else if(armor.glyph instanceof Swiftness)effect="_Swiftness_\nWhen no enemy is within a 3-tile passable path, movement-rate contribution = (1.2 + 0.04 × armor level) × power. Base before power: "+n(1.2f+.04f*raw)+" times normal."+(Float.isFinite(power)?" With current known power: "+n((1.2f+.04f*raw)*power)+" times normal.":"")+" Each effective level adds 0.04 to the base factor. This is a conditional potential benefit, not a report that no unseen enemy is nearby. It does not directly speed attacks.";
        else if(armor.glyph instanceof Camouflage)effect="_Camouflage_\nEligible grass activation prolongs invisibility to round((3 + armor level / 2) × power) turns."+(Float.isFinite(power)?" Current duration offered: "+Math.round((3+raw/2f)*power)+" turns.":"")+" It keeps the longer invisibility duration rather than adding every activation's duration together. Each effective level adds half a turn before power and integer rounding. Invisibility can still end early through an action that breaks it.";
        else if(armor.glyph instanceof AntiMagic)effect="_Anti-magic_\nSupported magical damage gains a protection roll from round(armor level × power) to round((3 + 1.5 × armor level) × power) damage points."+(Float.isFinite(power)?" Current range: "+Math.round(raw*power)+"–"+Math.round((3+1.5f*raw)*power)+" points.":"")+" This is damage blocked per applicable hit, not a percentage or universal spell immunity. Armor tier does not enter this glyph's range; level and power do. Rolls are biased toward the middle.";
        else if(armor.glyph instanceof Brimstone)effect="_Brimstone_\n"+brimstone(power,hero,true);
        else if(armor.glyph instanceof Stone)effect="_Stone_\nOrdinary evasion-based dodges are disabled. For an applicable attack, the glyph compares the attacker's accuracy with your would-be evasion, including its supported score modifiers and glyph power on evasion. Let p be its calculated hit chance: p = accuracy / (2 × evasion) when evasion ≥ accuracy, otherwise p = 1 − evasion / (2 × accuracy). Remaining damage factor = clamp((1 + 3 × p) / 4, 0.25, 1); damage is rounded upward after multiplying by this factor. Thus at most 75% is removed by this step, with the actual amount depending on that attacker and your scores. There is no fixed reduction percentage for all enemies. Armor level does not directly enter this glyph's conversion formula.";
        else if(armor.glyph instanceof Viscosity){float q=(level+1f)/(level+6f)*power;effect="_Viscosity_\nDeferred fraction q = (max(0, armor level) + 1) / (max(0, armor level) + 6) × power."+(Float.isFinite(power)?" Current q: "+n(q)+".":"")+" For q ≤ 1, ceil(remaining attack damage × q) is delayed. For q > 1, the remaining attack damage is first reduced to round(damage / q), and all of that is delayed. Delayed damage is not automatically cancelled: after an initial one-turn delay, each tick sends max(1, floor(remaining deferred damage × 0.1)) damage points to the normal damage handler. Existing deferred amounts add together. Later protection and immunity rules can still affect HP loss.";}
        else {
            float chance;String formula,strength;
            if(armor.glyph instanceof Potential){chance=(level+1f)/(level+6f);formula="(level + 1) / (level + 6)";strength="On activation, each eligible wand gains max(1, multiplied chance) charge units, limited by that wand's capacity.";}
            else if(armor.glyph instanceof Entanglement){chance=.25f;formula="1 / 4";strength="On activation, Earthroot reserve offered = round((5 + 2 × level) × max(1, multiplied chance)). This is a damage reserve, separate from Earthroot's depth-based per-hit block limit; moving ends Earthroot protection.";}
            else if(armor.glyph instanceof Repulsion){chance=(level+1f)/(level+5f);formula="(level + 1) / (level + 5)";strength="Only adjacent attackers qualify. Push distance = round(2 × max(1, multiplied chance)) tiles before walls, collisions and target restrictions.";}
            else if(armor.glyph instanceof Affection){chance=(level+3f)/(level+20f);formula="(level + 3) / (level + 20)";strength="On activation, charm duration offered = round("+n(Charm.DURATION)+" × max(1, multiplied chance)) turns before the target's resistance. Charm blocks attacks against the charmer, not against every other target; damage can shorten it.";}
            else if(armor.glyph instanceof Thorns){chance=(level+2f)/(level+12f);formula="(level + 2) / (level + 12)";strength="Only opposing-alignment attackers qualify. Bleeding input = round((4 + level) × max(1, multiplied chance)) damage points before Bleeding's initial random reduction and later diminishing ticks. It is not a guaranteed amount of total HP loss.";}
            else return "";
            effect="_"+name+"_\nUse level = max(0, effective armor level). Base activation chance = "+formula+" = "+pct(chance)+" per qualifying armor activation. Multiply that chance by glyph power."+(Float.isFinite(power)?" Current resulting chance, capped at 100%: "+pct(chance*power)+".":"")+" A guaranteed trigger still requires the stated conditions and a valid target. Power beyond a multiplied chance of 1 strengthens only the effect described here. "+strength;
        }
        return amount+effect;
    }
}
