package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;

public class ChaliceDamagePreviewTest {
    private Hero previousHero,hero;private Level previousLevel;private int previousDepth;
    static class Empty extends Level { protected boolean build(){return true;}protected void createMobs(){}protected void createItems(){} }
    static class QuietSprite extends HeroSprite {
        QuietSprite(Hero hero){ch=hero;hero.sprite=this;visible=false;}
        @Override public void place(int cell){}@Override public void idle(){}
        @Override public void showStatus(int color,String text,Object...args){}
        @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){}
        @Override public void showAlert(){}@Override public void hideAlert(){}
    }
    @Before public void setup(){
        HeadlessTestSupport.start();previousHero=Dungeon.hero;previousLevel=Dungeon.level;previousDepth=Dungeon.depth;
        hero=new Hero();hero.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(hero);hero.HT=hero.HP=1000;hero.pos=312;
        hero.damageInterrupt=false;Dungeon.hero=hero;Dungeon.depth=4;
        Level level=new Empty();level.setSize(25,25);Arrays.fill(level.map,Terrain.EMPTY);level.mobs=new HashSet<>();
        level.blobs=new HashMap<>();level.heaps=new com.watabou.utils.SparseArray<>();level.plants=new com.watabou.utils.SparseArray<>();level.traps=new com.watabou.utils.SparseArray<>();Dungeon.level=level;new QuietSprite(hero);
    }
    @After public void restore(){Dungeon.hero=previousHero;Dungeon.level=previousLevel;Dungeon.depth=previousDepth;}
    @Test public void everyUpgradeableLevelHasFullBaseRange(){
        int[] minima={3,6,13,26,43,66,93,126,163,206};
        int[] maxima={7,10,21,38,63,94,133,178,231,290};
        for(int level=0;level<10;level++){
            ChaliceDamagePreview p=ChaliceDamagePreview.calculate(hero,level);
            assertEquals(minima[level],p.baseMin);assertEquals(maxima[level],p.baseMax);
            assertEquals(p.baseMin,p.damageMin);assertEquals(p.baseMax,p.damageMax);
            assertEquals(p.damageMin,p.hpMin);assertEquals(p.damageMax,p.hpMax);
        }
    }
    @Test public void lethalUpperBoundaryIsInclusiveAndDoesNotUseTheAverage(){
        hero.HP=7;assertTrue(ChaliceDamagePreview.calculate(hero,0).couldBeFatal());
        hero.HP=8;assertFalse(ChaliceDamagePreview.calculate(hero,0).couldBeFatal());
        hero.HP=3;ChaliceDamagePreview p=ChaliceDamagePreview.calculate(hero,0);
        assertTrue(p.hpMin>=hero.HP);assertTrue(p.description().contains("Every outcome"));
    }
    @Test public void barrierSubtractsFromEveryOutcomeAndFatalityUsesRemainingHP(){
        Barrier barrier=Buff.affect(hero,Barrier.class);barrier.setShield(6);hero.HP=1;
        ChaliceDamagePreview p=ChaliceDamagePreview.calculate(hero,0);
        assertEquals(0,p.hpMin);assertEquals(1,p.hpMax);assertTrue(p.couldBeFatal());
        barrier.setShield(7);p=ChaliceDamagePreview.calculate(hero,0);
        assertEquals(0,p.hpMax);assertFalse(p.couldBeFatal());
    }
    @Test public void barkskinAndChangingBuffsKeepTheFullRandomRange(){
        Barkskin skin=Buff.affect(hero,Barkskin.class);skin.set(4,1);
        ChaliceDamagePreview p=ChaliceDamagePreview.calculate(hero,0);
        assertEquals(1,p.damageMin);assertEquals(7,p.damageMax);
        skin.detach();assertEquals(3,ChaliceDamagePreview.calculate(hero,0).damageMin);
    }
    @Test public void armorStrengthPenaltyMatchesRealDrRollSupport(){
        LeatherArmor armor=new LeatherArmor();hero.belongings.armor=armor;
        ChaliceDamagePreview p=ChaliceDamagePreview.calculate(hero,3);
        Random.pushGenerator(771);try{for(int i=0;i<1000;i++){
            int actual=Math.max(1,Random.NormalIntRange(p.baseMin,p.baseMax)-hero.drRoll());
            assertTrue(actual>=p.damageMin && actual<=p.damageMax);
        }}finally{Random.popGenerator();}
        hero.STR=1;p=ChaliceDamagePreview.calculate(hero,3);
        assertEquals(p.baseMin,p.damageMin);assertEquals(p.baseMax,p.damageMax);
    }
    @Test public void earthAndRockPreviewMatchesActualAbsorptionWithoutSpendingThem(){
        Earthroot.Armor earth=Buff.affect(hero,Earthroot.Armor.class);earth.level(40);
        WandOfLivingEarth.RockArmor rock=Buff.affect(hero,WandOfLivingEarth.RockArmor.class);
        Bundle rockState=new Bundle();rockState.put("armor",50);rockState.put("wand_level",3);rock.restoreFromBundle(rockState);
        for(int damage=1;damage<=300;damage++){
            Bundle e=new Bundle();earth.storeInBundle(e);Earthroot.Armor clone=new Earthroot.Armor();clone.attachTo(hero);clone.restoreFromBundle(e);
            assertEquals(clone.absorb(damage),earth.previewAbsorb(damage));clone.detach();
            Bundle r=new Bundle();rock.storeInBundle(r);WandOfLivingEarth.RockArmor copy=new WandOfLivingEarth.RockArmor();copy.attachTo(hero);copy.restoreFromBundle(r);
            assertEquals(copy.absorb(damage),rock.previewAbsorb(damage));copy.detach();
        }
        Bundle before=new Bundle();rock.storeInBundle(before);ChaliceDamagePreview.calculate(hero,5);
        Bundle after=new Bundle();rock.storeInBundle(after);assertEquals(before.getInt("armor"),after.getInt("armor"));
    }
    @Test public void simultaneousModifiersMatchActualIncomingDamage(){
        Endure.EndureTracker endure=Buff.affect(hero,Endure.EndureTracker.class);
        Buff.affect(hero,Doom.class);Buff.affect(hero,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge.ChallengeArena.class);
        Buff.affect(hero,MonkEnergy.MonkAbility.Meditate.MeditateResistance.class);
        ChaliceDamagePreview p=ChaliceDamagePreview.calculate(hero,9);int lo=Integer.MAX_VALUE,hi=0;
        for(int damage=p.baseMin;damage<=p.baseMax;damage++){
            hero.HP=hero.HT;hero.damage(damage,new ChaliceOfBlood());int actual=hero.HT-hero.HP;
            lo=Math.min(lo,actual);hi=Math.max(hi,actual);
        }
        assertEquals(lo,p.hpMin);assertEquals(hi,p.hpMax);assertTrue(endure.damageBonus>0);
    }
    @Test public void antimagicAndArcaneArmorReallyApplyToChalice(){
        LeatherArmor armor=new LeatherArmor();armor.inscribe(new AntiMagic());hero.belongings.armor=armor;
        Buff.affect(hero,ArcaneArmor.class).set(8,10);
        ChaliceDamagePreview p=ChaliceDamagePreview.calculate(hero,0);
        assertEquals(0,p.damageMin);assertEquals(7,p.damageMax);
        Random.pushGenerator(123);try{for(int i=0;i<1000;i++){
            hero.HP=hero.HT;hero.damage(Math.max(1,Random.NormalIntRange(p.baseMin,p.baseMax)-hero.drRoll()),new ChaliceOfBlood());
            int actual=hero.HT-hero.HP;assertTrue(actual>=p.hpMin && actual<=p.hpMax);
        }}finally{Random.popGenerator();}
    }
    @Test public void shieldActivationThresholdIsConsideredInsteadOfOnlyRangeEndpoints(){
        hero.HT=20;hero.HP=14;
        com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor armor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor();
        hero.belongings.armor=armor;armor.affixSeal(new BrokenSeal());
        BrokenSeal.WarriorShield seal=hero.buff(BrokenSeal.WarriorShield.class);
        assertEquals(5,seal.maxShield());ChaliceDamagePreview p=ChaliceDamagePreview.calculate(hero,0);
        // 3 damage does not trigger shield; 4 triggers it. Thus the largest HP loss is 3, not the base maximum 7.
        assertEquals(0,p.hpMin);assertEquals(3,p.hpMax);assertEquals(0,seal.shielding());assertFalse(seal.coolingDown());
    }
    @Test public void lifeLinkExplicitlyAvoidsFalseSurvivalGuarantee(){
        Buff.affect(hero,LifeLink.class);ChaliceDamagePreview p=ChaliceDamagePreview.calculate(hero,0);
        assertTrue(p.linked);assertTrue(p.couldBeFatal());assertTrue(p.description().contains("cannot be reliably predicted"));
        assertFalse(p.description().contains("HP loss after shields:"));
    }
    @Test public void readingPreviewDoesNotConsumeRngOrTemporaryBuffs(){
        Buff.affect(hero,Barkskin.class).set(20,1);Buff.affect(hero,Barrier.class).setShield(20);
        float now=Actor.now();int hp=hero.HP,shield=hero.shielding();int expected;
        Random.pushGenerator(87654);try{expected=Random.Int();}finally{Random.popGenerator();}
        Random.pushGenerator(87654);try{
            for(int i=0;i<10;i++)ChaliceDamagePreview.calculate(hero,i).description();assertEquals(expected,Random.Int());
        }finally{Random.popGenerator();}
        assertEquals(hp,hero.HP);assertEquals(shield,hero.shielding());assertEquals(20,hero.buff(Barkskin.class).level());assertEquals(now,Actor.now(),0f);
    }
    @Test public void saveReloadKeepsCurrentStatePreview(){
        Buff.affect(hero,Barkskin.class).set(3,1);Buff.affect(hero,Barrier.class).setShield(10);
        ChaliceDamagePreview before=ChaliceDamagePreview.calculate(hero,2);Bundle b=new Bundle();hero.storeInBundle(b);
        Hero restored=new Hero();restored.restoreFromBundle(b);Dungeon.hero=restored;
        ChaliceDamagePreview after=ChaliceDamagePreview.calculate(restored,2);
        assertEquals(before.description(),after.description());
    }
    @Test public void capeActivationByThisHitAndRandomDeflectionAreIncluded() throws Exception {
        com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns cape=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns();
        cape.level(10);cape.new Thorns().attachTo(hero);
        java.lang.reflect.Field charge=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("charge");charge.setAccessible(true);
        java.lang.reflect.Field cooldown=com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class.getDeclaredField("cooldown");cooldown.setAccessible(true);
        charge.setInt(cape,99);ChaliceDamagePreview p=ChaliceDamagePreview.calculate(hero,0);
        assertEquals(0,p.damageMin);assertEquals(7,p.damageMax);assertEquals(99,charge.getInt(cape));assertEquals(0,cooldown.getInt(cape));
        int minimum=Integer.MAX_VALUE,maximum=0;
        for(int seed=0;seed<1000;seed++){
            charge.setInt(cape,99);cooldown.setInt(cape,0);cape.level(10);
            Random.pushGenerator(seed);try{hero.HP=hero.HT;hero.damage(7,new ChaliceOfBlood());}finally{Random.popGenerator();}
            int actual=hero.HT-hero.HP;minimum=Math.min(minimum,actual);maximum=Math.max(maximum,actual);
            if(minimum==p.damageMin && maximum==p.damageMax)break;
        }
        assertEquals(p.damageMin,minimum);assertEquals(p.damageMax,maximum);
    }
    @Test public void holdFastAndWeaponDefenseDoNotRollOrSpendBuffs(){
        hero.belongings.weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff();
        hero.talents.get(1).put(Talent.HOLD_FAST,2);
        HoldFast hold=Buff.affect(hero,HoldFast.class);hold.pos=hero.pos;
        int expected;Random.pushGenerator(724);try{expected=Random.Int();}finally{Random.popGenerator();}
        ChaliceDamagePreview p;
        Random.pushGenerator(724);try{p=ChaliceDamagePreview.calculate(hero,2);assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
        assertTrue(p.damageMax<p.baseMax);assertSame(hold,hero.buff(HoldFast.class));
        hold.pos=hero.pos+1;p=ChaliceDamagePreview.calculate(hero,2);
        assertEquals(p.baseMax,p.damageMax);assertSame(hold,hero.buff(HoldFast.class));
    }
    @Test public void ironStomachCanMakeTheFinalDamageDeterministicallyZero(){
        hero.talents.get(1).put(Talent.IRON_STOMACH,2);Buff.affect(hero,Talent.WarriorFoodImmunity.class);
        ChaliceDamagePreview p=ChaliceDamagePreview.calculate(hero,9);
        assertEquals(0,p.damageMin);assertEquals(0,p.damageMax);assertFalse(p.couldBeFatal());
        assertTrue(p.description().contains("After reductions: 0\n"));
    }
    @Test public void stasisBlocksDamageButRemovingItRecalculatesImmediately(){
        TimeStasis stasis=Buff.affect(hero,TimeStasis.class);
        assertEquals(0,ChaliceDamagePreview.calculate(hero,9).hpMax);stasis.detach();
        assertEquals(290,ChaliceDamagePreview.calculate(hero,9).hpMax);
    }
    @Test public void magicImmunityIsRespectedAndDoesNotAdvertiseAnAvailablePrick(){
        ChaliceOfBlood chalice=new ChaliceOfBlood();hero.belongings.artifact=chalice;
        Buff.affect(hero,MagicImmune.class);hero.HP=hero.HT;
        assertEquals(0,ChaliceDamagePreview.calculate(hero,0).hpMax);
        assertFalse(chalice.actions(hero).contains(ChaliceOfBlood.AC_PRICK));
    }
    @Test public void fullyUpgradedAndUnequippedChaliceDoNotAdvertiseFalseNextAttempts(){
        ChaliceOfBlood chalice=new ChaliceOfBlood();
        assertTrue(chalice.desc().contains("Equip the Chalice"));hero.belongings.artifact=chalice;
        for(int i=0;i<10;i++)chalice.upgrade();
        assertFalse(chalice.actions(hero).contains(ChaliceOfBlood.AC_PRICK));
        assertFalse(chalice.desc().contains("Next prick"));
    }
}
