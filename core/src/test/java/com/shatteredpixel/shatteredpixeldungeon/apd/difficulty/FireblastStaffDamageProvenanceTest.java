package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class FireblastStaffDamageProvenanceTest {

    private Bundle previousRules;
    private Method apply;

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

    @Before
    public void setup() throws Exception {
        HeadlessTestSupport.start();
        previousRules = new Bundle();
        Dungeon.apdStoreCustomRules(previousRules);
        apply = WandOfFireblast.class.getDeclaredMethod(
                "apdApplyStaffExplosion", Char.class, Char.class, int.class, Object.class);
        apply.setAccessible(true);
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

    private void apply(Receiver target, Char attacker, int raw, Object source) throws Exception {
        apply.invoke(null, target, attacker, raw, source);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0; at += needle.length()) count++;
        return count;
    }

    @Test
    public void actualAttackerSelectsEveryPermittedFactorExactlyOnce() throws Exception {
        Char[] attackers = {new Hero(), new Rat(), new Goo()};
        for (int role = 0; role < attackers.length; role++) {
            for (int percentage = 25; percentage <= 500; percentage += 25) {
                rules(role == 0 ? percentage : 275,
                        role == 1 ? percentage : 325,
                        role == 2 ? percentage : 425);
                Receiver target = new Receiver();
                Object source = new WandOfFireblast();
                apply(target, attackers[role], 37, source);
                assertEquals(Math.round(37 * percentage / 100f), target.amount);
                assertEquals(1, target.calls);
                assertSame(source, target.source);
            }
        }
    }

    @Test
    public void neutralBoundaryPreservesAmountSourceAndNextRng() throws Exception {
        rules(100, 500, 500);
        Receiver target = new Receiver();
        Hero attacker = new Hero();
        Object source = new WandOfFireblast();
        Random.pushGenerator(6712);
        long expected;
        try {
            expected = Random.Long();
        } finally {
            Random.popGenerator();
        }
        Random.pushGenerator(6712);
        try {
            apply(target, attacker, 29, source);
            assertEquals(expected, Random.Long());
        } finally {
            Random.popGenerator();
        }
        assertEquals(29, target.amount);
        assertEquals(1, target.calls);
        assertSame(source, target.source);
    }

    @Test
    public void productionStaffExplosionUsesOnlyTheAttributedBoundary() throws Exception {
        Path root = Paths.get(System.getProperty("apd.game.src"));
        String source = Files.readString(root.resolve("items/wands/WandOfFireblast.java"));
        assertEquals(2, occurrences(source, "apdApplyStaffExplosion("));
        assertEquals(1, occurrences(source, "target, rawDamage, source, attacker"));
        assertFalse(source.contains(
                "ch.damage(Math.round(powerMulti*Hero.heroDamageIntRange"));
    }
}
