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
package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.CategoryHotbar;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar;
import java.util.ArrayList;
import java.util.List;

public final class WndCategoryHotbar {
    private WndCategoryHotbar() {}
    private static void show(com.shatteredpixel.shatteredpixeldungeon.ui.Window window) {
        ShatteredPixelDungeon.scene().addToFront(window);
    }
    private static void refresh() { QuickSlotButton.cancel(); Toolbar.updateLayout(); QuickSlotButton.refresh(); }
    public static void slots() {
        List<WndUimsdList.Entry> entries = new ArrayList<>();
        for (int i=0; i<CategoryHotbar.SLOTS; i++)
            entries.add(WndUimsdList.Entry.item("Slot " + (i+1) + ": " + CategoryHotbar.label(i)));
        show(WndUimsdList.menu("Custom hotbar", "Arrange each position independently. Repeated categories are allowed. Long-press a hotbar position to change it.",
                entries, (slot, checked) -> configure(slot)).restoreWith(WndCategoryHotbar::slots));
    }
    public static void configure(int slot) {
        List<WndUimsdList.Entry> entries = new ArrayList<>();
        for (CategoryHotbar.Category category : CategoryHotbar.Category.values())
            entries.add(WndUimsdList.Entry.item(category.label));
        entries.add(WndUimsdList.Entry.item("Specific item type"));
        show(WndUimsdList.menu("Hotbar slot " + (slot+1), "Choose a category or a carried item. Selecting a layout or item never uses it.",
                entries, (choice, checked) -> {
                    if (choice==CategoryHotbar.Category.values().length) pickItem(slot);
                    else { CategoryHotbar.chooseCategory(slot, CategoryHotbar.Category.values()[choice]); refresh(); slots(); }
                }).restoreWith(() -> configure(slot)));
    }
    private static void pickItem(int slot) {
        if (Dungeon.hero==null || !Dungeon.hero.isAlive() || !Dungeon.hero.ready) {
            show(new WndUimsdText("Hotbar", "Choose a specific item during a run.", false)); return;
        }
        List<Item> items = new ArrayList<>();
        List<WndUimsdList.Entry> entries = new ArrayList<>();
        for (Item item : Dungeon.hero.belongings) {
            if (CategoryHotbar.identityKnown(item) && (!Dungeon.hero.belongings.lostInventory() || item.keptThroughLostInventory())) {
                items.add(item); entries.add(WndUimsdList.Entry.item(item.name()));
            }
        }
        if (items.isEmpty()) { show(new WndUimsdText("Hotbar", "No known item types are available.", false)); return; }
        show(WndUimsdList.menu("Choose an item type",
                "This type stays saved between runs. Unknown potions, scrolls and rings remain available through their normal inventory or category until identified.",
                entries, (index, checked) -> {
                    Item item=items.get(index);
                    if (Dungeon.hero!=null && Dungeon.hero.belongings.contains(item) && CategoryHotbar.chooseItem(slot,item)) {
                        refresh(); slots();
                    }
                }).restoreWith(() -> pickItem(slot)));
    }
    public static void assign(Item item) {
        if (Dungeon.hero==null || !Dungeon.hero.isAlive() || !Dungeon.hero.ready || !Dungeon.hero.belongings.contains(item)) return;
        if (!CategoryHotbar.identityKnown(item)) {
            show(new WndUimsdText("Hotbar", "Identify this item before saving its type. Unknown potions still appear in Potions.", false)); return;
        }
        List<WndUimsdList.Entry> entries = new ArrayList<>();
        for (int i=0; i<CategoryHotbar.SLOTS; i++) entries.add(WndUimsdList.Entry.item("Slot " + (i+1)));
        show(WndUimsdList.menu("Save item to custom hotbar", "Choose its position. Classic SPD assignments are preserved.",
                entries, (slot, checked) -> {
                    if (Dungeon.hero!=null && Dungeon.hero.isAlive() && Dungeon.hero.ready
                            && Dungeon.hero.belongings.contains(item) && CategoryHotbar.chooseItem(slot,item)) refresh();
                }).restoreWith(() -> assign(item)));
    }
    public static void open(int slot) {
        if (Dungeon.hero==null || !Dungeon.hero.isAlive() || !Dungeon.hero.ready) return;
        if (CategoryHotbar.itemType(slot).isEmpty() && CategoryHotbar.category(slot)==CategoryHotbar.Category.ABILITIES) {
            abilities(); return;
        }
        List<Item> items=CategoryHotbar.items(Dungeon.hero,slot);
        if (items.isEmpty()) { show(new WndUimsdText("Hotbar", "No available items in this hotbar position.", false)); return; }
        com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing direct =
                com.shatteredpixel.shatteredpixeldungeon.apd.healing.HealingConfirmation.directQuickslotPotion(
                        items, SPDSettings.healingConfirmationThreshold());
        if (direct != null) {
            direct.execute(Dungeon.hero, com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.AC_DRINK);
            return;
        }
        List<WndUimsdList.Entry> entries = new ArrayList<>();
        for (Item item : items) entries.add(WndUimsdList.Entry.item(item.name(), "Quantity: " + item.quantity()));
        show(WndUimsdList.menu(CategoryHotbar.label(slot), "Choose an item, then its normal action. Opening this list spends no turn.",
                entries, (index, checked) -> {
                    Item item=items.get(index);
                    if (Dungeon.hero!=null && Dungeon.hero.isAlive() && Dungeon.hero.ready && Dungeon.hero.belongings.contains(item)) {
                        if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing
                                && com.shatteredpixel.shatteredpixeldungeon.apd.healing.HealingConfirmation.categoryDirectDrink(
                                    ((com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing)item).isKnown(),
                                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.healingConfirmationThreshold())) {
                            item.execute(Dungeon.hero, com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.AC_DRINK);
                        } else {
                            show(new WndUseItem(null,item));
                        }
                    }
                }).restoreWith(() -> open(slot)));
    }
    private static void abilities() {
        if (Dungeon.hero==null || !Dungeon.hero.isAlive() || !Dungeon.hero.ready) return;
        List<WndUimsdList.Entry> entries = new ArrayList<>();
        entries.add(WndUimsdList.Entry.item("Class talents and passive abilities"));
        boolean armor=Dungeon.hero.belongings.armor!=null && Dungeon.hero.armorAbility!=null;
        HolyTome tome=Dungeon.hero.belongings.getItem(HolyTome.class);
        if (armor) entries.add(WndUimsdList.Entry.item("Hero armor ability"));
        if (tome!=null) entries.add(WndUimsdList.Entry.item("Cleric spells"));
        show(WndUimsdList.menu("Abilities", "Passive abilities are information. Active abilities keep their normal costs and targeting.",
                entries, (index, checked) -> {
                    if (Dungeon.hero==null || !Dungeon.hero.isAlive() || !Dungeon.hero.ready) return;
                    if (index==0) show(new WndHero());
                    else if (armor && index==1 && Dungeon.hero.belongings.armor!=null)
                        show(new WndUseItem(null,Dungeon.hero.belongings.armor));
                    else if (tome!=null && Dungeon.hero.belongings.contains(tome))
                        tome.execute(Dungeon.hero,HolyTome.AC_CAST);
                }).restoreWith(WndCategoryHotbar::abilities));
    }
}


