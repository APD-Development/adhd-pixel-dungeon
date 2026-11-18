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

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

/** Nutrition and action-time inputs, without eating or previewing random meal effects. */
public final class FoodExplanation {
    private FoodExplanation() {}
    public static float baseNutrition(Food food){
        return food instanceof Pasty && com.shatteredpixel.shatteredpixeldungeon.utils.Holiday.getCurrentHoliday()==com.shatteredpixel.shatteredpixeldungeon.utils.Holiday.LUNAR_NEW_YEAR?Hunger.HUNGRY:food.energy;
    }
    public static float nutritionBeforeHorn(Food food){
        float base=baseNutrition(food);
        return nutritionBeforeHorn(base);
    }
    public static float nutritionBeforeHorn(float base){
        return Dungeon.isChallenged(Challenges.NO_FOOD)?base/3f:base;
    }
    private static String unavailable(Food food){
        if(food instanceof MysteryMeat.PlaceHolder)return "This recipe accepts raw, stewed, chargrilled or frozen meat.";
        if(food instanceof Blandfruit && ((Blandfruit)food).potionAttrib==null)return "This fruit cannot be eaten raw. Cook it with a seed first.";
        return "";
    }
    public static String summary(Food food){
        String unavailable=unavailable(food);if(!unavailable.isEmpty())return unavailable;
        String text="Refills _"+n(nutritionBeforeHorn(food))+" hunger_.";
        if(Dungeon.isChallenged(Challenges.NO_FOOD))text+=" On Diet's reduction is included.";
        float time=food.previewEatingTime();
        if(Float.isFinite(time))text+=(text.isEmpty()?"":" ")+"Eating takes _"+n(time)+(time==1?" turn_.":" turns_.");
        if(Dungeon.hero!=null&&Dungeon.hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty.class)!=null)
            text+=" An active Horn of Plenty curse refills 33% less.";
        if(food instanceof Blandfruit)text+="\n"+BlandfruitExplanation.summary((Blandfruit)food);
        else if(food instanceof Pasty)text+="\n"+PastyExplanation.summary(Dungeon.hero);
        else {String effect=specialEffect(food,Dungeon.hero);if(!effect.isEmpty())text+="\n"+effect.trim();}
        return text;
    }
    public static String details(Food food){
        String unavailable=unavailable(food);if(!unavailable.isEmpty())return unavailable;
        return "_Food and eating_\nThis meal refills "+n(nutritionBeforeHorn(food))+" hunger, including On Diet if active. You're Hungry at "+n(Hunger.HUNGRY)+" hunger and Starving at "+n(Hunger.STARVING)+". Hunger relief stops when you are full, so excess nourishment is wasted. On Diet reduces food's nourishment to one third. A cursed Horn of Plenty reduces it by another 33%.\nEating normally takes "+n(Food.TIME_TO_EAT)+" turns. Meal talents can shorten this, while berries and Supply Rations already take only one turn without those talents. The eating time above uses your current meal talents."+specialEffect(food,Dungeon.hero);

    }
    private static String n(float value){return ObfuscationExplanation.number(value);}
    public static String specialEffect(Food food,Hero hero){
        if(food instanceof MysteryMeat.PlaceHolder)return "";
        String health=hero==null?"one quarter of your maximum health, rounded down":(hero.HT/4)+" HP";
        boolean noHealing=hero!=null&&com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.disabled();
        String healing=noHealing?"a healing effect disabled by your current rules":"healing for "+health+" before healing modifiers";
        String bark=hero==null?"one quarter of your maximum health, rounded down":Integer.toString(hero.HT/4);
        String cure="cures poison, bleeding, blindness, weakness, vulnerability, cripple, drowsiness, slowing and vertigo";
        if(food instanceof FrozenCarpaccio)return "\n_Frozen meat_\nEach meal has five equally likely outcomes (20% each): invisibility for "+n(Invisibility.DURATION)+" turns; Barkskin that can block up to "+bark+" extra physical damage; a cure for the ailments listed below; "+healing+"; or no extra benefit. Barkskin weakens by 1 each turn. The cleansing outcome "+cure+".";
        if(food instanceof PhantomMeat)return "\n_Phantom meat_\nGrants invisibility for "+n(Invisibility.DURATION)+" turns and Barkskin that can block up to "+bark+" extra physical damage, weakening by 1 each turn. "+(noHealing?"Your current rules prevent its healing.":"Restores "+health+" before healing modifiers.")+" It also "+cure+".";
        if(food instanceof MysteryMeat)return "\n_Raw meat risk_\nEach meal has a 20% chance of each outcome: catching fire; being rooted for "+n(Roots.DURATION*2)+" turns; being poisoned for "+(hero==null?"a duration based on your maximum health":(hero.HT/5)+" turns")+"; being slowed for "+n(Slow.DURATION)+" turns; or no harmful effect. Poison damage varies over its duration. Immunities and resistances can protect you. Cook or freeze the meat to avoid these risks.";
        if(food instanceof MeatPie&&noHealing)return "\n_Well Fed_\nPauses hunger for "+n(Dungeon.isChallenged(Challenges.NO_FOOD)?Hunger.STARVING/3:Hunger.STARVING)+" turns, but your current rules prevent its healing. Salt Cube extends this duration. Eating another pie refreshes it.";
        if(food instanceof MeatPie)return "\n_Well Fed_\nPauses hunger and normally restores 1 HP every "+WellFed.HEAL_INTERVAL+" turns for "+n(Dungeon.isChallenged(Challenges.NO_FOOD)?Hunger.STARVING/3:Hunger.STARVING)+" turns. Salt Cube makes both the duration and the time between heals longer. Your healing setting still applies. Eating another pie refreshes Well Fed instead of stacking its duration.";
        if(food instanceof Berry)return "\nEvery second berry you eat drops one random seed at your feet.";
        if(food instanceof SupplyRation)return "\n"+(noHealing?"Your current rules prevent its healing. It still adds 1 charge":"Restores 5 HP before healing modifiers and adds 1 charge")+" to your Cloak of Shadows, if carried. A full cloak gains no extra charge.";
        if(food instanceof Blandfruit)return "\n"+BlandfruitExplanation.details((Blandfruit)food,hero);
        if(food instanceof Pasty)return "\n"+PastyExplanation.details(hero);
        return "";
    }

}
