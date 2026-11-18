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
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import java.util.Locale;
import java.util.Objects;

/** Item-owned, immutable location/source metadata. Never consults an item's hidden properties. */
public final class ItemOrigins {
    private ItemOrigins() { }
    public static final int PICKUP = 13, MIXED = 14, CRAFTED = 15;
    private static final String SAVE_KEY = "apd_item_origin_v1";

    public static final class Origin {
        public final int floor, branch, source;
        public final String room, generationRule;
        public Origin(int floor, int branch, int source, String room) { this(floor, branch, source, room, ""); }
        public Origin(int floor, int branch, int source, String room, String generationRule) {
            this.generationRule = generationRule == null ? "" : generationRule;
            this.floor = floor; this.branch = branch; this.source = source;
            this.room = room != null && room.matches("[A-Za-z]{1,80}") ? room : "";
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Origin)) return false;
            Origin o = (Origin) other;
            return floor == o.floor && branch == o.branch && source == o.source && room.equals(o.room);
        }
        @Override public int hashCode() { return Objects.hash(floor, branch, source, room); }
    }

    public static boolean eligible(Item item) { return item instanceof EquipableItem || item instanceof Wand; }

    public static String roomAt(Level level, int cell) {
        if (!(level instanceof RegularLevel) || cell < 0 || cell >= level.length()) return "";
        RegularLevel regular = (RegularLevel) level;
        // A partially built or synthetic level need not have rooms yet.
        if (!regular.apdHasRooms()) return "";
        Room room = regular.room(cell);
        return room == null ? "" : room.getClass().getSimpleName();
    }

    public static Origin at(Level level, int cell, int source) {
        return new Origin(Dungeon.depth, Dungeon.branch, source, roomAt(level, cell));
    }

    public static int container(Heap.Type type) {
        if (type == null) return GenerationEvents.C_HEAP;
        switch (type) {
            case FOR_SALE: return GenerationEvents.C_FOR_SALE;
            case CHEST: return GenerationEvents.C_CHEST;
            case LOCKED_CHEST: return GenerationEvents.C_LOCKED_CHEST;
            case CRYSTAL_CHEST: return GenerationEvents.C_CRYSTAL_CHEST;
            case TOMB: return GenerationEvents.C_TOMB;
            case SKELETON: return GenerationEvents.C_SKELETON;
            case REMAINS: return GenerationEvents.C_REMAINS;
            default: return GenerationEvents.C_HEAP;
        }
    }

    public static void record(Item item, Origin origin) {
        if (!eligible(item) || !valid(origin)) return;
        if (item.apdFoundOrigin == null || item.apdFoundOrigin.equals(origin)
                && item.apdFoundOrigin.generationRule.isEmpty() && !origin.generationRule.isEmpty()) item.apdFoundOrigin = origin;
        GenerationUpgradeRanges.recover(item, item.apdFoundOrigin);
    }

    /** Final heaps only: after painting/chest sealing, never on general Level.drop (which moves old loot too). */
    public static void captureLevel(Level level) {
        if (level == null || level.heaps == null) return;
        for (Heap heap : level.heaps.valueList()) {
            Origin origin = at(level, heap.pos, container(heap.type));
            for (Item item : heap.items) {
                GenerationUpgradeRanges.finalRoomSupport(item, origin);
                record(item, origin);
            }
        }
    }

    public static void recordSource(Item item, Level level, int cell, int source) {
        // A legacy object may already have been carried, dropped, sold or stolen. Only recorded evidence can recover its origin.
        if (eligible(item) && !item.apdOriginLegacyUnknown && item.apdFoundOrigin == null) record(item, at(level, cell, source));
    }

    public static Origin combine(Origin a, Origin b) {
        if (Objects.equals(a, b) && (a == null || a.generationRule.equals(b.generationRule))) return a;
        return a == null && b == null ? null : new Origin(0, 0, MIXED, "");
    }

    public static void merge(Item survivor, Item absorbed) {
        if (eligible(survivor)) {
            survivor.apdFoundOrigin = combine(survivor.apdFoundOrigin, absorbed.apdFoundOrigin);
            GenerationUpgradeRanges.merge(survivor, absorbed);
        }
    }

    public static void store(Item item, Bundle bundle) {
        Origin o = item.apdFoundOrigin;
        if (!eligible(item) || !valid(o)) return;
        Bundle saved = new Bundle();
        saved.put("floor", o.floor); saved.put("branch", o.branch);
        saved.put("source", o.source); saved.put("room", o.room);
        if (!o.generationRule.isEmpty()) saved.put("generation_rule", o.generationRule);
        bundle.put(SAVE_KEY, saved);
    }

    public static void restore(Item item, Bundle bundle) {
        item.apdFoundOrigin = null;
        item.apdOriginLegacyUnknown = !bundle.contains(SAVE_KEY);
        if (!eligible(item) || !bundle.contains(SAVE_KEY)) return;
        Bundle saved = bundle.getBundle(SAVE_KEY);
        if (!saved.contains("floor") || !saved.contains("branch") || !saved.contains("source")) return;
        Origin o = new Origin(saved.getInt("floor"), saved.getInt("branch"), saved.getInt("source"), saved.getString("room"), saved.getString("generation_rule"));
        if (valid(o)) item.apdFoundOrigin = o;
    }

    private static boolean valid(Origin o) {
        return o != null && o.branch >= 0 && o.source >= 0 && o.source <= CRAFTED
                && (o.source == MIXED || o.floor > 0);
    }

    /** A shared Item.info suffix reaches inventory, equipment and ordinary item inspections. */
    public static String describe(Item item) {
        if (!eligible(item) || item.isIdentified() || !valid(item.apdFoundOrigin)) return "";
        Origin o = item.apdFoundOrigin;
        if (o.source == MIXED) return "\n\n" + text("mixed") + GenerationUpgradeRanges.describe(item);
        String source;
        switch (o.source) {
            case GenerationEvents.C_CHEST: source = text("chest"); break;
            case GenerationEvents.C_LOCKED_CHEST: source = text("golden_chest"); break;
            case GenerationEvents.C_CRYSTAL_CHEST: source = text("crystal_chest"); break;
            case GenerationEvents.C_TOMB: source = text("tomb"); break;
            case GenerationEvents.C_SKELETON: source = text("skeleton"); break;
            case GenerationEvents.C_REMAINS: source = text("remains"); break;
            case GenerationEvents.C_FOR_SALE: source = text("shop"); break;
            case GenerationEvents.C_MIMIC: source = text("mimic"); break;
            case GenerationEvents.C_STATUE: source = text("statue"); break;
            case GenerationEvents.C_QUEST_REWARD: source = text("quest"); break;
            case GenerationEvents.C_MOB_LOOT: source = text("monster"); break;
            case GenerationEvents.C_BONUS_DROP: source = text("bonus"); break;
            case CRAFTED: source = text("crafted"); break;
            default: source = "";
        }
        String room = roomName(o.room);
        String detail = source.isEmpty() ? room : room.isEmpty() ? source : text("source_room", source, room);
        String location = text(o.branch == 0 ? "floor" : "branch_floor", o.floor, o.branch);
        return "\n\n" + text(o.source == PICKUP ? "picked_up" : "found", location, detail.isEmpty() ? "" : ", " + detail) + GenerationUpgradeRanges.describe(item);
    }

    public static String roomName(String room) {
        if (room == null || room.isEmpty()) return "";
        String label = text("room_" + room.toLowerCase(Locale.ROOT));
        return Messages.NO_TEXT_FOUND.equals(label) ? "" : label;
    }

    private static String text(String key, Object... args) { return Messages.get(ItemOrigins.class, key, args); }
}
