package com.shatteredpixel.shatteredpixeldungeon.apd.hotbar;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import java.util.ArrayList;
import java.util.List;

//Preferences describe slots; the original per-run QuickSlot assignments are never rewritten.
public final class CategoryHotbar {
    private CategoryHotbar() {}
    public static final int CLASSIC = 0, CUSTOM = 4, SLOTS = 6;
    public enum Category {
        HEALING("Healing", "Heal"), POTIONS("All potions", "Pot"),
        THROWABLES("Throwing weapons", "Throw"), WANDS("Wands", "Wand"),
        ARTIFACTS("Active artifacts", "Art"), ABILITIES("Abilities", "Skill"),
        FOOD("Food", "Food"), SCROLLS("Scrolls", "Scroll"), EMPTY("Empty", "-");
        public final String label, shortLabel;
        Category(String label, String shortLabel) { this.label = label; this.shortLabel = shortLabel; }
    }
    public static final String[] MODES = {"Classic SPD", "Categories", "Consumables first", "Abilities first", "Custom"};
    private static final int[][] PRESETS = {{0,1,2,3,4,5}, {0,1,2,3,4,5}, {0,6,1,7,2,3}, {5,3,4,0,1,2}};
    public static boolean enabled() { return SPDSettings.apdHotbarMode() != CLASSIC; }
    private static void slot(int value) {
        if (value < 0 || value >= SLOTS) throw new IllegalArgumentException("hotbar slot");
    }
    public static Category category(int index) {
        slot(index);
        int mode = SPDSettings.apdHotbarMode();
        return Category.values()[mode == CUSTOM ? SPDSettings.apdHotbarCategory(index) : PRESETS[mode][index]];
    }
    public static String itemType(int index) {
        slot(index);
        return SPDSettings.apdHotbarMode() == CUSTOM ? SPDSettings.apdHotbarItem(index) : "";
    }
    public static Item representative(Hero hero, int index) {
        if (itemType(index).isEmpty()) return null;
        List<Item> available = items(hero, index);
        return available.isEmpty() ? null : available.get(0);
    }
    public static String label(int index) {
        return label(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero, index);
    }
    public static String label(Hero hero, int index) {
        if (itemType(index).isEmpty()) return category(index).label;
        Item available = representative(hero, index);
        return available == null ? "Saved item type (unavailable)" : available.name();
    }
    public static String shortLabel(int index) { return itemType(index).isEmpty() ? category(index).shortLabel : "Item"; }
    public static void chooseCategory(int index, Category category) {
        slot(index); preservePreset(); SPDSettings.apdHotbarCategory(index, category.ordinal());
        SPDSettings.apdHotbarItem(index, ""); SPDSettings.apdHotbarMode(CUSTOM);
    }
    public static boolean identityKnown(Item item) {
        if (item instanceof Potion) return ((Potion)item).isKnown();
        if (item instanceof Scroll) return ((Scroll)item).isKnown();
        if (item instanceof Ring) return ((Ring)item).isKnown();
        return item != null;
    }
    public static boolean chooseItem(int index, Item item) {
        slot(index);
        if (!identityKnown(item)) return false;
        preservePreset();
        SPDSettings.apdHotbarItem(index, item.getClass().getName());
        SPDSettings.apdHotbarMode(CUSTOM);
        return true;
    }
    private static void preservePreset() {
        int mode = SPDSettings.apdHotbarMode();
        if (mode == CLASSIC || mode == CUSTOM) return;
        for (int i = 0; i < SLOTS; i++) {
            SPDSettings.apdHotbarCategory(i, PRESETS[mode][i]);
            SPDSettings.apdHotbarItem(i, "");
        }
    }
    public static boolean hasHealingSlot() {
        if (!enabled()) return false;
        for (int i = 0; i < SLOTS; i++) {
            String type = itemType(i);
            if (type.equals(PotionOfHealing.class.getName()) || type.isEmpty() && category(i) == Category.HEALING) return true;
        }
        return false;
    }
    public static boolean matches(Item item, Category category, boolean foodInHealing, boolean healingSlot) {
        if (item == null) return false;
        //Unknown potions stay in Potions: no classification from hidden item identity.
        boolean healing = item instanceof PotionOfHealing && ((Potion)item).isKnown();
        switch (category) {
            case HEALING: return healing || foodInHealing && item instanceof Food;
            case POTIONS: return item instanceof Potion && (!healing || !healingSlot);
            case THROWABLES: return item instanceof MissileWeapon;
            case WANDS: return item instanceof Wand;
            case ARTIFACTS: return item instanceof Artifact && item.defaultAction() != null;
            case FOOD: return item instanceof Food;
            case SCROLLS: return item instanceof Scroll;
            default: return false;
        }
    }
    public static List<Item> items(Hero hero, int index) {
        slot(index);
        List<Item> result = new ArrayList<>();
        if (hero == null) return result;
        String type = itemType(index);
        boolean healingSlot = hasHealingSlot();
        for (Item item : hero.belongings) {
            if (hero.belongings.lostInventory() && !item.keptThroughLostInventory()) continue;
            boolean matches = type.isEmpty()
                    ? matches(item, category(index), SPDSettings.apdFoodInHealing(), healingSlot)
                    : identityKnown(item) && item.getClass().getName().equals(type);
            if (matches) result.add(item);
        }
        return result;
    }
}

