package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.apd.loot.LootTransfers;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/** Working-name prototype; test-build acquisition uses approved provisional Teleportation weights. */
public class ScrollOfGathering extends Scroll {
    { icon = -1; }
    @Override public void reset() {
        super.reset();
        // A fixed, always-known scroll: never borrow a randomized vanilla rune.
        image = ItemSpriteSheet.SCROLL_GATHERING;
    }
    @Override public boolean isKnown() { return true; }
    @Override public void setKnown() { }
    @Override public String name() { return "Scroll of Gathering"; }
    @Override public String desc() {
        return "Gathers loose ground items on this floor into one pile beside the downstairs. "
                + "Leaves unopened containers, shop stock, keys, quest items and lost backpacks untouched. "
                + "If there is no safe destination, the scroll is not consumed.";
    }
    protected boolean sendsAhead() { return false; }
    @Override public void doRead() {
        if (curUser == null || (!anonymous && !curUser.belongings.backpack.contains(this)) || quantity() < 1) return;
        String failure = LootTransfers.cast(sendsAhead(), () -> {
            if (!anonymous) detach(curUser.belongings.backpack);
            identify();
            readAnimation();
        });
        if (failure != null) GLog.w(failure);
        else GLog.i(sendsAhead() ? "Your loose loot is bound for the shop outside the next region."
                : "Your loose loot gathers beside the downstairs.");
    }
}
