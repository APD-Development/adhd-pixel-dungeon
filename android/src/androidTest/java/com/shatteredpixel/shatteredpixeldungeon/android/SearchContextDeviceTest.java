/* ADHD Pixel Dungeon (C) 2026 Chris Charlton, GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.Rankings;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.DoorSearchEligibility;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchControlContext;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchControlContext.Action;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.WallSearchSession;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.RecorderStatus;
import com.shatteredpixel.shatteredpixeldungeon.effects.BadgeBanner;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.CrystalChoiceRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.CrystalVaultRoom;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ResumeIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.SeedHistoryNotice;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.Point;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Controlled observed geometry on disposable, normally initialized Warrior and Rogue floors.
 * The real chooser, Android pointer events, talent map and search/movement handlers are used.
 * The crystal-room objects and chest scenery are a synthetic layout, not a generated-room claim.
 */
@RunWith(AndroidJUnit4.class)
public class SearchContextDeviceTest {
    interface CheckedRunnable { void run() throws Exception; }

    static void render(CheckedRunnable task) throws Exception {
        TopScoresDeviceTest.render(() -> {
            try { task.run(); }
            catch (Exception error) { throw new RuntimeException(error); }
        });
    }

    static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    static void waitFor(Callable<Boolean> condition) throws Exception {
        long until = SystemClock.uptimeMillis() + 120000;
        while (SystemClock.uptimeMillis() < until) {
            if (condition.call()) return;
            Thread.sleep(100);
        }
        fail("APD445 native fixture timed out");
    }

    static void readyFrame() throws Exception {
        Field fade = field(Class.forName(PixelScene.class.getName() + "$Fader"), "INSTANCE");
        waitFor(() -> {
            AtomicBoolean ready = new AtomicBoolean();
            render(() -> ready.set(Game.scene() instanceof GameScene && Dungeon.hero != null
                    && Dungeon.hero.ready && !Dungeon.hero.sprite.isMoving && !Game.switchingScene()
                    && fade.get(null) == null && TopScoresDeviceTest.find(Game.scene(), BadgeBanner.class)
                    .stream().noneMatch(b -> b.exists && b.visible)));
            return ready.get();
        });
    }

    static Point point(Component component) {
        assertTrue("Actual visible control", component.exists && component.isVisible() && component.isActive());
        Point result = component.camera().cameraToScreen(component.left() + component.width()/2,
                component.top() + component.height()/2);
        assertTrue("Control must fit on screen: " + result, result.x >= 0 && result.y >= 0
                && result.x < Game.width && result.y < Game.height);
        return result;
    }

    static void touch(Point point) throws Exception {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long down = SystemClock.uptimeMillis();
        for (int action : new int[]{MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
            MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, point.x, point.y, 0);
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            try { instrumentation.sendPointerSync(event); }
            finally { event.recycle(); }
            Thread.sleep(100);
        }
        Thread.sleep(250);
    }

    static void press(Group parent, String text) throws Exception {
        Point[] target = new Point[1];
        render(() -> {
            List<StyledButton> matches = new ArrayList<>();
            for (StyledButton button : TopScoresDeviceTest.find(parent, StyledButton.class)) {
                if (button.exists && button.visible && button.active && text.equals(button.text())) matches.add(button);
            }
            assertEquals("One actual button labelled " + text, 1, matches.size());
            target[0] = point(matches.get(0));
        });
        touch(target[0]);
    }

    static String text(Group parent) throws Exception {
        StringBuilder result = new StringBuilder();
        for (RenderedTextBlock block : TopScoresDeviceTest.find(parent, RenderedTextBlock.class)) {
            if (block.exists && block.visible) result.append(block.text()).append('\n');
        }
        return result.toString();
    }

