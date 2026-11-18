package com.shatteredpixel.shatteredpixeldungeon.apd.history;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** Resolves existing item-sheet symbols without creating or identifying game items. */
public final class ApdSpriteRef {
    private ApdSpriteRef() {}
    public static Integer itemImage(String reference) {
        if (reference == null || !reference.startsWith("ITEM_")) return null;
        try {
            return ItemSpriteSheet.class.getField(reference.substring(5)).getInt(null);
        } catch (ReflectiveOperationException | IllegalArgumentException invalid) {
            return null;
        }
    }
}
