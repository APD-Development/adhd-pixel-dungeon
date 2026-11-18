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

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import java.util.ArrayList;

/** Explains the existing material rules without applying repair, wear or recycling. */
public final class MetalExplanation {
    private MetalExplanation(){}
    private static String n(float value){return ObfuscationExplanation.number(value);}
    public static String details(){
        return "_Repair material and durability_\nMetal amounts are material units. An ordinary thrown weapon has 100 durability points when full. The starting material amount for 100 durability points is 5 × (weapon tier + 1) × 1.35^(permanent weapon level). One metal unit restores 100 divided by that starting amount in durability points. The ordinary partial-repair branch computes whole units as ceil(starting amount × (0.999 − current durability / 100)); ceil means round up to a whole metal unit. The 0.999 adjustment prevents rounding errors from charging an extra unit. Available metal limits how much can be restored. If the current weapons already have their full number of uses but a weapon is missing from the normal stack, the existing branch can instead reconstruct a spare. Its required metal and the rebuilt weapon's condition follow that branch's normal rules. Darts and tipped darts are not eligible for this repair action.\n\n_Recycling a stack_\nThe recipe consumes the entire eligible stack and "+LiquidMetal.Recipe.energyCost()+" alchemy energy. It requires known, uncursed status. For a weapon whose default stack has three items, base yield per weapon = 5 × (tier + 1). Other default stack sizes use 3 / default stack size instead, replacing that tier formula. Multiply by 1.35^min(5, permanent level): levels above+5 do not increase recycling yield, although repair still uses the uncapped level. Stack weight = current quantity − 1 + 0.25 + 0.0075 × current durability points; the other items count as whole, while the current worn item contributes its remaining weight. Round(weight × yield per weapon) gives metal units. An unknown upgrade level has no exact preview yield; its true yield is resolved when consumed. Recycling consumes its input stack to produce the calculated material.";
    }
    public static String forWeapon(MissileWeapon weapon){
        if(!weapon.levelKnown||!weapon.cursedKnown)return "\n_Liquid Metal_\nIdentify this weapon before viewing its exact material calculations.";
        String text="\n_Liquid Metal for this stack_\n";
        if(weapon instanceof Dart)text+="Darts and tipped darts cannot be selected for metal repair. ";
        else{
            float scale=LiquidMetal.repairMetalScale(weapon);
            text+="Starting metal amount for 100 durability points: "+n(scale)+" metal units; each metal restores "+n(100/scale)+" durability points. The amount actually spent depends on wear, missing weapons and available metal. ";
        }
        ArrayList<Item> inputs=new ArrayList<>();inputs.add(weapon);LiquidMetal.Recipe recipe=new LiquidMetal.Recipe();
        if(recipe.testIngredients(inputs))text+="Recycling this current stack yields "+recipe.sampleOutput(inputs).quantity()+" metal units for "+recipe.cost(inputs)+" alchemy energy and consumes the stack. ";
        else text+="This stack is currently ineligible for recycling because it is cursed. ";
        return text+"These calculations use this weapon's own known permanent level and current stack condition; they do not include temporary damage-level bonuses.";
    }
}
