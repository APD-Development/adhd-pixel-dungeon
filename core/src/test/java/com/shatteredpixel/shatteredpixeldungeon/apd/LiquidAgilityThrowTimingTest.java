/* ADHD Pixel Dungeon (C) 2026 Chris Charlton, GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Speed;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.InventoryPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Scene;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Real cast callbacks, Potion.onThrow/drink, Actor.process and Hero.defenseSkill. */
public class LiquidAgilityThrowTimingTest {
    private static final class Arena extends Level {
        @Override protected boolean build() { return true; }
        @Override protected void createMobs() {}
        @Override protected void createItems() {}
    }

    private static final class QuietGame extends Game {
        QuietGame() { super(Scene.class, Game.platform); requestedReset = false; }
    }

    private static final class ActionHero extends Hero {
        Runnable afterActionCost;
        @Override public void spend(float time) {
            super.spend(time);
            if (afterActionCost != null) {
                Runnable once = afterActionCost; afterActionCost = null; once.run();
            }
        }
    }

    /** Texture construction and animation only are omitted. */
    public static class QuietSprite extends CharSprite {
        @Override public void zap(int cell) {}
        @Override public void operate(int cell) {}
        @Override public void add(State state) {}
        @Override public void remove(State state) {}
        @Override public void showStatusWithIcon(int color, String text, int icon, Object... args) {}
        @Override public void showStatus(int color, String text, Object... args) {}
    }

    /** Captures the production callback without replacing its body or potion effects. */
    public static class CapturedMissile extends MissileSprite {
        Callback completion;
        boolean creatureTarget;
        @Override public void reset(Visual from, Visual to, Item item, Callback listener) {
            assertNull(completion); creatureTarget = true; completion = listener;
        }
        @Override public void reset(Visual from, int to, Item item, Callback listener) {
            assertNull(completion); creatureTarget = false; completion = listener;
        }
    }

    private static final class MissileGroup extends Group {
        CapturedMissile missile;
        @Override public synchronized Gizmo recycle(Class<? extends Gizmo> type) {
            assertEquals(MissileSprite.class, type);
            return missile;
        }
    }

    /** Uses the ordinary creature scheduling priority and the real hero defense lookup. */
    private static final class DefenseObserver extends Char {
        boolean reached, evasionPresent;
        int defense;
        float observedTime;
        DefenseObserver() {
            actPriority = MOB_PRIO;
            alignment = Alignment.ENEMY;
            HP = HT = 100;
        }
        void makeImmovable() { properties.add(Property.IMMOVABLE); }
        @Override protected boolean act() {
            observedTime = Actor.now();
            evasionPresent = Dungeon.hero.buff(Talent.LiquidAgilEVATracker.class) != null;
            defense = Dungeon.hero.defenseSkill(this);
            reached = true;
            Actor.keepActorThreadAlive = false;
            return true;
        }
    }

