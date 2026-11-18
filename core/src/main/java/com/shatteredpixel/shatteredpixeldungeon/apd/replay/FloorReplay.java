package com.shatteredpixel.shatteredpixeldungeon.apd.replay;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ReplayFloor;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.Difficulty;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ReplayScan;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

//Generates the floors of a seed in order, outside any game, under a fixed reference context, and returns them as plain data.
//
//WHAT THE RESULT MEANS: canonical seed floors. Floor N is what the seed gives after floors 1..N-1 were generated in order with the
//reference hero (the run's class with its starting kit, no talent points), reference journal and tutorial (ReplayContext), the run's challenges and exotic chance, no bones, no
//play-time loot, and no branch levels. It is the exact counterfactual of nothing: a floor the run really met is described by the run's
//own recorded telemetry, never by this.
//
//ISOLATION: it refuses to run while a game could be live (see refusal()), runs one at a time, switches every recording hook off while it runs, never touches the
//journal or settings, and puts back what it found: the Dungeon fields, the three item identity tables, the limited-drop counters,
//the item generator, the statistics, the quest state and the notes. What it leaves is state that every new or continued game
//replaces before reading it (ReplayIsolationTest pins exactly which).
public final class FloorReplay {

	private FloorReplay(){}

	//bump when the engine, reference context or scan changes what a replay returns: cached results become stale
	public static final int ENGINE_VERSION = 2; // Approved Gathering scroll changes current reference loot, never raw recorded history.
	public static final String CONTEXT_ID = "ref1-classkit";
	public static final int LAST_FLOOR = 26;

	public static final class Refused extends Exception {
		public Refused( String why ){
			super(why);
		}
	}

	//the app plugs in a check for "a game scene is on screen"
	public interface LiveGameCheck {
		//null when no live game, else why
		String liveGame();
	}

	private static volatile LiveGameCheck liveGameCheck;
	private static final Object LOCK = new Object();
	private static volatile boolean running;

	public static void setLiveGameCheck( LiveGameCheck c ){
		liveGameCheck = c;
	}

	//null when a replay may run now, else the reason it may not
	public static String refusal(){
		if (running) return "another replay is running";
		//a game scene on screen (the level is being played or built): never borrow the game's state then
		try {
			com.watabou.noosa.Scene scene = com.watabou.noosa.Game.scene();
			if (scene instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene
					|| scene instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene){
				return "a game is on screen";
			}
		} catch (RuntimeException e){
			//no running game engine (a plain test process): nothing is on screen
		}
		LiveGameCheck c = liveGameCheck;
		if (c != null){
			String why = c.liveGame();
			if (why != null) return why;
		}
		return null;
	}

	public static boolean isRunning(){
		return running;
	}


    // A game must not replace the shared state until replay has restored it.
    // Monitor acquisition does not clear or bypass an existing interrupt.
    public static void awaitIdle(){
        synchronized (LOCK){
            // replay() holds this monitor through generation and its restoration finally block.
        }
    }

	//Verification hook: sees each freshly built level while the replay still holds it (tests fingerprint it). Must only read.
	public interface Observer {
		void level( int depth, Level level );
	}

	//Floors 1..maxDepth (at most LAST_FLOOR) of a seed.
	public static List<ReplayFloor> replay( long seed, int challenges, HeroClass heroClass, int maxDepth ) throws Refused {
		return replay(seed, challenges, 0, heroClass, maxDepth, null);
	}

	public static List<ReplayFloor> replay( long seed, int challenges, HeroClass heroClass, int maxDepth, Observer observer ) throws Refused {
		return replay(seed, challenges, 0, heroClass, maxDepth, observer);
	}

	//exoticChance: the run's own "exotic potions/scrolls" percent (0 = vanilla)
	public static List<ReplayFloor> replay( long seed, int challenges, int exoticChance, HeroClass heroClass, int maxDepth, Observer observer ) throws Refused {
		return replay(seed, challenges, exoticChance, Difficulty.DEFAULT, heroClass, maxDepth, observer);
	}

