package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfGathering;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfForwarding;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ShopRoom;
import com.watabou.utils.*;
import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;

public class LootRuntimeTest {
    private com.watabou.noosa.Game previousGame;
    @Before public void setup() throws Exception {
        previousGame=com.watabou.noosa.Game.instance;
        new com.watabou.noosa.Game(com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene.class,null);
        HeadlessTestSupport.start(); GamesInProgress.curSlot=1; LootTransfers.reset();
        assertTrue("Only disposable headless fixtures may be deleted",FileUtils.getFileHandle(GamesInProgress.gameFolder(1)).file().getCanonicalPath()
                .startsWith(HeadlessTestSupport.tempDir().getCanonicalPath()+java.io.File.separator));
        FileUtils.deleteDir(GamesInProgress.gameFolder(1));
        LevelGenerationHarness.generate(467831,0,HeroClass.WARRIOR,LevelGenerationHarness.Context.VETERAN,1);
        Dungeon.hero.pos=Dungeon.level.entrance();
        Dungeon.saveAll();
    }
    @After public void restoreGame() { com.watabou.noosa.Game.instance=previousGame; }
    private Heap pile(int cell,Heap.Type type,Item...items) {
        Heap heap=new Heap();heap.pos=cell;heap.type=type;Collections.addAll(heap.items,items);
        Dungeon.level.heaps.put(cell,heap);return heap;
    }
    private int safeAway() {
        for(int i=0;i<Dungeon.level.length();i++) if(LootPlacement.safe(Dungeon.level,i)
                && i!=LootPlacement.besideDownstairs(Dungeon.level)) return i;
        throw new AssertionError("Fixture has no safe source");
    }
    private void reload() throws Exception {
        Dungeon.loadGame(1); Level level=Dungeon.loadLevel(1); Dungeon.level=level;
    }
    private long named(String uid) {
        long found=0;for(Heap heap:Dungeon.level.heaps.valueList())
            for(Item item:heap.items)if(uid.equals(item.apdUid))found++;
        return found;
    }

    @Test public void realRegularEffectConsumesOnceAndSurvivesReloadWithoutDuplication() throws Exception {
        Dungeon.level.heaps.clear(); Pasty food=new Pasty();food.quantity(7);food.apdUid="runtime-food";
        Dagger weapon=new Dagger();weapon.upgrade(3);weapon.enchant(new Projecting());weapon.cursed=true;weapon.apdUid="runtime-weapon";
        weapon.apdFoundOrigin=new com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins.Origin(1,0,0,"ArmoryRoom");
        com.shatteredpixel.shatteredpixeldungeon.apd.qol.GenerationUpgradeRanges.set(weapon,0,2);
        pile(safeAway(),Heap.Type.HEAP,food,weapon);
        Gathering scroll=new Gathering();assertTrue(scroll.collect(Dungeon.hero.belongings.backpack));
        scroll.execute(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.AC_READ);
        assertEquals(1,scroll.reads);assertFalse(Dungeon.hero.belongings.backpack.contains(scroll));
        Heap target=Dungeon.level.heaps.get(LootPlacement.besideDownstairs(Dungeon.level));
        assertEquals(2,target.items.size());assertEquals(1,named("runtime-food"));assertEquals(1,named("runtime-weapon"));
        reload();assertEquals(1,named("runtime-food"));assertEquals(1,named("runtime-weapon"));
        Item restored=Dungeon.level.heaps.valueList().get(0).items.stream().filter(i->"runtime-weapon".equals(i.apdUid)).findFirst().get();
        assertEquals(3,restored.level());assertTrue(restored.cursed);assertTrue(((Dagger)restored).enchantment instanceof Projecting);
        assertEquals(weapon.apdFoundOrigin,restored.apdFoundOrigin);
        assertEquals(weapon.apdGenerationRange,restored.apdGenerationRange);
    }

