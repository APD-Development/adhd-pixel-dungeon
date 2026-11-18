package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Op;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Question;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Questions;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.SavedViews;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Query;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdGuide;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdText;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdTopic;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdAsk;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdTextInput;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.TextInput;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdChoices;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdColumns;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdFilter;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.ui.Component;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public class UimsdRestoreDeviceTest {
    private static void onRender(Runnable task) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Game.runOnRenderThread(() -> {
            try { task.run(); } catch (Throwable t) { error.set(t); }
            finally { done.countDown(); }
        });
        assertTrue("render thread timed out", done.await(60, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError(error.get());
    }

    @SuppressWarnings("unchecked")
    private static List<Gizmo> members(Group group) throws Exception {
        Field f = Group.class.getDeclaredField("members"); f.setAccessible(true);
        return new ArrayList<>((List<Gizmo>) f.get(group));
    }

    private static List<Window> windows() {
        try {
            List<Window> result = new ArrayList<>();
            for (Gizmo member : members(Game.scene())) if (member instanceof Window) result.add((Window) member);
            return result;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static String messageText(Window window) {
        assertTrue("UIMSD messages use the scrolling restorable window",
                window instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdText);
        try {
            Field text = window.getClass().getDeclaredField("savedText"); text.setAccessible(true);
            return (String) text.get(window);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static boolean renderedLiteral(Group group, String literal) throws Exception {
        for (Gizmo child : members(group)) {
            if (child instanceof com.watabou.noosa.RenderedText
                    && literal.equals(((com.watabou.noosa.RenderedText) child).text())) return true;
            if (child instanceof Group && renderedLiteral((Group) child, literal)) return true;
        }
        return false;
    }

    private static void assertMessageFits(Window window) {
        try {
            Field width = Window.class.getDeclaredField("width"); width.setAccessible(true);
            Field height = Window.class.getDeclaredField("height"); height.setAccessible(true);
            int w = width.getInt(window), h = height.getInt(window);
            assertTrue("message leaves horizontal screen margins", w <= PixelScene.uiCamera.width - 28);
            assertTrue("long preview scrolls within screen height", h <= PixelScene.uiCamera.height);
            assertContainedText(window, w);
            if (messageText(window).contains("__literal__")) {
                assertTrue("raw underscores remain visible", renderedLiteral(window, "_"));
                assertTrue("raw stars remain visible", renderedLiteral(window, "**"));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void assertContainedText(Group group, int width) throws Exception {
        for (Gizmo child : members(group)) {
            if (child instanceof RenderedTextBlock) {
                RenderedTextBlock text = (RenderedTextBlock) child;
                assertTrue("message text left boundary", text.left() >= 0);
                assertTrue("message text right boundary", text.right() <= width);
            } else if (child instanceof Group) assertContainedText((Group) child, width);
        }
    }

    private static void clearWindows() {
        for (Window window : windows()) window.hide();
    }

    private static UimsdModel model() {
        try {
            Field f = UimsdScene.class.getDeclaredField("model"); f.setAccessible(true);
            return (UimsdModel) f.get(null);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static TextInput input(Window window){
        try {
            Field f = WndTextInput.class.getDeclaredField("textBox"); f.setAccessible(true);
            return (TextInput) f.get(window);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void pickFirst(WndUimsdList list){ pickEntry(list, 0); }

    private static void pickEntry(WndUimsdList list, int entry){
        try {
            java.lang.reflect.Method method = WndUimsdList.class.getDeclaredMethod("picked", int.class);
            method.setAccessible(true); method.invoke(list, entry);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @SuppressWarnings("unchecked")
    private static List<WndUimsdList.Entry> listEntries(WndUimsdList list){
        try {
            Field f = WndUimsdList.class.getDeclaredField("entries"); f.setAccessible(true);
            return (List<WndUimsdList.Entry>) f.get(list);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void pickItem(WndUimsdList list, int item){
        List<WndUimsdList.Entry> entries = listEntries(list);
        for (int i = 0, n = 0; i < entries.size(); i++) {
            if (entries.get(i).isItem() && n++ == item) { pickEntry(list, i); return; }
        }
        throw new AssertionError("Missing picker item " + item);
    }

    private static List<String> itemLabels(WndUimsdList list){
        List<String> labels = new ArrayList<>();
        for (WndUimsdList.Entry entry : listEntries(list)) if (entry.isItem()) labels.add(entry.label);
        return labels;
    }

    private static List<Boolean> itemChecks(WndUimsdList list){
        List<Boolean> checks = new ArrayList<>();
        for (WndUimsdList.Entry entry : listEntries(list)) if (entry.isItem()) checks.add(Boolean.TRUE.equals(entry.checked));
        return checks;
    }

    private static void assertSplitChecks(WndUimsdList list){
        try {
            Field f = WndUimsdList.class.getDeclaredField("labelTexts"); f.setAccessible(true);
            List<?> labels = (List<?>) f.get(list);
            List<WndUimsdList.Entry> entries = listEntries(list);
            int checked = 0, item = 0;
            for (int i = 0; i < entries.size(); i++) {
                WndUimsdList.Entry entry = entries.get(i);
                if (!entry.isItem()) continue;
                boolean expected = item == 1 || item == 2;
                assertEquals("only latest two split fields selected, item " + item, expected, Boolean.TRUE.equals(entry.checked));
                assertTrue("rendered check matches selected field, item " + item,
                        ((RenderedTextBlock) labels.get(i)).text().startsWith(expected ? "[x] " : "[ ] "));
                if (Boolean.TRUE.equals(entry.checked)) checked++;
                item++;
            }
            assertEquals("exactly two split fields selected", 2, checked);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void options(String method, Class<?>[] types, Object... args){
        try {
            java.lang.reflect.Method m = WndUimsdOptions.class.getDeclaredMethod(method, types);
            m.setAccessible(true); m.invoke(null, args);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void awaitScene(Object previous) throws Exception {
        long deadline = System.currentTimeMillis() + 30000;
        while ((Game.scene() == previous || !(Game.scene() instanceof UimsdScene))
                && System.currentTimeMillis() < deadline) Thread.sleep(100);
        assertTrue("UIMSD scene did not rebuild", Game.scene() instanceof UimsdScene && Game.scene() != previous);
        //Restoration follows scene creation in the same render-thread call.
        onRender(() -> {});
    }

    private static void rebuildScene() throws Exception {
        Object previous = Game.scene();
        onRender(ShatteredPixelDungeon::seamlessResetScene);
        awaitScene(previous);
    }

    private static ScrollPane messagePane(Window window) {
        try {
            for (Gizmo child : members(window)) if (child instanceof ScrollPane) return (ScrollPane) child;
            throw new AssertionError("missing message scroll pane");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void answerPrompt(String value) {
        assertEquals(1, windows().size());
        WndUimsdTextInput prompt = (WndUimsdTextInput) windows().get(0);
        prompt.hide();
        prompt.onSelect(true, value);
    }

    private static Object sceneState(String name) {
        try {
            Field field = UimsdScene.class.getDeclaredField(name); field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test public void tourChoicesSurviveWithoutStartingATourOrRepeatingTheOffer() throws Exception {
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            long deadline = System.currentTimeMillis() + 120000;
            while (Game.scene() == null && System.currentTimeMillis() < deadline) Thread.sleep(200);
            assertNotNull("game did not start", Game.scene());
            Object previous = Game.scene();
            onRender(() -> UimsdScene.openNow(TitleScene.class));
            awaitScene(previous);
            String[][] expected = {{"Start the tour", "Later"}, {"My own runs", "Sample data"}};
            for (int choice = 0; choice < expected.length; choice++) {
                final int index = choice;
                AtomicReference<Query> query = new AtomicReference<>();
                AtomicReference<Object> root = new AtomicReference<>();
                java.util.concurrent.atomic.AtomicBoolean seen = new java.util.concurrent.atomic.AtomicBoolean();
                onRender(() -> {
                    clearWindows(); query.set(model().query()); root.set(sceneState("mainRoot"));
                    seen.set(com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdGuideSeen());
                    try {
                        java.lang.reflect.Method show = index == 0
                                ? UimsdScene.class.getDeclaredMethod("offerTour")
                                : UimsdScene.class.getDeclaredMethod("chooseTourData", int.class);
                        show.setAccessible(true);
                        if (index == 0) show.invoke(Game.scene()); else show.invoke(Game.scene(), 3);
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
                for (int reset = 0; reset < 2; reset++) {
                    rebuildScene();
                    onRender(() -> {
                        assertEquals("the choice restores exactly once", 1, windows().size());
                        assertTrue(windows().get(0) instanceof WndUimsdChoices);
                        List<String> labels = new ArrayList<>();
                        for (WndUimsdList.Entry entry : listEntries((WndUimsdList) windows().get(0))) {
                            if (entry.isItem()) labels.add(entry.label);
                        }
                        assertEquals(java.util.Arrays.asList(expected[index]), labels);
                        assertSame(query.get(), model().query());
                        assertSame(root.get(), sceneState("mainRoot"));
                        assertNull("restoring choices must not start a tour", sceneState("tutorial"));
                        assertEquals(false, sceneState("tutorialPreparing"));
                        assertNull("restoring choices must not generate sample history", sceneState("sampleRoot"));
                        assertEquals(seen.get(), com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdGuideSeen());
                    });
                }
                onRender(() -> {
                    if (index == 0) pickItem((WndUimsdList) windows().get(0), 1);
                    else windows().get(0).onBackPressed();
                    assertTrue("Later and Back close without starting the tour", windows().isEmpty());
                    assertNull(sceneState("tutorial")); assertEquals(false, sceneState("tutorialPreparing"));
                });
                rebuildScene();
                onRender(() -> assertTrue("dismissed choices stay dismissed", windows().isEmpty()));
            }
        }
    }

    @Test public void validationEmptyHistoryGroupedAndReportMessagesSurviveWithoutRepeatingActions() throws Exception {
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            long deadline = System.currentTimeMillis() + 120000;
            while (Game.scene() == null && System.currentTimeMillis() < deadline) Thread.sleep(200);
            assertNotNull("game did not start", Game.scene());
            Object previous = Game.scene();
            onRender(() -> UimsdScene.openNow(TitleScene.class));
            awaitScene(previous);
            AtomicReference<ColumnDef> number = new AtomicReference<>();
            onRender(() -> {
                clearWindows();
                model().selectView("runs");
                for (ColumnDef column : model().offeredColumns()) {
                    if (column.type == ColumnDef.Type.NUM && column.format != ColumnDef.Format.DATE_MILLIS) {
                        number.set(column); break;
                    }
                }
                assertNotNull("whole-number filter fixture exists", number.get());
            });
            Runnable[] actions = {
                () -> { WndUimsdFilter.pickValue(model(), -1, null, number.get(), Op.EQ); answerPrompt("not a number"); },
                () -> { WndUimsdFilter.pickValue(model(), -1, null, number.get(), Op.BETWEEN);
                    answerPrompt("1"); answerPrompt("not a number"); },
                () -> { model().setGroup(new com.shatteredpixel.shatteredpixeldungeon.apd.query.GroupBy(
                        java.util.Collections.singletonList(com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.HERO_CLASS),
                        java.util.Collections.emptyList())); WndUimsdColumns.show(model()); },
                () -> WndUimsdColumns.arrange(model()),
                () -> WndUimsdAsk.open(new Catalog(), "rules", (q, back) -> { throw new AssertionError("empty-history warning must not open a table"); }),
                () -> {
                    try {
                        java.lang.reflect.Method tell = com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdReport.class
                                .getDeclaredMethod("tell", String.class);
                        tell.setAccessible(true); tell.invoke(null, "Local export fixture: nothing was sent.");
                    } catch (Exception e) { throw new RuntimeException(e); }
                },
                () -> {
                    try {
                        Class<?> gate = Class.forName("com.shatteredpixel.shatteredpixeldungeon.windows.UimsdGate");
                        java.lang.reflect.Method unlocked = gate.getDeclaredMethod("tourAvailable"); unlocked.setAccessible(true);
                        assertEquals("fresh disposable test history has not earned ten runs", false, unlocked.invoke(null));
                        java.lang.reflect.Method start = UimsdScene.class.getDeclaredMethod("startTour", int.class);
                        start.setAccessible(true); start.invoke(Game.scene(), 0);
                    } catch (Exception e) { throw new RuntimeException(e); }
                }
            };
            String[] expected = {"not a valid value", "not a valid value", "grouped table shows",
                    "fixed layout", "nothing recorded", "nothing was sent", "guided tour opens"};
            for (int i = 0; i < actions.length; i++) {
                final int index = i;
                AtomicReference<Query> query = new AtomicReference<>();
                AtomicReference<String> text = new AtomicReference<>();
                onRender(() -> { clearWindows(); actions[index].run(); });
                //Report callbacks are posted separately to the render thread.
                onRender(() -> {
                    assertEquals("one warning/status window", 1, windows().size());
                    text.set(messageText(windows().get(0)));
                    assertTrue(text.get(), text.get().contains(expected[index]));
                    query.set(model().query());
                });
                rebuildScene();
                onRender(() -> {
                    assertEquals("warning restores exactly once", 1, windows().size());
                    assertEquals(text.get(), messageText(windows().get(0)));
                    assertSame("restoring a warning never repeats the query action", query.get(), model().query());
                    windows().get(0).onBackPressed();
                    assertTrue("Back closes the restored warning", windows().isEmpty());
                });
            }
            onRender(() -> { model().setGroup(null); clearWindows(); });
        }
    }

    @Test public void textAndGuideScrollSurviveRepeatedResetsAndClampAfterResize() throws Exception {
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            long deadline = System.currentTimeMillis() + 120000;
            while (Game.scene() == null && System.currentTimeMillis() < deadline) Thread.sleep(200);
            assertNotNull("game did not start", Game.scene());
            Object previous = Game.scene();
            onRender(() -> UimsdScene.openNow(TitleScene.class));
            awaitScene(previous);
            AtomicInteger showActions = new AtomicInteger();
            AtomicReference<Query> unchangedQuery = new AtomicReference<>();
            onRender(() -> {
                clearWindows();
                unchangedQuery.set(model().query());
                StringBuilder longText = new StringBuilder();
                for (int i = 0; i < 100; i++) longText.append("Recorded local detail ").append(i)
                        .append(": this complete line stays in the report after returning to the game.\n");
                Game.scene().addToFront(new WndUimsdText("Local preview", longText.toString(), false));
                try {
                    java.lang.reflect.Constructor<UimsdGuide.Topic> constructor = UimsdGuide.Topic.class
                            .getDeclaredConstructor(String.class, String.class, String.class, String.class, String.class);
                    constructor.setAccessible(true);
                    UimsdGuide.Topic topic = constructor.newInstance("scroll-fixture", "Testing", "Guide", longText.toString(), "");
                    Game.scene().addToFront(new WndUimsdTopic(topic, true, showActions::incrementAndGet));
                } catch (Exception e) { throw new RuntimeException(e); }
                messagePane(windows().get(0)).scrollTo(0, 37);
                messagePane(windows().get(1)).scrollTo(0, 61);
                assertEquals(37f, messagePane(windows().get(0)).content().camera.scroll.y, 0.01f);
                assertEquals(61f, messagePane(windows().get(1)).content().camera.scroll.y, 0.01f);
            });
            for (int reset = 0; reset < 2; reset++) {
                rebuildScene();
                onRender(() -> {
                    assertEquals("both reading windows restore exactly once", 2, windows().size());
                    assertTrue(windows().get(0) instanceof WndUimsdText);
                    assertTrue(windows().get(1) instanceof WndUimsdTopic);
                    assertEquals("preview keeps its reading position", 37f,
                            messagePane(windows().get(0)).content().camera.scroll.y, 0.01f);
                    assertEquals("guide keeps its reading position", 61f,
                            messagePane(windows().get(1)).content().camera.scroll.y, 0.01f);
                    assertEquals("restoration never runs Show me", 0, showActions.get());
                    assertSame("restoration never changes a query", unchangedQuery.get(), model().query());
                });
            }
            onRender(() -> {
                //Snapshot positions before destroying the original panes, as PixelScene does.
                List<Runnable> restorers = new ArrayList<>();
                for (Window window : windows()) {
                    messagePane(window).scrollTo(0, Float.MAX_VALUE);
                    assertTrue("fixture reaches a nonzero bottom position", messagePane(window).content().camera.scroll.y > 61);
                    restorers.add(window.restoreOnSceneReset());
                }
                clearWindows();
                int originalHeight = PixelScene.uiCamera.height;
                try {
                    //A taller viewport fits the whole fixture: stale offsets must be clamped to zero.
                    PixelScene.uiCamera.height = 10000;
                    for (Runnable restorer : restorers) restorer.run();
                    assertEquals(2, windows().size());
                    for (Window window : windows()) assertEquals("resize never leaves content off screen", 0f,
                            messagePane(window).content().camera.scroll.y, 0.01f);
                    assertEquals(0, showActions.get());
                } finally { clearWindows(); PixelScene.uiCamera.height = originalHeight; }
            });
        }
    }

    @Test public void privacySubflowsRestoreExactLocalStateWithoutQueueingOrSending() throws Exception {
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            long deadline = System.currentTimeMillis() + 120000;
            while (Game.scene() == null && System.currentTimeMillis() < deadline) Thread.sleep(200);
            assertNotNull("game did not start", Game.scene());
            Object previous = Game.scene();
            onRender(() -> UimsdScene.openNow(TitleScene.class));
            awaitScene(previous);

            java.io.File cache = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .getTargetContext().getCacheDir();
            java.io.File container = java.nio.file.Files.createTempDirectory(cache.toPath(), "privacy-restore-").toFile();
            java.io.File fixture = new java.io.File(container, "history");
            assertTrue(fixture.mkdir());
            try {
                UimsdModel fixtureModel = new UimsdModel("fixture-rules", "Fixture rules");
                Field root = UimsdModel.class.getDeclaredField("root");
                root.setAccessible(true);
                root.set(fixtureModel, fixture);

                long now = System.currentTimeMillis();
                com.shatteredpixel.shatteredpixeldungeon.apd.backend.EvidenceMessages.Asked asked =
                        new com.shatteredpixel.shatteredpixeldungeon.apd.backend.EvidenceMessages.Asked(
                                new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Evidence.Request(
                                        com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id.of(
                                                com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id.Kind.SUBMISSION,
                                                "sub_restore_fixture"),
                                        com.shatteredpixel.shatteredpixeldungeon.apd.backend.Evidence.Kind.FULL_RUN_LOG,
                                        now + 3600000L),
                                "restore-fixture-run");
                assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.EvidenceInbox.add(
                        fixture, asked, now));

                java.util.Set<String> voteOptions = new java.util.TreeSet<>(
                        java.util.Arrays.asList("Keep exact state", "Queue nothing", "Restore checks"));
                com.shatteredpixel.shatteredpixeldungeon.apd.backend.VoteMessages.Offer offer =
                        new com.shatteredpixel.shatteredpixeldungeon.apd.backend.VoteMessages.Offer(
                                new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Voting.Round(
                                        com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id.of(
                                                com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id.Kind.VOTE_ROUND,
                                                "vr_restore_fixture"),
                                        voteOptions, 2, now - 1000L, now + 3600000L),
                                "Restoration fixture vote");
                assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.VoteActions.store(
                        fixture, offer));

                java.io.File evidenceFile =
                        com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.EvidenceInbox.fileIn(fixture);
                java.io.File votesFile = new java.io.File(fixture,
                        com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.VoteActions.FILE_NAME);
                byte[] evidenceBefore = java.nio.file.Files.readAllBytes(evidenceFile.toPath());
                byte[] votesBefore = java.nio.file.Files.readAllBytes(votesFile.toPath());

                onRender(() -> {
                    clearWindows();
                    options("evidenceRequests", new Class<?>[]{UimsdModel.class}, fixtureModel);
                    assertEquals("local evidence list opened once", 1, windows().size());
                    assertEquals(java.util.Collections.singletonList("Run restore-fixture-run"),
                            itemLabels((WndUimsdList) windows().get(0)));
                });
                rebuildScene();
                onRender(() -> {
                    assertEquals("evidence list restored exactly once", 1, windows().size());
                    assertEquals(java.util.Collections.singletonList("Run restore-fixture-run"),
                            itemLabels((WndUimsdList) windows().get(0)));
                    pickItem((WndUimsdList) windows().get(0), 0);
                    assertEquals("evidence decision opened once", 1, windows().size());
                    assertEquals(java.util.Arrays.asList("Agree", "Decline", "Decide later"),
                            itemLabels((WndUimsdList) windows().get(0)));
                });
                for (int reset = 0; reset < 2; reset++) {
                    rebuildScene();
                    onRender(() -> {
                        assertEquals("evidence decision restored exactly once", 1, windows().size());
                        assertEquals(java.util.Arrays.asList("Agree", "Decline", "Decide later"),
                                itemLabels((WndUimsdList) windows().get(0)));
                        assertEquals("restoration never answers evidence", 1,
                                com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.EvidenceInbox.pending(fixture));
                        assertEquals("restoration never queues evidence", 0,
                                com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.SubmissionQueue.pending(fixture));
                    });
                }
                onRender(() -> {
                    windows().get(0).onBackPressed();
                    assertTrue("Back leaves the evidence request unanswered", windows().isEmpty());
                });
                assertArrayEquals("evidence fixture bytes stay exact", evidenceBefore,
                        java.nio.file.Files.readAllBytes(evidenceFile.toPath()));

                onRender(() -> {
                    options("communityVotes", new Class<?>[]{UimsdModel.class}, fixtureModel);
                    assertEquals("local vote list opened once", 1, windows().size());
                    assertEquals(java.util.Collections.singletonList("Restoration fixture vote"),
                            itemLabels((WndUimsdList) windows().get(0)));
                });
                rebuildScene();
                onRender(() -> {
                    assertEquals("vote offer list restored exactly once", 1, windows().size());
                    assertEquals(java.util.Collections.singletonList("Restoration fixture vote"),
                            itemLabels((WndUimsdList) windows().get(0)));
                    pickItem((WndUimsdList) windows().get(0), 0);
                    assertEquals(java.util.Arrays.asList("Keep exact state", "Queue nothing", "Restore checks"),
                            itemLabels((WndUimsdList) windows().get(0)));
                    pickItem((WndUimsdList) windows().get(0), 0);
                    pickItem((WndUimsdList) windows().get(0), 2);
                    assertEquals(java.util.Arrays.asList(true, false, true),
                            itemChecks((WndUimsdList) windows().get(0)));
                });
                for (int reset = 0; reset < 2; reset++) {
                    rebuildScene();
                    onRender(() -> {
                        assertEquals("in-progress vote restored exactly once", 1, windows().size());
                        assertEquals(java.util.Arrays.asList("Keep exact state", "Queue nothing", "Restore checks"),
                                itemLabels((WndUimsdList) windows().get(0)));
                        assertEquals("the exact two vote ticks survive", java.util.Arrays.asList(true, false, true),
                                itemChecks((WndUimsdList) windows().get(0)));
                        assertEquals("restoration never queues a ballot", 0,
                                com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.SubmissionQueue.pending(fixture));
                    });
                }
                onRender(() -> {
                    windows().get(0).onBackPressed();
                    assertTrue("Back leaves without voting", windows().isEmpty());
                });
                assertArrayEquals("stored vote offer bytes stay exact", votesBefore,
                        java.nio.file.Files.readAllBytes(votesFile.toPath()));

                onRender(() -> {
                    options("accountRequest", new Class<?>[]{
                                    UimsdModel.class,
                                    com.shatteredpixel.shatteredpixeldungeon.apd.backend.AccountRequests.Kind.class},
                            fixtureModel,
                            com.shatteredpixel.shatteredpixeldungeon.apd.backend.AccountRequests.Kind.EXPORT);
                    assertEquals("account request confirmation opened once", 1, windows().size());
                    assertEquals(java.util.Arrays.asList("Queue the request", "Cancel"),
                            itemLabels((WndUimsdList) windows().get(0)));
                });
                rebuildScene();
                onRender(() -> {
                    assertEquals("account confirmation restored exactly once", 1, windows().size());
                    assertEquals(java.util.Arrays.asList("Queue the request", "Cancel"),
                            itemLabels((WndUimsdList) windows().get(0)));
                    assertEquals("restoration never queues an account request", 0,
                            com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.SubmissionQueue.pending(fixture));
                    assertFalse("restoration never creates a session",
                            new java.io.File(fixture,
                                    com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.SessionStore.FILE_NAME).exists());
                    windows().get(0).onBackPressed();
                    assertTrue("Back cancels the account confirmation", windows().isEmpty());
                });

                assertArrayEquals(evidenceBefore, java.nio.file.Files.readAllBytes(evidenceFile.toPath()));
                assertArrayEquals(votesBefore, java.nio.file.Files.readAllBytes(votesFile.toPath()));
                assertEquals("all restoration paths leave the local queue empty", 0,
                        com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.SubmissionQueue.pending(fixture));
            } finally {
                onRender(() -> clearWindows());
                java.io.File[] created = fixture.listFiles();
                if (created != null) for (java.io.File file : created) assertTrue(file.delete());
                assertTrue(fixture.delete());
                assertTrue(container.delete());
            }
        }
    }

    @Test public void menusSurviveSceneRecreationAndNarrowActionsStayInsideTheirWindow() throws Exception {
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            long deadline = System.currentTimeMillis() + 120000;
            while (Game.scene() == null && System.currentTimeMillis() < deadline) Thread.sleep(200);
            assertNotNull("game did not start", Game.scene());
            Object previous = Game.scene();
            //The test enters directly; it does not change production unlock flags.
            onRender(() -> UimsdScene.openNow(TitleScene.class));
            awaitScene(previous);
            onRender(() -> {
                clearWindows();
                try {
                    java.lang.reflect.Method pickView = UimsdScene.class.getDeclaredMethod("pickView");
                    pickView.setAccessible(true);
                    pickView.invoke(Game.scene());
                } catch (Exception e) { throw new RuntimeException(e); }
                assertEquals("Tables picker opened once", 1, windows().size());
                assertTrue(windows().get(0) instanceof WndUimsdList);
                assertNotNull("Tables picker supplies an exact restorer", windows().get(0).restoreOnSceneReset());
            });
            for (int reset = 0; reset < 2; reset++) {
                rebuildScene();
                onRender(() -> {
                    assertEquals("Tables picker restored exactly once", 1, windows().size());
                    assertTrue(windows().get(0) instanceof WndUimsdList);
                    assertNotNull(windows().get(0).restoreOnSceneReset());
                });
            }
            onRender(() -> {
                pickItem((WndUimsdList) windows().get(0), 0);
                assertTrue("restored Tables callback closes only its picker", windows().isEmpty());
                assertNotNull("restored Tables callback keeps a selected view", model().viewId());
            });

            final AtomicReference<List<String>> searchLabels = new AtomicReference<>();
            final AtomicReference<Query> searchQuery = new AtomicReference<>();
            onRender(() -> {
                clearWindows();
                model().selectView("runs");
                searchQuery.set(model().query());
                try {
                    java.lang.reflect.Method openSearch = UimsdScene.class.getDeclaredMethod("openSearch");
                    openSearch.setAccessible(true);
                    openSearch.invoke(Game.scene());
                } catch (Exception e) { throw new RuntimeException(e); }
                assertEquals("Search prompt opened once", 1, windows().size());
                assertTrue(windows().get(0) instanceof WndTextInput);
                WndTextInput prompt = (WndTextInput) windows().get(0);
                prompt.onSelect(true, "run");
                prompt.hide();
                assertEquals("Search results opened once", 1, windows().size());
                assertTrue(windows().get(0) instanceof WndUimsdList);
                assertNotNull("Search results supply an exact restorer", windows().get(0).restoreOnSceneReset());
                List<String> labels = new ArrayList<>();
                for (WndUimsdList.Entry entry : listEntries((WndUimsdList) windows().get(0))) {
                    if (entry.isItem()) labels.add(entry.label);
                }
                assertFalse("fixture query returns visible results", labels.isEmpty());
                searchLabels.set(labels);
            });
            for (int reset = 0; reset < 2; reset++) {
                rebuildScene();
                onRender(() -> {
                    assertEquals("Search results restored exactly once", 1, windows().size());
                    assertTrue(windows().get(0) instanceof WndUimsdList);
                    assertNotNull(windows().get(0).restoreOnSceneReset());
                    List<String> labels = new ArrayList<>();
                    for (WndUimsdList.Entry entry : listEntries((WndUimsdList) windows().get(0))) {
                        if (entry.isItem()) labels.add(entry.label);
                    }
                    assertEquals("restored results retain the exact query ordering", searchLabels.get(), labels);
                    assertSame("restoring search results never changes the active query",
                            searchQuery.get(), model().query());
                });
            }
            onRender(() -> {
                try {
                    Field dirty = UimsdScene.class.getDeclaredField("dirty");
                    dirty.setAccessible(true);
                    dirty.setBoolean(Game.scene(), false);
                    pickItem((WndUimsdList) windows().get(0), 0);
                    assertTrue("restored Search callback closes only its picker", windows().isEmpty());
                    assertTrue("restored Search callback targets the rebuilt active scene",
                            dirty.getBoolean(Game.scene()));
                } catch (Exception e) { throw new RuntimeException(e); }
            });

            onRender(() -> {
                clearWindows();
                Game.scene().addToFront(new WndSettings());
            });
            rebuildScene();
            Object settingsScene = Game.scene();
            onRender(() -> {
                assertEquals("existing zero-argument fallback survived", 1, windows().size());
                assertTrue(windows().get(0) instanceof WndSettings);
                clearWindows();
            });
            //Ordinary Settings close deliberately stays in this UIMSD scene. Only the
            //specific language/system-font controls request their own reset.
            onRender(() -> {
                assertSame("ordinary Settings close must not rebuild UIMSD", settingsScene, Game.scene());
                assertTrue("ordinary Settings close leaves no stale window", windows().isEmpty());
            });
            onRender(() -> {
                UimsdModel model = model(); assertNotNull(model);
                model.selectView("runs");
                WndUimsdOptions.show(model);
                assertEquals(1, windows().size());
                assertNotNull(windows().get(0).restoreOnSceneReset());
            });
            rebuildScene();
            onRender(() -> {
                assertEquals("Options survived exactly once", 1, windows().size());
                assertTrue(windows().get(0) instanceof WndUimsdList);
                clearWindows();
                WndUimsdColumns.show(model());
            });
            rebuildScene();
            final AtomicReference<List<String>> previousColumns = new AtomicReference<>();
            final AtomicReference<List<String>> arrangedColumns = new AtomicReference<>();
            onRender(() -> {
                assertEquals("Columns survived exactly once", 1, windows().size());
                clearWindows();
                previousColumns.set(new ArrayList<>(model().query().columns));
                arrangedColumns.set(new ArrayList<>(previousColumns.get().isEmpty()
                        ? model().dataset().def.defaultColumnIds() : previousColumns.get()));
                try {
                    String onlyColumn = model().offeredColumns().get(0).id;
                    model().setColumns(java.util.Collections.singletonList(onlyColumn));
                    WndUimsdColumns.show(model());
                    WndUimsdList picker = (WndUimsdList) windows().get(0);
                    pickItem(picker, 0);
                    assertEquals(java.util.Collections.singletonList(onlyColumn), model().query().columns);
                    int index = 0;
                    while (!listEntries(picker).get(index).isItem()) index++;
                    assertEquals("last column stays checked after refused removal", Boolean.TRUE, listEntries(picker).get(index).checked);
                    try {
                        Field labels = WndUimsdList.class.getDeclaredField("labelTexts"); labels.setAccessible(true);
                        RenderedTextBlock label = (RenderedTextBlock) ((List<?>) labels.get(picker)).get(index);
                        assertTrue("rendered last-column check stays on", label.text().startsWith("[x] "));
                    } catch (Exception e) { throw new RuntimeException(e); }
                } finally {
                    clearWindows(); model().setColumns(arrangedColumns.get());
                }
                assertTrue("column-order fixture has at least two columns", arrangedColumns.get().size() >= 2);
                WndUimsdColumns.arrange(model());
            });
            rebuildScene();
            onRender(() -> {
                assertEquals("Column order survived exactly once", 1, windows().size());
                assertTrue(windows().get(0) instanceof WndUimsdList);
                assertNotNull("Column order provides an exact restorer", windows().get(0).restoreOnSceneReset());
                pickItem((WndUimsdList) windows().get(0), 1);
                assertEquals("choosing a column opens one move menu", 1, windows().size());
            });
            rebuildScene();
            onRender(() -> {
                assertEquals("Move column survived exactly once", 1, windows().size());
                assertTrue(windows().get(0) instanceof WndUimsdList);
                assertNotNull("Move column provides an exact restorer", windows().get(0).restoreOnSceneReset());
                pickItem((WndUimsdList) windows().get(0), 2);
                assertEquals("restored move callback returns to Column order", 1, windows().size());
                assertEquals("restored Make first moves the chosen column",
                        arrangedColumns.get().get(1), model().query().columns.get(0));
                clearWindows();
                model().setColumns(previousColumns.get());
                options("pickGroup", new Class<?>[]{UimsdModel.class}, model());
            });
            rebuildScene();
            onRender(() -> {
                assertEquals("grouping menu survived", 1, windows().size());
                pickFirst((WndUimsdList) windows().get(0));
                assertNull("restored grouping callback works", model().query().group);
                clearWindows();
                SavedViews views = new SavedViews();
                views.save("Restoration fixture", model().query(), 1);
                options("viewActions", new Class<?>[]{UimsdModel.class, SavedViews.View.class},
                        model(), views.get("Restoration fixture"));
            });
            Query savedQuery = model().query();
            rebuildScene();
            onRender(() -> {
                assertEquals("saved-view actions survived", 1, windows().size());
                assertSame("restoring does not apply a saved view", savedQuery, model().query());
                pickFirst((WndUimsdList) windows().get(0));
                assertEquals("restored Open callback works", savedQuery.dataset, model().query().dataset);
                clearWindows();
            });
            //Status/preview windows used to fall through the zero-argument restorer:
            //WndMessage(String) has no such constructor, so they silently disappeared.
            final AtomicReference<String> statusText = new AtomicReference<>();
            final AtomicReference<Query> statusQuery = new AtomicReference<>();
            onRender(() -> {
                statusQuery.set(model().query());
                options("privacySummary", new Class<?>[]{UimsdModel.class}, model());
                assertEquals(1, windows().size());
                statusText.set(messageText(windows().get(0)));
                assertFalse("privacy summary is present", statusText.get().isEmpty());
            });
            rebuildScene();
            onRender(() -> {
                assertEquals("privacy summary survives exactly once", 1, windows().size());
                assertEquals(statusText.get(), messageText(windows().get(0)));
                assertSame("restoring a status never changes the query", statusQuery.get(), model().query());
                clearWindows();
                StringBuilder preview = new StringBuilder("Queued on this device. Nothing was sent: there is no server yet.\n\n");
                preview.append("run_id: ").append(new String(new char[100]).replace('\0', 'W')).append("\n__literal__ **literal**\n");
                for (int i = 0; i < 40; i++) preview.append("Run preview field ").append(i).append(": recorded on this device.\n");
                statusText.set(preview.toString());
                int originalWidth = PixelScene.uiCamera.width;
                try {
                    PixelScene.uiCamera.width = 100;
                    options("say", new Class<?>[]{String.class}, statusText.get());
                    assertMessageFits(windows().get(0));
                } finally { PixelScene.uiCamera.width = originalWidth; }
            });
            //Two resets catch duplicate restoration and loss of the restored payload.
            rebuildScene();
            rebuildScene();
            onRender(() -> {
                assertEquals("long local preview survives exactly once", 1, windows().size());
                assertEquals("entire preview retained", statusText.get(), messageText(windows().get(0)));
                assertSame("preview restore never changes the query", statusQuery.get(), model().query());
                assertMessageFits(windows().get(0));
                clearWindows();
            });
            //A separate marked demo fixture: no player history or real demo records.
            java.io.File cache = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .getTargetContext().getCacheDir();
            java.io.File container = java.nio.file.Files.createTempDirectory(cache.toPath(), "dialog-check-").toFile();
            java.io.File fixture = new java.io.File(container, "history");
            assertTrue(fixture.mkdir());
            java.io.File demo = DemoData.demoRootFor(fixture);
            assertTrue(demo.mkdirs());
            java.io.File marker = new java.io.File(demo, DemoData.MARKER_FILE);
            assertTrue(marker.createNewFile());
            try {
                onRender(() -> options("deleteDemoData", new Class<?>[]{java.io.File.class}, fixture));
                rebuildScene();
                onRender(() -> {
                    assertEquals("demo deletion confirmation survived", 1, windows().size());
                    assertTrue("restoration never confirms deletion", marker.isFile());
                    windows().get(0).onBackPressed();
                    assertTrue("Back never confirms deletion", marker.isFile());
                });
            } finally {
                assertTrue(marker.delete()); assertTrue(demo.delete());
                assertTrue(fixture.delete()); assertTrue(container.delete());
            }
            onRender(() -> {
                WndUimsdFilter.add(model());
                WndUimsdFilter.pickOp(model(), -1, null, model().offeredColumns().get(0));
                assertEquals(2, windows().size());
            });
            rebuildScene();
            onRender(() -> {
                assertEquals("both filter steps survived", 2, windows().size());
                windows().get(1).onBackPressed();
                assertEquals("Back returns to the preceding filter step", 1, windows().size());
                clearWindows();
                WndUimsdAsk.builder(model().catalog(), model().currentRuleset(), (q, back) -> {}, Question.about("runs"));
            });
            rebuildScene();
            final AtomicReference<String> answer = new AtomicReference<>();
            onRender(() -> {
                assertEquals("Ask builder survived", 1, windows().size());
                clearWindows();
                WndUimsdTextInput prompt = new WndUimsdTextInput("Filter", "A whole number", "123", 40,
                        false, "OK", "Cancel", (positive, value) -> { if (positive) answer.set(value); });
                Game.scene().addToFront(prompt);
                input(prompt).setText("321");
            });
            rebuildScene();
            onRender(() -> {
                assertEquals("typed input survived", 1, windows().size());
                WndUimsdTextInput prompt = (WndUimsdTextInput) windows().get(0);
                assertEquals("321", input(prompt).getText());
                prompt.onSelect(true, input(prompt).getText());
                assertEquals("restored callback works", "321", answer.get());
                clearWindows();
                ColumnDef outcome = null;
                for (ColumnDef column : model().offeredColumns()) if (column.type == ColumnDef.Type.ENUM) { outcome = column; break; }
                assertNotNull("enum fixture", outcome);
                WndUimsdFilter.pickValue(model(), -1, null, outcome, Op.IN);
                pickFirst((WndUimsdList) windows().get(0));
            });
            rebuildScene();
            onRender(() -> {
                assertEquals("pending multi-select survived", 1, windows().size());
                try {
                    Field entries = WndUimsdList.class.getDeclaredField("entries"); entries.setAccessible(true);
                    List<?> saved = (List<?>) entries.get(windows().get(0));
                    assertEquals(Boolean.TRUE, ((WndUimsdList.Entry) saved.get(0)).checked);
                    java.lang.reflect.Method done = windows().get(0).getClass().getDeclaredMethod("onDone");
                    done.setAccessible(true); done.invoke(windows().get(0));
                    assertEquals("pending choice applies after restoration", 1, model().query().filters.size());
                } catch (Exception e) { throw new RuntimeException(e); }
                clearWindows();
                try {
                    java.lang.reflect.Method split = WndUimsdAsk.class.getDeclaredMethod("pickSplit",
                            Catalog.class, String.class, WndUimsdAsk.Host.class, Question.class, DatasetDef.class);
                    split.setAccessible(true);
                    Question question = Question.about("runs");
                    DatasetDef def = model().catalog().dataset("runs").def;
                    assertTrue("three groupable split fields exist", Questions.groupable(def, false).size() >= 3);
                    split.invoke(null, model().catalog(), model().currentRuleset(),
                            (WndUimsdAsk.Host) (q, back) -> {}, question, def);
                    WndUimsdList picker = (WndUimsdList) windows().get(0);
                    pickItem(picker, 0); pickItem(picker, 1); pickItem(picker, 2);
                    assertSplitChecks(picker);
                    assertTrue("pending choices do not modify original question", question.groupBy.isEmpty());
                    picker.restoreOnSceneReset().run();
                    picker.hide();
                    WndUimsdList restored = (WndUimsdList) windows().get(0);
                    assertSplitChecks(restored);
                    java.lang.reflect.Method done = restored.getClass().getDeclaredMethod("onDone");
                    done.setAccessible(true); done.invoke(restored);
                    Field last = WndUimsdAsk.class.getDeclaredField("last"); last.setAccessible(true);
                    List<ColumnDef> cols = Questions.groupable(def, false);
                    assertEquals(java.util.Arrays.asList(cols.get(1).id, cols.get(2).id), ((Question) last.get(null)).groupBy);
                    clearWindows();
                } catch (Exception e) { throw new RuntimeException(e); }
                int originalWidth = PixelScene.uiCamera.width;
                try {
                    Class<?> annotationType = Class.forName("com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene$APDAnnotation");
                    java.lang.reflect.Constructor<?> constructor = annotationType.getDeclaredConstructor(String.class, float.class);
                    constructor.setAccessible(true);
                    java.lang.reflect.Method place = annotationType.getDeclaredMethod("place", float.class, float.class, float.class, float.class);
                    place.setAccessible(true);
                    Field lines = annotationType.getDeclaredField("lines"); lines.setAccessible(true);
                    Field arrow = annotationType.getDeclaredField("body"); arrow.setAccessible(true);
                    Class<?> gate = Class.forName("com.shatteredpixel.shatteredpixeldungeon.windows.UimsdGate");
                    for (String field : new String[]{"LOCKED_ARROW_TEXT", "TENTH_RUN_ARROW_CLEAN", "TENTH_RUN_ARROW_ADULT"}) {
                        Field message = gate.getDeclaredField(field); message.setAccessible(true);
                        String full = (String) message.get(null);
                        for (int available : new int[]{72, 89, 120, 150}) {
                            PixelScene.uiCamera.width = available + 8;
                            Component annotation = (Component) constructor.newInstance(full, (float) available);
                            try {
                                place.invoke(annotation, 4f, (float) available, (available + 3) / 2f, 160f);
                                StringBuilder shown = new StringBuilder();
                                for (com.watabou.noosa.BitmapText line : (com.watabou.noosa.BitmapText[]) lines.get(annotation)) {
                                    assertTrue("annotation left boundary", line.x >= 4);
                                    assertTrue("annotation right boundary", line.x + line.width() <= available + 4);
                                    if (shown.length() > 0) shown.append(' ');
                                    shown.append(line.text());
                                }
                                assertEquals("entire instruction remains readable", full, shown.toString());
                                com.watabou.noosa.ColorBlock shaft = ((com.watabou.noosa.ColorBlock[]) arrow.get(annotation))[0];
                                float centre = shaft.x + shaft.width() / 2;
                                assertTrue("arrow over right/UIMSD column", centre >= 4 + (available + 3) / 2f && centre <= available + 4);
                            } finally { annotation.destroy(); }
                        }
                    }
                    PixelScene.uiCamera.width = 100;
                    WndUimsdChoices choices = new WndUimsdChoices("Queue", "Nothing is sent.",
                            "Queue for submission (preview, sends nothing)", "Show what is queued for this run");
                    Game.scene().addToFront(choices);
                    Field width = Window.class.getDeclaredField("width"); width.setAccessible(true);
                    int innerWidth = width.getInt(choices);
                    assertTrue("window leaves side margins", innerWidth <= 72);
                    Field labels = WndUimsdList.class.getDeclaredField("labelTexts"); labels.setAccessible(true);
                    int checkedActions = 0;
                    for (Object item : (List<?>) labels.get(choices)) {
                        //Notes/headings have no action label in this parallel list.
                        if (item == null) continue;
                        RenderedTextBlock label = (RenderedTextBlock) item;
                        assertTrue("label left edge", label.left() >= 0);
                        assertTrue("wrapped label right edge", label.right() <= innerWidth);
                        checkedActions++;
                    }
                    assertEquals("both long actions were checked", 2, checkedActions);
                    clearWindows();
                    WndUimsdTextInput prompt = new WndUimsdTextInput("Filter", "A number", "", 40,
                            false, "OK", "Cancel", (positive, value) -> {});
                    Game.scene().addToFront(prompt);
                    innerWidth = width.getInt(prompt);
                    assertTrue("input leaves side margins", innerWidth <= 72);
                    Field buttonText = StyledButton.class.getDeclaredField("text"); buttonText.setAccessible(true);
                    for (Gizmo child : members(prompt)) if (child instanceof RedButton){
                        RedButton button = (RedButton) child;
                        RenderedTextBlock label = (RenderedTextBlock) buttonText.get(button);
                        if (!label.text().isEmpty()) {
                            assertTrue("input action left edge", label.left() >= button.left());
                            assertTrue("input action right edge", label.right() <= button.right());
                        }
                    }
                } catch (Exception e) { throw new RuntimeException(e); }
                finally { PixelScene.uiCamera.width = originalWidth; clearWindows(); }
            });
        }
    }

    @Test public void arcanaItemDescriptionsRequireKnownEquippedRingsAndFitWindows() throws Exception {
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            long deadline = System.currentTimeMillis() + 120000;
            while (Game.scene() == null && System.currentTimeMillis() < deadline) Thread.sleep(200);
            assertNotNull("game did not start", Game.scene());
            Object previousScene = Game.scene();
            onRender(() -> UimsdScene.openNow(TitleScene.class));
            awaitScene(previousScene);
            onRender(() -> {
                com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero previousHero =
                        com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;
                try {
                    clearWindows();
                    com.shatteredpixel.shatteredpixeldungeon.journal.Notes.reset();
                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero =
                            new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
                    com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero = hero;
                    com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword =
                            new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();
                    sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing());
                    sword.levelKnown = true;
                    com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor armor =
                            new com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor();
                    armor.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow());
                    armor.levelKnown = true;
                    assertFalse(sword.info().contains("Current activation chance: 39.167%"));
                    com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana ring =
                            new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana();
                    ring.anonymize(); ring.levelKnown = true; ring.cursedKnown = true;
                    hero.belongings.ring = ring;
                    assertTrue(sword.info().contains("Current activation chance: 39.167%"));
                    assertTrue(armor.info().contains("movement rate is"));
                    for (com.shatteredpixel.shatteredpixeldungeon.items.Item item :
                            new com.shatteredpixel.shatteredpixeldungeon.items.Item[]{sword, armor}) {
                        com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem window =
                                new com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem(item);
                        Game.scene().addToFront(window);
                        Field width = Window.class.getDeclaredField("width"); width.setAccessible(true);
                        assertContainedText(window, width.getInt(window));
                        window.hide();
                    }
                    hero.heroClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.CLERIC;
                    hero.armorAbility=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity();
                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.initClassTalents(hero);
                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.initArmorTalents(hero);
                    hero.talents.get(3).put(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.SPIRIT_FORM,2);
                    ring.level(2);ring.cursed=true;
                    com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana spiritRing=new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana();
                    spiritRing.level(10);
                    com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(hero,
                        com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.SpiritFormBuff.class).setEffect(spiritRing);
                    assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.ArcanaInfoDisplay.weapon(sword).contains("62.22% increase"));
                    assertEquals("Arcana preview retains cached spirit item",10,spiritRing.level());
                    for(com.shatteredpixel.shatteredpixeldungeon.items.Item item:
                            new com.shatteredpixel.shatteredpixeldungeon.items.Item[]{sword,armor}) {
                        com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem window=new com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem(item);
                        Game.scene().addToFront(window);Field width=Window.class.getDeclaredField("width");width.setAccessible(true);
                        assertContainedText(window,width.getInt(window));window.hide();
                    }
                    ring.levelKnown = false;
                    assertFalse(sword.info().contains("Current activation chance: 39.167%"));
                    assertFalse(armor.info().contains("movement rate is"));
                    ring.levelKnown = true; hero.belongings.ring = null;
                    assertFalse(sword.info().contains("Current activation chance: 39.167%"));
                } catch (Exception e) { throw new RuntimeException(e); }
                finally {
                    clearWindows();
                    com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero = previousHero;
                }
            });
        }
    }

    @Test public void demoUnlockKeepsTenRunNoticeVisibleOnTitle() throws Exception {
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            long deadline = System.currentTimeMillis() + 120000;
            while (Game.scene() == null && System.currentTimeMillis() < deadline) Thread.sleep(200);
            assertNotNull("game did not start", Game.scene());
            final boolean[] saved = new boolean[5];
            final Object[] previous = new Object[1];
            onRender(() -> {
                saved[0] = com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdUnlocked();
                saved[1] = com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdDemoUnlockUsed();
                saved[2] = com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdTourUnlocked();
                saved[3] = com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdTenthPending();
                saved[4] = com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdTenthShown();
            });
            try {
                onRender(() -> {
                    assertTrue("test requires fewer than ten genuine runs; never delete history",
                        com.shatteredpixel.shatteredpixeldungeon.apd.demo.UimsdUnlock.genuineRuns(
                            com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder.historyRoot(), 10) < 10);
                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdUnlocked(true);
                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdDemoUnlockUsed(true);
                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdTourUnlocked(false);
                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdTenthPending(false);
                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdTenthShown(false);
                    com.shatteredpixel.shatteredpixeldungeon.windows.UimsdGate.forceGateOnForTest(true);
                    previous[0] = Game.scene();
                    ShatteredPixelDungeon.switchNoFade(TitleScene.class);
                });
                deadline = System.currentTimeMillis() + 30000;
                while ((Game.scene() == previous[0] || !(Game.scene() instanceof TitleScene)) && System.currentTimeMillis() < deadline) Thread.sleep(100);
                onRender(() -> {
                    assertTrue(Game.scene() instanceof TitleScene && Game.scene() != previous[0]);
                    try {
                        Field field = TitleScene.class.getDeclaredField("annotation"); field.setAccessible(true);
                        Component annotation = (Component) field.get(Game.scene());
                        assertTrue("instruction is actually visible on the main menu", annotation.visible);
                        Field lines = annotation.getClass().getDeclaredField("lines"); lines.setAccessible(true);
                        StringBuilder text = new StringBuilder();
                        for (com.watabou.noosa.BitmapText line : (com.watabou.noosa.BitmapText[]) lines.get(annotation)) {
                            if (text.length() > 0) text.append(' ');
                            text.append(line.text());
                            assertTrue(line.x >= 0 && line.x + line.width() <= PixelScene.uiCamera.width);
                        }
                        assertEquals("THIS BUTTON IS EFFECTIVELY USELESS TILL 10 RUNS HAVE BEEN COMPLETED.", text.toString());
                    } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
                });
            } finally {
                onRender(() -> {
                    com.shatteredpixel.shatteredpixeldungeon.windows.UimsdGate.forceGateOnForTest(false);
                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdUnlocked(saved[0]);
                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdDemoUnlockUsed(saved[1]);
                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdTourUnlocked(saved[2]);
                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdTenthPending(saved[3]);
                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdTenthShown(saved[4]);
                });
            }
        }
    }
}
