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

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Foresight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;


/** Chooser presentation only. Reading the context never starts or changes an action. */
public final class SearchControlContext {
    private SearchControlContext() { }

    public enum Action { SEARCH, WALL_SEARCH, RESUME_MOVEMENT, SHOW_SAVED, FORGET, CANCEL }

    // Reuse the chooser's fixed ordering instead of allocating an array on each open.
    private static final Action[] CHOICE_ORDER = {
            Action.SEARCH, Action.WALL_SEARCH, Action.SHOW_SAVED,
            Action.RESUME_MOVEMENT, Action.FORGET, Action.CANCEL
    };

    /** Malformed restored level data must not crash a read-only chooser or reveal hidden tiles. */
    private static boolean completeObservedMetadata(Level level) {
        if (level == null || level.width() <= 0 || level.height() <= 0) return false;
        int length = level.length();
        return level.map != null && level.map.length >= length
                && level.heroFOV != null && level.heroFOV.length >= length
                && level.visited != null && level.visited.length >= length
                && level.mapped != null && level.mapped.length >= length;
    }

    public static Action preferred(Hero hero, Level level) {
        if (SPDSettings.adaptiveSearchChoice() && hero != null
                && hero.wallSearch.matches(level) && hasMeaningfulWall(hero, level)) {
            return Action.WALL_SEARCH;
        }
        return hero != null && hero.lastAction != null ? Action.RESUME_MOVEMENT : Action.SEARCH;
    }

    public static Action[] choices(Hero hero, Level level) {
        Action first = preferred(hero, level);
        boolean saved = hero != null && hero.wallSearch.exists();
        boolean wallResume = saved && hero.wallSearch.matches(level) && hasMeaningfulWall(hero, level);
        boolean movement = hero != null && hero.lastAction != null;
        // R7 requires a current, meaningful wall context, not just saved memory.
        // Keep C485's one-array allocation and saved-position controls.
        // Allocate only the caller-owned result, not a list and its backing array.
        Action[] options = new Action[2 + (saved ? 2 : 0) + (wallResume ? 1 : 0) + (movement ? 1 : 0)];
        int next = 0;
        options[next++] = first;
        for (Action action : CHOICE_ORDER) {
            if (action == Action.WALL_SEARCH && !wallResume) continue;
            if ((action == Action.SHOW_SAVED || action == Action.FORGET) && !saved) continue;
            if (action == Action.RESUME_MOVEMENT && !movement) continue;
            if (action != first) options[next++] = action;
        }
        return options;
    }

    /** Legacy geometry query, no longer a wall-search eligibility gate.
     * Only observed cardinal walls can override the room-center rule; a distant
     * wall reached through Wide Search does not turn the whole room into wall mode.
     * This is read-only and never inspects hidden terrain behind a wall. */
    public static boolean openRoomInterior(Hero hero, Level level) {
        if (hero == null || !completeObservedMetadata(level)
                || hero.pos < 0 || hero.pos >= level.length()
                || !(level instanceof RegularLevel) || !((RegularLevel)level).apdHasRooms())
            return false;
        Room room = ((RegularLevel)level).room(hero.pos);
        if (room == null) return false;
        int width = level.width(), x = hero.pos % width, y = hero.pos / width;
        if (x < room.left + 2 || x > room.right - 2
                || y < room.top + 2 || y > room.bottom - 2) return false;
        // No temporary array in this frequently queried UI and Search path.
        // Keep the same north, south, west, east order and visibility checks.
        for (int direction=0; direction<4; direction++) {
            int neighbour=direction==0 ? hero.pos-width
                    : direction==1 ? hero.pos+width
                    : direction==2 ? hero.pos-1 : hero.pos+1;
            if (neighbour >= 0 && neighbour < level.length()
                    && level.heroFOV[neighbour]
                    && DoorSearchEligibility.candidate(level, neighbour)) return false;
        }
        return true;
    }

