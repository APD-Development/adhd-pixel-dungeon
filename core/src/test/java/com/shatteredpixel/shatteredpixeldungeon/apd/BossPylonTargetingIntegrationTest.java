/* ADHD Pixel Dungeon (C) 2026 Chris Charlton, GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.InventoryPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.noosa.NinePatch;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** Real target refresh and range/visibility filters; only HUD drawing is omitted. */
public class BossPylonTargetingIntegrationTest {
    private static final class Arena extends Level {
        @Override protected boolean build() { return true; }
        @Override protected void createMobs() {}
        @Override protected void createItems() {}
    }

    /** The indicator creates the target's sprite, but this test needs no texture. */
    public static class QuietSprite extends CharSprite {
        @Override public void idle() {}
        @Override public void linkVisuals(Char target) {}
    }

    /** Keep the real AttackIndicator constructor, private refresh and target state. */
    private static final class HeadlessIndicator extends AttackIndicator {
        @Override protected void createChildren() {
            try {
                bg = allocate(NinePatch.class);
            } catch (Exception error) {
                throw new AssertionError(error);
            }
        }
        @Override protected void layout() {}
    }

    private Hero previousHero;
    private Level previousLevel;
    private int previousChallenges;
    private Char previousQuickTarget, previousInventoryTarget;
    private TargetHealthIndicator previousHealthIndicator;
    private Object previousAttackIndicator, previousSelector;
    private final Map<Field, Object> previousActors = new LinkedHashMap<>();
    private Field indicatorInstance, indicatorTarget, cellSelector;
    private Arena arena;
    private Hero hero;
    private DM300 boss;
    private Pylon pylon;
    private HeadlessIndicator indicator;

    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Object unsafe = field(unsafeClass, "theUnsafe").get(null);
        return type.cast(unsafeClass.getMethod("allocateInstance", Class.class)
                .invoke(unsafe, type));
    }

    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        Random.pushGenerator(440202611L);
        previousHero = Dungeon.hero;
        previousLevel = Dungeon.level;
        previousChallenges = Dungeon.challenges;
        previousQuickTarget = QuickSlotButton.lastTarget;
        previousInventoryTarget = InventoryPane.lastTarget;
        previousHealthIndicator = TargetHealthIndicator.instance;
        indicatorInstance = field(AttackIndicator.class, "instance");
        indicatorTarget = field(AttackIndicator.class, "lastTarget");
        cellSelector = field(GameScene.class, "cellSelector");
        previousAttackIndicator = indicatorInstance.get(null);
        previousSelector = cellSelector.get(null);
        for (String name : new String[]{"all", "chars", "ids", "current", "now", "nextID"}) {
            Field state = field(Actor.class, name);
            previousActors.put(state, state.get(null));
        }
        field(Actor.class, "all").set(null, new HashSet<Actor>());
        field(Actor.class, "chars").set(null, new HashSet<Char>());
        field(Actor.class, "ids").set(null, new SparseArray<Actor>());
        field(Actor.class, "current").set(null, null);
        field(Actor.class, "now").setFloat(null, 0f);
        field(Actor.class, "nextID").setInt(null, 1);
        cellSelector.set(null, allocate(CellSelector.class));
        TargetHealthIndicator.instance = allocate(TargetHealthIndicator.class);
        Dungeon.challenges = 0;

        arena = new Arena();
        arena.setSize(15, 15);
        arena.mobs = new HashSet<>();
        arena.blobs = new HashMap<>();
        arena.traps = new SparseArray<>();
        arena.heaps = new SparseArray<>();
        arena.plants = new SparseArray<>();
        arena.customTiles = new ArrayList<>();
        arena.customWalls = new ArrayList<>();
        Arrays.fill(arena.map, Terrain.EMPTY);
        for (int i = 0; i < 15; i++) {
            arena.map[at(i, 0)] = arena.map[at(i, 14)] = Terrain.WALL;
            arena.map[at(0, i)] = arena.map[at(14, i)] = Terrain.WALL;
        }
        arena.buildFlagMaps();
        Dungeon.level = arena;
        hero = new Hero();
        hero.heroClass = HeroClass.WARRIOR;
        hero.pos = at(7, 7);
        hero.HP = hero.HT = 20;
        hero.ready = true;
        hero.fieldOfView = arena.heroFOV;
        Dungeon.hero = hero;
        boss = add(new DM300(), at(8, 7));
        boss.supercharged = true;
        pylon = add(new Pylon(), at(7, 3));
        pylon.alignment = Char.Alignment.ENEMY;
        indicator = new HeadlessIndicator();
        select(boss);
    }

    @After public void restore() throws Exception {
        for (Map.Entry<Field, Object> entry : previousActors.entrySet()) {
            entry.getKey().set(null, entry.getValue());
        }
        Dungeon.hero = previousHero;
        Dungeon.level = previousLevel;
        Dungeon.challenges = previousChallenges;
        QuickSlotButton.lastTarget = previousQuickTarget;
        InventoryPane.lastTarget = previousInventoryTarget;
        TargetHealthIndicator.instance = previousHealthIndicator;
        if (indicatorInstance != null) indicatorInstance.set(null, previousAttackIndicator);
        if (cellSelector != null) cellSelector.set(null, previousSelector);
        if (previousLevel != null) {
            PathFinder.setMapSize(previousLevel.width(), previousLevel.height());
        }
        Random.popGenerator();
    }

    private int at(int x, int y) { return y * arena.width() + x; }

    private <T extends Mob> T add(T mob, int position) {
        mob.pos = position;
        mob.spriteClass = QuietSprite.class;
        arena.mobs.add(mob);
        arena.heroFOV[position] = true;
        Actor.add(mob);
        return mob;
    }

    private void positionPylon(int position) {
        arena.heroFOV[pylon.pos] = false;
        pylon.pos = position;
        arena.heroFOV[position] = true;
    }

    private void select(Mob target) {
        AttackIndicator.target(target);
        assertSame(target, QuickSlotButton.lastTarget);
    }

    private void refresh() {
        hero.checkVisibleMobs();
        AttackIndicator.updateState();
    }

    private Mob meleeTarget() throws Exception {
        return (Mob) indicatorTarget.get(indicator);
    }

    @Test public void aimablePylonOutsideMeleeReachOnlyReplacesRangedTarget() throws Exception {
        assertTrue(hero.fieldOfView[pylon.pos]);
        assertEquals(pylon.pos, QuickSlotButton.autoAim(pylon));
        assertFalse(hero.canAttack(pylon));
        assertTrue(hero.canAttack(boss));
        refresh();
        assertSame(pylon, QuickSlotButton.lastTarget);
        assertSame(pylon, TargetHealthIndicator.instance.target());
        assertSame(pylon, InventoryPane.lastTarget);
        assertSame(boss, meleeTarget());
    }

    @Test public void reachableActivePylonReplacesInvulnerableBossInBothCallSites() throws Exception {
        positionPylon(at(7, 6));
        assertTrue(hero.canAttack(pylon));
        refresh();
        assertSame(pylon, QuickSlotButton.lastTarget);
        assertSame(pylon, meleeTarget());
    }

    @Test public void hiddenAndInactivePylonsAreExcludedBeforeEitherPriority() throws Exception {
        positionPylon(at(7, 6));
        assertTrue("range alone would permit this pylon", hero.canAttack(pylon));
        for (boolean hidden : new boolean[]{true, false}) {
            pylon.alignment = hidden ? Char.Alignment.ENEMY : Char.Alignment.NEUTRAL;
            hero.fieldOfView[pylon.pos] = !hidden;
            select(boss);
            refresh();
            assertFalse(hero.getVisibleEnemies().contains(pylon));
            assertSame(boss, QuickSlotButton.lastTarget);
            assertSame(boss, meleeTarget());
        }
    }

    @Test public void visibleButBlockedPylonDoesNotReplaceBossIncludingMindVisionPath() throws Exception {
        for (int x = 1; x < 14; x++) arena.map[at(x, 5)] = Terrain.WALL;
        arena.buildFlagMaps();
        hero.fieldOfView[pylon.pos] = true;
        assertEquals(-1, QuickSlotButton.autoAim(pylon));
        assertFalse(hero.canAttack(pylon));
        for (boolean mindVision : new boolean[]{false, true}) {
            hero.mindVisionEnemies.clear();
            if (mindVision) hero.mindVisionEnemies.add(pylon);
            select(boss);
            refresh();
            assertTrue("visible enemy is still reported", hero.getVisibleEnemies().contains(pylon));
            assertSame(boss, QuickSlotButton.lastTarget);
            assertSame(boss, meleeTarget());
        }
    }

    @Test public void validOtherManualTargetIsPreservedDespiteReachableActivePylon() throws Exception {
        positionPylon(at(7, 6));
        Rat manual = add(new Rat(), at(6, 7));
        assertTrue(hero.canAttack(pylon));
        assertTrue(hero.canAttack(manual));
        select(manual);
        refresh();
        assertSame(manual, QuickSlotButton.lastTarget);
        assertSame(manual, meleeTarget());
    }

    @Test public void ordinaryVulnerableBossRemainsSelectedAtBothCallSites() throws Exception {
        positionPylon(at(7, 6));
        boss.supercharged = false;
        assertTrue(hero.canAttack(pylon));
        refresh();
        assertSame(boss, QuickSlotButton.lastTarget);
        assertSame(boss, meleeTarget());
    }
}