	//difficulty: the run's own difficulty (1-5); only what the difficulty system changes is affected (nothing at 3, and nothing yet at the others)
	public static List<ReplayFloor> replay( long seed, int challenges, int exoticChance, int difficulty, HeroClass heroClass, int maxDepth, Observer observer ) throws Refused {
		int level = Difficulty.normalize(difficulty);
		return replay(seed, challenges, exoticChance, level, Difficulty.balanceRevision(level), heroClass, maxDepth, observer);
	}

	//balance: the balance revision the run was played with (0 = no modifiers). A revision this build does not know is refused, not guessed.
	public static List<ReplayFloor> replay( long seed, int challenges, int exoticChance, int difficulty, int balance, HeroClass heroClass, int maxDepth, Observer observer ) throws Refused {
		if (!Difficulty.supportsRevision(Difficulty.normalize(difficulty), balance)) throw new Refused("the run was played with a difficulty balance this version does not know");
		String why = refusal();
		if (why != null) throw new Refused(why);
		synchronized (LOCK){
			why = refusal();
			if (why != null) throw new Refused(why);
			running = true;
			Saved saved = null;
			boolean[] hooks = ApdRecorder.suspendHooks();
			try {
				saved = new Saved();
				ReplayContext.exoticChance(Math.max(0, Math.min(80, exoticChance)));
				ReplayContext.difficulty(Difficulty.normalize(difficulty));
				ReplayContext.balance(balance);
				ReplayContext.enter();
				return generate(seed, challenges, heroClass, Math.max(1, Math.min(LAST_FLOOR, maxDepth)), observer);
			} finally {
				ReplayContext.exit();
				try {
					if (saved != null) saved.restore();
				} finally {
					ApdRecorder.resumeHooks(hooks);
					running = false;
				}
			}
		}
	}

	//Generates with whatever state exists, for tests that manage isolation themselves.
	private static List<ReplayFloor> generate( long seed, int challenges, HeroClass heroClass, int maxDepth, Observer observer ){
		Dungeon.seed = seed;
		Dungeon.challenges = challenges;
		Dungeon.daily = false;
		Dungeon.dailyReplay = false;
		Dungeon.customSeedText = "";
		Dungeon.mobsToChampion = 1;

		Actor.clear();
		Actor.resetNextID();

		//the same start as Dungeon.init, minus everything that belongs to a real run (hero kit, badges, quick slots, recording)
		Random.pushGenerator(seed + 1);
		try {
			Scroll.initLabels();
			Potion.initColors();
			Ring.initGems();
			SpecialRoom.initForRun();
			SecretRoom.initForRun();
			Generator.fullReset();
		} finally {
			Random.popGenerator();
		}

		Statistics.reset();
		Notes.reset();
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.generatedLevels = new ArrayList<>();
		Dungeon.gold = 0;
		Dungeon.energy = 0;
		Dungeon.droppedItems = new com.watabou.utils.SparseArray<>();
		Dungeon.LimitedDrops.reset();
		Dungeon.chapters = new HashSet<>();
		Ghost.Quest.reset();
		Wandmaker.Quest.reset();
		Blacksmith.Quest.reset();
		Imp.Quest.reset();

		//The reference hero is the run's class with its starting kit (the kit decides which shop bags are offered). The kit is built while
		//the hero counts as not alive: the game only records journal discoveries and badges for a living hero, and a replay must record none.
		Hero hero = new Hero();
		hero.HP = 0;
		Dungeon.hero = hero;
		heroClass.initHero(hero);
		hero.HP = hero.HT;
		hero.live();

		List<ReplayFloor> out = new ArrayList<>();
		for (int depth = 1; depth <= maxDepth; depth++){
			Dungeon.depth = depth;
			Dungeon.branch = 0;
			Level level = Dungeon.newLevel();
			Dungeon.level = level;
			ReplayFloor f = ReplayScan.scan(level);
			f.depth = depth;
			out.add(f);
			if (observer != null) observer.level(depth, level);
		}
		Dungeon.level = null;
		Actor.clear();
		return out;
	}

