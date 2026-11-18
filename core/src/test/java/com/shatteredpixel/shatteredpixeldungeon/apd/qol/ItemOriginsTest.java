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
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdGen;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdItems;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretHoardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.EmptyRoom;
import com.watabou.utils.*;
import com.watabou.utils.Random;
import org.junit.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

public class ItemOriginsTest {
    private Hero oldHero; private Level oldLevel; private int oldDepth, oldBranch;
    public static class RoomsLevel extends SewerLevel {
        public void roomType(Room room) { room.set(0, 0, 7, 7); rooms = new ArrayList<>(); rooms.add(room); }
    }
    private RoomsLevel level;
    private java.lang.reflect.Field notesField; private Object oldNotes;
    @Before public void setup() throws Exception {
        HeadlessTestSupport.start(); notesField = com.shatteredpixel.shatteredpixeldungeon.journal.Notes.class.getDeclaredField("records"); notesField.setAccessible(true); oldNotes = notesField.get(null); com.shatteredpixel.shatteredpixeldungeon.journal.Notes.reset(); oldHero = Dungeon.hero; oldLevel = Dungeon.level;
        oldDepth = Dungeon.depth; oldBranch = Dungeon.branch;
        Dungeon.hero = new Hero(); Dungeon.depth = 8; Dungeon.branch = 0;
        level = new RoomsLevel(); level.setSize(8, 8); level.heaps = new SparseArray<>();
        level.roomType(new EmptyRoom()); Dungeon.level = level;
    }
    @After public void restore() throws Exception { notesField.set(null, oldNotes); Dungeon.hero = oldHero; Dungeon.level = oldLevel; Dungeon.depth = oldDepth; Dungeon.branch = oldBranch; }
    private Sword sword() { Sword sword = new Sword(); sword.levelKnown = false; sword.cursedKnown = false; return sword; }
    private Sword placed(Room room, Heap.Type type) {
        level.roomType(room); Sword sword = sword(); Heap heap = new Heap();
        heap.pos = 18; heap.type = type; heap.items.add(sword); level.heaps.put(18, heap);
        ItemOrigins.captureLevel(level); return sword;
    }
    private static void has(Item item, String... labels) { String s = ItemOrigins.describe(item); for (String label : labels) assertTrue(s, s.contains(label)); }
    @Test public void goldenKeyChestIsNotConfusedWithIronKeyRoomDoor() {
        Sword s = placed(new ArmoryRoom(), Heap.Type.LOCKED_CHEST);
        has(s, "Floor 8", "Golden Key chest", "Armory", "Special room", "Iron Key door");
    }
    @Test public void crystalVaultSeparatesItsIronDoorFromItsCrystalChest() {
        Sword s = placed(new CrystalVaultRoom(), Heap.Type.CRYSTAL_CHEST);
        has(s, "Crystal Key chest", "Crystal vault", "Iron Key door");
    }
    @Test public void crystalPathAndCrystalChoiceAreActualCrystalDoorRooms() {
        has(placed(new CrystalPathRoom(), Heap.Type.HEAP), "Crystal path", "Crystal Key doors");
        has(placed(new CrystalChoiceRoom(), Heap.Type.CHEST), "Chest", "Crystal choice", "Iron Key door", "Crystal Key doors");
    }
    @Test public void tombCryptRetainsItsContainerAndIronDoor() { has(placed(new CryptRoom(), Heap.Type.TOMB), "Tomb", "Crypt", "Iron Key door"); }
    @Test public void ordinarySecretAndTreasureRoomsRemainDistinct() {
        has(placed(new EmptyRoom(), Heap.Type.HEAP), "Ordinary room");
        has(placed(new SecretHoardRoom(), Heap.Type.HEAP), "Hoard", "Secret room");
        has(placed(new TreasuryRoom(), Heap.Type.CHEST), "Treasury", "Special room");
    }
    @Test public void everyConcreteRoomHasAnExplicitLocalizedClassification() throws Exception {
        java.nio.file.Path base = java.nio.file.Paths.get(System.getProperty("apd.game.src")).resolve("levels/rooms");
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(base)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                String name = p.getFileName().toString().replace(".java", "");
                if (Arrays.asList("Room", "SecretRoom", "SpecialRoom", "StandardRoom", "ConnectionRoom").contains(name)) return;
                assertFalse(name, ItemOrigins.roomName(name).isEmpty());
            });
        }
    }
    @Test public void allContainerKindsArePreserved() {
        String[] labels = {"", "Shop stock", "Chest", "Golden Key chest", "Crystal Key chest", "Tomb", "Skeletal remains", "Adventurer remains"};
        for (Heap.Type type : Heap.Type.values()) has(placed(new EmptyRoom(), type), labels[type.ordinal()]);
    }
    @Test public void monsterShopQuestMimicAndBonusAreExplicitSources() {
        int[] kinds = {GenerationEvents.C_MOB_LOOT, GenerationEvents.C_FOR_SALE, GenerationEvents.C_QUEST_REWARD, GenerationEvents.C_MIMIC, GenerationEvents.C_BONUS_DROP};
        String[] labels = {"Monster drop", "Shop stock", "Quest reward", "Mimic drop", "Bonus drop"};
        for (int i = 0; i < kinds.length; i++) { Sword s = sword(); ItemOrigins.recordSource(s, level, 18, kinds[i]); has(s, labels[i], "Floor 8"); }
    }
    @Test public void dropPickupTransferAndEquipCannotOverwriteOriginalSource() {
        Sword s = placed(new TreasuryRoom(), Heap.Type.LOCKED_CHEST); ItemOrigins.Origin original = s.apdFoundOrigin;
        Dungeon.depth = 17; level.roomType(new EmptyRoom());
        ItemOrigins.recordSource(s, level, 22, GenerationEvents.C_FOR_SALE);
        assertTrue(s.collect(Dungeon.hero.belongings.backpack)); Dungeon.hero.belongings.weapon = s;
        assertEquals(original, s.apdFoundOrigin); has(s, "Floor 8", "Treasury", "Golden Key chest");
    }
    @Test public void descriptionHidesImmediatelyAfterIdentificationWithoutDeletingHistory() {
        Sword s = placed(new EmptyRoom(), Heap.Type.HEAP); assertFalse(ItemOrigins.describe(s).isEmpty());
        s.identify(false); assertEquals("", ItemOrigins.describe(s)); assertNotNull(s.apdFoundOrigin);
    }
    @Test public void hiddenPropertiesNeverAffectOriginText() {
        Sword a = placed(new CryptRoom(), Heap.Type.TOMB), b = sword(); b.apdFoundOrigin = a.apdFoundOrigin; b.apdGenerationRange = a.apdGenerationRange;
        b.upgrade(12); b.cursed = true;
        assertEquals(ItemOrigins.describe(a), ItemOrigins.describe(b));
        assertFalse(ItemOrigins.describe(b).contains("+12")); assertFalse(ItemOrigins.describe(b).toLowerCase().contains("cursed"));
    }
    @Test public void actualItemInfoIncludesOriginWhileUnidentified() { Sword s = placed(new EmptyRoom(), Heap.Type.HEAP); assertTrue(s.info().contains("Found on Floor 8")); s.identify(false); assertFalse(s.info().contains("Found on Floor")); }
    @Test public void realItemBundleRoundTripPreservesOriginAndItemState() throws Exception {
        Sword s = placed(new CrystalVaultRoom(), Heap.Type.CRYSTAL_CHEST); s.upgrade(3); s.cursed = true;
        Bundle root = new Bundle(); root.put("item", s); ByteArrayOutputStream out = new ByteArrayOutputStream(); Bundle.write(root, out);
        Sword loaded = (Sword) Bundle.read(new ByteArrayInputStream(out.toByteArray())).get("item");
        assertEquals(s.apdFoundOrigin, loaded.apdFoundOrigin); assertEquals(3, loaded.level()); assertTrue(loaded.cursed);
        has(loaded, "Floor 8", "Crystal Key chest");
    }
    @Test public void legacyItemsHaveNoInventedOrigin() {
        Bundle old = new Bundle(); old.put("quantity", 1); Sword s = sword(); s.restoreFromBundle(old);
        assertNull(s.apdFoundOrigin); assertTrue(s.apdOriginLegacyUnknown); assertEquals("", ItemOrigins.describe(s));
    }
    @Test public void unknownLegacyGearCannotBeRelabelledByARepeatedPickupPurchaseOrChestOpening() {
        Bundle old=new Bundle();old.put("quantity",1);Sword s=sword();s.restoreFromBundle(old);
        for(int source:new int[]{GenerationEvents.C_FOR_SALE,GenerationEvents.C_CHEST,GenerationEvents.C_MIMIC})ItemOrigins.recordSource(s,level,18,source);
        assertNull(s.apdFoundOrigin);assertEquals("",ItemOrigins.describe(s));
    }
    @Test public void malformedSavedOriginDoesNotInventFloorZero() {
        Bundle save = new Bundle(), origin = new Bundle(); origin.put("floor", 0); origin.put("source", 0); origin.put("branch", 0); save.put("apd_item_origin_v1", origin);
        Sword s = sword(); s.restoreFromBundle(save); assertNull(s.apdFoundOrigin);
    }
    @Test public void sourceWithoutRoomRecordsFloorWithoutInventingRoom() { Sword s = sword(); ItemOrigins.record(s, new ItemOrigins.Origin(12, 0, GenerationEvents.C_MOB_LOOT, "")); has(s, "Floor 12", "Monster drop"); assertFalse(ItemOrigins.describe(s).contains("room")); }
    @Test public void unknownRoomClassDoesNotExposeCodeName() { Sword s = sword(); ItemOrigins.record(s, new ItemOrigins.Origin(8, 0, 0, "FutureMysteryRoom")); has(s, "Floor 8"); assertFalse(ItemOrigins.describe(s).contains("FutureMystery")); }
    @Test public void branchLocationIsNotMislabelledAsMainFloor() { Sword s = sword(); ItemOrigins.record(s, new ItemOrigins.Origin(12, 1, 0, "")); has(s, "Floor 12", "branch 1"); }
    @Test public void potionsAndScrollsDoNotGainEquipmentOriginLabels() { PotionOfHealing p = new PotionOfHealing(); ItemOrigins.record(p, new ItemOrigins.Origin(8, 0, 0, "CryptRoom")); assertNull(p.apdFoundOrigin); assertEquals("", ItemOrigins.describe(p)); }
    @Test public void splitAndDuplicatePreserveOriginalStackSource() {
        ThrowingKnife knife = new ThrowingKnife(); knife.quantity(4); ItemOrigins.recordSource(knife, level, 18, 0);
        Item split = knife.split(1), copy = knife.duplicate(); assertEquals(knife.apdFoundOrigin, split.apdFoundOrigin); assertEquals(knife.apdFoundOrigin, copy.apdFoundOrigin); assertEquals(3, knife.quantity());
    }
    @Test public void differentOrUnknownMergedSourcesAreNeverPresentedAsOneCertainRoom() {
        ThrowingKnife a = new ThrowingKnife(), b = new ThrowingKnife(); ItemOrigins.recordSource(a, level, 18, 0);
        a.quantity(1); b.quantity(1); b.setID = a.setID; int before = a.quantity() + b.quantity(); a.merge(b); assertEquals(ItemOrigins.MIXED, a.apdFoundOrigin.source); assertEquals(before, a.quantity());
    }
    @Test public void sameOriginMergeRetainsExactSource() {
        ThrowingKnife a = new ThrowingKnife(), b = new ThrowingKnife(); ItemOrigins.recordSource(a, level, 18, 0); ItemOrigins.recordSource(b, level, 18, 0);
        b.setID = a.setID; a.merge(b); assertEquals(0, b.quantity()); assertEquals(8, a.apdFoundOrigin.floor); assertEquals(0, a.apdFoundOrigin.source);
    }
    @Test public void generationMetadataWorksWithoutRecorder() { Sword s = sword(); Heap h = new Heap(); h.pos = 18; h.type = Heap.Type.CHEST; h.items.add(s); level.heaps.put(18, h); ApdGen.onLevelGenerated(level); has(s, "Chest", "Floor 8"); }
    @Test public void collectingMetadataConsumesNoGameRandomness() { Random.pushGenerator(981); try { long expected = Random.Long(); Random.popGenerator(); Random.pushGenerator(981); placed(new ArmoryRoom(), Heap.Type.LOCKED_CHEST); assertEquals(expected, Random.Long()); } finally { Random.popGenerator(); } }
    @Test public void anonymousUnidentifiedRingStillShowsItsOrigin() {
        com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce ring = new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce();
        ring.anonymize(); ring.levelKnown = false; ring.cursedKnown = false;
        Dungeon.hero.heroClass = com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.WARRIOR;
        ItemOrigins.recordSource(ring, level, 18, GenerationEvents.C_CHEST);
        assertTrue(ring.info().contains("Found on Floor 8"));
    }
    @Test public void craftedAndTransformedEquipmentHaveAppropriateOrigin() {
        Sword source = placed(new ArmoryRoom(), Heap.Type.HEAP), result = sword(); ApdItems.onTransformed(source, result, "transmute"); assertEquals(source.apdFoundOrigin, result.apdFoundOrigin);
        Sword made = sword(); ApdItems.onCrafted(Collections.emptyList(), made); has(made, "Crafted item", "Floor 8");
    }
}
