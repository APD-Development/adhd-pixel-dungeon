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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;

/** Species rules and public hero values only; no triggering, destination search or random roll. */
public final class PlantExplanation {
    private PlantExplanation(){}
    private static String n(float value){return ObfuscationExplanation.number(value);}
    public static String details(Class<? extends Plant> species,Hero hero){
        if(species==null||species==Plant.class)return "";
        String text;boolean warden=hero!=null&&hero.subClass==HeroSubClass.WARDEN;
        boolean liveFloor=hero!=null&&hero==Dungeon.hero&&Dungeon.level!=null;
        switch(species.getSimpleName()){
            case "Sungrass":text=warden?"For your Warden, offers a Healing pool equal to maximum HP, with zero fractional rate and one base HP per scheduled update. Healing uses its existing stronger-pool/rate merge, healing modifiers and missing-health limit; this is not an immediate full heal.":"Adds the recipient's maximum HP to a location-bound herbal healing pool. Each one-unit update adds (40 + maximum HP) / 150 toward a request; only progress strictly greater than one spends its whole-number part and retains the fraction. Moving ends the effect, although that detecting update can finish its healing step. Pool points are base HP requests before healing modifiers, not guaranteed actual HP restored.";break;
            case "Earthroot":text=warden?"For your Warden, offers Barkskin strength hero level + 5, currently "+(hero.lvl+5)+" damage-blocking points, with a five-unit decay interval. Barkskin uses its existing conditional replacement rule and random protection roll; the strength is not a flat block on every hit.":"Offers a location-bound armor reserve equal to the recipient's maximum HP. Each supported hit on that tile blocks at most floor((scaling floor + 5) / 2) damage points"+(liveFloor?", currently "+((Dungeon.scalingDepth()+5)/2)+" points":"; a current floor result requires an active run")+". The blocked amount spends the reserve; the final hit can use the full per-hit limit even if less reserve remains. Moving prevents further absorption. Reserve points are not extra HP or an armor upgrade.";break;
            case "Blindweed":text=warden?"For your Warden, offers Invisibility for "+n(Invisibility.DURATION/2)+" scheduled game-time units, with normal invisibility-breaking actions.":"Offers Blindness for "+n(Blindness.DURATION)+" and Cripple for "+n(Cripple.DURATION)+" scheduled units. A hunting monster can return to wandering and be beckoned to a normal destination. Resistance and reapplication still apply; it does not guarantee an enemy forgets every later stimulus.";break;
            case "Firebloom":text="Creates 2 units of attributed fire-cloud volume at the plant. This is not two damage points or a guaranteed two-turn burn; flammability, exposure and Burning rules apply."+(warden?" Your Warden also receives Fire Imbue with a duration input of "+n(FireImbue.DURATION*.3f)+" scheduled units through its existing replacement path.":"");break;
            case "Icecap":text="Applies freezing to eligible non-solid cells in the plant's 3 × 3 footprint. Exposure, thawing, immunity and terrain retain their own rules; this is not a guaranteed freeze on every nearby creature."+(warden?" Your Warden also receives Frost Imbue for "+n(FrostImbue.DURATION*.3f)+" scheduled units.":"");break;
            case "Swiftthistle":text="Resets Time Bubble to a six-unit action-time allowance plus one unit for the triggering action. Supported time spending reduces that allowance instead of advancing ordinary actor time. Attacks and other invisibility-dispelling actions end it; queued plant/trap presses keep their normal deferred handling. This does not mean six real-world seconds, six attacks or guaranteed safety from every action."+(warden?" Your Warden also receives Haste for one scheduled unit.":"");break;
            case "Starflower":text="Adds Bless for "+n(Bless.DURATION)+" scheduled units, using Bless's ordinary accuracy/evasion effects rather than fixed extra damage."+(warden?" Your Warden also gains "+n(Recharging.DURATION)+" scheduled units of wand Recharging through its additive path; charges still have capacity limits.":"");break;
            case "Fadeleaf":text="A hero uses the ordinary legal teleport handling; an eligible non-immovable monster can also be teleported. This does not choose or reveal a destination during inspection."+(warden?" Where inter-floor travel is permitted, your Warden instead returns to main-branch floor max(1, current floor − 1), using the normal transition/arrival rules. Otherwise the existing same-floor teleport path applies.":"");break;
            case "Mageroyal":text="Applies the Healing potion's supported ailment-cure list. It does not identify or remove equipment curses and is not an unlimited cure of every negative mechanic."+(warden?" Your Warden also receives supported environmental-cloud immunity for "+n(BlobImmunity.DURATION/2)+" scheduled units.":"");break;
            case "Sorrowmoss":text="Offers Poison for 5 + round(2 × scaling floor / 3) game-time units"+(liveFloor?", currently "+(5+Math.round(2*Dungeon.scalingDepth()/3f)):"; a current floor result requires an active run")+". That duration also controls the changing Poison tick damage; it is not guaranteed total HP lost. Poison keeps its resistance, attribution and stronger-duration merge."+(warden?" Your Warden also receives Toxic Imbue with a duration input of "+n(ToxicImbue.DURATION*.3f)+" scheduled units through its existing replacement path; its Poison immunity can prevent that negative effect.":"");break;
            case "Stormvine":text=warden?"For your Warden, offers Levitation for "+n(Levitation.DURATION/2)+" scheduled units, using normal flying and ground-hazard rules.":"Offers Vertigo for "+n(Vertigo.DURATION)+" scheduled units. Supported movement can go in an unintended direction; this is not a damage bonus or a fixed chance to miss attacks. Resistance and reapplication apply.";break;
            case "Rotberry":text=warden?"For your Warden, resets Adrenaline Surge to one added strength point, with its ordinary "+n(AdrenalineSurge.DURATION)+"-unit weakening interval. This is not a permanent strength potion.":"Creates 100 units of attributed Toxic Gas volume. Volume is not 100 damage points or a guaranteed 100-turn poison; spread, exposure and immunity determine its effects.";text+=" Its wither path returns one Rotberry seed and does not apply the ordinary lotus seed-drop bonus.";break;
            case "BlandfruitBush":text="Drops one uncooked Blandfruit through the normal floor-drop action. Raw fruit cannot be eaten; cooking creates the separate food/potion effect. Inspection does not produce or identify a meal.";break;
            default:return "";
        }
        return "_Plant effect and units_\n"+text+" Planting consumes a seed and its ordinary action time; discovering or inspecting a seed does not activate it. Scheduled durations, cloud volume, healing reserves and block points are different resources. Existing challenge, plant-trigger and subclass rules remain unchanged.";
    }
}
