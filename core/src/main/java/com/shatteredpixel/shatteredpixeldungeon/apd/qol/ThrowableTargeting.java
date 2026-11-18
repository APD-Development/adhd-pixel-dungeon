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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;

/** Finds an aim the player could already use; never changes projectile collision rules. */
public final class ThrowableTargeting {
    private ThrowableTargeting() {}

    public static boolean hasExistingTrajectory(int from, int target) {
        int width = Dungeon.level.width();
        int x = from % width, y = from / width;
        int targetX = target % width, targetY = target / width;
        for (int aim = 0; aim < Dungeon.level.length(); aim++) {
            if (aim == from || aim == target) continue;
            int dx = aim % width - x, dy = aim / width - y;
            int ax = Math.abs(dx), ay = Math.abs(dy);
            boolean horizontal = ax > ay;
            int major = Math.max(ax, ay), minor = Math.min(ax, ay);
            int steps = Math.abs(horizontal ? targetX - x : targetY - y);
            if (steps > major) continue;
            // Ballistica starts its integer error at major/2. Reject aims whose
            // unblocked raster cannot visit the target, before tracing collisions.
            int minorSteps = (int) ((major / 2L + (long) steps * minor) / major);
            int hitX = x + Integer.signum(dx) * (horizontal ? steps : minorSteps);
            int hitY = y + Integer.signum(dy) * (horizontal ? minorSteps : steps);
            if (hitX == targetX && hitY == targetY
                    && new Ballistica(from, aim, Ballistica.PROJECTILE).collisionPos == target) {
                return true;
            }
        }
        return false;
    }
}
