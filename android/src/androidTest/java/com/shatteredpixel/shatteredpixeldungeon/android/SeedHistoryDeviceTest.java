/* ADHD Pixel Dungeon (C) 2026 Chris Charlton, GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.android.SearchContextDeviceTest.*;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.Rankings;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.LegacyImporter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.SeedHistory;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadStatus;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.SeedHistoryNotice;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndRanking;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextInput;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.Point;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Actual seed Set/clear and ranking Copy callbacks, using owned raw logs and unchanged rankings.
 * Text is supplied to the real TextInput; submission uses Android pointer events. This is not
 * physical-keyboard, external imported-archive provenance, or newly generated dungeon evidence.
 */
@RunWith(AndroidJUnit4.class)
public class SeedHistoryDeviceTest {
    private static WndTextInput entry(String value) throws Exception {
        Point[] toggle = new Point[1];
        render(() -> {
            assertTrue(Game.scene() instanceof HeroSelectScene);
            Component pane = (Component) field(HeroSelectScene.class, "optionsPane").get(Game.scene());
            if (!pane.visible) toggle[0] = point((IconButton) field(HeroSelectScene.class, "btnOptions").get(Game.scene()));
        });
        if (toggle[0] != null) touch(toggle[0]);
        press(Game.scene(), Messages.get(HeroSelectScene.class, "custom_seed"));
        AtomicReference<WndTextInput> input = new AtomicReference<>();
        waitFor(() -> {
            render(() -> {
                List<WndTextInput> windows = TopScoresDeviceTest.find(Game.scene(), WndTextInput.class);
                if (!windows.isEmpty()) input.set(windows.get(windows.size()-1));
            });
            return input.get() != null;
        });
        render(() -> {
            TextInput box = (TextInput) field(WndTextInput.class, "textBox").get(input.get());
            if (value != null) box.setText(value);
            point(box);
        });
        return input.get();
    }

    private static WndTitledMessage notice(String prefix) throws Exception {
        AtomicReference<WndTitledMessage> found = new AtomicReference<>();
        waitFor(() -> {
            render(() -> {
                for (WndTitledMessage window : TopScoresDeviceTest.find(Game.scene(), WndTitledMessage.class)) {
                    if (window.exists && window.visible && text(window).contains("Seed History")) {
                        assertTrue("The result belongs to the current selection: " + text(window),
                                text(window).replace("_", "").contains(prefix));
                        found.set(window);
                    }
                }
            });
            return found.get() != null;
        });
        return found.get();
    }

    private static void noNotice() throws Exception {
        render(() -> {
            for (WndTitledMessage window : TopScoresDeviceTest.find(Game.scene(), WndTitledMessage.class)) {
                assertFalse("No obsolete or generated-selection notice", window.exists && window.visible
                        && text(window).contains("Seed History"));
            }
        });
    }

    private static long seedAbsentFromRankings(long first, LegacyImporter.Extract rankings) {
        for (long seed = first; seed < first+1000; seed++) {
            SeedHistory.Summary summary = SeedHistory.lookup(null, DungeonSeed.convertToCode(seed), rankings);
            if (summary.kind == SeedHistory.Kind.UNKNOWN_IMPORTED) return seed;
        }
        throw new AssertionError("No unused deterministic fixture seed");
    }

    private static void log(File root, String logId, String runId, long seed, int deepest) {
        File file = new DeviceLogs(root, logId, TelemetryVersion.CURRENT, 1700000000000L)
                .seeds(seed, "")
                .start(runId, 0, "WARRIOR", "default", 0, 0, false)
                .enter(deepest)
                .end(LifecycleEvents.OUTCOME_DIED, "native-fixture", deepest, 5, 1200)
                .close(true);
        assertTrue(file.isFile() && file.length() > 0);
    }

    private static void checkMatching(WndTitledMessage window) throws Exception {
        render(() -> {
            String visible = text(window).replace("_", "");
            assertTrue(visible, visible.contains("Matching local history."));
            assertTrue(visible, visible.contains("Recorded previous attempts: 2"));
            assertTrue(visible, visible.contains("Deepest recorded floor: 12"));
            assertTrue(visible, visible.contains("may include imported archives"));
            assertTrue(visible, visible.contains("does not establish who originally created the seed"));
            assertFalse(visible, visible.contains("Some history could not be read completely"));
        });
    }

