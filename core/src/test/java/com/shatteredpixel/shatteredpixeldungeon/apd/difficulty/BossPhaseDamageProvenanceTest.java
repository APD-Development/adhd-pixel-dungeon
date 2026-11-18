package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BossPhaseDamageProvenanceTest {
    private static final int CELL = 12;
    private Hero previousHero;
    private Level previousLevel;
    private Bundle previousRules;
    private Set<Actor> previousActors;

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
        previousHero = Dungeon.hero;
        previousLevel = Dungeon.level;
        previousRules = new Bundle();
        Dungeon.apdStoreCustomRules(previousRules);
        previousActors = Actor.all();
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
        for (Actor actor : Actor.all()) if (!previousActors.contains(actor)) Actor.remove(actor);
        Dungeon.hero = previousHero;
        Dungeon.level = previousLevel;
        Dungeon.apdRestoreCustomRules(previousRules);
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

    private ProbeBoss reactionBoss() throws Exception {
        ProbeBoss boss = new ProbeBoss();
        Field form = VaultBossElemental.class.getDeclaredField("form");
        form.setAccessible(true);
        form.set(boss, VaultBossElemental.ElementalForm.FROST);
        boss.pos = CELL;
        boss.HP = boss.HT = 600;
        Actor.add(boss);
        return boss;
    }

    private static int reaction(int seed) {
        Random.pushGenerator(seed);
        try {
            return Random.NormalIntRange(5, 10);
        } finally {
            Random.popGenerator();
        }
    }

    @Test public void allSixHeroClassesAndFiveBossesSelectTheirReplacementSlider() {
        rules(200, 50, 300);
        HeroClass[] classes = HeroClass.values();
        assertEquals(6, classes.length);
        Object source = new Object();
        for (HeroClass heroClass : classes) {
            Hero hero = new Hero();
            hero.heroClass = heroClass;
            assertEquals(OffensiveDamageOrigin.PLAYER, OffensiveDamageOrigin.capture(hero, null));
            Receiver receiver = new Receiver();
            CustomOffensiveHooks.raw(receiver, 10, source, hero);
            assertEquals(20, receiver.received);
            assertEquals(1, receiver.calls);
            assertSame(source, receiver.source);
        }
        Char[] bosses = {new Goo(), new Tengu(), new DM300(), new DwarfKing(), new YogDzewa()};
        for (Char boss : bosses) {
            assertEquals(OffensiveDamageOrigin.BOSS, OffensiveDamageOrigin.capture(boss, null));
            Receiver receiver = new Receiver();
            CustomOffensiveHooks.raw(receiver, 10, source, boss);
            assertEquals(30, receiver.received);
            assertEquals(1, receiver.calls);
            assertSame(source, receiver.source);
        }
    }

    @Test public void tenguFireFieldKeepsMixedCreatorThroughSaveAndImmediateReaction() throws Exception {
        rules(200, 50, 400);
        Tengu.FireAbility.FireBlob field = new Tengu.FireAbility.FireBlob();
        try (OffensiveEffectContext ignored = OffensiveEffectContext.enter(OffensiveDamageOrigin.PLAYER)) {
            field.seed(Dungeon.level, CELL, 1);
        }
        try (OffensiveEffectContext ignored = OffensiveEffectContext.enter(OffensiveDamageOrigin.BOSS)) {
            field.seed(Dungeon.level, CELL, 1);
        }
        Bundle saved = new Bundle();
        field.storeInBundle(saved);
        assertTrue(saved.contains("apd_tengu_fire_origins_version"));
        Tengu.FireAbility.FireBlob restored = new Tengu.FireAbility.FireBlob();
        restored.restoreFromBundle(saved);
        Field originsField = Tengu.FireAbility.FireBlob.class.getDeclaredField("apdOrigins");
        originsField.setAccessible(true);
        CellDamageOrigins origins = (CellDamageOrigins) originsField.get(restored);
        ProbeBoss boss = reactionBoss();
        int seed = 34211;
        Random.pushGenerator(seed);
        try {
            Burning.affectAttributed(boss, origins, CELL);
        } finally {
            Random.popGenerator();
        }
        assertEquals(Math.round(reaction(seed) * 3f), boss.received);
        assertEquals(1, boss.calls);
        assertTrue(boss.source instanceof Burning);
    }

    @Test public void tenguShockerFieldKeepsBossCreatorThroughSaveAndLegacyStaysUnknown() {
        rules(500, 25, 300);
        Tengu.ShockerAbility.ShockerBlob field = new Tengu.ShockerAbility.ShockerBlob();
        try (OffensiveEffectContext ignored = OffensiveEffectContext.enter(OffensiveDamageOrigin.BOSS)) {
            field.seed(Dungeon.level, CELL, 1);
        }
        Bundle saved = new Bundle();
        field.storeInBundle(saved);
        assertTrue(saved.contains("apd_tengu_shocker_origins_version"));
        Tengu.ShockerAbility.ShockerBlob restored = new Tengu.ShockerAbility.ShockerBlob();
        restored.restoreFromBundle(saved);
        Receiver receiver = new Receiver();
        receiver.pos = CELL;
        Actor.add(receiver);
        int base = 2 + Dungeon.scalingDepth();
        restored.act();
        assertEquals(Math.round(base * 3f), receiver.received);
        assertEquals(1, receiver.calls);
        assertTrue(receiver.source instanceof Electricity);

        CellDamageOrigins.clearMetadata(saved, "apd_tengu_shocker_origins");
        assertFalse(saved.contains("apd_tengu_shocker_origins_version"));
        Tengu.ShockerAbility.ShockerBlob legacyRestored = new Tengu.ShockerAbility.ShockerBlob();
        legacyRestored.restoreFromBundle(saved);
        legacyRestored.act();
        assertEquals(base, receiver.received);
        assertEquals(2, receiver.calls);
    }

    @Test public void everyBossPhaseDirectDamageHasOneExplicitBoundary() throws Exception {
        Path root = Paths.get(System.getProperty("apd.game.src"));
        String tengu = Files.readString(root.resolve("actors/mobs/Tengu.java"));
        String dm300 = Files.readString(root.resolve("actors/mobs/DM300.java"));
        String king = Files.readString(root.resolve("actors/mobs/DwarfKing.java"));
        String yog = Files.readString(root.resolve("actors/mobs/YogDzewa.java"));

        int bombRoll = tengu.indexOf("int dmg = Random.NormalIntRange(5 + Dungeon.scalingDepth()");
        int bombScale = tengu.indexOf("dmg = Math.round(CustomOffensiveHooks.attackDamage(target, dmg));", bombRoll);
        int bombArmor = tengu.indexOf("dmg -= ch.apdDrRoll();", bombScale);
        assertTrue(bombRoll >= 0 && bombRoll < bombScale && bombScale < bombArmor);
        assertTrue(tengu.contains("Burning.affectAttributed(ch, apdOrigins, cell);"));
        assertTrue(tengu.contains("ch.damage(apdOrigins.scale(2 + Dungeon.scalingDepth(), cell), new Electricity());"));
        assertFalse(tengu.contains("OffensiveDamageOrigin.BOSS).apply(ch)"));

        assertTrue(dm300.contains("CustomOffensiveHooks.raw(ch, Random.NormalIntRange(10, 20), this, target);"));
        assertTrue(dm300.contains("CustomOffensiveHooks.raw(ch, Random.NormalIntRange(6, 12), this, target);"));
        assertTrue(king.contains("CustomOffensiveHooks.raw(ch, Random.NormalIntRange(20, 40), this, target);"));
        assertTrue(king.contains("target.damage(target.HT/18, new KingDamager());"));
        assertTrue(king.contains("m.damage(damage, this);"));
        assertTrue(yog.contains("CustomOffensiveHooks.raw(ch, Random.NormalIntRange(30, 50), new Eye.DeathGaze(), this);"));
        assertTrue(yog.contains("CustomOffensiveHooks.raw(ch, Random.NormalIntRange(20, 30), new Eye.DeathGaze(), this);"));
    }
}
