package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Bundle;
import org.junit.*;
import static org.junit.Assert.*;
public class CustomMobSurvivalInteractionTest {
    private CustomDungeonRules prefs;private int difficulty,challenge;private boolean intro,recording;
    private HeroClass selected;
    private static class AddedRat extends Rat { void add(){super.onAdd();} }
    @Before public void start() {
        HeadlessTestSupport.start();prefs=SPDSettings.apdCustomRules();difficulty=SPDSettings.apdDifficulty();challenge=SPDSettings.challenges();
        intro=SPDSettings.intro();recording=SPDSettings.apdRecording();selected=GamesInProgress.selectedClass;
        SPDSettings.intro(false);SPDSettings.apdRecording(false);SPDSettings.challenges(0);SPDSettings.apdDifficulty(3);
        GamesInProgress.selectedClass=HeroClass.WARRIOR;
    }
    @After public void restore() {
        SPDSettings.apdCustomRules(prefs);SPDSettings.apdDifficulty(difficulty);SPDSettings.challenges(challenge);
        SPDSettings.intro(intro);SPDSettings.apdRecording(recording);GamesInProgress.selectedClass=selected;
        Dungeon.apdRestoreCustomRules(new Bundle());
    }
    private void rules(CustomDungeonRules r) {SPDSettings.apdCustomRules(r);Dungeon.init();}
    @Test public void everyEnemyHealthValueAppliesOnceAndSurvivesMobSaveLoad() {
        for(int p=25;p<=500;p+=25) {
            rules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_HP,p));
            AddedRat rat=new AddedRat();int base=rat.HT;rat.HP=base/2;
            CustomMobHooks.applyHealth(rat);
            assertEquals(Math.max(1,Math.round(base*p/100f)),rat.HT);
            assertEquals(Math.round(base/2*p/100f),rat.HP);
            int ht=rat.HT,hp=rat.HP;CustomMobHooks.applyHealth(rat);rat.add();
            assertEquals(ht,rat.HT);assertEquals(hp,rat.HP);
            Bundle b=new Bundle();rat.storeInBundle(b);AddedRat loaded=new AddedRat();loaded.restoreFromBundle(b);loaded.add();
            assertEquals(ht,loaded.HT);assertEquals(hp,loaded.HP);
        }
    }
    @Test public void bossesAlliesAndDeadEnemiesAreNotAccidentallyRescaledOrRevived() {
        rules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_HP,500));
        Goo boss=new Goo();int ht=boss.HT;CustomMobHooks.applyHealth(boss);assertEquals(ht,boss.HT);
        Rat ally=new Rat();ally.alignment=Char.Alignment.ALLY;ht=ally.HT;CustomMobHooks.applyHealth(ally);assertEquals(ht,ally.HT);
        Rat dead=new Rat();dead.HP=0;CustomMobHooks.applyHealth(dead);assertEquals(0,dead.HP);
    }
    @Test public void hungerTicksAndExplicitCostsScaleButFoodRestorationDoesNot() {
        for(int p=25;p<=400;p+=25) {
            rules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.HUNGER,p));
            Dungeon.depth=1;Dungeon.seed=88347;Dungeon.level=Dungeon.newLevel();
            Hunger hunger=Dungeon.hero.buff(Hunger.class);assertNotNull(hunger);
            hunger.affectHunger(-10);assertEquals((int)Math.ceil(10*p/100f),hunger.hunger());
            hunger.satisfy(2);int before=hunger.hunger();
            for(int tick=0;tick<4;tick++)hunger.act();
            assertEquals(before+p/25,hunger.hunger());
        }
    }
    @Test public void highSpawnsHighEnemyHealthAndFastHungerGenerateForAllClasses() {
        CustomDungeonRules rules=CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_SPAWN,300)
            .with(CustomDungeonRules.Slider.ENEMY_HP,500).with(CustomDungeonRules.Slider.HUNGER,400)
            .with(CustomDungeonRules.Slider.FOOD,25)
            .with(CustomDungeonRules.Slider.PLAYER_HP,25).with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,1);
        for(HeroClass c:HeroClass.values()) {
            GamesInProgress.selectedClass=c;rules(rules);Dungeon.seed=98342+c.ordinal();
            for(int depth=1;depth<=4;depth++) {
                Dungeon.depth=depth;Dungeon.branch=0;Dungeon.level=Dungeon.newLevel();
                assertNotNull(Dungeon.level);assertTrue(Dungeon.level.mobs.size()>=1);
                assertEquals(5,Dungeon.hero.HT);
                for(Mob mob:Dungeon.level.mobs)if(mob.alignment==Char.Alignment.ENEMY&&!Char.hasProp(mob,Char.Property.BOSS))
                    assertTrue(mob.apdCustomHealthApplied);
            }
        }
    }
    @Test public void oversaturatedCustomFloorStopsWithoutChangingNeutralPlacement() throws Exception {
        rules(CustomDungeonRules.defaults());Dungeon.seed=98342;Dungeon.depth=1;Dungeon.level=Dungeon.newLevel();
        Level level=Dungeon.level;
        java.util.List<Integer> cells=new java.util.ArrayList<>();
        for(Mob mob:level.mobs)if(mob.alignment==Char.Alignment.ENEMY)cells.add(mob.pos);
        assertEquals(8,cells.size());
        java.util.Arrays.fill(level.passable,false);for(int cell:cells)level.passable[cell]=true;
        level.mobs.clear();
        java.lang.reflect.Method create=com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel.class.getDeclaredMethod("createMobs");
        create.setAccessible(true);create.invoke(level);assertEquals(8,level.mobs.size());
        level.mobs.clear();Bundle wrapper=new Bundle(),settings=new Bundle();
        CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_SPAWN,300).store(settings);
        wrapper.put("apd_custom_rules",settings);Dungeon.apdRestoreCustomRules(wrapper);
        create.invoke(level);
        assertEquals("Only eight legal spawn cells exist; custom generation must terminate",8,level.mobs.size());
    }
    @Test public void spawnCountsAndCooldownsHaveSafeBoundariesAndNeutralIdentity() {
        for(int p=25;p<=300;p+=25) {
            rules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_SPAWN,p));
            assertEquals(Math.round(8*p/100f),CustomResourceHooks.spawnCount(8));
            assertEquals(0,CustomResourceHooks.spawnCount(0));
            assertEquals(50*100f/p,CustomResourceHooks.respawnCooldown(50),0.00001f);
        }
    }
}
