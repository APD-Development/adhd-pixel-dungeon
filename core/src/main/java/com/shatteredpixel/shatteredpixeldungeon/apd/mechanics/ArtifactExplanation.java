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

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.apd.ChaliceDamagePreview;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.*;

/** Reviewed artifact explanations, never an artifact use/recharge/proc. */
public final class ArtifactExplanation {
    private ArtifactExplanation(){}
    private static String n(float value){return ObfuscationExplanation.number(value);}
    public static String summary(Artifact artifact){
        if(!artifact.isIdentified())return "";
        if(artifact.cursed){String curse=ArtifactEconomyExplanation.summary(artifact,Dungeon.hero);return curse.isEmpty()?"":"\n\n"+curse;}
        String effect="";
        if(artifact instanceof CapeOfThorns)effect="When active, absorbs a random amount of damage from each hit. If the attacker is adjacent, it takes the absorbed damage instead.";
        else if(artifact instanceof ChaliceOfBlood&&com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.disabled())effect="Your current rules prevent natural healing."+(artifact.level()<artifact.maximumUpgradeSteps()?" Pricking still costs health and upgrades the Chalice.":"");
        else if(artifact instanceof ChaliceOfBlood)effect="While worn, restores _1 HP every "+n(10f-(1.33f+artifact.level()*.667f))+" turns_ before other healing effects. Does not heal while starving.";
        else if(artifact instanceof HornOfPlenty){int nourishment=(int)(Hunger.STARVING/5f);if(Dungeon.isChallenged(Challenges.NO_FOOD))nourishment/=3;effect="Each charge refills _"+nourishment+" hunger_. Eat uses multiple charges based on your hunger and stored food.";}
        if(artifact instanceof DriedRose)effect=RoseExplanation.summary((DriedRose)artifact);
        if(artifact instanceof CloakOfShadows)effect=CloakExplanation.summary((CloakOfShadows)artifact)+" "+ArtifactEconomyExplanation.summary(artifact,Dungeon.hero);
        if(artifact instanceof TalismanOfForesight)effect=TalismanExplanation.summary((TalismanOfForesight)artifact)+" "+ArtifactEconomyExplanation.summary(artifact,Dungeon.hero);
        if(effect.isEmpty())effect=ArtifactEconomyExplanation.summary(artifact,Dungeon.hero);
        if(effect.isEmpty())return "";
        if(Dungeon.hero!=null&&Dungeon.hero.buff(MagicImmune.class)!=null&&(artifact instanceof ChaliceOfBlood||artifact instanceof HornOfPlenty))effect="Magic immunity currently suppresses this artifact's normal activation/recharging benefit. "+effect;
        return "\n\n"+effect;
    }
    public static String details(Artifact artifact){
        if(!artifact.isIdentified())return "Identify this artifact before viewing its exact stored amounts and upgrade calculations.";
        if(artifact.cursed)return ArtifactEconomyExplanation.details(artifact,Dungeon.hero);
        String state="";
        if(artifact.maximumCharges()>0)state=artifact.maximumCharges()==100?"Charge: _"+artifact.currentCharges()+"%_.\n\n":"Charges: _"+artifact.currentCharges()+"/"+artifact.maximumCharges()+"_.\n\n";
        if(artifact instanceof DriedRose)state="Summoning charge: _"+artifact.currentCharges()+"/"+artifact.maximumCharges()+"_. While your ghost is alive, its icon shows the ghost’s health percentage.\n\n";
        int level=artifact.level();
        if(artifact instanceof CapeOfThorns)state+="_Charging and deflection_\nWhile inactive, gains "+n(.5f+.05f*level)+" charge points per damage point: damage × (0.5 + 0.05 × level), rounded down. At full charge, empties the meter and protects you for "+(10+level)+" turns, including the hit that filled it. Current time left: "+artifact.effectCountdown()+" turns. Each hit can have anywhere from none to all of its damage absorbed, with middle amounts more likely. An adjacent attacker takes the absorbed damage. Each upgrade adds 0.05 charge per damage and one turn to new activations.";
        else if(artifact instanceof ChaliceOfBlood)state+="_Healing and pricking_\nBase natural-healing interval: 10 − (1.33 + 0.667 × level) = "+n(10f-(1.33f+level*.667f))+" turns per HP. Each upgrade shortens it by 0.667 turn. Hunger, recharge bonuses and your run’s healing rules still apply. "+(level<artifact.maximumUpgradeSteps()?"Your next prick can deal "+ChaliceDamagePreview.baseMin(level)+"–"+ChaliceDamagePreview.baseMax(level)+" damage before protection. Minimum = round up(3 + 2.5 × level²); maximum = round down(7 + 3.5 × level²). Check the confirmation’s damage estimate before pricking; this can kill you.":"Maximum upgrade reached: this Chalice cannot be pricked again.");
        else if(artifact instanceof HornOfPlenty)state+="_Food and recharge_\nEach charge restores one fifth of the hunger needed to starve, rounded down; On Diet divides that amount by three. Snack spends one charge. Eat spends enough available charges to reduce your hunger, always at least one. Experience recharges the Horn at "+n((.25f+.125f*level)*5)+" charges per whole hero-level before recharge bonuses, capped by capacity. Formula: level progress × (0.25 + 0.125 × Horn level) × 5. Upgrades increase both this rate and capacity.";
        if(artifact instanceof DriedRose)state+="\n\n"+RoseExplanation.details((DriedRose)artifact,Dungeon.hero);
        if(artifact instanceof CloakOfShadows)state+="\n\n"+CloakExplanation.details((CloakOfShadows)artifact,Dungeon.hero);
        if(artifact instanceof TalismanOfForesight)state+="\n\n"+TalismanExplanation.details((TalismanOfForesight)artifact);
        return (state+"\n\n"+ArtifactEconomyExplanation.details(artifact,Dungeon.hero)).trim();
    }
}
