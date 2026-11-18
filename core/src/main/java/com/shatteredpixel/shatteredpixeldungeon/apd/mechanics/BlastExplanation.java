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
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** Pure explanation of the blast's public staff type and selected talent ranks. */
public final class BlastExplanation {
    private BlastExplanation(){}
    private static String n(float x){return ObfuscationExplanation.number(x);}
    /** Existing normal description: public staff type and talents, never hidden levels or targets. */
    public static String summary(Hero hero){
        if(hero==null)return "";
        MagesStaff staff=hero.belongings.getItem(MagesStaff.class);
        if(staff==null||staff.wandClass()==null)return "";
        Class<? extends Wand> type=staff.wandClass();float power=1+.25f*hero.pointsInTalent(Talent.ELEMENTAL_POWER),factor=ElementalBlast.blastDamageFactor(type);
        String damage=Float.isFinite(factor)&&factor>0?"Initial base damage: _"+Math.round(15*power*factor)+"–"+Math.round(25*power*factor)+"_. ":"";
        String effect;
        if(type==WandOfMagicMissile.class)effect="Grants _"+n(power*Recharging.DURATION/2)+" turns_ of wand recharging.";
        else if(type==WandOfLightning.class)effect="Electrifies water and paralyzes survivors for _"+n(power*Paralysis.DURATION/2)+" turns_.";
        else if(type==WandOfDisintegration.class)effect="Passes through walls.";
        else if(type==WandOfFireblast.class)effect="Sets creatures and flammable terrain on fire, opens ordinary doors and clears freezing effects.";
        else if(type==WandOfCorrosion.class)effect="Inflicts _4 turns_ of corrosion, starting at _"+Math.round(6*power)+" damage_.";
        else if(type==WandOfBlastWave.class)effect="Pushes creatures outward; obstacles can stop the push.";
        else if(type==WandOfLivingEarth.class)effect="Heals an existing Earth Guardian when creatures are hit.";
        else if(type==WandOfFrost.class)effect="Puts out fires and freezes survivors for _"+n(power*Frost.DURATION)+" turns_.";
        else if(type==WandOfPrismaticLight.class)effect="Maps nearby terrain and reveals secrets, blinds creatures for _"+n(power*Blindness.DURATION/2)+" turns_, and grants _"+n(power*(Dungeon.isChallenged(Challenges.DARKNESS)?10:50))+" turns_ of Light.";
        else if(type==WandOfWarding.class)effect="Restores spent attacks to upgraded wards and heals sentries. Basic wards are unchanged. This blast passes through walls.";
        else if(type==WandOfTransfusion.class)effect="Heals allies and charmed creatures by _"+Math.round(10*power)+" HP_, turning excess healing into shielding. Other creatures are charmed for _"+n(power*Charm.DURATION/2)+" turns_, or take _"+Math.round(15*power)+"–"+Math.round(25*power)+" base damage_ if undead.";
        else if(type==WandOfCorruption.class)effect="Inflicts _"+n(power*5)+" turns_ of amok, making creatures attack each other.";
        else if(type==WandOfRegrowth.class)effect="Roots creatures for _"+n(power*Roots.DURATION)+" turns_. Each suitable plant-free tile has a _"+n(Math.min(100,33*power))+"%_ chance to grow tall grass.";
        else return "";
        return damage+effect+(type==WandOfWarding.class?"":" Direct harmful effects spare allies.")
            +(type==WandOfFireblast.class||type==WandOfLightning.class?" Terrain hazards can still harm you and allies.":"");
    }
    public static String details(Hero hero){
        if(hero==null)return "";
        MagesStaff staff=hero.belongings.getItem(MagesStaff.class);
        if(staff==null||staff.wandClass()==null)return "\n\n_Elemental Blast_\nAn imbued Mage's Staff is required. The staff's wand type chooses the blast effects; its hidden upgrade level is not inspected here.";
        Class<? extends Wand> type=staff.wandClass();
        float power=1+.25f*hero.pointsInTalent(Talent.ELEMENTAL_POWER),factor=ElementalBlast.blastDamageFactor(type);
        int radius=4+hero.pointsInTalent(Talent.BLAST_RADIUS),barrier=hero.pointsInTalent(Talent.REACTIVE_BARRIER);
        String result="\n\n_Elemental Blast area and power_\nThe imbued "+Messages.get(type,"name")+" selects this blast. It covers a full circle with radius 4 + Blast Radius rank = "+radius+" tiles, limited by its obstacle rules. Disintegration and Warding blasts can pass solid terrain; other types use their own paths. Casting costs one game-time unit. Effect power = 1 + 0.25 × Elemental Power rank = "+n(power)+". This is a talent factor, not a permanent wand upgrade; the blast does not use a hidden wand level.";
        if(Float.isFinite(factor))result+="\nInitial direct damage = round(a 15–25 damage roll × effect power × this wand type's factor "+n(factor)+"). Current initial range: "+Math.round(15*power*factor)+"–"+Math.round(25*power*factor)+" damage points before normal offensive and target rules. A zero initial factor does not rule out harmful secondary effects.";
        if(type==WandOfMagicMissile.class)result+="\nOffers the hero "+n(power*Recharging.DURATION/2)+" scheduled units of Recharging, not that many whole wand charges.";
        else if(type==WandOfLightning.class)result+="\nWater tiles become electrified. A surviving non-allied target is offered "+n(power*Paralysis.DURATION/2)+" scheduled units of Paralysis. Electricity follows its normal area and damage rules.";
        else if(type==WandOfFireblast.class)result+="\nOpens closed ordinary doors in the area, clears freezing effects, ignites flammable tiles and attempts to ignite surviving non-allied targets. Burning uses its normal duration and damage rules.";
        else if(type==WandOfCorrosion.class)result+="\nSurviving non-allied targets are offered Corrosion for four scheduled units, with starting strength round(6 × effect power) = "+Math.round(6*power)+" damage-strength points. Its subsequent ticks and growth follow Corrosion's own rules.";
        else if(type==WandOfBlastWave.class)result+="\nNon-allied targets are offered outward knockback. Strength = truncate((radius + 1 − floor(straight-line tile distance)) × effect power). This strength is not guaranteed tiles moved; obstacles and target properties still limit travel.";
        else if(type==WandOfLivingEarth.class)result+="\nIf qualifying characters are counted, the first existing Earth Guardian is offered round(effect power × count × 5) HP, capped at its maximum health. This does not summon a new guardian for each tile.";
        else if(type==WandOfFrost.class)result+="\nClears fire in affected cells, removes Burning from the hero, and offers surviving non-allied targets "+n(power*Frost.DURATION)+" scheduled units of Frost before their normal resistance and freezing rules.";
        else if(type==WandOfPrismaticLight.class)result+="\nMaps discoverable cells and reveals secrets around affected cells. Surviving non-allied targets are offered "+n(power*Blindness.DURATION/2)+" units of Blindness. The hero is offered "+n(power*(Dungeon.isChallenged(Challenges.DARKNESS)?10:50))+" units of Light under the current challenge rules.";
        else if(type==WandOfWarding.class)result+="\nExisting tier-two wards recover round(effect power) available zaps; tier-three wards recover round(a uniform 1–2 roll × effect power) available zaps. Tier-one wards receive no repair. Tier-four, five and six sentries recover round(9, 12 or 16 × effect power) HP respectively, capped at maximum HP. This does not create a new ward on each tile.";
        else if(type==WandOfTransfusion.class)result+="\nAllied or charmed characters are offered round(10 × effect power) = "+Math.round(10*power)+" HP of healing, with excess offered as Barrier points. Other living targets are offered "+n(power*Charm.DURATION/2)+" units of Charm toward the hero. Other undead targets instead take a separate 15–25 roll scaled by effect power, through normal offensive damage rules.";
        else if(type==WandOfCorruption.class)result+="\nSurviving non-allied targets are offered "+n(power*5)+" units of Amok. This does not automatically make every target a permanent ally.";
        else if(type==WandOfRegrowth.class)result+="\nEach eligible plant-free terrain tile has a "+n(Math.min(100,33*power))+"% tall-grass check, from 0.33 × effect power. Non-allied targets are offered "+n(power*Roots.DURATION)+" units of Roots. Terrain, plants and target rules still apply.";
        result+="\nReactive Barrier at rank "+barrier+" offers round(min(4 + rank, qualifying affected-character count) × 2.5 × rank) shield points, only when learned and the count is positive. Different blast effects contribute to that count at their own steps; it is not simply final HP damage dealt. A stronger existing Barrier is kept. No hidden character count is calculated for this inspector.";
        return result;
    }
}
