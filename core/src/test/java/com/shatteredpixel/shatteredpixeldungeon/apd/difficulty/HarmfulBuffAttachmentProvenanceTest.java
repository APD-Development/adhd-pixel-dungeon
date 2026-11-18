package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class HarmfulBuffAttachmentProvenanceTest {
    private static final int CELL = 12;
    private Hero hero;
    private Level level;
    private Bundle run;
    private Set<Actor> actors;

    private static class ProbeBoss extends VaultBossElemental {
        int received;
        int calls;
        Object source;
        @Override public void damage(int amount, Object originalSource) {
            received = amount;
            calls++;
            source = originalSource;
        }
    }

    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        hero = Dungeon.hero;
        level = Dungeon.level;
        run = new Bundle();
        Dungeon.apdStoreCustomRules(run);
        actors = Actor.all();
        Dungeon.hero = new Hero();
        Dungeon.level = new SewerLevel();
        Method size = Level.class.getDeclaredMethod("setSize", int.class, int.class);
        size.setAccessible(true);
        size.invoke(Dungeon.level, 5, 5);
        Arrays.fill(Dungeon.level.map, Terrain.EMPTY);
        Arrays.fill(Dungeon.level.passable, true);
        Dungeon.level.blobs = new java.util.HashMap<>();
        Dungeon.level.heaps = new SparseArray<>();
        Dungeon.level.plants = new SparseArray<>();
    }

    @After public void cleanup() {
        for (Actor actor : Actor.all()) if (!actors.contains(actor)) Actor.remove(actor);
        Dungeon.hero = hero;
        Dungeon.level = level;
        Dungeon.apdRestoreCustomRules(run);
        assertEquals(OffensiveDamageOrigin.UNATTRIBUTED, OffensiveEffectContext.current());
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

    private ProbeBoss boss(VaultBossElemental.ElementalForm form) throws Exception {
        ProbeBoss boss = new ProbeBoss();
        Field field = VaultBossElemental.class.getDeclaredField("form");
        field.setAccessible(true);
        field.set(boss, form);
        boss.pos = CELL;
        boss.HP = boss.HT = 600;
        Actor.add(boss);
        return boss;
    }

    private int reaction(int seed) {
        Random.pushGenerator(seed);
        try {
            return Random.NormalIntRange(5, 10);
        } finally {
            Random.popGenerator();
        }
    }

    private Freezing freezing(int amount, OffensiveDamageOrigin origin) {
        try (OffensiveEffectContext ignored = OffensiveEffectContext.enter(origin)) {
            Freezing field = new Freezing();
            field.seed(Dungeon.level, CELL, amount);
            return field;
        }
    }

    @Test public void burningIsAttributedBeforeVaultReaction() throws Exception {
        rules(250, 50, 300);
        ProbeBoss boss = boss(VaultBossElemental.ElementalForm.FROST);
        int seed = 91273;
        Random.pushGenerator(seed);
        Burning burning;
        try {
            burning = Burning.affectAttributed(boss, Dungeon.hero);
        } finally {
            Random.popGenerator();
        }
        assertEquals(Math.round(reaction(seed) * 2.5f), boss.received);
        assertEquals(1, boss.calls);
        assertSame(burning, boss.source);
        assertNull(burning.target);
    }

    @Test public void directChillUsesExplicitBossReplacementAndKeepsBuffSource() throws Exception {
        rules(250, 50, 300);
        ProbeBoss target = boss(VaultBossElemental.ElementalForm.FIRE);
        ProbeBoss creator = new ProbeBoss();
        int seed = 11881;
        Random.pushGenerator(seed);
        Chill chill;
        try {
            chill = Buff.affectAttributed(target, Chill.class, 3f,
                    OffensiveDamageOrigin.capture(creator, null));
        } finally {
            Random.popGenerator();
        }
        assertEquals(Math.round(reaction(seed) * 3f), target.received);
        assertEquals(1, target.calls);
        assertSame(chill, target.source);
    }

    @Test public void mixedFreezingFieldWeightsReactionOnceAndSurvivesSave() throws Exception {
        rules(200, 50, 400);
        Freezing field = freezing(5, OffensiveDamageOrigin.PLAYER);
        try (OffensiveEffectContext ignored = OffensiveEffectContext.enter(OffensiveDamageOrigin.BOSS)) {
            field.seed(Dungeon.level, CELL, 5);
        }
        Bundle saved = new Bundle();
        field.storeInBundle(saved);
        assertTrue(saved.contains("apd_freezing_origins_version"));
        Freezing restored = new Freezing();
        restored.restoreFromBundle(saved);
        ProbeBoss boss = boss(VaultBossElemental.ElementalForm.FIRE);
        int seed = 77441;
        Random.pushGenerator(seed);
        try {
            restored.act();
        } finally {
            Random.popGenerator();
        }
        assertEquals(Math.round(reaction(seed) * 3f), boss.received);
        assertEquals(1, boss.calls);
        assertTrue(boss.source instanceof Chill);
    }

    @Test public void legacyEnvironmentalFreezingRemainsUnscaled() throws Exception {
        rules(500, 25, 500);
        Freezing field = freezing(4, OffensiveDamageOrigin.UNATTRIBUTED);
        Bundle saved = new Bundle();
        field.storeInBundle(saved);
        assertFalse(saved.contains("apd_freezing_origins_version"));
        ProbeBoss boss = boss(VaultBossElemental.ElementalForm.FIRE);
        int seed = 55109;
        Random.pushGenerator(seed);
        try {
            field.act();
        } finally {
            Random.popGenerator();
        }
        assertEquals(reaction(seed), boss.received);
        assertEquals(1, boss.calls);
    }

    @Test public void neutralReactionPreservesAmountSourceAndRng() throws Exception {
        rules(100, 100, 100);
        ProbeBoss boss = boss(VaultBossElemental.ElementalForm.FROST);
        int seed = 66107;
        long expectedNext;
        Random.pushGenerator(seed);
        try {
            Random.NormalIntRange(5, 10);
            expectedNext = Random.Long();
        } finally {
            Random.popGenerator();
        }
        Random.pushGenerator(seed);
        Burning burning;
        long actualNext;
        try {
            burning = Burning.affectAttributed(boss, Dungeon.hero);
            actualNext = Random.Long();
        } finally {
            Random.popGenerator();
        }
        assertEquals(reaction(seed), boss.received);
        assertSame(burning, boss.source);
        assertEquals(expectedNext, actualNext);
    }
}
