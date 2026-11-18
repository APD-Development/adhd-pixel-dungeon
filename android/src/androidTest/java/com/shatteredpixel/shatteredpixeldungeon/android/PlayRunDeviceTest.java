package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.util.Log;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemButton;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBlacksmith;
import java.util.ArrayList;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.RecorderStatus;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFactsBuilder;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

//A real run on a real device: start a new Warrior game the way the character screen does, take some turns, save, continue from the save,
//and die. The run's history must be recorded and complete through all of it. Nothing else in the suite plays the game, so this is the
//check that the recording hooks, the save/continue path and the run end work together on Android.
@RunWith(AndroidJUnit4.class)
public class PlayRunDeviceTest {

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

	private static void waitUntil( String what, Callable<Boolean> c, long millis ) throws Exception {
		long end = System.currentTimeMillis() + millis;
		while (System.currentTimeMillis() < end){
			if (c.call()) return;
			Thread.sleep(150);
		}
		throw new AssertionError("timed out waiting for " + what);
	}

	private static RunFacts factsOfLog( String path ) throws Exception {
		ApdRecorder.flushForReading();
		assertNotNull("the recorder has a log for the run", path);
		return RunFactsBuilder.fromLog(RunLogReader.read(new File(path)), "device-run");
	}

	private static Object field(Object target, String name) throws Exception {
		Class<?> type = target.getClass();
		while (type != null){
			try { java.lang.reflect.Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
			catch (NoSuchFieldException ignored){ type = type.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void click(Object button, String method) throws Exception {
		Class<?> type = button.getClass();
		while (type != null){
			try { java.lang.reflect.Method m = type.getDeclaredMethod(method); m.setAccessible(true); m.invoke(button); return; }
			catch (NoSuchMethodException ignored){ type = type.getSuperclass(); }
		}
		throw new NoSuchMethodException(method);
	}

    private static void assertAvailableClassesHaveNoUnlockAnnouncement() throws Exception {
        onRender(()->{try {
            java.lang.reflect.Field global=com.shatteredpixel.shatteredpixeldungeon.Badges.class.getDeclaredField("global");global.setAccessible(true);
            java.lang.reflect.Field dirty=com.shatteredpixel.shatteredpixeldungeon.Badges.class.getDeclaredField("saveNeeded");dirty.setAccessible(true);
            Object previous=global.get(null),previousDirty=dirty.get(null);
            int upgrades=com.shatteredpixel.shatteredpixeldungeon.Statistics.upgradesUsed;
            int sneaks=com.shatteredpixel.shatteredpixeldungeon.Statistics.sneakAttacks;
            int thrown=com.shatteredpixel.shatteredpixeldungeon.Statistics.thrownAttacks;
            int strength=Dungeon.hero.STR;
            com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon previousWeapon=Dungeon.hero.belongings.weapon;
            String seed=Dungeon.customSeedText;
            com.watabou.utils.Signal<String> oldLog=com.shatteredpixel.shatteredpixeldungeon.utils.GLog.update;
            java.util.List<String> messages=new java.util.ArrayList<>();
            try {
                global.set(null,new java.util.HashSet<com.shatteredpixel.shatteredpixeldungeon.Badges.Badge>());
                Dungeon.customSeedText="";
                com.shatteredpixel.shatteredpixeldungeon.utils.GLog.update=new com.watabou.utils.Signal<>();
                com.shatteredpixel.shatteredpixeldungeon.utils.GLog.update.add(s->{messages.add(s);return false;});
                assertEquals(6,HeroClass.values().length);
                for(HeroClass cls:HeroClass.values())assertTrue("already playable: "+cls,cls.isUnlocked());
                for(boolean identified:new boolean[]{false,true}) {
                    com.shatteredpixel.shatteredpixeldungeon.Statistics.upgradesUsed=1;
                    com.shatteredpixel.shatteredpixeldungeon.Statistics.sneakAttacks=10;
                    com.shatteredpixel.shatteredpixeldungeon.Statistics.thrownAttacks=10;
                    com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword();
                    weapon.level(2);weapon.levelKnown=identified;weapon.cursedKnown=identified;
                    Dungeon.hero.STR=20;Dungeon.hero.belongings.weapon=weapon;
                    com.shatteredpixel.shatteredpixeldungeon.Badges.validateMageUnlock();
                    com.shatteredpixel.shatteredpixeldungeon.Badges.validateRogueUnlock();
                    com.shatteredpixel.shatteredpixeldungeon.Badges.validateHuntressUnlock();
                    com.shatteredpixel.shatteredpixeldungeon.Badges.validateDuelistUnlock();
                    com.shatteredpixel.shatteredpixeldungeon.Badges.validateClericUnlock();
                    assertTrue("no redundant native unlock announcements",messages.isEmpty());
                    assertEquals("all five original conditions still record progression",5,com.shatteredpixel.shatteredpixeldungeon.Badges.allUnlocked().size());
                    com.watabou.utils.Bundle stored=new com.watabou.utils.Bundle();
                    com.shatteredpixel.shatteredpixeldungeon.Badges.store(stored,new java.util.HashSet<>(com.shatteredpixel.shatteredpixeldungeon.Badges.allUnlocked()));
                    global.set(null,com.shatteredpixel.shatteredpixeldungeon.Badges.restore(stored));
                }
                for(HeroClass cls:HeroClass.values())assertTrue(cls.isUnlocked());
            } finally {
                global.set(null,previous);dirty.set(null,previousDirty);
                com.shatteredpixel.shatteredpixeldungeon.Statistics.upgradesUsed=upgrades;
                com.shatteredpixel.shatteredpixeldungeon.Statistics.sneakAttacks=sneaks;
                com.shatteredpixel.shatteredpixeldungeon.Statistics.thrownAttacks=thrown;
                Dungeon.hero.STR=strength;Dungeon.hero.belongings.weapon=previousWeapon;Dungeon.customSeedText=seed;
                com.shatteredpixel.shatteredpixeldungeon.utils.GLog.update=oldLog;
            }
        }catch(Exception e){throw new RuntimeException(e);}});
    }

    private static void assertBossRewardsHaveNoPromotion() throws Exception {
        boolean previous=SPDSettings.supportNagged();
        try {
            for(boolean legacy:new boolean[]{false,true}) {
                waitForGameplayControls();
                onRender(()->{
                    SPDSettings.supportNagged(legacy);
                    com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey key=new com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey(5);
                    int before=com.shatteredpixel.shatteredpixeldungeon.journal.Notes.keyCount(key);
                    assertTrue(key.doPickUp(Dungeon.hero,Dungeon.hero.pos));
                    assertEquals(before+1,com.shatteredpixel.shatteredpixeldungeon.journal.Notes.keyCount(key));
                });
                waitUntil("normal key pickup completes",()->Dungeon.hero.ready,30000);Thread.sleep(500);
                onRender(()->{try {
                    assertTrue("no promotional pickup window",TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndSupportPrompt.class).isEmpty());
                }catch(Exception e){throw new RuntimeException(e);}});
            }
            TopScoresDeviceTest.screenshot("boss-key-no-promotion.png");
            onRender(()->GameScene.show(new com.shatteredpixel.shatteredpixeldungeon.windows.WndVictoryCongrats()));
            Thread.sleep(500);
            TopScoresDeviceTest.screenshot("victory-no-promotion.png");
            onRender(()->{try{
                com.shatteredpixel.shatteredpixeldungeon.windows.WndVictoryCongrats window=TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndVictoryCongrats.class).get(0);
                java.util.List<com.shatteredpixel.shatteredpixeldungeon.ui.RedButton> buttons=TopScoresDeviceTest.find(window,com.shatteredpixel.shatteredpixeldungeon.ui.RedButton.class);
                assertEquals("victory only needs its existing close action",1,buttons.size());
                click(buttons.get(0),"onClick");
            }catch(Exception e){throw new RuntimeException(e);}});
            waitForGameplayControls();
        } finally {SPDSettings.supportNagged(previous);}
    }

    private static void assertGameplayControls() throws Exception {
		com.watabou.noosa.ui.Component toolbar = (com.watabou.noosa.ui.Component) field(Game.scene(), "toolbar");
		assertTrue("toolbar is visible and usable before guidebook acknowledgement", toolbar.visible && toolbar.active);
		com.watabou.noosa.ui.Component status = (com.watabou.noosa.ui.Component) field(Game.scene(), "status");
		assertTrue("health/status remain visible", status.visible && status.active);
		for (String name : new String[]{"btnSearch", "btnWait", "btnInventory"}){
			com.watabou.noosa.ui.Component button = (com.watabou.noosa.ui.Component) field(toolbar, name);
			assertTrue(name + " is visible and active", button.visible && button.active);
		}
		Object[] slots = (Object[]) field(toolbar, "btnQuick");
		assertTrue("quick-use slots exist", slots.length > 0);
		int visibleSlots = 0;
		for (Object slot : slots) if (((com.watabou.noosa.ui.Component) slot).visible) visibleSlots++;
		assertTrue("the current quick-use page is visible", visibleSlots > 0);
	}

	// hero.ready is changed by the actor thread; toolbar.active follows on the next render update.
	// Wait for actual controls instead of racing that update. Persistent disabled controls still fail.
	private static void waitForGameplayControls() throws Exception {
		final AtomicReference<String> state = new AtomicReference<>("not sampled");
		long deadline = System.currentTimeMillis() + 30000;
		while (System.currentTimeMillis() < deadline) {
			final java.util.concurrent.atomic.AtomicBoolean usable = new java.util.concurrent.atomic.AtomicBoolean();
			onRender(() -> {
				try {
					com.watabou.noosa.ui.Component toolbar = (com.watabou.noosa.ui.Component) field(Game.scene(), "toolbar");
					boolean enabled = Dungeon.hero.ready && Dungeon.hero.isAlive() && toolbar.visible && toolbar.active;
					StringBuilder detail = new StringBuilder("heroReady=").append(Dungeon.hero.ready)
							.append(", lastEnabled=").append(field(toolbar, "lastEnabled"));
					for (String name : new String[]{"btnSearch", "btnWait", "btnInventory"}) {
						com.watabou.noosa.ui.Component button = (com.watabou.noosa.ui.Component) field(toolbar, name);
						enabled &= button.visible && button.active;
						detail.append(", ").append(name).append("=").append(button.visible).append("/").append(button.active);
					}
					state.set(detail.toString());
					usable.set(enabled);
				} catch (Exception e) { throw new RuntimeException(e); }
			});
			if (usable.get()) return;
			Thread.sleep(150);
		}
		throw new AssertionError("gameplay controls did not become usable: " + state.get());
	}

    // Real native controls at minimum portrait width and each original toolbar layout.
    private static void assertHotbarViewportMatrix() throws Exception {
        com.watabou.noosa.ui.Component toolbar = (com.watabou.noosa.ui.Component) field(Game.scene(), "toolbar");
        com.watabou.noosa.Camera camera = toolbar.camera();
        int oldWidth = camera.width, oldHeight = camera.height;
        float oldX = toolbar.left(), oldY = toolbar.top(), oldW = toolbar.width(), oldH = toolbar.height();
        String oldMode = SPDSettings.toolbarMode();
        boolean oldFlip = SPDSettings.flipToolbar();
        boolean oldSwap = com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.swappedQuickslots;
        try {
            for (int width : new int[]{135, 139, 140, 160, 180, 320}) {
                camera.resize(width, 240);
                for (String mode : new String[]{"SPLIT", "GROUP", "CENTER"}) {
                    SPDSettings.toolbarMode(mode);
                    for (boolean flip : new boolean[]{false, true}) {
                        SPDSettings.flipToolbar(flip);
                        for (boolean swapped : new boolean[]{false, true}) {
                            com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.swappedQuickslots = swapped;
                            toolbar.setRect(0, 214, width, 26);
                            com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.updateLayout();
                            java.util.List<com.watabou.noosa.ui.Component> controls = new java.util.ArrayList<>();
                            for (String name : new String[]{"btnSearch", "btnWait", "btnInventory", "btnSwap"}) {
                                com.watabou.noosa.ui.Component c=(com.watabou.noosa.ui.Component)field(toolbar,name);
                                if (c.visible) controls.add(c);
                            }
                            for (Object raw : (Object[])field(toolbar,"btnQuick")) {
                                com.watabou.noosa.ui.Component c=(com.watabou.noosa.ui.Component)raw;
                                if(c.visible) controls.add(c);
                            }
                            com.watabou.noosa.ui.Component pager=(com.watabou.noosa.ui.Component)field(toolbar,"btnSwap");
                            if(pager.visible && pager.top()<toolbar.top()) {
                                com.watabou.noosa.ui.Component log=(com.watabou.noosa.ui.Component)field(Game.scene(),"log");
                                assertTrue("Narrow pager must not cover message baseline",log.top()<=pager.top()-2+0.01f);
                                for(Object entry:(java.util.List<?>)field(log,"members"))
                                    if(entry instanceof com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock)
                                        assertTrue("Narrow pager must not cover rendered messages",((com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock)entry).bottom()<=pager.top()-2+0.01f);
                            }
                            for (int i=0;i<controls.size();i++) {
                                com.watabou.noosa.ui.Component a=controls.get(i);
                                String state="width="+width+" mode="+mode+" flipped="+flip+" page="+swapped;
                                assertTrue("usable target "+state,a.width()>=16 && a.height()>=16);
                                assertTrue("in viewport "+state,a.left()>=-0.01f && a.right()<=width+0.01f && a.top()>=0 && a.bottom()<=240);
                                for(int j=i+1;j<controls.size();j++) {
                                    com.watabou.noosa.ui.Component b=controls.get(j);
                                    // SPD intentionally overlaps the pager frame by two border pixels.
                                    float border=(a==pager || b==pager)?2f:0.01f;
                                    assertTrue("distinct touch targets "+state,a.right()<=b.left()+border || b.right()<=a.left()+border || a.bottom()<=b.top()+border || b.bottom()<=a.top()+border);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            camera.resize(oldWidth,oldHeight);SPDSettings.toolbarMode(oldMode);SPDSettings.flipToolbar(oldFlip);
            com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.swappedQuickslots=oldSwap;
            toolbar.setRect(oldX,oldY,oldW,oldH);com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.updateLayout();
        }
    }

    private static void captureAndTapCategoryPager() throws Exception {
        int oldMode=SPDSettings.apdHotbarMode();
        boolean oldPage=com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.swappedQuickslots;
        final com.watabou.utils.Point[] point=new com.watabou.utils.Point[1];
        try {
            onRender(()->{try {
                for(Object child:(java.util.List<?>)field(Game.scene(), "members"))
                    assertFalse("Pager test requires its setup modal to be closed", child instanceof com.shatteredpixel.shatteredpixeldungeon.ui.Window && ((com.shatteredpixel.shatteredpixeldungeon.ui.Window)child).visible);
                SPDSettings.apdHotbarMode(1);com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.updateLayout();
                com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.refresh();
                com.watabou.noosa.ui.Component pager=(com.watabou.noosa.ui.Component)field(field(Game.scene(),"toolbar"),"btnSwap");
                if(pager.visible)point[0]=pager.camera().cameraToScreen(pager.centerX(),pager.centerY());
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(400);TopScoresDeviceTest.screenshot("hotbar-controls.png");
            if(point[0]!=null) {
                final Object[] pager={null};
                onRender(()->{try {
                    com.watabou.noosa.ui.Component current=(com.watabou.noosa.ui.Component)field(field(Game.scene(),"toolbar"),"btnSwap");
                    assertTrue("Rendered pager is active",current.active&&current.visible&&Dungeon.hero.ready);pager[0]=current;
                    point[0]=current.camera().cameraToScreen(current.centerX(),current.centerY());
                }catch(Exception e){throw new RuntimeException(e);}});
                long down=android.os.SystemClock.uptimeMillis();
                android.view.MotionEvent event=android.view.MotionEvent.obtain(down,down,android.view.MotionEvent.ACTION_DOWN,point[0].x,point[0].y,0);
                event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendPointerSync(event);event.recycle();
                final boolean[] received={false};long by=System.currentTimeMillis()+3000;
                while(!received[0]&&System.currentTimeMillis()<by){
                    onRender(()->{try{received[0]=field(pager[0],"pressedButton")==pager[0]&&(Boolean)field(pager[0],"clickReady");}catch(Exception e){throw new RuntimeException(e);}});
                    if(!received[0])Thread.sleep(20);
                }
                assertTrue("Native DOWN reaches the actual pager at "+point[0],received[0]);
                long up=android.os.SystemClock.uptimeMillis();
                event=android.view.MotionEvent.obtain(down,up,android.view.MotionEvent.ACTION_UP,point[0].x,point[0].y,0);
                event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendPointerSync(event);event.recycle();
                final boolean[] switched={false};by=System.currentTimeMillis()+3000;
                while(!switched[0]&&System.currentTimeMillis()<by){onRender(()->switched[0]=com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.swappedQuickslots!=oldPage);if(!switched[0])Thread.sleep(20);}
                Log.i(TAG,"Native pager tap acknowledged; down/up elapsed ms="+(up-down));
                onRender(()->assertEquals("Native pager touch switches page",!oldPage,com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.swappedQuickslots));
            }
        } finally {
            onRender(()->{SPDSettings.apdHotbarMode(oldMode);com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.swappedQuickslots=oldPage;com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.updateLayout();com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.refresh();});
        }
    }

	//Exercise the actual rendered category toolbar without consuming items or changing turns.
	private static void assertCategoryHotbarControls() throws Exception {
		int oldMode = SPDSettings.apdHotbarMode();
        int[] oldCategories = new int[6];
        String[] oldItems = new String[6];
        for (int i=0; i<6; i++) { oldCategories[i] = SPDSettings.apdHotbarCategory(i); oldItems[i] = SPDSettings.apdHotbarItem(i); }
		try {
			SPDSettings.apdHotbarMode(1);
			com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.updateLayout();
			com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.refresh();
			assertGameplayControls();
            assertHotbarViewportMatrix();
			com.watabou.noosa.ui.Component toolbar =
					(com.watabou.noosa.ui.Component) field(Game.scene(), "toolbar");
			Object[] slots = (Object[]) field(toolbar, "btnQuick");
			int count = 0;
			for (Object raw : slots) {
				com.watabou.noosa.ui.Component button = (com.watabou.noosa.ui.Component) raw;
				if (!button.visible) continue;
				count++;
				assertTrue("hotbar touch target remains usable", button.width() >= 16 && button.height() >= 16);
				assertTrue("hotbar fits camera horizontally", button.left() >= -1 && button.right() <= button.camera().width+1);
				for (String name : new String[]{"btnSearch", "btnWait", "btnInventory"}) {
					com.watabou.noosa.ui.Component control = (com.watabou.noosa.ui.Component) field(toolbar, name);
					if (control.visible && button.camera() == control.camera()) {
						assertTrue("category hotbar must not overlap " + name + " slot="+button.left()+","+button.top()+","+button.width()+","+button.height()+" control="+control.left()+","+control.top()+","+control.width()+","+control.height()+" toolbar="+toolbar.left()+","+toolbar.width()+" camera="+button.camera().width+" mode="+SPDSettings.toolbarMode(),
								button.right() <= control.left()+1 || control.right() <= button.left()+1
								|| button.bottom() <= control.top()+1 || control.bottom() <= button.top()+1);
					}
				}
			}
			assertTrue("category positions visible", count > 0);
			if (count < 6) {
				com.watabou.noosa.ui.Component swap = (com.watabou.noosa.ui.Component) field(toolbar, "btnSwap");
				assertTrue("narrow screen can reach all six slots", swap.visible && swap.active);
			}
			com.shatteredpixel.shatteredpixeldungeon.items.Item[] classic =
					new com.shatteredpixel.shatteredpixeldungeon.items.Item[6];
			for (int i=0; i<6; i++) classic[i] = Dungeon.quickslot.getItem(i);
			int hp = Dungeon.hero.HP;
            // This assertion opens either the normal list or the empty-category message.
            // Close exactly the window(s) it creates before testing HUD touch input.
            java.util.Set<Object> windowsBefore = new java.util.HashSet<>((java.util.List<?>)field(Game.scene(), "members"));
			com.shatteredpixel.shatteredpixeldungeon.windows.WndCategoryHotbar.open(0);
			assertEquals("opening category spends no health", hp, Dungeon.hero.HP);
			assertTrue("opening category spends no turn", Dungeon.hero.ready);
			for (int i=0; i<6; i++) org.junit.Assert.assertSame("classic assignments preserved", classic[i], Dungeon.quickslot.getItem(i));
			for (Object child : new java.util.ArrayList<>((java.util.List<?>) field(Game.scene(), "members"))) {
                if (child instanceof com.shatteredpixel.shatteredpixeldungeon.ui.Window && !windowsBefore.contains(child)) {
                    ((com.shatteredpixel.shatteredpixeldungeon.ui.Window) child).hide();
                }
            }
            com.shatteredpixel.shatteredpixeldungeon.items.Item food = null;
            for (com.shatteredpixel.shatteredpixeldungeon.items.Item candidate : Dungeon.hero.belongings) {
                if (candidate instanceof com.shatteredpixel.shatteredpixeldungeon.items.food.Food) { food = candidate; break; }
            }
            assertNotNull("Warrior has food for a specific favorite", food);
            int quantity = food.quantity();
            assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.CategoryHotbar.chooseItem(0, food));
            com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.updateLayout();
            com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.refresh();
            Object first = field(((Object[]) field(toolbar, "btnQuick"))[0], "slot");
            org.junit.Assert.assertSame("specific favorite displays actual inventory item", food, field(field(first, "slot"), "item"));
            assertFalse("generic Item label does not cover favorite", ((com.watabou.noosa.ui.Component) field(first, "categoryLabel")).visible);
            java.lang.reflect.Method hover = first.getClass().getDeclaredMethod("hoverText");
            hover.setAccessible(true);
            assertEquals("specific favorite tooltip names its item", food.name(), hover.invoke(first));
            assertEquals("configuring favorite never consumes it", quantity, food.quantity());
            assertTrue("configuring favorite spends no turn", Dungeon.hero.ready);
            for (int i=0; i<6; i++) org.junit.Assert.assertSame("custom favorite preserves classic assignments", classic[i], Dungeon.quickslot.getItem(i));
		} finally {
            for (int i=0; i<6; i++) { SPDSettings.apdHotbarCategory(i, oldCategories[i]); SPDSettings.apdHotbarItem(i, oldItems[i]); }
			SPDSettings.apdHotbarMode(oldMode);
			com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.updateLayout();
			com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.refresh();
		}
	}

    // Scene membership precedes the next drawn frame. Require the native button's
    // red fill and light text pixels before calling a screenshot visual evidence.
    static void captureRenderedButton(Class<? extends com.shatteredpixel.shatteredpixeldungeon.ui.Window> type,
                                      String label, String filename) throws Exception {
        if ("true".equals(androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("apdSkipPixelEvidence"))) {
            final java.util.concurrent.atomic.AtomicBoolean visible=new java.util.concurrent.atomic.AtomicBoolean();
            onRender(()->{try {
                for(Object member:(java.util.List<?>)field(Game.scene(),"members")) {
                    if(!type.isInstance(member)) continue;
                    com.shatteredpixel.shatteredpixeldungeon.ui.Window window=(com.shatteredpixel.shatteredpixeldungeon.ui.Window)member;
                    if(!window.visible) continue;
                    for(Object child:(java.util.List<?>)field(window,"members"))
                        if(child instanceof com.shatteredpixel.shatteredpixeldungeon.ui.RedButton
                                && ((com.shatteredpixel.shatteredpixeldungeon.ui.RedButton)child).text().equals(label)
                                && ((com.shatteredpixel.shatteredpixeldungeon.ui.RedButton)child).visible) visible.set(true);
                }
            }catch(Exception e){throw new RuntimeException(e);}});
            assertTrue(label + " button is present and visible",visible.get());
            return;
        }
        final int[] stableFrames={0};
        waitUntil(label + " button actually rendered",()->{
            final int[] bounds=new int[4];
            final java.util.concurrent.atomic.AtomicBoolean found=new java.util.concurrent.atomic.AtomicBoolean();
            onRender(()->{try {
                for(Object member:(java.util.List<?>)field(Game.scene(),"members")) {
                    if(!type.isInstance(member))continue;
                    com.shatteredpixel.shatteredpixeldungeon.ui.Window window=(com.shatteredpixel.shatteredpixeldungeon.ui.Window)member;
                    if(!window.visible)continue;
                    for(Object child:(java.util.List<?>)field(window,"members"))
                        if(child instanceof com.shatteredpixel.shatteredpixeldungeon.ui.RedButton
                                && ((com.shatteredpixel.shatteredpixeldungeon.ui.RedButton)child).text().equals(label)) {
                            com.shatteredpixel.shatteredpixeldungeon.ui.RedButton button=(com.shatteredpixel.shatteredpixeldungeon.ui.RedButton)child;
                            com.watabou.utils.Point start=button.camera().cameraToScreen(button.left()+4,button.top()+4);
                            com.watabou.utils.Point end=button.camera().cameraToScreen(button.right()-4,button.bottom()-4);
                            bounds[0]=start.x;bounds[1]=start.y;bounds[2]=end.x;bounds[3]=end.y;found.set(true);
                        }
                }
            }catch(Exception e){throw new RuntimeException(e);}});
            if(!found.get()){stableFrames[0]=0;return false;}
            android.graphics.Bitmap bitmap=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            assertNotNull(bitmap);
            try {
                int red=0,light=0,total=0;
                for(int y=Math.max(0,bounds[1]);y<Math.min(bitmap.getHeight(),bounds[3]);y++)
                    for(int x=Math.max(0,bounds[0]);x<Math.min(bitmap.getWidth(),bounds[2]);x++) {
                        int pixel=bitmap.getPixel(x,y),r=android.graphics.Color.red(pixel),g=android.graphics.Color.green(pixel),b=android.graphics.Color.blue(pixel);total++;
                        if(r>100 && r>2*g && r>2*b)red++;
                        if(r>180 && g>180 && b>180)light++;
                    }
                boolean painted=total>0 && red>total/4 && light>20;
                if(!painted){stableFrames[0]=0;return false;}
                if(++stableFrames[0]<2)return false;
                saveScreenshotBitmap(filename,bitmap);
                return true;
            }finally{bitmap.recycle();}
        },10000);
    }

    private static void assertHealingConfirmationControls() throws Exception {
        int oldThreshold=SPDSettings.healingConfirmationThreshold();
        final com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing[] potion={null};
        final float[] before={0}; final int[] hp={0};
        try {
            final java.util.concurrent.atomic.AtomicReference<com.shatteredpixel.shatteredpixeldungeon.windows.WndHealingConfirmation.ThresholdWindow> thresholdWindow=new java.util.concurrent.atomic.AtomicReference<>();
            onRender(()->{try {
                thresholdWindow.set(new com.shatteredpixel.shatteredpixeldungeon.windows.WndHealingConfirmation.ThresholdWindow());
                GameScene.show(thresholdWindow.get());
                assertEquals("opening settings does not enable confirmation",oldThreshold,SPDSettings.healingConfirmationThreshold());
            }catch(Exception e){throw new RuntimeException(e);}});
            final java.util.concurrent.atomic.AtomicReference<com.watabou.utils.Point> sliderTouch=new java.util.concurrent.atomic.AtomicReference<>();
            onRender(()->{try {
                com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider slider=(com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider)field(thresholdWindow.get(),"percentage");
                sliderTouch.set(slider.camera().cameraToScreen(slider.left()+2,slider.bottom()-7));
            }catch(Exception e){throw new RuntimeException(e);}});
            com.watabou.utils.Point sliderAt=sliderTouch.get();
            com.watabou.input.PointerEvent.addPointerEvent(new com.watabou.input.PointerEvent(sliderAt.x,sliderAt.y,29,com.watabou.input.PointerEvent.Type.DOWN,com.watabou.input.PointerEvent.LEFT));
            Thread.sleep(150);
            com.watabou.input.PointerEvent.addPointerEvent(new com.watabou.input.PointerEvent(sliderAt.x,sliderAt.y,29,com.watabou.input.PointerEvent.Type.UP,com.watabou.input.PointerEvent.LEFT));
            waitUntil("native percentage slider responds to touch",()->{
                final java.util.concurrent.atomic.AtomicBoolean changed=new java.util.concurrent.atomic.AtomicBoolean();
                onRender(()->{try {
                    changed.set(((com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider)field(thresholdWindow.get(),"percentage")).getSelectedValue()==0);
                    assertEquals("editing without saving retains preference",oldThreshold,SPDSettings.healingConfirmationThreshold());
                    if(changed.get())assertEquals("Confirm at 0% HP",((com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock)field(thresholdWindow.get(),"value")).text());
                }catch(Exception e){throw new RuntimeException(e);}});return changed.get();
            },10000);
            captureRenderedButton(com.shatteredpixel.shatteredpixeldungeon.windows.WndHealingConfirmation.ThresholdWindow.class,"Save","healing-threshold-settings.png");
            onRender(()->{try {
                boolean saved=false;
                for(Object child:(java.util.List<?>)field(thresholdWindow.get(),"members"))
                    if(child instanceof com.shatteredpixel.shatteredpixeldungeon.ui.RedButton
                            && ((com.shatteredpixel.shatteredpixeldungeon.ui.RedButton)child).text().equals("Save")) {
                        click(child,"onClick");saved=true;break;
                    }
                assertTrue("native Save control works",saved);assertEquals(0,SPDSettings.healingConfirmationThreshold());
            }catch(Exception e){throw new RuntimeException(e);}});
            onRender(()->{
                SPDSettings.healingConfirmationThreshold(0);
                potion[0]=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing();
                potion[0].setKnown();
                assertTrue(potion[0].collect(Dungeon.hero.belongings.backpack));
                before[0]=Dungeon.hero.cooldown();hp[0]=Dungeon.hero.HP;
                // The same default action is invoked by both classic and category quickslots.
                potion[0].execute(Dungeon.hero);
            });
            waitUntil("healing confirmation opens",()->stairsMenu()!=null,30000);
            captureRenderedButton(com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions.class,"USE HEALING ITEM","healing-confirmation.png");
            onRender(()->{try{
                com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions window=stairsMenu();
                assertNotNull(window);
                assertTrue(Dungeon.hero.belongings.backpack.contains(potion[0]));
                assertEquals(before[0],Dungeon.hero.cooldown(),0f);
                java.lang.reflect.Method select=window.getClass().getDeclaredMethod("onSelect",int.class);
                select.setAccessible(true);select.invoke(window,1);window.hide();
                assertTrue("cancel retains item",Dungeon.hero.belongings.backpack.contains(potion[0]));
                assertEquals("cancel retains turn",before[0],Dungeon.hero.cooldown(),0f);
                assertEquals("cancel retains HP",hp[0],Dungeon.hero.HP);
                potion[0].execute(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.AC_DRINK);
                window=stairsMenu();assertNotNull(window);
                select=window.getClass().getDeclaredMethod("onSelect",int.class);select.setAccessible(true);
                float turn=Dungeon.hero.cooldown();select.invoke(window,0);window.hide();
                assertFalse("confirm consumes the real potion",Dungeon.hero.belongings.backpack.contains(potion[0]));
                assertEquals("confirm spends normal drinking time",turn+1f,Dungeon.hero.cooldown(),0f);
                assertNotNull("normal healing effect retained",Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing.class));
            }catch(Exception e){throw new RuntimeException(e);}});
            waitUntil("ready after confirmed healing",()->Dungeon.hero.ready,30000);
        } finally { SPDSettings.healingConfirmationThreshold(oldThreshold); }
    }

    private static com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent metamorphOriginal,metamorphTarget;

    private static void tapNative(com.watabou.noosa.ui.Component component) throws Exception {
        AtomicReference<com.watabou.utils.Point> point=new AtomicReference<>();
        onRender(()->point.set(component.camera().cameraToScreen(component.left()+component.width()/2,component.top()+component.height()/2)));
        com.watabou.utils.Point at=point.get();
        com.watabou.input.PointerEvent.addPointerEvent(new com.watabou.input.PointerEvent(at.x,at.y,15,com.watabou.input.PointerEvent.Type.DOWN,com.watabou.input.PointerEvent.LEFT));
        Thread.sleep(150);
        com.watabou.input.PointerEvent.addPointerEvent(new com.watabou.input.PointerEvent(at.x,at.y,15,com.watabou.input.PointerEvent.Type.UP,com.watabou.input.PointerEvent.LEFT));
        Thread.sleep(300);
    }

    private static com.shatteredpixel.shatteredpixeldungeon.ui.RedButton metamorphConfirmButton() throws Exception {
        AtomicReference<com.shatteredpixel.shatteredpixeldungeon.ui.RedButton> found=new AtomicReference<>();
        onRender(()->{try{
            for(Object child:(java.util.List<?>)field(Game.scene(),"members"))if(child instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoTalent&&((com.watabou.noosa.Gizmo)child).alive)
                for(Object button:(java.util.List<?>)field(child,"members"))if(button instanceof com.shatteredpixel.shatteredpixeldungeon.ui.RedButton)found.set((com.shatteredpixel.shatteredpixeldungeon.ui.RedButton)button);
        }catch(Exception e){throw new RuntimeException(e);}});
        assertNotNull("native metamorphosis confirmation button is visible",found.get());return found.get();
    }

    private static void assertMetamorphosisChoices() throws Exception {
        final com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis[] scroll={null};
        AtomicReference<com.shatteredpixel.shatteredpixeldungeon.ui.TalentButton> choose=new AtomicReference<>(),replacement=new AtomicReference<>();
        onRender(()->{try{
            metamorphOriginal=new java.util.ArrayList<>(Dungeon.hero.talents.get(0).keySet()).get(2);
            scroll[0]=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis();scroll[0].identify();scroll[0].quantity(2);
            assertTrue(scroll[0].collect(Dungeon.hero.belongings.backpack));scroll[0].execute(Dungeon.hero,com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.AC_READ);
            Object window=com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis.WndMetamorphChoose.INSTANCE;
            assertNotNull(window);Object pane=field(window,"pane");Object first=((java.util.List<?>)field(pane,"panes")).get(0);
            choose.set((com.shatteredpixel.shatteredpixeldungeon.ui.TalentButton)((java.util.List<?>)field(first,"buttons")).get(2));
        }catch(Exception e){throw new RuntimeException(e);}});
        tapNative(choose.get());tapNative(metamorphConfirmButton());
        onRender(()->{try{
            com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis.WndMetamorphReplace window=com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis.WndMetamorphReplace.INSTANCE;
            assertNotNull(window);java.util.Map<?,?> options=(java.util.Map<?,?>)field(window,"replaceOptions");assertEquals(5,options.size());
            java.util.Set<HeroClass> classes=new java.util.HashSet<>();
            for(Object value:options.keySet()){
                com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent talent=(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent)value;
                assertNotEquals(Dungeon.hero.heroClass,window.choiceClass(talent));assertTrue(classes.add(window.choiceClass(talent)));assertTrue(window.canReplace(talent));
            }
            assertEquals("known scroll consumption remains one",1,scroll[0].quantity());
            int width=((Number)field(window,"width")).intValue();
            for(Object child:(java.util.List<?>)field(window,"members"))if(child instanceof com.shatteredpixel.shatteredpixeldungeon.ui.TalentsPane.TalentTierPane){
                java.util.List<?> buttons=(java.util.List<?>)field(child,"buttons");assertEquals(5,buttons.size());
                for(Object object:buttons){com.shatteredpixel.shatteredpixeldungeon.ui.TalentButton button=(com.shatteredpixel.shatteredpixeldungeon.ui.TalentButton)object;assertTrue(button.left()>=0);assertTrue(button.right()<=width);}
                replacement.set((com.shatteredpixel.shatteredpixeldungeon.ui.TalentButton)buttons.get(0));
            }
            metamorphTarget=(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent)options.keySet().iterator().next();assertNotNull(replacement.get());
        }catch(Exception e){throw new RuntimeException(e);}});
        Thread.sleep(300);visibilityScreenshot("metamorph-five-choices.png");
        tapNative(replacement.get());
        onRender(()->{try{
            String expected=com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(com.shatteredpixel.shatteredpixeldungeon.apd.talents.MetamorphosisChoices.class,"choice_source",
                com.shatteredpixel.shatteredpixeldungeon.messages.Messages.titleCase(com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis.WndMetamorphReplace.INSTANCE.choiceClass(metamorphTarget).title()));
            assertFalse("new choice messages are available",expected.contains("NO TEXT FOUND"));
            boolean shown=false;
            for(Object window:(java.util.List<?>)field(Game.scene(),"members"))if(window instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoTalent&&((com.watabou.noosa.Gizmo)window).alive)
                for(com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock child:TopScoresDeviceTest.find((com.watabou.noosa.Group)window,com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock.class))
                    shown|=child.text().contains(expected);
            assertTrue("native detail identifies the source class",shown);
        }catch(Exception e){throw new RuntimeException(e);}});
        captureRenderedButton(com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoTalent.class,
            com.shatteredpixel.shatteredpixeldungeon.messages.Messages.titleCase(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis.class,"metamorphose_talent")),"metamorph-choice-detail.png");
        tapNative(metamorphConfirmButton());
        waitUntil("ready after normal metamorphosis turn",()->Dungeon.hero.ready,30000);
        onRender(()->{assertEquals(metamorphTarget,Dungeon.hero.metamorphedTalents.get(metamorphOriginal));assertTrue(Dungeon.hero.talents.get(0).containsKey(metamorphTarget));assertFalse(Dungeon.hero.talents.get(0).containsKey(metamorphOriginal));});
    }

	    private static void tapQolOption(String label)throws Exception {
        final com.watabou.utils.Point[] point=new com.watabou.utils.Point[1];
        onRender(()->{try{
            java.util.List<com.shatteredpixel.shatteredpixeldungeon.windows.WndActionConfirmation> dialogs=TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndActionConfirmation.class);
            assertEquals(1,dialogs.size());
            for(com.shatteredpixel.shatteredpixeldungeon.ui.RedButton b:TopScoresDeviceTest.find(dialogs.get(0),com.shatteredpixel.shatteredpixeldungeon.ui.RedButton.class))
                if(label.equals(b.text()))point[0]=b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2);
            assertNotNull(point[0]);
        }catch(Exception e){throw new RuntimeException(e);}});
        long down=android.os.SystemClock.uptimeMillis();
        for(int type:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}){
            android.view.MotionEvent event=android.view.MotionEvent.obtain(down,android.os.SystemClock.uptimeMillis(),type,point[0].x,point[0].y,0);
            event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
            try{assertTrue(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(event,true));}finally{event.recycle();}
            if(type==android.view.MotionEvent.ACTION_DOWN)Thread.sleep(80);
        }
        Thread.sleep(400);
    }
    private static void assertTesterActionConfirmations()throws Exception {
        boolean waterWas=SPDSettings.confirmWaterskin(),trapWas=SPDSettings.confirmKnownTraps();
        final com.shatteredpixel.shatteredpixeldungeon.items.Waterskin[] skin=new com.shatteredpixel.shatteredpixeldungeon.items.Waterskin[1];
        final int[] original=new int[3];final float[] time=new float[1];
        final com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap[] oldTrap=new com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap[1];
        final int[] cell={-1},terrain={0};
        try{
            onRender(()->{try{
                SPDSettings.confirmWaterskin(true);SPDSettings.confirmKnownTraps(true);
                skin[0]=Dungeon.hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.Waterskin.class);assertNotNull(skin[0]);
                original[0]=Dungeon.hero.HP;original[1]=(Integer)field(skin[0],"volume");original[2]=Dungeon.hero.pos;
                skin[0].fill();Dungeon.hero.HP=10;time[0]=Dungeon.hero.cooldown();skin[0].execute(Dungeon.hero,"DRINK");
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(400);TopScoresDeviceTest.screenshot("waterskin-confirm.png");tapQolOption("Cancel");
            onRender(()->{try{assertEquals(20,field(skin[0],"volume"));assertEquals(10,Dungeon.hero.HP);assertEquals(time[0],Dungeon.hero.cooldown(),0);skin[0].execute(Dungeon.hero,"DRINK");}catch(Exception e){throw new RuntimeException(e);}});
            tapQolOption("Confirm");waitUntil("Waterskin action completed",()->Dungeon.hero.ready,30000);
            onRender(()->{try{
                assertTrue((Integer)field(skin[0],"volume")<20);assertTrue(Dungeon.hero.HP>10);
                SPDSettings.confirmWaterskin(false);skin[0].fill();Dungeon.hero.HP=10;skin[0].execute(Dungeon.hero,"DRINK");
                assertTrue(TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndActionConfirmation.class).isEmpty());
            }catch(Exception e){throw new RuntimeException(e);}});
            waitUntil("Immediate Waterskin action completed",()->Dungeon.hero.ready,30000);
            onRender(()->{try{
                Dungeon.hero.HP=Dungeon.hero.HT;
                for(int offset:com.watabou.utils.PathFinder.NEIGHBOURS8){int c=Dungeon.hero.pos+offset;
                    if(c>=0 && c<Dungeon.level.length() && Dungeon.level.passable[c] && Dungeon.level.heroFOV[c] && !Dungeon.level.pit[c]
                            && com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(c)==null && Dungeon.level.heaps.get(c)==null && Dungeon.level.transitions.stream().noneMatch(t->t.inside(c))){cell[0]=c;break;}}
                assertTrue("Adjacent visible test floor exists",cell[0]>=0);
                terrain[0]=Dungeon.level.map[cell[0]];oldTrap[0]=Dungeon.level.traps.get(cell[0]);
                com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap trap=new com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap();trap.visible=true;
                Dungeon.level.setTrap(trap,cell[0]);com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(cell[0],com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.TRAP);
                Dungeon.hero.handle(cell[0]);Dungeon.hero.next();
            }catch(Exception e){throw new RuntimeException(e);}});
            waitUntil("Known-trap warning",()->!TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndActionConfirmation.class).isEmpty(),30000);
            TopScoresDeviceTest.screenshot("known-trap-confirm.png");tapQolOption("Cancel");
            onRender(()->{assertEquals(original[2],Dungeon.hero.pos);assertTrue(Dungeon.level.traps.get(cell[0]).active);Dungeon.hero.handle(cell[0]);Dungeon.hero.next();});
            waitUntil("Repeated intentional trap request",()->!TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndActionConfirmation.class).isEmpty(),30000);
            tapQolOption("Confirm");waitUntil("Confirmed trap step completed",()->Dungeon.hero.ready && Dungeon.hero.pos==cell[0],30000);
            onRender(()->{assertFalse("Trap activates through normal movement",Dungeon.level.traps.get(cell[0]).active);});
        }finally{
            onRender(()->{try{
                SPDSettings.confirmWaterskin(waterWas);SPDSettings.confirmKnownTraps(trapWas);
                if(cell[0]>=0){Dungeon.level.traps.remove(cell[0]);if(oldTrap[0]!=null)Dungeon.level.traps.put(cell[0],oldTrap[0]);com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(cell[0],terrain[0]);}
                if(skin[0]!=null){java.lang.reflect.Field f=com.shatteredpixel.shatteredpixeldungeon.items.Waterskin.class.getDeclaredField("volume");f.setAccessible(true);f.setInt(skin[0],original[1]);}
                Dungeon.hero.HP=original[0];
            }catch(Exception e){throw new RuntimeException(e);}});
        }
    }

	@Test
	public void aRunCanBePlayedSavedContinuedAndEndedWithItsHistoryRecorded() throws Exception {
        final com.shatteredpixel.shatteredpixeldungeon.QuickSlot previousQuickslots=Dungeon.quickslot;
		boolean recordingWas = SPDSettings.apdRecording();
		boolean introWas = SPDSettings.intro();
		boolean introPageFoundWas = false;
		boolean introPageReadWas = false;
		final int slot = 1;
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)){
			long end = System.currentTimeMillis() + 120000;
			while ((Gdx.app == null || Gdx.files == null || Game.scene() == null) && System.currentTimeMillis() < end) Thread.sleep(250);
			assertNotNull("the game did not start", Game.scene());
			long titleBy = System.currentTimeMillis() + 60000;
			while (!(Game.scene() instanceof TitleScene) && System.currentTimeMillis() < titleBy) Thread.sleep(250);
			Thread.sleep(2500);
			introPageFoundWas = com.shatteredpixel.shatteredpixeldungeon.journal.Document.ADVENTURERS_GUIDE.isPageFound(com.shatteredpixel.shatteredpixeldungeon.journal.Document.GUIDE_INTRO);
			introPageReadWas = com.shatteredpixel.shatteredpixeldungeon.journal.Document.ADVENTURERS_GUIDE.isPageRead(com.shatteredpixel.shatteredpixeldungeon.journal.Document.GUIDE_INTRO);
			SPDSettings.apdRecording(true);
			com.shatteredpixel.shatteredpixeldungeon.journal.Document.ADVENTURERS_GUIDE.deletePage(com.shatteredpixel.shatteredpixeldungeon.journal.Document.GUIDE_INTRO);
			SPDSettings.intro(true);

			//start: exactly what the character screen's Start button does
			onRender(new Runnable(){
				public void run(){
                    Dungeon.quickslot=new com.shatteredpixel.shatteredpixeldungeon.QuickSlot(12);
					GamesInProgress.curSlot = slot;
					GamesInProgress.selectedClass = HeroClass.WARRIOR;
					Dungeon.hero = null;
					Dungeon.daily = Dungeon.dailyReplay = false;
					Dungeon.initSeed();
					ActionIndicator.clearAction();
					InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
					Game.switchScene(InterlevelScene.class);
				}
			});
			waitUntil("the first floor", new Callable<Boolean>(){
				public Boolean call(){ return Game.scene() instanceof GameScene && Dungeon.hero != null && Dungeon.hero.ready; } }, 150000);
			Log.w(TAG, "run started: depth " + Dungeon.depth + ", class " + Dungeon.hero.heroClass + ", log " + ApdRecorder.status().logPath);
			assertEquals(1, Dungeon.depth);
			assertEquals(HeroClass.WARRIOR, Dungeon.hero.heroClass);
			waitForGameplayControls();
            assertHealingConfirmationControls();
            assertMetamorphosisChoices();
            assertTesterActionConfirmations();
            assertAvailableClassesHaveNoUnlockAnnouncement();
            assertBossRewardsHaveNoPromotion();
            onRender(()->{try {
                Dungeon.quickslot.setSlot(11,new com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty().virtual());
                assertEquals(12,Dungeon.quickslot.capacity());
                assertEquals("mobile controls remain six",6,((Object[])field(field(Game.scene(),"toolbar"),"btnQuick")).length);
            }catch(Exception e){throw new RuntimeException(e);}});
			onRender(() -> { try { assertTrue("exercise controls before acknowledging intro", SPDSettings.intro()); assertGameplayControls(); assertCategoryHotbarControls(); } catch (Exception e){ throw new RuntimeException(e); } });
            captureAndTapCategoryPager();
            // One real tap searches; holding inspects without spending a turn or firing on release.
            boolean originalSearchPreference=SPDSettings.singleTapSearch();
            SPDSettings.singleTapSearch(true);
            try {
            AtomicReference<com.watabou.noosa.ui.Component> searchControl=new AtomicReference<>();
            onRender(()->{try{assertTrue(SPDSettings.intro());searchControl.set((com.watabou.noosa.ui.Component)field(field(Game.scene(),"toolbar"),"btnSearch"));}catch(Exception e){throw new RuntimeException(e);}});
            float beforeSearch=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now();
            tapNative(searchControl.get());
            waitUntil("single tap completes its normal search",()->Dungeon.hero.ready&&com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now()>=beforeSearch+2f,30000);
            assertEquals("one tap pays exactly the normal search cost",beforeSearch+2f,com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now(),0.001f);
            AtomicReference<com.watabou.utils.Point> searchPoint=new AtomicReference<>();
            onRender(()->{com.watabou.noosa.ui.Component c=searchControl.get();searchPoint.set(c.camera().cameraToScreen(c.left()+c.width()/2,c.top()+c.height()/2));});
            com.watabou.utils.Point at=searchPoint.get();float beforeInspect=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now();
            com.watabou.input.PointerEvent.addPointerEvent(new com.watabou.input.PointerEvent(at.x,at.y,15,com.watabou.input.PointerEvent.Type.DOWN,com.watabou.input.PointerEvent.LEFT));
            try{
                waitUntil("holding opens inspect selection",()->{AtomicReference<Boolean> opened=new AtomicReference<>(false);onRender(()->{try{opened.set((Boolean)field(field(Game.scene(),"toolbar"),"examining"));}catch(Exception e){throw new RuntimeException(e);}});return opened.get();},10000);
            }finally{
                com.watabou.input.PointerEvent.addPointerEvent(new com.watabou.input.PointerEvent(at.x,at.y,15,com.watabou.input.PointerEvent.Type.UP,com.watabou.input.PointerEvent.LEFT));
            }
            Thread.sleep(300);
            assertEquals("holding and releasing never searches or spends turns",beforeInspect,com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now(),0f);
            onRender(()->{try{assertTrue((Boolean)field(field(Game.scene(),"toolbar"),"examining"));assertTrue(GameScene.cancelCellSelector());assertFalse((Boolean)field(field(Game.scene(),"toolbar"),"examining"));}catch(Exception e){throw new RuntimeException(e);}});

            } finally {SPDSettings.singleTapSearch(originalSearchPreference);}

			final String runId = ApdRecorder.currentRunId();
			assertNotNull("a run id was given to the new run", runId);

			//play: wait for ten turns
			for (int i = 0; i < 10; i++){
				waitUntil("the hero to be ready", new Callable<Boolean>(){ public Boolean call(){ return Dungeon.hero.ready; } }, 30000);
				onRender(() -> { try { click(field(field(Game.scene(), "toolbar"), "btnWait"), "onClick"); } catch (Exception e){ throw new RuntimeException(e); } });
				Thread.sleep(120);
			}
			waitUntil("the hero to be ready", new Callable<Boolean>(){ public Boolean call(){ return Dungeon.hero.ready; } }, 30000);

			RecorderStatus st = ApdRecorder.status();
			assertTrue("recording is on", st.enabled && !st.sessionDisabled);
			assertEquals("no recorder errors while playing: " + st.lastError, 0, st.errorCount);
			assertTrue("events were recorded", st.eventsAccepted > 0);
			//the recorder forgets its log path once the run is sealed, so keep it from here
			final String logPath = ApdRecorder.status().logPath;
			RunFacts live = factsOfLog(logPath);
			assertEquals("WARRIOR", live.textOr(FactKeys.HERO_CLASS, "?"));
			assertTrue("the difficulty of the run was recorded", live.isKnown(FactKeys.DIFFICULTY));
			assertFalse("the run is not finished yet", live.isKnown(FactKeys.OUTCOME));

			//save, then continue from the save like the Continue button does
			final int hpBefore = Dungeon.hero.HP;
			onRender(new Runnable(){
				public void run(){
					try {
						com.shatteredpixel.shatteredpixeldungeon.windows.WndGame menu = new com.shatteredpixel.shatteredpixeldungeon.windows.WndGame();
						GameScene.show(menu);
						com.shatteredpixel.shatteredpixeldungeon.ui.RedButton main = null;
						for (Object child : (java.util.List<?>) field(menu, "members")){
							if (child instanceof com.shatteredpixel.shatteredpixeldungeon.ui.RedButton &&
									((com.shatteredpixel.shatteredpixeldungeon.ui.RedButton) child).text().equals(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(menu, "menu"))) main = (com.shatteredpixel.shatteredpixeldungeon.ui.RedButton) child;
						}
						assertNotNull("Main Menu exists", main);
						assertTrue("Main Menu is enabled during intro", main.active);
						click(main, "onClick");
					} catch (Exception e){ throw new RuntimeException(e); }
				}
			});
			waitUntil("Main Menu saves and returns to title", () -> Game.scene() instanceof TitleScene, 30000);
			onRender(() -> {
				Dungeon.hero = null;
				InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
				Game.switchScene(InterlevelScene.class);
			});
			waitUntil("the game to continue", new Callable<Boolean>(){
				public Boolean call(){ return Game.scene() instanceof GameScene && Dungeon.hero != null && Dungeon.hero.ready; } }, 150000);
			assertEquals("the same run continues", runId, ApdRecorder.currentRunId());
			assertEquals(1, Dungeon.depth);
			assertEquals(hpBefore, Dungeon.hero.HP);
			assertEquals(HeroClass.WARRIOR, Dungeon.hero.heroClass);
			assertEquals("still recording after the continue", 0, ApdRecorder.status().errorCount);
            assertEquals("extended model capacity survives real save/load",12,Dungeon.quickslot.capacity());
            assertTrue(Dungeon.quickslot.getItem(11) instanceof com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty);
            assertEquals("extended placeholder survives",0,Dungeon.quickslot.getItem(11).quantity());
            assertEquals("metamorphosis survives actual save/load",metamorphTarget,Dungeon.hero.metamorphedTalents.get(metamorphOriginal));
            assertTrue(Dungeon.hero.talents.get(0).containsKey(metamorphTarget));
			waitForGameplayControls();
			onRender(() -> { try { assertGameplayControls(); assertCategoryHotbarControls(); } catch (Exception e){ throw new RuntimeException(e); } });

			//die: the run must be sealed with its outcome
			onRender(new Runnable(){ public void run(){
                Dungeon.hero.HP=0;
                Dungeon.hero.die(Rat.class);
                assertEquals("death cleanup awaits the final ranking",runId,ApdRecorder.currentRunId());
                Dungeon.fail(Rat.class);
            } });
			Thread.sleep(1500);
			RunFacts done = factsOfLog(logPath);
			assertEquals("the run ended", LifecycleEvents.OUTCOME_DIED, done.numOr(FactKeys.OUTCOME, -1));
			assertEquals("sealed", done.textOr(FactKeys.STATUS, "?"));
			Log.w(TAG, "the run was played, saved, continued and ended; " + ApdRecorder.status().eventsAccepted + " events");
		} finally {
			try {
				GamesInProgress.delete(slot);
				FileUtils.deleteDir(GamesInProgress.gameFolder(slot));
			} catch (Throwable ignored){
			}
            Dungeon.quickslot=previousQuickslots;
			SPDSettings.apdRecording(recordingWas);
			SPDSettings.intro(introWas);
			com.shatteredpixel.shatteredpixeldungeon.journal.Document.ADVENTURERS_GUIDE.deletePage(com.shatteredpixel.shatteredpixeldungeon.journal.Document.GUIDE_INTRO);
			if (introPageFoundWas) com.shatteredpixel.shatteredpixeldungeon.journal.Document.ADVENTURERS_GUIDE.findPage(com.shatteredpixel.shatteredpixeldungeon.journal.Document.GUIDE_INTRO);
			if (introPageReadWas) com.shatteredpixel.shatteredpixeldungeon.journal.Document.ADVENTURERS_GUIDE.readPage(com.shatteredpixel.shatteredpixeldungeon.journal.Document.GUIDE_INTRO);
		}
	}

	//the other way a run ends: winning. The win must be recorded as a win and the log sealed, like a death.
	@Test
	public void aWonRunIsRecordedAsWonAndSealed() throws Exception {
		boolean recordingWas = SPDSettings.apdRecording();
		boolean introWas = SPDSettings.intro();
		final int slot = 2;
		try (ActivityScenario<AndroidLauncher> scenario = ActivityScenario.launch(AndroidLauncher.class)){
			long end = System.currentTimeMillis() + 120000;
			while ((Gdx.app == null || Gdx.files == null || Game.scene() == null) && System.currentTimeMillis() < end) Thread.sleep(250);
			assertNotNull("the game did not start", Game.scene());
			long titleBy = System.currentTimeMillis() + 60000;
			while (!(Game.scene() instanceof TitleScene) && System.currentTimeMillis() < titleBy) Thread.sleep(250);
			Thread.sleep(2500);
			SPDSettings.apdRecording(true);
			SPDSettings.intro(false);
			onRender(new Runnable(){
				public void run(){
					GamesInProgress.curSlot = slot;
					GamesInProgress.selectedClass = HeroClass.MAGE;
					Dungeon.hero = null;
					Dungeon.daily = Dungeon.dailyReplay = false;
					Dungeon.initSeed();
					ActionIndicator.clearAction();
					InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
					Game.switchScene(InterlevelScene.class);
				}
			});
			waitUntil("the first floor", new Callable<Boolean>(){
				public Boolean call(){ return Game.scene() instanceof GameScene && Dungeon.hero != null && Dungeon.hero.ready; } }, 150000);
			assertEquals(HeroClass.MAGE, Dungeon.hero.heroClass);
			final String logPath = ApdRecorder.status().logPath;
			assertNotNull(logPath);
			for (int i = 0; i < 5; i++){
				waitUntil("the hero to be ready", new Callable<Boolean>(){ public Boolean call(){ return Dungeon.hero.ready; } }, 30000);
				onRender(new Runnable(){ public void run(){ Dungeon.hero.rest(false); } });
				Thread.sleep(120);
			}
			waitUntil("the hero to be ready", new Callable<Boolean>(){ public Boolean call(){ return Dungeon.hero.ready; } }, 30000);
			onRender(new Runnable(){ public void run(){ Dungeon.win(com.shatteredpixel.shatteredpixeldungeon.items.Amulet.class); } });
			Thread.sleep(1500);
			RunFacts done = factsOfLog(logPath);
			assertEquals("the run was won", LifecycleEvents.OUTCOME_WON, done.numOr(FactKeys.OUTCOME, -1));
			assertEquals("sealed", done.textOr(FactKeys.STATUS, "?"));
			assertEquals("MAGE", done.textOr(FactKeys.HERO_CLASS, "?"));
			Log.w(TAG, "the run was won and recorded as won");
		} finally {
			try {
				GamesInProgress.delete(slot);
				FileUtils.deleteDir(GamesInProgress.gameFolder(slot));
			} catch (Throwable ignored){
			}
			SPDSettings.apdRecording(recordingWas);
			SPDSettings.intro(introWas);
		}
	}

    private static com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList clericMenu() throws Exception {
        for (Object child : (java.util.List<?>) field(Game.scene(), "members")) {
            if (child instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList)
                return (com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList) child;
        }
        throw new AssertionError("Cleric assignment menu did not open");
    }

    private static void assertClericStack() throws Exception {
        com.shatteredpixel.shatteredpixeldungeon.ui.ClericQuickCastTag[] tags =
                (com.shatteredpixel.shatteredpixeldungeon.ui.ClericQuickCastTag[]) field(Game.scene(), "clericSpells");
        GameScene.layoutTags();
        int count = SPDSettings.apdClericQuickSlots();
        com.watabou.noosa.ui.Component toolbar = (com.watabou.noosa.ui.Component) field(Game.scene(), "toolbar");
        for (int i=0; i<tags.length; i++) {
            assertEquals("chosen number of spell buttons", i<count, tags[i].visible);
            assertEquals("hidden spell buttons must reactivate when shown", i<count, tags[i].active);
            if (i>=count) continue;
            assertTrue("spell touch target", tags[i].width()>=24 && tags[i].height()>=24);
            assertTrue("spell inside horizontal screen", tags[i].left()>=0 && tags[i].right()<=GameScene.uiCamera.width);
            assertTrue("spell inside vertical screen", tags[i].top()>=0 && tags[i].bottom()<=GameScene.uiCamera.height);
            assertTrue("spell never covers hotbar", tags[i].bottom()<=toolbar.top());
            float inward=SPDSettings.flipTags()?1f:-1f;
            assertEquals("approved two inward Cleric columns",tags[0].left()+inward*tags[0].width()*(i%2),tags[i].left(),0.01f);
            assertEquals("approved two Cleric rows",tags[0].top()-tags[0].height()*(i/2),tags[i].top(),0.01f);
            for(int j=0;j<i;j++)assertFalse("Cleric touch targets never overlap",tags[i].left()<tags[j].right()&&tags[i].right()>tags[j].left()&&tags[i].top()<tags[j].bottom()&&tags[i].bottom()>tags[j].top());
        }
    }

    @Test
    public void clericQuickCastsPersistAndKeepNormalCostsAndTargeting() throws Exception {
        int oldCount=SPDSettings.apdClericQuickSlots(), oldMode=SPDSettings.apdHotbarMode();
        boolean oldIntro=SPDSettings.intro(), oldFlip=SPDSettings.flipTags();
        String[] oldSpells=new String[4];
        for(int i=0;i<4;i++) oldSpells[i]=SPDSettings.apdClericQuickSpell(i);
        final int slot=4; // Isolated emulator scratch run only.
        try (ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)) {
            waitUntil("game initialized",()->Gdx.app!=null && Gdx.files!=null && Game.scene()!=null,120000);
            // ActivityScenario can resume the scene retained by the preceding test.
            onRender(()->Game.switchScene(TitleScene.class));
            waitUntil("title",()->Game.scene() instanceof TitleScene,60000);
            SPDSettings.intro(false); SPDSettings.flipTags(false); SPDSettings.apdHotbarMode(1);
            SPDSettings.apdClericQuickSlots(4);
            onRender(()-> {
                GamesInProgress.curSlot=slot; GamesInProgress.selectedClass=HeroClass.CLERIC;
                Dungeon.hero=null; Dungeon.daily=Dungeon.dailyReplay=false; Dungeon.initSeed();
                ActionIndicator.clearAction(); InterlevelScene.mode=InterlevelScene.Mode.DESCEND;
                Game.switchScene(InterlevelScene.class);
            });
            waitUntil("Cleric first floor",()->Game.scene() instanceof GameScene && Dungeon.hero!=null && Dungeon.hero.ready,150000);
            assertEquals(HeroClass.CLERIC,Dungeon.hero.heroClass);
            // Reproduce the real setting transition: hidden tags deactivate at one slot.
            onRender(() -> { SPDSettings.apdClericQuickSlots(1); GameScene.updateTags=true; });
            Thread.sleep(700);
            onRender(() -> {
                try {
                    Object[] tags=(Object[])field(Game.scene(),"clericSpells");
                    for(int i=1;i<4;i++) assertFalse("hidden slot is inactive before enabling", ((com.watabou.noosa.ui.Component)tags[i]).active);
                    com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome tome=Dungeon.hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome.class);
                    for(int i=1;i<4;i++) com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts.assign(i,null,tome);
                    SPDSettings.apdClericQuickSlots(4); GameScene.updateTags=true;
                } catch(Exception e) { throw new RuntimeException(e); }
            });
            Thread.sleep(700);
            for(int emptySlot=1;emptySlot<4;emptySlot++) {
                final int chosenSlot=emptySlot;
                AtomicReference<com.shatteredpixel.shatteredpixeldungeon.ui.ClericQuickCastTag> tag=new AtomicReference<>();
                onRender(() -> { try {
                    Object[] tags=(Object[])field(Game.scene(),"clericSpells");
                    tag.set((com.shatteredpixel.shatteredpixeldungeon.ui.ClericQuickCastTag)tags[chosenSlot]);
                    assertTrue("newly visible slot must accept touch",tag.get().visible && tag.get().active);
                } catch(Exception e) { throw new RuntimeException(e); } });
                tapNative(tag.get());
                onRender(() -> { try {
                    com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList menu=clericMenu();
                    assertNotNull("real tap opens empty spell position "+(chosenSlot+1),menu);
                    assertNull(com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts.saved(chosenSlot,Dungeon.hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome.class)));
                } catch(Exception e) { throw new RuntimeException(e); } });
                if(emptySlot==1) visibilityScreenshot("cleric-empty-slot-picker.png");
                // Existing list selection is exercised without casting or changing other slots.
                onRender(() -> { try {
                    com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList menu=clericMenu();
                    ((com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.Picker)field(menu,"picker")).onPick(1,false);
                    menu.hide();
                    assertNotNull("selected spell is stored in the tapped position",com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts.saved(chosenSlot,Dungeon.hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome.class)));
                } catch(Exception e) { throw new RuntimeException(e); } });
            }
            visibilityScreenshot("cleric-four-slot-touch.png");
            onRender(()-> { try {
                com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome tome =
                        Dungeon.hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome.class);
                assertNotNull(tome);
                com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts.assign(0,
                        com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.INSTANCE,tome);
                com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts.assign(1,
                        com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE,tome);
                com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts.assign(2,
                        com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.INSTANCE,tome);
                com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts.assign(3,
                        com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Sunray.INSTANCE,tome);
                float charge=((Number)field(tome,"charge")).floatValue(), time=Dungeon.hero.cooldown();
                for(int n=1;n<=4;n++) { SPDSettings.apdClericQuickSlots(n); assertClericStack(); }
                assertEquals("assignment does not spend charge",charge,((Number)field(tome,"charge")).floatValue(),0);
                assertEquals("assignment does not spend a turn",time,Dungeon.hero.cooldown(),0);

                // Real long-press on the original picker opens a position chooser without casting.
                com.shatteredpixel.shatteredpixeldungeon.windows.WndClericSpells picker =
                        new com.shatteredpixel.shatteredpixeldungeon.windows.WndClericSpells(tome,Dungeon.hero,false);
                GameScene.show(picker);
                for(Object child:(java.util.List<?>)field(picker,"members")) {
                    if(child instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndClericSpells.SpellButton) {
                        click(child,"onLongClick"); break;
                    }
                }
                com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList menu=clericMenu();
                assertEquals("four assignment positions",4,((java.util.List<?>)field(menu,"entries")).size());
                ((com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.Picker)field(menu,"picker")).onPick(0,false);
                menu.hide();
                assertEquals("long press does not cast",charge,((Number)field(tome,"charge")).floatValue(),0);

                Object[] tags=(Object[])field(Game.scene(),"clericSpells");
                click(tags[1],"onLongClick");
                menu=clericMenu();
                ((com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.Picker)field(menu,"picker")).onPick(0,false);
                menu.hide();
                assertNull("saved spell can be removed",com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts.saved(1,tome));
                com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts.assign(1,
                        com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE,tome);

                click(tags[3],"onClick");
                assertEquals("locked saved spell spends no charge",charge,((Number)field(tome,"charge")).floatValue(),0);
                click(tags[1],"onClick");
                assertNotNull("actual Holy Weapon effect",Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.HolyWepBuff.class));
                assertEquals("vanilla Holy Weapon charge cost",charge-2,((Number)field(tome,"charge")).floatValue(),0);
                assertEquals("vanilla Holy Weapon turn cost",time,Dungeon.hero.cooldown(),0);
                float remaining=((Number)field(tome,"charge")).floatValue();
                click(tags[0],"onClick");
                assertEquals("targeting waits for selection",remaining,((Number)field(tome,"charge")).floatValue(),0);
                assertTrue("targeted spell can be canceled",GameScene.cancel());
                assertEquals("cancel spends no charge",remaining,((Number)field(tome,"charge")).floatValue(),0);

                tome.directCharge(2);
                float beforeSwitch=((Number)field(tome,"charge")).floatValue();
                click(tags[0],"onClick");
                click(tags[2],"onClick");
                assertNotNull("switching to Holy Ward applies its normal effect",Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.HolyArmBuff.class));
                assertEquals("switched spell pays its own normal cost",beforeSwitch-1,((Number)field(tome,"charge")).floatValue(),0);
                assertFalse("old Guiding Light selector is canceled when switching spells",GameScene.cancel());

