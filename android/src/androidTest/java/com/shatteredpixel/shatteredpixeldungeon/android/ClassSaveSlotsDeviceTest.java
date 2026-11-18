package com.shatteredpixel.shatteredpixeldungeon.android;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.StartScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.Point;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Tests the real six-class chooser and 20-row scrolling on Android. */
@RunWith(AndroidJUnit4.class)
public class ClassSaveSlotsDeviceTest {

    private static void onRender(Runnable call) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Game.runOnRenderThread(() -> {
            try { call.run(); } catch(Throwable t) { error.set(t); }
            finally { latch.countDown(); }
        });
        assertTrue("render thread stalled",latch.await(45, TimeUnit.SECONDS));
        if (error.get()!=null) throw new AssertionError(error.get());
    }

    @SuppressWarnings("unchecked")
    private static List<Gizmo> members(Group g) throws Exception {
        Field f=Group.class.getDeclaredField("members");
        f.setAccessible(true);
        return new ArrayList<>((List<Gizmo>)f.get(g));
    }

    private static void waitFor(java.util.concurrent.Callable<Boolean> condition) throws Exception {
        long until=System.currentTimeMillis()+30000;
        while(System.currentTimeMillis()<until) {
            if(condition.call())return;
            Thread.sleep(100);
        }
        fail("Timed out opening save selection");
    }

    private static void tap(Point at) throws Exception {
        PointerEvent.addPointerEvent(new PointerEvent(at.x,at.y,81,PointerEvent.Type.DOWN,PointerEvent.LEFT));
        Thread.sleep(120);
        PointerEvent.addPointerEvent(new PointerEvent(at.x,at.y,81,PointerEvent.Type.UP,PointerEvent.LEFT));
        Thread.sleep(250);
    }

    private static WndUimsdList openMenu() throws Exception {
        for(Gizmo g:members(Game.scene())) if(g instanceof WndUimsdList && g.alive)return (WndUimsdList)g;
        return null;
    }

    @Test public void sixClassesOpenTwentyScrollableSaveRows() throws Exception {
        try(ActivityScenario<AndroidLauncher> activity=ActivityScenario.launch(AndroidLauncher.class)) {
            onRender(() -> ShatteredPixelDungeon.switchNoFade(StartScene.class));
            waitFor(() -> Game.scene() instanceof StartScene);
            final AtomicReference<Point> warrior=new AtomicReference<>();
            onRender(() -> {
                try {
                    int classes=0;
                    for(Gizmo g:members(Game.scene())) {
                        if(!(g instanceof StyledButton)) continue;
                        StyledButton button=(StyledButton)g;
                        if(button.text().contains("/20")) {
                            classes++;
                            if(button.text().contains("Warrior"))
                                warrior.set(button.camera().cameraToScreen(button.left()+button.width()/2,
                                        button.top()+button.height()/2));
                        }
                    }
                    assertEquals("All six classes visible on the active-runs page",6,classes);
                }catch(Exception e){throw new RuntimeException(e);}
            });
            assertNotNull("Warrior selection touch target",warrior.get());
            tap(warrior.get());
            waitFor(() -> openMenu()!=null);
            onRender(() -> {
                try {
                    WndUimsdList list=openMenu();
                    Field e=WndUimsdList.class.getDeclaredField("entries");
                    e.setAccessible(true);
                    java.util.List<?> entries=(java.util.List<?>)e.get(list);
                    assertEquals("Exactly twenty slots for this class",20,entries.size());
                    assertTrue(((WndUimsdList.Entry)entries.get(0)).label.startsWith("Run 1"));
                    assertTrue(((WndUimsdList.Entry)entries.get(19)).label.startsWith("Run 20"));
                    Field f=WndUimsdList.class.getDeclaredField("pane");
                    f.setAccessible(true);
                    ScrollPane scroll=(ScrollPane)f.get(list);
                    assertTrue("Twenty entries must scroll instead of leaving the screen",
                            scroll.content().height()>scroll.height());
                    Field spansField=WndUimsdList.class.getDeclaredField("spans");
                    spansField.setAccessible(true);
                    java.util.List<float[]> spans=(java.util.List<float[]>)spansField.get(list);
                    float[] last=spans.get(19);
                    assertNotNull("Final slot must be selectable",last);
                    scroll.scrollTo(0,Math.max(0,last[0]-scroll.height()/2));
                    assertTrue(scroll.content().camera().scroll.y>=0);
                }catch(Exception e){throw new RuntimeException(e);}
            });
        }
    }
}
