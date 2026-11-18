package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomRuleMultipliers;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomScoreSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Early-alpha roster gate. Uses one disposable slot to prove that every shipped
 * class can enter a real Android run, expose usable controls, save, and continue
 * with its exact locked custom-rule identity.
 */
@RunWith(AndroidJUnit4.class)
public class EarlyAlphaRosterDeviceTest {

    private static final int SLOT = 1;

    private static void onRender(Runnable task) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Game.runOnRenderThread(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        assertTrue("render thread did not answer", done.await(60, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError("render-thread failure", failure.get());
        }
    }

    private static void waitUntil(String description, Callable<Boolean> condition, long timeoutMillis) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.call()) return;
            Thread.sleep(150);
        }
        throw new AssertionError("timed out waiting for " + description);
    }

    private static Object field(Object target, String name) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                // Continue through the scene/component hierarchy.
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static CustomDungeonRules acceptanceRules() {
        return CustomDungeonRules.defaults()
                .with(CustomDungeonRules.Slider.ENEMY_HP, 500)
                .with(CustomDungeonRules.Slider.ENEMY_DAMAGE, 500)
                .with(CustomDungeonRules.Slider.ENEMY_DEFENSE, 500)
                .with(CustomDungeonRules.Slider.ENEMY_ACCURACY, 25)
                .with(CustomDungeonRules.Slider.ENEMY_EVASION, 0)
                .with(CustomDungeonRules.Slider.ENEMY_SPAWN, 300)
                .with(CustomDungeonRules.Slider.BOSS_HP, 500)
                .with(CustomDungeonRules.Slider.BOSS_DAMAGE, 25)
                .with(CustomDungeonRules.Slider.PLAYER_HP, 500)
                .with(CustomDungeonRules.Slider.PLAYER_DAMAGE, 25)
                .with(CustomDungeonRules.Slider.PLAYER_ACCURACY, 25)
                .with(CustomDungeonRules.Slider.PLAYER_EVASION, 0)
                .with(CustomDungeonRules.Slider.PLAYER_DEFENSE, 500)
                .with(CustomDungeonRules.Slider.FOOD, 25)
                .with(CustomDungeonRules.Slider.HUNGER, 400)
                .with(CustomDungeonRules.Slider.HEALING, 25)
                .with(CustomDungeonRules.Slider.EQUIPMENT, 25)
                .with(CustomDungeonRules.Slider.UPGRADE_SCROLLS, 1);
    }

    private static boolean controlsUsable() throws Exception {
        AtomicBoolean usable = new AtomicBoolean();
        onRender(() -> {
            try {
                if (!(Game.scene() instanceof GameScene) || Dungeon.hero == null) return;
                com.watabou.noosa.ui.Component toolbar =
                        (com.watabou.noosa.ui.Component) field(Game.scene(), "toolbar");
                boolean enabled = Dungeon.hero.ready && Dungeon.hero.isAlive()
                        && toolbar.visible && toolbar.active;
                for (String name : new String[]{"btnSearch", "btnWait", "btnInventory"}) {
                    com.watabou.noosa.ui.Component button =
                            (com.watabou.noosa.ui.Component) field(toolbar, name);
                    enabled &= button.visible && button.active;
                }
                Object[] quickSlots = (Object[]) field(toolbar, "btnQuick");
                int visibleQuickSlots = 0;
                for (Object slot : quickSlots) {
                    if (((com.watabou.noosa.ui.Component) slot).visible) visibleQuickSlots++;
                }
                usable.set(enabled && visibleQuickSlots > 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return usable.get();
    }

    private static void deleteDisposableSlot() {
        GamesInProgress.delete(SLOT);
        FileUtils.deleteDir(GamesInProgress.gameFolder(SLOT));
    }

    @Test
    public void allSixClassesStartSaveAndContinueWithLockedExtremeRules() throws Exception {
        try (ActivityScenario<AndroidLauncher> ignored = ActivityScenario.launch(AndroidLauncher.class)) {
            waitUntil("Android game startup", () -> Gdx.app != null && Game.scene() != null, 120000);
            CustomDungeonRules previousRules = SPDSettings.apdCustomRules();
            int previousDifficulty = SPDSettings.apdDifficulty();
            int previousChallenges = SPDSettings.challenges();
            boolean previousIntro = SPDSettings.intro();
            boolean previousRecording = SPDSettings.apdRecording();
            int previousSlot = GamesInProgress.curSlot;
            HeroClass previousSelected = GamesInProgress.selectedClass;
            try {
                CustomDungeonRules expected = acceptanceRules();
                String expectedMultiplier = CustomRuleMultipliers.combined(expected).toPlainString();
            int completed = 0;

            for (HeroClass heroClass : HeroClass.values()) {
                onRender(() -> {
                    deleteDisposableSlot();
                    SPDSettings.intro(false);
                    SPDSettings.apdRecording(false);
                    SPDSettings.apdCustomRules(expected);
                    SPDSettings.apdDifficulty(5);
                    SPDSettings.challenges(511);
                    GamesInProgress.curSlot = SLOT;
                    GamesInProgress.selectedClass = heroClass;
                    Dungeon.hero = null;
                    Dungeon.daily = false;
                    Dungeon.dailyReplay = false;
                    Dungeon.initSeed();
                    InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
                    Game.switchScene(InterlevelScene.class);
                });

                waitUntil(heroClass + " first floor",
                        () -> Game.scene() instanceof GameScene && Dungeon.hero != null && Dungeon.hero.ready,
                        150000);
                waitUntil(heroClass + " gameplay controls", EarlyAlphaRosterDeviceTest::controlsUsable, 30000);

                final int[] savedHealth = new int[1];
                onRender(() -> {
                    assertEquals(heroClass, Dungeon.hero.heroClass);
                    assertArrayEquals(expected.values(), Dungeon.customRules().values());
                    assertEquals(5, Dungeon.difficulty);
                    assertEquals(511, Dungeon.challenges);
                    assertNotNull(Dungeon.customScoring());
                    assertEquals(CustomScoreSnapshot.VERSION, Dungeon.customScoring().recordVersion);
                    assertEquals(CustomScoreSnapshot.Status.EXPERIMENTAL, Dungeon.customScoring().status);
                    assertEquals(CustomRuleMultipliers.VERSION, Dungeon.customScoring().calculationVersion);
                    assertEquals(expectedMultiplier, Dungeon.customScoring().multiplierExact);
                    assertTrue("scaled hero has positive health", Dungeon.hero.HT > 0 && Dungeon.hero.HP > 0);
                    Dungeon.hero.HP = Math.max(1, Dungeon.hero.HT - 7);
                    savedHealth[0] = Dungeon.hero.HP;
                    try { Dungeon.saveAll(); } catch (java.io.IOException failure) { throw new RuntimeException(failure); }
                    Game.switchScene(TitleScene.class);
                });
                waitUntil(heroClass + " saved title", () -> Game.scene() instanceof TitleScene, 30000);

                // Next-run preferences must not replace the locked saved-run identity.
                onRender(() -> {
                    SPDSettings.apdCustomRules(CustomDungeonRules.defaults());
                    SPDSettings.apdDifficulty(3);
                    SPDSettings.challenges(0);
                    Dungeon.hero = null;
                    InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
                    Game.switchScene(InterlevelScene.class);
                });
                waitUntil(heroClass + " continued floor",
                        () -> Game.scene() instanceof GameScene && Dungeon.hero != null && Dungeon.hero.ready,
                        150000);
                waitUntil(heroClass + " continued controls", EarlyAlphaRosterDeviceTest::controlsUsable, 30000);

                onRender(() -> {
                    assertEquals(heroClass, Dungeon.hero.heroClass);
                    assertArrayEquals(expected.values(), Dungeon.customRules().values());
                    assertEquals(5, Dungeon.difficulty);
                    assertEquals(511, Dungeon.challenges);
                    assertNotNull(Dungeon.customScoring());
                    assertEquals(CustomScoreSnapshot.VERSION, Dungeon.customScoring().recordVersion);
                    assertEquals(CustomScoreSnapshot.Status.EXPERIMENTAL, Dungeon.customScoring().status);
                    assertEquals(CustomRuleMultipliers.VERSION, Dungeon.customScoring().calculationVersion);
                    assertEquals(expectedMultiplier, Dungeon.customScoring().multiplierExact);
                    assertEquals(savedHealth[0], Dungeon.hero.HP);
                });
                Thread.sleep(400);
                TopScoresDeviceTest.screenshot("alpha-roster-"+heroClass.name().toLowerCase(java.util.Locale.ROOT)+".png");
                onRender(() -> Game.switchScene(TitleScene.class));
                waitUntil(heroClass + " cleanup title", () -> Game.scene() instanceof TitleScene, 30000);
                onRender(EarlyAlphaRosterDeviceTest::deleteDisposableSlot);
                completed++;
            }
                assertEquals("all shipped classes completed Android start/save/continue", 6, completed);
            } finally {
                onRender(() -> {
                    if (!(Game.scene() instanceof TitleScene)) Game.switchScene(TitleScene.class);
                    deleteDisposableSlot();
                    SPDSettings.apdCustomRules(previousRules);
                    SPDSettings.apdDifficulty(previousDifficulty);
                    SPDSettings.challenges(previousChallenges);
                    SPDSettings.intro(previousIntro);
                    SPDSettings.apdRecording(previousRecording);
                    GamesInProgress.curSlot = previousSlot;
                    GamesInProgress.selectedClass = previousSelected;
                });
            }
        }
    }
}
