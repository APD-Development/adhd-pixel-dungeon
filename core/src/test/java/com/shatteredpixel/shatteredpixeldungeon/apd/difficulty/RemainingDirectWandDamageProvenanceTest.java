package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.DamageWand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class RemainingDirectWandDamageProvenanceTest {

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

    private static class ProbeWand extends DamageWand {
        @Override public int min(int level) { return 0; }
        @Override public int max(int level) { return 0; }
        @Override public void onZap(Ballistica attack) {}
        @Override public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {}

        static void apply(Receiver target, int raw, Object source, Char creator) {
            apdApplyDirectDamage(target, raw, source, creator);
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
    public void sharedDirectWandBoundaryUsesEveryActualCreatorFactorOnce() {
        Char[] creators = {new Hero(), new Rat(), new Goo()};
        for (int role = 0; role < creators.length; role++) {
            for (int percentage = 25; percentage <= 500; percentage += 25) {
                rules(role == 0 ? percentage : 275,
                        role == 1 ? percentage : 325,
                        role == 2 ? percentage : 425);
                Receiver target = new Receiver();
                Object source = new ProbeWand();
                ProbeWand.apply(target, 41, source, creators[role]);
                assertEquals(Math.round(41 * percentage / 100f), target.amount);
                assertEquals(1, target.calls);
                assertSame(source, target.source);
            }
        }
    }

    @Test
    public void neutralBoundaryPreservesDirectDamageSourceAndNextRng() {
        rules(100, 500, 500);
        Receiver target = new Receiver();
        Object source = new ProbeWand();
        Random.pushGenerator(49127);
        long expected;
        try {
            expected = Random.Long();
        } finally {
            Random.popGenerator();
        }
        Random.pushGenerator(49127);
        try {
            ProbeWand.apply(target, 31, source, new Hero());
            assertEquals(expected, Random.Long());
        } finally {
            Random.popGenerator();
        }
        assertEquals(31, target.amount);
        assertEquals(1, target.calls);
        assertSame(source, target.source);
    }

    @Test
    public void everyKnownDirectWandPathUsesTheSharedBoundary() throws Exception {
        Path root = Paths.get(System.getProperty("apd.game.src"));
        String base = Files.readString(root.resolve("items/wands/DamageWand.java"));
        String blast = Files.readString(root.resolve("items/wands/WandOfBlastWave.java"));
        String earth = Files.readString(root.resolve("items/wands/WandOfLivingEarth.java"));
        String prism = Files.readString(root.resolve("items/wands/WandOfPrismaticLight.java"));
        assertEquals(1, occurrences(base, "apdApplyDirectDamage("));
        assertEquals(3, occurrences(blast, "apdApplyDirectDamage("));
        assertEquals(2, occurrences(earth, "apdApplyDirectDamage("));
        assertEquals(2, occurrences(prism, "apdApplyDirectDamage("));
        assertFalse(blast.contains("ch.damage(damageRoll(), this)"));
        assertFalse(blast.contains("defender.damage(Math.round(procChanceMultiplier(attacker) * dmg), this)"));
        assertFalse(earth.contains("ch.damage(damage, this)"));
        assertFalse(prism.contains("ch.damage(dmg, this)"));
        assertFalse(prism.contains("ch.damage(Math.round(dmg*1.333f), this)"));
    }
}
