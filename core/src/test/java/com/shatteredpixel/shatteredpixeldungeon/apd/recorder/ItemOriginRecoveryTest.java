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
package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import static org.junit.Assert.*;

public class ItemOriginRecoveryTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private File file; private RunLogWriter writer;
    @Before public void setup() { file = new File(tmp.getRoot(), "origin.apdlog"); writer = RunLogWriter.create(file, TelemetryVersion.CURRENT, "origin", "test", 1); }
    @After public void cleanup() { writer.close(); ApdRecorder.reset(); }
    private void generated(String uid, int floor, int cell, String room, int source) {
        GenerationEvents.ItemGenerated g = new GenerationEvents.ItemGenerated(); g.uid = uid;
        g.where.depth = floor; g.pos = cell; g.room = room; g.container = source;
        g.trueClass = "hidden.SecretWeapon"; g.trueLevel = 77; g.cursed = true; g.enchant = "hidden.Vampiric";
        GenerationEvents.emitItemGenerated(writer, 1, g);
    }
    private void seen(String uid, int floor, int cell) {
        ItemEvents.Seen s = new ItemEvents.Seen(); s.uid = uid; s.where.depth = floor; s.where.pos = cell;
        ItemEvents.emitSeen(writer, 2, 0, s);
    }
    private void acquired(String uid, int floor, int cell, int context) {
        ItemEvents.Acquired a = new ItemEvents.Acquired(); a.uid = uid; a.where.depth = floor; a.where.pos = cell; a.context = context;
        ItemEvents.emitAcquired(writer, 3, 0, a);
    }
    private void lineage(int kind, String from, String to) {
        ItemEvents.Lineage l = new ItemEvents.Lineage(); l.kind = kind; l.from.add(from); l.to = to; l.where.depth = 8;
        ItemEvents.emitLineage(writer, 4, l);
    }
    private ItemOriginHistory read() throws Exception {
        writer.close(); ReadResult log = RunLogReader.read(file); ItemOriginHistory history = new ItemOriginHistory();
        for (ApdEvent event : log.events) history.accept(event, log); history.finishReplay(); return history;
    }
    @Test public void historicalObservedEquipmentRecoversExactOriginalRoomAndContainer() throws Exception {
        generated("a", 8, 18, "ArmoryRoom", GenerationEvents.C_LOCKED_CHEST); seen("a", 8, 18); acquired("a", 8, 18, ItemEvents.ACQ_PICKUP);
        ItemOrigins.Origin origin = read().origins.get("a"); assertEquals(8, origin.floor); assertEquals("ArmoryRoom", origin.room); assertEquals(GenerationEvents.C_LOCKED_CHEST, origin.source);
    }
    @Test public void privateUnseenGenerationNeverBecomesAPlayerFacingOrigin() throws Exception { generated("a", 8, 18, "SecretHoardRoom", 0); assertFalse(read().origins.containsKey("a")); }
    @Test public void movedItemCannotLeakAnUnseenOriginalSecretRoom() throws Exception {
        generated("a", 8, 18, "SecretHoardRoom", 0); seen("a", 8, 32); acquired("a", 8, 32, ItemEvents.ACQ_PICKUP);
        ItemOrigins.Origin origin = read().origins.get("a"); assertEquals("", origin.room); assertEquals(ItemOrigins.PICKUP, origin.source);
    }
    @Test public void collectedWithoutSeenEventCanUseMatchingActualAcquisitionCell() throws Exception {
        generated("a", 8, 18, "CrystalVaultRoom", GenerationEvents.C_CRYSTAL_CHEST); acquired("a", 8, 18, ItemEvents.ACQ_PICKUP);
        assertEquals("CrystalVaultRoom", read().origins.get("a").room);
    }
    @Test public void floorOnlyPublicHistoryStaysHonestAboutUnknownRoom() throws Exception {
        acquired("a", 7, 25, ItemEvents.ACQ_PICKUP); ItemOrigins.Origin origin = read().origins.get("a"); assertEquals(7, origin.floor); assertEquals("", origin.room);
    }
    @Test public void noHistoryOrOnlyReconciliationDoesNotInventOrigin() throws Exception { acquired("a", 12, 25, ItemEvents.ACQ_RECONCILED); assertFalse(read().origins.containsKey("a")); }
    @Test public void shopHistoryIsAShopSourceNotAnInventedRoom() throws Exception { acquired("a", 11, 25, ItemEvents.ACQ_SHOP); ItemOrigins.Origin origin = read().origins.get("a"); assertEquals(GenerationEvents.C_FOR_SALE, origin.source); assertEquals("", origin.room); }
    @Test public void pickingUpAgainNeverReplacesOriginalFloor() throws Exception { acquired("a", 2, 25, ItemEvents.ACQ_PICKUP); acquired("a", 17, 90, ItemEvents.ACQ_PICKUP); assertEquals(2, read().origins.get("a").floor); }
    @Test public void historicalSplitCopyAndTransformationPreserveSource() throws Exception {
        generated("a", 8, 18, "ArmoryRoom", 0); seen("a", 8, 18);
        lineage(ItemEvents.LIN_SPLIT, "a", "b"); lineage(ItemEvents.LIN_COPY, "b", "c"); lineage(ItemEvents.LIN_TRANSFORM, "c", "d");
        ItemOriginHistory h = read(); assertEquals(h.origins.get("a"), h.origins.get("d"));
    }
    @Test public void historicalMixedStacksCannotClaimOneCertainRoom() throws Exception {
        acquired("a", 2, 18, ItemEvents.ACQ_PICKUP); acquired("b", 12, 18, ItemEvents.ACQ_PICKUP); lineage(ItemEvents.LIN_MERGE, "a", "b");
        assertEquals(ItemOrigins.MIXED, read().origins.get("b").source);
    }
    @Test public void existingBackpackGearRecoversOnRealSaveLoad() throws Exception { recoverCarriedGear(false); }
    @Test public void existingEquippedGearRecoversOnRealSaveLoad() throws Exception { recoverCarriedGear(true); }
    private void recoverCarriedGear(boolean equip) throws Exception {
        RecorderTestSupport.Session session = ItemTestSupport.startRun(83L);
        int cell = -1;
        for (int pos = 0; pos < Dungeon.level.length(); pos++) {
            if (Dungeon.level.passable[pos] && Dungeon.level.heaps.get(pos) == null && !ItemOrigins.roomAt(Dungeon.level, pos).isEmpty()) { cell = pos; break; }
        }
        assertTrue(cell >= 0);
        Sword gear = (Sword)Generator.random(Sword.class); gear.levelKnown = false; gear.cursedKnown = false;
        com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Skeleton rat = new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Skeleton(); rat.pos = cell;
        ApdGen.onLootCreated(rat, gear);
        Heap heap = ItemTestSupport.putInSight(gear, cell);
        ItemOrigins.Origin expected = gear.apdFoundOrigin; String uid = gear.apdUid;
        com.shatteredpixel.shatteredpixeldungeon.apd.qol.GenerationUpgradeRanges.Range expectedRange = gear.apdGenerationRange;
        ItemTestSupport.turn(); assertTrue(gear.collect(Dungeon.hero.belongings.backpack)); heap.items.remove(gear);
        ApdItems.noteAcquireContext(ItemEvents.ACQ_PICKUP); ItemTestSupport.turn();
        // Emulate pre-feature owned equipment: UID/public history exist, origin bundle field does not.
        gear.apdFoundOrigin = null; gear.apdGenerationRange = null;
        if (equip) { gear.detachAll(Dungeon.hero.belongings.backpack); Dungeon.hero.belongings.weapon = gear; }
        Dungeon.saveAll(); session.restartApp(); Dungeon.loadGame(session.slot);
        Item restored = null; for (Item item : Dungeon.hero.belongings) if (uid.equals(item.apdUid)) restored = item;
        assertNotNull(restored); assertEquals(expected, restored.apdFoundOrigin);
        assertEquals(expectedRange, restored.apdGenerationRange);
        assertEquals(equip, restored == Dungeon.hero.belongings.weapon);
        assertTrue(ItemOrigins.describe(restored).contains("Possible upgrade level: +0 to +2"));
        assertFalse(ItemOrigins.describe(restored).isEmpty());
        Dungeon.saveAll(); session.restartApp(); Dungeon.loadGame(session.slot);
        for (Item item : Dungeon.hero.belongings) if (uid.equals(item.apdUid)) assertEquals(expected, item.apdFoundOrigin);
    }
    @Test public void oldGearRangeRecoveryNeverUsesRecordedHiddenLevelOrCurse() throws Exception {
        for(int value=0;value<=3;value++) { generated("range"+value,8,18,"CryptRoom",GenerationEvents.C_TOMB);seen("range"+value,8,18); }
        ItemOriginHistory history=read();
        for(int hidden=0;hidden<=3;hidden++) {
            Sword item=new Sword();item.level(hidden);item.cursed=hidden%2==0;item.levelKnown=false;item.cursedKnown=false;
            ItemOrigins.record(item,history.origins.get("range"+hidden));
            assertEquals(new com.shatteredpixel.shatteredpixeldungeon.apd.qol.GenerationUpgradeRanges.Range(0,3),item.apdGenerationRange);
        }
    }
    @Test public void reconciliationThenRepickDoesNotFabricateAnOriginalSource() throws Exception {
        acquired("a",8,18,ItemEvents.ACQ_RECONCILED);acquired("a",12,25,ItemEvents.ACQ_SHOP);
        assertFalse(read().origins.containsKey("a"));
    }
    @Test public void questReceiptRecoversItsSourceAndRangeWithoutInventingTheGiversOldRoom() throws Exception {
        GenerationEvents.ItemGenerated g=new GenerationEvents.ItemGenerated();g.uid="quest";g.where.depth=8;g.pos=18;g.room="LaboratoryRoom";
        g.owner="actors.mobs.npcs.Wandmaker";g.container=GenerationEvents.C_QUEST_REWARD;GenerationEvents.emitItemGenerated(writer,1,g);
        acquired("quest",8,25,ItemEvents.ACQ_OTHER);ItemOrigins.Origin source=read().origins.get("quest");
        assertEquals(GenerationEvents.C_QUEST_REWARD,source.source);assertEquals("",source.room);
        com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();ItemOrigins.record(wand,source);
        assertEquals(new com.shatteredpixel.shatteredpixeldungeon.apd.qol.GenerationUpgradeRanges.Range(1,3),wand.apdGenerationRange);
    }
    @Test public void movingMimicSourceIsRecoveredWithoutDisclosingAnUnseenSpawnRoom() throws Exception {
        generated("m",8,18,"SecretHoardRoom",GenerationEvents.C_MIMIC);acquired("m",8,25,ItemEvents.ACQ_PICKUP);
        ItemOrigins.Origin source=read().origins.get("m");assertEquals(GenerationEvents.C_MIMIC,source.source);assertEquals("",source.room);
    }
    @Test public void eventsAfterTheLoadedSaveCannotPopulateOrigin() throws Exception {
        generated("a", 8, 18, "ArmoryRoom", 0); seen("a", 8, 18); writer.close(); ReadResult log = RunLogReader.read(file);
        long cutoff = -1; for (ApdEvent event : log.events) if (event.type == EventTypes.ITEM_GENERATED) cutoff = event.index;
        File out = new File(tmp.getRoot(), "resume.apdlog"); RunLogWriter sink = RunLogWriter.create(out, TelemetryVersion.CURRENT, "resume", "test", 1);
        ItemTracker tracker = new ItemTracker(sink); tracker.replay(log, RunTimeline.of(log.events), cutoff);
        Sword oldGear = new Sword(); oldGear.apdUid = "a"; tracker.ensureUid(oldGear, ItemEvents.ORIGIN_OBSERVED);
        assertNull(oldGear.apdFoundOrigin); sink.close();
    }
}
