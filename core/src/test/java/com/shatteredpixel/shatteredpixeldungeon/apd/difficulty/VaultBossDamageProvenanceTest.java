package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class VaultBossDamageProvenanceTest {
    private Bundle previousRules;

    private static class Receiver extends Char {
        int received;
        int calls;
        Object source;
        @Override public void damage(int amount, Object originalSource) {
            received = amount;
            calls++;
            source = originalSource;
        }
    }

    @Before public void setup() {
        HeadlessTestSupport.start();
        previousRules = new Bundle();
        Dungeon.apdStoreCustomRules(previousRules);
    }

    @After public void cleanup() {
        Dungeon.apdRestoreCustomRules(previousRules);
    }

    private void rules(int player, int enemy, int boss) {
        Bundle saved = new Bundle();
        Bundle custom = new Bundle();
        CustomDungeonRules.defaults()
                .with(CustomDungeonRules.Slider.PLAYER_DAMAGE, player)
                .with(CustomDungeonRules.Slider.ENEMY_DAMAGE, enemy)
                .with(CustomDungeonRules.Slider.BOSS_DAMAGE, boss)
                .store(custom);
        saved.put("apd_custom_rules", custom);
        Dungeon.apdRestoreCustomRules(saved);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0; at += needle.length()) count++;
        return count;
    }

    @Test public void concreteVaultBossUsesBossReplacementAcrossEveryPercentage() {
        VaultBossElemental boss = new VaultBossElemental();
        assertEquals(OffensiveDamageOrigin.BOSS, OffensiveDamageOrigin.capture(boss, null));
        Object source = new Object();
        for (int percentage = 25; percentage <= 500; percentage += 25) {
            rules(500, 300, percentage);
            Receiver receiver = new Receiver();
            CustomOffensiveHooks.raw(receiver, 17, source, boss);
            assertEquals(Math.round(17 * percentage / 100f), receiver.received);
            assertEquals(1, receiver.calls);
            assertSame(source, receiver.source);
        }
    }

    @Test public void neutralVaultBossDamagePreservesSourceAmountAndRng() {
        rules(100, 100, 100);
        VaultBossElemental boss = new VaultBossElemental();
        Receiver receiver = new Receiver();
        Object source = new Object();
        Random.pushGenerator(61741);
        long expected;
        try {
            expected = Random.Long();
        } finally {
            Random.popGenerator();
        }
        Random.pushGenerator(61741);
        try {
            CustomOffensiveHooks.raw(receiver, 23, source, boss);
            assertEquals(expected, Random.Long());
        } finally {
            Random.popGenerator();
        }
        assertEquals(23, receiver.received);
        assertEquals(1, receiver.calls);
        assertSame(source, receiver.source);
    }

    @Test public void everyKnownVaultBossAttackUsesOneExplicitBossBoundary() throws Exception {
        Path root = Paths.get(System.getProperty("apd.game.src"));
        String boss = Files.readString(root.resolve("actors/mobs/quest/vault/VaultBossElemental.java"));
        assertEquals(4, occurrences(boss, "CustomOffensiveHooks.raw("));
        assertTrue(boss.contains("CustomOffensiveHooks.raw(enemy, Random.IntRange(5, 10), new Shocking(), this);"));
        assertTrue(boss.contains("Fire.seedAttributed(cell+i, 2, OffensiveDamageOrigin.capture(this, null))"));
        assertTrue(boss.contains("Burning.affectAttributed(ch, this)"));
        assertTrue(boss.contains("Burning.affectAttributed(ch, 5, target)"));
        assertTrue(boss.contains("CustomOffensiveHooks.raw(ch, Random.NormalIntRange(10, 15), new Frost(), target);"));
        assertTrue(boss.contains("CustomOffensiveHooks.raw(ch, Random.NormalIntRange(20, 30), new Electricity(), this);"));
        assertTrue(boss.contains("CustomOffensiveHooks.raw(ch, Random.NormalIntRange(10, 15), new Electricity(), target);"));
        assertFalse(boss.contains("GameScene.add(Blob.seed(cell+i, 2, Fire.class));"));
        assertFalse(boss.contains("enemy.damage( Random.IntRange(5, 10), new Shocking() );"));
    }

    @Test public void vaultQuestMachinesRemainEnvironmentalAndUnscaled() throws Exception {
        Path root = Paths.get(System.getProperty("apd.game.src"));
        String sentry = Files.readString(root.resolve("actors/mobs/npcs/VaultSentry.java"));
        String flames = Files.readString(root.resolve("actors/blobs/VaultFlameTraps.java"));
        String laser = Files.readString(root.resolve("actors/mobs/npcs/VaultLaser.java"));
        for (String hazard : new String[]{sentry, flames, laser}) {
            assertFalse(hazard.contains("CustomOffensiveHooks"));
            assertFalse(hazard.contains("reigniteAttributed"));
            assertFalse(hazard.contains("seedAttributed"));
        }
        assertTrue(sentry.contains("ch.damage(Random.NormalIntRange(3, 6), new DM100.LightningBolt());"));
        assertTrue(sentry.contains("ch.damage(Random.NormalIntRange(6, 12), new DM100.LightningBolt());"));
        assertTrue(flames.contains("Buff.affect( ch, Burning.class ).reignite( ch, 4 );"));
        assertTrue(laser.contains("ch.damage(Random.NormalIntRange(10, 20), new Eye.DeathGaze());"));
    }
}
