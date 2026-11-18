package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.*;
import com.watabou.utils.*;
import com.watabou.utils.Random;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class LootScrollGenerationTest {
    private boolean gathering(Item item){return item instanceof ScrollOfGathering || item instanceof ScrollOfForwarding;}
    @Test public void bothScrollsAreIdentifiedAtRunStartAndAfterReloadForAllSixClasses()throws Exception {
        for(com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass type:com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.values()){
            com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness.generate(918271,0,type,
                    com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness.Context.VETERAN,1);
            for(Scroll scroll:new Scroll[]{new ScrollOfGathering(),new ScrollOfForwarding()}){
                assertTrue(type+" starts knowing "+scroll.name(),scroll.isKnown());
                assertTrue(type+" starts with identified "+scroll.name(),scroll.isIdentified());
                assertFalse(scroll.desc().contains("unknown_desc"));
                Bundle save=new Bundle();save.put("scroll",scroll);
                java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();Bundle.write(save,bytes);
                Scroll loaded=(Scroll)Bundle.read(new java.io.ByteArrayInputStream(bytes.toByteArray())).get("scroll");
                assertEquals(scroll.name(),loaded.name());assertEquals(scroll.desc(),loaded.desc());
                assertEquals("Appearance survives a real bundle reload",scroll.image(),loaded.image());
                if (loaded instanceof ScrollOfGathering) {
                    assertEquals(com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.SCROLL_GATHERING,loaded.image());
                    loaded.reset();
                    assertEquals("Reset must retain Gathering artwork",com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.SCROLL_GATHERING,loaded.image());
                    assertEquals("Do not overlay the unrelated Teleportation badge",-1,loaded.icon);
                }
                assertTrue(type+" still knows "+loaded.name(),loaded.isKnown());
                assertTrue(loaded.isIdentified());
            }
        }
    }
    @Test public void approvedDeckWeightsMatchTeleportationInBothDecks(){
        Generator.Category cat=Generator.Category.SCROLL;
        assertEquals(ScrollOfGathering.class,cat.classes[12]);
        assertEquals(cat.defaultProbs[5],cat.defaultProbs[12],0);
        assertEquals(cat.defaultProbs2[5],cat.defaultProbs2[12],0);
    }
    @Test public void allDepthsRespectCutoffThroughBothRandomDrawSystems(){
        HeadlessTestSupport.start();int oldDepth=Dungeon.depth;
        try{
            Random.pushGenerator(77128L);
            for(int depth=1;depth<=26;depth++){
                Dungeon.depth=depth;Generator.fullReset();int seen=0;
                for(int i=0;i<300;i++){
                    Item item=Generator.random(Generator.Category.SCROLL);
                    Item defaults=Generator.randomUsingDefaults(Generator.Category.SCROLL);
                    if(gathering(item))seen++;
                    if(gathering(defaults))seen++;
                    if(depth>19){assertFalse(gathering(item));assertFalse(gathering(defaults));}
                }
                if(depth<=19)assertTrue("Approved scroll is obtainable on depth "+depth,seen>0);
            }
        }finally{Random.popGenerator();Dungeon.depth=oldDepth;}
    }
    @Test public void explicitGenerationAlsoRejectsBothVariantsAfterNineteen(){
        HeadlessTestSupport.start();int oldDepth=Dungeon.depth;
        try{for(int depth=20;depth<=26;depth++){
            Dungeon.depth=depth;assertNull(Generator.random(ScrollOfGathering.class));assertNull(Generator.random(ScrollOfForwarding.class));
        }}finally{Dungeon.depth=oldDepth;}
    }
    @Test public void legacyTwelveEntryDeckRetainsAllExistingCounts(){
        HeadlessTestSupport.start();Generator.fullReset();Bundle save=new Bundle();Generator.storeInBundle(save);
        float[] original={0,2,1,1,0,1,1,0,1,1,0,0};save.put("scroll_probs",original);save.put("scroll_using_probs2",true);
        Generator.restoreFromBundle(save);assertEquals(13,Generator.Category.SCROLL.probs.length);
        for(int i=0;i<12;i++)assertEquals(original[i],Generator.Category.SCROLL.probs[i],0);
        assertEquals(2,Generator.Category.SCROLL.probs[12],0);
    }
    @Test public void exoticConversionUsesExistingSixEnergyRecipe(){
        HeadlessTestSupport.start();ArrayList<Item> input=new ArrayList<>();ScrollOfGathering regular=new ScrollOfGathering();regular.quantity(2);input.add(regular);
        ExoticScroll.ScrollToExotic recipe=new ExoticScroll.ScrollToExotic();assertTrue(recipe.testIngredients(input));assertEquals(6,recipe.cost(input));
        assertTrue(recipe.sampleOutput(input) instanceof ScrollOfForwarding);
        assertTrue(recipe.brew(input) instanceof ScrollOfForwarding);assertEquals(1,regular.quantity());
    }
    @Test public void currentThirteenScrollPoolIsSeedRepeatableForAllSixClasses()throws Exception {
        com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules previous=SPDSettings.apdCustomRules();
        try{
            SPDSettings.apdCustomRules(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.defaults());
            for(com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass type:com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.values()){
                java.util.List<com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness.LevelPrint> first=
                    com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness.generate(918271,0,type,com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness.Context.VETERAN,4);
                java.util.List<com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness.LevelPrint> second=
                    com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness.generate(918271,0,type,com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness.Context.VETERAN,4);
                for(int i=0;i<4;i++)assertEquals(type+" depth "+i,first.get(i).hash,second.get(i).hash);
            }
        }finally{SPDSettings.apdCustomRules(previous);}
    }
}