    /** Rooms whose walls can never hide a door from the inside.
     * Hidden doors only replace REGULAR doors between connected rooms. Every
     * special room (key/crystal vaults, wells, shops, altars, labs, gardens,
     * secret rooms, ...) has exactly one entrance, and the hero is standing
     * inside it, so that entrance is already known. Standard rooms and
     * hallways can still hide doors and stay eligible. */
    public static boolean noHiddenDoorRoom(Level level, int heroPos) {
        if (!(level instanceof RegularLevel) || !((RegularLevel) level).apdHasRooms()
                || heroPos < 0 || heroPos >= level.length()) return false;
        Room room = ((RegularLevel) level).room(heroPos);
        return room instanceof SpecialRoom;
    }

    /** Search also reveals hidden traps. A room the player can already see traps in is
     * worth a full search anywhere inside it. Decided only from traps already visible,
     * so the button never leaks whether anything is hidden. */
    /** The one rule every search control uses: a full search is worth its turns when a
     * door-capable wall is in reach, or the hero's room shows traps. */
    public static boolean fullSearchUseful(Hero hero, Level level) {
        return hasMeaningfulWall(hero, level) || visibleTrapRoom(hero, level) || roomHasSearchableWall(hero, level);
    }

    /** The hero's room still has a door-capable wall nobody has searched, anywhere on its edge.
     * Lets Search stay the tap action in the middle of a room, not only beside a wall. */
    public static boolean roomHasSearchableWall(Hero hero, Level level) {
        if (hero == null || !(level instanceof RegularLevel) || !((RegularLevel) level).apdHasRooms()
                || !completeObservedMetadata(level) || hero.pos < 0 || hero.pos >= level.length()
                || !DoorSearchEligibility.supportsDoors(level)) return false;
        if (noHiddenDoorRoom(level, hero.pos)) return false;
        Room room = ((RegularLevel) level).room(hero.pos);
        if (room == null) return false;
        int w = level.width();
        for (int x = room.left; x <= room.right; x++) {
            for (int y = room.top; y <= room.bottom; y++) {
                if (x != room.left && x != room.right && y != room.top && y != room.bottom) continue;
                if (x < 0 || y < 0 || x >= w || y >= level.height()) continue;
                int cell = x + y * w;
                if (DoorSearchEligibility.candidate(level, cell) && !level.searchedWalls.confirmed(level, cell)) return true;
            }
        }
        return false;
    }

    public static boolean visibleTrapRoom(Hero hero, Level level) {
        if (hero == null || !(level instanceof RegularLevel) || !((RegularLevel) level).apdHasRooms()
                || level.traps == null || hero.pos < 0 || hero.pos >= level.length()) return false;
        Room room = ((RegularLevel) level).room(hero.pos);
        if (room == null) return false;
        for (com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap trap : level.traps.valueList()) {
            if (trap != null && trap.visible && trap.active && room.inside(level.cellToPoint(trap.pos))) return true;
        }
        return false;
    }

    public static boolean hasMeaningfulWall(Hero hero, Level level) {
        if (hero == null || !completeObservedMetadata(level)
                || hero.pos < 0 || hero.pos >= level.length()
                || !DoorSearchEligibility.supportsDoors(level)) return false;
        // A gated special room is never an adaptive wall-search context.
        // Keep ordinary paid Search and discovered-door evidence unchanged.
        if (noHiddenDoorRoom(level, hero.pos)) return false;
        // Room middles are handled by reach: Wall Search is only meaningful when
        // a door-capable wall lies inside the hero's real search footprint
        // (1 tile, 2 for Rogue, +1 with Wide Search). Otherwise it is a normal Search.

        // Scan only the existing class/acquired-talent footprint rather than always
        // walking a Rogue rank-2 square. covers still enforces rounded corners,
        // visibility and Foresight's unchanged observed/mapped-terrain rules.
        boolean foresight = hero.buff(Foresight.class) != null;
        int radius = WallSearchSession.searchRadius(hero, foresight);
        int width = level.width();
        int x = hero.pos % width, y = hero.pos / width;
        for (int row = Math.max(0, y - radius); row <= Math.min(level.height() - 1, y + radius); row++) {
            for (int col = Math.max(0, x - radius); col <= Math.min(width - 1, x + radius); col++) {
                int cell = row * width + col;
                if (WallSearchSession.covers(hero, level, hero.pos, cell, foresight)
                        && DoorSearchEligibility.candidate(level, cell)
                        && !level.searchedWalls.confirmed(level, cell)) return true;
            }
        }
        return false;
    }
}
