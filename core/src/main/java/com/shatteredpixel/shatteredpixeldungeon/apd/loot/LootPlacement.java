package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ShopRoom;
import com.watabou.utils.Point;

/** Deterministic placement; never use Level.drop (it can scatter/press terrain). */
public final class LootPlacement {
    private LootPlacement() {}

    public static boolean safe(Level level, int cell) {
        if (cell < 0 || cell >= level.length() || !level.passable[cell] || level.pit[cell]
                || level.traps.get(cell) != null || level.plants.get(cell) != null) return false;
        Heap heap = level.heaps.get(cell);
        if (heap != null && heap.type != Heap.Type.HEAP) return false;
        for (Mob mob : level.mobs) if (mob.pos == cell) return false;
        for (Blob blob : level.blobs.values())
            if (blob.cur != null && cell < blob.cur.length && blob.cur[cell] > 0) return false;
        return true;
    }

    public static int besideDownstairs(Level level) {
        for (LevelTransition transition : level.transitions) {
            if (transition.type != LevelTransition.Type.REGULAR_EXIT) continue;
            int stairs = transition.cell(), x = stairs % level.width(), y = stairs / level.width();
            for (int[] delta : new int[][]{{0,1},{-1,0},{1,0},{0,-1},{-1,1},{1,1},{-1,-1},{1,-1}}) {
                int nx = x + delta[0], ny = y + delta[1];
                if (nx < 0 || nx >= level.width() || ny < 0 || ny >= level.height()) continue;
                int cell = nx + ny * level.width();
                if (safe(level, cell)) return cell;
            }
        }
        return -1;
    }

    public static int outsideShop(Level level) {
        if (level instanceof CityBossLevel) {
            ShopRoom room = ((CityBossLevel)level).apdLootShop();
            if (room == null) return -1;
            // Imp's saved logical entrance lies inside its footprint. Use the
            // immediately adjoining exterior approach, below that entrance.
            Point door = room.entrance();
            int x = door.x, y = room.bottom + 1;
            return outside(level, room, x, y);
        }
        if (level instanceof RegularLevel) for (Room room : ((RegularLevel)level).rooms()) {
            if (!(room instanceof ShopRoom)) continue;
            Point door = ((ShopRoom)room).entrance();
            if (door == null) continue;
            int x = door.x, y = door.y;
            if (x == room.left) x--;
            else if (x == room.right) x++;
            else if (y == room.top) y--;
            else if (y == room.bottom) y++;
            else continue;
            int cell = outside(level, room, x, y);
            if (cell >= 0) return cell;
        }
        return -1;
    }

    private static int outside(Level level, Room room, int x, int y) {
        if (x < 0 || x >= level.width() || y < 0 || y >= level.height()
                || (x >= room.left && x <= room.right && y >= room.top && y <= room.bottom)) return -1;
        int cell = x + y * level.width();
        return safe(level, cell) ? cell : -1;
    }
}
