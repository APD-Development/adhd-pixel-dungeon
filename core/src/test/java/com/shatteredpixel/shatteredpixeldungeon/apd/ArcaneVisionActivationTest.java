/* ADHD Pixel Dungeon (C) 2026 Chris Charlton, GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight.CharAwareness;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
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

/** Actual wand impact/ward attack handlers and FOV. Texture/particle drawing is omitted. */
public class ArcaneVisionActivationTest {
    private static final class Arena extends Level {
        @Override protected boolean build() { return true; }
        @Override protected void createMobs() {}
        @Override protected void createItems() {}
    }

    public static class QuietEmitter extends Emitter {
        @Override public void burst(Factory factory, int quantity) {}
        @Override public void start(Factory factory, float interval, int quantity) {}
    }

    public static class QuietSprite extends CharSprite {
        int zapRequests, zapCell;
        @Override public void zap(int cell) { zapRequests++; zapCell = cell; }
        @Override public void add(State state) {}
        @Override public void remove(State state) {}
        @Override public void burst(int color, int quantity) {}
        @Override public void bloodBurstA(PointF from, int damage) {}
        @Override public void flash() {}
        @Override public void showStatus(int color, String text, Object... args) {}
        @Override public void showStatusWithIcon(int color, String text, int icon, Object... args) {}
        @Override public Emitter emitter() { return new QuietEmitter(); }
        @Override public Emitter centerEmitter() { return new QuietEmitter(); }
    }

    private static final class QuietEmitters extends Group {
        @Override public synchronized Gizmo recycle(Class<? extends Gizmo> type) {
            assertEquals(Emitter.class, type);
            return new QuietEmitter();
        }
    }

    /** Selects a controlled enemy, but does not override the attack, damage or wand proc. */
    private static final class WardAttackProbe extends WandOfWarding.Ward {
        boolean attackNow(Char target) {
            enemy = target;
            assertTrue(super.canAttack(target));
            return super.doAttack(target);
        }
    }

    private final Map<Field, Object> previousActorState = new LinkedHashMap<>();
    private Hero previousHero, hero;
    private Level previousLevel;
    private int previousDepth, previousBranch, previousChallenges;
    private Bundle previousRules;
    private Field sceneField, userField, selectorField;
    private Object previousScene, previousUser, previousSelector;
    private GameScene particleScene;
    private Arena arena;

    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static <T> T withoutDrawingConstructor(Class<T> type) throws Exception {
        Object unsafe = field(Class.forName("sun.misc.Unsafe"), "theUnsafe").get(null);
        return type.cast(unsafe.getClass().getMethod("allocateInstance", Class.class)
                .invoke(unsafe, type));
    }

