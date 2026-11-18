package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.*;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

/** Replays the actual unchanged TEST285 loot code, not a reimplementation of its generator. */
public class RingOfWealthLootInvestigationTest {
    static final int[] LEVELS={0,1,3,5,10,15};
    static class Quiet extends ItemSprite { @Override public void drop(){} }
    static class Drops extends Level {
        final List<Item> items=new ArrayList<>();
        protected boolean build(){return true;} protected void createMobs(){} protected void createItems(){}
        @Override public Heap drop(Item item,int cell){
            items.add(item);Heap h=new Heap();h.drop(item);h.sprite=new Quiet();return h;
        }
    }
    static class BonusOnly extends Mob {
        BonusOnly(int role){maxLvl=100;pos=24;if(role==1)properties.add(Property.MINIBOSS);if(role==2)properties.add(Property.BOSS);}
    }
    private CustomDungeonRules oldRules;private HeroClass oldClass;
    private boolean oldIntro,oldRecording;private int oldDifficulty,oldChallenges,oldExotic;
    @Before public void setup(){
        HeadlessTestSupport.start();oldRules=SPDSettings.apdCustomRules();oldClass=GamesInProgress.selectedClass;
        oldIntro=SPDSettings.intro();oldRecording=SPDSettings.apdRecording();oldDifficulty=SPDSettings.apdDifficulty();
        oldChallenges=SPDSettings.challenges();oldExotic=SPDSettings.apdExoticChance();
        SPDSettings.intro(false);SPDSettings.apdRecording(false);SPDSettings.challenges(0);SPDSettings.apdExoticChance(0);
    }
    @After public void cleanup(){
        SPDSettings.apdCustomRules(oldRules);GamesInProgress.selectedClass=oldClass;SPDSettings.intro(oldIntro);
        SPDSettings.apdRecording(oldRecording);SPDSettings.apdDifficulty(oldDifficulty);SPDSettings.challenges(oldChallenges);
        SPDSettings.apdExoticChance(oldExotic);Dungeon.level=null;Dungeon.apdRestoreCustomRules(new Bundle());
    }
    private Drops init(int ringLevel,int depth,int difficulty,CustomDungeonRules rules){
        SPDSettings.apdDifficulty(difficulty);SPDSettings.apdCustomRules(rules);GamesInProgress.selectedClass=HeroClass.WARRIOR;
        Dungeon.seed=2851037;Dungeon.customSeedText="";Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.init();
        Dungeon.depth=depth;Dungeon.hero.lvl=1;Drops level=new Drops();level.setSize(7,7);Dungeon.level=level;
        if(ringLevel>=0){RingOfWealth ring=new RingOfWealth();ring.level(ringLevel);ring.cursed=false;ring.activate(Dungeon.hero);}
        return level;
    }
    private Drops init(int ringLevel,int depth){return init(ringLevel,depth,3,CustomDungeonRules.defaults());}
    private static boolean regularPotion(Item item){
        for(Class<?> c:Generator.Category.POTION.classes)if(item.getClass()==c)return true;return false;
    }
    private static String signature(Item item){return item.getClass().getSimpleName()+"/"+item.quantity()+"/"+item.level();}
    private static double[] tiers(int level){
        double low=Math.max(0,Math.min(1,.6-.04*level)),lowMid=Math.max(0,Math.min(1,.9-.02*level));
        return new double[]{low,Math.max(0,lowMid-low),1-lowMid};
    }
    private static double expectedHealingAmongRegularPotions(int level){
        double[] t=tiers(level);double generic=(t[0]+t[1]/6+t[2]/24)/4;
        return .2*generic/(generic+t[2]/4);
    }
    @Test public void weightedDefaultPotionDrawsIncludeEveryIntendedTypeAndAreSeedReproducible(){
        init(-1,1);float[] weights=Generator.Category.POTION.defaultProbsTotal;assertEquals(30f,sum(weights),0);
        int n=150000;int[] counts=new int[weights.length];long firstHash=0,secondHash=0;
        for(int pass=0;pass<2;pass++){
            Random.pushGenerator(2851701);
            try{for(int i=0;i<n;i++){Item item=Generator.randomUsingDefaults(Generator.Category.POTION);int at=Arrays.asList(Generator.Category.POTION.classes).indexOf(item.getClass());
                assertTrue(at>=0);if(pass==0){counts[at]++;firstHash=firstHash*31+at;}else secondHash=secondHash*31+at;
            }}finally{Random.popGenerator();}
        }
        assertEquals(firstHash,secondHash);
        for(int i=0;i<counts.length;i++){
            double expected=n*weights[i]/30.0;
            if(weights[i]==0)assertEquals(0,counts[i]);else assertTrue("Weighted potion "+i+" observed="+counts[i]+" expected="+expected,
                Math.abs(counts[i]-expected)<6*Math.sqrt(expected*(1-weights[i]/30.0))+10);
            System.out.println("ROW_POTION class="+Generator.Category.POTION.classes[i].getSimpleName()+" weight="+weights[i]+"/30 observed="+counts[i]+"/"+n);
        }
    }
    private static float sum(float[] values){float total=0;for(float v:values)total+=v;return total;}
    @Test public void rewardQualityAndHealingShareFollowActualTierMixtureAtSixUpgradeLevels()throws Exception{
        java.lang.reflect.Field tier=RingOfWealth.class.getDeclaredField("latestDropTier");tier.setAccessible(true);
        int n=120000;
        for(int level:LEVELS){
            init(level,11);int potions=0,heals=0,healUnits=0,streak=0,quartets=0;Set<Class<?>> seen=new HashSet<>();int[] observedTiers=new int[3];Map<String,Integer> potionCounts=new TreeMap<>();
            Random.pushGenerator(2852200+level);
            try{for(int i=0;i<n;i++){
                Item item=RingOfWealth.genConsumableDrop(level);seen.add(item.getClass());observedTiers[tier.getInt(null)-1]++;
                if(regularPotion(item)){potionCounts.merge(item.getClass().getSimpleName(),1,Integer::sum);potions++;if(item instanceof PotionOfHealing){heals++;healUnits+=item.quantity();if(++streak>=4)quartets++;}else streak=0;}
            }}finally{Random.popGenerator();}
            double[] probabilities=tiers(level);for(int j=0;j<3;j++){double expectedTier=n*probabilities[j];assertTrue("Reward tier "+j+" level "+level,Math.abs(observedTiers[j]-expectedTier)<6*Math.sqrt(expectedTier*(1-probabilities[j]))+20);}
            System.out.println("ROW_TIERS level="+level+" expected="+Arrays.toString(probabilities)+" observed="+Arrays.toString(observedTiers)+" potionStacks="+potionCounts);
            double p=expectedHealingAmongRegularPotions(level),expected=potions*p;
            assertTrue("Level "+level+" healing share",Math.abs(heals-expected)<6*Math.sqrt(expected*(1-p))+15);
            assertTrue(seen.contains(PotionOfHealing.class));assertTrue(seen.contains(PotionOfExperience.class));assertTrue(seen.size()>15);
            System.out.println("ROW_CONSUMABLE level="+level+" draws="+n+" regularPotionStacks="+potions+" healingStacks="+heals+" healingUnits="+healUnits+" expectedHealingShare="+p+" observedShare="+(heals/(double)potions)+" overlappingFourHealingWindows="+quartets+" expectedFourWindowProbability="+Math.pow(p,4));
        }
    }
    @Test public void actualMobBonusPathUsesIndependentRewardsAndSameCadenceAcrossLevelsRegionsAndRoles(){
        long totalKills=0,totalTries=0;
        for(int ring:LEVELS){
            long rewards=0,tries=0;
            for(int depth:new int[]{1,6,11,16,21})for(int role=0;role<3;role++){
                Drops level=init(ring,depth);int kills=role==0?4000:role==1?400:160,weight=role==0?1:role==1?5:15;int count=0;
                Random.pushGenerator(2853000+depth*17+role);
                try{for(int i=0;i<kills;i++){level.items.clear();new BonusOnly(role).rollToDropLoot();count+=level.items.size();}}
                finally{Random.popGenerator();}
                rewards+=count;tries+=(long)kills*weight;totalKills+=kills;totalTries+=(long)kills*weight;
                System.out.println("ROW_BONUS level="+ring+" depth="+depth+" role="+role+" eligibleKills="+kills+" weightedTries="+(kills*weight)+" rewards="+count);
            }
            assertTrue("Mean 10 weighted tries per reward, ring="+ring,Math.abs(rewards-tries/10.0)<tries/10.0*.08+20);
            System.out.println("ROW_BONUS_TOTAL level="+ring+" tries="+tries+" rewards="+rewards+" expectedMean="+tries/10.0);
        }
        assertEquals(136800L,totalKills);assertEquals(252000L,totalTries);
    }
    @Test public void ordinaryEnemyDropsRetainSourceTablesCapsAndIdentificationWithAndWithoutWealth(){
        for(int ring:new int[]{-1,0,3,10})for(int type=0;type<8;type++){
            Drops level=init(ring,1);
            if(ring>=0){Buff.affect(Dungeon.hero,RingOfWealth.TriesToDropTracker.class).countUp(1000000);Buff.affect(Dungeon.hero,RingOfWealth.DropsToEquipTracker.class).countUp(7);}
            int heals=0,otherPotions=0,normal=0;
            Random.pushGenerator(2854000+type);
            try{for(int i=0;i<5000;i++){
                Mob mob=type==0?new Swarm():type==1?new Bat():type==2?new Necromancer():type==3?new Warlock():type==4?new DM100():type==5?new Skeleton():type==6?new Crab():new Rat();
                mob.pos=24;level.items.clear();mob.rollToDropLoot();
                for(Item item:level.items){normal++;if(item instanceof PotionOfHealing){heals++;if(type<3)assertTrue("APD identifies normal source-specific healing",item.isIdentified());}else if(regularPotion(item))otherPotions++;}
            }}finally{Random.popGenerator();}
            if(type<3){assertEquals(type==0?5:type==1?7:6,heals);assertEquals(0,otherPotions);}
            if(type==3){assertTrue(heals<=7);assertTrue(otherPotions>100);}
            if(type>=4)assertEquals(0,heals+otherPotions);
            System.out.println("ROW_NORMAL level="+ring+" enemy="+type+" eligibleKills=5000 ordinaryStacks="+normal+" healing="+heals+" otherRegularPotions="+otherPotions+" bonusSuppressedByExplicitTestCounter=true");
        }
    }
    @Test public void ordinaryChanceMultiplierChangesFrequencyNotTheSourcePotionType(){
        for(int ring:new int[]{-1,0,1,3,5,10,15}){
            init(ring,6);double multiplier=ring<0?1:Math.pow(1.2,ring+1);
            assertEquals(multiplier,RingOfWealth.dropChanceMultiplier(Dungeon.hero),.00001);
            assertEquals(multiplier/6,new Swarm().lootChance(),.00001);
            assertEquals(.1667*multiplier,new Bat().lootChance(),.00002);
            assertEquals(.2*multiplier,new Necromancer().lootChance(),.00002);
            assertEquals(.5*multiplier,new Warlock().lootChance(),.00002);
            assertTrue(new Swarm().createLoot() instanceof PotionOfHealing);
            assertTrue(new Bat().createLoot() instanceof PotionOfHealing);
            assertTrue(new Necromancer().createLoot() instanceof PotionOfHealing);
        }
    }
    private List<String> consumables(long seed,int n){
        List<String> result=new ArrayList<>();Random.pushGenerator(seed);
        try{for(int i=0;i<n;i++)result.add(signature(RingOfWealth.genConsumableDrop(3)));}
        finally{Random.popGenerator();}return result;
    }
    @Test public void allDifficultyPresetsAndEighteenCustomRulesDoNotRewriteConsumableDraws(){
        init(3,11);List<String> baseline=consumables(2856000,1000);
        for(int difficulty=1;difficulty<=5;difficulty++){init(3,11,difficulty,CustomDungeonRules.defaults());assertEquals(baseline,consumables(2856000,1000));}
        for(CustomDungeonRules.Slider slider:CustomDungeonRules.Slider.values())for(int value:new int[]{slider.min,slider.max}){
            init(3,11,3,CustomDungeonRules.defaults().with(slider,value));assertEquals(slider+"="+value,baseline,consumables(2856000,1000));
            PotionOfHealing potion=new PotionOfHealing();assertEquals(Collections.singletonList(potion),CustomEquipmentHooks.adjust(potion,()->{throw new AssertionError("Potions must not be expanded as equipment");}));
        }
        System.out.println("ROW_RULES five difficulty presets; all18 rules at both limits; 41000 exact seeded consumable draws compared");
    }
    @Test public void gooHasNoOrdinaryHealingGuaranteeAndBossBonusRewardsAreSeparate(){
        Drops level=init(-1,5);assertNull(new Goo().createLoot());
        for(int i=0;i<100;i++){Goo goo=new Goo();goo.pos=24;goo.rollToDropLoot();}
        assertTrue(level.items.isEmpty());
        level=init(0,5);int healing=0,other=0;
        Random.pushGenerator(2857000);
        try{for(int i=0;i<2000;i++){level.items.clear();Goo goo=new Goo();goo.pos=24;goo.rollToDropLoot();for(Item item:level.items){if(item instanceof PotionOfHealing)healing++;else other++;}}}
        finally{Random.popGenerator();}
        assertTrue(healing>0);assertTrue(other>healing*5);
        System.out.println("ROW_GOO bossKills=2000 ordinaryHealing=0 bonusHealing="+healing+" otherBonusRewards="+other+" bossTryWeight=15");
    }
    @Test public void bonusCountersAndRingLevelSurviveBundlePersistenceWithoutChangingTheRewardSequence(){
        init(3,16);Random.pushGenerator(2858000);
        try{for(int i=0;i<37;i++)RingOfWealth.tryForBonusDrop(Dungeon.hero,1);}
        finally{Random.popGenerator();}
        Bundle tries=new Bundle(),equip=new Bundle(),generator=new Bundle();
        Dungeon.hero.buff(RingOfWealth.TriesToDropTracker.class).storeInBundle(tries);
        Dungeon.hero.buff(RingOfWealth.DropsToEquipTracker.class).storeInBundle(equip);Generator.storeInBundle(generator);
        List<String> expected=bonusSequence(2858001);
        Random.pushGenerator(3);try{Generator.restoreFromBundle(generator);}finally{Random.popGenerator();}
        Dungeon.hero=new Hero();RingOfWealth ring=new RingOfWealth();Bundle savedRing=new Bundle();ring.level(3);ring.storeInBundle(savedRing);
        RingOfWealth restoredRing=new RingOfWealth();restoredRing.restoreFromBundle(savedRing);restoredRing.activate(Dungeon.hero);
        RingOfWealth.TriesToDropTracker t=new RingOfWealth.TriesToDropTracker();t.restoreFromBundle(tries);t.attachTo(Dungeon.hero);
        RingOfWealth.DropsToEquipTracker e=new RingOfWealth.DropsToEquipTracker();e.restoreFromBundle(equip);e.attachTo(Dungeon.hero);
        assertEquals(3,restoredRing.level());assertEquals(expected,bonusSequence(2858001));
    }
    private List<String> bonusSequence(long seed){
        List<String> result=new ArrayList<>();Random.pushGenerator(seed);
        try{for(int i=0;i<500;i++){ArrayList<Item> items=RingOfWealth.tryForBonusDrop(Dungeon.hero,1);if(items!=null)for(Item item:items)result.add(i+":"+signature(item));}}
        finally{Random.popGenerator();}return result;
    }
}
