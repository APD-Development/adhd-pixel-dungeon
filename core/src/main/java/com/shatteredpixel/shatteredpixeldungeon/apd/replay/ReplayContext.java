package com.shatteredpixel.shatteredpixeldungeon.apd.replay;

import com.shatteredpixel.shatteredpixeldungeon.journal.Document;

//The reference context a floor replay runs in. Level generation reads two pieces of INSTALL state that have nothing to do with a run:
//the journal (which guide/lore pages the player has found) and the tutorial flag. A replay must not depend on them (the answer would
//change as the player progresses) and must not change them, so it never writes them: instead, while the replay thread is running,
//reads of those two things are answered with fixed reference values. Only the replay thread sees them.
public final class ReplayContext {

	private ReplayContext(){}

	private static volatile Thread thread;

	static void enter(){
		//its own random-number stack: the render thread draws from the shared one all the time, which would corrupt a seeded replay
		com.watabou.utils.Random.isolateThread();
		thread = Thread.currentThread();
	}

	static void exit(){
		thread = null;
		com.watabou.utils.Random.releaseThread();
	}

	//true only on the thread that is currently replaying
	public static boolean active(){
		return thread == Thread.currentThread();
	}

	//Reference journal: every Adventurer's Guide and Alchemy Guide page found (and read), no lore page found. The tutorial is off.
	public static boolean pageFound( Document doc ){
		return doc == Document.ADVENTURERS_GUIDE || doc == Document.ALCHEMY_GUIDE;
	}

	public static final boolean TUTORIAL = false;

	//the run's exotic-item chance (percent), which is part of its rules: set by FloorReplay for the replay thread
	private static volatile int exoticChance;

	static void exoticChance( int percent ){
		exoticChance = percent;
	}

	public static int exoticChance(){
		return exoticChance;
	}

	//the run's difficulty for the replay thread (see apd.difficulty.Difficulty)
	private static volatile int difficulty = 3;

	static void difficulty( int level ){
		difficulty = level;
	}

	public static int difficulty(){
		return difficulty;
	}

	//and the balance revision that run was played with
	private static volatile int balance;

	static void balance( int revision ){
		balance = revision;
	}

	public static int balance(){
		return balance;
	}

	//Tests that compare a replay with the game's own generation set this, so both break shop-bag ties the same (deterministic) way.
	public static volatile boolean forceDeterministicBags;

	public static boolean deterministicBags(){
		return forceDeterministicBags || active();
	}
}