    @Test public void exoticPayloadSurvivesSaveThenDeliversOutsideGeneratedShopOnce() throws Exception {
        Dungeon.level.heaps.clear();Pasty food=new Pasty();food.quantity(9);food.apdUid="forwarded-food";
        Dagger gear=new Dagger();gear.apdUid="forwarded-gear";
        gear.apdFoundOrigin=new com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins.Origin(1,0,com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents.C_LOCKED_CHEST,"ArmoryRoom");
        com.shatteredpixel.shatteredpixeldungeon.apd.qol.GenerationUpgradeRanges.set(gear,0,2);
        pile(safeAway(),Heap.Type.HEAP,food,gear);
        Forwarding scroll=new Forwarding();assertTrue(scroll.collect(Dungeon.hero.belongings.backpack));
        scroll.execute(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.AC_READ);
        assertEquals(1,scroll.reads);assertEquals(1,LootTransfers.state.pending().size());assertEquals(0,named("forwarded-food"));
        reload();assertEquals(1,LootTransfers.state.pending().size());
        assertEquals(9,LootTransfers.state.pending().get(0).items().get(0).quantity());
        Dungeon.depth=6;Dungeon.branch=0;Dungeon.level=Dungeon.newLevel();Dungeon.hero.pos=Dungeon.level.entrance();
        int destination=LootPlacement.outsideShop(Dungeon.level);assertTrue(destination>=0);
        Dungeon.saveAll();assertEquals(0,LootTransfers.state.pending().size());assertEquals(1,named("forwarded-food"));
        Heap target=Dungeon.level.heaps.get(destination);assertNotNull(target);assertEquals(Heap.Type.HEAP,target.type);
        for(Room room:((RegularLevel)Dungeon.level).rooms())if(room instanceof ShopRoom)
            assertFalse(room.inside(Dungeon.level.cellToPoint(destination)));
        Item delivered=target.items.stream().filter(i->"forwarded-gear".equals(i.apdUid)).findFirst().get();
        assertEquals(gear.apdFoundOrigin,delivered.apdFoundOrigin);
        assertEquals(1,delivered.apdFoundOrigin.floor);
        Set<String> receipts=new HashSet<>(Dungeon.level.apdLootReceipts);assertEquals(1,receipts.size());
        reload();Dungeon.saveAll();assertEquals(1,named("forwarded-food"));assertEquals(receipts,Dungeon.level.apdLootReceipts);
        Item loadedGear=Dungeon.level.heaps.get(destination).items.stream().filter(i->"forwarded-gear".equals(i.apdUid)).findFirst().get();
        assertEquals(gear.apdFoundOrigin,loadedGear.apdFoundOrigin);
        assertEquals(gear.apdGenerationRange,loadedGear.apdGenerationRange);
    }

    @Test public void noExitNoLootAndArmedBombNeverConsumeScrollOrReadTurn() throws Exception {
        Dungeon.level.heaps.clear();Gathering regular=new Gathering();regular.collect(Dungeon.hero.belongings.backpack);
        regular.execute(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.AC_READ);
        assertEquals(0,regular.reads);assertTrue(Dungeon.hero.belongings.backpack.contains(regular));
        pile(safeAway(),Heap.Type.HEAP,new Pasty());Dungeon.level.transitions.clear();
        regular.execute(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.AC_READ);
        assertEquals(0,regular.reads);assertTrue(Dungeon.hero.belongings.backpack.contains(regular));
        com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb bomb=new com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb();
        bomb.fuse=new com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb.Fuse();
        pile(safeAway(),Heap.Type.HEAP,bomb);Forwarding exotic=new Forwarding();exotic.collect(Dungeon.hero.belongings.backpack);
        exotic.execute(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.AC_READ);
        assertEquals(0,exotic.reads);assertTrue(Dungeon.hero.belongings.backpack.contains(exotic));assertTrue(LootTransfers.state.pending().isEmpty());
    }

