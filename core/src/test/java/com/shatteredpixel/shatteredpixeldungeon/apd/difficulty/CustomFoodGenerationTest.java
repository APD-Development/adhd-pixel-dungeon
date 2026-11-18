package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;
public class CustomFoodGenerationTest {
    private CustomDungeonRules prefs;private int challenge,difficulty,depth;private boolean intro,recording;private HeroClass selected;
    private static class Capture extends Level {
        java.util.List<Item> generated=new ArrayList<>();
        protected boolean build(){return true;}protected void createMobs(){}protected void createItems(){}
        @Override public void addItemToSpawn(Item item){generated.add(item);}
    }
    @Before public void setup(){HeadlessTestSupport.start();prefs=SPDSettings.apdCustomRules();challenge=SPDSettings.challenges();
        difficulty=SPDSettings.apdDifficulty();depth=Dungeon.depth;intro=SPDSettings.intro();recording=SPDSettings.apdRecording();selected=GamesInProgress.selectedClass;
        SPDSettings.intro(false);SPDSettings.apdRecording(false);SPDSettings.apdDifficulty(3);GamesInProgress.selectedClass=HeroClass.WARRIOR;
    }
    @After public void restore(){SPDSettings.apdCustomRules(prefs);SPDSettings.challenges(challenge);SPDSettings.apdDifficulty(difficulty);
        SPDSettings.intro(intro);SPDSettings.apdRecording(recording);GamesInProgress.selectedClass=selected;Dungeon.depth=depth;Dungeon.apdRestoreCustomRules(new Bundle());}
    private void rules(int p){SPDSettings.apdCustomRules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.FOOD,p));Dungeon.init();}
    @Test public void allFoodValuesAllocateTheExactRegionalBaseSupplyWithoutInventoryInsertion(){
        for(int p=25;p<=200;p+=25) {
            rules(p);int total=0;int inventory=Dungeon.hero.belongings.backpack.items.size();
            for(int region=0;region<5;region++) {
                int count=0;
                for(int floor=0;floor<4;floor++) {
                    Dungeon.depth=region*5+floor+1;Capture level=new Capture();CustomGenerationHooks.addFloorFood(level,false);
                    count+=level.generated.size();assertEquals(CustomGenerationHooks.foodForFloor(Dungeon.depth),level.generated.size());
                    if(p==25){assertEquals(floor==0?1:0,level.generated.size());if(floor==0)assertEquals(Food.class,level.generated.get(0).getClass());}
                }
                assertEquals(p/25,count);total+=count;
                assertEquals(0,CustomGenerationHooks.foodForFloor(region*5+5));
            }
            assertEquals(p/5,total);assertEquals(inventory,Dungeon.hero.belongings.backpack.items.size());
        }
    }
    @Test public void neutralBaseAndLargeFloorBonusKeepOriginalGeneratorAndRng(){
        rules(100);Dungeon.depth=12;
        for(boolean large:new boolean[]{false,true}) {
            Bundle generator=new Bundle();Generator.storeInBundle(generator);
            Random.pushGenerator(98523);Item expected;long next;
            try{expected=Generator.random(Generator.Category.FOOD);next=Random.Long();}finally{Random.popGenerator();}
            Generator.restoreFromBundle(generator);
            Capture level=new Capture();Random.pushGenerator(98523);
            try{CustomGenerationHooks.addFloorFood(level,large);assertEquals(next,Random.Long());}finally{Random.popGenerator();}
            assertEquals(1,level.generated.size());assertEquals(expected.getClass(),level.generated.get(0).getClass());
        }
    }
    @Test public void minimumActuallyPlacesStandardRegionalRationsForAllClassesIncludingFamine(){
        for(int challenge:new int[]{0,Challenges.NO_FOOD})for(HeroClass heroClass:HeroClass.values()) {
            SPDSettings.challenges(challenge);GamesInProgress.selectedClass=heroClass;rules(25);Dungeon.seed=938735;
            int inventory=Dungeon.hero.belongings.backpack.items.size();
            for(int region=0;region<5;region++) {
                Dungeon.depth=region*5+1;Dungeon.branch=0;Dungeon.level=Dungeon.newLevel();
                boolean found=false;
                for(Heap heap:Dungeon.level.heaps.valueList())for(Item item:heap.items)if(item.getClass()==Food.class)found=true;
                for(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob:Dungeon.level.mobs)
                    if(mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic && ((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic)mob).items!=null)
                        for(Item item:((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic)mob).items)if(item.getClass()==Food.class)found=true;
                assertTrue("Region "+region+", "+heroClass+", challenge "+challenge+" has its standard ration",found);
                assertEquals(inventory,Dungeon.hero.belongings.backpack.items.size());
            }
        }
    }
}
