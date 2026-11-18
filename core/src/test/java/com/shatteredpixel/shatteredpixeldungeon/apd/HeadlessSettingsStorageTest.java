package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.files.FileHandle;
import com.watabou.utils.GameSettings;
import org.junit.Test;
import java.io.File;
import static org.junit.Assert.*;

public class HeadlessSettingsStorageTest {
    @Test public void gamePreferencesAreWrittenInsideTheThrowAwayFolder() throws Exception {
        HeadlessTestSupport.start();
        String key = "apd-test-storage-isolation";
        GameSettings.put(key, "temporary");
        File settings = new File(HeadlessTestSupport.tempDir(), "prefs/settings.xml");
        assertTrue(settings.isFile());
        assertTrue(settings.getCanonicalPath().startsWith(HeadlessTestSupport.tempDir().getCanonicalPath() + File.separator));
        assertEquals("temporary", new HeadlessPreferences(new FileHandle(settings)).getString(key));
    }
}