    @Test public void oldSavesAndUnknownQueueVersionNeverInventOrDiscardShipment() {
        assertTrue(LootTransitState.restore(new Bundle()).pending().isEmpty());
        Bundle game=new Bundle(),state=new Bundle();state.put("version",2);state.put("count",1);state.put("pending",new ArrayList<Item>());game.put("apd_loot_transit",state);
        try{LootTransitState.restore(game);fail();}catch(IllegalArgumentException expected){}
    }

    @Test public void alreadyVisitedShopStillReceivesShipmentOnReturn() throws Exception {
        Dungeon.depth=6;Dungeon.level=Dungeon.newLevel();Dungeon.hero.pos=Dungeon.level.entrance();Dungeon.saveAll();
        Dungeon.depth=1;Dungeon.loadGame(1); // Use saved shop state, then revisit the original saved floor.
        Dungeon.depth=1;Dungeon.level=Dungeon.loadLevel(1);Dungeon.hero.pos=Dungeon.level.entrance();
        Dungeon.level.heaps.clear();Pasty food=new Pasty();food.apdUid="visited-shop-loot";pile(safeAway(),Heap.Type.HEAP,food);
        Forwarding scroll=new Forwarding();scroll.collect(Dungeon.hero.belongings.backpack);scroll.execute(Dungeon.hero,"READ");
        assertEquals(1,LootTransfers.state.pending().size());
        Dungeon.depth=6;Dungeon.level=Dungeon.loadLevel(1);Dungeon.hero.pos=Dungeon.level.entrance();Dungeon.saveAll();
        assertEquals(0,LootTransfers.state.pending().size());assertEquals(1,named("visited-shop-loot"));
        reload();assertEquals(1,named("visited-shop-loot"));
    }

    @Test public void blockedShopApproachRetainsPayloadUntilItBecomesSafe() throws Exception {
        Dungeon.level.heaps.clear();Pasty food=new Pasty();food.apdUid="blocked-shop-loot";pile(safeAway(),Heap.Type.HEAP,food);
        Forwarding scroll=new Forwarding();scroll.collect(Dungeon.hero.belongings.backpack);scroll.execute(Dungeon.hero,"READ");
        Dungeon.depth=6;Dungeon.level=Dungeon.newLevel();Dungeon.hero.pos=Dungeon.level.entrance();
        int target=LootPlacement.outsideShop(Dungeon.level);assertTrue(target>=0);
        Heap obstruction=pile(target,Heap.Type.CHEST,new Dagger());Dungeon.saveAll();
        assertEquals(1,LootTransfers.state.pending().size());assertSame(obstruction,Dungeon.level.heaps.get(target));assertEquals(0,named("blocked-shop-loot"));
        Dungeon.level.heaps.remove(target);Dungeon.saveAll();assertEquals(0,LootTransfers.state.pending().size());assertEquals(1,named("blocked-shop-loot"));
    }

    @Test public void demonHallsRouteReturnsToActualImpShopAndWaitsForOpening() throws Exception {
        // Generate the real final shop level in its ordinary unopened state.
        Dungeon.depth=20;Dungeon.level=Dungeon.newLevel();Dungeon.hero.pos=Dungeon.level.entrance();Dungeon.saveAll();
        assertEquals(-1,LootPlacement.outsideShop(Dungeon.level));
        Dungeon.depth=22;Dungeon.level=Dungeon.newLevel();Dungeon.hero.pos=Dungeon.level.entrance();Dungeon.level.heaps.clear();
        Pasty food=new Pasty();food.apdUid="imp-loot";pile(safeAway(),Heap.Type.HEAP,food);
        Forwarding scroll=new Forwarding();scroll.collect(Dungeon.hero.belongings.backpack);scroll.execute(Dungeon.hero,"READ");
        assertEquals(20,LootTransfers.state.pending().get(0).destinationDepth());
        Dungeon.depth=20;Dungeon.level=Dungeon.loadLevel(1);Dungeon.hero.pos=Dungeon.level.entrance();Dungeon.saveAll();
        assertEquals(1,LootTransfers.state.pending().size());
        java.lang.reflect.Field roomField=CityBossLevel.class.getDeclaredField("impShop");roomField.setAccessible(true);
        com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.ImpShopRoom room=
                (com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.ImpShopRoom)roomField.get(Dungeon.level);
        // Open the actual shop using its existing implementation, no custom replacement shop.
        room.spawnShop(Dungeon.level);
        int target=LootPlacement.outsideShop(Dungeon.level);assertTrue("actual Imp exterior must be usable",target>=0);
        assertTrue(target/Dungeon.level.width()>room.bottom);
        Dungeon.saveAll();assertTrue(LootTransfers.state.pending().isEmpty());assertEquals(1,named("imp-loot"));
        reload();Dungeon.saveAll();assertEquals(1,named("imp-loot"));
    }

