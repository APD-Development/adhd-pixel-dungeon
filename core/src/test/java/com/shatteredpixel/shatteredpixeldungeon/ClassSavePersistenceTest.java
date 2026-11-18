package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.Files;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.noosa.Game;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.*;

/** Real core save and restore at expansion IDs, no synthetic item/mob data. */
public class ClassSavePersistenceTest {
    @Rule public TemporaryFolder root = new TemporaryFolder();

    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        FileUtils.setDefaultFileProperties(Files.FileType.Absolute,
                root.newFolder("active-runs").getAbsolutePath() + File.separator);
        ApdRecorder.reset();
        SPDSettings.challenges(0);
        SPDSettings.intro(false);
    }

    @After public void tearDown() {
        ApdRecorder.reset();
        FileUtils.setDefaultFileProperties(Files.FileType.Absolute,
                new File(HeadlessTestSupport.tempDir(), "save").getAbsolutePath() + File.separator);
        GamesInProgress.selectedClass = null;
        GamesInProgress.curSlot = 0;
    }

    @Test public void savedExpansionRunsKeepClassEquipmentRulesAndRunStateOnReload() throws Exception {
        for (HeroClass cls : HeroClass.values()) {
            int slot=GamesInProgress.dedicatedSlot(cls,cls.ordinal()+1);
            GamesInProgress.selectedClass=cls;
            GamesInProgress.curSlot=slot;
            Dungeon.seed=120038L+cls.ordinal();
            Dungeon.customSeedText="";
            Dungeon.daily=false;
            Dungeon.dailyReplay=false;
            Game.realTime=1_700_000_000_000L+cls.ordinal()*1000;
            Dungeon.init();
            assertEquals(cls,Dungeon.hero.heroClass);
            int items=0;
            for (com.shatteredpixel.shatteredpixeldungeon.items.Item i : Dungeon.hero.belongings) items++;
            Dungeon.depth=1;
            Dungeon.branch=0;
            Dungeon.level=Dungeon.newLevel();
            Dungeon.switchLevel(Dungeon.level,-1);
            assertTrue("save did not materialize for "+cls,GamesInProgress.gameExists(slot));
            Bundle raw=FileUtils.bundleFromFile(GamesInProgress.gameFile(slot));
            assertTrue(raw.contains("hero"));
            assertTrue(raw.contains("apd_custom_rules"));
            assertTrue(raw.contains("apd_difficulty"));
            GamesInProgress.setUnknown(slot);
            assertEquals("preview wrong class",cls,GamesInProgress.check(slot).heroClass);
            Dungeon.hero=null;
            Dungeon.loadGame(slot);
            assertEquals("reload wrong hero",cls,Dungeon.hero.heroClass);
            assertEquals("reload wrong physical ID",slot,GamesInProgress.curSlot);
            assertNotNull("Custom rules lost",Dungeon.customRules());
            assertEquals("Restored difficulty",raw.getInt("apd_difficulty"),Dungeon.difficulty);
            int after=0;
            for (com.shatteredpixel.shatteredpixeldungeon.items.Item i : Dungeon.hero.belongings) after++;
            assertEquals("Inventory changed during save/load for "+cls,items,after);
            assertTrue("Saved floor missing",GamesInProgress.gameExists(slot));
            assertNotNull("Saved floor unavailable",Dungeon.loadLevel(slot));
        }
    }

    @Test public void legacySaveFolderNameStaysFixedAndNewSaveFoldersAreSeparate() {
        for (int legacy=1;legacy<=6;legacy++) {
            assertEquals("game"+legacy,GamesInProgress.gameFolder(legacy));
            assertEquals("game"+legacy+"/game.dat",GamesInProgress.gameFile(legacy));
        }
        for(HeroClass cls:HeroClass.values())for(int pos=0;pos<20;pos++)
            assertTrue(GamesInProgress.dedicatedSlot(cls,pos)>6);
    }
}
