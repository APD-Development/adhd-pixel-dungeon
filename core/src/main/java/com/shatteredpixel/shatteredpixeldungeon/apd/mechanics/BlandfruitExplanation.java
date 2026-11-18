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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;

/** Visible cooked-fruit effects; never eats, throws, identifies a potion or rolls an outcome. */
public final class BlandfruitExplanation {
    private BlandfruitExplanation(){}
    private static boolean recognized(Blandfruit fruit){
        return fruit.potionAttrib!=null&&Potion.SeedToPotion.types.containsValue(fruit.potionAttrib.getClass());
    }
    public static boolean releasesEffectWhenThrown(Blandfruit fruit){
        Potion p=fruit.potionAttrib;
        return p instanceof PotionOfLiquidFlame||p instanceof PotionOfToxicGas
                ||p instanceof PotionOfParalyticGas||p instanceof PotionOfFrost
                ||p instanceof PotionOfLevitation||p instanceof PotionOfPurity;
    }
    private static boolean harmfulMeal(Blandfruit fruit){
        Potion p=fruit.potionAttrib;
        return p instanceof PotionOfLiquidFlame||p instanceof PotionOfToxicGas
                ||p instanceof PotionOfParalyticGas||p instanceof PotionOfFrost;
    }
    private static String n(float value){return ObfuscationExplanation.number(value);}
    public static String summary(Blandfruit fruit){
        if(fruit.potionAttrib==null)return "Uncooked fruit cannot be eaten.";
        if(!recognized(fruit))return "Detailed effects are unavailable for this imbued fruit.";
        if(fruit.potionAttrib instanceof PotionOfLiquidFlame)return "_Eating releases fire around you._ Throw it to release the fire at the landing tile instead.";
        if(fruit.potionAttrib instanceof PotionOfToxicGas)return "_Eating releases toxic gas around you._ Throw it to release the gas at the landing tile instead.";
        if(fruit.potionAttrib instanceof PotionOfParalyticGas)return "_Eating releases paralysing gas around you._ Throw it to release the gas at the landing tile instead.";
        if(fruit.potionAttrib instanceof PotionOfFrost)return "_Eating releases freezing cold around you._ Throw it to freeze the area around the landing tile instead.";
        if(fruit.potionAttrib instanceof PotionOfHealing){
            if(Dungeon.hero!=null&&Dungeon.isChallenged(Challenges.NO_HEALING))return "_Pharmacophobia: poisons you instead of healing._ It still cures existing ailments first.";
            if(Dungeon.hero!=null&&com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.disabled())return "Your current rules disable healing. Still cures ailments such as poison, bleeding and blindness.";
            return "Gradually restores health and cures ailments such as poison, bleeding and blindness. Your healing setting affects how much health you recover.";
        }
        if(fruit.potionAttrib instanceof PotionOfStrength)return "Permanently increases your Strength by _1_.";
        if(fruit.potionAttrib instanceof PotionOfExperience)return "Grants XP equal to one full level’s requirement. At maximum level, this grants Bless instead.";
        if(fruit.potionAttrib instanceof PotionOfInvisibility)return "Makes you invisible for _"+n(Invisibility.DURATION)+" turns_. Attacking or using certain items ends invisibility early.";
        if(fruit.potionAttrib instanceof PotionOfHaste)return "Grants Haste for _"+n(Haste.DURATION)+" turns_, speeding up movement without speeding up attacks.";
        if(fruit.potionAttrib instanceof PotionOfMindVision)return "Reveals creatures across the floor for _"+n(MindVision.DURATION)+" turns_, even through walls. This does not let you attack through walls.";
        if(fruit.potionAttrib instanceof PotionOfLevitation)return "Floats you over pits and floor traps for _"+n(Levitation.DURATION)+" turns_. Reach safe ground before it ends. Throwing it releases Confusion Gas instead.";
        if(fruit.potionAttrib instanceof PotionOfPurity)return "Protects you from harmful clouds, fire and similar environmental hazards for _"+n(BlobImmunity.DURATION)+" turns_. Throwing it clears nearby harmful clouds instead.";
        return "Its visible cooked form determines its extra effect.";
    }
    public static String details(Blandfruit fruit,Hero hero){
        if(fruit.potionAttrib==null)return "Uncooked fruit cannot be eaten.";
        if(!recognized(fruit))return "Detailed effects are unavailable for this imbued fruit; no hidden effect is guessed.";
        String effect=PotionExplanation.effect(fruit.potionAttrib,hero);
        String text="_Cooked fruit: "+fruit.name()+"_\n"+effect;
        if(fruit.potionAttrib instanceof PotionOfHealing&&Dungeon.isChallenged(Challenges.NO_HEALING))
            text+="\n_Current challenge: Pharmacophobia._ The healing branch instead offers Poison"+(hero==null?" for a duration based on your level.":" for "+(4+hero.lvl/2)+" turns (4 plus half your level, rounded down).")+" Poison damage changes over time; immunity and resistance still apply.";
        if(harmfulMeal(fruit))text+="\n_Eating is hazardous:_ it releases the described shattering effect at your own location when eaten. Relieving hunger does not protect you from that effect.";
        else text+="\nEating grants the benefit as part of the meal; it takes only the displayed eating time.";
        if(releasesEffectWhenThrown(fruit)){
            text+="\nThrowing onto ordinary ground releases this form's shattering effect at the landing tile and leaves _1 portion of edible fruit chunks_. Chunks refill "+n(FoodExplanation.nutritionBeforeHorn(Hunger.STARVING))+" hunger before an active Horn curse. The potion effect and that later meal are separate; the thrown fruit does not feed you immediately. Wells and pits instead keep their ordinary item-drop/fall behavior.";
        }else text+="\nThrowing normally drops the _whole fruit_ without applying its eating effect or creating chunks. Wells and pits keep their ordinary item-drop/fall behavior.";
        return text;
    }
}