    private static QuietSprite quietSprite() throws Exception {
        return withoutDrawingConstructor(QuietSprite.class);
    }

    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        previousHero = Dungeon.hero;
        previousLevel = Dungeon.level;
        previousDepth = Dungeon.depth;
        previousBranch = Dungeon.branch;
        previousChallenges = Dungeon.challenges;
        previousRules = new Bundle();
        Dungeon.apdStoreCustomRules(previousRules);
        Dungeon.apdRestoreCustomRules(new Bundle());
        sceneField = field(GameScene.class, "scene");
        previousScene = sceneField.get(null);
        sceneField.set(null, null);
        userField = field(Item.class, "curUser");
        previousUser = userField.get(null);
        selectorField = field(GameScene.class, "cellSelector");
        previousSelector = selectorField.get(null);
        // Actual resetKeyHold only assigns the three action fields; no tilemap drawing is needed.
        selectorField.set(null, withoutDrawingConstructor(CellSelector.class));
        for (String name : new String[]{"all", "chars", "ids", "current", "now", "nextID"}) {
            Field state = field(Actor.class, name);
            previousActorState.put(state, state.get(null));
        }
        field(Actor.class, "all").set(null, new HashSet<Actor>());
        field(Actor.class, "chars").set(null, new HashSet<Char>());
        field(Actor.class, "ids").set(null, new SparseArray<Actor>());
        field(Actor.class, "current").set(null, null);
        field(Actor.class, "now").setFloat(null, 0f);
        field(Actor.class, "nextID").setInt(null, 1);
        Dungeon.challenges = 0;
        Dungeon.depth = 5;
        Dungeon.branch = 0;
        arena = new Arena();
        arena.setSize(17, 17);
        arena.mobs = new HashSet<>();
        arena.blobs = new HashMap<>();
        arena.heaps = new SparseArray<>();
        arena.traps = new SparseArray<>();
        arena.plants = new SparseArray<>();
        arena.customTiles = new ArrayList<>();
        arena.customWalls = new ArrayList<>();
        Arrays.fill(arena.map, Terrain.EMPTY);
        for (int i = 0; i < 17; i++) {
            arena.map[at(i, 0)] = arena.map[at(i, 16)] = Terrain.WALL;
            arena.map[at(0, i)] = arena.map[at(16, i)] = Terrain.WALL;
        }
        arena.buildFlagMaps();
        arena.cleanWalls();
        Dungeon.level = arena;
        particleScene = new GameScene();
        field(GameScene.class, "emitters").set(particleScene, new QuietEmitters());
        Random.pushGenerator(444);
    }

    @After public void restore() throws Exception {
        sceneField.set(null, null);
        Random.popGenerator();
        for (Map.Entry<Field, Object> entry : previousActorState.entrySet()) {
            entry.getKey().set(null, entry.getValue());
        }
        userField.set(null, previousUser);
        selectorField.set(null, previousSelector);
        Dungeon.hero = previousHero;
        Dungeon.level = previousLevel;
        Dungeon.depth = previousDepth;
        Dungeon.branch = previousBranch;
        Dungeon.challenges = previousChallenges;
        Dungeon.apdRestoreCustomRules(previousRules);
        sceneField.set(null, previousScene);
        if (previousLevel != null) {
            PathFinder.setMapSize(previousLevel.width(), previousLevel.height());
        }
    }

    private int at(int x, int y) { return y * arena.width() + x; }

    private void freshHero(int rank) throws Exception {
        for (Actor actor : Actor.all()) Actor.remove(actor);
        arena.mobs.clear();
        hero = new Hero();
        hero.heroClass = HeroClass.MAGE;
        Talent.initClassTalents(hero);
        hero.talents.get(1).put(Talent.ARCANE_VISION, rank);
        hero.HP = hero.HT = 100;
        hero.pos = at(3, 8);
        hero.viewDistance = 2;
        hero.fieldOfView = arena.heroFOV;
        hero.sprite = quietSprite();
        Dungeon.hero = hero;
        Actor.add(hero);
        userField.set(null, hero);
    }

    private Rat target(Char.Alignment alignment, int cell) throws Exception {
        Rat result = new Rat();
        result.alignment = alignment;
        result.pos = cell;
        result.HP = result.HT = 100;
        result.sprite = quietSprite();
        arena.mobs.add(result);
        Actor.add(result);
        return result;
    }

    private void impact(Wand wand, Char target) throws Exception {
        Ballistica beam = new Ballistica(hero.pos, target.pos, wand.collisionProperties(target.pos));
        assertEquals(target.pos, beam.collisionPos.intValue());
        // Only CellEmitter's particle host is supplied, not a rendered gameplay scene.
        sceneField.set(null, particleScene);
        try { wand.onZap(beam); } finally { sceneField.set(null, null); }
    }

    private boolean[] view() {
        boolean[] result = new boolean[arena.length()];
        arena.updateFieldOfView(hero, result);
        return result;
    }

    private void assertTracking(Char target, boolean expected) {
        int matching = 0;
        for (CharAwareness awareness : hero.buffs(CharAwareness.class)) {
            if (awareness.charID == target.id()) matching++;
        }
        assertEquals("target-specific awareness", expected ? 1 : 0, matching);
        boolean[] visible = view();
        for (int offset : PathFinder.NEIGHBOURS9) {
            assertEquals("target footprint " + (target.pos + offset), expected,
                    visible[target.pos + offset]);
        }
    }

    @Test public void frostImpactRequiresANonFrozenTargetToGrantActualAwareness() throws Exception {
        for (int rank = 0; rank <= 2; rank++) for (boolean frozen : new boolean[]{false, true}) {
            freshHero(rank);
            Rat enemy = target(Char.Alignment.ENEMY, at(4, 8));
            if (frozen) Buff.affect(enemy, Frost.class, 5f);
            int before = enemy.HP;
            impact(new WandOfFrost(), enemy);
            if (frozen) {
                assertEquals(before, enemy.HP);
                assertNotNull(enemy.buff(Frost.class));
                assertNull(enemy.buff(Chill.class));
            } else {
                assertTrue(enemy.HP < before);
                assertNotNull(enemy.buff(Chill.class));
            }
            enemy.pos = at(11, 8);
            assertTracking(enemy, rank > 0 && !frozen);
        }
    }

    @Test public void transfusionHealingTracksAnAllyBeyondOrdinarySight() throws Exception {
        for (int rank = 0; rank <= 2; rank++) {
            freshHero(rank);
            Rat ally = target(Char.Alignment.ALLY, at(4, 8));
            ally.HP = 50;
            int before = hero.HP;
            impact(new WandOfTransfusion(), ally);
            assertTrue("actual ally healing branch", ally.HP > 50);
            assertTrue("actual caster health cost", hero.HP < before);
            assertEquals(Char.Alignment.ALLY, ally.alignment);
            ally.pos = at(11, 8);
            assertTracking(ally, rank > 0);
        }
    }

    @Test public void autonomousWardAttackTracksASurvivorWithoutAnotherHeroZap() throws Exception {
        for (int rank = 0; rank <= 2; rank++) {
            freshHero(rank);
            WardAttackProbe ward = new WardAttackProbe();
            ward.pos = at(6, 8);
            ward.tier = 3; // Survives this real attack; no death/drawing tail is replaced.
            ward.viewDistance = 1;
            ward.sprite = quietSprite();
            arena.mobs.add(ward);
            Actor.add(ward);
            Rat enemy = target(Char.Alignment.ENEMY, at(11, 8));
            assertTracking(enemy, false); // Includes actual shared ward FOV, not only hero sight.
            float heroTime = hero.cooldown();
            int before = enemy.HP;
            // The real attack requests an animation. Complete its real callback without drawing it.
            assertFalse(ward.attackNow(enemy));
            QuietSprite sprite = (QuietSprite) ward.sprite;
            assertEquals(1, sprite.zapRequests);
            assertEquals(enemy.pos, sprite.zapCell);
            assertEquals(0, ward.totalZaps);
            ward.onZapComplete();
            assertEquals(1, ward.totalZaps);
            assertTrue(enemy.HP > 0 && enemy.HP < before);
            assertEquals(heroTime, hero.cooldown(), 0f);
            assertTracking(enemy, rank > 0);
        }
    }
}
