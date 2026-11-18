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

import com.shatteredpixel.shatteredpixeldungeon.items.keys.*;

/** Explains existing lock matching without testing or opening any map location. */
public final class KeyExplanation {
    private KeyExplanation(){}
    public static String details(Key key){
        String target=key instanceof GoldenKey?"a locked chest":key instanceof CrystalKey?"a crystal chest or crystal door":key instanceof IronKey?"an ordinary locked door":key instanceof WornKey?"a locked exit":"a matching lock";
        String text="_Using this key_\nThis key opens "+target+". ";
        text+=key.depth>0?"It belongs to Floor "+key.depth+"; a key from a different floor cannot substitute. ":"No floor is recorded for this key; a matching floor cannot be assumed. ";
        return text+"Picking it up records it in your journal; keys do not take up backpack slots. Using a key consumes it and takes "+ObfuscationExplanation.number(Key.TIME_TO_UNLOCK)+(Key.TIME_TO_UNLOCK==1?" turn.":" turns.")+" You must stand next to a locked door; you can open a chest from beside it or while standing on it. These keys work on main-dungeon floors, not side areas. Crystal doors and crystal chests on the same floor share your supply of crystal keys.";

    }
}
