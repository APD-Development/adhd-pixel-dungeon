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
package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdItems;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;

/** Backpack full: pick an item to drop in exchange for the one underfoot.
 * Costs exactly what dropping then picking up by hand costs (drop + pickup time).
 * Only the main backpack; items that belong in a full bag keep the old behavior. */
public final class FullPackSwap {
    private FullPackSwap() { }

    public static final float DROP_TIME = 1f;

    /** True when the pickup failed only because the main backpack has no free slot. */
    public static boolean applies(Hero hero, Heap heap, Item item) {
        if (hero == null || heap == null || item == null || heap.type != Heap.Type.HEAP) return false;
        if (heap.pos != hero.pos || heap.peek() != item) return false;
        if (hero.belongings.lostInventory()) return false;
        Bag pack = hero.belongings.backpack;
        if (pack.canHold(item)) return false;
        for (Item i : pack.items) {
            if (i instanceof Bag && ((Bag) i).canHold(item)) return false;
        }
        return true;
    }

    public static void offer(final Hero hero, final Heap heap, final Item item) {
        if (!applies(hero, heap, item)) return;
        //pickup runs on the game's actor thread; windows must be built on the render thread
        com.watabou.noosa.Game.runOnRenderThread(new com.watabou.utils.Callback() {
            @Override public void call() {
                if (applies(hero, heap, item)) show(hero, heap, item);
            }
        });
    }

    private static void show(final Hero hero, final Heap heap, final Item item) {
        final Bag pack = hero.belongings.backpack;
        GameScene.selectItem(new WndBag.ItemSelector() {
            @Override public String textPrompt() {
                return "Drop what to take " + Messages.titleCase(item.name()) + "?";
            }
            @Override public Class<? extends Bag> preferredBag() {
                return pack.getClass();
            }
            @Override public boolean itemSelectable(Item i) {
                return pack.items.contains(i) && !(i instanceof Bag)
                        && i.actions(hero).contains(Item.AC_DROP);
            }
            @Override public void onSelect(Item chosen) {
                swap(hero, heap, item, chosen);
            }
        });
    }

    static boolean swap(Hero hero, Heap heap, Item item, Item chosen) {
        if (chosen == null || hero != Dungeon.hero || !hero.isAlive() || !hero.ready) return false;
        Bag pack = hero.belongings.backpack;
        if (Dungeon.level.heaps.get(hero.pos) != heap || heap.peek() != item
                || !pack.items.contains(chosen) || chosen instanceof Bag) return false;

        ApdItems.noteAction(chosen, ApdItems.ACTION_DROP);
        Item dropped = chosen.detachAll(pack);
        //drop time first, then the normal pickup (which spends its own pickup time)
        hero.spend(DROP_TIME);
        ApdItems.noteAcquireContext(com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.ACQ_PICKUP);
        if (!item.doPickUp(hero)) {
            //should not happen; put everything back exactly as it was
            hero.spend(-DROP_TIME);
            dropped.collect(pack);
            return false;
        }
        heap.pickUp();
        Dungeon.level.drop(dropped, hero.pos).sprite.drop(hero.pos);
        GLog.i(Messages.capitalize(Messages.get(Hero.class, "you_now_have", item.name())));
        return true;
    }
}
