package com.shatteredpixel.shatteredpixeldungeon.apd.visibility;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.GameSettings;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
public class VisibilityPreferencesTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();private Preferences previous;private File file;
 @Before public void setup() throws Exception {HeadlessTestSupport.start();Field f=GameSettings.class.getDeclaredField("prefs");f.setAccessible(true);previous=(Preferences)f.get(null);file=new File(temp.newFolder(),"settings.xml");GameSettings.set(new HeadlessPreferences(new FileHandle(file)));}
 @After public void cleanup(){GameSettings.set(previous);}
 @Test public void originalVisualsAreTheDefaultAndStairControlsStartOff(){assertFalse(SPDSettings.enhancedTileVisibility());assertFalse(SPDSettings.stairNavigationShortcut());}
 @Test public void bothOptionsPersistAcrossPreferenceReload(){SPDSettings.enhancedTileVisibility(true);SPDSettings.stairNavigationShortcut(true);GameSettings.set(new HeadlessPreferences(new FileHandle(file)));assertTrue(SPDSettings.enhancedTileVisibility());assertTrue(SPDSettings.stairNavigationShortcut());}
 @Test public void turningEnhancementOffPreservesTheOriginalGridChoice(){SPDSettings.visualGrid(1);SPDSettings.enhancedTileVisibility(true);SPDSettings.enhancedTileVisibility(false);assertEquals(1,SPDSettings.visualGrid());}
}
