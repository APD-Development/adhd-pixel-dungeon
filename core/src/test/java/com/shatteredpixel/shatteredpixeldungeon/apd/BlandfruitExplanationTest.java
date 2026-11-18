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
package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.GrassDewPickupTest;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.sprites.*;
import com.watabou.utils.*;
import com.watabou.utils.Random;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class BlandfruitExplanationTest {
    private Hero oldHero,hero;private com.shatteredpixel.shatteredpixeldungeon.levels.Level oldLevel;private int oldChallenges;
    private Object oldHandler,oldNotes;private Field notesField;private com.watabou.noosa.Game oldGame;private Bundle oldRules;private HashSet<Class> oldDiscovered;
    private final Map<Field,Object> badgeFields=new HashMap<>();
    private final Map<Catalog,Map<Field,Map>> catalogFields=new HashMap<>();private Field journalDirty;private boolean oldJournalDirty;
    public static class Quiet extends HeroSprite {
        @Override public com.watabou.noosa.particles.Emitter centerEmitter(){return null;}
        @Override public void add(CharSprite.State state){} @Override public void remove(CharSprite.State state){}
        @Override public void sprint(float speed){} @Override public void showStatus(int color,String text,Object...args){}
        @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){}
    }
    @Before public void setup()throws Exception{
        HeadlessTestSupport.start();notesField=com.shatteredpixel.shatteredpixeldungeon.journal.Notes.class.getDeclaredField("records");notesField.setAccessible(true);oldNotes=notesField.get(null);com.shatteredpixel.shatteredpixeldungeon.journal.Notes.reset();oldGame=com.watabou.noosa.Game.instance;oldHero=Dungeon.hero;oldLevel=Dungeon.level;oldChallenges=Dungeon.challenges;Dungeon.challenges=0;
        oldHandler=Potion.apdHandler();Random.pushGenerator(831);try{Potion.initColors();}finally{Random.popGenerator();}
        oldDiscovered=Statistics.itemTypesDiscovered;Statistics.itemTypesDiscovered=new HashSet<>();
        oldRules=new Bundle();Dungeon.apdStoreCustomRules(oldRules);Dungeon.apdRestoreCustomRules(new Bundle());
        for(String name:new String[]{"global","local","saveNeeded"}){Field f=Badges.class.getDeclaredField(name);f.setAccessible(true);badgeFields.put(f,f.get(null));f.set(null,name.equals("saveNeeded")?false:new HashSet<>());}
        for(Catalog cat:Catalog.values()){Map<Field,Map> state=new HashMap<>();for(String name:new String[]{"seen","useCount"}){Field f=Catalog.class.getDeclaredField(name);f.setAccessible(true);state.put(f,new LinkedHashMap<>((Map)f.get(cat)));}catalogFields.put(cat,state);}
        journalDirty=com.shatteredpixel.shatteredpixeldungeon.journal.Journal.class.getDeclaredField("saveNeeded");journalDirty.setAccessible(true);oldJournalDirty=journalDirty.getBoolean(null);
        GrassDewPickupTest.TestLevel l=new GrassDewPickupTest.TestLevel();l.setSize(33,33);Arrays.fill(l.map,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);l.blobs=new HashMap<>();l.buildFlagMaps();l.mobs=new HashSet<>();l.heaps=new SparseArray<>();l.traps=new SparseArray<>();l.plants=new SparseArray<>();Dungeon.level=l;
        hero=new Hero();hero.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(hero);hero.pos=16*33+16;hero.HP=10;hero.HT=100;hero.STR=10;hero.fieldOfView=l.heroFOV;Dungeon.hero=hero;
        Field f=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");f.setAccessible(true);Object u=f.get(null);hero.sprite=(Quiet)u.getClass().getMethod("allocateInstance",Class.class).invoke(u,Quiet.class);com.watabou.noosa.Game.instance=(com.watabou.noosa.Game)u.getClass().getMethod("allocateInstance",Class.class).invoke(u,com.watabou.noosa.Game.class);
    }
    @After public void restore()throws Exception{
        if(hero!=null)for(Buff b:new ArrayList<>(hero.buffs()))b.detach();
        Dungeon.hero=oldHero;Dungeon.level=oldLevel;Dungeon.challenges=oldChallenges;Dungeon.apdRestoreCustomRules(oldRules);Potion.apdHandler(oldHandler);Statistics.itemTypesDiscovered=oldDiscovered;
        for(Map.Entry<Field,Object> e:badgeFields.entrySet())e.getKey().set(null,e.getValue());
        for(Map.Entry<Catalog,Map<Field,Map>> entry:catalogFields.entrySet())for(Map.Entry<Field,Map> e:entry.getValue().entrySet()){Map map=(Map)e.getKey().get(entry.getKey());map.clear();map.putAll(e.getValue());}
        journalDirty.setBoolean(null,oldJournalDirty);notesField.set(null,oldNotes);com.watabou.noosa.Game.instance=oldGame;
    }
    private static Blandfruit fruit(Potion potion){Blandfruit f=new Blandfruit();f.imbuePotion(potion);return f;}
    private static String saved(com.watabou.utils.Bundlable object){Bundle b=new Bundle();object.storeInBundle(b);return b.toString();}
    @Test public void everyActualSeedRecipeHasSpecificPureFruitDetails()throws Exception{
        assertEquals(12,Potion.SeedToPotion.types.size());Set<Class<? extends Potion>> before=new HashSet<>(Potion.getKnown());String heroBefore=saved(hero);
        for(Class<? extends com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed> seedType:Potion.SeedToPotion.types.keySet()){
            Blandfruit f=(Blandfruit)new Blandfruit().cook(seedType.getDeclaredConstructor().newInstance());String initial=saved(f);
            assertEquals(Potion.SeedToPotion.types.get(seedType),f.potionAttrib.getClass());String effect=PotionExplanation.effect(f.potionAttrib,hero);assertFalse(effect.isEmpty());
            Random.pushGenerator(511);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(511);
            try{for(int i=0;i<3;i++){String detail=FoodExplanation.details(f);assertTrue(detail,detail.contains(effect));assertFalse(detail.contains("Where ordinary drinking is available"));assertFalse(BlandfruitExplanation.summary(f).isEmpty());assertTrue(MechanicsDetails.item(f,f.info()).contains(effect));}assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
            assertEquals(initial,saved(f));assertEquals(heroBefore,saved(hero));assertEquals(before,Potion.getKnown());assertTrue(Statistics.itemTypesDiscovered.isEmpty());
        }
    }
    @Test public void sunfruitShowsTheActualLiveHealingPoolAndSupportedCure(){
        for(int hp:new int[]{20,47,100,151}){
            hero.HT=hp;hero.HP=1;Buff.detach(hero,Healing.class);Buff.affect(hero,Slow.class,5);
            Blandfruit f=fruit(new PotionOfHealing());int offer=(int)(.8f*hp+14);assertTrue(BlandfruitExplanation.details(f,hero).contains(offer+" base HP"));
            f.potionAttrib.apply(hero);assertEquals(offer,hero.buff(Healing.class).remainingHealingPool());assertNull(hero.buff(Slow.class));assertEquals(1,hero.HP);
        }
    }
    @Test public void pharmacophobiaFruitWarnsAndUsesActualPoisonInsteadOfHealing(){
        Dungeon.challenges=Challenges.NO_HEALING;hero.lvl=9;hero.sprite=null;Blandfruit f=fruit(new PotionOfHealing());
        assertTrue(BlandfruitExplanation.summary(f).contains("Pharmacophobia"));assertTrue(BlandfruitExplanation.details(f,hero).contains("8 turns"));
        int hp=hero.HP;f.potionAttrib.apply(hero);assertEquals(hp,hero.HP);assertNull(hero.buff(Healing.class));assertEquals(8,hero.buff(Poison.class).apdRemaining(),0);
    }
    @Test public void rotfruitAddsOneBaseStrengthWithoutDirectHealingOrMaximumHpChange(){
        Blandfruit f=fruit(new PotionOfStrength());int hp=hero.HP,ht=hero.HT,str=hero.STR;
        assertTrue(BlandfruitExplanation.summary(f).contains("Strength by _1_"));assertFalse(PotionExplanation.details(f.potionAttrib,hero).contains("updates the hero's health limits"));
        f.potionAttrib.apply(hero);assertEquals(str+1,hero.STR);assertEquals(hp,hero.HP);assertEquals(ht,hero.HT);
    }
    @Test public void timedFruitDescriptionsMatchActualOffersAndDoNotShortenLongerEffects(){
        Potion[] potions={new PotionOfInvisibility(),new PotionOfHaste(),new PotionOfLevitation(),new PotionOfPurity()};Class[] buffs={Invisibility.class,Haste.class,Levitation.class,BlobImmunity.class};float[] durations={Invisibility.DURATION,Haste.DURATION,Levitation.DURATION,BlobImmunity.DURATION};
        for(int i=0;i<potions.length;i++){
            Blandfruit f=fruit(potions[i]);assertTrue(BlandfruitExplanation.summary(f).contains(ObfuscationExplanation.number(durations[i])+" turns"));potions[i].apply(hero);
            Buff b=hero.buff(buffs[i]);assertNotNull(b);assertEquals(durations[i],b.cooldown(),.001f);String before=saved(hero);BlandfruitExplanation.details(f,hero);assertEquals(before,saved(hero));
            Buff.prolong(hero,buffs[i],durations[i]+7);potions[i].apply(hero);assertEquals(durations[i]+7,b.cooldown(),.001f);b.detach();
        }
    }
    @Test public void fruitAndChunksNutritionMatchActualSatisfactionAndExistingMealTime()throws Exception{
        Method satisfy=Food.class.getDeclaredMethod("satisfy",Hero.class);satisfy.setAccessible(true);
        for(int challenge:new int[]{0,Challenges.NO_FOOD})for(Food meal:new Food[]{fruit(new PotionOfLevitation()),new Blandfruit.Chunks()}){
            Dungeon.challenges=challenge;Hunger hunger=Buff.affect(hero,Hunger.class);Bundle state=new Bundle();hunger.storeInBundle(state);state.put("level",450f);hunger.restoreFromBundle(state);
            float offer=challenge==0?450:150;assertEquals(offer,FoodExplanation.nutritionBeforeHorn(meal),0);satisfy.invoke(meal,hero);assertEquals(450-offer,hunger.hungerPoints(),0);
            if(meal instanceof Blandfruit)assertTrue(BlandfruitExplanation.details((Blandfruit)meal,hero).contains("Chunks refill "+ObfuscationExplanation.number(offer)+" hunger"));
        }
        Food meal=fruit(new PotionOfInvisibility());assertEquals(3,meal.previewEatingTime(),0);hero.talents.get(0).put(Talent.IRON_STOMACH,1);assertEquals(1,meal.previewEatingTime(),0);
    }
    @Test public void allCookedFormsRetainDetailsAcrossCompressedSaveWithoutLearningOtherPotions()throws Exception{
        Set<Class<? extends Potion>> known=new HashSet<>(Potion.getKnown());
        for(Class<? extends Potion> type:Potion.SeedToPotion.types.values()){
            Blandfruit original=fruit(type.getDeclaredConstructor().newInstance());original.quantity(3);String text=original.info()+FoodExplanation.details(original);Bundle state=new Bundle();original.storeInBundle(state);ByteArrayOutputStream bytes=new ByteArrayOutputStream();assertTrue(Bundle.write(state,bytes));
            Blandfruit loaded=new Blandfruit();loaded.restoreFromBundle(Bundle.read(new ByteArrayInputStream(bytes.toByteArray())));assertEquals(3,loaded.quantity());assertEquals(text,loaded.info()+FoodExplanation.details(loaded));assertEquals(type,loaded.potionAttrib.getClass());assertEquals(known,Potion.getKnown());
        }
    }
    @Test public void rawUnexpectedAndNoHeroContextsDoNotInventAnEffectOrCurrentValue(){
        assertTrue(BlandfruitExplanation.details(new Blandfruit(),hero).contains("cannot be eaten"));
        Blandfruit exotic=fruit(new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding());assertTrue(BlandfruitExplanation.details(exotic,hero).contains("unavailable"));assertFalse(BlandfruitExplanation.details(exotic,hero).contains("shield points"));
        Blandfruit healing=fruit(new PotionOfHealing());Dungeon.hero=null;try{String text=FoodExplanation.details(healing);assertFalse(text.contains("At current maximum HP"));assertFalse(text.contains("Current eating action:"));}finally{Dungeon.hero=hero;}
    }
    private static int shattered;
    public static class ProbeFire extends PotionOfLiquidFlame {@Override public void shatter(int cell){shattered++;}}
    public static class ProbeIce extends PotionOfFrost {@Override public void shatter(int cell){shattered++;}}
    public static class ProbeGas extends PotionOfToxicGas {@Override public void shatter(int cell){shattered++;}}
    public static class ProbeParalysis extends PotionOfParalyticGas {@Override public void shatter(int cell){shattered++;}}
    public static class ProbeFloat extends PotionOfLevitation {@Override public void shatter(int cell){shattered++;}}
    public static class ProbePurity extends PotionOfPurity {@Override public void shatter(int cell){shattered++;}}
    public static class DropSprite extends ItemSprite {@Override public void drop(){} @Override public void drop(int from){}}
    public static class DropLevel extends GrassDewPickupTest.TestLevel {
        Item dropped;ItemSprite dropSprite;
        @Override public Heap drop(Item item,int cell){dropped=item;Heap h=new Heap();h.items.add(item);h.pos=cell;h.sprite=dropSprite;return h;}
    }
    @Test public void actualSecondBerryCreatesOneDefaultSeedAndClearsItsSavedCounter()throws Exception{
        DropLevel l=new DropLevel();l.setSize(33,33);l.blobs=new HashMap<>();Arrays.fill(l.map,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);l.buildFlagMaps();l.mobs=new HashSet<>();l.heaps=new SparseArray<>();l.traps=new SparseArray<>();l.plants=new SparseArray<>();Dungeon.level=l;
        Field unsafeField=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Object unsafe=unsafeField.get(null);l.dropSprite=(DropSprite)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,DropSprite.class);
        Method satisfy=Berry.class.getDeclaredMethod("satisfy",Hero.class);satisfy.setAccessible(true);Berry berry=new Berry();
        Random.pushGenerator(7731);
        try{
            satisfy.invoke(berry,hero);assertNull(l.dropped);
            Berry.SeedCounter counter=hero.buff(Berry.SeedCounter.class);assertNotNull(counter);assertEquals(1,counter.count(),0);assertTrue(counter.revivePersists);
            Bundle stored=new Bundle();counter.storeInBundle(stored);Berry.SeedCounter copy=new Berry.SeedCounter();copy.restoreFromBundle(stored);assertEquals(1,copy.count(),0);assertTrue(copy.revivePersists);
            String heroBefore=saved(hero);String detail=FoodExplanation.specialEffect(berry,hero);assertTrue(detail.contains("Every second berry you eat"));assertEquals(heroBefore,saved(hero));assertEquals(1,counter.count(),0);assertNull(l.dropped);
            satisfy.invoke(berry,hero);assertTrue(l.dropped instanceof com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed);assertEquals(1,l.dropped.quantity());assertNull(hero.buff(Berry.SeedCounter.class));
        }finally{Random.popGenerator();}
    }
    @Test public void actualFruitThrowDispatchMatchesAllSixChunkFormsAndWellPitExceptions()throws Exception{
        DropLevel l=new DropLevel();l.setSize(33,33);l.blobs=new HashMap<>();Arrays.fill(l.map,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);l.buildFlagMaps();Dungeon.level=l;
        Field f=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");f.setAccessible(true);Object u=f.get(null);l.dropSprite=(DropSprite)u.getClass().getMethod("allocateInstance",Class.class).invoke(u,DropSprite.class);
        Method onThrow=Blandfruit.class.getDeclaredMethod("onThrow",int.class);onThrow.setAccessible(true);int cell=hero.pos+2;
        for(Class type:new Class[]{ProbeFire.class,ProbeIce.class,ProbeGas.class,ProbeParalysis.class,ProbeFloat.class,ProbePurity.class})for(int terrain:new int[]{com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WELL,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.CHASM}){
            Potion probe=(Potion)type.getDeclaredConstructor().newInstance();Blandfruit meal=fruit(probe);shattered=0;l.dropped=null;l.map[cell]=terrain;l.pit[cell]=terrain==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.CHASM;
            assertTrue(BlandfruitExplanation.releasesEffectWhenThrown(meal));onThrow.invoke(meal,cell);
            if(terrain==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY){assertEquals(1,shattered);assertTrue(l.dropped instanceof Blandfruit.Chunks);assertEquals(1,l.dropped.quantity());assertEquals(Hunger.STARVING,((Food)l.dropped).energy,0);}
            else{assertEquals(0,shattered);assertSame(meal,l.dropped);}
            Blandfruit ordinary=fruit((Potion)type.getSuperclass().getDeclaredConstructor().newInstance());assertTrue(BlandfruitExplanation.details(ordinary,hero).contains("1 portion of edible fruit chunks"));
        }
        l.map[cell]=com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY;l.pit[cell]=false;
        for(Potion potion:new Potion[]{new PotionOfHealing(),new PotionOfStrength(),new PotionOfExperience(),new PotionOfInvisibility(),new PotionOfHaste(),new PotionOfMindVision()}){
            Blandfruit meal=fruit(potion);assertFalse(BlandfruitExplanation.releasesEffectWhenThrown(meal));onThrow.invoke(meal,cell);assertSame(meal,l.dropped);assertTrue(BlandfruitExplanation.details(meal,hero).contains("whole fruit"));
        }
    }

    @Test public void seasonalDescriptionsTrackCurrentFormAndRealHealingAndShieldOffers()throws Exception{
        Field cached=com.shatteredpixel.shatteredpixeldungeon.utils.Holiday.class.getDeclaredField("cached");cached.setAccessible(true);Object previous=cached.get(null);
        Method satisfy=Pasty.class.getDeclaredMethod("satisfy",Hero.class);satisfy.setAccessible(true);
        try{
            for(int maximum:new int[]{20,79,100,151}){
                cached.set(null,com.shatteredpixel.shatteredpixeldungeon.utils.Holiday.HALLOWEEN);hero.HT=maximum;hero.HP=1;
                Pasty meal=new Pasty();String description=FoodExplanation.specialEffect(meal,hero);
                int offer=Math.max(3,maximum/20);assertTrue(description,description.contains(offer+" HP"));assertEquals(1,hero.HP);
                satisfy.invoke(meal,hero);assertEquals(Math.min(maximum,1+offer),hero.HP);
                cached.set(null,com.shatteredpixel.shatteredpixeldungeon.utils.Holiday.NEW_YEARS);
                Barrier barrier=Buff.affect(hero,Barrier.class);barrier.setShield(100);
                String shield=PastyExplanation.details(hero);assertTrue(shield,shield.contains("100 shield points"));assertEquals(100,barrier.shielding());
                satisfy.invoke(new Pasty(),hero);assertEquals(100,barrier.shielding());barrier.detach();
            }
            for(com.shatteredpixel.shatteredpixeldungeon.utils.Holiday holiday:com.shatteredpixel.shatteredpixeldungeon.utils.Holiday.values()){
                cached.set(null,holiday);Pasty meal=new Pasty();float energy=meal.energy;int hp=hero.HP;
                String detail=PastyExplanation.details(hero);assertFalse(detail.isEmpty());assertFalse(PastyExplanation.details(null).isEmpty());assertTrue(FoodExplanation.summary(meal).contains(PastyExplanation.summary(hero)));
                assertEquals(energy,meal.energy,0);assertEquals(hp,hero.HP);
                if(holiday==com.shatteredpixel.shatteredpixeldungeon.utils.Holiday.NONE)assertFalse(detail.contains("Charm"));
            }
        }finally{cached.set(null,previous);}
    }
    @Test public void seasonalExperienceAndFractionalWandChargeMatchRealFoodAction()throws Exception{
        Field cached=com.shatteredpixel.shatteredpixeldungeon.utils.Holiday.class.getDeclaredField("cached");cached.setAccessible(true);Object previous=cached.get(null);
        Method satisfy=Pasty.class.getDeclaredMethod("satisfy",Hero.class);satisfy.setAccessible(true);
        try{
            cached.set(null,com.shatteredpixel.shatteredpixeldungeon.utils.Holiday.PD_BIRTHDAY);hero.lvl=8;hero.exp=0;
            int experience=Math.max(2,hero.maxExp()/10);assertTrue(PastyExplanation.summary(hero).contains(experience+" XP"));
            satisfy.invoke(new Pasty(),hero);assertEquals(experience,hero.exp);
            cached.set(null,com.shatteredpixel.shatteredpixeldungeon.utils.Holiday.WINTER_HOLIDAYS);
            com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();wand.maxCharges=3;wand.curCharges=1;wand.partialCharge=0;wand.charge(hero);
            try{
                for(int expected:new int[]{1,2,2,3,3}){int charges=wand.curCharges;float partial=wand.partialCharge;PastyExplanation.details(hero);assertEquals(charges,wand.curCharges);assertEquals(partial,wand.partialCharge,0);satisfy.invoke(new Pasty(),hero);assertEquals(expected,wand.curCharges);}
                assertEquals(0,wand.partialCharge,0);
            }finally{wand.stopCharging();}
        }finally{cached.set(null,previous);}
    }
}