    @Test public void originalScrollIdentitiesAndGenerationCategoryAreUnchanged() {
        Bundle before=new Bundle();com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.save(before);
        int classes=Generator.Category.SCROLL.classes.length;assertEquals(13,classes);
        new ScrollOfGathering();new ScrollOfForwarding();
        Bundle after=new Bundle();com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.save(after);
        for(String key:before.getKeys())assertEquals(before.getString(key),after.getString(key));
        assertEquals(before.getKeys().size(),after.getKeys().size());
        assertEquals(13,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll.exoToReg.size());
    }

    @Test public void stalePendingEntryCannotRedeliverARecordedReceipt() throws Exception {
        Dungeon.level.heaps.clear();Pasty food=new Pasty();food.apdUid="receipt-loot";pile(safeAway(),Heap.Type.HEAP,food);
        Forwarding scroll=new Forwarding();scroll.collect(Dungeon.hero.belongings.backpack);scroll.execute(Dungeon.hero,"READ");
        LootShipment shipment=LootTransfers.state.pending().get(0);
        Dungeon.depth=6;Dungeon.level=Dungeon.newLevel();Dungeon.hero.pos=Dungeon.level.entrance();Dungeon.saveAll();
        assertEquals(1,named("receipt-loot"));LootTransfers.state.add(shipment);
        Dungeon.saveAll();assertTrue(LootTransfers.state.pending().isEmpty());assertEquals(1,named("receipt-loot"));
        reload();assertEquals(1,named("receipt-loot"));
    }

    @Test public void floorEightCastsReachElevenAndLeaveCarriedInventoryAlone() throws Exception {
        Dungeon.depth=8;Dungeon.level=Dungeon.newLevel();Dungeon.hero.pos=Dungeon.level.entrance();Dungeon.level.heaps.clear();
        Dagger carried=new Dagger();carried.apdUid="carried-weapon";carried.collect(Dungeon.hero.belongings.backpack);
        Pasty food=new Pasty();food.apdUid="eight-eleven";pile(safeAway(),Heap.Type.HEAP,food);
        Forwarding scroll=new Forwarding();scroll.collect(Dungeon.hero.belongings.backpack);scroll.execute(Dungeon.hero,"READ");
        assertEquals(11,LootTransfers.state.pending().get(0).destinationDepth());assertTrue(Dungeon.hero.belongings.backpack.contains(carried));
        Dungeon.depth=11;Dungeon.level=Dungeon.newLevel();Dungeon.hero.pos=Dungeon.level.entrance();Dungeon.saveAll();
        assertEquals(1,named("eight-eleven"));assertEquals(0,named("carried-weapon"));assertTrue(Dungeon.hero.belongings.backpack.contains(carried));
    }

