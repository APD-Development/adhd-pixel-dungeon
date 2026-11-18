package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import java.util.HashSet;
import java.util.Set;

// Explicit seed lists let recovery preserve completed episodes instead of rerunning them.
public final class SimulationSeedPlan {
    private SimulationSeedPlan() {}
    public static long[] parse(String explicit, long first, int count) {
        if (count < 1 || count > 100) throw new IllegalArgumentException("episode count must be 1–100");
        long[] seeds = new long[count];
        if (explicit == null || explicit.trim().isEmpty()) {
            for (int i = 0; i < count; i++) seeds[i] = Math.addExact(first, i);
            return seeds;
        }
        String[] values = explicit.split(",", -1);
        if (values.length != count) throw new IllegalArgumentException("explicit seed count mismatch");
        Set<Long> unique = new HashSet<>();
        for (int i = 0; i < count; i++) {
            seeds[i] = Long.parseLong(values[i].trim());
            if (!unique.add(seeds[i])) throw new IllegalArgumentException("duplicate explicit seed");
        }
        return seeds;
    }
}
