package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.Key;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import java.io.*;
import java.util.*;

/** Runtime coordinator. Never loads another floor or alters its actor/RNG context. */
public final class LootTransfers {
    private LootTransfers() {}
    public static LootTransitState state = new LootTransitState();
    private static boolean reloadRequired;
    private static Level lastProbeLevel;
    private static float lastProbeDuration = Float.NaN;

    private static LootTransferJournal journal(int slot) throws IOException {
        return new LootTransferJournal(FileUtils.getFileHandle(GamesInProgress.gameFolder(slot)).file());
    }
    public static void requireClear(int slot) throws IOException {
        if (reloadRequired) throw new IOException("Reload required after interrupted loot transfer");
        if (FileUtils.dirExists(GamesInProgress.gameFolder(slot))) journal(slot).requireClear();
    }
    public static void recover(int slot) throws IOException {
        if (FileUtils.dirExists(GamesInProgress.gameFolder(slot))) journal(slot).recover();
        reloadRequired = false;
    }
    public static void reset() {
        state = new LootTransitState(); reloadRequired = false;
        lastProbeLevel = null; lastProbeDuration = Float.NaN;
    }

    private static byte[] bytes(Bundle bundle) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!Bundle.write(bundle, out)) throw new IOException("Cannot serialize loot save");
        return out.toByteArray();
    }
    private static void checkpoint() throws IOException {
        requireClear(GamesInProgress.curSlot);
        Actor.fixTime();
        Dungeon.updateLevelExplored();
        Bundle floor = new Bundle(); floor.put("level", Dungeon.level);
        Map<String,byte[]> images = new LinkedHashMap<>();
        images.put("game.dat", bytes(Dungeon.apdCreateGameBundle()));
        String path = GamesInProgress.depthFile(GamesInProgress.curSlot, Dungeon.depth, Dungeon.branch);
        images.put(path.substring(path.lastIndexOf('/') + 1), bytes(floor));
        journal(GamesInProgress.curSlot).commit(images);
        com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder.onGameSaved(Dungeon.lastPlayed);
        GamesInProgress.set(GamesInProgress.curSlot);
    }

    /** Progression objects stay put while the owner settles their transport policy. */
    public static boolean eligible(Item item) {
        return !(item instanceof Key || item instanceof LostBackpack
                || item.getClass().getPackage().getName().endsWith(".items.quest"));
    }

    /** Validate before consuming anything. completion performs normal scroll read effects once. */
    public static String cast(boolean exotic, Runnable completion) {
        Level level = Dungeon.level;
        if (level == null || Dungeon.hero == null) return "There is no dungeon floor to gather from.";
        int cell = exotic ? -1 : LootPlacement.besideDownstairs(level);
        if (!exotic && cell < 0) return "There is no safe space beside the downstairs. The scroll is not consumed.";
        int destination = LootScrollRules.destinationDepth(Dungeon.depth);
        if (exotic && destination < 0) return "There is no shop destination. The scroll is not consumed.";
        GroundLootPlan plan = GroundLootPlan.capture(level, LootTransfers::eligible);
        if (plan.stacks() == 0) return "There is no eligible loose loot to gather. The scroll is not consumed.";
        if (exotic) for (GroundLootPlan.Source source : plan.sources) for (Item item : source.items)
            if (item instanceof Bomb && ((Bomb)item).fuse != null)
                return "An armed bomb cannot be sent between floors. Let its fuse finish first. The scroll is not consumed.";
        try {
            // A pre-cast committed checkpoint makes failures before journal preparation safe too.
            checkpoint();
            List<Heap> oldHeaps = new ArrayList<>(level.heaps.valueList());
            List<Item> moved = GroundLootMover.remove(level, plan);
            if (exotic) state.add(new LootShipment(UUID.randomUUID().toString(), Dungeon.depth,
                    Dungeon.branch, destination, moved));
            else GroundLootMover.place(level, cell, moved);
            if (exotic) deliverInMemory();
            completion.run();
            checkpoint();
            // Only committed transfers update visuals. No fake pickup/drop callbacks.
            refreshVisuals(oldHeaps);
            return null;
        } catch (IOException | RuntimeException failure) {
            failClosed(failure);
            return "The transfer was interrupted while saving. Continue the saved run to recover it.";
        }
    }

    private static boolean deliverInMemory() {
        if (Dungeon.branch != 0 || Dungeon.level == null) return false;
        List<LootShipment> ready = new ArrayList<>();
        for (LootShipment shipment : state.pending())
            if (shipment.destinationDepth() == Dungeon.depth) ready.add(shipment);
        if (ready.isEmpty()) return false;
        int cell = LootPlacement.outsideShop(Dungeon.level);
        if (cell < 0) return false; // Keep full payload until shop opens / approach becomes safe.
        List<Item> items = new ArrayList<>();
        for (LootShipment shipment : ready)
            if (!Dungeon.level.apdLootReceipts.contains(shipment.id())) items.addAll(shipment.items());
        if (!items.isEmpty()) GroundLootMover.place(Dungeon.level, cell, items);
        for (LootShipment shipment : ready) {
            Dungeon.level.apdLootReceipts.add(shipment.id());
            state.remove(shipment);
        }
        return true;
    }

    /** Called only at a save boundary, after normal generation/load has finished. */
    public static void deliver() throws IOException {
        requireClear(GamesInProgress.curSlot);
        List<Heap> before = Dungeon.level == null ? Collections.emptyList() : new ArrayList<>(Dungeon.level.heaps.valueList());
        try { if (deliverInMemory()) { checkpoint(); refreshVisuals(before); } }
        catch (IOException | RuntimeException e) { failClosed(e); throw new IOException("Loot delivery requires reload", e); }
    }

    /** Called on the render thread before starting/waking the actor thread. */
    public static boolean tick() {
        if (reloadRequired) { failClosed(new IOException("Loot transfer requires Continue to recover")); return false; }
        if (state.pending().isEmpty() || Dungeon.hero == null || !Dungeon.hero.ready || Actor.processing()) return true;
        if (lastProbeLevel == Dungeon.level && lastProbeDuration == Statistics.duration) return true;
        lastProbeLevel = Dungeon.level; lastProbeDuration = Statistics.duration;
        try { deliver(); return true; } catch (IOException handledByFailClosed) { return false; }
    }

    private static void refreshVisuals(List<Heap> oldHeaps) {
        if (com.watabou.noosa.Game.instance == null || !(ShatteredPixelDungeon.scene() instanceof GameScene)) return;
        Level level = Dungeon.level;
        for (Heap heap : oldHeaps) if (level.heaps.get(heap.pos) != heap && heap.sprite != null) heap.sprite.kill();
        for (Heap heap : level.heaps.valueList()) {
            if (heap.sprite == null) GameScene.add(heap);
            else heap.sprite.link(heap);
        }
    }

    private static void failClosed(Exception failure) {
        reloadRequired = true;
        GameScene.endActorThread();
        ShatteredPixelDungeon.reportException(failure);
        // No mutable in-memory state may be saved after a failed transfer.
        if (com.watabou.noosa.Game.instance != null) ShatteredPixelDungeon.switchScene(TitleScene.class);
    }
}
