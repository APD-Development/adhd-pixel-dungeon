package com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic;

import com.shatteredpixel.shatteredpixeldungeon.apd.loot.LootTransfers;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/** Approved provisional exotic: six-energy conversion and generation only through depth 19. */
public class ScrollOfForwarding extends ExoticScroll {
    { image = ItemSpriteSheet.SCROLL_KAUNAN + 16; icon = ItemSpriteSheet.Icons.SCROLL_TELEPORT; }
    @Override public boolean isKnown() { return true; }
    @Override public void reset() {
        // Match Item.reset without asking the twelve-rune handler about this independent prototype.
        keptThoughLostInvent = false; apdUid = null; apdCopyOf = null;
        image = ItemSpriteSheet.SCROLL_KAUNAN + 16; rune = "KAUNAN";
    }
    @Override public void setKnown() { }
    @Override public String name() { return "Scroll of Forwarding"; }
    @Override public String desc() {
        return "Sends loose ground items to a pile outside the next region's shop. "
                + "On Floor 20 or later, uses the Imp's Floor-20 shop. Nothing is sold. "
                + "If the shop is closed or blocked, the items wait for a safe delivery spot. "
                + "Leaves unopened containers, shop stock, carried items, keys, quest items and Lost Backpacks in place. "
                + "An armed bomb among the loot prevents casting without consuming the scroll.";
    }
    @Override public void doRead() {
        if (curUser == null || (!anonymous && !curUser.belongings.backpack.contains(this)) || quantity() < 1) return;
        String failure = LootTransfers.cast(true, () -> {
            if (!anonymous) detach(curUser.belongings.backpack);
            identify();
            readAnimation();
        });
        if (failure != null) GLog.w(failure);
        else GLog.i("Your loose loot has been forwarded to the destination shop.");
    }
}
