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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;

/** Read-only resource explanation; never consumes dew or healing credit. */
public final class DewExplanation {
    private DewExplanation(){}
    public static String details(Item item, Hero hero){
        String text="_Dew and your Waterskin_\nDew restores health. A Waterskin stores up to "+Waterskin.capacity()+" drops; collected dew is saved while it has room. Drinking spends drops and "+ObfuscationExplanation.number(Waterskin.drinkingTime())+" game-time unit. A full Waterskin can instead bless an Ankh. ";
        if(item instanceof Waterskin)text+="This Waterskin contains "+((Waterskin)item).storedDrops()+" of "+Waterskin.capacity()+" drops. ";
        text+="Each drop contributes 5% of maximum HP before rounding, missing-health limits and other healing effects. Multiple drops are rounded together, so their value can differ from adding individually rounded drops. With Shielding Dew, unused base healing can become temporary Barrier points up to that talent's cap; existing Barrier points count toward the cap. This is not permanent maximum HP. ";
        if(hero!=null){
            int current=hero.buff(Barrier.class)==null?0:hero.buff(Barrier.class).shielding();
            text+="\n\n_Your current values_\nMaximum HP: "+hero.HT+" HP. One drop's base value: "+Dewdrop.baseDewEffect(hero,1)+" HP; five drops together: "+Dewdrop.baseDewEffect(hero,5)+" HP before those limits. Shielding Dew cap: "+Dewdrop.shieldingDewCap(hero)+" Barrier points; current Barrier: "+current+" points. These base values are not a promise of that much recovered HP. ";
        }else text+="Personal HP and shielding values require a loaded character; no sample character is assumed. ";
        return text+="\n\n_Calculation and exceptions_\nBase value = round(maximum HP × 0.05 × drops used), rounding to the nearest whole point. Shielding Dew cap = round(maximum HP × 0.2 × trained talent rank). The Waterskin chooses drops from missing health and missing eligible shielding, rounds up with its existing small rounding allowance, and limits consumption to its stored drops. Vial of Blood can reduce drops needed for multi-drop healing and deliver that healing over time; shielding allocation uses its separate rule. The run's Healing percentage and fractional healing credit apply through the normal healing path, not to these unmodified base values. Drinking does not upgrade the skin or change dew drop rates. Automatic collection retains the selected enemy-visibility and capacity safeguards. Inspection spends no dew, health, shielding or game time.";
    }
}
