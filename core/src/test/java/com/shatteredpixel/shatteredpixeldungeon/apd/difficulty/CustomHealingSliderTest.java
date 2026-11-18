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
package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import static org.junit.Assert.*;
public class CustomHealingSliderTest {
    private Hero previous;private Hero hero;private Quiet sprite;private com.shatteredpixel.shatteredpixeldungeon.levels.Level previousLevel;
    private static class Empty extends com.shatteredpixel.shatteredpixeldungeon.levels.Level {
        protected boolean build(){return true;}protected void createMobs(){}protected void createItems(){}
    }
    private static class Quiet extends HeroSprite {
        String lastStatus;
        Quiet(Hero h){ch=h;h.sprite=this;visible=false;}
        @Override public void place(int cell){}
        @Override public void idle(){}
        @Override public void add(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.State state){}
        @Override public void remove(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.State state){}
        @Override public void showStatus(int color,String text,Object...args){}
        @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){lastStatus=text;}
    }
    @Before public void setup(){HeadlessTestSupport.start();previous=Dungeon.hero;previousLevel=Dungeon.level;
        Dungeon.level=new Empty();Dungeon.level.setSize(25,25);Dungeon.level.mobs=new java.util.HashSet<>();
        hero=new Hero();hero.heroClass=HeroClass.WARRIOR;
        Talent.initClassTalents(hero);hero.HT=1000;hero.HP=100;Dungeon.hero=hero;sprite=new Quiet(hero);}
    @After public void clear(){Dungeon.hero=previous;Dungeon.level=previousLevel;Dungeon.apdRestoreCustomRules(new Bundle());}
    private void rules(int p){Bundle b=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.HEALING,p).store(r);
        b.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(b);}
    @Test public void allPercentagesAccumulateSmallHealsWithoutRngOrOverhealBanking(){
        for(int p=0;p<=200;p+=25) {
            rules(p);hero.HP=100;hero.apdCustomHealingFraction=0;
            Random.pushGenerator(9173);long next;try{next=Random.Long();}finally{Random.popGenerator();}
            Random.pushGenerator(9173);
            try{for(int i=0;i<40;i++)hero.HP+=CustomHealingHooks.amount(hero,1);assertEquals(next,Random.Long());}
            finally{Random.popGenerator();}
            assertEquals(100+40*p/100,hero.HP);
            hero.HP=hero.HT-1;hero.apdCustomHealingFraction=0;
            hero.HP=Math.min(hero.HT,hero.HP+CustomHealingHooks.amount(hero,100));
            if(p!=0)assertEquals(hero.HT,hero.HP);assertEquals(0,hero.apdCustomHealingFraction,0);
        }
        Rat ally=new Rat();rules(0);assertEquals(19,CustomHealingHooks.amount(ally,19));
    }
    @Test public void potionAndWellFedUseActualTickPathsAndLeaveShieldingSeparate(){
        rules(25);Barrier barrier=Buff.affect(hero,Barrier.class);barrier.setShield(17);
        Healing healing=Buff.affect(hero,Healing.class);healing.setHeal(4,0,1);
        for(int i=0;i<4;i++)healing.act();assertEquals(101,hero.HP);assertEquals(17,hero.shielding());
        WellFed food=Buff.affect(hero,WellFed.class);food.reset();
        for(int i=0;i<72;i++)food.act();assertEquals(102,hero.HP);assertEquals(17,hero.shielding());
        rules(0);int before=hero.HP;healing=Buff.affect(hero,Healing.class);healing.setHeal(4,0,1);
        for(int i=0;i<4;i++)healing.act();assertEquals(before,hero.HP);
    }
    @Test public void dewAndFullRecoveryRespectEffectivenessBeforeFinalCaps(){
        rules(50);hero.HT=100;hero.HP=99;Dewdrop.consumeDew(1,hero,true);assertEquals(100,hero.HP);
        hero.HP=1;hero.apdCustomHealingFraction=0;Dewdrop.consumeDew(20,hero,true);assertEquals(51,hero.HP);
        rules(25);hero.HP=1;hero.apdCustomHealingFraction=0;assertEquals(26,CustomHealingHooks.fullHealth(hero));
        rules(0);assertEquals(1,CustomHealingHooks.fullHealth(hero));
    }
    @Test public void fractionalRecoverySurvivesRealHeroBundleLoadAndInvalidDataIsNotRewritten(){
        rules(25);for(int i=0;i<3;i++)assertEquals(0,CustomHealingHooks.amount(hero,1));
        Bundle saved=new Bundle();hero.storeInBundle(saved);Hero restored=new Hero();restored.restoreFromBundle(saved);
        assertEquals(.75f,restored.apdCustomHealingFraction,0);assertEquals(1,CustomHealingHooks.amount(restored,1));
        saved.put("apd_custom_healing_fraction",1.25f);
        assertThrows(IllegalArgumentException.class,()->new Hero().restoreFromBundle(saved));
        assertEquals(1.25f,saved.getFloat("apd_custom_healing_fraction"),0);
    }
    @Test public void zeroHealingCannotLockRestorativeSleepAndDoesNotScaleMaximumHpGains(){
        rules(0);int hp=hero.HP;MagicalSleep sleep=Buff.affect(hero,MagicalSleep.class);
        assertEquals(0,hero.paralysed);assertNull(hero.buff(MagicalSleep.class));assertEquals(hp,hero.HP);
        rules(100);sleep=Buff.affect(hero,MagicalSleep.class);assertTrue(hero.paralysed>0);
        rules(0);sleep.act();assertEquals(0,hero.paralysed);assertNull(hero.buff(MagicalSleep.class));assertEquals(hp,hero.HP);
        hero.HT=20;hero.HP=10;hero.lvl=2;hero.updateHT(true);assertEquals(25,hero.HT);assertEquals(15,hero.HP);
    }
    @Test public void resurrectionRestoresNormalHealthEvenWhenHealingIsDisabled(){
        rules(0);hero.lvl=1;hero.updateHT(false);hero.HP=1;hero.apdCustomHealingFraction=.75f;
        hero.resurrect();assertEquals(hero.HT,hero.HP);assertEquals(0,hero.apdCustomHealingFraction,0);
        assertNotNull("Normal resurrection's backpack recovery behavior remains",hero.buff(LostInventory.class));
    }
    @Test public void blessedAnkhRevivalUsesTheLiveQuarterHpFormulaOutsideHealingScaling(){
        Ankh ankh=new Ankh();ankh.bless();
        for(int percent:new int[]{0,25,100,200}){
            rules(percent);
            for(int maximum:new int[]{20,21,99,100,101,1000}){
                hero.HT=maximum;hero.HP=0;
                int expected=maximum/4;
                assertEquals(expected,Ankh.blessedReviveHP(hero.HT));
                String detail=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.UtilityItemExplanation.details(ankh,hero);
                assertTrue(detail,detail.contains("blessed revival value is "+expected+" HP"));
                assertTrue(detail,detail.contains("Healing percentage does not multiply it"));
            }
        }
        assertTrue(ankh.isBlessed());assertEquals(0,hero.HP);
    }
    @Test public void supplyRationShowsScaledAndCappedHealingWhileNeutralKeepsOriginalText() throws Exception {
        SupplyRation ration=new SupplyRation();
        java.lang.reflect.Method satisfy=SupplyRation.class.getDeclaredMethod("satisfy",Hero.class);
        satisfy.setAccessible(true);
        for(int percent:new int[]{0,25,100,200}){
            rules(percent);hero.HT=1000;hero.HP=100;hero.apdCustomHealingFraction=0;sprite.lastStatus=null;
            satisfy.invoke(ration,hero);
            int gained=5*percent/100;
            assertEquals(100+gained,hero.HP);
            assertEquals(Integer.toString(percent==100?5:gained),sprite.lastStatus);
        }
        rules(200);hero.HT=1000;hero.HP=999;hero.apdCustomHealingFraction=0;sprite.lastStatus=null;
        satisfy.invoke(ration,hero);assertEquals(1000,hero.HP);assertEquals("1",sprite.lastStatus);
        rules(100);hero.HP=999;sprite.lastStatus=null;
        satisfy.invoke(ration,hero);assertEquals(1000,hero.HP);assertEquals("5",sprite.lastStatus);
    }
    @Test public void frozenMeatHealingFeedbackMatchesActualRollAndModifiedHp() {
        long healingSeed=-1;
        for(long seed=0;seed<1000;seed++){Random.pushGenerator(seed);int roll;try{roll=Random.Int(5);}finally{Random.popGenerator();}if(roll==3){healingSeed=seed;break;}}
        assertTrue(healingSeed>=0);
        for(int percent:new int[]{0,25,100,200})for(int missing:new int[]{1,900}){
            rules(percent);hero.HT=1000;hero.HP=hero.HT-missing;hero.apdCustomHealingFraction=0;sprite.lastStatus=null;int before=hero.HP;
            Random.pushGenerator(healingSeed);try{com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio.effect(hero);}finally{Random.popGenerator();}
            int gained=Math.min(missing,250*percent/100);assertEquals(before+gained,hero.HP);
            assertEquals(Integer.toString(percent==100?250:gained),sprite.lastStatus);
        }
    }
    @Test public void phantomMeatFeedbackPreservesAllBenefitsAndActualHealing() {
        for(int percent:new int[]{0,25,100,200})for(int missing:new int[]{1,900}){
            rules(percent);hero.HT=1000;hero.HP=hero.HT-missing;hero.apdCustomHealingFraction=0;sprite.lastStatus=null;int before=hero.HP;
            Buff.affect(hero,Slow.class,8);
            com.shatteredpixel.shatteredpixeldungeon.items.food.PhantomMeat.effect(hero);
            int gained=Math.min(missing,250*percent/100);assertEquals(before+gained,hero.HP);
            assertEquals(Integer.toString(percent==100?250:gained),sprite.lastStatus);
            assertEquals(250,Barkskin.currentLevel(hero));assertNotNull(hero.buff(Invisibility.class));assertNull(hero.buff(Slow.class));
        }
    }
    @Test public void actualHealingTicksWorkForEveryClassAtMinimumNeutralAndMaximum() {
        for (HeroClass type : HeroClass.values()) {
            hero.heroClass=type;Talent.initClassTalents(hero);
            for(int percent : new int[]{0,25,100,200}) {
                rules(percent);hero.HP=100;hero.HT=1000;hero.apdCustomHealingFraction=0;
                Healing effect=Buff.affect(hero,Healing.class);effect.setHeal(8,0,1);
                for(int tick=0;tick<8;tick++)effect.act();
                assertEquals(type+" at "+percent+"%",100+8*percent/100,hero.HP);
                Bundle saved=new Bundle();hero.storeInBundle(saved);
                Hero restored=new Hero();restored.restoreFromBundle(saved);
                assertEquals(type,restored.heroClass);assertEquals(hero.HP,restored.HP);
                assertEquals(hero.apdCustomHealingFraction,restored.apdCustomHealingFraction,0);
            }
        }
    }
}
