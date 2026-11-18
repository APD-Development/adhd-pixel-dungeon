package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ShopRoom;
import com.watabou.utils.Random;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
public class CustomEquipmentGenerationTest {
    static class Shop extends ShopRoom {static ArrayList<Item> stock(){return generateItems();} void stock(ArrayList<Item> items){itemsToSpawn=items;}}
    private CustomDungeonRules prefs;private HeroClass selected;private boolean intro,recording;
    private int difficulty,challenges;
    @Before public void start(){HeadlessTestSupport.start();prefs=SPDSettings.apdCustomRules();selected=GamesInProgress.selectedClass;
        difficulty=SPDSettings.apdDifficulty();challenges=SPDSettings.challenges();intro=SPDSettings.intro();recording=SPDSettings.apdRecording();
        SPDSettings.intro(false);SPDSettings.apdRecording(false);SPDSettings.apdDifficulty(3);SPDSettings.challenges(0);}
    @After public void clear(){SPDSettings.apdCustomRules(prefs);GamesInProgress.selectedClass=selected;SPDSettings.apdDifficulty(difficulty);
        SPDSettings.challenges(challenges);SPDSettings.intro(intro);SPDSettings.apdRecording(recording);Dungeon.apdRestoreCustomRules(new com.watabou.utils.Bundle());}
    private void init(int percent,HeroClass type,long seed){SPDSettings.apdCustomRules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.EQUIPMENT,percent));
        GamesInProgress.selectedClass=type;Dungeon.customSeedText="";Dungeon.seed=seed;Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.init();}
    @Test public void expandedShopStockPreservesProgressionAndUsesDistinctEquipment(){
        for(HeroClass type:HeroClass.values())for(int depth:new int[]{6,11,16,21}){
            init(200,type,9123+depth);Dungeon.depth=depth;
            ArrayList<Item> stock=Shop.stock();Set<Item> distinct=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());
            int equipment=0,bags=0;
            for(Item item:stock){assertNotNull(item);assertTrue("Stock repeats the same object",distinct.add(item));
                if(CustomEquipmentHooks.eligible(item))equipment++;
                if(item instanceof Bag)bags++;
                if(item instanceof Weapon || item instanceof Armor)assertEquals(0,item.level());
            }
            assertTrue(equipment>=8);assertEquals(1,bags);
            Shop shop=new Shop();shop.stock(stock);
            assertTrue(shop.minWidth()>=7);assertTrue(shop.minHeight()>=7);
        }
    }
    @Test public void floorGenerationAtBothEquipmentBoundariesWorksForAllClasses(){
        for(HeroClass type:HeroClass.values())for(int percent:new int[]{25,200}){
            init(percent,type,1873+type.ordinal());
            for(int depth:new int[]{1,6,11,16,21}){
                Dungeon.depth=depth;Dungeon.branch=0;Dungeon.level=Dungeon.newLevel();
                assertNotNull(Dungeon.level);assertTrue(Dungeon.level.entrance()>=0);assertTrue(Dungeon.level.exit()>=0);
                for(Heap heap:Dungeon.level.heaps.valueList())for(Item item:heap.items)assertNotNull(item);
            }
        }
    }
    static class Empty extends com.shatteredpixel.shatteredpixeldungeon.levels.Level {
        protected boolean build(){return true;}protected void createMobs(){}protected void createItems(){}
    }
    @Test(timeout=3000) public void multipleGeneratedItemsArePlacedBeforeSealingAnIsolatedCrystalChest(){
        init(200,HeroClass.WARRIOR,7123L);
        com.shatteredpixel.shatteredpixeldungeon.levels.Level previous=Dungeon.level;
        Empty room=new Empty();room.setSize(7,7);room.heaps=new com.watabou.utils.SparseArray<>();java.util.Arrays.fill(room.map,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WALL);
        room.map[24]=com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY;Dungeon.level=null; // Match room-paint generation, before the level becomes active.
        try {
            java.util.List<Heap> placed=CustomEquipmentHooks.dropGenerated(room,new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger(),24,Heap.Type.CRYSTAL_CHEST,false);
            assertEquals(2,placed.size());assertSame(placed.get(0),placed.get(1));
            assertEquals(Heap.Type.CRYSTAL_CHEST,room.heaps.get(24).type);assertEquals(2,room.heaps.get(24).items.size());
            assertEquals(1,room.heaps.valueList().size());
        }finally{Dungeon.level=previous;}
    }
    @Test public void legacyTwelveScrollPoolNeutralSnapshotsStillMatchVerifiedTest121Source() throws Exception {
        // Keep the complete historical baseline. The separately approved thirteenth
        // scroll changes the current loot pool; isolate ONLY that addition here.
        Generator.Category scrolls=Generator.Category.SCROLL;
        org.junit.Assert.assertEquals(13,scrolls.classes.length);
        float first=scrolls.defaultProbs[12],second=scrolls.defaultProbs2[12],combined=scrolls.defaultProbsTotal[12];
        scrolls.defaultProbs[12]=0;scrolls.defaultProbs2[12]=0;scrolls.defaultProbsTotal[12]=0;
        // Same isolation for the later owner-approved katana-family blades and shields.
        Generator.Category[] weaponTiers={Generator.Category.WEP_T1,Generator.Category.WEP_T2,Generator.Category.WEP_T3,Generator.Category.WEP_T4,Generator.Category.WEP_T5};
        float[][] weaponDefaults=new float[weaponTiers.length][];
        for(int t=0;t<weaponTiers.length;t++)weaponDefaults[t]=weaponTiers[t].defaultProbs.clone();
        com.shatteredpixel.shatteredpixeldungeon.apd.LegacyGeneration.withoutApdWeapons();
        try {
        java.util.List<String> expected=new ArrayList<>();
        try(java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(
                getClass().getResourceAsStream("/apd/custom_equipment_neutral_test121.sha256"),java.nio.charset.StandardCharsets.UTF_8))){
            String line;while((line=reader.readLine())!=null)if(!line.startsWith("#")&&!line.trim().isEmpty())expected.add(line);
        }
        java.util.List<String> actual=new ArrayList<>();
        for(String record:neutralRecords()){
            byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(record.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex=new StringBuilder();for(byte value:digest)hex.append(String.format(java.util.Locale.ROOT,"%02x",value&255));actual.add(hex.toString());
        }
        assertEquals(24,expected.size());assertEquals(expected,actual);
        } finally {
            scrolls.defaultProbs[12]=first;scrolls.defaultProbs2[12]=second;scrolls.defaultProbsTotal[12]=combined;
            for(int t=0;t<weaponTiers.length;t++)System.arraycopy(weaponDefaults[t],0,weaponTiers[t].defaultProbs,0,weaponDefaults[t].length);
            Generator.fullReset();
        }
    }
    public static void main(String[] args){for(String record:neutralRecords())System.out.println(record);}
    private static void guideIntroState(int state) {
        com.watabou.utils.Bundle root=new com.watabou.utils.Bundle(),docs=new com.watabou.utils.Bundle(),pages=new com.watabou.utils.Bundle();
        pages.put(com.shatteredpixel.shatteredpixeldungeon.journal.Document.GUIDE_INTRO,state);
        docs.put(com.shatteredpixel.shatteredpixeldungeon.journal.Document.ADVENTURERS_GUIDE.name(),pages);
        root.put("documents",docs);com.shatteredpixel.shatteredpixeldungeon.journal.Document.restore(root);
    }
    @Test public void neutralSnapshotsIgnoreAndRestorePriorGuidebookDiscovery() {
        com.shatteredpixel.shatteredpixeldungeon.journal.Document guide=com.shatteredpixel.shatteredpixeldungeon.journal.Document.ADVENTURERS_GUIDE;
        int original=guide.isPageRead(guide.GUIDE_INTRO)?2:guide.isPageFound(guide.GUIDE_INTRO)?1:0;
        try {
            guideIntroState(0);java.util.List<String> missing=neutralRecords();assertFalse(guide.isPageFound(guide.GUIDE_INTRO));
            guideIntroState(2);java.util.List<String> found=neutralRecords();assertTrue(guide.isPageRead(guide.GUIDE_INTRO));
            assertEquals(missing,found);
        }finally{guideIntroState(original);}
    }
    private static java.util.List<String> neutralRecords(){
        java.util.List<String> records=new ArrayList<>();
        HeadlessTestSupport.start();
        com.shatteredpixel.shatteredpixeldungeon.journal.Document guide=com.shatteredpixel.shatteredpixeldungeon.journal.Document.ADVENTURERS_GUIDE;
        int priorIntro=guide.isPageRead(guide.GUIDE_INTRO)?2:guide.isPageFound(guide.GUIDE_INTRO)?1:0;
        // Fixed reference journal: suite order must not add a first-run Guidebook to every snapshot.
        guideIntroState(2);
        try {
        SPDSettings.intro(false);SPDSettings.apdRecording(false);SPDSettings.apdDifficulty(3);SPDSettings.challenges(0);
        SPDSettings.apdCustomRules(CustomDungeonRules.defaults());GamesInProgress.selectedClass=HeroClass.WARRIOR;
        for(int seed=1;seed<=12;seed++){
            Dungeon.customSeedText="";Dungeon.seed=seed;Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.init();Random.pushGenerator(1000L+seed);try{Dungeon.depth=1;Dungeon.branch=0;Dungeon.level=Dungeon.newLevel();
            ArrayList<String> contents=new ArrayList<>();
            for(Heap heap:Dungeon.level.heaps.valueList())for(Item item:heap.items)contents.add(heap.pos+":"+heap.type+":"+item.getClass().getName()+":"+item.level()+":"+item.quantity()+":"+item.cursed);
            Collections.sort(contents);records.add("FLOOR "+seed+" "+contents+" RNG "+Random.Long());
            Dungeon.hero.belongings.backpack.items.add(new com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier());
            Dungeon.hero.belongings.backpack.items.add(new com.shatteredpixel.shatteredpixeldungeon.items.bags.ScrollHolder());
            Dungeon.hero.belongings.backpack.items.add(new com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster());
            Dungeon.LimitedDrops.SCROLL_HOLDER.drop();Dungeon.LimitedDrops.POTION_BANDOLIER.drop();Dungeon.LimitedDrops.MAGICAL_HOLSTER.drop();
            Dungeon.depth=11;ArrayList<String> shop=new ArrayList<>();
            for(Item item:Shop.stock())shop.add(item.getClass().getName()+":"+item.level()+":"+item.quantity()+":"+item.cursed+":"+item.isIdentified());
            records.add("SHOP "+seed+" "+shop+" RNG "+Random.Long());
            }finally{Random.popGenerator();}
        }
        return records;
        }finally{guideIntroState(priorIntro);}
    }
}

