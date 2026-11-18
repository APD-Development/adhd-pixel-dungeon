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
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.UimsdUnlock;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.UimsdGate;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndDemoUnlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

//The Demo Unlock on a real device, with the gate switched on for the test: UIMSD is locked, the warning appears, "I LIKE CONFUSION" makes the
//ten demo runs and opens UIMSD, the unlock and the used-up button are permanent, and deleting the demo data changes neither. The player's own
//settings and history are put back afterwards.
@RunWith(AndroidJUnit4.class)
public class DemoUnlockDeviceTest {

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

	private static Window top() throws Exception {
		Window w = null;
		for (Gizmo g : membersOf(Game.scene())) if (g instanceof Window && ((Window) g).alive) w = (Window) g;
		return w;
	}

	//what pressing one of the pop-up's two buttons does: it hides the card and runs the choice
	private static void choose( final Window w, final boolean confusion ) throws Exception {
		assertTrue("the pop-up is on top", w instanceof WndDemoUnlock);
		Field f = WndDemoUnlock.class.getDeclaredField("choice");
		f.setAccessible(true);
		final WndDemoUnlock.Choice c = (WndDemoUnlock.Choice) f.get(w);
		onRender(new Runnable(){
			public void run(){
				w.hide();
				if (confusion) c.confusion(); else c.playRuns();
			}
		});
		Thread.sleep(400);
	}

	//the two permanent flags as the game keeps them
	private static UimsdUnlock.Flags settingsFlags(){
		return new UimsdUnlock.Flags(){
			public boolean unlocked(){ return SPDSettings.apdUimsdUnlocked(); }
			public void setUnlocked(){ SPDSettings.apdUimsdUnlocked(true); }
			public boolean demoUsed(){ return SPDSettings.apdDemoUnlockUsed(); }
			public void setDemoUsed(){ SPDSettings.apdDemoUnlockUsed(true); }
			public boolean tourUnlocked(){ return SPDSettings.apdUimsdTourUnlocked(); }
			public void setTourUnlocked(){ SPDSettings.apdUimsdTourUnlocked(true); }
			public boolean promptPending(){ return SPDSettings.apdTenthPending(); }
			public void setPromptPending(){ SPDSettings.apdTenthPending(true); }
			public boolean promptShown(){ return SPDSettings.apdTenthShown(); }
			public void setPromptShown(){ SPDSettings.apdTenthShown(true); }
		};
	}

	@Test
	public void theDemoUnlockMakesTenDemoRunsOpensUimsdForGoodAndItsButtonIsUsedUp() throws Exception {
		boolean unlockedWas = SPDSettings.apdUimsdUnlocked();
		boolean usedWas = SPDSettings.apdDemoUnlockUsed();
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)){
			long end = System.currentTimeMillis() + 120000;
			while ((Gdx.app == null || Gdx.files == null || Game.scene() == null) && System.currentTimeMillis() < end) Thread.sleep(250);
			assertNotNull("the game did not start", Game.scene());
			long titleBy = System.currentTimeMillis() + 45000;
			while (!(Game.scene() instanceof TitleScene) && System.currentTimeMillis() < titleBy) Thread.sleep(250);
			if (!(Game.scene() instanceof TitleScene)){
				onRender(new Runnable(){ public void run(){ com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon.switchNoFade(TitleScene.class); } });
				waitUntil("the title scene", new java.util.concurrent.Callable<Boolean>(){ public Boolean call(){ return Game.scene() instanceof TitleScene; } }, 60000);
			}
			Thread.sleep(2500);

			final File real = ApdRecorder.historyRoot();
			assertNotNull(real);
			final File demo = DemoData.demoRootFor(real);
			DemoData.delete(demo);
			SPDSettings.apdUimsdUnlocked(false);
			SPDSettings.apdDemoUnlockUsed(false);
			UimsdGate.forceGateOnForTest(true);

			//locked: tapping UIMSD shows the warning pop-up (only then), not UIMSD
			onRender(new Runnable(){ public void run(){ UimsdScene.open(TitleScene.class); } });
			Thread.sleep(600);
			Window warning = top();
			assertTrue("the warning pop-up is up", warning instanceof WndDemoUnlock);
			assertFalse("and UIMSD did not open", Game.scene() instanceof UimsdScene);
			assertFalse("the tour is closed", UimsdGate.tourAvailable());
			//declining first: nothing is made and nothing is used up
			choose(warning, false);   //I'LL PLAY 10 RUNS
			assertFalse(demo.exists());
			assertFalse(SPDSettings.apdUimsdUnlocked() || SPDSettings.apdDemoUnlockUsed());
			assertFalse(Game.scene() instanceof UimsdScene);
			Log.w(TAG, "demo unlock: declining changed nothing");

			//now accept
			onRender(new Runnable(){ public void run(){ UimsdScene.open(TitleScene.class); } });
			Thread.sleep(600);
			assertTrue(top() instanceof WndDemoUnlock);
			choose(top(), true);   //I LIKE CONFUSION
			waitUntil("UIMSD to open on the demo data", new java.util.concurrent.Callable<Boolean>(){ public Boolean call(){ return Game.scene() instanceof UimsdScene; } }, 60000);
			assertTrue("the ten demo runs are in their own marked folder", DemoData.isSyntheticRoot(demo));
			File[] logs = new File(demo, "runs").listFiles();
			int n = 0;
			for (File f : logs) if (f.getName().endsWith(".apdlog")) n++;
			assertTrue("ten demo runs, one of them in two parts (" + n + " logs)", n >= UimsdUnlock.DEMO_RUNS);
			assertTrue(SPDSettings.apdUimsdUnlocked());
			assertTrue(SPDSettings.apdDemoUnlockUsed());
			assertFalse("UIMSD is open but the guided tour is still closed", UimsdGate.tourAvailable());
			assertFalse(SPDSettings.apdUimsdTourUnlocked());
			Log.w(TAG, "demo unlock: " + n + " demo logs made, UIMSD opened, the tour stayed closed");

			//from now on the button just opens UIMSD, with no pop-up
			onRender(new Runnable(){ public void run(){ UimsdScene.open(TitleScene.class); } });
			Thread.sleep(600);
			assertFalse("no pop-up any more", top() instanceof WndDemoUnlock);

			//delete the demo data: UIMSD stays unlocked and the button stays gone
			assertTrue(UimsdUnlock.deleteDemoData(real));
			assertFalse(demo.exists());
			assertTrue(SPDSettings.apdUimsdUnlocked());
			assertTrue(SPDSettings.apdDemoUnlockUsed());
			assertFalse("the button is not offered again", UimsdUnlock.demoButtonAvailable(settingsFlags(), real, true));
			Log.w(TAG, "demo unlock: deleting the demo data left the unlock and the used-up button as they were");
		} finally {
			UimsdGate.forceGateOnForTest(false);
			DemoData.viewDemo(false);
			SPDSettings.apdUimsdUnlocked(unlockedWas);
			SPDSettings.apdDemoUnlockUsed(usedWas);
			try {
				File real = ApdRecorder.historyRoot();
				if (real != null) DemoData.delete(DemoData.demoRootFor(real));
			} catch (Throwable ignored){
			}
		}
	}
}
