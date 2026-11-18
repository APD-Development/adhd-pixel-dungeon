package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CustomRuleCombinationAcceptanceTest {
    private CustomDungeonRules preferences;
    private int difficulty;
    private int challenges;
    private int slot;
    private HeroClass selected;
    private boolean intro;
    private boolean recording;
    private Hero previousHero;
    private Level previousLevel;
    private long previousSeed;
    private String previousCustomSeed;
    private boolean previousDaily;
    private boolean previousDailyReplay;

    private static class Board extends Level {
        @Override protected boolean build(){return true;}
        @Override protected void createMobs(){}
        @Override protected void createItems(){}
    }

    private static class Quiet extends CharSprite {
        @Override public void showStatus(int color,String text,Object...args){}
        @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){}
    }

    private static class FixedRat extends Rat {
        int incoming;
        Object source;
        FixedRat(boolean boss) {
            HP=HT=1000;
            alignment=Alignment.ENEMY;
            sprite=new Quiet();
            sprite.visible=false;
            if(boss)properties.add(Property.BOSS);
        }
        @Override public int attackSkill(Char target){return 10000;}
        @Override public int damageRoll(){return 20;}
        @Override public int defenseSkill(Char attacker){return 0;}
        @Override public int drRoll(){return 5;}
        @Override public int defenseProc(Char attacker,int damage){incoming=damage;return super.defenseProc(attacker,damage);}
        @Override public void damage(int amount,Object from){source=from;super.damage(amount,from);}
    }

    private static class FixedHero extends Hero {
        int incoming;
        Object source;
        FixedHero() {
            heroClass=HeroClass.WARRIOR;
            HP=HT=100;
            sprite=new Quiet();
            sprite.visible=false;
            Talent.initClassTalents(this);
            damageInterrupt=false;
        }
        @Override public void interrupt(){}
        @Override public int defenseSkill(Char attacker){return 0;}
        @Override public int drRoll(){return 5;}
        @Override public void damage(int amount,Object from){incoming=amount;source=from;super.damage(amount,from);}
    }

    @Before public void setup() {
        HeadlessTestSupport.start();
        preferences=SPDSettings.apdCustomRules();
        difficulty=SPDSettings.apdDifficulty();
        challenges=SPDSettings.challenges();
        slot=GamesInProgress.curSlot;
        selected=GamesInProgress.selectedClass;
        intro=SPDSettings.intro();
        recording=SPDSettings.apdRecording();
        previousHero=Dungeon.hero;
        previousLevel=Dungeon.level;
        previousSeed=Dungeon.seed;
        previousCustomSeed=Dungeon.customSeedText;
        previousDaily=Dungeon.daily;
        previousDailyReplay=Dungeon.dailyReplay;
        GamesInProgress.curSlot=1;
        SPDSettings.intro(false);
        SPDSettings.apdRecording(false);
        SPDSettings.apdDifficulty(3);
        SPDSettings.challenges(0);
    }

    @After public void cleanup() {
        SPDSettings.apdCustomRules(preferences);
        SPDSettings.apdDifficulty(difficulty);
        SPDSettings.challenges(challenges);
        SPDSettings.intro(intro);
        SPDSettings.apdRecording(recording);
        GamesInProgress.curSlot=slot;
        GamesInProgress.selectedClass=selected;
        Dungeon.hero=previousHero;
        Dungeon.level=previousLevel;
        Dungeon.seed=previousSeed;
        Dungeon.customSeedText=previousCustomSeed;
        Dungeon.daily=previousDaily;
        Dungeon.dailyReplay=previousDailyReplay;
        Dungeon.apdRestoreCustomRules(new Bundle());
    }

    private static CustomDungeonRules extremeRules() {
        return CustomDungeonRules.defaults()
                .with(CustomDungeonRules.Slider.ENEMY_HP,500)
                .with(CustomDungeonRules.Slider.ENEMY_DAMAGE,500)
                .with(CustomDungeonRules.Slider.ENEMY_DEFENSE,500)
                .with(CustomDungeonRules.Slider.ENEMY_ACCURACY,25)
                .with(CustomDungeonRules.Slider.ENEMY_EVASION,0)
                .with(CustomDungeonRules.Slider.ENEMY_SPAWN,300)
                .with(CustomDungeonRules.Slider.BOSS_HP,500)
                .with(CustomDungeonRules.Slider.BOSS_DAMAGE,25)
                .with(CustomDungeonRules.Slider.PLAYER_HP,500)
                .with(CustomDungeonRules.Slider.PLAYER_DAMAGE,25)
                .with(CustomDungeonRules.Slider.PLAYER_ACCURACY,25)
                .with(CustomDungeonRules.Slider.PLAYER_EVASION,0)
                .with(CustomDungeonRules.Slider.PLAYER_DEFENSE,500)
                .with(CustomDungeonRules.Slider.FOOD,25)
                .with(CustomDungeonRules.Slider.HUNGER,400)
                .with(CustomDungeonRules.Slider.HEALING,25)
                .with(CustomDungeonRules.Slider.EQUIPMENT,25)
                .with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,1);
    }

    private static void activeRules(CustomDungeonRules rules) {
        Bundle wrapper=new Bundle();
        Bundle stored=new Bundle();
        rules.store(stored);
        wrapper.put("apd_custom_rules",stored);
        Dungeon.apdRestoreCustomRules(wrapper);
    }

    private static Board board(Hero hero) {
        Board board=new Board();
        board.setSize(7,7);
        Dungeon.level=board;
        Dungeon.hero=hero;
        hero.pos=23;
        hero.fieldOfView=new boolean[49];
        hero.sprite=new Quiet();
        hero.sprite.visible=false;
        return board;
    }

    private static FixedRat attackTarget(Hero hero,boolean scaled,long seed) {
        board(hero);
        FixedRat target=new FixedRat(false);
        target.pos=24;
        if(scaled)CustomMobHooks.applyHealth(target);
        Random.pushGenerator(seed);
        try {assertTrue(hero.attack(target));}
        finally {Random.popGenerator();}
        return target;
    }

    @Test public void highEnemyHealthDefenseAndLowDamageKeepOriginalAttackOrderForAllSixClasses() {
        CustomDungeonRules extreme=extremeRules();
        for(HeroClass type:HeroClass.values()) {
            SPDSettings.apdCustomRules(CustomDungeonRules.defaults());
            GamesInProgress.selectedClass=type;
            Dungeon.seed=47000+type.ordinal();
            Dungeon.init();
            FixedRat neutral=attackTarget(Dungeon.hero,false,61829);
            assertTrue(type+" must produce a real pre-armor roll",neutral.incoming>0);

            SPDSettings.apdCustomRules(extreme);
            GamesInProgress.selectedClass=type;
            Dungeon.seed=47000+type.ordinal();
            Dungeon.init();
            FixedRat scaled=attackTarget(Dungeon.hero,true,61829);
            assertEquals(100,Dungeon.hero.HT);
            assertEquals(5000,scaled.HT);
            assertEquals(Math.round(neutral.incoming*0.25f),scaled.incoming);
            assertEquals(5000-Math.max(0,scaled.incoming-25),scaled.HP);
            assertSame(Dungeon.hero,scaled.source);
            assertEquals(1,Dungeon.customRules().get(CustomDungeonRules.Slider.UPGRADE_SCROLLS));
            assertEquals(100.0/15,new UpgradeScrollPlan(1).percentage(),0);
        }
    }

    @Test public void enemyAndBossReplacementDamageCombineWithPlayerDefenseAndHealingExactlyOnce() {
        activeRules(extremeRules());
        FixedHero hero=new FixedHero();
        board(hero);

        FixedRat enemy=new FixedRat(false);
        enemy.pos=24;
        Random.pushGenerator(71327);
        try {assertTrue(enemy.attack(hero));}
        finally {Random.popGenerator();}
        assertEquals(75,hero.incoming);
        assertEquals(25,hero.HP);
        assertSame(enemy,hero.source);

        Healing recovery=Buff.affect(hero,Healing.class);
        recovery.setHeal(4,0,1);
        for(int tick=0;tick<4;tick++)recovery.act();
        assertEquals(26,hero.HP);

        hero.HP=100;
        hero.apdCustomHealingFraction=0;
        FixedRat boss=new FixedRat(true);
        boss.pos=24;
        Random.pushGenerator(71327);
        try {assertTrue(boss.attack(hero));}
        finally {Random.popGenerator();}
        assertEquals(0,hero.incoming);
        assertEquals(100,hero.HP);
        assertSame(boss,hero.source);
    }

    @Test public void allEighteenExtremeValuesAndDamagedHighHpSurviveRealSaveLoadForEveryClass() throws Exception {
        CustomDungeonRules extreme=extremeRules();
        SPDSettings.apdDifficulty(5);
        SPDSettings.challenges(511);
        for(HeroClass type:HeroClass.values()) {
            SPDSettings.apdCustomRules(extreme);
            GamesInProgress.selectedClass=type;
            long expectedSeed=91337+type.ordinal();
            String expectedText="TEST167-"+type.name();
            Dungeon.seed=expectedSeed;
            Dungeon.customSeedText=expectedText;
            Dungeon.daily=false;
            Dungeon.dailyReplay=false;
            Dungeon.init();
            assertEquals(100,Dungeon.hero.HT);
            Dungeon.hero.HP=93;
            Dungeon.hero.apdCustomHealingFraction=.75f;
            Dungeon.depth=1;
            Dungeon.saveGame(1);

            SPDSettings.apdCustomRules(CustomDungeonRules.defaults());
            Dungeon.seed=0;
            Dungeon.customSeedText="";
            Dungeon.apdRestoreCustomRules(new Bundle());
            assertNull(Dungeon.customRules());
            Dungeon.loadGame(1,false);
            Dungeon.hero.updateHT(false);

            assertArrayEquals(extreme.values(),Dungeon.customRules().values());
            assertEquals(type,Dungeon.hero.heroClass);
            assertEquals(100,Dungeon.hero.HT);
            assertEquals(93,Dungeon.hero.HP);
            assertEquals(.75f,Dungeon.hero.apdCustomHealingFraction,0);
            assertEquals(5,Dungeon.difficulty);
            assertEquals(Difficulty.balanceRevision(5),Dungeon.difficultyBalance);
            assertEquals(511,Dungeon.challenges);
            assertEquals(expectedSeed,Dungeon.seed);
            assertEquals(expectedText,Dungeon.customSeedText);
            assertNotNull(Dungeon.customScoring());
        }
    }
}