    static String digest(File file) throws Exception {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            for (int count; (count = input.read(buffer)) != -1;) hash.update(buffer, 0, count);
        }
        StringBuilder out = new StringBuilder();
        for (byte value : hash.digest()) out.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return out.toString();
    }

    static Map<String,String> manifest(File directory) throws Exception {
        Map<String,String> result = new TreeMap<>();
        if (directory != null && directory.exists()) manifest(directory, directory, result);
        return result;
    }

    private static void manifest(File root, File current, Map<String,String> result) throws Exception {
        String base = root.getCanonicalPath();
        String path = current.getCanonicalPath();
        assertTrue("History traversal remains under its root", path.equals(base) || path.startsWith(base + File.separator));
        if (current.isDirectory()) {
            File[] children = current.listFiles();
            assertNotNull("Readable history directory", children);
            for (File child : children) manifest(root, child, result);
        } else if (current.isFile()) result.put(path.substring(base.length()), digest(current));
    }

    /** Owns only a fresh cache directory and slots verified empty before this fixture starts them. */
    static final class OwnedFixture implements AutoCloseable {
        final boolean intro, oldDaily, oldReplay, oldRecording;
        final String selectedSeed, oldSeedText;
        final long oldSeed;
        final int oldSlot;
        final HeroClass oldClass;
        final File previousRoot, root;
        final byte[] rankingBytes;
        final Map<String,String> originalHistory;
        final List<Integer> slots = new ArrayList<>();

        OwnedFixture(String prefix) throws Exception {
            assertNotNull("Launch Android before preferences", Gdx.app);
            SPDSettings.contains(SPDSettings.KEY_INTRO);
            intro = SPDSettings.intro();
            selectedSeed = SPDSettings.customSeed();
            oldSlot = GamesInProgress.curSlot;
            oldClass = GamesInProgress.selectedClass;
            oldDaily = Dungeon.daily;
            oldReplay = Dungeon.dailyReplay;
            oldSeedText = Dungeon.customSeedText;
            oldSeed = Dungeon.seed;
            RecorderStatus recorder = ApdRecorder.status();
            assertNull("Do not replace an active player's recorder", recorder.logId);
            assertFalse("Keep any failed recorder state intact by refusing this fixture", recorder.sessionDisabled);
            assertEquals("Do not clear recorder errors", 0, recorder.errorCount);
            previousRoot = ApdRecorder.historyRoot();
            oldRecording = recorder.enabled;
            originalHistory = manifest(previousRoot);
            com.badlogic.gdx.files.FileHandle rankings = FileUtils.getFileHandle(Rankings.RANKINGS_FILE);
            rankingBytes = rankings.exists() ? rankings.readBytes() : null;
            File cache = InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir();
            root = new File(cache, "apd445-" + prefix + "-" + UUID.randomUUID());
            assertFalse(root.exists());
            assertTrue(root.mkdir());
            ApdRecorder.configure(root, false);
            SPDSettings.intro(false);
            System.out.println("APD445_OWNED_FIXTURE captured intro=" + intro + " original_history_files="
                    + originalHistory.size() + " owned=" + root.getName());
        }

        void start(HeroClass heroClass, boolean secondFloor) throws Exception {
            render(() -> ShatteredPixelDungeon.switchNoFade(TitleScene.class));
            waitFor(() -> Game.scene() instanceof TitleScene && !Game.switchingScene());
            render(() -> {
                int slot = GamesInProgress.firstEmpty(heroClass);
                assertTrue("Only a free slot", slot > 0);
                assertTrue(GamesInProgress.isAvailableNewSlot(heroClass, slot));
                slots.add(slot);
                GamesInProgress.curSlot = slot;
                GamesInProgress.selectedClass = heroClass;
                Dungeon.hero = null;
                Dungeon.daily = Dungeon.dailyReplay = false;
                Dungeon.customSeedText = "";
                SPDSettings.customSeed("");
                Dungeon.initSeed();
                InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
                Game.switchScene(InterlevelScene.class);
            });
            readyFrame();
            render(() -> assertEquals(heroClass, Dungeon.hero.heroClass));
            if (secondFloor) {
                render(() -> {
                    assertEquals(1, Dungeon.depth);
                    assertFalse(DoorSearchEligibility.supportsDoors(Dungeon.level));
                    InterlevelScene.curTransition = Dungeon.level.getTransition(LevelTransition.Type.REGULAR_EXIT);
                    assertNotNull(InterlevelScene.curTransition);
                    InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
                    Game.switchScene(InterlevelScene.class);
                });
                readyFrame();
                render(() -> {
                    assertEquals(2, Dungeon.depth);
                    assertTrue(DoorSearchEligibility.supportsDoors(Dungeon.level));
                });
            }
        }

        void assertOriginalFiles() throws Exception {
            assertEquals("Original history remains byte for byte unchanged", originalHistory, manifest(previousRoot));
            com.badlogic.gdx.files.FileHandle file = FileUtils.getFileHandle(Rankings.RANKINGS_FILE);
            assertArrayEquals("No ranking file writes", rankingBytes, file.exists() ? file.readBytes() : null);
        }

        @Override public void close() throws Exception {
            try {
                render(() -> {
                    SeedHistoryNotice.selected("");
                    ShatteredPixelDungeon.switchNoFade(TitleScene.class);
                });
                waitFor(() -> Game.scene() instanceof TitleScene && !Game.switchingScene());
                ThreadPoolExecutor worker = (ThreadPoolExecutor) field(SeedHistoryNotice.class, "WORKER").get(null);
                waitFor(() -> worker.getActiveCount() == 0 && worker.getQueue().isEmpty());
            } finally {
                try {
                    render(() -> {
                        try {
                            // Slot deletion can notify the recorder. Keep those notifications
                            // inside the disabled owned store, never the original history root.
                            ApdRecorder.configure(root, false);
                            for (int slot : slots) Dungeon.deleteGame(slot, true);
                        } finally {
                            try {
                                ApdRecorder.configure(previousRoot, oldRecording);
                                SPDSettings.intro(intro);
                                SPDSettings.customSeed(selectedSeed);
                                assertEquals(intro, SPDSettings.intro());
                                assertEquals(selectedSeed, SPDSettings.customSeed());
                                assertEquals(previousRoot, ApdRecorder.historyRoot());
                                assertOriginalFiles();
                                System.out.println("APD445_OWNED_FIXTURE restored intro=" + intro
                                        + " original_history_and_rankings_unchanged=true");
                            } finally {
                                GamesInProgress.curSlot = oldSlot;
                                GamesInProgress.selectedClass = oldClass;
                                Dungeon.daily = oldDaily;
                                Dungeon.dailyReplay = oldReplay;
                                Dungeon.customSeedText = oldSeedText;
                                Dungeon.seed = oldSeed;
                            }
                        }
                    });
                } finally {
                    deleteOwned(root, root);
                }
            }
        }

        private static void deleteOwned(File root, File current) throws Exception {
            String base = root.getCanonicalPath(), path = current.getCanonicalPath();
            assertTrue(root.getName().startsWith("apd445-"));
            assertTrue(path.equals(base) || path.startsWith(base + File.separator));
            File[] children = current.listFiles();
            if (children != null) for (File child : children) deleteOwned(root, child);
            assertTrue("Delete only fixture-owned file " + current.getName(), !current.exists() || current.delete());
        }
    }

    private static final class Snapshot {
        final Hero hero = Dungeon.hero;
        final Level level = Dungeon.level;
        final int pos = hero.pos, hp = hero.HP;
        final float time = Actor.now(), cooldown = hero.cooldown();
        final HeroAction current = hero.curAction, previous = hero.lastAction;
        final String memory, marks;
        Snapshot() {
            Bundle saved = new Bundle(), checked = new Bundle();
            hero.wallSearch.store(saved);
            level.searchedWalls.store(checked);
            memory = saved.toString();
            marks = checked.toString();
        }
        void unchanged() throws Exception {
            assertSame(hero, Dungeon.hero);
            assertSame(level, Dungeon.level);
            assertSame("Keep the actual waiting actor", hero, field(Actor.class, "current").get(null));
            assertEquals(pos, hero.pos);
            assertEquals(hp, hero.HP);
            assertEquals(time, Actor.now(), 0);
            assertEquals(cooldown, hero.cooldown(), 0);
            assertSame(current, hero.curAction);
            assertSame(previous, hero.lastAction);
            Bundle saved = new Bundle(), checked = new Bundle();
            hero.wallSearch.store(saved);
            level.searchedWalls.store(checked);
            assertEquals(memory, saved.toString());
            assertEquals(marks, checked.toString());
        }
    }

    private static final class Probe {
        final Hero hero = Dungeon.hero;
        final RegularLevel level = (RegularLevel) Dungeon.level;
        final int width = level.width(), anchor = width * (level.height()/2) + width/2;
        final ResumeIndicator indicator;
        final Field rooms = field(RegularLevel.class, "rooms");
        WndOptions window;
        Snapshot before;

        Probe() throws Exception {
            assertTrue(width >= 15 && level.height() >= 15);
            assertTrue(hero.ready && hero.isAlive());
            assertSame(hero, field(Actor.class, "current").get(null));
            indicator = TopScoresDeviceTest.find(Game.scene(), ResumeIndicator.class).get(0);
            for (Mob mob : new ArrayList<>(level.mobs)) {
                Actor.remove(mob);
                if (mob.sprite != null) mob.sprite.killAndErase();
            }
            level.mobs.clear();
            openArea();
        }

        void openArea() throws Exception {
            assertTrue(hero.ready);
            rooms.set(level, new ArrayList<Room>());
            for (int y = -6; y <= 6; y++) for (int x = -6; x <= 6; x++) {
                int cell = anchor + y*width + x;
                Heap heap = level.heaps.get(cell);
                if (heap != null) {
                    if (heap.sprite != null) heap.sprite.killAndErase();
                    level.heaps.remove(cell);
                }
                level.plants.remove(cell);
                level.traps.remove(cell);
                Level.set(cell, Terrain.EMPTY);
                level.heroFOV[cell] = level.visited[cell] = level.discoverable[cell] = true;
                level.mapped[cell] = false;
            }
            level.searchedWalls.clear();
            hero.pos = anchor;
            hero.sprite.place(anchor);
            hero.curAction = hero.lastAction = null;
            hero.fieldOfView = level.heroFOV;
            hero.wallSearch.clear();
            hero.wallSearch.searched(hero); // Fixture input: one remembered ordinary search.
            assertEquals(anchor, hero.wallSearch.anchor());
            GameScene.updateMap();
            GameScene.updateFog();
            Camera.main.snapTo(hero.sprite.center());
        }

        int wall(int dx, int dy) {
            int cell = anchor + dx + dy*width;
            for (int at : new int[]{cell-width, cell, cell+width}) Level.set(at, Terrain.WALL);
            GameScene.updateMap();
            GameScene.updateFog();
            assertTrue("Observed door-compatible side frames", DoorSearchEligibility.candidate(level, cell));
            assertFalse(level.searchedWalls.confirmed(level, cell));
            return cell;
        }

        void crystal(Room room) throws Exception {
            openArea();
            int x = anchor%width, y = anchor/width;
            room.set(x-3, y-1, x+3, y+5);
            rooms.set(level, new ArrayList<>(Arrays.asList(room)));
            for (int side = -3; side <= 3; side++) Level.set(anchor-width+side, Terrain.WALL);
            for (int dx : new int[]{-2, 2}) {
                Heap heap = level.drop(new Gold(1), anchor + 2*width + dx);
                heap.type = Heap.Type.CRYSTAL_CHEST;
                if (heap.sprite != null) heap.sprite.link(heap);
            }
            GameScene.updateMap();
            assertSame("The actual room classifier sees the occupied crystal room", room, level.room(hero.pos));
            assertTrue(DoorSearchEligibility.candidate(level, anchor-width));
            assertTrue(WallSearchSession.covers(hero, level, anchor, anchor-width));
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
        }

        void open(Action primary, String title) throws Exception {
            Point[] target = new Point[1];
            render(() -> {
                assertEquals(primary, SearchControlContext.preferred(hero, level));
                indicator.update();
                before = new Snapshot();
                target[0] = point(indicator);
            });
            touch(target[0]);
            waitFor(() -> {
                AtomicBoolean found = new AtomicBoolean();
                render(() -> {
                    List<WndOptions> options = TopScoresDeviceTest.find(Game.scene(), WndOptions.class);
                    if (!options.isEmpty()) { window = options.get(options.size()-1); found.set(true); }
                });
                return found.get();
            });
            render(() -> {
                before.unchanged();
                List<RedButton> buttons = TopScoresDeviceTest.find(window, RedButton.class);
                List<String> actual = new ArrayList<>();
                for (RedButton button : buttons) { actual.add(button.text()); point(button); }
                String first = primary == Action.SEARCH ? "Search here"
                        : primary == Action.RESUME_MOVEMENT ? "Resume previous movement" : "Resume wall search";
                assertEquals(first, actual.get(0));
                LinkedHashSet<String> expected = new LinkedHashSet<>();
                expected.add(first);
                expected.addAll(Arrays.asList("Search here", "Resume wall search", "Show saved search position"));
                if (hero.lastAction != null) expected.add("Resume previous movement");
                expected.add("Forget this search");
                expected.add("Cancel");
                assertEquals(new ArrayList<>(expected), actual);
                assertTrue("Correct visible title", TopScoresDeviceTest.find(window, RenderedTextBlock.class)
                        .stream().anyMatch(block -> block.exists && block.isVisible() && title.equals(block.text())));
                System.out.println("APD445_SEARCH_CONTEXT class=" + hero.heroClass + " wide="
                        + hero.pointsInTalent(Talent.WIDE_SEARCH) + " primary=" + primary + " options=" + actual);
            });
        }

        void captureAndCancel(String image) throws Exception {
            TopScoresDeviceTest.screenshot(image);
            press(window, "Cancel");
            render(() -> { assertNull("Chooser was removed from the real scene", window.parent); before.unchanged(); });
        }
    }

    @Test public void resumeChooserUsesObservedContextAndDispatchesExplicitActions() throws Exception {
        try (ActivityScenario<AndroidLauncher> app = ActivityScenario.launch(AndroidLauncher.class)) {
            waitFor(() -> Game.scene() != null && PixelScene.uiCamera != null);
            AtomicReference<OwnedFixture> owned = new AtomicReference<>();
            try {
                render(() -> owned.set(new OwnedFixture("search")));
                owned.get().start(HeroClass.WARRIOR, true);
                AtomicReference<Probe> probe = new AtomicReference<>();
                render(() -> probe.set(new Probe()));
                Probe p = probe.get();
                p.open(Action.SEARCH, "Search");
                p.captureAndCancel("search-context-open-area.png");
                render(() -> p.hero.lastAction = new HeroAction.Move(p.anchor+1));
                p.open(Action.RESUME_MOVEMENT, "Resume");
                p.captureAndCancel("search-context-movement-first.png");
                render(() -> { p.openArea(); p.wall(1, 0); });
                p.open(Action.WALL_SEARCH, "Remembered wall search");
                p.captureAndCancel("search-context-observed-wall-first.png");
                render(() -> p.level.searchedWalls.recordCompletedDoorCheck(p.level, p.anchor+1, true));
                p.open(Action.SEARCH, "Search");
                p.captureAndCancel("search-context-confirmed-wall.png");
                render(() -> { p.openArea(); int cell = p.wall(1, 0); p.level.heroFOV[cell] = false; GameScene.updateFog(); });
                p.open(Action.SEARCH, "Search");
                p.captureAndCancel("search-context-unseen-wall.png");
                render(() -> {
                    p.openArea(); p.wall(1, 0);
                    int depth = Dungeon.depth;
                    try {
                        Dungeon.depth = depth+1;
                        p.hero.wallSearch.clear();
                        p.hero.wallSearch.searched(p.hero);
                    } finally { Dungeon.depth = depth; }
                    assertFalse(p.hero.wallSearch.matches(p.level));
                    assertTrue(SearchControlContext.hasMeaningfulWall(p.hero, p.level));
                });
                p.open(Action.SEARCH, "Search");
                p.captureAndCancel("search-context-other-floor-memory.png");
                render(() -> p.crystal(new CrystalVaultRoom()));
                p.open(Action.SEARCH, "Search");
                p.captureAndCancel("search-context-crystal-vault.png");
                render(() -> p.crystal(new CrystalChoiceRoom()));
                p.open(Action.SEARCH, "Search");
                p.captureAndCancel("search-context-crystal-choice.png");
                render(() -> {
                    p.openArea();
                    p.hero.metamorphedTalents.put(Talent.IRON_STOMACH, Talent.WIDE_SEARCH);
                    p.hero.talents.clear();
                    Talent.initClassTalents(p.hero);
                    assertEquals(HeroClass.WARRIOR, p.hero.heroClass);
                    assertEquals(Talent.WIDE_SEARCH, p.hero.metamorphedTalents.get(Talent.IRON_STOMACH));
                    assertEquals(0, p.hero.pointsInTalent(Talent.WIDE_SEARCH));
                    p.wall(2, 0);
                    assertFalse(WallSearchSession.covers(p.hero, p.level, p.anchor, p.anchor+2));
                });
                p.open(Action.SEARCH, "Search");
                p.captureAndCancel("search-context-metamorph-unacquired.png");
                render(() -> { p.hero.upgradeTalent(Talent.WIDE_SEARCH); assertEquals(1, p.hero.pointsInTalent(Talent.WIDE_SEARCH)); });
                p.open(Action.WALL_SEARCH, "Remembered wall search");
                p.captureAndCancel("search-context-metamorph-rank1-radius.png");
                render(() -> { p.openArea(); p.wall(2, 2); assertFalse(WallSearchSession.covers(p.hero, p.level, p.anchor, p.anchor+2+2*p.width)); });
                p.open(Action.SEARCH, "Search");
                p.captureAndCancel("search-context-metamorph-rank1-rounded.png");
                render(() -> { p.hero.upgradeTalent(Talent.WIDE_SEARCH); assertEquals(2, p.hero.pointsInTalent(Talent.WIDE_SEARCH)); assertTrue(WallSearchSession.covers(p.hero, p.level, p.anchor, p.anchor+2+2*p.width)); assertFalse(WallSearchSession.covers(p.hero, p.level, p.anchor, p.anchor+3)); });
                p.open(Action.WALL_SEARCH, "Remembered wall search");
                p.captureAndCancel("search-context-metamorph-rank2-square.png");
                render(p::openArea);
                p.open(Action.SEARCH, "Search");
                render(() -> p.hero.ready = false);
                try { press(p.window, "Search here"); }
                finally { render(() -> p.hero.ready = true); }
                render(() -> { assertNull("Busy selection closes without dispatch", p.window.parent); p.before.unchanged(); });
                p.open(Action.SEARCH, "Search");
                float[] spent = new float[1]; int[] searches = new int[1];
                render(() -> { spent[0] = Actor.now(); searches[0] = p.hero.wallSearch.searches(); });
                press(p.window, "Search here");
                readyFrame();
                render(() -> { assertEquals(searches[0]+1, p.hero.wallSearch.searches()); assertEquals(p.anchor, p.hero.pos); assertEquals(2, Actor.now()-spent[0], .001f); });
                TopScoresDeviceTest.screenshot("search-context-explicit-search.png");
                render(() -> { p.openArea(); p.wall(1, 0); });
                p.open(Action.WALL_SEARCH, "Remembered wall search");
                render(() -> { spent[0] = Actor.now(); searches[0] = p.hero.wallSearch.searches(); });
                press(p.window, "Resume wall search");
                readyFrame();
                render(() -> { assertEquals(searches[0]+1, p.hero.wallSearch.searches()); assertEquals(p.anchor, p.hero.pos); assertEquals(2, Actor.now()-spent[0], .001f); });
                TopScoresDeviceTest.screenshot("search-context-explicit-wall.png");
                render(() -> { p.openArea(); p.hero.lastAction = new HeroAction.Move(p.anchor+1); spent[0] = Actor.now(); searches[0] = p.hero.wallSearch.searches(); });
                p.open(Action.RESUME_MOVEMENT, "Resume");
                press(p.window, "Resume previous movement");
                readyFrame();
                render(() -> { assertEquals(p.anchor+1, p.hero.pos); assertNull(p.hero.lastAction); assertEquals(searches[0], p.hero.wallSearch.searches()); assertTrue(Actor.now() > spent[0]); });
                TopScoresDeviceTest.screenshot("search-context-explicit-movement.png");

                owned.get().start(HeroClass.ROGUE, true);
                render(() -> probe.set(new Probe()));
                Probe rogue = probe.get();
                render(() -> {
                    assertEquals(HeroClass.ROGUE, rogue.hero.heroClass);
                    assertEquals(0, rogue.hero.pointsInTalent(Talent.WIDE_SEARCH));
                    rogue.wall(2, 2);
                    assertTrue(WallSearchSession.covers(rogue.hero, rogue.level, rogue.anchor, rogue.anchor+2+2*rogue.width));
                    assertFalse(WallSearchSession.covers(rogue.hero, rogue.level, rogue.anchor, rogue.anchor+3));
                });
                rogue.open(Action.WALL_SEARCH, "Remembered wall search");
                rogue.captureAndCancel("search-context-rogue-rank0.png");
                render(() -> { rogue.openArea(); rogue.wall(3, 0); });
                rogue.open(Action.SEARCH, "Search");
                rogue.captureAndCancel("search-context-rogue-unacquired-radius.png");
                render(() -> { rogue.hero.upgradeTalent(Talent.WIDE_SEARCH); assertEquals(1, rogue.hero.pointsInTalent(Talent.WIDE_SEARCH)); });
                rogue.open(Action.WALL_SEARCH, "Remembered wall search");
                rogue.captureAndCancel("search-context-rogue-rank1-radius.png");
                render(() -> { rogue.openArea(); rogue.wall(3, 3); assertFalse(WallSearchSession.covers(rogue.hero, rogue.level, rogue.anchor, rogue.anchor+3+3*rogue.width)); });
                rogue.open(Action.SEARCH, "Search");
                rogue.captureAndCancel("search-context-rogue-rank1-rounded.png");
                render(() -> { rogue.hero.upgradeTalent(Talent.WIDE_SEARCH); assertEquals(2, rogue.hero.pointsInTalent(Talent.WIDE_SEARCH)); assertTrue(WallSearchSession.covers(rogue.hero, rogue.level, rogue.anchor, rogue.anchor+3+3*rogue.width)); assertFalse(WallSearchSession.covers(rogue.hero, rogue.level, rogue.anchor, rogue.anchor+4)); });
                rogue.open(Action.WALL_SEARCH, "Remembered wall search");
                rogue.captureAndCancel("search-context-rogue-rank2-square.png");
                render(() -> owned.get().assertOriginalFiles());
            } finally {
                if (owned.get() != null) owned.get().close();
            }
        }
    }
}
