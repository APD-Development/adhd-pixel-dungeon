package com.shatteredpixel.shatteredpixeldungeon.apd.hotbar;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;
import com.watabou.utils.GameSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

public class WaterskinStartupSlotTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private Preferences old;
    private File path;

    @Before public void setUp() throws Exception {
        Field f = GameSettings.class.getDeclaredField("prefs");
        f.setAccessible(true);
        old = (Preferences)f.get(null);
        path = new File(temp.getRoot(), "settings.xml");
        GameSettings.set(new HeadlessPreferences(new FileHandle(path)));
    }

    @After public void tearDown() { GameSettings.set(old); }

    @Test public void absentPreferenceDefaultsToSlotTwoAndPreservesLegacyAutomatic() {
        assertEquals(1, SPDSettings.waterskinStartingSlot());
        QuickSlot slots = new QuickSlot(6);
        Item classItem = new Item();
        Waterskin skin = new Waterskin();
        slots.setSlot(0, classItem);
        WaterskinStartupSlot.assign(slots, skin, SPDSettings.waterskinStartingSlot(), true);
        assertSame(classItem, slots.getItem(0));
        assertSame(skin, slots.getItem(1));
        QuickSlot optedOut = new QuickSlot(6);
        WaterskinStartupSlot.assign(optedOut, skin, SPDSettings.waterskinStartingSlot(), false);
        assertEquals(1, optedOut.getSlot(skin));
        SPDSettings.waterskinStartingSlot(WaterskinStartupSlot.AUTOMATIC);
        QuickSlot legacy=new QuickSlot(6);
        WaterskinStartupSlot.assign(legacy,skin,SPDSettings.waterskinStartingSlot(),false);
        assertEquals(-1,legacy.getSlot(skin));
    }

    @Test public void selectedSlotPersistsAndOverridesLegacyAutomaticOptOut() {
        SPDSettings.quickslotWaterskin(false);
        for (int slot = 0; slot < 6; slot++) {
            SPDSettings.waterskinStartingSlot(slot);
            GameSettings.set(new HeadlessPreferences(new FileHandle(path)));
            assertEquals(slot, SPDSettings.waterskinStartingSlot());
            QuickSlot q = new QuickSlot(6);
            Waterskin skin = new Waterskin();
            WaterskinStartupSlot.assign(q, skin, SPDSettings.waterskinStartingSlot(),
                    SPDSettings.quickslotWaterskin());
            assertSame(skin, q.getItem(slot));
            assertEquals(slot, q.getSlot(skin));
        }
        SPDSettings.waterskinStartingSlot(WaterskinStartupSlot.OFF);
        GameSettings.set(new HeadlessPreferences(new FileHandle(path)));
        assertEquals(WaterskinStartupSlot.OFF, SPDSettings.waterskinStartingSlot());
        QuickSlot off = new QuickSlot(6);
        Waterskin skin = new Waterskin();
        WaterskinStartupSlot.assign(off, skin, SPDSettings.waterskinStartingSlot(), true);
        assertEquals(-1, off.getSlot(skin));
    }

    @Test public void eachClassOccupiedSlotRelocatesItsItemWithoutLoss() {
        for (int selected = 0; selected < 6; selected++) {
            QuickSlot slots = new QuickSlot(6);
            Item first = new Item();
            Item second = new Item();
            Waterskin skin = new Waterskin();
            slots.setSlot(0, first);
            slots.setSlot(1, second);
            WaterskinStartupSlot.assign(slots, skin, selected, false);
            assertSame(skin, slots.getItem(selected));
            assertEquals(selected, slots.getSlot(skin));
            assertTrue("Original first class item preserved", slots.contains(first));
            assertTrue("Original second class item preserved", slots.contains(second));
            assertNotEquals(slots.getSlot(first), slots.getSlot(second));
            assertEquals(1, count(slots, skin));
        }
    }

    @Test public void swappingExistingWaterskinInFullHotbarPreservesEverything() {
        QuickSlot slots=new QuickSlot(6);
        Waterskin skin=new Waterskin();
        Item swapped=new Item();
        slots.setSlot(0,skin);
        slots.setSlot(1,swapped);
        for(int i=2;i<6;i++)slots.setSlot(i,new Item());
        WaterskinStartupSlot.assign(slots,skin,1,true);
        assertSame(skin,slots.getItem(1));
        assertSame(swapped,slots.getItem(0));
        assertEquals(1,count(slots,skin));
        assertEquals(1,count(slots,swapped));
    }

    @Test public void movingExistingWaterskinToChosenSlotKeepsOtherAssignments() {
        QuickSlot slots=new QuickSlot(6);
        Waterskin skin=new Waterskin();
        Item second=new Item();
        slots.setSlot(4,skin);
        slots.setSlot(1,second);
        WaterskinStartupSlot.assign(slots,skin,1,true);
        assertSame(skin,slots.getItem(1));
        assertSame(second,slots.getItem(4));
        assertEquals(1,count(slots,skin));
    }

    @Test public void fullHotbarDoesNotReplaceStartingClassItem() {
        QuickSlot slots = new QuickSlot(6);
        Item[] originals = new Item[6];
        for (int i = 0; i < 6; i++) {
            originals[i] = new Item();
            slots.setSlot(i, originals[i]);
        }
        Waterskin skin = new Waterskin();
        WaterskinStartupSlot.assign(slots, skin, 1, true);
        for (int i = 0; i < 6; i++) assertSame(originals[i], slots.getItem(i));
        assertEquals(-1, slots.getSlot(skin));
    }

    @Test public void preferenceLabelsAndValidationAreStable() {
        assertEquals("Automatic", WaterskinStartupSlot.label(-1));
        assertEquals("Slot 1", WaterskinStartupSlot.label(0));
        assertEquals("Slot 2", WaterskinStartupSlot.label(1));
        assertEquals("Slot 6", WaterskinStartupSlot.label(5));
        assertEquals("Off", WaterskinStartupSlot.label(6));
        for (int invalid : new int[]{-3, -2, 7, 100}) {
            try {
                SPDSettings.waterskinStartingSlot(invalid);
                fail("Invalid preference should be rejected");
            } catch (IllegalArgumentException expected) {}
        }
    }

    private static int count(QuickSlot slots, Item item) {
        int n = 0;
        for (int i = 0; i < slots.capacity(); i++) if (slots.getItem(i) == item) n++;
        return n;
    }
}
