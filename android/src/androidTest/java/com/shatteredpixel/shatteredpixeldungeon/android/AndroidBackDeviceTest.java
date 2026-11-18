package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.util.Log;
import android.view.KeyEvent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Query;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Question;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Questions;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.JournalScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndAPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdAsk;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Gizmo;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

//Android's Back, pressed for real (the system's own Back key is injected into the app), must walk backwards through whatever is open:
//a dialog closes before the screen under it, a menu opened from a menu returns to that menu, a tour ends before UIMSD is left, and UIMSD
//returns to where it was opened from. Each press must be ONE step (never two, never none).
@RunWith(AndroidJUnit4.class)
public class AndroidBackDeviceTest {

	private static final String TAG = "APDUI";

	// Activity recreation retains Game.sceneClass. The previous test may leave
	// UIMSD open, so launch is not evidence that this fixture starts at the title.
	private static void enterTitle() throws Exception {
		waitUntil("the running game", () -> Gdx.app != null && Game.instance != null
				&& Game.scene() != null, 120000);
		onRender(() -> ShatteredPixelDungeon.switchNoFade(TitleScene.class));
		waitUntil("the title scene", () -> Game.scene() instanceof TitleScene
				&& !Game.switchingScene(), 30000);
		// Assignment precedes create(); cross the render thread before using
		// the scene, rather than treating the published reference as a barrier.
		AtomicReference<com.watabou.noosa.Scene> observed=new AtomicReference<>();
		AtomicInteger stableFrames=new AtomicInteger();
		waitUntil("completed stable title frames",()->{
			onRender(()->{
				boolean stable=Game.scene() instanceof TitleScene&&!Game.switchingScene()
						&& Game.scene()==observed.get() && Game.width==Gdx.graphics.getWidth()
						&& Game.height==Gdx.graphics.getHeight();
				stableFrames.set(stable?stableFrames.get()+1:0); observed.set(Game.scene());
			});
			return stableFrames.get()>=2;
		},30000);
	}