    private static void unfinishedRecordUsesObservedHistory(OwnedFixture owned, long seed,
                                                            LegacyImporter.Extract rankings) throws Exception {
        String code = DungeonSeed.convertToCode(seed);
        assertEquals("No fixture record exists for this seed yet", SeedHistory.Kind.UNKNOWN_IMPORTED,
                SeedHistory.lookup(owned.root, code, rankings).kind);
        Map<String,String> before = manifest(owned.root);
        DeviceLogs log = new DeviceLogs(owned.root, "seed446-unfinished", TelemetryVersion.CURRENT, 1700000001000L)
                .seeds(seed, "")
                .start("seed446-logical-unfinished", 0, "WARRIOR", "default", 0, 0, false)
                .enter(4).enter(9).enter(4);
        assertTrue("Close the owned log without a final seal or run result", log.w.close(false, 10000));
        ReadResult raw = RunLogReader.read(log.file);
        assertEquals(ReadStatus.UNSEALED, raw.status);
        assertFalse(raw.sealed);
        assertEquals(0, raw.events.stream().filter(event -> event.type == EventTypes.RUN_END).count());
        Map<String,String> withUnfinished = manifest(owned.root);
        assertEquals(before.size() + 1, withUnfinished.size());
        for (Map.Entry<String,String> existing : before.entrySet()) {
            assertEquals("Previous fixture log is unchanged", existing.getValue(), withUnfinished.get(existing.getKey()));
        }

        SeedHistory.Summary expected = SeedHistory.lookup(owned.root, code, rankings);
        assertEquals(SeedHistory.Kind.MATCHING_LOCAL_HISTORY, expected.kind);
        assertEquals(1, expected.attempts);
        assertEquals(1, expected.unfinishedAttempts);
        assertEquals(0, expected.attempts - expected.unfinishedAttempts);
        assertEquals("Use the deepest canonical observed entry, not the last entry", 9, expected.deepestFloor);
        assertEquals(1, expected.attemptsWithKnownDepth);
        assertFalse("A clean unsealed log is readable history", expected.incompleteHistory);

        WndTextInput input = entry(code);
        press(input, Messages.get(HeroSelectScene.class, "custom_seed_set"));
        WndTitledMessage unfinished = notice("Matching local history.");
        render(() -> {
            String visible = text(unfinished).replace("_", "");
            assertTrue(visible, visible.contains("Recorded previous attempts: 1"));
            assertTrue(visible, visible.contains("Finished attempts: 0"));
            assertTrue(visible, visible.contains("Unfinished attempts: 1"));
            assertTrue(visible, visible.contains("Deepest recorded floor: 9"));
            assertTrue(visible, visible.contains("Unfinished depths are observed main-dungeon floors, not final results."));
            assertTrue(visible, visible.contains("may include imported archives"));
            assertTrue(visible, visible.contains("does not establish who originally created the seed"));
            assertFalse(visible, visible.contains("Unknown imported seed."));
            assertFalse(visible, visible.contains("Some history could not be read completely"));
            assertEquals(seed, DungeonSeed.convertFromText(SPDSettings.customSeed()));
            // This notice is an ordinary non-scrolling WndTitledMessage. Every text block
            // must fit in the actual viewport; collecting its text alone is not visual proof.
            for (RenderedTextBlock block : TopScoresDeviceTest.find(unfinished, RenderedTextBlock.class)) {
                if (!block.exists || !block.isVisible() || block.text() == null || block.text().isEmpty()) continue;
                Point top = block.camera().cameraToScreen(block.left(), block.top());
                Point bottom = block.camera().cameraToScreen(block.right(), block.bottom());
                assertTrue("Unfinished-history notice text fits without clipping", top.x >= 0 && top.y >= 0
                        && bottom.x <= Game.width && bottom.y <= Game.height);
            }
        });
        TopScoresDeviceTest.screenshot("seed-history-unfinished-record.png");
        render(() -> {
            unfinished.hide();
            owned.assertOriginalFiles();
        });
        assertEquals("Unfinished lookup does not rewrite logs or create a cache", withUnfinished, manifest(owned.root));
        System.out.println("APD446_SEED_HISTORY unfinished_exact_seed=true total_attempts=1"
                + " finished_attempts=0 unfinished_attempts=1 observed_main_depth=9"
                + " clean_unsealed=true no_terminal_record=true rankings_unchanged=true");
    }

