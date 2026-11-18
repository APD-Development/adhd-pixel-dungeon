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
package com.shatteredpixel.shatteredpixeldungeon.apd.healing;

/** Confirmation changes only whether an identified healing potion requires consent. */
public final class HealingConfirmation {
    private HealingConfirmation() {}
    // Category hotbar normally offers a second action menu. With confirmation Off,
    // selecting a known healing potion is itself the one-tap DRINK action.
    public static boolean categoryDirectDrink(boolean identified, int threshold) {
        return identified && threshold == -1;
    }

    /** An unambiguous healing shortcut must not add a chooser when warnings are off. */
    public static com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing directQuickslotPotion(
            java.util.List<? extends com.shatteredpixel.shatteredpixeldungeon.items.Item> items, int threshold) {
        if (items.size() != 1 || !(items.get(0) instanceof com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing)) return null;
        com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing potion =
                (com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing) items.get(0);
        return categoryDirectDrink(potion.isKnown(), threshold) ? potion : null;
    }

    public static boolean required(boolean identified, int threshold, int hp, int maxHp) {
        return identified && threshold >= 0 && threshold <= 100 && maxHp > 0
                && 100L * hp >= (long) threshold * maxHp;
    }
}
