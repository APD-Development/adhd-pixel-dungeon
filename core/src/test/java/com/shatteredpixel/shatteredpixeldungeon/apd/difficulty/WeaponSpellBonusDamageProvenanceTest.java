package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class WeaponSpellBonusDamageProvenanceTest {

    private Bundle previousRules;

    private static class Receiver extends Char {
        int amount;
        int calls;
        Object source;

        @Override
        public void damage(int damage, Object originalSource) {
            amount = damage;
            source = originalSource;
            calls++;
        }
    }

    private abstract static class ProbeWeapon extends Weapon {
        static void apply(Receiver target, int raw, Object source, Char creator) {
            apdApplyHeroSpellBonusDamage(target, raw, source, creator);
        }
    }

    @Before
    public void setup() {
        HeadlessTestSupport.start();
        previousRules = new Bundle();
        Dungeon.apdStoreCustomRules(previousRules);
    }

    @After
    public void cleanup() {
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

    @Test
    public void independentWeaponSpellBonusUsesActualCreatorFactorOnce() {
        Char[] creators = {new Hero(), new Rat(), new Goo()};
        for (int role = 0; role < creators.length; role++) {
            for (int percentage = 25; percentage <= 500; percentage += 25) {
                rules(role == 0 ? percentage : 275,
                        role == 1 ? percentage : 325,
                        role == 2 ? percentage : 425);
                Receiver target = new Receiver();
                Object source = new Object();
                ProbeWeapon.apply(target, 23, source, creators[role]);
                assertEquals(Math.round(23 * percentage / 100f), target.amount);
                assertEquals(1, target.calls);
                assertSame(source, target.source);
            }
        }
    }

    @Test
    public void neutralWeaponSpellBonusPreservesAmountSourceAndNextRng() {
        rules(100, 500, 500);
        Receiver target = new Receiver();
        Object source = new Object();
        Random.pushGenerator(73129);
        long expected;
        try {
            expected = Random.Long();
        } finally {
            Random.popGenerator();
        }
        Random.pushGenerator(73129);
        try {
            ProbeWeapon.apply(target, 17, source, new Hero());
            assertEquals(expected, Random.Long());
        } finally {
            Random.popGenerator();
        }
        assertEquals(17, target.amount);
        assertEquals(1, target.calls);
        assertSame(source, target.source);
    }

    @Test
    public void holyWeaponAndSmiteUseIndependentBonusBoundary() throws Exception {
        Path root = Paths.get(System.getProperty("apd.game.src"));
        String source = Files.readString(root.resolve("items/weapon/Weapon.java"));
        assertEquals(3, occurrences(source, "apdApplyHeroSpellBonusDamage("));
        assertFalse(source.contains("defender.damage(Math.round(dmg * Enchantment.genericProcChanceMultiplier(attacker)), HolyWeapon.INSTANCE)"));
        assertFalse(source.contains("defender.damage(Smite.bonusDmg((Hero) attacker, defender), Smite.INSTANCE)"));
        assertTrue(source.contains("HolyWeapon.INSTANCE, attacker"));
        assertTrue(source.contains("Smite.bonusDmg((Hero) attacker, defender), Smite.INSTANCE, attacker"));
    }
}
