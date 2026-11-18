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
package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;

/** If the hero can see an enemy, a throw at that enemy should reach it.
 * Field of view is more permissive than a straight projectile line, so when the
 * direct line clips a corner we look for another aim cell whose projectile path
 * still ends on the target. Never aims at anything the hero cannot see. */
public final class ThrowAim {
    private ThrowAim() { }

    private static final int MAX_RADIUS = 8;

    public static int aim(Hero hero, Item item, int dst) {
        Level l = Dungeon.level;
        if (hero == null || item == null || l == null || dst < 0 || dst >= l.length()) return dst;
        if (item.targetingPos(hero, dst) == dst) return dst;
        Char ch = Actor.findChar(dst);
        if (ch == null || ch == hero || !l.heroFOV[dst] || ch.invisible > 0) return dst;

        int w = l.width(), tx = dst % w, ty = dst / w;
        // Closest aim cells first so the shot looks as straight as possible.
        for (int r = 1; r <= MAX_RADIUS; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int x = tx + dx, y = ty + dy;
                    if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
                    int cell = x + y * w;
                    if (cell == hero.pos) continue;
                    if (item.targetingPos(hero, cell) == dst) return cell;
                }
            }
        }
        return dst;
    }
}
