package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import org.junit.*;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
public class CustomBossHealthSliderTest {
    private Hero beforeHero;private Level beforeLevel;private int beforeChallenges;
    private static class Empty extends Level {protected boolean build(){return true;}protected void createMobs(){}protected void createItems(){}}
    private static class QuietSprite extends CharSprite {
        @Override public void showStatus(int color,String text,Object...args){}
        @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){}
        @Override public Emitter centerEmitter(){return new Emitter();}
    }
    private static class GuardedKing extends DwarfKing {
        boolean block;
        @Override public boolean isInvulnerable(Class effect){return block || super.isInvulnerable(effect);}
    }
    @Before public void setup(){
        HeadlessTestSupport.start();beforeHero=Dungeon.hero;beforeLevel=Dungeon.level;beforeChallenges=Dungeon.challenges;
        Dungeon.hero=new Hero();Dungeon.level=new Empty();Dungeon.level.setSize(25,25);
        Arrays.fill(Dungeon.level.map,Terrain.EMPTY);Dungeon.level.mobs=new HashSet<>();
    }
    @After public void clear(){Dungeon.hero=beforeHero;Dungeon.level=beforeLevel;Dungeon.challenges=beforeChallenges;Dungeon.apdRestoreCustomRules(new Bundle());}
    private void rules(int percent){Bundle wrapper=new Bundle(),r=new Bundle();
        CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.BOSS_HP,percent)
            .with(CustomDungeonRules.Slider.ENEMY_HP,25).store(r);wrapper.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(wrapper);}
    private void phase(Mob boss,int phase)throws Exception{Field f=(boss instanceof DwarfKing?DwarfKing.class:boss.getClass()).getDeclaredField("phase");f.setAccessible(true);f.setInt(boss,phase);}
    @Test public void everyBossHealthValueReplacesEnemyHealthAndPersistsExactlyOnce(){
        for(int challenges:new int[]{0,Challenges.STRONGER_BOSSES})for(int p=25;p<=500;p+=25) {
            Dungeon.challenges=challenges;rules(p);
            for(Mob boss:new Mob[]{new Goo(),new Tengu(),new DM300(),new DwarfKing(),new YogDzewa(),new YogFist.BurningFist()}) {
                int original=boss.HT;CustomMobHooks.applyHealth(boss);
                assertEquals(Math.max(1,Math.round(original*p/100f)),boss.HT);assertEquals(boss.HT,boss.HP);
                Bundle saved=new Bundle();boss.storeInBundle(saved);int ht=boss.HT;
                boss.restoreFromBundle(saved);CustomMobHooks.applyHealth(boss);assertEquals(ht,boss.HT);assertEquals(ht,boss.HP);
                if(boss instanceof DwarfKing) {
                    saved.put("apd_shield_objectives_resolved",19);
                    assertThrows(IllegalArgumentException.class,()->boss.restoreFromBundle(saved));
                    assertEquals(19,saved.getInt("apd_shield_objectives_resolved"));
                }
            }
        }
    }
    @Test public void yogFinalPhaseBoundaryScalesWithoutLosingTheFinalFight()throws Exception {
        Dungeon.challenges=0;
        for(int p=25;p<=500;p+=25) {
            rules(p);YogDzewa yog=new YogDzewa();CustomMobHooks.applyHealth(yog);phase(yog,4);yog.sprite=new QuietSprite();
            yog.damage(100000,new Object());assertEquals(Math.round(100*p/100f),yog.HP);assertTrue(yog.isAlive());
        }
    }
    @Test public void dwarfShieldUsesAllOriginalObjectivesAndKeepsRemaindersAcrossReload()throws Exception {
        for(int challenges:new int[]{0,Challenges.STRONGER_BOSSES})for(int p=25;p<=500;p+=25) {
            Dungeon.challenges=challenges;rules(p);GuardedKing guarded=new GuardedKing();DwarfKing king=guarded;CustomMobHooks.applyHealth(king);phase(king,2);
            king.sprite=new QuietSprite();Buff.affect(king,DwarfKing.DKBarrior.class).setShield(king.HT);
            king.damage(-1,new DwarfKing.KingDamager());assertEquals(king.HT,king.shielding());
            guarded.block=true;king.damage(king.HT,new DwarfKing.KingDamager());assertEquals(king.HT,king.shielding());guarded.block=false;
            int objectives=challenges==0?12:18;
            for(int n=1;n<=objectives;n++) {
                king.damage(king.HT/objectives,new DwarfKing.KingDamager());
                assertEquals(king.HT-(int)((long)king.HT*n/objectives),king.shielding());
                if(n==objectives/2){Bundle saved=new Bundle();king.storeInBundle(saved);king=new DwarfKing();king.restoreFromBundle(saved);king.sprite=new QuietSprite();}
            }
            Field phase=DwarfKing.class.getDeclaredField("phase");phase.setAccessible(true);assertEquals(3,phase.getInt(king));
        }
    }
}