    @Test public void actualRunAndFloorBundlesRecoverAtEveryInterruptedWriteBoundary() throws Exception {
        Dungeon.level.heaps.clear();Pasty food=new Pasty();food.apdUid="interruption-loot";pile(safeAway(),Heap.Type.HEAP,food);
        Gathering scroll=new Gathering();scroll.collect(Dungeon.hero.belongings.backpack);Dungeon.saveAll();
        java.io.File folder=FileUtils.getFileHandle(GamesInProgress.gameFolder(1)).file();
        java.nio.file.Path game=new java.io.File(folder,"game.dat").toPath(),floor=new java.io.File(folder,"depth1.dat").toPath();
        byte[] oldGame=java.nio.file.Files.readAllBytes(game),oldFloor=java.nio.file.Files.readAllBytes(floor);
        scroll.execute(Dungeon.hero,"READ");
        Map<String,byte[]> images=new LinkedHashMap<>();images.put("game.dat",java.nio.file.Files.readAllBytes(game));images.put("depth1.dat",java.nio.file.Files.readAllBytes(floor));
        for(int failure=1;failure<=4;failure++)for(boolean after:new boolean[]{false,true}){
            java.nio.file.Files.write(game,oldGame);java.nio.file.Files.write(floor,oldFloor);
            java.nio.file.Files.deleteIfExists(new java.io.File(folder,LootTransferJournal.JOURNAL).toPath());
            final int selected=failure;final boolean selectedAfter=after;
            LootTransferJournal.Storage storage=new LootTransferJournal.Storage(){
                int mutation;
                java.nio.file.Path path(String name){return new java.io.File(folder,name).toPath();}
                public boolean exists(String n){return java.nio.file.Files.exists(path(n));}
                public byte[] read(String n)throws java.io.IOException{return java.nio.file.Files.readAllBytes(path(n));}
                void before()throws java.io.IOException{mutation++;if(mutation==selected&&!selectedAfter)throw new java.io.IOException("before");}
                void after()throws java.io.IOException{if(mutation==selected&&selectedAfter)throw new java.io.IOException("after");}
                public void replace(String n,byte[] b)throws java.io.IOException{before();java.nio.file.Files.write(path(n),b);after();}
                public void remove(String n)throws java.io.IOException{before();java.nio.file.Files.delete(path(n));after();}
            };
            try{new LootTransferJournal(storage).commit(images);fail();}catch(java.io.IOException expected){}
            reload();assertEquals(1,named("interruption-loot"));
            Heap location=Dungeon.level.heaps.valueList().stream().filter(h->h.items.stream().anyMatch(i->"interruption-loot".equals(i.apdUid))).findFirst().get();
            if(failure==1&&!after)assertNotEquals(LootPlacement.besideDownstairs(Dungeon.level),location.pos);
            else assertEquals(LootPlacement.besideDownstairs(Dungeon.level),location.pos);
            Dungeon.saveAll();reload();assertEquals(1,named("interruption-loot"));
        }
    }

    @Test public void bothActualReadAnimationsConsumeOneTurnForAllSixClasses() throws Exception {
        com.watabou.noosa.Camera original=com.watabou.noosa.Camera.main;
        try {
            com.watabou.noosa.Camera.main=new com.watabou.noosa.Camera(0,0,320,480,1);
            for(HeroClass type:HeroClass.values())for(boolean exotic:new boolean[]{false,true}) {
                LootTransfers.reset();LevelGenerationHarness.generate(78231+type.ordinal(),0,type,LevelGenerationHarness.Context.VETERAN,1);
                Dungeon.hero.pos=Dungeon.level.entrance();Dungeon.level.heaps.clear();pile(safeAway(),Heap.Type.HEAP,new Pasty());
                Dungeon.hero.sprite=new com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite();
                com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll scroll=exotic?new NaturalForwarding():new NaturalGathering();
                scroll.collect(Dungeon.hero.belongings.backpack);float before=Dungeon.hero.cooldown();
                scroll.execute(Dungeon.hero,"READ");
                assertEquals(type+" "+exotic,before+1,Dungeon.hero.cooldown(),0.0001f);
                assertFalse(Dungeon.hero.belongings.backpack.contains(scroll));
                reload();assertNull(Dungeon.hero.belongings.getItem(scroll.getClass()));
            }
        } finally { com.watabou.noosa.Camera.main=original; }
    }