    private final Map<Field, Object> savedStatics = new LinkedHashMap<>();
    private Hero oldHero;
    private Level oldLevel;
    private int oldDepth, oldBranch, oldChallenges;
    private Game oldGame;
    private boolean oldKeepAlive, oldItemRefresh;
    private Object oldPotionHandler;
    private Char oldQuickTarget, oldInventoryTarget;
    private TargetHealthIndicator oldHealthIndicator;
    private ActionHero hero;
    private Arena arena;
    private DefenseObserver observer;
    private CapturedMissile missile;
    private int stackSize;
    private float expectedActionCost;
    private boolean observeEnemy, allowExistingEvasion;

    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name); result.setAccessible(true); return result;
    }
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Object unsafe = field(unsafeClass, "theUnsafe").get(null);
        return type.cast(unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, type));
    }
    private void preserve(Class<?> type, String name) throws Exception {
        Field f = field(type, name); savedStatics.put(f, f.get(null));
    }

    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        oldHero = Dungeon.hero; oldLevel = Dungeon.level;
        oldDepth = Dungeon.depth; oldBranch = Dungeon.branch; oldChallenges = Dungeon.challenges;
        oldGame = Game.instance; oldKeepAlive = Actor.keepActorThreadAlive;
        oldPotionHandler = Potion.apdHandler(); oldItemRefresh = GameScene.updateItemDisplays;
        oldQuickTarget = QuickSlotButton.lastTarget; oldInventoryTarget = InventoryPane.lastTarget;
        oldHealthIndicator = TargetHealthIndicator.instance;
        for (String name : new String[]{"all", "chars", "ids", "current", "now", "nextID"}) preserve(Actor.class, name);
        preserve(Game.class, "sceneClass"); preserve(GameScene.class, "scene");
        preserve(Item.class, "curUser"); preserve(Item.class, "curItem");
        preserve(Badges.class, "global"); Badges.loadGlobal();
        preserve(Emitter.class, "freezeEmitters");
        field(Actor.class, "all").set(null, new HashSet<Actor>());
        field(Actor.class, "chars").set(null, new HashSet<Char>());
        field(Actor.class, "ids").set(null, new SparseArray<Actor>());
        field(GameScene.class, "scene").set(null, null);
        TargetHealthIndicator.instance = allocate(TargetHealthIndicator.class);
        Dungeon.depth = 1; Dungeon.branch = 0; Dungeon.challenges = 0;
        new QuietGame();
        freshWorld(1);
    }

    @After public void restore() throws Exception {
        for (Map.Entry<Field, Object> entry : savedStatics.entrySet()) entry.getKey().set(null, entry.getValue());
        Actor.keepActorThreadAlive = oldKeepAlive; Game.instance = oldGame;
        Dungeon.hero = oldHero; Dungeon.level = oldLevel;
        Dungeon.depth = oldDepth; Dungeon.branch = oldBranch; Dungeon.challenges = oldChallenges;
        Potion.apdHandler(oldPotionHandler); GameScene.updateItemDisplays = oldItemRefresh;
        QuickSlotButton.lastTarget = oldQuickTarget; InventoryPane.lastTarget = oldInventoryTarget;
        TargetHealthIndicator.instance = oldHealthIndicator;
        if (oldLevel != null) PathFinder.setMapSize(oldLevel.width(), oldLevel.height());
    }

    private int at(int x, int y) { return y * arena.width() + x; }

    private void freshWorld(int rank) throws Exception {
        Actor.clear(); field(Actor.class, "current").set(null, null); Actor.resetNextID();
        Emitter.freezeEmitters = false;
        stackSize = 2; expectedActionCost = 1f; observeEnemy = true; allowExistingEvasion = false;
        arena = new Arena(); arena.setSize(15, 15);
        arena.mobs = new HashSet<>(); arena.blobs = new HashMap<>();
        arena.traps = new SparseArray<>(); arena.heaps = new SparseArray<>(); arena.plants = new SparseArray<>();
        arena.customTiles = new ArrayList<>(); arena.customWalls = new ArrayList<>();
        Arrays.fill(arena.map, Terrain.EMPTY);
        for (int i = 0; i < 15; i++) {
            arena.map[at(i, 0)] = arena.map[at(i, 14)] = Terrain.WALL;
            arena.map[at(0, i)] = arena.map[at(14, i)] = Terrain.WALL;
        }
        arena.buildFlagMaps(); arena.cleanWalls(); Dungeon.level = arena;
        hero = new ActionHero(); hero.heroClass = HeroClass.DUELIST; Talent.initClassTalents(hero);
        hero.talents.get(1).put(Talent.LIQUID_AGILITY, rank);
        // A separately learned potion talent checks that the normal shared hook still executes.
        hero.talents.get(1).put(Talent.LIQUID_WILLPOWER, 1);
        hero.HP = hero.HT = 100; hero.pos = at(3, 7); hero.fieldOfView = arena.heroFOV;
        hero.sprite = allocate(QuietSprite.class); hero.sprite.visible = false; hero.sprite.alive = true;
        MissileGroup group = new MissileGroup(); missile = allocate(CapturedMissile.class);
        group.missile = missile; hero.sprite.parent = group;
        Dungeon.hero = hero; Actor.add(hero);
        // Leave the horizontal empty-cell throw ray unobstructed.
        observer = new DefenseObserver(); observer.pos = at(4, 6);
        observer.sprite = allocate(QuietSprite.class); observer.sprite.visible = false; observer.sprite.alive = true;
        Actor.add(observer);
        Potion.initColors();
    }

    private void finishCreatureTurn() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread worker = new Thread(Actor::process, "liquid-agility-real-scheduler");
        worker.setDaemon(true); worker.setUncaughtExceptionHandler((thread, problem) -> error.set(problem));
        Actor.keepActorThreadAlive = true; worker.start(); worker.join(5000);
        if (worker.isAlive()) {
            Actor.keepActorThreadAlive = false; worker.interrupt();
            synchronized (worker) { worker.notifyAll(); }
            worker.join(1000);
        }
        assertFalse("Real scheduler must terminate", worker.isAlive());
        assertNull("Real scheduler exception", error.get());
        assertTrue("The creature's ordinary-priority turn was reached", observer.reached);
        assertEquals(0f, observer.observedTime, 0f);
    }

    private int shield() { Barrier b = hero.buff(Barrier.class); return b == null ? 0 : b.shielding(); }
    private int uses() { Talent.LiquidAgilACCTracker b = hero.buff(Talent.LiquidAgilACCTracker.class); return b == null ? 0 : b.uses; }
    private float evasionTime() { Buff b = hero.buff(Talent.LiquidAgilEVATracker.class); return b == null ? -1 : b.cooldown(); }
    private int expectedDefense(int rank) { return rank == 2 ? Char.INFINITE_EVASION : 15; }

    private void log(String kind, int rank, int seed, float evasionBefore, int usesBefore,
                     int shieldBefore, boolean proc, boolean known, float actionTime) {
        System.out.printf(Locale.ROOT,
                "LIQUID_AGILITY_OBSERVATION {\"kind\":\"%s\",\"rank\":%d,\"seed\":%d,\"action_time\":%.1f,\"evasion_before_scheduler\":%.1f,\"accuracy_uses\":%d,\"barrier_before_scheduler\":%d,\"proc\":%s,\"identified\":%s,\"enemy_turn_time\":%.1f,\"evasion_present_at_enemy_turn\":%s,\"defense_at_enemy_turn\":%d}%n",
                kind, rank, seed, actionTime, evasionBefore, usesBefore, shieldBefore,
                proc, known, observer.observedTime, observer.evasionPresent, observer.defense);
    }

    /** The returned value is the actual potion-talent proc, not a copied chance formula. */
    private boolean throwPotion(Potion potion, boolean occupied, int rank, int seed, String kind) throws Exception {
        int target = occupied ? observer.pos : at(9, 7);
        potion.quantity(stackSize); hero.belongings.backpack.items.add(potion); potion.setCurrent(hero);
        potion.setKnown(); assertTrue(potion.isKnown());
        assertEquals(occupied ? observer : null, Actor.findChar(target));
        Fire fire = new Fire(); fire.seed(arena, target, 7); arena.blobs.put(Fire.class, fire);
        float before = hero.cooldown();
        Random.pushGenerator(seed);
        try {
            potion.cast(hero, target);
            assertNotNull("Item.cast must supply the real animation callback", missile.completion);
            assertEquals(occupied, missile.creatureTarget);
            assertEquals(stackSize, potion.quantity());
            assertEquals(before, hero.cooldown(), 0f);
            Callback actual = missile.completion; missile.completion = null; actual.call();
        } finally { Random.popGenerator(); }
        assertEquals("Existing action cost is preserved", before + expectedActionCost, hero.cooldown(), 0f);
        assertEquals("Exactly one item is detached by the real callback", Math.max(1, stackSize - 1), potion.quantity());
        assertEquals(stackSize > 1, hero.belongings.backpack.items.contains(potion));
        assertTrue("Existing identification is retained", potion.isKnown());
        assertEquals("The real splash clears the target's fire", 0, fire.cur[target]);
        assertEquals(100, hero.HP); assertEquals(100, observer.HP);
        if (potion instanceof AquaBrew) assertEquals("The actual Aqua/Geyser effect makes water", Terrain.WATER, arena.map[target]);
        float eva = evasionTime(); int accuracy = uses(), shielding = shield(); boolean proc = accuracy > 0;
        if (proc) {
            assertEquals(1, accuracy); assertEquals(7, shielding);
            assertEquals(5f, hero.buff(Talent.LiquidAgilACCTracker.class).cooldown(), 0f);
        } else {
            if (!allowExistingEvasion) assertEquals(-1f, eva, 0f);
            assertEquals(0, shielding);
        }
        if (observeEnemy) {
            finishCreatureTurn();
            log(kind, rank, seed, eva, accuracy, shielding, proc, potion.isKnown(), hero.cooldown());
        }
        return proc;
    }

    @Test public void occupiedPotionThrowRetainsRankOneEvasionForTheEnemyTurn() throws Exception {
        assertTrue(throwPotion(new PotionOfPurity(), true, 1, 44201, "occupied-purity"));
        assertEquals("Rank 1 potion throw should protect the following enemy turn", expectedDefense(1), observer.defense);
    }

    @Test public void occupiedPotionThrowRetainsRankTwoEvasionForTheEnemyTurn() throws Exception {
        freshWorld(2);
        assertTrue(throwPotion(new PotionOfPurity(), true, 2, 44202, "occupied-purity"));
        assertEquals("Rank 2 potion throw should protect the following enemy turn", expectedDefense(2), observer.defense);
    }

    @Test public void emptyCellPotionThrowPreservesBothRankBenefits() throws Exception {
        for (int rank = 1; rank <= 2; rank++) {
            freshWorld(rank);
            assertTrue(throwPotion(new PotionOfPurity(), false, rank, 44210 + rank, "empty-purity"));
            assertTrue(observer.evasionPresent); assertEquals(expectedDefense(rank), observer.defense);
        }
    }

    @Test public void actualDrinksKeepOrdinaryAndStrengthFactorDurationAndUses() throws Exception {
        Method drink = Potion.class.getDeclaredMethod("drink", Hero.class); drink.setAccessible(true);
        for (int rank = 1; rank <= 2; rank++) for (int factor = 1; factor <= 2; factor++) {
            freshWorld(rank);
            Potion potion = factor == 1 ? new PotionOfPurity() : new PotionOfStrength();
            potion.quantity(2); hero.belongings.backpack.items.add(potion); potion.setCurrent(hero);
            assertFalse(potion.isKnown()); int strength = hero.STR;
            drink.invoke(potion, hero);
            assertEquals(1f, hero.cooldown(), 0f); assertEquals(1, potion.quantity());
            assertTrue(hero.belongings.backpack.items.contains(potion)); assertTrue(potion.isKnown());
            assertEquals(strength + (factor == 2 ? 1 : 0), hero.STR);
            assertEquals((float)factor, evasionTime(), 0f); assertEquals(factor, uses());
            assertEquals(factor == 1 ? 7 : 13, shield());
            assertEquals(5f, hero.buff(Talent.LiquidAgilACCTracker.class).cooldown(), 0f);
            float eva = evasionTime(); int accuracy = uses(), shielding = shield();
            finishCreatureTurn();
            log("drink-factor-" + factor, rank, -1, eva, accuracy, shielding, true, potion.isKnown(), hero.cooldown());
            assertTrue(observer.evasionPresent); assertEquals(expectedDefense(rank), observer.defense);
        }
    }

    @Test public void multiOutputAquaBrewKeepsChanceAndRealSplashForProcAndNoProc() throws Exception {
        int triggered = 0, missed = 0;
        for (int seed = 0; seed < 64; seed++) {
            freshWorld(1);
            boolean proc = throwPotion(new AquaBrew(), false, 1, seed, "empty-aqua");
            if (proc) { triggered++; assertEquals(15, observer.defense); assertTrue(observer.evasionPresent); }
            else { missed++; assertEquals(5, observer.defense); assertFalse(observer.evasionPresent); }
        }
        assertTrue("Include successful real multi-output rolls", triggered > 0);
        assertTrue("Include failed real multi-output rolls", missed > 0);
        System.out.println("LIQUID_AGILITY_AQUA_CONTROLS triggered=" + triggered + " missed=" + missed);
    }

    @Test public void excludedStrengthThrowDoesNotAcquireAnyNewPotionTalentBonus() throws Exception {
        int strength = hero.STR;
        assertFalse(throwPotion(new PotionOfStrength(), true, 1, 44230, "excluded-strength-throw"));
        assertEquals(strength, hero.STR); assertEquals(5, observer.defense);
    }

    @Test public void removedTrackerIsNotRecreatedAfterTheActionCost() throws Exception {
        hero.afterActionCost = () -> {
            Talent.LiquidAgilEVATracker applied = hero.buff(Talent.LiquidAgilEVATracker.class);
            assertNotNull(applied); applied.detach();
            assertSame("A detached Buff still retains target, so target alone is insufficient", hero, applied.target);
        };
        assertTrue(throwPotion(new PotionOfPurity(), true, 1, 44240, "removed-tracker"));
        assertNull(hero.buff(Talent.LiquidAgilEVATracker.class)); assertEquals(5, observer.defense);
    }

    @Test public void replacementTrackerKeepsItsIndependentDeadline() throws Exception {
        AtomicReference<Talent.LiquidAgilEVATracker> replacement = new AtomicReference<>();
        hero.afterActionCost = () -> {
            Talent.LiquidAgilEVATracker applied = hero.buff(Talent.LiquidAgilEVATracker.class);
            assertNotNull(applied); applied.detach();
            replacement.set(Buff.prolong(hero, Talent.LiquidAgilEVATracker.class, .25f));
        };
        assertTrue(throwPotion(new PotionOfPurity(), true, 1, 44241, "replaced-tracker"));
        assertSame(replacement.get(), hero.buff(Talent.LiquidAgilEVATracker.class));
        assertEquals(.25f, replacement.get().cooldown(), 0f); assertEquals(15, observer.defense);
    }

    @Test public void successfulStackedThrowKeepsAnOlderLongerDeadline() throws Exception {
        Talent.LiquidAgilEVATracker original = Buff.prolong(hero, Talent.LiquidAgilEVATracker.class, 10f);
        assertTrue(throwPotion(new PotionOfPurity(), true, 1, 44242, "longer-existing-tracker"));
        assertSame(original, hero.buff(Talent.LiquidAgilEVATracker.class));
        assertEquals(10f, original.cooldown(), 0f);
    }

    @Test public void failedOccupiedBrewProcDoesNotExtendAnExistingTracker() throws Exception {
        observer.pos = at(9, 7); observer.makeImmovable();
        Talent.LiquidAgilEVATracker original = Buff.prolong(hero, Talent.LiquidAgilEVATracker.class, .25f);
        allowExistingEvasion = true;
        assertFalse(throwPotion(new AquaBrew(), true, 1, 0, "failed-occupied-aqua"));
        assertSame(original, hero.buff(Talent.LiquidAgilEVATracker.class));
        assertEquals(.25f, original.cooldown(), 0f); assertEquals(15, observer.defense);
    }

    @Test public void completionIsConsumedOnceAndNeverRestoredFromItemState() throws Exception {
        stackSize = 1;
        Potion potion = new PotionOfPurity();
        Method completion = Potion.class.getDeclaredMethod("onThrowComplete", Hero.class);
        completion.setAccessible(true);
        hero.afterActionCost = () -> {
            try {
                assertEquals(0f, evasionTime(), 0f);
                Bundle saved = new Bundle(); potion.storeInBundle(saved);
                Potion restored = new PotionOfPurity(); restored.restoreFromBundle(saved);
                completion.invoke(restored, hero);
                assertEquals("Restored item has no pending throw completion", 0f, evasionTime(), 0f);
            } catch (Exception failure) { throw new AssertionError(failure); }
        };
        assertTrue(throwPotion(potion, true, 1, 44243, "single-item-completion"));
        Talent.LiquidAgilEVATracker applied = hero.buff(Talent.LiquidAgilEVATracker.class);
        assertEquals(1f, applied.cooldown(), 0f);
        hero.spend(5f);
        completion.invoke(potion, hero);
        assertEquals("A second completion cannot refresh the consumed context", 1f, applied.cooldown(), 0f);
    }

    @Test public void realSlowAndSpeedCostsSetTheDeadlineWithoutChangingTheAction() throws Exception {
        for (int mode = 0; mode < 3; mode++) {
            freshWorld(1);
            if (mode != 1) Buff.prolong(hero, Slow.class, 10f);
            if (mode != 0) Buff.prolong(hero, Speed.class, 10f);
            expectedActionCost = mode == 0 ? 2f : mode == 1 ? .5f : 1f;
            assertTrue(throwPotion(new PotionOfPurity(), true, 1, 44250 + mode, "scaled-action-" + mode));
            assertEquals(expectedActionCost, evasionTime(), 0f); assertEquals(15, observer.defense);
        }
    }

    @Test public void freeMomentumAndInterceptedTimeDoNotInventAnActionDelay() throws Exception {
        for (int mode = 0; mode < 3; mode++) {
            freshWorld(1); expectedActionCost = 0f; observeEnemy = false;
            TimekeepersHourglass.timeFreeze freeze = null;
            Swiftthistle.TimeBubble bubble = null;
            if (mode == 0) Buff.affect(hero, Talent.LethalMomentumTracker.class);
            if (mode == 1) {
                freeze = new TimekeepersHourglass().new timeFreeze();
                assertTrue(freeze.attachTo(hero));
            }
            if (mode == 2) { bubble = Buff.affect(hero, Swiftthistle.TimeBubble.class); bubble.reset(); }
            assertTrue(throwPotion(new PotionOfPurity(), true, 1, 44260 + mode, "free-action-" + mode));
            assertEquals(0f, hero.cooldown(), 0f); assertEquals(0f, evasionTime(), 0f);
            if (mode == 0) assertNull(hero.buff(Talent.LethalMomentumTracker.class));
            if (mode == 1) {
                assertSame(freeze, hero.buff(TimekeepersHourglass.timeFreeze.class));
                Bundle state = new Bundle(); freeze.storeInBundle(state);
                assertEquals(1f, state.getFloat("turnsToCost"), 0f);
            }
            if (mode == 2) assertEquals(6f, bubble.remainingActionTime(), 0f);
        }
    }
}
