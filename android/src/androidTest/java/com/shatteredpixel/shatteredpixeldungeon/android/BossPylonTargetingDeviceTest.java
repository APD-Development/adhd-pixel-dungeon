/* ADHD Pixel Dungeon (C) 2026 Chris Charlton, GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;

import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.effects.BadgeBanner;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.InventoryPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.Point;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A synthetic, disposable live GameScene, not a naturally reached DM300 encounter.
 * The actual waiting hero turn remains current. No attack, damage, or turn is run.
 * Selection uses the public target API and the real visibility/indicator refreshes;
 * no sprite, targeting method, actor scheduler, or damage method is replaced.
 */
@RunWith(AndroidJUnit4.class)
public class BossPylonTargetingDeviceTest {
    private interface CheckedRunnable { void run() throws Exception; }

    private static void render(CheckedRunnable task) throws Exception {
        TopScoresDeviceTest.render(() -> {
            try { task.run(); }
            catch (Exception error) { throw new RuntimeException(error); }
        });
    }

    private static Field declared(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static void waitFor(Callable<Boolean> condition) throws Exception {
        long end = SystemClock.uptimeMillis() + 120000;
        while (SystemClock.uptimeMillis() < end) {
            if (condition.call()) return;
            Thread.sleep(100);
        }
        fail("Native pylon fixture timed out");
    }

    private static void waitForClearFrame() throws Exception {
        Field activeFade = declared(Class.forName(PixelScene.class.getName() + "$Fader"), "INSTANCE");
        waitFor(() -> {
            AtomicBoolean ready = new AtomicBoolean();
            render(() -> ready.set(Game.scene() instanceof GameScene
                    && Dungeon.hero != null && Dungeon.hero.ready
                    && !Dungeon.hero.sprite.isMoving && !Game.switchingScene()
                    && activeFade.get(null) == null
                    && TopScoresDeviceTest.find(Game.scene(), BadgeBanner.class).stream()
                        .noneMatch(banner -> banner.exists && banner.visible)));
            return ready.get();
        });
    }

    private static final class Probe {
        final Hero hero = Dungeon.hero;
        final Level level = Dungeon.level;
        final Object scene = Game.scene();
        final AttackIndicator indicator;
        final Field actorCurrent = declared(Actor.class, "current");
        final Field attackTarget = declared(AttackIndicator.class, "lastTarget");
        final Field attackSprite = declared(AttackIndicator.class, "sprite");
        final float actorTime = Actor.now(), heroTime = hero.cooldown();
        final int heroHp = hero.HP;
        final ArrayList<Mob> added = new ArrayList<>();
        final int width = level.width();
        final int anchor = width * (level.height() / 2) + width / 2;
        boolean randomPushed;
        DM300 boss;
        Pylon pylon;
        Rat manual;
        int bossHp, pylonHp, manualHp;
        String stage;
        Mob expectedQuick, expectedMelee;
        boolean expectedPylonVisible, expectedPylonEnemy;

        Probe(AttackIndicator indicator) throws Exception {
            this.indicator = indicator;
            assertHeldHeroTurn();
        }

        void assertHeldHeroTurn() throws Exception {
            assertSame("The fixture keeps the same live scene", scene, Game.scene());
            assertSame(hero, Dungeon.hero);
            assertSame(level, Dungeon.level);
            assertTrue("The real hero is waiting for input", hero.ready);
            assertSame("Hero.ready alone is insufficient: keep the real current actor",
                    hero, actorCurrent.get(null));
            assertNull("No automatic action may resume", hero.curAction);
            assertFalse(hero.resting);
            assertEquals("No world time spent", actorTime, Actor.now(), 0);
            assertEquals("No hero time spent", heroTime, hero.cooldown(), 0);
            assertEquals("No hero damage or healing", heroHp, hero.HP);
        }

        void prepare() throws Exception {
            assertHeldHeroTurn();
            assertEquals(HeroClass.WARRIOR, hero.heroClass);
            assertTrue("The disposable floor has room for the controlled arena",
                    width >= 15 && level.height() >= 15);
            assertTrue("The ranged control requires at least two tiles of vision",
                    hero.viewDistance >= 2);
            Random.pushGenerator(444202611L);
            randomPushed = true;
            // This is a new disposable run; its original floor is not restored or retained.
            // Remove actors without invoking death, rewards, or boss-floor callbacks.
            for (Mob mob : new ArrayList<>(level.mobs)) remove(mob);
            for (int y = -6; y <= 6; y++) {
                for (int x = -6; x <= 6; x++) {
                    int cell = anchor + y * width + x;
                    Heap heap = level.heaps.get(cell);
                    if (heap != null) {
                        if (heap.sprite != null) heap.sprite.killAndErase();
                        level.heaps.remove(cell);
                    }
                    level.plants.remove(cell);
                    Level.set(cell, Terrain.EMPTY);
                    level.discoverable[cell] = true;
                    level.visited[cell] = level.mapped[cell] = false;
                }
            }
            hero.pos = anchor;
            hero.sprite.place(anchor);
            hero.fieldOfView = level.heroFOV;
            GameScene.updateMap();

            boss = new DM300();
            // Existing phase data only. DM300.supercharge() needs a real CavesBossLevel
            // and is deliberately outside this target-selection fixture.
            boss.supercharged = true;
            add(boss, anchor + 1);
            pylon = new Pylon();
            add(pylon, anchor - width);
            manual = new Rat();
            add(manual, anchor - 1);
            bossHp = boss.HP;
            pylonHp = pylon.HP;
            manualHp = manual.HP;
            assertTrue(boss.isSupercharged());
            assertEquals(Char.Alignment.NEUTRAL, pylon.alignment);
            assertHeldHeroTurn();
        }

        private void add(Mob mob, int cell) {
            assertNull(Actor.findChar(cell));
            mob.pos = cell;
            added.add(mob);
            GameScene.add(mob);
            assertTrue(mob.isActive());
            assertNotNull(mob.sprite);
            assertSame(mob, mob.sprite.ch);
        }

        private void remove(Mob mob) {
            Actor.remove(mob);
            level.mobs.remove(mob);
            if (mob.sprite != null) mob.sprite.killAndErase();
        }

        private void movePylon(int cell) {
            assertNull(Actor.findChar(cell));
            pylon.pos = cell;
            pylon.sprite.place(cell);
            GameScene.sortMobSprites();
        }

        private void refresh(String name, Mob selected, Mob quick, Mob melee,
                             boolean visible, boolean enemy) throws Exception {
            assertHeldHeroTurn();
            stage = name;
            expectedQuick = quick;
            expectedMelee = melee;
            expectedPylonVisible = visible;
            expectedPylonEnemy = enemy;
            Dungeon.observe(); // Actual Level.updateFieldOfView and sprite-visibility path.
            Camera.main.snapTo(DungeonTilemap.tileCenterToWorld(anchor - width));
            AttackIndicator.target(selected); // Same public selection path as gameplay.
            assertSame(selected, QuickSlotButton.lastTarget);
            hero.checkVisibleMobs();
            AttackIndicator.updateState();
            GameScene.updateFog();
            assertTargets(false);
        }

        void inactive() throws Exception {
            assertTrue(hero.canAttack(pylon));
            refresh("inactive-visible", boss, boss, boss, true, false);
        }

        void active() throws Exception {
            // Activate only after GameScene.add has linked the real PylonSprite.
            pylon.activate();
            assertTrue(hero.canAttack(pylon));
            refresh("active-visible", boss, pylon, pylon, true, true);
        }

        void manual() throws Exception {
            assertTrue(hero.canAttack(manual));
            assertTrue(hero.canAttack(pylon));
            refresh("manual-rat-preserved", manual, manual, manual, true, true);
        }

        void ranged() throws Exception {
            movePylon(anchor - 2 * width);
            assertFalse(hero.canAttack(pylon));
            assertEquals(pylon.pos, QuickSlotButton.autoAim(pylon));
            refresh("ranged-pylon-melee-boss", boss, pylon, boss, true, true);
        }

        void hidden() throws Exception {
            movePylon(anchor - 4 * width);
            // Real opaque terrain, not a fabricated heroFOV flag, hides this pylon.
            for (int x = -5; x <= 5; x++) Level.set(anchor - 2 * width + x, Terrain.WALL);
            for (int y = -5; y <= -3; y++) {
                for (int x = -1; x <= 1; x++) {
                    int cell = anchor + y * width + x;
                    level.visited[cell] = level.mapped[cell] = false;
                }
            }
            GameScene.updateMap();
            refresh("hidden-pylon-excluded", boss, boss, boss, false, true);
            assertEquals(-1, QuickSlotButton.autoAim(pylon));
        }

        // This was not covered by the original five native captures: an active
        // pylon must not steal selection from DM-300 after its shield ends.
        // No extra capture is emitted, retaining the original native evidence
        // inventory and all five published screenshot names.
        void vulnerableBoss() throws Exception {
            for (int x = -5; x <= 5; x++) {
                Level.set(anchor - 2 * width + x, Terrain.EMPTY);
            }
            movePylon(anchor - width);
            boss.supercharged = false;
            GameScene.updateMap();
            assertFalse(boss.isSupercharged());
            assertEquals(Char.Alignment.ENEMY, pylon.alignment);
            assertTrue(hero.canAttack(pylon));
            refresh("vulnerable-boss-retains-target", boss, boss, boss, true, true);
        }

        void assertTargets(boolean rendered) throws Exception {
            assertHeldHeroTurn();
            assertEquals(bossHp, boss.HP);
            assertEquals(pylonHp, pylon.HP);
            assertEquals(manualHp, manual.HP);
            assertEquals(anchor, hero.pos);
            assertEquals(anchor + 1, boss.pos);
            assertEquals(anchor - 1, manual.pos);
            assertEquals(stage + " real field of view", expectedPylonVisible,
                    hero.fieldOfView[pylon.pos]);
            assertEquals(stage + " actual map sprite", expectedPylonVisible,
                    pylon.sprite.visible);
            assertEquals(expectedPylonEnemy, pylon.alignment == Char.Alignment.ENEMY);
            assertEquals(expectedPylonVisible && expectedPylonEnemy,
                    hero.getVisibleEnemies().contains(pylon));
            assertSame(stage + " ranged target", expectedQuick, QuickSlotButton.lastTarget);
            assertSame(stage + " inventory target", expectedQuick, InventoryPane.lastTarget);
            assertSame(stage + " health target", expectedQuick, TargetHealthIndicator.instance.target());
            assertSame(stage + " melee target", expectedMelee, attackTarget.get(indicator));
            if (rendered) {
                assertTrue("Actual target health bar is rendered", TargetHealthIndicator.instance.visible);
                assertTrue("Actual attack indicator remains visible", indicator.visible);
                CharSprite icon = (CharSprite) attackSprite.get(indicator);
                assertNotNull(icon);
                assertTrue("Actual attack portrait is rendered", icon.visible && icon.exists);
                assertEquals(expectedMelee.spriteClass, icon.getClass());
                Point topLeft = indicator.camera().cameraToScreen(indicator.left(), indicator.top());
                Point bottomRight = indicator.camera().cameraToScreen(indicator.right(), indicator.bottom());
                assertTrue("The whole attack control is inside the captured screen",
                        topLeft.x >= 0 && topLeft.y >= 0
                                && bottomRight.x <= Game.width && bottomRight.y <= Game.height);
                inViewport(hero);
                inViewport(boss);
                inViewport(manual);
                if (expectedPylonVisible) inViewport(pylon);
            }
        }

        private void inViewport(Char target) {
            PointF center = target.sprite.center();
            Point screen = Camera.main.cameraToScreen(center.x, center.y);
            assertTrue("Visible creature is inside the captured viewport: " + target.getClass().getSimpleName(),
                    screen.x > 0 && screen.y > 0 && screen.x < Game.width && screen.y < Game.height);
        }

        void log() throws Exception {
            assertTargets(true);
            System.out.println("NATIVE_PYLON_TARGET stage=" + stage
                    + " actorTime=" + Actor.now() + " heroTime=" + hero.cooldown()
                    + " heroHp=" + hero.HP + " bossHp=" + boss.HP + " pylonHp=" + pylon.HP
                    + " pylonVisible=" + hero.fieldOfView[pylon.pos]
                    + " pylonEnemy=" + (pylon.alignment == Char.Alignment.ENEMY)
                    + " quick=" + QuickSlotButton.lastTarget.getClass().getSimpleName()
                    + " melee=" + attackTarget.get(indicator).getClass().getSimpleName()
                    + " activeActorIsHero=" + (actorCurrent.get(null) == hero));
        }

        void close() {
            try {
                for (Mob mob : added) remove(mob);
                QuickSlotButton.lastTarget = null;
                InventoryPane.lastTarget = null;
                if (TargetHealthIndicator.instance != null) TargetHealthIndicator.instance.target(null);
                hero.checkVisibleMobs();
                AttackIndicator.updateState();
            } finally {
                if (randomPushed) { Random.popGenerator(); randomPushed = false; }
            }
        }
    }

    private static void capture(Probe probe, String name) throws Exception {
        waitForClearFrame();
        Thread.sleep(250); // Render time only; the same hero input turn must remain current.
        render(probe::log);
        TopScoresDeviceTest.screenshot(name);
        render(() -> probe.assertTargets(true));
    }

    @Test public void nativeIndicatorsPreserveManualTargetsAndExcludeHiddenOrInactivePylons()
            throws Exception {
        final AtomicReference<Boolean> intro = new AtomicReference<>();
        final int oldSlot = GamesInProgress.curSlot;
        final HeroClass oldClass = GamesInProgress.selectedClass;
        final boolean oldDaily = Dungeon.daily, oldReplay = Dungeon.dailyReplay;
        final String oldSeedText = Dungeon.customSeedText;
        final int[] slot = {-1};
        final AtomicReference<Probe> probe = new AtomicReference<>();
        try (ActivityScenario<AndroidLauncher> app = ActivityScenario.launch(AndroidLauncher.class)) {
            waitFor(() -> Game.scene() != null && PixelScene.uiCamera != null);
            render(() -> {
                assertNotNull("The launched game initializes Gdx.app", Gdx.app);
                boolean introStored = SPDSettings.contains(SPDSettings.KEY_INTRO);
                intro.set(SPDSettings.intro());
                System.out.println("NATIVE_PYLON_PREFERENCE stage=captured intro=" + intro.get()
                        + " stored=" + introStored);
                ShatteredPixelDungeon.switchNoFade(TitleScene.class);
            });
            waitFor(() -> Game.scene() instanceof TitleScene);
            try {
                render(() -> {
                    SPDSettings.intro(false);
                    slot[0] = GamesInProgress.firstEmpty(HeroClass.WARRIOR);
                    assertTrue("Use an empty slot only", slot[0] > 0);
                    GamesInProgress.curSlot = slot[0];
                    GamesInProgress.selectedClass = HeroClass.WARRIOR;
                    Dungeon.hero = null;
                    Dungeon.daily = Dungeon.dailyReplay = false;
                    Dungeon.customSeedText = "";
                    Dungeon.initSeed();
                    InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
                    Game.switchScene(InterlevelScene.class);
                });
                waitForClearFrame();
                render(() -> {
                    assertEquals(1, Dungeon.depth);
                    AttackIndicator indicator = TopScoresDeviceTest.find(Game.scene(), AttackIndicator.class).get(0);
                    probe.set(new Probe(indicator));
                    probe.get().prepare();
                    probe.get().inactive();
                });
                capture(probe.get(), "pylon-native-inactive-visible.png");
                render(() -> probe.get().active());
                capture(probe.get(), "pylon-native-active-visible.png");
                render(() -> probe.get().manual());
                capture(probe.get(), "pylon-native-manual-rat-preserved.png");
                render(() -> probe.get().ranged());
                capture(probe.get(), "pylon-native-ranged-pylon-melee-boss.png");
                render(() -> probe.get().hidden());
                capture(probe.get(), "pylon-native-hidden-excluded.png");
                // Assertion-only sixth state, preserving the five existing PNG receipts.
                render(() -> probe.get().vulnerableBoss());
            } finally {
                render(() -> {
                    try { if (probe.get() != null) probe.get().close(); }
                    finally { ShatteredPixelDungeon.switchNoFade(TitleScene.class); }
                });
                waitFor(() -> Game.scene() instanceof TitleScene);
            }
        } finally {
            try {
                if (intro.get() != null) {
                    SPDSettings.intro(intro.get());
                    assertEquals("Restore the actually captured intro preference",
                            intro.get().booleanValue(), SPDSettings.intro());
                    System.out.println("NATIVE_PYLON_PREFERENCE stage=restored intro=" + intro.get());
                }
            } finally {
                if (slot[0] > 0) Dungeon.deleteGame(slot[0], true);
                GamesInProgress.curSlot = oldSlot;
                GamesInProgress.selectedClass = oldClass;
                Dungeon.daily = oldDaily;
                Dungeon.dailyReplay = oldReplay;
                Dungeon.customSeedText = oldSeedText;
            }
        }
    }
}
