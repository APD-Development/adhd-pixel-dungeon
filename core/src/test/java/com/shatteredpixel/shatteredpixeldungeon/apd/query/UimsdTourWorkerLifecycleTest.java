package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Scene;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

//Exercises the real asynchronous tour loader without rendering or real player data.
public class UimsdTourWorkerLifecycleTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private final Map<Field, Object> saved = new LinkedHashMap<>();
    private Application oldApp;
    private final ConcurrentLinkedQueue<Runnable> posted = new ConcurrentLinkedQueue<>();
    private Game oldGame;
    private Object oldPlatform, oldSceneClass;
    private UimsdScene former, current;
    private TestGame game;
    private static class TestGame extends Game {
        TestGame(Scene current) { super(UimsdScene.class, null); scene = current; }
        void show(Scene next) { scene = next; }
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static void set(String name, Object value) throws Exception {
        field(UimsdScene.class, name).set(null, value);
    }
    private static Object get(String name) throws Exception {
        return field(UimsdScene.class, name).get(null);
    }
    private static void dirty(UimsdScene scene, boolean value) throws Exception {
        field(UimsdScene.class, "dirty").setBoolean(scene, value);
    }
    private static boolean dirty(UimsdScene scene) throws Exception {
        return field(UimsdScene.class, "dirty").getBoolean(scene);
    }
    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        for (Field f : UimsdScene.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())) {
                f.setAccessible(true); saved.put(f, f.get(null));
            }
        }
        oldApp = Gdx.app;
        Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(),
                new Class<?>[]{Application.class}, (proxy, method, args) -> {
                    if (method.getName().equals("postRunnable")) { posted.add((Runnable)args[0]); return null; }
                    return method.invoke(oldApp, args);
                });
        oldGame = Game.instance;
        oldPlatform = Game.platform;
        oldSceneClass = field(Game.class, "sceneClass").get(null);
        former = new UimsdScene(); current = new UimsdScene();
        game = new TestGame(current);
        UimsdModel main = new UimsdModel("", "");
        main.load(tmp.newFolder("history")); main.awaitLoad(10000);
        assertEquals(UimsdModel.State.READY, main.state());
        set("mainRoot", main.root()); set("mainModel", main); set("model", main);
        set("rulesId", ""); set("rulesLabel", ""); set("tutorial", null);
        set("tutorialPreparing", false); set("sampleRoot", null); set("showingSample", false);
        dirty(former, false); dirty(current, false);
    }
    private void drainRenderCallbacks() {
        Runnable next; while ((next = posted.poll()) != null) next.run();
    }
    private UimsdModel finishOldSceneWorker() throws Exception {
        //The old scene owns this worker; the current scene has already replaced it.
        Method begin = UimsdScene.class.getDeclaredMethod("beginTour", int.class, boolean.class);
        begin.setAccessible(true); begin.invoke(former, 0, true);
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals("apd-uimsd-tour")) {
                t.join(10000); assertFalse("tour loader did not finish", t.isAlive());
            }
        }
        assertFalse((Boolean)get("tutorialPreparing"));
        assertNotNull("real tour must have loaded", get("tutorial"));
        UimsdModel loaded = (UimsdModel)get("model");
        assertNotSame(get("mainModel"), loaded);
        assertEquals(UimsdModel.State.READY, loaded.state());
        return loaded;
    }
    @Test public void loaderCompletionInvalidatesTheReplacementScene() throws Exception {
        finishOldSceneWorker();
        drainRenderCallbacks();
        assertTrue("current scene must leave the Preparing display", dirty(current));
    }
    @Test public void loadedTourChangesInvalidateCurrentSceneRatherThanFormerScene() throws Exception {
        UimsdModel loaded = finishOldSceneWorker();
        drainRenderCallbacks();
        dirty(current, false); dirty(former, false);
        loaded.setMinSample(11);
        drainRenderCallbacks();
        assertTrue("tour listener must reach the replacement scene", dirty(current));
        assertFalse("retired scene must not own the model listener", dirty(former));
    }
    @Test public void callbackResolvesSceneAtDeliveryAndIsSafeAfterLeavingUimsd() throws Exception {
        UimsdModel loaded = finishOldSceneWorker();
        UimsdScene newest = new UimsdScene(); dirty(newest, false);
        game.show(newest); drainRenderCallbacks();
        assertTrue("resolve after the queued callback, not when worker posted it", dirty(newest));
        assertFalse("intermediate retired scene stays untouched", dirty(current));
        dirty(newest, false); loaded.setMinSample(7);
        game.show(new Scene()); drainRenderCallbacks();
        assertFalse("leaving UIMSD must not revive its old scene", dirty(newest));
        assertEquals(7, loaded.query().minSample);
    }
    @After public void cleanup() throws Exception {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals("apd-uimsd-tour")) t.join(10000);
        }
        for (Map.Entry<Field, Object> e : saved.entrySet()) e.getKey().set(null, e.getValue());
        posted.clear(); Gdx.app = oldApp;
        Game.instance = oldGame;
        field(Game.class, "platform").set(null, oldPlatform);
        field(Game.class, "sceneClass").set(null, oldSceneClass);
    }
}