                // Force simultaneous tags on portrait/narrow/landscape UI viewports and both sides.
                com.watabou.noosa.Camera camera=GameScene.uiCamera;
                int width=camera.width,height=camera.height;
                com.watabou.noosa.ui.Component toolbar=(com.watabou.noosa.ui.Component)field(Game.scene(),"toolbar");
                float tx=toolbar.left(),ty=toolbar.top(),tw=toolbar.width(),th=toolbar.height();
                String[] flags={"tagAttack","tagLoot","tagAction","tagResume","tagStairs"};
                boolean[] was=new boolean[flags.length];
                try {
                    for(int i=0;i<flags.length;i++) {
                        java.lang.reflect.Field flag=GameScene.class.getDeclaredField(flags[i]); flag.setAccessible(true);
                        was[i]=flag.getBoolean(Game.scene()); flag.setBoolean(Game.scene(),true);
                    }
                    for(int[] size:new int[][]{{160,240},{320,180},{240,320}}) {
                        camera.resize(size[0],size[1]); toolbar.setRect(0,size[1]-26,size[0],26);
                        for(boolean flip:new boolean[]{false,true}) {
                            SPDSettings.flipTags(flip); assertClericStack();
                            java.util.List<com.watabou.noosa.ui.Component> controls=new java.util.ArrayList<>();
                            for(Object tag:tags) controls.add((com.watabou.noosa.ui.Component)tag);
                            for(String name:new String[]{"attack","loot","action","resume","stairs"}) controls.add((com.watabou.noosa.ui.Component)field(Game.scene(),name));
                            for(int i=0;i<controls.size();i++) {
                                com.watabou.noosa.ui.Component a=controls.get(i);
                                assertTrue("tag stays on screen",a.left()>=0 && a.right()<=camera.width && a.top()>=0 && a.bottom()<=toolbar.top());
                                for(int j=i+1;j<controls.size();j++) {
                                    com.watabou.noosa.ui.Component b=controls.get(j);
                                    assertTrue("spell/attack/loot/action/resume tags never overlap",a.right()<=b.left()+0.01f || b.right()<=a.left()+0.01f
                                            || a.bottom()<=b.top()+0.01f || b.bottom()<=a.top()+0.01f);
                                }
                            }
                        }
                    }
                } finally {
                    camera.resize(width,height); toolbar.setRect(tx,ty,tw,th); SPDSettings.flipTags(false);
                    for(int i=0;i<flags.length;i++) {
                        java.lang.reflect.Field flag=GameScene.class.getDeclaredField(flags[i]); flag.setAccessible(true);
                        flag.setBoolean(Game.scene(),was[i]);
                    }
                    GameScene.updateTags=true; GameScene.layoutTags();
                }
                Dungeon.saveAll(); Game.switchScene(TitleScene.class);
            } catch(Exception e) { throw new RuntimeException(e); } });
            waitUntil("saved title",()->Game.scene() instanceof TitleScene,60000);
            onRender(()-> { Dungeon.hero=null; InterlevelScene.mode=InterlevelScene.Mode.CONTINUE; Game.switchScene(InterlevelScene.class); });
            waitUntil("saved Cleric resumed",()->Game.scene() instanceof GameScene && Dungeon.hero!=null && Dungeon.hero.ready,150000);
            onRender(()-> { try {
                assertEquals(4,SPDSettings.apdClericQuickSlots());
                assertClericStack();
                com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome tome =
                        Dungeon.hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome.class);
                org.junit.Assert.assertSame(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon.INSTANCE,
                        com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts.saved(1,tome));
                org.junit.Assert.assertSame(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Sunray.INSTANCE,
                        com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts.saved(3,tome));
                assertNull("locked preference preserved without granting spell",
                        com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts.available(Dungeon.hero,tome,3));
            } catch(Exception e) { throw new RuntimeException(e); } });
        } finally {
            GamesInProgress.delete(slot); FileUtils.deleteDir(GamesInProgress.gameFolder(slot));
            SPDSettings.intro(oldIntro); SPDSettings.flipTags(oldFlip); SPDSettings.apdHotbarMode(oldMode);
            SPDSettings.apdClericQuickSlots(oldCount);
            for(int i=0;i<4;i++) SPDSettings.apdClericQuickSpell(i,oldSpells[i]);
        }
    }

    static void visibilityScreenshot(String name) throws Exception {
        android.graphics.Bitmap bitmap=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        try{saveScreenshotBitmap(name,bitmap);}finally{bitmap.recycle();}
    }
    private static void saveScreenshotBitmap(String name,android.graphics.Bitmap bitmap) throws Exception {
        android.content.Context context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,name);
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"image/png");
        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Pictures/APD-Test-Visuals");
        android.net.Uri uri=context.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        assertNotNull(uri);
        try(java.io.OutputStream out=context.getContentResolver().openOutputStream(uri)){
            assertNotNull(out);assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));
        }
    }
    private static com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions stairsMenu() throws Exception {
        for(Object member:(java.util.List<?>)field(Game.scene(),"members"))
            if(member instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions)
                return (com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions)member;
        return null;
    }
    @Test public void tileVisibilityAndStairNavigationKeepNormalTurnsFogAndControls() throws Exception {
        boolean oldEnhanced=SPDSettings.enhancedTileVisibility(),oldShortcut=SPDSettings.stairNavigationShortcut();
        boolean oldRecord=SPDSettings.apdRecording(),oldIntro=SPDSettings.intro();int oldGrid=SPDSettings.visualGrid();
        int oldSpeed=SPDSettings.playbackSpeed();
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
            waitUntil("game ready",()->Gdx.app!=null && Game.scene()!=null,120000);
            onRender(()->{
                SPDSettings.apdRecording(false);SPDSettings.intro(false);SPDSettings.visualGrid(0);
                SPDSettings.enhancedTileVisibility(false);
                // Exercise a fresh preference: do not force-enable the shortcut as TEST79 did.
                com.badlogic.gdx.Preferences prefs=Gdx.app.getPreferences("ShatteredPixelDungeon");
                prefs.remove("apd_stair_navigation_shortcut");prefs.flush();
                assertTrue("stair shortcut is discoverable by default", SPDSettings.stairNavigationShortcut());
                GamesInProgress.curSlot=4;GamesInProgress.selectedClass=HeroClass.WARRIOR;
                Dungeon.hero=null;Dungeon.daily=Dungeon.dailyReplay=false;Dungeon.initSeed();
                ActionIndicator.clearAction();InterlevelScene.mode=InterlevelScene.Mode.DESCEND;Game.switchScene(InterlevelScene.class);
            });
            waitUntil("first floor ready",()->Game.scene() instanceof GameScene && Dungeon.hero!=null && Dungeon.hero.ready,150000);
            waitForGameplayControls();Thread.sleep(600);
            final long[] observed=new long[3];
            for(int mode=0;mode<3;mode++){
                final int selected=mode;
                final java.util.concurrent.atomic.AtomicLong started=new java.util.concurrent.atomic.AtomicLong(),finished=new java.util.concurrent.atomic.AtomicLong();
                final float turnBefore=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now();
                onRender(()->{try{
                    SPDSettings.playbackSpeed(selected);
                    com.watabou.noosa.Group world=(com.watabou.noosa.Group)field(Game.scene(),"mobs");
                    assertTrue("dungeon sprites use playback groups",world instanceof com.shatteredpixel.shatteredpixeldungeon.apd.PlaybackGroup);
                    started.set(System.nanoTime());
                    world.add(new com.watabou.noosa.tweeners.Tweener(new com.watabou.noosa.Gizmo(),0.5f){
                        @Override protected void updateValues(float progress){}
                        @Override protected void onComplete(){finished.set(System.nanoTime());killAndErase();}
                    });
                }catch(Exception e){throw new RuntimeException(e);}});
                waitUntil("native animation completes",()->finished.get()!=0,10000);
                observed[mode]=finished.get()-started.get();
                assertEquals("visual playback consumes no turn",turnBefore,com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now(),0f);
            }
            assertTrue("half-speed animation is slower than normal",observed[0]>observed[1]);
            assertTrue("double-speed animation is faster than normal",observed[1]>observed[2]);
            Log.i(TAG,"Playback animation nanoseconds slow/normal/fast: "+java.util.Arrays.toString(observed));
            onRender(()->SPDSettings.playbackSpeed(1));
            final int[] terrain=Dungeon.level.map.clone();final boolean[] visited=Dungeon.level.visited.clone();
            final boolean[] fov=Dungeon.hero.fieldOfView.clone();final int depth=Dungeon.depth;
            for(int zoom:new int[]{2,4}){
                onRender(()->{com.watabou.noosa.Camera.main.zoom(zoom);SPDSettings.enhancedTileVisibility(false);});Thread.sleep(350);
                visibilityScreenshot("tiles-original-"+zoom+".png");
                onRender(()->SPDSettings.enhancedTileVisibility(true));Thread.sleep(350);
                visibilityScreenshot("tiles-enhanced-"+zoom+".png");
                assertTrue(SPDSettings.enhancedTileVisibility());assertEquals(0,SPDSettings.visualGrid());
                org.junit.Assert.assertArrayEquals("terrain unchanged",terrain,Dungeon.level.map);
                org.junit.Assert.assertArrayEquals("exploration unchanged",visited,Dungeon.level.visited);
                org.junit.Assert.assertArrayEquals("fog unchanged",fov,Dungeon.hero.fieldOfView);
            }
            final java.util.concurrent.atomic.AtomicReference<com.watabou.utils.Point> stairTouch=new java.util.concurrent.atomic.AtomicReference<>();
            onRender(()->{try{
                com.watabou.noosa.ui.Component button=(com.watabou.noosa.ui.Component)field(Game.scene(),"stairs");
                com.watabou.noosa.ui.Component toolbar=(com.watabou.noosa.ui.Component)field(Game.scene(),"toolbar");
                assertTrue(button.visible);assertTrue(button.bottom()<=toolbar.top()+0.01f);
                assertTrue(button.left()>=0 && button.right()<=button.camera().width);
                stairTouch.set(button.camera().cameraToScreen(button.left()+button.width()/2f,button.top()+button.height()/2f));
            }catch(Exception e){throw new RuntimeException(e);}});
            com.watabou.utils.Point at=stairTouch.get();
            com.watabou.input.PointerEvent.addPointerEvent(new com.watabou.input.PointerEvent(at.x,at.y,9,com.watabou.input.PointerEvent.Type.DOWN,com.watabou.input.PointerEvent.LEFT));
            Thread.sleep(150);
            com.watabou.input.PointerEvent.addPointerEvent(new com.watabou.input.PointerEvent(at.x,at.y,9,com.watabou.input.PointerEvent.Type.UP,com.watabou.input.PointerEvent.LEFT));
            waitUntil("stair menu responds to a real touch",()->stairsMenu()!=null,30000);
            visibilityScreenshot("stairs-shortcut.png");
            onRender(()->{try{stairsMenu().hide();}catch(Exception e){throw new RuntimeException(e);}});
            final java.util.concurrent.atomic.AtomicInteger destination=new java.util.concurrent.atomic.AtomicInteger(-1);
            onRender(()->{
                for(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob:new java.util.ArrayList<>(Dungeon.level.mobs))
                    com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(mob);
                Dungeon.level.mobs.clear();Dungeon.hero.checkVisibleMobs();
                for(int offset:com.watabou.utils.PathFinder.NEIGHBOURS8){int cell=Dungeon.hero.pos+offset;
                    if(cell>=0 && cell<Dungeon.level.length() && Dungeon.hero.fieldOfView[cell] && com.shatteredpixel.shatteredpixeldungeon.apd.navigation.StairNavigation.safeStep(Dungeon.level,Dungeon.hero,cell)){destination.set(cell);break;}}
                assertTrue("a discovered safe adjacent tile exists",destination.get()>=0);
                Dungeon.level.transitions.add(0,new com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition(
                        Dungeon.level,destination.get(),com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition.Type.REGULAR_EXIT));
                assertNull(com.shatteredpixel.shatteredpixeldungeon.apd.navigation.StairNavigation.begin(Dungeon.hero,false));
                float turn=Dungeon.hero.cooldown();int origin=Dungeon.hero.pos;
                Dungeon.level.passable[destination.get()]=false;
                try{
                    java.lang.reflect.Method move=com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero.class.getDeclaredMethod("actMove",com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction.Move.class);
                    move.setAccessible(true);assertFalse((Boolean)move.invoke(Dungeon.hero,Dungeon.hero.curAction));
                }catch(Exception e){throw new RuntimeException(e);}finally{Dungeon.level.passable[destination.get()]=true;}
                assertNull("newly blocked route stops",Dungeon.hero.curAction);assertEquals(origin,Dungeon.hero.pos);assertEquals(turn,Dungeon.hero.cooldown(),0);
                assertNull(com.shatteredpixel.shatteredpixeldungeon.apd.navigation.StairNavigation.begin(Dungeon.hero,false));
                Dungeon.hero.interrupt();assertNull(Dungeon.hero.curAction);assertNull(Dungeon.hero.lastAction);
                assertNull(com.shatteredpixel.shatteredpixeldungeon.apd.navigation.StairNavigation.begin(Dungeon.hero,false));
                Dungeon.hero.damageInterrupt=false;Dungeon.hero.damage(1,new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger());
                assertNull("damage cancels the route",Dungeon.hero.curAction);
                assertNull(com.shatteredpixel.shatteredpixeldungeon.apd.navigation.StairNavigation.begin(Dungeon.hero,false));
                Rat threat=new Rat();threat.pos=destination.get();GameScene.add(threat);Dungeon.hero.checkVisibleMobs();
                assertNull("new visible enemy cancels the route",Dungeon.hero.curAction);
                com.shatteredpixel.shatteredpixeldungeon.actors.Actor.remove(threat);Dungeon.level.mobs.remove(threat);
                if(threat.sprite!=null)threat.sprite.killAndErase();Dungeon.hero.checkVisibleMobs();
                try{
                    click(field(Game.scene(),"stairs"),"onClick");
                    com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions menu=stairsMenu();assertNotNull(menu);
                    java.lang.reflect.Method choose=menu.getClass().getDeclaredMethod("onSelect",int.class);choose.setAccessible(true);choose.invoke(menu,1);menu.hide();
                    boolean marked=false;
                    for(Object effect:(java.util.List<?>)field(field(Game.scene(),"overFogEffects"),"members"))
                        if(effect instanceof com.shatteredpixel.shatteredpixeldungeon.effects.StairHighlight
                                && ((com.shatteredpixel.shatteredpixeldungeon.effects.StairHighlight)effect).cell==destination.get())marked=true;
                    assertTrue("the selected known stair is highlighted exactly",marked);
                }catch(Exception e){throw new RuntimeException(e);}
            });
            Thread.sleep(300); // Capture a completed rendered frame while the short marker is still visible.
            visibilityScreenshot("stairs-highlight.png");
            waitUntil("walk arrives and stops",()->Dungeon.hero.ready && Dungeon.hero.pos==destination.get(),30000);
            assertNull(Dungeon.hero.curAction);assertEquals("no automatic floor transition",depth,Dungeon.depth);
            onRender(()->{try{
                assertNull(com.shatteredpixel.shatteredpixeldungeon.apd.navigation.StairNavigation.begin(Dungeon.hero,true));
                assertTrue("save during pending navigation",Dungeon.hero.curAction instanceof com.shatteredpixel.shatteredpixeldungeon.apd.navigation.StairNavigation.Walk);
                Dungeon.saveAll();Game.switchScene(TitleScene.class);
            }catch(Exception e){throw new RuntimeException(e);}});
            waitUntil("title after save",()->Game.scene() instanceof TitleScene,30000);
            onRender(()->{Dungeon.hero=null;InterlevelScene.mode=InterlevelScene.Mode.CONTINUE;Game.switchScene(InterlevelScene.class);});
            waitUntil("saved navigation position",()->Game.scene() instanceof GameScene && Dungeon.hero!=null && Dungeon.hero.ready,150000);
            assertEquals(destination.get(),Dungeon.hero.pos);assertEquals(depth,Dungeon.depth);assertNull(Dungeon.hero.curAction);
            assertTrue(SPDSettings.enhancedTileVisibility());assertTrue(SPDSettings.stairNavigationShortcut());
            final com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem[] chaliceWindow=new com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem[1];
            final com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact[] oldArtifact=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact[1];
            final int[] oldHp=new int[1];
            onRender(()->{
                oldArtifact[0]=Dungeon.hero.belongings.artifact;oldHp[0]=Dungeon.hero.HP;
                com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood chalice=new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood();
                for(int i=0;i<5;i++)chalice.upgrade();
                Dungeon.hero.belongings.artifact=chalice;Dungeon.hero.HP=Dungeon.hero.HT;
                String description=chalice.desc();
                assertTrue(description.contains("Base damage:"));assertTrue(description.contains("HP loss after shields:"));
                assertTrue(description.contains("WARNING: This prick could kill you!"));
                chaliceWindow[0]=new com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem(chalice);
                GameScene.show(chaliceWindow[0]);
            });
            Thread.sleep(350);visibilityScreenshot("chalice-preview.png");
            onRender(()->{chaliceWindow[0].hide();Dungeon.hero.belongings.artifact=oldArtifact[0];Dungeon.hero.HP=oldHp[0];});
            ArrayList<Item> previousOffers=Blacksmith.Quest.smithRewards;
            Weapon.Enchantment previousEnchant=Blacksmith.Quest.smithEnchant;Armor.Glyph previousGlyph=Blacksmith.Quest.smithGlyph;
            final WndBlacksmith.WndSmith[] smithWindow=new WndBlacksmith.WndSmith[1];
            final Item[] chosenReward=new Item[1];
            final java.util.concurrent.atomic.AtomicReference<com.watabou.utils.Point> rewardTouch=new java.util.concurrent.atomic.AtomicReference<>();
            try {
                onRender(()->{try{
                    ArrayList<Item> offers=new ArrayList<>();
                    offers.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword());
                    offers.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BattleAxe());
                    offers.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpear());
                    offers.add(new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor());
                    for(int i=0;i<offers.size();i++){offers.get(i).level(i);offers.get(i).levelKnown=false;offers.get(i).cursedKnown=false;}
                    Blacksmith.Quest.smithRewards=offers;Blacksmith.Quest.smithEnchant=null;Blacksmith.Quest.smithGlyph=null;
                    smithWindow[0]=new WndBlacksmith.WndSmith(new Blacksmith(),Dungeon.hero);GameScene.show(smithWindow[0]);
                    int index=0;
                    for(Object member:(java.util.List<?>)field(smithWindow[0],"members")) if(member instanceof ItemButton){
                        ItemButton button=(ItemButton)member;Item reward=offers.get(index);
                        assertTrue("UI uses the exact reward object",button.item()==reward);
                        assertTrue(reward.levelKnown);assertFalse(reward.cursedKnown);
                        assertEquals("native slot shows actual +0 through +3",String.format(java.util.Locale.ROOT,"%+d",index),
                                ((com.watabou.noosa.BitmapText)field(button.slot(),"level")).text());
                        if(index==2){chosenReward[0]=reward;rewardTouch.set(button.camera().cameraToScreen(button.left()+button.width()/2f,button.top()+button.height()/2f));}
                        index++;
                    }
                    assertEquals(4,index);
                }catch(Exception e){throw new RuntimeException(e);}});
                Thread.sleep(350);visibilityScreenshot("blacksmith-rewards.png");
                com.watabou.utils.Point rewardAt=rewardTouch.get();
                com.watabou.input.PointerEvent.addPointerEvent(new com.watabou.input.PointerEvent(rewardAt.x,rewardAt.y,19,com.watabou.input.PointerEvent.Type.DOWN,com.watabou.input.PointerEvent.LEFT));
                Thread.sleep(150);
                com.watabou.input.PointerEvent.addPointerEvent(new com.watabou.input.PointerEvent(rewardAt.x,rewardAt.y,19,com.watabou.input.PointerEvent.Type.UP,com.watabou.input.PointerEvent.LEFT));
                waitUntil("blacksmith reward responds to actual touch",()->{
                    for(Object member:(java.util.List<?>)field(Game.scene(),"members"))if(member instanceof WndInfoItem)return true;
                    return false;
                },30000);
                Thread.sleep(350);visibilityScreenshot("blacksmith-reward-detail.png");
                AtomicReference<com.shatteredpixel.shatteredpixeldungeon.ui.RedButton> rewardConfirm=new AtomicReference<>();
                onRender(()->{try{
                    WndInfoItem info=null;
                    for(Object member:(java.util.List<?>)field(Game.scene(),"members"))if(member instanceof WndInfoItem)info=(WndInfoItem)member;
                    assertNotNull(info);
                    String label=com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndSadGhost.class,"confirm");
                    for(Object member:(java.util.List<?>)field(info,"members"))if(member instanceof com.shatteredpixel.shatteredpixeldungeon.ui.RedButton&&((com.shatteredpixel.shatteredpixeldungeon.ui.RedButton)member).text().equals(label))rewardConfirm.set((com.shatteredpixel.shatteredpixeldungeon.ui.RedButton)member);
                    assertNotNull("Select the reward confirmation rather than a calculation toggle",rewardConfirm.get());
                }catch(Exception e){throw new RuntimeException(e);}});
                tapNative(rewardConfirm.get());
                onRender(()->{try{
                    assertTrue("selected exact item is received",Dungeon.hero.belongings.contains(chosenReward[0]));
                    assertEquals(2,chosenReward[0].level());assertNull(Blacksmith.Quest.smithRewards);
                }catch(Exception e){throw new RuntimeException(e);}});
            }finally{
                onRender(()->{if(smithWindow[0]!=null)smithWindow[0].hide();Blacksmith.Quest.smithRewards=previousOffers;Blacksmith.Quest.smithEnchant=previousEnchant;Blacksmith.Quest.smithGlyph=previousGlyph;});
            }
            onRender(()->Game.switchScene(TitleScene.class));
        }finally{
            SPDSettings.enhancedTileVisibility(oldEnhanced);SPDSettings.stairNavigationShortcut(oldShortcut);
            SPDSettings.visualGrid(oldGrid);SPDSettings.apdRecording(oldRecord);SPDSettings.intro(oldIntro);
            SPDSettings.playbackSpeed(oldSpeed);
        }
    }

}

