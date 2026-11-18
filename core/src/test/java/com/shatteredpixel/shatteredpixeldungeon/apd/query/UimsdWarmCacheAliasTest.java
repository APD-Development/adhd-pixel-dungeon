/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/** Optional title preparation must not inspect aliased history sources. */
public class UimsdWarmCacheAliasTest {
    private static String stamp(File root) throws Exception {
        Method method = UimsdWarmCache.class.getDeclaredMethod("stamp", File.class);
        method.setAccessible(true);
        return (String) method.invoke(null, root);
    }

    private static boolean canPrepare(File root) throws Exception {
        Method method = UimsdWarmCache.class.getDeclaredMethod("canPrepare", File.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(null, root);
    }

    @After public void cleanup() {
        UimsdWarmCache.releaseForGameplay();
        UimsdWarmCache.resetForTest();
    }

    @Test public void externalLogAliasCannotBeFingerprintedOrPrewarmed() throws Exception {
        Path root = Files.createTempDirectory("apd-warm-alias-root");
        Path foreign = Files.createTempDirectory("apd-warm-alias-foreign");
        Path runs = Files.createDirectory(root.resolve("runs"));
        Path original = foreign.resolve("unrelated.apdlog");
        byte[] bytes = "unrelated-history-sentinel".getBytes(StandardCharsets.UTF_8);
        Path alias = runs.resolve("aliased.apdlog");
        try {
            Files.write(original, bytes);
            Files.createSymbolicLink(alias, original);
            assertNull("optional background fingerprint must refuse linked raw logs",
                    stamp(root.toFile()));
            assertFalse("optional memory preflight must refuse linked raw logs",
                    canPrepare(root.toFile()));
            UimsdWarmCache.prewarm(root.toFile(), "rules-a", "label", false);
            UimsdWarmCache.awaitPrewarmForTest(10000);
            assertNull("no cached model may be prepared from an aliased history",
                    UimsdWarmCache.takeReady(root.toFile(), "rules-a", "label", false));
            assertArrayEquals(bytes, Files.readAllBytes(original));
        } finally {
            Files.deleteIfExists(alias);
            Files.deleteIfExists(runs);
            Files.deleteIfExists(root);
            Files.deleteIfExists(original);
            Files.deleteIfExists(foreign);
        }
    }

    @Test public void linkedChildFolderAndMetadataCannotBecomeTitlePreloadSources() throws Exception {
        Path root = Files.createTempDirectory("apd-warm-child-alias-root");
        Path foreign = Files.createTempDirectory("apd-warm-child-alias-foreign");
        Path externalLog = foreign.resolve("external.apdlog");
        Path externalMetadata = foreign.resolve("external.txt");
        Path runsLink = root.resolve("runs");
        Path layoutsLink = root.resolve(UimsdModel.COLUMN_LAYOUTS_FILE);
        Path markerLink = root.resolve(DatasetStore.DEMO_MARKER_FILE);
        try {
            Files.write(externalLog, "foreign-run".getBytes(StandardCharsets.UTF_8));
            Files.write(externalMetadata, "foreign-metadata".getBytes(StandardCharsets.UTF_8));
            Files.createSymbolicLink(runsLink, foreign);
            assertNull("child runs folder must not redirect fingerprinting", stamp(root.toFile()));
            assertFalse("child runs folder must not redirect memory preflight", canPrepare(root.toFile()));
            Files.delete(runsLink);
            Files.createSymbolicLink(layoutsLink, externalMetadata);
            assertNull("linked column layouts must not be read by optional prewarm", stamp(root.toFile()));
            Files.delete(layoutsLink);
            Files.createSymbolicLink(markerLink, externalMetadata);
            assertNull("linked demo marker must not be read by optional prewarm", stamp(root.toFile()));
        } finally {
            Files.deleteIfExists(markerLink);
            Files.deleteIfExists(layoutsLink);
            Files.deleteIfExists(runsLink);
            Files.deleteIfExists(root);
            Files.deleteIfExists(externalLog);
            Files.deleteIfExists(externalMetadata);
            Files.deleteIfExists(foreign);
        }
    }

    @Test public void validSymlinkedStorageRootCanStillWarmAndTransfer() throws Exception {
        Path real = Files.createTempDirectory("apd-warm-canonical-data");
        Path parent = Files.createTempDirectory("apd-warm-canonical-parent");
        Path alias = parent.resolve("history");
        UimsdModel opened = null;
        try {
            Files.createSymbolicLink(alias, real);
            assertNotNull("a linked storage root itself is allowed", stamp(alias.toFile()));
            assertTrue("empty valid histories stay eligible", canPrepare(alias.toFile()));
            UimsdWarmCache.prewarm(alias.toFile(), "rules-a", "label", false);
            UimsdWarmCache.awaitPrewarmForTest(10000);
            opened = UimsdWarmCache.takeReady(alias.toFile(), "rules-a", "label", false);
            assertNotNull("the original storage-root behavior must be preserved", opened);
            assertEquals(UimsdModel.State.READY, opened.state());
        } finally {
            if (opened != null) opened.releaseForGameplay();
            Files.deleteIfExists(alias);
            Files.deleteIfExists(parent);
            Files.deleteIfExists(real);
        }
    }
}
