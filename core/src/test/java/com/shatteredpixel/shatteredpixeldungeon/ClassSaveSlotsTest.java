package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.Files;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/** No legacy file relocation; each class has exactly twenty independent active runs. */
public class ClassSaveSlotsTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private int previousSlot;
    private HeroClass previousClass;

    @Before public void start() throws Exception {
        HeadlessTestSupport.start();
        File dir = folder.newFolder("isolated-saves");
        FileUtils.setDefaultFileProperties(Files.FileType.Absolute, dir.getAbsolutePath() + File.separator);
        Field cache = GamesInProgress.class.getDeclaredField("slotStates");
        cache.setAccessible(true);
        ((Map<?, ?>)cache.get(null)).clear();
        previousSlot = GamesInProgress.curSlot;
        previousClass = GamesInProgress.selectedClass;
    }

    @After public void stop() throws Exception {
        GamesInProgress.curSlot = previousSlot;
        GamesInProgress.selectedClass = previousClass;
        FileUtils.setDefaultFileProperties(Files.FileType.Absolute,
                new File(HeadlessTestSupport.tempDir(), "save").getAbsolutePath() + File.separator);
        Field cache = GamesInProgress.class.getDeclaredField("slotStates");
        cache.setAccessible(true);
        ((Map<?, ?>)cache.get(null)).clear();
    }

    @Test public void sixClassesHaveExactlyTwentyDisjointDedicatedSlots() {
        Set<Integer> physical = new HashSet<>();
        assertEquals(120, GamesInProgress.MAX_SLOTS);
        assertEquals(126, GamesInProgress.MAX_SLOT_ID);
        assertEquals(6, HeroClass.values().length);
        for (HeroClass heroClass : HeroClass.values()) {
            ArrayList<Integer> slots = GamesInProgress.slotsFor(heroClass, new HashMap<>());
            assertEquals(20, slots.size());
            for (int i = 0; i < 20; i++) {
                assertEquals(GamesInProgress.dedicatedSlot(heroClass, i), (int) slots.get(i));
                assertTrue("Physical slot was assigned to two classes", physical.add(slots.get(i)));
                assertTrue(slots.get(i) > 6);
            }
        }
        assertEquals(120, physical.size());
        for (int i = 7; i <= 126; i++) assertTrue("No physical slot " + i, physical.contains(i));
    }

    @Test public void legacySlotsKeepExactPhysicalIdsAndCountAgainstTheirOriginalClasses() {
        Map<Integer, HeroClass> legacy = new HashMap<>();
        legacy.put(1, HeroClass.MAGE);
        legacy.put(2, HeroClass.WARRIOR);
        legacy.put(3, HeroClass.MAGE);
        legacy.put(4, HeroClass.CLERIC);
        legacy.put(5, HeroClass.DUELIST);
        legacy.put(6, HeroClass.HUNTRESS);
        for (HeroClass heroClass : HeroClass.values()) {
            ArrayList<Integer> slots = GamesInProgress.slotsFor(heroClass, legacy);
            assertEquals(20, slots.size());
            for (Map.Entry<Integer, HeroClass> e : legacy.entrySet()) {
                assertEquals("Legacy " + e.getKey() + " must remain with its own class",
                        heroClass == e.getValue(), slots.contains(e.getKey()));
            }
        }
        assertEquals(Arrays.asList(1,3), GamesInProgress.slotsFor(HeroClass.MAGE,legacy).subList(0,2));
        assertEquals(21, GamesInProgress.dedicatedSlot(HeroClass.WARRIOR,14));
        assertFalse(GamesInProgress.slotsFor(HeroClass.MAGE,legacy).contains(26));
    }

    @Test public void sixLegacyWarriorsReduceWarriorNewCapacityToFourteen() {
        Map<Integer, HeroClass> legacy = new HashMap<>();
        for(int i = 1; i <= 6; i++) legacy.put(i,HeroClass.WARRIOR);
        ArrayList<Integer> warriors = GamesInProgress.slotsFor(HeroClass.WARRIOR, legacy);
        assertEquals(20, warriors.size());
        assertEquals(Arrays.asList(1,2,3,4,5,6),warriors.subList(0,6));
        assertEquals(7,(int)warriors.get(6));
        assertEquals(20,(int)warriors.get(19));
        assertFalse("No 21st Warrior save",warriors.contains(21));
        assertEquals(20,GamesInProgress.slotsFor(HeroClass.MAGE,legacy).size());
    }

    private static void marker(int slot, String text) throws Exception {
        Bundle b = new Bundle();
        b.put("test-preserve", text);
        FileUtils.bundleToFile(GamesInProgress.gameFile(slot), b);
        GamesInProgress.setUnknown(slot);
    }

    @Test public void fullOneHundredTwentyPhysicalFoldersDoNotOverwriteLegacyAndReopen() throws Exception {
        marker(1, "historical Warrior inventory / custom rules / history link");
        byte[] legacyBefore = java.nio.file.Files.readAllBytes(
                FileUtils.getFileHandle(GamesInProgress.gameFile(1)).file().toPath());
        for (HeroClass heroClass : HeroClass.values()) {
            for (int i = 0; i < 20; i++) {
                int slot = GamesInProgress.dedicatedSlot(heroClass,i);
                marker(slot, heroClass.name() + "_" + i);
            }
        }
        Set<Integer> ids = new HashSet<>();
        for (HeroClass heroClass : HeroClass.values()) for(int i=0;i<20;i++) {
            int slot=GamesInProgress.dedicatedSlot(heroClass,i);
            assertTrue(GamesInProgress.gameExists(slot));
            assertTrue(ids.add(slot));
            assertEquals(heroClass.name()+"_"+i,
                    FileUtils.bundleFromFile(GamesInProgress.gameFile(slot)).getString("test-preserve"));
        }
        assertEquals(120, ids.size());
        assertArrayEquals("Unmoved legacy save has identical bytes",legacyBefore,
                java.nio.file.Files.readAllBytes(FileUtils.getFileHandle(GamesInProgress.gameFile(1)).file().toPath()));
        for (HeroClass heroClass : HeroClass.values()) assertEquals(-1,GamesInProgress.firstEmpty(heroClass));
    }

    @Test public void deletingOneClassesSaveReleasesOnlyItsOwnSlotAndLeavesOthers() throws Exception {
        int warrior = GamesInProgress.dedicatedSlot(HeroClass.WARRIOR,0);
        int mage = GamesInProgress.dedicatedSlot(HeroClass.MAGE,0);
        marker(warrior,"Warrior kit");
        marker(mage,"Mage staff");
        assertEquals(warrior+1,GamesInProgress.firstEmpty(HeroClass.WARRIOR));
        assertEquals(mage+1,GamesInProgress.firstEmpty(HeroClass.MAGE));
        Dungeon.deleteGame(warrior,true);
        assertFalse(GamesInProgress.gameExists(warrior));
        assertEquals(warrior,GamesInProgress.firstEmpty(HeroClass.WARRIOR));
        assertEquals(mage+1,GamesInProgress.firstEmpty(HeroClass.MAGE));
        assertEquals("Mage staff",FileUtils.bundleFromFile(GamesInProgress.gameFile(mage)).getString("test-preserve"));
    }

    @Test public void classSelectionCannotConsumeOtherClassesSave() throws Exception {
        int warrior=GamesInProgress.dedicatedSlot(HeroClass.WARRIOR,0);
        int mage=GamesInProgress.dedicatedSlot(HeroClass.MAGE,0);
        marker(mage,"Mage inventory");
        assertFalse(GamesInProgress.isAvailableNewSlot(HeroClass.WARRIOR,mage));
        assertFalse(GamesInProgress.isAvailableNewSlot(HeroClass.MAGE,mage));
        assertTrue(GamesInProgress.isAvailableNewSlot(HeroClass.WARRIOR,warrior));
        assertFalse(GamesInProgress.isAvailableNewSlot(HeroClass.WARRIOR,1));
        assertEquals("Mage inventory",FileUtils.bundleFromFile(GamesInProgress.gameFile(mage)).getString("test-preserve"));
    }
}
