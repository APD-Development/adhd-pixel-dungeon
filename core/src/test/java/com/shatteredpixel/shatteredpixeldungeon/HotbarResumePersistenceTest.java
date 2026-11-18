package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.CategoryHotbar;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike;
import com.watabou.utils.FileUtils;
import com.watabou.utils.GameSettings;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

/** Real save files, fresh QuickSlot instance, preference-file reopen, and Dungeon.loadGame. */
public class HotbarResumePersistenceTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Preferences oldPreferences;private File preferences;
    @Before public void setup()throws Exception{
        HeadlessTestSupport.start();Field f=GameSettings.class.getDeclaredField("prefs");f.setAccessible(true);oldPreferences=(Preferences)f.get(null);
        preferences=new File(temp.getRoot(),"preferences.xml");GameSettings.set(new HeadlessPreferences(new FileHandle(preferences)));
        FileUtils.setDefaultFileProperties(Files.FileType.Absolute,temp.newFolder("saves").getAbsolutePath()+File.separator);
        ApdRecorder.reset();SPDSettings.challenges(0);SPDSettings.intro(false);SPDSettings.waterskinStartingSlot(1);
    }
    @After public void cleanup(){ApdRecorder.reset();GameSettings.set(oldPreferences);FileUtils.setDefaultFileProperties(Files.FileType.Absolute,new File(HeadlessTestSupport.tempDir(),"save").getAbsolutePath()+File.separator);GamesInProgress.selectedClass=null;GamesInProgress.curSlot=0;}
    private void create()throws Exception{
        GamesInProgress.curSlot=1;GamesInProgress.selectedClass=HeroClass.DUELIST;Dungeon.seed=704214;Dungeon.customSeedText="";Dungeon.daily=false;Dungeon.dailyReplay=false;
        Dungeon.init();Dungeon.level=Dungeon.newLevel();Dungeon.switchLevel(Dungeon.level,-1);
        Waterskin skin=Dungeon.hero.belongings.getItem(Waterskin.class);ThrowingSpike spikes=Dungeon.hero.belongings.getItem(ThrowingSpike.class);
        assertNotNull(skin);assertNotNull(spikes);Dungeon.quickslot.setSlot(1,skin);Dungeon.quickslot.setSlot(4,spikes);
        Food food=Dungeon.hero.belongings.getItem(Food.class);assertNotNull(food);Dungeon.quickslot.setSlot(2,food);
        Item scroll=new ScrollOfIdentify();Dungeon.hero.belongings.backpack.items.add(scroll);Dungeon.quickslot.setSlot(3,scroll);
        Item healing=new PotionOfHealing();Dungeon.hero.belongings.backpack.items.add(healing);Dungeon.quickslot.setSlot(5,healing);
    }
    private String[] snapshot(){String[] s=new String[Dungeon.quickslot.capacity()];for(int i=0;i<s.length;i++){Item item=Dungeon.quickslot.getItem(i);s[i]=item==null?null:item.getClass().getName()+"/"+item.quantity();}return s;}
    private void reload()throws Exception{Dungeon.hero=null;Dungeon.quickslot=new QuickSlot(6);GameSettings.set(new HeadlessPreferences(new FileHandle(preferences)));Dungeon.loadGame(1);}
    @Test public void reportedWaterskinTwoSpikesFiveAndOtherItemsSurviveDiskReload()throws Exception{
        create();String[] before=snapshot();Dungeon.saveGame(1);reload();assertArrayEquals(before,snapshot());assertEquals(1,SPDSettings.waterskinStartingSlot());
        assertTrue(Dungeon.quickslot.getItem(1) instanceof Waterskin);assertTrue(Dungeon.quickslot.getItem(4) instanceof ThrowingSpike);
    }
    @Test public void newRunPreferenceNeverOverridesManualPositionsOrAnUnassignedWaterskinOnResume()throws Exception{
        for(int preference:new int[]{-1,0,1,4,5,6}){
            create();Dungeon.quickslot.clearSlot(2);String[] before=snapshot();Dungeon.saveGame(1);SPDSettings.waterskinStartingSlot(preference);reload();assertArrayEquals("Preference "+preference,before,snapshot());assertEquals(preference,SPDSettings.waterskinStartingSlot());
            Dungeon.quickslot.clearItem(Dungeon.hero.belongings.getItem(Waterskin.class));before=snapshot();Dungeon.saveGame(1);reload();assertArrayEquals("Manual unassignment must persist",before,snapshot());
        }
    }
    @Test public void everyLayoutKeepsClassicAssignmentsAndSavedCustomTypesAcrossReload()throws Exception{
        create();CategoryHotbar.chooseItem(1,Dungeon.hero.belongings.getItem(Waterskin.class));CategoryHotbar.chooseItem(4,Dungeon.hero.belongings.getItem(ThrowingSpike.class));
        String skin=SPDSettings.apdHotbarItem(1),spikes=SPDSettings.apdHotbarItem(4);String[] before=snapshot();
        for(int mode=0;mode<CategoryHotbar.MODES.length;mode++){
            SPDSettings.apdHotbarMode(mode);Dungeon.saveGame(1);reload();assertEquals(mode,SPDSettings.apdHotbarMode());assertEquals(skin,SPDSettings.apdHotbarItem(1));assertEquals(spikes,SPDSettings.apdHotbarItem(4));assertArrayEquals(before,snapshot());
        }
    }
    @Test public void savedPlaceholderAndEmptySlotsSurviveWithoutBeingFilledFromStartingPreference()throws Exception{
        create();Item consumed=Dungeon.quickslot.getItem(4);Dungeon.quickslot.convertToPlaceholder(consumed);
        assertTrue("A depleted stack leaves inventory before its placeholder is saved",Dungeon.hero.belongings.backpack.items.remove(consumed));
        Dungeon.quickslot.clearSlot(2);String[] before=snapshot();Dungeon.saveGame(1);SPDSettings.waterskinStartingSlot(-1);reload();assertArrayEquals(before,snapshot());assertEquals(0,Dungeon.quickslot.getItem(4).quantity());assertNull(Dungeon.quickslot.getItem(2));
    }
    @Test public void manualUserAssignmentsReachDiskBeforeAnyAppPauseOrNextTurn()throws Exception{
        create();Dungeon.hero.ready=true;String[] before=snapshot();int hp=Dungeon.hero.HP;float turns=Statistics.duration;long started=System.nanoTime();
        com.watabou.utils.Random.pushGenerator(93841);long expected;try{expected=com.watabou.utils.Random.Long();}finally{com.watabou.utils.Random.popGenerator();}
        com.watabou.utils.Random.pushGenerator(93841);try{
            com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.set(1,Dungeon.hero.belongings.getItem(Waterskin.class));
            com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.set(4,Dungeon.hero.belongings.getItem(ThrowingSpike.class));
            assertEquals("Saving a shortcut consumes no gameplay RNG",expected,com.watabou.utils.Random.Long());
        }finally{com.watabou.utils.Random.popGenerator();}
        assertEquals(hp,Dungeon.hero.HP);assertEquals(turns,Statistics.duration,0);
        System.out.println("HOTBAR_SAVE_PROBE commands=2 elapsedNs="+(System.nanoTime()-started)+" gameBytes="+FileUtils.getFileHandle(GamesInProgress.gameFile(1)).length());
        reload();assertArrayEquals("No explicit save or pause happened after these user edits",before,snapshot());
    }
}
