package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.graphics.Point;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.DifficultyBar;
import com.shatteredpixel.shatteredpixeldungeon.windows.APDSettingsPane;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

//Difficulty belongs to the run being set up, not to the app's settings: the character screen shows 1 to 5 right above Start, a touch on one
//sets what the NEXT run starts with, and APD Settings no longer has the control. Real touches are sent through the game's own pointer queue.
@RunWith(AndroidJUnit4.class)
public class RunSetupDifficultyDeviceTest {

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
		assertTrue("the render thread did not answer", done.await(60, TimeUnit.SECONDS));
		if (failure.get() != null) throw new AssertionError("failed on the render thread: " + failure.get(), failure.get());
	}

	private static void waitUntil( String what, java.util.concurrent.Callable<Boolean> c, long millis ) throws Exception {
		long end = System.currentTimeMillis() + millis;
		while (System.currentTimeMillis() < end){
			if (c.call()) return;
			Thread.sleep(100);
		}
		throw new AssertionError("timed out waiting for " + what);
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

	private static void tap( com.watabou.utils.Point at ) throws Exception {
		PointerEvent.addPointerEvent(new PointerEvent(at.x, at.y, 9, PointerEvent.Type.DOWN, PointerEvent.LEFT));
		Thread.sleep(150);
		PointerEvent.addPointerEvent(new PointerEvent(at.x, at.y, 9, PointerEvent.Type.UP, PointerEvent.LEFT));
		Thread.sleep(500);
	}

	@Test
	public void difficultyIsChosenOnTheCharacterScreenAndNotInSettings() throws Exception {
		int before = SPDSettings.apdDifficulty();
		boolean introBefore = SPDSettings.intro();
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)){
			long end = System.currentTimeMillis() + 120000;
			while ((Gdx.app == null || Gdx.files == null || Game.scene() == null) && System.currentTimeMillis() < end) Thread.sleep(250);
			assertNotNull("the game did not start", Game.scene());
			//normally the title; say which scene the game really opened on, and go on from any scene that has finished starting
			long titleBy = System.currentTimeMillis() + 60000;
			while (!(Game.scene() instanceof TitleScene) && System.currentTimeMillis() < titleBy) Thread.sleep(250);
			android.util.Log.w("APDUI", "the game opened on " + (Game.scene() == null ? "no scene" : Game.scene().getClass().getSimpleName()));
			Thread.sleep(2500);

			//APD Settings no longer has a difficulty control
			try {
				APDSettingsPane.class.getDeclaredField("optDifficulty");
				fail("APD Settings must not have a difficulty slider any more");
			} catch (NoSuchFieldException expected){
				//good
			}

			//the character screen, with a hero picked
			SPDSettings.intro(true);
			onRender(new Runnable(){ public void run(){ ShatteredPixelDungeon.switchNoFade(HeroSelectScene.class); } });
			waitUntil("the character screen", new java.util.concurrent.Callable<Boolean>(){
				public Boolean call(){ return Game.scene() instanceof HeroSelectScene; } }, 30000);
			Thread.sleep(2000);
			final Method pick = HeroSelectScene.class.getDeclaredMethod("setSelectedHero", HeroClass.class);
			pick.setAccessible(true);
			onRender(new Runnable(){
				public void run(){
					try {
						SPDSettings.intro(true);
						pick.invoke(Game.scene(), HeroClass.WARRIOR);
						assertTrue("exercise actual hero-selection layout with intro still true", SPDSettings.intro());
						DifficultyBar freshBar = find(Game.scene(), DifficultyBar.class);
						assertNotNull("fresh player difficulty exists", freshBar);
						assertTrue("fresh player difficulty is visible and usable", freshBar.visible && freshBar.active);
					} catch (Exception e){
						throw new RuntimeException(e);
					}
				}
			});
			Thread.sleep(1000);
			final DifficultyBar bar = find(Game.scene(), DifficultyBar.class);
			assertNotNull("the difficulty bar is on the character screen", bar);
			assertTrue("and it is visible", bar.visible);

			// A fresh tutorial flag must not hide or leave the gear menu empty.
			final AtomicReference<com.watabou.utils.Point> gearCentre = new AtomicReference<>();
			onRender(new Runnable(){ public void run(){
				try {
					SPDSettings.intro(true);
					pick.invoke(Game.scene(), HeroClass.WARRIOR);
					Field f = HeroSelectScene.class.getDeclaredField("btnOptions");
					f.setAccessible(true);
					com.shatteredpixel.shatteredpixeldungeon.ui.IconButton gear =
							(com.shatteredpixel.shatteredpixeldungeon.ui.IconButton) f.get(Game.scene());
					assertTrue("fresh player gear button is visible and active", gear.visible && gear.active);
					gearCentre.set(gear.camera().cameraToScreen(gear.left()+gear.width()/2f, gear.top()+gear.height()/2f));
				} catch (Exception e) { throw new RuntimeException(e); }
			} });
			tap(gearCentre.get());
			onRender(new Runnable(){ public void run(){
				try {
					Field f = HeroSelectScene.class.getDeclaredField("optionsPane");
					f.setAccessible(true);
					com.watabou.noosa.ui.Component pane = (com.watabou.noosa.ui.Component) f.get(Game.scene());
					assertTrue("gear tap opens the options", pane.visible && pane.active);
					Field buttons = pane.getClass().getDeclaredField("buttons");
					buttons.setAccessible(true);
					List<?> entries = (List<?>) buttons.get(pane);
					assertTrue("seed, daily and challenges are populated", entries.size() >= 3);
					for (Object entry : entries) {
						com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton button =
								(com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton) entry;
						assertTrue("menu option has readable text", !button.text().trim().isEmpty());
						assertTrue("menu option fits inside its panel", button.left() >= pane.left() && button.right() <= pane.right()+1);
					}
				} catch (Exception e) { throw new RuntimeException(e); }
			} });
			tap(gearCentre.get());

			//touch each level for real; the setting follows, and the label says what it is
			int[] levels = { 4, 1, 5, 3 };
			for (final int level : levels){
				final AtomicReference<com.watabou.utils.Point> centre = new AtomicReference<>();
				onRender(new Runnable(){
					public void run(){
						try {
							Field f = DifficultyBar.class.getDeclaredField("segments");
							f.setAccessible(true);
							Object[] segs = (Object[]) f.get(bar);
							com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton b = (com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton) segs[level - 1];
							centre.set(b.camera().cameraToScreen(b.left() + b.width() / 2f, b.top() + b.height() / 2f));
						} catch (Exception e){
							throw new RuntimeException(e);
						}
					}
				});
				tap(centre.get());
				assertEquals("a touch on " + level + " chose difficulty " + level, level, SPDSettings.apdDifficulty());
				Log.w(TAG, "difficulty " + level + " chosen on the character screen");
			}
			assertEquals("3 is the normal game", "Normal", DifficultyBar.nameOf(3));
		} finally {
			SPDSettings.apdDifficulty(before);
			SPDSettings.intro(introBefore);
		}
	}
	@Test
	public void heroButtonsStayAboveNavigationBarAndRemainSelectable() throws Exception {
		com.watabou.utils.PlatformSupport before = Game.platform;
		HeroClass selectedBefore = GamesInProgress.selectedClass;
		boolean landscapeBefore = SPDSettings.landscape();
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
			waitUntil("game startup", () -> Gdx.app != null && Game.scene() != null, 120000);
			Thread.sleep(2000);
			onRender(() -> {
				SPDSettings.landscape(false);
				Game.platform = new AndroidPlatformSupport() {
					@Override public com.watabou.utils.RectF getSafeInsets(int level) {
						com.watabou.utils.RectF result = super.getSafeInsets(level);
						result.bottom = Math.max(result.bottom, Game.height * 0.12f);
						return result;
					}
				};
				GamesInProgress.selectedClass = null;
				ShatteredPixelDungeon.switchNoFade(HeroSelectScene.class);
			});
			waitUntil("hero screen", () -> Game.scene() instanceof HeroSelectScene, 30000);
			Thread.sleep(1500);
			AtomicReference<com.watabou.utils.Point> warrior = new AtomicReference<>();
			onRender(() -> {
				try {
					Field buttons = HeroSelectScene.class.getDeclaredField("heroBtns");
					buttons.setAccessible(true);
					List<?> heroes = (List<?>)buttons.get(Game.scene());
					assertEquals("all six hero choices exist", 6, heroes.size());
					Field safeInsets = HeroSelectScene.class.getDeclaredField("insets");
					safeInsets.setAccessible(true);
					com.watabou.utils.RectF safe = (com.watabou.utils.RectF)safeInsets.get(Game.scene());
					for (Object raw : heroes) {
						com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton button =
								(com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton)raw;
						assertTrue("hero stays above blocked navigation area",
								button.bottom() <= com.watabou.noosa.Camera.main.height - safe.bottom + 0.01f);
						assertTrue("hero fits horizontally", button.left() >= safe.left - 0.01f
								&& button.right() <= com.watabou.noosa.Camera.main.width - safe.right + 0.01f);
						assertTrue("hero is visible and tappable", button.visible && button.active);
					}
					com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton first =
							(com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton)heroes.get(0);
					warrior.set(first.camera().cameraToScreen(first.left()+first.width()/2, first.top()+first.height()/2));
				} catch (Exception failure) { throw new RuntimeException(failure); }
			});
			tap(warrior.get());
			assertEquals("real pointer selects Warrior above the bar", HeroClass.WARRIOR, GamesInProgress.selectedClass);
		} finally {
			Game.platform = before;
			GamesInProgress.selectedClass = selectedBefore;
			SPDSettings.landscape(landscapeBefore);
		}
	}

	@Test
	public void navigationOverlapReservesVisibleBarsEvenWithFullscreenRequested() {
		boolean fullscreenBefore = SPDSettings.fullscreen();
		try {
			SPDSettings.fullscreen(true);
			android.view.WindowInsets visible = new android.view.WindowInsets.Builder()
					.setInsets(android.view.WindowInsets.Type.navigationBars(), android.graphics.Insets.of(0, 0, 0, 80))
					.setVisible(android.view.WindowInsets.Type.navigationBars(), true).build();
			com.watabou.utils.RectF bars = AndroidPlatformSupport.visibleNavigationInsets(visible);
			assertEquals(80f, bars.bottom, 0f);
			com.watabou.utils.RectF window = new com.watabou.utils.RectF(0, 0, 1080, 2400);
			assertEquals(80f, AndroidPlatformSupport.navigationOverlap(bars, window, window).bottom, 0f);
			assertEquals("already excluded bar is not subtracted twice", 0f,
					AndroidPlatformSupport.navigationOverlap(bars, window,
							new com.watabou.utils.RectF(0, 0, 1080, 2320)).bottom, 0f);
			assertEquals("partial surface overlap reserves only its covered area", 40f,
					AndroidPlatformSupport.navigationOverlap(bars, window,
							new com.watabou.utils.RectF(0, 0, 1080, 2360)).bottom, 0f);
			com.watabou.utils.RectF side = new com.watabou.utils.RectF(0, 0, 70, 0);
			assertEquals(70f, AndroidPlatformSupport.navigationOverlap(side, window, window).right, 0f);
			assertEquals(0f, AndroidPlatformSupport.visibleNavigationInsets(null).bottom, 0f);
		} finally { SPDSettings.fullscreen(fullscreenBefore); }
	}


    @Test
    public void clericLoreFirstRunPopupIsVisibleAndBackContinuesExactlyOnce() throws Exception {
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            waitUntil("game startup", () -> Gdx.files != null && Game.scene() != null, 120000);
            onRender(() -> ShatteredPixelDungeon.switchNoFade(HeroSelectScene.class));
            waitUntil("hero selection", () -> Game.scene() instanceof HeroSelectScene, 30000);
            Thread.sleep(2000);
            java.util.concurrent.atomic.AtomicInteger continued = new java.util.concurrent.atomic.AtomicInteger();
            AtomicReference<Object> oldEngine = new AtomicReference<>();
            AtomicReference<com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions> popup = new AtomicReference<>();
            Field engine = com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.class.getDeclaredField("instance");
            engine.setAccessible(true);
            String beforeFrequency = SPDSettings.apdGrudgePref("apd_grudge_popups");
            try {
                onRender(() -> {
                    try {
                        Method chooseCleric = HeroSelectScene.class.getDeclaredMethod("setSelectedHero", HeroClass.class);
                        chooseCleric.setAccessible(true);
                        chooseCleric.invoke(Game.scene(), HeroClass.CLERIC);
                        assertTrue("the packaged private asset must enable lore", com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.grudge());
                        assertTrue("Cleric remains playable", com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.clericPlayable());
                        oldEngine.set(engine.get(null));
                        java.util.List<com.shatteredpixel.shatteredpixeldungeon.apd.humor.Line> lines = com.shatteredpixel.shatteredpixeldungeon.apd.humor.ClericGrudge.loadPacks(new com.shatteredpixel.shatteredpixeldungeon.apd.humor.ClericGrudge.PackSource() {
                            public java.util.List<String> names() {
                                return java.util.Arrays.asList(Gdx.files.internal("apd/humor/packs.txt").readString("UTF-8").trim().split("\\r?\\n"));
                            }
                            public String read(String name) { return Gdx.files.internal("apd/humor/" + name).readString("UTF-8"); }
                        });
                        assertEquals(50, lines.size());
                        // Isolate the test ledger; never reset or overwrite the player's collection.
                        engine.set(null, new com.shatteredpixel.shatteredpixeldungeon.apd.humor.ClericGrudge(lines,
                            new com.shatteredpixel.shatteredpixeldungeon.apd.humor.Ledger(null), new java.util.Random(7), true));
                        SPDSettings.apdGrudgePref("apd_grudge_popups", "RARER");
                        assertTrue(com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.firstClericRun(continued::incrementAndGet));
                        popup.set(find(Game.scene(), com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class));
                        assertNotNull("the actual first-run window is on screen", popup.get());
                        assertTrue(popup.get().visible && popup.get().active);
                        assertEquals(0, continued.get());
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
                Thread.sleep(600);
                android.graphics.Bitmap shot = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                assertNotNull(shot);
                java.io.File dir = new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "APD-Test-Visuals");
                assertTrue(dir.isDirectory() || dir.mkdirs());
                try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(dir, "cleric-first-run.png"))) {
                    assertTrue(shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out));
                } finally { shot.recycle(); }
                onRender(() -> {
                    popup.get().onBackPressed();
                    assertEquals("Back must continue once", 1, continued.get());
                    assertFalse("the same install must not repeat its collected entry", com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.firstClericRun(continued::incrementAndGet));
                    assertEquals(1, continued.get());
                });
            } finally {
                onRender(() -> {
                    try { engine.set(null, oldEngine.get()); }
                    catch (Exception e) { throw new RuntimeException(e); }
                    SPDSettings.apdGrudgePref("apd_grudge_popups", beforeFrequency);
                });
            }
        }
    }

    // Send a true Android touch through the window dispatcher.
    private static void nativeTapCleric(com.watabou.utils.Point at) throws Exception {
        long down=android.os.SystemClock.uptimeMillis();
        for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}){
            android.view.MotionEvent event=android.view.MotionEvent.obtain(down,android.os.SystemClock.uptimeMillis(),action,at.x,at.y,0);
            event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
            try{assertTrue("Android accepted native Cleric Start touch",
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation().injectInputEvent(event,true));}
            finally{event.recycle();}
            if(action==android.view.MotionEvent.ACTION_DOWN)Thread.sleep(100);
        }
    }

    @Test
    public void clericStartsWithoutPrivateFeatureFlagInsteadOfShowingBlackScreen() throws Exception {
        boolean introBefore = SPDSettings.intro();
        try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)) {
            waitUntil("game startup", () -> Gdx.files != null && Game.scene() != null, 120000);
            final String packagedFeatures = Gdx.files.internal("apd/features.txt").exists()
                    ? Gdx.files.internal("apd/features.txt").readString("UTF-8") : "";
            try {
                onRender(() -> {
                    com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.parse("");
                    assertFalse("test regular-player feature flags",
                            com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.clericPlayable());
                    assertFalse(com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.grudge());
                    SPDSettings.intro(false);
                    GamesInProgress.selectedClass = null;
                    ShatteredPixelDungeon.switchNoFade(HeroSelectScene.class);
                });
                waitUntil("hero selection", () -> Game.scene() instanceof HeroSelectScene, 30000);
                final AtomicReference<com.watabou.utils.Point> start = new AtomicReference<>();
                onRender(() -> {
                    try {
                        int free = GamesInProgress.firstEmpty(HeroClass.CLERIC);
                        assertTrue("never replace an existing Cleric save", free >= 0);
                        GamesInProgress.curSlot = free;
                        Method select = HeroSelectScene.class.getDeclaredMethod("setSelectedHero", HeroClass.class);
                        select.setAccessible(true);
                        select.invoke(Game.scene(), HeroClass.CLERIC);
                        assertEquals(HeroClass.CLERIC, GamesInProgress.selectedClass);
                        Field startField = HeroSelectScene.class.getDeclaredField("startBtn");
                        startField.setAccessible(true);
                        com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton button =
                            (com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton) startField.get(Game.scene());
                        assertTrue("Cleric Start must be usable", button.visible && button.active);
                        start.set(button.camera().cameraToScreen(
                                button.left() + button.width()/2f, button.top() + button.height()/2f));
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
                nativeTapCleric(start.get());
                Thread.sleep(1500);
                onRender(() -> android.util.Log.w(TAG,"CLERIC_START_STATE scene="
                  +(Game.scene()==null?"null":Game.scene().getClass().getName())
                  +" selected="+GamesInProgress.selectedClass+" hero="
                  +(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero==null?"null":
                    com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.heroClass)));
                waitUntil("Cleric entered the actual dungeon",
                    () -> Game.scene() instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
                        && com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero != null
                        && com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.heroClass == HeroClass.CLERIC
                        && com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.ready,
                    90000);
                // Never accept a transient GameScene frame if a later loading scene replaces it.
                Thread.sleep(3000);
                onRender(() -> {
                    assertTrue("Cleric must remain on the dungeon floor after loading",
                            Game.scene() instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
                                && com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero != null
                                && com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.ready
                                && com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.heroClass == HeroClass.CLERIC);
                });
                android.graphics.Bitmap shot = androidx.test.platform.app.InstrumentationRegistry
                    .getInstrumentation().getUiAutomation().takeScreenshot();
                assertNotNull("dungeon screenshot", shot);
                java.io.File dir = new java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES), "APD-Test-Visuals");
                assertTrue(dir.isDirectory() || dir.mkdirs());
                try (java.io.FileOutputStream output = new java.io.FileOutputStream(
                        new java.io.File(dir, "cleric-unflagged-start.png"))) {
                    assertTrue(shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
                } finally { shot.recycle(); }
            } finally {
                com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.parse(packagedFeatures);
                SPDSettings.intro(introBefore);
            }
        }
    }

}

