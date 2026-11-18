package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;
import java.util.*;

/** Run-owned pending deliveries. Old saves without this field remain empty. */
public final class LootTransitState {
    private final List<LootShipment> pending = new ArrayList<>();
    public List<LootShipment> pending() { return Collections.unmodifiableList(pending); }
    public void add(LootShipment shipment) {
        for (LootShipment old : pending) if (old.id().equals(shipment.id()))
            throw new IllegalArgumentException("Duplicate loot shipment");
        pending.add(shipment);
    }
    public void remove(LootShipment shipment) { pending.remove(shipment); }
    public void save(Bundle game) {
        if (pending.isEmpty()) return;
        Bundle bundle = new Bundle();
        bundle.put("version", 1); bundle.put("count", pending.size()); bundle.put("pending", pending);
        game.put("apd_loot_transit", bundle);
    }
    public static LootTransitState restore(Bundle game) {
        LootTransitState state = new LootTransitState();
        if (!game.contains("apd_loot_transit")) return state;
        Bundle b = game.getBundle("apd_loot_transit");
        if (!b.contains("version") || b.getInt("version") != 1
                || !b.contains("count") || !b.contains("pending") || b.getInt("count") < 1)
            throw new IllegalArgumentException("Invalid pending loot archive");
        for (Bundlable value : b.getCollection("pending")) {
            if (!(value instanceof LootShipment)) throw new IllegalArgumentException("Unknown pending loot");
            state.add((LootShipment)value);
        }
        if (state.pending.size() != b.getInt("count"))
            throw new IllegalArgumentException("Incomplete pending loot archive");
        return state;
    }
}