    @Test public void corruptTransferBlocksEverySaveReplacementAndPreservesEvidence() throws Exception {
        byte[] game=FileUtils.getFileHandle(GamesInProgress.gameFile(1)).readBytes();
        byte[] floor=FileUtils.getFileHandle(GamesInProgress.depthFile(1,1,0)).readBytes();
        String marker=GamesInProgress.gameFolder(1)+"/"+LootTransferJournal.JOURNAL;
        FileUtils.getFileHandle(marker).writeBytes(new byte[0],false);
        String staging=marker+".loot-writing";FileUtils.getFileHandle(staging).writeBytes(new byte[]{1,2},false);
        FileUtils.cleanTempFiles(GamesInProgress.gameFolder(1)+"/");
        assertTrue(FileUtils.getFileHandle(marker).exists());assertEquals(0,FileUtils.getFileHandle(marker).length());
        assertTrue(FileUtils.getFileHandle(staging).exists());
        Dungeon.saveGame(1);
        try{Dungeon.saveLevel(1);fail();}catch(java.io.IOException expected){}
        try{Dungeon.saveAll();fail();}catch(java.io.IOException expected){}
        Dungeon.deleteGame(1,true);
        try{Dungeon.init();fail();}catch(IllegalStateException expected){}
        assertArrayEquals(game,FileUtils.getFileHandle(GamesInProgress.gameFile(1)).readBytes());
        assertArrayEquals(floor,FileUtils.getFileHandle(GamesInProgress.depthFile(1,1,0)).readBytes());
        try{Dungeon.loadGame(1);fail();}catch(java.io.IOException expected){}
        assertTrue(FileUtils.getFileHandle(marker).exists());assertEquals(0,FileUtils.getFileHandle(marker).length());
    }

    @Test public void stableGameplayTickDeliversWithoutRequiringPauseOrFloorChange() throws Exception {
        Dungeon.level.heaps.clear();Pasty food=new Pasty();food.apdUid="tick-loot";pile(safeAway(),Heap.Type.HEAP,food);
        Forwarding scroll=new Forwarding();scroll.collect(Dungeon.hero.belongings.backpack);scroll.execute(Dungeon.hero,"READ");
        Dungeon.depth=6;Dungeon.level=Dungeon.newLevel();Dungeon.hero.pos=Dungeon.level.entrance();
        int target=LootPlacement.outsideShop(Dungeon.level);assertTrue(target>=0);
        pile(target,Heap.Type.CHEST,new Dagger());Dungeon.hero.ready=true;
        assertTrue(LootTransfers.tick());assertEquals(1,LootTransfers.state.pending().size());
        Dungeon.level.heaps.remove(target);Statistics.duration+=1;
        assertTrue(LootTransfers.tick());assertTrue(LootTransfers.state.pending().isEmpty());assertEquals(1,named("tick-loot"));
        reload();assertEquals(1,named("tick-loot"));
    }

    public static class Gathering extends ScrollOfGathering {
        int reads;
        @Override public void execute(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero,String action) { curUser=hero;doRead(); }
        @Override public void readAnimation(){reads++;Dungeon.hero.spend(1);}
    }
    public static class Forwarding extends ScrollOfForwarding {
        int reads;
        @Override public void execute(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero,String action) { curUser=hero;doRead(); }
        @Override public void readAnimation(){reads++;Dungeon.hero.spend(1);}
    }
    public static class NaturalGathering extends ScrollOfGathering {
        @Override public void execute(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero,String action){curUser=hero;doRead();}
    }
    public static class NaturalForwarding extends ScrollOfForwarding {
        @Override public void execute(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero,String action){curUser=hero;doRead();}
    }
}
