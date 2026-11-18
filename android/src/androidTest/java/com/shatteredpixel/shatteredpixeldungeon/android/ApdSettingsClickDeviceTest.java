package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.windows.APDSettingsPane;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndAPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.Point;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

//Touching the controls of APD Settings, for real. Building the screen (UimsdScreensDeviceTest) cannot tell whether a button answers a touch: a button inside a
//scrolling pane only gets its touches if it was created AFTER the pane's own touch area (the newest touch area is asked first, and the pane's covers all of it).
//This opens the real window, scrolls each control into view, sends real pointer events (the same ones the touch screen produces) and checks the response.
@RunWith(AndroidJUnit4.class)
public class ApdSettingsClickDeviceTest {
    @Test public void categorizedSettingsPreserveControlsAndRespondOnSmallScreens()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            openTitleForTest();final AtomicReference<WndAPDSettings> window=new AtomicReference<>();
            final int oldMode=SPDSettings.apdHotbarMode();final boolean oldFood=SPDSettings.apdFoodInHealing();final int[] dimensions=new int[2];
            try{
                onRender(()->{dimensions[0]=com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.width;dimensions[1]=com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.height;
                    long started=System.nanoTime();window.set(new WndAPDSettings());Game.scene().addToFront(window.get());System.out.println("APD_SETTINGS_ROOT_CREATE_NS="+(System.nanoTime()-started));});Thread.sleep(400);
                APDSettingsPane pane=find(window.get(),APDSettingsPane.class);ScrollPane scroll=find(window.get(),ScrollPane.class);
                final List<?> initialRows=(List<?>)field(pane,"rows");
                for(Object row:initialRows)assertEquals("Category root must not allocate hidden descriptions",null,field(row,"description"));
                assertEquals(null,pane.section());assertEquals("",((com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock)field(pane,"status")).text());
                PlayRunDeviceTest.visibilityScreenshot("apd-settings-categories.png");
                String[] captures={"gameplay","rules","hotbar","visibility","history","advanced"};
                for(int i=0;i<6;i++){
                    final List<?> categories=(List<?>)field(pane,"categories");tap(centreOnScreen(scroll,(Gizmo)categories.get(i)));
                    assertEquals(com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection.values()[i],pane.section());
                    final List<?> lazyRows=(List<?>)field(pane,"rows");
                    for(Object row:lazyRows){
                        com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection rowSection=
                                (com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection)field(row,"section");
                        String note=(String)field(row,"note");
                        Object control=field(row,"control");
                        Object description=field(row,"description");
                        boolean eligible=control!=field(pane,"btnCurrentCustomRules")
                                || Game.scene() instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
                        if(!note.isEmpty()&&rowSection.ordinal()<=i&&eligible)
                            assertNotNull("Visited eligible section descriptions materialize once",description);
                        else
                            assertEquals("Unvisited, empty and context-ineligible descriptions stay unallocated",null,description);
                    }
                    final APDSettingsPane current=pane;
                    onRender(()->{try{
                        List<?> rows=(List<?>)field(current,"rows");java.util.Set<Object> seen=new java.util.HashSet<>();
                        for(Object row:rows){com.watabou.noosa.ui.Component control=(com.watabou.noosa.ui.Component)field(row,"control");assertTrue("A control appears in exactly one section",seen.add(control));
                            if(control.visible){assertTrue(control.left()>=current.left());assertTrue(control.right()<=current.right()+0.1f);
                                if(control instanceof RedButton)assertTrue("Usable touch height",control.height()>=20);}
                        }
                    }catch(Exception e){throw new RuntimeException(e);}});
                    PlayRunDeviceTest.visibilityScreenshot("apd-settings-"+captures[i]+".png");
                    tap(centreOnScreen(scroll,(Gizmo)field(pane,"btnSections")));assertEquals(null,pane.section());
                }
                RedButton mode=(RedButton)field(pane,"btnHotbarLayout");
                for(int i:new int[]{0,1,2,3,4,0,4}){
                    tap(centreOnScreen(scroll,mode));WndUimsdList list=find(Game.scene(),WndUimsdList.class);assertNotNull(list);assertEquals(5,communityEntries(list).size());tapCommunityEntry(list,i);assertEquals(i,SPDSettings.apdHotbarMode());
                }
                tap(centreOnScreen(scroll,(Gizmo)field(pane,"chkFoodInHealing")));assertEquals(!oldFood,SPDSettings.apdFoodInHealing());
                tap(centreOnScreen(scroll,(Gizmo)field(pane,"btnHotbarCustom")));WndUimsdList custom=find(Game.scene(),WndUimsdList.class);assertNotNull(custom);assertEquals(6,communityEntries(custom).size());onRender(custom::hide);
                final APDSettingsPane restorePane=pane;
                onRender(()->{Runnable restore=window.get().restoreOnSceneReset();window.get().hide();restore.run();});
                window.set(find(Game.scene(),WndAPDSettings.class));pane=find(window.get(),APDSettingsPane.class);assertEquals(restorePane.section(),pane.section());scroll=find(window.get(),ScrollPane.class);
                tap(centreOnScreen(scroll,(Gizmo)field(pane,"btnSections")));assertEquals(null,pane.section());
                onRender(()->{window.get().hide();com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.width=102;com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.height=160;
                    window.set(new WndAPDSettings(com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection.HOTBAR));Game.scene().addToFront(window.get());});Thread.sleep(400);
                scroll=find(window.get(),ScrollPane.class);final ScrollPane narrow=scroll;
                onRender(()->{narrow.scrollTo(0,Float.MAX_VALUE);assertTrue(narrow.content().camera().scroll.y>0);});
                PlayRunDeviceTest.visibilityScreenshot("apd-settings-narrow.png");
                onRender(()->window.get().onBackPressed());assertEquals(null,find(window.get(),APDSettingsPane.class).section());
            }finally{onRender(()->{if(window.get()!=null)window.get().hide();SPDSettings.apdHotbarMode(oldMode);SPDSettings.apdFoodInHealing(oldFood);
                com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.width=dimensions[0];com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.height=dimensions[1];});}
        }
    }
    private static APDSettingsPane paneFor(Gizmo control){return (APDSettingsPane)control.parent;}

	private static final String TAG = "APDUI";

	private static void onRender( final Runnable r ) throws Exception {
		final CountDownLatch done = new CountDownLatch(1);
		final AtomicReference<Throwable> failure = new AtomicReference<>();
		Game.runOnRenderThread(new com.watabou.utils.Callback(){
			@Override
			public void call(){
				try {
					r.run();
				} catch (Throwable t){
					failure.set(t);
				} finally {
					done.countDown();
				}
			}
		});
		if (!done.await(60, TimeUnit.SECONDS)){
			//say where the game thread is stuck
			for (java.util.Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()){
				String n = e.getKey().getName();
				if (n.contains("GL") || n.contains("Render") || n.contains("main")){
					StringBuilder sb = new StringBuilder("thread " + n + " " + e.getKey().getState());
					int k = 0;
					for (StackTraceElement el : e.getValue()) if (k++ < 14) sb.append("\n   at ").append(el);
					Log.w(TAG, sb.toString());
				}
			}
		}
		assertTrue("the render thread did not answer", done.getCount() == 0);
		if (failure.get() != null) throw new AssertionError("failed on the render thread: " + failure.get(), failure.get());
	}

	@SuppressWarnings("unchecked")
	private static List<Gizmo> membersOf( Group g ) throws Exception {
		Field f = Group.class.getDeclaredField("members");
		f.setAccessible(true);
		List<Gizmo> list = (List<Gizmo>) f.get(g);
		return list == null ? new ArrayList<Gizmo>() : new ArrayList<>(list);
	}

	private static <T> T find( Gizmo g, Class<T> type ) throws Exception {
		if (type.isInstance(g)) return type.cast(g);
		if (g instanceof Group){
			for (Gizmo m : membersOf((Group) g)){
				T t = find(m, type);
				if (t != null) return t;
			}
		}
		return null;
	}

	private static int count( Group g, Class<?> type ) throws Exception {
		int n = 0;
		for (Gizmo m : membersOf(g)) if (type.isInstance(m)) n++;
		return n;
	}

	private static Object field( Object o, String name ) throws Exception {
		for (Class<?> type = o.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field f = type.getDeclaredField(name);
				f.setAccessible(true);
				return f.get(o);
			} catch (NoSuchFieldException ignored) { }
		}
		throw new NoSuchFieldException(name);
	}

	//scrolls the control into view and returns the screen point of its centre
	private static Point centreOnScreen( final ScrollPane scroll, final Gizmo control ) throws Exception {
		final AtomicReference<Point> p = new AtomicReference<>();
		onRender(new Runnable(){
			public void run(){
				com.watabou.noosa.ui.Component c = (com.watabou.noosa.ui.Component) control;
				if(c.parent instanceof APDSettingsPane)((APDSettingsPane)c.parent).reveal(c);
				scroll.scrollTo(0, Math.max(0, c.top() - 20));
			}
		});
		Thread.sleep(400);
		onRender(new Runnable(){
			public void run(){
				com.watabou.noosa.ui.Component c = (com.watabou.noosa.ui.Component) control;
				p.set(c.camera().cameraToScreen(c.left() + c.width() / 2f, c.top() + c.height() / 2f));
			}
		});
		return p.get();
	}

	private static void tap( Point at ) throws Exception {
		PointerEvent.addPointerEvent(new PointerEvent(at.x, at.y, 9, PointerEvent.Type.DOWN, PointerEvent.LEFT));
		Thread.sleep(150);
		PointerEvent.addPointerEvent(new PointerEvent(at.x, at.y, 9, PointerEvent.Type.UP, PointerEvent.LEFT));
		Thread.sleep(400);
	}

	private static int messages() throws Exception {
		return count(Game.scene(), WndMessage.class);
	}

	//a result window is modal: close them so the next touch reaches the settings window again
	private static void dismissMessages() throws Exception {
		onRender(new Runnable(){
			public void run(){
				try {
					for (Gizmo m : membersOf(Game.scene())) if (m instanceof WndMessage) ((WndMessage) m).hide();
				} catch (Exception e){
					throw new RuntimeException(e);
				}
			}
		});
		Thread.sleep(300);
	}

	private static void waitUntil( String what, java.util.concurrent.Callable<Boolean> c, long millis ) throws Exception {
		long end = System.currentTimeMillis() + millis;
		while (System.currentTimeMillis() < end){
			if (c.call()) return;
			Thread.sleep(150);
		}
		throw new AssertionError("timed out waiting for " + what);
	}


	//touches the control until it answers: a late result window of an earlier step can swallow a touch, so closed windows and a missed
	//position are retried. A control that never answers (the bug this test exists for) fails after every attempt.
	private static void tapUntil( String what, ScrollPane scroll, Gizmo control, java.util.concurrent.Callable<Boolean> answered, long millis ) throws Exception {
		long end = System.currentTimeMillis() + millis;
		int attempt = 0;
		while (System.currentTimeMillis() < end){
			attempt++;
			dismissMessages();
			Point at = centreOnScreen(scroll, control);
			Log.w(TAG, what + ": touch " + attempt + " at " + at.x + "," + at.y + " windows=" + messages());
			tap(at);
			long until = Math.min(end, System.currentTimeMillis() + 6000);
			while (System.currentTimeMillis() < until){
				if (answered.call()) { Log.w(TAG, what + " answered"); return; }
				Thread.sleep(150);
			}
		}
		throw new AssertionError(what + " did not answer a touch");
	}

	@Test
	public void adultHumorControlWorksWithoutAnAgeProviderAndReopensWithSavedChoice() throws Exception {
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
			openTitleForTest();
			final boolean previous = SPDSettings.apdAdultHumor();
			final int previousSpellButtons = SPDSettings.apdClericQuickSlots();
            final int previousSpeed = SPDSettings.playbackSpeed();
			final AtomicReference<WndAPDSettings> window = new AtomicReference<>();
			try {
				onRender(() -> {
					SPDSettings.apdAdultHumor(false);
					window.set(new WndAPDSettings());
					ShatteredPixelDungeon.scene().add(window.get());
				});
				Thread.sleep(800);
				ScrollPane scroll = find(window.get(), ScrollPane.class);
				APDSettingsPane pane = find(window.get(), APDSettingsPane.class);
                // Finger starts on a real checkbox. Its swipe must move the page
                // without activating the checkbox; a deliberate tap still activates it.
                // The shorter category needs a compact viewport so this starts before its scroll limit.
                final ScrollPane gesturePane = scroll;
                final Gizmo gestureButton = (Gizmo) field(pane, "chkAdult");
                onRender(()->{paneFor(gestureButton).reveal((com.watabou.noosa.ui.Component)gestureButton);
                    window.get().resize((int)gesturePane.width(),48);gesturePane.setRect(gesturePane.left(),gesturePane.top(),gesturePane.width(),48);});
                Point start = centreOnScreen(gesturePane, gestureButton);
                float[] beforeSwipe = new float[1], afterSwipe = new float[1];
                onRender(() -> {beforeSwipe[0] = gesturePane.content().camera.scroll.y;
                    assertTrue("Swipe fixture must have room below its starting position", beforeSwipe[0]+3 < gesturePane.content().height()-gesturePane.height());
                    Point top=gesturePane.camera().cameraToScreen(gesturePane.left(),gesturePane.top());
                    Point bottom=gesturePane.camera().cameraToScreen(gesturePane.right(),gesturePane.bottom());
                    assertTrue("Checkbox gesture must begin inside the actual scrolling touch area",start.x>=top.x && start.x<bottom.x && start.y>=top.y && start.y<bottom.y);
                });
                final int gestureId = 37;
                PointerEvent.addPointerEvent(new PointerEvent(start.x, start.y, gestureId, PointerEvent.Type.DOWN, PointerEvent.LEFT));
                Thread.sleep(90);
                for (int step = 1; step <= 7; step++) {
                    PointerEvent.addPointerEvent(new PointerEvent(start.x, start.y - step * 16, gestureId, PointerEvent.Type.DOWN, PointerEvent.LEFT));
                    Thread.sleep(80);
                }
                PointerEvent.addPointerEvent(new PointerEvent(start.x, start.y - 112, gestureId, PointerEvent.Type.UP, PointerEvent.LEFT));
                Thread.sleep(450);
                onRender(() -> afterSwipe[0] = gesturePane.content().camera.scroll.y);
                assertTrue("Swipe starting on a checkbox must scroll down the content",
                        afterSwipe[0] > beforeSwipe[0] + 3);
                assertFalse("Swipe must not toggle the checkbox", SPDSettings.apdAdultHumor());
				tapUntil("enable Adult humor", scroll, (Gizmo) field(pane, "chkAdult"), () -> SPDSettings.apdAdultHumor(), 60000);
				assertEquals("no unavailable-verification warning", 0, messages());
				onRender(() -> {
					window.get().hide();
					window.set(new WndAPDSettings());
					ShatteredPixelDungeon.scene().add(window.get());
				});
				Thread.sleep(800);
				scroll = find(window.get(), ScrollPane.class);
				pane = find(window.get(), APDSettingsPane.class);
				assertTrue(((com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox) field(pane, "chkAdult")).checked());
				tapUntil("choose Clean humor", scroll, (Gizmo) field(pane, "chkAdult"), () -> !SPDSettings.apdAdultHumor(), 60000);
				onRender(() -> {
					window.get().hide();
					window.set(new WndAPDSettings());
					ShatteredPixelDungeon.scene().add(window.get());
				});
				Thread.sleep(800);
				pane = find(window.get(), APDSettingsPane.class);
				assertFalse(((com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox) field(pane, "chkAdult")).checked());
				assertFalse(SPDSettings.apdAdultHumor());
                for (Field setting : APDSettingsPane.class.getDeclaredFields())
                    assertFalse("removed hotbar settings must not return", setting.getName().equals("btnHotbar"));
                scroll = find(window.get(), ScrollPane.class);
                final RedButton spells = (RedButton) field(pane, "btnClericQuickCasts");
                assertNotNull("Cleric controls remain directly accessible", spells);
                tapUntil("Cleric quick-cast buttons", scroll, spells,
                        () -> find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class) != null, 30000);
                com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList choices =
                        find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class);
                assertEquals("only one through four spell-button choices", 4, communityEntries(choices).size());
                tapCommunityEntry(choices, 2);
                assertEquals("Cleric setting still answers real touches", 3, SPDSettings.apdClericQuickSlots());
                final RedButton speedButton = (RedButton)field(pane,"btnPlaybackSpeed");
                if (com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.extendedPlayback()) {
                    // Developer and Alpha Testers builds have 10 choices and a scrollable window. Real touches
                    // must reach the first, intermediate and extreme values, then return to 2x.
                    for(int mode:new int[]{0,1,2,3,9,2}){
                        final int selected=mode;
                        tapUntil("open private speed list",scroll,speedButton,
                                () -> find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class)!=null,30000);
                        com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList speedChoices =
                                find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class);
                        assertEquals("approved test builds show ten speeds",10,communityEntries(speedChoices).size());
                        assertEquals("1000x",communityEntries(speedChoices).get(9).label);
                        if(mode==9)TopScoresDeviceTest.screenshot("alpha-playback-choices.png");
                        tapCommunityEntry(speedChoices,mode);
                        waitUntil("private speed persists",()->SPDSettings.playbackSpeed()==selected,10000);
                    }
                } else {
                    // Production builds keep their original three-button popup.
                    for(int mode=0;mode<3;mode++){
                        final int selected=mode;
                        tapUntil("open game-speed choices",scroll,speedButton,
                                () -> find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class)!=null,30000);
                        com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions chooser=
                                find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class);
                        java.util.List<RedButton> options=new java.util.ArrayList<>();
                        for(Gizmo child:membersOf(chooser))if(child instanceof RedButton)options.add((RedButton)child);
                        assertEquals("exactly three playback choices",3,options.size());
                        RedButton choice=options.get(mode);
                        AtomicReference<Point> touch=new AtomicReference<>();
                        onRender(()->touch.set(choice.camera().cameraToScreen(choice.left()+choice.width()/2f,choice.top()+choice.height()/2f)));
                        tap(touch.get());
                        waitUntil("selected speed saved",()->SPDSettings.playbackSpeed()==selected &&
                                find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class)==null,10000);
                    }
                }
                onRender(()->{window.get().hide();window.set(new WndAPDSettings());ShatteredPixelDungeon.scene().add(window.get());});
                Thread.sleep(300);
                pane=find(window.get(),APDSettingsPane.class);
                assertEquals("Game speed: 2x",((RedButton)field(pane,"btnPlaybackSpeed")).text());

			} finally {
				onRender(() -> {
					if (window.get() != null) window.get().hide();
					SPDSettings.apdAdultHumor(previous);
                    SPDSettings.apdClericQuickSlots(previousSpellButtons);
                    SPDSettings.playbackSpeed(previousSpeed);
				});
			}
		}
	}

	// The game deliberately remembers its last scene across Activity recreation.
	// A preceding test can leave HeroSelectScene or GameScene active; launch is not a title reset.
	private static void openTitleForTest() throws Exception {
		waitUntil("the game", () -> Gdx.app != null && Gdx.files != null && Game.scene() != null, 120000);
		onRender(() -> ShatteredPixelDungeon.switchNoFade(com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene.class));
		waitUntil("the title scene", () -> Game.scene() instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene, 30000);
		onRender(() -> {});
	}

	@SuppressWarnings("unchecked")
	private static List<com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.Entry> communityEntries(
			com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList window) throws Exception {
		return (List<com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.Entry>) field(window, "entries");
	}

	@SuppressWarnings("unchecked")
	private static void tapCommunityEntry(com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList window, int index) throws Exception {
		ScrollPane pane = (ScrollPane) field(window, "pane");
		float[] span = ((List<float[]>) field(window, "spans")).get(index);
		assertNotNull("the selected row is actionable", span);
		onRender(() -> pane.scrollTo(0, Math.max(0, span[0] - 2)));
		Thread.sleep(300);
		AtomicReference<Point> point = new AtomicReference<>();
		onRender(() -> point.set(pane.content().camera().cameraToScreen(pane.width() / 2f, span[0] + span[1] / 2f)));
		tap(point.get());
		waitUntil("the Community destination", () -> find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class) != window, 10000);
	}

	private static void assertTextFits(Group group, float width) throws Exception {
		for (Gizmo member : membersOf(group)) {
			if (member instanceof com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock) {
				com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock text = (com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock) member;
				assertTrue("text exceeds its container: " + text.text(), text.left() >= 0 && text.right() <= width + 0.5f);
			}
		}
	}

	@Test
	public void communityMenuOpensEveryPreviewAndReturnsWithoutStackingWindows() throws Exception {
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
			openTitleForTest();
			com.watabou.noosa.ui.Component button = (com.watabou.noosa.ui.Component) field(Game.scene(), "btnCommunity");
			AtomicReference<Point> point = new AtomicReference<>();
			onRender(() -> point.set(button.camera().cameraToScreen(button.left() + button.width()/2f, button.top() + button.height()/2f)));
			tap(point.get());
			waitUntil("the Community hub", () -> Game.scene() instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.CommunityScene, 30000);
			for (com.shatteredpixel.shatteredpixeldungeon.apd.community.CommunityPreview.Page page :
					com.shatteredpixel.shatteredpixeldungeon.apd.community.CommunityPreview.Page.values()) {
				com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList hub = find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class);
				assertNotNull(hub);
				assertEquals(page.title, communityEntries(hub).get(page.ordinal()).label);
				assertEquals(page.summary, communityEntries(hub).get(page.ordinal()).detail);
				tapCommunityEntry(hub, page.ordinal());
				Object previousPageScene = Game.scene();
				onRender(ShatteredPixelDungeon::seamlessResetScene);
				waitUntil("Community page survives scene recreation", () ->
						Game.scene() instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.CommunityScene
						&& Game.scene() != previousPageScene, 30000);
				onRender(() -> {});
				com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList details = find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class);
				List<com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.Entry> entries = communityEntries(details);
				assertEquals(page.explanation, entries.get(0).label);
				assertTrue(entries.stream().anyMatch(e -> e.label.equals(page.availability())));
				assertTrue(entries.stream().noneMatch(e -> e.label.toLowerCase().contains("clan")));
				onRender(() -> {
					try {
						assertEquals("one dialog, no hidden stack", 1, count(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class));
						ScrollPane pane = (ScrollPane) field(details, "pane");
						assertTextFits(details, (Integer) field(details, "width"));
						assertTextFits(pane.content(), pane.width());
					} catch (Exception e) { throw new RuntimeException(e); }
				});
				//Render the actual selected page at a narrow/short UI size, then
				//rebuild at the device's real size before continuing touch navigation.
				onRender(() -> {
					int originalWidth = com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.width;
					int originalHeight = com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.height;
					try {
						details.hide();
						com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.width = 100;
						com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.height = 120;
						java.lang.reflect.Method show = Game.scene().getClass().getDeclaredMethod("showPage");
						show.setAccessible(true); show.invoke(Game.scene());
						com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList narrowPage =
								find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class);
						assertEquals("same selected page on narrow screen", page.explanation, communityEntries(narrowPage).get(0).label);
						assertTrue((Integer) field(narrowPage, "width") <= 72);
						assertTrue((Integer) field(narrowPage, "height") <= 120);
						ScrollPane pane = (ScrollPane) field(narrowPage, "pane");
						assertTextFits(narrowPage, (Integer) field(narrowPage, "width"));
						assertTextFits(pane.content(), pane.width());
						narrowPage.hide();
					} catch (Exception e) { throw new RuntimeException(e); }
					finally {
						com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.width = originalWidth;
						com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.height = originalHeight;
					}
				});
				Object narrowScene = Game.scene();
				onRender(ShatteredPixelDungeon::seamlessResetScene);
				waitUntil("Community returns to device layout", () -> Game.scene() != narrowScene, 30000);
				onRender(() -> {});
				com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList restored =
						find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class);
				assertEquals("no stack after narrow layout", 1, count(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class));
				tapCommunityEntry(restored, communityEntries(restored).size() - 1);
			}
			com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList hub = find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class);
			tapCommunityEntry(hub, communityEntries(hub).size() - 1);
			waitUntil("return to main menu", () -> Game.scene() instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene, 30000);
		}
	}

	@Test
	public void everyControlOfApdSettingsAnswersATouch() throws Exception {
		final AtomicInteger exports = new AtomicInteger(), imports = new AtomicInteger();
		ApdFiles.Provider before = ApdFiles.provider;
		final ApdFiles.Provider fake;
		//the system file picker is replaced by one that records the request and says the player cancelled, so the test can see the click arrive
		fake = new ApdFiles.Provider(){
			public void chooseExport( String suggestedName, ApdFiles.ExportTarget target ){
				exports.incrementAndGet();
				target.onCancelled();
			}

			public void chooseImport( ApdFiles.ImportSource source ){
				imports.incrementAndGet();
				source.onCancelled();
			}

			public String where(){
				return "test";
			}
		};
		boolean recordingWas = SPDSettings.apdRecording();
		boolean adultWas = SPDSettings.apdAdultHumor();
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)){
			long end = System.currentTimeMillis() + 120000;
			while ((Gdx.app == null || Gdx.files == null || Game.scene() == null) && System.currentTimeMillis() < end) Thread.sleep(250);
			assertNotNull("the game did not start", Game.scene());
			//the launcher installs the real file picker when it starts: put the stand-in in only now, or the touch opens the system picker
			ApdFiles.provider = fake;
			//the first scene (welcome / splash) is replaced by the title: a window added before that is destroyed with it
			waitUntil("the title scene", new java.util.concurrent.Callable<Boolean>(){
				public Boolean call() throws Exception { return Game.scene() instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene; } }, 120000);
			Thread.sleep(2500);

			final AtomicReference<WndAPDSettings> window = new AtomicReference<>();
			onRender(new Runnable(){
				public void run(){
					WndAPDSettings w = new WndAPDSettings();
					ShatteredPixelDungeon.scene().add(w);
					window.set(w);
				}
			});
			Thread.sleep(800);
			final ScrollPane scroll = find(window.get(), ScrollPane.class);
			final APDSettingsPane pane = find(window.get(), APDSettingsPane.class);
			assertNotNull("the window has a scrolling pane", scroll);
			assertNotNull("the window holds the APD settings", pane);

			//1. Run self-test: a result window follows
			dismissMessages();
			RedButton test = (RedButton) field(pane, "btnTest");
			tap(centreOnScreen(scroll, test));
			waitUntil("the self-test result (Run self-test did not answer a touch)", new java.util.concurrent.Callable<Boolean>(){
				public Boolean call() throws Exception { return messages() >= 1; } }, 120000);
			Log.w(TAG, "Run self-test answered");

			//2. Import APD rankings
			tapUntil("Import APD rankings", scroll, (RedButton) field(pane, "btnImport"), new java.util.concurrent.Callable<Boolean>(){
				public Boolean call() throws Exception { return messages() >= 1; } }, 90000);

			//3. Export history (ZIP): the file picker is asked for a place to save
			tapUntil("Export history (ZIP)", scroll, (RedButton) field(pane, "btnExportHistory"), new java.util.concurrent.Callable<Boolean>(){
				public Boolean call() throws Exception { return exports.get() >= 1; } }, 60000);

			//4. Import history (ZIP): the file picker is asked for a file
			tapUntil("Import history (ZIP)", scroll, (RedButton) field(pane, "btnImportHistory"), new java.util.concurrent.Callable<Boolean>(){
				public Boolean call() throws Exception { return imports.get() >= 1; } }, 60000);

			//5. the setting that explains itself with a message when touched
			final boolean recBefore = SPDSettings.apdRecording();
			tapUntil("Record run history", scroll, (com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox) field(pane, "chkRecord"), new java.util.concurrent.Callable<Boolean>(){
				public Boolean call() throws Exception { return SPDSettings.apdRecording() != recBefore; } }, 60000);

			//5b. Without an age provider, real touches can select Adult and switch back to Clean.
			assertFalse(SPDSettings.apdAdultHumor());
			tapUntil("Adult humor (18+)", scroll, (com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox) field(pane, "chkAdult"), new java.util.concurrent.Callable<Boolean>(){
				public Boolean call() { return SPDSettings.apdAdultHumor(); } }, 60000);
			assertTrue(SPDSettings.apdAdultHumor());
			assertEquals("toggling Adult must not produce an unavailable-verification warning", 0, messages());
			tapUntil("Clean humor", scroll, (com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox) field(pane, "chkAdult"), new java.util.concurrent.Callable<Boolean>(){
				public Boolean call() { return !SPDSettings.apdAdultHumor(); } }, 60000);
			assertFalse(SPDSettings.apdAdultHumor());

			//6. Open UIMSD (preview) last: it leaves the window by switching scenes
			tapUntil("Open UIMSD (preview)", scroll, (RedButton) field(pane, "btnUimsd"), new java.util.concurrent.Callable<Boolean>(){
				public Boolean call(){ return Game.scene() instanceof UimsdScene; } }, 60000);
			Log.w(TAG, "every APD Settings control answered a touch");
		} finally {
			SPDSettings.apdRecording(recordingWas);
			SPDSettings.apdAdultHumor(adultWas);
			ApdFiles.provider = before;
		}
	}


	// This is the remainder of the APD Settings touch surface. The older method named
	// "every control" predates these controls and did not cover them; this explicit method
	// prevents another visible-but-dead setting from reaching an owner build.
    private static void tapWaitingControl(com.watabou.noosa.ui.Component component)throws Exception{
        AtomicReference<Point> at=new AtomicReference<>();
        onRender(()->at.set(component.camera().cameraToScreen(component.left()+component.width()/2,component.top()+component.height()/2)));
        tap(at.get());
    }

	@Test
	public void remainingApdSettingsControlsRespondToActualTouches() throws Exception {
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
			openTitleForTest();
			final int oldExotic = SPDSettings.apdExoticSetting();
			final boolean oldTiles = SPDSettings.enhancedTileVisibility();
			final boolean oldStairs = SPDSettings.stairNavigationShortcut();
            final boolean oldAutoEnter = SPDSettings.autoEnterStairs();
            final boolean oldSingleTapSearch = SPDSettings.singleTapSearch();
            final boolean[] oldWaiting={SPDSettings.waitStopFullHealth(),SPDSettings.waitStopStarving(),SPDSettings.waitStopCritical(),SPDSettings.autoEatWhileWaiting(),SPDSettings.autoEatWhenHungry()};
            final int oldCriticalPercent=SPDSettings.waitCriticalPercent();
            final int oldWaterskinStart = SPDSettings.waterskinStartingSlot();
			final AtomicReference<WndAPDSettings> window = new AtomicReference<>();
			try {
				onRender(() -> {
					SPDSettings.apdExoticChance(0);
					SPDSettings.enhancedTileVisibility(false);
					SPDSettings.stairNavigationShortcut(false);
                    SPDSettings.autoEnterStairs(false);
                    SPDSettings.waterskinStartingSlot(-1);
					window.set(new WndAPDSettings());
					ShatteredPixelDungeon.scene().add(window.get());
				});
				Thread.sleep(500);
				final ScrollPane scroll = find(window.get(), ScrollPane.class);
				final APDSettingsPane pane = find(window.get(), APDSettingsPane.class);
				assertNotNull(scroll);
				assertNotNull(pane);

				final com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider exotic =
						(com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider) field(pane, "optExotic");
				final AtomicReference<Point> sliderPoint = new AtomicReference<>();
				onRender(() -> {
					pane.reveal(exotic);
                    scroll.scrollTo(0, Math.max(0, exotic.top() - 20));
					sliderPoint.set(exotic.camera().cameraToScreen(exotic.right() - 3, exotic.bottom() - 7));
				});
				Thread.sleep(300);
				tap(sliderPoint.get());
				waitUntil("Healing to Shielding slider", () -> SPDSettings.apdExoticSetting() > 0, 10000);

				tapUntil("Enhanced Tile Visibility", scroll, (Gizmo) field(pane, "chkTileVisibility"),
						SPDSettings::enhancedTileVisibility, 30000);
				tapUntil("Automatic Stair Navigation", scroll, (Gizmo) field(pane, "chkStairNavigation"),
						SPDSettings::stairNavigationShortcut, 30000);
				dismissMessages();
                tapUntil("Auto-enter Stairs on Arrival", scroll, (Gizmo) field(pane, "chkAutoEnterStairs"),
                        SPDSettings::autoEnterStairs, 30000);

                tapUntil("Single-tap Search toggle",scroll,(Gizmo)field(pane,"chkSingleTapSearch"),()->SPDSettings.singleTapSearch()!=oldSingleTapSearch,30000);
                assertEquals(!oldSingleTapSearch,((com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox)field(pane,"chkSingleTapSearch")).checked());
                tapUntil("Single-tap Search original mode",scroll,(Gizmo)field(pane,"chkSingleTapSearch"),()->SPDSettings.singleTapSearch()==oldSingleTapSearch,30000);

                tapUntil("Auto-Eat While Waiting toggle",scroll,(Gizmo)field(pane,"chkAutoEatWaiting"),()->SPDSettings.autoEatWhileWaiting()!=oldWaiting[3],30000);
                assertEquals(!oldWaiting[3],((com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox)field(pane,"chkAutoEatWaiting")).checked());
                tapUntil("Auto-Eat trigger picker",scroll,(Gizmo)field(pane,"btnAutoEatTrigger"),()->find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class)!=null,30000);
                com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions trigger=find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class);
                java.util.List<RedButton> triggerButtons=TopScoresDeviceTest.find(trigger,RedButton.class);assertEquals(2,triggerButtons.size());
                tapWaitingControl(triggerButtons.get(0));assertTrue(SPDSettings.autoEatWhenHungry());
                tapUntil("Waiting interruptions",scroll,(Gizmo)field(pane,"btnWaitingInterruptions"),()->find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndWaitingSettings.class)!=null,30000);
                com.shatteredpixel.shatteredpixeldungeon.windows.WndWaitingSettings waiting=find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndWaitingSettings.class);
                java.util.List<com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox> waitChecks=TopScoresDeviceTest.find(waiting,com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox.class);assertEquals(3,waitChecks.size());
                boolean[] prior={SPDSettings.waitStopFullHealth(),SPDSettings.waitStopStarving(),SPDSettings.waitStopCritical()};
                for(int index=0;index<3;index++){assertEquals(prior[index],waitChecks.get(index).checked());tapWaitingControl(waitChecks.get(index));}
                assertEquals(!prior[0],SPDSettings.waitStopFullHealth());assertEquals(!prior[1],SPDSettings.waitStopStarving());assertEquals(!prior[2],SPDSettings.waitStopCritical());
                RedButton none=null;for(RedButton button:TopScoresDeviceTest.find(waiting,RedButton.class))if(button.text().equals("Disable these three stops"))none=button;
                assertNotNull(none);tapWaitingControl(none);assertFalse(SPDSettings.waitStopFullHealth());assertFalse(SPDSettings.waitStopStarving());assertFalse(SPDSettings.waitStopCritical());
                assertEquals("preset does not alter Auto-Eat",!oldWaiting[3],SPDSettings.autoEatWhileWaiting());assertTrue(SPDSettings.autoEatWhenHungry());
                TopScoresDeviceTest.screenshot("qol-waiting-independent-stops.png");
                final com.shatteredpixel.shatteredpixeldungeon.windows.WndWaitingSettings closeWaiting=waiting;onRender(closeWaiting::hide);
                tapUntil("Reopen saved waiting choices",scroll,(Gizmo)field(pane,"btnWaitingInterruptions"),()->find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndWaitingSettings.class)!=null,30000);
                waiting=find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndWaitingSettings.class);
                for(com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox check:TopScoresDeviceTest.find(waiting,com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox.class))assertFalse("disabled state restored",check.checked());
                TopScoresDeviceTest.screenshot("qol-waiting-saved-stops.png");
                final com.shatteredpixel.shatteredpixeldungeon.windows.WndWaitingSettings closeReopened=waiting;onRender(closeReopened::hide);

                // The shared player/developer APD pane must expose all eight
                // Waterskin choices. Verify actual screen touches and persistence.
                final RedButton waterskinStart = (RedButton) field(pane, "btnWaterskinSlot");
                assertNotNull("Waterskin preference available in shared settings", waterskinStart);
                tapUntil("Waterskin start slot picker", scroll, waterskinStart,
                        () -> find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class) != null, 30000);
                com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList slotChoices =
                        find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.class);
                assertEquals("automatic, six slots and off", 8, communityEntries(slotChoices).size());
                assertEquals("Automatic", communityEntries(slotChoices).get(0).label);
                assertEquals("Slot 2", communityEntries(slotChoices).get(2).label);
                assertEquals("Off", communityEntries(slotChoices).get(7).label);
                tapCommunityEntry(slotChoices, 2);
                waitUntil("Waterskin start Slot 2 saved", () -> SPDSettings.waterskinStartingSlot() == 1, 10000);
                assertEquals("Waterskin start: Slot 2", waterskinStart.text());

				tapUntil("Healing confirmation", scroll, (Gizmo) field(pane, "btnHealingConfirmation"),
						() -> find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndHealingConfirmation.SettingsWindow.class) != null, 30000);
				onRender(() -> {
					try {
						com.shatteredpixel.shatteredpixeldungeon.windows.WndHealingConfirmation.SettingsWindow options =
								find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndHealingConfirmation.SettingsWindow.class);
						assertNotNull(options);
						options.hide();
					} catch (Exception e) { throw new RuntimeException(e); }
				});

				tapUntil("Custom Dungeon Rules", scroll, (Gizmo) field(pane, "btnCustomRules"),
						() -> find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndCustomDungeonRules.class) != null, 30000);
				onRender(() -> {
					try {
						com.shatteredpixel.shatteredpixeldungeon.windows.WndCustomDungeonRules rules =
								find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndCustomDungeonRules.class);
						assertNotNull(rules);
						assertNotNull("Custom Rules contains its slider controls",
								find(rules, com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider.class));
						rules.hide();
					} catch (Exception e) { throw new RuntimeException(e); }
				});
			} finally {
				onRender(() -> {
					try {
						com.shatteredpixel.shatteredpixeldungeon.windows.WndCustomDungeonRules rules =
								find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndCustomDungeonRules.class);
						if (rules != null) rules.hide();
						com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions options =
								find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class);
						if (options != null) options.hide();
					} catch (Exception e) { throw new RuntimeException(e); }
					if (window.get() != null) window.get().hide();
					SPDSettings.apdExoticChance(oldExotic);
					SPDSettings.enhancedTileVisibility(oldTiles);
					SPDSettings.stairNavigationShortcut(oldStairs);
                    SPDSettings.autoEnterStairs(oldAutoEnter);
                SPDSettings.singleTapSearch(oldSingleTapSearch);
                    SPDSettings.waitStopFullHealth(oldWaiting[0]);SPDSettings.waitStopStarving(oldWaiting[1]);SPDSettings.waitStopCritical(oldWaiting[2]);SPDSettings.autoEatWhileWaiting(oldWaiting[3]);SPDSettings.autoEatWhenHungry(oldWaiting[4]);SPDSettings.waitCriticalPercent(oldCriticalPercent);
                    SPDSettings.waterskinStartingSlot(oldWaterskinStart);
				});
			}
		}
	}

	// Every interactive field must have a named native acceptance owner. Adding a
	// control without updating this map fails on API 34 instead of silently reducing coverage.
	@Test
	public void everyDeclaredSettingControlHasAnExplicitAcceptanceOwner() throws Exception {
		final java.util.Map<String, Object[]> owners = new java.util.LinkedHashMap<>();
		owners.put("optExotic", new Object[]{ ApdSettingsClickDeviceTest.class, "remainingApdSettingsControlsRespondToActualTouches" });
		owners.put("chkRecord", new Object[]{ ApdSettingsClickDeviceTest.class, "everyControlOfApdSettingsAnswersATouch" });
        owners.put("chkDotOverlay", new Object[]{ ApdSettingsClickDeviceTest.class, "remainingApdSettingsControlsRespondToActualTouches" });
		owners.put("chkAdult", new Object[]{ ApdSettingsClickDeviceTest.class, "adultHumorControlWorksWithoutAnAgeProviderAndReopensWithSavedChoice" });
		owners.put("chkTileVisibility", new Object[]{ ApdSettingsClickDeviceTest.class, "remainingApdSettingsControlsRespondToActualTouches" });
		owners.put("chkStairNavigation", new Object[]{ ApdSettingsClickDeviceTest.class, "remainingApdSettingsControlsRespondToActualTouches" });
        owners.put("chkAutoEnterStairs", new Object[]{ ApdSettingsClickDeviceTest.class, "remainingApdSettingsControlsRespondToActualTouches" });
		owners.put("btnWaitingInterruptions", new Object[]{ ApdSettingsClickDeviceTest.class, "remainingApdSettingsControlsRespondToActualTouches" });
        owners.put("chkAutoEatWaiting", new Object[]{ ApdSettingsClickDeviceTest.class, "remainingApdSettingsControlsRespondToActualTouches" });
        owners.put("btnAutoEatTrigger", new Object[]{ ApdSettingsClickDeviceTest.class, "remainingApdSettingsControlsRespondToActualTouches" });
        owners.put("chkSingleTapSearch", new Object[]{ ApdSettingsClickDeviceTest.class, "remainingApdSettingsControlsRespondToActualTouches" });
        owners.put("btnPlaybackSpeed", new Object[]{ ApdSettingsClickDeviceTest.class, "adultHumorControlWorksWithoutAnAgeProviderAndReopensWithSavedChoice" });
        owners.put("btnWaterskinSlot", new Object[]{ ApdSettingsClickDeviceTest.class, "remainingApdSettingsControlsRespondToActualTouches" });
		owners.put("btnHealingConfirmation", new Object[]{ ApdSettingsClickDeviceTest.class, "remainingApdSettingsControlsRespondToActualTouches" });
		owners.put("btnCustomRules", new Object[]{ ApdSettingsClickDeviceTest.class, "remainingApdSettingsControlsRespondToActualTouches" });
		owners.put("btnCurrentCustomRules", new Object[]{ CustomRulesNoticeDeviceTest.class, "noticeStaysAboveUsableControlsAndDoesNotChangeLockedRunOrScores" });
		owners.put("btnClericQuickCasts", new Object[]{ ApdSettingsClickDeviceTest.class, "adultHumorControlWorksWithoutAnAgeProviderAndReopensWithSavedChoice" });
		owners.put("btnTest", new Object[]{ ApdSettingsClickDeviceTest.class, "everyControlOfApdSettingsAnswersATouch" });
		owners.put("btnExportHistory", new Object[]{ ApdSettingsClickDeviceTest.class, "everyControlOfApdSettingsAnswersATouch" });
		owners.put("btnImportHistory", new Object[]{ ApdSettingsClickDeviceTest.class, "everyControlOfApdSettingsAnswersATouch" });
		owners.put("btnImport", new Object[]{ ApdSettingsClickDeviceTest.class, "everyControlOfApdSettingsAnswersATouch" });
		owners.put("btnUimsd", new Object[]{ ApdSettingsClickDeviceTest.class, "everyControlOfApdSettingsAnswersATouch" });
		owners.put("btnGrudge", new Object[]{ ApdSettingsClickDeviceTest.class, "grudgeControlsRespondToActualTouches" });
		owners.put("btnDemoGenerate", new Object[]{ DemoUnlockDeviceTest.class, "theDemoUnlockMakesTenDemoRunsOpensUimsdForGoodAndItsButtonIsUsedUp" });
		owners.put("btnDemoRemove", new Object[]{ DemoUnlockDeviceTest.class, "theDemoUnlockMakesTenDemoRunsOpensUimsdForGoodAndItsButtonIsUsedUp" });
		owners.put("btnDemoView", new Object[]{ DemoUnlockDeviceTest.class, "theDemoUnlockMakesTenDemoRunsOpensUimsdForGoodAndItsButtonIsUsedUp" });

        owners.put("btnSearchedWallColor",new Object[]{QolItemFeedbackDeviceTest.class,"grassDewControlsRingBreakdownsSearchedWallAndOfflineLicenseRenderNatively"});
        owners.put("chkRatKingSearch",new Object[]{QolItemFeedbackDeviceTest.class,"grassDewControlsRingBreakdownsSearchedWallAndOfflineLicenseRenderNatively"});
        owners.put("btnDiagnosticReport",new Object[]{DiagnosticReportDeviceTest.class,"actualReportButtonSavesDeviceEvidenceAndMenusSurvivePauseResume"});
        for(String id:new String[]{"btnSections","btnHotbarLayout","btnHotbarCustom","chkFoodInHealing"})owners.put(id,new Object[]{ApdSettingsClickDeviceTest.class,"categorizedSettingsPreserveControlsAndRespondOnSmallScreens"});
		final java.util.Set<String> declared = new java.util.TreeSet<>();
		for (Field f : APDSettingsPane.class.getDeclaredFields()) {
			Class<?> type = f.getType();
			if (com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider.class.isAssignableFrom(type)
					|| com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox.class.isAssignableFrom(type)
					|| RedButton.class.isAssignableFrom(type)) declared.add(f.getName());
		}
		assertEquals("Every APD Settings control needs an explicit acceptance owner",
				new java.util.TreeSet<>(owners.keySet()), declared);
		for (java.util.Map.Entry<String, Object[]> entry : owners.entrySet()) {
			Class<?> owner = (Class<?>) entry.getValue()[0];
			String methodName = (String) entry.getValue()[1];
			java.lang.reflect.Method method = owner.getDeclaredMethod(methodName);
			assertNotNull(entry.getKey() + " owner must remain an executable native test",
					method.getAnnotation(Test.class));
		}
	}

	// Regression for the owner's first alpha screenshot: the Grudge page draws normally
	// but its scroll controller was registered after the button hit areas and ate every tap.
	@Test
	public void grudgeControlsRespondToActualTouches() throws Exception {
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
			openTitleForTest();
			assertTrue("Grudge page is enabled in this private test build",
					com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.available());
			final com.shatteredpixel.shatteredpixeldungeon.apd.humor.Frequency oldFrequency =
					com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.frequency();
			final com.shatteredpixel.shatteredpixeldungeon.apd.humor.Location place =
					com.shatteredpixel.shatteredpixeldungeon.apd.humor.Location.STARTUP;
			final boolean oldPlace =
					com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.prefs().enabled(place);
			final AtomicReference<WndAPDSettings> settings = new AtomicReference<>();
			try {
				onRender(() -> {
					settings.set(new WndAPDSettings());
					ShatteredPixelDungeon.scene().add(settings.get());
				});
				Thread.sleep(400);
				ScrollPane settingsScroll = find(settings.get(), ScrollPane.class);
				APDSettingsPane settingsPane = find(settings.get(), APDSettingsPane.class);
				RedButton opener = (RedButton) field(settingsPane, "btnGrudge");
				assertNotNull("Grudge option appears inside APD Settings", opener);
				tapUntil("open Grudge from APD Settings", settingsScroll, opener,
						() -> find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndClericGrudge.class) != null, 30000);

				com.shatteredpixel.shatteredpixeldungeon.windows.WndClericGrudge page =
						find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndClericGrudge.class);
				assertNotNull(page);
				ScrollPane scroll = find(page, ScrollPane.class);
				assertNotNull(scroll);
				RedButton frequency = (RedButton) field(page, "btnFrequency");
				assertNotNull(frequency);
				tapUntil("change Grudge frequency", scroll, frequency,
						() -> com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.frequency() == oldFrequency.next(), 30000);

				com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox startup =
						find(scroll.content(), com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox.class);
				assertNotNull("location switches exist in the Grudge scroll content", startup);
				assertEquals(oldPlace, startup.checked());
				tapUntil("toggle Grudge startup messages", scroll, startup,
						() -> com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.prefs().enabled(place) != oldPlace, 30000);
				assertEquals(!oldPlace, startup.checked());

				RedButton reset = null;
				for (Gizmo g : membersOf(scroll.content())) {
					if (g instanceof RedButton && "Forget what I've heard".equals(((RedButton)g).text())) {
						reset = (RedButton)g;
						break;
					}
				}
				assertNotNull("history confirmation control is present", reset);
				tapUntil("open history confirmation without clearing it", scroll, reset,
						() -> find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class) != null, 30000);
				assertNotNull("confirmation must appear without erasing history",
						find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class));
			} finally {
				onRender(() -> {
					try {
						com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions confirm =
								find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class);
						if (confirm != null) confirm.hide(); //never confirm destructive history reset
						com.shatteredpixel.shatteredpixeldungeon.windows.WndClericGrudge page =
								find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndClericGrudge.class);
						if (page != null) page.hide();
					} catch (Exception e) { throw new RuntimeException(e); }
					if (settings.get() != null) settings.get().hide();
					com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.setFrequency(oldFrequency);
					com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.prefs().setEnabled(place, oldPlace);
				});
			}
		}
	}

}
