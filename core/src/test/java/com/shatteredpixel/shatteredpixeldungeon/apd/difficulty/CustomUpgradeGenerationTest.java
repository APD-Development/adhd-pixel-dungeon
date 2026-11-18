package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Random;
import org.junit.*;
import static org.junit.Assert.*;
public class CustomUpgradeGenerationTest {
    private CustomDungeonRules preferences;
    private int challenge,difficulty; private boolean intro,recording;
    private HeroClass selected;
    @Before public void setup() {
        HeadlessTestSupport.start(); preferences=SPDSettings.apdCustomRules(); challenge=SPDSettings.challenges();
        difficulty=SPDSettings.apdDifficulty();intro=SPDSettings.intro();recording=SPDSettings.apdRecording();selected=GamesInProgress.selectedClass;
        SPDSettings.apdDifficulty(3);SPDSettings.intro(false);SPDSettings.apdRecording(false);
    }
    @After public void cleanup() {
        SPDSettings.apdCustomRules(preferences);SPDSettings.challenges(challenge);SPDSettings.apdDifficulty(difficulty);
        SPDSettings.intro(intro);SPDSettings.apdRecording(recording);GamesInProgress.selectedClass=selected;
        Dungeon.apdRestoreCustomRules(new com.watabou.utils.Bundle());
    }
    @Test public void everyCountSchedulesExactlyAcrossFiveRegionsAndNeverOnBossFloors() {
        for(int n=1;n<=60;n++) {
            UpgradeScrollPlan plan=new UpgradeScrollPlan(n);int sum=0;
            for(int region=0;region<5;region++) {
                int subtotal=0;
                for(int floor=region*5+1;floor<=region*5+5;floor++) {
                    int count=plan.floorCount(floor);
                    if(floor%5==0)assertEquals(0,count);
                    subtotal+=count;
                }
                assertEquals(plan.regionCount(region),subtotal);sum+=subtotal;
            }
            assertEquals(n,sum);assertEquals(0,plan.floorCount(26));
        }
    }
    @Test public void neutralPredicateAndNextRngValuesMatchOriginalForEveryFloorAndCounter() {
        SPDSettings.apdCustomRules(CustomDungeonRules.defaults());GamesInProgress.selectedClass=HeroClass.WARRIOR;Dungeon.init();
        for(int depth=1;depth<=24;depth++) if(depth%5!=0) for(int count=0;count<=15;count++) {
            Dungeon.depth=depth;Dungeon.LimitedDrops.UPGRADE_SCROLLS.count=count;
            Random.pushGenerator(34673);boolean expected;long next;
            try{expected=Dungeon.souNeeded();next=Random.Long();}finally{Random.popGenerator();}
            Random.pushGenerator(34673);
            try{assertEquals(expected?1:0,CustomGenerationHooks.upgradeScrollsForFloor());assertEquals(next,Random.Long());}
            finally{Random.popGenerator();}
        }
    }
    @Test public void allSixtyCountsActuallyGenerateWithAndWithoutForbiddenRunes() {
        HeroClass[] classes=HeroClass.values();
        for(int n=1;n<=60;n++)for(int mask:new int[]{0,Challenges.NO_SCROLLS}) {
            SPDSettings.apdCustomRules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,n));
            SPDSettings.challenges(mask);GamesInProgress.selectedClass=classes[(n-1)%classes.length];
            Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.customSeedText="";Dungeon.seed=823451L+n;
            Dungeon.init();int found=0;
            for(int depth=1;depth<=24;depth++) {
                Dungeon.depth=depth;Dungeon.branch=0;Dungeon.level=Dungeon.newLevel();Level level=Dungeon.level;
                for(Heap heap:level.heaps.valueList())for(Item item:heap.items)if(item instanceof ScrollOfUpgrade) {
                    assertTrue("Upgrade placed off normal terrain",level.passable[heap.pos]||level.avoid[heap.pos]);
                    found+=item.quantity();
                }
                for(Mob mob:level.mobs)if(mob instanceof Mimic && ((Mimic)mob).items!=null)
                    for(Item item:((Mimic)mob).items)if(item instanceof ScrollOfUpgrade)found+=item.quantity();
            }
            assertEquals("Selected count "+n+", challenge "+mask,mask==0?n:(n+1)/2,found);
        }
    }
}