    @Test public void seedEntryShowsOnlyMatchingRecordedHistory() throws Exception {
        try (ActivityScenario<AndroidLauncher> app = ActivityScenario.launch(AndroidLauncher.class)) {
            waitFor(() -> Game.scene() != null && PixelScene.uiCamera != null);
            AtomicReference<OwnedFixture> owned = new AtomicReference<>();
            try {
                render(() -> {
                    assertTrue("The existing unlock policy permits seed controls in these private debug APKs",
                            DeviceCompat.isDebug());
                    owned.set(new OwnedFixture("seed-history"));
                });
                // Finish startup before adding the measured history: TitleScene prepares
                // UIMSD derived rows, and entering gameplay retires that background load.
                owned.get().start(HeroClass.WARRIOR, false);
                waitFor(() -> {
                    for (Thread thread : Thread.getAllStackTraces().keySet()) {
                        if (thread.isAlive() && ("uimsd-title-prewarm".equals(thread.getName())
                                || "uimsd-load".equals(thread.getName()))) return false;
                    }
                    return true;
                });
                render(() -> {
                    assertTrue("Startup has left TitleScene before adding history", Game.scene() instanceof GameScene
                            && !Game.switchingScene());
                    assertNotNull(Dungeon.hero);
                    assertTrue(Dungeon.hero.ready);
                });
                LegacyImporter.Extract rankings = LegacyImporter.extractRankingsFile(System.currentTimeMillis());
                long knownSeed = seedAbsentFromRankings(445202611L, rankings);
                long unknownSeed = seedAbsentFromRankings(knownSeed+1, rankings);
                long unfinishedSeed = seedAbsentFromRankings(unknownSeed+1, rankings);
                String knownCode = DungeonSeed.convertToCode(knownSeed);
                String unknownCode = DungeonSeed.convertToCode(unknownSeed);
                log(owned.get().root, "seed445-attempt-one", "seed445-logical-one", knownSeed, 7);
                log(owned.get().root, "seed445-exact-copy", "seed445-logical-one", knownSeed, 7);
                log(owned.get().root, "seed445-attempt-two", "seed445-logical-two", knownSeed, 12);
                Map<String,String> fixtureLogs = manifest(owned.get().root);
                assertEquals(3, fixtureLogs.size());
                SeedHistory.Summary expected = SeedHistory.lookup(owned.get().root, knownCode, rankings);
                assertEquals(SeedHistory.Kind.MATCHING_LOCAL_HISTORY, expected.kind);
                assertEquals(2, expected.attempts);
                assertEquals(0, expected.unfinishedAttempts);
                assertEquals(12, expected.deepestFloor);
                assertEquals(2, expected.attemptsWithKnownDepth);
                assertFalse(expected.incompleteHistory);

                // Build a genuine ranking-page payload from an owned initialized hero, without
                // submitting a ranking or writing rankings.dat. The record/seed are fixture data.
                AtomicReference<Rankings.Record> record = new AtomicReference<>();
                render(() -> {
                    Rankings.Record current = new Rankings.Record();
                    current.heroClass = HeroClass.WARRIOR;
                    current.herolevel = Dungeon.hero.lvl;
                    current.depth = Dungeon.depth;
                    current.armorTier = 1;
                    current.score = 1200;
                    current.gameID = "seed445-ui-record";
                    current.customSeed = "";
                    Rankings.INSTANCE.saveGameData(current);
                    current.gameData.put(Rankings.SEED, knownSeed);
                    current.gameData.put(Rankings.CUSTOM_SEED, "");
                    current.gameData.put(Rankings.DAILY, false);
                    current.gameData.put(Rankings.DAILY_REPLAY, false);
                    record.set(current);
                    GamesInProgress.selectedClass = HeroClass.WARRIOR;
                    ShatteredPixelDungeon.switchNoFade(HeroSelectScene.class);
                });
                waitFor(() -> Game.scene() instanceof HeroSelectScene && !Game.switchingScene());
                WndTextInput input = entry(knownCode);
                long entryFrame = com.badlogic.gdx.Gdx.graphics.getFrameId();
                waitFor(() -> com.badlogic.gdx.Gdx.graphics.getFrameId() >= entryFrame + 4);
                Thread.sleep(150);
                render(() -> {
                    TextInput box = (TextInput) field(WndTextInput.class, "textBox").get(input);
                    assertEquals("The captured entry contains the assigned seed", knownCode, box.getText());
                    point(box);
                });
                TopScoresDeviceTest.screenshot("seed-history-entry.png");
                press(input, Messages.get(HeroSelectScene.class, "custom_seed_set"));
                WndTitledMessage matching = notice("Matching local history.");
                checkMatching(matching);
                render(() -> assertEquals(knownSeed, DungeonSeed.convertFromText(SPDSettings.customSeed())));
                TopScoresDeviceTest.screenshot("seed-history-matching-records.png");
                render(matching::hide);

                // Hold the existing worker, then exercise two real Set callbacks. The bounded
                // queue/cancellation must show only the latest queued selection. This does not
                // claim that an already-running old lookup completed after a newer selection.
                ThreadPoolExecutor worker = (ThreadPoolExecutor) field(SeedHistoryNotice.class, "WORKER").get(null);
                CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
                AtomicReference<Throwable> workerFailure = new AtomicReference<>();
                worker.execute(() -> {
                    started.countDown();
                    try { if (!release.await(60, TimeUnit.SECONDS)) throw new AssertionError("Fixture worker gate timeout"); }
                    catch (Throwable error) { workerFailure.set(error); }
                });
                assertTrue(started.await(10, TimeUnit.SECONDS));
                try {
                    WndTextInput first = entry(knownCode);
                    press(first, Messages.get(HeroSelectScene.class, "custom_seed_set"));
                    noNotice();
                    WndTextInput second = entry(unknownCode);
                    press(second, Messages.get(HeroSelectScene.class, "custom_seed_set"));
                    noNotice();
                } finally { release.countDown(); }
                WndTitledMessage unknown = notice("Unknown imported seed.");
                render(() -> {
                    String visible = text(unknown).replace("_", "");
                    assertTrue(visible, visible.contains("No matching recorded attempt with a known seed"));
                    assertTrue(visible, visible.contains("The original creator is unknown."));
                    assertFalse(visible, visible.contains("Recorded previous attempts: 2"));
                    assertEquals(unknownSeed, DungeonSeed.convertFromText(SPDSettings.customSeed()));
                });
                assertNull(workerFailure.get());
                TopScoresDeviceTest.screenshot("seed-history-latest-unknown-selection.png");
                render(unknown::hide);
                WndTextInput clear = entry(null);
                press(clear, Messages.get(HeroSelectScene.class, "custom_seed_clear"));
                render(() -> assertEquals("", SPDSettings.customSeed()));
                noNotice();
                WndTextInput blank = entry(null);
                render(() -> assertEquals("", ((TextInput) field(WndTextInput.class, "textBox").get(blank)).getText()));
                TopScoresDeviceTest.screenshot("seed-history-cleared-generated-selection.png");
                press(blank, Messages.get(HeroSelectScene.class, "custom_seed_clear"));
                noNotice();

                AtomicReference<WndRanking> rankingWindow = new AtomicReference<>();
                render(() -> {
                    WndRanking window = new WndRanking(record.get());
                    Game.scene().addToFront(window);
                    rankingWindow.set(window);
                    assertNotNull("The real ranking payload loads a hero", Dungeon.hero);
                    assertEquals(knownSeed, Dungeon.seed);
                });
                Class<?> statsTab = Class.forName(WndRanking.class.getName() + "$StatsTab");
                press(rankingWindow.get(), Messages.get(statsTab, "copy_seed"));
                AtomicReference<WndOptions> confirmation = new AtomicReference<>();
                waitFor(() -> {
                    render(() -> {
                        List<WndOptions> options = TopScoresDeviceTest.find(Game.scene(), WndOptions.class);
                        if (!options.isEmpty()) confirmation.set(options.get(options.size()-1));
                    });
                    return confirmation.get() != null;
                });
                TopScoresDeviceTest.screenshot("seed-history-ranking-copy-confirm.png");
                press(confirmation.get(), Messages.get(statsTab, "copy_seed_copy"));
                WndTitledMessage copied = notice("Matching local history.");
                checkMatching(copied);
                TopScoresDeviceTest.screenshot("seed-history-ranking-copy-result.png");
                render(() -> {
                    assertEquals(knownSeed, DungeonSeed.convertFromText(SPDSettings.customSeed()));
                    assertEquals("The existing historical score is unchanged", 1200, record.get().score);
                    copied.hide();
                    rankingWindow.get().hide();
                    owned.get().assertOriginalFiles();
                });
                assertEquals("The lookup writes no fixture log, cache or derived index", fixtureLogs, manifest(owned.get().root));
                System.out.println("APD445_SEED_HISTORY callbacks=hero-select-set,clear,ranking-copy"
                        + " logical_attempts=2 raw_logs=3 deepest=12 latest_queued_selection_only=true rankings_unchanged=true");
                unfinishedRecordUsesObservedHistory(owned.get(), unfinishedSeed, rankings);
            } finally {
                if (owned.get() != null) owned.get().close();
            }
        }
    }
}