	@Test
	public void menuBackReachesAResponsiveTitleWithoutHalfSecondWaits() throws Exception {
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)){
			enterTitle();
			boolean guideSeen=com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdGuideSeen();
			com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.Page oldPage=
					com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.page;
			Class<? extends com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene> oldBack=
					com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.backTo;
			List<String> slow=new ArrayList<>();
			try {
				onRender(()->com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdGuideSeen(true));
				List<Runnable> routes=java.util.Arrays.asList(
					()->ShatteredPixelDungeon.switchNoFade(com.shatteredpixel.shatteredpixeldungeon.scenes.AboutScene.class),
					()->ShatteredPixelDungeon.switchNoFade(JournalScene.class),
					()->ShatteredPixelDungeon.switchNoFade(com.shatteredpixel.shatteredpixeldungeon.scenes.RankingsScene.class),
					()->ShatteredPixelDungeon.switchNoFade(com.shatteredpixel.shatteredpixeldungeon.scenes.StartScene.class),
					()->ShatteredPixelDungeon.switchNoFade(com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene.class),
					()->{com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.page=com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.Page.NEWS;com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.backTo=TitleScene.class;ShatteredPixelDungeon.switchNoFade(com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.class);},
					()->{com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.page=com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.Page.CHANGES;com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.backTo=TitleScene.class;ShatteredPixelDungeon.switchNoFade(com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.class);},
					()->com.shatteredpixel.shatteredpixeldungeon.scenes.CommunityScene.open(),
					()->UimsdScene.openNow(TitleScene.class));
				String[] names={"About","Journal","Top Scores","Play slots","Hero selection","News","Changes","Community","UIMSD"};
				for(int i=0;i<routes.size();i++){
					final com.watabou.noosa.Scene titleBefore = Game.scene();
					final com.watabou.noosa.Camera cameraBefore = com.watabou.noosa.Camera.main;
					onRender(routes.get(i));
					waitUntil(names[i]+" menu",()->!(Game.scene() instanceof TitleScene)&&!Game.switchingScene(),30000);
					// Outside the measurement: let setup finish and the existing 120ms
					// duplicate-route gate expire before injecting an independent press.
					Thread.sleep(200);
					long begin=System.nanoTime();
					InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
					long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
					while(!(Game.scene() instanceof TitleScene)&&System.nanoTime()<deadline)Thread.sleep(5);
					assertTrue(names[i]+" Back must return to Title",Game.scene() instanceof TitleScene);
					// A render-thread round trip includes completed scene construction,
					// rather than merely seeing Game.scene assigned before create().
					onRender(()->{
						assertTrue(Game.scene() instanceof TitleScene);
						assertTrue("Back reuses the existing title", titleBefore == Game.scene());
						assertTrue("Back restores the title camera", cameraBefore == com.watabou.noosa.Camera.main);
						assertTrue("restored title accepts input", Game.scene().isActive());
					});
					long millis=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-begin);
					Log.w("APDPERF","MENU_BACK menu="+names[i]+" responsive_title_ms="+millis+" budget_ms=250");
					if(millis>=250)slow.add(names[i]+"="+millis+"ms");
				}
				assertTrue("Menu Back responsiveness budget exceeded: "+slow,slow.isEmpty());
			}finally{
				onRender(()->{
					com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdGuideSeen(guideSeen);
					com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.page=oldPage;
					com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.backTo=oldBack;
				});
			}
		}
	}

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
		assertTrue("the render thread did not answer", done.await(60, TimeUnit.SECONDS));
		if (failure.get() != null) throw new AssertionError("failed on the render thread: " + failure.get(), failure.get());
	}

	@Test
	public void homeTextKeepsItsWordsAndFitsOversizedTokens() throws Exception {
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)){
			enterTitle();
			onRender(()->{try{
				com.shatteredpixel.shatteredpixeldungeon.ui.UimsdHomePane pane=new com.shatteredpixel.shatteredpixeldungeon.ui.UimsdHomePane();
				try {
					Method text=pane.getClass().getDeclaredMethod("text",String.class,int.class,int.class,float.class,float.class,float.class);text.setAccessible(true);
					for(String value:new String[]{"Short","Several words wrap into a narrow Home column","very_long_synthetic_identifier_that_must_fit_the_column"}){
						com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock block=(com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock)text.invoke(pane,value,6,0xffffff,30f,0f,0f);
						assertEquals("presentation breaks preserve every character",value,block.text().replace("\n",""));
						assertTrue("Home text stays within its column: "+value,block.width()<=30.01f);
						if(value.length()>10)assertTrue("long text wraps",block.nLines>1);
					}
				} finally {pane.destroy();}
			}catch(Exception e){throw new RuntimeException(e);}});
		}
	}

	@Test
	public void warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms() throws Exception {
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)){
			enterTitle();
			java.io.File base = new java.io.File(InstrumentationRegistry.getInstrumentation()
					.getTargetContext().getFilesDir(), "apd-open-timing-"+System.nanoTime());
			java.io.File real = new java.io.File(base,"apd");
			java.io.File demo = com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData.demoRootFor(real);
			java.io.File originalRoot = com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder.historyRoot();
			boolean recording = com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder.status().enabled;
			boolean oldDemo = com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData.viewingDemo();
			boolean oldGuide = com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdGuideSeen();
			boolean oldUnlocked = com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdUnlocked();
			Method reset = com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdWarmCache.class.getDeclaredMethod("resetForTest");
			Method await = com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdWarmCache.class.getDeclaredMethod("awaitPrewarmForTest",long.class);
			reset.setAccessible(true); await.setAccessible(true);
			try {
				com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator.generate(demo,
						com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset.forNewRun(),System.currentTimeMillis());
				reset.invoke(null);
				onRender(()->{
					com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder.configure(real,false);
					com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData.viewDemo(true);
					com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdGuideSeen(true);
					com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdUnlocked(true);
					Game.resetScene();
				});
				waitUntil("fresh title for synthetic timing history",()->Game.scene() instanceof TitleScene&&!Game.switchingScene(),30000);
				onRender(()->{}); // title creation and its prewarm request have completed
				await.invoke(null,60000L);
				Field ready = com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdWarmCache.class.getDeclaredField("ready");
				ready.setAccessible(true); assertNotNull("actual title preload completed",ready.get(null));
				Field readyModel = ready.get(null).getClass().getDeclaredField("model");
				readyModel.setAccessible(true);
				AtomicReference<com.watabou.noosa.Scene> titleIdentity=new AtomicReference<>();
				AtomicReference<com.watabou.noosa.Camera> titleCamera=new AtomicReference<>();
				onRender(()->{
					titleIdentity.set(Game.scene());
					titleCamera.set(com.watabou.noosa.Camera.main);
				});
				UimsdModel previousReady=null;
				List<String> slowOpenings=new ArrayList<>();
				for(int opening=1;opening<=2;opening++){
					Object preparedEntry=ready.get(null);
					assertNotNull("opening "+opening+" title preload completed",preparedEntry);
					UimsdModel prepared=(UimsdModel)readyModel.get(preparedEntry);
					assertNotNull("opening "+opening+" preload contains its READY model",prepared);
					if(previousReady!=null)assertTrue("returning title replenishes a distinct READY model",previousReady!=prepared);
					AtomicReference<com.watabou.utils.Point> target=new AtomicReference<>();
					onRender(()->{try{
						Field field=TitleScene.class.getDeclaredField("btnStatistics"); field.setAccessible(true);
						com.shatteredpixel.shatteredpixeldungeon.ui.Button button=(com.shatteredpixel.shatteredpixeldungeon.ui.Button)field.get(Game.scene());
						assertTrue("UIMSD title button accepts taps",button.isActive());
						target.set(button.camera().cameraToScreen(button.left()+button.width()/2,button.top()+button.height()/2));
					}catch(Exception e){throw new RuntimeException(e);}});
					long begin=System.nanoTime(), down=android.os.SystemClock.uptimeMillis();
					long downDeliveredNanos=0, inputDeliveredNanos=0;
					for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}){
						android.view.MotionEvent event=android.view.MotionEvent.obtain(down,android.os.SystemClock.uptimeMillis(),action,target.get().x,target.get().y,0);
						try {InstrumentationRegistry.getInstrumentation().sendPointerSync(event);} finally {event.recycle();}
						if(action==android.view.MotionEvent.ACTION_DOWN)downDeliveredNanos=System.nanoTime();
						else inputDeliveredNanos=System.nanoTime();
					}
					Field drawn=UimsdScene.class.getDeclaredField("firstReadyHomeDrawNanos"); drawn.setAccessible(true);
					long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10),drawNanos=0,sceneSeenNanos=0;
					while(System.nanoTime()<deadline){
						if(Game.scene() instanceof UimsdScene){
							if(sceneSeenNanos==0)sceneSeenNanos=System.nanoTime();
							drawNanos=drawn.getLong(Game.scene());
						}
						if(drawNanos>begin) break;
						Thread.sleep(2);
					}
					assertTrue("opening "+opening+" complete Home was drawn after the native tap",drawNanos>begin);
					assertTrue("opening "+opening+" UIMSD scene was observed after the native tap",sceneSeenNanos>begin);
					long millis=TimeUnit.NANOSECONDS.toMillis(drawNanos-begin);
					Log.w("APDPERF","UIMSD_NATIVE_TAP opening="+opening+" ready_72_run_home_draw_ms="+millis+" budget_ms=250");
					Log.w("APDPERF","UIMSD_NATIVE_TAP_MILESTONE opening="+opening
							+" down_dispatch_ms="+TimeUnit.NANOSECONDS.toMillis(downDeliveredNanos-begin)
							+" input_dispatch_ms="+TimeUnit.NANOSECONDS.toMillis(inputDeliveredNanos-begin)
							+" scene_observed_ms="+TimeUnit.NANOSECONDS.toMillis(sceneSeenNanos-begin)
							+" scene_to_home_ms="+TimeUnit.NANOSECONDS.toMillis(drawNanos-sceneSeenNanos)
							+" input_to_home_ms="+TimeUnit.NANOSECONDS.toMillis(drawNanos-inputDeliveredNanos));
					for(String phase:new String[]{"createDurationNanos","rebuildDurationNanos","firstHomeDrawDurationNanos"}){
						Field timing=UimsdScene.class.getDeclaredField(phase);timing.setAccessible(true);
						Log.w("APDPERF","UIMSD_DRAW_PHASE opening="+opening+" "+phase+"="+timing.getLong(Game.scene())+"ns");
					}
					if(millis>=250)slowOpenings.add("opening "+opening+"="+millis+"ms");
					onRender(()->{try{
						assertTrue("opening claims the exact READY model",prepared==uimsd("model"));
						assertEquals(UimsdModel.State.READY,((UimsdModel)uimsd("model")).state());
						assertEquals("first Home draw has a pixel projection",2f*com.watabou.noosa.Camera.main.zoom/Game.width,com.watabou.noosa.Camera.main.matrix[0],0.000001f);
						assertEquals("all72 synthetic source runs were loaded",72,((UimsdModel)uimsd("model")).catalog().dataset("runs").rows.size());
						assertTrue("Home has finished-run statistics",((com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdHome)uimsd("cachedHome")).runs>0);
						assertEquals(0,windows());
					}catch(Exception e){throw new RuntimeException(e);}});
					previousReady=prepared;
					if(opening==1){
						pressBack();
						waitUntil("same retained title after first UIMSD opening",()->Game.scene() instanceof TitleScene&&!Game.switchingScene(),30000);
						onRender(()->{
							assertTrue("Back restores the exact retained title",titleIdentity.get()==Game.scene());
							assertTrue("Back restores the exact retained title camera",titleCamera.get()==com.watabou.noosa.Camera.main);
                            assertNull("leaving UIMSD releases its previous full catalog before title preparation",prepared.catalog());
                            assertNull("leaving UIMSD releases its previous query result",prepared.result());
						});
						await.invoke(null,60000L);
					}
				}
				assertTrue("native tap to complete Home exceeded 250ms: "+slowOpenings,slowOpenings.isEmpty());
                UimsdModel prior=previousReady;
                pressBack();waitUntil("title before cold reopening",()->Game.scene() instanceof TitleScene&&!Game.switchingScene(),30000);
                assertNull("old catalog released before a cold reload",prior.catalog());
                com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdWarmCache.releaseForGameplay();
                onRender(()->UimsdScene.openNow(TitleScene.class));
                waitUntil("cold UIMSD model loads and draws",()->{
                    if(!(Game.scene() instanceof UimsdScene)||Game.switchingScene())return false;
                    UimsdModel current=(UimsdModel)uimsd("model");
                    return current!=null&&current.state()==UimsdModel.State.READY;
                },60000);
                onRender(()->{try{assertEquals("cold reopening preserves all synthetic runs",72,((UimsdModel)uimsd("model")).catalog().dataset("runs").rows.size());}catch(Exception error){throw new RuntimeException(error);}});
                Runtime heap=Runtime.getRuntime();Log.w("APDPERF","UIMSD_REOPEN_HEAP used="+(heap.totalMemory()-heap.freeMemory())+" limit="+heap.maxMemory());
			} finally {
				onRender(()->{
					com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder.configure(originalRoot,recording);
					com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData.viewDemo(oldDemo);
					com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdGuideSeen(oldGuide);
					com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdUnlocked(oldUnlocked);
					Game.invalidateRetainedReturn(); ShatteredPixelDungeon.switchNoFade(TitleScene.class);
				});
				waitUntil("title restored after timing",()->Game.scene() instanceof TitleScene&&!Game.switchingScene(),30000);
				reset.invoke(null);
				assertTrue("only the marked temporary demo is deleted",com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData.delete(demo));
				base.delete();
			}
		}
	}

	private static void waitUntil( String what, java.util.concurrent.Callable<Boolean> c, long millis ) throws Exception {
		long end = System.currentTimeMillis() + millis;
		while (System.currentTimeMillis() < end){
			if (c.call()) return;
			Thread.sleep(100);
		}
		throw new AssertionError("timed out waiting for " + what);
	}

	//the system's Back key, exactly as the Back button does it
	private static void pressBack() throws Exception {
		Instrumentation in = InstrumentationRegistry.getInstrumentation();
		in.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
		Thread.sleep(500);
	}

	@SuppressWarnings("unchecked")
	private static List<Gizmo> membersOf( Group g ) throws Exception {
		Field f = Group.class.getDeclaredField("members");
		f.setAccessible(true);
		List<Gizmo> list = (List<Gizmo>) f.get(g);
		return list == null ? new ArrayList<Gizmo>() : new ArrayList<>(list);
	}

	private static int windows() throws Exception {
		int n = 0;
		for (Gizmo g : membersOf(Game.scene())) if (g instanceof Window && ((Window) g).alive) n++;
		return n;
	}

	private static Object uimsd( String name ) throws Exception {
		Field f = UimsdScene.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(null);
	}

	@Test
	public void ordinarySettingsBackClosesOnlyTheWindowWithoutRebuildingTheScene() throws Exception {
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)){
			enterTitle();
			final AtomicReference<com.watabou.noosa.Scene> sceneBefore = new AtomicReference<>();
			final AtomicInteger before = new AtomicInteger();
			onRender(() -> {try{
				sceneBefore.set(Game.scene()); before.set(windows());
				ShatteredPixelDungeon.scene().add(new com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings());
			}catch(Exception e){throw new RuntimeException(e);}});
			waitUntil("ordinary Settings window", () -> windows() == before.get() + 1, 30000);
			onRender(()->assertTrue("Settings fixture must still be on its opening scene before Back",Game.scene()==sceneBefore.get()));
			pressBack();
			waitUntil("ordinary Settings window closed", () -> windows() == before.get(), 30000);
			assertTrue("ordinary Settings Back must leave the current scene intact", Game.scene() == sceneBefore.get());
		}
	}

	@Test
	public void journalTabChangesKeepTheSameSceneAndOnlyReplaceTheCurrentTab() throws Exception {
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
			enterTitle();
			onRender(() -> { JournalScene.lastIDX = 0; ShatteredPixelDungeon.switchScene(JournalScene.class); });
			waitUntil("the Journal scene", () -> Game.scene() instanceof JournalScene, 30000);
			final com.watabou.noosa.Scene original = Game.scene();
			final Field currentTab = JournalScene.class.getDeclaredField("currentTab");
			currentTab.setAccessible(true);
			final Method selectTab = JournalScene.class.getDeclaredMethod("selectTab", int.class);
			selectTab.setAccessible(true);
			for (int tab : new int[]{1, 2, 3, 0}) {
				onRender(() -> {
					try {
						selectTab.invoke(original, tab);
						assertEquals(tab, JournalScene.lastIDX);
						assertTrue("Journal tab changes keep the current scene", Game.scene() == original);
						assertTrue("Journal tab changes do not request a scene reset", !Game.switchingScene());
						assertTrue("selected tab must be attached", ((Gizmo)currentTab.get(original)).parent == original);
					} catch (Exception exception) {
						throw new RuntimeException(exception);
					}
				});
			}
		}
	}

	@Test
	public void backWalksBackwardsOneStepPerPress() throws Exception {
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)){
			long end = System.currentTimeMillis() + 120000;
			while ((Gdx.app == null || Gdx.files == null || Game.scene() == null) && System.currentTimeMillis() < end) Thread.sleep(250);
			assertNotNull("the game did not start", Game.scene());
			enterTitle();
			Thread.sleep(2500);

			//1. two dialogs over the title: each Back closes ONE, the newest first, and the title stays
			final int base = windows();
			onRender(new Runnable(){
				public void run(){
					ShatteredPixelDungeon.scene().add(new WndAPDSettings());
					ShatteredPixelDungeon.scene().addToFront(new WndMessage("Back test"));
				}
			});
			Thread.sleep(500);
			assertEquals(base + 2, windows());
			pressBack();
			assertEquals("one Back closes one dialog", base + 1, windows());
			assertTrue(Game.scene() instanceof TitleScene);
			pressBack();
			assertEquals("the next Back closes the one under it", base, windows());
			assertTrue("the title screen is still there", Game.scene() instanceof TitleScene);
			Log.w(TAG, "Back closed two dialogs one at a time");

			//2. Enter UIMSD directly from the title because this test covers scene navigation, not the private-build demo unlock gate;
			//a tour on its sample history; Back ends the tour first, the next Back returns to the title
			onRender(new Runnable(){ public void run(){ UimsdScene.openNow(TitleScene.class); } });
			waitUntil("the UIMSD scene", new java.util.concurrent.Callable<Boolean>(){ public Boolean call(){ return Game.scene() instanceof UimsdScene; } }, 30000);
			Thread.sleep(2500);
			final Method begin = UimsdScene.class.getDeclaredMethod("beginTour", int.class, boolean.class);
			begin.setAccessible(true);
			final Method backDuringPreparation = UimsdScene.class.getDeclaredMethod("onBackPressed");
			backDuringPreparation.setAccessible(true);

			//Back must cancel the loading phase itself. The generation increment is the
			//permanent guard that prevents the still-running worker from publishing later.
			onRender(new Runnable(){
				public void run(){
					try {
						begin.invoke(Game.scene(), 0, false);
						assertTrue("tour entered its preparation phase", (Boolean) uimsd("tutorialPreparing"));
						long startedGeneration = (Long) uimsd("tourGeneration");
						backDuringPreparation.invoke(Game.scene());
						assertTrue("Back ends tour preparation immediately", !((Boolean) uimsd("tutorialPreparing")));
						assertNull("cancelled preparation has no tutorial", uimsd("tutorial"));
						assertEquals("cancelled preparation restores the player's model", uimsd("mainModel"), uimsd("model"));
						assertTrue("cancelled preparation returns to Home", (Boolean) uimsd("atHome"));
						assertTrue("Back invalidates the preparing worker", (Long) uimsd("tourGeneration") > startedGeneration);
					} catch (Exception e){
						throw new RuntimeException(e);
					}
				}
			});
			Thread.sleep(500);
			assertNull("invalidated worker never publishes after Back", uimsd("tutorial"));
			assertTrue("UIMSD stays open after cancelling preparation", Game.scene() instanceof UimsdScene);
			Log.w(TAG, "Back cancelled UIMSD tour preparation and invalidated its worker");
			onRender(new Runnable(){
				public void run(){
					try {
						begin.invoke(Game.scene(), 0, false);
					} catch (Exception e){
						throw new RuntimeException(e);
					}
				}
			});
			waitUntil("the tour", new java.util.concurrent.Callable<Boolean>(){
				public Boolean call() throws Exception { return uimsd("tutorial") != null && !((Boolean) uimsd("tutorialPreparing")); } }, 60000);
			Thread.sleep(1500);

			//2b. dialogs of the question builder, one over the other (built on the tour's sample history)
			final UimsdModel m = (UimsdModel) uimsd("model");
			assertNotNull(m);
			final int beforeAsk = windows();
			onRender(new Runnable(){
				public void run(){
					Catalog cat = m.catalog();
					WndUimsdAsk.Host host = new WndUimsdAsk.Host(){
						public void openAsTable( Query q, Runnable back ){}
					};
					Question q = Question.about(Questions.askable(cat).get(0).def.id);
					WndUimsdAsk.builder(cat, m.currentRuleset(), host, q);
					WndUimsdAsk.answer(cat, m.currentRuleset(), host, q);
				}
			});
			Thread.sleep(500);
			assertEquals("the answer is over the builder", beforeAsk + 2, windows());
			pressBack();
			assertEquals("Back from the answer returns to the builder", beforeAsk + 1, windows());
			pressBack();
			assertEquals("Back from the builder closes it", beforeAsk, windows());
			assertTrue(Game.scene() instanceof UimsdScene);
			Log.w(TAG, "Back walked back through the question builder");

			//a dialog the tour has open closes first (newest first); the tour itself ends on the Back after those, and the screen stays
			int presses = 0, open = windows();
			Log.w(TAG, "before ending the tour: " + open + " window(s), tutorial=" + uimsd("tutorial") + ", preparing=" + uimsd("tutorialPreparing"));
			while (uimsd("tutorial") != null && presses <= open){
				pressBack();
				presses++;
				Log.w(TAG, "after Back " + presses + ": scene=" + (Game.scene() == null ? "none" : Game.scene().getClass().getSimpleName()) + ", windows=" + windows()
						+ ", tutorial=" + (uimsd("tutorial") != null) + ", atHome=" + uimsd("atHome"));
				assertTrue("Back ends the tour first (press " + presses + ")", Game.scene() instanceof UimsdScene);
			}
			Log.w(TAG, "the tour ended after " + presses + " Back press(es); windows now " + windows());
			assertNull("the tour is over", uimsd("tutorial"));
			pressBack();
			waitUntil("the title again", new java.util.concurrent.Callable<Boolean>(){ public Boolean call(){ return Game.scene() instanceof TitleScene; } }, 15000);
			Log.w(TAG, "Back left the tour, then UIMSD, one step each");
		}
	}
}


