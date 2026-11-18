package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.*;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import static org.junit.Assert.*;

public class ProgressionPickupsTest {
    private Hero previousHero;
    private com.shatteredpixel.shatteredpixeldungeon.levels.Level previousLevel;
    private int previousDepth;
    private Bundle previousStatistics;
    @Before public void setup(){
        HeadlessTestSupport.start();previousHero=Dungeon.hero;previousLevel=Dungeon.level;previousDepth=Dungeon.depth;
        previousStatistics=new Bundle();Statistics.storeInBundle(previousStatistics);
        Statistics.reset();Dungeon.hero=null;Dungeon.depth=1;
    }
    @After public void restore(){
        Dungeon.hero=previousHero;Dungeon.level=previousLevel;Dungeon.depth=previousDepth;
        Statistics.restoreFromBundle(previousStatistics);
    }
    private static <T extends Item>T origin(T item,int floor,boolean guaranteed){
        ProgressionPickups.mark(item,floor,guaranteed);return item;
    }
    @Test public void strengthPairsAreExactlyFirstTwoAndLastTwoNonBossFloors(){
        for(int r=0;r<5;r++){
            assertEquals(r,ProgressionPickups.region(1+5*r));
            assertEquals(r*2,ProgressionPickups.pair(1+5*r));
            assertEquals(r*2,ProgressionPickups.pair(2+5*r));
            assertEquals(r*2+1,ProgressionPickups.pair(3+5*r));
            assertEquals(r*2+1,ProgressionPickups.pair(4+5*r));
            assertEquals(-1,ProgressionPickups.pair(5+5*r));
        }
        assertEquals(-1,ProgressionPickups.pair(0));assertEquals(-1,ProgressionPickups.pair(26));
    }
    @Test public void everyRegionCountsFirstCollectionOnlyAndDoesNotCountInventoryQuantity(){
        for(int r=0;r<5;r++){
            ScrollOfUpgrade scroll=origin(new ScrollOfUpgrade(),r*5+2,false);scroll.quantity(1);
            ProgressionPickups.claim(scroll);ProgressionPickups.claim(scroll);
            PotionOfStrength potion=origin(new PotionOfStrength(),r*5+3,true);
            assertFalse(ProgressionPickups.collectedPair(r*2+1));ProgressionPickups.claim(potion);
            assertEquals(1,ProgressionPickups.count(false,r));assertEquals(1,ProgressionPickups.count(true,r));
            assertTrue(ProgressionPickups.collectedPair(r*2+1));assertFalse(ProgressionPickups.collectedPair(r*2));
        }
    }
    @Test public void bonusPotionsCannotMarkTheGuaranteedPairCollected(){
        ProgressionPickups.claim(origin(new PotionOfStrength(),1,false));
        assertEquals(1,ProgressionPickups.count(true,0));assertFalse(ProgressionPickups.collectedPair(0));
        ProgressionPickups.claim(origin(new PotionOfStrength(),2,true));
        assertEquals(2,ProgressionPickups.count(true,0));assertTrue(ProgressionPickups.collectedPair(0));
        assertFalse(ProgressionPickups.collectedPair(1));
    }
    @Test public void originalRegionAndPairSurviveMergingSplittingAndMovingToAnotherRegion(){
        PotionOfStrength a=origin(new PotionOfStrength(),2,true),b=origin(new PotionOfStrength(),13,true);
        a.merge(b);assertEquals(2,a.quantity());assertEquals(0,b.quantity());
        Item piece=a.split(1);assertNotNull(piece);Dungeon.depth=21;
        ProgressionPickups.claim(piece);ProgressionPickups.claim(a);ProgressionPickups.claim(b);
        assertEquals(1,ProgressionPickups.count(true,0));assertEquals(1,ProgressionPickups.count(true,2));
        assertEquals(0,ProgressionPickups.count(true,4));
        assertTrue(ProgressionPickups.collectedPair(0));assertTrue(ProgressionPickups.collectedPair(5));
    }
    @Test public void fullBagDoesNotCountAndSuccessfulBagPickupAndRepickCountOnce(){
        Dungeon.hero=new Hero();
        PotionOfStrength potion=origin(new PotionOfStrength(),7,true);
        Bag full=new Bag(){@Override public boolean canHold(Item item){return false;}};
        assertFalse(potion.collect(full));assertEquals(0,ProgressionPickups.count(true,1));
        assertTrue(potion.collect(Dungeon.hero.belongings.backpack));
        assertEquals(1,ProgressionPickups.count(true,1));
        potion.detachAll(Dungeon.hero.belongings.backpack);
        Dungeon.depth=18;assertTrue(potion.collect(Dungeon.hero.belongings.backpack));
        assertEquals(1,ProgressionPickups.count(true,1));assertEquals(0,ProgressionPickups.count(true,3));
    }
    @Test public void mergingIntoAlreadyCarriedStackRecordsIncomingOriginsExactlyOnce(){
        Dungeon.hero=new Hero();
        ScrollOfUpgrade a=origin(new ScrollOfUpgrade(),1,false),b=origin(new ScrollOfUpgrade(),11,false);
        assertTrue(a.collect(Dungeon.hero.belongings.backpack));assertTrue(b.collect(Dungeon.hero.belongings.backpack));
        assertEquals(2,a.quantity());assertEquals(1,ProgressionPickups.count(false,0));assertEquals(1,ProgressionPickups.count(false,2));
        assertTrue(a.collect(Dungeon.hero.belongings.backpack));ProgressionPickups.claim(b);
        assertEquals(1,ProgressionPickups.count(false,0));assertEquals(1,ProgressionPickups.count(false,2));
    }
    @Test public void unrelatedContainersDoNotCountHeroCollections(){
        PotionOfStrength potion=origin(new PotionOfStrength(),3,true);
        assertTrue(potion.collect(new Bag()));assertEquals(0,ProgressionPickups.count(true,0));
    }
    @Test public void realBundleReloadPreservesPendingOriginsAndPermanentCounters()throws Exception{
        ProgressionPickups.claim(origin(new ScrollOfUpgrade(),16,false));
        PotionOfStrength pending=origin(new PotionOfStrength(),24,true);
        Bundle save=new Bundle();save.put("potion",pending);Bundle stats=new Bundle();Statistics.storeInBundle(stats);save.put("statistics",stats);
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();Bundle.write(save,bytes);
        Bundle restored=Bundle.read(new java.io.ByteArrayInputStream(bytes.toByteArray()));
        Statistics.reset();Statistics.restoreFromBundle(restored.getBundle("statistics"));
        Item loaded=(Item)restored.get("potion");ProgressionPickups.claim(loaded);ProgressionPickups.claim(loaded);
        assertEquals(1,ProgressionPickups.count(false,3));assertEquals(1,ProgressionPickups.count(true,4));
        assertTrue(ProgressionPickups.collectedPair(9));
        Bundle again=new Bundle();loaded.storeInBundle(again);PotionOfStrength reloaded=new PotionOfStrength();reloaded.restoreFromBundle(again);
        ProgressionPickups.claim(reloaded);assertEquals(1,ProgressionPickups.count(true,4));
    }
    @Test public void legacySavesNeverInventMissingPotionsAndKeepPartialHistoryAcrossReloads(){
        Statistics.restoreFromBundle(new Bundle());
        assertFalse(ProgressionPickups.knownPair(0));
        Bundle legacy=new Bundle();legacy.put("quantity",1);
        PotionOfStrength restored=new PotionOfStrength();restored.restoreFromBundle(legacy);
        ProgressionPickups.prepare(restored);ProgressionPickups.claim(restored);
        assertEquals(0,ProgressionPickups.count(true,0));
        PotionOfStrength future=origin(new PotionOfStrength(),7,true);
        assertTrue(ProgressionPickups.knownPair(2));assertFalse(ProgressionPickups.collectedPair(2));
        ProgressionPickups.claim(future);Bundle stats=new Bundle();Statistics.storeInBundle(stats);Statistics.reset();Statistics.restoreFromBundle(stats);
        assertFalse(ProgressionPickups.knownPair(0));assertTrue(ProgressionPickups.collectedPair(2));
        assertTrue(ProgressionPickups.info(true).contains("Unknown (older save)"));
    }
    @Test public void trackingDoesNotConsumeGenerationRng(){
        Random.pushGenerator(9121);long expected;try{expected=Random.Long();}finally{Random.popGenerator();}
        Random.pushGenerator(9121);try{
            ProgressionPickups.claim(origin(new PotionOfStrength(),4,true));
            assertEquals(expected,Random.Long());
        }finally{Random.popGenerator();}
    }
    @Test public void newlyCreatedUnplacedItemsRemainTrackableAfterSaving(){
        ScrollOfUpgrade reward=new ScrollOfUpgrade();Bundle save=new Bundle();reward.storeInBundle(save);
        ScrollOfUpgrade restored=new ScrollOfUpgrade();restored.restoreFromBundle(save);
        assertFalse(restored.apdProgressionLegacy);
        ProgressionPickups.mark(restored,8,false);ProgressionPickups.claim(restored);
        assertEquals(1,ProgressionPickups.count(false,1));
    }
    @Test public void actualSewersGenerationTagsBothGuaranteedPairsAndAllThreeUpgradeScrolls()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules previous=SPDSettings.apdCustomRules();
        try{
            SPDSettings.apdCustomRules(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults());
            LevelGenerationHarness.generate(918271,0,HeroClass.WARRIOR,LevelGenerationHarness.Context.VETERAN,1);
            for(int depth=1;depth<=4;depth++){
                assertEquals(depth,Dungeon.depth);
                for(com.shatteredpixel.shatteredpixeldungeon.items.Heap heap:Dungeon.level.heaps.valueList())
                    for(Item item:heap.items)if(ProgressionPickups.tracked(item)){
                        assertNotNull("Actual generated resources carry provenance",item.apdProgressionOrigins);
                        ProgressionPickups.claim(item);
                    }
                if(depth<4){Dungeon.depth++;Dungeon.level=Dungeon.newLevel();}
            }
            assertEquals(3,ProgressionPickups.count(false,0));assertEquals(2,ProgressionPickups.count(true,0));
            assertTrue(ProgressionPickups.collectedPair(0));assertTrue(ProgressionPickups.collectedPair(1));
        }finally{SPDSettings.apdCustomRules(previous);}
    }
    @Test public void allSixClassesSeeKnownDescriptionsWithActualLineBreaks()throws Exception{
        for(HeroClass heroClass:HeroClass.values()){
            LevelGenerationHarness.generate(918271,0,heroClass,LevelGenerationHarness.Context.VETERAN,1);
            PotionOfStrength potion=new PotionOfStrength();potion.setKnown();
            ScrollOfUpgrade scroll=new ScrollOfUpgrade();scroll.setKnown();
            PotionOfHealing heal=new PotionOfHealing();heal.setKnown();
            assertTrue(potion.info().contains("Floors 1–2: Not collected"));
            assertTrue(potion.info().contains("Floors 3–4: Not collected"));
            assertTrue(scroll.info().contains("Scrolls of Upgrade collected by region"));
            assertFalse(potion.info().contains("\\n"));assertFalse(scroll.info().contains("\\n"));assertFalse(heal.info().contains("\\n"));
        }
    }
    @Test public void legacyGoldenComparisonOmitsOnlyOptionalMetadataAndNeverMutatesTheSavedLevel()throws Exception{
        boolean previous=LevelGenerationHarness.historicalTutorialFingerprint;
        try{
            LevelGenerationHarness.historicalTutorialFingerprint=false;
            LevelGenerationHarness.generate(918271,0,HeroClass.WARRIOR,LevelGenerationHarness.Context.VETERAN,1);
            String live=LevelGenerationHarness.canonicalJson(Dungeon.level);
            assertTrue("Actual saved levels retain resource origins",live.contains("apd_progression_origins_v1"));
            String justBookkeepingRemoved=live.replaceAll(",\\\"apd_progression_origins_v1\\\":\\[(?:-?\\d+(?:,-?\\d+)*)?\\]", "");
            assertTrue("Actual saved equipment retains original location metadata", live.contains("apd_item_origin_v1"));
            justBookkeepingRemoved = justBookkeepingRemoved.replaceAll(",\\\"apd_item_origin_v1\\\":\\{[^{}]*\\}", "");
            justBookkeepingRemoved = justBookkeepingRemoved.replaceAll(",\\\"apd_item_upgrade_range_v1\\\":\\[-?\\d+,-?\\d+\\]", "");
            assertFalse(justBookkeepingRemoved.contains("apd_item_origin_v1"));
            assertFalse(justBookkeepingRemoved.contains("apd_item_upgrade_range_v1"));
            assertNotEquals(live,justBookkeepingRemoved);
            LevelGenerationHarness.historicalTutorialFingerprint=true;
            assertEquals("All original map/item/mob fields are still exact",justBookkeepingRemoved,
                    LevelGenerationHarness.canonicalJson(Dungeon.level));
            LevelGenerationHarness.historicalTutorialFingerprint=false;
            assertEquals("Historical comparison never edits live item save data",live,
                    LevelGenerationHarness.canonicalJson(Dungeon.level));
        }finally{LevelGenerationHarness.historicalTutorialFingerprint=previous;}
    }
}
