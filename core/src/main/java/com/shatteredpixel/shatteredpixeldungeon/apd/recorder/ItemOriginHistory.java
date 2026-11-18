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
package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.GenerationUpgradeRanges;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins.Origin;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import java.util.HashMap;
import java.util.Map;

/** Only location/source facts, joined to the same UID's PUBLIC observation. No hidden item truth reaches UI. */
final class ItemOriginHistory {
    final Map<String, Origin> origins = new HashMap<>();
    private final Map<String, GenerationEvents.ItemGenerated> generated = new HashMap<>();
    private final java.util.Set<String> acquiredBefore = new java.util.HashSet<>();

    void accept(ApdEvent event, ReadResult log) {
        switch (event.type) {
            case EventTypes.ITEM_GENERATED: {
                GenerationEvents.ItemGenerated g = GenerationEvents.decodeItemGenerated(event, log);
                // Retain a minimal geometry/source projection, not hidden type/curse/upgrade/enchantment fields.
                GenerationEvents.ItemGenerated location = new GenerationEvents.ItemGenerated();
                location.uid = g.uid; location.where = g.where; location.pos = g.pos;
                location.room = g.room; location.container = g.container;
                location.owner = GenerationUpgradeRanges.historicalRule(g);
                generated.putIfAbsent(g.uid, location);
                break;
            }
            case EventTypes.ITEM_SEEN: {
                ItemEvents.Seen seen = ItemEvents.decodeSeen(event, log);
                GenerationEvents.ItemGenerated g = generated.get(seen.uid);
                if (g != null && seen.where.depth == g.where.depth && seen.where.branch == g.where.branch
                        && seen.where.pos == g.pos) {
                    origins.putIfAbsent(seen.uid, new Origin(g.where.depth, g.where.branch, g.container, g.room, g.owner));
                }
                break;
            }
            case EventTypes.ITEM_ACQUIRED: {
                ItemEvents.Acquired a = ItemEvents.decodeAcquired(event, log);
                boolean firstReceipt = acquiredBefore.add(a.uid);
                GenerationEvents.ItemGenerated g = generated.get(a.uid);
                if (a.context != ItemEvents.ACQ_START_KIT && a.context != ItemEvents.ACQ_RECONCILED
                        && g != null && a.where.depth == g.where.depth && a.where.branch == g.where.branch && a.where.pos == g.pos) {
                    origins.putIfAbsent(a.uid, new Origin(g.where.depth, g.where.branch, g.container, g.room, g.owner));
                }
                if (g != null && a.context != ItemEvents.ACQ_RECONCILED && a.context != ItemEvents.ACQ_START_KIT
                        && a.where.depth == g.where.depth && a.where.branch == g.where.branch
                        && (g.container == GenerationEvents.C_QUEST_REWARD || g.container == GenerationEvents.C_MOB_LOOT
                        || g.container == GenerationEvents.C_MIMIC || g.container == GenerationEvents.C_STATUE)) {
                    // Receipt proves the same item's source, but not the giver's old room after they moved.
                    origins.putIfAbsent(a.uid, new Origin(g.where.depth, g.where.branch, g.container, "", g.owner));
                }
                if (firstReceipt && (a.context == ItemEvents.ACQ_SHOP || a.context == ItemEvents.ACQ_PICKUP)) {
                    origins.putIfAbsent(a.uid, new Origin(a.where.depth, a.where.branch,
                            a.context == ItemEvents.ACQ_SHOP ? GenerationEvents.C_FOR_SALE : ItemOrigins.PICKUP, ""));
                }
                break;
            }
            case EventTypes.ITEM_LINEAGE: {
                ItemEvents.Lineage l = ItemEvents.decodeLineage(event, log);
                Origin origin = origins.get(l.to);
                if (l.kind == ItemEvents.LIN_MERGE) {
                    for (String uid : l.from) origin = ItemOrigins.combine(origin, origins.get(uid));
                } else if (l.kind == ItemEvents.LIN_SPLIT || l.kind == ItemEvents.LIN_COPY || l.kind == ItemEvents.LIN_TRANSFORM) {
                    if (l.from.size() == 1) origin = origins.get(l.from.get(0));
                } else if (l.kind == ItemEvents.LIN_CRAFT) {
                    origin = new Origin(l.where.depth, l.where.branch, ItemOrigins.CRAFTED, "");
                }
                if (origin != null) origins.put(l.to, origin);
                break;
            }
            default: break;
        }
    }

    void finishReplay() { generated.clear(); }
}
