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
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;

/** Projects saved search memory onto visible wall faces, edges and raised overhangs. */
public final class SearchedWallMarkers {
    public static final int FACE=1, OVERHANG=2, LEFT=4, RIGHT=8;
    private SearchedWallMarkers(){}
    private static boolean known(Level l,int p){return p>=0&&p<l.length()&&(l.heroFOV[p]||l.visited[p]||l.mapped[p]);}
    private static boolean open(Level l,int p){return known(l,p)&&!DungeonTileSheet.wallStitcheable(l.map[p]);}
    /** A renderer and UI must use the same public floor/terrain/geometry eligibility rule. */
    public static boolean couldBeDoor(Level l,int p){return DoorSearchEligibility.candidate(l,p);}
    /**
     * A visible keyed, crystal-keyed or barricaded special-room entrance identifies a
     * deliberately gated room. Its perimeter walls must not suggest another hidden
     * doorway just because their geometry resembles an ordinary passage wall.
     * Do not consult unseen entrances: marker presentation must not reveal a room
     * the player has not observed. The recorded searches themselves are retained.
     */
    private static boolean gatedRoomPerimeter(Level l, int p) {
        if (!(l instanceof RegularLevel) || !((RegularLevel) l).apdHasRooms()) return false;
        int w = l.width(), y = p / w;
        // Old saves may contain checkmarks created from inside a keyed room. Hide
        // those within its actual bounds whenever the hero is inside that room.
        // The original memory bits are not deleted or migrated.
        if (Dungeon.level == l && Dungeon.hero != null
                && DoorSearchEligibility.insideGatedRoom(l, Dungeon.hero.pos)) {
            Room current = ((RegularLevel) l).room(Dungeon.hero.pos);
            int x = p % w;
            if (current != null && x >= current.left && x <= current.right
                    && y >= current.top && y <= current.bottom) return true;
        }
        // Only an immediate room interior can identify this wall as its perimeter.
        // Checking all four sides also handles walls that border another ordinary room.
        // The renderer is hot: do not allocate an array for each wall marker.
        for (int direction = 0; direction < 4; direction++) {
            int neighbour = direction == 0 ? p - w : direction == 1 ? p + w
                    : direction == 2 ? p - 1 : p + 1;
            if (neighbour < 0 || neighbour >= l.length()) continue;
            if (direction >= 2 && neighbour / w != y) continue;
            Room room = ((RegularLevel) l).room(neighbour);
            if (!(room instanceof SpecialRoom)) continue;
            SpecialRoom special = (SpecialRoom) room;
            Room.Door entrance = special.entrance();
            if (entrance == null || entrance.x < 0 || entrance.x >= w
                    || entrance.y < 0 || entrance.y >= l.height()) continue;
            int door = entrance.y * w + entrance.x;
            if (!known(l, door)) continue;
            if (entrance.type == Room.Door.Type.LOCKED
                    || entrance.type == Room.Door.Type.CRYSTAL
                    || entrance.type == Room.Door.Type.BARRICADE) return true;
        }
        return false;
    }
    private static boolean showWall(Level l,int p){
        return l.searchedWalls.shows(l,p)&&couldBeDoor(l,p)&&!gatedRoomPerimeter(l,p);
    }
    public static int visual(Level l,int p){
        if(p<0||p>=l.length())return -1;
        // Never host another tile's raised-wall mark on a visible doorway.
        int tile=l.map[p];
        if(known(l,p)&&(tile==Terrain.DOOR||tile==Terrain.OPEN_DOOR||tile==Terrain.LOCKED_DOOR
                ||tile==Terrain.HERO_LKD_DR||tile==Terrain.CRYSTAL_DOOR||tile==Terrain.BARRICADE))return -1;
        int w=l.width(),mask=0;
        if(showWall(l,p)){
            if(p+w<l.length()&&open(l,p+w))mask|=FACE;
            else {
                if(p%w>0&&open(l,p-1)&&open(l,p-1-w)&&open(l,p-1+w))mask|=LEFT;
                if(p%w<w-1&&open(l,p+1)&&open(l,p+1-w)&&open(l,p+1+w))mask|=RIGHT;
            }
        }
        // SPD draws a southern wall's visible lip on the preceding floor tile.
        // Its own tile is obscured by wall blocking/fog; never bypass either layer.
        if(open(l,p)&&p+w<l.length()&&showWall(l,p+w))mask|=OVERHANG;
        return mask==0?-1:mask;
    }
}
