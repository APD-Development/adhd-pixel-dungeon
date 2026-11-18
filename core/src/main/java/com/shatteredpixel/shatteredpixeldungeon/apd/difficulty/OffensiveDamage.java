package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;

/** A single damage amount with explicit scaling state. Keeps the original source for defenses/death logic. */
public final class OffensiveDamage {
    public final int amount;
    public final Object source;
    public final OffensiveDamageOrigin origin;
    public final boolean scaled;

    private OffensiveDamage(int amount, Object source, OffensiveDamageOrigin origin, boolean scaled) {
        if (source == null || origin == null) throw new IllegalArgumentException("Damage needs source and origin");
        this.amount=amount; this.source=source; this.origin=origin; this.scaled=scaled;
    }

    public static OffensiveDamage raw(int amount, Object source, OffensiveDamageOrigin origin) {
        return new OffensiveDamage(amount,source,origin,false);
    }

    /** Already-reduced attacks, shared damage and derived damage must not apply the factor again. */
    public static OffensiveDamage alreadyScaled(int amount, Object source, OffensiveDamageOrigin origin) {
        return new OffensiveDamage(amount,source,origin,true);
    }

    public OffensiveDamage scaled() {
        if (scaled) return this;
        int percent=origin.percentage();
        int value=amount;
        // Negative amounts are existing engine sentinels. Neutral does no rounding and consumes no RNG.
        if (percent!=100 && amount>0)
            value=(int)Math.min(Integer.MAX_VALUE,Math.round((double)amount*percent/100.0));
        return new OffensiveDamage(value,source,origin,true);
    }

    /** Dispatch through the original virtual method after scaling, retaining all target-specific defenses. */
    public void apply(Char target) { target.damage(scaled().amount,source); }
}