	//What a replay puts back.
	private static final class Saved {
		final Hero hero = Dungeon.hero;
		final Level level = Dungeon.level;
		final int depth = Dungeon.depth, branch = Dungeon.branch, gold = Dungeon.gold, energy = Dungeon.energy;
		final long seed = Dungeon.seed;
		final int challenges = Dungeon.challenges;
		final float mobsToChampion = Dungeon.mobsToChampion;
		final boolean daily = Dungeon.daily, dailyReplay = Dungeon.dailyReplay;
		final String customSeedText = Dungeon.customSeedText;
		final ArrayList<Integer> generatedLevels = Dungeon.generatedLevels;
		final HashSet<Integer> chapters = Dungeon.chapters;
		final com.watabou.utils.SparseArray<ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item>> dropped = Dungeon.droppedItems;
		//the quest empties and refills this same list, so what is in it now is copied
		final java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item> impRewards = new java.util.ArrayList<>(Imp.Quest.rewardOptions);
		final Object scroll = Scroll.apdHandler(), potion = Potion.apdHandler(), ring = Ring.apdHandler();
		final Bundle limited = new Bundle(), generator = new Bundle(), statistics = new Bundle(), quests = new Bundle(), notes = new Bundle();

		//In a process where no game was ever started some of these are not set up yet and cannot be written out; then there is nothing
		//to put back, and what the replay leaves is what a new game's own start would create.
		boolean limitedOk, generatorOk, statisticsOk, questsOk, notesOk;

		Saved(){
			try {
				Dungeon.LimitedDrops.store(limited);
				limitedOk = true;
			} catch (RuntimeException e){
				limitedOk = false;
			}
			try {
				Generator.storeInBundle(generator);
				generatorOk = true;
			} catch (RuntimeException e){
				generatorOk = false;
			}
			try {
				Statistics.storeInBundle(statistics);
				statisticsOk = true;
			} catch (RuntimeException e){
				statisticsOk = false;
			}
			try {
				Ghost.Quest.storeInBundle(quests);
				Wandmaker.Quest.storeInBundle(quests);
				Blacksmith.Quest.storeInBundle(quests);
				Imp.Quest.storeInBundle(quests);
				questsOk = true;
			} catch (RuntimeException e){
				questsOk = false;
			}
			try {
				Notes.storeInBundle(notes);
				notesOk = true;
			} catch (RuntimeException e){
				notesOk = false;
			}
		}

		void restore(){
			Dungeon.hero = hero;
			Dungeon.level = level;
			Dungeon.depth = depth;
			Dungeon.branch = branch;
			Dungeon.gold = gold;
			Dungeon.energy = energy;
			Dungeon.seed = seed;
			Dungeon.challenges = challenges;
			Dungeon.mobsToChampion = mobsToChampion;
			Dungeon.daily = daily;
			Dungeon.dailyReplay = dailyReplay;
			Dungeon.customSeedText = customSeedText;
			Dungeon.generatedLevels = generatedLevels;
			Dungeon.chapters = chapters;
			Dungeon.droppedItems = dropped;
			Scroll.apdHandler(scroll);
			Potion.apdHandler(potion);
			Ring.apdHandler(ring);
			if (limitedOk) Dungeon.LimitedDrops.restore(limited);
			if (generatorOk) Generator.restoreFromBundle(generator);
			if (statisticsOk) Statistics.restoreFromBundle(statistics);
			if (questsOk){
				//restoring only sets what the saved state mentions, so start each quest from nothing or the replay's leftovers stay
				Ghost.Quest.reset();
				Wandmaker.Quest.reset();
				Blacksmith.Quest.reset();
				Imp.Quest.reset();
				Ghost.Quest.restoreFromBundle(quests);
				Wandmaker.Quest.restoreFromBundle(quests);
				Blacksmith.Quest.restoreFromBundle(quests);
				Imp.Quest.restoreFromBundle(quests);
				Imp.Quest.rewardOptions.clear();
				Imp.Quest.rewardOptions.addAll(impRewards);
			}
			if (notesOk) Notes.restoreFromBundle(notes);
		}
	}
}

