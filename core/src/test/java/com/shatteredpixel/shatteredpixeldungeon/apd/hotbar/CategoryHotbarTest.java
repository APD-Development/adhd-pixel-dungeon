package com.shatteredpixel.shatteredpixeldungeon.apd.hotbar;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.watabou.utils.Random;
import com.watabou.utils.GameSettings;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.CategoryHotbar.Category.*;

public class CategoryHotbarTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private Preferences old;
    private Field field;
    private File file;
    private static class Healing extends PotionOfHealing {
        boolean known;
        Healing(boolean known) { this.known = known; }
        @Override public boolean isKnown() { return known; }
    }
    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        field = GameSettings.class.getDeclaredField("prefs"); field.setAccessible(true);
        old = (Preferences) field.get(null);
        file = new File(temp.newFolder(), "preferences.xml");
        GameSettings.set(new HeadlessPreferences(new FileHandle(file)));
    }
    @After public void cleanup() { GameSettings.set(old); }
    @Test public void defaultsKeepClassicAndFoodOff() {
        assertFalse(CategoryHotbar.enabled()); assertFalse(SPDSettings.apdFoodInHealing());
    }
    @Test public void customizingOnePresetSlotPreservesOtherPositions() {
        SPDSettings.apdHotbarMode(3);
        CategoryHotbar.chooseCategory(5, SCROLLS);
        assertEquals(ABILITIES, CategoryHotbar.category(0));
        assertEquals(WANDS, CategoryHotbar.category(1));
        assertEquals(ARTIFACTS, CategoryHotbar.category(2));
        assertEquals(SCROLLS, CategoryHotbar.category(5));
    }
    @Test public void duplicateCategoriesAndEmptySlotsSurviveReopening() {
        CategoryHotbar.chooseCategory(0, POTIONS);
        CategoryHotbar.chooseCategory(1, POTIONS);
        CategoryHotbar.chooseCategory(5, EMPTY);
        SPDSettings.apdFoodInHealing(true);
        GameSettings.set(new HeadlessPreferences(new FileHandle(file)));
        assertEquals(POTIONS, CategoryHotbar.category(0));
        assertEquals(POTIONS, CategoryHotbar.category(1));
        assertEquals(EMPTY, CategoryHotbar.category(5));
        assertTrue(SPDSettings.apdFoodInHealing());
    }
    @Test public void hiddenHealingStaysInPotionsWithoutRevealingIdentity() {
        Healing unknown = new Healing(false);
        assertFalse(CategoryHotbar.matches(unknown, HEALING, false, true));
        assertTrue(CategoryHotbar.matches(unknown, POTIONS, false, true));
        assertFalse(CategoryHotbar.chooseItem(0, unknown));
        assertEquals(CategoryHotbar.CLASSIC, SPDSettings.apdHotbarMode());
    }
    @Test public void knownHealingMovesOnlyWhenHealingSlotExists() {
        Healing known = new Healing(true);
        assertTrue(CategoryHotbar.matches(known, HEALING, false, false));
        assertTrue(CategoryHotbar.matches(known, POTIONS, false, false));
        assertFalse(CategoryHotbar.matches(known, POTIONS, false, true));
        assertFalse(CategoryHotbar.matches(new Food(), HEALING, false, true));
        assertTrue(CategoryHotbar.matches(new Food(), HEALING, true, true));
    }
    @Test public void savedItemTypeAndCustomLayoutSurviveClassicMode() {
        assertTrue(CategoryHotbar.chooseItem(0, new Food()));
        CategoryHotbar.chooseCategory(1, POTIONS);
        String type = CategoryHotbar.itemType(0);
        SPDSettings.apdHotbarMode(0); assertEquals("", CategoryHotbar.itemType(0));
        SPDSettings.apdHotbarMode(4); assertEquals(type, CategoryHotbar.itemType(0));
        assertEquals(POTIONS, CategoryHotbar.category(1));
    }
    @Test(expected=IllegalArgumentException.class) public void invalidSlotRejected() {
        CategoryHotbar.chooseCategory(6, POTIONS);
    }
    @Test public void shieldingIsNotMisclassifiedAsHealing() {
        PotionOfShielding potion = new PotionOfShielding(); potion.anonymize();
        assertTrue(CategoryHotbar.matches(potion, POTIONS, false, true));
        assertFalse(CategoryHotbar.matches(potion, HEALING, false, true));
    }
    @Test public void inventoryRefreshAndSlotSelectionDoNotConsumeItemsOrRng() {
        Hero hero = new Hero(); Food food = new Food();
        hero.belongings.backpack.items.add(food);
        int hp = hero.HP, quantity = food.quantity();
        Random.pushGenerator(12345); long expected;
        try { expected = Random.Long(); } finally { Random.popGenerator(); }
        Random.pushGenerator(12345);
        try {
            CategoryHotbar.chooseCategory(0, FOOD);
            assertEquals(1, CategoryHotbar.items(hero, 0).size());
            assertSame(food, CategoryHotbar.items(hero, 0).get(0));
            assertTrue(CategoryHotbar.chooseItem(1, food));
            assertEquals(expected, Random.Long());
        } finally { Random.popGenerator(); }
        assertEquals(hp, hero.HP); assertEquals(quantity, food.quantity());
        hero.belongings.backpack.items.remove(food);
        assertTrue(CategoryHotbar.items(hero, 0).isEmpty());
        assertTrue(CategoryHotbar.items(hero, 1).isEmpty());
    }
    @Test public void categorySettingsNeverRewriteClassicQuickSlots() {
        QuickSlot old = Dungeon.quickslot;
        try {
            Dungeon.quickslot = new QuickSlot(); Food food = new Food();
            Dungeon.quickslot.setSlot(2, food);
            CategoryHotbar.chooseCategory(2, POTIONS);
            SPDSettings.apdHotbarMode(1); SPDSettings.apdHotbarMode(0);
            assertSame(food, Dungeon.quickslot.getItem(2));
        } finally { Dungeon.quickslot = old; }
    }
    @Test public void unavailableFutureItemTypePreferenceIsNotDeleted() {
        SPDSettings.apdHotbarMode(4);
        SPDSettings.apdHotbarItem(0, "future.community.Artifact");
        assertTrue(CategoryHotbar.items(new Hero(), 0).isEmpty());
        assertEquals("future.community.Artifact", CategoryHotbar.itemType(0));
    }
    @Test public void customItemShowsActualKnownNameAndFollowsInventoryRemoval() {
        Hero hero = new Hero(); Food food = new Food();
        hero.belongings.backpack.items.add(food);
        assertTrue(CategoryHotbar.chooseItem(0, food));
        assertSame(food, CategoryHotbar.representative(hero, 0));
        assertEquals(food.name(), CategoryHotbar.label(hero, 0));
        hero.belongings.backpack.items.remove(food);
        assertNull(CategoryHotbar.representative(hero, 0));
        assertEquals("Saved item type (unavailable)", CategoryHotbar.label(hero, 0));
    }
    @Test public void savedHealingTypeDoesNotExposeUnidentifiedPotionInNewRun() {
        Hero hero = new Hero(); Healing known = new Healing(true);
        assertTrue(CategoryHotbar.chooseItem(0, known));
        Healing hidden = new Healing(false); hero.belongings.backpack.items.add(hidden);
        assertNull(CategoryHotbar.representative(hero, 0));
        assertEquals("Saved item type (unavailable)", CategoryHotbar.label(hero, 0));
        CategoryHotbar.chooseCategory(1, POTIONS);
        assertSame(hidden, CategoryHotbar.items(hero, 1).get(0));
        assertFalse(hidden.isKnown());
    }
}
