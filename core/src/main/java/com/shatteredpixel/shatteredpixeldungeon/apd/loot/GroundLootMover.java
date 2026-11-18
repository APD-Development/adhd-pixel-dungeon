package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import java.util.*;

/** Exact object relocation, without merge/UID changes or pickup/generation events. */
public final class GroundLootMover {
    private GroundLootMover() {}

    public static List<Item> remove(Level level, GroundLootPlan plan) {
        if (!plan.belongsTo(level) || !plan.stillValid()) throw new IllegalStateException("Ground loot changed before transfer");
        List<Item> moved = new ArrayList<>();
        for (GroundLootPlan.Source source : plan.sources) {
            for (Item selected : source.items) {
                boolean removed = false;
                for (Iterator<Item> it = source.heap.items.iterator(); it.hasNext();) {
                    if (it.next() == selected) { it.remove(); removed = true; break; }
                }
                if (!removed) throw new IllegalStateException("Ground loot disappeared");
                moved.add(selected);
            }
            if (source.heap.items.isEmpty()) level.heaps.remove(source.cell);
        }
        return moved;
    }

    public static Heap place(Level level, int cell, List<Item> items) {
        if (!LootPlacement.safe(level, cell) || items == null || items.isEmpty())
            throw new IllegalStateException("No safe loot destination");
        Set<Item> identities = Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());
        for (Heap h : level.heaps.valueList()) for (Item item : h.items) identities.add(item);
        for (Item item : items) if (item == null || item.quantity() <= 0 || !identities.add(item))
            throw new IllegalStateException("Invalid or duplicated transported item");
        Heap heap = level.heaps.get(cell);
        if (heap == null) {
            heap = new Heap(); heap.pos = cell; heap.type = Heap.Type.HEAP;
            heap.seen = level.heroFOV != null && level.heroFOV[cell];
            level.heaps.put(cell, heap);
        }
        heap.items.addAll(items);
        return heap;
    }
}
