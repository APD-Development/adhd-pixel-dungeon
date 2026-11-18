package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Scene;
import com.watabou.utils.PlatformSupport;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * First emulator-free execution seam for the real game. This deliberately proves only level
 * generation and one complete actor-clock wait. It is not yet the normal-play Warrior bot.
 */
public class HeadlessActorLoopProbeTest {

	private static final class HeadlessGame extends Game {
		HeadlessGame(Class<? extends Scene> sceneClass, PlatformSupport platform) {
			super(sceneClass, platform);
			requestedReset = false;
		}

		static Class<? extends Scene> sceneClass() {
			return sceneClass;
		}
	}

	private static final class HeadlessTurnDriver implements WarriorTurnDriver {
		@Override public void execute(WarriorTurnCommand command) {
			if (command.type() != WarriorTurnCommand.Type.WAIT)
				throw new IllegalArgumentException("unsupported turn command: " + command.type());
			Dungeon.hero.rest(false);
		}
	}

	private static final class InvisibleSprite extends CharSprite {
		void attach(Char character) {
			ch = character;
			character.sprite = this;
			visible = false;
		}

		@Override public void idle() {}
		@Override public void showAlert() {}
		@Override public void hideAlert() {}
		@Override public void hideLost() {}
		@Override public void hideInvestigate() {}
		@Override public void showStatus(int color, String text, Object... args) {}
		@Override public void showStatusWithIcon(int color, String text, int icon, Object... args) {}
	}

	private static Level createFirstFloor(long seed, boolean enter) {
		HeadlessTestSupport.start();
		SPDSettings.challenges(0);
		SPDSettings.apdDifficulty(3);
		SPDSettings.intro(false);
		Dungeon.daily = false;
		Dungeon.dailyReplay = false;
		Dungeon.customSeedText = "";
		Dungeon.seed = seed;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		GamesInProgress.randomizedClass = false;
		GamesInProgress.curSlot = 1;
		Dungeon.init();
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Level level = Dungeon.newLevel();
		if (enter) Dungeon.switchLevel(level, -1);
		return level;
	}

	private static void attachInvisibleSprites() {
		new InvisibleSprite().attach(Dungeon.hero);
		for (Mob mob : Dungeon.level.mobs) new InvisibleSprite().attach(mob);
	}

	private static void await(String description, Thread actorThread,
			AtomicReference<Throwable> failure, java.util.concurrent.Callable<Boolean> condition)
			throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (!condition.call()) {
			if (failure.get() != null) throw new AssertionError(description, failure.get());
			if (!actorThread.isAlive()) fail(description + ": actor thread stopped");
			if (System.nanoTime() >= deadline) fail(description + ": timeout");
			Thread.sleep(1);
		}
	}

	private static Actor currentActor() throws Exception {
		java.lang.reflect.Field current = Actor.class.getDeclaredField("current");
		current.setAccessible(true);
		return (Actor) current.get(null);
	}

	@Test
	public void oneHundredRealFirstFloorsNeedNoAndroidRuntime() {
		HeadlessTestSupport.start();
		int oldChallenges = SPDSettings.challenges();
		int oldDifficulty = SPDSettings.apdDifficulty();
		boolean oldIntro = SPDSettings.intro();
		try {
			long started = System.nanoTime();
			int mobs = 0;
			for (int i = 0; i < 100; i++) {
				Level level = createFirstFloor(1_100_000L + i, false);
				assertNotNull(Dungeon.hero);
				assertTrue(level.length() > 0);
				mobs += level.mobs.size();
			}
			long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
			assertTrue("real generated floors must contain enemies", mobs > 0);
			System.out.println("APD_HEADLESS_GENERATION floors=100 mobs=" + mobs
					+ " elapsedMillis=" + elapsedMillis);
		} finally {
			SPDSettings.challenges(oldChallenges);
			SPDSettings.apdDifficulty(oldDifficulty);
			SPDSettings.intro(oldIntro);
		}
	}

	@Test
	public void realActorClockReachesTheNextHeroTurnWithoutRendering() throws Exception {
		HeadlessTestSupport.start();
		int oldChallenges = SPDSettings.challenges();
		int oldDifficulty = SPDSettings.apdDifficulty();
		boolean oldIntro = SPDSettings.intro();

		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread actorThread = null;
		Game oldGame = Game.instance;
		PlatformSupport oldPlatform = Game.platform;
		Class<? extends Scene> oldSceneClass = HeadlessGame.sceneClass();
		try {
			//Actor.process asks the noosa shell whether a scene switch is pending. The existing
			//libGDX headless fixture supplies services but intentionally has no noosa Game.
			new HeadlessGame(oldSceneClass, oldPlatform);
			createFirstFloor(1_000_000L, true);
			attachInvisibleSprites();
			//A rendered GameScene normally marks the newly entered hero input-ready after its
			//first UI refresh. Headless mode starts at that already-established input boundary.
			Dungeon.hero.ready = true;
			float before = Actor.now();
			int beforeHp = Dungeon.hero.HP;
			int beforePosition = Dungeon.hero.pos;

			WarriorTurnCommand wait = WarriorTurnCommand.waitAt(
					WarriorTurnCommand.Reason.RECOVERY, beforePosition);
			new HeadlessTurnDriver().execute(wait);
			//Pause immediately before the next hero act. This proves the scheduler reached the
			//next input boundary without invoking Hero.ready's render-only UI callbacks.
			Dungeon.hero.sprite.isMoving = true;
			Actor.keepActorThreadAlive = true;
			actorThread = new Thread(Actor::process, "apd-headless-actor-probe");
			actorThread.setDaemon(true);
			actorThread.setUncaughtExceptionHandler((thread, thrown) -> failure.set(thrown));
			actorThread.start();
			final Thread runningActorThread = actorThread;
			await("next hero turn", runningActorThread, failure,
					() -> Actor.now() >= before + 1f && currentActor() == Dungeon.hero);

			assertEquals(beforePosition, Dungeon.hero.pos);
			assertTrue(Dungeon.hero.HP > 0 && Dungeon.hero.HP <= beforeHp);
			assertTrue("the real actor clock must advance", Actor.now() >= before + 1f);
		} finally {
			Actor.keepActorThreadAlive = false;
			if (actorThread != null) {
				for (int i = 0; i < 50 && actorThread.isAlive(); i++) {
					actorThread.interrupt();
					synchronized (actorThread) { actorThread.notifyAll(); }
					if (Dungeon.hero != null && Dungeon.hero.sprite != null) {
						synchronized (Dungeon.hero.sprite) { Dungeon.hero.sprite.notifyAll(); }
					}
					actorThread.join(100);
				}
			}
			Actor.keepActorThreadAlive = true;
			Game.instance = oldGame;
			Game.platform = oldPlatform;
			SPDSettings.challenges(oldChallenges);
			SPDSettings.apdDifficulty(oldDifficulty);
			SPDSettings.intro(oldIntro);
		}
		if (failure.get() != null) throw new AssertionError("headless actor loop failed", failure.get());
		assertTrue("actor thread must stop cleanly", actorThread == null || !actorThread.isAlive());
	}
}
