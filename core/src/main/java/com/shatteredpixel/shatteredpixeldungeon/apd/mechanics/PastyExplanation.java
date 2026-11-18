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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.utils.Holiday;

/** Describes the same current calendar form used by Pasty.satisfy; never applies it. */
public final class PastyExplanation {
    private PastyExplanation() {}
    public static String summary(Hero hero) {
        switch (Holiday.getCurrentHoliday()) {
            case LUNAR_NEW_YEAR:
                return "Leaves one extra meal for later, that refills _"+ObfuscationExplanation.number(FoodExplanation.nutritionBeforeHorn(Hunger.HUNGRY/2f))+" hunger_.";
            case APRIL_FOOLS: case EASTER:
                return "Gives non-cursed artifacts a small burst of recharge. The amount depends on the artifact.";
            case PRIDE:
                return "Charms one adjacent enemy for 5 turns before resistance, preventing it from attacking you. Bosses and minibosses are unaffected.";
            case SHATTEREDPD_BIRTHDAY: case PD_BIRTHDAY:
                return "Grants _"+(hero==null?"10% of a full level's XP requirement, with a minimum of 2 XP":Math.max(2,hero.maxExp()/10)+" XP")+"_.";
            case HALLOWEEN:
                return "Heals _"+(hero==null?"5% of your maximum HP, with a minimum of 3 HP":Math.max(3,hero.HT/20)+" HP")+"_ before your healing modifiers.";
            case WINTER_HOLIDAYS:
                return "Restores _half a charge_ to each wand. Full wands gain nothing; partial charges combine into usable charges.";
            case NEW_YEARS:
                return "Grants _"+(hero==null?"10% of your maximum HP in shielding, with a minimum of 5":Math.max(5,hero.HT/10)+" shielding")+"_. Keeps a stronger existing Barrier instead of adding to it.";
            default: return "";
        }
    }

    public static String details(Hero hero) {
        String text="_Current seasonal food effect_\n"+summary(hero);
        switch(Holiday.getCurrentHoliday()) {
            case LUNAR_NEW_YEAR:
                return text+" The main meal provides a regular ration’s nourishment; the leftovers provide half as much. On Diet reduces both portions to one third. The leftover goes into your inventory, or drops if it cannot be collected. Eating it later consumes its own normal meal action.";
            case APRIL_FOOLS: case EASTER:
                return text+" Adds 2 turns of artifact-specific bonus recharge to each non-cursed artifact currently benefiting from its recharge effect. This is not 2 completed charges for every artifact.";
            case PRIDE:
                return text+" Among adjacent eligible enemies, your focused target has priority. Charm prevents that enemy from attacking you while active; it does not make the enemy an ally. Immunity and resistance apply. Reapplication adds the resisted duration to an existing Charm schedule and assigns you as its source. ";
            case SHATTEREDPD_BIRTHDAY: case PD_BIRTHDAY:
                return text+" The XP bonus is one tenth of your current level’s full requirement, rounded down, with a minimum of 2 XP. It does not depend on how close you are to levelling up.";
            case HALLOWEEN:
                return text+" Normally heals 5% of your maximum HP, rounded down, with a minimum of 3 HP. Your healing setting changes the amount restored, and healing stops at full health.";
            case WINTER_HOLIDAYS:
                return text+" Each attached wand charger receives 0.5 charge. Existing fractional charge is retained until it reaches a whole charge; reaching capacity clears excess fractional charge. Already-full wands gain nothing. ";
            case NEW_YEARS:
                int offer=hero==null?0:Math.max(5,hero.HT/10);
                Barrier barrier=hero==null?null:hero.buff(Barrier.class);
                return text+" Normally grants shielding equal to 10% of your maximum HP, rounded down, with a minimum of 5. A stronger Barrier replaces a weaker one; the amounts do not add together."+(hero==null?"":" Your resulting Barrier reserve would be "+Math.max(offer,barrier==null?0:barrier.shielding())+" shield points.")+" A new shield at least as strong as your current Barrier resets its fading timer. A stronger existing Barrier keeps its current timer. The shield gradually fades and absorbs damage before you lose HP.";
            default: return text;
        }
    }
}
