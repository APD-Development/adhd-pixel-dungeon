package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.UimsdUnlock;
import com.watabou.utils.GameSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

//Uses the real settings adapter and libGDX's XML preferences, without a render loop.
//Reopening preferences models disk restoration, not Android process/device testing.
public class UimsdSettingsPersistenceTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private Preferences previous;
    private File file;
    private Preferences preferences;

    @Before public void isolatePreferences() throws Exception {
        Field field = GameSettings.class.getDeclaredField("prefs");
        field.setAccessible(true);
        previous = (Preferences) field.get(null);
        file = new File(folder.getRoot(), "settings.xml");
        reopen();
    }

    @After public void restorePreferences() { GameSettings.set(previous); }

    private void reopen() {
        preferences = new HeadlessPreferences(new FileHandle(file));
        GameSettings.set(preferences);
    }

    @Test public void cleanIsTheDefaultWithoutAnAgeProvider() {
        assertFalse(SPDSettings.apdAdultHumor());
        assertFalse(preferences.contains(SPDSettings.KEY_APD_ADULT_HUMOR));
    }

    @Test public void storedAdultChoiceIsEffectiveWithoutAnAgeProviderAndSurvivesReload() {
        preferences.putBoolean(SPDSettings.KEY_APD_ADULT_HUMOR, true).flush();
        assertTrue(SPDSettings.apdAdultHumor());
        reopen();
        assertTrue(preferences.getBoolean(SPDSettings.KEY_APD_ADULT_HUMOR));
        assertTrue(SPDSettings.apdAdultHumor());
    }

    @Test public void adultAndCleanSelectionsBothPersistWhenVerificationIsUnavailable() {
        SPDSettings.apdAdultHumor(true);
        assertTrue(SPDSettings.apdAdultHumor());
        reopen();
        assertTrue(SPDSettings.apdAdultHumor());
        SPDSettings.apdAdultHumor(false);
        reopen();
        assertFalse(preferences.getBoolean(SPDSettings.KEY_APD_ADULT_HUMOR, true));
        assertFalse(SPDSettings.apdAdultHumor());
    }

    @Test public void pendingTitleArrowUsesTheActualSavedHumorChoice() {
        UimsdGate.forceGateOnForTest(true);
        try {
            UimsdGate.FLAGS.setUnlocked();
            UimsdGate.FLAGS.setTourUnlocked();
            UimsdGate.FLAGS.setPromptPending();
            assertEquals(UimsdGate.TENTH_RUN_ARROW_CLEAN, UimsdGate.titleAnnotation(""));
            SPDSettings.apdAdultHumor(true);
            reopen();
            assertEquals(UimsdGate.TENTH_RUN_ARROW_ADULT, UimsdGate.titleAnnotation(""));
            SPDSettings.apdAdultHumor(false);
            reopen();
            assertEquals(UimsdGate.TENTH_RUN_ARROW_CLEAN, UimsdGate.titleAnnotation(""));
        } finally {
            UimsdGate.forceGateOnForTest(false);
        }
    }

    @Test public void genuineUnlockAndPromptRetirementSurviveSettingsReload() {
        UimsdGate.FLAGS.setUnlocked();
        UimsdGate.FLAGS.setTourUnlocked();
        UimsdGate.FLAGS.setPromptPending();
        reopen();
        assertTrue(UimsdGate.FLAGS.unlocked());
        assertTrue(UimsdGate.FLAGS.tourUnlocked());
        assertTrue(UimsdUnlock.promptActive(UimsdGate.FLAGS));
        assertTrue(UimsdUnlock.retirePrompt(UimsdGate.FLAGS));
        reopen();
        assertFalse(UimsdUnlock.promptActive(UimsdGate.FLAGS));
        assertTrue(UimsdUnlock.isTourUnlocked(UimsdGate.FLAGS, null, true));
        assertTrue(UimsdUnlock.isUnlocked(UimsdGate.FLAGS, null, true));
    }

    @Test public void demoUnlockReloadDoesNotOpenTheTourOrRestoreTheDemoButton() {
        UimsdGate.FLAGS.setUnlocked();
        UimsdGate.FLAGS.setDemoUsed();
        reopen();
        assertTrue(UimsdUnlock.isUnlocked(UimsdGate.FLAGS, null, true));
        assertFalse(UimsdUnlock.isTourUnlocked(UimsdGate.FLAGS, null, true));
        assertFalse(UimsdUnlock.demoButtonAvailable(UimsdGate.FLAGS, null, true));
        assertFalse(UimsdUnlock.promptActive(UimsdGate.FLAGS));
    }
}
