package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class CustomRulesSaveIntegrationTest {
    private CustomDungeonRules preferences;
    private int difficulty, challenges;
    private HeroClass selected;
    private int slot;
    @Before public void setup() {
        HeadlessTestSupport.start();
        preferences=SPDSettings.apdCustomRules();
        difficulty=SPDSettings.apdDifficulty(); challenges=SPDSettings.challenges();
        selected=GamesInProgress.selectedClass; slot=GamesInProgress.curSlot;
        GamesInProgress.curSlot=1;
    }
    @After public void cleanup() {
        SPDSettings.apdCustomRules(preferences);
        SPDSettings.apdDifficulty(difficulty); SPDSettings.challenges(challenges);
        GamesInProgress.selectedClass=selected; GamesInProgress.curSlot=slot;
        Dungeon.apdRestoreCustomRules(new Bundle());
    }
    @Test public void allScrollCountsSurviveActualGameSaveLoadWithClassesAndContexts() throws Exception {
        HeroClass[] classes=HeroClass.values();
        assertEquals(6,classes.length);
        for (int n=1;n<=60;n++) {
            CustomDungeonRules desired=CustomDungeonRules.defaults()
                .with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,n)
                .with(CustomDungeonRules.Slider.ENEMY_HP,25+25*(n%20));
            SPDSettings.apdCustomRules(desired);
            SPDSettings.apdDifficulty(1+n%5);
            SPDSettings.challenges(n%2==0 ? 511 : 0);
            GamesInProgress.selectedClass=classes[(n-1)%classes.length];
            Dungeon.init();
            assertEquals(desired.identityText(),Dungeon.customRules().identityText());
            SPDSettings.apdCustomRules(CustomDungeonRules.defaults());
            assertEquals("Changing next-run settings does not alter active run",desired.identityText(),Dungeon.customRules().identityText());
            Dungeon.depth=1;
            Dungeon.saveGame(1);
            Dungeon.apdRestoreCustomRules(new Bundle());
            assertNull(Dungeon.customRules());
            Dungeon.loadGame(1,false);
            assertEquals(desired.identityText(),Dungeon.customRules().identityText());
            assertEquals(1+n%5,Dungeon.difficulty);
            assertEquals(n%2==0 ? 511 : 0,Dungeon.challenges);
            assertEquals(classes[(n-1)%classes.length],Dungeon.hero.heroClass);
        }
    }
    @Test public void legacySaveRemainsUnknownAfterLoadAndResave() {
        Bundle current=new Bundle(); CustomDungeonRules.defaults().store(current);
        Bundle wrapper=new Bundle(); wrapper.put("apd_custom_rules",current);
        Dungeon.apdRestoreCustomRules(wrapper);
        assertNotNull(Dungeon.customRules());
        Dungeon.apdRestoreDifficulty(new Bundle());
        assertNull(Dungeon.customRules());
        Bundle resaved=new Bundle(); Dungeon.apdStoreCustomRules(resaved);
        assertFalse(resaved.contains("apd_custom_rules"));
    }
    @Test public void malformedRecordedRulesRejectRatherThanBorrowCurrentPreferences() {
        SPDSettings.apdCustomRules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,60));
        Bundle bad=new Bundle(); bad.put("custom_rules_version",999);
        Bundle wrapper=new Bundle(); wrapper.put("apd_custom_rules",bad);
        try { Dungeon.apdRestoreCustomRules(wrapper); fail("Malformed record accepted"); }
        catch (IllegalArgumentException expected) { }
        assertNull(Dungeon.customRules());
    }
    @Test public void settingsAndSaveMetadataDoNotConsumeGameplayRng() {
        SPDSettings.apdCustomRules(CustomDungeonRules.defaults());
        Random.pushGenerator(82345);
        int expected; try { expected=Random.Int(); } finally { Random.popGenerator(); }
        Random.pushGenerator(82345);
        try {
            CustomDungeonRules r=SPDSettings.apdCustomRules();
            Bundle b=new Bundle(); r.store(b); CustomDungeonRules.restore(b);
            assertEquals(expected,Random.Int());
        } finally { Random.popGenerator(); }
    }
}
