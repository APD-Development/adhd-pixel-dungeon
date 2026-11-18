package com.shatteredpixel.shatteredpixeldungeon.apd.hotbar;

import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

/** Repositions a Waterskin shortcut without losing other assignments or altering gameplay. */
public final class WaterskinStartupSlot {
    public static final int AUTOMATIC = -1;
    public static final int OFF = QuickSlot.MOBILE_VISIBLE_SLOTS;

    private WaterskinStartupSlot() {}


    public static String label(int position) {
        if (position == AUTOMATIC) return "Automatic";
        if (position == OFF) return "Off";
        if (position >= 0 && position < QuickSlot.MOBILE_VISIBLE_SLOTS) {
            return "Slot " + (position + 1);
        }
        throw new IllegalArgumentException("waterskin slot");
    }

    public static void assign(QuickSlot quickslot, Item waterskin, int position, boolean legacyAuto) {
        if (quickslot == null || waterskin == null) return;
        int limit = Math.min(quickslot.capacity(), QuickSlot.MOBILE_VISIBLE_SLOTS);
        if (position == OFF) return;
        if (position == AUTOMATIC) {
            if (!legacyAuto) return;
            for (int i = 0; i < limit; i++) {
                if (quickslot.getItem(i) == null) {
                    quickslot.setSlot(i, waterskin);
                    return;
                }
            }
            return;
        }
        if (position < 0 || position >= limit) return;

        Item displaced = quickslot.getItem(position);
        if (displaced == waterskin) return;
        int previous = quickslot.getSlot(waterskin);
        if (displaced != null) {
            int destination = previous;
            if (destination < 0) {
                for (int i = 0; i < limit; i++) {
                    if (quickslot.getItem(i) == null) {
                        destination = i;
                        break;
                    }
                }
            }
            // Swap if the bar is full and Waterskin already had a slot.
            // Otherwise leave everything untouched rather than deleting a shortcut.
            if (destination < 0) return;
            quickslot.setSlot(destination, displaced);
        }
        quickslot.setSlot(position, waterskin);
    }
}
