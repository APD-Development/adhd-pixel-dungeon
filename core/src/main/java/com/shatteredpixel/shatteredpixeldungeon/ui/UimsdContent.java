/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.watabou.noosa.Gizmo;
import com.watabou.noosa.ui.Component;

/** Content whose children are owned by a replaceable UIMSD view. */
final class UimsdContent extends Component {
    @Override
    public synchronized void clear() {
        if (length == 0) return; // Also safe after destroy, matching native Group.clear.
        // Snapshot because destroying a child may detach it from its parent.
        Gizmo[] previous = members.toArray(new Gizmo[0]);
        for (Gizmo child : previous) {
            if (child != null) child.destroy();
        }
        super.clear();
    }
}
