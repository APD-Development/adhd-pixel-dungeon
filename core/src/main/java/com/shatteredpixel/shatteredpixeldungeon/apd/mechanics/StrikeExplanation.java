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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;

/** Area-effect rules, without counting hidden targets or simulating an attack. */
public final class StrikeExplanation {
    private StrikeExplanation(){}
    private static String n(float x){return ObfuscationExplanation.number(x);}
    private static String chance(float value){return n(Math.min(100,100*value))+"%";}
    /** Ordinary ability text: public talents and a disclosed enchantment only; no target inspection or rolls. */
    public static String playerDescription(Hero hero){
        if(hero==null)return "";
        int reach=hero.pointsInTalent(Talent.ELEMENTAL_REACH);
        float power=1+.3f*hero.pointsInTalent(Talent.STRIKING_FORCE);
        String header="Strike a target within weapon reach with a guaranteed hit, then release a magic cone up to _"+(4+reach)+" tiles_ across _"+(65+10*reach)+" degrees_. Aiming closer shortens the cone; walls block it.";
        KindOfWeapon held=hero.belongings.weapon();
        Weapon.Enchantment enchantment=held instanceof MeleeWeapon?((MeleeWeapon)held).enchantment:null;
        if(enchantment!=null&&enchantment.curse()&&!held.cursedKnown)enchantment=null;
        if(enchantment==null)return header+"\n\nWithout an enchantment, the cone deals _"+Math.round(6*power)+"–"+Math.round(12*power)+" base damage_.";
        String effect;
        switch(enchantment.getClass().getSimpleName()){
            case "Blazing":effect="Spreads fire through the cone.";break;
            case "Chilling":effect="Spreads freezing air through the cone.";break;
            case "Shocking":effect="Spreads electricity through the cone.";break;
            case "Blocking":effect="Grants _"+n(6*power)+" base shielding per enemy_ in the cone, rounding the total.";break;
            case "Vampiric":effect=com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.disabled()?"Your current rules prevent this healing.":"Restores health based on the number of enemies in the cone, up to your missing health.";break;
            case "Kinetic":effect="Deals _"+n(40*power)+"%_ of your stored Kinetic damage to other targets in the cone.";break;
            case "Venomous":effect="Adds _"+n(10*power)+" turns_ of poison to other targets. Fresh poison starts after a _3-turn_ delay.";break;
            case "Blooming":effect="Grows grass and roots targets for _"+Math.round(6*power)+" turns_.";break;
            case "Elastic":effect="Pushes targets away up to _"+Math.round(5*power)+" tiles_. Obstacles can shorten the push.";break;
            case "Eldritch":effect="Frightens targets for _"+n(10*power)+" turns_, including the primary target.";break;
            case "Lucky":effect="Has a _"+chance(.125f*power)+"_ chance to drop loot beneath each enemy. Only one successful reward per enemy.";break;
            case "Projecting":effect="Deals _"+n(30*power)+"%_ of your normal attack damage to other targets in the cone.";break;
            case "Unstable":effect="Applies a random enchantment effect to each other target.";break;
            case "Vorpal":effect="Causes extra bleeding in other targets. The bleeding weakens over time.";break;
            case "Corrupting":effect="Has a _"+chance(.05f*power)+"–"+chance(.25f*power)+"_ chance to turn other targets into allies, rising as their health falls. Some enemies cannot be corrupted.";break;
            case "Crystal":effect="Deals _"+Math.round(10*power)+"–"+Math.round(20*power)+" base magic damage_ to other targets and repairs the weapon for each.";break;
            case "Grim":effect="Has a _"+chance(.06f*power)+"–"+chance(.30f*power)+"_ chance to kill other targets, rising as their health falls. Some targets resist.";break;
            case "Annoying":effect="Has a _"+chance(.2f*power)+"_ chance to drive each target amok for _6 turns_.";break;
            case "Displacing":effect="Has a _"+chance(.5f*power)+"_ chance to teleport each target elsewhere on this floor, if a destination is available.";break;
            case "Dazzling":effect="Has a _"+chance(.5f*power)+"_ chance to blind each target for _6 turns_.";break;
            case "Explosive":effect="Has a _"+chance(.5f*power)+"_ chance to explode at one target in the cone. The blast can hurt you and allies.";break;
            case "Sacrificial":effect="Makes you and targets in the cone bleed.";break;
            case "Wayward":effect="Has a _"+chance(.5f*power)+"_ chance to hex each target for _6 turns_.";break;
            case "Polarized":effect="Has a _"+chance(.5f*power)+"_ chance to deal _24–36 base damage_ to each target.";break;
            case "Friendly":effect="Has a _"+chance(.5f*power)+"_ chance to charm each target toward you for _6 turns_.";break;
            case "Wondrous":effect="Has a _"+chance(.5f*power)+"_ chance to trigger a random cursed-wand effect on each target.";break;
            case "Pressurized":effect="Can flood the cone and push targets away up to _"+Math.round(2*power)+" tiles_.";break;
            default:return header+"\n\n"+com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(enchantment,"elestrike_desc");
        }
        return header+"\n\n"+com.shatteredpixel.shatteredpixeldungeon.messages.Messages.capitalize(enchantment.name())+": "+effect;
    }
    public static String details(Hero hero){
        if(hero==null)return "";
        int reach=hero.pointsInTalent(Talent.ELEMENTAL_REACH),directed=hero.pointsInTalent(Talent.DIRECTED_POWER);
        float power=1+.3f*hero.pointsInTalent(Talent.STRIKING_FORCE);
        String result="\n\n_Elemental Strike_\nCone: _"+(4+reach)+" tiles_, _"+(65+10*reach)+" degrees_. Aiming closer shortens it; walls block it. A valid primary target within weapon reach takes a guaranteed-hit strike, subject to special dodge rules. Costs your normal attack time.\n\n_Talent effects_\nStriking Force strengthens the cone by _"+n(30f*hero.pointsInTalent(Talent.STRIKING_FORCE))+"%_ (×"+n(power)+"). Directed Power adds _"+n(30f*directed)+"% enchantment power per enemy_ in the cone to supported enchantment effects. It does not add that percentage to every cone effect.";
        KindOfWeapon held=hero.belongings.weapon();
        if(held instanceof MeleeWeapon&&!held.cursedKnown)return result+"\nIdentify the weapon's unknown properties before its specific cone effect is shown.";
        Weapon.Enchantment enchantment=held instanceof MeleeWeapon?((MeleeWeapon)held).enchantment:null;
        String name=enchantment==null?"None":enchantment.getClass().getSimpleName();String effect;
        switch(name){
            case "None":effect="Deals _"+Math.round(6*power)+"–"+Math.round(12*power)+" base damage_ to each non-allied target in the cone. Each target gets a separate roll. Formula: round(random 6–12 × "+n(power)+"); damage modifiers and defenses still apply.";break;
            case "Blazing":effect="Spreads fire through the cone. It can spread further and burn creatures and flammable terrain.";break;
            case "Chilling":effect="Spreads freezing air that chills and can freeze creatures. It can also affect items on the ground.";break;
            case "Shocking":effect="Spreads electricity through the cone. It can spread through water, damage creatures and paralyze vulnerable targets.";break;
            case "Kinetic":effect="Deals _"+n(40*power)+"% of the previously stored Kinetic damage_ to other targets, rounded per target. Uses the amount stored before the strike. With no primary target, the conserved-damage effect is removed afterward.";break;
            case "Venomous":effect="Adds _"+n(10*power)+" turns of Poison_ to other targets. Fresh Poison starts after _3 turns_. Poison weakens over time; extra duration is not immediate damage.";break;
            case "Blocking":effect="Before striking, grants _"+n(6*power)+" shield points per enemy_ in the cone, rounding the total. Formula: round(6 × enemy count × "+n(power)+"). A stronger existing Barrier is kept.";break;
            case "Blooming":effect="Grows up to _"+Math.round(8*power)+" tall-grass tiles_ on suitable ground and roots non-allied targets for _"+Math.round(6*power)+" turns_. Other suitable cells become short grass. Repeated use without experience eventually produces furrowed grass.\n\n_Grass limit_\nAfter 40 counted casts, new tall grass becomes furrowed. A cast with no visible enemies and no enemies in the cone counts as 4 casts. One full hero level of experience removes 20 counted casts. Plants and immovable occupants prevent tall-grass placement.";break;
            case "Eldritch":effect="Frightens targets away from you for _"+n(10*power)+" turns_, subject to resistance. Includes the primary target.";break;
            case "Elastic":effect="Pushes targets away up to _"+Math.round(5*power)+" tiles_. Walls, obstacles and target restrictions can shorten the push. A primary target already moved by the strike is not pushed again.";break;
            case "Lucky":effect="Each enemy has a _"+chance(.125f*power)+" chance_ to drop an item from Lucky’s usual loot selection. Each enemy can grant this bonus only once. Formula: chance = 12.5% × "+n(power)+", capped at 100%.";break;
            case "Projecting":effect="Deals _"+n(30*power)+"% of a normal attack damage roll_ to each other target, rounded before defenses. Rolls damage separately for each target.";break;
            case "Unstable":effect="Triggers a random enchantment effect against each other target using a separate weapon damage roll. The chosen enchantment keeps its own activation chance and restrictions.";break;
            case "Vorpal":effect="For fresh bleeding on other targets, the first damage tick is _"+Math.round(5*power)+"–"+Math.round(10*power)+" base damage_, before protection. Each later tick weakens randomly. A stronger existing bleed is kept. Formula: first tick = round(random between 5 × "+n(power)+" and 10 × "+n(power)+").";break;
            case "Corrupting":effect="Attempts to corrupt living monsters other than the primary target. Chance rises from _"+chance(.05f*power)+"_ at full health toward _"+chance(.25f*power)+"_ at very low health. Already-corrupted and immune monsters are excluded.\nFormula: (5% + 20% × missing-health fraction) × "+n(power)+", capped at 100%.";break;
            case "Crystal":effect="Deals _"+Math.round(10*power)+"–"+Math.round(20*power)+" base magic damage_ to each other target and restores up to _"+n(4*power)+"% of full weapon durability_ per target. Damage is rounded after multiplying a 10–20 roll by "+n(power)+"; repairs cannot exceed full durability.";break;
            case "Grim":effect="Attempts to kill other targets. Chance rises from _"+chance(.06f*power)+"_ at full health toward _"+chance(.30f*power)+"_ at very low health. On success, deals damage equal to current HP before other damage rules; protection or immunity can prevent a kill.\nFormula: (6% + 24% × missing-health fraction) × "+n(power)+", capped at 100%.";break;
            case "Vampiric":effect="Restores _"+n(2.5f*power)+" base HP per enemy_ in the cone, rounding the total. Healing rules and missing health limit the amount restored. Formula: round(2.5 × enemy count × "+n(power)+").";break;
            case "Annoying":effect="Each target has a _"+chance(.2f*power)+" chance_ to become amok for _6 turns_.";break;
            case "Displacing":effect="Each target has a _"+chance(.5f*power)+" chance_ to teleport elsewhere on this floor. Requires a legal destination. Successfully teleported hunting monsters return to wandering.";break;
            case "Dazzling":effect="Each target has a _"+chance(.5f*power)+" chance_ to become blind for _6 turns_.";break;
            case "Explosive":effect="Has a _"+chance(.5f*power)+" chance_ to explode at one random target. There is one chance for the whole cone, not one per target. The blast can hurt you and allies.";break;
            case "Friendly":effect="Each target has a _"+chance(.5f*power)+" chance_ to be charmed toward you for _6 turns_.";break;
            case "Polarized":effect="Each target has a _"+chance(.5f*power)+" chance_ to take a separate _24–36 base damage_ roll. Striking Force increases the chance, not that damage range. Defenses still apply.";break;
            case "Pressurized":effect="Each cone tile has a _"+chance(power/2)+" chance_ to become water where terrain permits. Pushes targets up to _"+Math.round(2*power)+" tiles_, limited by obstacles and target restrictions. A primary target already moved by the strike is not pushed again.";break;
            case "Sacrificial":effect="Makes you and targets bleed. Fresh bleeding starts at _"+Math.round(5*power)+"–"+Math.round(10*power)+" base damage_ for you and _"+Math.round(6*power)+"–"+Math.round(12*power)+"_ for targets, before protection. Each later tick weakens randomly; stronger existing bleeding is kept. Your bleeding is applied before the strike.";break;
            case "Wayward":effect="Each target has a _"+chance(.5f*power)+" chance_ to be hexed for _6 turns_.";break;
            case "Wondrous":effect="Each target has a _"+chance(power/2)+" chance_ to receive a random cursed-wand effect. Wondrous Resin can restrict the selection to beneficial effects. The chosen effect keeps its own restrictions.";break;
            default:return result+"\nThis enchantment's additional cone effect has not been calculated here; no unsupported effect or hidden property is invented.";
        }
        return result+"\n\n_Current known cone effect_\n"+effect;
    }
}
