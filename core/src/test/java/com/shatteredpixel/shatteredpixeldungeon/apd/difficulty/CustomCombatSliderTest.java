package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import static org.junit.Assert.*;
public class CustomCombatSliderTest {
    private CustomDungeonRules preferences;
    private HeroClass selected;
    private int slot, challenges, difficulty;
    @Before public void start() {
        HeadlessTestSupport.start();
        preferences=SPDSettings.apdCustomRules(); selected=GamesInProgress.selectedClass; slot=GamesInProgress.curSlot;
        challenges=SPDSettings.challenges(); difficulty=SPDSettings.apdDifficulty();
        GamesInProgress.selectedClass=HeroClass.WARRIOR; GamesInProgress.curSlot=1;
        SPDSettings.challenges(0);SPDSettings.apdDifficulty(3);
        Dungeon.hero=new Hero();Dungeon.hero.damageInterrupt=false;
    }
    @After public void restore() {
        SPDSettings.apdCustomRules(preferences);GamesInProgress.selectedClass=selected;GamesInProgress.curSlot=slot;
        SPDSettings.challenges(challenges);SPDSettings.apdDifficulty(difficulty);
        Dungeon.apdRestoreCustomRules(new Bundle());
    }
    private void rules(CustomDungeonRules rules) {
        Bundle b=new Bundle(), inner=new Bundle();rules.store(inner);b.put("apd_custom_rules",inner);
        Dungeon.apdRestoreCustomRules(b);
    }
    private void value(CustomDungeonRules.Slider s,int value) { rules(CustomDungeonRules.defaults().with(s,value)); }
    @Test public void everyAccuracyAndEvasionBoundaryScalesFiniteSkillsWithoutRng() {
        Hero hero=new Hero();Rat enemy=new Rat();Rat ally=new Rat();ally.alignment=Char.Alignment.ALLY;
        for(CustomDungeonRules.Slider s:new CustomDungeonRules.Slider[]{CustomDungeonRules.Slider.PLAYER_ACCURACY,CustomDungeonRules.Slider.ENEMY_ACCURACY,CustomDungeonRules.Slider.PLAYER_EVASION,CustomDungeonRules.Slider.ENEMY_EVASION}) {
            for(int v=s.min;v<=s.max;v+=s.step()) {
                value(s,v);
                Char actor=s==CustomDungeonRules.Slider.PLAYER_ACCURACY || s==CustomDungeonRules.Slider.PLAYER_EVASION ? hero : enemy;
                float actual=s==CustomDungeonRules.Slider.PLAYER_ACCURACY || s==CustomDungeonRules.Slider.ENEMY_ACCURACY
                    ? CustomCombatHooks.accuracy(actor,20) : CustomCombatHooks.evasion(actor,20);
                assertEquals(20f*v/100,actual,0);
                assertEquals(20,CustomCombatHooks.accuracy(ally,20),0);assertEquals(20,CustomCombatHooks.evasion(ally,20),0);
            }
        }
    }
    @Test public void zeroEvasionActuallyChangesHitResolutionAndKeepsRandomDrawCount() {
        value(CustomDungeonRules.Slider.ENEMY_EVASION,0);
        Hero hero=Dungeon.hero;Rat enemy=new Rat();
        Random.pushGenerator(88233);long expected;
        try { for(int i=0;i<1000;i++){Random.Float();Random.Float();}expected=Random.Long(); }
        finally { Random.popGenerator(); }
        Random.pushGenerator(88233);
        try { for(int i=0;i<1000;i++)assertTrue(Char.hit(hero,enemy,1,false));assertEquals(expected,Random.Long()); }
        finally { Random.popGenerator(); }
    }
    @Test public void infiniteSkillsAndSurpriseRulesArePreserved() {
        value(CustomDungeonRules.Slider.PLAYER_ACCURACY,25);
        assertEquals(Char.INFINITE_ACCURACY,CustomCombatHooks.accuracy(Dungeon.hero,Char.INFINITE_ACCURACY),0);
        value(CustomDungeonRules.Slider.PLAYER_EVASION,0);
        assertEquals(Char.INFINITE_EVASION,CustomCombatHooks.evasion(Dungeon.hero,Char.INFINITE_EVASION),0);
    }
    @Test public void allClassesStartAtSelectedMaximumAndLevelGainKeepsTheRule() {
        for(HeroClass c:HeroClass.values()) for(int v:new int[]{25,100,500}) {
            SPDSettings.apdCustomRules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_HP,v));
            GamesInProgress.selectedClass=c;Dungeon.init();
            assertEquals(20*v/100,Dungeon.hero.HT);assertEquals(Dungeon.hero.HT,Dungeon.hero.HP);
            Dungeon.hero.lvl=2;Dungeon.hero.updateHT(true);
            assertEquals(Math.round(25*v/100f),Dungeon.hero.HT);assertEquals(Dungeon.hero.HT,Dungeon.hero.HP);
        }
    }
    @Test public void maximumHpDoesNotCompoundAcrossUpdatesOrSaveLoad() throws Exception {
        SPDSettings.apdCustomRules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_HP,500));
        Dungeon.init();Dungeon.hero.lvl=3;Dungeon.hero.updateHT(true);
        assertEquals(150,Dungeon.hero.HT);Dungeon.hero.HP=77;
        for(int i=0;i<5;i++)Dungeon.hero.updateHT(false);
        assertEquals(150,Dungeon.hero.HT);assertEquals(77,Dungeon.hero.HP);
        Dungeon.depth=1;Dungeon.saveGame(1);
        SPDSettings.apdCustomRules(CustomDungeonRules.defaults());Dungeon.apdRestoreCustomRules(new Bundle());
        Dungeon.loadGame(1,false);Dungeon.hero.updateHT(false);
        assertEquals(150,Dungeon.hero.HT);assertEquals(77,Dungeon.hero.HP);
    }
    @Test public void neutralMetadataAndCombatFactorsAreExactIdentities() {
        rules(CustomDungeonRules.defaults());
        float[] skills={0,1,7.25f,999999f,Char.INFINITE_ACCURACY};
        for(float skill:skills) {
            assertEquals(Float.floatToIntBits(skill),Float.floatToIntBits(CustomCombatHooks.accuracy(Dungeon.hero,skill)));
            assertEquals(Float.floatToIntBits(skill),Float.floatToIntBits(CustomCombatHooks.evasion(new Rat(),skill)));
        }
        assertEquals(Integer.MAX_VALUE,CustomCombatHooks.playerMaximumHp(Integer.MAX_VALUE));
    }
}
