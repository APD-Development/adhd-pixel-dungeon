package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class ApdFeaturesStartupTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private Files previousFiles;

    @Before public void beforeFileServiceExists() throws Exception {
        previousFiles = Gdx.files;
        Gdx.files = null;
        ApdFeatures.resetForTest();
        Field loaded = ApdFeatures.class.getDeclaredField("loaded");
        loaded.setAccessible(true);
        loaded.setBoolean(null, false);
    }

    @After public void restore() {
        ApdFeatures.resetForTest();
        Gdx.files = previousFiles;
    }

    @Test public void earlyCheckDoesNotHideThePrivateBuildAsset() throws Exception {
        assertFalse(ApdFeatures.demoUnlock());
        assertFalse(ApdFeatures.testBuild());
        File asset = folder.newFile("features.txt");
        java.nio.file.Files.write(asset.toPath(), "testBuild\ndemoUnlock\n".getBytes(StandardCharsets.UTF_8));
        installFileService(asset);
        assertTrue(ApdFeatures.testBuild());
        assertTrue(ApdFeatures.demoUnlock());
        assertFalse(ApdFeatures.clericPlayable());
        assertFalse(ApdFeatures.grudge());
    }

    @Test public void normalBuildWithoutTheAssetStaysOff() {
        assertFalse(ApdFeatures.demoUnlock());
        installFileService(new File(folder.getRoot(), "absent.txt"));
        assertFalse(ApdFeatures.demoUnlock());
        assertFalse(ApdFeatures.testBuild());
        assertFalse(ApdFeatures.clericPlayable());
        assertFalse(ApdFeatures.grudge());
    }

    @Test public void unreadableAssetDoesNotEnablePrivateFeatures() throws Exception {
        assertFalse(ApdFeatures.demoUnlock());
        installFileService(folder.newFolder("not-a-readable-file"));
        assertFalse(ApdFeatures.demoUnlock());
        assertFalse(ApdFeatures.testBuild());
        assertFalse(ApdFeatures.clericPlayable());
    }

    private void installFileService(File asset) {
        Gdx.files = (Files) Proxy.newProxyInstance(Files.class.getClassLoader(), new Class<?>[]{Files.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("internal")) {
                        assertEquals("apd/features.txt", arguments[0]);
                        return new FileHandle(asset);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
