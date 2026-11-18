package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

/** Immutable allocation for non-neutral rules. Neutral generation must use Dungeon.souNeeded unchanged. */
public final class UpgradeScrollPlan {
    public static final int MIN = 1, NEUTRAL = 15, MAX = 60;
    private final int count;
    public UpgradeScrollPlan(int count) {
        if (count < MIN || count > MAX) throw new IllegalArgumentException("Upgrade scroll count outside 1..60");
        this.count = count;
    }
    public int count() { return count; }
    public double percentage() { return count * 100.0 / NEUTRAL; }
    public boolean usesVanillaGeneration() { return count == NEUTRAL; }
    /** Balanced allocation: regional counts differ by at most one. No RNG draws. */
    public int regionCount(int region) {
        if (region < 0 || region >= 5) throw new IllegalArgumentException("Region outside 0..4");
        return count * (region + 1) / 5 - count * region / 5;
    }
    /** Non-boss floors in the five main regions, with no new RNG or hidden placements. */
    public int floorCount(int depth) {
        if (depth<1 || depth>24 || depth%5==0) return 0;
        int region=(depth-1)/5, index=(depth-1)%5;
        int regional=regionCount(region);
        return regional*(index+1)/4-regional*index/4;
    }
    /** Count after existing Forbidden Runes removal, whose counter spans regions. */
    public int countAfterForbiddenRunes() { return (count + 1) / 2; }
}
